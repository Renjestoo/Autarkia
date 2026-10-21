package dev.autarkia.client.screen;

import dev.autarkia.Autarkia;
import dev.autarkia.network.MatterBufferActionPayload;
import dev.autarkia.registry.GuiLayout;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.storage.MatterBufferBlockEntity;
import dev.autarkia.storage.MatterBufferMenu;
import dev.autarkia.storage.MatterValues;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Screen of the {@code matter_buffer} (04 section 4.1): the MU bar (stored violet, free cyan, over the capacity), a
 * scroll list of the stored items with count and MU each, per row "Take" (one stack to the player) and "Dissolve" (a
 * second click within 3 s confirms), and the auto-dissolve toggle. Shift-clicking a stack in the inventory stores it.
 */
public class MatterBufferScreen extends AbstractContainerScreen<MatterBufferMenu> {
    private static final int BAR_X = 8;
    private static final int BAR_Y = 18;
    private static final int BAR_W = 160;
    private static final int LIST_X = 8;
    private static final int LIST_Y = 38;
    private static final int ROW_H = 20;
    private static final int ROWS = 4;
    private static final int TAKE_X = 104;
    private static final int TAKE_W = 25;
    private static final int DIS_X = 131;
    private static final int DIS_W = 32;
    private static final int NAME_W = TAKE_X - LIST_X - 22;
    private static final int CONFIRM_TICKS = 60;
    private final GuiLayout layout = GuiLayouts.MATTER_BUFFER;
    private final Identifier background;
    private int scroll;
    private @Nullable ItemResource confirm;
    private int confirmTicks;
    private int seenAuto = -1;

    public MatterBufferScreen(MatterBufferMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, GuiLayouts.MATTER_BUFFER.width(), GuiLayouts.MATTER_BUFFER.height());
        this.background = Autarkia.id("textures/gui/" + layout.name() + ".png");
        this.inventoryLabelY = layout.invY() - 11;
    }

    private @Nullable MatterBufferBlockEntity buffer() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos) instanceof MatterBufferBlockEntity b ? b : null;
    }

    private List<MatterBufferBlockEntity.Entry> rows() {
        MatterBufferBlockEntity b = buffer();
        return b == null ? List.of() : b.entries();
    }

    private void send(int action, ItemStack item) {
        ClientPacketDistributor.sendToServer(new MatterBufferActionPayload(menu.pos, action, item));
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        MatterBufferBlockEntity b = buffer();
        boolean on = b != null && b.autoDissolve();
        seenAuto = on ? 1 : 0;
        addRenderableWidget(Button.builder(Component.translatable(on ? "screen.autarkia.matter_buffer.auto_on" : "screen.autarkia.matter_buffer.auto_off"),
            btn -> send(MatterBufferActionPayload.AUTO, ItemStack.EMPTY)).bounds(leftPos + 104, topPos + 4, 64, 12).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        MatterBufferBlockEntity b = buffer();
        int now = b != null && b.autoDissolve() ? 1 : 0;
        if (now != seenAuto) {
            rebuild();
        }
        if (confirmTicks > 0 && --confirmTicks == 0) {
            confirm = null;
        }
        scroll = Math.min(scroll, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, rows().size() - ROWS);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (isHovering(LIST_X, LIST_Y, 160, ROWS * ROW_H, x, y)) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(dy)));
            return true;
        }
        return super.mouseScrolled(x, y, dx, dy);
    }

    private int rowAt(double x, double y) {
        if (!isHovering(LIST_X, LIST_Y, 160, ROWS * ROW_H, x, y)) {
            return -1;
        }
        int r = (int) Math.floor((y - topPos - LIST_Y) / ROW_H);
        return r < 0 || r >= ROWS ? -1 : scroll + r;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        int i = rowAt(e.x(), e.y());
        List<MatterBufferBlockEntity.Entry> list = rows();
        if (i >= 0 && i < list.size()) {
            ItemResource res = list.get(i).resource();
            int rowY = LIST_Y + (i - scroll) * ROW_H;
            if (isHovering(TAKE_X, rowY + 3, TAKE_W, 14, e.x(), e.y())) {
                send(MatterBufferActionPayload.EXTRACT, res.toStack(1));
                return true;
            }
            if (isHovering(DIS_X, rowY + 3, DIS_W, 14, e.x(), e.y())) {
                if (res.equals(confirm)) {
                    send(MatterBufferActionPayload.DISSOLVE, res.toStack(1));
                    confirm = null;
                    confirmTicks = 0;
                } else {
                    confirm = res;
                    confirmTicks = CONFIRM_TICKS;
                }
                return true;
            }
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        MatterBufferBlockEntity b = buffer();
        int x = leftPos + BAR_X;
        int y = topPos + BAR_Y;
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + 7, 0xFF373737);
        g.fill(x, y, x + BAR_W, y + 6, 0xFF1A1424);
        if (b != null) {
            double cap = Math.max(1, b.capacity());
            int used = (int) Math.round(BAR_W * Math.min(1.0, b.used() / cap));
            int free = (int) Math.round(BAR_W * Math.min(1.0, (b.used() + b.free()) / cap));
            g.fill(x, y, x + used, y + 6, 0xFF8A4FFF);
            g.fill(x + used, y, x + Math.max(used, free), y + 6, 0xFF4FD8E0);
        }
        List<MatterBufferBlockEntity.Entry> list = rows();
        int tier = b == null ? 5 : b.tier();
        for (int r = 0; r < ROWS; r++) {
            int i = scroll + r;
            if (i >= list.size()) {
                break;
            }
            MatterBufferBlockEntity.Entry e = list.get(i);
            int rx = leftPos + LIST_X;
            int ry = topPos + LIST_Y + r * ROW_H;
            if (r % 2 == 1) {
                g.fill(rx, ry, rx + 160, ry + ROW_H, 0x18FFFFFF);
            }
            ItemStack shown = e.resource().toStack(1);
            g.fakeItem(shown, rx + 1, ry + 2);
            String name = font.plainSubstrByWidth(shown.getHoverName().getString(), NAME_W);
            g.text(font, Component.literal(name), rx + 20, ry + 2, 0xFF303030, false);
            Component sub = Component.translatable("screen.autarkia.matter_buffer.row", MatterBufferBlockEntity.fmt(e.count()),
                MatterBufferBlockEntity.fmt(MatterValues.of(shown, tier)));
            g.text(font, sub, rx + 20, ry + 11, 0xFF5A5A5A, false);
            button(g, leftPos + TAKE_X, ry + 3, TAKE_W, Component.translatable("screen.autarkia.matter_buffer.extract"), 0xFF3A5A8A);
            boolean asking = e.resource().equals(confirm);
            button(g, leftPos + DIS_X, ry + 3, DIS_W, Component.translatable(asking ? "screen.autarkia.matter_buffer.confirm"
                : "screen.autarkia.matter_buffer.dissolve"), asking ? 0xFFB03030 : 0xFF6A3A8A);
        }
        if (list.size() > ROWS) {
            int track = ROWS * ROW_H;
            int thumb = Math.max(8, track * ROWS / list.size());
            int ty = topPos + LIST_Y + (track - thumb) * scroll / Math.max(1, maxScroll());
            g.fill(leftPos + 165, topPos + LIST_Y, leftPos + 167, topPos + LIST_Y + track, 0xFF2A2A2A);
            g.fill(leftPos + 165, ty, leftPos + 167, ty + thumb, 0xFFB0A0D0);
        }
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, Component label, int color) {
        g.fill(x, y, x + w, y + 14, 0xFF101010);
        g.fill(x + 1, y + 1, x + w - 1, y + 13, color);
        float s = Math.min(1f, (w - 4f) / Math.max(1, font.width(label)));
        g.pose().pushMatrix();
        g.pose().translate(x + w / 2f - font.width(label) * s / 2f, y + 7 - 4 * s);
        g.pose().scale(s, s);
        g.text(font, label, 0, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        MatterBufferBlockEntity b = buffer();
        if (b == null) {
            return;
        }
        g.text(font, Component.translatable("screen.autarkia.matter_buffer.bar", MatterBufferBlockEntity.fmt(b.used()), MatterBufferBlockEntity.fmt(b.free()),
            MatterBufferBlockEntity.fmt(b.capacity())), BAR_X, BAR_Y + 9, 0xFF404040, false);
        if (b.entries().isEmpty()) {
            g.centeredText(font, Component.translatable("screen.autarkia.matter_buffer.empty"), 88, LIST_Y + 36, 0xFF707070);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int i = rowAt(mouseX, mouseY);
        List<MatterBufferBlockEntity.Entry> list = rows();
        MatterBufferBlockEntity b = buffer();
        if (b != null && i >= 0 && i < list.size() && mouseX < leftPos + TAKE_X) {
            MatterBufferBlockEntity.Entry e = list.get(i);
            ItemStack s = e.resource().toStack(1);
            long each = MatterValues.of(s, b.tier());
            g.setTooltipForNextFrame(font, List.of(s.getHoverName().getVisualOrderText(), Component.translatable("screen.autarkia.matter_buffer.detail",
                MatterBufferBlockEntity.fmt(e.count()), MatterBufferBlockEntity.fmt(each), MatterBufferBlockEntity.fmt(each * e.count())).getVisualOrderText()),
                mouseX, mouseY);
        }
    }
}

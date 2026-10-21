package dev.autarkia.client.screen;

import dev.autarkia.Autarkia;
import dev.autarkia.logistics.MagRailStationBlockEntity;
import dev.autarkia.logistics.MagRailStationMenu;
import dev.autarkia.network.MagRailStationPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Screen of the {@code mag_rail_station} (04 section 4.1): name field (sent 10 ticks after the last edit and when the
 * screen closes), destination button (cycles Next and the named stations of the line), input and output tabs over one
 * 3 x 9 grid, crate slot, filter (3 x 3 ghost slots with whitelist and component toggles), energy bar and the line status.
 */
public class MagRailStationScreen extends AbstractContainerScreen<MagRailStationMenu> {
    private static final int TEXT_W = 86;
    private final Identifier background;
    private @Nullable EditBox name;
    private String sentName = "";
    private String destination = "";
    private int delay;
    private String seen = "";

    public MagRailStationScreen(MagRailStationMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, menu.layout.width(), menu.layout.height());
        this.background = Autarkia.id("textures/gui/" + menu.layout.name() + ".png");
        this.inventoryLabelY = menu.layout.invY() - 11;
    }

    private @Nullable MagRailStationBlockEntity station() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos) instanceof MagRailStationBlockEntity s ? s : null;
    }

    private void button(int id) {
        if (id <= 1) {
            menu.clickMenuButton(minecraft.player, id);
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void send() {
        if (name != null) {
            sentName = name.getValue();
            ClientPacketDistributor.sendToServer(new MagRailStationPayload(menu.pos, sentName, destination));
        }
    }

    private List<String> options() {
        List<String> out = new ArrayList<>();
        out.add("");
        MagRailStationBlockEntity s = station();
        if (s != null) {
            out.addAll(s.lineNames());
        }
        if (!out.contains(destination)) {
            out.add(destination);
        }
        return out;
    }

    @Override
    protected void init() {
        super.init();
        MagRailStationBlockEntity s = station();
        String current = s == null ? "" : s.name();
        destination = s == null ? "" : s.destination();
        sentName = current;
        name = new EditBox(font, leftPos + 8, topPos + 16, 76, 12, Component.translatable("screen.autarkia.mag_rail.name"));
        name.setMaxLength(MagRailStationBlockEntity.NAME_LENGTH);
        name.setValue(current);
        name.setHint(Component.translatable("screen.autarkia.mag_rail.name"));
        name.setResponder(v -> delay = 10);
        rebuild();
    }

    private String state() {
        MagRailStationBlockEntity s = station();
        return menu.tab() + "|" + menu.value(6) + "|" + menu.value(7) + "|" + destination + "|" + (s == null ? List.of() : s.lineNames());
    }

    private void rebuild() {
        clearWidgets();
        seen = state();
        int x = leftPos;
        int y = topPos;
        addRenderableWidget(name);
        Component to = Component.translatable("screen.autarkia.mag_rail.to", destination.isEmpty() ? Component.translatable("screen.autarkia.mag_rail.next")
            : Component.literal(destination));
        addRenderableWidget(Button.builder(to, b -> {
            List<String> opts = options();
            destination = opts.get((opts.indexOf(destination) + 1) % opts.size());
            send();
            rebuild();
        }).bounds(x + 88, y + 15, 80, 14).build());
        Button in = Button.builder(Component.translatable("screen.autarkia.mag_rail.input"), b -> button(0)).bounds(x + 7, y + 30, 44, 12).build();
        in.active = menu.tab() != 0;
        addRenderableWidget(in);
        Button out = Button.builder(Component.translatable("screen.autarkia.mag_rail.output"), b -> button(1)).bounds(x + 53, y + 30, 44, 12).build();
        out.active = menu.tab() != 1;
        addRenderableWidget(out);
        boolean white = menu.value(6) == 1;
        addRenderableWidget(Button.builder(Component.literal(white ? "W" : "B"), b -> button(2))
            .tooltip(Tooltip.create(Component.translatable(white ? "screen.autarkia.filter.whitelist" : "screen.autarkia.filter.blacklist")))
            .bounds(x + 97, y + 106, 16, 16).build());
        boolean comps = menu.value(7) == 1;
        addRenderableWidget(Button.builder(Component.literal(comps ? "C" : "I"), b -> button(3))
            .tooltip(Tooltip.create(Component.translatable(comps ? "screen.autarkia.filter.components" : "screen.autarkia.filter.item_only")))
            .bounds(x + 97, y + 124, 16, 16).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (delay > 0 && --delay == 0 && name != null && !name.getValue().equals(sentName)) {
            send();
        }
        if (!state().equals(seen)) {
            rebuild();
        }
    }

    @Override
    public void removed() {
        if (name != null && !name.getValue().equals(sentName)) {
            send();
        }
        super.removed();
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (name != null && name.isFocused() && e.key() != 256) {
            name.keyPressed(e);
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        int cap = Math.max(1, menu.value(1));
        int w = (int) Math.round(64.0 * menu.value(0) / cap);
        g.fill(leftPos + 28, topPos + 108, leftPos + 92, topPos + 113, 0xFF1A1424);
        g.fill(leftPos + 28, topPos + 108, leftPos + 28 + w, topPos + 113, 0xFFB070FF);
    }

    private void line(GuiGraphicsExtractor g, Component c, int y, int color) {
        float s = Math.min(1f, TEXT_W / (float) Math.max(1, font.width(c)));
        g.pose().pushMatrix();
        g.pose().translate(8, y);
        g.pose().scale(s, s);
        g.text(font, c, 0, 0, color, false);
        g.pose().popMatrix();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        MagRailStationBlockEntity.Status[] all = MagRailStationBlockEntity.Status.values();
        MagRailStationBlockEntity.Status st = all[Math.clamp(menu.value(2), 0, all.length - 1)];
        int color = switch (st) {
            case READY, WAITING -> 0xFF2E7D32;
            case LOCKED, NO_CRATE, BUSY -> 0xFF8D6E00;
            default -> 0xFFB71C1C;
        };
        line(g, Component.translatable(st.key()), 125, color);
        line(g, Component.translatable("screen.autarkia.mag_rail.line", menu.value(4), menu.value(5)), 135, 0xFF404040);
        line(g, Component.translatable("screen.autarkia.mag_rail.in_flight", menu.value(3)), 145, 0xFF404040);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(28, 107, 64, 7, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("tooltip.autarkia.energy", menu.value(0), menu.value(1)).getVisualOrderText()),
                mouseX, mouseY);
        }
    }
}

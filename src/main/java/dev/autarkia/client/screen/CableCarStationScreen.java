package dev.autarkia.client.screen;

import dev.autarkia.Autarkia;
import dev.autarkia.logistics.CableCarStationBlockEntity;
import dev.autarkia.logistics.CableCarStationMenu;
import dev.autarkia.logistics.CableLine;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the {@code cable_car_station} (04 section 4.1): input and output tabs over one 3 x 9 grid, the two gondola
 * part slots with the gondola count spinner (1 to the line maximum), the line length and state, the energy bar, and the
 * wind swing gauge: a needle from 0 to 60 degrees with marks at 30 (slow) and the hold angle.
 */
public class CableCarStationScreen extends AbstractContainerScreen<CableCarStationMenu> {
    private static final int GAUGE_X = 140;
    private static final int GAUGE_Y = 130;
    private static final int GAUGE_R = 24;
    private final Identifier background;
    private int seen = -1;

    public CableCarStationScreen(CableCarStationMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, menu.layout.width(), menu.layout.height());
        this.background = Autarkia.id("textures/gui/" + menu.layout.name() + ".png");
        this.inventoryLabelY = menu.layout.invY() - 11;
    }

    private void button(int id) {
        if (id <= 1) {
            menu.clickMenuButton(minecraft.player, id);
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int x = leftPos;
        int y = topPos;
        Button in = Button.builder(Component.translatable("screen.autarkia.cable_car.input"), b -> button(0)).bounds(x + 7, y + 16, 44, 12).build();
        in.active = menu.tab() != 0;
        addRenderableWidget(in);
        Button out = Button.builder(Component.translatable("screen.autarkia.cable_car.output"), b -> button(1)).bounds(x + 53, y + 16, 44, 12).build();
        out.active = menu.tab() != 1;
        addRenderableWidget(out);
        Button down = Button.builder(Component.literal("-"), b -> button(2)).bounds(x + 48, y + 93, 12, 12).build();
        down.active = menu.value(8) == 1 && menu.value(0) > 1;
        addRenderableWidget(down);
        Button up = Button.builder(Component.literal("+"), b -> button(3)).bounds(x + 90, y + 93, 12, 12).build();
        up.active = menu.value(8) == 1 && menu.value(0) < menu.value(1);
        addRenderableWidget(up);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int now = menu.tab() | menu.value(0) << 1 | menu.value(1) << 5 | menu.value(8) << 9;
        if (now != seen) {
            seen = now;
            rebuild();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        int cap = Math.max(1, menu.value(7));
        int w = (int) Math.round(96.0 * menu.value(6) / cap);
        g.fill(leftPos + 8, topPos + 134, leftPos + 104, topPos + 137, 0xFF2A1A12);
        g.fill(leftPos + 8, topPos + 134, leftPos + 8 + w, topPos + 137, 0xFFE0A030);
        gauge(g);
    }

    private void gauge(GuiGraphicsExtractor g) {
        int cx = leftPos + GAUGE_X;
        int cy = topPos + GAUGE_Y;
        int hold = dev.autarkia.config.AutarkiaConfig.GONDOLA_HOLD_DEGREES.get();
        for (int d = 0; d <= 60; d += 5) {
            int color = d >= hold ? 0xFFC03030 : d >= CableLine.SLOW_DEGREES ? 0xFFE0A030 : 0xFF3FB04F;
            double ang = Math.PI * (1 - d / 60.0);
            int px = cx + (int) Math.round(Math.cos(ang) * GAUGE_R);
            int py = cy - (int) Math.round(Math.sin(ang) * GAUGE_R);
            g.fill(px - 1, py - 1, px + 1, py + 1, color);
        }
        double swing = Math.min(60, menu.value(5) / 10.0);
        double ang = Math.PI * (1 - swing / 60.0);
        for (int s = 0; s <= GAUGE_R - 3; s += 2) {
            int px = cx + (int) Math.round(Math.cos(ang) * s);
            int py = cy - (int) Math.round(Math.sin(ang) * s);
            g.fill(px - 1, py - 1, px + 1, py + 1, 0xFF202020);
        }
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFF404040);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        g.text(font, Component.translatable("screen.autarkia.cable_car.parts"), 8, 84, 0xFF404040, false);
        String count = menu.value(0) + " / " + menu.value(1);
        g.text(font, Component.literal(count), 75 - font.width(count) / 2, 95, 0xFF303030, false);
        g.text(font, Component.translatable("screen.autarkia.cable_car.swing", menu.value(5) / 10), 112, 96, 0xFF404040, false);
        if (menu.value(8) == 0) {
            g.text(font, Component.translatable("screen.autarkia.cable_car.unlinked"), 8, 114, 0xFF8D6E00, false);
            return;
        }
        g.text(font, Component.translatable("screen.autarkia.cable_car.line", menu.value(3)), 8, 114, 0xFF404040, false);
        CableLine.State[] states = CableLine.State.values();
        CableLine.State st = states[Math.clamp(menu.value(4), 0, states.length - 1)];
        int color = st == CableLine.State.RUNNING ? 0xFF2E7D32 : st == CableLine.State.OBSTRUCTED || st == CableLine.State.WIND_HOLD ? 0xFFB71C1C : 0xFF8D6E00;
        g.text(font, Component.translatable(st.key()), 8, 124, color, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(8, 133, 96, 5, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("tooltip.autarkia.energy", menu.value(6), menu.value(7)).getVisualOrderText()),
                mouseX, mouseY);
        }
        if (minecraft.level != null && minecraft.level.getBlockEntity(menu.pos) instanceof CableCarStationBlockEntity s && s.partner() != null
            && isHovering(8, 113, 100, 9, mouseX, mouseY)) {
            BlockPos p = s.partner();
            g.setTooltipForNextFrame(font, List.of(Component.translatable("screen.autarkia.cable_car.partner", p.getX(), p.getY(), p.getZ()).getVisualOrderText()),
                mouseX, mouseY);
        }
    }
}

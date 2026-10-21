package dev.autarkia.client.screen;

import dev.autarkia.energy.BoilerBlockEntity;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the {@code boiler} menu (02 section 9.1): water bar (8,000 mB), steam bar, a pressure dial from 0 to 12 bar
 * with a red zone from 10 bar whose needle eases 0.2 bar per frame, temperature, and fire level buttons 0 to 4.
 */
public class BoilerScreen extends SlotScreen {
    private static final int WATER_X = 28;
    private static final int STEAM_X = 46;
    private static final int BAR_Y = 18;
    private static final int DIAL_X = 88;
    private static final int DIAL_Y = 46;
    private static final int DIAL_R = 22;
    private double needle;

    public BoilerScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i <= 4; i++) {
            int level = i;
            addRenderableWidget(Button.builder(Component.literal(String.valueOf(i)), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, level))
                .bounds(leftPos + 116 + i * 11, topPos + 62, 10, 12).build());
        }
    }

    private double pressure() {
        return menu.value(2) / 100.0;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = leftPos;
        int y = topPos;
        int water = Math.min(52, (int) Math.ceil(52.0 * menu.value(0) / BoilerBlockEntity.TANK));
        g.fill(x + WATER_X, y + BAR_Y + 52 - water, x + WATER_X + 12, y + BAR_Y + 52, 0xFF3A6FD8);
        int steam = Math.min(52, (int) Math.ceil(52.0 * menu.value(1) / 4800.0));
        g.fill(x + STEAM_X, y + BAR_Y + 52 - steam, x + STEAM_X + 12, y + BAR_Y + 52, 0xFFE8E8E8);
        int cx = x + DIAL_X;
        int cy = y + DIAL_Y;
        g.fill(cx - DIAL_R - 2, cy - DIAL_R - 2, cx + DIAL_R + 3, cy + 3, 0xFF2A2A2A);
        for (int deg = 0; deg <= 180; deg += 3) {
            double bar = deg / 180.0 * 12;
            int color = bar >= 10 ? 0xFFD03020 : bar >= 9 ? 0xFFE0A030 : 0xFFE0E0D0;
            double rad = Math.toRadians(180 - deg);
            for (int r = DIAL_R - 3; r <= DIAL_R; r++) {
                int px = cx + (int) Math.round(Math.cos(rad) * r);
                int py = cy - (int) Math.round(Math.sin(rad) * r);
                g.fill(px, py, px + 1, py + 1, color);
            }
        }
        double target = Math.min(12, pressure());
        needle += Math.max(-0.2, Math.min(0.2, target - needle));
        double rad = Math.toRadians(180 - needle / 12 * 180);
        for (int r = 0; r < DIAL_R - 4; r++) {
            int px = cx + (int) Math.round(Math.cos(rad) * r);
            int py = cy - (int) Math.round(Math.sin(rad) * r);
            g.fill(px, py, px + 1, py + 1, 0xFFFF4020);
        }
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF101010);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        g.text(font, Component.translatable("screen.autarkia.boiler.pressure", String.format(java.util.Locale.ROOT, "%.1f", pressure())), 116, 18,
            pressure() >= 10 ? 0xFFB01818 : 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.boiler.temperature", menu.value(3)), 116, 30, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.boiler.fire", menu.value(4)), 116, 42, 0xFF404040, false);
        if (menu.value(6) > 0) {
            g.text(font, Component.translatable("screen.autarkia.boiler.venting"), 116, 52, 0xFFB01818, false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(WATER_X, BAR_Y, 12, 52, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("screen.autarkia.boiler.water", menu.value(0), BoilerBlockEntity.TANK).getVisualOrderText()),
                mouseX, mouseY);
        } else if (isHovering(STEAM_X, BAR_Y, 12, 52, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("screen.autarkia.boiler.steam", menu.value(1)).getVisualOrderText()), mouseX, mouseY);
        }
    }
}

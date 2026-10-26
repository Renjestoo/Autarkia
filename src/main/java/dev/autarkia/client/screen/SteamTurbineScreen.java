package dev.autarkia.client.screen;

import dev.autarkia.energy.SteamTurbineBlockEntity;
import dev.autarkia.registry.SlotMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Steam turbine screen (02 section 9.1): rotor speed dial, steam input in mB/t, output in FE/t, the buffer bar and a
 * status text: running, no boiler, low steam (under 10 mB/t) or buffer full.
 */
public class SteamTurbineScreen extends SlotScreen {
    private static final int DIAL_X = 52;
    private static final int DIAL_Y = 52;
    private static final int RADIUS = 26;
    private static final int TEXT_X = 88;

    public SteamTurbineScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int cx = leftPos + DIAL_X;
        int cy = topPos + DIAL_Y;
        for (int i = 0; i <= 20; i++) {
            double ang = Math.PI * (1 - i / 20.0);
            int x = cx + (int) Math.round(Math.cos(ang) * RADIUS);
            int y = cy - (int) Math.round(Math.sin(ang) * RADIUS);
            int col = i >= 18 ? 0xFFD03030 : 0xFF404040;
            g.fill(x - 1, y - 1, x + 1, y + 1, col);
        }
        double r = menu.value(0) / 1000.0;
        double ang = Math.PI * (1 - r);
        for (int s = 0; s < RADIUS - 4; s++) {
            int x = cx + (int) Math.round(Math.cos(ang) * s);
            int y = cy - (int) Math.round(Math.sin(ang) * s);
            g.fill(x, y, x + 1, y + 1, 0xFFB01818);
        }
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFF202020);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        g.text(font, Component.translatable("screen.autarkia.turbine.rotor", menu.value(0) / 10), DIAL_X - 20, DIAL_Y + 6, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.turbine.steam", menu.value(4)), TEXT_X, 26, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.turbine.output", menu.value(5)), TEXT_X, 38, 0xFF404040, false);
        String key = switch (menu.value(6)) {
            case SteamTurbineBlockEntity.NO_BOILER -> "screen.autarkia.turbine.no_boiler";
            case SteamTurbineBlockEntity.LOW_STEAM -> "screen.autarkia.turbine.low_steam";
            case SteamTurbineBlockEntity.FULL -> "screen.autarkia.turbine.full";
            default -> "status.autarkia.running";
        };
        int color = switch (menu.value(6)) {
            case SteamTurbineBlockEntity.RUNNING -> 0xFF2E7D32;
            case SteamTurbineBlockEntity.FULL -> 0xFF8D6E00;
            default -> 0xFFB01818;
        };
        g.text(font, Component.translatable(key), TEXT_X, 50, color, false);
    }
}

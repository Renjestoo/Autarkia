package dev.autarkia.client.screen;

import dev.autarkia.registry.SlotMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Superconducting coil screen (02 section 9.1): 4 coolant slots, the temperature bar with a mark at 0.5 (I/O halved
 * above it), the energy bar, the charge or the quenched state, I/O of the last tick and the coolant seconds left.
 */
public class SuperconductingCoilScreen extends SlotScreen {
    private static final int TEMP_X = 134;
    private static final int TEMP_Y = 18;

    public SuperconductingCoilScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        float t = menu.value(0) / 1000f;
        int h = Math.min(52, Math.round(52 * t));
        int r = (int) Mth.lerp(t, 0x40, 0xFF);
        int b = (int) Mth.lerp(t, 0xFF, 0x30);
        g.fill(leftPos + TEMP_X, topPos + TEMP_Y + 52 - h, leftPos + TEMP_X + 12, topPos + TEMP_Y + 52, 0xFF000000 | r << 16 | 0x60 << 8 | b);
        g.fill(leftPos + TEMP_X - 1, topPos + TEMP_Y + 26, leftPos + TEMP_X + 13, topPos + TEMP_Y + 27, 0xFFFFFFFF);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        if (menu.value(7) == 1) {
            g.text(font, Component.translatable("screen.autarkia.coil.quenched"), 8, 40, 0xFFB71C1C, false);
        } else {
            long cap = Math.max(1, menu.value(3));
            g.text(font, Component.translatable("screen.autarkia.energy_storage.percent", menu.value(2) * 100L / cap), 8, 40, 0xFF404040, false);
        }
        g.text(font, Component.translatable("screen.autarkia.energy_storage.io", menu.value(4), menu.value(5)), 8, 50, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.cryo.coolant", menu.value(6) / 20), 8, 60, menu.value(6) > 0 ? 0xFF404040 : 0xFFB71C1C, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(TEMP_X, TEMP_Y, 12, 52, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("tooltip.autarkia.coil.temperature", Math.round(menu.value(0) / 10.0),
                menu.value(8)).getVisualOrderText()), mouseX, mouseY);
        }
    }
}

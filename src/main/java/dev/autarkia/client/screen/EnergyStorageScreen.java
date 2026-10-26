package dev.autarkia.client.screen;

import dev.autarkia.registry.SlotMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Screen of the {@code energy_storage} menu (02 section 9.1): energy bar, stored FE, input and output of the last tick, item charge slot. */
public class EnergyStorageScreen extends SlotScreen {
    public EnergyStorageScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        g.text(font, Component.translatable("screen.autarkia.energy_storage.stored", menu.value(2), menu.value(3)), 26, 22, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.energy_storage.percent", menu.value(3) == 0 ? 0 : menu.value(2) * 100L / menu.value(3)),
            26, 34, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.energy_storage.io_in", menu.value(4)), 26, 46, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.energy_storage.io_out", menu.value(5)), 26, 58, 0xFF404040, false);
    }
}

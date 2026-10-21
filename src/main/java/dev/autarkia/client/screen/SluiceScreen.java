package dev.autarkia.client.screen;

import dev.autarkia.processing.SluiceBlockEntity;
import dev.autarkia.registry.SlotMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Screen of the {@code sluice} menu (03 section 4.4): the machine layout plus the water state at the head. */
public class SluiceScreen extends SlotScreen {
    public SluiceScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        SluiceBlockEntity.Water w = SluiceBlockEntity.Water.values()[Math.max(0, Math.min(2, menu.value(5)))];
        g.text(font, Component.translatable("jade.autarkia.sluice." + w.name().toLowerCase(java.util.Locale.ROOT)), 58, 58,
            w == SluiceBlockEntity.Water.NONE ? 0xFFB01818 : 0xFF2050A0, false);
    }
}

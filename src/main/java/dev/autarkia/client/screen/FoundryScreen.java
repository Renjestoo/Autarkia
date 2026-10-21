package dev.autarkia.client.screen;

import dev.autarkia.colony.ReplicatorFoundryBlockEntity;
import dev.autarkia.registry.SlotMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the {@code replicator_foundry} menu (05b section 9.1): the slot layout with the print arrow and energy bar,
 * the foundry state under the title and the walker count "Walkers a/max" beside it.
 */
public class FoundryScreen extends SlotScreen {
    public FoundryScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        ReplicatorFoundryBlockEntity.Status[] all = ReplicatorFoundryBlockEntity.Status.values();
        ReplicatorFoundryBlockEntity.Status st = all[Math.max(0, Math.min(all.length - 1, menu.value(4)))];
        Component status = Component.translatable("jade.autarkia.replicator." + st.name().toLowerCase(java.util.Locale.ROOT));
        g.text(font, status, 24, titleLabelY + 10, st == ReplicatorFoundryBlockEntity.Status.RUNNING ? 0xFF2E7D32 : 0xFF6B4F00, false);
        Component walkers = Component.translatable("jade.autarkia.replicator.walkers", menu.value(5), menu.value(6));
        g.text(font, walkers, imageWidth - 8 - font.width(walkers), titleLabelY, 0xFF404040, false);
    }
}

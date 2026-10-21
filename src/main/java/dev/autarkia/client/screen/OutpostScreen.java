package dev.autarkia.client.screen;

import dev.autarkia.colony.Colony;
import dev.autarkia.registry.SlotMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Outpost core screen (05 section 8.1): outpost number, link, last catch-up, power factor, profile time and buffer. */
public class OutpostScreen extends SlotScreen {
    public OutpostScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        Colony.Link link = Colony.Link.values()[Math.max(0, Math.min(Colony.Link.values().length - 1, menu.value(2)))];
        g.text(font, Component.translatable("screen.autarkia.outpost.index", menu.value(0), menu.value(1)), 8, 18, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.outpost.link", Component.translatable("link.autarkia." + link.getSerializedName())), 8, 28,
            link == Colony.Link.NONE ? 0xFFB01818 : 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.outpost.catch_up", menu.value(3)), 8, 38, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.outpost.power", menu.value(4)), 8, 48, menu.value(4) < 100 ? 0xFFB01818 : 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.outpost.profile", menu.value(5) / 60), 8, 58, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.outpost.buffer", menu.value(6)), 8, 68, 0xFF404040, false);
    }
}

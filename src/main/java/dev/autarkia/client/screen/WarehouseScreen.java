package dev.autarkia.client.screen;

import dev.autarkia.registry.SlotMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Warehouse controller screen (04 section 2.4): indexed racks, fill %, overloaded racks, open requests and free crate positions. */
public class WarehouseScreen extends SlotScreen {
    public WarehouseScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        g.text(font, Component.translatable("screen.autarkia.warehouse.racks", menu.value(0)), 8, 18, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.warehouse.fill", menu.value(1)), 8, 28, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.warehouse.overloaded", menu.value(2)), 8, 38, menu.value(2) > 0 ? 0xFFB01818 : 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.warehouse.requests", menu.value(3)), 8, 48, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.warehouse.free", menu.value(4)), 8, 58, 0xFF404040, false);
    }
}

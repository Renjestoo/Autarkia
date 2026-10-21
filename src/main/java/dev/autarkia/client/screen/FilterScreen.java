package dev.autarkia.client.screen;

import dev.autarkia.Autarkia;
import dev.autarkia.logistics.FilterMenu;
import dev.autarkia.registry.AutarkiaMenus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the {@code belt_splitter} and {@code sorting_chute} menus (04 section 4.1): mode (splitter only) and side
 * toggles on the left, the 3 x 3 ghost filter on the right with the whitelist and match-components toggles.
 */
public class FilterScreen extends AbstractContainerScreen<FilterMenu> {
    private final boolean splitter;
    private final Identifier background;
    private int seen = -1;

    public FilterScreen(FilterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 166);
        this.splitter = menu.getType() == AutarkiaMenus.BELT_SPLITTER.get();
        this.background = Autarkia.id("textures/gui/" + (splitter ? "belt_splitter" : "sorting_chute") + ".png");
    }

    private void button(int id) {
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
        if (splitter) {
            addRenderableWidget(Button.builder(Component.translatable(menu.value(0) == 1 ? "screen.autarkia.filter.mode_filter"
                : "screen.autarkia.filter.mode_alternate"), b -> button(0)).bounds(x + 8, y + 17, 84, 13).build());
        }
        addRenderableWidget(Button.builder(Component.translatable(menu.value(1) == 1 ? "screen.autarkia.filter.side_right"
            : "screen.autarkia.filter.side_left"), b -> button(1)).bounds(x + 8, y + 31, 84, 13).build());
        addRenderableWidget(Button.builder(Component.translatable(menu.value(2) == 1 ? "screen.autarkia.filter.whitelist"
            : "screen.autarkia.filter.blacklist"), b -> button(2)).bounds(x + 8, y + 45, 84, 13).build());
        addRenderableWidget(Button.builder(Component.translatable(menu.value(3) == 1 ? "screen.autarkia.filter.components"
            : "screen.autarkia.filter.item_only"), b -> button(3)).bounds(x + 8, y + 59, 84, 13).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int now = menu.value(0) | menu.value(1) << 1 | menu.value(2) << 2 | menu.value(3) << 3;
        if (now != seen) {
            seen = now;
            rebuild();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }
}

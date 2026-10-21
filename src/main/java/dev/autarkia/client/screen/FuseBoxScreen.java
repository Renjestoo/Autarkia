package dev.autarkia.client.screen;

import dev.autarkia.registry.SlotMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Fuse box screen (02 section 9.1): state (closed, open with the reset countdown, locked), trip count, reset button. */
public class FuseBoxScreen extends AbstractContainerScreen<SlotMenu> {
    private static final Identifier BACKGROUND = dev.autarkia.Autarkia.id("textures/gui/fuse_box.png");

    public FuseBoxScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 166);
        this.inventoryLabelY = menu.layout.invY() - 11;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.fuse.reset"),
            b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)).bounds(leftPos + 108, topPos + 50, 60, 16).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        int state = menu.value(0);
        int color = state == 0 ? 0xFF3FA040 : state == 1 ? 0xFFE0A020 : 0xFFD03030;
        g.fill(leftPos + 8, topPos + 20, leftPos + 20, topPos + 32, 0xFF202020);
        g.fill(leftPos + 9, topPos + 21, leftPos + 19, topPos + 31, color);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        int state = menu.value(0);
        Component line = state == 2 ? Component.translatable("jade.autarkia.fuse.locked")
            : state == 1 ? Component.translatable("jade.autarkia.fuse.open", menu.value(2)) : Component.translatable("jade.autarkia.fuse.closed");
        g.text(font, line, 24, 22, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.fuse.trips", menu.value(1)), 8, 40, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.fuse.spans", menu.value(3)), 8, 54, 0xFF404040, false);
    }
}

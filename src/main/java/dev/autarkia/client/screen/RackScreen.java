package dev.autarkia.client.screen;

import dev.autarkia.Autarkia;
import dev.autarkia.storage.RackMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The rack screen (04 section 4.1): one crate slot per position with an Open button that shows that crate's 27 slots,
 * the load bar (green up to 100%, yellow up to 120%, red above) with "load / rating units", and the countdown in stage 3.
 */
public class RackScreen extends AbstractContainerScreen<RackMenu> {
    private static final int BAR_X = 8;
    private static final int BAR_Y = 40;
    private static final int BAR_W = 160;
    private final Identifier background;

    public RackScreen(RackMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, RackMenu.INV_Y + 82);
        this.background = Autarkia.id("textures/gui/rack_" + menu.positions + ".png");
        this.inventoryLabelY = RackMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < menu.positions; i++) {
            int id = i;
            addRenderableWidget(Button.builder(Component.literal("»"), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id))
                .tooltip(Tooltip.create(Component.translatable("screen.autarkia.rack.open")))
                .bounds(leftPos + RackMenu.CRATE_X + i * RackMenu.CRATE_STEP + 18, topPos + RackMenu.CRATE_Y, 16, 16).build());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        int load = menu.value(0);
        int rating = Math.max(1, menu.value(1));
        double ratio = load / (double) rating;
        int x = leftPos + BAR_X;
        int y = topPos + BAR_Y;
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + 7, 0xFF373737);
        g.fill(x, y, x + BAR_W, y + 6, 0xFF1A1A1A);
        int color = ratio <= 1.0 ? 0xFF4FBF4F : ratio <= 1.2 ? 0xFFE0C030 : 0xFFD03030;
        g.fill(x, y, x + (int) Math.round(BAR_W * Math.min(1.5, ratio) / 1.5), y + 6, color);
        int full = x + (int) Math.round(BAR_W / 1.5);
        g.fill(full, y - 2, full + 1, y + 8, 0xFFFFFFFF);
        int sel = menu.value(5);
        int sx = leftPos + RackMenu.CRATE_X + sel * RackMenu.CRATE_STEP - 1;
        g.fill(sx - 1, topPos + RackMenu.CRATE_Y - 2, sx + 19, topPos + RackMenu.CRATE_Y - 1, 0xFFF2D3A0);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        g.text(font, Component.translatable("screen.autarkia.rack.load", String.format(java.util.Locale.ROOT, "%,d", menu.value(0)),
            String.format(java.util.Locale.ROOT, "%,d", menu.value(1))), BAR_X, BAR_Y + 9, 0xFF404040, false);
        if (menu.value(2) == 3) {
            g.text(font, Component.translatable("screen.autarkia.rack.countdown", menu.value(3)), 100, BAR_Y + 9, 0xFFB01818, false);
        }
    }
}

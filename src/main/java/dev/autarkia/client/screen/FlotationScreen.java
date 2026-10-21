package dev.autarkia.client.screen;

import dev.autarkia.processing.FrothFlotationPlantBlockEntity;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the {@code froth_flotation_plant} menu (03 section 4.4): the machine layout plus the water tank gauge
 * (152, 18, 16 x 60, below the status label) with a mark every 1,000 mB and the tank amount as tooltip.
 */
public class FlotationScreen extends SlotScreen {
    private static final int TANK_X = 152;
    private static final int TANK_Y = 18;
    private static final int TANK_W = 16;
    private static final int TANK_H = 60;

    public FlotationScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = leftPos + TANK_X;
        int y = topPos + TANK_Y;
        int h = Math.min(TANK_H, (int) Math.ceil(TANK_H * (double) menu.value(5) / FrothFlotationPlantBlockEntity.TANK));
        g.fill(x, y + TANK_H - h, x + TANK_W, y + TANK_H, 0xFF3A6FD8);
        for (int mb = 1000; mb < FrothFlotationPlantBlockEntity.TANK; mb += 1000) {
            int my = y + TANK_H - Math.round(TANK_H * mb / (float) FrothFlotationPlantBlockEntity.TANK);
            g.fill(x, my, x + (mb % 4000 == 0 ? 8 : 4), my + 1, 0xFF1C3F73);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("jade.autarkia.flotation_water", String.format(Locale.ROOT, "%,d", menu.value(5)),
                String.format(Locale.ROOT, "%,d", FrothFlotationPlantBlockEntity.TANK)).getVisualOrderText()), mouseX, mouseY);
        }
    }
}

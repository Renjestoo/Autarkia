package dev.autarkia.client.screen;

import dev.autarkia.processing.BlastSmelterBlockEntity;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Screen of the {@code blast_smelter} menu (03 section 4.4): the machine layout plus the heat bar (24, 18, 6 x 52), orange at 1,000. */
public class BlastSmelterScreen extends SlotScreen {
    private static final int BAR_X = 24;
    private static final int BAR_Y = 18;
    private static final int BAR_H = 52;

    public BlastSmelterScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int heat = menu.value(5);
        int h = Math.round(BAR_H * Math.min(1f, heat / (float) BlastSmelterBlockEntity.MAX_HEAT));
        int x = leftPos + BAR_X;
        int y = topPos + BAR_Y;
        g.fill(x, y + BAR_H - h, x + 6, y + BAR_H, heat >= BlastSmelterBlockEntity.MAX_HEAT ? 0xFFFF8A20 : 0xFFB04020);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(BAR_X, BAR_Y, 6, BAR_H, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("jade.autarkia.smelter_heat", menu.value(5), BlastSmelterBlockEntity.MAX_HEAT)
                .getVisualOrderText()), mouseX, mouseY);
        }
    }
}

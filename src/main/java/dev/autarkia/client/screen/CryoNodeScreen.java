package dev.autarkia.client.screen;

import dev.autarkia.registry.SlotMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Cryo node screen (02 section 9.1): 4 coolant slots, the coolant units bar (right) with the seconds left, and the
 * temperature of up to 4 attached spans coloured by band (frost below 0.3, steam to 0.7, hiss above).
 */
public class CryoNodeScreen extends SlotScreen {
    private static final int BAR_X = 152;
    private static final int BAR_Y = 18;

    public CryoNodeScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int cap = menu.value(1);
        int h = cap <= 0 ? 0 : Math.min(52, (int) Math.ceil(52.0 * menu.value(0) / cap));
        g.fill(leftPos + BAR_X, topPos + BAR_Y + 52 - h, leftPos + BAR_X + 12, topPos + BAR_Y + 52, 0xFF7FD3FF);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        int spans = Math.min(4, menu.value(3));
        if (spans == 0) {
            g.text(font, Component.translatable("screen.autarkia.cryo.no_spans"), 84, 20, 0xFF404040, false);
        }
        for (int k = 0; k < spans; k++) {
            int milli = menu.value(4 + k);
            int color = milli < 300 ? 0xFF1F5F99 : milli < 700 ? 0xFF8D6E00 : 0xFFB71C1C;
            g.text(font, Component.translatable("screen.autarkia.cryo.span", k + 1, Math.round(milli / 10.0)), 84, 20 + 10 * k, color, false);
        }
        g.text(font, Component.translatable("screen.autarkia.cryo.coolant", menu.value(2) / 20), 8, 58, menu.value(0) > 0 ? 0xFF404040 : 0xFFB71C1C,
            false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(BAR_X, BAR_Y, 12, 52, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("tooltip.autarkia.coolant", menu.value(0), menu.value(1)).getVisualOrderText()),
                mouseX, mouseY);
        }
    }
}

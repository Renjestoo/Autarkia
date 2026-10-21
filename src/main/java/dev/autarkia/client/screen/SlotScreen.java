package dev.autarkia.client.screen;

import dev.autarkia.Autarkia;
import dev.autarkia.processing.MachineStatus;
import dev.autarkia.registry.GuiLayout;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of every {@link SlotMenu}: the generated background, then the energy bar, progress arrow and flame filled
 * from the widget atlas, and the machine status text under the title (03 section 4.4).
 */
public class SlotScreen extends AbstractContainerScreen<SlotMenu> {
    private static final Identifier WIDGETS = Autarkia.id("textures/gui/widgets.png");
    private final Identifier background;
    private final GuiLayout layout;

    public SlotScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, menu.layout.width(), menu.layout.height());
        this.layout = menu.layout;
        this.background = Autarkia.id("textures/gui/" + layout.name() + ".png");
        this.inventoryLabelY = layout.invY() - 11;
    }

    private boolean machine() {
        return layout.data() >= 5;
    }

    /** Return true when the screen shows the machine status line (subclasses with their own labels return false). */
    protected boolean showsStatus() {
        return machine();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = leftPos;
        int y = topPos;
        g.blit(RenderPipelines.GUI_TEXTURED, background, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        if (!machine()) {
            return;
        }
        int progress = menu.value(0);
        int duration = menu.value(1);
        if (layout.arrowX() >= 0 && duration > 0 && progress > 0) {
            int w = Math.min(24, (int) Math.ceil(24.0 * progress / duration));
            g.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, x + layout.arrowX(), y + layout.arrowY(), 0, 0, w, 17, 64, 64);
        }
        int cap = menu.value(3);
        if (layout.energyX() >= 0 && cap > 0) {
            int h = Math.min(52, (int) Math.ceil(52.0 * menu.value(2) / cap));
            g.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, x + layout.energyX(), y + layout.energyY() + 52 - h, 24, 52 - h, 12, h, 64, 64);
        }
        if (layout.flameX() >= 0 && menu.value(5) > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, x + layout.flameX(), y + layout.flameY(), 48, 0, 14, 14, 64, 64);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        if (showsStatus()) {
            MachineStatus.State st = MachineStatus.State.values()[Math.max(0, Math.min(MachineStatus.State.values().length - 1, menu.value(4)))];
            g.text(font, Component.translatable(st.key()), titleLabelX, titleLabelY + 10, 0xFF000000 | darker(st.color), false);
            if (layout.flameX() >= 0) {
                String heat = switch (menu.value(5)) {
                    case 1 -> "heat.autarkia.campfire";
                    case 2 -> "heat.autarkia.furnace";
                    case 3 -> "heat.autarkia.lava";
                    default -> "heat.autarkia.none";
                };
                g.text(font, Component.translatable(heat), layout.flameX() + 18, layout.flameY() + 3, 0xFF404040, false);
            }
        }
    }

    private static int darker(int rgb) {
        int r = (rgb >> 16 & 0xFF) * 3 / 5;
        int gg = (rgb >> 8 & 0xFF) * 3 / 5;
        int b = (rgb & 0xFF) * 3 / 5;
        return r << 16 | gg << 8 | b;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (machine() && layout.energyX() >= 0 && isHovering(layout.energyX(), layout.energyY(), 12, 52, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("tooltip.autarkia.energy", menu.value(2), menu.value(3)).getVisualOrderText()),
                mouseX, mouseY);
        }
    }
}

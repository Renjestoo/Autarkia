package dev.autarkia.client.screen;

import dev.autarkia.energy.SingularityCoreBlockEntity;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Singularity Core screen (02 section 9.1): buffer bar, state, base output, ignition progress bar, damage points paid,
 * explosions absorbed and FE paid in the last 1,200 ticks; Ignite (menu button 0, while OFF) and Stop (menu button 1).
 */
public class SingularityCoreScreen extends SlotScreen {
    private static final int BAR_X = 26;
    private static final int BAR_Y = 80;
    private static final int BAR_W = 142;
    private Button ignite;
    private Button stop;

    public SingularityCoreScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        ignite = Button.builder(Component.translatable("screen.autarkia.singularity.ignite"),
            b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)).bounds(leftPos + 116, topPos + 16, 52, 14).build();
        stop = Button.builder(Component.translatable("screen.autarkia.singularity.stop"),
            b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1)).bounds(leftPos + 116, topPos + 32, 52, 14).build();
        addRenderableWidget(ignite);
        addRenderableWidget(stop);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ignite.active = menu.value(10) == 1;
        stop.active = menu.value(10) == 0;
    }

    private SingularityCoreBlockEntity.State state() {
        SingularityCoreBlockEntity.State[] all = SingularityCoreBlockEntity.State.values();
        return all[Math.clamp(menu.value(4), 0, all.length - 1)];
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int w = (int) Math.round((double) BAR_W * menu.value(0) / Math.max(1, menu.value(1)));
        g.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + w, topPos + BAR_Y + 6, 0xFF7FFFF0);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        SingularityCoreBlockEntity.State st = state();
        int color = switch (st) {
            case OFF -> 0xFF404040;
            case IGNITING -> 0xFF8D6E00;
            case ONLINE -> 0xFF0F8C80;
        };
        g.text(font, Component.translatable(st.key()), 26, 20, color, false);
        g.text(font, Component.translatable("screen.autarkia.singularity.base", menu.value(5)), 26, 32, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.singularity.points", menu.value(6)), 26, 48, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.singularity.explosions", menu.value(7)), 26, 58, 0xFF404040, false);
        long paid = ((long) menu.value(8) << 31) | menu.value(9);
        g.text(font, Component.translatable("screen.autarkia.singularity.paid", paid), 26, 68, 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(BAR_X, BAR_Y, BAR_W, 6, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("screen.autarkia.singularity.ignition", menu.value(0) * 100L / Math.max(1, menu.value(1)))
                .getVisualOrderText()), mouseX, mouseY);
        }
    }
}

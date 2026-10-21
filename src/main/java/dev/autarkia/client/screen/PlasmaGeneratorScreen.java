package dev.autarkia.client.screen;

import dev.autarkia.energy.PlasmaGeneratorBlockEntity;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Plasma generator screen (02 section 9.1): fuel slot (stack 16), the buffer bar, the state text and output, the
 * ignition button (active while cold with fuel; menu button 0) and the ignition progress bar.
 */
public class PlasmaGeneratorScreen extends SlotScreen {
    private static final int BAR_X = 30;
    private static final int BAR_Y = 62;
    private static final int BAR_W = 138;
    private Button ignite;

    public PlasmaGeneratorScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        ignite = Button.builder(Component.translatable("screen.autarkia.plasma.ignite"),
            b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)).bounds(leftPos + 116, topPos + 16, 52, 14).build();
        addRenderableWidget(ignite);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ignite.active = menu.value(7) == 1;
    }

    private PlasmaGeneratorBlockEntity.State state() {
        PlasmaGeneratorBlockEntity.State[] all = PlasmaGeneratorBlockEntity.State.values();
        return all[Math.clamp(menu.value(4), 0, all.length - 1)];
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int w = (int) Math.round((double) BAR_W * menu.value(0) / Math.max(1, menu.value(1)));
        g.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + w, topPos + BAR_Y + 6, 0xFFFF5CF0);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        PlasmaGeneratorBlockEntity.State st = state();
        int color = switch (st) {
            case COLD -> 0xFF404040;
            case IGNITING -> 0xFF8D6E00;
            case HOT_IDLE -> 0xFFB71C1C;
            default -> 0xFF8E24AA;
        };
        g.text(font, Component.translatable(st.key()), 52, 20, color, false);
        g.text(font, Component.translatable("jade.autarkia.output", menu.value(5)), 52, 32, 0xFF404040, false);
        Component line = st == PlasmaGeneratorBlockEntity.State.HOT_IDLE ? Component.translatable("screen.autarkia.plasma.hot", menu.value(6) / 20)
            : Component.translatable("screen.autarkia.plasma.ignition", menu.value(0) * 100L / Math.max(1, menu.value(1)));
        g.text(font, line, 30, 50, 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(BAR_X, BAR_Y, BAR_W, 6, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("tooltip.autarkia.energy", menu.value(0), menu.value(1)).getVisualOrderText()),
                mouseX, mouseY);
        }
    }
}

package dev.autarkia.client.screen;

import dev.autarkia.Autarkia;
import dev.autarkia.logistics.CapsuleNetwork;
import dev.autarkia.logistics.CapsuleStationBlockEntity;
import dev.autarkia.logistics.CapsuleStationMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the {@code capsule_station} (04 section 4.1): top row mode buttons (the active mode is greyed) and the
 * priority spinner 0 to 9; middle the input (left) and output (right) buffers; right column the 3 x 3 ghost filter
 * with the whitelist and match-components toggles; bottom left the network size, capsules in flight and state.
 */
public class CapsuleStationScreen extends AbstractContainerScreen<CapsuleStationMenu> {
    private static final int[][] MODE_BUTTONS = {{7, 36}, {45, 46}, {93, 36}};
    private final Identifier background;
    private int seen = -1;

    public CapsuleStationScreen(CapsuleStationMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, menu.layout.width(), menu.layout.height());
        this.background = Autarkia.id("textures/gui/" + menu.layout.name() + ".png");
        this.inventoryLabelY = menu.layout.invY() - 11;
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
        CapsuleStationBlockEntity.Mode[] modes = CapsuleStationBlockEntity.Mode.values();
        for (int i = 0; i < modes.length; i++) {
            int id = i;
            Button b = Button.builder(Component.translatable(modes[i].key()), w -> button(id))
                .bounds(x + MODE_BUTTONS[i][0], y + 16, MODE_BUTTONS[i][1], 13).build();
            b.active = menu.value(0) != i;
            addRenderableWidget(b);
        }
        Button down = Button.builder(Component.literal("-"), w -> button(3)).bounds(x + 132, y + 16, 12, 13).build();
        down.active = menu.value(1) > 0;
        addRenderableWidget(down);
        Button up = Button.builder(Component.literal("+"), w -> button(4)).bounds(x + 158, y + 16, 12, 13).build();
        up.active = menu.value(1) < 9;
        addRenderableWidget(up);
        addRenderableWidget(Button.builder(Component.translatable(menu.value(2) == 1 ? "screen.autarkia.capsule_station.whitelist"
            : "screen.autarkia.capsule_station.blacklist"), w -> button(5)).bounds(x + 118, y + 99, 54, 12).build());
        addRenderableWidget(Button.builder(Component.translatable(menu.value(3) == 1 ? "screen.autarkia.capsule_station.exact"
            : "screen.autarkia.capsule_station.item_only"), w -> button(6)).bounds(x + 118, y + 113, 54, 12).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int now = menu.value(0) | menu.value(1) << 2 | menu.value(2) << 6 | menu.value(3) << 7;
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

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        String p = Integer.toString(menu.value(1));
        g.text(font, Component.literal(p), 151 - font.width(p) / 2, 19, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.capsule_station.input"), 7, 33, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.capsule_station.output"), 63, 33, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.capsule_station.filter"), 119, 33, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.capsule_station.tubes", menu.value(4)), 7, 100, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.capsule_station.stations", menu.value(5)), 7, 109, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.capsule_station.capsules", menu.value(6)), 7, 118, 0xFF404040, false);
        CapsuleNetwork.State[] states = CapsuleNetwork.State.values();
        CapsuleNetwork.State st = states[Math.clamp(menu.value(7), 0, states.length - 1)];
        int color = st == CapsuleNetwork.State.RUNNING ? 0xFF2E7D32 : st == CapsuleNetwork.State.CLOGGED || st == CapsuleNetwork.State.TOO_LARGE ? 0xFFB71C1C
            : 0xFF8D6E00;
        g.text(font, Component.translatable(st.key()), 7, 127, color, false);
    }
}

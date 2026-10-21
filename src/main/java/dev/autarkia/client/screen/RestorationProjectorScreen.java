package dev.autarkia.client.screen;

import dev.autarkia.colony.RestorationProjectorBlockEntity;
import dev.autarkia.registry.SlotMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Restoration projector screen (05 section 8.5, M9 decision G11): the state line, the box size and offset with - and +
 * buttons (a click with shift held steps by 8), the Snapshot button, the missing count and the FE bar.
 */
public class RestorationProjectorScreen extends SlotScreen {
    private static final int[] ROWS = {40, 66};
    private static final String[] AXES = {"X", "Y", "Z"};

    public RestorationProjectorScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        for (int row = 0; row < 2; row++) {
            for (int axis = 0; axis < 3; axis++) {
                int x0 = leftPos + 8 + axis * 46;
                int down = row * 6 + axis * 2;
                addRenderableWidget(Button.builder(Component.literal("-"), b -> click(down)).bounds(x0, topPos + ROWS[row], 12, 12).build());
                addRenderableWidget(Button.builder(Component.literal("+"), b -> click(down + 1)).bounds(x0 + 30, topPos + ROWS[row], 12, 12).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.restoration.snapshot"),
            b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RestorationProjectorBlockEntity.BUTTON_SNAPSHOT))
            .bounds(leftPos + 96, topPos + 80, 52, 14).build());
    }

    private void click(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, Minecraft.getInstance().hasShiftDown() ? id + RestorationProjectorBlockEntity.COARSE : id);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        RestorationProjectorBlockEntity.Status[] all = RestorationProjectorBlockEntity.Status.values();
        RestorationProjectorBlockEntity.Status st = all[Math.clamp(menu.value(4), 0, all.length - 1)];
        Component line = st == RestorationProjectorBlockEntity.Status.SNAPSHOT
            ? Component.translatable("screen.autarkia.restoration.reading", menu.value(0) * 100L / Math.max(1, menu.value(1)))
            : Component.translatable("jade.autarkia.restoration.state." + st.name().toLowerCase(java.util.Locale.ROOT));
        int color = switch (st) {
            case RESTORING, SNAPSHOT -> 0xFF1E6FA8;
            case NO_POWER, NO_MATTER, OUTSIDE, DISABLED -> 0xFFB71C1C;
            default -> 0xFF404040;
        };
        g.text(font, line, 8, 17, color, false);
        g.text(font, Component.translatable("screen.autarkia.restoration.size"), 8, 30, 0xFF404040, false);
        g.text(font, Component.translatable("screen.autarkia.restoration.offset"), 8, 56, 0xFF404040, false);
        for (int row = 0; row < 2; row++) {
            for (int axis = 0; axis < 3; axis++) {
                String v = AXES[axis] + menu.value(5 + row * 3 + axis);
                g.text(font, Component.literal(v), 8 + axis * 46 + 21 - font.width(v) / 2, ROWS[row] + 2, 0xFF202020, false);
            }
        }
        g.text(font, Component.translatable("jade.autarkia.restoration.missing", menu.value(11)), 8, 84, 0xFF404040, false);
    }
}

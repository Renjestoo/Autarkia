package dev.autarkia.client.screen;

import dev.autarkia.colony.SeedMatrixBlockEntity;
import dev.autarkia.registry.SlotMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Seed Matrix screen (05 section 8.4, G9): one row per card slot with its layout (x and z offset of the rotated box, the
 * rotation in degrees) and +/- buttons (sneak for steps of 8) and a rotate button; under the rows the footprint size and
 * block count, the state text, and the pack progress bar with the matter and power share; the Pack button sits on the
 * title row. Buttons of empty rows and every layout button during a pack are hidden.
 */
public class SeedMatrixScreen extends SlotScreen {
    private static final int ROW_Y = 17;
    private static final int ROW_H = 18;
    private static final int BAR_X = 8;
    private static final int BAR_Y = 184;
    private static final int BAR_W = 138;
    private final List<List<Button>> rows = new ArrayList<>();
    private Button pack;

    public SeedMatrixScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    private void click(int id) {
        int send = id < SeedMatrixBlockEntity.BUTTON_PACK && minecraft.hasShiftDown() ? id + SeedMatrixBlockEntity.BUTTON_STEP8 : id;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, send);
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        String[] labels = {"-", "+", "-", "+", "R"};
        int[] xs = {54, 65, 106, 117, 152};
        for (int i = 0; i < SeedMatrixBlockEntity.CARDS; i++) {
            List<Button> row = new ArrayList<>();
            for (int k = 0; k < 5; k++) {
                int id = i * 5 + k;
                Button b = Button.builder(Component.literal(labels[k]), x -> click(id))
                    .bounds(leftPos + xs[k], topPos + ROW_Y + ROW_H * i + 3, k == 4 ? 16 : 10, 10).build();
                row.add(addRenderableWidget(b));
            }
            rows.add(row);
        }
        pack = addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.seed_matrix.pack"), x -> click(SeedMatrixBlockEntity.BUTTON_PACK))
            .bounds(leftPos + 126, topPos + 3, 44, 12).build());
    }

    private SeedMatrixBlockEntity.Status state() {
        SeedMatrixBlockEntity.Status[] all = SeedMatrixBlockEntity.Status.values();
        return all[Math.clamp(menu.value(3), 0, all.length - 1)];
    }

    private boolean packing() {
        SeedMatrixBlockEntity.Status s = state();
        return s == SeedMatrixBlockEntity.Status.RUNNING || s == SeedMatrixBlockEntity.Status.NO_POWER || s == SeedMatrixBlockEntity.Status.NO_MATTER;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean busy = packing();
        for (int i = 0; i < rows.size(); i++) {
            boolean used = !menu.getSlot(i).getItem().isEmpty();
            rows.get(i).forEach(b -> b.visible = used && !busy);
        }
        SeedMatrixBlockEntity.Status s = state();
        pack.active = s == SeedMatrixBlockEntity.Status.READY || s == SeedMatrixBlockEntity.Status.MISSING_ITEMS;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int w = (int) Math.round((double) BAR_W * menu.value(0) / Math.max(1, menu.value(1)));
        g.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + w, topPos + BAR_Y + 5, 0xFF7FFFF0);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        for (int i = 0; i < SeedMatrixBlockEntity.CARDS; i++) {
            if (menu.getSlot(i).getItem().isEmpty()) {
                continue;
            }
            int y = ROW_Y + ROW_H * i + 4;
            int base = 12 + 3 * i;
            g.text(font, Component.literal("x " + menu.value(base)), 28, y, 0xFF404040, false);
            g.text(font, Component.literal("z " + menu.value(base + 1)), 80, y, 0xFF404040, false);
            g.text(font, Component.literal(menu.value(base + 2) * 90 + "°"), 131, y, 0xFF404040, false);
        }
        int blocks = menu.value(8) * 10000 + menu.value(9);
        g.text(font, Component.translatable("screen.autarkia.seed_matrix.size", menu.value(5), menu.value(6), menu.value(7), blocks), 8, 162, 0xFF404040,
            false);
        SeedMatrixBlockEntity.Status s = state();
        int color = s == SeedMatrixBlockEntity.Status.READY || s == SeedMatrixBlockEntity.Status.RUNNING ? 0xFF1B6E3A : 0xFF8B1E1E;
        g.text(font, Component.translatable(s.key()), 8, 172, color, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(BAR_X, BAR_Y, BAR_W, 5, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("screen.autarkia.seed_matrix.progress", menu.value(0) * 100 / Math.max(1,
                menu.value(1)), menu.value(4), menu.value(2)).getVisualOrderText()), mouseX, mouseY);
        }
    }
}

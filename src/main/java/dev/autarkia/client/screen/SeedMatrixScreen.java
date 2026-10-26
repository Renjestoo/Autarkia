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
 * Seed Matrix screen (05 section 8.4, G9): the 8 card slots in two columns of 4 under an X / Z / Rot header, each with
 * its layout (x and z offset of the rotated box) between - and + buttons (sneak for steps of 8) and a rotate button that
 * shows the rotation in degrees; under the rows the footprint size and block count, the state line and the pack progress
 * bar with the matter and power share; the Pack button sits on the title row. Buttons of empty rows and every layout button
 * during a pack are hidden. The panel is 304 x 220 so it fits the 320 x 240 GUI pixels of every GUI scale.
 */
public class SeedMatrixScreen extends SlotScreen {
    private static final int ROW_Y = 26;
    private static final int ROW_H = 18;
    private static final int COL_W = 146;
    private static final int PER_COL = 4;
    private static final int BAR_X = 8;
    private static final int BAR_Y = 121;
    private static final int BAR_H = 3;
    private static final int BAR_W = 260;
    private static final int[] BUTTON_X = {20, 52, 66, 98, 112};
    private static final int[] BUTTON_W = {10, 10, 10, 10, 28};
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

    private static int colX(int card) {
        return 8 + COL_W * (card / PER_COL);
    }

    private static int rowY(int card) {
        return ROW_Y + ROW_H * (card % PER_COL);
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        String[] labels = {"-", "+", "-", "+", ""};
        for (int i = 0; i < SeedMatrixBlockEntity.CARDS; i++) {
            List<Button> row = new ArrayList<>();
            for (int k = 0; k < 5; k++) {
                int id = i * 5 + k;
                Button b = Button.builder(Component.literal(labels[k]), x -> click(id))
                    .bounds(leftPos + colX(i) + BUTTON_X[k], topPos + rowY(i) + 3, BUTTON_W[k], 10).build();
                row.add(addRenderableWidget(b));
            }
            rows.add(row);
        }
        pack = addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.seed_matrix.pack"), x -> click(SeedMatrixBlockEntity.BUTTON_PACK))
            .bounds(leftPos + imageWidth - 52, topPos + 4, 44, 11).build());
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
            rows.get(i).get(4).setMessage(Component.literal(menu.value(12 + 3 * i + 2) * 90 + "°"));
        }
        SeedMatrixBlockEntity.Status s = state();
        pack.active = s == SeedMatrixBlockEntity.Status.READY || s == SeedMatrixBlockEntity.Status.MISSING_ITEMS;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int w = (int) Math.round((double) BAR_W * menu.value(0) / Math.max(1, menu.value(1)));
        g.fill(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + w, topPos + BAR_Y + BAR_H, 0xFF7FFFF0);
    }

    private void centered(GuiGraphicsExtractor g, String s, int cx, int y) {
        g.text(font, Component.literal(s), cx - font.width(s) / 2, y, 0xFF404040, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        for (int c = 0; c < SeedMatrixBlockEntity.CARDS / PER_COL; c++) {
            int x0 = 8 + COL_W * c;
            centered(g, "X", x0 + 41, 16);
            centered(g, "Z", x0 + 87, 16);
            centered(g, "Rot", x0 + 126, 16);
        }
        for (int i = 0; i < SeedMatrixBlockEntity.CARDS; i++) {
            if (menu.getSlot(i).getItem().isEmpty()) {
                continue;
            }
            int base = 12 + 3 * i;
            centered(g, Integer.toString(menu.value(base)), colX(i) + 41, rowY(i) + 4);
            centered(g, Integer.toString(menu.value(base + 1)), colX(i) + 87, rowY(i) + 4);
        }
        int blocks = menu.value(8) * 10000 + menu.value(9);
        g.text(font, fit(Component.translatable("screen.autarkia.seed_matrix.size", menu.value(5), menu.value(6), menu.value(7), blocks), BAR_W), 8, 101,
            0xFF404040, false);
        SeedMatrixBlockEntity.Status s = state();
        int color = s == SeedMatrixBlockEntity.Status.READY || s == SeedMatrixBlockEntity.Status.RUNNING ? 0xFF1B6E3A : 0xFF8B1E1E;
        g.text(font, fit(Component.translatable(s.key()), BAR_W), 8, 111, color, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(BAR_X, BAR_Y - 1, BAR_W, BAR_H + 2, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("screen.autarkia.seed_matrix.progress", menu.value(0) * 100 / Math.max(1,
                menu.value(1)), menu.value(4), menu.value(2)).getVisualOrderText()), mouseX, mouseY);
        }
    }
}

package dev.autarkia.client.screen;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.PatternLoomBlockEntity;
import dev.autarkia.colony.PatternLoomMenu;
import dev.autarkia.network.ColonyActionPayload;
import dev.autarkia.registry.AutarkiaComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The pattern loom screen (05 section 5.1): box offset and size with - and + per axis, the Protect, Library and Strict
 * air toggles, the card slot, the Pattern name and Record, and a state line: the scan progress, why recording is not
 * possible (no T2+ colony domain, no card, a used card, a box above the tier max), or ready with the max size. Record
 * is inactive unless the state is ready.
 */
public class PatternLoomScreen extends AbstractContainerScreen<PatternLoomMenu> {
    private static final Identifier BACKGROUND = Autarkia.id("textures/gui/pattern_loom.png");
    private static final String[] AXES = {"X", "Y", "Z"};
    private static final int COL_X = 44;
    private static final int COL_W = 42;
    private static final int BTN_W = 10;
    private static final int GROUP_W = 40;
    private static final int ROW_Y = 16;
    private static final int ROW_H = 13;
    private static final int NAME_Y = 59;
    private static final int STATE_Y = 78;
    private EditBox name;
    private Button record;
    private int seen = -1;

    public PatternLoomScreen(PatternLoomMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, PatternLoomMenu.INV_Y + 82);
        this.inventoryLabelY = PatternLoomMenu.INV_Y - 11;
    }

    private void button(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void init() {
        super.init();
        String keep = name == null ? "" : name.getValue();
        int x = leftPos;
        int y = topPos;
        for (int row = 0; row < 2; row++) {
            for (int a = 0; a < 3; a++) {
                int idx = row * 3 + a;
                int bx = x + COL_X + a * COL_W;
                int by = y + ROW_Y + row * ROW_H;
                Component axis = Component.translatable(row == 0 ? "screen.autarkia.loom.offset" : "screen.autarkia.loom.size").append(" " + AXES[a]);
                addRenderableWidget(Button.builder(Component.literal("-"), b -> button(idx)).tooltip(Tooltip.create(axis)).bounds(bx, by, BTN_W, 12).build());
                addRenderableWidget(Button.builder(Component.literal("+"), b -> button(idx + 6)).tooltip(Tooltip.create(axis))
                    .bounds(bx + GROUP_W - BTN_W, by, BTN_W, 12).build());
            }
        }
        String[] toggles = {"protect", "library", "strict"};
        for (int t = 0; t < 3; t++) {
            int id = 12 + t;
            boolean on = (menu.value(6) & flag(t)) != 0;
            addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.loom." + toggles[t]).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                    b -> button(id))
                .tooltip(Tooltip.create(Component.translatable(on ? "screen.autarkia.loom.on" : "screen.autarkia.loom.off")))
                .bounds(x + 8 + t * 54, y + 43, 52, 12).build());
        }
        name = new EditBox(font, x + 30, y + NAME_Y, 76, 13, Component.translatable("screen.autarkia.loom.name"));
        name.setMaxLength(32);
        name.setValue(keep);
        name.setHint(Component.translatable("screen.autarkia.loom.name"));
        addRenderableWidget(name);
        record = addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.loom.record"), b -> ClientPacketDistributor.sendToServer(
            new ColonyActionPayload(menu.colonyId, "pattern_record", List.of(Integer.toString(menu.pos.getX()), Integer.toString(menu.pos.getY()),
                Integer.toString(menu.pos.getZ()), name.getValue())))).bounds(x + 110, y + NAME_Y, 58, 13).build());
        record.active = ready();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.value(6) != seen && !name.isFocused()) {
            seen = menu.value(6);
            rebuildWidgets();
        }
        record.active = ready();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent key) {
        if (name.isFocused() && key.key() != 256) {
            return name.keyPressed(key) || name.canConsumeInput() || super.keyPressed(key);
        }
        return super.keyPressed(key);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    private boolean tooBig() {
        int max = menu.value(8);
        return menu.value(3) > max || menu.value(4) > max || menu.value(5) > max;
    }

    private String problem() {
        ItemStack card = menu.getSlot(0).getItem();
        if (menu.value(7) >= 0) {
            return "scanning";
        }
        if (menu.value(8) <= 0) {
            return "outside";
        }
        if (card.isEmpty()) {
            return "no_card";
        }
        if (card.has(AutarkiaComponents.PATTERN_REF.get())) {
            return "used";
        }
        return tooBig() ? "too_big" : "";
    }

    private boolean ready() {
        return problem().isEmpty();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        for (int row = 0; row < 2; row++) {
            g.text(font, Component.translatable(row == 0 ? "screen.autarkia.loom.offset" : "screen.autarkia.loom.size"), 8, ROW_Y + 2 + row * ROW_H,
                0xFF404040, false);
            for (int a = 0; a < 3; a++) {
                String value = Integer.toString(menu.value(row * 3 + a));
                g.text(font, Component.literal(value), COL_X + a * COL_W + GROUP_W / 2 - font.width(value) / 2, ROW_Y + 2 + row * ROW_H, 0xFF404040, false);
            }
        }
        String p = problem();
        Component state = switch (p) {
            case "scanning" -> Component.translatable("screen.autarkia.loom.scanning", menu.value(7) / 10);
            case "too_big" -> Component.translatable("screen.autarkia.loom.too_big", menu.value(8));
            case "" -> Component.translatable("screen.autarkia.loom.ready", menu.value(8));
            default -> Component.translatable("screen.autarkia.loom." + p);
        };
        int color = p.equals("scanning") ? 0xFF2050A0 : p.isEmpty() ? 0xFF1B6E3A : 0xFFB01818;
        g.text(font, SlotScreen.fit(font, state, imageWidth - 16), 8, STATE_Y, color, false);
    }

    private static int flag(int t) {
        return t == 0 ? PatternLoomBlockEntity.PROTECT : t == 1 ? PatternLoomBlockEntity.LIBRARY : PatternLoomBlockEntity.STRICT_AIR;
    }
}

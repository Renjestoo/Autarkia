package dev.autarkia.client.screen;

import dev.autarkia.client.ClientRecipes;
import dev.autarkia.processing.MachineRecipe;
import dev.autarkia.processing.MatterCompilerBlockEntity;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.storage.MatterBufferBlockEntity;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Matter compiler screen (03 section 4.4): feed slot (38, 35), MU bar (24, 18, 6 x 52), the target ghost slot (79, 17),
 * arrow (79, 35), 2 x 2 outputs from (116, 26) and the energy bar. A click on the ghost slot with an item on the cursor
 * targets the compiling recipe of that item; with an empty cursor a left click picks the next and a right click the
 * previous recipe, the mouse wheel scrolls them, and the x button clears the target.
 */
public class CompilerScreen extends SlotScreen {
    private static final int GHOST_X = 79;
    private static final int GHOST_Y = 17;
    private static final int BAR_X = 24;
    private static final int BAR_Y = 18;

    public CompilerScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("x"), b -> click(2))
            .tooltip(Tooltip.create(Component.translatable("screen.autarkia.matter_compiler.clear"))).bounds(leftPos + 98, topPos + 19, 12, 12).build());
    }

    private void click(int button) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    private List<RecipeHolder<MachineRecipe>> list() {
        return MatterCompilerBlockEntity.targets(ClientRecipes.map());
    }

    private ItemStack target() {
        int idx = menu.value(6) - 1;
        List<RecipeHolder<MachineRecipe>> list = list();
        return idx < 0 || idx >= list.size() ? ItemStack.EMPTY : list.get(idx).value().results().getFirst().item().create();
    }

    private long targetMatter() {
        int idx = menu.value(6) - 1;
        List<RecipeHolder<MachineRecipe>> list = list();
        return idx < 0 || idx >= list.size() ? 0 : list.get(idx).value().matter();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (isHovering(GHOST_X, GHOST_Y, 16, 16, e.x(), e.y())) {
            ItemStack carried = menu.getCarried();
            if (carried.isEmpty()) {
                click(e.button() == 1 ? 1 : 0);
                return true;
            }
            List<RecipeHolder<MachineRecipe>> list = list();
            for (int i = 0; i < list.size(); i++) {
                if (ItemStack.isSameItem(list.get(i).value().results().getFirst().item().create(), carried)) {
                    click(MatterCompilerBlockEntity.TARGET_BUTTON + i);
                    return true;
                }
            }
            return true;
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (isHovering(GHOST_X, GHOST_Y, 16, 16, x, y) && dy != 0) {
            click(dy > 0 ? 1 : 0);
            return true;
        }
        return super.mouseScrolled(x, y, dx, dy);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int bx = leftPos + BAR_X;
        int by = topPos + BAR_Y;
        int h = (int) Math.min(52, Math.ceil(52.0 * menu.value(5) / MatterCompilerBlockEntity.MU_CAPACITY));
        long need = targetMatter();
        g.fill(bx, by + 52 - h, bx + 6, by + 52, 0xFF8A4FFF);
        if (need > 0 && need <= MatterCompilerBlockEntity.MU_CAPACITY) {
            int mark = by + 52 - (int) Math.round(52.0 * need / MatterCompilerBlockEntity.MU_CAPACITY);
            g.fill(bx - 1, mark, bx + 7, mark + 1, 0xFFF5D76E);
        }
        ItemStack t = target();
        if (!t.isEmpty()) {
            int x = leftPos + GHOST_X;
            int y = topPos + GHOST_Y;
            g.fakeItem(t, x, y);
            g.fill(x, y, x + 16, y + 16, 0x60FFFFFF);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(BAR_X, BAR_Y, 6, 52, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("screen.autarkia.matter_compiler.matter", MatterBufferBlockEntity.fmt(menu.value(5)),
                MatterBufferBlockEntity.fmt(MatterCompilerBlockEntity.MU_CAPACITY)).getVisualOrderText()), mouseX, mouseY);
        } else if (isHovering(GHOST_X, GHOST_Y, 16, 16, mouseX, mouseY)) {
            ItemStack t = target();
            Component line = t.isEmpty() ? Component.translatable("screen.autarkia.matter_compiler.none")
                : Component.translatable("screen.autarkia.matter_compiler.target", t.getHoverName(), MatterBufferBlockEntity.fmt(targetMatter()),
                    String.format(Locale.ROOT, "%.1f", MatterCompilerBlockEntity.ticks(targetMatter()) / 20.0));
            g.setTooltipForNextFrame(font, List.of(line.getVisualOrderText(), Component.translatable("screen.autarkia.matter_compiler.hint")
                .getVisualOrderText()), mouseX, mouseY);
        }
    }
}

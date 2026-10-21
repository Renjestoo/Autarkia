package dev.autarkia.client.screen;

import dev.autarkia.client.ClientRecipes;
import dev.autarkia.processing.FabricatorBlockEntity;
import dev.autarkia.processing.MachineRecipe;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Fabricator screen (03 section 4.4): 3 x 3 buffer, the target recipe as a ghost item at (124, 17) with its
 * ingredients in the tooltip, recipe list buttons (previous, next, clear) under it, arrow and output.
 */
public class FabricatorScreen extends SlotScreen {
    private static final int GHOST_X = 124;
    private static final int GHOST_Y = 17;

    public FabricatorScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + GHOST_X - 10;
        int y = topPos + GHOST_Y + 20;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> click(1)).tooltip(Tooltip.create(Component.translatable("screen.autarkia.fabricator.prev")))
            .bounds(x, y, 12, 12).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> click(0)).tooltip(Tooltip.create(Component.translatable("screen.autarkia.fabricator.next")))
            .bounds(x + 13, y, 12, 12).build());
        addRenderableWidget(Button.builder(Component.literal("x"), b -> click(2)).tooltip(Tooltip.create(Component.translatable("screen.autarkia.fabricator.clear")))
            .bounds(x + 26, y, 12, 12).build());
    }

    private void click(int button) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    private ItemStack target() {
        int idx = menu.value(5) - 1;
        List<RecipeHolder<MachineRecipe>> list = FabricatorBlockEntity.targets(ClientRecipes.map());
        if (idx < 0 || idx >= list.size()) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> results = list.get(idx).value().allResults();
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = leftPos + GHOST_X;
        int y = topPos + GHOST_Y;
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
        ItemStack t = target();
        if (!t.isEmpty()) {
            g.fakeItem(t, x, y);
            g.fill(x, y, x + 16, y + 16, 0x60FFFFFF);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (isHovering(GHOST_X, GHOST_Y, 16, 16, mouseX, mouseY)) {
            ItemStack t = target();
            g.setTooltipForNextFrame(font, List.of((t.isEmpty() ? Component.translatable("screen.autarkia.fabricator.none")
                : Component.translatable("screen.autarkia.fabricator.target", t.getHoverName())).getVisualOrderText()), mouseX, mouseY);
        }
    }
}

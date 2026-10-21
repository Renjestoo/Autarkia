package dev.autarkia.processing;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** The input slots of a machine as a recipe input. */
public record MachineInput(List<ItemStack> items) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }
}

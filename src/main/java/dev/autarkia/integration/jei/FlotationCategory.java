package dev.autarkia.integration.jei;

import dev.autarkia.processing.MachineRecipe;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

/**
 * JEI category {@code flotation} (03 section 7.1), 138 x 40: the crushed ore on the left, the arrow over the recipe
 * time with the water amount under it, the 3 outputs on the right with the chance percents, FE/t and seconds at the bottom.
 */
public class FlotationCategory extends MachineCategory {
    private static final int TIME = 200;
    private static final int ENERGY = 768;

    public FlotationCategory(IRecipeHolderType<MachineRecipe> type, IGuiHelper gui, List<Block> stations) {
        super(type, Component.translatable("jei.autarkia.category.flotation"), gui, stations, TIME, ENERGY, 138, 40);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder extras, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        MachineRecipe r = holder.value();
        int time = r.timeOr(TIME);
        extras.addAnimatedRecipeArrowWidget(time).setPosition(23, 1);
        extras.addText(Component.translatable("jei.autarkia.water", r.water()), 60, 10).setPosition(23, 20).setColor(0xFF2A5C9E);
        StringBuilder chances = new StringBuilder();
        for (MachineRecipe.Result res : r.results()) {
            if (!res.certain()) {
                chances.append(chances.isEmpty() ? "" : " ").append(Math.round(res.chance() * 100)).append('%');
            }
        }
        if (!chances.isEmpty()) {
            extras.addText(Component.literal(chances.toString()), 54, 10).setPosition(getWidth() - 54, 20).setColor(0xFF808080);
        }
        String sec = String.format(Locale.ROOT, "%.1f", time / 20.0);
        extras.addText(Component.translatable("jei.autarkia.time_energy", sec, r.energyOr(ENERGY)), getWidth(), 10).setPosition(0, getHeight() - 10)
            .setColor(0xFF606060);
    }
}

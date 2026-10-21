package dev.autarkia.integration.jei;

import dev.autarkia.equipment.AscensionFittingRecipe;
import dev.autarkia.equipment.ForgeOfAscensionBlockEntity;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * JEI category {@code autarkia:ascension_fitting} (07 section 7.1, 10 section 3), 150 x 60: the worn carapace piece on
 * the left (marked "worn"), the forge inputs in up to 2 rows of 4, an arrow over the step time and the Aegis piece on
 * the right, with the step number and the FE of the piece at the bottom. Catalyst: {@code forge_of_ascension}.
 */
public class AscensionFittingCategory extends AbstractRecipeCategory<RecipeHolder<AscensionFittingRecipe>> {
    public static final int WIDTH = 150;
    public static final int HEIGHT = 60;

    public AscensionFittingCategory(IRecipeHolderType<AscensionFittingRecipe> type, IGuiHelper gui, ItemLike forge) {
        super(type, Component.translatable("jei.autarkia.category.ascension_fitting"), gui.createDrawableItemLike(forge), WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AscensionFittingRecipe> holder, IFocusGroup focuses) {
        AscensionFittingRecipe r = holder.value();
        builder.addInputSlot(1, 10).setStandardSlotBackground().add(new ItemStack(r.worn()))
            .addRichTooltipCallback((view, tip) -> tip.add(Component.translatable("jei.autarkia.ascension.worn")));
        List<SizedIngredient> in = r.inputs();
        for (int i = 0; i < in.size(); i++) {
            SizedIngredient s = in.get(i);
            IRecipeSlotBuilder slot = builder.addInputSlot(24 + (i % 4) * 18, 1 + (i / 4) * 18).setStandardSlotBackground();
            if (s.ingredient().isCustom()) {
                slot.add(s.ingredient());
            } else {
                slot.addItemStacks(s.ingredient().getValues().stream().map(h -> new ItemStack(h, s.count())).toList());
            }
        }
        builder.addOutputSlot(WIDTH - 18, 10).setOutputSlotBackground().add(new ItemStack(r.result()));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder extras, RecipeHolder<AscensionFittingRecipe> holder, IFocusGroup focuses) {
        AscensionFittingRecipe r = holder.value();
        int ticks = ForgeOfAscensionBlockEntity.STEPS.stream().filter(s -> s.number() == r.step()).mapToInt(s -> s.end() - s.start()).findFirst()
            .orElse(ForgeOfAscensionBlockEntity.TOTAL_TICKS);
        extras.addAnimatedRecipeArrowWidget(ticks).setPosition(WIDTH - 44, 10);
        extras.addText(Component.translatable("jei.autarkia.ascension.step", r.step(), String.format(java.util.Locale.ROOT, "%,d", r.energy())),
            WIDTH, 10).setPosition(0, HEIGHT - 10).setColor(0xFF606060);
    }
}

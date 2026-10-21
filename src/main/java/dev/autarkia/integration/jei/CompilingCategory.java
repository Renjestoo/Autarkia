package dev.autarkia.integration.jei;

import dev.autarkia.processing.MachineRecipe;
import dev.autarkia.processing.MatterCompilerBlockEntity;
import dev.autarkia.storage.MatterBufferBlockEntity;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

/**
 * JEI category {@code compiling} (03 section 7.1), 110 x 30: the MU text on the left, the arrow over the compile time,
 * the result on the right, seconds and FE/t under them. The time is max(20, ceil(MU / {@code matter.compilerMuPerTick})).
 */
public class CompilingCategory extends MachineCategory {
    public CompilingCategory(IRecipeHolderType<MachineRecipe> type, IGuiHelper gui, List<Block> stations) {
        super(type, Component.translatable("jei.autarkia.category.compiling"), gui, stations, MatterCompilerBlockEntity.MIN_TICKS,
            MatterCompilerBlockEntity.FE_PER_TICK, 110, 30);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        builder.addOutputSlot(getWidth() - 17, 1).setOutputSlotBackground().add(holder.value().results().getFirst().item().create());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder extras, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        MachineRecipe r = holder.value();
        int time = MatterCompilerBlockEntity.ticks(r.matter());
        extras.addText(Component.translatable("jei.autarkia.matter", MatterBufferBlockEntity.fmt(r.matter())), 58, 10).setPosition(0, 5)
            .setColor(0xFF6A3A8A);
        extras.addAnimatedRecipeArrowWidget(time).setPosition(60, 1);
        String sec = String.format(Locale.ROOT, "%.1f", time / 20.0);
        extras.addText(Component.translatable("jei.autarkia.time_energy", sec, MatterCompilerBlockEntity.FE_PER_TICK), getWidth(), 10)
            .setPosition(0, getHeight() - 10).setColor(0xFF606060);
    }
}

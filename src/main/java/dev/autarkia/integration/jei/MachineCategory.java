package dev.autarkia.integration.jei;

import dev.autarkia.processing.MachineRecipe;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * One JEI category per machine recipe kind (03 section 7.1): inputs on the left, an animated arrow over the recipe
 * time, outputs on the right with their chance, the stations that run the recipe, FE/t and seconds.
 */
public class MachineCategory extends AbstractRecipeCategory<RecipeHolder<MachineRecipe>> {
    private final List<Block> stations;
    private final int defaultTime;
    private final int defaultEnergy;

    public MachineCategory(IRecipeHolderType<MachineRecipe> type, Component title, IGuiHelper gui, List<Block> stations, int defaultTime,
                           int defaultEnergy, int width, int height) {
        super(type, title, gui.createDrawableItemLike(stations.getFirst()), width, height);
        this.stations = stations;
        this.defaultTime = defaultTime;
        this.defaultEnergy = defaultEnergy;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        MachineRecipe r = holder.value();
        List<SizedIngredient> in = r.inputs();
        for (int i = 0; i < in.size(); i++) {
            SizedIngredient s = in.get(i);
            IRecipeSlotBuilder slot = builder.addInputSlot(1 + (i % 3) * 18, 1 + (i / 3) * 18).setStandardSlotBackground();
            if (s.ingredient().isCustom()) {
                slot.add(s.ingredient());
            } else {
                slot.addItemStacks(s.ingredient().getValues().stream().map(h -> new ItemStack(h, s.count())).toList());
            }
        }
        for (int i = 0; i < r.results().size(); i++) {
            MachineRecipe.Result res = r.results().get(i);
            IRecipeSlotBuilder slot = builder.addOutputSlot(getWidth() - 18 * (r.results().size() - i) + 1, 1).setOutputSlotBackground()
                .add(res.item().create());
            if (!res.certain()) {
                int pct = Math.round(res.chance() * 100);
                slot.addRichTooltipCallback((view, tip) -> tip.add(Component.translatable("jei.autarkia.chance", pct)));
            }
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder extras, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        MachineRecipe r = holder.value();
        int time = r.timeOr(defaultTime);
        int arrowX = 1 + Math.min(3, Math.max(1, r.inputs().size())) * 18 + 4;
        extras.addAnimatedRecipeArrowWidget(time).setPosition(arrowX, 1);
        int fe = r.energyOr(defaultEnergy);
        String sec = String.format(Locale.ROOT, "%.1f", time / 20.0);
        Component info = fe > 0 ? Component.translatable("jei.autarkia.time_energy", sec, fe) : Component.translatable("jei.autarkia.time", sec);
        extras.addText(info, getWidth(), 10).setPosition(0, getHeight() - 10).setColor(0xFF606060);
        StringBuilder chances = new StringBuilder();
        for (MachineRecipe.Result res : r.results()) {
            if (!res.certain()) {
                chances.append(chances.isEmpty() ? "" : " ").append(Math.round(res.chance() * 100)).append('%');
            }
        }
        if (!chances.isEmpty()) {
            extras.addText(Component.literal(chances.toString()), 60, 10).setPosition(getWidth() - 60, 20).setColor(0xFF808080);
        }
        StringBuilder names = new StringBuilder();
        for (Block b : stations) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(b);
            if (r.allows(id.getPath())) {
                names.append(names.isEmpty() ? "" : ", ").append(b.getName().getString());
            }
        }
        extras.addText(Component.literal(names.toString()), getWidth(), 10).setPosition(0, getHeight() - 20).setColor(0xFF404040);
    }
}

package dev.autarkia.integration.jei;

import dev.autarkia.worker.WorkerAssemblyRecipe;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ITextWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * JEI category {@code autarkia:worker_assembly} (06 section 5.1, 10 section 3), 160 x 62: the frame slot on the left
 * (marked "frame"), the parts in up to 2 rows of 3, an arrow over the build time and the worker name on the right with
 * the worker's info text as its tooltip (a worker is an entity without an item form), then the station with the minimum
 * Core tier, and the seconds with FE/t. Catalysts: {@code worker_assembler}, {@code replicator_foundry},
 * {@code nanite_hive}.
 */
public class WorkerAssemblyCategory extends AbstractRecipeCategory<RecipeHolder<WorkerAssemblyRecipe>> {
    public static final int WIDTH = 160;
    public static final int HEIGHT = 62;
    private static final int TOOLTIP_WIDTH = 200;

    public WorkerAssemblyCategory(IRecipeHolderType<WorkerAssemblyRecipe> type, IGuiHelper gui, ItemLike assembler) {
        super(type, Component.translatable("jei.autarkia.category.worker_assembly"), gui.createDrawableItemLike(assembler), WIDTH, HEIGHT);
    }

    private static void fill(IRecipeSlotBuilder slot, SizedIngredient s) {
        if (s.ingredient().isCustom()) {
            slot.add(s.ingredient());
        } else {
            slot.addItemStacks(s.ingredient().getValues().stream().map(h -> new ItemStack(h, s.count())).toList());
        }
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<WorkerAssemblyRecipe> holder, IFocusGroup focuses) {
        WorkerAssemblyRecipe r = holder.value();
        r.frame().ifPresent(f -> fill(builder.addInputSlot(1, 10).setStandardSlotBackground()
            .addRichTooltipCallback((view, tip) -> tip.add(Component.translatable("jei.autarkia.worker_assembly.frame"))), f));
        List<SizedIngredient> parts = r.parts();
        for (int i = 0; i < parts.size(); i++) {
            fill(builder.addInputSlot(24 + (i % 3) * 18, 1 + (i / 3) * 18).setStandardSlotBackground(), parts.get(i));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder extras, RecipeHolder<WorkerAssemblyRecipe> holder, IFocusGroup focuses) {
        WorkerAssemblyRecipe r = holder.value();
        extras.addAnimatedRecipeArrowWidget(r.time()).setPosition(82, 10);
        ITextWidget worker = extras.addText(r.entityType().getDescription(), WIDTH - 108, 30).setPosition(108, 6).setColor(0xFF2A4A6A);
        String info = "jei.autarkia.info." + r.worker().getPath();
        if (Language.getInstance().has(info)) {
            worker.setTooltip(Minecraft.getInstance().font.getSplitter().splitLines(Component.translatable(info), TOOLTIP_WIDTH, Style.EMPTY));
        }
        extras.addText(Component.translatable("jei.autarkia.worker_assembly.station", BuiltInRegistries.BLOCK.getValue(r.machine()).getName(),
            r.minTier()), WIDTH, 10).setPosition(0, HEIGHT - 20).setColor(0xFF404040);
        String sec = String.format(Locale.ROOT, "%.1f", r.time() / 20.0);
        extras.addText(Component.translatable("jei.autarkia.time_energy", sec, r.fePerTick()), WIDTH, 10).setPosition(0, HEIGHT - 10)
            .setColor(0xFF606060);
    }
}

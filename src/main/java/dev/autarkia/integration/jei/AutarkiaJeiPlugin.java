package dev.autarkia.integration.jei;

import dev.autarkia.Autarkia;
import dev.autarkia.client.ClientRecipes;
import dev.autarkia.client.screen.SlotScreen;
import dev.autarkia.processing.MachineRecipe;
import dev.autarkia.processing.MachineRecipeKind;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaRecipes;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * JEI plugin (10 section 3): machine recipe categories, catalysts (the blast smelter also for vanilla smelting), info
 * pages and arrow click areas.
 */
@JeiPlugin
public class AutarkiaJeiPlugin implements IModPlugin {
    private static List<Block> stations(MachineRecipeKind kind) {
        return switch (kind) {
            case CRUSHING -> List.of(AutarkiaBlocks.QUERN.get(), AutarkiaBlocks.STAMP_MILL.get(), AutarkiaBlocks.JAW_CRUSHER.get());
            case ALLOYING -> List.of(AutarkiaBlocks.CRUCIBLE.get(), AutarkiaBlocks.BLAST_SMELTER.get());
            case WASHING -> List.of(AutarkiaBlocks.SLUICE.get());
            case PRESSING -> List.of(AutarkiaBlocks.PLATE_PRESS.get());
            case DRAWING -> List.of(AutarkiaBlocks.WIRE_DRAWER.get());
            case SEPARATING -> List.of(AutarkiaBlocks.MAGNETIC_SEPARATOR.get());
            case ELECTROLYZING -> List.of(AutarkiaBlocks.ELECTROLYZER.get());
            case FABRICATING -> List.of(AutarkiaBlocks.FABRICATOR.get());
        };
    }

    private static final Map<MachineRecipeKind, int[]> DEFAULTS = Map.of(
        MachineRecipeKind.CRUSHING, new int[] {160, 16},
        MachineRecipeKind.ALLOYING, new int[] {300, 0},
        MachineRecipeKind.WASHING, new int[] {100, 0},
        MachineRecipeKind.PRESSING, new int[] {80, 48},
        MachineRecipeKind.DRAWING, new int[] {100, 40},
        MachineRecipeKind.SEPARATING, new int[] {120, 128},
        MachineRecipeKind.ELECTROLYZING, new int[] {160, 256},
        MachineRecipeKind.FABRICATING, new int[] {200, 160});

    @Override
    public Identifier getPluginUid() {
        return Autarkia.id("jei");
    }

    private static IRecipeHolderType<MachineRecipe> type(MachineRecipeKind kind) {
        return IRecipeHolderType.create(AutarkiaRecipes.type(kind));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        IGuiHelper gui = reg.getJeiHelpers().getGuiHelper();
        for (MachineRecipeKind kind : MachineRecipeKind.values()) {
            int[] d = DEFAULTS.get(kind);
            reg.addRecipeCategories(new MachineCategory(type(kind), Component.translatable("jei.autarkia.category." + kind.id), gui,
                stations(kind), d[0], d[1], 138, 58));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        for (MachineRecipeKind kind : MachineRecipeKind.values()) {
            List<net.minecraft.world.item.crafting.RecipeHolder<MachineRecipe>> list = List.copyOf(ClientRecipes.map().byType(AutarkiaRecipes.type(kind)));
            reg.addRecipes(type(kind), list);
            Autarkia.LOG.info("[jei] category {} with {} recipes", kind.id, list.size());
        }
        int pages = 0;
        for (var holder : AutarkiaItems.ITEMS.getEntries()) {
            String key = "jei.autarkia.info." + holder.getId().getPath();
            if (net.minecraft.locale.Language.getInstance().has(key)) {
                reg.addIngredientInfo(holder.get(), Component.translatable(key));
                pages++;
            }
        }
        Autarkia.LOG.info("[jei] {} info pages", pages);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        for (MachineRecipeKind kind : MachineRecipeKind.values()) {
            reg.addCraftingStation(type(kind), stations(kind).toArray(ItemLike[]::new));
        }
        reg.addCraftingStation(mezz.jei.api.constants.RecipeTypes.SMELTING, AutarkiaBlocks.BLAST_SMELTER.get());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        reg.addRecipeClickArea(SlotScreen.class, 79, 35, 24, 17, java.util.Arrays.stream(MachineRecipeKind.values()).map(AutarkiaJeiPlugin::type)
            .toArray(IRecipeHolderType[]::new));
        reg.addRecipeClickArea(dev.autarkia.client.screen.FabricatorScreen.class, 94, 35, 24, 17, type(MachineRecipeKind.FABRICATING));
    }
}

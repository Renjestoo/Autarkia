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

/** JEI plugin (10 section 3): machine recipe categories, catalysts, info pages and arrow click areas. */
@JeiPlugin
public class AutarkiaJeiPlugin implements IModPlugin {
    private static List<Block> stations(MachineRecipeKind kind) {
        return switch (kind) {
            case CRUSHING -> List.of(AutarkiaBlocks.QUERN.get(), AutarkiaBlocks.STAMP_MILL.get());
            case ALLOYING -> List.of(AutarkiaBlocks.CRUCIBLE.get());
        };
    }

    private static final Map<MachineRecipeKind, int[]> DEFAULTS = Map.of(
        MachineRecipeKind.CRUSHING, new int[] {160, 16},
        MachineRecipeKind.ALLOYING, new int[] {300, 0});

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
        for (ItemLike item : List.<ItemLike>of(AutarkiaItems.HAND_CRANK.get(), AutarkiaItems.ROPE_PULLEY.get(), AutarkiaItems.ROPE_COIL.get(),
            AutarkiaItems.CRATE.get(), AutarkiaItems.TALLY_BOARD.get(), AutarkiaItems.CHUTE.get(), AutarkiaItems.QUERN.get(),
            AutarkiaItems.STAMP_MILL.get(), AutarkiaItems.CRUCIBLE.get(), AutarkiaItems.FOUNDATION.get(), AutarkiaItems.WRENCH.get(),
            AutarkiaItems.MULTIMETER.get())) {
            reg.addIngredientInfo(item, Component.translatable("jei.autarkia.info." + BuiltInRegistries.ITEM.getKey(item.asItem()).getPath()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        for (MachineRecipeKind kind : MachineRecipeKind.values()) {
            reg.addCraftingStation(type(kind), stations(kind).toArray(ItemLike[]::new));
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        reg.addRecipeClickArea(SlotScreen.class, 79, 35, 24, 17, type(MachineRecipeKind.CRUSHING), type(MachineRecipeKind.ALLOYING));
    }
}

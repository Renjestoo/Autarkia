package dev.autarkia.integration.jei;

import dev.autarkia.Autarkia;
import dev.autarkia.client.ClientRecipes;
import dev.autarkia.client.screen.SlotScreen;
import dev.autarkia.processing.MachineRecipe;
import dev.autarkia.processing.MachineRecipeKind;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaRecipes;
import dev.autarkia.registry.GuiLayout;
import dev.autarkia.worker.WorkerAssemblyRecipe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
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
 * JEI plugin (10 section 3): the 13 recipe categories (11 machine kinds, worker assembly, ascension fitting), catalysts
 * (the blast smelter also for vanilla smelting), info pages and click areas: the progress arrow of each machine screen
 * opens the categories of that machine only, the forge's fitting text opens ascension fitting.
 */
@JeiPlugin
public class AutarkiaJeiPlugin implements IModPlugin {
    private static List<Block> stations(MachineRecipeKind kind) {
        return switch (kind) {
            case CRUSHING -> List.of(AutarkiaBlocks.QUERN.get(), AutarkiaBlocks.STAMP_MILL.get(), AutarkiaBlocks.JAW_CRUSHER.get());
            case ALLOYING -> List.of(AutarkiaBlocks.CRUCIBLE.get(), AutarkiaBlocks.BLAST_SMELTER.get(), AutarkiaBlocks.PLASMA_SEPARATOR.get());
            case WASHING -> List.of(AutarkiaBlocks.SLUICE.get());
            case PRESSING -> List.of(AutarkiaBlocks.PLATE_PRESS.get());
            case DRAWING -> List.of(AutarkiaBlocks.WIRE_DRAWER.get());
            case SEPARATING -> List.of(AutarkiaBlocks.MAGNETIC_SEPARATOR.get());
            case ELECTROLYZING -> List.of(AutarkiaBlocks.ELECTROLYZER.get());
            case FABRICATING -> List.of(AutarkiaBlocks.FABRICATOR.get());
            case FLOTATION -> List.of(AutarkiaBlocks.FROTH_FLOTATION_PLANT.get());
            case PLASMA_SEPARATING -> List.of(AutarkiaBlocks.PLASMA_SEPARATOR.get());
            case COMPILING -> List.of(AutarkiaBlocks.MATTER_COMPILER.get());
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
        MachineRecipeKind.FABRICATING, new int[] {200, 160},
        MachineRecipeKind.PLASMA_SEPARATING, new int[] {100, 4096});

    @Override
    public Identifier getPluginUid() {
        return Autarkia.id("jei");
    }

    private static IRecipeHolderType<dev.autarkia.equipment.AscensionFittingRecipe> ascension() {
        return IRecipeHolderType.create(AutarkiaRecipes.ASCENSION_FITTING.get());
    }

    private static IRecipeHolderType<MachineRecipe> type(MachineRecipeKind kind) {
        return IRecipeHolderType.create(AutarkiaRecipes.type(kind));
    }

    private static IRecipeHolderType<WorkerAssemblyRecipe> assembly() {
        return IRecipeHolderType.create(AutarkiaRecipes.WORKER_ASSEMBLY.get());
    }

    private static List<Block> assemblers() {
        return List.of(AutarkiaBlocks.WORKER_ASSEMBLER.get(), AutarkiaBlocks.REPLICATOR_FOUNDRY.get(), AutarkiaBlocks.NANITE_HIVE.get());
    }

    private static Map<String, List<IRecipeType<?>>> typesByStation() {
        Map<String, List<IRecipeType<?>>> out = new HashMap<>();
        for (MachineRecipeKind kind : MachineRecipeKind.values()) {
            for (Block b : stations(kind)) {
                out.computeIfAbsent(BuiltInRegistries.BLOCK.getKey(b).getPath(), k -> new ArrayList<>()).add(type(kind));
            }
        }
        out.computeIfAbsent(BuiltInRegistries.BLOCK.getKey(AutarkiaBlocks.BLAST_SMELTER.get()).getPath(), k -> new ArrayList<>())
            .add(mezz.jei.api.constants.RecipeTypes.SMELTING);
        for (Block b : assemblers()) {
            out.computeIfAbsent(BuiltInRegistries.BLOCK.getKey(b).getPath(), k -> new ArrayList<>()).add(assembly());
        }
        return out;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        IGuiHelper gui = reg.getJeiHelpers().getGuiHelper();
        for (MachineRecipeKind kind : MachineRecipeKind.values()) {
            if (kind == MachineRecipeKind.FLOTATION) {
                reg.addRecipeCategories(new FlotationCategory(type(kind), gui, stations(kind)));
                continue;
            }
            if (kind == MachineRecipeKind.COMPILING) {
                reg.addRecipeCategories(new CompilingCategory(type(kind), gui, stations(kind)));
                continue;
            }
            int[] d = DEFAULTS.get(kind);
            reg.addRecipeCategories(new MachineCategory(type(kind), Component.translatable("jei.autarkia.category." + kind.id), gui,
                stations(kind), d[0], d[1], 138, 58));
        }
        reg.addRecipeCategories(new AscensionFittingCategory(ascension(), gui, AutarkiaBlocks.FORGE_OF_ASCENSION.get()));
        reg.addRecipeCategories(new WorkerAssemblyCategory(assembly(), gui, AutarkiaBlocks.WORKER_ASSEMBLER.get()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        for (MachineRecipeKind kind : MachineRecipeKind.values()) {
            List<net.minecraft.world.item.crafting.RecipeHolder<MachineRecipe>> list = List.copyOf(ClientRecipes.map().byType(AutarkiaRecipes.type(kind)));
            reg.addRecipes(type(kind), list);
            Autarkia.LOG.info("[jei] category {} with {} recipes", kind.id, list.size());
        }
        List<net.minecraft.world.item.crafting.RecipeHolder<dev.autarkia.equipment.AscensionFittingRecipe>> fittings =
            List.copyOf(ClientRecipes.map().byType(AutarkiaRecipes.ASCENSION_FITTING.get()));
        reg.addRecipes(ascension(), fittings);
        Autarkia.LOG.info("[jei] category ascension_fitting with {} recipes", fittings.size());
        List<net.minecraft.world.item.crafting.RecipeHolder<WorkerAssemblyRecipe>> builds =
            List.copyOf(ClientRecipes.map().byType(AutarkiaRecipes.WORKER_ASSEMBLY.get()));
        reg.addRecipes(assembly(), builds);
        Autarkia.LOG.info("[jei] category worker_assembly with {} recipes", builds.size());
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
        reg.addCraftingStation(ascension(), AutarkiaBlocks.FORGE_OF_ASCENSION.get());
        reg.addCraftingStation(assembly(), assemblers().toArray(ItemLike[]::new));
    }

    /**
     * Register one handler for every {@link SlotScreen}: JEI matches a handler on subclasses too, so the area comes from
     * the menu's layout (its arrow, 24 x 17) and the recipe types of the block with that layout name.
     */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        Map<String, List<IRecipeType<?>>> byStation = typesByStation();
        reg.addGuiContainerHandler(SlotScreen.class, new IGuiContainerHandler<SlotScreen>() {
            @Override
            public Collection<IGuiClickableArea> getGuiClickableAreas(SlotScreen screen, double mouseX, double mouseY) {
                GuiLayout layout = screen.getMenu().layout;
                List<IRecipeType<?>> types = byStation.get(layout.name());
                if (types == null || layout.arrowX() < 0) {
                    return List.of();
                }
                return List.of(IGuiClickableArea.createBasic(layout.arrowX(), layout.arrowY(), 24, 17, types.toArray(IRecipeType[]::new)));
            }
        });
        reg.addRecipeClickArea(dev.autarkia.client.screen.ForgeScreen.class, 11, 79, 154, 19, ascension());
    }
}

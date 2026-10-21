package dev.autarkia.client;

import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Keeps the recipe map the server syncs (03 section 5.1); JEI and the field manual read it on the client. */
public final class ClientRecipes {
    private static RecipeMap map = RecipeMap.EMPTY;

    private ClientRecipes() {
    }

    /** Register the capture listener. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((RecipesReceivedEvent e) -> map = e.getRecipeMap());
    }

    /** Return the last received recipe map. */
    public static RecipeMap map() {
        return map;
    }
}

package dev.autarkia.colony;

import dev.autarkia.processing.MachineRecipe;
import dev.autarkia.storage.MatterValues;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * The matter-unit price of the Replicator outpost kit (05 section 8.2 print rule 4; M8 decision G3): an item with a
 * matter value (03 section 6) costs that value; any other item costs the cheapest recipe that makes it (shaped,
 * shapeless and the mod's machine recipes, inputs at their own price, divided by the result count, cheapest item of a
 * tag), found from the loaded recipes. An item with neither a matter value nor a priced recipe (a natural material
 * such as clay, or a smelted one such as brick) costs {@link #LEAF} MU, the price of the common blocks of the table.
 * Machine blocks get no matter value of their own. Prices are cached for the current recipe map.
 */
public final class KitPrice {
    public static final long LEAF = 1;
    private static RecipeMap cachedFor;
    private static final Map<Item, Long> PRICES = new HashMap<>();

    private KitPrice() {
    }

    /** Return the kit price in matter units at the colony's outpost plan. */
    public static long of(Colony c, ServerLevel level) {
        long total = 0;
        for (Map.Entry<Item, Integer> e : OutpostProjects.kitBill(c, level).entrySet()) {
            total += price(level, e.getKey()) * e.getValue();
        }
        return total;
    }

    /** Return the matter-unit price of one item. */
    public static long price(ServerLevel level, Item item) {
        RecipeMap map = level.recipeAccess().recipeMap();
        if (map != cachedFor) {
            PRICES.clear();
            cachedFor = map;
        }
        return price(level, map, item, new HashSet<>());
    }

    private static long price(ServerLevel level, RecipeMap map, Item item, Set<Item> visiting) {
        Long known = PRICES.get(item);
        if (known != null) {
            return known;
        }
        long value = MatterValues.of(new ItemStack(item), 5);
        if (value > 0) {
            PRICES.put(item, value);
            return value;
        }
        if (!visiting.add(item)) {
            return Long.MAX_VALUE;
        }
        long best = Long.MAX_VALUE;
        for (RecipeHolder<?> h : map.values()) {
            Optional<Long> cost = cost(level, map, h.value(), item, visiting);
            if (cost.isPresent()) {
                best = Math.min(best, cost.get());
            }
        }
        visiting.remove(item);
        best = best == Long.MAX_VALUE ? LEAF : best;
        PRICES.put(item, best);
        return best;
    }

    private static Optional<Long> cost(ServerLevel level, RecipeMap map, Recipe<?> r, Item item, Set<Item> visiting) {
        ItemStack out;
        List<Ingredient> ins;
        List<Integer> counts;
        if (r instanceof ShapedRecipe s) {
            out = s.assemble(CraftingInput.EMPTY);
            ins = s.getIngredients().stream().flatMap(Optional::stream).toList();
            counts = ins.stream().map(i -> 1).toList();
        } else if (r instanceof ShapelessRecipe s) {
            out = s.result().create();
            ins = s.placementInfo().ingredients();
            counts = ins.stream().map(i -> 1).toList();
        } else if (r instanceof MachineRecipe m && !m.results().isEmpty()) {
            out = m.results().get(0).item().create();
            ins = m.inputs().stream().map(si -> si.ingredient()).toList();
            counts = m.inputs().stream().map(si -> si.count()).toList();
        } else {
            return Optional.empty();
        }
        if (!out.is(item) || out.getCount() <= 0 || ins.isEmpty()) {
            return Optional.empty();
        }
        long sum = 0;
        for (int i = 0; i < ins.size(); i++) {
            long cheapest = Long.MAX_VALUE;
            for (Holder<Item> option : (Iterable<Holder<Item>>) ins.get(i).items()::iterator) {
                cheapest = Math.min(cheapest, price(level, map, option.value(), visiting));
            }
            if (cheapest == Long.MAX_VALUE) {
                return Optional.empty();
            }
            sum += cheapest * counts.get(i);
        }
        return Optional.of((sum + out.getCount() - 1) / out.getCount());
    }
}

package dev.autarkia.colony;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Item reservations (05 section 7.5): per project, the reserved count of each item, plus the fill material the
 * leveling needs from storage. {@code available = stock - reserved}. Reserving takes the full remaining bill or nothing;
 * a worker fetch moves the count from the reservation to the worker (in transit).
 */
public final class ItemLedger {
    private final Map<UUID, Map<Item, Integer>> reserved = new LinkedHashMap<>();
    private final Map<UUID, Integer> fill = new HashMap<>();

    /** Return whether the stack is leveling fill material (dirt-like or overworld stone, or cobblestone). */
    public static boolean isFill(ItemStack stack) {
        if (stack.is(Items.COBBLESTONE)) {
            return true;
        }
        return stack.getItem() instanceof BlockItem bi && (bi.getBlock().defaultBlockState().is(BlockTags.DIRT)
            || bi.getBlock().defaultBlockState().is(BlockTags.BASE_STONE_OVERWORLD));
    }

    /** Return the count of an item over the colony storages. */
    public static int stock(ServerLevel level, ColonyRuntime rt, Item item) {
        int n = 0;
        for (BlockPos p : rt.storages) {
            if (level.getBlockEntity(p) instanceof ColonyStorage s) {
                ResourceHandler<ItemResource> h = s.colonyItems();
                for (int i = 0; i < h.size(); i++) {
                    if (h.getResource(i).is(item)) {
                        n += h.getAmountAsInt(i);
                    }
                }
            }
        }
        return n;
    }

    /** Return the count of fill material over the colony storages. */
    public static int fillStock(ServerLevel level, ColonyRuntime rt) {
        int n = 0;
        for (BlockPos p : rt.storages) {
            if (level.getBlockEntity(p) instanceof ColonyStorage s) {
                ResourceHandler<ItemResource> h = s.colonyItems();
                for (int i = 0; i < h.size(); i++) {
                    int a = h.getAmountAsInt(i);
                    if (a > 0 && isFill(h.getResource(i).toStack(1))) {
                        n += a;
                    }
                }
            }
        }
        return n;
    }

    /** Return the count of an item reserved by all projects. */
    public int reserved(Item item) {
        int n = 0;
        for (Map<Item, Integer> m : reserved.values()) {
            n += m.getOrDefault(item, 0);
        }
        return n;
    }

    /** Return the reserved count of an item for one project. */
    public int reservedFor(UUID project, Item item) {
        return reserved.getOrDefault(project, Map.of()).getOrDefault(item, 0);
    }

    /** Return the available count of an item: stock minus every reservation. */
    public int available(ServerLevel level, ColonyRuntime rt, Item item) {
        return stock(level, rt, item) - reserved(item);
    }

    /** Reserve the remaining bill of a project and its fill material; returns false and reserves nothing when short. */
    public boolean reserve(Colony c, ServerLevel level, ColonyRuntime rt, Project p) {
        Map<Item, Integer> need = Builds.remaining(level, p);
        for (Map.Entry<Item, Integer> e : need.entrySet()) {
            if (available(level, rt, e.getKey()) + reservedFor(p.id, e.getKey()) < e.getValue()) {
                return false;
            }
        }
        int fillNeed = p.phase <= 2 ? ProjectFactory.fillFromStorage(level, p) : 0;
        int fillReservedByOthers = fill.entrySet().stream().filter(e -> !e.getKey().equals(p.id)).mapToInt(Map.Entry::getValue).sum();
        if (fillStock(level, rt) - fillReservedByOthers < fillNeed) {
            return false;
        }
        reserved.put(p.id, new LinkedHashMap<>(need));
        fill.put(p.id, fillNeed);
        return true;
    }

    /** Move fetched items of a project from the reservation to the worker. */
    public void consume(UUID project, Item item, int count) {
        Map<Item, Integer> m = reserved.get(project);
        if (m != null) {
            m.computeIfPresent(item, (k, v) -> v - count <= 0 ? null : v - count);
        }
    }

    /** Move fetched fill material of a project from the reservation to the worker. */
    public void consumeFill(UUID project, int count) {
        fill.computeIfPresent(project, (k, v) -> Math.max(0, v - count));
    }

    /** Release every reservation of a project. */
    public void release(UUID project) {
        reserved.remove(project);
        fill.remove(project);
    }

    /** Release every reservation (server start, before jobs are rebuilt). */
    public void clear() {
        reserved.clear();
        fill.clear();
    }
}

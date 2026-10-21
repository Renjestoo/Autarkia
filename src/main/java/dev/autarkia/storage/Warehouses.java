package dev.autarkia.storage;

import dev.autarkia.config.AutarkiaConfig;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * Live warehouse controllers per level (04 section 2.4): a rack belongs to the nearest controller whose cube of
 * {@code warehouseRadius} contains it (tie: lowest asLong). Racks report content changes; rack placement or removal makes
 * every controller around it re-scan its area.
 */
public final class Warehouses {
    private static final Map<ServerLevel, Set<BlockPos>> CONTROLLERS = new WeakHashMap<>();

    private Warehouses() {
    }

    static void add(ServerLevel level, BlockPos pos) {
        CONTROLLERS.computeIfAbsent(level, k -> new HashSet<>()).add(pos.immutable());
    }

    static void remove(ServerLevel level, BlockPos pos) {
        Set<BlockPos> s = CONTROLLERS.get(level);
        if (s != null) {
            s.remove(pos);
        }
    }

    /** Return whether pos lies in the cube area of a controller at c. */
    public static boolean covers(BlockPos c, BlockPos pos) {
        int r = AutarkiaConfig.WAREHOUSE_RADIUS.get();
        return Math.abs(c.getX() - pos.getX()) <= r && Math.abs(c.getY() - pos.getY()) <= r && Math.abs(c.getZ() - pos.getZ()) <= r;
    }

    /** Return the controller that owns the position (nearest covering one, tie lowest asLong), or null. */
    public static @Nullable WarehouseControllerBlockEntity ownerOf(ServerLevel level, BlockPos pos) {
        BlockPos best = null;
        for (BlockPos c : CONTROLLERS.getOrDefault(level, Set.of())) {
            if (covers(c, pos) && (best == null || c.distSqr(pos) < best.distSqr(pos) || c.distSqr(pos) == best.distSqr(pos) && c.asLong() < best.asLong())) {
                best = c;
            }
        }
        return best != null && level.getBlockEntity(best) instanceof WarehouseControllerBlockEntity w ? w : null;
    }

    /** Tell the owner of a rack that the rack's contents changed. */
    public static void rackChanged(ServerLevel level, BlockPos rack) {
        WarehouseControllerBlockEntity w = ownerOf(level, rack);
        if (w != null) {
            w.markIndexDirty(false);
        }
    }

    /** Tell every controller around pos that a rack appeared or vanished there. */
    public static void layoutChanged(ServerLevel level, BlockPos pos) {
        for (BlockPos c : Set.copyOf(CONTROLLERS.getOrDefault(level, Set.of()))) {
            if (covers(c, pos) && level.getBlockEntity(c) instanceof WarehouseControllerBlockEntity w) {
                w.markIndexDirty(true);
            }
        }
    }

    /** Return all live controller positions of a level. */
    public static Set<BlockPos> all(ServerLevel level) {
        return Set.copyOf(CONTROLLERS.getOrDefault(level, Set.of()));
    }
}

package dev.autarkia.storage;

import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The matter of a domain (04 section 2.7): the loaded {@code matter_buffer}s of a level (registered by their center
 * block entity on load) and the sums over the buffers inside a colony domain. Free MU is taken from the buffers in
 * position order ({@code BlockPos.asLong}).
 */
public final class MatterBank {
    private static final Map<Level, Set<BlockPos>> LIVE = new WeakHashMap<>();

    /** Used MU, free MU and capacity summed over the buffers of a domain. */
    public record Totals(long used, long free, long capacity) {
        /** Return (used + free) / capacity, 0 without buffers (signal S27). */
        public double fill() {
            return capacity <= 0 ? 0 : (used + free) / (double) capacity;
        }
    }

    private MatterBank() {
    }

    /** Add or remove a loaded buffer center (block entity load and removal). */
    static void track(ServerLevel level, BlockPos center, boolean loaded) {
        if (loaded) {
            LIVE.computeIfAbsent(level, k -> new HashSet<>()).add(center.immutable());
        } else {
            Set<BlockPos> s = LIVE.get(level);
            if (s != null) {
                s.remove(center);
            }
        }
    }

    /** Return the loaded, formed buffers of the level in position order. */
    public static List<MatterBufferBlockEntity> loaded(ServerLevel level) {
        List<MatterBufferBlockEntity> out = new ArrayList<>();
        for (BlockPos p : List.copyOf(LIVE.getOrDefault(level, Set.of()))) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof MatterBufferBlockEntity b && !b.isRemoved() && b.isCenter()) {
                out.add(b);
            }
        }
        out.sort(Comparator.comparingLong(b -> b.getBlockPos().asLong()));
        return out;
    }

    /** Return the loaded buffers inside the domain of the colony (same dimension). */
    public static List<MatterBufferBlockEntity> inDomain(ServerLevel level, Colony c) {
        if (!c.dimension.equals(level.dimension())) {
            return List.of();
        }
        return loaded(level).stream().filter(b -> Colonies.inside(c, b.getBlockPos())).toList();
    }

    /** Return the free MU in the buffers of the domain. */
    public static long available(ServerLevel level, Colony c) {
        return inDomain(level, c).stream().mapToLong(MatterBufferBlockEntity::free).sum();
    }

    /** Return used MU, free MU and capacity over the buffers of the domain. */
    public static Totals totals(ServerLevel level, Colony c) {
        long used = 0;
        long free = 0;
        long cap = 0;
        for (MatterBufferBlockEntity b : inDomain(level, c)) {
            used += b.used();
            free += b.free();
            cap += b.capacity();
        }
        return new Totals(used, free, cap);
    }

    /** Take free MU from the buffers of the domain; returns false and takes nothing when not enough. */
    public static boolean withdraw(ServerLevel level, Colony c, long amount) {
        if (amount <= 0) {
            return true;
        }
        List<MatterBufferBlockEntity> list = inDomain(level, c);
        if (list.stream().mapToLong(MatterBufferBlockEntity::free).sum() < amount) {
            return false;
        }
        long left = amount;
        for (MatterBufferBlockEntity b : list) {
            long take = Math.min(left, b.free());
            if (take > 0 && b.takeFree(take)) {
                left -= take;
            }
            if (left == 0) {
                break;
            }
        }
        return true;
    }

    /** Return the nearest formed matter buffer to pos within range blocks, or null. */
    public static @Nullable MatterBufferBlockEntity nearest(ServerLevel level, BlockPos pos, int range) {
        MatterBufferBlockEntity best = null;
        double bestD = (double) range * range;
        for (MatterBufferBlockEntity b : loaded(level)) {
            double d = b.getBlockPos().distSqr(pos);
            if (d <= bestD) {
                bestD = d;
                best = b;
            }
        }
        return best;
    }
}

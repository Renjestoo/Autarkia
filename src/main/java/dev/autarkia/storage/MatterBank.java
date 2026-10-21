package dev.autarkia.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The matter of a domain (04 section 2.7): the loaded {@code matter_buffer}s of a level (registered by their center
 * block entity on load) and the sums over the buffers inside a colony domain. A withdrawal takes free MU fullest buffer
 * first (free MU descending, tie {@code BlockPos.asLong}), all or nothing (M9 C10), and records game time, amount and
 * reason in a per-colony log of the last {@value #LOG_SIZE} withdrawals that the {@code domain_archive} shows. The log
 * lives in memory only: a server restart empties it.
 */
public final class MatterBank {
    public static final int LOG_SIZE = 16;
    public static final String DEFAULT_REASON = "Matter";
    private static final Map<Level, Set<BlockPos>> LIVE = new WeakHashMap<>();
    private static final Map<UUID, ArrayDeque<Withdrawal>> LOG = new HashMap<>();

    /** Used MU, free MU and capacity summed over the buffers of a domain. */
    public record Totals(long used, long free, long capacity) {
        /** Return (used + free) / capacity, 0 without buffers (signal S27). */
        public double fill() {
            return capacity <= 0 ? 0 : (used + free) / (double) capacity;
        }
    }

    /** One logged withdrawal: game time, MU taken and the reason text (for example "Reform", "Seed", "Compiler"). */
    public record Withdrawal(long time, long amount, String reason) {
        public static final Codec<Withdrawal> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.LONG.fieldOf("time").forGetter(Withdrawal::time),
            Codec.LONG.fieldOf("amount").forGetter(Withdrawal::amount), Codec.STRING.fieldOf("reason").forGetter(Withdrawal::reason))
            .apply(i, Withdrawal::new));
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

    /** Take free MU from the buffers of the domain with the reason {@value #DEFAULT_REASON}; see {@link #withdraw(ServerLevel, Colony, long, String)}. */
    public static boolean withdraw(ServerLevel level, Colony c, long amount) {
        return withdraw(level, c, amount, DEFAULT_REASON);
    }

    /**
     * Take free MU from the buffers of the domain, fullest buffer first (free MU descending, tie {@code BlockPos.asLong});
     * returns false and takes nothing when the domain holds less. A positive withdrawal is logged with the reason.
     */
    public static boolean withdraw(ServerLevel level, Colony c, long amount, String reason) {
        if (amount <= 0) {
            return true;
        }
        List<MatterBufferBlockEntity> list = new ArrayList<>(inDomain(level, c));
        if (list.stream().mapToLong(MatterBufferBlockEntity::free).sum() < amount) {
            return false;
        }
        list.sort(Comparator.comparingLong((MatterBufferBlockEntity b) -> -b.free()).thenComparingLong(b -> b.getBlockPos().asLong()));
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
        ArrayDeque<Withdrawal> log = LOG.computeIfAbsent(c.id, k -> new ArrayDeque<>());
        log.addLast(new Withdrawal(level.getGameTime(), amount, reason));
        while (log.size() > LOG_SIZE) {
            log.removeFirst();
        }
        return true;
    }

    /** Return the last {@value #LOG_SIZE} withdrawals of the colony, oldest first. */
    public static List<Withdrawal> log(Colony c) {
        ArrayDeque<Withdrawal> log = LOG.get(c.id);
        return log == null ? List.of() : List.copyOf(log);
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

package dev.autarkia.logistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Per-level registry of the capsule tube networks (04 sections 1.4 and 3): the network epoch that invalidates every
 * cached network view, the server networks that hold capsules (for the colony sensors, Jade and tube removal) and the
 * client render index of capsules per tube block. Server maps are touched only on the server thread, the client index
 * only on the client thread.
 */
public final class CapsuleNetworks {
    private static final Map<Level, int[]> EPOCH = new WeakHashMap<>();
    private static final Map<Level, Map<Long, CapsuleNetwork>> HOLDERS = new WeakHashMap<>();
    private static final Map<Level, Map<Long, Map<Long, List<Capsule>>>> CLIENT = new WeakHashMap<>();

    private CapsuleNetworks() {
    }

    /** Invalidate every cached tube network view of the level (any tube or station placed or removed). */
    public static void bump(Level level) {
        EPOCH.computeIfAbsent(level, l -> new int[1])[0]++;
    }

    /** Return the network epoch of the level. */
    public static int epoch(Level level) {
        return EPOCH.computeIfAbsent(level, l -> new int[1])[0];
    }

    /** Return the number of networks of the level in state CLOGGED (sensor {@code transport.tube_clogged_networks}). */
    public static int cloggedNetworks(Level level) {
        int n = 0;
        for (CapsuleNetwork net : HOLDERS.getOrDefault(level, Map.of()).values()) {
            if (net.state() == CapsuleNetwork.State.CLOGGED) {
                n++;
            }
        }
        return n;
    }

    /** Return the number of capsules of the level that wait at a station mouth (sensor {@code transport.tube_capsules_waiting}). */
    public static int capsulesWaiting(Level level) {
        int n = 0;
        for (CapsuleNetwork net : HOLDERS.getOrDefault(level, Map.of()).values()) {
            n += net.waiting();
        }
        return n;
    }

    /** Return the running anchor network that contains the tube, or null. */
    public static @Nullable CapsuleNetwork containing(Level level, long tube) {
        for (CapsuleNetwork net : HOLDERS.getOrDefault(level, Map.of()).values()) {
            if (net.topology().tubes.contains(tube) && net.isAnchor()) {
                return net;
            }
        }
        return null;
    }

    /** Drop the capsules inside a removed tube (rule C2), in every network of the level. */
    public static void tubeRemoved(ServerLevel sl, BlockPos pos) {
        for (CapsuleNetwork net : List.copyOf(HOLDERS.getOrDefault(sl, Map.of()).values())) {
            net.dropIn(sl, pos.asLong());
        }
    }

    /** Return the station at pos when its chunk is loaded, else null. */
    public static @Nullable CapsuleStationBlockEntity station(Level level, long pos) {
        BlockPos p = BlockPos.of(pos);
        return level.isLoaded(p) && level.getBlockEntity(p) instanceof CapsuleStationBlockEntity s && !s.isRemoved() ? s : null;
    }

    /** Return the client capsules whose current or previous block is pos, over all networks of the client level. */
    public static List<Capsule> clientAt(Level level, BlockPos pos) {
        Map<Long, Map<Long, List<Capsule>>> all = CLIENT.get(level);
        if (all == null) {
            return List.of();
        }
        List<Capsule> out = new ArrayList<>();
        for (Map<Long, List<Capsule>> byBlock : all.values()) {
            out.addAll(byBlock.getOrDefault(pos.asLong(), List.of()));
        }
        return out;
    }

    static void register(Level level, long owner, CapsuleNetwork net) {
        HOLDERS.computeIfAbsent(level, l -> new HashMap<>()).put(owner, net);
    }

    static void unregister(Level level, long owner, CapsuleNetwork net) {
        Map<Long, CapsuleNetwork> m = HOLDERS.get(level);
        if (m != null) {
            m.remove(owner, net);
        }
    }

    static void publish(Level level, long owner, Map<Long, List<Capsule>> byBlock) {
        CLIENT.computeIfAbsent(level, l -> new HashMap<>()).put(owner, byBlock);
    }

    static void unpublish(Level level, long owner) {
        Map<Long, Map<Long, List<Capsule>>> all = CLIENT.get(level);
        if (all != null) {
            all.remove(owner);
        }
    }
}

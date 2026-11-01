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
    private static final Map<Level, SharedFloods> FLOODS = new WeakHashMap<>();

    private static final class SharedFloods {
        private int epoch = -1;
        private long tick = -1;
        private final Map<Long, CapsuleTopology> complete = new HashMap<>();
        private final Map<Long, CapsuleTopology> thisTick = new HashMap<>();
    }

    private CapsuleNetworks() {
    }

    /**
     * Return the topology of the network that contains the tube at start, with one flood per network. A complete flood is
     * shared until the next epoch; one that stopped at an unloaded chunk or a cap is shared for the current tick only.
     */
    public static CapsuleTopology flood(ServerLevel sl, BlockPos start, int maxTubes) {
        SharedFloods floods = FLOODS.computeIfAbsent(sl, l -> new SharedFloods());
        if (floods.epoch != epoch(sl)) {
            floods.epoch = epoch(sl);
            floods.complete.clear();
            floods.thisTick.clear();
        }
        if (floods.tick != sl.getGameTime()) {
            floods.tick = sl.getGameTime();
            floods.thisTick.clear();
        }
        CapsuleTopology known = floods.complete.getOrDefault(start.asLong(), floods.thisTick.get(start.asLong()));
        if (known != null) {
            return known;
        }
        CapsuleTopology flooded = CapsuleTopology.flood(sl, start, maxTubes);
        Map<Long, CapsuleTopology> into = flooded.tooLarge || flooded.incomplete ? floods.thisTick : floods.complete;
        for (long tube : flooded.tubes) {
            into.put(tube, flooded);
        }
        return flooded;
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

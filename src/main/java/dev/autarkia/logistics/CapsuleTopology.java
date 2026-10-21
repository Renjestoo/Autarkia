package dev.autarkia.logistics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Flood-filled view of one capsule tube network (04 section 1.4): its tubes, its stations (each with the tube at its
 * mouth) and one probe position per chunk. The flood never reads an unloaded block: it marks the view incomplete
 * instead (rule C3). A network over {@code tubeMaxNetworkTubes} tubes or {@link #MAX_STATIONS} stations stops the flood
 * and is marked too large. Routes are breadth-first searches over the tube graph, cached per source block until the
 * view is replaced.
 */
public final class CapsuleTopology {
    public static final int MAX_STATIONS = 32;
    public static final CapsuleTopology EMPTY = new CapsuleTopology(Set.of(), Collections.emptyNavigableMap(), List.of(), false, false);
    private static final Direction[] DIRS = Direction.values();
    public final Set<Long> tubes;
    public final NavigableMap<Long, Long> stations;
    public final List<BlockPos> chunkProbes;
    public final boolean tooLarge;
    public final boolean incomplete;
    private final Map<Long, Map<Long, Long>> parents = new HashMap<>();

    private CapsuleTopology(Set<Long> tubes, NavigableMap<Long, Long> stations, List<BlockPos> chunkProbes, boolean tooLarge, boolean incomplete) {
        this.tubes = tubes;
        this.stations = stations;
        this.chunkProbes = chunkProbes;
        this.tooLarge = tooLarge;
        this.incomplete = incomplete;
    }

    /** Return the direction with the given ordinal. */
    public static Direction dir(int ordinal) {
        return DIRS[ordinal];
    }

    /**
     * Flood the network that contains the tube at start.
     *
     * @return {@link #EMPTY} when start is not a loaded tube
     */
    public static CapsuleTopology flood(Level level, BlockPos start, int maxTubes) {
        if (!level.isLoaded(start) || !CapsuleTubeBlock.isTube(level.getBlockState(start))) {
            return level.isLoaded(start) ? EMPTY : new CapsuleTopology(Set.of(), Collections.emptyNavigableMap(), List.of(), false, true);
        }
        Set<Long> tubes = new HashSet<>();
        TreeMap<Long, Long> stations = new TreeMap<>();
        Map<Long, BlockPos> probes = new LinkedHashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        boolean tooLarge = false;
        boolean incomplete = false;
        tubes.add(start.asLong());
        probes.put(ChunkPos.pack(start), start.immutable());
        queue.add(start.immutable());
        search:
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (Direction d : DIRS) {
                BlockPos n = p.relative(d);
                long nl = n.asLong();
                if (tubes.contains(nl) || stations.containsKey(nl)) {
                    continue;
                }
                if (!level.isLoaded(n)) {
                    incomplete = true;
                    continue;
                }
                BlockState s = level.getBlockState(n);
                if (CapsuleTubeBlock.isTube(s)) {
                    if (tubes.size() >= maxTubes) {
                        tooLarge = true;
                        break search;
                    }
                    tubes.add(nl);
                    queue.add(n);
                } else if (CapsuleStationBlock.mouthOf(s) == d.getOpposite()) {
                    if (stations.size() >= MAX_STATIONS) {
                        tooLarge = true;
                        break search;
                    }
                    stations.put(nl, p.asLong());
                } else {
                    continue;
                }
                probes.putIfAbsent(ChunkPos.pack(n), n);
            }
        }
        return new CapsuleTopology(Set.copyOf(tubes), Collections.unmodifiableNavigableMap(stations), List.copyOf(probes.values()), tooLarge, incomplete);
    }

    /** Return the anchor (the station with the lowest {@code BlockPos.asLong}), or null when the network has no station. */
    public @Nullable Long anchor() {
        return stations.isEmpty() ? null : stations.firstKey();
    }

    /** Return true when the network can run: complete, within the caps, with at least one station. */
    public boolean runnable() {
        return !tooLarge && !incomplete && !stations.isEmpty();
    }

    /** Return true while every chunk of the network is loaded (rule C3). */
    public boolean loaded(Level level) {
        for (BlockPos p : chunkProbes) {
            if (!level.isLoaded(p)) {
                return false;
            }
        }
        return true;
    }

    /** Return true when the block is a tube or a station of this network. */
    public boolean contains(long block) {
        return tubes.contains(block) || stations.containsKey(block);
    }

    /**
     * Return the route from a tube or station of this network to a station, one direction ordinal per block step, or
     * null when the target is the source or is not reachable.
     */
    public byte @Nullable [] path(long from, long to) {
        if (from == to || !contains(from) || !stations.containsKey(to)) {
            return null;
        }
        Map<Long, Long> par = parents.computeIfAbsent(from, this::bfs);
        if (!par.containsKey(to)) {
            return null;
        }
        List<Long> chain = new ArrayList<>();
        for (long b = to; b != from; b = par.get(b)) {
            chain.add(b);
        }
        chain.add(from);
        Collections.reverse(chain);
        byte[] out = new byte[chain.size() - 1];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) step(chain.get(i), chain.get(i + 1));
        }
        return out;
    }

    /** Return true when the rest of the capsule's route from its current block to its target mouth lies in this network. */
    public boolean intact(Capsule c) {
        if (c.path.length == 0) {
            return false;
        }
        long[] b = c.blocks();
        for (int i = c.occIndex(c.pos); i < c.path.length; i++) {
            if (!tubes.contains(b[i])) {
                return false;
            }
        }
        Long mouth = stations.get(b[c.path.length]);
        return mouth != null && mouth == b[c.path.length - 1] && b[c.path.length] == c.target;
    }

    private Map<Long, Long> bfs(long from) {
        Map<Long, Long> par = new HashMap<>();
        par.put(from, from);
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(from);
        while (!queue.isEmpty()) {
            long b = queue.poll();
            if (b != from && stations.containsKey(b)) {
                continue;
            }
            for (long n : neighbours(b)) {
                if (!par.containsKey(n)) {
                    par.put(n, b);
                    queue.add(n);
                }
            }
        }
        return par;
    }

    private List<Long> neighbours(long b) {
        Long mouth = stations.get(b);
        if (mouth != null) {
            return List.of(mouth);
        }
        List<Long> out = new ArrayList<>(6);
        BlockPos p = BlockPos.of(b);
        for (Direction d : DIRS) {
            long n = p.relative(d).asLong();
            Long m = stations.get(n);
            if (tubes.contains(n) || m != null && m == b) {
                out.add(n);
            }
        }
        return out;
    }

    private static int step(long a, long b) {
        BlockPos pa = BlockPos.of(a);
        BlockPos pb = BlockPos.of(b);
        for (Direction d : DIRS) {
            if (pa.relative(d).equals(pb)) {
                return d.ordinal();
            }
        }
        throw new IllegalStateException("Route blocks " + pa.toShortString() + " and " + pb.toShortString() + " are not adjacent");
    }
}

package dev.autarkia.logistics;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.storage.FallingCrateEntity;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * One mag rail line (04 section 1.7): the chain of connected track cells (mag rails and stations, G6) through a cell,
 * in canonical order (the end with the lower {@code BlockPos.asLong} first). A trace stops at an unloaded chunk
 * (partial line) and after {@code magRailMaxRails} + 16 + 2 cells; a line with more rails or more than 16 stations is
 * over the cap (C11, per line), and a closed loop is reported as such (stations on it do not send). Lines are cached per
 * level until any rail or station changes ({@link #changed()}); a partial line is traced again every tick. The line
 * also answers the single-track rules for the sleds on it: the direction lock (a sled may start only when no sled moves
 * the other way on an overlapping section) and the 8-block spacing of sleds in the same direction.
 */
public final class MagRailLine {
    public static final int MAX_STATIONS = 16;
    public static final double SPACING = 8.0;
    private static final AtomicLong VERSION = new AtomicLong();
    private static final Map<Level, Cache> CACHE = new WeakHashMap<>();

    /** Why a sled may not start now. */
    public enum Hold { NONE, LOCK, SPACING }

    /** A sled on this line: canonical position (cells), travel direction (+1 or -1), canonical destination, arrival flag. */
    public record SledView(FallingCrateEntity entity, double canon, int dir, int dest, boolean arrived) {
    }

    private static final class Cache {
        long version = -1;
        final Map<Long, MagRailLine> lines = new HashMap<>();
        long sledTick = Long.MIN_VALUE;
        List<FallingCrateEntity> sleds = List.of();
    }

    private record Walk(List<BlockPos> cells, boolean partial, boolean loop) {
    }

    public final List<BlockPos> cells;
    private final Long2IntOpenHashMap index = new Long2IntOpenHashMap();
    private final IntOpenHashSet stationIndex = new IntOpenHashSet();
    public final boolean loop;
    public final boolean partial;
    public final boolean overCap;
    public final int rails;
    private final List<Integer> stations;
    private final long tracedAt;

    private MagRailLine(List<BlockPos> cells, boolean loop, boolean partial, boolean tooMany, ServerLevel level) {
        this.cells = List.copyOf(cells);
        this.loop = loop;
        this.partial = partial;
        this.tracedAt = level.getGameTime();
        index.defaultReturnValue(-1);
        List<Integer> st = new ArrayList<>();
        int r = 0;
        for (int i = 0; i < cells.size(); i++) {
            index.put(cells.get(i).asLong(), i);
            if (level.getBlockState(cells.get(i)).getBlock() instanceof MagRailStationBlock) {
                st.add(i);
                stationIndex.add(i);
            } else {
                r++;
            }
        }
        this.rails = r;
        this.stations = List.copyOf(st);
        this.overCap = tooMany || r > AutarkiaConfig.MAG_RAIL_MAX_RAILS.get() || st.size() > MAX_STATIONS;
    }

    /** Mark every cached line stale (a rail or station was placed, removed or reshaped). */
    public static void changed() {
        VERSION.incrementAndGet();
    }

    /** Return the line through a loaded track cell, or null when the cell is not track. */
    public static @Nullable MagRailLine of(ServerLevel level, BlockPos cell) {
        if (!level.isLoaded(cell) || !MagRailBlock.isTrack(level.getBlockState(cell))) {
            return null;
        }
        Cache c = CACHE.computeIfAbsent(level, k -> new Cache());
        long v = VERSION.get();
        if (c.version != v) {
            c.lines.clear();
            c.version = v;
        }
        MagRailLine l = c.lines.get(cell.asLong());
        if (l != null && (!l.partial || l.tracedAt == level.getGameTime())) {
            return l;
        }
        l = trace(level, cell);
        for (BlockPos p : l.cells) {
            c.lines.put(p.asLong(), l);
        }
        return l;
    }

    private static MagRailLine trace(ServerLevel level, BlockPos start) {
        int limit = AutarkiaConfig.MAG_RAIL_MAX_RAILS.get() + MAX_STATIONS + 2;
        List<MagRailBlock.Link> first = MagRailBlock.links(level, start);
        Walk a = first.isEmpty() ? new Walk(List.of(), false, false) : walk(level, start, first.get(0), limit);
        Walk b = a.loop() || first.size() < 2 ? new Walk(List.of(), false, false) : walk(level, start, first.get(1), limit - a.cells().size());
        List<BlockPos> cells = new ArrayList<>(b.cells());
        Collections.reverse(cells);
        cells.add(start);
        cells.addAll(a.cells());
        if (!a.loop() && cells.get(0).asLong() > cells.get(cells.size() - 1).asLong()) {
            Collections.reverse(cells);
        }
        return new MagRailLine(cells, a.loop(), a.partial() || b.partial(), cells.size() >= limit, level);
    }

    private static Walk walk(ServerLevel level, BlockPos start, MagRailBlock.Link first, int limit) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos prev = start;
        MagRailBlock.Link cur = first;
        while (true) {
            if (cur.unloaded()) {
                return new Walk(out, true, false);
            }
            BlockPos c = cur.pos();
            if (c.equals(start)) {
                return new Walk(out, false, true);
            }
            out.add(c);
            if (out.size() >= limit) {
                return new Walk(out, false, false);
            }
            MagRailBlock.Link next = null;
            for (MagRailBlock.Link l : MagRailBlock.links(level, c)) {
                if (l.unloaded() || !l.pos().equals(prev)) {
                    next = l;
                    break;
                }
            }
            if (next == null) {
                return new Walk(out, false, false);
            }
            prev = c;
            cur = next;
        }
    }

    /** Return the canonical index of a cell, or -1 when it is not on this line. */
    public int indexOf(BlockPos p) {
        return index.get(p.asLong());
    }

    /** Return the number of cells. */
    public int size() {
        return cells.size();
    }

    /** Return the canonical indices of the station cells. */
    public List<Integer> stations() {
        return stations;
    }

    /** Return true when the cell at a canonical index is a station. */
    public boolean isStation(int i) {
        return stationIndex.contains(i);
    }

    /** Return the cells from index a to index b inclusive, in travel order. */
    public List<BlockPos> path(int a, int b) {
        List<BlockPos> out = new ArrayList<>();
        int d = Integer.signum(b - a);
        for (int i = a; ; i += d) {
            out.add(cells.get(i));
            if (i == b || d == 0) {
                return out;
            }
        }
    }

    /** Return the number of mag rail cells (stations excluded) from index a to index b inclusive. */
    public int railsBetween(int a, int b) {
        int n = 0;
        for (int i = Math.min(a, b); i <= Math.max(a, b); i++) {
            n += isStation(i) ? 0 : 1;
        }
        return n;
    }

    /** Drop the per-tick sled list of the level (a sled started, arrived or fell during this tick). */
    public static void invalidateSleds(ServerLevel level) {
        Cache c = CACHE.get(level);
        if (c != null) {
            c.sledTick = Long.MIN_VALUE;
        }
    }

    /** Return every rail sled of the level (cached for the tick until a sled starts, arrives or falls). */
    public static List<FallingCrateEntity> sleds(ServerLevel level) {
        Cache c = CACHE.computeIfAbsent(level, k -> new Cache());
        if (c.sledTick != level.getGameTime()) {
            c.sleds = List.copyOf(level.getEntities(AutarkiaEntities.FALLING_CRATE.get(), e -> e.rail() != null && e.isAlive()));
            c.sledTick = level.getGameTime();
        }
        return c.sleds;
    }

    /** Return the sleds whose current cell lies on this line, with canonical position and direction. */
    public List<SledView> sledsOn(ServerLevel level) {
        List<SledView> out = new ArrayList<>();
        for (FallingCrateEntity e : sleds(level)) {
            MagRailSled.Route r = e.rail();
            if (r == null || e.isRemoved()) {
                continue;
            }
            int ci = r.cellIndex();
            int li = indexOf(r.cell(ci));
            int dest = indexOf(r.cell(r.last()));
            if (li < 0 || dest < 0) {
                continue;
            }
            int dir;
            if (ci < r.last()) {
                int ln = indexOf(r.cell(ci + 1));
                dir = ln > li ? 1 : -1;
            } else {
                int lp = indexOf(r.cell(Math.max(0, ci - 1)));
                dir = li >= lp ? 1 : -1;
            }
            out.add(new SledView(e, li + dir * r.fraction(), dir, dest, r.arrived()));
        }
        return out;
    }

    /** Return what stops a sled from starting at canonical index s toward index d now (lock, spacing or none). */
    public Hold startHold(ServerLevel level, int s, int d) {
        int dir = Integer.signum(d - s);
        double lo = Math.min(s, d);
        double hi = Math.max(s, d);
        Hold out = Hold.NONE;
        for (SledView o : sledsOn(level)) {
            if (o.arrived()) {
                if (Math.abs(o.canon() - s) < SPACING) {
                    out = Hold.SPACING;
                }
                continue;
            }
            if (o.dir() != dir) {
                double olo = Math.min(o.canon(), o.dest());
                double ohi = Math.max(o.canon(), o.dest());
                if (ohi > lo && hi > olo) {
                    return Hold.LOCK;
                }
                continue;
            }
            double delta = (o.canon() - s) * dir;
            if (delta >= 0 && delta < SPACING || delta < 0 && delta > -SPACING && (o.dest() - s) * dir > 0) {
                out = Hold.SPACING;
            }
        }
        return out;
    }

    /** Return the distance in blocks to the nearest sled ahead in the travel direction (moving the same way or arrived). */
    public double gapAhead(ServerLevel level, FallingCrateEntity self, double canon, int dir) {
        double best = Double.MAX_VALUE;
        for (SledView o : sledsOn(level)) {
            if (o.entity() == self || o.dir() != dir && !o.arrived()) {
                continue;
            }
            double delta = (o.canon() - canon) * dir;
            if (delta > 1e-6 && delta < best) {
                best = delta;
            }
        }
        return best;
    }

    /** Return the number of sleds of the level that wait for a direction lock, spacing or output space (sensor). */
    public static int sledsWaiting(ServerLevel level) {
        int n = MagRailStationBlockEntity.levelLocked(level);
        for (FallingCrateEntity e : sleds(level)) {
            MagRailSled.Route r = e.rail();
            n += r != null && r.arrived() ? 1 : 0;
        }
        return n;
    }

    /** Return the track height of a cell above its block position: 1 px on a flat rail, mid slope, 4 px on a station. */
    public static double trackHeight(BlockState s) {
        if (s.getBlock() instanceof MagRailStationBlock) {
            return 0.25;
        }
        return s.getBlock() instanceof MagRailBlock && s.getValue(MagRailBlock.SHAPE).isSlope() ? 0.5625 : 0.0625;
    }
}

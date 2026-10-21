package dev.autarkia.colony;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GridConnector;
import dev.autarkia.energy.GridNetwork;
import dev.autarkia.energy.Span;
import dev.autarkia.registry.AutarkiaBlocks;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * The route of template 22 {@code hv_link} (05 section 8.1): a line of {@code hv_pylon}s from the outpost's HV pylon
 * toward the nearest node of a colony HV or insulated network in the Core square, a pylon about every
 * {@code maxSpan(HV) - 4} = 44 blocks on the surface (a shorter step, down to 12, when the terrain makes the span too
 * long or a chord is blocked), each shifted up to 3 blocks sideways off water; the first one up to 12, so its chord
 * can pass the outpost's own buildings (the wind tower stands taller than the pylon). The end node of an insulated network gets
 * a {@code transformer} within 4 blocks (HV span in, insulated span out, 02 section 6.3). Logs and leaves in a chord
 * become cut cells. With {@code outpost.hvLinkLamps} each pylon gets an {@code industrial_lamp} beside its base (a
 * pylon feeds only the six neighbours of its base, and a lamp on its top would sit in the cable chord).
 */
final class PylonRoute {
    private static final int[] SHIFTS = {0, 1, -1, 2, -2, 3, -3};
    private static final int[] FIRST_SHIFTS = {0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6, 7, -7, 8, -8, 9, -9, 10, -10, 11, -11, 12, -12};
    private static final int MIN_STEP = 12;
    private static final int MAX_PYLONS = 64;
    private static final int MAX_CUTS = 48;

    /** A planned line: cells and spans in route order (outpost first) and the tree cuts of its chords. */
    record Plan(List<Project.Cell> cells, List<Project.SpanRec> spans, long[] cuts) {
    }

    private record End(BlockPos node, Vec3 attach, @Nullable BlockPos transformer, @Nullable Vec3 transformerAttach) {
    }

    private PylonRoute() {
    }

    /** Return the outpost's free HV pylon nearest its core (the line start), or null; load false searches loaded chunks only. */
    static @Nullable BlockPos start(ServerLevel level, Colony c, Colony.Outpost o, boolean load) {
        int h = ColonyTiers.outpostHalf(c.tier);
        BlockPos best = null;
        for (int cx = (o.corePos.getX() - h) >> 4; cx <= (o.corePos.getX() + h) >> 4; cx++) {
            for (int cz = (o.corePos.getZ() - h) >> 4; cz <= (o.corePos.getZ() + h) >> 4; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = load ? level.getChunk(cx, cz) : level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (net.minecraft.world.level.block.entity.BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (be.getType() == dev.autarkia.registry.AutarkiaBlockEntities.HV_PYLON.get()
                        && be instanceof net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity m && m.isCenter()
                        && OutpostProjects.outpostAt(c, p) == o && accepts(level, p, CableTier.HV)
                        && (best == null || p.distSqr(o.corePos) < best.distSqr(o.corePos))) {
                        best = p.immutable();
                    }
                }
            }
        }
        return best;
    }

    /** Return the highest load of the HV spans reachable from the outpost pylon outside the Core square (the link S06). */
    static double load(ServerLevel level, Colony c, BlockPos start) {
        var data = Grid.of(level).data();
        LongOpenHashSet seen = new LongOpenHashSet();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(start.asLong());
        seen.add(start.asLong());
        double max = 0;
        while (!queue.isEmpty()) {
            long node = queue.poll();
            IntArrayList ids = data.at(node);
            for (int i = 0; i < ids.size(); i++) {
                Span s = data.spans.get(ids.getInt(i));
                if (s == null || s.tier != CableTier.HV) {
                    continue;
                }
                max = Math.max(max, s.load);
                long other = s.a == node ? s.b : s.a;
                if (seen.add(other) && !OutpostProjects.inMain(c, BlockPos.of(other))) {
                    queue.add(other);
                }
            }
        }
        return max;
    }

    /** Plan the line from the start pylon, every pylon shifted sideways by the lateral offset (a parallel line); null when no route works. */
    static @Nullable Plan plan(ServerLevel level, Colony c, BlockPos start, int lateral) {
        End end = end(level, c, start);
        if (end == null) {
            return null;
        }
        BlockPos last = end.transformer() != null ? end.transformer() : end.node();
        Vec3 lastAttach = end.transformer() != null ? end.transformerAttach() : end.attach();
        BlockState pylon = AutarkiaBlocks.HV_PYLON.get().defaultBlockState().setValue(BaseMultiblock.CENTER, true);
        LongArrayList cuts = new LongArrayList();
        List<BlockPos> pylons = new ArrayList<>();
        BlockPos prev = start;
        Vec3 prevAttach = attach(level, start);
        while (true) {
            List<Long> direct = prevAttach.distanceTo(lastAttach) <= CableTier.HV.maxSpan ? chordCuts(level, prevAttach, lastAttach, prev, last) : null;
            if (direct != null) {
                cuts.addAll(direct);
                break;
            }
            BlockPos next = null;
            double remain = OutpostProjects.horizontal(prev, last);
            for (int step = CableTier.HV.maxSpan - 4; step >= MIN_STEP && next == null; step -= 4) {
                if (step >= remain) {
                    continue;
                }
                double dx = (last.getX() - prev.getX()) / remain;
                double dz = (last.getZ() - prev.getZ()) / remain;
                for (int s : pylons.isEmpty() ? FIRST_SHIFTS : SHIFTS) {
                    int shift = s + lateral;
                    BlockPos base = base(level, c, (int) Math.round(prev.getX() + dx * step - dz * shift), (int) Math.round(prev.getZ() + dz * step + dx * shift));
                    if (base == null) {
                        continue;
                    }
                    Vec3 a = AutarkiaBlocks.HV_PYLON.get().attachPoint(base, pylon);
                    List<Long> cut = a.distanceTo(prevAttach) <= CableTier.HV.maxSpan ? chordCuts(level, prevAttach, a, prev, base) : null;
                    if (cut != null) {
                        cuts.addAll(cut);
                        next = base;
                        prevAttach = a;
                        break;
                    }
                }
            }
            if (next == null || pylons.size() >= MAX_PYLONS) {
                return null;
            }
            pylons.add(next);
            prev = next;
        }
        List<Project.Cell> cells = new ArrayList<>();
        List<Project.SpanRec> spans = new ArrayList<>();
        BlockPos from = start;
        for (BlockPos p : pylons) {
            cells.add(new Project.Cell(p.asLong(), pylon));
            BlockPos lamp = AutarkiaConfig.OUTPOST_HV_LINK_LAMPS.get() ? lamp(level, from, p) : null;
            if (lamp != null) {
                cells.add(new Project.Cell(lamp.asLong(), AutarkiaBlocks.INDUSTRIAL_LAMP.get().defaultBlockState()));
            }
            spans.add(new Project.SpanRec(from.asLong(), p.asLong(), CableTier.HV));
            from = p;
        }
        spans.add(new Project.SpanRec(from.asLong(), last.asLong(), CableTier.HV));
        if (end.transformer() != null) {
            cells.add(new Project.Cell(end.transformer().asLong(), AutarkiaBlocks.TRANSFORMER.get().defaultBlockState()));
            spans.add(new Project.SpanRec(end.transformer().asLong(), end.node().asLong(), CableTier.INSULATED));
        }
        return new Plan(cells, spans, new LongOpenHashSet(cuts).toLongArray());
    }

    private static Vec3 attach(ServerLevel level, BlockPos node) {
        Grid.Node n = Grid.resolve(level, node);
        return n != null && n.connector() != null ? n.connector().attachPoint(n.pos(), n.state()) : Vec3.atCenterOf(node);
    }

    private static List<Span> spansAt(ServerLevel level, BlockPos node) {
        var data = Grid.of(level).data();
        IntArrayList ids = data.at(node.asLong());
        List<Span> out = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Span s = data.spans.get(ids.getInt(i));
            if (s != null) {
                out.add(s);
            }
        }
        return out;
    }

    private static boolean accepts(ServerLevel level, BlockPos node, CableTier tier) {
        Grid.Node n = Grid.resolve(level, node);
        GridConnector con = n == null ? null : n.connector();
        return con != null && con.accepts(tier, spansAt(level, node)) == Grid.SpanResult.OK;
    }

    /**
     * Return the end of the line: the nearest connector of a domain HV network in the Core square that takes one more
     * HV span; else the nearest insulated connector that takes one more insulated span, with a transformer spot.
     */
    private static @Nullable End end(ServerLevel level, Colony c, BlockPos start) {
        ColonyRuntime rt = ColonyRuntime.of(c);
        BlockPos hv = null;
        BlockPos ins = null;
        for (GridNetwork n : Grid.of(level).networks()) {
            if (!rt.networks.contains(n.id) || n.tier != CableTier.HV && n.tier != CableTier.INSULATED) {
                continue;
            }
            for (int i = 0; i < n.nodes.size(); i++) {
                BlockPos p = BlockPos.of(n.nodes.getLong(i));
                if (!OutpostProjects.inMain(c, p) || !accepts(level, p, n.tier)) {
                    continue;
                }
                if (n.tier == CableTier.HV && (hv == null || closer(start, p, hv))) {
                    hv = p;
                } else if (n.tier == CableTier.INSULATED && (ins == null || closer(start, p, ins))) {
                    ins = p;
                }
            }
        }
        if (hv != null) {
            return new End(hv, attach(level, hv), null, null);
        }
        if (ins == null) {
            return null;
        }
        Vec3 insAttach = attach(level, ins);
        BlockPos best = null;
        Vec3 bestAttach = null;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (Math.abs(dx) + Math.abs(dz) < 2) {
                    continue;
                }
                BlockPos t = surface(level, c, ins.getX() + dx, ins.getZ() + dz, 1);
                if (t == null || !OutpostProjects.inMain(c, t) || best != null && !closer(start, t, best)) {
                    continue;
                }
                Vec3 a = Vec3.atCenterOf(t).add(0, 0.5, 0);
                List<Long> cut = a.distanceTo(insAttach) <= CableTier.INSULATED.maxSpan ? chordCuts(level, a, insAttach, t, ins) : null;
                if (cut != null && cut.isEmpty()) {
                    best = t;
                    bestAttach = a;
                }
            }
        }
        return best == null ? null : new End(ins, insAttach, best, bestAttach);
    }

    private static boolean closer(BlockPos to, BlockPos a, BlockPos b) {
        double da = OutpostProjects.horizontal(to, a);
        double db = OutpostProjects.horizontal(to, b);
        return da < db || da == db && a.asLong() < b.asLong();
    }

    private static @Nullable BlockPos base(ServerLevel level, Colony c, int x, int z) {
        return surface(level, c, x, z, AutarkiaBlocks.HV_PYLON.get().height());
    }

    /**
     * Return the first free position above the surface of a column (chunk loaded on demand) when the ground is sturdy and
     * dry and the given height above it is free (replaceable, no fluid, no block entity) and not in another colony.
     */
    private static @Nullable BlockPos surface(ServerLevel level, Colony c, int x, int z, int height) {
        level.getChunk(x >> 4, z >> 4);
        BlockPos base = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        BlockPos ground = base.below();
        BlockState g = level.getBlockState(ground);
        if (!g.getFluidState().isEmpty() || !g.isFaceSturdy(level, ground, Direction.UP)) {
            return null;
        }
        for (int i = 0; i < height; i++) {
            BlockState st = level.getBlockState(base.above(i));
            if (!Builds.replaceable(st) || !st.getFluidState().isEmpty() || st.hasBlockEntity()) {
                return null;
            }
        }
        Colony other = Colonies.at(level, base);
        return other == null || other == c ? base : null;
    }

    private static @Nullable BlockPos lamp(ServerLevel level, BlockPos from, BlockPos pylon) {
        Direction along = Direction.getApproximateNearest(pylon.getX() - from.getX(), 0, pylon.getZ() - from.getZ());
        for (Direction side : List.of(along.getClockWise(), along.getCounterClockWise())) {
            BlockPos p = pylon.relative(side);
            BlockState st = level.getBlockState(p);
            if (Builds.replaceable(st) && st.getFluidState().isEmpty() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Return the log and leaf blocks a chord passes through (the same samples as {@link Grid#chordBlocked}), or null
     * when any other block blocks it or more than 48 would have to go.
     */
    private static @Nullable List<Long> chordCuts(ServerLevel level, Vec3 a, Vec3 b, BlockPos nodeA, BlockPos nodeB) {
        List<Long> out = new ArrayList<>();
        int steps = Math.max(2, (int) Math.ceil(a.distanceTo(b) / 0.1));
        for (int i = 1; i < steps; i++) {
            Vec3 p = a.lerp(b, i / (double) steps);
            BlockPos bp = BlockPos.containing(p);
            if (bp.getX() == nodeA.getX() && bp.getZ() == nodeA.getZ() || bp.getX() == nodeB.getX() && bp.getZ() == nodeB.getZ()
                || out.contains(bp.asLong())) {
                continue;
            }
            BlockState st = level.getBlockState(bp);
            VoxelShape shape = st.getCollisionShape(level, bp);
            if (shape.isEmpty() || !inside(shape, p.subtract(bp.getX(), bp.getY(), bp.getZ()))) {
                continue;
            }
            if (!st.is(BlockTags.LOGS) && !st.is(BlockTags.LEAVES) || out.size() >= MAX_CUTS) {
                return null;
            }
            out.add(bp.asLong());
        }
        return out;
    }

    private static boolean inside(VoxelShape shape, Vec3 local) {
        for (AABB box : shape.toAabbs()) {
            if (box.contains(local)) {
                return true;
            }
        }
        return false;
    }
}

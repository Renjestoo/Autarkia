package dev.autarkia.colony;

import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GridLevel;
import dev.autarkia.energy.GridMember;
import dev.autarkia.energy.GridNetwork;
import dev.autarkia.energy.Span;
import dev.autarkia.registry.AutarkiaBlocks;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Handler {@code span_upgrade} (05 section 3.3 template 10): the path is a hot span from S08 plus the spans of the same
 * tier between it and the nearest generator node (max 16), upgraded to the next tier the colony tier unlocks (copper at
 * T2, insulated at T3, HV at T4, superconductor at T5, phase at T6).
 * <ul>
 * <li>Rope: every {@code rope_pulley} on the path becomes a {@code wooden_pole} (the pulley and the fences under it are
 * cut, the pole stands on the fence base or on the pulley spot), and every path span is strung again in copper.</li>
 * <li>Metal tiers: connector mapping {@code wooden_pole} to {@code steel_pylon}, {@code steel_pylon} to {@code hv_pylon},
 * {@code hv_pylon} to {@code cryo_node}, {@code cryo_node} to {@code phase_node} (the Cable-Layer strings the phase links
 * with no item, decision G3). A connector whose spans are all on the path is cut and the new one stands on its
 * base; a connector with other spans stays, and the new one stands beside it with a {@code transformer} between them
 * (old tier to the transformer, new tier from it), where the path meets the lower tier.</li>
 * </ul>
 * Machine ports stay; a port whose other spans are not on the path takes them into the path once, and a path with a
 * port-to-port span or a span over the max length of the new tier is rejected. After a {@code quench} memory the
 * superconductor max span is x0.75, and the handler first repairs the quenched span: a {@code cryo_node} under its
 * middle and two superconductor spans to its old end nodes (05 section 6.2).
 */
final class SpanUpgrade {
    private static final int MAX_SPANS = 16;

    private SpanUpgrade() {
    }

    static @Nullable Project propose(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        GridLevel g = Grid.of(level);
        Project repair = quenchRepair(c, level, id, tpl, g);
        if (repair != null) {
            return repair;
        }
        for (int spanId : ColonyRuntime.of(c).hotSpans) {
            Span hot = g.data().spans.get(spanId);
            if (hot == null) {
                continue;
            }
            Project pr = hot.tier == CableTier.ROPE ? build(c, level, id, tpl, g, hot) : upgrade(c, level, id, tpl, g, hot);
            if (pr != null) {
                return pr;
            }
        }
        return null;
    }

    /** Return the colony tier that unlocks a cable tier (05 section 3.3 "next tier unlocked"). */
    static int unlockTier(CableTier t) {
        return switch (t) {
            case ROPE -> 1;
            case COPPER -> 2;
            case INSULATED -> 3;
            case HV -> 4;
            case SUPERCONDUCTOR -> 5;
            case PHASE -> 6;
        };
    }

    /** Return the max span of a tier for planning: x0.75 for superconductor while a {@code quench} memory has weight 0.5 or more. */
    static double maxSpan(Colony c, ServerLevel level, CableTier t) {
        return t == CableTier.SUPERCONDUCTOR && Planner.signal(c, level, "mem.quench") >= 0.5 ? t.maxSpan * 0.75 : t.maxSpan;
    }

    private static @Nullable BlockState connectorFor(CableTier t) {
        return switch (t) {
            case INSULATED -> AutarkiaBlocks.STEEL_PYLON.get().defaultBlockState();
            case HV -> AutarkiaBlocks.HV_PYLON.get().defaultBlockState();
            case SUPERCONDUCTOR -> AutarkiaBlocks.CRYO_NODE.get().defaultBlockState();
            case PHASE -> AutarkiaBlocks.PHASE_NODE.get().defaultBlockState();
            default -> null;
        };
    }

    private static boolean upgradable(BlockState st, CableTier from) {
        return switch (from) {
            case COPPER -> st.is(AutarkiaBlocks.WOODEN_POLE.get());
            case INSULATED -> st.is(AutarkiaBlocks.STEEL_PYLON.get());
            case HV -> st.is(AutarkiaBlocks.HV_PYLON.get());
            case SUPERCONDUCTOR -> st.is(AutarkiaBlocks.CRYO_NODE.get());
            default -> false;
        };
    }

    private static @Nullable Project upgrade(Colony c, ServerLevel level, String id, ColonyTemplate tpl, GridLevel g, Span hot) {
        CableTier from = hot.tier;
        CableTier to = from.next();
        BlockState conn = connectorFor(to);
        if (conn == null || to == from || c.tier < unlockTier(to)) {
            return null;
        }
        Set<Span> path = new LinkedHashSet<>(pathToGenerator(g, hot));
        if (path.isEmpty() || path.size() > MAX_SPANS) {
            return null;
        }
        Map<Long, BlockPos> moved = new HashMap<>();
        List<Project.Cell> cells = new ArrayList<>();
        List<Project.SpanRec> spans = new ArrayList<>();
        LongArrayList cuts = new LongArrayList();
        for (Span s : path) {
            if (port(level, s.a) && port(level, s.b)) {
                return null;
            }
            for (long end : new long[] {s.a, s.b}) {
                if (moved.containsKey(end)) {
                    continue;
                }
                BlockPos node = BlockPos.of(end);
                boolean offPath = false;
                for (int i = 0; i < g.data().at(end).size(); i++) {
                    Span o = g.data().spans.get(g.data().at(end).getInt(i));
                    offPath |= o != null && !path.contains(o);
                }
                if (port(level, end)) {
                    if (offPath) {
                        return null;
                    }
                    moved.put(end, node);
                    continue;
                }
                if (!upgradable(level.getBlockState(node), from) || !Colonies.inside(c, node)) {
                    return null;
                }
                BlockPos spot = offPath ? beside(level, node, conn, cells) : node;
                if (spot == null || !fits(level, spot, conn, offPath ? null : node)) {
                    return null;
                }
                moved.put(end, spot);
                cells.add(new Project.Cell(spot.asLong(), conn));
                if (offPath) {
                    BlockPos tr = transformerSpot(level, node, spot, cells);
                    if (tr == null) {
                        return null;
                    }
                    cells.add(new Project.Cell(tr.asLong(), AutarkiaBlocks.TRANSFORMER.get().defaultBlockState()));
                    spans.add(new Project.SpanRec(node.asLong(), tr.asLong(), from));
                    spans.add(new Project.SpanRec(tr.asLong(), spot.asLong(), to));
                } else {
                    cuts.add(node.asLong());
                }
            }
        }
        double max = maxSpan(c, level, to);
        for (Span s : path) {
            BlockPos a = moved.get(s.a);
            BlockPos b = moved.get(s.b);
            if (Math.sqrt(a.distSqr(b)) > max) {
                return null;
            }
            spans.add(new Project.SpanRec(a.asLong(), b.asLong(), to));
        }
        if (cells.isEmpty()) {
            return null;
        }
        return ProjectFactory.handlerProject(c, level, id, tpl, "span_upgrade", BlockPos.of(cells.get(0).pos()), cells, spans, cuts.toLongArray());
    }

    /** Return whether the connector fits at its base (every position free, or part of the replaced connector at old). */
    private static boolean fits(ServerLevel level, BlockPos base, BlockState conn, @Nullable BlockPos old) {
        if (!(conn.getBlock() instanceof net.nikdo53.tinymultiblocklib.block.BaseMultiblock mb)) {
            return level.getBlockState(base).canBeReplaced() || old != null && base.equals(old);
        }
        BlockState center = conn.setValue(net.nikdo53.tinymultiblocklib.block.BaseMultiblock.CENTER, true);
        for (BlockPos p : mb.getMultiblockShape(level, base, center).getGlobalPositions()) {
            BlockState st = level.getBlockState(p);
            boolean replaced = old != null && st.getBlock() instanceof dev.autarkia.energy.GridConnector && p.getX() == old.getX() && p.getZ() == old.getZ();
            if (!st.canBeReplaced() && !replaced) {
                return false;
            }
        }
        BlockPos below = base.below();
        return level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP);
    }

    private static @Nullable BlockPos beside(ServerLevel level, BlockPos node, BlockState conn, List<Project.Cell> taken) {
        for (int r = 2; r <= 3; r++) {
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos p = node.relative(d, r);
                if (taken.stream().noneMatch(cl -> BlockPos.of(cl.pos()).distManhattan(p) < 2) && fits(level, p, conn, null)) {
                    return p;
                }
            }
        }
        return null;
    }

    private static @Nullable BlockPos transformerSpot(ServerLevel level, BlockPos oldNode, BlockPos newNode, List<Project.Cell> taken) {
        BlockPos mid = new BlockPos((oldNode.getX() + newNode.getX()) / 2, oldNode.getY(), (oldNode.getZ() + newNode.getZ()) / 2);
        for (BlockPos p : List.of(mid, mid.north(), mid.south(), mid.east(), mid.west())) {
            if (!p.equals(oldNode) && !p.equals(newNode) && taken.stream().noneMatch(cl -> cl.pos() == p.asLong()) && level.getBlockState(p).canBeReplaced()
                && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Return the repair project for a span {@code quench} memory (weight 0.5 or more; its key names the two end nodes)
     * whose span is gone while both end nodes still stand: a third {@code cryo_node} on the ground under the middle and
     * two superconductor spans; null when none applies (a coil quench has a one-position key).
     */
    private static @Nullable Project quenchRepair(Colony c, ServerLevel level, String id, ColonyTemplate tpl, GridLevel g) {
        double half = dev.autarkia.config.AutarkiaConfig.MEMORY_HALF_LIFE_TICKS.get();
        BlockState cryo = AutarkiaBlocks.CRYO_NODE.get().defaultBlockState();
        for (Colony.Memory m : c.memories) {
            String[] ends = m.key().split("(?<=\\d)-");
            if (!m.type().equals("quench") || ends.length != 2 || m.weightAt(c.clock, half) < 0.5 || c.tier < 5) {
                continue;
            }
            BlockPos a = BlockPos.of(Long.parseLong(ends[0]));
            BlockPos b = BlockPos.of(Long.parseLong(ends[1]));
            if (!level.getBlockState(a).is(AutarkiaBlocks.CRYO_NODE.get()) || !level.getBlockState(b).is(AutarkiaBlocks.CRYO_NODE.get())
                || Builds.strung(level, a.asLong(), b.asLong())) {
                continue;
            }
            BlockPos base = groundAt(level, new BlockPos((a.getX() + b.getX()) / 2, Math.min(a.getY(), b.getY()), (a.getZ() + b.getZ()) / 2));
            if (base == null || !fits(level, base, cryo, null) || !Colonies.inside(c, base)) {
                continue;
            }
            List<Project.Cell> cells = new ArrayList<>(List.of(new Project.Cell(base.asLong(), cryo)));
            List<Project.SpanRec> spans = List.of(new Project.SpanRec(a.asLong(), base.asLong(), CableTier.SUPERCONDUCTOR),
                new Project.SpanRec(base.asLong(), b.asLong(), CableTier.SUPERCONDUCTOR));
            return ProjectFactory.handlerProject(c, level, id, tpl, "span_upgrade", base, cells, spans, new long[0]);
        }
        return null;
    }

    private static @Nullable BlockPos groundAt(ServerLevel level, BlockPos near) {
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.getX(), near.getZ());
        BlockPos p = new BlockPos(near.getX(), y, near.getZ());
        return Math.abs(y - near.getY()) <= 8 ? p : null;
    }

    private static @Nullable Project build(Colony c, ServerLevel level, String id, ColonyTemplate tpl, GridLevel g, Span hot) {
        Set<Span> path = new LinkedHashSet<>(pathToGenerator(g, hot));
        for (Span s : List.copyOf(path)) {
            for (long end : new long[] {s.a, s.b}) {
                if (port(level, end)) {
                    IntArrayList ids = g.data().at(end);
                    for (int i = 0; i < ids.size(); i++) {
                        Span o = g.data().spans.get(ids.getInt(i));
                        if (o != null && o.tier == CableTier.ROPE) {
                            path.add(o);
                        }
                    }
                }
            }
        }
        if (path.isEmpty() || path.size() > MAX_SPANS) {
            return null;
        }
        Map<Long, BlockPos> moved = new HashMap<>();
        List<Project.Cell> cells = new ArrayList<>();
        LongArrayList cuts = new LongArrayList();
        for (Span s : path) {
            if (port(level, s.a) && port(level, s.b)) {
                return null;
            }
            for (long end : new long[] {s.a, s.b}) {
                if (moved.containsKey(end)) {
                    continue;
                }
                BlockPos node = BlockPos.of(end);
                if (port(level, end)) {
                    for (int i = 0; i < g.data().at(end).size(); i++) {
                        Span o = g.data().spans.get(g.data().at(end).getInt(i));
                        if (o != null && !path.contains(o)) {
                            return null;
                        }
                    }
                    moved.put(end, node);
                    continue;
                }
                if (!level.getBlockState(node).is(AutarkiaBlocks.ROPE_PULLEY.get()) || !Colonies.inside(c, node)) {
                    return null;
                }
                BlockPos pole = poleSpot(level, node, cuts);
                if (pole == null) {
                    return null;
                }
                moved.put(end, pole);
                cells.add(new Project.Cell(pole.asLong(), AutarkiaBlocks.WOODEN_POLE.get().defaultBlockState()));
            }
        }
        List<Project.SpanRec> spans = new ArrayList<>();
        for (Span s : path) {
            BlockPos a = moved.get(s.a);
            BlockPos b = moved.get(s.b);
            if (Math.sqrt(a.distSqr(b)) > CableTier.COPPER.maxSpan) {
                return null;
            }
            spans.add(new Project.SpanRec(a.asLong(), b.asLong(), CableTier.COPPER));
        }
        if (cells.isEmpty()) {
            return null;
        }
        return ProjectFactory.handlerProject(c, level, id, tpl, "span_upgrade", BlockPos.of(cells.get(0).pos()), cells, spans, cuts.toLongArray());
    }

    private static boolean port(ServerLevel level, long node) {
        return !(level.getBlockState(BlockPos.of(node)).getBlock() instanceof dev.autarkia.energy.GridConnector);
    }

    /** Return the pole center that replaces a pulley and add the blocks to cut, or null when 3 free blocks do not fit. */
    private static @Nullable BlockPos poleSpot(ServerLevel level, BlockPos pulley, LongArrayList cuts) {
        BlockPos base = pulley;
        while (level.getBlockState(base.below()).is(Blocks.OAK_FENCE) && pulley.getY() - base.getY() < 2) {
            base = base.below();
        }
        for (int y = base.getY(); y <= base.getY() + 2; y++) {
            BlockPos p = new BlockPos(base.getX(), y, base.getZ());
            BlockState st = level.getBlockState(p);
            if (p.getY() <= pulley.getY()) {
                cuts.add(p.asLong());
            } else if (!st.canBeReplaced()) {
                return null;
            }
        }
        BlockState under = level.getBlockState(base.below());
        return under.isFaceSturdy(level, base.below(), net.minecraft.core.Direction.UP) ? base : null;
    }

    /** Return the spans of the hot span's tier on the shortest node path from it to a node with a generator member. */
    private static List<Span> pathToGenerator(GridLevel g, Span hot) {
        GridNetwork net = g.network(hot.a);
        Set<Long> generators = new LinkedHashSet<>();
        if (net != null) {
            for (GridNetwork.Attached m : net.members) {
                if (m.member.gridRole() == GridMember.Role.GENERATOR) {
                    generators.add(m.node);
                }
            }
        }
        Map<Long, Span> via = new LinkedHashMap<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        via.put(hot.a, hot);
        via.put(hot.b, hot);
        queue.add(hot.a);
        queue.add(hot.b);
        List<Span> out = new ArrayList<>(List.of(hot));
        while (!queue.isEmpty()) {
            long cur = queue.poll();
            if (generators.contains(cur)) {
                long n = cur;
                while (via.get(n) != hot) {
                    Span s = via.get(n);
                    out.add(s);
                    n = s.a == n ? s.b : s.a;
                }
                return out.size() > MAX_SPANS ? List.of() : out;
            }
            IntArrayList ids = g.data().at(cur);
            for (int i = 0; i < ids.size(); i++) {
                Span s = g.data().spans.get(ids.getInt(i));
                if (s == null || s.tier != hot.tier) {
                    continue;
                }
                long other = s.a == cur ? s.b : s.a;
                if (!via.containsKey(other)) {
                    via.put(other, s);
                    queue.add(other);
                }
            }
        }
        return out;
    }
}

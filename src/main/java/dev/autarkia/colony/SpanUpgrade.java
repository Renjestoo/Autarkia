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
 * Handler {@code span_upgrade} (05 section 3.3 template 10, T2 part): the path is a hot rope span from S08 plus the rope
 * spans between it and the nearest generator node (max 16). Every {@code rope_pulley} on the path becomes a
 * {@code wooden_pole} (the pulley and the fences under it are cut, the pole stands on the fence base or on the pulley
 * spot), and every path span is strung again in copper. Machine ports stay; a port whose other spans are not on the path
 * takes them into the path once, and a path with a port-to-port span or a copper span over the max length is rejected.
 */
final class SpanUpgrade {
    private static final int MAX_SPANS = 16;

    private SpanUpgrade() {
    }

    static @Nullable Project propose(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        GridLevel g = Grid.of(level);
        for (int spanId : ColonyRuntime.of(c).hotSpans) {
            Span hot = g.data().spans.get(spanId);
            if (hot == null || hot.tier != CableTier.ROPE) {
                continue;
            }
            Project pr = build(c, level, id, tpl, g, hot);
            if (pr != null) {
                return pr;
            }
        }
        return null;
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

    /** Return the rope spans of the shortest node path from the hot span to a node with a generator member. */
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
                if (s == null || s.tier != CableTier.ROPE) {
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

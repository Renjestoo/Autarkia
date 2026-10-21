package dev.autarkia.colony;

import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GridNetwork;
import dev.autarkia.worker.WorkerEntity;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Incremental site search (05 section 3.4): candidates on a step-2 grid around the rule anchor, ordered by ring
 * distance then x then z (first 256), each tried in the four rotations, at most the configured number of evaluations
 * per tick. A candidate is levelled to the median floor, checked against the protection rules and scored
 * {@code S = wR*rule + wD*(1 - d/R) + wL*(1 - L/Lmax) + wA*adj}. Plans with a power port also need a rope route to the
 * anchor network (one relay pulley on two fences when the port is more than one max span away) whose straight chord
 * crosses neither the plan's own volume nor a solid block.
 */
public final class SiteSearch {
    private static final Rotation[] ROTATIONS = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};
    private static final int MAX_CANDIDATES = 256;

    /** A scored site with its leveling plan and network route. */
    public record Result(BlockPos origin, Rotation rotation, double score, double ruleScore, int floor, long[] cuts, long[] fills, int edits,
                         @Nullable BlockPos connectNode, @Nullable BlockPos relay) {
    }

    public final String template;
    private final ColonyTemplate tpl;
    private final ColonyPlans.Plan plan;
    private final BlockPos anchor;
    private final List<BlockPos> netNodes;
    private final List<long[]> candidates = new ArrayList<>();
    private final List<Result> accepted = new ArrayList<>();
    private final List<long[]> reserved;
    private int index;
    private int rot;
    private boolean done;

    private SiteSearch(String template, ColonyTemplate tpl, ColonyPlans.Plan plan, BlockPos anchor, List<BlockPos> netNodes, List<long[]> reserved) {
        this.template = template;
        this.tpl = tpl;
        this.plan = plan;
        this.anchor = anchor;
        this.netNodes = netNodes;
        this.reserved = reserved;
    }

    /** Start a search for a template, or return null when the rule has no anchor (then the template has no site). */
    public static @Nullable SiteSearch start(Colony c, ServerLevel level, String template, ColonyTemplate tpl, ColonyPlans.Plan plan) {
        ColonyRuntime rt = ColonyRuntime.of(c);
        String rule = tpl.site().rule();
        List<BlockPos> netNodes = new ArrayList<>();
        BlockPos anchor;
        if (rule.equals("SR_NEAR_NET") || rule.equals("SR_HIGH_OPEN")) {
            GridNetwork best = null;
            for (GridNetwork n : Grid.of(level).networks()) {
                if (rt.networks.contains(n.id) && (best == null || n.avgDemand - n.avgGeneration > best.avgDemand - best.avgGeneration)) {
                    best = n;
                }
            }
            if (best == null) {
                return null;
            }
            long sx = 0;
            long sy = 0;
            long sz = 0;
            for (int i = 0; i < best.nodes.size(); i++) {
                BlockPos p = BlockPos.of(best.nodes.getLong(i));
                netNodes.add(p);
                sx += p.getX();
                sy += p.getY();
                sz += p.getZ();
            }
            BlockPos centroid = new BlockPos((int) (sx / netNodes.size()), (int) (sy / netNodes.size()), (int) (sz / netNodes.size()));
            netNodes.sort(Comparator.comparingDouble((BlockPos p) -> p.distSqr(centroid)).thenComparingLong(BlockPos::asLong));
            anchor = netNodes.get(0);
        } else if (rule.equals("SR_NEAR_STORAGE")) {
            anchor = centroid(rt.storages, c.corePos);
        } else if (rule.equals("SR_WORK_CENTER")) {
            List<BlockPos> work = new ArrayList<>();
            for (Colony.WorkerEntry w : c.workers.values()) {
                if (level.getEntity(w.id()) instanceof WorkerEntity we) {
                    work.add(we.workPos());
                }
            }
            anchor = centroid(work, c.corePos);
        } else {
            throw new IllegalStateException("Site rule " + rule + " of template " + template + " has no search (handler rules place directly)");
        }
        List<long[]> reserved = new ArrayList<>();
        for (Project p : c.openProjects()) {
            reserved.add(box(p));
        }
        SiteSearch s = new SiteSearch(template, tpl, plan, anchor, netNodes, reserved);
        s.buildCandidates(c);
        return s;
    }

    private static BlockPos centroid(List<BlockPos> list, BlockPos fallback) {
        if (list.isEmpty()) {
            return fallback;
        }
        long x = 0;
        long y = 0;
        long z = 0;
        for (BlockPos p : list) {
            x += p.getX();
            y += p.getY();
            z += p.getZ();
        }
        return new BlockPos((int) (x / list.size()), (int) (y / list.size()), (int) (z / list.size()));
    }

    private static long[] box(Project p) {
        int x0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        int z0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int y1 = Integer.MIN_VALUE;
        int z1 = Integer.MIN_VALUE;
        LongArrayList all = new LongArrayList();
        p.cells.forEach(c -> all.add(c.pos()));
        all.addElements(all.size(), p.cuts);
        all.addElements(all.size(), p.fills);
        for (long l : all) {
            BlockPos b = BlockPos.of(l);
            x0 = Math.min(x0, b.getX());
            y0 = Math.min(y0, b.getY());
            z0 = Math.min(z0, b.getZ());
            x1 = Math.max(x1, b.getX());
            y1 = Math.max(y1, b.getY());
            z1 = Math.max(z1, b.getZ());
        }
        return all.isEmpty() ? new long[] {0, 0, 0, -1, -1, -1} : new long[] {x0, y0, z0, x1, y1, z1};
    }

    private void buildCandidates(Colony c) {
        int r = tpl.site().radius();
        List<long[]> list = new ArrayList<>();
        for (int dx = -r; dx <= r; dx += 2) {
            for (int dz = -r; dz <= r; dz += 2) {
                int x = anchor.getX() + dx;
                int z = anchor.getZ() + dz;
                if (Colonies.inside(c, new BlockPos(x, 0, z))) {
                    list.add(new long[] {Math.max(Math.abs(dx), Math.abs(dz)), x, z});
                }
            }
        }
        list.sort(Comparator.<long[]>comparingLong(a -> a[0]).thenComparingLong(a -> a[1]).thenComparingLong(a -> a[2]));
        candidates.addAll(list.subList(0, Math.min(MAX_CANDIDATES, list.size())));
    }

    /** Return whether every candidate was evaluated. */
    public boolean done() {
        return done;
    }

    /** Evaluate up to {@code budget} (candidate, rotation) pairs. */
    public void step(Colony c, ServerLevel level, int budget) {
        for (int n = 0; n < budget && !done; n++) {
            if (index >= candidates.size()) {
                done = true;
                return;
            }
            long[] cand = candidates.get(index);
            Result r = evaluate(c, level, (int) cand[1], (int) cand[2], ROTATIONS[rot]);
            if (r != null) {
                accepted.add(r);
            }
            if (++rot == ROTATIONS.length) {
                rot = 0;
                index++;
            }
        }
    }

    /** Return the best site after the search ends, or null when none scores at least 0.2. */
    public @Nullable Result best() {
        if (accepted.isEmpty()) {
            return null;
        }
        List<Result> scored = accepted;
        if (tpl.site().rule().equals("SR_HIGH_OPEN")) {
            int minF = scored.stream().mapToInt(Result::floor).min().orElse(0);
            int maxF = scored.stream().mapToInt(Result::floor).max().orElse(0);
            double wR = tpl.site().weights().get(0);
            scored = scored.stream().map(r -> {
                double rule = (double) (r.floor() - minF) / Math.max(1, maxF - minF);
                return new Result(r.origin(), r.rotation(), r.score() + wR * rule, rule, r.floor(), r.cuts(), r.fills(), r.edits(), r.connectNode(), r.relay());
            }).toList();
        }
        Result best = null;
        for (Result r : scored) {
            if (best == null || r.score() > best.score() + 1e-9
                || Math.abs(r.score() - best.score()) <= 1e-9 && r.origin().asLong() < best.origin().asLong()) {
                best = r;
            }
        }
        return best == null || best.score() < 0.2 ? null : best;
    }

    /** Evaluate one candidate center with one rotation (card Rotate button); null when the site fails a rule. */
    public @Nullable Result evaluateAt(Colony c, ServerLevel level, int cx, int cz, Rotation r) {
        Result res = evaluate(c, level, cx, cz, r);
        if (res == null || !tpl.site().rule().equals("SR_HIGH_OPEN")) {
            return res;
        }
        return new Result(res.origin(), res.rotation(), res.score() + tpl.site().weights().get(0), 1, res.floor(), res.cuts(), res.fills(), res.edits(),
            res.connectNode(), res.relay());
    }

    private @Nullable Result evaluate(Colony c, ServerLevel level, int cx, int cz, Rotation r) {
        Vec3i size = ColonyPlans.rotatedSize(plan.size(), r);
        int x0 = cx - size.getX() / 2;
        int z0 = cz - size.getZ() / 2;
        int x1 = x0 + size.getX() - 1;
        int z1 = z0 + size.getZ() - 1;
        if (!Colonies.inside(c, new BlockPos(x0 - 1, 0, z0 - 1)) || !Colonies.inside(c, new BlockPos(x1 + 1, 0, z1 + 1))) {
            return null;
        }
        long chunk = ChunkPos.pack(x0 >> 4, z0 >> 4);
        for (Colony.DeniedSite d : c.template(template).deniedSites) {
            if (d.chunk() == chunk && d.until() > c.clock) {
                return null;
            }
        }
        int cols = size.getX() * size.getZ();
        int[] h = new int[cols];
        int k = 0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    return null;
                }
                h[k++] = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            }
        }
        int[] sorted = h.clone();
        java.util.Arrays.sort(sorted);
        int floor = sorted[(sorted.length - 1) / 2];
        int maxDiff = ColonyTiers.maxColumnDiff(c.tier);
        LongArrayList cuts = new LongArrayList();
        LongArrayList fills = new LongArrayList();
        int edits = 0;
        k = 0;
        int sy = size.getY();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int hc = h[k++];
                if (Math.abs(hc - floor) > maxDiff) {
                    return null;
                }
                for (int y = floor + sy; y >= floor + 1; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState st = level.getBlockState(p);
                    if (!st.isAir() && !st.canBeReplaced()) {
                        cuts.add(p.asLong());
                        edits += st.is(BlockTags.LOGS) || st.is(BlockTags.LEAVES) ? 2 : 1;
                    }
                }
                for (int y = hc + 1; y <= floor; y++) {
                    fills.add(new BlockPos(x, y, z).asLong());
                    edits++;
                }
            }
        }
        int maxEdits = ColonyTiers.maxLevelingEdits(c.tier);
        if (edits > maxEdits || !free(c, level, x0 - 1, floor, z0 - 1, x1 + 1, floor + sy, z1 + 1)) {
            return null;
        }
        BlockPos origin = new BlockPos(x0, floor + 1, z0);
        ColonyPlans.Placed placed = ColonyPlans.place(plan, origin, r);
        BlockPos connect = null;
        BlockPos relay = null;
        double rule = 1;
        String ruleId = tpl.site().rule();
        if (placed.port().isPresent() && !netNodes.isEmpty()) {
            BlockPos port = placed.port().get();
            connect = nearestFreeNode(level, port);
            if (connect == null) {
                return null;
            }
            double dist = Math.sqrt(port.distSqr(connect));
            if (dist > CableTier.ROPE.maxSpan) {
                if (ruleId.equals("SR_NEAR_NET") || dist > 2 * CableTier.ROPE.maxSpan) {
                    return null;
                }
                relay = relaySpot(c, level, port, connect);
                if (relay == null) {
                    return null;
                }
            }
            boolean clear = relay == null ? routeClear(level, placed, port, connect)
                : routeClear(level, placed, port, relay) && routeClear(level, placed, relay, connect);
            if (!clear) {
                return null;
            }
        }
        if (ruleId.equals("SR_HIGH_OPEN") && !level.canSeeSky(new BlockPos(cx, floor + sy + 1, cz))) {
            return null;
        }
        List<Double> w = tpl.site().weights();
        double d = Math.sqrt(Math.pow(cx - anchor.getX(), 2) + Math.pow(cz - anchor.getZ(), 2));
        int gap = Math.max(Math.max(x0 - anchor.getX(), anchor.getX() - x1), Math.max(z0 - anchor.getZ(), anchor.getZ() - z1));
        double adj = 1.0 / (1 + Math.max(0, gap) / 4.0);
        double base = w.get(1) * Math.max(0, 1 - d / tpl.site().radius()) + w.get(2) * (1 - (double) edits / maxEdits) + w.get(3) * adj;
        double score = ruleId.equals("SR_HIGH_OPEN") ? base : base + w.get(0) * rule;
        return new Result(origin, r, score, rule, floor, cuts.toLongArray(), fills.toLongArray(), edits, connect, relay);
    }

    private @Nullable BlockPos nearestFreeNode(ServerLevel level, BlockPos port) {
        BlockPos best = null;
        for (BlockPos n : netNodes) {
            int count = Grid.of(level).data().at(n.asLong()).size();
            if (count >= 4) {
                continue;
            }
            if (best == null || n.distSqr(port) < best.distSqr(port) || n.distSqr(port) == best.distSqr(port) && n.asLong() < best.asLong()) {
                best = n;
            }
        }
        return best;
    }

    private @Nullable BlockPos relaySpot(Colony c, ServerLevel level, BlockPos a, BlockPos b) {
        int x = (a.getX() + b.getX()) / 2;
        int z = (a.getZ() + b.getZ()) / 2;
        int g = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pulley = new BlockPos(x, g + 2, z);
        if (!Colonies.inside(c, pulley) || pulley.distSqr(a) > CableTier.ROPE.maxSpan * CableTier.ROPE.maxSpan
            || pulley.distSqr(b) > CableTier.ROPE.maxSpan * CableTier.ROPE.maxSpan) {
            return null;
        }
        for (int y = g; y <= g + 2; y++) {
            BlockState st = level.getBlockState(new BlockPos(x, y, z));
            if (!st.isAir() && !st.canBeReplaced()) {
                return null;
            }
        }
        return pulley;
    }

    private static boolean routeClear(ServerLevel level, ColonyPlans.Placed placed, BlockPos a, BlockPos b) {
        Vec3 pa = Vec3.atCenterOf(a);
        Vec3 pb = Vec3.atCenterOf(b);
        int steps = Math.max(2, (int) Math.ceil(pa.distanceTo(pb) / 0.25));
        for (int i = 1; i < steps; i++) {
            BlockPos p = BlockPos.containing(pa.lerp(pb, i / (double) steps));
            if (p.equals(a) || p.equals(b)) {
                continue;
            }
            if (p.getX() >= placed.min().getX() && p.getX() <= placed.max().getX() && p.getY() >= placed.min().getY() && p.getY() <= placed.max().getY()
                && p.getZ() >= placed.min().getZ() && p.getZ() <= placed.max().getZ()) {
                return false;
            }
            boolean ownPart = level.getBlockEntity(p) instanceof IMultiBlockEntity m && (m.getCenter().equals(a) || m.getCenter().equals(b));
            if (!ownPart && !level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean free(Colony c, ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (long[] b : reserved) {
            if (x0 <= b[3] && x1 >= b[0] && y0 <= b[4] && y1 >= b[1] && z0 <= b[5] && z1 >= b[2]) {
                return false;
            }
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                p.set(x, 0, z);
                if (Colonies.at(level, p) != c) {
                    return false;
                }
                for (int y = y0; y <= y1; y++) {
                    p.set(x, y, z);
                    BlockState st = level.getBlockState(p);
                    if (st.hasBlockEntity() || st.is(dev.autarkia.registry.AutarkiaTags.PLANNER_PROTECTED) || st.getFluidState().isSource()
                        || st.is(Blocks.BEDROCK)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}

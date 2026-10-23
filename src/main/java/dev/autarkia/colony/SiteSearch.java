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
 * crosses neither the plan's own volume nor a solid block; a connector port only joins a node whose tier it takes. T4
 * rules: SR_WATER_COLUMN anchors on the tallest falling water column ({@code water.column}), levels on the ocean
 * floor heightmap and scores min(1, head / 12) for the water above the turbine intake (rejected below 6 or with a
 * blocked outlet); SR_PERIMETER anchors on the perimeter point with the most S23 events. T6 rule: SR_EVENT_CLUSTER
 * anchors on the {@code machine_destroyed} cluster that no Restoration Field covers ({@link Restoration#clusterAnchor}).
 */
public final class SiteSearch {
    private static final Rotation[] ROTATIONS = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};
    private static final int MAX_CANDIDATES = 256;

    /**
     * A scored site with its leveling plan and network route. A {@code bridge} is a transformer between a port that cannot
     * take the network tier and the connect node: the port is strung in {@code portTier}, the bridge to the node in
     * {@code linkTier}.
     */
    public record Result(BlockPos origin, Rotation rotation, double score, double ruleScore, int floor, long[] cuts, long[] fills, int edits,
                         @Nullable BlockPos connectNode, @Nullable BlockPos relay, CableTier linkTier, @Nullable BlockPos bridge, CableTier portTier) {
        Result withRule(double add, double rule) {
            return new Result(origin, rotation, score + add, rule, floor, cuts, fills, edits, connectNode, relay, linkTier, bridge, portTier);
        }
    }

    private boolean heightRule() {
        String r = tpl.site().rule();
        return r.equals("SR_HIGH_OPEN") || r.equals("SR_PEAK");
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

    /**
     * Start a search for a template, or return null when the rule has no anchor (then the template has no site). Open
     * projects reserve their bounding box, a pylon line only the space around its cells (its box would span the whole
     * route), and a project without cells nothing; watched Pattern instances reserve their box.
     */
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
        } else if (rule.equals("SR_FLOW_WATER") || rule.equals("SR_PEAK")) {
            anchor = surfaceAnchor(c, level, rule.equals("SR_FLOW_WATER"));
            if (anchor == null) {
                return null;
            }
        } else if (rule.equals("SR_WATER_COLUMN")) {
            if (rt.waterColumn == null) {
                return null;
            }
            anchor = new BlockPos(rt.waterColumn[0], rt.waterColumn[1], rt.waterColumn[2]);
        } else if (rule.equals("SR_PERIMETER")) {
            List<BlockPos> edge = FrontierSignals.perimeter(c);
            int best = 0;
            for (int i = 1; i < edge.size(); i++) {
                best = c.stats.getOrDefault("S23.edge." + i, 0L) > c.stats.getOrDefault("S23.edge." + best, 0L) ? i : best;
            }
            anchor = edge.get(best);
        } else if (rule.equals("SR_EVENT_CLUSTER")) {
            anchor = Restoration.clusterAnchor(c);
            if (anchor == null) {
                return null;
            }
        } else {
            throw new IllegalStateException("Site rule " + rule + " of template " + template + " has no search (handler rules place directly)");
        }
        if ((rule.equals("SR_FLOW_WATER") || rule.equals("SR_PEAK") || rule.equals("SR_WATER_COLUMN") || rule.equals("SR_PERIMETER")
            || rule.equals("SR_EVENT_CLUSTER"))
            && plan.port().isPresent()) {
            for (GridNetwork n : Grid.of(level).networks()) {
                if (rt.networks.contains(n.id)) {
                    for (int i = 0; i < n.nodes.size(); i++) {
                        netNodes.add(BlockPos.of(n.nodes.getLong(i)));
                    }
                }
            }
            BlockPos a = anchor;
            netNodes.sort(Comparator.comparingDouble((BlockPos p) -> p.distSqr(a)).thenComparingLong(BlockPos::asLong));
        }
        List<long[]> reserved = new ArrayList<>();
        for (Project p : c.openProjects()) {
            if (OutpostProjects.sequential(p)) {
                for (Project.Cell cell : p.cells) {
                    BlockPos b = BlockPos.of(cell.pos());
                    reserved.add(new long[] {b.getX() - 1L, b.getY(), b.getZ() - 1L, b.getX() + 1L, b.getY() + 8L, b.getZ() + 1L});
                }
            } else if (!p.cells.isEmpty() || p.cuts.length > 0 || p.fills.length > 0) {
                reserved.add(box(p));
            }
        }
        for (Colony.Instance in : c.instances) {
            if (in.watched()) {
                BlockPos o = in.origin();
                reserved.add(new long[] {o.getX(), o.getY(), o.getZ(), o.getX() + in.size().getX() - 1L, o.getY() + in.size().getY() - 1L,
                    o.getZ() + in.size().getZ() - 1L});
            }
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
                if (Colonies.insideActive(c, new BlockPos(x, 0, z))) {
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
        if (heightRule()) {
            int minF = scored.stream().mapToInt(Result::floor).min().orElse(0);
            int maxF = scored.stream().mapToInt(Result::floor).max().orElse(0);
            double wR = tpl.site().weights().get(0);
            scored = scored.stream().map(r -> {
                double rule = (double) (r.floor() - minF) / Math.max(1, maxF - minF);
                return r.withRule(wR * rule, rule);
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
        if (res == null || !heightRule()) {
            return res;
        }
        return res.withRule(tpl.site().weights().get(0), 1);
    }

    private @Nullable Result evaluate(Colony c, ServerLevel level, int cx, int cz, Rotation r) {
        Vec3i size = ColonyPlans.rotatedSize(plan.size(), r);
        int x0 = cx - size.getX() / 2;
        int z0 = cz - size.getZ() / 2;
        int x1 = x0 + size.getX() - 1;
        int z1 = z0 + size.getZ() - 1;
        if (!Colonies.insideActive(c, new BlockPos(x0 - 1, 0, z0 - 1)) || !Colonies.insideActive(c, new BlockPos(x1 + 1, 0, z1 + 1))
            || !Colonies.insideActive(c, new BlockPos(x0 - 1, 0, z1 + 1)) || !Colonies.insideActive(c, new BlockPos(x1 + 1, 0, z0 - 1))) {
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
        String ruleId = tpl.site().rule();
        Heightmap.Types ground = ruleId.equals("SR_WATER_COLUMN") ? Heightmap.Types.OCEAN_FLOOR : Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    return null;
                }
                h[k++] = level.getHeight(ground, x, z) - 1;
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
        boolean water = ruleId.equals("SR_FLOW_WATER") || ruleId.equals("SR_WATER_COLUMN");
        if (edits > maxEdits || !free(c, level, x0 - 1, floor, z0 - 1, x1 + 1, floor + sy, z1 + 1, water)) {
            return null;
        }
        double rule = 1;
        if (ruleId.equals("SR_FLOW_WATER")) {
            rule = flowScore(level, x0, z0, x1, z1, floor, r);
            if (rule <= 0) {
                return null;
            }
        }
        if (ruleId.equals("SR_PEAK") && machineNear(level, cx, floor, cz)) {
            return null;
        }
        BlockPos origin = new BlockPos(x0, floor + 1, z0);
        ColonyPlans.Placed placed = ColonyPlans.place(plan, origin, r);
        if (ruleId.equals("SR_WATER_COLUMN")) {
            rule = columnScore(level, placed);
            if (rule <= 0) {
                return null;
            }
        }
        BlockPos connect = null;
        BlockPos relay = null;
        BlockPos bridge = null;
        CableTier link = CableTier.ROPE;
        CableTier portTier = CableTier.ROPE;
        if (placed.port().isPresent() && !netNodes.isEmpty()) {
            BlockPos port = placed.port().get();
            BlockState portState = placed.cells().stream().filter(cl -> cl.rel().equals(port)).map(ColonyPlans.Cell::state).findFirst().orElse(null);
            connect = nearestFreeNode(level, port, portState, true);
            boolean fitting = connect != null;
            if (connect == null) {
                connect = nearestFreeNode(level, port, portState, false);
            }
            if (connect == null) {
                return null;
            }
            link = linkTier(level, connect);
            portTier = link;
            if (!fitting) {
                if (c.tier < SpanUpgrade.unlockTier(CableTier.INSULATED)) {
                    return null;
                }
                portTier = Grid.accepts(portState, CableTier.ROPE) ? CableTier.ROPE : CableTier.COPPER;
                bridge = bridgeSpot(c, level, placed, port, connect, portTier, link);
                if (bridge == null || !routeClear(level, placed, port, bridge) || !routeClear(level, placed, bridge, connect)) {
                    return null;
                }
            } else {
                double dist = Math.sqrt(port.distSqr(connect));
                if (dist > link.maxSpan) {
                    if (ruleId.equals("SR_NEAR_NET") || dist > 2 * link.maxSpan) {
                        return null;
                    }
                    relay = relaySpot(c, level, port, connect, link);
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
        }
        if (ruleId.equals("SR_HIGH_OPEN") && !level.canSeeSky(new BlockPos(cx, floor + sy + 1, cz))) {
            return null;
        }
        List<Double> w = tpl.site().weights();
        double d = Math.sqrt(Math.pow(cx - anchor.getX(), 2) + Math.pow(cz - anchor.getZ(), 2));
        int gap = Math.max(Math.max(x0 - anchor.getX(), anchor.getX() - x1), Math.max(z0 - anchor.getZ(), anchor.getZ() - z1));
        double adj = 1.0 / (1 + Math.max(0, gap) / 4.0);
        double base = w.get(1) * Math.max(0, 1 - d / tpl.site().radius()) + w.get(2) * (1 - (double) edits / maxEdits) + w.get(3) * adj;
        double score = heightRule() ? base : base + w.get(0) * rule;
        return new Result(origin, r, score, rule, floor, cuts.toLongArray(), fills.toLongArray(), edits, connect, relay, link, bridge, portTier);
    }

    /**
     * Return the cable tier for a span to a network node: copper when the node is a connector that takes copper, else
     * superconductor, HV or insulated when it takes that tier (cryo nodes, pylons), else rope; the tier of its spans when
     * it is a machine port, else rope.
     */
    public static CableTier linkTier(ServerLevel level, BlockPos node) {
        Grid.Node n = Grid.resolve(level, node);
        if (n != null && n.connector() != null) {
            if (n.connector().acceptsTier(CableTier.COPPER)) {
                return CableTier.COPPER;
            }
            for (CableTier t : java.util.List.of(CableTier.SUPERCONDUCTOR, CableTier.HV, CableTier.INSULATED)) {
                if (n.connector().acceptsTier(t)) {
                    return t;
                }
            }
            return CableTier.ROPE;
        }
        it.unimi.dsi.fastutil.ints.IntArrayList ids = Grid.of(level).data().at(node.asLong());
        for (int i = 0; i < ids.size(); i++) {
            dev.autarkia.energy.Span s = Grid.of(level).data().spans.get(ids.getInt(i));
            if (s != null) {
                return s.tier;
            }
        }
        return CableTier.ROPE;
    }

    private static @Nullable BlockPos surfaceAnchor(Colony c, ServerLevel level, boolean water) {
        int h = ColonyTiers.domainHalf(c.tier);
        long sx = 0;
        long sz = 0;
        int n = 0;
        BlockPos peak = null;
        for (int x = c.corePos.getX() - h; x <= c.corePos.getX() + h; x++) {
            for (int z = c.corePos.getZ() - h; z <= c.corePos.getZ() + h; z++) {
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockPos p = new BlockPos(x, y, z);
                if (water) {
                    var f = level.getFluidState(p);
                    if (f.is(net.minecraft.tags.FluidTags.WATER) && !f.isSource()) {
                        sx += x;
                        sz += z;
                        n++;
                    }
                } else if (peak == null || y > peak.getY() || y == peak.getY() && p.asLong() < peak.asLong()) {
                    peak = p;
                }
            }
        }
        if (!water) {
            return peak;
        }
        return n == 0 ? null : new BlockPos((int) (sx / n), c.corePos.getY(), (int) (sz / n));
    }

    private static double flowScore(ServerLevel level, int x0, int z0, int x1, int z1, int floor, Rotation r) {
        boolean alongX = r == Rotation.CLOCKWISE_90 || r == Rotation.COUNTERCLOCKWISE_90;
        int n = 0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                BlockPos p = new BlockPos(x, floor, z);
                var f = level.getFluidState(p);
                if (f.is(net.minecraft.tags.FluidTags.WATER) && !f.isSource()) {
                    Vec3 flow = f.getFlow(level, p);
                    if (Math.abs(alongX ? flow.x : flow.z) > 1e-3) {
                        n++;
                    }
                }
            }
        }
        return Math.min(1, n / 3.0);
    }

    private static boolean machineNear(ServerLevel level, int cx, int y, int cz) {
        for (int chx = (cx - 8) >> 4; chx <= (cx + 8) >> 4; chx++) {
            for (int chz = (cz - 8) >> 4; chz <= (cz + 8) >> 4; chz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(chx, chz);
                if (chunk == null) {
                    continue;
                }
                for (net.minecraft.world.level.block.entity.BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (be instanceof dev.autarkia.energy.GridMember && Math.abs(p.getX() - cx) < 8 && Math.abs(p.getZ() - cz) < 8 && Math.abs(p.getY() - y) < 8) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Return the head score of a hydro plan (rule SR_WATER_COLUMN): min(1, h / 12) for the water blocks h straight
     * above the turbine intake, 0 when h is below 6 or the outlet in front of the turbine is not air or water.
     */
    private static double columnScore(ServerLevel level, ColonyPlans.Placed placed) {
        for (ColonyPlans.Cell cell : placed.cells()) {
            if (!cell.state().is(dev.autarkia.registry.AutarkiaBlocks.HYDRO_TURBINE.get())) {
                continue;
            }
            BlockState out = level.getBlockState(cell.rel().relative(dev.autarkia.registry.MachineBlock.front(cell.state()), 2));
            int head = 0;
            BlockPos.MutableBlockPos p = cell.rel().above(2).mutable();
            while (head < dev.autarkia.energy.HydroTurbineBlockEntity.MAX_HEAD && level.getBlockState(p.move(0, 1, 0)).is(Blocks.WATER)) {
                head++;
            }
            return head < 6 || !out.isAir() && !out.is(Blocks.WATER) ? 0 : Math.min(1, head / 12.0);
        }
        return 0;
    }

    /**
     * Return the nearest network node with room for a span; with {@code fitting} only nodes whose tier the port block
     * takes (rope on rope sheaves and pulleys, metal cables on bushings and metal connectors), else any node.
     */
    private @Nullable BlockPos nearestFreeNode(ServerLevel level, BlockPos port, @Nullable BlockState portState, boolean fitting) {
        BlockPos best = null;
        for (BlockPos n : netNodes) {
            int count = Grid.of(level).data().at(n.asLong()).size();
            if (count >= 4 || (fitting || portState != null && portState.getBlock() instanceof dev.autarkia.energy.GridConnector)
                && !Grid.accepts(portState, linkTier(level, n))) {
                continue;
            }
            if (best == null || n.distSqr(port) < best.distSqr(port) || n.distSqr(port) == best.distSqr(port) && n.asLong() < best.asLong()) {
                best = n;
            }
        }
        return best;
    }

    /**
     * Return the ground spot of a bridge transformer 2 to 3 blocks from the connect node, within reach of both tiers, free,
     * on a sturdy block, inside the domain and outside the plan; null when none fits.
     */
    private @Nullable BlockPos bridgeSpot(Colony c, ServerLevel level, ColonyPlans.Placed placed, BlockPos port, BlockPos node, CableTier portTier,
                                          CableTier link) {
        BlockPos best = null;
        for (int r = 2; r <= 3; r++) {
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos col = node.relative(d, r);
                BlockPos p = new BlockPos(col.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col.getX(), col.getZ()), col.getZ());
                boolean inPlan = placed.cells().stream().anyMatch(cl -> cl.rel().equals(p));
                if (inPlan || !Colonies.inside(c, p) || !level.getBlockState(p).canBeReplaced()
                    || !level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)
                    || Math.sqrt(p.distSqr(port)) > portTier.maxSpan || Math.sqrt(p.distSqr(node)) > link.maxSpan) {
                    continue;
                }
                if (best == null || p.distSqr(port) < best.distSqr(port)) {
                    best = p;
                }
            }
        }
        return best;
    }

    private @Nullable BlockPos relaySpot(Colony c, ServerLevel level, BlockPos a, BlockPos b, CableTier tier) {
        int x = (a.getX() + b.getX()) / 2;
        int z = (a.getZ() + b.getZ()) / 2;
        int g = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pulley = new BlockPos(x, tier == CableTier.ROPE ? g + 2 : g, z);
        if (!Colonies.inside(c, pulley) || pulley.distSqr(a) > tier.maxSpan * tier.maxSpan || pulley.distSqr(b) > tier.maxSpan * tier.maxSpan) {
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

    private boolean free(Colony c, ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, boolean water) {
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
                    if (st.hasBlockEntity() || st.is(dev.autarkia.registry.AutarkiaTags.PLANNER_PROTECTED) || !water && st.getFluidState().isSource()
                        || st.is(Blocks.BEDROCK)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}

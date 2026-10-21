package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.CableTier;
import dev.autarkia.logistics.CableCarStationBlockEntity;
import dev.autarkia.logistics.CableGeometry;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.storage.DepotBlockEntity;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * The handlers of templates 21 to 23 (05 sections 3.3 and 8.1) and what their completion does:
 * <ul>
 * <li>{@code outpost}: the deposit of {@link FrontierSignals#needDeposit}; the plan is an enabled OUTPOST Pattern of the
 * library, else {@code colony/outpost_basic}, whose anchor is the {@code outpost_core}; the site search (rule
 * SR_DEPOSIT, ruleScore {@code 1 - d/24}) runs on the step-2 grid around the deposit (chunks loaded on demand), the
 * plan must lie inside the new outpost square and hold a {@code depot} in the core chunk, and the 3x3 columns around
 * the core need neither fill nor cut in its two layers (the core is founded before the site is levelled);</li>
 * <li>{@code hv_link}: a pylon line ({@link PylonRoute}) for the first outpost without an HV link, else a parallel line 3
 * blocks aside for an HV-linked outpost whose link load reached 0.8; built and strung one cell at a time from the
 * outpost; on completion the outpost link gains HV and the colony fires {@code pylon_line_complete};</li>
 * <li>{@code cable_car}: two stations, one beside the outpost depot and one beside the nearest main storage at most 128
 * blocks away, free cable paths checked with the station geometry; on completion they are linked, the line runs 2
 * gondolas and standing deliveries bring their parts (8 {@code steel_plate}, 4 {@code iron_chain}) into the part slots
 * of the owner station.</li>
 * </ul>
 * Jobs of these projects, surveys and escorts are long trips with no assignment distance limit (05 section 7.4).
 */
public final class OutpostProjects {
    private static final Set<String> FAR = Set.of("outpost", "hv_link", "cable_car");
    private static final Rotation[] ROTATIONS = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};
    private static final int MAX_CANDIDATES = 256;
    private static final int CAR_MAX_PATH = 128;
    private static final int GONDOLAS = 2;

    /** A planned cable car: the outpost station center and the main station center with their facing states. */
    public record CarRoute(BlockPos outpost, BlockState outpostState, BlockPos main, BlockState mainState) {
    }

    private OutpostProjects() {
    }

    /** Return whether the project places and strings one cell at a time in route order (a pylon line). */
    static boolean sequential(Project p) {
        return p.template.getPath().equals("hv_link");
    }

    /** Return whether a job may go to a worker at any distance. */
    static boolean longTrip(Colony c, ColonyJob j) {
        if (j.type == ColonyJob.Type.SURVEY || j.type == ColonyJob.Type.ESCORT || j.type == ColonyJob.Type.FOUND_OUTPOST) {
            return true;
        }
        Project p = j.projectId == null ? null : c.project(j.projectId);
        return p != null && FAR.contains(p.template.getPath());
    }

    /** Return the horizontal distance between two positions. */
    public static double horizontal(BlockPos a, BlockPos b) {
        return Outposts.horizontal(a, b);
    }

    /** Return whether a position lies in the Core square (not counting outposts and beacons). */
    public static boolean inMain(Colony c, BlockPos p) {
        int h = ColonyTiers.domainHalf(c.tier);
        return Math.abs(p.getX() - c.corePos.getX()) <= h && Math.abs(p.getZ() - c.corePos.getZ()) <= h;
    }

    /** Return the outpost whose square holds the position, or null. */
    public static Colony.@Nullable Outpost outpostAt(Colony c, BlockPos p) {
        int h = ColonyTiers.outpostHalf(c.tier);
        for (Colony.Outpost o : c.outposts) {
            if (Math.abs(p.getX() - o.corePos.getX()) <= h && Math.abs(p.getZ() - o.corePos.getZ()) <= h) {
                return o;
            }
        }
        return null;
    }

    private static boolean pending(Colony c, String template, Colony.Outpost o) {
        return c.openProjects().stream().anyMatch(p -> p.template.getPath().equals(template) && outpostAt(c, p.origin) == o);
    }

    private static boolean hasHv(Colony.Outpost o) {
        return o.link == Colony.Link.HV || o.link == Colony.Link.BOTH;
    }

    /** Return the first outpost with a free HV pylon in its loaded chunks, no HV link and no open {@code hv_link} project, or null. */
    public static Colony.@Nullable Outpost unlinked(Colony c, ServerLevel level) {
        for (Colony.Outpost o : c.outposts) {
            if (!hasHv(o) && !pending(c, "hv_link", o) && PylonRoute.start(level, c, o, false) != null) {
                return o;
            }
        }
        return null;
    }

    /** Return the highest link load (S06 of the link spans) over the HV-linked outposts with a loaded pylon. */
    public static double linkLoad(Colony c, ServerLevel level) {
        double max = 0;
        for (Colony.Outpost o : c.outposts) {
            BlockPos s = hasHv(o) ? PylonRoute.start(level, c, o, false) : null;
            if (s != null) {
                max = Math.max(max, PylonRoute.load(level, c, s));
            }
        }
        return max;
    }

    /** Handler {@code outpost} (template 21). */
    static @Nullable Project outpost(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        Colony.Deposit d = FrontierSignals.needDeposit(c, level, ColonyRuntime.of(c));
        if (d == null) {
            return null;
        }
        ColonyPlans.Plan plan = outpostPlan(level, c, tpl);
        SiteSearch.Result site = outpostSite(c, level, tpl, plan, d.pos());
        if (site == null) {
            ColonyRuntime.of(c).log("No outpost site near the " + d.ore() + " deposit at " + d.pos().toShortString());
            return null;
        }
        return ProjectFactory.fromSite(c, level, id, tpl, plan, site);
    }

    private static ColonyPlans.Plan outpostPlan(ServerLevel level, Colony c, ColonyTemplate tpl) {
        for (Colony.LibraryEntry e : c.library) {
            Pattern pt = e.enabled() && e.role().equalsIgnoreCase("OUTPOST") ? Pattern.load(level.getServer(), e.pattern()) : null;
            ColonyPlans.Plan plan = pt == null ? null : patternPlan(pt);
            if (plan != null) {
                return plan;
            }
        }
        return ColonyPlans.plan(tpl.plan().orElseThrow());
    }

    /** Return a Pattern as a plan anchored on its {@code outpost_core}, or null when it has none. */
    private static ColonyPlans.@Nullable Plan patternPlan(Pattern pt) {
        Set<Integer> parts = Set.copyOf(pt.parts());
        List<ColonyPlans.Cell> cells = new ArrayList<>();
        BlockPos anchor = null;
        for (int y = 0; y < pt.size().getY(); y++) {
            for (int z = 0; z < pt.size().getZ(); z++) {
                for (int x = 0; x < pt.size().getX(); x++) {
                    int i = pt.index(x, y, z);
                    BlockState st = pt.state(i);
                    if (st == null || st.isAir() || parts.contains(i)) {
                        continue;
                    }
                    cells.add(new ColonyPlans.Cell(new BlockPos(x, y, z), st));
                    if (st.is(AutarkiaBlocks.OUTPOST_CORE.get())) {
                        anchor = new BlockPos(x, y, z);
                    }
                }
            }
        }
        if (anchor == null) {
            return null;
        }
        List<ColonyPlans.SpanSpec> spans = pt.spans().stream().map(s -> new ColonyPlans.SpanSpec(s.a(), s.b(), s.tier())).toList();
        return new ColonyPlans.Plan(Autarkia.id(Pattern.fileId(pt.id())), pt.size(), cells, spans, Optional.empty(), Optional.empty(), anchor, "none");
    }

    private static SiteSearch.@Nullable Result outpostSite(Colony c, ServerLevel level, ColonyTemplate tpl, ColonyPlans.Plan plan, BlockPos deposit) {
        int r = tpl.site().radius();
        List<int[]> cands = new ArrayList<>();
        for (int dx = -r; dx <= r; dx += 2) {
            for (int dz = -r; dz <= r; dz += 2) {
                cands.add(new int[] {Math.max(Math.abs(dx), Math.abs(dz)), deposit.getX() + dx, deposit.getZ() + dz});
            }
        }
        cands.sort(Comparator.<int[]>comparingInt(a -> a[0]).thenComparingInt(a -> a[1]).thenComparingInt(a -> a[2]));
        SiteSearch.Result best = null;
        for (int[] cand : cands.subList(0, Math.min(MAX_CANDIDATES, cands.size()))) {
            for (Rotation rot : ROTATIONS) {
                SiteSearch.Result res = evaluate(c, level, tpl, plan, deposit, cand[1], cand[2], rot);
                if (res != null && (best == null || res.score() > best.score() + 1e-9)) {
                    best = res;
                }
            }
        }
        return best == null || best.score() < 0.2 ? null : best;
    }

    private static SiteSearch.@Nullable Result evaluate(Colony c, ServerLevel level, ColonyTemplate tpl, ColonyPlans.Plan plan, BlockPos deposit,
                                                        int cx, int cz, Rotation rot) {
        Vec3i size = ColonyPlans.rotatedSize(plan.size(), rot);
        int x0 = cx - size.getX() / 2;
        int z0 = cz - size.getZ() / 2;
        int x1 = x0 + size.getX() - 1;
        int z1 = z0 + size.getZ() - 1;
        int[] h = new int[size.getX() * size.getZ()];
        int k = 0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                level.getChunk(x >> 4, z >> 4);
                h[k++] = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            }
        }
        int[] sorted = h.clone();
        java.util.Arrays.sort(sorted);
        int floor = sorted[(sorted.length - 1) / 2];
        BlockPos origin = new BlockPos(x0, floor + 1, z0);
        ColonyPlans.Placed placed = ColonyPlans.place(plan, origin, rot);
        BlockPos core = placed.anchor();
        int half = ColonyTiers.outpostHalf(c.tier);
        if (Math.max(Math.max(core.getX() - (x0 - 1), x1 + 1 - core.getX()), Math.max(core.getZ() - (z0 - 1), z1 + 1 - core.getZ())) > half
            || Outposts.invalid(level, c, core) != null || placed.cells().stream().noneMatch(cl -> cl.state().is(AutarkiaBlocks.DEPOT.get())
                && (cl.rel().getX() >> 4) == (core.getX() >> 4) && (cl.rel().getZ() >> 4) == (core.getZ() >> 4))) {
            return null;
        }
        LongArrayList cuts = new LongArrayList();
        LongArrayList fills = new LongArrayList();
        int edits = 0;
        k = 0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int hc = h[k++];
                boolean coreColumn = Math.abs(x - core.getX()) <= 1 && Math.abs(z - core.getZ()) <= 1;
                if (Math.abs(hc - floor) > ColonyTiers.maxColumnDiff(c.tier) || coreColumn && hc != floor) {
                    return null;
                }
                for (int y = floor + size.getY(); y >= floor + 1; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState st = level.getBlockState(p);
                    if (!st.isAir() && !st.canBeReplaced()) {
                        if (coreColumn && y <= floor + 2) {
                            return null;
                        }
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
        if (edits > maxEdits || !free(c, level, x0 - 1, floor, z0 - 1, x1 + 1, floor + size.getY(), z1 + 1)) {
            return null;
        }
        double d = Math.sqrt(Math.pow(cx - deposit.getX(), 2) + Math.pow(cz - deposit.getZ(), 2));
        double rule = Math.max(0, 1 - d / tpl.site().radius());
        int gap = Math.max(Math.max(x0 - deposit.getX(), deposit.getX() - x1), Math.max(z0 - deposit.getZ(), deposit.getZ() - z1));
        List<Double> w = tpl.site().weights();
        double score = w.get(0) * rule + w.get(1) * rule + w.get(2) * (1 - (double) edits / maxEdits) + w.get(3) / (1 + Math.max(0, gap) / 4.0);
        return new SiteSearch.Result(origin, rot, score, rule, floor, cuts.toLongArray(), fills.toLongArray(), edits, null, null, CableTier.ROPE);
    }

    private static boolean free(Colony c, ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (Project p : c.openProjects()) {
            for (Project.Cell cell : p.cells) {
                BlockPos b = BlockPos.of(cell.pos());
                if (b.getX() >= x0 && b.getX() <= x1 && b.getY() >= y0 && b.getY() <= y1 && b.getZ() >= z0 && b.getZ() <= z1) {
                    return false;
                }
            }
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                Colony other = Colonies.at(level, p.set(x, 0, z));
                if (other != null && other != c) {
                    return false;
                }
                for (int y = y0; y <= y1; y++) {
                    BlockState st = level.getBlockState(p.set(x, y, z));
                    if (st.hasBlockEntity() || st.is(dev.autarkia.registry.AutarkiaTags.PLANNER_PROTECTED) || st.getFluidState().isSource()
                        || st.is(Blocks.BEDROCK)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Handler {@code hv_link} (template 22). */
    static @Nullable Project hvLink(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        for (Colony.Outpost o : c.outposts) {
            if (pending(c, "hv_link", o)) {
                continue;
            }
            BlockPos start = PylonRoute.start(level, c, o, true);
            boolean parallel = hasHv(o);
            if (start == null || parallel && PylonRoute.load(level, c, start) < 0.8) {
                continue;
            }
            for (int lateral : parallel ? new int[] {3, -3} : new int[] {0}) {
                PylonRoute.Plan route = PylonRoute.plan(level, c, start, lateral);
                if (route != null) {
                    return ProjectFactory.orderedProject(c, level, id, tpl, "hv_link", start, new ArrayList<>(route.cells()), new ArrayList<>(route.spans()),
                        route.cuts());
                }
            }
            ColonyRuntime.of(c).log("No pylon route from outpost " + (c.outposts.indexOf(o) + 1));
        }
        return null;
    }

    /** Handler {@code cable_car} (template 23): the outpost with the highest S25 that has a free cable car route. */
    static @Nullable Project cableCar(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        Colony.Outpost best = null;
        CarRoute route = null;
        for (Colony.Outpost o : c.outposts) {
            if (o.link == Colony.Link.CABLE_CAR || o.link == Colony.Link.BOTH || pending(c, "cable_car", o)
                || best != null && FrontierSignals.flow(c, o) <= FrontierSignals.flow(c, best)) {
                continue;
            }
            CarRoute r = cableCarRoute(c, level, o);
            if (r != null) {
                best = o;
                route = r;
            }
        }
        if (route == null) {
            return null;
        }
        List<Project.Cell> cells = new ArrayList<>(List.of(new Project.Cell(route.outpost().asLong(), route.outpostState()),
            new Project.Cell(route.main().asLong(), route.mainState())));
        return ProjectFactory.orderedProject(c, level, id, tpl, "cable_car", route.outpost(), cells, new ArrayList<>(), new long[0]);
    }

    /**
     * Return the cable car route of an outpost: its first depot (core chunk first) and the nearest storage of the Core
     * square at most 128 blocks apart, a station beside each, facing each other, whose line passes the station link
     * checks; null when there is none.
     */
    public static @Nullable CarRoute cableCarRoute(Colony c, ServerLevel level, Colony.Outpost o) {
        List<DepotBlockEntity> depots = Outposts.depots(level, o);
        if (depots.isEmpty()) {
            return null;
        }
        BlockPos depot = depots.get(0).getBlockPos();
        BlockPos store = ColonyRuntime.of(c).storages.stream().filter(p -> inMain(c, p)).min(Comparator.comparingDouble((BlockPos p) -> horizontal(p, depot))
            .thenComparingLong(BlockPos::asLong)).orElse(null);
        if (store == null || horizontal(store, depot) > CAR_MAX_PATH) {
            return null;
        }
        BlockState[] outState = new BlockState[1];
        BlockState[] mainState = new BlockState[1];
        BlockPos a = stationSpot(level, depot, store, outState);
        BlockPos b = stationSpot(level, store, depot, mainState);
        if (a == null || b == null) {
            return null;
        }
        BaseMultiblock mb = (BaseMultiblock) AutarkiaBlocks.CABLE_CAR_STATION.get();
        Set<BlockPos> fa = mb.getMultiblockShape(level, a, outState[0]).getGlobalPositions();
        Set<BlockPos> fb = mb.getMultiblockShape(level, b, mainState[0]).getGlobalPositions();
        boolean aOwns = a.asLong() < b.asLong();
        CableGeometry geo = new CableGeometry(CableGeometry.anchor(aOwns ? fa : fb), CableGeometry.anchor(aOwns ? fb : fa));
        if (geo.horizontal < CableCarStationBlockEntity.MIN_LENGTH || geo.horizontal > dev.autarkia.config.AutarkiaConfig.CABLE_CAR_MAX_LENGTH.get()
            || Math.abs(geo.b.y - geo.a.y) > CableCarStationBlockEntity.MAX_HEIGHT
            || geo.firstBlocked(level, st -> st.is(AutarkiaBlocks.CABLE_CAR_STATION.get())) != null) {
            return null;
        }
        return new CarRoute(a, outState[0], b, mainState[0]);
    }

    /**
     * Return a station center whose footprint touches the block at {@code near}, stands on sturdy ground and is free,
     * facing {@code toward}; the spot nearest {@code toward} wins. The chosen state goes into {@code state[0]}.
     */
    private static @Nullable BlockPos stationSpot(ServerLevel level, BlockPos near, BlockPos toward, BlockState[] state) {
        Direction facing = Direction.getApproximateNearest(toward.getX() - near.getX(), 0, toward.getZ() - near.getZ());
        BlockState st = AutarkiaBlocks.CABLE_CAR_STATION.get().defaultBlockState().setValue(BaseMultiblock.CENTER, true)
            .setValue(HorizontalDirectionalBlock.FACING, facing);
        BaseMultiblock mb = (BaseMultiblock) st.getBlock();
        BlockPos best = null;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos center = near.offset(dx, dy, dz);
                    if (best != null && horizontal(center, toward) >= horizontal(best, toward)) {
                        continue;
                    }
                    Set<BlockPos> fp = mb.getMultiblockShape(level, center, st).getGlobalPositions();
                    int bottom = fp.stream().mapToInt(BlockPos::getY).min().orElse(center.getY());
                    boolean touches = fp.stream().anyMatch(p -> p.distManhattan(near) == 1);
                    boolean ok = touches && !fp.contains(near) && fp.stream().allMatch(p -> {
                        BlockState s = level.getBlockState(p);
                        return Builds.replaceable(s) && s.getFluidState().isEmpty()
                            && (p.getY() != bottom || level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP));
                    });
                    if (ok) {
                        best = center;
                    }
                }
            }
        }
        state[0] = st;
        return best;
    }

    /** Apply the completion of an {@code hv_link} or {@code cable_car} project. */
    static void finish(Colony c, ServerLevel level, Project p) {
        Colony.Outpost o = outpostAt(c, p.origin);
        if (o == null) {
            Autarkia.LOG.warn("[colony] {} finished {} at {}, but no outpost holds its start", c.name, p.template.getPath(), p.origin.toShortString());
            return;
        }
        if (p.template.getPath().equals("hv_link")) {
            o.link = o.link.with(Colony.Link.HV);
            ColonyEvents.worker(c, "pylon_line_complete");
            ColonyRuntime.of(c).log("Outpost " + (c.outposts.indexOf(o) + 1) + " linked by HV");
            return;
        }
        BlockPos a = BlockPos.of(p.cells.get(0).pos());
        BlockPos b = BlockPos.of(p.cells.get(1).pos());
        CableCarStationBlockEntity.Link link = CableCarStationBlockEntity.link(level, a, b, false);
        if (link.result() != CableCarStationBlockEntity.Result.OK) {
            ColonyRuntime.of(c).log("Cable car link failed: " + link.result().name().toLowerCase(java.util.Locale.ROOT)
                + (link.blocked() == null ? "" : " at " + link.blocked().toShortString()));
            return;
        }
        o.link = o.link.with(Colony.Link.CABLE_CAR);
        BlockPos owner = a.asLong() < b.asLong() ? a : b;
        if (level.getBlockEntity(owner) instanceof CableCarStationBlockEntity s && s.ownerLine() != null) {
            s.ownerLine().setTarget(GONDOLAS);
        }
        c.deliveries.add(new Colony.Delivery(owner, AutarkiaItems.STEEL_PLATE.get(), 4 * GONDOLAS));
        c.deliveries.add(new Colony.Delivery(owner, Items.IRON_CHAIN, 2 * GONDOLAS));
        ColonyRuntime.of(c).log("Outpost " + (c.outposts.indexOf(o) + 1) + " linked by cable car");
    }
}

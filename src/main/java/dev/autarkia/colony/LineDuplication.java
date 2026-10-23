package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GridConnector;
import dev.autarkia.energy.GridNetwork;
import dev.autarkia.logistics.BeltBlock;
import dev.autarkia.processing.MachineBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.MachineBlock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Line duplication (05 sections 3.3 template 16 and 5.6, W11): when a machine type that an enabled LINE Pattern holds
 * has a long queue and no idle machines, the colony places a copy of the Pattern near those machines (flat ground,
 * nothing in the way, nearest first, 4 rotations) and handler {@code connect_line} adds the three connections: the feed
 * belt cell nearest the site becomes a {@code belt_splitter} whose left or right side (set when the copy finishes) starts a belt path (same Y, at most 48 cells) to
 * the ITEM_IN port; a belt path from ITEM_OUT to the nearest storage or belt (at most 48); and a span from the POWER
 * port to the nearest node of a network with spare supply for the copy's machines (average generation minus average
 * work draw; buffer refills do not count). Any missing route stops the
 * proposal ("no belt route" or "no power route"). At most 4 copies per Pattern. The finished copy becomes a watched
 * instance.
 */
public final class LineDuplication {
    private static final int MAX_ROUTE = 48;
    private static final int MAX_COPIES = 4;
    private static final String PLAN_PREFIX = "line/";

    private LineDuplication() {
    }

    /** Return the block id paths in the enabled LINE Patterns of the library. */
    public static Set<String> lineTypes(Colony c, ServerLevel level) {
        Set<String> out = new LinkedHashSet<>();
        for (Pattern p : linePatterns(c, level)) {
            for (BlockState st : p.palette()) {
                if (st != null) {
                    out.add(BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath());
                }
            }
        }
        return out;
    }

    private static List<Pattern> linePatterns(Colony c, ServerLevel level) {
        List<Pattern> out = new ArrayList<>();
        for (Colony.LibraryEntry e : c.library) {
            if (e.enabled() && e.role().equalsIgnoreCase("LINE")) {
                Pattern p = Pattern.load(level.getServer(), e.pattern());
                if (p != null) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    /** Handler {@code connect_line}: return the duplicate_line proposal, or null with the reason in the board log. */
    public static @Nullable Project propose(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        ColonyRuntime rt = ColonyRuntime.of(c);
        String best = null;
        double queue = 0;
        for (String type : lineTypes(c, level)) {
            double q = rt.snapshot.getOrDefault("S12." + type, 0.0);
            if (rt.snapshot.getOrDefault("S13." + type, 1.0) < 0.1 && q > queue) {
                best = type;
                queue = q;
            }
        }
        if (best == null) {
            return null;
        }
        String type = best;
        List<MachineBlockEntity> machines = rt.machines.stream().map(level::getBlockEntity).filter(be -> be instanceof MachineBlockEntity m
            && BuiltInRegistries.BLOCK.getKey(m.getBlockState().getBlock()).getPath().equals(type)).map(be -> (MachineBlockEntity) be).toList();
        for (Pattern p : linePatterns(c, level)) {
            if (p.palette().stream().noneMatch(st -> st != null && BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath().equals(type))
                || c.instances.stream().filter(in -> in.pattern().equals(p.id())).count() > MAX_COPIES) {
                continue;
            }
            Project pr = build(c, level, id, tpl, p, machines, queue);
            if (pr != null) {
                return pr;
            }
        }
        return null;
    }

    private static @Nullable Project build(Colony c, ServerLevel level, String id, ColonyTemplate tpl, Pattern p, List<MachineBlockEntity> machines,
                                           double queue) {
        if (machines.isEmpty()) {
            return null;
        }
        Vec3 centroid = Vec3.ZERO;
        for (MachineBlockEntity m : machines) {
            centroid = centroid.add(Vec3.atCenterOf(m.getBlockPos()));
        }
        centroid = centroid.scale(1.0 / machines.size());
        Site site = site(c, level, p, BlockPos.containing(centroid));
        if (site == null) {
            ColonyRuntime.of(c).log("No site: duplicate_line");
            return null;
        }
        Function<BlockPos, BlockPos> map = ColonyPlans.mapper(p.size(), site.origin, site.rotation);
        Map<Long, BlockState> cells = new HashMap<>();
        int sx = p.size().getX();
        int sz = p.size().getZ();
        for (int i = 0; i < p.volume(); i++) {
            BlockState st = p.state(i);
            if (st != null) {
                cells.put(map.apply(new BlockPos(i % sx, i / (sx * sz), i / sx % sz)).asLong(), st.rotate(site.rotation));
            }
        }
        List<Project.SpanRec> spans = new ArrayList<>();
        for (Pattern.SpanRecord s : p.spans()) {
            spans.add(new Project.SpanRec(map.apply(s.a()).asLong(), map.apply(s.b()).asLong(), s.tier()));
        }
        List<Long> cuts = new ArrayList<>();
        Pattern.Port in = port(p, Pattern.PortKind.ITEM_IN);
        Pattern.Port out = port(p, Pattern.PortKind.ITEM_OUT);
        Pattern.Port power = port(p, Pattern.PortKind.POWER);
        if (in != null && !feed(level, machines, map.apply(in.rel()), site.rotation.rotate(in.dir()), cells, cuts)) {
            ColonyRuntime.of(c).log("duplicate_line: no belt route to the copy's input at " + map.apply(in.rel()).toShortString() + " (site "
                + site.origin.toShortString() + " " + site.rotation + ")");
            return null;
        }
        if (out != null && !output(c, level, map.apply(out.rel()), site.rotation.rotate(out.dir()), cells)) {
            ColonyRuntime.of(c).log("duplicate_line: no belt route from the copy's output at " + map.apply(out.rel()).toShortString() + " (site "
                + site.origin.toShortString() + " " + site.rotation + ")");
            return null;
        }
        if (power != null && !power(level, machines, p, map.apply(power.rel()), cells.get(map.apply(power.rel()).asLong()), spans)) {
            ColonyRuntime.of(c).log("duplicate_line: no power route");
            return null;
        }
        List<Project.Cell> list = new ArrayList<>();
        cells.forEach((pos, st) -> list.add(new Project.Cell(pos, st)));
        UUID instance = UUID.randomUUID();
        Project pr = ProjectFactory.handlerProject(c, level, id, tpl, PLAN_PREFIX + p.id() + "/" + instance, site.origin, list, spans,
            cuts.stream().mapToLong(Long::longValue).toArray());
        pr.rotation = site.rotation;
        pr.reason = new Project.Reason("machine_queue", List.of((double) Math.round(queue)));
        return pr;
    }

    private record Site(BlockPos origin, Rotation rotation) {
    }

    private static @Nullable Site site(Colony c, ServerLevel level, Pattern p, BlockPos anchor) {
        List<int[]> candidates = new ArrayList<>();
        for (int r = 0; r <= 24; r += 2) {
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dz = -r; dz <= r; dz += 2) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r) {
                        candidates.add(new int[] {anchor.getX() + dx, anchor.getZ() + dz});
                    }
                }
            }
        }
        for (int[] xz : candidates.subList(0, Math.min(256, candidates.size()))) {
            for (Rotation rot : List.of(Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90)) {
                BlockPos origin = new BlockPos(xz[0], anchor.getY() - 1, xz[1]);
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos o = origin.above(dy);
                    if (fits(c, level, p, o, rot)) {
                        return new Site(o, rot);
                    }
                }
            }
        }
        return null;
    }

    private static boolean fits(Colony c, ServerLevel level, Pattern p, BlockPos origin, Rotation rot) {
        net.minecraft.core.Vec3i size = ColonyPlans.rotatedSize(p.size(), rot);
        for (int x = -1; x <= size.getX(); x++) {
            for (int z = -1; z <= size.getZ(); z++) {
                for (int y = 0; y < size.getY(); y++) {
                    BlockPos q = origin.offset(x, y, z);
                    if (!level.isLoaded(q) || !Colonies.insideActive(c, q) || !level.getBlockState(q).canBeReplaced()
                        || level.getBlockEntity(q) != null) {
                        return false;
                    }
                }
                BlockPos floor = origin.offset(x, -1, z);
                if (x >= 0 && z >= 0 && x < size.getX() && z < size.getZ() && !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                    return false;
                }
            }
        }
        return c.instances.stream().noneMatch(in -> overlaps(in, origin, size));
    }

    private static boolean overlaps(Colony.Instance in, BlockPos o, net.minecraft.core.Vec3i s) {
        return o.getX() <= in.origin().getX() + in.size().getX() && in.origin().getX() <= o.getX() + s.getX()
            && o.getZ() <= in.origin().getZ() + in.size().getZ() && in.origin().getZ() <= o.getZ() + s.getZ()
            && o.getY() <= in.origin().getY() + in.size().getY() && in.origin().getY() <= o.getY() + s.getY();
    }

    private static Pattern.@Nullable Port port(Pattern p, Pattern.PortKind kind) {
        return p.ports().stream().filter(x -> x.kind() == kind).findFirst().orElse(null);
    }

    /** Return the belt cells that feed the machines (MachineView.inputBelt), walked back along the feed line. */
    private static List<BlockPos> feedLine(ServerLevel level, List<MachineBlockEntity> machines) {
        List<BlockPos> out = new ArrayList<>();
        for (MachineBlockEntity m : machines) {
            Set<BlockPos> parts = m.getBlockState().getBlock() instanceof net.nikdo53.tinymultiblocklib.block.BaseMultiblock mb
                ? mb.getMultiblockShape(level, m.getBlockPos(), m.getBlockState()).getGlobalPositions() : Set.of(m.getBlockPos());
            for (BlockPos part : parts) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos b = part.relative(d.getOpposite());
                    if (!parts.contains(b) && BeltBlock.beltFacing(level.getBlockState(b), d)) {
                        BlockPos cur = b;
                        for (int k = 0; k < MAX_ROUTE && level.getBlockState(cur).getBlock() instanceof BeltBlock && !out.contains(cur); k++) {
                            out.add(cur.immutable());
                            cur = cur.relative(level.getBlockState(cur).getValue(MachineBlock.HORIZONTAL_FACING).getOpposite());
                        }
                    }
                }
            }
        }
        return out;
    }

    private static boolean feed(ServerLevel level, List<MachineBlockEntity> machines, BlockPos inPort, Direction inDir, Map<Long, BlockState> cells,
                                List<Long> cuts) {
        BlockPos goal = inPort.relative(inDir);
        Direction into = inDir.getOpposite();
        for (int k = 0; k < MAX_ROUTE && cells.containsKey(goal.asLong()) && cells.get(goal.asLong()).getBlock() instanceof BeltBlock; k++) {
            into = cells.get(goal.asLong()).getValue(MachineBlock.HORIZONTAL_FACING);
            goal = goal.relative(into.getOpposite());
        }
        BlockPos entry = goal;
        Direction last = into;
        List<BlockPos> line = new ArrayList<>(feedLine(level, machines));
        line.sort(java.util.Comparator.comparingDouble((BlockPos b) -> b.distSqr(entry)).thenComparingLong(BlockPos::asLong));
        for (BlockPos split : line) {
            Direction f = level.getBlockState(split).getValue(MachineBlock.HORIZONTAL_FACING);
            for (Direction side : List.of(f.getCounterClockWise(), f.getClockWise())) {
                List<BlockPos> path = route(level, split.relative(side), entry, cells, split.getY());
                if (path == null) {
                    continue;
                }
                cuts.add(split.asLong());
                cells.put(split.asLong(), AutarkiaBlocks.BELT_SPLITTER.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, f));
                lay(path, last, cells);
                return true;
            }
        }
        return false;
    }

    private static boolean output(Colony c, ServerLevel level, BlockPos outPort, Direction outDir, Map<Long, BlockState> cells) {
        BlockPos exit = outPort.relative(outDir);
        if (!walkable(level, exit, cells)) {
            return false;
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(exit));
        Map<BlockPos, BlockPos> from = new HashMap<>();
        from.put(exit, exit);
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos next = cur.relative(d);
                boolean storage = level.getBlockEntity(next) instanceof ColonyStorage && Colonies.inside(c, next);
                if (storage || level.getBlockState(next).getBlock() instanceof BeltBlock && !cells.containsKey(next.asLong())) {
                    List<BlockPos> path = new ArrayList<>();
                    for (BlockPos p = cur; ; p = from.get(p)) {
                        path.add(0, p);
                        if (p.equals(exit)) {
                            break;
                        }
                    }
                    lay(path, d, cells);
                    return true;
                }
                if (!from.containsKey(next) && walkable(level, next, cells) && depth(from, next, cur) <= MAX_ROUTE) {
                    from.put(next, cur);
                    queue.add(next);
                }
            }
        }
        return false;
    }

    private static int depth(Map<BlockPos, BlockPos> from, BlockPos next, BlockPos cur) {
        int n = 1;
        for (BlockPos p = cur; !from.get(p).equals(p); p = from.get(p)) {
            n++;
        }
        return n;
    }

    private static boolean walkable(ServerLevel level, BlockPos p, Map<Long, BlockState> cells) {
        return !cells.containsKey(p.asLong()) && level.getBlockState(p).canBeReplaced() && level.getBlockEntity(p) == null
            && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP);
    }

    private static @Nullable List<BlockPos> route(ServerLevel level, BlockPos start, BlockPos goal, Map<Long, BlockState> cells, int y) {
        if (start.getY() != y || goal.getY() != y || !walkable(level, start, cells) || !walkable(level, goal, cells)) {
            return null;
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(start));
        Map<BlockPos, BlockPos> from = new HashMap<>();
        from.put(start, start);
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            if (cur.equals(goal)) {
                List<BlockPos> path = new ArrayList<>();
                for (BlockPos p = cur; ; p = from.get(p)) {
                    path.add(0, p);
                    if (p.equals(start)) {
                        return path.size() <= MAX_ROUTE ? path : null;
                    }
                }
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos next = cur.relative(d);
                if (!from.containsKey(next) && walkable(level, next, cells) && Math.abs(next.getX() - start.getX()) + Math.abs(next.getZ() - start.getZ())
                    <= MAX_ROUTE) {
                    from.put(next, cur);
                    queue.add(next);
                }
            }
        }
        return null;
    }

    private static void lay(List<BlockPos> path, Direction last, Map<Long, BlockState> cells) {
        for (int i = 0; i < path.size(); i++) {
            BlockPos p = path.get(i);
            Direction f = i + 1 < path.size() ? Direction.getApproximateNearest(path.get(i + 1).getX() - p.getX(), 0, path.get(i + 1).getZ() - p.getZ())
                : last;
            cells.put(p.asLong(), AutarkiaBlocks.BELT.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, f));
        }
    }

    private static boolean power(ServerLevel level, List<MachineBlockEntity> machines, Pattern p, BlockPos node, @Nullable BlockState nodeState,
                                 List<Project.SpanRec> spans) {
        double demand = 0;
        Map<String, Integer> perType = new HashMap<>();
        for (int i = 0; i < p.volume(); i++) {
            BlockState st = p.state(i);
            if (st != null) {
                perType.merge(BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath(), 1, Integer::sum);
            }
        }
        for (MachineBlockEntity m : machines) {
            String type = BuiltInRegistries.BLOCK.getKey(m.getBlockState().getBlock()).getPath();
            if (m.energy() != null && perType.containsKey(type)) {
                demand += m.energy().capacity() / 200.0 * perType.remove(type);
            }
        }
        GridNetwork best = null;
        BlockPos target = null;
        for (GridNetwork net : Grid.of(level).networks()) {
            CableTier tier = net.tier == CableTier.PHASE ? CableTier.INSULATED : net.tier;
            if (net.avgGeneration - net.avgWork < demand || !Grid.accepts(nodeState, tier)) {
                continue;
            }
            for (int i = 0; i < net.nodes.size(); i++) {
                BlockPos n = BlockPos.of(net.nodes.getLong(i));
                if (level.getBlockState(n).getBlock() instanceof GridConnector && Math.sqrt(n.distSqr(node)) <= net.tier.maxSpan
                    && (target == null || n.distSqr(node) < target.distSqr(node))) {
                    best = net;
                    target = n;
                }
            }
        }
        if (best == null) {
            return false;
        }
        spans.add(new Project.SpanRec(node.asLong(), target.asLong(), best.tier == CableTier.PHASE ? CableTier.INSULATED : best.tier));
        return true;
    }

    /** Register the finished copy as a watched instance and restore its block entity configuration. */
    public static void finish(Colony c, ServerLevel level, Project pr) {
        String path = pr.plan.getPath();
        if (!path.startsWith(PLAN_PREFIX)) {
            return;
        }
        String[] ids = path.substring(PLAN_PREFIX.length()).split("/");
        UUID pattern = UUID.fromString(ids[0]);
        UUID instance = UUID.fromString(ids[1]);
        Pattern p = Pattern.load(level.getServer(), pattern);
        if (p == null) {
            return;
        }
        c.instances.add(new Colony.Instance(instance, pattern, pr.origin, pr.rotation, ColonyPlans.rotatedSize(p.size(), pr.rotation), true,
            new long[0]));
        PatternRepair.restore(c, level, pr, instance);
        java.util.Set<Long> placed = new java.util.HashSet<>();
        pr.cells.forEach(cl -> placed.add(cl.pos()));
        for (Project.Cell cl : pr.cells) {
            BlockPos at = BlockPos.of(cl.pos());
            if (cl.state().is(AutarkiaBlocks.BELT_SPLITTER.get()) && level.getBlockEntity(at) instanceof dev.autarkia.logistics.FilterBlockEntity splitter) {
                splitter.setRightSide(placed.contains(at.relative(cl.state().getValue(MachineBlock.HORIZONTAL_FACING).getClockWise()).asLong()));
            }
        }
        Autarkia.LOG.info("[colony] {} duplicated line {} at {}", c.name, p.name(), pr.origin.toShortString());
    }
}

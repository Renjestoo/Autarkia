package dev.autarkia.colony;

import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.Span;
import dev.autarkia.registry.AutarkiaBlocks;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * Turns a found site or a handler result into a project: world cells and spans (with the network route and relay
 * pulley), bill of materials, cost {@code C = sum w(b) + 0.25 L + 0.5 spanLength + 2 newConnectors} (05 section
 * 3.5), reason, urgency and priority. After a {@code rack_collapse} memory a T3 colony plans steel racks instead of wood
 * racks (05 section 6.2).
 */
public final class ProjectFactory {
    private ProjectFactory() {
    }

    /** Build the proposal for a template at its best site. */
    public static Project fromSite(Colony c, ServerLevel level, String id, ColonyTemplate tpl, SiteSearch.Result site) {
        return fromSite(c, level, id, tpl, ColonyPlans.plan(tpl.plan().orElseThrow()), site);
    }

    /** Build the proposal for a plan (a built-in plan or an OUTPOST Pattern) at a site. */
    static Project fromSite(Colony c, ServerLevel level, String id, ColonyTemplate tpl, ColonyPlans.Plan plan, SiteSearch.Result site) {
        ColonyPlans.Placed placed = ColonyPlans.place(plan, site.origin(), site.rotation());
        List<Project.Cell> cells = new ArrayList<>();
        for (ColonyPlans.Cell cell : placed.cells()) {
            cells.add(new Project.Cell(cell.rel().asLong(), cell.state()));
        }
        List<Project.SpanRec> spans = new ArrayList<>();
        for (ColonyPlans.SpanSpec s : placed.spans()) {
            spans.add(new Project.SpanRec(s.a().asLong(), s.b().asLong(), s.tier()));
        }
        if (site.connectNode() != null && placed.port().isPresent()) {
            BlockPos port = placed.port().get();
            CableTier link = site.linkTier();
            if (site.relay() != null) {
                BlockPos relay = site.relay();
                if (link == CableTier.ROPE) {
                    cells.add(new Project.Cell(relay.below(2).asLong(), Blocks.OAK_FENCE.defaultBlockState()));
                    cells.add(new Project.Cell(relay.below().asLong(), Blocks.OAK_FENCE.defaultBlockState()));
                    cells.add(new Project.Cell(relay.asLong(), AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState()));
                } else {
                    cells.add(new Project.Cell(relay.asLong(), AutarkiaBlocks.WOODEN_POLE.get().defaultBlockState()));
                }
                spans.add(new Project.SpanRec(port.asLong(), relay.asLong(), link));
                spans.add(new Project.SpanRec(relay.asLong(), site.connectNode().asLong(), link));
            } else {
                spans.add(new Project.SpanRec(port.asLong(), site.connectNode().asLong(), link));
            }
        }
        if (id.equals("boiler_engine") && Planner.signal(c, level, "mem.boiler_burst") >= 0.5) {
            extraValve(level, placed, cells);
        }
        if (c.tier >= 3 && Planner.signal(c, level, "mem.rack_collapse") >= 0.5) {
            cells.replaceAll(cl -> cl.state().is(AutarkiaBlocks.WOOD_RACK.get()) ? new Project.Cell(cl.pos(),
                AutarkiaBlocks.STEEL_RACK.get().withPropertiesOf(cl.state())) : cl);
        }
        cells.sort(java.util.Comparator.comparingInt((Project.Cell cl) -> BlockPos.of(cl.pos()).getY()).thenComparingLong(Project.Cell::pos));
        Project pr = create(c, level, id, tpl, plan.id(), site.origin(), site.rotation(), cells, spans, site.score());
        pr.floor = site.floor();
        pr.cuts = site.cuts();
        pr.fills = site.fills();
        pr.anchor = placed.anchor().asLong();
        pr.commission = plan.commission();
        pr.port = placed.port().map(BlockPos::asLong).orElse(Long.MIN_VALUE);
        pr.signPos = placed.sign().map(BlockPos::asLong).orElse(Long.MIN_VALUE);
        finish(c, level, id, tpl, pr);
        return pr;
    }

    private static void extraValve(ServerLevel level, ColonyPlans.Placed placed, List<Project.Cell> cells) {
        for (Project.Cell cell : List.copyOf(cells)) {
            if (cell.state().is(AutarkiaBlocks.BOILER.get())) {
                BlockPos center = BlockPos.of(cell.pos());
                for (BlockPos top : dev.autarkia.energy.BoilerBlockEntity.tops(level, center, cell.state())) {
                    if (cells.stream().noneMatch(x -> x.pos() == top.asLong())) {
                        cells.add(new Project.Cell(top.asLong(), AutarkiaBlocks.PRESSURE_VALVE.get().defaultBlockState()));
                        return;
                    }
                }
            }
        }
    }

    /** Build a handler project (05 section 3.3); null when the handler finds no valid site. */
    public static @Nullable Project fromHandler(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        String handler = tpl.handler().orElseThrow();
        return switch (handler) {
            case "parallel_span" -> parallelSpan(c, level, id, tpl);
            case "span_upgrade" -> SpanUpgrade.propose(c, level, id, tpl);
            case "arrester" -> arresters(c, level, id, tpl);
            case "valve" -> valves(c, level, id, tpl);
            case "fuse_box" -> CorrectiveProjects.fuseBox(c, level, id, tpl);
            case "rack_upgrade" -> CorrectiveProjects.rackUpgrade(c, level, id, tpl);
            case "connect_line" -> LineDuplication.propose(c, level, id, tpl);
            case "outpost" -> OutpostProjects.outpost(c, level, id, tpl);
            case "hv_link" -> OutpostProjects.hvLink(c, level, id, tpl);
            case "cable_car" -> OutpostProjects.cableCar(c, level, id, tpl);
            case "replicator" -> Replication.propose(c, level, id, tpl);
            default -> throw new IllegalStateException("Colony template " + id + " names handler '" + handler + "', which this tier does not provide");
        };
    }

    /**
     * Handler {@code arrester} (05 section 3.3 template 12): a {@code lightning_arrester} on the block above the top of
     * every connector without one in each network that a {@code lightning_surge} memory names, inside the domain.
     */
    private static @Nullable Project arresters(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        double half = dev.autarkia.config.AutarkiaConfig.MEMORY_HALF_LIFE_TICKS.get();
        List<Colony.Memory> surges = c.memories.stream().filter(m -> m.type().equals("lightning_surge") && m.weightAt(c.clock, half) >= 0.1).toList();
        List<Project.Cell> cells = new ArrayList<>();
        for (dev.autarkia.energy.GridNetwork net : Grid.of(level).networks()) {
            if (!surged(net, surges)) {
                continue;
            }
            for (int i = 0; i < net.nodes.size(); i++) {
                BlockPos node = BlockPos.of(net.nodes.getLong(i));
                BlockPos spot = dev.autarkia.energy.Lightning.top(level, node).above();
                if (level.getBlockState(node).getBlock() instanceof dev.autarkia.energy.GridConnector && Colonies.inside(c, spot)
                    && level.getBlockState(spot).canBeReplaced() && !level.getBlockState(spot).is(AutarkiaBlocks.LIGHTNING_ARRESTER.get())) {
                    cells.add(new Project.Cell(spot.asLong(), AutarkiaBlocks.LIGHTNING_ARRESTER.get().defaultBlockState()));
                }
            }
        }
        return inPlace(c, level, id, tpl, "lightning_arrester", cells);
    }

    private static boolean surged(dev.autarkia.energy.GridNetwork net, List<Colony.Memory> surges) {
        for (Colony.Memory m : surges) {
            if (m.key().equals(String.valueOf(net.id))) {
                return true;
            }
            for (int i = 0; i < net.nodes.size(); i++) {
                if (BlockPos.of(net.nodes.getLong(i)).distSqr(m.pos()) <= 16 * 16) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Handler {@code valve} (05 section 3.3 template 13): pressure valves on the top of every boiler in the domain up to 1
     * valve per boiler, or 2 after a {@code boiler_burst} memory.
     */
    private static @Nullable Project valves(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        int want = Planner.signal(c, level, "mem.boiler_burst") >= 0.1 ? 2 : 1;
        List<Project.Cell> cells = new ArrayList<>();
        int h = ColonyTiers.domainHalf(c.tier);
        for (int cx = (c.corePos.getX() - h) >> 4; cx <= (c.corePos.getX() + h) >> 4; cx++) {
            for (int cz = (c.corePos.getZ() - h) >> 4; cz <= (c.corePos.getZ() + h) >> 4; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (net.minecraft.world.level.block.entity.BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof dev.autarkia.energy.BoilerBlockEntity b) || !b.isCenter() || !Colonies.inside(c, be.getBlockPos())) {
                        continue;
                    }
                    List<BlockPos> tops = dev.autarkia.energy.BoilerBlockEntity.tops(level, be.getBlockPos(), be.getBlockState());
                    int have = (int) tops.stream().filter(t -> level.getBlockState(t).is(AutarkiaBlocks.PRESSURE_VALVE.get())).count();
                    for (BlockPos t : tops) {
                        if (have >= want) {
                            break;
                        }
                        if (level.getBlockState(t).canBeReplaced()) {
                            cells.add(new Project.Cell(t.asLong(), AutarkiaBlocks.PRESSURE_VALVE.get().defaultBlockState()));
                            have++;
                        }
                    }
                }
            }
        }
        return inPlace(c, level, id, tpl, "pressure_valve", cells);
    }

    private static @Nullable Project inPlace(Colony c, ServerLevel level, String id, ColonyTemplate tpl, String plan, List<Project.Cell> cells) {
        if (cells.isEmpty()) {
            return null;
        }
        cells.sort(java.util.Comparator.comparingInt((Project.Cell cl) -> BlockPos.of(cl.pos()).getY()).thenComparingLong(Project.Cell::pos));
        BlockPos origin = BlockPos.of(cells.get(0).pos());
        Project pr = create(c, level, id, tpl, dev.autarkia.Autarkia.id(plan), origin, Rotation.NONE, cells, List.of(), 1.0);
        pr.anchor = origin.asLong();
        pr.commission = "none";
        pr.floor = origin.getY() - 1;
        finish(c, level, id, tpl, pr);
        return pr;
    }

    /** Create a handler project from cells and spans (shared with {@link SpanUpgrade}); cells are built bottom up. */
    static Project handlerProject(Colony c, ServerLevel level, String id, ColonyTemplate tpl, String plan, BlockPos origin, List<Project.Cell> cells,
                                  List<Project.SpanRec> spans, long[] cuts) {
        cells.sort(java.util.Comparator.comparingInt((Project.Cell cl) -> BlockPos.of(cl.pos()).getY()).thenComparingLong(Project.Cell::pos));
        return orderedProject(c, level, id, tpl, plan, origin, cells, spans, cuts);
    }

    /** Create a handler project that keeps the given cell and span order (a pylon line built from the outpost toward the Core). */
    static Project orderedProject(Colony c, ServerLevel level, String id, ColonyTemplate tpl, String plan, BlockPos origin, List<Project.Cell> cells,
                                  List<Project.SpanRec> spans, long[] cuts) {
        Project pr = create(c, level, id, tpl, dev.autarkia.Autarkia.id(plan), origin, Rotation.NONE, cells, spans, 1.0);
        pr.anchor = origin.asLong();
        pr.commission = "none";
        pr.floor = origin.getY() - 1;
        pr.cuts = cuts;
        finish(c, level, id, tpl, pr);
        return pr;
    }

    private static @Nullable Project parallelSpan(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        ColonyRuntime rt = ColonyRuntime.of(c);
        for (int spanId : rt.hotSpans) {
            Span s = Grid.of(level).data().spans.get(spanId);
            if (s == null || s.tier != CableTier.ROPE) {
                continue;
            }
            Project pr = parallel(c, level, id, tpl, s);
            if (pr != null) {
                return pr;
            }
        }
        return null;
    }

    private static @Nullable Project parallel(Colony c, ServerLevel level, String id, ColonyTemplate tpl, Span s) {
        BlockPos a = s.posA();
        BlockPos b = s.posB();
        int dx = Integer.signum(b.getX() - a.getX());
        int dz = Integer.signum(b.getZ() - a.getZ());
        int[][] sides = Math.abs(b.getX() - a.getX()) >= Math.abs(b.getZ() - a.getZ()) ? new int[][] {{0, 1}, {0, -1}} : new int[][] {{1, 0}, {-1, 0}};
        for (int[] side : sides) {
            BlockPos pa = a.offset(side[0], 0, side[1]);
            BlockPos pb = b.offset(side[0], 0, side[1]);
            List<Project.Cell> cells = new ArrayList<>();
            if (!column(c, level, pa, cells) || !column(c, level, pb, cells)) {
                continue;
            }
            List<Project.SpanRec> spans = List.of(new Project.SpanRec(a.asLong(), pa.asLong(), CableTier.ROPE),
                new Project.SpanRec(pa.asLong(), pb.asLong(), CableTier.ROPE), new Project.SpanRec(pb.asLong(), b.asLong(), CableTier.ROPE));
            cells.sort(java.util.Comparator.comparingInt((Project.Cell cl) -> BlockPos.of(cl.pos()).getY()).thenComparingLong(Project.Cell::pos));
            Project pr = create(c, level, id, tpl, dev.autarkia.Autarkia.id("parallel_span"), pa, Rotation.NONE, cells, spans, 1.0);
            pr.anchor = pa.asLong();
            pr.commission = "none";
            pr.floor = pa.getY() - 1;
            finish(c, level, id, tpl, pr);
            return pr;
        }
        return null;
    }

    private static boolean column(Colony c, ServerLevel level, BlockPos pulley, List<Project.Cell> cells) {
        if (!Colonies.inside(c, pulley) || !level.getBlockState(pulley).canBeReplaced()) {
            return false;
        }
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pulley.getX(), pulley.getZ());
        if (pulley.getY() < ground || pulley.getY() - ground > 6) {
            return false;
        }
        for (int y = ground; y < pulley.getY(); y++) {
            BlockPos p = new BlockPos(pulley.getX(), y, pulley.getZ());
            if (!level.getBlockState(p).canBeReplaced()) {
                return false;
            }
            cells.add(new Project.Cell(p.asLong(), Blocks.OAK_FENCE.defaultBlockState()));
        }
        cells.add(new Project.Cell(pulley.asLong(), AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState()));
        return true;
    }

    private static Project create(Colony c, ServerLevel level, String id, ColonyTemplate tpl, Identifier plan, BlockPos origin, Rotation rot,
                                  List<Project.Cell> cells, List<Project.SpanRec> spans, double score) {
        ColonyTemplate.ReasonSpec rs = tpl.reason();
        double arg = Planner.signal(c, level, rs.signal()) * rs.scale();
        Project pr = new Project(UUID.randomUUID(), dev.autarkia.Autarkia.id(id), plan, origin, rot, Project.State.PROPOSED,
            new Project.Reason(rs.key(), List.of(arg)), c.clock, 0, false, 0, 0, score, Map.of(), 1, 0);
        pr.cells.addAll(cells);
        pr.spans.addAll(spans);
        return pr;
    }

    private static void finish(Colony c, ServerLevel level, String id, ColonyTemplate tpl, Project pr) {
        Map<Item, Integer> bom = new LinkedHashMap<>();
        double w = 0;
        int connectors = 0;
        for (Project.Cell cell : pr.cells) {
            BlockState st = cell.state();
            bom.merge(Builds.itemFor(st), 1, Integer::sum);
            if (st.getBlock() instanceof BaseMultiblock) {
                w += 8;
            } else if (st.hasBlockEntity()) {
                w += 4;
            } else {
                w += 1;
            }
            if (st.is(AutarkiaBlocks.ROPE_PULLEY.get())) {
                connectors++;
            }
        }
        double spanLength = 0;
        for (Project.SpanRec s : pr.spans) {
            double len = Math.sqrt(BlockPos.of(s.a()).distSqr(BlockPos.of(s.b())));
            spanLength += len;
            bom.merge(Grid.cableItem(s.tier()), s.tier().itemCost(len), Integer::sum);
        }
        pr.bom.clear();
        pr.bom.putAll(bom);
        pr.cost = w + 0.25 * pr.levelingEdits() + 0.5 * spanLength + 2 * connectors;
        pr.fromMemory = Planner.params(c, id).memoryWeight() > 0;
        pr.urgency = Planner.urgency(c, level, id, tpl);
        pr.priority = Planner.priority(c, id, tpl, pr);
    }

    /** Return the number of fill blocks the project needs from storage after the usable cut material. */
    public static int fillFromStorage(ServerLevel level, Project p) {
        return Math.max(0, p.fills.length - usableCuts(level, p));
    }

    private static int usableCuts(ServerLevel level, Project p) {
        int n = 0;
        for (long cut : p.cuts) {
            BlockState st = level.getBlockState(BlockPos.of(cut));
            if (ItemLedger.isFill(st.getBlock().asItem().getDefaultInstance())) {
                n++;
            }
        }
        return n;
    }

    /** Return whether every planned cell is still buildable: air, replaceable, a planned cut, or already the target. */
    public static boolean siteStillFree(Colony c, ServerLevel level, Project p) {
        LongOpenHashSet cuts = new LongOpenHashSet(p.cuts);
        for (Project.Cell cell : p.cells) {
            BlockPos pos = BlockPos.of(cell.pos());
            BlockState st = level.getBlockState(pos);
            if (st.equals(cell.state()) || st.canBeReplaced() || cuts.contains(cell.pos())) {
                continue;
            }
            return false;
        }
        return true;
    }
}

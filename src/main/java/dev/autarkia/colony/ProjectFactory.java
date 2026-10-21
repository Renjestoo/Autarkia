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
 * 3.5), reason, urgency and priority.
 */
public final class ProjectFactory {
    private ProjectFactory() {
    }

    /** Build the proposal for a template at its best site. */
    public static Project fromSite(Colony c, ServerLevel level, String id, ColonyTemplate tpl, SiteSearch.Result site) {
        ColonyPlans.Plan plan = ColonyPlans.plan(tpl.plan().orElseThrow());
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
            if (site.relay() != null) {
                BlockPos relay = site.relay();
                cells.add(new Project.Cell(relay.below(2).asLong(), Blocks.OAK_FENCE.defaultBlockState()));
                cells.add(new Project.Cell(relay.below().asLong(), Blocks.OAK_FENCE.defaultBlockState()));
                cells.add(new Project.Cell(relay.asLong(), AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState()));
                spans.add(new Project.SpanRec(port.asLong(), relay.asLong(), CableTier.ROPE));
                spans.add(new Project.SpanRec(relay.asLong(), site.connectNode().asLong(), CableTier.ROPE));
            } else {
                spans.add(new Project.SpanRec(port.asLong(), site.connectNode().asLong(), CableTier.ROPE));
            }
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

    /** Build a handler project (05 section 3.3 handler {@code parallel_span}); null when there is no valid site. */
    public static @Nullable Project fromHandler(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        String handler = tpl.handler().orElseThrow();
        if (!handler.equals("parallel_span")) {
            throw new IllegalStateException("Colony template " + id + " names handler '" + handler + "', which this tier does not provide");
        }
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
            bom.merge(st.getBlock().asItem(), 1, Integer::sum);
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

package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.jspecify.annotations.Nullable;

/**
 * Repair from Patterns (05 section 5.5, W10): watched instances are scanned every {@code pattern.repairScanPeriodTicks}
 * and 20 ticks after an explosion, a mob that destroys a block or a non-trusted player break inside a watched box, at
 * most {@code pattern.repairScanCellsPerTick} cells per tick per colony. A missing cell (air, replaceable, fluid or fire
 * where the Pattern has a block) is planned again, a different solid block is a conflict the colony leaves alone, and a
 * block the owner or a trusted player breaks is an accepted removal (its mask bit is set). A finished scan with missing
 * cells or spans proposes one {@code pattern_repair} project per instance: cells bottom row first, then by distance to
 * the box center; cost x 0.5. The project restores the configuration of Autarkia block entities when it is done. Cells
 * inside the box of a Restoration Field are left to that field (M9 decision G11).
 */
public final class PatternRepair {
    /** The running scan of one watched instance. */
    public static final class Scan {
        final UUID instance;
        final Pattern pattern;
        final Function<BlockPos, BlockPos> map;
        int cursor;
        int conflicts;
        final List<Project.Cell> missing = new ArrayList<>();

        Scan(UUID instance, Pattern pattern, Function<BlockPos, BlockPos> map) {
            this.instance = instance;
            this.pattern = pattern;
            this.map = map;
        }
    }

    private static final String PLAN_PREFIX = "instance/";

    private PatternRepair() {
    }

    /** Register the event listeners that queue a rescan or record an accepted removal. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ExplosionEvent.Detonate e) -> {
            if (e.getLevel() instanceof ServerLevel sl && Colonies.server() != null) {
                e.getAffectedBlocks().forEach(p -> damaged(sl, p));
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingDestroyBlockEvent e) -> {
            if (e.getEntity().level() instanceof ServerLevel sl && Colonies.server() != null) {
                damaged(sl, e.getPos());
            }
        });
        NeoForge.EVENT_BUS.addListener((BreakBlockEvent e) -> {
            if (e.getLevel() instanceof ServerLevel sl && Colonies.server() != null && !e.isCanceled()) {
                broken(sl, e.getPos(), e.getPlayer().getUUID());
            }
        });
    }

    private static void damaged(ServerLevel sl, BlockPos pos) {
        Colony c = Colonies.at(sl, pos);
        if (c != null && c.instances.stream().anyMatch(in -> in.watched() && in.contains(pos))) {
            ColonyRuntime rt = ColonyRuntime.of(c);
            long at = sl.getGameTime() + 20;
            if (rt.repairScanAt < 0 || rt.repairScanAt > at) {
                rt.repairScanAt = at;
            }
        }
    }

    private static void broken(ServerLevel sl, BlockPos pos, UUID player) {
        Colony c = Colonies.at(sl, pos);
        if (c == null) {
            return;
        }
        if (!c.canManage(player)) {
            damaged(sl, pos);
            return;
        }
        for (int k = 0; k < c.instances.size(); k++) {
            Colony.Instance in = c.instances.get(k);
            if (!in.watched() || !in.contains(pos)) {
                continue;
            }
            Pattern p = Pattern.load(sl.getServer(), in.pattern());
            int idx = p == null ? -1 : indexOf(p, in, pos);
            if (idx >= 0) {
                long[] mask = java.util.Arrays.copyOf(in.mask(), Math.max(in.mask().length, idx / 64 + 1));
                mask[idx / 64] |= 1L << (idx % 64);
                c.instances.set(k, new Colony.Instance(in.id(), in.pattern(), in.origin(), in.rotation(), in.size(), true, mask));
                Colonies.changed();
            }
        }
    }

    private static int indexOf(Pattern p, Colony.Instance in, BlockPos world) {
        Function<BlockPos, BlockPos> map = ColonyPlans.mapper(p.size(), in.origin(), in.rotation());
        for (int y = 0; y < p.size().getY(); y++) {
            for (int z = 0; z < p.size().getZ(); z++) {
                for (int x = 0; x < p.size().getX(); x++) {
                    if (map.apply(new BlockPos(x, y, z)).equals(world)) {
                        return p.index(x, y, z);
                    }
                }
            }
        }
        return -1;
    }

    private static boolean masked(Colony.Instance in, int i) {
        return i / 64 < in.mask().length && (in.mask()[i / 64] >>> (i % 64) & 1) != 0;
    }

    /** Return the plan id a repair project of an instance uses. */
    public static net.minecraft.resources.Identifier planId(UUID instance) {
        return Autarkia.id(PLAN_PREFIX + instance);
    }

    /** Advance the repair scan of the colony; called every colony tick. */
    public static void tick(Colony c, ServerLevel level) {
        if (c.tier < 3) {
            return;
        }
        ColonyRuntime rt = ColonyRuntime.of(c);
        long now = level.getGameTime();
        if (rt.repairScan == null) {
            int period = AutarkiaConfig.PATTERN_REPAIR_SCAN_PERIOD_TICKS.get();
            boolean due = rt.repairScanAt >= 0 && now >= rt.repairScanAt || Math.floorMod(now, period) == Math.floorMod(c.id.hashCode(), period);
            if (!due) {
                return;
            }
            rt.repairScanAt = -1;
            rt.pendingInstances.clear();
            c.instances.stream().filter(Colony.Instance::watched).forEach(in -> rt.pendingInstances.add(in.id()));
            next(c, level, rt);
        }
        int budget = AutarkiaConfig.PATTERN_REPAIR_SCAN_CELLS_PER_TICK.get();
        while (rt.repairScan != null && budget > 0) {
            budget -= step(c, level, rt.repairScan, budget);
            if (rt.repairScan.cursor >= rt.repairScan.pattern.volume()) {
                finish(c, level, rt, rt.repairScan);
                next(c, level, rt);
            }
        }
    }

    private static void next(Colony c, ServerLevel level, ColonyRuntime rt) {
        rt.repairScan = null;
        while (!rt.pendingInstances.isEmpty() && rt.repairScan == null) {
            UUID id = rt.pendingInstances.remove(0);
            Colony.Instance in = instance(c, id);
            Pattern p = in == null ? null : Pattern.load(level.getServer(), in.pattern());
            if (p != null) {
                rt.repairScan = new Scan(id, p, ColonyPlans.mapper(p.size(), in.origin(), in.rotation()));
            }
        }
    }

    private static Colony.@Nullable Instance instance(Colony c, UUID id) {
        for (Colony.Instance in : c.instances) {
            if (in.id().equals(id)) {
                return in;
            }
        }
        return null;
    }

    private static int step(Colony c, ServerLevel level, Scan s, int budget) {
        Colony.Instance in = instance(c, s.instance);
        if (in == null || !in.watched()) {
            s.cursor = s.pattern.volume();
            return 1;
        }
        int sx = s.pattern.size().getX();
        int sz = s.pattern.size().getZ();
        int done = 0;
        while (done < budget && s.cursor < s.pattern.volume()) {
            int i = s.cursor++;
            done++;
            BlockState want = s.pattern.state(i);
            if (want == null || masked(in, i)) {
                continue;
            }
            BlockPos pos = s.map.apply(new BlockPos(i % sx, i / (sx * sz), i / sx % sz));
            if (!level.isLoaded(pos) || Restoration.covered(c, pos)) {
                continue;
            }
            BlockState rotated = want.rotate(in.rotation());
            BlockState have = level.getBlockState(pos);
            if (have.is(rotated.getBlock())) {
                continue;
            }
            if (have.isAir() || have.canBeReplaced() || !have.getFluidState().isEmpty() || have.is(BlockTags.FIRE)) {
                s.missing.add(new Project.Cell(pos.asLong(), rotated));
            } else {
                s.conflicts++;
            }
        }
        return done;
    }

    private static void finish(Colony c, ServerLevel level, ColonyRuntime rt, Scan s) {
        Colony.Instance in = instance(c, s.instance);
        if (in == null) {
            return;
        }
        List<Project.SpanRec> spans = new ArrayList<>();
        for (Pattern.SpanRecord sp : s.pattern.spans()) {
            BlockPos a = s.map.apply(sp.a());
            BlockPos b = s.map.apply(sp.b());
            if (!Builds.strung(level, a.asLong(), b.asLong())) {
                spans.add(new Project.SpanRec(a.asLong(), b.asLong(), sp.tier()));
            }
        }
        rt.patternMissing.put(in.id(), s.missing.size() + spans.size());
        rt.patternConflicts.put(in.id(), s.conflicts);
        if (s.missing.isEmpty() && spans.isEmpty()) {
            return;
        }
        ColonyTemplate tpl = ColonyPlans.templates().get("pattern_repair");
        net.minecraft.resources.Identifier plan = planId(in.id());
        if (tpl == null || !c.template("pattern_repair").enabled || c.projects.stream().anyMatch(p -> p.state.open() && p.plan.equals(plan))) {
            return;
        }
        Vec3 center = Vec3.atLowerCornerOf(in.origin()).add(in.size().getX() / 2.0, in.size().getY() / 2.0, in.size().getZ() / 2.0);
        List<Project.Cell> cells = new ArrayList<>(s.missing);
        cells.sort(Comparator.comparingInt((Project.Cell cl) -> BlockPos.of(cl.pos()).getY())
            .thenComparingDouble(cl -> Vec3.atCenterOf(BlockPos.of(cl.pos())).distanceToSqr(center)).thenComparingLong(Project.Cell::pos));
        Project pr = ProjectFactory.handlerProject(c, level, "pattern_repair", tpl, plan.getPath(), in.origin(), cells, spans, new long[0]);
        pr.cells.clear();
        pr.cells.addAll(cells);
        pr.cost *= 0.5;
        pr.reason = new Project.Reason("pattern_missing", List.of((double) (s.missing.size() + spans.size())));
        pr.priority = Planner.priority(c, "pattern_repair", tpl, pr);
        Planner.propose(c, pr);
        if (c.autonomy != Colony.Autonomy.MANUAL) {
            Planner.approve(c, pr, true);
        }
        Autarkia.LOG.info("[colony] {} repair of instance {}: {} cells, {} spans, {} conflicts", c.name, in.id(), s.missing.size(), spans.size(),
            s.conflicts);
    }

    /** Return the number of conflict cells of the last scan of an instance. */
    public static int conflicts(Colony c, UUID instance) {
        return ColonyRuntime.of(c).patternConflicts.getOrDefault(instance, 0);
    }

    /** Restore the Autarkia block entity configuration of a finished repair project from its Pattern. */
    public static void restoreData(Colony c, ServerLevel level, Project p) {
        if (p.plan.getPath().startsWith(PLAN_PREFIX)) {
            restore(c, level, p, UUID.fromString(p.plan.getPath().substring(PLAN_PREFIX.length())));
        }
    }

    /** Apply the Pattern configuration data of an instance to the block entities a project placed. */
    public static void restore(Colony c, ServerLevel level, Project p, UUID instanceId) {
        Colony.Instance in = instance(c, instanceId);
        Pattern pat = in == null ? null : Pattern.load(level.getServer(), in.pattern());
        if (pat == null) {
            return;
        }
        Function<BlockPos, BlockPos> map = ColonyPlans.mapper(pat.size(), in.origin(), in.rotation());
        int sx = pat.size().getX();
        int sz = pat.size().getZ();
        for (var e : pat.data().entrySet()) {
            int i = Integer.parseInt(e.getKey());
            BlockPos pos = map.apply(new BlockPos(i % sx, i / (sx * sz), i / sx % sz));
            if (p.cells.stream().anyMatch(cl -> cl.pos() == pos.asLong()) && level.getBlockEntity(pos) instanceof PatternDataHolder h) {
                CompoundTag tag = e.getValue();
                h.readPatternData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            }
        }
    }
}

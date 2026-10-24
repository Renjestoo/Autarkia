package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.GridMember;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The colony job board (05 section 7): opens the jobs of each building project phase (level with span cuts, place, string,
 * commission, sign), the standing CRANK, COLLECT_DEBRIS, DELIVER and SURVEY jobs, assigns work orders every 20 ticks (priority, phase, y, age, position;
 * nearest capable idle worker within 128 blocks Manhattan, any distance for the long trips of outposts, pylon lines,
 * surveys and escorts; a worker that already carries the job's item goes first), and handles leases, timeouts,
 * failures with backoff and the retry limit that blocks a project; an item that a colony worker still carries makes an
 * ITEM_MISSING failure wait without counting.
 * An outpost project first opens FOUND_OUTPOST for its core, before levelling, so the core charges the builders on site
 * (plus an ESCORT for a Sentinel when the carrier is assigned); a pylon line places and strings one cell at a time in
 * route order.
 */
public final class JobBoard {
    private static final int SURVEY_RADIUS = 64;
    private static final int SURVEY_STEP = 128;
    public final ItemLedger ledger = new ItemLedger();
    private final Map<UUID, ColonyJob> jobs = new LinkedHashMap<>();
    private final Long2ObjectOpenHashMap<UUID> positions = new Long2ObjectOpenHashMap<>();
    private final Map<UUID, Long> assignedAt = new LinkedHashMap<>();
    private final Map<UUID, Long> commissionBase = new LinkedHashMap<>();

    /** Return every job (read-only use). */
    public List<ColonyJob> all() {
        return List.copyOf(jobs.values());
    }

    /** Return a job by id, or null. */
    public @Nullable ColonyJob get(UUID id) {
        return jobs.get(id);
    }

    /** Return the open (not done) job count for S20. */
    public int openCount() {
        return (int) jobs.values().stream().filter(j -> j.status != ColonyJob.Status.DONE).count();
    }

    /** Return the age in ticks of the oldest open job. */
    public long oldestAge(long now) {
        return jobs.values().stream().filter(j -> j.status != ColonyJob.Status.DONE).mapToLong(j -> now - j.created).max().orElse(0);
    }

    /** Release all reservations and rebuild jobs from project phase and cursor (server start). */
    public void rebuild(Colony c) {
        jobs.clear();
        positions.clear();
        assignedAt.clear();
        ledger.clear();
        ServerLevel level = Colonies.level(c);
        if (level == null) {
            return;
        }
        ColonyRuntime rt = ColonyRuntime.of(c);
        ColonySensors.sample(c, level);
        for (Project p : c.projects) {
            if (p.state == Project.State.BUILDING || p.state == Project.State.BLOCKED || p.state == Project.State.COMMISSIONING) {
                if (!ledger.reserve(c, level, rt, p)) {
                    Autarkia.LOG.info("[colony] {}: project {} reloaded with items missing; jobs fail ITEM_MISSING until stock returns", c.name, p.shortId());
                }
                if (p.state == Project.State.BLOCKED) {
                    p.state = Project.State.BUILDING;
                }
                openPhase(c, level, p);
            }
        }
    }

    /** Remove every job of a project and release its reservations; workers holding its jobs abort. */
    public void dropProject(Colony c, Project p) {
        ServerLevel level = Colonies.level(c);
        for (ColonyJob j : new ArrayList<>(jobs.values())) {
            if (p.id.equals(j.projectId)) {
                if (j.assignee != null && level != null && level.getEntity(j.assignee) instanceof ColonyWorker w) {
                    w.abort(ColonyJob.Failure.WORKER_LOST);
                }
                remove(j);
            }
        }
        ledger.release(p.id);
    }

    private void remove(ColonyJob j) {
        jobs.remove(j.id);
        positions.remove(j.target.asLong(), j.id);
        assignedAt.remove(j.id);
    }

    /** Add an open job unless another job already targets its position (DELIVER jobs do not hold their position). */
    public void add(ColonyJob j) {
        if (j.type != ColonyJob.Type.DELIVER && positions.containsKey(j.target.asLong())) {
            return;
        }
        jobs.put(j.id, j);
        if (j.type != ColonyJob.Type.DELIVER) {
            positions.put(j.target.asLong(), j.id);
        }
    }

    /** Open the jobs of the project's current phase (also used after a load). */
    public void openPhase(Colony c, ServerLevel level, Project p) {
        long now = level.getGameTime();
        switch (p.phase) {
            case 2 -> {
                if (p.cursor == 0) {
                    if (found(level, p, now)) {
                        return;
                    }
                    List<BlockPos> cuts = new ArrayList<>(p.cutCells());
                    cuts.sort(Comparator.comparingInt((BlockPos b) -> -b.getY()).thenComparingLong(BlockPos::asLong));
                    for (BlockPos b : cuts) {
                        if (!level.getBlockState(b).isAir() && !level.getBlockState(b).canBeReplaced()) {
                            add(new ColonyJob(ColonyJob.Type.LEVEL_CUT, p.id, b, null, null, 0, p.priority, 2, now, null, null, 0));
                        }
                    }
                    for (Project.SpanRec s : p.cutSpans) {
                        if (Builds.strung(level, s.a(), s.b())) {
                            add(new ColonyJob(ColonyJob.Type.CUT_SPAN, p.id, BlockPos.of(s.a()), null, null, 0, p.priority, 2, now, BlockPos.of(s.b()),
                                s.tier(), 0));
                        }
                    }
                } else {
                    List<BlockPos> fills = new ArrayList<>();
                    for (long f : p.fills) {
                        fills.add(BlockPos.of(f));
                    }
                    fills.sort(Comparator.comparingInt((BlockPos b) -> b.getY()).thenComparingLong(BlockPos::asLong));
                    for (BlockPos b : fills) {
                        if (level.getBlockState(b).canBeReplaced()) {
                            add(new ColonyJob(ColonyJob.Type.LEVEL_FILL, p.id, b, null, null, 1, p.priority, 2, now, null, null, 0));
                        }
                    }
                }
            }
            case 3 -> {
                if (found(level, p, now)) {
                    return;
                }
                boolean sequential = OutpostProjects.sequential(p);
                for (Project.Cell cell : p.cells) {
                    BlockPos pos = BlockPos.of(cell.pos());
                    if (Builds.placed(level, cell)) {
                        continue;
                    }
                    if (!positions.containsKey(cell.pos()) && Builds.supported(level, pos)) {
                        add(new ColonyJob(ColonyJob.Type.PLACE, p.id, pos, cell.state(), Builds.itemFor(cell.state()), 1, p.priority, 3, now, null, null, 0));
                    }
                    if (sequential) {
                        break;
                    }
                }
            }
            case 4 -> {
                boolean sequential = OutpostProjects.sequential(p);
                for (Project.SpanRec s : p.spans) {
                    BlockPos a = BlockPos.of(s.a());
                    if (Builds.strung(level, s.a(), s.b())) {
                        continue;
                    }
                    if (!positions.containsKey(s.a())) {
                        int cost = s.tier().itemCost(Math.sqrt(a.distSqr(BlockPos.of(s.b()))));
                        add(new ColonyJob(ColonyJob.Type.STRING_CABLE, p.id, a, null, s.tier() == dev.autarkia.energy.CableTier.PHASE ? null
                            : dev.autarkia.energy.Grid.cableItem(s.tier()), cost, p.priority, 4, now,
                            BlockPos.of(s.b()), s.tier(), 0));
                    }
                    if (sequential) {
                        break;
                    }
                }
            }
            case 5 -> {
                if (p.commission.equals("crank")) {
                    add(new ColonyJob(ColonyJob.Type.CRANK, p.id, BlockPos.of(p.anchor), null, null, 0, p.priority, 5, now, null, null, 200));
                } else if (p.commission.equals("runs")) {
                    p.commissionStart = now;
                    commissionBase.put(p.id, stored(level, BlockPos.of(p.anchor)));
                }
            }
            case 6 -> {
                BlockPos spot = Signs.spot(c, level, p);
                if (spot != null) {
                    add(new ColonyJob(ColonyJob.Type.SIGN, p.id, spot, Signs.state(c, spot), null, 1, p.priority, 6, now, null, null, 0));
                }
            }
            default -> {
            }
        }
    }

    private boolean found(ServerLevel level, Project p, long now) {
        Project.Cell seed = p.cells.stream().filter(cl -> cl.state().is(dev.autarkia.registry.AutarkiaBlocks.OUTPOST_CORE.get())).findFirst().orElse(null);
        if (seed == null || Builds.placed(level, seed)) {
            return false;
        }
        if (!positions.containsKey(seed.pos())) {
            add(new ColonyJob(ColonyJob.Type.FOUND_OUTPOST, p.id, BlockPos.of(seed.pos()), seed.state(), Builds.itemFor(seed.state()), 1, p.priority,
                p.phase, now, null, null, 0));
        }
        return true;
    }

    private static long stored(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof GridMember m ? m.gridBuffer().stored() : 0;
    }

    private boolean phaseHasJobs(Project p) {
        for (ColonyJob j : jobs.values()) {
            if (p.id.equals(j.projectId) && j.status != ColonyJob.Status.DONE) {
                return true;
            }
        }
        return false;
    }

    /** Advance project phases, open standing jobs, expire leases and assign work every 20 ticks. */
    public void tick(Colony c, ServerLevel level) {
        long now = level.getGameTime();
        for (ColonyJob j : new ArrayList<>(jobs.values())) {
            if (j.status == ColonyJob.Status.ASSIGNED && (now > j.leaseUntil || now - assignedAt.getOrDefault(j.id, now) > j.timeout())) {
                Autarkia.LOG.info("[colony] {}: job {} at {} timed out ({})", c.name, j.type, j.target.toShortString(), now > j.leaseUntil
                    ? "lease not renewed for 1200 ticks" : "over its " + j.timeout() + "-tick limit since the worker arrived");
                if (j.assignee != null && level.getEntity(j.assignee) instanceof ColonyWorker w) {
                    w.abort(ColonyJob.Failure.TIMEOUT);
                }
                failed(c, level, j.id, ColonyJob.Failure.TIMEOUT);
            }
        }
        if (now % 20 != 0) {
            return;
        }
        jobs.values().removeIf(j -> {
            boolean gone = j.status == ColonyJob.Status.DONE;
            if (gone) {
                positions.remove(j.target.asLong(), j.id);
            }
            return gone;
        });
        for (Project p : new ArrayList<>(c.projects)) {
            if (p.state == Project.State.BUILDING || p.state == Project.State.COMMISSIONING) {
                advance(c, level, p);
            }
        }
        standingCrank(c, level);
        standingDebris(c, level);
        standingDeliveries(c, level);
        standingSurvey(c, level);
        assign(c, level);
    }

    /**
     * Open one SURVEY job (radius {@value #SURVEY_RADIUS}) every 1,200 ticks while {@link FrontierSignals#needSurvey}
     * holds, no SURVEY job is open and the colony has a worker that surveys. The areas are a square spiral of centers
     * {@value #SURVEY_STEP} blocks apart around the Core, without the Core area, each at most {@code outpost.maxDistance}
     * from the Core; each job takes the next area (colony stat {@code survey_areas}), after the last the first again.
     */
    private void standingSurvey(Colony c, ServerLevel level) {
        if (level.getGameTime() % 1200 != 0 || jobs.values().stream().anyMatch(j -> j.type == ColonyJob.Type.SURVEY)
            || c.workers.values().stream().noneMatch(w -> dev.autarkia.worker.WorkerType.byPath(w.type().getPath()).jobs().contains(ColonyJob.Type.SURVEY))
            || !FrontierSignals.needSurvey(c, level, ColonyRuntime.of(c))) {
            return;
        }
        int max = AutarkiaConfig.OUTPOST_MAX_DISTANCE.get();
        int scale = SURVEY_STEP / dev.autarkia.worker.WorkerBehavioursT4.SCAN_SPACING;
        List<BlockPos> areas = new ArrayList<>();
        for (BlockPos p : dev.autarkia.worker.WorkerBehavioursT4.spiral(BlockPos.ZERO, max / scale)) {
            BlockPos center = c.corePos.offset(p.getX() * scale, 0, p.getZ() * scale);
            if (!p.equals(BlockPos.ZERO) && Outposts.horizontal(c.corePos, center) <= max) {
                areas.add(center);
            }
        }
        if (areas.isEmpty()) {
            return;
        }
        BlockPos center = areas.get((int) Math.floorMod(c.stats.getOrDefault("survey_areas", 0L), (long) areas.size()));
        c.stat("survey_areas", 1);
        add(new ColonyJob(ColonyJob.Type.SURVEY, null, center, null, null, 0, 20, 0, level.getGameTime(), null, null, SURVEY_RADIUS));
        ColonyRuntime.of(c).log("Survey opened at " + center.toShortString());
        Colonies.changed();
    }

    /**
     * Open the DELIVER job of the first standing delivery order of each target that has no open job (so a frame goes into
     * an assembler before its parts); an order whose loaded target block takes no items is dropped.
     */
    private void standingDeliveries(Colony c, ServerLevel level) {
        java.util.Set<BlockPos> busy = new java.util.HashSet<>();
        jobs.values().stream().filter(j -> j.type == ColonyJob.Type.DELIVER && j.status != ColonyJob.Status.DONE).forEach(j -> busy.add(j.target));
        for (Colony.Delivery d : List.copyOf(c.deliveries)) {
            if (!busy.add(d.target()) || !level.isLoaded(d.target())) {
                continue;
            }
            BlockPos at = level.getBlockState(d.target()).getBlock() instanceof net.nikdo53.tinymultiblocklib.block.BaseMultiblock
                ? net.nikdo53.tinymultiblocklib.block.IMultiBlock.getCenter(level, d.target()) : d.target();
            if (!(level.getBlockEntity(at) instanceof Colony.DeliveryTarget)
                && level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, at, null) == null) {
                c.deliveries.remove(d);
                ColonyRuntime.of(c).log("Delivery to " + d.target().toShortString() + " dropped: the block is gone");
                Colonies.changed();
                continue;
            }
            add(new ColonyJob(ColonyJob.Type.DELIVER, null, d.target(), null, d.item(), Math.min(16, d.count()), 40, 0, level.getGameTime(), null, null, 0));
        }
    }

    /**
     * Open one COLLECT_DEBRIS job (05 section 7.2) every 100 ticks while debris lies in the domain, no such job is open
     * and the colony has a worker that collects debris; the job points at the debris nearest the Core.
     */
    private void standingDebris(Colony c, ServerLevel level) {
        if (level.getGameTime() % 100 != 0 || jobs.values().stream().anyMatch(j -> j.type == ColonyJob.Type.COLLECT_DEBRIS)) {
            return;
        }
        boolean collector = c.workers.values().stream().anyMatch(w -> dev.autarkia.worker.WorkerType.byPath(w.type().getPath()).jobs()
            .contains(ColonyJob.Type.COLLECT_DEBRIS));
        if (!collector) {
            return;
        }
        List<Entity> debris = dev.autarkia.worker.WorkerSensors.debris(level, c);
        debris.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(c.corePos)))).ifPresent(e ->
            add(new ColonyJob(ColonyJob.Type.COLLECT_DEBRIS, null, e.blockPosition(), null, null, 0, 15, 0, level.getGameTime(), null, null, 0)));
    }

    /**
     * Mark open (unassigned) jobs of the project done when the world already shows their result: a player built that
     * part (05 section 14: T2 projects before the T3 workers) or a worker finished it outside the job.
     */
    private void settle(Colony c, ServerLevel level, Project p) {
        for (ColonyJob j : new ArrayList<>(jobs.values())) {
            if (!p.id.equals(j.projectId) || j.status != ColonyJob.Status.OPEN || !level.isLoaded(j.target)) {
                continue;
            }
            BlockState st = level.getBlockState(j.target);
            boolean ok = switch (j.type) {
                case LEVEL_CUT -> st.isAir() || st.canBeReplaced() && st.getFluidState().isEmpty();
                case LEVEL_FILL -> !Builds.replaceable(st);
                case PLACE -> j.state != null && Builds.placed(level, new Project.Cell(j.target.asLong(), j.state));
                case STRING_CABLE -> j.nodeB != null && Builds.strung(level, j.target.asLong(), j.nodeB.asLong());
                case CUT_SPAN -> j.nodeB != null && !Builds.strung(level, j.target.asLong(), j.nodeB.asLong());
                default -> false;
            };
            if (ok) {
                done(c, level, j.id);
            }
        }
    }

    private void advance(Colony c, ServerLevel level, Project p) {
        settle(c, level, p);
        if (p.phase == 3 || p.phase == 4 && OutpostProjects.sequential(p)) {
            openPhase(c, level, p);
        }
        if (p.phase == 5 && p.commission.equals("runs")) {
            BlockPos a = BlockPos.of(p.anchor);
            boolean running = level.getBlockEntity(a) instanceof GridMember m && (m.gridMaxOut() > 0 || stored(level, a) > commissionBase.getOrDefault(p.id, 0L));
            if (running) {
                nextPhase(c, level, p);
            } else if (level.getGameTime() - p.commissionStart > 200) {
                block(c, p, "not_running");
            }
            return;
        }
        if (phaseHasJobs(p)) {
            return;
        }
        if (p.phase == 2 && p.cursor == 0) {
            openPhase(c, level, p);
            if (phaseHasJobs(p)) {
                return;
            }
            p.cursor = 1;
            openPhase(c, level, p);
            if (phaseHasJobs(p)) {
                return;
            }
        }
        if (p.phase == 3 && !allPlaced(level, p)) {
            return;
        }
        nextPhase(c, level, p);
    }

    private static boolean allPlaced(ServerLevel level, Project p) {
        for (Project.Cell cell : p.cells) {
            if (!Builds.placed(level, cell)) {
                return false;
            }
        }
        return true;
    }

    private void nextPhase(Colony c, ServerLevel level, Project p) {
        p.phase++;
        p.cursor = 0;
        if (p.phase == 5) {
            p.state = Project.State.COMMISSIONING;
        }
        if (p.phase == 6 && !Signs.applies(c, p)) {
            p.phase = 7;
        }
        if (p.phase == 6 && !workerForTier(c, p)) {
            p.phase = 7;
            ColonyRuntime.of(c).log("Sign skipped: no worker builds T" + projectTier(p) + " projects yet");
        }
        if (p.phase >= 7) {
            Projects.done(c, level, p);
            ledger.release(p.id);
            return;
        }
        openPhase(c, level, p);
        Colonies.changed();
    }

    private static int projectTier(Project p) {
        ColonyTemplate tpl = ColonyPlans.templates().get(p.template.getPath());
        return tpl == null ? 1 : tpl.tier();
    }

    private static boolean workerForTier(Colony c, Project p) {
        int tier = projectTier(p);
        for (Colony.WorkerEntry w : c.workers.values()) {
            dev.autarkia.worker.WorkerType type = dev.autarkia.worker.WorkerType.byPath(w.type().getPath());
            if (!type.jobs().isEmpty() && type.tier() >= tier) {
                return true;
            }
        }
        return false;
    }

    private void standingCrank(Colony c, ServerLevel level) {
        ColonyRuntime rt = ColonyRuntime.of(c);
        double s03 = rt.signal("S03", level.getGameTime(), 0);
        double idle = rt.signal("S17.porter", level.getGameTime(), 0);
        if (s03 < 0.1 || idle < 0.5) {
            return;
        }
        for (BlockPos g : rt.generators) {
            if (!positions.containsKey(g.asLong())) {
                add(new ColonyJob(ColonyJob.Type.CRANK, null, g, null, null, 0, 10, 0, level.getGameTime(), null, null, 2400));
            }
        }
    }

    private void assign(Colony c, ServerLevel level) {
        long now = level.getGameTime();
        List<ColonyJob> open = new ArrayList<>(jobs.values().stream().filter(j -> j.status == ColonyJob.Status.OPEN && j.backoffUntil <= now).toList());
        open.sort(Comparator.comparingDouble((ColonyJob j) -> -j.priority).thenComparingInt(j -> j.phase).thenComparingInt(j -> j.target.getY())
            .thenComparingLong(j -> j.created).thenComparingLong(j -> j.target.asLong()));
        List<ColonyWorker> idle = new ArrayList<>();
        for (Colony.WorkerEntry w : c.workers.values()) {
            Entity e = level.getEntity(w.id());
            if (e instanceof ColonyWorker cw && cw.isIdle() && cw.charge() >= 0.2f && level.isPositionEntityTicking(e.blockPosition())) {
                idle.add(cw);
            }
        }
        int budget = AutarkiaConfig.COLONY_JOB_ASSIGN_PER_PASS.get();
        for (ColonyJob j : open) {
            if (budget-- <= 0 || idle.isEmpty()) {
                return;
            }
            if (j.status != ColonyJob.Status.OPEN) {
                continue;
            }
            ColonyWorker best = null;
            int bestD = Integer.MAX_VALUE;
            boolean far = OutpostProjects.longTrip(c, j);
            for (ColonyWorker w : idle) {
                int d = j.item != null && w instanceof dev.autarkia.worker.WorkerEntity we && we.held(s -> s.is(j.item)) > 0 ? -1
                    : w.workPos().distManhattan(j.target);
                if (w.accepts(j) && (d <= 128 || far) && (d < bestD || d == bestD && w.workerId().compareTo(best.workerId()) < 0)) {
                    best = w;
                    bestD = d;
                }
            }
            if (best == null) {
                continue;
            }
            List<ColonyJob> order = bundle(j, open, best);
            for (ColonyJob o : order) {
                o.status = ColonyJob.Status.ASSIGNED;
                o.assignee = best.workerId();
                o.leaseUntil = now + 1200;
            }
            idle.remove(best);
            best.assign(new ColonyWorker.WorkOrder(UUID.randomUUID(), order));
            if (j.type == ColonyJob.Type.FOUND_OUTPOST && c.workers.values().stream().anyMatch(e -> e.type().getPath().equals("sentinel"))) {
                ColonyJob escort = new ColonyJob(ColonyJob.Type.ESCORT, j.projectId, j.target.above(), null, null, 0, j.priority, j.phase, now, null, null, 0);
                escort.subject = best.workerId();
                add(escort);
            }
        }
    }

    private static List<ColonyJob> bundle(ColonyJob first, List<ColonyJob> open, ColonyWorker w) {
        List<ColonyJob> out = new ArrayList<>(List.of(first));
        if (first.type != ColonyJob.Type.PLACE && first.type != ColonyJob.Type.LEVEL_CUT && first.type != ColonyJob.Type.LEVEL_FILL) {
            return out;
        }
        Item item = first.item;
        int limit = Math.min(8, Math.max(1, w.cargoStacks() * 64));
        BlockPos last = first.target;
        List<ColonyJob> pool = new ArrayList<>(open.stream().filter(o -> o != first && o.status == ColonyJob.Status.OPEN && o.type == first.type
            && o.target.getY() == first.target.getY() && java.util.Objects.equals(o.item, item) && java.util.Objects.equals(o.projectId, first.projectId)
            && w.accepts(o)).toList());
        while (out.size() < limit && !pool.isEmpty()) {
            BlockPos from = last;
            ColonyJob next = pool.stream().min(Comparator.comparingDouble((ColonyJob o) -> o.target.distSqr(from)).thenComparingLong(o -> o.target.asLong())).orElseThrow();
            pool.remove(next);
            out.add(next);
            last = next.target;
        }
        return out;
    }

    /** Renew the lease of a job the worker is executing. */
    public void progress(UUID jobId, long now) {
        ColonyJob j = jobs.get(jobId);
        if (j != null && j.status == ColonyJob.Status.ASSIGNED) {
            j.leaseUntil = now + 1200;
        }
    }

    /**
     * Start the timeout of a job when its worker reaches the job site: the timeout counts work time from arrival, travel
     * and the wait for earlier jobs of a bundle run on the renewed lease.
     */
    public void started(UUID jobId, long now) {
        ColonyJob j = jobs.get(jobId);
        if (j != null && j.status == ColonyJob.Status.ASSIGNED) {
            j.leaseUntil = now + 1200;
            assignedAt.put(jobId, now);
        }
    }

    /** Report a finished job. */
    public void done(Colony c, ServerLevel level, UUID jobId) {
        ColonyJob j = jobs.get(jobId);
        if (j == null) {
            return;
        }
        j.status = ColonyJob.Status.DONE;
        assignedAt.remove(jobId);
        if (j.type == ColonyJob.Type.DELIVER && j.projectId == null) {
            for (int i = 0; i < c.deliveries.size(); i++) {
                Colony.Delivery d = c.deliveries.get(i);
                if (d.target().equals(j.target) && d.item() == j.item) {
                    if (d.count() > j.count) {
                        c.deliveries.set(i, new Colony.Delivery(d.target(), d.item(), d.count() - j.count));
                    } else {
                        c.deliveries.remove(i);
                    }
                    Colonies.changed();
                    break;
                }
            }
        }
        if (j.projectId != null) {
            Project p = c.project(j.projectId);
            if (p != null) {
                p.cursor++;
                if (j.type == ColonyJob.Type.PLACE || j.type == ColonyJob.Type.SIGN) {
                    c.stat("blocks_placed", 1);
                }
                if (j.type == ColonyJob.Type.CRANK && p.phase == 5) {
                    nextPhase(c, level, p);
                }
                Colonies.changed();
            }
        }
    }

    /** Report a failed job: release it, count the attempt (not for unloaded chunks or a lost worker), back off or block. */
    public void failed(Colony c, ServerLevel level, UUID jobId, ColonyJob.Failure reason) {
        ColonyJob j = jobs.get(jobId);
        if (j == null) {
            return;
        }
        assignedAt.remove(jobId);
        j.assignee = null;
        j.status = ColonyJob.Status.OPEN;
        if (j.type == ColonyJob.Type.SIGN && reason == ColonyJob.Failure.ITEM_MISSING) {
            j.status = ColonyJob.Status.DONE;
            ColonyRuntime.of(c).log("Sign skipped: no sign in storage");
            return;
        }
        if (reason == ColonyJob.Failure.CHUNK_UNLOADED || reason == ColonyJob.Failure.WORKER_LOST) {
            return;
        }
        if (reason == ColonyJob.Failure.ITEM_MISSING && j.item != null && carried(c, level, j.item)) {
            j.backoffUntil = level.getGameTime() + 400;
            ColonyRuntime.of(c).log("Job " + j.type.name().toLowerCase(java.util.Locale.ROOT) + " at " + j.target.toShortString() + " waits: a worker still carries "
                + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(j.item).getPath());
            return;
        }
        j.attempts++;
        j.backoffUntil = level.getGameTime() + 200L * j.attempts;
        if (j.projectId == null && j.attempts >= AutarkiaConfig.COLONY_JOB_RETRY_LIMIT.get()) {
            remove(j);
            return;
        }
        if (j.attempts >= AutarkiaConfig.COLONY_JOB_RETRY_LIMIT.get()) {
            j.status = ColonyJob.Status.BLOCKED;
            j.blockReason = reason.name().toLowerCase(java.util.Locale.ROOT);
            Project p = c.project(j.projectId);
            if (p != null) {
                block(c, p, j.blockReason);
            }
        }
        ColonyRuntime.of(c).log("Job " + j.type.name().toLowerCase(java.util.Locale.ROOT) + " at " + j.target.toShortString() + " failed: "
            + reason.name().toLowerCase(java.util.Locale.ROOT));
        Autarkia.LOG.info("[colony] {}: job {} at {} failed {} (attempt {})", c.name, j.type, j.target.toShortString(), reason, j.attempts);
    }

    /** Return whether a worker of the colony carries the item (it took it for a job it then dropped; it brings it back to storage). */
    private static boolean carried(Colony c, ServerLevel level, Item item) {
        for (Colony.WorkerEntry e : c.workers.values()) {
            if (level.getEntity(e.id()) instanceof dev.autarkia.worker.WorkerEntity w && w.held(s -> s.is(item)) > 0) {
                return true;
            }
        }
        return false;
    }

    private void block(Colony c, Project p, String reason) {
        p.state = Project.State.BLOCKED;
        p.blockReason = reason;
        ColonyRuntime.of(c).log("Blocked " + p.template.getPath() + ": " + reason);
        ColonyEvents.alert(c, "message.autarkia.colony.blocked", p.template.getPath(), reason);
        Colonies.changed();
    }

    /** Reset every blocked job of a project and resume it (card Retry button). */
    public void retry(Colony c, Project p) {
        if (p.state != Project.State.BLOCKED) {
            return;
        }
        for (ColonyJob j : jobs.values()) {
            if (p.id.equals(j.projectId) && j.status == ColonyJob.Status.BLOCKED) {
                j.status = ColonyJob.Status.OPEN;
                j.attempts = 0;
                j.backoffUntil = 0;
            }
        }
        p.state = p.phase == 5 ? Project.State.COMMISSIONING : Project.State.BUILDING;
        p.blockReason = "";
        ServerLevel level = Colonies.level(c);
        if (level != null && p.phase == 5) {
            openPhase(c, level, p);
        }
        Colonies.changed();
    }

    /** Cancel one job (Jobs tab); a project job is opened again by its phase later. */
    public void cancel(Colony c, ServerLevel level, UUID jobId) {
        ColonyJob j = jobs.get(jobId);
        if (j == null) {
            return;
        }
        if (j.assignee != null && level.getEntity(j.assignee) instanceof ColonyWorker w) {
            w.abort(ColonyJob.Failure.WORKER_LOST);
        }
        remove(j);
    }

    /** Return the state a job places (cell state or sign state). */
    public static @Nullable BlockState placeState(ColonyJob j) {
        return j.state;
    }
}

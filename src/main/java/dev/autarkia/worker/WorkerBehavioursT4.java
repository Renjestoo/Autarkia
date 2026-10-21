package dev.autarkia.worker;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.colony.Deposits;
import dev.autarkia.colony.Outposts;
import dev.autarkia.processing.OreOutcropBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryCondition;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.core.behaviour.base.ExtendedBehaviour;
import net.tslat.smartbrainlib.library.object.MemoryTest;
import org.jspecify.annotations.Nullable;

/** The T4 worker behaviours of 06 section 3.6 (Surveyor, Sentinel, outpost carriers) and the generic DELIVER step. */
public final class WorkerBehavioursT4 {
    public static final int SCAN_SPACING = 32;
    public static final int SCAN_RADIUS = 32;
    public static final int SCAN_RING_COLOR = 0xFF7CFF6B;
    public static final int FLY_ABOVE = 6;

    private WorkerBehavioursT4() {
    }

    /** Fly toward a position, by 32-block waypoints when it is more than 48 blocks away. */
    static void fly(WorkerEntity w, BlockPos to) {
        double dx = to.getX() + 0.5 - w.getX();
        double dz = to.getZ() + 0.5 - w.getZ();
        WorkerBrain.walkTo(w, dx * dx + dz * dz > 48 * 48 ? WorkerBrain.waypoint(w, to) : to, 1);
    }

    /**
     * Return the scan points of a survey: a square spiral of points {@value #SCAN_SPACING} blocks apart around the center
     * (center first, then ring by ring), every point within the radius on both axes.
     */
    public static List<BlockPos> spiral(BlockPos center, int radius) {
        List<BlockPos> out = new ArrayList<>(List.of(center));
        int rings = Math.max(0, radius) / SCAN_SPACING;
        for (int k = 1; k <= rings; k++) {
            int x = -k;
            int z = -k;
            int[][] legs = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
            for (int[] leg : legs) {
                for (int step = 0; step < 2 * k; step++) {
                    out.add(center.offset(x * SCAN_SPACING, 0, z * SCAN_SPACING));
                    x += leg[0];
                    z += leg[1];
                }
            }
        }
        return out;
    }

    /**
     * One survey scan at a spiral point (06 section 3.6, 2,000 FE): trigger {@code scan}, a green ring of 24 glow
     * particles, the colony stat {@code survey_scans} + 1 and the scan of every column within 32 blocks (+16 per range upgrade) of the point: the top block of the
     * column (below the WORLD_SURFACE heightmap) is checked, and every {@code ore_outcrop} goes to the colony deposit
     * list with its ore and the count of {@code c:ores/<ore>} blocks in the 9 x 9 columns under it, 32 blocks deep.
     * Columns in chunks that are not loaded are loaded for the scan. Returns the outcrops found.
     */
    public static int scan(WorkerEntity w, ServerLevel level, BlockPos point) {
        w.trigger("scan");
        w.spend(WorkerType.SCAN_COST);
        w.sound("entity.worker.scan", 1.0f, 1.0f);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            level.sendParticles(GlowParticleOptions.of(SCAN_RING_COLOR).size(0.25f), w.getX() + Math.cos(a) * 2, w.getY() + 0.2, w.getZ() + Math.sin(a) * 2,
                1, 0, 0, 0, 0);
        }
        Colony c = w.colony();
        if (c == null) {
            return 0;
        }
        c.stat("survey_scans", 1);
        int r = SCAN_RADIUS + 16 * w.upgrades.count(WorkerUpgrades.Kind.RANGE);
        int found = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                int x = point.getX() + dx;
                int z = point.getZ() + dz;
                BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
                BlockState s = level.getBlockState(top);
                if (s.getBlock() instanceof OreOutcropBlock) {
                    OreOutcropBlock.Ore ore = s.getValue(OreOutcropBlock.ORE);
                    Deposits.report(c, level, top, ore.getSerializedName(), estimate(level, top, ore));
                    found++;
                }
            }
        }
        return found;
    }

    /** Return the number of blocks of the ore's {@code c:ores/<ore>} tag in the 9 x 9 columns under an outcrop, from y - 1 down 32 blocks. */
    public static int estimate(ServerLevel level, BlockPos outcrop, OreOutcropBlock.Ore ore) {
        int n = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = 1; dy <= 32; dy++) {
                    p.set(outcrop.getX() + dx, outcrop.getY() - dy, outcrop.getZ() + dz);
                    if (level.getBlockState(p).is(ore.oreTag())) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /**
     * SurveyArea (WORK, SURVEY): flies the square spiral over the job area ({@code duration} = radius) at the heightmap
     * + 6 with travel tickets and scans at every point; the job is done when the spiral is done. Each scan restarts the
     * job timeout (6,000 ticks without a scan time out). A scan needs its 2,000 FE above the return reserve, else the
     * worker releases the job to recharge, and a worker that gets the same job back after a recharge goes on from the
     * next point.
     */
    public static final class SurveyArea extends WorkerBehaviours.Step {
        private @Nullable UUID jobId;
        private int index;
        private List<BlockPos> points = List.of();

        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return WorkerBehaviours.phase(w) == WorkerMemories.Phase.WORK && job.type == ColonyJob.Type.SURVEY;
        }

        @Override
        protected void start(WorkerEntity w) {
            ColonyJob job = w.currentJob();
            if (job == null) {
                return;
            }
            if (!job.id.equals(jobId)) {
                jobId = job.id;
                index = 0;
            }
            points = spiral(job.target, job.duration);
        }

        @Override
        protected void tick(WorkerEntity w) {
            if (w.currentJob() == null) {
                return;
            }
            if (index >= points.size()) {
                jobId = null;
                w.jobDone();
                return;
            }
            ServerLevel level = (ServerLevel) w.level();
            BlockPos p = points.get(index);
            BlockPos at = new BlockPos(p.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ()) + FLY_ABOVE, p.getZ());
            if (!WorkerTravel.hold(w, at, WorkerEntity.State.WORKING)) {
                return;
            }
            double dx = at.getX() + 0.5 - w.getX();
            double dz = at.getZ() + 0.5 - w.getZ();
            if (dx * dx + dz * dz > 4 || Math.abs(at.getY() - w.getY()) > 3) {
                if (w.tickCount % 10 == 0 || !w.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
                    fly(w, at);
                }
                return;
            }
            w.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            if (w.energy() < WorkerType.SCAN_COST + w.reserve()) {
                w.getBrain().setMemory(WorkerMemories.NEEDS_RECHARGE.get(), net.minecraft.util.Unit.INSTANCE);
                w.releaseOrder();
                return;
            }
            scan(w, level, p);
            w.startedWork();
            index++;
        }
    }

    /**
     * FoundOutpost (WORK, FOUND_OUTPOST; Porter and Hauler Crab, 05 section 7.2): at the site the carrier plants the
     * {@code outpost_seed} it fetched; the job fails TARGET_OBSTRUCTED when the outpost cannot be founded there.
     */
    public static final class FoundOutpost extends WorkerBehaviours.Step {
        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return WorkerBehaviours.phase(w) == WorkerMemories.Phase.WORK && job.type == ColonyJob.Type.FOUND_OUTPOST;
        }

        @Override
        protected void tick(WorkerEntity w) {
            ColonyJob job = w.currentJob();
            Colony c = w.colony();
            if (job == null || w.tickCount % 10 != 0) {
                return;
            }
            if (c != null && Outposts.found((ServerLevel) w.level(), c, job.target, w)) {
                w.trigger("place");
                w.jobDone();
            } else {
                w.jobFailed(ColonyJob.Failure.TARGET_OBSTRUCTED);
            }
        }
    }

    /**
     * EscortEntity (WORK, ESCORT): flies 4 to 8 blocks from the escorted entity ({@code subject}), 4.5 blocks from it on
     * its own side and 3 blocks up (5.4 blocks, the middle of the band), aimed 5 ticks ahead of the entity's motion:
     * within 16 blocks of that point it steers
     * straight at it every tick (open air above the escorted entity), farther away it paths there, re-aimed every 2
     * ticks; with travel tickets. The Sentinel's target sensor covers hostiles within 12 blocks of it. Done when the
     * escorted entity is gone, is within 4 blocks of the job target, or (a worker) has finished its job; a worker that
     * never shows a job within 200 ticks ends the escort too.
     */
    public static final class EscortEntity extends WorkerBehaviours.Step {
        private static final double LEAD_TICKS = 5;
        private static final double SIDE = 4.5;
        private static final double DIRECT_RANGE = 16;
        private boolean seenJob;
        private int ticks;

        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return WorkerBehaviours.phase(w) == WorkerMemories.Phase.WORK && job.type == ColonyJob.Type.ESCORT;
        }

        @Override
        protected void start(WorkerEntity w) {
            seenJob = false;
            ticks = 0;
        }

        @Override
        protected void tick(WorkerEntity w) {
            ColonyJob job = w.currentJob();
            if (job == null) {
                return;
            }
            Entity subject = job.subject == null ? null : ((ServerLevel) w.level()).getEntity(job.subject);
            if (subject == null || !subject.isAlive() || subject.position().distanceToSqr(Vec3.atCenterOf(job.target)) <= 16) {
                w.jobDone();
                return;
            }
            if (subject instanceof WorkerEntity carrier) {
                if (carrier.currentJob() != null) {
                    seenJob = true;
                } else if (seenJob || ticks > 200) {
                    w.jobDone();
                    return;
                }
            }
            ticks++;
            Vec3 side = new Vec3(w.getX() - subject.getX(), 0, w.getZ() - subject.getZ());
            side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
            Vec3 lead = subject.getDeltaMovement().multiply(LEAD_TICKS, 0, LEAD_TICKS);
            Vec3 aim = subject.position().add(lead).add(side.scale(SIDE)).add(0, 3, 0);
            BlockPos spot = BlockPos.containing(aim);
            if (!WorkerTravel.hold(w, spot, WorkerEntity.State.WORKING)) {
                return;
            }
            w.getLookControl().setLookAt(subject);
            if (w.position().distanceTo(aim) > DIRECT_RANGE) {
                if (w.tickCount % 2 == 0 || !w.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
                    fly(w, spot);
                }
                return;
            }
            w.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            w.getNavigation().stop();
            w.getMoveControl().setWantedPosition(aim.x, aim.y, aim.z, 1.0);
        }
    }

    /** DELIVER (WORK): hands the fetched job items into the target block's item handler, see {@link WorkerActions#deliver}. */
    public static final class DeliverItems extends WorkerBehaviours.Step {
        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return WorkerBehaviours.phase(w) == WorkerMemories.Phase.WORK && job.type == ColonyJob.Type.DELIVER;
        }

        @Override
        protected void tick(WorkerEntity w) {
            ColonyJob job = w.currentJob();
            if (job != null && w.tickCount % 10 == 0) {
                WorkerBehaviours.finish(w, WorkerActions.deliver(w, job));
            }
        }
    }

    /**
     * GuardPatrol (IDLE, Sentinel without a job, 06 section 3.4): flies the {@code guard_route} loop and hovers 4 blocks
     * above each point for 100 ticks; without a route yet it holds above its home. It replaces ReturnToPark for the
     * Sentinel, so it also ends the RETURNING state after a job.
     */
    public static final class GuardPatrol extends ExtendedBehaviour<WorkerEntity> {
        public static final int HOVER_TICKS = 100;
        private int index;
        private int hover;

        public GuardPatrol() {
            noTimeout();
        }

        @Override
        public Set<MemoryCondition<?, ?>> getMemoryRequirements() {
            return MemoryTest.builder(1).noMemory(WorkerMemories.ASSIGNED_JOB.get());
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, WorkerEntity w) {
            return w.state() != WorkerEntity.State.FAILED && w.cargoEmpty();
        }

        @Override
        protected boolean shouldKeepRunning(WorkerEntity w) {
            return w.state() != WorkerEntity.State.FAILED && !w.getBrain().hasMemoryValue(WorkerMemories.ASSIGNED_JOB.get());
        }

        @Override
        protected void start(WorkerEntity w) {
            if (w.state() == WorkerEntity.State.RETURNING) {
                w.setState(WorkerEntity.State.IDLE);
            }
        }

        @Override
        protected void tick(WorkerEntity w) {
            List<BlockPos> route = w.getBrain().getMemory(WorkerMemories.GUARD_ROUTE.get()).filter(r -> !r.isEmpty()).orElse(List.of(w.home()));
            if (index >= route.size()) {
                index = 0;
            }
            BlockPos p = route.get(index).above(4);
            if (!WorkerBehaviours.near(w, p, 2.5)) {
                hover = 0;
                if (w.tickCount % 20 == 0 || !w.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
                    fly(w, p);
                }
                return;
            }
            w.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            if (++hover >= HOVER_TICKS) {
                hover = 0;
                index++;
            }
        }

        @Override
        protected void stop(WorkerEntity w) {
            w.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }
}

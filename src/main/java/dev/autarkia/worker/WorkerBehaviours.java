package dev.autarkia.worker;

import dev.autarkia.colony.ColonyEvents;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.energy.MuscleGenerator;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryCondition;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.core.behaviour.base.ExtendedBehaviour;
import net.tslat.smartbrainlib.library.object.MemoryTest;

/** The custom worker behaviours of 06 section 3.6 for the walker rig. */
public final class WorkerBehaviours {
    private WorkerBehaviours() {
    }

    static WorkerMemories.Phase phase(WorkerEntity w) {
        return w.getBrain().getMemory(WorkerMemories.JOB_PHASE.get()).orElse(null);
    }

    private static void setPhase(WorkerEntity w, WorkerMemories.Phase p) {
        w.getBrain().setMemory(WorkerMemories.JOB_PHASE.get(), p);
        if (p == WorkerMemories.Phase.WORK) {
            w.startedWork();
        }
        w.setState(switch (p) {
            case FETCH -> WorkerEntity.State.FETCHING;
            case TRAVEL -> WorkerEntity.State.TRAVELLING;
            case WORK -> WorkerEntity.State.WORKING;
            default -> WorkerEntity.State.RETURNING;
        });
    }

    static boolean near(WorkerEntity w, BlockPos p, double reach) {
        return w.position().distanceToSqr(Vec3.atCenterOf(p)) <= reach * reach;
    }

    /** Base: a behaviour that keeps running while its condition holds. */
    abstract static class Step extends ExtendedBehaviour<WorkerEntity> {
        Step() {
            noTimeout();
        }

        @Override
        public Set<MemoryCondition<?, ?>> getMemoryRequirements() {
            return MemoryTest.builder(1).hasMemory(WorkerMemories.ASSIGNED_JOB.get());
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, WorkerEntity w) {
            return w.currentJob() != null && active(w, w.currentJob());
        }

        @Override
        protected boolean shouldKeepRunning(WorkerEntity w) {
            return w.currentJob() != null && active(w, w.currentJob());
        }

        abstract boolean active(WorkerEntity w, ColonyJob job);
    }

    /**
     * Picks the phase of the current job and renews its lease (06 section 3.6 ExecuteJob). When no storage holds the
     * job's items, the job fails ITEM_MISSING and the worker shows FAILED item_missing for 200 ticks (06 test 11).
     */
    public static final class ExecuteJob extends Step {
        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return true;
        }

        @Override
        protected void tick(WorkerEntity w) {
            ColonyJob job = w.currentJob();
            if (job == null) {
                return;
            }
            if (phase(w) == null) {
                if (WorkerActions.missing(w, job) > 0) {
                    BlockPos src = WorkerActions.source(w, job);
                    if (src == null) {
                        w.jobFailed(ColonyJob.Failure.ITEM_MISSING);
                        w.fail("item_missing");
                        return;
                    }
                    w.getBrain().setMemory(WorkerMemories.FETCH_SOURCE.get(), GlobalPos.of(w.level().dimension(), src));
                    setPhase(w, WorkerMemories.Phase.FETCH);
                } else if (job.type == ColonyJob.Type.ESCORT) {
                    setPhase(w, WorkerMemories.Phase.WORK);
                } else {
                    setPhase(w, WorkerMemories.Phase.TRAVEL);
                }
            }
            if (w.tickCount % 20 == 0) {
                w.progress();
            }
        }
    }

    /** Walks to the reserved source and takes the job's material (FetchFromStorage). */
    public static final class FetchFromStorage extends Step {
        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return phase(w) == WorkerMemories.Phase.FETCH;
        }

        @Override
        protected void tick(WorkerEntity w) {
            BlockPos src = w.getBrain().getMemory(WorkerMemories.FETCH_SOURCE.get()).map(GlobalPos::pos).orElse(null);
            ColonyJob job = w.currentJob();
            if (src == null || job == null) {
                return;
            }
            if (!near(w, src, 2.6)) {
                if (WorkerTravel.hold(w, src, WorkerEntity.State.FETCHING)) {
                    WorkerBrain.walkTo(w, src, 1);
                }
                return;
            }
            if (!WorkerActions.fetch(w, src, job)) {
                w.jobFailed(ColonyJob.Failure.ITEM_MISSING);
                return;
            }
            w.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            setPhase(w, WorkerActions.missing(w, job) > 0 ? WorkerMemories.Phase.FETCH : WorkerMemories.Phase.TRAVEL);
            if (WorkerActions.missing(w, job) > 0) {
                BlockPos next = WorkerActions.source(w, job);
                if (next == null || next.equals(src)) {
                    setPhase(w, WorkerMemories.Phase.TRAVEL);
                } else {
                    w.getBrain().setMemory(WorkerMemories.FETCH_SOURCE.get(), GlobalPos.of(w.level().dimension(), next));
                }
            }
        }
    }

    /**
     * Walks into reach of the job position (TravelToJobSite). Within 40 blocks a path check runs every 20 ticks; three
     * checks in a row without a path that reaches the target (for a drone: its hover point near the target) mark it
     * unreachable and fail the job NO_PATH (06 section 6.2); longer trips rely on the stuck levels.
     */
    public static final class TravelToJobSite extends Step {
        private int noPath;

        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return phase(w) == WorkerMemories.Phase.TRAVEL && job.type != ColonyJob.Type.ESCORT;
        }

        @Override
        protected void start(WorkerEntity w) {
            noPath = 0;
        }

        @Override
        protected void tick(WorkerEntity w) {
            ColonyJob job = w.currentJob();
            if (job == null) {
                return;
            }
            double reach = job.type == ColonyJob.Type.CRANK ? 2.2 : w.reach();
            if (near(w, job.target, reach)) {
                w.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                setPhase(w, WorkerMemories.Phase.WORK);
                return;
            }
            if (w.unreachable(job.target)) {
                w.jobFailed(ColonyJob.Failure.NO_PATH);
                return;
            }
            int close = job.type == ColonyJob.Type.CRANK ? 1 : 2;
            if (w.tickCount % 20 == 0 && job.target.distSqr(w.blockPosition()) <= 40 * 40) {
                BlockPos aim = w.workerType.flying() ? WorkerBrain.waypoint(w, job.target) : job.target;
                noPath = pathReaches(w, aim, aim.equals(job.target) ? reach : reach + 2) ? 0 : noPath + 1;
                if (noPath >= 3) {
                    w.markUnreachable(job.target);
                    w.jobFailed(ColonyJob.Failure.NO_PATH);
                    return;
                }
            }
            if (WorkerTravel.hold(w, job.target, WorkerEntity.State.TRAVELLING)) {
                WorkerBrain.walkTo(w, WorkerBrain.waypoint(w, job.target), close);
            }
        }
    }

    /**
     * Return true when a path ends within reach of the target. The pathfinder accuracy is a Manhattan distance, so it is
     * widened and the end node is checked by straight distance; raised targets (cranks, pulley tops) stay reachable.
     */
    static boolean pathReaches(WorkerEntity w, BlockPos target, double reach) {
        net.minecraft.world.level.pathfinder.Path path = w.getNavigation().createPath(target, (int) Math.ceil(reach * 1.8) + 1);
        return path != null && path.getEndNode() != null
            && Math.sqrt(path.getEndNode().asBlockPos().distSqr(target)) <= reach + 1.5;
    }

    static void finish(WorkerEntity w, WorkerActions.Result r) {
        switch (r) {
            case DONE -> w.jobDone();
            case FAIL_ITEMS -> w.jobFailed(ColonyJob.Failure.ITEM_MISSING);
            case FAIL_OBSTRUCTED -> w.jobFailed(ColonyJob.Failure.TARGET_OBSTRUCTED);
            default -> {
            }
        }
    }

    /** Places the planned block, fill block or sign (PlaceBlockFromJob). */
    public static final class PlaceBlockFromJob extends Step {
        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return phase(w) == WorkerMemories.Phase.WORK
                && (job.type == ColonyJob.Type.PLACE || job.type == ColonyJob.Type.LEVEL_FILL || job.type == ColonyJob.Type.SIGN);
        }

        @Override
        protected void start(WorkerEntity w) {
            w.setAction(WorkerEntity.ACTION_WORK);
        }

        @Override
        protected void tick(WorkerEntity w) {
            ColonyJob job = w.currentJob();
            if (job != null && w.tickCount % 6 == 0) {
                finish(w, WorkerActions.place(w, job));
            }
        }

        @Override
        protected void stop(WorkerEntity w) {
            w.setAction(WorkerEntity.ACTION_NONE);
        }
    }

    /** Breaks a planned cut cell (BreakPlannedBlock). */
    public static final class BreakPlannedBlock extends Step {
        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return phase(w) == WorkerMemories.Phase.WORK && job.type == ColonyJob.Type.LEVEL_CUT;
        }

        @Override
        protected void start(WorkerEntity w) {
            w.setAction(WorkerEntity.ACTION_WORK);
        }

        @Override
        protected void tick(WorkerEntity w) {
            ColonyJob job = w.currentJob();
            if (job != null && w.tickCount % 10 == 0) {
                finish(w, WorkerActions.cut(w, job));
            }
        }

        @Override
        protected void stop(WorkerEntity w) {
            w.setAction(WorkerEntity.ACTION_NONE);
        }
    }

    /** Strings a rope span between the two nodes (StringCable). */
    public static final class StringCable extends Step {
        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return phase(w) == WorkerMemories.Phase.WORK && job.type == ColonyJob.Type.STRING_CABLE && w.workerType != WorkerType.LINEMAN;
        }

        @Override
        protected void tick(WorkerEntity w) {
            ColonyJob job = w.currentJob();
            if (job != null && w.tickCount % 10 == 0) {
                finish(w, WorkerActions.string(w, job));
            }
        }
    }

    /**
     * Drives a hand crank or treadwheel for the job duration (CrankGenerator, 06 section 1.2 anti-loop drain); below the
     * return reserve it releases the job to the board so it can be claimed again after winding.
     */
    public static final class CrankGenerator extends Step {
        private int ticks;

        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return phase(w) == WorkerMemories.Phase.WORK && job.type == ColonyJob.Type.CRANK;
        }

        @Override
        protected void start(WorkerEntity w) {
            ticks = 0;
            w.setAction(WorkerEntity.ACTION_CRANK);
        }

        @Override
        protected void tick(WorkerEntity w) {
            ColonyJob job = w.currentJob();
            if (job == null || !(w.level().getBlockEntity(job.target) instanceof MuscleGenerator gen)) {
                w.jobFailed(ColonyJob.Failure.TARGET_OBSTRUCTED);
                return;
            }
            w.getLookControl().setLookAt(Vec3.atCenterOf(job.target));
            gen.setWorkerDriving(w, true);
            w.spend((int) Math.ceil(0.75 * gen.workerOutput()));
            if (w.tickCount % 10 == 0) {
                w.sound("entity.worker.crank", 0.5f, 0.8f);
            }
            if (w.energy() < w.reserve()) {
                gen.setWorkerDriving(w, false);
                w.releaseOrder();
            } else if (++ticks >= job.duration || gen.storageFull()) {
                gen.setWorkerDriving(w, false);
                w.jobDone();
            }
        }

        @Override
        protected void stop(WorkerEntity w) {
            w.setAction(WorkerEntity.ACTION_NONE);
        }
    }

    /**
     * Walks to each debris target and collects it into the cargo with the {@code scoop} trigger, 30 T per collected
     * entity (CollectDebris); the job is done when the cargo is full or no target is left, then DepositToStorage runs.
     * Items that do not fit go back on the ground as debris.
     */
    public static final class CollectDebris extends Step {
        @Override
        boolean active(WorkerEntity w, ColonyJob job) {
            return phase(w) == WorkerMemories.Phase.WORK && job.type == ColonyJob.Type.COLLECT_DEBRIS;
        }

        @Override
        protected void start(WorkerEntity w) {
            w.setAction(WorkerEntity.ACTION_WORK);
        }

        @Override
        protected void tick(WorkerEntity w) {
            ServerLevel sl = (ServerLevel) w.level();
            java.util.List<java.util.UUID> targets = new java.util.ArrayList<>(w.getBrain().getMemory(WorkerMemories.DEBRIS_TARGETS.get()).orElse(java.util.List.of()));
            net.minecraft.world.entity.Entity target = null;
            for (java.util.UUID id : targets) {
                net.minecraft.world.entity.Entity e = sl.getEntity(id);
                if (e != null && WorkerSensors.isDebris(e) && !w.unreachable(e.blockPosition())) {
                    target = e;
                    break;
                }
            }
            if (target == null || w.cargoFull()) {
                w.jobDone();
                return;
            }
            if (w.distanceToSqr(target) > 4.0) {
                WorkerBrain.walkTo(w, target.blockPosition(), 0);
                return;
            }
            w.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            for (net.minecraft.world.item.ItemStack s : WorkerSensors.collect(sl, target)) {
                net.minecraft.world.item.ItemStack rest = w.addCargo(s);
                if (!rest.isEmpty()) {
                    net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(sl, w.getX(), w.getY() + 0.5, w.getZ(), rest);
                    ie.setUnlimitedLifetime();
                    sl.addFreshEntity(ie);
                }
            }
            targets.remove(target.getUUID());
            w.getBrain().setMemory(WorkerMemories.DEBRIS_TARGETS.get(), targets);
            w.spend(WorkerType.DEBRIS_COST);
            w.trigger("scoop");
            w.sound("entity.worker.servo", 0.5f, 1.4f);
            var c = w.colony();
            if (c != null) {
                c.stat("debris_collected", 1);
                ColonyEvents.worker(c, "debris_collected");
            }
        }

        @Override
        protected void stop(WorkerEntity w) {
            w.setAction(WorkerEntity.ACTION_NONE);
        }
    }

    /**
     * Puts leftovers, cut drops and debris into the nearest colony storage the worker can reach (DepositToStorage); a
     * storage without a path, or where the worker gets stuck, goes to the unreachable cache for 600 ticks.
     */
    public static final class DepositToStorage extends ExtendedBehaviour<WorkerEntity> {
        private BlockPos target;

        public DepositToStorage() {
            runFor(400);
        }

        @Override
        public Set<MemoryCondition<?, ?>> getMemoryRequirements() {
            return MemoryTest.builder(1).noMemory(WorkerMemories.ASSIGNED_JOB.get());
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, WorkerEntity w) {
            if (w.cargoEmpty() || w.state() == WorkerEntity.State.FAILED) {
                return false;
            }
            target = reachableTarget(w);
            return target != null;
        }

        /** Return the nearest storage that takes the cargo and that the navigation reaches; others go to the unreachable cache. */
        private static BlockPos reachableTarget(WorkerEntity w) {
            for (int i = 0; i < 8; i++) {
                BlockPos t = WorkerActions.depositTarget(w);
                if (t == null) {
                    return null;
                }
                if (near(w, t, 2.6) || t.distSqr(w.blockPosition()) > 40 * 40) {
                    return t;
                }
                if (pathReaches(w, t, 2.6)) {
                    return t;
                }
                w.markUnreachable(t);
            }
            return null;
        }

        @Override
        protected boolean shouldKeepRunning(WorkerEntity w) {
            return !w.cargoEmpty() && target != null;
        }

        @Override
        protected void tick(WorkerEntity w) {
            if (w.getBrain().getMemory(WorkerMemories.STUCK_LEVEL.get()).orElse(0) >= 2) {
                w.markUnreachable(target);
                w.resetStuck();
                target = reachableTarget(w);
                return;
            }
            if (!near(w, target, 2.6)) {
                WorkerBrain.walkTo(w, target, 1);
                return;
            }
            if (!WorkerActions.deposit(w, target)) {
                target = reachableTarget(w);
            }
        }
    }

    /** Walks home after an order or when more than 16 blocks from home (ReturnToPark). */
    public static final class ReturnToPark extends ExtendedBehaviour<WorkerEntity> {
        public ReturnToPark() {
            runFor(600);
        }

        @Override
        public Set<MemoryCondition<?, ?>> getMemoryRequirements() {
            return MemoryTest.builder(1).noMemory(WorkerMemories.ASSIGNED_JOB.get());
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, WorkerEntity w) {
            return w.state() != WorkerEntity.State.FAILED && w.cargoEmpty()
                && (w.state() == WorkerEntity.State.RETURNING || w.blockPosition().distManhattan(w.home()) > 16);
        }

        @Override
        protected boolean shouldKeepRunning(WorkerEntity w) {
            return !near(w, w.home(), 2.5);
        }

        @Override
        protected void tick(WorkerEntity w) {
            WorkerBrain.walkTo(w, w.home(), 2);
        }

        @Override
        protected void stop(WorkerEntity w) {
            if (w.state() == WorkerEntity.State.RETURNING) {
                w.setState(WorkerEntity.State.IDLE);
            }
        }
    }

    /**
     * Goes to the charger and winds until full (RechargeAtDock, 06 sections 3.6 and 5.7); a worker wider than one block
     * cannot stand on the dock spot, so it stops within one block of it and docks from a reach widened by its extra width.
     * A docked worker stops moving (a drone holds its hover), so it stays in the charger's range.
     */
    public static final class RechargeAtDock extends ExtendedBehaviour<WorkerEntity> {
        public RechargeAtDock() {
            noTimeout();
        }

        @Override
        public Set<MemoryCondition<?, ?>> getMemoryRequirements() {
            return MemoryTest.builder(1).hasMemory(WorkerMemories.NEEDS_RECHARGE.get());
        }

        @Override
        protected boolean shouldKeepRunning(WorkerEntity w) {
            return w.getBrain().hasMemoryValue(WorkerMemories.NEEDS_RECHARGE.get());
        }

        @Override
        protected void start(WorkerEntity w) {
            w.setState(WorkerEntity.State.RECHARGING);
        }

        @Override
        protected void tick(WorkerEntity w) {
            if (w.energy() >= w.workerType.capacity()) {
                w.getBrain().eraseMemory(WorkerMemories.NEEDS_RECHARGE.get());
                w.sound("entity.worker.recharge_done", 0.5f, 1.8f);
                return;
            }
            BlockPos post = w.getBrain().getMemory(WorkerMemories.CHARGER_POS.get()).map(GlobalPos::pos).orElse(null);
            if (post == null || !(w.level().getBlockEntity(post) instanceof WorkerCharger wp) || !wp.serves(w)) {
                w.setAction(WorkerEntity.ACTION_NONE);
                return;
            }
            boolean wide = w.getBbWidth() > 1;
            if (!near(w, post, wp.dockReach() + (wide ? (w.getBbWidth() - 1) * 2 : 0)) || !w.workerType.flying() && w.getY() >= post.getY() + 1) {
                w.setAction(WorkerEntity.ACTION_NONE);
                if (WorkerTravel.hold(w, post, WorkerEntity.State.RECHARGING)) {
                    WorkerBrain.walkTo(w, wp.dockSpot(w), wide ? 1 : 0);
                }
                return;
            }
            if (!wp.dock(w)) {
                w.setAction(WorkerEntity.ACTION_NONE);
                return;
            }
            w.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            w.getNavigation().stop();
            if (w.workerType.flying()) {
                w.setDeltaMovement(Vec3.ZERO);
            }
            w.getLookControl().setLookAt(Vec3.atCenterOf(post));
            w.setAction(WorkerEntity.ACTION_RECHARGE);
        }

        @Override
        protected void stop(WorkerEntity w) {
            w.setAction(WorkerEntity.ACTION_NONE);
            BlockPos post = w.getBrain().getMemory(WorkerMemories.CHARGER_POS.get()).map(GlobalPos::pos).orElse(null);
            if (post != null && w.level().getBlockEntity(post) instanceof WorkerCharger wp) {
                wp.undock(w);
            }
            if (w.state() == WorkerEntity.State.RECHARGING) {
                w.setState(WorkerEntity.State.IDLE);
            }
            w.recover();
        }
    }

    /**
     * Stuck recovery levels 1 to 4 (06 section 3.8): 1 new path, 2 jump to a free side (a drone rises 3 blocks), 3 mark
     * the target unreachable and fail the job NO_PATH, 4 fail and alert; level 4 never moves the worker more than one
     * block.
     */
    public static final class StuckRecovery extends ExtendedBehaviour<WorkerEntity> {
        private int done;

        public StuckRecovery() {
            runFor(20);
        }

        @Override
        public Set<MemoryCondition<?, ?>> getMemoryRequirements() {
            return MemoryTest.builder(1).hasMemory(WorkerMemories.STUCK_LEVEL.get());
        }

        @Override
        protected void start(WorkerEntity w) {
            int level = w.getBrain().getMemory(WorkerMemories.STUCK_LEVEL.get()).orElse(0);
            if (level < done) {
                done = 0;
            }
            if (level <= done) {
                return;
            }
            done = level;
            switch (level) {
                case 1 -> w.getNavigation().recomputePath();
                case 2 -> jump(w);
                case 3 -> {
                    ColonyJob job = w.currentJob();
                    if (job != null) {
                        w.markUnreachable(job.target);
                        w.jobFailed(ColonyJob.Failure.NO_PATH);
                    }
                    w.setState(WorkerEntity.State.RETURNING);
                }
                default -> {
                    nudge(w);
                    w.fail("no_path");
                    var c = w.colony();
                    if (c != null && dev.autarkia.config.AutarkiaConfig.STUCK_ALERTS.get()) {
                        ColonyEvents.alert(c, "message.autarkia.worker.stuck", w.blockPosition().toShortString());
                    }
                    w.resetStuck();
                    done = 0;
                }
            }
        }

        @Override
        protected void stop(WorkerEntity w) {
            if (!w.getBrain().hasMemoryValue(WorkerMemories.STUCK_LEVEL.get())) {
                done = 0;
            }
        }

        private static void jump(WorkerEntity w) {
            if (w.workerType.flying()) {
                w.setDeltaMovement(w.getDeltaMovement().add(0, 0.45, 0));
                w.getMoveControl().setWantedPosition(w.getX(), w.getY() + 3, w.getZ(), 1.0);
                return;
            }
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos p = w.blockPosition().relative(d);
                if (w.level().getBlockState(p).getCollisionShape(w.level(), p).isEmpty()
                    && w.level().getBlockState(p.above()).getCollisionShape(w.level(), p.above()).isEmpty()) {
                    hop(w);
                    w.getMoveControl().setWantedPosition(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 1.0);
                    return;
                }
            }
            hop(w);
        }

        private static void hop(WorkerEntity w) {
            if (w.onGround()) {
                w.jumpFromGround();
            } else {
                w.getJumpControl().jump();
            }
        }

        private static void nudge(WorkerEntity w) {
            BlockPos at = w.blockPosition();
            if (w.level().getBlockState(at).getCollisionShape(w.level(), at).isEmpty()) {
                return;
            }
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-1, 0, -1), at.offset(1, 1, 1))) {
                if (w.level().getBlockState(p).getCollisionShape(w.level(), p).isEmpty()
                    && w.level().getBlockState(p.above()).getCollisionShape(w.level(), p.above()).isEmpty()) {
                    w.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                    return;
                }
            }
        }
    }
}

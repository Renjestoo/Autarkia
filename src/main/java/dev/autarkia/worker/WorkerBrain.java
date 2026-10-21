package dev.autarkia.worker;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryCondition;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.ActivityBuilder;
import net.tslat.smartbrainlib.api.core.behaviour.base.ExtendedBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.base.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.base.OneRandomBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.FloatToSurfaceOfFluid;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.InteractWithDoor;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetRandomWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetPlayerLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor;
import net.tslat.smartbrainlib.library.object.MemoryTest;

/**
 * The SmartBrainLib brain of a worker (06 section 3.2): sensors, the always-running core, IDLE, WORK, REST and AVOID
 * activities with the priority {@code FIGHT, AVOID, REST, WORK, IDLE}. Getters read only the entity type, because the
 * brain is built inside the {@code LivingEntity} constructor before the worker's own fields exist.
 */
public final class WorkerBrain {
    private WorkerBrain() {
    }

    /** Set a walk target. */
    public static void walkTo(WorkerEntity w, BlockPos pos, int closeEnough) {
        w.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosTracker(pos), 1.0f, closeEnough));
        w.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
    }

    /**
     * Return the next point toward a target (06 section 3.7): a trip longer than 48 blocks goes by waypoints every 32
     * blocks on the straight line, at the heightmap height (drones 6 blocks above it); a drone aims 2 blocks above a near
     * target, or at the first free cell above or beside it when that spot is blocked.
     */
    public static BlockPos waypoint(WorkerEntity w, BlockPos target) {
        double dx = target.getX() - w.getX();
        double dz = target.getZ() - w.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d <= 48) {
            return w.workerType.flying() ? hover(w.level(), target) : target;
        }
        int x = (int) Math.floor(w.getX() + dx / d * 32);
        int z = (int) Math.floor(w.getZ() + dz / d * 32);
        int y = w.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y + (w.workerType.flying() ? 6 : 0), z);
    }

    private static BlockPos hover(net.minecraft.world.level.Level level, BlockPos target) {
        List<BlockPos> spots = new java.util.ArrayList<>(List.of(target.above(2), target.above(3), target.above()));
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            for (int dy = 0; dy <= 2; dy++) {
                spots.add(target.relative(d).above(dy));
            }
        }
        for (BlockPos p : spots) {
            if (level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                return p;
            }
        }
        return target.above(2);
    }

    /** The SmartBrainOwner part of {@link WorkerEntity}. */
    public interface Owner extends SmartBrainOwner<WorkerEntity> {
        @Override
        default List<? extends ExtendedSensor<? extends WorkerEntity>> getSensors(WorkerEntity entity) {
            List<ExtendedSensor<WorkerEntity>> out = new java.util.ArrayList<>(List.of(
                new NearbyPlayersSensor<WorkerEntity>().scanRate(40),
                new HurtBySensor<WorkerEntity>().afterScanning(WorkerBrain::avoidAttacker),
                new WorkerSensors.JobBoardSensor(),
                new WorkerSensors.EnergySensor(),
                new WorkerSensors.ChargerSensor(),
                new WorkerSensors.StuckSensor()));
            if (WorkerType.of(entity.getType()).jobs().contains(dev.autarkia.colony.ColonyJob.Type.COLLECT_DEBRIS)) {
                out.add(new WorkerSensors.DebrisSensor());
            }
            return out;
        }

        @Override
        default List<? extends net.minecraft.world.entity.ai.behavior.BehaviorControl<? super WorkerEntity>> getAlwaysRunningBehaviours(WorkerEntity entity) {
            if (!"walker".equals(WorkerType.of(entity.getType()).rig())) {
                return List.of(new LookAtTarget<>(), new MoveToWalkTarget<>(), new FloatToSurfaceOfFluid<>(), new WorkerBehaviours.StuckRecovery());
            }
            return List.of(new LookAtTarget<>(), new MoveToWalkTarget<>(), new FloatToSurfaceOfFluid<>(), new InteractWithDoor<>(),
                new WorkerBehaviours.StuckRecovery());
        }

        @Override
        default List<? extends net.minecraft.world.entity.ai.behavior.BehaviorControl<? super WorkerEntity>> getIdleBehaviours(WorkerEntity entity) {
            java.util.function.Predicate<WorkerEntity> wander = w -> w.state() == WorkerEntity.State.IDLE && w.blockPosition().distManhattan(w.home()) <= 16;
            ExtendedBehaviour<WorkerEntity> random = WorkerType.of(entity.getType()).flying()
                ? new net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetRandomFlyTarget<WorkerEntity>().setRadius(6, 3).startCondition(wander)
                : new SetRandomWalkTarget<WorkerEntity>().setRadius(6).startCondition(wander);
            return List.of(
                new WorkerBehaviours.DepositToStorage(),
                new FirstApplicableBehaviour<WorkerEntity>(new SetPlayerLookTarget<>(), new SetRandomLookTarget<>()),
                new WorkerBehaviours.ReturnToPark(),
                new OneRandomBehaviour<WorkerEntity>(
                    it.unimi.dsi.fastutil.objects.ObjectIntPair.of(random, 3),
                    it.unimi.dsi.fastutil.objects.ObjectIntPair.of(new Idle<WorkerEntity>().runFor(e -> 20 + e.getRandom().nextInt(40)), 1))
                    .cooldownFor(e -> dev.autarkia.config.AutarkiaConfig.IDLE_BRAIN_INTERVAL.get()));
        }

        @Override
        default List<ActivityBuilder<? extends WorkerEntity>> getAdditionalActivities(WorkerEntity entity, Activity[] activities) {
            return List.of(
                ActivityBuilder.<WorkerEntity>create(Activity.WORK).behaviours(new WorkerBehaviours.ExecuteJob(),
                    new FirstApplicableBehaviour<WorkerEntity>(new WorkerBehaviours.FetchFromStorage(), new WorkerBehaviours.TravelToJobSite(),
                        new WorkerBehaviours.PlaceBlockFromJob(), new WorkerBehaviours.BreakPlannedBlock(), new WorkerBehaviours.StringCable(),
                        new WorkerBehavioursT3.ClimbAndString(), new WorkerBehaviours.CrankGenerator(), new WorkerBehaviours.CollectDebris(),
                        new WorkerBehavioursT3.WarehouseTrip())).addMemoryRequirements(WorkerMemories.ASSIGNED_JOB.get()),
                ActivityBuilder.<WorkerEntity>create(Activity.REST).behaviours(new WorkerBehaviours.RechargeAtDock()
                    .cooldownFor(e -> dev.autarkia.config.AutarkiaConfig.IDLE_BRAIN_INTERVAL.get()))
                    .addMemoryRequirements(WorkerMemories.NEEDS_RECHARGE.get()),
                ActivityBuilder.<WorkerEntity>create(Activity.AVOID).behaviours(WorkerType.of(entity.getType()).flying() ? new RiseAway() : new Flee())
                    .addMemoryRequirements(MemoryModuleType.AVOID_TARGET));
        }

        @Override
        default Activity[] getActivityActivationPriority() {
            return new Activity[] {Activity.FIGHT, Activity.AVOID, Activity.REST, Activity.WORK, Activity.IDLE};
        }
    }

    private static void avoidAttacker(WorkerEntity w) {
        w.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY).ifPresent(e -> {
            if (e instanceof Enemy) {
                w.getBrain().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, e, 100);
            }
        });
    }

    /** Drone AVOID: climb 6 blocks and hold there for 60 ticks (06 section 3.5). */
    static final class RiseAway extends ExtendedBehaviour<WorkerEntity> {
        RiseAway() {
            runFor(60);
        }

        @Override
        public Set<MemoryCondition<?, ?>> getMemoryRequirements() {
            return MemoryTest.builder(1).hasMemory(MemoryModuleType.AVOID_TARGET);
        }

        @Override
        protected void start(WorkerEntity w) {
            walkTo(w, w.blockPosition().above(6), 0);
        }
    }

    /** Ground AVOID: run 8 blocks away from the attacker (06 section 3.5). */
    static final class Flee extends ExtendedBehaviour<WorkerEntity> {
        @Override
        public Set<MemoryCondition<?, ?>> getMemoryRequirements() {
            return MemoryTest.builder(1).hasMemory(MemoryModuleType.AVOID_TARGET);
        }

        @Override
        protected void start(WorkerEntity w) {
            LivingEntity from = w.getBrain().getMemory(MemoryModuleType.AVOID_TARGET).orElse(null);
            if (from == null) {
                return;
            }
            Vec3 away = w.position().subtract(from.position()).normalize().scale(8).add(w.position());
            walkTo(w, BlockPos.containing(away), 1);
        }
    }
}

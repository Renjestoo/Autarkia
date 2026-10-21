package dev.autarkia.worker;

import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;

/** The custom worker sensors of 06 section 3.4. */
public final class WorkerSensors {
    private WorkerSensors() {
    }

    /** Validates the pushed work order against the board every 20 ticks and drops it when the board no longer has it. */
    public static final class JobBoardSensor extends ExtendedSensor<WorkerEntity> {
        public JobBoardSensor() {
            scanRate(20);
        }

        @Override
        public SensorType<? extends ExtendedSensor<?>> type() {
            return WorkerMemories.JOB_BOARD.get();
        }

        @Override
        public List<MemoryModuleType<?>> memoriesUsed() {
            return List.of(WorkerMemories.ASSIGNED_JOB.get());
        }

        @Override
        protected void doTick(ServerLevel level, WorkerEntity w) {
            w.validateOrder();
        }
    }

    /** Sets {@code needs_recharge} below the return reserve (06 section 1.2). */
    public static final class EnergySensor extends ExtendedSensor<WorkerEntity> {
        public EnergySensor() {
            scanRate(10);
        }

        @Override
        public SensorType<? extends ExtendedSensor<?>> type() {
            return WorkerMemories.ENERGY.get();
        }

        @Override
        public List<MemoryModuleType<?>> memoriesUsed() {
            return List.of(WorkerMemories.NEEDS_RECHARGE.get());
        }

        @Override
        protected void doTick(ServerLevel level, WorkerEntity w) {
            if (w.energy() < w.reserve() && w.energy() > 0) {
                if (!w.getBrain().hasMemoryValue(WorkerMemories.NEEDS_RECHARGE.get())) {
                    w.getBrain().setMemory(WorkerMemories.NEEDS_RECHARGE.get(), Unit.INSTANCE);
                    w.releaseOrder();
                }
            }
        }
    }

    /** Finds the nearest free winding post in the domain every 100 ticks (06 section 3.4). */
    public static final class ChargerSensor extends ExtendedSensor<WorkerEntity> {
        public ChargerSensor() {
            scanRate(100);
        }

        @Override
        public SensorType<? extends ExtendedSensor<?>> type() {
            return WorkerMemories.CHARGER.get();
        }

        @Override
        public List<MemoryModuleType<?>> memoriesUsed() {
            return List.of(WorkerMemories.CHARGER_POS.get());
        }

        @Override
        protected void doTick(ServerLevel level, WorkerEntity w) {
            Colony c = w.colony();
            if (c == null) {
                w.getBrain().eraseMemory(WorkerMemories.CHARGER_POS.get());
                return;
            }
            BlockPos best = null;
            for (BlockPos p : ColonyRuntime.of(c).chargers) {
                if (level.getBlockEntity(p) instanceof WindingPostBlockEntity post && post.availableFor(w.getUUID())
                    && (best == null || p.distSqr(w.blockPosition()) < best.distSqr(w.blockPosition()))) {
                    best = p;
                }
            }
            if (best == null) {
                w.getBrain().eraseMemory(WorkerMemories.CHARGER_POS.get());
            } else {
                w.getBrain().setMemory(WorkerMemories.CHARGER_POS.get(), GlobalPos.of(level.dimension(), best));
            }
        }
    }

    /** Detects a worker that has a walk target but moved less than 0.5 blocks in 100 ticks (06 section 3.8). */
    public static final class StuckSensor extends ExtendedSensor<WorkerEntity> {
        public StuckSensor() {
            scanRate(20);
        }

        @Override
        public SensorType<? extends ExtendedSensor<?>> type() {
            return WorkerMemories.STUCK.get();
        }

        @Override
        public List<MemoryModuleType<?>> memoriesUsed() {
            return List.of(WorkerMemories.STUCK_LEVEL.get());
        }

        @Override
        protected void doTick(ServerLevel level, WorkerEntity w) {
            w.stuckSample();
        }
    }

    /** Lists the worker-collectable debris entities in the domain every 40 ticks (Hauler Crab). */
    public static final class DebrisSensor extends ExtendedSensor<WorkerEntity> {
        public DebrisSensor() {
            scanRate(40);
        }

        @Override
        public SensorType<? extends ExtendedSensor<?>> type() {
            return WorkerMemories.DEBRIS.get();
        }

        @Override
        public List<MemoryModuleType<?>> memoriesUsed() {
            return List.of(WorkerMemories.DEBRIS_TARGETS.get());
        }

        @Override
        protected void doTick(ServerLevel level, WorkerEntity w) {
            Colony c = w.colony();
            if (c == null) {
                return;
            }
            int h = dev.autarkia.colony.ColonyTiers.domainHalf(c.tier);
            AABB box = new AABB(c.corePos.getX() - h, level.getMinY(), c.corePos.getZ() - h, c.corePos.getX() + h + 1, level.getMaxY(), c.corePos.getZ() + h + 1);
            List<UUID> found = new ArrayList<>();
            for (Entity e : level.getEntities((Entity) null, box, e -> e instanceof WorkerCollectable)) {
                found.add(e.getUUID());
            }
            if (found.isEmpty()) {
                w.getBrain().eraseMemory(WorkerMemories.DEBRIS_TARGETS.get());
            } else {
                found.sort(java.util.Comparator.comparingDouble(id -> {
                    Entity e = level.getEntity(id);
                    return e == null ? Double.MAX_VALUE : e.distanceToSqr(w);
                }));
                w.getBrain().setMemory(WorkerMemories.DEBRIS_TARGETS.get(), found);
            }
        }
    }

    /** Builds the patrol loop through the Core and the domain corners every 200 ticks (Sentinel). */
    public static final class GuardRouteSensor extends ExtendedSensor<WorkerEntity> {
        public GuardRouteSensor() {
            scanRate(200);
        }

        @Override
        public SensorType<? extends ExtendedSensor<?>> type() {
            return WorkerMemories.GUARD_ROUTE_SENSOR.get();
        }

        @Override
        public List<MemoryModuleType<?>> memoriesUsed() {
            return List.of(WorkerMemories.GUARD_ROUTE.get());
        }

        @Override
        protected void doTick(ServerLevel level, WorkerEntity w) {
            Colony c = w.colony();
            if (c == null || !Colonies.inside(c, w.blockPosition())) {
                return;
            }
            int h = dev.autarkia.colony.ColonyTiers.domainHalf(c.tier) - 4;
            BlockPos core = c.corePos;
            List<BlockPos> route = new ArrayList<>();
            route.add(core);
            for (int[] d : new int[][] {{-h, -h}, {h, -h}, {h, h}, {-h, h}}) {
                Vec3 p = Vec3.atCenterOf(core.offset(d[0], 0, d[1]));
                route.add(BlockPos.containing(p.x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (int) p.x, (int) p.z), p.z));
            }
            w.getBrain().setMemory(WorkerMemories.GUARD_ROUTE.get(), route);
        }
    }
}

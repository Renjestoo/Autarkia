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

    /** Finds the nearest free charger of the worker's energy kind in the domain every 100 ticks (06 section 3.4). */
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
                if (level.getBlockEntity(p) instanceof WorkerCharger post && post.serves(w) && post.availableFor(w.getUUID())
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

    /**
     * Return whether an entity is debris for a Hauler Crab: a {@link WorkerCollectable} (snapped cable, falling crate) or
     * an item entity with unlimited lifetime (spilled crate contents, snapped cable drops; 04 section 2.3.5).
     */
    public static boolean isDebris(Entity e) {
        return e.isAlive() && (e instanceof WorkerCollectable && !(e instanceof dev.autarkia.storage.FallingCrateEntity f && f.rail() != null)
            || e instanceof net.minecraft.world.entity.item.ItemEntity ie && ie.getAge() == -32768);
    }

    /** Collect a debris entity: its items, the entity discarded. */
    public static List<net.minecraft.world.item.ItemStack> collect(ServerLevel level, Entity e) {
        if (e instanceof WorkerCollectable wc) {
            return wc.collect(level);
        }
        net.minecraft.world.item.ItemStack st = ((net.minecraft.world.entity.item.ItemEntity) e).getItem().copy();
        e.discard();
        return List.of(st);
    }

    /** Return the debris entities in the colony domain. */
    public static List<Entity> debris(ServerLevel level, Colony c) {
        int h = dev.autarkia.colony.ColonyTiers.domainHalf(c.tier);
        AABB box = new AABB(c.corePos.getX() - h, level.getMinY(), c.corePos.getZ() - h, c.corePos.getX() + h + 1, level.getMaxY(), c.corePos.getZ() + h + 1);
        return level.getEntities((Entity) null, box, WorkerSensors::isDebris);
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
            List<UUID> found = new ArrayList<>();
            for (Entity e : debris(level, c)) {
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

    /**
     * Builds the patrol loop every 200 ticks (Sentinel, 06 section 3.4): the {@code colony_core}, then each
     * {@code domain_beacon}, then each {@code outpost_core}, at the top of each column.
     */
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
            List<BlockPos> stops = new ArrayList<>();
            stops.add(c.corePos);
            c.beacons.forEach(b -> stops.add(b.pos()));
            c.outposts.forEach(o -> stops.add(o.corePos));
            List<BlockPos> route = new ArrayList<>();
            for (BlockPos p : stops) {
                route.add(new BlockPos(p.getX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ()),
                    p.getZ()));
            }
            w.getBrain().setMemory(WorkerMemories.GUARD_ROUTE.get(), route);
        }
    }
}

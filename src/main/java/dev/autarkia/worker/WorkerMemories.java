package dev.autarkia.worker;

import com.mojang.serialization.Codec;
import dev.autarkia.Autarkia;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Worker memory module types and sensor types (06 sections 3.3 and 3.4, 08b section 9). */
public final class WorkerMemories {
    /** Job phases of the {@code job_phase} memory. */
    public enum Phase implements StringRepresentable {
        FETCH, TRAVEL, WORK, DEPOSIT, RETURN;

        public static final Codec<Phase> CODEC = StringRepresentable.fromEnum(Phase::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final DeferredRegister<MemoryModuleType<?>> MEMORIES = DeferredRegister.create(Registries.MEMORY_MODULE_TYPE, Autarkia.MOD_ID);
    public static final DeferredRegister<SensorType<?>> SENSORS = DeferredRegister.create(Registries.SENSOR_TYPE, Autarkia.MOD_ID);

    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<UUID>> ASSIGNED_JOB = MEMORIES.register("assigned_job",
        () -> new MemoryModuleType<>(Optional.of(UUIDUtil.CODEC)));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Phase>> JOB_PHASE = MEMORIES.register("job_phase",
        () -> new MemoryModuleType<>(Optional.of(Phase.CODEC)));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Integer>> JOB_STEP = MEMORIES.register("job_step",
        () -> new MemoryModuleType<>(Optional.of(Codec.INT)));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<GlobalPos>> FETCH_SOURCE = MEMORIES.register("fetch_source",
        () -> new MemoryModuleType<>(Optional.empty()));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<GlobalPos>> CHARGER_POS = MEMORIES.register("charger_pos",
        () -> new MemoryModuleType<>(Optional.of(GlobalPos.CODEC)));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Unit>> NEEDS_RECHARGE = MEMORIES.register("needs_recharge",
        () -> new MemoryModuleType<>(Optional.empty()));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<Integer>> STUCK_LEVEL = MEMORIES.register("stuck_level",
        () -> new MemoryModuleType<>(Optional.empty()));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<List<UUID>>> DEBRIS_TARGETS = MEMORIES.register("debris_targets",
        () -> new MemoryModuleType<>(Optional.empty()));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<List<BlockPos>>> GUARD_ROUTE = MEMORIES.register("guard_route",
        () -> new MemoryModuleType<>(Optional.empty()));

    public static final DeferredHolder<SensorType<?>, SensorType<WorkerSensors.JobBoardSensor>> JOB_BOARD = SENSORS.register("job_board",
        () -> new SensorType<>(WorkerSensors.JobBoardSensor::new));
    public static final DeferredHolder<SensorType<?>, SensorType<WorkerSensors.EnergySensor>> ENERGY = SENSORS.register("energy",
        () -> new SensorType<>(WorkerSensors.EnergySensor::new));
    public static final DeferredHolder<SensorType<?>, SensorType<WorkerSensors.ChargerSensor>> CHARGER = SENSORS.register("charger",
        () -> new SensorType<>(WorkerSensors.ChargerSensor::new));
    public static final DeferredHolder<SensorType<?>, SensorType<WorkerSensors.StuckSensor>> STUCK = SENSORS.register("stuck",
        () -> new SensorType<>(WorkerSensors.StuckSensor::new));
    public static final DeferredHolder<SensorType<?>, SensorType<WorkerSensors.DebrisSensor>> DEBRIS = SENSORS.register("debris",
        () -> new SensorType<>(WorkerSensors.DebrisSensor::new));
    public static final DeferredHolder<SensorType<?>, SensorType<WorkerSensors.GuardRouteSensor>> GUARD_ROUTE_SENSOR = SENSORS.register("guard_route",
        () -> new SensorType<>(WorkerSensors.GuardRouteSensor::new));

    private WorkerMemories() {
    }

    /** Register both deferred registers on the mod bus. */
    public static void register(IEventBus modBus) {
        MEMORIES.register(modBus);
        SENSORS.register(modBus);
    }
}

package dev.autarkia.registry;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.RollingFlywheelEntity;
import dev.autarkia.energy.SnappedCableEntity;
import dev.autarkia.equipment.SlingShotEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity types of 08b section 1. */
public final class AutarkiaEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Autarkia.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SnappedCableEntity>> SNAPPED_CABLE = ENTITIES.registerEntityType("snapped_cable",
        SnappedCableEntity::new, MobCategory.MISC, b -> b.sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<RollingFlywheelEntity>> ROLLING_FLYWHEEL = ENTITIES.registerEntityType("rolling_flywheel",
        RollingFlywheelEntity::new, MobCategory.MISC, b -> b.sized(2.9f, 2.9f).clientTrackingRange(10).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<SlingShotEntity>> SLING_SHOT = ENTITIES.registerEntityType("sling_shot",
        SlingShotEntity::new, MobCategory.MISC, b -> b.sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10));

    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.worker.WorkerEntity>> PORTER = ENTITIES.register("porter", id -> {
        EntityType<dev.autarkia.worker.WorkerEntity> t = EntityType.Builder.<dev.autarkia.worker.WorkerEntity>of(dev.autarkia.worker.WorkerEntity::new,
            MobCategory.MISC).sized(0.6f, 1.3f).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.ENTITY_TYPE, id));
        dev.autarkia.worker.WorkerType.bind(t, dev.autarkia.worker.WorkerType.PORTER);
        return t;
    });

    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.worker.WorkerEntity>> HAULER_CRAB = ENTITIES.register("hauler_crab", id -> {
        EntityType<dev.autarkia.worker.WorkerEntity> t = EntityType.Builder.<dev.autarkia.worker.WorkerEntity>of(dev.autarkia.worker.WorkerEntity::new,
            MobCategory.MISC).sized(1.1f, 0.7f).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.ENTITY_TYPE, id));
        dev.autarkia.worker.WorkerType.bind(t, dev.autarkia.worker.WorkerType.HAULER_CRAB);
        return t;
    });
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.worker.WorkerEntity>> BUILDER_DRONE = ENTITIES.register("builder_drone",
        id -> worker(id, dev.autarkia.worker.DroneWorkerEntity::new, 0.8f, 0.5f, dev.autarkia.worker.WorkerType.BUILDER_DRONE));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.worker.WorkerEntity>> LOADER = ENTITIES.register("loader",
        id -> worker(id, dev.autarkia.worker.WorkerEntity::new, 1.3f, 1.4f, dev.autarkia.worker.WorkerType.LOADER));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.worker.WorkerEntity>> LINEMAN = ENTITIES.register("lineman",
        id -> worker(id, dev.autarkia.worker.WorkerEntity::new, 0.6f, 1.6f, dev.autarkia.worker.WorkerType.LINEMAN));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.worker.WorkerEntity>> SURVEYOR = ENTITIES.register("surveyor",
        id -> worker(id, dev.autarkia.worker.DroneWorkerEntity::new, 0.6f, 0.45f, dev.autarkia.worker.WorkerType.SURVEYOR));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.worker.WorkerEntity>> CABLE_LAYER_DRONE = ENTITIES.register("cable_layer_drone",
        id -> worker(id, dev.autarkia.worker.DroneWorkerEntity::new, 1.2f, 0.9f, dev.autarkia.worker.WorkerType.CABLE_LAYER));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.worker.WorkerEntity>> SENTINEL = ENTITIES.register("sentinel",
        id -> worker(id, dev.autarkia.worker.DroneWorkerEntity::new, 0.9f, 0.7f, dev.autarkia.worker.WorkerType.SENTINEL));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.worker.WorkerEntity>> REPLICATOR_WALKER = ENTITIES.register("replicator_walker",
        id -> worker(id, dev.autarkia.worker.WorkerEntity::new, 2.0f, 3.3f, dev.autarkia.worker.WorkerType.REPLICATOR));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.colony.RisingBlockEntity>> RISING_BLOCK = ENTITIES.registerEntityType(
        "rising_block", dev.autarkia.colony.RisingBlockEntity::new, MobCategory.MISC, b -> b.sized(0.98f, 0.98f).clientTrackingRange(10).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.colony.ThrownAutarkySeed>> THROWN_AUTARKY_SEED = ENTITIES.registerEntityType(
        "thrown_autarky_seed", dev.autarkia.colony.ThrownAutarkySeed::new, MobCategory.MISC, b -> b.sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.storage.FallingCrateEntity>> FALLING_CRATE = ENTITIES.registerEntityType(
        "falling_crate", dev.autarkia.storage.FallingCrateEntity::new, MobCategory.MISC, b -> b.sized(0.875f, 0.875f).clientTrackingRange(10).updateInterval(2));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.logistics.GondolaEntity>> GONDOLA = ENTITIES.registerEntityType("gondola",
        dev.autarkia.logistics.GondolaEntity::new, MobCategory.MISC, b -> b.sized(2.0f, 3.4f).clientTrackingRange(10).updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.equipment.GrappleHookEntity>> GRAPPLE_HOOK = ENTITIES.registerEntityType(
        "grapple_hook", dev.autarkia.equipment.GrappleHookEntity::new, MobCategory.MISC,
        b -> b.sized(0.25f, 0.25f).clientTrackingRange(6).updateInterval(1).noSave());
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.worker.NaniteSwarmEntity>> NANITE_SWARM = ENTITIES.register("nanite_swarm",
        id -> {
            EntityType<dev.autarkia.worker.NaniteSwarmEntity> t = EntityType.Builder.<dev.autarkia.worker.NaniteSwarmEntity>of(
                dev.autarkia.worker.NaniteSwarmEntity::new, MobCategory.MISC).sized(3.0f, 3.0f).fireImmune().clientTrackingRange(8).updateInterval(2)
                .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, id));
            dev.autarkia.worker.WorkerType.bind(t, dev.autarkia.worker.WorkerType.NANITE_SWARM);
            return t;
        });

    private AutarkiaEntities() {
    }

    private static EntityType<dev.autarkia.worker.WorkerEntity> worker(net.minecraft.resources.Identifier id,
                                                                        EntityType.EntityFactory<dev.autarkia.worker.WorkerEntity> factory, float w, float h,
                                                                        dev.autarkia.worker.WorkerType type) {
        EntityType<dev.autarkia.worker.WorkerEntity> t = EntityType.Builder.of(factory, MobCategory.MISC).sized(w, h).clientTrackingRange(10)
            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, id));
        dev.autarkia.worker.WorkerType.bind(t, type);
        return t;
    }

    /** Register the worker attributes (mod bus). */
    public static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) {
        e.put(PORTER.get(), dev.autarkia.worker.WorkerEntity.attributes(dev.autarkia.worker.WorkerType.PORTER).build());
        e.put(HAULER_CRAB.get(), dev.autarkia.worker.WorkerEntity.attributes(dev.autarkia.worker.WorkerType.HAULER_CRAB).build());
        e.put(BUILDER_DRONE.get(), dev.autarkia.worker.DroneWorkerEntity.droneAttributes(dev.autarkia.worker.WorkerType.BUILDER_DRONE).build());
        e.put(LOADER.get(), dev.autarkia.worker.WorkerEntity.attributes(dev.autarkia.worker.WorkerType.LOADER).build());
        e.put(LINEMAN.get(), dev.autarkia.worker.WorkerEntity.attributes(dev.autarkia.worker.WorkerType.LINEMAN).build());
        e.put(SURVEYOR.get(), dev.autarkia.worker.DroneWorkerEntity.droneAttributes(dev.autarkia.worker.WorkerType.SURVEYOR).build());
        e.put(CABLE_LAYER_DRONE.get(), dev.autarkia.worker.DroneWorkerEntity.droneAttributes(dev.autarkia.worker.WorkerType.CABLE_LAYER).build());
        e.put(SENTINEL.get(), dev.autarkia.worker.DroneWorkerEntity.droneAttributes(dev.autarkia.worker.WorkerType.SENTINEL).build());
        e.put(REPLICATOR_WALKER.get(), dev.autarkia.worker.WorkerEntity.attributes(dev.autarkia.worker.WorkerType.REPLICATOR).build());
    }
}

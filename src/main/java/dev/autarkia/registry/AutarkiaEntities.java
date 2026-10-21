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
    public static final DeferredHolder<EntityType<?>, EntityType<dev.autarkia.storage.FallingCrateEntity>> FALLING_CRATE = ENTITIES.registerEntityType(
        "falling_crate", dev.autarkia.storage.FallingCrateEntity::new, MobCategory.MISC, b -> b.sized(0.875f, 0.875f).clientTrackingRange(10).updateInterval(2));

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
    }
}

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

    private AutarkiaEntities() {
    }
}

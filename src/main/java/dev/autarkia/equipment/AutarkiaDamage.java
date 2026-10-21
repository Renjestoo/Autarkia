package dev.autarkia.equipment;

import dev.autarkia.Autarkia;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/** Damage type keys of 08b section 15 (the types are datapack JSON under data/autarkia/damage_type). */
public final class AutarkiaDamage {
    public static final ResourceKey<DamageType> SLING = ResourceKey.create(Registries.DAMAGE_TYPE, Autarkia.id("sling"));
    public static final ResourceKey<DamageType> SHOCK = ResourceKey.create(Registries.DAMAGE_TYPE, Autarkia.id("shock"));
    public static final ResourceKey<DamageType> ARC = ResourceKey.create(Registries.DAMAGE_TYPE, Autarkia.id("arc"));
    public static final ResourceKey<DamageType> RAIL = ResourceKey.create(Registries.DAMAGE_TYPE, Autarkia.id("rail"));
    public static final ResourceKey<DamageType> BEAM = ResourceKey.create(Registries.DAMAGE_TYPE, Autarkia.id("beam"));

    private AutarkiaDamage() {
    }
}

package dev.autarkia.registry;

import dev.autarkia.Autarkia;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

/** Colony and worker tags (08b section 14): planner protection and the never-grief rules of 06 section 3.10. */
public final class AutarkiaTags {
    public static final TagKey<Block> PLANNER_PROTECTED = TagKey.create(Registries.BLOCK, Autarkia.id("planner_protected"));
    public static final TagKey<Block> WORKER_REPLACEABLE = TagKey.create(Registries.BLOCK, Autarkia.id("worker_replaceable"));
    public static final TagKey<Block> WORKER_UNBREAKABLE = TagKey.create(Registries.BLOCK, Autarkia.id("worker_unbreakable"));
    public static final TagKey<EntityType<?>> WORKERS = TagKey.create(Registries.ENTITY_TYPE, Autarkia.id("workers"));

    public static final TagKey<Block> NANITE_IMMUNE = TagKey.create(Registries.BLOCK, Autarkia.id("nanite_immune"));

    private AutarkiaTags() {
    }
}

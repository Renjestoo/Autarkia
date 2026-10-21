package dev.autarkia.worker;

import com.klikli_dev.magicparticleslib.premade.particle.electricarc.ElectricArcParticleOptions;
import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.equipment.AutarkiaDamage;
import dev.autarkia.registry.EventTrigger;
import java.util.Comparator;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryCondition;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.core.behaviour.base.ExtendedBehaviour;
import net.tslat.smartbrainlib.api.core.sensor.custom.GenericAttackTargetSensor;
import net.tslat.smartbrainlib.library.object.MemoryTest;
import org.jspecify.annotations.Nullable;

/**
 * The Sentinel's guard duty (06 sections 3.4 and 3.6): the target rule, the target sensor and the {@code ArcGunAttack}
 * behaviour. A target is an {@link Enemy} inside the colony domain plus 8 blocks, or, while escorting, within 12 blocks
 * of the escorted entity; never a player, a tamed animal, a villager, a worker or an entity of the
 * {@code autarkia:sentinel_never_target} tag. Creepers within 10 blocks of a domain block come first, then the nearest.
 */
public final class SentinelCombat {
    /** Entity types the Sentinel never targets (08b section 16). */
    public static final TagKey<EntityType<?>> NEVER_TARGET = TagKey.create(Registries.ENTITY_TYPE, Autarkia.id("sentinel_never_target"));
    public static final int SHOT_INTERVAL = 30;
    public static final int RANGE = 16;
    public static final int ARC_COLOR = 0xFF66CCFF;

    private SentinelCombat() {
    }

    /** Return whether the Sentinel never targets the entity, wherever it is. */
    public static boolean never(Entity e) {
        return e instanceof Player || e instanceof AbstractVillager || e instanceof WorkerEntity || e instanceof TamableAnimal t && t.isTame()
            || e.typeHolder().is(NEVER_TARGET);
    }

    /** Return whether the Sentinel may strike back at an attacker. */
    public static boolean retaliable(LivingEntity e) {
        return e instanceof Enemy && !never(e);
    }

    /** Return whether the entity is a valid target of the Sentinel now (alive, hostile, in its guard area). */
    public static boolean canTarget(WorkerEntity w, LivingEntity e) {
        if (!e.isAlive() || !retaliable(e)) {
            return false;
        }
        Colony c = w.colony();
        if (c != null && c.dimension.equals(e.level().dimension()) && nearDomain(c, e.blockPosition(), 8)) {
            return true;
        }
        Entity escorted = escorted(w);
        return escorted != null && escorted.distanceToSqr(e) <= 12 * 12;
    }

    /** Return the entity the Sentinel escorts on its current ESCORT job, or null. */
    public static @Nullable Entity escorted(WorkerEntity w) {
        ColonyJob job = w.currentJob();
        if (job == null || job.type != ColonyJob.Type.ESCORT || job.subject == null || !(w.level() instanceof ServerLevel sl)) {
            return null;
        }
        Entity e = sl.getEntity(job.subject);
        return e != null && e.isAlive() ? e : null;
    }

    private static boolean nearDomain(Colony c, BlockPos p, int margin) {
        for (int[] s : Colonies.squares(c)) {
            if (Math.abs(p.getX() - s[0]) <= s[2] + margin && Math.abs(p.getZ() - s[1]) <= s[2] + margin) {
                return true;
            }
        }
        return false;
    }

    private static int priority(WorkerEntity w, LivingEntity e) {
        Colony c = w.colony();
        return e instanceof Creeper && c != null && nearDomain(c, e.blockPosition(), 10) ? 0 : 1;
    }

    /** Return the attack range: 16 blocks plus 4 per range upgrade. */
    public static double range(WorkerEntity w) {
        return RANGE + 4.0 * w.upgrades.count(WorkerUpgrades.Kind.RANGE);
    }

    /** Return whether the Sentinel can afford a shot and is not on its way to a charger. */
    public static boolean armed(WorkerEntity w) {
        return w.energy() >= WorkerType.ARC_SHOT_COST && !w.getBrain().hasMemoryValue(WorkerMemories.NEEDS_RECHARGE.get());
    }

    /** Return whether the current attack target stays valid (InvalidateAttackTarget: dead, invalid or no energy ends it). */
    public static boolean keep(WorkerEntity w, LivingEntity target) {
        return armed(w) && canTarget(w, target);
    }

    /**
     * The SBL generic attack target sensor (rate 10) with the Sentinel's rule and order: from the visible living
     * entities it picks creepers near the domain first, then the nearest valid target, into {@code nearest_attackable}.
     */
    public static final class TargetSensor extends GenericAttackTargetSensor<WorkerEntity> {
        public TargetSensor() {
            scanRate(10);
        }

        @Override
        protected @Nullable LivingEntity findMatches(WorkerEntity w, NearestVisibleLivingEntities visible) {
            if (!armed(w)) {
                return null;
            }
            return visible.find(e -> canTarget(w, e)).min(Comparator.comparingInt((LivingEntity e) -> priority(w, e))
                .thenComparingDouble(e -> e.distanceToSqr(w))).orElse(null);
        }
    }

    /**
     * ArcGunAttack (06 section 3.6): while the target is within range and in line of sight, one arc every 30 ticks for
     * 1,500 FE, {@code sentinelArcDamage} of damage type {@code autarkia:arc}, trigger {@code shoot}, sound
     * {@code entity.sentinel.arc_shot} and an arc particle to the target. A kill fires the {@code sentinel_kill} worker
     * event for players within 32 blocks.
     */
    public static final class ArcGunAttack extends ExtendedBehaviour<WorkerEntity> {
        private int cooldown;

        public ArcGunAttack() {
            noTimeout();
        }

        @Override
        public Set<MemoryCondition<?, ?>> getMemoryRequirements() {
            return MemoryTest.builder(1).hasMemory(MemoryModuleType.ATTACK_TARGET);
        }

        @Override
        protected boolean shouldKeepRunning(WorkerEntity w) {
            return w.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).filter(LivingEntity::isAlive).isPresent();
        }

        @Override
        protected void tick(WorkerEntity w) {
            LivingEntity target = w.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
            if (target == null) {
                return;
            }
            w.getLookControl().setLookAt(target);
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            if (armed(w) && w.distanceTo(target) <= range(w) && w.hasLineOfSight(target)) {
                shoot(w, target);
                cooldown = SHOT_INTERVAL - 1;
            }
        }
    }

    /** Fire one arc at the target (cost, damage, effects and the kill event). */
    public static void shoot(WorkerEntity w, LivingEntity target) {
        ServerLevel sl = (ServerLevel) w.level();
        w.spend(WorkerType.ARC_SHOT_COST);
        w.trigger("shoot");
        w.sound("entity.sentinel.arc_shot", 1.0f, 1.0f);
        Vec3 from = w.position().add(0, w.getBbHeight() * 0.5, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        sl.sendParticles(new ElectricArcParticleOptions(to, ARC_COLOR, 1.0F, w.getRandom().nextInt(), 3), from.x, from.y, from.z, 1, 0, 0, 0, 0);
        target.hurtServer(sl, sl.damageSources().source(AutarkiaDamage.ARC, w), AutarkiaConfig.SENTINEL_ARC_DAMAGE.get().floatValue());
        if (!target.isAlive()) {
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(w) <= 32 * 32) {
                    EventTrigger.WORKER.get().fire(p, "sentinel_kill");
                }
            }
            Colony c = w.colony();
            if (c != null) {
                c.stat("sentinel_kills", 1);
            }
        }
    }
}

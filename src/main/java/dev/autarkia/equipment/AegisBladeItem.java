package dev.autarkia.equipment;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jspecify.annotations.Nullable;

/**
 * {@code aegis_blade} (07 section 1.7): 30.0 damage at 2.0 attacks per second, unbreakable, 100,000,000 FE. A melee hit
 * with at least {@link #HIT_COST} FE spends it and multiplies the armor reduction of the hit by {@link #PIERCE}
 * (through the NeoForge damage container, as the lattice blade). Use: the Aegis sweep, {@link #SWEEP_DAMAGE} player
 * attack damage to every {@link Enemy} mob whose body center lies within a sphere of {@link #SWEEP_RADIUS} blocks around
 * the user's body center (decision G13; other mobs, players and villagers are never hit), for {@link #SWEEP_COST} FE,
 * then {@link #SWEEP_COOLDOWN} ticks of cooldown. Charges from the tether at 50,000 FE/t ({@link AegisTether}).
 */
public class AegisBladeItem extends PoweredItem {
    public static final int CAPACITY = 100000000;
    public static final int INPUT = AegisTether.CHARGE;
    public static final int HIT_COST = 500;
    public static final float PIERCE = 0.25f;
    public static final double SWEEP_RADIUS = 6.0;
    public static final float SWEEP_DAMAGE = 20f;
    public static final int SWEEP_COST = 50000;
    public static final int SWEEP_COOLDOWN = 100;

    public AegisBladeItem(Properties props) {
        super(props, CAPACITY, INPUT, 0);
    }

    /** Register the armor piercing listener (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(AegisBladeItem::onIncoming);
    }

    /** Return true when the source is a direct melee hit with an Aegis blade that can pay for the hit. */
    public static boolean piercing(DamageSource source) {
        ItemStack weapon = source.getWeaponItem();
        return weapon != null && source.isDirect() && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK))
            && weapon.getItem() instanceof AegisBladeItem && energy(weapon) >= HIT_COST;
    }

    private static void onIncoming(LivingIncomingDamageEvent e) {
        if (piercing(e.getSource())) {
            e.getContainer().addModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * PIERCE);
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide()) {
            spend(stack, HIT_COST);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack) || energy(stack) < SWEEP_COST) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel sl) {
            sweep(sl, player, stack);
        }
        return InteractionResult.SUCCESS;
    }

    /** Run the Aegis sweep now (FE, cooldown, damage, particles, sound) and return the mobs it hurt. */
    public static List<LivingEntity> sweep(ServerLevel sl, Player player, ItemStack stack) {
        if (!spend(stack, SWEEP_COST)) {
            return List.of();
        }
        player.getCooldowns().addCooldown(stack, SWEEP_COOLDOWN);
        Vec3 c = player.getBoundingBox().getCenter();
        List<LivingEntity> hit = sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(SWEEP_RADIUS),
            e -> e instanceof Enemy && e.isAlive() && e.getBoundingBox().getCenter().distanceToSqr(c) <= SWEEP_RADIUS * SWEEP_RADIUS);
        DamageSource source = sl.damageSources().playerAttack(player);
        for (LivingEntity e : hit) {
            e.hurtServer(sl, source, SWEEP_DAMAGE);
        }
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x + Math.cos(a) * SWEEP_RADIUS * 0.5, c.y, c.z + Math.sin(a) * SWEEP_RADIUS * 0.5, 1, 0, 0, 0, 0);
        }
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.6f);
        return hit;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        AegisTether.charge(level, entity, stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }
}

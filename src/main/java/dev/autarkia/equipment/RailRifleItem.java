package dev.autarkia.equipment;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.BeamVisualPayload;
import dev.autarkia.registry.AutarkiaSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * {@code rail_rifle} (07 section 1.5): 3,000,000 FE charged at 5,000 FE/t. Hold use 10 to 30 ticks, then release: a
 * shot costs 20,000 FE and deals 8 + 0.8 x (charge - 10) {@code autarkia:rail} damage (8 to 24) to the first 5 living
 * entities along a 96-block hitscan, which stops at the first solid block and never changes blocks. Each hit entity is
 * knocked back 1.5 along the shot. Cooldown 40 ticks. The trail is a {@code beam_visual} of kind
 * {@link BeamVisualPayload#KIND_RAIL} that the client turns into a glow trail.
 */
public class RailRifleItem extends PoweredGeoItem {
    public static final int CAPACITY = 3000000;
    public static final int INPUT = 5000;
    public static final int SHOT_COST = 20000;
    public static final int MIN_CHARGE = 10;
    public static final int FULL_CHARGE = 30;
    public static final double RANGE = 96;
    public static final int PIERCE = 5;
    public static final double KNOCKBACK = 1.5;
    public static final int COOLDOWN = 40;

    public RailRifleItem(Properties props) {
        super(props, CAPACITY, INPUT, 0, "rail_rifle", "fire");
    }

    /** Return the shot damage for a charge in ticks (0 below the minimum charge). */
    public static float damage(int charge) {
        return charge < MIN_CHARGE ? 0f : 8f + 0.8f * (Math.min(charge, FULL_CHARGE) - MIN_CHARGE);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (energy(stack) < SHOT_COST || player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel sl) {
            triggerAnim(player, GeoItem.getOrAssignId(stack, sl), "main", "charge");
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), AutarkiaSounds.of("item.rail_rifle.charge"), SoundSource.PLAYERS, 0.8f, 1.8f);
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        int charge = getUseDuration(stack, user) - remaining;
        float dmg = damage(charge);
        if (!(level instanceof ServerLevel sl)) {
            return dmg > 0;
        }
        if (dmg <= 0 || !spend(stack, SHOT_COST)) {
            stopTriggeredAnim(user, GeoItem.getOrAssignId(stack, sl), "main", "charge");
            return false;
        }
        fire(sl, user, dmg);
        playWork(user, stack);
        sl.playSound(null, user.getX(), user.getY(), user.getZ(), AutarkiaSounds.of("item.rail_rifle.fire"), SoundSource.PLAYERS, 0.4f, 2.0f);
        if (user instanceof Player p) {
            p.getCooldowns().addCooldown(stack, COOLDOWN);
        }
        return true;
    }

    /** Fire one slug from the shooter's eyes with the given damage and return the entities hit in order. */
    public static List<LivingEntity> fire(ServerLevel sl, LivingEntity shooter, float dmg) {
        Vec3 eye = shooter.getEyePosition();
        Vec3 dir = shooter.getLookAngle();
        Vec3 stop = sl.clip(new ClipContext(eye, eye.add(dir.scale(RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter)).getLocation();
        List<LivingEntity> hit = targets(sl, shooter, eye, stop);
        DamageSource source = sl.damageSources().source(AutarkiaDamage.RAIL, shooter);
        for (LivingEntity e : hit) {
            e.setInvulnerableTime(0);
            e.hurtServer(sl, source, dmg);
            e.knockback(KNOCKBACK, -dir.x, -dir.z, source, dmg);
        }
        Vec3 muzzle = eye.add(0, -0.2, 0);
        AutarkiaNetwork.toNear(sl, muzzle, 128, new BeamVisualPayload(shooter.getId(), muzzle, stop, 10, 0xFF5FB8B0, BeamVisualPayload.KIND_RAIL, 0.2f));
        return hit;
    }

    private static List<LivingEntity> targets(ServerLevel sl, LivingEntity shooter, Vec3 eye, Vec3 stop) {
        record Hit(LivingEntity entity, double distance) {
        }
        List<Hit> hits = new ArrayList<>();
        for (Entity e : sl.getEntities(shooter, new AABB(eye, stop).inflate(1.0), c -> c instanceof LivingEntity le && le.isAlive() && !c.isSpectator())) {
            Optional<Vec3> at = e.getBoundingBox().inflate(0.3).clip(eye, stop);
            at.ifPresent(p -> hits.add(new Hit((LivingEntity) e, p.distanceToSqr(eye))));
        }
        hits.sort(Comparator.comparingDouble(Hit::distance));
        return hits.stream().limit(PIERCE).map(Hit::entity).toList();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        var main = new AnimationController<RailRifleItem>("main", 2, test -> test.setAndContinue(RawAnimation.begin().thenLoop("animation.rail_rifle.idle")));
        main.triggerableAnim("charge", RawAnimation.begin().thenPlayAndHold("animation.rail_rifle.charge"));
        main.triggerableAnim("fire", RawAnimation.begin().thenPlay("animation.rail_rifle.fire"));
        controllers.add(main);
    }
}

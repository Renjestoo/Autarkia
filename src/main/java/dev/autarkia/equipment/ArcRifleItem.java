package dev.autarkia.equipment;

import com.klikli_dev.magicparticleslib.premade.particle.electricarc.ElectricArcParticleOptions;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.EventTrigger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * {@code arc_rifle} (07 section 1.4): 1,000,000 FE, charged at 2,000 FE/t. A shot costs 5,000 FE: hitscan 32 blocks,
 * the first target takes 10 {@code autarkia:arc} damage, then the arc chains 3 hops within 6 blocks at x0.7 per hop
 * (7, 4.9, 3.43). It skips the shooter, the shooter's pets, villagers, colony workers, and players while PvP is off.
 * Cooldown 20 ticks (use cooldown component). Four targets hit fire the {@code arc_chain_4} equipment event.
 */
public class ArcRifleItem extends PoweredGeoItem {
    public static final int SHOT_COST = 5000;
    public static final double RANGE = 32;
    public static final double HOP_RADIUS = 6;
    public static final int HOPS = 3;

    public ArcRifleItem(Properties props) {
        super(props, 1000000, 2000, 0, "arc_rifle", "fire");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) {
            return energy(stack) >= SHOT_COST ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (!spend(stack, SHOT_COST)) {
            return InteractionResult.FAIL;
        }
        List<LivingEntity> hit = fire(sl, player);
        playWork(player, stack);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(), AutarkiaSounds.of("item.arc_rifle.fire"), SoundSource.PLAYERS, 0.4f, 2.0f);
        if (hit.size() >= 4 && player instanceof ServerPlayer sp) {
            EventTrigger.EQUIPMENT.get().fire(sp, "arc_chain_4");
        }
        return InteractionResult.SUCCESS;
    }

    /** Fire one shot from the shooter's eyes and return the entities hit in order. */
    public static List<LivingEntity> fire(ServerLevel sl, LivingEntity shooter) {
        Vec3 eye = shooter.getEyePosition();
        Vec3 end = eye.add(shooter.getLookAngle().scale(RANGE));
        var block = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        Vec3 stop = block.getLocation();
        EntityHitResult first = ProjectileUtil.getEntityHitResult(sl, shooter, eye, stop, new AABB(eye, stop).inflate(1.0),
            e -> valid(sl, shooter, e), 0.3f);
        List<LivingEntity> hit = new ArrayList<>();
        Vec3 from = eye.add(0, -0.2, 0);
        if (first == null || !(first.getEntity() instanceof LivingEntity target)) {
            arc(sl, from, stop);
            return hit;
        }
        float damage = 10f;
        LivingEntity cur = target;
        for (int hop = 0; hop <= HOPS && cur != null; hop++) {
            Vec3 to = cur.position().add(0, cur.getBbHeight() / 2, 0);
            arc(sl, from, to);
            cur.setInvulnerableTime(0);
            cur.hurtServer(sl, sl.damageSources().source(AutarkiaDamage.ARC, shooter), damage);
            hit.add(cur);
            from = to;
            damage *= 0.7f;
            cur = next(sl, shooter, cur, hit);
        }
        return hit;
    }

    private static @Nullable LivingEntity next(ServerLevel sl, LivingEntity shooter, LivingEntity from, List<LivingEntity> done) {
        return sl.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(HOP_RADIUS),
                e -> !done.contains(e) && valid(sl, shooter, e) && e.distanceTo(from) <= HOP_RADIUS).stream()
            .min(Comparator.comparingDouble((LivingEntity e) -> e.distanceToSqr(from)).thenComparing(Entity::getUUID)).orElse(null);
    }

    /** Return true when the arc may hit the entity. */
    public static boolean valid(ServerLevel sl, LivingEntity shooter, Entity e) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e == shooter || e instanceof AbstractVillager
            || e instanceof dev.autarkia.worker.WorkerEntity) {
            return false;
        }
        if (e instanceof OwnableEntity o && o.getOwner() == shooter) {
            return false;
        }
        return !(e instanceof Player) || sl.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.PVP);
    }

    private static void arc(ServerLevel sl, Vec3 from, Vec3 to) {
        sl.sendParticles(new ElectricArcParticleOptions(to, 0xFF8FD3FF, 0.12F, sl.getRandom().nextInt(), 6), from.x, from.y, from.z, 1, 0, 0, 0, 0);
    }
}

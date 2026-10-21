package dev.autarkia.equipment;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * {@code vantite_blade} (07 section 1.5): a vantite sword whose use dashes 6 blocks along the horizontal look direction
 * (3 ticks of 2 blocks, stopped by walls), costs 12 durability and starts an 80-tick cooldown. Within 10 ticks of the
 * use, the first melee hit on a hostile mob ({@link Enemy}) deals +4 damage. The dash moves the player on the side that
 * simulates it: the local client for a real player; {@link #step(Player)} is the per-tick move. Dash state is kept per
 * side, so the client and server threads never share it.
 */
public class DashBladeItem extends Item {
    public static final int DASH_TICKS = 3;
    public static final double STEP = 2.0;
    public static final int COOLDOWN = 80;
    public static final int DURABILITY_COST = 12;
    public static final float BONUS = 4f;
    public static final int HIT_WINDOW = 10;
    private static final Map<Player, Dash> CLIENT = new WeakHashMap<>();
    private static final Map<Player, Dash> SERVER = new WeakHashMap<>();

    private static final class Dash {
        final Vec3 step;
        final long until;
        int left = DASH_TICKS;
        boolean struck;

        Dash(Vec3 step, long until) {
            this.step = step;
            this.until = until;
        }
    }

    public DashBladeItem(Properties props) {
        super(props);
    }

    /** Register the per-tick dash move of the local player (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Pre e) -> {
            if (e.getEntity().isLocalInstanceAuthoritative()) {
                step(e.getEntity());
            }
        });
    }

    private static Map<Player, Dash> dashes(Level level) {
        return level.isClientSide() ? CLIENT : SERVER;
    }

    /** Move the player one dash step if a dash is running; a wall ends the dash. */
    public static void step(Player player) {
        Dash d = dashes(player.level()).get(player);
        if (d == null || d.left <= 0) {
            return;
        }
        player.move(MoverType.SELF, d.step);
        player.resetFallDistance();
        d.left = player.horizontalCollision ? 0 : d.left - 1;
    }

    /** Return true while the player's dash window for the bonus hit is open and unused. */
    public static boolean bonusReady(Player player) {
        Dash d = dashes(player.level()).get(player);
        return d != null && !d.struck && player.level().getGameTime() <= d.until;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (player.getCooldowns().isOnCooldown(stack) || flat.lengthSqr() < 1.0E-6) {
            return InteractionResult.FAIL;
        }
        dashes(level).put(player, new Dash(flat.normalize().scale(STEP), level.getGameTime() + HIT_WINDOW));
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        if (level instanceof ServerLevel) {
            stack.hurtAndBreak(DURABILITY_COST, player, hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        return target instanceof Enemy && source.getEntity() instanceof Player p && p.getMainHandItem().getItem() == this && bonusReady(p) ? BONUS : 0f;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (target instanceof Enemy && attacker instanceof Player p) {
            Dash d = dashes(p.level()).get(p);
            if (d != null && p.level().getGameTime() <= d.until) {
                d.struck = true;
            }
        }
    }
}

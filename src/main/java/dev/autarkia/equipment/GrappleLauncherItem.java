package dev.autarkia.equipment;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.EventTrigger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import org.jspecify.annotations.Nullable;

/**
 * {@code grapple_launcher} (07 section 1.5): 1,000,000 FE charged at 5,000 FE/t. Use fires a {@link GrappleHookEntity}
 * for 2,000 FE. While use is held and the hook holds, the user is pulled toward it at 1.2 blocks/tick for 20 FE/t, or
 * hangs in place while sneaking for 5 FE/t; without the FE the hook lets go. Releasing use detaches the hook; for 40
 * ticks after a detach the user takes no fall damage. Cooldown 10 ticks after release. Arriving within 2 blocks of a
 * hook that attached 40 or more blocks away fires the {@code grapple_40} equipment event once per hook. The pull sets
 * the motion on both sides: the client's motion moves a real player, the server's moves players nobody else simulates.
 */
public class GrappleLauncherItem extends PoweredGeoItem {
    public static final int CAPACITY = 1000000;
    public static final int INPUT = 5000;
    public static final int SHOT_COST = 2000;
    public static final int PULL_COST = 20;
    public static final int HANG_COST = 5;
    public static final double PULL_SPEED = 1.2;
    public static final int SAFE_TICKS = 40;
    public static final int COOLDOWN = 10;
    public static final double SWING_DISTANCE = 40;
    public static final double ARRIVE = 2.0;
    private static final Map<UUID, Long> SAFE_UNTIL = new HashMap<>();

    public GrappleLauncherItem(Properties props) {
        super(props, CAPACITY, INPUT, 0, "grapple_launcher", "fire");
    }

    /** Register the fall damage listener (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(GrappleLauncherItem::onFall);
    }

    /** Grant the entity 40 ticks without fall damage from now (server side). */
    public static void protect(LivingEntity le) {
        long now = le.level().getGameTime();
        SAFE_UNTIL.values().removeIf(t -> t < now);
        SAFE_UNTIL.put(le.getUUID(), now + SAFE_TICKS);
    }

    /** Return true while the entity's fall protection after a detach lasts. */
    public static boolean safe(LivingEntity le) {
        Long until = SAFE_UNTIL.get(le.getUUID());
        return until != null && le.level().getGameTime() <= until;
    }

    private static void onFall(LivingFallEvent e) {
        if (!e.getEntity().level().isClientSide() && safe(e.getEntity())) {
            e.setDamageMultiplier(0);
        }
    }

    /** Return the hook the entity fired, or null. */
    public static @Nullable GrappleHookEntity hook(Level level, Entity owner) {
        List<GrappleHookEntity> hooks = level.getEntitiesOfClass(GrappleHookEntity.class, owner.getBoundingBox().inflate(GrappleHookEntity.RANGE + 8),
            h -> h.getOwner() == owner);
        return hooks.isEmpty() ? null : hooks.get(0);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (energy(stack) < SHOT_COST || player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel sl) {
            GrappleHookEntity old = hook(sl, player);
            if (old != null) {
                old.discard();
            }
            spend(stack, SHOT_COST);
            sl.addFreshEntity(new GrappleHookEntity(sl, player));
            playWork(player, stack);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), AutarkiaSounds.of("item.grapple.fire"), SoundSource.PLAYERS, 1f, 0.8f);
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        GrappleHookEntity hook = hook(level, user);
        if (hook == null || !hook.attached()) {
            return;
        }
        boolean hang = user.isShiftKeyDown();
        int cost = hang ? HANG_COST : PULL_COST;
        if (level.isClientSide() ? energy(stack) < cost : !spend(stack, cost)) {
            if (!level.isClientSide()) {
                protect(user);
                hook.discard();
            }
            return;
        }
        user.resetFallDistance();
        if (hang) {
            user.setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 to = hook.position().subtract(GrappleHookEntity.center(user));
        double d = to.length();
        user.setDeltaMovement(d <= PULL_SPEED ? to : to.scale(PULL_SPEED / d));
        if (!level.isClientSide()) {
            hook.reel(d);
            if (d <= ARRIVE && hook.completePull() && user instanceof ServerPlayer sp) {
                EventTrigger.EQUIPMENT.get().fire(sp, "grapple_40");
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (level instanceof ServerLevel sl) {
            GrappleHookEntity hook = hook(sl, user);
            if (hook != null) {
                protect(user);
                hook.discard();
            }
            stopTriggeredAnim(user, GeoItem.getOrAssignId(stack, sl), "main", "reel");
        }
        if (user instanceof Player p) {
            p.getCooldowns().addCooldown(stack, COOLDOWN);
        }
        return true;
    }

    /** Loop the reel animation of the stack for everyone tracking the holder. */
    public void playReel(LivingEntity holder, ItemStack stack) {
        if (holder.level() instanceof ServerLevel sl) {
            triggerAnim(holder, GeoItem.getOrAssignId(stack, sl), "main", "reel");
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        var main = new AnimationController<GrappleLauncherItem>("main", 2,
            test -> test.setAndContinue(RawAnimation.begin().thenLoop("animation.grapple_launcher.idle")));
        main.triggerableAnim("fire", RawAnimation.begin().thenPlay("animation.grapple_launcher.fire"));
        main.triggerableAnim("reel", RawAnimation.begin().thenLoop("animation.grapple_launcher.reel"));
        controllers.add(main);
    }
}

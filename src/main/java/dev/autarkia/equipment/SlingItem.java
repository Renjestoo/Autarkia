package dev.autarkia.equipment;

import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * Bronze sling (07 section 1.2): hold use 5 to 20 ticks and release. Ammo from {@code #autarkia:sling_ammo}:
 * nuggets 4 damage, other ammo (cobblestone) 3; velocity 0.6 to 1.8 by charge, inaccuracy 1.0, 1 durability per shot,
 * cooldown 10 ticks.
 */
public class SlingItem extends Item {
    public static final TagKey<Item> AMMO = TagKey.create(Registries.ITEM, Autarkia.id("sling_ammo"));
    public static final TagKey<Item> NUGGETS = TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "nuggets"));
    public static final int MIN_CHARGE = 5;
    public static final int FULL_CHARGE = 20;

    public SlingItem(Properties props) {
        super(props);
    }

    private static ItemStack findAmmo(Player player) {
        if (player.getOffhandItem().is(AMMO)) {
            return player.getOffhandItem();
        }
        for (ItemStack s : player.getInventory()) {
            if (s.is(AMMO)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (findAmmo(player).isEmpty() && !player.getAbilities().instabuild) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    /** Return the launch velocity for a charge in ticks (0 below the minimum charge). */
    public static float velocity(int charge) {
        if (charge < MIN_CHARGE) {
            return 0;
        }
        return 0.6f + 1.2f * (Math.min(charge, FULL_CHARGE) - MIN_CHARGE) / (float) (FULL_CHARGE - MIN_CHARGE);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        int charge = getUseDuration(stack, entity) - remaining;
        float v = velocity(charge);
        if (v <= 0) {
            return false;
        }
        ItemStack ammo = findAmmo(player);
        if (ammo.isEmpty() && !player.getAbilities().instabuild) {
            return false;
        }
        ItemStack shot = ammo.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.COBBLESTONE) : ammo.copyWithCount(1);
        if (level instanceof ServerLevel sl) {
            SlingShotEntity p = new SlingShotEntity(sl, player, shot, shot.is(NUGGETS) ? 4 : 3);
            p.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, v, 1.0f);
            sl.addFreshEntity(p);
            stack.hurtAndBreak(1, player, player.getUsedItemHand());
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), AutarkiaSounds.of("item.sling.throw"), SoundSource.PLAYERS, 1f, 0.7f);
        }
        if (!player.getAbilities().instabuild && !ammo.isEmpty()) {
            ammo.shrink(1);
        }
        player.getCooldowns().addCooldown(stack, 10);
        return true;
    }
}

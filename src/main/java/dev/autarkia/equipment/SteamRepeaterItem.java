package dev.autarkia.equipment;

import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.EventTrigger;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * {@code steam_repeater} (07 section 1.3): pressure 0 to 100 in {@code autarkia:pressure}. Sneak + hold use pumps 2.5
 * per tick (40 ticks to full); hold use fires one shot per 5 ticks for 10 pressure and one {@code #autarkia:steam_repeater_ammo}
 * item (steel nugget): damage 5, velocity 3.0, inaccuracy 1.5, 1 durability. Pressure leaks 1 per 20 ticks. Ten shots
 * from one full charge fire the {@code repeater_empty} equipment event.
 */
public class SteamRepeaterItem extends Item implements com.geckolib.animatable.GeoItem {
    public static final TagKey<Item> AMMO = TagKey.create(Registries.ITEM, Autarkia.id("steam_repeater_ammo"));
    public static final int SHOT_COST = 10;
    private static final java.util.Map<java.util.UUID, Integer> SHOTS = new java.util.HashMap<>();
    private final com.geckolib.animatable.instance.AnimatableInstanceCache cache = com.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    public SteamRepeaterItem(Properties props) {
        super(props);
        com.geckolib.animatable.GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar controllers) {
        var main = new com.geckolib.animation.AnimationController<SteamRepeaterItem>("main", 2,
            test -> test.setAndContinue(com.geckolib.animation.RawAnimation.begin().thenLoop("animation.steam_repeater.idle")));
        main.triggerableAnim("fire", com.geckolib.animation.RawAnimation.begin().thenPlay("animation.steam_repeater.fire"));
        main.triggerableAnim("pump", com.geckolib.animation.RawAnimation.begin().thenPlay("animation.steam_repeater.pump"));
        controllers.add(main);
    }

    @Override
    public com.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Supply the GeckoLib item renderer (client): model and animation {@code steam_repeater}, texture {@code item/steam_repeater_geo}. */
    @Override
    public void createGeoRenderer(java.util.function.Consumer<com.geckolib.animatable.client.GeoRenderProvider> consumer) {
        consumer.accept(new com.geckolib.animatable.client.GeoRenderProvider() {
            private com.geckolib.renderer.GeoItemRenderer<SteamRepeaterItem> renderer;

            @Override
            public com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new com.geckolib.renderer.GeoItemRenderer<>(new com.geckolib.model.DefaultedItemGeoModel<SteamRepeaterItem>(
                        Autarkia.id("steam_repeater")).withAltTexture(Autarkia.id("steam_repeater_geo")));
                }
                return renderer;
            }
        });
    }

    /** Return the pressure of a stack. */
    public static int pressure(ItemStack stack) {
        return stack.getOrDefault(AutarkiaComponents.PRESSURE.get(), 0);
    }

    private static @Nullable ItemStack ammo(Player player) {
        if (player.getOffhandItem().is(AMMO)) {
            return player.getOffhandItem();
        }
        for (ItemStack s : player.getInventory()) {
            if (s.is(AMMO)) {
                return s;
            }
        }
        return null;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
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

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel sl) || !(entity instanceof Player player)) {
            return;
        }
        int used = getUseDuration(stack, entity) - remaining;
        int p = pressure(stack);
        if (player.isShiftKeyDown()) {
            if (p < 100) {
                int next = Math.min(100, p + (used % 2 == 0 ? 3 : 2));
                stack.set(AutarkiaComponents.PRESSURE.get(), next);
                if (next == 100) {
                    SHOTS.put(player.getUUID(), 0);
                }
                if (used % 8 == 0) {
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(), AutarkiaSounds.of("item.steam_repeater.pump"), SoundSource.PLAYERS, 0.6f, 1.6f);
                    triggerAnim(player, com.geckolib.animatable.GeoItem.getOrAssignId(stack, sl), "main", "pump");
                }
            }
            return;
        }
        if (used % 5 != 0 || p < SHOT_COST) {
            return;
        }
        ItemStack ammo = ammo(player);
        boolean creative = player.getAbilities().instabuild;
        if (ammo == null && !creative) {
            return;
        }
        ItemStack shot = ammo == null ? new ItemStack(dev.autarkia.registry.AutarkiaItems.STEEL_NUGGET.get()) : ammo.copyWithCount(1);
        SlingShotEntity e = new SlingShotEntity(sl, player, shot, 5);
        e.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 3.0f, 1.5f);
        sl.addFreshEntity(e);
        if (ammo != null && !creative) {
            ammo.shrink(1);
        }
        stack.set(AutarkiaComponents.PRESSURE.get(), p - SHOT_COST);
        Integer since = SHOTS.get(player.getUUID());
        int shots = since == null ? -1 : since + 1;
        if (since != null) {
            SHOTS.put(player.getUUID(), shots);
        }
        stack.hurtAndBreak(1, player, player.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        Vec3 muzzle = player.getEyePosition().add(player.getLookAngle().scale(0.8));
        sl.sendParticles(ParticleTypes.CLOUD, muzzle.x, muzzle.y, muzzle.z, 3, 0.05, 0.05, 0.05, 0.02);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(), AutarkiaSounds.of("item.steam_repeater.fire"), SoundSource.PLAYERS, 0.8f, 1.4f);
        triggerAnim(player, com.geckolib.animatable.GeoItem.getOrAssignId(stack, sl), "main", "fire");
        if (shots >= 10 && p - SHOT_COST == 0 && player instanceof ServerPlayer sp) {
            SHOTS.remove(player.getUUID());
            EventTrigger.EQUIPMENT.get().fire(sp, "repeater_empty");
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        int p = pressure(stack);
        if (p > 0 && level.getGameTime() % 20 == 0 && !(entity instanceof LivingEntity le && le.isUsingItem() && le.getUseItem() == stack)) {
            stack.set(AutarkiaComponents.PRESSURE.get(), p - 1);
            SHOTS.remove(entity.getUUID());
            if (p - 1 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1, entity.getZ(), 4, 0.1, 0.1, 0.1, 0.01);
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), AutarkiaSounds.of("item.steam_repeater.hiss"), SoundSource.PLAYERS, 0.4f,
                    1.5f);
            }
        }
    }
}

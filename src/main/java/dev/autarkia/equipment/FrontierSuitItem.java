package dev.autarkia.equipment;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaSounds;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Frontier suit piece (07 section 1.5): 2,000,000 FE charged at 5,000 FE/t, every exo rig function of its slot plus its
 * own, all while the suit is on and the piece has FE. Helmet: night vision (refreshed every 100 ticks), 10 FE/t. Chest:
 * +0.3 knockback and +0.5 explosion knockback resistance, 2 FE/t. Legs: +20% speed while sprinting, 10 FE/t then.
 * Boots: jet while jump is held in the air, +0.10 vertical velocity per tick up to 0.5, air control 0.04, 80 FE/t; a
 * jet tick resets the fall distance; fall damage cancel costs 50 FE per block above 3. The server spends the FE and
 * moves players nobody else simulates; the local client applies the jet to its own player. GeckoLib armor model
 * {@code armor/frontier_suit}; the {@code jet} animation plays while the boots fire.
 */
public class FrontierSuitItem extends SuitItem {
    public static final int CAPACITY = 2000000;
    public static final int INPUT = 5000;
    public static final int FALL_COST = 50;
    public static final int NIGHT_VISION_COST = 10;
    public static final int WIND_COST = 2;
    public static final int SPRINT_COST = 10;
    public static final int JET_COST = 80;
    public static final double JET_ACCEL = 0.10;
    public static final double JET_MAX = 0.5;
    public static final float AIR_CONTROL = 0.04f;
    private static final float VANILLA_AIR_CONTROL = 0.02f;
    private static final Identifier KNOCKBACK = Autarkia.id("frontier_knockback");
    private static final Identifier EXPLOSION = Autarkia.id("frontier_explosion_knockback");
    private static final Identifier SPRINT = Autarkia.id("frontier_sprint");

    public FrontierSuitItem(Properties props, ArmorType type) {
        this(props, type, CAPACITY, INPUT, "frontier_suit");
    }

    /** Create a frontier-class suit piece with its own FE capacity, charge rate per tick and GeckoLib armor model name. */
    protected FrontierSuitItem(Properties props, ArmorType type, int capacity, int input, String model) {
        super(props, type, capacity, input, model);
        GeoItem.registerSyncedAnimatable(this);
    }

    /** Register the local player's jet move (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Pre e) -> {
            Player p = e.getEntity();
            if (p.isLocalInstanceAuthoritative() && jetting(p, p.isJumping())) {
                thrust(p);
                p.moveRelative(AIR_CONTROL - VANILLA_AIR_CONTROL, new Vec3(p.xxa, 0, p.zza));
            }
        });
    }

    /** Return true when the player's frontier boots fire: jump held in the air, boots on with at least 80 FE. */
    public static boolean jetting(Player p, boolean jump) {
        ItemStack boots = p.getItemBySlot(EquipmentSlot.FEET);
        return jump && boots.getItem() instanceof FrontierSuitItem && active(boots) && PoweredItem.energy(boots) >= JET_COST && !p.onGround()
            && !p.getAbilities().flying && !p.isInWater() && !p.isFallFlying() && !p.isPassenger();
    }

    /** Add one jet tick of lift: +0.10 vertical velocity up to 0.5 (faster climbs are kept), and reset the fall distance. */
    public static void thrust(LivingEntity le) {
        Vec3 v = le.getDeltaMovement();
        if (v.y < JET_MAX) {
            le.setDeltaMovement(v.x, Math.min(v.y + JET_ACCEL, JET_MAX), v.z);
        }
        le.resetFallDistance();
    }

    @Override
    public int fallCost() {
        return FALL_COST;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof LivingEntity le) || slot == null || slot != armorType().getSlot()) {
            return;
        }
        boolean on = active(stack);
        switch (armorType()) {
            case HELMET -> nightVision(le, stack, level, on && PoweredItem.energy(stack) >= NIGHT_VISION_COST);
            case CHESTPLATE -> {
                attribute(le, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, on ? 0.3 : 0, AttributeModifier.Operation.ADD_VALUE);
                attribute(le, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, EXPLOSION, on ? 0.5 : 0, AttributeModifier.Operation.ADD_VALUE);
                if (on) {
                    PoweredItem.setEnergy(stack, PoweredItem.energy(stack) - WIND_COST);
                }
            }
            case LEGGINGS -> {
                boolean sprint = on && le.isSprinting() && PoweredItem.energy(stack) >= SPRINT_COST;
                attribute(le, Attributes.MOVEMENT_SPEED, SPRINT, sprint ? 0.2 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                if (sprint) {
                    PoweredItem.setEnergy(stack, PoweredItem.energy(stack) - SPRINT_COST);
                }
            }
            case BOOTS -> {
                if (le instanceof ServerPlayer sp && jetting(sp, sp.getLastClientInput().jump())) {
                    PoweredItem.setEnergy(stack, PoweredItem.energy(stack) - JET_COST);
                    thrust(sp);
                    if (level.getGameTime() % 6 == 0) {
                        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), AutarkiaSounds.of("armor.jet.loop"), SoundSource.PLAYERS, 0.6f, 0.6f);
                        triggerArmorAnim(sp, GeoItem.getOrAssignId(stack, level), "main", "jet");
                    }
                }
            }
            default -> {
            }
        }
    }

    private static void nightVision(LivingEntity le, ItemStack stack, ServerLevel level, boolean on) {
        if (!on) {
            removeNightVision(le);
            return;
        }
        PoweredItem.setEnergy(stack, PoweredItem.energy(stack) - NIGHT_VISION_COST);
        if (!le.hasEffect(MobEffects.NIGHT_VISION) || level.getGameTime() % 100 == 0) {
            le.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, true));
        }
    }

    /** Remove night vision the helmet gave (ambient and without particles); potion night vision stays. */
    private static void removeNightVision(LivingEntity le) {
        MobEffectInstance nv = le.getEffect(MobEffects.NIGHT_VISION);
        if (nv != null && nv.isAmbient() && !nv.isVisible()) {
            le.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private static void attribute(LivingEntity le, Holder<Attribute> attr, Identifier id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = le.getAttribute(attr);
        if (inst == null) {
            return;
        }
        if (amount == 0) {
            inst.removeModifier(id);
        } else {
            inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
        }
    }

    @Override
    public void clear(LivingEntity le) {
        super.clear(le);
        switch (armorType()) {
            case HELMET -> removeNightVision(le);
            case CHESTPLATE -> {
                attribute(le, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, 0, AttributeModifier.Operation.ADD_VALUE);
                attribute(le, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, EXPLOSION, 0, AttributeModifier.Operation.ADD_VALUE);
            }
            case LEGGINGS -> attribute(le, Attributes.MOVEMENT_SPEED, SPRINT, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            default -> {
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        var main = new AnimationController<FrontierSuitItem>("main", 2, test -> test.setAndContinue(RawAnimation.begin().thenLoop("animation.frontier_suit.idle")));
        main.triggerableAnim("jet", RawAnimation.begin().thenPlay("animation.frontier_suit.jet"));
        controllers.add(main);
    }
}

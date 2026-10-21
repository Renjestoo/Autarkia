package dev.autarkia.equipment;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import org.jspecify.annotations.Nullable;

/**
 * Exo rig suit piece (07 sections 1.4 and 2.2): durability plus 500,000 FE in {@code autarkia:energy}, functions on
 * while {@code autarkia:suit_enabled} is true and the piece has FE. Helmet: every 20 ticks it balances FE between the
 * worn exo pieces, +1 oxygen bonus. Chest: +0.25 jump strength, 100 FE per jump. Legs: +0.5 step height, 2 FE per tick
 * while moving on the ground. Boots: cancel fall damage at 100 FE per block fallen above 3 (with too little FE only
 * FE / 100 blocks). The suit key turns all worn pieces on or off. GeckoLib armor model {@code armor/exo_rig}.
 */
public class SuitItem extends Item implements GeoItem {
    public static final int CAPACITY = 500000;
    public static final int INPUT = 2000;
    public static final int JUMP_COST = 100;
    public static final int FALL_COST = 100;
    private static final Identifier JUMP = Autarkia.id("exo_jump");
    private static final Identifier STEP = Autarkia.id("exo_step");
    private static final Identifier OXYGEN = Autarkia.id("exo_oxygen");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ArmorType type;

    public SuitItem(Properties props, ArmorType type) {
        super(props);
        this.type = type;
    }

    /** Register the jump and fall listeners (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((LivingEvent.LivingJumpEvent e) -> onJump(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(SuitItem::onFall);
    }

    /** Return the armor type of the piece. */
    public ArmorType armorType() {
        return type;
    }

    /** Return true when the piece works: suit enabled and FE left. */
    public static boolean active(ItemStack stack) {
        return stack.getItem() instanceof SuitItem && stack.getOrDefault(AutarkiaComponents.SUIT_ENABLED.get(), true) && PoweredItem.energy(stack) > 0;
    }

    /** Return the worn suit pieces of the entity. */
    public static List<ItemStack> worn(LivingEntity e) {
        List<ItemStack> out = new ArrayList<>();
        for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = e.getItemBySlot(s);
            if (st.getItem() instanceof SuitItem) {
                out.add(st);
            }
        }
        return out;
    }

    /** Toggle all worn suit pieces of the entity; return the new state, or null when no piece is worn. */
    public static @Nullable Boolean toggle(LivingEntity e) {
        List<ItemStack> pieces = worn(e);
        if (pieces.isEmpty()) {
            return null;
        }
        boolean next = !pieces.get(0).getOrDefault(AutarkiaComponents.SUIT_ENABLED.get(), true);
        for (ItemStack s : pieces) {
            s.set(AutarkiaComponents.SUIT_ENABLED.get(), next);
        }
        return next;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof LivingEntity le) || slot == null || slot != type.getSlot()) {
            return;
        }
        boolean on = active(stack);
        switch (type) {
            case HELMET -> {
                modifier(le, Attributes.OXYGEN_BONUS, OXYGEN, on ? 1.0 : 0);
                if (on && level.getGameTime() % 20 == 0) {
                    balance(le);
                }
            }
            case CHESTPLATE -> modifier(le, Attributes.JUMP_STRENGTH, JUMP, on && PoweredItem.energy(stack) >= JUMP_COST ? 0.25 : 0);
            case LEGGINGS -> {
                modifier(le, Attributes.STEP_HEIGHT, STEP, on ? 0.5 : 0);
                if (on && le.onGround() && le.getKnownMovement().horizontalDistanceSqr() > 1.0E-4) {
                    PoweredItem.setEnergy(stack, PoweredItem.energy(stack) - 2);
                }
            }
            default -> {
            }
        }
    }

    private static void modifier(LivingEntity le, Holder<Attribute> attr, Identifier id, double amount) {
        AttributeInstance inst = le.getAttribute(attr);
        if (inst == null) {
            return;
        }
        if (amount == 0) {
            inst.removeModifier(id);
        } else {
            inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void balance(LivingEntity le) {
        List<ItemStack> pieces = worn(le);
        long total = 0;
        for (ItemStack s : pieces) {
            total += PoweredItem.energy(s);
        }
        int each = (int) (total / pieces.size());
        int rest = (int) (total - (long) each * pieces.size());
        for (ItemStack s : pieces) {
            PoweredItem.setEnergy(s, each + (rest-- > 0 ? 1 : 0));
        }
    }

    private static void onJump(LivingEntity le) {
        if (le.level().isClientSide()) {
            return;
        }
        ItemStack chest = le.getItemBySlot(EquipmentSlot.CHEST);
        if (active(chest)) {
            PoweredItem.spend(chest, JUMP_COST);
        }
    }

    private static void onFall(LivingFallEvent e) {
        LivingEntity le = e.getEntity();
        ItemStack boots = le.getItemBySlot(EquipmentSlot.FEET);
        if (le.level().isClientSide() || !active(boots) || e.getDistance() <= 3) {
            return;
        }
        double over = e.getDistance() - 3;
        int fe = PoweredItem.energy(boots);
        double covered = Math.min(over, fe / (double) FALL_COST);
        PoweredItem.setEnergy(boots, fe - (int) Math.ceil(covered * FALL_COST));
        e.setDistance((float) (e.getDistance() - covered));
        if (covered >= over) {
            e.setDamageMultiplier(0);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * PoweredItem.energy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return PoweredItem.BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("tooltip.autarkia.energy", PoweredItem.energy(stack), CAPACITY).withColor(PoweredItem.BAR_COLOR));
        out.accept(Component.translatable(stack.getOrDefault(AutarkiaComponents.SUIT_ENABLED.get(), true) ? "tooltip.autarkia.suit.on"
            : "tooltip.autarkia.suit.off"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SuitItem>("main", 2, test -> test.setAndContinue(RawAnimation.begin().thenLoop("animation.exo_rig.idle"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<SuitItem, net.minecraft.client.renderer.entity.state.HumanoidRenderState> renderer;

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
                if (renderer == null) {
                    renderer = new GeoArmorRenderer<>(new com.geckolib.model.DefaultedGeoModel<SuitItem>(Autarkia.id("exo_rig")) {
                        @Override
                        protected String subtype() {
                            return "armor";
                        }
                    });
                }
                return renderer;
            }
        });
    }
}

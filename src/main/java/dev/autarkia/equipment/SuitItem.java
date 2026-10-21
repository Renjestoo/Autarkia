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
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import org.jspecify.annotations.Nullable;

/**
 * FE suit piece (07 sections 1.4, 1.5 and 2.2): durability plus FE in {@code autarkia:energy}, functions on while
 * {@code autarkia:suit_enabled} is true and the piece has FE. The exo rig (500,000 FE, 2,000 FE/t) functions: helmet
 * balances FE between the worn suit pieces every 20 ticks in proportion to their capacity, +1 oxygen bonus; chest +0.25
 * jump strength, 100 FE per jump; legs +0.5 step height, 2 FE per tick while moving on the ground; boots cancel fall
 * damage at {@link #fallCost()} FE per block fallen above 3 (with too little FE only FE / cost blocks). A piece that
 * leaves its slot drops its attribute modifiers. The suit key turns all worn pieces on or off. GeckoLib armor model
 * {@code armor/<model>}.
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
    private final int capacity;
    private final int input;
    private final String model;

    public SuitItem(Properties props, ArmorType type) {
        this(props, type, CAPACITY, INPUT, "exo_rig");
    }

    /** Create a suit piece with its own FE capacity, charge rate per tick and GeckoLib armor model name. */
    protected SuitItem(Properties props, ArmorType type, int capacity, int input, String model) {
        super(props);
        this.type = type;
        this.capacity = capacity;
        this.input = input;
        this.model = model;
    }

    /** Register the jump, fall and equipment change listeners (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((LivingEvent.LivingJumpEvent e) -> onJump(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(SuitItem::onFall);
        NeoForge.EVENT_BUS.addListener((LivingEquipmentChangeEvent e) -> {
            if (e.getFrom().getItem() instanceof SuitItem s && e.getSlot() == s.type.getSlot() && !e.getFrom().is(e.getTo().getItem())) {
                s.clear(e.getEntity());
            }
        });
    }

    /** Return the armor type of the piece. */
    public ArmorType armorType() {
        return type;
    }

    /** Return the FE capacity of the piece. */
    public int capacity() {
        return capacity;
    }

    /** Return the FE per tick a charger may insert. */
    public int input() {
        return input;
    }

    /** Return the FE the boots spend per block of cancelled fall damage. */
    public int fallCost() {
        return FALL_COST;
    }

    /** Return true when the piece's functions cost the wearer no FE right now (Aegis pieces while tethered); false by default. */
    protected boolean costFree(LivingEntity le) {
        return false;
    }

    /** Remove the attribute modifiers this piece adds (when it leaves its slot). */
    public void clear(LivingEntity le) {
        switch (type) {
            case HELMET -> modifier(le, Attributes.OXYGEN_BONUS, OXYGEN, 0);
            case CHESTPLATE -> modifier(le, Attributes.JUMP_STRENGTH, JUMP, 0);
            case LEGGINGS -> modifier(le, Attributes.STEP_HEIGHT, STEP, 0);
            default -> {
            }
        }
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
        long cap = 0;
        for (ItemStack s : pieces) {
            total += PoweredItem.energy(s);
            cap += ((SuitItem) s.getItem()).capacity;
        }
        long given = 0;
        for (ItemStack s : pieces) {
            long share = total * ((SuitItem) s.getItem()).capacity / cap;
            PoweredItem.setEnergy(s, (int) share);
            given += share;
        }
        for (ItemStack s : pieces) {
            int room = (int) Math.min(total - given, ((SuitItem) s.getItem()).capacity - PoweredItem.energy(s));
            PoweredItem.setEnergy(s, PoweredItem.energy(s) + room);
            given += room;
        }
    }

    private static void onJump(LivingEntity le) {
        if (le.level().isClientSide()) {
            return;
        }
        ItemStack chest = le.getItemBySlot(EquipmentSlot.CHEST);
        if (active(chest) && !((SuitItem) chest.getItem()).costFree(le)) {
            PoweredItem.spend(chest, JUMP_COST);
        }
    }

    private static void onFall(LivingFallEvent e) {
        LivingEntity le = e.getEntity();
        ItemStack boots = le.getItemBySlot(EquipmentSlot.FEET);
        if (le.level().isClientSide() || !active(boots) || e.getDistance() <= 3) {
            return;
        }
        SuitItem suit = (SuitItem) boots.getItem();
        int cost = suit.fallCost();
        double over = e.getDistance() - 3;
        int fe = PoweredItem.energy(boots);
        boolean free = suit.costFree(le);
        double covered = free ? over : Math.min(over, fe / (double) cost);
        if (!free) {
            PoweredItem.setEnergy(boots, fe - (int) Math.ceil(covered * cost));
        }
        e.setDistance(e.getDistance() - covered);
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
        return Math.round(13f * PoweredItem.energy(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return PoweredItem.BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("tooltip.autarkia.energy", PoweredItem.energy(stack), capacity).withColor(PoweredItem.BAR_COLOR));
        out.accept(Component.translatable(stack.getOrDefault(AutarkiaComponents.SUIT_ENABLED.get(), true) ? "tooltip.autarkia.suit.on"
            : "tooltip.autarkia.suit.off"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SuitItem>("main", 2, test -> test.setAndContinue(RawAnimation.begin().thenLoop("animation." + model + ".idle"))));
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
                    renderer = new GeoArmorRenderer<>(new com.geckolib.model.DefaultedGeoModel<SuitItem>(Autarkia.id(model)) {
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

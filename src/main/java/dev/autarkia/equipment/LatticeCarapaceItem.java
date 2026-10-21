package dev.autarkia.equipment;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import org.jspecify.annotations.Nullable;

/**
 * Lattice carapace piece (07 section 1.6, decision G11): 5,000,000 FE charged at 20,000 FE/t, every exo rig and frontier
 * suit function of its slot, plus self-repair: while worn, switched on ({@code autarkia:suit_enabled}) and damaged, the
 * piece mends {@link #REPAIR_POINTS} durability every {@link #REPAIR_PERIOD} ticks for {@link #REPAIR_COST} FE per
 * point. Anvil repair uses {@code #autarkia:repairs_lattice}. GeckoLib armor model {@code armor/lattice_carapace} with
 * the plate shimmer {@code idle} and the boot thrust {@code jet}.
 */
public class LatticeCarapaceItem extends FrontierSuitItem {
    public static final int CAPACITY = 5000000;
    public static final int INPUT = 20000;
    public static final int REPAIR_PERIOD = 20;
    public static final int REPAIR_POINTS = 1;
    public static final int REPAIR_COST = 2000;

    public LatticeCarapaceItem(Properties props, ArmorType type) {
        super(props, type, CAPACITY, INPUT, "lattice_carapace");
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (slot == armorType().getSlot() && level.getGameTime() % REPAIR_PERIOD == 0 && stack.isDamaged() && active(stack)
            && PoweredItem.spend(stack, REPAIR_POINTS * REPAIR_COST)) {
            stack.setDamageValue(stack.getDamageValue() - REPAIR_POINTS);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        var main = new AnimationController<LatticeCarapaceItem>("main", 2,
            test -> test.setAndContinue(RawAnimation.begin().thenLoop("animation.lattice_carapace.idle")));
        main.triggerableAnim("jet", RawAnimation.begin().thenPlay("animation.lattice_carapace.jet"));
        controllers.add(main);
    }
}

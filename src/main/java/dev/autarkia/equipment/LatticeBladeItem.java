package dev.autarkia.equipment;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * {@code lattice_blade} (07 section 1.6): 12.0 damage, 1.6 attacks per second, 5,000,000 FE charged at 20,000 FE/t. A
 * melee hit with at least {@link #HIT_COST} FE pierces armor: the armor reduction of that hit is multiplied by
 * {@link #PIERCE} (decision G13, through the NeoForge damage container), and the hit spends {@link #HIT_COST} FE. With
 * less FE the hit is a normal sword hit with no piercing.
 */
public class LatticeBladeItem extends PoweredItem {
    public static final int CAPACITY = 5000000;
    public static final int INPUT = 20000;
    public static final int HIT_COST = 200;
    public static final float PIERCE = 0.5f;

    public LatticeBladeItem(Properties props) {
        super(props, CAPACITY, INPUT, 0);
    }

    /** Register the armor piercing listener (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(LatticeBladeItem::onIncoming);
    }

    /** Return true when the source is a direct melee hit with a lattice blade that can pay for the hit. */
    public static boolean piercing(DamageSource source) {
        ItemStack weapon = source.getWeaponItem();
        return weapon != null && source.isDirect() && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK))
            && weapon.getItem() instanceof LatticeBladeItem && energy(weapon) >= HIT_COST;
    }

    private static void onIncoming(LivingIncomingDamageEvent e) {
        if (piercing(e.getSource())) {
            e.getContainer().addModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * PIERCE);
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide()) {
            spend(stack, HIT_COST);
        }
    }
}

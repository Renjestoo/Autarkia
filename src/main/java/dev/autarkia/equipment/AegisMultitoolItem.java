package dev.autarkia.equipment;

import dev.autarkia.registry.AutarkiaComponents;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jspecify.annotations.Nullable;

/**
 * {@code aegis_multitool} (07 sections 1.7 and 2.1): the nanite multitool rules (pickaxe, axe, shovel, hoe, shears and
 * sword-efficient blocks at netherite level, the block transformers on use, the shears abilities) at speed 2,000, so it
 * breaks blocks up to hardness 66 instantly. Unbreakable, 100,000,000 FE, 50 FE per block, 14.0 damage at 1.2 attacks
 * per second. Mode key: 1x1, 3x3 or 5x5 through {@link AreaMining} (speed factor 1.0, decision G13). Enchant key: none,
 * Silk Touch or Fortune III ({@code autarkia:enchant_mode}); each change rewrites the Silk Touch and Fortune entries of
 * {@code ENCHANTMENTS}, other enchantments stay. Charges from the tether at 50,000 FE/t ({@link AegisTether}). At 0 FE it
 * mines at speed 1.0 with no drops and the transforms and shears abilities stop.
 */
public class AegisMultitoolItem extends PoweredGeoItem implements ModalItem {
    public static final int CAPACITY = 100000000;
    public static final int INPUT = AegisTether.CHARGE;
    public static final int BLOCK_COST = 50;
    public static final float SPEED = 2000.0F;
    public static final float AREA_SPEED = 1.0F;
    public static final int FORTUNE_LEVEL = 3;

    public AegisMultitoolItem(Properties props) {
        super(props, CAPACITY, INPUT, BLOCK_COST, "aegis_multitool", "use");
    }

    @Override
    public List<ToolMode> modes() {
        return List.of(ToolMode.SINGLE, ToolMode.AREA3, ToolMode.AREA5);
    }

    @Override
    public float areaSpeed(ToolMode mode) {
        return AREA_SPEED;
    }

    /** Select the next enchant mode on the stack, rewrite its enchantments and return the new mode. */
    public static EnchantMode cycleEnchant(ItemStack stack, HolderLookup.Provider registries) {
        EnchantMode next = stack.getOrDefault(AutarkiaComponents.ENCHANT_MODE.get(), EnchantMode.NONE).next();
        applyEnchant(stack, next, registries);
        return next;
    }

    /** Set the enchant mode: NONE removes Silk Touch and Fortune, SILK sets Silk Touch I, FORTUNE sets Fortune III. */
    public static void applyEnchant(ItemStack stack, EnchantMode mode, HolderLookup.Provider registries) {
        var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        var silk = lookup.getOrThrow(Enchantments.SILK_TOUCH);
        var fortune = lookup.getOrThrow(Enchantments.FORTUNE);
        stack.set(AutarkiaComponents.ENCHANT_MODE.get(), mode);
        EnchantmentHelper.updateEnchantments(stack, e -> {
            e.removeIf(h -> h.is(Enchantments.SILK_TOUCH) || h.is(Enchantments.FORTUNE));
            if (mode == EnchantMode.SILK) {
                e.set(silk, 1);
            } else if (mode == EnchantMode.FORTUNE) {
                e.set(fortune, FORTUNE_LEVEL);
            }
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        return energy(ctx.getItemInHand()) <= 0 ? InteractionResult.PASS : NaniteMultitoolItem.transformBlocks(ctx);
    }

    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility ability) {
        return stack.getOrDefault(AutarkiaComponents.ENERGY.get(), 0) > 0
            && (ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(ability) || ability == ItemAbilities.SHOVEL_DOUSE);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        AegisTether.charge(level, entity, stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, display, out, flag);
        out.accept(Component.translatable("tooltip.autarkia.enchant_mode",
            Component.translatable(stack.getOrDefault(AutarkiaComponents.ENCHANT_MODE.get(), EnchantMode.NONE).key())));
    }
}

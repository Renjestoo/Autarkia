package dev.autarkia.equipment;

import dev.autarkia.registry.AutarkiaComponents;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An FE tool (07 sections 1.4 and 2.1 to 2.3): energy in {@code autarkia:energy}, no durability ({@code UNBREAKABLE}).
 * Mining a block costs blockCost FE; at 0 FE it mines at speed 1.0 with no drops and its abilities stop. The item bar
 * shows the charge in blue. Chargers insert through {@code Capabilities.Energy.ITEM} (maxExtract 0).
 */
public class PoweredItem extends Item {
    public static final int BAR_COLOR = 0x3FA9F5;
    private final int capacity;
    private final int input;
    private final int blockCost;

    public PoweredItem(Properties props, int capacity, int input, int blockCost) {
        super(props);
        this.capacity = capacity;
        this.input = input;
        this.blockCost = blockCost;
    }

    /** Return the FE capacity. */
    public int capacity() {
        return capacity;
    }

    /** Return the FE per tick a charger may insert. */
    public int input() {
        return input;
    }

    /** Return the stored FE of a stack. */
    public static int energy(ItemStack stack) {
        return stack.getOrDefault(AutarkiaComponents.ENERGY.get(), 0);
    }

    /** Set the stored FE of a stack, clamped to 0 and the capacity of its item. */
    public static void setEnergy(ItemStack stack, int fe) {
        int cap = stack.getItem() instanceof PoweredItem p ? p.capacity : stack.getItem() instanceof SuitItem s ? SuitItem.CAPACITY : Integer.MAX_VALUE;
        stack.set(AutarkiaComponents.ENERGY.get(), Math.max(0, Math.min(cap, fe)));
    }

    /** Take amount FE from the stack when it holds that much; return false (and take nothing) otherwise. */
    public static boolean spend(ItemStack stack, int amount) {
        int e = energy(stack);
        if (e < amount) {
            return false;
        }
        setEnergy(stack, e - amount);
        return true;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return energy(stack) <= 0 ? 1.0f : super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return energy(stack) > 0 && super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide() && blockCost > 0 && state.getDestroySpeed(level, pos) != 0 && energy(stack) > 0) {
            setEnergy(stack, energy(stack) - blockCost);
        }
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * energy(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("tooltip.autarkia.energy", energy(stack), capacity).withColor(BAR_COLOR));
    }
}

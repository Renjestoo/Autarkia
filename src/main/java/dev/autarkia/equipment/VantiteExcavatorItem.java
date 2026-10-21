package dev.autarkia.equipment;

import dev.autarkia.registry.AutarkiaComponents;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code vantite_excavator} (07 section 1.5): pickaxe and shovel at netherite level, speed 16, 2,000,000 FE charged at
 * 5,000 FE/t. Modes 1x1, 3x3 (through {@link AreaMining}, x0.5 speed) and vein (through {@link VeinMining}, x0.5 speed).
 * A block costs 250 FE, in vein mode 400 FE.
 */
public class VantiteExcavatorItem extends PoweredItem implements ModalItem {
    public static final int CAPACITY = 2000000;
    public static final int INPUT = 5000;
    public static final int BLOCK_COST = 250;
    public static final int VEIN_COST = 400;

    public VantiteExcavatorItem(Properties props) {
        super(props, CAPACITY, INPUT, BLOCK_COST);
    }

    /** Return true when the stack is in vein mode. */
    public static boolean vein(ItemStack stack) {
        return stack.get(AutarkiaComponents.TOOL_MODE.get()) == ToolMode.VEIN;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!vein(stack)) {
            return super.mineBlock(stack, level, state, pos, miner);
        }
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0 && energy(stack) > 0) {
            setEnergy(stack, energy(stack) - VEIN_COST);
        }
        return true;
    }

    @Override
    public List<ToolMode> modes() {
        return List.of(ToolMode.SINGLE, ToolMode.AREA3, ToolMode.VEIN);
    }

    @Override
    public float areaSpeed(ToolMode mode) {
        return mode == ToolMode.SINGLE ? 1f : 0.5f;
    }
}

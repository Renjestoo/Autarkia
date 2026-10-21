package dev.autarkia.logistics;

import dev.autarkia.registry.MachineBlock;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** The block of the {@code belt_splitter} and the {@code sorting_chute}: inline, belt height; entries travel away from the placing player. */
public class FilterBlock extends MachineBlock {
    public FilterBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props, type, false, Block.box(0, 0, 0, 16, 10, 16));
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }

    @Override
    public net.minecraft.world.level.block.state.@org.jspecify.annotations.Nullable BlockState getStateForPlacement(
        net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, ctx.getHorizontalDirection());
    }
}

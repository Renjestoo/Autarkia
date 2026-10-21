package dev.autarkia.logistics;

import dev.autarkia.registry.MachineBlock;
import java.util.function.Supplier;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * {@code matter_conduit} block (04 section 1.8): property {@code facing} is the output face a RECEIVE conduit pushes
 * into. Placed against a block, it faces that block, so a conduit set on a crate outputs into the crate.
 */
public class MatterConduitBlock extends MachineBlock {
    public MatterConduitBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props, type, false);
    }

    @Override
    protected Facing facingMode() {
        return Facing.ALL;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite());
    }
}

package dev.autarkia.logistics;

import dev.autarkia.registry.MachineBlock;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code mag_rail_station} (04 section 1.7): a 4 px platform with a block entity. It is one straight track cell of its
 * line along its facing axis (G6), so rails connect to both axis ends and sleds pass through or stop above it. Placing
 * it turns the free ends of the rails at its axis ends toward it; placing or removing it changes the line topology.
 */
public class MagRailStationBlock extends MachineBlock {
    public MagRailStationBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props, type, false, Block.box(0, 0, 0, 16, 4, 16));
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel && (!oldState.is(this) || front(oldState) != front(state))) {
            MagRailBlock.attachStation(level, pos, state);
            MagRailLine.changed();
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        MagRailLine.changed();
    }
}

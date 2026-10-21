package dev.autarkia.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A corner of a nanite claim box (G18): an 8 x 8 x 12 pixel post outline (registered with no collision). It is a point
 * of interest, so a Nanite Hive finds the markers within 64 blocks to list marker pairs.
 */
public class ClaimMarkerBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 12, 12);

    public ClaimMarkerBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}

package dev.autarkia.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lightning arrester (02 section 5): on the block above a node top it stops a lightning pulse at that node; next to a
 * machine it protects that machine and the pulse goes on.
 */
public class LightningArresterBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 3, 11), Block.box(7, 3, 7, 9, 14, 9));

    public LightningArresterBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}

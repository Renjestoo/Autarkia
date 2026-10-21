package dev.autarkia.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Pressure valve (02 section 6.2): on top of a boiler it vents 25 mB of steam per tick above 9 bar. */
public class PressureValveBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 4, 12), Block.box(6, 4, 6, 10, 12, 10), Block.box(3, 9, 7, 13, 11, 9));

    public PressureValveBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}

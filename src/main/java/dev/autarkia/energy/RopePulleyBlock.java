package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rope drive connector node (02 section 2.1): rope tier, 4 spans, attach point at the wheel centre. The facing points
 * away from the mount face; on a floor the wheel lies flat as a sheave.
 */
public class RopePulleyBlock extends MachineBlock implements GridConnector {
    private static final VoxelShape Y = Shapes.or(Block.box(2, 5, 2, 14, 11, 14), Block.box(6, 0, 6, 10, 16, 10));
    private static final VoxelShape X = Shapes.or(Block.box(5, 2, 2, 11, 14, 14), Block.box(0, 6, 6, 16, 10, 10));
    private static final VoxelShape Z = Shapes.or(Block.box(2, 2, 5, 14, 14, 11), Block.box(6, 6, 0, 10, 10, 16));

    public RopePulleyBlock(Properties props) {
        super(props, AutarkiaBlockEntities.ROPE_PULLEY, false);
    }

    @Override
    protected Facing facingMode() {
        return Facing.ALL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING).getAxis()) {
            case Y -> Y;
            case X -> X;
            case Z -> Z;
        };
    }

    @Override
    public boolean acceptsTier(CableTier tier) {
        return tier == CableTier.ROPE;
    }

    @Override
    public int maxSpans() {
        return 4;
    }

    @Override
    public Vec3 attachPoint(BlockPos pos, BlockState state) {
        return Vec3.atCenterOf(pos);
    }

    /** Return the facing after a wrench turn (next of the 6 directions). */
    public static Direction nextFacing(Direction d) {
        return Direction.from3DDataValue((d.get3DDataValue() + 1) % 6);
    }
}

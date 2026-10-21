package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wall insulator (02 section 2.1): a small insulated-cable node on a wall face, 2 spans, attach point 0.3 out from the
 * wall face, and spans to or from it at most 12 blocks (02 section 3). It drops when its wall is gone.
 */
public class WallInsulatorBlock extends MachineBlock implements GridConnector {
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.UP, box(5, 0, 5, 11, 7, 11));
        SHAPES.put(Direction.DOWN, box(5, 9, 5, 11, 16, 11));
        SHAPES.put(Direction.NORTH, box(5, 5, 9, 11, 11, 16));
        SHAPES.put(Direction.SOUTH, box(5, 5, 0, 11, 11, 7));
        SHAPES.put(Direction.WEST, box(9, 5, 5, 16, 11, 11));
        SHAPES.put(Direction.EAST, box(0, 5, 5, 7, 11, 11));
    }

    public WallInsulatorBlock(Properties props) {
        super(props, AutarkiaBlockEntities.WALL_INSULATOR, false);
    }

    @Override
    protected Facing facingMode() {
        return Facing.ALL;
    }

    @Override
    public boolean acceptsTier(CableTier tier) {
        return tier == CableTier.INSULATED;
    }

    @Override
    public int maxSpans() {
        return 2;
    }

    @Override
    public double spanLimit(CableTier tier) {
        return 12;
    }

    @Override
    public Vec3 attachPoint(BlockPos pos, BlockState state) {
        Direction out = state.getValue(FACING);
        return Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(out.getUnitVec3i()).scale(-0.2));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction out = state.getValue(FACING);
        BlockPos wall = pos.relative(out.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, out);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos neighbor,
                                     BlockState neighborState, RandomSource random) {
        if (dir == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, dir, neighbor, neighborState, random);
    }
}

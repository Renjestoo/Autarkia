package dev.autarkia.logistics;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Capsule station block (04 section 1.4), rendered by GeckoLib. Its {@code facing} is the mouth: the one face that
 * connects to a capsule tube. Placement points the mouth at the block clicked on when that is a tube, else at the
 * nearest tube in the look order, else up. Placing or removing a station invalidates the tube network views.
 */
public class CapsuleStationBlock extends MachineBlock {
    public CapsuleStationBlock(Properties props) {
        super(props, AutarkiaBlockEntities.CAPSULE_STATION, true);
    }

    @Override
    protected Facing facingMode() {
        return Facing.ALL;
    }

    /** Return the mouth of a station state, or null when the state is not a station. */
    public static @Nullable Direction mouthOf(BlockState state) {
        return state.getBlock() instanceof CapsuleStationBlock ? state.getValue(FACING) : null;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Direction clicked = ctx.getClickedFace().getOpposite();
        if (CapsuleTubeBlock.isTube(level.getBlockState(pos.relative(clicked)))) {
            return defaultBlockState().setValue(FACING, clicked);
        }
        for (Direction d : ctx.getNearestLookingDirections()) {
            if (CapsuleTubeBlock.isTube(level.getBlockState(pos.relative(d)))) {
                return defaultBlockState().setValue(FACING, d);
            }
        }
        return defaultBlockState().setValue(FACING, Direction.UP);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (level instanceof ServerLevel && (!old.is(this) || old.getValue(FACING) != state.getValue(FACING))) {
            CapsuleNetworks.bump(level);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
        CapsuleNetworks.bump(level);
    }
}

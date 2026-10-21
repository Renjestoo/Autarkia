package dev.autarkia.logistics;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Conveyor belt (04 section 1.3): {@code facing} and {@code slope}. The slope follows the neighbours: {@code up} when the
 * next belt of the same facing sits one block up in front, {@code down} when a belt of the same facing feeds it from one
 * block up behind, else {@code flat}. Placing or breaking a belt refreshes the slopes around it and the line cache.
 * Right-click with a stack puts up to 8 items onto the middle of the belt.
 */
public class BeltBlock extends MachineBlock {
    /** The belt slope. */
    public enum Slope implements StringRepresentable {
        FLAT, UP, DOWN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Slope> SLOPE = EnumProperty.create("slope", Slope.class);
    private static final VoxelShape FLAT_SHAPE = Block.box(0, 0, 0, 16, 5, 16);
    private static final VoxelShape RAMP_SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    public BeltBlock(Properties props) {
        super(props, AutarkiaBlockEntities.BELT, false);
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }

    @Override
    protected BlockState defaultState(BlockState state) {
        return state.setValue(SLOPE, Slope.FLAT);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SLOPE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(SLOPE) == Slope.FLAT ? FLAT_SHAPE : RAMP_SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction f = ctx.getHorizontalDirection();
        return defaultBlockState().setValue(HORIZONTAL_FACING, f).setValue(SLOPE, slopeAt(ctx.getLevel(), ctx.getClickedPos(), f));
    }

    /** Return true when the state is a belt facing f. */
    public static boolean beltFacing(BlockState state, Direction f) {
        return state.getBlock() instanceof BeltBlock && state.getValue(HORIZONTAL_FACING) == f;
    }

    /** Return the slope a belt facing f at pos takes from its neighbours. */
    public static Slope slopeAt(BlockGetter level, BlockPos pos, Direction f) {
        if (beltFacing(level.getBlockState(pos.relative(f).above()), f) && !beltFacing(level.getBlockState(pos.relative(f)), f)) {
            return Slope.UP;
        }
        BlockPos back = pos.relative(f.getOpposite());
        if (beltFacing(level.getBlockState(back.above()), f) && !beltFacing(level.getBlockState(back), f)) {
            return Slope.DOWN;
        }
        return Slope.FLAT;
    }

    /** Recompute the slope of every belt in the 3x3x3 cube around pos and invalidate the line caches. */
    public static void refreshAround(Level level, BlockPos pos) {
        BeltBlockEntity.bumpEpoch(level);
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof BeltBlock) {
                Slope s = slopeAt(level, p, st.getValue(HORIZONTAL_FACING));
                if (st.getValue(SLOPE) != s) {
                    level.setBlock(p.immutable(), st.setValue(SLOPE, s), Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (level instanceof ServerLevel && !old.is(this)) {
            refreshAround(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
        refreshAround(level, pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.getItem() instanceof BlockTool) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof BeltBlockEntity be) {
            if (!level.isClientSide()) {
                int n = Math.min(BeltBlockEntity.MAX_ITEMS, stack.getCount());
                if (be.accept(stack.copyWithCount(n), BeltBlockEntity.MIDDLE) && !player.getAbilities().instabuild) {
                    stack.shrink(n);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}

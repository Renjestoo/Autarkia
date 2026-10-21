package dev.autarkia.registry;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A one-block Autarkia block with a block entity: facing chosen by {@link #facingMode()} (the front faces the placing
 * player), optional GeckoLib rendering (the block model only gives the particle texture), menu opening and ticking
 * through the block entity.
 */
public class MachineBlock extends Block implements EntityBlock {
    /** How the block takes its facing. */
    public enum Facing { NONE, HORIZONTAL, ALL }

    public static final EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private final Supplier<? extends BlockEntityType<?>> type;
    private final boolean geo;
    private final VoxelShape shape;

    public MachineBlock(Properties props, Supplier<? extends BlockEntityType<?>> type, boolean geo, VoxelShape shape) {
        super(props);
        this.type = type;
        this.geo = geo;
        this.shape = shape;
        BlockState def = stateDefinition.any();
        if (facingMode() == Facing.HORIZONTAL) {
            def = def.setValue(HORIZONTAL_FACING, Direction.NORTH);
        } else if (facingMode() == Facing.ALL) {
            def = def.setValue(FACING, Direction.UP);
        }
        registerDefaultState(defaultState(def));
    }

    public MachineBlock(Properties props, Supplier<? extends BlockEntityType<?>> type, boolean geo) {
        this(props, type, geo, Shapes.block());
    }

    /** Return the facing mode. It is read while the block constructs, so overrides must return a constant. */
    protected Facing facingMode() {
        return Facing.NONE;
    }

    /** Return the default state after facing; subclasses set their extra property defaults here. */
    protected BlockState defaultState(BlockState state) {
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if (facingMode() == Facing.HORIZONTAL) {
            builder.add(HORIZONTAL_FACING);
        } else if (facingMode() == Facing.ALL) {
            builder.add(FACING);
        }
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return switch (facingMode()) {
            case NONE -> defaultBlockState();
            case HORIZONTAL -> defaultBlockState().setValue(HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
            case ALL -> defaultBlockState().setValue(FACING, ctx.getClickedFace());
        };
    }

    /** Return the front direction of a state (north when the block has no facing). */
    public static Direction front(BlockState state) {
        if (state.hasProperty(HORIZONTAL_FACING)) {
            return state.getValue(HORIZONTAL_FACING);
        }
        return state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        if (state.hasProperty(HORIZONTAL_FACING)) {
            return state.setValue(HORIZONTAL_FACING, rotation.rotate(state.getValue(HORIZONTAL_FACING)));
        }
        return state.hasProperty(FACING) ? state.setValue(FACING, rotation.rotate(state.getValue(FACING))) : state;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(front(state)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return geo ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        return ticker(level, t, type.get());
    }

    /** Return the shared ticker that forwards to {@link Ticking} on the center position only. */
    public static <T extends BlockEntity> @Nullable BlockEntityTicker<T> ticker(Level level, BlockEntityType<T> t, BlockEntityType<?> own) {
        if (t != own) {
            return null;
        }
        return level.isClientSide()
            ? (l, p, s, be) -> {
                if (be instanceof Ticking tk && be instanceof AutarkiaBlockEntity abe && abe.isCenter()) {
                    tk.clientTick();
                }
            }
            : (l, p, s, be) -> {
                if (be instanceof Ticking tk && be instanceof AutarkiaBlockEntity abe && abe.isCenter()) {
                    if (dev.autarkia.command.SystemProfiler.logistics(be)) {
                        long t0 = System.nanoTime();
                        tk.serverTick();
                        dev.autarkia.command.SystemProfiler.add(dev.autarkia.command.SystemProfiler.Part.LOGISTICS, t0);
                    } else {
                        tk.serverTick();
                    }
                }
            };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        return stack.getItem() instanceof BlockTool ? InteractionResult.PASS : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Marker for items whose own use on a block (spans, wrench, multimeter) takes priority over the block's use. */
    public interface BlockTool {
    }

    /** Open the menu of the block entity at pos, when it provides one. */
    public static InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            if (player instanceof ServerPlayer sp) {
                sp.openMenu(provider, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}

package dev.autarkia.logistics;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Capsule tube (04 section 1.4): a glass tube with a brass frame. It connects on all 6 faces to tubes and to stations
 * whose mouth faces it (one boolean property per face). It has a block entity for the capsule renderer and Jade, but
 * no ticker and no menu. Placing or removing a tube invalidates the tube network views.
 */
public class CapsuleTubeBlock extends MachineBlock {
    public static final Map<Direction, BooleanProperty> CONNECT = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        VoxelShape[] arms = {Block.box(5, 0, 5, 11, 5, 11), Block.box(5, 11, 5, 11, 16, 11), Block.box(5, 5, 0, 11, 11, 5),
            Block.box(5, 5, 11, 11, 11, 16), Block.box(0, 5, 5, 5, 11, 11), Block.box(11, 5, 5, 16, 11, 11)};
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape s = CORE;
            for (Direction d : Direction.values()) {
                if ((mask & 1 << d.ordinal()) != 0) {
                    s = Shapes.or(s, arms[d.ordinal()]);
                }
            }
            SHAPES[mask] = s;
        }
    }

    public CapsuleTubeBlock(Properties props) {
        super(props, AutarkiaBlockEntities.CAPSULE_TUBE, false);
    }

    /** Return true when the state is a capsule tube. */
    public static boolean isTube(BlockState state) {
        return state.getBlock() instanceof CapsuleTubeBlock;
    }

    /** Return true when a tube at pos connects toward d: a tube there, or a station whose mouth faces pos. */
    public static boolean connects(BlockGetter level, BlockPos pos, Direction d) {
        BlockState n = level.getBlockState(pos.relative(d));
        return isTube(n) || CapsuleStationBlock.mouthOf(n) == d.getOpposite();
    }

    @Override
    protected BlockState defaultState(BlockState state) {
        for (BooleanProperty p : CONNECT.values()) {
            state = state.setValue(p, false);
        }
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONNECT.values().toArray(new BooleanProperty[0]));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState();
        for (Direction d : Direction.values()) {
            s = s.setValue(CONNECT.get(d), connects(ctx.getLevel(), ctx.getClickedPos(), d));
        }
        return s;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos neighborPos,
                                     BlockState neighbor, RandomSource random) {
        return state.setValue(CONNECT.get(dir), connects(level, pos, dir));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            if (state.getValue(CONNECT.get(d))) {
                mask |= 1 << d.ordinal();
            }
        }
        return SHAPES[mask];
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        return null;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState s = state;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            s = s.setValue(CONNECT.get(rotation.rotate(d)), state.getValue(CONNECT.get(d)));
        }
        return s;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState s = state;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            s = s.setValue(CONNECT.get(mirror.mirror(d)), state.getValue(CONNECT.get(d)));
        }
        return s;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (level instanceof ServerLevel && !old.is(this)) {
            CapsuleNetworks.bump(level);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
        CapsuleNetworks.bump(level);
    }
}

package dev.autarkia.processing;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ore outcrop (03 section 2.1): a small rock pile, 12 x 6 x 12 pixels, that the {@code autarkia:outcrop_ore} feature
 * puts on the surface above an ore deposit. Property {@code ore} names the deposit ore; it can be waterlogged. It needs
 * no tool; the loot table drops 1 raw ore of its {@code ore} value (itself with silk touch).
 */
public class OreOutcropBlock extends Block implements SimpleWaterloggedBlock {
    /** The deposit ore an outcrop marks. */
    public enum Ore implements StringRepresentable {
        IRON, COPPER, GOLD, TIN, ZINC, VANTITE;

        /** Codec by serialized name (the {@code ore} field of {@code autarkia:outcrop_ore}). */
        public static final Codec<Ore> CODEC = StringRepresentable.fromEnum(Ore::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** Return the block tag {@code c:ores/<ore>} that holds the ore blocks of this deposit ore. */
        public TagKey<Block> oreTag() {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/" + getSerializedName()));
        }
    }

    /** The deposit ore of this outcrop. */
    public static final EnumProperty<Ore> ORE = EnumProperty.create("ore", Ore.class);
    /** True when the outcrop stands in a water source. */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 6, 14);

    public OreOutcropBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ORE, Ore.IRON).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORE, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).isSourceOfType(Fluids.WATER));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos neighbor,
                                     BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, dir, neighbor, neighborState, random);
    }
}

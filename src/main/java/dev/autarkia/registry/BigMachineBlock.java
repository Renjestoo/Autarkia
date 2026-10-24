package dev.autarkia.registry;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import net.nikdo53.tinymultiblocklib.block.IMultiBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.nikdo53.tinymultiblocklib.block.shape.MultiblockShape;
import net.nikdo53.tinymultiblocklib.block.shape.ShapeContext;
import net.nikdo53.tinymultiblocklib.block.shape.ShapeDataKey;
import net.nikdo53.tinymultiblocklib.blockentities.AbstractMultiBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * A Tiny Multiblock Lib big block (04 section 0.4, rules B1 to B7): one item places every position, each position has
 * the same block entity type, state lives in the center, use and menu go to the center, the front faces the player.
 * Offsets are given for facing north and rotated by the library.
 */
public class BigMachineBlock extends BaseMultiblock {
    private final Supplier<? extends BlockEntityType<?>> type;
    private final List<Vec3i> offsets;
    private final boolean geo;

    public BigMachineBlock(Properties props, Supplier<? extends BlockEntityType<?>> type, List<Vec3i> offsets, boolean geo) {
        super(props);
        this.type = type;
        this.offsets = offsets;
        this.geo = geo;
    }

    /** Return all offsets of a box footprint from min to max (blocks, relative to the center, facing north). */
    public static List<Vec3i> box(int x0, int y0, int z0, int x1, int y1, int z1) {
        List<Vec3i> list = new java.util.ArrayList<>();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    list.add(new Vec3i(x, y, z));
                }
            }
        }
        return list;
    }

    @Override
    public IMultiBlock.@Nullable DirectionContext makeDirectional() {
        return new IMultiBlock.DirectionContext(HorizontalDirectionalBlock.FACING, ctx -> ctx.getHorizontalDirection().getOpposite());
    }

    /**
     * Build the footprint; GeckoLib machines give each cell the shape voxelized from their model (MachineShapes), so the
     * outline follows the model and empty cells can be reached through. The generated cells cover the model's bounding box,
     * so a footprint with gaps (the forge chamber) uses a subset; throws when a footprint offset has no generated cell.
     */
    @Override
    public void makeMultiblockShape(MultiblockShape.Builder builder, ShapeContext context) {
        Direction dir = context.getDirection();
        builder.pushDirectionalOperation(dir);
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(this).getPath();
        java.util.Map<Vec3i, VoxelShape> shapes = geo ? MachineShapes.of(id) : null;
        if (shapes != null && !shapes.keySet().containsAll(offsets)) {
            throw new IllegalStateException("MachineShapes of " + id + " miss cells of its footprint " + offsets);
        }
        for (Vec3i o : offsets) {
            if (shapes != null) {
                builder.addNoLogic(o.getX(), o.getY(), o.getZ(), ShapeDataKey.Pair.of(ShapeDataKey.VOXEL_SHAPE, shapes.get(o)));
            } else if (o.getX() != 0 || o.getY() != 0 || o.getZ() != 0) {
                builder.addNoLogic(o.getX(), o.getY(), o.getZ());
            }
        }
    }

    @Override
    public boolean hasCustomBE() {
        return true;
    }

    @Override
    public @Nullable AbstractMultiBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return (AbstractMultiBlockEntity) type.get().create(pos, state);
    }

    @Override
    public RenderShape getMultiblockRenderShape(BlockState state, boolean isCenter) {
        return geo ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        return MachineBlock.ticker(level, t, type.get());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return MachineBlock.openMenu(level, IMultiBlock.getCenter(level, pos), player);
    }

    @Override
    protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return stack.getItem() instanceof MachineBlock.BlockTool ? InteractionResult.PASS : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Return the footprint offsets for facing north. */
    public List<Vec3i> offsets() {
        return offsets;
    }
}

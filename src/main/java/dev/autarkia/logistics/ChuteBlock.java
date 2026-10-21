package dev.autarkia.logistics;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.EnumMap;
import java.util.Map;

/** Wooden slide, high end at the back (04 section 1.2). Right-click with a stack inserts up to 4 items. */
public class ChuteBlock extends MachineBlock {
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape s = Shapes.empty();
            for (int k = 0; k < 4; k++) {
                int lo = k * 4;
                int h = 14 - k * 4;
                VoxelShape step = switch (d) {
                    case NORTH -> Block.box(0, 0, lo, 16, h, lo + 4);
                    case SOUTH -> Block.box(0, 0, 12 - lo, 16, h, 16 - lo);
                    case WEST -> Block.box(lo, 0, 0, lo + 4, h, 16);
                    default -> Block.box(12 - lo, 0, 0, 16 - lo, h, 16);
                };
                s = Shapes.or(s, step);
            }
            SHAPES.put(d, s);
        }
    }

    public ChuteBlock(Properties props) {
        super(props, AutarkiaBlockEntities.CHUTE, false);
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.get(state.getValue(HORIZONTAL_FACING).getOpposite());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.getItem() instanceof BlockTool) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof ChuteBlockEntity be) {
            if (!level.isClientSide()) {
                int n = Math.min(ChuteBlockEntity.MAX_ITEMS, stack.getCount());
                if (be.accept(stack.copyWithCount(n)) && !player.getAbilities().instabuild) {
                    stack.shrink(n);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}

package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.BigMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.nikdo53.tinymultiblocklib.block.IMultiBlock;

/** Treadwheel block, 3x3x1: only the inner floor of the bottom center collides, so the walker stands inside the ring. */
public class TreadwheelBlock extends BigMachineBlock {
    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 2, 16);

    public TreadwheelBlock(Properties props) {
        super(props, AutarkiaBlockEntities.TREADWHEEL, BigMachineBlock.box(-1, 0, 0, 1, 2, 0), true);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(CENTER) ? FLOOR : Shapes.empty();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!stack.is(Items.LEAD)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (!level.isClientSide() && level.getBlockEntity(IMultiBlock.getCenter(level, pos)) instanceof TreadwheelBlockEntity be) {
            if (!be.enter(player)) {
                player.sendOverlayMessage(Component.translatable("message.autarkia.treadwheel.refused"));
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(IMultiBlock.getCenter(level, pos)) instanceof TreadwheelBlockEntity be) {
            be.release(player);
        }
        return InteractionResult.SUCCESS;
    }
}

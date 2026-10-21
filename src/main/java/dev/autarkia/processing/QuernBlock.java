package dev.autarkia.processing;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Quern: use with an item inserts it, hold use with an empty hand to turn (03 section 4.2). */
public class QuernBlock extends MachineBlock {
    public QuernBlock(Properties props) {
        super(props, AutarkiaBlockEntities.QUERN, true, Block.box(1, 0, 1, 15, 12, 15));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.getItem() instanceof BlockTool) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof QuernBlockEntity be) {
            if (!level.isClientSide()) {
                ItemStack rest = be.insert(stack);
                if (rest.getCount() == stack.getCount()) {
                    return InteractionResult.TRY_WITH_EMPTY_HAND;
                }
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, rest);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof QuernBlockEntity be) {
            be.turn();
        }
        return InteractionResult.SUCCESS;
    }
}

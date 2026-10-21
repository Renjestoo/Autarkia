package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Hand crank: hold use to turn it (02 section 6.2). No GUI. */
public class HandCrankBlock extends MachineBlock {
    public HandCrankBlock(Properties props) {
        super(props, AutarkiaBlockEntities.HAND_CRANK, true, Block.box(1, 0, 1, 15, 14, 15));
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HandCrankBlockEntity be) {
            be.crank(player);
        }
        return InteractionResult.SUCCESS;
    }
}

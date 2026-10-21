package dev.autarkia.worker;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The {@code winding_post} block: a wooden post with a bronze ratchet; holding use on it winds a docked worker by hand. */
public class WindingPostBlock extends MachineBlock {
    public WindingPostBlock(Properties props) {
        super(props, AutarkiaBlockEntities.WINDING_POST, false, Block.box(4, 0, 4, 12, 16, 12));
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof WindingPostBlockEntity post) {
            if (!level.isClientSide()) {
                post.handWind();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}

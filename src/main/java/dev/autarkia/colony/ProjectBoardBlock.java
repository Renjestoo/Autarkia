package dev.autarkia.colony;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The {@code project_board} (05 section 4.2): a cork board inside a domain that opens the Projects tab of its colony
 * and shows the stuck-worker alerts of the board log.
 */
public class ProjectBoardBlock extends MachineBlock {
    public ProjectBoardBlock(Properties props) {
        super(props, AutarkiaBlockEntities.PROJECT_BOARD, false, Block.box(0, 0, 0, 16, 16, 16));
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Colony c = Colonies.at(level, pos);
            if (c == null || c.state != Colony.State.ACTIVE) {
                sp.sendOverlayMessage(Component.translatable("message.autarkia.colony.outside"));
            } else {
                ColonyMenu.open(sp, c, ColonyMenu.Mode.BOARD, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }
}

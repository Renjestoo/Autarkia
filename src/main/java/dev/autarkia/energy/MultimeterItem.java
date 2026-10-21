package dev.autarkia.energy;

import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.MultimeterReadingPayload;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Multimeter (02 section 9.1): use on a node sends its network reading; the client shows the reading screen. */
public class MultimeterItem extends Item implements MachineBlock.BlockTool {
    public MultimeterItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        MultimeterReadingPayload reading = Grid.reading(level, ctx.getClickedPos());
        if (reading == null) {
            player.sendOverlayMessage(Component.translatable("message.autarkia.multimeter.no_node"));
            return InteractionResult.FAIL;
        }
        AutarkiaNetwork.toPlayer(player, reading);
        return InteractionResult.SUCCESS;
    }
}

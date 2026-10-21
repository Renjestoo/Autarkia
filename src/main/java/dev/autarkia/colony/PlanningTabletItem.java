package dev.autarkia.colony;

import dev.autarkia.registry.AutarkiaComponents;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The {@code planning_tablet} (08a): a remote project board. Used inside a domain it links to that colony
 * ({@code autarkia:colony_link}); used anywhere it opens the Projects tab with the autonomy switch. While a player holds
 * it, the client keeps the ghosts of every open project of the colony around the player visible.
 */
public class PlanningTabletItem extends Item {
    public PlanningTabletItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        Colony here = Colonies.at(level, player.blockPosition());
        if (here != null && here.canManage(player.getUUID())) {
            stack.set(AutarkiaComponents.COLONY_LINK.get(), here.id);
        }
        UUID link = stack.get(AutarkiaComponents.COLONY_LINK.get());
        Colony c = link == null ? null : Colonies.registry().get(link);
        if (c == null || c.state != Colony.State.ACTIVE) {
            sp.sendOverlayMessage(Component.translatable("message.autarkia.tablet.unlinked"));
            return InteractionResult.FAIL;
        }
        ColonyMenu.open(sp, c, ColonyMenu.Mode.TABLET, player.blockPosition());
        return InteractionResult.SUCCESS;
    }
}

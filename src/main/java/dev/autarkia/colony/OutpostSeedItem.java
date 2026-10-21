package dev.autarkia.colony;

import dev.autarkia.registry.AutarkiaComponents;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.jspecify.annotations.Nullable;

/**
 * {@code outpost_seed} (08a): founds an outpost (05 section 8.1). Workers normally carry it (FOUND_OUTPOST); a player
 * who manages the colony can also plant it on the top face of a block. The seed belongs to the colony in its
 * {@code colony_link}, else to the colony of the player nearest to the site.
 */
public class OutpostSeedItem extends Item {
    public OutpostSeedItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos site = ctx.getClickedPos().above();
        Colony c = colony(ctx.getItemInHand().get(AutarkiaComponents.COLONY_LINK.get()), sp, site);
        if (c == null) {
            sp.sendOverlayMessage(Component.translatable("message.autarkia.outpost.no_colony"));
            return InteractionResult.FAIL;
        }
        String why = Outposts.invalid(level, c, site);
        if (why != null) {
            sp.sendOverlayMessage(Component.translatable(why));
            return InteractionResult.FAIL;
        }
        if (!Outposts.plant(level, c, site, sp)) {
            sp.sendOverlayMessage(Component.translatable("message.autarkia.outpost.blocked"));
            return InteractionResult.FAIL;
        }
        ctx.getItemInHand().consume(1, sp);
        return InteractionResult.SUCCESS;
    }

    private static @Nullable Colony colony(@Nullable UUID link, ServerPlayer sp, BlockPos site) {
        if (link != null) {
            Colony c = Colonies.registry().get(link);
            return c != null && c.canManage(sp.getUUID()) ? c : null;
        }
        Colony best = null;
        for (Colony c : Colonies.registry().all()) {
            if (c.dimension.equals(sp.level().dimension()) && c.canManage(sp.getUUID())
                && (best == null || Outposts.horizontal(c.corePos, site) < Outposts.horizontal(best.corePos, site))) {
                best = c;
            }
        }
        return best;
    }
}

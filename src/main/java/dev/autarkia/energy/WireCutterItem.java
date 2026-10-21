package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.MachineBlock;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * {@code wire_cutter} (02 section 3 rule 3): click one node, then the other; the span between them is removed and its
 * full item cost is returned. Sneak-use clears the first click. Each cut costs 1 durability.
 */
public class WireCutterItem extends Item implements MachineBlock.BlockTool {
    public WireCutterItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || ctx.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (player.isShiftKeyDown() && stack.has(AutarkiaComponents.LINK_START.get())) {
            stack.remove(AutarkiaComponents.LINK_START.get());
            player.sendOverlayMessage(Component.translatable("message.autarkia.span.cleared"));
            return InteractionResult.SUCCESS;
        }
        Grid.Node node = Grid.resolve(level, ctx.getClickedPos());
        if (node == null) {
            player.sendOverlayMessage(Component.translatable("message.autarkia.cutter.not_a_node"));
            return InteractionResult.FAIL;
        }
        AutarkiaComponents.LinkStart start = stack.get(AutarkiaComponents.LINK_START.get());
        long now = level.getGameTime();
        if (start == null || !start.node().dimension().equals(level.dimension()) || now - start.time() > CableItem.LINK_TIMEOUT) {
            stack.set(AutarkiaComponents.LINK_START.get(), new AutarkiaComponents.LinkStart(GlobalPos.of(level.dimension(), node.pos()), ctx.getClickedFace(), now));
            player.sendOverlayMessage(Component.translatable("message.autarkia.cutter.start"));
            return InteractionResult.SUCCESS;
        }
        stack.remove(AutarkiaComponents.LINK_START.get());
        BlockPos a = start.node().pos();
        GridLevel g = Grid.of(level);
        IntArrayList ids = g.data().at(a.asLong());
        Span found = null;
        for (int i = 0; i < ids.size() && found == null; i++) {
            Span s = g.data().spans.get(ids.getInt(i));
            if (s != null && s.joins(a.asLong(), node.pos().asLong())) {
                found = s;
            }
        }
        if (found == null) {
            player.sendOverlayMessage(Component.translatable("message.autarkia.cutter.no_span"));
            return InteractionResult.FAIL;
        }
        CableTier tier = found.tier;
        int cost = tier.itemCost(found.length);
        Grid.removeSpan(level, a, node.pos());
        if (!player.getAbilities().instabuild) {
            ItemStack back = new ItemStack(Grid.cableItem(tier), cost);
            if (!player.getInventory().add(back)) {
                net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), back);
            }
        }
        stack.hurtAndBreak(1, player, ctx.getHand());
        player.sendOverlayMessage(Component.translatable("message.autarkia.cutter.cut", cost));
        return InteractionResult.SUCCESS;
    }
}

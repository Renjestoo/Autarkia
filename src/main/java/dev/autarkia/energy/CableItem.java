package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cable item (02 section 3): click the first node, then the second. The first click is kept per player on the server
 * for 600 ticks, not on the stack, so a coil in progress still stacks with dropped coils; survival pays the item cost of
 * the span. Sneak-use clears the start. Stacks that still carry the older {@code autarkia:link_start} lose it.
 */
public class CableItem extends Item implements MachineBlock.BlockTool {
    public static final int LINK_TIMEOUT = 600;
    private final CableTier tier;
    private final Map<UUID, AutarkiaComponents.LinkStart> starts = new HashMap<>();

    public CableItem(Properties props, CableTier tier) {
        super(props);
        this.tier = tier;
    }

    /** Return the cable tier this item places. */
    public CableTier tier() {
        return tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || ctx.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        BlockPos clicked = ctx.getClickedPos();
        if (player.isShiftKeyDown() && starts.remove(player.getUUID()) != null) {
            player.sendOverlayMessage(Component.translatable("message.autarkia.span.cleared"));
            return InteractionResult.SUCCESS;
        }
        Grid.Node node = Grid.resolve(level, clicked);
        if (node == null) {
            player.sendOverlayMessage(Grid.SpanResult.NOT_A_NODE.message(tier));
            return InteractionResult.FAIL;
        }
        AutarkiaComponents.LinkStart start = starts.get(player.getUUID());
        long now = level.getGameTime();
        if (start == null || !start.node().dimension().equals(level.dimension()) || now < start.time() || now - start.time() > LINK_TIMEOUT) {
            if (node.connector() != null && !node.connector().acceptsTier(tier)) {
                player.sendOverlayMessage(Grid.SpanResult.WRONG_TIER.message(tier));
                return InteractionResult.FAIL;
            }
            starts.put(player.getUUID(), new AutarkiaComponents.LinkStart(GlobalPos.of(level.dimension(), clicked), ctx.getClickedFace(), now));
            player.sendOverlayMessage(Component.translatable("message.autarkia.span.start"));
            return InteractionResult.SUCCESS;
        }
        Grid.SpanResult check = Grid.addSpan(level, start.node().pos(), start.face(), clicked, ctx.getClickedFace(), tier, true);
        if (check != Grid.SpanResult.OK) {
            player.sendOverlayMessage(check.message(tier));
            return InteractionResult.FAIL;
        }
        Grid.Node a = Grid.resolve(level, start.node().pos());
        double length = Grid.attachPoint(a, start.node().pos(), start.face()).distanceTo(Grid.attachPoint(node, clicked, ctx.getClickedFace()));
        int cost = tier.itemCost(length);
        if (!player.getAbilities().instabuild && countCable(player) < cost) {
            player.sendOverlayMessage(Component.translatable("message.autarkia.span.needs_items", cost, getName(stack)));
            return InteractionResult.FAIL;
        }
        Grid.addSpan(level, start.node().pos(), start.face(), clicked, ctx.getClickedFace(), tier, false);
        starts.remove(player.getUUID());
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            dev.autarkia.registry.AutarkiaAdvancements.award(sp, "power/first_span");
            dev.autarkia.registry.AutarkiaAdvancements.award(sp, "guide/first_span");
        }
        if (!player.getAbilities().instabuild) {
            consumeCable(player, cost);
        }
        player.sendOverlayMessage(Component.translatable("message.autarkia.span.placed", String.format(java.util.Locale.ROOT, "%.1f", length), cost));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (stack.has(AutarkiaComponents.LINK_START.get())) {
            stack.remove(AutarkiaComponents.LINK_START.get());
        }
    }

    private int countCable(Player player) {
        int n = 0;
        for (ItemStack s : player.getInventory()) {
            if (s.is(this)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private void consumeCable(Player player, int cost) {
        for (ItemStack s : player.getInventory()) {
            if (cost <= 0) {
                return;
            }
            if (s.is(this)) {
                int take = Math.min(cost, s.getCount());
                s.shrink(take);
                cost -= take;
            }
        }
    }
}

package dev.autarkia.colony;

import dev.autarkia.registry.MachineBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.nikdo53.tinymultiblocklib.block.IMultiBlock;

/**
 * A Core upgrade item {@code core_upgrade_tN} (05 section 1.2): used by the owner or a trusted player on an active Core of
 * tier N - 1 it is consumed and the Core plays its 100-tick upgrade, then the colony has tier N.
 */
public class CoreUpgradeItem extends Item implements MachineBlock.BlockTool {
    private final int tier;

    public CoreUpgradeItem(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    /** Return the tier this item raises the Core to. */
    public int tier() {
        return tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel().getBlockEntity(IMultiBlock.getCenter(ctx.getLevel(), ctx.getClickedPos())) instanceof ColonyCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }
        if (!(ctx.getPlayer() instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        Colony c = core.colony();
        if (c == null || c.state != Colony.State.ACTIVE) {
            sp.sendOverlayMessage(Component.translatable("message.autarkia.core_upgrade.inactive"));
            return InteractionResult.FAIL;
        }
        if (!c.canManage(sp.getUUID())) {
            sp.sendOverlayMessage(Component.translatable("message.autarkia.colony.no_rights"));
            return InteractionResult.FAIL;
        }
        if (core.upgrading()) {
            sp.sendOverlayMessage(Component.translatable("message.autarkia.core_upgrade.busy"));
            return InteractionResult.FAIL;
        }
        if (c.tier != tier - 1) {
            sp.sendOverlayMessage(Component.translatable("message.autarkia.core_upgrade.wrong_tier", tier - 1, c.tier));
            return InteractionResult.FAIL;
        }
        core.startUpgrade(tier);
        ctx.getItemInHand().consume(1, sp);
        return InteractionResult.SUCCESS;
    }
}

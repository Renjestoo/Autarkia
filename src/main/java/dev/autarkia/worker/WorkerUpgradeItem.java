package dev.autarkia.worker;

import dev.autarkia.registry.AutarkiaItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A worker upgrade item (06 section 5.4): use it on a worker to install it; the item is used up. */
public class WorkerUpgradeItem extends Item {
    private final WorkerUpgrades.Kind kind;

    public WorkerUpgradeItem(Properties props, WorkerUpgrades.Kind kind) {
        super(props);
        this.kind = kind;
    }

    /** Return the item of an upgrade kind. */
    public static Item item(WorkerUpgrades.Kind kind) {
        return switch (kind) {
            case SPEED -> AutarkiaItems.WORKER_UPGRADE_SPEED.get();
            case CARGO -> AutarkiaItems.WORKER_UPGRADE_CARGO.get();
            case ARMOR -> AutarkiaItems.WORKER_UPGRADE_ARMOR.get();
            case RANGE -> AutarkiaItems.WORKER_UPGRADE_RANGE.get();
        };
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof WorkerEntity w)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (w.colony() != null && !w.colony().canManage(player.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.autarkia.upgrade.no_rights"));
            return InteractionResult.FAIL;
        }
        if (!w.upgrades.install(kind)) {
            player.sendOverlayMessage(Component.translatable("message.autarkia.upgrade.full"));
            return InteractionResult.FAIL;
        }
        Component label = stack.getHoverName();
        stack.consume(1, player);
        w.sound("entity.worker.servo", 0.5f, 1.4f);
        player.sendOverlayMessage(Component.translatable("message.autarkia.upgrade.installed", label));
        return InteractionResult.SUCCESS;
    }
}

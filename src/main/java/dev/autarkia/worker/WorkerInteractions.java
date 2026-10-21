package dev.autarkia.worker;

import dev.autarkia.registry.AutarkiaItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Player use on a worker (06 sections 1.2, 5.4 and 5.6): an empty hand winds a spring worker by 100 T and wakes it; the
 * repair item heals it; a sneaking wrench takes all upgrades out as items.
 */
public final class WorkerInteractions {
    private WorkerInteractions() {
    }

    /** Handle the use on the server; return PASS when nothing applied. */
    public static InteractionResult interact(WorkerEntity w, Player player, ItemStack stack) {
        if (stack.isEmpty() && !w.workerType.fe() && w.energy() < w.workerType.capacity()) {
            w.setEnergy(w.energy() + 100);
            w.recover();
            w.sound("entity.worker.wind", 0.5f, 1.8f);
            return InteractionResult.SUCCESS;
        }
        if (stack.is(w.workerType.repairItem().get()) && w.getHealth() < w.getMaxHealth()) {
            w.heal(w.workerType.repairHealth());
            stack.consume(1, player);
            w.sound("entity.worker.servo", 0.4f, 1.9f);
            return InteractionResult.SUCCESS;
        }
        if (stack.is(AutarkiaItems.WRENCH.get()) && player.isShiftKeyDown()) {
            if (w.colony() != null && !w.colony().canManage(player.getUUID())) {
                player.sendOverlayMessage(Component.translatable("message.autarkia.upgrade.no_rights"));
                return InteractionResult.FAIL;
            }
            var removed = w.upgrades.removeAll();
            for (ItemStack s : removed) {
                player.getInventory().placeItemBackInInventory(s, net.minecraft.util.Prediction.SERVER_ONLY);
            }
            player.sendOverlayMessage(Component.translatable("message.autarkia.upgrade.removed", removed.size()));
            return removed.isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}

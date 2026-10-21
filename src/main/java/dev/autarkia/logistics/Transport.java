package dev.autarkia.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Hand-over of one transit entry into the block ahead (04 rule C5: the whole entry in one transaction, or nothing). */
public final class Transport {
    private Transport() {
    }

    /**
     * Deliver a stack that travels in direction dir into the block at target: a belt (side-load at its middle), a
     * splitter or sorting chute entered from its back, a chute (entries of up to 4 items), or an item handler on the
     * face the stack enters. Returns true when the whole stack moved.
     */
    public static boolean deliver(ServerLevel sl, BlockPos target, Direction dir, ItemStack stack) {
        if (sl.getBlockEntity(target) instanceof BeltBlockEntity belt) {
            return belt.accept(stack, BeltBlockEntity.MIDDLE);
        }
        if (sl.getBlockEntity(target) instanceof FilterBlockEntity filter) {
            return filter.facing() == dir && filter.accept(stack);
        }
        if (sl.getBlockEntity(target) instanceof ChuteBlockEntity chute) {
            return chute.accept(stack);
        }
        ResourceHandler<ItemResource> h = sl.getCapability(Capabilities.Item.BLOCK, target, dir.getOpposite());
        if (h == null) {
            return false;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int moved = h.insert(ItemResource.of(stack), stack.getCount(), tx);
            if (moved == stack.getCount()) {
                tx.commit();
                return true;
            }
        }
        return false;
    }
}

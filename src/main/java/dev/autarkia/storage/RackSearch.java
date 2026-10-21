package dev.autarkia.storage;

import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

/** Searches the racks of a warehouse for a put-away crate and for a free crate position (04 section 2.5). */
final class RackSearch {
    private RackSearch() {
    }

    /**
     * Return the crate for a put-away from a terminal: a crate that holds the first input item and has space, else a
     * crate with free slots, else the empty crate from the terminal crate slot (index -1) when a rack position is free;
     * null when none. Crates that taken accepts (reserved by other requests) are skipped.
     */
    static LedgerRequest.@Nullable Reservation storeCrate(ServerLevel sl, List<BlockPos> racks, BiPredicate<BlockPos, Integer> taken, BlockPos terminal) {
        if (!(sl.getBlockEntity(terminal) instanceof LedgerTerminalBlockEntity term)) {
            return null;
        }
        ItemStack first = term.firstInput();
        LedgerRequest.Reservation spare = null;
        for (BlockPos p : racks) {
            if (!(sl.getBlockEntity(p) instanceof RackBlockEntity rack)) {
                continue;
            }
            for (int i = 0; i < rack.positionCount(); i++) {
                if (!rack.present(i) || taken.test(p, i)) {
                    continue;
                }
                ItemStacksResourceHandler h = rack.position(i);
                boolean free = false;
                for (int s = 0; s < h.size(); s++) {
                    int a = h.getAmountAsInt(s);
                    if (a == 0) {
                        free = true;
                    } else if (!first.isEmpty() && h.getResource(s).equals(ItemResource.of(first)) && a < first.getMaxStackSize()) {
                        return new LedgerRequest.Reservation(p, i, 0);
                    }
                }
                if (free && spare == null) {
                    spare = new LedgerRequest.Reservation(p, i, 0);
                }
            }
        }
        if (spare != null) {
            return spare;
        }
        return !term.crateSlot().isEmpty() && freePosition(sl, racks, terminal) != null ? new LedgerRequest.Reservation(terminal, -1, 0) : null;
    }

    /** Return the empty crate position of the racks nearest a point, or null. */
    static LedgerRequest.@Nullable Reservation freePosition(ServerLevel sl, List<BlockPos> racks, BlockPos near) {
        LedgerRequest.Reservation best = null;
        for (BlockPos p : racks) {
            if (sl.getBlockEntity(p) instanceof RackBlockEntity rack) {
                for (int i = 0; i < rack.positionCount(); i++) {
                    if (!rack.present(i) && (best == null || p.distSqr(near) < best.rack().distSqr(near))) {
                        best = new LedgerRequest.Reservation(p, i, 0);
                    }
                }
            }
        }
        return best;
    }
}

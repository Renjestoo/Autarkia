package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * The colony deposit list (05 section 2.2 S16): ore deposits marked by their surface {@code ore_outcrop}, reported by
 * Surveyor scans. A report within 8 blocks of a known deposit of the same ore updates its estimated count.
 */
public final class Deposits {
    private Deposits() {
    }

    /** Add or update a deposit; returns true when it is new. */
    public static boolean report(Colony c, ServerLevel level, BlockPos outcrop, String ore, int estCount) {
        for (int i = 0; i < c.deposits.size(); i++) {
            Colony.Deposit d = c.deposits.get(i);
            if (d.ore().equals(ore) && d.pos().distSqr(outcrop) <= 64) {
                if (d.estCount() != estCount) {
                    c.deposits.set(i, d.withCount(estCount));
                    Colonies.changed();
                }
                return false;
            }
        }
        c.deposits.add(new Colony.Deposit(UUID.randomUUID(), ore, outcrop.immutable(), estCount, c.clock, Optional.empty()));
        Colonies.changed();
        Autarkia.LOG.info("[colony] {} found a {} deposit at {} (about {} ore blocks)", c.name, ore, outcrop.toShortString(), estCount);
        ColonyRuntime.of(c).log("Deposit found: " + ore + " at " + outcrop.toShortString());
        return true;
    }

    /** Return the horizontal distance from pos to the Core or the nearest outpost core of the colony. */
    public static double distance(Colony c, BlockPos pos) {
        double best = Math.sqrt(sq(c.corePos, pos));
        for (Colony.Outpost o : c.outposts) {
            best = Math.min(best, Math.sqrt(sq(o.corePos, pos)));
        }
        return best;
    }

    private static double sq(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    /** Return the deposit with the id, or null. */
    public static Colony.@Nullable Deposit byId(Colony c, UUID id) {
        for (Colony.Deposit d : c.deposits) {
            if (d.id().equals(id)) {
                return d;
            }
        }
        return null;
    }

    /** Lower the estimated ore count of a deposit (outpost catch-up mining). */
    public static void mine(Colony c, UUID id, int blocks) {
        for (int i = 0; i < c.deposits.size(); i++) {
            Colony.Deposit d = c.deposits.get(i);
            if (d.id().equals(id)) {
                c.deposits.set(i, d.withCount(Math.max(0, d.estCount() - blocks)));
                Colonies.changed();
                return;
            }
        }
    }
}

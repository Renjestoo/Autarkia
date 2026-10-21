package dev.autarkia.colony;

import dev.autarkia.config.AutarkiaConfig;
import net.minecraft.server.level.ServerLevel;

/**
 * Colony clock and daily budget (05 section 3.7). Each colony tick adds the overworld clock delta since the last tick
 * (0 when the clock went back, at most 24,000), so sleep and up to one day of unloaded time count. A new colony day
 * refills the budget: {@code remaining = D * multiplier + min(remaining, 0.5 * D)}, which also carries a debt.
 */
public final class ColonyClock {
    public static final long DAY = 24000;

    private ColonyClock() {
    }

    /** Advance the colony clock from the overworld clock. */
    public static void tick(Colony c, ServerLevel level) {
        long now = level.getOverworldClockTime();
        if (c.lastClock < 0) {
            c.lastClock = now;
            rollDay(c);
            return;
        }
        long delta = Math.min(DAY, Math.max(0, now - c.lastClock));
        c.lastClock = now;
        if (delta > 0) {
            c.clock += delta;
            rollDay(c);
        }
    }

    /** Add ticks to the colony clock (debug {@code /autarkia clock skip}). */
    public static void skip(Colony c, long ticks) {
        c.clock += Math.max(0, ticks);
        rollDay(c);
        Colonies.changed();
    }

    /** Return the colony day index. */
    public static long day(Colony c) {
        return Math.floorDiv(c.clock, DAY);
    }

    private static void rollDay(Colony c) {
        long day = day(c);
        if (day == c.budgetDay) {
            return;
        }
        double d = ColonyTiers.dailyBudget(c.tier);
        double fresh = d * AutarkiaConfig.COLONY_BUDGET_MULTIPLIER.get();
        c.budgetRemaining = c.budgetDay < 0 ? fresh : fresh + Math.min(c.budgetRemaining, 0.5 * d);
        c.budgetDay = day;
        Colonies.changed();
    }

    /** Spend budget for an auto-approved project start; the debt is bounded at -D. */
    public static void spend(Colony c, double cost) {
        c.budgetRemaining = Math.max(-ColonyTiers.dailyBudget(c.tier), c.budgetRemaining - cost);
        Colonies.changed();
    }

    /** Return whether an auto-approved project of the given cost may start (05 section 3.7 start rule). */
    public static boolean canAutoStart(Colony c, double cost) {
        double d = ColonyTiers.dailyBudget(c.tier);
        if (c.budgetRemaining < 0) {
            return false;
        }
        return c.budgetRemaining >= cost || (cost > d && c.budgetRemaining >= d);
    }
}

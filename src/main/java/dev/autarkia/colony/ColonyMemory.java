package dev.autarkia.colony;

import dev.autarkia.config.AutarkiaConfig;
import net.minecraft.core.BlockPos;

/**
 * Failure memory entries (05 section 6.1): an event with a known key sets {@code w = decayed(w) + 1} (max 5) and
 * {@code t0 = now}; a new key starts at 1; weights halve every {@code memory.halfLifeTicks} on the colony clock;
 * entries below 0.1 go; at the entry cap the lowest current weight goes.
 */
public final class ColonyMemory {
    private ColonyMemory() {
    }

    /** Record a failure event. */
    public static void event(Colony c, String type, String key, BlockPos pos) {
        double half = AutarkiaConfig.MEMORY_HALF_LIFE_TICKS.get();
        for (int i = 0; i < c.memories.size(); i++) {
            Colony.Memory m = c.memories.get(i);
            if (m.type().equals(type) && m.key().equals(key)) {
                c.memories.set(i, new Colony.Memory(type, key, pos, Math.min(5, m.weightAt(c.clock, half) + 1), c.clock));
                Colonies.changed();
                return;
            }
        }
        prune(c);
        if (c.memories.size() >= AutarkiaConfig.MEMORY_MAX_ENTRIES.get()) {
            Colony.Memory low = c.memories.stream().min(java.util.Comparator.comparingDouble(m -> m.weightAt(c.clock, half))).orElseThrow();
            c.memories.remove(low);
        }
        c.memories.add(new Colony.Memory(type, key, pos, 1, c.clock));
        Colonies.changed();
    }

    /** Remove entries whose current weight is below 0.1. */
    public static void prune(Colony c) {
        double half = AutarkiaConfig.MEMORY_HALF_LIFE_TICKS.get();
        if (c.memories.removeIf(m -> m.weightAt(c.clock, half) < 0.1)) {
            Colonies.changed();
        }
    }
}

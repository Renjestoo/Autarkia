package dev.autarkia.worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Stuck detection and the unreachable-target cache of one worker (06 sections 3.8 and 6.2): a worker that wants to move
 * but stays within half a block over 6 samples is stuck, one level more per 100 ticks up to 4; targets that failed
 * NO_PATH stay unreachable for 600 ticks.
 */
final class WorkerStuck {
    private final List<BlockPos> trail = new ArrayList<>();
    private final Map<Long, Long> unreachable = new HashMap<>();
    private int level;
    private long since = -1;

    /** Sample the worker's position and update its {@code stuck_level} memory. */
    void sample(WorkerEntity w) {
        boolean wantsMove = w.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET) && w.action() == WorkerEntity.ACTION_NONE
            && w.state() != WorkerEntity.State.RECHARGING;
        trail.add(w.blockPosition());
        while (trail.size() > 6) {
            trail.remove(0);
        }
        if (!wantsMove || trail.size() < 6 || trail.get(0).distSqr(w.blockPosition()) >= 0.25) {
            since = wantsMove ? since : -1;
            if (!wantsMove || trail.size() >= 6 && trail.get(0).distSqr(w.blockPosition()) >= 0.25) {
                clear(w);
            }
            return;
        }
        long now = w.level().getGameTime();
        if (since < 0) {
            since = now;
        }
        int next = (int) Math.min(4, 1 + (now - since) / 100);
        if (next != level) {
            level = next;
            w.getBrain().setMemory(WorkerMemories.STUCK_LEVEL.get(), next);
        }
    }

    /** Clear the stuck level (memory included). */
    void clear(WorkerEntity w) {
        level = 0;
        w.getBrain().eraseMemory(WorkerMemories.STUCK_LEVEL.get());
    }

    /** Reset the trail and the stuck level (after a recovery action). */
    void reset(WorkerEntity w) {
        trail.clear();
        since = -1;
        clear(w);
    }

    /** Remember an unreachable target until the given game time. */
    void markUnreachable(BlockPos p, long until) {
        unreachable.put(p.asLong(), until);
    }

    /** Return whether a target is in the unreachable cache at the given game time. */
    boolean unreachable(BlockPos p, long now) {
        Long until = unreachable.get(p.asLong());
        return until != null && until > now;
    }
}

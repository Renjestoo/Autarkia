package dev.autarkia.colony;

import dev.autarkia.energy.CableTier;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A colony job (05 section 7): what to do where, with which item, its priority and phase, and its board state
 * (open, assigned, done, failed with backoff, blocked after the retry limit). Jobs are never saved.
 */
public final class ColonyJob {
    /** Job types of 05 section 7.2 that the board creates in this tier. */
    public enum Type { FETCH, CARRY, DELIVER, LEVEL_CUT, LEVEL_FILL, PLACE, STRING_CABLE, CRANK, SIGN, COLLECT_DEBRIS, WAREHOUSE_DELIVER, WAREHOUSE_STORE,
        CUT_SPAN }

    /** Board state of 05 section 7.6. */
    public enum Status { OPEN, ASSIGNED, DONE, FAILED, BLOCKED }

    /** Failure reasons of 05 section 7.3. */
    public enum Failure { NO_PATH, ITEM_MISSING, TARGET_OBSTRUCTED, WORKER_LOST, CHUNK_UNLOADED, NO_POWER, TIMEOUT }

    public final UUID id = UUID.randomUUID();
    public final Type type;
    public final @Nullable UUID projectId;
    public final BlockPos target;
    public final @Nullable BlockState state;
    public final @Nullable Item item;
    public final int count;
    public final double priority;
    public final int phase;
    public final long created;
    public final @Nullable BlockPos nodeB;
    public final @Nullable CableTier tier;
    public final int duration;
    public Status status = Status.OPEN;
    public int attempts;
    public long backoffUntil;
    public @Nullable UUID assignee;
    public long leaseUntil;
    public String blockReason = "";
    public @Nullable UUID requestId;

    public ColonyJob(Type type, @Nullable UUID projectId, BlockPos target, @Nullable BlockState state, @Nullable Item item, int count, double priority,
                     int phase, long created, @Nullable BlockPos nodeB, @Nullable CableTier tier, int duration) {
        this.type = type;
        this.projectId = projectId;
        this.target = target.immutable();
        this.state = state;
        this.item = item;
        this.count = count;
        this.priority = priority;
        this.phase = phase;
        this.created = created;
        this.nodeB = nodeB == null ? null : nodeB.immutable();
        this.tier = tier;
        this.duration = duration;
    }

    /** Return the timeout of the job type in ticks (05 section 7.2). */
    public int timeout() {
        return switch (type) {
            case LEVEL_CUT, LEVEL_FILL -> 200;
            case PLACE, SIGN -> 400;
            case CRANK -> Math.max(2400, duration + 600);
            case WAREHOUSE_DELIVER, WAREHOUSE_STORE -> 2400;
            default -> 1200;
        };
    }
}

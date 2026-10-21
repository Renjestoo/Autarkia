package dev.autarkia.colony;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * The worker side of the job interface (05 section 7.3): the board pushes a {@link WorkOrder} to an idle, capable,
 * charged worker; the worker reports through {@link JobBoard#done}, {@link JobBoard#failed} and
 * {@link JobBoard#progress} (lease renewal).
 */
public interface ColonyWorker {
    /** A work order: an optional FETCH followed by jobs of one kind, in nearest-neighbor order. */
    record WorkOrder(UUID id, List<ColonyJob> jobs) {
    }

    /** Return the worker UUID. */
    UUID workerId();

    /** Return whether the worker accepts the job (type, tier, item and size rules of 06 section 1.3). */
    boolean accepts(ColonyJob job);

    /** Return the charge fraction 0 to 1. */
    float charge();

    /** Return the position the worker works from (home when idle). */
    BlockPos workPos();

    /** Return whether the worker has no order and is not recharging or failed. */
    boolean isIdle();

    /** Return the number of cargo stacks the worker carries. */
    int cargoStacks();

    /** Start a work order. */
    void assign(WorkOrder order);

    /** Stop the current order for the reason (the board already released it). */
    void abort(ColonyJob.Failure reason);
}

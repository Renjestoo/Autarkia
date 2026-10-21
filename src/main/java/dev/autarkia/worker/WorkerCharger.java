package dev.autarkia.worker;

import java.util.UUID;
import net.minecraft.core.BlockPos;

/** A block that recharges one docked worker at a time (06 section 5.7): the winding post (spring) or the recharge dock (FE). */
public interface WorkerCharger {
    /** Return true when the charger can charge this worker's energy kind. */
    boolean serves(WorkerEntity w);

    /** Return whether a worker may use the charger (free, or already docked there). */
    boolean availableFor(UUID worker);

    /** Lock the charger to the worker; return false when another worker holds it. */
    boolean dock(WorkerEntity w);

    /** Release the lock of the worker. */
    void undock(WorkerEntity w);

    /** Return the block position the worker moves to for docking. */
    BlockPos dockSpot(WorkerEntity w);

    /** Return how close (blocks, straight line) the worker must be to the dock spot center to dock. */
    default double dockReach() {
        return 1.2;
    }
}

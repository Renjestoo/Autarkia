package dev.autarkia.worker;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The {@code Capabilities.Energy.ENTITY} view of an FE worker's battery (06 section 1.2): insert up to the dock charge
 * rate per call, extract nothing. Changes join the transaction and are applied to the worker on the root commit.
 */
public final class WorkerEnergyHandler extends SnapshotJournal<Integer> implements EnergyHandler {
    private final WorkerEntity worker;
    private final int maxInsert;
    private int pending = -1;

    public WorkerEnergyHandler(WorkerEntity worker, int maxInsert) {
        this.worker = worker;
        this.maxInsert = maxInsert;
    }

    private int current() {
        return pending >= 0 ? pending : worker.energy();
    }

    @Override
    public long getAmountAsLong() {
        return current();
    }

    @Override
    public long getCapacityAsLong() {
        return worker.workerType.capacity();
    }

    @Override
    public int insert(int amount, TransactionContext tx) {
        int add = Math.max(0, Math.min(Math.min(amount, maxInsert), worker.workerType.capacity() - current()));
        if (add > 0) {
            updateSnapshots(tx);
            pending = current() + add;
        }
        return add;
    }

    @Override
    public int extract(int amount, TransactionContext tx) {
        return 0;
    }

    @Override
    protected Integer createSnapshot() {
        return pending;
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
        pending = snapshot;
    }

    @Override
    protected void onRootCommit(Integer original) {
        if (pending >= 0) {
            worker.setEnergy(pending);
            pending = -1;
        }
    }
}

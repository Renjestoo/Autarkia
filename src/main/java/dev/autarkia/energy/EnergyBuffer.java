package dev.autarkia.energy;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * FE buffer of an Autarkia block entity. The grid reads and writes it directly (no transaction); other mods
 * reach it through Capabilities.Energy.BLOCK with the insert and extract limits given here.
 */
public class EnergyBuffer extends SimpleEnergyHandler {
    private final Runnable onChange;

    public EnergyBuffer(int capacity, int maxInsert, int maxExtract, Runnable onChange) {
        super(capacity, maxInsert, maxExtract);
        this.onChange = onChange;
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        onChange.run();
    }

    /** Return the stored FE. */
    public int stored() {
        return energy;
    }

    /** Return the capacity in FE. */
    public int capacity() {
        return capacity;
    }

    /** Return the free room in FE. */
    public int room() {
        return capacity - energy;
    }

    /** Add up to amount FE without rate limit; return the amount added. */
    public int fill(int amount) {
        int add = Math.max(0, Math.min(amount, capacity - energy));
        if (add > 0) {
            set(energy + add);
        }
        return add;
    }

    /** Remove up to amount FE without rate limit; return the amount removed. */
    public int drain(int amount) {
        int take = Math.max(0, Math.min(amount, energy));
        if (take > 0) {
            set(energy - take);
        }
        return take;
    }

    /** Change the capacity (for example after a tier change); excess energy is kept until drained. */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /** Return the fill ratio 0..1. */
    public float fillRatio() {
        return capacity == 0 ? 0 : energy / (float) capacity;
    }
}

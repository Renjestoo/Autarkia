package dev.autarkia.energy;

import net.minecraft.world.entity.Entity;

/** A generator a worker can drive (02 section 2.7): the {@code hand_crank} and the {@code treadwheel}. */
public interface MuscleGenerator {
    /** Mark the worker as driving for the current tick (call every tick while driving). */
    void setWorkerDriving(Entity worker, boolean driving);

    /** Return the FE/t the generator makes while a worker drives it (02 section 6.2). */
    int workerOutput();

    /** Return whether the generator's buffer is full (the worker stops cranking). */
    boolean storageFull();
}

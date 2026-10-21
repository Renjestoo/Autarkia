package dev.autarkia.energy;

/** A connector node block entity that collects FE inserted by other mods' generators into the network injection pool. */
public interface GridInjector {
    /** Take up to max FE from the pool collected since the last tick; the rest is discarded. */
    int takeInjected(int max);
}

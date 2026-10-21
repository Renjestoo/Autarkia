package dev.autarkia.registry;

/** A block entity that runs per-tick logic; the block ticker calls it only on the center position. */
public interface Ticking {
    /** Run one server tick. */
    void serverTick();

    /** Run one client tick (animation state, particles). */
    default void clientTick() {
    }
}

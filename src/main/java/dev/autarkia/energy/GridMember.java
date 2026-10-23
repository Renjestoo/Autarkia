package dev.autarkia.energy;

/**
 * An Autarkia block entity that takes part in the grid distribution of 02 section 2.3. The grid uses these
 * fields directly instead of the energy capability, so no member is counted twice.
 */
public interface GridMember {
    /** Grid role of a member. */
    enum Role { GENERATOR, CONSUMER, STORAGE }

    /** Return the role. */
    Role gridRole();

    /** Return the energy buffer. */
    EnergyBuffer gridBuffer();

    /** Return the FE per tick this member may give to the grid (generators and storage). */
    int gridMaxOut();

    /** Return the FE per tick this member may take from the grid (consumers and storage). */
    int gridMaxIn();

    /**
     * Return the FE per tick the member uses while it works steadily (the colony demand signal S02 of 05 section 2.2);
     * unlike the per-tick request it does not grow while a starved buffer refills. Defaults to the maximum intake of a
     * consumer and 0 for other roles.
     */
    default int workDraw() {
        return gridRole() == Role.CONSUMER ? gridMaxIn() : 0;
    }

    /** Return the storage priority group (1 capacitor, 2 battery and coil, 3 inertia); 0 for non-storage. */
    default int gridPriority() {
        return 0;
    }

    /**
     * Return true when a cable span may end on this member as a machine port (02 section 1.2); false for members that
     * only join the grid as a tap next to a connector node.
     */
    default boolean takesSpans() {
        return true;
    }

    /** Return true while the member is a valid, loaded part of the world. */
    boolean gridValid();

    /** Notified after each distribution with the FE given (positive) or taken (negative) this tick. */
    default void gridTransferred(int amount) {
    }
}

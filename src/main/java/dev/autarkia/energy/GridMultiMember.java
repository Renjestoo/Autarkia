package dev.autarkia.energy;

import org.jspecify.annotations.Nullable;

/** A block entity that is a member of one network per cable tier (the transformer, 02 section 6.3). */
public interface GridMultiMember {
    /** Return the member that takes part in the network of the tier, or null when the block has no side of that tier. */
    @Nullable GridMember gridSide(CableTier tier);
}

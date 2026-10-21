package dev.autarkia.logistics;

/** A block entity that owns and saves a transit segment; client deltas are applied to it. */
public interface TransitOwner {
    /** Return the owned segment. */
    TransitSegment segment();
}

package dev.autarkia.colony;

import net.minecraft.world.level.storage.ValueOutput;

/**
 * An Autarkia block entity whose configuration a Pattern keeps (05 section 5.2): sides, filters, modes and
 * priorities only, never items, FE or fluids.
 */
public interface PatternDataHolder {
    /** Write the configuration. */
    void writePatternData(ValueOutput out);

    /** Apply a configuration written by {@link #writePatternData} (Pattern repair and placement). */
    void readPatternData(net.minecraft.world.level.storage.ValueInput in);
}

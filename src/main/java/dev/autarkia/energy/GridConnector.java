package dev.autarkia.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A block that is a grid connector node (02 section 2.1): accepted tier, span limit and attach point. */
public interface GridConnector {
    /** Return true when spans of the tier may attach. */
    boolean acceptsTier(CableTier tier);

    /** Return the maximum number of spans on this node. */
    int maxSpans();

    /** Return the world attach point of spans for the node whose center block is at pos. */
    Vec3 attachPoint(BlockPos pos, BlockState state);
}

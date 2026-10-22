package dev.autarkia.energy;

import java.util.List;
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

    /** Return the world point where the drawn cable of a span toward the given point meets the node's visible fitting. */
    default Vec3 cableEnd(BlockPos pos, BlockState state, Vec3 toward) {
        return attachPoint(pos, state);
    }

    /** Return the refusal for one more span of the tier on this node, or OK; spans are the spans already on the node. */
    default Grid.SpanResult accepts(CableTier tier, List<Span> spans) {
        if (!acceptsTier(tier)) {
            return Grid.SpanResult.WRONG_TIER;
        }
        return spans.size() >= maxSpans() ? Grid.SpanResult.NODE_FULL : Grid.SpanResult.OK;
    }

    /** Return the longest span of the tier that may end on this node (the tier maximum unless the node is shorter). */
    default double spanLimit(CableTier tier) {
        return tier.maxSpan;
    }

    /** Return true when the node feeds the machines on its six neighbours (a transformer and a fuse box do not). */
    default boolean tapsNeighbours() {
        return true;
    }
}

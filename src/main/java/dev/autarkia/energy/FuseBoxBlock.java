package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Fuse box (02 sections 2.1 and 6.3): an inline node with exactly 2 spans of one tier, attach point on the top center.
 * It feeds no neighbour machines; an open fuse cuts the graph.
 */
public class FuseBoxBlock extends MachineBlock implements GridConnector {
    public FuseBoxBlock(Properties props) {
        super(props, AutarkiaBlockEntities.FUSE_BOX, false);
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }

    @Override
    public boolean acceptsTier(CableTier tier) {
        return tier != CableTier.PHASE;
    }

    @Override
    public int maxSpans() {
        return 2;
    }

    @Override
    public Grid.SpanResult accepts(CableTier tier, List<Span> spans) {
        if (!acceptsTier(tier) || !spans.isEmpty() && spans.get(0).tier != tier) {
            return Grid.SpanResult.WRONG_TIER;
        }
        return spans.size() >= maxSpans() ? Grid.SpanResult.NODE_FULL : Grid.SpanResult.OK;
    }

    @Override
    public Vec3 attachPoint(BlockPos pos, BlockState state) {
        return Vec3.atCenterOf(pos).add(0, 0.5, 0);
    }

    @Override
    public boolean tapsNeighbours() {
        return false;
    }
}

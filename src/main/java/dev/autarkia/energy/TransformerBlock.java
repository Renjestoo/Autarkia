package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Transformer (02 sections 2.1 and 6.3): the only node that joins two tiers, phase links included (decision G3). Side A is the tier of its first span, side
 * B the next different tier; up to 4 spans per side, attach point on the top center. It feeds no neighbour machines.
 */
public class TransformerBlock extends MachineBlock implements GridConnector {
    public static final int SPANS_PER_SIDE = 4;

    public TransformerBlock(Properties props) {
        super(props, AutarkiaBlockEntities.TRANSFORMER, false);
    }

    @Override
    public boolean acceptsTier(CableTier tier) {
        return true;
    }

    @Override
    public int maxSpans() {
        return 2 * SPANS_PER_SIDE;
    }

    @Override
    public Grid.SpanResult accepts(CableTier tier, List<Span> spans) {
        if (!acceptsTier(tier)) {
            return Grid.SpanResult.WRONG_TIER;
        }
        java.util.EnumSet<CableTier> tiers = java.util.EnumSet.noneOf(CableTier.class);
        int same = 0;
        for (Span s : spans) {
            tiers.add(s.tier);
            if (s.tier == tier) {
                same++;
            }
        }
        if (!tiers.contains(tier) && tiers.size() >= 2) {
            return Grid.SpanResult.WRONG_TIER;
        }
        return same >= SPANS_PER_SIDE ? Grid.SpanResult.NODE_FULL : Grid.SpanResult.OK;
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

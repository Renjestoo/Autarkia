package dev.autarkia.energy;

import dev.autarkia.registry.BigMachineBlock;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A pole connector node (02 section 2.1): a 1 x height x 1 big block of one tier with its attach point above the center
 * of the top block (wooden pole: copper, 3 high, 4 spans, +0.35; steel pylon: insulated, 5 high, 6 spans, +0.4).
 */
public class PoleBlock extends BigMachineBlock implements GridConnector {
    private final int height;
    private final CableTier tier;
    private final int maxSpans;
    private final double attachUp;

    public PoleBlock(Properties props, Supplier<? extends BlockEntityType<?>> type, int height, CableTier tier, int maxSpans, double attachUp) {
        super(props, type, box(0, 0, 0, 0, height - 1, 0), true);
        this.height = height;
        this.tier = tier;
        this.maxSpans = maxSpans;
        this.attachUp = attachUp;
    }

    /** Return the pole height in blocks. */
    public int height() {
        return height;
    }

    /** Return the tier this pole carries. */
    public CableTier tier() {
        return tier;
    }

    @Override
    public boolean acceptsTier(CableTier t) {
        return t == tier;
    }

    @Override
    public int maxSpans() {
        return maxSpans;
    }

    @Override
    public Vec3 attachPoint(BlockPos pos, BlockState state) {
        return Vec3.atCenterOf(pos.above(height - 1)).add(0, attachUp, 0);
    }
}

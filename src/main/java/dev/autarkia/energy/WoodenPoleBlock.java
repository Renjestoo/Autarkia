package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.BigMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Copper wire connector node (02 section 2.1): a 1x3x1 pole, copper tier, 4 spans, attach point 0.35 above the top block center. */
public class WoodenPoleBlock extends BigMachineBlock implements GridConnector {
    public WoodenPoleBlock(Properties props) {
        super(props, AutarkiaBlockEntities.WOODEN_POLE, box(0, 0, 0, 0, 2, 0), true);
    }

    @Override
    public boolean acceptsTier(CableTier tier) {
        return tier == CableTier.COPPER;
    }

    @Override
    public int maxSpans() {
        return 4;
    }

    @Override
    public Vec3 attachPoint(BlockPos pos, BlockState state) {
        return Vec3.atCenterOf(pos.above(2)).add(0, 0.35, 0);
    }
}

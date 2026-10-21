package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Phase node (02 sections 2.1 and 4.7): a one-block connector of tier PHASE with up to 4 links attached at its center.
 * The wrench links two nodes up to 64 blocks apart with a clear line of sight ({@link PhaseLinks}); the node also taps its
 * 6 neighbours and takes foreign FE into its injection pool on every face.
 */
public class PhaseNodeBlock extends MachineBlock implements GridConnector {
    public static final int MAX_LINKS = 4;

    public PhaseNodeBlock(Properties props) {
        super(props, AutarkiaBlockEntities.PHASE_NODE, false);
    }

    @Override
    public boolean acceptsTier(CableTier tier) {
        return tier == CableTier.PHASE;
    }

    @Override
    public int maxSpans() {
        return MAX_LINKS;
    }

    @Override
    public Vec3 attachPoint(BlockPos pos, BlockState state) {
        return Vec3.atCenterOf(pos);
    }
}

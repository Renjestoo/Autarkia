package dev.autarkia.logistics;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.JadeInfo;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity of a capsule tube: no state and no tick. Clients draw the capsules inside its block from the network
 * mirrors; Jade shows the network size, capsules and state; removing the tube drops the capsules inside it (rule C2).
 */
public class CapsuleTubeBlockEntity extends AutarkiaBlockEntity implements JadeInfo {
    private CapsuleTopology view = CapsuleTopology.EMPTY;
    private int viewEpoch = -1;

    public CapsuleTubeBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.CAPSULE_TUBE.get(), pos, state);
    }

    @Override
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<Component> out) {
        CapsuleNetwork net = CapsuleNetworks.containing(level, worldPosition.asLong());
        CapsuleTopology t;
        int capsules;
        CapsuleNetwork.State state;
        if (net != null) {
            t = net.topology();
            capsules = net.size();
            state = net.state();
        } else {
            int e = CapsuleNetworks.epoch(level);
            if (e != viewEpoch || view.incomplete) {
                viewEpoch = e;
                view = CapsuleTopology.flood(level, worldPosition, AutarkiaConfig.TUBE_MAX_NETWORK_TUBES.get());
            }
            t = view;
            capsules = 0;
            state = t.tooLarge ? CapsuleNetwork.State.TOO_LARGE : t.incomplete ? CapsuleNetwork.State.PAUSED
                : t.stations.isEmpty() ? CapsuleNetwork.State.NO_STATION : CapsuleNetwork.State.PAUSED;
        }
        out.add(Component.translatable("jade.autarkia.capsule_tube", t.tubes.size(), t.stations.size(), capsules, Component.translatable(state.key())));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel sl) {
            CapsuleNetworks.tubeRemoved(sl, pos);
            CapsuleNetworks.bump(sl);
        }
    }
}

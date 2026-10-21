package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rope pulley node: insert-only energy face on all sides (02 section 1.2); inserted FE goes to the network injection
 * pool once per tick, the rest is discarded.
 */
public class RopePulleyBlockEntity extends AutarkiaBlockEntity implements GridInjector, dev.autarkia.registry.JadeInfo {
    private final EnergyBuffer pool = new EnergyBuffer(CableTier.ROPE.rated * 20, CableTier.ROPE.rated, 0, () -> { });

    public RopePulleyBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.ROPE_PULLEY.get(), pos, state);
    }

    /** Return the insert-only handler other mods' generators push into. */
    public EnergyBuffer pool() {
        return pool;
    }

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(java.util.List<net.minecraft.network.chat.Component> out) {
        GridLevel g = Grid.of((net.minecraft.server.level.ServerLevel) level);
        it.unimi.dsi.fastutil.ints.IntArrayList ids = g.data().at(worldPosition.asLong());
        float load = 0;
        for (int i = 0; i < ids.size(); i++) {
            Span s = g.data().spans.get(ids.getInt(i));
            if (s != null) {
                load = Math.max(load, s.load);
            }
        }
        out.add(net.minecraft.network.chat.Component.translatable("jade.autarkia.tier",
            net.minecraft.network.chat.Component.translatable("tier.autarkia.rope")));
        out.add(net.minecraft.network.chat.Component.translatable("jade.autarkia.spans", ids.size(), 4));
        out.add(net.minecraft.network.chat.Component.translatable("jade.autarkia.network_load", Math.round(load * 100)));
    }

    @Override
    public int takeInjected(int max) {
        int got = pool.drain(max);
        pool.drain(pool.stored());
        return got;
    }
}

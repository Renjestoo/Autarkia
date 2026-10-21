package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.JadeInfo;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Connector node state: Jade node lines and an insert-only energy face (02 section 1.2) whose FE joins the network
 * injection pool once per tick (up to 4 x the tier rating, so a foreign generator can overload a line; the rest is
 * discarded).
 */
public class NodeBlockEntity extends AutarkiaBlockEntity implements JadeInfo, GridInjector {
    private final CableTier tier;
    private final int maxSpans;
    private final EnergyBuffer pool;

    protected NodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CableTier tier, int maxSpans) {
        super(type, pos, state);
        this.tier = tier;
        this.maxSpans = maxSpans;
        this.pool = new EnergyBuffer(tier.rated * 20, tier.rated * 4, 0, () -> { });
    }

    /** Return the wall insulator node (insulated, 2 spans). */
    public static NodeBlockEntity wallInsulator(BlockPos pos, BlockState state) {
        return new NodeBlockEntity(AutarkiaBlockEntities.WALL_INSULATOR.get(), pos, state, CableTier.INSULATED, 2);
    }

    /** Return the insert-only handler other mods' generators push into. */
    public EnergyBuffer pool() {
        return pool;
    }

    @Override
    public int takeInjected(int max) {
        int got = pool.drain(max);
        pool.drain(pool.stored());
        return got;
    }

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(List<Component> out) {
        Grid.nodeLines((ServerLevel) level, worldPosition, tier, maxSpans, out);
    }
}

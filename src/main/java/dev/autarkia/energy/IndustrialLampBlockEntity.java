package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.Ticking;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Industrial lamp: uses 1 FE per tick from a 40 FE buffer (it takes up to 2 FE per tick to fill it) and is lit while it
 * has energy; below 20 FE (its network
 * cannot keep it full) it flickers off one tick window in five. A snap that leaves its
 * network without supply schedules it dark at {@code 3 ticks x graph hops} from the snapped end (lamp ripple, W5).
 */
public class IndustrialLampBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, JadeInfo {
    private final EnergyBuffer buffer = new EnergyBuffer(40, 2, 0, this::setChanged);
    private long darkAt = Long.MAX_VALUE;

    public IndustrialLampBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.INDUSTRIAL_LAMP.get(), pos, state);
    }

    /** Turn the lamp dark at the given game tick (its stored energy is gone then). */
    public void scheduleDark(long tick) {
        darkAt = Math.min(darkAt, tick);
    }

    /** Return true while the lamp is lit. */
    public boolean lit() {
        return getBlockState().getValue(IndustrialLampBlock.LIT);
    }

    @Override
    public void serverTick() {
        long now = level.getGameTime();
        if (now >= darkAt) {
            buffer.drain(buffer.stored());
            darkAt = Long.MAX_VALUE;
        }
        boolean on = buffer.drain(1) == 1 && !(buffer.stored() < 20 && Math.floorMod(now / 3 + worldPosition.asLong(), 5) == 0);
        if (on != lit()) {
            level.setBlock(worldPosition, getBlockState().setValue(IndustrialLampBlock.LIT, on), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Role gridRole() {
        return Role.CONSUMER;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return buffer;
    }

    @Override
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int gridMaxIn() {
        return buffer.room() > 0 ? 2 : 0;
    }

    @Override
    public int workDraw() {
        return 1;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.buffer", buffer.stored(), buffer.capacity()));
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
    }
}

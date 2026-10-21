package dev.autarkia.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.nikdo53.tinymultiblocklib.blockentities.AbstractMultiBlockEntity;

/**
 * Base of every Autarkia block entity. It extends the Tiny Multiblock Lib entity so big blocks and single blocks share
 * one base (a single block is its own center); the update tag carries the full saved state for client renderers.
 */
public abstract class AutarkiaBlockEntity extends AbstractMultiBlockEntity {
    private long lastSync = Long.MIN_VALUE / 2;
    private boolean syncPending;

    protected AutarkiaBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Mark the entity changed and send its state to tracking clients now. */
    public void syncNow() {
        setChanged();
        if (level instanceof ServerLevel sl) {
            sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            lastSync = sl.getGameTime();
            syncPending = false;
        }
    }

    /** Mark the entity changed and send its state to clients at most once per interval ticks (call every tick until sent). */
    public void syncThrottled(int interval) {
        setChanged();
        syncPending = true;
        flushSync(interval);
    }

    /** Send a pending throttled sync when the interval has passed. */
    protected void flushSync(int interval) {
        if (syncPending && level != null && level.getGameTime() - lastSync >= interval) {
            syncNow();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel sl && isCenter() && !dev.autarkia.energy.Grid.of(sl).data().at(pos.asLong()).isEmpty()) {
            dev.autarkia.energy.Grid.nodeRemoved(sl, pos);
        }
    }

    /** Return the level as a server level, or null on the client. */
    protected ServerLevel serverLevel() {
        return level instanceof ServerLevel sl ? sl : null;
    }
}

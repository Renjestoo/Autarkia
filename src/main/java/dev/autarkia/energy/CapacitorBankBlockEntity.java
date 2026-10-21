package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Capacitor bank (02 section 6.2): 50,000 FE, I/O 2,048 FE/t, leak 0.0001 x E per tick, storage priority group 1,
 * charges an FE item in its slot at 512 FE/t, shows 0 to 4 charge stripes. Lightning input ({@link #absorbLightning})
 * has no rate limit.
 */
public class CapacitorBankBlockEntity extends StorageBankBlockEntity {
    public static final int CAPACITY = 50000;

    public CapacitorBankBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.CAPACITOR_BANK.get(), pos, state, CAPACITY, 2048, 512, 0.0001, 1);
    }

    /** Take lightning energy without the rate limit; returns the FE taken (capped at the free room). */
    public int absorbLightning(int fe) {
        int got = buffer().fill(fe);
        if (got > 0) {
            level.playSound(null, worldPosition, AutarkiaSounds.of("capacitor.charge"), SoundSource.BLOCKS, 0.6f, 1.2f);
            syncNow();
        }
        return got;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        int stripes = stripes();
        if (getBlockState().getValue(CapacitorBankBlock.CHARGE) != stripes) {
            level.setBlock(worldPosition, getBlockState().setValue(CapacitorBankBlock.CHARGE, stripes), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    /** Return the charge level 0 to 4 for the four stripes. */
    public int stripes() {
        return Math.min(4, (int) Math.ceil(4.0 * buffer().stored() / CAPACITY));
    }
}

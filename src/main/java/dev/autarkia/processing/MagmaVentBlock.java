package dev.autarkia.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Magma vent (02 and 03 section 2.2): a worldgen block in cave floors and the Nether, the endless lava source of the
 * Magma Tap. Hardness 3, resistance 6, light 6, pickaxe; it drops a magma block (itself with silk touch). Its display
 * tick shows a lava drop 1 time in 4 and large smoke rising from the top.
 */
public class MagmaVentBlock extends Block {
    public MagmaVentBlock(Properties props) {
        super(props);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.02;
        double z = pos.getZ() + 0.5;
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
        }
        level.addParticle(ParticleTypes.LARGE_SMOKE, x + (random.nextDouble() - 0.5) * 0.5, y, z + (random.nextDouble() - 0.5) * 0.5, 0, 0.07, 0);
    }
}

package dev.autarkia.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lattice cluster (03 section 2.1): a crystal on one face of a block, with the vanilla cluster {@code facing},
 * {@code waterlogged}, shape (7 pixels tall) and support rules. It needs a netherite-tier pickaxe for drops (tag
 * {@code neoforge:needs_netherite_tool}); the loot table gives 2 lattice shards with the fortune ore bonus, itself with
 * silk touch. Near a player it chimes now and then.
 */
public class LatticeClusterBlock extends AmethystClusterBlock {
    private static final int CHIME_CHANCE = 20;

    public LatticeClusterBlock(Properties props) {
        super(7f, 3f, props);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(CHIME_CHANCE) == 0) {
            level.playLocalSound(pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, 0.5f + random.nextFloat() * 1.2f, false);
        }
    }
}

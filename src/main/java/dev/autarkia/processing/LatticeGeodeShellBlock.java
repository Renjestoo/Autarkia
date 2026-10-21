package dev.autarkia.processing;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import org.jspecify.annotations.Nullable;

/**
 * Lattice geode shell (03 section 2.1), the inner layer of a lattice geode. A {@code charged} shell gets random ticks:
 * each one grows a {@code lattice_cluster} on a random adjacent air or water face with chance
 * {@code geode.chargedShellGrowthChance}. Pistons cannot move a charged shell, it shows a rare glow and its loot table
 * drops nothing; an uncharged shell drops itself.
 */
public class LatticeGeodeShellBlock extends Block {
    /** True for the alternate (budding) layer that grows clusters. */
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
    private static final int GLOW_CHANCE = 8;

    public LatticeGeodeShellBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(CHARGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGED);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(CHARGED);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CHARGED) || random.nextDouble() >= AutarkiaConfig.GEODE_CHARGED_SHELL_GROWTH_CHANCE.get()) {
            return;
        }
        Direction side = Direction.getRandom(random);
        BlockPos at = pos.relative(side);
        BlockState there = level.getBlockState(at);
        if (BuddingAmethystBlock.canClusterGrowAtState(there)) {
            level.setBlockAndUpdate(at, AutarkiaBlocks.LATTICE_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, side)
                .setValue(AmethystClusterBlock.WATERLOGGED, there.getFluidState().isSourceOfType(Fluids.WATER)));
        }
    }

    @Override
    public @Nullable PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(CHARGED) ? PushReaction.IMMOVEABLE : null;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CHARGED) || random.nextInt(GLOW_CHANCE) != 0) {
            return;
        }
        Direction side = Direction.getRandom(random);
        if (level.getBlockState(pos.relative(side)).isSolidRender()) {
            return;
        }
        double x = pos.getX() + 0.5 + side.getStepX() * 0.55 + (side.getStepX() == 0 ? random.nextDouble() - 0.5 : 0);
        double y = pos.getY() + 0.5 + side.getStepY() * 0.55 + (side.getStepY() == 0 ? random.nextDouble() - 0.5 : 0);
        double z = pos.getZ() + 0.5 + side.getStepZ() * 0.55 + (side.getStepZ() == 0 ? random.nextDouble() - 0.5 : 0);
        level.addParticle(ParticleTypes.GLOW, x, y, z, 0, 0.02, 0);
    }
}

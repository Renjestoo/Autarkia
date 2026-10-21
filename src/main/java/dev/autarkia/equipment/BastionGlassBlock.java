package dev.autarkia.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/** {@code bastion_glass} (07 section 3.4): the rules of {@link BastionBlock} on a transparent glass block. */
public class BastionGlassBlock extends TransparentBlock {
    public BastionGlassBlock(Properties props) {
        super(props);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return BastionBlock.progress(level, pos, player, super.getDestroyProgress(state, player, level, pos));
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return BastionBlock.resistance(level, pos);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return BastionBlock.entityMayDestroy(level, pos, entity, super.canEntityDestroy(state, level, pos, entity));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.IMMOVEABLE;
    }
}

package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.BigMachineBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

/**
 * Stone flywheel block, 3x3x1 with the mount at the bottom center (02 section 6.2): placement needs a sturdy block under
 * the mount; when that support goes, a 1-tick block tick breaks the mount and the whole wheel (release rule). No drop
 * while the center holds 2,000 FE or more.
 */
public class StoneFlywheelBlock extends BigMachineBlock {
    public StoneFlywheelBlock(Properties props) {
        super(props, AutarkiaBlockEntities.STONE_FLYWHEEL, BigMachineBlock.box(-1, 0, 0, 1, 2, 0), true);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos below = ctx.getClickedPos().below();
        if (!ctx.getLevel().getBlockState(below).isFaceSturdy(ctx.getLevel(), below, Direction.UP)) {
            return null;
        }
        return super.getStateForPlacement(ctx);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean moved) {
        super.neighborChanged(state, level, pos, block, orientation, moved);
        if (!level.isClientSide() && state.getValue(CENTER) && !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(CENTER) && !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof StoneFlywheelBlockEntity be && be.releases()) {
            return List.of();
        }
        return super.getDrops(state, params);
    }
}

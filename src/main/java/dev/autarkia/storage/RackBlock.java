package dev.autarkia.storage;

import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Rack storage block (04 section 2.3): crate positions, a load rating in load units and the salvage it drops when it
 * collapses. Property {@code bend} 0 to 3 shows the load stage. A rack needs a sturdy block or a rack below it; when
 * that support goes, the rack collapses. Right-click with a crate puts it into the first free position.
 */
public class RackBlock extends MachineBlock {
    public static final IntegerProperty BEND = IntegerProperty.create("bend", 0, 3);
    public final int positions;
    public final int rating;
    private final Supplier<ItemStack> salvage;

    public RackBlock(Properties props, Supplier<? extends BlockEntityType<?>> type, int positions, int rating, Supplier<ItemStack> salvage) {
        super(props, type, false);
        this.positions = positions;
        this.rating = rating;
        this.salvage = salvage;
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }

    @Override
    protected BlockState defaultState(BlockState state) {
        return state.setValue(BEND, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BEND);
    }

    /** Return a copy of the salvage stack dropped on collapse. */
    public ItemStack salvage() {
        return salvage.get().copy();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState b = level.getBlockState(below);
        return b.getBlock() instanceof RackBlock || b.isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos neighborPos,
                                     BlockState neighbor, RandomSource random) {
        if (dir == Direction.DOWN && !canSurvive(state, level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos) && level.getBlockEntity(pos) instanceof RackBlockEntity rack) {
            rack.collapse("support");
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.is(AutarkiaItems.CRATE.get()) && level.getBlockEntity(pos) instanceof RackBlockEntity rack) {
            if (!level.isClientSide() && rack.insertCrate(stack.copyWithCount(1)) && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}

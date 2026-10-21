package dev.autarkia.logistics;

import dev.autarkia.energy.WrenchItem;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import net.nikdo53.tinymultiblocklib.block.IMBStateSharer;
import net.nikdo53.tinymultiblocklib.block.IMultiBlock;
import net.nikdo53.tinymultiblocklib.block.shape.MultiblockShape;
import net.nikdo53.tinymultiblocklib.block.shape.ShapeContext;
import net.nikdo53.tinymultiblocklib.components.SharedStatePropertiesBuilder;
import org.jspecify.annotations.Nullable;

/**
 * {@code catch_net} (04 section 1.6): a 3x1x3 {@link BaseMultiblock} without an own block entity (each position holds
 * the library's simple multiblock entity). Its net hangs at 12 px; a launched crate that crosses that plane within the
 * catch radius of the centre is caught: all contents plus the empty crate go into the item handler directly below the
 * centre in one transaction. Success shows {@code POOF}, plays {@code net_catch} and sets the shared {@code bounce}
 * property on every position for 10 ticks (a scheduled tick on the centre clears it). A wrench that carries a launcher
 * as its link start targets this net.
 */
public class CatchNetBlock extends BaseMultiblock {
    public static final BooleanProperty BOUNCE = BooleanProperty.create("bounce");
    public static final double PLANE = 0.75;
    public static final int BOUNCE_TICKS = 10;
    private static final VoxelShape NET = Block.box(0, 10, 0, 16, 12, 16);

    /** What the item handler under the centre can do with a caught crate (Jade). */
    public enum Output { OK, MISSING, FULL }

    public CatchNetBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(BOUNCE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BOUNCE);
    }

    @Override
    public void createSharedBlockStates(SharedStatePropertiesBuilder builder) {
        super.createSharedBlockStates(builder);
        builder.add(BOUNCE);
    }

    @Override
    public IMultiBlock.@Nullable DirectionContext makeDirectional() {
        return new IMultiBlock.DirectionContext(HorizontalDirectionalBlock.FACING, ctx -> ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void makeMultiblockShape(MultiblockShape.Builder builder, ShapeContext context) {
        builder.pushDirectionalOperation(context.getDirection());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != 0 || z != 0) {
                    builder.addNoLogic(x, 0, z);
                }
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return NET;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return NET;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof WrenchItem) || !stack.has(AutarkiaComponents.LINK_START.get()) || player.isShiftKeyDown()) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            return CrateLauncherBlockEntity.targetFromWrench(sl, sp, stack, IMultiBlock.getCenter(level, pos));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Let the net at center take a launched crate: insert all contents plus the empty crate into the item handler
     * directly below the centre in one transaction. Returns false and changes nothing when there is no handler or not
     * everything fits (the crate then bounces off).
     */
    public static boolean tryCatch(ServerLevel sl, BlockPos center, ItemStack crate) {
        ResourceHandler<ItemResource> h = sl.getCapability(Capabilities.Item.BLOCK, center.below(), Direction.UP);
        if (h == null) {
            return false;
        }
        List<ItemStack> all = new ArrayList<>();
        ItemContainerContents c = crate.get(DataComponents.CONTAINER);
        if (c != null) {
            c.nonEmptyItemCopyStream().forEach(all::add);
        }
        ItemStack shell = crate.copyWithCount(1);
        shell.remove(DataComponents.CONTAINER);
        all.add(shell);
        try (Transaction tx = Transaction.openRoot()) {
            for (ItemStack s : all) {
                if (h.insert(ItemResource.of(s), s.getCount(), tx) < s.getCount()) {
                    return false;
                }
            }
            tx.commit();
        }
        sl.sendParticles(ParticleTypes.POOF, center.getX() + 0.5, center.getY() + PLANE + 0.2, center.getZ() + 0.5, 24, 1.0, 0.3, 1.0, 0.05);
        sl.playSound(null, center, AutarkiaSounds.of("net_catch"), SoundSource.BLOCKS, 1.0f, 0.7f);
        bounce(sl, center);
        return true;
    }

    /** Set {@code bounce} on every position of the net for 10 ticks. */
    public static void bounce(ServerLevel sl, BlockPos center) {
        BlockState st = sl.getBlockState(center);
        if (!(st.getBlock() instanceof CatchNetBlock net)) {
            return;
        }
        IMBStateSharer.setBlockState(sl, center, BOUNCE, true);
        sl.scheduleTick(center, net, BOUNCE_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(BOUNCE)) {
            IMBStateSharer.setBlockState(level, pos, BOUNCE, false);
        }
    }

    /** Return what the handler below the centre of the net offers a caught crate. */
    public static Output output(Level level, BlockPos center) {
        ResourceHandler<ItemResource> h = level.getCapability(Capabilities.Item.BLOCK, center.below(), Direction.UP);
        if (h == null) {
            return Output.MISSING;
        }
        return ResourceHandlerUtil.isFull(h) ? Output.FULL : Output.OK;
    }
}

package dev.autarkia.processing;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Manual grinder (03 section 4.2): use with an item inserts it, hold use with an empty hand to turn; 1 turn = 10 ticks,
 * {@code quern.turnsPerItem} turns per ore. Outputs go to an inventory below, else pop out on top. No GUI.
 */
public class QuernBlockEntity extends MachineBlockEntity implements GeoBlockEntity {
    private static final RawAnimation TURN = RawAnimation.begin().thenLoop("animation.quern.turn");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.quern.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private long turnUntil;
    private long workerUntil;

    public QuernBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.QUERN.get(), pos, state, 1, 2, 0);
    }

    /** Keep the runner turning for 6 more ticks (player hand). */
    public void turn() {
        turnUntil = level.getGameTime() + 6;
    }

    /** Set whether a worker turns the quern this tick. */
    public void setWorkerDriving(boolean driving) {
        workerUntil = driving ? level.getGameTime() + 1 : 0;
    }

    /** Insert as much of the stack as fits; return the rest. */
    public ItemStack insert(ItemStack stack) {
        ResourceHandler<ItemResource> h = inv.automation();
        try (Transaction tx = Transaction.openRoot()) {
            int moved = h.insert(0, ItemResource.of(stack), stack.getCount(), tx);
            tx.commit();
            return stack.copyWithCount(stack.getCount() - moved);
        }
    }

    @Override
    protected MachineRecipeKind kind() {
        return MachineRecipeKind.CRUSHING;
    }

    @Override
    protected int defaultTime() {
        return AutarkiaConfig.QUERN_TURNS_PER_ITEM.get() * 10;
    }

    @Override
    protected int defaultEnergy() {
        return 0;
    }

    @Override
    protected float workRate() {
        long now = level.getGameTime();
        return now < turnUntil || now <= workerUntil ? 1 : 0;
    }

    @Override
    protected MachineStatus.State conditionState() {
        return MachineStatus.State.IDLE_NO_INPUT;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (running() && Math.round(progress) % 10 == 0) {
            level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6f, 1f);
        }
        pushOutputs((ServerLevel) level);
    }

    private void pushOutputs(ServerLevel sl) {
        ResourceHandler<ItemResource> below = sl.getCapability(Capabilities.Item.BLOCK, worldPosition.below(), Direction.UP);
        for (int i = inv.inputs(); i < inv.size(); i++) {
            ItemStack s = inv.get(i);
            if (s.isEmpty()) {
                continue;
            }
            if (below != null) {
                try (Transaction tx = Transaction.openRoot()) {
                    int moved = below.insert(ItemResource.of(s), s.getCount(), tx);
                    tx.commit();
                    inv.shrink(i, moved);
                }
                continue;
            }
            ItemEntity e = new ItemEntity(sl, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5, s.copy());
            e.setDeltaMovement(0, 0.2, 0);
            sl.addFreshEntity(e);
            inv.put(i, ItemStack.EMPTY);
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<QuernBlockEntity>("main", 2, test -> test.setAndContinue(test.animatable().running() ? TURN : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

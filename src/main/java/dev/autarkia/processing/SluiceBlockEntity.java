package dev.autarkia.processing;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Sluice, 1x1x3 along its facing (03 section 4.2): the head is the back end; the block behind or above the head must
 * hold water (flowing: speed 1.0, only a source: {@code sluice.sourceWaterSpeed}, none: NO_CONDITION). 100 ticks per
 * washing. The tail pushes outputs into the item handler in front of it; with no handler there it drops them in front.
 */
public class SluiceBlockEntity extends MachineBlockEntity implements GeoBlockEntity {
    /** Water at the head: none, a source only, or flowing water. */
    public enum Water { NONE, SOURCE, FLOWING }

    private static final RawAnimation FLOW = RawAnimation.begin().thenLoop("animation.sluice.flow");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.sluice.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Water water = Water.NONE;

    public SluiceBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.SLUICE.get(), pos, state, 1, 2, 0);
    }

    private Direction front() {
        return MachineBlock.front(getBlockState());
    }

    /** Return the head (back end) position. */
    public BlockPos head() {
        return worldPosition.relative(front().getOpposite());
    }

    /** Return the tail (front end) position. */
    public BlockPos tail() {
        return worldPosition.relative(front());
    }

    /** Return the water found at the head in the last tick. */
    public Water water() {
        return water;
    }

    private Water findWater() {
        Water best = Water.NONE;
        for (BlockPos p : List.of(head().relative(front().getOpposite()), head().above())) {
            FluidState f = level.getFluidState(p);
            if (f.is(FluidTags.WATER)) {
                if (!f.isSource()) {
                    return Water.FLOWING;
                }
                best = Water.SOURCE;
            }
        }
        return best;
    }

    @Override
    protected MachineRecipeKind kind() {
        return MachineRecipeKind.WASHING;
    }

    @Override
    protected int defaultTime() {
        return 100;
    }

    @Override
    protected int defaultEnergy() {
        return 0;
    }

    @Override
    protected float workRate() {
        return switch (water) {
            case NONE -> 0;
            case SOURCE -> AutarkiaConfig.SLUICE_SOURCE_WATER_SPEED.get().floatValue();
            case FLOWING -> 1;
        };
    }

    @Override
    public void serverTick() {
        water = findWater();
        super.serverTick();
        ServerLevel sl = serverLevel();
        pushOutputs(sl);
        long now = sl.getGameTime();
        if (running()) {
            if (now % 40 == 0) {
                sl.playSound(null, worldPosition, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.4f, 1.2f);
            }
            if (now % 5 == 0) {
                BlockPos h = head();
                sl.sendParticles(ParticleTypes.SPLASH, h.getX() + 0.5, h.getY() + 0.6, h.getZ() + 0.5, 3, 0.2, 0.05, 0.2, 0.05);
                BlockPos t = tail();
                sl.sendParticles(ParticleTypes.FALLING_WATER, t.getX() + 0.5, t.getY() + 0.4, t.getZ() + 0.5, 2, 0.25, 0.05, 0.25, 0);
            }
        }
    }

    private void pushOutputs(ServerLevel sl) {
        BlockPos target = tail().relative(front());
        ResourceHandler<ItemResource> h = sl.getCapability(Capabilities.Item.BLOCK, target, front().getOpposite());
        for (int slot = inv.inputs(); slot < inv.size(); slot++) {
            ItemStack out = inv.get(slot);
            if (out.isEmpty()) {
                continue;
            }
            if (h == null) {
                ItemEntity e = new ItemEntity(sl, target.getX() + 0.5, target.getY() + 0.3, target.getZ() + 0.5, out.copy());
                e.setDeltaMovement(front().getStepX() * 0.05, 0, front().getStepZ() * 0.05);
                sl.addFreshEntity(e);
                inv.put(slot, ItemStack.EMPTY);
                continue;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int moved = h.insert(ItemResource.of(out), out.getCount(), tx);
                if (moved > 0) {
                    tx.commit();
                    inv.shrink(slot, moved);
                }
            }
        }
    }

    @Override
    public void jadeLines(List<Component> out) {
        super.jadeLines(out);
        out.add(Component.translatable("jade.autarkia.sluice." + water.name().toLowerCase(java.util.Locale.ROOT)));
    }

    @Override
    protected int extraData(int i) {
        return water.ordinal();
    }

    @Override
    protected int extraDataCount() {
        return 1;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.SLUICE.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.SLUICE);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SluiceBlockEntity>("main", 4, test -> test.setAndContinue(test.animatable().running() ? FLOW : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

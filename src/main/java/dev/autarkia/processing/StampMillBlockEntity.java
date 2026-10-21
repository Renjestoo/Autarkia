package dev.autarkia.processing;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.SlotMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Powered grinder, 1x2x1 (03 section 4.1): crushing at 16 FE/t, 160 ticks; the hammer drops once per 40 ticks. */
public class StampMillBlockEntity extends MachineBlockEntity implements GeoBlockEntity {
    private static final RawAnimation STAMP = RawAnimation.begin().thenLoop("animation.stamp_mill.stamp");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.stamp_mill.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int hammer;

    public StampMillBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.STAMP_MILL.get(), pos, state, 1, 2, 16);
    }

    @Override
    protected MachineRecipeKind kind() {
        return MachineRecipeKind.CRUSHING;
    }

    @Override
    protected int defaultTime() {
        return 160;
    }

    @Override
    protected int defaultEnergy() {
        return 16;
    }

    @Override
    protected float workRate() {
        return 1;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (!running()) {
            hammer = 0;
            return;
        }
        if (++hammer % 40 == 36) {
            ServerLevel sl = (ServerLevel) level;
            sl.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3f, 1f);
            ItemStack in = inv.get(0);
            if (!in.isEmpty()) {
                sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, in.getItem()), worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.45, worldPosition.getZ() + 0.5, 8, 0.15, 0.05, 0.15, 0.05);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.STAMP_MILL.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.STAMP_MILL);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<StampMillBlockEntity>("main", 0, test -> test.setAndContinue(test.animatable().running() ? STAMP : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The two T3 one-block machines of 03 sections 4.1 and 7.5: {@code magnetic_separator} (separating, 1 input, 3
 * outputs, 128 FE/t, 120 ticks; a beacon hum at pitch 1.6 loops while it works) and {@code electrolyzer}
 * (electrolyzing, 2 inputs, 2 outputs, 256 FE/t, 160 ticks; a brewing sound per operation). Both spark while working.
 */
public class ElectricMachineBlockEntity extends MachineBlockEntity implements GeoBlockEntity {
    private final boolean separator;
    private final RawAnimation run;
    private final RawAnimation idle;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private ElectricMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean separator) {
        super(type, pos, state, separator ? 1 : 2, separator ? 3 : 2, separator ? 128 : 256);
        this.separator = separator;
        String id = separator ? "magnetic_separator" : "electrolyzer";
        this.run = RawAnimation.begin().thenLoop("animation." + id + ".run");
        this.idle = RawAnimation.begin().thenLoop("animation." + id + ".idle");
    }

    /** Create a magnetic separator block entity. */
    public static ElectricMachineBlockEntity separator(BlockPos pos, BlockState state) {
        return new ElectricMachineBlockEntity(AutarkiaBlockEntities.MAGNETIC_SEPARATOR.get(), pos, state, true);
    }

    /** Create an electrolyzer block entity. */
    public static ElectricMachineBlockEntity electrolyzer(BlockPos pos, BlockState state) {
        return new ElectricMachineBlockEntity(AutarkiaBlockEntities.ELECTROLYZER.get(), pos, state, false);
    }

    @Override
    protected MachineRecipeKind kind() {
        return separator ? MachineRecipeKind.SEPARATING : MachineRecipeKind.ELECTROLYZING;
    }

    @Override
    protected int defaultTime() {
        return separator ? 120 : 160;
    }

    @Override
    protected int defaultEnergy() {
        return separator ? 128 : 256;
    }

    @Override
    protected float workRate() {
        return 1;
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (!running()) {
            return;
        }
        ServerLevel sl = serverLevel();
        long now = sl.getGameTime();
        if (now % 10 == 0) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5, 2, 0.25, 0.05,
                0.25, 0.02);
        }
        if (separator && now % 80 == 0) {
            sl.playSound(null, worldPosition, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.4f, 1.6f);
        }
    }

    @Override
    protected void onOperation(MachineRecipe recipe) {
        if (!separator) {
            serverLevel().playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6f, 1f);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return separator
            ? new SlotMenu(AutarkiaMenus.MAGNETIC_SEPARATOR.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.MAGNETIC_SEPARATOR)
            : new SlotMenu(AutarkiaMenus.ELECTROLYZER.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.ELECTROLYZER);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ElectricMachineBlockEntity>("main", 4, test -> test.setAndContinue(test.animatable().running() ? run : idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

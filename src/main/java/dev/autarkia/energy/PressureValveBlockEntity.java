package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The pressure valve's animation holder: the boiler under it triggers {@code vent} while it blows off steam. */
public class PressureValveBlockEntity extends AutarkiaBlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.pressure_valve.idle");
    private static final RawAnimation VENT = RawAnimation.begin().thenPlay("animation.pressure_valve.vent");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PressureValveBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.PRESSURE_VALVE.get(), pos, state);
    }

    /** Play the vent animation (cap lifts and drops) on tracking clients. */
    public void vent() {
        triggerAnim("main", "vent");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<PressureValveBlockEntity> main = new AnimationController<>("main", 0, test -> test.setAndContinue(IDLE));
        main.triggerableAnim("vent", VENT);
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

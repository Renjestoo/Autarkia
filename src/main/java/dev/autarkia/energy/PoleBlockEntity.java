package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.registry.AutarkiaBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pole node with a GeckoLib model (wooden pole, steel pylon). A lightning arrester on the block above the top protects
 * it (02 section 5).
 */
public class PoleBlockEntity extends NodeBlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation idle;

    private PoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CableTier tier, int maxSpans, String model) {
        super(type, pos, state, tier, maxSpans);
        this.idle = RawAnimation.begin().thenLoop("animation." + model + ".idle");
    }

    /** Return the wooden pole node (copper, 4 spans). */
    public static PoleBlockEntity wooden(BlockPos pos, BlockState state) {
        return new PoleBlockEntity(AutarkiaBlockEntities.WOODEN_POLE.get(), pos, state, CableTier.COPPER, 4, "wooden_pole");
    }

    /** Return the steel pylon node (insulated, 6 spans). */
    public static PoleBlockEntity steel(BlockPos pos, BlockState state) {
        return new PoleBlockEntity(AutarkiaBlockEntities.STEEL_PYLON.get(), pos, state, CableTier.INSULATED, 6, "steel_pylon");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<PoleBlockEntity>("idle", 0, test -> test.setAndContinue(idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.JadeInfo;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wooden pole node: GeckoLib model, Jade node lines, an insert-only energy face on every position (02 section 1.2) whose
 * FE joins the network injection pool once per tick (up to 4 x the copper rating, so a foreign generator can overload a
 * line; the rest is discarded). A lightning arrester on the block above the top protects it (02 section 5).
 */
public class WoodenPoleBlockEntity extends AutarkiaBlockEntity implements GeoBlockEntity, JadeInfo, GridInjector {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.wooden_pole.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer pool = new EnergyBuffer(CableTier.COPPER.rated * 20, CableTier.COPPER.rated * 4, 0, () -> { });

    public WoodenPoleBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.WOODEN_POLE.get(), pos, state);
    }

    /** Return the insert-only handler other mods' generators push into. */
    public EnergyBuffer pool() {
        return pool;
    }

    @Override
    public int takeInjected(int max) {
        int got = pool.drain(max);
        pool.drain(pool.stored());
        return got;
    }

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(List<Component> out) {
        Grid.nodeLines((ServerLevel) level, worldPosition, CableTier.COPPER, 4, out);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<WoodenPoleBlockEntity>("idle", 0, test -> test.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

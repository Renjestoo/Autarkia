package dev.autarkia.equipment;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.Autarkia;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An FE tool with a GeckoLib model (07 section 4.2): model, animation and texture {@code item/<model>_geo}. The
 * controller loops {@code idle}; the server triggers the work animation (drill {@code spin}, chainsaw {@code run},
 * rifle {@code fire}) when the tool works.
 */
public class PoweredGeoItem extends PoweredItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String model;
    private final String work;

    public PoweredGeoItem(Properties props, int capacity, int input, int blockCost, String model, String work) {
        super(props, capacity, input, blockCost);
        this.model = model;
        this.work = work;
        GeoItem.registerSyncedAnimatable(this);
    }

    /** Play the work animation of the stack for everyone tracking the holder. */
    public void playWork(LivingEntity holder, ItemStack stack) {
        if (holder.level() instanceof ServerLevel sl) {
            triggerAnim(holder, GeoItem.getOrAssignId(stack, sl), "main", work);
        }
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (energy(stack) > 0) {
            playWork(miner, stack);
        }
        return super.mineBlock(stack, level, state, pos, miner);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        var main = new AnimationController<PoweredGeoItem>("main", 2, test -> test.setAndContinue(RawAnimation.begin().thenLoop("animation." + model + ".idle")));
        main.triggerableAnim(work, RawAnimation.begin().thenPlay("animation." + model + "." + work));
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<PoweredGeoItem> renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new GeoItemRenderer<>(new DefaultedItemGeoModel<PoweredGeoItem>(Autarkia.id(model)).withAltTexture(Autarkia.id(model + "_geo")));
                }
                return renderer;
            }
        });
    }
}

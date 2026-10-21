package dev.autarkia.equipment;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.model.DefaultedGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import dev.autarkia.Autarkia;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import org.jspecify.annotations.Nullable;

/**
 * Aegis armor piece (07 sections 1.7 and 2.2): unbreakable, 10,000,000 FE, every exo rig, frontier suit and lattice
 * carapace function of its slot. While a powered domain node tethers the wearer ({@link AegisTether}) the functions
 * cost no FE: the FE the inherited functions take from the worn suit in a tick is given back to the piece, and jumps
 * and fall cancels cost nothing; without the tether they use the piece buffer at the exo and frontier rates (fall
 * cancel 50 FE per block, decision G13). Any Aegis item of a player charges from the tether at 50,000 FE/t; external
 * chargers insert at most 50,000 FE/t. GeckoLib armor model {@code armor/aegis} with the orbit rings ({@code idle}), the
 * heel thrust ({@code jet}) and a GeckoLib {@code AutoGlowingGeoLayer} from {@code textures/armor/aegis_glowmask.png}.
 * The layer is added through raw types: GeckoLib gives the humanoid render state its {@code GeoRenderState} interface
 * at runtime, which the compile-time type bounds of the armor renderer cannot express.
 */
public class AegisArmorItem extends LatticeCarapaceItem {
    public static final int CAPACITY = 10000000;
    public static final int INPUT = AegisTether.CHARGE;

    public AegisArmorItem(Properties props, ArmorType type) {
        super(props, type, CAPACITY, INPUT, "aegis");
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        AegisTether.charge(level, entity, stack);
        if (!(entity instanceof LivingEntity le) || slot != armorType().getSlot() || !AegisTether.tethered(level, entity)) {
            super.inventoryTick(stack, level, entity, slot);
            return;
        }
        long before = suitEnergy(le);
        super.inventoryTick(stack, level, entity, slot);
        long spent = before - suitEnergy(le);
        if (spent > 0) {
            PoweredItem.setEnergy(stack, (int) Math.min(Integer.MAX_VALUE, PoweredItem.energy(stack) + spent));
        }
    }

    @Override
    protected boolean costFree(LivingEntity le) {
        return le.level() instanceof ServerLevel sl && AegisTether.tethered(sl, le);
    }

    private static long suitEnergy(LivingEntity le) {
        long n = 0;
        for (ItemStack s : worn(le)) {
            n += PoweredItem.energy(s);
        }
        return n;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        var main = new AnimationController<AegisArmorItem>("main", 2, test -> test.setAndContinue(RawAnimation.begin().thenLoop("animation.aegis.idle")));
        main.triggerableAnim("jet", RawAnimation.begin().thenPlay("animation.aegis.jet"));
        controllers.add(main);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<AegisArmorItem, net.minecraft.client.renderer.entity.state.HumanoidRenderState> renderer;

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
                if (renderer == null) {
                    renderer = new GeoArmorRenderer<AegisArmorItem, net.minecraft.client.renderer.entity.state.HumanoidRenderState>(
                        new DefaultedGeoModel<AegisArmorItem>(Autarkia.id("aegis")) {
                            @Override
                            protected String subtype() {
                                return "armor";
                            }
                        });
                    glow(renderer);
                }
                return renderer;
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void glow(GeoArmorRenderer<AegisArmorItem, net.minecraft.client.renderer.entity.state.HumanoidRenderState> renderer) {
        renderer.withRenderLayer((GeoRenderLayer) new AutoGlowingGeoLayer(renderer));
    }
}

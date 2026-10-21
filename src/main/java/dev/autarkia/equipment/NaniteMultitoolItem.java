package dev.autarkia.equipment;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaComponents;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jspecify.annotations.Nullable;

/**
 * {@code nanite_multitool} (07 sections 1.6 and 2.1): pickaxe, axe, shovel, hoe, shears and sword-efficient blocks at
 * netherite level and speed 24, 10,000,000 FE charged at 20,000 FE/t, 100 FE per block. Mode key: 1x1 or 3x3 (through
 * {@link AreaMining}, x0.5 speed, decision G12). Use runs the vanilla block transformers through the registry holder
 * (the NeoForge 2-argument {@code transformBlock}, so data-map appenders of other mods apply): sneaking tills
 * ({@code HOE}), otherwise {@code AXE}, then {@code SHOVEL} when the axe passes; sneak + use on a {@code matter_buffer}
 * links it instead ({@link MatterLink}). At 0 FE the transforms and the shears abilities stop. The GeckoLib model shows
 * the pick, axe and shovel heads in 1x1 and the wide lattice head in 3x3. FE and cache changes do not replay the
 * equip animation.
 */
public class NaniteMultitoolItem extends PoweredGeoItem implements ModalItem {
    public static final int CAPACITY = 10000000;
    public static final int INPUT = 20000;
    public static final int BLOCK_COST = 100;
    public static final float AREA_SPEED = 0.5f;
    private static final List<String> SINGLE_HEADS = List.of("head_pick", "head_axe", "head_shovel");
    private static final String AREA_HEAD = "head_area";

    public NaniteMultitoolItem(Properties props) {
        super(props, CAPACITY, INPUT, BLOCK_COST, "nanite_multitool", "use");
    }

    @Override
    public List<ToolMode> modes() {
        return List.of(ToolMode.SINGLE, ToolMode.AREA3);
    }

    @Override
    public float areaSpeed(ToolMode mode) {
        return mode == ToolMode.SINGLE ? 1f : AREA_SPEED;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        InteractionResult link = MatterLink.tryLink(ctx);
        if (link != InteractionResult.PASS) {
            return link;
        }
        if (energy(ctx.getItemInHand()) <= 0) {
            return InteractionResult.PASS;
        }
        HolderLookup.RegistryLookup<BlockTransformer> transformers = ctx.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER);
        if (ctx.isSecondaryUseActive()) {
            return transform(ctx, transformers, BlockTransformers.HOE);
        }
        InteractionResult axe = transform(ctx, transformers, BlockTransformers.AXE);
        return axe != InteractionResult.PASS ? axe : transform(ctx, transformers, BlockTransformers.SHOVEL);
    }

    private static InteractionResult transform(UseOnContext ctx, HolderLookup.RegistryLookup<BlockTransformer> lookup, ResourceKey<BlockTransformer> key) {
        var holder = lookup.getOrThrow(key);
        return holder.value().transformBlock(ctx, holder);
    }

    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility ability) {
        return stack.getOrDefault(AutarkiaComponents.ENERGY.get(), 0) > 0
            && (ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(ability) || ability == ItemAbilities.SHOVEL_DOUSE);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        MatterLink.flow(level, entity, stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, display, out, flag);
        MatterLink.tooltip(stack, out);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<NaniteMultitoolItem> renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new HeadRenderer();
                }
                return renderer;
            }
        });
    }

    /** The multitool renderer: hides the heads that do not belong to the stack's mode after the animations run. */
    private static final class HeadRenderer extends GeoItemRenderer<NaniteMultitoolItem> {
        private final DataTicket<Boolean> area = DataTicket.create("autarkia_multitool_area", Boolean.class);

        HeadRenderer() {
            super(new DefaultedItemGeoModel<NaniteMultitoolItem>(Autarkia.id("nanite_multitool")).withAltTexture(Autarkia.id("nanite_multitool_geo")));
        }

        @Override
        public void addRenderData(NaniteMultitoolItem item, GeoItemRenderer.RenderData related, GeoRenderState state, float partialTick) {
            super.addRenderData(item, related, state, partialTick);
            state.addGeckolibData(area, related.itemStack().get(AutarkiaComponents.TOOL_MODE.get()) == ToolMode.AREA3);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> info, BoneSnapshots snapshots) {
            super.adjustModelBonesForRender(info, snapshots);
            boolean wide = info.renderState().getOrDefaultGeckolibData(area, false);
            for (String head : SINGLE_HEADS) {
                snapshots.ifPresent(head, b -> b.skipRender(wide));
            }
            snapshots.ifPresent(AREA_HEAD, b -> b.skipRender(!wide));
        }
    }
}

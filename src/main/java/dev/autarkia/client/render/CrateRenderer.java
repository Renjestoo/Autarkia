package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.autarkia.storage.CrateBlockEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Item piles on top of a crate (04 section 2.1): a 2x2 grid, pile n shows slot group n (slots 0-6, 7-13, 14-20,
 * 21-26) with its most common item, 1 to 4 layers by fill. No piles when the block above is not air. Lit with the
 * light of the block above (the crate itself is opaque).
 */
public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity, CrateRenderer.State> {
    private static final int[][] GROUPS = {{0, 7}, {7, 14}, {14, 21}, {21, 27}};
    private final ItemModelResolver items;

    /** Render state: one item and layer count per pile. */
    public static class State extends BlockEntityRenderState {
        final ItemStackRenderState[] piles = new ItemStackRenderState[4];
        final int[] layers = new int[4];
        boolean visible;
        int light;
    }

    public CrateRenderer(BlockEntityRendererProvider.Context ctx) {
        this.items = ctx.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrateBlockEntity be, State s, float pt, Vec3 cam, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, crumbling);
        s.visible = be.getLevel() != null && be.getLevel().getBlockState(be.getBlockPos().above()).isAir();
        if (!s.visible) {
            return;
        }
        s.light = LightCoordsUtil.pack(be.getLevel().getBrightness(LightLayer.BLOCK, be.getBlockPos().above()),
            be.getLevel().getBrightness(LightLayer.SKY, be.getBlockPos().above()));
        for (int g = 0; g < 4; g++) {
            Map<Item, Integer> count = new HashMap<>();
            int total = 0;
            int max = 0;
            Map<Item, ItemStack> sample = new HashMap<>();
            for (int i = GROUPS[g][0]; i < GROUPS[g][1]; i++) {
                ItemStack st = be.slot(i);
                max += st.isEmpty() ? 64 : st.getMaxStackSize();
                if (st.isEmpty()) {
                    continue;
                }
                total += st.getCount();
                count.merge(st.getItem(), st.getCount(), Integer::sum);
                sample.putIfAbsent(st.getItem(), st);
            }
            ItemStack best = count.entrySet().stream().max(Map.Entry.comparingByValue()).map(e -> sample.get(e.getKey())).orElse(ItemStack.EMPTY);
            s.layers[g] = total == 0 ? 0 : Math.max(1, Math.min(4, (int) Math.ceil(4.0 * total / max)));
            if (s.piles[g] == null) {
                s.piles[g] = new ItemStackRenderState();
            }
            items.updateForTopItem(s.piles[g], best, ItemDisplayContext.FIXED, be.getLevel(), null, (int) be.getBlockPos().asLong() + g);
        }
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        if (!s.visible) {
            return;
        }
        for (int g = 0; g < 4; g++) {
            if (s.layers[g] == 0 || s.piles[g] == null || s.piles[g].isEmpty()) {
                continue;
            }
            double cx = 0.27 + (g % 2) * 0.46;
            double cz = 0.27 + (g / 2) * 0.46;
            for (int l = 0; l < s.layers[g]; l++) {
                pose.pushPose();
                pose.translate(cx, 1.02 + l * 0.055, cz);
                pose.rotateDegrees(Axis.YP, (g * 37 + l * 53) % 360);
                pose.rotateDegrees(Axis.XP, 90f);
                pose.scale(0.38f, 0.38f, 0.38f);
                s.piles[g].submit(pose, out, s.light, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
        }
    }
}

package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.storage.RackBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Crates on rack shelves (04 section 2.3.1): one crate per occupied position, stacked bottom to top; the crates sink
 * with the bend stage and shake while the rack is critical, harder as the collapse nears.
 */
public class RackRenderer implements BlockEntityRenderer<RackBlockEntity, RackRenderer.State> {
    private final ItemModelResolver items;

    /** Render state: crate models per position, facing, stage, collapse countdown and animation time. */
    public static class State extends BlockEntityRenderState {
        ItemStackRenderState[] crates = new ItemStackRenderState[0];
        boolean[] present = new boolean[0];
        Direction facing = Direction.NORTH;
        int stage;
        int secondsLeft;
        float time;
    }

    public RackRenderer(BlockEntityRendererProvider.Context ctx) {
        this.items = ctx.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RackBlockEntity be, State s, float pt, Vec3 cam, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, crumbling);
        int n = be.positionCount();
        if (s.crates.length != n) {
            s.crates = new ItemStackRenderState[n];
            s.present = new boolean[n];
        }
        for (int i = 0; i < n; i++) {
            s.present[i] = be.present(i);
            if (s.crates[i] == null) {
                s.crates[i] = new ItemStackRenderState();
            }
            if (s.present[i]) {
                items.updateForTopItem(s.crates[i], be.crate(i), ItemDisplayContext.FIXED, be.getLevel(), null, (int) be.getBlockPos().asLong() + i);
            }
        }
        s.facing = MachineBlock.front(be.getBlockState());
        s.stage = be.stage();
        s.secondsLeft = be.clientSecondsLeft();
        s.time = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + pt;
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        int n = s.crates.length;
        float cell = 0.96f / Math.max(1, n);
        float size = cell * 0.9f;
        float sag = s.stage / 16f;
        float shake = s.stage == 3 ? 0.006f + 0.02f * (1f - Math.min(60, s.secondsLeft) / 60f) : 0f;
        for (int i = 0; i < n; i++) {
            if (!s.present[i]) {
                continue;
            }
            pose.pushPose();
            pose.translate(0.5, 0.04 + i * cell + size / 2 - sag * 0.5 * (i + 1) / n, 0.5);
            pose.rotateDegrees(Axis.YP, -s.facing.toYRot());
            if (shake > 0) {
                pose.translate(Math.sin(s.time * 2.7 + i) * shake, 0, Math.cos(s.time * 3.1 + i) * shake);
                pose.rotateDegrees(Axis.ZP, (float) Math.sin(s.time * 1.9 + i) * shake * 60);
            }
            pose.scale(size * 2, size * 2, size * 2);
            s.crates[i].submit(pose, out, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}

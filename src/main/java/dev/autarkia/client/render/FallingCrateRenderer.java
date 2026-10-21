package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.autarkia.storage.FallingCrateEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;

/**
 * The falling crate (04 section 4.8): the {@code crate} block state drawn like a falling block, 0.875 blocks wide,
 * tumbling about two axes at rates taken from its seed.
 */
public class FallingCrateRenderer extends EntityRenderer<FallingCrateEntity, FallingCrateRenderer.State> {
    /** Falling block render state plus the tumble angles. */
    public static class State extends FallingBlockRenderState {
        float pitch;
        float roll;
    }

    public FallingCrateRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.45f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FallingCrateEntity e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        BlockPos pos = BlockPos.containing(e.getX(), e.getBoundingBox().maxY, e.getZ());
        s.movingBlockRenderState.randomSeedPos = pos;
        s.movingBlockRenderState.blockPos = pos;
        s.movingBlockRenderState.blockState = e.renderState();
        if (e.level() instanceof ClientLevel cl) {
            s.movingBlockRenderState.biome = cl.getBiome(pos);
            s.movingBlockRenderState.cardinalLighting = cl.cardinalLighting();
            s.movingBlockRenderState.lightEngine = cl.getLightEngine();
        }
        float t = e.tickCount + pt;
        int seed = e.seed();
        s.pitch = t * (6 + (seed & 7));
        s.roll = t * (4 + (seed >> 3 & 7)) * ((seed & 64) == 0 ? 1 : -1);
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0, 0.4375, 0);
        pose.rotateDegrees(Axis.XP, s.pitch);
        pose.rotateDegrees(Axis.ZP, s.roll);
        pose.scale(0.875f, 0.875f, 0.875f);
        pose.translate(-0.5, -0.5, -0.5);
        out.submitMovingBlock(pose, s.movingBlockRenderState, s.outlineColor);
        pose.popPose();
        super.submit(s, pose, out, camera);
    }
}

package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.autarkia.colony.RisingBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.RenderShape;

/**
 * The {@code rising_block} (05 section 8.2): its block state drawn like a falling block at its target, growing from its
 * bottom center from nothing to full size. Big blocks without a block model draw nothing here (the entity shows a glow
 * column for them).
 */
public class RisingBlockRenderer extends EntityRenderer<RisingBlockEntity, RisingBlockRenderer.State> {
    /** Falling block render state plus the arrival scale. */
    public static class State extends FallingBlockRenderState {
        float scale;
        boolean visible;
    }

    public RisingBlockRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RisingBlockEntity e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.movingBlockRenderState.randomSeedPos = e.target();
        s.movingBlockRenderState.blockPos = e.target();
        s.movingBlockRenderState.blockState = e.blockState();
        if (e.level() instanceof ClientLevel cl) {
            s.movingBlockRenderState.biome = cl.getBiome(e.target());
            s.movingBlockRenderState.cardinalLighting = cl.cardinalLighting();
            s.movingBlockRenderState.lightEngine = cl.getLightEngine();
        }
        s.scale = e.progress(pt);
        s.visible = e.blockState().getRenderShape() == RenderShape.MODEL && s.scale > 0.01f;
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        if (s.visible) {
            pose.pushPose();
            pose.scale(s.scale, s.scale, s.scale);
            pose.translate(-0.5, 0, -0.5);
            out.submitMovingBlock(pose, s.movingBlockRenderState, s.outlineColor);
            pose.popPose();
        }
        super.submit(s, pose, out, camera);
    }
}

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
 * The {@code rising_block} (05 sections 8.2 and 8.4): its block state drawn like a falling block. SCALE grows it at its
 * target from its bottom center from nothing to full size; RISE draws the normal model at full size, starting at the
 * entity position 3 blocks below the target and moving up to the target, so it shows as it leaves the soil (G19). Big
 * blocks without a block model draw nothing here (the entity shows a glow column for them).
 */
public class RisingBlockRenderer extends EntityRenderer<RisingBlockEntity, RisingBlockRenderer.State> {
    /** Falling block render state plus the arrival scale and the rise offset above the entity position. */
    public static class State extends FallingBlockRenderState {
        float scale;
        float lift;
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
        boolean rise = e.mode() == RisingBlockEntity.Mode.RISE;
        s.scale = rise ? 1f : e.progress(pt);
        s.lift = rise ? RisingBlockEntity.RISE_DEPTH - e.depth(pt) : 0f;
        s.visible = e.blockState().getRenderShape() == RenderShape.MODEL && s.scale > 0.01f;
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        if (s.visible) {
            pose.pushPose();
            pose.translate(0, s.lift, 0);
            pose.scale(s.scale, s.scale, s.scale);
            pose.translate(-0.5, 0, -0.5);
            out.submitMovingBlock(pose, s.movingBlockRenderState, s.outlineColor);
            pose.popPose();
        }
        super.submit(s, pose, out, camera);
    }
}

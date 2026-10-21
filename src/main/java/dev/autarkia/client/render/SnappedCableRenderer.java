package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.SnappedCableEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a snapped cable end as its 8-point verlet chain from the anchor to the whipping tip, in the cable texture, lit
 * by the brightest chain point (the tip may swing into the ground).
 */
public class SnappedCableRenderer extends EntityRenderer<SnappedCableEntity, SnappedCableRenderer.State> {
    /** Render state: chain points relative to the entity position. */
    public static class State extends EntityRenderState {
        Vec3[] points = new Vec3[0];
        CableTier tier = CableTier.ROPE;
    }

    public SnappedCableRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected AABB getBoundingBoxForCulling(SnappedCableEntity e, float pt) {
        return new AABB(e.anchor(), e.position()).inflate(e.half() + 0.5);
    }

    @Override
    public void extractRenderState(SnappedCableEntity e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.tier = e.tier();
        Vec3 origin = e.getPosition(pt);
        if (e.chain[0] == null) {
            s.points = new Vec3[] {e.anchor().subtract(origin), Vec3.ZERO};
        } else {
            s.points = new Vec3[e.chain.length];
            for (int i = 0; i < e.chain.length; i++) {
                Vec3 p = e.chainOld[i].lerp(e.chain[i], pt);
                s.points[i] = (i == e.chain.length - 1 ? origin : p).subtract(origin);
            }
            s.points[0] = e.anchor().subtract(origin);
        }
        int block = 0;
        int sky = 0;
        for (Vec3 p : s.points) {
            BlockPos lp = BlockPos.containing(origin.add(p));
            block = Math.max(block, e.level().getBrightness(LightLayer.BLOCK, lp));
            sky = Math.max(sky, e.level().getBrightness(LightLayer.SKY, lp));
        }
        s.lightCoords = LightCoordsUtil.pack(block, sky);
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        super.submit(s, pose, out, camera);
        var rt = s.tier == CableTier.ROPE ? RenderTypes.entityCutout(ClientSpans.texture(s.tier)) : RenderTypes.entityTranslucentEmissive(ClientSpans.texture(s.tier));
        Vec3[] pts = s.points;
        int light = s.lightCoords;
        out.submitCustomGeometry(pose, rt, (p, vc) -> ClientSpans.tube(vc, p, pts, s.tier.thickness, 0xFFFFFFFF, light, 0));
    }
}

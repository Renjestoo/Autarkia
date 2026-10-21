package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.autarkia.Autarkia;
import dev.autarkia.equipment.GrappleHookEntity;
import dev.autarkia.equipment.GrappleLauncherItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The {@code grapple_hook} (07 section 4.2): the hook texture as a camera-facing sprite and a straight steel line from
 * the hook to the hand that holds the launcher (first person: the screen-space hand position like the vanilla fishing
 * line; third person: the side of the body).
 */
public class GrappleHookRenderer extends EntityRenderer<GrappleHookEntity, GrappleHookRenderer.State> {
    private static final Identifier HOOK = Autarkia.id("textures/entity/grapple_hook.png");
    private static final Identifier LINE = Autarkia.id("textures/entity/cable/gondola_cable.png");
    private static final float SIZE = 0.5f;
    private static final float LINE_THICKNESS = 0.05f;

    /** Render state: the hand position relative to the hook, or null without an owner. */
    public static class State extends EntityRenderState {
        Vec3 hand;
    }

    public GrappleHookRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected AABB getBoundingBoxForCulling(GrappleHookEntity e, float pt) {
        AABB box = e.getBoundingBox().inflate(0.5);
        return e.getOwner() instanceof Player p ? box.minmax(p.getBoundingBox()) : box;
    }

    @Override
    public void extractRenderState(GrappleHookEntity e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.hand = e.getOwner() instanceof Player p ? handPos(p, pt).subtract(e.getPosition(pt)) : null;
    }

    private Vec3 handPos(Player p, float pt) {
        HumanoidArm arm = p.getMainHandItem().getItem() instanceof GrappleLauncherItem ? p.getMainArm() : p.getMainArm().getOpposite();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        if (entityRenderDispatcher.options.getCameraType().isFirstPerson() && p == Minecraft.getInstance().player) {
            float fov = entityRenderDispatcher.options.fov().get();
            Vec3 offset = entityRenderDispatcher.camera.getNearPlane(fov).getPointOnPlane(side * 0.525f, -0.1f).scale(960.0 / fov);
            return p.getEyePosition(pt).add(offset);
        }
        float yaw = Mth.lerp(pt, p.yBodyRotO, p.yBodyRot) * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        double scale = p.getScale();
        double across = side * 0.35 * scale;
        double ahead = 0.8 * scale;
        double drop = (p.isCrouching() ? -0.1875 : 0) - 0.45 * scale;
        return p.getEyePosition(pt).add(-cos * across - sin * ahead, drop, -sin * across + cos * ahead);
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        int light = s.lightCoords;
        pose.pushPose();
        pose.rotate(camera.orientation);
        pose.scale(SIZE, SIZE, SIZE);
        out.submitCustomGeometry(pose, RenderTypes.entityCutout(HOOK), (p, vc) -> {
            vertex(vc, p, -0.5f, -0.5f, 0f, 1f, light);
            vertex(vc, p, 0.5f, -0.5f, 1f, 1f, light);
            vertex(vc, p, 0.5f, 0.5f, 1f, 0f, light);
            vertex(vc, p, -0.5f, 0.5f, 0f, 0f, light);
        });
        pose.popPose();
        if (s.hand != null) {
            Vec3[] pts = {Vec3.ZERO, s.hand};
            out.submitCustomGeometry(pose, RenderTypes.entityCutout(LINE), (p, vc) -> ClientSpans.tube(vc, p, pts, LINE_THICKNESS, 0xFFFFFFFF, light, 0));
        }
        super.submit(s, pose, out, camera);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v, int light) {
        vc.addVertex(pose, x, y, 0f).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f);
    }
}

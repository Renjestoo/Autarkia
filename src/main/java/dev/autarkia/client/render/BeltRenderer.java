package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.autarkia.logistics.BeltBlock;
import dev.autarkia.logistics.BeltBlockEntity;
import dev.autarkia.logistics.TransitSegment;
import dev.autarkia.registry.MachineBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Items on a belt (04 section 0.3): each entry of this belt's span lies flat at its interpolated position from the back
 * to the front edge, rising on an {@code up} belt and falling on a {@code down} belt; the end belt of a jammed line shows
 * a red warning stripe at its front edge. While the belt runs (powered and not jammed) a copy of the belt top texture
 * scrolls toward the front just above the static top at the belt speed; a stopped belt shows only the static top.
 */
public class BeltRenderer implements BlockEntityRenderer<BeltBlockEntity, BeltRenderer.State> {
    private static final Identifier STRIPE = Identifier.withDefaultNamespace("textures/block/red_concrete.png");
    private static final Identifier SURFACE = dev.autarkia.Autarkia.id("textures/block/belt_top.png");
    private static final float TOP = 0.32f;
    private static final float RAMP = 0.14f;
    private static final float FLAT_SURFACE = 5f / 16f + 0.004f;
    private static final float RAMP_SURFACE = 0.633f + 0.012f;
    private final ItemModelResolver items;

    /** Render state: one item and path fraction per entry, facing, slope, the jam flag and the surface scroll (0 to 1) while running. */
    public static class State extends BlockEntityRenderState {
        final List<ItemStackRenderState> stacks = new ArrayList<>();
        final List<Float> along = new ArrayList<>();
        Direction facing = Direction.NORTH;
        BeltBlock.Slope slope = BeltBlock.Slope.FLAT;
        boolean jammed;
        boolean running;
        float scroll;
    }

    public BeltRenderer(BlockEntityRendererProvider.Context ctx) {
        this.items = ctx.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BeltBlockEntity be, State s, float pt, Vec3 cam, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, crumbling);
        s.facing = MachineBlock.front(be.getBlockState());
        s.slope = be.getBlockState().getValue(BeltBlock.SLOPE);
        s.jammed = be.jammed();
        s.running = be.powered() && !be.jammed() && be.getLevel() != null;
        if (s.running) {
            double t = (be.getLevel().getGameTime() + pt) * dev.autarkia.config.AutarkiaConfig.BELT_SPEED_PU.get() / (double) BeltBlockEntity.LENGTH;
            s.scroll = (float) (t - Math.floor(t));
        }
        s.stacks.clear();
        s.along.clear();
        TransitSegment seg = be.segment();
        for (TransitSegment.Entry e : seg.entries) {
            ItemStackRenderState r = new ItemStackRenderState();
            items.updateForTopItem(r, e.stack, ItemDisplayContext.GROUND, be.getLevel(), null, e.id);
            s.stacks.add(r);
            float p = be.powered() ? e.prevPos + (e.pos - e.prevPos) * pt : e.pos;
            s.along.add(Math.min(1f, p / seg.length));
        }
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        float tilt = s.slope == BeltBlock.Slope.UP ? -45f : s.slope == BeltBlock.Slope.DOWN ? 45f : 0f;
        for (int i = 0; i < s.stacks.size(); i++) {
            float t = s.along.get(i);
            float y = switch (s.slope) {
                case UP -> RAMP + t;
                case DOWN -> RAMP + 1 - t;
                default -> TOP;
            };
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.rotateDegrees(Axis.YP, -s.facing.toYRot());
            pose.translate(0, y + 0.02, -0.5 + t);
            pose.rotateDegrees(Axis.XP, 90f + tilt);
            pose.scale(0.45f, 0.45f, 0.45f);
            s.stacks.get(i).submit(pose, out, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        if (s.running) {
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.rotateDegrees(Axis.YP, -s.facing.toYRot());
            out.submitCustomGeometry(pose, RenderTypes.entityCutout(SURFACE), (p, vc) -> surface(vc, p, s.slope, s.scroll, s.lightCoords));
            pose.popPose();
        }
        if (s.jammed) {
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.rotateDegrees(Axis.YP, -s.facing.toYRot());
            out.submitCustomGeometry(pose, RenderTypes.entityCutout(STRIPE), (p, vc) -> stripe(vc, p, s.slope == BeltBlock.Slope.UP ? RAMP + 1 : s.slope == BeltBlock.Slope.DOWN ? RAMP : TOP));
            pose.popPose();
        }
    }

    private static void surface(VertexConsumer vc, PoseStack.Pose p, BeltBlock.Slope slope, float scroll, int light) {
        piece(vc, p, slope, 0f, 1f - scroll, scroll, 1f, light);
        if (scroll > 0) {
            piece(vc, p, slope, 1f - scroll, 1f, 0f, scroll, light);
        }
    }

    private static void piece(VertexConsumer vc, PoseStack.Pose p, BeltBlock.Slope slope, float from, float to, float v0, float v1, int light) {
        float k = slope == BeltBlock.Slope.UP ? 1f : slope == BeltBlock.Slope.DOWN ? -1f : 0f;
        float base = k == 0 ? FLAT_SURFACE : RAMP_SURFACE;
        float zf = 0.5f - from;
        float zb = 0.5f - to;
        float yf = base + k * zf;
        float yb = base + k * zb;
        float ny = (float) (1 / Math.sqrt(1 + k * k));
        float[][] quad = {{-0.5f, yb, zb, 0, v1}, {-0.5f, yf, zf, 0, v0}, {0.5f, yf, zf, 1, v0}, {0.5f, yb, zb, 1, v1}};
        for (float[] q : quad) {
            vc.addVertex(p, q[0], q[1], q[2]).setColor(0xFFFFFFFF).setUv(q[3], q[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(p, 0, ny, -k * ny);
        }
    }

    private static void stripe(VertexConsumer vc, PoseStack.Pose p, float y) {
        float h = y + 0.012f;
        float[][] quad = {{-0.5f, h, 0.36f}, {-0.5f, h, 0.48f}, {0.5f, h, 0.48f}, {0.5f, h, 0.36f}};
        float[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        for (int k = 0; k < 4; k++) {
            vc.addVertex(p, quad[k][0], quad[k][1], quad[k][2]).setColor(0xFFFF3030).setUv(uv[k][0], uv[k][1]).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT).setNormal(p, 0, 1, 0);
        }
    }
}

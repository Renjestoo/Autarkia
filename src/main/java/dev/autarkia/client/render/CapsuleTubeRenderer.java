package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.autarkia.logistics.Capsule;
import dev.autarkia.logistics.CapsuleNetworks;
import dev.autarkia.logistics.CapsuleTopology;
import dev.autarkia.logistics.CapsuleTubeBlockEntity;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Capsules inside one tube block (04 section 0.3): each capsule whose interpolated path position lies in this block
 * draws its item at that point, turned to its travel direction. While its network is CLOGGED, a tube with a waiting
 * capsule glows red.
 */
public class CapsuleTubeRenderer implements BlockEntityRenderer<CapsuleTubeBlockEntity, CapsuleTubeRenderer.State> {
    private static final Identifier GLOW = Identifier.withDefaultNamespace("textures/block/red_stained_glass.png");
    private static final float LO = 4.8f / 16f;
    private static final float HI = 11.2f / 16f;
    private final ItemModelResolver items;

    /** Render state: item, offset in the block and yaw per capsule, and the red glow flag. */
    public static class State extends BlockEntityRenderState {
        final List<ItemStackRenderState> stacks = new ArrayList<>();
        final List<Vec3> at = new ArrayList<>();
        final List<Float> yaw = new ArrayList<>();
        boolean glow;
    }

    public CapsuleTubeRenderer(BlockEntityRendererProvider.Context ctx) {
        this.items = ctx.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CapsuleTubeBlockEntity be, State s, float pt, Vec3 cam, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, crumbling);
        s.stacks.clear();
        s.at.clear();
        s.yaw.clear();
        s.glow = false;
        BlockPos pos = be.getBlockPos();
        long self = pos.asLong();
        for (Capsule c : CapsuleNetworks.clientAt(be.getLevel(), pos)) {
            float p = c.prevPos + (c.pos - c.prevPos) * pt;
            int i = c.occIndex((int) Math.floor(p));
            if (c.blocks()[i] != self) {
                continue;
            }
            s.glow |= c.glow;
            ItemStackRenderState r = new ItemStackRenderState();
            items.updateForTopItem(r, c.stack, ItemDisplayContext.FIXED, be.getLevel(), null, c.id);
            s.stacks.add(r);
            s.at.add(c.point(p).subtract(Vec3.atLowerCornerOf(pos)));
            Direction d = c.path.length == 0 ? Direction.NORTH : CapsuleTopology.dir(c.path[i]);
            s.yaw.add(d.getAxis().isHorizontal() ? -d.toYRot() : 0f);
        }
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        for (int i = 0; i < s.stacks.size(); i++) {
            Vec3 v = s.at.get(i);
            pose.pushPose();
            pose.translate(v.x, v.y, v.z);
            pose.rotateDegrees(Axis.YP, s.yaw.get(i) + 90f);
            pose.scale(0.55f, 0.55f, 0.55f);
            s.stacks.get(i).submit(pose, out, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        if (s.glow) {
            out.submitCustomGeometry(pose, RenderTypes.entityTranslucent(GLOW), (p, vc) -> box(vc, p));
        }
    }

    private static void box(VertexConsumer vc, PoseStack.Pose p) {
        float[][][] faces = {
            {{LO, LO, LO}, {HI, LO, LO}, {HI, LO, HI}, {LO, LO, HI}, {0, -1, 0}},
            {{LO, HI, HI}, {HI, HI, HI}, {HI, HI, LO}, {LO, HI, LO}, {0, 1, 0}},
            {{HI, LO, LO}, {LO, LO, LO}, {LO, HI, LO}, {HI, HI, LO}, {0, 0, -1}},
            {{LO, LO, HI}, {HI, LO, HI}, {HI, HI, HI}, {LO, HI, HI}, {0, 0, 1}},
            {{LO, LO, LO}, {LO, LO, HI}, {LO, HI, HI}, {LO, HI, LO}, {-1, 0, 0}},
            {{HI, LO, HI}, {HI, LO, LO}, {HI, HI, LO}, {HI, HI, HI}, {1, 0, 0}}};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (float[][] f : faces) {
            for (int k = 0; k < 4; k++) {
                vc.addVertex(p, f[k][0], f[k][1], f[k][2]).setColor(0xB0FF3030).setUv(uv[k][0], uv[k][1]).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(p, f[4][0], f[4][1], f[4][2]);
            }
        }
    }
}

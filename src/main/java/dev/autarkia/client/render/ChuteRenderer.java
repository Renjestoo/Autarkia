package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.autarkia.logistics.ChuteBlockEntity;
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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Items sliding down a chute (04 section 0.3): each entry is drawn at its interpolated path position from the back top
 * to the front bottom; a stuck front entry is drawn tilted.
 */
public class ChuteRenderer implements BlockEntityRenderer<ChuteBlockEntity, ChuteRenderer.State> {
    private final ItemModelResolver items;

    /** Render state: one item and path fraction per entry. */
    public static class State extends BlockEntityRenderState {
        final List<ItemStackRenderState> stacks = new ArrayList<>();
        final List<Float> along = new ArrayList<>();
        Direction facing = Direction.NORTH;
        boolean stuck;
    }

    public ChuteRenderer(BlockEntityRendererProvider.Context ctx) {
        this.items = ctx.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChuteBlockEntity be, State s, float pt, Vec3 cam, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, crumbling);
        s.facing = MachineBlock.front(be.getBlockState());
        s.stuck = be.stuck();
        s.stacks.clear();
        s.along.clear();
        TransitSegment seg = be.segment();
        for (TransitSegment.Entry e : seg.entries) {
            ItemStackRenderState r = new ItemStackRenderState();
            items.updateForTopItem(r, e.stack, ItemDisplayContext.GROUND, be.getLevel(), null, e.id);
            s.stacks.add(r);
            float p = e.prevPos + (e.pos - e.prevPos) * pt;
            s.along.add(Math.min(1f, p / seg.length));
        }
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        for (int i = 0; i < s.stacks.size(); i++) {
            float t = s.along.get(i);
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.rotateDegrees(Axis.YP, -s.facing.toYRot());
            pose.translate(0, 0.98 - 0.8 * t, -0.42 + 0.84 * t);
            pose.rotateDegrees(Axis.XP, 38.66f);
            if (i == 0 && s.stuck) {
                pose.rotateDegrees(Axis.ZP, 25f);
            }
            pose.scale(0.6f, 0.6f, 0.6f);
            s.stacks.get(i).submit(pose, out, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}

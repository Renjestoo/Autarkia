package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.autarkia.logistics.FilterBlockEntity;
import dev.autarkia.logistics.TransitSegment;
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

/** The buffered entry of a belt splitter or sorting chute (04 section 0.3), drawn flat on its way from the back to the front. */
public class FilterRenderer implements BlockEntityRenderer<FilterBlockEntity, FilterRenderer.State> {
    private final ItemModelResolver items;

    /** Render state: one item and path fraction per entry and the facing. */
    public static class State extends BlockEntityRenderState {
        final List<ItemStackRenderState> stacks = new ArrayList<>();
        final List<Float> along = new ArrayList<>();
        Direction facing = Direction.NORTH;
    }

    public FilterRenderer(BlockEntityRendererProvider.Context ctx) {
        this.items = ctx.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FilterBlockEntity be, State s, float pt, Vec3 cam, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, crumbling);
        s.facing = be.facing();
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
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.rotateDegrees(Axis.YP, -s.facing.toYRot());
            pose.translate(0, 0.65, -0.5 + s.along.get(i));
            pose.rotateDegrees(Axis.XP, 90f);
            pose.scale(0.45f, 0.45f, 0.45f);
            s.stacks.get(i).submit(pose, out, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}

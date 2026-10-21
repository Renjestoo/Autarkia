package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.storage.TallyBoardBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
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
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Draws the 6 largest counts of a tally board on its front face: item icon and chalk-white number in a 2 x 3 grid. */
public class TallyBoardRenderer implements BlockEntityRenderer<TallyBoardBlockEntity, TallyBoardRenderer.State> {
    private final ItemModelResolver items;
    private final Font font;

    /** Render state of one board. */
    public static class State extends BlockEntityRenderState {
        final List<ItemStackRenderState> icons = new ArrayList<>();
        final List<String> counts = new ArrayList<>();
        Direction facing = Direction.NORTH;
        int light;
    }

    public TallyBoardRenderer(BlockEntityRendererProvider.Context ctx) {
        this.items = ctx.itemModelResolver();
        this.font = ctx.font();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TallyBoardBlockEntity be, State s, float pt, Vec3 cam, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, pt, cam, crumbling);
        s.facing = MachineBlock.front(be.getBlockState());
        s.light = LightCoordsUtil.pack(be.getLevel().getBrightness(LightLayer.BLOCK, be.getBlockPos().relative(s.facing)),
            be.getLevel().getBrightness(LightLayer.SKY, be.getBlockPos().relative(s.facing)));
        s.icons.clear();
        s.counts.clear();
        int i = 0;
        for (TallyBoardBlockEntity.Tally t : be.top()) {
            ItemStackRenderState r = new ItemStackRenderState();
            items.updateForTopItem(r, new ItemStack(t.item()), ItemDisplayContext.GUI, be.getLevel(), null, i++);
            s.icons.add(r);
            s.counts.add(format(t.count()));
        }
    }

    private static String format(long n) {
        if (n >= 1_000_000) {
            return String.format(java.util.Locale.ROOT, "%.1fM", n / 1_000_000.0);
        }
        if (n >= 10_000) {
            return String.format(java.util.Locale.ROOT, "%.1fk", n / 1000.0);
        }
        return Long.toString(n);
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.rotateDegrees(Axis.YP, -s.facing.toYRot());
        pose.translate(0, 0, 0.502);
        for (int i = 0; i < s.icons.size(); i++) {
            float x = i % 2 == 0 ? -0.36f : 0.04f;
            float y = 0.27f - (i / 2) * 0.27f;
            pose.pushPose();
            pose.translate(x + 0.07, y, 0.002);
            pose.scale(0.14f, 0.14f, 0.01f);
            s.icons.get(i).submit(pose, out, s.light, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
            pose.pushPose();
            pose.translate(x + 0.16, y + 0.035, 0.004);
            pose.scale(0.0085f, -0.0085f, 0.0085f);
            out.submitText(pose, 0, 0, Component.literal(s.counts.get(i)).getVisualOrderText(), false, Font.DisplayMode.POLYGON_OFFSET,
                s.light, 0xFFF2F2E6, 0, 0);
            pose.popPose();
        }
        pose.popPose();
    }
}

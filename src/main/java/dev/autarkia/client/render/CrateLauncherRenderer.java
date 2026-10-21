package dev.autarkia.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.autarkia.logistics.CrateLauncherBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * GeckoLib renderer of the {@code crate_launcher} (04 section 4.8): the generic machine renderer with model and texture
 * {@code crate_launcher} and its triggerable {@code throw} animation; while a target is set the model turns about its
 * vertical axis so that its arm throws toward the target net instead of along the block facing.
 */
public class CrateLauncherRenderer extends GeoMachineRenderer<CrateLauncherBlockEntity> {
    private static final DataTicket<Float> AIM_YAW = DataTicket.create("autarkia_launcher_aim", Float.class);

    public CrateLauncherRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, "crate_launcher", 1.0);
    }

    @Override
    public void addRenderData(CrateLauncherBlockEntity be, @Nullable Void related, BlockEntityRenderState state, float partialTick) {
        super.addRenderData(be, related, state, partialTick);
        BlockPos t = be.target();
        float yaw = Float.NaN;
        if (t != null) {
            double dx = t.getX() - be.getBlockPos().getX();
            double dz = t.getZ() - be.getBlockPos().getZ();
            yaw = (float) Math.toDegrees(Math.atan2(-dx, -dz));
        }
        ((GeoRenderState) (Object) state).addGeckolibData(AIM_YAW, yaw);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void tryRotateByBlockstate(RenderPassInfo info, PoseStack pose) {
        float yaw = (Float) info.getOrDefaultGeckolibData(AIM_YAW, Float.NaN);
        if (Float.isNaN(yaw)) {
            super.tryRotateByBlockstate(info, pose);
            return;
        }
        pose.rotateDegrees(Axis.YP, yaw);
    }
}

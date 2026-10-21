package dev.autarkia.client.render;

import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.autarkia.logistics.CapsuleStationBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/**
 * GeckoLib renderer of the {@code capsule_station}: the generic machine renderer with model and texture
 * {@code capsule_station}, but the model's top mouth is turned about the block center to the block's {@code facing}
 * (the GeckoLib default turns UP and DOWN about the bottom center and treats NORTH as unrotated).
 */
public class CapsuleStationRenderer extends GeoMachineRenderer<CapsuleStationBlockEntity> {
    public CapsuleStationRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, "capsule_station");
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void tryRotateByBlockstate(RenderPassInfo info, PoseStack pose) {
        Direction mouth = (Direction) info.getOrDefaultGeckolibData(DIRECTION_FACING, Direction.UP);
        if (mouth == Direction.UP) {
            return;
        }
        pose.translate(0, 0.5, 0);
        switch (mouth) {
            case DOWN -> pose.rotateDegrees(Axis.XP, 180f);
            case NORTH -> pose.rotateDegrees(Axis.XN, 90f);
            case SOUTH -> pose.rotateDegrees(Axis.XP, 90f);
            case EAST -> pose.rotateDegrees(Axis.ZN, 90f);
            default -> pose.rotateDegrees(Axis.ZP, 90f);
        }
        pose.translate(0, -0.5, 0);
    }
}

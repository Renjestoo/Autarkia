package dev.autarkia.client.render;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.BigMachineBlock;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * GeckoLib renderer for Autarkia machines: model {@code geckolib/models/block/<id>.geo.json}, texture
 * {@code textures/block/<id>_geo.png}. Only the center block entity draws, with bounds that cover the whole footprint.
 * Models are built around the footprint center, so an off-center footprint (the 2x2 assembler) is shifted by the
 * rotated footprint-center offset after the facing rotation.
 */
public class GeoMachineRenderer<T extends AutarkiaBlockEntity & GeoAnimatable> extends GeoBlockRenderer<T, BlockEntityRenderState> {
    private static final com.geckolib.constant.dataticket.DataTicket<Vec3> FOOTPRINT_CENTER =
        com.geckolib.constant.dataticket.DataTicket.create("autarkia_footprint_center", Vec3.class);
    private final double margin;

    public GeoMachineRenderer(BlockEntityRendererProvider.Context ctx, String id) {
        this(ctx, id, 0.5);
    }

    /** Create a renderer whose culling bounds extend margin blocks beyond the footprint (rotor blades, overhangs). */
    public GeoMachineRenderer(BlockEntityRendererProvider.Context ctx, String id, double margin) {
        super(ctx, (DefaultedBlockGeoModel<T>) new DefaultedBlockGeoModel<T>(Autarkia.id(id)).withAltTexture(Autarkia.id(id + "_geo")));
        this.margin = margin;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void adjustRenderPose(com.geckolib.renderer.base.RenderPassInfo info) {
        super.adjustRenderPose(info);
        Vec3 c = ((com.geckolib.renderer.base.GeoRenderState) info.renderState()).getOrDefaultGeckolibData(FOOTPRINT_CENTER, Vec3.ZERO);
        if (c.x != 0 || c.z != 0) {
            info.poseStack().translate(c.x, 0, c.z);
        }
    }

    @Override
    public void addRenderData(T be, @org.jspecify.annotations.Nullable Void related, BlockEntityRenderState state, float partialTick) {
        super.addRenderData(be, related, state, partialTick);
        if (be.getBlockState().getBlock() instanceof BigMachineBlock big) {
            double x0 = 0;
            double x1 = 0;
            double z0 = 0;
            double z1 = 0;
            for (Vec3i o : big.offsets()) {
                x0 = Math.min(x0, o.getX());
                x1 = Math.max(x1, o.getX());
                z0 = Math.min(z0, o.getZ());
                z1 = Math.max(z1, o.getZ());
            }
            ((com.geckolib.renderer.base.GeoRenderState) (Object) state).addGeckolibData(FOOTPRINT_CENTER, new Vec3((x0 + x1) / 2, 0, (z0 + z1) / 2));
        }
    }

    @Override
    public boolean shouldRender(T be, Vec3 camera) {
        return be.isCenter() && Vec3.atCenterOf(be.getBlockPos()).closerThan(camera, getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(T be) {
        BlockPos p = be.getBlockPos();
        BlockState st = be.getBlockState();
        AABB box = new AABB(p).inflate(margin);
        if (st.getBlock() instanceof BigMachineBlock big) {
            int r = 0;
            int y0 = 0;
            int y1 = 0;
            for (Vec3i o : big.offsets()) {
                r = Math.max(r, Math.max(Math.abs(o.getX()), Math.abs(o.getZ())));
                y0 = Math.min(y0, o.getY());
                y1 = Math.max(y1, o.getY());
            }
            box = new AABB(p.getX() - r, p.getY() + y0, p.getZ() - r, p.getX() + r + 1, p.getY() + y1 + 1, p.getZ() + r + 1).inflate(margin);
        }
        return box;
    }
}

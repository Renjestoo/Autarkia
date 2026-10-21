package dev.autarkia.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.autarkia.Autarkia;
import dev.autarkia.logistics.CableCarStationBlockEntity;
import dev.autarkia.logistics.CableGeometry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renderer of the {@code cable_car_station} (04 sections 1.5 and 4.8): the GeckoLib station with its bull wheel, and on
 * the owner station both cables of the line (wheel to wheel, 3% parabolic sag). The owner renders off screen with the
 * line bounds (both stations, both cables, sag and the gondola hang) and draws while the camera is within 64 blocks of
 * those bounds; a non-centre position keeps its own block box and draws nothing. Cables scroll while the line runs.
 */
public class CableCarStationRenderer extends GeoMachineRenderer<CableCarStationBlockEntity> {
    private static final Identifier CABLE = Autarkia.id("textures/entity/cable/gondola_cable.png");
    private static final float THICKNESS = 0.08f;
    private static final DataTicket<Cables> CABLES = DataTicket.create("autarkia_cable_car_cables", Cables.class);

    /** Both cables of a line relative to the owner block, with their light and texture scroll. */
    public record Cables(Vec3[] right, Vec3[] left, int light, double scroll) {
    }

    private static final Cables NONE = new Cables(new Vec3[0], new Vec3[0], 0, 0);

    public CableCarStationRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, "cable_car_station", 1.0);
    }

    private static @Nullable CableGeometry ownerGeometry(CableCarStationBlockEntity be) {
        return be.isCenter() && be.isOwner() ? be.geometry() : null;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(CableCarStationBlockEntity be) {
        if (!be.isCenter()) {
            return new AABB(be.getBlockPos());
        }
        CableGeometry geo = ownerGeometry(be);
        return geo == null ? super.getRenderBoundingBox(be) : geo.bounds().minmax(super.getRenderBoundingBox(be));
    }

    @Override
    public boolean shouldRender(CableCarStationBlockEntity be, Vec3 camera) {
        CableGeometry geo = ownerGeometry(be);
        if (geo == null) {
            return super.shouldRender(be, camera);
        }
        return getRenderBoundingBox(be).distanceToSqr(camera) < 64 * 64;
    }

    @Override
    public void addRenderData(CableCarStationBlockEntity be, @Nullable Void related, BlockEntityRenderState state, float partialTick) {
        super.addRenderData(be, related, state, partialTick);
        CableGeometry geo = ownerGeometry(be);
        if (geo == null || be.getLevel() == null) {
            ((GeoRenderState) (Object) state).addGeckolibData(CABLES, NONE);
            return;
        }
        BlockPos origin = be.getBlockPos();
        Vec3 o = Vec3.atLowerCornerOf(origin);
        Vec3[] r = geo.points(1);
        Vec3[] l = geo.points(-1);
        for (int i = 0; i < r.length; i++) {
            r[i] = r[i].subtract(o);
            l[i] = l[i].subtract(o);
        }
        BlockPos mid = BlockPos.containing(geo.cable(0.5, 0));
        int light = LightCoordsUtil.pack(be.getLevel().getBrightness(LightLayer.BLOCK, mid), be.getLevel().getBrightness(LightLayer.SKY, mid));
        double scroll = be.running() ? (be.getLevel().getGameTime() + partialTick) * 0.6 : 0;
        ((GeoRenderState) (Object) state).addGeckolibData(CABLES, new Cables(r, l, light, scroll));
    }

    @Override
    public void submit(BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        GeoRenderState geo = (GeoRenderState) (Object) state;
        Cables c = geo.hasGeckolibData(CABLES) ? geo.getGeckolibData(CABLES) : NONE;
        if (c.right().length == 0) {
            return;
        }
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(CABLE), (p, vc) -> {
            ClientSpans.tube(vc, p, c.right(), THICKNESS, 0xFFFFFFFF, c.light(), -c.scroll());
            ClientSpans.tube(vc, p, c.left(), THICKNESS, 0xFFFFFFFF, c.light(), c.scroll());
        });
    }
}

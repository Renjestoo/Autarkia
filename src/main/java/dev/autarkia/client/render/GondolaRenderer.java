package dev.autarkia.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import dev.autarkia.Autarkia;
import dev.autarkia.logistics.GondolaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jspecify.annotations.Nullable;

/**
 * Gondola renderer (04 section 4.8): model and animations {@code gondola}, texture {@code textures/entity/gondola.png}.
 * The entity yaw points the model's z axis along the line, so the model x axis is the right-hand side of the line. The
 * swing turns the {@code hanger} bone (pivot at the cable, 2.5 blocks above the floor) by the client-eased synced angles:
 * about x by the swing along the line, about z by the swing across it, on top of the idle and door animations.
 */
public class GondolaRenderer extends GeoEntityRenderer<GondolaEntity, GondolaRenderer.State> {
    private static final DataTicket<float[]> SWING = DataTicket.create("autarkia_gondola_swing", float[].class);

    /**
     * The gondola render state. GeckoLib mixes its data storage into every vanilla entity render state at runtime, so
     * this class only declares the interface for the compiler.
     */
    public static final class State extends EntityRenderState implements GeoRenderState {
    }

    public GondolaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(Autarkia.id("gondola")));
        this.shadowRadius = 0.9f;
    }

    @Override
    public State createRenderState(GondolaEntity g, @Nullable Void related) {
        return new State();
    }

    @Override
    public void addRenderData(GondolaEntity g, @Nullable Void related, State state, float partialTick) {
        super.addRenderData(g, related, state, partialTick);
        state.addGeckolibData(SWING, new float[] {g.clientAlong(partialTick), g.clientAcross(partialTick)});
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<State> info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        float[] swing = info.renderState().getOrDefaultGeckolibData(SWING, new float[2]);
        snapshots.ifPresent("hanger", b -> {
            b.setRotX(b.getRotX() + (float) Math.toRadians(swing[0]));
            b.setRotZ(b.getRotZ() + (float) Math.toRadians(swing[1]));
        });
    }
}

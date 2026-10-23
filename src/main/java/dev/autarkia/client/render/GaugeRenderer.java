package dev.autarkia.client.render;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import dev.autarkia.registry.AutarkiaBlockEntity;
import java.util.function.ToDoubleFunction;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jspecify.annotations.Nullable;

/**
 * A machine renderer whose gauge needle bone shows a live value of the block entity: the value (0 to 1, clamped) turns the
 * needle about its pivot from -120 degrees (empty) to +120 degrees (full) on the dial plane.
 */
public class GaugeRenderer<T extends AutarkiaBlockEntity & GeoAnimatable> extends GeoMachineRenderer<T> {
    private static final DataTicket<Float> READING = DataTicket.create("autarkia_gauge_reading", Float.class);
    private static final float SWEEP = (float) Math.toRadians(120);
    private final String needle;
    private final ToDoubleFunction<T> reading;

    public GaugeRenderer(BlockEntityRendererProvider.Context ctx, String id, double margin, String needle, ToDoubleFunction<T> reading) {
        super(ctx, id, margin);
        this.needle = needle;
        this.reading = reading;
    }

    @Override
    public void addRenderData(T be, @Nullable Void related, BlockEntityRenderState state, float partialTick) {
        super.addRenderData(be, related, state, partialTick);
        float v = (float) Math.max(0, Math.min(1, reading.applyAsDouble(be)));
        ((GeoRenderState) (Object) state).addGeckolibData(READING, v);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void adjustModelBonesForRender(RenderPassInfo info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        float v = ((GeoRenderState) info.renderState()).getOrDefaultGeckolibData(READING, 0f);
        snapshots.ifPresent(needle, b -> b.setRotZ(SWEEP - 2 * SWEEP * v));
    }
}

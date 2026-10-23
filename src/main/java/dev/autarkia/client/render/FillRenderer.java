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
 * A machine renderer whose level bone shows a live fill of the block entity: the fill (0 to 1, clamped) scales the bone
 * in Y about its pivot, so a bar modelled at full height grows from its base.
 */
public class FillRenderer<T extends AutarkiaBlockEntity & GeoAnimatable> extends GeoMachineRenderer<T> {
    private static final DataTicket<Float> FILL = DataTicket.create("autarkia_level_fill", Float.class);
    private static final float EMPTY = 0.001f;
    private final String bone;
    private final ToDoubleFunction<T> fill;

    public FillRenderer(BlockEntityRendererProvider.Context ctx, String id, String bone, ToDoubleFunction<T> fill) {
        super(ctx, id);
        this.bone = bone;
        this.fill = fill;
    }

    @Override
    public void addRenderData(T be, @Nullable Void related, BlockEntityRenderState state, float partialTick) {
        super.addRenderData(be, related, state, partialTick);
        float v = (float) Math.max(0, Math.min(1, fill.applyAsDouble(be)));
        ((GeoRenderState) (Object) state).addGeckolibData(FILL, v);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void adjustModelBonesForRender(RenderPassInfo info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        float v = ((GeoRenderState) info.renderState()).getOrDefaultGeckolibData(FILL, 0f);
        snapshots.ifPresent(bone, b -> b.setScaleY(Math.max(EMPTY, v)));
    }
}

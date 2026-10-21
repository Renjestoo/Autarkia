package dev.autarkia.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import dev.autarkia.worker.WorkerAssemblerBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jspecify.annotations.Nullable;

/** The worker assembler renderer: the {@code chassis} bone grows in four stages with the build progress (06 section 5.2). */
public class AssemblerRenderer extends GeoMachineRenderer<WorkerAssemblerBlockEntity> {
    private static final DataTicket<Integer> STAGE = DataTicket.create("autarkia_assembly_stage", Integer.class);

    public AssemblerRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, "worker_assembler");
    }

    @Override
    public void addRenderData(WorkerAssemblerBlockEntity be, @Nullable Void related, BlockEntityRenderState state, float partialTick) {
        super.addRenderData(be, related, state, partialTick);
        ((GeoRenderState) (Object) state).addGeckolibData(STAGE, be.stage());
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void adjustModelBonesForRender(RenderPassInfo info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        int stage = ((GeoRenderState) info.renderState()).getOrDefaultGeckolibData(STAGE, 0);
        float s = stage / 4f;
        snapshots.ifPresent("chassis", b -> {
            b.skipRender(stage == 0);
            b.setScale(s, s, s);
        });
    }
}

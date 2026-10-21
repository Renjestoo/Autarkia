package dev.autarkia.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import dev.autarkia.Autarkia;
import dev.autarkia.worker.WorkerEntity;
import dev.autarkia.worker.WorkerType;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

/**
 * Worker renderer for one rig (06 section 2.1): texture {@code textures/entity/<type>.png} from the worker type, the
 * glow mask layer, the cargo item at bone {@code cargo_slot} ({@code pallet_slot} for the Loader), and every optional
 * bone the type does not show hidden after the animations run; upgrade bones show while their upgrade is installed.
 */
public class WorkerRenderer extends GeoEntityRenderer<WorkerEntity, WorkerRenderer.State> {
    private static final DataTicket<String> WORKER_TYPE = DataTicket.create("autarkia_worker_type", String.class);
    private static final DataTicket<Integer> UPGRADE_MASK = DataTicket.create("autarkia_upgrade_mask", Integer.class);

    /**
     * The worker render state. GeckoLib mixes its data storage into every vanilla entity render state at runtime, so
     * this class only declares the interface for the compiler and keeps that storage.
     */
    public static final class State extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState implements GeoRenderState {
    }

    /** The rig model: geometry and animations of the rig, texture of the worker type. */
    static final class Model extends DefaultedEntityGeoModel<WorkerEntity> {
        Model(String rig) {
            super(Autarkia.id(rig));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return Autarkia.id("textures/entity/" + state.getOrDefaultGeckolibData(WORKER_TYPE, "porter") + ".png");
        }
    }

    public WorkerRenderer(EntityRendererProvider.Context ctx, String rig) {
        this(ctx, rig, 1.0f);
    }

    /** Create the renderer of a rig drawn at a type scale (Loader 1.2); the Loader carries its crate at {@code pallet_slot}. */
    public WorkerRenderer(EntityRendererProvider.Context ctx, String rig, float scale) {
        super(ctx, new Model(rig));
        withScale(scale);
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
        withRenderLayer(new BlockAndItemGeoLayer<WorkerEntity, Void, State>(ctx, this) {
            @Override
            protected List<RenderData> getRelevantBones(WorkerEntity w, @Nullable Void o, State s, float pt) {
                return List.of();
            }

            @Override
            public void addRenderData(WorkerEntity w, @Nullable Void o, State s, float pt) {
                if (w.cargoLook().isEmpty()) {
                    s.addGeckolibData(CONTENTS, List.of());
                    return;
                }
                ItemStackRenderState item = new ItemStackRenderState();
                itemModelResolver.updateForTopItem(item, w.cargoLook(), ItemDisplayContext.GROUND, w.level(), w, w.getId());
                boolean pallet = w.workerType == WorkerType.LOADER;
                s.addGeckolibData(CONTENTS, List.of(RenderData.item(pallet ? "pallet_slot" : "cargo_slot", pallet ? ItemDisplayContext.FIXED
                    : ItemDisplayContext.GROUND, item)));
            }
        });
    }

    @Override
    public State createRenderState(WorkerEntity w, @Nullable Void related) {
        return new State();
    }

    @Override
    public void addRenderData(WorkerEntity w, @Nullable Void related, State state, float partialTick) {
        super.addRenderData(w, related, state, partialTick);
        state.addGeckolibData(WORKER_TYPE, w.workerType.id().getPath());
        state.addGeckolibData(UPGRADE_MASK, w.upgradeMask());
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<State> info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        List<String> shown = WorkerType.byPath(info.renderState().getOrDefaultGeckolibData(WORKER_TYPE, "porter")).shownBones();
        int mask = info.renderState().getOrDefaultGeckolibData(UPGRADE_MASK, 0);
        for (String bone : WorkerType.OPTIONAL_BONES) {
            int up = WorkerType.UPGRADE_BONES.indexOf(bone);
            if (!shown.contains(bone) && (up < 0 || (mask >> up & 1) == 0)) {
                snapshots.ifPresent(bone, s -> s.skipRender(true));
            }
        }
    }
}

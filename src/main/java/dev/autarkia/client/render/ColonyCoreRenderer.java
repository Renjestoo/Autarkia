package dev.autarkia.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import dev.autarkia.colony.ColonyCoreBlockEntity;
import java.util.List;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jspecify.annotations.Nullable;

/**
 * The Colony Core renderer: each {@code tier_N} part shows from colony tier N on, and the orb takes the colour of the highest
 * {@code tier_N_orb} reached (amber {@code core_orb} at tiers 1 and 2, cyan from 3, violet at 5, white-gold at 6).
 */
public class ColonyCoreRenderer extends GeoMachineRenderer<ColonyCoreBlockEntity> {
    private static final DataTicket<Integer> TIER = DataTicket.create("autarkia_colony_tier", Integer.class);
    private static final List<String> PARTS = List.of("tier_2", "tier_3", "tier_3_ring", "tier_4", "tier_5", "tier_6");
    private static final List<String> ORBS = List.of("tier_3_orb", "tier_5_orb", "tier_6_orb");

    public ColonyCoreRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, "colony_core", 1.0);
    }

    @Override
    public void addRenderData(ColonyCoreBlockEntity be, @Nullable Void related, BlockEntityRenderState state, float partialTick) {
        super.addRenderData(be, related, state, partialTick);
        ((GeoRenderState) (Object) state).addGeckolibData(TIER, be.tier());
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void adjustModelBonesForRender(RenderPassInfo info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        int tier = ((GeoRenderState) info.renderState()).getOrDefaultGeckolibData(TIER, 1);
        for (String part : PARTS) {
            boolean hidden = part.charAt(5) - '0' > tier;
            snapshots.ifPresent(part, b -> b.skipRender(hidden).skipChildrenRender(hidden));
        }
        int orb = tier >= 6 ? 6 : tier == 5 ? 5 : tier >= 3 ? 3 : 1;
        snapshots.ifPresent("core_orb", b -> b.skipRender(orb > 1));
        for (String name : ORBS) {
            boolean hidden = name.charAt(5) - '0' != orb;
            snapshots.ifPresent(name, b -> b.skipRender(hidden));
        }
    }
}

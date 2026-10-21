package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Phase node state: the connector Jade lines of 02 section 9.2 plus {@code Links: n} and {@code Line of sight: ok / blocked}. */
public class PhaseNodeBlockEntity extends NodeBlockEntity {
    public PhaseNodeBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.PHASE_NODE.get(), pos, state, CableTier.PHASE, PhaseNodeBlock.MAX_LINKS);
    }

    /** Return the number of phase links on this node. */
    public int links() {
        return spans(false);
    }

    /** Return the number of phase links on this node whose line of sight is blocked. */
    public int blockedLinks() {
        return spans(true);
    }

    private int spans(boolean blockedOnly) {
        if (!(level instanceof ServerLevel sl)) {
            return 0;
        }
        GridSavedData data = Grid.of(sl).data();
        IntArrayList ids = data.at(worldPosition.asLong());
        int n = 0;
        for (int i = 0; i < ids.size(); i++) {
            Span s = data.spans.get(ids.getInt(i));
            if (s != null && s.tier == CableTier.PHASE && (!blockedOnly || s.blocked)) {
                n++;
            }
        }
        return n;
    }

    @Override
    public void jadeLines(List<Component> out) {
        super.jadeLines(out);
        out.add(Component.translatable("jade.autarkia.phase.links", links()));
        out.add(Component.translatable("jade.autarkia.phase.sight", Component.translatable(blockedLinks() > 0 ? "jade.autarkia.phase.blocked"
            : "jade.autarkia.phase.ok")));
    }
}

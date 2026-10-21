package dev.autarkia.colony;

import dev.autarkia.energy.Grid;
import dev.autarkia.energy.Span;
import dev.autarkia.registry.AutarkiaTags;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;

/** Build-state checks shared by the job board and the workers: placed cells, strung spans, support and the remaining bill. */
public final class Builds {
    private Builds() {
    }

    /** Return whether the cell already holds its planned block (same block; for big blocks a center of the same block). */
    public static boolean placed(ServerLevel level, Project.Cell cell) {
        BlockState now = level.getBlockState(BlockPos.of(cell.pos()));
        if (cell.state().getBlock() instanceof BaseMultiblock) {
            return now.is(cell.state().getBlock()) && now.getValue(BaseMultiblock.CENTER);
        }
        return now.is(cell.state().getBlock());
    }

    /** Return whether a span between the two node positions exists. */
    public static boolean strung(ServerLevel level, long a, long b) {
        for (int id : Grid.of(level).data().at(a)) {
            Span s = Grid.of(level).data().spans.get(id);
            if (s != null && s.joins(a, b)) {
                return true;
            }
        }
        return false;
    }

    /** Return whether a cell can be placed now: the block below or a side neighbor is solid. */
    public static boolean supported(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos.below()).isAir()) {
            return true;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockState n = level.getBlockState(pos.relative(d));
            if (!n.isAir() && !n.canBeReplaced()) {
                return true;
            }
        }
        return false;
    }

    /** Return whether a world block may be replaced by a planned cell (air, replaceable or tag worker_replaceable). */
    public static boolean replaceable(BlockState st) {
        return st.isAir() || st.canBeReplaced() || st.is(AutarkiaTags.WORKER_REPLACEABLE);
    }

    /** Return the item that builds a planned block: its block item, or the {@code outpost_seed} for an outpost core. */
    public static Item itemFor(BlockState state) {
        return state.is(dev.autarkia.registry.AutarkiaBlocks.OUTPOST_CORE.get()) ? dev.autarkia.registry.AutarkiaItems.OUTPOST_SEED.get()
            : state.getBlock().asItem();
    }

    /** Return the items still needed: cells not placed and cable items of spans not strung. */
    public static Map<Item, Integer> remaining(ServerLevel level, Project p) {
        Map<Item, Integer> need = new LinkedHashMap<>();
        for (Project.Cell cell : p.cells) {
            if (!placed(level, cell)) {
                need.merge(itemFor(cell.state()), 1, Integer::sum);
            }
        }
        for (Project.SpanRec s : p.spans) {
            if (!strung(level, s.a(), s.b())) {
                need.merge(Grid.cableItem(s.tier()), s.tier().itemCost(Math.sqrt(BlockPos.of(s.a()).distSqr(BlockPos.of(s.b())))), Integer::sum);
            }
        }
        return need;
    }
}

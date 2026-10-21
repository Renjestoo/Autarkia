package dev.autarkia.equipment;

import dev.autarkia.config.AutarkiaConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Vein mining of the {@code vantite_excavator} (07 section 1.5): breaking an ore in vein mode also breaks the connected
 * blocks (26 neighbours) that share one of its {@code c:ores/<material>} tags, so stone and deepslate variants form one
 * vein, up to {@code tools.maxVeinBlocks} (64) blocks including the first (breadth first, then nearest first). Every
 * block must be correct for the tool. The vein stops when the tool could not also pay the 400 FE of the first block,
 * which {@link VantiteExcavatorItem#mineBlock} charges after this handler. A guard stops the extra breaks from starting
 * veins of their own.
 */
public final class VeinMining {
    private static boolean busy;

    private VeinMining() {
    }

    /** Register the break listener (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(VeinMining::onBreak);
    }

    /** Return the {@code c:ores/<material>} tags of a block state (empty for blocks that are not ores). */
    public static List<TagKey<Block>> oreTags(BlockState state) {
        return state.typeHolder().tags().filter(t -> t.location().getNamespace().equals("c") && t.location().getPath().startsWith("ores/")).toList();
    }

    /** Return the vein blocks connected to start (start excluded) that share one of the ore tags, nearest first, at most limit. */
    public static List<BlockPos> vein(ServerLevel level, BlockPos start, List<TagKey<Block>> tags, ItemStack tool, int limit) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && found.size() < limit) {
            BlockPos cur = queue.poll();
            for (BlockPos n : BlockPos.betweenClosed(cur.offset(-1, -1, -1), cur.offset(1, 1, 1))) {
                if (found.size() >= limit) {
                    break;
                }
                BlockPos p = n.immutable();
                if (seen.add(p) && sameVein(level.getBlockState(p), tags, tool)) {
                    found.add(p);
                    queue.add(p);
                }
            }
        }
        found.sort(Comparator.comparingDouble((BlockPos p) -> p.distSqr(start)).thenComparingLong(BlockPos::asLong));
        return found;
    }

    private static boolean sameVein(BlockState state, List<TagKey<Block>> tags, ItemStack tool) {
        if (state.isAir() || !tool.isCorrectToolForDrops(state)) {
            return false;
        }
        for (TagKey<Block> t : tags) {
            if (state.is(t)) {
                return true;
            }
        }
        return false;
    }

    private static void onBreak(BreakBlockEvent e) {
        if (busy || e.isCanceled() || !(e.getPlayer() instanceof ServerPlayer sp) || !(e.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ItemStack held = sp.getMainHandItem();
        if (!(held.getItem() instanceof VantiteExcavatorItem) || !VantiteExcavatorItem.vein(held)
            || PoweredItem.energy(held) < VantiteExcavatorItem.VEIN_COST) {
            return;
        }
        List<TagKey<Block>> tags = oreTags(e.getState());
        if (tags.isEmpty()) {
            return;
        }
        int limit = Math.max(0, AutarkiaConfig.TOOLS_MAX_VEIN_BLOCKS.get() - 1);
        List<BlockPos> blocks = vein(level, e.getPos(), tags, held, limit);
        busy = true;
        try {
            for (BlockPos p : blocks) {
                if (sp.getMainHandItem() != held || PoweredItem.energy(held) < 2 * VantiteExcavatorItem.VEIN_COST) {
                    return;
                }
                sp.gameMode.destroyBlock(p);
            }
        } finally {
            busy = false;
        }
    }
}

package dev.autarkia.equipment;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.EventTrigger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Chainsaw tree felling (07 section 1.4): cutting one log (not sneaking) with an {@code electric_chainsaw} fells every
 * connected {@code #minecraft:logs} block (26 neighbours), at most {@code tools.maxFellLogs} (256), 16 per tick. Each log
 * costs 150 FE through the tool's block cost; the cut stops when the FE runs out. 64 or more logs in one cut fire the
 * {@code fell_64} equipment event.
 */
public final class TreeFelling {
    public static final int LOG_COST = 150;
    private static final int PER_TICK = 16;
    private static final List<Job> JOBS = new ArrayList<>();
    private static boolean busy;

    private record Job(ServerPlayer player, ServerLevel level, ArrayDeque<BlockPos> queue, int[] felled) {
    }

    private TreeFelling() {
    }

    /** Register the event listeners (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(TreeFelling::onBreak);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> JOBS.removeIf(TreeFelling::step));
    }

    /** Return the connected logs from start (start excluded), nearest first, at most limit. */
    public static List<BlockPos> connectedLogs(ServerLevel level, BlockPos start, int limit) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(start);
        seen.add(start);
        while (!q.isEmpty() && out.size() < limit) {
            BlockPos cur = q.poll();
            for (BlockPos n : BlockPos.betweenClosed(cur.offset(-1, -1, -1), cur.offset(1, 1, 1))) {
                if (out.size() >= limit) {
                    break;
                }
                BlockPos p = n.immutable();
                if (seen.add(p) && level.getBlockState(p).is(BlockTags.LOGS)) {
                    out.add(p);
                    q.add(p);
                }
            }
        }
        out.sort(Comparator.comparingInt((BlockPos p) -> p.getY()).thenComparingDouble(p -> p.distSqr(start)).thenComparingLong(BlockPos::asLong));
        return out;
    }

    private static void onBreak(BreakBlockEvent e) {
        if (busy || e.isCanceled() || !(e.getPlayer() instanceof ServerPlayer sp) || !(e.getLevel() instanceof ServerLevel level)
            || sp.isShiftKeyDown() || !e.getState().is(BlockTags.LOGS)) {
            return;
        }
        ItemStack held = sp.getMainHandItem();
        if (!held.is(AutarkiaItems.ELECTRIC_CHAINSAW.get()) || PoweredItem.energy(held) <= 0) {
            return;
        }
        List<BlockPos> logs = connectedLogs(level, e.getPos(), AutarkiaConfig.TOOLS_MAX_FELL_LOGS.get() - 1);
        if (!logs.isEmpty()) {
            JOBS.add(new Job(sp, level, new ArrayDeque<>(logs), new int[] {1}));
        }
    }

    private static boolean step(Job job) {
        ServerPlayer sp = job.player;
        if (sp.isRemoved() || sp.level() != job.level) {
            return true;
        }
        ItemStack held = sp.getMainHandItem();
        busy = true;
        try {
            for (int i = 0; i < PER_TICK && !job.queue.isEmpty(); i++) {
                if (!held.is(AutarkiaItems.ELECTRIC_CHAINSAW.get()) || PoweredItem.energy(held) < LOG_COST) {
                    job.queue.clear();
                    break;
                }
                BlockPos p = job.queue.poll();
                if (job.level.getBlockState(p).is(BlockTags.LOGS) && sp.gameMode.destroyBlock(p)) {
                    job.felled[0]++;
                }
            }
        } finally {
            busy = false;
        }
        if (!job.queue.isEmpty()) {
            return false;
        }
        if (job.felled[0] >= 64) {
            EventTrigger.EQUIPMENT.get().fire(sp, "fell_64");
        }
        return true;
    }
}

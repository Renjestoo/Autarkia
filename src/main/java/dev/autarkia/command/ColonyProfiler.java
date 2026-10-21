package dev.autarkia.command;

import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * {@code /autarkia worker perf} (06 section 8): {@code System.nanoTime} per worker type and colony subsystem over 200
 * ticks, for display only; it never changes a budget. Also drives the timed {@code /autarkia domain show} border.
 */
public final class ColonyProfiler {
    private static final Map<String, long[]> TIMES = new LinkedHashMap<>();
    private static @Nullable CommandSourceStack source;
    private static long until;
    private record Border(ServerLevel level, Colony colony, long until) {
    }
    private static final List<Border> BORDERS = new ArrayList<>();

    private ColonyProfiler() {
    }

    /** Register the tick listener. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            long now = e.getServer().getTickCount();
            BORDERS.removeIf(b -> {
                if (now % 10 == 0) {
                    ColonyCommand.border(b.level, b.colony);
                }
                return b.level.getGameTime() >= b.until;
            });
            if (source != null && now >= until) {
                report();
            }
        });
    }

    /** Start a 200-tick measurement for the source. */
    public static void start(CommandSourceStack src) {
        TIMES.clear();
        source = src;
        until = src.getServer().getTickCount() + 200;
    }

    /** Return whether a measurement runs. */
    public static boolean active() {
        return source != null;
    }

    /** Add nanoseconds to a subsystem. */
    public static void add(String subsystem, long nanos) {
        long[] t = TIMES.computeIfAbsent(subsystem, k -> new long[2]);
        t[0] += nanos;
        t[1]++;
    }

    /** Show the domain border of a colony for the given ticks. */
    public static void showDomain(ServerLevel level, Colony c, int ticks) {
        BORDERS.add(new Border(level, c, level.getGameTime() + ticks));
    }

    private static void report() {
        CommandSourceStack src = source;
        source = null;
        src.sendSuccess(() -> Component.literal("[profile] 200 ticks, " + Colonies.registry().all().size() + " colonies"), false);
        for (Map.Entry<String, long[]> e : TIMES.entrySet()) {
            boolean worker = e.getKey().startsWith("worker.");
            double meanMs = e.getValue()[0] / 1e6 / (worker ? Math.max(1, e.getValue()[1]) : 200.0);
            String line = String.format(java.util.Locale.ROOT, "[profile] %s: %.4f ms per %s (%d calls)", e.getKey(), meanMs, worker ? "worker tick" : "tick",
                e.getValue()[1]);
            src.sendSuccess(() -> Component.literal(line), false);
            dev.autarkia.Autarkia.LOG.info(line);
        }
    }
}

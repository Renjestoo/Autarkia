package dev.autarkia.command;

import dev.autarkia.Autarkia;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * {@code /autarkia profile} (05b section 11.1, 12 section 2 M10): the server time per tick of each Autarkia system,
 * measured with {@code System.nanoTime} around its tick entry point and kept for the last 200 server ticks (05b s11.1).
 * A tick closes after every other {@code ServerTickEvent.Post} listener. Display only; it never changes a budget.
 * {@link #add} is for the server thread only.
 */
public final class SystemProfiler {
    /** The measured systems, in report order. */
    public enum Part {
        GRID("grid"),
        WORKERS("workers"),
        COLONY("colony planner and jobs"),
        NANITES("nanites"),
        RESTORATION("restoration"),
        SEED("seed unpack"),
        LOGISTICS("logistics");

        private final String label;

        Part(String label) {
            this.label = label;
        }
    }

    private static final int WINDOW = 200;
    private static final Part[] PARTS = Part.values();
    private static final long[] CURRENT = new long[PARTS.length];
    private static final long[][] RING = new long[PARTS.length][WINDOW];
    private static final ClassValue<Boolean> LOGISTICS = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            String pkg = type.getPackageName();
            return pkg.equals(dev.autarkia.logistics.MagRailLine.class.getPackageName()) || pkg.equals(dev.autarkia.storage.MatterBank.class.getPackageName());
        }
    };
    private static int head;
    private static int filled;

    private SystemProfiler() {
    }

    /** Register the tick close and the server stop reset. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ServerTickEvent.Post e) -> roll());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> reset());
    }

    /** Add the time since {@code t0}, a {@code System.nanoTime} value, to the part's cost in the current tick. */
    public static void add(Part part, long t0) {
        CURRENT[part.ordinal()] += System.nanoTime() - t0;
    }

    /** Return whether a block entity belongs to the logistics and storage systems (04). */
    public static boolean logistics(Object blockEntity) {
        return LOGISTICS.get(blockEntity.getClass());
    }

    private static void roll() {
        for (int i = 0; i < PARTS.length; i++) {
            RING[i][head] = CURRENT[i];
            CURRENT[i] = 0;
        }
        head = (head + 1) % WINDOW;
        filled = Math.min(WINDOW, filled + 1);
    }

    private static void reset() {
        Arrays.fill(CURRENT, 0);
        for (long[] r : RING) {
            Arrays.fill(r, 0);
        }
        head = 0;
        filled = 0;
    }

    /** Send the average and maximum milliseconds per tick of every part and of their sum over the window; returns the ticks covered. */
    static int report(CommandSourceStack src) {
        int n = filled;
        if (n == 0) {
            line(src, "no server tick measured yet");
            return 0;
        }
        line(src, "Autarkia server time per tick over the last " + n + " ticks");
        long[] total = new long[n];
        for (int i = 0; i < PARTS.length; i++) {
            long sum = 0;
            long max = 0;
            for (int j = 0; j < n; j++) {
                long t = RING[i][j];
                sum += t;
                max = Math.max(max, t);
                total[j] += t;
            }
            line(src, stats(PARTS[i].label, sum, max, n));
        }
        line(src, stats("total", Arrays.stream(total).sum(), Arrays.stream(total).max().orElse(0), n));
        return n;
    }

    private static String stats(String label, long sum, long max, int n) {
        return String.format(Locale.ROOT, "%s: avg %.3f ms, max %.3f ms", label, sum / 1e6 / n, max / 1e6);
    }

    private static void line(CommandSourceStack src, String text) {
        src.sendSuccess(() -> Component.literal("[profile] " + text), false);
        Autarkia.LOG.info("[profile] {}", text);
    }
}

package dev.autarkia.colony;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Runtime state of one colony that is never saved: the last sensor snapshot, forced test signals, the hot span list
 * (S08), the running site search and the job board. Rebuilt after a server start (jobs from project cursors).
 */
public final class ColonyRuntime {
    private static final Map<UUID, ColonyRuntime> ALL = new HashMap<>();

    /** A signal override from {@code /autarkia sensor force} until a game tick. */
    public record Forced(double value, long until) {
    }

    public final Map<String, Double> snapshot = new LinkedHashMap<>();
    public final Map<String, Forced> forced = new LinkedHashMap<>();
    public final List<Integer> hotSpans = new ArrayList<>();
    public final List<net.minecraft.core.BlockPos> storages = new ArrayList<>();
    public final List<net.minecraft.core.BlockPos> chargers = new ArrayList<>();
    public final List<net.minecraft.core.BlockPos> generators = new ArrayList<>();
    public final List<net.minecraft.core.BlockPos> boards = new ArrayList<>();
    public final List<net.minecraft.core.BlockPos> transports = new ArrayList<>();
    public final List<net.minecraft.core.BlockPos> machines = new ArrayList<>();
    public final Map<UUID, Integer> patternMissing = new LinkedHashMap<>();
    public final Map<UUID, Integer> patternConflicts = new LinkedHashMap<>();
    public final List<UUID> pendingInstances = new ArrayList<>();
    public PatternRepair.@Nullable Scan repairScan;
    public long repairScanAt = -1;
    public String rackWarning = "";
    public final List<Integer> networks = new ArrayList<>();
    public final JobBoard jobs = new JobBoard();
    public @Nullable SiteSearch search;
    public final Map<String, Double> bestSite = new LinkedHashMap<>();
    public final List<String> log = new ArrayList<>();
    public final List<double[]> powerHistory = new ArrayList<>();
    public final Map<UUID, Integer> missingWorkers = new java.util.HashMap<>();
    public long lastSample = -1;
    public int @Nullable [] waterColumn;
    public long waterColumnAt = -1;

    private ColonyRuntime() {
    }

    /** Return the runtime state of a colony, creating it. */
    public static ColonyRuntime of(Colony c) {
        return ALL.computeIfAbsent(c.id, k -> new ColonyRuntime());
    }

    /** Drop the runtime state of one colony. */
    public static void forget(Colony c) {
        ALL.remove(c.id);
    }

    /** Drop every runtime state (server stop). */
    public static void clearAll() {
        ALL.clear();
    }

    /** Add a line to the board log (newest last, at most 32 lines). */
    public void log(String line) {
        log.add(line);
        while (log.size() > 32) {
            log.remove(0);
        }
    }

    /** Return a signal value: a live forced override first, then the last snapshot, else the fallback. */
    public double signal(String id, long gameTime, double fallback) {
        Forced f = forced.get(id);
        if (f != null) {
            if (gameTime <= f.until()) {
                return f.value();
            }
            forced.remove(id);
        }
        return snapshot.getOrDefault(id, fallback);
    }
}

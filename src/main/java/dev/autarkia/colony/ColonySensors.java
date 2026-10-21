package dev.autarkia.colony;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GridLevel;
import dev.autarkia.energy.GridNetwork;
import dev.autarkia.energy.Span;
import dev.autarkia.worker.WorkerEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Colony sensors (05 section 2): every sample scans the domain's loaded chunks for members (storage, chargers, muscle
 * generators, project boards), reads the power networks with a node in the domain and the colony workers, and writes
 * signals S01 to S10 (S10: highest rack load ratio, peak hold), S15 to S20 and S24 (lightning
 * strikes in the domain over the last 7 colony days) with the
 * smoothing of section 2.2 (EMA alpha from config, peak hold of 6 samples).
 * S15 sums the constant {@code cooking_fuel} burn times (a burn time given as a context reference needs a furnace
 * loot context and is not counted).
 */
public final class ColonySensors {
    /** The signals of this milestone in dump order. */
    public static final List<String> SIGNALS = List.of("S01", "S02", "S03", "S04", "S05", "S06", "S07", "S08", "S09", "S10", "S15", "S16", "S17",
        "S18", "S19", "S20", "S20.age", "S24");
    private static final String STRIKES = "S24.strikes";

    private ColonySensors() {
    }

    /** Count a lightning strike that landed at pos toward S24 of the colony whose domain holds it (week ring). */
    public static void strike(ServerLevel level, BlockPos pos) {
        if (Colonies.server() == null) {
            return;
        }
        Colony c = Colonies.at(level, pos);
        if (c != null) {
            c.peaks.computeIfAbsent(STRIKES, k -> new ArrayList<>()).add((double) c.clock);
            Colonies.changed();
        }
    }

    /** A domain member found by the scan. */
    public interface Member {
        /** Return the member kind; one block entity can be several kinds. */
        java.util.Set<Kind> colonyKinds();
    }

    /** Member kinds the scan sorts into runtime lists. */
    public enum Kind { STORAGE, CHARGER, GENERATOR, BOARD, TRANSPORT }

    /** Sample all signals of the colony now. */
    public static void sample(Colony c, ServerLevel level) {
        ColonyRuntime rt = ColonyRuntime.of(c);
        scanMembers(c, level, rt);
        Map<String, Double> raw = new LinkedHashMap<>();
        power(c, level, rt, raw);
        storage(level, rt, raw);
        logistics(c, level, rt);
        workers(c, level, rt, raw);
        raw.put("S16", 0.0);
        List<Double> strikes = c.peaks.computeIfAbsent(STRIKES, k -> new ArrayList<>());
        strikes.removeIf(t -> c.clock - t > 7 * ColonyClock.DAY);
        raw.put("S24", (double) strikes.size());
        raw.put("S20", (double) rt.jobs.openCount());
        raw.put("S20.age", (double) rt.jobs.oldestAge(level.getGameTime()));
        double alpha = AutarkiaConfig.COLONY_EMA_ALPHA.get();
        for (Map.Entry<String, Double> e : raw.entrySet()) {
            String k = e.getKey();
            double x = e.getValue();
            if (ema(k)) {
                Double old = c.ema.get(k);
                double v = old == null ? x : old + alpha * (x - old);
                c.ema.put(k, v);
                rt.snapshot.put(k, v);
            } else if (k.equals("S06") || k.equals("S07") || k.equals("S10")) {
                List<Double> ring = c.peaks.computeIfAbsent(k, kk -> new ArrayList<>());
                ring.add(x);
                while (ring.size() > 6) {
                    ring.remove(0);
                }
                rt.snapshot.put(k, ring.stream().mapToDouble(Double::doubleValue).max().orElse(0));
            } else {
                rt.snapshot.put(k, x);
            }
        }
        double s01 = rt.snapshot.getOrDefault("S01", 0.0);
        double s02 = rt.snapshot.getOrDefault("S02", 0.0);
        rt.snapshot.put("S03", Math.max(0, s02 - s01) / Math.max(s02, 1));
        rt.snapshot.put("S05", raw.getOrDefault("stored", 0.0) / Math.max(s02, 1) / 20);
        rt.snapshot.remove("stored");
        rt.powerHistory.add(new double[] {rt.snapshot.getOrDefault("S01", 0.0), rt.snapshot.getOrDefault("S02", 0.0)});
        while (rt.powerHistory.size() > 24) {
            rt.powerHistory.remove(0);
        }
        rt.lastSample = level.getGameTime();
        Planner.updateHolds(c, level, AutarkiaConfig.COLONY_SENSOR_PERIOD_TICKS.get());
        Colonies.changed();
    }

    private static boolean ema(String k) {
        return k.equals("S01") || k.equals("S02") || k.equals("S04") || k.equals("S09") || k.startsWith("S17") || k.equals("S19");
    }

    private static void scanMembers(Colony c, ServerLevel level, ColonyRuntime rt) {
        rt.storages.clear();
        rt.chargers.clear();
        rt.generators.clear();
        rt.boards.clear();
        rt.transports.clear();
        int h = ColonyTiers.domainHalf(c.tier);
        for (int cx = (c.corePos.getX() - h) >> 4; cx <= (c.corePos.getX() + h) >> 4; cx++) {
            for (int cz = (c.corePos.getZ() - h) >> 4; cz <= (c.corePos.getZ() + h) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof Member m) || !Colonies.inside(c, be.getBlockPos())
                        || be instanceof net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity mb && !mb.isCenter()) {
                        continue;
                    }
                    for (Kind k : m.colonyKinds()) {
                        (switch (k) {
                            case STORAGE -> rt.storages;
                            case CHARGER -> rt.chargers;
                            case GENERATOR -> rt.generators;
                            case BOARD -> rt.boards;
                            case TRANSPORT -> rt.transports;
                        }).add(be.getBlockPos().immutable());
                    }
                }
            }
        }
        java.util.Comparator<BlockPos> order = java.util.Comparator.comparingLong(BlockPos::asLong);
        rt.transports.sort(order);
        rt.storages.sort(order);
        rt.chargers.sort(order);
        rt.generators.sort(order);
        rt.boards.sort(order);
    }

    private static void power(Colony c, ServerLevel level, ColonyRuntime rt, Map<String, Double> raw) {
        GridLevel g = Grid.of(level);
        double supply = 0;
        double demand = 0;
        double stored = 0;
        double capacity = 0;
        double loadMax = 0;
        double heatMax = 0;
        rt.networks.clear();
        List<double[]> hot = new ArrayList<>();
        for (GridNetwork net : g.networks()) {
            boolean in = false;
            for (int i = 0; i < net.nodes.size() && !in; i++) {
                in = Colonies.inside(c, BlockPos.of(net.nodes.getLong(i)));
            }
            if (!in) {
                continue;
            }
            rt.networks.add(net.id);
            supply += net.avgGeneration;
            demand += net.workDemand();
            stored += net.stored;
            capacity += net.capacity;
            for (Span s : net.spans) {
                double load = Math.abs(s.requestedFlow) / Math.max(1, s.tier.rated);
                double heat = s.tier.hasHeat() ? s.heat : 0;
                loadMax = Math.max(loadMax, load);
                heatMax = Math.max(heatMax, heat);
                if (load >= 0.9 || heat >= 0.8) {
                    hot.add(new double[] {s.id, Math.max(load, heat)});
                }
            }
        }
        hot.sort((a, b) -> Double.compare(b[1], a[1]));
        rt.hotSpans.clear();
        for (int i = 0; i < Math.min(16, hot.size()); i++) {
            rt.hotSpans.add((int) hot.get(i)[0]);
        }
        raw.put("S01", supply);
        raw.put("S02", demand);
        raw.put("S04", capacity <= 0 ? 0 : stored / capacity);
        raw.put("S06", loadMax);
        raw.put("S07", heatMax);
        raw.put("S08", (double) rt.hotSpans.size());
        raw.put("stored", stored);
    }

    private static void storage(ServerLevel level, ColonyRuntime rt, Map<String, Double> raw) {
        int used = 0;
        int total = 0;
        double burn = 0;
        double rackMax = 0;
        for (BlockPos p : rt.storages) {
            if (!(level.getBlockEntity(p) instanceof ColonyStorage s)) {
                continue;
            }
            if (s instanceof dev.autarkia.storage.RackBlockEntity rack) {
                rackMax = Math.max(rackMax, rack.ratio());
            }
            ResourceHandler<ItemResource> h = s.colonyItems();
            for (int i = 0; i < h.size(); i++) {
                total++;
                int n = h.getAmountAsInt(i);
                if (n > 0) {
                    used++;
                    ItemStack st = h.getResource(i).toStack(n);
                    var fuel = st.get(net.minecraft.core.component.DataComponents.COOKING_FUEL);
                    if (fuel != null && fuel.burnTime() instanceof net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Constant k) {
                        burn += (double) k.value() * n;
                    }
                }
            }
        }
        raw.put("S09", total == 0 ? 0 : (double) used / total);
        raw.put("S10", rackMax);
        raw.put("S15", burn / 1600.0);
    }

    /**
     * The transport and rack values of the 04 section 3 logistics snapshot that exist in this tier, written straight into
     * the snapshot: {@code transport.chute_blocked}, {@code transport.belt_jammed_lines}, {@code transport.belt_jam_seconds},
     * {@code transport.belt_unpowered_lines}, {@code storage.racks_overloaded}, {@code storage.racks_critical},
     * {@code storage.rack_max_ratio} and {@code storage.free_crate_positions}. A rack turning critical makes the Core creak
     * once and sets the warning line of the project board.
     */
    private static void logistics(Colony c, ServerLevel level, ColonyRuntime rt) {
        int chuteBlocked = 0;
        int jammed = 0;
        int jamSeconds = 0;
        int unpowered = 0;
        for (BlockPos p : rt.transports) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof dev.autarkia.logistics.ChuteBlockEntity ch && ch.stuck()) {
                chuteBlocked++;
            }
            if (be instanceof dev.autarkia.logistics.BeltBlockEntity belt && belt.headPos().equals(p) && !belt.line().isEmpty()) {
                dev.autarkia.logistics.BeltBlockEntity end = belt.endBelt();
                if (end != null && end.jammed()) {
                    jammed++;
                    jamSeconds = Math.max(jamSeconds, end.blockedTicks() / 20);
                }
                if (!belt.powered()) {
                    unpowered++;
                }
            }
        }
        int overloaded = 0;
        int free = 0;
        double maxRatio = 0;
        dev.autarkia.storage.RackBlockEntity critical = null;
        int criticalCount = 0;
        for (BlockPos p : rt.storages) {
            if (level.getBlockEntity(p) instanceof dev.autarkia.storage.RackBlockEntity rack) {
                maxRatio = Math.max(maxRatio, rack.ratio());
                overloaded += rack.ratio() > 1.0 ? 1 : 0;
                free += rack.positionCount() - rack.crateCount();
                if (rack.stage() == 3) {
                    criticalCount++;
                    if (critical == null || rack.secondsLeft() < critical.secondsLeft()) {
                        critical = rack;
                    }
                }
            }
        }
        rt.snapshot.put("transport.chute_blocked", (double) chuteBlocked);
        rt.snapshot.put("transport.belt_jammed_lines", (double) jammed);
        rt.snapshot.put("transport.belt_jam_seconds", (double) jamSeconds);
        rt.snapshot.put("transport.belt_unpowered_lines", (double) unpowered);
        rt.snapshot.put("storage.racks_overloaded", (double) overloaded);
        rt.snapshot.put("storage.racks_critical", (double) criticalCount);
        rt.snapshot.put("storage.rack_max_ratio", maxRatio);
        rt.snapshot.put("storage.free_crate_positions", (double) free);
        String warning = critical == null ? "" : "Rack critical at " + critical.getBlockPos().toShortString() + ": " + Math.round(critical.ratio() * 100)
            + "%, collapse in " + critical.secondsLeft() + " s";
        if (!warning.isEmpty() && rt.rackWarning.isEmpty()) {
            ColonyEvents.sound(c, "rack_creak");
            rt.log(warning);
        }
        rt.rackWarning = warning;
    }

    private static void workers(Colony c, ServerLevel level, ColonyRuntime rt, Map<String, Double> raw) {
        Map<String, int[]> idle = new LinkedHashMap<>();
        int low = 0;
        double dist = 0;
        int counted = 0;
        for (Colony.WorkerEntry w : c.workers.values()) {
            Entity e = level.getEntity(w.id());
            if (!(e instanceof WorkerEntity we)) {
                continue;
            }
            int[] n = idle.computeIfAbsent(w.type().getPath(), k -> new int[2]);
            n[1]++;
            if (we.isIdle()) {
                n[0]++;
            }
            if (we.charge() < 0.2f) {
                low++;
            }
            BlockPos work = we.workPos();
            double best = Double.MAX_VALUE;
            for (BlockPos ch : rt.chargers) {
                best = Math.min(best, Math.sqrt(ch.distSqr(work)));
            }
            if (best < Double.MAX_VALUE) {
                dist += best;
                counted++;
            }
        }
        idle.forEach((type, n) -> raw.put("S17." + type, n[1] == 0 ? 0 : (double) n[0] / n[1]));
        raw.put("S18", (double) low);
        raw.put("S19", counted == 0 ? (rt.chargers.isEmpty() && !c.workers.isEmpty() ? 64.0 : 0.0) : dist / counted);
    }
}

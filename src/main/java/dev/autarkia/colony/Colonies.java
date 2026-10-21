package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Server-side colony service: the {@code autarkia:colonies} registry, the domain index (05 section 1.3, rebuilt and
 * never saved), ownership protection (05 section 1.4) and the colony tick that drives the clock, budget, sensors,
 * planner and job board of every active colony whose Core chunk is ticking.
 */
public final class Colonies {
    private static @Nullable MinecraftServer server;
    private static final Long2ObjectOpenHashMap<List<Colony>> INDEX = new Long2ObjectOpenHashMap<>();
    private static boolean dirty;

    private Colonies() {
    }

    /** Register the server listeners. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            server = e.getServer();
            rebuildIndex();
            for (Colony c : registry().all()) {
                ColonyRuntime.of(c).jobs.rebuild(c);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            server = null;
            INDEX.clear();
            ColonyRuntime.clearAll();
        });
        NeoForge.EVENT_BUS.addListener(Colonies::onBreak);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
    }

    /** Return the colony registry of the running server. */
    public static ColonyRegistry registry() {
        if (server == null) {
            throw new IllegalStateException("Colony registry used while no server is running");
        }
        return ColonyRegistry.get(server);
    }

    /** Return the running server, or null. */
    public static @Nullable MinecraftServer server() {
        return server;
    }

    /** Mark the registry dirty; it is written at the next save. */
    public static void changed() {
        dirty = true;
    }

    /** Rebuild the chunk index from every colony domain. */
    public static void rebuildIndex() {
        INDEX.clear();
        if (server == null) {
            return;
        }
        for (Colony c : registry().all()) {
            for (long chunk : chunks(c)) {
                List<Colony> list = INDEX.computeIfAbsent(chunk, k -> new ArrayList<>());
                if (!list.contains(c)) {
                    list.add(c);
                }
            }
        }
    }

    /** Return the domain squares {centerX, centerZ, half}: the Core square, then one per domain beacon (active or not). */
    public static List<int[]> squares(Colony c) {
        List<int[]> out = new ArrayList<>();
        out.add(new int[] {c.corePos.getX(), c.corePos.getZ(), ColonyTiers.domainHalf(c.tier)});
        int bh = ColonyTiers.beaconHalf(c.tier);
        for (Colony.Beacon b : c.beacons) {
            out.add(new int[] {b.pos().getX(), b.pos().getZ(), bh});
        }
        return out;
    }

    /** Return every chunk position (packed) that a domain square of the colony touches. */
    public static it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet chunks(Colony c) {
        it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet out = new it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet();
        for (int[] s : squares(c)) {
            for (int cx = (s[0] - s[2]) >> 4; cx <= (s[0] + s[2]) >> 4; cx++) {
                for (int cz = (s[1] - s[2]) >> 4; cz <= (s[1] + s[2]) >> 4; cz++) {
                    out.add(ChunkPos.pack(cx, cz));
                }
            }
        }
        return out;
    }

    private static boolean inSquare(int[] s, BlockPos pos) {
        return Math.abs(pos.getX() - s[0]) <= s[2] && Math.abs(pos.getZ() - s[1]) <= s[2];
    }

    /** Return whether pos lies in the colony's domain: the Core square or a beacon square, full build height. */
    public static boolean inside(Colony c, BlockPos pos) {
        for (int[] s : squares(c)) {
            if (inSquare(s, pos)) {
                return true;
            }
        }
        return false;
    }

    /** Return whether pos lies in the Core square or in the square of a powered beacon (the planner ignores INACTIVE areas). */
    public static boolean insideActive(Colony c, BlockPos pos) {
        if (inSquare(squares(c).get(0), pos)) {
            return true;
        }
        int bh = ColonyTiers.beaconHalf(c.tier);
        for (Colony.Beacon b : c.beacons) {
            if (b.active() && inSquare(new int[] {b.pos().getX(), b.pos().getZ(), bh}, pos)) {
                return true;
            }
        }
        return false;
    }

    /** Return whether a square overlaps a domain square of any colony other than c in the level. */
    public static boolean overlapsOther(Level level, Colony c, int x, int z, int half) {
        for (Colony o : registry().all()) {
            if (o == c || !o.dimension.equals(level.dimension())) {
                continue;
            }
            for (int[] s : squares(o)) {
                if (Math.abs(s[0] - x) <= s[2] + half && Math.abs(s[1] - z) <= s[2] + half) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Return the colony whose domain holds pos in this level, or null. */
    public static @Nullable Colony at(Level level, BlockPos pos) {
        List<Colony> list = INDEX.get(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4));
        if (list == null) {
            return null;
        }
        for (Colony c : list) {
            if (c.dimension.equals(level.dimension()) && inside(c, pos)) {
                return c;
            }
        }
        return null;
    }

    /** Return the smallest Chebyshev distance from pos to any domain edge in the level (negative inside a domain). */
    public static int distanceToDomains(Level level, BlockPos pos) {
        int best = Integer.MAX_VALUE;
        for (Colony c : registry().all()) {
            if (!c.dimension.equals(level.dimension())) {
                continue;
            }
            int h = ColonyTiers.domainHalf(c.tier);
            int d = Math.max(Math.abs(pos.getX() - c.corePos.getX()), Math.abs(pos.getZ() - c.corePos.getZ())) - h;
            best = Math.min(best, d);
        }
        return best;
    }

    /**
     * Delete a colony for a self-test reset: its jobs, runtime, record and domain go; its blocks and workers stay in the
     * world, unlinked.
     */
    public static void dissolve(Colony c) {
        Planner.cancelAllJobs(c);
        registry().remove(c.id);
        ColonyRuntime.forget(c);
        rebuildIndex();
        changed();
    }

    /** Return the colonies owned by the player. */
    public static List<Colony> ownedBy(UUID player) {
        return registry().all().stream().filter(c -> c.owner.equals(player)).toList();
    }

    /** Return the colony whose Core center is at pos, or null. */
    public static @Nullable Colony byCore(Level level, BlockPos center) {
        for (Colony c : registry().all()) {
            if (c.dimension.equals(level.dimension()) && c.corePos.equals(center)) {
                return c;
            }
        }
        return null;
    }

    /** Return the level of a colony, or null when the dimension is not loaded. */
    public static @Nullable ServerLevel level(Colony c) {
        return server == null ? null : server.getLevel(c.dimension);
    }

    private static void onBreak(net.neoforged.neoforge.event.level.block.BreakBlockEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || server == null) {
            return;
        }
        Colony c = at(level, e.getPos());
        if (c == null || c.canManage(e.getPlayer().getUUID())) {
            return;
        }
        BlockEntity be = level.getBlockEntity(e.getPos());
        boolean member = be != null && net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).getNamespace().equals(Autarkia.MOD_ID);
        if (member) {
            e.setCanceled(true);
            if (e.getPlayer() instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("message.autarkia.colony.protected", c.name));
            }
        }
    }

    private static void timed(String name, Runnable r) {
        if (!dev.autarkia.command.ColonyProfiler.active()) {
            r.run();
            return;
        }
        long t0 = System.nanoTime();
        r.run();
        dev.autarkia.command.ColonyProfiler.add(name, System.nanoTime() - t0);
    }

    private static void expireWorkers(Colony c, ServerLevel level) {
        java.util.Map<UUID, Integer> missing = ColonyRuntime.of(c).missingWorkers;
        boolean removed = c.workers.values().removeIf(w -> {
            if (level.getEntity(w.id()) != null || !level.getChunkSource().hasChunk(ChunkPos.getX(w.chunk()), ChunkPos.getZ(w.chunk()))) {
                missing.remove(w.id());
                return false;
            }
            int t = missing.merge(w.id(), 100, Integer::sum);
            return t >= 72000;
        });
        if (removed) {
            changed();
        }
    }

    private static void tick(MinecraftServer srv) {
        if (server == null) {
            return;
        }
        ColonyRegistry reg = registry();
        boolean on = AutarkiaConfig.COLONY_ENABLED.get();
        for (Colony c : new ArrayList<>(reg.all())) {
            ServerLevel level = srv.getLevel(c.dimension);
            if (level == null || c.state != Colony.State.ACTIVE) {
                continue;
            }
            if (!level.getChunkSource().isPositionTicking(ChunkPos.containing(c.corePos).pack())) {
                continue;
            }
            ColonyClock.tick(c, level);
            if (!on) {
                continue;
            }
            long now = level.getGameTime();
            int period = AutarkiaConfig.COLONY_SENSOR_PERIOD_TICKS.get();
            if (Math.floorMod(now, period) == Math.floorMod(c.id.hashCode(), period)) {
                timed("colony.sensors", () -> ColonySensors.sample(c, level));
            }
            int planPeriod = AutarkiaConfig.COLONY_PLANNER_PERIOD_TICKS.get();
            if (Math.floorMod(now, planPeriod) == Math.floorMod(c.id.hashCode(), planPeriod)) {
                timed("colony.planner", () -> Planner.pass(c, level));
            }
            timed("colony.search", () -> Planner.tick(c, level));
            timed("colony.repair", () -> PatternRepair.tick(c, level));
            timed("colony.jobs", () -> ColonyRuntime.of(c).jobs.tick(c, level));
            if (now % 100 == 0) {
                expireWorkers(c, level);
            }
        }
        if (dirty && srv.getTickCount() % 100 == 0) {
            reg.setDirty();
            dirty = false;
        }
    }
}

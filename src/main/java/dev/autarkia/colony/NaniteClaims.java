package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlocks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

/**
 * Nanite claims (05 section 8.3, W18): the claim_marker POI that lets a hive list marker pairs, the box rules and limits
 * of a new claim, pause and resume, the runtime stop and resume of {@code /autarkia nanite} (not saved; the config key
 * {@code nanite.enabled} is the saved kill switch) and the per-claim runtime figures. {@link NaniteEngine} runs the
 * claims on {@code LevelTickEvent.Post}.
 */
public final class NaniteClaims {
    public static final DeferredRegister<PoiType> POI = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Autarkia.MOD_ID);
    public static final DeferredHolder<PoiType, PoiType> MARKER_POI = POI.register("claim_marker",
        () -> new PoiType(Set.copyOf(AutarkiaBlocks.CLAIM_MARKER.get().getStateDefinition().getPossibleStates()), 0, 1));
    /** Blocks between the hive and its markers, and between the hive and every corner of a claim box (horizontal). */
    public static final int HIVE_RANGE = 64;
    /** Marker pair rows the hive lists (G5). */
    public static final int MAX_PAIRS = 8;
    private static final int MARKERS_SCANNED = 16;
    private static boolean halted;
    private static final Map<UUID, Stats> STATS = new HashMap<>();
    private static final Map<UUID, ServerPlayer> STARTERS = new HashMap<>();

    /** A marker pair the hive offers: the two markers and the top layer y of the box they span. */
    public record Pair(BlockPos a, BlockPos b, int yMax) {
        /** Return the box minimum corner at ref_y. */
        public BlockPos low() {
            return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        }

        /** Return the box maximum corner at the top layer. */
        public BlockPos high() {
            return new BlockPos(Math.max(a.getX(), b.getX()), yMax, Math.max(a.getZ(), b.getZ()));
        }
    }

    /** Runtime figures of one claim (not saved): first edited layer, edit units (1/8 edits), streams and bubbled containers. */
    public static final class Stats {
        public int firstEditY = Integer.MIN_VALUE;
        public long units;
        public int streams;
        public final Set<BlockPos> bubbled = new HashSet<>();
    }

    private NaniteClaims() {
    }

    /** Register the marker POI type (mod bus). */
    public static void register(IEventBus modBus) {
        POI.register(modBus);
    }

    /** Register the game listeners: the claim engine on every level tick and the runtime reset on server stop. */
    public static void registerGame() {
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
            if (e.getLevel() instanceof ServerLevel sl) {
                long t0 = System.nanoTime();
                NaniteEngine.tick(sl);
                dev.autarkia.command.SystemProfiler.add(dev.autarkia.command.SystemProfiler.Part.NANITES, t0);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            halted = false;
            STATS.clear();
            STARTERS.clear();
            NaniteEngine.clear();
        });
    }

    /** Return whether nanites may edit now: the config kill switch is on and no runtime stop holds. */
    public static boolean running() {
        return AutarkiaConfig.NANITE_ENABLED.get() && !halted;
    }

    /** Return whether {@code /autarkia nanite stop} holds. */
    public static boolean halted() {
        return halted;
    }

    /** Stop every claim at once (runtime): no edit runs from the next claim pass on; cursors stay. */
    public static void stop() {
        halted = true;
    }

    /** Lift the runtime stop; every claim continues at its cursor. */
    public static void resume() {
        halted = false;
    }

    /** Return the state a claim shows: PAUSED while a stop or the kill switch holds, else its own state. */
    public static NaniteClaim.State shown(NaniteClaim cl) {
        return cl.state != NaniteClaim.State.DONE && !running() ? NaniteClaim.State.PAUSED : cl.state;
    }

    /** Return the runtime figures of a claim. */
    public static Stats stats(UUID claim) {
        return STATS.computeIfAbsent(claim, k -> new Stats());
    }

    /** Return the player who started a claim while that player object is still in the game, or null. */
    static @Nullable ServerPlayer starter(UUID claim) {
        ServerPlayer p = STARTERS.get(claim);
        return p == null || p.hasDisconnected() ? null : p;
    }

    static void forget(UUID claim) {
        STATS.remove(claim);
        STARTERS.remove(claim);
    }

    /** Return the claim with the id in the colony, or null. */
    public static @Nullable NaniteClaim find(@Nullable UUID colony, UUID claim) {
        Colony c = colony == null || Colonies.server() == null ? null : Colonies.registry().get(colony);
        if (c == null) {
            return null;
        }
        for (NaniteClaim cl : c.claims) {
            if (cl.id.equals(claim)) {
                return cl;
            }
        }
        return null;
    }

    /** Return the unfinished claim of the hive at pos, or null (one claim per hive, G5). */
    public static @Nullable NaniteClaim hiveClaim(Colony c, BlockPos hive) {
        for (NaniteClaim cl : c.claims) {
            if (cl.hivePos.equals(hive) && cl.state != NaniteClaim.State.DONE) {
                return cl;
            }
        }
        return null;
    }

    /** Return the newest claim of the hive at pos, finished or not, or null. */
    public static @Nullable NaniteClaim lastClaim(Colony c, BlockPos hive) {
        NaniteClaim last = null;
        for (NaniteClaim cl : c.claims) {
            if (cl.hivePos.equals(hive)) {
                last = cl;
            }
        }
        return last;
    }

    /**
     * Return the claim shares of one tick (05 section 8.3): each of n claims gets floor(total / n) and the remainder goes
     * one each to the oldest claims, which come first.
     */
    public static int[] shares(int total, int n) {
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = total / n + (i < total % n ? 1 : 0);
        }
        return out;
    }

    /** Return the top layer y of a box (05 section 8.3): min(ref_y + maxY - 1, max(marker y, highest non-air in the box)). */
    public static int yMax(ServerLevel level, BlockPos a, BlockPos b) {
        int refY = Math.min(a.getY(), b.getY());
        int top = Math.max(a.getY(), b.getY());
        for (int x = Math.min(a.getX(), b.getX()); x <= Math.max(a.getX(), b.getX()); x++) {
            for (int z = Math.min(a.getZ(), b.getZ()); z <= Math.max(a.getZ(), b.getZ()); z++) {
                top = Math.max(top, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1);
            }
        }
        return Math.min(refY + AutarkiaConfig.NANITE_MAX_CLAIM_Y.get() - 1, top);
    }

    /**
     * Return the refusal message key for a box from the marker pair down to depth blocks below ref_y, or null when it
     * fits: markers in place within 64 blocks, X/Z and height limits, every corner within 64 blocks of the hive, fully in
     * the own domain, no overlap with another unfinished claim, a watched Pattern instance or an open project.
     */
    public static @Nullable String refusal(ServerLevel level, Colony c, BlockPos hive, BlockPos a, BlockPos b, int yMax, int depth) {
        if (!level.isLoaded(a) || !level.isLoaded(b) || !level.getBlockState(a).is(AutarkiaBlocks.CLAIM_MARKER.get())
            || !level.getBlockState(b).is(AutarkiaBlocks.CLAIM_MARKER.get()) || a.equals(b)) {
            return "message.autarkia.nanite.markers";
        }
        if (Math.max(a.distChessboard(hive), b.distChessboard(hive)) > HIVE_RANGE) {
            return "message.autarkia.nanite.range";
        }
        int maxXz = AutarkiaConfig.NANITE_MAX_CLAIM_XZ.get();
        if (Math.abs(a.getX() - b.getX()) + 1 > maxXz || Math.abs(a.getZ() - b.getZ()) + 1 > maxXz) {
            return "message.autarkia.nanite.size";
        }
        int refY = Math.min(a.getY(), b.getY());
        if (depth < 0 || yMax - (refY - depth) + 1 > AutarkiaConfig.NANITE_MAX_CLAIM_Y.get() || refY - depth < level.getMinY()) {
            return "message.autarkia.nanite.height";
        }
        BlockPos lo = new BlockPos(Math.min(a.getX(), b.getX()), refY - depth, Math.min(a.getZ(), b.getZ()));
        BlockPos hi = new BlockPos(Math.max(a.getX(), b.getX()), yMax, Math.max(a.getZ(), b.getZ()));
        for (int x : new int[] {lo.getX(), hi.getX()}) {
            for (int z : new int[] {lo.getZ(), hi.getZ()}) {
                double dx = x + 0.5 - (hive.getX() + 0.5);
                double dz = z + 0.5 - (hive.getZ() + 0.5);
                if (dx * dx + dz * dz > HIVE_RANGE * HIVE_RANGE) {
                    return "message.autarkia.nanite.range";
                }
            }
        }
        List<int[]> squares = Colonies.squares(c);
        for (int x = lo.getX(); x <= hi.getX(); x++) {
            for (int z = lo.getZ(); z <= hi.getZ(); z++) {
                if (!inSquares(squares, x, z)) {
                    return "message.autarkia.nanite.domain";
                }
            }
        }
        return overlap(level, c, lo, hi) ? "message.autarkia.nanite.overlap" : null;
    }

    private static boolean inSquares(List<int[]> squares, int x, int z) {
        for (int[] s : squares) {
            if (Math.abs(x - s[0]) <= s[2] && Math.abs(z - s[1]) <= s[2]) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlap(ServerLevel level, Colony c, BlockPos lo, BlockPos hi) {
        for (Colony o : Colonies.registry().all()) {
            if (!o.dimension.equals(level.dimension())) {
                continue;
            }
            for (NaniteClaim cl : o.claims) {
                if (cl.state != NaniteClaim.State.DONE && cl.intersects(lo, hi)) {
                    return true;
                }
            }
        }
        for (Colony.Instance in : c.instances) {
            BlockPos end = in.origin().offset(in.size().getX() - 1, in.size().getY() - 1, in.size().getZ() - 1);
            if (in.watched() && boxes(lo, hi, in.origin(), end)) {
                return true;
            }
        }
        for (Project p : c.openProjects()) {
            for (Project.Cell cell : p.cells) {
                BlockPos q = BlockPos.of(cell.pos());
                if (boxes(lo, hi, q, q)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean boxes(BlockPos lo, BlockPos hi, BlockPos a, BlockPos b) {
        return lo.getX() <= b.getX() && hi.getX() >= a.getX() && lo.getY() <= b.getY() && hi.getY() >= a.getY() && lo.getZ() <= b.getZ()
            && hi.getZ() >= a.getZ();
    }

    /**
     * Return up to 8 marker pairs the hive offers (G18): any 2 claim_markers within 64 blocks of the hive whose box (depth
     * 0) fits every rule, nearest box first; only the 16 nearest markers are paired.
     */
    public static List<Pair> pairs(ServerLevel level, Colony c, BlockPos hive) {
        List<BlockPos> markers = level.getPoiManager().getInSquare(h -> h.is(MARKER_POI.key()), hive, HIVE_RANGE, PoiManager.Occupancy.ANY)
            .map(r -> r.getPos().immutable()).filter(p -> Math.abs(p.getY() - hive.getY()) <= HIVE_RANGE)
            .sorted(Comparator.comparingDouble(p -> p.distSqr(hive))).limit(MARKERS_SCANNED).toList();
        List<Pair> out = new ArrayList<>();
        for (int i = 0; i < markers.size(); i++) {
            for (int j = i + 1; j < markers.size(); j++) {
                BlockPos a = markers.get(i);
                BlockPos b = markers.get(j);
                if (!level.isLoaded(a) || !level.isLoaded(b)) {
                    continue;
                }
                int top = yMax(level, a, b);
                if (refusal(level, c, hive, a, b, top, 0) == null) {
                    out.add(new Pair(a, b, top));
                }
            }
        }
        out.sort(Comparator.comparingDouble(p -> {
            double dx = (p.low().getX() + p.high().getX()) / 2.0 - hive.getX();
            double dz = (p.low().getZ() + p.high().getZ()) / 2.0 - hive.getZ();
            return dx * dx + dz * dz;
        }));
        return out.size() > MAX_PAIRS ? List.copyOf(out.subList(0, MAX_PAIRS)) : out;
    }

    /**
     * Start a claim of the hive at hivePos from the marker pair a, b with depth blocks below ref_y (colony_action
     * {@code claim}, owner or trusted); returns the refusal message key or null when the claim runs.
     */
    public static @Nullable String start(ServerLevel level, Colony c, BlockPos hivePos, BlockPos a, BlockPos b, int depth, boolean terrace,
                                         @Nullable ServerPlayer starter) {
        if (!(level.getBlockEntity(hivePos) instanceof NaniteHiveBlockEntity hive) || !hive.isCenter() || Colonies.at(level, hivePos) != c) {
            return "message.autarkia.nanite.no_hive";
        }
        if (hiveClaim(c, hivePos) != null) {
            return "message.autarkia.nanite.busy";
        }
        if (c.tier < 5) {
            return "message.autarkia.nanite.tier";
        }
        int top = yMax(level, a, b);
        String why = refusal(level, c, hivePos, a, b, top, depth);
        if (why != null) {
            return why;
        }
        int refY = Math.min(a.getY(), b.getY());
        NaniteClaim cl = NaniteClaim.create(hivePos, new BlockPos(Math.min(a.getX(), b.getX()), refY - depth, Math.min(a.getZ(), b.getZ())),
            new BlockPos(Math.max(a.getX(), b.getX()), top, Math.max(a.getZ(), b.getZ())), refY, terrace, level.getGameTime());
        c.claims.add(cl);
        if (starter != null) {
            STARTERS.put(cl.id, starter);
        }
        Colonies.changed();
        Autarkia.LOG.info("[nanite] claim {} started by hive {}: {} to {}, ref_y {}, terrace {}", cl.id.toString().substring(0, 8), hivePos.toShortString(),
            cl.min.toShortString(), cl.max.toShortString(), refY, terrace);
        return null;
    }

    /** Pause (or resume) the unfinished claim of the hive at hivePos; resume continues at the cursor. Returns false without a claim. */
    public static boolean pause(Colony c, BlockPos hivePos, boolean paused) {
        NaniteClaim cl = hiveClaim(c, hivePos);
        if (cl == null || paused == (cl.state == NaniteClaim.State.PAUSED)) {
            return cl != null;
        }
        cl.state = paused ? NaniteClaim.State.PAUSED : NaniteClaim.State.RUN;
        Colonies.changed();
        return true;
    }

    /** Handle colony_action {@code claim}: args hive pos, marker a, marker b (packed longs), depth, terrace on or off. */
    static void startAction(ServerPlayer player, Colony c, ServerLevel level, List<String> args) {
        String why = start(level, c, BlockPos.of(Long.parseLong(args.get(0))), BlockPos.of(Long.parseLong(args.get(1))), BlockPos.of(Long.parseLong(args.get(2))),
            Integer.parseInt(args.get(3)), args.get(4).equals("on"), player);
        player.sendOverlayMessage(Component.translatable(why == null ? "message.autarkia.nanite.started" : why));
    }

    /** Called when a hive center is removed: its unfinished claim ends without events and its swarms fly back to the spot and dissolve. */
    public static void hiveRemoved(ServerLevel level, BlockPos hivePos) {
        for (Colony c : Colonies.registry().all()) {
            if (!c.dimension.equals(level.dimension())) {
                continue;
            }
            for (NaniteClaim cl : List.copyOf(c.claims)) {
                if (cl.hivePos.equals(hivePos)) {
                    for (UUID s : cl.swarms) {
                        if (level.getEntity(s) instanceof dev.autarkia.worker.NaniteSwarmEntity sw) {
                            sw.recall();
                        }
                    }
                    if (cl.state != NaniteClaim.State.DONE) {
                        cl.state = NaniteClaim.State.DONE;
                        cl.ended = level.getGameTime();
                        Colonies.changed();
                    }
                }
            }
        }
    }

    /** Called when a swarm leaves the game for good (dissolve, death, discard): it leaves its claim and the worker registry. */
    public static void swarmGone(@Nullable UUID colony, @Nullable UUID claim, UUID swarm) {
        Colony c = colony == null || Colonies.server() == null ? null : Colonies.registry().get(colony);
        if (c == null) {
            return;
        }
        boolean changed = c.workers.remove(swarm) != null;
        NaniteClaim cl = claim == null ? null : find(colony, claim);
        if (cl != null) {
            changed |= cl.swarms.remove(swarm);
        }
        if (changed) {
            Colonies.changed();
        }
    }

    /** Return the lines of {@code /autarkia nanite status}: switches, then one line per claim of every colony. */
    public static List<String> status() {
        List<String> out = new ArrayList<>();
        out.add("nanite.enabled " + AutarkiaConfig.NANITE_ENABLED.get() + ", stopped " + halted + ", edits last tick "
            + String.format(java.util.Locale.ROOT, "%.3f", NaniteEngine.editsLastTick()) + " of " + AutarkiaConfig.NANITE_GLOBAL_EDITS_PER_TICK.get());
        for (Colony c : Colonies.registry().all()) {
            for (NaniteClaim cl : c.claims) {
                out.add(c.name + " claim " + cl.id.toString().substring(0, 8) + " hive " + cl.hivePos.toShortString() + " " + shown(cl).getSerializedName()
                    + " " + cl.percent() + "% layer " + Math.min(cl.layer(), cl.layers()) + "/" + cl.layers() + " cursor y " + cl.cursorY + " i " + cl.cursorI
                    + " swarms " + cl.swarms.size());
            }
        }
        return out;
    }
}

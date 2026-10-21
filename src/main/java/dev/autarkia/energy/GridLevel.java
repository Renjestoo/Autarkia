package dev.autarkia.energy;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.SpanListPayload;
import dev.autarkia.network.SpanStatePayload;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Runtime grid state of one server level (02 section 2.2 to 2.6): networks, node index, FIFO rebuild queue,
 * round-robin network ticking under the time budget, span physics (heat, wear, superconductor temperature), snaps,
 * quenches and client span sync.
 */
public final class GridLevel {
    final ServerLevel level;
    final GridSavedData data;
    final List<GridNetwork> networks = new ArrayList<>();
    final Long2ObjectOpenHashMap<GridNetwork> byNode = new Long2ObjectOpenHashMap<>();
    final Long2ObjectOpenHashMap<GridNetwork> second = new Long2ObjectOpenHashMap<>();
    final LongLinkedOpenHashSet dirty = new LongLinkedOpenHashSet();
    private final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap fuseOver = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
    private int nextNetId = 1;
    private int rr;
    private boolean loaded;
    public int rebuildsTotal;
    public int quenchesTotal;

    GridLevel(ServerLevel level) {
        this.level = level;
        this.data = GridSavedData.get(level);
    }

    /** Return the saved span data of the level. */
    public GridSavedData data() {
        return data;
    }

    /** Queue the node for a rebuild of its span component. */
    public void markDirty(long node) {
        dirty.add(node);
    }

    /** Return the network that holds the node, or null; a transformer node returns the network of its first tier. */
    public GridNetwork network(long node) {
        return byNode.get(node);
    }

    /** Return the network of the tier that holds the node, or null. */
    public GridNetwork network(long node, CableTier tier) {
        GridNetwork n = byNode.get(node);
        if (n != null && n.tier == tier) {
            return n;
        }
        GridNetwork s = second.get(node);
        return s != null && s.tier == tier ? s : null;
    }

    /** Return every network that holds the node (two for a transformer joining two tiers). */
    public List<GridNetwork> networks(long node) {
        List<GridNetwork> out = new ArrayList<>(2);
        GridNetwork n = byNode.get(node);
        if (n != null) {
            out.add(n);
        }
        GridNetwork s = second.get(node);
        if (s != null) {
            out.add(s);
        }
        return out;
    }

    /** Return true when the node is an open fuse (a graph cut, 02 section 6.3). */
    public boolean fuseOpen(long node) {
        GridSavedData.FuseState f = data.fuses.get(node);
        return f != null && f.open();
    }

    /** Return all live networks. */
    public List<GridNetwork> networks() {
        return networks;
    }

    void tick() {
        if (!loaded) {
            loaded = true;
            for (long n : data.byNode.keySet()) {
                dirty.add(n);
            }
        }
        int budget = AutarkiaConfig.GRID_REBUILDS_PER_TICK.get();
        while (!dirty.isEmpty() && budget-- > 0) {
            rebuild(dirty.removeFirstLong());
        }
        long now = level.getGameTime();
        long start = System.nanoTime();
        long limit = AutarkiaConfig.GRID_TICK_BUDGET_MICROS.get() * 1000L;
        int n = networks.size();
        List<Span> snapped = new ArrayList<>();
        List<Span> quenched = new ArrayList<>();
        int base = rr;
        for (int k = 0; k < n; k++) {
            GridNetwork net = networks.get((base + k) % n);
            if (net.lastTick == now) {
                continue;
            }
            if (System.nanoTime() - start > limit) {
                break;
            }
            int dt = net.lastTick < 0 ? 1 : (int) Math.min(20, now - net.lastTick);
            net.lagging = now - net.lastTick > 20 && net.lastTick >= 0;
            net.lastTick = now;
            if (net.tapsDirty) {
                net.tapsDirty = false;
                attachMembers(net);
            }
            net.tick(level, dt);
            physics(net, dt, now, snapped, quenched);
            rr = (base + k + 1) % n;
        }
        tickFuses(now);
        PhaseLinks.check(this, now);
        for (Span s : snapped) {
            Grid.snap(this, s);
        }
        for (Span s : quenched) {
            Grid.quench(this, s);
        }
        if (now % AutarkiaConfig.SPAN_SYNC_INTERVAL.get() == 0) {
            syncStates();
        }
    }

    private void physics(GridNetwork net, int dt, long now, List<Span> snapped, List<Span> quenched) {
        double heatMul = AutarkiaConfig.CABLE_HEAT_MULTIPLIER.get();
        double coolMul = AutarkiaConfig.CABLE_COOLING_MULTIPLIER.get();
        double wearMul = AutarkiaConfig.ROPE_WEAR_MULTIPLIER.get();
        for (Span s : net.spans) {
            double rated = (double) s.tier.rated * dt;
            if (s.tier == CableTier.ROPE) {
                double u = Math.abs(s.requestedFlow) / rated;
                s.slip = u > 1.0 + 1e-6;
                if (s.slip) {
                    s.wear += (float) (0.004 * (u - 1) * wearMul * dt);
                    if (now % 10 == 0) {
                        sound(s, "rope.squeal", 0.8f + 0.4f * (float) Math.min(u, 2));
                    }
                } else {
                    s.wear = (float) Math.max(0, s.wear - 0.0005 * dt);
                    if (u > 0.8 && now % 60 == 0) {
                        sound(s, "rope.creak", 1f);
                    }
                }
                s.load = (float) (s.slip ? Math.min(u, 2.55) : s.load);
                if (s.wear >= 1f) {
                    snapped.add(s);
                }
            } else if (s.tier.hasHeat()) {
                double u = s.load;
                double cool = 1.0 + (level.isRainingAt(BlockPos.containing(s.mid())) ? 0.25 : 0)
                    + (s.tier == CableTier.HV ? 0.5 * Wind.strength(level, s.mid()) : 0);
                s.heat += (float) (dt * (s.tier.heatGain * Math.max(0, u * u - 1) * heatMul - s.tier.heatCool * s.heat * cool * coolMul));
                s.heat = Math.max(0, s.heat);
                if (s.heat >= 0.7f && now % 20 == 0) {
                    sound(s, "wire.sizzle", 1.2f);
                    Vec3 m = s.mid();
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.x, m.y, m.z, 3, 0.2, 0.1, 0.2, 0.02);
                } else if (u > 0.8 && now % 80 == 0) {
                    sound(s, "wire.hum", 1.6f);
                }
                if (s.heat >= 1f) {
                    snapped.add(s);
                }
            } else if (s.tier == CableTier.SUPERCONDUCTOR) {
                superconductor(s, dt, now, quenched);
            }
        }
        data.setDirty();
    }

    /**
     * Superconductor span temperature over dt ticks (02 section 4.6): both ends cold {@code T = max(0, T - 0.01)}, else
     * {@code T + 0.002}; overload adds {@code 0.02 (u - 1)}. Below 0.3 the span plays coolant.frost every 100 ticks, from
     * 0.7 steam.hiss every 10 ticks, and at 1.0 (within float rounding of the per-tick steps) it is queued to quench.
     */
    private void superconductor(Span s, int dt, long now, List<Span> quenched) {
        boolean cold = Grid.cold(level, s.a) && Grid.cold(level, s.b);
        float t = cold ? Math.max(0f, s.temp - 0.01f * dt) : s.temp + 0.002f * dt;
        if (s.load > 1f) {
            t += 0.02f * (s.load - 1f) * dt;
        }
        s.temp = t;
        if (t >= 1f - 1e-4f) {
            quenched.add(s);
        } else if (t >= 0.7f && now % 10 == 0) {
            sound(s, "steam.hiss", 1f);
        } else if (t < 0.3f && now % 100 == 0) {
            sound(s, "coolant.frost", 1f);
        }
    }

    /**
     * Fuse boxes (02 section 6.3): a closed fuse trips when the load of its spans is at least 1.0 for 20 ticks in a
     * row or at least 1.5 once; an open fuse cools its idle spans and closes again after fuseResetTicks unless locked.
     */
    private void tickFuses(long now) {
        for (GridSavedData.FuseState f : new ArrayList<>(data.fuses.values())) {
            long node = f.pos();
            if (!nodeLoaded(node)) {
                continue;
            }
            IntArrayList ids = data.at(node);
            if (f.open()) {
                for (int i = 0; i < ids.size(); i++) {
                    Span s = data.spans.get(ids.getInt(i));
                    if (s != null) {
                        s.load = 0;
                        s.flow = 0;
                        s.heat = Math.max(0, s.heat - (float) (s.tier.heatCool * s.heat));
                    }
                }
                if (!f.locked() && now >= f.resetAt()) {
                    data.fuses.put(node, new GridSavedData.FuseState(node, false, f.resetAt(), f.recentTrips(), false, f.windowStart()));
                    fuseChanged(node);
                }
                continue;
            }
            float u = 0;
            for (int i = 0; i < ids.size(); i++) {
                Span s = data.spans.get(ids.getInt(i));
                if (s != null && s.active) {
                    u = Math.max(u, s.load);
                }
            }
            int over = u >= 1.0f ? fuseOver.get(node) + 1 : 0;
            fuseOver.put(node, over);
            if (u >= 1.5f || over >= 20) {
                tripFuse(node, "overload");
            }
        }
    }

    /** Open the fuse at node (overload or lightning); the third trip within 1,200 ticks locks it until a manual reset. */
    public void tripFuse(long node, String cause) {
        GridSavedData.FuseState f = data.fuses.get(node);
        if (f == null || f.open()) {
            return;
        }
        long now = level.getGameTime();
        boolean fresh = now - f.windowStart() > 1200 || f.recentTrips() == 0;
        int trips = fresh ? 1 : f.recentTrips() + 1;
        long window = fresh ? now : f.windowStart();
        boolean locked = trips >= 3;
        data.fuses.put(node, new GridSavedData.FuseState(node, true, now + AutarkiaConfig.FUSE_RESET_TICKS.get(), trips, locked, window));
        fuseOver.put(node, 0);
        BlockPos pos = BlockPos.of(node);
        level.playSound(null, pos, dev.autarkia.registry.AutarkiaSounds.of("fuse.trip"), SoundSource.BLOCKS, 0.8f, 0.6f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.05);
        dev.autarkia.colony.ColonyMemory.at(level, pos, "fuse_trip", Long.toString(node));
        Autarkia.LOG.info("[grid] fuse at {} tripped ({}), trip {} in window{}", pos.toShortString(), cause, trips, locked ? ", locked" : "");
        fuseChanged(node);
    }

    /** Close the fuse at node and clear its trip count and lock (GUI button or wrench). */
    public void resetFuse(long node) {
        if (data.fuses.containsKey(node)) {
            data.fuses.put(node, GridSavedData.FuseState.closed(node));
            fuseOver.put(node, 0);
            fuseChanged(node);
        }
    }

    private void fuseChanged(long node) {
        data.setDirty();
        markDirty(node);
        IntArrayList ids = data.at(node);
        for (int i = 0; i < ids.size(); i++) {
            Span s = data.spans.get(ids.getInt(i));
            if (s != null) {
                markDirty(s.other(node));
            }
        }
        if (level.getBlockEntity(BlockPos.of(node)) instanceof FuseBoxBlockEntity fb) {
            fb.stateChanged();
        }
    }

    private void sound(Span s, String id, float pitch) {
        Vec3 m = s.mid();
        level.playSound(null, m.x, m.y, m.z, dev.autarkia.registry.AutarkiaSounds.of(id), SoundSource.BLOCKS, 1f, pitch);
    }

    /**
     * Lamp ripple (02 section 2.6): when the span component of a snapped end has no supply left, every industrial lamp
     * in it goes dark {@code 3 ticks x graph hops} from that end. Lamps count at their own node or next to a connector.
     */
    void ripple(long start) {
        java.util.Map<Long, Integer> hops = new HashMap<>();
        LongArrayFIFOQueue q = new LongArrayFIFOQueue();
        hops.put(start, 0);
        q.enqueue(start);
        List<IndustrialLampBlockEntity> lamps = new ArrayList<>();
        List<Integer> lampHops = new ArrayList<>();
        boolean supply = false;
        while (!q.isEmpty()) {
            long cur = q.dequeueLong();
            int h = hops.get(cur);
            BlockPos pos = BlockPos.of(cur);
            List<GridMember> here = new ArrayList<>();
            GridMember own = Grid.memberAt(level, pos);
            if (own != null) {
                here.add(own);
            }
            if (level.getBlockState(pos).getBlock() instanceof GridConnector c && c.tapsNeighbours()) {
                for (Direction d : Direction.values()) {
                    GridMember tap = Grid.memberAt(level, pos.relative(d));
                    if (tap != null) {
                        here.add(tap);
                    }
                }
            }
            for (GridMember m : here) {
                if (m instanceof IndustrialLampBlockEntity lamp) {
                    lamps.add(lamp);
                    lampHops.add(h);
                } else if (m.gridRole() == GridMember.Role.GENERATOR && m.gridMaxOut() > 0 && m.gridBuffer().stored() > 0
                    || m.gridRole() == GridMember.Role.STORAGE && m.gridBuffer().stored() > 0) {
                    supply = true;
                }
            }
            IntArrayList ids = data.at(cur);
            for (int i = 0; i < ids.size(); i++) {
                Span s = data.spans.get(ids.getInt(i));
                if (s != null && !hops.containsKey(s.other(cur))) {
                    hops.put(s.other(cur), h + 1);
                    q.enqueue(s.other(cur));
                }
            }
        }
        if (supply) {
            return;
        }
        long now = level.getGameTime();
        for (int i = 0; i < lamps.size(); i++) {
            lamps.get(i).scheduleDark(now + 3L * lampHops.get(i));
        }
    }

    /** Rebuild the span component of the node: drop every network that touches it, then flood fill new ones. */
    void rebuild(long start) {
        rebuildsTotal++;
        LongOpenHashSet component = new LongOpenHashSet();
        LongArrayFIFOQueue q = new LongArrayFIFOQueue();
        q.enqueue(start);
        component.add(start);
        while (!q.isEmpty()) {
            long cur = q.dequeueLong();
            for (GridNetwork old : networks(cur)) {
                dropNetwork(old);
                for (long on : old.nodes) {
                    if (component.add(on)) {
                        q.enqueue(on);
                    }
                }
            }
            IntArrayList ids = data.at(cur);
            for (int i = 0; i < ids.size(); i++) {
                Span s = data.spans.get(ids.getInt(i));
                if (s != null && component.add(s.other(cur))) {
                    q.enqueue(s.other(cur));
                }
            }
        }
        Map<Long, java.util.EnumSet<CableTier>> done = new HashMap<>();
        for (long node : component) {
            if (!nodeLoaded(node) || fuseOpen(node)) {
                continue;
            }
            IntArrayList ids = data.at(node);
            for (int i = 0; i < ids.size(); i++) {
                Span s = data.spans.get(ids.getInt(i));
                if (s != null && !done.computeIfAbsent(node, k -> java.util.EnumSet.noneOf(CableTier.class)).contains(s.tier)) {
                    build(node, s.tier, done);
                }
            }
        }
        Autarkia.LOG.debug("[grid] rebuild from {} touched {} nodes", BlockPos.of(start), component.size());
    }

    /**
     * Flood fill one network of a single tier from the seed: only spans of that tier join it, so a transformer node
     * belongs to one network per tier, and an open fuse cuts the graph (its spans stay inactive).
     */
    private void build(long seed, CableTier tier, Map<Long, java.util.EnumSet<CableTier>> done) {
        LongArrayList nodes = new LongArrayList();
        List<Span> spans = new ArrayList<>();
        LongArrayFIFOQueue q = new LongArrayFIFOQueue();
        q.enqueue(seed);
        done.computeIfAbsent(seed, k -> java.util.EnumSet.noneOf(CableTier.class)).add(tier);
        while (!q.isEmpty()) {
            long cur = q.dequeueLong();
            nodes.add(cur);
            IntArrayList ids = data.at(cur);
            for (int i = 0; i < ids.size(); i++) {
                Span s = data.spans.get(ids.getInt(i));
                if (s == null || s.tier != tier) {
                    continue;
                }
                long o = s.other(cur);
                boolean active = nodeLoaded(o) && !fuseOpen(cur) && !fuseOpen(o) && !s.blocked;
                s.active = active;
                if (!active) {
                    continue;
                }
                if (s.a == cur) {
                    spans.add(s);
                }
                if (done.computeIfAbsent(o, k -> java.util.EnumSet.noneOf(CableTier.class)).add(tier)) {
                    q.enqueue(o);
                }
            }
        }
        if (spans.isEmpty()) {
            return;
        }
        GridNetwork net = new GridNetwork(nextNetId++, tier);
        net.nodes.addAll(nodes);
        net.spans.addAll(spans);
        attachMembers(net);
        net.buildTree();
        networks.add(net);
        for (long n : nodes) {
            if (byNode.containsKey(n)) {
                second.put(n, net);
            } else {
                byNode.put(n, net);
            }
        }
    }

    private void dropNetwork(GridNetwork net) {
        networks.remove(net);
        for (long n : net.nodes) {
            if (byNode.get(n) == net) {
                byNode.remove(n);
                GridNetwork other = second.remove(n);
                if (other != null) {
                    byNode.put(n, other);
                }
            } else if (second.get(n) == net) {
                second.remove(n);
            }
        }
        net.dead = true;
    }

    /**
     * Fill the member, foreign handler and injector lists of a network from its nodes and their taps. Members are
     * deduplicated by energy buffer, so the belts of one line (one shared head buffer) attach once.
     */
    void attachMembers(GridNetwork net) {
        net.members.clear();
        net.foreign.clear();
        net.injectors.clear();
        java.util.Set<Object> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (long node : net.nodes) {
            BlockPos pos = BlockPos.of(node);
            BlockEntity be = level.getBlockEntity(pos);
            GridMember own = be instanceof GridMultiMember mm ? mm.gridSide(net.tier) : be instanceof GridMember gm ? gm : null;
            if (own != null && seen.add(own.gridBuffer() == null ? own : own.gridBuffer())) {
                net.members.add(new GridNetwork.Attached(own, node));
            }
            if (be instanceof GridInjector inj && seen.add(inj)) {
                net.injectors.add(inj);
            }
            if (!(level.getBlockState(pos).getBlock() instanceof GridConnector c) || !c.tapsNeighbours()) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos np = pos.relative(d);
                if (level.getBlockState(np).getBlock() instanceof GridConnector) {
                    continue;
                }
                BlockEntity nbe = level.getBlockEntity(np);
                GridMember tap = Grid.memberAt(level, np);
                if (tap != null) {
                    if (seen.add(tap.gridBuffer())) {
                        net.members.add(new GridNetwork.Attached(tap, node));
                    }
                    continue;
                }
                if (nbe != null && level.getCapability(Capabilities.Energy.BLOCK, np, d.getOpposite()) != null) {
                    BlockCapabilityCache<net.neoforged.neoforge.transfer.energy.EnergyHandler, Direction> cache = BlockCapabilityCache.create(
                        Capabilities.Energy.BLOCK, level, np, d.getOpposite(), () -> !net.dead, () -> net.tapsDirty = true);
                    net.foreign.add(new GridNetwork.Foreign(cache, node));
                }
            }
        }
    }

    /** Return true when the chunk of the node is loaded on the server. */
    boolean nodeLoaded(long node) {
        return level.isLoaded(BlockPos.of(node));
    }

    /** Send span geometry for every span with an end in the chunk to one player. */
    void sendChunk(ServerPlayer player, ChunkPos chunk) {
        List<SpanListPayload.Entry> list = new ArrayList<>();
        for (Span s : data.spans.values()) {
            if (inChunk(s.a, chunk) || inChunk(s.b, chunk)) {
                list.add(entry(s));
            }
        }
        if (!list.isEmpty()) {
            AutarkiaNetwork.toPlayer(player, new SpanListPayload(list));
        }
    }

    /** Remove from one player the spans of the chunk whose other end is no longer tracked by that player. */
    void unsendChunk(ServerPlayer player, ChunkPos chunk) {
        List<SpanListPayload.Entry> list = new ArrayList<>();
        for (Span s : data.spans.values()) {
            boolean ina = inChunk(s.a, chunk);
            boolean inb = inChunk(s.b, chunk);
            if (!ina && !inb) {
                continue;
            }
            long other = ina ? s.b : s.a;
            ChunkPos oc = ChunkPos.containing(BlockPos.of(other));
            if (ina && inb || !player.getChunkTrackingView().contains(oc.x(), oc.z())) {
                list.add(new SpanListPayload.Entry(s.id, true, Vec3.ZERO, Vec3.ZERO, s.tier, 0, 0, 0, false));
            }
        }
        if (!list.isEmpty()) {
            AutarkiaNetwork.toPlayer(player, new SpanListPayload(list));
        }
    }

    /** Send a span add or remove to every player tracking either end. */
    void broadcast(Span s, boolean remove) {
        SpanListPayload p = new SpanListPayload(List.of(remove
            ? new SpanListPayload.Entry(s.id, true, Vec3.ZERO, Vec3.ZERO, s.tier, 0, 0, 0, false) : entry(s)));
        ChunkPos ca = ChunkPos.containing(BlockPos.of(s.a));
        ChunkPos cb = ChunkPos.containing(BlockPos.of(s.b));
        AutarkiaNetwork.toTracking(level, ca, p);
        if (!ca.equals(cb)) {
            AutarkiaNetwork.toTracking(level, cb, p);
        }
    }

    private void syncStates() {
        Map<ChunkPos, List<SpanStatePayload.Entry>> byChunk = new HashMap<>();
        for (Span s : data.spans.values()) {
            byte h = (byte) Math.round(Math.min(1, s.sagH()) * 255);
            byte l = (byte) Math.round(Math.min(2.55, s.load) * 100);
            byte t = (byte) Math.round(Math.min(1, s.temp) * 255);
            if (Math.abs((h & 0xFF) - (s.sentHeat & 0xFF)) <= 2 && Math.abs((l & 0xFF) - (s.sentLoad & 0xFF)) <= 2
                && Math.abs((t & 0xFF) - (s.sentTemp & 0xFF)) <= 2 && s.slip == s.sentSlip && s.active == s.sentActive) {
                continue;
            }
            s.sentHeat = h;
            s.sentLoad = l;
            s.sentTemp = t;
            s.sentSlip = s.slip;
            s.sentActive = s.active;
            SpanStatePayload.Entry e = new SpanStatePayload.Entry(s.id, h, l, t, s.slip, s.active);
            byChunk.computeIfAbsent(ChunkPos.containing(BlockPos.of(s.a)), k -> new ArrayList<>()).add(e);
            ChunkPos cb = ChunkPos.containing(BlockPos.of(s.b));
            if (!cb.equals(ChunkPos.containing(BlockPos.of(s.a)))) {
                byChunk.computeIfAbsent(cb, k -> new ArrayList<>()).add(e);
            }
        }
        byChunk.forEach((c, list) -> AutarkiaNetwork.toTracking(level, c, new SpanStatePayload(list)));
    }

    static SpanListPayload.Entry entry(Span s) {
        return new SpanListPayload.Entry(s.id, false, s.attachA, s.attachB, s.tier, s.sagH(), s.load, s.temp, s.active);
    }

    private static boolean inChunk(long node, ChunkPos c) {
        return (BlockPos.getX(node) >> 4) == c.x() && (BlockPos.getZ(node) >> 4) == c.z();
    }
}

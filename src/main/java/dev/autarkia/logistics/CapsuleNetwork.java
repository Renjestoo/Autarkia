package dev.autarkia.logistics;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.registry.AutarkiaAdvancements;
import dev.autarkia.registry.AutarkiaSounds;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The capsules of one tube network and their tick (04 sections 0.2 and 1.4). Every station owns one; on the server only
 * the anchor's network runs, on the client every station keeps the mirror it receives. Each tick: adopted capsules
 * join, a rebuilt network revalidates routes (capsules outside it go to the anchor of their part or drop, cut routes
 * re-target), every {@link #LAUNCH_INTERVAL} ticks each sending station launches its first input stack, all capsules
 * move ({@code tubeSpeedPu}; one capsule per tube block and travel direction, a capsule waits at the block boundary),
 * and capsules at their target mouth insert or wait. After {@link #CLOG_TICKS} at a mouth a capsule re-targets, else
 * returns to its origin input, else it is clogged and the network shows CLOGGED (red tubes, hiss, smoke). Capsules may
 * pass each other in opposite or crossing directions, so a two-way tube cannot deadlock.
 */
public final class CapsuleNetwork {
    /** Network or station state; NO_POWER is per station. */
    public enum State {
        RUNNING, CLOGGED, TOO_LARGE, PAUSED, NO_TUBE, NO_POWER, NO_STATION;

        /** Return the translation key of the state name. */
        public String key() {
            return "state.autarkia.capsule." + name().toLowerCase(Locale.ROOT);
        }

        /** Return true when capsules move in this network state. */
        public boolean moves() {
            return this == RUNNING || this == CLOGGED;
        }
    }

    /** A chosen target station and the route to it. */
    public record Choice(long station, byte[] path) {
    }

    private record Slot(long block, int dir) {
    }

    private record Candidate(CapsuleStationBlockEntity station, long pos, byte[] path) {
    }

    public static final int LAUNCH_FE = 40;
    public static final int LAUNCH_INTERVAL = 10;
    public static final int CLOG_TICKS = 600;
    public static final int RETRY_TICKS = 100;
    public static final int ADVANCEMENT_PU = 100 * Capsule.BLOCK;
    private static final Comparator<Candidate> ORDER = Comparator.<Candidate>comparingInt(c -> -c.station.priority())
        .thenComparingInt(c -> c.path.length).thenComparingLong(Candidate::pos);
    public final List<Capsule> capsules = new ArrayList<>();
    private final CapsuleStationBlockEntity owner;
    private final List<Capsule> incoming = new ArrayList<>();
    private final List<CapsuleDeltaPayload.Op> ops = new ArrayList<>();
    private CapsuleTopology topology = CapsuleTopology.EMPTY;
    private State state = State.RUNNING;
    private int nextId = 1;
    private boolean revalidate = true;
    private long revalidateAt = Long.MAX_VALUE;
    private boolean changed;
    private long lastSnapshot;

    public CapsuleNetwork(CapsuleStationBlockEntity owner) {
        this.owner = owner;
    }

    /** Return true when the owner is the anchor of its network view. */
    public boolean isAnchor() {
        return Long.valueOf(owner.getBlockPos().asLong()).equals(topology.anchor());
    }

    /** Return the view this network runs on. */
    public CapsuleTopology topology() {
        return topology;
    }

    /** Replace the network view after a flood; the next anchor tick revalidates all capsules. */
    public void setTopology(CapsuleTopology t) {
        topology = t;
        revalidate = true;
    }

    /** Return the network state (client: as last synced). */
    public State state() {
        return state;
    }

    /** Return true while this owner holds capsules. */
    public boolean holds() {
        return !capsules.isEmpty() || !incoming.isEmpty();
    }

    /** Return the number of capsules this owner holds. */
    public int size() {
        return capsules.size() + incoming.size();
    }

    /** Return the number of capsules that wait at their target mouth. */
    public int waiting() {
        int n = 0;
        for (Capsule c : capsules) {
            if (c.atEnd()) {
                n++;
            }
        }
        return n;
    }

    /** Take a capsule from another owner; it gets a new id at the next anchor tick. */
    public void adopt(Capsule c) {
        incoming.add(c);
    }

    /** Run one anchor tick (the caller checked that this owner is the anchor and the network can run). */
    public void tickAnchor(ServerLevel sl) {
        register(sl);
        long now = sl.getGameTime();
        if (!incoming.isEmpty()) {
            for (Capsule c : incoming) {
                Capsule n = c.withId(nextId++);
                capsules.add(n);
                ops.add(CapsuleDeltaPayload.Op.add(n));
            }
            incoming.clear();
            revalidate = true;
            changed = true;
        }
        if (revalidate || now >= revalidateAt) {
            revalidate = false;
            revalidateAt = Long.MAX_VALUE;
            redistribute(sl, true);
            validate(sl);
        }
        if (now % LAUNCH_INTERVAL == 0) {
            launches(sl);
        }
        move(AutarkiaConfig.TUBE_SPEED_PU.get());
        arrivals(sl);
        boolean clog = false;
        for (Capsule c : capsules) {
            clog |= c.clogged;
        }
        setState(clog ? State.CLOGGED : State.RUNNING);
        if (clog && now % 40 == 0) {
            clogEffects(sl);
        }
        flush(sl);
    }

    /** Hold the capsules without moving them (network too large, chunks not loaded or no anchor) and show the state. */
    public void pause(ServerLevel sl, State s) {
        if (holds()) {
            register(sl);
        }
        setState(s);
        flush(sl);
    }

    /** Hand every held capsule to the anchor of the part it is in, or drop it when that part has no station (non-anchor owner). */
    public void handOff(ServerLevel sl) {
        for (Capsule c : incoming) {
            capsules.add(c.withId(nextId++));
        }
        incoming.clear();
        redistribute(sl, false);
        flush(sl);
        if (holds()) {
            register(sl);
        } else {
            unregister(sl);
        }
    }

    /** Hand the capsules of a removed owner station to the anchor behind its mouth, or drop them where they are. */
    public void ownerRemoved(ServerLevel sl) {
        List<Capsule> all = new ArrayList<>(capsules);
        all.addAll(incoming);
        capsules.clear();
        incoming.clear();
        unregister(sl);
        if (all.isEmpty()) {
            return;
        }
        ops.add(CapsuleDeltaPayload.Op.clear());
        flush(sl);
        CapsuleTopology t = CapsuleTopology.flood(sl, owner.getBlockPos().relative(owner.mouth()), AutarkiaConfig.TUBE_MAX_NETWORK_TUBES.get());
        Long a = t.anchor();
        CapsuleStationBlockEntity next = a == null ? null : CapsuleNetworks.station(sl, a);
        for (Capsule c : all) {
            if (next != null && next != owner) {
                next.network().adopt(c);
            } else {
                drop(sl, c);
            }
        }
    }

    /** Remove this owner from the level registry (owner removed, unloaded or no longer holding). */
    public void unregister(Level level) {
        CapsuleNetworks.unregister(level, owner.getBlockPos().asLong(), this);
    }

    private void register(Level level) {
        CapsuleNetworks.register(level, owner.getBlockPos().asLong(), this);
    }

    private void setState(State s) {
        if (state != s) {
            state = s;
            ops.add(CapsuleDeltaPayload.Op.state(s.ordinal()));
            changed = true;
        }
    }

    private void redistribute(ServerLevel sl, boolean mine) {
        List<CapsuleTopology> parts = new ArrayList<>();
        boolean later = false;
        Iterator<Capsule> it = capsules.iterator();
        while (it.hasNext()) {
            Capsule c = it.next();
            long cb = c.currentBlock();
            if (mine && topology.tubes.contains(cb)) {
                continue;
            }
            CapsuleTopology part = partOf(sl, parts, cb);
            if (part.incomplete || part.tooLarge) {
                later = true;
                if (c.path.length > 0) {
                    c.route(cb, new byte[0], 0);
                    ops.add(CapsuleDeltaPayload.Op.route(c));
                }
                continue;
            }
            Long a = part.anchor();
            CapsuleStationBlockEntity st = a == null ? null : CapsuleNetworks.station(sl, a);
            if (st == owner) {
                continue;
            }
            if (st != null) {
                st.network().adopt(c);
            } else {
                drop(sl, c);
            }
            it.remove();
            ops.add(CapsuleDeltaPayload.Op.remove(c.id));
            changed = true;
        }
        if (later) {
            revalidateAt = sl.getGameTime() + 20;
        }
    }

    private static CapsuleTopology partOf(ServerLevel sl, List<CapsuleTopology> parts, long block) {
        for (CapsuleTopology p : parts) {
            if (p.tubes.contains(block)) {
                return p;
            }
        }
        CapsuleTopology t = CapsuleTopology.flood(sl, BlockPos.of(block), AutarkiaConfig.TUBE_MAX_NETWORK_TUBES.get());
        if (!t.tubes.isEmpty()) {
            parts.add(t);
        }
        return t;
    }

    private void validate(ServerLevel sl) {
        for (Capsule c : capsules) {
            if (topology.tubes.contains(c.currentBlock()) && !topology.intact(c)) {
                retarget(sl, c);
            }
        }
    }

    /**
     * Choose the target of a stack sent from a tube or station block: first the receiving stations whose non-empty
     * filter matches, then those with an empty filter; each group by highest priority, then shortest route. A target is
     * valid when its output buffer takes the stack on top of the capsules already reserved for it.
     *
     * @param ignoreId a capsule whose own reservation is left out (re-targeting), or -1
     */
    public @Nullable Choice choose(Level level, ItemStack stack, long from, int ignoreId) {
        List<Candidate> matched = new ArrayList<>();
        List<Candidate> sinks = new ArrayList<>();
        for (long s : topology.stations.keySet()) {
            CapsuleStationBlockEntity st = s == from ? null : CapsuleNetworks.station(level, s);
            if (st == null || !st.mode().receives()) {
                continue;
            }
            boolean empty = st.filterEmpty();
            if (!empty && !st.matches(stack)) {
                continue;
            }
            byte[] p = topology.path(from, s);
            if (p != null) {
                (empty ? sinks : matched).add(new Candidate(st, s, p));
            }
        }
        for (List<Candidate> group : List.of(matched, sinks)) {
            group.sort(ORDER);
            for (Candidate c : group) {
                if (c.station.fitsOutput(reserved(c.pos, ignoreId, false), stack)) {
                    return new Choice(c.pos, c.path);
                }
            }
        }
        return null;
    }

    private List<ItemStack> reserved(long station, int ignoreId, boolean returning) {
        List<ItemStack> out = new ArrayList<>();
        for (Capsule c : capsules) {
            if (c.target == station && c.returning == returning && c.id != ignoreId) {
                out.add(c.stack);
            }
        }
        return out;
    }

    private void retarget(ServerLevel sl, Capsule c) {
        long from = c.currentBlock();
        Choice ch = choose(sl, c.stack, from, c.id);
        if (ch != null) {
            reroute(c, from, ch.path(), ch.station(), false);
            return;
        }
        byte[] home = topology.path(from, c.origin);
        CapsuleStationBlockEntity o = CapsuleNetworks.station(sl, c.origin);
        if (home != null && o != null && o.fitsReturn(reserved(c.origin, c.id, true), c.stack)) {
            reroute(c, from, home, c.origin, true);
            return;
        }
        c.clogged = true;
        c.wait = CLOG_TICKS - RETRY_TICKS;
        if (c.path.length > 0 && !topology.intact(c)) {
            c.route(from, new byte[0], 0);
            ops.add(CapsuleDeltaPayload.Op.route(c));
            changed = true;
        }
    }

    private void reroute(Capsule c, long from, byte[] path, long target, boolean returning) {
        c.route(from, path, 0);
        c.target = target;
        c.returning = returning;
        c.clogged = false;
        c.wait = 0;
        ops.add(CapsuleDeltaPayload.Op.route(c));
        changed = true;
    }

    private void launches(ServerLevel sl) {
        int max = AutarkiaConfig.TUBE_MAX_CAPSULES.get();
        for (long s : topology.stations.keySet()) {
            CapsuleStationBlockEntity st = CapsuleNetworks.station(sl, s);
            if (st == null) {
                continue;
            }
            int slot = st.mode().sends() ? st.firstInput() : -1;
            st.setNoPower(slot >= 0 && st.energy().stored() < LAUNCH_FE);
            if (slot < 0 || st.energy().stored() < LAUNCH_FE || capsules.size() >= max) {
                continue;
            }
            ItemStack stack = st.inventory().get(slot).copy();
            Choice ch = choose(sl, stack, s, -1);
            if (ch == null) {
                continue;
            }
            Capsule c = new Capsule(nextId, stack, s, ch.path(), Capsule.HALF, s, ch.station());
            if (occupied(c)) {
                continue;
            }
            nextId++;
            st.energy().drain(LAUNCH_FE);
            st.inventory().put(slot, ItemStack.EMPTY);
            capsules.add(c);
            ops.add(CapsuleDeltaPayload.Op.add(c));
            changed = true;
            effects(sl, st, "capsule_launch");
        }
    }

    private boolean occupied(Capsule n) {
        Slot s = slot(n);
        for (Capsule c : capsules) {
            if (s != null && s.equals(slot(c))) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable Slot slot(Capsule c) {
        if (c.path.length == 0) {
            return null;
        }
        int i = c.occIndex(c.pos);
        return new Slot(c.blocks()[i], c.path[i]);
    }

    /**
     * Move every capsule one tick by speed pu (server and client run this same code). A capsule that would enter a
     * tube block already holding a capsule with the same exit direction stops 1 pu before the boundary; up to four
     * passes let a capsule follow one that moved on in the same tick.
     */
    public void move(int speed) {
        Map<Slot, Integer> occ = new HashMap<>();
        for (Capsule c : capsules) {
            c.prevPos = c.pos;
            Slot s = slot(c);
            if (s != null) {
                occ.put(s, c.id);
            }
        }
        int[] budget = new int[capsules.size()];
        Arrays.fill(budget, speed);
        boolean progress = true;
        for (int pass = 0; progress && pass < 4; pass++) {
            progress = false;
            for (int i = 0; i < capsules.size(); i++) {
                if (budget[i] > 0) {
                    int moved = step(capsules.get(i), budget[i], occ);
                    budget[i] -= moved;
                    progress |= moved > 0;
                }
            }
        }
    }

    private static int step(Capsule c, int budget, Map<Slot, Integer> occ) {
        int from = c.pos;
        int to = Math.min(from + budget, c.end());
        if (c.path.length == 0 || to <= from) {
            return 0;
        }
        int oi = c.occIndex(from);
        int ni = c.occIndex(to);
        if (ni != oi) {
            Slot next = new Slot(c.blocks()[ni], c.path[ni]);
            Integer other = occ.get(next);
            if (other != null && other != c.id) {
                to = Math.max(from, ni * Capsule.BLOCK - Capsule.HALF - 1);
                if (to <= from) {
                    return 0;
                }
            } else {
                occ.remove(new Slot(c.blocks()[oi], c.path[oi]), c.id);
                occ.put(next, c.id);
            }
        }
        c.pos = to;
        c.dist += to - from;
        return to - from;
    }

    private void arrivals(ServerLevel sl) {
        Iterator<Capsule> it = capsules.iterator();
        while (it.hasNext()) {
            Capsule c = it.next();
            if (c.path.length == 0) {
                if (topology.tubes.contains(c.currentBlock()) && ++c.wait >= CLOG_TICKS) {
                    retarget(sl, c);
                }
                continue;
            }
            if (!c.atEnd()) {
                continue;
            }
            CapsuleStationBlockEntity t = CapsuleNetworks.station(sl, c.target);
            if (t != null && (c.returning ? t.acceptReturn(c.stack) : t.acceptDelivery(c.stack))) {
                it.remove();
                ops.add(CapsuleDeltaPayload.Op.remove(c.id));
                changed = true;
                effects(sl, t, "capsule_arrive");
                if (c.dist >= ADVANCEMENT_PU) {
                    AutarkiaAdvancements.awardNear(sl, t.mouthPoint(), "logistics/pneumatic");
                }
                continue;
            }
            if (++c.wait >= CLOG_TICKS) {
                retarget(sl, c);
            }
        }
    }

    private void clogEffects(ServerLevel sl) {
        Set<Long> done = new HashSet<>();
        for (Capsule c : capsules) {
            if (c.pos != c.prevPos || !done.add(c.currentBlock()) || done.size() > 8) {
                continue;
            }
            Vec3 p = Vec3.atCenterOf(BlockPos.of(c.currentBlock()));
            sl.sendParticles(ParticleTypes.WHITE_SMOKE, p.x, p.y, p.z, 2, 0.15, 0.15, 0.15, 0.01);
            sl.playSound(null, p.x, p.y, p.z, AutarkiaSounds.of("tube_hiss"), SoundSource.BLOCKS, 1f, 1f);
        }
    }

    private static void effects(ServerLevel sl, CapsuleStationBlockEntity st, String sound) {
        Vec3 m = st.mouthPoint();
        sl.playSound(null, m.x, m.y, m.z, AutarkiaSounds.of(sound), SoundSource.BLOCKS, 1f, 1f);
        sl.sendParticles(ParticleTypes.POOF, m.x, m.y, m.z, 3, 0.08, 0.08, 0.08, 0.01);
        st.puff();
    }

    private static void drop(ServerLevel sl, Capsule c) {
        Vec3 p = c.point(c.pos);
        Containers.dropItemStack(sl, p.x, p.y, p.z, c.stack.copy());
    }

    void dropIn(ServerLevel sl, long block) {
        boolean any = false;
        Iterator<Capsule> it = capsules.iterator();
        while (it.hasNext()) {
            Capsule c = it.next();
            if (c.currentBlock() == block) {
                drop(sl, c);
                it.remove();
                ops.add(CapsuleDeltaPayload.Op.remove(c.id));
                any = true;
            }
        }
        if (any) {
            changed = true;
            revalidate = true;
            flush(sl);
        }
    }

    /** Send pending ops to every player tracking a chunk of the network, and a snapshot at most every 100 ticks. */
    public void flush(ServerLevel sl) {
        long now = sl.getGameTime();
        if (!ops.isEmpty()) {
            CapsuleDeltaPayload p = new CapsuleDeltaPayload(owner.getBlockPos(), now, List.copyOf(ops));
            ops.clear();
            Set<ServerPlayer> to = new LinkedHashSet<>(sl.getChunkSource().chunkMap.getPlayers(ChunkPos.containing(owner.getBlockPos()), false));
            for (BlockPos probe : topology.chunkProbes) {
                to.addAll(sl.getChunkSource().chunkMap.getPlayers(ChunkPos.containing(probe), false));
            }
            for (ServerPlayer player : to) {
                AutarkiaNetwork.toPlayer(player, p);
            }
            owner.setChanged();
        }
        if (changed && !capsules.isEmpty() && now - lastSnapshot >= 100) {
            lastSnapshot = now;
            changed = false;
            owner.syncNow();
        }
    }

    /** Apply synced ops to the client mirror. */
    public void apply(List<CapsuleDeltaPayload.Op> list) {
        for (CapsuleDeltaPayload.Op op : list) {
            switch (op.kind()) {
                case CapsuleDeltaPayload.ADD -> {
                    capsules.removeIf(c -> c.id == op.id());
                    capsules.add(new Capsule(op.id(), op.stack(), op.start(), op.path(), op.pos(), 0, 0));
                    capsules.sort(Comparator.comparingInt(c -> c.id));
                }
                case CapsuleDeltaPayload.REMOVE -> capsules.removeIf(c -> c.id == op.id());
                case CapsuleDeltaPayload.ROUTE -> {
                    for (Capsule c : capsules) {
                        if (c.id == op.id()) {
                            c.route(op.start(), op.path(), op.pos());
                        }
                    }
                }
                case CapsuleDeltaPayload.STATE -> state = State.values()[Math.clamp(op.pos(), 0, State.values().length - 1)];
                default -> capsules.clear();
            }
        }
    }

    /** Run one client tick: move the mirror like the server and publish it to the tube renderers. */
    public void clientTick(Level level) {
        if (state.moves() && !capsules.isEmpty()) {
            move(AutarkiaConfig.TUBE_SPEED_PU.get());
        }
        if (capsules.isEmpty()) {
            clientRemove(level);
            return;
        }
        Map<Long, List<Capsule>> byBlock = new HashMap<>();
        for (Capsule c : capsules) {
            c.glow = state == State.CLOGGED && c.pos == c.prevPos;
            long a = c.blocks()[c.occIndex(c.prevPos)];
            long b = c.blocks()[c.occIndex(c.pos)];
            byBlock.computeIfAbsent(a, k -> new ArrayList<>()).add(c);
            if (b != a) {
                byBlock.computeIfAbsent(b, k -> new ArrayList<>()).add(c);
            }
        }
        CapsuleNetworks.publish(level, owner.getBlockPos().asLong(), byBlock);
    }

    /** Remove this owner's capsules from the client render index. */
    public void clientRemove(Level level) {
        CapsuleNetworks.unpublish(level, owner.getBlockPos().asLong());
    }

    /** Write the held capsules and the state. */
    public void save(ValueOutput out) {
        ValueOutput.ValueOutputList list = out.childrenList("capsules");
        for (Capsule c : capsules) {
            c.save(list.addChild());
        }
        for (Capsule c : incoming) {
            c.save(list.addChild());
        }
        out.putInt("nextId", nextId);
        out.putInt("state", state.ordinal());
    }

    /** Read the held capsules and the state (replaces the current capsules; duplicate ids get new ones). */
    public void load(ValueInput in) {
        capsules.clear();
        incoming.clear();
        nextId = Math.max(nextId, in.getIntOr("nextId", 1));
        Set<Integer> ids = new HashSet<>();
        List<Capsule> later = new ArrayList<>();
        for (ValueInput ci : in.childrenListOrEmpty("capsules")) {
            Capsule c = Capsule.load(ci);
            if (c != null) {
                (ids.add(c.id) ? capsules : later).add(c);
                nextId = Math.max(nextId, c.id + 1);
            }
        }
        for (Capsule c : later) {
            capsules.add(c.withId(nextId++));
        }
        capsules.sort(Comparator.comparingInt(c -> c.id));
        state = State.values()[Math.clamp(in.getIntOr("state", 0), 0, State.values().length - 1)];
        revalidate = true;
    }
}

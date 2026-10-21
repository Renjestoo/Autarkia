package dev.autarkia.logistics;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The moving part of a cable car line (04 section 1.5), held, saved and ticked by the owner station (lowest
 * {@code BlockPos.asLong}). All gondolas grip one cable loop at even spacing (loop length / count), so the loop stops
 * whenever a gondola reaches a stop. At a stop a gondola unloads into the station output, then loads from its input;
 * the loop leaves after 60 ticks once every stopped gondola is full, else after 200 ticks. With both inputs and every
 * gondola empty the loop parks and draws no power; otherwise it draws 64 FE per tick from the owner buffer, then the
 * other station. Speed 300 pu per tick, 150 while the largest swing is above 30 degrees; above
 * {@code gondolaHoldDegrees} the loop stops (WIND_HOLD) until the swing stays below 20 degrees for 100 ticks. A cabin
 * that touches a solid block stops the loop (OBSTRUCTED) until the block is gone. The line ticks only while every chunk
 * under it is loaded (rule C3). Count changes apply at the next stop.
 */
public final class CableLine {
    /** Line state shown in the menu and by Jade. */
    public enum State {
        RUNNING, SLOWED, WIND_HOLD, OBSTRUCTED, PARKED, NO_POWER, PAUSED, NO_GONDOLAS;

        /** Return the translation key of the state name. */
        public String key() {
            return "state.autarkia.cable_car." + name().toLowerCase(Locale.ROOT);
        }
    }

    public static final int SPEED_PU = 300;
    public static final int FE_PER_TICK = 64;
    public static final int STOP_MIN = 60;
    public static final int STOP_MAX = 200;
    public static final double SLOW_DEGREES = 30;
    public static final double RESUME_DEGREES = 20;
    public static final int RESUME_TICKS = 100;
    private static final Map<Level, Map<Long, CableLine>> LIVE = new WeakHashMap<>();
    private final CableCarStationBlockEntity owner;
    private final List<UUID> gondolas = new ArrayList<>();
    private CableGeometry geo;
    private List<ChunkPos> chunks;
    private long offset;
    private int target = 1;
    private boolean stopped = true;
    private int stopTicks;
    private boolean parked;
    private boolean hold;
    private int calmTicks;
    private @Nullable BlockPos obstruction;
    private State state = State.NO_GONDOLAS;
    private double maxSwing;
    private int moved;

    CableLine(CableCarStationBlockEntity owner, CableGeometry geo) {
        this.owner = owner;
        this.geo = geo;
        this.chunks = List.copyOf(geo.chunks());
        this.offset = geo.stopA();
    }

    /** Return the number of lines of the level in WIND_HOLD (sensor {@code transport.gondola_wind_hold_lines}). */
    public static int windHoldLines(Level level) {
        return (int) LIVE.getOrDefault(level, Map.of()).values().stream().filter(l -> l.state == State.WIND_HOLD).count();
    }

    /** Return the blocks that stop gondolas in the level (sensor {@code transport.gondola_obstructed}). */
    public static List<BlockPos> obstructed(Level level) {
        List<BlockPos> out = new ArrayList<>();
        for (CableLine l : LIVE.getOrDefault(level, Map.of()).values()) {
            if (l.state == State.OBSTRUCTED && l.obstruction != null) {
                out.add(l.obstruction);
            }
        }
        return out;
    }

    /** Return the number of lines of the level that move or serve a stop (sensor {@code transport.cable_line_running}). */
    public static int runningLines(Level level) {
        return (int) LIVE.getOrDefault(level, Map.of()).values().stream().filter(l -> l.state == State.RUNNING || l.state == State.SLOWED).count();
    }

    void register(Level level) {
        LIVE.computeIfAbsent(level, k -> new HashMap<>()).put(owner.getBlockPos().asLong(), this);
    }

    void unregister(Level level) {
        Map<Long, CableLine> m = LIVE.get(level);
        if (m != null) {
            m.remove(owner.getBlockPos().asLong(), this);
        }
    }

    /** Return the line geometry. */
    public CableGeometry geometry() {
        return geo;
    }

    /** Return the state. */
    public State state() {
        return state;
    }

    /** Return the block that stops the loop, or null. */
    public @Nullable BlockPos obstruction() {
        return obstruction;
    }

    /** Return the largest gondola swing of the last tick in degrees. */
    public double maxSwing() {
        return maxSwing;
    }

    /** Return true while the loop stands at a stop (serving, parked or waiting to leave). */
    public boolean stopped() {
        return stopped;
    }

    /** Return the pu the loop moved in the last tick. */
    public int movedLastTick() {
        return moved;
    }

    /** Return the gondola count the player set. */
    public int target() {
        return target;
    }

    /** Return the largest gondola count of this line: floor(2 x length / 16), at most 8. */
    public int maxCount() {
        return Math.max(1, Math.min(8, (int) Math.floor(2 * geo.chord / 16)));
    }

    /** Set the gondola count, clamped to 1 to {@link #maxCount()}; it applies at the next stop. */
    public void setTarget(int n) {
        target = Math.clamp(n, 1, maxCount());
        owner.setChanged();
    }

    /** Return the number of gondolas on the loop. */
    public int count() {
        return gondolas.size();
    }

    /** Return true when the gondola belongs to this line. */
    public boolean holds(UUID id) {
        return gondolas.contains(id);
    }

    /** Return the live gondola entities in loop order (missing ones are skipped). */
    public List<GondolaEntity> gondolas(ServerLevel sl) {
        List<GondolaEntity> out = new ArrayList<>();
        for (UUID id : gondolas) {
            if (sl.getEntity(id) instanceof GondolaEntity g && g.isAlive()) {
                out.add(g);
            }
        }
        return out;
    }

    /** Return the loop position of gondola i in pu. */
    public long position(int i) {
        return offset + (long) i * geo.loopPu() / Math.max(1, gondolas.size());
    }

    private boolean loaded(ServerLevel sl) {
        for (ChunkPos c : chunks) {
            if (!sl.isLoaded(c.getWorldPosition()) || !sl.areEntitiesLoaded(c.pack())) {
                return false;
            }
        }
        return true;
    }

    private static Predicate<BlockState> stations() {
        return st -> st.is(AutarkiaBlocks.CABLE_CAR_STATION.get());
    }

    /** Hold the line while the other station's chunk is not loaded (rule C3). */
    void pause(Level level) {
        register(level);
        moved = 0;
        state = State.PAUSED;
    }

    /** Advance the line by one tick; other is the loaded partner station. */
    void tick(ServerLevel sl, CableCarStationBlockEntity other) {
        register(sl);
        moved = 0;
        if (!loaded(sl)) {
            state = State.PAUSED;
            return;
        }
        List<GondolaEntity> gs = resolve(sl);
        if (stopped || gs.isEmpty()) {
            gs = reconfigure(sl, gs);
        }
        if (gs.isEmpty()) {
            state = State.NO_GONDOLAS;
            return;
        }
        BlockPos hit = null;
        maxSwing = 0;
        for (int i = 0; i < gs.size(); i++) {
            BlockPos h = gs.get(i).swing(sl, geo, geo.hang(position(i)), stations());
            hit = hit == null ? h : hit;
            maxSwing = Math.max(maxSwing, gs.get(i).swingDegrees());
        }
        updateHold();
        if (hit == null && !stopped && !hold) {
            hit = aheadBlocked(sl, gs, maxSwing > SLOW_DEGREES ? SPEED_PU / 2 : SPEED_PU);
        }
        if (hit != null && obstruction == null) {
            Vec3 g = gs.get(0).position();
            sl.playSound(null, BlockPos.containing(g), AutarkiaSounds.of("gondola_strike"), SoundSource.BLOCKS, 0.8f, 0.8f);
            Autarkia.LOG.info("[cable car] line {} obstructed at {}", owner.getBlockPos().toShortString(), hit.toShortString());
        }
        obstruction = hit;
        if (obstruction != null) {
            state = State.OBSTRUCTED;
        } else if (hold) {
            state = State.WIND_HOLD;
            if (sl.getGameTime() % 10 == 0) {
                for (GondolaEntity g : gs) {
                    sl.sendParticles(ParticleTypes.SMALL_GUST, g.getX(), g.getY() + 1, g.getZ(), 2, 0.8, 0.4, 0.8, 0.0);
                }
            }
        } else if (stopped) {
            serveStop(sl, other, gs);
        } else {
            move(sl, other, gs);
        }
        for (int i = 0; i < gs.size(); i++) {
            gs.get(i).place(geo.hang(position(i)), geo.yaw());
            if (sl.getGameTime() % 2 == 0) {
                gs.get(i).syncSwing();
            }
        }
        boolean running = state == State.RUNNING || state == State.SLOWED;
        owner.setRunning(running);
        other.setRunning(running);
        if (moved > 0 && sl.getGameTime() % 20 == 0) {
            sl.playSound(null, owner.getBlockPos(), AutarkiaSounds.of("cable_car_run"), SoundSource.BLOCKS, 0.3f, 0.5f);
        }
    }

    private List<GondolaEntity> resolve(ServerLevel sl) {
        List<GondolaEntity> out = new ArrayList<>();
        for (UUID id : List.copyOf(gondolas)) {
            if (sl.getEntity(id) instanceof GondolaEntity g && g.isAlive() && !g.falling()) {
                out.add(g);
            } else {
                Autarkia.LOG.info("[cable car] gondola {} of line {} is gone", id, owner.getBlockPos().toShortString());
                gondolas.remove(id);
                owner.setChanged();
            }
        }
        return out;
    }

    private void updateHold() {
        double holdDeg = AutarkiaConfig.GONDOLA_HOLD_DEGREES.get();
        if (hold) {
            calmTicks = maxSwing < RESUME_DEGREES ? calmTicks + 1 : 0;
            if (calmTicks >= RESUME_TICKS) {
                hold = false;
                calmTicks = 0;
            }
        } else if (maxSwing > holdDeg) {
            hold = true;
            calmTicks = 0;
        }
    }

    private @Nullable BlockPos aheadBlocked(ServerLevel sl, List<GondolaEntity> gs, int speed) {
        for (int i = 0; i < gs.size(); i++) {
            GondolaEntity g = gs.get(i);
            BlockPos h = GondolaEntity.blocked(sl, geo.cabin(geo.hang(position(i) + nextStep(speed)), g.thetaAlong(), g.thetaAcross()), stations());
            if (h != null) {
                return h;
            }
        }
        return null;
    }

    private int nextStep(int speed) {
        int step = speed;
        for (int i = 0; i < gondolas.size(); i++) {
            for (int stop : new int[] {geo.stopA(), geo.stopB()}) {
                long d = Math.floorMod(stop - position(i), (long) geo.loopPu());
                if (d > 0 && d < step) {
                    step = (int) d;
                }
            }
        }
        return step;
    }

    private boolean atStop(int i) {
        long p = Math.floorMod(position(i), (long) geo.loopPu());
        return p == geo.stopA() % geo.loopPu() || p == geo.stopB();
    }

    private CableCarStationBlockEntity stationAt(int i, CableCarStationBlockEntity other) {
        return Math.floorMod(position(i), (long) geo.loopPu()) == geo.stopB() ? other : owner;
    }

    private boolean power(CableCarStationBlockEntity other) {
        if (owner.energy().stored() + other.energy().stored() < FE_PER_TICK) {
            return false;
        }
        int took = owner.energy().drain(FE_PER_TICK);
        other.energy().drain(FE_PER_TICK - took);
        return true;
    }

    private void move(ServerLevel sl, CableCarStationBlockEntity other, List<GondolaEntity> gs) {
        if (!power(other)) {
            state = State.NO_POWER;
            return;
        }
        boolean slow = maxSwing > SLOW_DEGREES;
        state = slow ? State.SLOWED : State.RUNNING;
        int step = nextStep(slow ? SPEED_PU / 2 : SPEED_PU);
        offset = Math.floorMod(offset + step, (long) geo.loopPu());
        moved = step;
        for (int i = 0; i < gs.size(); i++) {
            if (atStop(i)) {
                stopped = true;
                stopTicks = 0;
            }
        }
        if (stopped) {
            for (int i = 0; i < gs.size(); i++) {
                if (atStop(i)) {
                    arrive(sl, gs.get(i), stationAt(i, other));
                }
            }
        }
        owner.setChanged();
    }

    private void arrive(ServerLevel sl, GondolaEntity g, CableCarStationBlockEntity station) {
        if (!g.emptyCargo() && geo.horizontal >= 64 && g.tripSwing() > 15) {
            owner.award(sl, g.position(), "logistics/sky_freight");
        }
        if (!g.emptyCargo()) {
            g.openDoors();
        }
        station.unload(g);
    }

    private void serveStop(ServerLevel sl, CableCarStationBlockEntity other, List<GondolaEntity> gs) {
        boolean idle = owner.inputEmpty() && other.inputEmpty() && gs.stream().allMatch(GondolaEntity::emptyCargo);
        if (parked) {
            if (idle) {
                state = State.PARKED;
                return;
            }
            parked = false;
            stopTicks = 0;
        }
        if (!power(other)) {
            state = State.NO_POWER;
            return;
        }
        state = State.RUNNING;
        stopTicks++;
        boolean ready = true;
        for (int i = 0; i < gs.size(); i++) {
            if (atStop(i)) {
                GondolaEntity g = gs.get(i);
                stationAt(i, other).load(g);
                ready &= g.full() || stopTicks >= STOP_MAX;
            }
        }
        if (stopTicks < STOP_MIN) {
            return;
        }
        if (idle) {
            parked = true;
            state = State.PARKED;
            return;
        }
        if (ready) {
            stopped = false;
            for (GondolaEntity g : gs) {
                g.resetTrip();
            }
        }
        owner.setChanged();
    }

    private List<GondolaEntity> reconfigure(ServerLevel sl, List<GondolaEntity> gs) {
        List<GondolaEntity> live = new ArrayList<>(gs);
        boolean changed = false;
        while (live.size() > target) {
            GondolaEntity g = live.get(live.size() - 1);
            if (!owner.retire(sl, g)) {
                break;
            }
            live.remove(g);
            gondolas.remove(g.getUUID());
            changed = true;
        }
        while (live.size() < target && owner.takeParts()) {
            GondolaEntity g = GondolaEntity.spawn(sl, owner.getBlockPos(), geo.hang(geo.stopA()), geo.yaw());
            live.add(g);
            gondolas.add(g.getUUID());
            changed = true;
        }
        if (changed) {
            offset = geo.stopA();
            stopped = true;
            stopTicks = 0;
            parked = false;
            owner.setChanged();
            Autarkia.LOG.info("[cable car] line {} now runs {} gondolas", owner.getBlockPos().toShortString(), live.size());
        }
        return live;
    }

    /** Let every gondola fall and forget them (a station of the line broke). */
    void detachAll(ServerLevel sl) {
        for (GondolaEntity g : gondolas(sl)) {
            g.detach();
        }
        gondolas.clear();
        unregister(sl);
        owner.setChanged();
    }

    /** Rebuild the geometry after a load or a link. */
    void setGeometry(CableGeometry geo) {
        this.geo = geo;
        this.chunks = List.copyOf(geo.chunks());
        if (offset >= geo.loopPu()) {
            offset = geo.stopA();
        }
    }

    void save(ValueOutput out) {
        out.store("gondolas", UUIDUtil.CODEC.listOf(), List.copyOf(gondolas));
        out.putLong("offset", offset);
        out.putInt("target", target);
        out.putBoolean("stopped", stopped);
        out.putInt("stopTicks", stopTicks);
        out.putBoolean("parked", parked);
        out.putBoolean("hold", hold);
        out.putInt("calmTicks", calmTicks);
        out.putInt("state", state.ordinal());
        if (obstruction != null) {
            out.store("obstruction", BlockPos.CODEC, obstruction);
        }
    }

    void load(ValueInput in) {
        gondolas.clear();
        gondolas.addAll(in.read("gondolas", UUIDUtil.CODEC.listOf()).orElse(List.of()));
        offset = Math.floorMod(in.getLongOr("offset", geo.stopA()), (long) geo.loopPu());
        target = Math.max(1, in.getIntOr("target", 1));
        stopped = in.getBooleanOr("stopped", true);
        stopTicks = in.getIntOr("stopTicks", 0);
        parked = in.getBooleanOr("parked", false);
        hold = in.getBooleanOr("hold", false);
        calmTicks = in.getIntOr("calmTicks", 0);
        state = State.values()[Math.clamp(in.getIntOr("state", State.NO_GONDOLAS.ordinal()), 0, State.values().length - 1)];
        obstruction = in.read("obstruction", BlockPos.CODEC).orElse(null);
    }
}

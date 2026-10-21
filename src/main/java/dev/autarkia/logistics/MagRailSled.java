package dev.autarkia.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.storage.FallingCrateEntity;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A sled: a {@code falling_crate} in mode {@code RAIL} (04 section 1.7). It floats 0.4 blocks above the track along its
 * route (start cell plus one direction byte per cell, position in pu, 1 block = 1,000 pu), accelerates by 50 pu per tick
 * to 1,000 pu per tick and brakes linearly to 100 pu per tick over the last 20 blocks (G6). It keeps 8 blocks behind the
 * nearest sled ahead in its direction and passes through stations. It waits while one of the next cells is in a chunk
 * that is not loaded or not entity-ticking (rule C3). Entering a cell that is no longer connected track turns it into a
 * {@code COLLAPSE} crate at the gap, counted for {@code transport.rail_breaks}. A living entity on the track takes 6
 * damage and a push, once per sled. At the last cell it unloads into the station's output and crate slot in one step,
 * or waits above the station while they lack room; a delivery over 256 or more rails grants {@code logistics/float_on}
 * to the players within 32 blocks and to the player who set up the sending station. {@code ELECTRIC_SPARK} under the
 * sled every 4 ticks and {@code rail_hum} every 40 ticks while it moves.
 */
public final class MagRailSled {
    public static final int MAX_SPEED = 1000;
    public static final int ACCEL = 50;
    public static final int BRAKE_PU = 20000;
    public static final int BRAKE_MIN = 100;
    public static final double FLOAT = 0.4;
    public static final int FLOAT_ON_RAILS = 256;
    public static final float HIT_DAMAGE = 6.0f;
    public static final int BREAK_WINDOW = 24000;
    private static final Map<Level, List<Long>> BREAKS = new WeakHashMap<>();

    private MagRailSled() {
    }

    /** The route and motion state of one sled; {@code hit} holds the entities it already struck (not saved). */
    public static final class Route {
        public static final Codec<Route> CODEC = RecordCodecBuilder.create(i -> i.group(BlockPos.CODEC.fieldOf("start").forGetter(r -> r.start),
            Codec.BYTE.listOf().fieldOf("steps").forGetter(Route::stepList), Codec.LONG.fieldOf("pos").forGetter(r -> r.pos),
            Codec.INT.fieldOf("speed").forGetter(r -> r.speed), Codec.INT.fieldOf("rails").forGetter(r -> r.rails),
            Codec.BOOL.optionalFieldOf("arrived", false).forGetter(r -> r.arrived)).apply(i, Route::new));
        private final BlockPos start;
        private final byte[] steps;
        private final List<BlockPos> cells = new ArrayList<>();
        private final int rails;
        private final IntOpenHashSet hit = new IntOpenHashSet();
        public final LinkerRef linker = new LinkerRef();
        private long pos;
        private int speed;
        private int moved;
        private boolean arrived;

        private Route(BlockPos start, List<Byte> steps, long pos, int speed, int rails, boolean arrived) {
            this.start = start.immutable();
            this.steps = new byte[steps.size()];
            for (int k = 0; k < steps.size(); k++) {
                this.steps[k] = steps.get(k);
            }
            this.pos = pos;
            this.speed = speed;
            this.rails = rails;
            this.arrived = arrived;
            decode();
        }

        /** Create a route over the cells of a path (adjacent track cells) that passes the given number of rails. */
        public static Route of(List<BlockPos> path, int rails) {
            List<Byte> steps = new ArrayList<>();
            for (int k = 1; k < path.size(); k++) {
                BlockPos a = path.get(k - 1);
                BlockPos b = path.get(k);
                Direction d = Direction.getApproximateNearest(b.getX() - a.getX(), 0, b.getZ() - a.getZ());
                steps.add((byte) (d.get2DDataValue() * 3 + b.getY() - a.getY() + 1));
            }
            return new Route(path.get(0), steps, 0, 0, rails, false);
        }

        private void decode() {
            cells.clear();
            BlockPos p = start;
            cells.add(p);
            for (byte b : steps) {
                p = p.relative(Direction.from2DDataValue(b / 3)).above(b % 3 - 1);
                cells.add(p);
            }
        }

        private List<Byte> stepList() {
            List<Byte> out = new ArrayList<>(steps.length);
            for (byte b : steps) {
                out.add(b);
            }
            return out;
        }

        /** Return route cell i (0 is the sending station). */
        public BlockPos cell(int i) {
            return cells.get(i);
        }

        /** Return the index of the last cell (the destination station). */
        public int last() {
            return cells.size() - 1;
        }

        /** Return the route length in pu. */
        public long lengthPu() {
            return last() * 1000L;
        }

        /** Return the position along the route in pu. */
        public long pos() {
            return pos;
        }

        /** Return the speed of the last tick in pu per tick. */
        public int speed() {
            return speed;
        }

        /** Return the pu moved in the last tick. */
        public int moved() {
            return moved;
        }

        /** Return the number of mag rail cells the route passes. */
        public int rails() {
            return rails;
        }

        /** Return true once the sled reached the destination (it waits there while the station has no room). */
        public boolean arrived() {
            return arrived;
        }

        /** Return the index of the cell the sled is in or leaving. */
        public int cellIndex() {
            return (int) Math.min(last(), pos / 1000);
        }

        /** Return the fraction (0 to 1) of the way from the current cell to the next. */
        public double fraction() {
            return cellIndex() >= last() ? 0 : (pos % 1000) / 1000.0;
        }
    }

    /** Return the world position of a sled at pu p of the route: between the track points of two cells, 0.4 above. */
    public static Vec3 point(ServerLevel level, Route r, long p) {
        int i = (int) Math.min(r.last(), p / 1000);
        double f = i >= r.last() ? 0 : (p % 1000) / 1000.0;
        BlockPos a = r.cell(i);
        BlockPos b = r.cell(Math.min(r.last(), i + 1));
        double ya = a.getY() + MagRailLine.trackHeight(level.getBlockState(a));
        double yb = b.getY() + MagRailLine.trackHeight(level.getBlockState(b));
        return new Vec3(a.getX() + 0.5 + (b.getX() - a.getX()) * f, ya + (yb - ya) * f + FLOAT, a.getZ() + 0.5 + (b.getZ() - a.getZ()) * f);
    }

    /** Return the number of sleds of the level that fell in the last 24,000 ticks (sensor {@code transport.rail_breaks}). */
    public static int breaks(ServerLevel level) {
        List<Long> list = BREAKS.computeIfAbsent(level, k -> new ArrayList<>());
        long now = level.getGameTime();
        list.removeIf(t -> now - t > BREAK_WINDOW);
        return list.size();
    }

    private static boolean ready(ServerLevel level, BlockPos p) {
        return level.isLoaded(p) && level.areEntitiesLoaded(ChunkPos.pack(p));
    }

    /** Advance a sled one tick. */
    public static void tick(FallingCrateEntity e, ServerLevel sl, Route r) {
        if (r.arrived) {
            deliver(e, sl, r);
            return;
        }
        int ci = r.cellIndex();
        for (int k = ci; k <= Math.min(r.last(), ci + 2); k++) {
            if (!ready(sl, r.cell(k))) {
                r.speed = 0;
                r.moved = 0;
                e.setDeltaMovement(Vec3.ZERO);
                return;
            }
        }
        long total = r.lengthPu();
        int v = Math.min(MAX_SPEED, r.speed + ACCEL);
        long remaining = total - r.pos;
        if (remaining < BRAKE_PU) {
            v = (int) Math.min(v, BRAKE_MIN + (MAX_SPEED - BRAKE_MIN) * remaining / BRAKE_PU);
        }
        v = Math.min(v, spacingLimit(e, sl, r));
        long np = Math.min(total, r.pos + v);
        int from = (int) Math.min(r.last(), (r.pos + 500) / 1000);
        int to = (int) Math.min(r.last(), (np + 500) / 1000);
        if (!MagRailBlock.isTrack(sl.getBlockState(r.cell(from)))) {
            collapse(e, sl, r, r.pos, r.speed);
            return;
        }
        for (int k = from + 1; k <= to; k++) {
            if (!MagRailBlock.connected(sl, r.cell(k - 1), r.cell(k))) {
                collapse(e, sl, r, Math.max(r.pos, k * 1000L - 500), v);
                return;
            }
        }
        Vec3 old = e.position();
        Vec3 at = point(sl, r, np);
        hit(e, sl, r, old, at);
        e.setPos(at);
        e.setDeltaMovement(at.subtract(old));
        r.moved = (int) (np - r.pos);
        r.speed = r.moved;
        r.pos = np;
        if (r.moved > 0) {
            if (e.tickCount % 4 == 0) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y - 0.25, at.z, 3, 0.25, 0.02, 0.25, 0.02);
            }
            if (e.tickCount % 40 == 0) {
                sl.playSound(null, at.x, at.y, at.z, AutarkiaSounds.of("rail_hum"), SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
        if (np >= total) {
            r.arrived = true;
            deliver(e, sl, r);
        }
    }

    private static int spacingLimit(FallingCrateEntity e, ServerLevel sl, Route r) {
        int ci = r.cellIndex();
        MagRailLine line = MagRailLine.of(sl, r.cell(ci));
        if (line == null || ci >= r.last()) {
            return MAX_SPEED;
        }
        int li = line.indexOf(r.cell(ci));
        int ln = line.indexOf(r.cell(ci + 1));
        if (li < 0 || ln < 0) {
            return MAX_SPEED;
        }
        int dir = ln > li ? 1 : -1;
        double gap = line.gapAhead(sl, e, li + dir * r.fraction(), dir);
        if (gap == Double.MAX_VALUE) {
            return MAX_SPEED;
        }
        return (int) Math.max(0, Math.min(MAX_SPEED, Math.floor((gap - MagRailLine.SPACING) * 1000 + 1e-6)));
    }

    private static void hit(FallingCrateEntity e, ServerLevel sl, Route r, Vec3 old, Vec3 at) {
        Vec3 step = at.subtract(old);
        if (step.lengthSqr() < 1e-8) {
            return;
        }
        AABB box = e.getBoundingBox().expandTowards(step).inflate(0.1);
        Vec3 push = new Vec3(step.x, 0, step.z).normalize();
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box, le -> le.isAlive() && !le.isSpectator())) {
            if (r.hit.add(le.getId())) {
                le.hurtServer(sl, sl.damageSources().fallingBlock(e), HIT_DAMAGE);
                le.push(push.x * 0.8, 0.35, push.z * 0.8);
                le.needsSync = true;
            }
        }
    }

    private static void collapse(FallingCrateEntity e, ServerLevel sl, Route r, long atPu, int speed) {
        Vec3 at = point(sl, r, Math.min(atPu, r.lengthPu()));
        int i = (int) Math.min(r.last() - 1, Math.max(0, atPu / 1000));
        BlockPos a = r.cell(i);
        BlockPos b = r.cell(Math.min(r.last(), i + 1));
        Vec3 dir = new Vec3(b.getX() - a.getX(), 0, b.getZ() - a.getZ());
        Vec3 v = dir.lengthSqr() < 1e-8 ? Vec3.ZERO : dir.normalize().scale(speed / 1000.0);
        e.setPos(at);
        BREAKS.computeIfAbsent(sl, k -> new ArrayList<>()).add(sl.getGameTime());
        Autarkia.LOG.info("[mag_rail] sled fell at {} (missing track at cell {} of {}), {} pu/tick", BlockPos.containing(at).toShortString(),
            Math.min(r.last(), (atPu + 500) / 1000), r.last(), speed);
        e.toCollapse(v);
        MagRailLine.invalidateSleds(sl);
    }

    private static void deliver(FallingCrateEntity e, ServerLevel sl, Route r) {
        BlockPos dest = r.cell(r.last());
        if (!sl.isLoaded(dest)) {
            return;
        }
        if (!(sl.getBlockEntity(dest) instanceof MagRailStationBlockEntity station)) {
            collapse(e, sl, r, r.lengthPu(), 0);
            return;
        }
        if (!station.receive(e.crate())) {
            return;
        }
        Autarkia.LOG.info("[mag_rail] sled from {} delivered at {} over {} rails", r.cell(0).toShortString(), dest.toShortString(), r.rails);
        if (r.rails >= FLOAT_ON_RAILS) {
            r.linker.award(sl, Vec3.atCenterOf(dest), "logistics/float_on");
        }
        e.discard();
        MagRailLine.invalidateSleds(sl);
    }
}

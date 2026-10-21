package dev.autarkia.logistics;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Geometry of one cable car line (04 section 1.5). Two parallel cables 1.5 blocks apart run between the bull wheels of
 * the owner station A and the other station B; each sags as a parabola by 3% of the chord at mid span. The right cable
 * (looking from A to B) carries gondolas from A to B, the left one back. Gondolas turn around on a half circle at the
 * turn points 2.25 blocks in front of each wheel; the stop of a gondola is the middle of that half circle.
 *
 * <p>Loop positions are path units (1 block = 1,000 pu): {@code [0, cablePu)} right cable, then the B turn, the left
 * cable and the A turn. {@link #stopB()} and {@link #stopA()} are exactly half a loop apart.
 */
public final class CableGeometry {
    public static final double CABLE_HEIGHT = 2.9;
    public static final double HALF_GAP = 0.75;
    public static final double SAG_RATIO = 0.03;
    public static final double HANG = 2.5;
    public static final double TURN = 2.25;
    public static final double CABIN_HALF_ACROSS = 1.0;
    public static final double CABIN_HALF_ALONG = 0.625;
    public static final double CABIN_HEIGHT = 0.875;
    public final Vec3 a;
    public final Vec3 b;
    public final Vec3 dir;
    public final Vec3 right;
    public final double chord;
    public final double horizontal;
    public final double sag;
    private final double t0;
    private final double t1;
    private final int cablePu;
    private final int arcPu;
    private final int loopPu;
    private final int stopB;

    /** Build the geometry between the wheel anchor of A (the owner) and of B. */
    public CableGeometry(Vec3 a, Vec3 b) {
        this.a = a;
        this.b = b;
        Vec3 flat = new Vec3(b.x - a.x, 0, b.z - a.z);
        this.horizontal = flat.length();
        this.dir = horizontal < 1e-6 ? new Vec3(0, 0, 1) : flat.scale(1 / horizontal);
        this.right = new Vec3(-dir.z, 0, dir.x);
        this.chord = a.distanceTo(b);
        this.sag = SAG_RATIO * chord;
        this.t0 = Math.min(0.45, TURN / Math.max(1e-6, horizontal));
        this.t1 = 1 - t0;
        this.cablePu = (int) Math.round(chord * (t1 - t0) * 1000);
        this.arcPu = (int) Math.round(Math.PI * HALF_GAP * 1000);
        int half = cablePu + arcPu;
        this.loopPu = 2 * half;
        this.stopB = cablePu + arcPu / 2;
    }

    /** Return the anchor of a station: the footprint centre at bull wheel height, from its occupied block positions. */
    public static Vec3 anchor(Set<BlockPos> footprint) {
        int x0 = Integer.MAX_VALUE;
        int z0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int z1 = Integer.MIN_VALUE;
        for (BlockPos p : footprint) {
            x0 = Math.min(x0, p.getX());
            z0 = Math.min(z0, p.getZ());
            y0 = Math.min(y0, p.getY());
            x1 = Math.max(x1, p.getX());
            z1 = Math.max(z1, p.getZ());
        }
        return new Vec3((x0 + x1 + 1) / 2.0, y0 + CABLE_HEIGHT, (z0 + z1 + 1) / 2.0);
    }

    /** Return the loop length in pu. */
    public int loopPu() {
        return loopPu;
    }

    /** Return the stop position at station B in pu. */
    public int stopB() {
        return stopB;
    }

    /** Return the stop position at station A in pu (half a loop after the B stop). */
    public int stopA() {
        return stopB + loopPu / 2;
    }

    /**
     * Return a point on a cable at chord fraction t.
     *
     * @param side +1 for the right cable, -1 for the left one, 0 for the centre line between them
     */
    public Vec3 cable(double t, double side) {
        double drop = 4 * sag * t * (1 - t);
        return new Vec3(a.x + (b.x - a.x) * t + right.x * side * HALF_GAP, a.y + (b.y - a.y) * t - drop, a.z + (b.z - a.z) * t + right.z * side * HALF_GAP);
    }

    /** Return the cable hang point of a loop position (pu, wrapped into the loop). */
    public Vec3 hang(long pu) {
        int s = (int) Math.floorMod(pu, (long) loopPu);
        if (s < cablePu) {
            return cable(t0 + (t1 - t0) * s / (double) cablePu, 1);
        }
        s -= cablePu;
        if (s < arcPu) {
            double phi = Math.PI * s / arcPu;
            Vec3 c = cable(t1, 0);
            return c.add(right.scale(HALF_GAP * Math.cos(phi))).add(dir.scale(HALF_GAP * Math.sin(phi)));
        }
        s -= arcPu;
        if (s < cablePu) {
            return cable(t1 - (t1 - t0) * s / (double) cablePu, -1);
        }
        s -= cablePu;
        double phi = Math.PI * Math.min(s, arcPu) / arcPu;
        Vec3 c = cable(t0, 0);
        return c.subtract(right.scale(HALF_GAP * Math.cos(phi))).subtract(dir.scale(HALF_GAP * Math.sin(phi)));
    }

    /** Return the yaw in degrees that points an entity along the line from A to B (Minecraft yaw convention). */
    public float yaw() {
        return (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
    }

    /** Return the render bounds of the line: both stations, both cables with sag, and the gondola hang below them. */
    public AABB bounds() {
        double minY = Math.min(a.y, b.y) - sag - HANG - CABIN_HEIGHT;
        double maxY = Math.max(a.y, b.y) + 1;
        return new AABB(Math.min(a.x, b.x), minY, Math.min(a.z, b.z), Math.max(a.x, b.x), maxY, Math.max(a.z, b.z)).inflate(HALF_GAP + 1.5);
    }

    /** Return the render points of one cable from wheel to wheel, about one per block. */
    public Vec3[] points(double side) {
        int n = Math.max(8, (int) Math.ceil(chord));
        Vec3[] out = new Vec3[n + 1];
        for (int i = 0; i <= n; i++) {
            out[i] = cable(i / (double) n, side);
        }
        return out;
    }

    /** Return the chunks the line passes over (both cables, both stations). */
    public Set<ChunkPos> chunks() {
        Set<ChunkPos> out = new LinkedHashSet<>();
        int n = Math.max(2, (int) Math.ceil(chord / 4));
        for (int i = 0; i <= n; i++) {
            for (int side = -1; side <= 1; side += 2) {
                out.add(ChunkPos.containing(BlockPos.containing(cable(i / (double) n, side))));
            }
        }
        return out;
    }

    /**
     * Return the first blocked position of the line, or null when both cable paths and the gondola volume under them are
     * free. The server samples each cable every quarter block from A to B (right cable first) and tests the cable point
     * and the points 1, 2 and 2.5 blocks under it (the hanging gondola down to its floor). A block with a collision shape
     * blocks the line unless {@code ignore} accepts it (the two stations).
     */
    public @Nullable BlockPos firstBlocked(BlockGetter level, Predicate<BlockState> ignore) {
        int n = Math.max(4, (int) Math.ceil(horizontal * 4));
        for (int side = 1; side >= -1; side -= 2) {
            for (int i = 0; i <= n; i++) {
                Vec3 p = cable(i / (double) n, side);
                for (double dy : new double[] {0, 1, 2, HANG}) {
                    BlockPos bp = BlockPos.containing(p.x, p.y - dy, p.z);
                    BlockState st = level.getBlockState(bp);
                    if (!ignore.test(st) && !st.getCollisionShape(level, bp).isEmpty()) {
                        return bp;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return the box of a gondola cabin that hangs from a point, swung by the angles along and across the line (radians).
     * The box is axis aligned around the rotated cabin footprint (1.25 along, 2 across, 0.875 high) and shrunk by 0.05.
     */
    public AABB cabin(Vec3 hang, double along, double across) {
        double reach = HANG - CABIN_HEIGHT / 2;
        double sa = Math.sin(along);
        double sc = Math.sin(across);
        double down = reach * Math.sqrt(Math.max(0, 1 - sa * sa - sc * sc));
        Vec3 c = hang.add(dir.scale(reach * sa)).add(right.scale(reach * sc)).subtract(0, down, 0);
        double hx = Math.abs(dir.x) * CABIN_HALF_ALONG + Math.abs(right.x) * CABIN_HALF_ACROSS;
        double hz = Math.abs(dir.z) * CABIN_HALF_ALONG + Math.abs(right.z) * CABIN_HALF_ACROSS;
        return new AABB(c.x - hx, c.y - CABIN_HEIGHT / 2, c.z - hz, c.x + hx, c.y + CABIN_HEIGHT / 2, c.z + hz).deflate(0.05);
    }
}

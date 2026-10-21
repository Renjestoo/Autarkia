package dev.autarkia.logistics;

import java.util.function.Predicate;
import net.frozenblock.lib.wind.WindManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Ballistic arc of the crate launcher (04 section 1.6). The crate flies with gravity 0.05 blocks per tick squared, no
 * drag, and the wind acceleration {@code w x gondolaWindFactor x 0.025}. Steps are exact for constant acceleration:
 * {@code p += v + a / 2; v += a}, so tick samples lie on the analytic parabola. The speed for angle a over horizontal
 * distance D and height difference H is {@code v^2 = g D^2 / (2 cos^2(a) (D tan(a) - H))} (needs {@code D tan(a) > H},
 * at most 4 blocks per tick). With wind the launcher aims like this: solve the no-wind speed for the target, simulate the
 * flight with the wind at launch, shift the aim point by the predicted drift, and repeat 3 times. It picks the lowest of
 * 45, 55, 65 and 75 degrees whose sampled path (one sample per tick, crate box 0.875, at most 120 samples) is clear.
 */
public final class LaunchArc {
    public static final double GRAVITY = 0.05;
    public static final double MAX_SPEED = 4.0;
    public static final double WIND_SCALE = 0.025;
    public static final double BOX = 0.875;
    public static final int MAX_SAMPLES = 120;
    public static final int[] ANGLES = {45, 55, 65, 75};

    /** A chosen arc: launch angle in degrees, launch velocity in blocks per tick, flight ticks to the target plane. */
    public record Arc(int angle, Vec3 velocity, int ticks) {
    }

    /** Where a simulated flight crosses the target plane going down, after how many ticks. */
    public record Crossing(Vec3 point, int ticks) {
    }

    private LaunchArc() {
    }

    /** Return the no-wind launch velocity toward aim at the angle in degrees, or null when the angle cannot reach it. */
    public static @Nullable Vec3 velocity(Vec3 start, Vec3 aim, int angle) {
        double dx = aim.x - start.x;
        double dz = aim.z - start.z;
        double d = Math.sqrt(dx * dx + dz * dz);
        double h = aim.y - start.y;
        double a = Math.toRadians(angle);
        double lift = d * Math.tan(a) - h;
        if (d < 1e-6 || lift <= 0) {
            return null;
        }
        double cos = Math.cos(a);
        double v = Math.sqrt(GRAVITY * d * d / (2 * cos * cos * lift));
        if (v > MAX_SPEED) {
            return null;
        }
        return new Vec3(dx / d * v * cos, v * Math.sin(a), dz / d * v * cos);
    }

    /** Return the FrozenLib wind movement at a point, or zero while the wind manager is not usable (world seed unknown). */
    public static Vec3 wind(ServerLevel sl, Vec3 at) {
        WindManager wm = WindManager.getOrCreate(sl);
        return wm.usable() ? wm.getWindMovement(at) : Vec3.ZERO;
    }

    /** Return the acceleration per tick of a crate in the given FrozenLib wind, gravity included. */
    public static Vec3 acceleration(Vec3 wind, double windFactor) {
        Vec3 w = wind.scale(windFactor * WIND_SCALE);
        return new Vec3(w.x, w.y - GRAVITY, w.z);
    }

    /** Return the next step of a crate: the movement {@code v + a / 2} for the velocity v and the acceleration a. */
    public static Vec3 step(Vec3 v, Vec3 a) {
        return v.add(a.scale(0.5));
    }

    /** Simulate a flight to where it crosses the plane y going down; null when it does not within 600 ticks. */
    public static @Nullable Crossing simulate(Vec3 start, Vec3 v0, Vec3 accel, double planeY) {
        Vec3 p = start;
        Vec3 v = v0;
        for (int t = 1; t <= 600; t++) {
            Vec3 next = p.add(step(v, accel));
            v = v.add(accel);
            if (v.y < 0 && next.y <= planeY && p.y > planeY) {
                double f = (p.y - planeY) / (p.y - next.y);
                return new Crossing(p.add(next.subtract(p).scale(f)), t);
            }
            p = next;
        }
        return null;
    }

    /**
     * Return true when the crate box is clear of solid blocks at every tick sample from launch until it crosses the
     * target plane (at most {@link #MAX_SAMPLES}); the last sample is the crossing point itself, so the blocks under the
     * plane beside the net do not count. Blocks that ignore accepts (launcher, net) do not count either.
     */
    public static boolean clear(ServerLevel sl, Vec3 start, Vec3 v0, Vec3 accel, double planeY, Predicate<BlockState> ignore) {
        Vec3 p = start;
        Vec3 v = v0;
        for (int t = 1; t <= MAX_SAMPLES; t++) {
            Vec3 next = p.add(step(v, accel));
            v = v.add(accel);
            boolean crossing = v.y < 0 && next.y <= planeY && p.y > planeY;
            Vec3 at = crossing ? p.add(next.subtract(p).scale((p.y - planeY) / (p.y - next.y))) : next;
            if (GondolaEntity.blocked(sl, box(at), ignore) != null) {
                return false;
            }
            if (crossing) {
                return true;
            }
            p = next;
        }
        return false;
    }

    /** Return the box of a crate whose bottom centre is at p. */
    public static AABB box(Vec3 p) {
        return new AABB(p.x - BOX / 2, p.y, p.z - BOX / 2, p.x + BOX / 2, p.y + BOX, p.z + BOX / 2);
    }

    /**
     * Choose the arc to the target (the net centre on its catch plane) with the wind acceleration of the launch point,
     * or null when no angle has a clear path.
     */
    public static @Nullable Arc solve(ServerLevel sl, Vec3 start, Vec3 target, Vec3 accel, Predicate<BlockState> ignore) {
        for (int angle : ANGLES) {
            Vec3 aim = target;
            Vec3 v = velocity(start, aim, angle);
            for (int i = 0; i < 3 && v != null; i++) {
                Crossing c = simulate(start, v, accel, target.y);
                if (c == null) {
                    v = null;
                    break;
                }
                aim = aim.subtract(c.point().x - target.x, 0, c.point().z - target.z);
                v = velocity(start, aim, angle);
            }
            if (v == null) {
                continue;
            }
            Crossing c = simulate(start, v, accel, target.y);
            if (c != null && clear(sl, start, v, accel, target.y, ignore)) {
                return new Arc(angle, v, c.ticks());
            }
        }
        return null;
    }
}

package dev.autarkia.energy;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Catenary sag math of 02 section 4.1, shared by the client renderer and the server. */
public final class Catenary {
    private Catenary() {
    }

    /**
     * Return the sag depth d at the midpoint.
     *
     * @param h heat (metal tiers), wear (rope) or 0 (superconductor), 0..1
     * @param u load ratio
     */
    public static double sagDepth(CableTier tier, double length, double h, double u) {
        double d = tier.sagFactor * length * (1 + 1.5 * h) * (1 + 0.25 * Math.min(u, 2));
        return Math.min(d, 0.25 * length);
    }

    /** Return the catenary parameter a for horizontal distance dx and sag d (6 Newton steps). */
    public static double solveA(double dx, double d) {
        double a = dx * dx / (8 * d);
        for (int i = 0; i < 6; i++) {
            double k = dx / (2 * a);
            double f = a * (Math.cosh(k) - 1) - d;
            double fp = Math.cosh(k) - 1 - k * Math.sinh(k);
            if (Math.abs(fp) < 1e-12) {
                break;
            }
            double next = a - f / fp;
            a = next > 1e-6 ? next : a * 0.5;
        }
        return a;
    }

    /** Return the render points from a to b with clamp(ceil(2L), 8, 64) segments. */
    public static Vec3[] points(Vec3 a, Vec3 b, double sag) {
        double len = a.distanceTo(b);
        int segs = Mth.clamp((int) Math.ceil(2 * len), 8, 64);
        Vec3[] pts = new Vec3[segs + 1];
        double dxw = b.x - a.x;
        double dzw = b.z - a.z;
        double dh = Math.sqrt(dxw * dxw + dzw * dzw);
        boolean straight = dh < 0.5 || sag < 0.01;
        double ca = straight ? 0 : solveA(dh, sag);
        for (int i = 0; i <= segs; i++) {
            double t = i / (double) segs;
            double x = a.x + dxw * t;
            double z = a.z + dzw * t;
            double y = Mth.lerp(t, a.y, b.y);
            if (!straight) {
                double hx = t * dh;
                y = y - sag + ca * (Math.cosh((hx - dh / 2) / ca) - 1);
            }
            pts[i] = new Vec3(x, y, z);
        }
        return pts;
    }
}

package dev.autarkia.client.render;

import net.frozenblock.lib.wind.WindManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** Client-only HV span sway from FrozenLib wind (02 section 4.5): amplitude A = min(2, 0.04 L windStrength), period 40 + 2L. */
public final class HvSway {
    private HvSway() {
    }

    /** Return the signed lateral midpoint offset for the span this tick. */
    public static double amplitude(ClientLevel level, ClientSpans.Span s) {
        double len = s.a.distanceTo(s.b);
        Vec3 mid = s.a.add(s.b).scale(0.5);
        double amp = Math.min(2.0, 0.04 * len * dev.autarkia.energy.Wind.strength(level, mid));
        double period = 40 + 2 * len;
        double t = level.getGameTime() + s.id * 7;
        return 0.5 * amp + amp * Math.sin(2 * Math.PI * t / period);
    }

    /** Return the horizontal unit wind direction at the span midpoint. */
    public static Vec3 direction(ClientLevel level, ClientSpans.Span s) {
        WindManager wm = WindManager.getOrCreate(level);
        Vec3 w = wm.usable() ? wm.getWindMovement(s.a.add(s.b).scale(0.5)) : new Vec3(1, 0, 0);
        Vec3 h = new Vec3(w.x, 0, w.z);
        return h.lengthSqr() < 1e-8 ? new Vec3(1, 0, 0) : h.normalize();
    }
}

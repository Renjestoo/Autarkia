package dev.autarkia.client.render;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.energy.Catenary;
import dev.autarkia.network.LightningPulsePayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** Client part of the lightning pulse (02 section 5): a white glow runs along each pulsed span curve at the pulse speed. */
public final class ClientLightning {
    private record Run(int spanId, long start, boolean fromA, float speed) {
    }

    private static final List<Run> RUNS = new ArrayList<>();

    private ClientLightning() {
    }

    /** Add the spans of a pulse payload. */
    public static void handle(LightningPulsePayload p) {
        for (LightningPulsePayload.Entry e : p.spans()) {
            RUNS.add(new Run(e.spanId(), e.startTick(), e.fromA(), p.speed()));
        }
    }

    /** Spawn the glow of every running pulse span; called from the client tick. */
    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            RUNS.clear();
            return;
        }
        long now = level.getGameTime();
        RUNS.removeIf(r -> {
            ClientSpans.Span s = ClientSpans.get(r.spanId);
            if (s == null) {
                return true;
            }
            if (now < r.start) {
                return false;
            }
            Vec3[] pts = s.points != null ? s.points : Catenary.points(s.a, s.b, s.sag());
            double len = 0;
            double[] cum = new double[pts.length];
            for (int i = 1; i < pts.length; i++) {
                len += pts[i].distanceTo(pts[i - 1]);
                cum[i] = len;
            }
            double d = (now - r.start) * r.speed;
            if (d > len) {
                return true;
            }
            for (double back = 0; back <= 1.0; back += 0.5) {
                double at = Math.max(0, d - back);
                Vec3 p = along(pts, cum, r.fromA ? at : len - at);
                level.addParticle(GlowParticleOptions.of(0xFFFFFFFF).age(8), p.x, p.y, p.z, 0, 0, 0);
            }
            return false;
        });
    }

    private static Vec3 along(Vec3[] pts, double[] cum, double d) {
        for (int i = 1; i < pts.length; i++) {
            if (cum[i] >= d) {
                double seg = cum[i] - cum[i - 1];
                return pts[i - 1].lerp(pts[i], seg <= 1e-9 ? 0 : (d - cum[i - 1]) / seg);
            }
        }
        return pts[pts.length - 1];
    }
}

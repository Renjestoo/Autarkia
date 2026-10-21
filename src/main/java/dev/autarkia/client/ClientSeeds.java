package dev.autarkia.client;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.network.SeedUnpackPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;

/**
 * Client side of the seed unpack visuals (05 section 8.4, C17): for each {@code seed_unpack} payload, a glow ring that
 * grows from the footprint centre to the footprint edge in the first 40 ticks after the landing ("light runs over the
 * ground"), then from tick 40 to 140 the leveling glow: a light line sweeping the footprint along x with sparks over the
 * whole surface. Everything is drawn from the landing game time, so it needs no further packets.
 */
public final class ClientSeeds {
    private static final int RING = 0xFF7FFFF0;
    private static final int SWEEP = 0xFF9FE8FF;
    private static final int RING_TICKS = 40;
    private static final int END_TICKS = 140;
    private static final List<SeedUnpackPayload> ACTIVE = new ArrayList<>();

    private ClientSeeds() {
    }

    /** Start the visuals of a landed seed. */
    public static void start(SeedUnpackPayload p) {
        ACTIVE.add(p);
    }

    /** Forget every running visual (logout). */
    public static void clear() {
        ACTIVE.clear();
    }

    /** Spawn the ring and sweep particles of every running unpack (client tick). */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        RandomSource rnd = level.getRandom();
        ACTIVE.removeIf(p -> level.getGameTime() - p.start() >= END_TICKS);
        for (SeedUnpackPayload p : ACTIVE) {
            long age = Math.max(0, level.getGameTime() - p.start());
            double y = p.min().getY() + 0.15;
            double cx = (p.min().getX() + p.max().getX() + 1) / 2.0;
            double cz = (p.min().getZ() + p.max().getZ() + 1) / 2.0;
            double hx = (p.max().getX() - p.min().getX() + 1) / 2.0;
            double hz = (p.max().getZ() - p.min().getZ() + 1) / 2.0;
            if (age < RING_TICKS) {
                double f = (age + 1) / (double) RING_TICKS;
                ring(level, cx, y, cz, hx * f, hz * f);
            } else {
                double x = p.min().getX() + (age - RING_TICKS + 1) / (double) (END_TICKS - RING_TICKS) * hx * 2;
                GlowParticleOptions line = GlowParticleOptions.of(SWEEP).size(0.2f).age(12);
                for (double z = cz - hz; z <= cz + hz; z += 1) {
                    level.addParticle(line, x, y, z, 0, 0.02, 0);
                }
                GlowParticleOptions spark = GlowParticleOptions.of(RING).size(0.1f).age(16);
                for (int i = 0; i < 24; i++) {
                    level.addParticle(spark, cx + (rnd.nextDouble() * 2 - 1) * hx, y, cz + (rnd.nextDouble() * 2 - 1) * hz, 0, 0.04, 0);
                }
            }
        }
    }

    private static void ring(ClientLevel level, double cx, double y, double cz, double hx, double hz) {
        GlowParticleOptions glow = GlowParticleOptions.of(RING).size(0.18f).age(8);
        int nx = Math.max(1, (int) Math.ceil(hx * 2));
        int nz = Math.max(1, (int) Math.ceil(hz * 2));
        for (int i = 0; i <= nx; i++) {
            double x = cx - hx + 2 * hx * i / nx;
            level.addParticle(glow, x, y, cz - hz, 0, 0, 0);
            level.addParticle(glow, x, y, cz + hz, 0, 0, 0);
        }
        for (int i = 1; i < nz; i++) {
            double z = cz - hz + 2 * hz * i / nz;
            level.addParticle(glow, cx - hx, y, z, 0, 0, 0);
            level.addParticle(glow, cx + hx, y, z, 0, 0, 0);
        }
    }
}

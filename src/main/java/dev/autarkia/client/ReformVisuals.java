package dev.autarkia.client;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.network.ReformFxPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client visual of the Nanite Reform (07 section 3.2, 7.5): for 20 ticks a cloud of
 * {@code GlowParticleOptions.of(0x9FB4C8).size(0.15).age(20)} particles fades at the old position, a particle stream
 * runs to the new position, and in the last 8 ticks the particles gather into the body shape there.
 */
public final class ReformVisuals {
    private static final int COLOR = 0xFF9FB4C8;
    private static final int TICKS = 20;
    private static final List<Run> RUNS = new ArrayList<>();

    private record Run(Vec3 from, Vec3 to, int[] age) {
    }

    private ReformVisuals() {
    }

    /** Register the client tick driver. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    /** Start the visual of one Reform. */
    public static void handle(ReformFxPayload p) {
        RUNS.add(new Run(p.from(), p.to(), new int[] {0}));
    }

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            RUNS.clear();
            return;
        }
        RandomSource r = level.getRandom();
        GlowParticleOptions glow = GlowParticleOptions.of(COLOR).size(0.15f).age(20);
        RUNS.removeIf(run -> {
            int t = run.age()[0]++;
            int cloud = Math.max(2, 12 - t / 2);
            for (int i = 0; i < cloud; i++) {
                level.addParticle(glow, run.from().x + (r.nextDouble() - 0.5) * 0.8, run.from().y + r.nextDouble() * 1.8,
                    run.from().z + (r.nextDouble() - 0.5) * 0.8, 0, 0.01, 0);
            }
            Vec3 d = run.to().subtract(run.from());
            double reach = Math.min(1, (t + 1) / (double) (TICKS - 8));
            for (int i = 0; i < 6; i++) {
                double f = reach * r.nextDouble();
                Vec3 p = run.from().add(d.scale(f));
                level.addParticle(glow, p.x, p.y + 0.9 + Math.sin(f * Math.PI) * 0.8, p.z, 0, 0, 0);
            }
            if (t >= TICKS - 8) {
                for (int i = 0; i < 10; i++) {
                    level.addParticle(glow, run.to().x + (r.nextDouble() - 0.5) * 0.6, run.to().y + r.nextDouble() * 1.8,
                        run.to().z + (r.nextDouble() - 0.5) * 0.6, 0, 0, 0);
                }
            }
            return t + 1 >= TICKS;
        });
    }
}

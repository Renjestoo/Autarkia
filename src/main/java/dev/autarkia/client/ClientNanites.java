package dev.autarkia.client;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.config.AutarkiaClientConfig;
import dev.autarkia.network.NaniteBubblePayload;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.worker.NaniteSwarmEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client side of the nanites (06 sections 3.11 and 7.4, 05 section 8.3): every client tick each swarm within 96 blocks
 * spawns {@code clientNaniteParticles} silver glow particles inside its box (half beyond 32 blocks from the camera)
 * and gets one looping {@code entity.nanite_swarm.loop} while it is within 24 blocks; a bubble payload draws a ring of
 * 16 glow particles (0xA0E8FF) around each kept container.
 */
public final class ClientNanites {
    private static final int SWARM_COLOR = 0xFFC9D1D9;
    private static final int BUBBLE_COLOR = 0xFFA0E8FF;
    private static final double SOUND_RANGE = 24;
    private static final Map<Integer, SwarmLoop> LOOPS = new HashMap<>();

    private ClientNanites() {
    }

    /** Spawn the swarm particles and start or drop the loop sounds (client tick). */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Entity cam = mc.getCameraEntity();
        LOOPS.values().removeIf(l -> l.isStopped() || l.swarm.level() != level);
        if (level == null || cam == null || mc.isPaused()) {
            return;
        }
        Vec3 eye = cam.getEyePosition();
        int base = AutarkiaClientConfig.CLIENT_NANITE_PARTICLES.get();
        RandomSource r = level.getRandom();
        for (NaniteSwarmEntity s : level.getEntitiesOfClass(NaniteSwarmEntity.class, new AABB(eye, eye).inflate(96))) {
            double d = s.position().distanceTo(eye);
            int n = d > 32 ? base / 2 : base;
            AABB b = s.getBoundingBox();
            GlowParticleOptions glow = GlowParticleOptions.of(SWARM_COLOR).size(0.05f).age(6);
            for (int i = 0; i < n; i++) {
                level.addParticle(glow, b.minX + r.nextDouble() * b.getXsize(), b.minY + r.nextDouble() * b.getYsize(), b.minZ + r.nextDouble() * b.getZsize(),
                    0, 0, 0);
            }
            if (d <= SOUND_RANGE && !LOOPS.containsKey(s.getId())) {
                SwarmLoop loop = new SwarmLoop(s);
                LOOPS.put(s.getId(), loop);
                mc.getSoundManager().play(loop);
            }
        }
    }

    /** Draw one bubble ring around each position of the payload. */
    public static void bubble(NaniteBubblePayload p) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        GlowParticleOptions glow = GlowParticleOptions.of(BUBBLE_COLOR).size(0.12f).age(20);
        for (BlockPos pos : p.positions()) {
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI * 2 / 16;
                level.addParticle(glow, pos.getX() + 0.5 + Math.cos(a) * 0.9, pos.getY() + 0.5, pos.getZ() + 0.5 + Math.sin(a) * 0.9, 0, 0, 0);
            }
        }
    }

    /** Forget the loop sounds (logout). */
    public static void clear() {
        LOOPS.clear();
    }

    /** The loop of one swarm; it follows the swarm and stops when the swarm is removed or more than 24 blocks away. */
    private static final class SwarmLoop extends AbstractTickableSoundInstance {
        private final NaniteSwarmEntity swarm;

        SwarmLoop(NaniteSwarmEntity swarm) {
            super(AutarkiaSounds.of("entity.nanite_swarm.loop"), SoundSource.NEUTRAL, RandomSource.create());
            this.swarm = swarm;
            this.looping = true;
            this.delay = 0;
            follow();
        }

        private void follow() {
            x = swarm.getX();
            y = swarm.getY() + 1.5;
            z = swarm.getZ();
        }

        @Override
        public void tick() {
            Entity cam = Minecraft.getInstance().getCameraEntity();
            if (swarm.isRemoved() || cam == null || cam.distanceTo(swarm) > SOUND_RANGE) {
                stop();
                return;
            }
            follow();
        }
    }
}

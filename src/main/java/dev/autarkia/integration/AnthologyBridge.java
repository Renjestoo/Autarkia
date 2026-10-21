package dev.autarkia.integration;

import com.craisinlord.anthology.beam.BeamController;
import com.craisinlord.anthology.beam.BeamSettings;
import com.craisinlord.anthology.beam.BeamTerrainMode;
import com.craisinlord.anthology.shake.CameraShakeSync;
import dev.autarkia.equipment.SingularityLanceItem;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * The only entry point to anthology (1.0.0): camera shake for blooms, bursts, quenches, seeds and the forge, and the
 * {@code singularity_lance} beam ({@link BeamController}).
 */
public final class AnthologyBridge {
    private static final BeamSettings LANCE_SAFE = lance(false);
    private static final BeamSettings LANCE_DESTROY = lance(true);
    private AnthologyBridge() {
    }

    /**
     * Shake the camera of every player within the radius of pos.
     *
     * @param intensity 1.0 gives at most 2.5 degrees of yaw at the centre; fades linearly with distance and time
     * @param durationTicks shake length in ticks
     * @param radius blocks
     */
    public static void shake(ServerLevel level, Vec3 pos, float intensity, int durationTicks, float radius) {
        CameraShakeSync.sendNearby(level, pos, intensity, durationTicks, radius);
    }

    /** Return a new beam of the owner with the singularity lance settings (07 section 2.2; damage 0, decision C9). */
    public static Beam lanceBeam(LivingEntity owner) {
        return new Beam(new BeamController(owner));
    }

    private static BeamSettings lance(boolean destroy) {
        return new BeamSettings(SingularityLanceItem.RANGE, 0, 1.0, SingularityLanceItem.BEAM_TICKS, SingularityLanceItem.TRAVEL_TICKS,
            SingularityLanceItem.COOLDOWN, SingularityLanceItem.PATH_RADIUS, SingularityLanceItem.IMPACT_RADIUS, 0, SingularityLanceItem.KNOCKBACK, 1,
            SingularityLanceItem.RANGE, 0, SingularityLanceItem.IMPACT_TERRAIN_RADIUS, SingularityLanceItem.MAX_TERRAIN_MUTATIONS, 1.0F, 0,
            SingularityLanceItem.MAX_TERRAIN_RESISTANCE, destroy, true);
    }

    /** One anthology beam of one owner: start fixes the aim point, each tick moves it with the owner's eye and look. */
    public static final class Beam {
        private final BeamController controller;

        private Beam(BeamController controller) {
            this.controller = controller;
        }

        /** Start the beam aimed at aim for the given number of ticks. */
        public void start(Vec3 eye, Vec3 aim, int ticks) {
            controller.start(eye, aim, ticks);
        }

        /** Return true while the beam runs. */
        public boolean firing() {
            return controller.isFiring();
        }

        /** Stop the beam now. */
        public void stop() {
            controller.stop();
        }

        /**
         * Run one beam tick from eye along look; destroy selects the terrain mode DESTROY (else NONE). The callback gets the
         * beam end of the tick, then null once the beam has ended.
         */
        public void tick(Vec3 eye, Vec3 look, boolean destroy, Consumer<@Nullable Vec3> end) {
            controller.tick(eye, look, null, destroy ? LANCE_DESTROY : LANCE_SAFE, destroy ? BeamTerrainMode.DESTROY : BeamTerrainMode.NONE, end::accept);
        }
    }
}

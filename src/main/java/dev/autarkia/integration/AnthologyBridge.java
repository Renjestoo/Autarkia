package dev.autarkia.integration;

import com.craisinlord.anthology.shake.CameraShakeSync;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** The only entry point to anthology (1.0.0): camera shake for blooms, bursts, quenches, seeds and the forge. */
public final class AnthologyBridge {
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
}

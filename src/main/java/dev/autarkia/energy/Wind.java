package dev.autarkia.energy;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import net.frozenblock.lib.wind.WindManager;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Wind strength from FrozenLib (02 section 4.5): clamp(|w_xz| / windReference, 0, 1.5). Before the world seed is known
 * the wind manager is not usable: the strength is then 0.5 and one warning is logged per session.
 */
public final class Wind {
    private static boolean warned;

    private Wind() {
    }

    /** Return true when FrozenLib wind data is available for the level. */
    public static boolean available(Level level) {
        return WindManager.getOrCreate(level).usable();
    }

    /** Return the wind strength at a point, 0..1.5. */
    public static double strength(Level level, Vec3 p) {
        WindManager wm = WindManager.getOrCreate(level);
        if (!wm.usable()) {
            if (!warned && !level.isClientSide()) {
                warned = true;
                Autarkia.LOG.warn("FrozenLib wind is not usable yet (world seed unknown); wind generators use strength 0.5");
            }
            return 0.5;
        }
        Vec3 w = wm.getWindMovement(p);
        return Mth.clamp(Math.sqrt(w.x * w.x + w.z * w.z) / AutarkiaConfig.WIND_REFERENCE.get(), 0, 1.5);
    }
}

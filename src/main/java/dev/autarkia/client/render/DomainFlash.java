package dev.autarkia.client.render;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.network.DomainFlashPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

/** Client part of {@code domain_flash} (W23): every listed industrial lamp flashes with a bright glow for 10 ticks. */
public final class DomainFlash {
    private record Flash(List<BlockPos> lamps, int[] left) {
    }

    private static final List<Flash> FLASHES = new ArrayList<>();

    private DomainFlash() {
    }

    /** Start the flash of a payload. */
    public static void handle(DomainFlashPayload p) {
        FLASHES.add(new Flash(List.copyOf(p.lamps()), new int[] {DomainFlashPayload.FLASH_TICKS}));
    }

    /** Spawn the lamp glows of every running flash; called from the client tick. */
    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            FLASHES.clear();
            return;
        }
        FLASHES.removeIf(f -> {
            float t = f.left[0] / (float) DomainFlashPayload.FLASH_TICKS;
            for (BlockPos p : f.lamps) {
                level.addParticle(GlowParticleOptions.of(0xFFFFF5D0).size(0.6F + 0.6F * t).age(3), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 0, 0, 0);
            }
            return --f.left[0] <= 0;
        });
    }
}

package dev.autarkia.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/** Client configuration (autarkia-client.toml): rendering, HUD and effect settings. */
public final class AutarkiaClientConfig {
    public static final ModConfigSpec SPEC;
    public static final IntValue SPAN_RENDER_DISTANCE;
    public static final BooleanValue CABLE_PARTICLES;
    public static final IntValue CLIENT_NANITE_PARTICLES;
    public static final BooleanValue SHOW_LOAD_TOOLTIP;
    public static final BooleanValue HUD_ENABLED;
    public static final DoubleValue HUD_SCALE;
    public static final DoubleValue FX_SHAKE_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("render");
        SPAN_RENDER_DISTANCE = b.defineInRange("spanRenderDistance", 96, 16, 256);
        CABLE_PARTICLES = b.define("cableParticles", true);
        CLIENT_NANITE_PARTICLES = b.defineInRange("clientNaniteParticles", 60, 0, 400);
        SHOW_LOAD_TOOLTIP = b.define("showLoadTooltip", true);
        b.pop();
        b.push("hud");
        HUD_ENABLED = b.define("enabled", true);
        HUD_SCALE = b.defineInRange("scale", 1.0, 0.5, 2.0);
        b.pop();
        b.push("fx");
        FX_SHAKE_SCALE = b.defineInRange("shakeScale", 1.0, 0.0, 2.0);
        b.pop();
        SPEC = b.build();
    }

    private AutarkiaClientConfig() {
    }
}

package dev.autarkia.client.hud;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AegisStatusPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The Aegis part of the equipment HUD (07 section 2.6): the latest {@code aegis_status} of the local player, drawn as
 * the tether icon (green: linked with a grid of at least 1,000,000 FE; amber: linked with less; red: no tether) and
 * the reform status ("Reform ready" or the cooldown seconds, grey while the domain holds less than
 * {@code reform.matterCost} matter) right of the hotbar above the suit pips. A {@code NONE} state clears it.
 */
public final class AegisHud {
    private static final int GREEN = 0xFF55DD55;
    private static final int AMBER = 0xFFF2C230;
    private static final int RED = 0xFFDD4444;
    private static final int GREY = 0xFF8A8A8A;
    private static @Nullable AegisStatusPayload status;

    private AegisHud() {
    }

    /** Keep the latest status (client thread); a NONE state hides the Aegis elements. */
    public static void handle(AegisStatusPayload p) {
        status = p.state() == AegisStatusPayload.STATE_NONE ? null : p;
    }

    /** Forget the status (logout). */
    public static void clear() {
        status = null;
    }

    /** Draw the tether icon and the reform status with their left edge at x, the icon's top at y. */
    static void render(GuiGraphicsExtractor g, Minecraft mc, int x, int y) {
        AegisStatusPayload s = status;
        if (s == null || mc.level == null) {
            return;
        }
        int color = switch (s.state()) {
            case AegisStatusPayload.STATE_GREEN -> GREEN;
            case AegisStatusPayload.STATE_AMBER -> AMBER;
            default -> RED;
        };
        icon(g, x, y, color);
        g.text(mc.font, Component.translatable("hud.autarkia.tether"), x + 11, y + 1, color, true);
        long left = s.reformReadyAt() - mc.level.getGameTime();
        boolean matter = s.matter() >= AutarkiaConfig.REFORM_MATTER_COST.get();
        Component reform = left <= 0 ? Component.translatable("hud.autarkia.reform.ready")
            : Component.translatable("hud.autarkia.reform.cooldown", (left + 19) / 20);
        g.text(mc.font, reform, x, y + 11, !matter ? GREY : left <= 0 ? GREEN : AMBER, true);
    }

    /** Draw a 9 x 9 link icon: two interlocked rings in the tether colour. */
    private static void icon(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x - 1, y - 1, x + 10, y + 10, 0xA0101418);
        ring(g, x, y + 2, color);
        ring(g, x + 4, y + 2, color);
    }

    private static void ring(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x + 1, y, x + 4, y + 1, color);
        g.fill(x + 1, y + 4, x + 4, y + 5, color);
        g.fill(x, y + 1, x + 1, y + 4, color);
        g.fill(x + 4, y + 1, x + 5, y + 4, color);
    }
}

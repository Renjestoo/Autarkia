package dev.autarkia.client.hud;

import dev.autarkia.network.WorkerDebugPayload;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** HUD overlay for {@code /autarkia worker info}: the lines of the last {@code worker_debug} payload for 200 client ticks. */
public final class WorkerDebugHud {
    private static List<String> lines = List.of();
    private static long until;

    private WorkerDebugHud() {
    }

    /** Show the payload lines. */
    public static void show(WorkerDebugPayload p) {
        Minecraft mc = Minecraft.getInstance();
        lines = p.lines();
        until = mc.level == null ? 0 : mc.level.getGameTime() + 200;
    }

    /** Draw the layer. */
    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.getGameTime() > until || lines.isEmpty() || mc.gui.hud.isHidden()) {
            return;
        }
        int w = 0;
        for (String l : lines) {
            w = Math.max(w, mc.font.width(l));
        }
        g.fill(4, 4, 12 + w, 8 + lines.size() * 10, 0xC0101418);
        for (int i = 0; i < lines.size(); i++) {
            g.text(mc.font, Component.literal(lines.get(i)), 8, 8 + i * 10, 0xFFE0E0E0, false);
        }
    }
}

package dev.autarkia.client.hud;

import dev.autarkia.config.AutarkiaClientConfig;
import dev.autarkia.equipment.SlingItem;
import dev.autarkia.equipment.ToolMode;
import dev.autarkia.registry.AutarkiaComponents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Equipment HUD layer {@code autarkia:equipment_hud} (07 section 2.6), drawn above the hotbar: the sling charge arc
 * around the crosshair (grey below 5 ticks, green from 5) and the mode label of an item with modes.
 */
public final class EquipmentHud {
    private EquipmentHud() {
    }

    /** Draw the layer. */
    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || !AutarkiaClientConfig.HUD_ENABLED.get() || mc.gui.hud.isHidden()) {
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        if (p.isUsingItem() && p.getUseItem().getItem() instanceof SlingItem) {
            int charge = p.getTicksUsingItem();
            float frac = Math.min(1f, charge / (float) SlingItem.FULL_CHARGE);
            int color = charge >= SlingItem.MIN_CHARGE ? 0xFF55DD55 : 0xFF9A9A9A;
            arc(g, w / 2, h / 2, (int) Math.round(10 * AutarkiaClientConfig.HUD_SCALE.get()), frac, color);
        }
        ItemStack held = p.getMainHandItem();
        ToolMode mode = held.get(AutarkiaComponents.TOOL_MODE.get());
        if (mode != null) {
            Component label = Component.translatable(mode.key());
            g.text(mc.font, label, w / 2 - 91 - mc.font.width(label) - 6, h - 16, 0xFFF2D3A0, true);
        }
    }

    private static void arc(GuiGraphicsExtractor g, int cx, int cy, int r, float frac, int color) {
        int steps = 40;
        for (int i = 0; i < steps; i++) {
            float t = i / (float) steps;
            double a = -Math.PI / 2 + t * 2 * Math.PI;
            int x = cx + (int) Math.round(Math.cos(a) * r);
            int y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, t <= frac ? color : 0x60000000);
        }
    }
}

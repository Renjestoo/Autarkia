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
 * around the crosshair (grey below 5 ticks, green from 5), the steam repeater pressure gauge right of the hotbar, the
 * mode label of an item with modes, the FE bar of a held FE item left of the hotbar and the suit pips at the right, and
 * the rail rifle charge arc (grey below 10 ticks, teal from 10, full at 30).
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
        if (p.isUsingItem() && p.getUseItem().getItem() instanceof dev.autarkia.equipment.RailRifleItem) {
            int charge = p.getTicksUsingItem();
            float frac = Math.min(1f, charge / (float) dev.autarkia.equipment.RailRifleItem.FULL_CHARGE);
            int color = charge >= dev.autarkia.equipment.RailRifleItem.MIN_CHARGE ? 0xFF5FB8B0 : 0xFF9A9A9A;
            arc(g, w / 2, h / 2, (int) Math.round(10 * AutarkiaClientConfig.HUD_SCALE.get()), frac, color);
        }
        ItemStack held = p.getMainHandItem();
        if (held.getItem() instanceof dev.autarkia.equipment.SteamRepeaterItem) {
            gauge(g, w / 2 + 91 + 22, h - 8, dev.autarkia.equipment.SteamRepeaterItem.pressure(held));
        }
        ToolMode mode = held.get(AutarkiaComponents.TOOL_MODE.get());
        if (mode != null) {
            Component label = Component.translatable(mode.key());
            g.text(mc.font, label, w / 2 - 91 - mc.font.width(label) - 6, h - 16, 0xFFF2D3A0, true);
        }
        if (held.getItem() instanceof dev.autarkia.equipment.PoweredItem pi) {
            feBar(g, mc, w / 2 - 91 - 87, h - 8, dev.autarkia.equipment.PoweredItem.energy(held), pi.capacity());
        }
        pips(g, w / 2 + 91 + 6, h - 20, p);
    }

    /** Draw the FE bar of the held FE item (81 x 5) with its value as "123k FE" above it. */
    private static void feBar(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int fe, int cap) {
        int fill = cap <= 0 ? 0 : Math.round(81f * fe / cap);
        g.fill(x - 1, y - 1, x + 82, y + 6, 0xA0101418);
        g.fill(x, y, x + fill, y + 5, 0xFF000000 | dev.autarkia.equipment.PoweredItem.BAR_COLOR);
        String text = fe >= 1000 ? fe / 1000 + "k FE" : fe + " FE";
        g.text(mc.font, Component.literal(text), x, y - 10, 0xFF9BE7FF, true);
    }

    /** Draw one pip per worn suit piece (head to feet): green above 50% FE, amber above 10%, red below, grey when off. */
    private static void pips(GuiGraphicsExtractor g, int x, int y, Player p) {
        net.minecraft.world.entity.EquipmentSlot[] slots = {net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            ItemStack s = p.getItemBySlot(slots[i]);
            if (!(s.getItem() instanceof dev.autarkia.equipment.SuitItem suit)) {
                continue;
            }
            float r = dev.autarkia.equipment.PoweredItem.energy(s) / (float) suit.capacity();
            boolean on = s.getOrDefault(AutarkiaComponents.SUIT_ENABLED.get(), true);
            int color = !on ? 0xFF6A6A6A : r > 0.5f ? 0xFF55DD55 : r > 0.1f ? 0xFFF2C230 : 0xFFDD4444;
            g.fill(x, y + i * 5, x + 4, y + i * 5 + 4, color);
        }
    }

    /** Draw the pressure gauge: a half dial with one tick per shot (10 pressure) and a needle at the pressure. */
    private static void gauge(GuiGraphicsExtractor g, int cx, int cy, int pressure) {
        int r = 14;
        g.fill(cx - r - 2, cy - r - 2, cx + r + 3, cy + 2, 0xA0101418);
        for (int i = 0; i <= 10; i++) {
            double a = Math.PI + i / 10.0 * Math.PI;
            int x = cx + (int) Math.round(Math.cos(a) * r);
            int y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, i * 10 <= pressure ? 0xFFF2D3A0 : 0xFF5A5A5A);
        }
        double a = Math.PI + pressure / 100.0 * Math.PI;
        for (int k = 0; k < r - 2; k++) {
            int x = cx + (int) Math.round(Math.cos(a) * k);
            int y = cy + (int) Math.round(Math.sin(a) * k);
            g.fill(x, y, x + 1, y + 1, 0xFFFF6A3A);
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

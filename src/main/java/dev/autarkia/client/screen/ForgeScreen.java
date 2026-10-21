package dev.autarkia.client.screen;

import dev.autarkia.equipment.ForgeOfAscensionBlockEntity;
import dev.autarkia.registry.SlotMenu;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the {@code forge_of_ascension} menu (07 section 3.5, decision G15): the 27 input slots, then in the text well
 * the forge state, the fitting step with its progress, the FE drawn this tick and the FE on the forge's grid, and the
 * start hint (sneak + use the pad with empty hands).
 */
public class ForgeScreen extends SlotScreen {
    public ForgeScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        ForgeOfAscensionBlockEntity.Status[] all = ForgeOfAscensionBlockEntity.Status.values();
        ForgeOfAscensionBlockEntity.Status st = all[Math.max(0, Math.min(all.length - 1, menu.value(4)))];
        int x = 11;
        int y = 79;
        g.text(font, Component.translatable("jade.autarkia.forge." + st.name().toLowerCase(Locale.ROOT)), x, y, 0xFFE8D27A, false);
        if (st == ForgeOfAscensionBlockEntity.Status.FITTING) {
            Component name = Component.translatable(ForgeOfAscensionBlockEntity.phaseKey(menu.value(0)));
            g.text(font, Component.translatable("jade.autarkia.forge.progress", name, menu.value(0) * 100 / Math.max(1, menu.value(1))), x, y + 10,
                0xFFFFFFFF, false);
            g.text(font, Component.translatable("jade.autarkia.forge.fe", menu.value(3)), x, y + 20, 0xFF7FFFF0, false);
        } else {
            g.text(font, Component.translatable("jade.autarkia.forge.fitted", menu.value(2)), x, y + 10, 0xFFFFFFFF, false);
            g.text(font, Component.translatable("screen.autarkia.forge.hint"), x, y + 20, 0xFFBFBFBF, false);
        }
        g.text(font, Component.translatable("screen.autarkia.forge.grid", menu.value(5)), x, y + 30, 0xFFBFBFBF, false);
    }
}

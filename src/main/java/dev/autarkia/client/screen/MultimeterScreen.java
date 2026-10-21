package dev.autarkia.client.screen;

import dev.autarkia.network.MultimeterReadingPayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Client-only multimeter reading (02 section 9.1): a dark panel sized to its text with the node position, service
 * ratio bar, a 2 x 2 grid of generation, demand, loss and stored, rho, and one block per span of the node with load,
 * wear and heat bars.
 */
public class MultimeterScreen extends Screen {
    private static final int PAD = 8;
    private static final int LINE = 11;
    private final MultimeterReadingPayload r;

    public MultimeterScreen(MultimeterReadingPayload reading) {
        super(Component.translatable("item.autarkia.multimeter"));
        this.r = reading;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String fe(float v) {
        return String.format(Locale.ROOT, "%.1f FE/t", v);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int w = 236;
        int spanRows = r.lines().size();
        int maxRows = Math.max(0, (height - 120) / (LINE * 3 + 4));
        int shown = Math.min(spanRows, maxRows);
        int h = PAD * 2 + LINE * (r.connected() ? 6 : 2) + shown * (LINE * 3 + 4) + (spanRows > shown ? LINE : 0);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF8C5A2B);
        g.fill(x, y, x + w, y + h, 0xF0101418);
        int ty = y + PAD;
        g.text(font, Component.translatable("multimeter.autarkia.node", r.node().getX(), r.node().getY(), r.node().getZ()), x + PAD, ty, 0xFFF2D3A0);
        ty += LINE;
        if (!r.connected()) {
            g.text(font, Component.translatable("multimeter.autarkia.no_network"), x + PAD, ty, 0xFFAAAAAA);
        } else {
            g.text(font, Component.translatable("multimeter.autarkia.network", r.networkId(), Component.translatable("tier.autarkia." + r.tier().getSerializedName()),
                r.nodes(), r.spans()), x + PAD, ty, 0xFFDDDDDD);
            ty += LINE;
            g.text(font, Component.translatable("multimeter.autarkia.service", Math.round(r.phi() * 100)), x + PAD, ty, 0xFFDDDDDD);
            bar(g, x + 90, ty + 1, w - 90 - PAD, 7, r.phi(), r.phi() >= 0.99f ? 0xFF55CC55 : r.phi() >= 0.5f ? 0xFFDDCC33 : 0xFFDD4444);
            ty += LINE;
            int col = (w - PAD * 2) / 2;
            g.text(font, Component.translatable("multimeter.autarkia.generation", fe(r.generation())), x + PAD, ty, 0xFF9BE7FF);
            g.text(font, Component.translatable("multimeter.autarkia.demand", fe(r.demand())), x + PAD + col, ty, 0xFFFFC27A);
            ty += LINE;
            g.text(font, Component.translatable("multimeter.autarkia.loss", fe(r.loss())), x + PAD, ty, 0xFFFF8C8C);
            g.text(font, Component.translatable("multimeter.autarkia.stored", r.stored(), r.capacity()), x + PAD + col, ty, 0xFFB8E8D6);
            ty += LINE;
            g.text(font, Component.translatable(r.lagging() ? "multimeter.autarkia.rho_lagging" : "multimeter.autarkia.rho",
                String.format(Locale.ROOT, "%.4f", r.rho())), x + PAD, ty, 0xFF888888);
        }
        ty += LINE;
        for (int i = 0; i < shown; i++) {
            MultimeterReadingPayload.Line l = r.lines().get(i);
            Component title = l.tree()
                ? Component.translatable("multimeter.autarkia.span_flow", l.id(), String.format(Locale.ROOT, "%.1f", l.length()), fe(l.flow()))
                : Component.translatable("multimeter.autarkia.span_idle", l.id(), String.format(Locale.ROOT, "%.1f", l.length()));
            g.text(font, title, x + PAD, ty, l.slip() ? 0xFFFF7755 : 0xFFE0E0E0);
            ty += LINE;
            g.text(font, Component.translatable("multimeter.autarkia.load"), x + PAD + 4, ty, 0xFFAAAAAA);
            bar(g, x + 50, ty + 1, 70, 7, Math.min(1, l.load() / 2f), color(l.load() / 2f));
            g.text(font, Component.translatable("multimeter.autarkia.wear"), x + 126, ty, 0xFFAAAAAA);
            bar(g, x + 160, ty + 1, 68, 7, l.wear(), color(l.wear()));
            ty += LINE;
            if (l.heat() > 0 || l.temp() > 0) {
                g.text(font, Component.translatable("multimeter.autarkia.heat"), x + PAD + 4, ty, 0xFFAAAAAA);
                bar(g, x + 50, ty + 1, 70, 7, l.heat(), color(l.heat()));
            }
            ty += LINE + 4;
        }
        if (spanRows > shown) {
            g.text(font, Component.translatable("multimeter.autarkia.more", spanRows - shown), x + PAD, ty, 0xFF888888);
        }
    }

    private static int color(float v) {
        return v < 0.5f ? 0xFF55CC55 : v < 0.8f ? 0xFFDDCC33 : 0xFFDD4444;
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float v, int color) {
        g.fill(x, y, x + w, y + h, 0xFF2A2A2A);
        g.fill(x, y, x + Math.round(w * Math.max(0, Math.min(1, v))), y + h, color);
    }
}

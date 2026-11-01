package dev.autarkia.client.screen;

import dev.autarkia.client.ClientColony;
import dev.autarkia.colony.ColonyMenu;
import dev.autarkia.colony.ColonySync;
import dev.autarkia.network.ColonyActionPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The colony screen (05b section 9.1): tabs Overview, Projects, Jobs, Workers and Settings at the Core, the Projects
 * tab alone at a project board, Projects plus the autonomy switch on the planning tablet. Everything shown comes from
 * {@code colony_sync}; every button sends one {@code colony_action}.
 */
public class ColonyScreen extends AbstractContainerScreen<ColonyMenu> {
    private static final Identifier PANEL = dev.autarkia.Autarkia.id("colony_panel");
    private static final int W = 330;
    private static final int H = 220;
    private static final String[] TABS = {"overview", "projects", "jobs", "workers", "settings", "patterns"};
    private static final int TEMPLATES_PER_PAGE = 10;
    private static final int GRAPH_SAMPLES = 24;
    private static final int LOG_LINES = 7;
    private int tab;
    private int selected;
    private int templatePage;
    private int trustIndex;
    private int actionTop = H;
    private int seen = -1;
    private EditBox nameBox;
    private EditBox trustBox;
    private EditBox stockItem;
    private EditBox stockCount;

    public ColonyScreen(ColonyMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, W, H);
        tab = menu.mode == ColonyMenu.Mode.CORE ? ColonySync.OVERVIEW : ColonySync.PROJECTS;
    }

    private void act(String action, String... args) {
        ClientPacketDistributor.sendToServer(new ColonyActionPayload(menu.colonyId, action, List.of(args)));
    }

    private CompoundTag data() {
        return ClientColony.tab(tab);
    }

    private boolean manage() {
        return data().getBooleanOr("manage", false);
    }

    @Override
    protected void init() {
        super.init();
        seen = ClientColony.version();
        int x = leftPos;
        int y = topPos;
        if (menu.mode == ColonyMenu.Mode.CORE) {
            for (int i = 0; i < TABS.length; i++) {
                final int t = i;
                Button b = Button.builder(Component.translatable("screen.autarkia.colony.tab." + TABS[i]), p -> {
                    tab = t;
                    selected = 0;
                    act("tab", Integer.toString(t));
                    rebuildWidgets();
                }).bounds(x + 4 + i * 54, y + 4, 52, 16).build();
                b.active = tab != i;
                addRenderableWidget(b);
            }
        }
        switch (tab) {
            case ColonySync.OVERVIEW -> overviewWidgets(x, y);
            case ColonySync.PROJECTS -> projectWidgets(x, y);
            case ColonySync.JOBS -> rowButtons(x, y, "jobs", "job_cancel", "screen.autarkia.colony.cancel");
            case ColonySync.WORKERS -> rowButtons(x, y, "workers", "recall", "screen.autarkia.colony.recall");
            case ColonySync.PATTERNS -> patternWidgets(x, y);
            default -> settingsWidgets(x, y);
        }
        if (menu.mode == ColonyMenu.Mode.TABLET) {
            autonomyButtons(x + 4, y + H - 20);
        }
    }

    private void overviewWidgets(int x, int y) {
        if (data().getBooleanOr("owner", false)) {
            nameBox = new EditBox(font, x + 8, y + H - 22, 160, 16, Component.translatable("screen.autarkia.colony.name"));
            nameBox.setMaxLength(32);
            nameBox.setValue(data().getStringOr("name", ""));
            addRenderableWidget(nameBox);
            addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.colony.rename"), p -> act("rename", nameBox.getValue()))
                .bounds(x + 172, y + H - 22, 70, 16).build());
        }
    }

    private List<CompoundTag> list(String key) {
        ListTag l = data().getListOrEmpty(key);
        List<CompoundTag> out = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) {
            out.add(l.getCompoundOrEmpty(i));
        }
        return out;
    }

    private void projectWidgets(int x, int y) {
        List<CompoundTag> projects = list("projects");
        int top = y + (menu.mode == ColonyMenu.Mode.CORE ? 24 : 6);
        for (int i = 0; i < Math.min(9, projects.size()); i++) {
            final int k = i;
            CompoundTag p = projects.get(i);
            Button b = Button.builder(Component.translatable("colony_template.autarkia." + p.getStringOr("template", "")), btn -> {
                selected = k;
                rebuildWidgets();
            }).bounds(x + 4, top + i * 18, 104, 16).build();
            b.active = i != selected;
            addRenderableWidget(b);
        }
        actionTop = H;
        if (projects.isEmpty() || selected >= projects.size()) {
            return;
        }
        CompoundTag p = projects.get(selected);
        String id = p.getStringOr("id", "");
        String state = p.getStringOr("state", "");
        int bx = x + 112;
        int by = y + H - (menu.mode == ColonyMenu.Mode.TABLET && manage() ? 42 : 22);
        List<Button> row = new ArrayList<>();
        row.add(Button.builder(Component.translatable("screen.autarkia.colony.show"), b -> ClientColony.show(java.util.UUID.fromString(id))).build());
        if (manage()) {
            if (state.equals("proposed")) {
                row.add(Button.builder(Component.translatable("screen.autarkia.colony.approve"), b -> act("approve", id)).build());
                row.add(Button.builder(Component.translatable("screen.autarkia.colony.deny"), b -> act("deny", id)).build());
                row.add(Button.builder(Component.translatable("screen.autarkia.colony.rotate"), b -> act("rotate", id)).build());
            }
            if (state.equals("blocked")) {
                row.add(Button.builder(Component.translatable("screen.autarkia.colony.retry"), b -> act("retry", id)).build());
            }
            if (!state.equals("proposed") && !state.equals("done")) {
                row.add(Button.builder(Component.translatable("screen.autarkia.colony.cancel"), b -> act("cancel", id)).build());
            }
            row.add(Button.builder(Component.literal("+10"), b -> act("priority", id, "10")).build());
            row.add(Button.builder(Component.literal("-10"), b -> act("priority", id, "-10")).build());
        }
        int right = x + W - 8;
        int cx = bx;
        int cy = by;
        for (Button b : row) {
            int bw = font.width(b.getMessage()) + 6;
            if (cx + bw > right) {
                cx = bx;
                cy -= 18;
            }
            b.setRectangle(bw, 16, cx, cy);
            addRenderableWidget(b);
            cx += bw + 2;
        }
        actionTop = cy - y;
    }

    private void rowButtons(int x, int y, String key, String action, String label) {
        if (!manage()) {
            return;
        }
        List<CompoundTag> rows = list(key);
        for (int i = 0; i < Math.min(10, rows.size()); i++) {
            String id = rows.get(i).getStringOr("id", "");
            addRenderableWidget(Button.builder(Component.translatable(label), b -> act(action, id)).bounds(x + W - 66, y + 36 + i * 17, 60, 15).build());
        }
    }

    private void patternWidgets(int x, int y) {
        if (!manage()) {
            return;
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.colony.pattern_add"), b -> act("pattern_add"))
            .bounds(x + 8, y + H - 22, 120, 16).build());
        List<CompoundTag> lib = list("library");
        for (int i = 0; i < Math.min(6, lib.size()); i++) {
            CompoundTag e = lib.get(i);
            String id = e.getStringOr("id", "");
            boolean on = e.getBooleanOr("enabled", true);
            String role = e.getStringOr("role", "generic");
            String next = dev.autarkia.colony.ColonyActions.PATTERN_ROLES.get((dev.autarkia.colony.ColonyActions.PATTERN_ROLES.indexOf(role) + 1)
                % dev.autarkia.colony.ColonyActions.PATTERN_ROLES.size());
            int ry = y + 38 + i * 18;
            addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.colony.pattern_role." + role), b -> act("pattern_role", id, next))
                .bounds(x + 176, ry, 56, 16).build());
            addRenderableWidget(Button.builder(Component.literal(on ? "[x]" : "[ ]"), b -> act("pattern_enable", id, on ? "off" : "on"))
                .bounds(x + 234, ry, 26, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.colony.pattern_remove"), b -> act("pattern_remove", id))
                .bounds(x + 262, ry, 62, 16).build());
        }
        List<CompoundTag> inst = list("instances");
        for (int i = 0; i < Math.min(3, inst.size()); i++) {
            CompoundTag e = inst.get(i);
            String id = e.getStringOr("id", "");
            boolean w = e.getBooleanOr("watched", false);
            addRenderableWidget(Button.builder(Component.translatable(w ? "screen.autarkia.colony.pattern_unwatch" : "screen.autarkia.colony.pattern_watch"),
                b -> act("pattern_watch", id, w ? "off" : "on")).bounds(x + 262, y + 160 + i * 12, 62, 11).build());
        }
    }

    private void autonomyButtons(int x, int y) {
        if (!manage()) {
            return;
        }
        String[] levels = {"manual", "assisted", "autonomous"};
        String now = data().getStringOr("autonomy", "assisted");
        for (int i = 0; i < 3; i++) {
            String lv = levels[i];
            Button b = Button.builder(Component.translatable("colony.autonomy.autarkia." + lv), p -> act("autonomy", lv)).bounds(x + i * 82, y, 80, 16).build();
            b.active = !lv.equals(now);
            addRenderableWidget(b);
        }
    }

    private void settingsWidgets(int x, int y) {
        if (!manage()) {
            return;
        }
        autonomyButtons(x + 8, y + 36);
        List<CompoundTag> tpl = list("templates");
        int pages = Math.max(1, (tpl.size() + TEMPLATES_PER_PAGE - 1) / TEMPLATES_PER_PAGE);
        templatePage = Math.min(templatePage, pages - 1);
        int first = templatePage * TEMPLATES_PER_PAGE;
        for (int i = first; i < Math.min(tpl.size(), first + TEMPLATES_PER_PAGE); i++) {
            CompoundTag t = tpl.get(i);
            String id = t.getStringOr("id", "");
            boolean on = t.getBooleanOr("enabled", true);
            Component label = Component.literal(on ? "[x] " : "[ ] ").append(Component.translatable("colony_template.autarkia." + id));
            int k = i - first;
            addRenderableWidget(Button.builder(label, b -> act("template", id, on ? "off" : "on"))
                .bounds(x + 8 + (k % 2) * 158, y + 58 + (k / 2) * 18, 156, 16).build());
        }
        boolean signs = data().getBooleanOr("signs", true);
        addRenderableWidget(Button.builder(Component.translatable(signs ? "screen.autarkia.colony.signs_on" : "screen.autarkia.colony.signs_off"),
            b -> act("signs", signs ? "off" : "on")).bounds(x + 8, y + 150, 104, 16).build());
        if (pages > 1) {
            Button prev = Button.builder(Component.literal("<"), b -> {
                templatePage--;
                rebuildWidgets();
            }).bounds(x + W - 80, y + 150, 20, 16).build();
            prev.active = templatePage > 0;
            addRenderableWidget(prev);
            Button next = Button.builder(Component.literal(">"), b -> {
                templatePage++;
                rebuildWidgets();
            }).bounds(x + W - 28, y + 150, 20, 16).build();
            next.active = templatePage < pages - 1;
            addRenderableWidget(next);
        }
        stockItem = new EditBox(font, x + 8, y + 172, 130, 16, Component.translatable("screen.autarkia.colony.stock_item"));
        stockItem.setHint(Component.literal("minecraft:coal"));
        stockCount = new EditBox(font, x + 142, y + 172, 40, 16, Component.translatable("screen.autarkia.colony.stock_count"));
        stockCount.setHint(Component.literal("64"));
        addRenderableWidget(stockItem);
        addRenderableWidget(stockCount);
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.colony.stock_set"), b -> {
            if (!stockItem.getValue().isBlank() && stockCount.getValue().matches("\\d+")) {
                act("stock", stockItem.getValue().trim(), stockCount.getValue());
            }
        }).bounds(x + 186, y + 172, 50, 16).build());
        if (data().getBooleanOr("owner", false)) {
            trustBox = new EditBox(font, x + 8, y + 194, 130, 16, Component.translatable("screen.autarkia.colony.trust_name"));
            trustBox.setHint(Component.translatable("screen.autarkia.colony.trust_name"));
            addRenderableWidget(trustBox);
            addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.colony.trust"), b -> act("trust", trustBox.getValue().trim()))
                .bounds(x + 142, y + 194, 50, 16).build());
            ListTag ids = data().getListOrEmpty("trusted_ids");
            if (!ids.isEmpty()) {
                trustIndex %= ids.size();
                String id = ids.getString(trustIndex).orElse("");
                addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.colony.trust_remove"), b -> act("untrust", id))
                    .bounds(x + 272, y + 194, 50, 16).build());
                if (ids.size() > 1) {
                    addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                        trustIndex++;
                        rebuildWidgets();
                    }).bounds(x + 256, y + 194, 14, 16).build());
                }
            }
        }
    }

    private void settingsLabels(GuiGraphicsExtractor g, int top) {
        g.text(font, Component.translatable("screen.autarkia.colony.settings_title"), 8, top, 0xFFF2D3A0);
        int count = list("templates").size();
        int pages = Math.max(1, (count + TEMPLATES_PER_PAGE - 1) / TEMPLATES_PER_PAGE);
        if (manage() && pages > 1) {
            Component page = Component.literal((templatePage + 1) + " / " + pages);
            g.text(font, page, W - 44 - font.width(page) / 2, 154, 0xFFDDDDDD);
        }
        if (data().getBooleanOr("owner", false)) {
            ListTag names = data().getListOrEmpty("trusted");
            Component who = names.isEmpty() ? Component.translatable("screen.autarkia.colony.trusted_none")
                : Component.literal(names.getString(trustIndex % names.size()).orElse(""));
            g.text(font, fit(who, 58), 196, 198, 0xFFDDDDDD);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (ClientColony.version() != seen && (nameBox == null || !nameBox.isFocused()) && (trustBox == null || !trustBox.isFocused())
            && (stockItem == null || !stockItem.isFocused()) && (stockCount == null || !stockCount.isFocused())) {
            rebuildWidgets();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, leftPos, topPos, W, H);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        int top = menu.mode == ColonyMenu.Mode.CORE ? 26 : 8;
        if (!manage() && tab != ColonySync.OVERVIEW) {
            int nw = tab == ColonySync.PROJECTS ? 100 : W - 16;
            Component note = Component.translatable("screen.autarkia.colony.view_only");
            wrap(g, note, 8, H - 4 - 10 * font.split(note, nw).size(), nw, 0xFFAAAAAA, H - 2);
        }
        switch (tab) {
            case ColonySync.OVERVIEW -> overview(g, top);
            case ColonySync.PROJECTS -> projects(g, top);
            case ColonySync.JOBS -> rows(g, top, "jobs");
            case ColonySync.WORKERS -> rows(g, top, "workers");
            case ColonySync.PATTERNS -> patterns(g, top);
            default -> settingsLabels(g, top);
        }
    }

    private void patterns(GuiGraphicsExtractor g, int y) {
        List<CompoundTag> lib = list("library");
        g.text(font, Component.translatable("screen.autarkia.colony.pattern_header", lib.size(), data().getIntOr("slots", 0)), 8, y, 0xFFF2D3A0);
        if (lib.isEmpty()) {
            g.text(font, Component.translatable("screen.autarkia.colony.pattern_none"), 8, y + 14, 0xFF888888);
        }
        for (int i = 0; i < Math.min(6, lib.size()); i++) {
            CompoundTag e = lib.get(i);
            Component line = e.getBooleanOr("missing", false) ? Component.translatable("screen.autarkia.colony.pattern_missing")
                : Component.translatable("screen.autarkia.colony.pattern_row", e.getStringOr("name", ""), e.getStringOr("size", ""), e.getIntOr("instances", 0));
            g.text(font, fit(line, manage() ? 164 : W - 16), 8, y + 16 + i * 18, e.getBooleanOr("enabled", true) ? 0xFFDDDDDD : 0xFF888888);
        }
        List<CompoundTag> inst = list("instances");
        g.text(font, Component.translatable("screen.autarkia.colony.pattern_instances", inst.size()), 8, y + 124, 0xFFF2D3A0);
        for (int i = 0; i < Math.min(3, inst.size()); i++) {
            CompoundTag e = inst.get(i);
            BlockPos o = BlockPos.of(e.getLongOr("origin", 0));
            g.text(font, fit(Component.translatable("screen.autarkia.colony.pattern_instance", e.getStringOr("name", ""), o.getX(), o.getY(), o.getZ(),
                Component.translatable(e.getBooleanOr("watched", false) ? "screen.autarkia.colony.pattern_watched" : "screen.autarkia.colony.pattern_unwatched")),
                manage() ? 250 : W - 16), 8, y + 136 + i * 12, 0xFFBBBBBB);
        }
    }

    private void overview(GuiGraphicsExtractor g, int y) {
        CompoundTag d = data();
        int gx = W - 8 - GRAPH_SAMPLES * 5;
        int textWidth = gx - 14;
        int half = d.getIntOr("half", 16);
        Component[] lines = {
            Component.translatable("screen.autarkia.colony.title", d.getStringOr("name", ""), d.getIntOr("tier", 1)),
            Component.translatable("screen.autarkia.colony.domain", half * 2 + 1, half * 2 + 1),
            Component.translatable("screen.autarkia.colony.supply", fmt(d.getDoubleOr("s01", 0))),
            Component.translatable("screen.autarkia.colony.demand", fmt(d.getDoubleOr("s02", 0))),
            Component.translatable("screen.autarkia.colony.storage", Math.round(d.getDoubleOr("s09", 0) * 100)),
            Component.translatable("screen.autarkia.colony.workers", d.getIntOr("workers", 0), d.getIntOr("cap", 4)),
            Component.translatable("screen.autarkia.colony.autonomy", Component.translatable("colony.autonomy.autarkia." + d.getStringOr("autonomy", "assisted"))),
            Component.translatable("screen.autarkia.colony.budget", fmt(d.getDoubleOr("budget", 0)), d.getIntOr("daily", 64), d.getLongOr("day", 0))};
        int[] colors = {0xFFF2D3A0, 0xFFDDDDDD, 0xFF4DCCFF, 0xFFFFA040, 0xFFB8E8D6, 0xFFDDDDDD, 0xFFDDDDDD, 0xFFFFC27A};
        for (int i = 0; i < lines.length; i++) {
            g.text(font, fit(lines[i], textWidth), 8, y + i * 11, colors[i]);
        }
        ListTag hist = d.getListOrEmpty("history");
        int gy = y + 2;
        int gh = 60;
        double max = 1;
        for (int i = 0; i < hist.size(); i++) {
            max = Math.max(max, Math.max(hist.getCompoundOrEmpty(i).getDoubleOr("s", 0), hist.getCompoundOrEmpty(i).getDoubleOr("d", 0)));
        }
        g.fill(gx - 1, gy - 1, gx + GRAPH_SAMPLES * 5 + 1, gy + gh + 1, 0xFF333333);
        g.fill(gx, gy, gx + GRAPH_SAMPLES * 5, gy + gh, 0xFF101010);
        for (int i = Math.max(0, hist.size() - GRAPH_SAMPLES), k = 0; i < hist.size(); i++, k++) {
            CompoundTag h = hist.getCompoundOrEmpty(i);
            int s = (int) Math.round(gh * h.getDoubleOr("s", 0) / max);
            int dd = (int) Math.round(gh * h.getDoubleOr("d", 0) / max);
            g.fill(gx + k * 5, gy + gh - s, gx + k * 5 + 2, gy + gh, 0xFF4DCCFF);
            g.fill(gx + k * 5 + 2, gy + gh - dd, gx + k * 5 + 4, gy + gh, 0xFFFFA040);
        }
        Component supply = Component.translatable("screen.autarkia.colony.graph_supply");
        g.text(font, supply, gx, gy + gh + 3, 0xFF4DCCFF);
        g.text(font, Component.translatable("screen.autarkia.colony.graph_demand"), gx + font.width(supply) + 8, gy + gh + 3, 0xFFFFA040);
        g.text(font, Component.translatable("screen.autarkia.colony.graph_samples", GRAPH_SAMPLES), gx, gy + gh + 13, 0xFF888888);
        ListTag log = d.getListOrEmpty("log");
        int ly = y + 92;
        for (int i = Math.max(0, log.size() - LOG_LINES); i < log.size(); i++) {
            g.text(font, fit(logLine(log.getString(i).orElse("")), W - 16), 8, ly, 0xFFAAAAAA);
            ly += 10;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (tab != ColonySync.OVERVIEW || mouseX < leftPos + 8 || mouseX >= leftPos + W - 8) {
            return;
        }
        ListTag log = data().getListOrEmpty("log");
        int first = Math.max(0, log.size() - LOG_LINES);
        int row = (mouseY - topPos - (menu.mode == ColonyMenu.Mode.CORE ? 26 : 8) - 92) / 10;
        if (mouseY >= topPos + (menu.mode == ColonyMenu.Mode.CORE ? 26 : 8) + 92 && first + row < log.size()) {
            Component line = logLine(log.getString(first + row).orElse(""));
            if (font.width(line) > W - 16) {
                g.setTooltipForNextFrame(font, font.split(line, 240), mouseX, mouseY);
            }
        }
    }

    private Component fit(Component text, int width) {
        String s = text.getString();
        if (font.width(s) <= width) {
            return text;
        }
        return Component.literal(font.plainSubstrByWidth(s, width - font.width("...")) + "...");
    }

    private static Component logLine(String raw) {
        if (raw.isEmpty() || raw.charAt(0) != dev.autarkia.colony.ColonyRuntime.LOG_KEY) {
            return Component.literal(raw);
        }
        String[] parts = raw.substring(1).split(String.valueOf(dev.autarkia.colony.ColonyRuntime.LOG_KEY), -1);
        Object[] args = new Object[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            args[i - 1] = parts[i].startsWith("t:") ? Component.translatable("colony_template.autarkia." + parts[i].substring(2)) : parts[i];
        }
        return Component.translatable("colony.log.autarkia." + parts[0], args);
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    /** Draw text word-wrapped to width from (x, y) while whole lines fit above maxY; return the y under the last line drawn. */
    private int wrap(GuiGraphicsExtractor g, Component text, int x, int y, int width, int color, int maxY) {
        for (FormattedCharSequence line : font.split(text, width)) {
            if (y + 9 > maxY) {
                return y;
            }
            g.text(font, line, x, y, color);
            y += 10;
        }
        return y;
    }

    private void projects(GuiGraphicsExtractor g, int y) {
        String warning = data().getStringOr("warning", "");
        int maxY = actionTop - 3;
        List<CompoundTag> projects = list("projects");
        int x = 116;
        int wide = W - x - 8;
        if (!warning.isEmpty()) {
            maxY -= 10 * font.split(Component.literal(warning), wide).size();
            wrap(g, Component.literal(warning), x, maxY + 1, wide, 0xFFFF4040, actionTop);
        }
        if (projects.size() > 9) {
            g.text(font, Component.translatable("screen.autarkia.colony.bom_more", projects.size() - 9), 8, y - 2 + 9 * 18, 0xFF888888);
        }
        if (projects.isEmpty()) {
            wrap(g, Component.translatable("screen.autarkia.colony.no_projects"), x, y + 2, wide, 0xFF888888, maxY);
            return;
        }
        CompoundTag p = projects.get(Math.min(selected, projects.size() - 1));
        Component state = Component.translatable("colony.state.autarkia." + p.getStringOr("state", ""));
        int sw = font.width(state);
        g.text(font, state, x + wide - sw, y, 0xFF9BE7FF);
        int ty = wrap(g, Component.translatable("colony_template.autarkia." + p.getStringOr("template", "")), x, y, wide - sw - 6, 0xFFF2D3A0, maxY);
        long arg = Math.round(p.getDoubleOr("arg", 0));
        ty = wrap(g, Component.translatable("colony.reason.autarkia." + p.getStringOr("reason", ""), arg), x, ty + 1, wide, 0xFFDDDDDD, maxY);
        String block = p.getStringOr("block", "");
        if (!block.isEmpty()) {
            ty = wrap(g, Component.translatable("screen.autarkia.colony.blocked", Component.translatableWithFallback("jade.autarkia.worker.reason." + block, block)),
                x, ty + 1, wide, 0xFFFF6B6B, maxY);
        }
        ty = wrap(g, Component.translatable("screen.autarkia.colony.cost", fmt(p.getDoubleOr("cost", 0)), fmt(data().getDoubleOr("budget", 0))),
            x, ty + 1, wide, 0xFFFFC27A, maxY);
        int bomMax = maxY;
        ty = wrap(g, Component.translatable("screen.autarkia.colony.bom"), x, ty + 5, wide, 0xFFF2D3A0, bomMax);
        ListTag bom = p.getListOrEmpty("bom");
        for (int i = 0; i < bom.size(); i++) {
            CompoundTag l = bom.getCompoundOrEmpty(i);
            int need = l.getIntOr("need", 0);
            int stock = l.getIntOr("stock", 0);
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(l.getStringOr("item", "minecraft:air")));
            Component line = Component.translatable("screen.autarkia.colony.bom_line", item.getDefaultInstance().getHoverName(), need, stock,
                l.getIntOr("reserved", 0));
            int lines = font.split(line, wide - 4).size();
            boolean last = i == bom.size() - 1;
            if (ty + lines * 10 + (last ? 0 : 10) > bomMax + 1) {
                ty = wrap(g, Component.translatable("screen.autarkia.colony.bom_more", bom.size() - i), x + 4, ty, wide - 4, 0xFF888888, bomMax);
                break;
            }
            ty = wrap(g, line, x + 4, ty, wide - 4, stock >= need ? 0xFF8FE08F : 0xFFFF9B6B, bomMax);
        }
        ty = wrap(g, Component.translatable("screen.autarkia.colony.priority", fmt(p.getDoubleOr("priority", 0)), p.getIntOr("adjust", 0)),
            x, ty + 3, wide, 0xFFBBBBBB, maxY);
        BlockPos o = BlockPos.of(p.getLongOr("origin", 0));
        ty = wrap(g, Component.translatable("screen.autarkia.colony.site", o.getX(), o.getY(), o.getZ(), p.getStringOr("rotation", "none"),
            fmt(p.getDoubleOr("score", 0)), p.getIntOr("edits", 0)), x, ty + 1, wide, 0xFFBBBBBB, maxY);
        CompoundTag labor = p.getCompoundOrEmpty("labor");
        StringBuilder sb = new StringBuilder();
        for (String k : labor.keySet()) {
            sb.append(k).append(' ').append(labor.getIntOr(k, 0)).append("  ");
        }
        if (!sb.isEmpty()) {
            wrap(g, Component.translatable("screen.autarkia.colony.labor", sb.toString().trim()), x, ty + 1, wide, 0xFFAAAAAA, maxY);
        }
    }

    private void rows(GuiGraphicsExtractor g, int y, String key) {
        List<CompoundTag> rows = list(key);
        g.text(font, Component.translatable("screen.autarkia.colony." + key + "_header"), 8, y, 0xFFF2D3A0);
        if (rows.isEmpty()) {
            g.text(font, Component.translatable("screen.autarkia.colony.none"), 8, y + 14, 0xFF888888);
        }
        for (int i = 0; i < Math.min(10, rows.size()); i++) {
            CompoundTag r = rows.get(i);
            int ry = y + 12 + i * 17;
            if (key.equals("jobs")) {
                BlockPos t = BlockPos.of(r.getLongOr("target", 0));
                g.text(font, fit(Component.translatable("screen.autarkia.colony.job_row", r.getStringOr("type", ""), t.getX(), t.getY(), t.getZ(),
                    r.getStringOr("assignee", "-"), r.getIntOr("attempts", 0), r.getStringOr("status", "")), W - 82), 8, ry, 0xFFDDDDDD);
            } else {
                float charge = r.getFloatOr("charge", 0);
                String st = r.getStringOr("state", "");
                Component state = st.startsWith("failed:")
                    ? Component.translatable("screen.autarkia.colony.worker_failed", Component.translatableWithFallback("jade.autarkia.worker.reason." + st.substring(7), st.substring(7)))
                    : Component.translatableWithFallback("screen.autarkia.colony.worker_state." + st, st);
                g.text(font, fit(Component.translatable("screen.autarkia.colony.worker_row", Component.translatable("entity.autarkia." + r.getStringOr("type",
                    "porter")), state, r.getStringOr("job", "-")), 186), 8, ry, st.startsWith("failed") ? 0xFFFF9B6B : 0xFFDDDDDD);
                g.fill(200, ry + 1, 250, ry + 7, 0xFF2A2A2A);
                g.fill(200, ry + 1, 200 + Math.round(50 * charge), ry + 7, charge < 0.2f ? 0xFFDD4444 : 0xFF55CC55);
            }
        }
    }
}

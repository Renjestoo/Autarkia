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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The colony screen (05b section 9.1): tabs Overview, Projects, Jobs, Workers and Settings at the Core, the Projects
 * tab alone at a project board, Projects plus the autonomy switch on the planning tablet. Everything shown comes from
 * {@code colony_sync}; every button sends one {@code colony_action}.
 */
public class ColonyScreen extends AbstractContainerScreen<ColonyMenu> {
    private static final int W = 330;
    private static final int H = 220;
    private static final String[] TABS = {"overview", "projects", "jobs", "workers", "settings", "patterns"};
    private int tab;
    private int selected;
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
        if (projects.isEmpty() || selected >= projects.size()) {
            return;
        }
        CompoundTag p = projects.get(selected);
        String id = p.getStringOr("id", "");
        String state = p.getStringOr("state", "");
        int bx = x + 112;
        int by = y + H - (menu.mode == ColonyMenu.Mode.TABLET ? 42 : 22);
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
        int bw = Math.min(54, (W - 116) / Math.max(1, row.size()) - 2);
        for (int i = 0; i < row.size(); i++) {
            Button b = row.get(i);
            b.setRectangle(bw, 16, bx + i * (bw + 2), by);
            addRenderableWidget(b);
        }
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
        for (int i = 0; i < tpl.size(); i++) {
            CompoundTag t = tpl.get(i);
            String id = t.getStringOr("id", "");
            boolean on = t.getBooleanOr("enabled", true);
            Component label = Component.translatable("colony_template.autarkia." + id).append(on ? " [x]" : " [ ]");
            addRenderableWidget(Button.builder(label, b -> act("template", id, on ? "off" : "on"))
                .bounds(x + 8 + (i % 3) * 106, y + 66 + (i / 3) * 18, 104, 16).build());
        }
        boolean signs = data().getBooleanOr("signs", true);
        addRenderableWidget(Button.builder(Component.translatable(signs ? "screen.autarkia.colony.signs_on" : "screen.autarkia.colony.signs_off"),
            b -> act("signs", signs ? "off" : "on")).bounds(x + 8, y + 140, 104, 16).build());
        stockItem = new EditBox(font, x + 8, y + 162, 130, 16, Component.translatable("screen.autarkia.colony.stock_item"));
        stockItem.setHint(Component.literal("minecraft:coal"));
        stockCount = new EditBox(font, x + 142, y + 162, 40, 16, Component.translatable("screen.autarkia.colony.stock_count"));
        stockCount.setHint(Component.literal("64"));
        addRenderableWidget(stockItem);
        addRenderableWidget(stockCount);
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.colony.stock_set"), b -> {
            if (!stockItem.getValue().isBlank() && stockCount.getValue().matches("\\d+")) {
                act("stock", stockItem.getValue().trim(), stockCount.getValue());
            }
        }).bounds(x + 186, y + 162, 50, 16).build());
        if (data().getBooleanOr("owner", false)) {
            trustBox = new EditBox(font, x + 8, y + 184, 130, 16, Component.translatable("screen.autarkia.colony.trust_name"));
            addRenderableWidget(trustBox);
            addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.colony.trust"), b -> act("trust", trustBox.getValue().trim()))
                .bounds(x + 142, y + 184, 50, 16).build());
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
        g.fill(leftPos - 1, topPos - 1, leftPos + W + 1, topPos + H + 1, 0xFF8C5A2B);
        g.fill(leftPos, topPos, leftPos + W, topPos + H, 0xF0141A20);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        int top = menu.mode == ColonyMenu.Mode.CORE ? 26 : 8;
        switch (tab) {
            case ColonySync.OVERVIEW -> overview(g, top);
            case ColonySync.PROJECTS -> projects(g, top);
            case ColonySync.JOBS -> rows(g, top, "jobs");
            case ColonySync.WORKERS -> rows(g, top, "workers");
            case ColonySync.PATTERNS -> patterns(g, top);
            default -> g.text(font, Component.translatable("screen.autarkia.colony.settings_title"), 8, top, 0xFFF2D3A0);
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
            g.text(font, line, 8, y + 16 + i * 18, e.getBooleanOr("enabled", true) ? 0xFFDDDDDD : 0xFF888888);
        }
        List<CompoundTag> inst = list("instances");
        g.text(font, Component.translatable("screen.autarkia.colony.pattern_instances", inst.size()), 8, y + 124, 0xFFF2D3A0);
        for (int i = 0; i < Math.min(3, inst.size()); i++) {
            CompoundTag e = inst.get(i);
            BlockPos o = BlockPos.of(e.getLongOr("origin", 0));
            g.text(font, Component.translatable("screen.autarkia.colony.pattern_instance", e.getStringOr("name", ""), o.getX(), o.getY(), o.getZ(),
                Component.translatable(e.getBooleanOr("watched", false) ? "screen.autarkia.colony.pattern_watched" : "screen.autarkia.colony.pattern_unwatched")),
                8, y + 136 + i * 12, 0xFFBBBBBB);
        }
    }

    private void overview(GuiGraphicsExtractor g, int y) {
        CompoundTag d = data();
        g.text(font, Component.translatable("screen.autarkia.colony.title", d.getStringOr("name", ""), d.getIntOr("tier", 1)), 8, y, 0xFFF2D3A0);
        int half = d.getIntOr("half", 16);
        g.text(font, Component.translatable("screen.autarkia.colony.domain", half * 2 + 1, half * 2 + 1), 8, y + 12, 0xFFDDDDDD);
        g.text(font, Component.translatable("screen.autarkia.colony.power", fmt(d.getDoubleOr("s01", 0)), fmt(d.getDoubleOr("s02", 0))), 8, y + 24, 0xFF9BE7FF);
        g.text(font, Component.translatable("screen.autarkia.colony.storage", Math.round(d.getDoubleOr("s09", 0) * 100)), 8, y + 36, 0xFFB8E8D6);
        g.text(font, Component.translatable("screen.autarkia.colony.workers", d.getIntOr("workers", 0), d.getIntOr("cap", 4)), 8, y + 48, 0xFFDDDDDD);
        g.text(font, Component.translatable("screen.autarkia.colony.autonomy", Component.translatable("colony.autonomy.autarkia." + d.getStringOr("autonomy",
            "assisted"))), 8, y + 60, 0xFFDDDDDD);
        g.text(font, Component.translatable("screen.autarkia.colony.budget", fmt(d.getDoubleOr("budget", 0)), d.getIntOr("daily", 64),
            d.getLongOr("day", 0)), 8, y + 72, 0xFFFFC27A);
        ListTag hist = d.getListOrEmpty("history");
        int gx = 180;
        int gy = y + 4;
        int gh = 60;
        double max = 1;
        for (int i = 0; i < hist.size(); i++) {
            max = Math.max(max, Math.max(hist.getCompoundOrEmpty(i).getDoubleOr("s", 0), hist.getCompoundOrEmpty(i).getDoubleOr("d", 0)));
        }
        g.fill(gx - 1, gy - 1, gx + 24 * 6 + 1, gy + gh + 1, 0xFF333333);
        g.fill(gx, gy, gx + 24 * 6, gy + gh, 0xFF101010);
        for (int i = 0; i < hist.size(); i++) {
            CompoundTag h = hist.getCompoundOrEmpty(i);
            int s = (int) Math.round(gh * h.getDoubleOr("s", 0) / max);
            int dd = (int) Math.round(gh * h.getDoubleOr("d", 0) / max);
            g.fill(gx + i * 6, gy + gh - s, gx + i * 6 + 3, gy + gh, 0xFF4DCCFF);
            g.fill(gx + i * 6 + 3, gy + gh - dd, gx + i * 6 + 5, gy + gh, 0xFFFFA040);
        }
        g.text(font, Component.translatable("screen.autarkia.colony.graph"), gx, gy + gh + 3, 0xFF888888);
        ListTag log = d.getListOrEmpty("log");
        int ly = y + 92;
        for (int i = Math.max(0, log.size() - 8); i < log.size(); i++) {
            g.text(font, Component.literal(log.getString(i).orElse("")), 8, ly, 0xFFAAAAAA);
            ly += 10;
        }
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private void projects(GuiGraphicsExtractor g, int y) {
        String warning = data().getStringOr("warning", "");
        if (!warning.isEmpty()) {
            g.text(font, Component.literal(warning), 8, H - (menu.mode == ColonyMenu.Mode.TABLET ? 54 : 34), 0xFFFF4040, false);
        }
        List<CompoundTag> projects = list("projects");
        if (projects.isEmpty()) {
            g.text(font, Component.translatable("screen.autarkia.colony.no_projects"), 116, y + 2, 0xFF888888);
            return;
        }
        CompoundTag p = projects.get(Math.min(selected, projects.size() - 1));
        int x = 116;
        g.text(font, Component.translatable("colony_template.autarkia." + p.getStringOr("template", "")), x, y, 0xFFF2D3A0);
        g.text(font, Component.translatable("colony.state.autarkia." + p.getStringOr("state", "")), x + 150, y, 0xFF9BE7FF);
        long arg = Math.round(p.getDoubleOr("arg", 0));
        g.text(font, Component.translatable("colony.reason.autarkia." + p.getStringOr("reason", ""), arg), x, y + 11, 0xFFDDDDDD);
        BlockPos o = BlockPos.of(p.getLongOr("origin", 0));
        g.text(font, Component.translatable("screen.autarkia.colony.site", o.getX(), o.getY(), o.getZ(), p.getStringOr("rotation", "none"),
            fmt(p.getDoubleOr("score", 0)), p.getIntOr("edits", 0)), x, y + 22, 0xFFBBBBBB);
        g.text(font, Component.translatable("screen.autarkia.colony.cost", fmt(p.getDoubleOr("cost", 0)), fmt(data().getDoubleOr("budget", 0))), x, y + 33,
            0xFFFFC27A);
        g.text(font, Component.translatable("screen.autarkia.colony.priority", fmt(p.getDoubleOr("priority", 0)), p.getIntOr("adjust", 0)), x, y + 44,
            0xFFBBBBBB);
        String block = p.getStringOr("block", "");
        if (!block.isEmpty()) {
            g.text(font, Component.translatable("screen.autarkia.colony.blocked", block), x, y + 55, 0xFFFF6B6B);
        }
        int by = y + 68;
        g.text(font, Component.translatable("screen.autarkia.colony.bom"), x, by, 0xFFF2D3A0);
        ListTag bom = p.getListOrEmpty("bom");
        for (int i = 0; i < Math.min(7, bom.size()); i++) {
            CompoundTag l = bom.getCompoundOrEmpty(i);
            int need = l.getIntOr("need", 0);
            int stock = l.getIntOr("stock", 0);
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(l.getStringOr("item", "minecraft:air")));
            g.text(font, Component.translatable("screen.autarkia.colony.bom_line", item.getDefaultInstance().getHoverName(), need, stock,
                l.getIntOr("reserved", 0)), x + 4,
                by + 11 + i * 10, stock >= need ? 0xFF8FE08F : 0xFFFF9B6B);
        }
        CompoundTag labor = p.getCompoundOrEmpty("labor");
        StringBuilder sb = new StringBuilder();
        for (String k : labor.keySet()) {
            sb.append(k).append(' ').append(labor.getIntOr(k, 0)).append("  ");
        }
        if (!sb.isEmpty()) {
            g.text(font, Component.translatable("screen.autarkia.colony.labor", sb.toString().trim()), x, by + 86, 0xFFAAAAAA);
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
                g.text(font, Component.translatable("screen.autarkia.colony.job_row", r.getStringOr("type", ""), t.getX(), t.getY(), t.getZ(),
                    r.getStringOr("assignee", "-"), r.getIntOr("attempts", 0), r.getStringOr("status", "")), 8, ry, 0xFFDDDDDD);
            } else {
                float charge = r.getFloatOr("charge", 0);
                g.text(font, Component.translatable("screen.autarkia.colony.worker_row", Component.translatable("entity.autarkia." + r.getStringOr("type",
                    "porter")), r.getStringOr("state", ""), r.getStringOr("job", "-")), 8, ry, 0xFFDDDDDD);
                g.fill(200, ry + 1, 250, ry + 7, 0xFF2A2A2A);
                g.fill(200, ry + 1, 200 + Math.round(50 * charge), ry + 7, charge < 0.2f ? 0xFFDD4444 : 0xFF55CC55);
            }
        }
    }
}

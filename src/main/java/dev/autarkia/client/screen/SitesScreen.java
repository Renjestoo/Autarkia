package dev.autarkia.client.screen;

import dev.autarkia.network.SiteRequestPayload;
import dev.autarkia.network.StockViewPayload;
import dev.autarkia.network.StockViewRequestPayload;
import dev.autarkia.storage.TransferOrder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The linked stock view (04 sections 2.6 and 4.1): the ledger terminal's "Sites" tab and the depot's "Linked view". Left
 * the sites (main site first, then the outpost depots) with their slot fill; stale sites are grey with "Last seen N min
 * ago". Right the merged stock rows of the selected site, or of all sites with a site column, one page of 256 rows at a
 * time. Under the sites the recent transfer orders. From a ledger, "Request from site" orders the selected row's item
 * (amount 1 to 1,728) from its outpost site. The view never moves items itself; Escape and Back return to the screen
 * that opened it, whose menu stays open on the server.
 */
public class SitesScreen extends Screen {
    private static final int W = 320;
    private static final int H = 230;
    private static final int SITE_ROWS = 8;
    private static final int ROWS = 10;
    private static @Nullable StockViewPayload last;
    private final Screen parent;
    private final BlockPos pos;
    private final boolean ledger;
    private int page;
    private int site = -1;
    private int scroll;
    private int selected = -1;
    private int amount = 64;
    private int ticks;
    private Button request;

    public SitesScreen(Screen parent, BlockPos pos, boolean ledger) {
        super(Component.translatable("screen.autarkia.sites.title"));
        this.parent = parent;
        this.pos = pos;
        this.ledger = ledger;
    }

    /** Keep the latest stock view for the open sites screen. */
    public static void stock(StockViewPayload p) {
        last = p;
    }

    private StockViewPayload.Sites data() {
        return last == null || !last.pos().equals(pos) ? StockViewPayload.Sites.EMPTY : last.sites();
    }

    private void refresh() {
        ClientPacketDistributor.sendToServer(new StockViewRequestPayload(pos, page));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        int x = left();
        int y = top() + H - 22;
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.sites.back"), b -> onClose()).bounds(x + 8, y, 40, 16).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page = Math.max(0, page - 1);
            scroll = 0;
            refresh();
        }).bounds(x + 124, y, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page = Math.min(data().pages() - 1, page + 1);
            scroll = 0;
            refresh();
        }).bounds(x + 196, y, 16, 16).build());
        if (ledger) {
            int ay = top() + H - 42;
            addRenderableWidget(Button.builder(Component.literal("-64"), b -> amount = Math.max(1, amount - 64)).bounds(x + 124, ay, 24, 16).build());
            addRenderableWidget(Button.builder(Component.literal("+64"), b -> amount = Math.min(1728, amount + 64)).bounds(x + 184, ay, 24, 16).build());
            request = Button.builder(Component.translatable("screen.autarkia.sites.request"), b -> requestSelected()).bounds(x + 214, ay, 98, 16).build();
            addRenderableWidget(request);
        }
        refresh();
    }

    private List<StockViewPayload.Row> rows() {
        List<StockViewPayload.Row> out = new ArrayList<>();
        for (StockViewPayload.Row r : data().rows()) {
            if (site < 0 || r.site() == site) {
                out.add(r);
            }
        }
        return out;
    }

    private Component siteName(StockViewPayload.Site s) {
        if (s.main()) {
            return Component.translatable("screen.autarkia.sites.main");
        }
        return s.name().isEmpty() ? Component.translatable("screen.autarkia.sites.depot", s.pos().getX(), s.pos().getY(), s.pos().getZ())
            : Component.literal(s.name());
    }

    private void requestSelected() {
        List<StockViewPayload.Row> rows = rows();
        if (selected < 0 || selected >= rows.size()) {
            return;
        }
        StockViewPayload.Row r = rows.get(selected);
        StockViewPayload.Site s = data().sites().get(r.site());
        if (!s.main()) {
            ClientPacketDistributor.sendToServer(new SiteRequestPayload(pos, s.key(), r.item(), amount));
            refresh();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks % 40 == 0) {
            refresh();
        }
        if (request != null) {
            List<StockViewPayload.Row> rows = rows();
            request.active = selected >= 0 && selected < rows.size() && !data().sites().get(rows.get(selected).site()).main();
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll = Math.max(0, Math.min(Math.max(0, rows().size() - ROWS), scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        int x = left();
        int y = top();
        List<StockViewPayload.Site> sites = data().sites();
        for (int i = 0; i < Math.min(SITE_ROWS, sites.size()); i++) {
            if (e.x() >= x + 8 && e.x() < x + 118 && e.y() >= y + 22 + i * 14 && e.y() < y + 36 + i * 14) {
                site = site == i ? -1 : i;
                scroll = 0;
                selected = -1;
                return true;
            }
        }
        int rowCount = rows().size();
        for (int i = 0; i < ROWS && scroll + i < rowCount; i++) {
            if (e.x() >= x + 124 && e.x() < x + W - 8 && e.y() >= y + 22 + i * 16 && e.y() < y + 38 + i * 16) {
                selected = scroll + i;
                return true;
            }
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int x = left();
        int y = top();
        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xFF4A6A70);
        g.fill(x, y, x + W, y + H, 0xF0101418);
        g.text(font, title, x + 8, y + 8, 0xFFE0F0F0);
        StockViewPayload.Sites d = data();
        List<StockViewPayload.Site> sites = d.sites();
        for (int i = 0; i < Math.min(SITE_ROWS, sites.size()); i++) {
            StockViewPayload.Site s = sites.get(i);
            int ry = y + 22 + i * 14;
            if (i == site) {
                g.fill(x + 8, ry, x + 118, ry + 13, 0x804FA0FF);
            }
            int fill = s.total() == 0 ? 0 : s.used() * 100 / s.total();
            g.text(font, siteName(s), x + 10, ry + 2, s.stale() ? 0xFF808080 : 0xFFE0E0E0);
            String pct = fill + "%";
            g.text(font, Component.literal(pct), x + 116 - font.width(pct), ry + 2, s.stale() ? 0xFF808080 : 0xFF9BE7FF);
        }
        int oy = y + 22 + SITE_ROWS * 14 + 4;
        g.text(font, Component.translatable("screen.autarkia.sites.orders"), x + 8, oy, 0xFFB0B0B0);
        for (int i = 0; i < Math.min(3, d.orders().size()); i++) {
            StockViewPayload.Order o = d.orders().get(i);
            TransferOrder.Status st = TransferOrder.Status.valueOf(o.status());
            g.fakeItem(o.item(), x + 8, oy + 10 + i * 17);
            Component line = o.reason().isEmpty() ? Component.translatable(st.key(), o.moved(), o.count())
                : Component.translatable("screen.autarkia.sites.reason." + o.reason());
            g.text(font, line, x + 26, oy + 14 + i * 17, st == TransferOrder.Status.FAILED ? 0xFFFF6060 : 0xFFDDDDDD);
        }
        List<StockViewPayload.Row> rows = rows();
        for (int i = 0; i < ROWS && scroll + i < rows.size(); i++) {
            StockViewPayload.Row r = rows.get(scroll + i);
            int ry = y + 22 + i * 16;
            if (scroll + i == selected) {
                g.fill(x + 124, ry, x + W - 8, ry + 16, 0x804FA0FF);
            }
            StockViewPayload.Site s = r.site() < sites.size() ? sites.get(r.site()) : null;
            boolean stale = s != null && s.stale();
            g.fakeItem(r.item(), x + 124, ry);
            g.text(font, Component.literal(Long.toString(r.count())), x + 144, ry + 4, stale ? 0xFF808080 : 0xFFFFFFFF);
            if (site < 0 && s != null) {
                Component n = siteName(s);
                g.text(font, n, x + W - 10 - font.width(n), ry + 4, stale ? 0xFF808080 : 0xFF9BE7FF);
            }
        }
        String pageText = (d.page() + 1) + " / " + d.pages();
        g.text(font, Component.literal(pageText), x + 168 - font.width(pageText) / 2, y + H - 18, 0xFFE0E0E0);
        if (ledger) {
            String am = Integer.toString(amount);
            g.text(font, Component.literal(am), x + 166 - font.width(am) / 2, y + H - 38, 0xFFE0E0E0);
        }
        tooltips(g, mouseX, mouseY, sites, rows);
    }

    private void tooltips(GuiGraphicsExtractor g, int mouseX, int mouseY, List<StockViewPayload.Site> sites, List<StockViewPayload.Row> rows) {
        int x = left();
        int y = top();
        for (int i = 0; i < Math.min(SITE_ROWS, sites.size()); i++) {
            if (mouseX >= x + 8 && mouseX < x + 118 && mouseY >= y + 22 + i * 14 && mouseY < y + 36 + i * 14) {
                StockViewPayload.Site s = sites.get(i);
                List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
                lines.add(siteName(s).getVisualOrderText());
                lines.add(Component.translatable("screen.autarkia.sites.slots", s.used(), s.total()).getVisualOrderText());
                lines.add(Component.translatable("screen.autarkia.sites.where", s.pos().getX(), s.pos().getY(), s.pos().getZ(), s.dimension()).getVisualOrderText());
                if (s.stale()) {
                    lines.add(Component.translatable("screen.autarkia.sites.last_seen", s.age() / 1200).getVisualOrderText());
                }
                g.setTooltipForNextFrame(font, lines, mouseX, mouseY);
                return;
            }
        }
        for (int i = 0; i < ROWS && scroll + i < rows.size(); i++) {
            if (mouseX >= x + 124 && mouseX < x + W - 8 && mouseY >= y + 22 + i * 16 && mouseY < y + 38 + i * 16) {
                ItemStack item = rows.get(scroll + i).item();
                g.setTooltipForNextFrame(font, List.of(item.getHoverName().getVisualOrderText()), mouseX, mouseY);
                return;
            }
        }
    }
}

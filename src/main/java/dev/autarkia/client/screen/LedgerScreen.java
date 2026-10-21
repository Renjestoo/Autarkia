package dev.autarkia.client.screen;

import dev.autarkia.network.LedgerRequestPayload;
import dev.autarkia.network.LedgerStatusPayload;
import dev.autarkia.network.StockViewPayload;
import dev.autarkia.network.StockViewRequestPayload;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.storage.LedgerRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Ledger terminal screen (04 section 6): search box and scroll grid of the warehouse stock with available / total
 * counts on the left, the request queue (item, delivered / amount, status color; status, ETA and reason in the
 * tooltip) on the right, the amount row with -64 / -1 / +1 / +64 and Request, and the output, input and crate slots.
 */
public class LedgerScreen extends SlotScreen {
    private static final int GRID_X = 8;
    private static final int GRID_Y = 30;
    private static final int COLS = 5;
    private static final int ROWS = 4;
    private static final int QUEUE_X = 103;
    private static final int QUEUE_Y = 17;
    private static final int QUEUE_ROWS = 5;
    private static @Nullable LedgerStatusPayload lastStatus;
    private static @Nullable StockViewPayload lastStock;
    private EditBox search;
    private int scroll;
    private int amount = 64;
    private ItemStack selected = ItemStack.EMPTY;

    public LedgerScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    /** Keep the latest status for the open screen. */
    public static void status(LedgerStatusPayload p) {
        lastStatus = p;
    }

    /** Keep the latest stock view for the open screen. */
    public static void stock(StockViewPayload p) {
        lastStock = p;
    }

    @Override
    protected boolean showsStatus() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + GRID_X, topPos + 17, 91, 11, Component.translatable("screen.autarkia.ledger.search"));
        search.setHint(Component.translatable("screen.autarkia.ledger.search"));
        search.setResponder(s -> scroll = 0);
        addRenderableWidget(search);
        int y = topPos + 106;
        addRenderableWidget(Button.builder(Component.literal("-64"), b -> add(-64)).bounds(leftPos + 8, y, 22, 14).build());
        addRenderableWidget(Button.builder(Component.literal("-1"), b -> add(-1)).bounds(leftPos + 30, y, 14, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> add(1)).bounds(leftPos + 66, y, 14, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+64"), b -> add(64)).bounds(leftPos + 80, y, 22, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.ledger.request"), b -> request()).bounds(leftPos + 103, y, 46, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.ledger.sites"),
            b -> minecraft.gui.setScreen(new SitesScreen(this, menu.pos, true))).bounds(leftPos + 120, topPos + 3, 50, 12).build());
        ClientPacketDistributor.sendToServer(new StockViewRequestPayload(menu.pos));
    }

    private void add(int d) {
        amount = Math.max(1, Math.min(1728, amount + d));
    }

    private void request() {
        if (!selected.isEmpty()) {
            ClientPacketDistributor.sendToServer(new LedgerRequestPayload(menu.pos, selected, amount));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (minecraft.player != null && minecraft.player.tickCount % 40 == 0) {
            ClientPacketDistributor.sendToServer(new StockViewRequestPayload(menu.pos));
        }
    }

    private List<StockViewPayload.Stock> shown() {
        List<StockViewPayload.Stock> out = new ArrayList<>();
        if (lastStock == null || !lastStock.pos().equals(menu.pos)) {
            return out;
        }
        String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
        for (StockViewPayload.Stock s : lastStock.stock()) {
            if (q.isEmpty() || s.item().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(s);
            }
        }
        out.sort((a, b) -> a.item().getHoverName().getString().compareToIgnoreCase(b.item().getHoverName().getString()));
        return out;
    }

    private List<LedgerRequest.View> queue() {
        return lastStatus == null || !lastStatus.pos().equals(menu.pos) ? List.of() : lastStatus.queue();
    }

    private int maxScroll(int n) {
        return Math.max(0, (n + COLS - 1) / COLS - ROWS);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (isHovering(GRID_X, GRID_Y, COLS * 18, ROWS * 18, x, y)) {
            scroll = Math.max(0, Math.min(maxScroll(shown().size()), scroll - (int) Math.signum(dy)));
            return true;
        }
        return super.mouseScrolled(x, y, dx, dy);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        int cell = cellAt(e.x(), e.y());
        List<StockViewPayload.Stock> list = shown();
        if (cell >= 0 && cell < list.size()) {
            selected = list.get(cell).item().copyWithCount(1);
            return true;
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (search != null && search.isFocused() && e.key() != 256) {
            search.keyPressed(e);
            return true;
        }
        return super.keyPressed(e);
    }

    private int cellAt(double x, double y) {
        int cx = (int) Math.floor((x - leftPos - GRID_X) / 18);
        int cy = (int) Math.floor((y - topPos - GRID_Y) / 18);
        if (x < leftPos + GRID_X || y < topPos + GRID_Y || cx >= COLS || cy >= ROWS) {
            return -1;
        }
        return (scroll + cy) * COLS + cx;
    }

    private static String count(long n) {
        return n >= 1_000_000 ? n / 1_000_000 + "M" : n >= 10_000 ? n / 1000 + "k" : Long.toString(n);
    }

    private static int color(LedgerRequest.Status s) {
        return switch (s) {
            case QUEUED -> 0xFF8A8A8A;
            case ASSIGNED, FETCHING -> 0xFF3F7FD0;
            case DELIVERING -> 0xFFE0A030;
            case DONE -> 0xFF3FB04F;
            case PARTIAL -> 0xFFD0C030;
            case FAILED -> 0xFFC03030;
        };
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        List<StockViewPayload.Stock> list = shown();
        scroll = Math.min(scroll, maxScroll(list.size()));
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int i = (scroll + r) * COLS + c;
                int x = leftPos + GRID_X + c * 18;
                int y = topPos + GRID_Y + r * 18;
                if (i < list.size()) {
                    StockViewPayload.Stock s = list.get(i);
                    if (ItemStack.isSameItemSameComponents(s.item(), selected)) {
                        g.fill(x, y, x + 18, y + 18, 0x804FA0FF);
                    }
                    g.fakeItem(s.item(), x + 1, y + 1);
                    String n = count(s.available());
                    g.pose().pushMatrix();
                    g.pose().translate(x + 17.5f - font.width(n) * 0.5f, y + 13);
                    g.pose().scale(0.5f, 0.5f);
                    g.text(font, Component.literal(n), 0, 0, s.available() > 0 ? 0xFFFFFFFF : 0xFFFF6060, true);
                    g.pose().popMatrix();
                }
            }
        }
        List<LedgerRequest.View> q = queue();
        for (int i = 0; i < Math.min(QUEUE_ROWS, q.size()); i++) {
            LedgerRequest.View v = q.get(q.size() - 1 - i);
            int x = leftPos + QUEUE_X;
            int y = topPos + QUEUE_Y + i * 17;
            g.fill(x, y + 15, x + 66, y + 16, color(v.status()));
            g.fakeItem(v.item(), x, y);
            g.text(font, Component.literal(v.delivered() + "/" + v.amount()), x + 18, y + 4, 0xFF303030, false);
        }
        String amountText = Integer.toString(amount);
        g.text(font, Component.literal(amountText), leftPos + 55 - font.width(amountText) / 2, topPos + 109, 0xFF303030, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        String w = lastStatus == null || !lastStatus.pos().equals(menu.pos) ? "" : lastStatus.warning();
        if (!w.isEmpty()) {
            Component t = Component.translatable("screen.autarkia.ledger.warning." + w);
            g.text(font, t, imageWidth - 8 - font.width(t), inventoryLabelY, 0xFFB01818, false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int cell = cellAt(mouseX, mouseY);
        List<StockViewPayload.Stock> list = shown();
        if (cell >= 0 && cell < list.size()) {
            StockViewPayload.Stock s = list.get(cell);
            g.setTooltipForNextFrame(font, List.of(s.item().getHoverName().getVisualOrderText(),
                Component.translatable("screen.autarkia.ledger.counts", s.available(), s.total()).getVisualOrderText()), mouseX, mouseY);
            return;
        }
        List<LedgerRequest.View> q = queue();
        for (int i = 0; i < Math.min(QUEUE_ROWS, q.size()); i++) {
            if (isHovering(QUEUE_X, QUEUE_Y + i * 17, 66, 16, mouseX, mouseY)) {
                LedgerRequest.View v = q.get(q.size() - 1 - i);
                List<FormattedCharSequence> lines = new ArrayList<>();
                lines.add(v.item().getHoverName().getVisualOrderText());
                lines.add(Component.translatable("screen.autarkia.ledger.status." + v.status().name().toLowerCase(Locale.ROOT)).getVisualOrderText());
                lines.add(Component.translatable("screen.autarkia.ledger.delivered", v.delivered(), v.amount()).getVisualOrderText());
                if (v.etaSeconds() > 0) {
                    lines.add(Component.translatable("screen.autarkia.ledger.eta", v.etaSeconds()).getVisualOrderText());
                }
                if (!v.reason().isEmpty()) {
                    lines.add(Component.translatable("screen.autarkia.ledger.reason." + v.reason()).getVisualOrderText());
                }
                g.setTooltipForNextFrame(font, lines, mouseX, mouseY);
                return;
            }
        }
    }
}

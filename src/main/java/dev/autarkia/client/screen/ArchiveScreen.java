package dev.autarkia.client.screen;

import dev.autarkia.logistics.MatterConduitBlockEntity;
import dev.autarkia.network.ArchiveRequestPayload;
import dev.autarkia.network.StockViewPayload;
import dev.autarkia.network.StockViewRequestPayload;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.storage.DomainArchiveBlockEntity;
import dev.autarkia.storage.MatterBank;
import dev.autarkia.storage.MatterBufferBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Domain archive screen (04 section 4.1): the ledger grid (search box, 7 x 2 scroll grid of the domain stock with
 * available / total counts, a message when it is empty or nothing matches) with the amount row -64 / -1 / +1 / +64
 * under it, the free MU and the last matter withdrawals (newest first, "Reform" rows among them) on the right, the 3 x 9
 * buffer, and beside it "To buffer", "To conduit" (both inactive until an item is picked), the channel picker and the
 * energy bar. The panel is 264 x 240 so it fits the 320 x 240 GUI pixels of every GUI scale.
 */
public class ArchiveScreen extends SlotScreen {
    private static final int GRID_X = 8;
    private static final int GRID_Y = 30;
    private static final int COLS = 7;
    private static final int ROWS = 2;
    private static final int LOG_X = 141;
    private static final int LOG_Y = 37;
    private static final int LOG_W = 115;
    private static final int LOG_ROWS = 5;
    private static final int ROW_Y = 70;
    private static final int SIDE_X = 176;
    private static final int SIDE_Y = 88;
    private static final int SIDE_W = 64;
    private static final int CH_Y = SIDE_Y + 28;
    private static final int BTN_H = 12;
    private static @Nullable StockViewPayload lastStock;
    private final List<Button> deliverButtons = new ArrayList<>();
    private EditBox search;
    private int scroll;
    private int amount = 64;
    private int channel;
    private ItemStack selected = ItemStack.EMPTY;
    private @Nullable StockViewPayload shownStock;
    private String shownQuery = "";
    private List<StockViewPayload.Stock> shownList = List.of();

    public ArchiveScreen(SlotMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
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
        search = new EditBox(font, leftPos + GRID_X, topPos + 17, COLS * 18, 11, Component.translatable("screen.autarkia.domain_archive.search"));
        search.setHint(Component.translatable("screen.autarkia.domain_archive.search"));
        search.setResponder(s -> scroll = 0);
        addRenderableWidget(search);
        int y = topPos + ROW_Y;
        addRenderableWidget(Button.builder(Component.literal("-64"), b -> add(-64)).bounds(leftPos + 8, y, 22, BTN_H).build());
        addRenderableWidget(Button.builder(Component.literal("-1"), b -> add(-1)).bounds(leftPos + 30, y, 16, BTN_H).build());
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> add(1)).bounds(leftPos + 76, y, 16, BTN_H).build());
        addRenderableWidget(Button.builder(Component.literal("+64"), b -> add(64)).bounds(leftPos + 92, y, 22, BTN_H).build());
        deliverButtons.clear();
        deliverButtons.add(addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.domain_archive.to_buffer"),
            b -> deliver(DomainArchiveBlockEntity.TO_BUFFER)).bounds(leftPos + SIDE_X, topPos + SIDE_Y, SIDE_W, BTN_H).build()));
        deliverButtons.add(addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.domain_archive.to_conduit"), b -> deliver(channel))
            .bounds(leftPos + SIDE_X, topPos + SIDE_Y + BTN_H + 2, SIDE_W, BTN_H).build()));
        deliverButtons.forEach(b -> b.active = !selected.isEmpty());
        ClientPacketDistributor.sendToServer(new StockViewRequestPayload(menu.pos));
    }

    private void add(int d) {
        amount = Math.max(1, Math.min(DomainArchiveBlockEntity.MAX_REQUEST, amount + d));
    }

    private void deliver(int target) {
        if (!selected.isEmpty()) {
            ClientPacketDistributor.sendToServer(new ArchiveRequestPayload(menu.pos, selected, amount, target));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        deliverButtons.forEach(b -> b.active = !selected.isEmpty());
        if (minecraft.player != null && minecraft.player.tickCount % 40 == 0) {
            ClientPacketDistributor.sendToServer(new StockViewRequestPayload(menu.pos));
        }
    }

    private @Nullable DomainArchiveBlockEntity archive() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos) instanceof DomainArchiveBlockEntity a ? a : null;
    }

    private List<StockViewPayload.Stock> shown() {
        if (lastStock == null || !lastStock.pos().equals(menu.pos)) {
            return List.of();
        }
        String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
        if (lastStock != shownStock || !q.equals(shownQuery)) {
            shownStock = lastStock;
            shownQuery = q;
            List<Map.Entry<String, StockViewPayload.Stock>> named = new ArrayList<>();
            for (StockViewPayload.Stock s : lastStock.stock()) {
                String name = s.item().getHoverName().getString();
                if (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q)) {
                    named.add(Map.entry(name, s));
                }
            }
            named.sort(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));
            shownList = named.stream().map(Map.Entry::getValue).toList();
        }
        return shownList;
    }

    private int maxScroll(int n) {
        return Math.max(0, (n + COLS - 1) / COLS - ROWS);
    }

    private int cellAt(double x, double y) {
        int cx = (int) Math.floor((x - leftPos - GRID_X) / 18);
        int cy = (int) Math.floor((y - topPos - GRID_Y) / 18);
        if (x < leftPos + GRID_X || y < topPos + GRID_Y || cx >= COLS || cy >= ROWS) {
            return -1;
        }
        return (scroll + cy) * COLS + cx;
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
        if (isHovering(SIDE_X, CH_Y, BTN_H, BTN_H, e.x(), e.y())) {
            channel = Math.floorMod(channel + (e.button() == 1 ? -1 : 1), 16);
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

    private static String count(long n) {
        return n >= 1_000_000 ? n / 1_000_000 + "M" : n >= 10_000 ? n / 1000 + "k" : Long.toString(n);
    }

    private static Component reason(String r) {
        return Component.translatableWithFallback("screen.autarkia.domain_archive.reason." + r.toLowerCase(Locale.ROOT), r);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        List<StockViewPayload.Stock> list = shown();
        scroll = Math.min(scroll, maxScroll(list.size()));
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int i = (scroll + r) * COLS + c;
                if (i >= list.size()) {
                    continue;
                }
                int x = leftPos + GRID_X + c * 18;
                int y = topPos + GRID_Y + r * 18;
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
        int cx = leftPos + SIDE_X;
        int cy = topPos + CH_Y;
        g.fill(cx, cy, cx + BTN_H, cy + BTN_H, 0xFF202020);
        g.fill(cx + 1, cy + 1, cx + BTN_H - 1, cy + BTN_H - 1, 0xFF000000 | DyeColor.byId(channel).getTextureDiffuseColor());
        String amountText = Integer.toString(amount);
        g.text(font, Component.literal(amountText), leftPos + 61 - font.width(amountText) / 2, topPos + ROW_Y + 2, 0xFF303030, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        DomainArchiveBlockEntity arc = archive();
        g.text(font, Component.translatable("screen.autarkia.domain_archive.channel", channel), SIDE_X + BTN_H + 3, CH_Y + 2, 0xFF404040, false);
        int status = menu.value(4);
        boolean active = status == DomainArchiveBlockEntity.Status.ACTIVE.ordinal();
        if (!active) {
            DomainArchiveBlockEntity.Status st = DomainArchiveBlockEntity.Status.values()[Math.clamp(status, 0, 3)];
            Component t = Component.translatable("jade.autarkia.domain_archive.status." + st.name().toLowerCase(Locale.ROOT));
            g.text(font, t, imageWidth - 8 - font.width(t), inventoryLabelY, 0xFFB01818, false);
        }
        if (lastStock != null && lastStock.pos().equals(menu.pos) && shown().isEmpty() && (active || !search.getValue().isEmpty())) {
            Component t = Component.translatable(search.getValue().isEmpty() ? "screen.autarkia.domain_archive.empty" : "screen.autarkia.domain_archive.no_match");
            wrap(g, font, t, GRID_X + 4, GRID_Y + 5, COLS * 18 - 8, 0xFF303030, 3);
        }
        if (arc == null) {
            return;
        }
        g.text(font, fit(Component.translatable("screen.autarkia.domain_archive.free"), LOG_W), LOG_X, 17, 0xFF404040, false);
        g.text(font, fit(Component.literal(MatterBufferBlockEntity.fmt(arc.viewFree()) + " MU"), LOG_W), LOG_X, 26, 0xFF2A6A8A, false);
        List<MatterBank.Withdrawal> log = arc.viewLog();
        if (log.isEmpty()) {
            g.text(font, fit(Component.translatable("screen.autarkia.domain_archive.no_log"), LOG_W), LOG_X, LOG_Y, 0xFF505050, false);
        }
        for (int i = 0; i < Math.min(LOG_ROWS, log.size()); i++) {
            MatterBank.Withdrawal w = log.get(log.size() - 1 - i);
            Component row = Component.translatable("screen.autarkia.domain_archive.row", reason(w.reason()), MatterBufferBlockEntity.fmt(w.amount()));
            g.text(font, fit(row, LOG_W), LOG_X, LOG_Y + i * 9, 0xFF5A3A3A, false);
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
                Component.translatable("screen.autarkia.domain_archive.counts", s.available(), s.total()).getVisualOrderText()), mouseX, mouseY);
            return;
        }
        if (isHovering(SIDE_X, CH_Y, BTN_H, BTN_H, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(MatterConduitBlockEntity.channelName(channel).getVisualOrderText()), mouseX, mouseY);
            return;
        }
        if (selected.isEmpty() && isHovering(SIDE_X, SIDE_Y, SIDE_W, 2 * BTN_H + 2, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("screen.autarkia.domain_archive.select").getVisualOrderText()), mouseX, mouseY);
            return;
        }
        DomainArchiveBlockEntity arc = archive();
        if (arc != null && isHovering(LOG_X - 2, 15, LOG_W + 4, 71, mouseX, mouseY)) {
            long now = minecraft.level.getGameTime();
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(Component.translatable("screen.autarkia.domain_archive.free_full", MatterBufferBlockEntity.fmt(arc.viewFree())).getVisualOrderText());
            List<MatterBank.Withdrawal> log = arc.viewLog();
            for (int i = log.size() - 1; i >= 0; i--) {
                MatterBank.Withdrawal w = log.get(i);
                lines.add(Component.translatable("screen.autarkia.domain_archive.detail", reason(w.reason()), MatterBufferBlockEntity.fmt(w.amount()),
                    Math.max(0, (now - w.time()) / 20)).getVisualOrderText());
            }
            g.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }
}

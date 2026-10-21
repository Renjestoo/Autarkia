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
 * Domain archive screen (04 section 4.1): the ledger grid (search box, scroll grid of the domain stock with available /
 * total counts), the free MU row and the last matter withdrawals (newest first, "Reform" rows among them), the amount
 * row with -64 / -1 / +1 / +64, the channel picker, "Deliver to buffer" and "Deliver to conduit", the 3 x 9 buffer and
 * the energy bar.
 */
public class ArchiveScreen extends SlotScreen {
    private static final int GRID_X = 8;
    private static final int GRID_Y = 30;
    private static final int COLS = 5;
    private static final int ROWS = 4;
    private static final int LOG_X = 103;
    private static final int LOG_Y = 37;
    private static final int LOG_ROWS = 8;
    private static final int CH_X = 104;
    private static final int ROW_Y = 105;
    private static @Nullable StockViewPayload lastStock;
    private EditBox search;
    private int scroll;
    private int amount = 64;
    private int channel;
    private ItemStack selected = ItemStack.EMPTY;

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
        search = new EditBox(font, leftPos + GRID_X, topPos + 17, 91, 11, Component.translatable("screen.autarkia.domain_archive.search"));
        search.setHint(Component.translatable("screen.autarkia.domain_archive.search"));
        search.setResponder(s -> scroll = 0);
        addRenderableWidget(search);
        int y = topPos + ROW_Y;
        addRenderableWidget(Button.builder(Component.literal("-64"), b -> add(-64)).bounds(leftPos + 8, y, 22, 14).build());
        addRenderableWidget(Button.builder(Component.literal("-1"), b -> add(-1)).bounds(leftPos + 30, y, 14, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> add(1)).bounds(leftPos + 66, y, 14, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+64"), b -> add(64)).bounds(leftPos + 80, y, 22, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.domain_archive.to_buffer"), b -> deliver(DomainArchiveBlockEntity.TO_BUFFER))
            .bounds(leftPos + 8, y + 16, 80, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.autarkia.domain_archive.to_conduit"), b -> deliver(channel))
            .bounds(leftPos + 90, y + 16, 80, 14).build());
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
        if (minecraft.player != null && minecraft.player.tickCount % 40 == 0) {
            ClientPacketDistributor.sendToServer(new StockViewRequestPayload(menu.pos));
        }
    }

    private @Nullable DomainArchiveBlockEntity archive() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos) instanceof DomainArchiveBlockEntity a ? a : null;
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
        if (isHovering(CH_X, ROW_Y, 14, 14, e.x(), e.y())) {
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
        int cx = leftPos + CH_X;
        int cy = topPos + ROW_Y;
        g.fill(cx, cy, cx + 14, cy + 14, 0xFF202020);
        g.fill(cx + 1, cy + 1, cx + 13, cy + 13, 0xFF000000 | DyeColor.byId(channel).getTextureDiffuseColor());
        String amountText = Integer.toString(amount);
        g.text(font, Component.literal(amountText), leftPos + 55 - font.width(amountText) / 2, topPos + ROW_Y + 3, 0xFF303030, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        DomainArchiveBlockEntity arc = archive();
        g.text(font, Component.translatable("screen.autarkia.domain_archive.channel", channel), CH_X + 17, ROW_Y + 3, 0xFF404040, false);
        int status = menu.value(4);
        if (status != DomainArchiveBlockEntity.Status.ACTIVE.ordinal()) {
            DomainArchiveBlockEntity.Status st = DomainArchiveBlockEntity.Status.values()[Math.clamp(status, 0, 3)];
            Component t = Component.translatable("jade.autarkia.domain_archive.status." + st.name().toLowerCase(Locale.ROOT));
            g.text(font, t, imageWidth - 8 - font.width(t), inventoryLabelY, 0xFFB01818, false);
        }
        if (arc == null) {
            return;
        }
        small(g, Component.translatable("screen.autarkia.domain_archive.free"), LOG_X, 17, 0xFF404040);
        small(g, Component.literal(MatterBufferBlockEntity.fmt(arc.viewFree()) + " MU"), LOG_X, 25, 0xFF2A6A8A);
        List<MatterBank.Withdrawal> log = arc.viewLog();
        for (int i = 0; i < Math.min(LOG_ROWS, log.size()); i++) {
            MatterBank.Withdrawal w = log.get(log.size() - 1 - i);
            small(g, Component.translatable("screen.autarkia.domain_archive.row", reason(w.reason()), MatterBufferBlockEntity.fmt(w.amount())), LOG_X,
                LOG_Y + i * 8, 0xFF5A3A3A);
        }
    }

    private void small(GuiGraphicsExtractor g, Component text, int x, int y, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.7f, 0.7f);
        g.text(font, Component.literal(font.plainSubstrByWidth(text.getString(), 74)), 0, 0, color, false);
        g.pose().popMatrix();
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
        if (isHovering(CH_X, ROW_Y, 14, 14, mouseX, mouseY)) {
            g.setTooltipForNextFrame(font, List.of(MatterConduitBlockEntity.channelName(channel).getVisualOrderText()), mouseX, mouseY);
            return;
        }
        DomainArchiveBlockEntity arc = archive();
        if (arc != null && isHovering(101, 15, 55, 88, mouseX, mouseY)) {
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

package dev.autarkia.network;

import dev.autarkia.Autarkia;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * S2C stock view (04 sections 2.5 and 2.6): the warehouse stock of a terminal's controller, each item with its available
 * and total amount, and one page of the linked stock view over all sites (the sites, 256 rows of site, item and count,
 * the recent transfer orders). A depot's view carries only the sites part.
 */
public record StockViewPayload(BlockPos pos, List<Stock> stock, Sites sites) implements CustomPacketPayload {
    public static final Type<StockViewPayload> TYPE = new Type<>(Autarkia.id("stock_view"));

    /** One stock line. */
    public record Stock(ItemStack item, long available, long total) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Stock> CODEC = StreamCodec.composite(ItemStack.STREAM_CODEC, Stock::item,
            ByteBufCodecs.VAR_LONG, Stock::available, ByteBufCodecs.VAR_LONG, Stock::total, Stock::new);
    }

    /**
     * One site: key ({@code main} or dimension|pos), player-given name (empty for the default name), main flag, position,
     * dimension id, slots used and total, stale flag, ticks since its last update.
     */
    public record Site(String key, String name, boolean main, BlockPos pos, String dimension, int used, int total, boolean stale, long age) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Site> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Site::key,
            ByteBufCodecs.STRING_UTF8, Site::name, ByteBufCodecs.BOOL, Site::main, BlockPos.STREAM_CODEC, Site::pos, ByteBufCodecs.STRING_UTF8,
            Site::dimension, ByteBufCodecs.VAR_INT, Site::used, ByteBufCodecs.VAR_INT, Site::total, ByteBufCodecs.BOOL, Site::stale,
            ByteBufCodecs.VAR_LONG, Site::age, Site::new);
    }

    /** One row of the linked view: index into the site list, item, count at that site. */
    public record Row(int site, ItemStack item, long count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Row> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Row::site,
            ItemStack.STREAM_CODEC, Row::item, ByteBufCodecs.VAR_LONG, Row::count, Row::new);
    }

    /** One transfer order: id, item, ordered and moved counts, source site key, status name, reason key. */
    public record Order(int id, ItemStack item, int count, int moved, String from, String status, String reason) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Order> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Order::id,
            ItemStack.STREAM_CODEC, Order::item, ByteBufCodecs.VAR_INT, Order::count, ByteBufCodecs.VAR_INT, Order::moved, ByteBufCodecs.STRING_UTF8,
            Order::from, ByteBufCodecs.STRING_UTF8, Order::status, ByteBufCodecs.STRING_UTF8, Order::reason, Order::new);
    }

    /** The linked view part: the sites, one page of rows, the page index and count, and the recent orders. */
    public record Sites(List<Site> sites, List<Row> rows, int page, int pages, List<Order> orders) {
        public static final Sites EMPTY = new Sites(List.of(), List.of(), 0, 1, List.of());
        public static final StreamCodec<RegistryFriendlyByteBuf, Sites> CODEC = StreamCodec.composite(Site.CODEC.apply(ByteBufCodecs.list(1024)),
            Sites::sites, Row.CODEC.apply(ByteBufCodecs.list(256)), Sites::rows, ByteBufCodecs.VAR_INT, Sites::page, ByteBufCodecs.VAR_INT,
            Sites::pages, Order.CODEC.apply(ByteBufCodecs.list(64)), Sites::orders, Sites::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, StockViewPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
        StockViewPayload::pos, Stock.CODEC.apply(ByteBufCodecs.list(8192)), StockViewPayload::stock, Sites.CODEC, StockViewPayload::sites,
        StockViewPayload::new);

    /** Create a stock view without the linked view part. */
    public StockViewPayload(BlockPos pos, List<Stock> stock) {
        this(pos, stock, Sites.EMPTY);
    }

    /** Return this view with the linked view part. */
    public StockViewPayload withSites(Sites s) {
        return new StockViewPayload(pos, stock, s);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

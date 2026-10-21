package dev.autarkia.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.Autarkia;
import dev.autarkia.network.StockViewPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Domain stock per dimension (04 section 2.6), file {@code <dimension>/data/autarkia/domain_stock.dat}. Warehouse
 * controllers, tally boards and depots push their stock here (at most once per {@code stockSyncIntervalTicks}); the
 * linked stock view merges the data of every level of the server into sites: the main site (every source that is not
 * an outpost depot) and one site per outpost depot. A source is stale when its last push is older than 1,200 ticks and
 * its chunk is not loaded. A depot belongs to the nearest registered {@code outpost_core} within 16 blocks; outpost
 * cores register themselves with {@link #addOutpost} and leave with {@link #removeOutpost}. Remote site requests become
 * {@link TransferOrder}s, saved here with the source depot.
 */
public final class DomainStockData extends SavedData {
    public static final int PAGE = 256;
    public static final int STALE_TICKS = 1200;
    public static final int OUTPOST_RADIUS = 16;
    public static final int MAX_REQUEST = 1728;
    public static final String MAIN = "main";

    /** Kind of a stock source block. */
    public enum Kind { WAREHOUSE, TALLY, DEPOT }

    /** One item count. */
    record Entry(ItemResource item, long count) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(ItemResource.CODEC.fieldOf("item").forGetter(Entry::item),
            Codec.LONG.fieldOf("count").forGetter(Entry::count)).apply(i, Entry::new));
    }

    /** The last pushed stock of one source block. */
    public record Source(Kind kind, BlockPos pos, Optional<BlockPos> outpost, String name, Map<ItemResource, Long> stock, int used, int total,
                         long updated) {
        static final Codec<Source> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.xmap(Kind::valueOf, Kind::name).fieldOf("kind").forGetter(Source::kind), BlockPos.CODEC.fieldOf("pos").forGetter(Source::pos),
            BlockPos.CODEC.optionalFieldOf("outpost").forGetter(Source::outpost), Codec.STRING.optionalFieldOf("name", "").forGetter(Source::name),
            Entry.CODEC.listOf().fieldOf("stock").forGetter(s -> s.stock.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList()),
            Codec.INT.fieldOf("used").forGetter(Source::used), Codec.INT.fieldOf("total").forGetter(Source::total),
            Codec.LONG.fieldOf("updated").forGetter(Source::updated)
        ).apply(i, (k, p, o, n, list, u, t, up) -> {
            Map<ItemResource, Long> m = new LinkedHashMap<>();
            list.forEach(e -> m.merge(e.item(), e.count(), Long::sum));
            return new Source(k, p, o, n, m, u, t, up);
        }));
    }

    /** A registered outpost core and its name. */
    public record Outpost(BlockPos core, String name) {
        static final Codec<Outpost> CODEC = RecordCodecBuilder.create(i -> i.group(BlockPos.CODEC.fieldOf("core").forGetter(Outpost::core),
            Codec.STRING.optionalFieldOf("name", "").forGetter(Outpost::name)).apply(i, Outpost::new));
    }

    /** One site of the linked view: the merged main site or one outpost depot. */
    public record SiteView(String key, String name, boolean main, BlockPos pos, ResourceKey<Level> dimension, Map<ItemResource, Long> stock, int used,
                           int total, boolean stale, long age) {
        /** Return used slots / total slots (0 without slots). */
        public float fillRatio() {
            return total == 0 ? 0 : used / (float) total;
        }
    }

    public static final Codec<DomainStockData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Source.CODEC.listOf().optionalFieldOf("sources", List.of()).forGetter(d -> List.copyOf(d.sources.values())),
        Outpost.CODEC.listOf().optionalFieldOf("outposts", List.of()).forGetter(d -> List.copyOf(d.outposts.values())),
        TransferOrder.CODEC.listOf().optionalFieldOf("orders", List.of()).forGetter(d -> List.copyOf(d.orders)),
        Codec.INT.optionalFieldOf("nextOrder", 1).forGetter(d -> d.nextOrder)
    ).apply(i, DomainStockData::new));

    public static final SavedDataType<DomainStockData> TYPE = new SavedDataType<>(Autarkia.id("domain_stock"), DomainStockData::new, CODEC);

    private final Map<Long, Source> sources = new LinkedHashMap<>();
    private final Map<Long, Outpost> outposts = new LinkedHashMap<>();
    private final List<TransferOrder> orders = new ArrayList<>();
    private int nextOrder = 1;

    public DomainStockData() {
    }

    private DomainStockData(List<Source> sources, List<Outpost> outposts, List<TransferOrder> orders, int nextOrder) {
        sources.forEach(s -> this.sources.put(s.pos().asLong(), s));
        outposts.forEach(o -> this.outposts.put(o.core().asLong(), o));
        this.orders.addAll(orders);
        this.nextOrder = nextOrder;
    }

    /** Return the domain stock data of a level. */
    public static DomainStockData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Record the stock of a source block; outpost is the owning outpost core of a depot, or null for the main site. */
    public void push(Kind kind, BlockPos pos, @Nullable BlockPos outpost, String name, Map<ItemResource, Long> stock, int used, int total, long now) {
        sources.put(pos.asLong(), new Source(kind, pos.immutable(), Optional.ofNullable(outpost).map(BlockPos::immutable), name, new LinkedHashMap<>(stock),
            used, total, now));
        setDirty();
    }

    /** Forget a source block (it was broken). */
    public void remove(BlockPos pos) {
        if (sources.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    /** Return the last pushed stock of a source block, or null. */
    public @Nullable Source source(BlockPos pos) {
        return sources.get(pos.asLong());
    }

    /** Register an outpost core (the outpost core calls this when it is placed or loaded). */
    public void addOutpost(BlockPos core, String name) {
        outposts.put(core.asLong(), new Outpost(core.immutable(), name));
        setDirty();
    }

    /** Remove an outpost core (it was broken). */
    public void removeOutpost(BlockPos core) {
        if (outposts.remove(core.asLong()) != null) {
            setDirty();
        }
    }

    /** Return the nearest registered outpost core within 16 blocks of pos, or null (then pos belongs to the main site). */
    public @Nullable Outpost outpostNear(BlockPos pos) {
        Outpost best = null;
        for (Outpost o : outposts.values()) {
            double d = o.core().distSqr(pos);
            if (d <= OUTPOST_RADIUS * OUTPOST_RADIUS && (best == null || d < best.core().distSqr(pos))) {
                best = o;
            }
        }
        return best;
    }

    /** Return the site key of an outpost depot. */
    public static String key(ResourceKey<Level> dim, BlockPos pos) {
        return dim.identifier() + "|" + pos.asLong();
    }

    private static boolean matches(Kind kind, @Nullable BlockEntity be) {
        return switch (kind) {
            case WAREHOUSE -> be instanceof WarehouseControllerBlockEntity;
            case TALLY -> be instanceof TallyBoardBlockEntity;
            case DEPOT -> be instanceof DepotBlockEntity;
        };
    }

    private List<Source> live(ServerLevel level) {
        List<Source> out = new ArrayList<>();
        for (Source s : List.copyOf(sources.values())) {
            if (level.isLoaded(s.pos()) && !matches(s.kind(), level.getBlockEntity(s.pos()))) {
                sources.remove(s.pos().asLong());
                setDirty();
            } else {
                out.add(s);
            }
        }
        return out;
    }

    /** Return every site of the server: the merged main site first (when any main source exists), then the outpost depots. */
    public static List<SiteView> sites(MinecraftServer server) {
        Map<ItemResource, Long> main = new LinkedHashMap<>();
        int used = 0;
        int total = 0;
        boolean anyMain = false;
        boolean mainStale = true;
        long mainAge = Long.MAX_VALUE;
        BlockPos mainPos = BlockPos.ZERO;
        ResourceKey<Level> mainDim = Level.OVERWORLD;
        List<SiteView> outposts = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            long now = level.getGameTime();
            for (Source s : get(level).live(level)) {
                boolean stale = now - s.updated() > STALE_TICKS && !level.isLoaded(s.pos());
                long age = Math.max(0, now - s.updated());
                if (s.outpost().isPresent()) {
                    outposts.add(new SiteView(key(level.dimension(), s.pos()), s.name(), false, s.pos(), level.dimension(), s.stock(), s.used(), s.total(),
                        stale, age));
                    continue;
                }
                if (!anyMain) {
                    mainPos = s.pos();
                    mainDim = level.dimension();
                }
                anyMain = true;
                mainStale &= stale;
                mainAge = Math.min(mainAge, age);
                used += s.used();
                total += s.total();
                s.stock().forEach((k, v) -> main.merge(k, v, Long::sum));
            }
        }
        outposts.sort(Comparator.comparing(SiteView::key));
        List<SiteView> out = new ArrayList<>();
        if (anyMain) {
            out.add(new SiteView(MAIN, "", true, mainPos, mainDim, main, used, total, mainStale, mainAge));
        }
        out.addAll(outposts);
        return out;
    }

    /** Return one page of the linked stock view (256 rows of site, item, count), the sites and the recent transfer orders. */
    public static StockViewPayload.Sites view(MinecraftServer server, int page) {
        List<SiteView> sites = sites(server);
        List<StockViewPayload.Site> list = new ArrayList<>();
        List<StockViewPayload.Row> rows = new ArrayList<>();
        for (int i = 0; i < sites.size(); i++) {
            SiteView s = sites.get(i);
            list.add(new StockViewPayload.Site(s.key(), s.name(), s.main(), s.pos(), s.dimension().identifier().toString(), s.used(), s.total(), s.stale(),
                s.age()));
            int idx = i;
            s.stock().entrySet().stream().filter(e -> e.getValue() > 0)
                .sorted(Comparator.comparing((Map.Entry<ItemResource, Long> e) -> BuiltInRegistries.ITEM.getKey(e.getKey().getItem()).toString()))
                .forEach(e -> rows.add(new StockViewPayload.Row(idx, e.getKey().toStack(1), e.getValue())));
        }
        int pages = Math.max(1, (rows.size() + PAGE - 1) / PAGE);
        int p = Math.clamp(page, 0, pages - 1);
        List<StockViewPayload.Row> shown = List.copyOf(rows.subList(p * PAGE, Math.min(rows.size(), (p + 1) * PAGE)));
        List<TransferOrder> all = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            all.addAll(get(level).orders);
        }
        all.sort(Comparator.comparingLong((TransferOrder o) -> -o.created));
        List<StockViewPayload.Order> orders = all.stream().limit(16).map(o -> new StockViewPayload.Order(o.id, o.item, o.count, o.moved(), o.fromSite,
            o.status().name(), o.reason())).toList();
        return new StockViewPayload.Sites(list, shown, p, pages, orders);
    }

    /** Return the slot fill ratio of every outpost depot site (sensor {@code site.fill_ratio}), keyed by site key. */
    public static Map<String, Float> fillRatios(MinecraftServer server) {
        Map<String, Float> out = new LinkedHashMap<>();
        for (SiteView s : sites(server)) {
            if (!s.main()) {
                out.put(s.key(), s.fillRatio());
            }
        }
        return out;
    }

    /** Return the keys of the stale sites (sensor {@code site.stale}). */
    public static List<String> staleSites(MinecraftServer server) {
        return sites(server).stream().filter(SiteView::stale).map(SiteView::key).toList();
    }

    /**
     * Create a transfer order of up to amount (1 to 1,728, clamped to the site's last known stock) of item from the outpost
     * depot site with the key to the main site; return the refusal message key, or null when the order is queued.
     */
    public static @Nullable String request(MinecraftServer server, String key, ItemStack item, int amount) {
        if (item.isEmpty() || MAIN.equals(key)) {
            return "message.autarkia.sites.not_remote";
        }
        for (ServerLevel level : server.getAllLevels()) {
            DomainStockData d = get(level);
            for (Source s : d.sources.values()) {
                if (s.kind() != Kind.DEPOT || !key(level.dimension(), s.pos()).equals(key)) {
                    continue;
                }
                long have = s.stock().getOrDefault(ItemResource.of(item), 0L);
                int n = (int) Math.min(Math.min(MAX_REQUEST, Math.max(1, amount)), have);
                if (n <= 0) {
                    return "message.autarkia.sites.none_available";
                }
                d.orders.add(TransferOrder.create(d.nextOrder++, item, n, s.pos(), key, MAIN, level.getGameTime()));
                while (d.orders.size() > 32 && d.orders.stream().anyMatch(o -> !o.status().open())) {
                    d.orders.remove(d.orders.stream().filter(o -> !o.status().open()).findFirst().orElseThrow());
                }
                d.setDirty();
                Autarkia.LOG.info("[sites] transfer order {}: {} x {} from {}", d.nextOrder - 1, n, BuiltInRegistries.ITEM.getKey(item.getItem()), key);
                return null;
            }
        }
        return "message.autarkia.sites.unknown";
    }

    /** Return the open transfer orders whose source is the depot at pos. */
    public List<TransferOrder> openOrdersFrom(BlockPos pos) {
        return orders.stream().filter(o -> o.status().open() && o.source.equals(pos)).toList();
    }

    /** Return every transfer order of this dimension, oldest first. */
    public List<TransferOrder> orders() {
        return List.copyOf(orders);
    }

    /** Update an order's progress after the depot moved items or could not; marks the data dirty. */
    public void update(TransferOrder o, int movedNow, TransferOrder.Status status, String reason) {
        o.moved += movedNow;
        o.status = status;
        o.reason = reason;
        setDirty();
    }
}

package dev.autarkia.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.MatterValuesPayload;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.jspecify.annotations.Nullable;

/**
 * The one matter value table of 03 section 6, read from {@code data/autarkia/matter_values.json} of every data pack
 * (a later pack replaces single keys of an earlier one). Sections: {@code items} (item ids), {@code item_tags} and
 * {@code block_tags} (block items whose block is in the tag). An item id wins over the tags, item tags win over block
 * tags, and the first matching tag in file order wins. A value is a number of MU or a rule: {@code {"of": item, "mul": n,
 * "div": n}} (the MU of another item times mul divided by div: nuggets, storage blocks, processing forms, ore blocks) or
 * {@code {"raw": ingot}} (a raw ore: the ingot MU times {@code matter.rawOreMultiplierT5} at domain tier 5 and below,
 * {@code matter.rawOreMultiplierT6} at tier 6 and above, G19). Entries that name an item which is not registered are
 * skipped and logged once per reload; a malformed entry or a rule cycle fails the reload. The server sends the merged
 * file to every client with the data pack sync (load tooltip).
 */
public final class MatterValues {
    public static final TagKey<Item> DISSOLVABLE = TagKey.create(Registries.ITEM, Autarkia.id("dissolvable"));
    public static final TagKey<Item> BLACKLIST = TagKey.create(Registries.ITEM, Autarkia.id("matter_blacklist"));
    public static final TagKey<Block> BULK = TagKey.create(Registries.BLOCK, Autarkia.id("matter_bulk"));
    private static final Identifier FILE = Autarkia.id("matter_values.json");
    private static final int MAX_DEPTH = 8;
    private static final AtomicInteger VERSION = new AtomicInteger();

    private sealed interface Rule permits Fixed, Of, Raw {
    }

    private record Fixed(long mu) implements Rule {
    }

    private record Of(Item item, long mul, long div) implements Rule {
    }

    private record Raw(Item ingot) implements Rule {
    }

    private record Table(String json, Map<Item, Rule> items, List<Map.Entry<TagKey<Item>, Rule>> itemTags,
                         List<Map.Entry<TagKey<Block>, Rule>> blockTags) {
    }

    private static volatile Table table = new Table("{}", Map.of(), List.of(), List.of());

    private MatterValues() {
    }

    /** Register the server reload listener and the client sync on data pack sync. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) -> e.addListener(Autarkia.id("matter_values"), new Listener()));
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> {
            MatterValuesPayload p = new MatterValuesPayload(table.json());
            e.getRelevantPlayers().forEach(sp -> AutarkiaNetwork.toPlayer(sp, p));
        });
    }

    /** Replace the table from the merged JSON text the server sent (client side). */
    public static void applyClient(String json) {
        table = parse(json);
    }

    /** Return a number that changes whenever the table is replaced (caches compare it). */
    public static int version() {
        return VERSION.get();
    }

    /** Return the raw ore multiplier at a domain tier: T6 config value at tier 6 and above, else the T5 value. */
    public static int rawMultiplier(int domainTier) {
        return domainTier >= 6 ? AutarkiaConfig.MATTER_RAW_ORE_MULTIPLIER_T6.get() : AutarkiaConfig.MATTER_RAW_ORE_MULTIPLIER_T5.get();
    }

    /** Return the MU of one item at the domain tier (5 or 6 for raw ores; other tiers use 5), 0 when it has no value. */
    public static long of(ItemStack stack, int domainTier) {
        if (stack.isEmpty()) {
            return 0;
        }
        return value(table, stack.getItem(), domainTier, 0);
    }

    /** Return true when the item is accepted by a matter buffer (has a value, no non-empty container, not blacklisted). */
    public static boolean accepted(ItemStack stack, int domainTier) {
        if (stack.isEmpty() || stack.is(BLACKLIST) || stack.getItem() instanceof SpawnEggItem) {
            return false;
        }
        ItemContainerContents c = stack.get(DataComponents.CONTAINER);
        if (c != null && c.nonEmptyItemCopyStream().findAny().isPresent()) {
            return false;
        }
        return of(stack, domainTier) > 0;
    }

    private static long value(Table t, Item item, int tier, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException("Matter value rule cycle at " + BuiltInRegistries.ITEM.getKey(item));
        }
        Rule r = rule(t, item);
        return switch (r) {
            case null -> 0;
            case Fixed f -> f.mu();
            case Of o -> value(t, o.item(), tier, depth + 1) * o.mul() / o.div();
            case Raw raw -> value(t, raw.ingot(), tier, depth + 1) * rawMultiplier(tier);
        };
    }

    private static @Nullable Rule rule(Table t, Item item) {
        Rule r = t.items().get(item);
        if (r != null) {
            return r;
        }
        ItemStack probe = new ItemStack(item);
        for (Map.Entry<TagKey<Item>, Rule> e : t.itemTags()) {
            if (probe.is(e.getKey())) {
                return e.getValue();
            }
        }
        if (item instanceof BlockItem bi) {
            for (Map.Entry<TagKey<Block>, Rule> e : t.blockTags()) {
                if (bi.getBlock().defaultBlockState().is(e.getKey())) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    private static Table parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        List<String> skipped = new ArrayList<>();
        Map<Item, Rule> items = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : section(root, "items").entrySet()) {
            Item item = item(e.getKey());
            Rule r = rule(e.getKey(), e.getValue(), skipped);
            if (item == null) {
                skipped.add(e.getKey());
            } else if (r != null) {
                items.put(item, r);
            }
        }
        List<Map.Entry<TagKey<Item>, Rule>> itemTags = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : section(root, "item_tags").entrySet()) {
            Rule r = rule(e.getKey(), e.getValue(), skipped);
            if (r != null) {
                itemTags.add(Map.entry(TagKey.create(Registries.ITEM, Identifier.parse(e.getKey())), r));
            }
        }
        List<Map.Entry<TagKey<Block>, Rule>> blockTags = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : section(root, "block_tags").entrySet()) {
            Rule r = rule(e.getKey(), e.getValue(), skipped);
            if (r != null) {
                blockTags.add(Map.entry(TagKey.create(Registries.BLOCK, Identifier.parse(e.getKey())), r));
            }
        }
        if (!skipped.isEmpty()) {
            Autarkia.LOG.info("[matter] skipped {} matter value entries that name unregistered items: {}", skipped.size(), new TreeSet<>(skipped));
        }
        VERSION.incrementAndGet();
        return new Table(json, Map.copyOf(items), List.copyOf(itemTags), List.copyOf(blockTags));
    }

    private static JsonObject section(JsonObject root, String name) {
        return root.has(name) ? root.getAsJsonObject(name) : new JsonObject();
    }

    private static @Nullable Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(null);
    }

    private static @Nullable Rule rule(String key, JsonElement v, List<String> skipped) {
        if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) {
            long mu = v.getAsLong();
            if (mu <= 0) {
                throw new IllegalStateException("Matter value " + key + ": MU must be positive, got " + mu);
            }
            return new Fixed(mu);
        }
        if (!v.isJsonObject()) {
            throw new IllegalStateException("Matter value " + key + ": expected a number or an object, got " + v);
        }
        JsonObject o = v.getAsJsonObject();
        String ref = o.has("of") ? o.get("of").getAsString() : o.has("raw") ? o.get("raw").getAsString() : null;
        if (ref == null) {
            throw new IllegalStateException("Matter value " + key + ": an object needs \"of\" or \"raw\", got " + o);
        }
        Item target = item(ref);
        if (target == null) {
            skipped.add(key + " (" + ref + ")");
            return null;
        }
        if (o.has("raw")) {
            return new Raw(target);
        }
        long mul = o.has("mul") ? o.get("mul").getAsLong() : 1;
        long div = o.has("div") ? o.get("div").getAsLong() : 1;
        if (mul <= 0 || div <= 0) {
            throw new IllegalStateException("Matter value " + key + ": mul and div must be positive, got " + o);
        }
        return new Of(target, mul, div);
    }

    private static final class Listener extends SimplePreparableReloadListener<String> {
        @Override
        protected String prepare(ResourceManager manager, ProfilerFiller profiler) {
            JsonObject merged = new JsonObject();
            for (String s : List.of("items", "item_tags", "block_tags")) {
                merged.add(s, new JsonObject());
            }
            for (Resource res : manager.getResourceStack(FILE)) {
                try (Reader r = res.openAsReader()) {
                    JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                    for (String s : List.of("items", "item_tags", "block_tags")) {
                        if (o.has(s)) {
                            o.getAsJsonObject(s).entrySet().forEach(en -> merged.getAsJsonObject(s).add(en.getKey(), en.getValue()));
                        }
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException("Cannot read " + FILE + " from pack " + res.sourcePackId(), ex);
                }
            }
            return merged.toString();
        }

        @Override
        protected void apply(String json, ResourceManager manager, ProfilerFiller profiler) {
            table = parse(json);
            Autarkia.LOG.info("[matter] loaded {} item values, {} item tag values, {} block tag values", table.items().size(), table.itemTags().size(),
                table.blockTags().size());
        }
    }
}

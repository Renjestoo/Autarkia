package dev.autarkia.integration.yacl;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import dev.autarkia.config.AutarkiaClientConfig;
import dev.autarkia.config.AutarkiaConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.LongFieldControllerBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * YACL config screen (10 section 6) built from the {@link AutarkiaConfig} and {@link AutarkiaClientConfig} specs: one
 * category per domain, one group per config section, every value bound to its {@code ModConfigSpec} value so NeoForge
 * still owns the files and the server sync. Synced values are read only under the same rules as the NeoForge
 * {@code ConfigurationScreen} (not loaded, connected to a remote server, world open to LAN). Load this class only when
 * {@link #MOD_ID} is loaded.
 */
public final class YaclScreen {
    public static final String MOD_ID = "yet_another_config_lib_v3";
    private static final String KEY = "autarkia.configuration.";
    private static final List<String> CATEGORIES = List.of("power", "materials", "logistics", "colony", "workers", "equipment", "client");
    private static final Map<String, String> DOMAINS = Map.ofEntries(
        Map.entry("power", "power"), Map.entry("materials", "materials"), Map.entry("logistics", "logistics"),
        Map.entry("colony", "colony"), Map.entry("memory", "colony"), Map.entry("pattern", "colony"), Map.entry("outpost", "colony"),
        Map.entry("replicator", "colony"), Map.entry("nanite", "colony"), Map.entry("seed", "colony"),
        Map.entry("restoration", "colony"), Map.entry("absorb", "colony"), Map.entry("workers", "workers"),
        Map.entry("aegis", "equipment"), Map.entry("reform", "equipment"), Map.entry("bastion", "equipment"),
        Map.entry("tools", "equipment"), Map.entry("weapons", "equipment"), Map.entry("forge", "equipment"),
        Map.entry("render", "client"), Map.entry("hud", "client"), Map.entry("fx", "client"));
    private static final Set<ModConfigSpec.ConfigValue<?>> KILL_SWITCHES = Set.of(AutarkiaConfig.COLONY_ENABLED,
        AutarkiaConfig.OUTPOST_CHUNK_TICKETS, AutarkiaConfig.REPLICATOR_ENABLED, AutarkiaConfig.NANITE_ENABLED, AutarkiaConfig.SEED_ENABLED,
        AutarkiaConfig.RESTORATION_ENABLED, AutarkiaConfig.ABSORB_ENABLED, AutarkiaConfig.WORKER_BLOCK_EDITS);

    private YaclScreen() {
    }

    /**
     * Build the screen; Save writes both spec files (the synced one only when it is editable).
     *
     * @throws NullPointerException when a config section has no category in the domain map
     */
    public static Screen create(Screen parent) {
        Component lock = serverLock(Minecraft.getInstance());
        Map<String, ConfigCategory.Builder> categories = new LinkedHashMap<>();
        for (String id : CATEGORIES) {
            ConfigCategory.Builder category = ConfigCategory.createBuilder().name(Component.translatable(KEY + "category." + id));
            if (lock != null && !id.equals("client")) {
                category.option(LabelOption.create(lock));
            }
            categories.put(id, category);
        }
        if (AutarkiaConfig.SPEC.isLoaded()) {
            sections(AutarkiaConfig.SPEC, categories, lock == null);
        }
        sections(AutarkiaClientConfig.SPEC, categories, true);
        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable(KEY + "title"))
            .categories(categories.values().stream().map(ConfigCategory.Builder::build).toList())
            .save(() -> {
                if (lock == null) {
                    AutarkiaConfig.SPEC.save();
                }
                AutarkiaClientConfig.SPEC.save();
            })
            .build()
            .generateScreen(parent);
    }

    private static Component serverLock(Minecraft mc) {
        if (!AutarkiaConfig.SPEC.isLoaded()) {
            return ConfigurationScreen.TOOLTIP_CANNOT_EDIT_NOT_LOADED;
        }
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer().isPublished()) {
            return ConfigurationScreen.TOOLTIP_CANNOT_EDIT_THIS_WHILE_OPEN_TO_LAN;
        }
        return mc.getCurrentServer() != null ? ConfigurationScreen.TOOLTIP_CANNOT_EDIT_THIS_WHILE_ONLINE : null;
    }

    private static void sections(ModConfigSpec spec, Map<String, ConfigCategory.Builder> categories, boolean editable) {
        spec.getValues().valueMap().forEach((section, node) -> {
            String id = Objects.requireNonNull(DOMAINS.get(section), () -> "No YACL category for config section " + section);
            List<Option<?>> options = new ArrayList<>();
            collect(node, options, editable);
            ConfigCategory.Builder category = categories.get(id);
            if (section.equals(id)) {
                category.options(options);
            } else {
                category.group(OptionGroup.createBuilder().name(Component.translatable(KEY + "section." + section)).options(options).build());
            }
        });
    }

    private static void collect(Object node, List<Option<?>> out, boolean editable) {
        if (node instanceof ModConfigSpec.ConfigValue<?> value) {
            out.add(option(value, editable));
        } else {
            ((UnmodifiableConfig) node).valueMap().values().forEach(child -> collect(child, out, editable));
        }
    }

    private static Option<?> option(ModConfigSpec.ConfigValue<?> value, boolean editable) {
        return switch (value) {
            case ModConfigSpec.BooleanValue v -> build(v, null, editable, o -> BooleanControllerBuilder.create(o).coloured(true));
            case ModConfigSpec.IntValue v -> ranged(v, editable, (o, r) -> IntegerFieldControllerBuilder.create(o).range(r.getMin(), r.getMax()));
            case ModConfigSpec.LongValue v -> ranged(v, editable, (o, r) -> LongFieldControllerBuilder.create(o).range(r.getMin(), r.getMax()));
            case ModConfigSpec.DoubleValue v -> ranged(v, editable, (o, r) -> DoubleFieldControllerBuilder.create(o).range(r.getMin(), r.getMax()));
            default -> throw new IllegalStateException("No YACL controller for config value " + key(value));
        };
    }

    private static <T extends Comparable<? super T>> Option<T> ranged(ModConfigSpec.ConfigValue<T> value, boolean editable,
                                                                       BiFunction<Option<T>, ModConfigSpec.Range<T>, ControllerBuilder<T>> controller) {
        ModConfigSpec.Range<T> range = value.getSpec().getRange();
        return build(value, range, editable, o -> controller.apply(o, range));
    }

    private static <T> Option<T> build(ModConfigSpec.ConfigValue<T> value, ModConfigSpec.Range<?> range, boolean editable,
                                       Function<Option<T>, ControllerBuilder<T>> controller) {
        MutableComponent text = Component.translatable(key(value) + ".tooltip");
        if (KILL_SWITCHES.contains(value)) {
            text = text.withStyle(ChatFormatting.RED);
        }
        MutableComponent limits = range == null
            ? Component.translatable(KEY + "yacl.default", value.getDefault())
            : Component.translatable(KEY + "yacl.range", range.getMin(), range.getMax(), value.getDefault());
        return Option.<T>createBuilder()
            .name(Component.translatable(key(value)))
            .description(OptionDescription.of(text, limits.withStyle(ChatFormatting.GRAY)))
            .binding(value.getDefault(), value::get, value::set)
            .controller(controller)
            .available(editable)
            .build();
    }

    private static String key(ModConfigSpec.ConfigValue<?> value) {
        return KEY + String.join(".", value.getPath());
    }
}

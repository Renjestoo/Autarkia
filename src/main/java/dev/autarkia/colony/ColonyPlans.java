package dev.autarkia.colony;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.autarkia.Autarkia;
import dev.autarkia.energy.CableTier;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * Loads the colony templates ({@code colony_template/*.json}) and the built-in structure plans: each
 * {@code structure/colony/<name>.nbt} becomes an in-memory plan with the sidecar {@code colony_plan/<name>.json}
 * (spans, power port, sign spot, anchor, commission kind), as in 05 section 3.1. Air cells are ignored.
 */
public final class ColonyPlans {
    /** One planned cell (relative position, state). Big blocks keep only their center cell. */
    public record Cell(BlockPos rel, BlockState state) {
    }

    /** An internal span between two relative node positions. */
    public record SpanSpec(BlockPos a, BlockPos b, CableTier tier) {
    }

    /** A built-in plan. */
    public record Plan(Identifier id, Vec3i size, List<Cell> cells, List<SpanSpec> spans, Optional<BlockPos> port, Optional<BlockPos> sign,
                       BlockPos anchor, String commission) {
    }

    /** A plan placed at an origin with a rotation: world cells in build order, spans, port, sign and anchor. */
    public record Placed(List<Cell> cells, List<SpanSpec> spans, Optional<BlockPos> port, Optional<BlockPos> sign, BlockPos anchor, BlockPos min,
                         BlockPos max) {
    }

    private record Loaded(Map<String, ColonyTemplate> templates, Map<Identifier, Plan> plans) {
    }

    private static Map<String, ColonyTemplate> templates = Map.of();
    private static Map<Identifier, Plan> plans = Map.of();

    private ColonyPlans() {
    }

    /** Register the server reload listener. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) -> e.addListener(Autarkia.id("colony_plans"), new Listener()));
    }

    /** Return the templates in index order. */
    public static Map<String, ColonyTemplate> templates() {
        return templates;
    }

    /** Return a plan by id, or null. */
    public static Plan plan(Identifier id) {
        return plans.get(id);
    }

    /** Return all plans. */
    public static Map<Identifier, Plan> plans() {
        return plans;
    }

    /** Return the rotated size (x and z swap for quarter turns). */
    public static Vec3i rotatedSize(Vec3i size, Rotation r) {
        return r == Rotation.CLOCKWISE_90 || r == Rotation.COUNTERCLOCKWISE_90 ? new Vec3i(size.getZ(), size.getY(), size.getX()) : size;
    }

    /** Return the map of 05 section 5.3 from a relative position of a box of this size to the world: rotate, then shift the min corner to origin. */
    public static java.util.function.Function<BlockPos, BlockPos> mapper(Vec3i size, BlockPos origin, Rotation r) {
        BlockPos far = new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        BlockPos a = StructureTemplate.transform(BlockPos.ZERO, Mirror.NONE, r, BlockPos.ZERO);
        BlockPos b = StructureTemplate.transform(far, Mirror.NONE, r, BlockPos.ZERO);
        BlockPos shift = origin.subtract(new BlockPos(Math.min(a.getX(), b.getX()), 0, Math.min(a.getZ(), b.getZ())));
        return rel -> StructureTemplate.transform(rel, Mirror.NONE, r, BlockPos.ZERO).offset(shift);
    }

    /** Place a plan: rotate each relative position (05 section 5.3) and shift so the min corner is the origin. */
    public static Placed place(Plan plan, BlockPos origin, Rotation r) {
        java.util.function.Function<BlockPos, BlockPos> map = mapper(plan.size(), origin, r);
        List<Cell> cells = new ArrayList<>();
        for (Cell c : plan.cells()) {
            cells.add(new Cell(map.apply(c.rel()), c.state().rotate(r)));
        }
        List<SpanSpec> spans = plan.spans().stream().map(s -> new SpanSpec(map.apply(s.a()), map.apply(s.b()), s.tier())).toList();
        Vec3i rs = rotatedSize(plan.size(), r);
        return new Placed(cells, spans, plan.port().map(map), plan.sign().map(map), map.apply(plan.anchor()), origin,
            origin.offset(rs.getX() - 1, rs.getY() - 1, rs.getZ() - 1));
    }

    private static final class Listener extends SimplePreparableReloadListener<Loaded> {
        @Override
        protected Loaded prepare(ResourceManager manager, ProfilerFiller profiler) {
            Map<String, ColonyTemplate> t = new LinkedHashMap<>();
            List<Map.Entry<String, ColonyTemplate>> list = new ArrayList<>();
            for (Map.Entry<Identifier, Resource> e : manager.listResources("colony_template", id -> id.getPath().endsWith(".json")).entrySet()) {
                String name = e.getKey().getPath().substring("colony_template/".length(), e.getKey().getPath().length() - 5);
                try (Reader r = e.getValue().openAsReader()) {
                    ColonyTemplate tpl = ColonyTemplate.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(r)).getOrThrow(
                        msg -> new IllegalStateException("Colony template " + e.getKey() + ": " + msg));
                    list.add(Map.entry(name, tpl));
                } catch (java.io.IOException ex) {
                    throw new IllegalStateException("Cannot read colony template " + e.getKey(), ex);
                }
            }
            list.sort(Comparator.comparingInt(en -> en.getValue().index()));
            list.forEach(en -> t.put(en.getKey(), en.getValue()));
            Map<Identifier, Plan> p = new LinkedHashMap<>();
            for (Map.Entry<Identifier, Resource> e : manager.listResources("structure/colony", id -> id.getPath().endsWith(".nbt")).entrySet()) {
                String name = e.getKey().getPath().substring("structure/colony/".length(), e.getKey().getPath().length() - 4);
                Identifier id = Identifier.fromNamespaceAndPath(e.getKey().getNamespace(), name);
                Identifier sidecar = Identifier.fromNamespaceAndPath(e.getKey().getNamespace(), "colony_plan/" + name + ".json");
                Optional<Resource> side = manager.getResource(sidecar);
                if (side.isEmpty()) {
                    throw new IllegalStateException("Colony structure " + e.getKey() + " has no sidecar " + sidecar);
                }
                try (InputStream in = e.getValue().open(); Reader sr = side.get().openAsReader()) {
                    p.put(id, read(id, NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()), JsonParser.parseReader(sr).getAsJsonObject()));
                } catch (java.io.IOException ex) {
                    throw new IllegalStateException("Cannot read colony structure " + e.getKey(), ex);
                }
            }
            for (Map.Entry<String, ColonyTemplate> e : t.entrySet()) {
                e.getValue().plan().ifPresent(pid -> {
                    if (!p.containsKey(pid)) {
                        throw new IllegalStateException("Colony template " + e.getKey() + " names plan " + pid + ", which has no structure");
                    }
                });
            }
            return new Loaded(t, p);
        }

        @Override
        protected void apply(Loaded loaded, ResourceManager manager, ProfilerFiller profiler) {
            templates = loaded.templates();
            plans = loaded.plans();
            Autarkia.LOG.info("[colony] loaded {} templates and {} structure plans", templates.size(), plans.size());
        }
    }

    private static BlockPos pos(JsonElement e) {
        JsonArray a = e.getAsJsonArray();
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    private static Plan read(Identifier id, CompoundTag nbt, JsonObject side) {
        ListTag size = nbt.getListOrEmpty("size");
        Vec3i vs = new Vec3i(size.getIntOr(0, 1), size.getIntOr(1, 1), size.getIntOr(2, 1));
        ListTag paletteTag = nbt.getListOrEmpty("palette");
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++) {
            palette.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK, paletteTag.getCompoundOrEmpty(i)));
        }
        List<Cell> cells = new ArrayList<>();
        ListTag blocks = nbt.getListOrEmpty("blocks");
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag b = blocks.getCompoundOrEmpty(i);
            ListTag p = b.getListOrEmpty("pos");
            BlockState st = palette.get(b.getIntOr("state", 0));
            if (!st.isAir()) {
                cells.add(new Cell(new BlockPos(p.getIntOr(0, 0), p.getIntOr(1, 0), p.getIntOr(2, 0)), st));
            }
        }
        cells.sort(Comparator.comparingInt((Cell c) -> c.rel().getY()).thenComparingInt(c -> c.rel().getX()).thenComparingInt(c -> c.rel().getZ()));
        List<SpanSpec> spans = new ArrayList<>();
        if (side.has("spans")) {
            for (JsonElement s : side.getAsJsonArray("spans")) {
                JsonObject o = s.getAsJsonObject();
                spans.add(new SpanSpec(pos(o.get("a")), pos(o.get("b")), CableTier.valueOf(o.get("tier").getAsString().toUpperCase(java.util.Locale.ROOT))));
            }
        }
        Optional<BlockPos> port = side.has("port") ? Optional.of(pos(side.get("port"))) : Optional.empty();
        Optional<BlockPos> sign = side.has("sign") ? Optional.of(pos(side.get("sign"))) : Optional.empty();
        String commission = side.has("commission") ? side.get("commission").getAsString() : "none";
        return new Plan(id, vs, List.copyOf(cells), List.copyOf(spans), port, sign, pos(side.get("anchor")), commission);
    }
}

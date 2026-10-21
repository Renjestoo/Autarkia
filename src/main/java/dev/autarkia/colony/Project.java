package dev.autarkia.colony;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Rotation;

/**
 * A colony project (05 section 1.5 and 4): a planned build of one template at one site, its state, reason, cost and
 * priority, bill of materials, leveling cells, spans to cut in the leveling phase, whether failure memory raised it, and
 * build progress (phase and cursor). Jobs are rebuilt from phase and cursor after a load.
 */
public final class Project {
    /** Project states of the 05 section 4 state chart. */
    public enum State implements StringRepresentable {
        PROPOSED, APPROVED, WAITING_ITEMS, BUILDING, BLOCKED, COMMISSIONING, DONE, DENIED, EXPIRED, CANCELLED;

        public static final Codec<State> CODEC = StringRepresentable.fromEnum(State::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** Return whether the project still counts as open (proposed or approved and not finished). */
        public boolean open() {
            return this != DONE && this != DENIED && this != EXPIRED && this != CANCELLED;
        }

        /** Return whether the project is approved and not finished. */
        public boolean active() {
            return this == APPROVED || this == WAITING_ITEMS || this == BUILDING || this == BLOCKED || this == COMMISSIONING;
        }
    }

    /** The card reason: a language key suffix and its numeric arguments. */
    public record Reason(String key, List<Double> args) {
        public static final Codec<Reason> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("key").forGetter(Reason::key),
            Codec.DOUBLE.listOf().optionalFieldOf("args", List.of()).forGetter(Reason::args)).apply(i, Reason::new));
    }

    private static final Codec<Rotation> ROTATION = StringRepresentable.fromEnum(Rotation::values);
    private static final Codec<Map<Item, Integer>> BOM = Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.INT);

    private static final com.mojang.serialization.MapCodec<Project> BASE = RecordCodecBuilder.mapCodec(i -> i.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(p -> p.id),
        Identifier.CODEC.fieldOf("template").forGetter(p -> p.template),
        Identifier.CODEC.fieldOf("plan").forGetter(p -> p.plan),
        BlockPos.CODEC.fieldOf("origin").forGetter(p -> p.origin),
        ROTATION.fieldOf("rotation").forGetter(p -> p.rotation),
        State.CODEC.fieldOf("state").forGetter(p -> p.state),
        Reason.CODEC.fieldOf("reason").forGetter(p -> p.reason),
        Codec.LONG.fieldOf("created").forGetter(p -> p.created),
        Codec.LONG.optionalFieldOf("approved", 0L).forGetter(p -> p.approved),
        Codec.BOOL.optionalFieldOf("auto", false).forGetter(p -> p.auto),
        Codec.DOUBLE.fieldOf("cost").forGetter(p -> p.cost),
        Codec.DOUBLE.fieldOf("priority").forGetter(p -> p.priority),
        Codec.DOUBLE.fieldOf("site_score").forGetter(p -> p.siteScore),
        BOM.optionalFieldOf("bom", Map.of()).forGetter(p -> p.bom),
        Codec.INT.optionalFieldOf("phase", 0).forGetter(p -> p.phase),
        Codec.INT.optionalFieldOf("cursor", 0).forGetter(p -> p.cursor)
    ).apply(i, Project::new));

    private static final com.mojang.serialization.MapCodec<Extra> EXTRA = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.INT.optionalFieldOf("failures", 0).forGetter(Extra::failures),
        Codec.INT.optionalFieldOf("adjust", 0).forGetter(Extra::adjust),
        Codec.INT.optionalFieldOf("floor", 0).forGetter(Extra::floor),
        Codec.LONG_STREAM.xmap(s -> s.toArray(), java.util.Arrays::stream).optionalFieldOf("cuts", new long[0]).forGetter(Extra::cuts),
        Codec.LONG_STREAM.xmap(s -> s.toArray(), java.util.Arrays::stream).optionalFieldOf("fills", new long[0]).forGetter(Extra::fills),
        Codec.LONG.optionalFieldOf("false_since", -1L).forGetter(Extra::falseSince),
        Codec.DOUBLE.optionalFieldOf("urgency", 0.0).forGetter(Extra::urgency),
        Codec.STRING.optionalFieldOf("block_reason", "").forGetter(Extra::blockReason),
        Codec.LONG.optionalFieldOf("commission_start", -1L).forGetter(Extra::commissionStart),
        Codec.LONG.optionalFieldOf("sign_pos", Long.MIN_VALUE).forGetter(Extra::signPos),
        Codec.BOOL.optionalFieldOf("signed", false).forGetter(Extra::signed)
    ).apply(i, Extra::new));

    private record Extra(int failures, int adjust, int floor, long[] cuts, long[] fills, long falseSince, double urgency, String blockReason,
                         long commissionStart, long signPos, boolean signed) {
    }

    /** A planned world cell. */
    public record Cell(long pos, net.minecraft.world.level.block.state.BlockState state) {
        public static final Codec<Cell> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("pos").forGetter(Cell::pos),
            net.minecraft.world.level.block.state.BlockState.CODEC.fieldOf("state").forGetter(Cell::state)).apply(i, Cell::new));
    }

    /** A planned span between two world node positions. */
    public record SpanRec(long a, long b, dev.autarkia.energy.CableTier tier) {
        private static final Codec<dev.autarkia.energy.CableTier> TIER = Codec.STRING.xmap(
            s -> dev.autarkia.energy.CableTier.valueOf(s.toUpperCase(java.util.Locale.ROOT)), t -> t.name().toLowerCase(java.util.Locale.ROOT));
        public static final Codec<SpanRec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("a").forGetter(SpanRec::a), Codec.LONG.fieldOf("b").forGetter(SpanRec::b),
            TIER.fieldOf("tier").forGetter(SpanRec::tier)).apply(i, SpanRec::new));
    }

    private record Build(List<Cell> cells, List<SpanRec> spans, long anchor, String commission, long port, List<SpanRec> cutSpans,
                         boolean fromMemory) {
    }

    private static final com.mojang.serialization.MapCodec<Build> BUILD = RecordCodecBuilder.mapCodec(i -> i.group(
        Cell.CODEC.listOf().optionalFieldOf("cells", List.of()).forGetter(Build::cells),
        SpanRec.CODEC.listOf().optionalFieldOf("spans", List.of()).forGetter(Build::spans),
        Codec.LONG.optionalFieldOf("anchor", 0L).forGetter(Build::anchor),
        Codec.STRING.optionalFieldOf("commission", "none").forGetter(Build::commission),
        Codec.LONG.optionalFieldOf("port", Long.MIN_VALUE).forGetter(Build::port),
        SpanRec.CODEC.listOf().optionalFieldOf("cut_spans", List.of()).forGetter(Build::cutSpans),
        Codec.BOOL.optionalFieldOf("from_memory", false).forGetter(Build::fromMemory)
    ).apply(i, Build::new));

    public static final Codec<Project> CODEC = Codec.mapPair(BASE, Codec.mapPair(EXTRA, BUILD)).codec().xmap(p -> {
        Project pr = p.getFirst();
        Extra e = p.getSecond().getFirst();
        Build b = p.getSecond().getSecond();
        pr.cells.addAll(b.cells);
        pr.spans.addAll(b.spans);
        pr.anchor = b.anchor;
        pr.commission = b.commission;
        pr.port = b.port;
        pr.cutSpans.addAll(b.cutSpans);
        pr.fromMemory = b.fromMemory;
        pr.failures = e.failures;
        pr.adjust = e.adjust;
        pr.floor = e.floor;
        pr.cuts = e.cuts;
        pr.fills = e.fills;
        pr.falseSince = e.falseSince;
        pr.urgency = e.urgency;
        pr.blockReason = e.blockReason;
        pr.commissionStart = e.commissionStart;
        pr.signPos = e.signPos;
        pr.signed = e.signed;
        return pr;
    }, pr -> com.mojang.datafixers.util.Pair.of(pr, com.mojang.datafixers.util.Pair.of(new Extra(pr.failures, pr.adjust, pr.floor, pr.cuts, pr.fills,
        pr.falseSince, pr.urgency, pr.blockReason, pr.commissionStart, pr.signPos, pr.signed), new Build(pr.cells, pr.spans, pr.anchor, pr.commission, pr.port, pr.cutSpans, pr.fromMemory))));

    public final UUID id;
    public final Identifier template;
    public final Identifier plan;
    public BlockPos origin;
    public Rotation rotation;
    public State state;
    public Reason reason;
    public final long created;
    public long approved;
    public boolean auto;
    public double cost;
    public double priority;
    public double siteScore;
    public final Map<Item, Integer> bom = new LinkedHashMap<>();
    public int phase;
    public int cursor;
    public int failures;
    public int adjust;
    public int floor;
    public long[] cuts = new long[0];
    public long[] fills = new long[0];
    public long falseSince = -1;
    public double urgency;
    public String blockReason = "";
    public long commissionStart = -1;
    public long signPos = Long.MIN_VALUE;
    public boolean signed;
    public final List<Cell> cells = new ArrayList<>();
    public final List<SpanRec> spans = new ArrayList<>();
    public final List<SpanRec> cutSpans = new ArrayList<>();
    public boolean fromMemory;
    public long anchor;
    public String commission = "none";
    public long port = Long.MIN_VALUE;

    public Project(UUID id, Identifier template, Identifier plan, BlockPos origin, Rotation rotation, State state, Reason reason, long created,
                   long approved, boolean auto, double cost, double priority, double siteScore, Map<Item, Integer> bom, int phase, int cursor) {
        this.id = id;
        this.template = template;
        this.plan = plan;
        this.origin = origin;
        this.rotation = rotation;
        this.state = state;
        this.reason = reason;
        this.created = created;
        this.approved = approved;
        this.auto = auto;
        this.cost = cost;
        this.priority = priority;
        this.siteScore = siteScore;
        this.bom.putAll(bom);
        this.phase = phase;
        this.cursor = cursor;
    }

    /** Return the leveling edit count (cut plus fill cells). */
    public int levelingEdits() {
        return cuts.length + fills.length;
    }

    /** Return the short id used by the debug commands (first 8 hex digits). */
    public String shortId() {
        return id.toString().substring(0, 8);
    }

    /** Return a copy of the cut cells as positions. */
    public List<BlockPos> cutCells() {
        List<BlockPos> out = new ArrayList<>(cuts.length);
        for (long c : cuts) {
            out.add(BlockPos.of(c));
        }
        return out;
    }
}

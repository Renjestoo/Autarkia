package dev.autarkia.colony;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * One colony record of the {@code autarkia:colonies} save data (05 section 1.5): identity, owner and trusted players,
 * Core position and tier, state, autonomy, colony clock and daily budget, projects, failure memory, template state,
 * stock targets, sensor smoothing state, statistics, the worker registry (06 section 6.1), the domain beacons, the
 * outposts, the known ore deposits, standing deliveries, the Replicator Walker state, the Restoration Fields (05
 * section 1.5 {@code fields}, decoded entry by entry) and the Pattern ids packed into Autarky Seeds
 * ({@code seed_patterns}, kept out of {@code pattern gc}, M9 decision G20).
 */
public final class Colony {
    /** Colony life state. */
    public enum State implements StringRepresentable {
        GROWING, ACTIVE, DORMANT;

        public static final Codec<State> CODEC = StringRepresentable.fromEnum(State::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Autonomy level (05 section 4.3). */
    public enum Autonomy implements StringRepresentable {
        MANUAL, ASSISTED, AUTONOMOUS;

        public static final Codec<Autonomy> CODEC = StringRepresentable.fromEnum(Autonomy::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** A failure memory entry (05 section 6.1); weight decays with a half-life on the colony clock. */
    public record Memory(String type, String key, BlockPos pos, double weight, long t0) {
        public static final Codec<Memory> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("type").forGetter(Memory::type), Codec.STRING.fieldOf("key").forGetter(Memory::key),
            BlockPos.CODEC.fieldOf("pos").forGetter(Memory::pos), Codec.DOUBLE.fieldOf("weight").forGetter(Memory::weight),
            Codec.LONG.fieldOf("t0").forGetter(Memory::t0)).apply(i, Memory::new));

        /** Return the decayed weight at the colony clock time. */
        public double weightAt(long clock, double halfLife) {
            return weight * Math.pow(0.5, Math.max(0, clock - t0) / halfLife);
        }
    }

    /** A denied site: a chunk the planner rejects for this template until the given colony clock time. */
    public record DeniedSite(long chunk, long until) {
        public static final Codec<DeniedSite> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("chunk").forGetter(DeniedSite::chunk), Codec.LONG.fieldOf("until").forGetter(DeniedSite::until)).apply(i, DeniedSite::new));
    }

    /** Per-template planner state (05 section 1.5 template_state). */
    public static final class TemplateState {
        public static final Codec<TemplateState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(t -> t.enabled),
            Codec.LONG.optionalFieldOf("cooldown_until", 0L).forGetter(t -> t.cooldownUntil),
            DeniedSite.CODEC.listOf().optionalFieldOf("denied_sites", List.of()).forGetter(t -> t.deniedSites),
            Codec.INT.optionalFieldOf("built", 0).forGetter(t -> t.built),
            Codec.INT.listOf().optionalFieldOf("holds", List.of()).forGetter(t -> t.holds),
            Codec.LONG.optionalFieldOf("no_site_until", 0L).forGetter(t -> t.noSiteUntil)
        ).apply(i, TemplateState::new));

        public boolean enabled;
        public long cooldownUntil;
        public final List<DeniedSite> deniedSites = new ArrayList<>();
        public int built;
        public final List<Integer> holds = new ArrayList<>();
        public long noSiteUntil;

        public TemplateState() {
            this(true, 0, List.of(), 0, List.of(), 0);
        }

        private TemplateState(boolean enabled, long cooldownUntil, List<DeniedSite> denied, int built, List<Integer> holds, long noSiteUntil) {
            this.enabled = enabled;
            this.cooldownUntil = cooldownUntil;
            this.deniedSites.addAll(denied);
            this.built = built;
            this.holds.addAll(holds);
            this.noSiteUntil = noSiteUntil;
        }
    }

    /** A Pattern library slot (05 section 1.5 library). */
    public record LibraryEntry(UUID pattern, String role, boolean enabled) {
        public static final Codec<LibraryEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("pattern").forGetter(LibraryEntry::pattern), Codec.STRING.fieldOf("role").forGetter(LibraryEntry::role),
            Codec.BOOL.fieldOf("enabled").forGetter(LibraryEntry::enabled)).apply(i, LibraryEntry::new));
    }

    /**
     * A Pattern instance in the world (05 section 1.5 instances): origin (min corner), rotation, the rotated box size,
     * whether the colony watches it, and the accepted-removal mask of cell bits.
     */
    public record Instance(UUID id, UUID pattern, BlockPos origin, net.minecraft.world.level.block.Rotation rotation, net.minecraft.core.Vec3i size,
                           boolean watched, long[] mask) {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Instance::id), UUIDUtil.CODEC.fieldOf("pattern").forGetter(Instance::pattern),
            BlockPos.CODEC.fieldOf("origin").forGetter(Instance::origin),
            Codec.STRING.xmap(s -> net.minecraft.world.level.block.Rotation.valueOf(s.toUpperCase(java.util.Locale.ROOT)),
                r -> r.name().toLowerCase(java.util.Locale.ROOT)).fieldOf("rotation").forGetter(Instance::rotation),
            net.minecraft.core.Vec3i.CODEC.fieldOf("size").forGetter(Instance::size), Codec.BOOL.fieldOf("watched").forGetter(Instance::watched),
            Codec.LONG_STREAM.xmap(java.util.stream.LongStream::toArray, java.util.stream.LongStream::of).fieldOf("mask").forGetter(Instance::mask))
            .apply(i, Instance::new));

        /** Return whether the world position lies in the instance box. */
        public boolean contains(BlockPos p) {
            return p.getX() >= origin.getX() && p.getY() >= origin.getY() && p.getZ() >= origin.getZ() && p.getX() < origin.getX() + size.getX()
                && p.getY() < origin.getY() + size.getY() && p.getZ() < origin.getZ() + size.getZ();
        }

        /** Return a copy with the watched flag changed. */
        public Instance watched(boolean on) {
            return new Instance(id, pattern, origin, rotation, size, on, mask);
        }
    }

    /** A domain beacon (05 section 1.3): its position and whether it had power at the last tick. */
    public record Beacon(BlockPos pos, boolean active) {
        public static final Codec<Beacon> CODEC = RecordCodecBuilder.create(i -> i.group(BlockPos.CODEC.fieldOf("pos").forGetter(Beacon::pos),
            Codec.BOOL.fieldOf("active").forGetter(Beacon::active)).apply(i, Beacon::new));
    }

    /**
     * An ore deposit the colony knows (05 section 1.5 deposits): ore id path, position of its surface outcrop, the
     * estimated count of ore blocks below, the colony clock time it was found, and the outpost that mines it.
     */
    public record Deposit(UUID id, String ore, BlockPos pos, int estCount, long found, java.util.Optional<UUID> outpost) {
        public static final Codec<Deposit> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Deposit::id), Codec.STRING.fieldOf("ore").forGetter(Deposit::ore),
            BlockPos.CODEC.fieldOf("pos").forGetter(Deposit::pos), Codec.INT.fieldOf("est_count").forGetter(Deposit::estCount),
            Codec.LONG.fieldOf("found").forGetter(Deposit::found), UUIDUtil.CODEC.optionalFieldOf("outpost").forGetter(Deposit::outpost))
            .apply(i, Deposit::new));

        /** Return a copy with another estimated count. */
        public Deposit withCount(int count) {
            return new Deposit(id, ore, pos, count, found, outpost);
        }

        /** Return a copy claimed by an outpost. */
        public Deposit claimedBy(UUID outpostId) {
            return new Deposit(id, ore, pos, estCount, found, java.util.Optional.of(outpostId));
        }
    }

    /** Power or transport link of an outpost to the main base (05 section 1.5 outposts). */
    public enum Link implements StringRepresentable {
        NONE, HV, CABLE_CAR, BOTH;

        public static final Codec<Link> CODEC = StringRepresentable.fromEnum(Link::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** Return the link with another link kind added. */
        public Link with(Link other) {
            return this == other || other == NONE ? this : this == NONE ? other : BOTH;
        }
    }

    /**
     * An outpost record (05 sections 1.5 and 8.1): core position, founding time, link, the measured production profile
     * (items per tick entering its depots), the ticks the profile covers, the time of the last credit, the virtual
     * buffer of catch-up items that did not fit (max 4,096 items) and the item count of the last catch-up.
     */
    public static final class Outpost {
        private static final Codec<Map<Item, Double>> RATES = Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.DOUBLE);
        private static final Codec<Map<Item, Integer>> COUNTS = Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.INT);
        public static final Codec<Outpost> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(o -> o.id), BlockPos.CODEC.fieldOf("core_pos").forGetter(o -> o.corePos),
            Codec.LONG.fieldOf("founded").forGetter(o -> o.founded), Link.CODEC.optionalFieldOf("link", Link.NONE).forGetter(o -> o.link),
            RATES.optionalFieldOf("profile", Map.of()).forGetter(o -> o.profile), Codec.LONG.optionalFieldOf("profile_ticks", 0L).forGetter(o -> o.profileTicks),
            Codec.LONG.optionalFieldOf("last_credit", 0L).forGetter(o -> o.lastCredit),
            COUNTS.optionalFieldOf("virtual_buffer", Map.of()).forGetter(o -> o.virtualBuffer),
            Codec.INT.optionalFieldOf("last_catch_up", 0).forGetter(o -> o.lastCatchUp)).apply(i, Outpost::new));

        public final UUID id;
        public final BlockPos corePos;
        public final long founded;
        public Link link;
        public final Map<Item, Double> profile = new LinkedHashMap<>();
        public long profileTicks;
        public long lastCredit;
        public final Map<Item, Integer> virtualBuffer = new LinkedHashMap<>();
        public int lastCatchUp;

        private Outpost(UUID id, BlockPos corePos, long founded, Link link, Map<Item, Double> profile, long profileTicks, long lastCredit,
                        Map<Item, Integer> buffer, int lastCatchUp) {
            this.id = id;
            this.corePos = corePos;
            this.founded = founded;
            this.link = link;
            this.profile.putAll(profile);
            this.profileTicks = profileTicks;
            this.lastCredit = lastCredit;
            this.virtualBuffer.putAll(buffer);
            this.lastCatchUp = lastCatchUp;
        }

        /** Create a new outpost founded now at the core position. */
        public static Outpost create(BlockPos corePos, long now) {
            return new Outpost(UUID.randomUUID(), corePos, now, Link.NONE, Map.of(), 0, now, Map.of(), 0);
        }

        /** Return the items held in the virtual buffer. */
        public int buffered() {
            return virtualBuffer.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    /**
     * A standing delivery order: workers bring the items to the block at target (DELIVER jobs, one open job per target at
     * a time, at most 16 items each), for the Sentinel parts of a {@code sentinel_post} and the gondola parts of a cable
     * car (05 sections 3.3 and 8.1).
     */
    public record Delivery(BlockPos target, Item item, int count) {
        public static final Codec<Delivery> CODEC = RecordCodecBuilder.create(i -> i.group(BlockPos.CODEC.fieldOf("target").forGetter(Delivery::target),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Delivery::item), Codec.INT.fieldOf("count").forGetter(Delivery::count))
            .apply(i, Delivery::new));
    }

    /** A block that takes colony deliveries into its own handler instead of its item capability (gondola part slots). */
    public interface DeliveryTarget {
        /** Return the handler DELIVER jobs insert into. */
        net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> deliveryHandler();
    }

    /** A worker registry entry (06 section 6.1). */
    public record WorkerEntry(UUID id, Identifier type, int cost, long chunk, long heartbeat) {
        public static final Codec<WorkerEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(WorkerEntry::id), Identifier.CODEC.fieldOf("type").forGetter(WorkerEntry::type),
            Codec.INT.fieldOf("cost").forGetter(WorkerEntry::cost), Codec.LONG.fieldOf("chunk").forGetter(WorkerEntry::chunk),
            Codec.LONG.fieldOf("heartbeat").forGetter(WorkerEntry::heartbeat)).apply(i, WorkerEntry::new));
    }

    /** Replicator Walker state (05 sections 1.5 and 8.2): the walkers on their way and the colony clock of the last print. */
    public record Replicators(List<UUID> active, long lastPrint) {
        public static final Codec<Replicators> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.listOf().optionalFieldOf("active", List.of()).forGetter(Replicators::active),
            Codec.LONG.optionalFieldOf("last_print", Long.MIN_VALUE / 2).forGetter(Replicators::lastPrint)).apply(i, Replicators::new));
    }

    private static final Codec<Map<Item, Integer>> ITEM_INTS = Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.INT);

    private record Ascendant(List<RestorationField> fields, List<UUID> seedPatterns) {
    }

    private static final MapCodec<Ascendant> ASCENDANT = RecordCodecBuilder.mapCodec(i -> i.group(
        RestorationField.LIST.optionalFieldOf("fields", List.of()).forGetter(Ascendant::fields),
        UUIDUtil.CODEC.listOf().optionalFieldOf("seed_patterns", List.of()).forGetter(Ascendant::seedPatterns)
    ).apply(i, Ascendant::new));

    private static final MapCodec<Colony> IDENTITY = RecordCodecBuilder.mapCodec(i -> i.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(c -> c.id),
        Codec.STRING.optionalFieldOf("name", "Colony").forGetter(c -> c.name),
        UUIDUtil.CODEC.fieldOf("owner").forGetter(c -> c.owner),
        UUIDUtil.CODEC.listOf().optionalFieldOf("trusted", List.of()).forGetter(c -> c.trusted),
        ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(c -> c.dimension),
        BlockPos.CODEC.fieldOf("core_pos").forGetter(c -> c.corePos),
        Codec.INT.optionalFieldOf("tier", 1).forGetter(c -> c.tier),
        State.CODEC.fieldOf("state").forGetter(c -> c.state),
        Autonomy.CODEC.optionalFieldOf("autonomy", Autonomy.ASSISTED).forGetter(c -> c.autonomy),
        Codec.LONG.optionalFieldOf("clock", 0L).forGetter(c -> c.clock),
        Codec.LONG.optionalFieldOf("last_clock", -1L).forGetter(c -> c.lastClock),
        Codec.LONG.optionalFieldOf("budget_day", -1L).forGetter(c -> c.budgetDay),
        Codec.DOUBLE.optionalFieldOf("budget_remaining", 0.0).forGetter(c -> c.budgetRemaining),
        Codec.BOOL.optionalFieldOf("signs", true).forGetter(c -> c.signs),
        Codec.INT.optionalFieldOf("growth", 0).forGetter(c -> c.growth)
    ).apply(i, Colony::new));

    private record Planning(List<Project> projects, List<Memory> memories, Map<String, TemplateState> templates, Map<Item, Integer> stock,
                            Map<String, Double> ema, Map<String, List<Double>> peaks, Map<String, Long> stats, List<WorkerEntry> workers,
                            List<LibraryEntry> library, List<Instance> instances, List<Beacon> beacons, List<Outpost> outposts, List<Deposit> deposits,
                            List<Delivery> deliveries, Replicators replicators, List<NaniteClaim> claims) {
    }

    private static final MapCodec<Planning> PLANNING = RecordCodecBuilder.mapCodec(i -> i.group(
        Project.CODEC.listOf().optionalFieldOf("projects", List.of()).forGetter(Planning::projects),
        Memory.CODEC.listOf().optionalFieldOf("memories", List.of()).forGetter(Planning::memories),
        Codec.unboundedMap(Codec.STRING, TemplateState.CODEC).optionalFieldOf("template_state", Map.of()).forGetter(Planning::templates),
        ITEM_INTS.optionalFieldOf("stock_targets", Map.of()).forGetter(Planning::stock),
        Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("ema", Map.of()).forGetter(Planning::ema),
        Codec.unboundedMap(Codec.STRING, Codec.DOUBLE.listOf()).optionalFieldOf("peaks", Map.of()).forGetter(Planning::peaks),
        Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("stats", Map.of()).forGetter(Planning::stats),
        WorkerEntry.CODEC.listOf().optionalFieldOf("workers", List.of()).forGetter(Planning::workers),
        LibraryEntry.CODEC.listOf().optionalFieldOf("library", List.of()).forGetter(Planning::library),
        Instance.CODEC.listOf().optionalFieldOf("instances", List.of()).forGetter(Planning::instances),
        Beacon.CODEC.listOf().optionalFieldOf("beacons", List.of()).forGetter(Planning::beacons),
        Outpost.CODEC.listOf().optionalFieldOf("outposts", List.of()).forGetter(Planning::outposts),
        Deposit.CODEC.listOf().optionalFieldOf("deposits", List.of()).forGetter(Planning::deposits),
        Delivery.CODEC.listOf().optionalFieldOf("deliveries", List.of()).forGetter(Planning::deliveries),
        Replicators.CODEC.optionalFieldOf("replicators", new Replicators(List.of(), Long.MIN_VALUE / 2)).forGetter(Planning::replicators),
        NaniteClaim.CODEC.listOf().optionalFieldOf("claims", List.of()).forGetter(Planning::claims)
    ).apply(i, Planning::new));

    public static final Codec<Colony> CODEC = Codec.mapPair(Codec.mapPair(IDENTITY, PLANNING), ASCENDANT).codec().xmap(q -> {
        var p = q.getFirst();
        Colony c = p.getFirst();
        c.fields.addAll(q.getSecond().fields());
        c.seedPatterns.addAll(q.getSecond().seedPatterns());
        Planning pl = p.getSecond();
        c.projects.addAll(pl.projects);
        c.memories.addAll(pl.memories);
        c.templates.putAll(pl.templates);
        c.stockTargets.putAll(pl.stock);
        c.ema.putAll(pl.ema);
        pl.peaks.forEach((k, v) -> c.peaks.put(k, new ArrayList<>(v)));
        c.stats.putAll(pl.stats);
        pl.workers.forEach(w -> c.workers.put(w.id(), w));
        c.library.addAll(pl.library);
        c.instances.addAll(pl.instances);
        c.beacons.addAll(pl.beacons);
        c.outposts.addAll(pl.outposts);
        c.deposits.addAll(pl.deposits);
        c.deliveries.addAll(pl.deliveries);
        c.replicatorsActive.addAll(pl.replicators.active());
        c.lastPrint = pl.replicators.lastPrint();
        c.claims.addAll(pl.claims);
        return c;
    }, c -> com.mojang.datafixers.util.Pair.of(com.mojang.datafixers.util.Pair.of(c, new Planning(c.projects, c.memories, c.templates, c.stockTargets, c.ema, c.peaks, c.stats,
        new ArrayList<>(c.workers.values()), c.library, c.instances, c.beacons, c.outposts, c.deposits, c.deliveries,
        new Replicators(List.copyOf(c.replicatorsActive), c.lastPrint), c.claims)),
        new Ascendant(List.copyOf(c.fields), List.copyOf(c.seedPatterns))));

    public final UUID id;
    public String name;
    public UUID owner;
    public final List<UUID> trusted = new ArrayList<>();
    public final ResourceKey<Level> dimension;
    public BlockPos corePos;
    public int tier;
    public State state;
    public Autonomy autonomy;
    public long clock;
    public long lastClock;
    public long budgetDay;
    public double budgetRemaining;
    public boolean signs;
    public int growth;
    public final List<Project> projects = new ArrayList<>();
    public final List<Memory> memories = new ArrayList<>();
    public final Map<String, TemplateState> templates = new LinkedHashMap<>();
    public final Map<Item, Integer> stockTargets = new LinkedHashMap<>();
    public final Map<String, Double> ema = new LinkedHashMap<>();
    public final Map<String, List<Double>> peaks = new LinkedHashMap<>();
    public final Map<String, Long> stats = new LinkedHashMap<>();
    public final Map<UUID, WorkerEntry> workers = new LinkedHashMap<>();
    public final List<LibraryEntry> library = new ArrayList<>();
    public final List<Instance> instances = new ArrayList<>();
    public final List<Beacon> beacons = new ArrayList<>();
    public final List<Outpost> outposts = new ArrayList<>();
    public final List<Deposit> deposits = new ArrayList<>();
    public final List<Delivery> deliveries = new ArrayList<>();
    public final List<UUID> replicatorsActive = new ArrayList<>();
    public long lastPrint = Long.MIN_VALUE / 2;
    public final List<NaniteClaim> claims = new ArrayList<>();
    public final List<RestorationField> fields = new ArrayList<>();
    public final List<UUID> seedPatterns = new ArrayList<>();

    private Colony(UUID id, String name, UUID owner, List<UUID> trusted, ResourceKey<Level> dimension, BlockPos corePos, int tier, State state,
                   Autonomy autonomy, long clock, long lastClock, long budgetDay, double budgetRemaining, boolean signs, int growth) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.trusted.addAll(trusted);
        this.dimension = dimension;
        this.corePos = corePos;
        this.tier = tier;
        this.state = state;
        this.autonomy = autonomy;
        this.clock = clock;
        this.lastClock = lastClock;
        this.budgetDay = budgetDay;
        this.budgetRemaining = budgetRemaining;
        this.signs = signs;
        this.growth = growth;
    }

    /** Create a new growing colony at the Core center. */
    public static Colony create(UUID owner, String name, ResourceKey<Level> dimension, BlockPos core) {
        return new Colony(UUID.randomUUID(), name, owner, List.of(), dimension, core, 1, State.GROWING, Autonomy.ASSISTED, 0, -1, -1, 0, true, 0);
    }

    /** Return the template state, creating a default one. */
    public TemplateState template(String id) {
        return templates.computeIfAbsent(id, k -> new TemplateState());
    }

    /** Return the open (not finished) projects. */
    public List<Project> openProjects() {
        return projects.stream().filter(p -> p.state.open()).toList();
    }

    /** Return the project with the id, or null. */
    public Project project(UUID id) {
        for (Project p : projects) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** Return the project whose id starts with the given hex prefix, or null. */
    public Project projectByPrefix(String prefix) {
        for (Project p : projects) {
            if (p.id.toString().startsWith(prefix)) {
                return p;
            }
        }
        return null;
    }

    /** Return whether the player may change settings and approve projects (owner or trusted). */
    public boolean canManage(UUID player) {
        return owner.equals(player) || trusted.contains(player);
    }

    /** Add to a statistic counter. */
    public void stat(String key, long delta) {
        stats.merge(key, delta, Long::sum);
    }

    /** Return the summed cap cost of the registered workers. */
    public int workerCost() {
        return workers.values().stream().mapToInt(WorkerEntry::cost).sum();
    }

    /** Return the outpost with the id, or null. */
    public @org.jspecify.annotations.Nullable Outpost outpost(UUID id) {
        for (Outpost o : outposts) {
            if (o.id.equals(id)) {
                return o;
            }
        }
        return null;
    }

    /** Return the number of registered workers of a type. */
    public int workerCount(Identifier type) {
        return (int) workers.values().stream().filter(w -> w.type().equals(type)).count();
    }
}

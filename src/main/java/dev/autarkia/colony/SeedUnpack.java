package dev.autarkia.colony;

import com.klikli_dev.magicparticleslib.premade.particle.electricarc.ElectricArcParticleOptions;
import com.mojang.authlib.GameProfile;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.Grid;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.storage.MatterBank;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.jspecify.annotations.Nullable;

/**
 * A running Autarky Seed unpack (05 section 8.4, W21), kept and saved in the planted outpost core (C3). The clock t
 * counts unpack ticks from the tick after the landing (t0) and stops while {@code seed.enabled} is false or a footprint
 * chunk is not loaded; with 900 to 23,040 cells (default config) the whole unpack takes 140 + 900 + 20 + 40 = 1,100 ticks.
 * <ol>
 * <li>t 0 to 40: the client ring (payload {@code seed_unpack} at landing, C17);</li>
 * <li>from t 40: leveling, at most 64 edits per tick: the cuts (solid blocks above the floor up to the footprint height,
 * top down; each fires {@code BreakBlockEvent} with a fake player of the colony owner, a cancelled event skips the cell,
 * 05b D8.13) then the fills (dirt into the empty cells of each column below the floor, bottom up), each fill paid by one
 * cut block of material or else by 1 free MU (G10); block entities are never touched;</li>
 * <li>from t 140 and when leveling is done: the rise (C2): each tick B / {@code seed.targetRiseTicks} is added to an
 * accumulator and its whole part spawns as RISE {@code rising_block}s, at most {@code seed.maxRisingPerTick}, at least 1
 * while cells remain and never above {@code seed.maxRisingEntities} alive; cells go in y, then centre distance, then
 * position order; {@code rising_block} plays once per 8 spawned blocks (G19);</li>
 * <li>21 ticks after the last spawn: every span connects at once with an arc;</li>
 * <li>40 ticks later the finish: Autarkia block entity data from the Patterns, one watched Pattern instance per entry,
 * the FE charge into the outpost core store, the crew unfolds (G18), colony event {@code seed_unpacked}.</li>
 * </ol>
 * Leftover cut material after leveling is spent (logged, no drops).
 */
public final class SeedUnpack {
    public static final int LEVEL_START = 40;
    public static final int RISE_START = 140;
    public static final int LEVEL_EDITS_PER_TICK = 64;
    public static final int FINISH_DELAY = 40;
    public static final int SOUND_EVERY = 8;
    private final SeedContents contents;
    private final int turns;
    private final BlockPos min;
    private final long landedAt;
    private int t;
    private boolean leveled;
    private int levelEdits;
    private int maxEditsPerTick;
    private long material;
    private long fillMu;
    private int riseStartAt = -1;
    private int cursor;
    private long acc;
    private int lastSpawnAt = -1;
    private int spansAt = -1;
    private int maxAlive;
    private boolean waitingMatter;
    private SeedLayout.@Nullable Result layout;
    private SeedLayout.@Nullable World world;
    private @Nullable List<Project.Cell> cells;
    private @Nullable LongArrayList cuts;
    private @Nullable LongArrayList fills;
    private int cutIndex;
    private int fillIndex;
    private @Nullable String paused;

    /** Start the state of a seed that landed now with the given placement and the cut material of the core space. */
    public SeedUnpack(SeedContents contents, SeedLayout.World world, long landedAt, long material) {
        this(contents, SeedLayout.quarterTurns(world.rotation()), world.min(), landedAt);
        this.material = material;
    }

    private SeedUnpack(SeedContents contents, int turns, BlockPos min, long landedAt) {
        this.contents = contents;
        this.turns = turns;
        this.min = min;
        this.landedAt = landedAt;
    }

    /** Return the unpack ticks run so far. */
    public int ticks() {
        return t;
    }

    /** Return the most rising blocks alive in the level seen at any unpack tick. */
    public int maxAlive() {
        return maxAlive;
    }

    /** Return the most leveling edits made in one tick. */
    public int maxEditsPerTick() {
        return maxEditsPerTick;
    }

    /** Return the leveling edits made so far. */
    public int levelEdits() {
        return levelEdits;
    }

    /** Return the free MU paid for fills. */
    public long fillMatter() {
        return fillMu;
    }

    /** Return the unpack tick of the first rising spawn, or -1. */
    public int riseStart() {
        return riseStartAt;
    }

    /** Return the cells spawned so far. */
    public int spawned() {
        return cursor;
    }

    /** Return the seed contents. */
    public SeedContents contents() {
        return contents;
    }

    /** Return the game time of the landing. */
    public long landedAt() {
        return landedAt;
    }

    private boolean ensure(ServerLevel level) {
        if (layout != null) {
            return true;
        }
        SeedLayout.Result r = SeedLayout.check(level.getServer(), contents.entries(), Integer.MAX_VALUE);
        if (r.problem() == SeedLayout.Problem.NO_PATTERN || r.problem() == SeedLayout.Problem.NOT_OUTPOST) {
            Autarkia.LOG.error("[colony] seed unpack at {}: a Pattern of the seed is missing or invalid ({}); the unpack stops", min.toShortString(),
                r.problem());
            return false;
        }
        layout = r;
        world = new SeedLayout.World(SeedLayout.rotation(turns), min, ColonyPlans.rotatedSize(r.size(), SeedLayout.rotation(turns)));
        cells = SeedLayout.cells(r, world);
        return true;
    }

    private boolean loaded(ServerLevel level) {
        BlockPos max = world.max();
        for (int cx = min.getX() >> 4; cx <= max.getX() >> 4; cx++) {
            for (int cz = min.getZ() >> 4; cz <= max.getZ() >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void pause(String why) {
        if (!why.equals(paused)) {
            paused = why;
            Autarkia.LOG.info("[colony] seed unpack at {} paused at tick {}: {}", min.toShortString(), t, why);
        }
    }

    /** Advance one tick; returns true when the unpack is finished or cannot go on (the core then drops the state). */
    public boolean tick(ServerLevel level, Colony c, Colony.Outpost o, OutpostCoreBlockEntity core) {
        if (!AutarkiaConfig.SEED_ENABLED.get()) {
            pause("seed.enabled is false");
            return false;
        }
        if (!ensure(level)) {
            return true;
        }
        if (!loaded(level)) {
            pause("a footprint chunk is not loaded");
            return false;
        }
        paused = null;
        if (level.getGameTime() <= landedAt) {
            return false;
        }
        t++;
        if (t >= LEVEL_START && !leveled) {
            level(level, c);
        }
        if (leveled && t >= RISE_START && cursor < cells.size()) {
            rise(level);
        }
        if (leveled && cursor >= cells.size() && spansAt < 0 && t >= Math.max(RISE_START, lastSpawnAt + RisingBlockEntity.RISE_TICKS + 1)) {
            spans(level);
        }
        maxAlive = Math.max(maxAlive, RisingBlockEntity.alive(level));
        core.setChanged();
        if (spansAt >= 0 && t >= spansAt + FINISH_DELAY) {
            finish(level, c, o, core);
            return true;
        }
        return false;
    }

    private void queues(ServerLevel level) {
        cuts = new LongArrayList();
        fills = new LongArrayList();
        int floor = min.getY() - 1;
        int top = floor + world.size().getY();
        BlockPos max = world.max();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = top; y > floor; y--) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockState st = level.getBlockState(p.set(x, y, z));
                    if (!st.isAir() && !st.canBeReplaced() && !st.hasBlockEntity()) {
                        cuts.add(p.asLong());
                    }
                }
            }
        }
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z); y <= floor; y++) {
                    if (level.getBlockState(p.set(x, y, z)).canBeReplaced()) {
                        fills.add(p.asLong());
                    }
                }
            }
        }
        fills.sort((long a, long b) -> a == b ? 0 : BlockPos.getY(a) != BlockPos.getY(b) ? Integer.compare(BlockPos.getY(a), BlockPos.getY(b)) : Long.compare(a, b));
    }

    private void level(ServerLevel level, Colony c) {
        if (cuts == null) {
            queues(level);
        }
        FakePlayer owner = FakePlayerFactory.get(level, new GameProfile(c.owner, "[autarkia]"));
        int edits = 0;
        while (edits < LEVEL_EDITS_PER_TICK && cutIndex < cuts.size()) {
            BlockPos p = BlockPos.of(cuts.getLong(cutIndex++));
            BlockState st = level.getBlockState(p);
            if (st.isAir() || st.canBeReplaced() || st.hasBlockEntity()) {
                continue;
            }
            edits++;
            if (NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, p, st, owner)).isCanceled()) {
                continue;
            }
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            material++;
        }
        LongArrayList batch = new LongArrayList();
        it.unimi.dsi.fastutil.ints.IntArrayList at = new it.unimi.dsi.fastutil.ints.IntArrayList();
        int scan = fillIndex;
        while (edits + batch.size() < LEVEL_EDITS_PER_TICK && scan < fills.size()) {
            long q = fills.getLong(scan++);
            if (level.getBlockState(BlockPos.of(q)).canBeReplaced()) {
                batch.add(q);
                at.add(scan - 1);
            }
        }
        int allowed = batch.size();
        long need = Math.max(0, batch.size() - material);
        if (need > 0) {
            long pay = MatterBank.withdraw(level, c, need, "Seed") ? need : 0;
            if (pay == 0) {
                long avail = Math.min(need, MatterBank.available(level, c));
                pay = avail > 0 && MatterBank.withdraw(level, c, avail, "Seed") ? avail : 0;
            }
            fillMu += pay;
            allowed = (int) Math.min(batch.size(), material + pay);
            waitingMatter = allowed < batch.size();
            material += pay;
        } else {
            waitingMatter = false;
        }
        for (int i = 0; i < allowed; i++) {
            level.setBlock(BlockPos.of(batch.getLong(i)), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            material--;
            edits++;
        }
        fillIndex = allowed < batch.size() ? at.getInt(allowed) : scan;
        levelEdits += edits;
        maxEditsPerTick = Math.max(maxEditsPerTick, edits);
        if (cutIndex >= cuts.size() && fillIndex >= fills.size()) {
            leveled = true;
            Autarkia.LOG.info("[colony] seed unpack at {}: leveling done at tick {} with {} edits, {} MU for fills, {} cut blocks left over and spent",
                min.toShortString(), t, levelEdits, fillMu, material);
            material = 0;
        }
    }

    private void rise(ServerLevel level) {
        if (riseStartAt < 0) {
            riseStartAt = t;
        }
        int b = cells.size();
        int target = AutarkiaConfig.SEED_TARGET_RISE_TICKS.get();
        acc += b;
        int n = (int) Math.max(1, acc / target);
        n = Math.min(n, AutarkiaConfig.SEED_MAX_RISING_PER_TICK.get());
        n = Math.min(n, b - cursor);
        n = Math.min(n, AutarkiaConfig.SEED_MAX_RISING_ENTITIES.get() - RisingBlockEntity.alive(level));
        if (n <= 0) {
            return;
        }
        acc -= (long) n * target;
        for (int i = 0; i < n; i++) {
            Project.Cell cell = cells.get(cursor++);
            BlockPos p = BlockPos.of(cell.pos());
            RisingBlockEntity.rise(level, p, cell.state());
            if (cursor % SOUND_EVERY == 0) {
                level.playSound(null, p, AutarkiaSounds.of("rising_block"), SoundSource.BLOCKS, 1f, 1f);
            }
        }
        lastSpawnAt = t;
    }

    private void spans(ServerLevel level) {
        spansAt = t;
        int ok = 0;
        List<Project.SpanRec> list = SeedLayout.spans(layout, world);
        for (Project.SpanRec s : list) {
            BlockPos a = BlockPos.of(s.a());
            BlockPos b = BlockPos.of(s.b());
            if (Builds.strung(level, s.a(), s.b())) {
                ok++;
                continue;
            }
            Grid.SpanResult r = Grid.addSpan(level, a, b, s.tier());
            if (r == Grid.SpanResult.OK) {
                ok++;
                Vec3 from = Vec3.atCenterOf(a);
                level.sendParticles(new ElectricArcParticleOptions(Vec3.atCenterOf(b), 0xFF9FE8FF, 1.0F, level.getRandom().nextInt(), 3),
                    from.x, from.y, from.z, 1, 0, 0, 0, 0);
            } else {
                Autarkia.LOG.info("[colony] seed span {} to {} not connected: {}", a.toShortString(), b.toShortString(), r);
            }
        }
        Autarkia.LOG.info("[colony] seed unpack at {}: {} cells placed by tick {}, {} of {} spans connected", min.toShortString(), cells.size(), t, ok,
            list.size());
    }

    private void finish(ServerLevel level, Colony c, Colony.Outpost o, OutpostCoreBlockEntity core) {
        for (int k = 0; k < layout.placed().size(); k++) {
            Pattern pt = layout.placed().get(k).pattern();
            BlockPos origin = SeedLayout.entryMin(layout, world, k);
            net.minecraft.world.level.block.Rotation rot = SeedLayout.entryRotation(layout, world, k);
            Function<BlockPos, BlockPos> map = ColonyPlans.mapper(pt.size(), origin, rot);
            for (var e : pt.data().entrySet()) {
                BlockPos pos = map.apply(SeedLayout.rel(pt, Integer.parseInt(e.getKey())));
                if (level.getBlockEntity(pos) instanceof PatternDataHolder h) {
                    h.readPatternData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), e.getValue()));
                }
            }
            c.instances.add(new Colony.Instance(UUID.randomUUID(), pt.id(), origin, rot, ColonyPlans.rotatedSize(pt.size(), rot), true,
                new long[(pt.volume() + 63) / 64]));
        }
        EnergyBuffer store = core.charge();
        int add = (int) Math.min(Integer.MAX_VALUE - (long) store.stored(), contents.charge());
        store.setCapacity(Math.max(store.capacity(), store.stored() + add));
        store.fill(add);
        int crew = SeedWorkers.unfold(level, c, o, contents.workers());
        ColonyEvents.fire(c, "seed_unpacked");
        ColonyRuntime.of(c).log("Autarky Seed unpacked at " + o.corePos.toShortString() + ": " + cells.size() + " blocks, " + crew + " workers");
        Colonies.changed();
        Autarkia.LOG.info("[colony] {} seed unpacked at {} in {} ticks: {} blocks, charge {} FE, {} of {} workers, max {} rising alive", c.name,
            o.corePos.toShortString(), t, cells.size(), add, crew, contents.workers().size(), maxAlive);
    }

    /** Return the Jade line of the running unpack. */
    public Component jadeLine() {
        if (paused != null) {
            return Component.translatable("jade.autarkia.seed.paused");
        }
        if (t < LEVEL_START) {
            return Component.translatable("jade.autarkia.seed.ring");
        }
        if (!leveled) {
            return Component.translatable(waitingMatter ? "jade.autarkia.seed.matter" : "jade.autarkia.seed.leveling", levelEdits);
        }
        if (cells != null && cursor < cells.size()) {
            return Component.translatable("jade.autarkia.seed.rising", cursor, cells.size());
        }
        return Component.translatable("jade.autarkia.seed.linking");
    }

    /** Write the state (the cell, cut and fill lists are rebuilt from the Patterns and the world on load). */
    public void save(ValueOutput out) {
        out.store("contents", SeedContents.CODEC, contents);
        out.putInt("turns", turns);
        out.store("min", BlockPos.CODEC, min);
        out.putLong("landed", landedAt);
        out.putInt("t", t);
        out.putBoolean("leveled", leveled);
        out.putInt("edits", levelEdits);
        out.putInt("max_edits", maxEditsPerTick);
        out.putLong("material", material);
        out.putLong("fill_mu", fillMu);
        out.putInt("rise_start", riseStartAt);
        out.putInt("cursor", cursor);
        out.putLong("acc", acc);
        out.putInt("last_spawn", lastSpawnAt);
        out.putInt("spans_at", spansAt);
        out.putInt("max_alive", maxAlive);
    }

    /** Read a state written by {@link #save}, or null when its contents do not decode. */
    public static @Nullable SeedUnpack load(ValueInput in) {
        SeedContents contents = in.read("contents", SeedContents.CODEC).orElse(null);
        BlockPos min = in.read("min", BlockPos.CODEC).orElse(null);
        if (contents == null || min == null) {
            Autarkia.LOG.warn("[colony] a saved seed unpack does not decode; it is dropped");
            return null;
        }
        SeedUnpack s = new SeedUnpack(contents, in.getIntOr("turns", 0), min, in.getLongOr("landed", 0L));
        s.t = in.getIntOr("t", 0);
        s.leveled = in.getBooleanOr("leveled", false);
        s.levelEdits = in.getIntOr("edits", 0);
        s.maxEditsPerTick = in.getIntOr("max_edits", 0);
        s.material = in.getLongOr("material", 0L);
        s.fillMu = in.getLongOr("fill_mu", 0L);
        s.riseStartAt = in.getIntOr("rise_start", -1);
        s.cursor = in.getIntOr("cursor", 0);
        s.acc = in.getLongOr("acc", 0L);
        s.lastSpawnAt = in.getIntOr("last_spawn", -1);
        s.spansAt = in.getIntOr("spans_at", -1);
        s.maxAlive = in.getIntOr("max_alive", 0);
        return s;
    }
}

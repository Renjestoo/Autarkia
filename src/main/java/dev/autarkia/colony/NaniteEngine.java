package dev.autarkia.colony;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import com.mojang.authlib.GameProfile;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.NaniteBubblePayload;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.AutarkiaTags;
import dev.autarkia.registry.EventTrigger;
import dev.autarkia.storage.MatterBank;
import dev.autarkia.storage.MatterBufferBlockEntity;
import dev.autarkia.storage.MatterValues;
import dev.autarkia.worker.NaniteSwarmEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.jspecify.annotations.Nullable;

/**
 * The nanite claim pass (05 section 8.3) on {@code LevelTickEvent.Post}. Once per server tick the first level pass
 * splits {@code nanite.globalEditsPerTick} over every running claim of the server, oldest first ({@link
 * NaniteClaims#shares}); a claim spends its share and the {@code naniteBlocksPerSecond} allowance of its working swarms
 * (C12). One edit is 8 units; an air cell, a kept cell, an immune cell, a fluid wall or a cancelled break is 1 unit
 * (G27). A solid cell costs {@code min(feCap, feBase + fePerHardness * destroyTime)} FE from the swarm (C3), fires a
 * {@link BreakBlockEvent} with a fake player of the colony owner (G4), drops with a netherite tool chosen by the
 * {@code minecraft:mineable/*} tags (C4) and sends ores and raw ores as items on a stream to the linked matter buffer,
 * other valued drops as free MU, the rest at the swarm (C6). Every block entity stays; those holding items get the
 * bubble (C7). Fluids follow the 05 guard (C16).
 */
public final class NaniteEngine {
    /** Units of one edit; an air cell costs one unit (1/8 edit). */
    public static final int EDIT_UNITS = 8;
    /** FE an inner fluid source costs (05 section 8.3). */
    public static final int SOURCE_FE = 100;
    /** Ticks the bubbles show after a claim ends. */
    public static final int BUBBLE_TICKS = 600;
    private static final int RING_PERIOD = 20;
    private static final int LINK_RECHECK = 100;
    private static final List<Map.Entry<String, Integer>> ORE_COLORS = List.of(Map.entry("vantite", 0x6A4CFF), Map.entry("iron", 0xD8AF93),
        Map.entry("copper", 0xE07A3E), Map.entry("gold", 0xFCEE4B), Map.entry("tin", 0xC9D3DA), Map.entry("zinc", 0x9FB7B0), Map.entry("coal", 0x303030),
        Map.entry("redstone", 0xFF2020), Map.entry("lapis", 0x2A4BD7), Map.entry("diamond", 0x5CE1E6), Map.entry("emerald", 0x17DD62));
    private static final TagKey<Item> C_ORE_ITEMS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores"));
    private static final TagKey<Block> C_ORE_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));
    private static long planTick = Long.MIN_VALUE;
    private static final Map<UUID, Integer> SHARES = new HashMap<>();
    private static final Map<UUID, BlockPos> LINKS = new HashMap<>();
    private static final Map<UUID, Long> LINKED_AT = new HashMap<>();
    private static final Map<UUID, Integer> MISSING = new HashMap<>();
    private static final int MISSING_CHECKS = 3;
    private static int unitsThisTick;
    private static int maxUnits;
    private static long streamTick = Long.MIN_VALUE;
    private static int streamsThisTick;

    private enum Kind { SKIP, FLUID_FREE, FLUID_SOURCE, KEEP, SOLID }

    private enum Outcome { EDITED, CANCELLED, WAIT_FE, WAIT_BUFFER }

    private NaniteEngine() {
    }

    static void clear() {
        planTick = Long.MIN_VALUE;
        SHARES.clear();
        LINKS.clear();
        LINKED_AT.clear();
        MISSING.clear();
        unitsThisTick = 0;
        maxUnits = 0;
        streamsThisTick = 0;
    }

    /** Return the edits (1/8 per unit) run so far in the current server tick, over all levels. */
    public static double editsLastTick() {
        return unitsThisTick / (double) EDIT_UNITS;
    }

    /** Return the most edits run in one server tick since the last {@link #resetMax()}. */
    public static double maxEditsPerTick() {
        return maxUnits / (double) EDIT_UNITS;
    }

    /** Forget the highest edits-per-tick figure. */
    public static void resetMax() {
        maxUnits = 0;
    }

    /** Take one of the {@code nanite.maxStreamsPerTick} ore streams of this server tick; returns false when they are used up. */
    public static boolean tryStream(MinecraftServer server) {
        if (server.getTickCount() != streamTick) {
            streamTick = server.getTickCount();
            streamsThisTick = 0;
        }
        if (streamsThisTick >= AutarkiaConfig.NANITE_MAX_STREAMS_PER_TICK.get()) {
            return false;
        }
        streamsThisTick++;
        return true;
    }

    /** Return the FE of one solid edit for a destroy time: min(feCap, feBase + fePerHardness * destroyTime). */
    public static int feCost(float destroyTime) {
        return (int) Math.min(AutarkiaConfig.NANITE_FE_CAP.get(),
            AutarkiaConfig.NANITE_FE_BASE.get() + Math.round(AutarkiaConfig.NANITE_FE_PER_HARDNESS.get() * (double) destroyTime));
    }

    /** Return the ARGB stream color of an ore block by its id (05 section 8.3); unknown ores are white. */
    public static int oreColor(BlockState s) {
        Set<String> words = Set.of(BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().split("_"));
        for (Map.Entry<String, Integer> e : ORE_COLORS) {
            if (words.contains(e.getKey())) {
                return 0xFF000000 | e.getValue();
            }
        }
        return 0xFFFFFFFF;
    }

    /** Return whether a drop stays an item (05 section 8.3: c:ores and raw ores). */
    public static boolean keepsItem(ItemStack s) {
        return s.is(C_ORE_ITEMS) || s.is(Tags.Items.RAW_MATERIALS);
    }

    private static boolean oreCell(BlockState s) {
        return s.is(BlockTags.ORES) || s.is(C_ORE_BLOCKS);
    }

    static void tick(ServerLevel level) {
        if (Colonies.server() == null) {
            return;
        }
        if (level.getServer().getTickCount() != planTick) {
            plan(level.getServer().getTickCount());
        }
        long t0 = dev.autarkia.command.ColonyProfiler.active() ? System.nanoTime() : 0;
        long gt = level.getGameTime();
        for (Colony c : List.copyOf(Colonies.registry().all())) {
            if (c.dimension.equals(level.dimension())) {
                for (NaniteClaim cl : List.copyOf(c.claims)) {
                    claim(level, c, cl, gt);
                }
            }
        }
        if (t0 != 0) {
            dev.autarkia.command.ColonyProfiler.add("nanite.claims", System.nanoTime() - t0);
        }
    }

    private static void plan(long tick) {
        planTick = tick;
        unitsThisTick = 0;
        SHARES.clear();
        if (!NaniteClaims.running()) {
            return;
        }
        List<NaniteClaim> act = new ArrayList<>();
        for (Colony c : Colonies.registry().all()) {
            for (NaniteClaim cl : c.claims) {
                if (cl.state != NaniteClaim.State.DONE && cl.state != NaniteClaim.State.PAUSED) {
                    act.add(cl);
                }
            }
        }
        act.sort(Comparator.<NaniteClaim>comparingLong(cl -> cl.started).thenComparing(cl -> cl.id));
        int[] s = NaniteClaims.shares(AutarkiaConfig.NANITE_GLOBAL_EDITS_PER_TICK.get(), act.size());
        for (int i = 0; i < s.length; i++) {
            SHARES.put(act.get(i).id, s[i]);
        }
    }

    private static void claim(ServerLevel level, Colony c, NaniteClaim cl, long gt) {
        NaniteClaims.Stats st = NaniteClaims.stats(cl.id);
        if (gt % LINK_RECHECK == 0) {
            validateSwarms(level, c, cl);
        }
        if (gt % RING_PERIOD == 0) {
            bubbles(level, cl, st, gt);
        }
        if (cl.state == NaniteClaim.State.DONE) {
            if (cl.swarms.isEmpty() && gt - cl.ended > BUBBLE_TICKS) {
                c.claims.remove(cl);
                NaniteClaims.forget(cl.id);
                LINKS.remove(cl.id);
                LINKED_AT.remove(cl.id);
                Colonies.changed();
            }
            return;
        }
        if (cl.state == NaniteClaim.State.PAUSED || !NaniteClaims.running()) {
            return;
        }
        List<NaniteSwarmEntity> working = new ArrayList<>();
        for (UUID id : cl.swarms) {
            if (level.getEntity(id) instanceof NaniteSwarmEntity s && s.phase() == NaniteSwarmEntity.Phase.DISASSEMBLE) {
                working.add(s);
            }
        }
        if (working.isEmpty()) {
            return;
        }
        List<NaniteSwarmEntity> crew = working.stream().filter(s -> s.tethered() && s.energy() > 0).toList();
        if (crew.isEmpty()) {
            cl.state = NaniteClaim.State.WAIT_FE;
            return;
        }
        cl.normalize();
        if (cl.exhausted()) {
            finish(level, c, cl, gt);
            return;
        }
        if (!layerLoaded(level, cl)) {
            cl.state = NaniteClaim.State.WAIT_CHUNK;
            return;
        }
        MatterBufferBlockEntity buffer = link(level, c, cl, gt);
        if (buffer == null) {
            cl.state = NaniteClaim.State.WAIT_BUFFER;
            return;
        }
        run(level, c, cl, st, crew, buffer, SHARES.getOrDefault(cl.id, 0) * EDIT_UNITS, gt);
    }

    private static void run(ServerLevel level, Colony c, NaniteClaim cl, NaniteClaims.Stats st, List<NaniteSwarmEntity> crew, MatterBufferBlockEntity buffer,
                            int budget, long gt) {
        NaniteClaim.State result = NaniteClaim.State.RUN;
        boolean edited = false;
        while (budget > 0) {
            int layerBefore = cl.cursorY;
            cl.normalize();
            if (cl.exhausted()) {
                finish(level, c, cl, gt);
                return;
            }
            if (cl.cursorY != layerBefore && !layerLoaded(level, cl)) {
                result = NaniteClaim.State.WAIT_CHUNK;
                break;
            }
            BlockPos p = cl.cell(cl.cursorY, cl.cursorI);
            BlockState s = level.getBlockState(p);
            Kind kind = classify(level, c, cl, p, s);
            int units = kind == Kind.SOLID || kind == Kind.FLUID_SOURCE ? EDIT_UNITS : 1;
            if (units > budget) {
                break;
            }
            NaniteSwarmEntity sw = pick(crew, units);
            if (sw == null) {
                break;
            }
            switch (kind) {
                case SOLID -> {
                    Outcome o = solid(level, c, p, s, sw, buffer, st);
                    if (o == Outcome.WAIT_FE || o == Outcome.WAIT_BUFFER) {
                        result = o == Outcome.WAIT_FE ? NaniteClaim.State.WAIT_FE : NaniteClaim.State.WAIT_BUFFER;
                        budget = 0;
                        continue;
                    }
                    if (o == Outcome.CANCELLED) {
                        units = 1;
                    }
                }
                case FLUID_SOURCE -> {
                    if (sw.energy() < SOURCE_FE) {
                        result = NaniteClaim.State.WAIT_FE;
                        budget = 0;
                        continue;
                    }
                    sw.spend(SOURCE_FE);
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    cloud(level, p);
                }
                case FLUID_FREE -> level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                case KEEP -> {
                    if (!cl.kept.contains(p)) {
                        cl.kept.add(p.immutable());
                    }
                }
                default -> {
                }
            }
            sw.useAllowance(units);
            budget -= units;
            st.units += units;
            unitsThisTick += units;
            maxUnits = Math.max(maxUnits, unitsThisTick);
            cl.cursorI++;
            edited = true;
        }
        cl.state = result;
        if (edited) {
            Colonies.changed();
        }
    }

    private static @Nullable NaniteSwarmEntity pick(List<NaniteSwarmEntity> crew, int units) {
        NaniteSwarmEntity best = null;
        for (NaniteSwarmEntity s : crew) {
            if (s.allowance() >= units && (best == null || s.allowance() > best.allowance())) {
                best = s;
            }
        }
        return best;
    }

    private static Kind classify(ServerLevel level, Colony c, NaniteClaim cl, BlockPos p, BlockState s) {
        if (s.isAir() || !Colonies.inside(c, p)) {
            return Kind.SKIP;
        }
        if (s.getBlock() instanceof LiquidBlock) {
            if (fluidWall(level, cl, p)) {
                return Kind.SKIP;
            }
            return s.getFluidState().isSource() ? Kind.FLUID_SOURCE : Kind.FLUID_FREE;
        }
        if (s.getDestroySpeed(level, p) < 0 || s.is(AutarkiaTags.NANITE_IMMUNE)) {
            return Kind.SKIP;
        }
        if (s.hasBlockEntity()) {
            return holdsItems(level, p) ? Kind.KEEP : Kind.SKIP;
        }
        return fluidWall(level, cl, p) ? Kind.SKIP : Kind.SOLID;
    }

    private static boolean fluidWall(ServerLevel level, NaniteClaim cl, BlockPos p) {
        for (Direction d : Direction.values()) {
            BlockPos n = p.relative(d);
            if (!cl.inShape(n) && !level.getFluidState(n).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean holdsItems(ServerLevel level, BlockPos p) {
        var h = level.getCapability(Capabilities.Item.BLOCK, p, null);
        if (h != null) {
            for (int i = 0; i < h.size(); i++) {
                if (h.getAmountAsLong(i) > 0) {
                    return true;
                }
            }
        }
        return level.getBlockEntity(p) instanceof net.minecraft.world.Container ct && !ct.isEmpty();
    }

    private static Outcome solid(ServerLevel level, Colony c, BlockPos p, BlockState s, NaniteSwarmEntity sw, MatterBufferBlockEntity buffer,
                                 NaniteClaims.Stats st) {
        int fe = feCost(s.getDestroySpeed(level, p));
        if (sw.energy() < fe) {
            return Outcome.WAIT_FE;
        }
        ItemStack tool = new ItemStack(s.is(BlockTags.MINEABLE_WITH_SHOVEL) ? Items.NETHERITE_SHOVEL
            : s.is(BlockTags.MINEABLE_WITH_AXE) ? Items.NETHERITE_AXE : Items.NETHERITE_PICKAXE);
        List<ItemStack> items = new ArrayList<>();
        List<ItemStack> loose = new ArrayList<>();
        long mu = 0;
        int count = 0;
        for (ItemStack d : Block.getDrops(s, level, p, null, sw, tool)) {
            if (d.isEmpty()) {
                continue;
            }
            if (keepsItem(d)) {
                items.add(d);
                count += d.getCount();
                continue;
            }
            long v = MatterValues.of(d, c.tier);
            if (v > 0) {
                mu += v * d.getCount();
            } else {
                loose.add(d);
            }
        }
        if (mu > buffer.capacity() - buffer.used() - buffer.free() || count > 0 && sw.heldCount() + count > NaniteSwarmEntity.MAX_ITEMS) {
            return Outcome.WAIT_BUFFER;
        }
        BreakBlockEvent ev = new BreakBlockEvent(level, p, s, FakePlayerFactory.get(level, new GameProfile(c.owner, "[autarkia]")));
        NeoForge.EVENT_BUS.post(ev);
        if (ev.isCanceled()) {
            return Outcome.CANCELLED;
        }
        sw.spend(fe);
        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (mu > 0) {
            long took = buffer.addFree(mu);
            if (took < mu) {
                Autarkia.LOG.warn("[nanite] buffer at {} took {} of {} MU after the room check", buffer.getBlockPos().toShortString(), took, mu);
            }
        }
        if (oreCell(s) || !items.isEmpty()) {
            sw.queueStream(Vec3.atCenterOf(p), oreColor(s), items, buffer.getBlockPos());
        }
        loose.forEach(sw::dropAtSwarm);
        cloud(level, p);
        level.playSound(null, p, AutarkiaSounds.of("entity.nanite_swarm.consume"), SoundSource.NEUTRAL, 1f, 1f);
        if (st.firstEditY == Integer.MIN_VALUE) {
            st.firstEditY = p.getY();
        }
        return Outcome.EDITED;
    }

    private static void cloud(ServerLevel level, BlockPos p) {
        level.sendParticles(GlowParticleOptions.of(0xFF9AA0A6).size(0.08f), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.01);
    }

    private static boolean layerLoaded(ServerLevel level, NaniteClaim cl) {
        int[] r = cl.rect(cl.cursorY);
        for (int cx = r[0] >> 4; cx <= r[1] >> 4; cx++) {
            for (int cz = r[2] >> 4; cz <= r[3] >> 4; cz++) {
                if (!level.getChunkSource().hasChunk(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static @Nullable MatterBufferBlockEntity link(ServerLevel level, Colony c, NaniteClaim cl, long gt) {
        BlockPos at = LINKS.get(cl.id);
        Long when = LINKED_AT.get(cl.id);
        if (at != null && when != null && gt - when < LINK_RECHECK && level.getBlockEntity(at) instanceof MatterBufferBlockEntity b) {
            return b;
        }
        MatterBufferBlockEntity b = linkedBuffer(level, c, cl.hivePos);
        if (b == null) {
            LINKS.remove(cl.id);
            return null;
        }
        LINKS.put(cl.id, b.getBlockPos());
        LINKED_AT.put(cl.id, gt);
        return b;
    }

    /**
     * Return the buffer a hive delivers to (G30): the nearest formed matter buffer within 64 blocks of the hive inside the
     * colony domain, else null. The matter buffer takes no grid power, so it is never on the hive's power network and
     * the G30 network rule has no case to match.
     */
    public static @Nullable MatterBufferBlockEntity linkedBuffer(ServerLevel level, Colony c, BlockPos hivePos) {
        MatterBufferBlockEntity near = MatterBank.nearest(level, hivePos, NaniteClaims.HIVE_RANGE);
        return near != null && Colonies.at(level, near.getBlockPos()) == c ? near : null;
    }

    private static void validateSwarms(ServerLevel level, Colony c, NaniteClaim cl) {
        boolean changed = cl.swarms.removeIf(id -> {
            Colony.WorkerEntry e = c.workers.get(id);
            if (level.getEntity(id) instanceof NaniteSwarmEntity
                || e != null && !level.getChunkSource().hasChunk(ChunkPos.getX(e.chunk()), ChunkPos.getZ(e.chunk()))) {
                MISSING.remove(id);
                return false;
            }
            if (MISSING.merge(id, 1, Integer::sum) < MISSING_CHECKS) {
                return false;
            }
            MISSING.remove(id);
            c.workers.remove(id);
            Autarkia.LOG.warn("[nanite] swarm {} of claim {} is gone from its loaded chunk; the claim releases it", id.toString().substring(0, 8),
                cl.id.toString().substring(0, 8));
            return true;
        });
        if (changed) {
            Colonies.changed();
        }
    }

    private static void finish(ServerLevel level, Colony c, NaniteClaim cl, long gt) {
        cl.state = NaniteClaim.State.DONE;
        cl.ended = gt;
        cl.cursorY = cl.min.getY() - 1;
        cl.cursorI = 0;
        for (UUID id : cl.swarms) {
            if (level.getEntity(id) instanceof NaniteSwarmEntity s) {
                s.claimDone();
            }
        }
        ColonyEvents.fire(c, "claim_finished");
        ColonyEvents.worker(c, "claim_complete");
        ServerPlayer starter = NaniteClaims.starter(cl.id);
        if (starter != null && level.getServer().getPlayerList().getPlayer(starter.getUUID()) != starter) {
            EventTrigger.COLONY.get().fire(starter, "claim_finished");
            EventTrigger.WORKER.get().fire(starter, "claim_complete");
        }
        ColonyEvents.alert(c, "message.autarkia.nanite.done", cl.min.toShortString(), cl.max.toShortString());
        Colonies.changed();
        Autarkia.LOG.info("[nanite] claim {} done: {} edit units, {} streams, {} kept", cl.id.toString().substring(0, 8), NaniteClaims.stats(cl.id).units,
            NaniteClaims.stats(cl.id).streams, cl.kept.size());
    }

    private static void bubbles(ServerLevel level, NaniteClaim cl, NaniteClaims.Stats st, long gt) {
        if (cl.kept.isEmpty() || cl.state == NaniteClaim.State.DONE && gt - cl.ended > BUBBLE_TICKS) {
            return;
        }
        List<BlockPos> ring = new ArrayList<>();
        for (BlockPos p : cl.kept) {
            if (level.getBlockEntity(p) == null) {
                continue;
            }
            boolean free = true;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (cl.inShape(n) && !cl.processed(n)) {
                    free = false;
                    break;
                }
            }
            if (free) {
                ring.add(p);
            }
        }
        if (ring.isEmpty()) {
            return;
        }
        for (BlockPos p : ring) {
            if (st.bubbled.add(p)) {
                level.playSound(null, p, AutarkiaSounds.of("nanite_bubble"), SoundSource.BLOCKS, 1f, 1f);
            }
        }
        AutarkiaNetwork.toTracking(level, ChunkPos.containing(ring.get(0)), new NaniteBubblePayload(List.copyOf(ring.subList(0, Math.min(ring.size(), 1024)))));
    }
}

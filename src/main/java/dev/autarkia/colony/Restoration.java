package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlocks;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.nikdo53.tinymultiblocklib.block.IMultiBlock;
import net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The Restoration Field service (05 section 8.5): the projectors tick in a rotating order after the block entities of
 * their level under the server budgets ({@code restoration.scanCellsPerTick} scan cells and
 * {@code restoration.globalCellsPerTick} placed cells per server tick, {@code restoration.cellsPerTickPerProjector} per
 * projector); a break or placement by the owner or a trusted player inside a field box updates its snapshot (accept, one
 * tick later, the whole big block or door and bed pair included); a blast that hits a field box opens its event window of
 * 200 ticks. All listeners run at the lowest priority, so the explosion absorb hook has already removed the domain blocks
 * it saves. A colony member removed by an explosion, a mob or a non-trusted player records the failure memory
 * {@code machine_destroyed} (key: its chunk) that template 29 answers.
 */
public final class Restoration {
    public static final int EVENT_WINDOW = 200;
    public static final int EVENT_CELLS = 64;
    public static final String MEMORY = "machine_destroyed";

    /** The server budget of the current tick. */
    static final class Budget {
        int scan;
        int place;
    }

    private static final Budget BUDGET = new Budget();
    private static final Map<ServerLevel, List<RestorationProjectorBlockEntity>> QUEUED = new IdentityHashMap<>();
    private static final Map<ServerLevel, LongLinkedOpenHashSet> ACCEPTS = new IdentityHashMap<>();

    private Restoration() {
    }

    /** Register the game event listeners. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre e) -> {
            BUDGET.scan = AutarkiaConfig.RESTORATION_SCAN_CELLS_PER_TICK.get();
            BUDGET.place = AutarkiaConfig.RESTORATION_GLOBAL_CELLS_PER_TICK.get();
        });
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
            if (e.getLevel() instanceof ServerLevel sl) {
                tick(sl);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (BreakBlockEvent e) -> {
            if (e.getLevel() instanceof ServerLevel sl && Colonies.server() != null) {
                broken(sl, e.getPos(), e.getState(), e.getPlayer());
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (BlockEvent.EntityPlaceEvent e) -> {
            if (e.getLevel() instanceof ServerLevel sl && Colonies.server() != null && e.getEntity() instanceof Player p) {
                Colony c = Colonies.at(sl, e.getPos());
                if (c != null && c.canManage(p.getUUID()) && covered(c, e.getPos())) {
                    ACCEPTS.computeIfAbsent(sl, k -> new LongLinkedOpenHashSet()).add(e.getPos().asLong());
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ExplosionEvent.Detonate e) -> {
            if (e.getLevel() instanceof ServerLevel sl && Colonies.server() != null) {
                exploded(sl, e.getAffectedBlocks());
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingDestroyBlockEvent e) -> {
            if (e.getEntity().level() instanceof ServerLevel sl && Colonies.server() != null) {
                destroyed(sl, List.of(e.getPos()));
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            QUEUED.clear();
            ACCEPTS.clear();
        });
    }

    /** Queue a projector for this tick's work; called from its block entity tick. */
    static void enqueue(@Nullable ServerLevel level, RestorationProjectorBlockEntity p) {
        if (level != null) {
            QUEUED.computeIfAbsent(level, k -> new ArrayList<>()).add(p);
        }
    }

    private static void tick(ServerLevel level) {
        LongLinkedOpenHashSet accepts = ACCEPTS.remove(level);
        if (accepts != null) {
            accepts.forEach(p -> accept(level, BlockPos.of(p)));
        }
        List<RestorationProjectorBlockEntity> list = QUEUED.remove(level);
        if (list == null || list.isEmpty()) {
            return;
        }
        int start = (int) Math.floorMod(level.getGameTime(), (long) list.size());
        for (int k = 0; k < list.size(); k++) {
            RestorationProjectorBlockEntity p = list.get((start + k) % list.size());
            if (!p.isRemoved() && p.getLevel() == level) {
                p.work(level, BUDGET);
            }
        }
    }

    /** Return whether pos lies in a field box of the colony ({@code pattern_repair} leaves such cells to the field). */
    public static boolean covered(Colony c, BlockPos pos) {
        return owner(c, pos) != null;
    }

    /** Return the field that restores pos: the oldest field of the colony whose box holds it, or null. */
    public static @Nullable RestorationField owner(Colony c, BlockPos pos) {
        for (RestorationField f : c.fields) {
            if (f.contains(pos)) {
                return f;
            }
        }
        return null;
    }

    /** Return the second half of a door, tall plant or bed pair at p, or null for a single block. */
    public static @Nullable BlockPos partner(BlockPos p, BlockState s) {
        if (s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? p.above() : p.below();
        }
        if (s.hasProperty(BlockStateProperties.BED_PART) && s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction f = s.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return s.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT ? p.relative(f) : p.relative(f.getOpposite());
        }
        return null;
    }

    /** Return whether the state is the second half of a pair (upper door half, bed head); it is restored with its first half. */
    public static boolean secondary(BlockState s) {
        return s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
            || s.hasProperty(BlockStateProperties.BED_PART) && s.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD;
    }

    private static List<BlockPos> shape(ServerLevel level, BlockPos pos, BlockState state) {
        List<BlockPos> out = new ArrayList<>();
        if (IMultiBlock.isMultiblock(level, pos)) {
            out.addAll(IMultiBlock.getFullShape(level, pos));
        } else {
            out.add(pos.immutable());
        }
        BlockPos partner = partner(pos, state);
        if (partner != null) {
            out.add(partner);
        }
        return out;
    }

    private static void broken(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        Colony c = Colonies.at(level, pos);
        if (c == null) {
            return;
        }
        if (!c.canManage(player.getUUID())) {
            destroyed(level, List.of(pos));
            return;
        }
        if (covered(c, pos)) {
            LongLinkedOpenHashSet set = ACCEPTS.computeIfAbsent(level, k -> new LongLinkedOpenHashSet());
            shape(level, pos, state).forEach(p -> set.add(p.asLong()));
        }
    }

    private static void accept(ServerLevel level, BlockPos pos) {
        Colony c = Colonies.at(level, pos);
        if (c == null || !level.isLoaded(pos)) {
            return;
        }
        List<BlockPos> cells = shape(level, pos, level.getBlockState(pos));
        for (RestorationField f : c.fields) {
            if (!f.contains(pos)) {
                continue;
            }
            Snapshot s = Snapshot.load(level.getServer(), f.snapshot());
            if (s != null) {
                cells.forEach(q -> s.accept(level, q));
                Autarkia.LOG.info("[restore] field {} accepted {} cells at {}", f.projectorPos().toShortString(), cells.size(), pos.toShortString());
            }
        }
    }

    private static void exploded(ServerLevel level, List<BlockPos> affected) {
        long now = level.getGameTime();
        List<RestorationProjectorBlockEntity> hit = new ArrayList<>();
        for (BlockPos p : affected) {
            Colony c = Colonies.at(level, p);
            if (c == null) {
                continue;
            }
            for (RestorationField f : c.fields) {
                if (f.contains(p) && level.isLoaded(f.projectorPos()) && level.getBlockEntity(f.projectorPos()) instanceof RestorationProjectorBlockEntity rp
                    && !hit.contains(rp)) {
                    hit.add(rp);
                }
            }
        }
        hit.forEach(rp -> rp.blastHit(now));
        destroyed(level, affected);
    }

    private static void destroyed(ServerLevel level, List<BlockPos> positions) {
        LongArrayList seen = new LongArrayList();
        for (BlockPos p : positions) {
            BlockEntity be = level.getBlockEntity(p);
            if (be == null) {
                continue;
            }
            Identifier type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
            if (type == null || !type.getNamespace().equals(Autarkia.MOD_ID)) {
                continue;
            }
            BlockPos center = be instanceof IMultiBlockEntity m ? m.getCenter() : p;
            if (seen.contains(center.asLong())) {
                continue;
            }
            seen.add(center.asLong());
            ColonyMemory.at(level, center, MEMORY, Long.toString(ChunkPos.pack(center.getX() >> 4, center.getZ() >> 4)));
        }
    }

    /** Remove the field of a removed projector from its colony and delete its snapshot file. */
    static void removed(ServerLevel level, BlockPos pos, @Nullable UUID snapshot) {
        if (Colonies.server() == null) {
            return;
        }
        for (Colony c : Colonies.registry().all()) {
            if (c.dimension.equals(level.dimension()) && c.fields.removeIf(f -> f.projectorPos().equals(pos))) {
                Colonies.changed();
                Autarkia.LOG.info("[restore] {} removed its field at {}", c.name, pos.toShortString());
            }
        }
        if (snapshot != null) {
            Snapshot.delete(level.getServer(), snapshot);
        }
    }

    /**
     * Return the template 29 anchor (site rule {@code SR_EVENT_CLUSTER}, M9 decision G22): the {@code machine_destroyed}
     * memory with the highest current weight of at least 1 whose position no field covers, then the mean position of
     * the memories with its key; null when there is none.
     */
    public static @Nullable BlockPos clusterAnchor(Colony c) {
        double half = AutarkiaConfig.MEMORY_HALF_LIFE_TICKS.get();
        Colony.Memory best = null;
        for (Colony.Memory m : c.memories) {
            double w = m.weightAt(c.clock, half);
            if (!m.type().equals(MEMORY) || w < 1 || covered(c, m.pos())) {
                continue;
            }
            if (best == null || w > best.weightAt(c.clock, half) || w == best.weightAt(c.clock, half) && m.pos().asLong() < best.pos().asLong()) {
                best = m;
            }
        }
        if (best == null) {
            return null;
        }
        long x = 0;
        long y = 0;
        long z = 0;
        int n = 0;
        for (Colony.Memory m : c.memories) {
            if (m.type().equals(MEMORY) && m.key().equals(best.key())) {
                x += m.pos().getX();
                y += m.pos().getY();
                z += m.pos().getZ();
                n++;
            }
        }
        return new BlockPos((int) (x / n), (int) (y / n), (int) (z / n));
    }

    /** Give a projector a colony project built (template 29) the default box and start its snapshot. */
    public static void planBuilt(Colony c, ServerLevel level, Project p) {
        for (Project.Cell cell : p.cells) {
            BlockPos pos = BlockPos.of(cell.pos());
            if (cell.state().is(AutarkiaBlocks.RESTORATION_PROJECTOR.get()) && level.getBlockEntity(pos) instanceof RestorationProjectorBlockEntity rp) {
                rp.defaultBox();
                boolean started = rp.takeSnapshot();
                ColonyRuntime.of(c).log("Restoration field at " + pos.toShortString() + (started ? ": snapshot started" : ": outside the domain"));
            }
        }
    }

    /** Return one status line per field of every colony ({@code /autarkia restore status}, 05b section 11.1). */
    public static List<String> status() {
        List<String> out = new ArrayList<>();
        if (Colonies.server() == null) {
            return out;
        }
        for (Colony c : Colonies.registry().all()) {
            ServerLevel level = Colonies.level(c);
            for (RestorationField f : c.fields) {
                String box = (f.max().getX() - f.min().getX() + 1) + "x" + (f.max().getY() - f.min().getY() + 1) + "x" + (f.max().getZ() - f.min().getZ() + 1);
                if (level != null && level.isLoaded(f.projectorPos()) && level.getBlockEntity(f.projectorPos()) instanceof RestorationProjectorBlockEntity rp) {
                    long[] n = rp.restoredCounts();
                    out.add(c.name + ": field " + f.projectorPos().toShortString() + " " + box + ", missing " + rp.missing() + ", queued "
                        + rp.run().queued() + ", " + rp.status().name().toLowerCase(java.util.Locale.ROOT) + ", restored " + n[0] + " (" + n[1]
                        + " from debris, " + n[2] + " from matter, " + n[3] + " without material), FE " + rp.energy().stored() + "/" + rp.energy().capacity());
                } else {
                    out.add(c.name + ": field " + f.projectorPos().toShortString() + " " + box + ", projector not loaded");
                }
            }
        }
        if (out.isEmpty()) {
            out.add("No restoration fields");
        }
        return out;
    }
}

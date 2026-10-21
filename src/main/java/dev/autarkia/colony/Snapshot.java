package dev.autarkia.colony;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.Autarkia;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueOutput;
import net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The snapshot of a Restoration Field (05 sections 1.5 and 8.5): global server save data {@code autarkia:snapshot_<32hex>}
 * (file {@code <world>/data/autarkia/snapshot_<32hex>.dat}), rewritten on snapshot and accept. Cells are kept in x, z, y
 * order like a Pattern: a palette (index 0 = a non-center part of a big block, never restored on its own; air is a real
 * state) and, in the file, packed palette indices with {@code ceil(log2(paletteSize))} bits. The configuration data of
 * Autarkia block entities ({@link PatternDataHolder}) is stored by cell index; foreign block entities keep their state only.
 */
public final class Snapshot extends SavedData {
    public static final int VERSION = 1;
    public static final int CELLS_PER_TICK = 8192;
    private static final Map<UUID, SavedDataType<Snapshot>> TYPES = new ConcurrentHashMap<>();
    private static final Codec<long[]> LONGS = Codec.LONG_STREAM.xmap(LongStream::toArray, LongStream::of);

    private static final Codec<Snapshot> RAW = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.optionalFieldOf("version", VERSION).forGetter(s -> VERSION),
        UUIDUtil.CODEC.fieldOf("id").forGetter(s -> s.id),
        BlockPos.CODEC.fieldOf("min").forGetter(s -> s.min),
        Vec3i.CODEC.fieldOf("size").forGetter(s -> s.size),
        BlockState.CODEC.listOf().fieldOf("palette").forGetter(s -> s.palette.subList(1, s.palette.size())),
        Codec.INT.fieldOf("bits").forGetter(s -> Pattern.bitsFor(s.palette.size())),
        LONGS.fieldOf("cells").forGetter(s -> Pattern.pack(s.cells, Pattern.bitsFor(s.palette.size()))),
        Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC).optionalFieldOf("data", Map.of()).forGetter(Snapshot::dataByKey),
        Codec.LONG.optionalFieldOf("created", 0L).forGetter(s -> s.created)
    ).apply(i, Snapshot::decode));

    public static final Codec<Snapshot> CODEC = RAW.validate(s -> s.cells.length == s.volume() ? DataResult.success(s)
        : DataResult.error(() -> "Snapshot " + s.id + " holds " + s.cells.length + " cells for a box of " + s.volume()));

    public final UUID id;
    public final BlockPos min;
    public final Vec3i size;
    public final long created;
    private final List<@Nullable BlockState> palette = new ArrayList<>();
    private final Map<BlockState, Integer> lookup = new HashMap<>();
    private final int[] cells;
    private final Map<Integer, CompoundTag> data = new LinkedHashMap<>();
    private boolean deleted;

    private Snapshot(UUID id, BlockPos min, Vec3i size, long created, int[] cells) {
        this.id = id;
        this.min = min.immutable();
        this.size = size;
        this.created = created;
        this.cells = cells;
        palette.add(null);
    }

    private static Snapshot decode(int version, UUID id, BlockPos min, Vec3i size, List<BlockState> states, int bits, long[] packed,
                                   Map<String, CompoundTag> data, long created) {
        int volume = size.getX() * size.getY() * size.getZ();
        int[] cells = new int[volume];
        int perLong = bits == 0 ? 0 : 64 / bits;
        if (bits > 0 && packed.length * (long) perLong >= volume) {
            for (int i = 0; i < volume; i++) {
                cells[i] = (int) (packed[i / perLong] >>> (i % perLong * bits) & ((1L << bits) - 1));
            }
        } else if (bits > 0) {
            cells = new int[0];
        }
        Snapshot s = new Snapshot(id, min, size, created, cells);
        states.forEach(s::paletteIndex);
        data.forEach((k, v) -> s.data.put(Integer.parseInt(k), v));
        return s;
    }

    private Map<String, CompoundTag> dataByKey() {
        Map<String, CompoundTag> out = new LinkedHashMap<>();
        data.forEach((k, v) -> out.put(Integer.toString(k), v));
        return out;
    }

    private int paletteIndex(BlockState state) {
        return lookup.computeIfAbsent(state, k -> {
            palette.add(k);
            return palette.size() - 1;
        });
    }

    /** Return the cell count of the box. */
    public int volume() {
        return size.getX() * size.getY() * size.getZ();
    }

    /** Return the inclusive max corner of the box. */
    public BlockPos max() {
        return min.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
    }

    /** Return the cell index of a world position (x, then z, then y), or -1 outside the box. */
    public int index(BlockPos p) {
        int x = p.getX() - min.getX();
        int y = p.getY() - min.getY();
        int z = p.getZ() - min.getZ();
        if (x < 0 || y < 0 || z < 0 || x >= size.getX() || y >= size.getY() || z >= size.getZ()) {
            return -1;
        }
        return x + size.getX() * (z + size.getZ() * y);
    }

    /** Return the world position of cell i. */
    public BlockPos pos(int i) {
        int sx = size.getX();
        int sz = size.getZ();
        return min.offset(i % sx, i / (sx * sz), i / sx % sz);
    }

    /** Return the stored state of cell i, or null for a non-center part of a big block. */
    public @Nullable BlockState state(int i) {
        return palette.get(cells[i]);
    }

    /** Return the stored Autarkia block entity configuration of cell i, or null. */
    public @Nullable CompoundTag data(int i) {
        return data.get(i);
    }

    /** Return the number of distinct states, the part marker included. */
    public int paletteSize() {
        return palette.size();
    }

    /** Return whether the snapshot was deleted with its projector. */
    public boolean deleted() {
        return deleted;
    }

    /** Store the current world content of one position (accept, 05 section 8.5) and mark the file for rewriting. */
    public void accept(ServerLevel level, BlockPos p) {
        int i = index(p);
        if (i < 0 || deleted) {
            return;
        }
        Cell c = read(level, p);
        cells[i] = c.state == null ? 0 : paletteIndex(c.state);
        if (c.data == null) {
            data.remove(i);
        } else {
            data.put(i, c.data);
        }
        setDirty();
    }

    /** One read cell: the state (null for a big-block part) and the Autarkia configuration data. */
    record Cell(@Nullable BlockState state, @Nullable CompoundTag data) {
    }

    static Cell read(ServerLevel level, BlockPos p) {
        BlockState st = level.getBlockState(p);
        BlockEntity be = level.getBlockEntity(p);
        if (be instanceof IMultiBlockEntity mb && !mb.isCenter()) {
            return new Cell(null, null);
        }
        CompoundTag tag = null;
        if (be instanceof PatternDataHolder h) {
            Identifier type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
            if (type != null && type.getNamespace().equals(Autarkia.MOD_ID)) {
                TagValueOutput out = TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
                h.writePatternData(out);
                tag = out.buildResult();
            }
        }
        return new Cell(st, tag);
    }

    /** Return the save file id of a snapshot: {@code snapshot_<32 lower-case hex>}. */
    public static String fileId(UUID id) {
        return "snapshot_" + id.toString().replace("-", "");
    }

    private static SavedDataType<Snapshot> type(UUID id) {
        return TYPES.computeIfAbsent(id, k -> new SavedDataType<>(Autarkia.id(fileId(k)), () -> {
            throw new IllegalStateException("Snapshot " + k + " is created by a Restoration projector only");
        }, CODEC));
    }

    /** Load a snapshot, or null when it does not exist or was deleted. */
    public static @Nullable Snapshot load(MinecraftServer server, UUID id) {
        Snapshot s = server.getDataStorage().get(type(id));
        return s == null || s.deleted ? null : s;
    }

    /** Put a finished snapshot into the server storage; the file is written at the next save. */
    public static void store(MinecraftServer server, Snapshot s) {
        server.getDataStorage().set(type(s.id), s);
        s.setDirty();
    }

    /**
     * Delete a snapshot (its projector was removed): the cached copy is emptied and never written again, and the file
     * {@code data/autarkia/snapshot_<32hex>.dat} is removed.
     */
    public static void delete(MinecraftServer server, UUID id) {
        Snapshot s = server.getDataStorage().get(type(id));
        if (s != null) {
            s.deleted = true;
            s.data.clear();
            s.setDirty(false);
        }
        Path file = server.getWorldPath(LevelResource.DATA).resolve(Autarkia.MOD_ID).resolve(fileId(id) + ".dat");
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete the snapshot file " + file, e);
        }
        Autarkia.LOG.info("[restore] snapshot {} deleted", id);
    }

    /** An incremental snapshot read of a box, {@link #CELLS_PER_TICK} cells per tick; it waits while a cell's chunk is not loaded. */
    public static final class Builder {
        private final Snapshot target;
        private int cursor;

        public Builder(UUID id, BlockPos min, Vec3i size, long now) {
            this.target = new Snapshot(id, min, size, now, new int[size.getX() * size.getY() * size.getZ()]);
        }

        /** Return the cells read so far. */
        public int cursor() {
            return cursor;
        }

        /** Return the cell count of the box. */
        public int volume() {
            return target.volume();
        }

        /** Return the box of the snapshot being read. */
        public BlockPos min() {
            return target.min;
        }

        /** Read up to budget cells; return true when the whole box is read. */
        public boolean step(ServerLevel level, int budget) {
            int end = Math.min(target.volume(), cursor + budget);
            while (cursor < end) {
                BlockPos p = target.pos(cursor);
                if (!level.isLoaded(p)) {
                    return false;
                }
                Cell c = read(level, p);
                target.cells[cursor] = c.state == null ? 0 : target.paletteIndex(c.state);
                if (c.data != null) {
                    target.data.put(cursor, c.data);
                }
                cursor++;
            }
            return cursor >= target.volume();
        }

        /** Return the finished snapshot. */
        public Snapshot build() {
            if (cursor < target.volume()) {
                throw new IllegalStateException("Snapshot " + target.id + " is not finished: " + cursor + " of " + target.volume() + " cells read");
            }
            return target;
        }
    }
}

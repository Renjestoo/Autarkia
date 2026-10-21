package dev.autarkia.colony;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.Autarkia;
import dev.autarkia.energy.CableTier;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * A recorded Pattern (05 section 5.2): palette (index 0 = ignore), packed cells in x, z, y order with
 * {@code ceil(log2(paletteSize))} bits each, foreign block entity and big-block part cell lists, the configuration
 * data of Autarkia block entities, internal spans, ports and meta (BOM, CRC32 hash, role). One Pattern is one
 * immutable server save file {@code autarkia:pattern_<32hex>}.
 */
public record Pattern(UUID id, String name, UUID author, long created, Vec3i size, List<BlockState> palette, long[] cells, int bits, List<Integer> foreign,
                      List<Integer> parts, Map<String, CompoundTag> data, List<SpanRecord> spans, List<Port> ports, Map<Item, Integer> bom, int hash,
                      String role) {
    /** An internal span with both ends inside the box. */
    public record SpanRecord(BlockPos a, BlockPos b, CableTier tier) {
        public static final Codec<SpanRecord> CODEC = RecordCodecBuilder.create(i -> i.group(BlockPos.CODEC.fieldOf("a").forGetter(SpanRecord::a),
            BlockPos.CODEC.fieldOf("b").forGetter(SpanRecord::b), Codec.STRING.xmap(s -> CableTier.valueOf(s.toUpperCase(java.util.Locale.ROOT)),
                t -> t.name().toLowerCase(java.util.Locale.ROOT)).fieldOf("tier").forGetter(SpanRecord::tier)).apply(i, SpanRecord::new));
    }

    /** Port kinds of 05 section 5.2. */
    public enum PortKind implements StringRepresentable {
        POWER, ITEM_IN, ITEM_OUT;

        public static final Codec<PortKind> CODEC = StringRepresentable.fromEnum(PortKind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** A port: a span leaving the box (POWER) or a belt on a box face pointing in or out. */
    public record Port(BlockPos rel, PortKind kind, Direction dir) {
        public static final Codec<Port> CODEC = RecordCodecBuilder.create(i -> i.group(BlockPos.CODEC.fieldOf("rel").forGetter(Port::rel),
            PortKind.CODEC.fieldOf("kind").forGetter(Port::kind), Direction.CODEC.fieldOf("dir").forGetter(Port::dir)).apply(i, Port::new));
    }

    private static final Codec<long[]> LONGS = Codec.LONG_STREAM.xmap(LongStream::toArray, LongStream::of);
    private static final Codec<Vec3i> SIZE = Vec3i.CODEC;

    public static final Codec<Pattern> CODEC = RecordCodecBuilder.create(i -> i.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(Pattern::id),
        Codec.STRING.fieldOf("name").forGetter(Pattern::name),
        UUIDUtil.CODEC.fieldOf("author").forGetter(Pattern::author),
        Codec.LONG.fieldOf("created").forGetter(Pattern::created),
        SIZE.fieldOf("size").forGetter(Pattern::size),
        BlockState.CODEC.listOf().fieldOf("palette").forGetter(Pattern::palette),
        LONGS.fieldOf("cells").forGetter(Pattern::cells),
        Codec.INT.fieldOf("bits").forGetter(Pattern::bits),
        Codec.INT.listOf().fieldOf("foreign").forGetter(Pattern::foreign),
        Codec.INT.listOf().fieldOf("parts").forGetter(Pattern::parts),
        Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC).fieldOf("data").forGetter(Pattern::data),
        SpanRecord.CODEC.listOf().fieldOf("spans").forGetter(Pattern::spans),
        Port.CODEC.listOf().fieldOf("ports").forGetter(Pattern::ports),
        Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.INT).fieldOf("bom").forGetter(Pattern::bom),
        Codec.INT.fieldOf("hash").forGetter(Pattern::hash),
        Codec.STRING.fieldOf("role").forGetter(Pattern::role)
    ).apply(i, Pattern::new));

    /** Return the number of cells (sx * sy * sz). */
    public int volume() {
        return size.getX() * size.getY() * size.getZ();
    }

    /** Return the cell index of a relative position (x, then z, then y). */
    public int index(int x, int y, int z) {
        return x + size.getX() * (z + size.getZ() * y);
    }

    /** Return the palette index of cell i. */
    public int cell(int i) {
        if (bits == 0) {
            return 0;
        }
        int perLong = 64 / bits;
        long word = cells[i / perLong];
        return (int) (word >>> (i % perLong * bits) & ((1L << bits) - 1));
    }

    /** Return the state of cell i, or null for an ignored cell. */
    public @Nullable BlockState state(int i) {
        int k = cell(i);
        return k == 0 ? null : palette.get(k);
    }

    /** Return the non-ignored block count. */
    public int blocks() {
        int n = 0;
        for (int i = 0; i < volume(); i++) {
            n += cell(i) == 0 ? 0 : 1;
        }
        return n;
    }

    /** Pack palette indices with the minimal bit width for the palette (no index spans two longs). */
    public static long[] pack(int[] indices, int bits) {
        if (bits == 0) {
            return new long[0];
        }
        int perLong = 64 / bits;
        long[] out = new long[(indices.length + perLong - 1) / perLong];
        for (int i = 0; i < indices.length; i++) {
            out[i / perLong] |= (long) indices[i] << (i % perLong * bits);
        }
        return out;
    }

    /** Return {@code ceil(log2(paletteSize))}, at least 1 when the palette has a real state. */
    public static int bitsFor(int paletteSize) {
        return paletteSize <= 1 ? 0 : 32 - Integer.numberOfLeadingZeros(paletteSize - 1);
    }

    /** Return the save file id of a Pattern: {@code pattern_<32 lower-case hex>}. */
    public static String fileId(UUID id) {
        return "pattern_" + id.toString().replace("-", "");
    }

    /** The save file holder of one Pattern; the file is written once on capture. */
    public static final class Saved extends SavedData {
        public static final Codec<Saved> CODEC = Pattern.CODEC.xmap(Saved::new, s -> s.pattern);
        public final Pattern pattern;

        Saved(Pattern pattern) {
            this.pattern = pattern;
        }
    }

    private static SavedDataType<Saved> type(UUID id) {
        return new SavedDataType<>(Autarkia.id(fileId(id)), () -> {
            throw new IllegalStateException("Pattern " + id + " is created by capture only");
        }, Saved.CODEC);
    }

    /** Write a Pattern to its save file (05 section 1.5: {@code set} marks it dirty, the file is written on save). */
    public static void store(MinecraftServer server, Pattern p) {
        server.getDataStorage().set(type(p.id()), new Saved(p));
    }

    /** Load a Pattern from its save file, or null when the file does not exist. */
    public static @Nullable Pattern load(MinecraftServer server, UUID id) {
        Saved s = server.getDataStorage().get(type(id));
        return s == null ? null : s.pattern;
    }

    /** The {@code autarkia:pattern_ref} card component (05 section 5.4). */
    public record Ref(UUID id, String name, int sx, int sy, int sz, int blocks, int hash, UUID author) {
        public static final Codec<Ref> CODEC = RecordCodecBuilder.create(i -> i.group(UUIDUtil.CODEC.fieldOf("id").forGetter(Ref::id),
            Codec.STRING.fieldOf("name").forGetter(Ref::name), Codec.INT.fieldOf("sx").forGetter(Ref::sx), Codec.INT.fieldOf("sy").forGetter(Ref::sy),
            Codec.INT.fieldOf("sz").forGetter(Ref::sz), Codec.INT.fieldOf("blocks").forGetter(Ref::blocks), Codec.INT.fieldOf("hash").forGetter(Ref::hash),
            UUIDUtil.CODEC.fieldOf("author").forGetter(Ref::author)).apply(i, Ref::new));
        public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, Ref> STREAM_CODEC =
            net.minecraft.network.codec.ByteBufCodecs.fromCodec(CODEC);

        /** Return the card reference of a Pattern. */
        public static Ref of(Pattern p, int blocks) {
            return new Ref(p.id(), p.name(), p.size().getX(), p.size().getY(), p.size().getZ(), blocks, p.hash(), p.author());
        }
    }
}

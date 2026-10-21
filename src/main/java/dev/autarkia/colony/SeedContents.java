package dev.autarkia.colony;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * The {@code autarkia:seed_contents} component of an {@code autarky_seed} (05 section 8.4, 08b section 5): the packed
 * Pattern entries (Pattern id, the min corner dx and dz of its rotated box in the layout, quarter turns clockwise), the
 * worker entity types that unfold at the finish, the FE charge for the outpost core, the footprint size (x, height, z)
 * and the number of placed blocks. Entry offsets are normalized so the footprint min corner is (0, 0). The
 * {@code version} field is 1 (13 G10); an unknown later version does not decode.
 */
public record SeedContents(int version, List<Entry> entries, List<Identifier> workers, long charge, Vec3i size, int blocks) {
    public static final int VERSION = 1;

    /** One packed Pattern of the layout. */
    public record Entry(UUID pattern, int dx, int dz, int rot) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("pattern").forGetter(Entry::pattern), Codec.INT.fieldOf("dx").forGetter(Entry::dx),
            Codec.INT.fieldOf("dz").forGetter(Entry::dz), Codec.intRange(0, 3).fieldOf("rot").forGetter(Entry::rot)).apply(i, Entry::new));
    }

    public static final Codec<SeedContents> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.intRange(1, VERSION).fieldOf("version").forGetter(SeedContents::version),
        Entry.CODEC.listOf(1, 8).fieldOf("entries").forGetter(SeedContents::entries),
        Identifier.CODEC.listOf().fieldOf("workers").forGetter(SeedContents::workers),
        Codec.LONG.fieldOf("charge").forGetter(SeedContents::charge),
        Vec3i.CODEC.fieldOf("size").forGetter(SeedContents::size),
        Codec.INT.fieldOf("blocks").forGetter(SeedContents::blocks)).apply(i, SeedContents::new));
    public static final StreamCodec<ByteBuf, SeedContents> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /** Return the Pattern ids of the entries in layout order. */
    public List<UUID> patterns() {
        return entries.stream().map(Entry::pattern).toList();
    }
}

package dev.autarkia.colony;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.Autarkia;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtOps;

/**
 * A Restoration Field of a colony (05 section 1.5 {@code fields}): the projector center, the snapshot id and the
 * snapshot box (inclusive corners). The list codec decodes every entry on its own, so one bad entry is logged and
 * skipped instead of dropping the colony.
 */
public record RestorationField(BlockPos projectorPos, UUID snapshot, BlockPos min, BlockPos max) {
    public static final int VERSION = 1;

    public static final Codec<RestorationField> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.optionalFieldOf("version", VERSION).forGetter(f -> VERSION),
        BlockPos.CODEC.fieldOf("projector_pos").forGetter(RestorationField::projectorPos),
        UUIDUtil.CODEC.fieldOf("snapshot").forGetter(RestorationField::snapshot),
        BlockPos.CODEC.fieldOf("min").forGetter(RestorationField::min),
        BlockPos.CODEC.fieldOf("max").forGetter(RestorationField::max)
    ).apply(i, (version, pos, snapshot, min, max) -> new RestorationField(pos, snapshot, min, max)));

    public static final Codec<List<RestorationField>> LIST = Codec.PASSTHROUGH.listOf().xmap(
        list -> list.stream().map(d -> CODEC.parse(d).resultOrPartial(e -> Autarkia.LOG.error("Skipping a restoration field that does not parse: {}", e)))
            .flatMap(Optional::stream).toList(),
        list -> list.stream().<Dynamic<?>>map(f -> new Dynamic<>(NbtOps.INSTANCE, CODEC.encodeStart(NbtOps.INSTANCE, f).getOrThrow())).toList());

    /** Return whether the position lies in the field box. */
    public boolean contains(BlockPos p) {
        return p.getX() >= min.getX() && p.getX() <= max.getX() && p.getY() >= min.getY() && p.getY() <= max.getY() && p.getZ() >= min.getZ()
            && p.getZ() <= max.getZ();
    }
}

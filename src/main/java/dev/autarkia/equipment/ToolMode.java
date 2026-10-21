package dev.autarkia.equipment;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** Tool modes of 07 section 2.4 (the autarkia:tool_mode component). */
public enum ToolMode implements StringRepresentable {
    SINGLE("single"), AREA3("area3"), AREA5("area5"), VEIN("vein"), TERRAIN_SAFE("terrain_safe"), TERRAIN_DESTROY("terrain_destroy");

    public static final Codec<ToolMode> CODEC = StringRepresentable.fromEnum(ToolMode::values);
    public static final StreamCodec<ByteBuf, ToolMode> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);
    private final String id;

    ToolMode(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Return the language key of the mode label. */
    public String key() {
        return "mode.autarkia." + id;
    }

    /** Return the next mode in declaration order. */
    public ToolMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}

package dev.autarkia.equipment;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** Enchant modes of the {@code aegis_multitool} (07 section 2.4, the autarkia:enchant_mode component). */
public enum EnchantMode implements StringRepresentable {
    NONE("none"), SILK("silk"), FORTUNE("fortune");

    public static final Codec<EnchantMode> CODEC = StringRepresentable.fromEnum(EnchantMode::values);
    public static final StreamCodec<ByteBuf, EnchantMode> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);
    private final String id;

    EnchantMode(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Return the language key of the mode label. */
    public String key() {
        return "enchant.autarkia." + id;
    }

    /** Return the mode the enchant key selects after this one: none, Silk Touch, Fortune III, then none again. */
    public EnchantMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}

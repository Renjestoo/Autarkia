package dev.autarkia.network;

import dev.autarkia.Autarkia;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C matter value table (03 section 6): the merged {@code matter_values.json} text, sent with every data pack sync. */
public record MatterValuesPayload(String json) implements CustomPacketPayload {
    public static final Type<MatterValuesPayload> TYPE = new Type<>(Autarkia.id("matter_values"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MatterValuesPayload> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(1 << 20),
        MatterValuesPayload::json, MatterValuesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

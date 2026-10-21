package dev.autarkia.network;

import dev.autarkia.Autarkia;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C lightning pulse visual (02 section 5): per span the game tick the pulse enters it, the direction (from the
 * span's first node or its second) and the speed in blocks per tick; the client moves a white glow along each curve.
 */
public record LightningPulsePayload(List<Entry> spans, float speed) implements CustomPacketPayload {
    public static final Type<LightningPulsePayload> TYPE = new Type<>(Autarkia.id("lightning_pulse"));

    /** One span of the pulse. */
    public record Entry(int spanId, long startTick, boolean fromA) {
        static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Entry::spanId,
            ByteBufCodecs.VAR_LONG, Entry::startTick, ByteBufCodecs.BOOL, Entry::fromA, Entry::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, LightningPulsePayload> CODEC = StreamCodec.composite(
        Entry.CODEC.apply(ByteBufCodecs.list(512)), LightningPulsePayload::spans, ByteBufCodecs.FLOAT, LightningPulsePayload::speed,
        LightningPulsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

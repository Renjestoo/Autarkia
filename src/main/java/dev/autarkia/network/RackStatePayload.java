package dev.autarkia.network;

import dev.autarkia.Autarkia;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C rack state (04 section 2.3.3): bend stage, load ratio in per mille and the seconds left in stage 3; sent on a
 * stage change and every second in stage 3. The client shakes the crates of a critical rack and shows the countdown.
 */
public record RackStatePayload(BlockPos pos, int stage, int ratioPermille, int secondsLeft) implements CustomPacketPayload {
    public static final Type<RackStatePayload> TYPE = new Type<>(Autarkia.id("rack_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RackStatePayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
        RackStatePayload::pos, ByteBufCodecs.VAR_INT, RackStatePayload::stage, ByteBufCodecs.VAR_INT, RackStatePayload::ratioPermille,
        ByteBufCodecs.VAR_INT, RackStatePayload::secondsLeft, RackStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

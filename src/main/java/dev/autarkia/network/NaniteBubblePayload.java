package dev.autarkia.network;

import dev.autarkia.Autarkia;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C nanite bubble (05 section 8.3, 08b section 7): the kept containers of one claim whose claim neighbours are gone.
 * The server sends it every 20 ticks until 600 ticks after the claim ends; each arrival draws one ring per position.
 */
public record NaniteBubblePayload(List<BlockPos> positions) implements CustomPacketPayload {
    public static final Type<NaniteBubblePayload> TYPE = new Type<>(Autarkia.id("nanite_bubble"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NaniteBubblePayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(1024)), NaniteBubblePayload::positions, NaniteBubblePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package dev.autarkia.network;

import dev.autarkia.Autarkia;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C {@code seed_unpack} (05 section 8.4, 08b section 7, C17): sent once when a seed lands, to the players within 160
 * blocks. Footprint centre at floor + 1, footprint min and max corners, and the landing game time; the client draws the
 * glow ring growing to the footprint edge in the first 40 ticks and the leveling glow sweep from tick 40 to 140.
 */
public record SeedUnpackPayload(BlockPos center, BlockPos min, BlockPos max, long start) implements CustomPacketPayload {
    public static final Type<SeedUnpackPayload> TYPE = new Type<>(Autarkia.id("seed_unpack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SeedUnpackPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SeedUnpackPayload::center, BlockPos.STREAM_CODEC, SeedUnpackPayload::min, BlockPos.STREAM_CODEC, SeedUnpackPayload::max,
        ByteBufCodecs.VAR_LONG, SeedUnpackPayload::start, SeedUnpackPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

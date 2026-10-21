package dev.autarkia.network;

import dev.autarkia.Autarkia;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * S2C {@code autarkia:reform_fx} (07 section 3.2, decision G16): a Nanite Reform of the player with that entity id from
 * the old to the new position, started at that game time. The client draws the 20-tick nanite cloud and stream.
 */
public record ReformFxPayload(int playerId, Vec3 from, Vec3 to, long start) implements CustomPacketPayload {
    public static final Type<ReformFxPayload> TYPE = new Type<>(Autarkia.id("reform_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReformFxPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ReformFxPayload::playerId, Vec3.STREAM_CODEC, ReformFxPayload::from, Vec3.STREAM_CODEC, ReformFxPayload::to,
        ByteBufCodecs.VAR_LONG, ReformFxPayload::start, ReformFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

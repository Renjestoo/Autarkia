package dev.autarkia.network;

import dev.autarkia.Autarkia;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C {@code autarkia:ascension_pose} (07 section 3.5): play (or with play false, stop) a Player Animation Library
 * animation on the {@code autarkia:ascension} layer of the player with that entity id. Sent to the player and to every
 * player who tracks the player (decision C13).
 */
public record AscensionPosePayload(int playerId, Identifier animation, boolean play) implements CustomPacketPayload {
    public static final Type<AscensionPosePayload> TYPE = new Type<>(Autarkia.id("ascension_pose"));
    public static final Identifier FIT = Autarkia.id("ascension_fit");
    public static final Identifier RISE = Autarkia.id("ascension_rise");
    public static final StreamCodec<RegistryFriendlyByteBuf, AscensionPosePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, AscensionPosePayload::playerId, Identifier.STREAM_CODEC, AscensionPosePayload::animation, ByteBufCodecs.BOOL,
        AscensionPosePayload::play, AscensionPosePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package dev.autarkia.network;

import dev.autarkia.Autarkia;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/** S2C beam visual (07 section 2.2): a textured beam from one point to another for a number of ticks. */
public record BeamVisualPayload(int ownerId, Vec3 from, Vec3 to, int ticks, int color, byte kind, float width) implements CustomPacketPayload {
    public static final Type<BeamVisualPayload> TYPE = new Type<>(Autarkia.id("beam_visual"));
    public static final byte KIND_LANCE = 0;
    public static final byte KIND_DISASSEMBLER = 1;
    public static final byte KIND_PHASE = 2;
    public static final byte KIND_RAIL = 3;
    public static final StreamCodec<RegistryFriendlyByteBuf, BeamVisualPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, BeamVisualPayload::ownerId,
        Vec3.STREAM_CODEC, BeamVisualPayload::from,
        Vec3.STREAM_CODEC, BeamVisualPayload::to,
        ByteBufCodecs.VAR_INT, BeamVisualPayload::ticks,
        ByteBufCodecs.INT, BeamVisualPayload::color,
        ByteBufCodecs.BYTE, BeamVisualPayload::kind,
        ByteBufCodecs.FLOAT, BeamVisualPayload::width,
        BeamVisualPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

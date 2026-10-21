package dev.autarkia.network;

import dev.autarkia.Autarkia;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C {@code autarkia:aegis_status} (07 section 2.6, decision G16), sent every 10 ticks to a player in a full Aegis set.
 *
 * @param state {@link #STATE_NONE} (no full set; the last payload after the set came off), {@link #STATE_RED} (no tether),
 *     {@link #STATE_AMBER} (tethered, grid below 1,000,000 FE) or {@link #STATE_GREEN} (tethered, grid at least 1,000,000 FE)
 * @param gridFe stored FE of the tether colony's domain grid
 * @param reformReadyAt game time from which the Nanite Reform is ready again
 * @param matter free matter units in the domain's matter buffers
 */
public record AegisStatusPayload(byte state, long gridFe, long reformReadyAt, int matter) implements CustomPacketPayload {
    public static final Type<AegisStatusPayload> TYPE = new Type<>(Autarkia.id("aegis_status"));
    public static final StreamCodec<ByteBuf, AegisStatusPayload> CODEC = StreamCodec.composite(ByteBufCodecs.BYTE, AegisStatusPayload::state,
        ByteBufCodecs.VAR_LONG, AegisStatusPayload::gridFe, ByteBufCodecs.VAR_LONG, AegisStatusPayload::reformReadyAt, ByteBufCodecs.VAR_INT,
        AegisStatusPayload::matter, AegisStatusPayload::new);
    public static final byte STATE_NONE = 0;
    public static final byte STATE_RED = 1;
    public static final byte STATE_AMBER = 2;
    public static final byte STATE_GREEN = 3;
    public static final AegisStatusPayload NONE = new AegisStatusPayload(STATE_NONE, 0, 0, 0);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package dev.autarkia.network;

import dev.autarkia.Autarkia;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C span physics state: heat (or rope wear), load ratio, superconductor temperature and rope slip, one byte each (02 section 2.6). */
public record SpanStatePayload(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<SpanStatePayload> TYPE = new Type<>(Autarkia.id("span_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpanStatePayload> CODEC =
        Entry.CODEC.apply(ByteBufCodecs.list()).map(SpanStatePayload::new, SpanStatePayload::entries).cast();

    /** Byte-quantised span state; heat and temp map 0..255 to 0..1, load maps 0..255 to 0..2.55. */
    public record Entry(int id, byte heat, byte load, byte temp, boolean slip, boolean active) {
        public static final StreamCodec<ByteBuf, Entry> CODEC = new StreamCodec<>() {
            @Override
            public Entry decode(ByteBuf buf) {
                int id = buf.readInt();
                byte h = buf.readByte();
                byte l = buf.readByte();
                byte t = buf.readByte();
                int flags = buf.readByte();
                return new Entry(id, h, l, t, (flags & 1) != 0, (flags & 2) != 0);
            }

            @Override
            public void encode(ByteBuf buf, Entry e) {
                buf.writeInt(e.id).writeByte(e.heat).writeByte(e.load).writeByte(e.temp);
                buf.writeByte((e.slip ? 1 : 0) | (e.active ? 2 : 0));
            }
        };

        /** Return the heat as 0..1. */
        public float heatF() {
            return (heat & 0xFF) / 255f;
        }

        /** Return the load ratio u, 0..2.55. */
        public float loadF() {
            return (load & 0xFF) / 100f;
        }

        /** Return the superconductor temperature as 0..1. */
        public float tempF() {
            return (temp & 0xFF) / 255f;
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

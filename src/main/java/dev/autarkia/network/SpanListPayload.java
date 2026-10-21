package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.CableTier;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/** S2C span geometry: adds or removes spans in the client span store (02 section 2.6). */
public record SpanListPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<SpanListPayload> TYPE = new Type<>(Autarkia.id("span_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpanListPayload> CODEC =
        Entry.CODEC.apply(ByteBufCodecs.list()).map(SpanListPayload::new, SpanListPayload::entries).cast();

    /** One span; remove = true deletes span id from the client store. */
    public record Entry(int id, boolean remove, Vec3 a, Vec3 b, CableTier tier, float heat, float load, float temp, boolean active) {
        public static final StreamCodec<ByteBuf, Entry> CODEC = new StreamCodec<>() {
            @Override
            public Entry decode(ByteBuf buf) {
                int id = buf.readInt();
                boolean remove = buf.readBoolean();
                if (remove) {
                    return new Entry(id, true, Vec3.ZERO, Vec3.ZERO, CableTier.ROPE, 0, 0, 0, false);
                }
                Vec3 a = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                Vec3 b = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                CableTier tier = CableTier.STREAM_CODEC.decode(buf);
                return new Entry(id, false, a, b, tier, buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean());
            }

            @Override
            public void encode(ByteBuf buf, Entry e) {
                buf.writeInt(e.id);
                buf.writeBoolean(e.remove);
                if (e.remove) {
                    return;
                }
                buf.writeDouble(e.a.x).writeDouble(e.a.y).writeDouble(e.a.z);
                buf.writeDouble(e.b.x).writeDouble(e.b.y).writeDouble(e.b.z);
                CableTier.STREAM_CODEC.encode(buf, e.tier);
                buf.writeFloat(e.heat).writeFloat(e.load).writeFloat(e.temp);
                buf.writeBoolean(e.active);
            }
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

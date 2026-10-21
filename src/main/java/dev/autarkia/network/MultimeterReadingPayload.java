package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.CableTier;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C multimeter screen data (02 section 2.6 and 9.1): network stats and the span lines of the clicked node. */
public record MultimeterReadingPayload(BlockPos node, boolean connected, int networkId, CableTier tier, int nodes, int spans,
                                       float generation, float demand, float phi, float loss, long stored, long capacity,
                                       float rho, boolean lagging, List<Line> lines) implements CustomPacketPayload {
    public static final Type<MultimeterReadingPayload> TYPE = new Type<>(Autarkia.id("multimeter_reading"));

    /** One span of the node: tree edges carry flow, others are parallel and idle. */
    public record Line(int id, float length, float flow, float load, float wear, float heat, float temp, boolean tree, boolean slip) {
        public static final StreamCodec<ByteBuf, Line> CODEC = new StreamCodec<>() {
            @Override
            public Line decode(ByteBuf b) {
                return new Line(b.readInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(),
                    b.readBoolean(), b.readBoolean());
            }

            @Override
            public void encode(ByteBuf b, Line l) {
                b.writeInt(l.id).writeFloat(l.length).writeFloat(l.flow).writeFloat(l.load).writeFloat(l.wear).writeFloat(l.heat)
                    .writeFloat(l.temp).writeBoolean(l.tree).writeBoolean(l.slip);
            }
        };
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, MultimeterReadingPayload> CODEC = new StreamCodec<>() {
        @Override
        public MultimeterReadingPayload decode(RegistryFriendlyByteBuf b) {
            BlockPos node = BlockPos.STREAM_CODEC.decode(b);
            boolean connected = b.readBoolean();
            int id = b.readInt();
            CableTier tier = CableTier.STREAM_CODEC.decode(b);
            int nodes = b.readInt();
            int spans = b.readInt();
            float gen = b.readFloat();
            float dem = b.readFloat();
            float phi = b.readFloat();
            float loss = b.readFloat();
            long stored = b.readLong();
            long cap = b.readLong();
            float rho = b.readFloat();
            boolean lag = b.readBoolean();
            List<Line> lines = Line.CODEC.apply(ByteBufCodecs.list()).decode(b);
            return new MultimeterReadingPayload(node, connected, id, tier, nodes, spans, gen, dem, phi, loss, stored, cap, rho, lag, lines);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf b, MultimeterReadingPayload p) {
            BlockPos.STREAM_CODEC.encode(b, p.node);
            b.writeBoolean(p.connected);
            b.writeInt(p.networkId);
            CableTier.STREAM_CODEC.encode(b, p.tier);
            b.writeInt(p.nodes).writeInt(p.spans);
            b.writeFloat(p.generation).writeFloat(p.demand).writeFloat(p.phi).writeFloat(p.loss);
            b.writeLong(p.stored).writeLong(p.capacity);
            b.writeFloat(p.rho).writeBoolean(p.lagging);
            Line.CODEC.apply(ByteBufCodecs.list()).encode(b, p.lines);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package dev.autarkia.network;

import dev.autarkia.Autarkia;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** S2C transit changes of one segment (04 section 0.3); the segment id is the owner block position. */
public record TransitDeltaPayload(BlockPos owner, long gameTime, List<Op> ops) implements CustomPacketPayload {
    public static final Type<TransitDeltaPayload> TYPE = new Type<>(Autarkia.id("transit_delta"));
    public static final byte ADD = 0;
    public static final byte REMOVE = 1;
    public static final byte BLOCK = 2;
    public static final byte UNBLOCK = 3;

    /** One change; stack and pos are only used by ADD. */
    public record Op(byte kind, int id, ItemStack stack, int pos) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Op> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, Op::kind, ByteBufCodecs.VAR_INT, Op::id, ItemStack.OPTIONAL_STREAM_CODEC, Op::stack, ByteBufCodecs.VAR_INT, Op::pos, Op::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TransitDeltaPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, TransitDeltaPayload::owner, ByteBufCodecs.VAR_LONG, TransitDeltaPayload::gameTime,
        Op.CODEC.apply(ByteBufCodecs.list()), TransitDeltaPayload::ops, TransitDeltaPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

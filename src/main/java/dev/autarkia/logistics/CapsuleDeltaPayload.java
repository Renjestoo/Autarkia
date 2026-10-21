package dev.autarkia.logistics;

import dev.autarkia.Autarkia;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * S2C capsule changes of one tube network (04 section 0.3); the network id is the anchor station position. Capsules
 * need their route, so this payload carries it next to the {@link dev.autarkia.network.TransitDeltaPayload} fields.
 * Ops: ADD (id, stack, start, path, pos), REMOVE (id), ROUTE (id, start, path, pos), STATE (pos = state ordinal),
 * CLEAR (all capsules leave this owner).
 */
public record CapsuleDeltaPayload(BlockPos anchor, long gameTime, List<Op> ops) implements CustomPacketPayload {
    public static final Type<CapsuleDeltaPayload> TYPE = new Type<>(Autarkia.id("capsule_delta"));
    public static final byte ADD = 0;
    public static final byte REMOVE = 1;
    public static final byte ROUTE = 2;
    public static final byte STATE = 3;
    public static final byte CLEAR = 4;

    /** One change; unused fields are empty or 0. */
    public record Op(byte kind, int id, ItemStack stack, long start, byte[] path, int pos) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Op> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, Op::kind, ByteBufCodecs.VAR_INT, Op::id, ItemStack.OPTIONAL_STREAM_CODEC, Op::stack, ByteBufCodecs.VAR_LONG, Op::start,
            ByteBufCodecs.BYTE_ARRAY, Op::path, ByteBufCodecs.VAR_INT, Op::pos, Op::new);

        /** Return an ADD op for a new capsule (with a copy of its stack, so a local client never shares the server stack). */
        public static Op add(Capsule c) {
            return new Op(ADD, c.id, c.stack.copy(), c.start, c.path, c.pos);
        }

        /** Return a REMOVE op. */
        public static Op remove(int id) {
            return new Op(REMOVE, id, ItemStack.EMPTY, 0, new byte[0], 0);
        }

        /** Return a ROUTE op with the capsule's current route. */
        public static Op route(Capsule c) {
            return new Op(ROUTE, c.id, ItemStack.EMPTY, c.start, c.path, c.pos);
        }

        /** Return a STATE op. */
        public static Op state(int ordinal) {
            return new Op(STATE, 0, ItemStack.EMPTY, 0, new byte[0], ordinal);
        }

        /** Return a CLEAR op. */
        public static Op clear() {
            return new Op(CLEAR, 0, ItemStack.EMPTY, 0, new byte[0], 0);
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CapsuleDeltaPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, CapsuleDeltaPayload::anchor, ByteBufCodecs.VAR_LONG, CapsuleDeltaPayload::gameTime,
        Op.CODEC.apply(ByteBufCodecs.list()), CapsuleDeltaPayload::ops, CapsuleDeltaPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

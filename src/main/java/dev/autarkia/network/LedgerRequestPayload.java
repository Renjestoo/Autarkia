package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.storage.LedgerTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S ledger request (04 section 2.5): the player asks the open terminal for an amount of an item. */
public record LedgerRequestPayload(BlockPos pos, ItemStack item, int amount) implements CustomPacketPayload {
    public static final Type<LedgerRequestPayload> TYPE = new Type<>(Autarkia.id("ledger_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LedgerRequestPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
        LedgerRequestPayload::pos, ItemStack.STREAM_CODEC, LedgerRequestPayload::item, ByteBufCodecs.VAR_INT, LedgerRequestPayload::amount,
        LedgerRequestPayload::new);

    /** Handle the request on the server thread; only a player with this terminal's menu open may request. */
    public static void handle(LedgerRequestPayload p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof dev.autarkia.registry.SlotMenu m && m.pos.equals(p.pos)
            && sp.level().getBlockEntity(p.pos) instanceof LedgerTerminalBlockEntity t) {
            t.request(sp, p.item, p.amount);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.storage.LedgerTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S stock view request (04 section 2.5): the open ledger screen asks for the current stock (on open and every 2 seconds). */
public record StockViewRequestPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<StockViewRequestPayload> TYPE = new Type<>(Autarkia.id("stock_view_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StockViewRequestPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
        StockViewRequestPayload::pos, StockViewRequestPayload::new);

    /** Answer with a stock view; only a player with this terminal's menu open gets one. */
    public static void handle(StockViewRequestPayload p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof dev.autarkia.registry.SlotMenu m && m.pos.equals(p.pos)
            && sp.level().getBlockEntity(p.pos) instanceof LedgerTerminalBlockEntity t) {
            AutarkiaNetwork.toPlayer(sp, t.stockView());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

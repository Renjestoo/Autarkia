package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.storage.DepotBlockEntity;
import dev.autarkia.storage.DomainStockData;
import dev.autarkia.storage.LedgerTerminalBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S stock view request (04 sections 2.5 and 2.6): an open ledger screen or depot screen asks for the current stock and
 * one page of the linked stock view (on open, every 2 seconds and on a page change).
 */
public record StockViewRequestPayload(BlockPos pos, int page) implements CustomPacketPayload {
    public static final Type<StockViewRequestPayload> TYPE = new Type<>(Autarkia.id("stock_view_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StockViewRequestPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
        StockViewRequestPayload::pos, ByteBufCodecs.VAR_INT, StockViewRequestPayload::page, StockViewRequestPayload::new);

    /** Request the first page of the linked view with the stock. */
    public StockViewRequestPayload(BlockPos pos) {
        this(pos, 0);
    }

    /** Answer with a stock view; only a player with this terminal's or depot's menu open gets one. */
    public static void handle(StockViewRequestPayload p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp) || !(sp.containerMenu instanceof dev.autarkia.registry.SlotMenu m) || !m.pos.equals(p.pos)) {
            return;
        }
        BlockEntity be = sp.level().getBlockEntity(p.pos);
        if (be instanceof LedgerTerminalBlockEntity t) {
            AutarkiaNetwork.toPlayer(sp, t.stockView().withSites(DomainStockData.view(sp.level().getServer(), p.page)));
        } else if (be instanceof DepotBlockEntity) {
            AutarkiaNetwork.toPlayer(sp, new StockViewPayload(p.pos, List.of(), DomainStockData.view(sp.level().getServer(), p.page)));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

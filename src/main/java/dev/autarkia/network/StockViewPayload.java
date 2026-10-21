package dev.autarkia.network;

import dev.autarkia.Autarkia;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** S2C stock view (04 section 2.5): the warehouse stock of a terminal's controller, each item with its available and total amount. */
public record StockViewPayload(BlockPos pos, List<Stock> stock) implements CustomPacketPayload {
    public static final Type<StockViewPayload> TYPE = new Type<>(Autarkia.id("stock_view"));

    /** One stock line. */
    public record Stock(ItemStack item, long available, long total) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Stock> CODEC = StreamCodec.composite(ItemStack.STREAM_CODEC, Stock::item,
            ByteBufCodecs.VAR_LONG, Stock::available, ByteBufCodecs.VAR_LONG, Stock::total, Stock::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, StockViewPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
        StockViewPayload::pos, Stock.CODEC.apply(ByteBufCodecs.list(8192)), StockViewPayload::stock, StockViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

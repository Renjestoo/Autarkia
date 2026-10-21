package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.storage.LedgerRequest;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C ledger terminal status (04 section 2.5), sent once per second to each player with the terminal open: whether it
 * is linked, the warning key (empty, {@code no_warehouse}, {@code no_loader}, {@code warehouse_full}), the colony's
 * Loader count and the terminal's request queue.
 */
public record LedgerStatusPayload(BlockPos pos, boolean linked, String warning, int loaders, List<LedgerRequest.View> queue)
    implements CustomPacketPayload {
    public static final Type<LedgerStatusPayload> TYPE = new Type<>(Autarkia.id("ledger_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LedgerStatusPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
        LedgerStatusPayload::pos, ByteBufCodecs.BOOL, LedgerStatusPayload::linked, ByteBufCodecs.STRING_UTF8, LedgerStatusPayload::warning,
        ByteBufCodecs.VAR_INT, LedgerStatusPayload::loaders, LedgerRequest.View.CODEC.apply(ByteBufCodecs.list(64)), LedgerStatusPayload::queue,
        LedgerStatusPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

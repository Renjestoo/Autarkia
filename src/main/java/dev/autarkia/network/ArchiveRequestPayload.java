package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.storage.DomainArchiveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S domain archive request (04 section 2.8, M9 G7): the player asks the open archive for an amount of an item,
 * delivered into the archive buffer (target {@link DomainArchiveBlockEntity#TO_BUFFER}) or to the RECEIVE conduit of
 * channel target (0 to 15).
 */
public record ArchiveRequestPayload(BlockPos pos, ItemStack item, int amount, int target) implements CustomPacketPayload {
    public static final Type<ArchiveRequestPayload> TYPE = new Type<>(Autarkia.id("archive_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArchiveRequestPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
        ArchiveRequestPayload::pos, ItemStack.STREAM_CODEC, ArchiveRequestPayload::item, ByteBufCodecs.VAR_INT, ArchiveRequestPayload::amount,
        ByteBufCodecs.VAR_INT, ArchiveRequestPayload::target, ArchiveRequestPayload::new);

    /** Handle the request on the server thread; only a player with this archive's menu open may request. */
    public static void handle(ArchiveRequestPayload p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof dev.autarkia.registry.SlotMenu m && m.pos.equals(p.pos)
            && sp.level().getBlockEntity(p.pos) instanceof DomainArchiveBlockEntity a) {
            a.request(sp, p.item, p.amount, p.target);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

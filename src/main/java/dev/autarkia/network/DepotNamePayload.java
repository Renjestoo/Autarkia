package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.storage.DepotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S depot site name (04 section 4.1): the name field of the depot screen, at most 32 characters. */
public record DepotNamePayload(BlockPos pos, String name) implements CustomPacketPayload {
    public static final Type<DepotNamePayload> TYPE = new Type<>(Autarkia.id("depot_name"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DepotNamePayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, DepotNamePayload::pos,
        ByteBufCodecs.stringUtf8(DepotBlockEntity.NAME_LENGTH), DepotNamePayload::name, DepotNamePayload::new);

    /** Rename the depot on the server thread; only a player with that depot's menu open may rename it. */
    public static void handle(DepotNamePayload p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof SlotMenu m && m.pos.equals(p.pos)
            && sp.level().getBlockEntity(p.pos) instanceof DepotBlockEntity d) {
            d.setSiteName(p.name.strip());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

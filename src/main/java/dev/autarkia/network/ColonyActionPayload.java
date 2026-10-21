package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.ColonyActions;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S colony action (05b section 9.1): project, job, worker and settings actions, checked against the 05 section 1.4 rights. */
public record ColonyActionPayload(UUID colony, String action, List<String> args) implements CustomPacketPayload {
    public static final Type<ColonyActionPayload> TYPE = new Type<>(Autarkia.id("colony_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ColonyActionPayload> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, ColonyActionPayload::colony, ByteBufCodecs.STRING_UTF8, ColonyActionPayload::action,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(8)), ColonyActionPayload::args, ColonyActionPayload::new);

    /** Handle the action on the server thread. */
    public static void handle(ColonyActionPayload p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp) {
            ColonyActions.handle(sp, p.colony, p.action, p.args);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

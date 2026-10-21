package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.ColonyPreview;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S ghost preview request (05 section 4.1): one project, or every open project of the colony the player stands in
 * (planning tablet held). The server answers with one {@link ProjectPreviewPayload} per project.
 */
public record PreviewRequestPayload(Optional<UUID> projectId) implements CustomPacketPayload {
    public static final Type<PreviewRequestPayload> TYPE = new Type<>(Autarkia.id("preview_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PreviewRequestPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), PreviewRequestPayload::projectId, PreviewRequestPayload::new);

    /** Handle the request on the server thread. */
    public static void handle(PreviewRequestPayload p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp) {
            ColonyPreview.answer(sp, p.projectId.orElse(null));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

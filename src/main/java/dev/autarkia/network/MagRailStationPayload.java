package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.logistics.MagRailStationBlockEntity;
import dev.autarkia.logistics.MagRailStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S mag rail station settings (04 section 4.1): name and destination ("" for Next), each at most 16 characters. */
public record MagRailStationPayload(BlockPos pos, String name, String destination) implements CustomPacketPayload {
    public static final Type<MagRailStationPayload> TYPE = new Type<>(Autarkia.id("mag_rail_station"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MagRailStationPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
        MagRailStationPayload::pos, ByteBufCodecs.stringUtf8(MagRailStationBlockEntity.NAME_LENGTH), MagRailStationPayload::name,
        ByteBufCodecs.stringUtf8(MagRailStationBlockEntity.NAME_LENGTH), MagRailStationPayload::destination, MagRailStationPayload::new);

    /** Apply the settings on the server; only a player with that station's menu open may change them. */
    public static void handle(MagRailStationPayload p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof MagRailStationMenu m && m.pos.equals(p.pos)
            && sp.level().getBlockEntity(p.pos) instanceof MagRailStationBlockEntity s) {
            s.setSettings(p.name, p.destination, sp);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

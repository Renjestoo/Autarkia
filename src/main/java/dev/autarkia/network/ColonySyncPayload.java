package dev.autarkia.network;

import dev.autarkia.Autarkia;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C colony screen data for the active tab (05b section 9.1), sent every 20 ticks while a colony menu is open. */
public record ColonySyncPayload(UUID colony, int tab, CompoundTag data) implements CustomPacketPayload {
    public static final Type<ColonySyncPayload> TYPE = new Type<>(Autarkia.id("colony_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ColonySyncPayload> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, ColonySyncPayload::colony, ByteBufCodecs.VAR_INT, ColonySyncPayload::tab,
        ByteBufCodecs.COMPOUND_TAG, ColonySyncPayload::data, ColonySyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package dev.autarkia.network;

import dev.autarkia.Autarkia;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C domain flash (02 section 6.4, decision G5): the {@code industrial_lamp} positions (at most 256) that flash for 10 ticks. */
public record DomainFlashPayload(List<BlockPos> lamps) implements CustomPacketPayload {
    public static final int MAX_LAMPS = 256;
    public static final int FLASH_TICKS = 10;
    public static final Type<DomainFlashPayload> TYPE = new Type<>(Autarkia.id("domain_flash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DomainFlashPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LAMPS)), DomainFlashPayload::lamps, DomainFlashPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

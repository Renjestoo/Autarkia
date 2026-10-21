package dev.autarkia.network;

import dev.autarkia.Autarkia;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C worker debug overlay lines for {@code /autarkia worker info} (08b section 8), shown on the HUD for 200 ticks. */
public record WorkerDebugPayload(List<String> lines) implements CustomPacketPayload {
    public static final Type<WorkerDebugPayload> TYPE = new Type<>(Autarkia.id("worker_debug"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorkerDebugPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(32)), WorkerDebugPayload::lines, WorkerDebugPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package dev.autarkia.network;

import dev.autarkia.Autarkia;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C ghost preview of a project (05 section 4.1); an empty entry list clears the preview of that project. */
public record ProjectPreviewPayload(UUID projectId, int argb, List<Ghost> entries, List<SpanLine> spans) implements CustomPacketPayload {
    public static final Type<ProjectPreviewPayload> TYPE = new Type<>(Autarkia.id("project_preview"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProjectPreviewPayload> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, ProjectPreviewPayload::projectId,
        ByteBufCodecs.INT, ProjectPreviewPayload::argb,
        Ghost.CODEC.apply(ByteBufCodecs.list()), ProjectPreviewPayload::entries,
        SpanLine.CODEC.apply(ByteBufCodecs.list()), ProjectPreviewPayload::spans,
        ProjectPreviewPayload::new);

    /** One ghost block: packed position and block state id. */
    public record Ghost(long pos, int stateId) {
        public static final StreamCodec<ByteBuf, Ghost> CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG, Ghost::pos, ByteBufCodecs.VAR_INT, Ghost::stateId, Ghost::new);

        /** Return the unpacked position. */
        public BlockPos blockPos() {
            return BlockPos.of(pos);
        }
    }

    /** One planned span between two block positions with its cable tier ordinal. */
    public record SpanLine(long a, long b, byte tier) {
        public static final StreamCodec<ByteBuf, SpanLine> CODEC = StreamCodec.composite(
            ByteBufCodecs.LONG, SpanLine::a, ByteBufCodecs.LONG, SpanLine::b, ByteBufCodecs.BYTE, SpanLine::tier, SpanLine::new);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

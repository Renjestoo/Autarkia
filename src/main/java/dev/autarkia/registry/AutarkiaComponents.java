package dev.autarkia.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.Autarkia;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data component types of 08b section 5. */
public final class AutarkiaComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Autarkia.MOD_ID);

    /** First click of a span placement: node, clicked face and game time (expires after 600 ticks). */
    public record LinkStart(GlobalPos node, Direction face, long time) {
        public static final Codec<LinkStart> CODEC = RecordCodecBuilder.create(i -> i.group(
            GlobalPos.CODEC.fieldOf("node").forGetter(LinkStart::node),
            Direction.CODEC.fieldOf("face").forGetter(LinkStart::face),
            Codec.LONG.fieldOf("time").forGetter(LinkStart::time)).apply(i, LinkStart::new));
        public static final StreamCodec<ByteBuf, LinkStart> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC, LinkStart::node, Direction.STREAM_CODEC, LinkStart::face, ByteBufCodecs.VAR_LONG, LinkStart::time, LinkStart::new);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LinkStart>> LINK_START = COMPONENTS.registerComponentType("link_start",
        b -> b.persistent(LinkStart.CODEC).networkSynchronized(LinkStart.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = COMPONENTS.registerComponentType("energy",
        b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.autarkia.equipment.ToolMode>> TOOL_MODE = COMPONENTS.registerComponentType(
        "tool_mode", b -> b.persistent(dev.autarkia.equipment.ToolMode.CODEC).networkSynchronized(dev.autarkia.equipment.ToolMode.STREAM_CODEC));

    private AutarkiaComponents() {
    }
}

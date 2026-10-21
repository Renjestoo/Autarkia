package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.equipment.ToolMode;
import dev.autarkia.registry.AutarkiaComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S equipment key action (07 section 2.5): 0 = cycle the tool mode of the main-hand item. */
public record EquipmentActionPayload(byte action) implements CustomPacketPayload {
    public static final Type<EquipmentActionPayload> TYPE = new Type<>(Autarkia.id("equipment_action"));
    public static final StreamCodec<ByteBuf, EquipmentActionPayload> CODEC = ByteBufCodecs.BYTE.map(EquipmentActionPayload::new, EquipmentActionPayload::action);
    public static final byte MODE = 0;

    /** Handle the action on the server thread. */
    public static void handle(EquipmentActionPayload p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player) || p.action != MODE) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        ToolMode mode = held.get(AutarkiaComponents.TOOL_MODE.get());
        if (mode == null) {
            player.sendOverlayMessage(Component.translatable("message.autarkia.mode.none"));
            return;
        }
        ToolMode next = mode.next();
        held.set(AutarkiaComponents.TOOL_MODE.get(), next);
        player.sendOverlayMessage(Component.translatable("message.autarkia.mode", Component.translatable(next.key())));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

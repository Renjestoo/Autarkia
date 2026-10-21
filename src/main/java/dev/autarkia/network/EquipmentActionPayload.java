package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.equipment.AegisMultitoolItem;
import dev.autarkia.equipment.EnchantMode;
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
import org.jspecify.annotations.Nullable;

/**
 * C2S equipment key action (07 section 2.5): 0 = cycle the tool mode of the main-hand item, 1 = turn the worn suit on or
 * off, 2 = cycle the enchant mode of the main-hand {@code aegis_multitool}.
 */
public record EquipmentActionPayload(byte action) implements CustomPacketPayload {
    public static final Type<EquipmentActionPayload> TYPE = new Type<>(Autarkia.id("equipment_action"));
    public static final StreamCodec<ByteBuf, EquipmentActionPayload> CODEC = ByteBufCodecs.BYTE.map(EquipmentActionPayload::new, EquipmentActionPayload::action);
    public static final byte MODE = 0;
    public static final byte SUIT = 1;
    public static final byte ENCHANT = 2;

    /** Handle the action on the server thread. */
    public static void handle(EquipmentActionPayload p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player) {
            apply(player, p.action);
        }
    }

    /** Run an equipment action for the player, show its action-bar message and return that message (null for an unknown action). */
    public static @Nullable Component apply(ServerPlayer player, byte action) {
        Component msg = switch (action) {
            case SUIT -> {
                Boolean on = dev.autarkia.equipment.SuitItem.toggle(player);
                yield Component.translatable(on == null ? "message.autarkia.suit.none" : on ? "message.autarkia.suit.on" : "message.autarkia.suit.off");
            }
            case MODE -> mode(player.getMainHandItem());
            case ENCHANT -> enchant(player);
            default -> null;
        };
        if (msg != null) {
            player.sendOverlayMessage(msg);
        }
        return msg;
    }

    private static Component mode(ItemStack held) {
        ToolMode mode = held.get(AutarkiaComponents.TOOL_MODE.get());
        if (mode == null || !(held.getItem() instanceof dev.autarkia.equipment.ModalItem modal)) {
            return Component.translatable("message.autarkia.mode.none");
        }
        ToolMode next = modal.next(mode);
        held.set(AutarkiaComponents.TOOL_MODE.get(), next);
        return Component.translatable("message.autarkia.mode", Component.translatable(next.key()));
    }

    private static Component enchant(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof AegisMultitoolItem)) {
            return Component.translatable("message.autarkia.enchant.none");
        }
        EnchantMode next = AegisMultitoolItem.cycleEnchant(held, player.level().registryAccess());
        return Component.translatable("message.autarkia.enchant", Component.translatable(next.key()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

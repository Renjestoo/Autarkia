package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.storage.MatterBufferBlockEntity;
import dev.autarkia.storage.MatterBufferMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * C2S matter buffer action (04 section 4.1): {@link #EXTRACT} one stack of the given item to the player, {@link #DISSOLVE}
 * all stored items of that kind (the screen asks for a confirm first), {@link #AUTO} toggle auto-dissolve.
 */
public record MatterBufferActionPayload(BlockPos pos, int action, ItemStack item) implements CustomPacketPayload {
    public static final int EXTRACT = 0;
    public static final int DISSOLVE = 1;
    public static final int AUTO = 2;
    public static final Type<MatterBufferActionPayload> TYPE = new Type<>(Autarkia.id("matter_buffer_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MatterBufferActionPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
        MatterBufferActionPayload::pos, ByteBufCodecs.VAR_INT, MatterBufferActionPayload::action, ItemStack.OPTIONAL_STREAM_CODEC,
        MatterBufferActionPayload::item, MatterBufferActionPayload::new);

    /** Run the action on the server; only a player with that buffer's menu open may act. */
    public static void handle(MatterBufferActionPayload p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp) || !(sp.containerMenu instanceof MatterBufferMenu m) || !m.pos.equals(p.pos)
            || !(sp.level().getBlockEntity(p.pos) instanceof MatterBufferBlockEntity b)) {
            return;
        }
        switch (p.action) {
            case EXTRACT -> {
                if (p.item.isEmpty()) {
                    return;
                }
                ItemStack out = b.extractStack(ItemResource.of(p.item));
                sp.getInventory().add(out);
                if (!out.isEmpty()) {
                    Containers.dropItemStack(sp.level(), sp.getX(), sp.getY() + 0.5, sp.getZ(), out);
                }
            }
            case DISSOLVE -> {
                if (!p.item.isEmpty()) {
                    b.dissolve(ItemResource.of(p.item));
                }
            }
            case AUTO -> b.setAutoDissolve(!b.autoDissolve());
            default -> {
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

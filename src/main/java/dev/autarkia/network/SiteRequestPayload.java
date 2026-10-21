package dev.autarkia.network;

import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.storage.DomainStockData;
import dev.autarkia.storage.LedgerTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S "Request from site" (04 section 2.6, ledger Sites tab): the player at a ledger terminal asks a remote outpost
 * depot site (by site key) for an amount of an item; the server queues a {@link dev.autarkia.storage.TransferOrder}.
 */
public record SiteRequestPayload(BlockPos ledger, String site, ItemStack item, int amount) implements CustomPacketPayload {
    public static final Type<SiteRequestPayload> TYPE = new Type<>(Autarkia.id("site_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SiteRequestPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
        SiteRequestPayload::ledger, ByteBufCodecs.STRING_UTF8, SiteRequestPayload::site, ItemStack.STREAM_CODEC, SiteRequestPayload::item,
        ByteBufCodecs.VAR_INT, SiteRequestPayload::amount, SiteRequestPayload::new);

    /** Handle the request on the server thread; only a player with that ledger terminal's menu open may request. */
    public static void handle(SiteRequestPayload p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof SlotMenu m && m.pos.equals(p.ledger)
            && sp.level().getBlockEntity(p.ledger) instanceof LedgerTerminalBlockEntity) {
            String refusal = DomainStockData.request(sp.level().getServer(), p.site, p.item, p.amount);
            if (refusal != null) {
                sp.sendSystemMessage(Component.translatable(refusal));
                return;
            }
            sp.sendSystemMessage(Component.translatable("message.autarkia.sites.ordered"));
            sp.level().playSound(null, p.ledger, AutarkiaSounds.of("ledger_request"), SoundSource.BLOCKS, 0.6f, 1.2f);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

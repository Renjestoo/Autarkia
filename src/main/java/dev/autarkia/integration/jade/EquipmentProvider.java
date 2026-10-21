package dev.autarkia.integration.jade;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colony;
import dev.autarkia.energy.DomainPowerIndex;
import dev.autarkia.equipment.BastionBlock;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade lines of {@code bastion_block} and {@code bastion_glass} (07 section 7.1, provider {@code EquipmentProvider} of
 * 10 section 4): "Protected (grid 12.4M FE)" while powered, else "Unprotected". The server streams the powered flag and
 * the grid FE under {@code autarkia:bastion_data}; the client component is {@code autarkia:bastion}. The Forge of
 * Ascension lines come through its {@code JadeInfo} group {@code equipment}.
 */
public final class EquipmentProvider {
    private EquipmentProvider() {
    }

    /** Streamed Bastion state. */
    public record Bastion(boolean powered, long gridFe) {
        static final StreamCodec<RegistryFriendlyByteBuf, Bastion> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Bastion::powered,
            ByteBufCodecs.VAR_LONG, Bastion::gridFe, Bastion::new);
    }

    /** Server side: the powered flag and the grid FE of the protecting colony. */
    public enum Data implements StreamServerDataProvider<BlockAccessor, Bastion> {
        INSTANCE;

        @Override
        public Bastion streamData(BlockAccessor accessor) {
            if (!(accessor.getLevel() instanceof ServerLevel sl) || !BastionBlock.bastion(accessor.getBlockState())) {
                return null;
            }
            Colony c = BastionBlock.protector(sl, accessor.getPosition());
            return c == null ? new Bastion(false, 0) : new Bastion(true, DomainPowerIndex.gridStored(sl, c));
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Bastion> streamCodec() {
            return Bastion.CODEC;
        }

        @Override
        public Identifier getUid() {
            return Autarkia.id("bastion_data");
        }
    }

    /** Client side: the Protected or Unprotected line. */
    public enum View implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Data.INSTANCE.decodeFromData(accessor).ifPresent(b -> tooltip.add(b.powered()
                ? Component.translatable("jade.autarkia.bastion.protected", shortFe(b.gridFe())).withStyle(ChatFormatting.GOLD)
                : Component.translatable("jade.autarkia.bastion.unprotected").withStyle(ChatFormatting.GRAY)));
        }

        @Override
        public Identifier getUid() {
            return Autarkia.id("bastion");
        }
    }

    /** Return an FE amount in the short form of 07 section 7.1 (12.4M, 980.0k, 512). */
    public static String shortFe(long fe) {
        if (fe >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.1fG", fe / 1.0e9);
        }
        if (fe >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", fe / 1.0e6);
        }
        if (fe >= 1_000L) {
            return String.format(Locale.ROOT, "%.1fk", fe / 1.0e3);
        }
        return Long.toString(fe);
    }
}

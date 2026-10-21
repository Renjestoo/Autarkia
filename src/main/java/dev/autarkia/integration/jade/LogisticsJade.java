package dev.autarkia.integration.jade;

import dev.autarkia.Autarkia;
import dev.autarkia.logistics.CatchNetBlock;
import dev.autarkia.registry.JadeInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.nikdo53.tinymultiblocklib.block.IMultiBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

/**
 * Jade providers of the T4 logistics objects that are not Autarkia block entities (04 section 4.2): the
 * {@code catch_net} (the library's simple multiblock entity) shows "Output below: OK / Missing / Full"; the
 * {@code gondola} and {@code falling_crate} entities stream their {@link JadeInfo} lines under
 * {@code autarkia:logistics_entity_data} and draw them under {@code autarkia:logistics_entity}.
 */
public final class LogisticsJade {
    private LogisticsJade() {
    }

    /** Server side: the catch net output line, streamed for every position of a net. */
    public enum NetData implements StreamServerDataProvider<BlockAccessor, AutarkiaJadePlugin.Lines> {
        INSTANCE;

        @Override
        public AutarkiaJadePlugin.Lines streamData(BlockAccessor accessor) {
            if (!(accessor.getBlock() instanceof CatchNetBlock)) {
                return null;
            }
            CatchNetBlock.Output out = CatchNetBlock.output(accessor.getLevel(), IMultiBlock.getCenter(accessor.getLevel(), accessor.getPosition()));
            return new AutarkiaJadePlugin.Lines(List.of(Component.translatable("jade.autarkia.catch_net",
                Component.translatable("jade.autarkia.catch_net." + out.name().toLowerCase(Locale.ROOT)))), -1);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AutarkiaJadePlugin.Lines> streamCodec() {
            return AutarkiaJadePlugin.Lines.CODEC;
        }

        @Override
        public Identifier getUid() {
            return Autarkia.id("catch_net_data");
        }
    }

    /** Client side: draws the catch net line. */
    public enum NetView implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            NetData.INSTANCE.decodeFromData(accessor).ifPresent(l -> l.lines().forEach(tooltip::add));
        }

        @Override
        public Identifier getUid() {
            return Autarkia.id("catch_net");
        }
    }

    /** Server side: the lines of a logistics entity that implements {@link JadeInfo}. */
    public enum EntityData implements StreamServerDataProvider<EntityAccessor, AutarkiaJadePlugin.Lines> {
        INSTANCE;

        @Override
        public AutarkiaJadePlugin.Lines streamData(EntityAccessor accessor) {
            if (!(accessor.getEntity() instanceof JadeInfo info)) {
                return null;
            }
            List<Component> out = new ArrayList<>();
            info.jadeLines(out);
            return new AutarkiaJadePlugin.Lines(out, info.jadeProgress());
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AutarkiaJadePlugin.Lines> streamCodec() {
            return AutarkiaJadePlugin.Lines.CODEC;
        }

        @Override
        public Identifier getUid() {
            return Autarkia.id("logistics_entity_data");
        }
    }

    /** Client side: draws the entity lines with the optional progress bar. */
    public enum EntityView implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            EntityData.INSTANCE.decodeFromData(accessor).ifPresent(l -> {
                if (l.progress() >= 0) {
                    tooltip.add(JadeUI.progress(new ProgressView(ProgressView.Part.of(Math.min(1, l.progress())), null, JadeUI.progressStyle(),
                        BoxStyle.nestedBox())));
                }
                l.lines().forEach(tooltip::add);
            });
        }

        @Override
        public Identifier getUid() {
            return Autarkia.id("logistics_entity");
        }
    }
}

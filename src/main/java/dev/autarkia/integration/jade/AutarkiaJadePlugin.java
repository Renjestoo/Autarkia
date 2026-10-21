package dev.autarkia.integration.jade;

import dev.autarkia.Autarkia;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

/**
 * Jade plugin (10 section 4): one provider per group (power, machine, logistics). Each streams the server-side
 * {@link JadeInfo} lines under id {@code autarkia:<group>_data}; a separate client component with id
 * {@code autarkia:<group>} draws them (Jade refuses one provider that is both).
 */
@WailaPlugin(Autarkia.MOD_ID)
public class AutarkiaJadePlugin implements IWailaPlugin {
    static final List<String> GROUPS = List.of("power", "machine", "logistics");

    /** Streamed tooltip data. */
    public record Lines(List<Component> lines, float progress) {
        static final StreamCodec<RegistryFriendlyByteBuf, Lines> CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()), Lines::lines, ByteBufCodecs.FLOAT, Lines::progress, Lines::new);
    }

    static JadeInfo info(BlockEntity be) {
        if (be instanceof IMultiBlockEntity m && !m.isCenter() && be.getLevel() != null) {
            BlockEntity c = be.getLevel().getBlockEntity(m.getCenter());
            return c instanceof JadeInfo j ? j : null;
        }
        return be instanceof JadeInfo j ? j : null;
    }

    /** Server side: streams the lines of one group. */
    record Data(String group) implements StreamServerDataProvider<BlockAccessor, Lines> {
        @Override
        public Lines streamData(BlockAccessor accessor) {
            JadeInfo info = info(accessor.getBlockEntity());
            if (info == null || !info.jadeGroup().equals(group)) {
                return null;
            }
            List<Component> out = new ArrayList<>();
            info.jadeLines(out);
            return new Lines(out, info.jadeProgress());
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Lines> streamCodec() {
            return Lines.CODEC;
        }

        @Override
        public Identifier getUid() {
            return Autarkia.id(group + "_data");
        }
    }

    /** Client side: draws the streamed lines and the progress bar. */
    record View(Data data) implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            data.decodeFromData(accessor).ifPresent(l -> {
                if (l.progress() >= 0) {
                    tooltip.add(JadeUI.progress(new ProgressView(ProgressView.Part.of(Math.min(1, l.progress())), null,
                        JadeUI.progressStyle(), BoxStyle.nestedBox())));
                }
                l.lines().forEach(tooltip::add);
            });
        }

        @Override
        public Identifier getUid() {
            return Autarkia.id(data.group());
        }
    }

    private static final List<Data> DATA = GROUPS.stream().map(Data::new).toList();

    @Override
    public void register(IWailaCommonRegistration reg) {
        for (Data d : DATA) {
            reg.registerBlockDataProvider(d, dev.autarkia.registry.AutarkiaBlockEntity.class);
        }
    }

    @Override
    public void registerClient(IWailaClientRegistration reg) {
        for (Data d : DATA) {
            View v = new View(d);
            reg.registerBlockComponent(v, MachineBlock.class);
            reg.registerBlockComponent(v, BaseMultiblock.class);
        }
    }
}

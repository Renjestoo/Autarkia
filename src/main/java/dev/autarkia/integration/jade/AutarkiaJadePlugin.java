package dev.autarkia.integration.jade;

import dev.autarkia.Autarkia;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.worker.WorkerEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
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
 * Jade plugin (10 section 4): one provider per group (power, machine, logistics, colony). Each streams the server-side
 * {@link JadeInfo} lines under id {@code autarkia:<group>_data}; a separate client component with id
 * {@code autarkia:<group>} draws them (Jade refuses one provider that is both). Workers stream under
 * {@code autarkia:worker_data} and show the four lines of 06 section 7.1.
 */
@WailaPlugin(Autarkia.MOD_ID)
public class AutarkiaJadePlugin implements IWailaPlugin {
    static final List<String> GROUPS = List.of("power", "machine", "logistics", "colony", "equipment");
    static final List<String> WORKER_PARTS = List.of("worker_state", "worker_job", "worker_energy", "worker_cargo");

    /** Streamed worker data (06 section 7.1). */
    public record Worker(String state, String job, int step, int steps, int energy, int capacity, boolean linked, List<ItemStack> cargo) {
        static final StreamCodec<RegistryFriendlyByteBuf, Worker> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Worker::state, ByteBufCodecs.STRING_UTF8, Worker::job, ByteBufCodecs.VAR_INT, Worker::step,
            ByteBufCodecs.VAR_INT, Worker::steps, ByteBufCodecs.VAR_INT, Worker::energy, ByteBufCodecs.VAR_INT, Worker::capacity,
            ByteBufCodecs.BOOL, Worker::linked, ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), Worker::cargo, Worker::new);
    }

    /** Server side: streams the worker state, job, spring and cargo. */
    enum WorkerData implements StreamServerDataProvider<EntityAccessor, Worker> {
        INSTANCE;

        @Override
        public Worker streamData(EntityAccessor accessor) {
            if (!(accessor.getEntity() instanceof WorkerEntity w)) {
                return null;
            }
            int[] p = w.orderProgress();
            List<ItemStack> cargo = w.cargo.stream().filter(s -> !s.isEmpty()).map(ItemStack::copy).toList();
            return new Worker(w.stateName(), w.jobName(), p[0], p[1], w.energy(), w.workerType.capacity(), w.colony() != null, cargo);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Worker> streamCodec() {
            return Worker.CODEC;
        }

        @Override
        public Identifier getUid() {
            return Autarkia.id("worker_data");
        }
    }

    /** Client side: one Jade line of the worker data, by config id. */
    record WorkerView(String part) implements IEntityComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            WorkerData.INSTANCE.decodeFromData(accessor).ifPresent(w -> {
                switch (part) {
                    case "worker_state" -> {
                        if (!w.linked()) {
                            tooltip.add(Component.translatable("jade.autarkia.worker.unlinked").withStyle(ChatFormatting.GRAY));
                        } else if (w.state().startsWith("failed:")) {
                            tooltip.add(Component.translatable("jade.autarkia.worker.failed",
                                Component.translatable("jade.autarkia.worker.reason." + w.state().substring(7))).withStyle(ChatFormatting.RED));
                        } else {
                            tooltip.add(Component.translatable("jade.autarkia.worker.state." + w.state()).withStyle(ChatFormatting.AQUA));
                        }
                    }
                    case "worker_job" -> tooltip.add(w.job().isEmpty() ? Component.translatable("jade.autarkia.worker.no_job")
                        : Component.translatable("jade.autarkia.worker.job", Component.translatable("jade.autarkia.worker.job." + w.job()), w.step(), w.steps()));
                    case "worker_energy" -> tooltip.add(JadeUI.progress(new ProgressView(ProgressView.Part.of(Math.min(1f, (float) w.energy() / Math.max(1, w.capacity()))),
                        Component.translatable("jade.autarkia.worker.spring", w.energy(), w.capacity()), JadeUI.progressStyle(), BoxStyle.nestedBox())));
                    default -> {
                        if (!w.cargo().isEmpty()) {
                            tooltip.add(w.cargo().stream().map(JadeUI::item).toList());
                        }
                    }
                }
            });
        }

        @Override
        public Identifier getUid() {
            return Autarkia.id(part);
        }
    }

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
        reg.registerEntityDataProvider(WorkerData.INSTANCE, WorkerEntity.class);
        reg.registerBlockDataProvider(LogisticsJade.NetData.INSTANCE, net.nikdo53.tinymultiblocklib.blockentities.SimpleMultiBlockEntity.class);
        reg.registerEntityDataProvider(LogisticsJade.EntityData.INSTANCE, dev.autarkia.logistics.GondolaEntity.class);
        reg.registerEntityDataProvider(LogisticsJade.EntityData.INSTANCE, dev.autarkia.storage.FallingCrateEntity.class);
        reg.registerEntityDataProvider(LogisticsJade.EntityData.INSTANCE, dev.autarkia.worker.NaniteSwarmEntity.class);
        reg.registerBlockDataProvider(EquipmentProvider.Data.INSTANCE, dev.autarkia.equipment.BastionBlock.class);
        reg.registerBlockDataProvider(EquipmentProvider.Data.INSTANCE, dev.autarkia.equipment.BastionGlassBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration reg) {
        for (Data d : DATA) {
            View v = new View(d);
            reg.registerBlockComponent(v, MachineBlock.class);
            reg.registerBlockComponent(v, BaseMultiblock.class);
        }
        for (String part : WORKER_PARTS) {
            reg.registerEntityComponent(new WorkerView(part), WorkerEntity.class);
        }
        reg.registerBlockComponent(LogisticsJade.NetView.INSTANCE, dev.autarkia.logistics.CatchNetBlock.class);
        reg.registerEntityComponent(LogisticsJade.EntityView.INSTANCE, dev.autarkia.logistics.GondolaEntity.class);
        reg.registerEntityComponent(LogisticsJade.EntityView.INSTANCE, dev.autarkia.storage.FallingCrateEntity.class);
        reg.registerEntityComponent(LogisticsJade.EntityView.INSTANCE, dev.autarkia.worker.NaniteSwarmEntity.class);
        reg.registerBlockComponent(OutcropProvider.INSTANCE, dev.autarkia.processing.OreOutcropBlock.class);
        reg.registerBlockComponent(ChargedShellProvider.INSTANCE, dev.autarkia.processing.LatticeGeodeShellBlock.class);
        reg.registerBlockComponent(EquipmentProvider.View.INSTANCE, dev.autarkia.equipment.BastionBlock.class);
        reg.registerBlockComponent(EquipmentProvider.View.INSTANCE, dev.autarkia.equipment.BastionGlassBlock.class);
    }
}

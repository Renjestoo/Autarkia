package dev.autarkia.integration.jade;

import dev.autarkia.Autarkia;
import dev.autarkia.processing.OreOutcropBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Client Jade line of an {@code ore_outcrop} (03 section 7.2): {@code Marks a <ore> deposit below}, read from the block state. */
public enum OutcropProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        if (state.getBlock() instanceof OreOutcropBlock) {
            String ore = state.getValue(OreOutcropBlock.ORE).getSerializedName();
            tooltip.add(Component.translatable("jade.autarkia.outcrop", Component.translatable("jade.autarkia.outcrop." + ore)));
        }
    }

    @Override
    public Identifier getUid() {
        return Autarkia.id("ore_outcrop");
    }
}

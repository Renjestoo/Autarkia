package dev.autarkia.integration.jade;

import dev.autarkia.Autarkia;
import dev.autarkia.processing.LatticeGeodeShellBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Client Jade line of a {@code lattice_geode_shell} (03 section 7.2): {@code Charged} when {@code charged=true}, read from the block state. */
public enum ChargedShellProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        if (state.getBlock() instanceof LatticeGeodeShellBlock && state.getValue(LatticeGeodeShellBlock.CHARGED)) {
            tooltip.add(Component.translatable("jade.autarkia.charged").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    public Identifier getUid() {
        return Autarkia.id("lattice_geode_shell");
    }
}

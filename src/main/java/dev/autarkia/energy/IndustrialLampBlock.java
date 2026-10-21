package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Industrial lamp (02 section 7): a 1 FE/t consumer that lights at level 15 while it is powered. */
public class IndustrialLampBlock extends MachineBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public IndustrialLampBlock(Properties props) {
        super(props, AutarkiaBlockEntities.INDUSTRIAL_LAMP, false);
    }

    @Override
    protected BlockState defaultState(BlockState state) {
        return state.setValue(LIT, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }
}

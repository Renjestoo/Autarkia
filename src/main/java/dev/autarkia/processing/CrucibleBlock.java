package dev.autarkia.processing;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

/** Clay crucible: block model with a molten surface that glows while alloying. */
public class CrucibleBlock extends MachineBlock {
    public static final BooleanProperty MOLTEN = BooleanProperty.create("molten");

    public CrucibleBlock(Properties props) {
        super(props, AutarkiaBlockEntities.CRUCIBLE, false,
            Shapes.join(Block.box(2, 0, 2, 14, 12, 14), Block.box(4, 3, 4, 12, 12, 12), BooleanOp.ONLY_FIRST));
    }

    @Override
    protected BlockState defaultState(BlockState state) {
        return state.setValue(MOLTEN, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MOLTEN);
    }
}

package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Capacitor bank (02 section 6.2): a 50,000 FE storage block that faces the player and shows 0 to 4 charge stripes. */
public class CapacitorBankBlock extends MachineBlock {
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 4);

    public CapacitorBankBlock(Properties props) {
        super(props, AutarkiaBlockEntities.CAPACITOR_BANK, false);
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }

    @Override
    protected BlockState defaultState(BlockState state) {
        return state.setValue(CHARGE, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CHARGE);
    }
}

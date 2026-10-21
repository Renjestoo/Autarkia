package dev.autarkia.storage;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;

/** Tally board block: the front face shows the 6 largest item counts of nearby storage (04 section 2.2). */
public class TallyBoardBlock extends MachineBlock {
    public TallyBoardBlock(Properties props) {
        super(props, AutarkiaBlockEntities.TALLY_BOARD, false);
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }
}

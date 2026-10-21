package dev.autarkia.storage;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;

/** Crate block: 27-slot storage with item piles on top (04 section 2.1). */
public class CrateBlock extends MachineBlock {
    public CrateBlock(Properties props) {
        super(props, AutarkiaBlockEntities.CRATE, false);
    }
}

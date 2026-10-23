package dev.autarkia.processing;

import dev.autarkia.registry.MachineBlock;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** A one-block machine whose front faces the player (plate press, wire drawer, battery bank and the other T3 boxes). */
public class FormingMachineBlock extends MachineBlock {
    public FormingMachineBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        this(props, type, false);
    }

    /** Create the machine; geo machines are drawn by their GeckoLib renderer and the block model only gives the particle texture. */
    public FormingMachineBlock(Properties props, Supplier<? extends BlockEntityType<?>> type, boolean geo) {
        super(props, type, geo);
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }
}

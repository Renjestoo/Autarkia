package dev.autarkia.processing;

import dev.autarkia.registry.MachineBlock;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** A one-block machine whose front faces the player (plate press, wire drawer). */
public class FormingMachineBlock extends MachineBlock {
    public FormingMachineBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props, type, false);
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }
}

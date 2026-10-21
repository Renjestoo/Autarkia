package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.BigMachineBlock;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Boiler, a 2x3x2 big block (02 section 6.2); a burst removes it without drops. */
public class BoilerBlock extends BigMachineBlock {
    public BoilerBlock(Properties props) {
        super(props, AutarkiaBlockEntities.BOILER, box(0, 0, 0, 1, 2, 1), true);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BoilerBlockEntity b && b.suppressDrops()) {
            return List.of();
        }
        return super.getDrops(state, params);
    }
}

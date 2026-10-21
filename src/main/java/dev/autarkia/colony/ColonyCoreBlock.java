package dev.autarkia.colony;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.BigMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.nikdo53.tinymultiblocklib.block.IMultiBlock;

/**
 * The Colony Core, a 3x3x3 big block (08a). Only the owner or a trusted player can break it (05 section 1.4); then the
 * colony goes dormant and the Core drops a {@code core_seed} linked to the colony, which restores it when planted.
 */
public class ColonyCoreBlock extends BigMachineBlock {
    public ColonyCoreBlock(Properties props) {
        super(props, AutarkiaBlockEntities.COLONY_CORE, BigMachineBlock.box(-1, 0, -1, 1, 2, 1), true);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                                  net.minecraft.world.phys.BlockHitResult hit) {
        if (level.getBlockEntity(IMultiBlock.getCenter(level, pos)) instanceof ColonyCoreBlockEntity core) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                Colony c = core.colony();
                if (c != null && c.state == Colony.State.ACTIVE) {
                    ColonyMenu.open(sp, c, ColonyMenu.Mode.CORE, core.getBlockPos());
                } else {
                    sp.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.autarkia.colony.growing", core.growth() * 100
                        / ColonyCoreBlockEntity.GROWTH_TICKS));
                }
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel sl && level.getBlockEntity(IMultiBlock.getCenter(level, pos)) instanceof ColonyCoreBlockEntity core
            && core.colonyId() != null && !player.isCreative()) {
            ItemStack seed = new ItemStack(AutarkiaItems.CORE_SEED.get());
            seed.set(AutarkiaComponents.COLONY_LINK.get(), core.colonyId());
            BlockPos c = core.getBlockPos();
            Containers.dropItemStack(sl, c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, seed);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}

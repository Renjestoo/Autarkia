package dev.autarkia.energy;

import dev.autarkia.registry.MachineBlock;
import dev.autarkia.storage.CrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.nikdo53.tinymultiblocklib.block.IMultiBlock;

/**
 * Wrench (02, 04): use turns an Autarkia block (rope pulley: next of 6 facings; others: next horizontal facing) or runs
 * its own wrench action ({@link Wrenchable}); sneak + use picks a crate up with its contents, or any other Autarkia
 * block as its item.
 */
public class WrenchItem extends Item implements MachineBlock.BlockTool {
    public WrenchItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || ctx.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("autarkia")) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            return pickUp(level, pos, state, player);
        }
        if (level.getBlockEntity(pos) instanceof Wrenchable w && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            return w.wrench(sp);
        }
        if (state.getBlock() instanceof IMultiBlock) {
            return InteractionResult.PASS;
        }
        BlockState turned = state;
        if (state.getBlock() instanceof RopePulleyBlock) {
            turned = state.setValue(MachineBlock.FACING, RopePulleyBlock.nextFacing(state.getValue(MachineBlock.FACING)));
        } else if (state.hasProperty(MachineBlock.HORIZONTAL_FACING)) {
            turned = state.setValue(MachineBlock.HORIZONTAL_FACING, state.getValue(MachineBlock.HORIZONTAL_FACING).getClockWise());
        }
        if (turned == state) {
            return InteractionResult.PASS;
        }
        level.setBlock(pos, turned, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult pickUp(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        if (level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
            ItemStack stack = crate.pickUp();
            level.removeBlock(pos, false);
            player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
            player.sendOverlayMessage(Component.translatable("message.autarkia.wrench.crate"));
            return InteractionResult.SUCCESS;
        }
        BlockPos center = state.getBlock() instanceof IMultiBlock ? IMultiBlock.getCenter(level, pos) : pos;
        level.destroyBlock(center, !player.getAbilities().instabuild, player);
        return InteractionResult.SUCCESS;
    }
}

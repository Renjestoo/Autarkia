package dev.autarkia.equipment;

import dev.autarkia.registry.BigMachineBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.nikdo53.tinymultiblocklib.block.IMultiBlock;

/**
 * The {@code forge_of_ascension} big block (07 section 3.5, 3x4x3): a full 3x3 floor whose middle is the pad and the
 * center, 4 corner pillars two blocks high and a full 3x3 roof; the middle column and the side middles of the two
 * pillar layers stay open, so the player walks in and stands on the pad. Sneak + use with both hands empty on the pad
 * starts a fitting; any other use opens the menu.
 */
public class ForgeOfAscensionBlock extends BigMachineBlock {
    public ForgeOfAscensionBlock(Properties props, Supplier<? extends BlockEntityType<?>> type) {
        super(props, type, footprint(), true);
    }

    /** Return the 26 occupied offsets of the open chamber (facing north, relative to the center). */
    public static List<Vec3i> footprint() {
        List<Vec3i> out = new ArrayList<>();
        for (Vec3i o : BigMachineBlock.box(-1, 0, -1, 1, 3, 1)) {
            boolean pillarLayer = o.getY() == 1 || o.getY() == 2;
            boolean corner = o.getX() != 0 && o.getZ() != 0;
            if (!pillarLayer || corner) {
                out.add(o);
            }
        }
        return out;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos center = IMultiBlock.getCenter(level, pos);
        if (player.isShiftKeyDown() && pos.equals(center) && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) {
            if (player instanceof ServerPlayer sp && level.getBlockEntity(center) instanceof ForgeOfAscensionBlockEntity forge) {
                Component refusal = forge.start(sp);
                if (refusal != null) {
                    sp.sendOverlayMessage(refusal);
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}

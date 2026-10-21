package dev.autarkia.colony;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The pattern loom block (05 section 5.1); its menu also carries the id of the colony whose domain holds it. */
public class PatternLoomBlock extends MachineBlock {
    public PatternLoomBlock(Properties props) {
        super(props, AutarkiaBlockEntities.PATTERN_LOOM, false);
    }

    @Override
    protected Facing facingMode() {
        return Facing.HORIZONTAL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof PatternLoomBlockEntity be) {
            if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
                Colony c = Colonies.server() == null ? null : Colonies.at(sl, pos);
                UUID id = c == null ? new UUID(0, 0) : c.id;
                sp.openMenu(be, buf -> {
                    buf.writeBlockPos(pos);
                    UUIDUtil.STREAM_CODEC.encode(buf, id);
                });
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}

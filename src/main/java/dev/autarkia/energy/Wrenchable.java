package dev.autarkia.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

/** A block entity with its own wrench action (transformer mode, fuse reset, coil face mode), run instead of turning the block. */
public interface Wrenchable {
    /** Run the wrench action for the player and return the interaction result. */
    InteractionResult wrench(ServerPlayer player);

    /** Run the wrench action for a click on one face of the block at clicked; by default the face does not matter. */
    default InteractionResult wrench(ServerPlayer player, BlockPos clicked, Direction face) {
        return wrench(player);
    }
}

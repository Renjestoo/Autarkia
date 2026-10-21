package dev.autarkia.energy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

/** A block entity with its own wrench action (transformer mode, fuse reset), run instead of turning the block. */
public interface Wrenchable {
    /** Run the wrench action for the player and return the interaction result. */
    InteractionResult wrench(ServerPlayer player);
}

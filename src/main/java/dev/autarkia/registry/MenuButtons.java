package dev.autarkia.registry;

import net.minecraft.world.entity.player.Player;

/** A block entity whose {@link SlotMenu} has buttons; the menu forwards each button click to it on the server. */
public interface MenuButtons {
    /** Handle menu button id for the player; return false for an unknown id. */
    boolean menuButton(Player player, int id);
}

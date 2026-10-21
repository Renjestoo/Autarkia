package dev.autarkia.command;

import dev.autarkia.equipment.ForgeOfAscensionBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * The Aegis rule piece of {@code /autarkia showcase m9} (20 x 20 blocks from origin): a Forge of Ascension with its arms
 * idling and its 27 slots loaded with the inputs of all four fittings, an armor stand in the full Aegis set beside it,
 * and a Bastion wall (block and glass) in front. Placed in a powered T6 domain, the wall shows "Protected" in Jade and
 * holds against explosions and Withers; a player in lattice carapace starts the fitting with sneak + use on the pad.
 */
public final class ShowcaseM9Rule {
    private ShowcaseM9Rule() {
    }

    /** Build the piece; origin is the north-west floor corner (the floor itself is the scene's). */
    public static void buildRule(ServerLevel level, BlockPos origin) {
        BlockPos forge = origin.offset(6, 0, 6);
        if (!TestM1.placeBigAt(level, forge, AutarkiaBlocks.FORGE_OF_ASCENSION.get(), Direction.SOUTH)) {
            throw new IllegalStateException("showcase m9 rule: the Forge of Ascension footprint at " + forge.toShortString() + " is not free");
        }
        if (level.getBlockEntity(forge) instanceof ForgeOfAscensionBlockEntity f) {
            f.inventory().put(0, new ItemStack(AutarkiaItems.AEGIS_PLATE.get(), 24));
            f.inventory().put(1, new ItemStack(AutarkiaItems.AI_CORE.get(), 4));
            f.inventory().put(2, new ItemStack(AutarkiaItems.NANITE_PASTE.get(), 22));
        }
        ArmorStand stand = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
        stand.snapTo(origin.getX() + 11.5, origin.getY(), origin.getZ() + 6.5, 90, 0);
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AutarkiaItems.AEGIS_HELMET.get()));
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AutarkiaItems.AEGIS_CHEST.get()));
        stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(AutarkiaItems.AEGIS_LEGS.get()));
        stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(AutarkiaItems.AEGIS_BOOTS.get()));
        level.addFreshEntity(stand);
        for (int dx = 0; dx < 14; dx++) {
            for (int y = 0; y < 4; y++) {
                Block b = (dx >= 5 && dx <= 8 && y >= 1 && y <= 2 ? AutarkiaBlocks.BASTION_GLASS : AutarkiaBlocks.BASTION_BLOCK).get();
                level.setBlock(origin.offset(3 + dx, y, 14), b.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}

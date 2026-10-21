package dev.autarkia.command;

import dev.autarkia.equipment.AegisMultitoolItem;
import dev.autarkia.equipment.EnchantMode;
import dev.autarkia.equipment.PoweredItem;
import dev.autarkia.equipment.SingularityLanceItem;
import dev.autarkia.equipment.ToolMode;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.BeamVisualPayload;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The gear piece of {@code /autarkia showcase m9}: in a 20 x 20 area, an armor stand in the full Aegis set (charged, so
 * the GeckoLib orbit rings and glow seams show) holding the singularity lance and the aegis multitool (5x5, Fortune III),
 * a second stand with the aegis blade, and a black-gold target wall the lance beam visual hits for 30 seconds.
 */
public final class ShowcaseM9Gear {
    private static final int BEAM_TICKS = 600;

    private ShowcaseM9Gear() {
    }

    /** Build the gear piece with its corner at origin (floor at origin y - 1); the beam visual reaches players within 128 blocks. */
    public static void buildGear(ServerLevel level, BlockPos origin) {
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(0, -1, 0), origin.offset(19, -1, 19))) {
            level.setBlock(p, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(4, 0, 17), origin.offset(14, 4, 17))) {
            boolean seam = (p.getX() + p.getY()) % 3 == 0;
            level.setBlock(p, seam ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
        }
        ArmorStand suit = stand(level, origin.offset(9, 0, 4));
        suit.setItemSlot(EquipmentSlot.HEAD, charged(AutarkiaItems.AEGIS_HELMET.get()));
        suit.setItemSlot(EquipmentSlot.CHEST, charged(AutarkiaItems.AEGIS_CHEST.get()));
        suit.setItemSlot(EquipmentSlot.LEGS, charged(AutarkiaItems.AEGIS_LEGS.get()));
        suit.setItemSlot(EquipmentSlot.FEET, charged(AutarkiaItems.AEGIS_BOOTS.get()));
        ItemStack lance = charged(AutarkiaItems.SINGULARITY_LANCE.get());
        lance.set(AutarkiaComponents.TOOL_MODE.get(), ToolMode.TERRAIN_DESTROY);
        suit.setItemSlot(EquipmentSlot.MAINHAND, lance);
        ItemStack tool = charged(AutarkiaItems.AEGIS_MULTITOOL.get());
        tool.set(AutarkiaComponents.TOOL_MODE.get(), ToolMode.AREA5);
        AegisMultitoolItem.applyEnchant(tool, EnchantMode.FORTUNE, level.registryAccess());
        suit.setItemSlot(EquipmentSlot.OFFHAND, tool);
        ArmorStand blade = stand(level, origin.offset(13, 0, 6));
        blade.setItemSlot(EquipmentSlot.MAINHAND, charged(AutarkiaItems.AEGIS_BLADE.get()));
        Vec3 from = suit.position().add(0, 1.4, 0.6);
        Vec3 to = Vec3.atCenterOf(origin.offset(9, 1, 17)).add(0, 0.5, -0.5);
        AutarkiaNetwork.toNear(level, from, 128, new BeamVisualPayload(suit.getId(), from, to, BEAM_TICKS, SingularityLanceItem.COLOR,
            BeamVisualPayload.KIND_LANCE, 0.5f));
    }

    private static ArmorStand stand(ServerLevel level, BlockPos at) {
        ArmorStand s = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
        s.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        s.setShowArms(true);
        level.addFreshEntity(s);
        return s;
    }

    private static ItemStack charged(Item item) {
        ItemStack s = new ItemStack(item);
        PoweredItem.setEnergy(s, Integer.MAX_VALUE);
        return s;
    }
}

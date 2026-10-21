package dev.autarkia.energy;

import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.SlotMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;

/** Boiler menu (02 section 9.1): fuel, water bucket and empty bucket slots; menu buttons 0 to 4 set the fire level. */
public class BoilerMenu extends SlotMenu {
    public BoilerMenu(int id, Inventory playerInv, BlockPos pos, MachineInventory inv, ContainerData data) {
        super(AutarkiaMenus.BOILER.get(), id, playerInv, pos, inv, data, GuiLayouts.BOILER);
    }

    /** Client constructor. */
    public BoilerMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        super(AutarkiaMenus.BOILER.get(), id, playerInv, buf, GuiLayouts.BOILER);
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button < 0 || button > 4 || !(player.level().getBlockEntity(pos) instanceof BoilerBlockEntity b)) {
            return false;
        }
        b.setFire(button);
        return true;
    }
}

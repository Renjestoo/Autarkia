package dev.autarkia.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * Generic block menu: the block entity slots at their layout positions, the player inventory, and the container data
 * values (progress, energy, state and extras). The client side builds an empty mirror from the layout.
 */
public class SlotMenu extends AbstractContainerMenu {
    public final GuiLayout layout;
    public final BlockPos pos;
    private final ContainerData data;
    private final int own;

    public SlotMenu(MenuType<?> type, int id, Inventory playerInv, BlockPos pos, ItemStacksResourceHandler handler, ContainerData data, GuiLayout layout) {
        super(type, id);
        this.layout = layout;
        this.pos = pos;
        this.data = data;
        this.own = layout.slots().length;
        for (int i = 0; i < own; i++) {
            addSlot(new ResourceHandlerSlot(handler, handler::set, i, layout.slots()[i][0], layout.slots()[i][1]));
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, layout.invY() + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(playerInv, c, 8 + c * 18, layout.invY() + 58));
        }
        addDataSlots(data);
    }

    /** Client constructor: reads the block position and mirrors the layout. */
    public SlotMenu(MenuType<?> type, int id, Inventory playerInv, RegistryFriendlyByteBuf buf, GuiLayout layout) {
        this(type, id, playerInv, buf.readBlockPos(), new ItemStacksResourceHandler(layout.slots().length), new SimpleContainerData(layout.data()), layout);
    }

    /** Return synced data value i. */
    public int value(int i) {
        return data.get(i);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < own) {
            if (!moveItemStackTo(stack, own, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int end = layout.inputs() > 0 ? layout.inputs() : own;
            if (!moveItemStackTo(stack, 0, end, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return player.level().getBlockEntity(pos) instanceof MenuButtons b && b.menuButton(player, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) != null && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}

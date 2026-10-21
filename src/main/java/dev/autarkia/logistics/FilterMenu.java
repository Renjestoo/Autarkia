package dev.autarkia.logistics;

import dev.autarkia.registry.AutarkiaMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Menu of the {@code belt_splitter} and {@code sorting_chute} (04 section 4.1): 9 ghost filter slots (a click sets a
 * one-item copy of the carried stack or clears the slot; nothing is consumed), and buttons 0 mode (splitter), 1 side,
 * 2 whitelist, 3 match components.
 */
public class FilterMenu extends AbstractContainerMenu {
    public static final int GRID_X = 98;
    public static final int GRID_Y = 17;
    public final BlockPos pos;
    private final ContainerData data;
    private final @Nullable FilterBlockEntity be;

    /** A container view over the ghost list; changes on the server go to the block entity. */
    private record Ghosts(NonNullList<ItemStack> list, @Nullable FilterBlockEntity be) implements Container {
        @Override
        public int getContainerSize() {
            return list.size();
        }

        @Override
        public boolean isEmpty() {
            return list.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return list.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (be != null) {
                be.setGhost(slot, stack);
            } else {
                list.set(slot, stack);
            }
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < list.size(); i++) {
                setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /** A slot that shows a ghost stack and never holds a real item. */
    private static final class GhostSlot extends Slot {
        GhostSlot(Container c, int i, int x, int y) {
            super(c, i, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    public FilterMenu(int id, Inventory playerInv, FilterBlockEntity be) {
        this(be.isSplitter() ? AutarkiaMenus.BELT_SPLITTER.get() : AutarkiaMenus.SORTING_CHUTE.get(), id, playerInv, be.getBlockPos(),
            new Ghosts(be.filter(), be), be.data(), be);
    }

    /** Client constructor for the menu type of the block (belt_splitter or sorting_chute). */
    public FilterMenu(MenuType<?> type, int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(type, id, playerInv, buf.readBlockPos(), new Ghosts(NonNullList.withSize(9, ItemStack.EMPTY), null), new SimpleContainerData(5), null);
    }

    private FilterMenu(MenuType<?> type, int id, Inventory playerInv, BlockPos pos, Container ghosts, ContainerData data, @Nullable FilterBlockEntity be) {
        super(type, id);
        this.pos = pos;
        this.data = data;
        this.be = be;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                addSlot(new GhostSlot(ghosts, c + r * 3, GRID_X + c * 18, GRID_Y + r * 18));
            }
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(playerInv, c, 8 + c * 18, 142));
        }
        addDataSlots(data);
    }

    /** Return synced value: 0 filter mode, 1 right side, 2 whitelist, 3 match components, 4 splitter. */
    public int value(int i) {
        return data.get(i);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < 9) {
            slots.get(slotId).set(getCarried().isEmpty() ? ItemStack.EMPTY : getCarried().copyWithCount(1));
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || id < 0 || id > 3) {
            return false;
        }
        be.button(id);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof FilterBlockEntity && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}

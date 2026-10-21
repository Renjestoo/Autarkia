package dev.autarkia.logistics;

import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayout;
import dev.autarkia.registry.GuiLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jspecify.annotations.Nullable;

/**
 * Menu of the {@code capsule_station} (04 section 4.1), laid out by {@link GuiLayouts#CAPSULE_STATION}: slots 0 to 8
 * input, 9 to 17 output (take only), 18 to 26 the ghost filter (a click sets a one-item copy of the carried stack or
 * clears the slot), then the player inventory. Buttons: 0 send, 1 receive, 2 both, 3 priority down, 4 priority up,
 * 5 whitelist, 6 match components. Data: mode, priority, whitelist, components, tubes, stations, capsules, state.
 */
public class CapsuleStationMenu extends AbstractContainerMenu {
    public static final int BUFFER = 2 * CapsuleStationBlockEntity.SLOTS;
    public static final int GHOSTS = 9;
    public static final int OWN = BUFFER + GHOSTS;
    public final GuiLayout layout = GuiLayouts.CAPSULE_STATION;
    public final BlockPos pos;
    private final ContainerData data;
    private final @Nullable CapsuleStationBlockEntity be;

    /** A container view over the ghost list; changes on the server go to the block entity. */
    private record Ghosts(NonNullList<ItemStack> list, @Nullable CapsuleStationBlockEntity be) implements Container {
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

    public CapsuleStationMenu(int id, Inventory playerInv, CapsuleStationBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), be.inventory(), new Ghosts(be.filter(), be), be.data(), be);
    }

    /** Client constructor: reads the block position and mirrors the slots. */
    public CapsuleStationMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(id, playerInv, buf.readBlockPos(), new MachineInventory(CapsuleStationBlockEntity.SLOTS, CapsuleStationBlockEntity.SLOTS, s -> true, () -> { }),
            new Ghosts(NonNullList.withSize(GHOSTS, ItemStack.EMPTY), null), new SimpleContainerData(8), null);
    }

    private CapsuleStationMenu(int id, Inventory playerInv, BlockPos pos, MachineInventory inv, Container ghosts, ContainerData data,
                               @Nullable CapsuleStationBlockEntity be) {
        super(AutarkiaMenus.CAPSULE_STATION.get(), id);
        this.pos = pos;
        this.data = data;
        this.be = be;
        int[][] at = layout.slots();
        for (int i = 0; i < BUFFER; i++) {
            addSlot(new ResourceHandlerSlot(inv, inv::set, i, at[i][0], at[i][1]));
        }
        for (int i = 0; i < GHOSTS; i++) {
            addSlot(new GhostSlot(ghosts, i, at[BUFFER + i][0], at[BUFFER + i][1]));
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

    /** Return synced value: 0 mode, 1 priority, 2 whitelist, 3 match components, 4 tubes, 5 stations, 6 capsules, 7 state. */
    public int value(int i) {
        return data.get(i);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= BUFFER && slotId < OWN) {
            slots.get(slotId).set(getCarried().isEmpty() ? ItemStack.EMPTY : getCarried().copyWithCount(1));
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return be != null && be.button(id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= BUFFER && index < OWN) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < BUFFER) {
            if (!moveItemStackTo(stack, OWN, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, CapsuleStationBlockEntity.SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof CapsuleStationBlockEntity
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}

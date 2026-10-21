package dev.autarkia.logistics;

import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaItems;
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
 * Menu of the {@code mag_rail_station} (04 section 4.1), laid out by {@link GuiLayouts#MAG_RAIL_STATION}: slots 0 to 26
 * the input and 27 to 53 the output (take only) on the same 3 x 9 grid, shown one at a time by tab; 54 the crate slot;
 * 55 to 63 the ghost filter (a click sets a one-item copy of the carried stack or clears it, nothing is consumed); then
 * the player inventory. Buttons: 0 input tab, 1 output tab, 2 whitelist, 3 match components. Data: see
 * {@link MagRailStationBlockEntity#data()}. Name and destination go to the server as {@code mag_rail_station} payloads.
 */
public class MagRailStationMenu extends AbstractContainerMenu {
    public static final int GRID = MagRailStationBlockEntity.SLOTS;
    public static final int CRATE = 2 * GRID;
    public static final int GHOSTS = CRATE + 1;
    public static final int OWN = GHOSTS + 9;
    public final GuiLayout layout = GuiLayouts.MAG_RAIL_STATION;
    public final BlockPos pos;
    private final ContainerData data;
    private final @Nullable MagRailStationBlockEntity be;
    private int tab;

    /** A buffer slot shown only while its tab is open. */
    private final class TabSlot extends ResourceHandlerSlot {
        private final int shownOn;

        TabSlot(MachineInventory inv, int index, int x, int y, int shownOn) {
            super(inv, inv::set, index, x, y);
            this.shownOn = shownOn;
        }

        @Override
        public boolean isActive() {
            return tab == shownOn;
        }
    }

    /** A container view over the ghost list; changes on the server go to the block entity. */
    private record Ghosts(NonNullList<ItemStack> list, @Nullable MagRailStationBlockEntity be) implements Container {
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

    public MagRailStationMenu(int id, Inventory playerInv, MagRailStationBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), be.inventory(), new Ghosts(be.filter(), be), be.data(), be);
    }

    /** Client constructor: reads the block position and mirrors the slots. */
    public MagRailStationMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(id, playerInv, buf.readBlockPos(), new MachineInventory(GRID + 1, GRID, s -> true, () -> { }),
            new Ghosts(NonNullList.withSize(9, ItemStack.EMPTY), null), new SimpleContainerData(8), null);
    }

    private MagRailStationMenu(int id, Inventory playerInv, BlockPos pos, MachineInventory inv, Container ghosts, ContainerData data,
                               @Nullable MagRailStationBlockEntity be) {
        super(AutarkiaMenus.MAG_RAIL_STATION.get(), id);
        this.pos = pos;
        this.data = data;
        this.be = be;
        int[][] at = layout.slots();
        for (int i = 0; i < GRID; i++) {
            addSlot(new TabSlot(inv, i, at[i][0], at[i][1], 0));
        }
        for (int i = 0; i < GRID; i++) {
            addSlot(new TabSlot(inv, MagRailStationBlockEntity.OUT + i, at[GRID + 1 + i][0], at[GRID + 1 + i][1], 1));
        }
        addSlot(new ResourceHandlerSlot(inv, inv::set, MagRailStationBlockEntity.CRATE_SLOT, at[GRID][0], at[GRID][1]));
        for (int i = 0; i < 9; i++) {
            addSlot(new GhostSlot(ghosts, i, at[2 * GRID + 1 + i][0], at[2 * GRID + 1 + i][1]));
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

    /** Return synced value: 0 energy, 1 capacity, 2 status, 3 sleds in flight, 4 line rails, 5 line stations, 6 whitelist, 7 components. */
    public int value(int i) {
        return data.get(i);
    }

    /** Return the open tab: 0 input, 1 output. */
    public int tab() {
        return tab;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= GHOSTS && slotId < OWN) {
            slots.get(slotId).set(getCarried().isEmpty() ? ItemStack.EMPTY : getCarried().copyWithCount(1));
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 || id == 1) {
            tab = id;
            return true;
        }
        return be != null && be.button(id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (index >= GHOSTS && index < OWN || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < GHOSTS) {
            if (!moveItemStackTo(stack, OWN, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean crate = stack.is(AutarkiaItems.CRATE.get());
            if (!(crate && moveItemStackTo(stack, CRATE, CRATE + 1, false)) && !moveItemStackTo(stack, 0, GRID, false)) {
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
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof MagRailStationBlockEntity
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}

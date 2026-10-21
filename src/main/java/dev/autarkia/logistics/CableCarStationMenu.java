package dev.autarkia.logistics;

import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayout;
import dev.autarkia.registry.GuiLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jspecify.annotations.Nullable;

/**
 * Menu of the {@code cable_car_station} (04 section 4.1), laid out by {@link GuiLayouts#CABLE_CAR_STATION}: slots 0 to
 * 26 the input buffer and 27 to 53 the output buffer (take only) on the same 3 x 9 grid, shown one at a time by tab;
 * 54 and 55 the gondola parts; then the player inventory. Buttons: 0 input tab, 1 output tab, 2 one gondola less, 3
 * one gondola more. Data: see {@link CableCarStationBlockEntity#data()}.
 */
public class CableCarStationMenu extends AbstractContainerMenu {
    public static final int GRID = CableCarStationBlockEntity.SLOTS;
    public static final int PARTS = 2 * GRID;
    public static final int OWN = PARTS + 2;
    public final GuiLayout layout = GuiLayouts.CABLE_CAR_STATION;
    public final BlockPos pos;
    private final ContainerData data;
    private final @Nullable CableCarStationBlockEntity be;
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

    public CableCarStationMenu(int id, Inventory playerInv, CableCarStationBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), be.inventory(), be.parts(), be.data(), be);
    }

    /** Client constructor: reads the block position and mirrors the slots. */
    public CableCarStationMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(id, playerInv, buf.readBlockPos(), new MachineInventory(GRID, GRID, s -> true, () -> { }), new ItemStacksResourceHandler(2),
            new SimpleContainerData(9), null);
    }

    private CableCarStationMenu(int id, Inventory playerInv, BlockPos pos, MachineInventory inv, ItemStacksResourceHandler parts, ContainerData data,
                                @Nullable CableCarStationBlockEntity be) {
        super(AutarkiaMenus.CABLE_CAR_STATION.get(), id);
        this.pos = pos;
        this.data = data;
        this.be = be;
        int[][] at = layout.slots();
        for (int i = 0; i < GRID; i++) {
            addSlot(new TabSlot(inv, i, at[i][0], at[i][1], 0));
        }
        for (int i = 0; i < GRID; i++) {
            addSlot(new TabSlot(inv, GRID + i, at[GRID + 2 + i][0], at[GRID + 2 + i][1], 1));
        }
        for (int i = 0; i < 2; i++) {
            addSlot(new ResourceHandlerSlot(parts, parts::set, i, at[GRID + i][0], at[GRID + i][1]));
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

    /** Return synced value: 0 count, 1 max, 2 gondolas, 3 length, 4 state, 5 swing x10, 6 energy, 7 capacity, 8 linked. */
    public int value(int i) {
        return data.get(i);
    }

    /** Return the open tab: 0 input, 1 output. */
    public int tab() {
        return tab;
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
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < OWN) {
            if (!moveItemStackTo(stack, OWN, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean part = stack.is(AutarkiaItems.STEEL_PLATE.get()) || stack.is(Items.IRON_CHAIN);
            if (!(part && moveItemStackTo(stack, PARTS, OWN, false)) && !moveItemStackTo(stack, 0, GRID, false)) {
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
        return player.level().getBlockEntity(pos) instanceof CableCarStationBlockEntity
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}

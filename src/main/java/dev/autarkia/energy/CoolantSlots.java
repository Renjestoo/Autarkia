package dev.autarkia.energy;

import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Coolant cell slots of a cryo node or a superconducting coil (02 section 4.6, decision G8): each slot holds one
 * {@code coolant_cell}; a cell carries its units left in the {@code autarkia:coolant} component (a fresh cell has
 * 24,000) and is used up when they reach 0. Units are drawn from the cells in place, so taking the cells out stops the
 * cooling at once. Automation may insert cells but never extract them.
 */
public class CoolantSlots extends MachineInventory {
    public static final int CELL_UNITS = 24000;

    public CoolantSlots(int slots, Runnable changed) {
        super(slots, 0, s -> s.is(AutarkiaItems.COOLANT_CELL.get()), changed);
    }

    /** Return the coolant units left in one cell stack (0 for an empty stack). */
    public static int unitsOf(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getOrDefault(AutarkiaComponents.COOLANT.get(), CELL_UNITS);
    }

    /** Return the units left in all cells. */
    public int units() {
        int n = 0;
        for (int i = 0; i < size(); i++) {
            n += unitsOf(get(i));
        }
        return n;
    }

    /** Return the units all slots hold when every slot has a fresh cell. */
    public int capacity() {
        return size() * CELL_UNITS;
    }

    /** Return the number of cells in the slots. */
    public int cells() {
        int n = 0;
        for (int i = 0; i < size(); i++) {
            n += get(i).isEmpty() ? 0 : 1;
        }
        return n;
    }

    /**
     * Draw up to amount units, first slot first; an emptied cell is removed.
     *
     * @return the units drawn (less than amount when the cells run out)
     */
    public int draw(int amount) {
        int left = amount;
        for (int i = 0; i < size() && left > 0; i++) {
            ItemStack s = get(i);
            int have = unitsOf(s);
            if (have <= 0) {
                continue;
            }
            int take = Math.min(have, left);
            left -= take;
            if (take == have) {
                put(i, ItemStack.EMPTY);
            } else {
                ItemStack next = s.copyWithCount(1);
                next.set(AutarkiaComponents.COOLANT.get(), have - take);
                put(i, next);
            }
        }
        return amount - left;
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return 1;
    }
}

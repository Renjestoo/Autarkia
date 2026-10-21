package dev.autarkia.processing;

import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Item slots of a machine: the first {@code inputs} slots accept items that pass the filter, the rest are output
 * slots that only the machine fills. {@link #automation()} is the handler other blocks see (03 section 4.3).
 */
public class MachineInventory extends ItemStacksResourceHandler {
    private final int inputs;
    private final Predicate<ItemStack> filter;
    private final Runnable changed;
    private final ResourceHandler<ItemResource> automation = new Automation();

    public MachineInventory(int inputs, int outputs, Predicate<ItemStack> filter, Runnable changed) {
        super(inputs + outputs);
        this.inputs = inputs;
        this.filter = filter;
        this.changed = changed;
    }

    /** Return the number of input slots. */
    public int inputs() {
        return inputs;
    }

    /** Return the stack in a slot (live view; do not mutate). */
    public ItemStack get(int slot) {
        return stacks.get(slot);
    }

    /** Overwrite a slot. */
    public void put(int slot, ItemStack stack) {
        set(slot, ItemResource.of(stack), stack.getCount());
    }

    /** Shrink a slot by count. */
    public void shrink(int slot, int count) {
        ItemStack s = stacks.get(slot).copy();
        s.shrink(count);
        put(slot, s.isEmpty() ? ItemStack.EMPTY : s);
    }

    /** Return true when every stack fits into the output slots (simulated stacking). */
    public boolean fitsOutputs(java.util.List<ItemStack> results) {
        ItemStack[] sim = new ItemStack[size() - inputs];
        for (int i = 0; i < sim.length; i++) {
            sim[i] = stacks.get(inputs + i).copy();
        }
        for (ItemStack r : results) {
            int left = r.getCount();
            for (int i = 0; i < sim.length && left > 0; i++) {
                if (!sim[i].isEmpty() && ItemStack.isSameItemSameComponents(sim[i], r)) {
                    int add = Math.min(left, sim[i].getMaxStackSize() - sim[i].getCount());
                    sim[i].grow(add);
                    left -= add;
                }
            }
            for (int i = 0; i < sim.length && left > 0; i++) {
                if (sim[i].isEmpty()) {
                    int add = Math.min(left, r.getMaxStackSize());
                    sim[i] = r.copyWithCount(add);
                    left -= add;
                }
            }
            if (left > 0) {
                return false;
            }
        }
        return true;
    }

    /** Add stacks to the output slots; callers check {@link #fitsOutputs} first. */
    public void addOutputs(java.util.List<ItemStack> results) {
        for (ItemStack r : results) {
            int left = r.getCount();
            for (int i = inputs; i < size() && left > 0; i++) {
                ItemStack cur = stacks.get(i);
                if (!cur.isEmpty() && ItemStack.isSameItemSameComponents(cur, r)) {
                    int add = Math.min(left, cur.getMaxStackSize() - cur.getCount());
                    put(i, cur.copyWithCount(cur.getCount() + add));
                    left -= add;
                }
            }
            for (int i = inputs; i < size() && left > 0; i++) {
                if (stacks.get(i).isEmpty()) {
                    int add = Math.min(left, r.getMaxStackSize());
                    put(i, r.copyWithCount(add));
                    left -= add;
                }
            }
        }
    }

    /** Return the sum of item counts in the input slots. */
    public int queue() {
        int n = 0;
        for (int i = 0; i < inputs; i++) {
            n += stacks.get(i).getCount();
        }
        return n;
    }

    /** Return the handler exposed to other blocks: insert into inputs, extract from outputs. */
    public ResourceHandler<ItemResource> automation() {
        return automation;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return index < inputs && filter.test(resource.toStack(1));
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        changed.run();
    }

    private final class Automation implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return MachineInventory.this.size();
        }

        @Override
        public ItemResource getResource(int index) {
            return MachineInventory.this.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return MachineInventory.this.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return MachineInventory.this.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return MachineInventory.this.isValid(index, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext tx) {
            return index < inputs ? MachineInventory.this.insert(index, resource, amount, tx) : 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
            return index >= inputs ? MachineInventory.this.extract(index, resource, amount, tx) : 0;
        }
    }
}

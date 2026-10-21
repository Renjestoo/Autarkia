package dev.autarkia.storage;

import dev.autarkia.registry.AutarkiaItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** The contents of a crate item stack (04 section 2.3): the 27 slots the Loader carries on its pallet. */
public final class CrateItems {
    private CrateItems() {
    }

    /** Take up to max of the item out of a crate item's contents; returns what was taken. */
    public static ItemStack take(ItemStack crate, ItemStack item, int max) {
        NonNullList<ItemStack> list = contents(crate);
        int got = 0;
        for (ItemStack s : list) {
            if (got < max && ItemStack.isSameItemSameComponents(s, item)) {
                int n = Math.min(max - got, s.getCount());
                s.shrink(n);
                got += n;
            }
        }
        crate.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
        return got == 0 ? ItemStack.EMPTY : item.copyWithCount(got);
    }

    /** Put a stack into a crate item's contents; returns what did not fit. */
    public static ItemStack put(ItemStack crate, ItemStack stack) {
        NonNullList<ItemStack> list = contents(crate);
        ItemStack rest = stack.copy();
        for (int i = 0; i < list.size() && !rest.isEmpty(); i++) {
            ItemStack s = list.get(i);
            if (s.isEmpty()) {
                list.set(i, rest.split(rest.getMaxStackSize()));
            } else if (ItemStack.isSameItemSameComponents(s, rest)) {
                int n = Math.min(rest.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(n);
                rest.shrink(n);
            }
        }
        crate.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
        return rest;
    }

    private static NonNullList<ItemStack> contents(ItemStack crate) {
        NonNullList<ItemStack> list = NonNullList.withSize(RackBlockEntity.CRATE_SLOTS, ItemStack.EMPTY);
        ItemContainerContents c = crate.get(DataComponents.CONTAINER);
        if (c != null) {
            c.copyInto(list);
        }
        return list;
    }

    /** Return whether a stack is a crate item (the Loader's pallet load). */
    public static boolean isCrate(ItemStack stack) {
        return stack.is(AutarkiaItems.CRATE.get());
    }
}

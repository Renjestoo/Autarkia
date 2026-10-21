package dev.autarkia.storage;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.SlotMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * Crate (04 section 2.1): 27 slots on all faces. A filled crate item is refused (04 rule C6). Shift + wrench picks it
 * up with its contents in {@code minecraft:container}; a normal break drops the contents.
 */
public class CrateBlockEntity extends AutarkiaBlockEntity implements MenuProvider, dev.autarkia.registry.JadeInfo, dev.autarkia.colony.ColonyStorage,
    dev.autarkia.colony.ColonySensors.Member {
    @Override
    public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> colonyItems() {
        return items();
    }

    @Override
    public java.util.Set<dev.autarkia.colony.ColonySensors.Kind> colonyKinds() {
        return java.util.Set.of(dev.autarkia.colony.ColonySensors.Kind.STORAGE);
    }

    public static final int SLOTS = 27;
    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(SLOTS) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return !isFilledCrate(resource.toStack(1));
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            contentsChanged();
        }
    };
    private boolean pickedUp;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.CRATE.get(), pos, state);
    }

    /** Return true for a crate item that carries contents. */
    public static boolean isFilledCrate(ItemStack stack) {
        if (!stack.is(AutarkiaItems.CRATE.get())) {
            return false;
        }
        ItemContainerContents c = stack.get(DataComponents.CONTAINER);
        return c != null && c.nonEmptyItemCopyStream().findAny().isPresent();
    }

    /** Return the item handler. */
    public ItemStacksResourceHandler items() {
        return items;
    }

    /** Return a copy of slot i. */
    public ItemStack slot(int i) {
        return items.getResource(i).toStack(items.getAmountAsInt(i));
    }

    /** Return the used slot count. */
    public int usedSlots() {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (items.getAmountAsInt(i) > 0) {
                n++;
            }
        }
        return n;
    }

    private void contentsChanged() {
        setChanged();
        if (level instanceof ServerLevel) {
            syncNow();
        }
    }

    /** Turn the crate into an item stack with its contents and mark it so removal drops nothing else. */
    public ItemStack pickUp() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            list.add(slot(i));
        }
        ItemStack stack = new ItemStack(AutarkiaItems.CRATE.get());
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
        pickedUp = true;
        return stack;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        ItemContainerContents c = components.get(DataComponents.CONTAINER);
        if (c != null) {
            net.minecraft.core.NonNullList<ItemStack> nl = net.minecraft.core.NonNullList.withSize(SLOTS, ItemStack.EMPTY);
            c.copyInto(nl);
            for (int i = 0; i < SLOTS; i++) {
                items.set(i, ItemResource.of(nl.get(i)), nl.get(i).getCount());
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel && !pickedUp) {
            for (int i = 0; i < SLOTS; i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), slot(i));
            }
        }
    }

    @Override
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.fill", usedSlots(), SLOTS));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.autarkia.crate", Math.round(100f * usedSlots() / SLOTS));
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.CRATE.get(), id, playerInv, worldPosition, items, new SimpleContainerData(0), GuiLayouts.CRATE);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        items.serialize(out.child("items"));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("items").ifPresent(items::deserialize);
    }
}

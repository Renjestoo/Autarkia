package dev.autarkia.storage;

import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * The rack menu shared by every rack (04 section 4.1): one crate slot per crate position, the 27 slots of the crate
 * opened with button i, and the player inventory. Data: 0 load units, 1 rating, 2 stage, 3 seconds left, 4 positions,
 * 5 the opened position.
 */
public class RackMenu extends AbstractContainerMenu {
    public static final int CRATE_X = 8;
    public static final int CRATE_STEP = 40;
    public static final int CRATE_Y = 18;
    public static final int GRID_X = 8;
    public static final int GRID_Y = 60;
    public static final int INV_Y = 134;
    public final BlockPos pos;
    public final int positions;
    private final ContainerData data;
    private final int[] opened = {0};

    /** Crate positions as one-item slots: a crate item with its contents. */
    private record Crates(RackBlockEntity be) implements Container {
        @Override
        public int getContainerSize() {
            return be.positionCount();
        }

        @Override
        public boolean isEmpty() {
            return be.crateCount() == 0;
        }

        @Override
        public ItemStack getItem(int slot) {
            return be.crate(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack out = be.crate(slot);
            be.setCrate(slot, ItemStack.EMPTY);
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return removeItem(slot, 1);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            be.setCrate(slot, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            be.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < getContainerSize(); i++) {
                be.setCrate(i, ItemStack.EMPTY);
            }
        }
    }

    /** The 27 slots of the opened crate. */
    private record Opened(RackBlockEntity be, int[] opened) implements Container {
        private net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler h() {
            return be.position(opened[0]);
        }

        @Override
        public int getContainerSize() {
            return RackBlockEntity.CRATE_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < getContainerSize(); i++) {
                if (h().getAmountAsInt(i) > 0) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return h().getResource(slot).toStack(h().getAmountAsInt(slot));
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack now = getItem(slot);
            ItemStack out = now.split(count);
            h().set(slot, ItemResource.of(now), now.getCount());
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack out = getItem(slot);
            h().set(slot, ItemResource.EMPTY, 0);
            return out;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            h().set(slot, ItemResource.of(stack), stack.getCount());
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return h().isValid(slot, ItemResource.of(stack));
        }

        @Override
        public void setChanged() {
            be.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < getContainerSize(); i++) {
                h().set(i, ItemResource.EMPTY, 0);
            }
        }
    }

    public RackMenu(int id, Inventory playerInv, RackBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), be);
    }

    /** Client constructor: the block position; the position count comes from the rack block there. */
    public RackMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(id, playerInv, buf.readBlockPos(), null);
    }

    private RackMenu(int id, Inventory playerInv, BlockPos pos, @Nullable RackBlockEntity be) {
        super(AutarkiaMenus.RACK.get(), id);
        this.pos = pos;
        this.positions = be != null ? be.positionCount()
            : playerInv.player.level().getBlockState(pos).getBlock() instanceof RackBlock rb ? rb.positions : 2;
        Container c = be != null ? new Crates(be) : new SimpleContainer(positions);
        Container view = be != null ? new Opened(be, opened) : new SimpleContainer(RackBlockEntity.CRATE_SLOTS);
        for (int i = 0; i < positions; i++) {
            addSlot(new Slot(c, i, CRATE_X + i * CRATE_STEP, CRATE_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(AutarkiaItems.CRATE.get());
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int r = 0; r < 3; r++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(view, col + r * 9, GRID_X + col * 18, GRID_Y + r * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return container.canPlaceItem(getContainerSlot(), stack);
                    }
                });
            }
        }
        for (int r = 0; r < 3; r++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + r * 9 + 9, 8 + col * 18, INV_Y + r * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, INV_Y + 58));
        }
        ContainerData base = be != null ? be.data() : new SimpleContainerData(5);
        this.data = be != null ? new ContainerData() {
            @Override
            public int get(int i) {
                return i < 5 ? base.get(i) : opened[0];
            }

            @Override
            public void set(int i, int v) {
                if (i == 5) {
                    opened[0] = v;
                }
            }

            @Override
            public int getCount() {
                return 6;
            }
        } : new SimpleContainerData(6);
        addDataSlots(data);
    }

    /** Return synced value i (see the class comment). */
    public int value(int i) {
        return data.get(i);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= positions) {
            return false;
        }
        opened[0] = id;
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int own = positions + RackBlockEntity.CRATE_SLOTS;
        if (index < own) {
            if (!moveItemStackTo(stack, own, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(AutarkiaItems.CRATE.get()) && moveItemStackTo(stack, 0, positions, false)) {
            slot.setChanged();
        } else if (!moveItemStackTo(stack, positions, own, false)) {
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
        return player.level().getBlockEntity(pos) instanceof RackBlockEntity
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}

package dev.autarkia.colony;

import dev.autarkia.registry.AutarkiaMenus;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jspecify.annotations.Nullable;

/**
 * Pattern loom menu (05 section 5.1): the card slot, the player inventory, the box and toggle data, and buttons 0-14
 * (see {@link PatternLoomBlockEntity#button}). Recording goes through {@code colony_action} {@code pattern_record}
 * with the colony id sent when the menu opens (the nil id outside a domain).
 */
public class PatternLoomMenu extends AbstractContainerMenu {
    public static final int CARD_X = 152;
    public static final int CARD_Y = 18;
    public static final int INV_Y = 84;
    public final BlockPos pos;
    public final UUID colonyId;
    private final ContainerData data;
    private final @Nullable PatternLoomBlockEntity be;

    public PatternLoomMenu(int id, Inventory playerInv, PatternLoomBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), new UUID(0, 0), be.card(), be.data(), be);
    }

    /** Client constructor: block position and colony id. */
    public PatternLoomMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(id, playerInv, buf.readBlockPos(), UUIDUtil.STREAM_CODEC.decode(buf), new ItemStacksResourceHandler(1), new SimpleContainerData(9), null);
    }

    private PatternLoomMenu(int id, Inventory playerInv, BlockPos pos, UUID colonyId, ItemStacksResourceHandler card, ContainerData data,
                            @Nullable PatternLoomBlockEntity be) {
        super(AutarkiaMenus.PATTERN_LOOM.get(), id);
        this.pos = pos;
        this.colonyId = colonyId;
        this.data = data;
        this.be = be;
        addSlot(new ResourceHandlerSlot(card, card::set, 0, CARD_X, CARD_Y));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(playerInv, c, 8 + c * 18, INV_Y + 58));
        }
        addDataSlots(data);
    }

    /** Return synced value i: 0-2 offset, 3-5 size, 6 flags, 7 scan per mille (-1 idle), 8 max size. */
    public int value(int i) {
        return data.get(i);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || id < 0 || id > 14) {
            return false;
        }
        be.button(id);
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
        if (index == 0 ? !moveItemStackTo(stack, 1, slots.size(), true) : !moveItemStackTo(stack, 0, 1, false)) {
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
        return player.level().getBlockEntity(pos) instanceof PatternLoomBlockEntity
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}

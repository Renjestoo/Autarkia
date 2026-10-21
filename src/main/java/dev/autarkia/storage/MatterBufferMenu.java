package dev.autarkia.storage;

import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Menu of the {@code matter_buffer} (04 section 4.1), laid out by {@link GuiLayouts#MATTER_BUFFER}: only the player
 * inventory. A shift-click on a player slot inserts that stack into the buffer as far as it accepts it. The list,
 * the MU bar and the auto-dissolve state come from the synced block entity; Extract, Dissolve and the toggle go to the
 * server as {@code matter_buffer_action} payloads.
 */
public class MatterBufferMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    private final @Nullable MatterBufferBlockEntity be;

    public MatterBufferMenu(int id, Inventory playerInv, MatterBufferBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), be);
    }

    /** Client constructor: reads the center position. */
    public MatterBufferMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(id, playerInv, buf.readBlockPos(), null);
    }

    private MatterBufferMenu(int id, Inventory playerInv, BlockPos pos, @Nullable MatterBufferBlockEntity be) {
        super(AutarkiaMenus.MATTER_BUFFER.get(), id);
        this.pos = pos;
        this.be = be;
        int invY = GuiLayouts.MATTER_BUFFER.invY();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, invY + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(playerInv, c, 8 + c * 18, invY + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (be == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        int n = be.insertItems(stack, false);
        if (n > 0) {
            stack.shrink(n);
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof MatterBufferBlockEntity
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}

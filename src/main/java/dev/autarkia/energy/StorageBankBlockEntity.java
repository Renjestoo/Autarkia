package dev.autarkia.energy;

import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * FE storage block (02 section 6.2): a buffer with one I/O rate, an optional leak of a fraction of E per tick, a
 * storage priority group, and one item slot that charges an FE item at the charge rate. Menu {@code energy_storage}.
 */
public class StorageBankBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, JadeInfo, MenuProvider {
    private final EnergyBuffer buffer;
    private final MachineInventory inv = new MachineInventory(1, 0, s -> true, this::setChanged);
    private final int io;
    private final int chargeRate;
    private final double leak;
    private final int priority;
    private double leakCarry;
    private int lastIn;
    private int lastOut;
    private int pendingIn;
    private int pendingOut;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 2 -> buffer.stored();
                case 3 -> buffer.capacity();
                case 4 -> lastIn;
                case 5 -> lastOut;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    protected StorageBankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int io, int chargeRate, double leak,
                                     int priority) {
        super(type, pos, state);
        this.buffer = new EnergyBuffer(capacity, io, io, this::setChanged);
        this.io = io;
        this.chargeRate = chargeRate;
        this.leak = leak;
        this.priority = priority;
    }

    /** Return the battery bank (400,000 FE, I/O 1,024, no leak, group 2, charges items at 2,048 FE/t). */
    public static StorageBankBlockEntity battery(BlockPos pos, BlockState state) {
        return new StorageBankBlockEntity(AutarkiaBlockEntities.BATTERY_BANK.get(), pos, state, 400000, 1024, 2048, 0, 2);
    }

    /** Return the energy buffer. */
    public EnergyBuffer buffer() {
        return buffer;
    }

    /** Return the item slot inventory. */
    public MachineInventory inventory() {
        return inv;
    }

    @Override
    public void serverTick() {
        if (leak > 0) {
            leakCarry += leak * buffer.stored();
            int l = (int) leakCarry;
            if (l > 0) {
                buffer.drain(l);
                leakCarry -= l;
            }
        }
        if (!inv.get(0).isEmpty() && buffer.stored() > 0) {
            EnergyHandler item = ItemAccess.forHandlerIndex(inv.automation(), 0).getCapability(Capabilities.Energy.ITEM);
            if (item != null) {
                try (Transaction tx = Transaction.openRoot()) {
                    int moved = item.insert(Math.min(chargeRate, buffer.stored()), tx);
                    tx.commit();
                    buffer.drain(moved);
                }
            }
        }
        lastIn = pendingIn;
        lastOut = pendingOut;
        pendingIn = 0;
        pendingOut = 0;
    }

    @Override
    public void gridTransferred(int amount) {
        if (amount < 0) {
            pendingIn += -amount;
        } else {
            pendingOut += amount;
        }
    }

    @Override
    public Role gridRole() {
        return Role.STORAGE;
    }

    @Override
    public int gridPriority() {
        return priority;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return buffer;
    }

    @Override
    public int gridMaxOut() {
        return io;
    }

    @Override
    public int gridMaxIn() {
        return io;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.stored", buffer.stored(), buffer.capacity(), buffer.stored() * 100L / buffer.capacity()));
        out.add(Component.translatable("jade.autarkia.io", lastIn, lastOut));
    }

    @Override
    public float jadeProgress() {
        return buffer.fillRatio();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.ENERGY_STORAGE.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.ENERGY_STORAGE);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
        inv.serialize(out.child("items"));
        out.putDouble("leak", leakCarry);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        in.child("items").ifPresent(inv::deserialize);
        leakCarry = in.getDoubleOr("leak", 0);
    }
}

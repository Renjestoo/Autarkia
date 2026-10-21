package dev.autarkia.energy;

import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Capacitor bank (02 section 6.2): 50,000 FE, I/O 2,048 FE/t, leak 0.0001 x E per tick, storage priority group 1,
 * charges an FE item in its slot at 512 FE/t. Lightning input ({@link #absorbLightning}) has no rate limit.
 */
public class CapacitorBankBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, JadeInfo, MenuProvider {
    public static final int CAPACITY = 50000;
    private final EnergyBuffer buffer = new EnergyBuffer(CAPACITY, 2048, 2048, this::setChanged);
    private final MachineInventory inv = new MachineInventory(1, 0, s -> true, this::setChanged);
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

    public CapacitorBankBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.CAPACITOR_BANK.get(), pos, state);
    }

    /** Take lightning energy without the rate limit; returns the FE taken (capped at the free room). */
    public int absorbLightning(int fe) {
        int got = buffer.fill(fe);
        if (got > 0) {
            level.playSound(null, worldPosition, AutarkiaSounds.of("capacitor.charge"), SoundSource.BLOCKS, 0.6f, 1.2f);
            syncNow();
        }
        return got;
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
        leakCarry += 0.0001 * buffer.stored();
        int leak = (int) leakCarry;
        if (leak > 0) {
            buffer.drain(leak);
            leakCarry -= leak;
        }
        if (!inv.get(0).isEmpty() && buffer.stored() > 0) {
            EnergyHandler item = ItemAccess.forHandlerIndex(inv.automation(), 0).getCapability(Capabilities.Energy.ITEM);
            if (item != null) {
                try (Transaction tx = Transaction.openRoot()) {
                    int moved = item.insert(Math.min(512, buffer.stored()), tx);
                    tx.commit();
                    buffer.drain(moved);
                }
            }
        }
        lastIn = pendingIn;
        lastOut = pendingOut;
        pendingIn = 0;
        pendingOut = 0;
        int stripes = stripes();
        if (getBlockState().getValue(CapacitorBankBlock.CHARGE) != stripes) {
            level.setBlock(worldPosition, getBlockState().setValue(CapacitorBankBlock.CHARGE, stripes), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void gridTransferred(int amount) {
        if (amount < 0) {
            pendingIn += -amount;
        } else {
            pendingOut += amount;
        }
    }

    /** Return the charge level 0 to 4 for the four stripes. */
    public int stripes() {
        return Math.min(4, (int) Math.ceil(4.0 * buffer.stored() / CAPACITY));
    }

    @Override
    public Role gridRole() {
        return Role.STORAGE;
    }

    @Override
    public int gridPriority() {
        return 1;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return buffer;
    }

    @Override
    public int gridMaxOut() {
        return 2048;
    }

    @Override
    public int gridMaxIn() {
        return 2048;
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
        return Component.translatable("block.autarkia.capacitor_bank");
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

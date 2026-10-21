package dev.autarkia.processing;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.SlotMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The two T2 one-block forming machines of 03 section 4.1: {@code plate_press} (pressing, 1 input, 48 FE/t, 80 ticks;
 * piston and anvil sound per plate) and {@code wire_drawer} (drawing, material + optional core, 40 FE/t, 100 ticks; a
 * recipe without a core needs the core slot empty; chain sound per coil).
 */
public class PressMachineBlockEntity extends MachineBlockEntity {
    private final boolean drawer;

    private PressMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean drawer) {
        super(type, pos, state, drawer ? 2 : 1, 1, drawer ? 40 : 48);
        this.drawer = drawer;
    }

    /** Create a plate press block entity. */
    public static PressMachineBlockEntity press(BlockPos pos, BlockState state) {
        return new PressMachineBlockEntity(AutarkiaBlockEntities.PLATE_PRESS.get(), pos, state, false);
    }

    /** Create a wire drawer block entity. */
    public static PressMachineBlockEntity drawer(BlockPos pos, BlockState state) {
        return new PressMachineBlockEntity(AutarkiaBlockEntities.WIRE_DRAWER.get(), pos, state, true);
    }

    @Override
    protected MachineRecipeKind kind() {
        return drawer ? MachineRecipeKind.DRAWING : MachineRecipeKind.PRESSING;
    }

    @Override
    protected int defaultTime() {
        return drawer ? 100 : 80;
    }

    @Override
    protected int defaultEnergy() {
        return drawer ? 40 : 48;
    }

    @Override
    protected float workRate() {
        return 1;
    }

    @Override
    protected void onOperation(MachineRecipe recipe) {
        ServerLevel sl = serverLevel();
        if (drawer) {
            sl.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6f, 1.2f);
        } else {
            sl.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5f, 1f);
            sl.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.3f, 1.4f);
        }
        sl.sendParticles(ParticleTypes.CRIT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5, 4, 0.2, 0.02, 0.2, 0.05);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return drawer ? new SlotMenu(AutarkiaMenus.WIRE_DRAWER.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.WIRE_DRAWER)
            : new SlotMenu(AutarkiaMenus.PLATE_PRESS.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.PLATE_PRESS);
    }
}

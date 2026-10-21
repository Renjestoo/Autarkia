package dev.autarkia.processing;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.SlotMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crucible (03 section 4.2): alloys over a heat source directly below. Lit campfire 0.5x, lit furnace, smoker or blast
 * furnace 1.0x, lava source 1.5x, nothing NO_CONDITION. The molten metal glows while it works.
 */
public class CrucibleBlockEntity extends MachineBlockEntity {
    /** Heat source kinds shown in the GUI and Jade. */
    public enum Heat { NONE, CAMPFIRE, FURNACE, LAVA }

    private Heat heat = Heat.NONE;

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.CRUCIBLE.get(), pos, state, 2, 1, 0);
    }

    /** Return the heat source below. */
    public Heat heat() {
        return heat;
    }

    private Heat findHeat() {
        BlockState below = level.getBlockState(worldPosition.below());
        if (below.is(BlockTags.CAMPFIRES) && below.getValue(CampfireBlock.LIT)) {
            return Heat.CAMPFIRE;
        }
        if (below.getBlock() instanceof AbstractFurnaceBlock && below.getValue(AbstractFurnaceBlock.LIT)) {
            return Heat.FURNACE;
        }
        if (below.getFluidState().is(FluidTags.LAVA) && below.getFluidState().isSource()) {
            return Heat.LAVA;
        }
        return Heat.NONE;
    }

    @Override
    protected MachineRecipeKind kind() {
        return MachineRecipeKind.ALLOYING;
    }

    @Override
    protected int defaultTime() {
        return 300;
    }

    @Override
    protected int defaultEnergy() {
        return 0;
    }

    @Override
    protected float workRate() {
        heat = findHeat();
        return switch (heat) {
            case NONE -> 0;
            case CAMPFIRE -> AutarkiaConfig.CRUCIBLE_CAMPFIRE_SPEED.get().floatValue();
            case FURNACE -> AutarkiaConfig.CRUCIBLE_FURNACE_SPEED.get().floatValue();
            case LAVA -> AutarkiaConfig.CRUCIBLE_LAVA_SPEED.get().floatValue();
        };
    }

    @Override
    public void serverTick() {
        super.serverTick();
        BlockState st = getBlockState();
        if (st.getValue(CrucibleBlock.MOLTEN) != running()) {
            level.setBlock(worldPosition, st.setValue(CrucibleBlock.MOLTEN, running()), Block.UPDATE_CLIENTS);
        }
        if (running() && level.getRandom().nextInt(60) == 0) {
            level.playSound(null, worldPosition, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.4f, 1f);
        }
    }

    @Override
    public void clientTick() {
        if (getBlockState().getValue(CrucibleBlock.MOLTEN)) {
            RandomSource r = level.getRandom();
            if (r.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.SMOKE, worldPosition.getX() + 0.3 + r.nextDouble() * 0.4, worldPosition.getY() + 0.9,
                    worldPosition.getZ() + 0.3 + r.nextDouble() * 0.4, 0, 0.03, 0);
            }
            if (r.nextInt(12) == 0) {
                level.addParticle(ParticleTypes.FLAME, worldPosition.getX() + 0.35 + r.nextDouble() * 0.3, worldPosition.getY() + 0.85,
                    worldPosition.getZ() + 0.35 + r.nextDouble() * 0.3, 0, 0.01, 0);
            }
        }
    }

    @Override
    public void jadeLines(java.util.List<net.minecraft.network.chat.Component> out) {
        super.jadeLines(out);
        out.add(net.minecraft.network.chat.Component.translatable("heat.autarkia." + heat.name().toLowerCase(java.util.Locale.ROOT)));
    }

    @Override
    protected int extraData(int i) {
        return heat.ordinal();
    }

    @Override
    protected int extraDataCount() {
        return 1;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.CRUCIBLE.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.CRUCIBLE);
    }
}

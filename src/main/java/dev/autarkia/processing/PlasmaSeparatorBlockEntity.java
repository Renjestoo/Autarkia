package dev.autarkia.processing;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaRecipes;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * Plasma separator, 2x2x2 heavy T5 machine (03 section 4): {@code autarkia:plasma_separating} with input A (slot B
 * empty), else {@code autarkia:alloying} recipes that list station {@code plasma_separator} with A and B; 4,096 FE/t,
 * 100 ticks by default. Slots: A 0, B 1 (alloy inputs only, so raw ore never blocks the slot), outputs 2 to 4. Without a
 * foundation under each of the 4 bottom positions (checked every 20 ticks) the status is NO_CONDITION. A run start plays
 * the beacon power select sound, each result a soft lightning impact; sparks, end rod and soul fire particles show
 * while it runs. It counts the FE its operations drained ({@link #energyUsed()}, saved).
 */
public class PlasmaSeparatorBlockEntity extends MachineBlockEntity implements GeoBlockEntity {
    /** FE per tick while working. */
    public static final int FE_PER_TICK = 4096;
    /** Default ticks per operation. */
    public static final int TIME = 100;
    /** Slot index of input B. */
    public static final int SLOT_B = 1;
    private static final RawAnimation WORK = RawAnimation.begin().thenLoop("animation.plasma_separator.work");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.plasma_separator.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean foundation;
    private boolean surveyed;
    private Vec3 middle = Vec3.ZERO;
    private long energyUsed;

    public PlasmaSeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.PLASMA_SEPARATOR.get(), pos, state, 2, 3, FE_PER_TICK);
    }

    @Override
    protected MachineInventory createInventory(int inputs, int outputs) {
        return new MachineInventory(inputs, outputs, this::acceptsInput, this::setChanged) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return index == SLOT_B ? alloyInput(resource.toStack(1)) : super.isValid(index, resource);
            }
        };
    }

    /** Return the FE the finished and running operations drained from the buffer. */
    public long energyUsed() {
        return energyUsed;
    }

    private boolean alloyInput(ItemStack stack) {
        if (!(level instanceof ServerLevel sl)) {
            return true;
        }
        for (RecipeHolder<MachineRecipe> h : sl.recipeAccess().recipeMap().byType(AutarkiaRecipes.type(MachineRecipeKind.ALLOYING))) {
            MachineRecipe r = h.value();
            if (r.stations().contains(station()) && r.inputs().stream().anyMatch(i -> i.ingredient().test(stack))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected MachineRecipeKind kind() {
        return MachineRecipeKind.PLASMA_SEPARATING;
    }

    @Override
    protected int defaultTime() {
        return TIME;
    }

    @Override
    protected int defaultEnergy() {
        return FE_PER_TICK;
    }

    @Override
    protected boolean acceptsExtra(ServerLevel sl, ItemStack stack) {
        return alloyInput(stack);
    }

    @Override
    protected @Nullable MachineRecipe fallback(ServerLevel sl, MachineInput input) {
        for (RecipeHolder<MachineRecipe> h : sl.recipeAccess().recipeMap().byType(AutarkiaRecipes.type(MachineRecipeKind.ALLOYING))) {
            if (h.value().stations().contains(station()) && h.value().slotMatch(input) != null) {
                return h.value();
            }
        }
        return null;
    }

    private void survey() {
        Set<BlockPos> parts = ((BaseMultiblock) getBlockState().getBlock()).getMultiblockShape(level, worldPosition, getBlockState()).getGlobalPositions();
        double x = 0;
        double y = 0;
        double z = 0;
        for (BlockPos p : parts) {
            x += p.getX();
            y += p.getY();
            z += p.getZ();
        }
        int n = Math.max(1, parts.size());
        middle = new Vec3(x / n + 0.5, y / n + 0.5, z / n + 0.5);
        foundation = onFoundation();
        surveyed = true;
    }

    @Override
    protected float workRate() {
        if (level.getGameTime() % 20 == 0 || !surveyed) {
            survey();
        }
        return foundation ? 1 : 0;
    }

    @Override
    protected void onOperation(MachineRecipe recipe) {
        serverLevel().playSound(null, BlockPos.containing(middle), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.2f, 1.4f);
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        boolean was = running();
        int before = energy.stored();
        super.serverTick();
        energyUsed += Math.max(0, before - energy.stored());
        if (!running()) {
            return;
        }
        if (!was) {
            sl.playSound(null, BlockPos.containing(middle), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8f, 1.2f);
        }
        if (sl.getGameTime() % 3 == 0) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, middle.x, middle.y, middle.z, 4, 0.35, 0.35, 0.35, 0.05);
            sl.sendParticles(ParticleTypes.END_ROD, middle.x, middle.y + 0.4, middle.z, 1, 0.2, 0.2, 0.2, 0.01);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, middle.x, middle.y - 0.3, middle.z, 2, 0.3, 0.1, 0.3, 0.01);
        }
    }

    @Override
    public void jadeLines(List<Component> out) {
        super.jadeLines(out);
        if (!foundation) {
            out.add(Component.translatable("jade.autarkia.no_foundation"));
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.PLASMA_SEPARATOR.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.PLASMA_SEPARATOR);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<PlasmaSeparatorBlockEntity>("main", 4, test -> test.setAndContinue(test.animatable().running() ? WORK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putLong("energy_used", energyUsed);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        energyUsed = in.getLongOr("energy_used", 0);
    }
}

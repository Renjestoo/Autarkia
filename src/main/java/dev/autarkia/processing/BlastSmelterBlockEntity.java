package dev.autarkia.processing;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.SlotMenu;
import java.util.List;
import java.util.Optional;
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
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jspecify.annotations.Nullable;

/**
 * Blast smelter, 2x2x2 (03 section 4.2): heat 0 to 1,000; with work and FE it heats +10 per tick (64 FE/t), without
 * work or power it cools -2 per tick; it runs only at heat 1,000. It runs {@code autarkia:alloying} recipes with station
 * {@code blast_smelter} first, then vanilla smelting (one input stack, 100 ticks). A heavy machine (03 section 0): every
 * bottom position needs a {@code foundation} below, else the status is NO_CONDITION (checked every 20 ticks).
 */
public class BlastSmelterBlockEntity extends MachineBlockEntity implements GeoBlockEntity {
    public static final int MAX_HEAT = 1000;
    private static final RawAnimation WORKING = RawAnimation.begin().thenLoop("animation.blast_smelter.working");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.blast_smelter.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int heat;
    private boolean heldHeat;
    private boolean noPower;
    private boolean foundation;
    private boolean foundationKnown;

    public BlastSmelterBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.BLAST_SMELTER.get(), pos, state, 3, 2, 64);
    }

    /** Return the heat 0 to 1,000. */
    public int heat() {
        return heat;
    }

    @Override
    protected MachineRecipeKind kind() {
        return MachineRecipeKind.ALLOYING;
    }

    @Override
    protected int defaultTime() {
        return 200;
    }

    @Override
    protected int defaultEnergy() {
        return 64;
    }

    @Override
    protected float workRate() {
        if (level.getGameTime() % 20 == 0 || !foundationKnown) {
            foundation = onFoundation();
            foundationKnown = true;
        }
        int fe = (int) Math.ceil(defaultEnergy() * AutarkiaConfig.PROCESSING_ENERGY_MULTIPLIER.get());
        noPower = foundation && energy.stored() < fe;
        if (!foundation) {
            return 0;
        }
        if (heat >= MAX_HEAT) {
            heldHeat = !noPower;
            return 1;
        }
        if (!noPower) {
            energy.drain(fe);
            heat = Math.min(MAX_HEAT, heat + 10);
            heldHeat = true;
        }
        return 0;
    }

    @Override
    protected MachineStatus.State conditionState() {
        return noPower ? MachineStatus.State.NO_POWER : foundation ? MachineStatus.State.RUNNING : MachineStatus.State.NO_CONDITION;
    }

    @Override
    protected boolean acceptsExtra(ServerLevel sl, ItemStack stack) {
        return sl.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), sl).isPresent();
    }

    @Override
    protected @Nullable MachineRecipe fallback(ServerLevel sl, MachineInput input) {
        ItemStack only = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                if (!only.isEmpty()) {
                    return null;
                }
                only = input.getItem(i);
            }
        }
        if (only.isEmpty()) {
            return null;
        }
        ItemStack in = only;
        return sl.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(in), sl).map(h -> {
            ItemStack result = h.value().assemble(new SingleRecipeInput(in));
            return result.isEmpty() ? null : new MachineRecipe(MachineRecipeKind.ALLOYING, List.of(), List.of(new SizedIngredient(Ingredient.of(in.getItem()), 1)),
                List.of(new MachineRecipe.Result(ItemStackTemplate.fromNonEmptyStack(result), 1f)), Optional.of(100), Optional.empty(), 0, 0);
        }).orElse(null);
    }

    @Override
    public void serverTick() {
        heldHeat = false;
        super.serverTick();
        if (!heldHeat) {
            heat = Math.max(0, heat - 2);
        }
        ServerLevel sl = serverLevel();
        long now = sl.getGameTime();
        if (heat > 0 && now % 30 == 0) {
            sl.playSound(null, worldPosition, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.7f, 1f);
        }
        if (heat > 0 && now % 6 == 0) {
            double x = worldPosition.getX() + 1;
            double z = worldPosition.getZ() + 1;
            sl.sendParticles(ParticleTypes.SMOKE, x, worldPosition.getY() + 2.1, z, 2, 0.3, 0.05, 0.3, 0.01);
            sl.sendParticles(heat >= MAX_HEAT ? ParticleTypes.LAVA : ParticleTypes.FLAME, x, worldPosition.getY() + 0.6, z, 1, 0.4, 0.1, 0.4, 0);
        }
        if (now % 20 == 0) {
            syncNow();
        }
    }

    @Override
    public void jadeLines(List<Component> out) {
        super.jadeLines(out);
        out.add(Component.translatable("jade.autarkia.smelter_heat", heat, MAX_HEAT));
        if (!foundation) {
            out.add(Component.translatable("jade.autarkia.no_foundation"));
        }
    }

    @Override
    protected int extraData(int i) {
        return heat;
    }

    @Override
    protected int extraDataCount() {
        return 1;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.BLAST_SMELTER.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.BLAST_SMELTER);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BlastSmelterBlockEntity>("main", 4, test -> test.setAndContinue(test.animatable().running() ? WORKING : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("heat", heat);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        heat = in.getIntOr("heat", 0);
    }
}

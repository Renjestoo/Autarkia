package dev.autarkia.processing;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaRecipes;
import dev.autarkia.registry.Ticking;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Shared processing machine (03 section 4): input and output slots, optional FE buffer (FE/t x 200, input FE/t x 4),
 * recipe search by kind and station, chance results, status and progress. Subclasses decide when work may run.
 */
public abstract class MachineBlockEntity extends AutarkiaBlockEntity implements Ticking, MenuProvider, MachineStatus.View, GridMember,
    dev.autarkia.registry.JadeInfo {
    protected final MachineInventory inv;
    protected final @Nullable EnergyBuffer energy;
    protected float progress;
    protected int duration;
    protected MachineStatus.State state = MachineStatus.State.IDLE_NO_INPUT;
    private int workDraw;
    protected int idleTicks;
    private boolean running;
    private @Nullable RecipeHolder<MachineRecipe> last;
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> Math.round(progress);
                case 1 -> duration;
                case 2 -> energy == null ? 0 : energy.stored();
                case 3 -> energy == null ? 0 : energy.capacity();
                case 4 -> state.ordinal();
                default -> extraData(i - 5);
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 5 + extraDataCount();
        }
    };

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inputs, int outputs, int feTick) {
        super(type, pos, state);
        this.inv = new MachineInventory(inputs, outputs, this::acceptsInput, this::setChanged);
        this.energy = feTick > 0 ? new EnergyBuffer(feTick * 200, feTick * 4, 0, this::setChanged) : null;
    }

    /** Return the station id (block id path) that recipes list. */
    protected String station() {
        return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath();
    }

    /** Return the recipe kind this machine runs. */
    protected abstract MachineRecipeKind kind();

    /** Return the machine default time per operation in ticks. */
    protected abstract int defaultTime();

    /** Return the machine default FE per tick while working (0 for unpowered machines). */
    protected abstract int defaultEnergy();

    /** Return the work this tick in progress ticks (speed multipliers, heat, hand turning); 0 means the condition is missing. */
    protected abstract float workRate();

    /** Called after an operation finished (sounds, particles, animation triggers). */
    protected void onOperation(MachineRecipe recipe) {
    }

    /** Return extra menu data value i. */
    protected int extraData(int i) {
        return 0;
    }

    /** Return the number of extra menu data values. */
    protected int extraDataCount() {
        return 0;
    }

    /** Return the menu data (progress, duration, energy, capacity, state, extras). */
    public ContainerData data() {
        return data;
    }

    /** Return the inventory. */
    public MachineInventory inventory() {
        return inv;
    }

    /** Return the energy buffer, or null for unpowered machines. */
    public @Nullable EnergyBuffer energy() {
        return energy;
    }

    /** Return true while an operation runs (client: from the update tag). */
    public boolean running() {
        return running;
    }

    private boolean acceptsInput(ItemStack stack) {
        if (!(level instanceof ServerLevel sl)) {
            return true;
        }
        for (RecipeHolder<MachineRecipe> h : sl.recipeAccess().recipeMap().byType(AutarkiaRecipes.type(kind()))) {
            MachineRecipe r = h.value();
            if (r.allows(station()) && r.inputs().stream().anyMatch(i -> i.ingredient().test(stack))) {
                return true;
            }
        }
        return false;
    }

    private @Nullable RecipeHolder<MachineRecipe> findRecipe(ServerLevel sl, MachineInput input) {
        if (last != null && last.value().allows(station()) && last.value().slotMatch(input) != null) {
            return last;
        }
        for (RecipeHolder<MachineRecipe> h : sl.recipeAccess().recipeMap().byType(AutarkiaRecipes.type(kind()))) {
            if (h.value().allows(station()) && h.value().slotMatch(input) != null) {
                last = h;
                return h;
            }
        }
        return null;
    }

    /** Return the recipe input of the input slots. */
    protected MachineInput input() {
        ItemStack[] items = new ItemStack[inv.inputs()];
        for (int i = 0; i < items.length; i++) {
            items[i] = inv.get(i);
        }
        return new MachineInput(List.of(items));
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        idleTicks++;
        MachineInput input = input();
        RecipeHolder<MachineRecipe> holder = input.isEmpty() ? null : findRecipe(sl, input);
        MachineStatus.State next;
        workDraw = 0;
        if (holder == null) {
            next = MachineStatus.State.IDLE_NO_INPUT;
            progress = 0;
        } else {
            MachineRecipe r = holder.value();
            double speed = AutarkiaConfig.PROCESSING_SPEED_MULTIPLIER.get();
            duration = Math.max(1, (int) Math.round(r.timeOr(defaultTime()) / speed));
            int fe = (int) Math.ceil(r.energyOr(defaultEnergy()) * AutarkiaConfig.PROCESSING_ENERGY_MULTIPLIER.get());
            float rate = workRate();
            if (!inv.fitsOutputs(r.allResults())) {
                next = MachineStatus.State.BLOCKED_OUTPUT_FULL;
            } else if (rate <= 0) {
                next = conditionState();
            } else if (energy != null && fe > 0 && energy.stored() < fe) {
                workDraw = fe;
                next = MachineStatus.State.NO_POWER;
            } else {
                workDraw = energy == null ? 0 : fe;
                if (energy != null && fe > 0) {
                    energy.drain(fe);
                }
                next = MachineStatus.State.RUNNING;
                progress += rate;
                if (progress >= duration) {
                    finish(r, input);
                }
            }
        }
        state = next;
        boolean nowRunning = next == MachineStatus.State.RUNNING;
        if (nowRunning != running) {
            running = nowRunning;
            syncNow();
        }
    }

    /** Return the state shown when {@link #workRate()} is 0 (NO_CONDITION by default; manual machines show idle). */
    protected MachineStatus.State conditionState() {
        return MachineStatus.State.NO_CONDITION;
    }

    private void finish(MachineRecipe r, MachineInput input) {
        int[] slots = r.slotMatch(input);
        if (slots == null) {
            return;
        }
        for (int k = 0; k < slots.length; k++) {
            inv.shrink(slots[k], r.inputs().get(k).count());
        }
        inv.addOutputs(r.roll(level.getRandom()));
        progress = 0;
        idleTicks = 0;
        onOperation(r);
    }

    @Override
    public String jadeGroup() {
        return "machine";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable(state.key()).withColor(state.color));
        if (energy != null) {
            out.add(Component.translatable("jade.autarkia.energy", energy.stored(), energy.capacity()));
        }
        out.add(Component.translatable("jade.autarkia.queue", inv.queue()));
    }

    @Override
    public float jadeProgress() {
        return duration == 0 ? 0 : progress / duration;
    }

    @Override
    public MachineStatus machineStatus() {
        return new MachineStatus(state, inv.queue(), duration == 0 ? 0 : progress / duration, idleTicks,
            energy == null ? 0 : energy.stored(), energy == null ? 0 : energy.capacity());
    }

    @Override
    public Role gridRole() {
        return Role.CONSUMER;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return energy;
    }

    @Override
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int gridMaxIn() {
        return energy == null ? 0 : energy.capacity() / 50;
    }

    @Override
    public int workDraw() {
        return workDraw;
    }

    @Override
    public boolean gridValid() {
        return energy != null && !isRemoved();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel && isCenter()) {
            for (int i = 0; i < inv.size(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inv.get(i).copy());
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inv.serialize(out.child("inv"));
        if (energy != null) {
            energy.serialize(out.child("energy"));
        }
        out.putFloat("progress", progress);
        out.putInt("duration", duration);
        out.putBoolean("running", running);
        out.putInt("idle", idleTicks);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("inv").ifPresent(inv::deserialize);
        if (energy != null) {
            in.child("energy").ifPresent(energy::deserialize);
        }
        progress = in.getFloatOr("progress", 0);
        duration = in.getIntOr("duration", 0);
        running = in.getBooleanOr("running", false);
        idleTicks = in.getIntOr("idle", 0);
    }
}

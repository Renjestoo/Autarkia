package dev.autarkia.colony;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.processing.MachineInput;
import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import dev.autarkia.worker.WorkerAssemblyRecipe;
import dev.autarkia.worker.WorkerEntity;
import dev.autarkia.worker.WorkerType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The {@code replicator_foundry} (05 section 8.2, 06 section 5.1): a 3x3x3 workshop with the frame slot and six part
 * slots of the Replicator recipe ({@code worker_assembly} with machine {@code replicator_foundry}); slot 0 takes only
 * walker frames, and each part kind keeps to one part slot, so deliveries fill the slots the recipe reads. It prints only on a
 * colony order ({@link Replication#order}): with all parts in, it takes the kit matter units and reserves the target
 * deposit ({@link Replication#start}), then runs 2,400 ticks at 2,000 FE/t (GeckoLib {@code print}, sound
 * {@code replicator_print}); progress pauses without power. At the end it checks the caps again, consumes the bill, and
 * the walker steps out of the front with a full battery, plays {@code unfold} ({@code replicator_unfold}) and goes to
 * its site. The kill switch {@code replicator.enabled} stops new prints.
 */
public class ReplicatorFoundryBlockEntity extends AutarkiaBlockEntity implements Ticking, GeoBlockEntity, MenuProvider, GridMember, JadeInfo {
    /** Foundry states for the GUI and Jade. */
    public enum Status { IDLE, NO_PARTS, WAIT_MATTER, RUNNING, NO_POWER, CAP_FULL, TIER_LOW, OUTSIDE, DISABLED }

    public static final int SLOTS = 7;
    public static final int CAPACITY = 200000;
    public static final int MAX_IN = 8000;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.replicator_foundry.idle");
    private static final RawAnimation PRINT = RawAnimation.begin().thenLoop("animation.replicator_foundry.print");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final MachineInventory inv = new MachineInventory(SLOTS, 0, s -> true, this::setChanged) {
        @Override
        public boolean isValid(int index, net.neoforged.neoforge.transfer.item.ItemResource resource) {
            boolean frame = resource.is(dev.autarkia.registry.AutarkiaItems.WALKER_FRAME.get());
            if (index == 0 || frame) {
                return index == 0 && frame;
            }
            ItemStack here = get(index);
            if (!here.isEmpty()) {
                return resource.matches(here);
            }
            for (int s = 1; s < SLOTS; s++) {
                if (s != index && resource.matches(get(s))) {
                    return false;
                }
            }
            return index < SLOTS;
        }
    };
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_IN, 0, this::setChanged);
    private int progress;
    private int duration;
    private int drawPerTick;
    private boolean ordered;
    private @Nullable UUID walker;
    private Status status = Status.IDLE;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> duration;
                case 2 -> energy.stored();
                case 3 -> energy.capacity();
                case 4 -> status.ordinal();
                case 5 -> walkers();
                default -> AutarkiaConfig.REPLICATOR_MAX_PER_DOMAIN.get();
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public ReplicatorFoundryBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.REPLICATOR_FOUNDRY.get(), pos, state);
    }

    /** Return the inventory (frame slot 0, parts 1 to 6). */
    public MachineInventory inventory() {
        return inv;
    }

    /** Return the status. */
    public Status status() {
        return status;
    }

    /** Return the energy buffer. */
    public EnergyBuffer energy() {
        return energy;
    }

    /** Return the print progress in ticks. */
    public int progress() {
        return progress;
    }

    /** Return whether a print is ordered or running. */
    public boolean ordered() {
        return ordered;
    }

    /** Return whether the foundry has power for at least one print tick (or a print is running). */
    public boolean powered() {
        return energy.stored() >= 2000 || status == Status.RUNNING;
    }

    /** Mark a print ordered by the colony. */
    public void order() {
        ordered = true;
        setChanged();
    }

    /** Return the count of an item in the foundry slots. */
    public int held(@Nullable Item item) {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = inv.get(i);
            if (item != null && s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private int walkers() {
        Colony c = level == null ? null : Colonies.at(level, worldPosition);
        return c == null ? 0 : c.replicatorsActive.size();
    }

    private Optional<WorkerAssemblyRecipe> recipe(ServerLevel level) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            items.add(inv.get(i));
        }
        MachineInput input = new MachineInput(items);
        return Replication.recipe(level).filter(r -> r.matches(input, level));
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        Status before = status;
        status = step(sl);
        if (status != before || progress % 20 == 0) {
            syncNow();
        }
    }

    private Status step(ServerLevel sl) {
        if (!ordered) {
            return Status.IDLE;
        }
        if (!AutarkiaConfig.REPLICATOR_ENABLED.get() && progress == 0) {
            return Status.DISABLED;
        }
        Colony c = Colonies.at(sl, worldPosition);
        if (c == null) {
            return Status.OUTSIDE;
        }
        Optional<WorkerAssemblyRecipe> found = recipe(sl);
        if (found.isEmpty()) {
            return Status.NO_PARTS;
        }
        WorkerAssemblyRecipe r = found.get();
        duration = r.time();
        drawPerTick = r.fePerTick();
        if (c.tier < r.minTier() || c.tier < WorkerType.REPLICATOR.tier()) {
            return Status.TIER_LOW;
        }
        if (progress == 0 && (!capRoom(c) || !Replication.room(c))) {
            return Status.CAP_FULL;
        }
        if (walker == null) {
            UUID id = UUID.randomUUID();
            if (!Replication.start(c, sl, id)) {
                return Status.WAIT_MATTER;
            }
            walker = id;
            sl.playSound(null, worldPosition, AutarkiaSounds.of("replicator_print"), SoundSource.BLOCKS, 0.8f, 0.7f);
            setChanged();
        }
        if (progress >= duration) {
            if (!capRoom(c)) {
                return Status.CAP_FULL;
            }
            finish(sl, c, r);
            return Status.IDLE;
        }
        if (energy.stored() < drawPerTick) {
            return Status.NO_POWER;
        }
        energy.drain(drawPerTick);
        progress++;
        if (progress % 40 == 0) {
            Vec3 top = Vec3.atBottomCenterOf(worldPosition).add(0, 2.6, 0);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, top.x, top.y, top.z, 10, 0.8, 0.3, 0.8, 0.05);
        }
        return Status.RUNNING;
    }

    private static boolean capRoom(Colony c) {
        WorkerType t = WorkerType.REPLICATOR;
        return c.workerCost() + t.capCost() <= ColonyTiers.workerCap(c.tier) && c.workerCount(t.id()) < t.subCap(c.tier);
    }

    private void finish(ServerLevel sl, Colony c, WorkerAssemblyRecipe r) {
        if (!(AutarkiaEntities.REPLICATOR_WALKER.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof WorkerEntity w)) {
            throw new IllegalStateException("replicator_walker is not a worker entity");
        }
        r.frame().ifPresent(f -> inv.shrink(0, f.count()));
        for (var part : r.parts()) {
            for (int s = 1; s < SLOTS; s++) {
                ItemStack st = inv.get(s);
                if (!st.isEmpty() && part.ingredient().test(st) && st.getCount() >= part.count()) {
                    inv.shrink(s, part.count());
                    break;
                }
            }
        }
        Direction front = MachineBlock.front(getBlockState());
        Vec3 spawn = Vec3.atBottomCenterOf(worldPosition).add(front.getStepX() * 3.2, 0, front.getStepZ() * 3.2);
        w.setUUID(walker);
        w.snapTo(spawn.x, spawn.y, spawn.z, front.toYRot(), 0);
        w.setEnergy(w.workerType.capacity());
        sl.addFreshEntity(w);
        w.bind(c);
        w.trigger("unfold");
        sl.playSound(null, worldPosition, AutarkiaSounds.of("replicator_unfold"), SoundSource.BLOCKS, 0.9f, 0.6f);
        ColonyEvents.worker(c, "assembled:replicator_walker");
        Replication.printed(c, sl, w);
        Autarkia.LOG.info("[worker] printed replicator_walker {} at {}", walker.toString().substring(0, 8), worldPosition.toShortString());
        progress = 0;
        walker = null;
        ordered = false;
        setChanged();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (isCenter() && level != null) {
            for (int i = 0; i < inv.size(); i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inv.get(i).copy());
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.autarkia.replicator_foundry");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.REPLICATOR_FOUNDRY.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.REPLICATOR_FOUNDRY);
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
        return energy.room() > 0 ? MAX_IN : 0;
    }

    @Override
    public int workDraw() {
        return status == Status.RUNNING || status == Status.NO_POWER ? drawPerTick : 0;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "colony";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.replicator.walkers", walkers(), AutarkiaConfig.REPLICATOR_MAX_PER_DOMAIN.get()));
        out.add(Component.translatable("jade.autarkia.replicator.print", duration == 0 ? 0 : progress * 100 / duration));
        out.add(Component.translatable("jade.autarkia.replicator." + status.name().toLowerCase(java.util.Locale.ROOT)));
        out.add(Component.translatable("jade.autarkia.buffer", energy.stored(), energy.capacity()));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ReplicatorFoundryBlockEntity>("main", 4,
            test -> test.setAndContinue(test.animatable().status == Status.RUNNING ? PRINT : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inv.serialize(out.child("items"));
        energy.serialize(out.child("energy"));
        out.putInt("progress", progress);
        out.putInt("duration", duration);
        out.putInt("status", status.ordinal());
        out.putBoolean("ordered", ordered);
        if (walker != null) {
            out.store("walker", UUIDUtil.CODEC, walker);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("items").ifPresent(inv::deserialize);
        in.child("energy").ifPresent(energy::deserialize);
        progress = in.getIntOr("progress", 0);
        duration = in.getIntOr("duration", 0);
        status = Status.values()[Math.max(0, Math.min(Status.values().length - 1, in.getIntOr("status", 0)))];
        ordered = in.getBooleanOr("ordered", false);
        walker = in.read("walker", UUIDUtil.CODEC).orElse(null);
    }
}

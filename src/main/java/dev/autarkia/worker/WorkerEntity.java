package dev.autarkia.worker;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.ColonyTiers;
import dev.autarkia.colony.ColonyWorker;
import dev.autarkia.registry.AutarkiaSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.tslat.smartbrainlib.api.core.navigation.SmoothGroundNavigation;
import org.jspecify.annotations.Nullable;

/**
 * A colony worker (06): GeckoLib rig, SmartBrainLib brain ({@link WorkerBrain}), spring energy with drains every 20
 * ticks, one work order at a time pushed by the colony job board, cargo, home position, stuck levels and the domain
 * registry heartbeat. Workers never teleport; a worker outside a domain stays idle and unlinked.
 */
public class WorkerEntity extends PathfinderMob implements WorkerBrain.Owner, GeoEntity, ColonyWorker {
    /** Worker states of 06 section 4.1 ({@code DATA_STATE}). */
    public enum State { IDLE, ASSIGNED, FETCHING, TRAVELLING, WORKING, RETURNING, RECHARGING, FAILED }

    /** Loop actions of {@code DATA_ACTION}. */
    public static final byte ACTION_NONE = 0;
    public static final byte ACTION_WORK = 1;
    public static final byte ACTION_CRANK = 2;
    public static final byte ACTION_RECHARGE = 4;

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_CARRYING = SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RUNDOWN = SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> DATA_CARGO = SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.ITEM_STACK);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public final WorkerType workerType;
    public final List<ItemStack> cargo = new ArrayList<>();
    private int energy;
    private @Nullable UUID colonyId;
    private @Nullable BlockPos home;
    private @Nullable WorkOrder order;
    private int orderIndex;
    private String failReason = "";
    private long failedAt;
    private final List<BlockPos> stuckTrail = new ArrayList<>();
    private int stuckLevel;
    private long stuckSince = -1;
    private final java.util.Map<Long, Long> unreachable = new java.util.HashMap<>();
    private int drainCarry;

    public WorkerEntity(EntityType<? extends WorkerEntity> type, Level level) {
        super(type, level);
        this.workerType = WorkerType.of(type);
        for (int i = 0; i < workerType.cargoStacks(); i++) {
            cargo.add(ItemStack.EMPTY);
        }
        this.energy = workerType.capacity();
        setPersistenceRequired();
    }

    /** Return the attribute set of a worker type (06 section 1.1). */
    public static AttributeSupplier.Builder attributes(WorkerType t) {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, t.health()).add(Attributes.ARMOR, t.armor())
            .add(Attributes.MOVEMENT_SPEED, t.speed()).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        SmoothGroundNavigation nav = new SmoothGroundNavigation(this, level);
        nav.setCanFloat(true);
        nav.setCanOpenDoors(true);
        return nav;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DATA_STATE, (byte) 0);
        b.define(DATA_ACTION, ACTION_NONE);
        b.define(DATA_CARRYING, false);
        b.define(DATA_RUNDOWN, false);
        b.define(DATA_CARGO, ItemStack.EMPTY);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Return the synced state. */
    public State state() {
        return State.values()[entityData.get(DATA_STATE)];
    }

    /** Set the synced state. */
    public void setState(State s) {
        entityData.set(DATA_STATE, (byte) s.ordinal());
    }

    /** Return the state name for screens and Jade. */
    public String stateName() {
        return state() == State.FAILED && !failReason.isEmpty() ? "failed:" + failReason : state().name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Return the current job type name, or empty. */
    public String jobName() {
        ColonyJob j = currentJob();
        return j == null ? "" : j.type.name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Set the loop action. */
    public void setAction(byte action) {
        entityData.set(DATA_ACTION, action);
    }

    /** Return the loop action. */
    public byte action() {
        return entityData.get(DATA_ACTION);
    }

    /** Return the spring tension in T. */
    public int energy() {
        return energy;
    }

    /** Set the tension, clamped to the capacity; reaching 0 runs the worker down. */
    public void setEnergy(int value) {
        energy = Math.max(0, Math.min(workerType.capacity(), value));
        boolean down = energy == 0;
        if (down && !entityData.get(DATA_RUNDOWN)) {
            fail("no_energy");
            playSound(AutarkiaSounds.of("entity.worker.rundown"), 0.6f, 0.6f);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 12, 0.2, 0.3, 0.2, 0.02);
            }
        }
        entityData.set(DATA_RUNDOWN, down);
    }

    /** Spend tension for an action. */
    public void spend(int t) {
        setEnergy(energy - t);
    }

    @Override
    public float charge() {
        return (float) energy / workerType.capacity();
    }

    /** Return whether the spring is run down (client animation). */
    public boolean runDown() {
        return entityData.get(DATA_RUNDOWN);
    }

    /** Return the return reserve: path to the nearest free charger x drain per block x 1.5 + 200 (06 section 1.2). */
    public int reserve() {
        BlockPos ch = getBrain().getMemory(WorkerMemories.CHARGER_POS.get()).map(g -> g.pos()).orElse(null);
        BlockPos to = ch != null ? ch : home();
        return (int) (blockPosition().distManhattan(to) * workerType.drainPerBlock() * 1.5) + 200;
    }

    /** Return the linked colony, or null when unlinked. */
    public @Nullable Colony colony() {
        return colonyId == null || Colonies.server() == null ? null : Colonies.registry().get(colonyId);
    }

    /** Return the home position (the charger, else near the Core, else here). */
    public BlockPos home() {
        if (home != null) {
            return home;
        }
        Colony c = colony();
        return c != null ? c.corePos.south(3) : blockPosition();
    }

    /** Send the worker home: drop its order and walk back (Workers tab Recall / Move home). */
    public void recall(BlockPos core) {
        home = core.south(3);
        releaseOrder();
        WorkerBrain.walkTo(this, home, 2);
    }

    /** Return the failure reason, or empty. */
    public String failReason() {
        return failReason;
    }

    /** Put the worker into the failed state with a reason; it releases its order. */
    public void fail(String reason) {
        failReason = reason;
        failedAt = level().getGameTime();
        releaseOrder();
        setState(State.FAILED);
        setAction(ACTION_NONE);
    }

    /** Clear a failed state (retry timer, player fix, recharge). */
    public void recover() {
        if (state() == State.FAILED) {
            failReason = "";
            stuckLevel = 0;
            getBrain().eraseMemory(WorkerMemories.STUCK_LEVEL.get());
            setState(State.IDLE);
        }
    }

    /** Return whether all cargo slots are empty. */
    public boolean cargoEmpty() {
        return cargo.stream().allMatch(ItemStack::isEmpty);
    }

    /** Return the held count of items matching the predicate. */
    public int held(java.util.function.Predicate<ItemStack> filter) {
        return cargo.stream().filter(s -> !s.isEmpty() && filter.test(s)).mapToInt(ItemStack::getCount).sum();
    }

    /** Add a stack to the cargo; returns the part that did not fit. */
    public ItemStack addCargo(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = 0; i < cargo.size() && !rest.isEmpty(); i++) {
            ItemStack s = cargo.get(i);
            if (s.isEmpty()) {
                cargo.set(i, rest.copy());
                rest = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(s, rest)) {
                int move = Math.min(rest.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(move);
                rest.shrink(move);
            }
        }
        syncCargo();
        return rest;
    }

    /** Take one item matching the predicate from the cargo; returns it or empty. */
    public ItemStack takeCargo(java.util.function.Predicate<ItemStack> filter, int count) {
        for (ItemStack s : cargo) {
            if (!s.isEmpty() && filter.test(s) && s.getCount() >= count) {
                ItemStack out = s.split(count);
                syncCargo();
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Push the cargo look to clients. */
    public void syncCargo() {
        ItemStack first = cargo.stream().filter(s -> !s.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
        entityData.set(DATA_CARGO, first.copyWithCount(1));
        entityData.set(DATA_CARRYING, !first.isEmpty());
    }

    /** Return the first cargo stack for the renderer. */
    public ItemStack cargoLook() {
        return entityData.get(DATA_CARGO);
    }

    /** Return whether the worker carries something (pose animation). */
    public boolean carrying() {
        return entityData.get(DATA_CARRYING);
    }

    @Override
    public UUID workerId() {
        return getUUID();
    }

    @Override
    public boolean accepts(ColonyJob job) {
        return WorkerActions.accepts(this, job);
    }

    @Override
    public BlockPos workPos() {
        ColonyJob j = currentJob();
        return j != null ? j.target : home();
    }

    @Override
    public boolean isIdle() {
        return order == null && state() == State.IDLE && !getBrain().hasMemoryValue(WorkerMemories.NEEDS_RECHARGE.get()) && isAlive();
    }

    @Override
    public int cargoStacks() {
        return workerType.cargoStacks();
    }

    @Override
    public void assign(WorkOrder o) {
        order = o;
        orderIndex = 0;
        getBrain().setMemory(WorkerMemories.ASSIGNED_JOB.get(), o.jobs().get(0).id);
        getBrain().eraseMemory(WorkerMemories.JOB_PHASE.get());
        setState(State.ASSIGNED);
    }

    @Override
    public void abort(ColonyJob.Failure reason) {
        clearOrder();
    }

    /** Return the job of the order now being executed, or null. */
    public @Nullable ColonyJob currentJob() {
        return order == null || orderIndex >= order.jobs().size() ? null : order.jobs().get(orderIndex);
    }

    /** Return the 1-based step of the order now running and the step count, or {0, 0} without an order. */
    public int[] orderProgress() {
        return order == null ? new int[] {0, 0} : new int[] {Math.min(orderIndex + 1, order.jobs().size()), order.jobs().size()};
    }

    /** Return the jobs left in the order from the current one (for fetch counts). */
    public List<ColonyJob> remainingJobs() {
        return order == null ? List.of() : order.jobs().subList(Math.min(orderIndex, order.jobs().size()), order.jobs().size());
    }

    /** Drop the order without failing it (energy, repair, capture): the board gets it back (06 section 4.2). */
    public void releaseOrder() {
        Colony c = colony();
        if (order != null && c != null && level() instanceof ServerLevel sl) {
            for (int i = orderIndex; i < order.jobs().size(); i++) {
                ColonyRuntime.of(c).jobs.failed(c, sl, order.jobs().get(i).id, ColonyJob.Failure.WORKER_LOST);
            }
        }
        clearOrder();
    }

    private void clearOrder() {
        order = null;
        orderIndex = 0;
        getBrain().eraseMemory(WorkerMemories.ASSIGNED_JOB.get());
        getBrain().eraseMemory(WorkerMemories.JOB_PHASE.get());
        getBrain().eraseMemory(WorkerMemories.FETCH_SOURCE.get());
        getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
        setAction(ACTION_NONE);
        if (state() != State.FAILED && state() != State.RECHARGING) {
            setState(cargoEmpty() ? State.IDLE : State.RETURNING);
        }
    }

    /** Report the current job done and move to the next one (or finish the order). */
    public void jobDone() {
        ColonyJob j = currentJob();
        Colony c = colony();
        if (j != null && c != null && level() instanceof ServerLevel sl) {
            ColonyRuntime.of(c).jobs.done(c, sl, j.id);
        }
        orderIndex++;
        getBrain().eraseMemory(WorkerMemories.JOB_PHASE.get());
        if (currentJob() == null) {
            Colony col = colony();
            order = null;
            getBrain().eraseMemory(WorkerMemories.ASSIGNED_JOB.get());
            setAction(ACTION_NONE);
            setState(State.RETURNING);
            if (col != null) {
                Colonies.changed();
            }
        } else {
            getBrain().setMemory(WorkerMemories.ASSIGNED_JOB.get(), currentJob().id);
        }
    }

    /** Report the current job and the rest of the order failed with a reason. */
    public void jobFailed(ColonyJob.Failure reason) {
        Colony c = colony();
        if (order != null && c != null && level() instanceof ServerLevel sl) {
            ColonyRuntime.of(c).jobs.failed(c, sl, order.jobs().get(orderIndex).id, reason);
            for (int i = orderIndex + 1; i < order.jobs().size(); i++) {
                ColonyRuntime.of(c).jobs.failed(c, sl, order.jobs().get(i).id, ColonyJob.Failure.WORKER_LOST);
            }
        }
        clearOrder();
        if (reason == ColonyJob.Failure.NO_PATH) {
            setState(State.RETURNING);
        }
    }

    /** Renew the lease of the current job. */
    public void progress() {
        ColonyJob j = currentJob();
        Colony c = colony();
        if (j != null && c != null) {
            ColonyRuntime.of(c).jobs.progress(j.id, level().getGameTime());
        }
    }

    /** Drop the order when the board no longer knows the current job (cancel, project removed). */
    public void validateOrder() {
        ColonyJob j = currentJob();
        Colony c = colony();
        if (j != null && (c == null || ColonyRuntime.of(c).jobs.get(j.id) == null)) {
            clearOrder();
        }
    }

    /** Sample the position for stuck detection (06 section 3.8). */
    public void stuckSample() {
        boolean wantsMove = getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET)
            && action() == ACTION_NONE && state() != State.RECHARGING;
        stuckTrail.add(blockPosition());
        while (stuckTrail.size() > 6) {
            stuckTrail.remove(0);
        }
        if (!wantsMove || stuckTrail.size() < 6 || stuckTrail.get(0).distSqr(blockPosition()) >= 0.25) {
            stuckSince = wantsMove ? stuckSince : -1;
            if (!wantsMove || stuckTrail.size() >= 6 && stuckTrail.get(0).distSqr(blockPosition()) >= 0.25) {
                stuckLevel = 0;
                getBrain().eraseMemory(WorkerMemories.STUCK_LEVEL.get());
            }
            return;
        }
        long now = level().getGameTime();
        if (stuckSince < 0) {
            stuckSince = now;
        }
        int level = (int) Math.min(4, 1 + (now - stuckSince) / 100);
        if (level != stuckLevel) {
            stuckLevel = level;
            getBrain().setMemory(WorkerMemories.STUCK_LEVEL.get(), level);
        }
    }

    /** Remember an unreachable target for 600 ticks (06 section 6.2). */
    public void markUnreachable(BlockPos p) {
        unreachable.put(p.asLong(), level().getGameTime() + 600);
    }

    /** Return whether a target is in the unreachable cache. */
    public boolean unreachable(BlockPos p) {
        Long until = unreachable.get(p.asLong());
        return until != null && until > level().getGameTime();
    }

    /** Reset the stuck trail (after a recovery action). */
    public void resetStuck() {
        stuckTrail.clear();
        stuckSince = -1;
        stuckLevel = 0;
        getBrain().eraseMemory(WorkerMemories.STUCK_LEVEL.get());
    }

    @Override
    public void aiStep() {
        long t0 = dev.autarkia.command.ColonyProfiler.active() ? System.nanoTime() : 0;
        super.aiStep();
        if (t0 != 0) {
            dev.autarkia.command.ColonyProfiler.add("worker." + workerType.id().getPath(), System.nanoTime() - t0);
        }
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        long now = sl.getGameTime();
        if (now % 20 == 0) {
            bindOrHeartbeat(sl);
            if (energy > 0) {
                int drain = state() == State.RECHARGING && action() == ACTION_RECHARGE ? 0 : workerType.idleDrain();
                if (getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
                    drain += workerType.moveDrain() + (carrying() ? workerType.carryDrain() : 0);
                }
                spend(drain);
            }
            if (state() == State.FAILED && energy > 0 && now - failedAt >= 1200) {
                recover();
            }
        }
    }

    private void bindOrHeartbeat(ServerLevel sl) {
        Colony c = colony();
        if (c == null) {
            Colony here = Colonies.at(sl, blockPosition());
            if (here != null && here.workerCost() + workerType.capCost() <= ColonyTiers.workerCap(here.tier)
                && here.workerCount(workerType.id()) < workerType.subCap(here.tier)) {
                colonyId = here.id;
                here.workers.put(getUUID(), new Colony.WorkerEntry(getUUID(), workerType.id(), workerType.capCost(), chunkPosition().pack(), sl.getGameTime()));
                Colonies.changed();
            }
            return;
        }
        Colony.WorkerEntry e = c.workers.get(getUUID());
        if (e == null) {
            if (c.workerCost() + workerType.capCost() > ColonyTiers.workerCap(c.tier)) {
                colonyId = null;
                return;
            }
        }
        c.workers.put(getUUID(), new Colony.WorkerEntry(getUUID(), workerType.id(), workerType.capCost(), chunkPosition().pack(), sl.getGameTime()));
    }

    /** Link a freshly placed or assembled worker to a colony (registry entry now). */
    public void bind(Colony c) {
        colonyId = c.id;
        c.workers.put(getUUID(), new Colony.WorkerEntry(getUUID(), workerType.id(), workerType.capCost(), chunkPosition().pack(), level().getGameTime()));
        Colonies.changed();
    }

    /** Leave the colony registry (death, capture, discard). */
    public void unbind() {
        releaseOrder();
        Colony c = colony();
        if (c != null) {
            c.workers.remove(getUUID());
            Colonies.changed();
        }
        colonyId = null;
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel) {
            unbind();
        }
        super.die(source);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        for (ItemStack s : cargo) {
            if (!s.isEmpty()) {
                spawnAtLocation(level, s.copy());
            }
        }
        cargo.replaceAll(s -> ItemStack.EMPTY);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && !dev.autarkia.config.AutarkiaConfig.WORKERS_DAMAGEABLE_BY_NON_MEMBERS.get()) {
            Colony c = colony();
            if (c != null && !c.canManage(p.getUUID())) {
                return false;
            }
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (stack.isEmpty() && energy < workerType.capacity()) {
            setEnergy(energy + 100);
            recover();
            playSound(AutarkiaSounds.of("entity.worker.wind"), 0.5f, 1.8f);
            return InteractionResult.SUCCESS;
        }
        if (stack.is(workerType.repairItem().get()) && getHealth() < getMaxHealth()) {
            heal(workerType.repairHealth());
            stack.consume(1, player);
            playSound(AutarkiaSounds.of("entity.worker.servo"), 0.4f, 1.9f);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AutarkiaSounds.of("entity.worker.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AutarkiaSounds.of("entity.worker.death");
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(AutarkiaSounds.of("entity.worker.step_" + workerType.rig()), 0.4f, 1.6f);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("tension", energy);
        if (colonyId != null) {
            out.store("colony", UUIDUtil.CODEC, colonyId);
        }
        if (home != null) {
            out.store("home", BlockPos.CODEC, home);
        }
        out.store("cargo", ItemStack.OPTIONAL_CODEC.listOf(), cargo);
        out.putString("fail_reason", failReason);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setEnergy(in.getIntOr("tension", workerType.capacity()));
        colonyId = in.read("colony", UUIDUtil.CODEC).orElse(null);
        home = in.read("home", BlockPos.CODEC).orElse(null);
        List<ItemStack> saved = in.read("cargo", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < cargo.size(); i++) {
            cargo.set(i, i < saved.size() ? saved.get(i) : ItemStack.EMPTY);
        }
        failReason = in.getStringOr("fail_reason", "");
        syncCargo();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        WorkerAnimations.register(this, controllers);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Play a one-shot animation on the action controller for tracking clients. */
    public void trigger(String anim) {
        triggerAnim("action", anim);
    }

    /** Play a worker sound at the worker. */
    public void sound(String id, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), AutarkiaSounds.of(id), SoundSource.NEUTRAL, volume, pitch);
    }
}

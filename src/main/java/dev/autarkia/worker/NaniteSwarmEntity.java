package dev.autarkia.worker;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.NaniteClaim;
import dev.autarkia.colony.NaniteClaims;
import dev.autarkia.colony.NaniteEngine;
import dev.autarkia.colony.NaniteHiveBlockEntity;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.BeamVisualPayload;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.storage.MatterBufferBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Nanite Swarm (06 sections 1 and 3.11): a 3 x 3 x 3 cloud with no model, no physics and no gravity, run by a state
 * machine on the server. RELEASE above its hive, TRAVEL to the claim at 0.3 blocks per tick, DISASSEMBLE (it hovers on
 * the claim cursor while {@link NaniteEngine} spends its {@code naniteBlocksPerSecond} allowance and its FE), RETURN to
 * the hive when the claim is done (after its items are delivered, at most 600 ticks), and DISSOLVE into 2
 * {@code nanite_paste} for the hive. FE buffer 50,000, refilled by the hive tether within 64 blocks (horizontal);
 * outside that range or with an empty buffer it edits nothing. Drain 50 FE per second. Ore items wait in its 512-item
 * buffer and fly to the matter buffer on visual streams (05 section 8.3 colors, arrival after distance / 8 seconds);
 * items the buffer refuses come back and retry after 100 ticks. Integrity 40 (synced): only players and mobs hurt it; at
 * 0 it dissolves where it is, drops 2 {@code nanite_paste} and its items, and leaves its claim.
 */
public class NaniteSwarmEntity extends Entity implements JadeInfo {
    /** Swarm states of 06 section 3.11. */
    public enum Phase { RELEASE, TRAVEL, DISASSEMBLE, RETURN, DISSOLVE }

    public static final int CAPACITY = 50000;
    public static final float MAX_INTEGRITY = 40f;
    public static final int MAX_ITEMS = 512;
    public static final double SPEED = 0.3;
    public static final int TETHER_RANGE = 64;
    public static final int TETHER_RATE = 4000;
    public static final int DISSOLVE_PASTE = 2;
    private static final int RELEASE_TICKS = 20;
    private static final int DRAIN_PER_SECOND = 50;
    private static final int FLUSH_TICKS = 600;
    private static final int RETRY_TICKS = 100;
    private static final EntityDataAccessor<Float> INTEGRITY = SynchedEntityData.defineId(NaniteSwarmEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(NaniteSwarmEntity.class, EntityDataSerializers.BYTE);
    private @Nullable UUID colonyId;
    private @Nullable UUID claimId;
    private BlockPos hivePos = BlockPos.ZERO;
    private int energy;
    private double allowance;
    private int phaseTicks;
    private boolean hiveGone;
    private boolean dissolved;
    private long retryAt;
    private final List<Pending> pending = new ArrayList<>();
    private final List<Flight> flights = new ArrayList<>();

    private record Pending(Vec3 from, int color, List<ItemStack> items, @Nullable BlockPos target) {
    }

    private record Flight(long due, BlockPos target, List<ItemStack> items) {
    }

    public NaniteSwarmEntity(EntityType<? extends NaniteSwarmEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Create a swarm released at a point above its hive, bound to the colony and the claim; the caller adds it to the level. */
    public static NaniteSwarmEntity create(ServerLevel level, Vec3 at, BlockPos hive, UUID colony, UUID claim) {
        NaniteSwarmEntity s = new NaniteSwarmEntity(AutarkiaEntities.NANITE_SWARM.get(), level);
        s.colonyId = colony;
        s.claimId = claim;
        s.hivePos = hive.immutable();
        s.setPos(at.x, at.y, at.z);
        return s;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(INTEGRITY, MAX_INTEGRITY);
        b.define(PHASE, (byte) Phase.RELEASE.ordinal());
    }

    /** Return the state. */
    public Phase phase() {
        return Phase.values()[Math.max(0, Math.min(Phase.values().length - 1, entityData.get(PHASE)))];
    }

    private void setPhase(Phase p) {
        entityData.set(PHASE, (byte) p.ordinal());
        phaseTicks = 0;
    }

    /** Return the integrity 0 to 40. */
    public float integrity() {
        return entityData.get(INTEGRITY);
    }

    /** Return the stored FE. */
    public int energy() {
        return energy;
    }

    /** Return the FE the buffer can still take. */
    public int room() {
        return CAPACITY - energy;
    }

    /** Add FE from the hive tether; returns the FE taken. */
    public int charge(int fe) {
        int add = Math.max(0, Math.min(fe, room()));
        energy += add;
        return add;
    }

    /** Spend FE for an edit (the engine checks the amount first). */
    public void spend(int fe) {
        energy = Math.max(0, energy - fe);
    }

    /** Return the edit units the per-swarm rate allows now (8 per edit). */
    public double allowance() {
        return allowance;
    }

    /** Use edit units of the per-swarm rate. */
    public void useAllowance(int units) {
        allowance -= units;
    }

    /** Return the claim id. */
    public @Nullable UUID claimId() {
        return claimId;
    }

    /** Return the hive position. */
    public BlockPos hivePos() {
        return hivePos;
    }

    /** Return whether the swarm is within the 64-block (horizontal) tether of its hive. */
    public boolean tethered() {
        double dx = getX() - (hivePos.getX() + 0.5);
        double dz = getZ() - (hivePos.getZ() + 0.5);
        return dx * dx + dz * dz <= TETHER_RANGE * TETHER_RANGE;
    }

    /** Return the items held: waiting for a stream and in flight. */
    public int heldCount() {
        int n = 0;
        for (Pending p : pending) {
            n += p.items.stream().mapToInt(ItemStack::getCount).sum();
        }
        for (Flight f : flights) {
            n += f.items.stream().mapToInt(ItemStack::getCount).sum();
        }
        return n;
    }

    /** Queue the items of one ore cell for the next stream from the cell to the buffer at target (items may be empty: a visual stream). */
    public void queueStream(Vec3 from, int color, List<ItemStack> items, BlockPos target) {
        pending.add(new Pending(from, color, items.stream().map(ItemStack::copy).toList(), target));
    }

    /** Drop an item with no matter value at the swarm (C6). */
    public void dropAtSwarm(ItemStack stack) {
        Containers.dropItemStack(level(), getX(), getY() + 1.5, getZ(), stack);
    }

    /** Called when the claim is done: the swarm delivers its items, then flies home. */
    public void claimDone() {
        phaseTicks = 0;
    }

    /** Called when the hive is gone: the swarm flies to its spot and dissolves there. */
    public void recall() {
        hiveGone = true;
        if (phase() != Phase.RETURN && phase() != Phase.DISSOLVE) {
            setPhase(Phase.RETURN);
        }
    }

    private @Nullable Colony colony() {
        return colonyId == null || Colonies.server() == null ? null : Colonies.registry().get(colonyId);
    }

    private @Nullable NaniteClaim claim() {
        return claimId == null ? null : NaniteClaims.find(colonyId, claimId);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl) || dissolved) {
            return;
        }
        phaseTicks++;
        if (tickCount % 20 == 0) {
            heartbeat(sl);
        }
        deliver(sl);
        stream(sl);
        int bps = AutarkiaConfig.NANITE_BLOCKS_PER_SECOND.get();
        allowance = Math.min(bps * (double) NaniteEngine.EDIT_UNITS, allowance + bps * (double) NaniteEngine.EDIT_UNITS / 20.0);
        NaniteClaim cl = claim();
        switch (phase()) {
            case RELEASE -> {
                moveTo(home().add(0, 1, 0), false);
                if (phaseTicks >= RELEASE_TICKS) {
                    setPhase(cl == null || cl.state == NaniteClaim.State.DONE ? Phase.RETURN : Phase.TRAVEL);
                }
            }
            case TRAVEL -> {
                if (cl == null || cl.state == NaniteClaim.State.DONE) {
                    setPhase(Phase.RETURN);
                } else if (moveTo(workPoint(cl), true)) {
                    setPhase(Phase.DISASSEMBLE);
                }
            }
            case DISASSEMBLE -> {
                if (cl == null || cl.state == NaniteClaim.State.DONE) {
                    if (heldCount() == 0 || phaseTicks > FLUSH_TICKS) {
                        setPhase(Phase.RETURN);
                    }
                } else {
                    moveTo(workPoint(cl), true);
                }
            }
            case RETURN -> {
                if (moveTo(home(), false)) {
                    dissolve(sl, !hiveGone);
                }
            }
            default -> {
            }
        }
    }

    private void heartbeat(ServerLevel sl) {
        if (energy > 0) {
            energy = Math.max(0, energy - DRAIN_PER_SECOND);
        }
        Colony c = colony();
        if (c != null) {
            c.workers.put(getUUID(), new Colony.WorkerEntry(getUUID(), WorkerType.NANITE_SWARM.id(), WorkerType.NANITE_SWARM.capCost(), chunkPosition().pack(),
                sl.getGameTime()));
        }
        Phase p = phase();
        if ((c == null || claim() == null) && p != Phase.RETURN && p != Phase.DISSOLVE && p != Phase.DISASSEMBLE) {
            setPhase(Phase.RETURN);
        }
    }

    private Vec3 home() {
        return level().getBlockEntity(hivePos) instanceof NaniteHiveBlockEntity h ? h.releasePoint() : Vec3.atBottomCenterOf(hivePos).add(0, 2, 0);
    }

    private static Vec3 workPoint(NaniteClaim cl) {
        if (cl.cursorY < cl.min.getY()) {
            return Vec3.atBottomCenterOf(cl.min).add((cl.max.getX() - cl.min.getX()) / 2.0, 0, (cl.max.getZ() - cl.min.getZ()) / 2.0);
        }
        BlockPos cell = cl.cell(cl.cursorY, Math.min(cl.cursorI, cl.area(cl.cursorY) - 1));
        return Vec3.atBottomCenterOf(cell).add(0, -1, 0);
    }

    private boolean moveTo(Vec3 target, boolean needsEnergy) {
        if (needsEnergy && energy <= 0) {
            return false;
        }
        Vec3 d = target.subtract(position());
        double len = d.length();
        if (len <= SPEED) {
            setPos(target.x, target.y, target.z);
            return true;
        }
        Vec3 next = position().add(d.scale(SPEED / len));
        setPos(next.x, next.y, next.z);
        return false;
    }

    private @Nullable MatterBufferBlockEntity target(ServerLevel sl, @Nullable BlockPos at) {
        if (at != null && sl.getBlockEntity(at) instanceof MatterBufferBlockEntity b) {
            return b;
        }
        Colony c = colony();
        return c == null ? null : NaniteEngine.linkedBuffer(sl, c, hivePos);
    }

    private void stream(ServerLevel sl) {
        if (pending.isEmpty() || sl.getGameTime() < retryAt) {
            return;
        }
        Pending first = pending.get(0);
        MatterBufferBlockEntity buffer = target(sl, first.target);
        if (buffer == null) {
            retryAt = sl.getGameTime() + RETRY_TICKS;
            return;
        }
        if (!NaniteEngine.tryStream(sl.getServer())) {
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        pending.forEach(p -> items.addAll(p.items));
        pending.clear();
        Vec3 to = Vec3.atCenterOf(buffer.getBlockPos()).add(0, 0.5, 0);
        int ticks = Math.max(1, (int) Math.ceil(first.from.distanceTo(to) / 8.0 * 20));
        if (!items.isEmpty()) {
            flights.add(new Flight(sl.getGameTime() + ticks, buffer.getBlockPos(), items));
        }
        AutarkiaNetwork.toTracking(sl, ChunkPos.containing(BlockPos.containing(first.from)),
            new BeamVisualPayload(getId(), first.from, to, ticks, first.color, BeamVisualPayload.KIND_STREAM, 0.1f));
        if (claimId != null) {
            NaniteClaims.stats(claimId).streams++;
        }
    }

    private void deliver(ServerLevel sl) {
        if (flights.isEmpty()) {
            return;
        }
        long now = sl.getGameTime();
        List<ItemStack> back = new ArrayList<>();
        BlockPos retryTarget = null;
        for (Flight f : List.copyOf(flights)) {
            if (f.due > now) {
                continue;
            }
            flights.remove(f);
            MatterBufferBlockEntity b = target(sl, f.target);
            for (ItemStack s : f.items) {
                int took = b == null ? 0 : b.insertItems(s.copy(), false);
                if (took < s.getCount()) {
                    back.add(s.copyWithCount(s.getCount() - took));
                    retryTarget = b == null ? null : b.getBlockPos();
                }
            }
        }
        if (!back.isEmpty()) {
            pending.add(new Pending(position().add(0, 1.5, 0), 0xFFFFFFFF, back, retryTarget));
            retryAt = now + RETRY_TICKS;
        }
    }

    private List<ItemStack> allItems() {
        List<ItemStack> all = new ArrayList<>();
        pending.forEach(p -> all.addAll(p.items));
        flights.forEach(f -> all.addAll(f.items));
        return all;
    }

    private void dissolve(ServerLevel sl, boolean atHive) {
        if (dissolved) {
            return;
        }
        dissolved = true;
        setPhase(Phase.DISSOLVE);
        List<ItemStack> items = allItems();
        pending.clear();
        flights.clear();
        ItemStack paste = new ItemStack(AutarkiaItems.NANITE_PASTE.get(), DISSOLVE_PASTE);
        if (atHive && sl.getBlockEntity(hivePos) instanceof NaniteHiveBlockEntity hive) {
            paste = hive.returnPaste(paste);
            MatterBufferBlockEntity b = target(sl, null);
            List<ItemStack> rest = new ArrayList<>();
            for (ItemStack s : items) {
                int took = b == null ? 0 : b.insertItems(s.copy(), false);
                if (took < s.getCount()) {
                    rest.add(s.copyWithCount(s.getCount() - took));
                }
            }
            items = rest;
        }
        Vec3 at = position().add(0, 1, 0);
        if (!paste.isEmpty()) {
            Containers.dropItemStack(sl, at.x, at.y, at.z, paste);
        }
        items.forEach(s -> Containers.dropItemStack(sl, at.x, at.y, at.z, s));
        sl.sendParticles(GlowParticleOptions.of(0xFFC9D1D9).size(0.15f).age(20), at.x, at.y, at.z, 40, 1.0, 1.0, 1.0, 0.05);
        NaniteClaims.swarmGone(colonyId, claimId, getUUID());
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity by = source.getEntity();
        if (dissolved || !(by instanceof Player || by instanceof Mob) || isInvulnerableToBase(source)) {
            return false;
        }
        float left = Math.max(0f, integrity() - amount);
        entityData.set(INTEGRITY, left);
        level.sendParticles(GlowParticleOptions.of(0xFFC9D1D9).size(0.12f), getX(), getY() + 1.5, getZ(), 12, 0.8, 0.8, 0.8, 0.05);
        if (left <= 0f) {
            dissolve(level, false);
        }
        return true;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && reason.shouldDestroy()) {
            NaniteClaims.swarmGone(colonyId, claimId, getUUID());
        }
        super.remove(reason);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public String jadeGroup() {
        return "worker";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.nanite_swarm.integrity", Math.round(integrity()), Math.round(MAX_INTEGRITY)));
        out.add(Component.translatable("jade.autarkia.nanite_swarm.state",
            Component.translatable("nanite.autarkia.phase." + phase().name().toLowerCase(java.util.Locale.ROOT))));
        out.add(Component.translatable("jade.autarkia.nanite_swarm.buffer", energy, CAPACITY, heldCount(), MAX_ITEMS));
    }

    @Override
    public float jadeProgress() {
        return integrity() / MAX_INTEGRITY;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        colonyId = in.read("colony", UUIDUtil.CODEC).orElse(null);
        claimId = in.read("claim", UUIDUtil.CODEC).orElse(null);
        hivePos = in.read("hive", BlockPos.CODEC).orElse(BlockPos.ZERO);
        energy = in.getIntOr("energy", 0);
        allowance = in.getDoubleOr("allowance", 0);
        hiveGone = in.getBooleanOr("hive_gone", false);
        entityData.set(INTEGRITY, in.getFloatOr("integrity", MAX_INTEGRITY));
        entityData.set(PHASE, (byte) in.getIntOr("phase", Phase.RELEASE.ordinal()));
        pending.clear();
        flights.clear();
        List<ItemStack> items = in.read("items", ItemStack.CODEC.listOf()).orElse(List.of());
        if (!items.isEmpty()) {
            pending.add(new Pending(position().add(0, 1.5, 0), 0xFFFFFFFF, new ArrayList<>(items), null));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        if (colonyId != null) {
            out.store("colony", UUIDUtil.CODEC, colonyId);
        }
        if (claimId != null) {
            out.store("claim", UUIDUtil.CODEC, claimId);
        }
        out.store("hive", BlockPos.CODEC, hivePos);
        out.putInt("energy", energy);
        out.putDouble("allowance", allowance);
        out.putBoolean("hive_gone", hiveGone);
        out.putFloat("integrity", integrity());
        out.putInt("phase", phase().ordinal());
        List<ItemStack> items = allItems().stream().filter(s -> !s.isEmpty()).toList();
        if (!items.isEmpty()) {
            out.store("items", ItemStack.CODEC.listOf(), items);
        }
    }
}

package dev.autarkia.storage;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.worker.WorkerCollectable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A crate in free fall (04 sections 2.3.4 and 2.3.5), mode {@code COLLAPSE}: it tumbles, adds impact load to a rack it
 * hits at more than 0.3 blocks per tick, and lands as a {@code crate} block with all contents (nearest free position
 * within 3 blocks, floor first), or spills the items with an unlimited lifetime when there is no room or it lands in
 * lava. Below the minimum build height the contents are lost and the Core logs {@code crate_lost}. After 600 ticks in
 * the air it lands where it is. Hauler Crabs can collect it.
 */
public class FallingCrateEntity extends Entity implements WorkerCollectable {
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(FallingCrateEntity.class, EntityDataSerializers.INT);
    private ItemStack crate = ItemStack.EMPTY;
    private int age;

    public FallingCrateEntity(EntityType<? extends FallingCrateEntity> type, Level level) {
        super(type, level);
    }

    /**
     * Release a collapsing crate at start with the given velocity; over the {@code fallingCrateCap} of live crates in the
     * level it goes straight to a crate block on the nearest free floor.
     */
    public static void collapse(ServerLevel level, Vec3 start, Vec3 velocity, ItemStack crate) {
        int live = level.getEntities(AutarkiaEntities.FALLING_CRATE.get(), e -> true).size();
        if (live >= AutarkiaConfig.FALLING_CRATE_CAP.get()) {
            land(level, BlockPos.containing(start), crate, Vec3.atCenterOf(BlockPos.containing(start)));
            return;
        }
        FallingCrateEntity e = new FallingCrateEntity(AutarkiaEntities.FALLING_CRATE.get(), level);
        e.crate = crate.copyWithCount(1);
        e.setPos(start);
        e.setDeltaMovement(velocity);
        e.entityData.set(SEED, level.getRandom().nextInt());
        level.addFreshEntity(e);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(SEED, 0);
    }

    /** Return the tumble seed for the renderer. */
    public int seed() {
        return entityData.get(SEED);
    }

    /** Return the falling crate item with its contents (server). */
    public ItemStack crate() {
        return crate;
    }

    /** Return the crate block state the renderer draws. */
    public BlockState renderState() {
        return AutarkiaBlocks.CRATE.get().defaultBlockState();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 v = getDeltaMovement().add(0, -0.04, 0);
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        setDeltaMovement(getDeltaMovement().scale(0.98));
        age++;
        if (getY() < sl.getMinY() - 16) {
            lost(sl);
            return;
        }
        double speed = v.length();
        if (speed > 0.3 && (horizontalCollision || onGround())) {
            hitRacks(sl, v);
        }
        if (isInLava()) {
            spill(sl, blockPosition(), crate, position());
            discard();
            return;
        }
        if (onGround() || age >= 600) {
            land(sl, blockPosition(), crate, position());
            discard();
        }
    }

    private void hitRacks(ServerLevel sl, Vec3 v) {
        List<BlockPos> touched = new ArrayList<>();
        touched.add(BlockPos.containing(getX(), getBoundingBox().minY - 0.1, getZ()));
        if (horizontalCollision) {
            Direction d = Direction.getApproximateNearest(v.x, 0, v.z);
            touched.add(blockPosition().relative(d));
        }
        int q = RackBlockEntity.crateQ(crate) / 2;
        for (BlockPos p : touched) {
            if (sl.getBlockEntity(p) instanceof RackBlockEntity rack) {
                rack.impact(q);
            }
        }
    }

    private void lost(ServerLevel sl) {
        BlockPos at = BlockPos.containing(getX(), sl.getMinY(), getZ());
        Autarkia.LOG.info("[crate] lost below the world at {}", at.toShortString());
        if (Colonies.server() != null) {
            Colony c = Colonies.at(sl, at);
            if (c != null) {
                ColonyRuntime.of(c).log("crate_lost at " + at.toShortString());
                c.stat("crate_lost", 1);
            }
        }
        discard();
    }

    /** Land a crate at pos (04 section 2.3.5): a crate block there or within 3 blocks, else spill the items at the given point. */
    public static void land(ServerLevel sl, BlockPos pos, ItemStack crate, Vec3 at) {
        BlockPos target = free(sl, pos);
        if (target == null) {
            spill(sl, pos, crate, at);
            return;
        }
        BlockState st = AutarkiaBlocks.CRATE.get().defaultBlockState();
        sl.setBlockAndUpdate(target, st);
        if (sl.getBlockEntity(target) instanceof CrateBlockEntity be) {
            be.fill(crate);
        }
        sl.playSound(null, target, AutarkiaSounds.of("crate_land"), SoundSource.BLOCKS, 1.0f, 0.6f);
        Vec3 c = Vec3.atCenterOf(target);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), c.x, c.y, c.z, 20, 0.4, 0.3, 0.4, 0.05);
    }

    private static boolean freeAt(ServerLevel sl, BlockPos p) {
        BlockState st = sl.getBlockState(p);
        return st.canBeReplaced() && !st.getFluidState().is(FluidTags.LAVA) && p.getY() >= sl.getMinY() && p.getY() < sl.getMaxY();
    }

    private static BlockPos free(ServerLevel sl, BlockPos pos) {
        if (sl.getFluidState(pos).is(FluidTags.LAVA)) {
            return null;
        }
        if (freeAt(sl, pos)) {
            return pos;
        }
        List<BlockPos> cands = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))) {
            if (freeAt(sl, p)) {
                cands.add(p.immutable());
            }
        }
        cands.sort(Comparator.comparingInt((BlockPos p) -> sl.getBlockState(p.below()).isFaceSturdy(sl, p.below(), Direction.UP) ? 0 : 1)
            .thenComparingInt(p -> p.distManhattan(pos)).thenComparingLong(BlockPos::asLong));
        return cands.isEmpty() ? null : cands.get(0);
    }

    /** Spill the crate shell and contents as item entities that never despawn (Hauler Crabs collect them). */
    public static void spill(ServerLevel sl, BlockPos pos, ItemStack crate, Vec3 at) {
        List<ItemStack> out = new ArrayList<>();
        ItemContainerContents c = crate.get(DataComponents.CONTAINER);
        if (c != null) {
            c.nonEmptyItemCopyStream().forEach(out::add);
        }
        ItemStack shell = crate.copyWithCount(1);
        shell.remove(DataComponents.CONTAINER);
        out.add(shell);
        for (ItemStack s : out) {
            ItemEntity ie = new ItemEntity(sl, at.x, at.y + 0.5, at.z, s);
            ie.setUnlimitedLifetime();
            sl.addFreshEntity(ie);
        }
        Autarkia.LOG.info("[crate] spilled {} stacks at {}", out.size(), pos.toShortString());
    }

    @Override
    public List<ItemStack> collect(ServerLevel level) {
        discard();
        return List.of(crate.copy());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        crate = in.read("crate", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        age = in.getIntOr("age", 0);
        entityData.set(SEED, in.getIntOr("seed", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.store("crate", ItemStack.OPTIONAL_CODEC, crate);
        out.putInt("age", age);
        out.putInt("seed", seed());
    }
}

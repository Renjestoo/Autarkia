package dev.autarkia.colony;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;

/**
 * The {@code rising_block} effect entity in SCALE mode (05 section 8.2, 08b section 1): the block grows at its target
 * from nothing to full size, then the entity sets it with flags 3 and discards itself. No gravity, no collision. A big
 * block (Tiny Multiblock Lib center state) is placed with {@code canPlace} first; its block model is drawn by its block
 * entity renderer only, so the entity shows a glow column for it instead. A target that is no longer free is logged and
 * left as it is.
 */
public class RisingBlockEntity extends Entity {
    private static final EntityDataAccessor<BlockState> DATA_STATE = SynchedEntityData.defineId(RisingBlockEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<BlockPos> DATA_TARGET = SynchedEntityData.defineId(RisingBlockEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(RisingBlockEntity.class, EntityDataSerializers.INT);
    private int age;

    public RisingBlockEntity(EntityType<? extends RisingBlockEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Spawn a rising block that sets the state at the target after the given ticks; returns the entity. */
    public static RisingBlockEntity spawn(ServerLevel level, BlockPos target, BlockState state, int ticks) {
        RisingBlockEntity e = new RisingBlockEntity(AutarkiaEntities.RISING_BLOCK.get(), level);
        e.entityData.set(DATA_STATE, state);
        e.entityData.set(DATA_TARGET, target.immutable());
        e.entityData.set(DATA_DURATION, Math.max(1, ticks));
        e.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0, 0);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(DATA_STATE, Blocks.STONE.defaultBlockState());
        b.define(DATA_TARGET, BlockPos.ZERO);
        b.define(DATA_DURATION, 6);
    }

    /** Return the block state that arrives. */
    public BlockState blockState() {
        return entityData.get(DATA_STATE);
    }

    /** Return the target position. */
    public BlockPos target() {
        return entityData.get(DATA_TARGET);
    }

    /** Return the arrival progress 0 to 1 at the partial tick. */
    public float progress(float partialTick) {
        return Math.min(1f, (tickCount + partialTick) / entityData.get(DATA_DURATION));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        BlockPos t = target();
        age++;
        if (blockState().getRenderShape() != RenderShape.MODEL && age % 2 == 0) {
            sl.sendParticles(GlowParticleOptions.of(0xFFB39DFF).size(0.12f).age(10), t.getX() + 0.5, t.getY() + progress(0) * 1.5, t.getZ() + 0.5,
                4, 0.5, 0.4, 0.5, 0);
        }
        if (age >= entityData.get(DATA_DURATION)) {
            place(sl, t, blockState());
            discard();
        }
    }

    private static void place(ServerLevel level, BlockPos t, BlockState state) {
        if (state.getBlock() instanceof BaseMultiblock mb) {
            if (!mb.canPlace(level, t, state, null, false)) {
                Autarkia.LOG.info("[colony] rising block {} at {}: the space is not free, the block is not placed",
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()), t.toShortString());
                return;
            }
        } else if (!level.getBlockState(t).canBeReplaced()) {
            Autarkia.LOG.info("[colony] rising block {} at {}: the cell holds {}, the block is not placed",
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()), t.toShortString(), level.getBlockState(t));
            return;
        }
        level.setBlock(t, state, Block.UPDATE_ALL);
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
    protected void addAdditionalSaveData(ValueOutput out) {
        out.store("state", BlockState.CODEC, blockState());
        out.store("target", BlockPos.CODEC, target());
        out.putInt("duration", entityData.get(DATA_DURATION));
        out.putInt("age", age);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        in.read("state", BlockState.CODEC).ifPresent(s -> entityData.set(DATA_STATE, s));
        in.read("target", BlockPos.CODEC).ifPresent(p -> entityData.set(DATA_TARGET, p));
        entityData.set(DATA_DURATION, Math.max(1, in.getIntOr("duration", 6)));
        age = in.getIntOr("age", 0);
    }
}

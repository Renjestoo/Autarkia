package dev.autarkia.colony;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaEntities;
import java.util.Map;
import java.util.WeakHashMap;
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
 * The {@code rising_block} effect entity (05 sections 8.2 and 8.4, 08b section 1) in two modes. SCALE: the block grows
 * at its target from nothing to full size. RISE: the entity starts 3 blocks below its target and the block rises with
 * the normal model through the soil to the target in 20 ticks (M9 decision G19). On arrival the entity sets the block
 * with flags 3 and discards itself. No gravity, no collision; state, target, mode, start tick, duration and age are
 * synced or saved. A big block (Tiny Multiblock Lib center state) is placed with {@code canPlace} first; its block model
 * is drawn by its block entity renderer only, so the entity shows a glow column for it instead. A target that is no
 * longer free is logged and left as it is. The server counts the live entities per level for the seed alive cap. A
 * loaded RISE entity restarts its start tick from its saved age, so time spent in an unloaded chunk is not skipped.
 */
public class RisingBlockEntity extends Entity {
    /** Arrival mode. */
    public enum Mode { SCALE, RISE }

    public static final int RISE_TICKS = 20;
    public static final int RISE_DEPTH = 3;
    private static final EntityDataAccessor<BlockState> DATA_STATE = SynchedEntityData.defineId(RisingBlockEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<BlockPos> DATA_TARGET = SynchedEntityData.defineId(RisingBlockEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(RisingBlockEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(RisingBlockEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_START = SynchedEntityData.defineId(RisingBlockEntity.class, EntityDataSerializers.LONG);
    private static final Map<Level, int[]> ALIVE = new WeakHashMap<>();
    private int age;
    private boolean counted;

    public RisingBlockEntity(EntityType<? extends RisingBlockEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Spawn a rising block in SCALE mode that sets the state at the target after the given ticks; returns the entity. */
    public static RisingBlockEntity spawn(ServerLevel level, BlockPos target, BlockState state, int ticks) {
        RisingBlockEntity e = new RisingBlockEntity(AutarkiaEntities.RISING_BLOCK.get(), level);
        e.entityData.set(DATA_STATE, state);
        e.entityData.set(DATA_TARGET, target.immutable());
        e.entityData.set(DATA_DURATION, Math.max(1, ticks));
        e.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0, 0);
        level.addFreshEntity(e);
        return e;
    }

    /** Spawn a rising block in RISE mode: it starts at target y - 3 and arrives after 20 ticks; returns the entity. */
    public static RisingBlockEntity rise(ServerLevel level, BlockPos target, BlockState state) {
        RisingBlockEntity e = new RisingBlockEntity(AutarkiaEntities.RISING_BLOCK.get(), level);
        e.entityData.set(DATA_STATE, state);
        e.entityData.set(DATA_TARGET, target.immutable());
        e.entityData.set(DATA_DURATION, RISE_TICKS);
        e.entityData.set(DATA_MODE, (byte) Mode.RISE.ordinal());
        e.entityData.set(DATA_START, level.getGameTime());
        e.snapTo(target.getX() + 0.5, target.getY() - RISE_DEPTH, target.getZ() + 0.5, 0, 0);
        level.addFreshEntity(e);
        return e;
    }

    /** Return the number of rising blocks alive in the level (both modes), for the seed alive cap. */
    public static int alive(ServerLevel level) {
        int[] n = ALIVE.get(level);
        return n == null ? 0 : n[0];
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (level() instanceof ServerLevel sl && !counted) {
            counted = true;
            ALIVE.computeIfAbsent(sl, k -> new int[1])[0]++;
        }
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (level() instanceof ServerLevel sl && counted) {
            counted = false;
            int[] n = ALIVE.get(sl);
            if (n != null) {
                n[0] = Math.max(0, n[0] - 1);
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(DATA_STATE, Blocks.STONE.defaultBlockState());
        b.define(DATA_TARGET, BlockPos.ZERO);
        b.define(DATA_DURATION, 6);
        b.define(DATA_MODE, (byte) Mode.SCALE.ordinal());
        b.define(DATA_START, 0L);
    }

    /** Return the block state that arrives. */
    public BlockState blockState() {
        return entityData.get(DATA_STATE);
    }

    /** Return the target position. */
    public BlockPos target() {
        return entityData.get(DATA_TARGET);
    }

    /** Return the arrival mode. */
    public Mode mode() {
        return entityData.get(DATA_MODE) == Mode.RISE.ordinal() ? Mode.RISE : Mode.SCALE;
    }

    /** Return the game time the rise started (RISE mode). */
    public long start() {
        return entityData.get(DATA_START);
    }

    /** Return the arrival duration in ticks. */
    public int duration() {
        return entityData.get(DATA_DURATION);
    }

    /**
     * Return the arrival progress 0 to 1 at the partial tick: SCALE counts the ticks this entity lived on this side,
     * RISE counts game time from the synced start tick, so a client that starts tracking mid-rise sees the right height.
     */
    public float progress(float partialTick) {
        float ticks = mode() == Mode.RISE ? level().getGameTime() - start() + partialTick : tickCount + partialTick;
        return Math.max(0f, Math.min(1f, ticks / entityData.get(DATA_DURATION)));
    }

    /** Return the vertical offset of the drawn block below its target at the partial tick (RISE: 3 to 0, SCALE: 0). */
    public float depth(float partialTick) {
        return mode() == Mode.RISE ? RISE_DEPTH * (1f - progress(partialTick)) : 0f;
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
            float done = Math.min(1f, (float) age / entityData.get(DATA_DURATION));
            double y = mode() == Mode.RISE ? t.getY() - RISE_DEPTH * (1 - done) + 0.5 : t.getY() + done * 1.5;
            sl.sendParticles(GlowParticleOptions.of(0xFFB39DFF).size(0.12f).age(10), t.getX() + 0.5, y, t.getZ() + 0.5, 4, 0.5, 0.4, 0.5, 0);
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
        out.putString("mode", mode().name());
        out.putLong("start", start());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        in.read("state", BlockState.CODEC).ifPresent(s -> entityData.set(DATA_STATE, s));
        in.read("target", BlockPos.CODEC).ifPresent(p -> entityData.set(DATA_TARGET, p));
        entityData.set(DATA_DURATION, Math.max(1, in.getIntOr("duration", 6)));
        age = in.getIntOr("age", 0);
        entityData.set(DATA_MODE, (byte) (in.getStringOr("mode", Mode.SCALE.name()).equals(Mode.RISE.name()) ? Mode.RISE : Mode.SCALE).ordinal());
        entityData.set(DATA_START, mode() == Mode.RISE ? level().getGameTime() - age : in.getLongOr("start", 0L));
    }
}

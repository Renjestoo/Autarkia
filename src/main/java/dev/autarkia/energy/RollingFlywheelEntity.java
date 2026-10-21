package dev.autarkia.energy;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaSounds;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Runaway flywheel (02 section 6.2, W3): rolls along the wheel plane toward the lower side, v0 = 0.6 sqrt(E / 40,000),
 * slope +0.04 s, friction 0.985 (0.9 in water), gravity 0.08. Hits deal clamp(2 + 18 |v| / 0.6, 2, 20) and knock back
 * 1.5 |v|; soft blocks (destroy speed <= 1.0) break above 0.3 blocks/tick. Below 0.05 on level ground it wobbles for
 * 40 ticks, then drops a stone_flywheel item. Box: 3 along the roll axis, 1 thick, 3 tall.
 */
public class RollingFlywheelEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Boolean> ALONG_X = SynchedEntityData.defineId(RollingFlywheelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(RollingFlywheelEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> WOBBLING = SynchedEntityData.defineId(RollingFlywheelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation ROLL = RawAnimation.begin().thenLoop("animation.rolling_flywheel.roll");
    private static final RawAnimation WOBBLE = RawAnimation.begin().thenPlayAndHold("animation.rolling_flywheel.wobble");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Int2LongOpenHashMap lastHit = new Int2LongOpenHashMap();
    private double v;
    private double vy;
    private int wobble;
    private int age;
    private double lastSlope;

    public RollingFlywheelEntity(EntityType<? extends RollingFlywheelEntity> type, Level level) {
        super(type, level);
    }

    /** Create a released wheel at the mount with stored energy; facing is the wheel's facing (its plane is across it). */
    public static RollingFlywheelEntity create(ServerLevel level, Vec3 at, Direction facing, int energy) {
        RollingFlywheelEntity e = new RollingFlywheelEntity(AutarkiaEntities.ROLLING_FLYWHEEL.get(), level);
        boolean alongX = facing.getAxis() == Direction.Axis.Z;
        e.entityData.set(ALONG_X, alongX);
        e.setPos(at.x, at.y + 0.05, at.z);
        double v0 = 0.6 * Math.sqrt(energy / (double) StoneFlywheelBlockEntity.CAPACITY);
        int hPlus = e.groundAt(at.add(alongX ? 3 : 0, 0, alongX ? 0 : 3));
        int hMinus = e.groundAt(at.add(alongX ? -3 : 0, 0, alongX ? 0 : -3));
        e.v = hMinus < hPlus ? -v0 : v0;
        e.setYRot(alongX ? 90 : 0);
        e.setBoundingBox(e.makeBoundingBox(e.position()));
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(ALONG_X, true);
        b.define(SPEED, 0f);
        b.define(WOBBLING, false);
    }

    /** Return the roll speed in blocks per tick (signed along the roll axis). */
    public double speed() {
        return level().isClientSide() ? entityData.get(SPEED) : v;
    }

    /** Return the energy the wheel still carries, 40,000 (v / 0.6)^2. */
    public int energy() {
        double s = speed() / 0.6;
        return (int) Math.round(StoneFlywheelBlockEntity.CAPACITY * s * s);
    }

    private Vec3 axis() {
        return entityData.get(ALONG_X) ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
    }

    private int groundAt(Vec3 p) {
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(p.x), Mth.floor(p.z));
    }

    @Override
    protected AABB makeBoundingBox(Vec3 p) {
        boolean x = entityData.get(ALONG_X);
        double hx = x ? 1.45 : 0.5;
        double hz = x ? 0.5 : 1.45;
        return new AABB(p.x - hx, p.y, p.z - hz, p.x + hx, p.y + 2.9, p.z + hz);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ALONG_X.equals(key)) {
            setBoundingBox(makeBoundingBox(position()));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        if (++age > 1200) {
            finish(sl);
            return;
        }
        if (wobble > 0) {
            if (--wobble == 0) {
                finish(sl);
            }
            return;
        }
        Vec3 ax = axis();
        Vec3 here = position();
        double s = (groundAt(here.subtract(ax.scale(Math.signum(v) == 0 ? 1 : Math.signum(v)))) - groundAt(here.add(ax.scale(Math.signum(v) == 0 ? 1 : Math.signum(v))))) / 2.0;
        lastSlope = s;
        vy = (vy - 0.08) * 0.98;
        v += 0.04 * s * Math.signum(v == 0 ? 1 : v);
        v *= isInWater() ? 0.9 : onGround() ? 0.985 : 1.0;
        Vec3 before = position();
        move(MoverType.SELF, new Vec3(ax.x * v, vy, ax.z * v));
        if (onGround()) {
            vy = 0;
        }
        if (horizontalCollision) {
            wallHit(sl, ax);
        }
        hitEntities(sl, ax);
        entityData.set(SPEED, (float) v);
        setYRot(entityData.get(ALONG_X) ? (v >= 0 ? 270 : 90) : (v >= 0 ? 0 : 180));
        if (age % 20 == 0 && Math.abs(v) > 0.05) {
            sl.playSound(null, getX(), getY(), getZ(), AutarkiaSounds.of("flywheel.roll"), SoundSource.BLOCKS, 0.8f, 0.5f);
        }
        if (Math.abs(v) < 0.05 && Math.abs(lastSlope) < 0.1 && onGround() && before.distanceToSqr(position()) < 0.01) {
            wobble = 40;
            entityData.set(WOBBLING, true);
        }
    }

    private void wallHit(ServerLevel sl, Vec3 ax) {
        BlockPos p = BlockPos.containing(position().add(ax.scale(Math.signum(v) * 1.6)).add(0, 0.5, 0));
        BlockState st = sl.getBlockState(p);
        float hardness = st.getDestroySpeed(sl, p);
        if (Math.abs(v) > 0.3 && hardness >= 0 && hardness <= 1.0f && AutarkiaConfig.ROLLING_FLYWHEEL_BREAKS_BLOCKS.get() && !st.isAir()) {
            sl.destroyBlock(p, true, this);
            v *= 0.6;
        } else {
            v = -0.4 * v;
        }
        sl.playSound(null, getX(), getY(), getZ(), AutarkiaSounds.of("flywheel.impact"), SoundSource.BLOCKS, 0.9f, 0.6f);
    }

    private void hitEntities(ServerLevel sl, Vec3 ax) {
        long now = sl.getGameTime();
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.1))) {
            if (now - lastHit.getOrDefault(le.getId(), -100L) < 10 || Math.abs(v) < 0.05) {
                continue;
            }
            lastHit.put(le.getId(), now);
            float dmg = (float) Mth.clamp(2 + 18 * Math.abs(v) / 0.6, 2, 20);
            le.hurtServer(sl, sl.damageSources().fallingBlock(this), dmg);
            Vec3 kb = ax.scale(Math.signum(v) * 1.5 * Math.abs(v));
            le.push(kb.x, 0.1, kb.z);
            le.needsSync = true;
            v *= 0.8;
        }
    }

    private void finish(ServerLevel sl) {
        Containers.dropItemStack(sl, getX(), getY() + 0.5, getZ(), new ItemStack(AutarkiaItems.STONE_FLYWHEEL.get()));
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RollingFlywheelEntity>("roll", 4, test -> {
            RollingFlywheelEntity e = test.animatable();
            if (e.entityData.get(WOBBLING)) {
                test.setControllerSpeed(1f);
                return test.setAndContinue(WOBBLE);
            }
            test.setControllerSpeed((float) Math.max(0.05, Math.abs(e.speed()) / 0.3));
            return test.setAndContinue(ROLL);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        v = in.getDoubleOr("v", 0);
        vy = in.getDoubleOr("vy", 0);
        age = in.getIntOr("age", 0);
        wobble = in.getIntOr("wobble", 0);
        entityData.set(ALONG_X, in.getBooleanOr("alongX", true));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putDouble("v", v);
        out.putDouble("vy", vy);
        out.putInt("age", age);
        out.putInt("wobble", wobble);
        out.putBoolean("alongX", entityData.get(ALONG_X));
    }
}

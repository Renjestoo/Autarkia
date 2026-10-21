package dev.autarkia.energy;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaEntities;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Snapped cable end (02 section 4.4): a pendulum that whips around its anchor (theta starts at 0.3 rad, omega
 * +-0.6 rad/tick, gravity -0.08 sin(theta), damping 0.92), hurts entities within 0.8 blocks of the anchor-to-tip
 * segment (again after 10 ticks), and at the end of its lifetime drops ceil(L / 16) cable items where the tip rests.
 * The entity position is the tip; the client draws an 8-point verlet chain to it.
 */
public class SnappedCableEntity extends Entity {
    private static final EntityDataAccessor<org.joml.Vector3fc> ANCHOR = SynchedEntityData.defineId(SnappedCableEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Byte> TIER = SynchedEntityData.defineId(SnappedCableEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> HALF = SynchedEntityData.defineId(SnappedCableEntity.class, EntityDataSerializers.FLOAT);
    private double theta = 0.3;
    private double omega;
    private double phi;
    private int age;
    private int drops;
    private final Int2LongOpenHashMap lastHit = new Int2LongOpenHashMap();
    public final Vec3[] chain = new Vec3[8];
    public final Vec3[] chainOld = new Vec3[8];

    public SnappedCableEntity(EntityType<? extends SnappedCableEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Create a cable end hanging from anchor; phi is the span direction (radians), length the whole span length. */
    public static SnappedCableEntity create(ServerLevel level, Vec3 anchor, double phi, CableTier tier, float length) {
        SnappedCableEntity e = new SnappedCableEntity(AutarkiaEntities.SNAPPED_CABLE.get(), level);
        e.entityData.set(ANCHOR, new Vector3f((float) anchor.x, (float) anchor.y, (float) anchor.z));
        e.entityData.set(TIER, (byte) tier.ordinal());
        e.entityData.set(HALF, length / 2f);
        e.phi = phi;
        e.omega = level.getRandom().nextBoolean() ? 0.6 : -0.6;
        e.drops = (int) Math.ceil(length / 16.0);
        e.setPos(e.tipPos());
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(ANCHOR, new Vector3f());
        b.define(TIER, (byte) 0);
        b.define(HALF, 1f);
    }

    /** Return the anchor point. */
    public Vec3 anchor() {
        org.joml.Vector3fc a = entityData.get(ANCHOR);
        return new Vec3(a.x(), a.y(), a.z());
    }

    /** Return the cable tier. */
    public CableTier tier() {
        return CableTier.values()[entityData.get(TIER)];
    }

    /** Return the hanging length (half the span). */
    public float half() {
        return entityData.get(HALF);
    }

    private Vec3 tipPos() {
        double lh = half();
        return anchor().add(lh * Math.sin(theta) * Math.cos(phi), -lh * Math.cos(theta), lh * Math.sin(theta) * Math.sin(phi));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            tickChain();
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        omega += -0.08 * Math.sin(theta);
        omega *= 0.92;
        theta += omega;
        Vec3 tip = tipPos();
        setPos(tip);
        hitEntities(sl, tip);
        if (++age >= tier().snapLifetime) {
            dropItems(sl, tip);
            discard();
        }
    }

    private void hitEntities(ServerLevel sl, Vec3 tip) {
        Vec3 a = anchor();
        AABB box = new AABB(a, tip).inflate(0.8);
        float dmg = tier().snapDamage * AutarkiaConfig.SNAPPED_CABLE_DAMAGE_MULTIPLIER.get().floatValue();
        long now = sl.getGameTime();
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (distToSegment(le.getBoundingBox().getCenter(), a, tip) > 0.8 + le.getBbWidth() / 2) {
                continue;
            }
            if (now - lastHit.getOrDefault(le.getId(), -100L) < 10) {
                continue;
            }
            lastHit.put(le.getId(), now);
            DamageSource src = tier() == CableTier.ROPE ? sl.damageSources().generic() : sl.damageSources().lightningBolt();
            le.hurtServer(sl, src, dmg);
        }
    }

    private static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = ab.lengthSqr() < 1e-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / ab.lengthSqr()));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    private void dropItems(ServerLevel sl, Vec3 tip) {
        if (drops <= 0 || tier() == CableTier.SUPERCONDUCTOR) {
            return;
        }
        BlockPos p = BlockPos.containing(tip.x, anchor().y, tip.z);
        int guard = 0;
        while (guard++ < 64 && sl.getBlockState(p.below()).getCollisionShape(sl, p.below()).isEmpty() && p.getY() > sl.getMinY()) {
            p = p.below();
        }
        while (!sl.getBlockState(p).getCollisionShape(sl, p).isEmpty() && guard++ < 128) {
            p = p.above();
        }
        Containers.dropItemStack(sl, tip.x, p.getY() + 0.2, tip.z, new ItemStack(Grid.cableItem(tier()), drops));
    }

    private void tickChain() {
        Vec3 a = anchor();
        Vec3 tip = position();
        if (chain[0] == null) {
            for (int i = 0; i < chain.length; i++) {
                chain[i] = a.lerp(tip, i / (double) (chain.length - 1));
                chainOld[i] = chain[i];
            }
        }
        double seg = half() / (chain.length - 1);
        for (int i = 1; i < chain.length - 1; i++) {
            Vec3 v = chain[i].subtract(chainOld[i]).scale(0.96);
            chainOld[i] = chain[i];
            chain[i] = chain[i].add(v).add(0, -0.04, 0);
        }
        chain[0] = a;
        chain[chain.length - 1] = tip;
        for (int iter = 0; iter < 6; iter++) {
            for (int i = 0; i < chain.length - 1; i++) {
                Vec3 d = chain[i + 1].subtract(chain[i]);
                double len = d.length();
                if (len < 1e-6) {
                    continue;
                }
                Vec3 corr = d.scale((len - seg) / len * 0.5);
                if (i != 0) {
                    chain[i] = chain[i].add(corr);
                }
                if (i + 1 != chain.length - 1) {
                    chain[i + 1] = chain[i + 1].subtract(corr);
                }
            }
        }
        for (int i = 1; i < chain.length - 1; i++) {
            BlockPos bp = BlockPos.containing(chain[i]);
            var shape = level().getBlockState(bp).getCollisionShape(level(), bp);
            if (!shape.isEmpty()) {
                double top = bp.getY() + shape.max(net.minecraft.core.Direction.Axis.Y) + 0.03;
                if (chain[i].y < top) {
                    chain[i] = new Vec3(chain[i].x, top, chain[i].z);
                }
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        theta = in.getDoubleOr("theta", 0.3);
        omega = in.getDoubleOr("omega", 0);
        phi = in.getDoubleOr("phi", 0);
        age = in.getIntOr("age", 0);
        drops = in.getIntOr("drops", 1);
        entityData.set(ANCHOR, new Vector3f(in.getFloatOr("ax", 0), in.getFloatOr("ay", 0), in.getFloatOr("az", 0)));
        entityData.set(TIER, (byte) in.getIntOr("tier", 0));
        entityData.set(HALF, in.getFloatOr("half", 1));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putDouble("theta", theta);
        out.putDouble("omega", omega);
        out.putDouble("phi", phi);
        out.putInt("age", age);
        out.putInt("drops", drops);
        org.joml.Vector3fc a = entityData.get(ANCHOR);
        out.putFloat("ax", a.x());
        out.putFloat("ay", a.y());
        out.putFloat("az", a.z());
        out.putInt("tier", entityData.get(TIER));
        out.putFloat("half", half());
    }
}

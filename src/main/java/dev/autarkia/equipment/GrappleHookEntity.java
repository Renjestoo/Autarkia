package dev.autarkia.equipment;

import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The {@code grapple_hook} projectile (07 sections 1.5 and 4.2): flies straight at 3.0 blocks/tick without gravity and
 * vanishes after 48 blocks. It attaches where it first hits a block; the line length is the distance from the shooter's
 * center at that moment and only shrinks while the shooter is pulled. It ignores entities. The attached state and the
 * line length sync through entity data. The server removes the hook when the shooter is gone or stops using a
 * {@link GrappleLauncherItem}; every removal by the shooter's release grants the 40-tick fall protection.
 */
public class GrappleHookEntity extends Projectile {
    public static final float SPEED = 3.0f;
    public static final double RANGE = 48;
    private static final EntityDataAccessor<Boolean> ATTACHED = SynchedEntityData.defineId(GrappleHookEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(GrappleHookEntity.class, EntityDataSerializers.FLOAT);
    private Vec3 origin = Vec3.ZERO;
    private float attachLength;
    private boolean fullPull;

    public GrappleHookEntity(EntityType<? extends GrappleHookEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Create a hook at the shooter's eyes, flying along its look direction. */
    public GrappleHookEntity(Level level, LivingEntity shooter) {
        this(AutarkiaEntities.GRAPPLE_HOOK.get(), level);
        setOwner(shooter);
        Vec3 eye = shooter.getEyePosition();
        setPos(eye.x, eye.y - 0.1, eye.z);
        origin = position();
        shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0, SPEED, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ATTACHED, false);
        builder.define(LENGTH, 0f);
    }

    /** Return true once the hook holds on a block. */
    public boolean attached() {
        return entityData.get(ATTACHED);
    }

    /** Return the current line length in blocks (0 while flying). */
    public float length() {
        return entityData.get(LENGTH);
    }

    /** Return the line length at the moment the hook attached. */
    public float attachLength() {
        return attachLength;
    }

    /** Return true when the shooter was pulled to the hook along a line of at least 40 blocks. */
    public boolean fullPull() {
        return fullPull;
    }

    /** Shorten the line to the given distance (it never grows back). */
    public void reel(double distance) {
        if (distance < length()) {
            entityData.set(LENGTH, (float) distance);
        }
    }

    /** Mark a completed 40+ block pull; return true the first time. */
    public boolean completePull() {
        if (fullPull || attachLength < GrappleLauncherItem.SWING_DISTANCE) {
            return false;
        }
        fullPull = true;
        return true;
    }

    /** Return the center of a shooter, the point the line pulls toward the hook. */
    public static Vec3 center(Entity e) {
        return e.position().add(0, e.getBbHeight() / 2, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && !ownerHolds()) {
            if (getOwner() instanceof LivingEntity le) {
                GrappleLauncherItem.protect(le);
            }
            discard();
            return;
        }
        if (attached()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());
        BlockHitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS) {
            setPos(hit.getLocation());
            setDeltaMovement(Vec3.ZERO);
            if (!level().isClientSide()) {
                attach();
            }
            return;
        }
        setPos(to);
        if (!level().isClientSide() && position().distanceTo(origin) > RANGE) {
            discard();
        }
    }

    private boolean ownerHolds() {
        return getOwner() instanceof LivingEntity le && le.isAlive() && le.level() == level() && le.isUsingItem()
            && le.getUseItem().getItem() instanceof GrappleLauncherItem;
    }

    private void attach() {
        Entity owner = getOwner();
        attachLength = owner == null ? 0f : (float) center(owner).distanceTo(position());
        entityData.set(LENGTH, attachLength);
        entityData.set(ATTACHED, true);
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, getX(), getY(), getZ(), AutarkiaSounds.of("item.grapple.attach"), SoundSource.PLAYERS, 1f, 1.0f);
        }
        if (owner instanceof LivingEntity le && le.getUseItem().getItem() instanceof GrappleLauncherItem g) {
            g.playReel(le, le.getUseItem());
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}

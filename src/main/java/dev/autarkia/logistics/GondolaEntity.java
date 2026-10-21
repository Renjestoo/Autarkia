package dev.autarkia.logistics;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.storage.FallingCrateEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

/**
 * Cable car gondola (04 section 1.5): a GeckoLib entity that the owner station of its line places on the loop every
 * tick ({@code noPhysics}, no gravity). It holds 27 slots of cargo and swings as two pendulums, along and across the
 * cable: {@code alpha = -(g / L) sin(theta) - 0.08 omega + k w_axis} with g 0.08, L 2.5, k = {@code gondolaWindFactor}
 * and theta capped at 60 degrees, from the FrozenLib wind at the cabin. Only the server integrates; it syncs the two
 * angles every 2 ticks and the client eases toward them. A detached gondola falls with gravity and lands as a
 * {@code crate} block with its cargo (04 section 2.3.5); its parts (4 steel plates, 2 iron chains) drop as items.
 */
public class GondolaEntity extends Entity implements GeoEntity, JadeInfo {
    public static final int SLOTS = 27;
    public static final double G = 0.08;
    public static final double DAMPING = 0.08;
    public static final double LIMIT = Math.toRadians(60);
    private static final EntityDataAccessor<Float> SWING_ALONG = SynchedEntityData.defineId(GondolaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SWING_ACROSS = SynchedEntityData.defineId(GondolaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CARGO = SynchedEntityData.defineId(GondolaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FALLING = SynchedEntityData.defineId(GondolaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.gondola.idle");
    private static final RawAnimation DOORS = RawAnimation.begin().thenPlay("animation.gondola.doors_open");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemStacksResourceHandler cargo = new ItemStacksResourceHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            entityData.set(CARGO, usedSlots());
        }
    };
    private @Nullable BlockPos owner;
    private double thetaAlong;
    private double omegaAlong;
    private double thetaAcross;
    private double omegaAcross;
    private double tripSwing;
    private int fallTicks;
    private float clientAlong;
    private float clientAlongO;
    private float clientAcross;
    private float clientAcrossO;

    public GondolaEntity(EntityType<? extends GondolaEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Spawn a gondola of the line owned by the station at owner, hanging from the given cable point. */
    public static GondolaEntity spawn(ServerLevel level, BlockPos owner, Vec3 hang, float yaw) {
        GondolaEntity g = new GondolaEntity(AutarkiaEntities.GONDOLA.get(), level);
        g.owner = owner.immutable();
        g.place(hang, yaw);
        level.addFreshEntity(g);
        return g;
    }

    /** Return the parts one gondola costs and refunds: 4 steel plates and 2 iron chains. */
    public static List<ItemStack> parts() {
        return List.of(new ItemStack(AutarkiaItems.STEEL_PLATE.get(), 4), new ItemStack(Items.IRON_CHAIN, 2));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(SWING_ALONG, 0f);
        b.define(SWING_ACROSS, 0f);
        b.define(CARGO, 0);
        b.define(FALLING, false);
    }

    /** Return the owner station centre, or null for a detached gondola. */
    public @Nullable BlockPos owner() {
        return owner;
    }

    /** Return the cargo slots (server). */
    public ItemStacksResourceHandler cargo() {
        return cargo;
    }

    /** Return the number of occupied cargo slots (synced). */
    public int usedSlots() {
        if (level().isClientSide()) {
            return entityData.get(CARGO);
        }
        int n = 0;
        for (int i = 0; i < SLOTS; i++) {
            n += cargo.getAmountAsInt(i) > 0 ? 1 : 0;
        }
        return n;
    }

    /** Return true when no cargo slot is free. */
    public boolean full() {
        return usedSlots() >= SLOTS;
    }

    /** Return true when the gondola carries nothing. */
    public boolean emptyCargo() {
        return usedSlots() == 0;
    }

    /** Return true while the gondola falls after its line broke. */
    public boolean falling() {
        return entityData.get(FALLING);
    }

    /** Hang the gondola from a cable point: its floor 2.5 blocks under the point, facing along the line. */
    public void place(Vec3 hang, float yaw) {
        setPos(hang.x, hang.y - CableGeometry.HANG, hang.z);
        setYRot(yaw);
    }

    /** Return the swing angle along the cable in radians (server). */
    public double thetaAlong() {
        return thetaAlong;
    }

    /** Return the swing angle across the cable in radians (server). */
    public double thetaAcross() {
        return thetaAcross;
    }

    /** Return the combined swing angle in degrees (server: live value, client: last synced value). */
    public double swingDegrees() {
        if (level().isClientSide()) {
            return syncedSwingDegrees();
        }
        return Math.toDegrees(Math.sqrt(thetaAlong * thetaAlong + thetaAcross * thetaAcross));
    }

    /** Return the combined swing angle in degrees that clients last received. */
    public double syncedSwingDegrees() {
        float a = entityData.get(SWING_ALONG);
        float c = entityData.get(SWING_ACROSS);
        return Math.sqrt(a * a + c * c);
    }

    /** Return the largest swing in degrees since the gondola last left a station. */
    public double tripSwing() {
        return tripSwing;
    }

    /** Start a new trip record of the largest swing. */
    public void resetTrip() {
        tripSwing = swingDegrees();
    }

    /**
     * Integrate both pendulums for one tick from the wind at the cabin. When the swung cabin would touch a solid block
     * that {@code ignore} does not accept, the swing keeps its previous angles, both angular speeds drop to 0 and the
     * method returns that block (a strike); otherwise it returns null.
     */
    public @Nullable BlockPos swing(ServerLevel sl, CableGeometry geo, Vec3 hang, Predicate<BlockState> ignore) {
        Vec3 w = LaunchArc.wind(sl, position().add(0, CableGeometry.CABIN_HEIGHT / 2, 0));
        double k = AutarkiaConfig.GONDOLA_WIND_FACTOR.get();
        double wAlong = w.x * geo.dir.x + w.z * geo.dir.z;
        double wAcross = w.x * geo.right.x + w.z * geo.right.z;
        double na = omegaAlong - (G / CableGeometry.HANG) * Math.sin(thetaAlong) - DAMPING * omegaAlong + k * wAlong;
        double nc = omegaAcross - (G / CableGeometry.HANG) * Math.sin(thetaAcross) - DAMPING * omegaAcross + k * wAcross;
        double ta = thetaAlong + na;
        double tc = thetaAcross + nc;
        if (Math.abs(ta) > LIMIT) {
            ta = Math.copySign(LIMIT, ta);
            na = 0;
        }
        if (Math.abs(tc) > LIMIT) {
            tc = Math.copySign(LIMIT, tc);
            nc = 0;
        }
        BlockPos hit = blocked(sl, geo.cabin(hang, ta, tc), ignore);
        if (hit != null) {
            omegaAlong = 0;
            omegaAcross = 0;
            return hit;
        }
        thetaAlong = ta;
        thetaAcross = tc;
        omegaAlong = na;
        omegaAcross = nc;
        tripSwing = Math.max(tripSwing, swingDegrees());
        return null;
    }

    /** Return the first block with a collision shape inside the box that {@code ignore} does not accept, or null. */
    public static @Nullable BlockPos blocked(ServerLevel sl, AABB box, Predicate<BlockState> ignore) {
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            BlockState st = sl.getBlockState(p);
            if (st.isAir() || ignore.test(st)) {
                continue;
            }
            var shape = st.getCollisionShape(sl, p);
            if (!shape.isEmpty() && Shapes.joinIsNotEmpty(shape.move(p), Shapes.create(box), net.minecraft.world.phys.shapes.BooleanOp.AND)) {
                return p.immutable();
            }
        }
        return null;
    }

    /** Copy the swing angles into the synced data (the owner calls this every 2 ticks). */
    public void syncSwing() {
        entityData.set(SWING_ALONG, (float) Math.toDegrees(thetaAlong));
        entityData.set(SWING_ACROSS, (float) Math.toDegrees(thetaAcross));
    }

    /** Return the client swing along the cable in degrees at the partial tick. */
    public float clientAlong(float partialTick) {
        return Mth.lerp(partialTick, clientAlongO, clientAlong);
    }

    /** Return the client swing across the cable in degrees at the partial tick. */
    public float clientAcross(float partialTick) {
        return Mth.lerp(partialTick, clientAcrossO, clientAcross);
    }

    /** Play the door animation on tracking clients (the gondola unloads at a station). */
    public void openDoors() {
        triggerAnim("main", "doors_open");
    }

    /** Let the gondola fall: its line broke. */
    public void detach() {
        owner = null;
        noPhysics = false;
        setNoGravity(false);
        entityData.set(FALLING, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientAlongO = clientAlong;
            clientAcrossO = clientAcross;
            clientAlong += (entityData.get(SWING_ALONG) - clientAlong) * 0.5f;
            clientAcross += (entityData.get(SWING_ACROSS) - clientAcross) * 0.5f;
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        if (falling()) {
            fall(sl);
            return;
        }
        if (tickCount % 100 == 0 && !attached(sl)) {
            Autarkia.LOG.info("[gondola] line of {} is gone; the gondola at {} falls", owner == null ? "none" : owner.toShortString(), blockPosition().toShortString());
            detach();
        }
    }

    private boolean attached(ServerLevel sl) {
        if (owner == null) {
            return false;
        }
        if (!sl.isLoaded(owner)) {
            return true;
        }
        return sl.getBlockEntity(owner) instanceof CableCarStationBlockEntity s && s.line() != null && s.line().holds(getUUID());
    }

    private void fall(ServerLevel sl) {
        Vec3 v = getDeltaMovement().add(0, -0.04, 0);
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        setDeltaMovement(getDeltaMovement().scale(0.98));
        fallTicks++;
        if (getY() < sl.getMinY() - 16) {
            Autarkia.LOG.info("[gondola] fell below the world at {}; {} cargo stacks lost", blockPosition().toShortString(), usedSlots());
            discard();
            return;
        }
        if (isInLava()) {
            FallingCrateEntity.spill(sl, blockPosition(), cargoCrate(), position());
            dropParts(sl);
            discard();
            return;
        }
        if (onGround() || fallTicks >= 600) {
            if (!emptyCargo()) {
                FallingCrateEntity.land(sl, blockPosition(), cargoCrate(), position());
            }
            dropParts(sl);
            discard();
        }
    }

    /** Return a crate item that holds the cargo. */
    public ItemStack cargoCrate() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            list.add(cargo.getResource(i).toStack(cargo.getAmountAsInt(i)));
        }
        ItemStack crate = new ItemStack(AutarkiaItems.CRATE.get());
        crate.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
        return crate;
    }

    private void dropParts(ServerLevel sl) {
        for (ItemStack s : parts()) {
            Containers.dropItemStack(sl, getX(), getY() + 0.5, getZ(), s);
        }
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
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.gondola", usedSlots(), Math.round(swingDegrees())));
    }

    @Override
    public float jadeProgress() {
        return (float) Math.min(1, swingDegrees() / 60.0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<GondolaEntity> main = new AnimationController<>("main", 0, test -> test.setAndContinue(IDLE));
        main.triggerableAnim("doors_open", DOORS);
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        in.child("cargo").ifPresent(cargo::deserialize);
        owner = in.read("owner", BlockPos.CODEC).orElse(null);
        thetaAlong = in.getDoubleOr("thetaAlong", 0);
        omegaAlong = in.getDoubleOr("omegaAlong", 0);
        thetaAcross = in.getDoubleOr("thetaAcross", 0);
        omegaAcross = in.getDoubleOr("omegaAcross", 0);
        tripSwing = in.getDoubleOr("tripSwing", 0);
        fallTicks = in.getIntOr("fallTicks", 0);
        entityData.set(CARGO, usedSlots());
        if (in.getBooleanOr("falling", false)) {
            detach();
        }
        syncSwing();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        cargo.serialize(out.child("cargo"));
        if (owner != null) {
            out.store("owner", BlockPos.CODEC, owner);
        }
        out.putDouble("thetaAlong", thetaAlong);
        out.putDouble("omegaAlong", omegaAlong);
        out.putDouble("thetaAcross", thetaAcross);
        out.putDouble("omegaAcross", omegaAcross);
        out.putDouble("tripSwing", tripSwing);
        out.putInt("fallTicks", fallTicks);
        out.putBoolean("falling", falling());
    }
}

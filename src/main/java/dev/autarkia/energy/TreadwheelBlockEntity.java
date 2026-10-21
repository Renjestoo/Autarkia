package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.Ticking;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Treadwheel, 3x3x1 (02 section 6.2): one walker on the inner floor drives it. Horse, donkey, mule, llama 20 FE/t,
 * villager 16, cow, pig, sheep, goat 12, a Porter 24. Use a lead on the wheel while leading an eligible mob; the lead
 * stays in the wheel. Sneak + use with an empty hand returns the walker on your lead. Buffer 4,000 FE, out 40 FE/t.
 */
public class TreadwheelBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo, MuscleGenerator,
    dev.autarkia.colony.ColonySensors.Member {
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.treadwheel.spin");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.treadwheel.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(4000, 0, 40, this::setChanged);
    private @Nullable UUID walker;
    private int walkerId = -1;
    private boolean hasLead;
    private int missing;
    private int output;
    private long workerUntil;

    public TreadwheelBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.TREADWHEEL.get(), pos, state);
    }

    @Override
    public int workerOutput() {
        return 24;
    }

    @Override
    public boolean storageFull() {
        return buffer.room() <= 0;
    }

    @Override
    public java.util.Set<dev.autarkia.colony.ColonySensors.Kind> colonyKinds() {
        return walker == null ? java.util.Set.of(dev.autarkia.colony.ColonySensors.Kind.GENERATOR) : java.util.Set.of();
    }

    /** Return the FE/t a walker type gives, or 0 when the type cannot walk the wheel. */
    public static int outputFor(EntityType<?> type) {
        if (type == EntityTypes.HORSE || type == EntityTypes.DONKEY || type == EntityTypes.MULE || type == EntityTypes.LLAMA) {
            return 20;
        }
        if (type == EntityTypes.VILLAGER) {
            return 16;
        }
        if (type == EntityTypes.COW || type == EntityTypes.PIG || type == EntityTypes.SHEEP || type == EntityTypes.GOAT) {
            return 12;
        }
        return 0;
    }

    /** Return the walker position: the inner floor of the bottom center, 0.125 up. */
    public Vec3 floor() {
        return Vec3.atBottomCenterOf(worldPosition).add(0, 0.125, 0);
    }

    /** Return the client-side id of the walker entity, or -1. */
    public int walkerId() {
        return walkerId;
    }

    /** Return the FE/t made. */
    public int output() {
        return output;
    }

    /** Set whether a worker drives the wheel this tick (02 section 2.7); a Porter gives 24 FE/t. */
    public void setWorkerDriving(Entity worker, boolean driving) {
        workerUntil = driving ? level.getGameTime() + 1 : 0;
    }

    /** Put a mob led by the player into the wheel; returns false when it is full or nothing eligible is led. */
    public boolean enter(Player player) {
        if (walker != null) {
            return false;
        }
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(10))) {
            if (mob.getLeashHolder() == player && outputFor(mob.getType()) > 0) {
                mob.removeLeash();
                mob.setNoAi(true);
                walker = mob.getUUID();
                walkerId = mob.getId();
                hasLead = true;
                place(mob);
                syncNow();
                return true;
            }
        }
        return false;
    }

    /** Return the walker to the player's lead. */
    public boolean release(Player player) {
        Mob mob = walkerMob();
        if (mob == null) {
            return false;
        }
        mob.setNoAi(false);
        mob.setLeashedTo(player, true);
        hasLead = false;
        clearWalker();
        return true;
    }

    private void clearWalker() {
        walker = null;
        walkerId = -1;
        missing = 0;
        syncNow();
    }

    private @Nullable Mob walkerMob() {
        return walker != null && level instanceof ServerLevel sl && sl.getEntity(walker) instanceof Mob m && m.isAlive() ? m : null;
    }

    private void place(Mob mob) {
        Vec3 f = floor();
        Direction front = MachineBlock.front(getBlockState());
        float yaw = front.getClockWise().toYRot();
        mob.snapTo(f.x, f.y, f.z, yaw, 0);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void serverTick() {
        int next = 0;
        if (walker != null) {
            Mob mob = walkerMob();
            if (mob == null) {
                if (++missing >= 40) {
                    if (hasLead) {
                        Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, new ItemStack(Items.LEAD));
                        hasLead = false;
                    }
                    clearWalker();
                }
            } else {
                missing = 0;
                place(mob);
                next = outputFor(mob.getType());
                if (mob.getId() != walkerId) {
                    walkerId = mob.getId();
                    syncNow();
                }
            }
        }
        if (level.getGameTime() <= workerUntil) {
            next = 24;
        }
        if (next != output) {
            output = next;
            syncNow();
        }
        if (output > 0) {
            buffer.fill(output);
        }
    }

    @Override
    public void clientTick() {
        if (walkerId >= 0 && level.getEntity(walkerId) instanceof LivingEntity le) {
            le.walkAnimation.update(0.5f * output / 20f, 0.4f, 1f);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel && isCenter()) {
            Mob mob = walkerMob();
            if (mob != null) {
                mob.setNoAi(false);
            }
            if (hasLead) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.LEAD));
            }
        }
    }

    @Override
    public Role gridRole() {
        return Role.GENERATOR;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return buffer;
    }

    @Override
    public int gridMaxOut() {
        return 40;
    }

    @Override
    public int gridMaxIn() {
        return 0;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    /** Return the buffer (extract only). */
    public EnergyBuffer buffer() {
        return buffer;
    }

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.output", output));
        out.add(Component.translatable("jade.autarkia.buffer", buffer.stored(), buffer.capacity()));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<TreadwheelBlockEntity>("wheel", 4, test -> {
            int out = test.animatable().output;
            test.setControllerSpeed(Math.max(0.1f, out / 24f));
            return test.setAndContinue(out > 0 ? SPIN : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
        if (walker != null) {
            out.putString("walker", walker.toString());
        }
        out.putInt("walkerId", walkerId);
        out.putBoolean("lead", hasLead);
        out.putInt("output", output);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        walker = in.getString("walker").map(UUID::fromString).orElse(null);
        walkerId = in.getIntOr("walkerId", -1);
        hasLead = in.getBooleanOr("lead", false);
        output = in.getIntOr("output", 0);
    }
}

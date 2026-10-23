package dev.autarkia.worker;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.colony.ColonySensors;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.Ticking;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The {@code recharge_dock} (06 sections 5.6 and 5.7): a 50,000 FE buffer fed by any cable charges one docked FE
 * worker at up to {@code dockChargeRate} FE/t (default 2,000) and heals it 1 health per 100 ticks for 500 FE. Drones
 * land on top, ground workers stand next to it. Two glow particles in the worker's lamp color per 10 ticks while it
 * charges.
 */
public class RechargeDockBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, JadeInfo, ColonySensors.Member, WorkerCharger, GeoBlockEntity {
    public static final int CAPACITY = 50000;
    public static final int HEAL_COST = 500;
    private static final RawAnimation CHARGE = RawAnimation.begin().thenLoop("animation.recharge_dock.charge");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.recharge_dock.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(CAPACITY, Integer.MAX_VALUE, 0, this::setChanged);
    private @Nullable UUID docked;
    private boolean charging;
    private boolean shownCharging;

    public RechargeDockBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.RECHARGE_DOCK.get(), pos, state);
    }

    private static int rate() {
        return AutarkiaConfig.DOCK_CHARGE_RATE.get();
    }

    /** Return the energy buffer. */
    public EnergyBuffer buffer() {
        return buffer;
    }

    @Override
    public boolean serves(WorkerEntity w) {
        return w.workerType.fe();
    }

    @Override
    public boolean availableFor(UUID worker) {
        return docked == null || docked.equals(worker) || !(level instanceof ServerLevel sl && sl.getEntity(docked) instanceof WorkerEntity);
    }

    @Override
    public boolean dock(WorkerEntity w) {
        if (!availableFor(w.getUUID())) {
            return false;
        }
        docked = w.getUUID();
        return true;
    }

    @Override
    public void undock(WorkerEntity w) {
        if (w.getUUID().equals(docked)) {
            docked = null;
            charging = false;
        }
    }

    @Override
    public BlockPos dockSpot(WorkerEntity w) {
        if (w.workerType.flying()) {
            return worldPosition.above();
        }
        Direction d = Direction.getApproximateNearest(w.getX() - (worldPosition.getX() + 0.5), 0, w.getZ() - (worldPosition.getZ() + 0.5));
        return worldPosition.relative(d.getAxis().isHorizontal() ? d : Direction.SOUTH);
    }

    @Override
    public double dockReach() {
        return 1.8;
    }

    /** Return true while the dock charges or heals a docked worker (client: from the update tag). */
    public boolean charging() {
        return charging;
    }

    @Override
    public void serverTick() {
        charge();
        if (charging != shownCharging) {
            shownCharging = charging;
            syncNow();
        }
    }

    private void charge() {
        ServerLevel sl = serverLevel();
        WorkerEntity w = docked == null ? null : sl.getEntity(docked) instanceof WorkerEntity we ? we : null;
        charging = false;
        boolean mine = w != null && w.getBrain().getMemory(WorkerMemories.CHARGER_POS.get()).map(net.minecraft.core.GlobalPos::pos)
            .filter(worldPosition::equals).isPresent();
        if (w == null || !mine || w.position().distanceToSqr(Vec3.atCenterOf(worldPosition)) > 2.6 * 2.6) {
            if (w == null || !mine || w.state() != WorkerEntity.State.RECHARGING) {
                docked = null;
            }
            return;
        }
        long now = sl.getGameTime();
        int give = Math.min(rate(), Math.min(buffer.stored(), w.workerType.capacity() - w.energy()));
        if (give > 0) {
            buffer.drain(give);
            w.setEnergy(w.energy() + give);
            charging = true;
        }
        if (now % 100 == 0 && w.getHealth() < w.getMaxHealth() && buffer.stored() >= HEAL_COST) {
            buffer.drain(HEAL_COST);
            w.heal(1);
            charging = true;
        }
        if (charging && now % 10 == 0) {
            Vec3 top = Vec3.atCenterOf(worldPosition).add(0, 0.6, 0);
            sl.sendParticles(GlowParticleOptions.of(w.workerType.lamp()).size(0.15f), top.x, top.y, top.z, 2, 0.25, 0.05, 0.25, 0);
        }
        if (charging && now % 40 == 0) {
            w.sound("entity.worker.recharge", 0.5f, 1.4f);
        }
    }

    @Override
    public Set<ColonySensors.Kind> colonyKinds() {
        return Set.of(ColonySensors.Kind.CHARGER);
    }

    @Override
    public Role gridRole() {
        return Role.CONSUMER;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return buffer;
    }

    @Override
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int gridMaxIn() {
        return buffer.room() > 0 ? rate() : 0;
    }

    @Override
    public int workDraw() {
        return charging ? rate() : 0;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(List<Component> out) {
        out.add(Component.translatable("jade.autarkia.buffer", buffer.stored(), buffer.capacity()));
        out.add(Component.translatable(docked == null ? "jade.autarkia.post.free" : charging ? "jade.autarkia.dock.charging" : "jade.autarkia.post.docked"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RechargeDockBlockEntity>("main", 10, test -> test.setAndContinue(test.animatable().charging() ? CHARGE : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
        if (docked != null) {
            out.store("docked", UUIDUtil.CODEC, docked);
        }
        out.putBoolean("charging", charging);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        docked = in.read("docked", UUIDUtil.CODEC).orElse(null);
        charging = in.getBooleanOr("charging", false);
    }
}

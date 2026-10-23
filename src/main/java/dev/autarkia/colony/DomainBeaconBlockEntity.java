package dev.autarkia.colony;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.Ticking;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Domain beacon block entity (05 section 1.3): draws 32 FE/t; with power its square is ACTIVE, without power it stays in
 * the domain index as INACTIVE (the planner ignores it). Removing it removes its square from the colony.
 */
public class DomainBeaconBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, JadeInfo, GeoBlockEntity {
    public static final int FE_PER_TICK = 32;
    private static final RawAnimation TURN = RawAnimation.begin().thenLoop("animation.domain_beacon.idle");
    private static final RawAnimation OFF = RawAnimation.begin().thenLoop("animation.domain_beacon.off");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(FE_PER_TICK * 200, FE_PER_TICK * 4, 0, this::setChanged);
    private boolean active;

    public DomainBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.DOMAIN_BEACON.get(), pos, state);
    }

    private @Nullable Colony colony() {
        if (Colonies.server() == null) {
            return null;
        }
        for (Colony c : Colonies.registry().all()) {
            if (c.dimension.equals(level.dimension()) && c.beacons.stream().anyMatch(b -> b.pos().equals(worldPosition))) {
                return c;
            }
        }
        return null;
    }

    /** Return whether the beacon had power at its last tick. */
    public boolean active() {
        return active;
    }

    @Override
    public void serverTick() {
        boolean now = buffer.drain(FE_PER_TICK) == FE_PER_TICK;
        if (now == active && serverLevel().getGameTime() % 100 != 0) {
            return;
        }
        if (now != active) {
            active = now;
            syncNow();
        }
        Colony c = colony();
        if (c == null) {
            return;
        }
        for (int i = 0; i < c.beacons.size(); i++) {
            Colony.Beacon b = c.beacons.get(i);
            if (b.pos().equals(worldPosition) && b.active() != active) {
                c.beacons.set(i, new Colony.Beacon(b.pos(), active));
                Colonies.changed();
                syncNow();
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel) {
            Colony c = colony();
            if (c != null && c.beacons.removeIf(b -> b.pos().equals(pos))) {
                Colonies.rebuildIndex();
                Colonies.changed();
            }
        }
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
        return buffer.room() > 0 ? FE_PER_TICK * 4 : 0;
    }

    @Override
    public int workDraw() {
        return FE_PER_TICK;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "colony";
    }

    @Override
    public void jadeLines(List<Component> out) {
        Colony c = level instanceof ServerLevel ? colony() : null;
        out.add(Component.translatable("jade.autarkia.beacon.domain", c == null ? 0 : ColonyTiers.beaconHalf(c.tier)));
        out.add(Component.translatable(active ? "jade.autarkia.beacon.power_ok" : "jade.autarkia.beacon.power_off"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DomainBeaconBlockEntity>("main", 20, test -> test.setAndContinue(test.animatable().active() ? TURN : OFF)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
        out.putBoolean("active", active);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        active = in.getBooleanOr("active", false);
    }
}

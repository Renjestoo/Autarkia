package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaAdvancements;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.Ticking;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Stone flywheel storage, 3x3x1 (02 section 6.2, W3): 40,000 FE, omega = sqrt(E / 40,000), self-loss 0.5 + 0.00005 E
 * per tick, I/O 40 x max(0.25, omega). Removing it with E >= flywheelReleaseMinFE releases a rolling flywheel instead of
 * dropping the block.
 */
public class StoneFlywheelBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo {
    public static final int CAPACITY = 40000;
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.stone_flywheel.spin");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.stone_flywheel.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(CAPACITY, 1024, 1024, this::setChanged);
    private double loss;
    private int lastSyncedSpin = -1;
    private boolean released;

    public StoneFlywheelBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.STONE_FLYWHEEL.get(), pos, state);
    }

    /** Return the spin omega 0..1. */
    public double omega() {
        return Math.sqrt(buffer.stored() / (double) CAPACITY);
    }

    /** Return the stored FE. */
    public int stored() {
        return buffer.stored();
    }

    /** Return the buffer. */
    public EnergyBuffer buffer() {
        return buffer;
    }

    /** Return true when breaking now releases a rolling flywheel. */
    public boolean releases() {
        return buffer.stored() >= AutarkiaConfig.FLYWHEEL_RELEASE_MIN_FE.get();
    }

    @Override
    public void serverTick() {
        loss += 0.5 + 0.00005 * buffer.stored();
        if (loss >= 1) {
            buffer.drain((int) loss);
            loss -= (int) loss;
        }
        int spin = (int) Math.round(omega() * 20);
        if (spin != lastSyncedSpin) {
            lastSyncedSpin = spin;
            syncNow();
        }
        if (omega() > 0.05 && level.getGameTime() % 40 == 0) {
            level.playSound(null, worldPosition.above(), AutarkiaSounds.of("flywheel.spin"), SoundSource.BLOCKS, 0.4f, (float) (0.5 + omega()));
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel sl && isCenter() && !released && releases()) {
            released = true;
            RollingFlywheelEntity e = RollingFlywheelEntity.create(sl, Vec3.atBottomCenterOf(pos), MachineBlock.front(state), buffer.stored());
            sl.addFreshEntity(e);
            AutarkiaAdvancements.awardNear(sl, Vec3.atCenterOf(pos), "power/rolling_wheel");
        }
    }

    @Override
    public Role gridRole() {
        return Role.STORAGE;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return buffer;
    }

    @Override
    public int gridMaxOut() {
        return (int) Math.round(40 * Math.max(0.25, omega()));
    }

    @Override
    public int gridMaxIn() {
        return gridMaxOut();
    }

    @Override
    public int gridPriority() {
        return 3;
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
        out.add(Component.translatable("jade.autarkia.spin", Math.round(omega() * 100)));
        out.add(Component.translatable("jade.autarkia.stored", buffer.stored(), CAPACITY, Math.round(100f * buffer.stored() / CAPACITY)));
        out.add(Component.translatable("jade.autarkia.io", gridMaxIn(), gridMaxOut()));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<StoneFlywheelBlockEntity>("disc", 4, test -> {
            double w = test.animatable().omega();
            test.setControllerSpeed((float) Math.max(0.05, w));
            return test.setAndContinue(w > 0.02 ? SPIN : IDLE);
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
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
    }
}

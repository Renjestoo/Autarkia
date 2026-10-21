package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.registry.Ticking;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Windmill, 1x4x1 with the rotor on the top block (02 section 6.2): out = min(40, round(base x W x M x obstruction)),
 * base = clamp(4 + (yRotor - 62) x 0.4, 4, 32), W = clamp(0.5 + windStrength, 0.5, 1.5), M = 1.0 / 1.15 rain /
 * 1.3 thunder above the rotor, obstruction -10% per solid block in the 5 x 5 plane in front of the rotor (0 at 5+).
 * Recomputed every 20 ticks. Buffer 2,000 FE, out 40 FE/t.
 */
public class WindmillBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo {
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.windmill.spin");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.windmill.idle");
    private static final RawAnimation[] WOBBLE = {
        RawAnimation.begin().thenLoop("animation.windmill.wobble2"),
        RawAnimation.begin().thenLoop("animation.windmill.wobble4"),
        RawAnimation.begin().thenLoop("animation.windmill.wobble6")};
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(2000, 0, 40, this::setChanged);
    private int output;
    private int blocked;
    private float wind;
    private boolean windData = true;

    public WindmillBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.WINDMILL.get(), pos, state);
    }

    /** Return the rotor block position (top of the tower). */
    public BlockPos rotor() {
        return worldPosition.above(3);
    }

    /** Return the base output before wind, weather and obstruction for a rotor height. */
    public static double base(int yRotor) {
        return Mth.clamp(4 + (yRotor - 62) * 0.4, 4, 32);
    }

    /** Return the weather multiplier at the rotor. */
    public double weather() {
        BlockPos above = rotor().above();
        if (!level.isRainingAt(above)) {
            return 1.0;
        }
        return level.isThundering() ? 1.3 : 1.15;
    }

    /** Return the number of collision blocks in the 5 x 5 plane in front of the rotor. */
    public int obstruction() {
        Direction front = MachineBlock.front(getBlockState());
        BlockPos c = rotor().relative(front);
        Direction side = front.getClockWise();
        int n = 0;
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                BlockPos p = c.relative(side, a).above(b);
                if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                    n++;
                }
            }
        }
        return n;
    }

    /** Recompute the output now. */
    public void recompute() {
        Vec3 r = Vec3.atCenterOf(rotor());
        windData = Wind.available(level);
        wind = (float) Wind.strength(level, r);
        blocked = obstruction();
        double w = Mth.clamp(0.5 + wind, 0.5, 1.5);
        double obs = blocked >= 5 ? 0 : 1 - 0.1 * blocked;
        int next = (int) Math.min(40, Math.round(base(rotor().getY()) * w * weather() * obs));
        if (next != output) {
            output = next;
            syncNow();
        }
    }

    /** Return the FE/t made. */
    public int output() {
        return output;
    }

    /** Return the last sampled wind strength. */
    public float wind() {
        return wind;
    }

    @Override
    public void serverTick() {
        long now = level.getGameTime();
        if (now % 20 == Math.floorMod(worldPosition.asLong(), 20)) {
            recompute();
        }
        if (output > 0) {
            buffer.fill(output);
            if (now % 80 == 0) {
                level.playSound(null, rotor(), AutarkiaSounds.of("windmill.creak"), SoundSource.BLOCKS, 0.4f, 0.5f);
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
        out.add(windData ? Component.translatable("jade.autarkia.wind", Math.round(wind * 100)) : Component.translatable("jade.autarkia.wind_none"));
        out.add(Component.translatable("jade.autarkia.blocked", blocked));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<WindmillBlockEntity>("rotor", 4, test -> {
            WindmillBlockEntity be = test.animatable();
            test.setControllerSpeed(Math.max(0.2f, be.output / 20f));
            return test.setAndContinue(be.output > 0 ? SPIN : IDLE);
        }));
        controllers.add(new AnimationController<WindmillBlockEntity>("wobble", 10, test -> {
            float w = test.animatable().wind;
            return test.setAndContinue(w < 0.75f ? WOBBLE[0] : w < 1.25f ? WOBBLE[1] : WOBBLE[2]);
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
        out.putInt("output", output);
        out.putFloat("wind", wind);
        out.putInt("blocked", blocked);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        output = in.getIntOr("output", 0);
        wind = in.getFloatOr("wind", 0);
        blocked = in.getIntOr("blocked", 0);
    }
}

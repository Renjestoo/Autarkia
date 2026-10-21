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
 * Wind tower, 1x8x1 with the rotor on the top block (02 section 6.2): out = min(4,096, round(base x W x M x
 * obstruction)), base = clamp(512 + (yRotor - 64) x 20, 512, 3,072), W = clamp(0.5 + windStrength, 0.5, 1.5), M = 1.0 /
 * 1.15 rain / 1.3 thunder above the rotor, obstruction -3% per block with a collision shape in the 9 x 9 plane one
 * block in front of the rotor (0 at 25 or more). Recomputed every 20 ticks. Buffer 40,000 FE, out 8,192 FE/t. The
 * nacelle wobbles by +-4 deg x windStrength (02 section 4.5).
 */
public class WindTowerBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo {
    public static final int HEIGHT = 8;
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.wind_tower.spin");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.wind_tower.idle");
    private static final RawAnimation[] WOBBLE = {
        RawAnimation.begin().thenLoop("animation.wind_tower.wobble2"),
        RawAnimation.begin().thenLoop("animation.wind_tower.wobble4"),
        RawAnimation.begin().thenLoop("animation.wind_tower.wobble6")};
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(40000, 0, 8192, this::setChanged);
    private int output;
    private int blocked;
    private float wind;
    private boolean windData = true;

    public WindTowerBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.WIND_TOWER.get(), pos, state);
    }

    /** Return the rotor block position (top of the tower). */
    public BlockPos rotor() {
        return worldPosition.above(HEIGHT - 1);
    }

    /** Return the base output before wind, weather and obstruction for a rotor height. */
    public static double base(int yRotor) {
        return Mth.clamp(512 + (yRotor - 64) * 20, 512, 3072);
    }

    /** Return the obstruction factor for a number of blocking blocks: 1 - 0.03 n, 0 at 25 or more. */
    public static double obstructionFactor(int blocked) {
        return blocked >= 25 ? 0 : 1 - 0.03 * blocked;
    }

    /** Return the weather multiplier at the rotor. */
    public double weather() {
        if (!level.isRainingAt(rotor().above())) {
            return 1.0;
        }
        return level.isThundering() ? 1.3 : 1.15;
    }

    /** Return the number of blocks with a collision shape in the 9 x 9 plane in front of the rotor. */
    public int obstruction() {
        Direction front = MachineBlock.front(getBlockState());
        BlockPos c = rotor().relative(front);
        Direction side = front.getClockWise();
        int n = 0;
        for (int a = -4; a <= 4; a++) {
            for (int b = -4; b <= 4; b++) {
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
        windData = Wind.available(level);
        float nextWind = (float) Wind.strength(level, Vec3.atCenterOf(rotor()));
        blocked = obstruction();
        double w = Mth.clamp(0.5 + nextWind, 0.5, 1.5);
        int next = (int) Math.min(4096, Math.round(base(rotor().getY()) * w * weather() * obstructionFactor(blocked)));
        boolean wobbleChanged = wobbleIndex(nextWind) != wobbleIndex(wind);
        wind = nextWind;
        if (next != output || wobbleChanged) {
            output = next;
            syncNow();
        } else {
            setChanged();
        }
    }

    private static int wobbleIndex(float w) {
        return w < 0.75f ? 0 : w < 1.25f ? 1 : 2;
    }

    /** Return the FE/t made. */
    public int output() {
        return output;
    }

    /** Return the last sampled wind strength. */
    public float wind() {
        return wind;
    }

    /** Return the blocking blocks counted at the last recompute. */
    public int blocked() {
        return blocked;
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
                level.playSound(null, rotor(), AutarkiaSounds.of("windmill.creak"), SoundSource.BLOCKS, 0.6f, 0.35f);
            }
        }
    }

    /** Return the buffer (extract only). */
    public EnergyBuffer buffer() {
        return buffer;
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
        return 8192;
    }

    @Override
    public int gridMaxIn() {
        return 0;
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
        out.add(Component.translatable("jade.autarkia.output", output));
        out.add(Component.translatable("jade.autarkia.buffer", buffer.stored(), buffer.capacity()));
        out.add(windData ? Component.translatable("jade.autarkia.wind", Math.round(wind * 100)) : Component.translatable("jade.autarkia.wind_none"));
        out.add(Component.translatable("jade.autarkia.blocked", blocked));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<WindTowerBlockEntity>("rotor", 4, test -> {
            WindTowerBlockEntity be = test.animatable();
            test.setControllerSpeed(Math.max(0.2f, be.output / 2048f));
            return test.setAndContinue(be.output > 0 ? SPIN : IDLE);
        }));
        controllers.add(new AnimationController<WindTowerBlockEntity>("wobble", 10,
            test -> test.setAndContinue(WOBBLE[wobbleIndex(test.animatable().wind)])));
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

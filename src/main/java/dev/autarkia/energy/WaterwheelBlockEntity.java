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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Waterwheel, 3 long (along its facing), 3 tall, 1 thick, axle across the facing, center = bottom middle (02 section 6.2,
 * 08a 1x3x3). U = signed count of the 3 blocks
 * under the bottom row with flowing water along the wheel plane; O = signed count of the 3 blocks above the top row
 * with falling or flowing water (falling water on a side turns the wheel away from that side). Sources count 0.
 * out = min(128, 24 |U| + 40 |O|); with opposite signs only |U + O| counts, weight 24. Recomputed every 20 ticks.
 * Buffer 4,000 FE, out 256 FE/t.
 */
public class WaterwheelBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo {
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.waterwheel.spin");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.waterwheel.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(4000, 0, 256, this::setChanged);
    private int output;
    private int direction;

    public WaterwheelBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.WATERWHEEL.get(), pos, state);
    }

    private Direction axis() {
        return MachineBlock.front(getBlockState());
    }

    private int flowSign(BlockPos p, int side) {
        FluidState f = level.getFluidState(p);
        if (!f.is(net.minecraft.tags.FluidTags.WATER) || f.isSource()) {
            return 0;
        }
        Vec3 flow = f.getFlow(level, p);
        Direction ax = axis();
        double along = flow.x * ax.getStepX() + flow.z * ax.getStepZ();
        if (Math.abs(along) > 1e-3) {
            return along > 0 ? 1 : -1;
        }
        boolean falling = f.hasProperty(net.minecraft.world.level.material.FlowingFluid.FALLING)
            && f.getValue(net.minecraft.world.level.material.FlowingFluid.FALLING);
        return falling ? -side : 0;
    }

    /** Recompute the output from the water around the wheel. */
    public void recompute() {
        Direction ax = axis();
        int u = 0;
        int o = 0;
        for (int i = -1; i <= 1; i++) {
            u += flowSign(worldPosition.relative(ax, i).below(), i);
            o += flowSign(worldPosition.relative(ax, i).above(3), i);
        }
        int next;
        if (u != 0 && o != 0 && Integer.signum(u) != Integer.signum(o)) {
            next = Math.min(128, 24 * Math.abs(u + o));
            direction = Integer.signum(u + o);
        } else {
            next = Math.min(128, 24 * Math.abs(u) + 40 * Math.abs(o));
            direction = Integer.signum(u != 0 ? u : o);
        }
        if (next != output) {
            output = next;
            syncNow();
        }
    }

    /** Return the FE/t made. */
    public int output() {
        return output;
    }

    @Override
    public void serverTick() {
        long now = level.getGameTime();
        if (now % 20 == Math.floorMod(worldPosition.asLong(), 20)) {
            recompute();
        }
        if (output > 0) {
            buffer.fill(output);
            if (now % 40 == 0) {
                level.playSound(null, worldPosition, AutarkiaSounds.of("waterwheel.splash"), SoundSource.BLOCKS, 0.3f, 0.9f);
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
        return 256;
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
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<WaterwheelBlockEntity>("wheel", 4, test -> {
            WaterwheelBlockEntity be = test.animatable();
            test.setControllerSpeed(be.direction * Math.max(0.2f, be.output / 64f));
            return test.setAndContinue(be.output > 0 ? SPIN : IDLE);
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
        out.putInt("dir", direction);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        output = in.getIntOr("output", 0);
        direction = in.getIntOr("dir", 0);
    }
}

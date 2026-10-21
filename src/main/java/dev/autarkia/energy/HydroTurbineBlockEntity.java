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
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Hydro turbine, 3x3x3 with the center at the bottom middle (02 section 6.2): head h = water blocks (source or flowing)
 * in the straight column above the top-center intake, at most 32; the outlet (the neighbour of the bottom-center
 * position of the front face) must be air or water, else the output is 0. out = min(4,096, 128 h) FE/t; the water is
 * not used up. Recomputed every 20 ticks. Buffer 50,000 FE, out 8,192 FE/t.
 */
public class HydroTurbineBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo {
    public static final int MAX_HEAD = 32;
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.hydro_turbine.spin");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.hydro_turbine.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(50000, 0, 8192, this::setChanged);
    private int output;
    private int head;

    public HydroTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.HYDRO_TURBINE.get(), pos, state);
    }

    /** Return the intake position (top center of the cube). */
    public BlockPos intake() {
        return worldPosition.above(2);
    }

    /** Return the outlet position: the block in front of the bottom-center position of the front face. */
    public BlockPos outlet() {
        return worldPosition.relative(MachineBlock.front(getBlockState()), 2);
    }

    /** Return the FE/t made for a head in blocks: min(4,096, 128 x head). */
    public static int outputFor(int head) {
        return Math.min(4096, 128 * head);
    }

    /** Return the number of water blocks in the column above the intake, at most 32. */
    public int measureHead() {
        int h = 0;
        BlockPos.MutableBlockPos p = intake().mutable();
        while (h < MAX_HEAD && level.getBlockState(p.move(0, 1, 0)).is(Blocks.WATER)) {
            h++;
        }
        return h;
    }

    /** Return true when the outlet is air or water. */
    public boolean outletOpen() {
        BlockState s = level.getBlockState(outlet());
        return s.isAir() || s.is(Blocks.WATER);
    }

    /** Recompute the head and the output now. */
    public void recompute() {
        int nextHead = measureHead();
        int next = outletOpen() ? outputFor(nextHead) : 0;
        if (next != output || nextHead != head) {
            output = next;
            head = nextHead;
            syncNow();
        }
    }

    /** Return the FE/t made. */
    public int output() {
        return output;
    }

    /** Return the last measured head in blocks. */
    public int head() {
        return head;
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
                level.playSound(null, outlet(), AutarkiaSounds.of("waterwheel.splash"), SoundSource.BLOCKS, 0.5f, 0.7f);
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
        out.add(Component.translatable("jade.autarkia.head", head));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<HydroTurbineBlockEntity>("runner", 4, test -> {
            HydroTurbineBlockEntity be = test.animatable();
            test.setControllerSpeed(Math.max(0.2f, be.output / 1024f));
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
        out.putInt("head", head);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        output = in.getIntOr("output", 0);
        head = in.getIntOr("head", 0);
    }
}

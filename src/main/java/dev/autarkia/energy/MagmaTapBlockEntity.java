package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.Ticking;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Magma tap, 1x2x1 (02 section 6.2). Its bottom position stands on a {@code magma_vent} (2,048 FE/t, endless) or on a
 * vanilla lava source (1,024 FE/t; after 24,000 running ticks the source turns into obsidian and the output stops).
 * Without a water source touching its upper block (sides or top) the output is halved and the top smokes. Checked
 * every tick. Sound {@code magma.bubble} every 40 ticks while running. Buffer 20,000 FE, out 8,192 FE/t.
 */
public class MagmaTapBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo {
    public static final int LAVA_TICKS = 24000;
    private static final RawAnimation PUMP = RawAnimation.begin().thenLoop("animation.magma_tap.pump");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.magma_tap.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(20000, 0, 8192, this::setChanged);
    private int output;
    private int lavaTicks;
    private boolean onLava;
    private boolean cooled;

    public MagmaTapBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.MAGMA_TAP.get(), pos, state);
    }

    /** Return the FE/t of the heat source under the tap before the water factor: 2,048 vent, 1,024 lava source, else 0. */
    public int sourceOutput() {
        BlockPos below = worldPosition.below();
        BlockState s = level.getBlockState(below);
        if (s.is(AutarkiaBlocks.MAGMA_VENT.get())) {
            return 2048;
        }
        return s.is(Blocks.LAVA) && level.getFluidState(below).isSource() ? 1024 : 0;
    }

    /** Return true when a water source touches the upper block on a side or on top. */
    public boolean waterCooled() {
        BlockPos top = worldPosition.above();
        for (Direction d : Direction.values()) {
            if (d != Direction.DOWN && level.getFluidState(top.relative(d)).isSourceOfType(Fluids.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void serverTick() {
        int src = sourceOutput();
        boolean wet = waterCooled();
        int next = wet ? src : src / 2;
        boolean lava = src == 1024;
        if (lava && next > 0 && ++lavaTicks >= LAVA_TICKS) {
            level.setBlockAndUpdate(worldPosition.below(), Blocks.OBSIDIAN.defaultBlockState());
            lavaTicks = 0;
            lava = false;
            next = 0;
        }
        if (next != output || lava != onLava || wet != cooled) {
            output = next;
            onLava = lava;
            cooled = wet;
            syncNow();
        } else if (lava) {
            setChanged();
        }
        if (output <= 0) {
            return;
        }
        buffer.fill(output);
        long now = level.getGameTime();
        if (now % 40 == 0) {
            level.playSound(null, worldPosition, AutarkiaSounds.of("magma.bubble"), SoundSource.BLOCKS, 0.8f, 0.6f);
        }
        if (!cooled && now % 10 == 0) {
            serverLevel().sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 2.0, worldPosition.getZ() + 0.5, 3,
                0.2, 0.05, 0.2, 0.01);
        }
    }

    /** Return the FE/t made. */
    public int output() {
        return output;
    }

    /** Return true while the tap runs on a vanilla lava source (which it uses up). */
    public boolean onLava() {
        return onLava;
    }

    /** Return the running ticks left before the lava source under the tap turns into obsidian. */
    public int lavaTicksLeft() {
        return LAVA_TICKS - lavaTicks;
    }

    /** Return true when water touched the upper block at the last tick. */
    public boolean cooled() {
        return cooled;
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
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MagmaTapBlockEntity>("pump", 4, test -> {
            MagmaTapBlockEntity be = test.animatable();
            test.setControllerSpeed(Math.max(0.3f, be.output / 2048f));
            return test.setAndContinue(be.output > 0 ? PUMP : IDLE);
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
        out.putInt("lava_ticks", lavaTicks);
        out.putBoolean("on_lava", onLava);
        out.putBoolean("cooled", cooled);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        output = in.getIntOr("output", 0);
        lavaTicks = in.getIntOr("lava_ticks", 0);
        onLava = in.getBooleanOr("on_lava", false);
        cooled = in.getBooleanOr("cooled", false);
    }
}

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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Steam engine, 1x2x2 (02 section 6.2): its back (intake) face touches a boiler. It draws up to 20 mB of steam per
 * tick while its 4,000 FE buffer has at least 120 FE of room and makes 6 FE per mB (max 120 FE/t). Out 256 FE/t.
 */
public class SteamEngineBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo {
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.steam_engine.run");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.steam_engine.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(4000, 0, 256, this::setChanged);
    private int steamRate;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.STEAM_ENGINE.get(), pos, state);
    }

    /** Return the boiler touching the intake face, or null. */
    public @Nullable BoilerBlockEntity boiler() {
        Direction back = MachineBlock.front(getBlockState()).getOpposite();
        for (BlockPos p : ((BaseMultiblock) getBlockState().getBlock()).getMultiblockShape(level, worldPosition, getBlockState()).getGlobalPositions()) {
            BlockPos n = p.relative(back);
            BlockPos c = level.getBlockEntity(n) instanceof IMultiBlockEntity m ? m.getCenter() : n;
            if (level.getBlockEntity(c) instanceof BoilerBlockEntity b) {
                return b;
            }
        }
        return null;
    }

    /** Return the steam drawn in the last tick in mB. */
    public int steamRate() {
        return steamRate;
    }

    @Override
    public void serverTick() {
        int before = steamRate;
        steamRate = 0;
        if (buffer.room() >= 120) {
            BoilerBlockEntity b = boiler();
            if (b != null) {
                steamRate = b.drawSteam(worldPosition, 20);
                buffer.fill(steamRate * 6);
            }
        }
        long now = level.getGameTime();
        if (steamRate > 0 && now % Math.max(1, 40 / Math.max(1, steamRate / 5)) == 0) {
            level.playSound(null, worldPosition, AutarkiaSounds.of("engine.chuff"), SoundSource.BLOCKS, 0.5f, 0.6f);
        }
        if ((before > 0) != (steamRate > 0)) {
            syncNow();
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
        out.add(Component.translatable("jade.autarkia.output", steamRate * 6));
        out.add(Component.translatable("jade.autarkia.buffer", buffer.stored(), buffer.capacity()));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SteamEngineBlockEntity>("piston", 4, test -> {
            SteamEngineBlockEntity be = test.animatable();
            test.setControllerSpeed(Math.max(0.3f, be.steamRate / 20f));
            return test.setAndContinue(be.steamRate > 0 ? RUN : IDLE);
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
        out.putInt("rate", steamRate);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        steamRate = in.getIntOr("rate", 0);
    }
}

package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.Ticking;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Lightning mast, 1x4x1 with the port at the bottom block (02 section 5, W8). Valid while the sky is open above its
 * top. In thunder each valid mast calls a strike with chance 1 / {@code lightningMastStrikeChance} per tick; bolts
 * within {@code lightningMastRadius} move to it. Each strike splits {@code lightningMastStrikeFE} equally among the
 * capacitor banks of the mast network (each capped at its free room); the rest is grounded. Strikes never start a pulse.
 */
public class LightningMastBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.lightning_mast.idle");
    private static final RawAnimation CHARGE = RawAnimation.begin().thenPlay("animation.lightning_mast.charge");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer none = new EnergyBuffer(0, 0, 0, () -> { });
    private int strikes;
    private long lastStored;

    public LightningMastBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.LIGHTNING_MAST.get(), pos, state);
    }

    /** Return the top block of the mast. */
    public BlockPos top() {
        return worldPosition.above(3);
    }

    /** Return true while the sky is open above the top. */
    public boolean valid() {
        return level.canSeeSky(top().above());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel sl && isCenter()) {
            Lightning.addMast(sl, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel sl) {
            Lightning.removeMast(sl, worldPosition);
        }
        super.setRemoved();
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        if (sl.isThundering() && valid() && sl.getRandom().nextInt(AutarkiaConfig.LIGHTNING_MAST_STRIKE_CHANCE.get()) == 0) {
            LightningBolt bolt = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(sl, EntitySpawnReason.EVENT);
            if (bolt != null) {
                bolt.snapTo(top().getX() + 0.5, top().getY() + 1, top().getZ() + 0.5);
                sl.addFreshEntity(bolt);
            }
        }
    }

    /** Take a strike: split the strike FE among the capacitor banks of the mast network and ground the rest. */
    public void strike() {
        ServerLevel sl = serverLevel();
        strikes++;
        triggerAnim("main", "charge");
        int total = AutarkiaConfig.LIGHTNING_MAST_STRIKE_FE.get();
        List<CapacitorBankBlockEntity> banks = new ArrayList<>();
        GridNetwork net = Grid.of(sl).network(worldPosition.asLong());
        if (net != null) {
            for (GridNetwork.Attached a : net.members) {
                if (a.member instanceof CapacitorBankBlockEntity c && !c.isRemoved()) {
                    banks.add(c);
                }
            }
        }
        long stored = 0;
        if (!banks.isEmpty()) {
            int share = total / banks.size();
            for (CapacitorBankBlockEntity c : banks) {
                stored += c.absorbLightning(share);
                sl.playSound(null, c.getBlockPos(), AutarkiaSounds.of("mast.strike_charge"), SoundSource.BLOCKS, 0.8f, 1.5f);
            }
        }
        lastStored = stored;
        if (stored < total) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5, worldPosition.getY() + 0.2, worldPosition.getZ() + 0.5, 24, 0.5, 0.2, 0.5, 0.2);
        }
        if (stored >= 120000) {
            dev.autarkia.registry.AutarkiaAdvancements.awardNear(sl, Vec3.atCenterOf(worldPosition), "power/storm_harvest");
        }
        dev.autarkia.Autarkia.LOG.info("[lightning] mast at {} stored {} of {} FE in {} capacitor banks", worldPosition.toShortString(), stored, total, banks.size());
        syncNow();
    }

    /** Return the FE stored by the last strike. */
    public long lastStored() {
        return lastStored;
    }

    @Override
    public Role gridRole() {
        return Role.GENERATOR;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return none;
    }

    @Override
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int gridMaxIn() {
        return 0;
    }

    @Override
    public int workDraw() {
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
        out.add(Component.translatable(valid() ? "jade.autarkia.mast.valid" : "jade.autarkia.mast.covered"));
        out.add(Component.translatable("jade.autarkia.mast.strikes", strikes, lastStored));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<LightningMastBlockEntity> main = new AnimationController<>("main", 0, test -> test.setAndContinue(IDLE));
        main.triggerableAnim("charge", CHARGE);
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("strikes", strikes);
        out.putLong("last", lastStored);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        strikes = in.getIntOr("strikes", 0);
        lastStored = in.getLongOr("last", 0);
    }
}

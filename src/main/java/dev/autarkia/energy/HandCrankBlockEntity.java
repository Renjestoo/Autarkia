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
import dev.autarkia.registry.Ticking;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Hand crank generator (02 section 6.2): each use keeps it turning for 6 ticks at 20 FE/t (a driving worker gives
 * 10 FE/t); the cranking player gets 0.025 exhaustion per tick. Buffer 1,000 FE, out 40 FE/t.
 */
public class HandCrankBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, dev.autarkia.registry.JadeInfo, MuscleGenerator,
    dev.autarkia.colony.ColonySensors.Member {
    private static final RawAnimation TURN = RawAnimation.begin().thenLoop("animation.hand_crank.turn");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.hand_crank.idle");
    public static final int PLAYER_OUTPUT = 20;
    public static final int WORKER_OUTPUT = 10;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(1000, 0, 40, this::setChanged);
    private long crankUntil;
    private @Nullable UUID cranker;
    private long workerUntil;
    private int output;
    private int lastOutput;

    public HandCrankBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.HAND_CRANK.get(), pos, state);
    }

    /** Keep the crank turning for 6 more ticks under the player's hand. */
    public void crank(Player player) {
        crankUntil = level.getGameTime() + 6;
        cranker = player.getUUID();
    }

    @Override
    public void setWorkerDriving(Entity worker, boolean driving) {
        workerUntil = driving ? level.getGameTime() + 1 : 0;
    }

    @Override
    public int workerOutput() {
        return WORKER_OUTPUT;
    }

    @Override
    public boolean storageFull() {
        return buffer.room() <= 0;
    }

    @Override
    public java.util.Set<dev.autarkia.colony.ColonySensors.Kind> colonyKinds() {
        return java.util.Set.of(dev.autarkia.colony.ColonySensors.Kind.GENERATOR);
    }

    /** Return the FE/t made this tick. */
    public int output() {
        return output;
    }

    @Override
    public void serverTick() {
        long now = level.getGameTime();
        output = now < crankUntil ? PLAYER_OUTPUT : now <= workerUntil ? WORKER_OUTPUT : 0;
        if (output > 0) {
            buffer.fill(output);
            if (output == PLAYER_OUTPUT && cranker != null && ((ServerLevel) level).getEntity(cranker) instanceof Player p) {
                p.causeFoodExhaustion(0.025F);
                if (p instanceof net.minecraft.server.level.ServerPlayer sp && !(sp instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
                    dev.autarkia.registry.AutarkiaAdvancements.award(sp, "power/root");
                }
            }
            if (now % 8 == 0) {
                level.playSound(null, worldPosition, AutarkiaSounds.of("crank.turn"), SoundSource.BLOCKS, 0.6f, 1.2f);
            }
        }
        if ((output > 0) != (lastOutput > 0)) {
            syncNow();
        }
        lastOutput = output;
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

    @Override
    public String jadeGroup() {
        return "power";
    }

    @Override
    public void jadeLines(java.util.List<net.minecraft.network.chat.Component> out) {
        out.add(net.minecraft.network.chat.Component.translatable("jade.autarkia.output", output));
        out.add(net.minecraft.network.chat.Component.translatable("jade.autarkia.buffer", buffer.stored(), buffer.capacity()));
    }

    /** Return the buffer exposed on every face except the front (extract only). */
    public EnergyBuffer buffer() {
        return buffer;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<HandCrankBlockEntity>("main", 2, test -> {
            int out = test.animatable().output;
            test.setControllerSpeed(Math.max(0.1f, out / 20f));
            return test.setAndContinue(out > 0 ? TURN : IDLE);
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
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        output = in.getIntOr("output", 0);
    }
}

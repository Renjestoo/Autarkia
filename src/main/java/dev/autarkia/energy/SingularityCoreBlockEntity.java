package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.serialization.Codec;
import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MenuButtons;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

/**
 * Singularity Core, 5x5x5 with the center at the bottom middle (02 section 6.4, decision G1). States OFF, IGNITING and
 * ONLINE. Ignition (menu button 0) makes it a grid consumer on its machine port or taps at up to 1,048,576 FE/t until
 * 100,000,000 FE are in; that FE is spent and the core is ONLINE from then on, also across a reload. ONLINE it makes
 * 131,072 FE/t into its 1,000,000,000 FE buffer and gives up to 1,048,576 FE/t to the grid. Menu button 1, or the
 * wrench while sneaking, stops it (OFF, the buffer is kept, a partial ignition is lost). Explosion gains
 * ({@link EnergyApi#absorbExplosion}) and damage payments ({@link DomainPowerIndex#pay}) are counted here for the menu.
 * Breaking it while ONLINE drops the block and loses the buffer, without an explosion.
 */
public class SingularityCoreBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo, MenuProvider,
    MenuButtons {
    public static final int IGNITION_FE = 100_000_000;
    public static final int IGNITION_RATE = 1_048_576;
    public static final int BUFFER = 1_000_000_000;
    public static final int BASE_OUTPUT = 131_072;
    public static final int MAX_OUT = 1_048_576;
    public static final int PAID_WINDOW = 1200;

    /** Core state (decision G1). */
    public enum State {
        OFF, IGNITING, ONLINE;

        /** Return the translation key of the state name. */
        public String key() {
            return "singularity.autarkia.state." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.singularity_core.run");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.singularity_core.idle");
    private static final RawAnimation DRINK = RawAnimation.begin().thenPlay("animation.singularity_core.drink");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(BUFFER, 0, MAX_OUT, this::setChanged);
    private final EnergyBuffer ignition = new EnergyBuffer(IGNITION_FE, IGNITION_RATE, 0, this::setChanged);
    private final LongArrayList paidLog = new LongArrayList();
    private State state = State.OFF;
    private int output;
    private int explosions;
    private long pointsMilli;
    private long paidTotal;
    private long lastGain = -1;
    private float lastRadius;
    private int lastLamps;
    private long lastAbsorbAt = -1;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            long paid = paidRecent();
            return switch (i) {
                case 0 -> ignition.stored();
                case 1 -> IGNITION_FE;
                case 2 -> buffer.stored();
                case 3 -> buffer.capacity();
                case 4 -> state.ordinal();
                case 5 -> output;
                case 6 -> (int) Math.min(Integer.MAX_VALUE, pointsMilli / 1000);
                case 7 -> explosions;
                case 8 -> (int) (paid >>> 31);
                case 9 -> (int) (paid & Integer.MAX_VALUE);
                default -> state == State.OFF ? 1 : 0;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 11;
        }
    };

    public SingularityCoreBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.SINGULARITY_CORE.get(), pos, state);
    }

    /** Return the core state. */
    public State state() {
        return state;
    }

    /** Return the FE in the 1,000,000,000 FE buffer. */
    public long buffer() {
        return buffer.stored();
    }

    /** Return the buffer for direct grid access (extract limit 1,048,576 FE/t on the capability). */
    public EnergyBuffer energy() {
        return buffer;
    }

    /** Return the ignition FE taken so far. */
    public int ignitionDrawn() {
        return ignition.stored();
    }

    /** Return the ignition buffer (it fills from the grid while IGNITING). */
    public EnergyBuffer ignitionBuffer() {
        return ignition;
    }

    /** Return the FE made in the last tick (131,072 while ONLINE with room, else less). */
    public int output() {
        return output;
    }

    /** Return the number of explosions this core absorbed. */
    public int explosions() {
        return explosions;
    }

    /** Return the damage points paid from this core's domain grid, in thousandths. */
    public long pointsMilli() {
        return pointsMilli;
    }

    /** Return the FE paid for damage in total. */
    public long paidTotal() {
        return paidTotal;
    }

    /** Return the FE added by the last absorbed explosion, or -1 before the first one. */
    public long lastGain() {
        return lastGain;
    }

    /** Return the radius of the last absorbed explosion. */
    public float lastRadius() {
        return lastRadius;
    }

    /** Return the number of lamps the last absorbed explosion flashed. */
    public int lastLamps() {
        return lastLamps;
    }

    /** Return the game time of the last absorbed explosion, or -1. */
    public long lastAbsorbAt() {
        return lastAbsorbAt;
    }

    /** Return the FE paid for damage in the last 1,200 ticks. */
    public long paidRecent() {
        long now = level == null ? 0 : level.getGameTime();
        long sum = 0;
        for (int i = 0; i + 1 < paidLog.size(); i += 2) {
            if (now - paidLog.getLong(i) < PAID_WINDOW) {
                sum += paidLog.getLong(i + 1);
            }
        }
        return sum;
    }

    /** Start the ignition; refused unless the core is OFF. */
    public void ignite() {
        if (state != State.OFF) {
            return;
        }
        setState(State.IGNITING);
    }

    /** Stop the core: OFF, the buffer is kept and a partial ignition is lost; refused while already OFF. */
    public void stop() {
        if (state == State.OFF) {
            return;
        }
        ignition.drain(ignition.stored());
        setState(State.OFF);
    }

    private void setState(State next) {
        State before = state;
        state = next;
        Autarkia.LOG.info("[grid] singularity core at {}: {} -> {}", worldPosition.toShortString(), before, next);
        syncNow();
        if (level instanceof ServerLevel sl) {
            DomainPowerIndex.changed(sl);
        }
    }

    /**
     * Stop the core whose part is at pos for a sneaking wrench user; return false when pos holds no running core (the
     * wrench then picks the block up as usual).
     */
    public static boolean wrenchStop(ServerLevel level, BlockPos pos, Player player) {
        if (!(Grid.memberAt(level, pos) instanceof SingularityCoreBlockEntity core) || core.state == State.OFF) {
            return false;
        }
        core.stop();
        player.sendOverlayMessage(Component.translatable("message.autarkia.singularity.stopped"));
        return true;
    }

    /** Add up to fe FE to the buffer, capped at its room; return the FE added. */
    long fill(long fe) {
        return buffer.fill((int) Math.min(Integer.MAX_VALUE, Math.max(0, fe)));
    }

    /** Count an absorbed explosion whose gain went to the domain's cores (added FE in total), play the drink animation and sound. */
    void recordAbsorb(long added, float radius, int lamps) {
        explosions++;
        lastGain = added;
        lastRadius = radius;
        lastLamps = lamps;
        lastAbsorbAt = level.getGameTime();
        triggerAnim("core", "drink");
        level.playSound(null, worldPosition.above(2), AutarkiaSounds.of("singularity.drink"), SoundSource.BLOCKS, 1f, 1f);
        syncThrottled(10);
    }

    /** Count a damage payment of the domain grid: FE and damage points in thousandths. */
    void recordPayment(long fe, long milli) {
        paidTotal += fe;
        pointsMilli += milli;
        paidLog.add(level.getGameTime());
        paidLog.add(fe);
        setChanged();
    }

    @Override
    public boolean menuButton(Player player, int id) {
        if (id == 0 && state == State.OFF) {
            ignite();
            return true;
        }
        if (id == 1 && state != State.OFF) {
            stop();
            return true;
        }
        return false;
    }

    @Override
    public void serverTick() {
        output = 0;
        if (state == State.IGNITING && ignition.stored() >= IGNITION_FE) {
            ignition.drain(ignition.stored());
            setState(State.ONLINE);
        } else if (state == State.ONLINE) {
            output = buffer.fill(BASE_OUTPUT);
        }
        long now = level.getGameTime();
        while (paidLog.size() >= 2 && now - paidLog.getLong(0) >= PAID_WINDOW) {
            paidLog.removeElements(0, 2);
        }
        if (now % 40 == 0) {
            syncThrottled(20);
        } else {
            flushSync(20);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState st) {
        super.preRemoveSideEffects(pos, st);
        if (level instanceof ServerLevel sl && isCenter()) {
            if (state == State.ONLINE) {
                Autarkia.LOG.info("[grid] singularity core at {} broke while ONLINE; {} FE of its buffer are lost", pos.toShortString(), buffer.stored());
            }
            DomainPowerIndex.changed(sl);
        }
    }

    @Override
    public Role gridRole() {
        return state == State.IGNITING ? Role.CONSUMER : Role.GENERATOR;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return state == State.IGNITING ? ignition : buffer;
    }

    @Override
    public int gridMaxOut() {
        return state == State.ONLINE ? MAX_OUT : 0;
    }

    @Override
    public int gridMaxIn() {
        return state == State.IGNITING ? IGNITION_RATE : 0;
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
        out.add(Component.translatable("jade.autarkia.singularity.state", Component.translatable(state.key())));
        if (state == State.IGNITING) {
            out.add(Component.translatable("jade.autarkia.singularity.ignition", ignition.stored() * 100L / IGNITION_FE));
        }
    }

    @Override
    public float jadeProgress() {
        return state == State.IGNITING ? ignition.fillRatio() : buffer.fillRatio();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SingularityCoreBlockEntity>("core", 10, test -> {
            SingularityCoreBlockEntity be = test.animatable();
            test.setControllerSpeed(be.state == State.IGNITING ? Math.max(0.1f, be.ignition.fillRatio()) : 1f);
            return test.setAndContinue(be.state == State.OFF ? IDLE : RUN);
        }).triggerableAnim("drink", DRINK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.autarkia.singularity_core");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SlotMenu(AutarkiaMenus.SINGULARITY_CORE.get(), id, inv, worldPosition, new ItemStacksResourceHandler(0), data,
            GuiLayouts.SINGULARITY_CORE);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
        ignition.serialize(out.child("ignition"));
        out.putInt("state", state.ordinal());
        out.putInt("output", output);
        out.putInt("explosions", explosions);
        out.putLong("points_milli", pointsMilli);
        out.putLong("paid_total", paidTotal);
        out.store("paid_log", Codec.LONG.listOf(), new ArrayList<>(paidLog));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        in.child("ignition").ifPresent(ignition::deserialize);
        state = State.values()[Math.clamp(in.getIntOr("state", 0), 0, State.values().length - 1)];
        output = in.getIntOr("output", 0);
        explosions = in.getIntOr("explosions", 0);
        pointsMilli = in.getLongOr("points_milli", 0);
        paidTotal = in.getLongOr("paid_total", 0);
        paidLog.clear();
        in.read("paid_log", Codec.LONG.listOf()).ifPresent(paidLog::addAll);
    }
}

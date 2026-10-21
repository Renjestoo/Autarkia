package dev.autarkia.energy;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.Autarkia;
import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MenuButtons;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Plasma generator, 3x3x3 with the center at the bottom middle (02 sections 6.1 and 6.2, decision G7). States COLD,
 * IGNITING, RAMPING, RUNNING and HOT_IDLE. The ignition button (with fuel present) makes it a grid consumer on its
 * machine port or taps until it has taken 1,000,000 FE at most 5,000 FE/t; then the output ramps linearly over 200
 * burning ticks to 32,768 FE/t. Fuel is counted in FE: one {@code lattice_fuel_cell} is 78,643,200 FE, 2,400 ticks at
 * full output, and the next cell loads when the rest cannot cover a tick. A tick burns only while the 1,000,000 FE
 * buffer has room for its whole output and fuel is left; otherwise the plasma idles hot, and after 200 idle ticks it
 * is cold and needs a new ignition. Out 65,536 FE/t. Breaking it while hot (ramping, running or idling hot) makes an
 * explosion of power 4.0 without block damage; the blast runs as a server task right after the removal, because while
 * the 27 positions still stand they shield every entity from the center. {@code plasma.hum} plays every 80 ticks while
 * hot.
 */
public class PlasmaGeneratorBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, GeoBlockEntity, JadeInfo, MenuProvider,
    MenuButtons {
    public static final int IGNITION_FE = 1_000_000;
    public static final int IGNITION_RATE = 5000;
    public static final int RAMP_TICKS = 200;
    public static final int FULL_OUTPUT = 32768;
    public static final int CELL_TICKS = 2400;
    public static final int CELL_FE = CELL_TICKS * FULL_OUTPUT;
    public static final int HOT_TICKS = 200;
    public static final int BUFFER = 1_000_000;
    public static final int MAX_OUT = 65536;
    public static final float HOT_BREAK_POWER = 4.0F;

    /** Plasma state (decision G7); the ordinal is the {@code gen.plasma.state} sensor value. */
    public enum State {
        COLD, IGNITING, RAMPING, RUNNING, HOT_IDLE;

        /** Return the translation key of the state name. */
        public String key() {
            return "plasma.autarkia.state." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.plasma_generator.run");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.plasma_generator.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(BUFFER, 0, MAX_OUT, this::setChanged);
    private final EnergyBuffer ignition = new EnergyBuffer(IGNITION_FE, IGNITION_RATE, 0, this::setChanged);
    private final MachineInventory fuel = new MachineInventory(1, 0, s -> s.is(AutarkiaItems.LATTICE_FUEL_CELL.get()), this::setChanged) {
        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return Math.min(16, super.getCapacity(index, resource));
        }
    };
    private State state = State.COLD;
    private int ramp;
    private int fuelLeft;
    private int hotTicks;
    private int output;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> ignition.stored();
                case 1 -> IGNITION_FE;
                case 2 -> buffer.stored();
                case 3 -> buffer.capacity();
                case 4 -> state.ordinal();
                case 5 -> output;
                case 6 -> state == State.HOT_IDLE ? Math.max(0, HOT_TICKS - hotTicks) : 0;
                default -> canIgnite() ? 1 : 0;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public PlasmaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.PLASMA_GENERATOR.get(), pos, state);
    }

    /** Return the output buffer (extract only). */
    public EnergyBuffer buffer() {
        return buffer;
    }

    /** Return the fuel slot (one slot, at most 16 cells). */
    public MachineInventory fuel() {
        return fuel;
    }

    /** Return the plasma state. */
    public State state() {
        return state;
    }

    /** Return the FE made in the last tick. */
    public int output() {
        return output;
    }

    /** Return the burning ticks of the ramp so far (200 when running). */
    public int ramp() {
        return ramp;
    }

    /** Return the ignition FE taken so far. */
    public int ignitionDrawn() {
        return ignition.stored();
    }

    /** Return the fuel FE left of the loaded cell. */
    public int fuelLeft() {
        return fuelLeft;
    }

    /** Return the cells in the fuel slot. */
    public int fuelCells() {
        return fuel.get(0).getCount();
    }

    /** Return true while the plasma is hot: ramping, running or idling hot. */
    public boolean hot() {
        return state == State.RAMPING || state == State.RUNNING || state == State.HOT_IDLE;
    }

    /** Return true when the ignition button works: cold, with a cell in the slot or fuel left. */
    public boolean canIgnite() {
        return state == State.COLD && (fuelLeft > 0 || !fuel.get(0).isEmpty());
    }

    /** Return the FE/t of one burning tick at the current ramp step: 32,768 x step / 200 (step counted from 1). */
    public static int rampOutput(int step) {
        return (int) ((long) FULL_OUTPUT * Math.min(step, RAMP_TICKS) / RAMP_TICKS);
    }

    @Override
    public boolean menuButton(Player player, int id) {
        if (id != 0 || !canIgnite()) {
            return false;
        }
        state = State.IGNITING;
        syncNow();
        return true;
    }

    @Override
    public void serverTick() {
        State before = state;
        output = 0;
        switch (state) {
            case IGNITING -> {
                if (ignition.stored() >= IGNITION_FE) {
                    ignition.drain(ignition.stored());
                    state = State.RAMPING;
                    ramp = 0;
                    hotTicks = 0;
                }
            }
            case RAMPING, RUNNING, HOT_IDLE -> burn();
            default -> {
            }
        }
        if (hot() && level.getGameTime() % 80 == 0) {
            level.playSound(null, worldPosition.above(), AutarkiaSounds.of("plasma.hum"), SoundSource.BLOCKS, 1f, 1f);
        }
        if (state != before) {
            Autarkia.LOG.debug("[grid] plasma generator at {}: {} -> {}", worldPosition.toShortString(), before, state);
            syncNow();
        } else if (level.getGameTime() % 40 == 0) {
            syncThrottled(20);
        }
    }

    private void burn() {
        int rate = rampOutput(ramp + 1);
        if (fuelLeft < rate && !fuel.get(0).isEmpty()) {
            fuel.shrink(0, 1);
            fuelLeft += CELL_FE;
        }
        if (fuelLeft <= 0 || buffer.room() < rate) {
            state = State.HOT_IDLE;
            if (++hotTicks > HOT_TICKS) {
                state = State.COLD;
                ramp = 0;
                hotTicks = 0;
            }
            return;
        }
        int made = Math.min(rate, fuelLeft);
        buffer.fill(made);
        fuelLeft -= made;
        output = made;
        ramp = Math.min(RAMP_TICKS, ramp + 1);
        hotTicks = 0;
        state = ramp >= RAMP_TICKS ? State.RUNNING : State.RAMPING;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState st) {
        super.preRemoveSideEffects(pos, st);
        if (!(level instanceof ServerLevel sl) || !isCenter()) {
            return;
        }
        Containers.dropItemStack(sl, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, fuel.get(0).copy());
        if (hot()) {
            BlockPos c = pos.immutable();
            State was = state;
            var server = sl.getServer();
            server.schedule(server.wrapRunnable(() -> {
                sl.explode(null, c.getX() + 0.5, c.getY() + 1.5, c.getZ() + 0.5, HOT_BREAK_POWER, false, Level.ExplosionInteraction.NONE);
                Autarkia.LOG.info("[grid] plasma generator at {} broke while hot ({})", c.toShortString(), was);
            }));
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
        return state == State.IGNITING ? 0 : MAX_OUT;
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
        out.add(Component.translatable("jade.autarkia.plasma.state", Component.translatable(state.key())));
        if (state == State.IGNITING) {
            out.add(Component.translatable("jade.autarkia.plasma.ignition", ignition.stored() * 100L / IGNITION_FE));
        }
    }

    @Override
    public float jadeProgress() {
        return state == State.IGNITING ? ignition.fillRatio() : buffer.fillRatio();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<PlasmaGeneratorBlockEntity>("plasma", 10, test -> {
            PlasmaGeneratorBlockEntity be = test.animatable();
            test.setControllerSpeed(be.state == State.RAMPING ? Math.max(0.2f, be.ramp / (float) RAMP_TICKS) : 1f);
            return test.setAndContinue(be.state == State.RAMPING || be.state == State.RUNNING ? RUN : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.autarkia.plasma_generator");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SlotMenu(AutarkiaMenus.PLASMA_GENERATOR.get(), id, inv, worldPosition, fuel, data, GuiLayouts.PLASMA_GENERATOR);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
        ignition.serialize(out.child("ignition"));
        fuel.serialize(out.child("fuel"));
        out.putInt("state", state.ordinal());
        out.putInt("ramp", ramp);
        out.putInt("fuel_left", fuelLeft);
        out.putInt("hot", hotTicks);
        out.putInt("output", output);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        in.child("ignition").ifPresent(ignition::deserialize);
        in.child("fuel").ifPresent(fuel::deserialize);
        state = State.values()[Math.clamp(in.getIntOr("state", 0), 0, State.values().length - 1)];
        ramp = in.getIntOr("ramp", 0);
        fuelLeft = in.getIntOr("fuel_left", 0);
        hotTicks = in.getIntOr("hot", 0);
        output = in.getIntOr("output", 0);
    }
}

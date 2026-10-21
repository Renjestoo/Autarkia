package dev.autarkia.logistics;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.energy.WrenchItem;
import dev.autarkia.energy.Wrenchable;
import dev.autarkia.processing.MachineInventory;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.MenuButtons;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import dev.autarkia.storage.CrateBlockEntity;
import dev.autarkia.storage.FallingCrateEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * {@code crate_launcher} (04 section 1.6), a 1x2x1 big block (rules B1 to B7). Buffers: input 27 slots and a crate slot
 * (empty crates, stack 16); 16,000 FE. A wrench on the launcher, then on a {@code catch_net} 8 to {@code launcherMaxRange}
 * blocks away (height -32 to +24) sets the target. It fires when the input is full (every slot used), or not empty and
 * unchanged for 200 ticks, with one empty crate, {@code 1,000 + 40 x distance} FE, 60 ticks after the last shot and
 * below {@code launcherMaxInFlight} crates in the air: all 27 slots pack into the crate, which flies as a
 * {@code falling_crate} in mode LAUNCHED on the lowest clear arc ({@link LaunchArc}). 3 misses within 12,000 ticks pause
 * it until a player presses Reset or, inside a colony domain, the Core resets it after 6,000 ticks.
 */
public class CrateLauncherBlockEntity extends AutarkiaBlockEntity implements Ticking, GridMember, JadeInfo, MenuProvider, MenuButtons, GeoBlockEntity,
    Wrenchable {
    /** Why the launcher does not fire right now (READY when it can). */
    public enum Status {
        READY, NO_TARGET, OUT_OF_RANGE, WAITING, COOLDOWN, NO_CRATE, NO_POWER, BUSY, NO_ARC, PAUSED;

        /** Return the translation key of the status. */
        public String key() {
            return "state.autarkia.launcher." + name().toLowerCase(Locale.ROOT);
        }
    }

    public static final int SLOTS = 27;
    public static final int CRATE_SLOT = 27;
    public static final int CAPACITY = 16000;
    public static final int MIN_RANGE = 8;
    public static final int COOLDOWN = 60;
    public static final int IDLE_FIRE = 200;
    public static final int PAUSE_MISSES = 3;
    public static final int PAUSE_WINDOW = 12000;
    public static final int STATS_WINDOW = 24000;
    public static final int CORE_RESET = 6000;
    private static final Map<Level, Set<BlockPos>> LIVE = new WeakHashMap<>();
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.crate_launcher.idle");
    private static final RawAnimation THROW = RawAnimation.begin().thenPlay("animation.crate_launcher.throw");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final MachineInventory inv = new MachineInventory(SLOTS + 1, 0, s -> true, this::inputChanged) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            ItemStack s = resource.toStack(1);
            return index < SLOTS ? !CrateBlockEntity.isFilledCrate(s) : index == CRATE_SLOT && s.is(AutarkiaItems.CRATE.get()) && !CrateBlockEntity.isFilledCrate(s);
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return index == CRATE_SLOT ? Math.min(16, super.getCapacity(index, resource)) : super.getCapacity(index, resource);
        }
    };
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, 512, 0, this::setChanged);
    private final LinkerRef linker = new LinkerRef();
    private final List<Long> shots = new ArrayList<>();
    private final List<Long> misses = new ArrayList<>();
    private @Nullable BlockPos target;
    private long lastInput;
    private long nextFire;
    private int angle;
    private int lastResult;
    private double lastMiss;
    private boolean lastRefused;
    private boolean paused;
    private long pausedAt;
    private Status status = Status.NO_TARGET;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> energy.stored();
                case 1 -> energy.capacity();
                case 2 -> (int) Math.round(distance());
                case 3 -> angle;
                case 4 -> lastResult;
                case 5 -> (int) Math.round(lastMiss * 10);
                case 6 -> paused ? 1 : 0;
                case 7 -> status.ordinal();
                default -> inv.get(CRATE_SLOT).getCount();
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public CrateLauncherBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.CRATE_LAUNCHER.get(), pos, state);
    }

    private static List<CrateLauncherBlockEntity> live(Level level) {
        List<CrateLauncherBlockEntity> out = new ArrayList<>();
        for (BlockPos p : List.copyOf(LIVE.getOrDefault(level, Set.of()))) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof CrateLauncherBlockEntity l && !l.isRemoved()) {
                out.add(l);
            }
        }
        return out;
    }

    /** Return the shots of the level's loaded launchers in the last 24,000 ticks (sensor {@code transport.launcher_shots}). */
    public static int levelShots(Level level) {
        return live(level).stream().mapToInt(CrateLauncherBlockEntity::shots).sum();
    }

    /** Return the misses of the level's loaded launchers in the last 24,000 ticks (sensor {@code transport.launcher_misses}). */
    public static int levelMisses(Level level) {
        return live(level).stream().mapToInt(CrateLauncherBlockEntity::misses).sum();
    }

    /** Return the number of the level's loaded launchers that are paused (sensor {@code transport.launcher_paused}). */
    public static int levelPaused(Level level) {
        return (int) live(level).stream().filter(CrateLauncherBlockEntity::paused).count();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel sl && isCenter()) {
            LIVE.computeIfAbsent(sl, k -> new HashSet<>()).add(worldPosition.immutable());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel sl) {
            Set<BlockPos> s = LIVE.get(sl);
            if (s != null) {
                s.remove(worldPosition);
            }
        }
    }

    private void inputChanged() {
        setChanged();
        if (level != null) {
            lastInput = level.getGameTime();
        }
    }

    /** Return the 28 slots: 0 to 26 input, 27 crate slot. */
    public MachineInventory inventory() {
        return inv;
    }

    /** Return the handler other blocks see (insert into the input and the crate slot). */
    public ResourceHandler<ItemResource> items() {
        return inv.automation();
    }

    /** Return the energy buffer. */
    public EnergyBuffer energy() {
        return energy;
    }

    /** Return the target net centre, or null. */
    public @Nullable BlockPos target() {
        return target;
    }

    /** Return the status of the last tick. */
    public Status status() {
        return status;
    }

    /** Return true while paused after 3 misses. */
    public boolean paused() {
        return paused;
    }

    /** Return the angle of the last chosen arc in degrees (0 before the first). */
    public int angle() {
        return angle;
    }

    /** Return the last shot result: 0 none, 1 hit, 2 miss. */
    public int lastResult() {
        return lastResult;
    }

    /** Return the distance of the last miss from the net centre in blocks. */
    public double lastMiss() {
        return lastMiss;
    }

    /** Return true when the last miss was a crate the net refused (net output full). */
    public boolean lastRefused() {
        return lastRefused;
    }

    /** Return the shots of the last 24,000 ticks. */
    public int shots() {
        return shots.size();
    }

    /** Return the misses of the last 24,000 ticks. */
    public int misses() {
        return misses.size();
    }

    /** Return the launch point: the centre of the top face of the launcher. */
    public Vec3 launchPoint() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 2.0, worldPosition.getZ() + 0.5);
    }

    /** Return the horizontal distance from the launch point to the target net centre (0 without target). */
    public double distance() {
        if (target == null) {
            return 0;
        }
        Vec3 t = LaunchedCrate.target(target);
        Vec3 s = launchPoint();
        return Math.sqrt((t.x - s.x) * (t.x - s.x) + (t.z - s.z) * (t.z - s.z));
    }

    /** Return the FE one shot at the current target costs: 1,000 + 40 per block of distance. */
    public int shotCost() {
        return 1000 + (int) Math.ceil(40 * distance());
    }

    /** Set the target net centre (null clears it) and remember the player; returns the refusal key, or null when set. */
    public @Nullable String setTarget(@Nullable BlockPos net, @Nullable ServerPlayer by) {
        if (net != null) {
            Vec3 t = LaunchedCrate.target(net);
            Vec3 s = launchPoint();
            double d = Math.sqrt((t.x - s.x) * (t.x - s.x) + (t.z - s.z) * (t.z - s.z));
            int dy = net.getY() - worldPosition.getY();
            if (d < MIN_RANGE || d > AutarkiaConfig.LAUNCHER_MAX_RANGE.get()) {
                return "message.autarkia.launcher.range";
            }
            if (dy < -32 || dy > 24) {
                return "message.autarkia.launcher.height";
            }
        }
        target = net == null ? null : net.immutable();
        if (by != null) {
            linker.set(by);
        }
        syncNow();
        return null;
    }

    /** Handle the second wrench click on a catch net: target it from the launcher stored in the wrench. */
    public static InteractionResult targetFromWrench(ServerLevel sl, ServerPlayer player, ItemStack wrench, BlockPos net) {
        AutarkiaComponents.LinkStart start = wrench.get(AutarkiaComponents.LINK_START.get());
        if (start == null || !start.node().dimension().equals(sl.dimension())
            || !(sl.getBlockEntity(start.node().pos()) instanceof CrateLauncherBlockEntity launcher)) {
            player.sendOverlayMessage(Component.translatable("message.autarkia.launcher.no_launcher"));
            return InteractionResult.FAIL;
        }
        wrench.remove(AutarkiaComponents.LINK_START.get());
        String refusal = launcher.setTarget(net, player);
        player.sendSystemMessage(refusal == null ? Component.translatable("message.autarkia.launcher.targeted", net.getX(), net.getY(), net.getZ(),
            String.format(Locale.ROOT, "%.1f", launcher.distance())) : Component.translatable(refusal));
        return refusal == null ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public InteractionResult wrench(ServerPlayer player) {
        if (!isCenter() && level != null && level.getBlockEntity(getCenter()) instanceof CrateLauncherBlockEntity c) {
            return c.wrench(player);
        }
        ItemStack stack = player.getMainHandItem().getItem() instanceof WrenchItem ? player.getMainHandItem() : player.getOffhandItem();
        stack.set(AutarkiaComponents.LINK_START.get(), new AutarkiaComponents.LinkStart(GlobalPos.of(level.dimension(), worldPosition), Direction.UP,
            level.getGameTime()));
        player.sendOverlayMessage(Component.translatable("message.autarkia.launcher.first"));
        return InteractionResult.SUCCESS;
    }

    /** Clear the pause (Reset button or the Core). */
    public void reset() {
        paused = false;
        misses.clear();
        setChanged();
        syncNow();
    }

    /** Record the result of a shot: a hit, or a miss by a distance (refused = the net output was full). */
    void shotResult(boolean hit, double missBy, boolean refused) {
        long now = level.getGameTime();
        lastResult = hit ? 1 : 2;
        lastMiss = missBy;
        lastRefused = refused;
        if (!hit) {
            misses.add(now);
            if (misses.stream().filter(t -> now - t <= PAUSE_WINDOW).count() >= PAUSE_MISSES && !paused) {
                paused = true;
                pausedAt = now;
                Autarkia.LOG.info("[launcher] {} paused after {} misses", worldPosition.toShortString(), PAUSE_MISSES);
            }
        }
        setChanged();
        syncNow();
    }

    /** Award an advancement to the players near the event and to the player who set the target. */
    void award(ServerLevel sl, Vec3 at, String path) {
        linker.award(sl, at, path);
    }

    private boolean inputFull() {
        for (int i = 0; i < SLOTS; i++) {
            if (inv.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean inputEmpty() {
        for (int i = 0; i < SLOTS; i++) {
            if (!inv.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        long now = sl.getGameTime();
        shots.removeIf(t -> now - t > STATS_WINDOW);
        misses.removeIf(t -> now - t > STATS_WINDOW);
        Status before = status;
        status = evaluate(sl, now);
        if (status == Status.READY) {
            fire(sl, now);
        }
        if (status != before) {
            syncNow();
        }
    }

    private Status evaluate(ServerLevel sl, long now) {
        if (paused) {
            if (now - pausedAt >= CORE_RESET && Colonies.server() != null && Colonies.at(sl, worldPosition) != null) {
                Autarkia.LOG.info("[launcher] the Core reset {}", worldPosition.toShortString());
                reset();
            } else {
                return Status.PAUSED;
            }
        }
        if (target == null || !sl.isLoaded(target) || !(sl.getBlockState(target).getBlock() instanceof CatchNetBlock)) {
            return Status.NO_TARGET;
        }
        double d = distance();
        int dy = target.getY() - worldPosition.getY();
        if (d < MIN_RANGE || d > AutarkiaConfig.LAUNCHER_MAX_RANGE.get() || dy < -32 || dy > 24) {
            return Status.OUT_OF_RANGE;
        }
        if (inputEmpty() || !inputFull() && now - lastInput < IDLE_FIRE) {
            return Status.WAITING;
        }
        if (now < nextFire) {
            return Status.COOLDOWN;
        }
        if (inv.get(CRATE_SLOT).isEmpty()) {
            return Status.NO_CRATE;
        }
        if (energy.stored() < shotCost()) {
            return Status.NO_POWER;
        }
        if (LaunchedCrate.inFlight(sl) >= AutarkiaConfig.LAUNCHER_MAX_IN_FLIGHT.get()) {
            return Status.BUSY;
        }
        return Status.READY;
    }

    private void fire(ServerLevel sl, long now) {
        Vec3 start = launchPoint();
        Vec3 aim = LaunchedCrate.target(target);
        Vec3 accel = LaunchArc.acceleration(LaunchArc.wind(sl, start), AutarkiaConfig.GONDOLA_WIND_FACTOR.get());
        LaunchArc.Arc arc = LaunchArc.solve(sl, start, aim, accel,
            st -> st.is(AutarkiaBlocks.CRATE_LAUNCHER.get()) || st.getBlock() instanceof CatchNetBlock);
        if (arc == null) {
            status = Status.NO_ARC;
            nextFire = now + 20;
            return;
        }
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            contents.add(inv.get(i).copy());
            inv.put(i, ItemStack.EMPTY);
        }
        ItemStack crate = inv.get(CRATE_SLOT).copyWithCount(1);
        inv.shrink(CRATE_SLOT, 1);
        crate.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        energy.drain(shotCost());
        double d = distance();
        FallingCrateEntity.launch(sl, start, arc.velocity(), crate, new LaunchedCrate.Flight(worldPosition, target, d, false, 0));
        angle = arc.angle();
        nextFire = now + COOLDOWN;
        shots.add(now);
        triggerAnim("main", "throw");
        sl.playSound(null, worldPosition, AutarkiaSounds.of("launcher_fire"), SoundSource.BLOCKS, 1.0f, 0.6f);
        sl.sendParticles(ParticleTypes.CLOUD, start.x, start.y, start.z, 12, 0.3, 0.2, 0.3, 0.05);
        sl.sendParticles(ParticleTypes.CRIT, start.x, start.y, start.z, 16, 0.3, 0.3, 0.3, 0.3);
        Autarkia.LOG.info("[launcher] {} fired at {}: {} degrees, {} blocks, {} ticks", worldPosition.toShortString(), target.toShortString(), angle,
            String.format(Locale.ROOT, "%.1f", d), arc.ticks());
        setChanged();
    }

    @Override
    public boolean menuButton(Player player, int id) {
        if (id == 0 && paused) {
            reset();
            return true;
        }
        return false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel && isCenter()) {
            for (int i = 0; i < inv.size(); i++) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inv.get(i).copy());
            }
        }
    }

    @Override
    public Role gridRole() {
        return Role.CONSUMER;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return energy;
    }

    @Override
    public int gridMaxIn() {
        return energy.room() > 0 ? 512 : 0;
    }

    @Override
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int workDraw() {
        return target == null || inputEmpty() ? 0 : shotCost() / COOLDOWN;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "logistics";
    }

    @Override
    public void jadeLines(List<Component> out) {
        if (target == null) {
            out.add(Component.translatable(Status.NO_TARGET.key()));
            return;
        }
        out.add(Component.translatable("jade.autarkia.launcher.target", Math.round(distance()), angle));
        out.add(Component.translatable("jade.autarkia.launcher.energy", energy.stored(), energy.capacity()));
        out.add(lastShot());
        if (status != Status.READY && status != Status.WAITING) {
            out.add(Component.translatable(status.key()));
        }
    }

    /** Return the "Last shot" line: none, HIT, or MISS by X.X blocks (net output full). */
    public Component lastShot() {
        return switch (lastResult) {
            case 1 -> Component.translatable("screen.autarkia.launcher.hit");
            case 2 -> Component.translatable(lastRefused ? "screen.autarkia.launcher.miss_full" : "screen.autarkia.launcher.miss",
                String.format(Locale.ROOT, "%.1f", lastMiss));
            default -> Component.translatable("screen.autarkia.launcher.none");
        };
    }

    @Override
    public float jadeProgress() {
        return energy.fillRatio();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SlotMenu(AutarkiaMenus.CRATE_LAUNCHER.get(), id, playerInv, worldPosition, inv, data, GuiLayouts.CRATE_LAUNCHER);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<CrateLauncherBlockEntity> main = new AnimationController<>("main", 0, test -> test.setAndContinue(IDLE));
        main.triggerableAnim("throw", THROW);
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inv.serialize(out.child("inv"));
        energy.serialize(out.child("energy"));
        linker.save(out);
        if (target != null) {
            out.store("target", BlockPos.CODEC, target);
        }
        out.putLong("lastInput", lastInput);
        out.putLong("nextFire", nextFire);
        out.putInt("angle", angle);
        out.putInt("lastResult", lastResult);
        out.putDouble("lastMiss", lastMiss);
        out.putBoolean("lastRefused", lastRefused);
        out.putBoolean("paused", paused);
        out.putLong("pausedAt", pausedAt);
        out.putInt("status", status.ordinal());
        out.store("shots", com.mojang.serialization.Codec.LONG.listOf(), List.copyOf(shots));
        out.store("misses", com.mojang.serialization.Codec.LONG.listOf(), List.copyOf(misses));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("inv").ifPresent(inv::deserialize);
        in.child("energy").ifPresent(energy::deserialize);
        linker.load(in);
        target = in.read("target", BlockPos.CODEC).orElse(null);
        lastInput = in.getLongOr("lastInput", 0);
        nextFire = in.getLongOr("nextFire", 0);
        angle = in.getIntOr("angle", 0);
        lastResult = in.getIntOr("lastResult", 0);
        lastMiss = in.getDoubleOr("lastMiss", 0);
        lastRefused = in.getBooleanOr("lastRefused", false);
        paused = in.getBooleanOr("paused", false);
        pausedAt = in.getLongOr("pausedAt", 0);
        status = Status.values()[Math.clamp(in.getIntOr("status", 0), 0, Status.values().length - 1)];
        shots.clear();
        shots.addAll(in.read("shots", com.mojang.serialization.Codec.LONG.listOf()).orElse(List.of()));
        misses.clear();
        misses.addAll(in.read("misses", com.mojang.serialization.Codec.LONG.listOf()).orElse(List.of()));
    }
}

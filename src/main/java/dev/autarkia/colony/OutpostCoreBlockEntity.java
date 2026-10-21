package dev.autarkia.colony;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

/**
 * The mini Core of an outpost (05 section 8.1, 2x2x2): grows for 200 ticks after the seed is planted (GeckoLib
 * {@code grow}), then holds the outpost chunk ticket, draws 256 FE/t from the outpost network (02 section 7) and every
 * 1,200 ticks measures the outpost production profile or credits the catch-up ({@link Outposts#sample}). The share of the
 * last 1,200 ticks it had power is the power factor of the catch-up. Breaking it removes the outpost.
 *
 * <p>A planted core is also a charger for the colony's FE workers (one docked worker, on top of it for a drone, the
 * charger rate of the recharge dock, charging once grown): it starts with the 64,000 FE the seed was fabricated with, and network power
 * beyond its own 256 FE/t refills that store (up to 768 FE/t). Builder Drones cannot reach an outpost 200 blocks from
 * the Core docks and back, so the outpost is built from this charge until its own generator or the HV link runs.
 *
 * <p>A core planted by an Autarky Seed also holds the running unpack ({@link SeedUnpack}, M9 decision C3), saved with the
 * core; its FE charge goes into the charger store, whose capacity is saved with it.
 */
public class OutpostCoreBlockEntity extends AutarkiaBlockEntity implements Ticking, GridMember, JadeInfo, MenuProvider, GeoBlockEntity,
    dev.autarkia.worker.WorkerCharger, ColonySensors.Member {
    public static final int GROWTH_TICKS = 200;
    public static final int FE_PER_TICK = 256;
    public static final int SEED_ENERGY = 64000;
    public static final int KIT_PER_TICK = 4;
    private final EnergyBuffer charge = new EnergyBuffer(SEED_ENERGY, 0, 0, this::setChanged);
    private @Nullable UUID docked;
    private final java.util.ArrayDeque<Project.Cell> kit = new java.util.ArrayDeque<>();
    private final List<Project.SpanRec> kitSpans = new java.util.ArrayList<>();
    private int kitWait;
    private @Nullable SeedUnpack seed;
    private static final RawAnimation GROW = RawAnimation.begin().thenPlay("animation.outpost_core.grow");
    private static final RawAnimation ACTIVE = RawAnimation.begin().thenLoop("animation.outpost_core.active");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EnergyBuffer buffer = new EnergyBuffer(FE_PER_TICK * 200, FE_PER_TICK * 4, 0, this::setChanged);
    private @Nullable UUID colonyId;
    private @Nullable UUID outpostId;
    private int growth;
    private int poweredTicks;
    private int windowTicks;
    private double powerFactor = 1;
    final Map<Item, Long> lastStock = new HashMap<>();
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            Colony c = colony();
            Colony.Outpost o = c == null ? null : outpost(c);
            return switch (i) {
                case 0 -> c == null || o == null ? 0 : c.outposts.indexOf(o) + 1;
                case 1 -> c == null ? 0 : ColonyTiers.maxOutposts(c.tier);
                case 2 -> o == null ? 0 : o.link.ordinal();
                case 3 -> o == null ? 0 : o.lastCatchUp;
                case 4 -> (int) Math.round(powerFactor * 100);
                case 5 -> o == null ? 0 : (int) Math.min(Integer.MAX_VALUE, o.profileTicks / 20);
                default -> o == null ? 0 : o.buffered();
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public OutpostCoreBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.OUTPOST_CORE.get(), pos, state);
    }

    /** Bind the core to its colony and outpost record (seed planting). */
    public void bind(UUID colony, UUID outpost) {
        this.colonyId = colony;
        this.outpostId = outpost;
        this.growth = 0;
        charge.fill(SEED_ENERGY);
        setChanged();
        triggerAnim("main", "grow");
    }

    /** Return the colony of the outpost, or null. */
    public @Nullable Colony colony() {
        return colonyId == null || Colonies.server() == null ? null : Colonies.registry().get(colonyId);
    }

    /** Return the outpost record of this core in the colony, or null. */
    public Colony.@Nullable Outpost outpost(Colony c) {
        return outpostId == null ? null : c.outpost(outpostId);
    }

    /** Return the outpost id, or null before planting. */
    public @Nullable UUID outpostId() {
        return outpostId;
    }

    /** Return true once the 200-tick growth is over. */
    public boolean grown() {
        return growth >= GROWTH_TICKS;
    }

    /** Return the share of the last full 1,200-tick window the core had its 256 FE/t (1 before the first window ends). */
    public double powerFactor() {
        return powerFactor;
    }

    /** Register a grown outpost's site for the linked stock view when its chunk loads (repeated registration is harmless). */
    @Override
    public void onLoad() {
        super.onLoad();
        Colony c = colony();
        Colony.Outpost o = c == null ? null : outpost(c);
        if (level instanceof ServerLevel sl && o != null && grown()) {
            dev.autarkia.storage.DomainStockData.get(sl).addOutpost(worldPosition, "Outpost " + (c.outposts.indexOf(o) + 1));
        }
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        Colony c = colony();
        if (c == null) {
            return;
        }
        Colony.Outpost o = outpost(c);
        if (o == null) {
            return;
        }
        if (seed != null) {
            long t0 = System.nanoTime();
            boolean done = seed.tick(sl, c, o, this);
            dev.autarkia.command.SystemProfiler.add(dev.autarkia.command.SystemProfiler.Part.SEED, t0);
            if (done) {
                seed = null;
                setChanged();
            }
        }
        if (!kit.isEmpty() || !kitSpans.isEmpty()) {
            buildKit(sl);
        }
        if (growth < GROWTH_TICKS) {
            if (++growth == GROWTH_TICKS) {
                Outposts.grown(sl, c, o);
                syncNow();
            }
            return;
        }
        windowTicks++;
        if (buffer.drain(FE_PER_TICK) == FE_PER_TICK) {
            poweredTicks++;
        }
        int spare = Math.min(buffer.stored(), FE_PER_TICK * 3);
        if (spare > 0) {
            buffer.drain(charge.fill(spare));
        }
        chargeDocked(sl);
        if (windowTicks >= Outposts.SAMPLE_TICKS) {
            powerFactor = (double) poweredTicks / windowTicks;
            windowTicks = 0;
            poweredTicks = 0;
            Outposts.sample(sl, c, o, this, -1);
        }
    }

    /**
     * Take the outpost kit of a Replicator Walker (05 section 8.2): cuts, then fills (dirt), then the plan cells, 4 per
     * tick, each fill and cell arriving as a {@code rising_block} in SCALE mode (6 ticks); the spans are strung 20 ticks
     * after the last cell. Cuts drop their blocks as items and never touch a block entity. The queue is saved.
     */
    public void queueKit(OutpostProjects.Kit k) {
        k.cuts().forEach(p -> kit.add(new Project.Cell(p.asLong(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState())));
        k.fills().forEach(p -> kit.add(new Project.Cell(p.asLong(), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState())));
        kit.addAll(k.cells());
        kitSpans.addAll(k.spans());
        setChanged();
    }

    /** Start the unpack of a seed that landed on this core (C3). */
    public void startSeed(SeedUnpack unpack) {
        seed = unpack;
        setChanged();
    }

    /** Return the running seed unpack, or null. */
    public @Nullable SeedUnpack seed() {
        return seed;
    }

    /** Return the kit steps still waiting (cuts, fills, cells and spans). */
    public int kitLeft() {
        return kit.size() + kitSpans.size();
    }

    private void buildKit(ServerLevel sl) {
        for (int i = 0; i < KIT_PER_TICK && !kit.isEmpty(); i++) {
            Project.Cell cell = kit.poll();
            BlockPos p = BlockPos.of(cell.pos());
            if (cell.state().isAir()) {
                if (sl.getBlockEntity(p) == null && !sl.getBlockState(p).isAir()) {
                    sl.destroyBlock(p, true);
                }
            } else {
                RisingBlockEntity.spawn(sl, p, cell.state(), 6);
            }
            setChanged();
        }
        if (!kit.isEmpty() || ++kitWait < 20) {
            return;
        }
        kitWait = 0;
        for (Project.SpanRec s : List.copyOf(kitSpans)) {
            dev.autarkia.energy.Grid.SpanResult r = dev.autarkia.energy.Grid.addSpan(sl, BlockPos.of(s.a()), BlockPos.of(s.b()), s.tier());
            if (r != dev.autarkia.energy.Grid.SpanResult.OK) {
                dev.autarkia.Autarkia.LOG.info("[colony] outpost kit span {} to {} not strung: {}", BlockPos.of(s.a()).toShortString(),
                    BlockPos.of(s.b()).toShortString(), r);
            }
            kitSpans.remove(s);
        }
        setChanged();
    }

    private void chargeDocked(ServerLevel sl) {
        dev.autarkia.worker.WorkerEntity w = docked != null && sl.getEntity(docked) instanceof dev.autarkia.worker.WorkerEntity we ? we : null;
        boolean mine = w != null && w.getBrain().getMemory(dev.autarkia.worker.WorkerMemories.CHARGER_POS.get())
            .map(net.minecraft.core.GlobalPos::pos).filter(worldPosition::equals).isPresent();
        if (w == null || !mine || w.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(dockSpot(w))) > 3.5 * 3.5) {
            if (w == null || !mine || w.state() != dev.autarkia.worker.WorkerEntity.State.RECHARGING) {
                docked = null;
            }
            return;
        }
        int give = Math.min(dev.autarkia.config.AutarkiaConfig.DOCK_CHARGE_RATE.get(), Math.min(charge.stored(), w.workerType.capacity() - w.energy()));
        if (give > 0) {
            charge.drain(give);
            w.setEnergy(w.energy() + give);
        }
    }

    /** Return the FE store the core charges workers from (the seed's 64,000 FE, refilled by surplus network power). */
    public EnergyBuffer charge() {
        return charge;
    }

    @Override
    public boolean serves(dev.autarkia.worker.WorkerEntity w) {
        return colonyId != null && w.workerType.fe();
    }

    @Override
    public boolean availableFor(UUID worker) {
        return docked == null || docked.equals(worker) || !(level instanceof ServerLevel sl && sl.getEntity(docked) instanceof dev.autarkia.worker.WorkerEntity);
    }

    @Override
    public boolean dock(dev.autarkia.worker.WorkerEntity w) {
        if (!availableFor(w.getUUID())) {
            return false;
        }
        docked = w.getUUID();
        return true;
    }

    @Override
    public void undock(dev.autarkia.worker.WorkerEntity w) {
        if (w.getUUID().equals(docked)) {
            docked = null;
        }
    }

    @Override
    public BlockPos dockSpot(dev.autarkia.worker.WorkerEntity w) {
        return w.workerType.flying() ? worldPosition.above(2) : worldPosition.relative(net.minecraft.core.Direction.NORTH);
    }

    @Override
    public double dockReach() {
        return 2.5;
    }

    @Override
    public java.util.Set<ColonySensors.Kind> colonyKinds() {
        return colonyId != null ? java.util.Set.of(ColonySensors.Kind.CHARGER) : java.util.Set.of();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Colony c = colony();
        if (level instanceof ServerLevel sl && c != null && outpostId != null) {
            Outposts.remove(sl, c, outpostId);
        }
    }

    @Override
    public Role gridRole() {
        return Role.CONSUMER;
    }

    @Override
    public EnergyBuffer gridBuffer() {
        return buffer;
    }

    @Override
    public int gridMaxOut() {
        return 0;
    }

    @Override
    public int gridMaxIn() {
        return buffer.room() > 0 ? FE_PER_TICK * 4 : 0;
    }

    @Override
    public int workDraw() {
        return grown() ? FE_PER_TICK : 0;
    }

    @Override
    public boolean gridValid() {
        return !isRemoved();
    }

    @Override
    public String jadeGroup() {
        return "colony";
    }

    @Override
    public void jadeLines(List<Component> out) {
        Colony c = colony();
        Colony.Outpost o = c == null ? null : outpost(c);
        if (c == null || o == null) {
            out.add(Component.translatable("jade.autarkia.outpost.unlinked"));
            return;
        }
        if (!grown()) {
            out.add(Component.translatable("jade.autarkia.outpost.growing", growth * 100 / GROWTH_TICKS));
        }
        out.add(Component.translatable("jade.autarkia.outpost.index", c.outposts.indexOf(o) + 1, ColonyTiers.maxOutposts(c.tier)));
        out.add(Component.translatable("jade.autarkia.outpost.link", Component.translatable("link.autarkia." + o.link.getSerializedName())));
        out.add(Component.translatable("jade.autarkia.outpost.catch_up", o.lastCatchUp));
        if (seed != null) {
            out.add(seed.jadeLine());
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.autarkia.outpost_core");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SlotMenu(AutarkiaMenus.OUTPOST_CORE.get(), id, inv, worldPosition, new ItemStacksResourceHandler(0), data, GuiLayouts.OUTPOST_CORE);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<OutpostCoreBlockEntity> main = new AnimationController<>("main", 0, test -> test.setAndContinue(ACTIVE));
        main.triggerableAnim("grow", GROW);
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
        charge.serialize(out.child("charge"));
        out.putInt("charge_capacity", charge.capacity());
        if (seed != null) {
            seed.save(out.child("seed"));
        }
        if (colonyId != null && outpostId != null) {
            out.store("colony", UUIDUtil.CODEC, colonyId);
            out.store("outpost", UUIDUtil.CODEC, outpostId);
        }
        out.putInt("growth", growth);
        out.putDouble("power_factor", powerFactor);
        if (!kit.isEmpty() || !kitSpans.isEmpty()) {
            out.store("kit", Project.Cell.CODEC.listOf(), List.copyOf(kit));
            out.store("kit_spans", Project.SpanRec.CODEC.listOf(), List.copyOf(kitSpans));
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
        charge.setCapacity(Math.max(SEED_ENERGY, in.getIntOr("charge_capacity", SEED_ENERGY)));
        in.child("charge").ifPresent(charge::deserialize);
        seed = in.child("seed").map(SeedUnpack::load).orElse(null);
        colonyId = in.read("colony", UUIDUtil.CODEC).orElse(null);
        outpostId = in.read("outpost", UUIDUtil.CODEC).orElse(null);
        growth = in.getIntOr("growth", GROWTH_TICKS);
        powerFactor = in.getDoubleOr("power_factor", 1);
        kit.clear();
        kitSpans.clear();
        in.read("kit", Project.Cell.CODEC.listOf()).ifPresent(kit::addAll);
        in.read("kit_spans", Project.SpanRec.CODEC.listOf()).ifPresent(kitSpans::addAll);
    }
}

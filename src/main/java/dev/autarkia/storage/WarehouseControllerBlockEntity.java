package dev.autarkia.storage;

import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.colony.ColonyMemory;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.EnergyBuffer;
import dev.autarkia.energy.GridMember;
import dev.autarkia.registry.AutarkiaAdvancements;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.AutarkiaMenus;
import dev.autarkia.registry.GuiLayouts;
import dev.autarkia.registry.JadeInfo;
import dev.autarkia.registry.SlotMenu;
import dev.autarkia.registry.Ticking;
import dev.autarkia.worker.WorkerEntity;
import dev.autarkia.worker.WorkerType;
import dev.autarkia.worker.WorkerUpgrades;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

/**
 * Warehouse controller (04 sections 2.4 and 2.5): indexes up to 256 racks in its cube area (racks report content
 * changes, rack placement or removal re-scans; no periodic scan), keeps stock per item and the crates that hold it,
 * reserves crate contents for requests ("available" = stock - reserved; a lifted crate is in transit and out of the
 * index), queues up to {@code warehouseQueueSize} ledger requests and posts one WAREHOUSE_DELIVER or WAREHOUSE_STORE job
 * per trip on the colony board. A trip moves one crate, two with a cargo upgrade on the Loader. 64 FE/t while requests
 * are open; without power the queue waits. It pushes its stock to {@link DomainStockData} once per
 * {@code stockSyncIntervalTicks} (main site of the linked view).
 */
public class WarehouseControllerBlockEntity extends AutarkiaBlockEntity implements GridMember, Ticking, JadeInfo, MenuProvider {
    public static final int MAX_RACKS = 256;
    public static final int FE_PER_TICK = 64;
    public static final int MAX_REQUEST = 1728;
    public static final int STUCK_TICKS = 2400;
    public static final int LIFT_TICKS = 40;
    public static final int HANDOVER_TICKS = 20;
    /** Measured ground speed of a pathing worker in blocks per tick per point of MOVEMENT_SPEED (W14 run: 19.3 blocks in 175 ticks at 0.20). */
    public static final double WALK_BLOCKS_PER_TICK_PER_SPEED = 0.55;
    public static final UUID STORE_PLAYER = net.minecraft.util.Util.NIL_UUID;
    private final EnergyBuffer buffer = new EnergyBuffer(FE_PER_TICK * 200, FE_PER_TICK * 4, 0, this::setChanged);
    private final List<BlockPos> racks = new ArrayList<>();
    private final Map<ItemResource, Long> stock = new LinkedHashMap<>();
    private final Map<ItemResource, List<LedgerRequest.Reservation>> where = new HashMap<>();
    private final Map<Long, Integer> reserved = new HashMap<>();
    private final List<LedgerRequest> requests = new ArrayList<>();
    private final List<long[]> finished = new ArrayList<>();
    private int overloaded;
    private int slotsUsed;
    private int slotsTotal;
    private int positionsFree;
    private boolean scanDirty = true;
    private boolean indexDirty = true;
    private boolean powered = true;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> racks.size();
                case 1 -> fillPercent();
                case 2 -> overloaded;
                case 3 -> openRequests();
                default -> positionsFree;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public WarehouseControllerBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.WAREHOUSE_CONTROLLER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel sl) {
            Warehouses.add(sl, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel sl) {
            Warehouses.remove(sl, worldPosition);
        }
    }

    /** Mark the index for a rebuild; a layout change also re-scans the area for racks. */
    public void markIndexDirty(boolean layout) {
        indexDirty = true;
        scanDirty |= layout;
    }

    private static long key(BlockPos rack, int index) {
        return rack.asLong() * 8 + index;
    }

    /** Return the indexed rack positions. */
    public List<BlockPos> racks() {
        return List.copyOf(racks);
    }

    /** Return the slot fill of the indexed crates in percent. */
    public int fillPercent() {
        return slotsTotal == 0 ? 0 : slotsUsed * 100 / slotsTotal;
    }

    /** Return the number of overloaded racks (load above the rating). */
    public int overloaded() {
        return overloaded;
    }

    /** Return the number of open requests. */
    public int openRequests() {
        return (int) requests.stream().filter(LedgerRequest::open).count();
    }

    /** Return the indexed stock per item (reserved amounts included). */
    public Map<ItemResource, Long> stock() {
        return stock;
    }

    /** Return the amount of an item that new requests may still take. */
    public long available(ItemResource item) {
        long n = 0;
        for (LedgerRequest.Reservation r : where.getOrDefault(item, List.of())) {
            n += Math.max(0, r.amount() - reserved.getOrDefault(key(r.rack(), r.index()), 0));
        }
        return n;
    }

    /** Return how many delivery requests ended FAILED or PARTIAL since the game time. */
    public int failedSince(long since) {
        return (int) finished.stream().filter(f -> f[0] >= since && f[1] == 0).count();
    }

    /** Return the durations in ticks of the last 20 delivery requests that ended DONE. */
    public List<Long> recentDeliveries() {
        return finished.stream().filter(f -> f[1] == 1).map(f -> f[2]).toList();
    }

    private void ended(LedgerRequest r) {
        if (r.store) {
            return;
        }
        long now = level.getGameTime();
        finished.add(new long[] {now, r.status == LedgerRequest.Status.DONE ? 1 : 0, now - r.created});
        finished.removeIf(f -> now - f[0] > 24000 && f[1] == 0);
        while (finished.stream().filter(f -> f[1] == 1).count() > 20) {
            finished.remove(finished.stream().filter(f -> f[1] == 1).findFirst().orElseThrow());
        }
    }

    /** Return the open and recent requests of a terminal, oldest first. */
    public List<LedgerRequest> requests(BlockPos terminal) {
        return requests.stream().filter(r -> r.terminal.equals(terminal)).toList();
    }

    /** Return the request with the id, or null. */
    public @Nullable LedgerRequest request(UUID id) {
        for (LedgerRequest r : requests) {
            if (r.id.equals(id)) {
                return r;
            }
        }
        return null;
    }

    /** Return how many Loaders the colony of the controller has. */
    public int loaders() {
        Colony c = colony();
        return c == null ? 0 : c.workerCount(WorkerType.LOADER.id());
    }

    /** Return the colony whose domain holds the controller, or null. */
    public @Nullable Colony colony() {
        return Colonies.server() == null ? null : Colonies.at(serverLevel(), worldPosition);
    }

    private void scan() {
        racks.clear();
        int r = AutarkiaConfig.WAREHOUSE_RADIUS.get();
        ServerLevel sl = serverLevel();
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-r, -r, -r), worldPosition.offset(r, r, r))) {
            if (sl.isLoaded(p) && sl.getBlockEntity(p) instanceof RackBlockEntity && Warehouses.ownerOf(sl, p) == this) {
                found.add(p.immutable());
            }
        }
        found.sort(Comparator.comparingDouble((BlockPos p) -> p.distSqr(worldPosition)).thenComparingLong(BlockPos::asLong));
        racks.addAll(found.subList(0, Math.min(MAX_RACKS, found.size())));
        scanDirty = false;
    }

    private void reindex() {
        stock.clear();
        where.clear();
        overloaded = 0;
        slotsUsed = 0;
        slotsTotal = 0;
        positionsFree = 0;
        ServerLevel sl = serverLevel();
        for (BlockPos p : racks) {
            if (!(sl.getBlockEntity(p) instanceof RackBlockEntity rack)) {
                continue;
            }
            overloaded += rack.ratio() > 1.0 ? 1 : 0;
            for (int i = 0; i < rack.positionCount(); i++) {
                if (!rack.present(i)) {
                    positionsFree++;
                    continue;
                }
                Map<ItemResource, Integer> here = new LinkedHashMap<>();
                ItemStacksResourceHandler h = rack.position(i);
                for (int s = 0; s < h.size(); s++) {
                    slotsTotal++;
                    int a = h.getAmountAsInt(s);
                    if (a > 0) {
                        slotsUsed++;
                        here.merge(h.getResource(s), a, Integer::sum);
                    }
                }
                for (Map.Entry<ItemResource, Integer> e : here.entrySet()) {
                    stock.merge(e.getKey(), (long) e.getValue(), Long::sum);
                    where.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(new LedgerRequest.Reservation(p, i, e.getValue()));
                }
            }
        }
        indexDirty = false;
    }

    private List<LedgerRequest.Reservation> cratesFor(ItemResource item, BlockPos terminal) {
        List<LedgerRequest.Reservation> out = new ArrayList<>();
        for (LedgerRequest.Reservation r : where.getOrDefault(item, List.of())) {
            int free = r.amount() - reserved.getOrDefault(key(r.rack(), r.index()), 0);
            if (free > 0) {
                out.add(new LedgerRequest.Reservation(r.rack(), r.index(), free));
            }
        }
        out.sort(Comparator.comparingInt((LedgerRequest.Reservation r) -> -r.amount()).thenComparingDouble(r -> r.rack().distSqr(terminal))
            .thenComparingLong(r -> r.rack().asLong()).thenComparingInt(LedgerRequest.Reservation::index));
        return out;
    }

    private void plan(LedgerRequest r) {
        int need = r.remaining() - r.plan.stream().mapToInt(LedgerRequest.Reservation::amount).sum();
        for (LedgerRequest.Reservation f : cratesFor(ItemResource.of(r.item), r.terminal)) {
            if (need <= 0) {
                break;
            }
            LedgerRequest.Reservation take = new LedgerRequest.Reservation(f.rack(), f.index(), Math.min(need, f.amount()));
            r.plan.add(take);
            reserved.merge(key(take.rack(), take.index()), take.amount(), Integer::sum);
            need -= take.amount();
        }
    }

    private void release(LedgerRequest.Reservation t) {
        reserved.computeIfPresent(key(t.rack(), t.index()), (k, v) -> v - t.amount() <= 0 ? null : v - t.amount());
    }

    private void releaseAll(LedgerRequest r) {
        r.plan.forEach(this::release);
        r.plan.clear();
    }

    /**
     * Queue a delivery of the item, clamped to 1 to 1,728 and to the available amount, and reserve it in the fewest
     * crates (largest first, then nearest the terminal); return the refusal key, or null when queued.
     */
    public @Nullable String submit(ServerPlayer player, BlockPos terminal, ItemStack item, int amount) {
        if (openRequests() >= AutarkiaConfig.WAREHOUSE_QUEUE_SIZE.get()) {
            return "message.autarkia.ledger.queue_full";
        }
        int clamped = (int) Math.min(Math.min(MAX_REQUEST, Math.max(1, amount)), available(ItemResource.of(item)));
        if (clamped <= 0) {
            return "message.autarkia.ledger.none_available";
        }
        LedgerRequest r = new LedgerRequest(player.getUUID(), terminal, item, clamped, false, level.getGameTime());
        r.requester = player;
        plan(r);
        add(r);
        return null;
    }

    /** Queue a put-away of the terminal input buffer unless one is already open for the terminal. */
    public void submitStore(BlockPos terminal) {
        if (requests.stream().anyMatch(r -> r.open() && r.store && r.terminal.equals(terminal)) || openRequests() >= AutarkiaConfig.WAREHOUSE_QUEUE_SIZE.get()) {
            return;
        }
        add(new LedgerRequest(STORE_PLAYER, terminal, ItemStack.EMPTY, 0, true, level.getGameTime()));
    }

    private void add(LedgerRequest r) {
        requests.add(r);
        while (requests.size() > 48 && !requests.get(0).open()) {
            requests.remove(0);
        }
        setChanged();
    }

    @Override
    public void serverTick() {
        ServerLevel sl = serverLevel();
        if (scanDirty) {
            scan();
            indexDirty = true;
        }
        if (indexDirty && (sl.getGameTime() + worldPosition.asLong()) % 10 == 0) {
            reindex();
        }
        if ((sl.getGameTime() + worldPosition.asLong()) % AutarkiaConfig.STOCK_SYNC_INTERVAL_TICKS.get() == 0) {
            DomainStockData.get(sl).push(DomainStockData.Kind.WAREHOUSE, worldPosition, null, "", stock, slotsUsed, slotsTotal, sl.getGameTime());
        }
        if (openRequests() == 0) {
            powered = true;
            return;
        }
        powered = buffer.drain(FE_PER_TICK) == FE_PER_TICK;
        if (!powered) {
            return;
        }
        Colony c = colony();
        for (LedgerRequest r : List.copyOf(requests)) {
            if (r.open()) {
                step(sl, c, r);
            }
        }
    }

    private void step(ServerLevel sl, @Nullable Colony c, LedgerRequest r) {
        long now = sl.getGameTime();
        if (r.status != LedgerRequest.Status.QUEUED && now - r.assignedAt > STUCK_TICKS) {
            fail(r, "loader_stuck");
            ColonyMemory.at(sl, worldPosition, "loader_stuck", worldPosition.toShortString());
            return;
        }
        if (c == null || c.workerCount(WorkerType.LOADER.id()) == 0) {
            if (r.status == LedgerRequest.Status.QUEUED) {
                r.reason = "no_loader";
            }
            return;
        }
        ColonyRuntime rt = ColonyRuntime.of(c);
        ColonyJob job = r.jobId == null ? null : rt.jobs.get(r.jobId);
        if (r.status == LedgerRequest.Status.QUEUED) {
            r.reason = "";
            if (job == null) {
                if (!r.store && r.plan.isEmpty()) {
                    finishShort(r);
                    return;
                }
                job = postJob(sl, rt, r);
            }
            if (job != null && job.status == ColonyJob.Status.ASSIGNED && job.assignee != null) {
                r.status = LedgerRequest.Status.ASSIGNED;
                r.loader = job.assignee;
                r.assignedAt = now;
                r.etaTicks = eta(sl, r);
            }
        } else if (r.status == LedgerRequest.Status.ASSIGNED && r.trip.isEmpty() && (job == null || job.status == ColonyJob.Status.OPEN)) {
            r.status = LedgerRequest.Status.QUEUED;
        }
    }

    private @Nullable ColonyJob postJob(ServerLevel sl, ColonyRuntime rt, LedgerRequest r) {
        BlockPos first = r.store ? storeTarget(sl, r) : r.plan.get(0).rack();
        if (first == null) {
            fail(r, "warehouse_full");
            return null;
        }
        ColonyJob job = new ColonyJob(r.store ? ColonyJob.Type.WAREHOUSE_STORE : ColonyJob.Type.WAREHOUSE_DELIVER, null, first, null,
            r.item.isEmpty() ? null : r.item.getItem(), r.remaining(), 20, 0, sl.getGameTime(), r.terminal, null, 0);
        job.requestId = r.id;
        rt.jobs.add(job);
        if (rt.jobs.get(job.id) == null) {
            return null;
        }
        r.jobId = job.id;
        return job;
    }

    private int eta(ServerLevel sl, LedgerRequest r) {
        Vec3 loader = r.loader != null && sl.getEntity(r.loader) instanceof WorkerEntity w ? w.position() : Vec3.atCenterOf(worldPosition);
        BlockPos rack = r.plan.isEmpty() ? r.terminal : r.plan.get(0).rack();
        double path = Math.sqrt(loader.distanceToSqr(Vec3.atCenterOf(rack))) + Math.sqrt(rack.distSqr(r.terminal));
        return (int) Math.ceil(path / (WorkerType.LOADER.speed() * WALK_BLOCKS_PER_TICK_PER_SPEED)) + LIFT_TICKS + HANDOVER_TICKS;
    }

    private @Nullable BlockPos storeTarget(ServerLevel sl, LedgerRequest r) {
        LedgerRequest.Reservation t = storeCrate(sl, r);
        return t == null ? null : t.index() < 0 ? r.terminal : t.rack();
    }

    private LedgerRequest.@Nullable Reservation storeCrate(ServerLevel sl, LedgerRequest r) {
        return RackSearch.storeCrate(sl, racks, (p, i) -> reserved.containsKey(key(p, i)), r.terminal);
    }

    private LedgerRequest.@Nullable Reservation freePosition(ServerLevel sl, BlockPos near) {
        return RackSearch.freePosition(sl, racks, near);
    }

    /**
     * Start the trip of the Loader on a request's job: up to one crate (two with a cargo upgrade) of the plan, or the
     * put-away crate; the request goes FETCHING. Returns the crate positions in visit order (index -1: the terminal
     * crate slot), empty when the request has nothing to move.
     */
    public List<LedgerRequest.Reservation> beginTrip(LedgerRequest r, WorkerEntity w) {
        ServerLevel sl = serverLevel();
        r.trip.clear();
        if (r.store) {
            LedgerRequest.Reservation t = storeCrate(sl, r);
            if (t != null) {
                r.trip.add(t);
                if (t.index() >= 0) {
                    reserved.merge(key(t.rack(), t.index()), 0, Integer::sum);
                }
            }
        } else {
            int n = w.upgrades.count(WorkerUpgrades.Kind.CARGO) > 0 ? 2 : 1;
            for (int i = 0; i < Math.min(n, r.plan.size()); i++) {
                r.trip.add(r.plan.get(i));
            }
        }
        r.status = LedgerRequest.Status.FETCHING;
        r.loader = w.getUUID();
        return List.copyOf(r.trip);
    }

    /** Lift the crate of a trip stop off its rack (or the empty crate out of the terminal slot); returns it with its contents, or empty when gone. */
    public ItemStack lift(LedgerRequest r, LedgerRequest.Reservation t) {
        ServerLevel sl = serverLevel();
        if (t.index() < 0) {
            return sl.getBlockEntity(r.terminal) instanceof LedgerTerminalBlockEntity term ? term.takeEmptyCrate() : ItemStack.EMPTY;
        }
        if (!r.store) {
            r.plan.remove(t);
            release(t);
        } else {
            reserved.remove(key(t.rack(), t.index()));
        }
        if (!(sl.getBlockEntity(t.rack()) instanceof RackBlockEntity rack) || t.index() >= rack.positionCount() || !rack.present(t.index())) {
            return ItemStack.EMPTY;
        }
        ItemStack crate = rack.crate(t.index());
        rack.setCrate(t.index(), ItemStack.EMPTY);
        r.status = LedgerRequest.Status.DELIVERING;
        return crate;
    }

    /**
     * Hand the trip's items over at the terminal: for a delivery, take up to the reserved amount of the item out of each
     * crate (fewer when the crate holds fewer) and give it to the requesting player when within 6 blocks of the terminal,
     * the rest to the output buffer; for a put-away, fill the crates from the input buffer. Returns false while the
     * output buffer is full; the items stay in the crate.
     */
    public boolean handOver(LedgerRequest r, List<ItemStack> crates) {
        ServerLevel sl = serverLevel();
        if (!(sl.getBlockEntity(r.terminal) instanceof LedgerTerminalBlockEntity term)) {
            return true;
        }
        if (r.store) {
            for (ItemStack crate : crates) {
                term.fillCrate(crate);
            }
            return true;
        }
        ServerPlayer player = r.requester != null && !r.requester.isRemoved() ? r.requester : sl.getServer().getPlayerList().getPlayer(r.player);
        boolean near = player != null && player.level() == sl && player.position().distanceToSqr(Vec3.atCenterOf(r.terminal)) <= 36;
        boolean done = true;
        for (int i = 0; i < crates.size(); i++) {
            int want = i < r.trip.size() ? Math.min(r.trip.get(i).amount(), r.remaining()) : r.remaining();
            ItemStack taken = CrateItems.take(crates.get(i), r.item, want);
            if (taken.isEmpty()) {
                continue;
            }
            int total = taken.getCount();
            if (near) {
                player.getInventory().add(taken);
                if (taken.getCount() < total) {
                    AutarkiaAdvancements.award(player, "logistics/fetch");
                    AutarkiaAdvancements.award(player, "workers/loader_delivery");
                }
            }
            ItemStack rest = taken.isEmpty() ? ItemStack.EMPTY : term.output(taken);
            if (!rest.isEmpty()) {
                CrateItems.put(crates.get(i), rest);
                done = false;
            }
            r.delivered += total - rest.getCount();
        }
        return done;
    }

    /** Fail the request after the Loader waited 600 ticks at a full output buffer. */
    public void outputFull(LedgerRequest r) {
        fail(r, "output_full");
    }

    /**
     * Put a crate back: at its trip position when free, else at the nearest free position of the warehouse, else as a
     * crate block next to the terminal with the "Warehouse full" warning.
     */
    public void returnCrate(LedgerRequest r, LedgerRequest.Reservation t, ItemStack crate) {
        if (crate.isEmpty()) {
            return;
        }
        ServerLevel sl = serverLevel();
        if (t.index() >= 0 && sl.getBlockEntity(t.rack()) instanceof RackBlockEntity rack && t.index() < rack.positionCount() && !rack.present(t.index())) {
            rack.setCrate(t.index(), crate);
            return;
        }
        LedgerRequest.Reservation free = freePosition(sl, t.rack());
        if (free != null && sl.getBlockEntity(free.rack()) instanceof RackBlockEntity rack) {
            rack.setCrate(free.index(), crate);
            return;
        }
        FallingCrateEntity.land(sl, r.terminal.above(), crate, Vec3.atCenterOf(r.terminal.above()));
        if (sl.getBlockEntity(r.terminal) instanceof LedgerTerminalBlockEntity term) {
            term.warn("warehouse_full");
        }
    }

    /** Return the position a trip stop's crate goes back to (its rack, or the nearest free position for a terminal crate). */
    public BlockPos returnPos(LedgerRequest r, LedgerRequest.Reservation t) {
        if (t.index() >= 0) {
            return t.rack();
        }
        LedgerRequest.Reservation free = freePosition(serverLevel(), r.terminal);
        return free == null ? r.terminal : free.rack();
    }

    /** Close the trip: a delivery with items left plans more crates and queues the next trip, else it ends DONE or PARTIAL. */
    public void tripFinished(LedgerRequest r) {
        r.trip.clear();
        r.jobId = null;
        if (!r.open()) {
            return;
        }
        if (r.store) {
            r.status = LedgerRequest.Status.DONE;
            return;
        }
        if (r.remaining() <= 0) {
            r.status = LedgerRequest.Status.DONE;
            releaseAll(r);
            ended(r);
            return;
        }
        if (r.plan.isEmpty()) {
            reindex();
            plan(r);
        }
        if (r.plan.isEmpty()) {
            finishShort(r);
        } else {
            r.status = LedgerRequest.Status.QUEUED;
        }
    }

    private void finishShort(LedgerRequest r) {
        r.status = r.delivered > 0 ? LedgerRequest.Status.PARTIAL : LedgerRequest.Status.FAILED;
        r.reason = r.delivered > 0 ? "partial" : "none_available";
        releaseAll(r);
        ended(r);
    }

    private void fail(LedgerRequest r, String reason) {
        r.status = LedgerRequest.Status.FAILED;
        r.reason = reason;
        releaseAll(r);
        ended(r);
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
        return openRequests() > 0 ? FE_PER_TICK : 0;
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
        out.add(Component.translatable("jade.autarkia.warehouse.summary", racks.size(), fillPercent(), overloaded));
        out.add(Component.translatable("jade.autarkia.warehouse.queue", openRequests(), loaders()));
        if (!powered) {
            out.add(Component.translatable("jade.autarkia.warehouse.no_power"));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SlotMenu(AutarkiaMenus.WAREHOUSE_CONTROLLER.get(), id, inv, worldPosition, new ItemStacksResourceHandler(0), data,
            GuiLayouts.WAREHOUSE_CONTROLLER);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        buffer.serialize(out.child("energy"));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("energy").ifPresent(buffer::deserialize);
    }
}

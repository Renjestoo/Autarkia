package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.storage.DepotBlockEntity;
import dev.autarkia.storage.DomainStockData;
import dev.autarkia.worker.WorkerEntity;
import dev.autarkia.worker.WorkerType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * Outposts (05 section 8.1): founding from an {@code outpost_seed} (64 to 512 blocks from the Core, 64 from other
 * outposts, at most {@link ColonyTiers#maxOutposts}), the chunk ticket of the core chunk ({@code autarkia:outpost}, kill
 * switch {@code outpost.chunkTickets}), and the production profile and catch-up credit every 1,200 ticks:
 * <ol>
 * <li>with every chunk of the outpost square loaded, the profile r_i is the EMA (alpha 0.25 per 1,200 ticks) of the net
 * items of type i that entered the outpost depots per tick, and the credit clock moves on;</li>
 * <li>else (abstract mode, or a forced credit) g = min(gap, {@code outpost.catchUpCapTicks}); no credit below 24,000
 * profile ticks; p = the core's power factor (the share of its 256 FE/t it got, standing for supply over demand of the
 * outpost network; no outpost generator burns depot fuel, so the fuel factor is 1);</li>
 * <li>add_i = floor(r_i x g x {@code outpost.catchUpEfficiency} x p), minus what really entered the depots during the
 * gap (machines in the forced core chunk keep running); ore products are capped by the claimed deposit's estimated
 * count, which goes down;</li>
 * <li>items go into the depots, the rest into the virtual buffer (max 4,096, flushed first next time), the excess is
 * lost.</li>
 * </ol>
 */
public final class Outposts {
    public static final int SAMPLE_TICKS = 1200;
    public static final int MIN_PROFILE_TICKS = 24000;
    public static final int BUFFER_CAP = 4096;
    public static final TicketController TICKETS = new TicketController(Autarkia.id("outpost"), Outposts::validate);

    private Outposts() {
    }

    /** Register the ticket controller (mod bus). */
    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterTicketControllersEvent e) -> e.register(TICKETS));
    }

    private static void validate(ServerLevel level, TicketHelper helper) {
        for (BlockPos owner : List.copyOf(helper.getBlockTickets().keySet())) {
            boolean known = Colonies.server() != null && Colonies.registry().all().stream().anyMatch(c -> c.dimension.equals(level.dimension())
                && c.outposts.stream().anyMatch(o -> o.corePos.equals(owner)));
            if (!known) {
                helper.removeAllTickets(owner);
            }
        }
    }

    /** Return the translation key why an outpost cannot be founded at the site, or null when it can. */
    public static @Nullable String invalid(ServerLevel level, Colony c, BlockPos site) {
        if (c.tier < 4 || c.state != Colony.State.ACTIVE) {
            return "message.autarkia.outpost.tier";
        }
        if (c.outposts.size() >= ColonyTiers.maxOutposts(c.tier)) {
            return "message.autarkia.outpost.max";
        }
        double d = horizontal(c.corePos, site);
        if (d < AutarkiaConfig.OUTPOST_MIN_SPACING.get() || d > AutarkiaConfig.OUTPOST_MAX_DISTANCE.get()) {
            return "message.autarkia.outpost.distance";
        }
        for (Colony.Outpost o : c.outposts) {
            if (horizontal(o.corePos, site) < AutarkiaConfig.OUTPOST_MIN_SPACING.get()) {
                return "message.autarkia.outpost.spacing";
            }
        }
        if (Colonies.overlapsOther(level, c, site.getX(), site.getZ(), ColonyTiers.outpostHalf(c.tier))) {
            return "message.autarkia.outpost.overlap";
        }
        return null;
    }

    /** Return the horizontal distance between two positions. */
    public static double horizontal(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Plant the seed a worker carries at the site (FOUND_OUTPOST) with the plan's core state (null: unrotated); the seed is
     * consumed. Returns false when it cannot.
     */
    public static boolean found(ServerLevel level, Colony c, BlockPos site, @Nullable BlockState coreState, WorkerEntity carrier) {
        int slot = -1;
        for (int i = 0; i < carrier.cargo.size() && slot < 0; i++) {
            slot = carrier.cargo.get(i).is(AutarkiaItems.OUTPOST_SEED.get()) ? i : -1;
        }
        String why = slot < 0 ? "no outpost_seed in the cargo" : invalid(level, c, site);
        if (why == null && !plant(level, c, site, coreState, carrier)) {
            why = "the 2 x 2 x 2 space is not free";
        }
        if (why != null) {
            Autarkia.LOG.info("[colony] {} could not found an outpost at {}: {}", c.name, site.toShortString(), why);
            return false;
        }
        carrier.cargo.get(slot).shrink(1);
        carrier.syncCargo();
        return true;
    }

    /**
     * Place a growing outpost core with its center at the site and add the outpost record; returns false when blocked.
     * The core takes coreState (the rotated core cell of the outpost plan, so the plan's other cells fit around its
     * footprint), or the default state when null. The planting entity (the carrier or the player) does not count as an
     * obstruction of the core's space.
     */
    public static boolean plant(ServerLevel level, Colony c, BlockPos site, @Nullable BlockState coreState, net.minecraft.world.entity.@Nullable Entity planter) {
        Block block = AutarkiaBlocks.OUTPOST_CORE.get();
        BlockState state = (coreState != null && coreState.is(block) ? coreState : block.defaultBlockState()).setValue(BaseMultiblock.CENTER, true);
        if (!(block instanceof BaseMultiblock mb) || !mb.canPlace(level, site, state, planter, false)) {
            return false;
        }
        level.setBlock(site, state, Block.UPDATE_ALL);
        if (!(level.getBlockEntity(site) instanceof OutpostCoreBlockEntity core)) {
            return false;
        }
        Colony.Outpost o = Colony.Outpost.create(site.immutable(), level.getGameTime());
        c.outposts.add(o);
        core.bind(c.id, o.id);
        claim(c, o);
        Colonies.rebuildIndex();
        Colonies.changed();
        level.playSound(null, site, dev.autarkia.registry.AutarkiaSounds.of("outpost_seed_plant"), SoundSource.BLOCKS, 1f, 1f);
        ColonyEvents.fire(c, "outpost_founded");
        ColonyRuntime.of(c).log("Outpost founded at " + site.toShortString());
        Autarkia.LOG.info("[colony] {} founded outpost {} at {}", c.name, c.outposts.size(), site.toShortString());
        return true;
    }

    private static void claim(Colony c, Colony.Outpost o) {
        Colony.Deposit best = null;
        for (Colony.Deposit d : c.deposits) {
            if (d.outpost().isEmpty() && horizontal(d.pos(), o.corePos) <= 32 && (best == null || d.pos().distSqr(o.corePos) < best.pos().distSqr(o.corePos))) {
                best = d;
            }
        }
        if (best != null) {
            c.deposits.set(c.deposits.indexOf(best), best.claimedBy(o.id));
        }
    }

    /**
     * Finish the growth: hold the core chunk ticket, register the outpost site for the linked stock view, and move the
     * nearest idle builder and idle carrier of the colony to the outpost (their new home); a busy worker is not moved,
     * since a recall drops its order and the items it carries for it.
     */
    static void grown(ServerLevel level, Colony c, Colony.Outpost o) {
        ticket(level, o.corePos, true);
        DomainStockData.get(level).addOutpost(o.corePos, "Outpost " + (c.outposts.indexOf(o) + 1));
        WorkerEntity builder = nearest(level, c, o.corePos, w -> w.workerType == WorkerType.BUILDER_DRONE && w.isIdle());
        WorkerEntity carrier = nearest(level, c, o.corePos, w -> w.workerType.jobs().contains(ColonyJob.Type.FOUND_OUTPOST) && w.isIdle());
        for (WorkerEntity w : java.util.Arrays.asList(builder, carrier)) {
            if (w != null) {
                w.recall(o.corePos);
            }
        }
        ColonyRuntime.of(c).log("Outpost " + (c.outposts.indexOf(o) + 1) + " is grown");
    }

    private static @Nullable WorkerEntity nearest(ServerLevel level, Colony c, BlockPos to, java.util.function.Predicate<WorkerEntity> filter) {
        WorkerEntity best = null;
        for (Colony.WorkerEntry e : c.workers.values()) {
            if (level.getEntity(e.id()) instanceof WorkerEntity w && filter.test(w) && (best == null || w.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(to))
                < best.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(to)))) {
                best = w;
            }
        }
        return best;
    }

    /** Add or remove the chunk ticket of an outpost core (only the chunk of the core center). */
    public static void ticket(ServerLevel level, BlockPos core, boolean add) {
        if (add && !AutarkiaConfig.OUTPOST_CHUNK_TICKETS.get()) {
            return;
        }
        TICKETS.forceChunk(level, core, core.getX() >> 4, core.getZ() >> 4, add, true);
    }

    /** Remove an outpost: its record, ticket, deposit claim and stock site. */
    public static void remove(ServerLevel level, Colony c, UUID outpostId) {
        Colony.Outpost o = c.outpost(outpostId);
        if (o == null) {
            return;
        }
        c.outposts.remove(o);
        ticket(level, o.corePos, false);
        for (int i = 0; i < c.deposits.size(); i++) {
            Colony.Deposit d = c.deposits.get(i);
            if (d.outpost().filter(outpostId::equals).isPresent()) {
                c.deposits.set(i, new Colony.Deposit(d.id(), d.ore(), d.pos(), d.estCount(), d.found(), java.util.Optional.empty()));
            }
        }
        DomainStockData.get(level).removeOutpost(o.corePos);
        Colonies.rebuildIndex();
        Colonies.changed();
        ColonyRuntime.of(c).log("Outpost removed at " + o.corePos.toShortString());
    }

    /** Return the depots of an outpost: depots within 16 blocks of its core, core chunk first. */
    public static List<DepotBlockEntity> depots(ServerLevel level, Colony.Outpost o) {
        List<DepotBlockEntity> out = new ArrayList<>();
        int cx = o.corePos.getX() >> 4;
        int cz = o.corePos.getZ() >> 4;
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                net.minecraft.world.level.chunk.LevelChunk ch = level.getChunkSource().getChunkNow(x, z);
                if (ch == null) {
                    continue;
                }
                for (net.minecraft.world.level.block.entity.BlockEntity be : ch.getBlockEntities().values()) {
                    if (be instanceof DepotBlockEntity d && horizontal(d.getBlockPos(), o.corePos) <= 16) {
                        out.add(d);
                    }
                }
            }
        }
        out.sort(Comparator.comparingInt((DepotBlockEntity d) -> (d.getBlockPos().getX() >> 4) == cx && (d.getBlockPos().getZ() >> 4) == cz ? 0 : 1)
            .thenComparingLong(d -> d.getBlockPos().asLong()));
        return out;
    }

    private static Map<Item, Long> stock(List<DepotBlockEntity> depots) {
        Map<Item, Long> out = new LinkedHashMap<>();
        for (DepotBlockEntity d : depots) {
            d.stock().forEach((res, n) -> out.merge(res.getItem(), n, Long::sum));
        }
        return out;
    }

    /** Return whether every chunk of the outpost square is loaded (full simulation). */
    public static boolean simulated(ServerLevel level, Colony c, Colony.Outpost o) {
        int h = ColonyTiers.outpostHalf(c.tier);
        for (int x = (o.corePos.getX() - h) >> 4; x <= (o.corePos.getX() + h) >> 4; x++) {
            for (int z = (o.corePos.getZ() - h) >> 4; z <= (o.corePos.getZ() + h) >> 4; z++) {
                if (!level.hasChunk(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Measure the profile or credit the catch-up of an outpost (class steps 1 to 4); a non-negative forcedGap credits that
     * many ticks at once ({@code /autarkia outpost credit}). Returns the items credited.
     */
    public static int sample(ServerLevel level, Colony c, Colony.Outpost o, OutpostCoreBlockEntity core, long forcedGap) {
        long now = level.getGameTime();
        List<DepotBlockEntity> depots = depots(level, o);
        flush(o, depots);
        Map<Item, Long> stock = stock(depots);
        if (forcedGap < 0 && simulated(level, c, o)) {
            for (Map.Entry<Item, Long> e : stock.entrySet()) {
                double rate = Math.max(0, e.getValue() - core.lastStock.getOrDefault(e.getKey(), 0L)) / (double) SAMPLE_TICKS;
                o.profile.merge(e.getKey(), rate, (old, r) -> old + 0.25 * (r - old));
            }
            o.profile.replaceAll((item, r) -> stock.containsKey(item) ? r : r * 0.75);
            o.profileTicks += SAMPLE_TICKS;
            o.lastCredit = now;
            core.lastStock.clear();
            core.lastStock.putAll(stock);
            Colonies.changed();
            return 0;
        }
        long g = Math.min(forcedGap >= 0 ? forcedGap : now - o.lastCredit, AutarkiaConfig.OUTPOST_CATCH_UP_CAP_TICKS.get());
        int total = 0;
        if (o.profileTicks >= MIN_PROFILE_TICKS && g > 0) {
            double p = Math.min(1, core.powerFactor());
            double eta = AutarkiaConfig.OUTPOST_CATCH_UP_EFFICIENCY.get();
            Colony.Deposit deposit = c.deposits.stream().filter(d -> d.outpost().filter(o.id::equals).isPresent()).findFirst().orElse(null);
            int oreLeft = deposit == null ? Integer.MAX_VALUE : deposit.estCount();
            for (Map.Entry<Item, Double> e : o.profile.entrySet()) {
                long add = (long) Math.floor(e.getValue() * g * eta * p);
                if (forcedGap < 0) {
                    add -= Math.max(0, stock.getOrDefault(e.getKey(), 0L) - core.lastStock.getOrDefault(e.getKey(), 0L));
                }
                if (add <= 0) {
                    continue;
                }
                if (deposit != null && oreProduct(e.getKey(), deposit.ore())) {
                    add = Math.min(add, oreLeft);
                    oreLeft -= (int) add;
                    Deposits.mine(c, deposit.id(), (int) add);
                }
                total += (int) add;
                insert(o, depots, e.getKey(), (int) Math.min(Integer.MAX_VALUE, add));
            }
            ColonyRuntime.of(c).log("Outpost " + (c.outposts.indexOf(o) + 1) + " catch-up +" + total + " items for " + g + " ticks");
            Autarkia.LOG.info("[colony] {} outpost {} catch-up: {} ticks, p {}, +{} items", c.name, c.outposts.indexOf(o) + 1, g,
                String.format(java.util.Locale.ROOT, "%.2f", p), total);
        }
        o.lastCatchUp = total;
        o.lastCredit = now;
        core.lastStock.clear();
        core.lastStock.putAll(stock(depots));
        Colonies.changed();
        return total;
    }

    private static boolean oreProduct(Item item, String ore) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        return path.equals("raw_" + ore) || path.equals("crushed_" + ore) || path.equals("washed_" + ore) || path.equals("pure_" + ore)
            || path.equals(ore + "_concentrate");
    }

    private static void insert(Colony.Outpost o, List<DepotBlockEntity> depots, Item item, int count) {
        int left = count;
        ItemResource res = ItemResource.of(new ItemStack(item));
        for (DepotBlockEntity d : depots) {
            if (left <= 0) {
                break;
            }
            try (Transaction tx = Transaction.openRoot()) {
                left -= d.items().insert(res, left, tx);
                tx.commit();
            }
        }
        if (left > 0) {
            int room = Math.max(0, BUFFER_CAP - o.buffered());
            int kept = Math.min(room, left);
            if (kept > 0) {
                o.virtualBuffer.merge(item, kept, Integer::sum);
            }
        }
    }

    private static void flush(Colony.Outpost o, List<DepotBlockEntity> depots) {
        if (o.virtualBuffer.isEmpty()) {
            return;
        }
        Map<Item, Integer> held = new LinkedHashMap<>(o.virtualBuffer);
        o.virtualBuffer.clear();
        held.forEach((item, n) -> insert(o, depots, item, n));
    }

    /** Return one text line per outpost for {@code /autarkia outpost list}. */
    public static List<String> list(Colony c) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < c.outposts.size(); i++) {
            Colony.Outpost o = c.outposts.get(i);
            out.add(String.format(java.util.Locale.ROOT, "%d %s at %s link %s profile %d ticks %d items/day buffer %d last +%d", i + 1,
                o.id.toString().substring(0, 8), o.corePos.toShortString(), o.link.getSerializedName(), o.profileTicks,
                Math.round(o.profile.values().stream().mapToDouble(Double::doubleValue).sum() * 24000), o.buffered(), o.lastCatchUp));
        }
        return out;
    }

    /** Return the outpost whose id starts with the prefix (or whose 1-based number it is), or null. */
    public static Colony.@Nullable Outpost find(Colony c, String key) {
        for (int i = 0; i < c.outposts.size(); i++) {
            Colony.Outpost o = c.outposts.get(i);
            if (o.id.toString().startsWith(key) || key.equals(String.valueOf(i + 1))) {
                return o;
            }
        }
        return null;
    }
}

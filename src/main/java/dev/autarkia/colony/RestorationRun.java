package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.BeamVisualPayload;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.storage.MatterBank;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The running work of one Restoration projector (05 section 8.5), not saved: the snapshot read, the scan every 100 ticks
 * under the server cell budget, and the restore queue. A missing cell is a snapshot cell whose state has an item form and
 * differs from the world while the world cell is air, replaceable, fluid or fire; cells owned by an older field and the
 * second halves of doors, tall plants and beds (restored with their first half) are not scanned. The queue is edge first:
 * distance to the centroid of the missing cells descending, then y ascending, then position. Each cell costs
 * {@code restoration.fePerBlock} FE and its material: the nearest matching item entity ({@code state.getBlock().asItem()})
 * within 16 blocks, one item, flown back on a reverse glow trail; else its {@link KitPrice} in free matter units (at least
 * 1) through {@link MatterBank}; else the cell stays missing. Cells arrive through a {@code rising_block} in SCALE mode (6
 * ticks); containers come back empty and Autarkia block entities get their snapshot configuration on arrival.
 */
final class RestorationRun {
    static final int RISE_TICKS = 6;
    static final double ITEM_RANGE = 16;
    static final int ORDER_KEEP = 4096;

    private record Step(int primary, int partner) {
    }

    private record Pending(BlockPos pos, CompoundTag tag, long due) {
    }

    private final RestorationProjectorBlockEntity owner;
    Snapshot.@Nullable Builder builder;
    private int scanCursor = -1;
    private final IntArrayList scanMissing = new IntArrayList();
    private List<RestorationField> older = List.of();
    private final ArrayDeque<Step> queue = new ArrayDeque<>();
    private final Long2LongOpenHashMap inFlight = new Long2LongOpenHashMap();
    private final List<Pending> pending = new ArrayList<>();
    final IntArrayList order = new IntArrayList();
    int placedLastTick;
    int maxPerTick;
    long restored;
    long fromItems;
    long fromMatter;
    long noMatter;
    long feUsed;

    RestorationRun(RestorationProjectorBlockEntity owner) {
        this.owner = owner;
    }

    /** Return the queued cells (a pair counts once). */
    int queued() {
        return queue.size();
    }

    /** Return whether a scan is running. */
    boolean scanning() {
        return scanCursor >= 0;
    }

    /** Drop the running scan: its early cells were read before a blast hit the box. */
    void abortScan() {
        scanCursor = -1;
        scanMissing.clear();
    }

    /** Drop the running scan and the queue after a blast: the next scan orders the whole crater edge first again. */
    void blast() {
        abortScan();
        queue.clear();
    }

    /** Drop the queue and the running scan (a new snapshot replaces the old one). */
    void reset() {
        abortScan();
        queue.clear();
        order.clear();
    }

    /** Return whether the cell would be restored: the snapshot has a block there that the world lost. */
    boolean missing(ServerLevel level, BlockPos pos, @Nullable BlockState want) {
        if (want == null || want.isAir() || want.getBlock().asItem() == Items.AIR || inFlight.containsKey(pos.asLong())) {
            return false;
        }
        BlockState have = level.getBlockState(pos);
        return !have.equals(want) && (have.isAir() || have.canBeReplaced() || !have.getFluidState().isEmpty() || have.is(BlockTags.FIRE));
    }

    /** Advance the scan by up to budget cells; return the cells read. A finished scan replaces the queue. */
    int scan(ServerLevel level, Colony c, Snapshot s, int budget, long now) {
        if (scanCursor < 0) {
            if (Math.floorMod(now + owner.getBlockPos().asLong(), 100L) != 0) {
                return 0;
            }
            scanCursor = 0;
            scanMissing.clear();
            older = new ArrayList<>();
            for (RestorationField f : c.fields) {
                if (f.projectorPos().equals(owner.getBlockPos())) {
                    break;
                }
                older.add(f);
            }
        }
        int used = 0;
        int volume = s.volume();
        while (used < budget && scanCursor < volume) {
            int i = scanCursor++;
            used++;
            BlockPos pos = s.pos(i);
            BlockState want = s.state(i);
            if (want == null || Restoration.secondary(want) || !level.isLoaded(pos) || ownedByOlder(pos)) {
                continue;
            }
            if (missing(level, pos, want)) {
                scanMissing.add(i);
            }
        }
        if (scanCursor >= volume) {
            finishScan(s);
        }
        return used;
    }

    private boolean ownedByOlder(BlockPos pos) {
        for (RestorationField f : older) {
            if (f.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    private void finishScan(Snapshot s) {
        scanCursor = -1;
        double cx = 0;
        double cy = 0;
        double cz = 0;
        for (int k = 0; k < scanMissing.size(); k++) {
            BlockPos p = s.pos(scanMissing.getInt(k));
            cx += p.getX() + 0.5;
            cy += p.getY() + 0.5;
            cz += p.getZ() + 0.5;
        }
        int n = Math.max(1, scanMissing.size());
        Vec3 centroid = new Vec3(cx / n, cy / n, cz / n);
        List<Integer> sorted = new ArrayList<>(scanMissing);
        sorted.sort(Comparator.comparingDouble((Integer i) -> -Vec3.atCenterOf(s.pos(i)).distanceToSqr(centroid))
            .thenComparingInt(i -> s.pos(i).getY()).thenComparingLong(i -> s.pos(i).asLong()));
        queue.clear();
        for (int i : sorted) {
            BlockState want = s.state(i);
            BlockPos partner = want == null ? null : Restoration.partner(s.pos(i), want);
            int pi = partner == null ? -1 : s.index(partner);
            BlockState ps = pi < 0 ? null : s.state(pi);
            queue.add(new Step(i, ps != null && Restoration.secondary(ps) ? pi : -1));
        }
        if (!queue.isEmpty()) {
            order.clear();
        }
        owner.missing = scanMissing.size();
        scanMissing.clear();
    }

    /**
     * Restore up to allowed cells from the queue; return the cells placed. Sets the projector status to RESTORING,
     * NO_POWER (the buffer cannot pay the next cell) or NO_MATTER (a cell found no material and stays missing).
     */
    int restore(ServerLevel level, Colony c, Snapshot s, int allowed, long now) {
        int placed = 0;
        int fe = AutarkiaConfig.RESTORATION_FE_PER_BLOCK.get();
        RestorationProjectorBlockEntity.Status status = queue.isEmpty() ? null : RestorationProjectorBlockEntity.Status.RESTORING;
        while (placed < allowed && !queue.isEmpty()) {
            Step st = queue.peek();
            BlockPos pos = s.pos(st.primary);
            BlockState want = s.state(st.primary);
            if (!level.isLoaded(pos) || !missing(level, pos, want)) {
                queue.poll();
                owner.missing = Math.max(0, owner.missing - 1);
                continue;
            }
            boolean pair = st.partner >= 0 && missing(level, s.pos(st.partner), s.state(st.partner));
            int cost = pair ? 2 : 1;
            if (placed + cost > allowed) {
                if (placed > 0) {
                    break;
                }
                pair = false;
                cost = 1;
            }
            if (owner.energy.stored() < (long) fe * cost) {
                status = RestorationProjectorBlockEntity.Status.NO_POWER;
                break;
            }
            queue.poll();
            if (!material(level, c, pos, want)) {
                noMatter++;
                status = RestorationProjectorBlockEntity.Status.NO_MATTER;
                continue;
            }
            arrive(level, c, s, st.primary, fe, now);
            if (pair) {
                arrive(level, c, s, st.partner, fe, now);
            } else if (st.partner >= 0 && missing(level, s.pos(st.partner), s.state(st.partner))) {
                queue.addFirst(new Step(st.partner, -1));
            }
            placed += cost;
        }
        if (status != null) {
            owner.status = status;
        }
        placedLastTick = placed;
        maxPerTick = Math.max(maxPerTick, placed);
        return placed;
    }

    private boolean material(ServerLevel level, Colony c, BlockPos pos, BlockState want) {
        Item item = want.getBlock().asItem();
        Vec3 at = Vec3.atCenterOf(pos);
        ItemEntity best = null;
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(ITEM_RANGE), e -> e.isAlive() && e.getItem().is(item))) {
            double d = e.position().distanceToSqr(at);
            if (d <= ITEM_RANGE * ITEM_RANGE && (best == null || d < best.position().distanceToSqr(at))) {
                best = e;
            }
        }
        if (best != null) {
            ItemStack stack = best.getItem().copy();
            stack.shrink(1);
            if (stack.isEmpty()) {
                best.discard();
            } else {
                best.setItem(stack);
            }
            AutarkiaNetwork.toTracking(level, ChunkPos.containing(pos),
                new BeamVisualPayload(-1, best.position(), at, RISE_TICKS, 0xFF9FE8FF, BeamVisualPayload.KIND_RESTORE, 0.1f));
            fromItems++;
            return true;
        }
        long price = Math.max(1, KitPrice.price(level, item));
        if (MatterBank.withdraw(level, c, price, "Restore")) {
            fromMatter++;
            return true;
        }
        return false;
    }

    private void arrive(ServerLevel level, Colony c, Snapshot s, int index, int fe, long now) {
        BlockPos pos = s.pos(index);
        BlockState want = s.state(index);
        owner.energy.drain(fe);
        feUsed += fe;
        RisingBlockEntity.spawn(level, pos, want, RISE_TICKS);
        inFlight.put(pos.asLong(), now + RISE_TICKS + 2);
        CompoundTag tag = s.data(index);
        if (tag != null) {
            pending.add(new Pending(pos, tag, now + RISE_TICKS + 1));
        }
        owner.missing = Math.max(0, owner.missing - 1);
        restored++;
        if (order.size() < ORDER_KEEP) {
            order.add(index);
        }
        c.stat("repairs", 1);
        Colonies.changed();
        if (restored % 8 == 0) {
            level.playSound(null, pos, AutarkiaSounds.of("restoration_return"), SoundSource.BLOCKS, 1f, 1f);
        }
        owner.countEvent(c, now);
    }

    /** Apply the snapshot configuration to Autarkia block entities that arrived and forget finished rises. */
    void settle(ServerLevel level, long now) {
        inFlight.long2LongEntrySet().removeIf(e -> e.getLongValue() <= now);
        pending.removeIf(p -> {
            if (p.due > now) {
                return false;
            }
            if (level.isLoaded(p.pos) && level.getBlockEntity(p.pos) instanceof PatternDataHolder h) {
                h.readPatternData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), p.tag));
            } else {
                Autarkia.LOG.info("[restore] {}: no block entity at {} for its snapshot configuration", owner.getBlockPos().toShortString(),
                    p.pos.toShortString());
            }
            return true;
        });
    }
}

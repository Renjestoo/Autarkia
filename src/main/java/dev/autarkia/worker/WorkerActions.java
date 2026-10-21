package dev.autarkia.worker;

import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.colony.ColonyPlans;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.ColonyStorage;
import dev.autarkia.colony.ColonyTemplate;
import dev.autarkia.colony.ItemLedger;
import dev.autarkia.colony.Project;
import dev.autarkia.colony.Signs;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.registry.AutarkiaTags;
import dev.autarkia.storage.RackBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * What a worker does at a job (06 sections 3.6, 3.9 and 3.10): fetch from colony storage, place with the NeoForge
 * entity place events (big blocks by their center after {@code canPlace}), break only planned cut cells, string a
 * span through {@code Grid.addSpan}, drive a muscle generator, deposit. Workers change blocks only at job positions
 * inside the domain, never break unbreakable, tagged or filled-container blocks, and fail instead of forcing.
 */
public final class WorkerActions {
    /** The outcome of a work step. */
    public enum Result { DONE, CONTINUE, FAIL_ITEMS, FAIL_OBSTRUCTED }

    private static final int BUILDS_ANY_TIER = 3;

    private WorkerActions() {
    }

    /**
     * Return whether the worker type accepts the job (06 section 1.3 and the Porter limits of 05 section 7.2); a Builder
     * Drone strings spans only inside a {@code pattern_repair} project. A worker takes project jobs up to its own tier;
     * T3 and higher workers take any tier (no T4 builder exists, 05 section 7.2 gives the Builder Drone "PLACE any"), and
     * FOUND_OUTPOST goes to its carriers at any tier: a seed job (it names the {@code outpost_seed}) to a Porter or Hauler
     * Crab, a kit job (no item) only to a Replicator Walker.
     */
    public static boolean accepts(WorkerEntity w, ColonyJob job) {
        if (!w.workerType.jobs().contains(job.type)) {
            return false;
        }
        if (job.type == ColonyJob.Type.FOUND_OUTPOST && (job.item == null) != (w.workerType == WorkerType.REPLICATOR)) {
            return false;
        }
        if ((job.type == ColonyJob.Type.STRING_CABLE || job.type == ColonyJob.Type.CUT_SPAN)
            && (job.tier == null || job.tier.ordinal() > w.workerType.maxCable().ordinal())) {
            return false;
        }
        if (job.type == ColonyJob.Type.STRING_CABLE && w.workerType == WorkerType.BUILDER_DRONE && !repairJob(w, job)) {
            return false;
        }
        if (job.type == ColonyJob.Type.STRING_CABLE && w.workerType == WorkerType.CABLE_LAYER && job.tier.ordinal() < CableTier.HV.ordinal()) {
            return false;
        }
        if (job.type == ColonyJob.Type.LEVEL_CUT && !w.workerType.flying() && !w.level().getBlockState(job.target).is(BlockTags.MINEABLE_WITH_SHOVEL)
            && !w.level().getBlockState(job.target).is(BlockTags.LEAVES) && !w.level().getBlockState(job.target).canBeReplaced()) {
            return false;
        }
        if (job.type == ColonyJob.Type.PLACE && !placeAllowed(w, job)) {
            return false;
        }
        if (job.projectId != null && job.type != ColonyJob.Type.FOUND_OUTPOST) {
            Colony c = w.colony();
            Project p = c == null ? null : c.project(job.projectId);
            ColonyTemplate tpl = p == null ? null : ColonyPlans.templates().get(p.template.getPath());
            return tpl == null || tpl.tier() <= w.workerType.tier() || w.workerType.tier() >= BUILDS_ANY_TIER;
        }
        return true;
    }

    private static boolean repairJob(WorkerEntity w, ColonyJob job) {
        Colony c = w.colony();
        Project p = c == null || job.projectId == null ? null : c.project(job.projectId);
        return p != null && p.template.getPath().equals("pattern_repair");
    }

    /**
     * Return whether the worker may place the job's block (05 section 7.2): a Builder Drone anything, a Cable-Layer only
     * {@code hv_pylon} and {@code cryo_node}, a Lineman only connectors, fuse boxes, transformers and lightning arresters,
     * a Porter every cell of a T1 project and otherwise at most 4 blocks above the ground and no big block larger than
     * 2 x 2 x 2 (06 M3 note 1.3).
     */
    private static boolean placeAllowed(WorkerEntity w, ColonyJob job) {
        Block b = job.state == null ? null : job.state.getBlock();
        if (w.workerType == WorkerType.CABLE_LAYER) {
            String id = b == null ? "" : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).toString();
            return id.equals("autarkia:hv_pylon") || id.equals("autarkia:cryo_node");
        }
        if (w.workerType.flying()) {
            return true;
        }
        if (w.workerType == WorkerType.LINEMAN) {
            return b instanceof dev.autarkia.energy.GridConnector || b == dev.autarkia.registry.AutarkiaBlocks.LIGHTNING_ARRESTER.get();
        }
        Colony c = w.colony();
        Project p = c == null || job.projectId == null ? null : c.project(job.projectId);
        ColonyTemplate tpl = p == null ? null : ColonyPlans.templates().get(p.template.getPath());
        if (tpl != null && tpl.tier() <= 1) {
            return true;
        }
        if (b instanceof dev.autarkia.registry.BigMachineBlock big) {
            int[] min = {0, 0, 0};
            int[] max = {0, 0, 0};
            for (net.minecraft.core.Vec3i o : big.offsets()) {
                min[0] = Math.min(min[0], o.getX());
                min[1] = Math.min(min[1], o.getY());
                min[2] = Math.min(min[2], o.getZ());
                max[0] = Math.max(max[0], o.getX());
                max[1] = Math.max(max[1], o.getY());
                max[2] = Math.max(max[2], o.getZ());
            }
            if (max[0] - min[0] > 1 || max[1] - min[1] > 1 || max[2] - min[2] > 1) {
                return false;
            }
        }
        return heightAboveGround(w, job.target) <= 4;
    }

    private static int heightAboveGround(WorkerEntity w, BlockPos target) {
        for (int d = 1; d <= 8; d++) {
            BlockState below = w.level().getBlockState(target.below(d));
            if (!below.isAir() && !below.canBeReplaced()) {
                return d - 1;
            }
        }
        return 9;
    }

    /** Return the item filter of the job's material, or null when the job needs no items. */
    public static @Nullable Predicate<ItemStack> material(ColonyJob job) {
        return switch (job.type) {
            case PLACE, STRING_CABLE, FOUND_OUTPOST, DELIVER -> job.item == null ? null : s -> s.is(job.item);
            case LEVEL_FILL -> ItemLedger::isFill;
            case SIGN -> s -> s.is(ItemTags.SIGNS);
            default -> null;
        };
    }

    /**
     * Return how many items the worker still needs for this job and the same-kind jobs after it in the order. A
     * Cable-Layer carries one pylon per cargo slot (06 section 1.1) and goes back for more only when it has placed all it
     * carries, so a longer line takes more trips.
     */
    public static int missing(WorkerEntity w, ColonyJob job) {
        Predicate<ItemStack> m = material(job);
        if (m == null) {
            return 0;
        }
        int need = 0;
        for (ColonyJob j : w.remainingJobs()) {
            if (j.type == job.type && java.util.Objects.equals(j.item, job.item)) {
                need += j.type == ColonyJob.Type.STRING_CABLE || j.type == ColonyJob.Type.DELIVER ? j.count : 1;
            }
        }
        if (w.workerType == WorkerType.CABLE_LAYER && job.type == ColonyJob.Type.PLACE) {
            return w.held(m) > 0 ? 0 : Math.min(need, w.cargoStacks());
        }
        return Math.max(0, Math.min(need, 64 * w.cargoStacks()) - w.held(m));
    }

    /** Return the storage that holds the job's material (most stock, then nearest), or null. */
    public static @Nullable BlockPos source(WorkerEntity w, ColonyJob job) {
        Colony c = w.colony();
        Predicate<ItemStack> m = material(job);
        if (c == null || m == null) {
            return null;
        }
        BlockPos best = null;
        int bestCount = 0;
        for (BlockPos p : ColonyRuntime.of(c).storages) {
            if (!(w.level().getBlockEntity(p) instanceof ColonyStorage s)) {
                continue;
            }
            int n = count(s.colonyItems(), m);
            if (n > bestCount || n > 0 && n == bestCount && p.distSqr(w.blockPosition()) < best.distSqr(w.blockPosition())) {
                best = p;
                bestCount = n;
            }
        }
        return best;
    }

    private static int count(ResourceHandler<ItemResource> h, Predicate<ItemStack> m) {
        int n = 0;
        for (int i = 0; i < h.size(); i++) {
            int a = h.getAmountAsInt(i);
            if (a > 0 && m.test(h.getResource(i).toStack(1))) {
                n += a;
            }
        }
        return n;
    }

    /** Extract the missing material of the job from the storage at pos into the cargo; returns false when none was found. */
    public static boolean fetch(WorkerEntity w, BlockPos storage, ColonyJob job) {
        Predicate<ItemStack> m = material(job);
        int want = missing(w, job);
        if (m == null || want == 0) {
            return true;
        }
        if (!(w.level().getBlockEntity(storage) instanceof ColonyStorage s)) {
            return false;
        }
        ResourceHandler<ItemResource> h = s.colonyItems();
        int got = 0;
        try (Transaction tx = Transaction.openRoot()) {
            for (int i = 0; i < h.size() && got < want; i++) {
                ItemResource r = h.getResource(i);
                if (h.getAmountAsInt(i) > 0 && m.test(r.toStack(1))) {
                    int n = h.extract(r, want - got, tx);
                    if (n > 0) {
                        ItemStack rest = w.addCargo(r.toStack(n));
                        got += n - rest.getCount();
                        if (!rest.isEmpty()) {
                            h.insert(ItemResource.of(rest), rest.getCount(), tx);
                        }
                    }
                }
            }
            tx.commit();
        }
        Colony c = w.colony();
        if (c != null && got > 0) {
            dev.autarkia.colony.FrontierSignals.carried(c, storage, job.target, got);
        }
        if (c != null && job.projectId != null && got > 0) {
            if (job.type == ColonyJob.Type.LEVEL_FILL) {
                ColonyRuntime.of(c).jobs.ledger.consumeFill(job.projectId, got);
            } else if (job.item != null) {
                ColonyRuntime.of(c).jobs.ledger.consume(job.projectId, job.item, got);
            }
        }
        return got > 0;
    }

    private static boolean allowed(WorkerEntity w, BlockPos pos) {
        Colony c = w.colony();
        return c != null && c.dimension.equals(w.level().dimension()) && Colonies.insideOrApproved(c, pos) && AutarkiaConfig.WORKER_BLOCK_EDITS.get();
    }

    /** Place the job's block (PLACE, LEVEL_FILL, SIGN) following 06 section 3.9. */
    public static Result place(WorkerEntity w, ColonyJob job) {
        ServerLevel level = (ServerLevel) w.level();
        BlockPos pos = job.target;
        if (!allowed(w, pos)) {
            return Result.FAIL_OBSTRUCTED;
        }
        BlockState now = level.getBlockState(pos);
        BlockState target = job.state;
        Predicate<ItemStack> m = material(job);
        ItemStack held = m == null ? ItemStack.EMPTY : w.cargo.stream().filter(s -> !s.isEmpty() && m.test(s)).findFirst().orElse(ItemStack.EMPTY);
        if (job.type == ColonyJob.Type.LEVEL_FILL) {
            target = held.getItem() instanceof BlockItem bi ? bi.getBlock().defaultBlockState() : null;
        } else if (job.type == ColonyJob.Type.SIGN) {
            Colony c = w.colony();
            target = held.getItem() instanceof BlockItem bi && c != null ? Signs.withRotation(bi.getBlock(), c, pos) : null;
        }
        if (target != null && now.is(target.getBlock()) && job.type == ColonyJob.Type.PLACE) {
            return Result.DONE;
        }
        if (target == null || held.isEmpty()) {
            return Result.FAIL_ITEMS;
        }
        if (!dev.autarkia.colony.Builds.replaceable(now)) {
            return Result.FAIL_OBSTRUCTED;
        }
        boolean ok = target.getBlock() instanceof BaseMultiblock mb ? placeBig(w, level, pos, target, mb) : placeOne(w, level, pos, target);
        if (!ok) {
            return Result.FAIL_OBSTRUCTED;
        }
        ItemStack used = w.takeCargo(m, 1);
        target.getBlock().setPlacedBy(level, pos, level.getBlockState(pos), w, used);
        level.gameEvent(w, GameEvent.BLOCK_PLACE, pos);
        var sound = target.getSoundType(level, pos, w);
        level.playSound(null, pos, sound.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, (sound.getVolume() + 1) / 2f, sound.getPitch() * 0.8f);
        w.trigger("place");
        w.spend(w.workerType.placeCost());
        if (job.type == ColonyJob.Type.SIGN) {
            Colony c = w.colony();
            Project p = c == null || job.projectId == null ? null : c.project(job.projectId);
            if (p != null) {
                Signs.write(level, pos, c, p);
                p.signed = true;
                p.signPos = pos.asLong();
            }
        }
        return Result.DONE;
    }

    private static boolean placeOne(WorkerEntity w, ServerLevel level, BlockPos pos, BlockState state) {
        BlockSnapshot snap = BlockSnapshot.create(level.dimension(), level, pos);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (EventHooks.onBlockPlace(w, snap, Direction.UP)) {
            snap.restore();
            return false;
        }
        return true;
    }

    private static boolean placeBig(WorkerEntity w, ServerLevel level, BlockPos center, BlockState state, BaseMultiblock mb) {
        List<BlockPos> parts = new ArrayList<>(mb.getMultiblockShape(level, center, state).getGlobalPositions());
        for (BlockPos p : parts) {
            if (!allowed(w, p) || !dev.autarkia.colony.Builds.replaceable(level.getBlockState(p))) {
                return false;
            }
        }
        if (!mb.canPlace(level, center, state, null, false)) {
            return false;
        }
        List<BlockSnapshot> snaps = new ArrayList<>();
        snaps.add(BlockSnapshot.create(level.dimension(), level, center));
        parts.stream().filter(p -> !p.equals(center)).forEach(p -> snaps.add(BlockSnapshot.create(level.dimension(), level, p)));
        level.setBlock(center, state, Block.UPDATE_ALL);
        if (EventHooks.onMultiBlockPlace(w, snaps, Direction.UP)) {
            mb.destroy(center, level, state, false);
            snaps.forEach(BlockSnapshot::restore);
            return false;
        }
        return true;
    }

    /**
     * Break a planned cut cell (06 section 3.9 break, never-grief rules of 3.10): a block that holds items is never broken,
     * except a crate block, which the worker sets aside whole (with its contents) on the nearest free spot that is not
     * part of the project.
     */
    public static Result cut(WorkerEntity w, ColonyJob job) {
        ServerLevel level = (ServerLevel) w.level();
        BlockPos pos = job.target;
        BlockState st = level.getBlockState(pos);
        if (st.isAir() || st.canBeReplaced() && st.getFluidState().isEmpty()) {
            if (!st.isAir()) {
                level.destroyBlock(pos, false, w);
            }
            return Result.DONE;
        }
        if (level.getBlockEntity(pos) instanceof dev.autarkia.storage.CrateBlockEntity crate && allowed(w, pos)) {
            BlockPos aside = aside(w, level, pos, job);
            if (aside == null) {
                return Result.FAIL_OBSTRUCTED;
            }
            ItemStack item = crate.pickUp();
            level.removeBlock(pos, false);
            level.setBlock(aside, st, Block.UPDATE_ALL);
            if (level.getBlockEntity(aside) instanceof dev.autarkia.storage.CrateBlockEntity moved) {
                moved.fill(item);
            }
            w.trigger("place");
            w.spend(w.workerType.placeCost());
            return Result.DONE;
        }
        if (!allowed(w, pos) || st.getDestroySpeed(level, pos) < 0 || st.is(AutarkiaTags.WORKER_UNBREAKABLE) || holdsItems(level, pos)) {
            return Result.FAIL_OBSTRUCTED;
        }
        if (!EventHooks.onEntityDestroyBlock(w, pos, st)) {
            return Result.FAIL_OBSTRUCTED;
        }
        level.destroyBlock(pos, true, w);
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))) {
            ItemStack rest = w.addCargo(e.getItem());
            if (rest.isEmpty()) {
                e.discard();
            } else {
                e.setItem(rest);
            }
        }
        w.trigger("place");
        w.spend(w.workerType.placeCost());
        return Result.DONE;
    }

    /**
     * Return the nearest free floor spot within 4 blocks on the same level for a crate block in the way of a project:
     * replaceable, on a sturdy block, inside the domain and not a cell of the job's project; null when there is none.
     */
    private static @Nullable BlockPos aside(WorkerEntity w, ServerLevel level, BlockPos pos, ColonyJob job) {
        Colony c = w.colony();
        Project p = c == null || job.projectId == null ? null : c.project(job.projectId);
        it.unimi.dsi.fastutil.longs.LongOpenHashSet planned = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        if (p != null) {
            p.cells.forEach(cl -> planned.add(cl.pos()));
            for (long cut : p.cuts) {
                planned.add(cut);
            }
        }
        BlockPos best = null;
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 0, 4))) {
            BlockPos b = q.immutable();
            if (b.equals(pos) || planned.contains(b.asLong()) || c != null && !Colonies.insideOrApproved(c, b) || !level.getBlockState(b).canBeReplaced()
                || !level.getBlockState(b.below()).isFaceSturdy(level, b.below(), Direction.UP)) {
                continue;
            }
            if (best == null || b.distSqr(pos) < best.distSqr(pos) || b.distSqr(pos) == best.distSqr(pos) && b.asLong() < best.asLong()) {
                best = b;
            }
        }
        return best;
    }

    private static boolean holdsItems(ServerLevel level, BlockPos pos) {
        ResourceHandler<ItemResource> h = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, pos, null);
        if (h == null) {
            return false;
        }
        for (int i = 0; i < h.size(); i++) {
            if (h.getAmountAsInt(i) > 0) {
                return true;
            }
        }
        return false;
    }

    /** String the job's span with cable items from the cargo (06 section 3.6 StringCable). */
    public static Result string(WorkerEntity w, ColonyJob job) {
        ServerLevel level = (ServerLevel) w.level();
        if (job.nodeB == null || job.tier == null) {
            return Result.FAIL_OBSTRUCTED;
        }
        if (dev.autarkia.colony.Builds.strung(level, job.target.asLong(), job.nodeB.asLong())) {
            return Result.DONE;
        }
        if (w.held(s -> s.is(Grid.cableItem(job.tier))) < job.count) {
            return Result.FAIL_ITEMS;
        }
        Grid.SpanResult r = Grid.addSpan(level, job.target, job.nodeB, job.tier);
        if (r != Grid.SpanResult.OK) {
            dev.autarkia.Autarkia.LOG.info("[worker] span {} -> {} refused: {}", job.target.toShortString(), job.nodeB.toShortString(), r);
            return Result.FAIL_OBSTRUCTED;
        }
        w.takeCargo(s -> s.is(Grid.cableItem(job.tier)), job.count);
        w.trigger("string");
        w.sound("entity.worker.weld", 0.6f, 1.6f);
        w.spend(w.workerType.stringCost());
        return Result.DONE;
    }

    /** Cut the job's span (Lineman, 200 FE); its full cable cost goes into the cargo (06 section 1.2, FUSE_INSTALL order). */
    public static Result cutSpan(WorkerEntity w, ColonyJob job) {
        ServerLevel level = (ServerLevel) w.level();
        if (job.nodeB == null || job.tier == null) {
            return Result.FAIL_OBSTRUCTED;
        }
        dev.autarkia.energy.Span span = null;
        it.unimi.dsi.fastutil.ints.IntArrayList ids = Grid.of(level).data().at(job.target.asLong());
        for (int i = 0; i < ids.size(); i++) {
            dev.autarkia.energy.Span s = Grid.of(level).data().spans.get(ids.getInt(i));
            if (s != null && s.joins(job.target.asLong(), job.nodeB.asLong())) {
                span = s;
            }
        }
        if (span == null) {
            return Result.DONE;
        }
        int cost = span.tier.itemCost(span.length);
        Grid.removeSpan(level, job.target, job.nodeB);
        ItemStack rest = w.addCargo(new ItemStack(Grid.cableItem(span.tier), cost));
        if (!rest.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, w.getX(), w.getY(), w.getZ(), rest);
        }
        w.trigger("weld");
        w.sound("entity.worker.weld", 0.6f, 1.2f);
        w.spend(200);
        return Result.DONE;
    }

    /**
     * Hand the job's item (up to the job count, as far as the cargo holds it) into the item handler of the target block
     * (DELIVER; a big block through its center; a {@link dev.autarkia.colony.Colony.DeliveryTarget} through its delivery
     * handler) in one transaction; a missing handler or one that does not take all of it fails the step and keeps the cargo.
     */
    public static Result deliver(WorkerEntity w, ColonyJob job) {
        ServerLevel level = (ServerLevel) w.level();
        if (job.item == null) {
            return Result.FAIL_OBSTRUCTED;
        }
        BlockPos at = level.getBlockState(job.target).getBlock() instanceof BaseMultiblock
            ? net.nikdo53.tinymultiblocklib.block.IMultiBlock.getCenter(level, job.target) : job.target;
        ResourceHandler<ItemResource> h = level.getBlockEntity(at) instanceof dev.autarkia.colony.Colony.DeliveryTarget d ? d.deliveryHandler()
            : level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, at, null);
        if (h == null) {
            return Result.FAIL_OBSTRUCTED;
        }
        int want = Math.min(job.count, w.held(s -> s.is(job.item)));
        if (want <= 0) {
            return Result.FAIL_ITEMS;
        }
        try (Transaction tx = Transaction.openRoot()) {
            if (h.insert(ItemResource.of(new ItemStack(job.item)), want, tx) < want) {
                return Result.FAIL_OBSTRUCTED;
            }
            tx.commit();
        }
        int left = want;
        for (ItemStack s : w.cargo) {
            if (left > 0 && s.is(job.item)) {
                int n = Math.min(left, s.getCount());
                s.shrink(n);
                left -= n;
            }
        }
        w.syncCargo();
        w.trigger("crab".equals(w.workerType.rig()) ? "lower" : "place");
        w.sound("entity.worker.servo", 0.5f, 1.2f);
        return Result.DONE;
    }

    /** Deposit the cargo into colony storage, nearest first; returns whether the cargo is now empty. */
    public static boolean deposit(WorkerEntity w, BlockPos storage) {
        if (!(w.level().getBlockEntity(storage) instanceof ColonyStorage s)) {
            return false;
        }
        ResourceHandler<ItemResource> h = s.colonyItems();
        try (Transaction tx = Transaction.openRoot()) {
            for (int i = 0; i < w.cargo.size(); i++) {
                ItemStack st = w.cargo.get(i);
                if (dev.autarkia.storage.CrateItems.isCrate(st) && s instanceof RackBlockEntity rack) {
                    if (rack.insertCrate(st)) {
                        w.cargo.set(i, ItemStack.EMPTY);
                    }
                } else if (!st.isEmpty()) {
                    int n = h.insert(ItemResource.of(st), st.getCount(), tx);
                    st.shrink(n);
                }
            }
            tx.commit();
        }
        w.syncCargo();
        return w.cargoEmpty();
    }

    /** Return the nearest colony storage that accepts at least one cargo item and is not in the unreachable cache, or null. */
    public static @Nullable BlockPos depositTarget(WorkerEntity w) {
        Colony c = w.colony();
        if (c == null) {
            return null;
        }
        BlockPos best = null;
        for (BlockPos p : ColonyRuntime.of(c).storages) {
            if (!w.unreachable(p) && w.level().getBlockEntity(p) instanceof ColonyStorage s && (accepts(s.colonyItems(), w)
                || s instanceof RackBlockEntity rack && rack.crateCount() < rack.positionCount() && w.cargo.stream().anyMatch(dev.autarkia.storage.CrateItems::isCrate))
                && (best == null || p.distSqr(w.blockPosition()) < best.distSqr(w.blockPosition()))) {
                best = p;
            }
        }
        return best;
    }

    private static boolean accepts(ResourceHandler<ItemResource> h, WorkerEntity w) {
        for (ItemStack st : w.cargo) {
            if (st.isEmpty()) {
                continue;
            }
            try (Transaction tx = Transaction.openRoot()) {
                if (h.insert(ItemResource.of(st), 1, tx) > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}

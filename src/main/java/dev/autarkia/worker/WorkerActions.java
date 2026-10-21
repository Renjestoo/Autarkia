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

    private WorkerActions() {
    }

    /** Return whether the worker type accepts the job (06 section 1.3 and the Porter limits of 05 section 7.2). */
    public static boolean accepts(WorkerEntity w, ColonyJob job) {
        if (!w.workerType.jobs().contains(job.type)) {
            return false;
        }
        if (job.type == ColonyJob.Type.STRING_CABLE && job.tier != CableTier.ROPE) {
            return false;
        }
        if (job.type == ColonyJob.Type.LEVEL_CUT && !w.level().getBlockState(job.target).is(BlockTags.MINEABLE_WITH_SHOVEL)
            && !w.level().getBlockState(job.target).is(BlockTags.LEAVES) && !w.level().getBlockState(job.target).canBeReplaced()) {
            return false;
        }
        if (job.type == ColonyJob.Type.PLACE && heightAboveGround(w, job.target) > 4) {
            return false;
        }
        if (job.projectId != null) {
            Colony c = w.colony();
            Project p = c == null ? null : c.project(job.projectId);
            ColonyTemplate tpl = p == null ? null : ColonyPlans.templates().get(p.template.getPath());
            return tpl == null || tpl.tier() <= w.workerType.tier();
        }
        return true;
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
            case PLACE, STRING_CABLE -> job.item == null ? null : s -> s.is(job.item);
            case LEVEL_FILL -> ItemLedger::isFill;
            case SIGN -> s -> s.is(ItemTags.SIGNS);
            default -> null;
        };
    }

    /** Return how many items the worker still needs for this job and the same-kind jobs after it in the order. */
    public static int missing(WorkerEntity w, ColonyJob job) {
        Predicate<ItemStack> m = material(job);
        if (m == null) {
            return 0;
        }
        int need = 0;
        for (ColonyJob j : w.remainingJobs()) {
            if (j.type == job.type && java.util.Objects.equals(j.item, job.item)) {
                need += j.type == ColonyJob.Type.STRING_CABLE ? j.count : 1;
            }
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
        return c != null && Colonies.at(w.level(), pos) == c && AutarkiaConfig.WORKER_BLOCK_EDITS.get();
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

    /** Break a planned cut cell (06 section 3.9 break, never-grief rules of 3.10). */
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
                if (!st.isEmpty()) {
                    int n = h.insert(ItemResource.of(st), st.getCount(), tx);
                    st.shrink(n);
                }
            }
            tx.commit();
        }
        w.syncCargo();
        return w.cargoEmpty();
    }

    /** Return the nearest colony storage that accepts at least one cargo item, or null. */
    public static @Nullable BlockPos depositTarget(WorkerEntity w) {
        Colony c = w.colony();
        if (c == null) {
            return null;
        }
        BlockPos best = null;
        for (BlockPos p : ColonyRuntime.of(c).storages) {
            if (w.level().getBlockEntity(p) instanceof ColonyStorage s && accepts(s.colonyItems(), w)
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

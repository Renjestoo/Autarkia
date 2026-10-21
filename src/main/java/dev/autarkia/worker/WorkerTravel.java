package dev.autarkia.worker;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.config.AutarkiaConfig;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Travel tickets of 06 section 3.12 ({@code autarkia:worker_travel}): a Surveyor, a Cable-Layer, a Sentinel on ESCORT
 * and a carrier on FOUND_OUTPOST force the chunk they are in while it is not force-loaded for another reason (the
 * vanilla force-load set or an outpost core chunk). One ticket is one worker: it forces the worker's chunk and, within
 * 3 blocks of the border it is about to cross, the chunk ahead as well, because a worker that steps into a chunk that
 * does not tick entities freezes there. A colony holds at most {@code maxTravelTickets} tickets; a worker without a free
 * ticket stops where it is in state WAITING. A Surveyor, Cable-Layer or Sentinel that ends or leaves its job far out
 * keeps its ticket on the way back to its charger (when it needs a recharge) or its home, until it is within 8 blocks
 * of it or in force-loaded chunks. Tickets go then, or when the worker is removed; tickets saved with the world are
 * dropped on load, the workers take new ones as they move.
 */
public final class WorkerTravel {
    public static final TicketController TICKETS = new TicketController(Autarkia.id("worker_travel"), WorkerTravel::validate);
    private static final int LOOK_AHEAD = 3;
    private static final Map<UUID, Held> HELD = new LinkedHashMap<>();

    private record Held(WorkerEntity worker, UUID colony, ServerLevel level, LongSet chunks) {
    }

    private WorkerTravel() {
    }

    /** Register the ticket controller (mod bus), the release sweep and the server stop reset (game bus). */
    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterTicketControllersEvent e) -> e.register(TICKETS));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> sweep());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> HELD.clear());
    }

    private static void validate(ServerLevel level, TicketHelper helper) {
        for (UUID owner : List.copyOf(helper.getEntityTickets().keySet())) {
            helper.removeAllTickets(owner);
        }
    }

    /** Return whether the worker travels with tickets on this job. */
    public static boolean travels(WorkerEntity w, ColonyJob job) {
        return w.workerType == WorkerType.SURVEYOR || w.workerType == WorkerType.CABLE_LAYER || job.type == ColonyJob.Type.ESCORT
            || job.type == ColonyJob.Type.FOUND_OUTPOST;
    }

    /**
     * Return where a T4 drone without a job is heading with a ticket: its charger when it needs a recharge, else its home;
     * null within 8 blocks of it or for other workers. A drone that ends or leaves a job far out keeps its ticket on the
     * way back, since without it its chunk would stop ticking and it would freeze there.
     */
    private static @Nullable BlockPos returnTarget(WorkerEntity w) {
        if (w.workerType != WorkerType.SURVEYOR && w.workerType != WorkerType.CABLE_LAYER && w.workerType != WorkerType.SENTINEL) {
            return null;
        }
        BlockPos to = w.getBrain().hasMemoryValue(WorkerMemories.NEEDS_RECHARGE.get())
            ? w.getBrain().getMemory(WorkerMemories.CHARGER_POS.get()).map(net.minecraft.core.GlobalPos::pos).orElse(w.home()) : w.home();
        return w.blockPosition().distSqr(to) <= 64 ? null : to;
    }

    private static boolean ticketed(WorkerEntity w, @Nullable ColonyJob job) {
        return job != null ? travels(w, job) : returnTarget(w) != null;
    }

    /**
     * Keep the worker's chunks loaded on its way toward a position; returns false when the worker must wait for a free
     * ticket (it then stops, state WAITING). A worker without a travel job, or inside force-loaded chunks, needs none.
     *
     * @param resume the state the worker shows again once it may move
     */
    public static boolean hold(WorkerEntity w, BlockPos toward, WorkerEntity.State resume) {
        ColonyJob job = w.currentJob();
        Colony c = w.colony();
        if (!(w.level() instanceof ServerLevel level) || c == null || !ticketed(w, job)) {
            return true;
        }
        LongSet want = wanted(level, w, toward);
        Held h = HELD.get(w.getUUID());
        if (want.isEmpty()) {
            release(w);
        } else if (h == null) {
            if (count(c) >= AutarkiaConfig.MAX_TRAVEL_TICKETS.get()) {
                w.setState(WorkerEntity.State.WAITING);
                w.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                w.getNavigation().stop();
                return false;
            }
            h = new Held(w, c.id, level, new LongOpenHashSet());
            HELD.put(w.getUUID(), h);
            move(h, want);
        } else {
            move(h, want);
        }
        if (w.state() == WorkerEntity.State.WAITING) {
            w.setState(resume);
        }
        return true;
    }

    private static LongSet wanted(ServerLevel level, WorkerEntity w, BlockPos toward) {
        LongSet out = new LongOpenHashSet();
        long here = ChunkPos.pack(w.blockPosition());
        if (!forcedElsewhere(level, here)) {
            out.add(here);
        }
        Vec3 dir = new Vec3(toward.getX() + 0.5 - w.getX(), 0, toward.getZ() + 0.5 - w.getZ());
        if (dir.lengthSqr() > 1.0E-4) {
            Vec3 ahead = w.position().add(dir.normalize().scale(LOOK_AHEAD));
            long next = ChunkPos.pack(BlockPos.containing(ahead));
            if (next != here && !forcedElsewhere(level, next)) {
                out.add(next);
            }
        }
        return out;
    }

    private static boolean forcedElsewhere(ServerLevel level, long chunk) {
        if (level.getForceLoadedChunks().contains(chunk)) {
            return true;
        }
        if (Colonies.server() == null) {
            return false;
        }
        for (Colony c : Colonies.registry().all()) {
            if (c.dimension.equals(level.dimension()) && c.outposts.stream().anyMatch(o -> ChunkPos.pack(o.corePos) == chunk)) {
                return true;
            }
        }
        return false;
    }

    private static void move(Held h, LongSet want) {
        UUID id = h.worker().getUUID();
        for (long ch : want) {
            if (h.chunks().add(ch)) {
                TICKETS.forceChunk(h.level(), id, ChunkPos.getX(ch), ChunkPos.getZ(ch), true, true);
            }
        }
        for (long ch : new ArrayList<>(h.chunks())) {
            if (!want.contains(ch)) {
                h.chunks().remove(ch);
                TICKETS.forceChunk(h.level(), id, ChunkPos.getX(ch), ChunkPos.getZ(ch), false, true);
            }
        }
    }

    /** Release every chunk the worker holds. */
    public static void release(WorkerEntity w) {
        Held h = HELD.remove(w.getUUID());
        if (h != null) {
            move(h, new LongOpenHashSet());
        }
    }

    /** Return the number of travel tickets the colony holds. */
    public static int count(Colony c) {
        return (int) HELD.values().stream().filter(h -> h.colony().equals(c.id)).count();
    }

    /** Return whether the worker holds a travel ticket. */
    public static boolean holds(WorkerEntity w) {
        return HELD.containsKey(w.getUUID());
    }

    private static void sweep() {
        HELD.values().removeIf(h -> {
            WorkerEntity w = h.worker();
            ColonyJob job = w.currentJob();
            if (!w.isRemoved() && job != null && travels(w, job)) {
                return false;
            }
            BlockPos to = w.isRemoved() || job != null ? null : returnTarget(w);
            LongSet want = to == null ? new LongOpenHashSet() : wanted(h.level(), w, to);
            move(h, want);
            return want.isEmpty();
        });
    }
}

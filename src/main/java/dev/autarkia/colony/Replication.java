package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.registry.AutarkiaRecipes;
import dev.autarkia.worker.WorkerAssemblyRecipe;
import dev.autarkia.worker.WorkerEntity;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jspecify.annotations.Nullable;

/**
 * Replicator Walkers (05 section 8.2, W19). The print rules, all true: colony T5+ with a formed, powered
 * {@code replicator_foundry}; active walkers below {@code replicator.maxPerDomain} and outposts plus active walkers below
 * the outpost maximum; an unclaimed deposit outside all domains within {@code outpost.maxDistance} of the Core
 * (horizontal) with a walker site; the foundry bill in storage and the kit matter units ({@link KitPrice}) in the
 * matter buffers; 24,000 colony ticks since the last print. Template 27 ({@code replicator.ready}) proposes the
 * print as a project; in Manual autonomy it waits for approval like any proposal. When the project is done, standing
 * deliveries bring the missing bill into the foundry; the foundry takes the kit matter units, reserves the deposit and
 * prints for 2,400 ticks. The walker walks to the site, checks it again (a changed site fails TARGET_OBSTRUCTED and it
 * gets a new site), deploys for 100 ticks, plants an outpost core and leaves the kit to build through rising blocks,
 * then discards itself (no drops, its cap is freed). A walker that dies loses its kit: the reservation goes, and a
 * {@code machine_destroyed} memory is recorded at its position.
 */
public final class Replication {
    public static final long PRINT_GAP = 24000;
    public static final String TEMPLATE = "replicator_walker";
    public static final int DEPLOY_TICKS = 100;

    private Replication() {
    }

    /** Return the Replicator recipe of the foundry, or empty when the data pack has none. */
    public static Optional<WorkerAssemblyRecipe> recipe(ServerLevel level) {
        return level.recipeAccess().recipeMap().byType(AutarkiaRecipes.WORKER_ASSEMBLY.get()).stream().map(RecipeHolder::value)
            .filter(r -> r.forMachine(Autarkia.id("replicator_foundry"))).findFirst();
    }

    /** Return the kit price in matter units ({@link KitPrice}). */
    public static long kitMu(Colony c, ServerLevel level) {
        return KitPrice.of(c, level);
    }

    /** Return the nearest formed foundry in the domain, or null. */
    public static @Nullable ReplicatorFoundryBlockEntity foundry(Colony c, ServerLevel level) {
        ReplicatorFoundryBlockEntity best = null;
        for (long packed : Colonies.chunks(c)) {
            net.minecraft.world.level.chunk.LevelChunk ch = level.getChunkSource().getChunkNow(net.minecraft.world.level.ChunkPos.getX(packed),
                net.minecraft.world.level.ChunkPos.getZ(packed));
            if (ch == null) {
                continue;
            }
            for (net.minecraft.world.level.block.entity.BlockEntity be : ch.getBlockEntities().values()) {
                if (be instanceof ReplicatorFoundryBlockEntity f && f.isCenter() && Colonies.inside(c, be.getBlockPos())
                    && (best == null || be.getBlockPos().distSqr(c.corePos) < best.getBlockPos().distSqr(c.corePos))) {
                    best = f;
                }
            }
        }
        return best;
    }

    /** Return the deposit the next walker goes to: unclaimed, outside all domains, in outpost range of the Core; nearest first. */
    public static Colony.@Nullable Deposit target(Colony c, ServerLevel level) {
        Colony.Deposit best = null;
        for (Colony.Deposit d : c.deposits) {
            double dist = Outposts.horizontal(c.corePos, d.pos());
            if (d.outpost().isPresent() || Colonies.at(level, d.pos()) != null || dist > AutarkiaConfig.OUTPOST_MAX_DISTANCE.get()
                || dist < AutarkiaConfig.OUTPOST_MIN_SPACING.get()
                || c.outposts.stream().anyMatch(o -> Outposts.horizontal(o.corePos, d.pos()) < AutarkiaConfig.OUTPOST_MIN_SPACING.get())) {
                continue;
            }
            if (best == null || dist < Outposts.horizontal(c.corePos, best.pos())) {
                best = d;
            }
        }
        return best;
    }

    /** Return whether the colony may print one more walker by count (walker cap and outpost slots). */
    public static boolean room(Colony c) {
        int active = c.replicatorsActive.size();
        return active < AutarkiaConfig.REPLICATOR_MAX_PER_DOMAIN.get() && c.outposts.size() + active < ColonyTiers.maxOutposts(c.tier);
    }

    /** Return whether the colony storage plus the foundry slots hold the whole walker bill. */
    public static boolean billInStorage(Colony c, ServerLevel level, WorkerAssemblyRecipe r, @Nullable ReplicatorFoundryBlockEntity f) {
        ColonyRuntime rt = ColonyRuntime.of(c);
        for (SizedIngredient s : r.bill()) {
            Item item = first(s);
            int have = ItemLedger.stock(level, rt, item) + (f == null ? 0 : f.held(item));
            if (item == null || have < s.count()) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable Item first(SizedIngredient s) {
        return s.ingredient().items().findFirst().map(Holder::value).orElse(null);
    }

    /**
     * Return {@code replicator.ready}: 1 when every print rule holds, no print is ordered or running and no
     * {@code replicator_walker} project is open; else 0.
     */
    public static double ready(Colony c, ServerLevel level) {
        if (!AutarkiaConfig.REPLICATOR_ENABLED.get() || c.tier < 5 || c.state != Colony.State.ACTIVE || !room(c) || c.clock - c.lastPrint < PRINT_GAP
            || c.openProjects().stream().anyMatch(p -> p.template.getPath().equals(TEMPLATE))) {
            return 0;
        }
        ReplicatorFoundryBlockEntity f = foundry(c, level);
        Optional<WorkerAssemblyRecipe> r = recipe(level);
        if (f == null || f.ordered() || !f.powered() || r.isEmpty() || target(c, level) == null) {
            return 0;
        }
        if (!billInStorage(c, level, r.get(), f) || dev.autarkia.storage.MatterBank.available(level, c) < kitMu(c, level)) {
            return 0;
        }
        return 1;
    }

    /** Handler {@code replicator} (template 27): a print project at the foundry with no cells, or null. */
    static @Nullable Project propose(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        ReplicatorFoundryBlockEntity f = foundry(c, level);
        Colony.Deposit d = target(c, level);
        if (f == null || d == null || OutpostProjects.walkerSite(c, level, d.pos()) == null) {
            return null;
        }
        return ProjectFactory.orderedProject(c, level, id, tpl, TEMPLATE, f.getBlockPos(), new java.util.ArrayList<>(), new java.util.ArrayList<>(),
            new long[0]);
    }

    /** Order the print at the foundry when its project is done: standing deliveries bring the missing bill into it. */
    static void order(Colony c, ServerLevel level, Project p) {
        ReplicatorFoundryBlockEntity f = level.getBlockEntity(BlockPos.of(p.anchor)) instanceof ReplicatorFoundryBlockEntity fb ? fb : foundry(c, level);
        Optional<WorkerAssemblyRecipe> r = recipe(level);
        if (f == null || r.isEmpty()) {
            ColonyRuntime.of(c).log("Walker print skipped: " + (f == null ? "no replicator foundry" : "no Replicator assembly recipe"));
            return;
        }
        f.order();
        BlockPos at = f.getBlockPos();
        for (SizedIngredient s : r.get().bill()) {
            Item item = first(s);
            int missing = s.count() - f.held(item) - c.deliveries.stream().filter(dl -> dl.target().equals(at) && dl.item() == item)
                .mapToInt(Colony.Delivery::count).sum();
            if (item != null && missing > 0) {
                c.deliveries.add(new Colony.Delivery(at, item, missing));
            }
        }
        Colonies.changed();
        ColonyRuntime.of(c).log("Walker print ordered at the foundry " + at.toShortString());
    }

    /**
     * Start the print: take the kit matter units and reserve the target deposit for the walker; returns false (nothing
     * taken) when the matter or the target is missing.
     */
    public static boolean start(Colony c, ServerLevel level, UUID walker) {
        Colony.Deposit d = target(c, level);
        long mu = kitMu(c, level);
        if (d == null || !dev.autarkia.storage.MatterBank.withdraw(level, c, mu)) {
            return false;
        }
        c.deposits.set(c.deposits.indexOf(d), d.claimedBy(walker));
        c.lastPrint = c.clock;
        Colonies.changed();
        return true;
    }

    /** Register a printed walker and send it to its site. */
    public static void printed(Colony c, ServerLevel level, WorkerEntity w) {
        c.replicatorsActive.add(w.getUUID());
        Colonies.changed();
        dispatch(c, level, w);
    }

    private static Colony.@Nullable Deposit reserved(Colony c, UUID walker) {
        return c.deposits.stream().filter(d -> d.outpost().filter(walker::equals).isPresent()).findFirst().orElse(null);
    }

    /** Open the FOUND_OUTPOST kit job of the walker at a site near its deposit; false when no site is valid now. */
    public static boolean dispatch(Colony c, ServerLevel level, WorkerEntity w) {
        Colony.Deposit d = reserved(c, w.getUUID());
        OutpostProjects.WalkerSite site = d == null ? null : OutpostProjects.walkerSite(c, level, d.pos());
        if (site == null) {
            ColonyRuntime.of(c).log("No walker site near " + (d == null ? "a reserved deposit" : "the " + d.ore() + " deposit at " + d.pos().toShortString()));
            return false;
        }
        ColonyRuntime.of(c).jobs.add(new ColonyJob(ColonyJob.Type.FOUND_OUTPOST, null, site.core(), site.coreState(), null, 0, 60, 0, level.getGameTime(),
            null, null, 0));
        Autarkia.LOG.info("[colony] {}: walker {} goes to {}", c.name, w.getUUID().toString().substring(0, 8), site.core().toShortString());
        return true;
    }

    /** Give idle walkers without a kit job a site again (after a failed site check). Runs at each colony sample. */
    static void tick(Colony c, ServerLevel level) {
        for (UUID id : java.util.List.copyOf(c.replicatorsActive)) {
            if (!(level.getEntity(id) instanceof WorkerEntity w)) {
                continue;
            }
            boolean jobOpen = ColonyRuntime.of(c).jobs.all().stream().anyMatch(j -> j.type == ColonyJob.Type.FOUND_OUTPOST && j.item == null
                && j.status != ColonyJob.Status.DONE);
            if (w.isIdle() && !jobOpen) {
                dispatch(c, level, w);
            }
        }
    }

    /** Return whether the walker may deploy at the job site now: the core space is free and the outpost rules hold. */
    public static boolean siteValid(Colony c, ServerLevel level, ColonyJob job, WorkerEntity w) {
        if (job.state == null || Outposts.invalid(level, c, job.target) != null) {
            return false;
        }
        return job.state.getBlock() instanceof net.nikdo53.tinymultiblocklib.block.BaseMultiblock mb && mb.canPlace(level, job.target, job.state, w, false);
    }

    /**
     * Deploy: plant the outpost core, hand the kit to it, fire {@code walker_outpost_founded} and
     * {@code replicator_deploy}, and discard the walker. Returns false when the core cannot be planted.
     */
    public static boolean deploy(Colony c, ServerLevel level, WorkerEntity w, ColonyJob job) {
        Colony.Deposit d = reserved(c, w.getUUID());
        int at = d == null ? -1 : c.deposits.indexOf(d);
        if (d != null) {
            c.deposits.set(at, new Colony.Deposit(d.id(), d.ore(), d.pos(), d.estCount(), d.found(), Optional.empty()));
        }
        if (!Outposts.plant(level, c, job.target, job.state, w)) {
            if (d != null) {
                c.deposits.set(at, d);
            }
            return false;
        }
        if (level.getBlockEntity(job.target) instanceof OutpostCoreBlockEntity core) {
            core.queueKit(OutpostProjects.kit(c, level, job.target, job.state));
        }
        w.jobDone();
        c.replicatorsActive.remove(w.getUUID());
        ColonyEvents.fire(c, "walker_outpost_founded");
        ColonyEvents.worker(c, "replicator_deploy");
        ColonyRuntime.of(c).log("A Replicator Walker founded an outpost at " + job.target.toShortString());
        Autarkia.LOG.info("[colony] {}: walker {} deployed at {}", c.name, w.getUUID().toString().substring(0, 8), job.target.toShortString());
        w.unbind();
        w.discard();
        Colonies.changed();
        return true;
    }

    /** A walker died: it leaves the active list, its reservation goes, and a {@code machine_destroyed} memory is recorded. */
    public static void lost(Colony c, WorkerEntity w) {
        c.replicatorsActive.remove(w.getUUID());
        Colony.Deposit d = reserved(c, w.getUUID());
        if (d != null) {
            c.deposits.set(c.deposits.indexOf(d), new Colony.Deposit(d.id(), d.ore(), d.pos(), d.estCount(), d.found(), Optional.empty()));
        }
        ColonyMemory.event(c, "machine_destroyed", "replicator_walker", w.blockPosition());
        ColonyRuntime.of(c).log("A Replicator Walker was lost at " + w.blockPosition().toShortString() + "; its kit is gone");
        Colonies.changed();
    }
}

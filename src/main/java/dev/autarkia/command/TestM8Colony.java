package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.colony.ColonyMemory;
import dev.autarkia.colony.ColonyPlans;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.ColonySensors;
import dev.autarkia.colony.ColonyTiers;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.Deposits;
import dev.autarkia.colony.KitPrice;
import dev.autarkia.colony.OutpostCoreBlockEntity;
import dev.autarkia.colony.Planner;
import dev.autarkia.colony.Project;
import dev.autarkia.colony.Replication;
import dev.autarkia.colony.ReplicatorFoundryBlockEntity;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.processing.OreOutcropBlock;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.storage.MatterBufferBlockEntity;
import dev.autarkia.worker.WorkerEntity;
import dev.autarkia.worker.WorkerType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jspecify.annotations.Nullable;

/**
 * The T5 colony part of {@code /autarkia test m8} (05b section 11.2 W19, 06 #24 and #25): a T5 colony with its Core at
 * x 400, z 30 of the platform frame (its domain x 336..464, z -34..94; the nanite claims of {@link TestM8Nanites} run in
 * it), a stone strip to x 740 at z -40..100 (open up to y 100 so natural overhangs stay out of the
 * heightmaps the claim box, the site search and the pylon route read), a {@code replicator_foundry}, storage with the walker bill, a matter buffer
 * with the kit matter, two Porters for the deliveries, an HV line in the Core square and a vantite deposit at x 700
 * (300 blocks east). Template 27 proposes the print (Autonomous), the foundry prints, the walker walks out; a block put
 * on its site during the walk makes the check fail TARGET_OBSTRUCTED and it gets a new site; it deploys, the outpost
 * core and the kit appear, and the colony proposes the lamp-lit HV link. Also: the walker count rule, template 28, the
 * quench memory stock target and the T5 caps. Foundry power is kept full by the test; the FE it takes is counted.
 */
final class TestM8Colony {
    private static final List<WorkerEntity> WORKERS = new ArrayList<>();
    private static final long[] FE_ADDED = new long[1];

    private TestM8Colony() {
    }

    private static @Nullable Colony colony() {
        return TestM8.colony();
    }

    private static BlockPos core(TestM8.Run r) {
        return r.at(400, 0, 30);
    }

    private static BlockPos deposit(TestM8.Run r) {
        return r.at(700, 0, 30);
    }

    private static BlockPos foundry(TestM8.Run r) {
        return r.at(430, 0, 70);
    }

    private static BlockPos buffer(TestM8.Run r) {
        return r.at(445, 0, 80);
    }

    private static List<BlockPos> stores(TestM8.Run r) {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            out.add(r.at(420 + i, 0, 85));
        }
        return out;
    }

    /** Return the two halves of the colony area for {@code forceload add} (one command takes at most 256 chunks). */
    private static List<String> areas(TestM8.Run r) {
        List<String> out = new ArrayList<>();
        for (int[] x : new int[][] {{330, 535}, {536, 740}}) {
            BlockPos a = r.at(x[0], 0, -40);
            BlockPos b = r.at(x[1], 0, 100);
            out.add(a.getX() + " " + a.getZ() + " " + b.getX() + " " + b.getZ());
        }
        return out;
    }

    private static @Nullable ReplicatorFoundryBlockEntity foundryBe(SelfTest t, TestM8.Run r) {
        return t.level.getBlockEntity(foundry(r)) instanceof ReplicatorFoundryBlockEntity f ? f : null;
    }

    /** Keep the foundry buffer full (counting the FE added) and the Porters wound (winding is covered by 06 #3). */
    private static void watch(SelfTest t, TestM8.Run r) {
        ReplicatorFoundryBlockEntity f = foundryBe(t, r);
        if (f != null) {
            FE_ADDED[0] += f.energy().fill(f.energy().room());
        }
        for (WorkerEntity w : WORKERS) {
            if (w.isAlive() && w.energy() < w.workerType.capacity() / 2) {
                w.setEnergy(w.workerType.capacity());
            }
        }
    }

    private static InteractionResult use(FakePlayer p, ItemStack stack, BlockPos clicked) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.snapTo(clicked.getX() + 0.5, clicked.getY() + 1, clicked.getZ() + 3.5, 180, 30);
        return stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(clicked).add(0, 0.5, 0), Direction.UP, clicked, false)));
    }

    /** Build the T5 colony: platform strip, Core T5, foundry, storage, matter buffer, Porters, HV line and the far deposit. */
    static void setup(TestM8.Run r) {
        SelfTest t = r.t();
        BlockPos core = core(r);
        t.then(x -> {
            areas(r).forEach(a -> x.command("forceload add " + a));
            AABB box = new AABB(Vec3.atCenterOf(r.at(326, -8, -44)), Vec3.atCenterOf(r.at(744, 40, 104)));
            x.level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
            SelfTest.clearSpans(x.level, box);
            for (Colony c : List.copyOf(Colonies.registry().all())) {
                if (c.owner.equals(TestM6Colony.owner(x).getUUID()) || c.dimension.equals(x.level.dimension()) && Math.abs(c.corePos.getZ() - core.getZ()) <= 300
                    && c.corePos.getX() >= core.getX() - 300 && c.corePos.getX() <= deposit(r).getX() + 300) {
                    Colonies.dissolve(c);
                }
            }
            WORKERS.clear();
            TestM8.colony(null);
        });
        for (int z0 = -40; z0 <= 100; z0 += 2) {
            int a = z0;
            int b = Math.min(100, z0 + 1);
            t.then(x -> {
                TestM8.box(r, 330, 0, a, 740, 100, b, Blocks.AIR.defaultBlockState());
                TestM8.box(r, 330, -1, a, 740, -1, b, Blocks.STONE.defaultBlockState());
            });
        }
        t.then(x -> {
            InteractionResult res = use(TestM6Colony.owner(x), new ItemStack(AutarkiaItems.CORE_SEED.get()), core.below());
            Colony c = Colonies.byCore(x.level, core);
            TestM8.colony(c);
            if (c != null && x.player != null) {
                c.trusted.add(x.player.getUUID());
            }
            x.report("colony setup: the test seed grows a Core at x 400", res == InteractionResult.SUCCESS && c != null, res.toString());
        });
        t.waitUntil("colony setup: the Core blooms", 1200, () -> colony() != null && colony().state == Colony.State.ACTIVE, x -> { });
        for (Item up : List.of(AutarkiaItems.CORE_UPGRADE_T2.get(), AutarkiaItems.CORE_UPGRADE_T3.get(), AutarkiaItems.CORE_UPGRADE_T4.get(),
            AutarkiaItems.CORE_UPGRADE_T5.get())) {
            int[] before = new int[1];
            t.then(x -> {
                before[0] = colony().tier;
                use(TestM6Colony.owner(x), new ItemStack(up), core);
            });
            t.waitUntil("colony setup: " + BuiltInRegistries.ITEM.getKey(up).getPath() + " raises the Core one tier", 160, () -> colony().tier == before[0] + 1,
                x -> x.level.getEntitiesOfClass(ItemEntity.class, new AABB(core).inflate(8)).forEach(Entity::discard));
        }
        t.check("05 1.2: core_upgrade_t5 gives tier 5: domain half-size 64, worker cap 26, 6 outposts of half-size 32, Replicator sub-cap 2",
            () -> colony().tier == 5 && ColonyTiers.domainHalf(5) == 64 && ColonyTiers.workerCap(5) >= 26 && ColonyTiers.outpostHalf(5) == 32
                && WorkerType.REPLICATOR.subCap(5) >= 2,
            () -> "tier " + colony().tier + ", half " + ColonyTiers.domainHalf(colony().tier) + ", cap " + ColonyTiers.workerCap(colony().tier));
        t.then(x -> {
            Colony c = colony();
            c.autonomy = Colony.Autonomy.AUTONOMOUS;
            ColonyPlans.templates().keySet().forEach(k -> c.template(k).enabled = k.equals(Replication.TEMPLATE) || k.equals("hv_link"));
            for (Project p : List.copyOf(c.openProjects())) {
                ColonyRuntime.of(c).jobs.dropProject(c, p);
                c.projects.remove(p);
            }
            for (BlockPos s : stores(r)) {
                TestM1.place(x, s, AutarkiaBlocks.CRATE.get().defaultBlockState());
            }
            ItemStack[] bill = {new ItemStack(AutarkiaItems.WALKER_FRAME.get(), 2), new ItemStack(AutarkiaItems.AI_CORE.get(), 1),
                new ItemStack(AutarkiaItems.NANITE_PASTE.get(), 8), new ItemStack(AutarkiaItems.LATTICE_ALLOY_INGOT.get(), 16),
                new ItemStack(AutarkiaItems.ADVANCED_CIRCUIT.get(), 4), new ItemStack(AutarkiaItems.SERVO.get(), 8),
                new ItemStack(AutarkiaItems.OUTPOST_SEED.get(), 1), new ItemStack(AutarkiaItems.HV_PYLON.get(), 12),
                new ItemStack(AutarkiaItems.HV_CABLE_SPOOL.get(), 48), new ItemStack(AutarkiaItems.INDUSTRIAL_LAMP.get(), 12)};
            for (ItemStack s : bill) {
                ItemStack rest = s.copy();
                for (BlockPos p : stores(r)) {
                    if (rest.isEmpty()) {
                        break;
                    }
                    rest.shrink(TestM1.insertAt(x.level, p, rest));
                }
            }
            boolean ok = TestM1.placeBig(x, foundry(r), AutarkiaBlocks.REPLICATOR_FOUNDRY.get(), Direction.SOUTH)
                && TestM1.placeBig(x, buffer(r), AutarkiaBlocks.MATTER_BUFFER.get(), Direction.NORTH);
            BlockPos a = r.at(345, 0, 85);
            BlockPos b = r.at(365, 0, 85);
            ok &= TestM1.placeBig(x, a, AutarkiaBlocks.HV_PYLON.get(), Direction.NORTH) && TestM1.placeBig(x, b, AutarkiaBlocks.HV_PYLON.get(), Direction.NORTH)
                && Grid.addSpan(x.level, a, b, CableTier.HV) == Grid.SpanResult.OK;
            BlockPos dep = deposit(r);
            for (BlockPos p : BlockPos.betweenClosed(dep.offset(-1, -4, -1), dep.offset(1, -2, 1))) {
                TestM1.place(x, p.immutable(), AutarkiaBlocks.VANTITE_ORE.get().defaultBlockState());
            }
            TestM1.place(x, dep, AutarkiaBlocks.ORE_OUTCROP.get().defaultBlockState().setValue(OreOutcropBlock.ORE, OreOutcropBlock.Ore.VANTITE));
            Deposits.report(c, x.level, dep, "vantite", 400);
            for (int i = 0; i < 2; i++) {
                WorkerEntity w = AutarkiaEntities.PORTER.get().create(x.level, EntitySpawnReason.COMMAND);
                BlockPos p = r.at(425 + 2 * i, 0, 80);
                w.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
                x.level.addFreshEntity(w);
                WORKERS.add(w);
            }
            x.report("colony setup: foundry, matter buffer, storage with the walker bill, HV line in the Core square, deposit 300 blocks east", ok,
                "placed " + ok);
        });
        t.waitUntil("colony setup: the 2 Porters join the colony", 200, () -> WORKERS.stream().allMatch(w -> colony().workers.containsKey(w.getUUID())),
            x -> watch(x, r));
    }

    /** Run the W19 checks and clean up the colony. */
    static void run(TestM8.Run r) {
        SelfTest t = r.t();
        data(r);
        long[] kit = new long[1];
        t.then(x -> {
            kit[0] = KitPrice.of(colony(), x.level);
            if (x.level.getBlockEntity(buffer(r)) instanceof MatterBufferBlockEntity mb && kit[0] != Long.MAX_VALUE) {
                mb.addFree(kit[0] + 4096);
            }
            ColonySensors.sample(colony(), x.level);
        });
        t.check("W19 the kit price is the recipe matter of the outpost_basic cells, and the buffer holds it", () -> kit[0] > 0 && kit[0] != Long.MAX_VALUE
            && dev.autarkia.storage.MatterBank.available(t.level, colony()) >= kit[0], () -> "kit " + kit[0] + " MU, available "
            + dev.autarkia.storage.MatterBank.available(t.level, colony()));
        t.check("W19 every print rule holds: replicator.ready 1", () -> Planner.signal(colony(), t.level, "replicator.ready") == 1,
            () -> "ready " + Planner.signal(colony(), t.level, "replicator.ready") + ", foundry " + foundryBe(t, r) + ", target "
                + Replication.target(colony(), t.level) + ", room " + Replication.room(colony()));
        Project[] pr = new Project[1];
        t.then(x -> {
            Planner.pass(colony(), x.level);
            pr[0] = colony().openProjects().stream().filter(p -> p.template.getPath().equals(Replication.TEMPLATE)).findFirst().orElse(null);
            if (pr[0] != null && pr[0].state == Project.State.PROPOSED) {
                Planner.approve(colony(), pr[0], false);
            }
            x.report("W19 template 27 proposes the print at the foundry", pr[0] != null && pr[0].origin.equals(foundry(r)),
                pr[0] == null ? ColonyRuntime.of(colony()).log.toString() : "origin " + pr[0].origin.toShortString());
        });
        t.waitUntil("W19 the print project finishes and orders the print (deliveries of the bill to the foundry)", 2400,
            () -> pr[0] != null && pr[0].state == Project.State.DONE && foundryBe(t, r) != null && foundryBe(t, r).ordered(), x -> watch(x, r),
            () -> pr[0] == null ? "no project" : TestM6Colony.projectState(x(r), colony(), pr[0]));
        long[] started = new long[2];
        int[] costBefore = new int[1];
        t.waitUntil("W19 the Porters deliver the bill and the foundry starts printing", 6000,
            () -> foundryBe(t, r).status() == ReplicatorFoundryBlockEntity.Status.RUNNING, x -> {
                watch(x, r);
                started[0] = x.level.getGameTime();
                FE_ADDED[0] = 0;
                costBefore[0] = colony().workerCost();
            }, () -> "status " + foundryBe(t, r).status() + ", deliveries " + colony().deliveries + ", deliver jobs " + ColonyRuntime.of(colony()).jobs.all()
                .stream().filter(j -> j.type == ColonyJob.Type.DELIVER).map(j -> j.item + " " + j.status + " " + j.assignee + " att " + j.attempts).toList()
                + ", seeds in storage " + dev.autarkia.colony.ItemLedger.stock(t.level, ColonyRuntime.of(colony()), AutarkiaItems.OUTPOST_SEED.get())
                + ", foundry slots " + java.util.stream.IntStream.range(0, ReplicatorFoundryBlockEntity.SLOTS).mapToObj(i -> foundryBe(t, r).inventory().get(i))
                .toList());
        t.check("W19 the print took the kit matter and reserved the deposit", () -> colony().deposits.stream().anyMatch(d -> d.outpost().isPresent())
            && dev.autarkia.storage.MatterBank.available(t.level, colony()) < kit[0] + 4096, () -> "deposits " + colony().deposits);
        WorkerEntity[] walker = new WorkerEntity[1];
        t.waitUntil("W19 after 2,400 ticks the walker steps out of the foundry", 3000, () -> !colony().replicatorsActive.isEmpty(), x -> watch(x, r));
        t.then(x -> {
            started[1] = x.level.getGameTime();
            UUID id = colony().replicatorsActive.isEmpty() ? null : colony().replicatorsActive.get(0);
            walker[0] = id != null && x.level.getEntity(id) instanceof WorkerEntity w ? w : null;
        });
        t.check("W19 the print ran 2,400 ticks at 2,000 FE/t (4,800,000 FE)", () -> Math.abs(started[1] - started[0] - 2400) <= 20
            && Math.abs(FE_ADDED[0] - 4800000) <= 2 * ReplicatorFoundryBlockEntity.MAX_IN, () -> (started[1] - started[0]) + " ticks, " + FE_ADDED[0] + " FE");
        t.check("06#24 the walker is a T5 Replicator with a full battery, cap cost 3, and a kit job toward the deposit", () -> walker[0] != null
            && walker[0].workerType == WorkerType.REPLICATOR && walker[0].energy() == 1000000 && colony().workerCost() == costBefore[0] + 3
            && kitJob(colony()) != null, () -> walker[0] == null ? "no walker" : walker[0].energy() + " FE, cost " + colony().workerCost() + " (was "
            + costBefore[0] + "), job " + kitJob(colony()));
        t.then(x -> {
            Colony c = colony();
            c.replicatorsActive.add(UUID.randomUUID());
            ColonySensors.sample(c, x.level);
            x.report("W19 with 2 walkers active a third print never starts (replicator.ready 0, no room)",
                Planner.signal(c, x.level, "replicator.ready") == 0 && !Replication.room(c), "active " + c.replicatorsActive.size());
            c.replicatorsActive.remove(c.replicatorsActive.size() - 1);
        });
        BlockPos[] firstSite = new BlockPos[1];
        t.waitUntil("06#25 the walker walks toward its site", 8000, () -> {
            ColonyJob j = kitJob(colony());
            return walker[0] != null && walker[0].currentJob() == j && j != null && walker[0].distanceToSqr(Vec3.atCenterOf(j.target)) < 60 * 60
                && walker[0].distanceToSqr(Vec3.atCenterOf(j.target)) > 24 * 24;
        }, x -> watch(x, r), () -> (walker[0] == null ? "no walker" : TestM6Colony.workerState(walker[0])) + ", job " + kitJob(colony()));
        t.then(x -> {
            ColonyJob j = kitJob(colony());
            firstSite[0] = j == null ? BlockPos.ZERO : j.target;
            if (j != null) {
                TestM1.place(x, firstSite[0], Blocks.STONE.defaultBlockState());
            }
            x.report("06#25 rig: a stone block on the walker's site while it walks", j != null, "job " + j);
        });
        t.waitUntil("06#25 a block on the site during travel: the site check fails TARGET_OBSTRUCTED and the walker gets a new site", 4000, () -> {
            ColonyJob j = kitJob(colony());
            return j != null && !j.target.equals(firstSite[0]) && ColonyRuntime.of(colony()).log.stream().anyMatch(l -> l.contains("failed: target_obstructed"));
        }, x -> watch(x, r), () -> "job " + kitJob(colony()) + ", walker " + (walker[0] == null ? "none" : TestM6Colony.workerState(walker[0])));
        int[] costAtDeploy = new int[1];
        t.then(x -> costAtDeploy[0] = colony().workerCost());
        t.waitUntil("06#24 the walker deploys (100 ticks), the outpost appears and the walker entity is gone", 6000,
            () -> !colony().outposts.isEmpty() && walker[0] != null && (walker[0].isRemoved() || !walker[0].isAlive()), x -> watch(x, r),
            () -> (walker[0] == null ? "no walker" : TestM6Colony.workerState(walker[0])) + ", outposts " + colony().outposts.size());
        t.check("06#24 the cap drops by 3, the walker leaves the active list, the deposit belongs to the outpost", () -> {
            Colony c = colony();
            Colony.Outpost o = c.outposts.get(0);
            return c.workerCost() == costAtDeploy[0] - 3 && c.replicatorsActive.isEmpty()
                && c.deposits.stream().anyMatch(d -> d.outpost().filter(o.id::equals).isPresent());
        }, () -> "cost " + colony().workerCost() + " (was " + costAtDeploy[0] + "), active " + colony().replicatorsActive + ", deposits " + colony().deposits);
        t.waitUntil("W19 the kit builds the outpost through rising blocks: depot, hv_pylon, wind tower, recharge dock, jaw crusher, HV span", 1200,
            () -> kitBuilt(t), x -> watch(x, r), () -> core(t) == null ? "no core" : core(t).kitLeft() + " kit steps left, built mask " + kitMask(t) + " of 63");
        t.check("W19 the outpost core grows and holds its chunk ticket; colony events walker_outpost_founded and replicator_deploy fired", () -> {
            Colony.Outpost o = colony().outposts.get(0);
            return ColonyRuntime.of(colony()).log.stream().anyMatch(l -> l.startsWith("A Replicator Walker founded an outpost"))
                && t.level.getChunkSource().getChunkNow(o.corePos.getX() >> 4, o.corePos.getZ() >> 4) != null
                && (t.player == null || advancement(t, "colony/not_built_by_you") && advancement(t, "workers/replicator_deploy"));
        }, () -> colony().outposts.get(0).corePos.toShortString());
        t.waitUntil("W19 after the core grows the colony proposes the lamp-lit HV link back to the Core", 1200, () -> {
            ColonySensors.sample(colony(), t.level);
            Planner.pass(colony(), t.level);
            return colony().openProjects().stream().anyMatch(p -> p.template.getPath().equals("hv_link")
                && p.cells.stream().anyMatch(cl -> cl.state().is(AutarkiaBlocks.INDUSTRIAL_LAMP.get())));
        }, x -> watch(x, r), () -> {
            List<String> log = ColonyRuntime.of(colony()).log;
            return "link_need " + Planner.signal(colony(), t.level, "outpost.link_need") + ", projects "
                + colony().openProjects().stream().map(p -> p.template.getPath()).toList() + ", log " + log.subList(Math.max(0, log.size() - 4), log.size());
        });
        memory(r);
        matterTemplate(r);
        t.then(x -> {
            Colony c = colony();
            if (c != null) {
                Colonies.dissolve(c);
            }
            TestM8.colony(null);
        });
    }

    private static SelfTest x(TestM8.Run r) {
        return r.t();
    }

    private static boolean advancement(SelfTest t, String id) {
        var adv = t.level.getServer().getAdvancements().get(Autarkia.id(id));
        return adv != null && t.player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    private static @Nullable ColonyJob kitJob(Colony c) {
        return ColonyRuntime.of(c).jobs.all().stream().filter(j -> j.type == ColonyJob.Type.FOUND_OUTPOST && j.item == null
            && j.status != ColonyJob.Status.DONE).findFirst().orElse(null);
    }

    private static @Nullable OutpostCoreBlockEntity core(SelfTest t) {
        Colony c = colony();
        return c == null || c.outposts.isEmpty() ? null : t.level.getBlockEntity(c.outposts.get(0).corePos) instanceof OutpostCoreBlockEntity o ? o : null;
    }

    private static boolean kitBuilt(SelfTest t) {
        OutpostCoreBlockEntity core = core(t);
        return core != null && core.kitLeft() == 0 && kitMask(t) == 63;
    }

    /** Return one bit per kit part found around the outpost core (depot 1, wind tower 2, dock 4, crusher 8, pylon 16, HV span 32). */
    private static int kitMask(SelfTest t) {
        OutpostCoreBlockEntity core = core(t);
        if (core == null) {
            return 0;
        }
        BlockPos at = core.getBlockPos();
        int found = 0;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-8, -1, -8), at.offset(8, 2, 8))) {
            var b = t.level.getBlockState(p);
            if (b.is(AutarkiaBlocks.DEPOT.get()) || b.is(AutarkiaBlocks.WIND_TOWER.get()) || b.is(AutarkiaBlocks.RECHARGE_DOCK.get())
                || b.is(AutarkiaBlocks.JAW_CRUSHER.get()) || b.is(AutarkiaBlocks.HV_PYLON.get())) {
                found |= b.is(AutarkiaBlocks.DEPOT.get()) ? 1 : b.is(AutarkiaBlocks.WIND_TOWER.get()) ? 2 : b.is(AutarkiaBlocks.RECHARGE_DOCK.get()) ? 4
                    : b.is(AutarkiaBlocks.JAW_CRUSHER.get()) ? 8 : 16;
            }
        }
        boolean spanned = Grid.of(t.level).data().spans.values().stream().anyMatch(s -> s.tier == CableTier.HV && s.posA().distSqr(at) < 16 * 16);
        return found | (spanned ? 32 : 0);
    }

    private static void data(TestM8.Run r) {
        r.t().check("05 data: templates 27 and 28 (tier 5), plan matter_buffer, the foundry, the Replicator recipe at the foundry (2,400 ticks, "
            + "2,000 FE/t), the walker and rising block entities", () -> {
                var t27 = ColonyPlans.templates().get(Replication.TEMPLATE);
                var t28 = ColonyPlans.templates().get("matter_buffer");
                var rec = Replication.recipe(r.t().level);
                return t27 != null && t27.tier() == 5 && t27.handler().filter("replicator"::equals).isPresent() && t28 != null && t28.tier() == 5
                    && ColonyPlans.plan(Autarkia.id("matter_buffer")) != null && BuiltInRegistries.BLOCK.containsKey(Autarkia.id("replicator_foundry"))
                    && BuiltInRegistries.MENU.containsKey(Autarkia.id("replicator_foundry")) && rec.isPresent() && rec.get().time() == 2400
                    && rec.get().fePerTick() == 2000 && rec.get().minTier() == 5 && BuiltInRegistries.ENTITY_TYPE.containsKey(Autarkia.id("replicator_walker"))
                    && BuiltInRegistries.ENTITY_TYPE.containsKey(Autarkia.id("rising_block"));
            }, () -> "templates " + ColonyPlans.templates().keySet().stream().filter(k -> ColonyPlans.templates().get(k).tier() == 5).toList());
    }

    private static void memory(TestM8.Run r) {
        SelfTest t = r.t();
        int[] before = new int[1];
        t.then(x -> {
            before[0] = colony().stockTargets.getOrDefault(AutarkiaItems.COOLANT_CELL.get(), 64);
            ColonyMemory.event(colony(), "quench", "span", r.at(355, 0, 85));
        });
        t.check("05 6.2 a quench memory raises the coolant_cell stock target by 8", () -> colony().stockTargets.getOrDefault(AutarkiaItems.COOLANT_CELL.get(), 0)
            == before[0] + 8, () -> before[0] + " -> " + colony().stockTargets.get(AutarkiaItems.COOLANT_CELL.get()));
    }

    private static void matterTemplate(TestM8.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            Colony c = colony();
            c.template("matter_buffer").enabled = true;
            c.template("matter_buffer").holds.clear();
            ColonyRuntime.of(c).forced.put("S27", new ColonyRuntime.Forced(0.95, x.level.getGameTime() + 400));
        });
        t.waitUntil("05 template 28: with S27 at 0.95 the planner proposes a matter_buffer near the storage", 600, () -> {
            ColonySensors.sample(colony(), t.level);
            Planner.pass(colony(), t.level);
            return colony().projects.stream().anyMatch(p -> p.template.getPath().equals("matter_buffer"));
        }, x -> { }, () -> "S27 " + Planner.signal(colony(), t.level, "S27") + ", log " + ColonyRuntime.of(colony()).log);
    }
}

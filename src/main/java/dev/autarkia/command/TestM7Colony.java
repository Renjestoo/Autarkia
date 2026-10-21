package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.colony.ColonyPlans;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.ColonySensors;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.Deposits;
import dev.autarkia.colony.OutpostCoreBlockEntity;
import dev.autarkia.colony.OutpostProjects;
import dev.autarkia.colony.Outposts;
import dev.autarkia.colony.Planner;
import dev.autarkia.colony.Project;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.processing.OreOutcropBlock;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.storage.DepotBlockEntity;
import dev.autarkia.worker.RechargeDockBlockEntity;
import dev.autarkia.worker.WorkerEntity;
import dev.autarkia.worker.WorkerTravel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jspecify.annotations.Nullable;

/**
 * The T4 colony part of {@code /autarkia test m7} (05b section 11.2 W15 and W15 catch-up) on its own strip x 424..716,
 * z 64..176 of the platform frame (far from the other parts and from the player): a T4 colony at x 480 with an HV line
 * in its square, storage with the outpost and pylon line bill, a Porter, two Builder Drones, a Cable-Layer and a
 * Sentinel, and a known vantite deposit 200 blocks east. The outpost template trigger is real (deposit beyond the
 * domain, S11[vantite_ingot] below 0.5) with its 24,000-tick hold set directly; the player approves. A carrier with the
 * Sentinel escort plants the seed, the outpost grows and is built, then the pylon line goes up from the outpost toward
 * the Core one pylon at a time; {@code outpost list} shows link HV and only the core chunk stays forced once the test
 * force-load is gone. The catch-up credits 12,000 ticks with a set profile. Worker batteries are kept full; recharging is
 * covered by 06 #20 and #23.
 */
final class TestM7Colony {
    private static final UUID[] ID = new UUID[1];
    private static final List<WorkerEntity> WORKERS = new ArrayList<>();
    private static final WorkerEntity[] SENTINEL = new WorkerEntity[1];

    private TestM7Colony() {
    }

    static void run(TestM7.Run r) {
        data(r);
        setup(r);
        outpost(r);
        pylonLine(r);
        forcedChunks(r);
        catchUp(r);
        r.t().then(x -> {
            Colony c = colony();
            if (c != null) {
                Colonies.dissolve(c);
            }
        });
    }

    private static @Nullable Colony colony() {
        return ID[0] == null ? null : Colonies.registry().get(ID[0]);
    }

    private static BlockPos core(TestM7.Run r) {
        return r.at(480, 0, 120);
    }

    private static BlockPos deposit(TestM7.Run r) {
        return r.at(680, 0, 120);
    }

    private static List<BlockPos> stores(TestM7.Run r) {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            out.add(r.at(468, 0, 108 + i));
        }
        return out;
    }

    private static List<BlockPos> docks(TestM7.Run r) {
        return List.of(r.at(470, 0, 130), r.at(472, 0, 130), r.at(474, 0, 130));
    }

    private static String area(TestM7.Run r) {
        BlockPos a = r.at(424, 0, 64);
        BlockPos b = r.at(716, 0, 176);
        return a.getX() + " " + a.getZ() + " " + b.getX() + " " + b.getZ();
    }

    /** Keep the docks and the worker batteries full. */
    private static void watch(SelfTest t, TestM7.Run r) {
        for (BlockPos d : docks(r)) {
            if (t.level.getBlockEntity(d) instanceof RechargeDockBlockEntity dock) {
                dock.buffer().fill(50000);
            }
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

    private static <T extends WorkerEntity> T spawn(SelfTest t, EntityType<T> type, BlockPos at) {
        T w = type.create(t.level, EntitySpawnReason.COMMAND);
        w.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        t.level.addFreshEntity(w);
        WORKERS.add(w);
        return w;
    }

    private static void data(TestM7.Run r) {
        r.t().check("05 data: templates 21 to 26, plans outpost_basic, sentinel_post, wind_tower and hydro, core_upgrade_t4, outpost seed and core load",
            () -> {
                for (String id : List.of("outpost", "hv_link", "cable_car", "sentinel_post", "wind_tower", "hydro_turbine")) {
                    if (!ColonyPlans.templates().containsKey(id) || ColonyPlans.templates().get(id).tier() != 4) {
                        return false;
                    }
                }
                for (String id : List.of("outpost_basic", "sentinel_post", "wind_tower", "hydro")) {
                    if (ColonyPlans.plan(Autarkia.id(id)) == null) {
                        return false;
                    }
                }
                return BuiltInRegistries.ITEM.containsKey(Autarkia.id("core_upgrade_t4")) && BuiltInRegistries.ITEM.containsKey(Autarkia.id("outpost_seed"))
                    && BuiltInRegistries.BLOCK.containsKey(Autarkia.id("outpost_core")) && r.t().level.getServer().getAdvancements()
                    .get(Autarkia.id("colony/frontier")) != null && r.t().level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.RECIPE, Autarkia.id("fabricating/core_upgrade_t4"))).isPresent();
            }, () -> ColonyPlans.templates().keySet().stream().filter(k -> ColonyPlans.templates().get(k).tier() == 4).toList().toString());
    }

    private static void setup(TestM7.Run r) {
        SelfTest t = r.t();
        BlockPos core = core(r);
        t.then(x -> {
            x.command("forceload add " + area(r));
            AABB box = new AABB(Vec3.atCenterOf(r.at(420, -8, 60)), Vec3.atCenterOf(r.at(720, 40, 180)));
            x.level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
            SelfTest.clearSpans(x.level, box);
            for (Colony c : List.copyOf(Colonies.registry().all())) {
                if (c.owner.equals(TestM6Colony.owner(x).getUUID()) || c.dimension.equals(x.level.dimension()) && Math.abs(c.corePos.getZ() - core.getZ()) <= 200
                    && c.corePos.getX() >= core.getX() - 200 && c.corePos.getX() <= deposit(r).getX() + 200) {
                    Colonies.dissolve(c);
                }
            }
            WORKERS.clear();
        });
        for (int z0 = 64; z0 <= 176; z0 += 6) {
            int a = z0;
            int b = Math.min(176, z0 + 5);
            t.then(x -> {
                TestM7.box(r, 424, 0, a, 716, 24, b, Blocks.AIR.defaultBlockState());
                TestM7.box(r, 424, -1, a, 716, -1, b, Blocks.STONE.defaultBlockState());
            });
        }
        t.then(x -> {
            InteractionResult res = use(TestM6Colony.owner(x), new ItemStack(AutarkiaItems.CORE_SEED.get()), core.below());
            Colony c = Colonies.byCore(x.level, core);
            ID[0] = c == null ? null : c.id;
            if (c != null && x.player != null) {
                c.trusted.add(x.player.getUUID());
            }
            x.report("W15 setup: the test seed grows a Core at x 480", res == InteractionResult.SUCCESS && c != null, res.toString());
        });
        t.waitUntil("W15 setup: the Core blooms", 1200,() -> colony() != null && colony().state == Colony.State.ACTIVE, x -> { });
        for (Item up : List.of(AutarkiaItems.CORE_UPGRADE_T2.get(), AutarkiaItems.CORE_UPGRADE_T3.get(), AutarkiaItems.CORE_UPGRADE_T4.get())) {
            int[] before = new int[1];
            t.then(x -> {
                before[0] = colony().tier;
                use(TestM6Colony.owner(x), new ItemStack(up), core);
            });
            t.waitUntil("W15 setup: " + BuiltInRegistries.ITEM.getKey(up).getPath() + " raises the Core one tier", 160, () -> colony().tier == before[0] + 1,
                x -> x.level.getEntitiesOfClass(ItemEntity.class, new AABB(core).inflate(8)).forEach(Entity::discard));
        }
        t.then(x -> {
            Colony c = colony();
            ColonyPlans.templates().keySet().forEach(k -> c.template(k).enabled = k.equals("outpost") || k.equals("hv_link"));
            for (BlockPos d : docks(r)) {
                TestM1.place(x, d, AutarkiaBlocks.RECHARGE_DOCK.get().defaultBlockState());
            }
            for (BlockPos s : stores(r)) {
                TestM1.place(x, s, AutarkiaBlocks.CRATE.get().defaultBlockState());
            }
            stock(x, r, new ItemStack(AutarkiaItems.OUTPOST_SEED.get(), 1), new ItemStack(AutarkiaItems.DEPOT.get(), 1),
                new ItemStack(AutarkiaItems.HV_PYLON.get(), 12), new ItemStack(AutarkiaItems.WIND_TOWER.get(), 1),
                new ItemStack(AutarkiaItems.RECHARGE_DOCK.get(), 1), new ItemStack(AutarkiaItems.FOUNDATION.get(), 4),
                new ItemStack(AutarkiaItems.JAW_CRUSHER.get(), 1), new ItemStack(AutarkiaItems.HV_CABLE_SPOOL.get(), 48),
                new ItemStack(AutarkiaItems.INDUSTRIAL_LAMP.get(), 12), new ItemStack(AutarkiaItems.TRANSFORMER.get(), 1));
            BlockPos a = r.at(500, 0, 120);
            BlockPos b = r.at(520, 0, 120);
            boolean ok = TestM1.placeBig(x, a, AutarkiaBlocks.HV_PYLON.get(), Direction.NORTH) && TestM1.placeBig(x, b, AutarkiaBlocks.HV_PYLON.get(), Direction.NORTH)
                && Grid.addSpan(x.level, a, b, CableTier.HV) == Grid.SpanResult.OK;
            BlockPos dep = deposit(r);
            for (BlockPos p : BlockPos.betweenClosed(dep.offset(-1, -4, -1), dep.offset(1, -2, 1))) {
                TestM1.place(x, p.immutable(), AutarkiaBlocks.VANTITE_ORE.get().defaultBlockState());
            }
            TestM1.place(x, dep, AutarkiaBlocks.ORE_OUTCROP.get().defaultBlockState().setValue(OreOutcropBlock.ORE, OreOutcropBlock.Ore.VANTITE));
            Deposits.report(c, x.level, dep, "vantite", 400);
            c.stockTargets.put(AutarkiaItems.VANTITE_INGOT.get(), 64);
            spawn(x, AutarkiaEntities.PORTER.get(), r.at(476, 0, 126));
            spawn(x, AutarkiaEntities.BUILDER_DRONE.get(), r.at(470, 3, 126));
            spawn(x, AutarkiaEntities.BUILDER_DRONE.get(), r.at(472, 3, 126));
            spawn(x, AutarkiaEntities.CABLE_LAYER_DRONE.get(), r.at(474, 3, 126));
            SENTINEL[0] = spawn(x, AutarkiaEntities.SENTINEL.get(), r.at(478, 3, 126));
            x.report("W15 setup: a T4 colony with an HV line (2 hv_pylons, one HV span) in its square, storage with the outpost and line bill",
                ok && c.tier == 4, "HV line " + ok);
        });
        t.waitUntil("W15 setup: Porter, 2 Builder Drones, Cable-Layer and Sentinel join the colony", 200,
            () -> WORKERS.stream().allMatch(w -> colony().workers.containsKey(w.getUUID())), x -> watch(x, r));
    }

    private static void stock(SelfTest t, TestM7.Run r, ItemStack... stacks) {
        for (ItemStack s : stacks) {
            ItemStack rest = s.copy();
            for (BlockPos p : stores(r)) {
                rest.shrink(TestM1.insertAt(t.level, p, rest));
                if (rest.isEmpty()) {
                    break;
                }
            }
        }
    }

    private static @Nullable Project project(String template) {
        Colony c = colony();
        return c == null ? null : c.projects.stream().filter(p -> p.template.getPath().equals(template) && p.state.open()).findFirst().orElse(null);
    }

    private static void outpost(TestM7.Run r) {
        SelfTest t = r.t();
        Project[] pr = new Project[1];
        ColonyJob[] found = new ColonyJob[1];
        int[] samples = new int[2];
        double[] range = {Double.MAX_VALUE, 0};
        t.then(x -> ColonySensors.sample(colony(), x.level));
        t.check("W15 the deposit 200 blocks out is known (S16 1) and its metal is short: S11.vantite_ingot below 0.5, outpost.need 1",
            () -> Planner.signal(colony(), t.level, "S16") == 1 && Planner.signal(colony(), t.level, "S11.vantite_ingot") < 0.5
                && Planner.signal(colony(), t.level, "outpost.need") == 1 && Planner.signal(colony(), t.level, "outpost.room") == 6,
            () -> String.format(Locale.ROOT, "S16 %.0f, S11 %.2f, need %.0f, room %.0f", Planner.signal(colony(), t.level, "S16"),
                Planner.signal(colony(), t.level, "S11.vantite_ingot"), Planner.signal(colony(), t.level, "outpost.need"),
                Planner.signal(colony(), t.level, "outpost.room")));
        t.then(x -> {
            Colony.TemplateState ts = colony().template("outpost");
            while (ts.holds.size() < 2) {
                ts.holds.add(0);
            }
            ts.holds.set(0, 24000);
            Planner.pass(colony(), x.level);
            pr[0] = project("outpost");
            x.report("W15 after the 24,000-tick hold the planner proposes an outpost at the deposit (site within 24 blocks)", pr[0] != null
                && OutpostProjects.horizontal(pr[0].origin, deposit(r)) <= 32, pr[0] == null ? ColonyRuntime.of(colony()).log.toString()
                : "origin " + pr[0].origin.toShortString());
            if (pr[0] != null) {
                Planner.approve(colony(), pr[0], false);
            }
        });
        java.util.function.Consumer<SelfTest> sample = x -> {
            watch(x, r);
            Entity carrier = found[0] == null || found[0].assignee == null ? null : x.level.getEntity(found[0].assignee);
            ColonyJob escort = SENTINEL[0].currentJob();
            if (carrier instanceof WorkerEntity w && w.state() == WorkerEntity.State.TRAVELLING && escort != null && escort.type == ColonyJob.Type.ESCORT) {
                double d = SENTINEL[0].distanceTo(w);
                samples[0]++;
                samples[1] += d <= 12 ? 1 : 0;
                range[0] = Math.min(range[0], d);
                range[1] = Math.max(range[1], d);
            }
        };
        t.waitUntil("W15 before the site is levelled a carrier takes FOUND_OUTPOST with the seed and a Sentinel takes the ESCORT", 12000, () -> {
            for (ColonyJob j : ColonyRuntime.of(colony()).jobs.all()) {
                if (j.type == ColonyJob.Type.FOUND_OUTPOST && j.assignee != null) {
                    found[0] = j;
                }
            }
            return found[0] != null && SENTINEL[0].currentJob() != null && SENTINEL[0].currentJob().type == ColonyJob.Type.ESCORT;
        }, sample, () -> pr[0] == null ? "no project" : TestM6Colony.projectState(x(r), colony(), pr[0]) + " | sentinel " + TestM6Colony.workerState(SENTINEL[0]));
        t.waitUntil("W15 the carrier walks out and plants the seed; the Sentinel stays with it on the way", 12000, () -> !colony().outposts.isEmpty(), sample,
            () -> TestM6Colony.projectState(x(r), colony(), pr[0]));
        t.check("W15 the Sentinel escorted the carrier (within 12 blocks in 90% of the samples while it travelled)", () -> samples[0] >= 20
            && samples[1] >= 0.9 * samples[0], () -> samples[1] + " of " + samples[0] + " samples, range " + String.format(Locale.ROOT, "%.1f..%.1f",
            range[0], range[1]) + ", sentinel " + TestM6Colony.workerState(SENTINEL[0]));
        t.waitUntil("W15 the outpost core grows (200 ticks)", 400, () -> core(colony()) != null && core(colony()).grown(), x -> watch(x, r));
        t.check("W15 the outpost is recorded: its core chunk holds a ticket, the deposit is claimed, colony event outpost_founded fired", () -> {
            Colony.Outpost o = colony().outposts.get(0);
            return colony().deposits.stream().anyMatch(d -> d.outpost().filter(o.id::equals).isPresent())
                && ColonyRuntime.of(colony()).log.stream().anyMatch(l -> l.startsWith("Outpost founded"))
                && t.level.getChunkSource().getChunkNow(o.corePos.getX() >> 4, o.corePos.getZ() >> 4) != null;
        }, () -> colony().outposts.get(0).corePos.toShortString());
        t.then(x -> ColonySensors.sample(colony(), x.level));
        t.check("W15 the grown core is a worker charger with the seed's 64,000 FE", () -> core(colony()).charge().stored() > 0
            && ColonyRuntime.of(colony()).chargers.contains(colony().outposts.get(0).corePos), () -> core(colony()).charge().stored() + " FE, chargers "
            + ColonyRuntime.of(colony()).chargers);
        t.waitUntil("W15 the workers build outpost_basic around the core (depot, hv_pylon, wind tower, dock, crusher on foundations, HV span)", 30000,
            () -> pr[0].state == Project.State.DONE, x -> watch(x, r), () -> TestM6Colony.projectState(x(r), colony(), pr[0]));
        t.check("W15 the outpost depot stands in the core chunk", () -> {
            Colony.Outpost o = colony().outposts.get(0);
            return Outposts.depots(t.level, o).stream().anyMatch(d -> (d.getBlockPos().getX() >> 4) == (o.corePos.getX() >> 4)
                && (d.getBlockPos().getZ() >> 4) == (o.corePos.getZ() >> 4));
        }, () -> Outposts.depots(t.level, colony().outposts.get(0)).size() + " depots");
    }

    private static SelfTest x(TestM7.Run r) {
        return r.t();
    }

    private static @Nullable OutpostCoreBlockEntity core(@Nullable Colony c) {
        net.minecraft.server.level.ServerLevel level = c == null ? null : Colonies.level(c);
        return level == null || c.outposts.isEmpty() ? null
            : level.getBlockEntity(c.outposts.get(0).corePos) instanceof OutpostCoreBlockEntity be ? be : null;
    }

    private static double maxGap(@Nullable BlockPos start, List<BlockPos> pylons) {
        double max = 0;
        BlockPos prev = start;
        for (BlockPos p : pylons) {
            max = prev == null ? max : Math.max(max, OutpostProjects.horizontal(prev, p));
            prev = p;
        }
        return max;
    }

    private static BlockPos neighbour(long chunk, BlockPos core) {
        return new BlockPos(net.minecraft.world.level.ChunkPos.getX(chunk) * 16 + 8, core.getY(), net.minecraft.world.level.ChunkPos.getZ(chunk) * 16 + 8);
    }

    private static it.unimi.dsi.fastutil.longs.LongSet vanillaForced(SelfTest t) {
        return t.level.getDataStorage().computeIfAbsent(net.minecraft.world.level.TicketStorage.TYPE).getForceLoadedChunks();
    }

    private static java.util.function.LongPredicate inside(TestM7.Run r) {
        BlockPos a = r.at(424, 0, 64);
        BlockPos b = r.at(716, 0, 176);
        return packed -> {
            int cx = net.minecraft.world.level.ChunkPos.getX(packed);
            int cz = net.minecraft.world.level.ChunkPos.getZ(packed);
            return cx >= a.getX() >> 4 && cx <= b.getX() >> 4 && cz >= a.getZ() >> 4 && cz <= b.getZ() >> 4;
        };
    }

    private static void pylonLine(TestM7.Run r) {
        SelfTest t = r.t();
        Project[] pr = new Project[1];
        List<BlockPos> pylons = new ArrayList<>();
        List<Long> placedAt = new ArrayList<>();
        int[] maxOpen = new int[1];
        t.then(x -> {
            Project open = project("hv_link");
            ColonySensors.sample(colony(), x.level);
            x.report("W15 the grown outpost has no power link: outpost.link_need 1, or the planner already proposed its hv_link",
                open != null || Planner.signal(colony(), x.level, "outpost.link_need") == 1,
                open != null ? "hv_link already " + open.state.getSerializedName() : "link_need " + Planner.signal(colony(), x.level, "outpost.link_need"));
            if (open == null) {
                Planner.pass(colony(), x.level);
            }
            pr[0] = project("hv_link");
            if (pr[0] != null) {
                for (Project.Cell cell : pr[0].cells) {
                    if (cell.state().is(AutarkiaBlocks.HV_PYLON.get())) {
                        pylons.add(BlockPos.of(cell.pos()));
                        placedAt.add(-1L);
                    }
                }
                if (pr[0].state == Project.State.PROPOSED) {
                    Planner.approve(colony(), pr[0], false);
                }
            }
            BlockPos start = pr[0] == null ? null : pr[0].origin;
            boolean ordered = start != null && !pylons.isEmpty();
            for (int i = 1; ordered && i < pylons.size(); i++) {
                ordered = OutpostProjects.horizontal(pylons.get(i), core(r)) < OutpostProjects.horizontal(pylons.get(i - 1), core(r));
            }
            x.report("W15 hv_link plans a pylon line from the outpost toward the Core, a pylon at most 44 blocks after the last", ordered
                && maxGap(start, pylons) <= 44.5, pr[0] == null ? ColonyRuntime.of(colony()).log.toString()
                : pylons.size() + " pylons, max gap " + String.format(Locale.ROOT, "%.1f", maxGap(start, pylons)));
        });
        t.waitUntil("W15 Cable-Layers and drones place the pylons one at a time and string the HV spans", 40000, () -> pr[0].state == Project.State.DONE, x -> {
            watch(x, r);
            for (int i = 0; i < pylons.size(); i++) {
                if (placedAt.get(i) < 0 && x.level.getBlockState(pylons.get(i)).is(AutarkiaBlocks.HV_PYLON.get())) {
                    placedAt.set(i, x.level.getGameTime());
                }
            }
            long open = ColonyRuntime.of(colony()).jobs.all().stream().filter(j -> pr[0].id.equals(j.projectId) && j.type == ColonyJob.Type.PLACE
                && j.status != ColonyJob.Status.DONE).count();
            maxOpen[0] = (int) Math.max(maxOpen[0], open);
        }, () -> TestM6Colony.projectState(x(r), colony(), pr[0]));
        t.check("W15 one pylon at a time: never more than one open PLACE job, the pylons went up in route order", () -> {
            for (int i = 1; i < placedAt.size(); i++) {
                if (placedAt.get(i) < placedAt.get(i - 1)) {
                    return false;
                }
            }
            return maxOpen[0] <= 1 && !placedAt.contains(-1L);
        }, () -> "max open " + maxOpen[0] + ", placed at " + placedAt);
        t.check("W15 the line is HV and joined: outpost list shows link HV, the outpost core is on the colony HV network", () -> {
            Colony.Outpost o = colony().outposts.get(0);
            return (o.link == Colony.Link.HV || o.link == Colony.Link.BOTH) && Outposts.list(colony()).get(0).contains("link hv")
                && ColonyRuntime.of(colony()).log.stream().anyMatch(l -> l.contains("linked by HV"));
        }, () -> Outposts.list(colony()).toString());
    }

    private static void forcedChunks(TestM7.Run r) {
        SelfTest t = r.t();
        long[] other = new long[1];
        t.then(x -> {
            x.command("forceload remove " + area(r));
            Colony.Outpost o = colony().outposts.get(0);
            other[0] = net.minecraft.world.level.ChunkPos.pack((o.corePos.getX() >> 4) + 1, o.corePos.getZ() >> 4);
        });
        t.waitUntil("W15 only one forced chunk: with the test force-load gone only the outpost core chunk still ticks entities (a forced chunk "
            + "lets its neighbours tick blocks only), no travel tickets", 2400, () -> {
                Colony.Outpost o = colony().outposts.get(0);
                return t.level.isPositionEntityTicking(o.corePos) && !t.level.isPositionEntityTicking(neighbour(other[0], o.corePos))
                    && WorkerTravel.count(colony()) == 0 && vanillaForced(t).longStream().noneMatch(inside(r));
            }, x -> { }, () -> {
                Colony.Outpost o = colony().outposts.get(0);
                return "core chunk entity ticking " + t.level.isPositionEntityTicking(o.corePos) + ", next chunk entity ticking "
                    + t.level.isPositionEntityTicking(neighbour(other[0], o.corePos)) + ", travel tickets " + WorkerTravel.count(colony())
                    + ", vanilla forced in the strip " + vanillaForced(t).longStream().filter(inside(r)).count();
            });
    }

    private static void catchUp(TestM7.Run r) {
        SelfTest t = r.t();
        Item crushed = AutarkiaItems.CRUSHED_VANTITE.get();
        long[] before = new long[2];
        int[] est = new int[1];
        long[] expect = new long[2];
        t.then(x -> {
            Colony c = colony();
            Colony.Outpost o = c.outposts.get(0);
            OutpostCoreBlockEntity coreBe = core(c);
            o.profile.clear();
            o.profile.put(crushed, 0.02);
            o.profile.put(Items.COBBLESTONE, 0.05);
            o.profileTicks = 24000;
            double p = Math.min(1, coreBe == null ? 0 : coreBe.powerFactor());
            expect[0] = (long) Math.floor(0.02 * 12000 * 0.75 * p);
            expect[1] = (long) Math.floor(0.05 * 12000 * 0.75 * p);
            before[0] = count(x, o, crushed);
            before[1] = count(x, o, Items.COBBLESTONE);
            est[0] = c.deposits.stream().filter(d -> d.outpost().filter(o.id::equals).isPresent()).findFirst().map(Colony.Deposit::estCount).orElse(-1);
            x.command("autarkia outpost credit " + o.id.toString().substring(0, 8) + " 12000");
        });
        t.check("W15 catch-up: from 600 blocks away a 12,000-tick credit adds floor(r x 12000 x 0.75 x p) per output to the core-chunk depot (+-1)", () -> {
            Colony.Outpost o = colony().outposts.get(0);
            return Math.abs(count(t, o, crushed) - before[0] - expect[0]) <= 1 && Math.abs(count(t, o, Items.COBBLESTONE) - before[1] - expect[1]) <= 1;
        }, () -> {
            Colony.Outpost o = colony().outposts.get(0);
            return "crushed +" + (count(t, o, crushed) - before[0]) + " (expected " + expect[0] + "), cobblestone +" + (count(t, o, Items.COBBLESTONE)
                - before[1]) + " (expected " + expect[1] + ")";
        });
        t.check("W15 catch-up: the ore output lowers the deposit estimate by the same count", () -> colony().deposits.stream()
            .anyMatch(d -> d.estCount() == est[0] - expect[0]), () -> colony().deposits.toString());
    }

    private static long count(SelfTest t, Colony.Outpost o, Item item) {
        long n = 0;
        for (DepotBlockEntity d : Outposts.depots(t.level, o)) {
            n += d.stock().entrySet().stream().filter(e -> e.getKey().getItem() == item).mapToLong(java.util.Map.Entry::getValue).sum();
        }
        return n;
    }
}

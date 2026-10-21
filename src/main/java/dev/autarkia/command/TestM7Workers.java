package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.ColonyTiers;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Span;
import dev.autarkia.equipment.AutarkiaDamage;
import dev.autarkia.processing.OreOutcropBlock;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.worker.RechargeDockBlockEntity;
import dev.autarkia.worker.SentinelCombat;
import dev.autarkia.worker.WorkerBehavioursT4;
import dev.autarkia.worker.WorkerEntity;
import dev.autarkia.worker.WorkerMemories;
import dev.autarkia.worker.WorkerTravel;
import dev.autarkia.worker.WorkerType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * The T4 worker part of {@code /autarkia test m7} (06 #18 to #23) on the platform region x 50..130, z 180..240: a T4
 * colony (seed, bloom, three core upgrades) with docks and storage crates, then the Surveyor survey with deposits and
 * travel tickets (#18), the Cable-Layer pylon line with a second pylon trip (#19), its recharge and stuck rise (#20), the
 * Sentinel guard fight against 3 zombies and a creeper (#21), the Sentinel escort of a walking Porter (#22), a Porter
 * DELIVER into a barrel, and the Sentinel recharge and stuck rise (#23).
 */
final class TestM7Workers {
    private static final UUID[] ID = new UUID[1];
    private static final WorkerEntity[] SURVEYOR = new WorkerEntity[1];
    private static final WorkerEntity[] LAYER = new WorkerEntity[1];
    private static final WorkerEntity[] SENTINEL = new WorkerEntity[1];
    private static final WorkerEntity[] PORTER = new WorkerEntity[1];
    private static final int[] MAX_TICKETS = new int[1];

    private TestM7Workers() {
    }

    static void run(TestM7.Run r) {
        setup(r);
        survey(r);
        pylonLine(r);
        rechargeAndStuck(r, "06#20 Cable-Layer", LAYER, docks(r).get(1), r.at(112, 0, 186));
        r.t().check("06#20 the colony never held more travel tickets than maxTravelTickets", () -> MAX_TICKETS[0] <= AutarkiaConfig.MAX_TRAVEL_TICKETS.get(),
            () -> "max held " + MAX_TICKETS[0] + ", limit " + AutarkiaConfig.MAX_TRAVEL_TICKETS.get());
        guard(r);
        escort(r);
        deliver(r);
        rechargeAndStuck(r, "06#23 Sentinel", SENTINEL, docks(r).get(2), r.at(120, 0, 186));
    }

    private static @Nullable Colony colony() {
        return ID[0] == null ? null : Colonies.registry().get(ID[0]);
    }

    private static BlockPos core(TestM7.Run r) {
        return r.at(90, 0, 210);
    }

    private static List<BlockPos> docks(TestM7.Run r) {
        return List.of(r.at(80, 0, 200), r.at(82, 0, 200), r.at(84, 0, 200));
    }

    private static List<BlockPos> stores(TestM7.Run r) {
        return List.of(r.at(74, 0, 204), r.at(74, 0, 205), r.at(74, 0, 206), r.at(74, 0, 207));
    }

    /** Keep the docks full and record the most travel tickets the colony held at once. */
    private static void watch(SelfTest t, TestM7.Run r) {
        for (BlockPos d : docks(r)) {
            if (t.level.getBlockEntity(d) instanceof RechargeDockBlockEntity dock) {
                dock.buffer().fill(50000);
            }
        }
        if (colony() != null) {
            MAX_TICKETS[0] = Math.max(MAX_TICKETS[0], WorkerTravel.count(colony()));
        }
    }

    private static InteractionResult use(FakePlayer p, ItemStack stack, BlockPos clicked) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.snapTo(clicked.getX() + 0.5, clicked.getY() + 1, clicked.getZ() + 3.5, 180, 30);
        return stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(clicked).add(0, 0.5, 0), Direction.UP, clicked, false)));
    }

    private static WorkerEntity spawn(SelfTest t, EntityType<WorkerEntity> type, BlockPos at) {
        WorkerEntity w = type.create(t.level, EntitySpawnReason.COMMAND);
        w.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        t.level.addFreshEntity(w);
        return w;
    }

    private static <T extends Mob> T mob(SelfTest t, EntityType<T> type, BlockPos at) {
        T m = type.create(t.level, EntitySpawnReason.COMMAND);
        m.setNoAi(true);
        m.setPersistenceRequired();
        m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        t.level.addFreshEntity(m);
        return m;
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

    private static ColonyJob add(SelfTest t, ColonyJob j) {
        ColonyRuntime.of(colony()).jobs.add(j);
        return j;
    }

    private static void setup(TestM7.Run r) {
        SelfTest t = r.t();
        BlockPos core = core(r);
        t.check("06 data: Surveyor, Cable-Layer Drone and Sentinel register with their 06 stats, jobs, caps and HV for the Cable-Layer", () -> {
            for (String id : List.of("surveyor", "cable_layer_drone", "sentinel")) {
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(Autarkia.id(id))) {
                    return false;
                }
            }
            return WorkerType.SURVEYOR.capacity() == 60000 && WorkerType.CABLE_LAYER.capacity() == 200000 && WorkerType.SENTINEL.capacity() == 150000
                && WorkerType.CABLE_LAYER.maxCable() == CableTier.HV && WorkerType.CABLE_LAYER.stringCost() == 2000 && WorkerType.CABLE_LAYER.placeCost() == 5000
                && WorkerType.SURVEYOR.subCap(4) == 2 && WorkerType.CABLE_LAYER.subCap(4) == 2 && WorkerType.SENTINEL.subCap(4) == 4
                && WorkerType.SENTINEL.subCap(3) == 0 && WorkerType.SENTINEL.jobs().contains(ColonyJob.Type.ESCORT)
                && WorkerType.PORTER.jobs().contains(ColonyJob.Type.FOUND_OUTPOST) && WorkerType.HAULER_CRAB.jobs().contains(ColonyJob.Type.FOUND_OUTPOST);
        }, () -> "3 entity types, capacities 60,000 / 200,000 / 150,000, T4 sub-caps 2 / 2 / 4");
        t.then(x -> {
            for (Colony c : List.copyOf(Colonies.registry().all())) {
                if (c.owner.equals(TestM6Colony.owner(x).getUUID()) || c.dimension.equals(x.level.dimension())
                    && Math.max(Math.abs(c.corePos.getX() - core.getX()), Math.abs(c.corePos.getZ() - core.getZ())) <= 96) {
                    Colonies.dissolve(c);
                }
            }
            InteractionResult res = use(TestM6Colony.owner(x), new ItemStack(AutarkiaItems.CORE_SEED.get()), core.below());
            Colony c = Colonies.byCore(x.level, core);
            ID[0] = c == null ? null : c.id;
            if (c != null && x.player != null) {
                c.trusted.add(x.player.getUUID());
            }
            x.report("workers: the test seed grows a Core", res == InteractionResult.SUCCESS && c != null, res + ", owner leads "
                + Colonies.ownedBy(TestM6Colony.owner(x).getUUID()).size() + " colonies");
        });
        t.waitUntil("workers: the Core blooms", 300, () -> colony() != null && colony().state == Colony.State.ACTIVE, x -> { });
        t.then(x -> use(TestM6Colony.owner(x), new ItemStack(AutarkiaItems.CORE_UPGRADE_T2.get()), core));
        t.waitUntil("workers: the Core upgrades to T2", 160, () -> colony().tier == 2, x -> { });
        t.then(x -> use(TestM6Colony.owner(x), new ItemStack(AutarkiaItems.CORE_UPGRADE_T3.get()), core));
        t.waitUntil("workers: the Core upgrades to T3", 160, () -> colony().tier == 3, x -> { });
        t.then(x -> use(TestM6Colony.owner(x), new ItemStack(AutarkiaItems.CORE_UPGRADE_T4.get()), core));
        t.waitUntil("workers: core_upgrade_t4 raises the Core to T4 (domain half-size 48)", 160, () -> colony().tier == 4
            && ColonyTiers.domainHalf(colony().tier) == 48, x -> x.level.getEntitiesOfClass(ItemEntity.class, new AABB(core).inflate(8)).forEach(e -> e.discard()));
        t.then(x -> {
            dev.autarkia.colony.ColonyPlans.templates().keySet().forEach(k -> colony().template(k).enabled = false);
            for (BlockPos d : docks(r)) {
                TestM1.place(x, d, AutarkiaBlocks.RECHARGE_DOCK.get().defaultBlockState());
            }
            for (BlockPos s : stores(r)) {
                TestM1.place(x, s, AutarkiaBlocks.CRATE.get().defaultBlockState());
            }
            SURVEYOR[0] = spawn(x, AutarkiaEntities.SURVEYOR.get(), r.at(86, 3, 200));
            LAYER[0] = spawn(x, AutarkiaEntities.CABLE_LAYER_DRONE.get(), r.at(88, 3, 200));
            MAX_TICKETS[0] = 0;
        });
        t.waitUntil("workers: the Surveyor and the Cable-Layer join the T4 colony", 100, () -> colony().workers.containsKey(SURVEYOR[0].getUUID())
            && colony().workers.containsKey(LAYER[0].getUUID()), x -> watch(x, r));
        t.check("06 1.1 the T4 drones fly: no gravity, FLYING_SPEED 0.70 and 0.45, health 14 and 30", () -> SURVEYOR[0].isNoGravity()
            && SURVEYOR[0].getAttributeValue(Attributes.FLYING_SPEED) == 0.70 && LAYER[0].getAttributeValue(Attributes.FLYING_SPEED) == 0.45
            && SURVEYOR[0].getMaxHealth() == 14 && LAYER[0].getMaxHealth() == 30, () -> "gravity off " + SURVEYOR[0].isNoGravity());
    }

    private static void survey(TestM7.Run r) {
        SelfTest t = r.t();
        BlockPos outcrop = r.at(100, 0, 222);
        ColonyJob[] job = new ColonyJob[1];
        long[] scans = new long[1];
        boolean[] held = new boolean[1];
        int points = WorkerBehavioursT4.spiral(core(r), 64).size();
        t.then(x -> {
            TestM7.box(r, 96, -33, 218, 104, -2, 226, Blocks.STONE.defaultBlockState());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    TestM1.place(x, outcrop.offset(dx, -2, dz), Blocks.IRON_ORE.defaultBlockState());
                }
                TestM1.place(x, outcrop.offset(dx, -3, 0), Blocks.IRON_ORE.defaultBlockState());
            }
            TestM1.place(x, outcrop, AutarkiaBlocks.ORE_OUTCROP.get().defaultBlockState().setValue(OreOutcropBlock.ORE, OreOutcropBlock.Ore.IRON));
            colony().deposits.removeIf(d -> d.pos().equals(outcrop));
            scans[0] = colony().stats.getOrDefault("survey_scans", 0L);
            job[0] = add(x, new ColonyJob(ColonyJob.Type.SURVEY, null, core(r), null, null, 0, 50, 0, x.level.getGameTime(), null, null, 64));
        });
        t.waitUntil("06#18 the Surveyor flies the radius-64 spiral (" + points + " scan points) and the SURVEY job is done", 40000,
            () -> job[0].status == ColonyJob.Status.DONE && SURVEYOR[0].currentJob() == null, x -> {
                watch(x, r);
                held[0] |= WorkerTravel.holds(SURVEYOR[0]);
            }, () -> TestM6Colony.workerState(SURVEYOR[0]) + ", scans " + (colony().stats.getOrDefault("survey_scans", 0L) - scans[0]) + ", idle "
                + SURVEYOR[0].isIdle() + ", recharge " + SURVEYOR[0].getBrain().hasMemoryValue(WorkerMemories.NEEDS_RECHARGE.get()) + ", charger "
                + SURVEYOR[0].getBrain().getMemory(WorkerMemories.CHARGER_POS.get()).map(g -> g.pos().toShortString()).orElse("-") + ", reserve "
                + SURVEYOR[0].reserve() + ", job " + job[0].status + " x" + job[0].attempts + " backoff " + (job[0].backoffUntil - t.level.getGameTime())
                + (job[0].assignee == null ? "" : " to " + job[0].assignee.toString().substring(0, 4)));
        t.check("06#18 a scan ring went out at every spiral point", () -> colony().stats.getOrDefault("survey_scans", 0L) - scans[0] == points,
            () -> (colony().stats.getOrDefault("survey_scans", 0L) - scans[0]) + " scans of " + points);
        t.check("06#18 the iron outcrop is in the deposit registry with its 12 ore blocks", () -> colony().deposits.stream().anyMatch(d -> d.pos().equals(outcrop)
            && d.ore().equals("iron") && d.estCount() == 12), () -> colony().deposits.toString());
        t.waitUntil("06#18 the Surveyor held a travel ticket outside the force-loaded area and released it after the job (once it is back home)",
            1200, () -> held[0] && !WorkerTravel.holds(SURVEYOR[0]) && WorkerTravel.count(colony()) == 0, x -> watch(x, r),
            () -> "held " + held[0] + ", now " + WorkerTravel.count(colony()) + ", " + TestM6Colony.workerState(SURVEYOR[0]));
    }

    private static void pylonLine(TestM7.Run r) {
        SelfTest t = r.t();
        List<BlockPos> pylons = List.of(r.at(60, 0, 236), r.at(80, 0, 236), r.at(100, 0, 236), r.at(120, 0, 236));
        int cost = CableTier.HV.itemCost(20);
        int[] fetches = new int[1];
        int[] maxHeld = new int[1];
        WorkerEntity.State[] last = new WorkerEntity.State[1];
        Item pylon = AutarkiaItems.HV_PYLON.get();
        t.then(x -> {
            stock(x, r, new ItemStack(pylon, 4), new ItemStack(AutarkiaItems.HV_CABLE_SPOOL.get(), 3 * cost));
            BlockState st = AutarkiaBlocks.HV_PYLON.get().defaultBlockState().setValue(BaseMultiblock.CENTER, true)
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
            long now = x.level.getGameTime();
            for (BlockPos p : pylons) {
                add(x, new ColonyJob(ColonyJob.Type.PLACE, null, p, st, pylon, 1, 50, 3, now, null, null, 0));
            }
            last[0] = LAYER[0].state();
        });
        t.waitUntil("06#19 the Cable-Layer places 4 hv_pylons from storage", 12000, () -> pylons.stream().allMatch(p -> t.level.getBlockState(p)
            .is(AutarkiaBlocks.HV_PYLON.get())), x -> {
                watch(x, r);
                WorkerEntity w = LAYER[0];
                ColonyJob j = w.currentJob();
                if (w.state() == WorkerEntity.State.FETCHING && last[0] != WorkerEntity.State.FETCHING && j != null && j.type == ColonyJob.Type.PLACE) {
                    fetches[0]++;
                }
                last[0] = w.state();
                maxHeld[0] = Math.max(maxHeld[0], w.held(s -> s.is(pylon)));
            }, () -> TestM6Colony.workerState(LAYER[0]));
        t.then(x -> {
            long now = x.level.getGameTime();
            for (int i = 0; i + 1 < pylons.size(); i++) {
                add(x, new ColonyJob(ColonyJob.Type.STRING_CABLE, null, pylons.get(i), null, AutarkiaItems.HV_CABLE_SPOOL.get(), cost, 40, 4, now,
                    pylons.get(i + 1), CableTier.HV, 0));
            }
        });
        t.waitUntil("06#19 then it strings 3 HV spans along the line (the span jobs open once the pylons stand, as in a project)", 12000, () -> {
            for (int i = 0; i + 1 < pylons.size(); i++) {
                if (TestM1.spanBetween(t, pylons.get(i), pylons.get(i + 1)) == null) {
                    return false;
                }
            }
            return true;
        }, x -> {
            watch(x, r);
            WorkerEntity w = LAYER[0];
            ColonyJob j = w.currentJob();
            if (w.state() == WorkerEntity.State.FETCHING && last[0] != WorkerEntity.State.FETCHING && j != null && j.type == ColonyJob.Type.PLACE) {
                fetches[0]++;
            }
            last[0] = w.state();
            maxHeld[0] = Math.max(maxHeld[0], w.held(s -> s.is(pylon)));
        }, () -> TestM6Colony.workerState(LAYER[0]) + ", idle " + LAYER[0].isIdle() + ", recharge " + LAYER[0].getBrain()
            .hasMemoryValue(WorkerMemories.NEEDS_RECHARGE.get()) + ", jobs " + ColonyRuntime.of(colony()).jobs.all().stream()
            .map(j -> j.type + "@" + j.target.toShortString() + ":" + j.status + (j.assignee == null ? "" : ">" + j.assignee.toString().substring(0, 4))
                + (j.attempts > 0 ? " x" + j.attempts : "")).toList());
        t.check("06#19 the spans are HV", () -> {
            for (int i = 0; i + 1 < pylons.size(); i++) {
                Span s = TestM1.spanBetween(t, pylons.get(i), pylons.get(i + 1));
                if (s == null || s.tier != CableTier.HV) {
                    return false;
                }
            }
            return true;
        }, () -> "3 spans of 20 blocks");
        t.check("06#19 one pylon per cargo slot: at most 3 carried, so it went back to storage for the 4th", () -> maxHeld[0] <= 3 && fetches[0] >= 2,
            () -> "max carried " + maxHeld[0] + ", pylon fetch trips " + fetches[0]);
    }

    /** 06 tests 20 and 23 (as test 12): the worker charges at the dock at the config rate, and a roofed hole makes it rise at stuck level 2. */
    private static void rechargeAndStuck(TestM7.Run r, String label, WorkerEntity[] w, BlockPos dock, BlockPos hole) {
        SelfTest t = r.t();
        int[] rate = new int[2];
        double[] y = new double[2];
        int[] n = new int[1];
        t.waitUntil(label + " is idle", 2400, () -> w[0].isIdle(), x -> watch(x, r));
        t.then(x -> {
            w[0].getBrain().setMemory(WorkerMemories.CHARGER_POS.get(), net.minecraft.core.GlobalPos.of(x.level.dimension(), dock));
            w[0].setEnergy(w[0].reserve() - 50);
        });
        t.waitUntil(label + " goes to the recharge dock and charges", 1200, () -> w[0].state() == WorkerEntity.State.RECHARGING
            && w[0].action() == WorkerEntity.ACTION_RECHARGE, x -> watch(x, r), () -> TestM6Colony.workerState(w[0]));
        int[] docked = new int[1];
        t.then(x -> {
            rate[0] = w[0].energy();
            docked[0] = 0;
        });
        t.repeat(20, x -> {
            watch(x, r);
            docked[0] += w[0].action() == WorkerEntity.ACTION_RECHARGE ? 1 : 0;
        });
        t.then(x -> rate[1] = w[0].energy() - rate[0]);
        t.check(label + " charges at the config rate (2,000 FE/t; 19 to 21 dock ticks fall in the 20-tick window)", () -> rate[1] >= 19 * 2000
            && rate[1] <= 21 * 2000 || w[0].energy() == w[0].workerType.capacity(), () -> rate[1] + " FE in 20 ticks, docked " + docked[0]
            + " ticks, " + TestM6Colony.workerState(w[0]));
        t.waitUntil(label + " is full and idle again", 6000, () -> w[0].isIdle() && w[0].energy() >= w[0].workerType.capacity() - 2000, x -> watch(x, r));
        t.then(x -> {
            n[0] = (int) Math.ceil(w[0].getBbWidth());
            for (int dx = -1; dx <= n[0]; dx++) {
                for (int dz = -1; dz <= n[0]; dz++) {
                    for (int dy = 0; dy <= 3; dy++) {
                        boolean inside = dx >= 0 && dz >= 0 && dx < n[0] && dz < n[0];
                        if (!inside || dy == 3) {
                            TestM1.place(x, hole.offset(dx, dy, dz), Blocks.STONE.defaultBlockState());
                        }
                    }
                }
            }
            w[0].snapTo(hole.getX() + n[0] / 2.0, hole.getY(), hole.getZ() + n[0] / 2.0, 0, 0);
            y[0] = w[0].getY();
            y[1] = y[0];
        });
        t.waitUntil(label + " in a roofed hole reaches stuck level 2 and rises", 400, () -> w[0].getBrain()
            .getMemory(WorkerMemories.STUCK_LEVEL.get()).orElse(0) >= 2 && y[1] > y[0] + 0.2, x -> {
                watch(x, r);
                dev.autarkia.worker.WorkerBrain.walkTo(w[0], hole.offset(12, 0, 0), 1);
                y[1] = Math.max(y[1], w[0].getY());
            }, () -> TestM6Colony.workerState(w[0]) + String.format(java.util.Locale.ROOT, " max rise %.2f", y[1] - y[0]));
        t.then(x -> {
            for (BlockPos p : BlockPos.betweenClosed(hole.offset(-1, 0, -1), hole.offset(n[0], 3, n[0]))) {
                x.level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            w[0].resetStuck();
        });
        t.waitUntil(label + " recovers once the hole is open", 600, () -> !w[0].getBrain().hasMemoryValue(WorkerMemories.STUCK_LEVEL.get()), x -> {
            watch(x, r);
            w[0].recover();
        });
    }

    private static void guard(TestM7.Run r) {
        SelfTest t = r.t();
        List<Mob> hostiles = new ArrayList<>();
        long[] died = new long[4];
        boolean[] byArc = new boolean[4];
        boolean[] wrong = new boolean[1];
        LivingEntity[] bystanders = new LivingEntity[2];
        float[] health = new float[2];
        t.then(x -> SENTINEL[0] = spawn(x, AutarkiaEntities.SENTINEL.get(), r.at(70, 3, 196)));
        t.waitUntil("06#21 the Sentinel joins the colony", 100, () -> colony().workers.containsKey(SENTINEL[0].getUUID()), x -> watch(x, r));
        t.then(x -> {
            hostiles.clear();
            java.util.Arrays.fill(died, -1);
            Villager v = mob(x, EntityTypes.VILLAGER, r.at(64, 0, 200));
            Wolf wolf = mob(x, EntityTypes.WOLF, r.at(66, 0, 200));
            wolf.setTame(true, true);
            wolf.setOwner(TestM6Colony.owner(x));
            bystanders[0] = v;
            bystanders[1] = wolf;
            health[0] = v.getHealth();
            health[1] = wolf.getHealth();
            for (BlockPos p : List.of(r.at(66, 0, 188), r.at(68, 0, 190), r.at(64, 0, 190))) {
                Mob z = mob(x, EntityTypes.ZOMBIE, p);
                z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                hostiles.add(z);
            }
            hostiles.add(mob(x, EntityTypes.CREEPER, r.at(60, 0, 186)));
        });
        t.waitUntil("06#21 the Sentinel kills the 3 zombies and the creeper", 3000, () -> hostiles.stream().noneMatch(LivingEntity::isAlive), x -> {
            watch(x, r);
            for (int i = 0; i < hostiles.size(); i++) {
                Mob m = hostiles.get(i);
                if (!m.isAlive() && died[i] < 0) {
                    died[i] = x.level.getGameTime();
                    byArc[i] = m.getLastDamageSource() != null && m.getLastDamageSource().is(AutarkiaDamage.ARC);
                }
            }
            LivingEntity target = SENTINEL[0].getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
            wrong[0] |= target != null && !hostiles.contains(target);
        }, () -> TestM6Colony.workerState(SENTINEL[0]) + ", alive " + hostiles.stream().filter(LivingEntity::isAlive).count());
        t.check("06#21 every hostile died by arcs (damage type autarkia:arc)", () -> byArc[0] && byArc[1] && byArc[2] && byArc[3],
            () -> java.util.Arrays.toString(byArc));
        t.check("06#21 the creeper died first", () -> died[3] >= 0 && died[3] < Math.min(died[0], Math.min(died[1], died[2])),
            () -> java.util.Arrays.toString(died));
        t.check("06#21 no player, villager, tamed wolf or worker was targeted; the villager and the wolf are unhurt", () -> !wrong[0]
            && bystanders[0].isAlive() && bystanders[0].getHealth() >= health[0] && bystanders[1].isAlive()
            && bystanders[1].getHealth() >= health[1] && !SentinelCombat.canTarget(SENTINEL[0], TestM6Colony.owner(t))
            && !SentinelCombat.canTarget(SENTINEL[0], bystanders[0]) && !SentinelCombat.canTarget(SENTINEL[0], bystanders[1])
            && !SentinelCombat.canTarget(SENTINEL[0], SURVEYOR[0]), () -> "wrong target " + wrong[0]);
        t.then(x -> {
            bystanders[0].discard();
            bystanders[1].discard();
        });
    }

    private static void escort(TestM7.Run r) {
        SelfTest t = r.t();
        BlockPos site = r.at(126, 0, 236);
        ColonyJob[] found = new ColonyJob[1];
        ColonyJob[] guard = new ColonyJob[1];
        int[] samples = new int[2];
        double[] range = {Double.MAX_VALUE, 0};
        boolean[] caught = new boolean[1];
        t.then(x -> {
            PORTER[0] = spawn(x, AutarkiaEntities.PORTER.get(), r.at(88, 0, 214));
            stock(x, r, new ItemStack(AutarkiaItems.OUTPOST_SEED.get(), 1));
        });
        t.waitUntil("06#22 a Porter joins the colony", 100, () -> colony().workers.containsKey(PORTER[0].getUUID()), x -> watch(x, r));
        t.then(x -> found[0] = add(x, new ColonyJob(ColonyJob.Type.FOUND_OUTPOST, null, site, null, AutarkiaItems.OUTPOST_SEED.get(), 1, 50, 0,
            x.level.getGameTime(), null, null, 0)));
        t.waitUntil("06#22 the Porter takes the FOUND_OUTPOST job", 600, () -> PORTER[0].currentJob() == found[0], x -> watch(x, r));
        t.then(x -> {
            guard[0] = ColonyRuntime.of(colony()).jobs.all().stream().filter(j -> j.type == ColonyJob.Type.ESCORT && PORTER[0].getUUID().equals(j.subject))
                .findFirst().orElse(null);
            x.report("06#22 the board opens an ESCORT job for the Porter carrying the seed (subject = the Porter)", guard[0] != null,
                ColonyRuntime.of(colony()).jobs.all().stream().map(j -> j.type.name()).toList().toString());
            if (guard[0] == null) {
                guard[0] = new ColonyJob(ColonyJob.Type.ESCORT, null, site.above(), null, null, 0, 60, 0, x.level.getGameTime(), null, null, 0);
            }
        });
        t.waitUntil("06#22 the Sentinel takes the ESCORT job", 600, () -> SENTINEL[0].currentJob() == guard[0], x -> watch(x, r),
            () -> TestM6Colony.workerState(SENTINEL[0]) + ", idle " + SENTINEL[0].isIdle() + ", recharge " + SENTINEL[0].getBrain()
                .hasMemoryValue(WorkerMemories.NEEDS_RECHARGE.get()) + ", target " + SENTINEL[0].getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                + ", escort " + guard[0].status + (guard[0].assignee == null ? "" : " to " + guard[0].assignee.toString().substring(0, 4))
                + ", accepts " + SENTINEL[0].accepts(guard[0]) + ", charge " + SENTINEL[0].charge());
        t.waitUntil("06#22 the Porter walks out to the far site while the Sentinel escorts it", 4000, () -> PORTER[0].distanceToSqr(Vec3.atCenterOf(site)) <= 20
            || guard[0].status == ColonyJob.Status.DONE, x -> {
                watch(x, r);
                if (PORTER[0].state() == WorkerEntity.State.TRAVELLING && SENTINEL[0].currentJob() == guard[0]) {
                    double d = SENTINEL[0].distanceTo(PORTER[0]);
                    caught[0] |= d <= 8;
                    if (caught[0]) {
                        samples[0]++;
                        samples[1] += d >= 4 && d <= 8 ? 1 : 0;
                        range[0] = Math.min(range[0], d);
                        range[1] = Math.max(range[1], d);
                    }
                }
            }, () -> TestM6Colony.workerState(PORTER[0]) + " | " + TestM6Colony.workerState(SENTINEL[0]));
        t.check("06#22 while the Porter walked, the Sentinel stayed 4 to 8 blocks from it (90% of the samples, never beyond 12)", () -> samples[0] >= 40
            && samples[1] >= 0.9 * samples[0] && range[1] <= 12, () -> String.format(java.util.Locale.ROOT, "%d of %d samples in 4..8, range %.2f..%.2f",
            samples[1], samples[0], range[0], range[1]));
        t.then(x -> {
            ColonyRuntime.of(colony()).jobs.cancel(colony(), x.level, found[0].id);
            ColonyRuntime.of(colony()).jobs.cancel(colony(), x.level, guard[0].id);
        });
    }

    private static void deliver(TestM7.Run r) {
        SelfTest t = r.t();
        BlockPos barrel = r.at(70, 0, 230);
        Item servo = AutarkiaItems.SERVO.get();
        t.then(x -> {
            TestM1.place(x, barrel, Blocks.BARREL.defaultBlockState());
            stock(x, r, new ItemStack(servo, 4));
            add(x, new ColonyJob(ColonyJob.Type.DELIVER, null, barrel, null, servo, 4, 50, 0, x.level.getGameTime(), null, null, 0));
        });
        t.waitUntil("DELIVER: a worker brings 4 servos from storage into the barrel's item handler", 2400, () -> {
            ResourceHandler<ItemResource> h = t.level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, barrel, null);
            int n = 0;
            for (int i = 0; h != null && i < h.size(); i++) {
                n += h.getResource(i).toStack(1).is(servo) ? h.getAmountAsInt(i) : 0;
            }
            return n == 4;
        }, x -> watch(x, r), () -> TestM6Colony.workerState(PORTER[0]));
    }
}

package dev.autarkia.command;

import com.mojang.authlib.GameProfile;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyClock;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.Pattern;
import dev.autarkia.colony.PatternLoomBlockEntity;
import dev.autarkia.colony.PatternRepair;
import dev.autarkia.colony.Project;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.worker.RechargeDockBlockEntity;
import dev.autarkia.worker.WorkerEntity;
import dev.autarkia.worker.WorkerMemories;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * The colony part of {@code /autarkia test m6}: a T3 colony with 2 Builder Drones and a Lineman, then W10 / 06 #10
 * (a creeper blast in a watched Pattern line is repaired bottom row first), W10 accept (an owner break is not
 * repaired), 06 #11 (no items: FAILED item_missing), 06 #12 and #17 (recharge dock and stuck rise), W13 / 06 #15 (a
 * snapped and restrung copper line gets a fuse box from failure memory), 06 #16 (climb, string, climb down), W12 (a
 * rack aisle collapses like dominoes and is rebuilt with steel racks), W11 (a busy crusher line is duplicated) and
 * W13 decay.
 */
final class TestM6Colony {
    private static final UUID[] ID = new UUID[1];
    private static final WorkerEntity[] DRONES = new WorkerEntity[2];
    private static final WorkerEntity[] LINEMAN = new WorkerEntity[1];

    private TestM6Colony() {
    }

    static @Nullable Colony colony() {
        return ID[0] == null ? null : Colonies.registry().get(ID[0]);
    }

    static FakePlayer owner(SelfTest t) {
        return FakePlayerFactory.get(t.level, new GameProfile(TestM3.OWNER, "AutarkiaTest"));
    }

    private static InteractionResult use(FakePlayer p, ItemStack stack, BlockPos clicked) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.snapTo(clicked.getX() + 0.5, clicked.getY() + 1, clicked.getZ() + 3.5, 180, 30);
        return stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(clicked).add(0, 0.5, 0), Direction.UP, clicked, false)));
    }

    static BlockPos core(TestM6.Run r) {
        return r.at(60, 0, 110);
    }

    private static List<BlockPos> docks(TestM6.Run r) {
        return List.of(r.at(54, 0, 104), r.at(56, 0, 104), r.at(58, 0, 104));
    }

    private static List<BlockPos> stores(TestM6.Run r) {
        return List.of(r.at(52, 0, 112), r.at(52, 0, 113), r.at(52, 0, 114), r.at(52, 0, 115));
    }

    /** Keep the docks full (the colony's charging power). */
    static void powerDocks(SelfTest t, TestM6.Run r) {
        for (BlockPos d : docks(r)) {
            if (t.level.getBlockEntity(d) instanceof RechargeDockBlockEntity dock) {
                dock.buffer().fill(50000);
            }
        }
    }

    static void setup(TestM6.Run r) {
        SelfTest t = r.t();
        BlockPos core = core(r);
        t.then(x -> {
            for (Colony c : List.copyOf(Colonies.registry().all())) {
                if (c.owner.equals(TestM3.OWNER) || c.dimension.equals(x.level.dimension()) && c.corePos.distManhattan(core) < 300) {
                    Colonies.dissolve(c);
                }
            }
            InteractionResult res = use(owner(x), new ItemStack(AutarkiaItems.CORE_SEED.get()), core.below());
            Colony c = Colonies.byCore(x.level, core);
            ID[0] = c == null ? null : c.id;
            if (c != null && x.player != null) {
                c.trusted.add(x.player.getUUID());
            }
            x.report("colony: the test seed grows a Core", res == InteractionResult.SUCCESS && c != null, res.toString());
        });
        t.waitUntil("colony: the Core blooms", 300, () -> colony() != null && colony().state == Colony.State.ACTIVE, x -> { });
        t.then(x -> use(owner(x), new ItemStack(AutarkiaItems.CORE_UPGRADE_T2.get()), core));
        t.waitUntil("colony: the Core upgrades to T2", 160, () -> colony().tier == 2, x -> { });
        t.then(x -> use(owner(x), new ItemStack(AutarkiaItems.CORE_UPGRADE_T3.get()), core));
        t.waitUntil("colony: core_upgrade_t3 raises the Core to T3 (domain half-size 32)", 160, () -> colony().tier == 3
            && dev.autarkia.colony.ColonyTiers.domainHalf(colony().tier) == 32, x -> x.level.getEntitiesOfClass(ItemEntity.class,
            new AABB(core).inflate(8)).forEach(e -> e.discard()));
        t.then(x -> {
            dev.autarkia.colony.ColonyPlans.templates().keySet().forEach(k -> colony().template(k).enabled = k.equals("pattern_repair"));
            for (BlockPos d : docks(r)) {
                TestM1.place(x, d, AutarkiaBlocks.RECHARGE_DOCK.get().defaultBlockState());
            }
            for (BlockPos s : stores(r)) {
                TestM1.place(x, s, AutarkiaBlocks.CRATE.get().defaultBlockState());
            }
            for (int i = 0; i < 2; i++) {
                DRONES[i] = spawn(x, AutarkiaEntities.BUILDER_DRONE.get(), r.at(56 + 2 * i, 2, 100));
            }
            LINEMAN[0] = spawn(x, AutarkiaEntities.LINEMAN.get(), r.at(62, 0, 100));
        });
        t.waitUntil("colony: 2 Builder Drones and a Lineman join (T3 sub-caps 4 and 2)", 100, () -> colony().workers.containsKey(DRONES[0].getUUID())
            && colony().workers.containsKey(DRONES[1].getUUID()) && colony().workers.containsKey(LINEMAN[0].getUUID()), x -> powerDocks(x, r));
        t.check("06 1.1 the drone flies: no gravity, FLYING_SPEED 0.5, no fall damage", () -> DRONES[0].isNoGravity()
            && DRONES[0].getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED) == 0.5
            && !DRONES[0].causeFallDamage(10, 1, t.level.damageSources().fall()), () -> "gravity off " + DRONES[0].isNoGravity());
    }

    private static WorkerEntity spawn(SelfTest t, net.minecraft.world.entity.EntityType<WorkerEntity> type, BlockPos at) {
        WorkerEntity w = type.create(t.level, EntitySpawnReason.COMMAND);
        w.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        t.level.addFreshEntity(w);
        return w;
    }

    /** Put stacks into the colony storage crates. */
    static void stock(SelfTest t, TestM6.Run r, ItemStack... stacks) {
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

    private static void emptyStock(SelfTest t, TestM6.Run r) {
        for (BlockPos p : stores(r)) {
            if (t.level.getBlockEntity(p) instanceof dev.autarkia.storage.CrateBlockEntity c) {
                for (int i = 0; i < c.items().size(); i++) {
                    c.items().set(i, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
                }
            }
        }
    }

    static void run(TestM6.Run r) {
        UUID[] instance = new UUID[1];
        repair(r, instance);
        accept(r, instance);
        noItems(r);
        rechargeAndStuck(r, "06#12 Builder Drone", DRONES, docks(r).get(0), r.at(96 - 8, 0, 84));
        fuse(r);
        climb(r);
        rechargeAndStuck(r, "06#17 Lineman", LINEMAN, docks(r).get(2), r.at(84, 0, 90));
        TestM6Aisle.run(r);
        TestM6Line.run(r);
        decay(r);
    }

    /** Set a loom's capture box by its menu buttons, put a blank card in and record. */
    static String capture(SelfTest t, BlockPos loom, BlockPos min, net.minecraft.core.Vec3i size, String name) {
        PatternLoomBlockEntity be = (PatternLoomBlockEntity) t.level.getBlockEntity(loom);
        BlockPos rel = min.subtract(loom);
        int[] want = {rel.getX(), rel.getY(), rel.getZ(), size.getX(), size.getY(), size.getZ()};
        for (int axis = 0; axis < 6; axis++) {
            for (int guard = 0; guard < 64 && be.data().get(axis) != want[axis]; guard++) {
                be.button(be.data().get(axis) > want[axis] ? axis : axis + 6);
            }
        }
        be.card().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(new ItemStack(AutarkiaItems.PATTERN_CARD.get())), 1);
        return be.record(owner(t), name);
    }

    private static List<BlockPos> lineCells(TestM6.Run r) {
        List<BlockPos> out = new ArrayList<>();
        for (int dx = 1; dx <= 5; dx++) {
            out.add(r.at(70 + dx, 0, 90));
            out.add(r.at(70 + dx, 1, 90));
        }
        return out;
    }

    private static void repair(TestM6.Run r, UUID[] instance) {
        SelfTest t = r.t();
        BlockPos loom = r.at(73, 0, 96);
        BlockPos poleA = r.at(70, 0, 92);
        BlockPos poleB = r.at(76, 0, 92);
        BlockPos press = r.at(71, 0, 92);
        Pattern.Ref[] ref = new Pattern.Ref[1];
        long[][] placedAt = new long[2][1];
        t.then(x -> {
            TestM4.pole(x, poleA);
            TestM4.pole(x, poleB);
            Grid.addSpan(x.level, poleA, poleB, CableTier.COPPER);
            TestM1.place(x, press, AutarkiaBlocks.PLATE_PRESS.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
            for (int dx = 1; dx <= 5; dx++) {
                TestM1.place(x, r.at(70 + dx, 0, 90), Blocks.OAK_PLANKS.defaultBlockState());
                TestM1.place(x, r.at(70 + dx, 1, 90), Blocks.GLASS.defaultBlockState());
            }
            TestM1.place(x, loom, AutarkiaBlocks.PATTERN_LOOM.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
            String err = capture(x, loom, r.at(70, 0, 90), new net.minecraft.core.Vec3i(7, 4, 3), "Press Line");
            x.report("W10 rig: a press line between two copper poles is recorded at the loom", err == null, String.valueOf(err));
        });
        t.waitUntil("W10 the capture finishes: a watched instance and a library entry", 60, () -> {
            ref[0] = ((PatternLoomBlockEntity) t.level.getBlockEntity(loom)).cardStack().get(AutarkiaComponents.PATTERN_REF.get());
            return ref[0] != null && colony().instances.stream().anyMatch(in -> in.pattern().equals(ref[0].id()) && in.watched());
        }, x -> { });
        t.then(x -> {
            instance[0] = colony().instances.stream().filter(in -> in.pattern().equals(ref[0].id())).findFirst().orElseThrow().id();
            stock(x, r, new ItemStack(Blocks.OAK_PLANKS, 16), new ItemStack(Blocks.GLASS, 16), new ItemStack(AutarkiaItems.WOODEN_POLE.get(), 2),
                new ItemStack(AutarkiaItems.PLATE_PRESS.get(), 1), new ItemStack(AutarkiaItems.COPPER_WIRE_SPOOL.get(), 4));
            x.level.explode(null, r.at(73, 0, 91).getX() + 0.5, r.at(73, 0, 91).getY() + 0.5, r.at(73, 0, 91).getZ() + 0.5, 3.0f,
                Level.ExplosionInteraction.MOB);
            x.level.getEntitiesOfClass(ItemEntity.class, new AABB(r.at(73, 0, 91)).inflate(10)).forEach(e -> e.discard());
        });
        t.waitUntil("W10 within 40 ticks a pattern_repair project appears and starts at once", 40, () -> repairProject(t, instance[0]) != null
            && repairProject(t, instance[0]).state.active(), x -> powerDocks(x, r));
        t.check("W10 the blast left missing cells and no conflicts", () -> !repairProject(t, instance[0]).cells.isEmpty()
            && PatternRepair.conflicts(colony(), instance[0]) == 0, () -> repairProject(t, instance[0]).cells.size() + " cells");
        t.waitUntil("W10 the drones put every block back, string the span again, and the press stands", 6000, () -> {
            Project p = repairProject(t, instance[0]);
            return p != null && p.state == Project.State.DONE;
        }, x -> {
            powerDocks(x, r);
            for (BlockPos c : lineCells(r)) {
                int lvl = c.getY() - r.base().getY();
                if (!x.level.getBlockState(c).isAir()) {
                    placedAt[lvl][0] = Math.max(placedAt[lvl][0], x.level.getGameTime());
                }
            }
        });
        t.check("W10 the line is whole again: planks, glass, poles, press and the copper span", () -> lineCells(r).stream().noneMatch(c -> t.level.getBlockState(c)
            .isAir()) && t.level.getBlockState(press).is(AutarkiaBlocks.PLATE_PRESS.get()) && TestM1.spanBetween(t, poleA, poleB) != null,
            () -> "span " + (TestM1.spanBetween(t, poleA, poleB) != null));
        t.check("W10 bottom row first: the last plank went in no later than the last glass", () -> placedAt[0][0] <= placedAt[1][0],
            () -> "planks " + placedAt[0][0] + ", glass " + placedAt[1][0]);
    }

    private static @Nullable Project repairProject(SelfTest t, UUID instance) {
        var plan = PatternRepair.planId(instance);
        return colony().projects.stream().filter(p -> p.plan.equals(plan)).reduce((a, b) -> b).orElse(null);
    }

    private static void accept(TestM6.Run r, UUID[] instance) {
        SelfTest t = r.t();
        BlockPos glass = r.at(73, 1, 90);
        int[] before = new int[1];
        t.then(x -> {
            before[0] = (int) colony().projects.stream().filter(p -> p.template.getPath().equals("pattern_repair")).count();
            FakePlayer o = owner(x);
            o.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            o.gameMode.destroyBlock(glass);
            ColonyRuntime.of(colony()).repairScanAt = x.level.getGameTime() + 1;
        });
        t.waitTicks(60);
        t.check("W10 accept: the owner's break is an accepted removal, no repair job for that cell", () -> t.level.getBlockState(glass).isAir()
            && colony().projects.stream().filter(p -> p.template.getPath().equals("pattern_repair")).count() == before[0]
            && ColonyRuntime.of(colony()).patternMissing.getOrDefault(instance[0], -1) == 0, () -> "missing " + ColonyRuntime.of(colony()).patternMissing);
    }

    private static void noItems(TestM6.Run r) {
        SelfTest t = r.t();
        BlockPos plank = r.at(72, 0, 90);
        boolean[] failed = new boolean[1];
        t.then(x -> {
            emptyStock(x, r);
            stock(x, r, new ItemStack(Blocks.OAK_PLANKS, 1));
            FakePlayer stranger = FakePlayerFactory.get(x.level, new GameProfile(UUID.nameUUIDFromBytes("m6stranger".getBytes()), "Stranger"));
            stranger.gameMode.destroyBlock(plank);
            x.level.getEntitiesOfClass(ItemEntity.class, new AABB(plank).inflate(4)).forEach(e -> e.discard());
        });
        t.waitUntil("06#11 the repair of the stranger's break starts with the plank reserved", 200, () -> colony().projects.stream().anyMatch(p ->
            p.template.getPath().equals("pattern_repair") && p.state == Project.State.BUILDING), x -> powerDocks(x, r));
        t.then(x -> emptyStock(x, r));
        t.waitUntil("06#11 with empty storage the step fails and the drone shows FAILED item_missing (Jade reason)", 1200, () -> failed[0], x -> {
            powerDocks(x, r);
            for (WorkerEntity d : DRONES) {
                failed[0] |= d.stateName().equals("failed:item_missing");
            }
        });
        t.then(x -> {
            for (Project p : colony().projects) {
                if (p.template.getPath().equals("pattern_repair") && p.state.open()) {
                    dev.autarkia.colony.Planner.cancel(colony(), p);
                }
            }
            TestM1.place(x, plank, Blocks.OAK_PLANKS.defaultBlockState());
        });
    }

    /** 06 tests 12, 14 and 17: the worker charges at the dock at the config rate, and a roofed 1x1 hole makes it rise or jump at stuck level 2. */
    static void rechargeAndStuck(TestM6.Run r, String label, WorkerEntity[] w, BlockPos dock, BlockPos hole) {
        SelfTest t = r.t();
        int[] rate = new int[2];
        double[] y = new double[2];
        t.waitUntil(label + " is idle", 2400, () -> w[0].isIdle(), x -> powerDocks(x, r));
        t.then(x -> {
            w[0].getBrain().setMemory(WorkerMemories.CHARGER_POS.get(), net.minecraft.core.GlobalPos.of(x.level.dimension(), dock));
            w[0].setEnergy(w[0].reserve() - 50);
        });
        t.waitUntil(label + " goes to the recharge dock and charges", 1200, () -> w[0].state() == WorkerEntity.State.RECHARGING
            && w[0].action() == WorkerEntity.ACTION_RECHARGE, x -> powerDocks(x, r), () -> workerState(w[0]));
        t.then(x -> rate[0] = w[0].energy());
        t.repeat(20, x -> powerDocks(x, r));
        t.then(x -> rate[1] = w[0].energy() - rate[0]);
        t.check(label + " charges at the config rate (2,000 FE/t; 19 to 21 dock ticks fall in the 20-tick window)", () -> rate[1] >= 19 * 2000
            && rate[1] <= 21 * 2000 || w[0].energy() == w[0].workerType.capacity(), () -> rate[1] + " FE in 20 ticks");
        t.waitUntil(label + " is full and idle again", 2400, () -> w[0].isIdle() && w[0].energy() >= w[0].workerType.capacity() - 2000, x -> powerDocks(x, r));
        int[] n = new int[1];
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
        t.waitUntil(label + " in a roofed 1x1 hole reaches stuck level 2 and tries to get out upward", 400, () -> w[0].getBrain()
            .getMemory(WorkerMemories.STUCK_LEVEL.get()).orElse(0) >= 2 && y[1] > y[0] + 0.2, x -> {
                powerDocks(x, r);
                dev.autarkia.worker.WorkerBrain.walkTo(w[0], hole.offset(12, 0, 0), 1);
                y[1] = Math.max(y[1], w[0].getY());
            }, () -> workerState(w[0]) + String.format(java.util.Locale.ROOT, " max rise %.2f", y[1] - y[0]));
        t.then(x -> {
            for (BlockPos p : BlockPos.betweenClosed(hole.offset(-1, 0, -1), hole.offset(n[0], 3, n[0]))) {
                x.level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            w[0].resetStuck();
        });
        t.waitUntil(label + " recovers once the hole is open", 600, () -> !w[0].getBrain().hasMemoryValue(WorkerMemories.STUCK_LEVEL.get()), x -> {
            powerDocks(x, r);
            w[0].recover();
        });
    }

    private static void fuse(TestM6.Run r) {
        SelfTest t = r.t();
        BlockPos g = r.at(36, 0, 120);
        BlockPos l = r.at(52, 0, 120);
        BlockPos crank = r.at(36, 0, 121);
        boolean[] cut = new boolean[1];
        t.then(x -> {
            TestM4.pole(x, g);
            TestM4.pole(x, l);
            TestM1.place(x, crank, AutarkiaBlocks.HAND_CRANK.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.SOUTH));
            List.of(r.at(53, 0, 120), r.at(52, 0, 121), r.at(52, 0, 119)).forEach(b -> TestM4.bank(x, b));
            Grid.addSpan(x.level, g, l, CableTier.COPPER);
            colony().memories.removeIf(m -> m.type().equals("cable_snap"));
            colony().autonomy = Colony.Autonomy.ASSISTED;
            colony().templates.forEach((k, v) -> v.enabled = k.equals("fuse_box"));
        });
        t.waitUntil("W13 a copper span overloaded at 512 FE/t snaps", 400, () -> TestM1.spanBetween(t, g, l) == null, x -> TestM1.pushEnergy(x, g, 512));
        t.check("W13 the colony remembers cable_snap with weight 1", () -> colony().memories.stream().anyMatch(m -> m.type().equals("cable_snap")
            && m.weightAt(colony().clock, 168000) > 0.99), () -> colony().memories.toString());
        t.then(x -> {
            Grid.addSpan(x.level, g, l, CableTier.COPPER);
            x.level.getEntitiesOfClass(dev.autarkia.energy.SnappedCableEntity.class, new AABB(g).inflate(24)).forEach(e -> e.discard());
            x.level.getEntitiesOfClass(ItemEntity.class, new AABB(g).inflate(24)).forEach(e -> e.discard());
            emptyStock(x, r);
            stock(x, r, new ItemStack(AutarkiaItems.FUSE_BOX.get(), 1), new ItemStack(AutarkiaItems.COPPER_WIRE_SPOOL.get(), 4));
        });
        t.waitUntil("W13 the planner proposes a fuse_box (B 85) on the source side of the line", 1200, () -> fuseProject() != null, x -> powerDocks(x, r));
        t.check("W13 the fuse goes near the generator end, with a cut of the old span and 2 new spans", () -> {
            Project p = fuseProject();
            BlockPos spot = BlockPos.of(p.cells.get(0).pos());
            return p.cutSpans.size() == 1 && p.spans.size() == 2 && spot.distSqr(g) < spot.distSqr(l) && p.fromMemory;
        }, () -> fuseProject().origin.toShortString());
        t.then(x -> {
            ColonyClock.skip(colony(), 24000);
            ColonyClock.skip(colony(), 24000);
        });
        t.waitUntil("W13 after one colony day Assisted approves it and the Lineman cuts the span", 3000, () -> cut[0], x -> {
            powerDocks(x, r);
            cut[0] |= LINEMAN[0].currentJob() != null && LINEMAN[0].currentJob().type == ColonyJob.Type.CUT_SPAN;
        });
        t.waitUntil("W13 the fuse box stands inline with 2 spans: project done, corrective from memory", 6000, () -> fuseProject().state == Project.State.DONE,
            x -> powerDocks(x, r), () -> projectState(t, fuseProject()));
        t.check("W13 the line now runs pole - fuse box - pole", () -> {
            BlockPos spot = BlockPos.of(fuseProject().cells.get(0).pos());
            return t.level.getBlockState(spot).is(AutarkiaBlocks.FUSE_BOX.get()) && TestM1.spanBetween(t, g, spot) != null && TestM1.spanBetween(t, spot, l) != null
                && TestM1.spanBetween(t, g, l) == null;
        }, () -> "fuse at " + BlockPos.of(fuseProject().cells.get(0).pos()).toShortString());
    }

    private static @Nullable Project fuseProject() {
        return colony().projects.stream().filter(p -> p.template.getPath().equals("fuse_box")).reduce((a, b) -> b).orElse(null);
    }

    private static void climb(TestM6.Run r) {
        SelfTest t = r.t();
        BlockPos a = r.at(40, 0, 132);
        BlockPos b = r.at(58, 0, 132);
        double[] top = new double[1];
        boolean[] climbed = new boolean[1];
        t.then(x -> {
            TestM6.pylon(x, a);
            TestM6.pylon(x, b);
            stock(x, r, new ItemStack(AutarkiaItems.INSULATED_CABLE_SPOOL.get(), 4));
            int cost = CableTier.INSULATED.itemCost(Math.sqrt(a.distSqr(b)));
            ColonyRuntime.of(colony()).jobs.add(new ColonyJob(ColonyJob.Type.STRING_CABLE, null, a, null, AutarkiaItems.INSULATED_CABLE_SPOOL.get(), cost, 50, 4,
                x.level.getGameTime(), b, CableTier.INSULATED, 0));
            top[0] = 0;
        });
        t.waitUntil("06#16 the Lineman climbs the pylon, strings the insulated span, welds at the second pylon", 4000, () -> TestM1.spanBetween(t, a, b) != null
            && LINEMAN[0].onGround() && LINEMAN[0].currentJob() == null, x -> {
                powerDocks(x, r);
                if (LINEMAN[0].action() == WorkerEntity.ACTION_CLIMB) {
                    climbed[0] = true;
                }
                top[0] = Math.max(top[0], LINEMAN[0].getY() - a.getY());
            });
        t.check("06#16 it climbed to the pylon top (4 blocks) and came back down", () -> climbed[0] && top[0] >= 3.9 && LINEMAN[0].getY() - a.getY() < 0.5,
            () -> String.format(java.util.Locale.ROOT, "top %.2f, now %.2f", top[0], LINEMAN[0].getY() - a.getY()));
    }

    private static void decay(TestM6.Run r) {
        SelfTest t = r.t();
        BlockPos at = r.at(60, 0, 100);
        t.then(x -> {
            dev.autarkia.colony.ColonyMemory.event(colony(), "cable_snap", "decaytest", at);
            ColonyClock.skip(colony(), 168000);
        });
        t.check("W13 decay: one colony week later the memory weight is 0.5 +- 0.01", () -> colony().memories.stream().filter(m -> m.key().equals("decaytest"))
            .anyMatch(m -> Math.abs(m.weightAt(colony().clock, dev.autarkia.config.AutarkiaConfig.MEMORY_HALF_LIFE_TICKS.get()) - 0.5) <= 0.01),
            () -> colony().memories.stream().filter(m -> m.key().equals("decaytest")).map(m -> String.valueOf(m.weightAt(colony().clock, 168000))).toList().toString());
    }

    /** Describe a project for a failed wait: state, phase, cursor and its jobs with status, attempts and assignee type. */
    static String projectState(SelfTest t, @Nullable Project p) {
        if (p == null) {
            return "no project";
        }
        StringBuilder sb = new StringBuilder(p.state.getSerializedName() + " phase " + p.phase + " cursor " + p.cursor + ", jobs");
        for (ColonyJob j : ColonyRuntime.of(colony()).jobs.all()) {
            if (p.id.equals(j.projectId)) {
                String who = j.assignee != null && t.level.getEntity(j.assignee) instanceof WorkerEntity w ? w.workerType.id().getPath() + " " + w.state() : "-";
                sb.append(" | ").append(j.type).append(' ').append(j.target.toShortString()).append(' ').append(j.status).append(" x").append(j.attempts)
                    .append(' ').append(who).append(j.blockReason.isEmpty() ? "" : " " + j.blockReason);
            }
        }
        return sb.toString();
    }

    /** Describe a worker for a failed wait: state, action, energy, stuck level, position and job. */
    static String workerState(WorkerEntity w) {
        ColonyJob j = w.currentJob();
        return String.format(java.util.Locale.ROOT, "%s action %d energy %d stuck %d ground %s at %.2f %.2f %.2f job %s walk %s", w.state(), w.action(), w.energy(),
            w.getBrain().getMemory(WorkerMemories.STUCK_LEVEL.get()).orElse(0), w.onGround(), w.getX(), w.getY(), w.getZ(), j == null ? "-" : j.type,
            w.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET).map(wt -> wt.getTarget().currentBlockPosition().toShortString())
                .orElse("-"));
    }

    static WorkerEntity[] drones() {
        return DRONES;
    }

    static void clearStock(SelfTest t, TestM6.Run r) {
        emptyStock(t, r);
    }
}

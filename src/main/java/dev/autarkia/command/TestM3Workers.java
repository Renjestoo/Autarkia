package dev.autarkia.command;

import dev.autarkia.colony.ColonyJob;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GridMember;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.worker.WorkerAssemblerBlockEntity;
import dev.autarkia.worker.WorkerEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The 06 worker checks of {@code /autarkia test m3}: #1, #2, #4 to #7 and #28 to #30 with Porters. */
final class TestM3Workers {
    private TestM3Workers() {
    }

    static BlockPos post(TestM3.Run r) {
        return r.at(20, 0, 10);
    }

    /** Keep the test winding post full: it stands for a powered cable in these checks. */
    static void powerPost(TestM3.Run r) {
        if (r.t.level.getBlockEntity(post(r)) instanceof GridMember m) {
            m.gridBuffer().fill(4000);
        }
    }

    private static int cost(TestM3.Run r) {
        return r.colony().workerCost();
    }

    private static WorkerEntity spawn(TestM3.Run r, BlockPos at) {
        WorkerEntity w = AutarkiaEntities.PORTER.get().create(r.t.level, EntitySpawnReason.COMMAND);
        w.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        r.t.level.addFreshEntity(w);
        return w;
    }

    private static void addJob(TestM3.Run r, ColonyJob j) {
        r.rt().jobs.add(j);
    }

    private static boolean logHas(TestM3.Run r, String text) {
        return r.rt().log.stream().anyMatch(l -> l.contains(text));
    }

    static void run(TestM3.Run r) {
        SelfTest t = r.t;
        WorkerEntity[] p = new WorkerEntity[3];
        ItemStack[] capsule = new ItemStack[1];
        Vec3[] start = new Vec3[1];
        t.then(x -> {
            TestM1.place(x, post(r), AutarkiaBlocks.WINDING_POST.get().defaultBlockState());
            ItemEntity drop = x.level.getEntitiesOfClass(ItemEntity.class, new AABB(r.at(24, 0, 22)).inflate(6), e -> e.getItem().is(AutarkiaItems.WORKER_CAPSULE.get())
                && e.getItem().has(DataComponents.ENTITY_DATA)).stream().findFirst().orElse(null);
            capsule[0] = drop == null ? ItemStack.EMPTY : drop.getItem().copy();
            if (drop != null) {
                drop.discard();
            }
            InteractionResult res = r.use(capsule[0], r.at(20, -1, 16));
            List<WorkerEntity> ws = r.workers();
            p[0] = ws.isEmpty() ? null : ws.get(0);
            x.report("06#28 the starter capsule releases a Porter on the ground in the domain", res == InteractionResult.SUCCESS && p[0] != null
                && p[0].colony() == r.colony() && cost(r) == 1, res + ", " + ws.size() + " workers, cost " + cost(r));
            if (p[0] != null) {
                start[0] = p[0].position();
            }
        });
        t.waitUntil("06#1 the brain ticks: the Porter wanders within 200 ticks", 200, () -> p[0] != null && p[0].position().distanceTo(start[0]) > 1.5,
            x -> powerPost(r));
        assemble(r, p);
        capsule(r, p, capsule);
        crank(r, p);
        runDown(r, p);
        stuck(r, p);
        neverGrief(r, p);
        t.then(x -> {
            InteractionResult res = r.use(capsule[0], r.at(22, -1, 16));
            WorkerEntity back = r.workers().stream().filter(w -> w != p[0]).findFirst().orElse(null);
            x.report("06#28 the stored Porter returns with the same spring and cargo", res == InteractionResult.SUCCESS && back != null
                && back.energy() == 4321 && back.held(s -> s.is(AutarkiaItems.BRONZE_INGOT.get())) == 3 && cost(r) == 2,
                back == null ? res.toString() : back.energy() + " T, " + back.held(s -> s.is(AutarkiaItems.BRONZE_INGOT.get())) + " ingots, cost " + cost(r));
        });
    }

    private static void assemble(TestM3.Run r, WorkerEntity[] p) {
        SelfTest t = r.t;
        BlockPos a = r.at(10, 0, 22);
        t.then(x -> {
            TestM1.placeBig(x, a, AutarkiaBlocks.WORKER_ASSEMBLER.get(), Direction.SOUTH);
            if (x.level.getBlockEntity(a) instanceof WorkerAssemblerBlockEntity be) {
                load(be);
                be.energy().fill(40000);
            }
        });
        t.waitTicks(3);
        t.check("06#2 the assembler starts with parts and power", () -> t.level.getBlockEntity(a) instanceof WorkerAssemblerBlockEntity be
            && be.status() == WorkerAssemblerBlockEntity.Status.RUNNING, () -> t.level.getBlockEntity(a) instanceof WorkerAssemblerBlockEntity be ? be.status().toString() : "missing");
        t.waitUntil("06#2 after 600 ticks a Porter walks out of the front", 700, () -> r.workers().size() == 2 && cost(r) == 2, x -> powerPost(r));
        t.then(x -> {
            WorkerEntity made = r.workers().stream().filter(w -> w != p[0]).findFirst().orElse(null);
            p[1] = made;
            x.report("06#2 the new Porter stands south of the assembler and the parts are used", made != null && made.getZ() > a.getZ() + 0.5
                && x.level.getBlockEntity(a) instanceof WorkerAssemblerBlockEntity be && be.inventory().get(0).isEmpty(),
                made == null ? "none" : made.blockPosition().toShortString());
            p[2] = spawn(r, r.at(26, 0, 16));
            spawn(r, r.at(26, 0, 18));
        });
        t.waitUntil("06#29 four Porters fill the T1 cap", 60, () -> cost(r) == 4, x -> { });
        t.then(x -> {
            if (x.level.getBlockEntity(a) instanceof WorkerAssemblerBlockEntity be) {
                load(be);
                be.energy().fill(40000);
            }
        });
        t.waitTicks(100);
        t.check("06#29 a 5th Porter does not start: 'Worker cap full'", () -> t.level.getBlockEntity(a) instanceof WorkerAssemblerBlockEntity be
            && be.status() == WorkerAssemblerBlockEntity.Status.CAP_FULL && be.stage() == 0 && r.workers().size() == 4,
            () -> t.level.getBlockEntity(a) instanceof WorkerAssemblerBlockEntity be ? be.status() + ", " + r.workers().size() + " workers" : "missing");
        t.then(x -> {
            if (x.level.getBlockEntity(a) instanceof WorkerAssemblerBlockEntity be) {
                for (int i = 0; i < 7; i++) {
                    be.inventory().put(i, ItemStack.EMPTY);
                }
            }
            for (WorkerEntity w : r.workers()) {
                if (w != p[0] && w != p[1]) {
                    w.unbind();
                    w.discard();
                }
            }
        });
    }

    private static void load(WorkerAssemblerBlockEntity be) {
        be.inventory().put(0, new ItemStack(AutarkiaItems.WALKER_FRAME.get()));
        be.inventory().put(1, new ItemStack(AutarkiaItems.MAINSPRING.get(), 2));
        be.inventory().put(2, new ItemStack(AutarkiaItems.BRONZE_GEAR.get(), 4));
        be.inventory().put(3, new ItemStack(AutarkiaItems.BRONZE_INGOT.get(), 4));
    }

    private static void capsule(TestM3.Run r, WorkerEntity[] p, ItemStack[] capsule) {
        SelfTest t = r.t;
        t.waitUntil("06#28 the second Porter is idle", 600, () -> p[1] != null && p[1].isIdle(), x -> powerPost(r));
        t.then(x -> {
            WorkerEntity w = p[1];
            w.setEnergy(4321);
            w.addCargo(new ItemStack(AutarkiaItems.BRONZE_INGOT.get(), 3));
            capsule[0] = new ItemStack(AutarkiaItems.WORKER_CAPSULE.get());
            r.owner.setShiftKeyDown(true);
            r.owner.setItemInHand(InteractionHand.MAIN_HAND, capsule[0]);
            InteractionResult res = capsule[0].interactLivingEntity(r.owner, w, InteractionHand.MAIN_HAND);
            r.owner.setShiftKeyDown(false);
            x.report("06#28 sneak-use captures the idle Porter and frees its cap", res == InteractionResult.SUCCESS && w.isRemoved()
                && capsule[0].has(DataComponents.ENTITY_DATA) && cost(r) == 1, res + ", cost " + cost(r));
        });
    }

    private static void crank(TestM3.Run r, WorkerEntity[] p) {
        SelfTest t = r.t;
        BlockPos crank = r.at(26, 0, 8);
        BlockPos wheel = r.at(26, 0, 12);
        int[] e0 = new int[1];
        ColonyJob[] job = new ColonyJob[1];
        t.then(x -> {
            TestM1.place(x, crank, AutarkiaBlocks.HAND_CRANK.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
            TestM1.placeBig(x, wheel, AutarkiaBlocks.STONE_FLYWHEEL.get(), Direction.NORTH);
            Grid.SpanResult s = Grid.addSpan(x.level, crank, wheel, CableTier.ROPE);
            job[0] = new ColonyJob(ColonyJob.Type.CRANK, null, crank, null, null, 0, 50, 0, x.level.getGameTime(), null, null, 2400);
            addJob(r, job[0]);
            x.report("06#4 rig: hand crank on rope to a stone flywheel, CRANK job open", s == Grid.SpanResult.OK, s.toString());
        });
        t.waitUntil("06#4 the Porter turns the crank", 600, () -> p[0].action() == WorkerEntity.ACTION_CRANK, x -> powerPost(r));
        t.then(x -> e0[0] = p[0].energy());
        t.waitTicks(100);
        t.check("06#4 the spring drains at 0.75 x 10 T per tick (+ idle)", () -> {
            int d = e0[0] - p[0].energy();
            return d >= 780 && d <= 830;
        }, () -> (e0[0] - p[0].energy()) + " T in 100 ticks");
        t.check("06#4 output reaches the stone flywheel", () -> t.level.getBlockEntity(wheel) instanceof GridMember m && m.gridBuffer().stored() > 0,
            () -> t.level.getBlockEntity(wheel) instanceof GridMember m ? m.gridBuffer().stored() + " FE" : "missing");
        t.then(x -> p[0].setEnergy(p[0].reserve() - 20));
        t.waitUntil("06#5 below the reserve the Porter releases the job and winds at the post", 400, () -> p[0].state() == WorkerEntity.State.RECHARGING
            && p[0].action() == WorkerEntity.ACTION_RECHARGE && job[0].status == ColonyJob.Status.OPEN, x -> powerPost(r));
        t.waitUntil("06#5 the spring winds full", 600, () -> p[0].energy() >= p[0].workerType.capacity(), x -> powerPost(r));
        t.waitUntil("06#5 then it claims the crank job again", 400, () -> job[0].status == ColonyJob.Status.ASSIGNED && p[0].currentJob() == job[0],
            x -> powerPost(r));
        t.then(x -> {
            r.rt().jobs.cancel(r.colony(), x.level, job[0].id);
            if (x.level.getBlockEntity(wheel) instanceof GridMember m) {
                m.gridBuffer().drain(m.gridBuffer().stored());
            }
            x.level.destroyBlock(crank, false);
            x.level.destroyBlock(wheel, false);
            x.level.getEntitiesOfClass(ItemEntity.class, new AABB(wheel).inflate(8)).forEach(e -> e.discard());
        });
    }

    private static void runDown(TestM3.Run r, WorkerEntity[] p) {
        SelfTest t = r.t;
        t.waitUntil("06#6 the Porter is idle again", 400, () -> p[0].isIdle(), x -> powerPost(r));
        t.then(x -> p[0].setEnergy(0));
        t.waitTicks(2);
        t.check("06#6 at 0 T the Porter runs down and fails 'no energy'", () -> p[0].runDown() && p[0].state() == WorkerEntity.State.FAILED
            && p[0].failReason().equals("no_energy"), () -> p[0].stateName() + ", rundown " + p[0].runDown());
        t.then(x -> {
            r.owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            InteractionResult res = p[0].interact(r.owner, InteractionHand.MAIN_HAND, p[0].position());
            x.report("06#6 a right-click adds 100 T and wakes it", res.consumesAction() && p[0].energy() == 100 && !p[0].runDown()
                && p[0].state() != WorkerEntity.State.FAILED, res + ", " + p[0].energy() + " T, " + p[0].stateName());
        });
        t.waitUntil("06#6 it winds back to full at the post", 900, () -> p[0].energy() >= p[0].workerType.capacity() && p[0].isIdle(), x -> powerPost(r));
    }

    private static void stuck(TestM3.Run r, WorkerEntity[] p) {
        SelfTest t = r.t;
        BlockPos far = r.at(36, 1, 34);
        ColonyJob[] job = new ColonyJob[1];
        List<BlockPos> wall = new ArrayList<>();
        BlockPos[] at = new BlockPos[1];
        t.then(x -> {
            TestM1.place(x, far.below(), Blocks.STONE.defaultBlockState());
            TestM1.place(x, far, AutarkiaBlocks.HAND_CRANK.get().defaultBlockState());
            r.rt().log.clear();
            job[0] = new ColonyJob(ColonyJob.Type.CRANK, null, far, null, null, 0, 50, 0, x.level.getGameTime(), null, null, 200);
            addJob(r, job[0]);
            at[0] = p[0].blockPosition();
        });
        t.waitUntil("06#7 the Porter travels to a far job", 400, () -> p[0].state() == WorkerEntity.State.TRAVELLING
            && p[0].blockPosition().distManhattan(at[0]) >= 3 && p[0].blockPosition().distManhattan(post(r)) >= 4, x -> powerPost(r));
        t.then(x -> {
            BlockPos c = p[0].blockPosition();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        for (int dy = 0; dy <= 1; dy++) {
                            BlockPos b = c.offset(dx, dy, dz);
                            if (x.level.getBlockState(b).isAir()) {
                                x.level.setBlock(b, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
                                wall.add(b);
                            }
                        }
                    }
                }
            }
            p[0].snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, p[0].getYRot(), 0);
            at[0] = c;
        });
        t.waitUntil("06#7 walled in, stuck levels 1 to 3 run and the job fails NO_PATH", 900, () -> logHas(r, "failed: no_path"), x -> powerPost(r));
        t.then(x -> wall.forEach(b -> x.level.setBlock(b, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)));
        t.waitUntil("06#7 with the wall gone it recovers and moves on", 1800, () -> p[0].state() != WorkerEntity.State.FAILED
            && p[0].blockPosition().distManhattan(at[0]) >= 3, x -> powerPost(r));
        t.then(x -> {
            r.rt().jobs.cancel(r.colony(), x.level, job[0].id);
            x.level.setBlock(far, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        });
    }

    private static void neverGrief(TestM3.Run r, WorkerEntity[] p) {
        SelfTest t = r.t;
        BlockPos crate = r.at(14, 0, 24);
        BlockPos target = r.at(16, 0, 26);
        t.waitUntil("06#30 the Porter is idle and wound", 1200, () -> p[0].isIdle() && p[0].charge() > 0.5f, x -> powerPost(r));
        t.then(x -> {
            TestM1.place(x, crate, AutarkiaBlocks.CRATE.get().defaultBlockState());
            TestM1.insertItems(x, crate, new ItemStack(Items.OAK_PLANKS, 4));
            TestM1.place(x, target, Blocks.STONE.defaultBlockState());
            r.rt().log.clear();
            addJob(r, new ColonyJob(ColonyJob.Type.PLACE, null, target, Blocks.OAK_PLANKS.defaultBlockState(), Items.OAK_PLANKS, 1, 50, 3,
                x.level.getGameTime(), null, null, 0));
        });
        t.waitUntil("06#30 a planned cell holding a player block fails TARGET_CHANGED (target_obstructed)", 1200,
            () -> logHas(r, "failed: target_obstructed"), x -> powerPost(r));
        t.check("06#30 the player block stays", () -> t.level.getBlockState(target).is(Blocks.STONE), () -> t.level.getBlockState(target).toString());
        t.then(x -> {
            for (ColonyJob j : r.rt().jobs.all()) {
                if (j.target.equals(target)) {
                    r.rt().jobs.cancel(r.colony(), x.level, j.id);
                }
            }
            x.level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        });
    }
}

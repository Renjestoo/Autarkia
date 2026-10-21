package dev.autarkia.command;

import com.mojang.authlib.GameProfile;
import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyClock;
import dev.autarkia.colony.ColonyPlans;
import dev.autarkia.colony.ColonyPreview;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.Planner;
import dev.autarkia.colony.Project;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.HandCrankBlockEntity;
import dev.autarkia.network.ProjectPreviewPayload;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.worker.WorkerEntity;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * {@code /autarkia test m3}: 05 W1, W1 fail, W2 and W2 manual plus 06 #1, #2, #4 to #7 and #28 to #30 (in
 * {@link TestM3Workers}) on a stone platform at y 112 or higher, so the windmill of W2 reaches a useful rotor height.
 * The colony belongs to a fixed test fake player; a calling player is added as trusted and gets the advancements.
 */
public final class TestM3 {
    static final UUID OWNER = UUID.nameUUIDFromBytes("autarkia-test-owner".getBytes(StandardCharsets.UTF_8));

    private TestM3() {
    }

    /** One M3 run: the test, the platform base, the owner fake player and the colony once it exists. */
    static final class Run {
        final SelfTest t;
        final BlockPos base;
        final FakePlayer owner;
        @Nullable UUID colonyId;

        Run(SelfTest t, BlockPos base, FakePlayer owner) {
            this.t = t;
            this.base = base;
            this.owner = owner;
        }

        BlockPos at(int x, int y, int z) {
            return base.offset(x, y, z);
        }

        Colony colony() {
            Colony c = colonyId == null ? null : Colonies.registry().get(colonyId);
            if (c == null) {
                throw new IllegalStateException("The M3 test colony does not exist");
            }
            return c;
        }

        ColonyRuntime rt() {
            return ColonyRuntime.of(colony());
        }

        List<WorkerEntity> workers() {
            return t.level.getEntitiesOfClass(WorkerEntity.class, new AABB(Vec3.atCenterOf(at(-4, -4, -4)), Vec3.atCenterOf(at(52, 16, 50))));
        }

        InteractionResult use(ItemStack stack, BlockPos clicked) {
            owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
            owner.snapTo(clicked.getX() + 0.5, clicked.getY() + 1, clicked.getZ() + 3.5, 180, 30);
            UseOnContext ctx = new UseOnContext(owner, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(clicked).add(0, 0.5, 0), Direction.UP,
                clicked, false));
            return stack.useOn(ctx);
        }

        void crank(BlockPos pos) {
            if (t.level.getGameTime() % 4 == 0 && t.level.getBlockEntity(pos) instanceof HandCrankBlockEntity c) {
                c.crank(owner);
            }
        }
    }

    /** Build the platform and run the M3 script. */
    public static int run(CommandSourceStack src) {
        SelfTest t = SelfTest.create("m3", src);
        BlockPos o = t.origin;
        Run r = new Run(t, new BlockPos(o.getX(), Math.max(o.getY() + 8, 112), o.getZ()), FakePlayerFactory.get(t.level, new GameProfile(OWNER, "AutarkiaTest")));
        t.then(x -> reset(r));
        data(r);
        w1(r);
        TestM3Workers.run(r);
        w2(r);
        w2Manual(r);
        return t.start();
    }

    private static void box(Run r, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(r.at(x0, y0, z0), r.at(x1, y1, z1))) {
            r.t.level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private static void reset(Run r) {
        SelfTest t = r.t;
        t.command("forceload add " + (r.base.getX() - 8) + " " + (r.base.getZ() - 8) + " " + (r.base.getX() + 56) + " " + (r.base.getZ() + 52));
        for (Colony c : List.copyOf(Colonies.registry().all())) {
            if (c.owner.equals(OWNER) || c.dimension.equals(t.level.dimension()) && c.corePos.distManhattan(r.base) < 160) {
                Colonies.dissolve(c);
            }
        }
        AABB area = new AABB(Vec3.atCenterOf(r.at(-8, -6, -8)), Vec3.atCenterOf(r.at(56, 20, 54)));
        t.level.getEntitiesOfClass(WorkerEntity.class, area).forEach(w -> w.discard());
        box(r, -2, 0, -2, 50, 14, 46, Blocks.AIR.defaultBlockState());
        box(r, -2, -1, -2, 50, -1, 46, Blocks.STONE.defaultBlockState());
        for (int x = 8; x <= 40; x++) {
            for (int z = 6; z <= 38; z++) {
                if ((x > 28 || z > 26) && (x + z) % 2 == 1) {
                    t.level.setBlock(r.at(x, 0, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        t.level.getEntitiesOfClass(ItemEntity.class, area).forEach(e -> e.discard());
        r.colonyId = null;
        Autarkia.LOG.info("[TEST m3] platform at {}", r.base.toShortString());
    }

    private static void data(Run r) {
        SelfTest t = r.t;
        t.check("templates: the 6 T1 colony templates and 5 structure plans load", () -> ColonyPlans.templates().entrySet().stream()
                .filter(e -> e.getValue().tier() == 1).map(java.util.Map.Entry::getKey).collect(java.util.stream.Collectors.toSet()).equals(
                java.util.Set.of("windmill", "treadwheel", "crate_cluster", "flywheel", "parallel_span", "winding_post"))
                && ColonyPlans.plans().keySet().stream().map(i -> i.getPath()).collect(java.util.stream.Collectors.toSet()).containsAll(
                java.util.Set.of("windmill", "treadwheel", "crate_cluster", "flywheel", "winding_post")),
            () -> ColonyPlans.templates().keySet() + " / " + ColonyPlans.plans().keySet());
        t.check("plans: the windmill plan holds the windmill, a pulley and its span", () -> {
            ColonyPlans.Plan p = ColonyPlans.plan(Autarkia.id("windmill"));
            return p != null && p.cells().size() == 2 && p.spans().size() == 1 && p.port().isPresent() && p.commission().equals("runs");
        }, () -> String.valueOf(ColonyPlans.plan(Autarkia.id("windmill"))));
        t.check("advancements: colony/root is a root; power/root and workers/first_porter hang under it", () -> {
            AdvancementHolder root = t.level.getServer().getAdvancements().get(Autarkia.id("colony/root"));
            AdvancementHolder power = t.level.getServer().getAdvancements().get(Autarkia.id("power/root"));
            AdvancementHolder porter = t.level.getServer().getAdvancements().get(Autarkia.id("workers/first_porter"));
            return root != null && root.value().parent().isEmpty() && power != null && power.value().parent().orElseThrow().equals(root.id())
                && porter != null && porter.value().parent().orElseThrow().equals(root.id())
                && t.level.getServer().getAdvancements().get(Autarkia.id("colony/it_fixed_itself")) != null
                && t.level.getServer().getAdvancements().get(Autarkia.id("workers/self_built")) != null;
        }, () -> "colony, power and workers trees");
        t.check("recipes: core seed, board, tablet, assembler, post, frame, capsule, manual and the Porter assembly exist", () -> {
            var map = t.level.recipeAccess().recipeMap();
            for (String id : List.of("core_seed", "project_board", "planning_tablet", "worker_assembler", "winding_post", "walker_frame", "worker_capsule",
                "field_manual", "worker_assembly/porter")) {
                if (map.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Autarkia.id(id))) == null) {
                    return false;
                }
            }
            return true;
        }, () -> "9 recipes");
    }

    private static void w1(Run r) {
        SelfTest t = r.t;
        BlockPos hole = r.at(44, -3, 22);
        BlockPos ground = r.at(24, -1, 22);
        t.then(x -> {
            box(r, 42, -3, 20, 46, -1, 24, Blocks.STONE.defaultBlockState());
            x.level.setBlock(hole.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            x.level.setBlock(hole.above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            InteractionResult res = r.use(new ItemStack(AutarkiaItems.CORE_SEED.get()), hole);
            x.report("W1 fail: the seed in a 2-block hole is refused and places nothing", res == InteractionResult.FAIL
                && x.level.getBlockState(hole.above()).isAir(), res.toString());
            x.level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            InteractionResult ok = r.use(new ItemStack(AutarkiaItems.CORE_SEED.get()), ground);
            Colony c = Colonies.byCore(x.level, ground.above());
            r.colonyId = c == null ? null : c.id;
            if (c != null && x.player != null) {
                c.trusted.add(x.player.getUUID());
            }
            x.report("W1 the seed on grass places a growing Colony Core", ok == InteractionResult.SUCCESS && c != null && c.state == Colony.State.GROWING
                && x.level.getBlockState(ground.above()).is(AutarkiaBlocks.COLONY_CORE.get()), ok + ", " + (c == null ? "no colony" : c.state));
        });
        t.waitUntil("W1 the Core blooms after 200 ticks: T1 ACTIVE", 300, () -> r.colonyId != null && r.colony().state == Colony.State.ACTIVE
            && r.colony().tier == 1, x -> { });
        t.check("W1 a Porter capsule and a field manual drop at the Core", () -> {
            List<ItemEntity> items = t.level.getEntitiesOfClass(ItemEntity.class, new AABB(ground).inflate(6));
            boolean capsule = items.stream().anyMatch(e -> e.getItem().is(AutarkiaItems.WORKER_CAPSULE.get()) && e.getItem().has(DataComponents.ENTITY_DATA));
            boolean manual = items.stream().anyMatch(e -> e.getItem().is(AutarkiaItems.FIELD_MANUAL.get()));
            return capsule && manual;
        }, () -> t.level.getEntitiesOfClass(ItemEntity.class, new AABB(ground).inflate(6)).size() + " items");
        t.check("W1 the domain is 33 x 33 around the Core", () -> Colonies.at(t.level, r.at(8, 0, 6)) == r.colony()
            && Colonies.at(t.level, r.at(40, 0, 38)) == r.colony() && Colonies.at(t.level, r.at(7, 0, 22)) == null && Colonies.at(t.level, r.at(24, 0, 39)) == null,
            () -> "edges 8..40 x 6..38");
        t.then(x -> {
            InteractionResult res = r.use(new ItemStack(AutarkiaItems.CORE_SEED.get()), r.at(16, -1, 8));
            x.report("W1 fail: a seed inside the domain is refused and places nothing", res == InteractionResult.FAIL
                && x.level.getBlockState(r.at(16, 0, 8)).isAir(), res.toString());
            if (x.player != null) {
                AdvancementHolder root = x.level.getServer().getAdvancements().get(Autarkia.id("colony/root"));
                x.report("W1 the trusted player has colony/root", root != null && x.player.getAdvancements().getOrStartProgress(root).isDone(), "colony/root");
            }
        });
    }

    static @Nullable Project project(Run r, String template) {
        for (Project p : r.colony().projects) {
            if (p.template.getPath().equals(template) && p.state.open()) {
                return p;
            }
        }
        return null;
    }

    private static @Nullable SignBlockEntity sign(Run r, BlockPos around) {
        for (BlockPos p : BlockPos.betweenClosed(around.offset(-4, -2, -4), around.offset(4, 2, 4))) {
            if (r.t.level.getBlockEntity(p) instanceof SignBlockEntity s) {
                return s;
            }
        }
        return null;
    }

    private static void w2(Run r) {
        SelfTest t = r.t;
        BlockPos crank = r.at(12, 0, 12);
        BlockPos mill1 = r.at(16, 0, 12);
        BlockPos mill2 = r.at(12, 0, 16);
        BlockPos crate1 = r.at(18, 0, 20);
        BlockPos crate2 = r.at(19, 0, 20);
        Project[] w = new Project[1];
        BlockPos[] anchor = new BlockPos[1];
        t.then(x -> {
            TestM1.place(x, crank, AutarkiaBlocks.HAND_CRANK.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
            TestM1.placeBig(x, mill1, AutarkiaBlocks.STAMP_MILL.get(), Direction.NORTH);
            TestM1.placeBig(x, mill2, AutarkiaBlocks.STAMP_MILL.get(), Direction.NORTH);
            Grid.SpanResult a = Grid.addSpan(x.level, crank, mill1, CableTier.ROPE);
            Grid.SpanResult b = Grid.addSpan(x.level, crank, mill2, CableTier.ROPE);
            TestM1.insertItems(x, mill1, new ItemStack(Items.RAW_IRON, 64));
            TestM1.insertItems(x, mill2, new ItemStack(Items.RAW_IRON, 64));
            TestM1.place(x, crate1, AutarkiaBlocks.CRATE.get().defaultBlockState());
            TestM1.place(x, crate2, AutarkiaBlocks.CRATE.get().defaultBlockState());
            TestM1.insertItems(x, crate1, new ItemStack(AutarkiaItems.WINDMILL.get()));
            TestM1.insertItems(x, crate1, new ItemStack(AutarkiaItems.ROPE_PULLEY.get(), 2));
            TestM1.insertItems(x, crate1, new ItemStack(AutarkiaItems.ROPE_COIL.get(), 16));
            TestM1.insertItems(x, crate1, new ItemStack(Items.OAK_SIGN));
            TestM1.insertItems(x, crate2, new ItemStack(Items.DIRT, 32));
            TestM1.insertItems(x, crate2, new ItemStack(Items.OAK_FENCE, 4));
            Colony c = r.colony();
            c.autonomy = Colony.Autonomy.ASSISTED;
            Planner.cancelAllJobs(c);
            c.projects.clear();
            x.report("W2 rig: crank and two loaded stamp mills on rope, crates with the windmill BOM and a sign", a == Grid.SpanResult.OK
                && b == Grid.SpanResult.OK, a + ", " + b);
        });
        t.waitUntil("W2 the power deficit holds: S03 >= 0.25 for 1200 ticks", 3000, () -> Planner.triggered(r.colony(), t.level, "windmill",
            ColonyPlans.templates().get("windmill")), x -> {
                r.crank(crank);
                TestM3Workers.powerPost(r);
            });
        t.then(x -> {
            Autarkia.LOG.info("[TEST m3] W2 planner run with S01 {} S02 {} S03 {}", r.rt().signal("S01", x.level.getGameTime(), 0),
                r.rt().signal("S02", x.level.getGameTime(), 0), r.rt().signal("S03", x.level.getGameTime(), 0));
            Planner.pass(r.colony(), x.level);
        });
        t.waitUntil("W2 planner run proposes a windmill", 400, () -> project(r, "windmill") != null, x -> {
            r.crank(crank);
            TestM3Workers.powerPost(r);
        });
        t.then(x -> {
            w[0] = project(r, "windmill");
            double arg = w[0] == null ? -1 : w[0].reason.args().get(0);
            x.report("W2 the card reads 'Power 40% short' (+-5)", w[0] != null && w[0].reason.key().equals("power_short") && Math.abs(arg - 40) <= 5,
                w[0] == null ? "none" : w[0].reason.key() + " " + Math.round(arg));
            ProjectPreviewPayload ghost = w[0] == null ? null : ColonyPreview.build(x.level, r.owner, w[0]);
            x.report("W2 the ghost preview is cyan and holds the windmill", ghost != null && ghost.argb() == 0x734DCCFF && ghost.entries().stream()
                .anyMatch(g -> Block.stateById(g.stateId()).is(AutarkiaBlocks.WINDMILL.get())), ghost == null ? "none" : ghost.entries().size() + " ghosts");
            x.report("W2 the site needs leveling", w[0] != null && w[0].levelingEdits() > 0, w[0] == null ? "none" : w[0].levelingEdits() + " edits");
        });
        t.waitTicks(600);
        t.check("W2 Assisted: no approval before one day", () -> w[0] != null && w[0].state == Project.State.PROPOSED,
            () -> w[0] == null ? "none" : w[0].state.toString());
        t.then(x -> ColonyClock.skip(r.colony(), 24000));
        t.waitUntil("W2 after clock skip 24000 the project auto-approves and starts", 200, () -> w[0] != null && w[0].auto
            && (w[0].state == Project.State.BUILDING || w[0].state == Project.State.COMMISSIONING || w[0].state == Project.State.DONE), x -> {
                r.crank(crank);
                TestM3Workers.powerPost(r);
            });
        t.waitUntil("W2 Porters level, place, string the rope and commission the windmill", 9000, () -> w[0] != null && w[0].state == Project.State.DONE,
            x -> {
                r.crank(crank);
                TestM3Workers.powerPost(r);
                if (x.level.getGameTime() % 1000 == 0) {
                    trace(r, w[0]);
                }
                if (w[0] != null && w[0].state == Project.State.BLOCKED) {
                    x.report("W2 project blocked", false, w[0].blockReason);
                    w[0].state = Project.State.CANCELLED;
                }
            });
        t.then(x -> {
            anchor[0] = w[0] == null ? r.base : BlockPos.of(w[0].anchor);
            boolean filled = w[0] != null && java.util.Arrays.stream(w[0].fills).allMatch(f -> !x.level.getBlockState(BlockPos.of(f)).isAir());
            x.report("W2 the leveled cells hold blocks", filled, w[0] == null ? "none" : w[0].fills.length + " fills");
            x.report("W2 the windmill stands at the site", x.level.getBlockState(anchor[0]).is(AutarkiaBlocks.WINDMILL.get()), anchor[0].toShortString());
            boolean strung = w[0] != null && w[0].spans.stream().allMatch(s -> TestM1.spanBetween(x, BlockPos.of(s.a()), BlockPos.of(s.b())) != null);
            x.report("W2 every planned rope span is strung", strung, w[0] == null ? "none" : w[0].spans.size() + " spans");
        });
        t.waitUntil("W2 a sign stands next to it: 'Power 40% short. / Fixed. / Day N'", 600, () -> {
            SignBlockEntity s = sign(r, anchor[0]);
            if (s == null) {
                return false;
            }
            var lines = s.getText(net.minecraft.world.level.block.entity.SignTextSlot.FRONT).getMessages(false);
            return lines.get(0).getContents() instanceof TranslatableContents tc && tc.getKey().equals("autarkia.sign.power_short")
                && String.valueOf(tc.getArgs()[0]).equals("40") && lines.get(1).getContents() instanceof TranslatableContents f
                && f.getKey().equals("autarkia.sign.fixed");
        }, x -> r.crank(crank));
        t.waitUntil("W2 S03 goes below 0.1", 2400, () -> r.rt().signal("S03", t.level.getGameTime(), 1) < 0.1, x -> {
            r.crank(crank);
            TestM3Workers.powerPost(r);
        });
        t.then(x -> {
            if (x.player != null) {
                for (String a : List.of("colony/first_proposal", "colony/it_fixed_itself", "workers/self_built")) {
                    AdvancementHolder h = x.level.getServer().getAdvancements().get(Autarkia.id(a));
                    x.report("W2 the trusted player has " + a, h != null && x.player.getAdvancements().getOrStartProgress(h).isDone(), a);
                }
            }
        });
    }

    private static void trace(Run r, @Nullable Project p) {
        Autarkia.LOG.info("[TEST m3] trace: project {} phase {} cursor {}; jobs {}; workers {}", p == null ? "none" : p.state, p == null ? -1 : p.phase,
            p == null ? -1 : p.cursor, r.rt().jobs.all().stream().map(j -> j.type + "@" + j.target.toShortString() + ":" + j.status + "/" + j.attempts).toList(),
            r.workers().stream().map(wk -> wk.stateName() + ":" + wk.action() + ":" + wk.energy() + ":" + wk.cargo + ":" + wk.jobName() + ":"
                + wk.blockPosition().toShortString() + ":charger " + wk.getBrain().getMemory(dev.autarkia.worker.WorkerMemories.CHARGER_POS.get())
                .map(g -> g.pos().toShortString()).orElse("-")).toList() + "; chargers " + r.rt().chargers);
    }

    private static void w2Manual(Run r) {
        SelfTest t = r.t;
        Project[] p = new Project[1];
        t.then(x -> {
            Colony c = r.colony();
            c.autonomy = Colony.Autonomy.MANUAL;
            r.rt().forced.put("S09", new ColonyRuntime.Forced(0.9, x.level.getGameTime() + 3000));
        });
        t.waitUntil("W2 manual: storage 90% full holds 600 ticks", 1500, () -> Planner.triggered(r.colony(), t.level, "crate_cluster",
            ColonyPlans.templates().get("crate_cluster")), x -> { });
        t.then(x -> Planner.pass(r.colony(), x.level));
        t.waitUntil("W2 manual: a crate cluster is proposed", 400, () -> project(r, "crate_cluster") != null, x -> { });
        t.then(x -> {
            p[0] = project(r, "crate_cluster");
            ColonyClock.skip(r.colony(), 48000);
        });
        t.waitTicks(200);
        t.check("W2 manual: after clock skip 48000 the proposal stays PROPOSED and no job exists for it", () -> p[0] != null
            && p[0].state == Project.State.PROPOSED && r.rt().jobs.all().stream().noneMatch(j -> p[0].id.equals(j.projectId)),
            () -> p[0] == null ? "none" : p[0].state + ", " + r.rt().jobs.all().size() + " jobs");
        t.then(x -> r.rt().forced.remove("S09"));
    }
}

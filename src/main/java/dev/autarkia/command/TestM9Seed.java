package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Builds;
import dev.autarkia.colony.ColonyPlans;
import dev.autarkia.colony.ColonySensors;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.OutpostCoreBlockEntity;
import dev.autarkia.colony.Outposts;
import dev.autarkia.colony.Pattern;
import dev.autarkia.colony.RisingBlockEntity;
import dev.autarkia.colony.SeedContents;
import dev.autarkia.colony.SeedLanding;
import dev.autarkia.colony.SeedLayout;
import dev.autarkia.colony.SeedMatrixBlockEntity;
import dev.autarkia.colony.SeedUnpack;
import dev.autarkia.colony.SeedWorkers;
import dev.autarkia.colony.ThrownAutarkySeed;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.worker.WorkerEntity;
import dev.autarkia.worker.WorkerType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jspecify.annotations.Nullable;

/**
 * The seed section of {@code /autarkia test m9} (05b section 11.2 W21 and W21 fail): the seed matrix at x 230, z 170
 * packs the three demo Patterns (an OUTPOST Pattern, a yard with a copper span, a tower) with its colony bill; the owner,
 * facing east and looking straight down from 12 blocks above the top of a stone hill at x 400, z 100, throws the seed
 * (a real throw at 1.5, its speed is read) and the test watches the unpack every tick (rising blocks alive, spawn
 * heights, start 3 blocks below the target). W21 fail: 5 more outposts at z 240 to 290 (64 or more apart, clear of the earlier rigs) give 6 of 6, a
 * second seed falls at x 400, z 250, and the
 * block snapshot of its footprint area must not change. The rigs stay.
 */
final class TestM9Seed {
    private static final int HILL_HALF = 18;
    private static final int HILL_LAYERS = 6;

    private TestM9Seed() {
    }

    private static BlockPos matrix(TestM9.Run r) {
        return r.at(230, 0, 170);
    }

    /** Measured values of the W21 run. */
    private static final class Watch {
        long pressAt = -1;
        long packedAt = -1;
        long feAdded;
        ItemStack seed = ItemStack.EMPTY;
        double speed;
        int outpostsBefore;
        long landAt = -1;
        long finishAt = -1;
        BlockPos core;
        int maxAlive;
        int riseStart = -1;
        int maxEdits;
        int edits;
        long fillMu;
        int spawned;
        int blocks;
        int soilFail;
        int orderFail;
        int maxY = Integer.MIN_VALUE;
        final Set<Integer> seen = new HashSet<>();
        String packState = "";
    }

    static void run(TestM9.Run r) {
        Watch w = new Watch();
        w21(r, w);
        fail(r, w);
    }

    private static void w21(TestM9.Run r, Watch w) {
        SelfTest t = r.t();
        List<Pattern> demo = new ArrayList<>();
        t.then(x -> {
            Colony c = TestM9.colony();
            for (int y = 0; y < HILL_LAYERS; y++) {
                int h = HILL_HALF - 3 * y;
                TestM9.box(r, 400 - h, y, 100 - h, 400 + h, y, 100 + h, Blocks.STONE.defaultBlockState());
            }
            TestM1.placeBig(x, matrix(r), AutarkiaBlocks.SEED_MATRIX.get(), Direction.SOUTH);
            demo.addAll(ShowcaseM9Seed.demoPatterns(x.level, TestM6Colony.owner(x).getUUID()));
            List<ItemStack> bill = new ArrayList<>(List.of(new ItemStack(AutarkiaItems.AI_CORE.get(), SeedMatrixBlockEntity.AI_CORES),
                new ItemStack(AutarkiaItems.NANITE_PASTE.get(), SeedMatrixBlockEntity.NANITE_PASTE)));
            for (var id : SeedWorkers.DEFAULT) {
                bill.addAll(SeedWorkers.refund(SeedWorkers.bill(x.level, id)));
            }
            for (ItemStack s : bill) {
                ItemStack rest = s.copy();
                for (int i = 0; i < 4 && !rest.isEmpty(); i++) {
                    rest.shrink(TestM1.insertAt(x.level, r.at(210 + i, 0, 150), rest));
                }
            }
            ColonySensors.sample(c, x.level);
        });
        t.waitTicks(2);
        t.then(x -> {
            if (x.level.getBlockEntity(matrix(r)) instanceof SeedMatrixBlockEntity m) {
                for (int i = 0; i < demo.size(); i++) {
                    m.inventory().put(i, ShowcaseM9Seed.card(demo.get(i)));
                }
            }
        });
        t.waitTicks(2);
        t.then(x -> {
            if (!(x.level.getBlockEntity(matrix(r)) instanceof SeedMatrixBlockEntity m)) {
                w.packState = "no matrix";
                return;
            }
            m.layout(1, 21, 0, 0);
            m.layout(2, 0, 21, 0);
            TestM9.freeMatter(x, 1_000_000L);
            m.energy().fill(m.energy().room());
            m.menuButton(TestM6Colony.owner(x), SeedMatrixBlockEntity.BUTTON_PACK);
            w.pressAt = x.level.getGameTime();
            w.packState = m.status().name();
        });
        t.repeat(1210, x -> {
            if (x.level.getBlockEntity(matrix(r)) instanceof SeedMatrixBlockEntity m) {
                w.feAdded += m.energy().fill(m.energy().room());
                ItemStack out = m.inventory().get(SeedMatrixBlockEntity.OUTPUT);
                if (w.packedAt < 0 && out.is(AutarkiaItems.AUTARKY_SEED.get())) {
                    w.packedAt = x.level.getGameTime();
                    w.seed = out.copy();
                    long[] mu = m.matter();
                    w.packState = "packed in " + (w.packedAt - w.pressAt) + " ticks, FE " + m.feUsed() + ", matter " + mu[1] + "/" + mu[0] + " MU";
                }
            }
        });
        t.then(x -> {
            if (x.level.getBlockEntity(matrix(r)) instanceof SeedMatrixBlockEntity m && w.packedAt >= 0) {
                m.inventory().put(SeedMatrixBlockEntity.OUTPUT, ItemStack.EMPTY);
            }
            w.outpostsBefore = TestM9.colony().outposts.size();
            ThrownAutarkySeed seed = throwSeed(x, r.at(400, HILL_LAYERS + 12, 100), -90, w.seed.copy());
            w.speed = seed == null ? 0 : seed.getDeltaMovement().length();
        });
        t.repeat(1500, x -> watch(x, r, w));
        t.check("05 W21: the thrown seed (3 Patterns) unpacks on the hill: ring, leveling, blocks rise from the soil in y order, spans connect, the "
            + "crew unfolds; 1,100 to 1,300 ticks, never more than 512 rising blocks alive", () -> w21Pass(t, w), () -> w21Detail(t, w));
    }

    private static @Nullable ThrownAutarkySeed throwSeed(SelfTest x, BlockPos from, float yaw, ItemStack stack) {
        FakePlayer owner = TestM6Colony.owner(x);
        owner.snapTo(from.getX() + 0.5, from.getY(), from.getZ() + 0.5, yaw, 90);
        owner.setYRot(yaw);
        owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(x.level, owner, InteractionHand.MAIN_HAND);
        List<ThrownAutarkySeed> list = x.level.getEntitiesOfClass(ThrownAutarkySeed.class, new AABB(from).inflate(4));
        return list.isEmpty() ? null : list.get(0);
    }

    private static void watch(SelfTest x, TestM9.Run r, Watch w) {
        Colony c = TestM9.colony();
        long now = x.level.getGameTime();
        w.maxAlive = Math.max(w.maxAlive, RisingBlockEntity.alive(x.level));
        if (w.landAt < 0 && c.outposts.size() > w.outpostsBefore) {
            w.landAt = now;
            w.core = c.outposts.get(c.outposts.size() - 1).corePos;
        }
        if (w.landAt < 0 || w.finishAt >= 0) {
            return;
        }
        if (x.level.getBlockEntity(w.core) instanceof OutpostCoreBlockEntity core && core.seed() != null) {
            SeedUnpack s = core.seed();
            w.riseStart = s.riseStart();
            w.maxEdits = s.maxEditsPerTick();
            w.edits = s.levelEdits();
            w.fillMu = s.fillMatter();
            w.spawned = s.spawned();
            w.blocks = s.contents().blocks();
        } else {
            w.finishAt = now;
        }
        List<RisingBlockEntity> fresh = new ArrayList<>(x.level.getEntitiesOfClass(RisingBlockEntity.class, new AABB(w.core).inflate(48),
            e -> e.mode() == RisingBlockEntity.Mode.RISE && w.seen.add(e.getId())));
        fresh.sort(java.util.Comparator.comparingInt(RisingBlockEntity::getId));
        for (RisingBlockEntity e : fresh) {
            int ty = e.target().getY();
            if (Math.abs(e.getY() - (ty - RisingBlockEntity.RISE_DEPTH)) > 0.01 || e.duration() != RisingBlockEntity.RISE_TICKS) {
                w.soilFail++;
            }
            if (ty < w.maxY) {
                w.orderFail++;
            }
            w.maxY = Math.max(w.maxY, ty);
        }
    }

    private static int[] cells(SelfTest t, Colony c) {
        int checked = 0;
        int wrong = 0;
        int n = c.instances.size();
        for (int k = Math.max(0, n - 3); k < n; k++) {
            Colony.Instance in = c.instances.get(k);
            Pattern p = Pattern.load(t.level.getServer(), in.pattern());
            if (p == null) {
                wrong++;
                continue;
            }
            Function<BlockPos, BlockPos> map = ColonyPlans.mapper(p.size(), in.origin(), in.rotation());
            for (int i = 0; i < p.volume(); i++) {
                BlockState want = p.state(i);
                if (want == null || want.isAir() || want.is(AutarkiaBlocks.OUTPOST_CORE.get())) {
                    continue;
                }
                checked++;
                wrong += t.level.getBlockState(map.apply(SeedLayout.rel(p, i))).is(want.getBlock()) ? 0 : 1;
            }
        }
        return new int[] {checked, wrong};
    }

    private static int[] crew(SelfTest t, Colony c, BlockPos core) {
        int porters = 0;
        int drones = 0;
        for (WorkerEntity e : t.level.getEntitiesOfClass(WorkerEntity.class, new AABB(core).inflate(24))) {
            if (e.colony() != c || e.home().distSqr(core.south(3)) > 1) {
                continue;
            }
            porters += e.workerType == WorkerType.PORTER ? 1 : 0;
            drones += e.workerType == WorkerType.BUILDER_DRONE ? 1 : 0;
        }
        return new int[] {porters, drones};
    }

    private static boolean spanned(SelfTest t, Colony c) {
        for (Colony.Instance in : c.instances) {
            Pattern p = Pattern.load(t.level.getServer(), in.pattern());
            if (p != null && !p.spans().isEmpty()) {
                Function<BlockPos, BlockPos> map = ColonyPlans.mapper(p.size(), in.origin(), in.rotation());
                Pattern.SpanRecord s = p.spans().get(0);
                return Builds.strung(t.level, map.apply(s.a()).asLong(), map.apply(s.b()).asLong());
            }
        }
        return false;
    }

    private static boolean advancement(SelfTest t, String id) {
        var adv = t.level.getServer().getAdvancements().get(Autarkia.id(id));
        return adv != null && t.player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    private static boolean w21Pass(SelfTest t, Watch w) {
        Colony c = TestM9.colony();
        if (w.packedAt < 0 || w.landAt < 0 || w.finishAt < 0) {
            return false;
        }
        long total = w.finishAt - w.landAt;
        int[] cells = cells(t, c);
        int[] crew = crew(t, c, w.core);
        return Math.abs(w.speed - ThrownAutarkySeed.VELOCITY) < 0.05 && total >= 1100 && total <= 1300 && w.maxAlive <= 512
            && w.riseStart >= SeedUnpack.RISE_START && w.maxEdits <= SeedUnpack.LEVEL_EDITS_PER_TICK && w.edits > 0 && w.spawned == w.blocks
            && w.soilFail == 0 && w.orderFail == 0 && cells[0] == w.blocks && cells[1] == 0 && spanned(t, c) && crew[0] >= 2 && crew[1] >= 2
            && c.outposts.size() == w.outpostsBefore + 1 && (t.player == null || advancement(t, "colony/instant_base"));
    }

    private static String w21Detail(SelfTest t, Watch w) {
        Colony c = TestM9.colony();
        int[] cells = w.landAt < 0 ? new int[2] : cells(t, c);
        int[] crew = w.core == null ? new int[2] : crew(t, c, w.core);
        return "pack: " + w.packState + " (test added " + w.feAdded + " FE); throw speed " + String.format(java.util.Locale.ROOT, "%.3f", w.speed)
            + "; landed " + (w.landAt >= 0) + " at core " + (w.core == null ? "-" : w.core.toShortString()) + ", outposts " + w.outpostsBefore + " -> "
            + c.outposts.size() + "; unpack " + (w.finishAt < 0 || w.landAt < 0 ? "unfinished" : (w.finishAt - w.landAt) + " ticks") + "; leveling "
            + w.edits + " edits, max " + w.maxEdits + "/tick, " + w.fillMu + " MU for fills; rise from tick " + w.riseStart + ", " + w.spawned + "/"
            + w.blocks + " spawned, max " + w.maxAlive + " alive, " + w.soilFail + " not from y-3, " + w.orderFail + " out of y order; cells "
            + (cells[0] - cells[1]) + "/" + cells[0] + " placed; span " + (w.landAt >= 0 && spanned(t, c)) + "; crew " + crew[0] + " Porter, " + crew[1]
            + " Builder Drone; instant_base " + (t.player == null ? "no player" : advancement(t, "colony/instant_base")) + "; log "
            + SeedLanding.lastFailure();
    }

    private static void fail(TestM9.Run r, Watch w) {
        SelfTest t = r.t();
        BlockState[][] before = new BlockState[1][];
        int[] outposts = new int[2];
        t.then(x -> {
            Colony c = TestM9.colony();
            int[][] sites = {{130, 240}, {200, 280}, {270, 240}, {340, 280}, {430, 290}};
            for (int i = 0; i < sites.length && c.outposts.size() < 6; i++) {
                Outposts.plant(x.level, c, r.at(sites[i][0], 0, sites[i][1]), null, null);
            }
            outposts[0] = c.outposts.size();
            before[0] = snapshot(x, r);
            ItemStack seed = new ItemStack(AutarkiaItems.AUTARKY_SEED.get());
            SeedContents contents = w.seed.get(AutarkiaComponents.SEED_CONTENTS.get());
            if (contents != null) {
                seed.set(AutarkiaComponents.SEED_CONTENTS.get(), contents);
            }
            throwSeed(x, r.at(400, 12, 250), -90, seed);
        });
        t.repeat(60, x -> {
        });
        t.check("05 W21 fail: with outposts 6/6 the thrown seed drops as an item with a message and no block in its footprint changes", () -> {
            outposts[1] = TestM9.colony().outposts.size();
            return outposts[0] == 6 && outposts[1] == 6 && "message.autarkia.outpost.max".equals(SeedLanding.lastFailure()) && drops(t, r) == 1
                && changed(t, r, before[0]) == 0;
        }, () -> "outposts " + outposts[0] + " -> " + TestM9.colony().outposts.size() + " (outpost.max 6), reason " + SeedLanding.lastFailure()
            + ", seed items at the landing " + drops(t, r) + ", changed blocks " + changed(t, r, before[0]));
    }

    private static int drops(SelfTest t, TestM9.Run r) {
        int n = 0;
        for (ItemEntity e : t.level.getEntitiesOfClass(ItemEntity.class, new AABB(r.at(400, 0, 250)).inflate(4))) {
            n += e.getItem().is(AutarkiaItems.AUTARKY_SEED.get()) ? e.getItem().getCount() : 0;
        }
        return n;
    }

    private static BlockState[] snapshot(SelfTest t, TestM9.Run r) {
        List<BlockState> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(r.at(375, -1, 225), r.at(425, 14, 275))) {
            out.add(t.level.getBlockState(p));
        }
        return out.toArray(new BlockState[0]);
    }

    private static int changed(SelfTest t, TestM9.Run r, BlockState @Nullable [] before) {
        if (before == null) {
            return -1;
        }
        BlockState[] now = snapshot(t, r);
        int n = 0;
        for (int i = 0; i < now.length; i++) {
            n += now[i] == before[i] ? 0 : 1;
        }
        return n;
    }
}

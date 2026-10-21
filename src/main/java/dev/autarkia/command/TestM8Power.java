package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.CryoNodeBlockEntity;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GridNetwork;
import dev.autarkia.energy.PlasmaGeneratorBlockEntity;
import dev.autarkia.energy.SnappedCableEntity;
import dev.autarkia.energy.Span;
import dev.autarkia.energy.SuperconductingCoilBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The M8 power part of {@code /autarkia test m8} on the region x 50 to 130, z 0 to 60 (stone floor at y -1, air
 * above). 02 #16 (W20): a superconductor span between two cryo nodes loses the coolant at one end; T reaches 0.3 after
 * 150 ticks, 0.7 after 350 and quenches after 500: no block damage, 2 snapped ends, the network splits, failure event
 * {@code quench}, {@code power/quench} for the unconnected test player who tended the node. 02 #17: a charged coil
 * without coolant halves its I/O at T 0.5 (500 ticks) and quenches after 1,000 ticks with a blast power of
 * 2 + 4 E / Emax, E = 0, no I/O until it is cold again. Formula checks: cryo node coolant use (1 + 1 per span above
 * u 0.5) and the plasma generator (ignition 1,000,000 FE at 5,000 FE/t, 200-tick ramp to 32,768 FE/t, 78,643,200 FE per
 * cell, cold 200 ticks after the burn pauses, hot break blast, cold break none). The failure events land in a
 * temporary tier-3 colony record (not ticked) that the section dissolves at its end. About 3,100 ticks.
 */
public final class TestM8Power {
    private static final UUID OWNER = UUID.nameUUIDFromBytes("autarkia-m8-power-test".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    private TestM8Power() {
    }

    /** Add the power checks to the M8 script. */
    static void run(TestM8.Run r) {
        Colony[] fixture = new Colony[1];
        data(r);
        quenchSpan(r, fixture);
        coil(r, fixture);
        plasma(r);
        r.t().then(x -> {
            if (fixture[0] != null) {
                Colonies.dissolve(fixture[0]);
            }
            x.report("M8 power: the temporary failure-memory colony is dissolved", Colonies.at(x.level, r.at(90, 0, 28)) == null, "dissolved");
        });
    }

    private static CryoNodeBlockEntity node(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof CryoNodeBlockEntity n ? n : null;
    }

    private static SuperconductingCoilBlockEntity coilAt(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof SuperconductingCoilBlockEntity c ? c : null;
    }

    private static PlasmaGeneratorBlockEntity plasmaAt(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof PlasmaGeneratorBlockEntity g ? g : null;
    }

    private static int cells(SelfTest t, BlockPos p, int n) {
        return TestM1.insertAt(t.level, p, new ItemStack(AutarkiaItems.COOLANT_CELL.get(), n));
    }

    private static void dryOut(dev.autarkia.energy.CoolantSlots slots) {
        for (int i = 0; i < slots.size(); i++) {
            slots.put(i, ItemStack.EMPTY);
        }
    }

    private static float temp(SelfTest t, BlockPos a, BlockPos b) {
        Span s = TestM1.spanBetween(t, a, b);
        return s == null ? -1 : s.temp;
    }

    private static boolean hasQuench(SelfTest t) {
        ServerPlayer p = TestM6Logistics.courier(t);
        AdvancementHolder h = t.level.getServer().getAdvancements().get(Autarkia.id("power/quench"));
        return h != null && p.getAdvancements().getOrStartProgress(h).isDone();
    }

    private static boolean remembers(Colony c, String key) {
        return c != null && c.memories.stream().anyMatch(m -> m.type().equals("quench") && m.key().equals(key));
    }

    private static void data(TestM8.Run r) {
        SelfTest t = r.t();
        t.check("02 data: the 4 M8 power recipes and power/quench load, superconductor spans drop superconductor_spool, the cryo node is 3 high with "
            + "4 superconductor spans attached at +0.3 on its top block", () -> {
            var map = t.level.recipeAccess().recipeMap();
            for (String id : List.of("cryo_node", "superconductor_spool", "superconducting_coil", "plasma_generator")) {
                if (map.byKey(ResourceKey.create(Registries.RECIPE, Autarkia.id(id))) == null) {
                    Autarkia.LOG.info("[TEST m8] missing recipe {}", id);
                    return false;
                }
            }
            var n = AutarkiaBlocks.CRYO_NODE.get();
            Vec3 attach = n.attachPoint(BlockPos.ZERO, n.defaultBlockState());
            return Grid.cableItem(CableTier.SUPERCONDUCTOR) == AutarkiaItems.SUPERCONDUCTOR_SPOOL.get() && n.height() == 3 && n.maxSpans() == 4
                && n.tier() == CableTier.SUPERCONDUCTOR && Math.abs(attach.y - 2.8) < 1e-9
                && t.level.getServer().getAdvancements().get(Autarkia.id("power/quench")) != null;
        }, () -> "cable item " + Grid.cableItem(CableTier.SUPERCONDUCTOR) + ", node " + AutarkiaBlocks.CRYO_NODE.get().height() + " high");
    }

    /** Build the colony record that receives the failure events (domain x 58 to 122, z -4 to 60). */
    private static Colony fixtureColony(SelfTest t, BlockPos core) {
        Colony c = Colony.create(OWNER, "M8 power test", t.level.dimension(), core);
        c.tier = 3;
        Colonies.registry().add(c);
        Colonies.rebuildIndex();
        Colonies.changed();
        return c;
    }

    private static void quenchSpan(TestM8.Run r, Colony[] fixture) {
        SelfTest t = r.t();
        BlockPos c = r.at(54, 0, 10);
        BlockPos a = r.at(70, 0, 10);
        BlockPos b = r.at(90, 0, 10);
        List<BlockPos> glass = List.of(r.at(78, 2, 8), r.at(83, 2, 13), r.at(80, 0, 8), r.at(81, 4, 10));
        Vec3 mid = Vec3.atLowerCornerOf(r.at(80, 0, 10)).add(0.5, 2.8, 0.5);
        long[] m = {-1, -1, -1, -1, -1};
        String key = Math.min(a.asLong(), b.asLong()) + "-" + Math.max(a.asLong(), b.asLong());
        t.then(x -> {
            AdvancementHolder h = x.level.getServer().getAdvancements().get(Autarkia.id("power/quench"));
            if (h != null) {
                TestM6Logistics.courier(x).getAdvancements().revoke(h, "done");
            }
            boolean ok = true;
            for (BlockPos p : List.of(c, a, b)) {
                ok &= TestM1.placeBig(x, p, AutarkiaBlocks.CRYO_NODE.get(), Direction.NORTH);
                ok &= cells(x, p, 1) == 1;
            }
            Grid.SpanResult s1 = Grid.addSpan(x.level, c, a, CableTier.SUPERCONDUCTOR);
            Grid.SpanResult s2 = Grid.addSpan(x.level, a, b, CableTier.SUPERCONDUCTOR);
            glass.forEach(p -> TestM1.place(x, p, Blocks.GLASS.defaultBlockState()));
            fixture[0] = fixtureColony(x, r.at(90, 0, 28));
            m[4] = Grid.of(x.level).quenchesTotal;
            x.report("02#16 rig: cryo nodes C, A, B with one coolant cell each, superconductor spans C-A (16) and A-B (20), 4 glass witnesses by "
                + "the A-B midpoint", ok && s1 == Grid.SpanResult.OK && s2 == Grid.SpanResult.OK, "placed " + ok + ", " + s1 + ", " + s2);
        });
        t.waitTicks(20);
        t.check("02#16 C, A and B form one superconductor network, every node is cold, span A-B is at T 0", () -> {
            GridNetwork na = Grid.of(t.level).network(a.asLong());
            return na != null && na.tier == CableTier.SUPERCONDUCTOR && na == Grid.of(t.level).network(b.asLong()) && na == Grid.of(t.level).network(c.asLong())
                && node(t, a).cold() && node(t, b).cold() && node(t, c).cold() && temp(t, a, b) == 0f;
        }, () -> "T " + temp(t, a, b));
        t.then(x -> {
            ServerPlayer courier = TestM6Logistics.courier(x);
            node(x, b).createMenu(0, courier.getInventory(), courier);
            dryOut(node(x, b).coolant());
            m[0] = x.level.getGameTime();
        });
        t.waitUntil("02#16 without coolant at B the span warms to T 0.3 (frost gone)", 220, () -> temp(t, a, b) >= 0.3f, x -> { },
            () -> "T " + temp(t, a, b));
        t.then(x -> m[1] = x.level.getGameTime() - m[0]);
        t.waitUntil("02#16 the span warms on to T 0.7 (steam to hiss)", 260, () -> temp(t, a, b) >= 0.7f, x -> { }, () -> "T " + temp(t, a, b));
        t.then(x -> m[2] = x.level.getGameTime() - m[0]);
        t.waitUntil("02#16 the span quenches", 220, () -> TestM1.spanBetween(t, a, b) == null, x -> { }, () -> "T " + temp(t, a, b));
        t.then(x -> m[3] = x.level.getGameTime() - m[0]);
        t.check("02#16 T 0.3 after about 150 ticks, 0.7 after about 350, quench after about 500 (25 s; each +-2)", () -> Math.abs(m[1] - 150) <= 2
            && Math.abs(m[2] - 350) <= 2 && Math.abs(m[3] - 500) <= 2, () -> "0.3 at " + m[1] + ", 0.7 at " + m[2] + ", quench at " + m[3]);
        t.check("02#16 the 2.5 blast did no block damage (quenchBreaksBlocks false): the 4 glass witnesses stand", () -> glass.stream()
            .allMatch(p -> t.level.getBlockState(p).is(Blocks.GLASS)) && Grid.of(t.level).quenchesTotal == m[4] + 1, () -> "quenches "
            + (Grid.of(t.level).quenchesTotal - m[4]));
        t.check("02#16 two superconductor snapped cable ends whip at the nodes", () -> t.level.getEntitiesOfClass(SnappedCableEntity.class,
            new AABB(mid, mid).inflate(16), s -> s.tier() == CableTier.SUPERCONDUCTOR).size() == 2, () -> t.level.getEntitiesOfClass(
            SnappedCableEntity.class, new AABB(mid, mid).inflate(16)).size() + " ends");
        t.check("02#16 failure event quench (key: the two node positions) reached the colony memory", () -> remembers(fixture[0], key),
            () -> fixture[0] == null ? "no colony" : fixture[0].memories.toString());
        t.check("02#16 advancement power/quench went to the unconnected player who tended node B", () -> hasQuench(t), () -> "not done");
        t.waitTicks(5);
        t.check("02#16 the network split: C-A is still one network, B has none", () -> {
            GridNetwork na = Grid.of(t.level).network(a.asLong());
            return na != null && na == Grid.of(t.level).network(c.asLong()) && Grid.of(t.level).network(b.asLong()) == null;
        }, () -> "A " + Grid.of(t.level).network(a.asLong()) + ", B " + Grid.of(t.level).network(b.asLong()));
        t.waitTicks(CableTier.SUPERCONDUCTOR.snapLifetime + 10);
        t.check("02#16 after 60 ticks the ends are gone and drop no superconductor_spool", () -> t.level.getEntitiesOfClass(SnappedCableEntity.class,
            new AABB(mid, mid).inflate(16)).isEmpty() && TestM1.countItems(t, BlockPos.containing(mid), 24, AutarkiaItems.SUPERCONDUCTOR_SPOOL.get()) == 0,
            () -> TestM1.countItems(t, BlockPos.containing(mid), 24, AutarkiaItems.SUPERCONDUCTOR_SPOOL.get()) + " spools");
    }

    /**
     * Return the two cryo nodes of the coil rig, the one with the lower {@code BlockPos.asLong} first: a network without
     * generators roots its tree there, so pushing energy into that node puts the whole flow on the span.
     */
    private static BlockPos[] coilEnds(TestM8.Run r) {
        BlockPos p = r.at(96, 0, 36);
        BlockPos q = r.at(112, 0, 36);
        return p.asLong() < q.asLong() ? new BlockPos[] {p, q} : new BlockPos[] {q, p};
    }

    private static void coil(TestM8.Run r, Colony[] fixture) {
        SelfTest t = r.t();
        BlockPos[] e = coilEnds(r);
        BlockPos src = e[0];
        BlockPos far = e[1];
        BlockPos coil = far.getX() > src.getX() ? far.east() : far.west(2);
        long[] m = new long[8];
        long[] stored = {0, 0};
        t.then(x -> {
            boolean ok = TestM1.placeBig(x, src, AutarkiaBlocks.CRYO_NODE.get(), Direction.NORTH)
                && TestM1.placeBig(x, far, AutarkiaBlocks.CRYO_NODE.get(), Direction.NORTH)
                && TestM1.placeBig(x, coil, AutarkiaBlocks.SUPERCONDUCTING_COIL.get(), Direction.NORTH);
            ok &= cells(x, src, 1) == 1 && cells(x, far, 1) == 1 && cells(x, coil, 1) == 1;
            Grid.SpanResult s = Grid.addSpan(x.level, src, far, CableTier.SUPERCONDUCTOR);
            x.report("coil rig: two cryo nodes 16 apart with one cell each, a superconducting coil with one cell tapped by the far node", ok
                && s == Grid.SpanResult.OK, "placed " + ok + ", " + s);
        });
        t.waitTicks(20);
        t.check("coil: cold at T 0, I/O limit 65,536 FE/t, attached to the superconductor network", () -> {
            SuperconductingCoilBlockEntity c = coilAt(t, coil);
            GridNetwork n = Grid.of(t.level).network(far.asLong());
            return c != null && c.cold() && c.temperature() == 0 && c.io() == 65536 && n != null && n.members.stream().anyMatch(a -> a.member == c);
        }, () -> coilAt(t, coil) == null ? "missing" : "T " + coilAt(t, coil).temperature() + ", io " + coilAt(t, coil).io());
        t.then(x -> {
            m[0] = node(x, src).coolant().units();
            m[1] = x.level.getGameTime();
        });
        t.waitTicks(101);
        t.check("cryo node: an idle node uses 1 coolant unit per tick", () -> m[0] - node(t, src).coolant().units() == t.level.getGameTime() - m[1],
            () -> (m[0] - node(t, src).coolant().units()) + " units in " + (t.level.getGameTime() - m[1]) + " ticks");
        t.repeat(20, x -> TestM1.pushEnergy(x, src, 70000));
        t.then(x -> {
            TestM1.pushEnergy(x, src, 70000);
            m[0] = node(x, src).coolant().units();
            m[2] = node(x, far).coolant().units();
            m[1] = x.level.getGameTime();
        });
        t.repeat(100, x -> TestM1.pushEnergy(x, src, 70000));
        t.check("cryo node: with its span at u 1.0 (65,536 FE/t into the coil) each node uses 1 + 1 = 2 units per tick", () -> {
            long dt = t.level.getGameTime() - m[1];
            Span s = TestM1.spanBetween(t, src, far);
            return s != null && s.load > 0.5f && m[0] - node(t, src).coolant().units() == 2 * dt && m[2] - node(t, far).coolant().units() == 2 * dt
                && node(t, src).lastUse() == 2;
        }, () -> String.format(Locale.ROOT, "u %.3f, src %d, far %d units in %d ticks", TestM1.spanBetween(t, src, far).load,
            m[0] - node(t, src).coolant().units(), m[2] - node(t, far).coolant().units(), t.level.getGameTime() - m[1]));
        t.check("coil: below T 0.5 it charges at the full 65,536 FE/t", () -> coilAt(t, coil).lastIn() == 65536, () -> "in " + coilAt(t, coil).lastIn());
        t.then(x -> {
            dryOut(coilAt(x, coil).coolant());
            m[3] = x.level.getGameTime();
            TestM1.pushEnergy(x, src, 70000);
        });
        t.waitUntil("02#17 the dry coil warms to T 0.5", 560, () -> coilAt(t, coil).temperature() >= 0.5, x -> TestM1.pushEnergy(x, src, 70000),
            () -> "T " + coilAt(t, coil).temperature());
        t.then(x -> m[4] = x.level.getGameTime() - m[3]);
        t.repeat(5, x -> TestM1.pushEnergy(x, src, 70000));
        t.check("coil: from T 0.5 (after 500 dry ticks, +0.001 per tick) its I/O is halved to 32,768 FE/t", () -> m[4] == 500
            && coilAt(t, coil).io() == 32768 && coilAt(t, coil).lastIn() == 32768, () -> "T 0.5 after " + m[4] + " ticks, in "
            + coilAt(t, coil).lastIn());
        t.waitUntil("02#17 the charged dry coil quenches", 560, () -> coilAt(t, coil).quenched(), x -> {
            stored[0] = stored[1];
            stored[1] = coilAt(x, coil).buffer().stored();
            TestM1.pushEnergy(x, src, 70000);
        }, () -> "T " + coilAt(t, coil).temperature());
        t.then(x -> m[5] = x.level.getGameTime() - m[3]);
        t.check("02#17 quench after 1,000 dry ticks with blast power 2 + 4 E / Emax in 2..6, then E = 0 and the block stays quenched", () -> {
            SuperconductingCoilBlockEntity c = coilAt(t, coil);
            double expect = 2 + 4.0 * stored[0] / SuperconductingCoilBlockEntity.CAPACITY;
            return m[5] == 1000 && c != null && Math.abs(c.lastBlastPower() - expect) < 1e-3 && c.lastBlastPower() >= 2 && c.lastBlastPower() <= 6
                && c.buffer().stored() == 0 && c.quenched() && t.level.getBlockState(coil).is(AutarkiaBlocks.SUPERCONDUCTING_COIL.get());
        }, () -> String.format(Locale.ROOT, "after %d ticks, power %.4f (E before %d), E %d", m[5], coilAt(t, coil).lastBlastPower(), stored[0],
            coilAt(t, coil).buffer().stored()));
        t.check("02#17 the coil quench sent failure event quench (key: the coil position)", () -> remembers(fixture[0], Long.toString(coil.asLong())),
            () -> fixture[0] == null ? "no colony" : fixture[0].memories.toString());
        t.repeat(5, x -> TestM1.pushEnergy(x, src, 70000));
        t.check("02#17 a quenched coil takes no energy", () -> coilAt(t, coil).lastIn() == 0 && coilAt(t, coil).io() == 0,
            () -> "in " + coilAt(t, coil).lastIn());
        t.then(x -> {
            cells(x, coil, 1);
            m[6] = x.level.getGameTime();
        });
        t.waitUntil("02#17 with coolant again the coil cools to T 0", 260, () -> !coilAt(t, coil).quenched(), x -> { },
            () -> "T " + coilAt(t, coil).temperature());
        t.then(x -> m[7] = x.level.getGameTime() - m[6]);
        t.repeat(5, x -> TestM1.pushEnergy(x, src, 70000));
        t.check("02#17 it leaves the quenched state after 200 ticks (-0.005 per tick) and charges at 65,536 FE/t again", () -> m[7] == 200
            && coilAt(t, coil).lastIn() == 65536, () -> "unquenched after " + m[7] + " ticks, in " + coilAt(t, coil).lastIn());
    }

    private static LivingEntity pig(SelfTest t, BlockPos at) {
        t.command("summon minecraft:pig " + at.getX() + " " + at.getY() + " " + at.getZ() + " {NoAI:1b}");
        List<LivingEntity> list = t.level.getEntitiesOfClass(LivingEntity.class, new AABB(at).inflate(1.5), le -> le.getType() == EntityTypes.PIG);
        return list.isEmpty() ? null : list.get(0);
    }

    private static void plasma(TestM8.Run r) {
        SelfTest t = r.t();
        BlockPos g = r.at(56, 0, 50);
        BlockPos h = r.at(72, 0, 50);
        BlockPos p1 = r.at(74, 0, 50);
        BlockPos p2 = r.at(100, 0, 52);
        long[] m = new long[10];
        int[] sample = {-1, -1};
        LivingEntity[] pigs = new LivingEntity[2];
        t.then(x -> {
            boolean ok = TestM1.placeBig(x, g, AutarkiaBlocks.CRYO_NODE.get(), Direction.NORTH)
                && TestM1.placeBig(x, h, AutarkiaBlocks.CRYO_NODE.get(), Direction.NORTH)
                && TestM1.placeBig(x, p1, AutarkiaBlocks.PLASMA_GENERATOR.get(), Direction.NORTH)
                && TestM1.placeBig(x, p2, AutarkiaBlocks.PLASMA_GENERATOR.get(), Direction.NORTH);
            ok &= cells(x, g, 1) == 1 && cells(x, h, 1) == 1;
            ok &= TestM1.insertAt(x.level, p1, new ItemStack(AutarkiaItems.LATTICE_FUEL_CELL.get(), 2)) == 2;
            Grid.SpanResult s = Grid.addSpan(x.level, g, h, CableTier.SUPERCONDUCTOR);
            x.report("plasma rig: two cryo nodes, a plasma generator with 2 lattice fuel cells tapped by the second node, a second cold generator",
                ok && s == Grid.SpanResult.OK, "placed " + ok + ", " + s);
        });
        t.waitTicks(20);
        t.check("plasma: cold with output 0 and attached to the network; the ignition button is refused on the second generator (no fuel)", () -> {
            PlasmaGeneratorBlockEntity p = plasmaAt(t, p1);
            GridNetwork n = Grid.of(t.level).network(h.asLong());
            return p != null && p.state() == PlasmaGeneratorBlockEntity.State.COLD && p.output() == 0 && n != null
                && n.members.stream().anyMatch(a -> a.member == p) && !plasmaAt(t, p2).menuButton(TestM6Logistics.courier(t), 0);
        }, () -> plasmaAt(t, p1) == null ? "missing" : plasmaAt(t, p1).state().toString());
        t.then(x -> {
            boolean pressed = plasmaAt(x, p1).menuButton(TestM6Logistics.courier(x), 0);
            m[0] = x.level.getGameTime();
            x.report("plasma: the ignition button starts IGNITING", pressed && plasmaAt(x, p1).state() == PlasmaGeneratorBlockEntity.State.IGNITING,
                plasmaAt(x, p1).state().toString());
        });
        t.waitUntil("plasma: the ignition draws its 1,000,000 FE from the network", 400,
            () -> plasmaAt(t, p1).state() == PlasmaGeneratorBlockEntity.State.RAMPING, x -> {
                TestM1.pushEnergy(x, g, 6000);
                int drawn = plasmaAt(x, p1).ignitionDrawn();
                if (drawn > sample[0] && sample[0] >= 0) {
                    m[1] = Math.max(m[1], drawn - sample[0]);
                }
                sample[0] = drawn;
                m[2] = Math.max(m[2], drawn);
            }, () -> "drawn " + plasmaAt(t, p1).ignitionDrawn());
        t.then(x -> m[3] = x.level.getGameTime() - m[0]);
        t.check("plasma: ignition took 1,000,000 FE at max 5,000 FE/t (200 ticks, the ramp starts the tick after)", () -> m[1] == 5000
            && m[2] == PlasmaGeneratorBlockEntity.IGNITION_FE && m[3] >= 200 && m[3] <= 202, () -> "max " + m[1] + " FE/t, total " + m[2] + ", "
            + m[3] + " ticks");
        t.waitUntil("plasma: the output ramps to RUNNING", 260, () -> plasmaAt(t, p1).state() == PlasmaGeneratorBlockEntity.State.RUNNING, x -> {
            PlasmaGeneratorBlockEntity p = plasmaAt(x, p1);
            p.buffer().drain(p.buffer().stored());
            if (p.ramp() == 100) {
                sample[1] = p.output();
            }
        }, () -> "ramp " + plasmaAt(t, p1).ramp());
        t.then(x -> m[4] = x.level.getGameTime() - m[0] - m[3]);
        t.check("plasma: linear ramp over 200 burning ticks (16,384 FE/t at step 100) to 32,768 FE/t; one fuel cell loaded", () -> {
            PlasmaGeneratorBlockEntity p = plasmaAt(t, p1);
            return sample[1] == 16384 && p.output() == 32768 && m[4] == 200 && p.fuelCells() == 1;
        }, () -> "step 100 " + sample[1] + ", out " + plasmaAt(t, p1).output() + ", ramp ticks " + m[4] + ", cells " + plasmaAt(t, p1).fuelCells());
        t.then(x -> {
            m[5] = plasmaAt(x, p1).fuelLeft();
            m[6] = x.level.getGameTime();
            PlasmaGeneratorBlockEntity p = plasmaAt(x, p1);
            p.buffer().drain(p.buffer().stored());
        });
        t.repeat(100, x -> {
            PlasmaGeneratorBlockEntity p = plasmaAt(x, p1);
            p.buffer().drain(p.buffer().stored());
        });
        t.check("plasma: running burns 32,768 FE of fuel per tick; a cell is 78,643,200 FE = 2,400 ticks at full output", () -> {
            long dt = t.level.getGameTime() - m[6];
            return m[5] - plasmaAt(t, p1).fuelLeft() == 32768L * dt && PlasmaGeneratorBlockEntity.CELL_FE == 78_643_200
                && PlasmaGeneratorBlockEntity.CELL_FE / PlasmaGeneratorBlockEntity.FULL_OUTPUT == 2400;
        }, () -> (m[5] - plasmaAt(t, p1).fuelLeft()) + " FE in " + (t.level.getGameTime() - m[6]) + " ticks");
        t.waitUntil("plasma: with its 1,000,000 FE buffer full the burn pauses (HOT_IDLE)", 60, () -> plasmaAt(t, p1).state()
            == PlasmaGeneratorBlockEntity.State.HOT_IDLE, x -> { }, () -> plasmaAt(t, p1).state().toString());
        t.then(x -> m[7] = x.level.getGameTime());
        t.waitUntil("plasma: the paused plasma goes cold", 260, () -> plasmaAt(t, p1).state() == PlasmaGeneratorBlockEntity.State.COLD, x -> { },
            () -> plasmaAt(t, p1).state().toString());
        t.check("plasma: it stayed hot 200 ticks, then COLD (needs a new ignition)", () -> t.level.getGameTime() - m[7] == 200
            && plasmaAt(t, p1).ramp() == 0 && plasmaAt(t, p1).canIgnite(), () -> "cold after " + (t.level.getGameTime() - m[7]) + " ticks");
        t.then(x -> {
            pigs[1] = pig(x, p2.south(3));
            x.level.destroyBlock(p2, false);
        });
        t.waitTicks(10);
        t.check("plasma: breaking a cold generator makes no explosion (the pig 3 blocks away is unhurt)", () -> pigs[1] != null && !pigs[1].isRemoved()
            && pigs[1].getHealth() == pigs[1].getMaxHealth() && plasmaAt(t, p2) == null, () -> pigs[1] == null ? "no pig" : "pig " + pigs[1].getHealth());
        t.then(x -> {
            PlasmaGeneratorBlockEntity p = plasmaAt(x, p1);
            p.buffer().drain(p.buffer().stored());
            p.menuButton(TestM6Logistics.courier(x), 0);
        });
        t.waitUntil("plasma: a second ignition makes it hot again", 400, () -> plasmaAt(t, p1).hot(), x -> TestM1.pushEnergy(x, g, 6000),
            () -> plasmaAt(t, p1).state().toString());
        t.waitTicks(5);
        t.then(x -> {
            pigs[0] = pig(x, p1.south(4));
            x.level.destroyBlock(p1, false);
        });
        t.waitTicks(10);
        t.check("plasma: breaking it hot makes a 4.0 explosion without block damage (the pig 4 blocks away is hit, the nodes and floor stand)",
            () -> pigs[0] != null && (pigs[0].isRemoved() || pigs[0].getHealth() < pigs[0].getMaxHealth()) && plasmaAt(t, p1) == null
                && node(t, h) != null && !t.level.getBlockState(p1.below()).isAir(), () -> pigs[0] == null ? "no pig" : "pig " + pigs[0].getHealth());
        t.then(x -> x.level.getEntitiesOfClass(LivingEntity.class, new AABB(Vec3.atCenterOf(r.at(50, -1, 40)), Vec3.atCenterOf(r.at(130, 4, 60))),
            le -> le.getType() == EntityTypes.PIG).forEach(le -> le.discard()));
    }
}

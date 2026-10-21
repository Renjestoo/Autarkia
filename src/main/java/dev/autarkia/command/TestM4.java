package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.BoilerBlockEntity;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.CapacitorBankBlockEntity;
import dev.autarkia.energy.Catenary;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.IndustrialLampBlockEntity;
import dev.autarkia.energy.LightningMastBlockEntity;
import dev.autarkia.energy.SnappedCableEntity;
import dev.autarkia.energy.Span;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /autarkia test m4}: 02 #6 and #7 (W5, heat, snap, lamp ripple), #9 and #10 (W7, lightning pulse and
 * arrester), #11 (W8, mast), #12 and #13 (W9, boiler burst and a loaded boiler) on a platform at y 120 or higher (open
 * sky for the mast), then the T2 colony checks of {@link TestM4Colony}. The FE sources are pushes into a pole's
 * insert-only face, as a foreign generator does.
 */
public final class TestM4 {
    private TestM4() {
    }

    /** One M4 run: the test and the platform base. */
    record Run(SelfTest t, BlockPos base) {
        BlockPos at(int x, int y, int z) {
            return base.offset(x, y, z);
        }
    }

    /** Build the platform and run the M4 script. */
    public static int run(CommandSourceStack src) {
        SelfTest t = SelfTest.create("m4", src);
        BlockPos o = t.origin;
        Run r = new Run(t, new BlockPos(o.getX(), Math.max(o.getY() + 8, 120), o.getZ()));
        t.then(x -> reset(r));
        data(r);
        heatAndSnap(r);
        ripple(r);
        settle(r);
        lightning(r);
        mast(r);
        burst(r);
        loaded(r);
        TestM4Colony.run(r);
        return t.start();
    }

    private static void box(Run r, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(r.at(x0, y0, z0), r.at(x1, y1, z1))) {
            r.t().level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private static void reset(Run r) {
        SelfTest t = r.t();
        t.command("forceload add " + (r.base().getX() - 8) + " " + (r.base().getZ() - 8) + " " + (r.base().getX() + 88) + " " + (r.base().getZ() + 124));
        t.command("weather clear");
        AABB area = new AABB(Vec3.atCenterOf(r.at(-8, -6, -8)), Vec3.atCenterOf(r.at(88, 24, 124)));
        t.level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area, e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(e -> e.discard());
        box(r, -2, 0, -2, 84, 16, 58, Blocks.AIR.defaultBlockState());
        box(r, -2, -1, -2, 84, -1, 58, Blocks.STONE.defaultBlockState());
        Autarkia.LOG.info("[TEST m4] platform at {}", r.base().toShortString());
    }

    static void pole(SelfTest t, BlockPos p) {
        TestM1.placeBig(t, p, AutarkiaBlocks.WOODEN_POLE.get(), Direction.NORTH);
    }

    static void bank(SelfTest t, BlockPos p) {
        TestM1.place(t, p, AutarkiaBlocks.CAPACITOR_BANK.get().defaultBlockState().setValue(dev.autarkia.registry.MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
    }

    static CapacitorBankBlockEntity bankAt(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof CapacitorBankBlockEntity b ? b : null;
    }

    private static void drain(SelfTest t, List<BlockPos> banks) {
        for (BlockPos p : banks) {
            CapacitorBankBlockEntity b = bankAt(t, p);
            if (b != null) {
                b.buffer().drain(b.buffer().stored());
            }
        }
    }

    private static void data(Run r) {
        SelfTest t = r.t();
        t.check("registries: the 6 T2 templates, 3 T2 plans and 4 T2 power advancements load", () -> {
            for (String a : List.of("power/red_hot", "power/surge", "power/storm_harvest", "power/boiler_burst")) {
                if (t.level.getServer().getAdvancements().get(Autarkia.id(a)) == null) {
                    return false;
                }
            }
            var tpl = dev.autarkia.colony.ColonyPlans.templates();
            return tpl.entrySet().stream().filter(e -> e.getValue().tier() == 2).map(java.util.Map.Entry::getKey).collect(java.util.stream.Collectors.toSet())
                .containsAll(java.util.Set.of("waterwheel", "boiler_engine", "span_upgrade", "lightning_mast", "lightning_arrester", "pressure_valve"))
                && dev.autarkia.colony.ColonyPlans.plans().keySet().stream().map(i -> i.getPath()).collect(java.util.stream.Collectors.toSet())
                    .containsAll(java.util.Set.of("waterwheel", "boiler_engine", "lightning_mast"));
        }, () -> dev.autarkia.colony.ColonyPlans.templates().keySet().toString());
        t.check("recipes: the 14 M4 recipes exist (spool from the wire drawer)", () -> {
            var map = t.level.recipeAccess().recipeMap();
            for (String id : List.of("wooden_pole", "drawing/copper_wire_spool", "industrial_lamp", "waterwheel", "boiler", "pressure_valve", "steam_engine",
                "lightning_mast", "lightning_arrester", "capacitor_bank", "catwalk", "catwalk_stairs", "hazard_block", "core_upgrade_t2")) {
                if (map.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Autarkia.id(id))) == null) {
                    return false;
                }
            }
            return true;
        }, () -> "14 recipes");
    }

    private static void heatAndSnap(Run r) {
        SelfTest t = r.t();
        BlockPos src = r.at(2, 0, 2);
        BlockPos p1 = r.at(18, 0, 2);
        List<BlockPos> banks = List.of(r.at(19, 0, 2), r.at(18, 0, 3), r.at(18, 0, 1));
        long[] marks = {-1, -1, -1, 0};
        double[] sag = new double[2];
        t.then(x -> {
            pole(x, src);
            pole(x, p1);
            banks.forEach(b -> bank(x, b));
            TestM1.place(x, r.at(17, 0, 2), AutarkiaBlocks.INDUSTRIAL_LAMP.get().defaultBlockState());
            Grid.SpanResult s = Grid.addSpan(x.level, src, p1, CableTier.COPPER);
            Span sp = TestM1.spanBetween(x, src, p1);
            x.report("02#6 copper span of 16 blocks between two poles", s == Grid.SpanResult.OK && sp != null && Math.abs(sp.length - 16) < 0.01,
                s + (sp == null ? "" : ", length " + sp.length));
            sag[0] = sp == null ? 0 : Catenary.sagDepth(CableTier.COPPER, sp.length, 0, 0);
            marks[3] = x.level.getGameTime();
        });
        t.waitUntil("02#6 at 512 FE/t (u = 2) the span heats and snaps", 400, () -> TestM1.spanBetween(t, src, p1) == null, x -> {
            TestM1.pushEnergy(x, src, 512);
            Span sp = TestM1.spanBetween(x, src, p1);
            if (sp != null) {
                long dt = x.level.getGameTime() - marks[3];
                if (marks[0] < 0 && sp.heat >= 0.4f) {
                    marks[0] = dt;
                }
                if (marks[1] < 0 && sp.heat >= 0.7f) {
                    marks[1] = dt;
                    sag[1] = Catenary.sagDepth(CableTier.COPPER, sp.length, sp.heat, sp.load);
                }
                marks[2] = dt;
            }
        });
        t.check("02#6 the snap comes at about 160 ticks (orange from h 0.4, red from h 0.7 on the way)",
            () -> marks[0] > 0 && marks[1] > marks[0] && marks[2] >= 130 && marks[2] <= 200, () -> "h 0.4 at " + marks[0] + ", h 0.7 at " + marks[1] + ", snap at " + marks[2]);
        t.check("02#6 the sag grows with heat", () -> sag[1] > sag[0] * 1.05, () -> String.format(java.util.Locale.ROOT, "sag %.3f -> %.3f", sag[0], sag[1]));
        t.check("02#6 two copper snapped cable ends arc", () -> t.level.getEntitiesOfClass(SnappedCableEntity.class, new AABB(Vec3.atCenterOf(src), Vec3.atCenterOf(p1)).inflate(6),
            e -> e.tier() == CableTier.COPPER).size() == 2, () -> t.level.getEntitiesOfClass(SnappedCableEntity.class, new AABB(Vec3.atCenterOf(src), Vec3.atCenterOf(p1)).inflate(6)).size() + " ends");
    }

    private static void ripple(Run r) {
        SelfTest t = r.t();
        BlockPos q0 = r.at(2, 0, 10);
        List<BlockPos> chain = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            chain.add(r.at(2 + 4 * i, 0, 10));
        }
        long[] dark = new long[6];
        long[] snapAt = new long[1];
        t.then(x -> {
            pole(x, q0);
            BlockPos prev = q0;
            boolean ok = true;
            for (BlockPos q : chain) {
                pole(x, q);
                TestM1.place(x, q.south(), AutarkiaBlocks.INDUSTRIAL_LAMP.get().defaultBlockState());
                ok &= Grid.addSpan(x.level, prev, q, CableTier.COPPER) == Grid.SpanResult.OK;
                prev = q;
            }
            java.util.Arrays.fill(dark, -1);
            x.report("02#6 ripple rig: a pole chain with 6 lamps, one per pole", ok, "7 poles, 6 spans");
        });
        t.repeat(80, x -> TestM1.pushEnergy(x, q0, 20));
        t.check("02#6 the 6 lamps are lit", () -> chain.stream().allMatch(q -> t.level.getBlockEntity(q.south()) instanceof IndustrialLampBlockEntity l && l.lit()),
            () -> chain.stream().map(q -> t.level.getBlockEntity(q.south()) instanceof IndustrialLampBlockEntity l && l.lit() ? "on" : "off").toList().toString());
        t.then(x -> {
            Span s = TestM1.spanBetween(x, q0, chain.get(0));
            if (s != null) {
                s.heat = 1.5f;
            }
            snapAt[0] = x.level.getGameTime();
        });
        t.waitUntil("02#6 after the snap every lamp goes dark", 60, () -> java.util.Arrays.stream(dark).allMatch(d -> d >= 0), x -> {
            TestM1.pushEnergy(x, q0, 20);
            for (int i = 0; i < chain.size(); i++) {
                if (dark[i] < 0 && x.level.getBlockEntity(chain.get(i).south()) instanceof IndustrialLampBlockEntity l && !l.lit()) {
                    dark[i] = x.level.getGameTime() - snapAt[0];
                }
            }
        });
        t.check("02#6 lamps go out one by one, 3 ticks per hop from the break", () -> {
            for (int i = 1; i < dark.length; i++) {
                if (dark[i] - dark[i - 1] < 2 || dark[i] - dark[i - 1] > 4) {
                    return false;
                }
            }
            return true;
        }, () -> java.util.Arrays.toString(dark));
    }

    private static void settle(Run r) {
        SelfTest t = r.t();
        BlockPos a = r.at(2, 0, 18);
        BlockPos b = r.at(18, 0, 18);
        List<BlockPos> banks = List.of(r.at(19, 0, 18), r.at(18, 0, 19), r.at(18, 0, 17));
        t.then(x -> {
            pole(x, a);
            pole(x, b);
            banks.forEach(p -> bank(x, p));
            Grid.SpanResult s = Grid.addSpan(x.level, a, b, CableTier.COPPER);
            Span sp = TestM1.spanBetween(x, a, b);
            if (sp != null) {
                sp.heat = 0.3f;
            }
            x.report("02#7 rig: copper span of 16 blocks at h 0.3", s == Grid.SpanResult.OK, s.toString());
        });
        t.repeat(1200, x -> {
            TestM1.pushEnergy(x, a, 300);
            if (x.level.getGameTime() % 20 == 0) {
                drain(x, banks);
            }
        });
        t.check("02#7 at 300 FE/t (u = 1.17) the span settles near h 0.47 and does not snap", () -> {
            Span sp = TestM1.spanBetween(t, a, b);
            return sp != null && sp.heat >= 0.42f && sp.heat <= 0.48f;
        }, () -> {
            Span sp = TestM1.spanBetween(t, a, b);
            return sp == null ? "snapped" : String.format(java.util.Locale.ROOT, "h %.3f, u %.2f", sp.heat, sp.load);
        });
    }

    private static void mill(SelfTest t, BlockPos p) {
        TestM1.placeBig(t, p, AutarkiaBlocks.STAMP_MILL.get(), Direction.NORTH);
    }

    private static boolean millAt(SelfTest t, BlockPos p) {
        return t.level.getBlockState(p).is(AutarkiaBlocks.STAMP_MILL.get());
    }

    private static void lightning(Run r) {
        SelfTest t = r.t();
        BlockPos l0 = r.at(40, 2, 4);
        List<BlockPos> poles = List.of(r.at(50, 0, 4), r.at(60, 0, 4), r.at(70, 0, 4));
        List<BlockPos> mills = poles.stream().map(BlockPos::south).toList();
        BlockPos strike = r.at(40, 0, 10);
        t.then(x -> {
            box(r, 39, 0, 3, 41, 1, 5, Blocks.STONE.defaultBlockState());
            pole(x, l0);
            BlockPos prev = l0;
            boolean ok = true;
            for (BlockPos p : poles) {
                pole(x, p);
                ok &= Grid.addSpan(x.level, prev, p, CableTier.COPPER) == Grid.SpanResult.OK;
                prev = p;
            }
            mills.forEach(m -> mill(x, m));
            x.report("02#9 rig: 4 copper poles (the first on a pedestal), 3 stamp mills, no arresters", ok && mills.stream().allMatch(m -> millAt(x, m)), "spans " + ok);
            x.command("summon minecraft:lightning_bolt " + strike.getX() + " " + strike.getY() + " " + strike.getZ());
        });
        t.waitTicks(40);
        t.check("02#9 the bolt goes to the highest pole and the pulse destroys the first machine, which drops itself",
            () -> !millAt(t, mills.get(0)) && TestM1.countItems(t, mills.get(0), 4, AutarkiaItems.STAMP_MILL.get()) == 1,
            () -> "first mill " + (millAt(t, mills.get(0)) ? "intact" : "gone") + ", items " + TestM1.countItems(t, mills.get(0), 4, AutarkiaItems.STAMP_MILL.get()));
        t.check("02#9 the other machines are intact", () -> millAt(t, mills.get(1)) && millAt(t, mills.get(2)),
            () -> millAt(t, mills.get(1)) + " / " + millAt(t, mills.get(2)));
        t.then(x -> {
            TestM1.killItems(x, mills.get(0), 4);
            x.level.getEntitiesOfClass(net.minecraft.world.entity.LightningBolt.class, new AABB(l0).inflate(32)).forEach(e -> e.discard());
            mill(x, mills.get(0));
            TestM1.place(x, l0.above(3), AutarkiaBlocks.LIGHTNING_ARRESTER.get().defaultBlockState());
            x.command("summon minecraft:lightning_bolt " + strike.getX() + " " + strike.getY() + " " + strike.getZ());
        });
        t.waitTicks(40);
        t.check("02#10 with an arrester on the struck pole the pulse stops there: no machine is lost", () -> mills.stream().allMatch(m -> millAt(t, m)),
            () -> mills.stream().map(m -> millAt(t, m) ? "intact" : "gone").toList().toString());
    }

    private static void mast(Run r) {
        SelfTest t = r.t();
        BlockPos m = r.at(40, 0, 40);
        BlockPos node = r.at(46, 0, 40);
        List<BlockPos> banks = List.of(r.at(47, 0, 40), r.at(46, 0, 41), r.at(46, 0, 39));
        BlockPos strike = r.at(40, 0, 56);
        Husk[] h = new Husk[1];
        t.then(x -> {
            x.level.getEntitiesOfClass(net.minecraft.world.entity.LightningBolt.class, new AABB(m).inflate(64)).forEach(e -> e.discard());
            TestM1.placeBig(x, m, AutarkiaBlocks.LIGHTNING_MAST.get(), Direction.NORTH);
            pole(x, node);
            banks.forEach(b -> bank(x, b));
            Grid.SpanResult s = Grid.addSpan(x.level, m.above(3), Direction.UP, node, Direction.UP, CableTier.COPPER, false);
            h[0] = TestM2.husk(x, m.west());
            x.report("02#11 rig: mast with a copper span to a pole with 3 empty capacitor banks", s == Grid.SpanResult.OK, s.toString());
        });
        t.waitTicks(10);
        t.then(x -> x.command("summon minecraft:lightning_bolt " + strike.getX() + " " + strike.getY() + " " + strike.getZ()));
        t.waitTicks(5);
        t.check("02#11 the bolt goes to the mast: each bank gains 40,000 FE and no pulse runs", () -> {
            LightningMastBlockEntity be = t.level.getBlockEntity(m) instanceof LightningMastBlockEntity mm ? mm : null;
            return be != null && be.lastStored() == 120000 && banks.stream().allMatch(b -> bankAt(t, b) != null && bankAt(t, b).buffer().stored() >= 39500);
        }, () -> banks.stream().map(b -> bankAt(t, b) == null ? "missing" : String.valueOf(bankAt(t, b).buffer().stored())).toList().toString());
        t.then(x -> {
            banks.forEach(b -> bankAt(x, b).buffer().fill(CapacitorBankBlockEntity.CAPACITY));
            x.command("summon minecraft:lightning_bolt " + strike.getX() + " " + strike.getY() + " " + strike.getZ());
        });
        t.waitTicks(5);
        t.check("02#11 with full banks the strike is grounded at the mast base: nothing stored, no damage", () -> {
            LightningMastBlockEntity be = t.level.getBlockEntity(m) instanceof LightningMastBlockEntity mm ? mm : null;
            return be != null && be.lastStored() == 0 && h[0].getHealth() == h[0].getMaxHealth() && t.level.getBlockState(m).is(AutarkiaBlocks.LIGHTNING_MAST.get());
        }, () -> "husk " + h[0].getHealth());
    }

    private static BoilerBlockEntity boiler(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof BoilerBlockEntity b ? b : null;
    }

    private static void boilerRig(SelfTest t, BlockPos center, int engines) {
        TestM1.placeBig(t, center, AutarkiaBlocks.BOILER.get(), Direction.NORTH);
        for (int i = 0; i < engines; i++) {
            TestM1.placeBig(t, center.offset(i, 0, -2), AutarkiaBlocks.STEAM_ENGINE.get(), Direction.NORTH);
        }
        TestM1.place(t, center.offset(1, 3, 1), AutarkiaBlocks.PRESSURE_VALVE.get().defaultBlockState());
        for (int dz = -1; dz <= 2; dz++) {
            TestM1.place(t, center.offset(-2, 0, dz), Blocks.STONE.defaultBlockState());
        }
        TestM1.place(t, center.offset(-1, 0, -1), Blocks.STONE.defaultBlockState());
        TestM1.place(t, center.offset(-1, 0, 2), Blocks.STONE.defaultBlockState());
        TestM1.place(t, center.offset(-1, 0, 0), Blocks.WATER.defaultBlockState());
        TestM1.place(t, center.offset(-1, 0, 1), Blocks.WATER.defaultBlockState());
        BoilerBlockEntity b = boiler(t, center);
        b.inventory().put(0, new ItemStack(Items.COAL, 64));
        b.setFire(4);
    }

    private static void burst(Run r) {
        SelfTest t = r.t();
        BlockPos c = r.at(10, 0, 40);
        Husk[] h = new Husk[1];
        int[] floor = new int[1];
        int[] falling = new int[2];
        t.then(x -> {
            boilerRig(x, c, 1);
            h[0] = TestM2.husk(x, c.offset(4, 0, 0));
            floor[0] = stoneCount(x, c);
            x.report("02#12 rig: boiler, 1 steam engine with no load, 1 valve, fire level 4, coal and water", boiler(x, c) != null
                && x.level.getBlockState(c.offset(0, 0, -2)).is(AutarkiaBlocks.STEAM_ENGINE.get()), "placed");
        });
        t.waitUntil("02#12 pressure rises: at 9 bar the valve vents and screams", 900, () -> boiler(t, c) != null && boiler(t, c).venting() > 0, x -> { });
        t.waitUntil("02#12 it passes 10 bar (red zone)", 400, () -> boiler(t, c) != null && boiler(t, c).pressure() >= 10, x -> { });
        t.waitUntil("02#12 it passes 11 bar (camera shake)", 400, () -> boiler(t, c) != null && boiler(t, c).pressure() >= 11, x -> { });
        t.waitUntil("02#12 at 12 bar the boiler bursts", 400, () -> !t.level.getBlockState(c).is(AutarkiaBlocks.BOILER.get()), x -> {
            List<FallingBlockEntity> fb = x.level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(c).inflate(6));
            falling[0] = Math.max(falling[0], (int) fb.stream().filter(e -> e.getBlockState().is(Blocks.BRICKS)).count());
            falling[1] = Math.max(falling[1], (int) fb.stream().filter(e -> e.getBlockState().is(Blocks.GRAVEL)).count());
        });
        t.check("02#12 the burst throws 4 brick and 8 gravel falling blocks", () -> {
            List<FallingBlockEntity> fb = t.level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(c).inflate(6));
            falling[0] = Math.max(falling[0], (int) fb.stream().filter(e -> e.getBlockState().is(Blocks.BRICKS)).count());
            falling[1] = Math.max(falling[1], (int) fb.stream().filter(e -> e.getBlockState().is(Blocks.GRAVEL)).count());
            return falling[0] == 4 && falling[1] == 8;
        }, () -> falling[0] + " bricks, " + falling[1] + " gravel");
        t.waitTicks(10);
        t.check("02#12 nearby mobs take damage and the terrain is not damaged", () -> h[0].getHealth() < h[0].getMaxHealth() && stoneCount(t, c) == floor[0],
            () -> "husk " + h[0].getHealth() + ", floor stone " + stoneCount(t, c) + " / " + floor[0]);
    }

    private static int stoneCount(SelfTest t, BlockPos c) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-4, -1, -4), c.offset(5, -1, 5))) {
            n += t.level.getBlockState(p).is(Blocks.STONE) ? 1 : 0;
        }
        return n;
    }

    private static void loaded(Run r) {
        SelfTest t = r.t();
        BlockPos c = r.at(30, 0, 40);
        List<BlockPos> banks = List.of(r.at(30, 0, 33), r.at(31, 0, 33));
        double[] max = new double[1];
        t.then(x -> {
            boilerRig(x, c, 2);
            banks.forEach(b -> bank(x, b));
            Grid.SpanResult a = Grid.addSpan(x.level, c.offset(0, 1, -2), Direction.UP, banks.get(0), Direction.UP, CableTier.COPPER, false);
            Grid.SpanResult b = Grid.addSpan(x.level, c.offset(1, 1, -2), Direction.UP, banks.get(1), Direction.UP, CableTier.COPPER, false);
            x.report("02#13 rig: boiler, 2 steam engines wired to a 240 FE/t load, 1 valve, fire level 4", a == Grid.SpanResult.OK && b == Grid.SpanResult.OK, a + ", " + b);
        });
        t.repeat(900, x -> {
            BoilerBlockEntity b = boiler(x, c);
            if (b != null) {
                max[0] = Math.max(max[0], b.pressure());
            }
            if (x.level.getGameTime() % 20 == 0) {
                drain(x, banks);
            }
        });
        t.check("02#13 with 2 engines and a 240 FE/t load the pressure stays below 9 bar: no burst", () -> boiler(t, c) != null && max[0] < 9,
            () -> String.format(java.util.Locale.ROOT, "max %.2f bar, boiler %s", max[0], boiler(t, c) != null ? "intact" : "gone"));
    }
}

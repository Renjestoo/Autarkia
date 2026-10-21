package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.CapacitorBankBlockEntity;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.HydroTurbineBlockEntity;
import dev.autarkia.energy.MagmaTapBlockEntity;
import dev.autarkia.energy.SnappedCableEntity;
import dev.autarkia.energy.Span;
import dev.autarkia.energy.Wind;
import dev.autarkia.energy.WindTowerBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import java.util.List;
import java.util.Locale;
import net.frozenblock.lib.wind.WindManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The M7 power part of {@code /autarkia test m7} on the platform region x 50 to 130, z 0 to 40. 12 lists no 02 test
 * for M7, so the checks come from the 02 formulas: an HV span between two {@code hv_pylon}s 40 blocks apart; at u = 2
 * it snaps after about 280 ticks (230 to 330) at mean wind and drops ceil(40 / 16) spools per end; wind cools an idle
 * HV span faster (cool factor 1 + 0.5 windStrength); a hydro turbine with head 10 makes 1,280 FE/t and 0 with its outlet
 * blocked; a wind tower makes the formula output at its rotor height and loses 3% per blocking block; magma taps make
 * 2,048 FE/t on a vent, 1,024 on lava, half without water, and a lava source turns into obsidian after 24,000 running
 * ticks (that wait runs last, so the other checks run while the lava tap works).
 */
public final class TestM7Power {
    private TestM7Power() {
    }

    /** Add the power checks to the M7 script. */
    static void run(TestM7.Run r) {
        data(r);
        long[] lava = magma(r);
        hv(r);
        cooling(r);
        hydro(r);
        windTower(r);
        lavaEnd(r, lava);
    }

    private static void data(TestM7.Run r) {
        SelfTest t = r.t();
        t.check("02 data: the 5 M7 power recipes load, HV spans drop hv_cable_spool, the hv_pylon is 7 high with 4 HV spans", () -> {
            var map = t.level.recipeAccess().recipeMap();
            for (String id : List.of("hv_pylon", "hv_cable_spool", "hydro_turbine", "wind_tower", "magma_tap")) {
                if (map.byKey(ResourceKey.create(Registries.RECIPE, Autarkia.id(id))) == null) {
                    Autarkia.LOG.info("[TEST m7] missing recipe {}", id);
                    return false;
                }
            }
            var pylon = AutarkiaBlocks.HV_PYLON.get();
            return Grid.cableItem(CableTier.HV) == AutarkiaItems.HV_CABLE_SPOOL.get() && pylon.height() == 7 && pylon.maxSpans() == 4
                && pylon.tier() == CableTier.HV;
        }, () -> "5 recipes, cable item " + Grid.cableItem(CableTier.HV) + ", pylon " + AutarkiaBlocks.HV_PYLON.get().height() + " high");
    }

    private static MagmaTapBlockEntity tap(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof MagmaTapBlockEntity m ? m : null;
    }

    private static int tapOut(SelfTest t, BlockPos p) {
        return tap(t, p) == null ? -1 : tap(t, p).output();
    }

    /** Build both magma taps and check their outputs; returns {game time of the lava check, running ticks left then}. */
    private static long[] magma(TestM7.Run r) {
        SelfTest t = r.t();
        BlockPos vent = r.at(56, 0, 36);
        BlockPos lava = r.at(64, 0, 36);
        long[] mark = {-1, -1};
        t.then(x -> {
            TestM7.box(r, 55, -2, 35, 57, -2, 37, Blocks.STONE.defaultBlockState());
            TestM7.box(r, 63, -2, 35, 65, -2, 37, Blocks.STONE.defaultBlockState());
            TestM1.place(x, vent.below(), AutarkiaBlocks.MAGMA_VENT.get().defaultBlockState());
            TestM1.place(x, lava.below(), Blocks.LAVA.defaultBlockState());
            boolean ok = TestM1.placeBig(x, vent, AutarkiaBlocks.MAGMA_TAP.get(), Direction.NORTH);
            ok &= TestM1.placeBig(x, lava, AutarkiaBlocks.MAGMA_TAP.get(), Direction.NORTH);
            for (BlockPos p : List.of(vent, lava)) {
                TestM1.place(x, p.east(), Blocks.STONE.defaultBlockState());
                TestM6.water(x, p.east().above());
            }
            x.report("02 magma rig: one tap on a magma vent, one on a lava source, each with a water source at its upper block", ok, "placed " + ok);
        });
        t.waitUntil("02 magma: on a magma vent with water the tap makes 2,048 FE/t", 10, () -> tapOut(t, vent) == 2048, x -> { },
            () -> "out " + tapOut(t, vent));
        t.waitUntil("02 magma: on a lava source with water the tap makes 1,024 FE/t", 10, () -> tapOut(t, lava) == 1024 && tap(t, lava).onLava(),
            x -> { }, () -> "out " + tapOut(t, lava));
        t.then(x -> {
            MagmaTapBlockEntity lt = tap(x, lava);
            mark[0] = x.level.getGameTime();
            mark[1] = lt == null ? -1 : lt.lavaTicksLeft();
            TestM1.place(x, vent.east().above(), Blocks.AIR.defaultBlockState());
        });
        t.waitUntil("02 magma: without water the vent tap halves to 1,024 FE/t (and smokes)", 10, () -> tapOut(t, vent) == 1024 && !tap(t, vent).cooled(),
            x -> { }, () -> "out " + tapOut(t, vent));
        return mark;
    }

    private static void lavaEnd(TestM7.Run r, long[] mark) {
        SelfTest t = r.t();
        BlockPos lava = r.at(64, 0, 36);
        long[] seen = new long[1];
        t.waitUntil("02 magma: the lava source under the tap turns into obsidian", MagmaTapBlockEntity.LAVA_TICKS + 100, () -> {
            seen[0] = t.level.getGameTime();
            return t.level.getBlockState(lava.below()).is(Blocks.OBSIDIAN);
        }, x -> { }, () -> tap(t, lava) == null ? "missing" : tap(t, lava).lavaTicksLeft() + " running ticks left");
        t.check("02 magma: the obsidian came after 24,000 running ticks and the lava tap stopped", () -> mark[1] > 0 && tapOut(t, lava) == 0
            && Math.abs(seen[0] - (mark[0] + mark[1])) <= 1, () -> "expected at " + (mark[0] + mark[1]) + ", seen at " + seen[0] + ", out " + tapOut(t, lava));
    }

    /**
     * Return the two ends of the 40-block HV line, the end with the lower {@code BlockPos.asLong} first: a network
     * without generators roots its spanning tree there, so feeding that end puts the whole sink flow on the long span.
     */
    private static BlockPos[] ends(TestM7.Run r) {
        BlockPos a = r.at(52, 0, 20);
        BlockPos b = r.at(92, 0, 20);
        return a.asLong() < b.asLong() ? new BlockPos[] {a, b} : new BlockPos[] {b, a};
    }

    private static void hv(TestM7.Run r) {
        SelfTest t = r.t();
        BlockPos[] e = ends(r);
        BlockPos src = e[0];
        BlockPos far = e[1];
        List<BlockPos> hubs = List.of(far, far.offset(0, 0, 4), far.offset(0, 0, -4));
        List<BlockPos> banks = hubs.stream().flatMap(h -> Direction.Plane.HORIZONTAL.stream().map(h::relative)).toList();
        BlockPos mid = r.at(72, 6, 20);
        long[] m = {-1, -1};
        float[] load = new float[1];
        t.then(x -> {
            x.command("weather clear");
            x.command("frozenlib wind override 0.34 0 0");
            boolean ok = TestM1.placeBig(x, src, AutarkiaBlocks.HV_PYLON.get(), Direction.NORTH);
            for (BlockPos h : hubs) {
                ok &= TestM1.placeBig(x, h, AutarkiaBlocks.HV_PYLON.get(), Direction.NORTH);
            }
            banks.forEach(b -> TestM4.bank(x, b));
            Grid.SpanResult line = Grid.addSpan(x.level, src, far, CableTier.HV);
            Grid.SpanResult s1 = Grid.addSpan(x.level, far, hubs.get(1), CableTier.HV);
            Grid.SpanResult s2 = Grid.addSpan(x.level, far, hubs.get(2), CableTier.HV);
            Span sp = TestM1.spanBetween(x, src, far);
            x.report("02 HV: an HV span joins two hv_pylons 40 blocks apart (12 capacitor banks on 3 pylons at the far end)", ok
                && line == Grid.SpanResult.OK && s1 == Grid.SpanResult.OK && s2 == Grid.SpanResult.OK && sp != null && sp.tier == CableTier.HV
                && Math.abs(sp.length - 40) < 0.01, line + ", " + s1 + ", " + s2 + (sp == null ? "" : ", length " + sp.length));
        });
        t.waitTicks(5);
        t.check("02 HV: the wind override holds a mean wind (windStrength 0.68)", () -> WindManager.getOrCreate(t.level).windOverride.isPresent()
            && Math.abs(Wind.strength(t.level, Vec3.atCenterOf(mid)) - 0.68) < 0.03, () -> String.format(Locale.ROOT, "strength %.3f",
            Wind.strength(t.level, Vec3.atCenterOf(mid))));
        t.then(x -> m[0] = x.level.getGameTime());
        t.waitUntil("02 HV: at u = 2 (16,384 FE/t) the span heats and snaps", 600, () -> TestM1.spanBetween(t, src, far) == null, x -> {
            TestM1.pushEnergy(x, src, 16384);
            for (BlockPos b : banks) {
                CapacitorBankBlockEntity be = TestM4.bankAt(x, b);
                if (be != null) {
                    be.buffer().drain(be.buffer().stored());
                }
            }
            Span sp = TestM1.spanBetween(x, src, far);
            if (sp != null) {
                load[0] = Math.max(load[0], sp.load);
                m[1] = x.level.getGameTime() - m[0];
            }
        });
        t.check("02 HV: the snap came after about 280 ticks (230 to 330) at u 2.0", () -> load[0] >= 1.95f && m[1] >= 230 && m[1] <= 330,
            () -> String.format(Locale.ROOT, "u %.3f, snap after %d ticks", load[0], m[1]));
        t.check("02 HV: two HV snapped cable ends whip and arc", () -> t.level.getEntitiesOfClass(SnappedCableEntity.class, new AABB(mid).inflate(32),
            s -> s.tier() == CableTier.HV).size() == 2, () -> t.level.getEntitiesOfClass(SnappedCableEntity.class, new AABB(mid).inflate(32)).size() + " ends");
        t.waitTicks(CableTier.HV.snapLifetime + 10);
        t.check("02 HV: after 200 ticks each end drops ceil(40 / 16) = 3 hv_cable_spool", () -> TestM1.countItems(t, mid, 32,
            AutarkiaItems.HV_CABLE_SPOOL.get()) == 6, () -> TestM1.countItems(t, mid, 32, AutarkiaItems.HV_CABLE_SPOOL.get()) + " spools");
        t.then(x -> TestM1.killItems(x, mid, 32));
    }

    private static void cooling(TestM7.Run r) {
        SelfTest t = r.t();
        BlockPos[] e = ends(r);
        Vec3 mid = Vec3.atCenterOf(r.at(72, 6, 20));
        double[] calm = new double[3];
        double[] windy = new double[3];
        t.then(x -> {
            Grid.SpanResult s = Grid.addSpan(x.level, e[0], e[1], CableTier.HV);
            x.report("02 HV cooling rig: a new idle 40-block HV span", s == Grid.SpanResult.OK, s.toString());
        });
        decay(r, e, "0 0 0", mid, calm);
        decay(r, e, "1 0 0", mid, windy);
        t.check("02 HV cooling: still air cools at factor 1.0 (rate C h, C 0.0012)", () -> Math.abs(coolFactor(calm) - (1 + 0.5 * calm[2])) < 0.03
            && calm[2] < 0.05, () -> String.format(Locale.ROOT, "factor %.3f, windStrength %.3f", coolFactor(calm), calm[2]));
        t.check("02 HV cooling: strong wind cools at 1 + 0.5 windStrength (1.75) and faster than still air", () -> Math.abs(coolFactor(windy)
            - (1 + 0.5 * windy[2])) < 0.03 && windy[2] > 1.4 && windy[1] < calm[1], () -> String.format(Locale.ROOT,
            "factor %.3f, windStrength %.3f, heat after 100 ticks %.4f (still %.4f)", coolFactor(windy), windy[2], windy[1], calm[1]));
    }

    /**
     * Set the wind, heat the idle span to 0.9 and record {heat before, heat after 100 grid ticks, windStrength at mid};
     * a waitTicks(n) step spans n - 1 further server ticks.
     */
    private static void decay(TestM7.Run r, BlockPos[] e, String wind, Vec3 mid, double[] out) {
        SelfTest t = r.t();
        t.then(x -> x.command("frozenlib wind override " + wind));
        t.waitTicks(5);
        t.then(x -> {
            Span sp = TestM1.spanBetween(x, e[0], e[1]);
            if (sp != null) {
                sp.heat = 0.9f;
                out[0] = sp.heat;
            }
            out[2] = Wind.strength(x.level, mid);
        });
        t.waitTicks(101);
        t.then(x -> {
            Span sp = TestM1.spanBetween(x, e[0], e[1]);
            out[1] = sp == null ? 0 : sp.heat;
        });
    }

    private static double coolFactor(double[] run) {
        if (run[0] <= 0 || run[1] <= 0) {
            return -1;
        }
        return (1 - Math.pow(run[1] / run[0], 1 / 100.0)) / CableTier.HV.heatCool;
    }

    private static HydroTurbineBlockEntity hydroAt(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof HydroTurbineBlockEntity h ? h : null;
    }

    private static void hydro(TestM7.Run r) {
        SelfTest t = r.t();
        BlockPos c = r.at(110, 0, 8);
        t.then(x -> {
            boolean ok = TestM1.placeBig(x, c, AutarkiaBlocks.HYDRO_TURBINE.get(), Direction.NORTH);
            TestM7.box(r, 109, 3, 7, 111, 12, 9, Blocks.STONE.defaultBlockState());
            TestM7.box(r, 110, 3, 8, 110, 12, 8, Blocks.WATER.defaultBlockState());
            x.report("02 hydro rig: turbine with a 10-block water column in a stone shaft above its intake", ok, "placed " + ok);
        });
        t.waitTicks(25);
        t.check("02 hydro: head 10 gives 128 x 10 = 1,280 FE/t", () -> hydroAt(t, c) != null && hydroAt(t, c).head() == 10
            && hydroAt(t, c).output() == 1280, () -> hydroAt(t, c) == null ? "missing" : "head " + hydroAt(t, c).head() + ", out " + hydroAt(t, c).output());
        t.then(x -> TestM1.place(x, c.north(2), Blocks.STONE.defaultBlockState()));
        t.waitTicks(25);
        t.check("02 hydro: with the outlet blocked the output is 0", () -> hydroAt(t, c).output() == 0 && hydroAt(t, c).head() == 10,
            () -> "head " + hydroAt(t, c).head() + ", out " + hydroAt(t, c).output());
        t.then(x -> TestM1.place(x, c.north(2), Blocks.AIR.defaultBlockState()));
        t.waitTicks(25);
        t.check("02 hydro: with the outlet open again it makes 1,280 FE/t", () -> hydroAt(t, c).output() == 1280,
            () -> "out " + hydroAt(t, c).output());
    }

    private static WindTowerBlockEntity towerAt(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof WindTowerBlockEntity w ? w : null;
    }

    /** Return the 02 section 6.2 wind tower output for the tower's sampled wind and weather and n blocking blocks. */
    private static long expected(WindTowerBlockEntity w, int blocked) {
        double base = Mth.clamp(512 + (w.rotor().getY() - 64) * 20, 512, 3072);
        double windFactor = Mth.clamp(0.5 + w.wind(), 0.5, 1.5);
        double obstruction = blocked >= 25 ? 0 : 1 - 0.03 * blocked;
        return Math.min(4096, Math.round(base * windFactor * w.weather() * obstruction));
    }

    private static void windTower(TestM7.Run r) {
        SelfTest t = r.t();
        BlockPos c = r.at(80, 0, 8);
        long[] clear = new long[1];
        t.then(x -> {
            x.command("weather clear");
            x.command("frozenlib wind override 0.25 0 0");
            boolean ok = TestM1.placeBig(x, c, AutarkiaBlocks.WIND_TOWER.get(), Direction.NORTH);
            x.report("02 wind tower rig: an 8-block tower facing north under open sky, wind override 0.25 (windStrength 0.5)", ok, "placed " + ok);
        });
        t.waitTicks(45);
        t.check("02 wind tower: output = clamp(512 + (yRotor - 64) x 20, 512, 3,072) x W x M at the rotor height, nothing in front", () -> {
            WindTowerBlockEntity w = towerAt(t, c);
            if (w == null) {
                return false;
            }
            clear[0] = w.output();
            return w.blocked() == 0 && Math.abs(w.wind() - Wind.strength(t.level, Vec3.atCenterOf(w.rotor()))) < 0.01 && w.weather() == 1.0
                && w.output() == expected(w, 0) && w.output() > 0;
        }, () -> towerAt(t, c) == null ? "missing" : String.format(Locale.ROOT, "rotor y %d, wind %.3f, out %d, expected %d",
            towerAt(t, c).rotor().getY(), towerAt(t, c).wind(), towerAt(t, c).output(), expected(towerAt(t, c), 0)));
        t.then(x -> {
            BlockPos plane = c.above(WindTowerBlockEntity.HEIGHT - 1).north();
            for (int a = -2; a <= 2; a++) {
                TestM1.place(x, plane.east(a), Blocks.STONE.defaultBlockState());
            }
        });
        t.waitTicks(25);
        t.check("02 wind tower: 5 solid blocks in the 9 x 9 plane cut the output by 15%", () -> {
            WindTowerBlockEntity w = towerAt(t, c);
            return w.blocked() == 5 && w.output() == expected(w, 5) && Math.abs(w.output() - clear[0] * 0.85) <= 1;
        }, () -> "blocked " + towerAt(t, c).blocked() + ", out " + towerAt(t, c).output() + " of " + clear[0]);
        t.then(x -> {
            BlockPos plane = c.above(WindTowerBlockEntity.HEIGHT - 1).north();
            for (int a = -2; a <= 2; a++) {
                for (int b = -2; b <= 2; b++) {
                    TestM1.place(x, plane.east(a).above(b), Blocks.STONE.defaultBlockState());
                }
            }
        });
        t.waitTicks(25);
        t.check("02 wind tower: 25 blocks in the plane stop it", () -> towerAt(t, c).blocked() == 25 && towerAt(t, c).output() == 0,
            () -> "blocked " + towerAt(t, c).blocked() + ", out " + towerAt(t, c).output());
        t.then(x -> x.command("frozenlib wind override disable"));
        t.waitTicks(2);
        t.check("02 wind: the wind override is off again", () -> WindManager.getOrCreate(t.level).windOverride.isEmpty(), () -> "override "
            + WindManager.getOrCreate(t.level).windOverride);
    }
}

package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.BoilerBlockEntity;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.FuseBoxBlockEntity;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.Span;
import dev.autarkia.energy.SteamTurbineBlockEntity;
import dev.autarkia.energy.StorageBankBlockEntity;
import dev.autarkia.energy.TransformerBlockEntity;
import dev.autarkia.processing.MachineBlockEntity;
import dev.autarkia.processing.SluiceBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /autarkia test m6}: the M6 data, the T3 power tests 02 #8 (an inline fuse box trips, resets and locks), #14
 * (steam turbine with 3 boilers) and #15 (transformer cap and loss; insulated to copper, as HV comes in T4), the T3
 * ore line 03 #15 to #17, then the equipment ({@link TestM6Equipment}), logistics ({@link TestM6Logistics}) and colony
 * ({@link TestM6Colony}) parts on a platform at y 120 or higher.
 */
public final class TestM6 {
    private TestM6() {
    }

    /** One M6 run: the test and the platform base. */
    record Run(SelfTest t, BlockPos base) {
        BlockPos at(int x, int y, int z) {
            return base.offset(x, y, z);
        }
    }

    /** Build the platform and run the whole M6 script. */
    public static int run(CommandSourceStack src) {
        return run(src, "all");
    }

    /**
     * Build the platform and run the M6 script from a section: {@code all}, {@code equipment}, {@code logistics} or
     * {@code colony} (the colony part needs no earlier section), or only the colony setup and W11 ({@code line}).
     */
    public static int run(CommandSourceStack src, String from) {
        SelfTest t = SelfTest.create("m6", src);
        BlockPos o = t.origin;
        Run r = new Run(t, new BlockPos(o.getX(), Math.max(o.getY() + 8, 120), o.getZ()));
        int start = List.of("all", "equipment", "logistics", "colony", "line").indexOf(from);
        t.then(x -> reset(r));
        data(r);
        if (start <= 0) {
            fuse(r);
            turbine(r);
            transformer(r);
            ores(r);
        }
        if (start <= 1) {
            TestM6Equipment.run(r);
        }
        if (start <= 2) {
            TestM6Logistics.capsules(r);
        }
        TestM6Colony.setup(r);
        if (start == 4) {
            TestM6Line.run(r);
            return t.start();
        }
        TestM6Logistics.warehouse(r);
        TestM6Colony.run(r);
        return t.start();
    }

    static void box(Run r, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(r.at(x0, y0, z0), r.at(x1, y1, z1))) {
            r.t().level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private static void reset(Run r) {
        SelfTest t = r.t();
        t.command("forceload add " + (r.base().getX() - 8) + " " + (r.base().getZ() - 8) + " " + (r.base().getX() + 136) + " " + (r.base().getZ() + 152));
        t.command("weather clear");
        t.command("gamerule doMobSpawning false");
        AABB area = new AABB(Vec3.atCenterOf(r.at(-8, -6, -8)), Vec3.atCenterOf(r.at(136, 40, 152)));
        t.level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area, e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(e -> e.discard());
        SelfTest.clearSpans(t.level, area);
        box(r, -2, 0, -2, 130, 24, 148, Blocks.AIR.defaultBlockState());
        box(r, -2, -1, -2, 130, -1, 148, Blocks.STONE.defaultBlockState());
        Autarkia.LOG.info("[TEST m6] platform at {}", r.base().toShortString());
    }

    private static void data(Run r) {
        SelfTest t = r.t();
        t.check("data: the M6 advancements, recipes, T3 templates 14 to 20 and their 3 plans load", () -> {
            for (String a : List.of("materials/pure", "colony/self_repair", "colony/second_line", "colony/learned", "logistics/pneumatic", "logistics/fetch",
                "workers/drone_repair", "workers/fuse_unordered", "workers/loader_delivery", "equipment/live_wire", "equipment/clean_cut")) {
                if (t.level.getServer().getAdvancements().get(Autarkia.id(a)) == null) {
                    Autarkia.LOG.info("[TEST m6] missing advancement {}", a);
                    return false;
                }
            }
            var map = t.level.recipeAccess().recipeMap();
            for (String id : List.of("steel_pylon", "transformer", "fuse_box", "steam_turbine", "battery_bank", "insulated_cable_spool", "jaw_crusher",
                "magnetic_separator", "electrolyzer", "fabricator", "basic_circuit", "fabricating/servo", "crushing/copper_jaw", "separating/pure_copper",
                "electrolyzing/copper_from_pure", "steel_rack", "warehouse_controller", "ledger_terminal", "capsule_tube", "capsule_station", "recharge_dock",
                "domain_beacon", "prop_beam", "drone_frame", "worker_upgrade_speed", "worker_upgrade_cargo", "worker_upgrade_armor", "worker_upgrade_range",
                "survey_lens", "fabricating/core_upgrade_t3", "fabricating/electric_drill", "worker_assembly/builder_drone", "worker_assembly/loader",
                "worker_assembly/lineman")) {
                if (map.byKey(ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Autarkia.id(id))) == null) {
                    Autarkia.LOG.info("[TEST m6] missing recipe {}", id);
                    return false;
                }
            }
            var tpl = dev.autarkia.colony.ColonyPlans.templates();
            for (String id : List.of("fuse_box", "rack_upgrade", "duplicate_line", "pattern_repair", "recharge_dock", "steam_turbine", "battery_bank")) {
                if (tpl.get(id) == null || tpl.get(id).tier() != 3) {
                    return false;
                }
            }
            for (String p : List.of("recharge_dock", "turbine_hall", "battery_row")) {
                if (dev.autarkia.colony.ColonyPlans.plan(Autarkia.id(p)) == null) {
                    return false;
                }
            }
            return true;
        }, () -> "11 advancements, 34 recipes, 7 templates, 3 plans");
    }

    static void pylon(SelfTest t, BlockPos p) {
        TestM1.placeBig(t, p, AutarkiaBlocks.STEEL_PYLON.get(), Direction.NORTH);
    }

    static void battery(SelfTest t, BlockPos p) {
        TestM1.place(t, p, AutarkiaBlocks.BATTERY_BANK.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
    }

    private static void fuse(Run r) {
        SelfTest t = r.t();
        BlockPos src = r.at(2, 0, 2);
        BlockPos fuse = r.at(10, 0, 2);
        BlockPos p1 = r.at(18, 0, 2);
        float[] heat = new float[1];
        long[] first = new long[2];
        t.then(x -> {
            TestM4.pole(x, src);
            TestM4.pole(x, p1);
            List.of(r.at(19, 0, 2), r.at(18, 0, 3), r.at(18, 0, 1)).forEach(b -> TestM4.bank(x, b));
            TestM1.place(x, fuse, AutarkiaBlocks.FUSE_BOX.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
            Grid.SpanResult a = Grid.addSpan(x.level, src, fuse, CableTier.COPPER);
            Grid.SpanResult b = Grid.addSpan(x.level, fuse, p1, CableTier.COPPER);
            x.report("02#8 rig: copper pole, inline fuse box, copper pole with 3 capacitor banks", a == Grid.SpanResult.OK && b == Grid.SpanResult.OK, a + ", " + b);
        });
        java.util.function.Consumer<SelfTest> feed = x -> {
            TestM1.pushEnergy(x, src, 512);
            Span sp = TestM1.spanBetween(x, src, fuse);
            if (sp != null) {
                heat[0] = Math.max(heat[0], sp.heat);
            }
        };
        t.waitUntil("02#8 at 512 FE/t (u = 2) the fuse trips at once", 40, () -> fuseAt(t, fuse) != null && fuseAt(t, fuse).open(), x -> {
            feed.accept(x);
            first[0] = x.level.getGameTime();
        });
        t.waitUntil("02#8 the fuse closes again after 10 s", 260, () -> !fuseAt(t, fuse).open(), feed);
        t.check("02#8 the reset came 195 to 210 ticks after the trip", () -> t.level.getGameTime() - first[0] >= 195 && t.level.getGameTime() - first[0] <= 210,
            () -> (t.level.getGameTime() - first[0]) + " ticks");
        t.waitUntil("02#8 under the same load it trips again", 40, () -> fuseAt(t, fuse).open(), feed);
        t.waitUntil("02#8 after 3 trips in a minute it locks open", 500, () -> fuseAt(t, fuse).locked(), feed);
        t.repeat(240, feed);
        t.check("02#8 locked: still open after 12 s, 3 trips, the span stayed below h 0.1", () -> fuseAt(t, fuse).open() && fuseAt(t, fuse).trips() >= 3
            && heat[0] < 0.1f, () -> "trips " + fuseAt(t, fuse).trips() + ", max heat " + heat[0]);
    }

    private static FuseBoxBlockEntity fuseAt(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof FuseBoxBlockEntity f ? f : null;
    }

    static void water(SelfTest t, BlockPos p) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (t.level.getBlockState(p.relative(d)).isAir()) {
                TestM1.place(t, p.relative(d), Blocks.STONE.defaultBlockState());
            }
        }
        TestM1.place(t, p, Blocks.WATER.defaultBlockState());
    }

    private static void turbine(Run r) {
        SelfTest t = r.t();
        BlockPos c = r.at(40, 0, 8);
        List<BlockPos> boilers = List.of(r.at(39, 0, 10), r.at(37, 0, 8), r.at(42, 0, 8));
        long[] marks = {-1, -1, -1, -1};
        int[] best = new int[1];
        t.then(x -> {
            boolean ok = TestM1.placeBig(x, c, AutarkiaBlocks.STEAM_TURBINE.get(), Direction.NORTH);
            for (BlockPos b : boilers) {
                ok &= TestM1.placeBig(x, b, AutarkiaBlocks.BOILER.get(), Direction.NORTH);
                TestM1.place(x, b.offset(1, 3, 1), AutarkiaBlocks.PRESSURE_VALVE.get().defaultBlockState());
            }
            water(x, r.at(36, 0, 8));
            water(x, r.at(36, 0, 9));
            water(x, r.at(44, 0, 8));
            water(x, r.at(44, 0, 9));
            water(x, r.at(39, 0, 12));
            water(x, r.at(40, 0, 12));
            for (BlockPos b : boilers) {
                BoilerBlockEntity be = (BoilerBlockEntity) x.level.getBlockEntity(b);
                be.inventory().put(0, new ItemStack(Items.COAL, 64));
                be.setFire(4);
            }
            x.report("02#14 rig: turbine with 3 boilers on its back and sides at fire level 4", ok
                && x.level.getBlockEntity(c) instanceof SteamTurbineBlockEntity tb && tb.boilers().size() == 3, "placed " + ok);
        });
        t.waitUntil("02#14 the rotor reaches 100%", 4000, () -> turbineAt(t, c).rotor() >= 0.999f, x -> {
            SteamTurbineBlockEntity tb = turbineAt(x, c);
            tb.buffer().drain(tb.buffer().stored());
            if (marks[0] < 0 && tb.steamRate() >= 10) {
                marks[0] = x.level.getGameTime();
            }
            for (BlockPos b : boilers) {
                BoilerBlockEntity be = (BoilerBlockEntity) x.level.getBlockEntity(b);
                if (be.inventory().get(0).getCount() < 8) {
                    be.inventory().put(0, new ItemStack(Items.COAL, 64));
                }
            }
        });
        t.then(x -> marks[1] = x.level.getGameTime());
        t.repeat(40, x -> {
            SteamTurbineBlockEntity tb = turbineAt(x, c);
            tb.buffer().drain(tb.buffer().stored());
            best[0] = Math.max(best[0], tb.output());
        });
        t.check("02#14 spin-up takes about 30 s (560 to 680 ticks of steam) and the output is 1,000 FE/t", () -> marks[1] - marks[0] >= 560
            && marks[1] - marks[0] <= 680 && best[0] == 1000, () -> (marks[1] - marks[0]) + " ticks, output " + best[0]);
        t.then(x -> {
            for (BlockPos b : boilers) {
                BoilerBlockEntity be = (BoilerBlockEntity) x.level.getBlockEntity(b);
                be.setFire(0);
                be.inventory().put(0, ItemStack.EMPTY);
            }
        });
        t.waitUntil("02#14 without steam the rotor stops", 4000, () -> turbineAt(t, c).rotor() <= 0f, x -> {
            SteamTurbineBlockEntity tb = turbineAt(x, c);
            tb.buffer().drain(tb.buffer().stored());
            if (marks[2] < 0 && tb.steamRate() < 10) {
                marks[2] = x.level.getGameTime();
            }
            marks[3] = x.level.getGameTime();
        });
        t.check("02#14 spin-down takes about 60 s (1,150 to 1,260 ticks without steam)", () -> marks[3] - marks[2] >= 1150 && marks[3] - marks[2] <= 1260,
            () -> (marks[3] - marks[2]) + " ticks");
    }

    private static SteamTurbineBlockEntity turbineAt(SelfTest t, BlockPos p) {
        return (SteamTurbineBlockEntity) t.level.getBlockEntity(p);
    }

    private static void transformer(Run r) {
        SelfTest t = r.t();
        BlockPos ip = r.at(2, 0, 20);
        BlockPos tr = r.at(14, 0, 20);
        BlockPos cp = r.at(22, 0, 20);
        List<BlockPos> banks = List.of(r.at(23, 0, 20), r.at(22, 0, 21), r.at(22, 0, 19));
        long[] sums = new long[4];
        t.then(x -> {
            pylon(x, ip);
            TestM1.place(x, tr, AutarkiaBlocks.TRANSFORMER.get().defaultBlockState());
            TestM4.pole(x, cp);
            banks.forEach(b -> battery(x, b));
            Grid.SpanResult a = Grid.addSpan(x.level, ip, tr, CableTier.INSULATED);
            Grid.SpanResult b = Grid.addSpan(x.level, tr, cp, CableTier.COPPER);
            if (x.level.getBlockEntity(tr) instanceof TransformerBlockEntity te) {
                te.setMode(TransformerBlockEntity.Mode.A_TO_B);
            }
            x.report("02#15 rig: steel pylon, insulated span, transformer, copper span, pole with 3 battery banks", a == Grid.SpanResult.OK
                && b == Grid.SpanResult.OK, a + ", " + b);
        });
        t.repeat(100, x -> TestM1.pushEnergy(x, ip, 1000));
        t.then(x -> sums[0] = stored(x, banks));
        t.repeat(200, x -> {
            TestM1.pushEnergy(x, ip, 1000);
            sums[1] += ((TransformerBlockEntity) x.level.getBlockEntity(tr)).intake();
            int f = ((TransformerBlockEntity) x.level.getBlockEntity(tr)).flow();
            sums[2] = Math.max(sums[2], f);
            sums[3] += f;
        });
        t.check("02#15 the transfer is capped at 256 FE/t (copper rating) and the copper span stays at u <= 1.0", () -> {
            Span sp = TestM1.spanBetween(t, tr, cp);
            return sums[2] <= 256 && sums[3] >= 200 * 240 && sp != null && sp.load <= 1.0f + 1e-4f;
        }, () -> "max flow " + sums[2] + ", mean " + sums[3] / 200 + ", copper u " + (TestM1.spanBetween(t, tr, cp) == null ? -1 : TestM1.spanBetween(t, tr, cp).load));
        t.check("02#15 the transformer loses 2%: given out / taken in 0.975 to 0.985", () -> {
            double ratio = sums[3] / (double) Math.max(1, sums[1]);
            return ratio >= 0.975 && ratio <= 0.985;
        }, () -> sums[3] + " out of " + sums[1] + " in");
    }

    private static long stored(SelfTest t, List<BlockPos> banks) {
        long n = 0;
        for (BlockPos b : banks) {
            if (t.level.getBlockEntity(b) instanceof StorageBankBlockEntity s) {
                n += s.buffer().stored();
            }
        }
        return n;
    }

    static int take(MachineBlockEntity m, Item item) {
        int n = 0;
        for (int i = m.inventory().inputs(); i < m.inventory().size(); i++) {
            ItemStack s = m.inventory().get(i);
            if (s.is(item)) {
                n += s.getCount();
                m.inventory().put(i, ItemStack.EMPTY);
            }
        }
        return n;
    }

    private static void ores(Run r) {
        SelfTest t = r.t();
        BlockPos jaw = r.at(8, 0, 34);
        BlockPos sluice = r.at(20, 0, 36);
        BlockPos crate = r.at(20, 0, 34);
        BlockPos sep = r.at(30, 0, 34);
        BlockPos ely = r.at(34, 0, 34);
        int[] n = new int[8];
        t.then(x -> {
            box(r, 8, -1, 34, 9, -1, 35, AutarkiaBlocks.FOUNDATION.get().defaultBlockState());
            boolean ok = TestM1.placeBig(x, jaw, AutarkiaBlocks.JAW_CRUSHER.get(), Direction.NORTH);
            n[7] = TestM1.insertAt(x.level, jaw, new ItemStack(Items.RAW_COPPER, 64));
            x.report("03#15 rig: jaw crusher on 4 foundations with 64 raw copper", ok && n[7] == 64, "inserted " + n[7]);
        });
        t.waitUntil("03#15 the jaw crusher turns 64 raw copper into 128 crushed copper", 64 * 100 + 600, () -> n[0] == 128, x -> {
            TestM1.pushEnergy(x, jaw, 400);
            if (x.level.getBlockEntity(jaw) instanceof MachineBlockEntity m) {
                n[0] += take(m, AutarkiaItems.CRUSHED_COPPER.get());
            }
        });
        t.check("03#15 exactly 128 crushed copper and nothing left in the crusher", () -> n[0] == 128 && ((MachineBlockEntity) t.level.getBlockEntity(jaw))
            .inventory().queue() == 0, () -> n[0] + " crushed");
        t.then(x -> {
            TestM1.placeBig(x, sluice, AutarkiaBlocks.SLUICE.get(), Direction.NORTH);
            TestM1.place(x, crate, AutarkiaBlocks.CRATE.get().defaultBlockState());
            box(r, 19, 0, 38, 19, 1, 40, Blocks.STONE.defaultBlockState());
            box(r, 21, 0, 38, 21, 1, 40, Blocks.STONE.defaultBlockState());
            box(r, 20, 0, 40, 20, 1, 40, Blocks.STONE.defaultBlockState());
            TestM1.place(x, r.at(20, 0, 39), Blocks.WATER.defaultBlockState());
            n[7] = 0;
        });
        t.waitUntil("03#16 the sluice washes the 128 crushed copper", 128 * 100 + 900, () -> TestM1.crateCount(t, crate, AutarkiaItems.WASHED_COPPER.get()) == 128,
            x -> {
                if (n[7] < 128 && x.level.getBlockEntity(sluice) instanceof SluiceBlockEntity s && s.inventory().get(0).getCount() < 8) {
                    n[7] += TestM1.insertAt(x.level, sluice, new ItemStack(AutarkiaItems.CRUSHED_COPPER.get(), Math.min(8, 128 - n[7])));
                }
            });
        t.then(x -> {
            TestM1.place(x, sep, AutarkiaBlocks.MAGNETIC_SEPARATOR.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
            n[7] = 0;
        });
        t.waitUntil("03#16 the magnetic separator makes 192 pure copper from the 128 washed copper", 64 * 120 + 900, () -> n[1] == 192, x -> {
            TestM1.pushEnergy(x, sep, 400);
            if (x.level.getBlockEntity(sep) instanceof MachineBlockEntity m) {
                if (n[7] < 128 && m.inventory().queue() < 32) {
                    n[7] += TestM1.insertAt(x.level, sep, new ItemStack(AutarkiaItems.WASHED_COPPER.get(), Math.min(32, 128 - n[7])));
                }
                n[1] += take(m, AutarkiaItems.PURE_COPPER.get());
                n[2] += take(m, AutarkiaItems.SLAG.get());
                n[3] += take(m, Items.GOLD_NUGGET);
            }
        });
        t.check("03#16 192 pure copper, 64 slag, gold nuggets near 32 (doc 95% range 24 to 40; checked at 99.9%)", () -> n[1] == 192 && n[2] == 64
            && SelfTest.binomial(n[3], 64, 0.5),
            () -> n[1] + " pure, " + n[2] + " slag, " + n[3] + " gold");
        t.then(x -> {
            TestM1.place(x, ely, AutarkiaBlocks.ELECTROLYZER.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
            n[7] = 0;
        });
        t.waitUntil("03#17 the electrolyzer turns the 192 pure copper into 192 copper ingots", 192 * 160 + 900, () -> n[4] == 192, x -> {
            TestM1.pushEnergy(x, ely, 600);
            if (x.level.getBlockEntity(ely) instanceof MachineBlockEntity m) {
                if (n[7] < 192 && m.inventory().queue() < 32) {
                    n[7] += TestM1.insertAt(x.level, ely, new ItemStack(AutarkiaItems.PURE_COPPER.get(), Math.min(32, 192 - n[7])));
                }
                n[4] += take(m, Items.COPPER_INGOT);
                n[5] += take(m, Items.GOLD_NUGGET);
            }
        });
        t.check("03#17 exactly 192 copper ingots (3 per raw ore), gold nuggets near 19 (doc 95% range 12 to 26; checked at 99.9%)", () -> n[4] == 192
            && SelfTest.binomial(n[5], 192, 0.1),
            () -> n[4] + " ingots, " + n[5] + " gold");
    }
}

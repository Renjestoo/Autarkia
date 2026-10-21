package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.equipment.AreaMining;
import dev.autarkia.equipment.SlingShotEntity;
import dev.autarkia.equipment.SteamRepeaterItem;
import dev.autarkia.equipment.ToolMode;
import dev.autarkia.processing.BlastSmelterBlockEntity;
import dev.autarkia.processing.MachineBlockEntity;
import dev.autarkia.processing.MachineStatus;
import dev.autarkia.processing.SluiceBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * {@code /autarkia test m5}: the T2 processing chain 03 #11 to #14 (sluice with flowing water, no water and a source
 * only; blast smelter heat, washed ore and steel; plate press and wire drawer), the steel equipment 07 #4 to #6
 * (diamond-level mining, sledgehammer 3x3 on three faces, steam repeater charge), the M5 data, then the logistics part
 * ({@link TestM5Logistics}) and the colony part ({@link TestM5Colony}) on a platform at y 120 or higher.
 */
public final class TestM5 {
    private TestM5() {
    }

    /** One M5 run: the test and the platform base. */
    record Run(SelfTest t, BlockPos base) {
        BlockPos at(int x, int y, int z) {
            return base.offset(x, y, z);
        }
    }

    /** Build the platform and run the M5 script. */
    public static int run(CommandSourceStack src) {
        SelfTest t = SelfTest.create("m5", src);
        BlockPos o = t.origin;
        Run r = new Run(t, new BlockPos(o.getX(), Math.max(o.getY() + 8, 120), o.getZ()));
        t.then(x -> reset(r));
        data(r);
        sluice(r);
        smelter(r);
        forming(r);
        equipment(r);
        TestM5Colony.setup(r);
        TestM5Logistics.run(r);
        TestM5Colony.run(r);
        return t.start();
    }

    static void box(Run r, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(r.at(x0, y0, z0), r.at(x1, y1, z1))) {
            r.t().level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private static void reset(Run r) {
        SelfTest t = r.t();
        t.command("forceload add " + (r.base().getX() - 8) + " " + (r.base().getZ() - 8) + " " + (r.base().getX() + 72) + " " + (r.base().getZ() + 136));
        t.command("weather clear");
        AABB area = new AABB(Vec3.atCenterOf(r.at(-8, -6, -8)), Vec3.atCenterOf(r.at(72, 24, 136)));
        t.level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area, e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(e -> e.discard());
        box(r, -2, 0, -2, 64, 16, 132, Blocks.AIR.defaultBlockState());
        box(r, -2, -1, -2, 64, -1, 132, Blocks.STONE.defaultBlockState());
        Autarkia.LOG.info("[TEST m5] platform at {}", r.base().toShortString());
    }

    static Object be(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p);
    }

    private static void data(Run r) {
        SelfTest t = r.t();
        t.check("data: the M5 advancements, recipes, tags and template 9 load", () -> {
            for (String a : List.of("materials/tin", "materials/bronze", "materials/crushed", "materials/washed", "materials/steel", "logistics/root",
                "logistics/belt_line", "logistics/sorted", "logistics/rack_collapse", "workers/debris_crab", "equipment/bronze_age",
                "equipment/full_pressure", "colony/pattern_recorded")) {
                if (t.level.getServer().getAdvancements().get(Autarkia.id(a)) == null) {
                    return false;
                }
            }
            var map = t.level.recipeAccess().recipeMap();
            for (String id : List.of("washing/washed_iron", "washing/washed_zinc", "alloying/iron_from_washed", "alloying/steel_ingot", "alloying/brass_ingot",
                "pressing/steel_plate", "pressing/brass_plate", "drawing/copper_coil", "drawing/copper_wire_spool", "belt", "wood_rack", "pattern_loom",
                "steel_sledgehammer", "steam_repeater", "worker_assembly/hauler_crab", "crab_frame", "smelting/zinc_ingot_from_raw_zinc")) {
                if (map.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Autarkia.id(id))) == null) {
                    Autarkia.LOG.info("[TEST m5] missing recipe {}", id);
                    return false;
                }
            }
            var tpl = dev.autarkia.colony.ColonyPlans.templates().get("wood_racks");
            var plan = dev.autarkia.colony.ColonyPlans.plan(Autarkia.id("rack_row_wood"));
            return tpl != null && tpl.tier() == 2 && plan != null && plan.cells().size() == 8
                && new ItemStack(Items.IRON_INGOT).is(dev.autarkia.storage.RackBlockEntity.DENSE)
                && new ItemStack(Items.IRON_BLOCK).is(dev.autarkia.storage.RackBlockEntity.HEAVY);
        }, () -> "13 advancements, 17 recipes, template wood_racks, load tags");
    }

    private static int take(MachineBlockEntity m, Item item) {
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

    private static void sluice(Run r) {
        SelfTest t = r.t();
        BlockPos c = r.at(4, 0, 6);
        BlockPos crate = r.at(4, 0, 4);
        int[] fed = new int[1];
        int[] doneAt = new int[2];
        t.then(x -> {
            TestM1.placeBig(x, c, AutarkiaBlocks.SLUICE.get(), Direction.NORTH);
            TestM1.place(x, crate, AutarkiaBlocks.CRATE.get().defaultBlockState());
            box(r, 3, 0, 8, 3, 1, 10, Blocks.STONE.defaultBlockState());
            box(r, 5, 0, 8, 5, 1, 10, Blocks.STONE.defaultBlockState());
            box(r, 4, 0, 10, 4, 1, 10, Blocks.STONE.defaultBlockState());
            TestM1.place(x, r.at(4, 0, 9), Blocks.WATER.defaultBlockState());
        });
        t.waitUntil("03#11 flowing water reaches the head", 60, () -> be(t, c) instanceof SluiceBlockEntity s && s.water() == SluiceBlockEntity.Water.FLOWING,
            x -> { });
        t.waitUntil("03#11 the sluice washes 96 crushed iron into the crate in front of the tail", 96 * 100 + 600,
            () -> TestM1.crateCount(t, crate, AutarkiaItems.WASHED_IRON.get()) == 96, x -> {
                if (fed[0] < 96 && x.level.getBlockEntity(c) instanceof SluiceBlockEntity s && s.inventory().get(0).getCount() < 8) {
                    fed[0] += TestM1.insertAt(x.level, c, new ItemStack(AutarkiaItems.CRUSHED_IRON.get(), Math.min(8, 96 - fed[0])));
                }
            });
        t.check("03#11 96 washed iron and 4 to 16 zinc nuggets", () -> {
            int z = TestM1.crateCount(t, crate, AutarkiaItems.ZINC_NUGGET.get());
            return TestM1.crateCount(t, crate, AutarkiaItems.WASHED_IRON.get()) == 96 && z >= 4 && z <= 16;
        }, () -> TestM1.crateCount(t, crate, AutarkiaItems.WASHED_IRON.get()) + " washed, " + TestM1.crateCount(t, crate, AutarkiaItems.ZINC_NUGGET.get()) + " nuggets");
        t.then(x -> {
            box(r, 4, 0, 8, 4, 1, 9, Blocks.AIR.defaultBlockState());
            TestM1.insertAt(x.level, c, new ItemStack(AutarkiaItems.CRUSHED_IRON.get(), 2));
        });
        t.waitTicks(40);
        t.check("03#11 without water the sluice stops: NO_CONDITION", () -> be(t, c) instanceof SluiceBlockEntity s && s.water() == SluiceBlockEntity.Water.NONE
            && s.machineStatus().state() == MachineStatus.State.NO_CONDITION, () -> be(t, c) instanceof SluiceBlockEntity s ? s.machineStatus().state().toString() : "missing");
        t.then(x -> TestM1.place(x, r.at(4, 0, 8), Blocks.WATER.defaultBlockState()));
        t.waitUntil("03#11 with a source only the first item takes about 200 ticks", 400, () -> {
            boolean got = TestM1.crateCount(t, crate, AutarkiaItems.WASHED_IRON.get()) >= 97;
            return got;
        }, x -> {
            doneAt[0]++;
            if (x.level.getBlockEntity(c) instanceof SluiceBlockEntity s && s.water() != SluiceBlockEntity.Water.SOURCE) {
                doneAt[1]++;
            }
        });
        t.check("03#11 the source-only item took 195 to 210 ticks and the head saw only a source", () -> doneAt[0] >= 195 && doneAt[0] <= 210 && doneAt[1] <= 1,
            () -> doneAt[0] + " ticks, " + doneAt[1] + " ticks without a plain source");
    }

    private static void smelter(Run r) {
        SelfTest t = r.t();
        BlockPos c = r.at(12, 0, 6);
        int[] out = new int[4];
        int[] fed = new int[1];
        long[] start = new long[1];
        t.then(x -> {
            box(r, 12, -1, 6, 13, -1, 7, AutarkiaBlocks.FOUNDATION.get().defaultBlockState());
            boolean ok = TestM1.placeBig(x, c, AutarkiaBlocks.BLAST_SMELTER.get(), Direction.NORTH);
            start[0] = x.level.getGameTime();
            x.report("03#12 the blast smelter stands on 4 foundations", ok, c.toShortString());
        });
        t.waitUntil("03#12 the blast smelter makes 128 iron ingots from 96 washed iron", 96 / 3 * 200 + 900, () -> out[0] == 128, x -> {
            TestM1.pushEnergy(x, c, 256);
            if (x.level.getBlockEntity(c) instanceof BlastSmelterBlockEntity b) {
                if (fed[0] < 96 && b.inventory().queue() < 30) {
                    fed[0] += TestM1.insertAt(x.level, c, new ItemStack(AutarkiaItems.WASHED_IRON.get(), Math.min(24, 96 - fed[0])));
                }
                if (x.level.getGameTime() - start[0] < 100 && b.inventory().get(3).getCount() + b.inventory().get(4).getCount() > 0) {
                    out[3]++;
                }
                out[0] += take(b, Items.IRON_INGOT);
                out[1] += take(b, AutarkiaItems.SLAG.get());
            }
        });
        t.check("03#12 exactly 128 iron, 4 to 16 slag, nothing in the first 100 ticks", () -> out[0] == 128 && out[1] >= 4 && out[1] <= 16 && out[3] == 0,
            () -> out[0] + " iron, " + out[1] + " slag, early outputs " + out[3]);
        int[] steel = new int[2];
        t.then(x -> {
            TestM1.insertAt(x.level, c, new ItemStack(Items.IRON_INGOT, 10));
            TestM1.insertAt(x.level, c, new ItemStack(Items.COAL, 20));
        });
        t.waitUntil("03#13 10 iron + 20 coal make 10 steel", 10 * 200 + 400, () -> steel[0] == 10, x -> {
            TestM1.pushEnergy(x, c, 256);
            if (x.level.getBlockEntity(c) instanceof BlastSmelterBlockEntity b) {
                steel[0] += take(b, AutarkiaItems.STEEL_INGOT.get());
                steel[1] += take(b, AutarkiaItems.SLAG.get());
            }
        });
        t.check("03#13 10 steel ingots and 0 to 6 slag", () -> steel[0] == 10 && steel[1] <= 6, () -> steel[0] + " steel, " + steel[1] + " slag");
        t.then(x -> {
            x.level.setBlock(r.at(12, -1, 6), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            TestM1.insertAt(x.level, c, new ItemStack(AutarkiaItems.WASHED_IRON.get(), 3));
        });
        t.waitTicks(25);
        t.check("03 heavy machine: without its foundation the blast smelter is NO_CONDITION", () -> be(t, c) instanceof BlastSmelterBlockEntity b
            && b.machineStatus().state() == MachineStatus.State.NO_CONDITION,
            () -> be(t, c) instanceof BlastSmelterBlockEntity b ? b.machineStatus().state().toString() : "missing");
    }

    private static void forming(Run r) {
        SelfTest t = r.t();
        BlockPos press = r.at(20, 0, 6);
        BlockPos drawer = r.at(22, 0, 6);
        int[] got = new int[4];
        t.then(x -> {
            TestM1.place(x, press, AutarkiaBlocks.PLATE_PRESS.get().defaultBlockState());
            TestM1.place(x, drawer, AutarkiaBlocks.WIRE_DRAWER.get().defaultBlockState());
            TestM1.insertAt(x.level, press, new ItemStack(AutarkiaItems.STEEL_INGOT.get(), 10));
            TestM1.insertAt(x.level, drawer, new ItemStack(Items.COPPER_INGOT, 8));
            TestM1.insertAt(x.level, drawer, new ItemStack(Items.IRON_INGOT, 2));
        });
        t.waitUntil("03#14 10 steel plates and 4 copper coils", 10 * 80 + 600, () -> got[0] == 10 && got[1] == 4, x -> {
            TestM1.pushEnergy(x, press, 200);
            TestM1.pushEnergy(x, drawer, 200);
            if (x.level.getBlockEntity(press) instanceof MachineBlockEntity m) {
                got[0] += take(m, AutarkiaItems.STEEL_PLATE.get());
            }
            if (x.level.getBlockEntity(drawer) instanceof MachineBlockEntity m) {
                got[1] += take(m, AutarkiaItems.COPPER_COIL.get());
            }
        });
        t.then(x -> TestM1.insertAt(x.level, drawer, new ItemStack(Items.COPPER_INGOT, 4)));
        t.waitUntil("03#14 without a core the drawer makes no coil (2 copper make a wire spool)", 600, () -> got[3] == 2, x -> {
            TestM1.pushEnergy(x, drawer, 200);
            if (x.level.getBlockEntity(drawer) instanceof MachineBlockEntity m) {
                got[2] += take(m, AutarkiaItems.COPPER_COIL.get());
                got[3] += take(m, AutarkiaItems.COPPER_WIRE_SPOOL.get());
            }
        });
        t.check("03#14 no coil without a core", () -> got[2] == 0, () -> got[2] + " coils, " + got[3] + " spools");
    }

    private static void equipment(Run r) {
        SelfTest t = r.t();
        FakePlayer fake = FakePlayerFactory.getMinecraft(t.level);
        t.check("07#4 a steel pickaxe drops obsidian (diamond level), a bronze pickaxe drops nothing", () -> {
            BlockState obs = Blocks.OBSIDIAN.defaultBlockState();
            return new ItemStack(AutarkiaItems.STEEL_PICKAXE.get()).isCorrectToolForDrops(obs)
                && !new ItemStack(AutarkiaItems.BRONZE_PICKAXE.get()).isCorrectToolForDrops(obs);
        }, () -> "obsidian is in #minecraft:needs_diamond_tool like vantite_ore");
        int[] broken = new int[3];
        int[] used = new int[3];
        t.then(x -> {
            ItemStack hammer = new ItemStack(AutarkiaItems.STEEL_SLEDGEHAMMER.get());
            hammer.set(AutarkiaComponents.TOOL_MODE.get(), ToolMode.AREA3);
            fake.setItemInHand(InteractionHand.MAIN_HAND, hammer);
            Direction[] faces = {Direction.NORTH, Direction.UP, Direction.EAST};
            BlockPos[] centers = {r.at(32, 2, 6), r.at(40, 2, 6), r.at(48, 2, 6)};
            for (int k = 0; k < 3; k++) {
                box(r, 30 + k * 8, 0, 4, 34 + k * 8, 4, 8, Blocks.STONE.defaultBlockState());
                int before = hammer.getDamageValue();
                AreaMining.remember(fake, faces[k]);
                fake.gameMode.destroyBlock(centers[k]);
                used[k] = hammer.getDamageValue() - before;
                int air = 0;
                for (int a = -1; a <= 1; a++) {
                    for (int b = -1; b <= 1; b++) {
                        BlockPos p = switch (faces[k].getAxis()) {
                            case X -> centers[k].offset(0, a, b);
                            case Y -> centers[k].offset(a, 0, b);
                            case Z -> centers[k].offset(a, b, 0);
                        };
                        air += x.level.getBlockState(p).isAir() ? 1 : 0;
                    }
                }
                broken[k] = air;
            }
            x.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, AABB.encapsulatingFullBlocks(r.at(28, 0, 2), r.at(52, 6, 10))).forEach(e -> e.discard());
        });
        t.check("07#5 the sledgehammer in 3x3 breaks 9 blocks in the plane of each of 3 faces, 9 durability each", () -> broken[0] == 9 && broken[1] == 9
            && broken[2] == 9 && used[0] == 9 && used[1] == 9 && used[2] == 9, () -> java.util.Arrays.toString(broken) + " broken, " + java.util.Arrays.toString(used) + " used");
        int[] shots = new int[3];
        t.then(x -> {
            ItemStack rep = new ItemStack(AutarkiaItems.STEAM_REPEATER.get());
            fake.setItemInHand(InteractionHand.MAIN_HAND, rep);
            fake.getInventory().clearContent();
            fake.setItemInHand(InteractionHand.MAIN_HAND, rep);
            fake.getInventory().add(new ItemStack(AutarkiaItems.STEEL_NUGGET.get(), 10));
            fake.snapTo(r.at(56, 1, 20).getX() + 0.5, r.at(56, 1, 20).getY(), r.at(56, 1, 20).getZ() + 0.5, 0, -10);
            fake.setShiftKeyDown(true);
            int dur = rep.getUseDuration(fake);
            for (int tick = 0; tick < 40; tick++) {
                rep.getItem().onUseTick(x.level, fake, rep, dur - tick);
            }
            shots[2] = SteamRepeaterItem.pressure(rep);
            fake.setShiftKeyDown(false);
            for (int tick = 40; tick < 100; tick++) {
                rep.getItem().onUseTick(x.level, fake, rep, dur - tick);
            }
            shots[0] = x.level.getEntitiesOfClass(SlingShotEntity.class, new AABB(r.at(56, 1, 20)).inflate(64)).size();
            shots[1] = SteamRepeaterItem.pressure(rep);
            int left = 0;
            for (ItemStack s : fake.getInventory()) {
                if (s.is(AutarkiaItems.STEEL_NUGGET.get())) {
                    left += s.getCount();
                }
            }
            x.report("07#6 the steam repeater pumps to 100 in 40 ticks and fires 10 shots to 0 pressure with 10 nuggets",
                shots[2] == 100 && shots[0] == 10 && shots[1] == 0 && left == 0 && rep.getDamageValue() == 10,
                "pumped " + shots[2] + ", " + shots[0] + " shots, pressure " + shots[1] + ", " + left + " nuggets left, " + rep.getDamageValue() + " durability");
            x.level.getEntitiesOfClass(SlingShotEntity.class, new AABB(r.at(56, 1, 20)).inflate(64)).forEach(e -> e.discard());
        });
    }
}

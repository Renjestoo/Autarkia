package dev.autarkia.command;

import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.ColonySensors;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.logistics.BeltBlockEntity;
import dev.autarkia.logistics.FilterBlockEntity;
import dev.autarkia.logistics.TransitSegment;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.storage.CrateBlockEntity;
import dev.autarkia.storage.RackBlock;
import dev.autarkia.storage.RackBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * The logistics part of {@code /autarkia test m5} inside the colony domain: 04 L3 to L7 (a 40-belt line: speed, power
 * cut, jam with the sensor, breaking one loaded belt, the splitter in ALTERNATE), L10 (belt and splitter entries survive
 * a save and load of their block entity), L11 and L12 (a wood rack at 127% collapses after 1,200 ticks and its crates
 * land with every item; one at 115% only bends for 6,000 ticks).
 */
final class TestM5Logistics {
    private static final int Z = 100;
    private static final int X0 = 8;
    private static final int LEN = 40;

    private TestM5Logistics() {
    }

    private static BeltBlockEntity belt(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof BeltBlockEntity b ? b : null;
    }

    private static int entries(SelfTest t, TestM5.Run r, int from, int to) {
        int n = 0;
        for (int i = from; i <= to; i++) {
            BeltBlockEntity b = belt(t, r.at(X0 + i, 0, Z));
            if (b != null) {
                for (TransitSegment.Entry e : b.segment().entries) {
                    n += e.stack.getCount();
                }
            }
        }
        return n;
    }

    private static long posSum(SelfTest t, TestM5.Run r) {
        long s = 0;
        for (int i = 0; i < LEN; i++) {
            BeltBlockEntity b = belt(t, r.at(X0 + i, 0, Z));
            if (b != null) {
                for (TransitSegment.Entry e : b.segment().entries) {
                    s += e.pos * 31L + i * 1000L;
                }
            }
        }
        return s;
    }

    static void run(TestM5.Run r) {
        line(r);
        splitter(r);
        racks(r);
    }

    private static void line(TestM5.Run r) {
        SelfTest t = r.t();
        BlockPos head = r.at(X0, 0, Z);
        BlockPos crate = r.at(X0 + LEN, 0, Z);
        boolean[] power = {true};
        int[] fed = new int[2];
        long[] first = new long[2];
        t.then(x -> {
            for (int i = 0; i < LEN; i++) {
                TestM1.place(x, r.at(X0 + i, 0, Z), AutarkiaBlocks.BELT.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.EAST));
            }
            TestM1.place(x, crate, AutarkiaBlocks.CRATE.get().defaultBlockState());
        });
        t.check("04 belts: 40 belts form one line with its head at the first belt", () -> belt(t, head) != null && belt(t, head).line().size() == LEN
            && belt(t, r.at(X0 + 20, 0, Z)).headPos().equals(head), () -> belt(t, head) == null ? "missing" : belt(t, head).line().size() + " belts");
        t.waitUntil("04 L3 the first iron ingot arrives", 900, () -> TestM1.crateCount(t, crate, Items.IRON_INGOT) > 0, x -> {
            feed(x, head, power, fed, 64, first);
            if (first[1] == 0 && TestM1.crateCount(x, crate, Items.IRON_INGOT) > 0) {
                first[1] = x.level.getGameTime();
            }
        });
        t.check("04 L3 items move at 2 blocks/s: 39.5 blocks in 380 to 420 ticks", () -> first[1] - first[0] >= 380 && first[1] - first[0] <= 420,
            () -> (first[1] - first[0]) + " ticks");
        t.waitUntil("04 L3 the crate fills with the 64 ingots", 1200, () -> TestM1.crateCount(t, crate, Items.IRON_INGOT) == 64,
            x -> feed(x, head, power, fed, 64, first));
        long[] before = new long[1];
        t.then(x -> fed[0] = 0);
        t.repeat(120, x -> feed(x, head, power, fed, 16, first));
        t.then(x -> {
            power[0] = false;
            belt(x, head).lineBuffer().drain(belt(x, head).lineBuffer().stored());
        });
        t.waitTicks(4);
        t.then(x -> before[0] = posSum(x, r));
        t.waitTicks(100);
        t.check("04 L4 without power every item stops in place and the belts say No power", () -> posSum(t, r) == before[0] && entries(t, r, 0, LEN - 1) > 0
            && !belt(t, head).powered() && !belt(t, r.at(X0 + LEN - 1, 0, Z)).powered(), () -> entries(t, r, 0, LEN - 1) + " items on the line");
        t.then(x -> power[0] = true);
        t.waitUntil("04 L4 with power back the items resume and reach the crate", 900, () -> TestM1.crateCount(t, crate, Items.IRON_INGOT) == 80,
            x -> feed(x, head, power, fed, 16, first));
        t.then(x -> {
            if (x.level.getBlockEntity(crate) instanceof CrateBlockEntity c) {
                for (int i = 0; i < CrateBlockEntity.SLOTS; i++) {
                    c.items().set(i, ItemResource.of(new ItemStack(Items.STONE)), 64);
                }
            }
            fed[0] = 0;
        });
        t.waitUntil("04 L5 items compress to half-block spacing and stop at the full crate", 1200, () -> entries(t, r, LEN - 10, LEN - 1) == 20,
            x -> feed(x, head, power, fed, 20, first));
        t.repeat(AutarkiaConfig.JAM_WARNING_TICKS.get() + 20, x -> TestM1.pushEnergy(x, head, 80));
        t.check("04 L5 after 30 s the line is JAMMED: red stripe state on the end belt and transport.belt_jammed_lines = 1", () -> {
            BeltBlockEntity end = belt(t, r.at(X0 + LEN - 1, 0, Z));
            Colony c = TestM5Colony.colony();
            if (end == null || !end.jammed() || c == null || end.segment().entries.size() < 2) {
                return false;
            }
            ColonySensors.sample(c, t.level);
            return ColonyRuntime.of(c).snapshot.getOrDefault("transport.belt_jammed_lines", 0.0) == 1.0
                && end.segment().entries.get(0).pos - end.segment().entries.get(1).pos >= BeltBlockEntity.SPACING;
        }, () -> {
            Colony c = TestM5Colony.colony();
            return c == null ? "no colony" : "sensor " + ColonyRuntime.of(c).snapshot.get("transport.belt_jammed_lines");
        });
        saveFormat(r, r.at(X0 + LEN - 1, 0, Z));
        int[] counts = new int[4];
        t.then(x -> {
            BlockPos cut = r.at(X0 + LEN - 4, 0, Z);
            TestM1.killItems(x, cut, 4);
            counts[0] = entries(x, r, LEN - 4, LEN - 4);
            counts[1] = entries(x, r, 0, LEN - 1) - counts[0];
            x.level.destroyBlock(cut, false);
        });
        t.waitTicks(2);
        t.check("04 L6 breaking one loaded belt drops only its items; the others stay on their belts", () -> {
            int dropped = TestM1.countItems(t, r.at(X0 + LEN - 4, 0, Z), 2, Items.IRON_INGOT);
            int rest = entries(t, r, 0, LEN - 5) + entries(t, r, LEN - 3, LEN - 1);
            return counts[0] > 0 && dropped == counts[0] && rest == counts[1];
        }, () -> counts[0] + " on the cut belt, " + TestM1.countItems(t, r.at(X0 + LEN - 4, 0, Z), 2, Items.IRON_INGOT) + " dropped, "
            + (entries(t, r, 0, LEN - 5) + entries(t, r, LEN - 3, LEN - 1)) + " of " + counts[1] + " remain");
    }

    private static void feed(SelfTest x, BlockPos head, boolean[] power, int[] fed, int max, long[] first) {
        if (power[0]) {
            TestM1.pushEnergy(x, head, 80);
        }
        if (fed[0] < max && x.level.getGameTime() % 4 == 0 && TestM1.insertAt(x.level, head, new ItemStack(Items.IRON_INGOT)) == 1) {
            if (fed[0] == 0 && first[0] == 0) {
                first[0] = x.level.getGameTime();
            }
            fed[0]++;
        }
    }

    private static void saveFormat(TestM5.Run r, BlockPos endBelt) {
        SelfTest t = r.t();
        t.check("04 L10 belt entries keep id, position and stack through a save and load of the block entity", () -> {
            BeltBlockEntity b = belt(t, endBelt);
            if (b == null || b.segment().entries.isEmpty()) {
                return false;
            }
            CompoundTag tag = b.saveWithFullMetadata(t.level.registryAccess());
            BlockEntity copy = BlockEntity.loadStatic(endBelt, b.getBlockState(), tag, t.level.registryAccess());
            return copy instanceof BeltBlockEntity cb && same(b.segment(), cb.segment());
        }, () -> belt(t, endBelt) == null ? "missing" : belt(t, endBelt).segment().entries.size() + " entries");
    }

    private static boolean same(TransitSegment a, TransitSegment b) {
        if (a.entries.size() != b.entries.size() || a.blocked != b.blocked) {
            return false;
        }
        for (int i = 0; i < a.entries.size(); i++) {
            TransitSegment.Entry x = a.entries.get(i);
            TransitSegment.Entry y = b.entries.get(i);
            if (x.id != y.id || x.pos != y.pos || !ItemStack.matches(x.stack, y.stack)) {
                return false;
            }
        }
        return true;
    }

    private static void splitter(TestM5.Run r) {
        SelfTest t = r.t();
        int z = Z + 8;
        BlockPos head = r.at(X0, 0, z);
        BlockPos split = r.at(X0 + 2, 0, z);
        BlockPos front = r.at(X0 + 3, 0, z);
        BlockPos side = r.at(X0 + 2, 0, z - 1);
        int[] fed = new int[1];
        t.then(x -> {
            for (int i = 0; i < 2; i++) {
                TestM1.place(x, r.at(X0 + i, 0, z), AutarkiaBlocks.BELT.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.EAST));
            }
            TestM1.place(x, split, AutarkiaBlocks.BELT_SPLITTER.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.EAST));
            TestM1.place(x, front, AutarkiaBlocks.CRATE.get().defaultBlockState());
            TestM1.place(x, side, AutarkiaBlocks.CRATE.get().defaultBlockState());
        });
        t.waitUntil("04 L7 the splitter in ALTERNATE passes 64 items", 1600, () -> TestM1.crateCount(t, front, Items.COPPER_INGOT)
            + TestM1.crateCount(t, side, Items.COPPER_INGOT) == 64, x -> feedOne(x, head, fed, 64, Items.COPPER_INGOT));
        t.check("04 L7 32 go to the front and 32 to the side", () -> TestM1.crateCount(t, front, Items.COPPER_INGOT) == 32
            && TestM1.crateCount(t, side, Items.COPPER_INGOT) == 32, () -> TestM1.crateCount(t, front, Items.COPPER_INGOT) + " front, "
            + TestM1.crateCount(t, side, Items.COPPER_INGOT) + " side");
        t.check("04 L10 the splitter buffer survives a save and load", () -> t.level.getBlockEntity(split) instanceof FilterBlockEntity f
            && BlockEntity.loadStatic(split, f.getBlockState(), f.saveWithFullMetadata(t.level.registryAccess()), t.level.registryAccess()) instanceof FilterBlockEntity g
            && same(f.segment(), g.segment()) && g.isSplitter(), () -> "splitter");
        t.then(x -> {
            if (x.level.getBlockEntity(side) instanceof CrateBlockEntity c) {
                for (int i = 0; i < CrateBlockEntity.SLOTS; i++) {
                    c.items().set(i, ItemResource.of(new ItemStack(Items.STONE)), 64);
                }
            }
            fed[0] = 0;
        });
        t.waitUntil("04 L7 with the side blocked all 16 more items go to the front", 800, () -> TestM1.crateCount(t, front, Items.COPPER_INGOT) == 48,
            x -> feedOne(x, head, fed, 16, Items.COPPER_INGOT));
    }

    private static void feedOne(SelfTest x, BlockPos head, int[] fed, int max, Item item) {
        TestM1.pushEnergy(x, head, 40);
        if (fed[0] < max && x.level.getGameTime() % 6 == 0 && TestM1.insertAt(x.level, head, new ItemStack(item)) == 1) {
            fed[0]++;
        }
    }

    /** Return a crate item holding count items of one kind (64 per slot). */
    static ItemStack filledCrate(Item item, int count) {
        List<ItemStack> list = new ArrayList<>();
        int left = count;
        for (int i = 0; i < CrateBlockEntity.SLOTS; i++) {
            int n = Math.min(64, left);
            list.add(n > 0 ? new ItemStack(item, n) : ItemStack.EMPTY);
            left -= n;
        }
        ItemStack s = new ItemStack(AutarkiaItems.CRATE.get());
        s.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
        return s;
    }

    private static RackBlockEntity rack(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof RackBlockEntity rb ? rb : null;
    }

    private static int[] landed(SelfTest t, BlockPos around) {
        int[] n = new int[3];
        for (BlockPos p : BlockPos.betweenClosed(around.offset(-6, -2, -6), around.offset(6, 3, 6))) {
            if (t.level.getBlockEntity(p) instanceof CrateBlockEntity c) {
                n[0]++;
                for (int i = 0; i < CrateBlockEntity.SLOTS; i++) {
                    ItemStack s = c.slot(i);
                    n[1] += s.is(Items.IRON_INGOT) ? s.getCount() : 0;
                    n[2] += s.is(Items.COBBLESTONE) ? s.getCount() : 0;
                }
            }
        }
        return n;
    }

    private static void racks(TestM5.Run r) {
        SelfTest t = r.t();
        BlockPos a = r.at(20, 0, 112);
        BlockPos b = r.at(36, 0, 112);
        long[] start = new long[1];
        t.then(x -> {
            TestM5.box(r, 14, 0, 113, 42, 3, 113, Blocks.STONE.defaultBlockState());
            for (BlockPos p : List.of(a, b)) {
                TestM1.place(x, p, AutarkiaBlocks.WOOD_RACK.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
            }
            rack(x, a).insertCrate(filledCrate(Items.IRON_INGOT, 1728));
            rack(x, a).insertCrate(filledCrate(Items.COBBLESTONE, 1728));
            rack(x, b).insertCrate(filledCrate(Items.IRON_INGOT, 1728));
            rack(x, b).insertCrate(filledCrate(Items.COBBLESTONE, 1222));
            start[0] = x.level.getGameTime();
        });
        t.check("04 L11 the 127% rack is in stage 3 at once with a 60 s countdown and bend = 3", () -> rack(t, a) != null && rack(t, a).stage() == 3
            && rack(t, a).secondsLeft() == 60 && t.level.getBlockState(a).getValue(RackBlock.BEND) == 3 && Math.round(rack(t, a).ratio() * 100) == 127,
            () -> rack(t, a) == null ? "missing" : Math.round(rack(t, a).ratio() * 100) + "%, stage " + rack(t, a).stage() + ", " + rack(t, a).secondsLeft() + " s");
        t.check("04 L12 the 115% rack is in stage 2 (bend = 2)", () -> rack(t, b) != null && rack(t, b).stage() == 2
            && t.level.getBlockState(b).getValue(RackBlock.BEND) == 2, () -> rack(t, b) == null ? "missing" : Math.round(rack(t, b).ratio() * 1000) / 10.0 + "%");
        t.check("04 S10 and storage.racks_critical see the critical rack; the board shows its warning", () -> {
            Colony c = TestM5Colony.colony();
            if (c == null) {
                return false;
            }
            ColonySensors.sample(c, t.level);
            ColonyRuntime rt = ColonyRuntime.of(c);
            return rt.snapshot.getOrDefault("S10", 0.0) >= 1.26 && rt.snapshot.getOrDefault("storage.racks_critical", 0.0) == 1.0
                && rt.rackWarning.contains("Rack critical");
        }, () -> TestM5Colony.colony() == null ? "no colony" : ColonyRuntime.of(TestM5Colony.colony()).snapshot.get("S10") + ", "
            + ColonyRuntime.of(TestM5Colony.colony()).rackWarning);
        t.waitUntil("04 L11 the rack collapses after 1,200 ticks", 1300, () -> !t.level.getBlockState(a).is(AutarkiaBlocks.WOOD_RACK.get()), x -> { });
        t.check("04 L11 it held for 1,200 ticks", () -> t.level.getGameTime() - start[0] >= 1199 && t.level.getGameTime() - start[0] <= 1205,
            () -> (t.level.getGameTime() - start[0]) + " ticks");
        t.waitUntil("04 L11 two crate blocks land with all 1,728 iron and 1,728 cobblestone", 600, () -> {
            int[] n = landed(t, a);
            return n[0] == 2 && n[1] == 1728 && n[2] == 1728;
        }, x -> { });
        t.check("04 L11 the salvage drops and the colony remembers rack_collapse", () -> TestM1.countItems(t, a, 3, Items.STICK) == 3
            && TestM5Colony.colony() != null && TestM5Colony.colony().memories.stream().anyMatch(m -> m.type().equals("rack_collapse")),
            () -> TestM1.countItems(t, a, 3, Items.STICK) + " sticks");
        t.waitUntil("04 L12 6,000 ticks pass with the 115% rack standing", 6000, () -> t.level.getGameTime() - start[0] >= 6000 || rack(t, b) == null, x -> { });
        t.check("04 L12 no collapse, stage 2", () -> rack(t, b) != null && rack(t, b).stage() == 2 && t.level.getGameTime() - start[0] >= 6000,
            () -> rack(t, b) == null ? "collapsed" : (t.level.getGameTime() - start[0]) + " ticks, stage " + rack(t, b).stage());
    }
}

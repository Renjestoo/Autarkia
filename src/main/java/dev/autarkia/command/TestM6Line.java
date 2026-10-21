package dev.autarkia.command;

import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.PatternLoomBlockEntity;
import dev.autarkia.colony.Project;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.SteamTurbineBlockEntity;
import dev.autarkia.logistics.BeltBlockEntity;
import dev.autarkia.processing.MachineBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * W11 of {@code /autarkia test m6}: a jaw crusher line fed by a belt (on a foundation floor, powered from a steam
 * turbine through an insulated span) is recorded as a LINE Pattern. With a backlog of 40 raw ores and fed 1 ore per 80
 * ticks (one crusher works 1 per 100), its queue stays above 32; the Autonomous colony proposes duplicate_line with a
 * belt splitter on the feed belt, builds the copy with its belt route and power span, and with the feed split the queue
 * drops below 32 in both lines.
 */
final class TestM6Line {
    private static final int Y = 0;

    private TestM6Line() {
    }

    private static void floor(TestM6.Run r) {
        TestM6.box(r, 60, -1, 118, 92, -1, 141, AutarkiaBlocks.FOUNDATION.get().defaultBlockState());
    }

    private static List<MachineBlockEntity> crushers(SelfTest t) {
        List<MachineBlockEntity> out = new ArrayList<>();
        Colony c = TestM6Colony.colony();
        for (BlockPos p : ColonyRuntime.of(c).machines) {
            if (t.level.getBlockEntity(p) instanceof dev.autarkia.processing.JawCrusherBlockEntity m) {
                out.add(m);
            }
        }
        return out;
    }

    private static void service(SelfTest t, TestM6.Run r, BlockPos turbine, int[] fed, boolean feed) {
        TestM6Colony.powerDocks(t, r);
        if (t.level.getBlockEntity(turbine) instanceof SteamTurbineBlockEntity tb) {
            tb.buffer().fill(20000);
        }
        for (BlockPos p : BlockPos.betweenClosed(r.at(60, Y, 118), r.at(92, Y, 141))) {
            var be = t.level.getBlockEntity(p);
            if (be instanceof BeltBlockEntity || be instanceof dev.autarkia.logistics.FilterBlockEntity) {
                TestM1.pushEnergy(t, p.immutable(), 64);
            }
        }
        for (MachineBlockEntity m : crushers(t)) {
            TestM6.take(m, AutarkiaItems.CRUSHED_COPPER.get());
        }
        if (feed && t.level.getGameTime() % 80 == 0) {
            fed[0] += TestM1.insertAt(t.level, r.at(64, Y, 125), new ItemStack(Items.RAW_COPPER, 1));
        }
    }

    private static long lineInstances(SelfTest t, BlockPos loom) {
        var ref = ((PatternLoomBlockEntity) t.level.getBlockEntity(loom)).cardStack().get(AutarkiaComponents.PATTERN_REF.get());
        return ref == null ? 0 : TestM6Colony.colony().instances.stream().filter(in -> in.watched() && in.pattern().equals(ref.id())).count();
    }

    static void run(TestM6.Run r) {
        SelfTest t = r.t();
        BlockPos loom = r.at(74, Y, 121);
        BlockPos crusher = r.at(73, Y, 125);
        BlockPos pylon = r.at(75, Y, 126);
        BlockPos gen = r.at(75, Y, 132);
        BlockPos turbine = r.at(77, Y, 132);
        int[] fed = new int[1];
        Project[] dup = new Project[1];
        t.then(x -> {
            floor(r);
            TestM1.placeBig(x, crusher, AutarkiaBlocks.JAW_CRUSHER.get(), Direction.NORTH);
            for (int dx = 64; dx <= 72; dx++) {
                TestM1.place(x, r.at(dx, Y, 125), AutarkiaBlocks.BELT.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.EAST));
            }
            TestM6.pylon(x, pylon);
            TestM6.pylon(x, gen);
            TestM1.placeBig(x, turbine, AutarkiaBlocks.STEAM_TURBINE.get(), Direction.NORTH);
            Grid.SpanResult s = Grid.addSpan(x.level, pylon, gen, CableTier.INSULATED);
            TestM1.insertAt(x.level, crusher, new ItemStack(Items.RAW_COPPER, 40));
            TestM1.place(x, loom, AutarkiaBlocks.PATTERN_LOOM.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
            String err = TestM6Colony.capture(x, loom, r.at(72, Y, 124), new net.minecraft.core.Vec3i(4, 5, 4), "Crusher Line");
            Colony c = TestM6Colony.colony();
            c.templates.forEach((k, v) -> v.enabled = k.equals("duplicate_line"));
            c.autonomy = Colony.Autonomy.AUTONOMOUS;
            TestM6Colony.clearStock(x, r);
            TestM6Colony.stock(x, r, new ItemStack(AutarkiaItems.JAW_CRUSHER.get(), 1), new ItemStack(AutarkiaItems.BELT.get(), 48),
                new ItemStack(AutarkiaItems.BELT_SPLITTER.get(), 1), new ItemStack(AutarkiaItems.STEEL_PYLON.get(), 1),
                new ItemStack(AutarkiaItems.INSULATED_CABLE_SPOOL.get(), 4));
            x.report("W11 rig: belt-fed jaw crusher, pylon with an insulated span to the turbine pylon, recorded at the loom", s == Grid.SpanResult.OK && err == null,
                s + ", " + err);
        });
        t.waitUntil("W11 the capture is a LINE Pattern in the library", 60, () -> {
            var ref = ((PatternLoomBlockEntity) t.level.getBlockEntity(loom)).cardStack().get(AutarkiaComponents.PATTERN_REF.get());
            return ref != null && TestM6Colony.colony().library.stream().anyMatch(e -> e.pattern().equals(ref.id()) && e.role().equalsIgnoreCase("line"));
        }, x -> service(x, r, turbine, fed, false));
        t.waitUntil("W11 fed faster than one crusher works, the queue passes 32 (S12 jaw_crusher)", 12000, () -> ColonyRuntime.of(TestM6Colony.colony())
            .snapshot.getOrDefault("S12.jaw_crusher", 0.0) >= 32, x -> service(x, r, turbine, fed, true));
        t.waitUntil("W11 after the 2,400-tick hold the colony proposes duplicate_line", 6000, () -> {
            dup[0] = TestM6Colony.colony().projects.stream().filter(p -> p.template.getPath().equals("duplicate_line")).findFirst().orElse(null);
            return dup[0] != null;
        }, x -> service(x, r, turbine, fed, true), () -> ColonyCommand.explainText(TestM6Colony.colony(), t.level, "duplicate_line") + "; log "
            + ColonyRuntime.of(TestM6Colony.colony()).log.subList(Math.max(0, ColonyRuntime.of(TestM6Colony.colony()).log.size() - 4),
                ColonyRuntime.of(TestM6Colony.colony()).log.size()));
        t.check("W11 the proposal puts a belt splitter on the feed belt and plans the copy with its power span", () -> dup[0] != null && dup[0].cells.stream().anyMatch(cl ->
            cl.state().is(AutarkiaBlocks.BELT_SPLITTER.get()) && BlockPos.of(cl.pos()).getZ() == r.at(0, 0, 125).getZ() && BlockPos.of(cl.pos()).getX() <= r.at(72, 0, 0)
            .getX()) && dup[0].cells.stream().anyMatch(cl -> cl.state().is(AutarkiaBlocks.JAW_CRUSHER.get())) && !dup[0].spans.isEmpty(),
            () -> dup[0] == null ? "no project" : dup[0].cells.size() + " cells, " + dup[0].spans.size() + " spans");
        t.then(x -> dev.autarkia.colony.ColonyClock.skip(TestM6Colony.colony(), 24000));
        t.waitUntil("W11 the drones build the second line", 20000, () -> dup[0] != null && dup[0].state == Project.State.DONE, x -> service(x, r, turbine, fed, true),
            () -> TestM6Colony.projectState(t, dup[0]));
        t.waitUntil("W11 with both lines running the queue drops below 32 in both", 15000, () -> crushers(t).size() == 2 && crushers(t).stream()
            .allMatch(m -> m.inventory().queue() < 32), x -> service(x, r, turbine, fed, true));
        t.check("W11 the copy is a watched instance of the Pattern", () -> lineInstances(t, loom) >= 2, () -> lineInstances(t, loom) + " instances of the line Pattern");
        t.then(x -> TestM6Colony.colony().templates.forEach((k, v) -> v.enabled = true));
    }
}

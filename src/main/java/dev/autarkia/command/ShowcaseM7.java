package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.logistics.CableCarStationBlockEntity;
import dev.autarkia.logistics.CrateLauncherBlockEntity;
import dev.autarkia.processing.OreOutcropBlock;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * {@code /autarkia showcase m7}: builds the T4 scene in front of the caller and drives it: an HV span between two
 * {@code hv_pylon}s with a wind tower and a lamp, a hydro turbine under a falling water column, a magma tap on a magma
 * vent, a froth flotation plant on foundation with crushed vantite, the vantite ore, outcrop and storage blocks, an
 * outpost core, a depot, a cable car line with two gondolas carrying cobblestone, and a crate launcher shooting full
 * crates into a catch net.
 */
public final class ShowcaseM7 {
    private record Driver(ServerLevel level, BlockPos plant, BlockPos stationA, BlockPos stationB, BlockPos launcher, long start, long until) {
    }

    private static final List<Driver> DRIVERS = new ArrayList<>();

    private ShowcaseM7() {
    }

    /** Register the driver tick. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> DRIVERS.removeIf(ShowcaseM7::drive));
    }

    private static boolean drive(Driver d) {
        ServerLevel l = d.level;
        long age = l.getGameTime() - d.start;
        TestM1.pushEnergyAt(l, d.plant, 800);
        for (BlockPos s : List.of(d.stationA, d.stationB)) {
            if (l.getBlockEntity(s) instanceof CableCarStationBlockEntity st) {
                st.energy().fill(CableCarStationBlockEntity.CAPACITY);
            }
        }
        if (age % 40 == 0 && l.getBlockEntity(d.stationA) instanceof CableCarStationBlockEntity st && st.inputEmpty()) {
            TestM1.insertAt(l, d.stationA, new ItemStack(Items.COBBLESTONE, 64));
        }
        if (l.getBlockEntity(d.launcher) instanceof CrateLauncherBlockEntity lb) {
            lb.energy().fill(CrateLauncherBlockEntity.CAPACITY);
            if (age % 100 == 0) {
                lb.inventory().put(CrateLauncherBlockEntity.CRATE_SLOT, new ItemStack(AutarkiaItems.CRATE.get(), 4));
                for (int i = 0; i < CrateLauncherBlockEntity.SLOTS; i++) {
                    lb.inventory().put(i, new ItemStack(Items.COBBLESTONE, 32));
                }
            }
        }
        if (age % 200 == 0 && l.getBlockEntity(d.plant) instanceof dev.autarkia.processing.MachineBlockEntity mb) {
            for (int i = mb.inventory().inputs(); i < mb.inventory().size(); i++) {
                mb.inventory().put(i, ItemStack.EMPTY);
            }
            TestM1.insertAt(l, d.plant, new ItemStack(AutarkiaItems.CRUSHED_VANTITE.get(), 32));
        }
        return l.getGameTime() >= d.until;
    }

    private static void set(ServerLevel l, BlockPos p, BlockState s) {
        l.setBlock(p, s, Block.UPDATE_ALL);
    }

    /** Put a water source at p held by stone on its open sides and below. */
    private static void water(ServerLevel l, BlockPos p) {
        for (Direction d : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN)) {
            if (l.getBlockState(p.relative(d)).isAir()) {
                set(l, p.relative(d), Blocks.STONE.defaultBlockState());
            }
        }
        set(l, p, Blocks.WATER.defaultBlockState());
    }

    /** Build the M7 scene and start its driver. */
    public static int build(CommandSourceStack src, int ticks) {
        ServerLevel l = src.getLevel();
        BlockPos o = BlockPos.containing(src.getPosition()).relative(src.getPlayer() != null ? src.getPlayer().getDirection() : Direction.SOUTH, 4);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-24, 0, -3), o.offset(24, 16, 36))) {
            set(l, p, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-24, -2, -3), o.offset(24, -1, 36))) {
            set(l, p, Blocks.STONE.defaultBlockState());
        }
        AABB box = AABB.encapsulatingFullBlocks(o.offset(-26, -2, -5), o.offset(26, 18, 38));
        l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, box, e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(e -> e.discard());
        SelfTest.clearSpans(l, box);

        BlockPos p1 = o.offset(-22, 0, 4);
        BlockPos p2 = o.offset(-2, 0, 4);
        TestM1.placeBigAt(l, p1, AutarkiaBlocks.HV_PYLON.get(), Direction.NORTH);
        TestM1.placeBigAt(l, p2, AutarkiaBlocks.HV_PYLON.get(), Direction.NORTH);
        TestM1.placeBigAt(l, o.offset(-22, 0, 8), AutarkiaBlocks.WIND_TOWER.get(), Direction.NORTH);
        set(l, p1.south(), AutarkiaBlocks.INDUSTRIAL_LAMP.get().defaultBlockState());
        set(l, p2.south(), AutarkiaBlocks.INDUSTRIAL_LAMP.get().defaultBlockState());
        Grid.SpanResult span = Grid.addSpan(l, p1, p2, CableTier.HV);

        BlockPos hydro = o.offset(-16, 0, 13);
        TestM1.placeBigAt(l, hydro, AutarkiaBlocks.HYDRO_TURBINE.get(), Direction.NORTH);
        for (int y = 3; y <= 11; y++) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                set(l, hydro.offset(0, y, 0).relative(d), Blocks.GLASS.defaultBlockState());
            }
        }
        set(l, hydro.above(12), Blocks.GLASS.defaultBlockState());
        set(l, hydro.above(11), Blocks.WATER.defaultBlockState());

        BlockPos tap = o.offset(-10, 0, 13);
        set(l, tap.below(), AutarkiaBlocks.MAGMA_VENT.get().defaultBlockState());
        TestM1.placeBigAt(l, tap, AutarkiaBlocks.MAGMA_TAP.get(), Direction.NORTH);
        water(l, tap.offset(1, 1, 0));

        BlockPos plant = o.offset(-4, 0, 14);
        for (BlockPos p : BlockPos.betweenClosed(plant.offset(-1, -1, -1), plant.offset(1, -1, 1))) {
            set(l, p, AutarkiaBlocks.FOUNDATION.get().defaultBlockState());
        }
        TestM1.placeBigAt(l, plant, AutarkiaBlocks.FROTH_FLOTATION_PLANT.get(), Direction.NORTH);
        water(l, plant.offset(-2, 0, 0));
        TestM1.insertAt(l, plant, new ItemStack(AutarkiaItems.CRUSHED_VANTITE.get(), 32));

        set(l, o.offset(2, 0, 12), AutarkiaBlocks.VANTITE_ORE.get().defaultBlockState());
        set(l, o.offset(2, 1, 12), AutarkiaBlocks.ORE_OUTCROP.get().defaultBlockState().setValue(OreOutcropBlock.ORE, OreOutcropBlock.Ore.VANTITE));
        set(l, o.offset(4, 0, 12), AutarkiaBlocks.RAW_VANTITE_BLOCK.get().defaultBlockState());
        set(l, o.offset(5, 0, 12), AutarkiaBlocks.VANTITE_BLOCK.get().defaultBlockState());
        set(l, o.offset(6, 0, 12), AutarkiaBlocks.VANTITE_STEEL_BLOCK.get().defaultBlockState());
        TestM1.placeBigAt(l, o.offset(10, 0, 12), AutarkiaBlocks.OUTPOST_CORE.get(), Direction.NORTH);
        BlockPos depot = o.offset(14, 0, 12);
        set(l, depot, AutarkiaBlocks.DEPOT.get().defaultBlockState());
        TestM1.insertAt(l, depot, new ItemStack(AutarkiaItems.RAW_VANTITE.get(), 48));

        BlockPos a = o.offset(-20, 0, 22);
        BlockPos b = o.offset(2, 0, 22);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-16, -6, 18), o.offset(-2, -1, 26))) {
            set(l, p, Blocks.AIR.defaultBlockState());
        }
        TestM1.placeBigAt(l, a, AutarkiaBlocks.CABLE_CAR_STATION.get(), Direction.EAST);
        TestM1.placeBigAt(l, b, AutarkiaBlocks.CABLE_CAR_STATION.get(), Direction.WEST);
        CableCarStationBlockEntity.Link link = CableCarStationBlockEntity.link(l, a, b, false);
        BlockPos owner = a.asLong() < b.asLong() ? a : b;
        if (link.result() == CableCarStationBlockEntity.Result.OK && l.getBlockEntity(owner) instanceof CableCarStationBlockEntity own && own.line() != null) {
            own.parts().set(0, ItemResource.of(AutarkiaItems.STEEL_PLATE.get()), 8);
            own.parts().set(1, ItemResource.of(Items.IRON_CHAIN), 4);
            own.line().setTarget(2);
        }

        BlockPos launcher = o.offset(6, 0, 32);
        BlockPos net = o.offset(21, 0, 32);
        TestM1.placeBigAt(l, launcher, AutarkiaBlocks.CRATE_LAUNCHER.get(), Direction.EAST);
        TestM1.placeBigAt(l, net, AutarkiaBlocks.CATCH_NET.get(), Direction.NORTH);
        set(l, net.below(), AutarkiaBlocks.CRATE.get().defaultBlockState());
        String refusal = l.getBlockEntity(launcher) instanceof CrateLauncherBlockEntity lb ? lb.setTarget(net, null) : "no launcher";

        DRIVERS.add(new Driver(l, plant, a, b, launcher, l.getGameTime(), l.getGameTime() + ticks));
        String state = "HV span " + span + ", cable car " + link.result() + (link.blocked() == null ? "" : " at " + link.blocked().toShortString())
            + ", launcher " + (refusal == null ? "aimed" : refusal);
        src.sendSuccess(() -> Component.literal("[showcase] M7 scene built at " + o.toShortString() + ", driven for " + ticks + " ticks; " + state), false);
        Autarkia.LOG.info("[showcase] M7 scene at {}: {}", o.toShortString(), state);
        return 1;
    }
}

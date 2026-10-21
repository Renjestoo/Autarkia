package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.BoilerBlockEntity;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.SteamTurbineBlockEntity;
import dev.autarkia.logistics.CapsuleStationBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.storage.RackBlockEntity;
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

/**
 * {@code /autarkia showcase m6}: builds the T3 scene in front of the caller and drives it: an insulated line on steel
 * pylons with a transformer to a copper pole and an inline fuse box, a steam turbine fed by two boilers, a battery bank,
 * the T3 ore machines (jaw crusher on foundation, magnetic separator, electrolyzer, fabricator), steel racks with a
 * warehouse controller and a ledger terminal, a capsule tube loop with three stations sending items, a recharge dock
 * and a domain beacon.
 */
public final class ShowcaseM6 {
    private record Driver(ServerLevel level, List<BlockPos> machines, BlockPos turbine, List<BlockPos> boilers, BlockPos sender, List<BlockPos> stations,
                          BlockPos pylon, long start, long until) {
    }

    private static final List<Driver> DRIVERS = new ArrayList<>();

    private ShowcaseM6() {
    }

    /** Register the driver tick. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> DRIVERS.removeIf(ShowcaseM6::drive));
    }

    private static boolean drive(Driver d) {
        ServerLevel l = d.level;
        long age = l.getGameTime() - d.start;
        for (BlockPos m : d.machines) {
            TestM1.pushEnergyAt(l, m, 400);
        }
        TestM1.pushEnergyAt(l, d.pylon, 300);
        if (l.getBlockEntity(d.turbine) instanceof SteamTurbineBlockEntity t) {
            t.buffer().drain(t.buffer().stored() / 2);
        }
        for (BlockPos b : d.boilers) {
            if (l.getBlockEntity(b) instanceof BoilerBlockEntity be && be.inventory().get(0).getCount() < 8) {
                be.inventory().put(0, new ItemStack(Items.COAL, 64));
            }
        }
        for (BlockPos s : d.stations) {
            if (l.getBlockEntity(s) instanceof CapsuleStationBlockEntity st) {
                st.energy().fill(800);
            }
        }
        if (age % 20 == 0 && l.getBlockEntity(d.sender) instanceof CapsuleStationBlockEntity st && st.firstInput() < 0) {
            TestM1.insertAt(l, d.sender, new ItemStack(age / 20 % 2 == 0 ? Items.IRON_INGOT : Items.COPPER_INGOT, 16));
        }
        if (age % 100 == 0) {
            for (BlockPos m : d.machines) {
                if (l.getBlockEntity(m) instanceof dev.autarkia.processing.MachineBlockEntity mb) {
                    for (int i = mb.inventory().inputs(); i < mb.inventory().size(); i++) {
                        mb.inventory().put(i, ItemStack.EMPTY);
                    }
                }
            }
            for (BlockPos s : d.stations) {
                if (l.getBlockEntity(s) instanceof CapsuleStationBlockEntity st) {
                    for (int i = CapsuleStationBlockEntity.SLOTS; i < 2 * CapsuleStationBlockEntity.SLOTS; i++) {
                        st.inventory().put(i, ItemStack.EMPTY);
                    }
                }
            }
        }
        return l.getGameTime() >= d.until;
    }

    private static void set(ServerLevel l, BlockPos p, BlockState s) {
        l.setBlock(p, s, Block.UPDATE_ALL);
    }

    private static BlockState facing(Block b, Direction d) {
        return b.defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, d);
    }

    private static void water(ServerLevel l, BlockPos p) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (l.getBlockState(p.relative(d)).isAir()) {
                set(l, p.relative(d), Blocks.STONE.defaultBlockState());
            }
        }
        set(l, p, Blocks.WATER.defaultBlockState());
    }

    /** Build the M6 scene and start its driver. */
    public static int build(CommandSourceStack src, int ticks) {
        ServerLevel l = src.getLevel();
        BlockPos o = BlockPos.containing(src.getPosition()).relative(src.getPlayer() != null ? src.getPlayer().getDirection() : Direction.SOUTH, 4);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-24, 0, -3), o.offset(24, 14, 30))) {
            set(l, p, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-24, -2, -3), o.offset(24, -1, 30))) {
            set(l, p, Blocks.STONE.defaultBlockState());
        }
        l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, AABB.encapsulatingFullBlocks(o.offset(-26, -2, -5), o.offset(26, 16, 32)),
            e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(e -> e.discard());

        BlockPos p1 = o.offset(-20, 0, 6);
        BlockPos p2 = o.offset(-4, 0, 6);
        TestM1.placeBigAt(l, p1, AutarkiaBlocks.STEEL_PYLON.get(), Direction.NORTH);
        TestM1.placeBigAt(l, p2, AutarkiaBlocks.STEEL_PYLON.get(), Direction.NORTH);
        BlockPos tr = o.offset(4, 0, 6);
        set(l, tr, AutarkiaBlocks.TRANSFORMER.get().defaultBlockState());
        BlockPos fuse = o.offset(10, 0, 6);
        set(l, fuse, facing(AutarkiaBlocks.FUSE_BOX.get(), Direction.NORTH));
        BlockPos pole = o.offset(18, 0, 6);
        TestM1.placeBigAt(l, pole, AutarkiaBlocks.WOODEN_POLE.get(), Direction.NORTH);
        set(l, pole.east(), facing(AutarkiaBlocks.BATTERY_BANK.get(), Direction.NORTH));
        set(l, pole.south(), AutarkiaBlocks.INDUSTRIAL_LAMP.get().defaultBlockState());
        Grid.addSpan(l, p1, p2, CableTier.INSULATED);
        Grid.addSpan(l, p2, tr, CableTier.INSULATED);
        Grid.addSpan(l, tr, fuse, CableTier.COPPER);
        Grid.addSpan(l, fuse, pole, CableTier.COPPER);

        BlockPos turbine = o.offset(-18, 0, 1);
        TestM1.placeBigAt(l, turbine, AutarkiaBlocks.STEAM_TURBINE.get(), Direction.NORTH);
        List<BlockPos> boilers = List.of(o.offset(-21, 0, 1), o.offset(-16, 0, 1));
        for (BlockPos b : boilers) {
            TestM1.placeBigAt(l, b, AutarkiaBlocks.BOILER.get(), Direction.NORTH);
            set(l, b.offset(1, 3, 1), AutarkiaBlocks.PRESSURE_VALVE.get().defaultBlockState());
            if (l.getBlockEntity(b) instanceof BoilerBlockEntity be) {
                be.inventory().put(0, new ItemStack(Items.COAL, 64));
                be.setFire(3);
            }
        }
        water(l, o.offset(-22, 0, 1));
        water(l, o.offset(-22, 0, 2));
        water(l, o.offset(-14, 0, 1));
        water(l, o.offset(-14, 0, 2));

        BlockPos jaw = o.offset(-12, 0, 14);
        for (BlockPos p : BlockPos.betweenClosed(jaw.offset(0, -1, 0), jaw.offset(1, -1, 1))) {
            set(l, p, AutarkiaBlocks.FOUNDATION.get().defaultBlockState());
        }
        TestM1.placeBigAt(l, jaw, AutarkiaBlocks.JAW_CRUSHER.get(), Direction.NORTH);
        TestM1.insertAt(l, jaw, new ItemStack(Items.RAW_COPPER, 64));
        BlockPos sep = o.offset(-8, 0, 14);
        BlockPos ely = o.offset(-6, 0, 14);
        set(l, sep, facing(AutarkiaBlocks.MAGNETIC_SEPARATOR.get(), Direction.NORTH));
        set(l, ely, facing(AutarkiaBlocks.ELECTROLYZER.get(), Direction.NORTH));
        TestM1.insertAt(l, sep, new ItemStack(AutarkiaItems.WASHED_COPPER.get(), 64));
        TestM1.insertAt(l, ely, new ItemStack(AutarkiaItems.PURE_COPPER.get(), 64));
        BlockPos fab = o.offset(-3, 0, 14);
        TestM1.placeBigAt(l, fab, AutarkiaBlocks.FABRICATOR.get(), Direction.NORTH);

        for (int dx = 2; dx <= 9; dx++) {
            set(l, o.offset(dx, 0, 22), Blocks.STONE_BRICKS.defaultBlockState());
            set(l, o.offset(dx, 1, 22), Blocks.STONE_BRICKS.defaultBlockState());
        }
        for (int i = 0; i < 4; i++) {
            BlockPos rp = o.offset(3 + 2 * i, 0, 21);
            set(l, rp, facing(AutarkiaBlocks.STEEL_RACK.get(), Direction.NORTH));
            if (l.getBlockEntity(rp) instanceof RackBlockEntity rack) {
                rack.insertCrate(TestM6Logistics.crate(i == 0 ? Items.IRON_INGOT : Items.COBBLESTONE, 400));
                rack.insertCrate(TestM6Logistics.crate(Items.COPPER_INGOT, 200));
            }
        }
        set(l, o.offset(5, 0, 17), AutarkiaBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState());
        set(l, o.offset(8, 0, 17), facing(AutarkiaBlocks.LEDGER_TERMINAL.get(), Direction.NORTH));

        BlockPos sender = o.offset(12, 0, 12);
        List<BlockPos> stations = List.of(sender, o.offset(20, 0, 12), o.offset(16, 0, 18));
        set(l, sender, AutarkiaBlocks.CAPSULE_STATION.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        set(l, stations.get(1), AutarkiaBlocks.CAPSULE_STATION.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
        set(l, stations.get(2), AutarkiaBlocks.CAPSULE_STATION.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        for (int dx = 13; dx <= 19; dx++) {
            tube(l, o.offset(dx, 0, 12));
        }
        for (int dz = 13; dz <= 17; dz++) {
            tube(l, o.offset(16, 0, dz));
        }
        for (int dx = 13; dx <= 19; dx++) {
            tube(l, o.offset(dx, 0, 12));
        }
        if (l.getBlockEntity(sender) instanceof CapsuleStationBlockEntity st) {
            st.button(0);
        }
        if (l.getBlockEntity(stations.get(1)) instanceof CapsuleStationBlockEntity st) {
            st.setGhost(0, new ItemStack(Items.IRON_INGOT));
        }

        set(l, o.offset(-16, 0, 22), AutarkiaBlocks.RECHARGE_DOCK.get().defaultBlockState());
        set(l, o.offset(-12, 0, 22), AutarkiaBlocks.DOMAIN_BEACON.get().defaultBlockState());

        DRIVERS.add(new Driver(l, List.of(jaw, sep, ely, fab), turbine, boilers, sender, stations, p1, l.getGameTime(), l.getGameTime() + ticks));
        src.sendSuccess(() -> Component.literal("[showcase] M6 scene built at " + o.toShortString() + ", driven for " + ticks + " ticks"), false);
        Autarkia.LOG.info("[showcase] M6 scene at {}", o.toShortString());
        return 1;
    }

    private static void tube(ServerLevel l, BlockPos p) {
        l.setBlock(p, Block.updateFromNeighbourShapes(AutarkiaBlocks.CAPSULE_TUBE.get().defaultBlockState(), l, p), Block.UPDATE_ALL);
    }
}

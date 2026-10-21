package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.PlasmaGeneratorBlockEntity;
import dev.autarkia.energy.SuperconductingCoilBlockEntity;
import dev.autarkia.logistics.MagRailStationBlockEntity;
import dev.autarkia.processing.LatticeGeodeShellBlock;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.storage.MatterBufferBlockEntity;
import dev.autarkia.storage.RackBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * {@code /autarkia showcase m8}: builds the T5 scene in front of the caller and drives it: a lattice geode cut (shells,
 * charged shells with clusters) with the lattice storage blocks, a plasma separator on foundations working raw iron, two
 * cryo nodes with coolant joined by a superconductor span between a plasma generator (ignited from the charged coil)
 * and a superconducting coil, a matter buffer with stored items and free matter, a mag rail line with two stations
 * sending sleds of cobblestone, a lattice rack with 4 crates, a nanite hive with two claim markers, a replicator foundry
 * with a Replicator Walker beside it.
 */
public final class ShowcaseM8 {
    private record Driver(ServerLevel level, BlockPos separator, BlockPos coil, BlockPos generator, BlockPos west, BlockPos east, long start, long until) {
    }

    private static final List<Driver> DRIVERS = new ArrayList<>();

    private ShowcaseM8() {
    }

    /** Register the driver tick. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> DRIVERS.removeIf(ShowcaseM8::drive));
    }

    private static boolean drive(Driver d) {
        ServerLevel l = d.level;
        long age = l.getGameTime() - d.start;
        TestM1.pushEnergyAt(l, d.separator, 16384);
        if (age % 200 == 0) {
            TestM1.insertAt(l, d.separator, new ItemStack(Items.RAW_IRON, 8));
        }
        if (l.getBlockEntity(d.coil) instanceof SuperconductingCoilBlockEntity coil) {
            coil.buffer().fill(coil.buffer().room());
        }
        if (age == 20 && l.getBlockEntity(d.generator) instanceof PlasmaGeneratorBlockEntity g) {
            g.menuButton(FakePlayerFactory.getMinecraft(l), 0);
        }
        for (BlockPos s : List.of(d.west, d.east)) {
            if (l.getBlockEntity(s) instanceof MagRailStationBlockEntity st) {
                st.energy().fill(st.energy().room());
                if (s.equals(d.west) && age % 60 == 0) {
                    for (int i = 0; i < MagRailStationBlockEntity.SLOTS; i++) {
                        st.inventory().put(i, new ItemStack(Items.COBBLESTONE, 64));
                    }
                    st.inventory().put(MagRailStationBlockEntity.SLOTS, new ItemStack(AutarkiaItems.CRATE.get(), 8));
                }
                if (s.equals(d.east) && age % 60 == 30) {
                    for (int i = MagRailStationBlockEntity.SLOTS + 1; i < st.inventory().size(); i++) {
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

    /** Build the M8 scene and start its driver. */
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

        for (int x = -21; x <= -16; x++) {
            for (int z = 2; z <= 5; z++) {
                boolean charged = (x + z) % 3 == 0;
                set(l, o.offset(x, 0, z), AutarkiaBlocks.LATTICE_GEODE_SHELL.get().defaultBlockState().setValue(LatticeGeodeShellBlock.CHARGED, charged));
                if (charged) {
                    set(l, o.offset(x, 1, z), AutarkiaBlocks.LATTICE_CLUSTER.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
                }
            }
        }
        for (int x = -21; x <= -16; x++) {
            set(l, o.offset(x, 0, 6), Blocks.CALCITE.defaultBlockState());
            set(l, o.offset(x, 0, 7), Blocks.BLACKSTONE.defaultBlockState());
        }
        set(l, o.offset(-13, 0, 3), AutarkiaBlocks.LATTICE_ALLOY_BLOCK.get().defaultBlockState());
        set(l, o.offset(-12, 0, 3), AutarkiaBlocks.LATTICE_BLOCK.get().defaultBlockState());

        BlockPos separator = o.offset(-7, 0, 3);
        for (BlockPos p : BlockPos.betweenClosed(separator.below(), separator.offset(1, -1, 1))) {
            set(l, p, AutarkiaBlocks.FOUNDATION.get().defaultBlockState());
        }
        TestM1.placeBigAt(l, separator, AutarkiaBlocks.PLASMA_SEPARATOR.get(), Direction.NORTH);
        TestM1.insertAt(l, separator, new ItemStack(Items.RAW_IRON, 16));

        BlockPos nodeA = o.offset(-14, 0, 13);
        BlockPos nodeB = o.offset(4, 0, 13);
        TestM1.placeBigAt(l, nodeA, AutarkiaBlocks.CRYO_NODE.get(), Direction.NORTH);
        TestM1.placeBigAt(l, nodeB, AutarkiaBlocks.CRYO_NODE.get(), Direction.NORTH);
        TestM1.insertAt(l, nodeA, new ItemStack(AutarkiaItems.COOLANT_CELL.get(), 4));
        TestM1.insertAt(l, nodeB, new ItemStack(AutarkiaItems.COOLANT_CELL.get(), 4));
        Grid.SpanResult span = Grid.addSpan(l, nodeA, nodeB, CableTier.SUPERCONDUCTOR);
        BlockPos generator = o.offset(-17, 0, 13);
        TestM1.placeBigAt(l, generator, AutarkiaBlocks.PLASMA_GENERATOR.get(), Direction.NORTH);
        TestM1.insertAt(l, generator, new ItemStack(AutarkiaItems.LATTICE_FUEL_CELL.get(), 8));
        BlockPos coil = o.offset(5, 0, 13);
        TestM1.placeBigAt(l, coil, AutarkiaBlocks.SUPERCONDUCTING_COIL.get(), Direction.NORTH);
        TestM1.insertAt(l, coil, new ItemStack(AutarkiaItems.COOLANT_CELL.get(), 4));

        BlockPos buffer = o.offset(10, 0, 3);
        TestM1.placeBigAt(l, buffer, AutarkiaBlocks.MATTER_BUFFER.get(), Direction.NORTH);
        if (l.getBlockEntity(buffer) instanceof MatterBufferBlockEntity mb) {
            mb.setAutoDissolve(false);
            mb.insertItems(new ItemStack(Items.RAW_IRON, 64), false);
            mb.insertItems(new ItemStack(AutarkiaItems.LATTICE_SHARD.get(), 16), false);
            mb.insertItems(new ItemStack(Items.DIAMOND, 8), false);
            mb.addFree(65536);
        }

        BlockPos west = o.offset(-20, 0, 24);
        BlockPos east = o.offset(20, 0, 24);
        set(l, west, AutarkiaBlocks.MAG_RAIL_STATION.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.WEST));
        set(l, east, AutarkiaBlocks.MAG_RAIL_STATION.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.EAST));
        for (int x = -19; x <= 19; x++) {
            set(l, o.offset(x, 0, 24), AutarkiaBlocks.MAG_RAIL.get().defaultBlockState().setValue(BlockStateProperties.RAIL_SHAPE, RailShape.EAST_WEST));
        }
        if (l.getBlockEntity(west) instanceof MagRailStationBlockEntity ws && l.getBlockEntity(east) instanceof MagRailStationBlockEntity es) {
            ws.setSettings("West", "East", FakePlayerFactory.getMinecraft(l));
            es.setSettings("East", "West", FakePlayerFactory.getMinecraft(l));
        }

        BlockPos rack = o.offset(16, 0, 12);
        set(l, rack, AutarkiaBlocks.LATTICE_RACK.get().defaultBlockState());
        if (l.getBlockEntity(rack) instanceof RackBlockEntity rb) {
            for (int i = 0; i < 4; i++) {
                List<ItemStack> stacks = new ArrayList<>();
                for (int s = 0; s < 27; s++) {
                    stacks.add(new ItemStack(i % 2 == 0 ? Items.IRON_BLOCK : AutarkiaItems.LATTICE_SHARD.get(), 64));
                }
                ItemStack crate = new ItemStack(AutarkiaItems.CRATE.get());
                crate.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
                rb.setCrate(i, crate);
            }
        }

        BlockPos hive = o.offset(-8, 0, 30);
        TestM1.placeBigAt(l, hive, AutarkiaBlocks.NANITE_HIVE.get(), Direction.NORTH);
        set(l, o.offset(-14, 0, 33), AutarkiaBlocks.CLAIM_MARKER.get().defaultBlockState());
        set(l, o.offset(-3, 0, 35), AutarkiaBlocks.CLAIM_MARKER.get().defaultBlockState());

        BlockPos foundry = o.offset(8, 0, 31);
        TestM1.placeBigAt(l, foundry, AutarkiaBlocks.REPLICATOR_FOUNDRY.get(), Direction.NORTH);
        var walker = AutarkiaEntities.REPLICATOR_WALKER.get().create(l, EntitySpawnReason.COMMAND);
        if (walker != null) {
            walker.snapTo(o.getX() + 14.5, o.getY(), o.getZ() + 28.5, 200, 0);
            l.addFreshEntity(walker);
        }

        DRIVERS.add(new Driver(l, separator, coil, generator, west, east, l.getGameTime(), l.getGameTime() + ticks));
        String state = "SC span " + span;
        src.sendSuccess(() -> Component.literal("[showcase] M8 scene built at " + o.toShortString() + ", driven for " + ticks + " ticks; " + state), false);
        Autarkia.LOG.info("[showcase] M8 scene at {}: {}", o.toShortString(), state);
        return 1;
    }
}

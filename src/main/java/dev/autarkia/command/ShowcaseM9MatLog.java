package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.logistics.MatterConduitBlockEntity;
import dev.autarkia.processing.MatterCompilerBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.storage.CrateBlockEntity;
import dev.autarkia.storage.DomainArchiveBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * The materials and logistics piece of {@code /autarkia showcase m9} (20 x 20 at the origin): a matter compiler on 4
 * foundations compiling raw iron into iron ingots, a SEND and a RECEIVE matter conduit on channel 3 moving cobblestone
 * into a crate, and a domain archive beside a depot. A driver keeps the FE buffers full, feeds the compiler and the
 * sender and empties the crate for {@value #TICKS} ticks. The conduits and the archive work only when the origin lies in
 * a colony domain (the root builds the scene in the T6 domain).
 */
public final class ShowcaseM9MatLog {
    private static final int TICKS = 3600;

    private record Driver(ServerLevel level, BlockPos compiler, BlockPos send, BlockPos recv, BlockPos crate, BlockPos archive, long start, long until) {
    }

    private static final List<Driver> DRIVERS = new ArrayList<>();

    private ShowcaseM9MatLog() {
    }

    /** Register the driver tick (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> DRIVERS.removeIf(ShowcaseM9MatLog::drive));
    }

    private static boolean drive(Driver d) {
        ServerLevel l = d.level;
        long age = l.getGameTime() - d.start;
        if (l.getBlockEntity(d.compiler) instanceof MatterCompilerBlockEntity c) {
            c.energy().fill(c.energy().room());
            if (age % 100 == 0) {
                TestM1.insertAt(l, d.compiler.above(), new ItemStack(Items.RAW_IRON, 4));
                for (int i = 1; i <= 4; i++) {
                    c.inventory().put(i, ItemStack.EMPTY);
                }
            }
            if (age == 2) {
                c.setTarget(Autarkia.id("compiling/iron_ingot"));
            }
        }
        if (l.getBlockEntity(d.send) instanceof MatterConduitBlockEntity s) {
            s.energy().fill(s.energy().room());
            if (age == 2) {
                s.setMode(MatterConduitBlockEntity.Mode.SEND);
                s.setChannel(3);
            }
            if (age % 20 == 0) {
                s.buffer().set(0, ItemResource.of(Items.COBBLESTONE), 32);
            }
        }
        if (age == 2 && l.getBlockEntity(d.recv) instanceof MatterConduitBlockEntity v) {
            v.setMode(MatterConduitBlockEntity.Mode.RECEIVE);
            v.setChannel(3);
        }
        if (age % 200 == 100 && l.getBlockEntity(d.crate) instanceof CrateBlockEntity cr) {
            for (int i = 0; i < CrateBlockEntity.SLOTS; i++) {
                cr.items().set(i, ItemResource.EMPTY, 0);
            }
        }
        if (l.getBlockEntity(d.archive) instanceof DomainArchiveBlockEntity a) {
            a.energy().fill(a.energy().room());
        }
        return l.getGameTime() >= d.until;
    }

    private static void set(ServerLevel l, BlockPos p, BlockState s) {
        l.setBlock(p, s, Block.UPDATE_ALL);
    }

    /** Place the compiler, the two conduits with their crate, the archive and a stocked depot at origin and start the driver. */
    public static void buildMatLog(ServerLevel level, BlockPos origin) {
        for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(19, 6, 19))) {
            set(level, p, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos p : BlockPos.betweenClosed(origin.below(), origin.offset(19, -1, 19))) {
            set(level, p, Blocks.STONE.defaultBlockState());
        }
        BlockPos compiler = origin.offset(3, 0, 3);
        for (BlockPos p : BlockPos.betweenClosed(compiler.below(), compiler.offset(1, -1, 1))) {
            set(level, p, AutarkiaBlocks.FOUNDATION.get().defaultBlockState());
        }
        TestM1.placeBigAt(level, compiler, AutarkiaBlocks.MATTER_COMPILER.get(), Direction.NORTH);
        BlockPos send = origin.offset(3, 0, 12);
        BlockPos recv = origin.offset(12, 0, 12);
        BlockPos crate = recv.east();
        set(level, send, AutarkiaBlocks.MATTER_CONDUIT.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.DOWN));
        set(level, recv, AutarkiaBlocks.MATTER_CONDUIT.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
        set(level, crate, AutarkiaBlocks.CRATE.get().defaultBlockState());
        BlockPos archive = origin.offset(12, 0, 4);
        set(level, archive, AutarkiaBlocks.DOMAIN_ARCHIVE.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.SOUTH));
        BlockPos depot = archive.east(2);
        set(level, depot, AutarkiaBlocks.DEPOT.get().defaultBlockState());
        TestM1.insertAt(level, depot, new ItemStack(Items.IRON_INGOT, 64));
        TestM1.insertAt(level, depot, new ItemStack(Items.REDSTONE, 64));
        DRIVERS.add(new Driver(level, compiler, send, recv, crate, archive, level.getGameTime(), level.getGameTime() + TICKS));
        Autarkia.LOG.info("[showcase] M9 materials and logistics piece at {}", origin.toShortString());
    }
}

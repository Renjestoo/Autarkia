package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.BoilerBlockEntity;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.CapacitorBankBlockEntity;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.Span;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.MachineBlock;
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
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * {@code /autarkia showcase m4}: builds the T2 display scene in front of the caller and drives it for the given ticks:
 * two overloaded copper lines held at their equilibrium heat (red at 335 FE/t, orange at 300 FE/t, fed into the first
 * pole as a foreign generator would), a burning boiler with two steam engines charging capacitor banks under a catwalk,
 * a waterwheel on a flowing channel, a lightning mast on a pole with capacitor banks, and 60 blocks to the side (out of
 * the mast radius) a pole line with a stamp mill for a lightning pulse.
 */
public final class ShowcaseM4 {
    private record Driver(ServerLevel level, BlockPos red, BlockPos orange, List<BlockPos> sinks, long until) {
    }

    private static final List<Driver> DRIVERS = new ArrayList<>();

    private ShowcaseM4() {
    }

    /** Register the driver tick. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> DRIVERS.removeIf(ShowcaseM4::drive));
    }

    private static void push(ServerLevel l, BlockPos p, int amount) {
        EnergyHandler h = l.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, p, Direction.UP);
        if (h != null) {
            try (Transaction tx = Transaction.openRoot()) {
                h.insert(amount, tx);
                tx.commit();
            }
        }
    }

    private static boolean drive(Driver d) {
        push(d.level, d.red, 335);
        push(d.level, d.orange, 300);
        if (d.level.getGameTime() % 20 == 0) {
            for (BlockPos p : d.sinks) {
                if (d.level.getBlockEntity(p) instanceof CapacitorBankBlockEntity b) {
                    b.buffer().drain(b.buffer().stored());
                }
            }
        }
        return d.level.getGameTime() >= d.until;
    }

    private static void set(ServerLevel l, BlockPos p, BlockState s) {
        l.setBlock(p, s, Block.UPDATE_ALL);
    }

    private static void pole(ServerLevel l, BlockPos p) {
        TestM1.placeBigAt(l, p, AutarkiaBlocks.WOODEN_POLE.get(), Direction.NORTH);
    }

    private static BlockState bank() {
        return AutarkiaBlocks.CAPACITOR_BANK.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH);
    }

    private static void line(ServerLevel l, BlockPos a, BlockPos b, float heat, List<BlockPos> sinks) {
        pole(l, a);
        pole(l, b);
        for (BlockPos s : List.of(b.east(), b.north())) {
            set(l, s, bank());
            sinks.add(s);
        }
        set(l, b.south(), AutarkiaBlocks.INDUSTRIAL_LAMP.get().defaultBlockState());
        Grid.addSpan(l, a, b, CableTier.COPPER);
        for (Span s : Grid.of(l).data().spans.values()) {
            if (s.joins(a.asLong(), b.asLong())) {
                s.heat = heat;
            }
        }
    }

    /** Build the M4 scene and start its driver. */
    public static int build(CommandSourceStack src, int ticks) {
        ServerLevel l = src.getLevel();
        BlockPos o = BlockPos.containing(src.getPosition()).relative(src.getPlayer() != null ? src.getPlayer().getDirection() : Direction.SOUTH, 4);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-20, 0, -3), o.offset(20, 12, 24))) {
            set(l, p, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-20, -2, -3), o.offset(20, -1, 24))) {
            set(l, p, Blocks.STONE.defaultBlockState());
        }
        l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, AABB.encapsulatingFullBlocks(o.offset(-76, -2, -5), o.offset(22, 14, 30)),
            e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(e -> e.discard());
        List<BlockPos> sinks = new ArrayList<>();
        line(l, o.offset(-18, 0, 2), o.offset(-2, 0, 2), 0.86f, sinks);
        line(l, o.offset(-18, 0, 8), o.offset(-2, 0, 8), 0.46f, sinks);

        BlockPos boiler = o.offset(4, 0, 4);
        TestM1.placeBigAt(l, boiler, AutarkiaBlocks.BOILER.get(), Direction.NORTH);
        for (int i = 0; i < 2; i++) {
            BlockPos engine = boiler.offset(i, 0, -2);
            TestM1.placeBigAt(l, engine, AutarkiaBlocks.STEAM_ENGINE.get(), Direction.NORTH);
            BlockPos k = boiler.offset(i, 0, -5);
            set(l, k, bank());
            Grid.addSpan(l, engine.above(), Direction.UP, k, Direction.UP, CableTier.COPPER, false);
        }
        set(l, boiler.offset(1, 3, 1), AutarkiaBlocks.PRESSURE_VALVE.get().defaultBlockState());
        for (int dz = -1; dz <= 2; dz++) {
            set(l, boiler.offset(-2, 0, dz), Blocks.STONE.defaultBlockState());
        }
        set(l, boiler.offset(-1, 0, -1), Blocks.STONE.defaultBlockState());
        set(l, boiler.offset(-1, 0, 2), Blocks.STONE.defaultBlockState());
        set(l, boiler.offset(-1, 0, 0), Blocks.WATER.defaultBlockState());
        set(l, boiler.offset(-1, 0, 1), Blocks.WATER.defaultBlockState());
        if (l.getBlockEntity(boiler) instanceof BoilerBlockEntity b) {
            b.inventory().put(0, new ItemStack(Items.COAL, 64));
            b.setFire(2);
        }
        for (int dz = -2; dz <= 2; dz++) {
            set(l, boiler.offset(2, 2, dz + 1), AutarkiaBlocks.CATWALK.get().defaultBlockState());
        }
        BlockState stairs = AutarkiaBlocks.CATWALK_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        set(l, boiler.offset(2, 1, 4), stairs);
        set(l, boiler.offset(2, 0, 5), stairs);
        for (int dx = -2; dx <= 3; dx++) {
            set(l, boiler.offset(dx, -1, 6), AutarkiaBlocks.HAZARD_BLOCK.get().defaultBlockState());
        }

        BlockPos source = o.offset(2, -1, 15);
        for (int dx = 0; dx <= 9; dx++) {
            set(l, source.offset(dx, 0, 0), Blocks.AIR.defaultBlockState());
        }
        set(l, source, Blocks.WATER.defaultBlockState());
        TestM1.placeBigAt(l, o.offset(7, 0, 15), AutarkiaBlocks.WATERWHEEL.get(), Direction.EAST);

        BlockPos mast = o.offset(14, 0, 4);
        BlockPos node = o.offset(14, 0, 10);
        TestM1.placeBigAt(l, mast, AutarkiaBlocks.LIGHTNING_MAST.get(), Direction.NORTH);
        pole(l, node);
        for (BlockPos s : List.of(node.east(), node.west(), node.south())) {
            set(l, s, bank());
        }
        Grid.addSpan(l, mast.above(3), Direction.UP, node, Direction.UP, CableTier.COPPER, false);
        BlockPos guard = o.offset(18, 0, 10);
        pole(l, guard);
        set(l, guard.above(3), AutarkiaBlocks.LIGHTNING_ARRESTER.get().defaultBlockState());
        Grid.addSpan(l, node, guard, CableTier.COPPER);

        BlockPos far = o.offset(-60, 0, 20);
        for (BlockPos p : BlockPos.betweenClosed(far.offset(-14, 0, -16), far.offset(14, 14, 8))) {
            set(l, p, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos p : BlockPos.betweenClosed(far.offset(-14, -1, -16), far.offset(14, -1, 8))) {
            set(l, p, Blocks.STONE.defaultBlockState());
        }
        BlockPos r1 = far.offset(-10, 2, 0);
        set(l, r1.below(), Blocks.STONE.defaultBlockState());
        set(l, r1.below(2), Blocks.STONE.defaultBlockState());
        pole(l, r1);
        BlockPos prev = r1;
        for (BlockPos r : List.of(far, far.offset(10, 0, 0))) {
            pole(l, r);
            Grid.addSpan(l, prev, r, CableTier.COPPER);
            prev = r;
        }
        TestM1.placeBigAt(l, far.offset(10, 0, 1), AutarkiaBlocks.STAMP_MILL.get(), Direction.NORTH);

        DRIVERS.add(new Driver(l, o.offset(-18, 0, 2), o.offset(-18, 0, 8), sinks, l.getGameTime() + ticks));
        src.sendSuccess(() -> Component.literal("[showcase] M4 scene built at " + o.toShortString() + ", driven for " + ticks + " ticks"), false);
        Autarkia.LOG.info("[showcase] M4 scene at {}", o.toShortString());
        return 1;
    }
}

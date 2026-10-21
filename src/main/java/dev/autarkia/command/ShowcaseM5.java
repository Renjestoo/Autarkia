package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.logistics.FilterBlockEntity;
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
 * {@code /autarkia showcase m5}: builds the T2 processing and logistics scene in front of the caller and drives it: a
 * sluice on a flowing channel, a hot blast smelter on foundation, a plate press and a wire drawer, a belt line with a
 * ramp up and down that feeds a filtering splitter and a sorting chute, a row of wood racks at 85%, 115% and 127% (the
 * last collapses after its 1,200-tick countdown), a pattern loom with a blank card, and a snapped copper end.
 */
public final class ShowcaseM5 {
    private record Driver(ServerLevel level, BlockPos head, List<BlockPos> machines, BlockPos sluice, long start, long until) {
    }

    private static final List<Driver> DRIVERS = new ArrayList<>();

    private ShowcaseM5() {
    }

    /** Register the driver tick. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> DRIVERS.removeIf(ShowcaseM5::drive));
    }

    private static boolean drive(Driver d) {
        ServerLevel l = d.level;
        long age = l.getGameTime() - d.start;
        for (BlockPos m : d.machines) {
            push(l, m, 256);
        }
        push(l, d.head, 60);
        if (age % 10 == 0) {
            ItemStack s = switch ((int) (age / 10 % 3)) {
                case 0 -> new ItemStack(Items.IRON_INGOT, 4);
                case 1 -> new ItemStack(Items.COPPER_INGOT, 4);
                default -> new ItemStack(AutarkiaItems.STEEL_PLATE.get(), 2);
            };
            TestM1.insertAt(l, d.head, s);
        }
        if (age % 100 == 0) {
            TestM1.insertAt(l, d.sluice, new ItemStack(AutarkiaItems.CRUSHED_IRON.get(), 4));
            for (BlockPos m : d.machines) {
                if (l.getBlockEntity(m) instanceof dev.autarkia.processing.MachineBlockEntity mb) {
                    for (int i = mb.inventory().inputs(); i < mb.inventory().size(); i++) {
                        mb.inventory().put(i, ItemStack.EMPTY);
                    }
                }
            }
        }
        return l.getGameTime() >= d.until;
    }

    private static void push(ServerLevel l, BlockPos p, int amount) {
        var h = l.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK, p, Direction.UP);
        if (h != null) {
            try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                h.insert(amount, tx);
                tx.commit();
            }
        }
    }

    private static void set(ServerLevel l, BlockPos p, BlockState s) {
        l.setBlock(p, s, Block.UPDATE_ALL);
    }

    private static BlockState facing(Block b, Direction d) {
        return b.defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, d);
    }

    /** Build the M5 scene and start its driver. */
    public static int build(CommandSourceStack src, int ticks) {
        ServerLevel l = src.getLevel();
        BlockPos o = BlockPos.containing(src.getPosition()).relative(src.getPlayer() != null ? src.getPlayer().getDirection() : Direction.SOUTH, 4);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-20, 0, -3), o.offset(20, 12, 24))) {
            set(l, p, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-20, -2, -3), o.offset(20, -1, 24))) {
            set(l, p, Blocks.STONE.defaultBlockState());
        }
        l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, AABB.encapsulatingFullBlocks(o.offset(-22, -2, -5), o.offset(22, 14, 30)),
            e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(e -> e.discard());

        BlockPos sluice = o.offset(-16, 0, 4);
        TestM1.placeBigAt(l, sluice, AutarkiaBlocks.SLUICE.get(), Direction.NORTH);
        for (int dz = 6; dz <= 7; dz++) {
            set(l, o.offset(-17, 0, dz), Blocks.STONE.defaultBlockState());
            set(l, o.offset(-15, 0, dz), Blocks.STONE.defaultBlockState());
        }
        set(l, o.offset(-16, 0, 8), Blocks.STONE.defaultBlockState());
        set(l, o.offset(-16, 0, 7), Blocks.WATER.defaultBlockState());
        set(l, o.offset(-16, 0, 2), AutarkiaBlocks.CRATE.get().defaultBlockState());

        BlockPos smelter = o.offset(-11, 0, 4);
        for (BlockPos p : BlockPos.betweenClosed(smelter.offset(0, -1, 0), smelter.offset(1, -1, 1))) {
            set(l, p, AutarkiaBlocks.FOUNDATION.get().defaultBlockState());
        }
        TestM1.placeBigAt(l, smelter, AutarkiaBlocks.BLAST_SMELTER.get(), Direction.NORTH);
        TestM1.insertAt(l, smelter, new ItemStack(AutarkiaItems.WASHED_IRON.get(), 48));
        BlockPos press = o.offset(-7, 0, 4);
        BlockPos drawer = o.offset(-6, 0, 4);
        set(l, press, facing(AutarkiaBlocks.PLATE_PRESS.get(), Direction.NORTH));
        set(l, drawer, facing(AutarkiaBlocks.WIRE_DRAWER.get(), Direction.NORTH));
        TestM1.insertAt(l, press, new ItemStack(AutarkiaItems.STEEL_INGOT.get(), 64));
        TestM1.insertAt(l, drawer, new ItemStack(Items.COPPER_INGOT, 64));
        TestM1.insertAt(l, drawer, new ItemStack(Items.IRON_INGOT, 16));

        BlockPos head = o.offset(-3, 0, 10);
        int[] heights = {0, 0, 0, 1, 1, 1, 0, 0, 0, 0};
        for (int i = 0; i < heights.length; i++) {
            set(l, o.offset(-3 + i, heights[i], 10), facing(AutarkiaBlocks.BELT.get(), Direction.EAST));
            if (heights[i] == 1) {
                set(l, o.offset(-3 + i, 0, 10), Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
        BlockPos split = o.offset(7, 0, 10);
        set(l, split, facing(AutarkiaBlocks.BELT_SPLITTER.get(), Direction.EAST));
        if (l.getBlockEntity(split) instanceof FilterBlockEntity f) {
            f.setGhost(0, new ItemStack(Items.COPPER_INGOT));
            f.button(0);
        }
        set(l, o.offset(7, 0, 9), AutarkiaBlocks.CRATE.get().defaultBlockState());
        set(l, o.offset(8, 0, 10), facing(AutarkiaBlocks.BELT.get(), Direction.EAST));
        BlockPos chute = o.offset(9, 0, 10);
        set(l, chute, facing(AutarkiaBlocks.SORTING_CHUTE.get(), Direction.EAST));
        if (l.getBlockEntity(chute) instanceof FilterBlockEntity f) {
            f.setGhost(0, new ItemStack(AutarkiaItems.STEEL_PLATE.get()));
        }
        set(l, o.offset(9, 0, 9), AutarkiaBlocks.CRATE.get().defaultBlockState());
        set(l, o.offset(10, 0, 10), AutarkiaBlocks.CRATE.get().defaultBlockState());

        for (int dx = 2; dx <= 16; dx++) {
            set(l, o.offset(dx, 0, 19), Blocks.STONE_BRICKS.defaultBlockState());
            set(l, o.offset(dx, 1, 19), Blocks.STONE_BRICKS.defaultBlockState());
        }
        int[][] loads = {{1728, 0}, {1728, 1222}, {1728, 1728}};
        for (int i = 0; i < 3; i++) {
            BlockPos rp = o.offset(4 + i * 5, 0, 18);
            set(l, rp, facing(AutarkiaBlocks.WOOD_RACK.get(), Direction.NORTH));
            if (l.getBlockEntity(rp) instanceof RackBlockEntity rack) {
                rack.insertCrate(TestM5Logistics.filledCrate(i == 0 ? Items.COBBLESTONE : Items.IRON_INGOT, 1728));
                if (loads[i][1] > 0) {
                    rack.insertCrate(TestM5Logistics.filledCrate(Items.COBBLESTONE, loads[i][1]));
                } else {
                    rack.insertCrate(TestM5Logistics.filledCrate(Items.COBBLESTONE, 1728));
                }
            }
        }

        BlockPos loom = o.offset(-12, 0, 16);
        set(l, loom, facing(AutarkiaBlocks.PATTERN_LOOM.get(), Direction.NORTH));
        if (l.getBlockEntity(loom) instanceof dev.autarkia.colony.PatternLoomBlockEntity be) {
            be.card().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(new ItemStack(AutarkiaItems.PATTERN_CARD.get())), 1);
        }
        l.addFreshEntity(dev.autarkia.energy.SnappedCableEntity.create(l, net.minecraft.world.phys.Vec3.atCenterOf(o.offset(16, 3, 4)), 0.4,
            dev.autarkia.energy.CableTier.COPPER, 16));

        DRIVERS.add(new Driver(l, head, List.of(smelter, press, drawer), sluice, l.getGameTime(), l.getGameTime() + ticks));
        src.sendSuccess(() -> Component.literal("[showcase] M5 scene built at " + o.toShortString() + ", driven for " + ticks + " ticks"), false);
        Autarkia.LOG.info("[showcase] M5 scene at {}", o.toShortString());
        return 1;
    }
}

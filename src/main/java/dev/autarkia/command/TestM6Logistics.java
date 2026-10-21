package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.ColonySensors;
import dev.autarkia.logistics.CapsuleNetwork;
import dev.autarkia.logistics.CapsuleStationBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.storage.LedgerRequest;
import dev.autarkia.storage.LedgerTerminalBlockEntity;
import dev.autarkia.storage.RackBlockEntity;
import dev.autarkia.storage.WarehouseControllerBlockEntity;
import dev.autarkia.worker.WorkerEntity;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * The logistics part of {@code /autarkia test m6}: 04 L8 and L9 (capsule tubes with a sender, a filtered and a default
 * receiver: routing, waiting, return home and CLOGGED), then in the T3 colony domain L13 (a ledger request without a
 * Loader stays QUEUED) and W14 / 06 #13 (a Loader fetches 64 iron ingots from a steel rack to the player), and 06 #14
 * (the Loader charges at its recharge dock and gets out of a roofed hole).
 */
final class TestM6Logistics {
    private static final java.util.UUID COURIER_ID = java.util.UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static @Nullable ServerPlayer courier;

    private TestM6Logistics() {
    }

    /** Return a crate item holding count items, in stacks of 64. */
    static ItemStack crate(Item item, int count) {
        NonNullList<ItemStack> list = NonNullList.withSize(RackBlockEntity.CRATE_SLOTS, ItemStack.EMPTY);
        for (int i = 0; count > 0 && i < list.size(); i++) {
            int n = Math.min(64, count);
            list.set(i, new ItemStack(item, n));
            count -= n;
        }
        ItemStack c = new ItemStack(AutarkiaItems.CRATE.get());
        c.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
        return c;
    }

    private static CapsuleStationBlockEntity station(SelfTest t, BlockPos p) {
        return (CapsuleStationBlockEntity) t.level.getBlockEntity(p);
    }

    private static void tube(SelfTest t, BlockPos p) {
        BlockState s = Block.updateFromNeighbourShapes(AutarkiaBlocks.CAPSULE_TUBE.get().defaultBlockState(), t.level, p);
        t.level.setBlock(p, s, Block.UPDATE_ALL);
    }

    private static int output(SelfTest t, BlockPos p, Item item) {
        int n = 0;
        CapsuleStationBlockEntity s = station(t, p);
        for (int i = CapsuleStationBlockEntity.SLOTS; i < 2 * CapsuleStationBlockEntity.SLOTS; i++) {
            n += s.inventory().get(i).is(item) ? s.inventory().get(i).getCount() : 0;
        }
        return n;
    }

    private static int input(SelfTest t, BlockPos p, Item item) {
        int n = 0;
        CapsuleStationBlockEntity s = station(t, p);
        for (int i = 0; i < CapsuleStationBlockEntity.SLOTS; i++) {
            n += s.inventory().get(i).is(item) ? s.inventory().get(i).getCount() : 0;
        }
        return n;
    }

    private static void power(SelfTest t, List<BlockPos> stations) {
        for (BlockPos p : stations) {
            station(t, p).energy().fill(800);
        }
    }

    static void capsules(TestM6.Run r) {
        SelfTest t = r.t();
        BlockPos send = r.at(4, 0, 62);
        BlockPos filtered = r.at(22, 0, 62);
        BlockPos fallback = r.at(12, 0, 68);
        List<BlockPos> all = List.of(send, filtered, fallback);
        boolean[] seen = new boolean[1];
        t.then(x -> {
            TestM1.place(x, send, AutarkiaBlocks.CAPSULE_STATION.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.EAST));
            TestM1.place(x, filtered, AutarkiaBlocks.CAPSULE_STATION.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.WEST));
            TestM1.place(x, fallback, AutarkiaBlocks.CAPSULE_STATION.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
            for (int dx = 5; dx <= 21; dx++) {
                tube(x, r.at(dx, 0, 62));
            }
            for (int dz = 63; dz <= 67; dz++) {
                tube(x, r.at(12, 0, dz));
            }
            for (int dx = 5; dx <= 21; dx++) {
                tube(x, r.at(dx, 0, 62));
            }
            station(x, send).button(0);
            station(x, filtered).setGhost(0, new ItemStack(Items.IRON_INGOT));
            power(x, all);
            TestM1.insertAt(x.level, send, new ItemStack(Items.IRON_INGOT, 64));
            TestM1.insertAt(x.level, send, new ItemStack(Items.COBBLESTONE, 32));
            TestM1.insertAt(x.level, send, new ItemStack(Items.GOLD_INGOT, 16));
        });
        t.waitUntil("04 L8 iron goes to the filtered receiver, the rest to the default receiver", 600, () -> output(t, filtered, Items.IRON_INGOT) == 64
            && output(t, fallback, Items.COBBLESTONE) == 32 && output(t, fallback, Items.GOLD_INGOT) == 16, x -> {
                power(x, all);
                CapsuleStationBlockEntity a = station(x, send).anchorStation();
                if (a != null && !a.network().capsules.isEmpty()) {
                    var c = a.network().capsules.get(0);
                    seen[0] |= dev.autarkia.logistics.CapsuleTubeBlock.isTube(x.level.getBlockState(BlockPos.of(c.currentBlock())));
                }
            });
        t.check("04 L8 capsules travelled inside the glass (a capsule was seen in a tube block) and nothing went astray", () -> seen[0]
            && output(t, filtered, Items.COBBLESTONE) == 0 && output(t, fallback, Items.IRON_INGOT) == 0 && input(t, send, Items.IRON_INGOT) == 0,
            () -> "seen " + seen[0]);
        java.util.function.Consumer<SelfTest> fill = x -> {
            for (BlockPos p : List.of(filtered, fallback)) {
                for (int i = CapsuleStationBlockEntity.SLOTS; i < 2 * CapsuleStationBlockEntity.SLOTS; i++) {
                    station(x, p).inventory().put(i, new ItemStack(Items.STONE, 64));
                }
            }
        };
        t.then(x -> TestM1.insertAt(x.level, send, new ItemStack(Items.IRON_INGOT, 20)));
        t.waitUntil("04 L9 the iron capsule launches", 60, () -> input(t, send, Items.IRON_INGOT) == 0, x -> power(x, all));
        t.then(fill);
        t.waitUntil("04 L9 with both receivers full it waits 30 s, then returns to the sender", 1800, () -> input(t, send, Items.IRON_INGOT) == 20,
            x -> power(x, all));
        t.then(x -> station(x, filtered).inventory().put(CapsuleStationBlockEntity.SLOTS, ItemStack.EMPTY));
        t.waitUntil("04 L9 with room again the iron launches", 60, () -> input(t, send, Items.IRON_INGOT) == 0, x -> power(x, all));
        t.then(x -> {
            fill.accept(x);
            for (int i = 0; i < CapsuleStationBlockEntity.SLOTS; i++) {
                station(x, send).inventory().put(i, new ItemStack(Items.DIRT, 64));
            }
        });
        t.waitUntil("04 L9 with the sender full too the network turns CLOGGED", 1800, () -> station(t, send).displayState() == CapsuleNetwork.State.CLOGGED,
            x -> power(x, all));
        t.check("04 L9 the clogged capsule still holds the 20 iron (nothing lost)", () -> station(t, send).anchorStation().network().capsules.stream()
            .anyMatch(c -> c.stack.is(Items.IRON_INGOT) && c.stack.getCount() == 20), () -> station(t, send).capsulesInFlight() + " capsules");
    }

    private static @Nullable WarehouseControllerBlockEntity controller(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof WarehouseControllerBlockEntity w ? w : null;
    }

    static void warehouse(TestM6.Run r) {
        SelfTest t = r.t();
        BlockPos ctrl = r.at(40, 0, 100);
        BlockPos term = r.at(40, 0, 96);
        BlockPos dock = r.at(46, 0, 98);
        List<BlockPos> racks = List.of(r.at(36, 0, 104), r.at(37, 0, 104), r.at(38, 0, 104), r.at(39, 0, 104));
        LedgerRequest[] req = new LedgerRequest[1];
        WorkerEntity[] loader = new WorkerEntity[1];
        Set<LedgerRequest.Status> statuses = EnumSet.noneOf(LedgerRequest.Status.class);
        long[] times = {-1, -1, -1};
        boolean[] away = new boolean[1];
        t.then(x -> {
            TestM6.box(r, 35, 0, 105, 40, 1, 105, Blocks.STONE.defaultBlockState());
            for (BlockPos p : racks) {
                TestM1.place(x, p, AutarkiaBlocks.STEEL_RACK.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.SOUTH));
            }
            ((RackBlockEntity) x.level.getBlockEntity(racks.get(0))).setCrate(0, crate(Items.IRON_INGOT, 200));
            for (int i = 1; i < racks.size(); i++) {
                ((RackBlockEntity) x.level.getBlockEntity(racks.get(i))).setCrate(0, crate(Items.COBBLESTONE, 128));
            }
            TestM1.place(x, ctrl, AutarkiaBlocks.WAREHOUSE_CONTROLLER.get().defaultBlockState());
            TestM1.place(x, term, AutarkiaBlocks.LEDGER_TERMINAL.get().defaultBlockState());
            TestM1.place(x, dock, AutarkiaBlocks.RECHARGE_DOCK.get().defaultBlockState());
        });
        t.waitTicks(20);
        t.check("04 2.4 the controller indexes the 4 racks: 200 iron and 384 cobblestone", () -> controller(t, ctrl).racks().size() == 4
            && controller(t, ctrl).available(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.IRON_INGOT)) == 200
            && controller(t, ctrl).available(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.COBBLESTONE)) == 384,
            () -> controller(t, ctrl).racks().size() + " racks, stock " + controller(t, ctrl).stock());
        t.then(x -> {
            ServerPlayer p = courier(x);
            p.getInventory().clearContent();
            p.snapTo(term.getX() + 0.5, term.getY(), term.getZ() - 1.5, 0, 0);
            controller(x, ctrl).gridBuffer().fill(20000);
            ((LedgerTerminalBlockEntity) x.level.getBlockEntity(term)).request(p, new ItemStack(Items.IRON_INGOT), 64);
            req[0] = controller(x, ctrl).requests(term).get(0);
        });
        t.waitTicks(40);
        t.then(x -> ColonySensors.sample(TestM6Colony.colony(), x.level));
        t.check("04 L13 without a Loader the request stays QUEUED with \"No Loader assigned\" and warehouse.no_loader = 1", () -> req[0].status
            == LedgerRequest.Status.QUEUED && req[0].reason.equals("no_loader") && ColonyRuntime.of(TestM6Colony.colony()).snapshot
            .getOrDefault("warehouse.no_loader", 0.0) == 1.0 && ((LedgerTerminalBlockEntity) t.level.getBlockEntity(term)).warning().equals("no_loader"),
            () -> req[0].status + " " + req[0].reason);
        t.then(x -> {
            WorkerEntity w = AutarkiaEntities.LOADER.get().create(x.level, EntitySpawnReason.COMMAND);
            w.snapTo(dock.getX() - 1.5, dock.getY(), dock.getZ() + 0.5, 0, 0);
            x.level.addFreshEntity(w);
            loader[0] = w;
        });
        t.waitUntil("06#13 the Loader joins the T3 colony", 100, () -> TestM6Colony.colony().workers.containsKey(loader[0].getUUID()), x -> {
            controller(x, ctrl).gridBuffer().fill(20000);
            ((dev.autarkia.worker.RechargeDockBlockEntity) x.level.getBlockEntity(dock)).buffer().fill(50000);
        });
        t.waitUntil("W14 the Loader brings the 64 iron ingots to the player", 4000, () -> req[0].status == LedgerRequest.Status.DONE, x -> {
            controller(x, ctrl).gridBuffer().fill(20000);
            ((dev.autarkia.worker.RechargeDockBlockEntity) x.level.getBlockEntity(dock)).buffer().fill(50000);
            statuses.add(req[0].status);
            if (times[0] < 0 && req[0].status == LedgerRequest.Status.ASSIGNED) {
                times[0] = x.level.getGameTime();
                times[2] = req[0].etaTicks;
            }
            if (times[1] < 0 && req[0].delivered > 0) {
                times[1] = x.level.getGameTime();
            }
            away[0] |= !((RackBlockEntity) x.level.getBlockEntity(racks.get(0))).present(0);
        });
        t.check("W14 status went ASSIGNED, FETCHING, DELIVERING, DONE; the shelf was empty while the crate was away", () -> statuses.containsAll(
            List.of(LedgerRequest.Status.ASSIGNED, LedgerRequest.Status.FETCHING, LedgerRequest.Status.DELIVERING)) && away[0], statuses::toString);
        t.check("W14 the player got 64 iron ingots and the crate is back on its shelf with 136", () -> {
            int n = 0;
            for (ItemStack s : courier(t).getInventory()) {
                n += s.is(Items.IRON_INGOT) ? s.getCount() : 0;
            }
            RackBlockEntity rack = (RackBlockEntity) t.level.getBlockEntity(racks.get(0));
            int left = 0;
            for (int s = 0; s < rack.position(0).size(); s++) {
                left += rack.position(0).getResource(s).is(Items.IRON_INGOT) ? rack.position(0).getAmountAsInt(s) : 0;
            }
            return n == 64 && rack.present(0) && left == 136;
        }, () -> "rack 0 present " + ((RackBlockEntity) t.level.getBlockEntity(racks.get(0))).present(0));
        t.check("W14 the ETA was within 20% of the real delivery time", () -> times[0] > 0 && times[1] > 0
            && Math.abs((times[1] - times[0]) - times[2]) <= 0.2 * (times[1] - times[0]), () -> "eta " + times[2] + ", real " + (times[1] - times[0]));
        t.check("W14 advancement logistics/fetch", () -> courier(t).getAdvancements().getOrStartProgress(
            t.level.getServer().getAdvancements().get(Autarkia.id("logistics/fetch"))).isDone(), () -> "fetch");
        TestM6Colony.rechargeAndStuck(r, "06#14 Loader", loader, dock, r.at(52, 0, 96));
    }

    /**
     * Return the W14 requester: an unconnected real player of the test level, because NeoForge gives fake players a
     * no-op advancement tracker and the fetch advancement must be observable.
     */
    static ServerPlayer courier(SelfTest t) {
        if (courier == null || courier.level() != t.level) {
            courier = new ServerPlayer(t.level.getServer(), t.level, new com.mojang.authlib.GameProfile(COURIER_ID, "AutarkiaCourier"),
                net.minecraft.server.level.ClientInformation.createDefault());
        }
        return courier;
    }

    static List<BlockPos> rackRow(TestM6.Run r) {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            out.add(r.at(60 + i, 0, 132));
        }
        return out;
    }

    static int countAround(SelfTest t, BlockPos center, int radius, Item item) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 3, radius))) {
            if (t.level.getBlockEntity(p) instanceof dev.autarkia.storage.CrateBlockEntity c) {
                n += TestM1.crateCount(t, p.immutable(), item);
            } else if (t.level.getBlockEntity(p) instanceof RackBlockEntity rack) {
                for (int i = 0; i < rack.positionCount(); i++) {
                    for (int s = 0; s < rack.position(i).size(); s++) {
                        n += rack.position(i).getResource(s).is(item) ? rack.position(i).getAmountAsInt(s) : 0;
                    }
                }
            }
        }
        for (var e : t.level.getEntitiesOfClass(dev.autarkia.storage.FallingCrateEntity.class, new net.minecraft.world.phys.AABB(center).inflate(radius))) {
            ItemContainerContents c = e.crate().get(DataComponents.CONTAINER);
            if (c != null) {
                n += c.nonEmptyItemCopyStream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
            }
        }
        return n;
    }

    static Colony colony() {
        return TestM6Colony.colony();
    }
}

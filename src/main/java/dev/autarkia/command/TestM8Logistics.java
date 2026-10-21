package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.logistics.MagRailBlock;
import dev.autarkia.logistics.MagRailLine;
import dev.autarkia.logistics.MagRailSled;
import dev.autarkia.logistics.MagRailStationBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.storage.CrateBlockEntity;
import dev.autarkia.storage.FallingCrateEntity;
import dev.autarkia.storage.MatterBank;
import dev.autarkia.storage.MatterBufferBlockEntity;
import dev.autarkia.storage.MatterValues;
import dev.autarkia.storage.RackBlock;
import dev.autarkia.storage.RackBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * The logistics part of {@code /autarkia test m8} (region x 140 to 240 of the platform; the mag rail line runs along z
 * from z 0 to z 272 at x 150, beyond the platform's z 60, on its own stone floor and force-loaded strip): L15 and the
 * matter values of 03 section 6 on a matter buffer at x 200, z 20; the {@code lattice_rack} rating at x 220, z 20; and a
 * mag rail line of 259 rails between station Alpha (z 4) and Bravo (z 264): top speed, acceleration and braking, 2,000 FE
 * per launch, exact delivery and {@code logistics/float_on} on the unconnected courier, 8-block spacing of two sleds, a
 * sled waiting for output space, the direction lock against a sled the other way, and a removed rail that makes a sled
 * collapse into a crate block with all items. About 1,900 ticks. A placed block entity joins its level registries in
 * {@code onLoad}, which NeoForge runs on the next block entity tick, so lookups wait one tick after a placement.
 */
final class TestM8Logistics {
    private static final int LX = 150;
    private static final int LZ0 = 4;
    private static final int LZ1 = 264;
    private static final int GAP_Z = 180;
    private static final int RAILS = LZ1 - LZ0 - 1;
    private static final List<Item> CARGO = List.of(Items.RAW_IRON, Items.COBBLESTONE, Items.OAK_PLANKS);

    private TestM8Logistics() {
    }

    /** Add the matter, lattice rack and mag rail steps to the M8 script. */
    static void run(TestM8.Run r) {
        data(r);
        matter(r);
        rack(r);
        rail(r);
    }

    private static long mu(Item item, int tier) {
        return MatterValues.of(new ItemStack(item), tier);
    }

    private static @Nullable MatterBufferBlockEntity buffer(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof MatterBufferBlockEntity b ? b : null;
    }

    private static @Nullable MagRailStationBlockEntity station(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof MagRailStationBlockEntity s ? s : null;
    }

    private static void revoke(SelfTest t, String path) {
        AdvancementHolder h = t.level.getServer().getAdvancements().get(Autarkia.id(path));
        if (h != null) {
            TestM6Logistics.courier(t).getAdvancements().revoke(h, "done");
        }
    }

    private static boolean done(SelfTest t, String path) {
        AdvancementHolder h = t.level.getServer().getAdvancements().get(Autarkia.id(path));
        return h != null && TestM6Logistics.courier(t).getAdvancements().getOrStartProgress(h).isDone();
    }

    private static void box(TestM8.Run r, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(r.at(x0, y0, z0), r.at(x1, y1, z1))) {
            r.t().level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private static void data(TestM8.Run r) {
        SelfTest t = r.t();
        t.check("data: matter_buffer, mag_rail, mag_rail_station and lattice_rack register (blocks, items, block entities, menus, recipes, "
            + "advancements float_on and matter_of_fact) and the matter tags load", () -> {
                for (String id : List.of("matter_buffer", "mag_rail", "mag_rail_station", "lattice_rack")) {
                    if (!BuiltInRegistries.BLOCK.containsKey(Autarkia.id(id)) || !BuiltInRegistries.ITEM.containsKey(Autarkia.id(id))
                        || t.level.recipeAccess().recipeMap().byKey(ResourceKey.create(Registries.RECIPE, Autarkia.id(id))) == null) {
                        Autarkia.LOG.info("[TEST m8] missing block, item or recipe {}", id);
                        return false;
                    }
                }
                for (String id : List.of("matter_buffer", "mag_rail_station", "lattice_rack")) {
                    if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(Autarkia.id(id))) {
                        return false;
                    }
                }
                return BuiltInRegistries.MENU.containsKey(Autarkia.id("matter_buffer")) && BuiltInRegistries.MENU.containsKey(Autarkia.id("mag_rail_station"))
                    && t.level.getServer().getAdvancements().get(Autarkia.id("logistics/float_on")) != null
                    && t.level.getServer().getAdvancements().get(Autarkia.id("logistics/matter_of_fact")) != null
                    && new ItemStack(Items.COBBLESTONE).is(MatterValues.DISSOLVABLE) && new ItemStack(AutarkiaItems.SLAG.get()).is(MatterValues.DISSOLVABLE)
                    && new ItemStack(AutarkiaItems.WORKER_CAPSULE.get()).is(MatterValues.BLACKLIST)
                    && Blocks.STONE.defaultBlockState().is(MatterValues.BULK);
            }, () -> "4 blocks and items, 3 block entity types, 2 menus, 4 recipes, 2 advancements, 3 tags");
    }

    private static void matter(TestM8.Run r) {
        SelfTest t = r.t();
        BlockPos buf = r.at(200, 0, 20);
        ItemResource cobble = ItemResource.of(Items.COBBLESTONE);
        int[] inserted = new int[1];
        long[] gained = new long[1];
        long[] free0 = new long[1];
        t.check("matter values: raw_iron 216 MU at T5 (x6) and 288 at T6 (x8); copper_ore 486 at T5 and 648 at T6 (3 raw copper)",
            () -> mu(Items.RAW_IRON, 5) == 216 && mu(Items.RAW_IRON, 6) == 288 && mu(Items.COPPER_ORE, 5) == 486 && mu(Items.COPPER_ORE, 6) == 648,
            () -> "raw_iron " + mu(Items.RAW_IRON, 5) + "/" + mu(Items.RAW_IRON, 6) + ", copper_ore " + mu(Items.COPPER_ORE, 5) + "/"
                + mu(Items.COPPER_ORE, 6));
        t.check("matter values: alloy sums of 03 s6 (3 x 27 + 27 = 4 x 27; 36 + 2 x 9 = 54; 270 + 54 = 324; 864 + 324 = 1,188) and 4 shards = 1 crystal",
            () -> {
                long bronze = mu(AutarkiaItems.BRONZE_INGOT.get(), 5);
                long steel = mu(AutarkiaItems.STEEL_INGOT.get(), 5);
                long vSteel = mu(AutarkiaItems.VANTITE_STEEL_INGOT.get(), 5);
                long crystal = mu(AutarkiaItems.LATTICE_CRYSTAL.get(), 5);
                long alloy = mu(AutarkiaItems.LATTICE_ALLOY_INGOT.get(), 5);
                return 3 * mu(Items.COPPER_INGOT, 5) + mu(AutarkiaItems.TIN_INGOT.get(), 5) == 4 * bronze && bronze == 27
                    && mu(Items.IRON_INGOT, 5) + 2 * mu(Items.COAL, 5) == steel && steel == 54
                    && mu(AutarkiaItems.VANTITE_INGOT.get(), 5) + steel == vSteel && vSteel == 324
                    && crystal + vSteel == alloy && alloy == 1188 && 4 * mu(AutarkiaItems.LATTICE_SHARD.get(), 5) == crystal;
            },
            () -> "bronze " + mu(AutarkiaItems.BRONZE_INGOT.get(), 5) + ", steel " + mu(AutarkiaItems.STEEL_INGOT.get(), 5) + ", vantite steel "
                + mu(AutarkiaItems.VANTITE_STEEL_INGOT.get(), 5) + ", lattice alloy " + mu(AutarkiaItems.LATTICE_ALLOY_INGOT.get(), 5));
        t.check("matter values: stone 1 (block tag), cobblestone 1, end stone 2, oak log 4, obsidian 16, coal 9, diamond 512, iron nugget 4, iron block "
            + "324, raw iron block 1,944 at T5, netherite 4,096, washed iron 36, slag 2", () -> mu(Items.STONE, 5) == 1 && mu(Items.COBBLESTONE, 5) == 1
                && mu(Items.END_STONE, 5) == 2 && mu(Items.OAK_LOG, 5) == 4 && mu(Items.OBSIDIAN, 5) == 16 && mu(Items.COAL, 5) == 9
                && mu(Items.DIAMOND, 5) == 512 && mu(Items.IRON_NUGGET, 5) == 4 && mu(Items.IRON_BLOCK, 5) == 324 && mu(Items.RAW_IRON_BLOCK, 5) == 1944
                && mu(Items.NETHERITE_INGOT, 5) == 4096 && mu(AutarkiaItems.WASHED_IRON.get(), 5) == 36 && mu(AutarkiaItems.SLAG.get(), 5) == 2,
            () -> "stone " + mu(Items.STONE, 5) + ", log " + mu(Items.OAK_LOG, 5) + ", raw iron block " + mu(Items.RAW_IRON_BLOCK, 5));
        t.then(x -> {
            boolean ok = TestM1.placeBig(x, buf, AutarkiaBlocks.MATTER_BUFFER.get(), Direction.NORTH);
            MatterBufferBlockEntity b = buffer(x, buf);
            if (b != null) {
                b.setAutoDissolve(false);
            }
            x.report("L15 rig: a 2x2x2 matter buffer stands, auto-dissolve off, capacity 1,048,576 MU", ok && b != null && b.capacity() == 1_048_576L,
                "placed " + ok);
        });
        t.waitTicks(2);
        t.check("matter rejects: a filled crate, a core seed (#autarkia:matter_blacklist) and a stick (no value); cobblestone is accepted", () -> {
            ItemStack filled = new ItemStack(AutarkiaItems.CRATE.get());
            filled.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.COBBLESTONE, 64))));
            ItemStack seed = new ItemStack(AutarkiaItems.CORE_SEED.get());
            MatterBufferBlockEntity b = buffer(t, buf);
            return b != null && !MatterValues.accepted(filled, 5) && b.insertItems(filled, true) == 0 && seed.is(MatterValues.BLACKLIST)
                && !MatterValues.accepted(seed, 5) && b.insertItems(seed, true) == 0 && !MatterValues.accepted(new ItemStack(Items.STICK), 5)
                && MatterValues.accepted(new ItemStack(Items.COBBLESTONE), 5) && b.insertItems(new ItemStack(Items.COBBLESTONE, 64), true) == 64;
        }, () -> "rejects");
        t.then(x -> {
            ResourceHandler<ItemResource> h = x.level.getCapability(Capabilities.Item.BLOCK, buf.above(), Direction.UP);
            if (h == null) {
                inserted[0] = -1;
                return;
            }
            try (Transaction tx = Transaction.openRoot()) {
                inserted[0] = h.insert(cobble, 640, tx);
                tx.commit();
            }
        });
        t.check("L15 640 cobblestone go in through the item handler of an upper position: 640 stored, used 640 MU, free 0", () -> {
            MatterBufferBlockEntity b = buffer(t, buf);
            return inserted[0] == 640 && b.count(cobble) == 640 && b.used() == 640 && b.free() == 0;
        }, () -> "inserted " + inserted[0] + ", used " + buffer(t, buf).used());
        t.then(x -> gained[0] = buffer(x, buf).dissolve(cobble));
        t.check("L15 dissolve: free MU +640, no stored cobblestone, used + free stays 640 (capacity 1,048,576)", () -> {
            MatterBufferBlockEntity b = buffer(t, buf);
            return gained[0] == 640 && b.free() == 640 && b.count(cobble) == 0 && b.used() == 0 && b.used() + b.free() == 640;
        }, () -> "gained " + gained[0] + ", free " + buffer(t, buf).free() + ", used " + buffer(t, buf).used());
        t.check("capacity: addFree takes only capacity - used - free (1,047,936 of 2,000,000), a full buffer refuses items, takeFree refuses more than free",
            () -> {
                MatterBufferBlockEntity b = buffer(t, buf);
                long acc = b.addFree(2_000_000);
                boolean full = b.free() == 1_048_576 && b.insertItems(new ItemStack(Items.COBBLESTONE), true) == 0;
                boolean take = !b.takeFree(2_000_000) && b.takeFree(1_048_576 - 640) && b.free() == 640;
                return acc == 1_048_576 - 640 && full && take;
            }, () -> "free " + buffer(t, buf).free());
        t.check("MatterBank.nearest finds the buffer from 10 blocks and none within 16 blocks of a point 40 blocks away", () ->
            MatterBank.nearest(t.level, buf.offset(10, 0, 0), 16) == buffer(t, buf) && MatterBank.nearest(t.level, buf.offset(-40, 0, 0), 16) == null,
            () -> "nearest " + MatterBank.nearest(t.level, buf.offset(10, 0, 0), 16));
        t.then(x -> {
            MatterBufferBlockEntity b = buffer(x, buf);
            b.insertItems(new ItemStack(Items.IRON_INGOT, 10), false);
            b.insertItems(new ItemStack(Items.DIRT, 64), false);
            free0[0] = b.free();
            b.setAutoDissolve(true);
        });
        t.waitUntil("auto-dissolve: below matterReserve (65,536 free MU) the buffer dissolves #autarkia:dissolvable within 20 ticks: 64 dirt "
            + "become 64 free MU, the 10 iron ingots stay", 40, () -> {
                MatterBufferBlockEntity b = buffer(t, buf);
                return b.count(ItemResource.of(Items.DIRT)) == 0 && b.free() == free0[0] + 64 && b.count(ItemResource.of(Items.IRON_INGOT)) == 10;
            }, x -> {
            }, () -> "dirt " + buffer(t, buf).count(ItemResource.of(Items.DIRT)) + ", free " + buffer(t, buf).free());
        t.check("Jade: the buffer shows matter used / capacity and free MU", () -> {
            List<Component> out = new ArrayList<>();
            buffer(t, buf).jadeLines(out);
            return out.size() == 2 && out.get(0).getContents() instanceof TranslatableContents tc && tc.getKey().equals("jade.autarkia.matter_buffer.used");
        }, () -> "lines");
    }

    private static void rack(TestM8.Run r) {
        SelfTest t = r.t();
        BlockPos p = r.at(220, 0, 20);
        t.then(x -> {
            TestM1.place(x, p, AutarkiaBlocks.LATTICE_RACK.get().defaultBlockState());
            if (x.level.getBlockEntity(p) instanceof RackBlockEntity rack) {
                for (int i = 0; i < 4; i++) {
                    List<ItemStack> blocks = new ArrayList<>();
                    for (int s = 0; s < 27; s++) {
                        blocks.add(new ItemStack(Items.IRON_BLOCK, 64));
                    }
                    ItemStack crate = new ItemStack(AutarkiaItems.CRATE.get());
                    crate.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(blocks));
                    rack.setCrate(i, crate);
                }
            }
        });
        t.check("lattice_rack: 4 crate positions, rating 65,536 units, salvage 2 lattice_shard", () -> {
            if (!(t.level.getBlockEntity(p) instanceof RackBlockEntity rack) || !(t.level.getBlockState(p).getBlock() instanceof RackBlock rb)) {
                return false;
            }
            ItemStack salvage = rb.salvage();
            return rack.positionCount() == 4 && rack.rating() == 65_536 && salvage.is(AutarkiaItems.LATTICE_SHARD.get()) && salvage.getCount() == 2;
        }, () -> "rack " + t.level.getBlockState(p));
        t.check("lattice_rack: 4 crates of iron blocks load 62,240 units = 95% (stage 0)", () -> t.level.getBlockEntity(p) instanceof RackBlockEntity rack
            && rack.load() == 62_240 && Math.round(rack.ratio() * 100) == 95 && rack.stage() == 0 && rack.crateCount() == 4,
            () -> t.level.getBlockEntity(p) instanceof RackBlockEntity rack ? rack.load() + " units, " + Math.round(rack.ratio() * 100) + "%" : "no rack");
    }

    private static void fill(SelfTest t, BlockPos p) {
        MagRailStationBlockEntity s = station(t, p);
        for (int i = 0; s != null && i < MagRailStationBlockEntity.SLOTS; i++) {
            s.inventory().put(i, new ItemStack(CARGO.get(i % 3), 64));
        }
    }

    private static int outCount(SelfTest t, BlockPos p, Item item) {
        MagRailStationBlockEntity s = station(t, p);
        int n = 0;
        for (int i = MagRailStationBlockEntity.OUT; s != null && i < MagRailStationBlockEntity.OUT + MagRailStationBlockEntity.SLOTS; i++) {
            n += s.inventory().get(i).is(item) ? s.inventory().get(i).getCount() : 0;
        }
        return n;
    }

    private static boolean delivered(SelfTest t, BlockPos p) {
        return outCount(t, p, Items.RAW_IRON) == 576 && outCount(t, p, Items.COBBLESTONE) == 576 && outCount(t, p, Items.OAK_PLANKS) == 576;
    }

    private static void clearOut(SelfTest t, BlockPos p) {
        MagRailStationBlockEntity s = station(t, p);
        for (int i = MagRailStationBlockEntity.OUT; s != null && i < MagRailStationBlockEntity.OUT + MagRailStationBlockEntity.SLOTS; i++) {
            s.inventory().put(i, ItemStack.EMPTY);
        }
    }

    private static @Nullable FallingCrateEntity sledFrom(SelfTest t, BlockPos start, @Nullable FallingCrateEntity except) {
        for (FallingCrateEntity e : t.level.getEntities(AutarkiaEntities.FALLING_CRATE.get(), e -> e.rail() != null && e.isAlive())) {
            if (e != except && e.rail().cell(0).equals(start)) {
                return e;
            }
        }
        return null;
    }

    private static String sledInfo(@Nullable FallingCrateEntity e) {
        if (e == null) {
            return "no sled";
        }
        MagRailSled.Route ro = e.rail();
        return ro == null ? "mode " + e.mode() + (e.isAlive() ? "" : " (gone)")
            : String.format(Locale.ROOT, "at %d pu of %d, speed %d, arrived %s", ro.pos(), ro.lengthPu(), ro.speed(), ro.arrived());
    }

    private static int @Nullable [] crateNear(SelfTest t, BlockPos gap) {
        for (BlockPos p : BlockPos.betweenClosed(gap.offset(-4, -1, -4), gap.offset(4, 3, 16))) {
            if (t.level.getBlockEntity(p) instanceof CrateBlockEntity) {
                BlockPos q = p.immutable();
                return new int[] {q.getX(), q.getY(), q.getZ(), TestM1.crateCount(t, q, Items.RAW_IRON), TestM1.crateCount(t, q, Items.COBBLESTONE),
                    TestM1.crateCount(t, q, Items.OAK_PLANKS)};
            }
        }
        return null;
    }

    private static void rail(TestM8.Run r) {
        SelfTest t = r.t();
        BlockPos a = r.at(LX, 0, LZ0);
        BlockPos b = r.at(LX, 0, LZ1);
        BlockPos gap = r.at(LX, 0, GAP_Z);
        String strip = (a.getX() - 4) + " " + (a.getZ() - 4) + " " + (b.getX() + 4) + " " + (b.getZ() + 8);
        t.then(x -> x.command("forceload add " + strip));
        t.waitUntil("mag rail rig: the line strip (x 146 to 154, z 0 to 272) is loaded", 2400, () -> t.level.isLoaded(b) && t.level.isLoaded(gap), x -> {
        });
        for (int z0 = 0; z0 <= 272; z0 += 40) {
            int lo = z0;
            int hi = Math.min(272, z0 + 39);
            t.then(x -> {
                box(r, LX - 4, -1, lo, LX + 4, -1, hi, Blocks.STONE.defaultBlockState());
                box(r, LX - 4, 0, lo, LX + 4, 4, hi, Blocks.AIR.defaultBlockState());
            });
        }
        t.then(x -> {
            revoke(x, "logistics/float_on");
            BlockState st = AutarkiaBlocks.MAG_RAIL_STATION.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH);
            TestM1.place(x, a, st);
            TestM1.place(x, b, st);
            for (int z = LZ0 + 1; z < LZ1; z++) {
                TestM1.place(x, r.at(LX, 0, z), AutarkiaBlocks.MAG_RAIL.get().defaultBlockState());
            }
            MagRailStationBlockEntity sa = station(x, a);
            MagRailStationBlockEntity sb = station(x, b);
            for (MagRailStationBlockEntity s : List.of(sa, sb)) {
                s.energy().fill(MagRailStationBlockEntity.CAPACITY);
                s.inventory().put(MagRailStationBlockEntity.CRATE_SLOT, new ItemStack(AutarkiaItems.CRATE.get(), 8));
            }
            sa.setSettings("Alpha", "Bravo", TestM6Logistics.courier(x));
            sb.setSettings("Bravo", "", TestM6Logistics.courier(x));
            MagRailLine line = MagRailLine.of(x.level, a);
            boolean straight = true;
            for (int z = LZ0 + 1; z < LZ1; z++) {
                straight &= x.level.getBlockState(r.at(LX, 0, z)).getValue(MagRailBlock.SHAPE) == RailShape.NORTH_SOUTH;
            }
            x.report("mag rail rig: stations Alpha (z 4) and Bravo (z 264) with 259 straight rails between form one line (261 cells, 2 stations, no cap)",
                line != null && line.rails == RAILS && line.stations().size() == 2 && line.size() == RAILS + 2 && line.indexOf(b) >= 0 && !line.loop
                    && !line.overCap && !line.partial && straight, line == null ? "no line" : line.rails + " rails, " + line.stations().size() + " stations");
        });
        trip(r, a, b);
        spacing(r, a, b);
        lock(r, a, b);
        breakRail(r, a, gap);
        t.then(x -> x.command("forceload remove " + strip));
    }

    private static void trip(TestM8.Run r, BlockPos a, BlockPos b) {
        SelfTest t = r.t();
        FallingCrateEntity[] sled = new FallingCrateEntity[1];
        int[] energy = new int[1];
        List<long[]> moves = new ArrayList<>();
        t.then(x -> fill(x, a));
        t.waitUntil("float on: Alpha sends at once with a full input and destination Bravo; the crate starts as a sled in mode RAIL", 20,
            () -> sled[0] != null, x -> {
                if (sled[0] == null) {
                    sled[0] = sledFrom(x, a, null);
                    energy[0] = station(x, a).energy().stored();
                }
            }, () -> "status " + station(t, a).status());
        t.check("float on: one launch costs exactly 2,000 FE (16,000 to 14,000), takes one empty crate (8 to 7) and empties the input", () -> {
            MagRailStationBlockEntity s = station(t, a);
            return energy[0] == MagRailStationBlockEntity.CAPACITY - MagRailStationBlockEntity.SEND_FE && sled[0].mode() == FallingCrateEntity.Mode.RAIL
                && s.inventory().get(MagRailStationBlockEntity.CRATE_SLOT).getCount() == 7 && s.inventory().get(0).isEmpty();
        }, () -> "energy " + energy[0]);
        t.check("float on: the route has 261 cells (260 blocks) over 259 rails", () -> sled[0].rail() != null && sled[0].rail().last() == RAILS + 1
            && sled[0].rail().rails() == RAILS, () -> sledInfo(sled[0]));
        t.waitUntil("float on: the sled runs the 260 blocks and unloads at Bravo", 700, () -> !sled[0].isAlive(), x -> {
            MagRailSled.Route ro = sled[0].rail();
            if (ro != null && sled[0].isAlive()) {
                moves.add(new long[] {ro.pos() - ro.moved(), ro.moved()});
            }
        }, () -> sledInfo(sled[0]));
        t.check("float on: acceleration 50 pu/tick^2 (first moves 50, 100, 150 pu)", () -> {
            List<Long> pos = moves.stream().filter(m -> m[1] > 0).map(m -> m[1]).limit(3).toList();
            return pos.equals(List.of(50L, 100L, 150L));
        }, () -> moves.stream().filter(m -> m[1] > 0).map(m -> m[1]).limit(5).toList().toString());
        t.check("float on: top speed 1,000 pu/tick (1 block per tick), never faster, held from the end of the acceleration to the last 20 blocks", () -> {
            long total = (RAILS + 1) * 1000L;
            long top = 0;
            boolean cruise = true;
            for (long[] m : moves) {
                top = Math.max(top, m[1]);
                if (m[0] >= 10_500 && total - m[0] > MagRailSled.BRAKE_PU + 1000) {
                    cruise &= m[1] == MagRailSled.MAX_SPEED;
                }
            }
            return top == MagRailSled.MAX_SPEED && cruise && moves.stream().filter(m -> m[1] == 1000).count() >= 200;
        }, () -> "moves " + moves.size());
        t.check("float on: braking in the last 20 blocks, linear to 100 pu/tick (no move below 100 before the final step)", () -> {
            long total = (RAILS + 1) * 1000L;
            List<long[]> late = moves.stream().filter(m -> total - m[0] <= MagRailSled.BRAKE_PU && m[1] > 0).toList();
            if (late.size() < 10) {
                return false;
            }
            for (int i = 0; i < late.size() - 1; i++) {
                long rem = total - late.get(i)[0];
                long limit = MagRailSled.BRAKE_MIN + (long) (MagRailSled.MAX_SPEED - MagRailSled.BRAKE_MIN) * rem / MagRailSled.BRAKE_PU;
                if (late.get(i)[1] < MagRailSled.BRAKE_MIN || late.get(i)[1] > limit) {
                    return false;
                }
            }
            return true;
        }, () -> "braking moves");
        t.check("float on: Bravo's output holds the 27 stacks with exact counts (576 raw iron, 576 cobblestone, 576 oak planks), the crate is in its crate "
            + "slot (8 to 9)", () -> delivered(t, b) && station(t, b).inventory().get(MagRailStationBlockEntity.CRATE_SLOT).getCount() == 9,
            () -> outCount(t, b, Items.RAW_IRON) + " raw iron, " + outCount(t, b, Items.COBBLESTONE) + " cobblestone, " + outCount(t, b, Items.OAK_PLANKS)
                + " planks");
        t.check("float on: advancement logistics/float_on (a sled delivered over 259 >= 256 rails) on the unconnected courier who set up Alpha",
            () -> done(t, "logistics/float_on"), () -> "float_on");
    }

    private static void spacing(TestM8.Run r, BlockPos a, BlockPos b) {
        SelfTest t = r.t();
        FallingCrateEntity[] s1 = new FallingCrateEntity[1];
        FallingCrateEntity[] s2 = new FallingCrateEntity[1];
        long[] startGap = {-1};
        double[] minGap = {Double.MAX_VALUE};
        int[] locked = new int[1];
        boolean[] waited = new boolean[1];
        t.then(x -> {
            clearOut(x, b);
            fill(x, a);
        });
        t.waitUntil("spacing: the first sled leaves Alpha; Alpha gets a second full input at once", 20, () -> s1[0] != null, x -> {
            s1[0] = sledFrom(x, a, null);
            if (s1[0] != null) {
                fill(x, a);
            }
        }, () -> "status " + station(t, a).status());
        t.waitUntil("spacing: Alpha holds the second sled (LOCKED) until the first is 8 blocks ahead, then sends it", 60, () -> s2[0] != null, x -> {
            locked[0] += station(x, a).status() == MagRailStationBlockEntity.Status.LOCKED ? 1 : 0;
            FallingCrateEntity n = sledFrom(x, a, s1[0]);
            if (n != null) {
                s2[0] = n;
                startGap[0] = s1[0].rail().pos();
            }
        }, () -> "first " + sledInfo(s1[0]));
        t.check("spacing: LOCKED for 15+ ticks, the first sled was 8.0 to 9.0 blocks ahead when the second started", () -> locked[0] >= 15
            && startGap[0] >= 8000 && startGap[0] < 9000, () -> "locked " + locked[0] + ", gap " + startGap[0] + " pu");
        t.waitUntil("spacing: both sleds run to Bravo; the second, finding the output full, waits above Bravo", 700,
            () -> !s1[0].isAlive() && s2[0].rail() != null && s2[0].rail().arrived(), x -> {
                if (s1[0].isAlive() && s1[0].rail() != null && s2[0].rail() != null) {
                    minGap[0] = Math.min(minGap[0], (s1[0].rail().pos() - s2[0].rail().pos()) / 1000.0);
                }
                waited[0] |= MagRailLine.sledsWaiting(x.level) >= 1;
            }, () -> "first " + sledInfo(s1[0]) + ", second " + sledInfo(s2[0]));
        t.check("spacing: the two sleds never came closer than 8 blocks (closest 8.0 to 9.0) and the waiting sled counts in transport.rail_sleds_waiting",
            () -> minGap[0] >= MagRailLine.SPACING - 1e-9 && minGap[0] < 9.0 && waited[0] && MagRailLine.sledsWaiting(t.level) >= 1 && delivered(t, b),
            () -> String.format(Locale.ROOT, "closest %.3f blocks, waiting %d", minGap[0], MagRailLine.sledsWaiting(t.level)));
        t.then(x -> clearOut(x, b));
        t.waitUntil("spacing: with the output emptied the waiting sled unloads its 27 stacks", 40, () -> !s2[0].isAlive() && delivered(t, b), x -> {
        }, () -> sledInfo(s2[0]));
    }

    private static void lock(TestM8.Run r, BlockPos a, BlockPos b) {
        SelfTest t = r.t();
        FallingCrateEntity[] s3 = new FallingCrateEntity[1];
        FallingCrateEntity[] s4 = new FallingCrateEntity[1];
        int[] locked = new int[1];
        boolean[] headOn = new boolean[1];
        boolean[] waiting = new boolean[1];
        t.then(x -> {
            clearOut(x, a);
            clearOut(x, b);
            fill(x, a);
        });
        t.waitUntil("direction lock: Alpha sends a sled toward Bravo", 20, () -> s3[0] != null, x -> {
            s3[0] = sledFrom(x, a, null);
            if (s3[0] != null) {
                fill(x, b);
            }
        }, () -> "status " + station(t, a).status());
        t.waitUntil("direction lock: Bravo (full input, destination Next = Alpha) waits while the Alpha sled runs the other way", 700, () -> !s3[0].isAlive(),
            x -> {
                locked[0] += station(x, b).status() == MagRailStationBlockEntity.Status.LOCKED ? 1 : 0;
                headOn[0] |= s3[0].isAlive() && sledFrom(x, b, null) != null;
                waiting[0] |= MagRailLine.sledsWaiting(x.level) >= 1;
            }, () -> sledInfo(s3[0]));
        t.check("direction lock: no sled left Bravo while the Alpha sled moved; Bravo was LOCKED 200+ ticks; transport.rail_sleds_waiting counted it",
            () -> !headOn[0] && locked[0] >= 200 && waiting[0] && delivered(t, b), () -> "locked " + locked[0] + ", head-on " + headOn[0]);
        t.waitUntil("direction lock: with the line clear Bravo sends toward Alpha", 40, () -> s4[0] != null, x -> s4[0] = sledFrom(x, b, null),
            () -> "status " + station(t, b).status());
        t.waitUntil("direction lock: the Bravo sled delivers its 27 stacks into Alpha's output", 700, () -> !s4[0].isAlive() && delivered(t, a), x -> {
        }, () -> sledInfo(s4[0]));
    }

    private static void breakRail(TestM8.Run r, BlockPos a, BlockPos gap) {
        SelfTest t = r.t();
        FallingCrateEntity[] s5 = new FallingCrateEntity[1];
        int[] breaks = new int[1];
        int[][] found = new int[1][];
        t.then(x -> {
            clearOut(x, a);
            fill(x, a);
        });
        t.waitUntil("rail break: Alpha sends a sled", 20, () -> s5[0] != null, x -> s5[0] = sledFrom(x, a, null), () -> "status " + station(t, a).status());
        t.waitUntil("rail break: the sled passes z 100 (96 blocks from Alpha)", 300, () -> s5[0].rail() != null && s5[0].rail().pos() >= 96_000, x -> {
        }, () -> sledInfo(s5[0]));
        t.then(x -> {
            breaks[0] = MagRailSled.breaks(x.level);
            x.level.setBlock(gap, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        });
        t.waitUntil("rail break: at the removed rail (z 180) the sled switches to mode COLLAPSE and falls", 200,
            () -> s5[0].mode() == FallingCrateEntity.Mode.COLLAPSE || !s5[0].isAlive(), x -> {
            }, () -> sledInfo(s5[0]));
        t.waitUntil("rail break: it lands as a crate block near the gap with all 27 stacks (576 raw iron, 576 cobblestone, 576 oak planks); "
            + "transport.rail_breaks counts it", 200, () -> {
                found[0] = crateNear(t, gap);
                return found[0] != null && found[0][3] == 576 && found[0][4] == 576 && found[0][5] == 576 && MagRailSled.breaks(t.level) == breaks[0] + 1;
            }, x -> {
            }, () -> found[0] == null ? "no crate, " + sledInfo(s5[0]) : "crate at " + found[0][0] + " " + found[0][1] + " " + found[0][2]);
        t.then(x -> {
            if (found[0] != null) {
                BlockPos c = new BlockPos(found[0][0], found[0][1], found[0][2]);
                if (x.level.getBlockEntity(c) instanceof CrateBlockEntity crate) {
                    crate.pickUp();
                }
                x.level.setBlock(c, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            TestM1.place(x, gap, AutarkiaBlocks.MAG_RAIL.get().defaultBlockState());
            MagRailLine line = MagRailLine.of(x.level, a);
            x.report("rail break: the rail is back and the line is whole again (259 rails, 2 stations)", line != null && line.rails == RAILS
                && line.stations().size() == 2 && x.level.getBlockState(gap).getValue(MagRailBlock.SHAPE) == RailShape.NORTH_SOUTH,
                line == null ? "no line" : line.rails + " rails");
        });
    }
}

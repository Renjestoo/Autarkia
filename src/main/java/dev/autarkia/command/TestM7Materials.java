package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.processing.FrothFlotationPlantBlockEntity;
import dev.autarkia.processing.MachineBlockEntity;
import dev.autarkia.processing.OreOutcropBlock;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Materials part of {@code /autarkia test m7} (03 section 8): the M7 data, worldgen 03 #2 (outcrops over their
 * deposits) and #3 (vantite depth and air exposure) in 30 chunks at a fixed far area, the T4 ore line 03 #18 (jaw
 * crusher and froth flotation plant with pumped water) and #19 (smelting the concentrate), and the M7 repeat of 07 #4
 * with vantite ore. Platform region x 0 to 40, z 0 to 40.
 */
final class TestM7Materials {
    private static final int SCAN_CX = 1500;
    private static final int SCAN_CZ = 1500;
    private static final int SCAN_W = 6;
    private static final int SCAN_D = 5;
    private static final int RAW = 0;
    private static final int BUCKET = 1;
    private static final int CRUSHED = 2;
    private static final int FED = 3;
    private static final int CONC = 4;
    private static final int SLAG = 5;
    private static final int COPPER = 6;

    private TestM7Materials() {
    }

    /** Results of the worldgen scan. */
    private static final class Scan {
        int outcrops;
        final Map<OreOutcropBlock.Ore, Integer> byOre = new EnumMap<>(OreOutcropBlock.Ore.class);
        final List<String> bad = new ArrayList<>();
        int vantite;
        int vantiteOutside;
        int vantiteFaces;
        int iron;
        int ironFaces;
    }

    /** Add the materials checks to the M7 run. */
    static void run(TestM7.Run r) {
        int[] n = new int[7];
        data(r);
        worldgen(r);
        line(r, n);
        smelt(r, n);
        pickaxes(r);
    }

    private static void data(TestM7.Run r) {
        SelfTest t = r.t();
        t.check("data: the M7 materials advancements, recipes, placed features and the outcrop_ore feature type load", () -> {
            for (String a : List.of("materials/concentrate", "materials/vantite")) {
                if (t.level.getServer().getAdvancements().get(Autarkia.id(a)) == null) {
                    Autarkia.LOG.info("[TEST m7] missing advancement {}", a);
                    return false;
                }
            }
            var map = t.level.recipeAccess().recipeMap();
            for (String id : List.of("froth_flotation_plant", "raw_vantite_block", "vantite_block", "vantite_steel_block", "vantite_ingot_from_vantite_block",
                "crushing/vantite_from_raw", "crushing/vantite_jaw", "washing/washed_vantite", "separating/pure_vantite", "flotation/gold_concentrate",
                "flotation/vantite_concentrate", "electrolyzing/gold_from_concentrate", "electrolyzing/vantite_from_pure", "electrolyzing/advanced_circuit",
                "alloying/vantite_from_washed", "alloying/vantite_ingot_from_raw", "alloying/vantite_steel_ingot", "smelting/gold_ingot_from_concentrate",
                "blasting/gold_ingot_from_concentrate", "fabricating/outpost_seed")) {
                if (map.byKey(ResourceKey.create(Registries.RECIPE, Autarkia.id(id))) == null) {
                    Autarkia.LOG.info("[TEST m7] missing recipe {}", id);
                    return false;
                }
            }
            var placed = t.level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
            for (String id : List.of("tin_deposit", "zinc_deposit", "vantite_ore", "vantite_deposit", "iron_deposit", "copper_deposit", "gold_deposit",
                "magma_vent", "magma_vent_nether")) {
                if (!placed.containsKey(Autarkia.id(id))) {
                    Autarkia.LOG.info("[TEST m7] missing placed feature {}", id);
                    return false;
                }
            }
            return BuiltInRegistries.FEATURE_TYPE.containsKey(Autarkia.id("outcrop_ore"));
        }, () -> "2 advancements, 20 recipes, 9 placed features, 1 feature type");
    }

    private static boolean loaded(SelfTest t) {
        for (int cx = SCAN_CX - 1; cx <= SCAN_CX + SCAN_W; cx++) {
            for (int cz = SCAN_CZ - 1; cz <= SCAN_CZ + SCAN_D; cz++) {
                if (t.level.getChunkSource().getChunkNow(cx, cz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void worldgen(TestM7.Run r) {
        SelfTest t = r.t();
        int bx = SCAN_CX * 16;
        int bz = SCAN_CZ * 16;
        String area = (bx - 16) + " " + (bz - 16) + " " + (bx + SCAN_W * 16 + 15) + " " + (bz + SCAN_D * 16 + 15);
        Scan s = new Scan();
        t.then(x -> x.command("forceload add " + area));
        t.waitUntil("03#2 rig: the 30 scan chunks from chunk " + SCAN_CX + " " + SCAN_CZ + " and their neighbours are generated", 6000, () -> loaded(t),
            x -> { });
        t.then(x -> scan(x, s));
        t.check("03#2 every ore_outcrop in 30 chunks has at least 12 matching ore blocks (8 for gold and vantite) below it", () -> s.outcrops > 0
            && s.bad.isEmpty(), () -> s.outcrops + " outcrops " + s.byOre + (s.bad.isEmpty() ? "" : ", short: " + s.bad));
        t.check("03#3 vantite_ore lies only from Y -64 to 0 and has fewer exposed faces to air than iron ore at the same depth", () -> s.vantite > 0
            && s.vantiteOutside == 0 && s.iron > 0 && s.vantiteFaces / (double) s.vantite < s.ironFaces / (double) s.iron,
            () -> s.vantite + " vantite (" + s.vantiteOutside + " outside), " + String.format(java.util.Locale.ROOT, "%.3f", s.vantiteFaces / (double) Math.max(1, s.vantite))
                + " air faces per block vs iron " + String.format(java.util.Locale.ROOT, "%.3f", s.ironFaces / (double) Math.max(1, s.iron)) + " (" + s.iron + ")");
        t.then(x -> x.command("forceload remove " + area));
    }

    private static void scan(SelfTest t, Scan s) {
        ServerLevel level = t.level;
        TagKey<Block> ironTag = OreOutcropBlock.Ore.IRON.oreTag();
        List<BlockPos> found = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int cx = SCAN_CX; cx < SCAN_CX + SCAN_W; cx++) {
            for (int cz = SCAN_CZ; cz < SCAN_CZ + SCAN_D; cz++) {
                LevelChunk ch = level.getChunk(cx, cz);
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        for (int y = level.getMinY(); y <= level.getMaxY(); y++) {
                            p.set(cx * 16 + lx, y, cz * 16 + lz);
                            BlockState st = ch.getBlockState(p);
                            if (st.is(AutarkiaBlocks.ORE_OUTCROP.get())) {
                                found.add(p.immutable());
                            } else if (st.is(AutarkiaBlocks.VANTITE_ORE.get())) {
                                s.vantite++;
                                s.vantiteOutside += y < -64 || y > 0 ? 1 : 0;
                                s.vantiteFaces += airFaces(level, p);
                            } else if (y >= -64 && y <= 0 && st.is(ironTag)) {
                                s.iron++;
                                s.ironFaces += airFaces(level, p);
                            }
                        }
                    }
                }
            }
        }
        for (BlockPos o : found) {
            OreOutcropBlock.Ore ore = level.getBlockState(o).getValue(OreOutcropBlock.ORE);
            int size = switch (ore) {
                case GOLD, VANTITE -> 12;
                case ZINC -> 18;
                default -> 20;
            };
            int need = size == 12 ? 8 : 12;
            int radius = Math.min(16, (size + 1) / 2);
            int count = 0;
            TagKey<Block> tag = ore.oreTag();
            for (BlockPos q : BlockPos.betweenClosed(new BlockPos(o.getX() - radius, level.getMinY(), o.getZ() - radius), o.offset(radius, -1, radius))) {
                if (level.getBlockState(q).is(tag)) {
                    count++;
                }
            }
            s.byOre.merge(ore, 1, Integer::sum);
            Autarkia.LOG.info("[TEST m7] outcrop {} at {}: {} matching ore blocks below", ore.getSerializedName(), o.toShortString(), count);
            if (count < need) {
                s.bad.add(ore.getSerializedName() + " at " + o.toShortString() + " has " + count);
            }
        }
        s.outcrops = found.size();
    }

    private static int airFaces(ServerLevel level, BlockPos p) {
        int n = 0;
        for (Direction d : Direction.values()) {
            n += level.getBlockState(p.relative(d)).isAir() ? 1 : 0;
        }
        return n;
    }

    private static FrothFlotationPlantBlockEntity plantAt(SelfTest t, BlockPos p) {
        return (FrothFlotationPlantBlockEntity) t.level.getBlockEntity(p);
    }

    private static int extract(SelfTest t, BlockPos p, Item item) {
        ResourceHandler<ItemResource> h = t.level.getCapability(Capabilities.Item.BLOCK, p, Direction.UP);
        if (h == null) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int moved = h.extract(ItemResource.of(item), 1, tx);
            tx.commit();
            return moved;
        }
    }

    private static void line(TestM7.Run r, int[] n) {
        SelfTest t = r.t();
        BlockPos jaw = r.at(4, 0, 4);
        BlockPos plant = r.at(14, 0, 6);
        BlockPos source = r.at(16, 0, 6);
        t.then(x -> {
            TestM7.box(r, 4, -1, 4, 5, -1, 5, AutarkiaBlocks.FOUNDATION.get().defaultBlockState());
            TestM7.box(r, 13, -1, 5, 15, -1, 7, AutarkiaBlocks.FOUNDATION.get().defaultBlockState());
            boolean ok = TestM1.placeBig(x, jaw, AutarkiaBlocks.JAW_CRUSHER.get(), Direction.NORTH);
            ok &= TestM1.placeBig(x, plant, AutarkiaBlocks.FROTH_FLOTATION_PLANT.get(), Direction.NORTH);
            n[RAW] = TestM1.insertAt(x.level, jaw, new ItemStack(Items.RAW_GOLD, 64));
            n[BUCKET] = TestM1.insertAt(x.level, plant, new ItemStack(Items.WATER_BUCKET));
            x.report("03#18 rig: jaw crusher on 4 foundations, froth flotation plant on 9 foundations, 64 raw gold, 1 water bucket", ok && n[RAW] == 64
                && n[BUCKET] == 1, "placed " + ok + ", raw " + n[RAW] + ", bucket " + n[BUCKET]);
        });
        t.waitTicks(2);
        t.check("flotation bucket slot: the water bucket fills 1,000 mB and the empty bucket comes out of the same slot", () -> plantAt(t, plant).waterAmount() == 1000
            && extract(t, plant, Items.BUCKET) == 1, () -> plantAt(t, plant).waterAmount() + " mB");
        t.then(x -> TestM6.water(x, source));
        t.waitUntil("03#18 the jaw crusher and the flotation plant turn 64 raw gold into 256 gold concentrate", 128 * 200 + 2400, () -> n[CONC] >= 256, x -> {
            TestM1.pushEnergy(x, jaw, 400);
            TestM1.pushEnergy(x, plant, 3072);
            if (!(x.level.getBlockEntity(jaw) instanceof MachineBlockEntity j) || !(x.level.getBlockEntity(plant) instanceof FrothFlotationPlantBlockEntity f)) {
                return;
            }
            n[CRUSHED] += TestM6.take(j, AutarkiaItems.CRUSHED_GOLD.get());
            int pool = n[CRUSHED] - n[FED];
            if (pool > 0 && f.inventory().get(0).getCount() < 16) {
                n[FED] += TestM1.insertAt(x.level, plant, new ItemStack(AutarkiaItems.CRUSHED_GOLD.get(), Math.min(16, pool)));
            }
            n[CONC] += TestM6.take(f, AutarkiaItems.GOLD_CONCENTRATE.get());
            n[SLAG] += TestM6.take(f, AutarkiaItems.SLAG.get());
            n[COPPER] += TestM6.take(f, Items.COPPER_NUGGET);
        }, () -> n[CRUSHED] + " crushed, " + n[FED] + " fed, " + n[CONC] + " concentrate, "
            + (t.level.getBlockEntity(plant) instanceof FrothFlotationPlantBlockEntity f ? f.machineStatus().state() : "no plant"));
        t.check("03#18 exactly 256 gold concentrate from 128 crushed gold, 54 to 74 slag, 34 to 56 copper nuggets", () -> n[CRUSHED] == 128 && n[CONC] == 256
            && n[SLAG] >= 54 && n[SLAG] <= 74 && n[COPPER] >= 34 && n[COPPER] <= 56 && plantAt(t, plant).inventory().queue() == 0,
            () -> n[CRUSHED] + " crushed, " + n[CONC] + " concentrate, " + n[SLAG] + " slag, " + n[COPPER] + " copper nuggets");
        t.check("03#18 the plant pumped its water from the adjacent source and used 32,000 mB", () -> plantAt(t, plant).waterUsed() == 32000,
            () -> plantAt(t, plant).waterUsed() + " mB used (1,000 mB came from the bucket), " + plantAt(t, plant).waterAmount() + " mB in the tank");
    }

    private static int ingots(SelfTest t, List<BlockPos> furnaces) {
        int sum = 0;
        for (BlockPos p : furnaces) {
            if (t.level.getBlockEntity(p) instanceof BlastFurnaceBlockEntity f && f.getItem(2).is(Items.GOLD_INGOT)) {
                sum += f.getItem(2).getCount();
            }
        }
        return sum;
    }

    private static void smelt(TestM7.Run r, int[] n) {
        SelfTest t = r.t();
        List<BlockPos> furnaces = new ArrayList<>();
        for (int k = 0; k < 8; k++) {
            furnaces.add(r.at(4 + 2 * k, 0, 20));
        }
        t.then(x -> {
            int left = n[CONC];
            int per = (n[CONC] + furnaces.size() - 1) / furnaces.size();
            for (BlockPos p : furnaces) {
                TestM1.place(x, p, Blocks.BLAST_FURNACE.defaultBlockState());
                int give = Math.min(per, left);
                left -= give;
                if (x.level.getBlockEntity(p) instanceof BlastFurnaceBlockEntity f) {
                    f.setItem(0, give > 0 ? new ItemStack(AutarkiaItems.GOLD_CONCENTRATE.get(), give) : ItemStack.EMPTY);
                    f.setItem(1, new ItemStack(Items.COAL, 8));
                }
            }
        });
        t.waitUntil("03#19 8 blast furnaces smelt the gold concentrate", 64 * 100 + 600, () -> n[CONC] > 0 && ingots(t, furnaces) >= n[CONC], x -> { },
            () -> ingots(t, furnaces) + " of " + n[CONC]);
        t.check("03#19 256 gold ingots from the 256 concentrate (4 per raw gold)", () -> ingots(t, furnaces) == 256 && n[CONC] == 256,
            () -> ingots(t, furnaces) + " gold ingots from " + n[CONC] + " concentrate");
    }

    private static int itemsNear(SelfTest t, BlockPos p) {
        int sum = 0;
        for (ItemEntity e : t.level.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(1.5))) {
            sum += e.getItem().getCount();
        }
        return sum;
    }

    private static void pickaxes(TestM7.Run r) {
        SelfTest t = r.t();
        FakePlayer fake = FakePlayerFactory.getMinecraft(t.level);
        BlockPos steel = r.at(30, 0, 30);
        BlockPos bronze = r.at(36, 0, 30);
        boolean[] broken = new boolean[2];
        t.then(x -> {
            TestM1.place(x, steel, AutarkiaBlocks.VANTITE_ORE.get().defaultBlockState());
            TestM1.place(x, bronze, AutarkiaBlocks.VANTITE_ORE.get().defaultBlockState());
            TestM1.killItems(x, steel, 3);
            TestM1.killItems(x, bronze, 3);
            fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AutarkiaItems.STEEL_PICKAXE.get()));
            broken[0] = fake.gameMode.destroyBlock(steel);
            fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AutarkiaItems.BRONZE_PICKAXE.get()));
            broken[1] = fake.gameMode.destroyBlock(bronze);
            fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        t.waitTicks(2);
        t.check("07#4 (M7 repeat) vantite ore: a steel pickaxe drops 1 raw vantite, a bronze pickaxe drops nothing", () -> broken[0] && broken[1]
            && TestM1.countItems(t, steel, 1.5, AutarkiaItems.RAW_VANTITE.get()) == 1 && itemsNear(t, bronze) == 0,
            () -> "broken " + broken[0] + "/" + broken[1] + ", steel drops " + TestM1.countItems(t, steel, 1.5, AutarkiaItems.RAW_VANTITE.get())
                + " raw vantite, bronze drops " + itemsNear(t, bronze) + " items");
        t.then(x -> {
            TestM1.killItems(x, steel, 3);
            TestM1.killItems(x, bronze, 3);
        });
    }
}

package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.equipment.PoweredItem;
import dev.autarkia.equipment.VantiteExcavatorItem;
import dev.autarkia.processing.LatticeGeodeShellBlock;
import dev.autarkia.processing.MachineRecipe;
import dev.autarkia.processing.MachineStatus;
import dev.autarkia.processing.PlasmaSeparatorBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Materials part of {@code /autarkia test m8} (03 section 8): the M8 materials data, 03 #21 (lattice alloy), #20 (64 raw
 * zinc in the plasma separator), the foundation rule, #5 (charged shell growth at random tick speed 300, shell and
 * cluster drops) and #4 (a lattice geode in far generated chunks, up to 5 batches of 14 x 14 chunks from chunk 2600,
 * 2600). Platform region x 0 to 40, z 0 to 60.
 */
final class TestM8Materials {
    private static final int SCAN_CX = 2600;
    private static final int SCAN_CZ = 2600;
    private static final int BATCH = 14;
    private static final int BATCHES = 5;
    private static final int SCAN_TOP = -8;
    private static final int RAY = 14;
    private static final int GOOD = 0;
    private static final int PARTIAL = 1;
    private static final int WRONG = 2;
    private static final List<String> METALS = List.of("iron", "copper", "gold", "tin", "zinc", "vantite");

    private TestM8Materials() {
    }

    /** State of the geode search across batches. */
    private static final class Scan {
        int batch = -1;
        boolean active;
        boolean done;
        boolean layered;
        int chunks;
        int geodes;
        int clusters;
        int loose;
        String best = "none";
    }

    /** Add the materials checks to the M8 run. */
    static void run(TestM8.Run r) {
        data(r);
        alloy(r);
        separate(r);
        foundation(r);
        growth(r);
        drops(r);
        geode(r);
    }

    private static void box(TestM8.Run r, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(r.at(x0, y0, z0), r.at(x1, y1, z1))) {
            r.t().level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private static PlasmaSeparatorBlockEntity separator(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof PlasmaSeparatorBlockEntity s ? s : null;
    }

    private static void data(TestM8.Run r) {
        SelfTest t = r.t();
        t.check("data: the M8 materials blocks, items, block entity, menu, recipe type, geode feature and placed feature register", () -> {
            for (String id : List.of("lattice_cluster", "lattice_geode_shell", "lattice_alloy_block", "lattice_block", "plasma_separator")) {
                if (!BuiltInRegistries.BLOCK.containsKey(Autarkia.id(id)) || !BuiltInRegistries.ITEM.containsKey(Autarkia.id(id))) {
                    Autarkia.LOG.info("[TEST m8] missing block or block item {}", id);
                    return false;
                }
            }
            for (String id : List.of("lattice_shard", "lattice_crystal", "lattice_alloy_ingot", "nanite_paste", "ai_core", "lattice_fuel_cell", "coolant_cell",
                "core_upgrade_t5")) {
                if (!BuiltInRegistries.ITEM.containsKey(Autarkia.id(id))) {
                    Autarkia.LOG.info("[TEST m8] missing item {}", id);
                    return false;
                }
            }
            return BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(Autarkia.id("plasma_separator")) && BuiltInRegistries.MENU.containsKey(Autarkia.id("plasma_separator"))
                && BuiltInRegistries.RECIPE_TYPE.containsKey(Autarkia.id("plasma_separating"))
                && t.level.registryAccess().lookupOrThrow(Registries.FEATURE).containsKey(Autarkia.id("lattice_geode"))
                && t.level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).containsKey(Autarkia.id("lattice_geode"));
        }, () -> "5 blocks, 13 items, 1 block entity type, 1 menu, 1 recipe type, 1 feature, 1 placed feature");
        t.check("data: the 17 M8 materials recipes and 2 advancements load; each plasma_separating recipe gives 6 ingots and a 20% secondary ingot", () -> {
            for (String a : List.of("materials/lattice", "materials/plasma")) {
                if (t.level.getServer().getAdvancements().get(Autarkia.id(a)) == null) {
                    Autarkia.LOG.info("[TEST m8] missing advancement {}", a);
                    return false;
                }
            }
            var map = t.level.recipeAccess().recipeMap();
            List<String> ids = new ArrayList<>(List.of("lattice_crystal", "lattice_alloy_block", "lattice_alloy_ingot_from_lattice_alloy_block", "lattice_block",
                "plasma_separator", "alloying/lattice_alloy_ingot", "fabricating/nanite_paste", "fabricating/ai_core", "fabricating/lattice_fuel_cell",
                "fabricating/coolant_cell", "fabricating/core_upgrade_t5"));
            METALS.forEach(m -> ids.add("plasma_separating/" + m + "_from_raw"));
            for (String id : ids) {
                RecipeHolder<?> h = map.byKey(ResourceKey.create(Registries.RECIPE, Autarkia.id(id)));
                if (h == null) {
                    Autarkia.LOG.info("[TEST m8] missing recipe {}", id);
                    return false;
                }
                if (id.startsWith("plasma_separating/") && !(h.value() instanceof MachineRecipe m && m.results().size() == 2
                    && m.results().get(0).item().create().getCount() == 6 && m.results().get(0).certain() && m.results().get(1).chance() == 0.2f)) {
                    Autarkia.LOG.info("[TEST m8] plasma recipe {} does not give 6 ingots + 20% secondary", id);
                    return false;
                }
            }
            return true;
        }, () -> "17 recipes, 2 advancements");
    }

    private static void alloy(TestM8.Run r) {
        SelfTest t = r.t();
        BlockPos sep = r.at(12, 0, 4);
        int[] out = new int[1];
        t.then(x -> {
            box(r, 12, -1, 4, 13, -1, 5, AutarkiaBlocks.FOUNDATION.get().defaultBlockState());
            boolean ok = TestM1.placeBig(x, sep, AutarkiaBlocks.PLASMA_SEPARATOR.get(), Direction.NORTH);
            int a = TestM1.insertAt(x.level, sep, new ItemStack(AutarkiaItems.LATTICE_CRYSTAL.get()));
            int b = TestM1.insertAt(x.level, sep, new ItemStack(AutarkiaItems.VANTITE_STEEL_INGOT.get()));
            PlasmaSeparatorBlockEntity p = separator(x, sep);
            boolean inB = p != null && p.inventory().get(PlasmaSeparatorBlockEntity.SLOT_B).is(AutarkiaItems.VANTITE_STEEL_INGOT.get());
            x.report("03#21 rig: plasma separator on 4 foundations, lattice crystal in slot A, vantite steel ingot in slot B", ok && a == 1 && b == 1 && inB,
                "placed " + ok + ", crystal " + a + ", steel " + b + ", steel in B " + inB);
        });
        t.waitUntil("03#21 the plasma separator alloys the lattice crystal and the vantite steel ingot", 600, () -> out[0] >= 1, x -> {
            TestM1.pushEnergy(x, sep, 16384);
            PlasmaSeparatorBlockEntity p = separator(x, sep);
            if (p != null) {
                out[0] += TestM6.take(p, AutarkiaItems.LATTICE_ALLOY_INGOT.get());
            }
        }, () -> separator(t, sep) == null ? "no separator" : separator(t, sep).machineStatus().state().toString());
        t.check("03#21 exactly 1 lattice alloy ingot, both inputs used, 819,200 FE (200 ticks x 4,096 FE/t)", () -> separator(t, sep) != null && out[0] == 1
            && separator(t, sep).inventory().queue() == 0 && separator(t, sep).energyUsed() == 819200L,
            () -> out[0] + " ingots, queue " + (separator(t, sep) == null ? -1 : separator(t, sep).inventory().queue()) + ", "
                + (separator(t, sep) == null ? -1 : separator(t, sep).energyUsed()) + " FE used");
    }

    private static void separate(TestM8.Run r) {
        SelfTest t = r.t();
        BlockPos sep = r.at(4, 0, 4);
        int[] n = new int[3];
        t.then(x -> {
            box(r, 4, -1, 4, 5, -1, 5, AutarkiaBlocks.FOUNDATION.get().defaultBlockState());
            boolean ok = TestM1.placeBig(x, sep, AutarkiaBlocks.PLASMA_SEPARATOR.get(), Direction.NORTH);
            n[0] = TestM1.insertAt(x.level, sep, new ItemStack(AutarkiaItems.RAW_ZINC.get(), 64));
            x.report("03#20 rig: plasma separator on 4 foundations with 64 raw zinc", ok && n[0] == 64, "placed " + ok + ", raw zinc " + n[0]);
        });
        t.waitUntil("03#20 the plasma separator turns 64 raw zinc into zinc ingots", 64 * 100 + 600, () -> n[1] >= 384, x -> {
            TestM1.pushEnergy(x, sep, 8192);
            PlasmaSeparatorBlockEntity p = separator(x, sep);
            if (p != null) {
                n[1] += TestM6.take(p, AutarkiaItems.ZINC_INGOT.get());
                n[2] += TestM6.take(p, Items.COPPER_INGOT);
            }
        }, () -> n[1] + " zinc, " + n[2] + " copper, " + (separator(t, sep) == null ? "no separator" : separator(t, sep).machineStatus().state()));
        t.check("03#20 exactly 384 zinc ingots, copper ingots in the 99.9% range of 64 x 0.2 (3 to 23), 26,214,400 FE used", () -> separator(t, sep) != null
            && n[1] == 384 && SelfTest.binomial(n[2], 64, 0.2) && separator(t, sep).energyUsed() == 26214400L && separator(t, sep).inventory().queue() == 0,
            () -> n[1] + " zinc ingots, " + n[2] + " copper ingots, " + (separator(t, sep) == null ? -1 : separator(t, sep).energyUsed()) + " FE used");
    }

    private static void foundation(TestM8.Run r) {
        SelfTest t = r.t();
        BlockPos sep = r.at(20, 0, 4);
        t.then(x -> {
            TestM1.placeBig(x, sep, AutarkiaBlocks.PLASMA_SEPARATOR.get(), Direction.NORTH);
            TestM1.insertAt(x.level, sep, new ItemStack(AutarkiaItems.RAW_ZINC.get()));
        });
        t.repeat(25, x -> TestM1.pushEnergy(x, sep, 8192));
        t.check("foundation rule: a plasma separator on stone stays NO_CONDITION, uses no FE and keeps its raw zinc", () -> separator(t, sep) != null
            && separator(t, sep).machineStatus().state() == MachineStatus.State.NO_CONDITION && separator(t, sep).energyUsed() == 0
            && separator(t, sep).inventory().queue() == 1, () -> separator(t, sep) == null ? "no separator"
            : separator(t, sep).machineStatus().state() + ", " + separator(t, sep).energyUsed() + " FE used, queue " + separator(t, sep).inventory().queue());
    }

    private static int grown(SelfTest t, BlockPos shell, boolean facingOnly) {
        int n = 0;
        for (Direction d : Direction.values()) {
            BlockState s = t.level.getBlockState(shell.relative(d));
            if (s.is(AutarkiaBlocks.LATTICE_CLUSTER.get()) && (!facingOnly || s.getValue(AmethystClusterBlock.FACING) == d)) {
                n++;
            }
        }
        return n;
    }

    private static void growth(TestM8.Run r) {
        SelfTest t = r.t();
        BlockPos shell = r.at(30, 3, 30);
        int[] speed = new int[1];
        t.then(x -> {
            TestM1.place(x, shell, AutarkiaBlocks.LATTICE_GEODE_SHELL.get().defaultBlockState().setValue(LatticeGeodeShellBlock.CHARGED, true));
            speed[0] = x.level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
            x.level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, 300, x.level.getServer());
        });
        t.waitUntil("03#5 a charged lattice geode shell grows lattice clusters at random tick speed 300", 4000, () -> grown(t, shell, false) >= 2, x -> { },
            () -> grown(t, shell, false) + " clusters");
        t.then(x -> x.level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, speed[0], x.level.getServer()));
        t.check("03#5 every grown cluster stands on the shell and points away from it", () -> grown(t, shell, false) >= 2
            && grown(t, shell, true) == grown(t, shell, false), () -> grown(t, shell, true) + " of " + grown(t, shell, false) + " clusters face away");
    }

    private static int itemsNear(SelfTest t, BlockPos p) {
        int sum = 0;
        for (ItemEntity e : t.level.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(1.5))) {
            sum += e.getItem().getCount();
        }
        return sum;
    }

    private static void drops(TestM8.Run r) {
        SelfTest t = r.t();
        FakePlayer fake = FakePlayerFactory.getMinecraft(t.level);
        BlockPos charged = r.at(4, 0, 20);
        BlockPos plain = r.at(8, 0, 20);
        BlockPos steel = r.at(12, 0, 20);
        BlockPos vantite = r.at(16, 0, 20);
        List<BlockPos> all = List.of(charged, plain, steel, vantite);
        boolean[] broken = new boolean[4];
        t.then(x -> {
            BlockState shell = AutarkiaBlocks.LATTICE_GEODE_SHELL.get().defaultBlockState();
            BlockState cluster = AutarkiaBlocks.LATTICE_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP);
            TestM1.place(x, charged, shell.setValue(LatticeGeodeShellBlock.CHARGED, true));
            TestM1.place(x, plain, shell);
            TestM1.place(x, steel, cluster);
            TestM1.place(x, vantite, cluster);
            all.forEach(p -> TestM1.killItems(x, p, 3));
            fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AutarkiaItems.STEEL_PICKAXE.get()));
            broken[0] = fake.gameMode.destroyBlock(charged);
            broken[1] = fake.gameMode.destroyBlock(plain);
            broken[2] = fake.gameMode.destroyBlock(steel);
            ItemStack excavator = new ItemStack(AutarkiaItems.VANTITE_EXCAVATOR.get());
            PoweredItem.setEnergy(excavator, VantiteExcavatorItem.CAPACITY);
            fake.setItemInHand(InteractionHand.MAIN_HAND, excavator);
            broken[3] = fake.gameMode.destroyBlock(vantite);
            fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        t.waitTicks(2);
        t.check("03#5 a charged lattice geode shell mined with a pickaxe drops nothing; an uncharged shell drops itself", () -> broken[0] && broken[1]
            && itemsNear(t, charged) == 0 && TestM1.countItems(t, plain, 1.5, AutarkiaItems.LATTICE_GEODE_SHELL.get()) == 1 && itemsNear(t, plain) == 1,
            () -> "broken " + broken[0] + "/" + broken[1] + ", charged drops " + itemsNear(t, charged) + " items, uncharged drops "
                + TestM1.countItems(t, plain, 1.5, AutarkiaItems.LATTICE_GEODE_SHELL.get()) + " shells");
        t.check("03#5 lattice cluster: a steel pickaxe gets no shards; a charged vantite_excavator gets 2", () -> broken[2] && broken[3]
            && itemsNear(t, steel) == 0 && TestM1.countItems(t, vantite, 1.5, AutarkiaItems.LATTICE_SHARD.get()) == 2 && itemsNear(t, vantite) == 2,
            () -> "broken " + broken[2] + "/" + broken[3] + ", steel pickaxe drops " + itemsNear(t, steel) + " items, excavator drops "
                + TestM1.countItems(t, vantite, 1.5, AutarkiaItems.LATTICE_SHARD.get()) + " shards");
        t.then(x -> all.forEach(p -> TestM1.killItems(x, p, 3)));
    }

    private static int cx0(int batch) {
        return SCAN_CX + batch * BATCH;
    }

    private static String area(int batch) {
        int x0 = cx0(batch) * 16;
        int z0 = SCAN_CZ * 16;
        return x0 + " " + z0 + " " + (x0 + BATCH * 16 - 1) + " " + (z0 + BATCH * 16 - 1);
    }

    private static boolean loaded(SelfTest t, int batch) {
        for (int cx = cx0(batch); cx < cx0(batch) + BATCH; cx++) {
            for (int cz = SCAN_CZ; cz < SCAN_CZ + BATCH; cz++) {
                if (t.level.getChunkSource().getChunkNow(cx, cz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void geode(TestM8.Run r) {
        SelfTest t = r.t();
        Scan s = new Scan();
        t.waitUntil("03#4 rig: up to " + BATCHES + " batches of " + BATCH * BATCH + " far chunks from chunk " + SCAN_CX + " " + SCAN_CZ
            + " are generated and scanned for lattice geodes", BATCHES * 12000 + 100, () -> s.done, x -> step(x, s),
            () -> "batch " + s.batch + (s.active ? " loading" : "") + ", " + s.chunks + " chunks scanned");
        t.then(x -> {
            if (s.active) {
                x.command("forceload remove " + area(s.batch));
                s.active = false;
            }
        });
        t.check("03#4 a lattice geode generates: from its centre at least 3 of 6 axis rays cross lattice_geode_shell, calcite, blackstone in this order "
            + "and none in another order", () -> s.layered, () -> s.geodes + " complete geodes in " + s.chunks + " chunks, best: " + s.best);
        t.check("03#4 every lattice_cluster of the scanned whole geodes (no ray crosses another geode) sits next to a charged shell",
            () -> s.clusters > 0 && s.loose == 0,
            () -> s.clusters + " clusters, " + s.loose + " without a charged shell beside them");
    }

    private static void step(SelfTest t, Scan s) {
        if (s.done) {
            return;
        }
        if (!s.active) {
            if ((s.layered && s.clusters > 0) || s.batch + 1 >= BATCHES) {
                s.done = true;
                return;
            }
            s.batch++;
            t.command("forceload add " + area(s.batch));
            s.active = true;
            return;
        }
        if (!loaded(t, s.batch)) {
            return;
        }
        scanBatch(t, s, s.batch);
        t.command("forceload remove " + area(s.batch));
        s.active = false;
        s.chunks += BATCH * BATCH;
    }

    private static void scanBatch(SelfTest t, Scan s, int batch) {
        ServerLevel level = t.level;
        int x0 = cx0(batch) * 16;
        int z0 = SCAN_CZ * 16;
        int x1 = x0 + BATCH * 16 - 1;
        int z1 = z0 + BATCH * 16 - 1;
        Set<BlockPos> shells = new HashSet<>();
        List<BlockPos> clusters = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int cx = cx0(batch); cx < cx0(batch) + BATCH; cx++) {
            for (int cz = SCAN_CZ; cz < SCAN_CZ + BATCH; cz++) {
                LevelChunk ch = level.getChunkSource().getChunkNow(cx, cz);
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        for (int y = level.getMinY(); y <= SCAN_TOP; y++) {
                            p.set(cx * 16 + lx, y, cz * 16 + lz);
                            BlockState st = ch.getBlockState(p);
                            if (st.is(AutarkiaBlocks.LATTICE_GEODE_SHELL.get())) {
                                shells.add(p.immutable());
                            } else if (st.is(AutarkiaBlocks.LATTICE_CLUSTER.get())) {
                                clusters.add(p.immutable());
                            }
                        }
                    }
                }
            }
        }
        for (List<BlockPos> g : groups(shells)) {
            analyse(level, s, g, clusters, x0, z0, x1, z1);
        }
        Autarkia.LOG.info("[TEST m8] geode batch {}: {} shell blocks, {} clusters; totals {} geodes, {} clusters, {} loose", batch, shells.size(),
            clusters.size(), s.geodes, s.clusters, s.loose);
    }

    private static List<List<BlockPos>> groups(Set<BlockPos> shells) {
        List<List<BlockPos>> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (BlockPos start : shells) {
            if (!seen.add(start)) {
                continue;
            }
            List<BlockPos> g = new ArrayList<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(start));
            while (!queue.isEmpty()) {
                BlockPos q = queue.poll();
                g.add(q);
                for (BlockPos n : BlockPos.betweenClosed(q.offset(-1, -1, -1), q.offset(1, 1, 1))) {
                    if (shells.contains(n) && seen.add(n.immutable())) {
                        queue.add(n.immutable());
                    }
                }
            }
            out.add(g);
        }
        return out;
    }

    private static void analyse(ServerLevel level, Scan s, List<BlockPos> g, List<BlockPos> clusters, int x0, int z0, int x1, int z1) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        double sx = 0;
        double sy = 0;
        double sz = 0;
        int charged = 0;
        for (BlockPos q : g) {
            minX = Math.min(minX, q.getX());
            minY = Math.min(minY, q.getY());
            minZ = Math.min(minZ, q.getZ());
            maxX = Math.max(maxX, q.getX());
            maxY = Math.max(maxY, q.getY());
            maxZ = Math.max(maxZ, q.getZ());
            sx += q.getX();
            sy += q.getY();
            sz += q.getZ();
            charged += level.getBlockState(q).getValue(LatticeGeodeShellBlock.CHARGED) ? 1 : 0;
        }
        if (minX - 6 < x0 || maxX + 6 > x1 || minZ - 6 < z0 || maxZ + 6 > z1 || g.size() < 24) {
            return;
        }
        s.geodes++;
        BlockPos centre = BlockPos.containing(sx / g.size(), sy / g.size(), sz / g.size());
        int good = 0;
        int wrong = 0;
        for (Direction d : Direction.values()) {
            int k = ray(level, centre, d, x0, z0, x1, z1);
            good += k == GOOD ? 1 : 0;
            wrong += k == WRONG ? 1 : 0;
        }
        int found = 0;
        int loose = 0;
        for (BlockPos c : clusters) {
            if (c.getX() < minX - 1 || c.getX() > maxX + 1 || c.getY() < minY - 1 || c.getY() > maxY + 1 || c.getZ() < minZ - 1 || c.getZ() > maxZ + 1) {
                continue;
            }
            found++;
            boolean beside = false;
            for (Direction d : Direction.values()) {
                BlockState n = level.getBlockState(c.relative(d));
                beside |= n.is(AutarkiaBlocks.LATTICE_GEODE_SHELL.get()) && n.getValue(LatticeGeodeShellBlock.CHARGED);
            }
            loose += beside ? 0 : 1;
        }
        if (wrong == 0) {
            s.clusters += found;
            s.loose += loose;
        }
        String line = String.format(Locale.ROOT, "geode at %s: %d shell blocks (%d charged), %d clusters (%d loose), rays %d good %d wrong", centre.toShortString(),
            g.size(), charged, found, loose, good, wrong);
        Autarkia.LOG.info("[TEST m8] {}", line);
        if (good >= 3 && wrong == 0) {
            if (!s.layered) {
                s.best = line;
            }
            s.layered = true;
        } else if (!s.layered) {
            s.best = line;
        }
    }

    private static int ray(ServerLevel level, BlockPos centre, Direction d, int x0, int z0, int x1, int z1) {
        int shell = -1;
        int calcite = -1;
        int blackstone = -1;
        for (int i = 1; i <= RAY; i++) {
            BlockPos q = centre.relative(d, i);
            if (q.getX() < x0 || q.getX() > x1 || q.getZ() < z0 || q.getZ() > z1) {
                break;
            }
            BlockState st = level.getBlockState(q);
            if (st.is(AutarkiaBlocks.LATTICE_GEODE_SHELL.get())) {
                shell = shell < 0 ? i : shell;
            } else if (st.is(Blocks.CALCITE)) {
                calcite = calcite < 0 ? i : calcite;
            } else if (st.is(Blocks.BLACKSTONE)) {
                blackstone = blackstone < 0 ? i : blackstone;
            } else if (!st.canBeReplaced() && !st.is(AutarkiaBlocks.LATTICE_CLUSTER.get())) {
                break;
            }
        }
        boolean wrong = (shell >= 0 && calcite >= 0 && calcite < shell) || (calcite >= 0 && blackstone >= 0 && blackstone < calcite)
            || (shell >= 0 && blackstone >= 0 && blackstone < shell);
        if (wrong) {
            return WRONG;
        }
        return shell >= 0 && calcite > shell && blackstone > calcite ? GOOD : PARTIAL;
    }
}

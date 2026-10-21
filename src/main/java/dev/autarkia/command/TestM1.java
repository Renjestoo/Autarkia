package dev.autarkia.command;

import com.mojang.serialization.DynamicOps;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GridSavedData;
import dev.autarkia.energy.HandCrankBlockEntity;
import dev.autarkia.energy.Span;
import dev.autarkia.logistics.ChuteBlockEntity;
import dev.autarkia.network.MultimeterReadingPayload;
import dev.autarkia.processing.CrucibleBlockEntity;
import dev.autarkia.processing.MachineRecipe;
import dev.autarkia.processing.MachineRecipeKind;
import dev.autarkia.processing.MachineStatus;
import dev.autarkia.processing.QuernBlockEntity;
import dev.autarkia.processing.StampMillBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaRecipes;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.storage.CrateBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;

/** {@code /autarkia test m1}: the M1 checks of 12-build-plan (02 #1, #2; 03 #1, #7 to #10; 04 L1, L2 and extras). */
public final class TestM1 {
    private TestM1() {
    }

    static void place(SelfTest t, BlockPos p, BlockState s) {
        t.level.setBlock(p, s, Block.UPDATE_ALL);
    }

    static boolean placeBig(SelfTest t, BlockPos center, Block block, Direction facing) {
        return placeBigAt(t.level, center, block, facing);
    }

    /** Place a big block by code (09 R2): canPlace on the center state, then setBlock on the center. */
    static boolean placeBigAt(net.minecraft.server.level.ServerLevel level, BlockPos center, Block block, Direction facing) {
        BlockState st = block.defaultBlockState().setValue(BaseMultiblock.CENTER, true).setValue(HorizontalDirectionalBlock.FACING, facing);
        if (!((BaseMultiblock) block).canPlace(level, center, st, null, false)) {
            return false;
        }
        level.setBlock(center, st, Block.UPDATE_ALL);
        return true;
    }

    /** Insert a stack through the item capability of a block (top face); returns the inserted count. */
    static int insertAt(net.minecraft.server.level.ServerLevel level, BlockPos p, ItemStack stack) {
        ResourceHandler<ItemResource> h = level.getCapability(Capabilities.Item.BLOCK, p, Direction.UP);
        if (h == null) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int n = h.insert(ItemResource.of(stack), stack.getCount(), tx);
            tx.commit();
            return n;
        }
    }

    static int countItems(SelfTest t, BlockPos around, double r, Item item) {
        int n = 0;
        for (ItemEntity e : t.level.getEntitiesOfClass(ItemEntity.class, new AABB(around).inflate(r))) {
            if (e.getItem().is(item)) {
                n += e.getItem().getCount();
            }
        }
        return n;
    }

    static void killItems(SelfTest t, BlockPos around, double r) {
        t.level.getEntitiesOfClass(ItemEntity.class, new AABB(around).inflate(r)).forEach(e -> e.discard());
    }

    static int crateCount(SelfTest t, BlockPos p, Item item) {
        if (!(t.level.getBlockEntity(p) instanceof CrateBlockEntity c)) {
            return -1;
        }
        int n = 0;
        for (int i = 0; i < CrateBlockEntity.SLOTS; i++) {
            if (c.slot(i).is(item)) {
                n += c.slot(i).getCount();
            }
        }
        return n;
    }

    static int pushEnergy(SelfTest t, BlockPos p, int amount) {
        EnergyHandler h = t.level.getCapability(Capabilities.Energy.BLOCK, p, Direction.UP);
        if (h == null) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int n = h.insert(amount, tx);
            tx.commit();
            return n;
        }
    }

    static int insertItems(SelfTest t, BlockPos p, ItemStack stack) {
        return insertAt(t.level, p, stack);
    }

    static Span spanBetween(SelfTest t, BlockPos a, BlockPos b) {
        GridSavedData d = Grid.of(t.level).data();
        for (Span s : d.spans.values()) {
            if (s.joins(a.asLong(), b.asLong())) {
                return s;
            }
        }
        return null;
    }

    /** Build the rigs and run the M1 script. */
    public static int run(CommandSourceStack src) {
        SelfTest t = SelfTest.create("m1", src);
        FakePlayer fake = FakePlayerFactory.getMinecraft(t.level);
        BlockPos crank = t.at(0, 0, 0);
        BlockPos pulley = t.at(8, 0, 0);
        BlockPos mill = t.at(11, 0, 0);
        long[] rebuilds = new long[1];
        int[] crankTick = new int[1];

        t.then(x -> x.clear(-2, 0, -4, 92, 10, 6));
        t.then(x -> {
            place(x, crank, AutarkiaBlocks.HAND_CRANK.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
            place(x, pulley, AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState());
            x.report("place stamp mill by code", placeBig(x, mill, AutarkiaBlocks.STAMP_MILL.get(), Direction.SOUTH),
                "canPlace then setBlock on the center");
        });
        t.then(x -> {
            Grid.SpanResult r1 = Grid.addSpan(x.level, crank, Direction.EAST, pulley, Direction.WEST, CableTier.ROPE, false);
            Grid.SpanResult r2 = Grid.addSpan(x.level, pulley, Direction.EAST, mill, Direction.WEST, CableTier.ROPE, false);
            x.report("02#1 spans crank-pulley-mill", r1 == Grid.SpanResult.OK && r2 == Grid.SpanResult.OK, r1 + ", " + r2);
            insertItems(x, mill, new ItemStack(Items.RAW_IRON, 16));
        });
        t.repeat(300, x -> {
            if (x.level.getGameTime() % 4 == 0 && x.level.getBlockEntity(crank) instanceof HandCrankBlockEntity c) {
                c.crank(fake);
                crankTick[0] = (int) x.tick;
            }
        });
        t.check("02#1 stamp mill runs on the crank", () -> be(t,mill) instanceof StampMillBlockEntity m && m.machineStatus().state() == MachineStatus.State.RUNNING,
            () -> "state " + (be(t,mill) instanceof StampMillBlockEntity m ? m.machineStatus() : "missing"));
        t.check("02#1 multimeter generation ~20, loss ~0.6", () -> {
            MultimeterReadingPayload r = Grid.reading(t.level, pulley);
            return r != null && r.connected() && r.generation() > 15 && r.generation() < 24 && r.loss() > 0.2 && r.loss() < 1.2;
        }, () -> {
            MultimeterReadingPayload r = Grid.reading(t.level, pulley);
            return r == null ? "no reading" : String.format("gen %.2f demand %.2f loss %.3f phi %.2f", r.generation(), r.demand(), r.loss(), r.phi());
        });
        t.waitTicks(7);
        t.check("02#1 output stops within 6 ticks of release", () -> be(t,crank) instanceof HandCrankBlockEntity c && c.output() == 0,
            () -> "output " + (be(t,crank) instanceof HandCrankBlockEntity c ? c.output() : -1));
        t.check("multimeter reading lists both pulley spans", () -> {
            MultimeterReadingPayload r = Grid.reading(t.level, pulley);
            return r != null && r.lines().size() == 2 && r.tier() == CableTier.ROPE;
        }, () -> "lines " + (Grid.reading(t.level, pulley) == null ? -1 : Grid.reading(t.level, pulley).lines().size()));

        t.then(x -> {
            killItems(x, pulley, 3);
            x.level.destroyBlock(pulley, true);
            rebuilds[0] = Grid.of(x.level).rebuildsTotal;
        });
        t.waitTicks(5);
        t.check("02#2 breaking the pulley drops the span items", () -> countItems(t, pulley, 2.5, AutarkiaItems.ROPE_COIL.get()) == 2,
            () -> countItems(t, pulley, 2.5, AutarkiaItems.ROPE_COIL.get()) + " rope coils");
        t.check("02#2 network split: crank has no network", () -> Grid.of(t.level).network(crank.asLong()) == null, () -> "crank network gone");
        t.then(x -> rebuilds[0] = Grid.of(x.level).rebuildsTotal);
        t.waitTicks(40);
        t.check("02#2 no rebuild in later ticks", () -> Grid.of(t.level).rebuildsTotal == rebuilds[0],
            () -> "rebuilds " + rebuilds[0] + " -> " + Grid.of(t.level).rebuildsTotal);

        refusals(t);
        slipAndSnap(t, fake);
        saveFormat(t);
        recipesAndFilter(t, mill);
        quern(t, fake);
        crucibles(t);
        stampMill(t);
        worldgen(t);
        chute(t);
        loot(t);
        return t.start();
    }

    private static Object be(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p);
    }

    private static void refusals(SelfTest t) {
        BlockPos p1 = t.at(20, 0, 0);
        t.then(x -> {
            BlockState pul = AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState();
            place(x, p1, pul);
            place(x, x.at(34, 0, 0), pul);
            place(x, x.at(26, 0, 2), pul);
            place(x, x.at(23, 0, 1), Blocks.STONE.defaultBlockState());
            place(x, x.at(20, 0, 1), Blocks.DIRT.defaultBlockState());
            x.report("span refused: too long", Grid.addSpan(x.level, p1, Direction.UP, x.at(34, 0, 0), Direction.UP, CableTier.ROPE, true)
                == Grid.SpanResult.TOO_LONG, "14 blocks > 12");
            x.report("span refused: blocked chord", Grid.addSpan(x.level, p1, Direction.UP, x.at(26, 0, 2), Direction.UP, CableTier.ROPE, true)
                == Grid.SpanResult.BLOCKED, "stone in the chord");
            x.report("span refused: not a node", Grid.addSpan(x.level, p1, Direction.UP, x.at(20, 0, 1), Direction.UP, CableTier.ROPE, true)
                == Grid.SpanResult.NOT_A_NODE, "dirt");
            x.report("span refused: wrong tier", Grid.addSpan(x.level, p1, Direction.UP, x.at(34, 0, 0), Direction.UP, CableTier.COPPER, true)
                == Grid.SpanResult.WRONG_TIER, "copper on a rope pulley");
            int ok = 0;
            List<BlockPos> targets = List.of(x.at(20, 0, -3), x.at(18, 0, -2), x.at(22, 0, -2), x.at(20, 3, 0));
            for (int i = 0; i < 4; i++) {
                BlockPos q = targets.get(i);
                place(x, q, pul);
                if (Grid.addSpan(x.level, p1, Direction.UP, q, Direction.UP, CableTier.ROPE, false) == Grid.SpanResult.OK) {
                    ok++;
                }
            }
            place(x, x.at(17, 0, 0), pul);
            int fourOk = ok;
            x.report("span refused: node full (5th span)", fourOk == 4
                && Grid.addSpan(x.level, p1, Direction.UP, x.at(17, 0, 0), Direction.UP, CableTier.ROPE, true) == Grid.SpanResult.NODE_FULL,
                fourOk + " spans placed, then refused");
        });
    }

    private static void slipAndSnap(SelfTest t, FakePlayer fake) {
        BlockPos pa = t.at(42, 0, 0);
        BlockPos pb = t.at(52, 0, 0);
        List<BlockPos> cranks = List.of(t.at(42, 0, 1), t.at(42, 0, -1), t.at(41, 0, 0));
        List<BlockPos> mills = List.of(t.at(53, 0, 0), t.at(52, 0, 1), t.at(52, 0, -1));
        t.then(x -> {
            place(x, pa, AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState());
            place(x, pb, AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState());
            for (BlockPos c : cranks) {
                place(x, c, AutarkiaBlocks.HAND_CRANK.get().defaultBlockState());
            }
            for (BlockPos m : mills) {
                placeBig(x, m, AutarkiaBlocks.STAMP_MILL.get(), Direction.NORTH);
                insertItems(x, m, new ItemStack(Items.RAW_COPPER, 64));
            }
            x.report("slip rig span", Grid.addSpan(x.level, pa, Direction.UP, pb, Direction.UP, CableTier.ROPE, false) == Grid.SpanResult.OK, "10 blocks");
        });
        float[] maxFlow = new float[1];
        t.waitUntil("rope slips above 40 FE/t", 200, () -> {
            Span s = spanBetween(t, pa, pb);
            return s != null && s.slip;
        }, x -> crankAll(x, cranks, fake));
        t.repeat(100, x -> {
            crankAll(x, cranks, fake);
            Span s = spanBetween(x, pa, pb);
            if (s != null) {
                maxFlow[0] = (float) Math.max(maxFlow[0], Math.abs(s.flow));
            }
        });
        t.check("rope slip caps delivered flow at 40", () -> maxFlow[0] <= 40.5f && maxFlow[0] > 30, () -> "max flow " + maxFlow[0]);
        t.check("rope wear rises while slipping", () -> spanBetween(t, pa, pb) != null && spanBetween(t, pa, pb).wear > 0.05f,
            () -> "wear " + (spanBetween(t, pa, pb) == null ? "gone" : spanBetween(t, pa, pb).wear));
        t.then(x -> killItems(x, x.at(47, 0, 0), 8));
        t.waitUntil("rope snaps from wear", 2000, () -> spanBetween(t, pa, pb) == null, x -> crankAll(x, cranks, fake));
        t.waitTicks(110);
        t.check("snap: two cable ends drop ceil(L/16) rope coil each after their lifetime",
            () -> countItems(t, t.at(47, 0, 0), 9, AutarkiaItems.ROPE_COIL.get()) == 2,
            () -> countItems(t, t.at(47, 0, 0), 9, AutarkiaItems.ROPE_COIL.get()) + " rope coils");
    }

    private static void crankAll(SelfTest t, List<BlockPos> cranks, FakePlayer fake) {
        if (t.level.getGameTime() % 4 != 0) {
            return;
        }
        for (BlockPos c : cranks) {
            if (t.level.getBlockEntity(c) instanceof HandCrankBlockEntity be) {
                be.crank(fake);
            }
        }
    }

    private static void saveFormat(SelfTest t) {
        t.check("grid save format round trip", () -> {
            GridSavedData d = Grid.of(t.level).data();
            DynamicOps<Tag> ops = t.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            Tag tag = GridSavedData.CODEC.encodeStart(ops, d).getOrThrow();
            GridSavedData back = GridSavedData.CODEC.parse(ops, tag).getOrThrow();
            if (back.spans.size() != d.spans.size() || back.nextSpanId != d.nextSpanId) {
                return false;
            }
            for (Span s : d.spans.values()) {
                Span b = back.spans.get(s.id);
                if (b == null || b.a != s.a || b.b != s.b || b.tier != s.tier || Math.abs(b.length - s.length) > 1e-4 || Math.abs(b.wear - s.wear) > 1e-6) {
                    return false;
                }
            }
            return true;
        }, () -> Grid.of(t.level).data().spans.size() + " spans");
    }

    private static void recipesAndFilter(SelfTest t, BlockPos mill) {
        t.check("crushing recipes for iron, copper, gold, tin", () -> {
            List<String> metals = new ArrayList<>(List.of("iron", "copper", "gold", "tin"));
            for (RecipeHolder<MachineRecipe> h : t.level.recipeAccess().recipeMap().byType(AutarkiaRecipes.type(MachineRecipeKind.CRUSHING))) {
                metals.removeIf(m -> h.id().identifier().getPath().equals("crushing/" + m + "_from_raw") && h.value().allows("quern") && h.value().allows("stamp_mill"));
            }
            return metals.isEmpty();
        }, () -> "quern and stamp_mill stations");
        t.check("alloying bronze recipe (crucible)", () -> t.level.recipeAccess().recipeMap().byType(AutarkiaRecipes.type(MachineRecipeKind.ALLOYING)).stream()
            .anyMatch(h -> h.value().allows("crucible") && h.value().assemble(null).is(AutarkiaItems.BRONZE_INGOT.get()) && h.value().assemble(null).getCount() == 4),
            () -> "4 bronze from 3 copper + 1 tin");
        t.check("insert filter: stamp mill refuses dirt, accepts raw iron", () -> insertItems(t, mill, new ItemStack(Items.DIRT, 4)) == 0
            && insertItems(t, mill, new ItemStack(Items.RAW_IRON, 1)) == 1, () -> "automation handler");
        t.check("03#8 crushed tin smelts to 1 tin ingot", () -> t.level.recipeAccess().getRecipeFor(RecipeType.SMELTING,
                new SingleRecipeInput(new ItemStack(AutarkiaItems.CRUSHED_TIN.get())), t.level)
            .map(h -> h.value().assemble(new SingleRecipeInput(new ItemStack(AutarkiaItems.CRUSHED_TIN.get()))).is(AutarkiaItems.TIN_INGOT.get())).orElse(false),
            () -> "furnace recipe");
    }

    private static void quern(SelfTest t, FakePlayer fake) {
        BlockPos q = t.at(60, 1, 0);
        BlockPos below = t.at(60, 0, 0);
        t.then(x -> {
            place(x, below, AutarkiaBlocks.CRATE.get().defaultBlockState());
            place(x, q, AutarkiaBlocks.QUERN.get().defaultBlockState());
            if (x.level.getBlockEntity(q) instanceof QuernBlockEntity be) {
                for (int i = 0; i < 4; i++) {
                    be.insert(new ItemStack(AutarkiaItems.RAW_TIN.get(), 16));
                }
            }
        });
        int[] start = new int[1];
        t.then(x -> start[0] = (int) x.tick);
        t.waitUntil("03#7 quern empties 64 raw tin", 64 * 80 + 400, () -> be(t,q) instanceof QuernBlockEntity be && be.inventory().queue() == 0
            && be.inventory().get(1).isEmpty() && be.inventory().get(2).isEmpty(), x -> {
                if (x.level.getGameTime() % 4 == 0 && x.level.getBlockEntity(q) instanceof QuernBlockEntity be) {
                    be.turn();
                }
            });
        t.check("03#7 quern yields 88 to 104 crushed tin, 8 turns per ore", () -> {
            int n = crateCount(t, below, AutarkiaItems.CRUSHED_TIN.get());
            int ticks = (int) t.tick - start[0];
            return n >= 88 && n <= 104 && ticks >= 64 * 80 && ticks <= 64 * 80 + 200;
        }, () -> crateCount(t, below, AutarkiaItems.CRUSHED_TIN.get()) + " crushed in " + ((int) t.tick - start[0]) + " ticks");
    }

    private static void crucibles(SelfTest t) {
        BlockPos[] pots = {t.at(64, 1, 0), t.at(66, 1, 0), t.at(68, 1, 0), t.at(70, 1, 0)};
        int[] done = {-1, -1, -1};
        int[] start = new int[1];
        t.then(x -> {
            BlockState furnace = Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.LIT, true);
            place(x, x.at(64, 0, 0), furnace);
            if (x.level.getBlockEntity(x.at(64, 0, 0)) instanceof FurnaceBlockEntity f) {
                f.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
                f.setItem(1, new ItemStack(Items.COAL, 16));
            }
            place(x, x.at(66, 0, 0), Blocks.CAMPFIRE.defaultBlockState());
            for (BlockPos rim : List.of(x.at(67, 0, 0), x.at(69, 0, 0), x.at(68, 0, 1), x.at(68, 0, -1))) {
                place(x, rim, Blocks.STONE.defaultBlockState());
            }
            place(x, x.at(68, 0, 0), Blocks.LAVA.defaultBlockState());
            for (BlockPos p : pots) {
                place(x, p, AutarkiaBlocks.CRUCIBLE.get().defaultBlockState());
                insertItems(x, p, new ItemStack(Items.COPPER_INGOT, 3));
                insertItems(x, p, new ItemStack(AutarkiaItems.TIN_INGOT.get(), 1));
            }
            start[0] = (int) x.tick;
        });
        t.waitUntil("03#9 crucibles finish", 700, () -> done[1] >= 0, x -> {
            for (int i = 0; i < 3; i++) {
                if (done[i] < 0 && x.level.getBlockEntity(pots[i]) instanceof CrucibleBlockEntity c && !c.inventory().get(2).isEmpty()) {
                    done[i] = (int) x.tick - start[0];
                }
            }
        });
        t.check("03#9 furnace 300, campfire 600, lava 200 ticks for 4 bronze", () -> Math.abs(done[0] - 300) <= 3 && Math.abs(done[1] - 600) <= 3
            && Math.abs(done[2] - 200) <= 3 && be(t,pots[0]) instanceof CrucibleBlockEntity c && c.inventory().get(2).getCount() == 4,
            () -> "furnace " + done[0] + ", campfire " + done[1] + ", lava " + done[2]);
        t.check("03#9 no heat: NO_CONDITION", () -> be(t,pots[3]) instanceof CrucibleBlockEntity c && c.machineStatus().state() == MachineStatus.State.NO_CONDITION,
            () -> be(t,pots[3]) instanceof CrucibleBlockEntity c ? c.machineStatus().state().toString() : "missing");
    }

    private static void stampMill(SelfTest t) {
        BlockPos m = t.at(74, 0, 0);
        BlockPos out = t.at(74, 0, 3);
        long[] fe = new long[1];
        int[] crushed = new int[1];
        t.then(x -> {
            placeBig(x, m, AutarkiaBlocks.STAMP_MILL.get(), Direction.NORTH);
            place(x, out, AutarkiaBlocks.CRATE.get().defaultBlockState());
            insertItems(x, m, new ItemStack(Items.RAW_IRON, 64));
        });
        t.waitUntil("03#10 stamp mill crushes 64 raw iron", 64 * 160 + 600, () -> be(t,m) instanceof StampMillBlockEntity s && s.inventory().queue() == 0
            && !s.running(), x -> {
                fe[0] += pushEnergy(x, m, 64);
                if (x.level.getBlockEntity(m) instanceof StampMillBlockEntity s) {
                    for (int i = 1; i <= 2; i++) {
                        crushed[0] += s.inventory().get(i).getCount();
                        s.inventory().put(i, ItemStack.EMPTY);
                    }
                }
            });
        t.check("03#10 88 to 104 crushed iron, 2,560 FE per ore", () -> {
            int stored = be(t,m) instanceof StampMillBlockEntity s && s.energy() != null ? s.energy().stored() : 0;
            long used = fe[0] - stored;
            return crushed[0] >= 88 && crushed[0] <= 104 && used == 64L * 2560;
        }, () -> crushed[0] + " crushed, " + (fe[0] - (be(t,m) instanceof StampMillBlockEntity s && s.energy() != null ? s.energy().stored() : 0)) + " FE used");
    }

    private static void worldgen(SelfTest t) {
        t.check("03#1 tin ore generates in the chunks around the test", () -> {
            int n = 0;
            BlockPos c = t.origin;
            for (int dx = -32; dx <= 32; dx++) {
                for (int dz = -32; dz <= 32; dz++) {
                    for (int y = -16; y <= 96; y++) {
                        BlockState s = t.level.getBlockState(new BlockPos(c.getX() + dx, y, c.getZ() + dz));
                        if (s.is(AutarkiaBlocks.TIN_ORE.get()) || s.is(AutarkiaBlocks.DEEPSLATE_TIN_ORE.get())) {
                            n++;
                        }
                    }
                }
            }
            return n > 0;
        }, () -> "scan 65 x 65 columns, y -16 to 96");
    }

    private static List<ItemStack> drops(SelfTest t, BlockPos p, ItemStack tool) {
        BlockState s = t.level.getBlockState(p);
        net.minecraft.world.level.storage.loot.LootParams.Builder b = new net.minecraft.world.level.storage.loot.LootParams.Builder(t.level)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(p))
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, tool)
            .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY, t.level.getBlockEntity(p));
        return s.getDrops(b);
    }

    private static void loot(SelfTest t) {
        BlockPos ore = t.at(88, 0, 0);
        BlockPos mill = t.at(90, 0, 0);
        BlockPos crate = t.at(86, 0, 0);
        t.then(x -> {
            place(x, ore, AutarkiaBlocks.TIN_ORE.get().defaultBlockState());
            placeBig(x, mill, AutarkiaBlocks.STAMP_MILL.get(), Direction.NORTH);
            place(x, crate, AutarkiaBlocks.CRATE.get().defaultBlockState());
        });
        t.check("loot: tin ore drops raw tin with a pickaxe", () -> {
            List<ItemStack> d = drops(t, ore, new ItemStack(Items.IRON_PICKAXE));
            return d.size() == 1 && d.getFirst().is(AutarkiaItems.RAW_TIN.get());
        }, () -> drops(t, ore, new ItemStack(Items.IRON_PICKAXE)).toString());
        t.check("loot: stamp mill center drops 1 stamp mill, top position drops nothing", () -> {
            List<ItemStack> c = drops(t, mill, ItemStack.EMPTY);
            List<ItemStack> top = drops(t, mill.above(), ItemStack.EMPTY);
            return c.size() == 1 && c.getFirst().is(AutarkiaItems.STAMP_MILL.get()) && top.isEmpty();
        }, () -> drops(t, mill, ItemStack.EMPTY) + " / " + drops(t, mill.above(), ItemStack.EMPTY));
        t.check("loot: crate drops itself", () -> {
            List<ItemStack> d = drops(t, crate, ItemStack.EMPTY);
            return d.size() == 1 && d.getFirst().is(AutarkiaItems.CRATE.get());
        }, () -> drops(t, crate, ItemStack.EMPTY).toString());
    }

    private static void chute(SelfTest t) {
        BlockPos c1 = t.at(82, 0, 0);
        BlockPos ch = t.at(82, 1, 0);
        BlockPos c2 = t.at(83, 0, 0);
        int[] fed = new int[1];
        t.then(x -> {
            killItems(x, ch, 6);
            place(x, c1, AutarkiaBlocks.CRATE.get().defaultBlockState());
            place(x, ch, AutarkiaBlocks.CHUTE.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.EAST));
            place(x, c2, AutarkiaBlocks.CRATE.get().defaultBlockState());
        });
        t.waitUntil("04 L1 four entries slide into the lower crate", 400, () -> crateCount(t, c2, Items.COBBLESTONE) == 16, x -> {
            if (fed[0] < 4 && x.level.getBlockEntity(ch) instanceof ChuteBlockEntity be && be.accept(new ItemStack(Items.COBBLESTONE, 4))) {
                fed[0]++;
            }
        });
        t.check("04 L1 16 cobblestone arrive, no item entities", () -> crateCount(t, c2, Items.COBBLESTONE) == 16
            && t.level.getEntitiesOfClass(ItemEntity.class, new AABB(ch).inflate(4)).isEmpty(), () -> crateCount(t, c2, Items.COBBLESTONE) + " in the crate");
        t.then(x -> {
            if (x.level.getBlockEntity(ch) instanceof ChuteBlockEntity be) {
                be.accept(new ItemStack(Items.COBBLESTONE, 4));
            }
            x.level.removeBlock(c2, false);
            killItems(x, ch, 4);
        });
        t.waitTicks(ChuteBlockEntity.STUCK_TICKS + 40);
        t.check("04 L2 items wait at the low end, stuck after 30 s, nothing drops", () -> be(t,ch) instanceof ChuteBlockEntity be && be.segment().blocked
            && be.stuck() && be.segment().entries.size() == 1 && t.level.getEntitiesOfClass(ItemEntity.class, new AABB(ch).inflate(4)).isEmpty(),
            () -> be(t,ch) instanceof ChuteBlockEntity be ? "blocked " + be.segment().blocked + ", ticks " + be.blockedTicks() : "missing");
    }
}

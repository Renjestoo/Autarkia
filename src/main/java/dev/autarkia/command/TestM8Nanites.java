package dev.autarkia.command;

import com.mojang.authlib.GameProfile;
import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyActions;
import dev.autarkia.colony.NaniteClaim;
import dev.autarkia.colony.NaniteClaims;
import dev.autarkia.colony.NaniteEngine;
import dev.autarkia.colony.NaniteHiveBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaTags;
import dev.autarkia.storage.MatterBufferBlockEntity;
import dev.autarkia.storage.MatterValues;
import dev.autarkia.worker.NaniteSwarmEntity;
import dev.autarkia.worker.WorkerAssemblyRecipe;
import dev.autarkia.worker.WorkerType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * The nanite part of {@code /autarkia test m8} (05b section 11.2 W18 and W18 kill, 06 section 8 #26 and #27) on x 358..392,
 * z 2..22 inside the T5 test colony (Core at x 400, z 30): a 12 x 12 claim (x 362..373, z 6..17) over a hill of dirt and
 * stone with 5 iron ore and 2 coal ore, a buried chest and an Autarkia crate with items, 2 bedrock cells at the pit
 * bottom (y -7), a hive at x 380 and a matter buffer at x 386, both fed by the test. An unconnected real player, trusted
 * by the colony, starts the claim through colony_action (depth 7, terraces on) and must get both advancements. About
 * 3,000 ticks under sprint.
 */
final class TestM8Nanites {
    private static final UUID NANITE_PLAYER = UUID.nameUUIDFromBytes("autarkia-m8-nanite".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final UUID STRANGER = UUID.nameUUIDFromBytes("autarkia-m8-stranger".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final int DEPTH = 7;
    private static @Nullable ServerPlayer player;
    private static final long[] BUFFER_START = new long[2];
    private static final int[] COUNTS = new int[2];
    private static final boolean[] FLAGS = new boolean[4];
    private static final int[] LAST_Y = {Integer.MAX_VALUE};
    private static final long[] SAVED = new long[3];
    private static final double[] SWARM_DIST = {-1, Double.MAX_VALUE};
    private static final List<Vec3> SPAWN = new ArrayList<>();

    private TestM8Nanites() {
    }

    static void run(TestM8.Run r) {
        formulas(r);
        setup(r);
        start(r);
        kill(r);
        damage(r);
        finish(r);
    }

    private static BlockPos hive(TestM8.Run r) {
        return r.at(380, 0, 10);
    }

    private static BlockPos buffer(TestM8.Run r) {
        return r.at(386, 0, 10);
    }

    private static BlockPos markerA(TestM8.Run r) {
        return r.at(362, 0, 6);
    }

    private static BlockPos markerB(TestM8.Run r) {
        return r.at(373, 0, 17);
    }

    private static BlockPos chest(TestM8.Run r) {
        return r.at(367, -3, 11);
    }

    private static BlockPos crate(TestM8.Run r) {
        return r.at(369, -1, 13);
    }

    private static List<BlockPos> bedrock(TestM8.Run r) {
        return List.of(r.at(367, -7, 11), r.at(368, -7, 12));
    }

    private static List<BlockPos> irons(TestM8.Run r) {
        return List.of(r.at(365, 2, 9), r.at(370, 2, 9), r.at(365, 2, 14), r.at(370, 2, 14), r.at(364, -2, 8));
    }

    private static List<BlockPos> coals(TestM8.Run r) {
        return List.of(r.at(366, 3, 10), r.at(369, 3, 13));
    }

    /** Return the claim geometry the test expects, built with the same rules as the engine but from the test's own numbers. */
    private static NaniteClaim shape(TestM8.Run r) {
        return NaniteClaim.create(hive(r), r.at(362, -DEPTH, 6), r.at(373, 4, 17), r.at(0, 0, 0).getY(), true, 0);
    }

    private static ServerPlayer player(SelfTest t) {
        if (player == null || player.level() != t.level) {
            player = new ServerPlayer(t.level.getServer(), t.level, new GameProfile(NANITE_PLAYER, "AutarkiaNanite"), ClientInformation.createDefault());
        }
        return player;
    }

    private static @Nullable NaniteHiveBlockEntity hiveBe(TestM8.Run r) {
        return r.t().level.getBlockEntity(hive(r)) instanceof NaniteHiveBlockEntity h ? h : null;
    }

    private static @Nullable MatterBufferBlockEntity bufferBe(TestM8.Run r) {
        return r.t().level.getBlockEntity(buffer(r)) instanceof MatterBufferBlockEntity b ? b : null;
    }

    private static @Nullable NaniteClaim claim(TestM8.Run r) {
        Colony c = TestM8.colony();
        return c == null ? null : NaniteClaims.lastClaim(c, hive(r));
    }

    private static @Nullable NaniteSwarmEntity swarm(TestM8.Run r) {
        NaniteClaim cl = claim(r);
        if (cl == null || cl.swarms.isEmpty()) {
            return null;
        }
        return r.t().level.getEntity(cl.swarms.get(0)) instanceof NaniteSwarmEntity s ? s : null;
    }

    private static boolean done(SelfTest t, String path) {
        AdvancementHolder h = t.level.getServer().getAdvancements().get(Autarkia.id(path));
        return h != null && player(t).getAdvancements().getOrStartProgress(h).isDone();
    }

    /** Feed the hive and the buffer, and sample the cursor layer and the swarm distance every tick. */
    private static void watch(SelfTest t, TestM8.Run r) {
        TestM1.pushEnergyAt(t.level, hive(r), NaniteHiveBlockEntity.INPUT);
        TestM1.pushEnergyAt(t.level, buffer(r), 8000);
        NaniteClaim cl = claim(r);
        if (cl != null && cl.state != NaniteClaim.State.DONE) {
            FLAGS[0] |= cl.cursorY > LAST_Y[0];
            LAST_Y[0] = cl.cursorY;
        }
        NaniteSwarmEntity s = swarm(r);
        if (s != null && s.phase() == NaniteSwarmEntity.Phase.TRAVEL && cl != null) {
            double d = s.position().distanceTo(Vec3.atBottomCenterOf(cl.cell(cl.max.getY(), 0)).add(0, -1, 0));
            FLAGS[1] = true;
            FLAGS[2] |= d > SWARM_DIST[1] + 1e-6;
            SWARM_DIST[1] = d;
        }
    }

    private static void formulas(TestM8.Run r) {
        SelfTest t = r.t();
        t.check("W18 data: nanite_hive, claim_marker, nanite_swarm, the hive menu, the swarm recipe (4 nanite_paste, 200 ticks, 100 FE/t, hive), "
            + "advancements colony/clean_pit and workers/claim_done", () -> {
                var recipe = t.level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Autarkia.id("worker_assembly/nanite_swarm")));
                boolean rec = recipe.isPresent() && recipe.get().value() instanceof WorkerAssemblyRecipe w && w.forMachine(NaniteHiveBlockEntity.ID)
                    && w.frame().isEmpty() && w.time() == 200 && w.fePerTick() == 100 && w.minTier() == 5 && w.parts().size() == 1
                    && w.parts().get(0).count() == 4 && w.parts().get(0).ingredient().test(new ItemStack(AutarkiaItems.NANITE_PASTE.get()));
                return rec && BuiltInRegistries.BLOCK.containsKey(Autarkia.id("nanite_hive")) && BuiltInRegistries.BLOCK.containsKey(Autarkia.id("claim_marker"))
                    && BuiltInRegistries.ITEM.containsKey(Autarkia.id("nanite_hive")) && BuiltInRegistries.ITEM.containsKey(Autarkia.id("claim_marker"))
                    && BuiltInRegistries.ENTITY_TYPE.containsKey(Autarkia.id("nanite_swarm")) && BuiltInRegistries.MENU.containsKey(Autarkia.id("nanite_hive"))
                    && BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(Autarkia.id("nanite_hive"))
                    && t.level.getServer().getAdvancements().get(Autarkia.id("colony/clean_pit")) != null
                    && t.level.getServer().getAdvancements().get(Autarkia.id("workers/claim_done")) != null;
            }, () -> "recipe worker_assembly/nanite_swarm");
        t.check("W18 budget split: 48 over 5 claims gives 10, 10, 10, 9, 9 (remainder to the oldest); 1 claim gets 48", () ->
            Arrays.equals(NaniteClaims.shares(48, 5), new int[] {10, 10, 10, 9, 9}) && Arrays.equals(NaniteClaims.shares(48, 1), new int[] {48})
                && NaniteClaims.shares(48, 0).length == 0, () -> Arrays.toString(NaniteClaims.shares(48, 5)));
        t.check("W18 FE per edit min(2000, 200 + 50 x destroyTime): stone 275, iron ore 350, obsidian 2000 (cap)", () -> NaniteEngine.feCost(1.5f) == 275
            && NaniteEngine.feCost(3.0f) == 350 && NaniteEngine.feCost(50f) == 2000, () -> NaniteEngine.feCost(1.5f) + ", " + NaniteEngine.feCost(3.0f) + ", "
            + NaniteEngine.feCost(50f));
        t.check("W18 terraces: 12 wide box, ref_y 0: y -1 full 12, y -2 inset 1 (10), y -7 inset 3 (6), y -9 stops at 4; a 6 wide box stops at 4 "
            + "from y -2; serpentine rows along x", () -> {
                NaniteClaim big = NaniteClaim.create(BlockPos.ZERO, new BlockPos(0, -9, 0), new BlockPos(11, 4, 11), 0, true, 0);
                NaniteClaim small = NaniteClaim.create(BlockPos.ZERO, new BlockPos(0, -9, 0), new BlockPos(5, 0, 5), 0, true, 0);
                NaniteClaim flat = NaniteClaim.create(BlockPos.ZERO, new BlockPos(0, -9, 0), new BlockPos(11, 4, 11), 0, false, 0);
                return big.area(-1) == 144 && big.area(-2) == 100 && big.area(-7) == 36 && big.area(-9) == 16 && big.area(2) == 144 && small.area(-2) == 16
                    && small.area(-1) == 36 && flat.area(-9) == 144 && big.cell(-1, 12).equals(new BlockPos(11, -1, 1))
                    && big.index(big.cell(-5, 23)) == 23;
            }, () -> "inset rule floor((ref_y - y) / 2)");
        t.check("06 swarm caps: cost 2, sub-cap T5 2, T6 3; tag nanite_immune holds reinforced deepslate", () -> WorkerType.NANITE_SWARM.capCost() == 2
            && WorkerType.NANITE_SWARM.subCap(5) == 2 && WorkerType.NANITE_SWARM.subCap(6) == 3
            && Blocks.REINFORCED_DEEPSLATE.defaultBlockState().is(AutarkiaTags.NANITE_IMMUNE), () -> "nanite_swarm worker type");
    }

    private static void box(SelfTest t, BlockPos a, BlockPos b, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            t.level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private static void setup(TestM8.Run r) {
        SelfTest t = r.t();
        AABB area = new AABB(Vec3.atCenterOf(r.at(356, -12, 0)), Vec3.atCenterOf(r.at(394, 14, 24)));
        t.then(x -> {
            BlockPos lo = r.at(356, 0, 0);
            BlockPos hi = r.at(394, 0, 24);
            x.command("forceload add " + lo.getX() + " " + lo.getZ() + " " + hi.getX() + " " + hi.getZ());
            NaniteClaims.resume();
            Colony c = TestM8.colony();
            if (c != null) {
                c.claims.removeIf(cl -> area.contains(Vec3.atCenterOf(cl.hivePos)));
            }
            x.level.getEntitiesOfClass(Entity.class, area, e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
            SelfTest.clearSpans(x.level, area);
        });
        t.then(x -> {
            box(x, r.at(358, 0, 2), r.at(392, 12, 22), Blocks.AIR.defaultBlockState());
            box(x, r.at(358, -10, 2), r.at(392, -1, 22), Blocks.STONE.defaultBlockState());
        });
        t.then(x -> {
            box(x, r.at(363, 0, 7), r.at(372, 0, 16), Blocks.DIRT.defaultBlockState());
            box(x, r.at(364, 1, 8), r.at(371, 1, 15), Blocks.STONE.defaultBlockState());
            box(x, r.at(365, 2, 9), r.at(370, 2, 14), Blocks.STONE.defaultBlockState());
            box(x, r.at(366, 3, 10), r.at(369, 3, 13), Blocks.STONE.defaultBlockState());
            box(x, r.at(367, 4, 11), r.at(368, 4, 12), Blocks.DIRT.defaultBlockState());
            irons(r).forEach(p -> x.level.setBlock(p, Blocks.IRON_ORE.defaultBlockState(), Block.UPDATE_ALL));
            coals(r).forEach(p -> x.level.setBlock(p, Blocks.COAL_ORE.defaultBlockState(), Block.UPDATE_ALL));
            bedrock(r).forEach(p -> x.level.setBlock(p, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL));
            x.level.setBlock(chest(r), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
            if (x.level.getBlockEntity(chest(r)) instanceof Container ct) {
                ct.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
            }
            x.level.setBlock(crate(r), AutarkiaBlocks.CRATE.get().defaultBlockState(), Block.UPDATE_ALL);
            TestM1.insertAt(x.level, crate(r), new ItemStack(Items.IRON_INGOT, 16));
            x.level.setBlock(markerA(r), AutarkiaBlocks.CLAIM_MARKER.get().defaultBlockState(), Block.UPDATE_ALL);
            x.level.setBlock(markerB(r), AutarkiaBlocks.CLAIM_MARKER.get().defaultBlockState(), Block.UPDATE_ALL);
        });
        t.then(x -> {
            boolean placed = TestM1.placeBigAt(x.level, hive(r), AutarkiaBlocks.NANITE_HIVE.get(), Direction.NORTH)
                && TestM1.placeBigAt(x.level, buffer(r), AutarkiaBlocks.MATTER_BUFFER.get(), Direction.NORTH);
            int paste = TestM1.insertAt(x.level, hive(r), new ItemStack(AutarkiaItems.NANITE_PASTE.get(), 4));
            MatterBufferBlockEntity mb = bufferBe(r);
            if (mb != null) {
                mb.setAutoDissolve(false);
            }
            Colony c = TestM8.colony();
            ServerPlayer p = player(x);
            if (c != null && !c.trusted.contains(p.getUUID())) {
                c.trusted.add(p.getUUID());
            }
            for (String a : List.of("colony/clean_pit", "workers/claim_done")) {
                AdvancementHolder h = x.level.getServer().getAdvancements().get(Autarkia.id(a));
                if (h != null) {
                    p.getAdvancements().revoke(h, "done");
                }
            }
            NaniteClaim geo = shape(r);
            int stone = 0;
            for (BlockPos q : BlockPos.betweenClosed(geo.min, geo.max)) {
                if (geo.inShape(q) && x.level.getBlockState(q).is(Blocks.STONE)) {
                    stone++;
                }
            }
            COUNTS[0] = stone;
            x.report("W18 setup: hill, buried chest and crate, 5 iron and 2 coal ore, bedrock, 2 markers, hive with 4 nanite_paste, matter buffer, T5 colony",
                placed && paste == 4 && c != null && c.tier == 5, "placed " + placed + ", paste " + paste + ", stone cells " + stone + ", tier "
                    + (c == null ? "none" : c.tier));
        });
        t.waitUntil("W18 setup: the hive is in the colony domain and links the formed matter buffer", 200, () -> {
            NaniteHiveBlockEntity h = hiveBe(r);
            Colony c = TestM8.colony();
            return h != null && c != null && c.id.equals(h.colonyId()) && NaniteEngine.linkedBuffer(t.level, c, hive(r)) == bufferBe(r) && bufferBe(r) != null;
        }, x -> watch(x, r));
        t.waitUntil("W18 the hive lists the marker pair (box 12 x 12, top y 4) within its 20-tick list refresh", 40, () -> {
            NaniteHiveBlockEntity h = hiveBe(r);
            return h != null && h.pairs().stream().anyMatch(p -> Set.of(p.a(), p.b()).equals(Set.of(markerA(r), markerB(r))) && p.yMax() == r.at(0, 4, 0).getY());
        }, x -> watch(x, r), () -> hiveBe(r) == null ? "no hive" : hiveBe(r).pairs() + ", refusal " + dev.autarkia.colony.NaniteClaims.refusal(t.level,
            TestM8.colony(), hive(r), markerA(r), markerB(r), r.at(0, 4, 0).getY(), 0) + ", markers found " + t.level.getPoiManager().getInSquare(
            h -> h.is(dev.autarkia.colony.NaniteClaims.MARKER_POI.key()), hive(r), 64, net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY).count()
            + ", hive top " + dev.autarkia.colony.NaniteClaims.yMax(t.level, markerA(r), markerB(r)) + " refusal there " + dev.autarkia.colony.NaniteClaims
            .refusal(t.level, TestM8.colony(), hive(r), markerA(r), markerB(r), dev.autarkia.colony.NaniteClaims.yMax(t.level, markerA(r), markerB(r)), 0));
    }

    private static void start(TestM8.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            MatterBufferBlockEntity b = bufferBe(r);
            BUFFER_START[0] = b == null ? 0 : b.used();
            BUFFER_START[1] = b == null ? 0 : b.free();
            NaniteEngine.resetMax();
            LAST_Y[0] = Integer.MAX_VALUE;
            Arrays.fill(FLAGS, false);
            SWARM_DIST[1] = Double.MAX_VALUE;
            SPAWN.clear();
            Colony c = TestM8.colony();
            ColonyActions.handle(FakePlayerFactory.get(x.level, new GameProfile(STRANGER, "AutarkiaStranger")), c.id, "claim",
                List.of(Long.toString(hive(r).asLong()), Long.toString(markerA(r).asLong()), Long.toString(markerB(r).asLong()), Integer.toString(DEPTH), "on"));
            x.report("W18 colony_action claim from a player who is neither owner nor trusted starts nothing (05 section 1.4)", claim(r) == null,
                claim(r) == null ? "refused" : "claim started");
            String why = NaniteClaims.start(x.level, c, hive(r), markerA(r), markerB(r), DEPTH, true, player(x));
            NaniteClaim cl = claim(r);
            x.report("W18 the trusted player selects the pair and starts: box x 362..373, y -7..4, z 6..17, ref_y 0, terraces on", why == null && cl != null
                && cl.min.equals(r.at(362, -DEPTH, 6)) && cl.max.equals(r.at(373, 4, 17)) && cl.refY == r.at(0, 0, 0).getY() && cl.terrace
                && cl.state == NaniteClaim.State.RUN, cl == null ? "refused: " + why : cl.min.toShortString() + " to " + cl.max.toShortString());
        });
        t.waitUntil("06#26 the hive assembles a swarm from 4 nanite_paste (200 ticks at 100 FE/t) and releases it", 600, () -> swarm(r) != null, x -> {
            watch(x, r);
            NaniteSwarmEntity s = swarm(r);
            if (s != null && SPAWN.isEmpty()) {
                SPAWN.add(s.position());
            }
        });
        t.check("06#26 the swarm starts above the hive, the paste is used, the colony registry counts it (cost 2)", () -> {
            NaniteSwarmEntity s = swarm(r);
            NaniteHiveBlockEntity h = hiveBe(r);
            Colony.WorkerEntry e = s == null ? null : TestM8.colony().workers.get(s.getUUID());
            return s != null && h != null && !SPAWN.isEmpty() && SPAWN.get(0).distanceTo(h.releasePoint()) <= 1.5 && h.inventory().get(0).isEmpty()
                && e != null && e.cost() == 2 && e.type().getPath().equals("nanite_swarm");
        }, () -> SPAWN.isEmpty() ? "no spawn" : "spawned " + SPAWN.get(0));
        t.waitUntil("06#26 the swarm travels from the hive to the box (TRAVEL, distance shrinking) and starts DISASSEMBLE", 600, () -> swarm(r) != null
            && swarm(r).phase() == NaniteSwarmEntity.Phase.DISASSEMBLE, x -> watch(x, r));
        t.check("06#26 it travelled on a straight approach: TRAVEL seen, the distance to the box never grew", () -> FLAGS[1] && !FLAGS[2],
            () -> "travel seen " + FLAGS[1] + ", grew " + FLAGS[2]);
        t.waitUntil("06#26 the first edit", 400, () -> NaniteClaims.stats(claim(r).id).firstEditY != Integer.MIN_VALUE, x -> watch(x, r));
        t.check("06#26 W18 the swarm takes the claim apart top layer first (first edit at y 4)", () -> NaniteClaims.stats(claim(r).id).firstEditY
            == r.at(0, 4, 0).getY(), () -> "first edit y " + NaniteClaims.stats(claim(r).id).firstEditY);
    }

    private static void kill(TestM8.Run r) {
        SelfTest t = r.t();
        t.waitUntil("W18 kill: the claim runs down to layer y 1", 4000, () -> claim(r).cursorY <= r.at(0, 1, 0).getY(), x -> watch(x, r),
            () -> NaniteClaims.status().toString());
        t.then(x -> {
            NaniteClaim cl = claim(r);
            x.command("autarkia nanite stop");
            SAVED[0] = cl.cursorY;
            SAVED[1] = cl.cursorI;
            SAVED[2] = NaniteClaims.stats(cl.id).units;
        });
        t.repeat(40, x -> {
            watch(x, r);
            FLAGS[3] |= NaniteClaims.stats(claim(r).id).units != SAVED[2];
        });
        t.check("W18 kill: /autarkia nanite stop stops every edit from that tick on (40 ticks, no edit unit, cursor unchanged, state PAUSED)", () -> {
            NaniteClaim cl = claim(r);
            return NaniteClaims.halted() && !FLAGS[3] && cl.cursorY == SAVED[0] && cl.cursorI == SAVED[1]
                && NaniteClaims.shown(cl) == NaniteClaim.State.PAUSED;
        }, () -> "units changed " + FLAGS[3] + ", cursor " + claim(r).cursorY + "/" + claim(r).cursorI + " saved " + SAVED[0] + "/" + SAVED[1]);
        t.then(x -> {
            x.command("autarkia nanite resume");
            NaniteClaim cl = claim(r);
            x.report("W18 kill: resume keeps the cursor where stop left it", !NaniteClaims.halted() && cl.cursorY == SAVED[0] && cl.cursorI == SAVED[1],
                cl.cursorY + "/" + cl.cursorI);
        });
        t.waitUntil("W18 kill: after resume the claim edits again from that cursor", 200, () -> NaniteClaims.stats(claim(r).id).units > SAVED[2],
            x -> watch(x, r));
    }

    private static void damage(TestM8.Run r) {
        SelfTest t = r.t();
        Vec3[] at = new Vec3[1];
        int[] paste = new int[1];
        long[] units = new long[1];
        boolean[] ok = new boolean[2];
        t.waitUntil("06#27 the claim reaches y -1 with an empty swarm buffer", 4000, () -> claim(r).cursorY <= r.at(0, -1, 0).getY() && swarm(r) != null
            && swarm(r).heldCount() == 0, x -> watch(x, r));
        t.then(x -> {
            NaniteSwarmEntity s = swarm(r);
            at[0] = s.position();
            ok[0] = !s.hurtServer(x.level, x.level.damageSources().fall(), 10f) && s.integrity() == NaniteSwarmEntity.MAX_INTEGRITY;
            paste[0] = TestM1.countItems(x, BlockPos.containing(at[0]), 6, AutarkiaItems.NANITE_PASTE.get());
            s.hurtServer(x.level, x.level.damageSources().playerAttack(player(x)), 25f);
            ok[1] = s.integrity() == 15f && s.isAlive();
            s.hurtServer(x.level, x.level.damageSources().playerAttack(player(x)), 25f);
            units[0] = NaniteClaims.stats(claim(r).id).units;
        });
        t.check("06#27 fall damage does nothing; player hits take integrity 40 to 15 to 0 and the swarm dissolves", () -> ok[0] && ok[1]
            && swarm(r) == null, () -> "fall ignored " + ok[0] + ", 15 left " + ok[1]);
        t.check("06#27 it drops 2 nanite_paste at its position", () -> TestM1.countItems(t, BlockPos.containing(at[0]), 6, AutarkiaItems.NANITE_PASTE.get())
            == paste[0] + NaniteSwarmEntity.DISSOLVE_PASTE, () -> TestM1.countItems(t, BlockPos.containing(at[0]), 6, AutarkiaItems.NANITE_PASTE.get())
            + " paste near " + at[0]);
        t.check("06#27 the claim releases it: no swarm on the claim, none in the colony registry, the claim is not done", () -> claim(r).swarms.isEmpty()
            && TestM8.colony().workerCount(WorkerType.NANITE_SWARM.id()) == 0 && claim(r).state != NaniteClaim.State.DONE, () -> NaniteClaims.status().toString());
        t.repeat(60, x -> watch(x, r));
        t.check("06#27 the claim waits for a new swarm: no edits, the hive shows that it needs paste", () -> NaniteClaims.stats(claim(r).id).units == units[0]
            && (hiveBe(r).flags() & NaniteHiveBlockEntity.FLAG_NEEDS_PASTE) != 0, () -> "flags " + hiveBe(r).flags());
        t.then(x -> {
            x.level.getEntitiesOfClass(ItemEntity.class, new AABB(BlockPos.containing(at[0])).inflate(6), e -> e.getItem().is(AutarkiaItems.NANITE_PASTE.get()))
                .forEach(Entity::discard);
            TestM1.insertAt(x.level, hive(r), new ItemStack(AutarkiaItems.NANITE_PASTE.get(), 4));
        });
        t.waitUntil("06#27 with 4 new nanite_paste the hive assembles a new swarm and the claim goes on", 1200,
            () -> NaniteClaims.stats(claim(r).id).units > units[0], x -> watch(x, r));
    }

    private static String pitReport(TestM8.Run r) {
        NaniteClaim geo = shape(r);
        List<String> bad = new ArrayList<>();
        for (BlockPos q : BlockPos.betweenClosed(geo.min, geo.max)) {
            BlockState s = r.t().level.getBlockState(q);
            boolean kept = q.equals(chest(r)) || q.equals(crate(r)) || bedrock(r).contains(q);
            boolean ok = geo.inShape(q) ? s.isAir() || kept : s.is(Blocks.STONE);
            if (!ok && bad.size() < 6) {
                bad.add(q.toShortString() + " " + BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath());
            }
        }
        return String.join(", ", bad);
    }

    private static void finish(TestM8.Run r) {
        SelfTest t = r.t();
        t.waitUntil("W18 the claim finishes (DONE)", 9000, () -> claim(r).state == NaniteClaim.State.DONE, x -> watch(x, r),
            () -> NaniteClaims.status().toString());
        t.waitUntil("W18 the swarm delivers its items, flies home and dissolves into 2 nanite_paste in the hive", 1200, () -> claim(r).swarms.isEmpty()
            && hiveBe(r).inventory().get(0).getCount() == NaniteSwarmEntity.DISSOLVE_PASTE, x -> watch(x, r));
        t.check("W18 layers went top down (the cursor layer never rose)", () -> !FLAGS[0], () -> "rose " + FLAGS[0]);
        t.check("W18 never more than 48 edits in a server tick", () -> NaniteEngine.maxEditsPerTick() > 0 && NaniteEngine.maxEditsPerTick() <= 48,
            () -> String.format(Locale.ROOT, "max %.3f", NaniteEngine.maxEditsPerTick()));
        t.check("W18 terraced pit: every claim cell is empty (inset floor((0 - y) / 2) per side below y 0, down to 4 x 4), the terrace steps and the "
            + "ground outside stay stone", () -> pitReport(r).isEmpty(), () -> pitReport(r));
        t.check("W18 bedrock stays", () -> bedrock(r).stream().allMatch(p -> t.level.getBlockState(p).is(Blocks.BEDROCK)), () -> "bedrock");
        t.check("W18 the chest and the Autarkia crate stay with their items", () -> t.level.getBlockEntity(chest(r)) instanceof Container ct
            && ct.getItem(0).is(Items.COBBLESTONE) && ct.getItem(0).getCount() == 32 && TestM1.crateCount(t, crate(r), Items.IRON_INGOT) == 16,
            () -> "crate " + TestM1.crateCount(t, crate(r), Items.IRON_INGOT));
        t.check("W18 the kept chest (and the crate) got the bubble payload", () -> NaniteClaims.stats(claim(r).id).bubbled.containsAll(List.of(chest(r),
            crate(r))), () -> NaniteClaims.stats(claim(r).id).bubbled.toString());
        t.check("W18 ore streams flew to the buffer", () -> NaniteClaims.stats(claim(r).id).streams >= 1,
            () -> NaniteClaims.stats(claim(r).id).streams + " streams");
        t.check("W18 the 5 iron ores arrive as raw iron items in the buffer (used MU + 5 x raw_iron)", () -> {
            MatterBufferBlockEntity b = bufferBe(r);
            return b != null && b.used() - BUFFER_START[0] == 5L * MatterValues.of(new ItemStack(Items.RAW_IRON), TestM8.colony().tier);
        }, () -> bufferBe(r) == null ? "no buffer" : "used +" + (bufferBe(r).used() - BUFFER_START[0]) + ", raw_iron "
            + MatterValues.of(new ItemStack(Items.RAW_IRON), TestM8.colony().tier));
        t.check("W18 stone becomes free MU (free MU + at least stone cells x cobblestone)", () -> {
            MatterBufferBlockEntity b = bufferBe(r);
            long cob = MatterValues.of(new ItemStack(Items.COBBLESTONE), TestM8.colony().tier);
            return b != null && cob > 0 && b.free() - BUFFER_START[1] >= COUNTS[0] * cob;
        }, () -> bufferBe(r) == null ? "no buffer" : "free +" + (bufferBe(r).free() - BUFFER_START[1]) + ", stone cells " + COUNTS[0]);
        t.check("W18 advancements colony/clean_pit and workers/claim_done on the (unconnected) player who started the claim",
            () -> done(t, "colony/clean_pit") && done(t, "workers/claim_done"), () -> "clean_pit " + done(t, "colony/clean_pit") + ", claim_done "
                + done(t, "workers/claim_done"));
    }
}

package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.logistics.CableCarStationBlockEntity;
import dev.autarkia.logistics.CableGeometry;
import dev.autarkia.logistics.CableLine;
import dev.autarkia.logistics.CatchNetBlock;
import dev.autarkia.logistics.CrateLauncherBlockEntity;
import dev.autarkia.logistics.GondolaEntity;
import dev.autarkia.logistics.StrayCrates;
import dev.autarkia.network.StockViewPayload;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.storage.DepotBlockEntity;
import dev.autarkia.storage.DomainStockData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.frozenblock.lib.wind.WindManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * The logistics part of {@code /autarkia test m7}: W16 (a cable car line of 100 blocks along z across a dug ravine with
 * 2 gondolas: 6 blocks/s, swing near 18 degrees in wind 0.5, slow above 30 and WIND_HOLD above 45 degrees in wind 1.2,
 * resume after the override is disabled, a strike on a block in the swing path, advancement sky_freight), W17 (a crate
 * launcher throws 27 stacks over a hill into a catch net 60 blocks away, then misses into a stray crate when the net has
 * no output, advancement special_delivery) on the platform region x 0 to 130, z 50 to 170, and L14 (two depots of an
 * outpost site, placed 2,000 blocks away so their chunk can unload, turn stale 1,200 ticks after their last update).
 */
final class TestM7Logistics {
    private TestM7Logistics() {
    }

    /** Add the W16, W17 and L14 steps to the M7 script. */
    static void run(TestM7.Run r) {
        cableCar(r);
        launcher(r);
        sites(r);
    }

    private static void wind(SelfTest t, @Nullable Vec3 w) {
        WindManager wm = WindManager.getOrCreate(t.level);
        wm.windOverride = Optional.ofNullable(w);
        wm.trySync(t.level);
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

    private static @Nullable CableCarStationBlockEntity station(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof CableCarStationBlockEntity s ? s : null;
    }

    private static @Nullable CableCarStationBlockEntity owner(SelfTest t, BlockPos a, BlockPos b) {
        CableCarStationBlockEntity s = station(t, a);
        CableCarStationBlockEntity o = station(t, b);
        return s != null && s.isOwner() ? s : o != null && o.isOwner() ? o : null;
    }

    private static @Nullable CableLine line(SelfTest t, BlockPos a, BlockPos b) {
        CableCarStationBlockEntity o = owner(t, a, b);
        return o == null ? null : o.line();
    }

    private static void feed(SelfTest t, BlockPos a, BlockPos b) {
        for (BlockPos p : List.of(a, b)) {
            CableCarStationBlockEntity s = station(t, p);
            if (s != null) {
                s.energy().fill(CableCarStationBlockEntity.CAPACITY);
                for (int i = CableCarStationBlockEntity.SLOTS; i < 2 * CableCarStationBlockEntity.SLOTS; i++) {
                    s.inventory().put(i, ItemStack.EMPTY);
                }
            }
        }
        CableCarStationBlockEntity s = station(t, a);
        for (int i = 0; s != null && i < CableCarStationBlockEntity.SLOTS; i++) {
            if (s.inventory().get(i).isEmpty()) {
                s.inventory().put(i, new ItemStack(Items.RAW_IRON, 64));
            }
        }
    }

    private static boolean jadeSays(CableCarStationBlockEntity s, String key) {
        List<Component> out = new ArrayList<>();
        s.jadeLines(out);
        for (Component c : out) {
            if (c.getContents() instanceof TranslatableContents tc) {
                for (Object arg : tc.getArgs()) {
                    if (arg instanceof Component inner && inner.getContents() instanceof TranslatableContents it && it.getKey().equals(key)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String describe(SelfTest t, BlockPos a, BlockPos b) {
        CableLine l = line(t, a, b);
        return l == null ? "no line" : l.state() + ", swing " + String.format(java.util.Locale.ROOT, "%.1f", l.maxSwing()) + ", gondolas " + l.count()
            + ", moved " + l.movedLastTick() + (l.stopped() ? ", at a stop" : "");
    }

    /** Add a wait until the loop leaves a stop (it was seen standing, now it moves at full speed). */
    private static void departure(SelfTest t, BlockPos a, BlockPos b, String check) {
        boolean[] stood = new boolean[1];
        t.then(x -> stood[0] = false);
        t.waitUntil(check, 1200, () -> {
            CableLine l = line(t, a, b);
            return stood[0] && l != null && !l.stopped() && l.movedLastTick() == CableLine.SPEED_PU;
        }, x -> {
            feed(x, a, b);
            CableLine l = line(x, a, b);
            stood[0] |= l != null && l.stopped();
        }, () -> describe(t, a, b));
        t.repeat(10, x -> feed(x, a, b));
    }

    private static void settle(SelfTest t, BlockPos a, BlockPos b, String check) {
        int[] calm = new int[1];
        t.then(x -> calm[0] = 0);
        t.waitUntil(check, 1500, () -> calm[0] >= 60, x -> {
            feed(x, a, b);
            CableLine l = line(x, a, b);
            calm[0] = l != null && l.count() > 0 && Math.abs(l.maxSwing() - 18.2) <= 2 ? calm[0] + 1 : 0;
        }, () -> describe(t, a, b));
    }

    private static void cableCar(TestM7.Run r) {
        SelfTest t = r.t();
        BlockPos a = r.at(10, 0, 56);
        BlockPos b = r.at(10, 0, 156);
        double[] z0 = new double[1];
        int[] moved = new int[1];
        boolean[] slow = new boolean[1];
        BlockPos[] block = new BlockPos[1];
        t.then(x -> {
            revoke(x, "logistics/sky_freight");
            TestM7.box(r, 3, -12, 58, 19, -1, 155, Blocks.AIR.defaultBlockState());
            boolean ok = TestM1.placeBig(x, a, AutarkiaBlocks.CABLE_CAR_STATION.get(), Direction.NORTH);
            ok &= TestM1.placeBig(x, b, AutarkiaBlocks.CABLE_CAR_STATION.get(), Direction.NORTH);
            CableCarStationBlockEntity.Link link = CableCarStationBlockEntity.link(x.level, a, b, false);
            CableCarStationBlockEntity own = owner(x, a, b);
            if (own != null && own.line() != null) {
                own.parts().set(0, ItemResource.of(AutarkiaItems.STEEL_PLATE.get()), 8);
                own.parts().set(1, ItemResource.of(Items.IRON_CHAIN), 4);
                own.line().setTarget(2);
                station(x, a).setLinker(TestM6Logistics.courier(x));
                station(x, b).setLinker(TestM6Logistics.courier(x));
            }
            for (int i = 0; i < CableCarStationBlockEntity.SLOTS && station(x, a) != null; i++) {
                station(x, a).inventory().put(i, new ItemStack(Items.RAW_IRON, 64));
            }
            feed(x, a, b);
            wind(x, new Vec3(0.5, 0, 0));
            x.report("W16 rig: two cable car stations 100 blocks apart along z across a ravine, linked, input full of raw iron", ok
                && link.result() == CableCarStationBlockEntity.Result.OK && Math.abs(link.length() - 100) < 0.01, link.toString());
        });
        t.waitUntil("W16 the owner spawns 2 gondolas from the part slots (4 steel plates and 2 iron chains each)", 100, () -> {
            CableCarStationBlockEntity o = owner(t, a, b);
            return o != null && o.line() != null && o.line().count() == 2 && o.parts().getAmountAsInt(0) == 0 && o.parts().getAmountAsInt(1) == 0;
        }, x -> feed(x, a, b), () -> describe(t, a, b));
        settle(t, a, b, "W16 wind override 0.5 0 0 (across the line): the swing settles near 18 degrees (18.2 +- 2 for 60 ticks)");
        t.check("W16 the swing the clients get (swingAlong / swingAcross, synced every 2 ticks) matches the server within 2 degrees", () -> {
            CableLine l = line(t, a, b);
            return l != null && l.gondolas(t.level).stream().allMatch(g -> Math.abs(g.syncedSwingDegrees() - g.swingDegrees()) <= 2);
        }, () -> describe(t, a, b));
        departure(t, a, b, "W16 the loaded loop leaves its stops");
        Vec3[] last = new Vec3[1];
        t.then(x -> {
            moved[0] = -line(x, a, b).movedLastTick();
            z0[0] = 0;
            last[0] = line(x, a, b).gondolas(x.level).get(0).position();
        });
        t.repeat(21, x -> {
            feed(x, a, b);
            moved[0] += line(x, a, b).movedLastTick();
            Vec3 now = line(x, a, b).gondolas(x.level).get(0).position();
            z0[0] += now.distanceTo(last[0]);
            last[0] = now;
        });
        t.check("W16 gondolas move at 6 blocks/s along the cable: 300 pu per tick, 6.0 blocks of path in 20 ticks", () -> moved[0] == 6000
            && Math.abs(z0[0] - 6.0) < 0.05, () -> moved[0] + " pu, " + String.format(java.util.Locale.ROOT, "%.3f", z0[0]) + " blocks of path");
        t.waitUntil("W16 advancement logistics/sky_freight: a loaded gondola crosses the 100-block line swinging over 15 degrees", 2000,
            () -> done(t, "logistics/sky_freight"), x -> feed(x, a, b), () -> describe(t, a, b));
        departure(t, a, b, "W16 the loop leaves its stops again");
        t.then(x -> {
            slow[0] = false;
            wind(x, new Vec3(1.2, 0, 0));
        });
        t.waitUntil("W16 wind override 1.2 0 0: above 30 degrees the line runs at 50% (150 pu per tick)", 300, () -> slow[0], x -> {
            feed(x, a, b);
            CableLine l = line(x, a, b);
            slow[0] |= l.state() == CableLine.State.SLOWED && l.movedLastTick() == CableLine.SPEED_PU / 2 && l.maxSwing() > CableLine.SLOW_DEGREES;
        }, () -> describe(t, a, b));
        t.waitUntil("W16 wind 1.2: above 45 degrees the line stops, Jade says Wind hold, transport.gondola_wind_hold_lines counts it", 400, () -> {
            CableLine l = line(t, a, b);
            return l.state() == CableLine.State.WIND_HOLD && l.maxSwing() > 45 && jadeSays(owner(t, a, b), CableLine.State.WIND_HOLD.key())
                && CableLine.windHoldLines(t.level) >= 1;
        }, x -> feed(x, a, b), () -> describe(t, a, b));
        t.then(x -> wind(x, null));
        t.waitUntil("W16 wind override disabled: below 20 degrees for 100 ticks the line runs again", 1500, () -> {
            CableLine l = line(t, a, b);
            return l.state() == CableLine.State.RUNNING || l.state() == CableLine.State.SLOWED;
        }, x -> feed(x, a, b), () -> describe(t, a, b));
        t.then(x -> wind(x, new Vec3(0.5, 0, 0)));
        settle(t, a, b, "W16 strike rig: with wind 0.5 the swing settles near 18 degrees again");
        departure(t, a, b, "W16 strike rig: the loop leaves its stops");
        t.then(x -> {
            CableLine l = line(x, a, b);
            GondolaEntity g = l.gondolas(x.level).get(0);
            CableGeometry geo = l.geometry();
            AABB box = geo.cabin(geo.hang(l.position(0) + 4000), g.thetaAlong(), g.thetaAcross());
            Vec3 p = box.getCenter().add(geo.right.scale(Math.signum(g.thetaAcross()) * 0.7));
            block[0] = BlockPos.containing(p);
            x.level.setBlock(block[0], Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        });
        t.waitUntil("W16 a block in the swing path: the gondola strikes it, the line stops OBSTRUCTED at that block", 200, () -> {
            CableLine l = line(t, a, b);
            return l.state() == CableLine.State.OBSTRUCTED && block[0].equals(l.obstruction()) && CableLine.obstructed(t.level).contains(block[0]);
        }, x -> feed(x, a, b), () -> describe(t, a, b) + ", block " + block[0].toShortString());
        t.then(x -> x.level.setBlock(block[0], Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
        t.waitUntil("W16 the block is gone: the line resumes", 100, () -> line(t, a, b).state() != CableLine.State.OBSTRUCTED, x -> feed(x, a, b),
            () -> describe(t, a, b));
        t.then(x -> wind(x, null));
    }

    private static @Nullable CrateLauncherBlockEntity launcherAt(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof CrateLauncherBlockEntity l ? l : null;
    }

    private static void fill(SelfTest t, BlockPos launcher) {
        CrateLauncherBlockEntity l = launcherAt(t, launcher);
        for (int i = 0; i < CrateLauncherBlockEntity.SLOTS && l != null; i++) {
            l.inventory().put(i, new ItemStack(Items.COBBLESTONE, 32));
        }
    }

    private static @Nullable BlockPos strayNear(SelfTest t, BlockPos net, BlockPos except) {
        for (BlockPos p : BlockPos.betweenClosed(net.offset(-6, -2, -6), net.offset(6, 3, 6))) {
            if (!p.equals(except) && TestM1.crateCount(t, p, Items.COBBLESTONE) > 0) {
                return p.immutable();
            }
        }
        return null;
    }

    private static void launcher(TestM7.Run r) {
        SelfTest t = r.t();
        BlockPos launcher = r.at(40, 0, 100);
        BlockPos net = r.at(100, 0, 100);
        BlockPos below = r.at(100, -1, 100);
        long[] fired = {-1, -1};
        boolean[] bounced = new boolean[1];
        t.then(x -> {
            revoke(x, "logistics/special_delivery");
            TestM7.box(r, 67, 0, 97, 73, 18, 103, Blocks.STONE.defaultBlockState());
            boolean ok = TestM1.placeBig(x, launcher, AutarkiaBlocks.CRATE_LAUNCHER.get(), Direction.NORTH);
            ok &= TestM1.placeBig(x, net, AutarkiaBlocks.CATCH_NET.get(), Direction.NORTH);
            TestM1.place(x, below, AutarkiaBlocks.CRATE.get().defaultBlockState());
            CrateLauncherBlockEntity l = launcherAt(x, launcher);
            String refusal = l == null ? "no launcher" : l.setTarget(net, TestM6Logistics.courier(x));
            if (l != null) {
                l.energy().fill(CrateLauncherBlockEntity.CAPACITY);
                l.inventory().put(CrateLauncherBlockEntity.CRATE_SLOT, new ItemStack(AutarkiaItems.CRATE.get(), 2));
            }
            fill(x, launcher);
            x.report("W17 rig: launcher and catch net 60 blocks apart, a hill 19 blocks high between, a crate below the net, 27 stacks and empty crates",
                ok && refusal == null && Math.abs(l.distance() - 60) < 0.01, "target " + refusal + ", distance " + (l == null ? -1 : l.distance()));
        });
        t.waitUntil("W17 the arm throws the crate", 100, () -> fired[0] >= 0, x -> {
            CrateLauncherBlockEntity l = launcherAt(x, launcher);
            l.energy().fill(CrateLauncherBlockEntity.CAPACITY);
            if (fired[0] < 0 && l.shots() > 0) {
                fired[0] = x.level.getGameTime();
            }
        }, () -> launcherAt(t, launcher).status().toString());
        t.check("W17 the crate flies on a high arc over the hill: angle 55 to 75 degrees (45 is blocked)", () -> {
            int angle = launcherAt(t, launcher).angle();
            return angle >= 55 && angle <= 75;
        }, () -> launcherAt(t, launcher).angle() + " degrees");
        t.waitUntil("W17 the net catches the crate", 200, () -> launcherAt(t, launcher).lastResult() == 1, x -> {
            launcherAt(x, launcher).energy().fill(CrateLauncherBlockEntity.CAPACITY);
            bounced[0] |= x.level.getBlockState(net).getValue(CatchNetBlock.BOUNCE);
            if (fired[1] < 0 && launcherAt(x, launcher).lastResult() == 1) {
                fired[1] = x.level.getGameTime();
            }
        }, () -> "result " + launcherAt(t, launcher).lastResult());
        t.check("W17 the crate flies about 3 s (50 to 80 ticks)", () -> fired[1] - fired[0] >= 50 && fired[1] - fired[0] <= 80,
            () -> (fired[1] - fired[0]) + " ticks");
        t.check("W17 all 27 stacks (864 cobblestone) plus the empty crate are in the crate below the net", () -> TestM1.crateCount(t, below,
            Items.COBBLESTONE) == 864 && TestM1.crateCount(t, below, AutarkiaItems.CRATE.get()) == 1,
            () -> TestM1.crateCount(t, below, Items.COBBLESTONE) + " cobblestone, " + TestM1.crateCount(t, below, AutarkiaItems.CRATE.get()) + " crates");
        t.check("W17 the net bounces (block property bounce)", () -> bounced[0], () -> "bounce " + bounced[0]);
        t.check("W17 advancement logistics/special_delivery", () -> done(t, "logistics/special_delivery"), () -> "special_delivery");
        t.then(x -> {
            x.level.setBlock(below, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            fill(x, launcher);
        });
        t.waitUntil("W17 without a crate below the net refuses: the crate bounces off and the shot is a MISS", 300, () -> launcherAt(t, launcher)
            .lastResult() == 2, x -> launcherAt(x, launcher).energy().fill(CrateLauncherBlockEntity.CAPACITY),
            () -> launcherAt(t, launcher).status() + ", result " + launcherAt(t, launcher).lastResult());
        t.check("W17 the crate landed as a crate block beside the net with all 864 cobblestone and transport.stray_crates lists it", () -> {
            BlockPos p = strayNear(t, net, below);
            boolean outside = p != null && (Math.abs(p.getX() - net.getX()) > 1 || Math.abs(p.getZ() - net.getZ()) > 1 || p.getY() != net.getY());
            return p != null && outside && TestM1.crateCount(t, p, Items.COBBLESTONE) == 864 && StrayCrates.list(t.level).contains(p);
        }, () -> "stray crate " + strayNear(t, net, below));
        t.check("W17 the launcher shows MISS (net output full), 2 shots, 1 miss, not paused", () -> {
            CrateLauncherBlockEntity l = launcherAt(t, launcher);
            return l.lastRefused() && l.lastShot().getContents() instanceof TranslatableContents tc && tc.getKey().equals("screen.autarkia.launcher.miss_full")
                && l.shots() == 2 && l.misses() == 1 && !l.paused() && CrateLauncherBlockEntity.levelMisses(t.level) >= 1;
        }, () -> "shots " + launcherAt(t, launcher).shots() + ", misses " + launcherAt(t, launcher).misses());
        t.then(x -> TestM7.box(r, 67, 0, 97, 73, 18, 103, Blocks.AIR.defaultBlockState()));
    }

    private static @Nullable DepotBlockEntity depot(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof DepotBlockEntity d ? d : null;
    }

    private static DomainStockData.@Nullable SiteView site(SelfTest t, BlockPos p) {
        String key = DomainStockData.key(t.level.dimension(), p);
        for (DomainStockData.SiteView s : DomainStockData.sites(t.level.getServer())) {
            if (s.key().equals(key)) {
                return s;
            }
        }
        return null;
    }

    private static void sites(TestM7.Run r) {
        SelfTest t = r.t();
        BlockPos far = r.at(2080, 0, 2144);
        ChunkPos cp = ChunkPos.containing(far);
        BlockPos d1 = new BlockPos(cp.getMinBlockX() + 2, far.getY(), cp.getMinBlockZ() + 2);
        BlockPos d2 = d1.offset(4, 0, 0);
        BlockPos core = d1.offset(2, 0, 7);
        String chunk = d1.getX() + " " + d1.getZ();
        long[] ages = new long[2];
        t.then(x -> x.command("forceload add " + chunk));
        t.waitUntil("L14 rig: the far outpost chunk loads", 2400, () -> t.level.isLoaded(d1), x -> {
        });
        t.then(x -> {
            DomainStockData.get(x.level).addOutpost(core, "Test Outpost");
            TestM1.place(x, d1, AutarkiaBlocks.DEPOT.get().defaultBlockState());
            TestM1.place(x, d2, AutarkiaBlocks.DEPOT.get().defaultBlockState());
            depot(x, d1).items().set(0, ItemResource.of(Items.RAW_IRON), 64);
            depot(x, d2).items().set(0, ItemResource.of(Items.COPPER_INGOT), 32);
            depot(x, d1).push(x.level);
            depot(x, d2).push(x.level);
        });
        t.check("L14 the Sites view lists both depots of the outpost as sites with their stock", () -> {
            DomainStockData.SiteView s1 = site(t, d1);
            DomainStockData.SiteView s2 = site(t, d2);
            StockViewPayload.Sites view = DomainStockData.view(t.level.getServer(), 0);
            boolean listed = view.sites().stream().anyMatch(s -> s.key().equals(DomainStockData.key(t.level.dimension(), d1)))
                && view.sites().stream().anyMatch(s -> s.key().equals(DomainStockData.key(t.level.dimension(), d2)));
            return s1 != null && s2 != null && !s1.main() && !s2.main() && !s1.stale() && !s2.stale() && listed
                && s1.stock().getOrDefault(ItemResource.of(Items.RAW_IRON), 0L) == 64 && s2.stock().getOrDefault(ItemResource.of(Items.COPPER_INGOT), 0L) == 32;
        }, () -> "sites " + DomainStockData.sites(t.level.getServer()).stream().map(DomainStockData.SiteView::key).toList());
        t.then(x -> x.command("forceload remove " + chunk));
        t.waitUntil("L14 the outpost chunk unloads (no ticket)", 1200, () -> !t.level.isLoaded(d1), x -> {
        });
        t.check("L14 right after the unload both sites are still current (last update younger than 1,200 ticks)", () -> {
            DomainStockData.SiteView s1 = site(t, d1);
            DomainStockData.SiteView s2 = site(t, d2);
            return s1 != null && s2 != null && !s1.stale() && !s2.stale();
        }, () -> "age " + (site(t, d1) == null ? -1 : site(t, d1).age()));
        t.waitUntil("L14 the unloaded sites turn grey (stale)", 1600, () -> {
            DomainStockData.SiteView s1 = site(t, d1);
            DomainStockData.SiteView s2 = site(t, d2);
            if (s1 == null || s2 == null) {
                return false;
            }
            ages[0] = s1.age();
            ages[1] = s2.age();
            return s1.stale() && s2.stale();
        }, x -> {
        }, () -> "ages " + ages[0] + ", " + ages[1]);
        t.check("L14 they turned stale 1,200 ticks after their last update and site.stale lists both", () -> ages[0] > DomainStockData.STALE_TICKS
            && ages[0] <= DomainStockData.STALE_TICKS + 2 && DomainStockData.staleSites(t.level.getServer()).containsAll(List.of(
                DomainStockData.key(t.level.dimension(), d1), DomainStockData.key(t.level.dimension(), d2))), () -> "ages " + ages[0] + ", " + ages[1]);
        t.then(x -> x.command("forceload add " + chunk));
        t.waitUntil("L14 cleanup: the outpost chunk loads again", 2400, () -> t.level.isLoaded(d1), x -> {
        });
        t.then(x -> {
            for (BlockPos p : List.of(d1, d2)) {
                DepotBlockEntity d = depot(x, p);
                for (int i = 0; d != null && i < DepotBlockEntity.SLOTS; i++) {
                    d.items().set(i, ItemResource.EMPTY, 0);
                }
                x.level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            DomainStockData.get(x.level).removeOutpost(core);
            x.command("forceload remove " + chunk);
            x.report("L14 cleanup: both depot sites are gone after the depots were removed", site(x, d1) == null && site(x, d2) == null, "removed");
        });
    }
}

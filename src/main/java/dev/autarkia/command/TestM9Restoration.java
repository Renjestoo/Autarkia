package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.Restoration;
import dev.autarkia.colony.RestorationProjectorBlockEntity;
import dev.autarkia.colony.Snapshot;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.logistics.FilterBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.storage.MatterBank;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The M9 restoration section (05b W25, W25 accept) in x 110..190, z 110..190 of the M9 platform: a 15 x 7 x 3 white wool
 * wall (wool drops itself and is soft enough for a crater of more than 64 cells) with a glass row (no drops, so matter
 * units pay), a chest with diamonds and a configured belt splitter, a {@code restoration_projector} 10 blocks in front
 * with an edited 21 x 12 x 11 box, a full FE buffer, the automatic snapshot on first power and 100,000 free matter units.
 * Three TNT inside the wall blow it with {@code absorb.enabled} set to false for the run (the power section keeps a
 * singularity core ONLINE in this domain); the old value comes back afterwards.
 */
final class TestM9Restoration {
    private static final int WALL_X0 = 140;
    private static final int WALL_X1 = 154;
    private static final int WALL_Z0 = 150;
    private static final int WALL_Z1 = 152;
    private static final List<Item> DEBRIS = List.of(Blocks.WOOL.white().asItem(), Items.CHEST, AutarkiaItems.BELT_SPLITTER.get());

    private TestM9Restoration() {
    }

    private static @Nullable RestorationProjectorBlockEntity projector(TestM9.Run r) {
        return r.t().level.getBlockEntity(r.at(147, 0, 140)) instanceof RestorationProjectorBlockEntity p ? p : null;
    }

    private static int debris(SelfTest t, BlockPos center) {
        int n = 0;
        for (Item item : DEBRIS) {
            n += TestM1.countItems(t, center, 32, item);
        }
        return n;
    }

    private static List<Integer> lost(SelfTest t, Snapshot s) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < s.volume(); i++) {
            BlockState want = s.state(i);
            BlockState have = t.level.getBlockState(s.pos(i));
            if (want != null && !want.isAir() && want.getBlock().asItem() != Items.AIR && !Restoration.secondary(want) && !have.equals(want)
                && (have.isAir() || have.canBeReplaced() || !have.getFluidState().isEmpty() || have.is(BlockTags.FIRE))) {
                out.add(i);
            }
        }
        return out;
    }

    private static boolean done(SelfTest t, String id) {
        var adv = t.level.getServer().getAdvancements().get(Autarkia.id(id));
        return adv != null && t.player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    /** Run W25 and W25 accept. */
    static void run(TestM9.Run r) {
        SelfTest t = r.t();
        BlockPos wallCenter = r.at(147, 3, 151);
        boolean[] absorb = new boolean[1];
        long[] before = new long[4];
        List<Integer> damaged = new ArrayList<>();
        String[] setup = {""};
        t.then(x -> {
            absorb[0] = AutarkiaConfig.ABSORB_ENABLED.get();
            AutarkiaConfig.ABSORB_ENABLED.set(false);
            TestM9.box(r, 130, 0, 135, 165, 14, 160, Blocks.AIR.defaultBlockState());
            TestM9.box(r, WALL_X0, 0, WALL_Z0, WALL_X1, 6, WALL_Z1, Blocks.WOOL.white().defaultBlockState());
            TestM9.box(r, WALL_X0, 3, WALL_Z0, WALL_X1, 3, WALL_Z1, Blocks.GLASS.defaultBlockState());
            TestM1.place(x, r.at(147, 1, 151), Blocks.CHEST.defaultBlockState());
            TestM1.place(x, r.at(145, 1, 151), AutarkiaBlocks.BELT_SPLITTER.get().defaultBlockState());
            boolean placed = TestM1.placeBig(x, r.at(147, 0, 140), AutarkiaBlocks.RESTORATION_PROJECTOR.get(), Direction.NORTH);
            setup[0] = "projector placed " + placed;
        });
        t.waitTicks(2);
        t.then(x -> {
            if (x.level.getBlockEntity(r.at(147, 1, 151)) instanceof ChestBlockEntity chest) {
                chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
            }
            if (x.level.getBlockEntity(r.at(145, 1, 151)) instanceof FilterBlockEntity f) {
                f.setRightSide(true);
            }
            RestorationProjectorBlockEntity p = projector(r);
            if (p != null) {
                boolean box = p.setBox(21, 12, 11, -10, -1, 5);
                p.energy().fill(p.energy().room());
                setup[0] += ", box set " + box;
            }
            TestM9.freeMatter(x, 100_000);
        });
        t.waitTicks(5);
        t.then(x -> {
            RestorationProjectorBlockEntity p = projector(r);
            Snapshot s = p == null || p.snapshotId() == null ? null : Snapshot.load(x.level.getServer(), p.snapshotId());
            setup[0] += ", snapshot " + (s == null ? "missing" : s.size.getX() + "x" + s.size.getY() + "x" + s.size.getZ());
            TestM1.killItems(x, wallCenter, 40);
            for (int dx : new int[] {-4, 0, 4}) {
                BlockPos at = r.at(147 + dx, 2, 151);
                x.level.setBlock(at, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                PrimedTnt tnt = new PrimedTnt(x.level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, null);
                tnt.setFuse(1);
                x.level.addFreshEntity(tnt);
            }
        });
        t.waitTicks(4);
        t.then(x -> {
            RestorationProjectorBlockEntity p = projector(r);
            Snapshot s = p == null || p.snapshotId() == null ? null : Snapshot.load(x.level.getServer(), p.snapshotId());
            if (p != null && s != null) {
                damaged.addAll(lost(x, s));
                long[] n = p.restoredCounts();
                before[0] = p.energy().stored();
                before[1] = MatterBank.available(x.level, TestM9.colony());
                before[2] = debris(x, wallCenter);
                before[3] = n[0];
            }
        });
        t.waitTicks(400);
        t.check("05b W25: TNT at the wall (absorb off): debris items fly back on arcs, the blocks return edge first at most 8 per tick, missing "
            + "reaches 0, containers come back empty, 512 FE per cell, colony/undo_crater", () -> {
            RestorationProjectorBlockEntity p = projector(r);
            Snapshot s = p == null || p.snapshotId() == null ? null : Snapshot.load(t.level.getServer(), p.snapshotId());
            if (p == null || s == null) {
                return false;
            }
            long[] n = p.restoredCounts();
            long restored = n[0] - before[3];
            long items = before[2] - debris(t, wallCenter);
            long fe = before[0] - p.energy().stored();
            long mu = before[1] - MatterBank.available(t.level, TestM9.colony());
            boolean edge = edgeFirst(s, p.restoreOrder());
            boolean chestEmpty = t.level.getBlockEntity(r.at(147, 1, 151)) instanceof ChestBlockEntity c && c.isEmpty();
            boolean splitter = t.level.getBlockEntity(r.at(145, 1, 151)) instanceof FilterBlockEntity f && f.data().get(1) == 1;
            return damaged.size() >= Restoration.EVENT_CELLS && lost(t, s).isEmpty() && p.missing() == 0 && restored == damaged.size()
                && p.restoreOrder().length == damaged.size() && edge && p.maxPerTick() >= 1 && p.maxPerTick() <= 8 && n[1] > 0 && items == n[1]
                && n[2] > 0 && mu > 0 && fe == restored * AutarkiaConfig.RESTORATION_FE_PER_BLOCK.get() && chestEmpty && splitter
                && (t.player == null || done(t, "colony/undo_crater"));
        }, () -> {
            RestorationProjectorBlockEntity p = projector(r);
            Snapshot s = p == null || p.snapshotId() == null ? null : Snapshot.load(t.level.getServer(), p.snapshotId());
            if (p == null || s == null) {
                return setup[0];
            }
            long[] n = p.restoredCounts();
            return setup[0] + "; damaged " + damaged.size() + ", still missing " + lost(t, s).size() + ", missing count " + p.missing() + ", restored "
                + (n[0] - before[3]) + " (debris " + n[1] + ", matter " + n[2] + ", no material " + n[3] + "), debris items used "
                + (before[2] - debris(t, wallCenter)) + ", order " + p.restoreOrder().length + " edge first " + edgeFirst(s, p.restoreOrder())
                + ", max per tick " + p.maxPerTick() + ", FE used " + (before[0] - p.energy().stored()) + ", MU used "
                + (before[1] - MatterBank.available(t.level, TestM9.colony())) + ", event cells " + p.eventCells() + ", chest empty "
                + (t.level.getBlockEntity(r.at(147, 1, 151)) instanceof ChestBlockEntity c && c.isEmpty()) + ", splitter right "
                + (t.level.getBlockEntity(r.at(145, 1, 151)) instanceof FilterBlockEntity f && f.data().get(1) == 1) + ", undo_crater "
                + (t.player == null ? "no player" : done(t, "colony/undo_crater"));
        });
        BlockPos broken = r.at(WALL_X0, 6, WALL_Z0);
        long[] accepted = new long[2];
        t.then(x -> {
            RestorationProjectorBlockEntity p = projector(r);
            accepted[0] = p == null ? -1 : p.restoredCounts()[0];
            TestM6Colony.owner(x).gameMode.destroyBlock(broken);
        });
        t.waitTicks(2);
        t.then(x -> {
            RestorationProjectorBlockEntity p = projector(r);
            Snapshot s = p == null || p.snapshotId() == null ? null : Snapshot.load(x.level.getServer(), p.snapshotId());
            BlockState cell = s == null || s.index(broken) < 0 ? null : s.state(s.index(broken));
            accepted[1] = cell != null && cell.isAir() ? 1 : 0;
        });
        t.waitTicks(220);
        t.check("05b W25 accept: the owner breaks a wall block: the snapshot cell becomes air and the next scans restore nothing", () -> {
            RestorationProjectorBlockEntity p = projector(r);
            return p != null && accepted[1] == 1 && t.level.getBlockState(broken).isAir() && p.restoredCounts()[0] == accepted[0] && p.missing() == 0;
        }, () -> {
            RestorationProjectorBlockEntity p = projector(r);
            return "snapshot cell air " + (accepted[1] == 1) + ", world " + t.level.getBlockState(broken) + ", restored before " + accepted[0] + " after "
                + (p == null ? -1 : p.restoredCounts()[0]) + ", missing " + (p == null ? -1 : p.missing());
        });
        t.then(x -> AutarkiaConfig.ABSORB_ENABLED.set(absorb[0]));
    }

    private static boolean edgeFirst(Snapshot s, int[] order) {
        if (order.length == 0) {
            return false;
        }
        double cx = 0;
        double cy = 0;
        double cz = 0;
        for (int i : order) {
            Vec3 v = Vec3.atCenterOf(s.pos(i));
            cx += v.x;
            cy += v.y;
            cz += v.z;
        }
        Vec3 c = new Vec3(cx / order.length, cy / order.length, cz / order.length);
        double last = Double.MAX_VALUE;
        for (int i : order) {
            double d = Vec3.atCenterOf(s.pos(i)).distanceToSqr(c);
            if (d > last + 1e-6) {
                return false;
            }
            last = d;
        }
        return true;
    }
}

package dev.autarkia.command;

import com.mojang.authlib.GameProfile;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyActions;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.Pattern;
import dev.autarkia.colony.PatternLoomBlockEntity;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.GridMember;
import dev.autarkia.energy.SnappedCableEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaEntities;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.worker.WorkerEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * The colony part of {@code /autarkia test m5}: a T2 colony owned by the test fake player (setup), then 06 #8 (a
 * Hauler Crab collects a snapped cable end and spilled items and deposits them in a crate), 06 #9 (it winds at the post
 * below its reserve and recovers from being walled in), and the Pattern capture at the loom with the library and
 * watched instance actions of the Patterns tab.
 */
final class TestM5Colony {
    private static final UUID[] ID = new UUID[1];

    private TestM5Colony() {
    }

    /** Return the M5 test colony, or null before setup. */
    static @Nullable Colony colony() {
        return ID[0] == null ? null : Colonies.registry().get(ID[0]);
    }

    private static FakePlayer owner(SelfTest t) {
        return FakePlayerFactory.get(t.level, new GameProfile(TestM3.OWNER, "AutarkiaTest"));
    }

    private static InteractionResult use(FakePlayer p, ItemStack stack, BlockPos clicked) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.snapTo(clicked.getX() + 0.5, clicked.getY() + 1, clicked.getZ() + 3.5, 180, 30);
        return stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(clicked).add(0, 0.5, 0), Direction.UP, clicked, false)));
    }

    static void setup(TestM5.Run r) {
        SelfTest t = r.t();
        BlockPos core = r.at(30, 0, 90);
        t.then(x -> {
            for (Colony c : List.copyOf(Colonies.registry().all())) {
                if (c.owner.equals(TestM3.OWNER) || c.dimension.equals(x.level.dimension()) && c.corePos.distManhattan(core) < 200) {
                    Colonies.dissolve(c);
                }
            }
            InteractionResult res = use(owner(x), new ItemStack(AutarkiaItems.CORE_SEED.get()), core.below());
            Colony c = Colonies.byCore(x.level, core);
            ID[0] = c == null ? null : c.id;
            if (c != null && x.player != null) {
                c.trusted.add(x.player.getUUID());
            }
            x.report("colony: the test seed grows a Core", res == InteractionResult.SUCCESS && c != null, res.toString());
        });
        t.waitUntil("colony: the Core blooms", 300, () -> colony() != null && colony().state == Colony.State.ACTIVE, x -> { });
        t.then(x -> use(owner(x), new ItemStack(AutarkiaItems.CORE_UPGRADE_T2.get()), core));
        t.waitUntil("colony: the Core upgrades to T2", 160, () -> colony() != null && colony().tier == 2, x -> x.level.getEntitiesOfClass(ItemEntity.class,
            new net.minecraft.world.phys.AABB(core).inflate(8)).forEach(e -> e.discard()));
    }

    static void run(TestM5.Run r) {
        WorkerEntity[] crab = new WorkerEntity[1];
        crab(r, crab);
        recharge(r, crab);
        stuck(r, crab);
        patterns(r);
    }

    private static BlockPos post(TestM5.Run r) {
        return r.at(40, 0, 80);
    }

    private static void powerPost(TestM5.Run r) {
        if (r.t().level.getBlockEntity(post(r)) instanceof GridMember m) {
            m.gridBuffer().fill(4000);
        }
    }

    private static void crab(TestM5.Run r, WorkerEntity[] crab) {
        SelfTest t = r.t();
        BlockPos crate = r.at(36, 0, 80);
        t.then(x -> {
            TestM1.place(x, post(r), AutarkiaBlocks.WINDING_POST.get().defaultBlockState());
            TestM1.place(x, crate, AutarkiaBlocks.CRATE.get().defaultBlockState());
            WorkerEntity w = AutarkiaEntities.HAULER_CRAB.get().create(x.level, EntitySpawnReason.COMMAND);
            BlockPos at = r.at(38, 0, 78);
            w.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            x.level.addFreshEntity(w);
            crab[0] = w;
        });
        t.waitUntil("06#8 the Hauler Crab joins the T2 colony (sub-cap 4)", 100, () -> colony() != null && colony().workers.containsKey(crab[0].getUUID()),
            x -> powerPost(r));
        int[] base = new int[2];
        t.then(x -> {
            base[0] = stored(t, AutarkiaItems.COPPER_WIRE_SPOOL.get());
            base[1] = stored(t, Items.IRON_INGOT);
            SnappedCableEntity end = SnappedCableEntity.create(x.level, Vec3.atCenterOf(r.at(24, 3, 78)), 0, CableTier.COPPER, 16);
            x.level.addFreshEntity(end);
            ItemEntity spill = new ItemEntity(x.level, r.at(20, 0, 82).getX() + 0.5, r.at(20, 0, 82).getY() + 0.2, r.at(20, 0, 82).getZ() + 0.5,
                new ItemStack(Items.IRON_INGOT, 12));
            spill.setUnlimitedLifetime();
            x.level.addFreshEntity(spill);
        });
        t.waitUntil("06#8 the board opens a COLLECT_DEBRIS job for the crab", 200, () -> ColonyRuntime.of(colony()).jobs.all().stream()
            .anyMatch(j -> j.type == ColonyJob.Type.COLLECT_DEBRIS), x -> powerPost(r));
        t.waitUntil("06#8 the crab collects the snapped cable and the spill and deposits them in colony storage", 2400,
            () -> stored(t, AutarkiaItems.COPPER_WIRE_SPOOL.get()) == base[0] + 1 && stored(t, Items.IRON_INGOT) == base[1] + 12,
            x -> powerPost(r));
        t.check("06#8 no debris is left, the colony counts the pickups and fires debris_collected", () -> dev.autarkia.worker.WorkerSensors.debris(t.level,
            colony()).isEmpty() && colony().stats.getOrDefault("debris_collected", 0L) >= 2, () -> colony().stats.toString());
    }

    private static void recharge(TestM5.Run r, WorkerEntity[] crab) {
        SelfTest t = r.t();
        t.waitUntil("06#9 the crab is idle", 600, () -> crab[0].isIdle(), x -> powerPost(r));
        t.then(x -> crab[0].setEnergy(crab[0].reserve() - 20));
        t.waitUntil("06#9 below the reserve the crab winds at the post", 600, () -> crab[0].state() == WorkerEntity.State.RECHARGING
            && crab[0].action() == WorkerEntity.ACTION_RECHARGE, x -> powerPost(r));
        t.waitUntil("06#9 the spring winds full (12,000 T)", 900, () -> crab[0].energy() >= 12000, x -> powerPost(r));
    }

    private static void stuck(TestM5.Run r, WorkerEntity[] crab) {
        SelfTest t = r.t();
        List<BlockPos> wall = new ArrayList<>();
        BlockPos[] at = new BlockPos[1];
        t.waitUntil("06#9 the crab is idle again", 600, () -> crab[0].isIdle(), x -> powerPost(r));
        t.then(x -> {
            ItemEntity far = new ItemEntity(x.level, r.at(10, 0, 70).getX() + 0.5, r.at(10, 0, 70).getY() + 0.2, r.at(10, 0, 70).getZ() + 0.5,
                new ItemStack(Items.GOLD_INGOT, 3));
            far.setUnlimitedLifetime();
            x.level.addFreshEntity(far);
            ColonyRuntime.of(colony()).log.clear();
            at[0] = crab[0].blockPosition();
        });
        t.waitUntil("06#9 the crab travels to the far debris", 600, () -> crab[0].state() == WorkerEntity.State.TRAVELLING
            && crab[0].blockPosition().distManhattan(at[0]) >= 4, x -> powerPost(r));
        t.then(x -> {
            BlockPos c = crab[0].blockPosition();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                        for (int dy = -1; dy <= 2; dy++) {
                            BlockPos b = c.offset(dx, dy, dz);
                            if (x.level.getBlockState(b).isAir()) {
                                x.level.setBlock(b, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
                                wall.add(b);
                            }
                        }
                    }
                }
            }
            at[0] = c;
            dev.autarkia.Autarkia.LOG.info("[TEST m5] wall of {} blocks around the crab at {} (y {}, below {})", wall.size(), c.toShortString(),
                crab[0].getY(), x.level.getBlockState(c.below()).getBlock());
        });
        t.waitUntil("06#9 walled in, the stuck levels run and the job fails NO_PATH", 900, () -> ColonyRuntime.of(colony()).log.stream()
            .anyMatch(l -> l.contains("failed: no_path")), x -> powerPost(r));
        t.then(x -> {
            dev.autarkia.Autarkia.LOG.info("[TEST m5] wall removed, crab at {} ({})", crab[0].blockPosition().toShortString(), crab[0].stateName());
            wall.forEach(b -> x.level.setBlock(b, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
        });
        t.waitUntil("06#9 with the wall gone the crab recovers and stores the far debris in colony storage", 3000,
            () -> stored(t, Items.GOLD_INGOT) == 3, x -> powerPost(r));
        t.check("06#9 the crab is back to work: cargo empty, no debris left", () -> crab[0].cargoEmpty() && dev.autarkia.worker.WorkerSensors.debris(t.level,
            colony()).isEmpty(), () -> crab[0].stateName() + ", cargo " + crab[0].cargo + ", debris " + dev.autarkia.worker.WorkerSensors.debris(t.level,
            colony()).size());
    }

    private static int stored(SelfTest t, net.minecraft.world.item.Item item) {
        int n = 0;
        for (BlockPos p : ColonyRuntime.of(colony()).storages) {
            if (t.level.getBlockEntity(p) instanceof dev.autarkia.colony.ColonyStorage s) {
                var h = s.colonyItems();
                for (int i = 0; i < h.size(); i++) {
                    if (h.getResource(i).is(item)) {
                        n += h.getAmountAsInt(i);
                    }
                }
            }
        }
        return n;
    }

    private static void patterns(TestM5.Run r) {
        SelfTest t = r.t();
        BlockPos loom = r.at(50, 0, 96);
        Pattern.Ref[] ref = new Pattern.Ref[1];
        t.then(x -> {
            TestM1.place(x, loom, AutarkiaBlocks.PATTERN_LOOM.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
            PatternLoomBlockEntity be = (PatternLoomBlockEntity) x.level.getBlockEntity(loom);
            for (int[] press : new int[][] {{6, 3}, {8, 5}, {3, 5}, {4, 6}, {5, 5}}) {
                for (int k = 0; k < press[1]; k++) {
                    be.button(press[0]);
                }
            }
            for (BlockPos p : BlockPos.betweenClosed(loom.offset(-1, 0, -4), loom.offset(1, 0, -2))) {
                x.level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            }
            x.level.setBlock(loom.offset(0, 1, -3), AutarkiaBlocks.BELT_SPLITTER.get().defaultBlockState()
                .setValue(MachineBlock.HORIZONTAL_FACING, Direction.WEST), Block.UPDATE_ALL);
            x.level.setBlock(loom.offset(1, 1, -2), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
            if (x.level.getBlockEntity(loom.offset(0, 1, -3)) instanceof dev.autarkia.logistics.FilterBlockEntity f) {
                f.button(0);
            }
            be.card().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(new ItemStack(AutarkiaItems.PATTERN_CARD.get())), 1);
            String err = be.record(owner(x), "Test Line");
            x.report("05 5.1 the loom box is 3 x 2 x 3 in front and Record starts", err == null && be.boxSize().equals(new net.minecraft.core.Vec3i(3, 2, 3))
                && be.boxMin().equals(loom.offset(-1, 0, -4)), err + ", box " + be.boxMin().subtract(loom).toShortString() + " size " + be.boxSize());
        });
        t.waitUntil("05 5.1 the scan finishes and the card gets autarkia:pattern_ref", 40, () -> {
            PatternLoomBlockEntity be = (PatternLoomBlockEntity) t.level.getBlockEntity(loom);
            ref[0] = be.cardStack().get(AutarkiaComponents.PATTERN_REF.get());
            return ref[0] != null;
        }, x -> { });
        t.check("05 5.2 the Pattern file holds 11 blocks, 1 foreign block entity (placed empty), the splitter settings and a 2-bit palette", () -> {
            Pattern p = Pattern.load(t.level.getServer(), ref[0].id());
            return p != null && ref[0].blocks() == 11 && p.blocks() == 11 && p.foreign().size() == 1 && p.data().size() == 1
                && p.data().values().iterator().next().getBooleanOr("filterMode", false) && p.bits() == 2 && p.name().equals("Test Line")
                && p.size().equals(new net.minecraft.core.Vec3i(3, 2, 3));
        }, () -> {
            Pattern p = Pattern.load(t.level.getServer(), ref[0].id());
            return p == null ? "no file" : p.blocks() + " blocks, " + p.foreign().size() + " foreign, " + p.data().size() + " data, " + p.bits() + " bits";
        });
        t.check("05 5.1 Protect and Library: a watched instance at the box and a library entry", () -> {
            Colony c = colony();
            return c.library.size() == 1 && c.library.get(0).pattern().equals(ref[0].id()) && c.instances.size() == 1 && c.instances.get(0).watched()
                && c.instances.get(0).origin().equals(loom.offset(-1, 0, -4)) && c.instances.get(0).contains(loom.offset(0, 1, -3));
        }, () -> colony().library + " / " + colony().instances.size() + " instances");
        t.then(x -> {
            FakePlayer o = owner(x);
            Colony c = colony();
            ColonyActions.handle(o, c.id, "pattern_remove", List.of(ref[0].id().toString()));
            boolean removed = c.library.isEmpty() && !c.instances.get(0).watched();
            ColonyActions.handle(o, c.id, "pattern_watch", List.of(c.instances.get(0).id().toString(), "on"));
            o.setItemInHand(InteractionHand.MAIN_HAND, ((PatternLoomBlockEntity) x.level.getBlockEntity(loom)).cardStack().copy());
            ColonyActions.handle(o, c.id, "pattern_add", List.of());
            ColonyActions.handle(o, c.id, "pattern_role", List.of(ref[0].id().toString(), "line"));
            x.report("05 5.4 Patterns tab: remove unwatches, Watch, the held card adds it back, Set role", removed && c.instances.get(0).watched()
                && c.library.size() == 1 && c.library.get(0).role().equals("line") && !o.getMainHandItem().isEmpty(), c.library.toString());
        });
    }
}

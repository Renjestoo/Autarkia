package dev.autarkia.command;

import dev.autarkia.colony.ColonyPlans;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.ColonyTiers;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.Project;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.storage.MatterBank;
import dev.autarkia.storage.MatterBufferBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * The T6 colony of {@code /autarkia test m9}: the test seed grows a Core at x 200, z 100 of the platform (domain x
 * 104..296, z 4..196 at T6), the upgrades raise it to tier 6, the test player is trusted, autonomy is Manual with every
 * template disabled, and the domain gets one {@code matter_buffer} at x 200, z 150 with 1,000,000 free matter units and
 * four crates at x 210..213, z 150.
 */
final class TestM9Colony {
    static final long START_MATTER = 1_000_000L;

    private TestM9Colony() {
    }

    static BlockPos core(TestM9.Run r) {
        return r.at(200, 0, 100);
    }

    static BlockPos buffer(TestM9.Run r) {
        return r.at(200, 0, 150);
    }

    private static InteractionResult use(FakePlayer p, ItemStack stack, BlockPos clicked) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.snapTo(clicked.getX() + 0.5, clicked.getY() + 1, clicked.getZ() + 3.5, 180, 30);
        return stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(clicked).add(0, 0.5, 0), Direction.UP, clicked, false)));
    }

    /** Build the T6 test colony: Core, tier upgrades, trust, Manual autonomy, matter buffer and crates. */
    static void setup(TestM9.Run r) {
        SelfTest t = r.t();
        BlockPos core = core(r);
        t.then(x -> {
            for (Colony c : List.copyOf(Colonies.registry().all())) {
                if (c.owner.equals(TestM6Colony.owner(x).getUUID()) || c.dimension.equals(x.level.dimension()) && Math.abs(c.corePos.getZ() - core.getZ()) <= 400
                    && Math.abs(c.corePos.getX() - core.getX()) <= 400) {
                    Colonies.dissolve(c);
                }
            }
            TestM9.colony(null);
        });
        t.then(x -> {
            InteractionResult res = use(TestM6Colony.owner(x), new ItemStack(AutarkiaItems.CORE_SEED.get()), core.below());
            Colony c = Colonies.byCore(x.level, core);
            TestM9.colony(c);
            if (c != null && x.player != null) {
                c.trusted.add(x.player.getUUID());
            }
            x.report("colony setup: the test seed grows a Core at x 200", res == InteractionResult.SUCCESS && c != null, res.toString());
        });
        t.waitUntil("colony setup: the Core blooms", 1200, () -> TestM9.colony() != null && TestM9.colony().state == Colony.State.ACTIVE, x -> { });
        for (Item up : List.of(AutarkiaItems.CORE_UPGRADE_T2.get(), AutarkiaItems.CORE_UPGRADE_T3.get(), AutarkiaItems.CORE_UPGRADE_T4.get(),
            AutarkiaItems.CORE_UPGRADE_T5.get(), AutarkiaItems.CORE_UPGRADE_T6.get())) {
            int[] before = new int[1];
            t.then(x -> {
                before[0] = TestM9.colony().tier;
                use(TestM6Colony.owner(x), new ItemStack(up), core);
            });
            t.waitUntil("colony setup: " + BuiltInRegistries.ITEM.getKey(up).getPath() + " raises the Core one tier", 160,
                () -> TestM9.colony().tier == before[0] + 1,
                x -> x.level.getEntitiesOfClass(ItemEntity.class, new AABB(core).inflate(8)).forEach(Entity::discard));
        }
        t.check("05 1.2: core_upgrade_t6 gives tier 6: domain half-size 96, worker cap 32, outposts of half-size 32",
            () -> TestM9.colony().tier == 6 && ColonyTiers.domainHalf(6) == 96 && ColonyTiers.workerCap(6) >= 32 && ColonyTiers.outpostHalf(6) == 32,
            () -> "tier " + TestM9.colony().tier + ", half " + ColonyTiers.domainHalf(TestM9.colony().tier) + ", cap "
                + ColonyTiers.workerCap(TestM9.colony().tier) + ", outpost half " + ColonyTiers.outpostHalf(TestM9.colony().tier));
        t.then(x -> {
            Colony c = TestM9.colony();
            c.autonomy = Colony.Autonomy.MANUAL;
            ColonyPlans.templates().keySet().forEach(k -> c.template(k).enabled = false);
            for (Project p : List.copyOf(c.openProjects())) {
                ColonyRuntime.of(c).jobs.dropProject(c, p);
                c.projects.remove(p);
            }
            boolean ok = TestM1.placeBig(x, buffer(r), AutarkiaBlocks.MATTER_BUFFER.get(), Direction.NORTH);
            for (int i = 0; i < 4; i++) {
                TestM1.place(x, r.at(210 + i, 0, 150), AutarkiaBlocks.CRATE.get().defaultBlockState());
            }
            x.report("colony setup: matter buffer and 4 crates in the domain", ok, "placed " + ok);
        });
        t.waitTicks(2);
        t.then(x -> {
            if (x.level.getBlockEntity(buffer(r)) instanceof MatterBufferBlockEntity mb) {
                mb.addFree(START_MATTER);
            }
            long free = MatterBank.available(x.level, TestM9.colony());
            x.report("colony setup: the domain holds 1,000,000 free matter units", free == START_MATTER, "free " + free);
        });
    }
}

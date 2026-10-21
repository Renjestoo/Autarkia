package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.processing.MachineStatus;
import dev.autarkia.processing.MatterCompilerBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The materials part of {@code /autarkia test m9} (region x 175 to 235, z 10 to 70, inside the T6 domain): 03 #22 (a
 * compiler at x 180, z 20 turns 1 raw iron into 288 MU and 8 iron ingots, 20 ticks each) and 03 #23 (a compiler at
 * x 200, z 20 credits 64 cobblestone as 64 MU and waits with NO_CONDITION for an aegis plate). The domain free MU is 0
 * for both, so no compiler pulls matter from the domain buffers; the section sets it back to 1,000,000 at the end.
 */
final class TestM9Materials {
    private TestM9Materials() {
    }

    /** Add the 03 #22 and #23 steps to the M9 script. */
    static void run(TestM9.Run r) {
        rig22(r);
        rig23(r);
        r.t().then(x -> TestM9.freeMatter(x, 1_000_000));
    }

    private static @Nullable MatterCompilerBlockEntity compiler(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof MatterCompilerBlockEntity c ? c : null;
    }

    private static boolean place(SelfTest t, BlockPos p) {
        for (BlockPos f : BlockPos.betweenClosed(p.below(), p.offset(1, -1, 1))) {
            TestM1.place(t, f.immutable(), AutarkiaBlocks.FOUNDATION.get().defaultBlockState());
        }
        return TestM1.placeBig(t, p, AutarkiaBlocks.MATTER_COMPILER.get(), Direction.NORTH);
    }

    private static int outputs(MatterCompilerBlockEntity c) {
        int n = 0;
        for (int i = 1; i <= 4; i++) {
            n += c.inventory().get(i).getCount();
        }
        return n;
    }

    private static void target(MatterCompilerBlockEntity c, String result) {
        c.setTarget(Autarkia.id("compiling/" + result));
    }

    private static void rig22(TestM9.Run r) {
        SelfTest t = r.t();
        BlockPos p = r.at(180, 0, 20);
        long[] start = new long[2];
        long[] maxMu = {-1};
        List<Long> done = new ArrayList<>();
        t.then(x -> {
            TestM9.freeMatter(x, 0);
            x.report("03#22 rig: a matter compiler stands at x 180, z 20 on 4 foundations", place(x, p), "placed");
        });
        t.waitTicks(2);
        t.then(x -> {
            MatterCompilerBlockEntity c = compiler(x, p);
            c.energy().fill(c.energy().capacity());
            start[0] = c.energyUsed();
            start[1] = x.level.getGameTime();
            target(c, "iron_ingot");
            TestM1.insertAt(x.level, p.above(), new ItemStack(Items.RAW_IRON));
        });
        t.repeat(175, x -> {
            MatterCompilerBlockEntity c = compiler(x, p);
            maxMu[0] = Math.max(maxMu[0], c.matter());
            while (done.size() < outputs(c)) {
                done.add(x.level.getGameTime() - start[1]);
            }
        });
        t.check("03#22 feed 1 raw_iron, target iron_ingot: the buffer shows 288 MU (36 x 8), 8 iron ingots 20 ticks apart (160 ticks, "
            + "8 x 20 x 16,384 = 2,621,440 FE), the buffer is back to 0", () -> {
                MatterCompilerBlockEntity c = compiler(t, p);
                boolean steps = done.size() == 8;
                for (int i = 1; steps && i < done.size(); i++) {
                    steps = done.get(i) - done.get(i - 1) == 20;
                }
                return maxMu[0] == 288 && steps && c.inventory().get(1).is(Items.IRON_INGOT) && outputs(c) == 8 && c.matter() == 0
                    && c.energyUsed() - start[0] == 8L * MatterCompilerBlockEntity.energyFor(36) && MatterCompilerBlockEntity.ticks(36) == 20;
            }, () -> {
                MatterCompilerBlockEntity c = compiler(t, p);
                return "max buffer " + maxMu[0] + " MU, results at ticks " + done + ", outputs " + outputs(c) + ", buffer now " + c.matter()
                    + " MU, FE used " + (c.energyUsed() - start[0]);
            });
    }

    private static void rig23(TestM9.Run r) {
        SelfTest t = r.t();
        BlockPos p = r.at(200, 0, 20);
        int[] fe = new int[1];
        t.then(x -> {
            TestM9.freeMatter(x, 0);
            x.report("03#23 rig: a matter compiler stands at x 200, z 20 on 4 foundations", place(x, p), "placed");
        });
        t.waitTicks(2);
        t.then(x -> {
            MatterCompilerBlockEntity c = compiler(x, p);
            c.energy().fill(c.energy().capacity());
            fe[0] = c.energy().stored();
            target(c, "aegis_plate");
            TestM1.insertAt(x.level, p.above(), new ItemStack(Items.COBBLESTONE, 64));
        });
        t.waitTicks(40);
        t.check("03#23 feed 64 cobblestone, target aegis_plate: 64 MU only, status NO_CONDITION (not enough MU), no output, no FE used", () -> {
            MatterCompilerBlockEntity c = compiler(t, p);
            return c.matter() == 64 && c.inventory().get(0).isEmpty() && c.machineStatus().state() == MachineStatus.State.NO_CONDITION && outputs(c) == 0
                && c.energy().stored() == fe[0];
        }, () -> {
            MatterCompilerBlockEntity c = compiler(t, p);
            return "buffer " + c.matter() + " MU, feed " + c.inventory().get(0) + ", status " + c.machineStatus().state() + ", outputs " + outputs(c)
                + ", FE " + fe[0] + " to " + c.energy().stored();
        });
    }
}

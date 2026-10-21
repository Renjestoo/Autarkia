package dev.autarkia.command;

import dev.autarkia.logistics.MatterConduitBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.MachineBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * The logistics part of {@code /autarkia test m9}: 04 L16. A SEND conduit at x 180, z 85 and a RECEIVE conduit at
 * x 190, z 85 (output east into a crate), both on channel 3 inside the T6 domain: 576 cobblestone arrive at 64 per tick
 * with no travel time, 8 FE per item. Then the receiver moves outside the domain to x 60, z 80: it shows "Outside
 * domain" and nothing moves. The rigs stay standing.
 */
final class TestM9Logistics {
    private static final int SENT = 576;

    private TestM9Logistics() {
    }

    /** Add the L16 steps to the M9 script. */
    static void run(TestM9.Run r) {
        SelfTest t = r.t();
        BlockPos send = r.at(180, 0, 85);
        BlockPos recv = r.at(190, 0, 85);
        BlockPos crate = recv.east();
        BlockPos far = r.at(60, 0, 80);
        BlockPos farCrate = far.east();
        List<int[]> samples = new ArrayList<>();
        int[] outside = new int[3];
        t.then(x -> {
            TestM1.place(x, crate, AutarkiaBlocks.CRATE.get().defaultBlockState());
            TestM1.place(x, send, conduit(Direction.DOWN));
            TestM1.place(x, recv, conduit(Direction.EAST));
        });
        t.waitTicks(2);
        t.then(x -> {
            MatterConduitBlockEntity s = at(x, send);
            MatterConduitBlockEntity v = at(x, recv);
            boolean ok = s != null && v != null;
            if (ok) {
                s.setMode(MatterConduitBlockEntity.Mode.SEND);
                s.setChannel(3);
                v.setMode(MatterConduitBlockEntity.Mode.RECEIVE);
                v.setChannel(3);
                s.energy().fill(s.energy().capacity());
                for (int i = 0; i < MatterConduitBlockEntity.SLOTS; i++) {
                    s.buffer().set(i, ItemResource.of(Items.COBBLESTONE), 64);
                }
            }
            x.report("L16 rig: SEND conduit at x 180, RECEIVE at x 190 into a crate, channel 3, 9 stacks of cobblestone, 102,400 FE", ok, "conduits " + ok);
        });
        t.repeat(11, x -> samples.add(new int[] {count(at(x, send)), TestM1.crateCount(x, crate, Items.COBBLESTONE), at(x, send).energy().stored()}));
        t.then(x -> {
            x.level.setBlock(recv, Blocks.AIR.defaultBlockState(), 3);
            TestM1.place(x, farCrate, AutarkiaBlocks.CRATE.get().defaultBlockState());
            TestM1.place(x, far, conduit(Direction.EAST));
        });
        t.waitTicks(2);
        t.then(x -> {
            MatterConduitBlockEntity v = at(x, far);
            v.setMode(MatterConduitBlockEntity.Mode.RECEIVE);
            v.setChannel(3);
            at(x, send).buffer().set(0, ItemResource.of(Items.COBBLESTONE), 64);
        });
        t.waitTicks(5);
        t.then(x -> {
            outside[0] = count(at(x, send));
            outside[1] = TestM1.crateCount(x, farCrate, Items.COBBLESTONE);
            outside[2] = at(x, far).outside() && jadeOutside(at(x, far)) ? 1 : 0;
        });
        t.check("L16 64 items per tick arrive in the crate with no travel time (576 in 9 ticks, sender + crate = 576 every tick, 512 FE per tick); "
            + "with the receiver moved outside the domain it shows \"Outside domain\" and nothing moves", () -> {
                boolean steps = samples.size() == 11 && samples.get(0)[0] == SENT && samples.get(0)[1] == 0;
                for (int i = 1; steps && i <= 9; i++) {
                    int[] s = samples.get(i);
                    steps = s[0] == SENT - 64 * i && s[1] == 64 * i && s[0] + s[1] == SENT && samples.get(i - 1)[2] - s[2] == 64 * 8;
                }
                return steps && samples.get(10)[1] == SENT && outside[0] == 64 && outside[1] == 0 && outside[2] == 1;
            }, () -> "sender / crate / FE per tick " + samples.stream().map(s -> s[0] + "/" + s[1] + "/" + s[2]).toList() + "; outside: sender "
                + outside[0] + ", far crate " + outside[1] + ", outside flag and Jade line " + (outside[2] == 1));
    }

    private static net.minecraft.world.level.block.state.BlockState conduit(Direction face) {
        return AutarkiaBlocks.MATTER_CONDUIT.get().defaultBlockState().setValue(MachineBlock.FACING, face);
    }

    private static @Nullable MatterConduitBlockEntity at(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof MatterConduitBlockEntity c ? c : null;
    }

    private static int count(MatterConduitBlockEntity c) {
        int n = 0;
        for (int i = 0; i < MatterConduitBlockEntity.SLOTS; i++) {
            n += c.buffer().getAmountAsInt(i);
        }
        return n;
    }

    private static boolean jadeOutside(MatterConduitBlockEntity c) {
        List<Component> out = new ArrayList<>();
        c.jadeLines(out);
        return out.stream().anyMatch(l -> l.getContents() instanceof TranslatableContents tc && tc.getKey().equals("jade.autarkia.matter_conduit.outside"));
    }
}

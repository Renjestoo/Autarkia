package dev.autarkia.command;

import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyClock;
import dev.autarkia.colony.Project;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.storage.FallingCrateEntity;
import dev.autarkia.storage.RackBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * W12 of {@code /autarkia test m6}: an aisle of 6 wood racks against a wall at 110%, the first one at 147%. It
 * collapses after 60 s, its crates are thrown against the next rack, whose impact load brings it down at once, and so
 * on down the aisle; every item survives in crate blocks; each collapse is in the failure memory, and the next colony
 * day the Autonomous colony rebuilds the 6 positions with steel racks (template rack_upgrade).
 */
final class TestM6Aisle {
    private TestM6Aisle() {
    }

    private static List<BlockPos> row(TestM6.Run r) {
        return List.of(r.at(60, 0, 134), r.at(61, 0, 134), r.at(62, 0, 134), r.at(63, 0, 134), r.at(64, 0, 134), r.at(65, 0, 134));
    }

    private static int total(SelfTest t, TestM6.Run r, Item item) {
        BlockPos c = r.at(62, 0, 136);
        return TestM6Logistics.countAround(t, c, 9, item) + TestM1.countItems(t, c, 10, item);
    }

    private static String around(SelfTest t, List<BlockPos> row) {
        StringBuilder sb = new StringBuilder();
        for (BlockPos p : row) {
            sb.append(p.getX()).append(':');
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    var st = t.level.getBlockState(p.offset(0, dy, dz));
                    sb.append(st.isAir() ? '.' : st.is(AutarkiaBlocks.CRATE.get()) ? 'c' : st.is(AutarkiaBlocks.STEEL_RACK.get()) ? 's' : '#');
                }
                sb.append('/');
            }
            sb.append(' ');
        }
        return sb.toString();
    }

    static void run(TestM6.Run r) {
        SelfTest t = r.t();
        List<BlockPos> row = row(r);
        int[] before = new int[2];
        t.then(x -> {
            TestM6.box(r, 59, 0, 133, 66, 1, 133, Blocks.STONE.defaultBlockState());
            for (BlockPos p : row) {
                TestM1.place(x, p, AutarkiaBlocks.WOOD_RACK.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.SOUTH));
            }
            RackBlockEntity first = (RackBlockEntity) x.level.getBlockEntity(row.get(0));
            first.setCrate(0, TestM6Logistics.crate(Items.IRON_INGOT, 1500));
            first.setCrate(1, TestM6Logistics.crate(Items.IRON_INGOT, 1500));
            for (int i = 1; i < row.size(); i++) {
                RackBlockEntity rack = (RackBlockEntity) x.level.getBlockEntity(row.get(i));
                rack.setCrate(0, TestM6Logistics.crate(Items.IRON_INGOT, 1500));
                rack.setCrate(1, TestM6Logistics.crate(Items.COBBLESTONE, 1472));
            }
            before[0] = total(x, r, Items.IRON_INGOT);
            before[1] = total(x, r, Items.COBBLESTONE);
            Colony c = TestM6Colony.colony();
            c.memories.removeIf(m -> m.type().equals("rack_collapse"));
            c.templates.forEach((k, v) -> v.enabled = k.equals("rack_upgrade"));
            x.report("W12 rig: 6 wood racks against a wall, the first at 147% (stage 3), the others at 110%", first.stage() == 3
                && row.subList(1, 6).stream().allMatch(p -> ((RackBlockEntity) x.level.getBlockEntity(p)).ratio() > 1.09
                    && ((RackBlockEntity) x.level.getBlockEntity(p)).ratio() < 1.11), String.format(java.util.Locale.ROOT, "%.2f / %.2f",
                first.ratio(), ((RackBlockEntity) x.level.getBlockEntity(row.get(1))).ratio()));
        });
        t.waitUntil("W12 the first rack collapses after 60 s and the crates bring down the whole aisle", 1500, () -> row.stream()
            .noneMatch(p -> t.level.getBlockEntity(p) instanceof RackBlockEntity), x -> TestM6Colony.powerDocks(x, r));
        t.waitUntil("W12 every falling crate lands", 300, () -> t.level.getEntitiesOfClass(FallingCrateEntity.class, new AABB(r.at(62, 0, 136)).inflate(16)).isEmpty(),
            x -> { });
        t.check("W12 every item survives in crate blocks (10,500 iron, 7,360 cobblestone)", () -> total(t, r, Items.IRON_INGOT) == before[0]
            && total(t, r, Items.COBBLESTONE) == before[1], () -> total(t, r, Items.IRON_INGOT) + " / " + before[0] + " iron, " + total(t, r, Items.COBBLESTONE)
            + " / " + before[1] + " cobblestone");
        t.check("W12 each of the 6 collapses is in the failure memory", () -> TestM6Colony.colony().memories.stream().filter(m -> m.type().equals("rack_collapse"))
            .count() >= 6, () -> TestM6Colony.colony().memories.stream().filter(m -> m.type().equals("rack_collapse")).count() + " memories");
        t.then(x -> {
            Colony c = TestM6Colony.colony();
            c.autonomy = Colony.Autonomy.AUTONOMOUS;
            TestM6Colony.clearStock(x, r);
            TestM6Colony.stock(x, r, new ItemStack(AutarkiaItems.STEEL_RACK.get(), 6));
            ColonyClock.skip(c, 24000);
        });
        t.waitUntil("W12 the next day the Core proposes and builds steel racks at the same 6 positions", 9000, () -> row.stream().allMatch(p -> t.level
            .getBlockState(p).is(AutarkiaBlocks.STEEL_RACK.get())) && TestM6Colony.colony().projects.stream().anyMatch(p -> p.template.getPath()
            .equals("rack_upgrade") && p.state == Project.State.DONE), x -> TestM6Colony.powerDocks(x, r),() -> TestM6Colony.projectState(t, TestM6Colony.colony()
            .projects.stream().filter(p -> p.template.getPath().equals("rack_upgrade")).reduce((a, b) -> b).orElse(null)) + "; around " + around(t, row)
            + "; drones " + java.util.Arrays.stream(TestM6Colony.drones()).map(TestM6Colony::workerState).toList());
        t.check("W12 the rebuild came from rack_upgrade and the racks face the aisle", () -> TestM6Colony.colony().projects.stream().anyMatch(p -> p.template
            .getPath().equals("rack_upgrade") && p.state == Project.State.DONE) && row.stream().allMatch(p -> MachineBlock.front(t.level.getBlockState(p))
            == Direction.SOUTH), () -> "facing " + MachineBlock.front(t.level.getBlockState(row.get(0))));
        t.then(x -> {
            x.level.getEntitiesOfClass(ItemEntity.class, new AABB(r.at(62, 0, 136)).inflate(16)).forEach(e -> e.discard());
            TestM6Colony.colony().autonomy = Colony.Autonomy.ASSISTED;
        });
    }
}

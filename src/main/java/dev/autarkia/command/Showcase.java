package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.HandCrankBlockEntity;
import dev.autarkia.energy.RollingFlywheelEntity;
import dev.autarkia.energy.SnappedCableEntity;
import dev.autarkia.energy.StoneFlywheelBlockEntity;
import dev.autarkia.energy.TreadwheelBlockEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import dev.autarkia.logistics.ChuteBlockEntity;
import dev.autarkia.processing.QuernBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.MachineBlock;
import dev.autarkia.storage.CrateBlockEntity;
import dev.autarkia.storage.TallyBoardBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * {@code /autarkia showcase m1 [ticks]}: builds the T1 display scene in front of the caller (crank, rope pulleys,
 * stamp mill, quern, crucible, crates with piles, tally board, chute) and, for the given ticks, turns the crank as a
 * player would and the quern as a worker would, so the animations, spans and items are visible.
 */
public final class Showcase {
    private record Driver(ServerLevel level, List<BlockPos> drive, BlockPos chute, long until) {
    }

    private static final List<Driver> DRIVERS = new ArrayList<>();

    private Showcase() {
    }

    /** Register the driver tick. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> DRIVERS.removeIf(Showcase::drive));
    }

    private static boolean drive(Driver d) {
        long now = d.level.getGameTime();
        for (BlockPos p : d.drive) {
            if (d.level.getBlockEntity(p) instanceof HandCrankBlockEntity c) {
                if (now % 4 == 0) {
                    c.crank(net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(d.level));
                }
            } else if (d.level.getBlockEntity(p) instanceof QuernBlockEntity q) {
                q.setWorkerDriving(true);
            }
        }
        if (now % 20 == 0 && d.level.getBlockEntity(d.chute) instanceof ChuteBlockEntity ch) {
            ch.accept(new ItemStack(Items.COBBLESTONE, 4));
        }
        return now >= d.until;
    }

    private static void set(ServerLevel l, BlockPos p, BlockState s) {
        l.setBlock(p, s, 3);
    }

    /** Build the M1 scene and start its driver. */
    public static int m1(CommandSourceStack src, int ticks) {
        ServerLevel l = src.getLevel();
        BlockPos o = BlockPos.containing(src.getPosition()).relative(src.getPlayer() != null ? src.getPlayer().getDirection() : Direction.SOUTH, 4);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-7, 0, -2), o.offset(9, 6, 6))) {
            set(l, p, Blocks.AIR.defaultBlockState());
        }
        l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            net.minecraft.world.phys.AABB.encapsulatingFullBlocks(o.offset(-9, -1, -4), o.offset(11, 8, 8))).forEach(e -> e.discard());
        BlockPos crank = o.offset(-6, 0, 0);
        BlockPos p1 = o.offset(-4, 2, 0);
        BlockPos p2 = o.offset(4, 2, 0);
        BlockPos mill = o.offset(6, 0, 0);
        set(l, crank, AutarkiaBlocks.HAND_CRANK.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
        set(l, p1, AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState());
        set(l, p2, AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState());
        set(l, o.offset(-4, 1, 0), Blocks.OAK_FENCE.defaultBlockState());
        set(l, o.offset(-4, 0, 0), Blocks.OAK_FENCE.defaultBlockState());
        set(l, o.offset(4, 1, 0), Blocks.OAK_FENCE.defaultBlockState());
        set(l, o.offset(4, 0, 0), Blocks.OAK_FENCE.defaultBlockState());
        TestM1.placeBigAt(l, mill, AutarkiaBlocks.STAMP_MILL.get(), Direction.NORTH);
        Grid.addSpan(l, crank, Direction.UP, p1, Direction.UP, CableTier.ROPE, false);
        Grid.addSpan(l, p1, Direction.UP, p2, Direction.UP, CableTier.ROPE, false);
        Grid.addSpan(l, p2, Direction.UP, mill, Direction.WEST, CableTier.ROPE, false);
        TestM1.insertAt(l, mill, new ItemStack(Items.RAW_IRON, 32));

        BlockPos quern = o.offset(-2, 0, 3);
        set(l, quern, AutarkiaBlocks.QUERN.get().defaultBlockState());
        if (l.getBlockEntity(quern) instanceof QuernBlockEntity q) {
            q.insert(new ItemStack(AutarkiaItems.RAW_TIN.get(), 32));
        }
        BlockPos crucible = o.offset(0, 1, 3);
        set(l, o.offset(0, 0, 3), Blocks.CAMPFIRE.defaultBlockState());
        set(l, crucible, AutarkiaBlocks.CRUCIBLE.get().defaultBlockState());
        TestM1.insertAt(l, crucible, new ItemStack(Items.COPPER_INGOT, 30));
        TestM1.insertAt(l, crucible, new ItemStack(AutarkiaItems.TIN_INGOT.get(), 10));

        BlockPos[] crates = {o.offset(3, 0, 3), o.offset(4, 0, 3), o.offset(5, 0, 3)};
        ItemStack[][] fill = {
            {new ItemStack(Items.IRON_INGOT, 64), new ItemStack(Items.IRON_INGOT, 64), new ItemStack(Items.COAL, 64), new ItemStack(Items.COAL, 30)},
            {new ItemStack(AutarkiaItems.TIN_INGOT.get(), 64), new ItemStack(Items.COPPER_INGOT, 64), new ItemStack(Items.COPPER_INGOT, 64)},
            {new ItemStack(AutarkiaItems.BRONZE_INGOT.get(), 64), new ItemStack(Items.REDSTONE, 40), new ItemStack(Items.OAK_LOG, 64)}};
        for (int i = 0; i < crates.length; i++) {
            set(l, crates[i], AutarkiaBlocks.CRATE.get().defaultBlockState());
            if (l.getBlockEntity(crates[i]) instanceof CrateBlockEntity c) {
                for (int k = 0; k < fill[i].length; k++) {
                    for (int g = 0; g < 4; g++) {
                        int slot = g * 7 + k;
                        if (slot < CrateBlockEntity.SLOTS && (g + k + i) % 3 != 0) {
                            c.items().set(slot, ItemResource.of(fill[i][k]), fill[i][k].getCount());
                        }
                    }
                }
            }
        }
        BlockPos tally = o.offset(4, 1, 4);
        set(l, o.offset(4, 0, 4), Blocks.SPRUCE_PLANKS.defaultBlockState());
        set(l, tally, AutarkiaBlocks.TALLY_BOARD.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.NORTH));
        if (l.getBlockEntity(tally) instanceof TallyBoardBlockEntity tb) {
            tb.refresh();
        }
        BlockPos chuteTop = o.offset(7, 1, 3);
        set(l, o.offset(7, 0, 3), AutarkiaBlocks.CRATE.get().defaultBlockState());
        set(l, chuteTop, AutarkiaBlocks.CHUTE.get().defaultBlockState().setValue(MachineBlock.HORIZONTAL_FACING, Direction.EAST));
        set(l, o.offset(8, 0, 3), AutarkiaBlocks.CRATE.get().defaultBlockState());
        DRIVERS.add(new Driver(l, List.of(crank, quern), chuteTop, l.getGameTime() + ticks));
        src.sendSuccess(() -> Component.literal("[showcase] M1 scene built at " + o.toShortString() + ", driven for " + ticks + " ticks"), false);
        return 1;
    }

    /**
     * Build the M2 scene in front of the caller: a cow in a treadwheel and a windmill feeding a charged stone flywheel
     * over rope, a bronze-armored stand, a freshly snapped rope whipping between two pulleys and a released flywheel
     * rolling across the floor.
     */
    public static int m2(CommandSourceStack src) {
        ServerLevel l = src.getLevel();
        BlockPos o = BlockPos.containing(src.getPosition()).relative(src.getPlayer() != null ? src.getPlayer().getDirection() : Direction.SOUTH, 4);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-16, 0, -2), o.offset(12, 8, 14))) {
            set(l, p, Blocks.AIR.defaultBlockState());
        }
        l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, AABB.encapsulatingFullBlocks(o.offset(-18, -1, -4), o.offset(14, 10, 16)),
            e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(e -> e.discard());
        FakePlayer fake = FakePlayerFactory.getMinecraft(l);
        BlockPos tw = o.offset(-6, 0, 3);
        BlockPos pulley = o.offset(0, 3, 3);
        BlockPos mill = o.offset(0, 0, 7);
        BlockPos fly = o.offset(6, 0, 3);
        TestM1.placeBigAt(l, tw, AutarkiaBlocks.TREADWHEEL.get(), Direction.NORTH);
        TestM1.placeBigAt(l, mill, AutarkiaBlocks.WINDMILL.get(), Direction.NORTH);
        TestM1.placeBigAt(l, fly, AutarkiaBlocks.STONE_FLYWHEEL.get(), Direction.NORTH);
        for (int y = 0; y < 3; y++) {
            set(l, pulley.below(3 - y), Blocks.OAK_FENCE.defaultBlockState());
        }
        set(l, pulley, AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState());
        List<String> spans = new ArrayList<>();
        for (BlockPos[] s : new BlockPos[][] {{tw, pulley}, {pulley, fly}, {pulley, mill}}) {
            spans.add(Grid.addSpan(l, s[0], s[1], CableTier.ROPE).toString());
        }
        if (l.getBlockEntity(fly) instanceof StoneFlywheelBlockEntity w) {
            w.buffer().fill(24000);
        }
        Mob cow = EntityTypes.COW.create(l, EntitySpawnReason.COMMAND);
        fake.snapTo(tw.getX() + 0.5, tw.getY(), tw.getZ() - 3.5, 0, 0);
        cow.snapTo(tw.getX() + 0.5, tw.getY(), tw.getZ() - 2.5, 0, 0);
        l.addFreshEntity(cow);
        cow.setLeashedTo(fake, true);
        boolean walking = l.getBlockEntity(tw) instanceof TreadwheelBlockEntity be && be.enter(fake);

        ArmorStand stand = EntityTypes.ARMOR_STAND.create(l, EntitySpawnReason.COMMAND);
        stand.snapTo(o.getX() - 2.5, o.getY(), o.getZ() + 0.5, 180, 0);
        stand.setShowArms(true);
        l.addFreshEntity(stand);
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AutarkiaItems.BRONZE_HELMET.get()));
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(AutarkiaItems.BRONZE_CHEST.get()));
        stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(AutarkiaItems.BRONZE_LEGS.get()));
        stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(AutarkiaItems.BRONZE_BOOTS.get()));
        stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(AutarkiaItems.BRONZE_PICKAXE.get()));
        stand.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(AutarkiaItems.BRONZE_SLING.get()));

        BlockPos snapA = o.offset(9, 3, 0);
        BlockPos snapB = o.offset(9, 3, 9);
        for (BlockPos p : new BlockPos[] {snapA, snapB}) {
            for (int y = 0; y < 3; y++) {
                set(l, p.below(3 - y), Blocks.OAK_FENCE.defaultBlockState());
            }
            set(l, p, AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState());
        }
        double phi = Math.atan2(snapB.getZ() - snapA.getZ(), snapB.getX() - snapA.getX());
        l.addFreshEntity(SnappedCableEntity.create(l, Vec3.atCenterOf(snapA), phi, CableTier.ROPE, 9));
        l.addFreshEntity(SnappedCableEntity.create(l, Vec3.atCenterOf(snapB), phi + Math.PI, CableTier.ROPE, 9));
        l.addFreshEntity(RollingFlywheelEntity.create(l, Vec3.atBottomCenterOf(o.offset(-14, 0, 12)), Direction.NORTH, 6000));
        src.sendSuccess(() -> Component.literal("[showcase] M2 scene built at " + o.toShortString() + ", spans " + spans + ", walker " + walking), false);
        Autarkia.LOG.info("[showcase] M2 scene at {}: spans {}, walker {}", o, spans, walking);
        return 1;
    }
}

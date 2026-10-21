package dev.autarkia.command;

import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.HandCrankBlockEntity;
import dev.autarkia.energy.RollingFlywheelEntity;
import dev.autarkia.energy.SnappedCableEntity;
import dev.autarkia.energy.Span;
import dev.autarkia.energy.StoneFlywheelBlockEntity;
import dev.autarkia.energy.TreadwheelBlockEntity;
import dev.autarkia.energy.WindmillBlockEntity;
import dev.autarkia.equipment.SlingItem;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** {@code /autarkia test m2}: 02 #3 (W4), #4 and #5 (W3), #20; 07 #1 to #3; treadwheel, flywheel and windmill rules. */
public final class TestM2 {
    private TestM2() {
    }

    static Husk husk(SelfTest t, BlockPos p) {
        Husk h = EntityTypes.HUSK.create(t.level, EntitySpawnReason.COMMAND);
        h.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        h.setNoAi(true);
        h.setPersistenceRequired();
        h.getAttribute(Attributes.ARMOR).setBaseValue(0);
        t.level.addFreshEntity(h);
        return h;
    }

    /** Build the rigs and run the M2 script. */
    public static int run(CommandSourceStack src) {
        SelfTest t = SelfTest.create("m2", src);
        FakePlayer fake = FakePlayerFactory.getMinecraft(t.level);
        t.then(x -> {
            x.command("forceload add " + (x.origin.getX() - 16) + " " + (x.origin.getZ() - 16) + " " + (x.origin.getX() + 160) + " " + (x.origin.getZ() + 32));
            x.clear(-2, 0, -6, 60, 14, 22);
            x.clear(61, 0, 6, 150, 8, 14);
        });
        t.then(x -> x.level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(Vec3.atCenterOf(x.at(-40, -20, -40)), Vec3.atCenterOf(x.at(160, 30, 60))),
            e -> e instanceof Mob || e instanceof RollingFlywheelEntity || e instanceof SnappedCableEntity
                || e instanceof net.minecraft.world.entity.item.ItemEntity || e instanceof ArmorStand).forEach(e -> e.discard()));
        ropeSnap(t, fake);
        flywheel(t);
        windmill(t);
        treadwheel(t, fake);
        equipment(t, fake);
        t.check("registries: power advancements and the sling damage type are loaded", () -> {
            for (String a : List.of("power/root", "power/first_span", "power/rope_snap", "power/rolling_wheel")) {
                if (t.level.getServer().getAdvancements().get(dev.autarkia.Autarkia.id(a)) == null) {
                    return false;
                }
            }
            return t.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(dev.autarkia.equipment.AutarkiaDamage.SLING).isPresent();
        }, () -> "4 advancements, autarkia:sling");
        return t.start();
    }

    private static void ropeSnap(SelfTest t, FakePlayer fake) {
        BlockPos pa = t.at(0, 4, 0);
        BlockPos pb = t.at(12, 4, 0);
        List<BlockPos> cranks = List.of(t.at(0, 4, 1), t.at(0, 4, -1), t.at(-1, 4, 0));
        List<BlockPos> mills = List.of(t.at(13, 4, 0), t.at(12, 4, 1), t.at(12, 4, -1));
        Husk[] h = new Husk[1];
        t.then(x -> {
            TestM1.place(x, pa, AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState());
            TestM1.place(x, pb, AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState());
            cranks.forEach(c -> TestM1.place(x, c, AutarkiaBlocks.HAND_CRANK.get().defaultBlockState()));
            for (BlockPos m : mills) {
                TestM1.placeBig(x, m, AutarkiaBlocks.STAMP_MILL.get(), Direction.NORTH);
                TestM1.insertItems(x, m, new ItemStack(Items.RAW_IRON, 64));
            }
            Grid.SpanResult r = Grid.addSpan(x.level, pa, Direction.UP, pb, Direction.UP, CableTier.ROPE, false);
            x.report("02#3 rope span of 12 blocks", r == Grid.SpanResult.OK, r.toString());
            h[0] = husk(x, x.at(3, 0, 0));
        });
        t.waitUntil("02#3 rope snaps", 3000, () -> TestM1.spanBetween(t, pa, pb) == null, x -> {
            if (x.level.getGameTime() % 4 == 0) {
                for (BlockPos c : cranks) {
                    if (x.level.getBlockEntity(c) instanceof HandCrankBlockEntity be) {
                        be.crank(fake);
                    }
                }
            }
        });
        t.check("02#3 two snapped cable ends spawn", () -> t.level.getEntitiesOfClass(SnappedCableEntity.class, new AABB(Vec3.atCenterOf(pa), Vec3.atCenterOf(pb)).inflate(8)).size() == 2,
            () -> t.level.getEntitiesOfClass(SnappedCableEntity.class, new AABB(Vec3.atCenterOf(pa), Vec3.atCenterOf(pb)).inflate(8)).size() + " ends");
        t.waitTicks(60);
        t.check("02#3 the whipping cable hurts the husk (3 per hit)", () -> h[0].getHealth() <= h[0].getMaxHealth() - 3
            && Math.round(h[0].getMaxHealth() - h[0].getHealth()) % 3 == 0, () -> "husk health " + h[0].getHealth());
        t.waitTicks(50);
        t.check("02#3 after 100 ticks each end drops 1 rope coil", () -> TestM1.countItems(t, t.at(6, 0, 0), 10, AutarkiaItems.ROPE_COIL.get()) == 2
            && t.level.getEntitiesOfClass(SnappedCableEntity.class, new AABB(Vec3.atCenterOf(pa), Vec3.atCenterOf(pb)).inflate(8)).isEmpty(),
            () -> TestM1.countItems(t, t.at(6, 0, 0), 10, AutarkiaItems.ROPE_COIL.get()) + " coils");
    }

    private static StoneFlywheelBlockEntity wheel(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof StoneFlywheelBlockEntity w ? w : null;
    }

    private static AABB rollStrip(SelfTest t) {
        return AABB.encapsulatingFullBlocks(t.at(-12, -2, 6), t.at(152, 12, 14));
    }

    private static List<ItemEntity> wheelItems(SelfTest t) {
        return t.level.getEntitiesOfClass(ItemEntity.class, rollStrip(t), e -> e.getItem().is(AutarkiaItems.STONE_FLYWHEEL.get()));
    }

    private static void flywheel(SelfTest t) {
        BlockPos top = t.at(20, 6, 10);
        BlockPos small = t.at(20, 0, 18);
        Husk[] h = new Husk[1];
        int[] lossCheck = new int[2];
        t.then(x -> {
            for (int dx = -10; dx <= 40; dx++) {
                int height = dx <= 1 ? 6 : Math.max(0, 6 - (dx - 1) / 2);
                for (int y = 0; y < height; y++) {
                    for (int dz = 8; dz <= 12; dz++) {
                        TestM1.place(x, x.at(20 + dx, y, dz), Blocks.STONE.defaultBlockState());
                    }
                }
            }
            x.report("W3 flywheel placed on the hill", TestM1.placeBig(x, top, AutarkiaBlocks.STONE_FLYWHEEL.get(), Direction.NORTH), "3x3x1");
            wheel(x, top).buffer().fill(40000);
            h[0] = husk(x, x.at(27, 3, 10));
            lossCheck[0] = wheel(x, top).stored();
        });
        t.waitTicks(100);
        t.check("flywheel self-loss 0.5 + 0.00005 E per tick", () -> {
            int lost = lossCheck[0] - wheel(t, top).stored();
            return lost >= 220 && lost <= 260;
        }, () -> "lost " + (lossCheck[0] - wheel(t, top).stored()) + " FE in 100 ticks");
        t.check("flywheel I/O = 40 x max(0.25, omega)", () -> wheel(t, top).gridMaxOut() == Math.round(40 * Math.max(0.25, wheel(t, top).omega())),
            () -> "out " + wheel(t, top).gridMaxOut());
        t.then(x -> x.level.destroyBlock(top.relative(Direction.EAST), true));
        t.waitTicks(2);
        t.check("02#4 breaking a position releases a rolling flywheel and drops nothing",
            () -> !t.level.getEntitiesOfClass(RollingFlywheelEntity.class, new AABB(Vec3.atCenterOf(top), Vec3.atCenterOf(top)).inflate(4)).isEmpty()
                && TestM1.countItems(t, top, 4, AutarkiaItems.STONE_FLYWHEEL.get()) == 0,
            () -> t.level.getEntitiesOfClass(RollingFlywheelEntity.class, new AABB(Vec3.atCenterOf(top), Vec3.atCenterOf(top)).inflate(4)).size() + " wheels");
        t.waitUntil("02#4 the wheel rolls down and hits the husk for 15+", 200, () -> h[0].getMaxHealth() - h[0].getHealth() >= 15 || !h[0].isAlive(), x -> { });
        t.waitUntil("02#4 on flat ground it wobbles and drops a stone_flywheel item", 1300,
            () -> t.level.getEntitiesOfClass(RollingFlywheelEntity.class, rollStrip(t)).isEmpty(), x -> { });
        t.check("02#4 a stone_flywheel item lies where the wheel stopped",
            () -> wheelItems(t).size() == 1, () -> wheelItems(t).size() + " items");
        t.then(x -> {
            wheelItems(x).forEach(e -> e.discard());
            TestM1.placeBig(x, small, AutarkiaBlocks.STONE_FLYWHEEL.get(), Direction.NORTH);
            wheel(x, small).buffer().fill(1000);
            x.level.destroyBlock(small.above(), true);
        });
        t.waitTicks(3);
        t.check("02#5 with 1,000 FE no wheel rolls and the block drops", () -> TestM1.countItems(t, small, 3, AutarkiaItems.STONE_FLYWHEEL.get()) == 1
            && t.level.getEntitiesOfClass(RollingFlywheelEntity.class, new AABB(Vec3.atCenterOf(small), Vec3.atCenterOf(small)).inflate(4)).isEmpty(),
            () -> TestM1.countItems(t, small, 3, AutarkiaItems.STONE_FLYWHEEL.get()) + " items");
        BlockPos mount = t.at(28, 0, 18);
        t.then(x -> {
            TestM1.place(x, mount.below(), Blocks.SAND.defaultBlockState());
            TestM1.placeBig(x, mount, AutarkiaBlocks.STONE_FLYWHEEL.get(), Direction.NORTH);
            x.level.removeBlock(mount.below(), false);
        });
        t.waitTicks(4);
        t.check("flywheel mount: losing the block below breaks the wheel", () -> !t.level.getBlockState(mount).is(AutarkiaBlocks.STONE_FLYWHEEL.get()),
            () -> t.level.getBlockState(mount).toString());
    }

    private static void windmill(SelfTest t) {
        BlockPos low = t.at(4, 0, 18);
        t.check("02#20 windmill base 7.2 at rotor y 70 and 32 at y 140", () -> Math.abs(WindmillBlockEntity.base(70) - 7.2) < 1e-6
            && WindmillBlockEntity.base(140) == 32, () -> WindmillBlockEntity.base(70) + " / " + WindmillBlockEntity.base(140));
        boolean[] advanceWeather = new boolean[1];
        t.then(x -> {
            TestM1.placeBig(x, low, AutarkiaBlocks.WINDMILL.get(), Direction.NORTH);
            advanceWeather[0] = x.level.getGameRules().get(GameRules.ADVANCE_WEATHER);
            x.level.getGameRules().set(GameRules.ADVANCE_WEATHER, true, x.level.getServer());
            BlockPos a = low.offset(-4, -2, -4);
            BlockPos b = low.offset(4, 12, 4);
            x.command("fillbiome " + a.getX() + " " + a.getY() + " " + a.getZ() + " " + b.getX() + " " + b.getY() + " " + b.getZ() + " minecraft:plains");
            x.command("weather thunder");
        });
        t.waitTicks(150);
        t.then(x -> {
            if (x.level.getBlockEntity(low) instanceof WindmillBlockEntity w) {
                w.recompute();
            }
        });
        t.check("02#20 thunder multiplies the output by 1.3", () -> t.level.getBlockEntity(low) instanceof WindmillBlockEntity w && w.weather() == 1.3,
            () -> t.level.getBlockEntity(low) instanceof WindmillBlockEntity w ? "M " + w.weather() : "missing");
        t.then(x -> {
            x.command("weather clear");
            x.level.getGameRules().set(GameRules.ADVANCE_WEATHER, advanceWeather[0], x.level.getServer());
        });
        t.check("windmill output = min(40, round(base x W x M x obstruction))", () -> {
            if (!(t.level.getBlockEntity(low) instanceof WindmillBlockEntity w)) {
                return false;
            }
            for (int i = -1; i <= 1; i++) {
                TestM1.place(t, w.rotor().relative(Direction.NORTH).east(i), Blocks.STONE.defaultBlockState());
            }
            w.recompute();
            long expect = Math.min(40, Math.round(WindmillBlockEntity.base(w.rotor().getY()) * Math.max(0.5, Math.min(1.5, 0.5 + w.wind())) * w.weather() * 0.7));
            return w.obstruction() == 3 && w.output() == expect;
        }, () -> t.level.getBlockEntity(low) instanceof WindmillBlockEntity w ? "out " + w.output() + ", blocked " + w.obstruction() + ", wind " + w.wind() : "missing");
        t.check("windmill: 5 blocks in front stop it", () -> {
            if (!(t.level.getBlockEntity(low) instanceof WindmillBlockEntity w)) {
                return false;
            }
            TestM1.place(t, w.rotor().relative(Direction.NORTH).above(), Blocks.STONE.defaultBlockState());
            TestM1.place(t, w.rotor().relative(Direction.NORTH).below(), Blocks.STONE.defaultBlockState());
            w.recompute();
            return w.output() == 0;
        }, () -> "output 0 at 5 blocks");
    }

    private static void treadwheel(SelfTest t, FakePlayer fake) {
        BlockPos tw = t.at(10, 0, 18);
        t.then(x -> {
            TestM1.placeBig(x, tw, AutarkiaBlocks.TREADWHEEL.get(), Direction.NORTH);
            Mob cow = EntityTypes.COW.create(x.level, EntitySpawnReason.COMMAND);
            fake.snapTo(tw.getX() + 0.5, tw.getY(), tw.getZ() + 3.5, 0, 0);
            cow.snapTo(tw.getX() + 0.5, tw.getY(), tw.getZ() + 2.5, 0, 0);
            x.level.addFreshEntity(cow);
            cow.setLeashedTo(fake, true);
            x.report("treadwheel takes a led cow", x.level.getBlockEntity(tw) instanceof TreadwheelBlockEntity be && be.enter(fake), "lead on wheel");
        });
        t.waitTicks(5);
        t.check("treadwheel: a cow gives 12 FE/t", () -> t.level.getBlockEntity(tw) instanceof TreadwheelBlockEntity be && be.output() == 12,
            () -> t.level.getBlockEntity(tw) instanceof TreadwheelBlockEntity be ? "out " + be.output() : "missing");
        t.check("treadwheel keeps the lead: no lead item drops on entry", () -> TestM1.countItems(t, tw, 8, Items.LEAD) == 0,
            () -> TestM1.countItems(t, tw, 8, Items.LEAD) + " lead items");
        BlockPos pulley = t.at(10, 3, 21);
        BlockPos mill1 = t.at(14, 0, 21);
        BlockPos mill2 = t.at(6, 0, 21);
        t.then(x -> {
            for (int y = 0; y < 3; y++) {
                TestM1.place(x, pulley.below(3 - y), Blocks.OAK_FENCE.defaultBlockState());
            }
            TestM1.place(x, pulley, AutarkiaBlocks.ROPE_PULLEY.get().defaultBlockState());
            TestM1.placeBig(x, mill1, AutarkiaBlocks.STAMP_MILL.get(), Direction.NORTH);
            TestM1.placeBig(x, mill2, AutarkiaBlocks.STAMP_MILL.get(), Direction.NORTH);
            Grid.SpanResult a = Grid.addSpan(x.level, tw, pulley, CableTier.ROPE);
            Grid.SpanResult b = Grid.addSpan(x.level, pulley, mill1, CableTier.ROPE);
            x.report("treadwheel spans to a pulley and a stamp mill", a == Grid.SpanResult.OK && b == Grid.SpanResult.OK, a + ", " + b);
        });
        t.waitTicks(40);
        t.check("treadwheel feeds the stamp mill over rope", () -> millEnergy(t, mill1) > 0, () -> "mill energy " + millEnergy(t, mill1));
        t.then(x -> {
            Grid.SpanResult c = Grid.addSpan(x.level, pulley, mill2, CableTier.ROPE);
            x.report("span added to the live network", c == Grid.SpanResult.OK, c.toString());
        });
        t.waitTicks(40);
        t.check("the live-extended network serves the new mill", () -> millEnergy(t, mill2) > 0, () -> "new mill energy " + millEnergy(t, mill2));
    }

    private static int millEnergy(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof dev.autarkia.processing.MachineBlockEntity m && m.energy() != null ? m.energy().stored() : -1;
    }

    private static void equipment(SelfTest t, FakePlayer fake) {
        t.check("07#1 bronze pickaxe mines iron and diamond ore, not obsidian; 4.0 / 1.2", () -> {
            ItemStack pick = new ItemStack(AutarkiaItems.BRONZE_PICKAXE.get());
            boolean drops = pick.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()) && pick.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())
                && !pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState());
            double[] dmg = {1};
            double[] spd = {4};
            pick.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
                if (attr.equals(Attributes.ATTACK_DAMAGE)) {
                    dmg[0] += mod.amount();
                } else if (attr.equals(Attributes.ATTACK_SPEED)) {
                    spd[0] += mod.amount();
                }
            });
            return drops && Math.abs(dmg[0] - 4.0) < 1e-4 && Math.abs(spd[0] - 1.2) < 1e-4 && pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 6.5f;
        }, () -> "drop rules, attack and speed");
        ArmorStand[] stand = new ArmorStand[1];
        t.then(x -> {
            stand[0] = EntityTypes.ARMOR_STAND.create(x.level, EntitySpawnReason.COMMAND);
            stand[0].snapTo(Vec3.atCenterOf(x.at(30, 0, 18)));
            x.level.addFreshEntity(stand[0]);
            stand[0].setItemSlot(EquipmentSlot.HEAD, new ItemStack(AutarkiaItems.BRONZE_HELMET.get()));
            stand[0].setItemSlot(EquipmentSlot.CHEST, new ItemStack(AutarkiaItems.BRONZE_CHEST.get()));
            stand[0].setItemSlot(EquipmentSlot.LEGS, new ItemStack(AutarkiaItems.BRONZE_LEGS.get()));
            stand[0].setItemSlot(EquipmentSlot.FEET, new ItemStack(AutarkiaItems.BRONZE_BOOTS.get()));
        });
        t.waitTicks(2);
        t.check("07#2 full bronze gives 13 armor points", () -> stand[0].getArmorValue() == 13, () -> "armor " + stand[0].getArmorValue());
        Husk[] target = new Husk[2];
        ItemStack[] sling = new ItemStack[1];
        int[] ammo = new int[1];
        for (int i = 0; i < 2; i++) {
            final int k = i;
            t.then(x -> {
                target[k] = husk(x, x.at(36 + k * 6, 0, 18));
                fake.snapTo(Vec3.atCenterOf(x.at(36 + k * 6, 0, 13)).x, x.origin.getY(), Vec3.atCenterOf(x.at(36 + k * 6, 0, 13)).z, 0, 0);
                fake.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target[k].getBoundingBox().getCenter());
                fake.getInventory().clearContent();
                sling[0] = new ItemStack(AutarkiaItems.BRONZE_SLING.get());
                fake.setItemInHand(InteractionHand.MAIN_HAND, sling[0]);
                fake.getInventory().add(new ItemStack(k == 0 ? Items.COBBLESTONE : AutarkiaItems.BRONZE_NUGGET.get(), 4));
                fake.startUsingItem(InteractionHand.MAIN_HAND);
                sling[0].getItem().releaseUsing(sling[0], x.level, fake, 72000 - SlingItem.FULL_CHARGE);
                ammo[0] = fake.getInventory().countItem(k == 0 ? Items.COBBLESTONE : AutarkiaItems.BRONZE_NUGGET.get());
            });
            t.waitTicks(20);
            t.check("07#3 sling " + (k == 0 ? "cobblestone 3" : "nugget 4") + " damage, 1 ammo, 1 durability",
                () -> Math.abs(target[k].getMaxHealth() - target[k].getHealth() - (k == 0 ? 3 : 4)) < 1e-3 && ammo[0] == 3 && sling[0].getDamageValue() == 1,
                () -> "husk " + target[k].getHealth() + ", ammo left " + ammo[0] + ", sling damage " + sling[0].getDamageValue());
        }
    }
}

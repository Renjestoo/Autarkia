package dev.autarkia.command;

import com.mojang.authlib.GameProfile;
import dev.autarkia.Autarkia;
import dev.autarkia.equipment.AutarkiaDamage;
import dev.autarkia.equipment.DashBladeItem;
import dev.autarkia.equipment.FrontierSuitItem;
import dev.autarkia.equipment.GrappleHookEntity;
import dev.autarkia.equipment.GrappleLauncherItem;
import dev.autarkia.equipment.PoweredItem;
import dev.autarkia.equipment.RailRifleItem;
import dev.autarkia.equipment.SuitItem;
import dev.autarkia.equipment.ToolMode;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * The equipment part of {@code /autarkia test m7} (07 #12 to #16) on the platform region x 0..40, z 180..230: excavator
 * vein mode on iron veins, the grapple pull, hang and protected drop, the blade dash and bonus hit, the full frontier
 * suit (night vision, jet, explosion knockback) and the rail rifle through 6 zombies. The level does not tick fake
 * players, so the script runs their per-tick work: item use ticks, suit inventory ticks, the dash step, the cooldown
 * clock and the server's replay of a player move (move, fall damage check, then gravity 0.08 with drag 0.98 / 0.91).
 * Explosions only reach level entities, so the knockback check compares two zombies, one in the suit.
 */
final class TestM7Equipment {
    private static final UUID FAKE = UUID.nameUUIDFromBytes("autarkia-m7-equipment".getBytes(StandardCharsets.UTF_8));
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private TestM7Equipment() {
    }

    static void run(TestM7.Run r) {
        data(r);
        vein(r);
        grapple(r);
        blade(r);
        suit(r);
        rail(r);
    }

    private static FakePlayer gear(SelfTest t) {
        return FakePlayerFactory.get(t.level, new GameProfile(FAKE, "AutarkiaM7Gear"));
    }

    private static FakePlayer fresh(SelfTest t) {
        FakePlayer f = gear(t);
        f.stopUsingItem();
        for (EquipmentSlot s : ARMOR) {
            if (f.getItemBySlot(s).getItem() instanceof SuitItem suit) {
                suit.clear(f);
            }
            f.setItemSlot(s, ItemStack.EMPTY);
        }
        f.getInventory().clearContent();
        for (Item item : List.of(AutarkiaItems.GRAPPLE_LAUNCHER.get(), AutarkiaItems.VANTITE_BLADE.get(), AutarkiaItems.RAIL_RIFLE.get())) {
            f.getCooldowns().removeCooldown(f.getCooldowns().getCooldownGroup(new ItemStack(item)));
        }
        f.setLastClientInput(Input.EMPTY);
        f.setShiftKeyDown(false);
        return f;
    }

    private static ItemStack charged(Item item, int fe) {
        ItemStack s = new ItemStack(item);
        PoweredItem.setEnergy(s, fe);
        return s;
    }

    private static void place(FakePlayer f, BlockPos p, boolean ground) {
        f.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        f.setDeltaMovement(Vec3.ZERO);
        f.resetFallDistance();
        f.setOnGround(ground);
    }

    private static void airMove(FakePlayer f) {
        Vec3 from = f.position();
        f.move(MoverType.PLAYER, f.getDeltaMovement());
        Vec3 d = f.position().subtract(from);
        f.doCheckFallDamage(d.x, d.y, d.z, f.onGround());
        Vec3 v = f.getDeltaMovement();
        f.setDeltaMovement(v.x * 0.91, (v.y - 0.08) * 0.98, v.z * 0.91);
    }

    private static void useTick(FakePlayer f) {
        ItemStack s = f.getUseItem();
        if (!s.isEmpty()) {
            s.getItem().onUseTick(f.level(), f, s, f.getUseItemRemainingTicks());
        }
    }

    private static <T extends Mob> T mob(SelfTest t, EntityType<T> type, BlockPos at, double health) {
        T m = type.create(t.level, EntitySpawnReason.COMMAND);
        m.setNoAi(true);
        m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180, 0);
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        m.setHealth((float) health);
        t.level.addFreshEntity(m);
        return m;
    }

    private static void data(TestM7.Run r) {
        SelfTest t = r.t();
        t.check("07 data: equipment recipes 29 to 36, advancement equipment/swing_line, damage type autarkia:rail", () -> {
            var map = t.level.recipeAccess().recipeMap();
            for (String id : List.of("fabricating/vantite_excavator", "fabricating/grapple_launcher", "vantite_blade", "fabricating/frontier_suit_helmet",
                "fabricating/frontier_suit_chest", "fabricating/frontier_suit_legs", "fabricating/frontier_suit_boots", "fabricating/rail_rifle")) {
                if (map.byKey(ResourceKey.create(Registries.RECIPE, Autarkia.id(id))) == null) {
                    Autarkia.LOG.info("[TEST m7] missing recipe {}", id);
                    return false;
                }
            }
            return t.level.getServer().getAdvancements().get(Autarkia.id("equipment/swing_line")) != null
                && t.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(AutarkiaDamage.RAIL).isPresent();
        }, () -> "8 recipes, 1 advancement, 1 damage type");
    }

    private static void vein(TestM7.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            List<BlockPos> small = new ArrayList<>();
            for (int dx = 0; dx < 2; dx++) {
                for (int y = 0; y < 3; y++) {
                    for (int dz = 0; dz < 2; dz++) {
                        BlockPos p = r.at(2 + dx, y, 182 + dz);
                        small.add(p);
                        TestM1.place(x, p, (y == 2 ? Blocks.DEEPSLATE_IRON_ORE : Blocks.IRON_ORE).defaultBlockState());
                    }
                }
            }
            BlockPos coal = r.at(4, 0, 182);
            BlockPos stone = r.at(4, 1, 183);
            TestM1.place(x, coal, Blocks.COAL_ORE.defaultBlockState());
            TestM1.place(x, stone, Blocks.STONE.defaultBlockState());
            TestM7.box(r, 8, 0, 182, 11, 4, 185, Blocks.IRON_ORE.defaultBlockState());
            List<BlockPos> big = new ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(r.at(8, 0, 182), r.at(11, 4, 185))) {
                big.add(p.immutable());
            }
            FakePlayer f = fresh(x);
            ItemStack tool = charged(AutarkiaItems.VANTITE_EXCAVATOR.get(), 2000000);
            tool.set(AutarkiaComponents.TOOL_MODE.get(), ToolMode.VEIN);
            f.setItemInHand(InteractionHand.MAIN_HAND, tool);
            f.gameMode.destroyBlock(small.get(0));
            int used = 2000000 - PoweredItem.energy(f.getMainHandItem());
            long left = small.stream().filter(p -> !x.level.getBlockState(p).isAir()).count();
            boolean kept = x.level.getBlockState(coal).is(Blocks.COAL_ORE) && x.level.getBlockState(stone).is(Blocks.STONE);
            x.report("07#12 vein mode breaks the whole 12-block iron vein (stone and deepslate ore), not the coal or stone, 400 FE each", left == 0 && kept
                && used == 12 * 400, "left " + left + ", coal and stone kept " + kept + ", used " + used + " FE");
            int before = PoweredItem.energy(f.getMainHandItem());
            f.gameMode.destroyBlock(big.get(0));
            int usedBig = before - PoweredItem.energy(f.getMainHandItem());
            long broken = big.stream().filter(p -> x.level.getBlockState(p).isAir()).count();
            x.report("07#12 an 80-block iron vein breaks at most 64 blocks, 400 FE each", broken == 64 && usedBig == 64 * 400,
                broken + " broken, " + usedBig + " FE");
            TestM7.box(r, 2, 0, 182, 11, 4, 185, Blocks.AIR.defaultBlockState());
            TestM1.killItems(x, r.at(6, 2, 184), 10);
        });
    }

    private static GrappleHookEntity hook(SelfTest t) {
        return GrappleLauncherItem.hook(t.level, gear(t));
    }

    private static void grapple(TestM7.Run r) {
        SelfTest t = r.t();
        List<Double> steps = new ArrayList<>();
        int[] fe = new int[3];
        long[] released = new long[1];
        double[] hang = new double[1];
        float[] health = new float[2];
        t.then(x -> {
            TestM7.box(r, 20, 0, 181, 20, 11, 181, Blocks.STONE.defaultBlockState());
            TestM7.box(r, 17, 0, 222, 23, 20, 222, Blocks.STONE.defaultBlockState());
            FakePlayer f = fresh(x);
            place(f, r.at(20, 12, 181), true);
            f.setPermanentlyInvulnerable(false);
            f.connection.markClientLoaded();
            f.setHealth(f.getMaxHealth());
            f.setItemInHand(InteractionHand.MAIN_HAND, charged(AutarkiaItems.GRAPPLE_LAUNCHER.get(), GrappleLauncherItem.CAPACITY));
            InteractionResult res = f.getMainHandItem().getItem().use(x.level, f, InteractionHand.MAIN_HAND);
            int used = GrappleLauncherItem.CAPACITY - PoweredItem.energy(f.getMainHandItem());
            x.report("07#13 firing the grapple launcher costs 2,000 FE and launches a hook", res.consumesAction() && used == GrappleLauncherItem.SHOT_COST
                && hook(x) != null && f.isUsingItem(), res + ", " + used + " FE");
        });
        t.waitUntil("07#13 the hook attaches to the wall 40 blocks away", 40, () -> hook(t) != null && hook(t).attached(), x -> useTick(gear(x)));
        t.check("07#13 the line is at least 40 blocks long at attach", () -> hook(t).attachLength() >= GrappleLauncherItem.SWING_DISTANCE,
            () -> "length " + hook(t).attachLength());
        t.waitUntil("07#13 the pull brings the player to the wall", 80, () -> hook(t) != null
            && GrappleHookEntity.center(gear(t)).distanceTo(hook(t).position()) <= GrappleLauncherItem.ARRIVE, x -> {
                FakePlayer f = gear(x);
                double far = GrappleHookEntity.center(f).distanceTo(hook(x).position());
                Vec3 before = f.position();
                int e0 = PoweredItem.energy(f.getMainHandItem());
                useTick(f);
                airMove(f);
                if (far > 2 * GrappleLauncherItem.PULL_SPEED) {
                    steps.add(f.position().distanceTo(before));
                    fe[0] += e0 - PoweredItem.energy(f.getMainHandItem());
                    fe[1]++;
                }
            });
        t.check("07#13 the pull moves 1.2 blocks per tick for 20 FE per tick", () -> !steps.isEmpty()
            && steps.stream().allMatch(d -> Math.abs(d - GrappleLauncherItem.PULL_SPEED) < 0.01) && fe[0] == GrappleLauncherItem.PULL_COST * fe[1],
            () -> String.format(Locale.ROOT, "%d steps, min %.3f, max %.3f, %d FE", steps.size(), steps.stream().mapToDouble(d -> d).min().orElse(0),
                steps.stream().mapToDouble(d -> d).max().orElse(0), fe[0]));
        t.then(x -> useTick(gear(x)));
        t.check("07#13 the full pull of a 40+ block line fires grapple_40 (equipment/swing_line) on the tick use is held at the hook",
            () -> hook(t).fullPull(), () -> "full pull recorded " + hook(t).fullPull());
        t.then(x -> {
            FakePlayer f = gear(x);
            f.setShiftKeyDown(true);
            fe[2] = PoweredItem.energy(f.getMainHandItem());
        });
        t.repeat(20, x -> {
            FakePlayer f = gear(x);
            Vec3 before = f.position();
            useTick(f);
            airMove(f);
            hang[0] = Math.max(hang[0], f.position().distanceTo(before));
        });
        t.check("07#13 sneaking hangs in place for 5 FE per tick", () -> hang[0] < 0.001
            && fe[2] - PoweredItem.energy(gear(t).getMainHandItem()) == 20 * GrappleLauncherItem.HANG_COST,
            () -> String.format(Locale.ROOT, "max drift %.4f, %d FE in 20 ticks", hang[0], fe[2] - PoweredItem.energy(gear(t).getMainHandItem())));
        t.then(x -> {
            FakePlayer f = gear(x);
            f.setShiftKeyDown(false);
            health[0] = f.getHealth();
            released[0] = x.level.getGameTime();
            f.releaseUsingItem();
            x.report("07#13 releasing detaches the hook", hook(x) == null && !f.isUsingItem(), "using " + f.isUsingItem());
        });
        t.waitUntil("07#13 after the release the player drops to the floor", 60, () -> gear(t).onGround(), x -> airMove(gear(x)));
        t.check("07#13 the drop within 40 ticks of the release does no fall damage", () -> t.level.getGameTime() - released[0] <= GrappleLauncherItem.SAFE_TICKS
            && gear(t).getHealth() == health[0], () -> (t.level.getGameTime() - released[0]) + " ticks, health " + health[0] + " -> " + gear(t).getHealth());
        t.waitUntil("07#13 the 40-tick protection runs out", 60, () -> t.level.getGameTime() > released[0] + GrappleLauncherItem.SAFE_TICKS, x -> { });
        t.then(x -> {
            FakePlayer f = gear(x);
            f.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            place(f, r.at(20, 12, 221), false);
            health[1] = f.getHealth();
        });
        t.waitUntil("07#13 control: the same drop later lands", 60, () -> gear(t).onGround(), x -> airMove(gear(x)));
        t.check("07#13 control: the same drop after the protection does fall damage", () -> gear(t).getHealth() < health[1],
            () -> "health " + health[1] + " -> " + gear(t).getHealth());
        t.then(x -> {
            FakePlayer f = gear(x);
            f.setHealth(f.getMaxHealth());
            f.setPermanentlyInvulnerable(true);
            TestM7.box(r, 20, 0, 181, 20, 11, 181, Blocks.AIR.defaultBlockState());
            TestM7.box(r, 17, 0, 222, 23, 20, 222, Blocks.AIR.defaultBlockState());
        });
    }

    private static void blade(TestM7.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            FakePlayer f = fresh(x);
            place(f, r.at(30, 0, 184), true);
            Skeleton a = mob(x, EntityTypes.SKELETON, r.at(30, 0, 192), 20);
            Skeleton b = mob(x, EntityTypes.SKELETON, r.at(36, 0, 192), 20);
            ItemStack blade = new ItemStack(AutarkiaItems.VANTITE_BLADE.get());
            f.setItemInHand(InteractionHand.MAIN_HAND, blade);
            InteractionResult res = blade.getItem().use(x.level, f, InteractionHand.MAIN_HAND);
            Vec3 p0 = f.position();
            for (int i = 0; i < DashBladeItem.DASH_TICKS; i++) {
                DashBladeItem.step(f);
            }
            Vec3 moved = f.position().subtract(p0);
            int dashWear = f.getMainHandItem().getDamageValue();
            float ha = a.getHealth();
            f.attack(a);
            float da = ha - a.getHealth();
            float hb = b.getHealth();
            f.attack(b);
            float db = hb - b.getHealth();
            x.report("07#14 the dash moves 6 blocks forward and costs 12 durability", res.consumesAction() && Math.abs(moved.z - 6.0) < 1.0E-6
                && moved.horizontalDistance() - moved.z < 1.0E-6 && dashWear == DashBladeItem.DURABILITY_COST,
                String.format(Locale.ROOT, "%s, moved %s, durability used by the dash %d", res, moved, dashWear));
            x.report("07#14 the first hit on a skeleton after the dash deals +4 damage, the next hit does not", Math.abs(da - db - DashBladeItem.BONUS) < 0.01f,
                String.format(Locale.ROOT, "first %.2f, second %.2f", da, db));
            InteractionResult again = blade.getItem().use(x.level, f, InteractionHand.MAIN_HAND);
            for (int i = 0; i < DashBladeItem.COOLDOWN - 1; i++) {
                f.getCooldowns().tick();
            }
            boolean still = f.getCooldowns().isOnCooldown(f.getMainHandItem());
            f.getCooldowns().tick();
            boolean free = !f.getCooldowns().isOnCooldown(f.getMainHandItem());
            x.report("07#14 the dash has an 80-tick (4 s) cooldown", !again.consumesAction() && still && free, again + ", at 79 ticks " + still
                + ", free at 80 " + free);
            a.discard();
            b.discard();
        });
    }

    private static ItemStack[] frontier() {
        return new ItemStack[] {charged(AutarkiaItems.FRONTIER_SUIT_HELMET.get(), FrontierSuitItem.CAPACITY),
            charged(AutarkiaItems.FRONTIER_SUIT_CHEST.get(), FrontierSuitItem.CAPACITY), charged(AutarkiaItems.FRONTIER_SUIT_LEGS.get(), FrontierSuitItem.CAPACITY),
            charged(AutarkiaItems.FRONTIER_SUIT_BOOTS.get(), FrontierSuitItem.CAPACITY)};
    }

    private static long suitEnergy(LivingEntity le) {
        long n = 0;
        for (EquipmentSlot s : ARMOR) {
            n += PoweredItem.energy(le.getItemBySlot(s));
        }
        return n;
    }

    private static void suitTick(Level level, LivingEntity le) {
        for (EquipmentSlot s : ARMOR) {
            le.getItemBySlot(s).inventoryTick(level, le, s);
        }
    }

    private static void suit(TestM7.Run r) {
        SelfTest t = r.t();
        double[] lift = new double[3];
        long[] fe = new long[1];
        t.then(x -> {
            FakePlayer f = fresh(x);
            place(f, r.at(30, 2, 205), false);
            ItemStack[] pieces = frontier();
            for (int i = 0; i < ARMOR.length; i++) {
                f.setItemSlot(ARMOR[i], pieces[i]);
            }
            f.setLastClientInput(new Input(false, false, false, false, true, false, false));
            fe[0] = suitEnergy(f);
            lift[0] = f.getY();
        });
        t.repeat(20, x -> {
            FakePlayer f = gear(x);
            suitTick(x.level, f);
            lift[2] = Math.max(lift[2], f.getDeltaMovement().y);
            airMove(f);
        });
        t.then(x -> {
            FakePlayer f = gear(x);
            lift[1] = f.getY() - lift[0];
            long used = fe[0] - suitEnergy(f);
            MobEffectInstance nv = f.getEffect(MobEffects.NIGHT_VISION);
            x.report("07#15 the frontier helmet gives night vision", nv != null && nv.isAmbient() && nv.getDuration() > 200, "effect " + nv);
            x.report("07#15 holding jump in the air flies up (+0.10 per tick, at most 0.5 blocks/tick)", lift[1] > 3.0
                && lift[2] <= FrontierSuitItem.JET_MAX + 1.0E-9, String.format(Locale.ROOT, "rose %.2f blocks, peak %.3f blocks/tick", lift[1], lift[2]));
            x.report("07#15 suit FE over 20 ticks: jet 80, night vision 10, wind resistance 2 per tick", used == 20L * (FrontierSuitItem.JET_COST
                + FrontierSuitItem.NIGHT_VISION_COST + FrontierSuitItem.WIND_COST), used + " FE");
            fresh(x);
        });
        t.then(x -> {
            Zombie plain = mob(x, EntityTypes.ZOMBIE, r.at(26, 0, 214), 20);
            Zombie suited = mob(x, EntityTypes.ZOMBIE, r.at(34, 0, 214), 20);
            plain.setPermanentlyInvulnerable(true);
            suited.setPermanentlyInvulnerable(true);
            ItemStack[] pieces = frontier();
            for (int i = 0; i < ARMOR.length; i++) {
                suited.setItemSlot(ARMOR[i], pieces[i]);
            }
            suitTick(x.level, suited);
            plain.setDeltaMovement(Vec3.ZERO);
            suited.setDeltaMovement(Vec3.ZERO);
            Vec3 c = Vec3.atBottomCenterOf(r.at(30, 0, 214)).add(0, 1, 0);
            x.level.explode(null, c.x, c.y, c.z, 3.0f, Level.ExplosionInteraction.NONE);
            double vp = plain.getDeltaMovement().horizontalDistance();
            double vs = suited.getDeltaMovement().horizontalDistance();
            x.report("07#15 a creeper-strength blast pushes the suited mob half as far (+0.5 explosion knockback resistance)", vp > 0.05
                && vs / vp >= 0.45 && vs / vp <= 0.55, String.format(Locale.ROOT, "plain %.3f, suited %.3f", vp, vs));
            plain.discard();
            suited.discard();
        });
    }

    private static void rail(TestM7.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            TestM7.box(r, 2, 0, 218, 6, 3, 218, Blocks.STONE.defaultBlockState());
            FakePlayer f = fresh(x);
            place(f, r.at(4, 0, 200), true);
            List<Zombie> zs = new ArrayList<>();
            for (int k = 0; k < 6; k++) {
                Zombie z = mob(x, EntityTypes.ZOMBIE, r.at(4, 0, 203 + 2 * k), 100);
                z.setBaby(false);
                zs.add(z);
            }
            ItemStack rifle = charged(AutarkiaItems.RAIL_RIFLE.get(), RailRifleItem.CAPACITY);
            f.setItemInHand(InteractionHand.MAIN_HAND, rifle);
            boolean shot = rifle.getItem().releaseUsing(rifle, x.level, f, rifle.getItem().getUseDuration(rifle, f) - RailRifleItem.FULL_CHARGE);
            float[] d = new float[zs.size()];
            for (int k = 0; k < zs.size(); k++) {
                d[k] = 100 - zs.get(k).getHealth();
            }
            boolean five = true;
            for (int k = 0; k < RailRifleItem.PIERCE; k++) {
                five &= d[k] >= 23.5f && d[k] <= 24.0f;
            }
            int used = RailRifleItem.CAPACITY - PoweredItem.energy(f.getMainHandItem());
            x.report("07#16 a full charge hits the first 5 zombies for 24 (armor 2: 23.6), not the sixth, and uses 20,000 FE", shot && five && d[5] == 0
                && used == RailRifleItem.SHOT_COST && f.getCooldowns().isOnCooldown(f.getMainHandItem()), Arrays.toString(d) + ", " + used + " FE");
            zs.forEach(z -> z.discard());
            TestM7.box(r, 2, 0, 218, 6, 3, 218, Blocks.AIR.defaultBlockState());
        });
    }
}

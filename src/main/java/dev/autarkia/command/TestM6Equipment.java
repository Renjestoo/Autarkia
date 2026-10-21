package dev.autarkia.command;

import dev.autarkia.equipment.PoweredItem;
import dev.autarkia.registry.AutarkiaItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * The equipment part of {@code /autarkia test m6}: 07 #7 (electric drill empty and charged), #8 (chainsaw fells a
 * large spruce at 150 FE per log; sneaking cuts one log), #9 (shock baton +4 damage, Slowness II, 1,000 FE), #10 (exo
 * rig jump, step and fall) and #11 (arc rifle chain over 4 of 5 zombies).
 */
final class TestM6Equipment {
    private TestM6Equipment() {
    }

    static void run(TestM6.Run r) {
        drill(r);
        chainsaw(r);
        baton(r);
        rig(r);
        rifle(r);
    }

    private static ItemStack charged(net.minecraft.world.item.Item item, int fe) {
        ItemStack s = new ItemStack(item);
        PoweredItem.setEnergy(s, fe);
        return s;
    }

    private static void drill(TestM6.Run r) {
        SelfTest t = r.t();
        BlockPos stone = r.at(4, 0, 46);
        t.then(x -> {
            FakePlayer fake = FakePlayerFactory.getMinecraft(x.level);
            ItemStack empty = charged(AutarkiaItems.ELECTRIC_DRILL.get(), 0);
            boolean slow = empty.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 1.0f && !empty.isCorrectToolForDrops(Blocks.STONE.defaultBlockState());
            ItemStack full = charged(AutarkiaItems.ELECTRIC_DRILL.get(), 400000);
            fake.getInventory().clearContent();
            fake.setItemInHand(InteractionHand.MAIN_HAND, full);
            TestM1.place(x, stone, Blocks.STONE.defaultBlockState());
            boolean broke = fake.gameMode.destroyBlock(stone);
            ItemStack held = fake.getMainHandItem();
            int used = 400000 - PoweredItem.energy(held);
            int drops = TestM1.countItems(x, stone, 2, Items.COBBLESTONE);
            x.report("07#7 empty drill: speed 1.0, no drops; charged: breaks stone with a drop for 200 FE", slow && broke && used == 200 && drops == 1,
                "slow " + slow + ", broke " + broke + ", used " + used + ", drops " + drops);
            TestM1.killItems(x, stone, 4);
        });
    }

    private static void chainsaw(TestM6.Run r) {
        SelfTest t = r.t();
        BlockPos base = r.at(12, 0, 46);
        BlockPos small = r.at(20, 0, 46);
        List<BlockPos> logs = new ArrayList<>();
        int[] e = new int[2];
        t.then(x -> {
            for (int y = 0; y < 16; y++) {
                for (int dx = 0; dx < 2; dx++) {
                    for (int dz = 0; dz < 2; dz++) {
                        logs.add(base.offset(dx, y, dz));
                    }
                }
            }
            for (int k = 1; k <= 4; k++) {
                logs.add(base.offset(-k, 14, 0));
                logs.add(base.offset(1 + k, 14, 1));
                logs.add(base.offset(0, 15, -k));
                logs.add(base.offset(1, 15, 1 + k));
            }
            logs.forEach(p -> TestM1.place(x, p, Blocks.SPRUCE_LOG.defaultBlockState()));
            TestM1.place(x, small, Blocks.SPRUCE_LOG.defaultBlockState());
            TestM1.place(x, small.above(), Blocks.SPRUCE_LOG.defaultBlockState());
            FakePlayer fake = FakePlayerFactory.getMinecraft(x.level);
            fake.setItemInHand(InteractionHand.MAIN_HAND, charged(AutarkiaItems.ELECTRIC_CHAINSAW.get(), 400000));
            fake.setShiftKeyDown(false);
            fake.gameMode.destroyBlock(base);
        });
        t.waitUntil("07#8 the chainsaw fells the whole 80-log spruce", 40, () -> logs.stream().allMatch(p -> t.level.getBlockState(p).isAir()), x -> { });
        t.then(x -> {
            FakePlayer fake = FakePlayerFactory.getMinecraft(x.level);
            e[0] = 400000 - PoweredItem.energy(fake.getMainHandItem());
            fake.setShiftKeyDown(true);
            int before = PoweredItem.energy(fake.getMainHandItem());
            fake.gameMode.destroyBlock(small);
            e[1] = before - PoweredItem.energy(fake.getMainHandItem());
            fake.setShiftKeyDown(false);
        });
        t.waitTicks(5);
        t.check("07#8 150 FE per log (80 logs, 12,000 FE); sneaking cuts only one log (150 FE)", () -> e[0] == logs.size() * 150 && e[1] == 150
            && t.level.getBlockState(small).isAir() && !t.level.getBlockState(small.above()).isAir(), () -> e[0] + " FE for " + logs.size() + " logs, sneak " + e[1]);
        t.then(x -> {
            TestM1.killItems(x, base, 24);
            TestM1.place(x, small.above(), Blocks.AIR.defaultBlockState());
        });
    }

    private static Zombie zombie(SelfTest t, BlockPos at) {
        Zombie z = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(t.level, EntitySpawnReason.COMMAND);
        z.setNoAi(true);
        z.setBaby(false);
        z.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180, 0);
        t.level.addFreshEntity(z);
        return z;
    }

    private static void baton(TestM6.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            FakePlayer fake = FakePlayerFactory.getMinecraft(x.level);
            fake.snapTo(r.at(30, 0, 46).getX() + 0.5, r.at(30, 0, 46).getY(), r.at(30, 0, 46).getZ() + 0.5, 0, 0);
            Zombie a = zombie(x, r.at(30, 0, 47));
            Zombie b = zombie(x, r.at(33, 0, 47));
            ItemStack live = charged(AutarkiaItems.SHOCK_BATON.get(), 200000);
            fake.setItemInHand(InteractionHand.MAIN_HAND, live);
            float ha = a.getHealth();
            fake.attack(a);
            float da = ha - a.getHealth();
            int used = 200000 - PoweredItem.energy(fake.getMainHandItem());
            MobEffectInstance slow = a.getEffect(MobEffects.SLOWNESS);
            fake.setItemInHand(InteractionHand.MAIN_HAND, charged(AutarkiaItems.SHOCK_BATON.get(), 0));
            float hb = b.getHealth();
            fake.attack(b);
            float db = hb - b.getHealth();
            x.report("07#9 a charged hit adds 4 shock damage (armor 2), Slowness II for 2 s and uses 1,000 FE", da - db >= 3.5f && da - db <= 4.01f && used == 1000
                && slow != null && slow.getAmplifier() == 1 && slow.getDuration() <= 40 && b.getEffect(MobEffects.SLOWNESS) == null,
                String.format(java.util.Locale.ROOT, "charged %.2f, empty %.2f, used %d, slowness %s", da, db, used, slow));
            a.discard();
            b.discard();
        });
    }

    private static void rig(TestM6.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            FakePlayer fake = FakePlayerFactory.getMinecraft(x.level);
            fake.snapTo(r.at(40, 0, 46).getX() + 0.5, r.at(40, 0, 46).getY(), r.at(40, 0, 46).getZ() + 0.5, 0, 0);
            double jump0 = fake.getAttributeValue(Attributes.JUMP_STRENGTH);
            double step0 = fake.getAttributeValue(Attributes.STEP_HEIGHT);
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            ItemStack[] pieces = {charged(AutarkiaItems.EXO_RIG_HELMET.get(), 500000), charged(AutarkiaItems.EXO_RIG_CHEST.get(), 500000),
                charged(AutarkiaItems.EXO_RIG_LEGS.get(), 500000), charged(AutarkiaItems.EXO_RIG_BOOTS.get(), 500000)};
            for (int i = 0; i < 4; i++) {
                fake.setItemSlot(slots[i], pieces[i]);
            }
            for (int i = 0; i < 4; i++) {
                fake.getItemBySlot(slots[i]).inventoryTick(x.level, fake, slots[i]);
            }
            double jump1 = fake.getAttributeValue(Attributes.JUMP_STRENGTH);
            double step1 = fake.getAttributeValue(Attributes.STEP_HEIGHT);
            int chest0 = PoweredItem.energy(fake.getItemBySlot(EquipmentSlot.CHEST));
            fake.setOnGround(true);
            fake.jumpFromGround();
            double vy = fake.getDeltaMovement().y;
            int jumpCost = chest0 - PoweredItem.energy(fake.getItemBySlot(EquipmentSlot.CHEST));
            int boots0 = PoweredItem.energy(fake.getItemBySlot(EquipmentSlot.FEET));
            boolean hurt = fake.causeFallDamage(20, 1f, x.level.damageSources().fall());
            int fallCost = boots0 - PoweredItem.energy(fake.getItemBySlot(EquipmentSlot.FEET));
            x.report("07#10 exo rig: jump +0.25 for 100 FE, step-up 1.1 blocks, a 20-block fall does no damage for 1,700 FE",
                Math.abs(jump1 - jump0 - 0.25) < 1e-6 && Math.abs(step1 - step0 - 0.5) < 1e-6 && step1 >= 1.0 && vy > 0.6 && jumpCost == 100 && !hurt
                    && fallCost == 1700, String.format(java.util.Locale.ROOT, "jump %.2f->%.2f (vy %.2f, %d FE), step %.2f->%.2f, fall hurt %s for %d FE", jump0,
                    jump1, vy, jumpCost, step0, step1, hurt, fallCost));
            for (EquipmentSlot s : slots) {
                fake.setItemSlot(s, ItemStack.EMPTY);
            }
            for (int i = 0; i < 4; i++) {
                pieces[i].inventoryTick(x.level, fake, slots[i]);
            }
        });
    }

    private static void rifle(TestM6.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            FakePlayer fake = FakePlayerFactory.getMinecraft(x.level);
            BlockPos from = r.at(50, 0, 46);
            fake.snapTo(from.getX() + 0.5, from.getY(), from.getZ() + 0.5, 0, 0);
            List<Zombie> zs = new ArrayList<>();
            for (int k = 0; k < 5; k++) {
                zs.add(zombie(x, from.offset(3 * k, 0, 10)));
            }
            float[] before = new float[5];
            for (int k = 0; k < 5; k++) {
                before[k] = zs.get(k).getHealth();
            }
            ItemStack rifle = charged(AutarkiaItems.ARC_RIFLE.get(), 1000000);
            fake.setItemInHand(InteractionHand.MAIN_HAND, rifle);
            rifle.getItem().use(x.level, fake, InteractionHand.MAIN_HAND);
            float[] d = new float[5];
            for (int k = 0; k < 5; k++) {
                d[k] = before[k] - zs.get(k).getHealth();
            }
            int used = 1000000 - PoweredItem.energy(fake.getMainHandItem());
            boolean chain = d[0] >= 9f && d[0] <= 10f && d[4] == 0;
            for (int k = 1; k < 4; k++) {
                chain &= d[k] / d[k - 1] >= 0.66f && d[k] / d[k - 1] <= 0.74f;
            }
            x.report("07#11 the arc hits 4 of 5 zombies at 10, 7, 4.9, 3.4 (armor 2) and uses 5,000 FE", chain && used == 5000, java.util.Arrays.toString(d) + ", "
                + used + " FE");
            zs.forEach(z -> z.discard());
            x.level.getEntitiesOfClass(ItemEntity.class, new AABB(from).inflate(32)).forEach(i -> i.discard());
        });
    }
}

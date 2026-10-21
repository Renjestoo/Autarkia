package dev.autarkia.command;

import com.mojang.authlib.GameProfile;
import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colony;
import dev.autarkia.energy.DomainPowerIndex;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.PhaseLinks;
import dev.autarkia.energy.SingularityCoreBlockEntity;
import dev.autarkia.equipment.AegisRule;
import dev.autarkia.equipment.AegisTether;
import dev.autarkia.equipment.BastionBlock;
import dev.autarkia.equipment.ForgeOfAscensionBlockEntity;
import dev.autarkia.network.AegisStatusPayload;
import dev.autarkia.registry.AutarkiaAttachments;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.storage.MatterBank;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * The Aegis rule part of {@code /autarkia test m9} (07 #24 to #35) in the region x 240..290, z 110..190 of the T6 test
 * domain, with the walk to x 20, z 170 for #25. One check per checklist test, each with its pass condition and the
 * measured values. Damage goes to a test player that NeoForge's fake player would refuse (it is permanently
 * invulnerable and its death does nothing): {@link Wearer} turns the invulnerability off and runs the real
 * {@code LivingDeathEvent} on death, counting a death that nobody cancels. Grid-empty tests drain the domain grid in the
 * same tick as the hit, because the ONLINE core refills 131,072 FE per tick. The difficulty is set to normal so a zombie
 * hit is the vanilla 3 points. The rigs stay standing; the Wither and the zombies are removed after their tests.
 */
final class TestM9Rule {
    private static final BlockPos SPOT = new BlockPos(260, 0, 120);
    private static final BlockPos FAR = new BlockPos(20, 0, 170);
    private static final BlockPos REFORM = new BlockPos(256, 0, 135);
    private static final BlockPos WALL = new BlockPos(275, 0, 115);
    private static final BlockPos LONE = new BlockPos(285, 0, 115);
    private static final BlockPos CAGE = new BlockPos(282, 0, 150);
    private static final BlockPos FORGE = new BlockPos(246, 0, 165);
    private static final BlockPos TAP = new BlockPos(244, 0, 165);
    private static final BlockPos CORE = new BlockPos(130, 0, 30);
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    private static final Map<String, Wearer> PLAYERS = new LinkedHashMap<>();
    private static final long[] NUM = new long[8];
    private static final List<String> NOTES = new ArrayList<>();
    private static @Nullable Zombie zombie;
    private static @Nullable WitherBoss wither;
    private static @Nullable BlockPos link;

    private TestM9Rule() {
    }

    /** A real-damage test player: a fake player that can be hurt and whose death fires LivingDeathEvent. */
    static final class Wearer extends FakePlayer {
        private final java.util.Map<EquipmentSlot, ItemStack> worn = new java.util.EnumMap<>(EquipmentSlot.class);
        int deaths;

        Wearer(ServerLevel level, GameProfile profile) {
            super(level, profile);
            setPermanentlyInvulnerable(false);
            connection.markClientLoaded();
        }

        @Override
        public void die(DamageSource source) {
            if (CommonHooks.onLivingDeath(this, source)) {
                return;
            }
            deaths++;
            dead = true;
        }

        void wear() {
            EquipmentSlot.VALUES.forEach(s -> worn.putIfAbsent(s, ItemStack.EMPTY));
            java.util.Map<EquipmentSlot, ItemStack> changed = collectEquipmentChanges(worn);
            if (changed != null) {
                changed.forEach((s, st) -> worn.put(s, st.copy()));
            }
        }

        void restore() {
            dead = false;
            damageCooldownTime = 0;
            removeAllEffects();
            clearFire();
            setHealth(getMaxHealth());
            setDeltaMovement(Vec3.ZERO);
        }
    }

    static void run(TestM9.Run r) {
        aegis(r);
        reform(r);
        bastion(r);
        wither(r);
        forge(r);
    }

    private static Wearer player(SelfTest t, String name) {
        Wearer w = PLAYERS.get(name);
        if (w == null || w.level() != t.level) {
            w = new Wearer(t.level, new GameProfile(UUID.nameUUIDFromBytes(("autarkia-m9-" + name).getBytes(StandardCharsets.UTF_8)), "AutarkiaM9" + name));
            PLAYERS.put(name, w);
        }
        return w;
    }

    private static BlockPos at(TestM9.Run r, BlockPos rel) {
        return r.at(rel.getX(), rel.getY(), rel.getZ());
    }

    private static void stand(Wearer p, BlockPos pos) {
        p.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 90, 0);
        p.setOnGround(true);
        p.setDeltaMovement(Vec3.ZERO);
    }

    private static Item[] aegis() {
        return new Item[] {AutarkiaItems.AEGIS_BOOTS.get(), AutarkiaItems.AEGIS_LEGS.get(), AutarkiaItems.AEGIS_CHEST.get(), AutarkiaItems.AEGIS_HELMET.get()};
    }

    private static void suit(Wearer p, boolean aegis) {
        Item[] items = aegis ? aegis() : new Item[] {AutarkiaItems.LATTICE_CARAPACE_BOOTS.get(), AutarkiaItems.LATTICE_CARAPACE_LEGS.get(),
            AutarkiaItems.LATTICE_CARAPACE_CHEST.get(), AutarkiaItems.LATTICE_CARAPACE_HELMET.get()};
        for (int i = 0; i < 4; i++) {
            p.setItemSlot(ARMOR[i], new ItemStack(items[i]));
        }
        p.wear();
    }

    private static Colony colony() {
        Colony c = TestM9.colony();
        if (c == null) {
            throw new IllegalStateException("M9 test colony is not set up; TestM9Rule needs TestM9Colony.setup first");
        }
        return c;
    }

    private static long grid(SelfTest t) {
        return DomainPowerIndex.gridStored(t.level, colony());
    }

    private static void drain(SelfTest t) {
        DomainPowerIndex.pay(t.level, colony(), grid(t));
    }

    private static long matter(SelfTest t) {
        return MatterBank.available(t.level, colony());
    }

    private static Zombie zombie(SelfTest t, BlockPos pos) {
        if (zombie != null) {
            zombie.discard();
        }
        Zombie z = EntityTypes.ZOMBIE.create(t.level, EntitySpawnReason.COMMAND);
        z.setNoAi(true);
        z.setPersistenceRequired();
        z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        z.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, -90, 0);
        t.level.addFreshEntity(z);
        zombie = z;
        return z;
    }

    private static void lethal(SelfTest t, Wearer p) {
        Zombie z = zombie;
        p.damageCooldownTime = 0;
        p.hurtServer(t.level, t.level.damageSources().mobAttack(z), 1000F);
    }

    private static byte state(Wearer p) {
        return AegisRule.status(p).state();
    }

    private static boolean done(ServerPlayer p, SelfTest t, String id) {
        AdvancementHolder h = t.level.getServer().getAdvancements().get(Autarkia.id(id));
        return h != null && p.getAdvancements().getOrStartProgress(h).isDone();
    }

    private static void aegis(TestM9.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            x.command("difficulty normal");
            TestM9Power.refill(x);
            Wearer p = player(x, "Wearer");
            p.restore();
            suit(p, true);
            stand(p, at(r, SPOT));
            zombie(x, at(r, SPOT).east(2));
            java.util.Arrays.fill(NUM, 0);
            NUM[2] = 1;
        });
        for (int hit = 0; hit < 10; hit++) {
            t.waitTicks(12);
            t.then(x -> {
                Wearer p = player(x, "Wearer");
                long before = grid(x);
                float health = p.getHealth();
                p.setDeltaMovement(Vec3.ZERO);
                p.damageCooldownTime = 0;
                zombie.doHurtTarget(x.level, p);
                NUM[0] += before - grid(x);
                NUM[1] += Math.round((health - p.getHealth()) * 1000);
                NUM[3] = Math.max(NUM[3], Math.round(p.getDeltaMovement().lengthSqr() * 1.0E6));
                NUM[2] &= state(p) == AegisStatusPayload.STATE_GREEN ? 1 : 0;
            });
        }
        t.check("07#24 Aegis tether: 10 zombie hits within 64 blocks of a domain node: no damage, no knockback, grid down 10 x 3 x 50,000 FE, HUD green",
            () -> NUM[0] == 1_500_000L && NUM[1] == 0 && NUM[3] == 0 && NUM[2] == 1,
            () -> "grid down " + NUM[0] + " FE (want 1500000), health lost " + NUM[1] / 1000.0 + ", knockback speed^2 x1e6 " + NUM[3] + ", green every hit "
                + (NUM[2] == 1) + ", tether " + (AegisTether.tether(t.level, player(t, "Wearer")) != null));
        t.then(x -> {
            Wearer p = player(x, "Wearer");
            stand(p, at(r, FAR));
            zombie(x, at(r, FAR).east(2));
        });
        t.waitTicks(25);
        t.then(x -> {
            Wearer p = player(x, "Wearer");
            long before = grid(x);
            NUM[4] = state(p);
            p.damageCooldownTime = 0;
            zombie.doHurtTarget(x.level, p);
            NUM[5] = Math.round((20 - p.getHealth()) * 1000);
            NUM[6] = AegisTether.tether(x.level, p) == null ? 1 : 0;
            NUM[7] = before - grid(x);
        });
        t.check("07#25 Aegis tether: 70 blocks from all nodes: HUD red, hits do damage", () -> NUM[4] == AegisStatusPayload.STATE_RED && NUM[5] > 0
                && NUM[6] == 1 && NUM[7] <= 0,
            () -> "state " + NUM[4] + " (red " + AegisStatusPayload.STATE_RED + "), damage taken " + NUM[5] / 1000.0 + ", no tether " + (NUM[6] == 1)
                + ", grid change " + (-NUM[7]));
        t.then(x -> {
            Wearer p = player(x, "Wearer");
            p.restore();
            stand(p, at(r, SPOT));
            zombie(x, at(r, SPOT).east(2));
            drain(x);
            NUM[4] = state(p);
            NUM[7] = grid(x);
            p.damageCooldownTime = 0;
            zombie.doHurtTarget(x.level, p);
            NUM[5] = Math.round((20 - p.getHealth()) * 1000);
        });
        t.check("07#26 Aegis, grid empty: HUD amber or red, the hit does normal damage through armor",
            () -> (NUM[4] == AegisStatusPayload.STATE_AMBER || NUM[4] == AegisStatusPayload.STATE_RED) && NUM[5] > 0 && NUM[5] < 3000,
            () -> "state " + NUM[4] + ", grid " + NUM[7] + " FE, damage taken " + NUM[5] / 1000.0 + " of 3 (armor reduced)");
        t.then(TestM9Power::refill);
    }

    private static void reform(TestM9.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            Wearer p = player(x, "Wearer");
            p.restore();
            p.setData(AutarkiaAttachments.REFORM_READY_AT, 0L);
            stand(p, at(r, REFORM));
            zombie(x, at(r, REFORM).east(1));
            TestM9.freeMatter(x, 10_000);
            Vec3 from = p.position();
            long mu = matter(x);
            drain(x);
            lethal(x, p);
            NUM[0] = mu - matter(x);
            NUM[1] = Math.round(p.getHealth() * 1000);
            NUM[2] = p.getData(AutarkiaAttachments.REFORM_READY_AT) - x.level.getGameTime();
            NUM[3] = Math.round((p.getX() - from.x) * 1000);
            NUM[4] = Math.round((p.getZ() - from.z) * 1000);
            NUM[5] = p.deaths;
            NUM[6] = p.hasEffect(MobEffects.INVISIBILITY) ? 1 : 0;
            lethal(x, p);
            NUM[7] = Math.round(p.getHealth() * 1000);
        });
        t.check("07#27 W24 Nanite Reform: grid empty, matter >= 2,000, lethal hit: forms 3 blocks away at 50% health, 2,000 MU taken, 30 s cooldown",
            () -> NUM[0] == 2000 && NUM[1] == 10_000 && NUM[2] == 600 && NUM[3] == -3000 && NUM[4] == 0 && NUM[5] == 0 && NUM[6] == 1 && NUM[7] == 10_000,
            () -> "MU taken " + NUM[0] + ", health " + NUM[1] / 1000.0 + ", ready in " + NUM[2] + " ticks, moved dx " + NUM[3] / 1000.0 + " dz "
                + NUM[4] / 1000.0 + " (away from the zombie at +x), deaths " + NUM[5] + ", invisible " + (NUM[6] == 1) + ", health after a second lethal hit "
                + NUM[7] / 1000.0 + " (immune)");
        t.waitTicks(45);
        t.then(x -> {
            Wearer p = player(x, "Wearer");
            int deaths = p.deaths;
            long mu = matter(x);
            drain(x);
            lethal(x, p);
            NUM[0] = p.deaths - deaths;
            NUM[1] = mu - matter(x);
            NUM[2] = p.getData(AutarkiaAttachments.REFORM_READY_AT) - x.level.getGameTime();
        });
        t.check("07#28 Reform again within 30 s: normal death, no matter taken", () -> NUM[0] == 1 && NUM[1] == 0 && NUM[2] > 0,
            () -> "deaths +" + NUM[0] + ", MU taken " + NUM[1] + ", cooldown left " + NUM[2] + " ticks");
        t.then(x -> {
            Wearer p = player(x, "Wearer");
            p.restore();
            p.setData(AutarkiaAttachments.REFORM_READY_AT, 0L);
            TestM9.freeMatter(x, 1999);
            int deaths = p.deaths;
            drain(x);
            lethal(x, p);
            NUM[0] = p.deaths - deaths;
            NUM[1] = matter(x);
        });
        t.check("07#29 Reform with matter < 2,000: normal death", () -> NUM[0] == 1 && NUM[1] == 1999,
            () -> "deaths +" + NUM[0] + ", MU left " + NUM[1] + " (1999)");
        t.then(x -> {
            TestM9Power.refill(x);
            TestM9.freeMatter(x, 10_000);
            Wearer p = player(x, "Wearer");
            p.restore();
            p.setData(AutarkiaAttachments.REFORM_READY_AT, 0L);
            int deaths = p.deaths;
            long g = grid(x);
            p.kill(x.level);
            long g2 = grid(x);
            NUM[0] = p.deaths - deaths;
            NUM[1] = g - g2;
            p.restore();
            p.setHealth(2);
            p.hurtServer(x.level, x.level.damageSources().fellOutOfWorld(), 4F);
            NUM[2] = p.deaths - deaths;
            NUM[3] = g2 - grid(x);
            NUM[4] = 10_000 - matter(x);
            NUM[5] = p.getData(AutarkiaAttachments.REFORM_READY_AT);
            p.restore();
            if (zombie != null) {
                zombie.discard();
                zombie = null;
            }
            TestM9.freeMatter(x, 1_000_000);
        });
        t.check("07#30 /kill and the void: death in both cases, no FE or matter spent",
            () -> NUM[0] == 1 && NUM[2] == 2 && NUM[1] == 0 && NUM[3] == 0 && NUM[4] == 0 && NUM[5] == 0,
            () -> "deaths after /kill " + NUM[0] + ", after the void " + NUM[2] + ", FE spent " + NUM[1] + " + " + NUM[3] + ", MU spent " + NUM[4]
                + ", reform cooldown set " + (NUM[5] != 0));
    }

    private static List<BlockPos> wall(TestM9.Run r) {
        List<BlockPos> out = new ArrayList<>();
        for (int dx = 0; dx < 5; dx++) {
            for (int y = 0; y < 3; y++) {
                out.add(at(r, WALL).offset(dx, y, 0));
            }
        }
        return out;
    }

    private static @Nullable SingularityCoreBlockEntity core(TestM9.Run r) {
        return r.t().level.getBlockEntity(at(r, CORE)) instanceof SingularityCoreBlockEntity c ? c : null;
    }

    private static boolean intact(SelfTest t, List<BlockPos> cells) {
        return cells.stream().allMatch(p -> BastionBlock.bastion(t.level.getBlockState(p)));
    }

    private static void bastion(TestM9.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            TestM9Power.refill(x);
            List<BlockPos> w = wall(r);
            for (int i = 0; i < w.size(); i++) {
                TestM1.place(x, w.get(i), (i == 7 ? AutarkiaBlocks.BASTION_GLASS : AutarkiaBlocks.BASTION_BLOCK).get().defaultBlockState());
            }
        });
        t.waitTicks(25);
        t.then(x -> {
            List<BlockPos> w = wall(r);
            FakePlayer miner = FakePlayerFactory.get(x.level, new GameProfile(UUID.nameUUIDFromBytes("autarkia-m9-miner".getBytes(StandardCharsets.UTF_8)),
                "AutarkiaM9Miner"));
            miner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
            float a = x.level.getBlockState(w.get(0)).getDestroyProgress(miner, x.level, w.get(0));
            float b = x.level.getBlockState(w.get(7)).getDestroyProgress(miner, x.level, w.get(7));
            NUM[0] = a == 0 && b == 0 ? 1 : 0;
            SingularityCoreBlockEntity c = core(r);
            NUM[1] = c == null ? -1 : c.explosions();
            BlockPos tnt = at(r, WALL).offset(2, 0, 2);
            x.command("summon minecraft:tnt " + tnt.getX() + " " + tnt.getY() + " " + tnt.getZ() + " {fuse:1s}");
        });
        t.waitTicks(10);
        t.then(x -> {
            SingularityCoreBlockEntity c = core(r);
            NUM[2] = c == null ? -1 : c.lastGain();
            BlockPos cr = at(r, WALL).offset(2, 0, 3);
            x.command("summon minecraft:creeper " + cr.getX() + " " + cr.getY() + " " + cr.getZ() + " {powered:1b,ignited:1b,Fuse:1s}");
        });
        t.waitTicks(15);
        t.check("07#31 Bastion, grid powered: a netherite pickaxe makes no progress, TNT and a charged creeper break nothing, the core gains FE per explosion",
            () -> {
                SingularityCoreBlockEntity c = core(r);
                return NUM[0] == 1 && intact(t, wall(r)) && c != null && c.explosions() - NUM[1] == 2 && NUM[2] == 800_000 && c.lastGain() == 1_800_000;
            },
            () -> {
                SingularityCoreBlockEntity c = core(r);
                return "pickaxe progress 0 " + (NUM[0] == 1) + ", wall intact " + intact(t, wall(r)) + ", explosions absorbed "
                    + (c == null ? "no core" : c.explosions() - NUM[1]) + ", TNT gain " + NUM[2] + " (800000), creeper gain " + (c == null ? -1 : c.lastGain())
                    + " (1800000)";
            });
        t.then(x -> {
            BlockPos p = at(r, LONE);
            TestM1.place(x, p, AutarkiaBlocks.BASTION_BLOCK.get().defaultBlockState());
            FakePlayer miner = FakePlayerFactory.get(x.level, new GameProfile(UUID.nameUUIDFromBytes("autarkia-m9-miner".getBytes(StandardCharsets.UTF_8)),
                "AutarkiaM9Miner"));
            miner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            miner.snapTo(p.getX() + 0.5, p.getY() + 1, p.getZ() - 1.5, 0, 0);
            miner.setOnGround(true);
            drain(x);
            BlockState st = x.level.getBlockState(p);
            NUM[0] = Math.round(st.getDestroyProgress(miner, x.level, p) * 1.0E6);
            NUM[1] = Math.round(st.getExplosionResistance(x.level, p, null));
            NUM[3] = grid(x);
            miner.gameMode.destroyBlock(p);
            NUM[2] = x.level.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(2)).stream()
                .filter(e -> e.getItem().is(AutarkiaItems.BASTION_BLOCK.get())).mapToInt(e -> e.getItem().getCount()).sum();
            NUM[4] = x.level.getBlockState(p).isAir() ? 1 : 0;
            TestM9Power.refill(x);
        });
        t.check("07#32 Bastion, grid below 1,000,000 FE: breaks like obsidian with a diamond pickaxe",
            () -> NUM[0] == Math.round(8.0 / 50.0 / 30.0 * 1.0E6) && NUM[1] == 1200 && NUM[4] == 1 && NUM[2] == 1,
            () -> "grid " + NUM[3] + " FE, progress per tick " + NUM[0] / 1.0E6 + " (8 / 50 / 30), resistance " + NUM[1] + ", broken " + (NUM[4] == 1)
                + ", drops " + NUM[2]);
    }

    private static List<BlockPos> cage(TestM9.Run r) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos c = at(r, CAGE);
        for (int y = 0; y <= 4; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (y == 4 || dx != 0 || dz != 0) {
                        out.add(c.offset(dx, y, dz));
                    }
                }
            }
        }
        return out;
    }

    private static void wither(TestM9.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            TestM9Power.refill(x);
            List<BlockPos> cells = cage(r);
            for (int i = 0; i < cells.size(); i++) {
                TestM1.place(x, cells.get(i), (i % 2 == 0 ? AutarkiaBlocks.BASTION_BLOCK : AutarkiaBlocks.BASTION_GLASS).get().defaultBlockState());
            }
            if (x.player != null) {
                AdvancementHolder h = x.level.getServer().getAdvancements().get(Autarkia.id("equipment/unbroken"));
                if (h != null) {
                    x.player.getAdvancements().revoke(h, "done");
                }
            }
            SingularityCoreBlockEntity c = core(r);
            NUM[0] = c == null ? -1 : c.explosions();
            BlockPos at = at(r, CAGE);
            WitherBoss w = EntityTypes.WITHER.create(x.level, EntitySpawnReason.COMMAND);
            w.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            w.setPersistenceRequired();
            w.makeInvulnerable();
            x.level.addFreshEntity(w);
            wither = w;
        });
        t.waitTicks(230);
        t.then(x -> {
            if (wither != null) {
                wither.hurtServer(x.level, x.level.damageSources().generic(), 1F);
            }
        });
        t.waitTicks(30);
        t.check("07#33 W23 Wither next to Bastion walls, grid powered: no domain block breaks, the spawn blast is absorbed, advancement equipment/unbroken",
            () -> {
                SingularityCoreBlockEntity c = core(r);
                boolean floor = !t.level.getBlockState(at(r, CAGE).below()).isAir();
                return intact(t, cage(r)) && floor && c != null && c.explosions() > NUM[0] && (t.player == null || done(t.player, t, "equipment/unbroken"));
            },
            () -> {
                SingularityCoreBlockEntity c = core(r);
                return "cage intact " + intact(t, cage(r)) + " (" + cage(r).size() + " cells), floor " + !t.level.getBlockState(at(r, CAGE).below()).isAir()
                    + ", explosions absorbed " + (c == null ? "no core" : c.explosions() - NUM[0]) + ", wither alive " + (wither != null && wither.isAlive())
                    + ", unbroken " + (t.player == null ? "no player" : done(t.player, t, "equipment/unbroken"));
            });
        t.then(x -> {
            if (wither != null) {
                wither.discard();
                wither = null;
            }
        });
    }

    private static @Nullable ForgeOfAscensionBlockEntity forgeAt(TestM9.Run r) {
        return r.t().level.getBlockEntity(at(r, FORGE)) instanceof ForgeOfAscensionBlockEntity f ? f : null;
    }

    private static void load(ForgeOfAscensionBlockEntity f) {
        for (int i = 0; i < f.inventory().size(); i++) {
            f.inventory().put(i, ItemStack.EMPTY);
        }
        f.inventory().put(0, new ItemStack(AutarkiaItems.AEGIS_PLATE.get(), 24));
        f.inventory().put(1, new ItemStack(AutarkiaItems.AI_CORE.get(), 4));
        f.inventory().put(2, new ItemStack(AutarkiaItems.NANITE_PASTE.get(), 22));
    }

    private static int count(ForgeOfAscensionBlockEntity f, Item item) {
        int n = 0;
        for (int i = 0; i < f.inventory().size(); i++) {
            if (f.inventory().get(i).is(item)) {
                n += f.inventory().get(i).getCount();
            }
        }
        return n;
    }

    private static InteractionResult sneakUse(SelfTest t, TestM9.Run r, Wearer p) {
        BlockPos c = at(r, FORGE);
        p.setShiftKeyDown(true);
        InteractionResult res = t.level.getBlockState(c).useWithoutItem(t.level, p, new BlockHitResult(Vec3.atCenterOf(c).add(0, 0.5, 0), Direction.UP, c, false));
        p.setShiftKeyDown(false);
        return res;
    }

    private static void forge(TestM9.Run r) {
        SelfTest t = r.t();
        Map<EquipmentSlot, Integer> on = new LinkedHashMap<>();
        t.then(x -> {
            TestM9Power.refill(x);
            boolean ok = TestM1.placeBig(x, at(r, FORGE), AutarkiaBlocks.FORGE_OF_ASCENSION.get(), Direction.NORTH);
            TestM1.place(x, at(r, TAP), AutarkiaBlocks.PHASE_NODE.get().defaultBlockState());
            NOTES.clear();
            NOTES.add("forge placed " + ok);
        });
        t.waitTicks(2);
        t.then(x -> {
            link = null;
            for (int z : new int[] {160, 190, 120}) {
                BlockPos n = r.at(200, TestM9Power.CHAIN_Y, z);
                Grid.SpanResult res = PhaseLinks.link(x.level, null, at(r, TAP), n);
                NOTES.add("link to z " + z + ": " + res);
                if (res == Grid.SpanResult.OK) {
                    link = n;
                    break;
                }
            }
        });
        t.waitTicks(5);
        t.then(x -> {
            ForgeOfAscensionBlockEntity f = forgeAt(r);
            Wearer p = player(x, "Fitter");
            p.restore();
            suit(p, false);
            if (f != null) {
                load(f);
                Vec3 pad = f.pad();
                p.snapTo(pad.x, pad.y, pad.z, 0, 0);
            }
            on.clear();
            NUM[0] = sneakUse(x, r, p).consumesAction() ? 1 : 0;
            NUM[1] = f == null ? -1 : f.status().ordinal();
            NUM[2] = 0;
            zombie(x, at(r, FORGE).south(3));
        });
        t.repeat(605, x -> {
            ForgeOfAscensionBlockEntity f = forgeAt(r);
            Wearer p = player(x, "Fitter");
            if (f == null) {
                return;
            }
            for (int i = 0; i < 4; i++) {
                EquipmentSlot s = ARMOR[i];
                if (!on.containsKey(s) && p.getItemBySlot(s).is(aegis()[i])) {
                    on.put(s, f.status() == ForgeOfAscensionBlockEntity.Status.FITTING ? f.tick() - 1 : -1);
                }
            }
            if (f.status() == ForgeOfAscensionBlockEntity.Status.FITTING && f.tick() == 300) {
                float h = p.getHealth();
                p.hurtServer(x.level, x.level.damageSources().mobAttack(zombie), 1000F);
                NUM[2] = Math.round((h - p.getHealth()) * 1000) + (p.deaths > 0 ? 100_000 : 0);
            }
        });
        t.check("07#34 W22 fitting: 30 s sequence, 4 pieces fitted in order at ticks 150, 270, 410, 510, 50,000,000 FE, no damage while locked",
            () -> {
                ForgeOfAscensionBlockEntity f = forgeAt(r);
                Wearer p = player(t, "Fitter");
                return f != null && NUM[0] == 1 && NUM[1] == ForgeOfAscensionBlockEntity.Status.FITTING.ordinal() && f.status() == ForgeOfAscensionBlockEntity.Status.DONE
                    && on.equals(Map.of(EquipmentSlot.FEET, 150, EquipmentSlot.LEGS, 270, EquipmentSlot.CHEST, 410, EquipmentSlot.HEAD, 510))
                    && f.drawn() == 50_000_000L && f.fitted() == 4 && AegisRule.fullSet(p) && !ForgeOfAscensionBlockEntity.locked(p) && NUM[2] == 0
                    && count(f, AutarkiaItems.AEGIS_PLATE.get()) == 0;
            },
            () -> {
                ForgeOfAscensionBlockEntity f = forgeAt(r);
                return NOTES + ", started " + (NUM[0] == 1) + ", status " + (f == null ? "no forge" : f.status()) + ", pieces on at " + on + ", FE drawn "
                    + (f == null ? -1 : f.drawn()) + ", fitted " + (f == null ? -1 : f.fitted()) + ", full set " + AegisRule.fullSet(player(t, "Fitter"))
                    + ", locked " + ForgeOfAscensionBlockEntity.locked(player(t, "Fitter")) + ", damage while locked " + NUM[2] / 1000.0
                    + ", plates left " + (f == null ? -1 : count(f, AutarkiaItems.AEGIS_PLATE.get())) + "; shakes, arm triggers and poses go to clients"
                    + " and are not observable on the server";
            });
        abort(r);
    }

    private static void abort(TestM9.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            TestM9Power.refill(x);
            ForgeOfAscensionBlockEntity f = forgeAt(r);
            Wearer p = player(x, "Fitter");
            p.restore();
            suit(p, false);
            if (f != null) {
                load(f);
                Vec3 pad = f.pad();
                p.snapTo(pad.x, pad.y, pad.z, 0, 0);
            }
            NUM[0] = sneakUse(x, r, p).consumesAction() ? 1 : 0;
        });
        t.waitUntil("07#35 setup: the second fitting reaches tick 300 (step 4, chest)", 400, () -> {
            ForgeOfAscensionBlockEntity f = forgeAt(r);
            return f != null && f.status() == ForgeOfAscensionBlockEntity.Status.FITTING && f.tick() >= 300;
        }, x -> {
        });
        t.then(x -> {
            if (link != null) {
                Grid.removeSpan(x.level, at(r, TAP), link);
            }
        });
        t.waitUntil("07#35 the forge aborts after the power cut", 60, () -> {
            ForgeOfAscensionBlockEntity f = forgeAt(r);
            return f != null && f.status() == ForgeOfAscensionBlockEntity.Status.ABORTED;
        }, x -> {
        });
        t.check("07#35 W22 abort at step 4: boots and legs stay Aegis, the chest returns to carapace, chest inputs not used, player unlocked",
            () -> {
                ForgeOfAscensionBlockEntity f = forgeAt(r);
                Wearer p = player(t, "Fitter");
                return f != null && link != null && p.getItemBySlot(EquipmentSlot.FEET).is(AutarkiaItems.AEGIS_BOOTS.get())
                    && p.getItemBySlot(EquipmentSlot.LEGS).is(AutarkiaItems.AEGIS_LEGS.get())
                    && p.getItemBySlot(EquipmentSlot.CHEST).is(AutarkiaItems.LATTICE_CARAPACE_CHEST.get())
                    && p.getItemBySlot(EquipmentSlot.HEAD).is(AutarkiaItems.LATTICE_CARAPACE_HELMET.get())
                    && count(f, AutarkiaItems.AEGIS_PLATE.get()) == 13 && count(f, AutarkiaItems.AI_CORE.get()) == 2
                    && count(f, AutarkiaItems.NANITE_PASTE.get()) == 12 && !ForgeOfAscensionBlockEntity.locked(p);
            },
            () -> {
                ForgeOfAscensionBlockEntity f = forgeAt(r);
                Wearer p = player(t, "Fitter");
                return "link cut " + link + ", aborted at tick " + (f == null ? -1 : f.tick()) + ", armor " + p.getItemBySlot(EquipmentSlot.FEET).getItem()
                    + " / " + p.getItemBySlot(EquipmentSlot.LEGS).getItem() + " / " + p.getItemBySlot(EquipmentSlot.CHEST).getItem() + " / "
                    + p.getItemBySlot(EquipmentSlot.HEAD).getItem() + ", left in forge plates " + (f == null ? -1 : count(f, AutarkiaItems.AEGIS_PLATE.get()))
                    + " (13) cores " + (f == null ? -1 : count(f, AutarkiaItems.AI_CORE.get())) + " (2) paste "
                    + (f == null ? -1 : count(f, AutarkiaItems.NANITE_PASTE.get())) + " (12), locked " + ForgeOfAscensionBlockEntity.locked(p);
            });
        t.then(x -> {
            if (zombie != null) {
                zombie.discard();
                zombie = null;
            }
        });
    }
}

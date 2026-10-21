package dev.autarkia.command;

import com.mojang.authlib.GameProfile;
import dev.autarkia.Autarkia;
import dev.autarkia.equipment.AutarkiaDamage;
import dev.autarkia.equipment.AutarkiaMaterials;
import dev.autarkia.equipment.DisassemblerBeamItem;
import dev.autarkia.equipment.FrontierSuitItem;
import dev.autarkia.equipment.LatticeBladeItem;
import dev.autarkia.equipment.LatticeCarapaceItem;
import dev.autarkia.equipment.MatterLink;
import dev.autarkia.equipment.NaniteMultitoolItem;
import dev.autarkia.equipment.PoweredItem;
import dev.autarkia.equipment.SuitItem;
import dev.autarkia.equipment.ToolMode;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.storage.MatterBank;
import dev.autarkia.storage.MatterBufferBlockEntity;
import dev.autarkia.storage.MatterValues;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;

/**
 * The equipment part of {@code /autarkia test m8} (07 #17 to #20 plus the item numbers of 07 section 1.6) on the
 * platform region x 250..330, z 0..60: the linked nanite multitool mining 64 stone 50 blocks from a matter buffer and
 * streaming the cache at 20 blocks, its block transformers, the {@code equipment/ghost_matter} advancement on a real
 * unconnected player, lattice blade armor piercing against diamond armor, carapace self-repair, and the disassembler
 * on a hill with a chest. The level does not tick fake players, so the script runs their inventory, suit, use and
 * cooldown ticks. Melee hits run the damage and post-hit steps of a player attack directly (fake players never charge
 * their attack strength) and clear the target's damage cooldown first. A placed block entity joins its level
 * registries in {@code onLoad}, which NeoForge runs on the next block entity tick, so lookups wait for it.
 */
final class TestM8Equipment {
    private static final UUID FAKE = UUID.nameUUIDFromBytes("autarkia-m8-equipment".getBytes(StandardCharsets.UTF_8));
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final BlockPos BUFFER = new BlockPos(256, 0, 10);
    private static final BlockPos HILL = new BlockPos(300, 0, 40);

    private TestM8Equipment() {
    }

    static void run(TestM8.Run r) {
        data(r);
        stats(r);
        multitool(r);
        transforms(r);
        ghostMatter(r);
        blade(r);
        carapace(r);
        disassembler(r);
    }

    private static FakePlayer gear(SelfTest t) {
        return FakePlayerFactory.get(t.level, new GameProfile(FAKE, "AutarkiaM8Gear"));
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
        f.getCooldowns().removeCooldown(f.getCooldowns().getCooldownGroup(new ItemStack(AutarkiaItems.DISASSEMBLER_BEAM.get())));
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

    private static BlockPos at(TestM8.Run r, BlockPos rel) {
        return r.at(rel.getX(), rel.getY(), rel.getZ());
    }

    /** Place a big block by code (09 R2) on its center, facing north when the block has a facing. */
    private static boolean placeCenter(SelfTest t, BlockPos center, Block block) {
        BlockState st = block.defaultBlockState().setValue(BaseMultiblock.CENTER, true);
        if (st.hasProperty(HorizontalDirectionalBlock.FACING)) {
            st = st.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        }
        if (!((BaseMultiblock) block).canPlace(t.level, center, st, null, false)) {
            return false;
        }
        t.level.setBlock(center, st, Block.UPDATE_ALL);
        return true;
    }

    private static <T extends Mob> T mob(SelfTest t, net.minecraft.world.entity.EntityType<T> type, BlockPos at, double health) {
        T m = type.create(t.level, EntitySpawnReason.COMMAND);
        m.setNoAi(true);
        m.setPersistenceRequired();
        m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180, 0);
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        m.setHealth((float) health);
        t.level.addFreshEntity(m);
        return m;
    }

    private static double attribute(ItemStack s, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a, double base,
                                    EquipmentSlot slot) {
        return s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).compute(a, base, slot);
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-4;
    }

    private static void useTick(FakePlayer f) {
        ItemStack s = f.getUseItem();
        if (!s.isEmpty()) {
            s.getItem().onUseTick(f.level(), f, s, f.getUseItemRemainingTicks());
        }
    }

    private static int itemsAround(SelfTest t, BlockPos center, double r) {
        return t.level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(r)).stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static void data(TestM8.Run r) {
        SelfTest t = r.t();
        t.check("07 data: equipment recipes 37 to 43, advancement equipment/ghost_matter, damage type autarkia:beam, repair and immune tags", () -> {
            var map = t.level.recipeAccess().recipeMap();
            for (String id : List.of("nanite_multitool", "lattice_blade", "lattice_carapace_helmet", "lattice_carapace_chest", "lattice_carapace_legs",
                "lattice_carapace_boots", "disassembler_beam")) {
                if (map.byKey(ResourceKey.create(Registries.RECIPE, Autarkia.id("fabricating/" + id))) == null) {
                    Autarkia.LOG.info("[TEST m8] missing recipe fabricating/{}", id);
                    return false;
                }
            }
            return t.level.getServer().getAdvancements().get(Autarkia.id("equipment/ghost_matter")) != null
                && t.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(AutarkiaDamage.BEAM).isPresent()
                && new ItemStack(AutarkiaItems.LATTICE_ALLOY_INGOT.get()).is(AutarkiaMaterials.REPAIRS_LATTICE)
                && t.level.registryAccess().lookupOrThrow(Registries.BLOCK).get(DisassemblerBeamItem.IMMUNE).isPresent();
        }, () -> "7 recipes, 1 advancement, 1 damage type, 2 tags");
    }

    private static void stats(TestM8.Run r) {
        SelfTest t = r.t();
        t.check("07 1.6 attack values: multitool 9.0 / 1.2, lattice blade 12.0 / 1.6, disassembler 1.0 / 4.0", () -> {
            ItemStack m = new ItemStack(AutarkiaItems.NANITE_MULTITOOL.get());
            ItemStack b = new ItemStack(AutarkiaItems.LATTICE_BLADE.get());
            ItemStack d = new ItemStack(AutarkiaItems.DISASSEMBLER_BEAM.get());
            return near(attribute(m, Attributes.ATTACK_DAMAGE, 1, EquipmentSlot.MAINHAND), 9.0) && near(attribute(m, Attributes.ATTACK_SPEED, 4, EquipmentSlot.MAINHAND), 1.2)
                && near(attribute(b, Attributes.ATTACK_DAMAGE, 1, EquipmentSlot.MAINHAND), 12.0) && near(attribute(b, Attributes.ATTACK_SPEED, 4, EquipmentSlot.MAINHAND), 1.6)
                && near(attribute(d, Attributes.ATTACK_DAMAGE, 1, EquipmentSlot.MAINHAND), 1.0) && near(attribute(d, Attributes.ATTACK_SPEED, 4, EquipmentSlot.MAINHAND), 4.0);
        }, () -> String.format(Locale.ROOT, "multitool %.2f / %.2f, blade %.2f / %.2f",
            attribute(new ItemStack(AutarkiaItems.NANITE_MULTITOOL.get()), Attributes.ATTACK_DAMAGE, 1, EquipmentSlot.MAINHAND),
            attribute(new ItemStack(AutarkiaItems.NANITE_MULTITOOL.get()), Attributes.ATTACK_SPEED, 4, EquipmentSlot.MAINHAND),
            attribute(new ItemStack(AutarkiaItems.LATTICE_BLADE.get()), Attributes.ATTACK_DAMAGE, 1, EquipmentSlot.MAINHAND),
            attribute(new ItemStack(AutarkiaItems.LATTICE_BLADE.get()), Attributes.ATTACK_SPEED, 4, EquipmentSlot.MAINHAND)));
        t.check("07 1.6 carapace: durability 550/800/750/650, armor 4/9/7/4, toughness 4, knockback resistance 0.15, 5,000,000 FE at 20,000 FE/t", () -> {
            List<Item> pieces = List.of(AutarkiaItems.LATTICE_CARAPACE_HELMET.get(), AutarkiaItems.LATTICE_CARAPACE_CHEST.get(),
                AutarkiaItems.LATTICE_CARAPACE_LEGS.get(), AutarkiaItems.LATTICE_CARAPACE_BOOTS.get());
            int[] dur = {550, 800, 750, 650};
            int[] def = {4, 9, 7, 4};
            for (int i = 0; i < 4; i++) {
                ItemStack s = new ItemStack(pieces.get(i));
                SuitItem suit = (SuitItem) pieces.get(i);
                if (s.getMaxDamage() != dur[i] || !near(attribute(s, Attributes.ARMOR, 0, ARMOR[i]), def[i]) || !near(attribute(s, Attributes.ARMOR_TOUGHNESS, 0, ARMOR[i]), 4)
                    || !near(attribute(s, Attributes.KNOCKBACK_RESISTANCE, 0, ARMOR[i]), 0.15) || suit.capacity() != LatticeCarapaceItem.CAPACITY
                    || suit.input() != LatticeCarapaceItem.INPUT) {
                    return false;
                }
            }
            return true;
        }, () -> "4 pieces");
        t.check("07 1.6 multitool: speed 24 on pickaxe, axe, shovel, hoe, sword and shears blocks; netherite drops; speed 1 and no drops at 0 FE", () -> {
            ItemStack m = charged(AutarkiaItems.NANITE_MULTITOOL.get(), NaniteMultitoolItem.CAPACITY);
            ItemStack empty = charged(AutarkiaItems.NANITE_MULTITOOL.get(), 0);
            for (BlockState s : List.of(Blocks.STONE.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), Blocks.DIRT.defaultBlockState(),
                Blocks.HAY_BLOCK.defaultBlockState(), Blocks.BAMBOO_BLOCK.defaultBlockState(), Blocks.COBWEB.defaultBlockState(),
                Blocks.WOOL.white().defaultBlockState(), Blocks.VINE.defaultBlockState(), Blocks.OAK_LEAVES.defaultBlockState())) {
                if (m.getDestroySpeed(s) != 24f || empty.getDestroySpeed(s) != 1f) {
                    Autarkia.LOG.info("[TEST m8] multitool speed on {}: {}", s, m.getDestroySpeed(s));
                    return false;
                }
            }
            return m.isCorrectToolForDrops(Blocks.ANCIENT_DEBRIS.defaultBlockState()) && m.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())
                && !empty.isCorrectToolForDrops(Blocks.STONE.defaultBlockState());
        }, () -> "9 block kinds");
        t.check("07 G12 the 3x3 mode mines at x0.5 speed", () -> {
            FakePlayer f = fresh(t);
            ItemStack m = charged(AutarkiaItems.NANITE_MULTITOOL.get(), NaniteMultitoolItem.CAPACITY);
            f.setItemInHand(InteractionHand.MAIN_HAND, m);
            float one = f.getDestroySpeed(Blocks.STONE.defaultBlockState(), f.blockPosition());
            m.set(AutarkiaComponents.TOOL_MODE.get(), ToolMode.AREA3);
            float three = f.getDestroySpeed(Blocks.STONE.defaultBlockState(), f.blockPosition());
            fresh(t);
            return one > 0 && near(three / one, NaniteMultitoolItem.AREA_SPEED);
        }, () -> "ratio");
    }

    private static MatterBufferBlockEntity buffer(SelfTest t, BlockPos center) {
        return MatterBank.nearest(t.level, center, 2);
    }

    private static void multitool(TestM8.Run r) {
        SelfTest t = r.t();
        BlockPos buf = at(r, BUFFER);
        List<BlockPos> stone = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(r.at(304, 0, 8), r.at(307, 3, 11))) {
            stone.add(p.immutable());
        }
        long[] free = new long[2];
        long[] flow = new long[2];
        int[] fe = new int[2];
        boolean[] placed = new boolean[1];
        t.then(x -> placed[0] = placeCenter(x, buf, AutarkiaBlocks.MATTER_BUFFER.get()));
        t.waitTicks(2);
        t.then(x -> {
            MatterBufferBlockEntity b = buffer(x, buf);
            FakePlayer f = fresh(x);
            ItemStack tool = charged(AutarkiaItems.NANITE_MULTITOOL.get(), NaniteMultitoolItem.CAPACITY);
            f.setItemInHand(InteractionHand.MAIN_HAND, tool);
            place(f, buf.offset(3, 0, 0), true);
            f.setShiftKeyDown(true);
            InteractionResult res = tool.useOn(new UseOnContext(f, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(buf), Direction.UP, buf, false)));
            f.setShiftKeyDown(false);
            GlobalPos link = tool.get(AutarkiaComponents.MATTER_LINK.get());
            x.report("07#17 sneak + use on a matter buffer links the multitool to the buffer center", placed[0] && b != null && res.consumesAction()
                && link != null && link.equals(GlobalPos.of(x.level.dimension(), b.getBlockPos())), "placed " + placed[0] + ", " + res + ", link " + link);
            free[0] = b == null ? 0 : b.free();
            TestM8.box(r, 304, 0, 8, 307, 3, 11, Blocks.STONE.defaultBlockState());
        });
        t.then(x -> {
            FakePlayer f = gear(x);
            place(f, r.at(306, 0, 13), true);
            ItemStack tool = f.getMainHandItem();
            fe[0] = PoweredItem.energy(tool);
            for (BlockPos p : stone) {
                f.gameMode.destroyBlock(p);
            }
            int used = fe[0] - PoweredItem.energy(tool);
            long left = stone.stream().filter(p -> !x.level.getBlockState(p).isAir()).count();
            int items = itemsAround(x, r.at(305, 1, 9), 6);
            long unit = MatterValues.of(new ItemStack(Items.COBBLESTONE), 5);
            x.report("07#17 mining 64 stone 50 blocks from the buffer fills the cache with 64 MU and drops nothing", left == 0 && items == 0 && unit == 1
                && MatterLink.cache(tool) == 64, "left " + left + ", items on the ground " + items + ", cobblestone " + unit + " MU, cache " + MatterLink.cache(tool));
            x.report("07#17 the 64 blocks cost 64 x 100 + 64 x 20 = 7,680 FE", used == 64 * NaniteMultitoolItem.BLOCK_COST + 64 * MatterLink.FE_PER_ITEM,
                used + " FE");
        });
        t.repeat(20, x -> {
            FakePlayer f = gear(x);
            f.getMainHandItem().inventoryTick(x.level, f, EquipmentSlot.MAINHAND);
        });
        t.check("07#17 50 blocks from the buffer (outside tools.matterStreamRange 32) the cache stays", () -> MatterLink.cache(gear(t).getMainHandItem()) == 64,
            () -> "cache " + MatterLink.cache(gear(t).getMainHandItem()));
        t.then(x -> {
            place(gear(x), r.at(276, 0, 10), true);
            flow[0] = x.level.getGameTime();
            flow[1] = -1;
        });
        t.waitUntil("07#17 20 blocks from the buffer the cache streams into it", 60, () -> MatterLink.cache(gear(t).getMainHandItem()) == 0, x -> {
            FakePlayer f = gear(x);
            f.getMainHandItem().inventoryTick(x.level, f, EquipmentSlot.MAINHAND);
            if (flow[1] < 0 && MatterLink.cache(f.getMainHandItem()) == 0) {
                flow[1] = x.level.getGameTime();
            }
        });
        t.check("07#17 the buffer gained 64 free MU at 64 MU per second (15 to 20 ticks)", () -> {
            MatterBufferBlockEntity b = buffer(t, buf);
            free[1] = b == null ? -1 : b.free();
            long ticks = flow[1] - flow[0];
            return free[1] - free[0] == 64 && ticks >= 15 && ticks <= 20;
        }, () -> "free " + free[0] + " -> " + free[1] + ", " + (flow[1] - flow[0]) + " ticks");
    }

    private static void transforms(TestM8.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            BlockPos log = r.at(262, 0, 20);
            BlockPos grass = r.at(264, 0, 20);
            BlockPos dirt = r.at(266, 0, 20);
            BlockPos dead = r.at(268, 0, 20);
            TestM1.place(x, log, Blocks.OAK_LOG.defaultBlockState());
            TestM1.place(x, grass, Blocks.GRASS_BLOCK.defaultBlockState());
            TestM1.place(x, dirt, Blocks.DIRT.defaultBlockState());
            TestM1.place(x, dead, Blocks.OAK_LOG.defaultBlockState());
            FakePlayer f = fresh(x);
            place(f, r.at(265, 0, 18), true);
            ItemStack tool = charged(AutarkiaItems.NANITE_MULTITOOL.get(), NaniteMultitoolItem.CAPACITY);
            f.setItemInHand(InteractionHand.MAIN_HAND, tool);
            tool.useOn(new UseOnContext(f, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(log), Direction.UP, log, false)));
            tool.useOn(new UseOnContext(f, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(grass), Direction.UP, grass, false)));
            f.setShiftKeyDown(true);
            tool.useOn(new UseOnContext(f, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(dirt), Direction.UP, dirt, false)));
            f.setShiftKeyDown(false);
            ItemStack empty = charged(AutarkiaItems.NANITE_MULTITOOL.get(), 0);
            f.setItemInHand(InteractionHand.MAIN_HAND, empty);
            InteractionResult none = empty.useOn(new UseOnContext(f, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(dead), Direction.UP, dead, false)));
            boolean stripped = x.level.getBlockState(log).is(Blocks.STRIPPED_OAK_LOG);
            boolean path = x.level.getBlockState(grass).is(Blocks.DIRT_PATH);
            boolean tilled = x.level.getBlockState(dirt).is(Blocks.FARMLAND);
            x.report("13 A17 the multitool strips (axe), makes paths (shovel after the axe passes) and tills when sneaking (hoe)", stripped && path && tilled,
                "stripped " + stripped + ", path " + path + ", tilled " + tilled);
            x.report("07 at 0 FE the multitool transforms nothing", none == InteractionResult.PASS && x.level.getBlockState(dead).is(Blocks.OAK_LOG), none.toString());
            fresh(x);
        });
    }

    private static void ghostMatter(TestM8.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            AdvancementHolder h = x.level.getServer().getAdvancements().get(Autarkia.id("equipment/ghost_matter"));
            var ghost = TestM6Logistics.courier(x);
            if (h != null) {
                ghost.getAdvancements().revoke(h, "done");
            }
            ItemStack tool = charged(AutarkiaItems.NANITE_MULTITOOL.get(), NaniteMultitoolItem.CAPACITY);
            tool.set(AutarkiaComponents.MATTER_LINK.get(), GlobalPos.of(x.level.dimension(), at(r, BUFFER)));
            tool.set(AutarkiaComponents.MATTER_CACHE.get(), MatterLink.MAX_CACHE - 6);
            ghost.setItemInHand(InteractionHand.MAIN_HAND, tool);
            List<ItemStack> drops = new ArrayList<>(List.of(new ItemStack(Items.COBBLESTONE, 10)));
            MatterLink.absorb(x.level, ghost, tool, drops);
            boolean done = h != null && ghost.getAdvancements().getOrStartProgress(h).isDone();
            x.report("07 7.3 the cache reaching 4,096 MU fires matter_cache_full (equipment/ghost_matter); the 4 items that do not fit stay drops",
                done && MatterLink.cache(tool) == MatterLink.MAX_CACHE && drops.get(0).getCount() == 4,
                "advancement " + done + ", cache " + MatterLink.cache(tool) + ", left " + drops.get(0).getCount());
            ghost.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
    }

    private static float meleeHit(SelfTest t, FakePlayer f, LivingEntity target) {
        float before = target.getHealth();
        ItemStack weapon = f.getMainHandItem();
        target.damageCooldownTime = 0;
        target.hurtServer(t.level, t.level.damageSources().playerAttack(f), (float) attribute(weapon, Attributes.ATTACK_DAMAGE, 1, EquipmentSlot.MAINHAND));
        weapon.getItem().postHurtEnemy(weapon, target, f);
        return before - target.getHealth();
    }

    private static void blade(TestM8.Run r) {
        SelfTest t = r.t();
        List<Mob> mobs = new ArrayList<>();
        t.then(x -> {
            Mob plain = mob(x, EntityTypes.VINDICATOR, r.at(262, 0, 30), 200);
            Mob armored = mob(x, EntityTypes.VINDICATOR, r.at(266, 0, 30), 200);
            armored.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            armored.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            armored.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            armored.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            mobs.clear();
            mobs.add(plain);
            mobs.add(armored);
        });
        t.waitTicks(2);
        t.then(x -> {
            Mob plain = mobs.get(0);
            Mob armored = mobs.get(1);
            FakePlayer f = fresh(x);
            place(f, r.at(264, 0, 27), true);
            f.setItemInHand(InteractionHand.MAIN_HAND, charged(AutarkiaItems.LATTICE_BLADE.get(), LatticeBladeItem.CAPACITY));
            float plainCharged = meleeHit(x, f, plain);
            float armoredCharged = meleeHit(x, f, armored);
            int used = LatticeBladeItem.CAPACITY - PoweredItem.energy(f.getMainHandItem());
            f.setItemInHand(InteractionHand.MAIN_HAND, charged(AutarkiaItems.LATTICE_BLADE.get(), 0));
            float plainEmpty = meleeHit(x, f, plain);
            float armoredEmpty = meleeHit(x, f, armored);
            int usedEmpty = PoweredItem.energy(f.getMainHandItem());
            DamageSource src = x.level.damageSources().playerAttack(f);
            float expected = 12f - CombatRules.getDamageAfterAbsorb(armored, 12f, src, (float) armored.getArmorValue(),
                (float) armored.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            float pierced = plainCharged - armoredCharged;
            float normal = plainEmpty - armoredEmpty;
            x.report("07#18 full diamond armor: the charged blade's armor reduction is half the normal one", armored.getArmorValue() == 20 && near(plainCharged, 12)
                && Math.abs(normal - expected) < 0.01f && Math.abs(pierced - normal * LatticeBladeItem.PIERCE) < 0.01f,
                String.format(Locale.ROOT, "armor %d, plain %.2f, reduction charged %.2f, empty %.2f (expected %.2f)", armored.getArmorValue(), plainCharged,
                    pierced, normal, expected));
            x.report("07#18 each charged hit costs 200 FE; at 0 FE hits are normal and cost nothing", used == 2 * LatticeBladeItem.HIT_COST && usedEmpty == 0
                && near(plainEmpty, 12), used + " FE for 2 hits, empty blade FE " + usedEmpty);
            mobs.forEach(m -> m.discard());
            fresh(x);
        });
    }

    private static ItemStack[] carapace() {
        ItemStack[] out = {charged(AutarkiaItems.LATTICE_CARAPACE_HELMET.get(), LatticeCarapaceItem.CAPACITY),
            charged(AutarkiaItems.LATTICE_CARAPACE_CHEST.get(), LatticeCarapaceItem.CAPACITY),
            charged(AutarkiaItems.LATTICE_CARAPACE_LEGS.get(), LatticeCarapaceItem.CAPACITY),
            charged(AutarkiaItems.LATTICE_CARAPACE_BOOTS.get(), LatticeCarapaceItem.CAPACITY)};
        for (ItemStack s : out) {
            s.setDamageValue(20);
        }
        return out;
    }

    private static long suitEnergy(LivingEntity le) {
        long n = 0;
        for (EquipmentSlot s : ARMOR) {
            n += PoweredItem.energy(le.getItemBySlot(s));
        }
        return n;
    }

    private static void suitTick(SelfTest t, LivingEntity le) {
        for (EquipmentSlot s : ARMOR) {
            le.getItemBySlot(s).inventoryTick(t.level, le, s);
        }
    }

    private static void carapace(TestM8.Run r) {
        SelfTest t = r.t();
        long[] fe = new long[2];
        t.then(x -> {
            FakePlayer f = fresh(x);
            place(f, r.at(290, 2, 30), false);
            ItemStack[] pieces = carapace();
            for (int i = 0; i < ARMOR.length; i++) {
                f.setItemSlot(ARMOR[i], pieces[i]);
            }
            fe[0] = suitEnergy(f);
        });
        t.repeat(200, x -> suitTick(x, gear(x)));
        t.check("07#19 each damaged carapace piece repairs 10 points in 200 ticks", () -> {
            for (EquipmentSlot s : ARMOR) {
                if (gear(t).getItemBySlot(s).getDamageValue() != 10) {
                    return false;
                }
            }
            return true;
        }, () -> "damage " + java.util.Arrays.stream(ARMOR).map(s -> String.valueOf(gear(t).getItemBySlot(s).getDamageValue())).toList());
        t.check("07#19 the repair costs 20,000 FE per piece (suit total 4 x 20,000 + 200 x (10 night vision + 2 wind) = 82,400)", () -> {
            fe[1] = suitEnergy(gear(t));
            return fe[0] - fe[1] == 4L * 10 * LatticeCarapaceItem.REPAIR_COST + 200L * (FrontierSuitItem.NIGHT_VISION_COST + FrontierSuitItem.WIND_COST);
        }, () -> (fe[0] - fe[1]) + " FE");
        t.then(x -> {
            FakePlayer f = gear(x);
            SuitItem.toggle(f);
            for (EquipmentSlot s : ARMOR) {
                f.getItemBySlot(s).setDamageValue(15);
            }
            fe[0] = suitEnergy(f);
        });
        t.repeat(40, x -> suitTick(x, gear(x)));
        t.check("07 suit_enabled off: no self-repair and no FE use", () -> {
            for (EquipmentSlot s : ARMOR) {
                if (gear(t).getItemBySlot(s).getDamageValue() != 15) {
                    return false;
                }
            }
            return suitEnergy(gear(t)) == fe[0];
        }, () -> "FE " + fe[0] + " -> " + suitEnergy(gear(t)));
        t.then(x -> fresh(x));
    }

    private static void disassembler(TestM8.Run r) {
        SelfTest t = r.t();
        List<BlockPos> cells = new ArrayList<>();
        for (int y = 0; y <= 2; y++) {
            for (int z = 0; z <= 3; z++) {
                for (int dx = 0; dx <= 4; dx++) {
                    boolean in = y == 0 || (y == 1 && dx >= 1 && dx <= 3) || (y == 2 && dx == 2 && (z == 1 || z == 2));
                    if (in && !(y == 0 && z == 3 && (dx == 2 || dx == 4))) {
                        cells.add(r.at(HILL.getX() + dx, y, HILL.getZ() + z));
                    }
                }
            }
        }
        cells.sort(Comparator.comparingInt((BlockPos p) -> -p.getY()).thenComparingInt(BlockPos::getZ).thenComparingInt(BlockPos::getX));
        BlockPos chest = r.at(HILL.getX() + 2, 0, HILL.getZ() + 3);
        BlockPos obsidian = r.at(HILL.getX() + 4, 0, HILL.getZ() + 3);
        List<Integer> feTicks = new ArrayList<>();
        int[] maxPerTick = new int[1];
        float[] health = new float[1];
        Mob[] target = new Mob[1];
        t.then(x -> {
            TestM8.box(r, 296, -1, 34, 308, -1, 58, Blocks.OBSIDIAN.defaultBlockState());
            for (BlockPos p : cells) {
                x.level.setBlock(p, ((p.getX() + p.getY() + p.getZ()) & 1) == 0 ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            }
            x.level.setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
            if (x.level.getBlockEntity(chest) instanceof ChestBlockEntity c) {
                c.setItem(0, new ItemStack(Items.DIAMOND, 3));
            }
            x.level.setBlock(obsidian, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
            TestM1.killItems(x, chest, 16);
            FakePlayer f = fresh(x);
            place(f, r.at(HILL.getX() + 2, 3, HILL.getZ() - 6), false);
            ItemStack beam = charged(AutarkiaItems.DISASSEMBLER_BEAM.get(), DisassemblerBeamItem.CAPACITY);
            f.setItemInHand(InteractionHand.MAIN_HAND, beam);
            f.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(cells.get(0)));
            InteractionResult res = beam.getItem().use(x.level, f, InteractionHand.MAIN_HAND);
            x.report("07#20 using the disassembler starts the beam", res.consumesAction() && f.isUsingItem(), res.toString());
        });
        t.waitUntil("07#20 the beam overheats and stops", 120, () -> !gear(t).isUsingItem(), x -> {
            FakePlayer f = gear(x);
            if (!f.isUsingItem()) {
                return;
            }
            BlockPos aim = cells.stream().filter(p -> !x.level.getBlockState(p).isAir()).findFirst().orElse(chest);
            f.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(aim));
            int e0 = PoweredItem.energy(f.getMainHandItem());
            long air0 = cells.stream().filter(p -> x.level.getBlockState(p).isAir()).count();
            useTick(f);
            feTicks.add(e0 - PoweredItem.energy(f.getMainHandItem()));
            long air1 = cells.stream().filter(p -> x.level.getBlockState(p).isAir()).count();
            maxPerTick[0] = (int) Math.max(maxPerTick[0], air1 - air0);
        });
        t.check("07#20 the beam ran 100 ticks at 2,000 FE/t (200,000 FE) and took at most 2 blocks per tick", () -> feTicks.size() == DisassemblerBeamItem.MAX_FIRE
            && feTicks.stream().allMatch(v -> v == DisassemblerBeamItem.FE_PER_TICK) && maxPerTick[0] <= DisassemblerBeamItem.BLOCKS_PER_TICK && maxPerTick[0] > 0,
            () -> feTicks.size() + " beam ticks, FE per tick " + feTicks.stream().distinct().toList() + ", max " + maxPerTick[0] + " blocks per tick");
        t.check("07#20 the hill is gone, the chest and its diamonds are untouched, no block over resistance 50 was removed", () -> {
            boolean gone = cells.stream().allMatch(p -> t.level.getBlockState(p).isAir());
            boolean kept = t.level.getBlockEntity(chest) instanceof ChestBlockEntity c && c.getItem(0).is(Items.DIAMOND) && c.getItem(0).getCount() == 3;
            long floor = 0;
            for (BlockPos p : BlockPos.betweenClosed(r.at(296, -1, 34), r.at(308, -1, 58))) {
                floor += t.level.getBlockState(p).is(Blocks.OBSIDIAN) ? 1 : 0;
            }
            return gone && kept && t.level.getBlockState(obsidian).is(Blocks.OBSIDIAN) && floor == 13L * 25;
        }, () -> "left " + cells.stream().filter(p -> !t.level.getBlockState(p).isAir()).count() + " of " + cells.size());
        t.check("07 G14 without a link every removed block dropped its item (netherite tool drops)", () -> itemsAround(t, chest, 16) == cells.size(),
            () -> itemsAround(t, chest, 16) + " items for " + cells.size() + " blocks");
        t.check("07#20 after the overheat the beam is on a 60-tick cooldown and cannot fire", () -> {
            FakePlayer f = gear(t);
            return f.getCooldowns().isOnCooldown(f.getMainHandItem()) && !f.getMainHandItem().getItem().use(t.level, f, InteractionHand.MAIN_HAND).consumesAction();
        }, () -> "cooldown " + gear(t).getCooldowns().isOnCooldown(gear(t).getMainHandItem()));
        t.then(x -> {
            FakePlayer f = gear(x);
            for (int i = 0; i < DisassemblerBeamItem.OVERHEAT_COOLDOWN - 1; i++) {
                f.getCooldowns().tick();
            }
            boolean still = f.getCooldowns().isOnCooldown(f.getMainHandItem());
            f.getCooldowns().tick();
            boolean free = !f.getCooldowns().isOnCooldown(f.getMainHandItem());
            x.report("07#20 the cooldown lasts 60 ticks", still && free, "at 59 ticks " + still + ", free at 60 " + free);
            target[0] = mob(x, EntityTypes.VINDICATOR, r.at(HILL.getX() + 2, 0, HILL.getZ() + 6), 100);
            health[0] = target[0].getHealth();
            feTicks.clear();
            f.lookAt(EntityAnchorArgument.Anchor.EYES, target[0].position().add(0, target[0].getBbHeight() / 2, 0));
            f.getMainHandItem().getItem().use(x.level, f, InteractionHand.MAIN_HAND);
        });
        t.repeat(20, x -> {
            FakePlayer f = gear(x);
            f.lookAt(EntityAnchorArgument.Anchor.EYES, target[0].position().add(0, target[0].getBbHeight() / 2, 0));
            int e0 = PoweredItem.energy(f.getMainHandItem());
            useTick(f);
            feTicks.add(e0 - PoweredItem.energy(f.getMainHandItem()));
        });
        t.then(x -> {
            FakePlayer f = gear(x);
            f.releaseUsingItem();
            float dealt = health[0] - target[0].getHealth();
            x.report("07 G14 a beam held on a mob for 20 ticks deals 6 beam damage once per 10 ticks (12) and stops at the mob", Math.abs(dealt - 2 * DisassemblerBeamItem.DAMAGE) < 0.01f
                && feTicks.size() == 20 && feTicks.stream().allMatch(v -> v == DisassemblerBeamItem.FE_PER_TICK) && !f.isUsingItem() && DisassemblerBeamItem.firing(f) < 0,
                String.format(Locale.ROOT, "dealt %.2f, %d ticks", dealt, feTicks.size()));
            target[0].discard();
            fresh(x);
        });
    }
}

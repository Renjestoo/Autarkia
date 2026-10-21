package dev.autarkia.command;

import com.mojang.authlib.GameProfile;
import dev.autarkia.equipment.AegisBladeItem;
import dev.autarkia.equipment.AegisMultitoolItem;
import dev.autarkia.equipment.AreaMining;
import dev.autarkia.equipment.EnchantMode;
import dev.autarkia.equipment.PoweredItem;
import dev.autarkia.equipment.SingularityLanceItem;
import dev.autarkia.equipment.SuitItem;
import dev.autarkia.equipment.ToolMode;
import dev.autarkia.network.EquipmentActionPayload;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaItems;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * The gear part of {@code /autarkia test m9} (07 #21, #22, #23, #37), one check per checklist test, in the region
 * x 240..290, z 10..70 inside the T6 domain and x 0..90, z 220..290 outside every domain. The level does not tick fake
 * players, so the script runs their use ticks, cooldown ticks and lance beam ticks. The rigs stay standing.
 */
final class TestM9Gear {
    private static final UUID FAKE = UUID.nameUUIDFromBytes("autarkia-m9-gear".getBytes(StandardCharsets.UTF_8));
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private TestM9Gear() {
    }

    static void run(TestM9.Run r) {
        multitool(r);
        blade(r);
        lance(r);
        keys(r);
    }

    private static FakePlayer gear(SelfTest t) {
        return FakePlayerFactory.get(t.level, new GameProfile(FAKE, "AutarkiaM9Gear"));
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
        for (Item i : List.of(AutarkiaItems.AEGIS_BLADE.get(), AutarkiaItems.SINGULARITY_LANCE.get())) {
            f.getCooldowns().removeCooldown(f.getCooldowns().getCooldownGroup(new ItemStack(i)));
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

    private static void place(FakePlayer f, BlockPos p) {
        f.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        f.setDeltaMovement(Vec3.ZERO);
        f.setOnGround(true);
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

    private static int items(SelfTest t, BlockPos center, double r, Item item) {
        return t.level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(r), e -> e.getItem().is(item)).stream()
            .mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static int level(SelfTest t, ItemStack stack, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY)
            .getLevel(t.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key));
    }

    private static String key(Component c) {
        return c != null && c.getContents() instanceof TranslatableContents tc ? tc.getKey() : String.valueOf(c);
    }

    private static void multitool(TestM9.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            TestM9.box(r, 250, 0, 20, 254, 4, 20, Blocks.STONE.defaultBlockState());
            TestM9.box(r, 258, 0, 26, 277, 0, 26, Blocks.DIAMOND_ORE.defaultBlockState());
            TestM1.killItems(x, r.at(264, 1, 23), 20);
            FakePlayer f = fresh(x);
            place(f, r.at(252, 0, 17));
            ItemStack tool = charged(AutarkiaItems.AEGIS_MULTITOOL.get(), AegisMultitoolItem.CAPACITY);
            tool.set(AutarkiaComponents.TOOL_MODE.get(), ToolMode.AREA5);
            AegisMultitoolItem.applyEnchant(tool, EnchantMode.SILK, x.level.registryAccess());
            f.setItemInHand(InteractionHand.MAIN_HAND, tool);
            BlockPos center = r.at(252, 2, 20);
            float progress = x.level.getBlockState(center).getDestroyProgress(f, x.level, center);
            AreaMining.remember(f, Direction.NORTH);
            f.gameMode.destroyBlock(center);
            long left = BlockPos.betweenClosedStream(r.at(250, 0, 20), r.at(254, 4, 20)).filter(p -> !x.level.getBlockState(p).isAir()).count();
            int stone = items(x, center, 6, Items.STONE);
            int cobble = items(x, center, 6, Items.COBBLESTONE);
            int used = AegisMultitoolItem.CAPACITY - PoweredItem.energy(tool);
            tool.set(AutarkiaComponents.TOOL_MODE.get(), ToolMode.SINGLE);
            AegisMultitoolItem.applyEnchant(tool, EnchantMode.FORTUNE, x.level.registryAccess());
            place(f, r.at(267, 0, 24));
            int ores = 0;
            for (BlockPos p : BlockPos.betweenClosed(r.at(258, 0, 26), r.at(277, 0, 26))) {
                if (f.gameMode.destroyBlock(p.immutable())) {
                    ores++;
                }
            }
            int diamonds = items(x, r.at(267, 0, 26), 14, Items.DIAMOND);
            boolean fortuneOn = level(x, tool, Enchantments.FORTUNE) == AegisMultitoolItem.FORTUNE_LEVEL;
            x.report("07#21 5x5 + Silk Touch breaks 25 stone instantly as stone; Fortune III on 20 diamond ores drops more than 20 diamonds",
                progress >= 1f && left == 0 && stone == 25 && cobble == 0 && used == 25 * AegisMultitoolItem.BLOCK_COST && ores == 20 && diamonds > 20
                    && fortuneOn,
                String.format(Locale.ROOT, "progress per tick %.1f, left %d, stone %d, cobblestone %d, FE %d; fortune %b, %d ores, %d diamonds (%.2f per ore)",
                    progress, left, stone, cobble, used, fortuneOn, ores, diamonds, diamonds / Math.max(1.0, ores)));
            TestM1.killItems(x, r.at(264, 1, 23), 20);
            fresh(x);
        });
    }

    private static void blade(TestM9.Run r) {
        SelfTest t = r.t();
        List<Mob> mobs = new ArrayList<>();
        t.then(x -> {
            mobs.clear();
            mobs.add(mob(x, EntityTypes.VINDICATOR, r.at(262, 0, 40), 100));
            mobs.add(mob(x, EntityTypes.PILLAGER, r.at(265, 0, 44), 100));
            mobs.add(mob(x, EntityTypes.VINDICATOR, r.at(268, 0, 40), 100));
            mobs.add(mob(x, EntityTypes.VILLAGER, r.at(265, 0, 37), 20));
            mobs.add(mob(x, EntityTypes.VINDICATOR, r.at(274, 0, 40), 100));
        });
        t.waitTicks(2);
        t.then(x -> {
            FakePlayer f = fresh(x);
            place(f, r.at(265, 0, 40));
            f.setItemInHand(InteractionHand.MAIN_HAND, charged(AutarkiaItems.AEGIS_BLADE.get(), AegisBladeItem.CAPACITY));
            ItemStack blade = f.getMainHandItem();
            InteractionResult res = blade.getItem().use(x.level, f, InteractionHand.MAIN_HAND);
            int used = AegisBladeItem.CAPACITY - PoweredItem.energy(blade);
            boolean again = blade.getItem().use(x.level, f, InteractionHand.MAIN_HAND).consumesAction();
            for (int i = 0; i < AegisBladeItem.SWEEP_COOLDOWN - 1; i++) {
                f.getCooldowns().tick();
            }
            boolean still = f.getCooldowns().isOnCooldown(blade);
            f.getCooldowns().tick();
            boolean free = !f.getCooldowns().isOnCooldown(blade);
            float[] lost = new float[mobs.size()];
            for (int i = 0; i < mobs.size(); i++) {
                lost[i] = mobs.get(i).getMaxHealth() - mobs.get(i).getHealth();
            }
            boolean hostiles = Math.abs(lost[0] - 20) < 0.01f && Math.abs(lost[1] - 20) < 0.01f && Math.abs(lost[2] - 20) < 0.01f;
            x.report("07#22 the Aegis sweep hits the hostiles within 6 blocks for 20, leaves the villager and the far mob alone, 5 s cooldown",
                res.consumesAction() && hostiles && lost[3] == 0 && lost[4] == 0 && used == AegisBladeItem.SWEEP_COST && !again && still && free,
                String.format(Locale.ROOT, "use %s, damage %.2f / %.2f / %.2f, villager %.2f, mob at 9 blocks %.2f, FE %d, second use %b, cooldown at 99 %b, free at 100 %b",
                    res, lost[0], lost[1], lost[2], lost[3], lost[4], used, again, still, free));
            fresh(x);
        });
    }

    private static Map<BlockPos, BlockState> snapshot(SelfTest t, BlockPos a, BlockPos b) {
        Map<BlockPos, BlockState> out = new HashMap<>();
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            out.put(p.immutable(), t.level.getBlockState(p));
        }
        return out;
    }

    private static List<BlockPos> changed(SelfTest t, Map<BlockPos, BlockState> before) {
        return before.entrySet().stream().filter(e -> t.level.getBlockState(e.getKey()) != e.getValue()).map(Map.Entry::getKey).toList();
    }

    private static void hill(TestM9.Run r, int x0, int z0, int w, int h, int d) {
        TestM9.box(r, x0, 0, z0, x0 + w - 1, h - 1, z0 + d - 1, Blocks.STONE.defaultBlockState());
        int cx = x0 + w / 2;
        r.t().level.setBlock(r.at(cx - 1, 1, z0 + 1), Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
        r.t().level.setBlock(r.at(cx + 1, 1, z0 + 1), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
    }

    /**
     * One lance shot: aim level at the hill face, charge with use ticks until it fires, then run the beam ticks. out holds
     * (use accepted 1/0, use ticks until it fired, use ticks run); axis gets the eye and the look of the shot.
     */
    private static void shot(TestM9.Run r, BlockPos stand, BlockPos aim, ToolMode mode, int[] out, Vec3[] axis) {
        SelfTest t = r.t();
        t.then(x -> {
            x.level.getGameRules().set(GameRules.MOB_GRIEFING, true, x.level.getServer());
            FakePlayer f = fresh(x);
            place(f, stand);
            ItemStack lance = charged(AutarkiaItems.SINGULARITY_LANCE.get(), SingularityLanceItem.CAPACITY);
            lance.set(AutarkiaComponents.TOOL_MODE.get(), mode);
            f.setItemInHand(InteractionHand.MAIN_HAND, lance);
            f.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(aim.getX() + 0.5, f.getEyeY(), aim.getZ() + 0.5));
            axis[0] = f.getEyePosition();
            axis[1] = f.getLookAngle();
            out[0] = lance.getItem().use(x.level, f, InteractionHand.MAIN_HAND).consumesAction() ? 1 : 0;
            out[1] = -1;
            out[2] = 0;
        });
        t.repeat(SingularityLanceItem.CHARGE_TICKS + 2, x -> {
            FakePlayer f = gear(x);
            ItemStack s = f.getUseItem();
            if (!s.isEmpty()) {
                out[2]++;
                s.getItem().onUseTick(x.level, f, s, f.getUseItemRemainingTicks());
            }
            if (out[1] < 0 && SingularityLanceItem.firing(f)) {
                out[1] = out[2];
            }
        });
        t.repeat(SingularityLanceItem.BEAM_TICKS + 5, x -> SingularityLanceItem.tickShot(x.level, gear(x)));
    }

    private static double axisDistance(Vec3 eye, Vec3 dir, BlockPos p) {
        Vec3 c = Vec3.atCenterOf(p).subtract(eye);
        return c.subtract(dir.scale(c.dot(dir))).length();
    }

    private static void lance(TestM9.Run r) {
        SelfTest t = r.t();
        int[] safe = new int[3];
        int[] destroy = new int[3];
        int[] inside = new int[3];
        List<Map<BlockPos, BlockState>> snaps = new ArrayList<>();
        Mob[] targets = new Mob[2];
        Vec3[] axis = new Vec3[2];
        Vec3[] unused = new Vec3[2];
        long[] fe = new long[2];
        t.then(x -> {
            snaps.clear();
            hill(r, 38, 244, 5, 5, 5);
            hill(r, 56, 264, 9, 7, 7);
            TestM9.box(r, 52, -1, 271, 68, 8, 271, Blocks.OBSIDIAN.defaultBlockState());
            hill(r, 268, 62, 5, 5, 5);
            snaps.add(snapshot(x, r.at(36, -1, 242), r.at(44, 6, 250)));
            snaps.add(snapshot(x, r.at(52, -1, 262), r.at(68, 8, 274)));
            snaps.add(snapshot(x, r.at(266, -1, 60), r.at(274, 6, 68)));
            targets[0] = mob(x, EntityTypes.VINDICATOR, r.at(40, 0, 238), 200);
            Wolf wolf = mob(x, EntityTypes.WOLF, r.at(41, 0, 239), 40);
            wolf.tame(fresh(x));
            targets[1] = wolf;
        });
        t.waitTicks(2);
        shot(r, r.at(40, 0, 230), r.at(40, 1, 244), ToolMode.TERRAIN_SAFE, safe, unused);
        t.then(x -> {
            fe[0] = SingularityLanceItem.CAPACITY - PoweredItem.energy(gear(x).getMainHandItem());
            fe[1] = gear(x).getCooldowns().isOnCooldown(gear(x).getMainHandItem()) ? 1 : 0;
        });
        shot(r, r.at(60, 0, 250), r.at(60, 1, 264), ToolMode.TERRAIN_DESTROY, destroy, axis);
        shot(r, r.at(270, 0, 52), r.at(270, 1, 62), ToolMode.TERRAIN_DESTROY, inside, unused);
        t.check("07#23 lance: terrain-safe changes no block; terrain-destroy outside the domain digs within radius 3 of the beam (obsidian and "
            + "chest kept); inside the domain no block changes; 80 beam damage once per shot, the owner's wolf spared; 1,000,000 FE after 60 ticks", () -> {
                List<BlockPos> a = changed(t, snaps.get(0));
                List<BlockPos> b = changed(t, snaps.get(1));
                List<BlockPos> c = changed(t, snaps.get(2));
                double far = b.stream().mapToDouble(p -> axisDistance(axis[0], axis[1], p)).max().orElse(-1);
                boolean kept = t.level.getBlockState(r.at(59, 1, 265)).is(Blocks.OBSIDIAN) && t.level.getBlockState(r.at(61, 1, 265)).is(Blocks.CHEST);
                SingularityLanceItem.ShotState last = SingularityLanceItem.lastShot(gear(t));
                float dealt = targets[0].getMaxHealth() - targets[0].getHealth();
                float wolf = targets[1].getMaxHealth() - targets[1].getHealth();
                return safe[0] == 1 && destroy[0] == 1 && inside[0] == 1 && safe[1] == SingularityLanceItem.CHARGE_TICKS && a.isEmpty() && !b.isEmpty()
                    && far <= SingularityLanceItem.IMPACT_TERRAIN_RADIUS + 1.0E-6 && kept && c.isEmpty() && last != null && last.done()
                    && last.terrainTicks() == 0 && Math.abs(dealt - SingularityLanceItem.DAMAGE) < 0.01f && wolf == 0
                    && fe[0] == SingularityLanceItem.SHOT_COST && fe[1] == 1;
            }, () -> String.format(Locale.ROOT, "fired after %d/%d/%d use ticks, FE per shot %d, cooldown %b; safe changed %d; destroy changed %d "
                + "(max %.2f from the axis), obsidian and chest kept %b; inside changed %d, terrain ticks %s; vindicator took %.2f, wolf took %.2f",
                safe[1], destroy[1], inside[1], fe[0], fe[1] == 1, changed(t, snaps.get(0)).size(), changed(t, snaps.get(1)).size(),
                changed(t, snaps.get(1)).stream().mapToDouble(p -> axisDistance(axis[0], axis[1], p)).max().orElse(-1),
                t.level.getBlockState(r.at(59, 1, 265)).is(Blocks.OBSIDIAN) && t.level.getBlockState(r.at(61, 1, 265)).is(Blocks.CHEST),
                changed(t, snaps.get(2)).size(), SingularityLanceItem.lastShot(gear(t)) == null ? "none" : SingularityLanceItem.lastShot(gear(t)).terrainTicks(),
                targets[0].getMaxHealth() - targets[0].getHealth(), targets[1].getMaxHealth() - targets[1].getHealth()));
        t.then(x -> fresh(x));
    }

    private static void keys(TestM9.Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            FakePlayer f = fresh(x);
            place(f, r.at(280, 0, 60));
            List<String> log = new ArrayList<>();
            ItemStack tool = charged(AutarkiaItems.AEGIS_MULTITOOL.get(), AegisMultitoolItem.CAPACITY);
            f.setItemInHand(InteractionHand.MAIN_HAND, tool);
            List<ToolMode> modes = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                log.add(key(EquipmentActionPayload.apply(f, EquipmentActionPayload.MODE)));
                modes.add(tool.get(AutarkiaComponents.TOOL_MODE.get()));
            }
            List<String> enchants = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                log.add(key(EquipmentActionPayload.apply(f, EquipmentActionPayload.ENCHANT)));
                enchants.add(tool.get(AutarkiaComponents.ENCHANT_MODE.get()) + ":" + level(x, tool, Enchantments.SILK_TOUCH) + "/"
                    + level(x, tool, Enchantments.FORTUNE));
            }
            ItemStack lance = charged(AutarkiaItems.SINGULARITY_LANCE.get(), 0);
            f.setItemInHand(InteractionHand.MAIN_HAND, lance);
            log.add(key(EquipmentActionPayload.apply(f, EquipmentActionPayload.MODE)));
            ToolMode lanceMode = lance.get(AutarkiaComponents.TOOL_MODE.get());
            log.add(key(EquipmentActionPayload.apply(f, EquipmentActionPayload.ENCHANT)));
            List<Item> pieces = List.of(AutarkiaItems.AEGIS_HELMET.get(), AutarkiaItems.AEGIS_CHEST.get(), AutarkiaItems.AEGIS_LEGS.get(),
                AutarkiaItems.AEGIS_BOOTS.get());
            for (int i = 0; i < 4; i++) {
                f.setItemSlot(ARMOR[i], charged(pieces.get(i), 1000));
            }
            log.add(key(EquipmentActionPayload.apply(f, EquipmentActionPayload.SUIT)));
            boolean off = SuitItem.worn(f).stream().noneMatch(s -> s.getOrDefault(AutarkiaComponents.SUIT_ENABLED.get(), true));
            ItemStack helmet = f.getItemBySlot(EquipmentSlot.HEAD);
            boolean saved = true;
            for (ItemStack s : List.of(tool, lance, helmet)) {
                var ops = x.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                ItemStack back = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, s).getOrThrow()).getOrThrow();
                saved &= back.get(AutarkiaComponents.TOOL_MODE.get()) == s.get(AutarkiaComponents.TOOL_MODE.get())
                    && back.get(AutarkiaComponents.ENCHANT_MODE.get()) == s.get(AutarkiaComponents.ENCHANT_MODE.get())
                    && java.util.Objects.equals(back.get(AutarkiaComponents.SUIT_ENABLED.get()), s.get(AutarkiaComponents.SUIT_ENABLED.get()))
                    && java.util.Objects.equals(back.get(DataComponents.ENCHANTMENTS), s.get(DataComponents.ENCHANTMENTS));
            }
            List<String> want = List.of("message.autarkia.mode", "message.autarkia.mode", "message.autarkia.mode", "message.autarkia.enchant",
                "message.autarkia.enchant", "message.autarkia.enchant", "message.autarkia.mode",
                "message.autarkia.enchant.none", "message.autarkia.suit.off");
            x.report("07#37 mode, enchant and suit keys: action-bar messages, the component the HUD labels show changes, the state survives a save",
                modes.equals(List.of(ToolMode.AREA3, ToolMode.AREA5, ToolMode.SINGLE))
                    && enchants.equals(List.of("SILK:1/0", "FORTUNE:0/" + AegisMultitoolItem.FORTUNE_LEVEL, "NONE:0/0"))
                    && lanceMode == ToolMode.TERRAIN_DESTROY && off && saved && log.equals(want),
                "modes " + modes + ", enchants " + enchants + ", lance " + lanceMode + ", suit off " + off + ", saved " + saved + ", messages " + log);
            fresh(x);
        });
    }
}

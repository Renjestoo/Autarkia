package dev.autarkia.equipment;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.BeamVisualPayload;
import dev.autarkia.registry.AutarkiaSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.jspecify.annotations.Nullable;

/**
 * {@code disassembler_beam} (07 sections 1.6 and 2.2, decision G14): 20,000,000 FE charged at 20,000 FE/t, main hand
 * only. Hold use: each beam tick costs {@link #FE_PER_TICK} FE, clips from the eye along the look vector
 * ({@link #RANGE} blocks) and takes apart up to {@link #BLOCKS_PER_TICK} blocks in a row. A block stops the beam
 * unharmed when it has a block entity, is a bastion block, is in {@code #autarkia:disassembler_immune}, resists more
 * than {@code weapons.disassemblerMaxResistance}, is unbreakable, lies in another colony's domain where the user is not
 * owner or trusted, is spawn protected, or when the {@code BreakBlockEvent} fired with the user is cancelled. Drops use
 * the netherite tool of the block's {@code mineable} tag (decision C4) and go through the {@link MatterLink}; without a
 * link they fall as items. The first valid living target in front of the hit block (not the user, the rules of
 * {@link ArcRifleItem#valid}) takes {@link #DAMAGE} {@code autarkia:beam} damage at most once per {@link #HIT_PERIOD}
 * ticks (its own per-target clock; the vanilla damage cooldown is cleared for each beam hit) and stops the beam. After {@link #MAX_FIRE} beam ticks the beam overheats: use stops and a
 * {@link #OVERHEAT_COOLDOWN}-tick cooldown starts. Each beam tick sends a {@code beam_visual}; the loop sound plays every
 * 20 ticks.
 */
public class DisassemblerBeamItem extends PoweredGeoItem {
    public static final int CAPACITY = 20000000;
    public static final int INPUT = 20000;
    public static final int FE_PER_TICK = 2000;
    public static final double RANGE = 24;
    public static final float DAMAGE = 6f;
    public static final int HIT_PERIOD = 10;
    public static final int BLOCKS_PER_TICK = 2;
    public static final int MAX_FIRE = 100;
    public static final int OVERHEAT_COOLDOWN = 60;
    public static final TagKey<Block> IMMUNE = TagKey.create(Registries.BLOCK, Autarkia.id("disassembler_immune"));
    private static final Set<Identifier> BASTION = Set.of(Autarkia.id("bastion_block"), Autarkia.id("bastion_glass"));
    private static final int BEAM_COLOR = 0xFFB39DFF;
    private static final Map<UUID, Integer> FIRING = new HashMap<>();
    private static final Map<UUID, Long> LAST_HIT = new HashMap<>();

    public DisassemblerBeamItem(Properties props) {
        super(props, CAPACITY, INPUT, 0, "disassembler_beam", "fire");
    }

    /** Return the beam ticks fired since the user started this use, or -1 when the user is not firing. */
    public static int firing(LivingEntity user) {
        return FIRING.getOrDefault(user.getUUID(), -1);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        return MatterLink.tryLink(ctx);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || energy(stack) < FE_PER_TICK || player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel sl) {
            FIRING.put(player.getUUID(), 0);
            triggerAnim(player, GeoItem.getOrAssignId(stack, sl), "main", "fire");
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), AutarkiaSounds.of("item.beam.loop"), SoundSource.PLAYERS, 0.6f, 2.0f);
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel sl) || !(user instanceof Player player)) {
            return;
        }
        int fired = FIRING.getOrDefault(player.getUUID(), 0);
        if (!spend(stack, FE_PER_TICK)) {
            player.stopUsingItem();
            return;
        }
        beam(sl, player, stack);
        FIRING.put(player.getUUID(), ++fired);
        if (fired % 20 == 0) {
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), AutarkiaSounds.of("item.beam.loop"), SoundSource.PLAYERS, 0.6f, 2.0f);
        }
        if (fired >= MAX_FIRE) {
            player.getCooldowns().addCooldown(stack, OVERHEAT_COOLDOWN);
            player.stopUsingItem();
        }
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity user, int remaining) {
        FIRING.remove(user.getUUID());
        if (user.level() instanceof ServerLevel sl) {
            stopTriggeredAnim(user, GeoItem.getOrAssignId(stack, sl), "main", "fire");
        }
    }

    /**
     * Fire one beam tick from the player's eyes: damage the first valid target or take apart up to
     * {@link #BLOCKS_PER_TICK} blocks, then send the beam visual. Return the removed positions.
     */
    public static List<BlockPos> beam(ServerLevel sl, Player player, ItemStack stack) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(RANGE));
        List<BlockPos> removed = new ArrayList<>();
        Vec3 stop = end;
        for (int i = 0; i < BLOCKS_PER_TICK; i++) {
            BlockHitResult hit = sl.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            stop = hit.getLocation();
            EntityHitResult target = ProjectileUtil.getEntityHitResult(sl, player, eye, stop, new AABB(eye, stop).inflate(1.0),
                e -> ArcRifleItem.valid(sl, player, e), 0.3f);
            if (target != null && target.getEntity() instanceof LivingEntity le) {
                stop = target.getLocation();
                hurt(sl, player, le);
                break;
            }
            if (hit.getType() != HitResult.Type.BLOCK || !takeApart(sl, player, stack, hit.getBlockPos())) {
                break;
            }
            removed.add(hit.getBlockPos());
        }
        if (FIRING.getOrDefault(player.getUUID(), 0) % 2 == 0) {
            Vec3 muzzle = eye.add(0, -0.2, 0);
            AutarkiaNetwork.toNear(sl, muzzle, 128, new BeamVisualPayload(player.getId(), muzzle, stop, 2, BEAM_COLOR, BeamVisualPayload.KIND_DISASSEMBLER, 0.25f));
        }
        return removed;
    }

    /** Return true when the beam may take the block apart (see the class rules); fires the break event last. */
    public static boolean removable(ServerLevel sl, Player player, BlockPos pos, BlockState state) {
        if (state.isAir() || state.hasBlockEntity() || state.getDestroySpeed(sl, pos) < 0 || state.is(IMMUNE)
            || BASTION.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()))
            || state.getExplosionResistance(sl, pos, null) > AutarkiaConfig.WEAPONS_DISASSEMBLER_MAX_RESISTANCE.get() || !sl.mayInteract(player, pos)) {
            return false;
        }
        Colony c = Colonies.at(sl, pos);
        if (c != null && !c.canManage(player.getUUID())) {
            return false;
        }
        return !NeoForge.EVENT_BUS.post(new BreakBlockEvent(sl, pos, state, player)).isCanceled();
    }

    /** Return the tool the drops of a block are rolled with: the netherite tool of its {@code mineable} tag, else empty. */
    public static ItemStack dropTool(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return new ItemStack(Items.NETHERITE_PICKAXE);
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return new ItemStack(Items.NETHERITE_SHOVEL);
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return new ItemStack(Items.NETHERITE_AXE);
        }
        return state.is(BlockTags.MINEABLE_WITH_HOE) ? new ItemStack(Items.NETHERITE_HOE) : ItemStack.EMPTY;
    }

    private static boolean takeApart(ServerLevel sl, Player player, ItemStack stack, BlockPos pos) {
        BlockState state = sl.getBlockState(pos);
        if (!removable(sl, player, pos, state)) {
            return false;
        }
        ItemStack tool = dropTool(state);
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, sl, pos, null, player, tool));
        sl.destroyBlock(pos, false, player, 512);
        MatterLink.absorb(sl, player, stack, drops);
        for (ItemStack d : drops) {
            if (!d.isEmpty()) {
                Block.popResource(sl, pos, d);
            }
        }
        state.spawnAfterBreak(sl, pos, tool, true);
        return true;
    }

    private static void hurt(ServerLevel sl, Player player, LivingEntity target) {
        long now = sl.getGameTime();
        Long last = LAST_HIT.get(target.getUUID());
        if (last != null && now - last < HIT_PERIOD) {
            return;
        }
        if (LAST_HIT.size() > 256) {
            LAST_HIT.values().removeIf(t -> now - t >= HIT_PERIOD);
        }
        LAST_HIT.put(target.getUUID(), now);
        target.damageCooldownTime = 0;
        target.hurtServer(sl, sl.damageSources().source(AutarkiaDamage.BEAM, player), DAMAGE);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        MatterLink.flow(level, entity, stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, display, out, flag);
        MatterLink.tooltip(stack, out);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        var main = new AnimationController<DisassemblerBeamItem>("main", 2,
            test -> test.setAndContinue(RawAnimation.begin().thenLoop("animation.disassembler_beam.idle")));
        main.triggerableAnim("fire", RawAnimation.begin().thenLoop("animation.disassembler_beam.fire"));
        controllers.add(main);
    }
}

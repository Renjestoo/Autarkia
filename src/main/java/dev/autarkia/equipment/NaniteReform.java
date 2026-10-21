package dev.autarkia.equipment;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.ReformFxPayload;
import dev.autarkia.registry.AutarkiaAttachments;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.EventTrigger;
import dev.autarkia.storage.MatterBank;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * Nanite Reform (07 section 3.2, W24): a lethal, not excluded hit on a player in the full Aegis set is cancelled when the
 * Reform is ready (game time at or after the {@code autarkia:reform_ready_at} attachment) and the Reform domain (the
 * domain that holds the player, else the tether colony) pays {@code reform.matterCost} free MU through
 * {@link MatterBank#withdraw} (fullest buffer first, reason "Reform"). The player gets 50% health, no fire, no harmful
 * effects, 20 ticks of invisibility, 40 ticks of damage immunity and a cooldown of {@code reform.cooldownSeconds}, and
 * forms again {@code reform.offsetBlocks} away from the damage source position (a random direction without one): the 8
 * directions are tried from the one closest to "away", each at Y offset 0, +1, -1, +2, -2; a spot needs 2 air blocks,
 * a sturdy floor and no lava or fire, else the first free 2-block gap above the death position is used. The client
 * draws {@code reform_fx}; the sound {@code aegis.reform} plays and {@code equipment/not_today} fires.
 */
public final class NaniteReform {
    public static final int IMMUNITY_TICKS = 40;
    public static final int INVISIBILITY_TICKS = 20;
    private static final Map<UUID, Long> IMMUNE_UNTIL = new HashMap<>();
    private static final int[] DY = {0, 1, -1, 2, -2};

    private NaniteReform() {
    }

    /** Return the colony that pays the player's Reform: the domain at the player, else the tether colony, or null. */
    public static @Nullable Colony colony(ServerLevel level, ServerPlayer player) {
        Colony c = Colonies.at(level, player.blockPosition());
        return c != null ? c : AegisTether.tether(level, player);
    }

    /** Return the free MU of the player's Reform domain (0 without one). */
    public static long matter(ServerLevel level, ServerPlayer player) {
        Colony c = colony(level, player);
        return c == null ? 0 : MatterBank.available(level, c);
    }

    /** Return true while the player is in the damage immunity that follows a Reform. */
    public static boolean immune(ServerPlayer player, long now) {
        Long until = IMMUNE_UNTIL.get(player.getUUID());
        return until != null && now < until;
    }

    /** Return true when the Reform cooldown of the player is over at that game time. */
    public static boolean ready(ServerPlayer player, long now) {
        return now >= player.getData(AutarkiaAttachments.REFORM_READY_AT);
    }

    /**
     * Run the Nanite Reform for a dying player; return true when it took place (the caller cancels the death), false for a
     * normal death (no full set, excluded source, cooldown running, no domain or not enough matter; nothing is spent).
     */
    public static boolean tryReform(ServerPlayer player, DamageSource source) {
        if (!AegisRule.fullSet(player) || AegisRule.excluded(source)) {
            return false;
        }
        ServerLevel level = player.level();
        long now = level.getGameTime();
        if (!ready(player, now)) {
            return false;
        }
        Colony c = colony(level, player);
        long cost = AutarkiaConfig.REFORM_MATTER_COST.get();
        if (c == null || !MatterBank.withdraw(level, c, cost, "Reform")) {
            return false;
        }
        Vec3 from = player.position();
        Vec3 to = destination(level, player, source.getSourcePosition());
        player.setHealth(player.getMaxHealth() * 0.5f);
        player.clearFire();
        for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect.getEffect());
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_TICKS, 0, false, false));
        IMMUNE_UNTIL.put(player.getUUID(), now + IMMUNITY_TICKS);
        player.setData(AutarkiaAttachments.REFORM_READY_AT, now + AutarkiaConfig.REFORM_COOLDOWN_SECONDS.get() * 20L);
        move(player, to);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        level.playSound(null, to.x, to.y, to.z, AutarkiaSounds.of("aegis.reform"), SoundSource.PLAYERS, 1f, 1f);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ReformFxPayload(player.getId(), from, to, now));
        EventTrigger.EQUIPMENT.get().fire(player, "reform");
        Autarkia.LOG.info("[aegis] nanite reform of {} from {} to {} for {} MU of colony {}", player.getScoreboardName(), BlockPos.containing(from).toShortString(),
            BlockPos.containing(to).toShortString(), cost, c.name);
        return true;
    }

    /** Move a server player to pos (a real connection teleports, a fake player is placed directly). */
    static void move(ServerPlayer player, Vec3 pos) {
        player.snapTo(pos.x, pos.y, pos.z);
        player.connection.teleport(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
    }

    /** Return the Reform spot of a player whose damage came from source (null = no position) under the rules of 07 section 3.2. */
    public static Vec3 destination(ServerLevel level, ServerPlayer player, @Nullable Vec3 source) {
        Vec3 at = player.position();
        double away;
        if (source != null && Math.hypot(at.x - source.x, at.z - source.z) > 1.0E-3) {
            away = Math.atan2(at.z - source.z, at.x - source.x);
        } else {
            away = level.getRandom().nextDouble() * Math.PI * 2;
        }
        int dist = AutarkiaConfig.REFORM_OFFSET_BLOCKS.get();
        List<Double> dirs = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            dirs.add(Math.PI / 4 * i);
        }
        double base = away;
        dirs.sort(Comparator.comparingDouble(d -> Math.abs(Math.atan2(Math.sin(d - base), Math.cos(d - base)))));
        for (double d : dirs) {
            BlockPos column = BlockPos.containing(at.x + Math.cos(d) * dist, at.y, at.z + Math.sin(d) * dist);
            for (int dy : DY) {
                BlockPos p = column.above(dy);
                if (valid(level, p)) {
                    return Vec3.atBottomCenterOf(p);
                }
            }
        }
        BlockPos death = BlockPos.containing(at);
        for (BlockPos p = death; p.getY() < level.getMaxY() - 1; p = p.above()) {
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) {
                return Vec3.atBottomCenterOf(p);
            }
        }
        return at;
    }

    private static boolean valid(ServerLevel level, BlockPos p) {
        if (!level.isInWorldBounds(p) || !level.isLoaded(p)) {
            return false;
        }
        BlockState feet = level.getBlockState(p);
        BlockState head = level.getBlockState(p.above());
        BlockState floor = level.getBlockState(p.below());
        return feet.isAir() && head.isAir() && floor.isFaceSturdy(level, p.below(), Direction.UP) && !hot(floor)
            && !level.getFluidState(p.below()).is(FluidTags.LAVA);
    }

    private static boolean hot(BlockState s) {
        return s.is(BlockTags.FIRE) || s.getFluidState().is(FluidTags.LAVA);
    }
}

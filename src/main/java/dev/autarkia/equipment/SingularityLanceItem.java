package dev.autarkia.equipment;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import com.klikli_dev.magicparticleslib.premade.particle.lightning.LightningParticleOptions;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.integration.AnthologyBridge;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.BeamVisualPayload;
import dev.autarkia.registry.AutarkiaComponents;
import dev.autarkia.registry.AutarkiaSounds;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * {@code singularity_lance} (07 sections 1.7 and 2.2): unbreakable, 50,000,000 FE. Hold use {@link #CHARGE_TICKS} ticks
 * (the charge is counted per user, lightning arcs gather at the muzzle every 4 ticks); then it fires by itself: it
 * spends {@link #SHOT_COST} FE and starts an anthology beam ({@link AnthologyBridge#lanceBeam}) aimed at the eye plus
 * look x {@link #RANGE}, {@link #BEAM_TICKS} ticks long, followed by {@link #COOLDOWN} ticks of cooldown. Releasing
 * earlier cancels at no cost. Each beam tick follows the current eye and look, sends a {@code beam_visual} and deals
 * {@link #DAMAGE} {@code autarkia:beam} damage once per shot (decision C9: the anthology settings carry damage 0) to each
 * living entity within {@link #PATH_RADIUS} of the beam or {@link #IMPACT_RADIUS} of its end with line of sight, never
 * to the owner, allies (team allies, tamed animals of the owner, players who manage a colony with the owner, decision
 * G13), creative or spectator players. Mode key: terrain-safe (default) or terrain-destroy. The anthology terrain mode
 * is DESTROY only in terrain-destroy mode with {@code weapons.lanceAllowTerrain} and {@code mobGriefing} on and while
 * the beam's box (eye to eye + look x 128, grown by 4 blocks) touches no domain square of any colony; otherwise NONE,
 * so inside any domain terrain-destroy acts as terrain-safe. DESTROY removes blocks within {@link #IMPACT_TERRAIN_RADIUS}
 * of the beam end each tick (at most 64 per tick, blast resistance at most 50, never block entities, no drops).
 */
public class SingularityLanceItem extends PoweredGeoItem implements ModalItem {
    public static final int CAPACITY = 50000000;
    public static final int INPUT = AegisTether.CHARGE;
    public static final int SHOT_COST = 1000000;
    public static final int CHARGE_TICKS = 60;
    public static final double RANGE = 128;
    public static final int BEAM_TICKS = 20;
    public static final int TRAVEL_TICKS = 4;
    public static final int COOLDOWN = 200;
    public static final float DAMAGE = 80f;
    public static final float PATH_RADIUS = 1.5F;
    public static final float IMPACT_RADIUS = 4.0F;
    public static final float KNOCKBACK = 1.0F;
    public static final float IMPACT_TERRAIN_RADIUS = 3.0F;
    public static final int MAX_TERRAIN_MUTATIONS = 64;
    public static final float MAX_TERRAIN_RESISTANCE = 50F;
    public static final int COLOR = 0xFF7FFFF0;
    private static final Map<UUID, Integer> CHARGING = new HashMap<>();
    private static final Map<UUID, Shot> SHOTS = new HashMap<>();

    /** One shot of a player: the beam, its terrain wish and the entities it already hurt. */
    private static final class Shot {
        final AnthologyBridge.Beam beam;
        final boolean destroy;
        final Set<Integer> hit = new HashSet<>();
        int ticks;
        int terrainTicks;
        @Nullable Vec3 end;
        boolean done;

        Shot(AnthologyBridge.Beam beam, boolean destroy) {
            this.beam = beam;
            this.destroy = destroy;
        }
    }

    /**
     * The state of a player's latest shot.
     *
     * @param ticks beam ticks run so far
     * @param terrainTicks beam ticks that ran in DESTROY mode
     * @param hurt entities hurt by the shot
     * @param end beam end of the latest tick, null before the first
     * @param done true once the beam ended
     */
    public record ShotState(int ticks, int terrainTicks, int hurt, @Nullable Vec3 end, boolean done) {
    }

    public SingularityLanceItem(Properties props) {
        super(props, CAPACITY, INPUT, 0, "singularity_lance", "fire");
    }

    /** Register the beam tick of the players and the logout clean-up (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                tickShot(sp.level(), sp);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            CHARGING.remove(e.getEntity().getUUID());
            SHOTS.remove(e.getEntity().getUUID());
        });
    }

    @Override
    public List<ToolMode> modes() {
        return List.of(ToolMode.TERRAIN_SAFE, ToolMode.TERRAIN_DESTROY);
    }

    @Override
    public float areaSpeed(ToolMode mode) {
        return 1f;
    }

    /** Return the charge ticks of the user's current hold, or -1 while not charging. */
    public static int charge(LivingEntity user) {
        return CHARGING.getOrDefault(user.getUUID(), -1);
    }

    /** Return true while the player's beam runs. */
    public static boolean firing(Player player) {
        Shot s = SHOTS.get(player.getUUID());
        return s != null && !s.done;
    }

    /** Return the state of the player's latest shot, or null when the player never fired. */
    public static @Nullable ShotState lastShot(Player player) {
        Shot s = SHOTS.get(player.getUUID());
        return s == null ? null : new ShotState(s.ticks, s.terrainTicks, s.hit.size(), s.end, s.done);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (energy(stack) < SHOT_COST || player.getCooldowns().isOnCooldown(stack) || firing(player)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel sl) {
            CHARGING.put(player.getUUID(), 0);
            triggerAnim(player, GeoItem.getOrAssignId(stack, sl), "main", "charge");
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), AutarkiaSounds.of("item.lance.charge"), SoundSource.PLAYERS, 1f, 0.6f);
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel sl) || !(user instanceof Player player) || !CHARGING.containsKey(player.getUUID())) {
            return;
        }
        int held = CHARGING.merge(player.getUUID(), 1, Integer::sum);
        if (held % 4 == 1) {
            chargeArc(sl, player);
        }
        if (held >= CHARGE_TICKS) {
            CHARGING.remove(player.getUUID());
            fire(sl, player, stack);
            player.stopUsingItem();
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (CHARGING.remove(user.getUUID()) != null && level instanceof ServerLevel sl) {
            stopTriggeredAnim(user, GeoItem.getOrAssignId(stack, sl), "main", "charge");
        }
        return false;
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity user, int remaining) {
        CHARGING.remove(user.getUUID());
    }

    private static Vec3 muzzle(Player player) {
        return player.getEyePosition().add(player.getLookAngle().scale(0.8)).add(0, -0.2, 0);
    }

    private static void chargeArc(ServerLevel sl, Player player) {
        Vec3 m = muzzle(player);
        RandomSource rnd = sl.getRandom();
        Vec3 from = m.add((rnd.nextDouble() - 0.5) * 2.4, (rnd.nextDouble() - 0.5) * 2.4, (rnd.nextDouble() - 0.5) * 2.4);
        sl.sendParticles(new LightningParticleOptions(m, COLOR, 0.5F, 0.75F, rnd.nextInt(), 4), from.x, from.y, from.z, 1, 0, 0, 0, 0);
    }

    /**
     * Fire a shot now: spend {@link #SHOT_COST} FE, start the beam at the player's eye aimed along the look, start the
     * cooldown, play the fire animation and sound. Return false (and change nothing) without the FE.
     */
    public static boolean fire(ServerLevel sl, Player player, ItemStack stack) {
        if (!spend(stack, SHOT_COST)) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        AnthologyBridge.Beam beam = AnthologyBridge.lanceBeam(player);
        beam.start(eye, eye.add(player.getLookAngle().scale(RANGE)), BEAM_TICKS);
        SHOTS.put(player.getUUID(), new Shot(beam, stack.getOrDefault(AutarkiaComponents.TOOL_MODE.get(), ToolMode.TERRAIN_SAFE) == ToolMode.TERRAIN_DESTROY));
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        if (stack.getItem() instanceof SingularityLanceItem lance) {
            lance.playWork(player, stack);
        }
        sl.playSound(null, player.getX(), player.getY(), player.getZ(), AutarkiaSounds.of("item.lance.fire"), SoundSource.PLAYERS, 1f, 0.7f);
        return true;
    }

    /** Run one beam tick of the player's shot (the player tick does this for real players); return true while it runs. */
    public static boolean tickShot(ServerLevel sl, Player player) {
        Shot s = SHOTS.get(player.getUUID());
        if (s == null || s.done) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        boolean destroy = s.destroy && terrainAllowed(sl, eye, look);
        Vec3[] end = {null};
        boolean[] over = {false};
        s.beam.tick(eye, look, destroy, e -> {
            if (e == null) {
                over[0] = true;
            } else {
                end[0] = e;
            }
        });
        if (end[0] != null) {
            s.ticks++;
            s.terrainTicks += destroy ? 1 : 0;
            s.end = end[0];
            hurt(sl, player, s, eye, end[0]);
            Vec3 m = muzzle(player);
            AutarkiaNetwork.toNear(sl, m, 160, new BeamVisualPayload(player.getId(), m, end[0], 2, COLOR, BeamVisualPayload.KIND_LANCE, 0.5f));
            sl.sendParticles(GlowParticleOptions.of(COLOR).size(0.3f).age(12), end[0].x, end[0].y, end[0].z, 4, 0.4, 0.4, 0.4, 0.05);
        }
        if (over[0]) {
            s.done = true;
        }
        return !s.done;
    }

    /** Return true when the beam may change terrain: config and mobGriefing on, no domain square of any colony near the beam. */
    public static boolean terrainAllowed(ServerLevel sl, Vec3 eye, Vec3 look) {
        return AutarkiaConfig.WEAPONS_LANCE_ALLOW_TERRAIN.get() && sl.getGameRules().get(GameRules.MOB_GRIEFING)
            && !inAnyDomain(sl, new AABB(eye, eye.add(look.scale(RANGE))).inflate(IMPACT_TERRAIN_RADIUS + 1));
    }

    /** Return true when the box touches a domain square (Core, beacon or outpost) of any colony in the level. */
    public static boolean inAnyDomain(Level level, AABB box) {
        int x0 = Mth.floor(box.minX);
        int x1 = Mth.floor(box.maxX);
        int z0 = Mth.floor(box.minZ);
        int z1 = Mth.floor(box.maxZ);
        for (Colony c : Colonies.registry().all()) {
            if (!c.dimension.equals(level.dimension())) {
                continue;
            }
            for (int[] q : Colonies.squares(c)) {
                if (q[0] - q[2] <= x1 && q[0] + q[2] >= x0 && q[1] - q[2] <= z1 && q[1] + q[2] >= z0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Return true when the beam must not hurt the entity: the owner, an ally, a creative or spectator player. */
    public static boolean spared(Player owner, Entity e) {
        if (e == owner || owner.isAlliedTo(e) || e instanceof OwnableEntity o && o.getOwner() == owner) {
            return true;
        }
        if (!(e instanceof Player q)) {
            return false;
        }
        return q.isCreative() || q.isSpectator()
            || Colonies.registry().all().stream().anyMatch(c -> c.canManage(owner.getUUID()) && c.canManage(q.getUUID()));
    }

    private static void hurt(ServerLevel sl, Player owner, Shot s, Vec3 eye, Vec3 end) {
        DamageSource source = sl.damageSources().source(AutarkiaDamage.BEAM, owner);
        Vec3 d = end.subtract(eye);
        double len = d.length();
        Vec3 dir = len < 1.0E-6 ? owner.getLookAngle() : d.scale(1 / len);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(IMPACT_RADIUS),
            e -> e.isAlive() && !s.hit.contains(e.getId()) && !spared(owner, e))) {
            Vec3 from = reach(e.getBoundingBox(), eye, dir, len, end);
            if (from == null || !sight(sl, owner, from, e.getBoundingBox().getCenter())) {
                continue;
            }
            s.hit.add(e.getId());
            e.setInvulnerableTime(0);
            if (e.hurtServer(sl, source, DAMAGE)) {
                e.knockback(KNOCKBACK, owner.getX() - e.getX(), owner.getZ() - e.getZ(), source, DAMAGE);
            }
        }
    }

    private static @Nullable Vec3 reach(AABB bb, Vec3 eye, Vec3 dir, double len, Vec3 end) {
        if (closest(bb, end).distanceToSqr(end) <= IMPACT_RADIUS * IMPACT_RADIUS) {
            return end;
        }
        for (double t = 1.0; t < len; t += 1.0) {
            Vec3 p = eye.add(dir.scale(t));
            if (closest(bb, p).distanceToSqr(p) <= PATH_RADIUS * PATH_RADIUS) {
                return p;
            }
        }
        return null;
    }

    private static Vec3 closest(AABB bb, Vec3 p) {
        return new Vec3(Mth.clamp(p.x, bb.minX, bb.maxX), Mth.clamp(p.y, bb.minY, bb.maxY), Mth.clamp(p.z, bb.minZ, bb.maxZ));
    }

    private static boolean sight(ServerLevel sl, Player owner, Vec3 from, Vec3 to) {
        return from.distanceToSqr(to) < 1.0E-6
            || sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getType() == HitResult.Type.MISS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        AegisTether.charge(level, entity, stack);
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        var main = new AnimationController<SingularityLanceItem>("main", 2,
            test -> test.setAndContinue(RawAnimation.begin().thenLoop("animation.singularity_lance.idle")));
        main.triggerableAnim("charge", RawAnimation.begin().thenPlayAndHold("animation.singularity_lance.charge"));
        main.triggerableAnim("fire", RawAnimation.begin().thenPlay("animation.singularity_lance.fire"));
        controllers.add(main);
    }
}

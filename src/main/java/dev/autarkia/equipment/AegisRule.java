package dev.autarkia.equipment;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colony;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.DomainPowerIndex;
import dev.autarkia.network.AegisStatusPayload;
import dev.autarkia.registry.AutarkiaAttachments;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.EventTrigger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The Aegis rule (07 section 3.1). Damage to a player in the full Aegis set that is not excluded ({@code generic_kill},
 * {@code fell_out_of_world}, {@code #minecraft:bypasses_invulnerability}, {@code #autarkia:aegis_bypass}) and that
 * reaches a tethered player ({@link AegisTether#tether}: a powered domain node within {@code aegis.tetherRange}) is paid
 * by the domain grid at {@code aegis.fePerDamage} FE per point of the incoming amount before armor, all or nothing
 * through {@link DomainPowerIndex#pay}; a paid hit cancels the {@link LivingIncomingDamageEvent}, so no damage and no
 * hurtServer knockback follow, and a {@link LivingKnockBackEvent} in the same tick is cancelled too. A paid hit opens a
 * repeat window of {@code aegis.repeatWindowTicks}: the same damage type in it costs only the part above the largest
 * paid amount and a smaller hit is free, like vanilla invulnerability frames (a cancelled event sets none). A payment
 * shows the gold glow ring at the chest, plays {@code aegis.absorb}, and adds the points to the player's total
 * ({@code equipment/paid_in_full} at 1,000). The same listener cancels all non-bypassing damage while the player is
 * locked in a Forge of Ascension fitting or immune after a Nanite Reform; a lethal hit that is not excluded runs the
 * Nanite Reform ({@link NaniteReform}).
 */
public final class AegisRule {
    public static final TagKey<DamageType> BYPASS = TagKey.create(Registries.DAMAGE_TYPE, Autarkia.id("aegis_bypass"));
    public static final long GREEN_FE = 1_000_000L;
    public static final long PAID_IN_FULL_MILLI = 1_000_000L;
    private static final int PULSE_COLOR = 0xFFF5D76E;
    private static final Map<UUID, Paid> LAST = new HashMap<>();
    private static final Map<UUID, Long> KNOCKBACK_TICK = new HashMap<>();

    /** Aegis state of a player for the HUD and {@code aegis_status} (07 section 2.6): state byte as in {@link AegisStatusPayload}. */
    public record Status(byte state, long gridFe, long reformReadyAt, int matter) {
    }

    private record Paid(Optional<ResourceKey<DamageType>> type, float amount, long time) {
    }

    private AegisRule() {
    }

    /** Register the damage, knockback, death and logout listeners (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, AegisRule::incoming);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, AegisRule::knockback);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (LivingDeathEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && NaniteReform.tryReform(p, e.getSource())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            LAST.remove(e.getEntity().getUUID());
            KNOCKBACK_TICK.remove(e.getEntity().getUUID());
        });
    }

    /** Return true when the player wears all four {@code aegis_*} armor pieces. */
    public static boolean fullSet(Player player) {
        return worn(player, EquipmentSlot.HEAD, AutarkiaItems.AEGIS_HELMET.get()) && worn(player, EquipmentSlot.CHEST, AutarkiaItems.AEGIS_CHEST.get())
            && worn(player, EquipmentSlot.LEGS, AutarkiaItems.AEGIS_LEGS.get()) && worn(player, EquipmentSlot.FEET, AutarkiaItems.AEGIS_BOOTS.get());
    }

    private static boolean worn(Player player, EquipmentSlot slot, Item item) {
        return player.getItemBySlot(slot).is(item);
    }

    /** Return true for a damage source the Aegis rule and the Nanite Reform never handle (07 section 3.1 "Not covered"). */
    public static boolean excluded(DamageSource source) {
        return source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
            || source.is(BYPASS);
    }

    /**
     * Return the Aegis state of a player: NONE without the full set, RED without a tether, AMBER while the tether grid holds
     * less than {@link #GREEN_FE}, GREEN otherwise; with the tether grid FE, the Reform ready time and the free matter of the
     * Reform domain (clamped to an int).
     */
    public static Status status(ServerPlayer player) {
        ServerLevel level = player.level();
        long readyAt = player.getData(AutarkiaAttachments.REFORM_READY_AT);
        int matter = (int) Math.min(Integer.MAX_VALUE, NaniteReform.matter(level, player));
        if (!fullSet(player)) {
            return new Status(AegisStatusPayload.STATE_NONE, 0, readyAt, matter);
        }
        Colony c = AegisTether.tether(level, player);
        if (c == null) {
            return new Status(AegisStatusPayload.STATE_RED, 0, readyAt, matter);
        }
        long grid = DomainPowerIndex.gridStored(level, c);
        return new Status(grid >= GREEN_FE ? AegisStatusPayload.STATE_GREEN : AegisStatusPayload.STATE_AMBER, grid, readyAt, matter);
    }

    /** Return the FE a hit of that many damage points costs: points x {@code aegis.fePerDamage}, rounded up. */
    public static long cost(float points) {
        return (long) Math.ceil(points * (double) AutarkiaConfig.AEGIS_FE_PER_DAMAGE.get());
    }

    /** Return the damage points the tether paid for the player in total. */
    public static double paidPoints(Player player) {
        return player.getData(AutarkiaAttachments.AEGIS_PAID_MILLI) / 1000.0;
    }

    private static void incoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        ServerLevel level = p.level();
        DamageSource src = e.getSource();
        long now = level.getGameTime();
        if (!src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && (ForgeOfAscensionBlockEntity.locked(p) || NaniteReform.immune(p, now))) {
            e.setCanceled(true);
            KNOCKBACK_TICK.put(p.getUUID(), now);
            return;
        }
        if (!fullSet(p) || excluded(src)) {
            return;
        }
        Colony c = AegisTether.tether(level, p);
        if (c == null) {
            return;
        }
        float amount = e.getAmount();
        Optional<ResourceKey<DamageType>> type = src.typeHolder().unwrapKey();
        Paid last = LAST.get(p.getUUID());
        boolean repeat = last != null && last.type.equals(type) && now - last.time < AutarkiaConfig.AEGIS_REPEAT_WINDOW_TICKS.get();
        float points = repeat ? amount - last.amount : amount;
        if (points <= 0) {
            e.setCanceled(true);
            KNOCKBACK_TICK.put(p.getUUID(), now);
            return;
        }
        long fe = cost(points);
        if (!DomainPowerIndex.pay(level, c, fe)) {
            return;
        }
        DomainPowerIndex.recordDamage(level, c, fe, Math.round(points * 1000.0));
        e.setCanceled(true);
        LAST.put(p.getUUID(), repeat ? new Paid(type, amount, last.time) : new Paid(type, amount, now));
        KNOCKBACK_TICK.put(p.getUUID(), now);
        pulse(level, p);
        long before = p.getData(AutarkiaAttachments.AEGIS_PAID_MILLI);
        long after = before + Math.round(points * 1000.0);
        p.setData(AutarkiaAttachments.AEGIS_PAID_MILLI, after);
        if (before < PAID_IN_FULL_MILLI && after >= PAID_IN_FULL_MILLI) {
            EventTrigger.EQUIPMENT.get().fire(p, "tether_1000");
        }
    }

    private static void knockback(LivingKnockBackEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Long t = KNOCKBACK_TICK.get(p.getUUID());
            if (t != null && t == p.level().getGameTime()) {
                e.setCanceled(true);
            }
        }
    }

    private static void pulse(ServerLevel level, ServerPlayer p) {
        double y = p.getY() + p.getBbHeight() * 0.6;
        GlowParticleOptions glow = GlowParticleOptions.of(PULSE_COLOR).size(0.15f).age(14);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            level.sendParticles(glow, p.getX() + Math.cos(a) * 0.6, y, p.getZ() + Math.sin(a) * 0.6, 1, 0, 0, 0, 0);
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), AutarkiaSounds.of("aegis.absorb"), SoundSource.PLAYERS, 1f, 1f);
    }
}

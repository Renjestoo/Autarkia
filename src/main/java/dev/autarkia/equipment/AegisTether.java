package dev.autarkia.equipment;

import dev.autarkia.colony.Colony;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.DomainPowerIndex;
import dev.autarkia.network.AegisStatusPayload;
import dev.autarkia.network.AutarkiaNetwork;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Tether services of the Aegis items (07 sections 1.7, 2.3 and 2.6, decision G13). The tether colony of a holder is
 * {@link DomainPowerIndex#tether} within {@code aegis.tetherRange} of the holder's position, looked up at most once per
 * holder and game tick. A player holding an Aegis item anywhere in the inventory charges it from that colony's domain
 * grid at {@link #CHARGE} FE/t per item (all or nothing per tick through {@link DomainPowerIndex#pay}). Every
 * {@link #STATUS_PERIOD} ticks each player in a full Aegis set gets an {@code aegis_status} built from
 * {@code AegisRule.status}; when the set becomes incomplete the player gets one last {@code NONE} state, then nothing.
 */
public final class AegisTether {
    public static final int CHARGE = 50000;
    public static final int STATUS_PERIOD = 10;
    private static final Map<UUID, Cached> CACHE = new HashMap<>();
    private static final Set<UUID> SENT = new HashSet<>();

    private record Cached(long time, ResourceKey<Level> dimension, @Nullable Colony colony) {
    }

    private AegisTether() {
    }

    /** Register the status loop and the logout clean-up (game bus). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                status(sp);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            SENT.remove(e.getEntity().getUUID());
            CACHE.remove(e.getEntity().getUUID());
        });
    }

    /** Return the colony whose powered domain node tethers the holder this tick, or null. */
    public static @Nullable Colony tether(ServerLevel level, Entity holder) {
        long now = level.getGameTime();
        Cached c = CACHE.get(holder.getUUID());
        if (c != null && c.time == now && c.dimension.equals(level.dimension())) {
            return c.colony;
        }
        if (CACHE.size() > 256) {
            CACHE.values().removeIf(v -> v.time != now);
        }
        Colony colony = DomainPowerIndex.tether(level, holder.position(), AutarkiaConfig.AEGIS_TETHER_RANGE.get());
        CACHE.put(holder.getUUID(), new Cached(now, level.dimension(), colony));
        return colony;
    }

    /** Return true while a powered domain node tethers the holder. */
    public static boolean tethered(ServerLevel level, Entity holder) {
        return tether(level, holder) != null;
    }

    /** Return the FE capacity of an FE item or suit piece, 0 for any other stack. */
    public static int capacity(ItemStack stack) {
        return stack.getItem() instanceof PoweredItem p ? p.capacity() : stack.getItem() instanceof SuitItem s ? s.capacity() : 0;
    }

    /**
     * Charge one Aegis item of a player from the tether grid: min({@link #CHARGE}, free room) FE when the grid pays all of
     * it. Return the FE added (0 without a player holder, tether, room or payment).
     */
    public static int charge(ServerLevel level, Entity holder, ItemStack stack) {
        if (!(holder instanceof Player p) || p.isSpectator()) {
            return 0;
        }
        int room = capacity(stack) - PoweredItem.energy(stack);
        if (room <= 0) {
            return 0;
        }
        Colony c = tether(level, holder);
        int amount = Math.min(CHARGE, room);
        if (c == null || !DomainPowerIndex.pay(level, c, amount)) {
            return 0;
        }
        PoweredItem.setEnergy(stack, PoweredItem.energy(stack) + amount);
        return amount;
    }

    /**
     * Send the Aegis status of a player when the game time is a multiple of {@link #STATUS_PERIOD}: the rule part's status
     * while the full set is worn, one {@code NONE} state right after it stops being worn. Return the payload sent, or null.
     */
    public static @Nullable AegisStatusPayload status(ServerPlayer p) {
        if (p.level().getGameTime() % STATUS_PERIOD != 0) {
            return null;
        }
        AegisStatusPayload payload;
        if (AegisRule.fullSet(p)) {
            AegisRule.Status s = AegisRule.status(p);
            payload = new AegisStatusPayload(s.state(), s.gridFe(), s.reformReadyAt(), s.matter());
            SENT.add(p.getUUID());
        } else if (SENT.remove(p.getUUID())) {
            payload = AegisStatusPayload.NONE;
        } else {
            return null;
        }
        AutarkiaNetwork.toPlayer(p, payload);
        return payload;
    }
}

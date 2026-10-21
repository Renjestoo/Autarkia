package dev.autarkia.colony;

import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.EventTrigger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/** Colony feedback: advancement events for the owner and trusted players, Core sounds and alerts to online members. */
public final class ColonyEvents {
    private ColonyEvents() {
    }

    private static List<ServerPlayer> members(Colony c) {
        MinecraftServer server = Colonies.server();
        List<ServerPlayer> out = new ArrayList<>();
        if (server == null) {
            return out;
        }
        List<UUID> ids = new ArrayList<>(c.trusted);
        ids.add(0, c.owner);
        for (UUID id : ids) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /** Fire {@code autarkia:colony_event} for the online owner and trusted players. */
    public static void fire(Colony c, String event) {
        members(c).forEach(p -> EventTrigger.COLONY.get().fire(p, event));
    }

    /** Fire {@code autarkia:worker_event} for the online owner and trusted players. */
    public static void worker(Colony c, String event) {
        members(c).forEach(p -> EventTrigger.WORKER.get().fire(p, event));
    }

    /** Play a colony sound at the Core. */
    public static void sound(Colony c, String sound) {
        ServerLevel level = Colonies.level(c);
        if (level != null) {
            level.playSound(null, c.corePos, AutarkiaSounds.of(sound), SoundSource.BLOCKS, 1f, 1f);
        }
    }

    /** Send an overlay alert to online members inside the domain and to the board log. */
    public static void alert(Colony c, String key, Object... args) {
        ColonyRuntime.of(c).log(Component.translatable(key, args).getString());
        for (ServerPlayer p : members(c)) {
            if (p.level().dimension().equals(c.dimension) && Colonies.inside(c, p.blockPosition())) {
                p.sendOverlayMessage(Component.translatable(key, args));
            }
        }
    }
}

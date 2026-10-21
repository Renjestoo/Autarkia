package dev.autarkia.logistics;

import dev.autarkia.registry.AutarkiaAdvancements;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The player who linked a transport with the wrench (a cable car line or a launcher target). Its advancements go to the
 * players within 32 blocks of the event and to this player: the online player with the saved id, else the player object
 * that made the link while it has not disconnected.
 */
public final class LinkerRef {
    private @Nullable UUID id;
    private @Nullable ServerPlayer player;

    /** Remember the player who made the link. */
    public void set(ServerPlayer p) {
        id = p.getUUID();
        player = p;
    }

    /** Forget the player (the link was removed). */
    public void clear() {
        id = null;
        player = null;
    }

    /** Return the linking player when reachable, else null. */
    public @Nullable ServerPlayer resolve(ServerLevel level) {
        if (id == null) {
            return null;
        }
        ServerPlayer online = level.getServer().getPlayerList().getPlayer(id);
        if (online != null) {
            return online;
        }
        return player != null && !player.hasDisconnected() && player.getUUID().equals(id) ? player : null;
    }

    /** Award autarkia:path to the players near the event and to the linking player. */
    public void award(ServerLevel level, Vec3 at, String path) {
        AutarkiaAdvancements.awardNear(level, at, path);
        ServerPlayer p = resolve(level);
        if (p != null) {
            AutarkiaAdvancements.award(p, path);
        }
    }

    /** Save the player id. */
    public void save(ValueOutput out) {
        if (id != null) {
            out.store("linker", UUIDUtil.CODEC, id);
        }
    }

    /** Load the player id; the player object is not saved. */
    public void load(ValueInput in) {
        id = in.read("linker", UUIDUtil.CODEC).orElse(null);
        player = null;
    }
}

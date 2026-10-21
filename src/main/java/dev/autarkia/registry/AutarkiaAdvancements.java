package dev.autarkia.registry;

import dev.autarkia.Autarkia;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Grants the code-driven advancements (criterion "done", trigger minecraft:impossible). */
public final class AutarkiaAdvancements {
    private AutarkiaAdvancements() {
    }

    /** Award autarkia:<path> to the player. */
    public static void award(ServerPlayer player, String path) {
        AdvancementHolder h = player.level().getServer().getAdvancements().get(Autarkia.id(path));
        if (h == null) {
            throw new IllegalStateException("Advancement autarkia:" + path + " is not loaded");
        }
        player.getAdvancements().award(h, "done");
    }

    /** Award autarkia:<path> to every player within 32 blocks of a world event. */
    public static void awardNear(ServerLevel level, Vec3 at, String path) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(at, at).inflate(32))) {
            award(p, path);
        }
    }
}

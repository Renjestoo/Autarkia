package dev.autarkia.client;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import dev.autarkia.client.render.GhostBlocks;
import dev.autarkia.network.ColonySyncPayload;
import dev.autarkia.network.PreviewRequestPayload;
import dev.autarkia.network.ProjectPreviewPayload;
import dev.autarkia.registry.AutarkiaItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Client colony state: the last {@code colony_sync} data per tab, the ghost preview toggles (600 ticks, refreshed
 * every 40 ticks; ghosts live 60 ticks so a missed refresh removes them, 05 section 4.1), the planning-tablet previews
 * and the glow line along each planned span.
 */
public final class ClientColony {
    private static final Map<Integer, CompoundTag> TABS = new HashMap<>();
    private static final Map<UUID, Long> TOGGLES = new HashMap<>();
    private static final Map<UUID, List<ProjectPreviewPayload.SpanLine>> SPANS = new HashMap<>();
    private static UUID colony;
    private static int version;

    private ClientColony() {
    }

    /** Store synced tab data. */
    public static void sync(ColonySyncPayload p) {
        colony = p.colony();
        TABS.put(p.tab(), p.data());
        version++;
    }

    /** Return the data of a tab, or an empty tag. */
    public static CompoundTag tab(int tab) {
        return TABS.getOrDefault(tab, new CompoundTag());
    }

    /** Return the colony id of the open screen, or null. */
    public static UUID colony() {
        return colony;
    }

    /** Return a counter that changes with each sync (screens rebuild their widgets). */
    public static int version() {
        return version;
    }

    /** Toggle the ghost preview of a project on for 600 ticks and ask the server now. */
    public static void show(UUID project) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        TOGGLES.put(project, mc.level.getGameTime() + 600);
        ClientPacketDistributor.sendToServer(new PreviewRequestPayload(Optional.of(project)));
    }

    /** Apply a preview: replace the project's ghosts (60-tick lifetime) and remember its spans; empty clears at once. */
    public static void preview(ProjectPreviewPayload p) {
        GhostBlocks.clear(p.projectId());
        SPANS.remove(p.projectId());
        if (p.entries().isEmpty() && p.spans().isEmpty()) {
            TOGGLES.remove(p.projectId());
            return;
        }
        for (ProjectPreviewPayload.Ghost g : p.entries()) {
            GhostBlocks.add(p.projectId(), g.blockPos(), Block.stateById(g.stateId()), 60, p.argb(), 64, 48);
        }
        SPANS.put(p.projectId(), p.spans());
    }

    /** Client tick: refresh toggled previews every 40 ticks, keep tablet previews alive, draw span glow lines. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        long now = mc.level.getGameTime();
        TOGGLES.entrySet().removeIf(e -> {
            boolean expired = now > e.getValue();
            if (expired) {
                GhostBlocks.clear(e.getKey());
                SPANS.remove(e.getKey());
            }
            return expired;
        });
        if (now % 40 == 0) {
            for (UUID id : new ArrayList<>(TOGGLES.keySet())) {
                ClientPacketDistributor.sendToServer(new PreviewRequestPayload(Optional.of(id)));
            }
            if (mc.player.getMainHandItem().is(AutarkiaItems.PLANNING_TABLET.get()) || mc.player.getOffhandItem().is(AutarkiaItems.PLANNING_TABLET.get())) {
                ClientPacketDistributor.sendToServer(new PreviewRequestPayload(Optional.empty()));
            }
        }
        SPANS.keySet().removeIf(id -> GhostBlocks.count(id) == 0 && !TOGGLES.containsKey(id));
        if (now % 10 == 0) {
            for (List<ProjectPreviewPayload.SpanLine> lines : SPANS.values()) {
                for (ProjectPreviewPayload.SpanLine s : lines) {
                    Vec3 a = Vec3.atCenterOf(BlockPos.of(s.a()));
                    Vec3 b = Vec3.atCenterOf(BlockPos.of(s.b()));
                    int n = (int) Math.ceil(a.distanceTo(b) / 0.5);
                    for (int i = 0; i <= n; i++) {
                        Vec3 p = a.lerp(b, (double) i / Math.max(1, n));
                        mc.level.addParticle(GlowParticleOptions.of(0xFF4DCCFF).size(0.12f).age(12), p.x, p.y, p.z, 0, 0, 0);
                    }
                }
            }
        }
    }

    /** Forget everything (log out). */
    public static void clear() {
        TABS.clear();
        TOGGLES.clear();
        SPANS.clear();
        colony = null;
    }
}

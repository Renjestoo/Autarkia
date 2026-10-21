package dev.autarkia.energy;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.DomainFlashPayload;
import dev.autarkia.registry.AutarkiaAdvancements;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Energy API of the T6 grid (02 section 6.4): explosion absorption by the Singularity Core. */
public final class EnergyApi {
    private EnergyApi() {
    }

    /** Return the FE an explosion of radius r gives: {@code singularityFePerExplosionRadiusSq x r^2}, rounded. */
    public static long explosionGain(float radius) {
        return Math.round(AutarkiaConfig.SINGULARITY_FE_PER_EXPLOSION_RADIUS_SQ.get() * (double) radius * radius);
    }

    /**
     * Absorb an explosion into the colony's domain grid (02 section 6.4, 05 section 8.6). Returns false when no core of the
     * colony grid is ONLINE. Otherwise the gain fills the ONLINE cores' buffers (lowest position first, capped at their
     * room), every {@code industrial_lamp} in loaded domain chunks flashes for 10 ticks ({@code domain_flash} to the players
     * in the domain), the first core plays {@code singularity.drink}, {@code stats.absorbed} grows by 1 and
     * {@code power/drink_the_blast} goes to the players within 32 blocks of that core and to the online owner and trusted
     * players of the colony. {@code insideCount} (affected blocks inside the domain) goes only to the log (decision G5).
     */
    public static boolean absorbExplosion(ServerLevel level, Colony colony, float radius, int insideCount) {
        List<SingularityCoreBlockEntity> cores = DomainPowerIndex.onlineCores(level, colony);
        if (cores.isEmpty()) {
            return false;
        }
        long gain = explosionGain(radius);
        long added = 0;
        for (SingularityCoreBlockEntity c : cores) {
            added += c.fill(gain - added);
        }
        List<BlockPos> lamps = lamps(level, colony);
        DomainFlashPayload flash = new DomainFlashPayload(lamps);
        Set<ServerPlayer> award = new LinkedHashSet<>();
        for (ServerPlayer p : level.players()) {
            if (Colonies.inside(colony, p.blockPosition())) {
                AutarkiaNetwork.toPlayer(p, flash);
                award.add(p);
            }
            if (colony.canManage(p.getUUID())) {
                award.add(p);
            }
        }
        SingularityCoreBlockEntity first = cores.get(0);
        Vec3 at = Vec3.atCenterOf(first.getBlockPos().above(2));
        award.addAll(level.getEntitiesOfClass(ServerPlayer.class, new AABB(at, at).inflate(32)));
        first.recordAbsorb(added, radius, lamps.size());
        colony.stat("absorbed", 1);
        Colonies.changed();
        award.forEach(p -> AutarkiaAdvancements.award(p, "power/drink_the_blast"));
        String line = String.format(Locale.ROOT, "The Singularity Core drank an explosion: radius %.1f, +%d FE, %d domain blocks kept, %d lamps", radius, added,
            insideCount, lamps.size());
        ColonyRuntime.of(colony).log(line);
        Autarkia.LOG.info("[grid] {} ({})", line, colony.name);
        return true;
    }

    /** Return the positions of the industrial lamps in the loaded chunks of the colony's domain, at most 256, lowest first. */
    public static List<BlockPos> lamps(ServerLevel level, Colony colony) {
        List<BlockPos> out = new ArrayList<>();
        for (long packed : Colonies.chunks(colony)) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(packed), ChunkPos.getZ(packed));
            if (chunk == null) {
                continue;
            }
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be instanceof IndustrialLampBlockEntity && Colonies.inside(colony, be.getBlockPos())) {
                    out.add(be.getBlockPos().immutable());
                }
            }
        }
        out.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
        return out.size() > DomainFlashPayload.MAX_LAMPS ? List.copyOf(out.subList(0, DomainFlashPayload.MAX_LAMPS)) : out;
    }
}

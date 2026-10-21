package dev.autarkia.energy;

import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * Per-level index of the powered T6 domains (02 section 6.4, 07 sections 3.1 and 3.4, decisions G4 and G14). A colony
 * grid network is a network with a node inside the colony's domain; it is powered while it holds an ONLINE
 * {@link SingularityCoreBlockEntity}. The online flag and the tether nodes of each colony are cached and refreshed every
 * 20 ticks and right after a core changes state ({@link #changed}); stored FE is read live, at most once per tick and
 * colony. Domain nodes for the tether (G4) are the Colony Core, outpost cores, domain beacons and phase nodes whose block,
 * or a connector within 1 block of it, is a node or member of a powered network.
 */
public final class DomainPowerIndex {
    private static final Map<ServerLevel, DomainPowerIndex> LEVELS = new IdentityHashMap<>();
    private final Set<UUID> online = new LinkedHashSet<>();
    private final Map<UUID, List<Vec3>> nodes = new HashMap<>();
    private final Map<UUID, long[]> stored = new HashMap<>();
    private boolean dirty = true;
    private long refreshedAt = Long.MIN_VALUE / 2;

    private DomainPowerIndex() {
    }

    private static DomainPowerIndex of(ServerLevel level) {
        DomainPowerIndex idx = LEVELS.computeIfAbsent(level, l -> new DomainPowerIndex());
        if (idx.dirty || level.getGameTime() - idx.refreshedAt >= 20) {
            idx.refresh(level);
        }
        return idx;
    }

    /** Mark the index of the level stale; the next query rebuilds it (a core changed state). */
    public static void changed(ServerLevel level) {
        DomainPowerIndex idx = LEVELS.get(level);
        if (idx != null) {
            idx.dirty = true;
        }
    }

    /** Drop every cached index (server stop or level unload). */
    static void clear(@Nullable ServerLevel level) {
        if (level == null) {
            LEVELS.clear();
        } else {
            LEVELS.remove(level);
        }
    }

    /** Refresh the index of the level when 20 ticks have passed since the last refresh (level tick). */
    static void tick(ServerLevel level) {
        if (Colonies.server() != null) {
            of(level);
        }
    }

    /** Return the colony whose domain holds pos when its grid has an ONLINE core, or null. */
    public static @Nullable Colony poweredColony(ServerLevel level, BlockPos pos) {
        if (Colonies.server() == null) {
            return null;
        }
        Colony c = Colonies.at(level, pos);
        return c != null && coreOnline(level, c) ? c : null;
    }

    /** Return true while a colony grid network of the colony holds an ONLINE core (cached flag). */
    public static boolean coreOnline(ServerLevel level, Colony colony) {
        return Colonies.server() != null && colony.dimension.equals(level.dimension()) && of(level).online.contains(colony.id);
    }

    /** Return the FE in the ONLINE cores' buffers plus the storage members of the powered colony grid networks (read live). */
    public static long gridStored(ServerLevel level, Colony colony) {
        if (Colonies.server() == null || !colony.dimension.equals(level.dimension())) {
            return 0;
        }
        DomainPowerIndex idx = LEVELS.computeIfAbsent(level, l -> new DomainPowerIndex());
        long now = level.getGameTime();
        long[] memo = idx.stored.get(colony.id);
        if (memo != null && memo[0] == now) {
            return memo[1];
        }
        long sum = 0;
        for (SingularityCoreBlockEntity core : onlineCores(level, colony)) {
            sum += core.buffer();
        }
        for (GridMember m : storage(level, colony)) {
            sum += m.gridBuffer().stored();
        }
        idx.stored.put(colony.id, new long[] {now, sum});
        return sum;
    }

    /**
     * Pay fe from the domain grid, all or nothing: the ONLINE core buffers first (lowest position first), then the storage
     * members of the powered networks. Return true when paid. A payment counts no damage points; a damage payment is
     * counted by its caller through {@link #recordDamage}.
     */
    public static boolean pay(ServerLevel level, Colony colony, long fe) {
        if (fe <= 0) {
            return true;
        }
        List<SingularityCoreBlockEntity> cores = onlineCores(level, colony);
        if (cores.isEmpty()) {
            return false;
        }
        List<GridMember> store = storage(level, colony);
        long total = 0;
        for (SingularityCoreBlockEntity c : cores) {
            total += c.buffer();
        }
        for (GridMember m : store) {
            total += m.gridBuffer().stored();
        }
        if (total < fe) {
            return false;
        }
        long left = fe;
        for (SingularityCoreBlockEntity c : cores) {
            left -= c.energy().drain((int) Math.min(Integer.MAX_VALUE, left));
        }
        for (GridMember m : store) {
            if (left <= 0) {
                break;
            }
            left -= m.gridBuffer().drain((int) Math.min(Integer.MAX_VALUE, left));
        }
        LEVELS.computeIfAbsent(level, l -> new DomainPowerIndex()).stored.remove(colony.id);
        return true;
    }

    /**
     * Count a damage payment that {@link #pay} just made: fe and the damage points in thousandths go to the first ONLINE
     * core and the points to the colony stat {@code singularity.points_milli} (sensor {@code singularity.absorbed_points}).
     */
    public static void recordDamage(ServerLevel level, Colony colony, long fe, long milli) {
        List<SingularityCoreBlockEntity> cores = onlineCores(level, colony);
        if (!cores.isEmpty()) {
            cores.get(0).recordPayment(fe, milli);
        }
        colony.stat("singularity.points_milli", milli);
        Colonies.changed();
    }

    /** Return the colony of the nearest domain node (G4) within range of pos (3D distance) in the level, or null. */
    public static @Nullable Colony tether(ServerLevel level, Vec3 pos, double range) {
        if (Colonies.server() == null) {
            return null;
        }
        DomainPowerIndex idx = of(level);
        UUID best = null;
        double bestD = range * range;
        for (Map.Entry<UUID, List<Vec3>> e : idx.nodes.entrySet()) {
            for (Vec3 n : e.getValue()) {
                double d = n.distanceToSqr(pos);
                if (d <= bestD) {
                    bestD = d;
                    best = e.getKey();
                }
            }
        }
        return best == null ? null : Colonies.registry().get(best);
    }

    /** Return the ONLINE cores on the colony grid networks, lowest position first (read live). */
    public static List<SingularityCoreBlockEntity> onlineCores(ServerLevel level, Colony colony) {
        Set<SingularityCoreBlockEntity> out = Collections.newSetFromMap(new IdentityHashMap<>());
        for (GridNetwork net : networks(level, colony)) {
            for (GridNetwork.Attached a : net.members) {
                if (a.member instanceof SingularityCoreBlockEntity c && c.state() == SingularityCoreBlockEntity.State.ONLINE && c.gridValid()) {
                    out.add(c);
                }
            }
        }
        List<SingularityCoreBlockEntity> list = new ArrayList<>(out);
        list.sort(Comparator.comparingLong(c -> c.getBlockPos().asLong()));
        return list;
    }

    private static List<GridMember> storage(ServerLevel level, Colony colony) {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<GridMember> out = new ArrayList<>();
        for (GridNetwork net : networks(level, colony)) {
            for (GridNetwork.Attached a : net.members) {
                if (a.member.gridValid() && a.member.gridRole() == GridMember.Role.STORAGE && seen.add(a.member.gridBuffer())) {
                    out.add(a.member);
                }
            }
        }
        return out;
    }

    private static List<GridNetwork> networks(ServerLevel level, Colony colony) {
        List<GridNetwork> out = new ArrayList<>();
        for (GridNetwork net : Grid.of(level).networks()) {
            if (powered(net) && touches(net, colony)) {
                out.add(net);
            }
        }
        return out;
    }

    private static boolean powered(GridNetwork net) {
        for (GridNetwork.Attached a : net.members) {
            if (a.member instanceof SingularityCoreBlockEntity c && c.state() == SingularityCoreBlockEntity.State.ONLINE && c.gridValid()) {
                return true;
            }
        }
        return false;
    }

    private static boolean touches(GridNetwork net, Colony colony) {
        for (int i = 0; i < net.nodes.size(); i++) {
            if (Colonies.inside(colony, BlockPos.of(net.nodes.getLong(i)))) {
                return true;
            }
        }
        return false;
    }

    private void refresh(ServerLevel level) {
        dirty = false;
        refreshedAt = level.getGameTime();
        online.clear();
        nodes.clear();
        stored.clear();
        for (Colony c : Colonies.registry().all()) {
            if (!c.dimension.equals(level.dimension())) {
                continue;
            }
            List<GridNetwork> nets = networks(level, c);
            if (nets.isEmpty()) {
                continue;
            }
            online.add(c.id);
            LongOpenHashSet netNodes = new LongOpenHashSet();
            Set<Object> members = Collections.newSetFromMap(new IdentityHashMap<>());
            for (GridNetwork net : nets) {
                netNodes.addAll(net.nodes);
                net.members.forEach(a -> members.add(a.member));
            }
            List<BlockPos> candidates = new ArrayList<>();
            candidates.add(c.corePos);
            c.outposts.forEach(o -> candidates.add(o.corePos));
            c.beacons.forEach(b -> candidates.add(b.pos()));
            for (long n : netNodes) {
                if (level.isLoaded(BlockPos.of(n)) && level.getBlockState(BlockPos.of(n)).getBlock() instanceof PhaseNodeBlock) {
                    candidates.add(BlockPos.of(n));
                }
            }
            List<Vec3> out = new ArrayList<>();
            for (BlockPos p : candidates) {
                if (level.isLoaded(p) && domainNode(level, p, netNodes, members)) {
                    out.add(Vec3.atCenterOf(p));
                }
            }
            nodes.put(c.id, out);
        }
    }

    private static boolean domainNode(ServerLevel level, BlockPos p, LongOpenHashSet netNodes, Set<Object> members) {
        if (netNodes.contains(p.asLong())) {
            return true;
        }
        BlockEntity be = level.getBlockEntity(p);
        if (be != null && members.contains(be)) {
            return true;
        }
        BlockState st = level.getBlockState(p);
        Set<BlockPos> footprint = st.getBlock() instanceof BaseMultiblock mb && st.hasProperty(BaseMultiblock.CENTER)
            ? mb.getMultiblockShape(level, p, st).getGlobalPositions() : Set.of(p);
        for (BlockPos f : footprint) {
            for (BlockPos q : BlockPos.betweenClosed(f.offset(-1, -1, -1), f.offset(1, 1, 1))) {
                if (netNodes.contains(q.asLong()) && level.getBlockState(q).getBlock() instanceof GridConnector) {
                    return true;
                }
            }
        }
        return false;
    }
}

package dev.autarkia.colony;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GridConnector;
import dev.autarkia.energy.GridMember;
import dev.autarkia.energy.GridNetwork;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.storage.RackBlock;
import dev.autarkia.storage.RackBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The T3 corrective handlers of 05 sections 3.3 and 6.2: {@code fuse_box} (template 14) cuts a snapped-and-restrung
 * span of a line without a fuse box and puts a {@code fuse_box} on its source side with two new spans, and
 * {@code rack_upgrade} (template 15) replaces the wood racks of a failing row with steel racks, props them first and
 * rebuilds the collapsed positions.
 */
public final class CorrectiveProjects {
    private CorrectiveProjects() {
    }

    /**
     * Handler {@code fuse_box}: for each {@code cable_snap} memory with weight at least 0.75 (one event, decayed for at
     * most about 3 colony days) whose two nodes still stand
     * and whose network has no fuse box, the end nearer a generator is the source side; the fuse box goes on the first
     * ground cell along the line 2 to 6 blocks from it from which both new chords are clear and within the tier's span.
     */
    public static @Nullable Project fuseBox(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        double half = AutarkiaConfig.MEMORY_HALF_LIFE_TICKS.get();
        for (Colony.Memory m : c.memories) {
            if (!m.type().equals("cable_snap") || m.weightAt(c.clock, half) < 0.75) {
                continue;
            }
            long[] ends = ends(m.key());
            if (ends == null) {
                continue;
            }
            Project pr = fuseFor(c, level, id, tpl, BlockPos.of(ends[0]), BlockPos.of(ends[1]));
            if (pr != null) {
                return pr;
            }
        }
        return null;
    }

    private static long @Nullable [] ends(String key) {
        int colon = key.indexOf(':');
        int dash = key.indexOf('-', colon + 2);
        if (colon < 0 || dash < 0) {
            return null;
        }
        try {
            return new long[] {Long.parseLong(key.substring(colon + 1, dash)), Long.parseLong(key.substring(dash + 1))};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static @Nullable Project fuseFor(Colony c, ServerLevel level, String id, ColonyTemplate tpl, BlockPos a, BlockPos b) {
        if (!Colonies.inside(c, a) || !Colonies.inside(c, b) || !(level.getBlockState(a).getBlock() instanceof GridConnector ca)
            || !(level.getBlockState(b).getBlock() instanceof GridConnector cb)) {
            return null;
        }
        CableTier tier = spanTier(level, a, b);
        if (tier == null || tier == CableTier.ROPE) {
            return null;
        }
        for (GridNetwork net : Grid.of(level).networks(a.asLong())) {
            for (int i = 0; i < net.nodes.size(); i++) {
                if (level.getBlockState(BlockPos.of(net.nodes.getLong(i))).is(AutarkiaBlocks.FUSE_BOX.get())) {
                    return null;
                }
            }
        }
        if (generatorDistance(level, b) < generatorDistance(level, a)) {
            BlockPos t = a;
            a = b;
            b = t;
            GridConnector tc = ca;
            ca = cb;
            cb = tc;
        }
        Vec3 pa = ca.attachPoint(a, level.getBlockState(a));
        Vec3 pb = cb.attachPoint(b, level.getBlockState(b));
        BlockPos spot = fuseSpot(c, level, a, b, pa, pb, tier);
        if (spot == null) {
            return null;
        }
        Direction face = Direction.getApproximateNearest(b.getX() - a.getX(), 0, b.getZ() - a.getZ());
        BlockState fuse = AutarkiaBlocks.FUSE_BOX.get().defaultBlockState();
        if (fuse.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            fuse = fuse.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                face.getAxis().isHorizontal() ? face.getClockWise() : Direction.NORTH);
        }
        List<Project.Cell> cells = new ArrayList<>(List.of(new Project.Cell(spot.asLong(), fuse)));
        List<Project.SpanRec> spans = List.of(new Project.SpanRec(a.asLong(), spot.asLong(), tier), new Project.SpanRec(spot.asLong(), b.asLong(), tier));
        Project pr = ProjectFactory.handlerProject(c, level, id, tpl, "fuse_box", spot, cells, new ArrayList<>(spans), new long[0]);
        pr.cutSpans.add(new Project.SpanRec(a.asLong(), b.asLong(), tier));
        return pr;
    }

    private static @Nullable CableTier spanTier(ServerLevel level, BlockPos a, BlockPos b) {
        for (var s : Grid.of(level).data().spans.values()) {
            if (s.joins(a.asLong(), b.asLong())) {
                return s.tier;
            }
        }
        return null;
    }

    private static double generatorDistance(ServerLevel level, BlockPos end) {
        double best = Double.MAX_VALUE;
        for (GridNetwork net : Grid.of(level).networks(end.asLong())) {
            for (GridNetwork.Attached m : net.members) {
                if (m.member.gridRole() == GridMember.Role.GENERATOR) {
                    best = Math.min(best, BlockPos.of(m.node).distSqr(end));
                }
            }
        }
        return best;
    }

    private static @Nullable BlockPos fuseSpot(Colony c, ServerLevel level, BlockPos a, BlockPos b, Vec3 pa, Vec3 pb, CableTier tier) {
        double dx = b.getX() - a.getX();
        double dz = b.getZ() - a.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 4) {
            return null;
        }
        for (int d = 2; d <= Math.min(6, len - 2); d++) {
            int x = (int) Math.floor(a.getX() + 0.5 + dx / len * d);
            int z = (int) Math.floor(a.getZ() + 0.5 + dz / len * d);
            BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!Colonies.inside(c, spot) || !level.getBlockState(spot).canBeReplaced() || !Builds.supported(level, spot)) {
                continue;
            }
            Vec3 top = Vec3.atCenterOf(spot).add(0, 0.5, 0);
            if (top.distanceTo(pa) <= tier.maxSpan && top.distanceTo(pb) <= tier.maxSpan && !Grid.chordBlocked(level, top, pa, spot, a)
                && !Grid.chordBlocked(level, top, pb, spot, b)) {
                return spot;
            }
        }
        return null;
    }

    /**
     * Handler {@code rack_upgrade}: the row through a {@code rack_collapse} memory position, or else through a wood rack
     * above 85%: every wood rack of the row is cut and rebuilt as a {@code steel_rack} with the same facing, the collapsed
     * positions (memory positions; a crate block that landed there is cut first) get a steel rack facing away from the
     * wall, and a {@code prop_beam} goes first under every rack of the row that stands on nothing solid. Crates of a cut
     * rack or crate block drop and go back into racks through the workers.
     */
    public static @Nullable Project rackUpgrade(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        double half = AutarkiaConfig.MEMORY_HALF_LIFE_TICKS.get();
        java.util.Set<BlockPos> fallen = new java.util.LinkedHashSet<>();
        c.memories.stream().filter(m -> m.type().equals("rack_collapse") && m.weightAt(c.clock, half) >= 0.5).forEach(m -> fallen.add(m.pos()));
        List<BlockPos> seeds = new ArrayList<>(fallen);
        ColonyRuntime rt = ColonyRuntime.of(c);
        rt.storages.stream().filter(p -> level.getBlockEntity(p) instanceof RackBlockEntity r && level.getBlockState(p).is(AutarkiaBlocks.WOOD_RACK.get())
            && r.ratio() >= 0.85).forEach(seeds::add);
        for (BlockPos seed : seeds) {
            Project pr = rackRow(c, level, id, tpl, seed, fallen);
            if (pr != null) {
                return pr;
            }
        }
        return null;
    }

    private static boolean rebuildable(ServerLevel level, BlockPos p, java.util.Set<BlockPos> fallen) {
        BlockState st = level.getBlockState(p);
        return st.is(AutarkiaBlocks.WOOD_RACK.get()) || fallen.contains(p) && (st.canBeReplaced() || st.is(AutarkiaBlocks.CRATE.get()));
    }

    private static @Nullable Project rackRow(Colony c, ServerLevel level, String id, ColonyTemplate tpl, BlockPos seed, java.util.Set<BlockPos> fallen) {
        List<BlockPos> row = new ArrayList<>();
        Direction facing = null;
        for (Direction along : List.of(Direction.EAST, Direction.SOUTH)) {
            List<BlockPos> line = new ArrayList<>();
            Direction f = null;
            for (int k = -8; k <= 8; k++) {
                BlockPos p = seed.relative(along, k);
                BlockState st = level.getBlockState(p);
                if (st.getBlock() instanceof RackBlock) {
                    line.add(p.immutable());
                    f = dev.autarkia.registry.MachineBlock.front(st);
                } else if (rebuildable(level, p, fallen)) {
                    line.add(p.immutable());
                }
            }
            if (line.stream().filter(p -> rebuildable(level, p, fallen)).count() > row.stream().filter(p -> rebuildable(level, p, fallen)).count()) {
                row = line;
                facing = f != null ? f : awayFromWall(level, line.get(0));
            }
        }
        if (row.isEmpty() || facing == null) {
            return null;
        }
        BlockState steel = AutarkiaBlocks.STEEL_RACK.get().defaultBlockState();
        if (steel.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            steel = steel.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        List<Project.Cell> cells = new ArrayList<>();
        List<Long> cuts = new ArrayList<>();
        for (BlockPos p : row) {
            BlockState st = level.getBlockState(p);
            boolean wood = st.is(AutarkiaBlocks.WOOD_RACK.get()) || st.is(AutarkiaBlocks.CRATE.get());
            if (!rebuildable(level, p, fallen) || !Colonies.inside(c, p)) {
                continue;
            }
            BlockPos below = p.below();
            if (level.getBlockState(below).canBeReplaced()) {
                cells.add(new Project.Cell(below.asLong(), AutarkiaBlocks.PROP_BEAM.get().defaultBlockState()));
            }
            if (wood) {
                cuts.add(p.asLong());
            }
            cells.add(new Project.Cell(p.asLong(), steel));
        }
        if (cells.isEmpty()) {
            return null;
        }
        return ProjectFactory.handlerProject(c, level, id, tpl, "rack_upgrade", row.get(0), cells, new ArrayList<>(),
            cuts.stream().mapToLong(Long::longValue).toArray());
    }

    private static Direction awayFromWall(ServerLevel level, BlockPos p) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos back = p.relative(d);
            if (level.getBlockState(back).isFaceSturdy(level, back, d.getOpposite()) && level.getBlockState(back.above()).isFaceSturdy(level, back.above(),
                d.getOpposite())) {
                return d.getOpposite();
            }
        }
        return Direction.NORTH;
    }
}

package dev.autarkia.colony;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The geometry of an Autarky Seed (05 section 8.4): the layout of 1 to 8 Patterns, its checks and its placement in the
 * world. Layout space has y 0 at the floor; each entry puts the min corner of its rotated Pattern box at (dx, 0, dz).
 * The checks: every Pattern exists, the first one has role OUTPOST and holds an {@code outpost_core}, no two footprints
 * overlap, the total is at most 96 x 48 x 96, and (C4) every footprint cell lies in the outpost square of the colony
 * tier centred on that outpost core. Placed cells skip ignored, air and big-block part cells and every
 * {@code outpost_core} cell (the seed plants the first one itself, C3). World placement rotates the whole layout by
 * the thrower's facing and centres the footprint on the landing column.
 */
public final class SeedLayout {
    public static final int MAX_XZ = 96;
    public static final int MAX_Y = 48;
    private static final Rotation[] ROTATIONS = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};

    /** Layout check results in check order. */
    public enum Problem { NONE, NO_PATTERN, NOT_OUTPOST, OVERLAP, TOO_BIG, NO_FIT }

    /** One entry with its loaded Pattern and its rotated box size. */
    public record Placed(Pattern pattern, SeedContents.Entry entry, Vec3i rotated) {
        /** Return the map from Pattern-relative positions to layout space. */
        public Function<BlockPos, BlockPos> map() {
            return ColonyPlans.mapper(pattern.size(), new BlockPos(entry.dx(), 0, entry.dz()), rotation(entry.rot()));
        }
    }

    /**
     * A checked layout: the first problem (NONE when valid), the entries normalized to the footprint min corner, the
     * footprint size, the outpost core cell in layout space (null without one), the placed block count and the core
     * margin (largest horizontal Chebyshev distance from the core to a footprint cell).
     */
    public record Result(Problem problem, List<Placed> placed, Vec3i size, @Nullable BlockPos core, int blocks, int margin) {
        /** Return the normalized entries. */
        public List<SeedContents.Entry> entries() {
            return placed.stream().map(Placed::entry).toList();
        }
    }

    /** A world placement: rotation, footprint min corner (y = floor + 1) and rotated footprint size. */
    public record World(Rotation rotation, BlockPos min, Vec3i size) {
        /** Return the map from layout space to the world for a layout of the given size. */
        public Function<BlockPos, BlockPos> map(Vec3i layoutSize) {
            return ColonyPlans.mapper(layoutSize, min, rotation);
        }

        /** Return the footprint max corner. */
        public BlockPos max() {
            return min.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        }
    }

    private SeedLayout() {
    }

    /** Return the rotation of a quarter-turn count 0 to 3 (clockwise). */
    public static Rotation rotation(int quarterTurns) {
        return ROTATIONS[Math.floorMod(quarterTurns, 4)];
    }

    /** Return the quarter turns of a rotation. */
    public static int quarterTurns(Rotation r) {
        for (int i = 0; i < 4; i++) {
            if (ROTATIONS[i] == r) {
                return i;
            }
        }
        return 0;
    }

    /** Return the world rotation for a thrower facing: north 0, east 1, south 2, west 3 quarter turns. */
    public static Rotation facing(Direction d) {
        return switch (d) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** Check a layout for a colony whose outpost square has the given half-size. */
    public static Result check(MinecraftServer server, List<SeedContents.Entry> entries, int half) {
        List<Pattern> patterns = new ArrayList<>();
        for (SeedContents.Entry e : entries) {
            Pattern p = Pattern.load(server, e.pattern());
            if (p == null) {
                return new Result(Problem.NO_PATTERN, List.of(), Vec3i.ZERO, null, 0, 0);
            }
            patterns.add(p);
        }
        if (patterns.isEmpty()) {
            return new Result(Problem.NO_PATTERN, List.of(), Vec3i.ZERO, null, 0, 0);
        }
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        List<Vec3i> rotated = new ArrayList<>();
        for (int i = 0; i < patterns.size(); i++) {
            Vec3i rs = ColonyPlans.rotatedSize(patterns.get(i).size(), rotation(entries.get(i).rot()));
            rotated.add(rs);
            minX = Math.min(minX, entries.get(i).dx());
            minZ = Math.min(minZ, entries.get(i).dz());
        }
        List<Placed> placed = new ArrayList<>();
        int w = 0;
        int h = 0;
        int d = 0;
        for (int i = 0; i < patterns.size(); i++) {
            SeedContents.Entry e = entries.get(i);
            SeedContents.Entry n = new SeedContents.Entry(e.pattern(), e.dx() - minX, e.dz() - minZ, Math.floorMod(e.rot(), 4));
            placed.add(new Placed(patterns.get(i), n, rotated.get(i)));
            w = Math.max(w, n.dx() + rotated.get(i).getX());
            h = Math.max(h, rotated.get(i).getY());
            d = Math.max(d, n.dz() + rotated.get(i).getZ());
        }
        Vec3i size = new Vec3i(w, h, d);
        BlockPos core = core(placed.get(0));
        int blocks = placed.stream().mapToInt(SeedLayout::blocks).sum();
        int margin = core == null ? 0 : Math.max(Math.max(core.getX(), w - 1 - core.getX()), Math.max(core.getZ(), d - 1 - core.getZ()));
        Problem problem = Problem.NONE;
        if (!placed.get(0).pattern().role().equalsIgnoreCase("outpost") || core == null) {
            problem = Problem.NOT_OUTPOST;
        } else if (overlap(placed)) {
            problem = Problem.OVERLAP;
        } else if (w > MAX_XZ || d > MAX_XZ || h > MAX_Y) {
            problem = Problem.TOO_BIG;
        } else if (margin > half) {
            problem = Problem.NO_FIT;
        }
        return new Result(problem, placed, size, core, blocks, margin);
    }

    private static boolean overlap(List<Placed> placed) {
        for (int i = 0; i < placed.size(); i++) {
            for (int j = i + 1; j < placed.size(); j++) {
                SeedContents.Entry a = placed.get(i).entry();
                SeedContents.Entry b = placed.get(j).entry();
                Vec3i ra = placed.get(i).rotated();
                Vec3i rb = placed.get(j).rotated();
                if (a.dx() < b.dx() + rb.getX() && b.dx() < a.dx() + ra.getX() && a.dz() < b.dz() + rb.getZ() && b.dz() < a.dz() + ra.getZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static @Nullable BlockPos core(Placed p) {
        Pattern pt = p.pattern();
        Function<BlockPos, BlockPos> map = p.map();
        for (int i = 0; i < pt.volume(); i++) {
            BlockState st = pt.state(i);
            if (st != null && st.is(dev.autarkia.registry.AutarkiaBlocks.OUTPOST_CORE.get())) {
                return map.apply(rel(pt, i));
            }
        }
        return null;
    }

    /** Return the Pattern-relative position of cell index i (x, then z, then y). */
    public static BlockPos rel(Pattern p, int i) {
        int sx = p.size().getX();
        int sz = p.size().getZ();
        return new BlockPos(i % sx, i / (sx * sz), i / sx % sz);
    }

    private static boolean placedCell(Pattern p, int i, Set<Integer> parts) {
        BlockState st = p.state(i);
        return st != null && !st.isAir() && !parts.contains(i) && !st.is(dev.autarkia.registry.AutarkiaBlocks.OUTPOST_CORE.get());
    }

    private static int blocks(Placed p) {
        Set<Integer> parts = new HashSet<>(p.pattern().parts());
        int n = 0;
        for (int i = 0; i < p.pattern().volume(); i++) {
            n += placedCell(p.pattern(), i, parts) ? 1 : 0;
        }
        return n;
    }

    /** Return the outpost core state of the first entry, rotated by its own and the world rotation. */
    public static BlockState coreState(Result r, Rotation world) {
        Placed p = r.placed().get(0);
        for (int i = 0; i < p.pattern().volume(); i++) {
            BlockState st = p.pattern().state(i);
            if (st != null && st.is(dev.autarkia.registry.AutarkiaBlocks.OUTPOST_CORE.get())) {
                return st.rotate(rotation(p.entry().rot())).rotate(world);
            }
        }
        throw new IllegalStateException("Seed layout without an outpost_core in its first Pattern " + p.pattern().id());
    }

    /** Return the world position of the outpost core cell of the first entry. */
    public static BlockPos coreWorld(Result r, World w) {
        Pattern pt = r.placed().get(0).pattern();
        Function<BlockPos, BlockPos> map = ColonyPlans.mapper(pt.size(), entryMin(r, w, 0), entryRotation(r, w, 0));
        for (int i = 0; i < pt.volume(); i++) {
            BlockState st = pt.state(i);
            if (st != null && st.is(dev.autarkia.registry.AutarkiaBlocks.OUTPOST_CORE.get())) {
                return map.apply(rel(pt, i));
            }
        }
        throw new IllegalStateException("Seed layout without an outpost_core in its first Pattern " + pt.id());
    }

    /** Return the world placement for a landing column, a floor height and a world rotation. */
    public static World place(Result r, int x, int floor, int z, Rotation world) {
        Vec3i rs = ColonyPlans.rotatedSize(r.size(), world);
        return new World(world, new BlockPos(x - rs.getX() / 2, floor + 1, z - rs.getZ() / 2), rs);
    }

    /** Return the world box min corner of entry i (the origin of its colony Pattern instance). */
    public static BlockPos entryMin(Result r, World w, int i) {
        Placed p = r.placed().get(i);
        Function<BlockPos, BlockPos> m = w.map(r.size());
        BlockPos a = m.apply(new BlockPos(p.entry().dx(), 0, p.entry().dz()));
        BlockPos b = m.apply(new BlockPos(p.entry().dx() + p.rotated().getX() - 1, p.rotated().getY() - 1, p.entry().dz() + p.rotated().getZ() - 1));
        return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    /** Return the combined rotation of entry i in the world. */
    public static Rotation entryRotation(Result r, World w, int i) {
        return rotation(r.placed().get(i).entry().rot() + quarterTurns(w.rotation()));
    }

    /**
     * Return the placed world cells in unpack order (05 section 8.4): y ascending, then horizontal distance to the
     * footprint centre ascending, then {@code BlockPos.asLong}.
     */
    public static List<Project.Cell> cells(Result r, World w) {
        List<Project.Cell> out = new ArrayList<>();
        for (int k = 0; k < r.placed().size(); k++) {
            Pattern pt = r.placed().get(k).pattern();
            Function<BlockPos, BlockPos> map = ColonyPlans.mapper(pt.size(), entryMin(r, w, k), entryRotation(r, w, k));
            Rotation rot = entryRotation(r, w, k);
            Set<Integer> parts = new HashSet<>(pt.parts());
            for (int i = 0; i < pt.volume(); i++) {
                if (placedCell(pt, i, parts)) {
                    out.add(new Project.Cell(map.apply(rel(pt, i)).asLong(), pt.state(i).rotate(rot)));
                }
            }
        }
        double cx = w.min().getX() + w.size().getX() / 2.0;
        double cz = w.min().getZ() + w.size().getZ() / 2.0;
        out.sort(Comparator.comparingInt((Project.Cell c) -> BlockPos.of(c.pos()).getY())
            .thenComparingDouble(c -> {
                BlockPos p = BlockPos.of(c.pos());
                double dx = p.getX() + 0.5 - cx;
                double dz = p.getZ() + 0.5 - cz;
                return dx * dx + dz * dz;
            }).thenComparingLong(Project.Cell::pos));
        return out;
    }

    /** Return the internal spans of every entry in world positions. */
    public static List<Project.SpanRec> spans(Result r, World w) {
        List<Project.SpanRec> out = new ArrayList<>();
        for (int k = 0; k < r.placed().size(); k++) {
            Pattern pt = r.placed().get(k).pattern();
            Function<BlockPos, BlockPos> map = ColonyPlans.mapper(pt.size(), entryMin(r, w, k), entryRotation(r, w, k));
            for (Pattern.SpanRecord s : pt.spans()) {
                out.add(new Project.SpanRec(map.apply(s.a()).asLong(), map.apply(s.b()).asLong(), s.tier()));
            }
        }
        return out;
    }
}

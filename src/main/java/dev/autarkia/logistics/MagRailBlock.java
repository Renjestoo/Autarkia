package dev.autarkia.logistics;

import dev.autarkia.registry.MachineBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * {@code mag_rail} (04 section 1.7, Q7): a plain {@code Block} with the vanilla property {@code shape}
 * ({@link RailShape}, all 10 values), not a {@code BaseRailBlock} and not in {@code #minecraft:rails}, so vanilla rail
 * code ignores it. Placement takes NORTH_SOUTH or EAST_WEST from the player facing, then connects to neighbour track
 * (mag rails and the axis ends of {@code mag_rail_station}s, same level, one up or one down) with the rules of the
 * vanilla rail state, without junctions, straight-only or power variants. A rail with an open end connects again when a
 * neighbour changes. It needs a sturdy block below, and a slope also on its high side; otherwise it drops. Rotate and
 * mirror map both ends of the shape. Every topology change bumps {@link MagRailLine#changed()}.
 */
public class MagRailBlock extends Block {
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE;
    private static final VoxelShape FLAT = Block.box(0, 0, 0, 16, 2, 16);
    private static final VoxelShape SLOPE = Block.box(0, 0, 0, 16, 8, 16);

    /** One end of a track cell: a horizontal direction and whether the connection lies one block up. */
    public record End(Direction dir, boolean up) {
    }

    public MagRailBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(SHAPE).isSlope() ? SLOPE : FLAT;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(SHAPE, ctx.getHorizontalDirection().getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }
        RailShape s = state.getValue(SHAPE);
        if (!s.isSlope()) {
            return true;
        }
        Direction high = ends(s).stream().filter(End::up).findFirst().orElseThrow().dir();
        BlockPos side = pos.relative(high);
        return level.getBlockState(side).isFaceSturdy(level, side, Direction.UP);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!(level instanceof ServerLevel)) {
            return;
        }
        if (!oldState.is(this)) {
            new Track(level, pos, state).place(true, state.getValue(SHAPE));
            MagRailLine.changed();
        } else if (oldState.getValue(SHAPE) != state.getValue(SHAPE)) {
            MagRailLine.changed();
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        MagRailLine.changed();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        Track t = new Track(level, pos, state);
        t.removeSoftConnections();
        if (t.connections.size() < 2) {
            new Track(level, pos, state).place(false, state.getValue(SHAPE));
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        RailShape s = state.getValue(SHAPE);
        List<End> e = ends(s);
        return state.setValue(SHAPE, shapeOf(new End(rotation.rotate(e.get(0).dir()), e.get(0).up()), new End(rotation.rotate(e.get(1).dir()), e.get(1).up())));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        RailShape s = state.getValue(SHAPE);
        List<End> e = ends(s);
        return state.setValue(SHAPE, shapeOf(new End(mirror.mirror(e.get(0).dir()), e.get(0).up()), new End(mirror.mirror(e.get(1).dir()), e.get(1).up())));
    }

    /** Return the two ends of a rail shape (vanilla rail connection layout). */
    public static List<End> ends(RailShape s) {
        return switch (s) {
            case NORTH_SOUTH -> List.of(new End(Direction.NORTH, false), new End(Direction.SOUTH, false));
            case EAST_WEST -> List.of(new End(Direction.WEST, false), new End(Direction.EAST, false));
            case ASCENDING_EAST -> List.of(new End(Direction.WEST, false), new End(Direction.EAST, true));
            case ASCENDING_WEST -> List.of(new End(Direction.WEST, true), new End(Direction.EAST, false));
            case ASCENDING_NORTH -> List.of(new End(Direction.NORTH, true), new End(Direction.SOUTH, false));
            case ASCENDING_SOUTH -> List.of(new End(Direction.NORTH, false), new End(Direction.SOUTH, true));
            case SOUTH_EAST -> List.of(new End(Direction.EAST, false), new End(Direction.SOUTH, false));
            case SOUTH_WEST -> List.of(new End(Direction.WEST, false), new End(Direction.SOUTH, false));
            case NORTH_WEST -> List.of(new End(Direction.WEST, false), new End(Direction.NORTH, false));
            case NORTH_EAST -> List.of(new End(Direction.EAST, false), new End(Direction.NORTH, false));
        };
    }

    /** Return the rail shape with exactly these two ends. */
    public static RailShape shapeOf(End a, End b) {
        for (RailShape s : RailShape.values()) {
            List<End> e = ends(s);
            if (e.contains(a) && e.contains(b)) {
                return s;
            }
        }
        throw new IllegalArgumentException("No rail shape joins " + a + " and " + b);
    }

    /** Return true for a track cell: a mag rail or a mag rail station. */
    public static boolean isTrack(BlockState s) {
        return s.getBlock() instanceof MagRailBlock || s.getBlock() instanceof MagRailStationBlock;
    }

    /** Return the straight shape of a station: along its facing axis. */
    public static RailShape stationShape(BlockState s) {
        return MachineBlock.front(s).getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
    }

    /** Return the shape of a track cell (a station is straight along its axis). */
    public static RailShape shape(BlockState s) {
        return s.getBlock() instanceof MagRailStationBlock ? stationShape(s) : s.getValue(SHAPE);
    }

    /** Return the connected track cells of a loaded track cell (0 to 2, mutual connections only); null entries are unloaded. */
    public static List<Link> links(Level level, BlockPos pos) {
        List<Link> out = new ArrayList<>(2);
        BlockState st = level.getBlockState(pos);
        if (!isTrack(st)) {
            return out;
        }
        for (End e : ends(shape(st))) {
            BlockPos exit = pos.relative(e.dir()).above(e.up() ? 1 : 0);
            boolean unloaded = false;
            BlockPos found = null;
            for (BlockPos q : List.of(exit, exit.above(), exit.below())) {
                if (!level.isLoaded(q)) {
                    unloaded = true;
                    continue;
                }
                BlockState qs = level.getBlockState(q);
                if (isTrack(qs) && connectsBack(q, qs, pos)) {
                    found = q;
                    break;
                }
            }
            if (found != null) {
                out.add(new Link(found, false));
            } else if (unloaded) {
                out.add(new Link(exit, true));
            }
        }
        return out;
    }

    /** A neighbour of a track cell: its position, or the exit position when its chunk is not loaded. */
    public record Link(BlockPos pos, boolean unloaded) {
    }

    /** Return true when the track cell at q has an end that leads to target (same x and z). */
    public static boolean connectsBack(BlockPos q, BlockState qs, BlockPos target) {
        for (End f : ends(shape(qs))) {
            BlockPos b = q.relative(f.dir());
            if (b.getX() == target.getX() && b.getZ() == target.getZ()) {
                return true;
            }
        }
        return false;
    }

    /** Return true when the track cells a and b are loaded, track, and connected to each other. */
    public static boolean connected(Level level, BlockPos a, BlockPos b) {
        if (!level.isLoaded(a) || !level.isLoaded(b)) {
            return false;
        }
        BlockState sa = level.getBlockState(a);
        BlockState sb = level.getBlockState(b);
        return isTrack(sa) && isTrack(sb) && Math.abs(a.getY() - b.getY()) <= 1 && connectsBack(a, sa, b) && connectsBack(b, sb, a);
    }

    /** Let the rails at both axis ends of a newly placed station turn toward it when they have a free end. */
    static void attachStation(Level level, BlockPos station, BlockState state) {
        Track s = new Track(level, station, state);
        for (BlockPos c : List.copyOf(s.connections)) {
            Track r = s.getRail(c);
            if (r != null && !r.station) {
                r.removeSoftConnections();
                if (r.canConnectTo(s)) {
                    r.connectTo(s);
                }
            }
        }
    }

    /** The vanilla rail state rules (RailState) for mag rails and stations; a station keeps its straight shape. */
    static final class Track {
        private final Level level;
        private final BlockPos pos;
        private BlockState state;
        private final boolean station;
        private final List<BlockPos> connections = new ArrayList<>();

        Track(Level level, BlockPos pos, BlockState state) {
            this.level = level;
            this.pos = pos;
            this.state = state;
            this.station = state.getBlock() instanceof MagRailStationBlock;
            updateConnections(shape(state));
        }

        private void updateConnections(RailShape shape) {
            connections.clear();
            for (End e : ends(shape)) {
                connections.add(pos.relative(e.dir()).above(e.up() ? 1 : 0));
            }
        }

        private boolean trackAt(BlockPos p) {
            return level.isLoaded(p) && isTrack(level.getBlockState(p));
        }

        private @Nullable Track getRail(BlockPos p) {
            for (BlockPos q : List.of(p, p.above(), p.below())) {
                if (trackAt(q)) {
                    return new Track(level, q, level.getBlockState(q));
                }
            }
            return null;
        }

        private void removeSoftConnections() {
            if (station) {
                return;
            }
            for (int i = 0; i < connections.size(); i++) {
                Track t = getRail(connections.get(i));
                if (t != null && t.connectsTo(this)) {
                    connections.set(i, t.pos);
                } else {
                    connections.remove(i--);
                }
            }
        }

        private boolean connectsTo(Track other) {
            return hasConnection(other.pos);
        }

        private boolean hasConnection(BlockPos p) {
            for (BlockPos c : connections) {
                if (c.getX() == p.getX() && c.getZ() == p.getZ()) {
                    return true;
                }
            }
            return false;
        }

        private boolean canConnectTo(Track other) {
            return station ? hasConnection(other.pos) : connectsTo(other) || connections.size() != 2;
        }

        private void connectTo(Track other) {
            if (station) {
                return;
            }
            connections.add(other.pos);
            BlockPos n = pos.north();
            BlockPos s = pos.south();
            BlockPos w = pos.west();
            BlockPos e = pos.east();
            boolean bn = hasConnection(n);
            boolean bs = hasConnection(s);
            boolean bw = hasConnection(w);
            boolean be = hasConnection(e);
            RailShape shape = null;
            if (bn || bs) {
                shape = RailShape.NORTH_SOUTH;
            }
            if (bw || be) {
                shape = RailShape.EAST_WEST;
            }
            if (bs && be && !bn && !bw) {
                shape = RailShape.SOUTH_EAST;
            }
            if (bs && bw && !bn && !be) {
                shape = RailShape.SOUTH_WEST;
            }
            if (bn && bw && !bs && !be) {
                shape = RailShape.NORTH_WEST;
            }
            if (bn && be && !bs && !bw) {
                shape = RailShape.NORTH_EAST;
            }
            shape = slope(shape == null ? RailShape.NORTH_SOUTH : shape, n, s, w, e);
            state = state.setValue(SHAPE, shape);
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }

        private RailShape slope(RailShape shape, BlockPos n, BlockPos s, BlockPos w, BlockPos e) {
            if (shape == RailShape.NORTH_SOUTH) {
                if (trackAt(n.above())) {
                    shape = RailShape.ASCENDING_NORTH;
                }
                if (trackAt(s.above())) {
                    shape = RailShape.ASCENDING_SOUTH;
                }
            }
            if (shape == RailShape.EAST_WEST) {
                if (trackAt(e.above())) {
                    shape = RailShape.ASCENDING_EAST;
                }
                if (trackAt(w.above())) {
                    shape = RailShape.ASCENDING_WEST;
                }
            }
            return shape;
        }

        private boolean hasNeighborRail(BlockPos p) {
            Track t = getRail(p);
            if (t == null) {
                return false;
            }
            t.removeSoftConnections();
            return t.canConnectTo(this);
        }

        /** Choose the shape from the neighbours (vanilla place, no power, curves allowed) and connect the neighbours. */
        void place(boolean first, RailShape fallback) {
            if (station) {
                return;
            }
            BlockPos n = pos.north();
            BlockPos s = pos.south();
            BlockPos w = pos.west();
            BlockPos e = pos.east();
            boolean bn = hasNeighborRail(n);
            boolean bs = hasNeighborRail(s);
            boolean bw = hasNeighborRail(w);
            boolean be = hasNeighborRail(e);
            boolean ns = bn || bs;
            boolean ew = bw || be;
            RailShape shape = null;
            if (ns && !ew) {
                shape = RailShape.NORTH_SOUTH;
            }
            if (ew && !ns) {
                shape = RailShape.EAST_WEST;
            }
            boolean se = bs && be;
            boolean sw = bs && bw;
            boolean ne = bn && be;
            boolean nw = bn && bw;
            if (se && !bn && !bw) {
                shape = RailShape.SOUTH_EAST;
            }
            if (sw && !bn && !be) {
                shape = RailShape.SOUTH_WEST;
            }
            if (nw && !bs && !be) {
                shape = RailShape.NORTH_WEST;
            }
            if (ne && !bs && !bw) {
                shape = RailShape.NORTH_EAST;
            }
            if (shape == null) {
                if (ns && ew) {
                    shape = fallback;
                } else if (ns) {
                    shape = RailShape.NORTH_SOUTH;
                } else if (ew) {
                    shape = RailShape.EAST_WEST;
                }
                if (nw) {
                    shape = RailShape.NORTH_WEST;
                }
                if (ne) {
                    shape = RailShape.NORTH_EAST;
                }
                if (sw) {
                    shape = RailShape.SOUTH_WEST;
                }
                if (se) {
                    shape = RailShape.SOUTH_EAST;
                }
            }
            shape = slope(shape == null ? fallback : shape, n, s, w, e);
            updateConnections(shape);
            state = state.setValue(SHAPE, shape);
            if (first || level.getBlockState(pos) != state) {
                level.setBlock(pos, state, Block.UPDATE_ALL);
                for (BlockPos c : List.copyOf(connections)) {
                    Track t = getRail(c);
                    if (t != null) {
                        t.removeSoftConnections();
                        if (t.canConnectTo(this)) {
                            t.connectTo(this);
                        }
                    }
                }
            }
        }
    }
}

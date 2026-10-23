package dev.autarkia.energy;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.MachineBlock;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rope drive connector node (02 section 2.1): rope tier, 4 spans, attach point at the wheel centre. The facing points
 * away from the mount face; a bracket on the mount face carries a grooved wheel across the facing (rim radius 5 px), so on
 * a floor it lies flat as a sheave. The block model is written by tools/content_m1.py.
 */
public class RopePulleyBlock extends MachineBlock implements GridConnector {
    private static final double RIM = 5.0 / 16;
    private static final double[][] UP_BOXES = {{5, 0, 5, 11, 1, 11}, {6, 1, 6, 10, 3, 10}, {7, 3, 7, 9, 11.5, 9}, {3, 7, 3, 13, 9, 13}};
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.values()) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : UP_BOXES) {
                double[] p = turn(d, b[0], b[1], b[2]);
                double[] q = turn(d, b[3], b[4], b[5]);
                shape = Shapes.or(shape, Block.box(Math.min(p[0], q[0]), Math.min(p[1], q[1]), Math.min(p[2], q[2]),
                    Math.max(p[0], q[0]), Math.max(p[1], q[1]), Math.max(p[2], q[2])));
            }
            SHAPES.put(d, shape);
        }
    }

    public RopePulleyBlock(Properties props) {
        super(props, AutarkiaBlockEntities.ROPE_PULLEY, false);
    }

    private static double[] turn(Direction d, double x, double y, double z) {
        return switch (d) {
            case UP -> new double[] {x, y, z};
            case DOWN -> new double[] {x, 16 - y, 16 - z};
            case NORTH -> new double[] {x, z, 16 - y};
            case SOUTH -> new double[] {16 - x, z, y};
            case EAST -> new double[] {y, z, x};
            case WEST -> new double[] {16 - y, z, 16 - x};
        };
    }

    @Override
    protected Facing facingMode() {
        return Facing.ALL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public boolean acceptsTier(CableTier tier) {
        return tier == CableTier.ROPE;
    }

    @Override
    public int maxSpans() {
        return 4;
    }

    @Override
    public Vec3 attachPoint(BlockPos pos, BlockState state) {
        return Vec3.atCenterOf(pos);
    }

    /**
     * Return the point of the wheel rim (5 px from the hub, in the groove) on the side toward the other end, in the wheel
     * plane; a span along the axle leaves from the top of the rim (from the east side when the axle is vertical).
     */
    @Override
    public Vec3 cableEnd(BlockPos pos, BlockState state, Vec3 toward) {
        Vec3 centre = Vec3.atCenterOf(pos);
        Vec3 axis = Vec3.atLowerCornerOf(state.getValue(FACING).getUnitVec3i());
        Vec3 inPlane = inPlane(toward.subtract(centre), axis);
        if (inPlane.lengthSqr() < 1e-6) {
            inPlane = inPlane(new Vec3(0, 1, 0), axis);
        }
        if (inPlane.lengthSqr() < 1e-6) {
            inPlane = new Vec3(1, 0, 0);
        }
        return centre.add(inPlane.normalize().scale(RIM));
    }

    private static Vec3 inPlane(Vec3 d, Vec3 axis) {
        return d.subtract(axis.scale(d.dot(axis)));
    }

    /** Return the facing after a wrench turn (next of the 6 directions). */
    public static Direction nextFacing(Direction d) {
        return Direction.from3DDataValue((d.get3DDataValue() + 1) % 6);
    }
}

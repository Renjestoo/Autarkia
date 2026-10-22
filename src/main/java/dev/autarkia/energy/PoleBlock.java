package dev.autarkia.energy;

import dev.autarkia.registry.BigMachineBlock;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A pole connector node (02 section 2.1): a 1 x height x 1 big block of one tier with its attach point above the center
 * of the top block (wooden pole: copper, 3 high, 4 spans, +0.35; steel pylon: insulated, 5 high, 6 spans, +0.4; HV pylon: HV, 7 high, 4 spans, +0.45;
 * cryo node: superconductor, 3 high, 4 spans, +0.3). The attach point rules span length and chord checks; the drawn cable
 * ends on the crossarm insulator nearest the other end, as placed by the model generator (tools/geo_blocks_t2..t4.py).
 */
public class PoleBlock extends BigMachineBlock implements GridConnector {
    private final int height;
    private final CableTier tier;
    private final int maxSpans;
    private final double attachUp;
    private final double reach;
    private final double[] fittings;

    /**
     * Create a pole.
     *
     * @param reach    distance in blocks of the insulators from the pole axis along the crossarm, which runs across the facing
     * @param fittings heights in blocks above the pole base where a cable meets an insulator, one per crossarm; none ends the
     *                 cable at the attach point
     */
    public PoleBlock(Properties props, Supplier<? extends BlockEntityType<?>> type, int height, CableTier tier, int maxSpans, double attachUp,
                     double reach, double... fittings) {
        super(props, type, box(0, 0, 0, 0, height - 1, 0), true);
        this.height = height;
        this.tier = tier;
        this.maxSpans = maxSpans;
        this.attachUp = attachUp;
        this.reach = reach;
        this.fittings = fittings;
    }

    /** Return the pole height in blocks. */
    public int height() {
        return height;
    }

    /** Return the tier this pole carries. */
    public CableTier tier() {
        return tier;
    }

    @Override
    public boolean acceptsTier(CableTier t) {
        return t == tier;
    }

    @Override
    public int maxSpans() {
        return maxSpans;
    }

    @Override
    public Vec3 attachPoint(BlockPos pos, BlockState state) {
        return Vec3.atCenterOf(pos.above(height - 1)).add(0, attachUp, 0);
    }

    /**
     * Return the insulator on the crossarm level nearest in height to toward, on the side of the pole toward faces; a span
     * square to the crossarm takes the insulator on the positive world axis, so a straight line keeps to one side.
     */
    @Override
    public Vec3 cableEnd(BlockPos pos, BlockState state, Vec3 toward) {
        if (fittings.length == 0) {
            return attachPoint(pos, state);
        }
        Vec3 base = Vec3.atBottomCenterOf(pos);
        double rise = toward.y - base.y;
        double up = fittings[0];
        for (double f : fittings) {
            if (Math.abs(f - rise) < Math.abs(up - rise)) {
                up = f;
            }
        }
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        Direction arm = Direction.fromAxisAndDirection(facing.getClockWise().getAxis(), Direction.AxisDirection.POSITIVE);
        Vec3 armDir = Vec3.atLowerCornerOf(arm.getUnitVec3i());
        double side = toward.subtract(base).dot(armDir) < -1e-3 ? -reach : reach;
        return base.add(armDir.scale(side)).add(0, up, 0);
    }
}

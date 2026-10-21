package dev.autarkia.colony;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.StringRepresentable;

/**
 * One nanite claim of a colony (05 sections 1.5 and 8.3): the claim box from {@code min} to {@code max} (inclusive),
 * {@code ref_y} (the lower marker), the terrace flag, the cursor (layer y and serpentine index in that layer) and the
 * state. Saved besides the fields of 05 section 1.5: the game time it started (budget order) and ended (bubble timer),
 * the kept containers (bubble positions) and the swarms working on it.
 *
 * <p>Layers run from {@code max.y} down. A layer below {@code ref_y} is inset by {@code floor((ref_y - y) / 2)} on each
 * side while terraces are on, and the inset stops where the layer is 4 wide (or keeps its width when the box is
 * narrower). Each layer runs in rows along x, alternating direction (serpentine), rows stepping along z.
 */
public final class NaniteClaim {
    /** Claim states (05b section 9.2 plus G5). */
    public enum State implements StringRepresentable {
        RUN, WAIT_CHUNK, WAIT_BUFFER, WAIT_FE, PAUSED, DONE;

        public static final Codec<State> CODEC = StringRepresentable.fromEnum(State::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final Codec<NaniteClaim> CODEC = RecordCodecBuilder.create(i -> i.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(c -> c.id),
        BlockPos.CODEC.fieldOf("hive_pos").forGetter(c -> c.hivePos),
        BlockPos.CODEC.fieldOf("min").forGetter(c -> c.min),
        BlockPos.CODEC.fieldOf("max").forGetter(c -> c.max),
        Codec.INT.fieldOf("ref_y").forGetter(c -> c.refY),
        Codec.BOOL.optionalFieldOf("terrace", true).forGetter(c -> c.terrace),
        Codec.INT.fieldOf("cursor_y").forGetter(c -> c.cursorY),
        Codec.INT.optionalFieldOf("cursor_i", 0).forGetter(c -> c.cursorI),
        State.CODEC.optionalFieldOf("state", State.RUN).forGetter(c -> c.state),
        Codec.LONG.optionalFieldOf("started", 0L).forGetter(c -> c.started),
        Codec.LONG.optionalFieldOf("ended", -1L).forGetter(c -> c.ended),
        BlockPos.CODEC.listOf().optionalFieldOf("kept", List.of()).forGetter(c -> c.kept),
        UUIDUtil.CODEC.listOf().optionalFieldOf("swarms", List.of()).forGetter(c -> c.swarms)
    ).apply(i, NaniteClaim::new));

    public final UUID id;
    public final BlockPos hivePos;
    public final BlockPos min;
    public final BlockPos max;
    public final int refY;
    public final boolean terrace;
    public int cursorY;
    public int cursorI;
    public State state;
    public final long started;
    public long ended;
    public final List<BlockPos> kept = new ArrayList<>();
    public final List<UUID> swarms = new ArrayList<>();

    private NaniteClaim(UUID id, BlockPos hivePos, BlockPos min, BlockPos max, int refY, boolean terrace, int cursorY, int cursorI, State state,
                        long started, long ended, List<BlockPos> kept, List<UUID> swarms) {
        this.id = id;
        this.hivePos = hivePos;
        this.min = min;
        this.max = max;
        this.refY = refY;
        this.terrace = terrace;
        this.cursorY = cursorY;
        this.cursorI = cursorI;
        this.state = state;
        this.started = started;
        this.ended = ended;
        this.kept.addAll(kept);
        this.swarms.addAll(swarms);
    }

    /** Create a running claim with its cursor at the first cell of the top layer. */
    public static NaniteClaim create(BlockPos hive, BlockPos min, BlockPos max, int refY, boolean terrace, long now) {
        return new NaniteClaim(UUID.randomUUID(), hive.immutable(), min.immutable(), max.immutable(), refY, terrace, max.getY(), 0, State.RUN, now, -1,
            List.of(), List.of());
    }

    /** Return the inset per side of layer y: {@code floor((ref_y - y) / 2)} below ref_y with terraces on, else 0 (no cap). */
    public int inset(int y) {
        return terrace && y < refY ? (refY - y) / 2 : 0;
    }

    /** Return the layer rectangle {x0, x1, z0, z1} of layer y with the terrace inset, stopping at a 4-wide layer. */
    public int[] rect(int y) {
        int w = max.getX() - min.getX() + 1;
        int d = max.getZ() - min.getZ() + 1;
        int ix = Math.min(inset(y), Math.max(0, (w - 4) / 2));
        int iz = Math.min(inset(y), Math.max(0, (d - 4) / 2));
        return new int[] {min.getX() + ix, max.getX() - ix, min.getZ() + iz, max.getZ() - iz};
    }

    /** Return the number of cells of layer y. */
    public int area(int y) {
        int[] r = rect(y);
        return (r[1] - r[0] + 1) * (r[3] - r[2] + 1);
    }

    /** Return whether pos is a cell of the claim (inside the box and inside the terrace shape of its layer). */
    public boolean inShape(BlockPos p) {
        if (p.getY() < min.getY() || p.getY() > max.getY()) {
            return false;
        }
        int[] r = rect(p.getY());
        return p.getX() >= r[0] && p.getX() <= r[1] && p.getZ() >= r[2] && p.getZ() <= r[3];
    }

    /** Return the cell at serpentine index i of layer y. */
    public BlockPos cell(int y, int i) {
        int[] r = rect(y);
        int w = r[1] - r[0] + 1;
        int row = i / w;
        int col = i % w;
        return new BlockPos(row % 2 == 0 ? r[0] + col : r[1] - col, y, r[2] + row);
    }

    /** Return the serpentine index of a cell inside the shape of its layer. */
    public int index(BlockPos p) {
        int[] r = rect(p.getY());
        int w = r[1] - r[0] + 1;
        int row = p.getZ() - r[2];
        return row * w + (row % 2 == 0 ? p.getX() - r[0] : r[1] - p.getX());
    }

    /** Return whether the cursor has passed the cell (true for every cell once the claim is done). */
    public boolean processed(BlockPos p) {
        if (state == State.DONE) {
            return true;
        }
        return p.getY() > cursorY || p.getY() == cursorY && index(p) < cursorI;
    }

    /** Return whether the cursor ran past the bottom layer. */
    public boolean exhausted() {
        return cursorY < min.getY();
    }

    /** Move the cursor to the next layer while the current one is used up; the cursor ends below min.y. */
    public void normalize() {
        while (cursorY >= min.getY() && cursorI >= area(cursorY)) {
            cursorY--;
            cursorI = 0;
        }
    }

    /** Return the number of layers (box height). */
    public int layers() {
        return max.getY() - min.getY() + 1;
    }

    /** Return the 1-based number of the cursor layer, counted from the top (layers() + 1 once exhausted). */
    public int layer() {
        return max.getY() - cursorY + 1;
    }

    /** Return the number of cells of the whole claim shape. */
    public long totalCells() {
        long n = 0;
        for (int y = min.getY(); y <= max.getY(); y++) {
            n += area(y);
        }
        return n;
    }

    /** Return the number of cells the cursor has passed. */
    public long doneCells() {
        long n = 0;
        for (int y = max.getY(); y > Math.max(cursorY, min.getY() - 1); y--) {
            n += area(y);
        }
        return n + (cursorY >= min.getY() ? cursorI : 0);
    }

    /** Return the progress 0 to 100. */
    public int percent() {
        long total = totalCells();
        return state == State.DONE || total == 0 ? 100 : (int) (doneCells() * 100 / total);
    }

    /** Return whether the claim box intersects the box from a to b (inclusive corners in any order). */
    public boolean intersects(BlockPos a, BlockPos b) {
        return Math.max(min.getX(), Math.min(a.getX(), b.getX())) <= Math.min(max.getX(), Math.max(a.getX(), b.getX()))
            && Math.max(min.getY(), Math.min(a.getY(), b.getY())) <= Math.min(max.getY(), Math.max(a.getY(), b.getY()))
            && Math.max(min.getZ(), Math.min(a.getZ(), b.getZ())) <= Math.min(max.getZ(), Math.max(a.getZ(), b.getZ()));
    }

    /** Return whether the claim box holds pos. */
    public boolean contains(BlockPos p) {
        return p.getX() >= min.getX() && p.getX() <= max.getX() && p.getY() >= min.getY() && p.getY() <= max.getY() && p.getZ() >= min.getZ()
            && p.getZ() <= max.getZ();
    }
}

package dev.autarkia.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * One capsule of a tube network (04 section 1.4): a stack on a route of direction ordinals from a start block. The
 * path position {@code pos} is in pu with 0 at the center of the start block, so block j of the route spans
 * {@code j * 1000 - 500} to {@code j * 1000 + 500}. A launch starts at the origin mouth (500); the route ends at the
 * target mouth ({@link #end()}). A capsule with an empty path is stalled in its start block.
 */
public final class Capsule {
    public static final int BLOCK = 1000;
    public static final int HALF = 500;
    public final int id;
    public final ItemStack stack;
    public long start;
    public byte[] path;
    public int pos;
    public int prevPos;
    public long origin;
    public long target;
    public boolean returning;
    public boolean clogged;
    public int wait;
    public int dist;
    public boolean glow;
    private long @Nullable [] blocks;

    public Capsule(int id, ItemStack stack, long start, byte[] path, int pos, long origin, long target) {
        this.id = id;
        this.stack = stack;
        this.origin = origin;
        this.target = target;
        route(start, path, pos);
    }

    /** Return a copy with another id (adoption by a new anchor); the route, flags and counters are kept. */
    public Capsule withId(int newId) {
        Capsule c = new Capsule(newId, stack, start, path, pos, origin, target);
        c.returning = returning;
        c.clogged = clogged;
        c.wait = wait;
        c.dist = dist;
        return c;
    }

    /** Replace the route; the capsule continues from pos on the new path. */
    public void route(long newStart, byte[] newPath, int newPos) {
        start = newStart;
        path = newPath;
        pos = newPos;
        prevPos = newPos;
        blocks = null;
    }

    /** Return the route blocks: the start block, then one block per path step (the last is the target station). */
    public long[] blocks() {
        if (blocks == null) {
            long[] b = new long[path.length + 1];
            BlockPos p = BlockPos.of(start);
            b[0] = start;
            for (int i = 0; i < path.length; i++) {
                p = p.relative(CapsuleTopology.dir(path[i]));
                b[i + 1] = p.asLong();
            }
            blocks = b;
        }
        return blocks;
    }

    /** Return the end position: the target mouth, or 0 for a stalled capsule. */
    public int end() {
        return path.length == 0 ? 0 : path.length * BLOCK - HALF;
    }

    /** Return true while the capsule waits at its target mouth. */
    public boolean atEnd() {
        return path.length > 0 && pos >= end();
    }

    /** Return the index of the route block that holds path position p (the target station counts). */
    public int blockIndex(int p) {
        return Math.clamp(Math.floorDiv(p + HALF, BLOCK), 0, path.length);
    }

    /** Return the index of the tube block a capsule at p occupies; at the target mouth this is the last tube. */
    public int occIndex(int p) {
        return Math.min(blockIndex(p), Math.max(0, path.length - 1));
    }

    /** Return the block the capsule occupies now. */
    public long currentBlock() {
        return blocks()[occIndex(pos)];
    }

    /** Return the world position at path position p (interpolated positions are allowed). */
    public Vec3 point(float p) {
        long[] b = blocks();
        int j = blockIndex((int) Math.floor(p));
        float f = (p + HALF - j * BLOCK) / BLOCK;
        Vec3 c = Vec3.atCenterOf(BlockPos.of(b[j]));
        if (path.length == 0) {
            return c;
        }
        if (f < 0.5f) {
            Direction in = CapsuleTopology.dir(path[Math.max(0, j - 1)]);
            return c.add(Vec3.atLowerCornerOf(in.getUnitVec3i()).scale(f - 0.5f));
        }
        return j < path.length ? c.add(Vec3.atLowerCornerOf(CapsuleTopology.dir(path[j]).getUnitVec3i()).scale(f - 0.5f)) : c;
    }

    /** Write the capsule (keys id, stack, start, path, pos, origin, target, ret, wait, dist, clog). */
    public void save(ValueOutput out) {
        int[] p = new int[path.length];
        for (int i = 0; i < p.length; i++) {
            p[i] = path[i];
        }
        out.putInt("id", id);
        out.store("stack", ItemStack.CODEC, stack);
        out.putLong("start", start);
        out.putIntArray("path", p);
        out.putInt("pos", pos);
        out.putLong("origin", origin);
        out.putLong("target", target);
        out.putBoolean("ret", returning);
        out.putBoolean("clog", clogged);
        out.putInt("wait", wait);
        out.putInt("dist", dist);
    }

    /** Read a capsule written by {@link #save}; returns null when the stack is missing or the path is invalid. */
    public static @Nullable Capsule load(ValueInput in) {
        ItemStack stack = in.read("stack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        int[] p = in.getIntArray("path").orElse(new int[0]);
        byte[] path = new byte[p.length];
        for (int i = 0; i < p.length; i++) {
            if (p[i] < 0 || p[i] > 5) {
                return null;
            }
            path[i] = (byte) p[i];
        }
        if (stack.isEmpty()) {
            return null;
        }
        Capsule c = new Capsule(in.getIntOr("id", 0), stack, in.getLongOr("start", 0), path, in.getIntOr("pos", 0),
            in.getLongOr("origin", 0), in.getLongOr("target", 0));
        c.returning = in.getBooleanOr("ret", false);
        c.clogged = in.getBooleanOr("clog", false);
        c.wait = in.getIntOr("wait", 0);
        c.dist = in.getIntOr("dist", 0);
        return c;
    }
}

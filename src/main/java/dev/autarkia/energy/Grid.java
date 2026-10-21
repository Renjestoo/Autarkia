package dev.autarkia.energy;

import dev.autarkia.network.MultimeterReadingPayload;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaSounds;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity;
import org.jspecify.annotations.Nullable;

/** Static grid API (02 section 2.7) and the game events that drive the per-level grids. */
public final class Grid {
    private static final Map<ServerLevel, GridLevel> LEVELS = new IdentityHashMap<>();

    /** Result of a span placement; every refusal has an action bar message. */
    public enum SpanResult {
        OK, TOO_LONG, WRONG_TIER, NODE_FULL, PORT_TIER, BLOCKED, SAME_NODE, NOT_A_NODE, TOO_MANY_PARALLEL, NOT_LOADED;

        /** Return the player message for this result. */
        public Component message(CableTier tier) {
            return Component.translatable("message.autarkia.span." + name().toLowerCase(java.util.Locale.ROOT), tier.maxSpan,
                Component.translatable("tier.autarkia." + tier.getSerializedName()));
        }
    }

    /** A resolved grid node: its position (big-block center) and connector or port nature. */
    public record Node(BlockPos pos, @Nullable GridConnector connector, BlockState state) {
        /** Return true when the node is a machine port. */
        public boolean port() {
            return connector == null;
        }
    }

    private Grid() {
    }

    /** Register the game event listeners. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
            if (e.getLevel() instanceof ServerLevel sl) {
                of(sl).tick();
            }
        });
        NeoForge.EVENT_BUS.addListener((ChunkWatchEvent.Sent e) -> of(e.getLevel()).sendChunk(e.getPlayer(), e.getPos()));
        NeoForge.EVENT_BUS.addListener((ChunkWatchEvent.UnWatch e) -> of(e.getLevel()).unsendChunk(e.getPlayer(), e.getPos()));
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load e) -> chunkChanged(e));
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Unload e) -> chunkChanged(e));
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload e) -> {
            if (e.getLevel() instanceof ServerLevel sl) {
                LEVELS.remove(sl);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> LEVELS.clear());
    }

    private static void chunkChanged(ChunkEvent e) {
        if (e.getLevel() instanceof ServerLevel sl && LEVELS.containsKey(sl)) {
            GridLevel g = LEVELS.get(sl);
            for (long n : g.data.nodesIn(e.getChunk().getPos().pack())) {
                g.markDirty(n);
            }
        }
    }

    /** Return the grid of a level. */
    public static GridLevel of(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, GridLevel::new);
    }

    /** Return the grid member at a position (resolving big blocks to their center), or null. */
    public static @Nullable GridMember memberAt(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IMultiBlockEntity m && !m.isCenter()) {
            be = level.getBlockEntity(m.getCenter());
        }
        return be instanceof GridMember gm && gm.gridBuffer() != null ? gm : null;
    }

    /** Resolve a clicked block to a grid node, or null when it is neither a connector nor a machine port. */
    public static @Nullable Node resolve(Level level, BlockPos clicked) {
        BlockPos center = level.getBlockEntity(clicked) instanceof IMultiBlockEntity m ? m.getCenter() : clicked;
        BlockState state = level.getBlockState(center);
        if (state.getBlock() instanceof GridConnector c) {
            return new Node(center, c, state);
        }
        return memberAt(level, center) != null ? new Node(center, null, state) : null;
    }

    /** Return the attach point of a span on a node clicked at a block face. */
    public static Vec3 attachPoint(Node node, BlockPos clicked, Direction face) {
        if (node.connector() != null) {
            return node.connector().attachPoint(node.pos(), node.state());
        }
        return Vec3.atCenterOf(clicked).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.5));
    }

    /** Worker API: place a span between two nodes using their top faces (02 section 2.7). */
    public static SpanResult addSpan(ServerLevel level, BlockPos nodeA, BlockPos nodeB, CableTier tier) {
        return addSpan(level, nodeA, Direction.UP, nodeB, Direction.UP, tier, false);
    }

    /** Place (or with simulate, only check) a span between two clicked blocks under the rules of 02 section 3. */
    public static SpanResult addSpan(ServerLevel level, BlockPos clickedA, Direction faceA, BlockPos clickedB, Direction faceB,
                                     CableTier tier, boolean simulate) {
        if (!level.isLoaded(clickedA) || !level.isLoaded(clickedB)) {
            return SpanResult.NOT_LOADED;
        }
        Node a = resolve(level, clickedA);
        Node b = resolve(level, clickedB);
        if (a == null || b == null) {
            return SpanResult.NOT_A_NODE;
        }
        if (a.pos().equals(b.pos())) {
            return SpanResult.SAME_NODE;
        }
        GridLevel g = of(level);
        SpanResult nodeCheck = checkNode(g, a, tier);
        if (nodeCheck == SpanResult.OK) {
            nodeCheck = checkNode(g, b, tier);
        }
        if (nodeCheck != SpanResult.OK) {
            return nodeCheck;
        }
        Vec3 pa = attachPoint(a, clickedA, faceA);
        Vec3 pb = attachPoint(b, clickedB, faceB);
        float length = (float) pa.distanceTo(pb);
        if (length > tier.maxSpan) {
            return SpanResult.TOO_LONG;
        }
        int parallel = 0;
        IntArrayList ids = g.data.at(a.pos().asLong());
        for (int i = 0; i < ids.size(); i++) {
            Span s = g.data.spans.get(ids.getInt(i));
            if (s != null && s.joins(a.pos().asLong(), b.pos().asLong())) {
                parallel++;
            }
        }
        if (parallel >= 4) {
            return SpanResult.TOO_MANY_PARALLEL;
        }
        if (chordBlocked(level, pa, pb, a.pos(), b.pos())) {
            return SpanResult.BLOCKED;
        }
        if (!simulate) {
            Span s = new Span(g.data.nextSpanId++, a.pos().asLong(), b.pos().asLong(), pa, pb, tier, length, 0, 0, 0);
            g.data.add(s);
            g.markDirty(s.a);
            g.markDirty(s.b);
            g.broadcast(s, false);
        }
        return SpanResult.OK;
    }

    private static SpanResult checkNode(GridLevel g, Node n, CableTier tier) {
        IntArrayList ids = g.data.at(n.pos().asLong());
        if (n.connector() != null) {
            if (!n.connector().acceptsTier(tier)) {
                return SpanResult.WRONG_TIER;
            }
            return ids.size() >= n.connector().maxSpans() ? SpanResult.NODE_FULL : SpanResult.OK;
        }
        for (int i = 0; i < ids.size(); i++) {
            Span s = g.data.spans.get(ids.getInt(i));
            if (s != null && s.tier != tier) {
                return SpanResult.PORT_TIER;
            }
        }
        return ids.size() >= 2 ? SpanResult.NODE_FULL : SpanResult.OK;
    }

    /** Return true when the straight chord between two attach points passes through a collision shape. */
    static boolean chordBlocked(Level level, Vec3 a, Vec3 b, BlockPos nodeA, BlockPos nodeB) {
        double len = a.distanceTo(b);
        int steps = Math.max(2, (int) Math.ceil(len / 0.1));
        for (int i = 1; i < steps; i++) {
            Vec3 p = a.lerp(b, i / (double) steps);
            BlockPos bp = BlockPos.containing(p);
            if (partOf(level, bp, nodeA) || partOf(level, bp, nodeB)) {
                continue;
            }
            VoxelShape shape = level.getBlockState(bp).getCollisionShape(level, bp);
            if (shape.isEmpty()) {
                continue;
            }
            Vec3 local = p.subtract(bp.getX(), bp.getY(), bp.getZ());
            for (AABB box : shape.toAabbs()) {
                if (box.contains(local)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean partOf(Level level, BlockPos pos, BlockPos node) {
        if (pos.equals(node)) {
            return true;
        }
        return level.getBlockEntity(pos) instanceof IMultiBlockEntity m && m.getCenter().equals(node);
    }

    /** Remove one span between two nodes (any order); drops nothing. Returns false when no span joins them. */
    public static boolean removeSpan(ServerLevel level, BlockPos a, BlockPos b) {
        GridLevel g = of(level);
        IntArrayList ids = g.data.at(a.asLong());
        for (int i = 0; i < ids.size(); i++) {
            Span s = g.data.spans.get(ids.getInt(i));
            if (s != null && s.joins(a.asLong(), b.asLong())) {
                remove(g, s);
                return true;
            }
        }
        return false;
    }

    /** Remove a node: every span at it is removed and its full cable cost drops at the node. */
    public static void nodeRemoved(ServerLevel level, BlockPos node) {
        GridLevel g = of(level);
        IntArrayList ids = new IntArrayList(g.data.at(node.asLong()));
        for (int i = 0; i < ids.size(); i++) {
            Span s = g.data.spans.get(ids.getInt(i));
            if (s != null) {
                remove(g, s);
                drop(level, Vec3.atCenterOf(node), s.tier, s.tier.itemCost(s.length));
            }
        }
        g.markDirty(node.asLong());
    }

    /** Mark the node's network for a member refresh (a tap appeared or vanished next to it). */
    public static void tapsChanged(ServerLevel level, BlockPos node) {
        GridNetwork net = of(level).network(node.asLong());
        if (net != null) {
            net.tapsDirty = true;
        }
    }

    static void remove(GridLevel g, Span s) {
        g.data.remove(s.id);
        g.markDirty(s.a);
        g.markDirty(s.b);
        g.broadcast(s, true);
    }

    /** Snap a span (02 section 4.4, M1 form): remove it and drop ceil(L / 16) cable items at each node. */
    static void snap(GridLevel g, Span s) {
        if (!g.data.spans.containsKey(s.id)) {
            return;
        }
        remove(g, s);
        int each = (int) Math.ceil(s.length / 16.0);
        drop(g.level, s.attachA, s.tier, each);
        drop(g.level, s.attachB, s.tier, each);
        Vec3 m = s.mid();
        g.level.playSound(null, m.x, m.y, m.z, AutarkiaSounds.of(s.tier == CableTier.ROPE ? "rope.snap" : "wire.snap"), SoundSource.BLOCKS, 1f, 1f);
    }

    private static void drop(ServerLevel level, Vec3 at, CableTier tier, int count) {
        Item item = cableItem(tier);
        while (count > 0) {
            int n = Math.min(count, item.getDefaultMaxStackSize());
            Containers.dropItemStack(level, at.x, at.y, at.z, new ItemStack(item, n));
            count -= n;
        }
    }

    /** Return the cable item of a tier. */
    public static Item cableItem(CableTier tier) {
        return switch (tier) {
            case ROPE -> AutarkiaItems.ROPE_COIL.get();
            default -> throw new IllegalStateException("No cable item is registered for tier " + tier);
        };
    }

    /** Build the multimeter reading of the node clicked at pos (null when the block is no node). */
    public static @Nullable MultimeterReadingPayload reading(ServerLevel level, BlockPos clicked) {
        Node node = resolve(level, clicked);
        if (node == null) {
            return null;
        }
        GridLevel g = of(level);
        GridNetwork net = g.network(node.pos().asLong());
        List<MultimeterReadingPayload.Line> lines = new ArrayList<>();
        IntArrayList ids = g.data.at(node.pos().asLong());
        for (int i = 0; i < ids.size(); i++) {
            Span s = g.data.spans.get(ids.getInt(i));
            if (s != null) {
                lines.add(new MultimeterReadingPayload.Line(s.id, s.length, (float) Math.abs(s.flow), s.load, s.wear, s.heat, s.temp,
                    s.treeEdge, s.slip));
            }
        }
        if (net == null) {
            return new MultimeterReadingPayload(node.pos(), false, 0, CableTier.ROPE, 0, 0, 0, 0, 1, 0, 0, 0, 0, false, lines);
        }
        return new MultimeterReadingPayload(node.pos(), true, net.id, net.tier, net.nodes.size(), net.spans.size(),
            (float) net.avgGeneration, (float) net.avgDemand, (float) net.avgPhi, (float) net.loss, (long) net.stored, (long) net.capacity,
            (float) net.rho, net.lagging, lines);
    }
}

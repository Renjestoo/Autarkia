package dev.autarkia.energy;

import dev.autarkia.network.MultimeterReadingPayload;
import dev.autarkia.registry.AutarkiaItems;
import dev.autarkia.registry.AutarkiaSounds;
import dev.autarkia.registry.CableTerminals;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
        OK, TOO_LONG, WRONG_TIER, NODE_FULL, PORT_TIER, PORT_KIND, BLOCKED, SAME_NODE, NOT_A_NODE, TOO_MANY_PARALLEL, NOT_LOADED;

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
                long t0 = System.nanoTime();
                of(sl).tick();
                dev.autarkia.command.SystemProfiler.add(dev.autarkia.command.SystemProfiler.Part.GRID, t0);
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

    /** Resolve a clicked block to a grid node, or null when it is neither a connector nor a machine port (tap-only members). */
    public static @Nullable Node resolve(Level level, BlockPos clicked) {
        BlockPos center = level.getBlockEntity(clicked) instanceof IMultiBlockEntity m ? m.getCenter() : clicked;
        BlockState state = level.getBlockState(center);
        if (state.getBlock() instanceof GridConnector c) {
            return new Node(center, c, state);
        }
        GridMember member = memberAt(level, center);
        return member != null && member.takesSpans() ? new Node(center, null, state) : null;
    }

    /** Return the attach point of a span on a node clicked at a block face. */
    public static Vec3 attachPoint(Node node, BlockPos clicked, Direction face) {
        if (node.connector() != null) {
            return node.connector().attachPoint(node.pos(), node.state());
        }
        return Vec3.atCenterOf(clicked).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.5));
    }

    /** Return true when a span of the tier may end on a block: connectors by their tier, machine ports by their terminal kind. */
    public static boolean accepts(@Nullable BlockState state, CableTier tier) {
        if (state == null) {
            return true;
        }
        if (state.getBlock() instanceof GridConnector c) {
            return c.acceptsTier(tier);
        }
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (tier == CableTier.PHASE) {
            return false;
        }
        return tier == CableTier.ROPE ? CableTerminals.takesRope(id) : CableTerminals.takesMetal(id);
    }

    /**
     * Return where the drawn cable of a span of the tier toward the given point ends on a node: among the block's cable
     * terminals of the span's kind (rope sheaves for rope, bushings for metal; tools/terminals.py, turned with the horizontal
     * facing) the one whose outward direction best faces that point, ties going to the nearer one, and on a sheave rim
     * toward that point; else the connector's own end; else the center of the clicked face of the clicked block's shape.
     */
    public static Vec3 cableEnd(Level level, Node node, BlockPos clicked, Direction face, Vec3 toward, CableTier tier) {
        CableTerminals.Terminal[] terminals = CableTerminals.of(BuiltInRegistries.BLOCK.getKey(node.state().getBlock()).getPath());
        if (terminals != null) {
            return terminalEnd(node, terminals, toward, tier);
        }
        if (node.connector() != null) {
            return node.connector().cableEnd(node.pos(), node.state(), toward);
        }
        VoxelShape shape = level.getBlockState(clicked).getShape(level, clicked);
        AABB box = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
        Vec3 n = Vec3.atLowerCornerOf(face.getUnitVec3i());
        return Vec3.atLowerCornerOf(clicked).add(box.getCenter())
            .add(n.x * box.getXsize() / 2, n.y * box.getYsize() / 2, n.z * box.getZsize() / 2);
    }

    private static Vec3 terminalEnd(Node node, CableTerminals.Terminal[] terminals, Vec3 toward, CableTier tier) {
        CableTerminals.Kind wanted = tier == CableTier.ROPE ? CableTerminals.Kind.RIM : CableTerminals.Kind.POINT;
        boolean any = false;
        for (CableTerminals.Terminal t : terminals) {
            any |= t.kind() == wanted;
        }
        BlockState state = node.state();
        Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        Vec3 base = Vec3.atBottomCenterOf(node.pos());
        CableTerminals.Terminal best = null;
        Vec3 bestPoint = base;
        Vec3 bestDir = Vec3.ZERO;
        double bestFacing = 0;
        double bestDist = 0;
        double bestSide = 0;
        for (CableTerminals.Terminal t : terminals) {
            if (any && t.kind() != wanted) {
                continue;
            }
            Vec3 offset = turn(facing, t.x() / 16, t.y() / 16, t.z() / 16);
            Vec3 out = turn(facing, t.outX(), 0, t.outZ());
            Vec3 p = base.add(offset);
            Vec3 flat = new Vec3(toward.x - p.x, 0, toward.z - p.z);
            Vec3 dir = flat.lengthSqr() < 1e-9 ? Vec3.ZERO : flat.normalize();
            double facingScore = out.dot(dir);
            double dist = p.distanceToSqr(toward);
            double side = offset.x + offset.z;
            boolean tie = Math.abs(facingScore - bestFacing) <= 1e-3;
            boolean nearer = dist < bestDist - 1e-6 || (dist <= bestDist + 1e-6 && side > bestSide);
            boolean better = best == null || facingScore > bestFacing + 1e-3 || (tie && nearer);
            if (better) {
                best = t;
                bestPoint = p;
                bestDir = dir;
                bestFacing = facingScore;
                bestDist = dist;
                bestSide = side;
            }
        }
        return best != null && best.kind() == CableTerminals.Kind.RIM ? bestPoint.add(bestDir.scale(best.rim() / 16)) : bestPoint;
    }

    private static Vec3 turn(Direction facing, double x, double y, double z) {
        return switch (facing) {
            case SOUTH -> new Vec3(-x, y, -z);
            case EAST -> new Vec3(-z, y, x);
            case WEST -> new Vec3(z, y, -x);
            default -> new Vec3(x, y, z);
        };
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
        double limit = tier.maxSpan;
        for (Node n : List.of(a, b)) {
            if (n.connector() != null) {
                limit = Math.min(limit, n.connector().spanLimit(tier));
            }
        }
        if (length > limit + 1e-6) {
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
            Span s = new Span(g.data.nextSpanId++, a.pos().asLong(), b.pos().asLong(), cableEnd(level, a, clickedA, faceA, pb, tier),
                cableEnd(level, b, clickedB, faceB, pa, tier), tier, length, 0, 0, 0);
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
            List<Span> spans = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                Span s = g.data.spans.get(ids.getInt(i));
                if (s != null) {
                    spans.add(s);
                }
            }
            return n.connector().accepts(tier, spans);
        }
        if (!accepts(n.state(), tier)) {
            return SpanResult.PORT_KIND;
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
    /** Return whether a straight chord from a to b passes through a solid block other than the two node blocks. */
    public static boolean chordBlocked(Level level, Vec3 a, Vec3 b, BlockPos nodeA, BlockPos nodeB) {
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
        for (GridNetwork net : of(level).networks(node.asLong())) {
            net.tapsDirty = true;
        }
    }

    static void remove(GridLevel g, Span s) {
        g.data.remove(s.id);
        g.markDirty(s.a);
        g.markDirty(s.b);
        g.broadcast(s, true);
    }

    /** Snap a span (02 section 4.4): remove it and spawn a whipping snapped cable end at each node. */
    static void snap(GridLevel g, Span s) {
        if (!g.data.spans.containsKey(s.id)) {
            return;
        }
        GridNetwork net = g.network(s.a, s.tier);
        String cause = s.tier == CableTier.ROPE ? "wear" : g.level.getGameTime() - s.struckAt < 40 ? "lightning" : "overload";
        dev.autarkia.colony.ColonyMemory.at(g.level, BlockPos.containing(s.mid()), "cable_snap",
            (net == null ? 0 : net.id) + ":" + Math.min(s.a, s.b) + "-" + Math.max(s.a, s.b));
        remove(g, s);
        g.ripple(s.a);
        g.ripple(s.b);
        if (s.tier == CableTier.COPPER && cause.equals("overload")) {
            dev.autarkia.registry.AutarkiaAdvancements.awardNear(g.level, s.mid(), "power/red_hot");
        }
        dev.autarkia.Autarkia.LOG.info("[grid] {} span {} snapped ({}) between {} and {}", s.tier.getSerializedName(), s.id, cause, BlockPos.of(s.a),
            BlockPos.of(s.b));
        Vec3 d = s.attachB.subtract(s.attachA);
        double phi = Math.atan2(d.z, d.x);
        g.level.addFreshEntity(SnappedCableEntity.create(g.level, s.attachA, phi, s.tier, s.length));
        g.level.addFreshEntity(SnappedCableEntity.create(g.level, s.attachB, phi + Math.PI, s.tier, s.length));
        if (s.tier == CableTier.ROPE && s.wear >= 1f) {
            dev.autarkia.registry.AutarkiaAdvancements.awardNear(g.level, s.mid(), "power/rope_snap");
        }
        Vec3 m = s.mid();
        g.level.playSound(null, m.x, m.y, m.z, AutarkiaSounds.of(s.tier == CableTier.ROPE ? "rope.snap" : "wire.snap"), SoundSource.BLOCKS, 1f, 1f);
    }

    /**
     * Quench a superconductor span (02 section 4.6, W20): remove it, an explosion of power 2.5 at the chord midpoint (block
     * damage only with quenchBreaksBlocks), 60 CLOUD particles, a camera shake (0.6, 20 ticks, 24 blocks), two snapped
     * cable ends, the lamp ripple, failure event {@code quench} (key: both node positions), {@code quench.blast}, and
     * {@code power/quench} to the players within 32 blocks and to the tenders of both cryo nodes. The removal queues the
     * network rebuild.
     */
    static void quench(GridLevel g, Span s) {
        if (!g.data.spans.containsKey(s.id)) {
            return;
        }
        ServerLevel level = g.level;
        Vec3 m = s.mid();
        remove(g, s);
        level.explode(null, m.x, m.y, m.z, 2.5F, false, dev.autarkia.config.AutarkiaConfig.QUENCH_BREAKS_BLOCKS.get()
            ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, m.x, m.y, m.z, 60, 0.8, 0.5, 0.8, 0.08);
        dev.autarkia.integration.AnthologyBridge.shake(level, m, 0.6F, 20, 24F);
        Vec3 d = s.attachB.subtract(s.attachA);
        double phi = Math.atan2(d.z, d.x);
        level.addFreshEntity(SnappedCableEntity.create(level, s.attachA, phi, s.tier, s.length));
        level.addFreshEntity(SnappedCableEntity.create(level, s.attachB, phi + Math.PI, s.tier, s.length));
        g.ripple(s.a);
        g.ripple(s.b);
        dev.autarkia.colony.ColonyMemory.at(level, BlockPos.containing(m), "quench", Math.min(s.a, s.b) + "-" + Math.max(s.a, s.b));
        level.playSound(null, m.x, m.y, m.z, AutarkiaSounds.of("quench.blast"), SoundSource.BLOCKS, 1f, 1f);
        dev.autarkia.registry.AutarkiaAdvancements.awardNear(level, m, "power/quench");
        for (long end : new long[] {s.a, s.b}) {
            if (level.getBlockEntity(BlockPos.of(end)) instanceof CryoNodeBlockEntity node
                && node.tender().resolve(level) instanceof net.minecraft.server.level.ServerPlayer p) {
                dev.autarkia.registry.AutarkiaAdvancements.award(p, "power/quench");
            }
        }
        g.quenchesTotal++;
        dev.autarkia.Autarkia.LOG.info("[grid] superconductor span {} quenched between {} and {}", s.id, BlockPos.of(s.a), BlockPos.of(s.b));
    }

    /**
     * Return true when a superconductor span end is cold (02 section 4.6): a cryo node while its cells hold coolant, a
     * superconducting coil while it has coolant and is not quenched. Any other end (a machine port, a transformer) carries
     * no coolant and never warms a span.
     */
    public static boolean cold(Level level, long node) {
        BlockEntity be = level.getBlockEntity(BlockPos.of(node));
        if (be instanceof CryoNodeBlockEntity c) {
            return c.cold();
        }
        return !(be instanceof SuperconductingCoilBlockEntity coil) || coil.cold();
    }

    private static void drop(ServerLevel level, Vec3 at, CableTier tier, int count) {
        if (count <= 0) {
            return;
        }
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
            case COPPER -> AutarkiaItems.COPPER_WIRE_SPOOL.get();
            case INSULATED -> AutarkiaItems.INSULATED_CABLE_SPOOL.get();
            case HV -> AutarkiaItems.HV_CABLE_SPOOL.get();
            case SUPERCONDUCTOR -> AutarkiaItems.SUPERCONDUCTOR_SPOOL.get();
            default -> throw new IllegalStateException("No cable item is registered for tier " + tier);
        };
    }

    /** Add the Jade lines of a connector node (02 section 9.2): tier, spans of max, highest span load. */
    public static void nodeLines(ServerLevel level, BlockPos node, CableTier tier, int max, List<Component> out) {
        GridLevel g = of(level);
        IntArrayList ids = g.data.at(node.asLong());
        float load = 0;
        for (int i = 0; i < ids.size(); i++) {
            Span s = g.data.spans.get(ids.getInt(i));
            if (s != null) {
                load = Math.max(load, s.load);
            }
        }
        out.add(Component.translatable("jade.autarkia.tier", Component.translatable("tier.autarkia." + tier.getSerializedName())));
        out.add(Component.translatable("jade.autarkia.spans", ids.size(), max));
        out.add(Component.translatable("jade.autarkia.network_load", Math.round(load * 100)));
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

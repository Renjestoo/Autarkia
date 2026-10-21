package dev.autarkia.energy;

import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaAdvancements;
import dev.autarkia.registry.AutarkiaComponents;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.nikdo53.tinymultiblocklib.blockentities.IMultiBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Phase links (02 section 4.7, decision C15): a link is a grid span of tier PHASE that the wrench places with no item,
 * from a {@code phase_node} to a second {@code phase_node} or a {@code transformer} (decision G3) up to 64 blocks away,
 * with a clear {@code level.clip} (COLLIDER) between the attach points. Every 40 ticks the line of sight of each loaded
 * link is checked; a blocked link is inactive (its nodes are rebuilt, the change event of 02 section 2.2) and renders
 * dim, and it comes back by itself when the line clears.
 */
public final class PhaseLinks {
    public static final int CHECK_TICKS = 40;

    private PhaseLinks() {
    }

    /**
     * Run the wrench on a block for phase linking. The first click on a phase node keeps it in {@code autarkia:link_start}
     * for 600 ticks; the second click on a phase node or a transformer places the link. Return null when the click is no
     * phase action (the wrench then does its usual action).
     */
    public static @Nullable InteractionResult wrench(ServerLevel level, Player player, ItemStack stack, BlockPos clicked, Direction face) {
        Grid.Node node = Grid.resolve(level, clicked);
        boolean phaseNode = node != null && node.state().getBlock() instanceof PhaseNodeBlock;
        AutarkiaComponents.LinkStart start = stack.get(AutarkiaComponents.LINK_START.get());
        long now = level.getGameTime();
        boolean pending = start != null && start.node().dimension().equals(level.dimension()) && now - start.time() <= CableItem.LINK_TIMEOUT
            && level.getBlockState(start.node().pos()).getBlock() instanceof PhaseNodeBlock;
        boolean target = node != null && node.connector() != null && node.connector().acceptsTier(CableTier.PHASE);
        if (!phaseNode && !(pending && target)) {
            return null;
        }
        if (!pending) {
            stack.set(AutarkiaComponents.LINK_START.get(), new AutarkiaComponents.LinkStart(GlobalPos.of(level.dimension(), clicked), face, now));
            player.sendOverlayMessage(Component.translatable("message.autarkia.span.start"));
            return InteractionResult.SUCCESS;
        }
        Grid.SpanResult r = link(level, player, start.node().pos(), clicked);
        if (r != Grid.SpanResult.OK) {
            player.sendOverlayMessage(r.message(CableTier.PHASE));
            return InteractionResult.FAIL;
        }
        stack.remove(AutarkiaComponents.LINK_START.get());
        return InteractionResult.SUCCESS;
    }

    /**
     * Place a phase link between two nodes for a player: the line of sight first, then the span rules of 02 section 3 (64
     * blocks, 4 links per node). On success the player gets {@code power/phase_link} and a message.
     */
    public static Grid.SpanResult link(ServerLevel level, @Nullable Player player, BlockPos a, BlockPos b) {
        Grid.Node na = Grid.resolve(level, a);
        Grid.Node nb = Grid.resolve(level, b);
        if (na == null || nb == null) {
            return Grid.SpanResult.NOT_A_NODE;
        }
        Vec3 pa = Grid.attachPoint(na, a, Direction.UP);
        Vec3 pb = Grid.attachPoint(nb, b, Direction.UP);
        Grid.SpanResult check = Grid.addSpan(level, a, Direction.UP, b, Direction.UP, CableTier.PHASE, true);
        if (check != Grid.SpanResult.OK) {
            return check;
        }
        if (!lineOfSight(level, pa, pb, na.pos(), nb.pos())) {
            return Grid.SpanResult.BLOCKED;
        }
        Grid.SpanResult r = Grid.addSpan(level, a, Direction.UP, b, Direction.UP, CableTier.PHASE, false);
        if (r == Grid.SpanResult.OK) {
            level.playSound(null, pb.x, pb.y, pb.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8f, 1.4f);
            if (player instanceof ServerPlayer sp) {
                AutarkiaAdvancements.award(sp, "power/phase_link");
                sp.sendOverlayMessage(Component.translatable("message.autarkia.phase.linked", String.format(java.util.Locale.ROOT, "%.1f", pa.distanceTo(pb))));
            }
            Autarkia.LOG.info("[grid] phase link {} -> {} ({} blocks)", na.pos().toShortString(), nb.pos().toShortString(),
                String.format(java.util.Locale.ROOT, "%.1f", pa.distanceTo(pb)));
        }
        return r;
    }

    /**
     * Return true when {@code level.clip} with the COLLIDER shapes finds nothing between a and b except the blocks of the
     * two end nodes (their own shapes would stop the ray at once).
     */
    public static boolean lineOfSight(Level level, Vec3 a, Vec3 b, BlockPos nodeA, BlockPos nodeB) {
        Vec3 from = a;
        Vec3 dir = b.subtract(a).normalize();
        for (int i = 0; i < 16; i++) {
            BlockHitResult hit = level.clip(new ClipContext(from, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.MISS) {
                return true;
            }
            BlockPos hp = hit.getBlockPos();
            if (!partOf(level, hp, nodeA) && !partOf(level, hp, nodeB)) {
                return false;
            }
            Optional<Vec3> exit = new AABB(hp).clip(b, from);
            if (exit.isEmpty() || exit.get().distanceToSqr(b) < 1e-6) {
                return true;
            }
            from = exit.get().add(dir.scale(1e-3));
        }
        return false;
    }

    private static boolean partOf(Level level, BlockPos pos, BlockPos node) {
        return pos.equals(node) || level.getBlockEntity(pos) instanceof IMultiBlockEntity m && m.getCenter().equals(node);
    }

    /**
     * Check the line of sight of every phase link with both ends loaded once per 40 ticks; a change flips the span's
     * blocked flag and rebuilds the span component at once, so the link is inactive (or active again) in the tick of the
     * check.
     */
    static void check(GridLevel g, long now) {
        if (now % CHECK_TICKS != 0) {
            return;
        }
        java.util.List<Span> changed = new java.util.ArrayList<>();
        for (Span s : g.data.spans.values()) {
            if (s.tier != CableTier.PHASE || !g.nodeLoaded(s.a) || !g.nodeLoaded(s.b)) {
                continue;
            }
            boolean blocked = !lineOfSight(g.level, s.attachA, s.attachB, s.posA(), s.posB());
            if (blocked != s.blocked) {
                s.blocked = blocked;
                changed.add(s);
                Autarkia.LOG.info("[grid] phase link {} between {} and {}: line of sight {}", s.id, s.posA().toShortString(), s.posB().toShortString(),
                    blocked ? "lost" : "restored");
            }
        }
        for (Span s : changed) {
            g.rebuild(s.a);
        }
    }
}

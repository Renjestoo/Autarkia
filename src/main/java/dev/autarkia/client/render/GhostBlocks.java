package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3fc;

/**
 * Translucent ghost blocks for proposals, Pattern previews and seed previews (replaces the Tiny Multiblock Lib
 * ghost renderer, which crashes 26.3 with improvedTransparency on; 13 risk A6). A ghost lives for its lifetime
 * in client ticks and is removed when the count drops below zero.
 */
public final class GhostBlocks {
    private static final Map<Object, Map<Long, Ghost>> GROUPS = new HashMap<>();
    private static final Direction[] FACES_AND_NULL = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};

    private record Ghost(BlockPos pos, BlockState state, int argb, int fadeMax, int fadeStart, int[] life) {
    }

    private GhostBlocks() {
    }

    /**
     * Add or refresh a ghost in a group.
     *
     * @param group owner key (a project id); clearing the group removes all its ghosts at once
     * @param lifetimeTicks client ticks until removal unless refreshed
     * @param argb tint and alpha in one int
     * @param fadeMax blocks from the camera at which the ghost is no longer drawn
     * @param fadeStart blocks from the camera at which fading starts; must be below fadeMax
     */
    public static void add(Object group, BlockPos pos, BlockState state, int lifetimeTicks, int argb, int fadeMax, int fadeStart) {
        if (fadeStart >= fadeMax) {
            throw new IllegalArgumentException("fadeStart " + fadeStart + " must be below fadeMax " + fadeMax);
        }
        GROUPS.computeIfAbsent(group, g -> new HashMap<>()).put(pos.asLong(), new Ghost(pos.immutable(), state, argb, fadeMax, fadeStart, new int[]{lifetimeTicks}));
    }

    /** Remove every ghost of a group. */
    public static void clear(Object group) {
        GROUPS.remove(group);
    }

    /** Return the number of live ghosts in a group. */
    public static int count(Object group) {
        Map<Long, Ghost> m = GROUPS.get(group);
        return m == null ? 0 : m.size();
    }

    /** Count down lifetimes; called from the client tick. */
    public static void tick() {
        for (Iterator<Map<Long, Ghost>> git = GROUPS.values().iterator(); git.hasNext(); ) {
            Map<Long, Ghost> m = git.next();
            m.values().removeIf(g -> --g.life[0] < 0);
            if (m.isEmpty()) {
                git.remove();
            }
        }
    }

    /** Submit all ghosts as translucent block geometry. */
    public static void submit(SubmitCustomGeometryEvent event) {
        if (GROUPS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        List<Ghost> visible = new ArrayList<>();
        for (Map<Long, Ghost> m : GROUPS.values()) {
            for (Ghost g : m.values()) {
                double dist = Math.sqrt(g.pos.distToCenterSqr(cam));
                if (dist < g.fadeMax && g.state.getRenderShape() == RenderShape.MODEL) {
                    visible.add(g);
                }
            }
        }
        if (visible.isEmpty()) {
            return;
        }
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        RandomSource random = RandomSource.create();
        List<BlockStateModelPart> parts = new ArrayList<>();
        event.getSubmitNodeCollector().submitCustomGeometry(ps, Sheets.translucentBlockItemSheet(), (pose, vc) -> {
            for (Ghost g : visible) {
                double dist = Math.sqrt(g.pos.distToCenterSqr(cam));
                float fade = dist <= g.fadeStart ? 1f : (float) (1.0 - (dist - g.fadeStart) / (g.fadeMax - g.fadeStart));
                int a = Math.max(0, Math.min(255, (int) (((g.argb >>> 24) & 0xFF) * fade)));
                BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(g.state);
                random.setSeed(42L);
                parts.clear();
                model.collectParts(net.minecraft.client.renderer.block.BlockAndTintGetter.EMPTY, BlockPos.ZERO, g.state, random, parts);
                for (BlockStateModelPart part : parts) {
                    for (Direction d : FACES_AND_NULL) {
                        for (BakedQuad q : part.getQuads(d)) {
                            emit(vc, pose, q, g.pos, g.argb, a);
                        }
                    }
                }
            }
        });
        ps.popPose();
    }

    private static void emit(VertexConsumer vc, PoseStack.Pose pose, BakedQuad q, BlockPos pos, int argb, int alpha) {
        Direction n = q.direction();
        int r = (argb >> 16) & 0xFF;
        int gr = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        for (int i = 0; i < 4; i++) {
            Vector3fc p = q.position(i);
            long uv = q.packedUV(i);
            vc.addVertex(pose, pos.getX() + p.x(), pos.getY() + p.y(), pos.getZ() + p.z())
                .setColor(r, gr, b, alpha)
                .setUv(UVPair.unpackU(uv), UVPair.unpackV(uv))
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, n.getStepX(), n.getStepY(), n.getStepZ());
        }
    }
}

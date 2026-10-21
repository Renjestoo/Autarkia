package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaItems;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.minecraft.tags.BlockTags;

/**
 * The {@code survey_lens} view (08a section 2.5): while a lens is held, the client finds the ore deposits (connected
 * ore blocks of one kind) within 64 blocks every 2 seconds and draws each deposit's box as an outline in the ore's
 * color that shows through terrain (own line pipeline without depth test), nearest 256 deposits.
 */
public final class SurveyOverlay {
    private static final int RADIUS = 64;
    private static final int MAX = 256;
    private static final RenderPipeline PIPELINE = RenderPipelines.LINES.toBuilder().withLocation(Autarkia.id("pipeline/survey_lines"))
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false)).build();
    private static RenderType type;
    private static final Map<Block, Integer> COLORS = Map.ofEntries(Map.entry(Blocks.IRON_ORE, 0xFFD8AF93), Map.entry(Blocks.DEEPSLATE_IRON_ORE, 0xFFD8AF93),
        Map.entry(Blocks.COPPER_ORE, 0xFFE0773A), Map.entry(Blocks.DEEPSLATE_COPPER_ORE, 0xFFE0773A), Map.entry(Blocks.GOLD_ORE, 0xFFFCEE4B),
        Map.entry(Blocks.DEEPSLATE_GOLD_ORE, 0xFFFCEE4B), Map.entry(Blocks.COAL_ORE, 0xFF606060), Map.entry(Blocks.DEEPSLATE_COAL_ORE, 0xFF606060),
        Map.entry(Blocks.DIAMOND_ORE, 0xFF5DECF5), Map.entry(Blocks.DEEPSLATE_DIAMOND_ORE, 0xFF5DECF5), Map.entry(Blocks.REDSTONE_ORE, 0xFFFF3030),
        Map.entry(Blocks.DEEPSLATE_REDSTONE_ORE, 0xFFFF3030), Map.entry(Blocks.LAPIS_ORE, 0xFF3060E0), Map.entry(Blocks.DEEPSLATE_LAPIS_ORE, 0xFF3060E0),
        Map.entry(Blocks.EMERALD_ORE, 0xFF20E070), Map.entry(Blocks.DEEPSLATE_EMERALD_ORE, 0xFF20E070));

    private record Deposit(AABB box, int color, int count) {
    }

    private static List<Deposit> deposits = List.of();
    private static int heldTicks;

    private SurveyOverlay() {
    }

    /** Register the outline pipeline (mod bus). */
    public static void pipelines(RegisterRenderPipelinesEvent e) {
        e.registerPipeline(PIPELINE);
    }

    private static RenderType type() {
        if (type == null) {
            type = RenderType.create("autarkia_survey_lines", RenderSetup.builder(PIPELINE).createRenderSetup());
        }
        return type;
    }

    /** Rescan every 40 client ticks while the player holds a lens; clear the view when it is put away. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        boolean held = p != null && (p.getMainHandItem().is(AutarkiaItems.SURVEY_LENS.get()) || p.getOffhandItem().is(AutarkiaItems.SURVEY_LENS.get()));
        if (!held || mc.level == null) {
            heldTicks = 0;
            deposits = List.of();
            return;
        }
        if (heldTicks++ % 40 == 0) {
            deposits = scan(mc.level, p.blockPosition());
        }
    }

    /** Return the number of deposits now shown. */
    public static int shown() {
        return deposits.size();
    }

    private static final net.minecraft.tags.TagKey<Block> C_ORES = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
        net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "ores"));

    private static boolean ore(BlockState st) {
        return st.is(BlockTags.ORES) || st.is(C_ORES);
    }

    private static int color(BlockState st) {
        return COLORS.getOrDefault(st.getBlock(), 0xFFE8E8E8);
    }

    private static List<Deposit> scan(ClientLevel level, BlockPos center) {
        LongOpenHashSet seen = new LongOpenHashSet();
        List<Deposit> out = new ArrayList<>();
        int minY = Math.max(level.getMinY(), center.getY() - RADIUS);
        int maxY = Math.min(level.getMaxY(), center.getY() + RADIUS);
        for (int cx = (center.getX() - RADIUS) >> 4; cx <= (center.getX() + RADIUS) >> 4; cx++) {
            for (int cz = (center.getZ() - RADIUS) >> 4; cz <= (center.getZ() + RADIUS) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) {
                    continue;
                }
                LevelChunkSection[] sections = chunk.getSections();
                for (int si = 0; si < sections.length; si++) {
                    int baseY = level.getSectionYFromSectionIndex(si) << 4;
                    if (baseY + 15 < minY || baseY > maxY || sections[si].hasOnlyAir() || !sections[si].maybeHas(SurveyOverlay::ore)) {
                        continue;
                    }
                    for (int i = 0; i < 4096; i++) {
                        int x = i & 15;
                        int z = i >> 4 & 15;
                        int y = i >> 8;
                        BlockState st = sections[si].getBlockState(x, y, z);
                        BlockPos pos = new BlockPos((cx << 4) + x, baseY + y, (cz << 4) + z);
                        if (ore(st) && pos.distSqr(center) <= RADIUS * RADIUS && seen.add(pos.asLong())) {
                            out.add(fill(level, pos, st.getBlock(), seen));
                        }
                    }
                }
            }
        }
        Vec3 c = Vec3.atCenterOf(center);
        out.sort(Comparator.comparingDouble(d -> d.box.getCenter().distanceToSqr(c)));
        return out.subList(0, Math.min(MAX, out.size()));
    }

    private static Deposit fill(ClientLevel level, BlockPos start, Block block, LongOpenHashSet seen) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(start));
        AABB box = new AABB(start);
        int count = 0;
        while (!queue.isEmpty() && count < 512) {
            BlockPos p = queue.poll();
            count++;
            box = box.minmax(new AABB(p));
            for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1))) {
                if (level.getBlockState(n).is(block) && seen.add(n.asLong())) {
                    queue.add(n.immutable());
                }
            }
        }
        return new Deposit(box.inflate(0.02), color(block.defaultBlockState()), count);
    }

    /** Submit the deposit outlines. */
    public static void submit(SubmitCustomGeometryEvent event) {
        if (deposits.isEmpty()) {
            return;
        }
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        List<Deposit> list = deposits;
        event.getSubmitNodeCollector().submitCustomGeometry(ps, type(), (pose, vc) -> {
            for (Deposit d : list) {
                box(vc, pose, d.box, d.color);
            }
        });
        ps.popPose();
    }

    private static void box(VertexConsumer vc, PoseStack.Pose pose, AABB b, int argb) {
        double[][] corners = {{b.minX, b.minY, b.minZ}, {b.maxX, b.minY, b.minZ}, {b.maxX, b.minY, b.maxZ}, {b.minX, b.minY, b.maxZ},
            {b.minX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.maxZ}, {b.minX, b.maxY, b.maxZ}};
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            double[] a = corners[e[0]];
            double[] c = corners[e[1]];
            float nx = (float) Math.signum(c[0] - a[0]);
            float ny = (float) Math.signum(c[1] - a[1]);
            float nz = (float) Math.signum(c[2] - a[2]);
            line(vc, pose, a, argb, nx, ny, nz);
            line(vc, pose, c, argb, nx, ny, nz);
        }
    }

    private static void line(VertexConsumer vc, PoseStack.Pose pose, double[] p, int argb, float nx, float ny, float nz) {
        vc.addVertex(pose, (float) p[0], (float) p[1], (float) p[2]).setColor(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, 255)
            .setNormal(pose, nx, ny, nz).setLineWidth(2.5f);
    }
}

package dev.autarkia.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaClientConfig;
import dev.autarkia.energy.Catenary;
import dev.autarkia.energy.CableTier;
import dev.autarkia.network.SpanListPayload;
import dev.autarkia.network.SpanStatePayload;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * Client store of all spans (filled by span_list and span_state) and their catenary tube renderer, drawn from
 * SubmitCustomGeometryEvent with per-span frustum culling (09 section 8.1, 02 section 4.1).
 */
public final class ClientSpans {
    private static final Int2ObjectOpenHashMap<Span> SPANS = new Int2ObjectOpenHashMap<>();
    private static long clientTicks;

    /** One client span with cached render points. */
    public static final class Span {
        public final int id;
        public final Vec3 a;
        public final Vec3 b;
        public final CableTier tier;
        public float heat;
        public float load;
        public float temp;
        public boolean slip;
        public boolean active;
        public double scroll;
        public Vec3[] points;
        public double cachedSag = -1;
        public long cachedAt = -100;
        public final AABB bounds;

        Span(SpanListPayload.Entry e) {
            this.id = e.id();
            this.a = e.a();
            this.b = e.b();
            this.tier = e.tier();
            this.heat = e.heat();
            this.load = e.load();
            this.temp = e.temp();
            this.active = e.active();
            double len = a.distanceTo(b);
            double sagMax = 0.25 * len + 2.5;
            this.bounds = new AABB(Math.min(a.x, b.x), Math.min(a.y, b.y) - sagMax, Math.min(a.z, b.z), Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z)).inflate(1.0);
        }

        /** Return the current sag depth from synced heat and load (02 section 4.1). */
        public double sag() {
            double h = tier == CableTier.SUPERCONDUCTOR ? 0 : heat;
            return Catenary.sagDepth(tier, a.distanceTo(b), h, load);
        }
    }

    private ClientSpans() {
    }

    /** Apply a span_list payload. */
    public static void handleList(SpanListPayload p) {
        for (SpanListPayload.Entry e : p.entries()) {
            if (e.remove()) {
                SPANS.remove(e.id());
            } else {
                SPANS.put(e.id(), new Span(e));
            }
        }
    }

    /** Apply a span_state payload. */
    public static void handleState(SpanStatePayload p) {
        for (SpanStatePayload.Entry e : p.entries()) {
            Span s = SPANS.get(e.id());
            if (s != null) {
                s.heat = e.heatF();
                s.load = e.loadF();
                s.temp = e.tempF();
                s.slip = e.slip();
                s.active = e.active();
            }
        }
    }

    /** Return a span by id, or null. */
    public static Span get(int id) {
        return SPANS.get(id);
    }

    /** Return the number of spans the client knows. */
    public static int size() {
        return SPANS.size();
    }

    /** Return all spans (read only use). */
    public static Iterable<Span> all() {
        return SPANS.values();
    }

    /** Drop all spans (level change or disconnect). */
    public static void clear() {
        SPANS.clear();
    }

    /** Advance texture scroll and spawn heat and frost particles; called from the client tick. */
    public static void tick() {
        clientTicks++;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        boolean particles = AutarkiaClientConfig.CABLE_PARTICLES.get();
        for (Span s : SPANS.values()) {
            double speed = s.tier == CableTier.ROPE ? (s.load > 0.02 ? Math.min(1.0, s.load) / (s.slip ? Math.max(1.0, s.load) : 1.0) : 0) : 0;
            s.scroll += speed * 0.08;
            if (!particles || s.points == null) {
                continue;
            }
            Vec3 mid = s.points[s.points.length / 2];
            if (s.tier.hasHeat() && s.tier != CableTier.INSULATED) {
                if (s.heat >= 0.9f && clientTicks % 4 == 0) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, mid.x, mid.y, mid.z, 0, 0.02, 0);
                } else if (s.heat >= 0.7f && clientTicks % 10 == 0) {
                    level.addParticle(ParticleTypes.SMOKE, mid.x, mid.y, mid.z, 0, 0.03, 0);
                }
            } else if (s.tier == CableTier.INSULATED) {
                if (s.heat >= 0.9f && clientTicks % 6 == 0) {
                    level.addParticle(ParticleTypes.FLAME, mid.x, mid.y, mid.z, 0, -0.02, 0);
                } else if (s.heat >= 0.7f && clientTicks % 8 == 0) {
                    level.addParticle(ParticleTypes.LARGE_SMOKE, mid.x, mid.y, mid.z, 0, 0.02, 0);
                }
            } else if (s.tier == CableTier.SUPERCONDUCTOR) {
                Vec3 p = s.points[(int) (clientTicks / 2 % s.points.length)];
                if (s.temp < 0.3f && clientTicks % 10 == 0) {
                    level.addParticle(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 0, -0.01, 0);
                } else if (s.temp >= 0.7f && clientTicks % 4 == 0) {
                    level.addParticle(ParticleTypes.CLOUD, p.x, p.y, p.z, 0, 0.05, 0);
                } else if (s.temp >= 0.3f && s.temp < 0.7f && clientTicks % 8 == 0) {
                    level.addParticle(ParticleTypes.WHITE_SMOKE, p.x, p.y, p.z, 0, -0.02, 0);
                }
            }
        }
    }

    /** Submit all visible spans. */
    public static void submit(SubmitCustomGeometryEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || SPANS.isEmpty()) {
            return;
        }
        var camState = event.getLevelRenderState().cameraRenderState;
        Vec3 cam = camState.pos;
        double maxDist = AutarkiaClientConfig.SPAN_RENDER_DISTANCE.get();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        for (Span s : SPANS.values()) {
            if (s.tier == CableTier.PHASE) {
                continue;
            }
            if (s.bounds.distanceToSqr(cam) > maxDist * maxDist || !camState.cullFrustum.isVisible(s.bounds)) {
                continue;
            }
            refreshPoints(level, s);
            RenderType rt = s.tier == CableTier.ROPE ? RenderTypes.entityCutout(texture(s.tier)) : RenderTypes.entityTranslucentEmissive(texture(s.tier));
            Vec3 mid = s.points[s.points.length / 2];
            BlockPos lp = BlockPos.containing(mid);
            int light = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, lp), level.getBrightness(LightLayer.SKY, lp));
            int color = tint(s);
            if (s.tier.hasHeat() && s.tier != CableTier.INSULATED && s.heat >= 0.4f) {
                light = LightCoordsUtil.FULL_BRIGHT;
            }
            final int lightF = light;
            event.getSubmitNodeCollector().submitCustomGeometry(ps, rt, (pose, vc) -> tube(vc, pose, s.points, s.tier.thickness, color, lightF, s.scroll));
        }
        ps.popPose();
    }

    /** Return the cable texture of a tier. */
    public static Identifier texture(CableTier tier) {
        return Autarkia.id("textures/entity/cable/" + tier.getSerializedName() + ".png");
    }

    private static int tint(Span s) {
        if (s.tier == CableTier.SUPERCONDUCTOR) {
            return s.temp < 0.3f ? 0xFFDDF2FF : 0xFFFFFFFF;
        }
        if (!s.tier.hasHeat() || s.heat < 0.4f) {
            return 0xFFFFFFFF;
        }
        if (s.tier == CableTier.INSULATED) {
            int v = (int) Mth.lerp(Math.min(1f, (s.heat - 0.4f) / 0.6f), 255, 90);
            return 0xFF000000 | (v << 16) | (v << 8) | v;
        }
        if (s.heat < 0.7f) {
            return blend(0xFFFFFFFF, 0xFFFF8C1A, (s.heat - 0.4f) / 0.3f);
        }
        if (s.heat < 0.9f) {
            return blend(0xFFFF8C1A, 0xFFFF2A00, (s.heat - 0.7f) / 0.2f);
        }
        boolean flicker = (clientTicks / 2 + s.id) % 3 == 0;
        return flicker ? 0xFFFFF0C0 : 0xFFFFD27F;
    }

    private static int blend(int c1, int c2, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int r = (int) Mth.lerp(t, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, c1 & 0xFF, c2 & 0xFF);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void refreshPoints(ClientLevel level, Span s) {
        double sag = s.sag();
        if (s.points != null && Math.abs(sag - s.cachedSag) < 0.01 && clientTicks - s.cachedAt < (s.tier == CableTier.HV ? 1 : 20)) {
            return;
        }
        s.cachedSag = sag;
        s.cachedAt = clientTicks;
        double sway = s.tier == CableTier.HV ? HvSway.amplitude(level, s) : 0;
        Vec3[] pts = Catenary.points(s.a, s.b, sag);
        if (sway != 0) {
            Vec3 dir = HvSway.direction(level, s);
            for (int i = 1; i < pts.length - 1; i++) {
                double t = i / (double) (pts.length - 1);
                pts[i] = pts[i].add(dir.scale(sway * 4 * t * (1 - t)));
            }
        }
        double half = s.tier.thickness / 2;
        for (int i = 1; i < pts.length - 1; i++) {
            double t = i / (double) (pts.length - 1);
            double chordY = Mth.lerp(t, s.a.y, s.b.y);
            BlockPos bp = BlockPos.containing(pts[i]);
            VoxelShape shape = level.getBlockState(bp).getCollisionShape(level, bp);
            if (!shape.isEmpty()) {
                double top = bp.getY() + shape.max(net.minecraft.core.Direction.Axis.Y) + half;
                double localX = pts[i].x - bp.getX();
                double localZ = pts[i].z - bp.getZ();
                boolean inside = shape.bounds().contains(localX, pts[i].y - bp.getY(), localZ);
                if (inside && top <= chordY) {
                    pts[i] = new Vec3(pts[i].x, top, pts[i].z);
                }
            }
        }
        s.points = pts;
    }

    /** Emit a square tube along the points. */
    public static void tube(VertexConsumer vc, PoseStack.Pose pose, Vec3[] pts, float thick, int argb, int light, double scroll) {
        double r = thick / 2.0;
        double dist = 0;
        for (int i = 0; i < pts.length - 1; i++) {
            Vec3 p0 = pts[i];
            Vec3 p1 = pts[i + 1];
            Vec3 d = p1.subtract(p0);
            double len = d.length();
            if (len < 1e-6) {
                continue;
            }
            Vec3 dn = d.scale(1 / len);
            Vec3 side = dn.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1e-6) {
                side = new Vec3(1, 0, 0);
            }
            side = side.normalize().scale(r);
            Vec3 up = side.cross(dn).normalize().scale(r);
            double u0 = dist * 2 + scroll;
            double u1 = (dist + len) * 2 + scroll;
            dist += len;
            Vec3[] corners = {side.add(up), side.scale(-1).add(up), side.scale(-1).add(up.scale(-1)), side.add(up.scale(-1))};
            for (int f = 0; f < 4; f++) {
                Vec3 c0 = corners[f];
                Vec3 c1 = corners[(f + 1) % 4];
                Vec3 n = c0.add(c1).normalize();
                quad(vc, pose, p0.add(c0), p1.add(c0), p1.add(c1), p0.add(c1), (float) u0, (float) u1, argb, light, n);
            }
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float u1, int argb, int light, Vec3 n) {
        vertex(vc, pose, a, u0, 0f, argb, light, n);
        vertex(vc, pose, b, u1, 0f, argb, light, n);
        vertex(vc, pose, c, u1, 1f, argb, light, n);
        vertex(vc, pose, d, u0, 1f, argb, light, n);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, int argb, int light, Vec3 n) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }
}

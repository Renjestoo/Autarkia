package dev.autarkia.client.render;

import com.craisinlord.anthology.beam.client.BeamRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.autarkia.Autarkia;
import dev.autarkia.network.BeamVisualPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** Client beams from beam_visual payloads, drawn as crossed camera-independent ribbons with the anthology beam render type (09 R17). */
public final class BeamVisuals {
    private static final List<Beam> BEAMS = new ArrayList<>();

    private record Beam(Vec3 from, Vec3 to, int[] ticks, int color, byte kind, float width) {
    }

    private BeamVisuals() {
    }

    /**
     * Add a beam from a payload; a rail payload (07 section 7.5), a nanite ore stream (05 section 8.3, in the payload
     * color) and a restoration debris arc (05 section 8.5, 0x9FE8FF to 0xF5D76E) become a glow trail entity instead.
     */
    public static void handle(BeamVisualPayload p) {
        if (p.kind() == BeamVisualPayload.KIND_RAIL || p.kind() == BeamVisualPayload.KIND_STREAM || p.kind() == BeamVisualPayload.KIND_RESTORE) {
            net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                boolean stream = p.kind() == BeamVisualPayload.KIND_STREAM;
                boolean restore = p.kind() == BeamVisualPayload.KIND_RESTORE;
                com.klikli_dev.magicparticleslib.premade.projectile.VisualEntitySpawner.spawn(level,
                    new com.klikli_dev.magicparticleslib.premade.projectile.glowtrail.GlowTrailProjectile(level, p.from(), p.to())
                        .colors(restore ? 0xFF9FE8FF : stream ? p.color() : 0xFF5FB8B0, restore ? 0xFFF5D76E : stream ? p.color() : 0xFFA8F0DC));
            }
            return;
        }
        BEAMS.add(new Beam(p.from(), p.to(), new int[]{p.ticks()}, p.color(), p.kind(), p.width()));
    }

    /** Add a client-side beam for a number of ticks. */
    public static void add(Vec3 from, Vec3 to, int ticks, int color, byte kind, float width) {
        BEAMS.add(new Beam(from, to, new int[]{ticks}, color, kind, width));
    }

    /** Return the number of live beams. */
    public static int count() {
        return BEAMS.size();
    }

    /** Count down beam lifetimes. */
    public static void tick() {
        BEAMS.removeIf(b -> --b.ticks[0] < 0);
    }

    /** Submit all live beams. */
    public static void submit(SubmitCustomGeometryEvent event) {
        if (BEAMS.isEmpty() || Minecraft.getInstance().level == null) {
            return;
        }
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        for (Beam b : BEAMS) {
            Identifier tex = Autarkia.id(switch (b.kind) {
                case BeamVisualPayload.KIND_DISASSEMBLER -> "textures/entity/beam/disassembler.png";
                case BeamVisualPayload.KIND_PHASE -> "textures/entity/cable/phase.png";
                default -> "textures/entity/beam/lance.png";
            });
            event.getSubmitNodeCollector().submitCustomGeometry(ps, BeamRenderTypes.beam(tex), (pose, vc) -> ribbon(vc, pose, b));
        }
        ps.popPose();
    }

    /** Submit one phase link as a beam with the phase texture: full white while active, dim while its line of sight is blocked. */
    public static void phase(SubmitCustomGeometryEvent event, PoseStack ps, Vec3 a, Vec3 b, boolean active) {
        Beam beam = new Beam(a, b, new int[] {0}, active ? 0xFFFFFFFF : 0x50505050, BeamVisualPayload.KIND_PHASE, 0.12f);
        event.getSubmitNodeCollector().submitCustomGeometry(ps, BeamRenderTypes.beam(Autarkia.id("textures/entity/cable/phase.png")),
            (pose, vc) -> ribbon(vc, pose, beam));
    }

    private static void ribbon(VertexConsumer vc, PoseStack.Pose pose, Beam b) {
        Vec3 d = b.to.subtract(b.from);
        double len = d.length();
        if (len < 1e-4) {
            return;
        }
        Vec3 dn = d.scale(1 / len);
        Vec3 s1 = dn.cross(new Vec3(0, 1, 0));
        if (s1.lengthSqr() < 1e-6) {
            s1 = new Vec3(1, 0, 0);
        }
        s1 = s1.normalize().scale(b.width / 2);
        Vec3 s2 = s1.cross(dn).normalize().scale(b.width / 2);
        float scroll = (Minecraft.getInstance().level.getGameTime() % 40) / 40f;
        for (Vec3 s : new Vec3[]{s1, s2}) {
            v(vc, pose, b.from.add(s), -scroll, 0, b.color);
            v(vc, pose, b.to.add(s), (float) len - scroll, 0, b.color);
            v(vc, pose, b.to.subtract(s), (float) len - scroll, 1, b.color);
            v(vc, pose, b.from.subtract(s), -scroll, 1, b.color);
        }
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float vv, int color) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(color).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }
}

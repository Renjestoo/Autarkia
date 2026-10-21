package dev.autarkia.client;

import dev.autarkia.Autarkia;
import dev.autarkia.client.render.BeamVisuals;
import dev.autarkia.client.render.ClientSpans;
import dev.autarkia.client.render.GhostBlocks;
import dev.autarkia.config.AutarkiaClientConfig;
import dev.autarkia.network.BeamVisualPayload;
import dev.autarkia.network.ProjectPreviewPayload;
import dev.autarkia.network.SpanListPayload;
import dev.autarkia.network.SpanStatePayload;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client entry point: renderers, screens, payload handlers, HUD and key mappings. */
@Mod(value = Autarkia.MOD_ID, dist = Dist.CLIENT)
public final class AutarkiaClient {
    public AutarkiaClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, AutarkiaClientConfig.SPEC, "autarkia-client.toml");
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(AutarkiaClient::payloads);
        NeoForge.EVENT_BUS.addListener(AutarkiaClient::tick);
        NeoForge.EVENT_BUS.addListener(ClientDirector::tick);
        NeoForge.EVENT_BUS.addListener(AutarkiaClient::geometry);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientSpans.clear());
    }

    private static void payloads(RegisterClientPayloadHandlersEvent e) {
        e.register(SpanListPayload.TYPE, (p, ctx) -> ClientSpans.handleList(p));
        e.register(SpanStatePayload.TYPE, (p, ctx) -> ClientSpans.handleState(p));
        e.register(BeamVisualPayload.TYPE, (p, ctx) -> BeamVisuals.handle(p));
        e.register(ProjectPreviewPayload.TYPE, (p, ctx) -> previews(p));
    }

    private static void previews(ProjectPreviewPayload p) {
        GhostBlocks.clear(p.projectId());
        for (ProjectPreviewPayload.Ghost g : p.entries()) {
            GhostBlocks.add(p.projectId(), g.blockPos(), Block.stateById(g.stateId()), 600, p.argb(), 64, 48);
        }
    }

    private static void tick(ClientTickEvent.Post e) {
        GhostBlocks.tick();
        BeamVisuals.tick();
        ClientSpans.tick();
    }

    private static void geometry(SubmitCustomGeometryEvent e) {
        ClientSpans.submit(e);
        GhostBlocks.submit(e);
        BeamVisuals.submit(e);
    }
}

package dev.autarkia.client;

import dev.autarkia.Autarkia;
import dev.autarkia.client.render.BeamVisuals;
import dev.autarkia.client.render.ChuteRenderer;
import dev.autarkia.client.render.ClientSpans;
import dev.autarkia.client.render.CrateRenderer;
import dev.autarkia.client.render.GeoMachineRenderer;
import dev.autarkia.client.render.GhostBlocks;
import dev.autarkia.client.render.TallyBoardRenderer;
import dev.autarkia.client.screen.MultimeterScreen;
import dev.autarkia.client.screen.SlotScreen;
import dev.autarkia.config.AutarkiaClientConfig;
import dev.autarkia.logistics.TransitOwner;
import dev.autarkia.logistics.TransitSegment;
import dev.autarkia.network.BeamVisualPayload;
import dev.autarkia.network.MultimeterReadingPayload;
import dev.autarkia.network.ProjectPreviewPayload;
import dev.autarkia.network.SpanListPayload;
import dev.autarkia.network.SpanStatePayload;
import dev.autarkia.network.TransitDeltaPayload;
import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client entry point: renderers, screens, payload handlers, HUD and key mappings. */
@Mod(value = Autarkia.MOD_ID, dist = Dist.CLIENT)
public final class AutarkiaClient {
    static final net.minecraft.client.KeyMapping.Category KEYS = new net.minecraft.client.KeyMapping.Category(Autarkia.id("main"));
    static final net.minecraft.client.KeyMapping MODE_KEY = new net.minecraft.client.KeyMapping("key.autarkia.mode",
        net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME, com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD,
        com.mojang.blaze3d.platform.InputConstants.KEY_V, KEYS);

    public AutarkiaClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(AutarkiaClient::keys);
        modBus.addListener(AutarkiaClient::layers);
        container.registerConfig(ModConfig.Type.CLIENT, AutarkiaClientConfig.SPEC, "autarkia-client.toml");
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(AutarkiaClient::payloads);
        modBus.addListener(AutarkiaClient::renderers);
        modBus.addListener(AutarkiaClient::screens);
        ClientRecipes.register();
        NeoForge.EVENT_BUS.addListener(AutarkiaClient::tick);
        NeoForge.EVENT_BUS.addListener(ClientDirector::tick);
        NeoForge.EVENT_BUS.addListener(AutarkiaClient::geometry);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            ClientSpans.clear();
            ClientColony.clear();
        });
        guideme.Guide.builder(dev.autarkia.colony.FieldManualItem.GUIDE).folder("guides/autarkia").build();
    }

    private static void payloads(RegisterClientPayloadHandlersEvent e) {
        e.register(SpanListPayload.TYPE, (p, ctx) -> ClientSpans.handleList(p));
        e.register(SpanStatePayload.TYPE, (p, ctx) -> ClientSpans.handleState(p));
        e.register(BeamVisualPayload.TYPE, (p, ctx) -> BeamVisuals.handle(p));
        e.register(ProjectPreviewPayload.TYPE, (p, ctx) -> ClientColony.preview(p));
        e.register(dev.autarkia.network.ColonySyncPayload.TYPE, (p, ctx) -> ClientColony.sync(p));
        e.register(dev.autarkia.network.WorkerDebugPayload.TYPE, (p, ctx) -> dev.autarkia.client.hud.WorkerDebugHud.show(p));
        e.register(MultimeterReadingPayload.TYPE, (p, ctx) -> Minecraft.getInstance().gui.setScreen(new MultimeterScreen(p)));
        e.register(TransitDeltaPayload.TYPE, (p, ctx) -> transit(p));
    }

    private static void transit(TransitDeltaPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getBlockEntity(p.owner()) instanceof TransitOwner owner)) {
            return;
        }
        TransitSegment seg = owner.segment();
        for (TransitDeltaPayload.Op op : p.ops()) {
            switch (op.kind()) {
                case TransitDeltaPayload.ADD -> seg.insertWithId(op.id(), op.stack(), op.pos());
                case TransitDeltaPayload.REMOVE -> seg.remove(op.id());
                case TransitDeltaPayload.BLOCK -> seg.blocked = true;
                default -> seg.blocked = false;
            }
        }
    }

    private static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent e) {
        e.registerCategory(KEYS);
        e.register(MODE_KEY);
    }

    private static void layers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent e) {
        e.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR, Autarkia.id("equipment_hud"), dev.autarkia.client.hud.EquipmentHud::render);
        e.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR, Autarkia.id("worker_debug"), dev.autarkia.client.hud.WorkerDebugHud::render);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.SNAPPED_CABLE.get(), dev.autarkia.client.render.SnappedCableRenderer::new);
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.SLING_SHOT.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.ROLLING_FLYWHEEL.get(),
            ctx -> new com.geckolib.renderer.GeoEntityRenderer<dev.autarkia.energy.RollingFlywheelEntity, net.minecraft.client.renderer.entity.state.EntityRenderState>(
                ctx, new com.geckolib.model.DefaultedEntityGeoModel<>(Autarkia.id("rolling_flywheel"))));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.TREADWHEEL.get(), ctx -> new GeoMachineRenderer<>(ctx, "treadwheel"));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.WINDMILL.get(), ctx -> new GeoMachineRenderer<>(ctx, "windmill", 3.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.STONE_FLYWHEEL.get(), ctx -> new GeoMachineRenderer<>(ctx, "stone_flywheel"));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.HAND_CRANK.get(), ctx -> new GeoMachineRenderer<>(ctx, "hand_crank"));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.QUERN.get(), ctx -> new GeoMachineRenderer<>(ctx, "quern"));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.STAMP_MILL.get(), ctx -> new GeoMachineRenderer<>(ctx, "stamp_mill"));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.CRATE.get(), CrateRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.TALLY_BOARD.get(), TallyBoardRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.CHUTE.get(), ChuteRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.COLONY_CORE.get(), ctx -> new GeoMachineRenderer<>(ctx, "colony_core", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.WORKER_ASSEMBLER.get(), dev.autarkia.client.render.AssemblerRenderer::new);
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.PORTER.get(), ctx -> new dev.autarkia.client.render.WorkerRenderer(ctx, "walker"));
    }

    private static void screens(RegisterMenuScreensEvent e) {
        e.register(AutarkiaMenus.STAMP_MILL.get(), SlotScreen::new);
        e.register(AutarkiaMenus.CRUCIBLE.get(), SlotScreen::new);
        e.register(AutarkiaMenus.CRATE.get(), SlotScreen::new);
        e.register(AutarkiaMenus.WORKER_ASSEMBLER.get(), SlotScreen::new);
        e.register(AutarkiaMenus.COLONY_CORE.get(), dev.autarkia.client.screen.ColonyScreen::new);
        e.register(AutarkiaMenus.PROJECT_BOARD.get(), dev.autarkia.client.screen.ColonyScreen::new);
        e.register(AutarkiaMenus.PLANNING_TABLET.get(), dev.autarkia.client.screen.ColonyScreen::new);
    }

    private static void tick(ClientTickEvent.Post e) {
        while (MODE_KEY.consumeClick()) {
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new dev.autarkia.network.EquipmentActionPayload(dev.autarkia.network.EquipmentActionPayload.MODE));
        }
        GhostBlocks.tick();
        ClientColony.tick();
        BeamVisuals.tick();
        ClientSpans.tick();
    }

    private static void geometry(SubmitCustomGeometryEvent e) {
        ClientSpans.submit(e);
        GhostBlocks.submit(e);
        BeamVisuals.submit(e);
    }
}

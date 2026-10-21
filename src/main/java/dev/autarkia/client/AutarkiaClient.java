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
    static final net.minecraft.client.KeyMapping SUIT_KEY = new net.minecraft.client.KeyMapping("key.autarkia.suit",
        net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME, com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD,
        com.mojang.blaze3d.platform.InputConstants.KEY_N, KEYS);
    static final net.minecraft.client.KeyMapping ENCHANT_KEY = new net.minecraft.client.KeyMapping("key.autarkia.enchant",
        net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME, com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD,
        com.mojang.blaze3d.platform.InputConstants.KEY_B, KEYS);

    public AutarkiaClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(AutarkiaClient::keys);
        modBus.addListener(AutarkiaClient::layers);
        container.registerConfig(ModConfig.Type.CLIENT, AutarkiaClientConfig.SPEC, "autarkia-client.toml");
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(AutarkiaClient::payloads);
        modBus.addListener(AutarkiaClient::renderers);
        modBus.addListener(AutarkiaClient::screens);
        modBus.addListener(dev.autarkia.client.render.SurveyOverlay::pipelines);
        ClientRecipes.register();
        dev.autarkia.client.ReformVisuals.register();
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) -> e.enqueueWork(dev.autarkia.client.AscensionPoses::register));
        NeoForge.EVENT_BUS.addListener(AutarkiaClient::tick);
        NeoForge.EVENT_BUS.addListener(ClientDirector::tick);
        NeoForge.EVENT_BUS.addListener(AutarkiaClient::geometry);
        NeoForge.EVENT_BUS.addListener(LoadTooltip::tooltip);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            ClientSpans.clear();
            ClientColony.clear();
            ClientNanites.clear();
            ClientSeeds.clear();
            dev.autarkia.client.hud.AegisHud.clear();
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
        e.register(dev.autarkia.network.NaniteBubblePayload.TYPE, (p, ctx) -> ClientNanites.bubble(p));
        e.register(dev.autarkia.network.SeedUnpackPayload.TYPE, (p, ctx) -> ClientSeeds.start(p));
        e.register(dev.autarkia.network.LightningPulsePayload.TYPE, (p, ctx) -> dev.autarkia.client.render.ClientLightning.handle(p));
        e.register(dev.autarkia.network.LedgerStatusPayload.TYPE, (p, ctx) -> dev.autarkia.client.screen.LedgerScreen.status(p));
        e.register(dev.autarkia.logistics.CapsuleDeltaPayload.TYPE, (p, ctx) -> dev.autarkia.client.render.ClientCapsules.handle(p));
        e.register(dev.autarkia.network.StockViewPayload.TYPE, (p, ctx) -> {
            dev.autarkia.client.screen.LedgerScreen.stock(p);
            dev.autarkia.client.screen.SitesScreen.stock(p);
            dev.autarkia.client.screen.ArchiveScreen.stock(p);
        });
        e.register(dev.autarkia.network.RackStatePayload.TYPE, (p, ctx) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.level.getBlockEntity(p.pos()) instanceof dev.autarkia.storage.RackBlockEntity rack) {
                rack.clientState(p.stage(), p.secondsLeft());
            }
        });
        e.register(dev.autarkia.network.MatterValuesPayload.TYPE, (p, ctx) -> dev.autarkia.storage.MatterValues.applyClient(p.json()));
        e.register(dev.autarkia.network.ReformFxPayload.TYPE, (p, ctx) -> dev.autarkia.client.ReformVisuals.handle(p));
        e.register(dev.autarkia.network.AscensionPosePayload.TYPE, (p, ctx) -> dev.autarkia.client.AscensionPoses.handle(p));
        e.register(dev.autarkia.network.DomainFlashPayload.TYPE, (p, ctx) -> dev.autarkia.client.render.DomainFlash.handle(p));
        e.register(dev.autarkia.network.AegisStatusPayload.TYPE, (p, ctx) -> dev.autarkia.client.hud.AegisHud.handle(p));
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
        e.register(SUIT_KEY);
        e.register(ENCHANT_KEY);
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
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.WOODEN_POLE.get(), ctx -> new GeoMachineRenderer<>(ctx, "wooden_pole"));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.WATERWHEEL.get(), ctx -> new GeoMachineRenderer<>(ctx, "waterwheel", 0.5));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.BOILER.get(), ctx -> new GeoMachineRenderer<>(ctx, "boiler"));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.STEAM_ENGINE.get(), ctx -> new GeoMachineRenderer<>(ctx, "steam_engine"));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.LIGHTNING_MAST.get(), ctx -> new GeoMachineRenderer<>(ctx, "lightning_mast", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.SLUICE.get(), ctx -> new GeoMachineRenderer<>(ctx, "sluice", 0.5));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.BLAST_SMELTER.get(), ctx -> new GeoMachineRenderer<>(ctx, "blast_smelter", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.BELT.get(), dev.autarkia.client.render.BeltRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.BELT_SPLITTER.get(), dev.autarkia.client.render.FilterRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.SORTING_CHUTE.get(), dev.autarkia.client.render.FilterRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.WOOD_RACK.get(), dev.autarkia.client.render.RackRenderer::new);
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.HAULER_CRAB.get(), ctx -> new dev.autarkia.client.render.WorkerRenderer(ctx, "crab"));
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.BUILDER_DRONE.get(), ctx -> new dev.autarkia.client.render.WorkerRenderer(ctx, "drone"));
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.LOADER.get(),
            ctx -> new dev.autarkia.client.render.WorkerRenderer(ctx, "crab", dev.autarkia.worker.WorkerType.LOADER.scale()));
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.LINEMAN.get(), ctx -> new dev.autarkia.client.render.WorkerRenderer(ctx, "walker"));
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.SURVEYOR.get(),
            ctx -> new dev.autarkia.client.render.WorkerRenderer(ctx, "drone", dev.autarkia.worker.WorkerType.SURVEYOR.scale()));
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.CABLE_LAYER_DRONE.get(),
            ctx -> new dev.autarkia.client.render.WorkerRenderer(ctx, "drone", dev.autarkia.worker.WorkerType.CABLE_LAYER.scale()));
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.REPLICATOR_WALKER.get(),
            ctx -> new dev.autarkia.client.render.WorkerRenderer(ctx, "walker", dev.autarkia.worker.WorkerType.REPLICATOR.scale()));
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.RISING_BLOCK.get(), dev.autarkia.client.render.RisingBlockRenderer::new);
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.SENTINEL.get(),
            ctx -> new dev.autarkia.client.render.WorkerRenderer(ctx, "drone", dev.autarkia.worker.WorkerType.SENTINEL.scale()));
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.FALLING_CRATE.get(), dev.autarkia.client.render.FallingCrateRenderer::new);
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.GRAPPLE_HOOK.get(), dev.autarkia.client.render.GrappleHookRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.STEEL_PYLON.get(), ctx -> new GeoMachineRenderer<>(ctx, "steel_pylon", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.STEAM_TURBINE.get(), ctx -> new GeoMachineRenderer<>(ctx, "steam_turbine", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.JAW_CRUSHER.get(), ctx -> new GeoMachineRenderer<>(ctx, "jaw_crusher", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.STEEL_RACK.get(), dev.autarkia.client.render.RackRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.FABRICATOR.get(), ctx -> new GeoMachineRenderer<>(ctx, "fabricator", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.CAPSULE_TUBE.get(), dev.autarkia.client.render.CapsuleTubeRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.CAPSULE_STATION.get(), dev.autarkia.client.render.CapsuleStationRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.CABLE_CAR_STATION.get(), dev.autarkia.client.render.CableCarStationRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.CRATE_LAUNCHER.get(), dev.autarkia.client.render.CrateLauncherRenderer::new);
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.GONDOLA.get(), dev.autarkia.client.render.GondolaRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.FROTH_FLOTATION_PLANT.get(), ctx -> new GeoMachineRenderer<>(ctx, "froth_flotation_plant", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.OUTPOST_CORE.get(), ctx -> new GeoMachineRenderer<>(ctx, "outpost_core", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.REPLICATOR_FOUNDRY.get(), ctx -> new GeoMachineRenderer<>(ctx, "replicator_foundry", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.RESTORATION_PROJECTOR.get(), ctx -> new GeoMachineRenderer<>(ctx, "restoration_projector", 0.5));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.HV_PYLON.get(), ctx -> new GeoMachineRenderer<>(ctx, "hv_pylon", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.HYDRO_TURBINE.get(), ctx -> new GeoMachineRenderer<>(ctx, "hydro_turbine", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.WIND_TOWER.get(), ctx -> new GeoMachineRenderer<>(ctx, "wind_tower", 5.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.MAGMA_TAP.get(), ctx -> new GeoMachineRenderer<>(ctx, "magma_tap", 0.5));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.PLASMA_SEPARATOR.get(), ctx -> new GeoMachineRenderer<>(ctx, "plasma_separator", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.CRYO_NODE.get(), ctx -> new GeoMachineRenderer<>(ctx, "cryo_node", 0.5));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.SUPERCONDUCTING_COIL.get(), ctx -> new GeoMachineRenderer<>(ctx, "superconducting_coil", 0.5));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.PLASMA_GENERATOR.get(), ctx -> new GeoMachineRenderer<>(ctx, "plasma_generator", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.MATTER_BUFFER.get(), ctx -> new GeoMachineRenderer<>(ctx, "matter_buffer", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.LATTICE_RACK.get(), dev.autarkia.client.render.RackRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.NANITE_HIVE.get(), ctx -> new GeoMachineRenderer<>(ctx, "nanite_hive", 0.5));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.FORGE_OF_ASCENSION.get(), ctx -> new GeoMachineRenderer<>(ctx, "forge_of_ascension", 0.5));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.SEED_MATRIX.get(), ctx -> new GeoMachineRenderer<>(ctx, "seed_matrix", 1.0));
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.THROWN_AUTARKY_SEED.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.MATTER_COMPILER.get(), ctx -> new GeoMachineRenderer<>(ctx, "matter_compiler", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.SINGULARITY_CORE.get(), ctx -> new GeoMachineRenderer<>(ctx, "singularity_core", 1.0));
        e.registerBlockEntityRenderer(AutarkiaBlockEntities.GYRO_HEART.get(), ctx -> new GeoMachineRenderer<>(ctx, "gyro_heart", 1.0));
        e.registerEntityRenderer(dev.autarkia.registry.AutarkiaEntities.NANITE_SWARM.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    private static void screens(RegisterMenuScreensEvent e) {
        e.register(AutarkiaMenus.STAMP_MILL.get(), SlotScreen::new);
        e.register(AutarkiaMenus.CRUCIBLE.get(), SlotScreen::new);
        e.register(AutarkiaMenus.CRATE.get(), SlotScreen::new);
        e.register(AutarkiaMenus.WORKER_ASSEMBLER.get(), SlotScreen::new);
        e.register(AutarkiaMenus.REPLICATOR_FOUNDRY.get(), dev.autarkia.client.screen.FoundryScreen::new);
        e.register(AutarkiaMenus.RESTORATION_PROJECTOR.get(), dev.autarkia.client.screen.RestorationProjectorScreen::new);
        e.register(AutarkiaMenus.COLONY_CORE.get(), dev.autarkia.client.screen.ColonyScreen::new);
        e.register(AutarkiaMenus.PROJECT_BOARD.get(), dev.autarkia.client.screen.ColonyScreen::new);
        e.register(AutarkiaMenus.PLANNING_TABLET.get(), dev.autarkia.client.screen.ColonyScreen::new);
        e.register(AutarkiaMenus.BOILER.get(), dev.autarkia.client.screen.BoilerScreen::new);
        e.register(AutarkiaMenus.ENERGY_STORAGE.get(), dev.autarkia.client.screen.EnergyStorageScreen::new);
        e.register(AutarkiaMenus.SLUICE.get(), dev.autarkia.client.screen.SluiceScreen::new);
        e.register(AutarkiaMenus.BLAST_SMELTER.get(), dev.autarkia.client.screen.BlastSmelterScreen::new);
        e.register(AutarkiaMenus.PLATE_PRESS.get(), SlotScreen::new);
        e.register(AutarkiaMenus.WIRE_DRAWER.get(), SlotScreen::new);
        e.register(AutarkiaMenus.BELT_SPLITTER.get(), dev.autarkia.client.screen.FilterScreen::new);
        e.register(AutarkiaMenus.SORTING_CHUTE.get(), dev.autarkia.client.screen.FilterScreen::new);
        e.register(AutarkiaMenus.RACK.get(), dev.autarkia.client.screen.RackScreen::new);
        e.register(AutarkiaMenus.PATTERN_LOOM.get(), dev.autarkia.client.screen.PatternLoomScreen::new);
        e.register(AutarkiaMenus.FUSE_BOX.get(), dev.autarkia.client.screen.FuseBoxScreen::new);
        e.register(AutarkiaMenus.STEAM_TURBINE.get(), dev.autarkia.client.screen.SteamTurbineScreen::new);
        e.register(AutarkiaMenus.JAW_CRUSHER.get(), SlotScreen::new);
        e.register(AutarkiaMenus.MAGNETIC_SEPARATOR.get(), SlotScreen::new);
        e.register(AutarkiaMenus.ELECTROLYZER.get(), SlotScreen::new);
        e.register(AutarkiaMenus.FABRICATOR.get(), dev.autarkia.client.screen.FabricatorScreen::new);
        e.register(AutarkiaMenus.WAREHOUSE_CONTROLLER.get(), dev.autarkia.client.screen.WarehouseScreen::new);
        e.register(AutarkiaMenus.LEDGER_TERMINAL.get(), dev.autarkia.client.screen.LedgerScreen::new);
        e.register(AutarkiaMenus.CAPSULE_STATION.get(), dev.autarkia.client.screen.CapsuleStationScreen::new);
        e.register(AutarkiaMenus.CABLE_CAR_STATION.get(), dev.autarkia.client.screen.CableCarStationScreen::new);
        e.register(AutarkiaMenus.CRATE_LAUNCHER.get(), dev.autarkia.client.screen.CrateLauncherScreen::new);
        e.register(AutarkiaMenus.DEPOT.get(), dev.autarkia.client.screen.DepotScreen::new);
        e.register(AutarkiaMenus.FROTH_FLOTATION_PLANT.get(), dev.autarkia.client.screen.FlotationScreen::new);
        e.register(AutarkiaMenus.OUTPOST_CORE.get(), dev.autarkia.client.screen.OutpostScreen::new);
        e.register(AutarkiaMenus.PLASMA_SEPARATOR.get(), SlotScreen::new);
        e.register(AutarkiaMenus.CRYO_NODE.get(), dev.autarkia.client.screen.CryoNodeScreen::new);
        e.register(AutarkiaMenus.SUPERCONDUCTING_COIL.get(), dev.autarkia.client.screen.SuperconductingCoilScreen::new);
        e.register(AutarkiaMenus.PLASMA_GENERATOR.get(), dev.autarkia.client.screen.PlasmaGeneratorScreen::new);
        e.register(AutarkiaMenus.MATTER_BUFFER.get(), dev.autarkia.client.screen.MatterBufferScreen::new);
        e.register(AutarkiaMenus.MAG_RAIL_STATION.get(), dev.autarkia.client.screen.MagRailStationScreen::new);
        e.register(AutarkiaMenus.NANITE_HIVE.get(), dev.autarkia.client.screen.NaniteHiveScreen::new);
        e.register(AutarkiaMenus.FORGE_OF_ASCENSION.get(), dev.autarkia.client.screen.ForgeScreen::new);
        e.register(AutarkiaMenus.SEED_MATRIX.get(), dev.autarkia.client.screen.SeedMatrixScreen::new);
        e.register(AutarkiaMenus.MATTER_COMPILER.get(), dev.autarkia.client.screen.CompilerScreen::new);
        e.register(AutarkiaMenus.MATTER_CONDUIT.get(), dev.autarkia.client.screen.ConduitScreen::new);
        e.register(AutarkiaMenus.DOMAIN_ARCHIVE.get(), dev.autarkia.client.screen.ArchiveScreen::new);
        e.register(AutarkiaMenus.SINGULARITY_CORE.get(), dev.autarkia.client.screen.SingularityCoreScreen::new);
    }

    private static void tick(ClientTickEvent.Post e) {
        dev.autarkia.client.render.SurveyOverlay.tick();
        while (MODE_KEY.consumeClick()) {
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new dev.autarkia.network.EquipmentActionPayload(dev.autarkia.network.EquipmentActionPayload.MODE));
        }
        while (SUIT_KEY.consumeClick()) {
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new dev.autarkia.network.EquipmentActionPayload(dev.autarkia.network.EquipmentActionPayload.SUIT));
        }
        while (ENCHANT_KEY.consumeClick()) {
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                new dev.autarkia.network.EquipmentActionPayload(dev.autarkia.network.EquipmentActionPayload.ENCHANT));
        }
        GhostBlocks.tick();
        ClientColony.tick();
        BeamVisuals.tick();
        ClientSpans.tick();
        dev.autarkia.client.render.ClientLightning.tick();
        ClientNanites.tick();
        ClientSeeds.tick();
        dev.autarkia.client.render.DomainFlash.tick();
    }

    private static void geometry(SubmitCustomGeometryEvent e) {
        ClientSpans.submit(e);
        GhostBlocks.submit(e);
        BeamVisuals.submit(e);
        dev.autarkia.client.render.SurveyOverlay.submit(e);
    }
}

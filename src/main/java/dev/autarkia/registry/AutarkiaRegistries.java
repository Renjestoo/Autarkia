package dev.autarkia.registry;

import dev.autarkia.command.AutarkiaCommand;
import dev.autarkia.energy.Grid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Wires every deferred register and every game event listener of Autarkia. */
public final class AutarkiaRegistries {
    private AutarkiaRegistries() {
    }

    /** Register all deferred registers on the mod bus. */
    public static void register(IEventBus modBus) {
        AutarkiaSounds.SOUNDS.register(modBus);
        AutarkiaBlocks.BLOCKS.register(modBus);
        AutarkiaItems.ITEMS.register(modBus);
        AutarkiaBlockEntities.register(modBus);
        AutarkiaEntities.ENTITIES.register(modBus);
        AutarkiaMenus.MENUS.register(modBus);
        AutarkiaRecipes.register(modBus);
        dev.autarkia.processing.OutcropOreFeature.TYPES.register(modBus);
        AutarkiaComponents.COMPONENTS.register(modBus);
        AutarkiaTabs.register(modBus);
        EventTrigger.TRIGGERS.register(modBus);
        dev.autarkia.worker.WorkerMemories.register(modBus);
        dev.autarkia.colony.Outposts.register(modBus);
        dev.autarkia.worker.WorkerTravel.register(modBus);
        modBus.addListener(AutarkiaEntities::attributes);
    }

    /** Register game-bus listeners. */
    public static void registerGameListeners() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> AutarkiaCommand.register(e.getDispatcher(), e.getBuildContext()));
        Grid.register();
        dev.autarkia.energy.Lightning.register();
        dev.autarkia.colony.Colonies.register();
        dev.autarkia.colony.FrontierSignals.register();
        dev.autarkia.colony.PatternRepair.register();
        dev.autarkia.colony.ColonyPlans.register();
        dev.autarkia.colony.ColonySync.register();
        dev.autarkia.command.ColonyProfiler.register();
        dev.autarkia.command.SelfTest.register();
        dev.autarkia.command.Showcase.register();
        dev.autarkia.command.ShowcaseM4.register();
        dev.autarkia.command.ShowcaseM5.register();
        dev.autarkia.command.ShowcaseM6.register();
        dev.autarkia.command.ShowcaseM7.register();
        dev.autarkia.equipment.AreaMining.register();
        dev.autarkia.equipment.TreeFelling.register();
        dev.autarkia.equipment.SuitItem.register();
        dev.autarkia.equipment.VeinMining.register();
        dev.autarkia.equipment.GrappleLauncherItem.register();
        dev.autarkia.equipment.DashBladeItem.register();
        dev.autarkia.equipment.FrontierSuitItem.register();
    }
}

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
        AutarkiaComponents.COMPONENTS.register(modBus);
        AutarkiaTabs.register(modBus);
        EventTrigger.TRIGGERS.register(modBus);
        dev.autarkia.worker.WorkerMemories.register(modBus);
        modBus.addListener(AutarkiaEntities::attributes);
    }

    /** Register game-bus listeners. */
    public static void registerGameListeners() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> AutarkiaCommand.register(e.getDispatcher(), e.getBuildContext()));
        Grid.register();
        dev.autarkia.colony.Colonies.register();
        dev.autarkia.colony.ColonyPlans.register();
        dev.autarkia.colony.ColonySync.register();
        dev.autarkia.command.ColonyProfiler.register();
        dev.autarkia.command.SelfTest.register();
        dev.autarkia.command.Showcase.register();
    }
}

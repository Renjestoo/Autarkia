package dev.autarkia.registry;

import dev.autarkia.command.AutarkiaCommand;
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
        AutarkiaTabs.register(modBus);
    }

    /** Register mod-bus listeners (capabilities, attributes). */
    public static void registerModListeners(IEventBus modBus) {
    }

    /** Register game-bus listeners. */
    public static void registerGameListeners() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> AutarkiaCommand.register(e.getDispatcher(), e.getBuildContext()));
    }
}

package dev.autarkia;

import com.mojang.logging.LogUtils;
import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.registry.AutarkiaRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/** Common entry point of Autarkia: registers content, configuration, payloads and game event listeners. */
@Mod(Autarkia.MOD_ID)
public final class Autarkia {
    public static final String MOD_ID = "autarkia";
    public static final Logger LOG = LogUtils.getLogger();

    public Autarkia(IEventBus modBus, ModContainer container) {
        AutarkiaRegistries.register(modBus);
        container.registerConfig(ModConfig.Type.SYNCED, AutarkiaConfig.SPEC, "autarkia-server.toml");
        modBus.addListener(AutarkiaNetwork::register);
        AutarkiaRegistries.registerModListeners(modBus);
        AutarkiaRegistries.registerGameListeners();
    }

    /** Return an identifier in the autarkia namespace. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

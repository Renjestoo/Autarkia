package dev.autarkia.client;

import dev.autarkia.Autarkia;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Scripted camera director for repeatable test scenes: started by the system property autarkia.director (a script
 * path). Lines: "wait N", "cmd COMMAND" (runs on the integrated server with full permission, as the server),
 * "pcmd COMMAND" (runs as the local player), "shot NAME" (saves screenshots/NAME.png), "hud on|off", "log TEXT",
 * "use X Y Z [FACE]" (the player uses the held item on that block, which opens menus), "close" (closes the screen),
 * "quit". The script starts 100 ticks after the player joins a world and logs AUTARKIA_DIRECTOR_DONE at the end.
 */
public final class ClientDirector {
    private static final Deque<String> LINES = new ArrayDeque<>();
    private static boolean loaded;
    private static int waitTicks = 100;
    private static int idleTicks;

    private ClientDirector() {
    }

    private static void read(Path file) throws IOException {
        for (String l : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (l.startsWith("include ")) {
                read(file.resolveSibling(l.substring(8).strip()));
            } else if (!l.isBlank() && !l.startsWith("#")) {
                LINES.add(l.strip());
            }
        }
    }

    private static void proceedPastWarnings(net.neoforged.neoforge.client.gui.LoadingErrorScreen s) {
        try {
            var errorsField = s.getClass().getDeclaredField("modLoadErrors");
            var taskField = s.getClass().getDeclaredField("nextScreenTask");
            errorsField.setAccessible(true);
            taskField.setAccessible(true);
            if (!((java.util.List<?>) errorsField.get(s)).isEmpty()) {
                throw new IllegalStateException("Mod loading errors; the scripted scene cannot continue (see latest.log)");
            }
            Autarkia.LOG.info("[director] mod loading warnings only (other mods); proceeding");
            ((Runnable) taskField.get(s)).run();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot read LoadingErrorScreen fields", ex);
        }
    }

    /** Advance the script; called every client tick. */
    public static void tick(ClientTickEvent.Post e) {
        String path = System.getProperty("autarkia.director");
        Minecraft mc = Minecraft.getInstance();
        if (path == null) {
            return;
        }
        if (mc.player == null || mc.level == null) {
            if (mc.gui.screen() instanceof net.neoforged.neoforge.client.gui.LoadingErrorScreen s) {
                proceedPastWarnings(s);
                return;
            }
            if (mc.gui.screen() != null && mc.gui.screen().getClass().getName().equals("org.betterx.bclib.client.gui.screens.ConfirmFixScreen")) {
                try {
                    var resolve = mc.gui.screen().getClass().getDeclaredMethod("resolve", boolean.class, boolean.class);
                    resolve.setAccessible(true);
                    resolve.invoke(mc.gui.screen(), false, true);
                    Autarkia.LOG.info("[director] BCLib world fix prompt answered: apply fixes, no backup");
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException("Cannot answer the BCLib fix prompt", ex);
                }
                return;
            }
            if (++idleTicks % 200 == 0) {
                Autarkia.LOG.info("[director] waiting for a world: screen {}, overlay {}",
                    mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getName(),
                    mc.gui.overlay() == null ? "none" : mc.gui.overlay().getClass().getName());
            }
            return;
        }
        if (!loaded) {
            loaded = true;
            try {
                read(Path.of(path));
                Autarkia.LOG.info("[director] loaded {} lines from {}", LINES.size(), path);
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot read director script " + path, ex);
            }
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        while (!LINES.isEmpty() && waitTicks == 0) {
            String line = LINES.poll();
            String cmd = line.contains(" ") ? line.substring(0, line.indexOf(' ')) : line;
            String arg = line.contains(" ") ? line.substring(line.indexOf(' ') + 1) : "";
            switch (cmd) {
                case "wait" -> waitTicks = Integer.parseInt(arg);
                case "cmd" -> {
                    MinecraftServer server = mc.getSingleplayerServer();
                    if (server == null) {
                        mc.player.connection.sendCommand(arg);
                    } else {
                        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), arg));
                    }
                    Autarkia.LOG.info("[director] cmd {}", arg);
                }
                case "pcmd" -> mc.player.connection.sendCommand(arg);
                case "shot" -> {
                    Screenshot.grab(mc.gameDirectory, arg + ".png", mc.gameRenderer.mainRenderTarget(), 1, msg -> Autarkia.LOG.info("[director] shot {}", arg));
                    waitTicks = 5;
                }
                case "hud" -> {
                    if (mc.gui.hud.isHidden() != arg.equals("off")) {
                        mc.gui.hud.toggle();
                    }
                }
                case "use" -> {
                    String[] p = arg.split(" ");
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                    net.minecraft.core.Direction face = net.minecraft.core.Direction.byName(p.length > 3 ? p[3] : "up");
                    mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), face, pos, false));
                    Autarkia.LOG.info("[director] use {}", arg);
                }
                case "close" -> {
                    if (mc.player.containerMenu != mc.player.inventoryMenu) {
                        mc.player.closeContainer();
                    }
                    mc.gui.setScreen(null);
                }
                case "log" -> Autarkia.LOG.info("[director] {}", arg);
                case "quit" -> {
                    Autarkia.LOG.info("[director] AUTARKIA_DIRECTOR_DONE");
                    mc.stop();
                }
                default -> throw new IllegalStateException("Unknown director command: " + line);
            }
        }
    }
}

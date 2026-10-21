package dev.autarkia.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.autarkia.Autarkia;
import dev.autarkia.colony.NaniteClaims;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /autarkia nanite status|stop|resume} (05b section 11.1): show every claim, stop all nanite edits at once (the
 * cursors stay), continue them. The stop holds until resume or a server restart; {@code nanite.enabled} is the saved
 * kill switch.
 */
public final class NaniteCommand {
    private NaniteCommand() {
    }

    /** Add the nanite subcommands to the root. */
    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("nanite")
            .then(Commands.literal("status").executes(c -> {
                for (String line : NaniteClaims.status()) {
                    reply(c.getSource(), line);
                }
                return 1;
            }))
            .then(Commands.literal("stop").executes(c -> {
                NaniteClaims.stop();
                return reply(c.getSource(), "all claims stopped");
            }))
            .then(Commands.literal("resume").executes(c -> {
                NaniteClaims.resume();
                return reply(c.getSource(), "claims resumed");
            })));
    }

    private static int reply(CommandSourceStack src, String text) {
        src.sendSuccess(() -> Component.literal("[nanite] " + text), false);
        Autarkia.LOG.info("[nanite] {}", text);
        return 1;
    }
}

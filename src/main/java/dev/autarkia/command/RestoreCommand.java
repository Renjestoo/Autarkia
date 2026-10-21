package dev.autarkia.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.autarkia.Autarkia;
import dev.autarkia.colony.Restoration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** {@code /autarkia restore status} (05b section 11.1): the missing counts and work state of every Restoration Field. */
public final class RestoreCommand {
    private RestoreCommand() {
    }

    /** Add the restore subcommand to the root. */
    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("restore").then(Commands.literal("status").executes(c -> {
            int n = 0;
            for (String line : Restoration.status()) {
                c.getSource().sendSuccess(() -> Component.literal("[restore] " + line), false);
                Autarkia.LOG.info("[restore] {}", line);
                n++;
            }
            return n;
        })));
    }
}

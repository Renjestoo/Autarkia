package dev.autarkia.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** The /autarkia command root (permission level 2): colony, worker, nanite, restoration and pattern administration and the profiler. */
public final class AutarkiaCommand {
    private AutarkiaCommand() {
    }

    /** Register the command tree. */
    public static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("autarkia").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        ColonyCommand.register(root);
        WorkerCommand.register(root);
        NaniteCommand.register(root);
        RestoreCommand.register(root);
        root.then(Commands.literal("profile").executes(c -> SystemProfiler.report(c.getSource())));
        PatternCommand.register(root);
        d.register(root);
    }
}

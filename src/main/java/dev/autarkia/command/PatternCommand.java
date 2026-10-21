package dev.autarkia.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.Pattern;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;

/**
 * {@code /autarkia pattern list|gc} (05b section 11): list the Pattern library and instances of every colony; delete the
 * Pattern files that no colony library or instance references.
 */
final class PatternCommand {
    private PatternCommand() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("pattern")
            .then(Commands.literal("list").executes(c -> list(c.getSource())))
            .then(Commands.literal("gc").executes(c -> gc(c.getSource()))));
    }

    private static int list(CommandSourceStack src) {
        int n = 0;
        for (Colony c : Colonies.registry().all()) {
            for (Colony.LibraryEntry e : c.library) {
                Pattern p = Pattern.load(src.getServer(), e.pattern());
                String line = c.name + ": " + (p == null ? "Missing pattern " + e.pattern() : p.name() + " " + p.size().getX() + "x" + p.size().getY() + "x"
                    + p.size().getZ() + ", " + p.blocks() + " blocks") + ", role " + e.role() + (e.enabled() ? "" : " (disabled)");
                src.sendSuccess(() -> Component.literal(line), false);
                n++;
            }
            for (Colony.Instance in : c.instances) {
                String line = c.name + ": instance " + in.pattern() + " at " + in.origin().toShortString() + (in.watched() ? ", watched" : "");
                src.sendSuccess(() -> Component.literal(line), false);
            }
        }
        int total = n;
        src.sendSuccess(() -> Component.literal(total + " library entries"), false);
        return total;
    }

    private static int gc(CommandSourceStack src) {
        Set<UUID> used = new HashSet<>();
        for (Colony c : Colonies.registry().all()) {
            c.library.forEach(e -> used.add(e.pattern()));
            c.instances.forEach(i -> used.add(i.pattern()));
        }
        Path dir = src.getServer().getWorldPath(LevelResource.DATA).resolve("autarkia");
        int deleted = 0;
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                for (Path f : files.filter(p -> p.getFileName().toString().matches("pattern_[0-9a-f]{32}\\.dat")).toList()) {
                    String hex = f.getFileName().toString().substring(8, 40);
                    UUID id = UUID.fromString(hex.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5"));
                    if (!used.contains(id) && Files.deleteIfExists(f)) {
                        deleted++;
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot clean Pattern files in " + dir, e);
            }
        }
        int d = deleted;
        src.sendSuccess(() -> Component.literal("Deleted " + d + " unused Pattern files"), true);
        return d;
    }
}

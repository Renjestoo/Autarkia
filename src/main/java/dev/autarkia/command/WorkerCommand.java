package dev.autarkia.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.WorkerDebugPayload;
import dev.autarkia.worker.WorkerEntity;
import dev.autarkia.worker.WorkerMemories;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

/** {@code /autarkia worker info|energy|perf} (06 section 8 and 08b section 15), acting on the nearest worker within 16 blocks. */
public final class WorkerCommand {
    private static final SimpleCommandExceptionType NONE = new SimpleCommandExceptionType(Component.translatable("command.autarkia.no_worker"));

    private WorkerCommand() {
    }

    /** Add the worker subcommands to the root. */
    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("worker")
            .then(Commands.literal("info").executes(c -> info(c.getSource())))
            .then(Commands.literal("energy").then(Commands.argument("tension", IntegerArgumentType.integer(0)).executes(c -> {
                WorkerEntity w = nearest(c.getSource());
                w.setEnergy(IntegerArgumentType.getInteger(c, "tension"));
                c.getSource().sendSuccess(() -> Component.literal("[worker] tension " + w.energy() + " T, reserve " + w.reserve() + " T"), false);
                return 1;
            })))
            .then(Commands.literal("perf").executes(c -> {
                ColonyProfiler.start(c.getSource());
                c.getSource().sendSuccess(() -> Component.literal("[worker] measuring worker server time for 200 ticks"), false);
                return 1;
            })));
    }

    private static WorkerEntity nearest(CommandSourceStack src) throws CommandSyntaxException {
        return src.getLevel().getEntitiesOfClass(WorkerEntity.class, new AABB(src.getPosition(), src.getPosition()).inflate(16)).stream()
            .min(Comparator.comparingDouble(w -> w.position().distanceToSqr(src.getPosition()))).orElseThrow(NONE::create);
    }

    private static int info(CommandSourceStack src) throws CommandSyntaxException {
        WorkerEntity w = nearest(src);
        List<String> lines = new ArrayList<>();
        lines.add(w.workerType.id().getPath() + " " + w.getUUID().toString().substring(0, 8) + (w.colony() == null ? " unlinked" : " colony " + w.colony().name));
        lines.add("state " + w.stateName() + " action " + w.action());
        lines.add("tension " + w.energy() + "/" + w.workerType.capacity() + " T, reserve " + w.reserve());
        ColonyJob j = w.currentJob();
        lines.add(j == null ? "job none" : "job " + j.type.name().toLowerCase(java.util.Locale.ROOT) + " at " + j.target.toShortString());
        lines.add("phase " + w.getBrain().getMemory(WorkerMemories.JOB_PHASE.get()).map(p -> p.getSerializedName()).orElse("-")
            + " stuck " + w.getBrain().getMemory(WorkerMemories.STUCK_LEVEL.get()).orElse(0));
        lines.add("home " + w.home().toShortString() + " cargo " + w.cargo);
        if (src.getPlayer() != null) {
            AutarkiaNetwork.toPlayer(src.getPlayer(), new WorkerDebugPayload(lines));
        }
        for (String l : lines) {
            src.sendSuccess(() -> Component.literal("[worker] " + l), false);
        }
        return 1;
    }
}

package dev.autarkia.command;

import com.klikli_dev.magicparticleslib.premade.particle.glow.GlowParticleOptions;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.ColonyClock;
import dev.autarkia.colony.ColonyJob;
import dev.autarkia.colony.ColonyPlans;
import dev.autarkia.colony.ColonyRuntime;
import dev.autarkia.colony.ColonySensors;
import dev.autarkia.colony.ColonyTemplate;
import dev.autarkia.colony.ColonyTiers;
import dev.autarkia.colony.OutpostCoreBlockEntity;
import dev.autarkia.colony.Outposts;
import dev.autarkia.colony.Planner;
import dev.autarkia.colony.Project;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * The colony debug commands of 05b section 11.1 (permission level 2): colony, domain, sensor, planner, project,
 * clock, job, memory and profile. The target colony is the one whose domain holds the command position.
 */
public final class ColonyCommand {
    private static final SimpleCommandExceptionType NO_COLONY = new SimpleCommandExceptionType(Component.translatable("command.autarkia.no_colony"));

    private ColonyCommand() {
    }

    /** Add the colony subcommands to the root. */
    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("colony")
            .then(Commands.literal("info").executes(c -> info(c.getSource())))
            .then(Commands.literal("settier").then(Commands.argument("tier", IntegerArgumentType.integer(1, 6)).executes(c -> {
                Colony col = colony(c.getSource());
                col.tier = IntegerArgumentType.getInteger(c, "tier");
                Colonies.rebuildIndex();
                Colonies.changed();
                return reply(c.getSource(), "colony " + col.name + " tier " + col.tier);
            })))
            .then(Commands.literal("autonomy").then(Commands.argument("level", StringArgumentType.word()).suggests((c, b) -> {
                for (Colony.Autonomy a : Colony.Autonomy.values()) {
                    b.suggest(a.getSerializedName());
                }
                return b.buildFuture();
            }).executes(c -> {
                Colony col = colony(c.getSource());
                col.autonomy = Colony.Autonomy.valueOf(StringArgumentType.getString(c, "level").toUpperCase(java.util.Locale.ROOT));
                Colonies.changed();
                return reply(c.getSource(), "autonomy " + col.autonomy.getSerializedName());
            }))));
        root.then(Commands.literal("domain").then(Commands.literal("show").executes(c -> domainShow(c.getSource()))));
        root.then(Commands.literal("sensor")
            .then(Commands.literal("dump").executes(c -> sensorDump(c.getSource())))
            .then(Commands.literal("force").then(Commands.argument("signal", StringArgumentType.string())
                .then(Commands.argument("value", DoubleArgumentType.doubleArg()).then(Commands.argument("ticks", IntegerArgumentType.integer(1)).executes(c -> {
                    Colony col = colony(c.getSource());
                    String sig = StringArgumentType.getString(c, "signal");
                    ColonyRuntime.of(col).forced.put(sig, new ColonyRuntime.Forced(DoubleArgumentType.getDouble(c, "value"),
                        c.getSource().getLevel().getGameTime() + IntegerArgumentType.getInteger(c, "ticks")));
                    return reply(c.getSource(), "forced " + sig);
                }))))));
        root.then(Commands.literal("planner")
            .then(Commands.literal("run").executes(c -> {
                Colony col = colony(c.getSource());
                Planner.pass(col, c.getSource().getLevel());
                return reply(c.getSource(), "planner pass done; search " + (ColonyRuntime.of(col).search == null ? "idle" : "running"));
            }))
            .then(Commands.literal("explain").then(Commands.argument("template", StringArgumentType.word()).executes(c -> explain(c.getSource(),
                StringArgumentType.getString(c, "template"))))));
        root.then(Commands.literal("project")
            .then(Commands.literal("list").executes(c -> projectList(c.getSource())))
            .then(projectAction("approve")).then(projectAction("deny")).then(projectAction("cancel")));
        root.then(Commands.literal("clock").then(Commands.literal("skip").then(Commands.argument("ticks", IntegerArgumentType.integer(1)).executes(c -> {
            Colony col = colony(c.getSource());
            ColonyClock.skip(col, IntegerArgumentType.getInteger(c, "ticks"));
            return reply(c.getSource(), "colony clock " + col.clock + " day " + ColonyClock.day(col) + " budget " + Math.round(col.budgetRemaining));
        }))));
        root.then(Commands.literal("job").then(Commands.literal("list").executes(c -> jobList(c.getSource()))));
        root.then(Commands.literal("memory")
            .then(Commands.literal("list").executes(c -> memoryList(c.getSource())))
            .then(Commands.literal("add").then(Commands.argument("type", StringArgumentType.word()).then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(c -> memoryAdd(c.getSource(), StringArgumentType.getString(c, "type"), BlockPosArgument.getBlockPos(c, "pos"))))))
            .then(Commands.literal("clear").executes(c -> {
                Colony col = colony(c.getSource());
                col.memories.clear();
                Colonies.changed();
                return reply(c.getSource(), "memory cleared");
            })));
        root.then(Commands.literal("profile").executes(c -> {
            ColonyProfiler.start(c.getSource());
            return reply(c.getSource(), "profiling 200 ticks");
        }));
        root.then(Commands.literal("outpost")
            .then(Commands.literal("list").executes(c -> outpostList(c.getSource())))
            .then(Commands.literal("credit").then(Commands.argument("id", StringArgumentType.word()).then(Commands.argument("ticks", IntegerArgumentType.integer(0))
                .executes(c -> outpostCredit(c.getSource(), StringArgumentType.getString(c, "id"), IntegerArgumentType.getInteger(c, "ticks")))))));
    }

    private static int outpostList(CommandSourceStack src) {
        int n = 0;
        for (Colony c : Colonies.registry().all()) {
            for (String line : Outposts.list(c)) {
                reply(src, c.name + ": " + line);
                n++;
            }
        }
        return n == 0 ? reply(src, "no outposts") : n;
    }

    /** Force a catch-up credit of the given ticks on an outpost of any colony (05b section 11.1; works from far away). */
    private static int outpostCredit(CommandSourceStack src, String id, int ticks) throws CommandSyntaxException {
        for (Colony c : Colonies.registry().all()) {
            Colony.Outpost o = Outposts.find(c, id);
            ServerLevel level = Colonies.level(c);
            if (o != null && level != null && level.getBlockEntity(o.corePos) instanceof OutpostCoreBlockEntity core) {
                int added = Outposts.sample(level, c, o, core, ticks);
                return reply(src, "outpost " + id + " credited " + ticks + " ticks: +" + added + " items");
            }
        }
        throw new SimpleCommandExceptionType(Component.literal("No loaded outpost core for " + id)).create();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> projectAction(String action) {
        return Commands.literal(action).then(Commands.argument("id", StringArgumentType.word()).executes(c -> {
            Colony col = colony(c.getSource());
            Project p = col.projectByPrefix(StringArgumentType.getString(c, "id"));
            if (p == null) {
                throw new SimpleCommandExceptionType(Component.translatable("command.autarkia.no_project")).create();
            }
            switch (action) {
                case "approve" -> Planner.approve(col, p, false);
                case "deny" -> Planner.deny(col, p);
                default -> Planner.cancel(col, p);
            }
            return reply(c.getSource(), action + " " + p.shortId() + " -> " + p.state.getSerializedName());
        }));
    }

    /** Return the colony at the command position. */
    static Colony colony(CommandSourceStack src) throws CommandSyntaxException {
        Colony c = Colonies.at(src.getLevel(), BlockPos.containing(src.getPosition()));
        if (c == null) {
            throw NO_COLONY.create();
        }
        return c;
    }

    private static int reply(CommandSourceStack src, String text) {
        src.sendSuccess(() -> Component.literal("[colony] " + text), false);
        dev.autarkia.Autarkia.LOG.info("[colony] {}", text);
        return 1;
    }

    private static int info(CommandSourceStack src) throws CommandSyntaxException {
        Colony c = colony(src);
        long active = c.projects.stream().filter(p -> p.state.active()).count();
        long proposed = c.projects.stream().filter(p -> p.state == Project.State.PROPOSED).count();
        return reply(src, String.format(java.util.Locale.ROOT, "%s id %s T%d %s %s budget %.1f/%d day %d projects %d active %d proposed workers %d/%d memories %d",
            c.name, c.id.toString().substring(0, 8), c.tier, c.state.getSerializedName().toUpperCase(java.util.Locale.ROOT), c.autonomy.getSerializedName(),
            c.budgetRemaining, ColonyTiers.dailyBudget(c.tier), ColonyClock.day(c), active, proposed, c.workerCost(), ColonyTiers.workerCap(c.tier),
            c.memories.size()));
    }

    private static int domainShow(CommandSourceStack src) throws CommandSyntaxException {
        Colony c = colony(src);
        ColonyProfiler.showDomain(src.getLevel(), c, 200);
        return reply(src, "domain border for 200 ticks, half-size " + ColonyTiers.domainHalf(c.tier));
    }

    private static int sensorDump(CommandSourceStack src) throws CommandSyntaxException {
        Colony c = colony(src);
        ColonySensors.sample(c, src.getLevel());
        ColonyRuntime rt = ColonyRuntime.of(c);
        StringBuilder sb = new StringBuilder("signals:");
        for (Map.Entry<String, Double> e : rt.snapshot.entrySet()) {
            sb.append(' ').append(e.getKey()).append('=').append(String.format(java.util.Locale.ROOT, "%.3f", rt.signal(e.getKey(), src.getLevel().getGameTime(), 0)));
        }
        reply(src, sb.toString());
        for (Map.Entry<String, Colony.TemplateState> e : c.templates.entrySet()) {
            reply(src, "holds " + e.getKey() + " " + e.getValue().holds);
        }
        return 1;
    }

    private static int explain(CommandSourceStack src, String id) throws CommandSyntaxException {
        Colony c = colony(src);
        if (ColonyPlans.templates().get(id) == null) {
            throw new SimpleCommandExceptionType(Component.translatable("command.autarkia.no_template")).create();
        }
        return reply(src, explainText(c, src.getLevel(), id));
    }

    /** Return the planner explanation of a loaded template: its conditions with their current values, holds, urgency, site and open projects. */
    public static String explainText(Colony c, ServerLevel level, String id) {
        ColonyTemplate tpl = ColonyPlans.templates().get(id);
        Colony.TemplateState ts = c.template(id);
        StringBuilder sb = new StringBuilder(id + ": ");
        for (ColonyTemplate.Condition cond : tpl.allConditions()) {
            sb.append(cond.signal()).append(cond.op()).append(cond.valueSignal().map(v -> cond.factor() + "x" + v).orElse(String.valueOf(cond.value())))
                .append(" now ").append(String.format(java.util.Locale.ROOT, "%.3f", Planner.signal(c, level, cond.signal()))).append("; ");
        }
        sb.append("holds ").append(ts.holds).append(" triggered ").append(Planner.triggered(c, level, id, tpl));
        sb.append(" U ").append(String.format(java.util.Locale.ROOT, "%.2f", Planner.urgency(c, level, id, tpl)));
        sb.append(" best site ").append(String.format(java.util.Locale.ROOT, "%.2f", ColonyRuntime.of(c).bestSite.getOrDefault(id, 0.0)));
        sb.append(" cooldown ").append(Math.max(0, ts.cooldownUntil - c.clock)).append(" no-site ").append(Math.max(0, ts.noSiteUntil - c.clock));
        for (Project p : c.projects) {
            if (p.template.getPath().equals(id) && p.state.open()) {
                sb.append(String.format(java.util.Locale.ROOT, " | project %s %s S %.2f C %.1f P %.1f", p.shortId(), p.state.getSerializedName(), p.siteScore, p.cost,
                    p.priority));
            }
        }
        return sb.toString();
    }

    private static int projectList(CommandSourceStack src) throws CommandSyntaxException {
        Colony c = colony(src);
        if (c.projects.isEmpty()) {
            return reply(src, "no projects");
        }
        for (Project p : c.projects) {
            reply(src, String.format(java.util.Locale.ROOT, "%s %s %s at %s phase %d C %.1f P %.1f %s", p.shortId(), p.template.getPath(),
                p.state.getSerializedName(), p.origin.toShortString(), p.phase, p.cost, p.priority, p.blockReason));
        }
        return c.projects.size();
    }

    private static int jobList(CommandSourceStack src) throws CommandSyntaxException {
        Colony c = colony(src);
        var jobs = ColonyRuntime.of(c).jobs.all();
        if (jobs.isEmpty()) {
            return reply(src, "no jobs");
        }
        for (ColonyJob j : jobs) {
            reply(src, j.type.name().toLowerCase(java.util.Locale.ROOT) + " " + j.target.toShortString() + " " + j.status.name().toLowerCase(java.util.Locale.ROOT)
                + " attempts " + j.attempts);
        }
        return jobs.size();
    }

    private static int memoryList(CommandSourceStack src) throws CommandSyntaxException {
        Colony c = colony(src);
        double half = dev.autarkia.config.AutarkiaConfig.MEMORY_HALF_LIFE_TICKS.get();
        if (c.memories.isEmpty()) {
            return reply(src, "no memories");
        }
        for (Colony.Memory m : c.memories) {
            reply(src, String.format(java.util.Locale.ROOT, "%s %s at %s w %.3f", m.type(), m.key(), m.pos().toShortString(), m.weightAt(c.clock, half)));
        }
        return c.memories.size();
    }

    private static int memoryAdd(CommandSourceStack src, String type, BlockPos pos) throws CommandSyntaxException {
        Colony c = colony(src);
        dev.autarkia.colony.ColonyMemory.event(c, type, type + "@" + pos.toShortString(), pos);
        return reply(src, "memory " + type + " at " + pos.toShortString());
    }

    /** Draw the domain border with glow particles once. */
    static void border(ServerLevel level, Colony c) {
        int h = ColonyTiers.domainHalf(c.tier);
        BlockPos core = c.corePos;
        for (int d = -h; d <= h; d += 2) {
            for (int[] p : new int[][] {{d, -h}, {d, h}, {-h, d}, {h, d}}) {
                int x = core.getX() + p[0];
                int z = core.getZ() + p[1];
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                level.sendParticles(GlowParticleOptions.of(0xFF60E0FF).size(0.2f).age(40), x + 0.5, y + 0.6, z + 0.5, 1, 0, 0.2, 0, 0);
            }
        }
    }
}

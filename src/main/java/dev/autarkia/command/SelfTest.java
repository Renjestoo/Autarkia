package dev.autarkia.command;

import dev.autarkia.Autarkia;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Milestone self-test runner: a script of actions, waits and checks that runs on the server tick (use it under
 * {@code /tick sprint}). Each check prints PASS or FAIL in chat and in the log; the run ends with {@code RESULT x/y}.
 */
public final class SelfTest {
    private static @Nullable SelfTest active;
    private static final java.util.ArrayDeque<Runnable> queue = new java.util.ArrayDeque<>();

    private final String name;
    private final CommandSourceStack source;
    public final ServerLevel level;
    public final BlockPos origin;
    public final @Nullable ServerPlayer player;
    private final List<Step> steps = new ArrayList<>();
    private int index;
    private int waited;
    private int passed;
    private int total;
    public long tick;

    private interface Step {
        boolean run(SelfTest t);
    }

    private SelfTest(String name, CommandSourceStack source) {
        this.name = name;
        this.source = source;
        this.level = source.getLevel();
        this.player = source.getPlayer();
        BlockPos base = BlockPos.containing(source.getPosition());
        this.origin = player != null ? base.relative(player.getDirection(), 3) : base;
    }

    /** Create a test script for the source; {@link #start()} runs it. */
    public static SelfTest create(String name, CommandSourceStack source) {
        return new SelfTest(name, source);
    }

    /** Add an immediate action. */
    public SelfTest then(Consumer<SelfTest> action) {
        steps.add(t -> {
            action.accept(t);
            return true;
        });
        return this;
    }

    /** Add an action that runs every tick for the given number of ticks. */
    public SelfTest repeat(int ticks, Consumer<SelfTest> action) {
        steps.add(t -> {
            action.accept(t);
            return ++t.waited >= ticks;
        });
        return this;
    }

    /** Wait a number of ticks. */
    public SelfTest waitTicks(int ticks) {
        steps.add(t -> ++t.waited >= ticks);
        return this;
    }

    /** Wait until the condition holds, failing the named check after timeout ticks. */
    public SelfTest waitUntil(String check, int timeout, BooleanSupplier cond, Consumer<SelfTest> each) {
        steps.add(t -> {
            each.accept(t);
            if (cond.getAsBoolean()) {
                t.report(check, true, "after " + t.waited + " ticks");
                return true;
            }
            if (++t.waited >= timeout) {
                t.report(check, false, "timeout after " + timeout + " ticks");
                return true;
            }
            return false;
        });
        return this;
    }

    /** Add a check. */
    public SelfTest check(String check, BooleanSupplier cond, Supplier<String> detail) {
        steps.add(t -> {
            boolean ok;
            String d;
            try {
                ok = cond.getAsBoolean();
                d = detail.get();
            } catch (RuntimeException e) {
                ok = false;
                d = "exception " + e;
                Autarkia.LOG.error("[TEST {}] check {} threw", name, check, e);
            }
            t.report(check, ok, d);
            return true;
        });
        return this;
    }

    /** Record a check result directly (inside actions). */
    public void report(String check, boolean ok, String detail) {
        total++;
        if (ok) {
            passed++;
        }
        String line = (ok ? "PASS " : "FAIL ") + check + ": " + detail;
        Autarkia.LOG.info("[TEST {}] {}", name, line);
        source.sendSuccess(() -> Component.literal("[" + name + "] " + line).withColor(ok ? 0x55FF55 : 0xFF5555), false);
    }

    /** Clear a box to air above a stone floor (both corners relative to the origin). */
    public void clear(int x0, int y0, int z0, int x1, int y1, int z1) {
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(x0, y0 - 1, z0), origin.offset(x1, y0 - 1, z1))) {
            level.setBlock(p, Blocks.STONE.defaultBlockState(), 2 | 16);
        }
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(x0, y0, z0), origin.offset(x1, y1, z1))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    /** Run a server command with the test source (output suppressed). */
    public void command(String cmd) {
        level.getServer().getCommands().performPrefixedCommand(source.withSuppressedOutput(), cmd);
    }

    /** Return the origin offset. */
    public BlockPos at(int x, int y, int z) {
        return origin.offset(x, y, z);
    }

    /** Start the script (one test at a time). */
    public int start() {
        if (active != null) {
            source.sendFailure(Component.literal("A self test is already running"));
            return 0;
        }
        active = this;
        level.getServer().tickRateManager().requestGameToSprint(1_000_000);
        Autarkia.LOG.info("[TEST {}] start at {} ({} steps)", name, origin, steps.size());
        return 1;
    }

    /**
     * Run the given tests one after another, each at its own origin 400 blocks further south (force-loaded first).
     * Prints {@code [TEST all] RESULT x/y tests passed} at the end.
     */
    public static int runAll(CommandSourceStack source, List<java.util.function.ToIntFunction<CommandSourceStack>> tests) {
        if (active != null || !queue.isEmpty()) {
            source.sendFailure(Component.literal("A self test is already running"));
            return 0;
        }
        int[] ok = new int[1];
        List<SelfTest> runs = new ArrayList<>();
        for (int i = 0; i < tests.size(); i++) {
            Vec3 at = source.getPosition().add(0, 0, 400.0 * i);
            var test = tests.get(i);
            queue.add(() -> {
                BlockPos o = BlockPos.containing(at);
                source.getServer().getCommands().performPrefixedCommand(source.withSuppressedOutput(),
                    "forceload add " + (o.getX() - 40) + " " + (o.getZ() - 40) + " " + (o.getX() + 160) + " " + (o.getZ() + 60));
                test.applyAsInt(source.withPosition(at));
                if (active != null) {
                    runs.add(active);
                }
            });
        }
        queue.add(() -> {
            runs.forEach(r -> ok[0] += r.passed == r.total ? 1 : 0);
            String res = "RESULT " + ok[0] + "/" + tests.size() + " tests passed";
            Autarkia.LOG.info("[TEST all] {}", res);
            source.sendSuccess(() -> Component.literal("[all] " + res).withColor(ok[0] == tests.size() ? 0x55FF55 : 0xFF5555), false);
        });
        queue.poll().run();
        return 1;
    }

    /** Register the server tick driver. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            SelfTest t = active;
            if (t == null) {
                Runnable next = queue.poll();
                if (next != null) {
                    next.run();
                }
                return;
            }
            t.tick++;
            int guard = 0;
            while (t.index < t.steps.size() && guard++ < 64) {
                try {
                    if (!t.steps.get(t.index).run(t)) {
                        return;
                    }
                } catch (RuntimeException ex) {
                    Autarkia.LOG.error("[TEST {}] step {} threw; the rest of the test is skipped", t.name, t.index, ex);
                    t.report("step " + t.index + " ran without an exception", false, ex.toString());
                    t.index = t.steps.size();
                    break;
                }
                t.index++;
                t.waited = 0;
            }
            if (t.index >= t.steps.size()) {
                active = null;
                t.level.getServer().tickRateManager().stopSprinting();
                String res = "RESULT " + t.passed + "/" + t.total;
                Autarkia.LOG.info("[TEST {}] {}", t.name, res);
                t.source.sendSuccess(() -> Component.literal("[" + t.name + "] " + res).withColor(t.passed == t.total ? 0x55FF55 : 0xFF5555), false);
            }
        });
    }
}

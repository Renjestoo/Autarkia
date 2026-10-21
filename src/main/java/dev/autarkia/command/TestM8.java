package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * {@code /autarkia test m8}: the materials ({@link TestM8Materials}, x 0..40), power ({@link TestM8Power}, x 50..130),
 * logistics ({@link TestM8Logistics}, x 140..240, the rail line up to z 300) and equipment ({@link TestM8Equipment},
 * x 250..330) parts on a platform at y 120 or higher (x -2 to 340, z -2 to 300, air from y 0 to 24 over a stone floor
 * at y -1, force-loaded in two parts since one {@code forceload add} takes at most 256 chunks; the script waits until every
 * platform chunk ticks with its entities loaded, because block entities only tick in such chunks), then the T5 colony ({@link TestM8Colony}: a Core at x 400, z 30 with its domain x 336..464,
 * z -34..94, and a strip to x 740 for the Replicator Walker) with the nanite claims ({@link TestM8Nanites}) and the
 * Replicator (W19).
 */
public final class TestM8 {
    private static final List<String> SECTIONS = List.of("all", "materials", "power", "logistics", "equipment", "nanites", "colony");
    private static final int SLAB = 12;
    private static final UUID[] COLONY = new UUID[1];

    private TestM8() {
    }

    /** One M8 run: the test and the platform base. */
    record Run(SelfTest t, BlockPos base) {
        BlockPos at(int x, int y, int z) {
            return base.offset(x, y, z);
        }
    }

    /** Return the T5 test colony of the running M8 test, or null before its setup. */
    static @Nullable Colony colony() {
        return COLONY[0] == null ? null : Colonies.registry().get(COLONY[0]);
    }

    static void colony(@Nullable Colony c) {
        COLONY[0] = c == null ? null : c.id;
    }

    /** Build the platform and run the whole M8 script. */
    public static int run(CommandSourceStack src) {
        return run(src, "all");
    }

    /**
     * Build the platform and run the M8 script from a section to the end: {@code all}, {@code materials}, {@code power},
     * {@code logistics}, {@code equipment}, {@code nanites} or {@code colony}. The T5 colony setup runs before the nanite
     * and colony sections. An unknown section fails the command without a run.
     */
    public static int run(CommandSourceStack src, String from) {
        return run(src, from, false);
    }

    /**
     * Build the platform and run the M8 script from a section to the end, or with only true just that section (the
     * nanite section still sets up the T5 colony first). An unknown section fails the command without a run.
     */
    public static int run(CommandSourceStack src, String from, boolean only) {
        int start = SECTIONS.indexOf(from);
        if (start < 0) {
            src.sendFailure(Component.literal("Unknown M8 section '" + from + "'; use one of " + String.join(", ", SECTIONS)));
            return 0;
        }
        int end = only && start > 0 ? start : SECTIONS.size() - 1;
        SelfTest t = SelfTest.create("m8", src);
        BlockPos o = t.origin;
        Run r = new Run(t, new BlockPos(o.getX(), Math.max(o.getY() + 8, 120), o.getZ()));
        reset(r);
        if (start <= 1 && end >= 1) {
            TestM8Materials.run(r);
        }
        if (start <= 2 && end >= 2) {
            TestM8Power.run(r);
        }
        if (start <= 3 && end >= 3) {
            TestM8Logistics.run(r);
        }
        if (start <= 4 && end >= 4) {
            TestM8Equipment.run(r);
        }
        if (end >= 5) {
            TestM8Colony.setup(r);
        }
        if (start <= 5 && end >= 5) {
            TestM8Nanites.run(r);
        }
        if (end >= 6) {
            TestM8Colony.run(r);
        }
        return t.start();
    }

    static void box(Run r, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (BlockPos p : BlockPos.betweenClosed(r.at(x0, y0, z0), r.at(x1, y1, z1))) {
            r.t().level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private static boolean ticking(Run r) {
        BlockPos lo = r.at(-2, 0, -2);
        BlockPos hi = r.at(340, 0, 300);
        for (int cx = lo.getX() >> 4; cx <= hi.getX() >> 4; cx++) {
            for (int cz = lo.getZ() >> 4; cz <= hi.getZ() >> 4; cz++) {
                if (!r.t().level.isPositionTickingWithEntitiesLoaded(ChunkPos.pack(cx, cz))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void reset(Run r) {
        SelfTest t = r.t();
        t.then(x -> {
            x.command("forceload add " + (r.base().getX() - 8) + " " + (r.base().getZ() - 8) + " " + (r.base().getX() + 346) + " "
                + (r.base().getZ() + 150));
            x.command("forceload add " + (r.base().getX() - 8) + " " + (r.base().getZ() + 151) + " " + (r.base().getX() + 346) + " "
                + (r.base().getZ() + 308));
            x.command("weather clear");
            x.command("gamerule doMobSpawning false");
            AABB area = new AABB(Vec3.atCenterOf(r.at(-8, -60, -8)), Vec3.atCenterOf(r.at(346, 40, 308)));
            x.level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area, e -> !(e instanceof net.minecraft.world.entity.player.Player))
                .forEach(e -> e.discard());
            SelfTest.clearSpans(x.level, area);
        });
        t.waitUntil("M8 platform: every force-loaded platform chunk ticks with its entities loaded", 24000, () -> ticking(r), x -> {
        });
        for (int z0 = -2; z0 <= 300; z0 += SLAB) {
            int a = z0;
            int b = Math.min(300, z0 + SLAB - 1);
            t.then(x -> {
                box(r, -2, 0, a, 340, 24, b, Blocks.AIR.defaultBlockState());
                box(r, -2, -1, a, 340, -1, b, Blocks.STONE.defaultBlockState());
            });
        }
        t.then(x -> Autarkia.LOG.info("[TEST m8] platform at {}", r.base().toShortString()));
    }
}

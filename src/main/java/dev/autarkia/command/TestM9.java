package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.storage.MatterBank;
import dev.autarkia.storage.MatterBufferBlockEntity;
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
 * {@code /autarkia test m9}: a T6 colony ({@link TestM9Colony}: Core at x 200, z 100, domain x 104..296, z 4..196) on a
 * platform at y 120 or higher (x -2 to 440, z -2 to 300, air from y 0 to 24 over a stone floor at y -1, force-loaded in
 * three parts since one {@code forceload add} takes at most 256 chunks; the script waits until every platform chunk ticks
 * with its entities loaded), then the power ({@link TestM9Power}), materials ({@link TestM9Materials}), logistics
 * ({@link TestM9Logistics}), Aegis gear ({@link TestM9Gear}), Aegis rule ({@link TestM9Rule}), restoration
 * ({@link TestM9Restoration}) and seed ({@link TestM9Seed}) sections in their regions of that platform.
 */
public final class TestM9 {
    private static final List<String> SECTIONS = List.of("all", "power", "materials", "logistics", "gear", "rule", "restoration", "seed");
    private static final int SLAB = 8;
    private static final int X1 = 440;
    private static final int Z1 = 300;
    private static final UUID[] COLONY = new UUID[1];

    private TestM9() {
    }

    /** One M9 run: the test and the platform base. */
    record Run(SelfTest t, BlockPos base) {
        BlockPos at(int x, int y, int z) {
            return base.offset(x, y, z);
        }
    }

    /** Return the T6 test colony of the running M9 test, or null before its setup. */
    static @Nullable Colony colony() {
        return COLONY[0] == null ? null : Colonies.registry().get(COLONY[0]);
    }

    static void colony(@Nullable Colony c) {
        COLONY[0] = c == null ? null : c.id;
    }

    /** Set the free matter units of the test domain to exactly mu: every buffer is emptied, the first buffer gets mu. */
    static void freeMatter(SelfTest t, long mu) {
        Colony c = colony();
        if (c == null) {
            throw new IllegalStateException("M9 test colony is not set up; freeMatter needs TestM9Colony.setup first");
        }
        List<MatterBufferBlockEntity> buffers = MatterBank.inDomain(t.level, c);
        if (buffers.isEmpty()) {
            throw new IllegalStateException("M9 test colony has no matter buffer in its domain");
        }
        for (MatterBufferBlockEntity b : buffers) {
            b.takeFree(b.free());
        }
        buffers.get(0).addFree(mu);
    }

    /** Build the platform and run the whole M9 script. */
    public static int run(CommandSourceStack src) {
        return run(src, "all", false);
    }

    /**
     * Build the platform and the T6 colony, then run the M9 script from a section to the end, or with only true just that
     * section: {@code all}, {@code power}, {@code materials}, {@code logistics}, {@code gear}, {@code rule},
     * {@code restoration} or {@code seed}. Every section after power needs its powered domain, so a run that starts later
     * still brings the singularity core online first. An unknown section fails the command without a run.
     */
    public static int run(CommandSourceStack src, String from, boolean only) {
        int start = SECTIONS.indexOf(from);
        if (start < 0) {
            src.sendFailure(Component.literal("Unknown M9 section '" + from + "'; use one of " + String.join(", ", SECTIONS)));
            return 0;
        }
        int end = only && start > 0 ? start : SECTIONS.size() - 1;
        SelfTest t = SelfTest.create("m9", src);
        BlockPos o = t.origin;
        Run r = new Run(t, new BlockPos(o.getX(), Math.max(o.getY() + 8, 120), o.getZ()));
        reset(r);
        TestM9Colony.setup(r);
        if (start <= 1 && end >= 1) {
            TestM9Power.run(r);
        } else {
            TestM9Power.online(r);
        }
        if (start <= 2 && end >= 2) {
            TestM9Materials.run(r);
        }
        if (start <= 3 && end >= 3) {
            TestM9Logistics.run(r);
        }
        if (start <= 4 && end >= 4) {
            TestM9Gear.run(r);
        }
        if (start <= 5 && end >= 5) {
            TestM9Rule.run(r);
        }
        if (start <= 6 && end >= 6) {
            TestM9Restoration.run(r);
        }
        if (start <= 7 && end >= 7) {
            TestM9Seed.run(r);
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
        BlockPos hi = r.at(X1, 0, Z1);
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
            int bx = r.base().getX();
            int bz = r.base().getZ();
            for (int[] z : new int[][] {{-8, 95}, {96, 199}, {200, Z1 + 8}}) {
                x.command("forceload add " + (bx - 8) + " " + (bz + z[0]) + " " + (bx + X1 + 6) + " " + (bz + z[1]));
            }
            x.command("weather clear");
            x.command("gamerule doMobSpawning false");
            AABB area = new AABB(Vec3.atCenterOf(r.at(-8, -60, -8)), Vec3.atCenterOf(r.at(X1 + 6, 60, Z1 + 8)));
            x.level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area, e -> !(e instanceof net.minecraft.world.entity.player.Player))
                .forEach(e -> e.discard());
            SelfTest.clearSpans(x.level, area);
        });
        t.waitUntil("M9 platform: every force-loaded platform chunk ticks with its entities loaded", 24000, () -> ticking(r), x -> {
        });
        for (int z0 = -2; z0 <= Z1; z0 += SLAB) {
            int a = z0;
            int b = Math.min(Z1, z0 + SLAB - 1);
            t.then(x -> {
                box(r, -2, 0, a, X1, 24, b, Blocks.AIR.defaultBlockState());
                box(r, -2, -1, a, X1, -1, b, Blocks.STONE.defaultBlockState());
            });
        }
        t.then(x -> Autarkia.LOG.info("[TEST m9] platform at {}", r.base().toShortString()));
    }
}

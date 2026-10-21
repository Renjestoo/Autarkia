package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colony;
import dev.autarkia.energy.CableTier;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GridNetwork;
import dev.autarkia.energy.GyroHeartBlockEntity;
import dev.autarkia.energy.IndustrialLampBlockEntity;
import dev.autarkia.energy.PhaseLinks;
import dev.autarkia.energy.SingularityCoreBlockEntity;
import dev.autarkia.energy.Span;
import dev.autarkia.registry.AutarkiaBlocks;
import dev.autarkia.registry.AutarkiaItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The power section of {@code /autarkia test m9}: 02 #18 (a phase pair 40 blocks apart outside the domain at x 10 to 60,
 * z 10 to 30, linked with the wrench by the unconnected courier player) and 02 #19 (W23: a real
 * {@code /summon minecraft:wither ... {Invul:220}} inside the domain at x 140 to 170, z 50 to 70; the tag gives the summoned
 * Wither the spawn sequence of a built one, which ends in the r 7 explosion), one check each. Between them
 * {@link #online} builds the T6 power rig that later sections rely on and that stays standing.
 */
public final class TestM9Power {
    public static final int REFILL_FE = 500_000_000;
    public static final int CHAIN_Y = 20;
    private static final BlockPos[] BASE = new BlockPos[1];

    private TestM9Power() {
    }

    /** Add the power checks to the M9 script; it ends with the online state of {@link #online}. */
    static void run(TestM9.Run r) {
        phaseLink(r);
        online(r);
        wither(r);
        r.t().then(TestM9Power::refill);
    }

    private static BlockPos core(BlockPos base) {
        return base.offset(130, 0, 30);
    }

    private static BlockPos gyro(BlockPos base) {
        return base.offset(150, 0, 20);
    }

    private static List<BlockPos> chain(TestM9.Run r) {
        List<BlockPos> out = new ArrayList<>();
        for (int x : new int[] {110, 150, 190, 230, 270, 290}) {
            out.add(r.at(x, CHAIN_Y, 40));
        }
        for (int z : new int[] {40, 80, 120, 160, 190}) {
            out.add(r.at(200, CHAIN_Y, z));
        }
        return out;
    }

    private static List<BlockPos[]> links(TestM9.Run r) {
        List<BlockPos> c = chain(r);
        List<BlockPos[]> out = new ArrayList<>();
        out.add(new BlockPos[] {r.at(130, 5, 30), r.at(150, CHAIN_Y, 40)});
        out.add(new BlockPos[] {r.at(150, 3, 20), r.at(130, 5, 30)});
        for (int i = 0; i + 1 < 6; i++) {
            out.add(new BlockPos[] {c.get(i), c.get(i + 1)});
        }
        out.add(new BlockPos[] {c.get(2), c.get(6)});
        for (int i = 6; i + 1 < c.size(); i++) {
            out.add(new BlockPos[] {c.get(i), c.get(i + 1)});
        }
        return out;
    }

    private static @Nullable SingularityCoreBlockEntity coreAt(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof SingularityCoreBlockEntity c ? c : null;
    }

    private static @Nullable GyroHeartBlockEntity gyroAt(SelfTest t, BlockPos p) {
        return t.level.getBlockEntity(p) instanceof GyroHeartBlockEntity g ? g : null;
    }

    private static boolean advancement(ServerPlayer p, SelfTest t, String id) {
        AdvancementHolder h = t.level.getServer().getAdvancements().get(Autarkia.id(id));
        return h != null && p.getAdvancements().getOrStartProgress(h).isDone();
    }

    /**
     * Build the T6 power rig of the M9 platform and bring it ONLINE: the Singularity Core at (130, 0, 30) with a phase node
     * on its top, a Gyro Heart at (150, 0, 20) with a phase node on its top (charged full to feed the ignition), a chain of
     * phase nodes at y {@link #CHAIN_Y} (above every test rig) every 40 blocks along z 40 from x 110 to 290 and along x 200
     * from z 40 to 190, linked by the wrench rules, and 8 industrial lamps tapped by the chain. The core ignites from the grid (at least 96 ticks); the
     * section waits 140 ticks and then calls {@link #refill}.
     */
    public static void online(TestM9.Run r) {
        SelfTest t = r.t();
        BASE[0] = r.base();
        BlockPos core = core(r.base());
        BlockPos gyro = gyro(r.base());
        t.then(x -> {
            boolean ok = TestM1.placeBig(x, core, AutarkiaBlocks.SINGULARITY_CORE.get(), Direction.NORTH)
                & TestM1.placeBig(x, gyro, AutarkiaBlocks.GYRO_HEART.get(), Direction.NORTH);
            List<BlockPos> c = chain(r);
            for (int i = 0; i < 8; i++) {
                BlockPos lamp = i < 6 ? c.get(i).below() : c.get(i + 1).below();
                TestM1.place(x, lamp, AutarkiaBlocks.INDUSTRIAL_LAMP.get().defaultBlockState());
            }
            for (BlockPos p : List.of(r.at(130, 5, 30), r.at(150, 3, 20))) {
                TestM1.place(x, p, AutarkiaBlocks.PHASE_NODE.get().defaultBlockState());
            }
            c.forEach(p -> TestM1.place(x, p, AutarkiaBlocks.PHASE_NODE.get().defaultBlockState()));
            Autarkia.LOG.info("[TEST m9] power rig placed: {}", ok);
        });
        t.waitTicks(2);
        t.then(x -> {
            List<String> refused = new ArrayList<>();
            for (BlockPos[] l : links(r)) {
                Grid.SpanResult res = PhaseLinks.link(x.level, null, l[0], l[1]);
                if (res != Grid.SpanResult.OK) {
                    refused.add(l[0].toShortString() + "-" + l[1].toShortString() + " " + res);
                }
            }
            GyroHeartBlockEntity g = gyroAt(x, gyro);
            if (g != null) {
                g.buffer().fill(g.buffer().room());
            }
            Autarkia.LOG.info("[TEST m9] power rig links refused: {}", refused);
        });
        t.waitTicks(10);
        t.then(x -> {
            SingularityCoreBlockEntity c = coreAt(x, core);
            if (c != null) {
                c.ignite();
            }
        });
        t.waitTicks(140);
        t.then(TestM9Power::refill);
    }

    /**
     * Refill the online rig for the next test: an OFF core is ignited with its ignition FE given directly (ONLINE from its
     * next tick), the core buffer is set to exactly 500,000,000 FE (so explosions have 500,000,000 FE of room) and the
     * Gyro Heart is charged full. A missing rig is a FAIL line.
     */
    public static void refill(SelfTest t) {
        SingularityCoreBlockEntity c = BASE[0] == null ? null : coreAt(t, core(BASE[0]));
        GyroHeartBlockEntity g = BASE[0] == null ? null : gyroAt(t, gyro(BASE[0]));
        if (c == null || g == null) {
            t.report("M9 power rig: the Singularity Core and Gyro Heart of the online rig stand", false, "core " + c + ", gyro " + g);
            return;
        }
        if (c.state() == SingularityCoreBlockEntity.State.OFF) {
            c.ignite();
            c.ignitionBuffer().fill(SingularityCoreBlockEntity.IGNITION_FE);
        }
        long now = c.buffer();
        if (now < REFILL_FE) {
            c.energy().fill((int) (REFILL_FE - now));
        } else {
            c.energy().drain((int) (now - REFILL_FE));
        }
        g.buffer().fill(g.buffer().room());
    }

    private static void wrench(ServerPlayer p, BlockPos clicked) {
        ItemStack stack = p.getMainHandItem();
        stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false)));
    }

    private static void phaseLink(TestM9.Run r) {
        SelfTest t = r.t();
        BlockPos a = r.at(12, 1, 15);
        BlockPos b = r.at(52, 1, 15);
        BlockPos lamp = r.at(52, 0, 15);
        BlockPos wall = r.at(32, 1, 15);
        long[] m = {-1, -1, -1, -1};
        String[] s = {"", "", ""};
        boolean[] ok = new boolean[4];
        t.then(x -> {
            ServerPlayer courier = x.player != null ? x.player : net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(x.level);
            AdvancementHolder h = x.level.getServer().getAdvancements().get(Autarkia.id("power/phase_link"));
            if (h != null && x.player != null) {
                x.player.getAdvancements().revoke(h, "done");
            }
            TestM1.place(x, a, AutarkiaBlocks.PHASE_NODE.get().defaultBlockState());
            TestM1.place(x, b, AutarkiaBlocks.PHASE_NODE.get().defaultBlockState());
            TestM1.place(x, lamp, AutarkiaBlocks.INDUSTRIAL_LAMP.get().defaultBlockState());
            courier.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AutarkiaItems.WRENCH.get()));
            wrench(courier, a);
            wrench(courier, b);
            courier.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        t.repeat(30, x -> TestM1.pushEnergy(x, a, 1000));
        t.then(x -> {
            Span span = TestM1.spanBetween(x, a, b);
            GridNetwork na = Grid.of(x.level).network(a.asLong());
            ok[0] = span != null && span.tier == CableTier.PHASE && Math.abs(span.length - 40) < 1e-3 && span.length <= CableTier.PHASE.maxSpan
                && span.active && na != null && na == Grid.of(x.level).network(b.asLong()) && na.loss == 0 && span.heat == 0
                && CableTier.PHASE.sagFactor == 0 && x.level.getBlockEntity(lamp) instanceof IndustrialLampBlockEntity l && l.lit()
                && (x.player == null || advancement(x.player, x, "power/phase_link"));
            s[0] = span == null ? "no span" : String.format(Locale.ROOT, "linked %.1f blocks, active %b, loss %.1f, heat %.2f, phase_link %b", span.length,
                span.active, na == null ? -1 : na.loss, span.heat, x.player != null && advancement(x.player, x, "power/phase_link"));
            TestM1.place(x, wall, Blocks.STONE.defaultBlockState());
            m[0] = x.level.getGameTime();
        });
        t.repeat(PhaseLinks.CHECK_TICKS + 5, x -> {
            Span span = TestM1.spanBetween(x, a, b);
            if (m[1] < 0 && span != null && span.blocked && !span.active) {
                m[1] = x.level.getGameTime() - m[0];
                ok[1] = Grid.of(x.level).network(a.asLong()) == null && Grid.of(x.level).network(b.asLong()) == null;
            }
        });
        t.then(x -> {
            TestM1.place(x, wall, Blocks.AIR.defaultBlockState());
            m[2] = x.level.getGameTime();
        });
        t.repeat(PhaseLinks.CHECK_TICKS + 5, x -> {
            Span span = TestM1.spanBetween(x, a, b);
            GridNetwork na = Grid.of(x.level).network(a.asLong());
            if (m[3] < 0 && span != null && !span.blocked && span.active && na != null && na == Grid.of(x.level).network(b.asLong())) {
                m[3] = x.level.getGameTime() - m[2];
            }
            TestM1.pushEnergy(x, a, 1000);
        });
        t.check("02#18 two phase nodes 40 blocks apart, linked with the wrench (power/phase_link), carry FE with no loss or heat; a block between "
            + "them makes the link inactive within 40 ticks (network split), removing it makes it active again", () -> ok[0] && m[1] >= 1
            && m[1] <= PhaseLinks.CHECK_TICKS && ok[1] && m[3] >= 1 && m[3] <= PhaseLinks.CHECK_TICKS + 1,
            () -> s[0] + "; inactive after " + m[1] + " ticks (split " + ok[1] + "), active again after " + m[3] + " ticks");
    }

    private static void wither(TestM9.Run r) {
        SelfTest t = r.t();
        BlockPos spawn = r.at(155, 2, 60);
        List<BlockPos> witness = new ArrayList<>();
        for (int dx = -3; dx <= 3; dx += 3) {
            for (int dz = -3; dz <= 3; dz += 3) {
                if (dx != 0 || dz != 0) {
                    witness.add(r.at(155 + dx, 0, 60 + dz));
                }
            }
        }
        witness.add(r.at(155, -1, 60));
        List<BlockPos> cage = new ArrayList<>();
        WitherBoss[] boss = new WitherBoss[1];
        long[] m = {-1, -1, -1, -1, 0, 0};
        long[] prev = {0};
        String[] s = {""};
        t.then(x -> {
            witness.forEach(p -> TestM1.place(x, p, Blocks.STONE.defaultBlockState()));
            refill(x);
            SingularityCoreBlockEntity c = coreAt(x, core(r.base()));
            Colony col = TestM9.colony();
            m[4] = c == null ? -1 : c.explosions();
            m[5] = col == null ? -1 : col.stats.getOrDefault("absorbed", 0L);
            x.command("summon minecraft:wither " + spawn.getX() + " " + spawn.getY() + " " + spawn.getZ() + " {Invul:220}");
            List<WitherBoss> found = x.level.getEntitiesOfClass(WitherBoss.class, new AABB(spawn).inflate(3));
            boss[0] = found.isEmpty() ? null : found.get(0);
            if (boss[0] != null) {
                boss[0].setPersistenceRequired();
                boss[0].getAttribute(Attributes.FLYING_SPEED).setBaseValue(0);
                boss[0].getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
            }
        });
        t.repeat(250, x -> {
            SingularityCoreBlockEntity c = coreAt(x, core(r.base()));
            if (c == null) {
                return;
            }
            if (m[0] < 0 && c.lastAbsorbAt() == x.level.getGameTime() && c.lastRadius() == 7.0F) {
                m[0] = c.lastGain();
                m[1] = c.buffer() - prev[0] - c.output();
                m[2] = c.lastLamps();
            }
            prev[0] = c.buffer();
        });
        t.then(x -> {
            WitherBoss w = boss[0];
            if (w == null || w.isRemoved()) {
                return;
            }
            BlockPos at = w.blockPosition();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 3; dy++) {
                        if (dx != 0 || dz != 0) {
                            cage.add(at.offset(dx, dy, dz));
                        }
                    }
                }
            }
            cage.forEach(p -> TestM1.place(x, p, Blocks.STONE.defaultBlockState()));
            w.hurtServer(x.level, x.level.damageSources().generic(), 1.0F);
            m[3] = w.getHealth() < w.getMaxHealth() ? 1 : 0;
        });
        t.waitTicks(30);
        t.check("02#19 W23: the Wither spawn explosion (r 7) in the T6 domain adds 2,450,000 FE to the ONLINE core, flashes the domain lamps and breaks "
            + "no block (nor does the hurt Wither body); stats.absorbed +1, power/drink_the_blast", () -> {
            SingularityCoreBlockEntity c = coreAt(t, core(r.base()));
            Colony col = TestM9.colony();
            long absorbed = col == null ? -1 : col.stats.getOrDefault("absorbed", 0L);
            boolean blocks = witness.stream().allMatch(p -> t.level.getBlockState(p).is(Blocks.STONE)) && !cage.isEmpty()
                && cage.stream().allMatch(p -> t.level.getBlockState(p).is(Blocks.STONE));
            s[0] = String.format(Locale.ROOT, "wither %s, core %s, gain %d, buffer delta without base %d, lamps %d, explosions %d -> %d, absorbed %d -> %d, "
                    + "hurt %b, witnesses and %d cage blocks stand %b", boss[0] == null ? "missing" : "spawned", c == null ? "missing" : c.state(), m[0], m[1],
                m[2], m[4], c == null ? -1 : c.explosions(), m[5], absorbed, m[3] == 1, cage.size(), blocks);
            return c != null && c.state() == SingularityCoreBlockEntity.State.ONLINE && m[0] == 2_450_000 && Math.abs(m[1] - 2_450_000) <= 2000
                && m[2] >= 8 && c.explosions() > m[4] && absorbed > m[5] && m[3] == 1 && blocks
                && (t.player == null || advancement(t.player, t, "power/drink_the_blast"));
        }, () -> s[0]);
        t.then(x -> {
            if (boss[0] != null) {
                boss[0].discard();
            }
            x.level.getEntitiesOfClass(WitherSkull.class, new AABB(Vec3.atCenterOf(r.at(130, 0, 40)), Vec3.atCenterOf(r.at(180, 24, 80))))
                .forEach(e -> e.discard());
        });
    }
}

package dev.autarkia.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.autarkia.Autarkia;
import dev.autarkia.energy.CableTier;
import dev.autarkia.integration.AnthologyBridge;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.BeamVisualPayload;
import dev.autarkia.network.ProjectPreviewPayload;
import dev.autarkia.network.SpanListPayload;
import dev.autarkia.network.SpanStatePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The /autarkia command root (permission level 2): effect checks, debug commands and milestone self tests. */
public final class AutarkiaCommand {
    private static final UUID FX_GHOST_GROUP = UUID.fromString("00000000-0000-0000-0000-0000000000f0");
    private static final int FX_SPAN_ID = -1000;

    private AutarkiaCommand() {
    }

    /** Register the command tree. */
    public static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("autarkia").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        root.then(Commands.literal("fx")
            .then(Commands.literal("shake").executes(c -> shake(c.getSource(), 1.0f))
                .then(Commands.argument("intensity", FloatArgumentType.floatArg(0, 4)).executes(c -> shake(c.getSource(), FloatArgumentType.getFloat(c, "intensity")))))
            .then(Commands.literal("beam").executes(c -> beam(c.getSource())))
            .then(Commands.literal("ghost").executes(c -> ghost(c.getSource())))
            .then(Commands.literal("span").executes(c -> span(c.getSource(), 0f))
                .then(Commands.argument("heat", FloatArgumentType.floatArg(0, 1)).executes(c -> span(c.getSource(), FloatArgumentType.getFloat(c, "heat"))))));
        root.then(Commands.literal("showcase")
            .then(Commands.literal("m1").executes(c -> Showcase.m1(c.getSource(), 2400))
                .then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 72000))
                    .executes(c -> Showcase.m1(c.getSource(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "ticks")))))
            .then(Commands.literal("m2").executes(c -> Showcase.m2(c.getSource())))
            .then(Commands.literal("m4").executes(c -> ShowcaseM4.build(c.getSource(), 2400))));
        ColonyCommand.register(root);
        WorkerCommand.register(root);
        root.then(Commands.literal("test")
            .then(Commands.literal("m1").executes(c -> TestM1.run(c.getSource())))
            .then(Commands.literal("m2").executes(c -> TestM2.run(c.getSource())))
            .then(Commands.literal("m3").executes(c -> TestM3.run(c.getSource())))
            .then(Commands.literal("m4").executes(c -> TestM4.run(c.getSource())))
            .then(Commands.literal("all").executes(c -> SelfTest.runAll(c.getSource(), List.of(TestM1::run, TestM2::run, TestM3::run, TestM4::run)))));
        d.register(root);
    }

    private static int shake(CommandSourceStack src, float intensity) {
        AnthologyBridge.shake(src.getLevel(), src.getPosition(), intensity, 40, 16f);
        src.sendSuccess(() -> Component.literal("[fx] shake intensity " + intensity), false);
        Autarkia.LOG.info("[fx] shake intensity {} at {}", intensity, src.getPosition());
        return 1;
    }

    private static int beam(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Vec3 from = p.getEyePosition().add(0, -0.3, 0);
        Vec3 to = from.add(p.getLookAngle().scale(16));
        AutarkiaNetwork.toNear(src.getLevel(), from, 64, new BeamVisualPayload(p.getId(), from, to, 60, 0xFF7FFFF0, BeamVisualPayload.KIND_LANCE, 0.5f));
        src.sendSuccess(() -> Component.literal("[fx] beam 16 blocks, 60 ticks"), false);
        Autarkia.LOG.info("[fx] beam from {} to {}", from, to);
        return 1;
    }

    private static int ghost(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        BlockPos base = p.blockPosition().relative(p.getDirection(), 3);
        List<ProjectPreviewPayload.Ghost> ghosts = new ArrayList<>();
        int id = Block.getId(Blocks.OAK_PLANKS.defaultBlockState());
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y < 3; y++) {
                ghosts.add(new ProjectPreviewPayload.Ghost(base.offset(x, y, 0).asLong(), id));
            }
        }
        AutarkiaNetwork.toPlayer(p, new ProjectPreviewPayload(FX_GHOST_GROUP, 0x734DCCFF, ghosts, List.of()));
        src.sendSuccess(() -> Component.literal("[fx] ghost 3x3 at " + base.toShortString()), false);
        Autarkia.LOG.info("[fx] ghost 9 blocks at {}", base);
        return 1;
    }

    private static int span(CommandSourceStack src, float heat) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Direction dir = p.getDirection();
        Vec3 a = Vec3.atCenterOf(p.blockPosition().relative(dir, 2)).add(0, 3, 0);
        Vec3 b = a.add(Vec3.atLowerCornerOf(dir.getClockWise().getUnitVec3i()).scale(16));
        AutarkiaNetwork.toPlayer(p, new SpanListPayload(List.of(new SpanListPayload.Entry(FX_SPAN_ID, false, a, b, CableTier.COPPER, heat, 1.0f, 0, true))));
        AutarkiaNetwork.toPlayer(p, new SpanStatePayload(List.of(new SpanStatePayload.Entry(FX_SPAN_ID, (byte) (heat * 255), (byte) 100, (byte) 0, false, true))));
        src.sendSuccess(() -> Component.literal("[fx] test span 16 blocks, heat " + heat), false);
        Autarkia.LOG.info("[fx] span {} -> {} heat {}", a, b, heat);
        return 1;
    }
}

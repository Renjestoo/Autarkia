package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.energy.Grid;
import dev.autarkia.energy.GyroHeartBlockEntity;
import dev.autarkia.energy.PhaseLinks;
import dev.autarkia.energy.SingularityCoreBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * The power piece of {@code /autarkia showcase m9} in a 20 x 20 area: a Singularity Core and a charged Gyro Heart joined by
 * a phase link between the phase nodes on their tops, and a third phase node on a stone pillar with two industrial lamps.
 * The core ignites from the gyro through the grid (about 96 ticks), then runs ONLINE with its rings turning, feeds the lamps
 * and recharges the spinning gyro.
 */
public final class ShowcaseM9Power {
    private ShowcaseM9Power() {
    }

    /** Build the power piece with its minimum corner at origin (floor at y -1, content from y 0 to 8). */
    public static void buildPower(ServerLevel level, BlockPos origin) {
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(0, 0, 0), origin.offset(19, 8, 19))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(0, -1, 0), origin.offset(19, -1, 19))) {
            level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        }
        SelfTest.clearSpans(level, AABB.encapsulatingFullBlocks(origin.offset(-1, -2, -1), origin.offset(20, 10, 20)));
        BlockPos core = origin.offset(4, 0, 4);
        BlockPos gyro = origin.offset(14, 0, 4);
        BlockPos coreTop = origin.offset(4, 5, 4);
        BlockPos gyroTop = origin.offset(14, 3, 4);
        BlockPos post = origin.offset(10, 4, 16);
        boolean placed = TestM1.placeBigAt(level, core, AutarkiaBlocks.SINGULARITY_CORE.get(), Direction.SOUTH)
            & TestM1.placeBigAt(level, gyro, AutarkiaBlocks.GYRO_HEART.get(), Direction.SOUTH);
        for (int y = 0; y < 4; y++) {
            level.setBlock(post.below(4 - y), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(post.west(), AutarkiaBlocks.INDUSTRIAL_LAMP.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(post.east(), AutarkiaBlocks.INDUSTRIAL_LAMP.get().defaultBlockState(), Block.UPDATE_ALL);
        for (BlockPos p : List.of(coreTop, gyroTop, post)) {
            level.setBlock(p, AutarkiaBlocks.PHASE_NODE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(gyro) instanceof GyroHeartBlockEntity g) {
            g.buffer().fill(g.buffer().room());
        }
        Grid.SpanResult a = PhaseLinks.link(level, null, gyroTop, coreTop);
        Grid.SpanResult b = PhaseLinks.link(level, null, coreTop, post);
        if (level.getBlockEntity(core) instanceof SingularityCoreBlockEntity c) {
            c.ignite();
        }
        Autarkia.LOG.info("[showcase m9] power piece at {}: placed {}, links {} / {}", origin.toShortString(), placed, a, b);
    }
}

package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import dev.autarkia.colony.RestorationProjectorBlockEntity;
import dev.autarkia.registry.AutarkiaBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The restoration piece of {@code /autarkia showcase m9}: a 12 x 5 x 2 stone brick wall with a glass row and a
 * {@code restoration_projector} in front of it, its box set over the wall, kept at full FE, and a {@code matter_buffer}
 * with 100,000 free matter units for the glass (glass drops nothing). Every 300 ticks the driver
 * knocks a 3 x 3 hole into the wall (the blocks drop as debris); the field flies the debris back on arcs and the blocks
 * grow back edge first. Outside every colony domain the piece registers its own tier 6 showcase colony record (no Core
 * block, never ticked) so the projector has a domain.
 */
public final class ShowcaseM9Restoration {
    private static final UUID OWNER = UUID.nameUUIDFromBytes("autarkia-m9-showcase-restoration".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final int PERIOD = 300;

    private record Driver(ServerLevel level, BlockPos projector, BlockPos wall, long start) {
    }

    private static final List<Driver> DRIVERS = new ArrayList<>();

    private ShowcaseM9Restoration() {
    }

    /** Register the driver tick. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> DRIVERS.removeIf(ShowcaseM9Restoration::drive));
    }

    private static boolean drive(Driver d) {
        if (!(d.level.getBlockEntity(d.projector) instanceof RestorationProjectorBlockEntity p)) {
            return true;
        }
        p.energy().fill(p.energy().room());
        long age = d.level.getGameTime() - d.start;
        if (age > 0 && age % PERIOD == 0) {
            int hole = (int) (age / PERIOD % 3);
            BlockPos c = d.wall.offset(2 + hole * 4, 1, 0);
            for (BlockPos q : BlockPos.betweenClosed(c, c.offset(2, 2, 0))) {
                d.level.destroyBlock(q, true);
            }
        }
        return false;
    }

    /** Place the restoration piece in the 20 x 20 area at origin (the min corner) and start its driver. */
    public static void buildRestoration(ServerLevel level, BlockPos origin) {
        if (Colonies.at(level, origin) == null) {
            for (Colony old : List.copyOf(Colonies.ownedBy(OWNER))) {
                Colonies.dissolve(old);
            }
            Colony c = Colony.create(OWNER, "Restoration showcase", level.dimension(), origin.offset(10, 0, 10));
            c.tier = 6;
            Colonies.registry().add(c);
            Colonies.rebuildIndex();
            Colonies.changed();
        }
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(0, 0, 0), origin.offset(19, 9, 19))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(0, -1, 0), origin.offset(19, -1, 19))) {
            level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        BlockPos wall = origin.offset(4, 0, 13);
        for (BlockPos p : BlockPos.betweenClosed(wall, wall.offset(11, 4, 1))) {
            level.setBlock(p, (p.getY() - wall.getY() == 2 ? Blocks.GLASS : Blocks.STONE_BRICKS).defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockPos buffer = origin.offset(1, 0, 1);
        if (TestM1.placeBigAt(level, buffer, AutarkiaBlocks.MATTER_BUFFER.get(), Direction.NORTH)
            && level.getBlockEntity(buffer) instanceof dev.autarkia.storage.MatterBufferBlockEntity mb) {
            mb.addFree(100_000);
        }
        BlockPos projector = origin.offset(10, 0, 5);
        boolean placed = TestM1.placeBigAt(level, projector, AutarkiaBlocks.RESTORATION_PROJECTOR.get(), Direction.SOUTH);
        boolean box = level.getBlockEntity(projector) instanceof RestorationProjectorBlockEntity p && p.setBox(16, 7, 6, -8, -1, 6);
        DRIVERS.add(new Driver(level, projector, wall, level.getGameTime()));
        Autarkia.LOG.info("[showcase] M9 restoration piece at {}: projector {}, box {}", origin.toShortString(), placed, box);
    }
}

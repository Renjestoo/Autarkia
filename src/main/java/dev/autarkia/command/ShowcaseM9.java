package dev.autarkia.command;

import dev.autarkia.Autarkia;
import dev.autarkia.colony.Colonies;
import dev.autarkia.colony.Colony;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * {@code /autarkia showcase m9}: builds the T6 scene in front of the caller. One tier 6 showcase colony record (no Core
 * block, never ticked, the caller trusted) covers the scene, so the domain-bound pieces work; then the six part pieces
 * stand in 20 x 20 slots of that domain: materials and logistics, power (the Singularity Core that ignites from the Gyro
 * Heart and powers the domain grid), gear, Aegis rule, restoration and seed. A colony that already holds the scene
 * centre is used instead of a new record.
 */
public final class ShowcaseM9 {
    private static final UUID OWNER = UUID.nameUUIDFromBytes("autarkia-m9-showcase".getBytes(StandardCharsets.UTF_8));
    private static final int SLOT = 22;
    private static final int X0 = -32;
    private static final int Z0 = 2;

    private ShowcaseM9() {
    }

    private static BlockPos slot(BlockPos o, int column, int row) {
        return o.offset(X0 + column * SLOT, 0, Z0 + row * (SLOT + 2));
    }

    /** Build the M9 scene; the pieces with a driver run it for their own time. */
    public static int build(CommandSourceStack src) {
        ServerLevel l = src.getLevel();
        ServerPlayer player = src.getPlayer();
        BlockPos o = BlockPos.containing(src.getPosition()).relative(player != null ? player.getDirection() : Direction.SOUTH, 4);
        for (BlockPos p : BlockPos.betweenClosed(o.offset(X0 - 1, 0, Z0 - 2), o.offset(-X0, 20, Z0 + 2 * SLOT + 2))) {
            l.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (BlockPos p : BlockPos.betweenClosed(o.offset(X0 - 1, -2, Z0 - 2), o.offset(-X0, -1, Z0 + 2 * SLOT + 2))) {
            l.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        }
        AABB box = AABB.encapsulatingFullBlocks(o.offset(X0 - 3, -2, Z0 - 4), o.offset(-X0 + 2, 22, Z0 + 2 * SLOT + 4));
        l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, box, e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(e -> e.discard());
        SelfTest.clearSpans(l, box);

        BlockPos centre = o.offset(0, 0, Z0 + SLOT);
        Colony colony = Colonies.at(l, centre);
        if (colony == null) {
            for (Colony old : List.copyOf(Colonies.ownedBy(OWNER))) {
                Colonies.dissolve(old);
            }
            colony = Colony.create(OWNER, "M9 showcase", l.dimension(), centre);
            colony.tier = 6;
            if (player != null) {
                colony.trusted.add(player.getUUID());
            }
            Colonies.registry().add(colony);
            Colonies.rebuildIndex();
            Colonies.changed();
        }

        ShowcaseM9MatLog.buildMatLog(l, slot(o, 0, 0));
        ShowcaseM9Power.buildPower(l, slot(o, 1, 0));
        ShowcaseM9Gear.buildGear(l, slot(o, 2, 0));
        ShowcaseM9Rule.buildRule(l, slot(o, 0, 1));
        ShowcaseM9Restoration.buildRestoration(l, slot(o, 1, 1));
        ShowcaseM9Seed.buildSeed(l, slot(o, 2, 1));

        String state = "colony " + colony.name + " tier " + colony.tier + " at " + colony.corePos.toShortString();
        src.sendSuccess(() -> Component.literal("[showcase] M9 scene built at " + o.toShortString() + "; " + state), false);
        Autarkia.LOG.info("[showcase] M9 scene at {}: {}", o.toShortString(), state);
        return 1;
    }
}

package dev.autarkia.colony;

import dev.autarkia.config.AutarkiaConfig;
import dev.autarkia.network.AutarkiaNetwork;
import dev.autarkia.network.ProjectPreviewPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.nikdo53.tinymultiblocklib.block.BaseMultiblock;
import org.jspecify.annotations.Nullable;

/**
 * Server side of the ghost preview (05 section 4.1): planned cells not yet built (big blocks over their whole
 * footprint), nearest to the player first, at most {@code colony.ghostMaxBlocks}, colored by project state (proposed cyan,
 * approved or waiting green, blocked red), plus the planned spans.
 */
public final class ColonyPreview {
    private static final int CYAN = 0x734DCCFF;
    private static final int GREEN = 0x7366FF66;
    private static final int RED = 0x73FF4D4D;

    private ColonyPreview() {
    }

    /** Answer a request: one project, or every open project of the colony the player stands in. */
    public static void answer(ServerPlayer player, @Nullable UUID projectId) {
        if (Colonies.server() == null) {
            return;
        }
        ServerLevel level = player.level();
        for (Colony c : Colonies.registry().all()) {
            if (!c.dimension.equals(level.dimension())) {
                continue;
            }
            for (Project p : c.projects) {
                boolean wanted = projectId != null ? p.id.equals(projectId) : Colonies.inside(c, player.blockPosition()) && p.state.open();
                if (wanted) {
                    AutarkiaNetwork.toPlayer(player, build(level, player, p));
                }
            }
        }
    }

    /** Build the preview payload of a project for a player (empty when the project is closed). */
    public static ProjectPreviewPayload build(ServerLevel level, ServerPlayer player, Project p) {
        int color = switch (p.state) {
            case PROPOSED -> CYAN;
            case BLOCKED -> RED;
            default -> GREEN;
        };
        List<ProjectPreviewPayload.Ghost> ghosts = new ArrayList<>();
        if (p.state.open()) {
            for (Project.Cell cell : p.cells) {
                if (Builds.placed(level, cell)) {
                    continue;
                }
                BlockState st = cell.state();
                BlockPos at = BlockPos.of(cell.pos());
                if (st.getBlock() instanceof BaseMultiblock mb) {
                    for (BlockPos part : mb.getMultiblockShape(level, at, st).getGlobalPositions()) {
                        ghosts.add(new ProjectPreviewPayload.Ghost(part.asLong(), Block.getId(st)));
                    }
                } else {
                    ghosts.add(new ProjectPreviewPayload.Ghost(cell.pos(), Block.getId(st)));
                }
            }
        }
        BlockPos eye = player.blockPosition();
        ghosts.sort(Comparator.comparingDouble(g -> BlockPos.of(g.pos()).distSqr(eye)));
        int max = AutarkiaConfig.COLONY_GHOST_MAX_BLOCKS.get();
        List<ProjectPreviewPayload.Ghost> cut = ghosts.size() > max ? new ArrayList<>(ghosts.subList(0, max)) : ghosts;
        List<ProjectPreviewPayload.SpanLine> spans = new ArrayList<>();
        if (p.state.open()) {
            for (Project.SpanRec s : p.spans) {
                if (!Builds.strung(level, s.a(), s.b())) {
                    spans.add(new ProjectPreviewPayload.SpanLine(s.a(), s.b(), (byte) s.tier().ordinal()));
                }
            }
        }
        return new ProjectPreviewPayload(p.id, color, cut, spans);
    }
}

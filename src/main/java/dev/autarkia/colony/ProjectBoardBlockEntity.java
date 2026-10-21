package dev.autarkia.colony;

import dev.autarkia.registry.AutarkiaBlockEntities;
import dev.autarkia.registry.AutarkiaBlockEntity;
import dev.autarkia.registry.JadeInfo;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/** The project board block entity: a colony board member and the Jade lines of 05b section 9.2. */
public class ProjectBoardBlockEntity extends AutarkiaBlockEntity implements ColonySensors.Member, JadeInfo {
    public ProjectBoardBlockEntity(BlockPos pos, BlockState state) {
        super(AutarkiaBlockEntities.PROJECT_BOARD.get(), pos, state);
    }

    @Override
    public Set<ColonySensors.Kind> colonyKinds() {
        return Set.of(ColonySensors.Kind.BOARD);
    }

    @Override
    public String jadeGroup() {
        return "colony";
    }

    @Override
    public void jadeLines(List<Component> out) {
        Colony c = level == null || Colonies.server() == null ? null : Colonies.at(level, worldPosition);
        if (c == null) {
            out.add(Component.translatable("jade.autarkia.board.unlinked"));
            return;
        }
        List<Project> proposed = c.projects.stream().filter(p -> p.state == Project.State.PROPOSED)
            .sorted(Comparator.comparingDouble((Project p) -> -p.priority)).toList();
        out.add(Component.translatable("jade.autarkia.board.proposals", proposed.size()));
        if (!proposed.isEmpty()) {
            Project top = proposed.get(0);
            out.add(Component.translatable("jade.autarkia.board.top", Component.translatable("colony_template.autarkia." + top.template.getPath()),
                Math.round(top.priority)));
        }
    }
}

package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.registry.AutarkiaRecipes;
import dev.autarkia.worker.WorkerAssemblerBlockEntity;
import dev.autarkia.worker.WorkerAssemblyRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Project completion (05 section 4): state DONE, template counters and cooldown, statistics, events and sound. A
 * finished {@code sentinel_post} orders a Sentinel (template 24): the frame and parts of the {@code worker_assembly}
 * recipe of {@code autarkia:sentinel} become standing deliveries to the domain's {@code worker_assembler} nearest the
 * post, frame first.
 */
public final class Projects {
    private Projects() {
    }

    /** Mark a project done. */
    public static void done(Colony c, ServerLevel level, Project p) {
        p.state = Project.State.DONE;
        String id = p.template.getPath();
        ColonyTemplate tpl = ColonyPlans.templates().get(id);
        Colony.TemplateState ts = c.template(id);
        ts.built++;
        ts.cooldownUntil = c.clock + (tpl == null ? 6000 : tpl.cooldown());
        c.stat("projects_done", 1);
        ColonyEvents.sound(c, "project_complete");
        ColonyEvents.worker(c, "job_complete:build_t" + (tpl == null ? 1 : tpl.tier()));
        if (p.auto && p.signed) {
            ColonyEvents.fire(c, "it_fixed_itself");
        }
        switch (id) {
            case "pattern_repair" -> {
                PatternRepair.restoreData(c, level, p);
                ColonyEvents.fire(c, "self_repair");
                ColonyEvents.worker(c, "repair_complete");
            }
            case "duplicate_line" -> {
                LineDuplication.finish(c, level, p);
                ColonyEvents.fire(c, "second_line");
            }
            case "fuse_box" -> ColonyEvents.worker(c, "fuse_installed");
            case "hv_link", "cable_car" -> OutpostProjects.finish(c, level, p);
            case "sentinel_post" -> orderSentinel(c, level, p);
            case Replication.TEMPLATE -> Replication.order(c, level, p);
            case "span_upgrade" -> coolant(c, p);
            case "restoration_projector" -> Restoration.planBuilt(c, level, p);
            default -> {
            }
        }
        if (p.fromMemory) {
            ColonyEvents.fire(c, "learned");
        }
        ColonyRuntime.of(c).log("Done " + id);
        Autarkia.LOG.info("[colony] {} finished {} at {}", c.name, id, p.origin.toShortString());
        Colonies.changed();
    }

    /** Order 4 {@code coolant_cell} into every {@code cryo_node} the project built (M8 decision G26). */
    private static void coolant(Colony c, Project p) {
        for (Project.Cell cell : p.cells) {
            if (cell.state().is(dev.autarkia.registry.AutarkiaBlocks.CRYO_NODE.get())) {
                c.deliveries.add(new Colony.Delivery(BlockPos.of(cell.pos()), dev.autarkia.registry.AutarkiaItems.COOLANT_CELL.get(), 4));
            }
        }
    }

    private static void orderSentinel(Colony c, ServerLevel level, Project p) {
        WorkerAssemblyRecipe r = level.recipeAccess().recipeMap().byType(AutarkiaRecipes.WORKER_ASSEMBLY.get()).stream().map(RecipeHolder::value)
            .filter(x -> x.worker().equals(Autarkia.id("sentinel"))).findFirst().orElse(null);
        BlockPos assembler = null;
        for (long packed : Colonies.chunks(c)) {
            net.minecraft.world.level.chunk.LevelChunk ch = level.getChunkSource().getChunkNow(net.minecraft.world.level.ChunkPos.getX(packed),
                net.minecraft.world.level.ChunkPos.getZ(packed));
            if (ch == null) {
                continue;
            }
            for (net.minecraft.world.level.block.entity.BlockEntity be : ch.getBlockEntities().values()) {
                if (be instanceof WorkerAssemblerBlockEntity a && a.isCenter() && Colonies.inside(c, be.getBlockPos())
                    && (assembler == null || be.getBlockPos().distSqr(p.origin) < assembler.distSqr(p.origin))) {
                    assembler = be.getBlockPos();
                }
            }
        }
        if (r == null || assembler == null) {
            ColonyRuntime.of(c).log("Sentinel order skipped: " + (r == null ? "no sentinel assembly recipe" : "no worker_assembler in the domain"));
            return;
        }
        for (SizedIngredient s : r.bill()) {
            BlockPos at = assembler;
            s.ingredient().items().findFirst().map(Holder::value).ifPresent(item -> c.deliveries.add(new Colony.Delivery(at, item, s.count())));
        }
        ColonyRuntime.of(c).log("Sentinel ordered at the assembler " + assembler.toShortString());
    }
}

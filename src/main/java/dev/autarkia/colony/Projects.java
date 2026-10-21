package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import net.minecraft.server.level.ServerLevel;

/** Project completion (05 section 4): state DONE, template counters and cooldown, statistics, events and sound. */
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
        ColonyRuntime.of(c).log("Done " + id);
        Autarkia.LOG.info("[colony] {} finished {} at {}", c.name, id, p.origin.toShortString());
        Colonies.changed();
    }
}

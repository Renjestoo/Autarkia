package dev.autarkia.colony;

import dev.autarkia.Autarkia;
import dev.autarkia.config.AutarkiaConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;

/**
 * The colony planner (05 sections 3.2, 3.6, 4 and 6.2): hold counters per sample, the planner pass (triggers,
 * limits, one site search at a time, proposal revalidation and expiry, the proposal cap), and the per-tick part (site
 * search steps, autonomy approvals, reservation and project state changes).
 */
public final class Planner {
    /** Template parameters from failure memory (05 section 6.2); not saved. */
    public record Params(double thresholdFactor, double priorityMul, double memoryWeight) {
        static final Params DEFAULT = new Params(1, 1, 0);
    }

    private static final Map<String, List<String>> CORRECTIVE = Map.of(
        "cable_snap", List.of("parallel_span", "span_upgrade"),
        "fuse_trip", List.of("span_upgrade"),
        "boiler_burst", List.of("pressure_valve"),
        "lightning_surge", List.of("lightning_arrester", "lightning_mast"),
        "power_deficit_long", List.of("windmill", "treadwheel", "waterwheel", "boiler_engine"),
        "storage_full_long", List.of("crate_cluster"));
    private static final List<String> GENERATORS = List.of("windmill", "treadwheel", "waterwheel", "boiler_engine");
    private static final List<String> STORAGE = List.of("crate_cluster");

    private Planner() {
    }

    /** Return the template parameters of a template from memories with weight at least 0.5. */
    public static Params params(Colony c, String template) {
        double half = AutarkiaConfig.MEMORY_HALF_LIFE_TICKS.get();
        double threshold = 1;
        double m = 0;
        double priorityMul = 1;
        for (Colony.Memory mem : c.memories) {
            double w = mem.weightAt(c.clock, half);
            if (w < 0.5) {
                continue;
            }
            if (CORRECTIVE.getOrDefault(mem.type(), List.of()).contains(template)) {
                m += w;
            }
            if (mem.type().equals("lightning_surge") && template.equals("lightning_mast")) {
                priorityMul = Math.max(priorityMul, 1 + 0.5 * w);
            }
            if (mem.type().equals("cable_snap") && template.equals("span_upgrade")) {
                threshold = Math.min(threshold, Math.max(0.6, 0.8 - 0.05 * w) / 0.8);
            }
            if (mem.type().equals("power_deficit_long") && GENERATORS.contains(template)) {
                threshold = Math.min(threshold, Math.max(0.6, 1 - 0.1 * w));
            }
            if (mem.type().equals("storage_full_long") && STORAGE.contains(template)) {
                threshold = Math.min(threshold, 0.75 / 0.85);
            }
        }
        return new Params(threshold, priorityMul, Math.min(4, m));
    }

    /**
     * Return the value of a signal or of a special source: {@code tier}, {@code site.<template>}, {@code mem.<type>} (the
     * highest current weight of that failure memory type) or {@code recent.<type>} (1 when such a memory was recorded or
     * merged in the last colony day, else 0).
     */
    public static double signal(Colony c, ServerLevel level, String id) {
        if (id.equals("tier")) {
            return c.tier;
        }
        if (id.startsWith("mem.") || id.startsWith("recent.")) {
            boolean recent = id.startsWith("recent.");
            String type = id.substring(recent ? 7 : 4);
            double half = AutarkiaConfig.MEMORY_HALF_LIFE_TICKS.get();
            double best = 0;
            for (Colony.Memory mem : c.memories) {
                if (mem.type().equals(type)) {
                    best = Math.max(best, recent ? (c.clock - mem.t0() <= ColonyClock.DAY ? 1 : 0) : mem.weightAt(c.clock, half));
                }
            }
            return best;
        }
        ColonyRuntime rt = ColonyRuntime.of(c);
        if (id.startsWith("site.")) {
            return rt.bestSite.getOrDefault(id.substring(5), 0.0);
        }
        return rt.signal(id, level.getGameTime(), 0);
    }

    private static boolean raw(Colony c, ServerLevel level, ColonyTemplate.Condition cond, Params p) {
        double x = signal(c, level, cond.signal());
        double t = cond.valueSignal().isPresent() ? cond.factor() * signal(c, level, cond.valueSignal().get()) : cond.value() * p.thresholdFactor();
        return cond.test(x, t);
    }

    /** Add one sample period to the hold counter of every true condition and reset false ones (05 section 2.1). */
    public static void updateHolds(Colony c, ServerLevel level, int period) {
        for (Map.Entry<String, ColonyTemplate> e : ColonyPlans.templates().entrySet()) {
            Colony.TemplateState ts = c.template(e.getKey());
            List<ColonyTemplate.Condition> all = e.getValue().allConditions();
            Params p = params(c, e.getKey());
            while (ts.holds.size() < all.size()) {
                ts.holds.add(0);
            }
            for (int i = 0; i < all.size(); i++) {
                ts.holds.set(i, raw(c, level, all.get(i), p) ? ts.holds.get(i) + period : 0);
            }
        }
    }

    /** Return whether the template trigger is true now with its holds. */
    public static boolean triggered(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        Colony.TemplateState ts = c.template(id);
        Params p = params(c, id);
        int k = 0;
        for (ColonyTemplate.Condition cond : tpl.triggers()) {
            if (!holds(c, level, cond, p, ts, k++)) {
                return false;
            }
        }
        if (tpl.any().isEmpty()) {
            return true;
        }
        boolean any = false;
        for (List<ColonyTemplate.Condition> group : tpl.any()) {
            boolean all = true;
            for (ColonyTemplate.Condition cond : group) {
                all &= holds(c, level, cond, p, ts, k++);
            }
            any |= all;
        }
        return any;
    }

    private static boolean holds(Colony c, ServerLevel level, ColonyTemplate.Condition cond, Params p, Colony.TemplateState ts, int k) {
        int held = k < ts.holds.size() ? ts.holds.get(k) : 0;
        return raw(c, level, cond, p) && held >= cond.hold();
    }

    /** Return the urgency U of the template from its first numeric trigger (05 section 3.3). */
    public static double urgency(Colony c, ServerLevel level, String id, ColonyTemplate tpl) {
        Params p = params(c, id);
        List<ColonyTemplate.Condition> conds = tpl.allConditions();
        for (ColonyTemplate.Condition cond : conds) {
            if (cond.valueSignal().isPresent() || cond.signal().equals("tier") || cond.signal().startsWith("site.")) {
                continue;
            }
            double x = signal(c, level, cond.signal());
            double t = cond.value() * p.thresholdFactor();
            if (t == 0) {
                return 0;
            }
            double ratio = cond.op().startsWith(">") ? x / t : t / Math.max(1e-6, x);
            return Math.max(0, Math.min(2, ratio - 1));
        }
        return p.memoryWeight() > 0 ? Math.min(2, 0.5 * p.memoryWeight()) : 0;
    }

    /** Return P = B (0.5 + U)(1 + 0.5 M) S / (1 + C / D) Tm + A (05 section 3.6). */
    public static double priority(Colony c, String id, ColonyTemplate tpl, Project pr) {
        Params p = params(c, id);
        double d = ColonyTiers.dailyBudget(c.tier);
        return tpl.base() * (0.5 + pr.urgency) * (1 + 0.5 * p.memoryWeight()) * pr.siteScore / (1 + pr.cost / d) * p.priorityMul() + pr.adjust;
    }

    /** Run one planner pass now. */
    public static void pass(Colony c, ServerLevel level) {
        ColonyRuntime rt = ColonyRuntime.of(c);
        ColonyMemory.prune(c);
        for (Map.Entry<String, ColonyTemplate> e : ColonyPlans.templates().entrySet()) {
            String id = e.getKey();
            ColonyTemplate tpl = e.getValue();
            Colony.TemplateState ts = c.template(id);
            if (tpl.tier() > c.tier || !ts.enabled || ts.cooldownUntil > c.clock || ts.noSiteUntil > c.clock) {
                continue;
            }
            if (!triggered(c, level, id, tpl)) {
                continue;
            }
            long open = c.projects.stream().filter(p -> p.state.open() && p.template.getPath().equals(id)).count();
            if (open >= tpl.maxOpen() || tpl.maxBuilt() > 0 && ts.built >= tpl.maxBuilt()) {
                continue;
            }
            if (tpl.handler().isPresent()) {
                Project pr = ProjectFactory.fromHandler(c, level, id, tpl);
                if (pr == null) {
                    ts.noSiteUntil = c.clock + 6000;
                    rt.log("No site: " + id);
                } else {
                    propose(c, pr);
                }
                continue;
            }
            if (rt.search == null) {
                ColonyPlans.Plan plan = ColonyPlans.plan(tpl.plan().orElseThrow());
                rt.search = SiteSearch.start(c, level, id, tpl, plan);
                if (rt.search == null) {
                    ts.noSiteUntil = c.clock + 6000;
                    rt.bestSite.put(id, 0.0);
                    rt.log("No site: " + id + " (no anchor)");
                }
            }
        }
        revalidate(c, level);
        Colonies.changed();
    }

    private static void revalidate(Colony c, ServerLevel level) {
        for (Project p : c.projects) {
            if (p.state != Project.State.PROPOSED) {
                continue;
            }
            ColonyTemplate tpl = ColonyPlans.templates().get(p.template.getPath());
            if (tpl == null) {
                p.state = Project.State.EXPIRED;
                continue;
            }
            if (triggered(c, level, p.template.getPath(), tpl)) {
                p.falseSince = -1;
                p.urgency = urgency(c, level, p.template.getPath(), tpl);
                p.priority = priority(c, p.template.getPath(), tpl, p);
            } else if (p.falseSince < 0) {
                p.falseSince = c.clock;
            } else if (c.clock - p.falseSince >= 12000) {
                p.state = Project.State.EXPIRED;
            }
            if (p.state == Project.State.PROPOSED && !ProjectFactory.siteStillFree(c, level, p)) {
                p.state = Project.State.EXPIRED;
                ColonyRuntime.of(c).log("Site blocked: " + p.template.getPath() + ", searching again");
            }
        }
        List<Project> proposed = new ArrayList<>(c.projects.stream().filter(p -> p.state == Project.State.PROPOSED).toList());
        proposed.sort(Comparator.comparingDouble((Project p) -> p.priority));
        int max = AutarkiaConfig.COLONY_MAX_OPEN_PROPOSALS.get();
        for (int i = 0; i < proposed.size() - max; i++) {
            proposed.get(i).state = Project.State.EXPIRED;
        }
        c.projects.removeIf(p -> !p.state.open() && c.clock - p.created > ColonyClock.DAY * 7);
    }

    /** Add a new proposal and fire the first-proposal event. */
    public static void propose(Colony c, Project pr) {
        c.projects.add(pr);
        ColonyRuntime.of(c).log("Proposed " + pr.template.getPath() + " P" + Math.round(pr.priority));
        ColonyEvents.fire(c, "first_proposal");
        ColonyEvents.sound(c, "proposal_ping");
        Autarkia.LOG.info("[colony] {} proposed {} at {} rot {} S {} C {} P {}", c.name, pr.template.getPath(), pr.origin.toShortString(),
            pr.rotation, String.format("%.2f", pr.siteScore), String.format("%.1f", pr.cost), String.format("%.1f", pr.priority));
        Colonies.changed();
    }

    /** Approve a proposal (player or autonomy); auto marks an autonomy approval that uses the budget. */
    public static void approve(Colony c, Project p, boolean auto) {
        if (p.state != Project.State.PROPOSED) {
            return;
        }
        p.state = Project.State.APPROVED;
        p.approved = c.clock;
        p.auto = auto;
        ColonyRuntime.of(c).log((auto ? "Auto-approved " : "Approved ") + p.template.getPath());
        Colonies.changed();
    }

    /** Deny a proposal: template cooldown 72,000 and the origin chunk denied for 7 days (05 section 4.2). */
    public static void deny(Colony c, Project p) {
        if (p.state != Project.State.PROPOSED) {
            return;
        }
        p.state = Project.State.DENIED;
        Colony.TemplateState ts = c.template(p.template.getPath());
        ts.cooldownUntil = c.clock + 72000;
        ts.deniedSites.add(new Colony.DeniedSite(net.minecraft.world.level.ChunkPos.pack(p.origin.getX() >> 4, p.origin.getZ() >> 4), c.clock + 7 * ColonyClock.DAY));
        Colonies.changed();
    }

    /** Cancel an approved project: open jobs go, held items return to storage, placed blocks stay. */
    public static void cancel(Colony c, Project p) {
        if (!p.state.open()) {
            return;
        }
        p.state = Project.State.CANCELLED;
        ColonyRuntime.of(c).jobs.dropProject(c, p);
        Colonies.changed();
    }

    /** Drop every job of every project and the running search (dormant colony); projects stay for a relinked Core. */
    public static void cancelAllJobs(Colony c) {
        ColonyRuntime rt = ColonyRuntime.of(c);
        for (Project p : c.projects) {
            rt.jobs.dropProject(c, p);
        }
        rt.search = null;
    }

    /** Advance the running site search, autonomy approvals and project starts. */
    public static void tick(Colony c, ServerLevel level) {
        ColonyRuntime rt = ColonyRuntime.of(c);
        if (rt.search != null) {
            rt.search.step(c, level, AutarkiaConfig.COLONY_SITE_EVALS_PER_TICK.get());
            if (rt.search.done()) {
                finishSearch(c, level, rt);
            }
        }
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        long delay = AutarkiaConfig.COLONY_ASSISTED_DELAY_TICKS.get();
        for (Project p : new ArrayList<>(c.projects)) {
            if (p.state == Project.State.PROPOSED) {
                boolean go = c.autonomy == Colony.Autonomy.AUTONOMOUS || c.autonomy == Colony.Autonomy.ASSISTED && c.clock - p.created >= delay;
                if (go) {
                    approve(c, p, true);
                }
            }
        }
        long building = c.projects.stream().filter(p -> p.state == Project.State.BUILDING || p.state == Project.State.BLOCKED
            || p.state == Project.State.COMMISSIONING).count();
        List<Project> waiting = new ArrayList<>(c.projects.stream().filter(p -> p.state == Project.State.APPROVED
            || p.state == Project.State.WAITING_ITEMS).toList());
        waiting.sort(Comparator.comparingDouble((Project p) -> -p.priority).thenComparingLong(p -> p.created));
        for (Project p : waiting) {
            if (building >= ColonyTiers.maxActiveProjects(c.tier)) {
                break;
            }
            if (p.auto && !ColonyClock.canAutoStart(c, p.cost)) {
                continue;
            }
            if (rt.jobs.ledger.reserve(c, level, rt, p)) {
                if (p.auto) {
                    ColonyClock.spend(c, p.cost);
                }
                p.state = Project.State.BUILDING;
                p.phase = 2;
                p.cursor = 0;
                rt.jobs.openPhase(c, level, p);
                building++;
                rt.log("Building " + p.template.getPath());
            } else if (p.state != Project.State.WAITING_ITEMS) {
                p.state = Project.State.WAITING_ITEMS;
                rt.log("Waiting for items: " + p.template.getPath());
            }
            Colonies.changed();
        }
    }

    private static void finishSearch(Colony c, ServerLevel level, ColonyRuntime rt) {
        SiteSearch s = rt.search;
        rt.search = null;
        SiteSearch.Result best = s.best();
        ColonyTemplate tpl = ColonyPlans.templates().get(s.template);
        Colony.TemplateState ts = c.template(s.template);
        rt.bestSite.put(s.template, best == null ? 0.0 : best.score());
        if (best == null || tpl == null) {
            ts.noSiteUntil = c.clock + 6000;
            rt.log("No site: " + s.template);
            Autarkia.LOG.info("[colony] {} found no site for {}", c.name, s.template);
            return;
        }
        Project pr = ProjectFactory.fromSite(c, level, s.template, tpl, best);
        propose(c, pr);
    }
}

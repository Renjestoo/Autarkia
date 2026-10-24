"""M3 content data: Colony Core, project board, planning tablet, worker assembler, winding post, Porter items, colony
templates and their structures, tags, advancements, entity loot and language."""

from datagen import (advancement, block_item, generated_item, geo_single, horizontal_states, item_def, item_model, loot_center_only,
                     loot_self, name, orientable, particle_model, rl, shaped, shapeless, simple_blockstate, structure, tag, write_json)

AXE = "minecraft:mineable/axe"
PICK = "minecraft:mineable/pickaxe"
NORTH = {"facing": "north"}
BIG = {"center": "true", "facing": "north"}


def blocks():
    particle_model("colony_core")
    simple_blockstate("colony_core")
    write_json("data/autarkia/loot_table/blocks/colony_core.json", {"type": "minecraft:block", "random_sequence": rl("blocks/colony_core"), "pools": []})
    tag("block", PICK, rl("colony_core"))
    name("block.autarkia.colony_core", "Colony Core")

    orientable("project_board", "project_board_front", "project_board_side", "project_board_side")
    horizontal_states("project_board")
    block_item("project_board")
    loot_self("project_board")
    tag("block", AXE, rl("project_board"))
    name("block.autarkia.project_board", "Project Board")
    shaped("project_board", ["PPP", "PBP", "PSP"], {"P": "#minecraft:planks", "B": "minecraft:book", "S": "minecraft:stick"})

    particle_model("worker_assembler")
    simple_blockstate("worker_assembler")
    generated_item("worker_assembler")
    loot_center_only("worker_assembler")
    tag("block", PICK, rl("worker_assembler"))
    name("block.autarkia.worker_assembler", "Worker Assembler")
    shaped("worker_assembler", ["BGB", "PCP", "BBB"], {"B": "bronze_block", "G": "bronze_gear", "P": "#minecraft:planks",
                                                       "C": "minecraft:crafting_table"})

    geo_single("winding_post")
    loot_self("winding_post")
    tag("block", AXE, rl("winding_post"))
    name("block.autarkia.winding_post", "Winding Post")
    shaped("winding_post", [" G ", "LML", "LLL"], {"G": "bronze_gear", "L": "#minecraft:logs", "M": "mainspring"})


def items():
    for iid, title in [("core_seed", "Core Seed"), ("planning_tablet", "Planning Tablet"), ("field_manual", "Field Manual"),
                       ("walker_frame", "Walker Frame")]:
        generated_item(iid)
        name(f"item.autarkia.{iid}", title)
    shaped("core_seed", ["SBS", "BGB", "SBS"], {"S": "minecraft:wheat_seeds", "B": "bronze_ingot", "G": "bronze_gear"})
    shaped("planning_tablet", ["BGB", "BRB", "BBB"], {"B": "bronze_ingot", "G": "minecraft:glass_pane", "R": "minecraft:redstone"},
           category="equipment")
    shapeless("field_manual", ["minecraft:book", "bronze_nugget"])
    shaped("walker_frame", ["BGB", "B B", "BGB"], {"B": "bronze_ingot", "G": "bronze_gear"})
    shaped("worker_capsule", ["BGB", "GMG", "BGB"], {"B": "bronze_ingot", "G": "minecraft:glass", "M": "mainspring"})
    item_model("worker_capsule", {"parent": "minecraft:item/generated", "textures": {"layer0": rl("item/worker_capsule")}})
    item_model("worker_capsule_filled", {"parent": "minecraft:item/generated", "textures": {"layer0": rl("item/worker_capsule_filled")}})
    item_def("worker_capsule", obj={"model": {"type": "minecraft:condition", "property": "minecraft:has_component",
                                              "component": "minecraft:entity_data",
                                              "on_true": {"type": "minecraft:model", "model": rl("item/worker_capsule_filled")},
                                              "on_false": {"type": "minecraft:model", "model": rl("item/worker_capsule")}}})
    name("item.autarkia.worker_capsule", "Worker Capsule")
    machine = {"type": rl("worker_assembly"), "worker": rl("porter"), "frame": {"ingredient": rl("walker_frame"), "count": 1},
               "parts": [{"ingredient": rl("mainspring"), "count": 2}, {"ingredient": rl("bronze_gear"), "count": 4},
                         {"ingredient": rl("bronze_ingot"), "count": 4}], "time": 600, "fe_per_tick": 20, "min_tier": 1}
    write_json("data/autarkia/recipe/worker_assembly/porter.json", machine)
    write_json("data/autarkia/loot_table/entities/porter.json", {"type": "minecraft:entity", "random_sequence": rl("entities/porter"), "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rl(i), "modifier": [{"type": "minecraft:set_count", "count": n, "add": False}]}]}
        for i, n in [("walker_frame", 1), ("mainspring", 1), ("bronze_gear", 2), ("bronze_ingot", 2)]]})
    name("entity.autarkia.porter", "Porter")


def tags():
    tag("block", "autarkia:planner_protected", "#minecraft:beds", "#minecraft:doors", "#minecraft:trapdoors", "#minecraft:fence_gates",
        "#minecraft:crops", "#minecraft:flower_pots", "#minecraft:all_signs", "#minecraft:banners", "#minecraft:rails", "#minecraft:candles",
        "#minecraft:wool_carpets", "minecraft:farmland", "minecraft:dirt_path", "minecraft:torch", "minecraft:wall_torch", "minecraft:lantern",
        "minecraft:glass", "minecraft:glass_pane")
    tag("block", "autarkia:worker_replaceable", "#minecraft:replaceable", "#minecraft:replaceable_by_trees", "#minecraft:small_flowers")
    tag("block", "autarkia:worker_unbreakable", "#minecraft:dragon_immune", "minecraft:spawner", "minecraft:budding_amethyst", rl("colony_core"))
    tag("entity_type", "autarkia:workers", rl("porter"))


def template(tid, index, title, triggers, site, base, max_built, reason, any_=None, plan=None, handler=None, sign=None):
    obj = {"index": index, "tier": 1, "triggers": triggers, "site": {"rule": site, "radius": 24}, "base": base, "max_built": max_built,
           "reason": reason}
    if any_:
        obj["any"] = any_
    if plan:
        obj["plan"] = rl(plan)
    if handler:
        obj["handler"] = handler
    if sign:
        obj["sign"] = sign
    write_json(f"data/autarkia/colony_template/{tid}.json", obj)
    name(f"colony_template.autarkia.{tid}", title)


def cond(signal, op, value=0.0, hold=0, value_signal=None, factor=1.0):
    c = {"signal": signal, "op": op, "value": value}
    if hold:
        c["hold"] = hold
    if value_signal:
        c["value_signal"] = value_signal
        c["factor"] = factor
    return c


def plan(pid, size, cells, anchor, commission, port=None, spans=None):
    structure(pid, size, cells)
    side = {"anchor": anchor, "commission": commission}
    if port:
        side["port"] = port
    if spans:
        side["spans"] = spans
    write_json(f"data/autarkia/colony_plan/{pid}.json", side)


def templates():
    power_short = {"key": "power_short", "signal": "S03", "scale": 100}
    template("windmill", 1, "Windmill", [cond("S03", ">=", 0.25, 1200)], "SR_HIGH_OPEN", 60, 8, power_short, plan="windmill", sign="power_short")
    template("treadwheel", 2, "Treadwheel", [cond("S03", ">=", 0.25, 1200)], "SR_NEAR_NET", 50, 4, power_short,
             any_=[[cond("site.windmill", "<", 0.4)], [cond("S17.porter", ">=", 0.5)]], plan="treadwheel", sign="power_short")
    template("crate_cluster", 3, "Crate Cluster", [cond("S09", ">=", 0.85, 600)], "SR_NEAR_STORAGE", 40, 16,
             {"key": "storage_full", "signal": "S09", "scale": 100}, plan="crate_cluster", sign="storage_full")
    template("flywheel", 4, "Flywheel", [cond("S05", "<", 30, 1200), cond("S01", ">", 0, 1200, value_signal="S02", factor=1.2)], "SR_NEAR_NET",
             35, 4, {"key": "store_low", "signal": "S05", "scale": 1}, plan="flywheel")
    template("parallel_span", 5, "Parallel Span", [cond("S06", ">=", 0.9, 600), cond("tier", "<", 2)], "SR_ON_SPAN", 45, 0,
             {"key": "span_load", "signal": "S06", "scale": 100}, handler="parallel_span", sign="line_snapped")
    template("winding_post", 6, "Winding Post", [], "SR_WORK_CENTER", 55, 4, {"key": "charger_far", "signal": "S19", "scale": 1},
             any_=[[cond("S18", ">=", 1, 1200)], [cond("S19", ">=", 24)]], plan="winding_post")
    plan("windmill", (1, 4, 3), [(0, 0, 0, "windmill", BIG), (0, 0, 2, "rope_pulley", {"facing": "up"})], [0, 0, 0], "runs", port=[0, 0, 2],
         spans=[{"a": [0, 0, 0], "b": [0, 0, 2], "tier": "rope"}])
    plan("treadwheel", (3, 3, 1), [(1, 0, 0, "treadwheel", BIG)], [1, 0, 0], "crank", port=[1, 0, 0])
    plan("flywheel", (3, 3, 1), [(1, 0, 0, "stone_flywheel", BIG)], [1, 0, 0], "runs", port=[1, 0, 0])
    plan("crate_cluster", (2, 2, 2), [(0, 0, 0, "crate", None), (1, 0, 0, "crate", None), (0, 0, 1, "crate", None), (1, 0, 1, "crate", None),
                                      (0, 1, 0, "tally_board", NORTH)], [0, 0, 0], "none")
    plan("winding_post", (1, 1, 1), [(0, 0, 0, "winding_post", NORTH)], [0, 0, 0], "none", port=[0, 0, 0])
    for k, v in {"power_short": "Power %s%% short", "storage_full": "Storage %s%% full", "store_low": "Stored power lasts %s s",
                 "span_load": "Rope span at %s%% load", "charger_far": "Workers %s blocks from a charger"}.items():
        name(f"colony.reason.autarkia.{k}", v)
    for k, v in {"power_short": "Power %s%% short.", "storage_full": "Storage %s%% full.", "line_snapped": "Line snapped on day %s.",
                 "rack_failed": "Rack failed on day %s.", "storm_hit": "Lightning hit on day %s.", "fixed": "Fixed.", "day": "Day %s"}.items():
        name(f"autarkia.sign.{k}", v)


def advancements():
    ev = lambda trigger, event: {"done": {"trigger": rl(trigger), "conditions": {"event": event}}}
    advancement("colony/root", "A Colony Takes Root", "Grow a Colony Core from a core seed", "core_seed", criteria=ev("colony_event", "root"),
                background="minecraft:gui/advancements/backgrounds/stone")
    advancement("colony/first_proposal", "It Has Ideas", "Get the first project proposal of your colony", "project_board", parent="colony/root",
                criteria=ev("colony_event", "first_proposal"))
    advancement("colony/it_fixed_itself", "It Fixed Itself", "A project your colony approved on its own is done and signed", "minecraft:oak_sign",
                parent="colony/first_proposal", criteria=ev("colony_event", "it_fixed_itself"), frame="goal")
    advancement("workers/first_porter", "Wind-Up Help", "Assemble a Porter", "walker_frame", parent="colony/root",
                criteria=ev("worker_event", "assembled:porter"))
    advancement("workers/self_built", "Fixed.", "Your workers finish a T1 build on their own", "worker_capsule", parent="workers/first_porter",
                criteria=ev("worker_event", "job_complete:build_t1"))


def lang():
    states = ["proposed", "approved", "waiting_items", "building", "blocked", "commissioning", "done", "denied", "expired", "cancelled"]
    for s in states:
        name(f"colony.state.autarkia.{s}", s.replace("_", " ").capitalize())
    for a, t in [("manual", "Manual"), ("assisted", "Assisted"), ("autonomous", "Autonomous")]:
        name(f"colony.autonomy.autarkia.{a}", t)
    screen = {"title": "%s (T%s)", "domain": "Domain %s x %s", "supply": "Power made: %s FE/t", "demand": "Power used: %s FE/t",
              "storage": "Storage %s%% full", "workers": "Workers %s / %s", "autonomy": "Autonomy: %s", "budget": "Budget %s of %s (day %s)",
              "graph_supply": "Made", "graph_demand": "Used", "graph_samples": "Last %s samples", "name": "Colony name", "rename": "Rename",
              "no_projects": "No projects yet. The colony proposes work when its sensors see a need.", "site": "Site %s %s %s, %s, score %s, %s edits",
              "cost": "Cost %s, budget left %s", "priority": "Priority %s (adjust %s)", "blocked": "Blocked: %s", "bom": "Bill of materials",
              "bom_line": "%s: need %s, stock %s, reserved %s", "labor": "Labor: %s", "show": "Show", "approve": "Approve", "deny": "Deny",
              "rotate": "Rotate", "retry": "Retry", "cancel": "Cancel", "recall": "Recall", "none": "None", "jobs_header": "Open jobs",
              "workers_header": "Workers", "job_row": "%s at %s %s %s by %s, try %s, %s", "worker_row": "%s: %s, job %s",
              "settings_title": "Settings", "signs_on": "Signs: on", "signs_off": "Signs: off", "stock_item": "Item id", "stock_count": "Count",
              "stock_set": "Set", "trust_name": "Player name", "trust": "Trust"}
    for k, v in screen.items():
        name(f"screen.autarkia.colony.{k}", v)
    for k, v in {"proposed": "Proposed %s (priority %s)", "no_site": "No site found for %s", "no_anchor": "No anchor block for %s",
                 "building": "Building %s", "waiting_items": "Waiting for items: %s",
                 "rack_critical": "Rack critical at %s: %s%%, collapse in %s s"}.items():
        name(f"colony.log.autarkia.{k}", v)
    for t, v in [("overview", "Overview"), ("projects", "Projects"), ("jobs", "Jobs"), ("workers", "Workers"), ("settings", "Settings")]:
        name(f"screen.autarkia.colony.tab.{t}", v)
    for m, v in [("colony_core", "Colony Core"), ("project_board", "Project Board"), ("planning_tablet", "Planning Tablet")]:
        name(f"menu.autarkia.{m}", v)
    seed = {"top_face": "Use the seed on the top face of a block", "ground_type": "The seed needs dirt or stone below",
            "ground": "The 3 x 3 ground below must be solid", "blocked": "The 3 x 3 x 3 space above must be free",
            "inside_domain": "This is inside another colony's domain", "near_domain": "Too close to another colony's domain",
            "too_many": "You already lead the maximum number of colonies", "lost_link": "This seed's colony no longer exists",
            "link_active": "This seed's colony is still active"}
    for k, v in seed.items():
        name(f"message.autarkia.seed.{k}", v)
    msgs = {"colony.growing": "The Core is growing: %s%%", "colony.no_rights": "Only the owner or trusted players can do this",
            "colony.outside": "Place it inside your colony's domain", "colony.protected": "This block belongs to %s",
            "colony.rotate_failed": "No valid site for that rotation", "colony.blocked": "Project %s blocked: %s",
            "tablet.unlinked": "Use the tablet on a Colony Core to link it", "worker.stuck": "A worker is stuck at %s",
            "capsule.not_yours": "This worker belongs to another colony", "capsule.busy": "The worker must be idle, recharging or failed",
            "capsule.outside": "Outside your domain", "capsule.cap_full": "Worker cap full"}
    for k, v in msgs.items():
        name(f"message.autarkia.{k}", v)
    name("tooltip.autarkia.core_seed", "Use on dirt or stone to grow a Colony Core")
    name("tooltip.autarkia.core_seed.linked", "Restores a dormant colony")
    name("tooltip.autarkia.capsule.empty", "Sneak + use on your idle worker to store it")
    name("tooltip.autarkia.capsule.filled", "%s, spring %s%%")
    for k, v in {"no_colony": "No colony here", "no_project": "No such project", "no_template": "No such template",
                 "no_worker": "No worker within 16 blocks"}.items():
        name(f"command.autarkia.{k}", v)
    jade = {"colony.growing": "Growing: %s%%", "colony.name": "Colony %s T%s", "colony.power": "Domain grid: %s FE/t made, %s FE/t used",
            "colony.projects": "Projects %s active, %s proposed", "colony.workers": "Workers %s / %s", "board.proposals": "Proposals %s",
            "board.top": "Top: %s P%s", "board.unlinked": "Not inside a colony domain", "post.free": "Free", "post.winding": "Winding a worker",
            "post.docked": "Worker docked", "assembler.progress": "Progress %s%%", "assembler.no_parts": "Waiting for parts",
            "assembler.running": "Assembling", "assembler.no_power": "No power", "assembler.cap_full": "Worker cap full",
            "assembler.tier_low": "Core tier too low", "assembler.outside": "Outside a colony domain", "worker.unlinked": "Unlinked",
            "worker.failed": "Failed: %s", "worker.no_job": "Job: none", "worker.job": "Job: %s (%s/%s)", "worker.spring": "Spring %s / %s T"}
    for k, v in jade.items():
        name(f"jade.autarkia.{k}", v)
    for s in ["idle", "assigned", "fetching", "travelling", "working", "returning", "recharging"]:
        name(f"jade.autarkia.worker.state.{s}", s.capitalize())
    for r in ["no_energy", "no_path", "item_missing", "target_obstructed", "worker_lost", "chunk_unloaded", "no_power", "timeout"]:
        name(f"jade.autarkia.worker.reason.{r}", r.replace("_", " "))
    for j in ["fetch", "carry", "deliver", "level_cut", "level_fill", "place", "string_cable", "crank", "sign"]:
        name(f"jade.autarkia.worker.job.{j}", j.replace("_", " ").capitalize())
    for g, v in [("colony", "Autarkia colony"), ("worker_state", "Worker state"), ("worker_job", "Worker job"),
                 ("worker_energy", "Worker spring"), ("worker_cargo", "Worker cargo")]:
        name(f"config.jade.plugin_autarkia.{g}", v)
    info = {
        "core_seed": "Use on the top of dirt or stone with a free 3 x 3 x 3 space. The Core grows for 10 s, then claims a 33 x 33 domain, gives a Porter capsule and a field manual.",
        "project_board": "Place inside a domain: shows the colony's proposals with their reason, site, cost and bill of materials. Approve, deny or show the ghost.",
        "planning_tablet": "Use on a Colony Core to link. While held, the colony's proposals show as ghost blocks. Use to open the project list and the autonomy switch.",
        "worker_assembler": "2 x 2 x 2. Put a frame and the parts in, give it FE: a Porter needs a walker frame, 2 mainsprings, 4 bronze gears and 4 bronze ingots, 600 ticks at 20 FE/t.",
        "winding_post": "Charger for spring workers: 40 T/t for 80 FE/t from a cable, or 10 T/t while a player holds use on it.",
        "walker_frame": "The frame of a Porter or Lineman.",
        "worker_capsule": "Sneak + use on your idle worker to store it with its spring, cargo and upgrades. Use on the ground inside your domain to release it.",
        "field_manual": "The Autarkia guide: tiers, machines, the colony and its workers.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def generate():
    blocks()
    items()
    tags()
    templates()
    advancements()
    lang()

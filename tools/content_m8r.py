"""M8 content data, Replicator part (05 section 8.2, 06): the replicator_foundry block, the Replicator Walker (worker_assembly
recipe at the foundry, entity loot), templates 27 (replicator_walker) and 28 (matter_buffer) with the colony/matter_buffer
plan, the colony and worker advancements of W19, Jade and JEI text and the rising_block name."""

from content_m3 import BIG, cond, plan
from content_m6 import geo_block
from content_m6c import ev
from content_m7w import assembly
from datagen import advancement, name, rl, shaped, write_json

REPLICATOR_PARTS = [("ai_core", 1), ("nanite_paste", 8), ("lattice_alloy_ingot", 16), ("advanced_circuit", 4), ("servo", 8), ("outpost_seed", 1)]
STATES = {"idle": "No print ordered", "no_parts": "Waiting for parts", "wait_matter": "Waiting for kit matter",
          "running": "Printing", "no_power": "No power", "cap_full": "Walker cap full", "tier_low": "Needs a tier 5 colony",
          "outside": "Outside a colony domain", "disabled": "Replication is switched off"}


def template(tid, index, tier, title, site, base, max_built, reason, triggers=None, any_=None, plan_=None, handler=None, sign=None):
    obj = {"index": index, "tier": tier, "triggers": triggers or [], "site": {"rule": site, "radius": 24}, "base": base, "max_built": max_built,
           "reason": reason}
    if any_:
        obj["any"] = any_
    if plan_:
        obj["plan"] = rl(plan_)
    if handler:
        obj["handler"] = handler
    if sign:
        obj["sign"] = sign
    write_json(f"data/autarkia/colony_template/{tid}.json", obj)
    name(f"colony_template.autarkia.{tid}", title)


def foundry():
    geo_block("replicator_foundry", "Replicator Foundry")
    shaped("replicator_foundry", ["LAL", "SXS", "FFF"], {"L": "lattice_alloy_block", "A": "ai_core", "S": "servo", "X": "worker_assembler",
                                                         "F": "foundation"})
    assembly("replicator_walker", "walker_frame", 2, REPLICATOR_PARTS, 2400, 2000, 5)
    write_json("data/autarkia/recipe/worker_assembly/replicator_walker.json", {
        "type": rl("worker_assembly"), "worker": rl("replicator_walker"), "machine": rl("replicator_foundry"),
        "frame": {"ingredient": rl("walker_frame"), "count": 2}, "parts": [{"ingredient": rl(i), "count": n} for i, n in REPLICATOR_PARTS],
        "time": 2400, "fe_per_tick": 2000, "min_tier": 5})
    name("entity.autarkia.replicator_walker", "Replicator Walker")
    name("entity.autarkia.rising_block", "Rising Block")
    for k, v in STATES.items():
        name(f"jade.autarkia.replicator.{k}", v)
    name("jade.autarkia.replicator.walkers", "Walkers %s/%s")
    name("jade.autarkia.replicator.print", "Print %s%%")
    name("message.autarkia.capsule.no_capture", "This worker cannot be captured")
    name("jei.autarkia.info.replicator_foundry", "T5, 3 x 3 x 3. Prints a Replicator Walker for the colony: 2 walker frames, 1 AI core, 8 nanite "
         "paste, 16 lattice alloy ingots, 4 advanced circuits, 8 servos and 1 outpost seed, plus the outpost kit as matter units from "
         "the matter buffers; 2,400 ticks at 2,000 FE/t. The walker walks to an unclaimed deposit up to 512 blocks away, digs in and "
         "becomes an outpost. At most 2 walkers at a time, one print per colony day.")
    print("  replicator_foundry block, recipe; replicator_walker assembly at the foundry, loot")


def templates():
    template("replicator_walker", 27, 5, "Replicator Walker", "foundry", 50, 0, {"key": "replicator_ready", "signal": "replicator.active", "scale": 1},
             triggers=[cond("replicator.ready", ">=", 1)], handler="replicator")
    template("matter_buffer", 28, 5, "Matter Buffer", "SR_NEAR_STORAGE", 45, 4, {"key": "storage_full", "signal": "S09", "scale": 100},
             any_=[[cond("S09", ">=", 0.9, 2400)], [cond("S27", ">=", 0.9)]], plan_="matter_buffer", sign="storage_full")
    plan("matter_buffer", (2, 2, 2), [(0, 0, 0, "matter_buffer", BIG)], [0, 0, 0], "none")
    name("colony.reason.autarkia.replicator_ready", "%s walkers on their way; an unclaimed deposit waits and the foundry has its bill")
    print("  templates 27 replicator_walker, 28 matter_buffer; plan colony/matter_buffer")


def advancements():
    advancement("colony/not_built_by_you", "Not Built By You", "A Replicator Walker founds an outpost", "replicator_foundry", parent="colony/frontier",
                criteria=ev("colony_event", "walker_outpost_founded"), frame="goal")
    advancement("workers/replicator_deploy", "A Base You Did Not Build", "A Replicator Walker digs in and becomes an outpost", "replicator_foundry",
                parent="workers/pylon_line", criteria=ev("worker_event", "replicator_deploy"), frame="challenge")
    print("  advancements colony/not_built_by_you, workers/replicator_deploy")


def generate():
    print("content_m8r: Replicator")
    foundry()
    templates()
    advancements()
    print("content_m8r: done")

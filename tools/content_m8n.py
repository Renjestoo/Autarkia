"""M8 content data, nanite part (05 section 8.3, 05b sections 9 and 11, 06 sections 1 and 5 Nanite Swarm): the nanite_hive
and claim_marker models, items, loot, tags and recipes, the nanite_swarm worker_assembly recipe (G23), the
nanite_immune block tag, the swarm entity name and tag, Jade, screen, message and state language, JEI pages and the
advancements colony/clean_pit and workers/claim_done (G28)."""

from content_m6c import ev
from datagen import (advancement, block_model, generated_item, item_def, item_model, loot_center_only, loot_self, name, particle_model, rl, shaped,
                     simple_blockstate, tag, write_json)

PICK = "minecraft:mineable/pickaxe"
STATES = {"run": "Running", "wait_chunk": "Waiting for chunks", "wait_buffer": "Waiting for the matter buffer", "wait_fe": "Waiting for FE",
          "paused": "Paused", "done": "Done"}
PHASES = {"release": "Release", "travel": "Travel", "disassemble": "Disassemble", "return": "Return", "dissolve": "Dissolve"}


def blocks():
    particle_model("nanite_hive")
    simple_blockstate("nanite_hive")
    generated_item("nanite_hive")
    loot_center_only("nanite_hive")
    tag("block", PICK, rl("nanite_hive"))
    tag("block", "minecraft:needs_iron_tool", rl("nanite_hive"))
    tag("block", "autarkia:worker_unbreakable", rl("nanite_hive"))
    shaped("nanite_hive", ["LNL", "AXA", "LFL"], {"L": "lattice_alloy_ingot", "N": "nanite_paste", "A": "advanced_circuit", "X": "minecraft:beehive",
                                                  "F": "foundation"})
    block_model("claim_marker", {"parent": "minecraft:block/cross", "textures": {"cross": rl("block/claim_marker")}})
    simple_blockstate("claim_marker")
    item_model("claim_marker", {"parent": "minecraft:item/generated", "textures": {"layer0": rl("block/claim_marker")}})
    item_def("claim_marker")
    loot_self("claim_marker")
    tag("block", PICK, rl("claim_marker"))
    shaped("claim_marker", [" G ", "LSL"], {"G": "minecraft:glowstone_dust", "L": "lattice_shard", "S": "steel_ingot"}, 4)
    tag("block", "autarkia:nanite_immune", "minecraft:reinforced_deepslate")
    name("block.autarkia.nanite_hive", "Nanite Hive")
    name("block.autarkia.claim_marker", "Claim Marker")
    name("menu.autarkia.nanite_hive", "Nanite Hive")
    print("  blocks nanite_hive (2x2x2 GeckoLib, center-only loot), claim_marker (cross model, x4 recipe), tag nanite_immune")


def swarm():
    write_json("data/autarkia/recipe/worker_assembly/nanite_swarm.json", {
        "type": rl("worker_assembly"), "worker": rl("nanite_swarm"), "machine": rl("nanite_hive"),
        "parts": [{"ingredient": rl("nanite_paste"), "count": 4}], "time": 200, "fe_per_tick": 100, "min_tier": 5})
    tag("entity_type", "autarkia:workers", rl("nanite_swarm"))
    name("entity.autarkia.nanite_swarm", "Nanite Swarm")
    print("  nanite_swarm: worker_assembly in nanite_hive (4 nanite_paste, 200 ticks, 100 FE/t, T5), workers tag")


def lang():
    for k, v in STATES.items():
        name(f"nanite.autarkia.state.{k}", v)
    for k, v in PHASES.items():
        name(f"nanite.autarkia.phase.{k}", v)
    for k, v in {"claim": "Claim %s%%, layer %s/%s", "state": "State %s", "idle": "No claim"}.items():
        name(f"jade.autarkia.nanite_hive.{k}", v)
    for k, v in {"integrity": "Integrity %s/%s", "state": "State %s", "buffer": "Buffer %s/%s FE, %s/%s items"}.items():
        name(f"jade.autarkia.nanite_swarm.{k}", v)
    for k, v in {"start": "Start", "stop": "Stop", "terrace_on": "Terrace on", "terrace_off": "Terrace off", "pair": "%s x %s at %s, %s (%s high)",
                 "needs_paste": "Needs 4 paste", "cap_full": "Worker cap full", "no_recipe": "No swarm recipe", "assembling": "Assembling %s%%", "swarms": "Swarms out: %s",
                 "no_claim": "No claim", "state": "%s: %s%%, layer %s/%s", "stopped": "Nanites stopped (server)",
                 "no_buffer": "No matter buffer linked", "height": "Height %s/%s", "pick": "Pick a marker pair",
                 "no_colony": "Outside a colony domain"}.items():
        name(f"screen.autarkia.nanite_hive.{k}", v)
    for k, v in {"markers": "Both claim markers must stand where the pair was listed",
                 "range": "The claim box must lie within 64 blocks of the hive",
                 "size": "The claim box is wider than nanite.maxClaimXZ",
                 "height": "The claim box is higher than nanite.maxClaimY",
                 "domain": "The claim box must lie fully inside your colony domain",
                 "overlap": "The claim box overlaps another claim, a watched Pattern or an open project",
                 "no_hive": "No nanite hive of this colony here", "busy": "This hive already runs a claim",
                 "tier": "Nanite claims need a tier 5 colony", "started": "Claim started", "done": "Claim %s to %s is done"}.items():
        name(f"message.autarkia.nanite.{k}", v)
    info = {
        "nanite_hive": "2 x 2 x 2, FE buffer 200,000 (8,000 FE/t in). Lists marker pairs within 64 blocks; pick one, set the depth below the lower "
                       "marker (height up to 48) and terraces, press Start. Assembles a Nanite Swarm from 4 nanite paste (200 ticks, 100 FE/t), "
                       "tethers it with up to 4,000 FE/t and takes the box apart top down: ores as items, other blocks as free matter into the "
                       "linked matter buffer. Containers and machines stay.",
        "claim_marker": "Place two to mark the corners of a nanite claim box. Glows faintly, no collision.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def advancements():
    advancement("colony/clean_pit", "Clean Cut Pit", "Your colony finishes its first nanite claim", "claim_marker", parent="colony/frontier",
                criteria=ev("colony_event", "claim_finished"), frame="goal")
    advancement("workers/claim_done", "Clean Pit", "A Nanite Swarm takes a claim apart to the last layer", "nanite_hive", parent="workers/pylon_line",
                criteria=ev("worker_event", "claim_complete"))
    print("  advancements colony/clean_pit, workers/claim_done")


def generate():
    print("content_m8n: nanites")
    blocks()
    swarm()
    lang()
    advancements()
    print("content_m8n: done")

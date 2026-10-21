"""M9 content data, seed part (05 sections 1.2 and 8.4, 06 section 7.3): core_upgrade_t6 (fabricating, G8), the seed_matrix
block and recipe, the autarky_seed item, the thrown seed name, the advancements colony/ascendant, colony/instant_base and
workers/full_cap, and the Jade, JEI, screen, tooltip and message texts of the seed."""

from content_m6c import ev
from content_m8b import geo_block
from datagen import advancement, generated_item, machine_recipe, name, shaped

MATRIX_STATES = {
    "ready": "Ready to pack", "no_cards": "Insert recorded pattern cards", "no_pattern": "A card holds no recorded Pattern",
    "not_outpost": "Card 1 must be an OUTPOST Pattern with an outpost core", "overlap": "Two footprints overlap",
    "too_big": "Footprint over 96 x 48 x 96", "no_fit": "Footprint leaves the outpost square",
    "outside": "Outside a colony domain", "tier_low": "Needs a tier 6 colony", "output_full": "Take the seed out first",
    "missing_items": "Colony storage lacks pack items", "running": "Packing", "no_power": "No power", "no_matter": "Waiting for matter",
}
SEED_JADE = {
    "paused": "Seed unpack paused", "ring": "Seed: glow ring", "leveling": "Seed: leveling, %s edits",
    "matter": "Seed: leveling waits for matter (%s edits)", "rising": "Seed: rising %s/%s", "linking": "Seed: connecting spans",
}
MESSAGES = {
    "empty": "This seed holds no Patterns", "disabled": "Seed unpacking is switched off (seed.enabled)",
    "tier": "An Autarky Seed needs an active tier 6 colony", "patterns": "A Pattern of this seed is missing on this server",
    "layout": "The seed layout does not fit this colony's outpost square",
    "chunks": "The seed footprint is not fully loaded", "block_entity": "A block entity stands in the seed footprint",
    "fluid": "Fluid covers half or more of the seed footprint", "protected": "Protection refuses the outpost core space",
    "failed": "The seed drops: %s",
}


def items():
    for iid, title in (("core_upgrade_t6", "Core Upgrade T6"), ("autarky_seed", "Autarky Seed")):
        generated_item(iid)
        name(f"item.autarkia.{iid}", title)
    machine_recipe("fabricating", "core_upgrade_t6", [("aegis_plate", 2), ("ai_core", 2), ("lattice_crystal", 8)], [("core_upgrade_t6", 1)],
                   time=200, energy=160)
    name("entity.autarkia.thrown_autarky_seed", "Thrown Autarky Seed")
    print("  core_upgrade_t6 (fabricating 200 ticks x 160 FE/t = 32,000 FE), autarky_seed, thrown_autarky_seed")


def matrix():
    geo_block("seed_matrix", "Seed Matrix")
    shaped("seed_matrix", ["PAP", "NXN", "PFP"], {"P": "aegis_plate", "A": "ai_core", "N": "nanite_paste", "X": "matter_buffer", "F": "foundation"})
    name("menu.autarkia.seed_matrix", "Seed Matrix")
    for k, v in MATRIX_STATES.items():
        name(f"jade.autarkia.seed_matrix.{k}", v)
    name("jade.autarkia.seed_matrix.patterns", "Patterns %s/%s")
    name("jade.autarkia.seed_matrix.matter", "Matter %s%%")
    name("jade.autarkia.seed_matrix.lacks", "Lacks %s")
    name("screen.autarkia.seed_matrix.pack", "Pack")
    name("screen.autarkia.seed_matrix.size", "%s x %s x %s, %s blocks")
    name("screen.autarkia.seed_matrix.progress", "Pack %s%%, matter %s%%, power buffer %s%%")
    print("  seed_matrix block, recipe PAP/NXN/PFP, menu, Jade and screen texts")


def seed_texts():
    for k, v in SEED_JADE.items():
        name(f"jade.autarkia.seed.{k}", v)
    for k, v in MESSAGES.items():
        name(f"message.autarkia.seed.{k}", v)
    name("tooltip.autarkia.autarky_seed.empty", "Empty seed")
    name("tooltip.autarkia.autarky_seed.contents", "%s Patterns, %s x %s x %s, %s blocks")
    name("tooltip.autarkia.autarky_seed.crew", "%s workers, %s FE charge")
    info = {
        "seed_matrix": "T6, 3 x 3 x 3. Packs 1 to 8 recorded pattern cards into an Autarky Seed: card 1 must be an OUTPOST Pattern; set x, z "
                       "and rotation per card, no footprints may overlap, and the whole footprint must lie in the outpost square (65 x 65 at "
                       "T6) around the outpost core. Pack takes 1 AI core, 16 nanite paste and the parts of 2 Porters and 2 Builder Drones "
                       "from colony storage, the Pattern bills as matter units from the matter buffers (20 parts), and 1,000,000 FE at "
                       "834 FE/t over 1,200 ticks. The cards stay in the matrix.",
        "autarky_seed": "Made by the Seed Matrix only. Throw it like an ender pearl onto open ground of a T6 colony: it needs a free outpost "
                        "slot, 64 to 512 blocks from the Core, 64 from other outposts, a loaded footprint without block entities and with "
                        "less than half fluid. A glow ring spreads, the ground levels, the blocks rise from the soil, spans connect and the "
                        "crew unfolds, in about 1,100 ticks. A failed landing drops the seed with a message.",
        "core_upgrade_t6": "Use on an active tier 5 Core (owner or trusted): after 100 ticks the colony is tier 6. Domain half-size 96, "
                           "worker cap 32, 8 projects, 6 domain beacons of half-size 24, 6 outposts.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)
    print(f"  {len(SEED_JADE)} seed Jade lines, {len(MESSAGES)} seed messages, tooltips, 3 JEI pages")


def advancements():
    advancement("colony/ascendant", "Ascendant", "Raise the Core to tier 6", "core_upgrade_t6", parent="colony/not_built_by_you",
                criteria=ev("colony_event", "tier_6"), frame="goal")
    advancement("colony/instant_base", "Instant Base", "An Autarky Seed unpacks into a powered base", "autarky_seed",
                parent="colony/not_built_by_you", criteria=ev("colony_event", "seed_unpacked"), frame="challenge")
    advancement("workers/full_cap", "Autarky Workforce", "The workers of a tier 6 colony fill its whole worker cap", "drone_frame",
                parent="workers/replicator_deploy", criteria=ev("worker_event", "cap_full:32"), frame="goal")
    print("  advancements colony/ascendant, colony/instant_base, workers/full_cap")


def generate():
    print("content_m9s: seed")
    items()
    matrix()
    seed_texts()
    advancements()
    print("content_m9s: done")

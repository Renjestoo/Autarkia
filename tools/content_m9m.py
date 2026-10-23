"""M9 content data, materials and logistics part (03, 04, T6): aegis_plate, the matter compiler with its 21 compiling
recipes (03 section 6, M9 C6), the matter conduit and the domain archive, with models, block states, loot, tags,
recipes (03 s5.6, 04 s1.9, s2.9), the advancements materials/compiler, materials/aegis and logistics/everywhere
(M9 G12), JEI and Jade texts, screen texts and language."""

from content_m6 import geo_block
from datagen import (advancement, block_item, block_model, blockstate, cube_all, generated_item, horizontal_states, inv_changed, loot_self,
                     machine_recipe, name, orientable, rl, shaped, tag)

PICK = "minecraft:mineable/pickaxe"
IRON_TOOL = "minecraft:needs_iron_tool"
COMPILING = {
    "copper_ingot": ("minecraft:copper_ingot", 27), "tin_ingot": ("autarkia:tin_ingot", 27), "zinc_ingot": ("autarkia:zinc_ingot", 27),
    "bronze_ingot": ("autarkia:bronze_ingot", 27), "brass_ingot": ("autarkia:brass_ingot", 27), "iron_ingot": ("minecraft:iron_ingot", 36),
    "steel_ingot": ("autarkia:steel_ingot", 54), "gold_ingot": ("minecraft:gold_ingot", 108), "vantite_ingot": ("autarkia:vantite_ingot", 270),
    "vantite_steel_ingot": ("autarkia:vantite_steel_ingot", 324), "netherite_ingot": ("minecraft:netherite_ingot", 4096),
    "iron_nugget": ("minecraft:iron_nugget", 4), "gold_nugget": ("minecraft:gold_nugget", 12), "copper_nugget": ("minecraft:copper_nugget", 3),
    "tin_nugget": ("autarkia:tin_nugget", 3), "zinc_nugget": ("autarkia:zinc_nugget", 3), "bronze_nugget": ("autarkia:bronze_nugget", 3),
    "steel_nugget": ("autarkia:steel_nugget", 6), "lattice_crystal": ("autarkia:lattice_crystal", 864),
    "lattice_alloy_ingot": ("autarkia:lattice_alloy_ingot", 1188), "aegis_plate": ("autarkia:aegis_plate", 32768),
}


def compiler():
    geo_block("matter_compiler", "Matter Compiler")
    name("menu.autarkia.matter_compiler", "Matter Compiler")
    shaped("matter_compiler", ["LAL", "NXN", "LBL"], {"L": "lattice_alloy_ingot", "A": "ai_core", "N": "nanite_paste", "X": "minecraft:nether_star",
                                                      "B": "lattice_alloy_block"})
    for rid, (result, mu) in COMPILING.items():
        machine_recipe("compiling", rid, [], [(result, 1)], matter=mu)
    generated_item("aegis_plate")
    name("item.autarkia.aegis_plate", "Aegis Plate")
    jade = {"matter": "Matter: %s / %s MU", "target": "Target: %s (%s MU)", "no_target": "Target: none"}
    for k, v in jade.items():
        name(f"jade.autarkia.matter_compiler.{k}", v)
    screen = {"clear": "Clear target", "matter": "Matter: %s / %s MU", "none": "No target", "target": "Target: %s (%s MU, %s s)",
              "hint": "Click with an item to target it; click or scroll to cycle"}
    for k, v in screen.items():
        name(f"screen.autarkia.matter_compiler.{k}", v)
    name("jei.autarkia.category.compiling", "Matter compiling")
    name("jei.autarkia.matter", "%s MU")


def conduit():
    cube_all("matter_conduit")
    block_model("matter_conduit_terminal", {"textures": {"particle": rl("block/matter_conduit")}})
    rot = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "east": {"x": 90, "y": 90},
           "west": {"x": 90, "y": 270}}
    blockstate("matter_conduit", {"multipart": [{"when": {"facing": d}, "apply": {"model": rl("block/matter_conduit"), **r}} for d, r in rot.items()]
                                  + [{"apply": {"model": rl("block/matter_conduit_terminal")}}]})
    block_item("matter_conduit")
    loot_self("matter_conduit")
    tag("block", PICK, rl("matter_conduit"))
    tag("block", IRON_TOOL, rl("matter_conduit"))
    name("block.autarkia.matter_conduit", "Matter Conduit")
    name("menu.autarkia.matter_conduit", "Matter Conduit")
    shaped("matter_conduit", ["LNL", "NAN", "LNL"], {"L": "lattice_alloy_ingot", "N": "nanite_paste", "A": "aegis_plate"}, count=4)
    jade = {"outside": "Outside domain", "mode": "Mode: %s", "mode.send": "Send", "mode.receive": "Receive", "channel": "Channel %s: %s",
            "rate": "Items: %s/s"}
    for k, v in jade.items():
        name(f"jade.autarkia.matter_conduit.{k}", v)
    screen = {"mode": "Mode: %s", "face": "Output: %s", "channel": "Ch %s", "face.down": "Down", "face.up": "Up", "face.north": "North",
              "face.south": "South", "face.west": "West", "face.east": "East"}
    for k, v in screen.items():
        name(f"screen.autarkia.matter_conduit.{k}", v)


def archive():
    orientable("domain_archive", "domain_archive_front", "domain_archive_side", "domain_archive_side")
    horizontal_states("domain_archive")
    block_item("domain_archive")
    loot_self("domain_archive")
    tag("block", PICK, rl("domain_archive"))
    tag("block", IRON_TOOL, rl("domain_archive"))
    name("block.autarkia.domain_archive", "Domain Archive")
    name("menu.autarkia.domain_archive", "Domain Archive")
    shaped("domain_archive", ["PAP", "CXC", "PAP"], {"P": "aegis_plate", "A": "ai_core", "C": "lattice_crystal", "X": "matter_buffer"})
    for k, v in {"active": "Active", "duplicate": "Duplicate archive", "outside": "Outside domain", "no_power": "No power"}.items():
        name(f"jade.autarkia.domain_archive.status.{k}", v)
    name("jade.autarkia.domain_archive.sources", "Sources %s, %s FE/t")
    messages = {"duplicate": "Duplicate archive: this domain already has one", "outside": "The archive is outside every domain",
                "no_power": "The archive has no power", "none_available": "None available in the domain", "queue_full": "The archive queue is full",
                "no_conduit": "No receiving matter conduit on channel %s", "delivered": "Delivered %s x %s"}
    for k, v in messages.items():
        name(f"message.autarkia.domain_archive.{k}", v)
    screen = {"search": "Search", "to_buffer": "Deliver to buffer", "to_conduit": "Deliver to conduit", "channel": "Ch %s", "free": "Free MU",
              "free_full": "Free MU in the domain: %s", "row": "%s -%s", "counts": "Available %s / total %s", "detail": "%s: -%s MU, %s s ago",
              "reason.reform": "Reform", "reason.seed": "Seed", "reason.compiler": "Compiler", "reason.matter": "Matter",
              "reason.restoration": "Restoration"}
    for k, v in screen.items():
        name(f"screen.autarkia.domain_archive.{k}", v)


def advancements():
    advancement("materials/compiler", "Written Matter", "Place a Matter Compiler", "matter_compiler", parent="materials/plasma",
                criteria={"placed": {"trigger": "minecraft:placed_block",
                                     "conditions": {"location": {"type": "minecraft:match_block", "blocks": rl("matter_compiler")}}}})
    advancement("materials/aegis", "Unbreakable", "Compile an Aegis Plate", "aegis_plate", parent="materials/compiler", criteria=inv_changed("aegis_plate"),
                frame="challenge")
    advancement("logistics/everywhere", "Everywhere at Once", "A matter conduit moves 1,000 items", "matter_conduit", parent="logistics/matter_of_fact")


def lang():
    info = {
        "matter_compiler": "2 x 2 x 2 heavy machine on foundation, 16,384 FE/t. Writes matter units into items: pick a target (ingots, nuggets, "
                           "lattice crystal, lattice alloy, aegis plate), feed anything with a matter value (raw ores count x8) or let it pull free MU "
                           "from the matter buffers of its domain. Buffer 65,536 MU; one item takes max(1 s, MU / 32 ticks): an iron ingot 1 s, "
                           "an aegis plate 51.2 s.",
        "aegis_plate": "32,768 MU, made only in the matter compiler (51.2 s, 16,777,216 FE). For all T6 machines and the Aegis gear.",
        "matter_conduit": "Instant item transfer inside a colony domain. A SEND conduit moves up to 64 items per tick (8 FE each) to the RECEIVE "
                          "conduits of the same colour channel, which push into the block they face. Buffer 9 slots. Without FE items wait; "
                          "outside a domain the conduit does nothing.",
        "domain_archive": "One per domain: a live view of every warehouse, depot, matter buffer and tally board area of the domain, and instant "
                          "delivery of up to 1,728 items per request into its 27-slot buffer or to a receiving matter conduit (16 FE per item, "
                          "idle 256 FE/t). It also shows the free MU and the last matter withdrawals. A second archive does nothing.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def generate():
    print("content_m9m: materials and logistics (T6)")
    compiler()
    print(f"  matter_compiler, aegis_plate, {len(COMPILING)} compiling recipes")
    conduit()
    archive()
    print("  matter_conduit, domain_archive")
    advancements()
    lang()
    print("content_m9m: done")

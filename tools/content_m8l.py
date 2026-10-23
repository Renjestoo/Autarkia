"""M8 content data, logistics part (03 s6, 04 s1.7, s2.3, s2.7): the matter value table, the matter tags, the matter
buffer, mag rail and mag rail station, the lattice rack, with models, block states, loot, tool tags, recipes (04 s1.9,
s2.9), language, JEI info pages and the two logistics advancements."""

from content_m5 import rack_block
from content_m6 import geo_block
from datagen import advancement, block_model, blockstate, geo_single, item_def, item_model, loot_self, name, rl, shaped, tag, write_json

PICK = "minecraft:mineable/pickaxe"
IRON_TOOL = "minecraft:needs_iron_tool"
METALS = {"iron": "minecraft:iron_ingot", "copper": "minecraft:copper_ingot", "gold": "minecraft:gold_ingot", "tin": "autarkia:tin_ingot",
          "zinc": "autarkia:zinc_ingot", "vantite": "autarkia:vantite_ingot"}
RAW = {"iron": "minecraft:raw_iron", "copper": "minecraft:raw_copper", "gold": "minecraft:raw_gold", "tin": "autarkia:raw_tin",
       "zinc": "autarkia:raw_zinc", "vantite": "autarkia:raw_vantite"}


def of(item, mul=1, div=1):
    rule = {"of": item}
    if mul != 1:
        rule["mul"] = mul
    if div != 1:
        rule["div"] = div
    return rule


def matter_table():
    """Write data/autarkia/matter_values.json: the 03 s6 table (copied exactly), item keys before tags."""
    items = {"minecraft:gravel": 1, "minecraft:glass": 1, "minecraft:end_stone": 2, "minecraft:obsidian": 16, "autarkia:slag": 2,
             "minecraft:coal": 9, "minecraft:charcoal": 9, "minecraft:redstone": 8, "minecraft:lapis_lazuli": 8, "minecraft:quartz": 16,
             "minecraft:amethyst_shard": 16, "minecraft:emerald": 384, "minecraft:diamond": 512, "minecraft:ancient_debris": 1024,
             "minecraft:copper_ingot": 27, "autarkia:tin_ingot": 27, "autarkia:zinc_ingot": 27, "autarkia:bronze_ingot": 27,
             "autarkia:brass_ingot": 27, "minecraft:iron_ingot": 36, "autarkia:steel_ingot": 54, "minecraft:gold_ingot": 108,
             "autarkia:vantite_ingot": 270, "autarkia:vantite_steel_ingot": 324, "minecraft:netherite_ingot": 4096,
             "autarkia:lattice_shard": 216, "autarkia:lattice_crystal": 864, "autarkia:lattice_alloy_ingot": 1188, "autarkia:aegis_plate": 32768}
    for nugget, ingot in (("minecraft:iron_nugget", "minecraft:iron_ingot"), ("minecraft:gold_nugget", "minecraft:gold_ingot"),
                          ("minecraft:copper_nugget", "minecraft:copper_ingot"), ("autarkia:tin_nugget", "autarkia:tin_ingot"),
                          ("autarkia:zinc_nugget", "autarkia:zinc_ingot"), ("autarkia:bronze_nugget", "autarkia:bronze_ingot"),
                          ("autarkia:steel_nugget", "autarkia:steel_ingot")):
        items[nugget] = of(ingot, div=9)
    for block, unit in (("minecraft:iron_block", "minecraft:iron_ingot"), ("minecraft:gold_block", "minecraft:gold_ingot"),
                        ("minecraft:copper_block", "minecraft:copper_ingot"), ("autarkia:tin_block", "autarkia:tin_ingot"),
                        ("autarkia:zinc_block", "autarkia:zinc_ingot"), ("autarkia:bronze_block", "autarkia:bronze_ingot"),
                        ("autarkia:brass_block", "autarkia:brass_ingot"), ("autarkia:steel_block", "autarkia:steel_ingot"),
                        ("autarkia:vantite_block", "autarkia:vantite_ingot"), ("autarkia:vantite_steel_block", "autarkia:vantite_steel_ingot"),
                        ("minecraft:netherite_block", "minecraft:netherite_ingot"), ("autarkia:lattice_alloy_block", "autarkia:lattice_alloy_ingot"),
                        ("minecraft:coal_block", "minecraft:coal"), ("minecraft:redstone_block", "minecraft:redstone"),
                        ("minecraft:lapis_block", "minecraft:lapis_lazuli"), ("minecraft:diamond_block", "minecraft:diamond"),
                        ("minecraft:emerald_block", "minecraft:emerald")):
        items[block] = of(unit, mul=9)
    for metal, ingot in METALS.items():
        items[RAW[metal]] = {"raw": ingot}
        raw_block = f"minecraft:raw_{metal}_block" if metal in ("iron", "copper", "gold") else f"autarkia:raw_{metal}_block"
        items[raw_block] = of(RAW[metal], mul=9)
        for form in (f"autarkia:crushed_{metal}", f"autarkia:washed_{metal}", f"autarkia:pure_{metal}", f"autarkia:{metal}_concentrate"):
            items[form] = of(ingot)
    for ore, raw, count in (("minecraft:iron_ore", "minecraft:raw_iron", 1), ("minecraft:deepslate_iron_ore", "minecraft:raw_iron", 1),
                            ("minecraft:copper_ore", "minecraft:raw_copper", 3), ("minecraft:deepslate_copper_ore", "minecraft:raw_copper", 3),
                            ("minecraft:gold_ore", "minecraft:raw_gold", 1), ("minecraft:deepslate_gold_ore", "minecraft:raw_gold", 1),
                            ("autarkia:tin_ore", "autarkia:raw_tin", 1), ("autarkia:deepslate_tin_ore", "autarkia:raw_tin", 1),
                            ("autarkia:zinc_ore", "autarkia:raw_zinc", 1), ("autarkia:deepslate_zinc_ore", "autarkia:raw_zinc", 1),
                            ("autarkia:vantite_ore", "autarkia:raw_vantite", 1)):
        items[ore] = of(raw, mul=count)
    item_tags = {"c:cobblestones": 1, "minecraft:dirt": 1, "minecraft:sand": 1, "minecraft:planks": 1, "minecraft:logs": 4}
    for metal, ingot in METALS.items():
        item_tags[f"c:raw_materials/{metal}"] = {"raw": ingot}
        item_tags[f"c:ingots/{metal}"] = of(ingot)
    block_tags = {"minecraft:base_stone_overworld": 1, "minecraft:base_stone_nether": 1}
    write_json("data/autarkia/matter_values.json", {"items": items, "item_tags": item_tags, "block_tags": block_tags})


def matter_tags():
    tag("item", "autarkia:dissolvable", "minecraft:cobblestone", "minecraft:cobbled_deepslate", "minecraft:dirt", "minecraft:gravel",
        "minecraft:sand", "minecraft:netherrack", "minecraft:tuff", rl("slag"))
    write_json("data/autarkia/tags/item/matter_blacklist.json", {"replace": False, "values": [
        rl("core_seed"), rl("outpost_seed"), rl("worker_capsule"), {"id": rl("autarky_seed"), "required": False}, "minecraft:command_block",
        "minecraft:chain_command_block", "minecraft:repeating_command_block", "minecraft:command_block_minecart"]})
    tag("block", "autarkia:matter_bulk", "#minecraft:base_stone_overworld", "#minecraft:base_stone_nether", "#minecraft:dirt", "#minecraft:sand",
        "minecraft:gravel", "minecraft:cobblestone", "minecraft:cobbled_deepslate")


def matter_buffer():
    geo_block("matter_buffer", "Matter Buffer")
    shaped("matter_buffer", ["LNL", "CXC", "LNL"], {"L": "lattice_alloy_ingot", "N": "nanite_paste", "C": "advanced_circuit", "X": "lattice_crystal"})
    name("menu.autarkia.matter_buffer", "Matter Buffer")
    screen = {"auto_on": "Auto: on", "auto_off": "Auto: off", "extract": "Take", "dissolve": "Dissolve", "confirm": "Sure?", "row": "%s x %s MU",
              "detail": "%s stored, %s MU each, %s MU in all", "bar": "%s + %s / %s MU", "empty": "Empty"}
    for k, v in screen.items():
        name(f"screen.autarkia.matter_buffer.{k}", v)
    name("jade.autarkia.matter_buffer.used", "Matter: %s / %s MU")
    name("jade.autarkia.matter_buffer.free", "Free: %s MU")
    name("tooltip.autarkia.load", "Load: %s units")
    name("tooltip.autarkia.load_matter", "Load: %s units, Matter: %s MU")
    name("jei.autarkia.info.matter_buffer", "2 x 2 x 2 store of 1,048,576 matter units (MU). Items keep their identity; each counts its MU from the "
                                            "matter table (Shift on any tooltip shows it; raw ores x6 at T5, x8 at T6). Dissolve turns all of one item "
                                            "into free MU for good. With Auto on it dissolves cobblestone, dirt, gravel, sand, netherrack, tuff and slag "
                                            "while the domain has less than 65,536 free MU. Refuses items with no value, filled containers and seeds. "
                                            "No power needed.")


def mag_rail():
    block_model("mag_rail", {"parent": "minecraft:block/rail_flat", "textures": {"rail": rl("block/mag_rail")}})
    block_model("mag_rail_corner", {"parent": "minecraft:block/rail_curved", "textures": {"rail": rl("block/mag_rail_corner")}})
    block_model("mag_rail_raised_ne", {"parent": "minecraft:block/template_rail_raised_ne", "textures": {"rail": rl("block/mag_rail")}})
    block_model("mag_rail_raised_sw", {"parent": "minecraft:block/template_rail_raised_sw", "textures": {"rail": rl("block/mag_rail")}})
    variants = {"shape=north_south": {"model": rl("block/mag_rail")}, "shape=east_west": {"model": rl("block/mag_rail"), "y": 90},
                "shape=ascending_north": {"model": rl("block/mag_rail_raised_ne")}, "shape=ascending_east": {"model": rl("block/mag_rail_raised_ne"), "y": 90},
                "shape=ascending_south": {"model": rl("block/mag_rail_raised_sw")}, "shape=ascending_west": {"model": rl("block/mag_rail_raised_sw"), "y": 90},
                "shape=south_east": {"model": rl("block/mag_rail_corner")}, "shape=south_west": {"model": rl("block/mag_rail_corner"), "y": 90},
                "shape=north_west": {"model": rl("block/mag_rail_corner"), "y": 180}, "shape=north_east": {"model": rl("block/mag_rail_corner"), "y": 270}}
    blockstate("mag_rail", {"variants": variants})
    item_model("mag_rail", {"parent": "minecraft:item/generated", "textures": {"layer0": rl("block/mag_rail")}})
    item_def("mag_rail")
    loot_self("mag_rail")
    tag("block", PICK, rl("mag_rail"))
    name("block.autarkia.mag_rail", "Mag Rail")
    shaped("mag_rail", ["I I", "IXI", "I I"], {"I": "steel_ingot", "X": "lattice_shard"}, count=16)
    name("jei.autarkia.info.mag_rail", "Track for mag rail sleds: place in a chain between mag rail stations; it bends and climbs like a rail, without "
                                        "junctions, and needs a solid block below. A line holds up to 2,048 rails, 16 stations and 32 sleds. A sled "
                                        "that reaches a gap falls and lands as a crate block with all its items.")


def station():
    geo_single("mag_rail_station")
    loot_self("mag_rail_station")
    tag("block", PICK, rl("mag_rail_station"))
    tag("block", IRON_TOOL, rl("mag_rail_station"))
    name("block.autarkia.mag_rail_station", "Mag Rail Station")
    name("menu.autarkia.mag_rail_station", "Mag Rail Station")
    shaped("mag_rail_station", ["LAL", "RXR", "LFL"], {"L": "lattice_alloy_ingot", "A": "advanced_circuit", "R": "mag_rail", "X": "crate",
                                                       "F": "foundation"})
    screen = {"name": "Station name", "to": "To: %s", "next": "Next", "input": "Input", "output": "Output", "line": "Line: %s rails, %s stations",
              "in_flight": "Sleds in flight: %s"}
    for k, v in screen.items():
        name(f"screen.autarkia.mag_rail.{k}", v)
    states = {"ready": "Ready", "waiting": "Waiting for items", "no_line": "No track", "loop": "Closed loop: no sends", "over_cap": "Line over the cap",
              "no_destination": "No destination", "no_crate": "No empty crate", "no_power": "No power", "busy": "Too many sleds on the line",
              "locked": "Waiting: lock or spacing"}
    for k, v in states.items():
        name(f"state.autarkia.mag_rail.{k}", v)
    jade = {"name": "Station: %s", "unnamed": "unnamed", "destination": "Destination: %s", "in_flight": "Sleds in flight: %s",
            "lock": "Direction lock: %s", "lock.none": "none", "lock.inbound": "sleds coming in", "lock.outbound": "sleds going out",
            "lock.both": "both ways"}
    for k, v in jade.items():
        name(f"jade.autarkia.mag_rail.{k}", v)
    name("jade.autarkia.falling_crate.mode.rail", "Rail sled")
    name("jei.autarkia.info.mag_rail_station", "Inline or end stop of a mag rail line. It packs its 27 input stacks into an empty crate and sends the "
                                                "crate as a sled when the input is full or idle 10 s: 2,000 FE per launch (16,000 FE buffer, 512 FE/t), "
                                                "20 blocks/s with acceleration and braking. Destination: a named station or Next. One track: a sled "
                                                "waits in the station while another runs the other way or is less than 8 blocks ahead. Arrivals unload "
                                                "into the output; with no room the sled waits above the station.")


def lattice_rack():
    rack_block("lattice_rack", "Lattice Rack", PICK, 4)
    tag("block", IRON_TOOL, rl("lattice_rack"))
    shaped("lattice_rack", ["A A", "CCC", "A A"], {"A": "lattice_alloy_ingot", "C": "lattice_crystal"})
    name("menu.autarkia.rack_4", "Lattice Rack")
    name("jei.autarkia.info.lattice_rack", "Holds 4 crates (filled or empty). Load rating 65,536 units: 4 crates of iron blocks are 62,240 units "
                                           "(95%). Bends above 100%, collapses after 60 s above 120%, at once above 150%. Drops 2 lattice shards "
                                           "when it collapses.")


def advancements():
    advancement("logistics/float_on", "Float On", "A sled delivers over 256+ rails", "mag_rail", parent="logistics/special_delivery")
    advancement("logistics/matter_of_fact", "Matter of Fact", "A domain holds 100,000+ MU", "matter_buffer", parent="logistics/float_on")


def generate():
    matter_table()
    matter_tags()
    matter_buffer()
    mag_rail()
    station()
    lattice_rack()
    advancements()

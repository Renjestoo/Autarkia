"""M6 content data, power and processing part: insulated cable, steel pylon, wall insulator, transformer, fuse box, steam
turbine and battery bank (02), the T3 machines, pure forms, circuits and servo (03), with recipes, tags and language.
The logistics, worker, colony and equipment data are in content_m6b."""

from datagen import (block_item, block_model, blockstate, cooking, generated_item, geo_single, horizontal_states, loot_center_only, loot_self,
                     machine_recipe, name, orientable, particle_model, rl, shaped, shapeless, simple_blockstate, tag)

PICK = "minecraft:mineable/pickaxe"
IRON_TOOL = "minecraft:needs_iron_tool"
SIDES = ("north", "south", "east", "west", "up", "down")
METALS = ["iron", "copper", "gold", "tin", "zinc"]
INGOT = {"iron": "minecraft:iron_ingot", "copper": "minecraft:copper_ingot", "gold": "minecraft:gold_ingot", "tin": "tin_ingot",
         "zinc": "zinc_ingot"}
SECONDARY = {"iron": "zinc_nugget", "copper": "minecraft:gold_nugget", "gold": "minecraft:copper_nugget", "tin": "minecraft:iron_nugget",
             "zinc": "minecraft:copper_nugget"}


def faces(tex):
    return {k: {"texture": f"#{tex}"} for k in SIDES}


def geo_block(bid, title):
    particle_model(bid)
    simple_blockstate(bid)
    generated_item(bid)
    loot_center_only(bid)
    tag("block", PICK, rl(bid))
    tag("block", IRON_TOOL, rl(bid))
    name(f"block.autarkia.{bid}", title)


def facing_states(bid, model):
    rot = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "east": {"x": 90, "y": 90},
           "west": {"x": 90, "y": 270}}
    blockstate(bid, {"variants": {f"facing={d}": {"model": rl(f"block/{model}"), **r} for d, r in rot.items()}})


def power():
    geo_block("steel_pylon", "Steel Pylon")
    geo_block("steam_turbine", "Steam Turbine")
    shaped("steel_pylon", ["BSB", "SPS", "S S"], {"B": "minecraft:brick", "S": "steel_ingot", "P": "steel_plate"})
    shaped("steam_turbine", ["BCB", "GEG", "BXB"], {"B": "steel_block", "C": "copper_coil", "G": "steel_gear", "E": "steam_engine",
                                                     "X": "basic_circuit"})

    block_model("wall_insulator", {"textures": {"particle": rl("block/wall_insulator"), "i": rl("block/wall_insulator")}, "elements": [
        {"from": [5, 0, 5], "to": [11, 2, 11], "faces": faces("i")},
        {"from": [6, 2, 6], "to": [10, 6, 10], "faces": faces("i")},
        {"from": [5.5, 3, 5.5], "to": [10.5, 4, 10.5], "faces": faces("i")},
        {"from": [7, 6, 7], "to": [9, 7, 9], "faces": faces("i")}]})
    facing_states("wall_insulator", "wall_insulator")
    block_item("wall_insulator")
    loot_self("wall_insulator")
    tag("block", PICK, rl("wall_insulator"))
    name("block.autarkia.wall_insulator", "Wall Insulator")
    shapeless("wall_insulator", ["steel_ingot", "minecraft:brick", "minecraft:brick"], 4)

    block_model("transformer", {"parent": "minecraft:block/cube_column",
                                "textures": {"end": rl("block/transformer_top"), "side": rl("block/transformer_side")}})
    simple_blockstate("transformer")
    block_item("transformer")
    loot_self("transformer")
    orientable("fuse_box", "fuse_box_front", "fuse_box_side", "fuse_box_side")
    horizontal_states("fuse_box")
    block_item("fuse_box")
    geo_single("battery_bank")
    for bid in ("fuse_box", "battery_bank"):
        loot_self(bid)
    for bid, title in [("transformer", "Transformer"), ("fuse_box", "Fuse Box"), ("battery_bank", "Battery Bank")]:
        tag("block", PICK, rl(bid))
        name(f"block.autarkia.{bid}", title)
    shaped("transformer", ["PCP", "IBI", "PCP"], {"P": "steel_plate", "C": "copper_coil", "I": "minecraft:iron_block", "B": "basic_circuit"})
    shaped("fuse_box", ["PRP", "CBC", "PPP"], {"P": "steel_plate", "R": "minecraft:redstone", "C": "minecraft:copper_ingot", "B": "basic_circuit"})
    shaped("battery_bank", ["PZP", "CXC", "PZP"], {"P": "steel_plate", "Z": "zinc_block", "C": "minecraft:copper_ingot", "X": "basic_circuit"})

    generated_item("insulated_cable_spool")
    name("item.autarkia.insulated_cable_spool", "Insulated Cable Spool")
    shapeless("insulated_cable_spool", ["copper_wire_spool", "minecraft:dried_kelp", "minecraft:dried_kelp", "minecraft:dried_kelp"])
    tag("item", "autarkia:load/dense", rl("insulated_cable_spool"))


def processing():
    geo_block("jaw_crusher", "Jaw Crusher")
    geo_block("fabricator", "Fabricator")
    for bid, title in [("magnetic_separator", "Magnetic Separator"), ("electrolyzer", "Electrolyzer")]:
        geo_single(bid)
        loot_self(bid)
        tag("block", PICK, rl(bid))
        name(f"block.autarkia.{bid}", title)
    for iid, title in [("basic_circuit", "Basic Circuit"), ("servo", "Servo")] + [(f"pure_{m}", f"Pure {m.capitalize()} Ore") for m in METALS]:
        generated_item(iid)
        name(f"item.autarkia.{iid}", title)
    for m in METALS:
        tag("item", "autarkia:pure_ores", rl(f"pure_{m}"))
        tag("item", f"c:pure_ores/{m}", rl(f"pure_{m}"))
    tag("item", "autarkia:load/dense", "#autarkia:pure_ores")
    shaped("basic_circuit", ["RRR", "CPC", "RRR"], {"R": "minecraft:redstone", "C": "copper_coil", "P": "brass_plate"})
    shaped("fabricator", ["SBS", "GTG", "SPS"], {"S": "steel_plate", "B": "basic_circuit", "G": "steel_gear", "T": "minecraft:crafting_table",
                                                 "P": "minecraft:piston"})
    shaped("jaw_crusher", ["SGS", "CXC", "SGS"], {"S": "steel_plate", "G": "steel_gear", "C": "basic_circuit", "X": "steel_block"})
    shaped("magnetic_separator", ["SCS", "CBC", "SGS"], {"S": "steel_plate", "C": "copper_coil", "B": "basic_circuit", "G": "steel_gear"})
    shaped("electrolyzer", ["SBS", "GCG", "SKS"], {"S": "steel_plate", "B": "basic_circuit", "G": "minecraft:glass", "C": "copper_coil",
                                                   "K": "minecraft:cauldron"})
    raw = {"iron": "#c:raw_materials/iron", "copper": "#c:raw_materials/copper", "gold": "#c:raw_materials/gold", "tin": "#c:raw_materials/tin",
           "zinc": "#c:raw_materials/zinc"}
    for m in METALS:
        machine_recipe("crushing", f"{m}_jaw", [(raw[m], 1)], [(f"crushed_{m}", 2)], stations=["jaw_crusher"])
        machine_recipe("separating", f"pure_{m}", [(f"washed_{m}", 2)], [(f"pure_{m}", 3), ("slag", 1), (SECONDARY[m], 1, 0.5)])
        machine_recipe("electrolyzing", f"{m}_from_pure", [(f"pure_{m}", 1)], [(INGOT[m], 1), (SECONDARY[m], 1, 0.1)])
        for kind in ("smelting", "blasting"):
            cooking(kind, INGOT[m], f"pure_{m}", rid=f"{kind}/{m}_ingot_from_pure")
    machine_recipe("crushing", "gravel_from_cobblestone", [("#c:cobblestones", 1)], [("minecraft:gravel", 1)],
                   stations=["quern", "stamp_mill", "jaw_crusher"])
    machine_recipe("crushing", "sand_from_gravel", [("minecraft:gravel", 1)], [("minecraft:sand", 1)], stations=["quern", "stamp_mill", "jaw_crusher"])
    machine_recipe("fabricating", "basic_circuit", [("#c:plates/brass", 1), ("minecraft:redstone", 2), ("copper_coil", 1)], [("basic_circuit", 2)],
                   time=200)
    machine_recipe("fabricating", "servo", [("steel_gear", 2), ("copper_coil", 1), ("steel_plate", 1), ("basic_circuit", 1)], [("servo", 1)],
                   time=200)


def lang():
    jade = {"transformer.sides": "A: %s B: %s", "transformer.mode": "Mode: %s", "transformer.flow": "Flow: %s FE/t", "transformer.none": "none",
            "fuse.closed": "State: closed", "fuse.open": "State: open (%s s)", "fuse.locked": "State: locked", "turbine.rotor": "Rotor: %s%%"}
    for k, v in jade.items():
        name(f"jade.autarkia.{k}", v)
    for k, v in (("auto", "Auto"), ("a_to_b", "A to B"), ("b_to_a", "B to A")):
        name(f"mode.autarkia.transformer.{k}", v)
    screen = {"fuse.reset": "Reset", "fuse.trips": "Trips in the last minute: %s", "fuse.spans": "Spans: %s / 2", "turbine.rotor": "Rotor %s%%",
              "turbine.steam": "Steam: %s mB/t", "turbine.output": "Out: %s FE/t"}
    for k, v in screen.items():
        name(f"screen.autarkia.{k}", v)
    for k, v in (("transformer.mode", "Transformer mode: %s"), ("fuse.reset", "Fuse reset")):
        name(f"message.autarkia.{k}", v)
    for m in ("fuse_box", "steam_turbine", "jaw_crusher", "magnetic_separator", "electrolyzer", "fabricator"):
        name(f"menu.autarkia.{m}", m.replace("_", " ").title())
    for c, v in (("separating", "Magnetic separation"), ("electrolyzing", "Electrolysis"), ("fabricating", "Fabricating")):
        name(f"jei.autarkia.category.{c}", v)
    info = {
        "insulated_cable_spool": "Insulated cable: 1,024 FE/t, spans up to 24 blocks (12 to or from a wall insulator), loss 0.1% per block. One spool per 16 blocks. Overload heats it: smoke, flame, snap, fire.",
        "steel_pylon": "1 x 5 x 1 node for insulated cable, 6 spans. Feeds the machines next to it.",
        "wall_insulator": "Small node on a wall face for insulated cable, 2 spans of at most 12 blocks.",
        "transformer": "Joins two cable tiers: side A is the tier of the first span, side B the next. Moves up to the lower tier's rating per tick with 2% loss. Wrench: Auto (from the network with more spare power), A to B, B to A.",
        "fuse_box": "Inline node with exactly 2 spans of one tier. Opens at 100% load for 1 s or 150% at once, or when lightning reaches it; closes again after 10 s. After 3 trips in a minute it stays open until reset (button or wrench).",
        "steam_turbine": "3 x 3 x 2. Takes up to 100 mB of steam per tick from 1 to 4 boilers touching its back or sides. The rotor spins up in 30 s and down in 60 s; out = rotor x steam x 10 FE/t, max 1,000.",
        "battery_bank": "Stores 400,000 FE, I/O 1,024 FE/t, no leak. Charges an FE item in its slot at 2,048 FE/t.",
        "jaw_crusher": "2 x 2 x 2 heavy machine on foundation. 192 FE/t, 100 ticks: 1 raw ore to 2 crushed ore; cobblestone to gravel, gravel to sand.",
        "magnetic_separator": "128 FE/t, 120 ticks: 2 washed ore to 3 pure ore, 1 slag and a secondary nugget at 50%.",
        "electrolyzer": "256 FE/t, 160 ticks: 1 pure ore to 1 ingot plus a secondary nugget at 10%.",
        "fabricator": "2 x 2 x 1. 160 FE/t. Pick a target recipe with the recipe button; it pulls the ingredients from its 3 x 3 buffer and makes circuits, servos, frames and equipment.",
        "basic_circuit": "Crafting table or fabricator. For electric machines and drones.", "servo": "Fabricator. For workers and T4 machines.",
    }
    for m in METALS:
        info[f"pure_{m}"] = "Magnetic separator: 2 washed to 3 pure. Smelts or electrolyzes to 1 ingot."
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def generate():
    power()
    processing()
    lang()

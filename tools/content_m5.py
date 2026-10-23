"""M5 content data: zinc, brass and steel, the T2 processing machines and their recipes, belts, splitter, sorting chute,
wood rack, pattern loom and card, the Hauler Crab, steel equipment, template 9, tags, advancements and language."""

from content_m3 import NORTH, cond, plan
from datagen import (advancement, block_item, block_model, blockstate, cooking, cube_all, generated_item, geo_single, height_range,
                     horizontal_states, inv_changed, item_def, item_model, loot_center_only, loot_ore, loot_self, machine_recipe, name, ore_feature,
                     ore_targets, particle_model, placed_feature, biome_modifier, rl, shaped, shapeless, simple_blockstate, tag, write_json)

PICK = "minecraft:mineable/pickaxe"
AXE = "minecraft:mineable/axe"
STONE_TOOL = "minecraft:needs_stone_tool"
IRON_TOOL = "minecraft:needs_iron_tool"
METALS = ["iron", "copper", "gold", "tin", "zinc"]
INGOT = {"iron": "minecraft:iron_ingot", "copper": "minecraft:copper_ingot", "gold": "minecraft:gold_ingot", "tin": "tin_ingot",
         "zinc": "zinc_ingot"}
SECONDARY = {"iron": "zinc_nugget", "copper": "minecraft:gold_nugget", "gold": "minecraft:copper_nugget", "tin": "minecraft:iron_nugget",
             "zinc": "minecraft:copper_nugget"}
SIDES = ("north", "south", "east", "west", "up", "down")


def faces(tex, only=None):
    return {k: {"texture": f"#{tex}"} for k in (only or SIDES)}


def simple_block(bid, title, tool, needs=None):
    cube_all(bid)
    simple_blockstate(bid)
    block_item(bid)
    name(f"block.autarkia.{bid}", title)
    tag("block", tool, rl(bid))
    if needs:
        tag("block", needs, rl(bid))


def ores():
    for bid, title in [("zinc_ore", "Zinc Ore"), ("deepslate_zinc_ore", "Deepslate Zinc Ore")]:
        simple_block(bid, title, PICK, STONE_TOOL)
        loot_ore(bid, "raw_zinc")
        tag("block", "c:ores/zinc", rl(bid))
        tag("block", "c:ores", "#c:ores/zinc")
        tag("item", "c:ores/zinc", rl(bid))
        tag("item", "c:ores", "#c:ores/zinc")
    tag("block", "c:ores_in_ground/stone", rl("zinc_ore"))
    tag("block", "c:ores_in_ground/deepslate", rl("deepslate_zinc_ore"))
    for bid, title, ctag, part, needs in [("raw_zinc_block", "Block of Raw Zinc", "raw_zinc", "raw_zinc", STONE_TOOL),
                                          ("zinc_block", "Block of Zinc", "zinc", "zinc_ingot", STONE_TOOL),
                                          ("brass_block", "Block of Brass", "brass", "brass_ingot", STONE_TOOL),
                                          ("steel_block", "Block of Steel", "steel", "steel_ingot", IRON_TOOL)]:
        simple_block(bid, title, PICK, needs)
        loot_self(bid)
        tag("block", f"c:storage_blocks/{ctag}", rl(bid))
        tag("block", "c:storage_blocks", f"#c:storage_blocks/{ctag}")
        tag("item", f"c:storage_blocks/{ctag}", rl(bid))
        tag("item", "c:storage_blocks", f"#c:storage_blocks/{ctag}")
        shaped(bid, ["XXX", "XXX", "XXX"], {"X": part}, category="building")
        shapeless(part, [bid], 9, rid=f"{part}_from_{bid}")
    ore_feature("zinc_ore", 8, ore_targets("zinc_ore", "deepslate_zinc_ore"))
    placed_feature("zinc_ore", "zinc_ore", [{"type": "minecraft:count", "count": 8}, {"type": "minecraft:in_square"},
                                            height_range("uniform", 0, 64), {"type": "minecraft:biome"}])
    biome_modifier("zinc_ore", "#minecraft:is_overworld", ["zinc_ore", "zinc_deposit"], "underground_ores")


def materials():
    items = [("raw_zinc", "Raw Zinc"), ("zinc_ingot", "Zinc Ingot"), ("zinc_nugget", "Zinc Nugget"), ("brass_ingot", "Brass Ingot"),
             ("steel_ingot", "Steel Ingot"), ("steel_nugget", "Steel Nugget"), ("slag", "Slag"), ("crushed_zinc", "Crushed Zinc Ore"),
             ("steel_gear", "Steel Gear"), ("copper_coil", "Copper Coil"), ("steel_plate", "Steel Plate"), ("brass_plate", "Brass Plate"),
             ("crab_frame", "Crab Frame")]
    for m in METALS:
        items.append((f"washed_{m}", f"Washed {m.capitalize()} Ore"))
    for iid, title in items:
        generated_item(iid)
        name(f"item.autarkia.{iid}", title)
    tag("item", "c:raw_materials/zinc", rl("raw_zinc"))
    tag("item", "c:raw_materials", "#c:raw_materials/zinc")
    for m in ("zinc", "brass", "steel"):
        tag("item", f"c:ingots/{m}", rl(f"{m}_ingot"))
        tag("item", "c:ingots", f"#c:ingots/{m}")
    for m in ("zinc", "steel"):
        tag("item", f"c:nuggets/{m}", rl(f"{m}_nugget"))
        tag("item", "c:nuggets", f"#c:nuggets/{m}")
        shaped(f"{m}_ingot", ["NNN", "NNN", "NNN"], {"N": f"{m}_nugget"}, rid=f"{m}_ingot_from_nuggets")
        shapeless(f"{m}_nugget", [f"{m}_ingot"], 9)
    for p in ("steel", "brass"):
        tag("item", f"c:plates/{p}", rl(f"{p}_plate"))
        tag("item", "c:plates", f"#c:plates/{p}")
    tag("item", "c:gears/steel", rl("steel_gear"))
    tag("item", "c:gears", "#c:gears/steel")
    tag("item", "autarkia:crushed_ores", rl("crushed_zinc"))
    tag("item", "c:crushed_ores/zinc", rl("crushed_zinc"))
    for m in METALS:
        tag("item", "autarkia:washed_ores", rl(f"washed_{m}"))
        for form in (f"washed_{m}",) + ((f"crushed_{m}",) if m == "zinc" else ()):
            cooking("smelting", INGOT[m], form, rid=f"smelting/{m}_ingot_from_{form.split('_')[0]}")
            cooking("blasting", INGOT[m], form, rid=f"blasting/{m}_ingot_from_{form.split('_')[0]}")
    for src in ("raw_zinc", "zinc_ore", "deepslate_zinc_ore"):
        cooking("smelting", "zinc_ingot", src, rid=f"smelting/zinc_ingot_from_{src}")
        cooking("blasting", "zinc_ingot", src, rid=f"blasting/zinc_ingot_from_{src}")
    shaped("steel_gear", [" P ", "PIP", " P "], {"P": "steel_plate", "I": "#c:ingots/iron"}, 2)
    shaped("foundation", ["SS", "SS"], {"S": "slag"}, 2, category="building", rid="foundation_from_slag")
    shaped("crab_frame", ["PBP", "G G", "PBP"], {"P": "steel_plate", "B": "brass_plate", "G": "steel_gear"})


def processing():
    machine_recipe("crushing", "zinc_from_raw", [("#c:raw_materials/zinc", 1)], [("crushed_zinc", 1), ("crushed_zinc", 1, 0.5)],
                   stations=["quern", "stamp_mill"])
    for m in METALS:
        machine_recipe("washing", f"washed_{m}", [(f"crushed_{m}", 1)], [(f"washed_{m}", 1), (SECONDARY[m], 1, 0.1)])
        machine_recipe("alloying", f"{m}_from_washed", [(f"washed_{m}", 3)], [(INGOT[m], 4), ("slag", 1, 0.3)], stations=["blast_smelter"],
                       time=200)
    machine_recipe("alloying", "brass_ingot", [("#c:ingots/copper", 3), ("#c:ingots/zinc", 1)], [("brass_ingot", 4)],
                   stations=["crucible", "blast_smelter"], time=300)
    machine_recipe("alloying", "steel_ingot", [("#c:ingots/iron", 1), ("#minecraft:coals", 2)], [("steel_ingot", 1), ("slag", 1, 0.25)],
                   stations=["blast_smelter"], time=200)
    machine_recipe("pressing", "steel_plate", [("#c:ingots/steel", 1)], [("steel_plate", 1)])
    machine_recipe("pressing", "brass_plate", [("#c:ingots/brass", 1)], [("brass_plate", 1)])
    machine_recipe("drawing", "copper_coil", [("#c:ingots/copper", 4), ("#c:ingots/iron", 1)], [("copper_coil", 2)])
    machine_recipe("drawing", "copper_wire_spool", [("#c:ingots/copper", 2)], [("copper_wire_spool", 1)])

    for bid, title in [("sluice", "Sluice"), ("blast_smelter", "Blast Smelter")]:
        particle_model(bid)
        simple_blockstate(bid)
        generated_item(bid)
        loot_center_only(bid)
        tag("block", AXE if bid == "sluice" else PICK, rl(bid))
        name(f"block.autarkia.{bid}", title)
    shaped("sluice", ["L L", "LXL", "PPP"], {"L": "#minecraft:logs", "X": "minecraft:iron_bars", "P": "brass_plate"})
    shaped("blast_smelter", ["KBK", "BFB", "KCK"], {"K": "minecraft:bricks", "B": "brass_plate", "F": "minecraft:blast_furnace", "C": "copper_coil"})
    for bid, title in [("plate_press", "Plate Press"), ("wire_drawer", "Wire Drawer")]:
        geo_single(bid)
        loot_self(bid)
        tag("block", PICK, rl(bid))
        name(f"block.autarkia.{bid}", title)
    shaped("plate_press", ["IPI", "BGB", "BBB"], {"I": "#c:ingots/iron", "P": "minecraft:piston", "B": "#c:ingots/brass", "G": "bronze_gear"})
    shaped("wire_drawer", ["BGB", "XPX", "BBB"], {"B": "#c:ingots/brass", "G": "bronze_gear", "X": "minecraft:iron_bars", "P": "minecraft:piston"})


def belt_models():
    tex = {"particle": rl("block/belt_side"), "top": rl("block/belt_top"), "side": rl("block/belt_side")}
    flat = {"from": [0, 0, 0], "to": [16, 5, 16], "faces": {"up": {"texture": "#top"}, "down": {"texture": "#side"}, "north": {"texture": "#side"},
                                                            "south": {"texture": "#side"}, "east": {"texture": "#side"}, "west": {"texture": "#side"}}}
    block_model("belt", {"textures": tex, "elements": [flat]})
    for slope, angle in (("up", 45), ("down", -45)):
        ramp = {"from": [0, 6.5, -3.3], "to": [16, 9.5, 19.3], "rotation": {"angle": angle, "axis": "x", "origin": [8, 8, 8]},
                "faces": {"up": {"texture": "#top", "uv": [0, 0, 16, 16]}, "down": {"texture": "#side", "uv": [0, 0, 16, 16]},
                          "north": {"texture": "#side", "uv": [0, 0, 16, 3]}, "south": {"texture": "#side", "uv": [0, 0, 16, 3]},
                          "east": {"texture": "#side", "uv": [0, 0, 16, 3]}, "west": {"texture": "#side", "uv": [0, 0, 16, 3]}}}
        legs = [{"from": [1, 0, 1], "to": [3, 6, 3], "faces": faces("side")}, {"from": [13, 0, 1], "to": [15, 6, 3], "faces": faces("side")},
                {"from": [1, 0, 13], "to": [3, 6, 15], "faces": faces("side")}, {"from": [13, 0, 13], "to": [15, 6, 15], "faces": faces("side")}]
        block_model(f"belt_{slope}", {"textures": tex, "elements": [ramp] + legs})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    variants = {}
    for d, y in rot.items():
        for slope, model in (("flat", "belt"), ("up", "belt_up"), ("down", "belt_down")):
            v = {"model": rl(f"block/{model}")}
            if y:
                v["y"] = y
            variants[f"facing={d},slope={slope}"] = v
    blockstate("belt", {"variants": variants})
    block_item("belt")
    loot_self("belt")
    tag("block", PICK, rl("belt"))
    name("block.autarkia.belt", "Belt")
    for bid, title in [("belt_splitter", "Belt Splitter"), ("sorting_chute", "Sorting Chute")]:
        t = {"particle": rl(f"block/{bid}_top"), "top": rl(f"block/{bid}_top"), "side": rl("block/belt_side")}
        block_model(bid, {"textures": t, "elements": [
            {"from": [0, 0, 0], "to": [16, 5, 16], "faces": {"up": {"texture": "#top"}, "down": {"texture": "#side"}, "north": {"texture": "#side"},
                                                             "south": {"texture": "#side"}, "east": {"texture": "#side"}, "west": {"texture": "#side"}}},
            {"from": [0, 5, 0], "to": [2, 10, 16], "faces": faces("side")}, {"from": [14, 5, 0], "to": [16, 10, 16], "faces": faces("side")}]})
        horizontal_states(bid)
        block_item(bid)
        loot_self(bid)
        tag("block", PICK if bid == "belt_splitter" else AXE, rl(bid))
        name(f"block.autarkia.{bid}", title)
    shaped("belt", ["KKK", "GCG", "KKK"], {"K": "minecraft:dried_kelp", "G": "steel_gear", "C": "copper_coil"}, 8)
    shaped("belt_splitter", [" R ", "GBG"], {"R": "minecraft:comparator", "G": "steel_gear", "B": "belt"})
    shaped("sorting_chute", ["H", "B", "U"], {"H": "minecraft:hopper", "B": "belt", "U": "chute"})


def uv_faces(tex, side_uv, end_uv):
    return {k: {"texture": f"#{tex}", "uv": end_uv if k in ("up", "down") else side_uv} for k in SIDES}


def rack_block(bid, title, tool, positions):
    """Write the four bend models, block state, item, loot, tool tag and name of a rack with the given crate positions."""
    tex = {"particle": rl(f"block/{bid}"), "frame": rl(f"block/{bid}"), "tag": rl("block/rack_tag")}
    post_faces = uv_faces("frame", [0, 0, 2, 16], [0, 0, 2, 2])
    shelf_faces = uv_faces("frame", [3, 0, 13, 1], [3, 0, 13, 3])
    for bend in range(4):
        sag = bend
        posts = []
        for x, z in ((0, 0), (14, 0), (0, 14), (14, 14)):
            post = {"from": [x, 0, z], "to": [x + 2, 16, z + 2], "faces": post_faces}
            if bend >= 2:
                post["rotation"] = {"angle": -2.5 if x == 0 else 2.5, "axis": "z", "origin": [x + 1, 0, z + 1]}
            posts.append(post)
        shelves = [{"from": [2, 0, 1], "to": [14, 1, 15], "faces": shelf_faces}]
        for k in range(1, positions):
            y = 16 * k / positions - 0.5
            shelves.append({"from": [2, y - sag, 1], "to": [14, y + 1 - sag, 15], "faces": shelf_faces})
        shelves.append({"from": [2, 15 - sag / 2, 1], "to": [14, 16 - sag / 2, 15], "faces": shelf_faces})
        elements = posts + shelves
        if bend == 3:
            elements.append({"from": [6, 9, -0.1], "to": [10, 12, 0.5], "faces": uv_faces("tag", [0, 0, 16, 16], [0, 0, 16, 16])})
        block_model(f"{bid}_bend{bend}", {"textures": tex, "elements": elements})
    horizontal_states(bid, extra={f"bend={b}": rl(f"block/{bid}_bend{b}") for b in range(4)})
    block_item(bid, model=rl(f"block/{bid}_bend0"))
    loot_self(bid)
    tag("block", tool, rl(bid))
    name(f"block.autarkia.{bid}", title)


def rack_models():
    rack_block("wood_rack", "Wood Rack", AXE, 2)
    shaped("wood_rack", ["L L", "HHH", "L L"], {"L": "#minecraft:logs", "H": "#minecraft:wooden_slabs"})


def colony():
    geo_single("pattern_loom")
    loot_self("pattern_loom")
    tag("block", AXE, rl("pattern_loom"))
    name("block.autarkia.pattern_loom", "Pattern Loom")
    shaped("pattern_loom", ["BPB", "GLG", "BBB"], {"B": "brass_plate", "P": "minecraft:paper", "G": "steel_gear", "L": "minecraft:loom"})
    item_model("pattern_card", {"parent": "minecraft:item/generated", "textures": {"layer0": rl("item/pattern_card")}})
    item_model("pattern_card_recorded", {"parent": "minecraft:item/generated", "textures": {"layer0": rl("item/pattern_card_recorded")}})
    item_def("pattern_card", obj={"model": {"type": "minecraft:condition", "property": "minecraft:has_component", "component": rl("pattern_ref"),
                                            "on_true": {"type": "minecraft:model", "model": rl("item/pattern_card_recorded")},
                                            "on_false": {"type": "minecraft:model", "model": rl("item/pattern_card")}}})
    name("item.autarkia.pattern_card", "Pattern Card")
    shapeless("pattern_card", ["minecraft:paper", "brass_plate"], 4)
    tag("block", "autarkia:pattern_ignored", "minecraft:fire", "minecraft:soul_fire", "minecraft:short_grass", "minecraft:snow")
    racks = [(x, 0, 0, "wood_rack", NORTH) for x in range(8)]
    plan("rack_row_wood", (8, 1, 1), racks, [0, 0, 0], "none")
    write_json("data/autarkia/colony_template/wood_racks.json", {
        "index": 9, "tier": 2, "triggers": [cond("S09", ">=", 0.85, 1200)], "site": {"rule": "SR_NEAR_STORAGE", "radius": 24}, "base": 45,
        "max_built": 12, "reason": {"key": "storage_full", "signal": "S09", "scale": 100}, "plan": rl("rack_row_wood"), "sign": "storage_full"})
    name("colony_template.autarkia.wood_racks", "Wood Racks")


def workers():
    machine = {"type": rl("worker_assembly"), "worker": rl("hauler_crab"), "frame": {"ingredient": rl("crab_frame"), "count": 1},
               "parts": [{"ingredient": rl("mainspring"), "count": 3}, {"ingredient": rl("steel_gear"), "count": 4},
                         {"ingredient": rl("copper_coil"), "count": 2}, {"ingredient": rl("brass_plate"), "count": 4}],
               "time": 900, "fe_per_tick": 40, "min_tier": 2}
    write_json("data/autarkia/recipe/worker_assembly/hauler_crab.json", machine)
    write_json("data/autarkia/loot_table/entities/hauler_crab.json", {"type": "minecraft:entity", "random_sequence": rl("entities/hauler_crab"),
                                                                       "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rl(i), "modifier": [{"type": "minecraft:set_count", "count": n, "add": False}]}]}
        for i, n in [("crab_frame", 1), ("mainspring", 1), ("steel_gear", 2), ("copper_coil", 1), ("brass_plate", 2)]]})
    tag("entity_type", "autarkia:workers", rl("hauler_crab"))
    name("entity.autarkia.hauler_crab", "Hauler Crab")
    name("entity.autarkia.falling_crate", "Falling Crate")
    name("jade.autarkia.worker.job.collect_debris", "Collect debris")


def equipment():
    tools = [("steel_pickaxe", "Steel Pickaxe", ["PPP", " s ", " s "], "minecraft:pickaxes"),
             ("steel_axe", "Steel Axe", ["PP", "Ps", " s"], "minecraft:axes"),
             ("steel_shovel", "Steel Shovel", ["P", "s", "s"], "minecraft:shovels"),
             ("steel_sword", "Steel Sword", ["P", "P", "s"], "minecraft:swords")]
    for iid, title, pattern, ttag in tools:
        generated_item(iid, handheld=True)
        name(f"item.autarkia.{iid}", title)
        shaped(iid, pattern, {"P": "#c:ingots/steel", "s": "minecraft:stick"}, category="equipment")
        tag("item", ttag, rl(iid))
    generated_item("steel_sledgehammer", handheld=True)
    name("item.autarkia.steel_sledgehammer", "Steel Sledgehammer")
    shaped("steel_sledgehammer", ["RRR", "RGR", " s "], {"R": "steel_plate", "G": "steel_gear", "s": "minecraft:stick"}, category="equipment")
    tag("item", "minecraft:pickaxes", rl("steel_sledgehammer"))
    armor = [("steel_helmet", "Steel Helmet", ["RRR", "R R"], "minecraft:head_armor"),
             ("steel_chest", "Steel Chestplate", ["R R", "RGR", "RRR"], "minecraft:chest_armor"),
             ("steel_legs", "Steel Leggings", ["RRR", "R R", "R R"], "minecraft:leg_armor"),
             ("steel_boots", "Steel Boots", ["R R", "R R"], "minecraft:foot_armor")]
    for iid, title, pattern, atag in armor:
        generated_item(iid)
        name(f"item.autarkia.{iid}", title)
        key = {"R": "steel_plate"} | ({"G": "steel_gear"} if "G" in "".join(pattern) else {})
        shaped(iid, pattern, key, category="equipment")
        tag("item", atag, rl(iid))
    write_json("assets/autarkia/equipment/steel_rig.json", {"layers": {
        "humanoid": [{"texture": "autarkia:steel_rig"}], "humanoid_leggings": [{"texture": "autarkia:steel_rig"}]}})
    item_model("steam_repeater", {"parent": "minecraft:item/handheld", "textures": {"layer0": rl("item/steam_repeater")}})
    item_def("steam_repeater", obj={"model": {"type": "minecraft:special", "base": rl("item/steam_repeater"), "model": {"type": "geckolib:geckolib"}}})
    name("item.autarkia.steam_repeater", "Steam Repeater")
    shaped("steam_repeater", ["RRR", "bGK", " s "], {"R": "steel_plate", "b": "minecraft:bucket", "G": "steel_gear", "K": "brass_plate",
                                                     "s": "minecraft:stick"}, category="equipment")
    tag("item", "minecraft:enchantable/durability", rl("steam_repeater"), rl("steel_sledgehammer"))
    tag("item", "autarkia:steel_tool_materials", rl("steel_ingot"))
    tag("item", "autarkia:repairs_steel_armor", rl("steel_plate"))
    tag("item", "autarkia:steam_repeater_ammo", rl("steel_nugget"))
    generated_item("wire_cutter", handheld=True)
    name("item.autarkia.wire_cutter", "Wire Cutter")
    shaped("wire_cutter", ["S S", " S ", "B B"], {"S": "steel_ingot", "B": "bronze_ingot"}, category="equipment")


def load_tags():
    tag("item", "autarkia:load/light", "minecraft:paper", "minecraft:string", "minecraft:feather", "#minecraft:saplings", "#minecraft:flowers",
        "#minecraft:leaves", "#c:dyes", "#c:foods", "#c:seeds", "#c:nuggets")
    tag("item", "autarkia:load/dense", "#c:ores", "#c:raw_materials", "#c:ingots", "#c:gems", "#autarkia:crushed_ores", "#autarkia:washed_ores",
        "#c:plates", "#c:gears", rl("rope_coil"), rl("copper_wire_spool"))
    tag("item", "autarkia:load/machine", "minecraft:anvil", "minecraft:chipped_anvil", "minecraft:damaged_anvil")
    tag("item", "autarkia:load/heavy", "#c:storage_blocks", rl("foundation"), "minecraft:obsidian", "minecraft:crying_obsidian")


def advancements():
    ev = lambda trigger, event: {"done": {"trigger": rl(trigger), "conditions": {"event": event}}}
    advancement("materials/tin", "Soft Metal", "Find raw tin", "raw_tin", parent="colony/root", criteria=inv_changed("raw_tin"))
    advancement("materials/bronze", "Bronze Age", "Make a bronze ingot", "bronze_ingot", parent="materials/tin", criteria=inv_changed("bronze_ingot"))
    advancement("materials/crushed", "Hammer Time", "Get crushed ore", "crushed_iron", parent="materials/bronze",
                criteria=inv_changed(tag_id="autarkia:crushed_ores"))
    advancement("materials/washed", "Panning Out", "Wash crushed ore in a sluice", "washed_iron", parent="materials/crushed",
                criteria=inv_changed(tag_id="autarkia:washed_ores"))
    advancement("materials/steel", "Forged in Fire", "Make a steel ingot", "steel_ingot", parent="materials/washed", criteria=inv_changed("steel_ingot"))
    advancement("logistics/root", "Down the Slide", "An item you fed into a chute slides out", "chute",
                background="minecraft:gui/advancements/backgrounds/stone")
    advancement("logistics/belt_line", "Assembly Line", "A powered belt line of 32 or more belts delivers 64 items", "belt", parent="logistics/root")
    advancement("logistics/sorted", "Sorted", "A sorting chute or splitter in filter mode moves 256 items", "sorting_chute", parent="logistics/belt_line")
    advancement("logistics/rack_collapse", "Too Much Iron", "Be near when a rack collapses", "wood_rack", parent="logistics/root")
    advancement("workers/debris_crab", "Tidy Claws", "A Hauler Crab collects debris", "crab_frame", parent="workers/first_porter",
                criteria=ev("worker_event", "debris_collected"))
    advancement("equipment/bronze_age", "Bronze Age", "Get any bronze tool", "bronze_pickaxe", parent="colony/root",
                criteria={"got": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [rl(i) for i in (
                    "bronze_pickaxe", "bronze_axe", "bronze_shovel", "bronze_hoe", "bronze_sword")]}]}}})
    advancement("equipment/full_pressure", "Full Pressure", "Fire 10 shots from one full steam repeater charge", "steam_repeater",
                parent="equipment/bronze_age", criteria=ev("equipment_event", "repeater_empty"))
    advancement("colony/pattern_recorded", "Blueprint", "Record your first Pattern at a pattern loom", "pattern_card", parent="colony/root",
                criteria=ev("colony_event", "pattern_recorded"))


def lang():
    jade = {"sluice.none": "Water: none", "sluice.source": "Water: source 50%", "sluice.flowing": "Water: flowing 100%",
            "smelter_heat": "Heat: %s / %s", "no_foundation": "Needs a foundation under every bottom block",
            "belt_line": "Line: %s belts, %s entries", "belt_power_ok": "Power: %s FE/t (OK)", "belt_power_none": "Power: %s FE/t (No power)",
            "belt_running": "State: Running", "belt_jammed": "State: Jammed (%s s)", "filter_alternate": "Mode: Alternate, side %s, filter %s",
            "filter_filter": "Mode: Filter, side %s, filter %s", "side_left": "left", "side_right": "right",
            "rack.load": "Load: %s / %s units (%s%%)", "rack.stage": "Stage %s", "rack.collapse_in": "Collapse in %s s", "rack.crates": "Crates: %s / %s",
            "loom.pattern": "Pattern %s %sx%sx%s, %s blocks", "loom.blank": "Blank card", "loom.no_card": "No card", "loom.scanning": "Scanning %s%%"}
    for k, v in jade.items():
        name(f"jade.autarkia.{k}", v)
    screen = {"filter.mode_alternate": "Mode: Alternate", "filter.mode_filter": "Mode: Filter", "filter.side_left": "Side: Left",
              "filter.side_right": "Side: Right", "filter.whitelist": "Whitelist", "filter.blacklist": "Blacklist",
              "filter.components": "Match components", "filter.item_only": "Item only", "rack.open": "Open", "rack.load": "%s / %s units",
              "rack.countdown": "Collapse in %s s", "loom.protect": "Protect", "loom.library": "Library", "loom.strict": "Strict air",
              "loom.name": "Pattern name", "loom.record": "Record", "loom.offset": "Offset", "loom.size": "Size", "loom.on": "On", "loom.off": "Off", "loom.scanning": "Scanning %s%%", "loom.max": "Max size %s",
              "colony.pattern_add": "Insert held card", "colony.pattern_remove": "Remove", "colony.pattern_header": "Library %s / %s",
              "colony.pattern_none": "No Patterns yet. Record one at a pattern loom.", "colony.pattern_missing": "Missing pattern",
              "colony.pattern_row": "%s (%s), watched %s", "colony.pattern_instances": "Instances: %s",
              "colony.pattern_instance": "%s at %s %s %s, %s", "colony.pattern_watched": "watched", "colony.pattern_unwatched": "not watched",
              "colony.pattern_watch": "Watch", "colony.pattern_unwatch": "Unwatch", "colony.tab.patterns": "Patterns"}
    for k, v in screen.items():
        name(f"screen.autarkia.{k}", v)
    for role in ("generic", "line", "generator", "storage", "defense", "outpost"):
        name(f"screen.autarkia.colony.pattern_role.{role}", role.capitalize())
    msgs = {"pattern.busy": "The loom is already scanning", "pattern.no_blank": "Put a blank pattern card in the loom",
            "pattern.no_rights": "Only the owner or trusted players of this domain can record", "pattern.tier": "Patterns need a T2 colony",
            "pattern.too_big": "The box is larger than the colony tier allows", "pattern.unloaded": "Part of the box is not loaded",
            "pattern.recorded": "Pattern %s recorded: %s blocks, %s foreign block entities placed empty. %s",
            "pattern.in_library": "Added to the library.", "pattern.not_in_library": "Not added to the library.",
            "pattern.hold_card": "Hold a recorded pattern card", "pattern.missing": "Missing pattern", "pattern.already": "This Pattern is already in the library",
            "pattern.library_full": "The library is full", "cutter.not_a_node": "Not a connector or machine port",
            "cutter.start": "First end set; click the other node of the span", "cutter.no_span": "No span joins these two nodes",
            "cutter.cut": "Span removed, %s cable items returned"}
    for k, v in msgs.items():
        name(f"message.autarkia.{k}", v)
    name("tooltip.autarkia.pattern_card.blank", "Blank card")
    name("tooltip.autarkia.pattern_card.pattern", "Pattern %s %sx%sx%s, %s blocks")
    for m in ("sluice", "blast_smelter", "plate_press", "wire_drawer", "belt_splitter", "sorting_chute", "rack", "pattern_loom"):
        name(f"menu.autarkia.{m}", m.replace("_", " ").title())
    for c, v in (("washing", "Washing"), ("pressing", "Pressing"), ("drawing", "Wire drawing")):
        name(f"jei.autarkia.category.{c}", v)
    info = {
        "zinc_ore": "Zinc ore, Y 0 to 64. Drops raw zinc.", "deepslate_zinc_ore": "Zinc ore in deepslate. Drops raw zinc.",
        "sluice": "1 x 1 x 3. Washes crushed ore: 1 crushed to 1 washed plus a secondary nugget at 10%, 100 ticks. The block behind or above the head needs water: flowing water 100%, a source only 50%. Outputs go into an inventory or belt in front of the tail, else drop.",
        "blast_smelter": "2 x 2 x 2 heavy machine on foundation. 64 FE/t heats it to 1,000 (+10 per tick); it works only when hot. 3 washed ore to 4 ingots, 1 iron + 2 coal to steel, 3 copper + 1 zinc to 4 brass, and any furnace recipe in 100 ticks.",
        "plate_press": "48 FE/t, 80 ticks: 1 ingot to 1 plate (steel, brass).",
        "wire_drawer": "40 FE/t, 100 ticks: 4 copper + iron core to 2 copper coils; 2 copper with no core to 1 copper wire spool.",
        "belt": "Conveyor, 2 blocks/s, 2 entries of up to 8 items per block. All belts of a line share the head's 1,000 FE buffer and use 1 FE/t each. A cable cannot end on a belt: put a connector node (rope pulley, wooden pole or wall insulator) next to any belt of the line to power the whole line. Inputs at the back, the sides and the top; output into the block in front. A blocked line jams after 30 s.",
        "belt_splitter": "Inline: Alternate sends entries front and side in turn (the other output when one is full); Filter sends matching entries to the side. 2 FE/t.",
        "sorting_chute": "Inline, no power: entries that match the filter leave through the side, the rest go on. A full side blocks it.",
        "wood_rack": "Holds 2 crates (filled or empty). Load rating 2,048 units: light items 0.25, most items 0.5, ingots and ores 1, machines 8, storage blocks 9, each crate 8. Above 100% it bends, above 120% it collapses after 60 s, above 150% (with a falling crate's impact) at once.",
        "pattern_loom": "Records a box of up to the tier size into a Pattern on a blank card: Autarkia machine settings are kept, foreign block entities are placed empty. Protect watches the box; Library adds the Pattern to the colony.",
        "pattern_card": "Blank, or a recorded Pattern. Insert a recorded card in the Core's Patterns tab to add it to the library.",
        "crab_frame": "The frame of a Hauler Crab or Loader.", "wire_cutter": "Click one node, then the other: the span is removed and its cable comes back.",
        "steel_pickaxe": "Mines what a diamond pickaxe mines. Speed 7.5, 1,200 uses.", "steel_axe": "Strips logs, disables shields for 5 s. 9 damage.",
        "steel_shovel": "Makes paths. Speed 7.5, 1,200 uses.", "steel_sword": "6.5 damage, 1.6 attacks per second, sweep attack.",
        "steel_sledgehammer": "Mode key: 1x1 or 3x3. 3x3 breaks the plane at right angles to the hit face at 35% speed, 1 durability per block. 10.5 damage, disables shields.",
        "steel_helmet": "3 armor, 1 toughness, +4% block break speed.", "steel_chest": "7 armor, 1 toughness, +4% block break speed.",
        "steel_legs": "5 armor, 1 toughness, +4% block break speed.", "steel_boots": "2 armor, 1 toughness, +4% block break speed.",
        "steam_repeater": "Sneak + hold use pumps pressure (40 ticks to 100). Hold use fires a steel nugget every 5 ticks for 10 pressure: 5 damage. Pressure leaks 1 per second when not in use.",
        "steel_ingot": "Blast smelter: 1 iron ingot + 2 coal.", "brass_ingot": "Crucible or blast smelter: 3 copper + 1 zinc.",
        "slag": "Byproduct of the blast smelter. 4 slag make 2 foundation.", "copper_coil": "Wire drawer: 4 copper + iron core.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def generate():
    ores()
    materials()
    processing()
    belt_models()
    rack_models()
    colony()
    workers()
    equipment()
    load_tags()
    advancements()
    lang()

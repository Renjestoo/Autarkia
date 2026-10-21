"""M7 content data, materials part (03): vantite ore, storage blocks and forms, the concentrates, the advanced circuit,
the ore outcrop and magma vent blocks, the froth flotation plant, the M7 recipe rows of 03 section 5, the deposit and vent
worldgen, tags, advancements and language."""

from datagen import (advancement, biome_modifier, block_item, block_model, blockstate, cooking, cube_all, generated_item, height_range, inv_changed,
                     loot_center_only, loot_ore, loot_self, machine_recipe, name, ore_feature, ore_targets, particle_model, placed_feature, rl, shaped,
                     shapeless, simple_blockstate, tag, write_json)

PICK = "minecraft:mineable/pickaxe"
DIAMOND_TOOL = "minecraft:needs_diamond_tool"
IRON_TOOL = "minecraft:needs_iron_tool"
SIDES = ("north", "south", "east", "west", "up", "down")
METALS = ["iron", "copper", "gold", "tin", "zinc", "vantite"]
INGOT = {"iron": "minecraft:iron_ingot", "copper": "minecraft:copper_ingot", "gold": "minecraft:gold_ingot", "tin": "tin_ingot", "zinc": "zinc_ingot",
         "vantite": "vantite_ingot"}
SECONDARY = {"iron": "zinc_nugget", "copper": "minecraft:gold_nugget", "gold": "minecraft:copper_nugget", "tin": "minecraft:iron_nugget",
             "zinc": "minecraft:copper_nugget", "vantite": "minecraft:gold_nugget"}
RAW = {"iron": "minecraft:raw_iron", "copper": "minecraft:raw_copper", "gold": "minecraft:raw_gold", "tin": "raw_tin", "zinc": "raw_zinc",
       "vantite": "raw_vantite"}
DEPOSITS = [("tin", 20, 6, 0, 64, 12, "tin_ore", "deepslate_tin_ore"), ("zinc", 18, 8, 8, 48, 12, "zinc_ore", "deepslate_zinc_ore"),
            ("vantite", 12, 16, -56, -16, 8, "vantite_ore", "vantite_ore"),
            ("iron", 20, 8, -8, 56, 12, "minecraft:iron_ore", "minecraft:deepslate_iron_ore"),
            ("copper", 20, 8, 0, 64, 12, "minecraft:copper_ore", "minecraft:deepslate_copper_ore"),
            ("gold", 12, 16, -48, -8, 8, "minecraft:gold_ore", "minecraft:deepslate_gold_ore")]


def simple_block(bid, title, needs):
    cube_all(bid)
    simple_blockstate(bid)
    block_item(bid)
    name(f"block.autarkia.{bid}", title)
    tag("block", PICK, rl(bid))
    tag("block", needs, rl(bid))


def vantite():
    simple_block("vantite_ore", "Vantite Ore", DIAMOND_TOOL)
    loot_ore("vantite_ore", "raw_vantite")
    tag("block", "c:ores/vantite", rl("vantite_ore"))
    tag("block", "c:ores", "#c:ores/vantite")
    tag("block", "c:ores_in_ground/deepslate", rl("vantite_ore"))
    tag("item", "c:ores/vantite", rl("vantite_ore"))
    tag("item", "c:ores", "#c:ores/vantite")
    for bid, title, ctag, part in [("raw_vantite_block", "Block of Raw Vantite", "raw_vantite", "raw_vantite"),
                                   ("vantite_block", "Block of Vantite", "vantite", "vantite_ingot"),
                                   ("vantite_steel_block", "Block of Vantite Steel", "vantite_steel", "vantite_steel_ingot")]:
        simple_block(bid, title, DIAMOND_TOOL)
        loot_self(bid)
        tag("block", f"c:storage_blocks/{ctag}", rl(bid))
        tag("block", "c:storage_blocks", f"#c:storage_blocks/{ctag}")
        tag("item", f"c:storage_blocks/{ctag}", rl(bid))
        tag("item", "c:storage_blocks", f"#c:storage_blocks/{ctag}")
        shaped(bid, ["XXX", "XXX", "XXX"], {"X": part}, category="building")
        shapeless(part, [bid], 9, rid=f"{part}_from_{bid}")
    items = [("raw_vantite", "Raw Vantite"), ("vantite_ingot", "Vantite Ingot"), ("vantite_steel_ingot", "Vantite Steel Ingot"),
             ("crushed_vantite", "Crushed Vantite Ore"), ("washed_vantite", "Washed Vantite Ore"), ("pure_vantite", "Pure Vantite Ore"),
             ("advanced_circuit", "Advanced Circuit")] + [(f"{m}_concentrate", f"{m.capitalize()} Concentrate") for m in METALS]
    for iid, title in items:
        generated_item(iid)
        name(f"item.autarkia.{iid}", title)
    tag("item", "c:raw_materials/vantite", rl("raw_vantite"))
    tag("item", "c:raw_materials", "#c:raw_materials/vantite")
    for m in ("vantite", "vantite_steel"):
        tag("item", f"c:ingots/{m}", rl(f"{m}_ingot"))
        tag("item", "c:ingots", f"#c:ingots/{m}")
    tag("item", "autarkia:crushed_ores", rl("crushed_vantite"))
    tag("item", "c:crushed_ores/vantite", rl("crushed_vantite"))
    tag("item", "autarkia:washed_ores", rl("washed_vantite"))
    tag("item", "autarkia:pure_ores", rl("pure_vantite"))
    tag("item", "c:pure_ores/vantite", rl("pure_vantite"))
    for m in METALS:
        tag("item", "autarkia:concentrates", rl(f"{m}_concentrate"))
    tag("item", "autarkia:load/dense", "#autarkia:concentrates")


def outcrop():
    piles = [([2, 0, 3], [10, 4, 11]), ([7, 0, 2], [14, 3, 9]), ([4, 3, 5], [9, 6, 10]), ([9, 0, 9], [13, 2, 14])]
    rock = [{"from": a, "to": b, "faces": {k: {"texture": "#rock"} for k in SIDES}} for a, b in piles]
    def uvs(a, b):
        return {"north": [16 - b[0], 16 - b[1], 16 - a[0], 16 - a[1]], "south": [a[0], 16 - b[1], b[0], 16 - a[1]],
                "east": [16 - b[2], 16 - b[1], 16 - a[2], 16 - a[1]], "west": [a[2], 16 - b[1], b[2], 16 - a[1]], "up": [a[0], a[2], b[0], b[2]]}
    specks = [{"from": [v - 0.01 for v in a], "to": [v + 0.01 for v in b], "faces": {k: {"texture": "#specks", "uv": uv} for k, uv in uvs(a, b).items()}}
              for a, b in piles]
    variants = {}
    for ore in METALS:
        block_model(f"ore_outcrop_{ore}", {"parent": "minecraft:block/block", "textures": {
            "particle": rl("block/ore_outcrop"), "rock": rl("block/ore_outcrop"), "specks": rl(f"block/ore_outcrop_{ore}")}, "elements": rock + specks})
        for wet in ("false", "true"):
            variants[f"ore={ore},waterlogged={wet}"] = {"model": rl(f"block/ore_outcrop_{ore}")}
    blockstate("ore_outcrop", {"variants": variants})
    block_item("ore_outcrop", model=rl("block/ore_outcrop_iron"))
    children = [{"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": rl("ore_outcrop"),
                 "modifier": [{"type": "minecraft:copy_state", "block": rl("ore_outcrop"), "properties": ["ore"]}]}]
    for ore in METALS:
        drop = RAW[ore]
        children.append({"type": "minecraft:item", "name": drop if ":" in drop else rl(drop),
                         "condition": {"type": "minecraft:match_block", "blocks": rl("ore_outcrop"), "state": {"ore": ore}},
                         "modifier": [{"type": "minecraft:explosion_decay"}]})
    write_json("data/autarkia/loot_table/blocks/ore_outcrop.json", {"type": "minecraft:block", "random_sequence": rl("blocks/ore_outcrop"),
                                                                     "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": children}]}]})
    name("block.autarkia.ore_outcrop", "Ore Outcrop")
    cube_all("magma_vent")
    simple_blockstate("magma_vent")
    block_item("magma_vent")
    write_json("data/autarkia/loot_table/blocks/magma_vent.json", {"type": "minecraft:block", "random_sequence": rl("blocks/magma_vent"), "pools": [
        {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": rl("magma_vent")},
            {"type": "minecraft:item", "name": "minecraft:magma_block"}]}]}]})
    tag("block", PICK, rl("magma_vent"))
    name("block.autarkia.magma_vent", "Magma Vent")


def worldgen():
    for ore, size, chance, lo, hi, need, stone, deep in DEPOSITS:
        write_json(f"data/autarkia/worldgen/feature/{ore}_deposit.json", {
            "type": rl("outcrop_ore"), "size": size, "discard_chance_on_air_exposure": 0.0, "targets": ore_targets(stone, deep), "ore": ore,
            "min_blocks": need, "max_depth": 128})
        placed_feature(f"{ore}_deposit", f"{ore}_deposit", [{"type": "minecraft:rarity_filter", "chance": chance}, {"type": "minecraft:in_square"},
                                                            height_range("uniform", lo, hi), {"type": "minecraft:biome"}])
    ore_feature("vantite_ore", 5, ore_targets("vantite_ore", "vantite_ore"), discard=0.5)
    placed_feature("vantite_ore", "vantite_ore", [{"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"},
                                                  height_range("trapezoid", -64, 0), {"type": "minecraft:biome"}])
    scan = {"type": "minecraft:environment_scan", "direction_of_search": "down", "max_steps": 16, "target_condition": {"type": "minecraft:solid"},
            "allowed_search_condition": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}}
    for fid, stone, first, lo, hi in (("magma_vent", "minecraft:base_stone_overworld", {"type": "minecraft:rarity_filter", "chance": 8}, -60, -16),
                                      ("magma_vent_nether", "minecraft:base_stone_nether", {"type": "minecraft:count", "count": 1}, 10, 60)):
        write_json(f"data/autarkia/worldgen/feature/{fid}.json", {"type": "minecraft:replace_single_block", "targets": [
            {"target": {"predicate_type": "minecraft:tag_match", "tag": stone}, "state": rl("magma_vent")}]})
        placed_feature(fid, fid, [first, {"type": "minecraft:in_square"}, height_range("uniform", lo, hi), scan, {"type": "minecraft:biome"}])
    biome_modifier("vantite_ore", "#minecraft:is_overworld", ["vantite_ore", "vantite_deposit"], "underground_ores")
    biome_modifier("vanilla_deposits", "#minecraft:is_overworld", ["iron_deposit", "copper_deposit", "gold_deposit"], "underground_ores")
    biome_modifier("magma_vent", "#minecraft:is_overworld", ["magma_vent"], "underground_decoration")
    biome_modifier("magma_vent_nether", "#minecraft:is_nether", ["magma_vent_nether"], "underground_decoration")


def flotation_plant():
    particle_model("froth_flotation_plant")
    simple_blockstate("froth_flotation_plant")
    generated_item("froth_flotation_plant")
    loot_center_only("froth_flotation_plant")
    tag("block", PICK, rl("froth_flotation_plant"))
    tag("block", IRON_TOOL, rl("froth_flotation_plant"))
    name("block.autarkia.froth_flotation_plant", "Froth Flotation Plant")
    name("menu.autarkia.froth_flotation_plant", "Froth Flotation Plant")
    shaped("froth_flotation_plant", ["VAV", "CKC", "VSV"], {"V": "vantite_steel_ingot", "A": "advanced_circuit", "C": "minecraft:cauldron", "K": "servo",
                                                             "S": "steel_block"})


def recipes():
    machine_recipe("crushing", "vantite_from_raw", [("#c:raw_materials/vantite", 1)], [("crushed_vantite", 1), ("crushed_vantite", 1, 0.5)],
                   stations=["quern", "stamp_mill"])
    machine_recipe("crushing", "vantite_jaw", [("#c:raw_materials/vantite", 1)], [("crushed_vantite", 2)], stations=["jaw_crusher"])
    machine_recipe("washing", "washed_vantite", [("crushed_vantite", 1)], [("washed_vantite", 1), (SECONDARY["vantite"], 1, 0.1)])
    machine_recipe("alloying", "vantite_from_washed", [("washed_vantite", 3)], [("vantite_ingot", 4), ("slag", 1, 0.3)], stations=["blast_smelter"], time=200)
    machine_recipe("separating", "pure_vantite", [("washed_vantite", 2)], [("pure_vantite", 3), ("slag", 1), (SECONDARY["vantite"], 1, 0.5)])
    machine_recipe("electrolyzing", "vantite_from_pure", [("pure_vantite", 1)], [("vantite_ingot", 1), (SECONDARY["vantite"], 1, 0.1)])
    for form in ("raw", "crushed", "pure", "concentrate"):
        src = {"raw": "raw_vantite", "crushed": "crushed_vantite", "pure": "pure_vantite", "concentrate": "vantite_concentrate"}[form]
        machine_recipe("alloying", f"vantite_ingot_from_{form}", [(src, 1)], [("vantite_ingot", 1)], stations=["blast_smelter"], time=200)
    for m in METALS:
        machine_recipe("flotation", f"{m}_concentrate", [(f"crushed_{m}", 1)], [(f"{m}_concentrate", 2), ("slag", 1, 0.5), (SECONDARY[m], 1, 0.35)], water=250)
        machine_recipe("electrolyzing", f"{m}_from_concentrate", [(f"{m}_concentrate", 1)], [(INGOT[m], 1), (SECONDARY[m], 1, 0.1)])
        if m != "vantite":
            for kind in ("smelting", "blasting"):
                cooking(kind, INGOT[m], f"{m}_concentrate", rid=f"{kind}/{m}_ingot_from_concentrate")
    machine_recipe("alloying", "vantite_steel_ingot", [("#c:ingots/vantite", 1), ("#c:ingots/steel", 1)], [("vantite_steel_ingot", 1)],
                   stations=["blast_smelter"], time=300)
    machine_recipe("electrolyzing", "advanced_circuit", [("basic_circuit", 2), ("#c:ingots/vantite", 1)], [("advanced_circuit", 1)], time=400)
    machine_recipe("fabricating", "outpost_seed", [("advanced_circuit", 1), ("servo", 2), ("crate", 1), ("vantite_steel_ingot", 4), ("minecraft:oak_sapling", 1)],
                   [("outpost_seed", 1)], time=400, energy=-(-64000 // 400))


def advancements():
    advancement("materials/concentrate", "Bubbles of Ore", "Get an ore concentrate from a froth flotation plant", "gold_concentrate",
                parent="materials/pure", criteria=inv_changed(tag_id="autarkia:concentrates"))
    advancement("materials/vantite", "Deep Blue", "Make a vantite ingot", "vantite_ingot", parent="materials/pure", criteria=inv_changed("vantite_ingot"))


def lang():
    name("jei.autarkia.category.flotation", "Froth flotation")
    name("jei.autarkia.water", "%s mB")
    name("jade.autarkia.flotation_water", "Water: %s / %s mB")
    name("jade.autarkia.outcrop", "Marks a %s deposit below")
    for m in METALS:
        name(f"jade.autarkia.outcrop.{m}", m)
    name("config.jade.plugin_autarkia.ore_outcrop", "Autarkia ore outcrops")
    info = {
        "vantite_ore": "Deep ore from Y -64 to 0, rarely exposed to caves. Needs a steel or diamond pickaxe; drops raw vantite. Vantite does not smelt in "
                       "a furnace: use the blast smelter, electrolyzer or the flotation chain.",
        "raw_vantite": "Jaw crusher: 2 crushed vantite. Blast smelter: 1 vantite ingot.",
        "vantite_ingot": "Blast smelter, electrolyzer or flotation chain only. With steel in the blast smelter: vantite steel.",
        "vantite_steel_ingot": "Blast smelter: 1 vantite ingot + 1 steel ingot, 15 s. For T4 machines and vantite equipment.",
        "advanced_circuit": "Electrolyzer: 2 basic circuits + 1 vantite ingot, 20 s. For T4 machines and outposts.",
        "ore_outcrop": "A small rock pile on the surface. It marks an ore deposit below its column: at least 12 ore blocks of its kind (8 for gold and "
                       "vantite). Breaking it gives 1 raw ore of its kind.",
        "magma_vent": "Glowing vent in cave floors (Y -60 to -16, about 1 chunk in 8) and in the Nether (Y 10 to 60). An endless lava source for the "
                      "Magma Tap. Drops a magma block; silk touch keeps the vent.",
        "froth_flotation_plant": "3 x 3 x 3 heavy machine on foundation. 768 FE/t, 10 s: 1 crushed ore + 250 mB water to 2 concentrate, slag at 50% "
                                 "and a secondary nugget at 35%. Water from the bucket slot, from pipes, or pumped at 50 mB/t from a water source "
                                 "next to the bottom layer.",
    }
    for m in METALS:
        info[f"{m}_concentrate"] = ("Froth flotation: 1 crushed ore to 2 concentrate. Blast smelter or electrolyzer to 1 vantite ingot." if m == "vantite"
                                    else "Froth flotation: 1 crushed ore to 2 concentrate. Smelts, blasts or electrolyzes to 1 ingot.")
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def generate():
    vantite()
    outcrop()
    worldgen()
    flotation_plant()
    recipes()
    advancements()
    lang()

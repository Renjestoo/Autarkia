"""M8 content data, materials part (03, T5): the lattice geode blocks and worldgen, lattice_block, the lattice alloy
storage block, the T5 materials, components and cells, core_upgrade_t5, the plasma separator with its plasma_separating
and alloying recipes, tags, advancements and language."""

from content_m6 import facing_states, geo_block
from datagen import (advancement, biome_modifier, block_item, block_model, blockstate, cube_all, generated_item, height_range, inv_changed, item_def,
                     item_model, loot_ore, loot_self, machine_recipe, name, placed_feature, rl, shaped, shapeless, simple_blockstate, tag, write_json)

PICK = "minecraft:mineable/pickaxe"
NETHERITE_TOOL = "neoforge:needs_netherite_tool"
DIAMOND_TOOL = "minecraft:needs_diamond_tool"
METALS = ["iron", "copper", "gold", "tin", "zinc", "vantite"]
INGOT = {"iron": "minecraft:iron_ingot", "copper": "minecraft:copper_ingot", "gold": "minecraft:gold_ingot", "tin": "tin_ingot", "zinc": "zinc_ingot",
         "vantite": "vantite_ingot"}
SECONDARY_INGOT = {"iron": "zinc_ingot", "copper": "minecraft:gold_ingot", "gold": "minecraft:copper_ingot", "tin": "minecraft:iron_ingot",
                   "zinc": "minecraft:copper_ingot", "vantite": "minecraft:gold_ingot"}
FABRICATING_FE_PER_TICK = 160


def geode_blocks():
    block_model("lattice_cluster", {"parent": "minecraft:block/cross", "textures": {"cross": rl("block/lattice_cluster")}})
    facing_states("lattice_cluster", "lattice_cluster")
    item_model("lattice_cluster", {"parent": "minecraft:item/generated", "textures": {"layer0": rl("block/lattice_cluster")}})
    item_def("lattice_cluster")
    loot_ore("lattice_cluster", "lattice_shard", count=2)
    tag("block", PICK, rl("lattice_cluster"))
    tag("block", NETHERITE_TOOL, rl("lattice_cluster"))
    name("block.autarkia.lattice_cluster", "Lattice Cluster")

    cube_all("lattice_geode_shell")
    cube_all("lattice_geode_shell_charged")
    blockstate("lattice_geode_shell", {"variants": {"charged=false": {"model": rl("block/lattice_geode_shell")},
                                                    "charged=true": {"model": rl("block/lattice_geode_shell_charged")}}})
    block_item("lattice_geode_shell")
    write_json("data/autarkia/loot_table/blocks/lattice_geode_shell.json", {
        "type": "minecraft:block", "random_sequence": rl("blocks/lattice_geode_shell"),
        "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                   "entries": [{"type": "minecraft:item", "name": rl("lattice_geode_shell"),
                                "condition": {"type": "minecraft:match_block", "blocks": rl("lattice_geode_shell"), "state": {"charged": "false"}}}]}]})
    tag("block", PICK, rl("lattice_geode_shell"))
    name("block.autarkia.lattice_geode_shell", "Lattice Geode Shell")


def storage_blocks():
    for bid, title, needs in (("lattice_alloy_block", "Block of Lattice Alloy", NETHERITE_TOOL), ("lattice_block", "Lattice Block", DIAMOND_TOOL)):
        cube_all(bid)
        simple_blockstate(bid)
        block_item(bid)
        loot_self(bid)
        tag("block", PICK, rl(bid))
        tag("block", needs, rl(bid))
        name(f"block.autarkia.{bid}", title)
    for kind in ("block", "item"):
        tag(kind, "c:storage_blocks/lattice_alloy", rl("lattice_alloy_block"))
        tag(kind, "c:storage_blocks", "#c:storage_blocks/lattice_alloy")
    shaped("lattice_alloy_block", ["XXX", "XXX", "XXX"], {"X": "lattice_alloy_ingot"}, category="building")
    shapeless("lattice_alloy_ingot", ["lattice_alloy_block"], 9, rid="lattice_alloy_ingot_from_lattice_alloy_block")
    shaped("lattice_block", ["SSS", "SLS", "SSS"], {"S": "minecraft:stone", "L": "lattice_alloy_ingot"}, count=8, category="building")


def worldgen():
    write_json("data/autarkia/worldgen/feature/lattice_geode.json", {
        "type": "minecraft:geode",
        "blocks": {
            "filling_provider": {"id": "minecraft:air"},
            "inner_layer_provider": {"id": "autarkia:lattice_geode_shell", "properties": {"charged": "false"}},
            "alternate_inner_layer_provider": {"id": "autarkia:lattice_geode_shell", "properties": {"charged": "true"}},
            "middle_layer_provider": {"id": "minecraft:calcite"},
            "outer_layer_provider": {"id": "minecraft:blackstone"},
            "inner_placements": ["autarkia:lattice_cluster"],
            "cannot_replace": "#minecraft:features_cannot_replace",
            "invalid_blocks": "#minecraft:geode_invalid_blocks"},
        "layers": {"filling": 2.2, "inner_layer": 2.8, "middle_layer": 3.8, "outer_layer": 4.8},
        "crack": {"generate_crack_chance": 0.6, "base_crack_size": 2.0, "crack_point_offset": 2},
        "use_alternate_layer0_chance": 0.06,
        "outer_wall_distance": {"type": "minecraft:uniform", "min_inclusive": 5, "max_inclusive": 7},
        "distribution_points": {"type": "minecraft:uniform", "min_inclusive": 4, "max_inclusive": 6},
        "min_gen_offset": -15, "max_gen_offset": 15,
        "invalid_blocks_threshold": 1})
    placed_feature("lattice_geode", "lattice_geode", [{"type": "minecraft:rarity_filter", "chance": 40}, {"type": "minecraft:in_square"},
                                                      height_range("uniform", -58, -24), {"type": "minecraft:biome"}])
    biome_modifier("lattice_geode", "#minecraft:is_overworld", ["lattice_geode"], "underground_structures")


def items():
    for iid, title in (("lattice_shard", "Lattice Shard"), ("lattice_crystal", "Lattice Crystal"), ("lattice_alloy_ingot", "Lattice Alloy Ingot"),
                       ("nanite_paste", "Nanite Paste"), ("ai_core", "AI Core"), ("lattice_fuel_cell", "Lattice Fuel Cell"),
                       ("coolant_cell", "Coolant Cell"), ("core_upgrade_t5", "Core Upgrade T5")):
        generated_item(iid)
        name(f"item.autarkia.{iid}", title)
    tag("item", "c:ingots/lattice_alloy", rl("lattice_alloy_ingot"))
    tag("item", "c:ingots", "#c:ingots/lattice_alloy")
    shaped("lattice_crystal", ["SS", "SS"], {"S": "lattice_shard"})


def plasma_separator():
    geo_block("plasma_separator", "Plasma Separator")
    name("menu.autarkia.plasma_separator", "Plasma Separator")
    shaped("plasma_separator", ["VLV", "ANA", "VBV"], {"V": "vantite_steel_ingot", "L": "lattice_crystal", "A": "advanced_circuit", "N": "nanite_paste",
                                                       "B": "vantite_steel_block"})


def fabricating(iid, inputs, count, ticks):
    machine_recipe("fabricating", iid, inputs, [(iid, count)], time=ticks, energy=FABRICATING_FE_PER_TICK)


def recipes():
    for m in METALS:
        machine_recipe("plasma_separating", f"{m}_from_raw", [(f"#c:raw_materials/{m}", 1)], [(INGOT[m], 6), (SECONDARY_INGOT[m], 1, 0.2)])
    machine_recipe("alloying", "lattice_alloy_ingot", [("lattice_crystal", 1), ("#c:ingots/vantite_steel", 1)], [("lattice_alloy_ingot", 1)],
                   stations=["plasma_separator"], time=200, energy=4096)
    fabricating("nanite_paste", [("lattice_shard", 1), ("minecraft:redstone", 4), ("minecraft:slime_ball", 1)], 2, 200)
    fabricating("ai_core", [("advanced_circuit", 4), ("lattice_crystal", 1), ("nanite_paste", 2), ("minecraft:diamond", 1)], 1, 400)
    fabricating("lattice_fuel_cell", [("lattice_shard", 2), ("#c:ingots/vantite_steel", 1), ("minecraft:blaze_powder", 1)], 1, 200)
    fabricating("coolant_cell", [("#c:ingots/vantite_steel", 1), ("minecraft:blue_ice", 2), ("lattice_shard", 1)], 2, 200)
    fabricating("core_upgrade_t5", [("ai_core", 1), ("#c:ingots/lattice_alloy", 8), ("nanite_paste", 8)], 1, 200)


def advancements():
    advancement("materials/lattice", "Crystal Lattice", "Mine a lattice shard from a cluster in a lattice geode", "lattice_shard", parent="materials/vantite",
                criteria=inv_changed("lattice_shard"))
    advancement("materials/plasma", "Six From One", "Make a lattice alloy ingot in a plasma separator", "lattice_alloy_ingot", parent="materials/lattice",
                criteria=inv_changed("lattice_alloy_ingot"))


def lang():
    name("jei.autarkia.category.plasma_separating", "Plasma separation")
    name("jade.autarkia.charged", "Charged")
    name("config.jade.plugin_autarkia.lattice_geode_shell", "Autarkia charged geode shells")
    info = {
        "lattice_cluster": "Grows in lattice geodes deep underground (Y -58 to -24, about 1 chunk in 40): blackstone outside, calcite, then the shell. "
                           "Clusters sit only on charged shells. Needs a vantite or better pickaxe: 2 lattice shards (fortune adds more); silk touch "
                           "keeps the cluster.",
        "lattice_geode_shell": "Inner layer of a lattice geode (Y -58 to -24). Charged shells grow new lattice clusters on free faces over time, "
                               "cannot be moved by pistons and break into nothing; plain shells drop themselves.",
        "lattice_shard": "From lattice clusters. 4 shards craft a lattice crystal; the fabricator turns shards into nanite paste and cells.",
        "lattice_crystal": "4 lattice shards. Plasma separator: 1 crystal + 1 vantite steel ingot to 1 lattice alloy ingot.",
        "lattice_alloy_ingot": "Plasma separator alloying: 1 lattice crystal + 1 vantite steel ingot, 10 s at 4,096 FE/t. For T5 machines and lattice "
                               "gear.",
        "plasma_separator": "2 x 2 x 2 heavy machine on foundation. 4,096 FE/t, 5 s: 1 raw ore to 6 ingots and a secondary ingot at 20%. With a "
                            "second input it alloys: lattice crystal + vantite steel ingot to lattice alloy.",
        "lattice_fuel_cell": "Fabricator: 2 lattice shards, 1 vantite steel ingot, 1 blaze powder. Fuel of the plasma generator.",
        "coolant_cell": "Fabricator: 1 vantite steel ingot, 2 blue ice, 1 lattice shard to 2 cells. Keeps superconductors cold.",
        "nanite_paste": "Fabricator: 1 lattice shard, 4 redstone, 1 slime ball to 2 paste. For the Nanite Hive, the Replicator and lattice gear.",
        "ai_core": "Fabricator, 20 s: 4 advanced circuits, 1 lattice crystal, 2 nanite paste, 1 diamond. For Replicator Walkers.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def generate():
    print("content_m8a: materials")
    geode_blocks()
    storage_blocks()
    print("  lattice_cluster, lattice_geode_shell, lattice_alloy_block, lattice_block")
    worldgen()
    print("  worldgen lattice_geode (feature, placed feature, biome modifier)")
    items()
    plasma_separator()
    recipes()
    print("  8 items, plasma_separator, 6 plasma_separating, 1 alloying, 5 fabricating and 5 crafting recipes")
    advancements()
    lang()
    print("content_m8a: done")

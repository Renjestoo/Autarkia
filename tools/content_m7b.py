"""M7 content data, power part (02): the hv_pylon node, the HV cable spool, the hydro turbine, the wind tower and the
magma tap, with recipes, loot, tags, Jade lines and JEI info pages. The magma_vent block data comes with the materials
part."""

from datagen import generated_item, loot_center_only, name, particle_model, rl, shaped, simple_blockstate, tag

PICK = "minecraft:mineable/pickaxe"
IRON_TOOL = "minecraft:needs_iron_tool"


def geo_block(bid, title):
    particle_model(bid)
    simple_blockstate(bid)
    generated_item(bid)
    loot_center_only(bid)
    tag("block", PICK, rl(bid))
    tag("block", IRON_TOOL, rl(bid))
    name(f"block.autarkia.{bid}", title)


def power():
    for bid, title in (("hv_pylon", "HV Pylon"), ("hydro_turbine", "Hydro Turbine"), ("wind_tower", "Wind Tower"), ("magma_tap", "Magma Tap")):
        geo_block(bid, title)
    generated_item("hv_cable_spool")
    name("item.autarkia.hv_cable_spool", "HV Cable Spool")
    tag("item", "autarkia:load/dense", rl("hv_cable_spool"))
    shaped("hv_pylon", ["BVB", "VPV", "V V"], {"B": "minecraft:brick", "V": "vantite_steel_ingot", "P": "steel_plate"})
    shaped("hv_cable_spool", [" V ", "VIV", " V "], {"V": "vantite_steel_ingot", "I": "insulated_cable_spool"}, 2)
    shaped("hydro_turbine", ["SPS", "GAG", "SCS"], {"S": "vantite_steel_block", "P": "steel_plate", "G": "steel_gear", "A": "advanced_circuit",
                                                     "C": "copper_coil"})
    shaped("wind_tower", ["VWV", "CAC", "VMV"], {"V": "vantite_steel_ingot", "W": "#minecraft:wool", "C": "copper_coil", "A": "advanced_circuit",
                                                  "M": "windmill"})
    shaped("magma_tap", ["VCV", "VAV", "OPO"], {"V": "vantite_steel_ingot", "C": "copper_coil", "A": "advanced_circuit", "O": "minecraft:obsidian",
                                                 "P": "steel_plate"})


def lang():
    name("jade.autarkia.head", "Head: %s blocks")
    info = {
        "hv_pylon": "1 x 7 x 1 node for HV line, 4 spans. Feeds the machines next to its base. Spans attach at the top arms.",
        "hv_cable_spool": "HV line: 8,192 FE/t, spans up to 48 blocks, loss 0.02% per block. One spool per 16 blocks. Overload heats it; wind cools "
                          "it and makes it sway. A snapped HV line whips for 10 s with a large arc: 10 damage and fire.",
        "hydro_turbine": "3 x 3 x 3 generator. Head = water blocks in a straight column above the top-center intake, max 32. Output 128 FE/t per "
                         "block of head, max 4,096. The outlet in front of the bottom center must be air or water. Water is not used up. "
                         "Buffer 50,000 FE, out 8,192 FE/t.",
        "wind_tower": "Generator, 8 blocks tall. Base 512 FE/t at rotor height 64, +20 per block, max 3,072; wind x0.5 to x1.5, rain x1.15, "
                      "thunder x1.3. Every solid block in the 9 x 9 area in front of the rotor costs 3%; 25 or more stop it. "
                      "Buffer 40,000 FE, out 8,192 FE/t.",
        "magma_tap": "1 x 2 x 1 generator on a magma vent (2,048 FE/t, endless) or a lava source (1,024 FE/t; after 24,000 running ticks the "
                     "lava turns into obsidian). Needs a water source next to its upper block; without one, half output and smoke. "
                     "Buffer 20,000 FE, out 8,192 FE/t.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def generate():
    power()
    lang()

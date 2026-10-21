"""M1 content data: T1 power and processing blocks and items, recipes, loot, tags, tin worldgen and language."""

from datagen import (block_item, block_model, blockstate, cooking, cube_all, generated_item, height_range, horizontal_states,
                     item_def, loot_center_only, loot_ore, loot_self, machine_recipe, name, ore_feature, ore_targets,
                     orientable, particle_model, placed_feature, biome_modifier, rl, shaped, shapeless, simple_blockstate, tag)

PICK = "minecraft:mineable/pickaxe"
AXE = "minecraft:mineable/axe"
STONE_TOOL = "minecraft:needs_stone_tool"
METALS_T1 = ["iron", "copper", "gold", "tin"]
INGOT = {"iron": "minecraft:iron_ingot", "copper": "minecraft:copper_ingot", "gold": "minecraft:gold_ingot", "tin": "tin_ingot"}


def simple_block(bid, title, tool, needs_stone=False):
    cube_all(bid)
    simple_blockstate(bid)
    block_item(bid)
    name(f"block.autarkia.{bid}", title)
    tag("block", tool, rl(bid))
    if needs_stone:
        tag("block", STONE_TOOL, rl(bid))


def geo_block(bid, title, tool, center_only=False):
    particle_model(bid)
    simple_blockstate(bid)
    generated_item(bid)
    name(f"block.autarkia.{bid}", title)
    tag("block", tool, rl(bid))
    (loot_center_only if center_only else loot_self)(bid)


def ores():
    for bid, title in [("tin_ore", "Tin Ore"), ("deepslate_tin_ore", "Deepslate Tin Ore")]:
        simple_block(bid, title, PICK, True)
        loot_ore(bid, "raw_tin")
        tag("block", "c:ores/tin", rl(bid))
        tag("block", "c:ores", "#c:ores/tin")
        tag("item", "c:ores/tin", rl(bid))
        tag("item", "c:ores", "#c:ores/tin")
    tag("block", "c:ores_in_ground/stone", rl("tin_ore"))
    tag("block", "c:ores_in_ground/deepslate", rl("deepslate_tin_ore"))
    for bid, title, ctag, part in [("raw_tin_block", "Block of Raw Tin", "raw_tin", "raw_tin"),
                                   ("tin_block", "Block of Tin", "tin", "tin_ingot"),
                                   ("bronze_block", "Block of Bronze", "bronze", "bronze_ingot")]:
        simple_block(bid, title, PICK, True)
        loot_self(bid)
        tag("block", f"c:storage_blocks/{ctag}", rl(bid))
        tag("block", "c:storage_blocks", f"#c:storage_blocks/{ctag}")
        tag("item", f"c:storage_blocks/{ctag}", rl(bid))
        tag("item", "c:storage_blocks", f"#c:storage_blocks/{ctag}")
        shaped(bid, ["XXX", "XXX", "XXX"], {"X": part}, category="building")
        shapeless(part, [bid], 9, rid=f"{part}_from_{bid}")
    ore_feature("tin_ore", 9, ore_targets("tin_ore", "deepslate_tin_ore"))
    placed_feature("tin_ore", "tin_ore", [{"type": "minecraft:count", "count": 12}, {"type": "minecraft:in_square"},
                                          height_range("trapezoid", -16, 96), {"type": "minecraft:biome"}])
    biome_modifier("tin_ore", "#minecraft:is_overworld", ["tin_ore"], "underground_ores")


def materials():
    items = [("raw_tin", "Raw Tin"), ("tin_ingot", "Tin Ingot"), ("tin_nugget", "Tin Nugget"), ("bronze_ingot", "Bronze Ingot"),
             ("bronze_nugget", "Bronze Nugget"), ("bronze_gear", "Bronze Gear"), ("mainspring", "Mainspring")]
    for m in METALS_T1:
        items.append((f"crushed_{m}", f"Crushed {m.capitalize()} Ore"))
    for iid, title in items:
        generated_item(iid)
        name(f"item.autarkia.{iid}", title)
    tag("item", "c:raw_materials/tin", rl("raw_tin"))
    tag("item", "c:raw_materials", "#c:raw_materials/tin")
    for m in ("tin", "bronze"):
        tag("item", f"c:ingots/{m}", rl(f"{m}_ingot"))
        tag("item", "c:ingots", f"#c:ingots/{m}")
        tag("item", f"c:nuggets/{m}", rl(f"{m}_nugget"))
        tag("item", "c:nuggets", f"#c:nuggets/{m}")
        shaped(f"{m}_ingot", ["NNN", "NNN", "NNN"], {"N": f"{m}_nugget"}, rid=f"{m}_ingot_from_nuggets")
        shapeless(f"{m}_nugget", [f"{m}_ingot"], 9)
    tag("item", "c:gears/bronze", rl("bronze_gear"))
    tag("item", "c:gears", "#c:gears/bronze")
    for m in METALS_T1:
        tag("item", "autarkia:crushed_ores", rl(f"crushed_{m}"))
        tag("item", f"c:crushed_ores/{m}", rl(f"crushed_{m}"))
        cooking("smelting", INGOT[m], f"crushed_{m}", rid=f"smelting/{m}_ingot_from_crushed")
        cooking("blasting", INGOT[m], f"crushed_{m}", rid=f"blasting/{m}_ingot_from_crushed")
    for src in ("raw_tin", "tin_ore", "deepslate_tin_ore"):
        cooking("smelting", "tin_ingot", src, rid=f"smelting/tin_ingot_from_{src}")
        cooking("blasting", "tin_ingot", src, rid=f"blasting/tin_ingot_from_{src}")
    shaped("bronze_gear", [" B ", "B B", " B "], {"B": "#c:ingots/bronze"}, 2)
    shaped("mainspring", [" I ", "I I", " I "], {"I": "#c:ingots/iron"})


def processing():
    for m in METALS_T1:
        machine_recipe("crushing", f"{m}_from_raw", [(f"#c:raw_materials/{m}", 1)],
                       [(f"crushed_{m}", 1), (f"crushed_{m}", 1, 0.5)], stations=["quern", "stamp_mill"])
    machine_recipe("crushing", "gravel_from_cobblestone", [("#c:cobblestones", 1)], [("minecraft:gravel", 1)],
                   stations=["quern", "stamp_mill"])
    machine_recipe("crushing", "sand_from_gravel", [("minecraft:gravel", 1)], [("minecraft:sand", 1)], stations=["quern", "stamp_mill"])
    machine_recipe("alloying", "bronze_ingot", [("#c:ingots/copper", 3), ("#c:ingots/tin", 1)], [("bronze_ingot", 4)],
                   stations=["crucible", "blast_smelter"], time=300)

    geo_block("hand_crank", "Hand Crank", PICK)
    shaped("hand_crank", ["  S", " SG", "CCC"], {"S": "minecraft:stick", "G": "bronze_gear", "C": "minecraft:cobblestone"})
    geo_block("quern", "Quern", PICK)
    shaped("quern", [" K ", "SFS", "SSS"], {"K": "minecraft:stick", "S": "minecraft:stone", "F": "minecraft:flint"})
    geo_block("stamp_mill", "Stamp Mill", AXE, center_only=True)
    shaped("stamp_mill", ["LGL", "LIL", "CBC"], {"L": "#minecraft:logs", "G": "bronze_gear", "I": "#c:ingots/iron",
                                                 "C": "#c:cobblestones", "B": "#c:ingots/bronze"})

    pot = [{"from": [2, 0, 2], "to": [14, 2, 14], "faces": face_all("crucible_side")},
           {"from": [2, 2, 2], "to": [14, 12, 4], "faces": face_all("crucible_side")},
           {"from": [2, 2, 12], "to": [14, 12, 14], "faces": face_all("crucible_side")},
           {"from": [2, 2, 4], "to": [4, 12, 12], "faces": face_all("crucible_side")},
           {"from": [12, 2, 4], "to": [14, 12, 12], "faces": face_all("crucible_side")}]
    rims = [{"from": [1, 11, 1], "to": [15, 12, 15], "faces": {"up": {"texture": "#top"}, "down": {"texture": "#top"},
                                                                 "north": {"texture": "#side"}, "south": {"texture": "#side"},
                                                                 "east": {"texture": "#side"}, "west": {"texture": "#side"}}}]
    molten = [{"from": [4, 9, 4], "to": [12, 10, 12], "faces": {"up": {"texture": "#molten"}}}]
    tex = {"particle": rl("block/crucible_side"), "side": rl("block/crucible_side"), "top": rl("block/crucible_top"),
           "molten": rl("block/crucible_molten")}
    for e in pot:
        e["faces"] = {k: {"texture": "#side"} for k in ("north", "south", "east", "west", "up", "down")}
    block_model("crucible", {"textures": tex, "elements": pot + rims})
    block_model("crucible_molten", {"textures": tex, "elements": pot + rims + [dict(molten[0], **{"light_emission": 15})]})
    blockstate("crucible", {"variants": {"molten=false": {"model": rl("block/crucible")}, "molten=true": {"model": rl("block/crucible_molten")}}})
    block_item("crucible")
    loot_self("crucible")
    tag("block", PICK, rl("crucible"))
    name("block.autarkia.crucible", "Crucible")
    shaped("crucible", ["K K", "K K", "KKK"], {"K": "minecraft:brick"})


def face_all(tex):
    return {k: {"texture": f"#{tex}"} for k in ("north", "south", "east", "west", "up", "down")}


def power():
    wheel = {"textures": {"particle": rl("block/rope_pulley"), "wheel": rl("block/rope_pulley"), "side": rl("block/rope_pulley_side")},
             "elements": [
                 {"from": [2, 5, 2], "to": [14, 11, 14], "faces": {"up": {"texture": "#wheel"}, "down": {"texture": "#wheel"},
                                                                   "north": {"texture": "#side", "uv": [0, 5, 16, 11]},
                                                                   "south": {"texture": "#side", "uv": [0, 5, 16, 11]},
                                                                   "east": {"texture": "#side", "uv": [0, 5, 16, 11]},
                                                                   "west": {"texture": "#side", "uv": [0, 5, 16, 11]}}},
                 {"from": [6.5, 0, 6.5], "to": [9.5, 16, 9.5], "faces": {k: {"texture": "#side", "uv": [6, 0, 9, 16]} for k in
                                                                         ("north", "south", "east", "west")} | {
                     "up": {"texture": "#side", "uv": [6, 6, 9, 9]}, "down": {"texture": "#side", "uv": [6, 6, 9, 9]}}}]}
    block_model("rope_pulley", wheel)
    rots = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "west": {"x": 90, "y": 270}, "east": {"x": 90, "y": 90}}
    blockstate("rope_pulley", {"variants": {f"facing={d}": dict({"model": rl("block/rope_pulley")}, **r) for d, r in rots.items()}})
    block_item("rope_pulley")
    loot_self("rope_pulley")
    tag("block", AXE, rl("rope_pulley"))
    name("block.autarkia.rope_pulley", "Rope Pulley")
    shaped("rope_pulley", [" P ", "PGP", " P "], {"P": "#minecraft:planks", "G": "bronze_gear"}, 2)

    generated_item("rope_coil")
    name("item.autarkia.rope_coil", "Rope Coil")
    shaped("rope_coil", ["SSS", "S S", "SSS"], {"S": "minecraft:string"}, 2)
    generated_item("wrench", handheld=True)
    name("item.autarkia.wrench", "Wrench")
    shaped("wrench", ["B B", " B ", " B "], {"B": "#c:ingots/bronze"}, category="equipment")
    generated_item("multimeter")
    name("item.autarkia.multimeter", "Multimeter")
    shaped("multimeter", [" G ", "BCB", "BRB"], {"G": "minecraft:glass_pane", "B": "brass_plate", "C": "copper_coil", "R": "minecraft:redstone"},
           category="equipment")


def logistics():
    block_model("crate", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": rl("block/crate_side"), "top": rl("block/crate_top"), "bottom": rl("block/crate_top")}})
    simple_blockstate("crate")
    block_item("crate")
    loot_self("crate")
    tag("block", AXE, rl("crate"))
    name("block.autarkia.crate", "Crate")
    shaped("crate", ["PNP", "P P", "PPP"], {"P": "#minecraft:planks", "N": "bronze_nugget"})

    orientable("tally_board", "tally_board_front", "tally_board_side", "tally_board_side")
    horizontal_states("tally_board")
    block_item("tally_board")
    loot_self("tally_board")
    tag("block", AXE, rl("tally_board"))
    name("block.autarkia.tally_board", "Tally Board")
    shaped("tally_board", [" F ", "PGP", " P "], {"F": "minecraft:item_frame", "P": "#minecraft:planks", "G": "bronze_gear"})

    slide = {"textures": {"particle": rl("block/chute"), "slide": rl("block/chute")}, "elements": [
        {"from": [1, 7, -3.3], "to": [15, 8.5, 19.3], "rotation": {"angle": -45, "axis": "x", "origin": [8, 8, 8]},
         "faces": {"up": {"texture": "#slide", "uv": [1, 0, 15, 16]}, "down": {"texture": "#slide", "uv": [1, 0, 15, 16]},
                   "north": {"texture": "#slide", "uv": [1, 0, 15, 2]}, "south": {"texture": "#slide", "uv": [1, 0, 15, 2]},
                   "east": {"texture": "#slide", "uv": [0, 0, 16, 2]}, "west": {"texture": "#slide", "uv": [0, 0, 16, 2]}}},
        {"from": [0, 7, -3.3], "to": [1, 11.5, 19.3], "rotation": {"angle": -45, "axis": "x", "origin": [8, 8, 8]},
         "faces": {k: {"texture": "#slide", "uv": [0, 2, 16, 6]} for k in ("north", "south", "east", "west", "up", "down")}},
        {"from": [15, 7, -3.3], "to": [16, 11.5, 19.3], "rotation": {"angle": -45, "axis": "x", "origin": [8, 8, 8]},
         "faces": {k: {"texture": "#slide", "uv": [0, 2, 16, 6]} for k in ("north", "south", "east", "west", "up", "down")}},
        {"from": [1, 0, 13], "to": [3, 6, 15], "faces": face_all("slide")},
        {"from": [13, 0, 13], "to": [15, 6, 15], "faces": face_all("slide")}]}
    block_model("chute", slide)
    horizontal_states("chute")
    block_item("chute")
    loot_self("chute")
    tag("block", AXE, rl("chute"))
    name("block.autarkia.chute", "Chute")
    shaped("chute", ["P P", "PNP", " P "], {"P": "#minecraft:planks", "N": "bronze_nugget"}, 4)

    simple_block("foundation", "Foundation", PICK)
    loot_self("foundation")
    shaped("foundation", ["SCS", "CSC", "SCS"], {"S": "minecraft:stone", "C": "minecraft:clay_ball"}, 4, category="building")


def lang():
    name("tier.autarkia.rope", "rope drive")
    name("tier.autarkia.copper", "copper wire")
    name("tier.autarkia.insulated", "insulated cable")
    name("tier.autarkia.hv", "HV line")
    name("tier.autarkia.superconductor", "superconductor")
    name("tier.autarkia.phase", "phase link")
    msgs = {"ok": "Span placed", "too_long": "Too long: max %s blocks for %s", "wrong_tier": "This node does not take %2$s",
            "node_full": "Node is full", "port_tier": "Port is wired for another tier. Use a transformer.",
            "blocked": "The span is blocked by a block", "same_node": "Pick a different node", "not_a_node": "Not a connector or machine port",
            "too_many_parallel": "Max 4 parallel spans between two nodes", "not_loaded": "The other end is not loaded",
            "cleared": "Span start cleared", "start": "First end set; click the second node", "needs_items": "Needs %s x %s",
            "placed": "Span placed: %s blocks, %s items"}
    for k, v in msgs.items():
        name(f"message.autarkia.span.{k}", v)
    name("message.autarkia.wrench.crate", "Crate picked up with its contents")
    name("message.autarkia.multimeter.no_node", "Not a grid node")
    for st, text in [("running", "Running"), ("idle_no_input", "Idle"), ("blocked_output_full", "Output full"),
                     ("no_power", "No power"), ("no_condition", "Condition missing")]:
        name(f"status.autarkia.{st}", text)
    for h, text in [("none", "No heat source"), ("campfire", "Campfire (0.5x)"), ("furnace", "Furnace (1.0x)"), ("lava", "Lava (1.5x)")]:
        name(f"heat.autarkia.{h}", text)
    name("tooltip.autarkia.energy", "%s / %s FE")
    name("container.autarkia.crate", "Crate (%s%% full)")
    mm = {"node": "Node %s %s %s", "no_network": "No network (node not connected)", "network": "Network #%s, %s, %s nodes, %s spans",
          "service": "Service %s%%", "generation": "Gen %s", "demand": "Demand %s", "loss": "Loss %s", "stored": "Stored %s / %s FE",
          "rho": "Loss ratio %s", "rho_lagging": "Loss ratio %s (Lagging)", "span_flow": "Span #%s, %s blocks, flow %s",
          "span_idle": "Span #%s, %s blocks, Parallel, idle", "load": "Load", "wear": "Wear", "heat": "Heat", "more": "+%s more spans"}
    for k, v in mm.items():
        name(f"multimeter.autarkia.{k}", v)
    jade = {"energy": "Energy: %s / %s FE", "queue": "Queue: %s", "output": "Output: %s FE/t", "buffer": "Buffer: %s / %s FE",
            "tier": "Tier: %s", "spans": "Spans: %s / %s", "network_load": "Network: %s%% load", "fill": "Fill: %s / %s slots",
            "items": "Items: %s", "chute_blocked": "State: Blocked", "chute_waiting": "State: Waiting at the end",
            "chute_moving": "State: Moving"}
    for k, v in jade.items():
        name(f"jade.autarkia.{k}", v)
    for g, v in [("power", "Autarkia power"), ("machine", "Autarkia machines"), ("logistics", "Autarkia logistics")]:
        name(f"config.jade.plugin_autarkia.{g}", v)
    name("jei.autarkia.chance", "%s%% chance")
    name("jei.autarkia.time", "%s s")
    name("jei.autarkia.time_energy", "%s s, %s FE/t")
    name("jei.autarkia.category.crushing", "Crushing")
    name("jei.autarkia.category.alloying", "Alloying")
    info = {
        "hand_crank": "Generator. Hold use on it to turn: 20 FE/t (a Porter gives 10 FE/t). Buffer 1,000 FE, out 40 FE/t. Output stops 6 ticks after you let go.",
        "rope_pulley": "Rope drive connector node. Takes up to 4 rope spans of max 12 blocks. Machines and generators next to it join its network.",
        "rope_coil": "Rope drive cable: click one node, then another (max 12 blocks, rated 40 FE/t, 1 coil per 8 blocks). Above 40 FE/t the rope slips, wears and snaps.",
        "crate": "27-slot storage. The top shows piles of its contents. Sneak + use with a wrench picks it up with everything inside.",
        "tally_board": "Counts the items of all crates within 5 blocks every 2 seconds and shows the 6 largest counts on its face. The Colony Core reads it.",
        "chute": "Gravity slide: 1 to 4 items per entry slide down and leave one block lower in front. Right-click inserts; it also pulls from an inventory above. A full target makes items wait; nothing drops.",
        "quern": "Manual grinder. Use with ore to insert, hold use with an empty hand to turn. 8 turns per raw ore: 1 crushed ore plus 1 more at 50%. Outputs go to an inventory below.",
        "stamp_mill": "Powered grinder (2 blocks tall). 16 FE/t, 8 s per raw ore: 1 crushed ore plus 1 more at 50%.",
        "crucible": "Alloys over heat: 3 copper + 1 tin = 4 bronze. Lit campfire 0.5x, lit furnace 1.0x, lava 1.5x speed.",
        "foundation": "Heavy building block. Heavy machines from T2 need it under every bottom position.",
        "wrench": "Use: turns Autarkia blocks. Sneak + use: picks up a crate with its contents, or any other Autarkia block.",
        "multimeter": "Use on a connector or machine port: shows the network generation, demand, service, losses and every span of that node.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def generate():
    ores()
    materials()
    processing()
    power()
    logistics()
    lang()

"""M4 content data: T2 power (wooden pole, copper wire, lamp, waterwheel, boiler, valve, steam engine, lightning mast,
arrester, capacitor bank), catwalks and hazard block, core_upgrade_t2, T2 colony templates and plans, advancements and
language. The recipes use the T2 parts of M5 (02 section 8); the copper wire spool comes from the wire drawer (content_m5)."""

from content_m3 import BIG, NORTH, cond, plan
from datagen import (advancement, block_item, block_model, blockstate, cube_all, generated_item, geo_single, horizontal_states, loot_center_only,
                     loot_self, name, particle_model, rl, shaped, shapeless, simple_blockstate, tag, write_json)

AXE = "minecraft:mineable/axe"
PICK = "minecraft:mineable/pickaxe"
STRIPES = (2, 5, 9, 12)


def faces(tex, only=None):
    return {k: {"texture": f"#{tex}"} for k in (only or ("north", "south", "east", "west", "up", "down"))}


def geo_blocks():
    for bid, title, tool in [("wooden_pole", "Wooden Pole", AXE), ("waterwheel", "Waterwheel", AXE), ("boiler", "Boiler", PICK),
                             ("steam_engine", "Steam Engine", PICK), ("lightning_mast", "Lightning Mast", PICK)]:
        particle_model(bid)
        simple_blockstate(bid)
        generated_item(bid)
        loot_center_only(bid)
        tag("block", tool, rl(bid))
        name(f"block.autarkia.{bid}", title)
    shaped("wooden_pole", ["BLB", " L ", " L "], {"B": "minecraft:brick", "L": "#minecraft:logs"})
    shaped("waterwheel", ["PLP", "LGL", "PCP"], {"P": "#minecraft:planks", "L": "#minecraft:logs", "G": "steel_gear", "C": "copper_coil"})
    shaped("boiler", ["BPB", "PFP", "BBB"], {"B": "minecraft:bricks", "P": "steel_plate", "F": "minecraft:furnace"})
    shaped("steam_engine", ["PSP", "CGC", "PPP"], {"P": "steel_plate", "S": "minecraft:piston", "C": "copper_coil", "G": "steel_gear"})
    shaped("lightning_mast", [" R ", "CBC", " B "], {"R": "#minecraft:lightning_rods", "C": "copper_coil", "B": "brass_block"})


def small_blocks():
    geo_single("pressure_valve", facing=False)
    arrester = {"textures": {"particle": rl("block/lightning_arrester"), "a": rl("block/lightning_arrester")}, "elements": [
        {"from": [5, 0, 5], "to": [11, 3, 11], "faces": faces("a")},
        {"from": [7, 3, 7], "to": [9, 14, 9], "faces": faces("a")},
        {"from": [7.5, 14, 7.5], "to": [8.5, 16, 8.5], "faces": faces("a")}]}
    block_model("lightning_arrester", arrester)
    simple_blockstate("lightning_arrester")
    block_item("lightning_arrester")
    for bid, title, tool in [("pressure_valve", "Pressure Valve", PICK), ("lightning_arrester", "Lightning Arrester", PICK)]:
        loot_self(bid)
        tag("block", tool, rl(bid))
        name(f"block.autarkia.{bid}", title)
    shaped("pressure_valve", [" C ", "PRP"], {"C": "minecraft:copper_ingot", "P": "brass_plate", "R": "minecraft:redstone"})
    shaped("lightning_arrester", [" C ", "BRB"], {"C": "minecraft:copper_ingot", "B": "minecraft:brick", "R": "minecraft:redstone"})

    cube_all("industrial_lamp")
    cube_all("industrial_lamp_on")
    blockstate("industrial_lamp", {"variants": {"lit=false": {"model": rl("block/industrial_lamp")}, "lit=true": {"model": rl("block/industrial_lamp_on")}}})
    block_item("industrial_lamp")
    loot_self("industrial_lamp")
    tag("block", PICK, rl("industrial_lamp"))
    name("block.autarkia.industrial_lamp", "Industrial Lamp")
    shaped("industrial_lamp", [" C ", "CGC", " R "], {"C": "minecraft:copper_ingot", "G": "minecraft:glowstone", "R": "minecraft:redstone"}, 2)

    extra = {}
    for k in range(5):
        elements = [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
            "north": {"texture": "#front"}, "south": {"texture": "#side"}, "east": {"texture": "#side"}, "west": {"texture": "#side"},
            "up": {"texture": "#side"}, "down": {"texture": "#side"}}}]
        for sx in STRIPES[:k]:
            elements.append({"from": [14 - sx, 3, -0.05], "to": [16 - sx, 13, 0], "light_emission": 12,
                             "faces": {"north": {"texture": "#stripe"}}})
        block_model(f"capacitor_bank_c{k}", {"parent": "minecraft:block/block", "textures": {
            "particle": rl("block/capacitor_bank_side"), "front": rl("block/capacitor_bank_front"), "side": rl("block/capacitor_bank_side"),
            "stripe": rl("block/capacitor_bank_stripe")}, "elements": elements})
        extra[f"charge={k}"] = rl(f"block/capacitor_bank_c{k}")
    horizontal_states("capacitor_bank", extra=extra)
    block_item("capacitor_bank", model=rl("block/capacitor_bank_c0"))
    loot_self("capacitor_bank")
    tag("block", PICK, rl("capacitor_bank"))
    name("block.autarkia.capacitor_bank", "Capacitor Bank")
    shaped("capacitor_bank", ["PCP", "CRC", "PCP"], {"P": "brass_plate", "C": "copper_coil", "R": "minecraft:redstone_block"})

    cube_all("catwalk")
    simple_blockstate("catwalk")
    block_item("catwalk")
    loot_self("catwalk")
    stairs("catwalk_stairs", "catwalk")
    cube_all("hazard_block")
    simple_blockstate("hazard_block")
    block_item("hazard_block")
    loot_self("hazard_block")
    for bid, title in [("catwalk", "Catwalk"), ("catwalk_stairs", "Catwalk Stairs"), ("hazard_block", "Hazard Block")]:
        tag("block", PICK, rl(bid))
        name(f"block.autarkia.{bid}", title)
    tag("block", "minecraft:stairs", rl("catwalk_stairs"))
    tag("item", "minecraft:stairs", rl("catwalk_stairs"))
    shaped("catwalk", ["B B", " I ", "B B"], {"B": "minecraft:iron_bars", "I": "minecraft:iron_ingot"}, 4, category="building")
    shaped("catwalk_stairs", ["C  ", "CC ", "CCC"], {"C": "catwalk"}, 4, category="building")
    shapeless("hazard_block", ["minecraft:stone", "minecraft:yellow_dye", "minecraft:black_dye"], category="building")


def stairs(bid, tex):
    t = {"bottom": rl(f"block/{tex}"), "top": rl(f"block/{tex}"), "side": rl(f"block/{tex}")}
    for suffix, parent in [("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")]:
        block_model(f"{bid}{suffix}", {"parent": f"minecraft:block/{parent}", "textures": t})
    variants = {}
    base_y = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, fy in base_y.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model = rl(f"block/{bid}" + ("_inner" if shape.startswith("inner") else "_outer" if shape.startswith("outer") else ""))
                y = fy
                if shape in ("inner_left", "outer_left") and half == "bottom":
                    y -= 90
                if shape in ("inner_right", "outer_right") and half == "top":
                    y += 90
                v = {"model": model}
                if half == "top":
                    v["x"] = 180
                y %= 360
                if y:
                    v["y"] = y
                if v.get("x") or y:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    blockstate(bid, {"variants": variants})
    block_item(bid)
    loot_self(bid)


def items():
    generated_item("copper_wire_spool")
    name("item.autarkia.copper_wire_spool", "Copper Wire Spool")
    generated_item("core_upgrade_t2")
    name("item.autarkia.core_upgrade_t2", "Core Upgrade T2")
    shaped("core_upgrade_t2", ["PCP", "GSG", "PCP"], {"P": "#c:ingots/bronze", "C": "minecraft:copper_ingot", "G": "bronze_gear",
                                                       "S": "minecraft:iron_ingot"})


def template(tid, index, title, site, base, max_built, reason, triggers=None, any_=None, plan_=None, handler=None, sign=None):
    obj = {"index": index, "tier": 2, "triggers": triggers or [], "site": {"rule": site, "radius": 24}, "base": base, "max_built": max_built,
           "reason": reason}
    if any_:
        obj["any"] = any_
    if plan_:
        obj["plan"] = rl(plan_)
    if handler:
        obj["handler"] = handler
    if sign:
        obj["sign"] = sign
    write_json(f"data/autarkia/colony_template/{tid}.json", obj)
    name(f"colony_template.autarkia.{tid}", title)


def templates():
    power_short = {"key": "power_short", "signal": "S03", "scale": 100}
    template("waterwheel", 7, "Waterwheel", "SR_FLOW_WATER", 65, 6, power_short, triggers=[cond("S03", ">=", 0.20, 1200)], plan_="waterwheel",
             sign="power_short")
    template("boiler_engine", 8, "Boiler and Engine", "SR_NEAR_NET", 55, 4, power_short,
             triggers=[cond("S03", ">=", 0.30, 1200), cond("S15", ">=", 64)], plan_="boiler_engine", sign="power_short")
    template("span_upgrade", 10, "Span Upgrade", "SR_REPLACE", 50, 0, {"key": "span_hot", "signal": "S06", "scale": 100},
             any_=[[cond("S06", ">=", 0.8, 1200)], [cond("S07", ">=", 0.8, 600)], [cond("mem.cable_snap", ">=", 1)], [cond("mem.fuse_trip", ">=", 1)],
                   [cond("mem.quench", ">=", 0.75)]],
             handler="span_upgrade", sign="line_snapped")
    template("lightning_mast", 11, "Lightning Mast", "SR_PEAK", 70, 2, {"key": "storm_hits", "signal": "S24", "scale": 1},
             any_=[[cond("S24", ">=", 2)], [cond("mem.lightning_surge", ">=", 1)]], plan_="lightning_mast", sign="storm_hit")
    template("lightning_arrester", 12, "Lightning Arresters", "SR_ON_SPAN", 75, 0, {"key": "surge_memory", "signal": "mem.lightning_surge", "scale": 1},
             triggers=[cond("mem.lightning_surge", ">=", 0.5)], handler="arrester", sign="storm_hit")
    template("pressure_valve", 13, "Pressure Valve", "SR_IN_PLACE", 80, 0, {"key": "boiler_high", "signal": "recent.boiler_high", "scale": 1},
             any_=[[cond("recent.boiler_high", ">=", 1)], [cond("mem.boiler_burst", ">=", 0.5)]], handler="valve")
    plan("waterwheel", (1, 3, 3), [(0, 0, 1, "waterwheel", BIG)], [0, 0, 1], "runs", port=[0, 0, 1])
    found = [(x, 0, z, "foundation", None) for x in range(2) for z in range(4)]
    plan("boiler_engine", (2, 5, 4), found + [(0, 1, 0, "steam_engine", BIG), (0, 1, 2, "boiler", BIG), (1, 4, 3, "pressure_valve", None)],
         [0, 1, 0], "none", port=[0, 1, 0])
    plan("lightning_mast", (1, 5, 1), [(0, 0, 0, "foundation", None), (0, 1, 0, "lightning_mast", BIG)], [0, 1, 0], "none", port=[0, 1, 0])
    for k, v in {"span_hot": "Span at %s%% of its rating", "storm_hits": "%s lightning strikes this week",
                 "surge_memory": "Lightning surge remembered (weight %s)", "boiler_high": "Boiler pressure was high today (%s)"}.items():
        name(f"colony.reason.autarkia.{k}", v)


def advancements():
    advancement("power/red_hot", "Glowing Wire", "Watch a copper span snap from heat", "copper_wire_spool", parent="power/first_span")
    advancement("power/surge", "Struck", "A lightning surge destroys a machine", "lightning_arrester", parent="power/root")
    advancement("power/storm_harvest", "Free Energy", "A lightning mast puts 120,000 FE or more into capacitor banks", "lightning_mast",
                parent="power/surge", frame="goal")
    advancement("power/boiler_burst", "Overpressure", "A boiler bursts", "boiler", parent="power/root")


def lang():
    jade = {"pressure": "Pressure: %s bar", "water": "Water: %s mB", "fire_level": "Fire level: %s", "mast.valid": "Open sky: catches bolts",
            "mast.covered": "Covered: no open sky above", "mast.strikes": "Strikes: %s, last stored %s FE"}
    for k, v in jade.items():
        name(f"jade.autarkia.{k}", v)
    screen = {"pressure": "P %s bar", "temperature": "T %s C", "fire": "Fire %s", "venting": "Venting", "water": "Water: %s / %s mB",
              "steam": "Steam: %s mB"}
    for k, v in screen.items():
        name(f"screen.autarkia.boiler.{k}", v)
    for k, v in {"stored": "%s / %s FE", "percent": "%s%% charged", "io": "In %s, out %s FE/t"}.items():
        name(f"screen.autarkia.energy_storage.{k}", v)
    name("menu.autarkia.boiler", "Boiler")
    name("menu.autarkia.energy_storage", "Energy Storage")
    msgs = {"core_upgrade.inactive": "The Core must be active", "core_upgrade.busy": "The Core is already upgrading",
            "core_upgrade.wrong_tier": "This upgrade needs a T%s Core (this one is T%s)"}
    for k, v in msgs.items():
        name(f"message.autarkia.{k}", v)
    info = {
        "wooden_pole": "Copper wire connector, 3 blocks tall. Takes up to 4 copper spans of max 16 blocks at its top. Machines next to its base join its network.",
        "copper_wire_spool": "Copper wire: click one node, then another (max 16 blocks, rated 256 FE/t). Above its rating the wire heats: orange at 40%, red at 70%, it snaps with arcs at 100% and can set fires. Metal cables end only on ceramic bushings and on poles, pylons and nodes; a transformer joins rope and metal.",
        "industrial_lamp": "Light 15 while it has power (1 FE/t). Flickers when its network cannot keep it full. When a snap cuts its supply it goes dark 3 ticks per hop from the break.",
        "waterwheel": "Generator, 3 wide and 3 tall. Flowing water under the bottom row (24 FE/t per block) or falling water on the top (40 FE/t per block) turns it; max 128 FE/t. Sources do not count.",
        "boiler": "2 x 3 x 2. Burns fuel at fire level 0 to 4 (GUI) and makes 10 mB steam per level per tick from water (bucket slot, pipes or water sources next to its bottom). Pressure = steam / 400 bar. It bursts at 12 bar or after 10 s dry at 200 C.",
        "pressure_valve": "Place on top of a boiler: vents 25 mB/t of steam above 9 bar. One valve cannot hold fire level 4 with no load; two can.",
        "steam_engine": "1 x 2 x 2. Its back face must touch a boiler. Uses up to 20 mB steam per tick: 6 FE per mB, max 120 FE/t. A boiler feeds at most 2 engines.",
        "lightning_mast": "4 blocks tall. Bolts within 48 blocks move to it; it puts 120,000 FE into the capacitor banks of its network. Thunder can strike it on its own.",
        "lightning_arrester": "Place on top of a pole: a lightning pulse stops there. Next to a machine: it protects that machine.",
        "capacitor_bank": "Energy storage: 50,000 FE, I/O 2,048 FE/t, small leak. Charges an FE item in its slot. Takes lightning energy without a rate limit.",
        "catwalk": "See-through steel grate to walk on.", "catwalk_stairs": "Stairs of the catwalk.", "hazard_block": "Yellow and black warning stripes.",
        "core_upgrade_t2": "Use on an active T1 Colony Core (owner or trusted): the Core upgrades for 5 s, then the colony is T2 with a bigger domain, more workers and the T2 templates.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def generate():
    geo_blocks()
    small_blocks()
    items()
    templates()
    advancements()
    lang()

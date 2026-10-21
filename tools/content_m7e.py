"""M7 content data, colony part (05, 05b): outpost core and seed, core upgrade T4, templates 21 to 26 with their plans
(outpost_basic, sentinel_post, wind_tower, hydro), outpost language, JEI pages and the frontier advancement."""

from content_m3 import BIG, cond, plan
from datagen import advancement, generated_item, machine_recipe, name, particle_model, rl, simple_blockstate, tag, write_json

PICK = "minecraft:mineable/pickaxe"
NORTH = {"facing": "north"}


def ev(trigger, event):
    return {"done": {"trigger": rl(trigger), "conditions": {"event": event}}}


def template(tid, index, title, site, base, max_built, reason, triggers=None, any_=None, plan_=None, handler=None, sign=None):
    obj = {"index": index, "tier": 4, "triggers": triggers or [], "site": {"rule": site, "radius": 24}, "base": base, "max_built": max_built,
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


def outpost_blocks():
    particle_model("outpost_core")
    simple_blockstate("outpost_core")
    write_json("data/autarkia/loot_table/blocks/outpost_core.json", {"type": "minecraft:block", "random_sequence": rl("blocks/outpost_core"), "pools": []})
    tag("block", PICK, rl("outpost_core"))
    tag("block", "autarkia:worker_unbreakable", rl("outpost_core"))
    name("block.autarkia.outpost_core", "Outpost Core")
    name("menu.autarkia.outpost_core", "Outpost Core")
    for iid, title in (("outpost_seed", "Outpost Seed"), ("core_upgrade_t4", "Core Upgrade T4")):
        generated_item(iid)
        name(f"item.autarkia.{iid}", title)
    machine_recipe("fabricating", "core_upgrade_t4", [("advanced_circuit", 4), ("vantite_steel_ingot", 8), ("servo", 4)], [("core_upgrade_t4", 1)],
                   time=200, energy=32000 // 200)


def templates():
    power_short = {"key": "power_short", "signal": "S03", "scale": 100}
    template("outpost", 21, "Outpost", "SR_DEPOSIT", 60, 6, {"key": "outpost_need", "signal": "S16", "scale": 1},
             triggers=[cond("outpost.need", ">=", 1, 24000), cond("outpost.room", ">=", 1)], plan_="outpost_basic", handler="outpost")
    template("hv_link", 22, "HV Link", "route", 65, 0, {"key": "outpost_unlinked", "signal": "outpost.link_load", "scale": 100},
             any_=[[cond("outpost.link_need", ">=", 1)], [cond("outpost.link_load", ">=", 0.8, 1200)]], handler="hv_link")
    template("cable_car", 23, "Cable Car", "route", 40, 0, {"key": "outpost_flow", "signal": "cable_car.need", "scale": 1},
             triggers=[cond("cable_car.need", ">=", 256)], handler="cable_car")
    template("sentinel_post", 24, "Sentinel Post", "SR_PERIMETER", 50, 6, {"key": "hostiles", "signal": "S22", "scale": 1},
             any_=[[cond("S22", ">=", 3, 2400)], [cond("S23", ">=", 8)], [cond("mem.machine_destroyed", ">=", 0.75)]], plan_="sentinel_post")
    template("wind_tower", 25, "Wind Tower", "SR_HIGH_OPEN", 55, 6, power_short,
             triggers=[cond("S03", ">=", 0.20, 2400), cond("net.hv", ">=", 1)], plan_="wind_tower", sign="power_short")
    template("hydro_turbine", 26, "Hydro Turbine", "SR_WATER_COLUMN", 60, 3, power_short,
             triggers=[cond("S03", ">=", 0.20, 2400), cond("water.column", ">=", 6)], plan_="hydro", sign="power_short")
    found = [(x, 0, z, "foundation", None) for x in (4, 5) for z in (5, 6)]
    plan("outpost_basic", (6, 8, 7), found + [(2, 0, 3, "outpost_core", BIG), (1, 0, 3, "depot", None), (4, 0, 4, "hv_pylon", BIG),
                                              (5, 0, 4, "wind_tower", BIG), (4, 0, 3, "recharge_dock", NORTH), (4, 1, 5, "jaw_crusher", BIG)],
         [2, 0, 3], "none", spans=[{"a": [4, 0, 4], "b": [4, 1, 5], "tier": "hv"}])
    plan("sentinel_post", (3, 5, 3), [(1, 0, 1, "steel_pylon", BIG), (1, 0, 0, "recharge_dock", NORTH), (0, 0, 1, "industrial_lamp", None),
                                      (2, 0, 1, "catwalk", None), (1, 0, 2, "catwalk", None)], [1, 0, 1], "none", port=[1, 0, 1])
    plan("wind_tower", (1, 8, 1), [(0, 0, 0, "wind_tower", BIG)], [0, 0, 0], "none", port=[0, 0, 0])
    plan("hydro", (3, 3, 3), [(1, 0, 1, "hydro_turbine", BIG)], [1, 0, 1], "none", port=[1, 0, 1])
    for k, v in {"outpost_need": "%s known deposits; a metal is short and its ore lies far from the Core",
                 "outpost_unlinked": "An outpost has no power link (link load %s%%)", "outpost_flow": "%s items a day carried by hand from an outpost",
                 "hostiles": "%s hostile mobs in the domain"}.items():
        name(f"colony.reason.autarkia.{k}", v)


def lang():
    for k, v in {"tier": "Outposts need an active colony of tier 4 or higher", "max": "This colony has all the outposts its tier allows",
                 "distance": "An outpost must be 64 to 512 blocks from the Core", "spacing": "Too close to another outpost (64 blocks minimum)",
                 "overlap": "The outpost area would overlap another colony", "no_colony": "You manage no colony this seed can serve",
                 "blocked": "The 2 x 2 x 2 space for the outpost core must be free"}.items():
        name(f"message.autarkia.outpost.{k}", v)
    for k, v in {"index": "Outpost %s/%s", "link": "Link: %s", "catch_up": "Last catch-up: +%s items", "power": "Power: %s%%",
                 "profile": "Profile: %s min", "buffer": "Virtual buffer: %s items"}.items():
        name(f"screen.autarkia.outpost.{k}", v)
    for k, v in {"index": "Outpost %s/%s", "link": "Link %s", "catch_up": "Last catch-up +%s items", "growing": "Growing %s%%",
                 "unlinked": "No colony"}.items():
        name(f"jade.autarkia.outpost.{k}", v)
    for k, v in {"none": "none", "hv": "HV", "cable_car": "cable car", "both": "HV and cable car"}.items():
        name(f"link.autarkia.{k}", v)
    info = {
        "outpost_seed": "Fabricator: 1 advanced circuit, 2 servos, 1 crate, 4 vantite steel, 1 oak sapling; 20 s, 64,000 FE. A T4 colony carries it to a "
                        "far ore deposit (64 to 512 blocks from the Core) and plants it: an Outpost Core grows in 10 s. You can plant it yourself on a "
                        "free spot of a colony you manage.",
        "core_upgrade_t4": "Use on a T3 Colony Core: T4, domain half-size 48, 20 workers, up to 6 outposts (half-size 24) with HV pylon lines and "
                           "cable cars, Surveyors, Cable-Layers and Sentinels.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)
    advancement("colony/frontier", "Frontier", "Your colony founds its first outpost", "outpost_seed", parent="colony/it_fixed_itself",
                criteria=ev("colony_event", "outpost_founded"), frame="goal")


def generate():
    outpost_blocks()
    templates()
    lang()

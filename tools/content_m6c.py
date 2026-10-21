"""M6 content data, third part: warehouse, ledger terminal and capsule tubes (04), the T3 workers, recharge dock,
drone frame and upgrades (06), and the T3 colony data: domain beacon, prop beam, core upgrade, survey lens, templates
14 to 20 with their structures, and advancements (05)."""

from content_m3 import BIG, cond, plan
from datagen import (advancement, block_item, block_model, blockstate, cube_column, generated_item, horizontal_states, inv_changed, loot_self,
                     machine_recipe, name, orientable, particle_model, rl, shaped, tag, write_json)

PICK = "minecraft:mineable/pickaxe"
SIDES = ("north", "south", "east", "west", "up", "down")


def ev(trigger, event):
    return {"done": {"trigger": rl(trigger), "conditions": {"event": event}}}


def machine_block(bid, title, front, side, top=None):
    orientable(bid, front, side, top or side)
    horizontal_states(bid)
    block_item(bid)
    loot_self(bid)
    tag("block", PICK, rl(bid))
    name(f"block.autarkia.{bid}", title)


def warehouse():
    machine_block("warehouse_controller", "Warehouse Controller", "warehouse_controller_front", "warehouse_controller_side")
    machine_block("ledger_terminal", "Ledger Terminal", "ledger_terminal_front", "ledger_terminal_side")
    shaped("warehouse_controller", ["PCP", "BXB", "PRP"], {"P": "steel_plate", "C": "basic_circuit", "B": "minecraft:barrel", "X": "steel_rack",
                                                            "R": "minecraft:redstone"})
    shaped("ledger_terminal", [" G ", "PCP", "PBP"], {"G": "minecraft:glass_pane", "C": "basic_circuit", "B": "minecraft:book", "P": "steel_plate"})
    name("menu.autarkia.warehouse_controller", "Warehouse Controller")
    name("menu.autarkia.ledger_terminal", "Ledger Terminal")
    screen = {"warehouse.racks": "Racks: %s", "warehouse.fill": "Fill: %s%%", "warehouse.overloaded": "Overloaded racks: %s",
              "warehouse.requests": "Open requests: %s", "warehouse.free": "Free crate positions: %s", "ledger.search": "Search",
              "ledger.request": "Request", "ledger.counts": "Available %s / total %s", "ledger.delivered": "Delivered %s / %s",
              "ledger.eta": "ETA %s s", "ledger.warning.no_warehouse": "No warehouse", "ledger.warning.no_loader": "No Loader assigned",
              "ledger.warning.warehouse_full": "Warehouse full"}
    for st in ("queued", "assigned", "fetching", "delivering", "done", "partial", "failed"):
        screen[f"ledger.status.{st}"] = st.capitalize()
    for r, v in (("no_loader", "No Loader assigned"), ("partial", "Not enough in the warehouse"), ("none_available", "None available"),
                 ("output_full", "FAILED: output full"), ("loader_stuck", "FAILED: Loader stuck"), ("warehouse_full", "Warehouse full")):
        screen[f"ledger.reason.{r}"] = v
    for k, v in screen.items():
        name(f"screen.autarkia.{k}", v)
    for k, v in (("ledger.queue_full", "The warehouse queue is full (16 requests)"), ("ledger.none_available", "None of that item is available"),
                 ("ledger.no_warehouse", "No warehouse")):
        name(f"message.autarkia.{k}", v)
    jade = {"warehouse.summary": "Racks %s, fill %s%%, overloaded %s", "warehouse.queue": "Queue %s, Loaders %s", "warehouse.no_power": "No power",
            "ledger.linked": "Linked: yes", "ledger.unlinked": "Linked: no (no warehouse)", "ledger.queue": "Queue %s. Loader: %s assigned"}
    for k, v in jade.items():
        name(f"jade.autarkia.{k}", v)
    name("jei.autarkia.info.warehouse_controller", "Indexes the racks within 16 blocks (cube; config 8 to 32), up to 256. Queues up to 16 ledger requests and "
                                                   "hands each trip to a Loader. 64 FE/t while requests are open.")
    name("jei.autarkia.info.ledger_terminal", "Search the warehouse stock and request 1 to 1,728 of an item. A Loader brings the crate: the items go to you "
                                              "within 6 blocks, else to the 18-slot output. Items in the 9 input slots are put away; empty crates in the crate "
                                              "slot become new stock positions.")


def capsules():
    tex = {"particle": rl("block/capsule_tube_frame"), "glass": rl("block/capsule_tube"), "frame": rl("block/capsule_tube_frame")}
    core = {"from": [5, 5, 5], "to": [11, 11, 11], "faces": {k: {"texture": "#glass"} for k in SIDES}}

    def arm(z0, z1, c0, c1, open_face):
        return [{"from": [5, 5, z0], "to": [11, 11, z1], "faces": {k: {"texture": "#glass"} for k in SIDES if k not in ("north", "south")}},
                {"from": [4.5, 4.5, c0], "to": [11.5, 11.5, c1], "faces": {k: {"texture": "#frame"} for k in SIDES if k != open_face}}]
    block_model("capsule_tube_core", {"ambientocclusion": False, "textures": tex, "elements": [core]})
    block_model("capsule_tube_arm", {"ambientocclusion": False, "textures": tex, "elements": arm(0, 5, 0, 1.5, "south")})
    block_model("capsule_tube_item", {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": tex,
                                      "elements": [core] + arm(0, 5, 0, 1.5, "south") + arm(11, 16, 14.5, 16, "north")})
    rot = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    parts = [{"apply": {"model": rl("block/capsule_tube_core")}}]
    parts += [{"when": {d: "true"}, "apply": {"model": rl("block/capsule_tube_arm"), **r}} for d, r in rot.items()]
    blockstate("capsule_tube", {"multipart": parts})
    block_item("capsule_tube", model=rl("block/capsule_tube_item"))
    particle_model("capsule_station")
    blockstate("capsule_station", {"variants": {f"facing={d}": {"model": rl("block/capsule_station")} for d in SIDES}})
    generated_item("capsule_station")
    for bid in ("capsule_tube", "capsule_station"):
        loot_self(bid)
        tag("block", PICK, rl(bid))
    tag("block", "minecraft:needs_stone_tool", rl("capsule_station"))
    shaped("capsule_tube", ["BGB", "G G", "BGB"], {"B": "brass_ingot", "G": "minecraft:glass"}, 8)
    shaped("capsule_station", ["PTP", "CXC", "PHP"], {"P": "steel_plate", "T": "capsule_tube", "C": "basic_circuit", "X": "minecraft:piston",
                                                      "H": "minecraft:hopper"})
    advancement("logistics/pneumatic", "Pneumatic Post", "A capsule travels 100 or more blocks", "capsule_tube", parent="logistics/belt_line")
    for k, v in {
        "block.autarkia.capsule_tube": "Capsule Tube", "block.autarkia.capsule_station": "Capsule Station", "menu.autarkia.capsule_station": "Capsule Station",
        "screen.autarkia.capsule_station.send": "Send", "screen.autarkia.capsule_station.receive": "Receive", "screen.autarkia.capsule_station.both": "Both",
        "screen.autarkia.capsule_station.whitelist": "Whitelist", "screen.autarkia.capsule_station.blacklist": "Blacklist",
        "screen.autarkia.capsule_station.exact": "Exact", "screen.autarkia.capsule_station.item_only": "Item only",
        "screen.autarkia.capsule_station.input": "Input", "screen.autarkia.capsule_station.output": "Output", "screen.autarkia.capsule_station.filter": "Filter",
        "screen.autarkia.capsule_station.tubes": "Tubes: %s", "screen.autarkia.capsule_station.stations": "Stations: %s",
        "screen.autarkia.capsule_station.capsules": "Capsules: %s", "state.autarkia.capsule.running": "Running", "state.autarkia.capsule.clogged": "Clogged",
        "state.autarkia.capsule.too_large": "Network too large", "state.autarkia.capsule.paused": "Chunks not loaded",
        "state.autarkia.capsule.no_tube": "No tube at the mouth", "state.autarkia.capsule.no_power": "No power", "state.autarkia.capsule.no_station": "No station",
        "jade.autarkia.capsule_tube": "Network: %s tubes, %s stations, %s capsules. %s", "jade.autarkia.capsule_station": "Mode %s, priority %s",
        "jade.autarkia.capsule_station_buffers": "Input %s, output %s", "jade.autarkia.capsule_station_target": "Next target: %s %s %s",
        "jade.autarkia.capsule_station_no_target": "Next target: none", "jade.autarkia.capsule_station_state": "State: %s",
        "jei.autarkia.info.capsule_tube": "Glass tube for capsules, 8 blocks/s. Connects to tubes and to station mouths on all sides. A network holds up to "
                                          "512 tubes and 32 stations; over the cap it stops. Breaking a tube drops the capsules inside it.",
        "jei.autarkia.info.capsule_station": "Sends and receives one stack per capsule. Mode Send, Receive or Both; filter; priority 0 to 9. A launch costs "
                                             "40 FE, one per 10 ticks. Capsules go to matching filtered stations first (highest priority, then nearest), else "
                                             "to stations with an empty filter. A capsule that cannot unload waits 30 s, then re-targets or returns home.",
    }.items():
        name(k, v)


def assembly(worker, frame, parts, time, fe, drops):
    write_json(f"data/autarkia/recipe/worker_assembly/{worker}.json", {
        "type": rl("worker_assembly"), "worker": rl(worker), "frame": {"ingredient": rl(frame), "count": 1},
        "parts": [{"ingredient": rl(i) if ":" not in i else i, "count": n} for i, n in parts], "time": time, "fe_per_tick": fe, "min_tier": 3})
    write_json(f"data/autarkia/loot_table/entities/{worker}.json", {"type": "minecraft:entity", "random_sequence": rl(f"entities/{worker}"), "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rl(i), "modifier": [{"type": "minecraft:set_count", "count": n, "add": False}]}]}
        for i, n in drops]})
    tag("entity_type", "autarkia:workers", rl(worker))


def workers():
    assembly("builder_drone", "drone_frame", [("basic_circuit", 2), ("servo", 4), ("copper_coil", 4), ("steel_plate", 4)], 1200, 100,
             [("drone_frame", 1), ("basic_circuit", 1), ("servo", 2), ("copper_coil", 2), ("steel_plate", 2)])
    assembly("loader", "crab_frame", [("basic_circuit", 2), ("servo", 6), ("steel_plate", 6), ("steel_gear", 4)], 1200, 100,
             [("crab_frame", 1), ("basic_circuit", 1), ("servo", 3), ("steel_plate", 3), ("steel_gear", 2)])
    assembly("lineman", "walker_frame", [("basic_circuit", 1), ("servo", 4), ("insulated_cable_spool", 1), ("steel_plate", 2)], 1000, 80,
             [("walker_frame", 1), ("servo", 2), ("steel_plate", 1)])
    for eid, title in (("builder_drone", "Builder Drone"), ("loader", "Loader"), ("lineman", "Lineman")):
        name(f"entity.autarkia.{eid}", title)
    for iid, title in (("drone_frame", "Drone Frame"), ("worker_upgrade_speed", "Speed Upgrade"), ("worker_upgrade_cargo", "Cargo Upgrade"),
                       ("worker_upgrade_armor", "Armor Upgrade"), ("worker_upgrade_range", "Range Upgrade")):
        generated_item(iid)
        name(f"item.autarkia.{iid}", title)
    name("message.autarkia.upgrade.removed", "Removed %s upgrades")
    shaped("drone_frame", ["PCP", "PXP", " C "], {"P": "steel_plate", "C": "copper_coil", "X": "basic_circuit"})
    shaped("worker_upgrade_speed", ["SCS", " X ", " C "], {"S": "servo", "C": "copper_coil", "X": "basic_circuit"})
    shaped("worker_upgrade_cargo", ["PPP", "PCP"], {"P": "steel_plate", "C": "crate"})
    shaped("worker_upgrade_armor", ["PPP", "PBP", "PPP"], {"P": "steel_plate", "B": "steel_block"})
    shaped("worker_upgrade_range", ["CXC", "CXC"], {"C": "copper_coil", "X": "basic_circuit"})
    machine_block("recharge_dock", "Recharge Dock", "recharge_dock_side", "recharge_dock_side", "recharge_dock_top")
    shaped("recharge_dock", ["PCP", "BXB", "PPP"], {"P": "steel_plate", "C": "copper_coil", "B": "battery_bank", "X": "basic_circuit"})
    name("jade.autarkia.dock.charging", "Charging")
    for k, v in (("upgrade.installed", "Installed: %s"), ("upgrade.full", "Both upgrade slots are full"),
                 ("upgrade.no_rights", "Only the owner or a trusted player can upgrade this worker")):
        name(f"message.autarkia.{k}", v)
    info = {
        "builder_drone": "T3 drone, FE battery 80,000. Flies; places, cuts and repairs blocks (any block), strings spans up to insulated. Charges at a recharge dock.",
        "loader": "T3 forklift crab, FE battery 120,000. Carries warehouse crates between racks and ledger terminals (1,500 FE per lift).",
        "lineman": "T3 walker, FE battery 60,000. Climbs poles at 0.1 blocks/tick; strings and cuts spans up to insulated, places fuse boxes and poles.",
        "recharge_dock": "50,000 FE buffer from any cable. Charges one FE worker at up to 2,000 FE/t and heals 1 health per 5 s for 500 FE. Drones land on top.",
        "drone_frame": "Frame of the Builder Drone, Surveyor, Cable-Layer and Sentinel.",
        "worker_upgrade_speed": "Right-click a worker: +20% speed, +10% move drain. 2 slots per worker.",
        "worker_upgrade_cargo": "Right-click a worker: +1 carry slot (Loader: +9 pallet slots, 2 crates per trip), +5% move drain.",
        "worker_upgrade_armor": "Right-click a worker: +6 armor, +25% max health.",
        "worker_upgrade_range": "Right-click a worker: +2 reach, +50% charger search radius.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def template(tid, index, title, site, base, max_built, reason, triggers=None, any_=None, plan_=None, handler=None, sign=None):
    obj = {"index": index, "tier": 3, "triggers": triggers or [], "site": {"rule": site, "radius": 24}, "base": base, "max_built": max_built,
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


def colony():
    cube_column("domain_beacon", "domain_beacon_side", "domain_beacon_top")
    blockstate("domain_beacon", {"variants": {"": {"model": rl("block/domain_beacon")}}})
    block_item("domain_beacon")
    loot_self("domain_beacon")
    tag("block", PICK, rl("domain_beacon"))
    tag("block", "minecraft:needs_iron_tool", rl("domain_beacon"))
    name("block.autarkia.domain_beacon", "Domain Beacon")
    shaped("domain_beacon", ["GAG", "PXP", "PPP"], {"G": "minecraft:glass", "A": "minecraft:amethyst_shard", "P": "steel_plate", "X": "basic_circuit"})
    tx = {"particle": rl("block/prop_beam"), "w": rl("block/prop_beam")}
    face = {k: {"texture": "#w"} for k in SIDES}
    block_model("prop_beam", {"textures": tx, "elements": [{"from": [5, 0, 5], "to": [11, 14, 11], "faces": face},
                                                           {"from": [1, 14, 1], "to": [15, 16, 15], "faces": face}]})
    blockstate("prop_beam", {"variants": {"": {"model": rl("block/prop_beam")}}})
    block_item("prop_beam")
    loot_self("prop_beam")
    tag("block", "minecraft:mineable/axe", rl("prop_beam"))
    name("block.autarkia.prop_beam", "Prop Beam")
    shaped("prop_beam", ["PPP", " L ", " L "], {"P": "#minecraft:planks", "L": "#minecraft:logs"}, 4)
    for iid, title in (("core_upgrade_t3", "Core Upgrade T3"), ("survey_lens", "Survey Lens")):
        generated_item(iid)
        name(f"item.autarkia.{iid}", title)
    machine_recipe("fabricating", "core_upgrade_t3", [("basic_circuit", 4), ("servo", 2), ("steel_plate", 4), ("battery_bank", 1)],
                   [("core_upgrade_t3", 1)], time=400)
    shaped("survey_lens", [" G ", "GXG", " S "], {"G": "minecraft:glass_pane", "X": "basic_circuit", "S": "steel_plate"})
    power_short = {"key": "power_short", "signal": "S03", "scale": 100}
    template("fuse_box", 14, "Fuse Box", "SR_ON_SPAN", 85, 0, {"key": "snap_memory", "signal": "mem.cable_snap", "scale": 1},
             triggers=[cond("mem.cable_snap", ">=", 0.75)], handler="fuse_box", sign="line_snapped")
    template("rack_upgrade", 15, "Rack Upgrade", "SR_REPLACE", 75, 0, {"key": "rack_load", "signal": "S10", "scale": 100},
             any_=[[cond("mem.rack_collapse", ">=", 0.5)], [cond("S10", ">=", 0.85, 1200)]], handler="rack_upgrade", sign="rack_failed")
    template("duplicate_line", 16, "Second Line", "SR_NEAR_MACHINE", 70, 0, {"key": "machine_queue", "signal": "S12.line", "scale": 1},
             triggers=[cond("S12.line", ">=", 32, 2400)], handler="connect_line")
    template("pattern_repair", 17, "Pattern Repair", "SR_IN_PLACE", 95, 0, {"key": "pattern_missing", "signal": "S21", "scale": 1},
             triggers=[cond("S21", ">", 0)], handler="pattern_repair")
    template("recharge_dock", 18, "Recharge Dock", "SR_WORK_CENTER", 55, 6, {"key": "charger_far", "signal": "S19", "scale": 1},
             any_=[[cond("S18", ">=", 1, 1200)], [cond("S19", ">=", 32)]], plan_="recharge_dock")
    template("steam_turbine", 19, "Turbine Hall", "SR_NEAR_NET", 55, 3, power_short,
             triggers=[cond("S03", ">=", 0.25, 2400), cond("S01", ">=", 512)], plan_="turbine_hall", sign="power_short")
    template("battery_bank", 20, "Battery Row", "SR_NEAR_NET", 35, 6, {"key": "store_low", "signal": "S05", "scale": 1},
             triggers=[cond("S05", "<", 60, 2400)], plan_="battery_row")
    plan("recharge_dock", (2, 5, 1), [(0, 0, 0, "recharge_dock", {"facing": "north"}), (1, 0, 0, "steel_pylon", BIG)], [0, 0, 0], "none", port=[1, 0, 0])
    found = [(x, 0, z, "foundation", None) for x in range(3) for z in range(4)]
    plan("turbine_hall", (3, 5, 4), found + [(1, 1, 0, "steam_turbine", BIG), (0, 1, 2, "boiler", BIG), (1, 4, 3, "pressure_valve", None)], [1, 1, 0],
         "none", port=[1, 1, 0])
    plan("battery_row", (3, 5, 2), [(0, 0, 1, "battery_bank", {"facing": "north"}), (2, 0, 1, "battery_bank", {"facing": "north"}),
                                    (1, 0, 0, "battery_bank", {"facing": "north"}), (1, 0, 1, "steel_pylon", BIG)], [1, 0, 1], "none", port=[1, 0, 1])
    for k, v in {"snap_memory": "A cable on this line snapped (weight %s)", "rack_load": "Rack at %s%% of its rating",
                 "machine_queue": "%s items wait at each machine", "pattern_missing": "%s blocks of a watched build are missing"}.items():
        name(f"colony.reason.autarkia.{k}", v)
    for k, v in (("beacon.outside", "Place the beacon inside your T3 or higher domain"), ("beacon.no_rights", "Only the owner or a trusted player"),
                 ("beacon.max", "This colony has all the beacons its tier allows"), ("beacon.overlap", "The beacon area would overlap another colony")):
        name(f"message.autarkia.{k}", v)
    for k, v in (("beacon.domain", "Domain +%s blocks"), ("beacon.power_ok", "Power OK"), ("beacon.power_off", "Power OFF")):
        name(f"jade.autarkia.{k}", v)
    info = {
        "domain_beacon": "Place inside a T3+ domain: adds a square of half-size 16 (T5+: 24) around it. Needs 32 FE/t; without power its area is "
                         "INACTIVE and the planner ignores it. Max 2 at T3, 3 at T4, 4 at T5, 6 at T6.",
        "prop_beam": "Temporary support. Workers put it under racks of a failing row before they replace them.",
        "core_upgrade_t3": "Use on a T2 Colony Core: T3, domain half-size 32, 14 workers, Builder Drones, warehouse, failure memory.",
        "survey_lens": "Hold it: outlines of the ore deposits within 64 blocks, through the ground, in the color of the ore.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def advancements():
    advancement("materials/pure", "Clean Separation", "Make a pure ore in a magnetic separator", "pure_iron", parent="materials/washed",
                criteria=inv_changed(tag_id="autarkia:pure_ores"))
    advancement("colony/self_repair", "Same Blocks, Same Place", "Your colony repairs a watched build from its Pattern", "pattern_card",
                parent="colony/pattern_recorded", criteria=ev("colony_event", "self_repair"), frame="goal")
    advancement("colony/second_line", "Second Line", "Your colony copies a production line from its Pattern", "jaw_crusher", parent="colony/pattern_recorded",
                criteria=ev("colony_event", "second_line"), frame="goal")
    advancement("colony/learned", "Lesson Learned", "A project from failure memory is done", "fuse_box", parent="colony/it_fixed_itself",
                criteria=ev("colony_event", "learned"))
    advancement("logistics/fetch", "Fetch!", "A Loader delivers a ledger request to you", "ledger_terminal", parent="logistics/rack_collapse")
    advancement("workers/drone_repair", "Same Blocks, Same Place", "Builder Drones finish a Pattern repair", "drone_frame", parent="workers/self_built",
                criteria=ev("worker_event", "repair_complete"))
    advancement("workers/fuse_unordered", "Lesson Learned", "A Lineman installs a fuse box nobody ordered", "fuse_box", parent="workers/self_built",
                criteria=ev("worker_event", "fuse_installed"))
    advancement("workers/loader_delivery", "Forklift Service", "A Loader brings your request", "crab_frame", parent="workers/debris_crab")


def generate():
    warehouse()
    capsules()
    workers()
    colony()
    advancements()

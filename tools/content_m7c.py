"""M7 content data, logistics part (04): cable car station and gondola, crate launcher and catch net, depot and the
linked stock view, with recipes (04 s1.9 and s2.9; crate launcher V = vantite_ingot), loot, tags, language, JEI info
pages, Jade config names and the two logistics advancements."""

from content_m6 import geo_block
from datagen import advancement, block_item, block_model, blockstate, cube_column, loot_center_only, loot_self, name, rl, shaped, simple_blockstate, tag

PICK = "minecraft:mineable/pickaxe"
IRON_TOOL = "minecraft:needs_iron_tool"
SIDES = ("north", "south", "east", "west", "up", "down")


def cable_car():
    geo_block("cable_car_station", "Cable Car Station")
    shaped("cable_car_station", ["VAV", "GWG", "FFF"], {"V": "vantite_ingot", "A": "advanced_circuit", "G": "steel_gear", "W": "copper_coil",
                                                        "F": "foundation"})
    name("entity.autarkia.gondola", "Gondola")
    name("menu.autarkia.cable_car_station", "Cable Car Station")
    screen = {"input": "Input", "output": "Output", "parts": "Gondola parts", "swing": "Swing %s°", "line": "Line: %s blocks", "unlinked": "Not linked",
              "partner": "Linked to %s %s %s"}
    for k, v in screen.items():
        name(f"screen.autarkia.cable_car.{k}", v)
    states = {"running": "Running", "slowed": "Slowed by wind", "wind_hold": "Wind hold", "obstructed": "Obstructed", "parked": "Parked",
              "no_power": "No power", "paused": "Chunks not loaded", "no_gondolas": "No gondolas"}
    for k, v in states.items():
        name(f"state.autarkia.cable_car.{k}", v)
    jade = {"unlinked": "Not linked", "line": "Line length %s blocks, gondolas %s", "state": "State: %s", "obstructed": "State: Obstructed at %s %s %s",
            "swing": "Swing: %s°"}
    for k, v in jade.items():
        name(f"jade.autarkia.cable_car.{k}", v)
    name("jade.autarkia.gondola", "Cargo %s stacks. Swing %s°")
    messages = {"first": "Station marked. Use the wrench on the second station.", "linked": "Cable line linked: %s blocks",
                "same": "Use the wrench on a different station", "not_station": "The first station is gone",
                "linked_already": "This station is already linked to %s %s %s", "too_short": "Too short: %s blocks (16 to %s)",
                "too_long": "Too long: %s blocks (16 to %s)", "too_steep": "Height difference %s blocks (at most 48)", "blocked": "Blocked at %s %s %s"}
    for k, v in messages.items():
        name(f"message.autarkia.cable_car.{k}", v)
    name("jei.autarkia.info.cable_car_station", "2 x 3 x 2 end of a cable car line. Wrench one station, then another 16 to 160 blocks away (height "
                                                 "difference up to 48); both cables and the space under them must be free. 1 to 8 gondolas (4 steel "
                                                 "plates + 2 iron chains each) carry 27 stacks at 6 blocks/s for 64 FE/t; they stop 60 to 200 ticks at "
                                                 "each station. Strong wind slows the line above 30 degrees of swing and holds it above 45.")


def launcher():
    geo_block("crate_launcher", "Crate Launcher")
    shaped("crate_launcher", ["VDV", "PXP", "FAF"], {"V": "vantite_ingot", "D": "minecraft:dispenser", "P": "steel_plate", "X": "minecraft:piston",
                                                     "A": "advanced_circuit", "F": "foundation"})
    net = {"particle": rl("block/catch_net"), "net": rl("block/catch_net")}
    for model, y0 in (("catch_net", 10), ("catch_net_bounce", 7)):
        block_model(model, {"ambientocclusion": False, "textures": net, "elements": [
            {"from": [0, y0, 0], "to": [16, y0 + 2, 16], "faces": {k: {"texture": "#net"} for k in SIDES}}]})
    blockstate("catch_net", {"variants": {"bounce=false": {"model": rl("block/catch_net")}, "bounce=true": {"model": rl("block/catch_net_bounce")}}})
    block_item("catch_net")
    loot_center_only("catch_net")
    tag("block", PICK, rl("catch_net"))
    name("block.autarkia.catch_net", "Catch Net")
    shaped("catch_net", ["VSV", "SLS", "VSV"], {"V": "vantite_ingot", "S": "minecraft:string", "L": "minecraft:slime_ball"})
    name("menu.autarkia.crate_launcher", "Crate Launcher")
    states = {"ready": "Ready", "no_target": "No target (wrench the launcher, then a net)", "out_of_range": "Target out of range",
              "waiting": "Waiting for items", "cooldown": "Reloading", "no_crate": "No empty crate", "no_power": "No power",
              "busy": "Too many crates in the air", "no_arc": "No clear arc", "paused": "Paused: check arc and wind"}
    for k, v in states.items():
        name(f"state.autarkia.launcher.{k}", v)
    screen = {"reset": "Reset", "no_target": "No target", "target": "Target %s %s %s", "distance": "Distance %s, angle %s°", "hit": "Last shot: HIT",
              "miss": "Last shot: MISS by %s blocks", "miss_full": "Last shot: MISS by %s blocks (net output full)", "none": "Last shot: none"}
    for k, v in screen.items():
        name(f"screen.autarkia.launcher.{k}", v)
    name("jade.autarkia.launcher.target", "Target %s blocks, angle %s°")
    name("jade.autarkia.launcher.energy", "Energy %s / %s FE")
    messages = {"first": "Launcher marked. Use the wrench on a catch net.", "targeted": "Target set: catch net at %s %s %s, %s blocks",
                "range": "The net must be 8 to 96 blocks away", "height": "The net must be at most 32 blocks lower or 24 blocks higher",
                "no_launcher": "Use the wrench on a crate launcher first"}
    for k, v in messages.items():
        name(f"message.autarkia.launcher.{k}", v)
    name("jade.autarkia.catch_net", "Output below: %s")
    for k, v in (("ok", "OK"), ("missing", "Missing"), ("full", "Full")):
        name(f"jade.autarkia.catch_net.{k}", v)
    name("jade.autarkia.falling_crate", "%s, contents %s stacks")
    name("jade.autarkia.falling_crate.mode.collapse", "Collapse")
    name("jade.autarkia.falling_crate.mode.launched", "Launched")
    name("config.jade.plugin_autarkia.catch_net", "Catch net output")
    name("config.jade.plugin_autarkia.logistics_entity", "Gondola and falling crate")
    name("jei.autarkia.info.crate_launcher", "Throws a crate with all 27 input stacks on a ballistic arc (45, 55, 65 or 75 degrees, the lowest clear "
                                              "one) into a catch net 8 to 96 blocks away. Wrench the launcher, then the net. Needs an empty crate and "
                                              "1,000 FE + 40 FE per block; one shot per 60 ticks. Fires when the input is full or idle for 10 s. Three "
                                              "misses in 10 minutes pause it until Reset.")
    name("jei.autarkia.info.catch_net", "3 x 3 net. Catches launched crates that come down within 1.5 blocks of its centre and puts the contents "
                                        "and the empty crate into the storage directly below its centre. No room below: the crate bounces off and "
                                        "lands beside the net as a crate block.")


def depot():
    cube_column("depot", "depot_side", "depot_top")
    simple_blockstate("depot")
    block_item("depot")
    loot_self("depot")
    tag("block", PICK, rl("depot"))
    tag("block", IRON_TOOL, rl("depot"))
    name("block.autarkia.depot", "Depot")
    name("menu.autarkia.depot", "Depot")
    shaped("depot", ["VAV", "PXP", "VPV"], {"V": "vantite_ingot", "A": "advanced_circuit", "P": "steel_plate", "X": "crate"})
    name("jade.autarkia.depot.main", "Site: main")
    name("jade.autarkia.depot.outpost", "Site: outpost at %s %s %s")
    name("screen.autarkia.depot.name", "Site name")
    name("screen.autarkia.depot.linked", "Linked view")
    name("screen.autarkia.ledger.sites", "Sites")
    sites = {"title": "Linked Stock View", "back": "Back", "request": "Request from site", "main": "Main site", "depot": "Depot %s %s %s",
             "orders": "Transfer orders", "slots": "Slots %s / %s", "where": "%s %s %s in %s", "last_seen": "Last seen %s min ago",
             "order.waiting": "Waiting %s/%s", "order.moving": "Moving %s/%s", "order.done": "Done %s/%s", "order.partial": "Partial %s/%s",
             "order.failed": "Failed %s/%s", "reason.no_transport": "No transport at site", "reason.none_available": "None left at the site"}
    for k, v in sites.items():
        name(f"screen.autarkia.sites.{k}", v)
    messages = {"ordered": "Transfer order queued", "none_available": "None of that item at the site", "not_remote": "Choose a row of a remote site",
                "unknown": "That site is gone"}
    for k, v in messages.items():
        name(f"message.autarkia.sites.{k}", v)
    name("jei.autarkia.info.depot", "54-slot outpost storage. It belongs to the outpost core within 16 blocks (else to the main site) and reports its "
                                     "stock to the linked stock view of every ledger terminal. Remote site requests push the items into an adjacent "
                                     "cable car station, crate launcher or capsule station.")


def advancements():
    advancement("logistics/sky_freight", "Sky Freight", "A loaded gondola crosses a 64+ block line swinging more than 15 degrees", "cable_car_station",
                parent="logistics/fetch")
    advancement("logistics/special_delivery", "Special Delivery", "A catch net catches a crate launched 48+ blocks", "crate_launcher",
                parent="logistics/fetch")


def generate():
    cable_car()
    launcher()
    depot()
    advancements()

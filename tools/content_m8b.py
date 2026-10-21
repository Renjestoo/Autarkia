"""M8 content data, power part (02, T5): the cryo node, the superconductor spool, the superconducting coil and the plasma
generator, with recipes (02 section 8), loot, tags, the power/quench advancement, screen, Jade and JEI texts. The
coolant_cell, lattice_fuel_cell, lattice_crystal, lattice_alloy_ingot, lattice_alloy_block and ai_core data come with the
materials part."""

from datagen import advancement, generated_item, loot_center_only, name, particle_model, rl, shaped, simple_blockstate, tag

PICK = "minecraft:mineable/pickaxe"
DIAMOND_TOOL = "minecraft:needs_diamond_tool"


def geo_block(bid, title):
    particle_model(bid)
    simple_blockstate(bid)
    generated_item(bid)
    loot_center_only(bid)
    tag("block", PICK, rl(bid))
    tag("block", DIAMOND_TOOL, rl(bid))
    name(f"block.autarkia.{bid}", title)


def power():
    for bid, title in (("cryo_node", "Cryo Node"), ("superconducting_coil", "Superconducting Coil"), ("plasma_generator", "Plasma Generator")):
        geo_block(bid, title)
    generated_item("superconductor_spool")
    name("item.autarkia.superconductor_spool", "Superconductor Spool")
    tag("item", "autarkia:load/dense", rl("superconductor_spool"))
    shaped("cryo_node", [" X ", "LVL", " V "], {"X": "coolant_cell", "L": "lattice_alloy_ingot", "V": "vantite_steel_ingot"})
    shaped("superconductor_spool", ["VLV", "LCL", "VLV"], {"V": "vantite_ingot", "L": "lattice_crystal", "C": "copper_wire_spool"}, 4)
    shaped("superconducting_coil", ["LSL", "SAS", "LSL"], {"L": "lattice_alloy_block", "S": "superconductor_spool", "A": "ai_core"})
    shaped("plasma_generator", ["LCL", "XAX", "LBL"], {"L": "lattice_alloy_ingot", "C": "copper_coil", "X": "coolant_cell", "A": "ai_core",
                                                       "B": "vantite_steel_block"})
    advancement("power/quench", "Quench", "A superconductor span or coil quenches", "superconductor_spool", parent="power/red_hot", frame="goal")


def lang():
    jade = {"coolant": "Coolant: %s s", "temperature": "Temperature: %s%%", "quenched": "Quenched: no I/O until cold again",
            "plasma.state": "State: %s", "plasma.ignition": "Ignition: %s%%"}
    for k, v in jade.items():
        name(f"jade.autarkia.{k}", v)
    for k, v in {"cold": "Cold", "igniting": "Igniting", "ramping": "Ramping up", "running": "Running", "hot_idle": "Hot, idle"}.items():
        name(f"plasma.autarkia.state.{k}", v)
    for k, v in {"none": "none", "in": "in", "out": "out", "both": "in and out"}.items():
        name(f"face.autarkia.{k}", v)
    for d in ("down", "up", "north", "south", "west", "east"):
        name(f"direction.autarkia.{d}", d.capitalize())
    name("message.autarkia.coil.face", "%s face: %s")
    name("message.autarkia.coil.faces", "Faces: %s")
    screen = {"cryo.no_spans": "No spans", "cryo.span": "Span %s: %s%%", "cryo.coolant": "Coolant: %s s", "coil.quenched": "QUENCHED",
              "plasma.ignite": "Ignite", "plasma.hot": "Hot for %s s", "plasma.ignition": "Ignition: %s%%"}
    for k, v in screen.items():
        name(f"screen.autarkia.{k}", v)
    name("tooltip.autarkia.coolant", "%s / %s coolant units")
    name("tooltip.autarkia.coil.temperature", "Temperature %s%%, I/O limit %s FE/t")
    info = {
        "cryo_node": "1 x 3 x 1 superconductor node, 4 spans at its top. 4 coolant cell slots; each cell holds 24,000 units. It uses 1 unit per "
                     "tick plus 1 per attached span above half load, and it is cold while it has coolant. A span warms when one of its ends "
                     "is not cold and quenches with a blast at full temperature (about 25 s).",
        "superconductor_spool": "Superconductor: 65,536 FE/t, spans up to 32 blocks between cryo nodes or machine ports, no loss. One spool per 8 "
                                "blocks. Both ends must stay cold: frost below 30%, steam to 70%, hiss above, then a quench blast that drops "
                                "nothing.",
        "superconducting_coil": "2 x 2 x 2 storage: 400,000,000 FE, I/O 65,536 FE/t (half from 50% temperature). 4 coolant cell slots, 2 units "
                                "per tick. Dry it warms 0.1% per tick and quenches after 1,000 ticks: a blast of power 2 to 6, the energy "
                                "is lost and it stays quenched until cold again. The wrench cycles a face: none, in, out, both.",
        "plasma_generator": "3 x 3 x 3 generator: 32,768 FE/t. Ignition (button) takes 1,000,000 FE from the network at 5,000 FE/t, then the "
                            "output ramps up over 10 s. One lattice fuel cell lasts 2,400 ticks at full output. A full buffer pauses the "
                            "burn; after 10 s of pause it is cold and needs a new ignition. Breaking it while hot makes an explosion. "
                            "Buffer 1,000,000 FE, out 65,536 FE/t.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def generate():
    power()
    lang()

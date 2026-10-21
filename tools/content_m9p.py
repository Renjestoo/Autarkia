"""M9 content data, power part (02, T6): the singularity core, the gyro heart and the phase node, with recipes (02 section
8), loot, tags, the power/phase_link and power/drink_the_blast advancements, screen, Jade, JEI and message texts. The
aegis_plate, ai_core, lattice_* and superconducting_coil data come with other parts or milestones."""

from datagen import advancement, block_item, cube_all, generated_item, loot_center_only, loot_self, name, particle_model, rl, shaped, \
    simple_blockstate, tag

PICK = "minecraft:mineable/pickaxe"
DIAMOND_TOOL = "minecraft:needs_diamond_tool"


def blocks():
    for bid, title in (("singularity_core", "Singularity Core"), ("gyro_heart", "Gyro Heart")):
        particle_model(bid)
        simple_blockstate(bid)
        generated_item(bid)
        loot_center_only(bid)
        tag("block", PICK, rl(bid))
        tag("block", DIAMOND_TOOL, rl(bid))
        name(f"block.autarkia.{bid}", title)
    cube_all("phase_node")
    simple_blockstate("phase_node")
    block_item("phase_node")
    loot_self("phase_node")
    tag("block", PICK, rl("phase_node"))
    tag("block", DIAMOND_TOOL, rl("phase_node"))
    name("block.autarkia.phase_node", "Phase Node")


def recipes():
    shaped("singularity_core", ["PAP", "ZNZ", "PAP"], {"P": "aegis_plate", "A": "ai_core", "Z": "superconducting_coil", "N": "minecraft:nether_star"})
    shaped("gyro_heart", ["PFP", "LCL", "PFP"], {"P": "aegis_plate", "F": "stone_flywheel", "L": "lattice_alloy_block", "C": "ai_core"})
    shaped("phase_node", [" E ", "PAP", " L "], {"E": "minecraft:ender_eye", "P": "lattice_alloy_ingot", "A": "ai_core", "L": "lattice_crystal"}, 2)


def advancements():
    advancement("power/phase_link", "No Wires Attached", "Two phase nodes linked", "phase_node", parent="power/quench")
    advancement("power/drink_the_blast", "Nothing Breaks", "The Singularity Core absorbs an explosion", "singularity_core", parent="power/phase_link",
                frame="challenge")


def lang():
    jade = {"singularity.state": "State: %s", "singularity.ignition": "Ignition: %s%%", "gyro.energy": "Energy: %s FE",
            "phase.links": "Links: %s", "phase.sight": "Line of sight: %s", "phase.ok": "ok", "phase.blocked": "blocked"}
    for k, v in jade.items():
        name(f"jade.autarkia.{k}", v)
    for k, v in {"off": "Off", "igniting": "Igniting", "online": "Online"}.items():
        name(f"singularity.autarkia.state.{k}", v)
    screen = {"singularity.ignite": "Ignite", "singularity.stop": "Stop", "singularity.base": "Base output: %s FE/t",
              "singularity.points": "Damage points paid: %s", "singularity.explosions": "Explosions absorbed: %s",
              "singularity.paid": "FE paid (60 s): %s", "singularity.ignition": "Ignition: %s%%"}
    for k, v in screen.items():
        name(f"screen.autarkia.{k}", v)
    name("message.autarkia.singularity.stopped", "Singularity Core stopped")
    name("message.autarkia.gyro.spin_down", "Spin it down first.")
    name("message.autarkia.phase.linked", "Phase link: %s blocks")
    info = {
        "singularity_core": "5 x 5 x 5 generator: 131,072 FE/t into a 1,000,000,000 FE buffer, out 1,048,576 FE/t through its machine port or "
                            "adjacent connectors. Ignite takes 100,000,000 FE from its network at up to 1,048,576 FE/t; then it runs while "
                            "loaded. Stop (button or sneaking wrench) keeps the buffer. In a T6 domain it drinks every explosion: 50,000 FE x "
                            "radius squared, no domain block breaks and every lamp flashes. It pays the Aegis tether.",
        "gyro_heart": "5 x 3 x 5 flywheel storage: 2,000,000,000 FE, I/O 1,048,576 FE/t x max(0.1, spin), spin = sqrt(E / max). Self-loss "
                      "1 + 0.0000001 x E per tick. From 1% charge it cannot be broken by hand or by explosions: spin it down first. "
                      "The wrench cycles a face: none, in, out, both.",
        "phase_node": "Wireless phase link node: 1,048,576 FE/t, links up to 64 blocks with a clear line of sight, no loss, no heat, no sag. "
                      "Use the wrench on one node, then on the other node or a transformer. 4 links per node; it feeds the blocks next to it. "
                      "A blocked line turns the link off within 40 ticks; it comes back by itself.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def generate():
    blocks()
    recipes()
    advancements()
    lang()

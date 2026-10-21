"""M9 content data, restoration part (05 sections 8.5 and 3.3, 05b section 9): the restoration_projector block (loot, tags,
recipe), template 29 restoration_projector with its colony/restoration_projector plan, the colony/undo_crater advancement
and the projector screen, Jade, JEI and template language."""

from content_m3 import BIG, cond, plan
from content_m6c import ev
from content_m8b import geo_block
from content_m8r import template
from datagen import advancement, name, shaped

STATES = {"no_snapshot": "No snapshot", "snapshot": "Taking the snapshot", "watching": "Watching the field", "restoring": "Restoring",
          "no_power": "No power", "no_matter": "No material for some cells", "outside": "Outside a colony domain",
          "disabled": "Restoration is switched off"}


def projector():
    geo_block("restoration_projector", "Restoration Projector")
    shaped("restoration_projector", ["PLP", "AXA", "PFP"], {"P": "aegis_plate", "L": "lattice_crystal", "A": "ai_core", "X": "minecraft:beacon",
                                                            "F": "foundation"})
    for k, v in STATES.items():
        name(f"jade.autarkia.restoration.state.{k}", v)
    name("jade.autarkia.restoration.snapshot", "Snapshot %sx%sx%s")
    name("jade.autarkia.restoration.no_snapshot", "No snapshot")
    name("jade.autarkia.restoration.missing", "Missing %s")
    for k, v in {"snapshot": "Snapshot", "reading": "Taking the snapshot: %s%%", "size": "Box size (shift: step 8)",
                 "offset": "Box offset from the projector"}.items():
        name(f"screen.autarkia.restoration.{k}", v)
    name("menu.autarkia.restoration_projector", "Restoration Projector")
    name("jei.autarkia.info.restoration_projector", "T6, 1 x 2 x 1. Snapshots a box of up to 64 x 32 x 64 inside your colony domain (default: "
         "centered, from 8 below to 23 above the projector) and puts lost blocks back: every 100 ticks it finds the cells that are now air, "
         "fluid, fire or replaceable, and restores them edge first, 8 per tick (32 per server), 512 FE each. Debris items within 16 blocks fly "
         "back first; else the matter buffers pay. Containers come back empty. Blocks you or a trusted player break or place in the box "
         "update the snapshot. The first power takes the snapshot; the Snapshot button takes it again.")
    print("  restoration_projector block, recipe, screen, Jade and JEI text")


def templates():
    template("restoration_projector", 29, 6, "Restoration Projector", "SR_EVENT_CLUSTER", 60, 8,
             {"key": "machine_destroyed", "signal": "mem.machine_destroyed", "scale": 1}, triggers=[cond("mem.machine_destroyed", ">=", 1)],
             plan_="restoration_projector")
    plan("restoration_projector", (1, 3, 1), [(0, 0, 0, "foundation", None), (0, 1, 0, "restoration_projector", BIG)], [0, 1, 0], "none",
         port=[0, 1, 0])
    name("colony.reason.autarkia.machine_destroyed", "Machines were destroyed here (memory weight %s) and no Restoration Field covers it")
    print("  template 29 restoration_projector; plan colony/restoration_projector")


def advancements():
    advancement("colony/undo_crater", "Undo the Crater", "A Restoration Field puts back 64 or more blocks after one blast", "restoration_projector",
                parent="colony/self_repair", criteria=ev("colony_event", "field_restored_64"), frame="goal")
    print("  advancement colony/undo_crater")


def generate():
    print("content_m9x: restoration")
    projector()
    templates()
    advancements()
    print("content_m9x: done")

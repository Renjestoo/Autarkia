"""Player guidance data: the "Autarkia: Getting Started" advancement tab. Each step's description says what to do to earn it,
and the next step appears when the step before it is done. guide/root is awarded on the first join (FieldManualItem),
guide/first_span by CableItem; the other steps use inventory and colony triggers."""

from datagen import advancement, rl


def has(item):
    return {"got": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}}


def ev(trigger, event):
    return {"done": {"trigger": rl(trigger), "conditions": {"event": event}}}


STEPS = [
    ("tin", "Tin Strike", "Mine tin ore. Small veins are common from Y -16 to 96; big deposits sit near the surface.", "raw_tin",
     has("autarkia:raw_tin"), "task"),
    ("crucible", "Hot Pot", "Craft a Crucible from 7 bricks and heat it from below: a campfire, a lit furnace or lava.", "crucible",
     has("autarkia:crucible"), "task"),
    ("bronze", "Bronze Age", "Put 3 copper ingots and 1 tin ingot in the heated Crucible for 4 bronze ingots.", "bronze_ingot",
     has("autarkia:bronze_ingot"), "task"),
    ("crank", "Elbow Grease", "Craft bronze gears and a Hand Crank. Hold use on the crank to make 20 FE/t.", "hand_crank",
     has("autarkia:hand_crank"), "task"),
    ("first_span", "Strung Up", "Use a Rope Coil on the Hand Crank, then on a machine or Rope Pulley, to carry the power.", "rope_coil",
     None, "task"),
    ("stamp_mill", "Heavy Hammer", "Craft a Stamp Mill and power it over a rope span.", "stamp_mill", has("autarkia:stamp_mill"), "task"),
    ("crushed", "Double Yield", "Crush raw ore in the powered Stamp Mill. Smelt crushed ore for extra ingots.", "crushed_iron",
     has("#autarkia:crushed_ores"), "task"),
    ("core_seed", "The Seed", "Craft a Core Seed from wheat seeds, bronze ingots and a bronze gear.", "core_seed",
     has("autarkia:core_seed"), "task"),
    ("colony", "It Lives", "Plant the Core Seed on grass, dirt or stone. The Colony Core grows and gives you a Porter.", "worker_assembler",
     ev("colony_event", "root"), "goal"),
    ("proposal", "It Has Ideas", "Keep items in storage near the Core. The colony proposes its first project; approve it in the Core screen.",
     "project_board", ev("colony_event", "first_proposal"), "task"),
    ("built", "Hands Off", "Let your Porters build an approved project from your storage.", "worker_capsule",
     ev("worker_event", "job_complete:build_t1"), "task"),
    ("tier_2", "Steam Ahead", "Craft a Core Upgrade (Tier 2) and use it on the Colony Core.", "core_upgrade_t2", ev("colony_event", "tier_2"), "goal"),
    ("tier_3", "Wired", "Make a Core Upgrade (Tier 3) in a Fabricator: 4 basic circuits, 2 servos, 4 steel plates and a battery bank.",
     "core_upgrade_t3", ev("colony_event", "tier_3"), "goal"),
    ("tier_4", "Go Far", "Fabricate a Core Upgrade (Tier 4): 4 advanced circuits, 8 vantite steel ingots and 4 servos.", "core_upgrade_t4",
     ev("colony_event", "tier_4"), "goal"),
    ("tier_5", "Matter Matters", "Fabricate a Core Upgrade (Tier 5): an AI core, 8 lattice alloy ingots and 8 nanite paste.",
     "core_upgrade_t5", ev("colony_event", "tier_5"), "goal"),
    ("tier_6", "Self-Sufficient", "Fabricate a Core Upgrade (Tier 6): 2 aegis plates, 2 AI cores and 8 lattice crystals.", "core_upgrade_t6",
     ev("colony_event", "tier_6"), "challenge"),
]


def generate():
    advancement("guide/root", "Autarkia: Getting Started", "A factory that builds itself. Open your Field Manual, then follow this tab.",
                "field_manual", background="minecraft:gui/advancements/backgrounds/stone")
    parent = "guide/root"
    for key, title, desc, icon, criteria, frame in STEPS:
        path = f"guide/{key}"
        advancement(path, title, desc, icon, parent=parent, criteria=criteria, frame=frame)
        parent = path

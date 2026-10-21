"""M6 content data, second part: the electric equipment (07 section 1.4), then the T3 logistics, workers and colony data
(04, 05, 06). The power and processing data are in content_m6."""

from content_m5 import rack_block
from datagen import advancement, generated_item, item_def, item_model, machine_recipe, name, rl, shaped, tag, write_json


def equipment():
    for iid, title in [("electric_drill", "Electric Drill"), ("electric_chainsaw", "Electric Chainsaw"), ("arc_rifle", "Arc Rifle")]:
        item_model(iid, {"parent": "minecraft:item/handheld", "textures": {"layer0": rl(f"item/{iid}")}})
        item_def(iid, obj={"model": {"type": "minecraft:special", "base": rl(f"item/{iid}"), "model": {"type": "geckolib:geckolib"}}})
        name(f"item.autarkia.{iid}", title)
    generated_item("shock_baton", handheld=True)
    name("item.autarkia.shock_baton", "Shock Baton")
    for iid, title, atag in [("exo_rig_helmet", "Exo Rig Helmet", "minecraft:head_armor"), ("exo_rig_chest", "Exo Rig Chestplate", "minecraft:chest_armor"),
                             ("exo_rig_legs", "Exo Rig Leggings", "minecraft:leg_armor"), ("exo_rig_boots", "Exo Rig Boots", "minecraft:foot_armor")]:
        generated_item(iid)
        name(f"item.autarkia.{iid}", title)
        tag("item", atag, rl(iid))
    write_json("assets/autarkia/equipment/exo_rig.json", {"layers": {
        "humanoid": [{"texture": "autarkia:exo_rig"}], "humanoid_leggings": [{"texture": "autarkia:exo_rig"}]}})
    tag("item", "minecraft:pickaxes", rl("electric_drill"))
    tag("item", "minecraft:shovels", rl("electric_drill"))
    tag("item", "minecraft:axes", rl("electric_chainsaw"))
    tag("item", "minecraft:swords", rl("shock_baton"))
    tag("item", "autarkia:electric_tool_materials", rl("steel_plate"))
    tag("item", "autarkia:repairs_exo_rig", rl("steel_plate"))
    for dt, msg, player in (("shock", "%1$s was shocked", "%1$s was shocked by %2$s"), ("arc", "%1$s was struck by an arc", "%1$s was arced by %2$s")):
        write_json(f"data/autarkia/damage_type/{dt}.json", {"message_id": f"autarkia.{dt}", "exhaustion": 0.1,
                                                              "scaling": "when_caused_by_living_non_player"})
        name(f"death.attack.autarkia.{dt}", msg)
        name(f"death.attack.autarkia.{dt}.player", player)
    recipes = {
        "electric_drill": ([("steel_plate", 3), ("steel_gear", 1), ("servo", 1), ("basic_circuit", 2), ("capacitor_bank", 1), ("minecraft:diamond", 1)],
                           20000, 200),
        "electric_chainsaw": ([("steel_plate", 3), ("steel_gear", 2), ("servo", 1), ("basic_circuit", 2), ("capacitor_bank", 1)], 20000, 200),
        "shock_baton": ([("steel_ingot", 2), ("copper_coil", 2), ("basic_circuit", 1), ("capacitor_bank", 1), ("minecraft:leather", 1)], 10000, 100),
        "exo_rig_helmet": ([("steel_helmet", 1), ("steel_plate", 4), ("basic_circuit", 1), ("minecraft:glass_pane", 1), ("capacitor_bank", 1)], 20000, 200),
        "exo_rig_chest": ([("steel_chest", 1), ("steel_plate", 6), ("servo", 2), ("basic_circuit", 2), ("minecraft:piston", 2), ("capacitor_bank", 1)],
                          40000, 300),
        "exo_rig_legs": ([("steel_legs", 1), ("steel_plate", 5), ("servo", 2), ("basic_circuit", 1), ("capacitor_bank", 1)], 30000, 250),
        "exo_rig_boots": ([("steel_boots", 1), ("steel_plate", 3), ("servo", 1), ("minecraft:slime_ball", 2), ("capacitor_bank", 1)], 20000, 200),
        "arc_rifle": ([("steel_plate", 4), ("copper_coil", 4), ("basic_circuit", 2), ("servo", 1), ("capacitor_bank", 2), ("minecraft:lightning_rod", 1)],
                      60000, 400),
    }
    for iid, (inputs, fe, ticks) in recipes.items():
        machine_recipe("fabricating", iid, inputs, [(iid, 1)], time=ticks, energy=-(-fe // ticks))
    ev = lambda event: {"done": {"trigger": rl("equipment_event"), "conditions": {"event": event}}}
    advancement("equipment/live_wire", "Live Wire", "Hit 4 targets with one arc rifle shot", "arc_rifle", parent="equipment/full_pressure",
                criteria=ev("arc_chain_4"))
    advancement("equipment/clean_cut", "Clean Cut", "Fell 64 or more logs in one chainsaw cut", "electric_chainsaw", parent="equipment/full_pressure",
                criteria=ev("fell_64"))
    name("key.autarkia.suit", "Suit on/off")
    name("tooltip.autarkia.suit.on", "Suit: on")
    name("tooltip.autarkia.suit.off", "Suit: off")
    for k, v in (("suit.none", "No suit pieces worn"), ("suit.on", "Suit on"), ("suit.off", "Suit off")):
        name(f"message.autarkia.{k}", v)
    for k, v in (("fabricator.prev", "Previous recipe"), ("fabricator.next", "Next recipe"), ("fabricator.clear", "No target"),
                 ("fabricator.none", "No target recipe"), ("fabricator.target", "Target: %s")):
        name(f"screen.autarkia.{k}", v)
    info = {
        "electric_drill": "Pickaxe and shovel, diamond level, speed 12. 400,000 FE, 200 FE per block. Empty: speed 1, no drops.",
        "electric_chainsaw": "Axe, speed 12, 9 damage. Cutting a log fells the whole tree (up to 256 logs, 16 per tick, 150 FE per log). Sneak to cut one log.",
        "shock_baton": "6 damage. With 1,000 FE a hit adds 4 shock damage and Slowness II for 2 s. 200,000 FE.",
        "arc_rifle": "5,000 FE per shot, 32 blocks. 10 arc damage, then 3 hops within 6 blocks at x0.7 (7, 4.9, 3.4). Skips you, your pets, villagers and workers. 1 s cooldown. 1,000,000 FE.",
        "exo_rig_helmet": "3 armor, 2 toughness. Balances FE between the worn exo pieces every second, +1 oxygen. 500,000 FE. Suit key: on/off.",
        "exo_rig_chest": "7 armor, 2 toughness. +0.25 jump strength, 100 FE per jump. 500,000 FE.",
        "exo_rig_legs": "6 armor, 2 toughness. Step up 1.1 blocks, 2 FE per tick while walking. 500,000 FE.",
        "exo_rig_boots": "3 armor, 2 toughness. Cancels fall damage above 3 blocks for 100 FE per block. 500,000 FE.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def logistics():
    rack_block("steel_rack", "Steel Rack", "minecraft:mineable/pickaxe", 3)
    tag("block", "minecraft:needs_stone_tool", rl("steel_rack"))
    shaped("steel_rack", ["I I", "PPP", "I I"], {"I": "steel_ingot", "P": "steel_plate"})
    name("jei.autarkia.info.steel_rack", "Holds 3 crates (filled or empty). Load rating 8,192 units. Bends above 100%, collapses after 60 s above 120%, "
                                          "at once above 150%. Drops 2 steel nuggets when it collapses.")
    name("menu.autarkia.rack_3", "Steel Rack")


def generate():
    equipment()
    logistics()

"""M7 content data, equipment part: the 8 vantite items (07 section 1.5), the grapple hook entity, the frontier suit
equipment asset, the rail damage type, the vantite and frontier repair tags, recipes 29 to 36 (07 section 5), JEI info
pages, the swing_line advancement (07 section 7.3) and the language entries. The sound events come from sounds_spec."""

from datagen import advancement, generated_item, item_def, item_model, machine_recipe, name, rl, shaped, tag, write_json

GEO_ITEMS = (("grapple_launcher", "Grapple Launcher"), ("rail_rifle", "Rail Rifle"))
SUIT = (("helmet", "Helmet", "minecraft:head_armor"), ("chest", "Chestplate", "minecraft:chest_armor"),
        ("legs", "Leggings", "minecraft:leg_armor"), ("boots", "Boots", "minecraft:foot_armor"))


def items():
    for iid, title in GEO_ITEMS:
        item_model(iid, {"parent": "minecraft:item/handheld", "textures": {"layer0": rl(f"item/{iid}")}})
        item_def(iid, obj={"model": {"type": "minecraft:special", "base": rl(f"item/{iid}"), "model": {"type": "geckolib:geckolib"}}})
        name(f"item.autarkia.{iid}", title)
    generated_item("vantite_excavator", handheld=True)
    name("item.autarkia.vantite_excavator", "Vantite Excavator")
    generated_item("vantite_blade", handheld=True)
    name("item.autarkia.vantite_blade", "Vantite Blade")
    for piece, title, atag in SUIT:
        iid = f"frontier_suit_{piece}"
        generated_item(iid)
        name(f"item.autarkia.{iid}", f"Frontier Suit {title}")
        tag("item", atag, rl(iid))
    write_json("assets/autarkia/equipment/frontier_suit.json", {"layers": {
        "humanoid": [{"texture": "autarkia:frontier_suit"}], "humanoid_leggings": [{"texture": "autarkia:frontier_suit"}]}})
    tag("item", "minecraft:pickaxes", rl("vantite_excavator"))
    tag("item", "minecraft:shovels", rl("vantite_excavator"))
    tag("item", "minecraft:swords", rl("vantite_blade"))
    tag("item", "autarkia:vantite_tool_materials", rl("vantite_steel_ingot"))
    tag("item", "autarkia:repairs_frontier_suit", rl("vantite_steel_ingot"))
    name("entity.autarkia.grapple_hook", "Grapple Hook")
    write_json("data/autarkia/damage_type/rail.json", {"message_id": "autarkia.rail", "exhaustion": 0.1, "scaling": "when_caused_by_living_non_player"})
    name("death.attack.autarkia.rail", "%1$s was pierced by a rail slug")
    name("death.attack.autarkia.rail.player", "%1$s was railed by %2$s")


def recipes():
    fab = {
        "vantite_excavator": ([("vantite_steel_ingot", 5), ("steel_gear", 2), ("servo", 2), ("advanced_circuit", 2), ("battery_bank", 1),
                               ("minecraft:diamond", 2)], 150000, 400),
        "grapple_launcher": ([("vantite_steel_ingot", 3), ("servo", 2), ("advanced_circuit", 1), ("battery_bank", 1), ("minecraft:tripwire_hook", 1),
                              ("hv_cable_spool", 1)], 100000, 300),
        "frontier_suit_helmet": ([("exo_rig_helmet", 1), ("vantite_steel_ingot", 5), ("advanced_circuit", 1), ("battery_bank", 1),
                                  ("minecraft:golden_carrot", 1)], 150000, 400),
        "frontier_suit_chest": ([("exo_rig_chest", 1), ("vantite_steel_ingot", 8), ("advanced_circuit", 2), ("battery_bank", 1),
                                 ("minecraft:phantom_membrane", 2)], 200000, 500),
        "frontier_suit_legs": ([("exo_rig_legs", 1), ("vantite_steel_ingot", 7), ("servo", 2), ("advanced_circuit", 1), ("battery_bank", 1)], 175000, 450),
        "frontier_suit_boots": ([("exo_rig_boots", 1), ("vantite_steel_ingot", 4), ("advanced_circuit", 1), ("battery_bank", 1), ("minecraft:blaze_rod", 2)],
                                150000, 400),
        "rail_rifle": ([("arc_rifle", 1), ("vantite_steel_ingot", 6), ("copper_coil", 8), ("advanced_circuit", 2), ("battery_bank", 2)], 300000, 600),
    }
    for iid, (inputs, fe, ticks) in fab.items():
        machine_recipe("fabricating", iid, inputs, [(iid, 1)], time=ticks, energy=-(-fe // ticks))
    shaped("vantite_blade", [" V ", " V ", " P "], {"V": "vantite_steel_ingot", "P": "steel_ingot"}, category="equipment")


def info():
    pages = {
        "vantite_excavator": "Pickaxe and shovel, netherite level, speed 16. 2,000,000 FE, 250 FE per block. Mode key: 1x1, 3x3 (x0.5 speed) or vein: "
                             "breaks up to 64 connected blocks of the same ore (stone and deepslate variants together), 400 FE each. Empty: speed 1, "
                             "no drops.",
        "grapple_launcher": "2,000 FE per shot: the hook flies 48 blocks at 3 blocks/tick and holds on the first block it hits. Hold use: pulled at "
                            "1.2 blocks/tick for 20 FE/t; sneak to hang in place for 5 FE/t. Release to let go: no fall damage for 2 s. "
                            "0.5 s cooldown. 1,000,000 FE.",
        "vantite_blade": "8.5 damage, 1.6 attacks/s, 3,000 durability. Use: dash 6 blocks forward for 12 durability; the first hit on a hostile "
                         "mob right after the dash deals +4 damage. 4 s cooldown.",
        "frontier_suit_helmet": "3 armor, 3 toughness. Night vision, 10 FE/t. Keeps every exo rig helmet function. 2,000,000 FE. Suit key: on/off.",
        "frontier_suit_chest": "8 armor, 3 toughness. Wind resistance: +0.3 knockback and +0.5 explosion knockback resistance, 2 FE/t. Keeps the "
                               "exo rig jump boost. 2,000,000 FE.",
        "frontier_suit_legs": "6 armor, 3 toughness. Step up 1.1 blocks (2 FE/t while walking), +20% speed while sprinting (10 FE/t). 2,000,000 FE.",
        "frontier_suit_boots": "3 armor, 3 toughness. Jet boots: hold jump in the air to climb (+0.1 per tick, up to 0.5 blocks/tick), 80 FE/t. "
                               "Cancels fall damage above 3 blocks for 50 FE per block. 2,000,000 FE.",
        "rail_rifle": "Hold use 0.5 to 1.5 s, then release: 20,000 FE per shot, 8 to 24 rail damage. Hitscan 96 blocks through up to 5 targets, "
                      "stops at blocks without harming them, knockback 1.5. 2 s cooldown. 3,000,000 FE.",
    }
    for k, v in pages.items():
        name(f"jei.autarkia.info.{k}", v)


def advancements():
    advancement("equipment/swing_line", "Swing Line", "Pull yourself to a grapple hook 40 or more blocks away", "grapple_launcher",
                parent="equipment/live_wire", criteria={"done": {"trigger": rl("equipment_event"), "conditions": {"event": "grapple_40"}}})


def generate():
    items()
    recipes()
    info()
    advancements()

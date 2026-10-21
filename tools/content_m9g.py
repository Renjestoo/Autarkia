"""M9 content data, gear part: the 7 Aegis items (07 section 1.7), the aegis equipment asset, the empty Aegis repair
tags (07 section 1.1, decision G13), recipes 44, 45 and 50 (07 section 5), JEI info pages, the enchant mode labels,
the enchant key messages and the Aegis HUD texts. The key name key.autarkia.enchant comes from content_base; the sounds
item.lance.charge and item.lance.fire come from sounds_spec."""

from datagen import generated_item, item_def, item_model, machine_recipe, name, rl, tag, write_json

GEO_ITEMS = (("aegis_multitool", "Aegis Multitool"), ("singularity_lance", "Singularity Lance"))
ARMOR = (("helmet", "Helmet", "minecraft:head_armor"), ("chest", "Chestplate", "minecraft:chest_armor"),
         ("legs", "Leggings", "minecraft:leg_armor"), ("boots", "Boots", "minecraft:foot_armor"))


def items():
    for iid, title in GEO_ITEMS:
        item_model(iid, {"parent": "minecraft:item/handheld", "textures": {"layer0": rl(f"item/{iid}")}})
        item_def(iid, obj={"model": {"type": "minecraft:special", "base": rl(f"item/{iid}"), "model": {"type": "geckolib:geckolib"}}})
        name(f"item.autarkia.{iid}", title)
    generated_item("aegis_blade", handheld=True)
    name("item.autarkia.aegis_blade", "Aegis Blade")
    for piece, title, atag in ARMOR:
        iid = f"aegis_{piece}"
        generated_item(iid)
        name(f"item.autarkia.{iid}", f"Aegis {title}")
        tag("item", atag, rl(iid))
    write_json("assets/autarkia/equipment/aegis.json", {"layers": {
        "humanoid": [{"texture": "autarkia:aegis"}], "humanoid_leggings": [{"texture": "autarkia:aegis"}]}})
    for t in ("minecraft:pickaxes", "minecraft:axes", "minecraft:shovels", "minecraft:hoes"):
        tag("item", t, rl("aegis_multitool"))
    tag("item", "minecraft:swords", rl("aegis_blade"))
    for tid in ("repairs_aegis", "aegis_tool_materials"):
        write_json(f"data/autarkia/tags/item/{tid}.json", {"replace": False, "values": []})


def recipes():
    fab = {
        "aegis_multitool": ([("nanite_multitool", 1), ("aegis_plate", 4), ("ai_core", 1), ("lattice_crystal", 4)], 10000000, 1200),
        "aegis_blade": ([("lattice_blade", 1), ("aegis_plate", 4), ("ai_core", 1)], 8000000, 1000),
        "singularity_lance": ([("rail_rifle", 1), ("disassembler_beam", 1), ("aegis_plate", 6), ("ai_core", 2), ("lattice_crystal", 8)],
                              20000000, 1600),
    }
    for iid, (inputs, fe, ticks) in fab.items():
        machine_recipe("fabricating", iid, inputs, [(iid, 1)], time=ticks, energy=-(-fe // ticks))


def info():
    suit = ("Unbreakable, 10,000,000 FE. Keeps every exo rig, frontier suit and lattice carapace {f} function. Within 64 blocks of a "
            "powered domain node (tether) the functions cost no FE and the piece charges at 50,000 FE/t; without it they use the "
            "piece's FE. Full set: the tether pays 50,000 FE per damage point. Suit key: on/off.")
    pages = {
        "aegis_multitool": "Pickaxe, axe, shovel, hoe, shears and sword-efficient, netherite level, speed 2,000: instant up to hardness 66. "
                           "14 damage, 1.2 attacks/s. 50 FE per block. Mode key: 1x1, 3x3 or 5x5 (full speed). Enchant key: none, "
                           "Silk Touch or Fortune III. Unbreakable, 100,000,000 FE, charges from the tether at 50,000 FE/t.",
        "aegis_blade": "30 damage, 2 attacks/s. 500 FE per hit: armor protects only a quarter as much against the hit. Use: Aegis "
                       "sweep, 20 damage to every hostile mob within 6 blocks, 50,000 FE, 5 s cooldown. Unbreakable, 100,000,000 FE.",
        "aegis_helmet": "5 armor, 5 toughness, 0.25 knockback resistance. " + suit.format(f="helmet"),
        "aegis_chest": "10 armor, 5 toughness, 0.25 knockback resistance. " + suit.format(f="chest"),
        "aegis_legs": "8 armor, 5 toughness, 0.25 knockback resistance. " + suit.format(f="leg"),
        "aegis_boots": "5 armor, 5 toughness, 0.25 knockback resistance. " + suit.format(f="boot"),
        "singularity_lance": "Hold use 3 s to charge, then a 128-block beam for 1 s: 80 beam damage to everything in its path (1.5) "
                             "and at its end (4.0), never you or your allies. 1,000,000 FE per shot, 10 s cooldown. Mode key: "
                             "terrain-safe or terrain-destroy (a crater of radius 3, needs mobGriefing; inside any domain the beam "
                             "never changes blocks). Unbreakable, 50,000,000 FE.",
    }
    for k, v in pages.items():
        name(f"jei.autarkia.info.{k}", v)


def lang():
    for mode, label in (("none", "No enchant"), ("silk", "Silk"), ("fortune", "Fortune")):
        name(f"enchant.autarkia.{mode}", label)
    name("message.autarkia.enchant", "Enchant: %s")
    name("message.autarkia.enchant.none", "This item has no enchant modes.")
    name("tooltip.autarkia.enchant_mode", "Enchant mode: %s")
    name("hud.autarkia.tether", "Tether")
    name("hud.autarkia.reform.ready", "Reform ready")
    name("hud.autarkia.reform.cooldown", "Reform %s s")


def generate():
    items()
    recipes()
    info()
    lang()

"""M8 content data, equipment part: the 7 lattice items (07 section 1.6), the lattice carapace equipment asset, the beam
damage type, the lattice repair tag, the disassembler immune block tag, recipes 37 to 43 (07 section 5), JEI
info pages, the ghost_matter advancement (07 section 7.3), the matter link messages and the language entries. The
sound event item.beam.loop comes from sounds_spec."""

from datagen import advancement, generated_item, item_def, item_model, machine_recipe, name, rl, tag, write_json

GEO_ITEMS = (("nanite_multitool", "Nanite Multitool"), ("disassembler_beam", "Disassembler Beam"))
CARAPACE = (("helmet", "Helmet", "minecraft:head_armor"), ("chest", "Chestplate", "minecraft:chest_armor"),
            ("legs", "Leggings", "minecraft:leg_armor"), ("boots", "Boots", "minecraft:foot_armor"))


def items():
    for iid, title in GEO_ITEMS:
        item_model(iid, {"parent": "minecraft:item/handheld", "textures": {"layer0": rl(f"item/{iid}")}})
        item_def(iid, obj={"model": {"type": "minecraft:special", "base": rl(f"item/{iid}"), "model": {"type": "geckolib:geckolib"}}})
        name(f"item.autarkia.{iid}", title)
    generated_item("lattice_blade", handheld=True)
    name("item.autarkia.lattice_blade", "Lattice Blade")
    for piece, title, atag in CARAPACE:
        iid = f"lattice_carapace_{piece}"
        generated_item(iid)
        name(f"item.autarkia.{iid}", f"Lattice Carapace {title}")
        tag("item", atag, rl(iid))
    write_json("assets/autarkia/equipment/lattice_carapace.json", {"layers": {
        "humanoid": [{"texture": "autarkia:lattice_carapace"}], "humanoid_leggings": [{"texture": "autarkia:lattice_carapace"}]}})
    for t in ("minecraft:pickaxes", "minecraft:axes", "minecraft:shovels", "minecraft:hoes"):
        tag("item", t, rl("nanite_multitool"))
    tag("item", "minecraft:swords", rl("lattice_blade"))
    tag("item", "autarkia:repairs_lattice", rl("lattice_alloy_ingot"))
    write_json("data/autarkia/tags/block/disassembler_immune.json", {"replace": False, "values": [
        {"id": rl("bastion_block"), "required": False}, {"id": rl("bastion_glass"), "required": False}]})
    write_json("data/autarkia/damage_type/beam.json", {"message_id": "autarkia.beam", "exhaustion": 0.1, "scaling": "when_caused_by_living_non_player"})
    name("death.attack.autarkia.beam", "%1$s was taken apart by a beam")
    name("death.attack.autarkia.beam.player", "%1$s was taken apart by %2$s")
    name("message.autarkia.matter_link", "Linked to the matter buffer at %s, %s, %s")
    name("message.autarkia.matter_link.unformed", "This matter buffer is not complete")
    name("tooltip.autarkia.matter_link", "Matter link: %s, %s, %s (%s)")
    name("tooltip.autarkia.matter_unlinked", "Matter link: none (sneak + use on a matter buffer)")
    name("tooltip.autarkia.matter_cache", "Matter cache: %s / %s MU")


def recipes():
    fab = {
        "nanite_multitool": ([("vantite_excavator", 1), ("lattice_alloy_ingot", 4), ("nanite_paste", 4), ("ai_core", 1), ("lattice_crystal", 2),
                              ("coolant_cell", 1)], 1000000, 800),
        "lattice_blade": ([("vantite_blade", 1), ("lattice_alloy_ingot", 4), ("lattice_crystal", 2), ("nanite_paste", 2)], 800000, 600),
        "lattice_carapace_helmet": ([("frontier_suit_helmet", 1), ("lattice_alloy_ingot", 5), ("nanite_paste", 2), ("lattice_crystal", 1),
                                     ("coolant_cell", 1)], 800000, 600),
        "lattice_carapace_chest": ([("frontier_suit_chest", 1), ("lattice_alloy_ingot", 8), ("nanite_paste", 4), ("lattice_crystal", 2),
                                    ("coolant_cell", 1)], 1200000, 800),
        "lattice_carapace_legs": ([("frontier_suit_legs", 1), ("lattice_alloy_ingot", 7), ("nanite_paste", 3), ("lattice_crystal", 2),
                                   ("coolant_cell", 1)], 1000000, 700),
        "lattice_carapace_boots": ([("frontier_suit_boots", 1), ("lattice_alloy_ingot", 4), ("nanite_paste", 2), ("lattice_crystal", 1),
                                    ("coolant_cell", 1)], 800000, 600),
        "disassembler_beam": ([("lattice_alloy_ingot", 6), ("nanite_paste", 4), ("ai_core", 1), ("lattice_crystal", 4), ("coolant_cell", 2)],
                              2000000, 1000),
    }
    for iid, (inputs, fe, ticks) in fab.items():
        machine_recipe("fabricating", iid, inputs, [(iid, 1)], time=ticks, energy=-(-fe // ticks))


def info():
    pages = {
        "nanite_multitool": "Pickaxe, axe, shovel, hoe, shears and sword-efficient, netherite level, speed 24, 9 damage. 10,000,000 FE, 100 FE per "
                            "block. Mode key: 1x1 or 3x3 (x0.5 speed). Use: strip or path, sneak + use: till. Sneak + use on a matter buffer "
                            "links it: drops with a matter value become MU in the tool (max 4,096 MU, 20 FE per item); within 32 blocks of the "
                            "buffer the cache streams into it at 64 MU/s. Full cache or no value: drops fall. Empty: speed 1, no drops.",
        "lattice_blade": "12 damage, 1.6 attacks/s. 200 FE per hit: armor protects only half as much against the hit. At 0 FE: normal hits. "
                         "5,000,000 FE.",
        "lattice_carapace_helmet": "4 armor, 4 toughness, 0.15 knockback resistance. Keeps every exo rig and frontier suit helmet function. "
                                   "Self-repair: 1 durability per second for 2,000 FE. 5,000,000 FE. Suit key: on/off.",
        "lattice_carapace_chest": "9 armor, 4 toughness, 0.15 knockback resistance. Keeps the exo rig and frontier suit chest functions. "
                                  "Self-repair: 1 durability per second for 2,000 FE. 5,000,000 FE.",
        "lattice_carapace_legs": "7 armor, 4 toughness, 0.15 knockback resistance. Keeps the exo rig and frontier suit leg functions. "
                                 "Self-repair: 1 durability per second for 2,000 FE. 5,000,000 FE.",
        "lattice_carapace_boots": "4 armor, 4 toughness, 0.15 knockback resistance. Jet boots and fall protection of the frontier suit. "
                                  "Self-repair: 1 durability per second for 2,000 FE. 5,000,000 FE.",
        "disassembler_beam": "Hold use: a 24-block beam for up to 5 s, 2,000 FE/t. Takes apart 2 blocks per tick up to blast resistance 50; "
                             "never block entities, bastion blocks or other colonies' domains. 6 beam damage to the first mob, once per 0.5 s. "
                             "Matter link like the nanite multitool. Overheat: 3 s cooldown. 20,000,000 FE.",
    }
    for k, v in pages.items():
        name(f"jei.autarkia.info.{k}", v)


def advancements():
    advancement("equipment/ghost_matter", "Ghost in the Matter", "Fill a tool's matter cache to 4,096 MU", "nanite_multitool",
                parent="equipment/swing_line", criteria={"done": {"trigger": rl("equipment_event"), "conditions": {"event": "matter_cache_full"}}})


def generate():
    items()
    recipes()
    info()
    advancements()

"""M9 content data, Aegis rule part (07 sections 3 and 7): bastion_block, bastion_glass and forge_of_ascension (models,
blockstates, items, loot, tags, recipes G14 and 07 section 3.5), the 4 ascension_fitting recipes 46 to 49, the empty
damage type tag autarkia:aegis_bypass, the Player Animation Library file of the two ascension poses, the 4 equipment
advancements of 07 section 7.3 (G12 parents), JEI pages, Jade and screen texts, and the language entries. The Aegis
items, aegis_plate and their textures come with the gear and materials parts."""

from datagen import (advancement, block_item, cube_all, generated_item, loot_center_only, loot_self, name, particle_model, rl, shaped,
                     simple_blockstate, tag, write_json)

PICK = "minecraft:mineable/pickaxe"
DIAMOND_TOOL = "minecraft:needs_diamond_tool"
FITTINGS = (("aegis_helmet", "head", "lattice_carapace_helmet", 5, 1, 4, 5), ("aegis_chest", "chest", "lattice_carapace_chest", 8, 1, 8, 4),
            ("aegis_legs", "legs", "lattice_carapace_legs", 7, 1, 6, 3), ("aegis_boots", "feet", "lattice_carapace_boots", 4, 1, 4, 2))


def blocks():
    for bid, title in (("bastion_block", "Bastion Block"), ("bastion_glass", "Bastion Glass")):
        cube_all(bid)
        simple_blockstate(bid)
        block_item(bid)
        loot_self(bid)
        tag("block", PICK, rl(bid))
        tag("block", DIAMOND_TOOL, rl(bid))
        name(f"block.autarkia.{bid}", title)
    particle_model("forge_of_ascension")
    simple_blockstate("forge_of_ascension")
    generated_item("forge_of_ascension")
    loot_center_only("forge_of_ascension")
    tag("block", PICK, rl("forge_of_ascension"))
    tag("block", DIAMOND_TOOL, rl("forge_of_ascension"))
    name("block.autarkia.forge_of_ascension", "Forge of Ascension")
    write_json("data/autarkia/tags/damage_type/aegis_bypass.json", {"replace": False, "values": []})


def recipes():
    shaped("bastion_block", ["LLL", "LAL", "LLL"], {"L": "lattice_block", "A": "aegis_plate"}, 8)
    shaped("bastion_glass", ["GGG", "GAG", "GGG"], {"G": "minecraft:glass", "A": "aegis_plate"}, 8)
    shaped("forge_of_ascension", ["PAP", "LXL", "FFF"], {"P": "aegis_plate", "A": "ai_core", "L": "lattice_alloy_block", "X": "worker_assembler",
                                                         "F": "foundation"})
    for result, piece, worn, plates, cores, paste, step in FITTINGS:
        write_json(f"data/autarkia/recipe/ascension_fitting/{result}.json", {
            "type": rl("ascension_fitting"), "piece": piece, "worn": rl(worn),
            "inputs": [{"ingredient": rl("aegis_plate"), "count": plates}, {"ingredient": rl("ai_core"), "count": cores},
                       {"ingredient": rl("nanite_paste"), "count": paste}],
            "energy": 12500000, "step": step, "result": rl(result)})


def poses():
    """Write the two ascension poses (07 section 3.5) as one Blockbench Bedrock file for the Player Animation Library."""
    def key(frames):
        return {f"{t:.2f}": list(v) for t, v in frames}

    fit = {
        "head": {"rotation": key([(0.0, (0, 0, 0)), (0.5, (-20, 0, 0)), (1.0, (-15, 0, 0))])},
        "body": {"rotation": key([(0.0, (0, 0, 0)), (1.0, (-4, 0, 0))])},
        "right_arm": {"rotation": key([(0.0, (0, 0, 0)), (0.6, (-25, 0, 35)), (1.0, (-20, 0, 40))])},
        "left_arm": {"rotation": key([(0.0, (0, 0, 0)), (0.6, (-25, 0, -35)), (1.0, (-20, 0, -40))])},
        "right_leg": {"rotation": key([(0.0, (0, 0, 0)), (1.0, (0, 0, 8))])},
        "left_leg": {"rotation": key([(0.0, (0, 0, 0)), (1.0, (0, 0, -8))])},
    }
    rise = {
        "head": {"rotation": key([(0.0, (-15, 0, 0)), (1.5, (-35, 0, 0)), (4.0, (0, 0, 0))])},
        "body": {"rotation": key([(0.0, (-4, 0, 0)), (1.5, (-10, 0, 0)), (4.0, (0, 0, 0))]),
                 "position": key([(0.0, (0, 0, 0)), (1.5, (0, 2, 0)), (3.0, (0, 2, 0)), (4.0, (0, 0, 0))])},
        "right_arm": {"rotation": key([(0.0, (-20, 0, 40)), (1.5, (-170, 0, 15)), (3.0, (-170, 0, 15)), (4.0, (0, 0, 0))])},
        "left_arm": {"rotation": key([(0.0, (-20, 0, -40)), (1.5, (-170, 0, -15)), (3.0, (-170, 0, -15)), (4.0, (0, 0, 0))])},
        "right_leg": {"rotation": key([(0.0, (0, 0, 8)), (1.5, (0, 0, 4)), (4.0, (0, 0, 0))])},
        "left_leg": {"rotation": key([(0.0, (0, 0, -8)), (1.5, (0, 0, -4)), (4.0, (0, 0, 0))])},
    }
    write_json("assets/autarkia/player_animations/ascension.json", {"format_version": "1.8.0", "animations": {
        "ascension_fit": {"loop": "hold_on_last_frame", "animation_length": 1.0, "bones": fit},
        "ascension_rise": {"loop": False, "animation_length": 4.0, "bones": rise}}})


def advancements():
    def event(e):
        return {"done": {"trigger": rl("equipment_event"), "conditions": {"event": e}}}

    advancement("equipment/ascended", "Ascended", "Be fitted with the full Aegis set in the Forge of Ascension", "aegis_chest",
                parent="equipment/ghost_matter", criteria=event("ascension_complete"), frame="challenge")
    advancement("equipment/paid_in_full", "Paid in Full", "Let the Aegis tether pay for 1,000 points of damage", "aegis_helmet",
                parent="equipment/ascended", criteria=event("tether_1000"), frame="goal")
    advancement("equipment/not_today", "Not Today", "Survive a lethal hit through the Nanite Reform", "aegis_boots",
                parent="equipment/ascended", criteria=event("reform"), frame="goal")
    advancement("equipment/unbroken", "Unbroken Walls", "Watch a Wither fail against powered Bastion walls", "bastion_block",
                parent="equipment/ascended", criteria=event("bastion_wither"), frame="challenge")


def lang():
    msg = {"not_formed": "The Forge of Ascension is not complete", "busy": "A fitting is already running",
           "pad": "Stand on the pad in the middle of the Forge", "nothing": "Nothing to fit: wear lattice carapace pieces and load their inputs",
           "grid": "The Forge needs %s FE on its grid (%s FE now)", "abort.left": "Fitting aborted: you left the pad",
           "abort.logout": "Fitting aborted: logged out", "abort.power": "Fitting aborted: the grid cannot pay",
           "abort.inputs": "Fitting aborted: the inputs are missing", "abort.recipe": "Fitting aborted: the fitting recipe is gone",
           "abort.broken": "Fitting aborted: the Forge was broken"}
    for k, v in msg.items():
        name(f"message.autarkia.forge.{k}", v)
    jade = {"forge.idle": "Idle", "forge.fitting": "Fitting", "forge.done": "Fitting complete", "forge.aborted": "Fitting aborted",
            "forge.step.lock": "Lock", "forge.step.scan": "Scan", "forge.step.2": "Step 2: boots", "forge.step.3": "Step 3: legs",
            "forge.step.4": "Step 4: chest", "forge.step.5": "Step 5: helmet", "forge.step.seal": "Seal", "forge.progress": "%s, %s%%",
            "forge.fe": "FE per tick: %s", "forge.fitted": "Pieces fitted: %s", "bastion.protected": "Protected (grid %s FE)",
            "bastion.unprotected": "Unprotected"}
    for k, v in jade.items():
        name(f"jade.autarkia.{k}", v)
    name("config.jade.plugin_autarkia.equipment", "Autarkia Forge of Ascension")
    name("config.jade.plugin_autarkia.bastion", "Autarkia Bastion blocks")
    name("screen.autarkia.forge.hint", "Sneak + use the pad with empty hands")
    name("screen.autarkia.forge.grid", "Grid: %s kFE")
    name("jei.autarkia.category.ascension_fitting", "Ascension Fitting")
    name("jei.autarkia.ascension.worn", "Worn by the player in this slot")
    name("jei.autarkia.ascension.step", "Step %s, %s FE")
    info = {
        "bastion_block": "Indestructible while its own colony's T6 grid runs a Singularity Core and stores at least 1,000,000 FE: no break progress "
                         "for survival players, blast resistance 3,600,000, Withers and the Ender Dragon cannot break it, pistons cannot "
                         "move it. Otherwise it is like obsidian (hardness 50, resistance 1,200, diamond pickaxe).",
        "bastion_glass": "Glass variant of the Bastion block with the same protection rules.",
        "forge_of_ascension": "3 x 4 x 3 chamber. Load the Aegis inputs, wear lattice carapace pieces, stand on the pad and sneak + use it with "
                              "empty hands. 30 s fitting: boots, legs, chest, helmet, 12,500,000 FE each from the Forge's grid (50,000,000 FE "
                              "needed at the start). You cannot move or take damage while fitted. Leaving the pad or a grid that cannot pay "
                              "aborts: fitted pieces stay, the current carapace piece comes back.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def generate():
    blocks()
    recipes()
    poses()
    advancements()
    lang()

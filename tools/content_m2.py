"""M2 content data: treadwheel, windmill, stone flywheel, bronze equipment, sling, damage type, power advancements."""

from datagen import (advancement, generated_item, loot_center_only, name, particle_model, rl, shaped, simple_blockstate, tag,
                     write_json)

AXE = "minecraft:mineable/axe"
PICK = "minecraft:mineable/pickaxe"


def blocks():
    for bid, title, tool in [("treadwheel", "Treadwheel", AXE), ("windmill", "Windmill", AXE), ("stone_flywheel", "Stone Flywheel", PICK)]:
        particle_model(bid)
        simple_blockstate(bid)
        generated_item(bid)
        loot_center_only(bid)
        tag("block", tool, rl(bid))
        name(f"block.autarkia.{bid}", title)
    shaped("treadwheel", ["LPL", "PGP", "LPL"], {"L": "#minecraft:logs", "P": "#minecraft:planks", "G": "bronze_gear"})
    shaped("windmill", ["WWW", "WGW", "LBL"], {"W": "#minecraft:wool", "G": "bronze_gear", "L": "#minecraft:logs", "B": "bronze_ingot"})
    shaped("stone_flywheel", ["SSS", "SGS", "SBS"], {"S": "minecraft:smooth_stone", "G": "bronze_gear", "B": "bronze_block"})


def equipment():
    tools = [("bronze_pickaxe", "Bronze Pickaxe", ["BBB", " s ", " s "], "minecraft:pickaxes"),
             ("bronze_axe", "Bronze Axe", ["BB", "Bs", " s"], "minecraft:axes"),
             ("bronze_shovel", "Bronze Shovel", ["B", "s", "s"], "minecraft:shovels"),
             ("bronze_hoe", "Bronze Hoe", ["BB", " s", " s"], "minecraft:hoes"),
             ("bronze_sword", "Bronze Sword", ["B", "B", "s"], "minecraft:swords")]
    for iid, title, pattern, ttag in tools:
        generated_item(iid, handheld=True)
        name(f"item.autarkia.{iid}", title)
        shaped(iid, pattern, {"B": "#c:ingots/bronze", "s": "minecraft:stick"}, category="equipment")
        tag("item", ttag, rl(iid))
    armor = [("bronze_helmet", "Bronze Helmet", ["BBB", "B B"], "minecraft:head_armor"),
             ("bronze_chest", "Bronze Chestplate", ["B B", "BBB", "BBB"], "minecraft:chest_armor"),
             ("bronze_legs", "Bronze Leggings", ["BBB", "B B", "B B"], "minecraft:leg_armor"),
             ("bronze_boots", "Bronze Boots", ["B B", "B B"], "minecraft:foot_armor")]
    for iid, title, pattern, atag in armor:
        generated_item(iid)
        name(f"item.autarkia.{iid}", title)
        shaped(iid, pattern, {"B": "#c:ingots/bronze"}, category="equipment")
        tag("item", atag, rl(iid))
    write_json("assets/autarkia/equipment/bronze.json", {"layers": {
        "humanoid": [{"texture": "autarkia:bronze"}], "humanoid_leggings": [{"texture": "autarkia:bronze"}]}})
    generated_item("bronze_sling", handheld=True)
    name("item.autarkia.bronze_sling", "Bronze Sling")
    shaped("bronze_sling", ["s s", "TLT", " s "], {"s": "minecraft:stick", "T": "minecraft:string", "L": "minecraft:leather"},
           category="equipment")
    tag("item", "minecraft:enchantable/durability", rl("bronze_sling"))
    tag("item", "autarkia:bronze_tool_materials", rl("bronze_ingot"))
    tag("item", "autarkia:repairs_bronze_armor", rl("bronze_ingot"))
    tag("item", "autarkia:sling_ammo", "minecraft:cobblestone", "#c:nuggets")
    name("entity.autarkia.sling_shot", "Sling Shot")
    write_json("data/autarkia/damage_type/sling.json", {"message_id": "autarkia.sling", "exhaustion": 0.1,
                                                         "scaling": "when_caused_by_living_non_player"})
    name("death.attack.autarkia.sling", "%1$s was hit by a sling stone")
    name("death.attack.autarkia.sling.player", "%1$s was slung by %2$s")
    for m, label in [("single", "1x1"), ("area3", "3x3"), ("area5", "5x5"), ("vein", "Vein"), ("terrain_safe", "Terrain safe"),
                     ("terrain_destroy", "Terrain destroy")]:
        name(f"mode.autarkia.{m}", label)
    name("message.autarkia.mode", "Mode: %s")
    name("message.autarkia.mode.none", "This item has no modes.")


def power():
    name("entity.autarkia.snapped_cable", "Snapped Cable")
    name("entity.autarkia.rolling_flywheel", "Rolling Flywheel")
    name("message.autarkia.treadwheel.refused", "Lead an animal or villager to the wheel (one walker at a time)")
    for k, v in {"wind": "Wind: %s%%", "wind_none": "Wind data: none", "blocked": "Blocked: %s", "spin": "Spin: %s%%",
                 "stored": "Stored: %s / %s FE (%s%%)", "io": "I/O: in %s / out %s FE/t", "energy_fe": "Energy: %s FE"}.items():
        name(f"jade.autarkia.{k}", v)
    advancement("power/root", "Power to the Colony", "Make your first FE with a hand crank", "hand_crank",
                background="minecraft:gui/advancements/backgrounds/stone")
    advancement("power/first_span", "Tied Together", "Place your first cable span", "rope_coil", parent="power/root")
    advancement("power/rope_snap", "Too Much Pull", "Watch a rope span snap from wear", "rope_coil", parent="power/first_span")
    advancement("power/rolling_wheel", "Runaway Wheel", "Release a spinning stone flywheel", "stone_flywheel", parent="power/root")
    info = {
        "treadwheel": "Generator. Use a lead on it while leading a horse, donkey, mule or llama (20 FE/t), a villager (16) or a cow, pig, sheep or goat (12). Sneak + use with an empty hand takes the walker back. A Porter gives 24 FE/t.",
        "windmill": "Generator, 4 blocks tall. Output 4 to 40 FE/t: higher rotors, stronger wind and rain or thunder give more. Every solid block in the 5 x 5 area in front of the rotor costs 10%; 5 or more stop it.",
        "stone_flywheel": "Energy storage with inertia: 40,000 FE, in/out 40 x spin. Needs a solid block under its middle. Break it while it holds 2,000 FE or more and the wheel rolls away and hits what is in its path.",
        "bronze_pickaxe": "Mines what an iron pickaxe mines. Speed 6.5, 300 uses.", "bronze_axe": "Strips logs, disables shields for 5 s. 9 damage.",
        "bronze_shovel": "Makes paths. Speed 6.5, 300 uses.", "bronze_hoe": "Tills soil. Speed 6.5, 300 uses.",
        "bronze_sword": "6 damage, 1.6 attacks per second, sweep attack.", "bronze_helmet": "2 armor.", "bronze_chest": "5 armor.",
        "bronze_legs": "4 armor.", "bronze_boots": "2 armor.",
        "bronze_sling": "Hold use 5 to 20 ticks, then release. Throws cobblestone (3 damage) or any nugget (4 damage). 1 durability per shot, 0.5 s cooldown.",
    }
    for k, v in info.items():
        name(f"jei.autarkia.info.{k}", v)


def generate():
    blocks()
    equipment()
    power()

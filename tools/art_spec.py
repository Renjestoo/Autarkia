"""Single source of truth for every Autarkia asset that tools/textures.py, tools/geo.py and tools/data.py produce.

Coordinates and conventions
- Block textures: 16x16 RGBA, path assets/autarkia/textures/block/<name>.png.
- Item sprites: 16x16 RGBA, path assets/autarkia/textures/item/<name>.png.
- GeckoLib models: assets/autarkia/geckolib/models/<group>/<id>.geo.json, animations
  assets/autarkia/geckolib/animations/<group>/<id>.animation.json, texture
  assets/autarkia/textures/<group>/<id>.png (group = block, entity, item, armor).
- Geo units are pixels (16 per block). Block model origin = bottom centre of the WHOLE footprint
  (the horizontal centre of the footprint box, y = 0 at its floor). Java shifts the model to the
  footprint, so models are symmetric around their own origin box. The front of every directional
  model faces -Z (north). +Y is up. Entity and rig origin = between the feet.
- Big-block footprints are given as block offsets (min, max) from the CENTER block for facing north.
  The geo model must fill exactly that footprint box (it may overhang only where noted).
- Animation names are "animation.<id>.<name>" for blocks/items/armor/entities and
  "animation.<rig>.<name>" for the three worker rigs.
- Palettes: one per material, 5 shades dark->light plus accents. Every texture uses only its palette
  ramps plus pure black/white highlights, so each tier reads as one family.
"""

PALETTES = {
    "wood": ["#3B2614", "#5A3B22", "#7A5230", "#9C6B3F", "#C08A55"],
    "dark_wood": ["#2A1B0E", "#3B2614", "#5A3B22", "#7A5230", "#9C6B3F"],
    "bronze": ["#5A3A1A", "#8C5A2B", "#B87333", "#D9A066", "#F2D3A0"],
    "tin": ["#5E666C", "#838C93", "#A9B2B8", "#C9D3DA", "#ECF1F4"],
    "copper": ["#5C2A16", "#8E4424", "#C66A3D", "#E08A5A", "#F5B98F"],
    "verdigris": ["#1F4A40", "#2E6B5C", "#4FA38A", "#7FC9AF", "#B8E8D6"],
    "zinc": ["#4A5A57", "#6E8480", "#9FB7B0", "#C2D4CF", "#E3EEEB"],
    "brass": ["#5C4410", "#8A6A1E", "#B8902E", "#D9B552", "#F2DC8C"],
    "steel": ["#2E3338", "#4F575E", "#7A848C", "#A7B0B7", "#D5DCE0"],
    "iron": ["#3D3D3D", "#5E5E5E", "#8A8A8A", "#B8B8B8", "#E2E2E2"],
    "gold": ["#6B4A08", "#A6780F", "#D9A51E", "#FCD34B", "#FFF1A8"],
    "electric": ["#2E3338", "#4F575E", "#7A848C", "#A7B0B7", "#D5DCE0"],
    "electric_accent": ["#1A5A8C", "#2A7FC4", "#3FA9F5", "#6CC8FF", "#9BE7FF"],
    "hazard": ["#1A1A1A", "#2B2B2B", "#C99A12", "#F2C230", "#FFE27A"],
    "vantite": ["#1B2A3A", "#2C4A63", "#3E7A8C", "#5FB8B0", "#A8F0DC"],
    "vantite_steel": ["#1E2A33", "#33495A", "#4F6E80", "#7DA3B3", "#B8D9E3"],
    "lattice": ["#2A1F3D", "#4B3A75", "#7A63B8", "#B39DFF", "#EDE6FF"],
    "aegis": ["#1A1A1A", "#3C3C3C", "#C9A227", "#F5D76E", "#FFFFFF"],
    "aegis_glow": ["#0F5C55", "#1FA597", "#3FE0CF", "#7FFFF0", "#D8FFFA"],
    "stone": ["#4A4A4A", "#626262", "#7A7A7A", "#919191", "#ABABAB"],
    "deepslate": ["#262629", "#36363A", "#47474C", "#5B5B60", "#727277"],
    "brick": ["#4A2018", "#6E2E22", "#96432F", "#B5604A", "#CE8670"],
    "foundation": ["#3A3D40", "#55595D", "#6F7479", "#8C9196", "#AAB0B5"],
    "slag": ["#2B2622", "#423A33", "#5C5047", "#776A5E", "#93857A"],
    "rubber": ["#0E0E0E", "#1E1E1E", "#2C2C2C", "#3D3D3D", "#505050"],
    "glass": ["#3A5560", "#557C8A", "#7FA8B5", "#B5D6E0", "#E8F6FA"],
    "frost": ["#5A7F99", "#7FA6C2", "#A8CCE3", "#CFE7F5", "#F2FAFF"],
    "magma": ["#3A0E05", "#7A1E08", "#C2410C", "#F97316", "#FDBA74"],
    "core_root": ["#2E1F10", "#4A3219", "#7A5230", "#A0703F", "#C9A06A"],
    "nanite": ["#3D454D", "#5C6670", "#8A939C", "#C9D1D9", "#F0F4F7"],
    "canvas": ["#6E6656", "#958B76", "#BDB49C", "#DCD5C0", "#F4F0E4"],
    "ceramic": ["#5F6B73", "#8A979F", "#B8C3C9", "#DCE3E7", "#F7FAFB"],
    "leather": ["#3A2214", "#5C3720", "#7E4E2E", "#A06A42", "#C28D62"],
    "red_paint": ["#4A0E0E", "#7A1A16", "#A82A20", "#D0463A", "#EE7A6A"],
    "water": ["#1C3F73", "#2A5C9E", "#3F7FC4", "#6FA8E0", "#B5D8F5"],
    "rig_gray": ["#3C3C3C", "#646464", "#8C8C8C", "#B4B4B4", "#DCDCDC"],
    "aegis_body": ["#0F0F0F", "#1A1A1A", "#2B2B2B", "#3C3C3C", "#C9A227"],
    "aegis_gold": ["#5E4A10", "#8F7318", "#C9A227", "#F5D76E", "#FFFFFF"],
    "pale_wood": ["#5A3B22", "#7A5230", "#9C6B3F", "#C08A55", "#DDB07A"],
}

TIER_CASING = {1: "bronze", 2: "brass", 3: "steel", 4: "vantite_steel", 5: "lattice", 6: "aegis"}
TIER_LAMP = {1: "#FFB347", 2: "#FFD27F", 3: "#4FE3FF", 4: "#7CFF6B", 5: "#FF5CF0", 6: "#7FFFF0"}

METAL_COLORS = {
    "iron": "iron", "copper": "copper", "gold": "gold", "tin": "tin", "zinc": "zinc", "vantite": "vantite",
}

# ---------------------------------------------------------------------------------------------
# Block textures for blocks that use plain JSON models (cube or simple elements).
# key = texture name (textures/block/<key>.png), value = (palette, style, description)
# Styles are painter names in tools/paint.py. The painter must produce a finished, detailed
# 16x16 texture (no flat fills): bevels, rivets, grain, noise, highlights.
# ---------------------------------------------------------------------------------------------
BLOCK_TEXTURES = {
    "tin_ore": ("tin", "ore_stone", "Stone with pale silver-grey tin flecks"),
    "deepslate_tin_ore": ("tin", "ore_deepslate", "Deepslate with tin flecks"),
    "zinc_ore": ("zinc", "ore_stone", "Stone with blue-grey zinc crystals"),
    "deepslate_zinc_ore": ("zinc", "ore_deepslate", "Deepslate with zinc crystals"),
    "vantite_ore": ("vantite", "ore_deepslate_glow", "Deepslate with glowing teal vantite veins"),
    "raw_tin_block": ("tin", "raw_block", "Chunky raw tin lumps"),
    "raw_zinc_block": ("zinc", "raw_block", "Chunky raw zinc lumps"),
    "raw_vantite_block": ("vantite", "raw_block", "Chunky raw vantite lumps"),
    "tin_block": ("tin", "metal_block", "Polished tin block, beveled tiles"),
    "bronze_block": ("bronze", "metal_block", "Bronze block, riveted plates"),
    "zinc_block": ("zinc", "metal_block", "Zinc block"),
    "brass_block": ("brass", "metal_block", "Brass block with engraved border"),
    "steel_block": ("steel", "metal_block_rivet", "Steel plate block with rivets"),
    "vantite_block": ("vantite", "metal_block", "Vantite block with faint glow seams"),
    "vantite_steel_block": ("vantite_steel", "metal_block_rivet", "Blue-grey alloy plates"),
    "lattice_alloy_block": ("lattice", "metal_block_hex", "Violet alloy with hex lattice inlay"),
    "lattice_block": ("lattice", "lattice_building", "Building block: violet hex lattice, dark mortar"),
    "foundation": ("foundation", "foundation", "Heavy grey concrete slab with steel edge strips"),
    "lattice_cluster": ("lattice", "crystal_cross", "Violet crystal cluster sprite (cross model)"),
    "lattice_geode_shell": ("lattice", "geode_shell", "Dark violet shell with crystal speckles"),
    "lattice_geode_shell_charged": ("lattice", "geode_shell_charged", "Shell with bright glowing veins"),
    "magma_vent": ("magma", "magma_vent", "Cracked basalt with glowing magma core"),
    "ore_outcrop": ("stone", "outcrop_base", "Small rock pile base (tinted per ore by overlay)"),
    "ore_outcrop_iron": ("iron", "outcrop_specks", "Overlay specks"),
    "ore_outcrop_copper": ("copper", "outcrop_specks", "Overlay specks"),
    "ore_outcrop_gold": ("gold", "outcrop_specks", "Overlay specks"),
    "ore_outcrop_tin": ("tin", "outcrop_specks", "Overlay specks"),
    "ore_outcrop_zinc": ("zinc", "outcrop_specks", "Overlay specks"),
    "ore_outcrop_vantite": ("vantite", "outcrop_specks", "Overlay specks"),
    "bastion_block": ("aegis", "bastion", "Black and gold armoured block, glow seams"),
    "bastion_glass": ("aegis", "bastion_glass", "Gold-framed glass, mostly transparent"),
    "catwalk": ("steel", "grate", "Steel grate, see-through holes (cutout)"),
    "hazard_block": ("hazard", "hazard_stripes", "Diagonal yellow and black stripes, worn"),
    "industrial_lamp": ("steel", "lamp_off", "Caged lamp, glass dark"),
    "industrial_lamp_on": ("steel", "lamp_on", "Caged lamp, glass bright warm white"),
    "prop_beam": ("wood", "beam", "Rough timber beam with iron bands"),
    "claim_marker": ("lattice", "marker", "Small crystal on a steel stake"),
    "crate_side": ("wood", "crate_side", "Wooden crate side, planks, bronze corner brackets"),
    "crate_top": ("wood", "crate_top", "Crate lid with bronze straps"),
    "tally_board_front": ("wood", "board_front", "Chalk board with wood frame"),
    "tally_board_side": ("wood", "plank", "Planks"),
    "chute": ("wood", "chute", "Polished wooden slide"),
    "project_board_front": ("wood", "cork_board", "Cork board with pinned paper cards"),
    "project_board_side": ("wood", "plank", "Planks"),
    "winding_post": ("wood", "winding_post", "Wooden post with bronze ratchet"),
    "rope_pulley": ("wood", "pulley_wheel", "Wooden sheave with bronze hub"),
    "rope_pulley_side": ("bronze", "pulley_side", "Bronze bracket"),
    "crucible_side": ("brick", "crucible_side", "Clay brick pot"),
    "crucible_top": ("brick", "crucible_top", "Pot rim, dark inside"),
    "crucible_molten": ("bronze", "molten", "Glowing molten metal surface"),
    "quern_side": ("stone", "quern_side", "Two stacked millstones"),
    "quern_top": ("stone", "quern_top", "Millstone top with handle hole"),
    "pressure_valve": ("brass", "valve", "Brass valve with red wheel"),
    "lightning_arrester": ("copper", "arrester", "Copper spike on ceramic"),
    "capacitor_bank_front": ("brass", "capacitor_front", "Brass casing, 4 charge stripes (dark)"),
    "capacitor_bank_side": ("brass", "panel", "Brass panel with vents"),
    "capacitor_bank_stripe": ("electric_accent", "charge_stripe", "Glowing charge stripe"),
    "plate_press_front": ("brass", "press_front", "Brass press with steel ram"),
    "plate_press_side": ("brass", "panel", "Brass panel"),
    "plate_press_top": ("steel", "panel_top", "Top hatch"),
    "wire_drawer_front": ("brass", "drawer_front", "Brass with spinning die and wire spool"),
    "wire_drawer_side": ("brass", "panel", "Brass panel"),
    "belt_top": ("rubber", "belt", "Rubber belt with ribs (scrolls)"),
    "belt_side": ("steel", "belt_side", "Steel side rails and rollers"),
    "belt_splitter_top": ("rubber", "splitter_top", "Belt with diverter arrow"),
    "sorting_chute_top": ("wood", "sorting_top", "Chute with gate"),
    "wood_rack": ("wood", "rack_frame", "Wooden rack timber"),
    "steel_rack": ("steel", "rack_frame", "Steel rack beams, orange paint chips"),
    "lattice_rack": ("lattice", "rack_frame", "Violet alloy rack"),
    "rack_tag": ("hazard", "red_tag", "Red warning tag"),
    "pattern_loom_front": ("brass", "loom_front", "Brass loom with punch-card reader"),
    "pattern_loom_side": ("wood", "plank", "Planks"),
    "pattern_loom_top": ("brass", "loom_top", "Card slot and glass"),
    "wall_insulator": ("steel", "insulator", "Ceramic insulator on bracket"),
    "transformer_side": ("steel", "transformer_side", "Grey cooling fins"),
    "transformer_top": ("steel", "transformer_top", "Bushings"),
    "fuse_box_front": ("steel", "fuse_front", "Grey box with big lever and fuse window"),
    "fuse_box_side": ("steel", "panel", "Steel panel"),
    "battery_bank_front": ("steel", "battery_front", "Steel cabinet with gauge and cells"),
    "battery_bank_side": ("steel", "panel_vent", "Vents"),
    "magnetic_separator_front": ("steel", "separator_front", "Drum with copper coils"),
    "magnetic_separator_side": ("steel", "panel", "Steel panel"),
    "electrolyzer_front": ("steel", "electrolyzer_front", "Glass tank with bubbles and electrodes"),
    "electrolyzer_side": ("steel", "panel", "Steel panel"),
    "warehouse_controller_front": ("steel", "controller_front", "Screen with fill bar and lamps"),
    "warehouse_controller_side": ("steel", "panel", "Steel panel"),
    "ledger_terminal_front": ("steel", "terminal_front", "Keyboard terminal with green screen"),
    "ledger_terminal_side": ("steel", "panel", "Steel panel"),
    "capsule_tube": ("brass", "tube_glass", "Glass tube with brass rings"),
    "capsule_tube_frame": ("brass", "tube_frame", "Brass frame ring"),
    "recharge_dock_top": ("steel", "dock_top", "Charging pad with yellow-black edge"),
    "recharge_dock_side": ("steel", "dock_side", "Panel with cable sockets"),
    "domain_beacon_side": ("steel", "beacon_side", "Steel frame with glass and amethyst"),
    "domain_beacon_top": ("steel", "beacon_top", "Emitter lens"),
    "depot_side": ("vantite_steel", "depot_side", "Vantite steel container with stencil"),
    "depot_top": ("vantite_steel", "panel_top", "Top"),
    "mag_rail": ("lattice", "mag_rail", "Rail with glowing violet magnets"),
    "mag_rail_station_side": ("lattice", "station_side", "Lattice station panel"),
    "mag_rail_station_top": ("lattice", "station_top", "Docking clamps"),
    "phase_node": ("lattice", "phase_node", "Crystal sphere in an aegis cage"),
    "domain_archive_front": ("aegis", "archive_front", "Black-gold terminal with holo grid"),
    "domain_archive_side": ("aegis", "panel", "Black-gold panel"),
    "matter_conduit": ("aegis", "conduit", "Gold pipe with violet core"),
    "steam_cloud": ("steel", "noise", "unused filler"),
}

# Block textures that are only used as particle textures for GeckoLib-rendered blocks.
PARTICLE_TEXTURES = [
    "hand_crank", "treadwheel", "windmill", "stone_flywheel", "stamp_mill", "colony_core", "worker_assembler",
    "waterwheel", "boiler", "steam_engine", "lightning_mast", "wooden_pole", "sluice", "blast_smelter",
    "steel_pylon", "steam_turbine", "jaw_crusher", "fabricator", "hv_pylon", "hydro_turbine", "wind_tower",
    "magma_tap", "froth_flotation_plant", "cable_car_station", "crate_launcher", "catch_net", "outpost_core",
    "plasma_generator", "cryo_node", "superconducting_coil", "plasma_separator", "nanite_hive", "matter_buffer",
    "replicator_foundry", "singularity_core", "gyro_heart", "matter_compiler", "seed_matrix",
    "restoration_projector", "forge_of_ascension", "quern", "capsule_station",
]

# ---------------------------------------------------------------------------------------------
# GeckoLib block models. footprint = ((minX,minY,minZ),(maxX,maxY,maxZ)) in blocks from the CENTER
# block for facing north. bones = required bone names (Java animates or hides them by name).
# anims = {name: (loop_type, seconds)}; loop_type in loop, once, hold.
# ---------------------------------------------------------------------------------------------
GEO_BLOCKS = {
    "hand_crank": dict(tier=1, footprint=((0, 0, 0), (0, 0, 0)), palette="bronze",
                       bones=["base", "shaft", "crank", "handle"],
                       anims={"turn": ("loop", 1.0), "idle": ("loop", 1.0)},
                       look="Cobblestone base block (10px tall), bronze gearbox, a crank arm on the front face rotating about Z with a wooden handle."),
    "quern": dict(tier=1, footprint=((0, 0, 0), (0, 0, 0)), palette="stone",
                  bones=["base", "runner", "handle"],
                  anims={"turn": ("loop", 1.0), "idle": ("loop", 1.0)},
                  look="Two round millstones (bottom fixed 6px, top runner 5px) with a wooden stick handle; runner rotates about Y."),
    "treadwheel": dict(tier=1, footprint=((-1, 0, 0), (1, 2, 0)), palette="wood",
                       bones=["frame", "wheel"], anims={"spin": ("loop", 2.0), "idle": ("loop", 1.0)},
                       look="Large wooden wheel 46px diameter, 14px wide drum with slats on the inside, on an A-frame with bronze axle; wheel rotates about Z."),
    "windmill": dict(tier=1, footprint=((0, 0, 0), (0, 3, 0)), palette="wood",
                     bones=["tower", "cap", "rotor", "blade_1", "blade_2", "blade_3", "blade_4"],
                     anims={"spin": ("loop", 2.0), "wobble2": ("loop", 2.0), "wobble4": ("loop", 2.0), "wobble6": ("loop", 2.0), "idle": ("loop", 1.0)},
                     look="Tapered wooden tower 4 blocks tall; cap on top; rotor hub on the front (-Z) at y=56; 4 wool-sail blades 40px long (overhang allowed). Rotor rotates about Z. wobble anims rock the cap about Y by 2/4/6 degrees."),
    "stone_flywheel": dict(tier=1, footprint=((-1, 0, 0), (1, 2, 0)), palette="stone",
                           bones=["mount", "disc"], anims={"spin": ("loop", 1.0), "idle": ("loop", 1.0)},
                           look="Heavy smooth-stone disc 44px diameter, 10px thick, bronze rim and spokes, on a stone mount with bronze bearing; disc rotates about Z around pivot (0,24,0)."),
    "stamp_mill": dict(tier=1, footprint=((0, 0, 0), (0, 1, 0)), palette="wood",
                       bones=["frame", "cam", "hammer"], anims={"stamp": ("loop", 2.0), "idle": ("loop", 1.0)},
                       look="Log frame 2 blocks tall, cobblestone mortar box at bottom, iron-shod wooden hammer lifted by a cam on a bronze shaft; stamp = hammer rises 8px over 1.6s then drops in 0.2s."),
    "colony_core": dict(tier=1, footprint=((-1, 0, -1), (1, 2, 1)), palette="bronze",
                        bones=["roots", "frame", "petal_1", "petal_2", "petal_3", "petal_4", "petal_5", "petal_6", "core_orb", "ring"],
                        anims={"roots": ("hold", 3.0), "frame": ("hold", 4.0), "bloom": ("hold", 3.0), "active": ("loop", 4.0), "upgrade": ("once", 5.0), "dormant": ("loop", 2.0)},
                        look="Organic-mechanical brain: bronze roots spread over the 3x3 floor, a ribbed frame rises, six petals open like a flower around a glowing amber orb, a rotating ring above. roots: roots scale from 0 to 1. frame: frame rises from y=-40 to 0. bloom: petals rotate open 70 degrees and orb scales up. active: orb pulse + ring spin. upgrade: ring spins fast, petals flex. dormant: petals closed, orb dim."),
    "worker_assembler": dict(tier=1, footprint=((0, 0, 0), (1, 1, 1)), palette="bronze",
                             bones=["base", "gantry", "arm_l", "arm_r", "weld_tip_l", "weld_tip_r", "chassis", "hatch"],
                             anims={"idle": ("loop", 2.0), "assemble": ("loop", 2.0), "finish": ("once", 2.0)},
                             look="2x2x2 workshop cage: bronze base plate, gantry over it with two jointed welding arms, chassis frame in the middle (Java scales it by progress), front hatch that swings open in finish."),
    "waterwheel": dict(tier=2, footprint=((0, 0, -1), (0, 2, 1)), palette="wood",
                       bones=["frame", "wheel"], anims={"spin": ("loop", 3.0), "idle": ("loop", 1.0)},
                       look="Wooden paddle wheel 46px diameter in the Y-Z plane, axle along X, 12 paddles, steel hub, timber frame; wheel rotates about X."),
    "boiler": dict(tier=2, footprint=((0, 0, 0), (1, 2, 1)), palette="brick",
                   bones=["body", "firebox", "door", "gauge", "gauge_needle", "glow"],
                   anims={"idle": ("loop", 1.0), "burning": ("loop", 1.0)},
                   look="Brick firebox at the bottom with a steel door and orange glow window (bone glow), riveted steel drum on top (2 blocks tall), pressure gauge dial on the front with a needle (Java rotates gauge_needle about Z)."),
    "steam_engine": dict(tier=2, footprint=((0, 0, 0), (0, 1, 1)), palette="steel",
                         bones=["frame", "cylinder", "piston", "rod", "flywheel"],
                         anims={"run": ("loop", 1.0), "idle": ("loop", 1.0)},
                         look="Horizontal steel cylinder on a brass bed, piston rod driving a red-spoked flywheel; run = piston slides 6px and flywheel turns once."),
    "lightning_mast": dict(tier=2, footprint=((0, 0, 0), (0, 3, 0)), palette="copper",
                           bones=["mast", "coil", "rod", "glow"], anims={"idle": ("loop", 1.0), "charge": ("once", 1.0)},
                           look="Lattice copper mast 4 blocks tall, brass base with coil rings, spike rod on top; glow bone flashes in charge."),
    "wooden_pole": dict(tier=2, footprint=((0, 0, 0), (0, 2, 0)), palette="wood",
                        bones=["pole", "crossarm", "insulators"], anims={"idle": ("loop", 1.0)},
                        look="Telegraph pole 3 blocks tall, 6px round log, crossarm with brown glass insulators at the top (attach point y=+0.35 over the top block centre)."),
    "sluice": dict(tier=2, footprint=((0, 0, -1), (0, 0, 1)), palette="wood",
                   bones=["trough", "head", "riffles", "water"], anims={"idle": ("loop", 1.0), "flow": ("loop", 1.0)},
                   look="Inclined wooden trough 3 blocks long, high end (head) at +Z, riffle bars, brass plate lining; water bone is a translucent blue sheet that ripples in flow."),
    "blast_smelter": dict(tier=2, footprint=((0, 0, 0), (1, 1, 1)), palette="brick",
                          bones=["body", "door", "chimney", "glow", "pipes"], anims={"idle": ("loop", 1.0), "working": ("loop", 1.0)},
                          look="Brick furnace tower with brass bands, copper pipes, a chimney, front door with glowing window; working = glow pulses and door rattles."),
    "steel_pylon": dict(tier=3, footprint=((0, 0, 0), (0, 4, 0)), palette="steel",
                        bones=["mast", "arm", "insulators"], anims={"idle": ("loop", 1.0)},
                        look="Steel lattice mast 5 blocks tall on a brick foot, crossarm with grey ceramic insulators."),
    "steam_turbine": dict(tier=3, footprint=((-1, 0, 0), (1, 2, 1)), palette="steel",
                          bones=["casing", "rotor", "intake", "gauge"], anims={"spin": ("loop", 1.0), "idle": ("loop", 1.0)},
                          look="Large steel turbine casing (half-cylinder) with a visible bladed rotor behind a grille on the front, intake pipes at the back; rotor rotates about Z."),
    "jaw_crusher": dict(tier=3, footprint=((0, 0, 0), (1, 1, 1)), palette="steel",
                        bones=["body", "jaw", "flywheel", "hopper"], anims={"crush": ("loop", 1.0), "idle": ("loop", 1.0)},
                        look="Steel crusher with a hopper on top, swinging jaw plate in front, side flywheel; crush = jaw swings 10 degrees, flywheel turns."),
    "fabricator": dict(tier=3, footprint=((0, 0, 0), (1, 1, 0)), palette="steel",
                       bones=["body", "gantry", "head", "arm"], anims={"work": ("loop", 2.0), "idle": ("loop", 1.0)},
                       look="Glass-fronted cabinet with an XY gantry and a robotic head that moves over a bed; cyan indicator lights."),
    "hv_pylon": dict(tier=4, footprint=((0, 0, 0), (0, 6, 0)), palette="vantite_steel",
                     bones=["mast", "arms", "insulators"], anims={"idle": ("loop", 1.0)},
                     look="Tall lattice transmission tower 7 blocks, wide arms at the top with long insulator strings, red-white bands at the top."),
    "hydro_turbine": dict(tier=4, footprint=((-1, 0, -1), (1, 2, 1)), palette="vantite_steel",
                          bones=["casing", "runner", "intake", "outlet"], anims={"spin": ("loop", 1.0), "idle": ("loop", 1.0)},
                          look="Spiral case turbine: intake funnel on top centre, spiral casing, outlet on front bottom, runner visible through a port; runner rotates about Y."),
    "wind_tower": dict(tier=4, footprint=((0, 0, 0), (0, 7, 0)), palette="vantite_steel",
                       bones=["tower", "nacelle", "rotor", "blade_1", "blade_2", "blade_3"],
                       anims={"spin": ("loop", 2.0), "wobble2": ("loop", 3.0), "wobble4": ("loop", 3.0), "wobble6": ("loop", 3.0), "idle": ("loop", 1.0)},
                       look="Slim white-teal tower 8 blocks, nacelle on top, 3 long blades (64px, overhang allowed) on the front rotor; rotor about Z."),
    "magma_tap": dict(tier=4, footprint=((0, 0, 0), (0, 1, 0)), palette="vantite_steel",
                      bones=["base", "pump", "glow"], anims={"pump": ("loop", 1.0), "idle": ("loop", 1.0)},
                      look="Heavy pump head on a heat-shielded pipe, glowing orange intake at the bottom, pump piston moves in pump."),
    "froth_flotation_plant": dict(tier=4, footprint=((-1, 0, -1), (1, 2, 1)), palette="vantite_steel",
                                  bones=["tank", "agitator", "froth", "window"], anims={"agitate": ("loop", 1.0), "idle": ("loop", 1.0)},
                                  look="Round tank with a glass window ring, agitator shaft from a motor on top, froth bone on top surface; agitator spins."),
    "cable_car_station": dict(tier=4, footprint=((0, 0, 0), (1, 2, 1)), palette="vantite_steel",
                              bones=["frame", "bull_wheel", "platform"], anims={"wheel_spin": ("loop", 2.0), "idle": ("loop", 1.0)},
                              look="Station tower with a big horizontal bull wheel on top (rotates about Y), loading platform with railing."),
    "crate_launcher": dict(tier=4, footprint=((0, 0, 0), (0, 1, 0)), palette="vantite_steel",
                           bones=["base", "arm", "cup"], anims={"throw": ("once", 1.0), "idle": ("loop", 1.0)},
                           look="Catapult: base with spring drum, long arm with a crate cup; throw = arm swings 110 degrees up fast then returns."),
    "outpost_core": dict(tier=4, footprint=((0, 0, 0), (1, 1, 1)), palette="vantite_steel",
                         bones=["base", "spire", "orb", "fins"], anims={"grow": ("hold", 10.0), "active": ("loop", 3.0)},
                         look="Compact colony core: vantite base, spire with green orb; grow rises from the ground."),
    "plasma_generator": dict(tier=5, footprint=((-1, 0, -1), (1, 2, 1)), palette="lattice",
                             bones=["casing", "torus", "plasma", "coils"], anims={"run": ("loop", 2.0), "idle": ("loop", 1.0)},
                             look="Tokamak ring of violet coils around a glowing magenta plasma torus inside a lattice frame; plasma pulses and spins."),
    "cryo_node": dict(tier=5, footprint=((0, 0, 0), (0, 2, 0)), palette="frost",
                      bones=["pole", "head", "frost"], anims={"idle": ("loop", 2.0)},
                      look="Pole with a frosted cryo head, white coolant pipes; frost bone gently pulses."),
    "superconducting_coil": dict(tier=5, footprint=((0, 0, 0), (1, 1, 1)), palette="lattice",
                                 bones=["casing", "coil", "frost"], anims={"idle": ("loop", 2.0), "charge": ("loop", 2.0)},
                                 look="Cryostat drum with a visible copper-violet coil winding, frost on the outside."),
    "plasma_separator": dict(tier=5, footprint=((0, 0, 0), (1, 1, 1)), palette="lattice",
                             bones=["body", "chamber", "arc", "nozzles"], anims={"work": ("loop", 1.0), "idle": ("loop", 1.0)},
                             look="Glass chamber with electrodes, nozzles feeding coloured streams into receivers."),
    "nanite_hive": dict(tier=5, footprint=((0, 0, 0), (1, 1, 1)), palette="nanite",
                        bones=["shell", "cells", "vent"], anims={"release": ("loop", 2.0), "idle": ("loop", 2.0)},
                        look="Hexagonal silver hive cells on a lattice base, vent on top opens in release."),
    "matter_buffer": dict(tier=5, footprint=((0, 0, 0), (1, 1, 1)), palette="lattice",
                          bones=["frame", "swirl", "core"], anims={"swirl": ("loop", 3.0)},
                          look="Lattice frame around a glass sphere with a swirling violet matter vortex."),
    "replicator_foundry": dict(tier=5, footprint=((-1, 0, -1), (1, 2, 1)), palette="lattice",
                               bones=["frame", "printer_head", "bed", "doors"], anims={"print": ("loop", 3.0), "idle": ("loop", 2.0)},
                               look="Large print bay with a moving printer head over a bed, front doors."),
    "singularity_core": dict(tier=6, footprint=((-2, 0, -2), (2, 4, 2)), palette="aegis",
                             bones=["base", "ring_outer", "ring_mid", "ring_inner", "core"],
                             anims={"run": ("loop", 6.0), "idle": ("loop", 4.0), "drink": ("once", 1.0)},
                             look="Black-gold pedestal with three nested gyro rings around a black sphere with cyan rim glow; rings rotate on different axes."),
    "gyro_heart": dict(tier=6, footprint=((-2, 0, -2), (2, 2, 2)), palette="aegis",
                       bones=["base", "rotor", "rim"], anims={"spin": ("loop", 1.0), "idle": ("loop", 1.0)},
                       look="Huge horizontal gyroscope disc in a black-gold housing, cyan glow seams; rotor spins about Y."),
    "matter_compiler": dict(tier=6, footprint=((0, 0, 0), (1, 1, 1)), palette="aegis",
                            bones=["body", "chamber", "emitters", "glow"], anims={"compile": ("loop", 2.0), "idle": ("loop", 1.0)},
                            look="Black-gold block with an inner chamber and three emitter arms pointing at the centre."),
    "seed_matrix": dict(tier=6, footprint=((-1, 0, -1), (1, 2, 1)), palette="aegis",
                        bones=["base", "pillars", "seed", "rings"], anims={"pack": ("loop", 3.0), "idle": ("loop", 2.0)},
                        look="Four pillars around a floating seed pod, rings of light pack it."),
    "restoration_projector": dict(tier=6, footprint=((0, 0, 0), (0, 1, 0)), palette="aegis",
                                  bones=["base", "lens", "ring"], anims={"project": ("loop", 2.0), "idle": ("loop", 2.0)},
                                  look="Pedestal with a big projector lens on top, rotating ring."),
    "forge_of_ascension": dict(tier=6, footprint=((-1, 0, -1), (1, 3, 1)), palette="aegis",
                               hollow=[(0, 1, 0), (0, 2, 0), (-1, 1, 0), (1, 1, 0), (0, 1, -1), (0, 2, -1), (-1, 2, 0), (1, 2, 0), (0, 1, 1), (0, 2, 1)],
                               bones=["base", "pad", "pillars", "roof", "arm_1", "arm_2", "arm_3", "arm_4", "scan_ring"],
                               anims={"idle": ("loop", 2.0), "wake": ("once", 1.0), "scan": ("once", 2.0), "fit_boots": ("once", 5.0),
                                      "fit_legs": ("once", 6.0), "fit_chest": ("once", 7.0), "fit_helmet": ("once", 5.0),
                                      "seal": ("once", 4.0), "retract": ("once", 2.0)},
                               look="Open chamber: 3x3 floor with a glowing centre pad (2px high), 4 corner pillars, roof with 4 jointed robotic arms hanging down toward the player, a scan ring that sweeps down. The positions in hollow are empty (player stands at the pad)."),
    "capsule_station": dict(tier=3, footprint=((0, 0, 0), (0, 0, 0)), palette="brass",
                            bones=["body", "mouth", "valve"], anims={"puff": ("once", 0.5), "idle": ("loop", 1.0)},
                            look="Brass cabinet with a round tube mouth on top and a glass window; puff = valve flap pops."),
}

# Blocks that have no block entity but are shaped beyond one block use JSON (catch_net 3x1x3).
JSON_BIG = {
    "catch_net": dict(footprint=((-1, 0, -1), (1, 0, 1)), palette="vantite_steel"),
}

# ---------------------------------------------------------------------------------------------
# Worker rigs (06 section 2). Bones exactly as listed; optional bones hidden by Java per type.
# ---------------------------------------------------------------------------------------------
RIGS = {
    "walker": dict(
        base=["root", "body", "head", "eye_lamp", "arm_l", "arm_r", "hand_l", "hand_r", "leg_l", "leg_r", "foot_l", "foot_r", "cargo_slot"],
        optional=["carry_rack", "spring_key", "battery_pack", "tool_belt", "climb_spikes", "cable_reel", "leg_aux_l", "leg_aux_r", "drill",
                  "pod_shell_top", "pod_shell_l", "pod_shell_r", "lamp_mast"],
        upgrades=["upg_armor", "upg_cargo", "upg_range", "upg_speed"],
        height_px=21,
        anims=["idle", "walk", "carry", "work", "crank", "climb", "place", "weld", "string", "unfold", "deploy", "recharge", "rundown", "death"],
        look="Squat bipedal robot 1.3 blocks tall: boxy torso with a round single eye lamp, arms with clamp hands, stout legs with flat feet; spring key on the back (Porter), etc."),
    "crab": dict(
        base=["root", "carapace", "eye_stalk_l", "eye_stalk_r", "eye_lamp", "leg_l1", "leg_l2", "leg_l3", "leg_r1", "leg_r2", "leg_r3", "cargo_slot"],
        optional=["claw_l", "claw_r", "cargo_bed", "debris_scoop", "spring_key", "battery_pack", "fork_mast", "fork_carriage", "fork_tines", "pallet_slot"],
        upgrades=["upg_armor", "upg_cargo", "upg_range", "upg_speed"],
        height_px=11,
        anims=["idle", "walk", "carry", "work", "place", "lift", "lower", "scoop", "unfold", "recharge", "rundown", "death"],
        look="Six-legged crab robot: wide flat carapace, two eye stalks, claws in front, cargo bed on the back; Loader variant: fork mast at the front."),
    "drone": dict(
        base=["root", "hull", "eye_lamp", "rotor_fl", "rotor_fr", "rotor_bl", "rotor_br", "blade_fl", "blade_fr", "blade_bl", "blade_br", "battery_pack", "cargo_slot"],
        optional=["cargo_claw", "weld_arm", "weld_tip", "nozzle", "scanner_dish", "scanner_beam", "spool", "winch_hook", "heavy_plates", "arc_gun", "arc_coil"],
        upgrades=["upg_armor", "upg_cargo", "upg_range", "upg_speed"],
        height_px=8,
        anims=["idle", "fly", "carry", "work", "place", "weld", "scan", "string", "shoot", "unfold", "recharge", "rundown", "death", "rotor_spin"],
        look="Quad-rotor drone: compact hull, four arms with rotor nacelles and blades, eye lamp at the front, battery under the hull, tool bones below."),
}

WORKER_TYPES = {
    "porter": ("walker", "#B0793B", "#5A3B22", "#FFB347"),
    "hauler_crab": ("crab", "#C66A3D", "#4FA38A", "#FFD27F"),
    "builder_drone": ("drone", "#E6B422", "#6E7681", "#4FE3FF"),
    "loader": ("crab", "#E07B22", "#3D434A", "#F2F2F2"),
    "lineman": ("walker", "#3E6A8C", "#1E1E1E", "#FFE066"),
    "surveyor": ("drone", "#2BB3A3", "#D9DEE2", "#7CFF6B"),
    "cable_layer_drone": ("drone", "#B8322E", "#6E7681", "#FF9A3C"),
    "sentinel": ("drone", "#2C3036", "#C2263B", "#FF3B3B"),
    "replicator_walker": ("walker", "#7B4FD1", "#E8E2F5", "#FF5CF0"),
}

# Optional rig bones each worker type shows (06 section 2.2); all other optional bones are hidden.
WORKER_SHOWN = {
    "porter": ["carry_rack", "spring_key"],
    "lineman": ["battery_pack", "tool_belt", "climb_spikes", "cable_reel"],
    "replicator_walker": ["battery_pack", "leg_aux_l", "leg_aux_r", "drill", "pod_shell_top", "pod_shell_l", "pod_shell_r", "lamp_mast"],
    "hauler_crab": ["claw_l", "claw_r", "cargo_bed", "debris_scoop", "spring_key"],
    "loader": ["battery_pack", "fork_mast", "fork_carriage", "fork_tines", "pallet_slot"],
    "builder_drone": ["cargo_claw", "weld_arm", "weld_tip", "nozzle"],
    "surveyor": ["scanner_dish", "scanner_beam"],
    "cable_layer_drone": ["cargo_claw", "spool", "winch_hook", "heavy_plates"],
    "sentinel": ["arc_gun", "arc_coil", "heavy_plates"],
}

GEO_ENTITIES = {
    "gondola": dict(palette="vantite_steel", bones=["root", "hanger", "cabin", "door_l", "door_r", "cargo"],
                    anims={"doors_open": ("once", 3.0), "idle": ("loop", 1.0)},
                    look="Open-top steel ore gondola 2 blocks wide hanging from a hanger arm (hanger top at y=+40px = cable), doors swing."),
    "rolling_flywheel": dict(palette="stone", bones=["root", "disc"], anims={"roll": ("loop", 1.0), "wobble": ("once", 2.0)},
                             look="The stone flywheel disc alone (44px, bronze rim), rolling about its local X axis; wobble = tilting circular wobble to rest flat."),
}

# GeckoLib equipment items (07 section 4.2). Model group "item".
GEO_ITEMS = {
    "steam_repeater": dict(palette="steel", bones=["body", "drum", "barrel", "gauge"], anims={"idle": ("loop", 1.0), "fire": ("once", 0.25), "pump": ("once", 0.5)}),
    "electric_drill": dict(palette="electric", bones=["body", "bit", "battery"], anims={"idle": ("loop", 1.0), "spin": ("loop", 0.25)}),
    "electric_chainsaw": dict(palette="electric", bones=["body", "bar", "chain"], anims={"idle": ("loop", 1.0), "run": ("loop", 0.25)}),
    "arc_rifle": dict(palette="electric", bones=["body", "coils", "barrel"], anims={"idle": ("loop", 1.0), "fire": ("once", 0.5)}),
    "grapple_launcher": dict(palette="vantite", bones=["body", "reel", "hook"], anims={"idle": ("loop", 1.0), "fire": ("once", 0.5), "reel": ("loop", 0.5)}),
    "rail_rifle": dict(palette="vantite", bones=["body", "rail_top", "rail_bottom", "scope"], anims={"idle": ("loop", 1.0), "charge": ("hold", 1.5), "fire": ("once", 0.5)}),
    "nanite_multitool": dict(palette="lattice", bones=["body", "head_pick", "head_axe", "head_shovel", "glow"], anims={"idle": ("loop", 2.0), "use": ("loop", 0.5)}),
    "disassembler_beam": dict(palette="lattice", bones=["body", "emitter", "fins"], anims={"idle": ("loop", 2.0), "fire": ("loop", 0.5)}),
    "aegis_multitool": dict(palette="aegis", bones=["body", "head", "rings"], anims={"idle": ("loop", 2.0), "use": ("loop", 0.5)}),
    "singularity_lance": dict(palette="aegis", bones=["body", "ring_1", "ring_2", "ring_3", "tip"], anims={"idle": ("loop", 2.0), "charge": ("hold", 3.0), "fire": ("once", 1.0)}),
}

# GeckoLib armor sets. Bones armorHead, armorBody, armorRightArm, armorLeftArm, armorRightLeg,
# armorLeftLeg, armorRightBoot, armorLeftBoot (GeckoLib 5.5.7 names). Model group "armor".
ARMOR_BONES = ["armorHead", "armorBody", "armorRightArm", "armorLeftArm", "armorRightLeg", "armorLeftLeg",
               "armorRightBoot", "armorLeftBoot"]

GEO_ARMOR = {
    "exo_rig": dict(palette="electric", anims={"idle": ("loop", 2.0)}, look="Steel exoskeleton frame with pistons on the legs, battery pack on the back, visor."),
    "frontier_suit": dict(palette="vantite", anims={"idle": ("loop", 2.0), "jet": ("loop", 0.3)}, look="Teal expedition suit, visor glow, jet nozzles on the boots."),
    "lattice_carapace": dict(palette="lattice", anims={"idle": ("loop", 3.0)}, look="Violet crystalline plates over a dark undersuit, shimmer."),
    "aegis": dict(palette="aegis", anims={"idle": ("loop", 4.0)}, look="Black-gold armour with cyan glow seams and small orbit rings at the shoulders."),
}

# Flat vanilla armor layers (07 section 4.1): textures/entity/equipment/humanoid/<asset>.png (64x32)
# and humanoid_leggings/<asset>.png. Assets for all six sets (fallback layers for geo sets).
ARMOR_ASSETS = {"bronze": "bronze", "steel_rig": "steel", "exo_rig": "electric", "frontier_suit": "vantite",
                "lattice_carapace": "lattice", "aegis": "aegis"}

# ---------------------------------------------------------------------------------------------
# Item sprites. value = (palette, shape, description). Shapes are sprite painters in tools/paint.py.
# Every item id of 08a section 2 that is not a GeckoLib item has a sprite here.
# ---------------------------------------------------------------------------------------------
ITEM_SPRITES = {}


def _metal_items():
    for m, pal in [("tin", "tin"), ("zinc", "zinc"), ("vantite", "vantite")]:
        ITEM_SPRITES[f"raw_{m}"] = (pal, "raw_chunk", f"Raw {m} lump")
    for m, pal in [("tin", "tin"), ("bronze", "bronze"), ("zinc", "zinc"), ("brass", "brass"), ("steel", "steel"),
                   ("vantite", "vantite"), ("vantite_steel", "vantite_steel"), ("lattice_alloy", "lattice")]:
        ITEM_SPRITES[f"{m}_ingot"] = (pal, "ingot", f"{m} ingot")
    for m, pal in [("tin", "tin"), ("bronze", "bronze"), ("zinc", "zinc"), ("steel", "steel")]:
        ITEM_SPRITES[f"{m}_nugget"] = (pal, "nugget", f"{m} nugget")
    for m, pal in METAL_COLORS.items():
        ITEM_SPRITES[f"crushed_{m}"] = (pal, "crushed", f"Crushed {m} ore gravel")
        ITEM_SPRITES[f"washed_{m}"] = (pal, "washed", f"Washed {m}: wet glossy pile")
        ITEM_SPRITES[f"pure_{m}"] = (pal, "pure", f"Pure {m}: clean crystal shards")
        ITEM_SPRITES[f"{m}_concentrate"] = (pal, "concentrate", f"{m} concentrate: round pellets")


_metal_items()
ITEM_SPRITES.update({
    "lattice_shard": ("lattice", "shard", "Violet crystal shard"),
    "lattice_crystal": ("lattice", "crystal", "Cut violet crystal, glowing core"),
    "aegis_plate": ("aegis", "plate_glow", "Black-gold armour plate with cyan seam"),
    "slag": ("slag", "slag", "Porous dark slag lump"),
    "bronze_gear": ("bronze", "gear", "Bronze gear"),
    "mainspring": ("iron", "spring", "Coiled steel spring"),
    "steel_gear": ("steel", "gear", "Steel gear"),
    "copper_coil": ("copper", "coil", "Copper wire coil on iron core"),
    "steel_plate": ("steel", "plate", "Steel plate"),
    "brass_plate": ("brass", "plate", "Brass plate"),
    "basic_circuit": ("verdigris", "circuit", "Green circuit board, copper traces"),
    "servo": ("steel", "servo", "Small servo motor with shaft"),
    "advanced_circuit": ("vantite", "circuit_adv", "Teal circuit board with chip"),
    "nanite_paste": ("nanite", "paste", "Silver shimmering paste in a tube"),
    "ai_core": ("lattice", "ai_core", "Glowing violet processor cube"),
    "lattice_fuel_cell": ("lattice", "cell_fuel", "Fuel cell, magenta glow"),
    "coolant_cell": ("frost", "cell_coolant", "Coolant cell, icy blue"),
    "rope_coil": ("wood", "rope", "Coil of hemp rope"),
    "copper_wire_spool": ("copper", "spool", "Wooden spool with copper wire"),
    "insulated_cable_spool": ("rubber", "spool", "Spool with black insulated cable"),
    "hv_cable_spool": ("vantite_steel", "spool_hv", "Steel spool with thick red-banded HV cable"),
    "superconductor_spool": ("frost", "spool_frost", "Frosted spool, violet cable"),
    "wrench": ("bronze", "wrench", "Bronze wrench"),
    "wire_cutter": ("steel", "cutter", "Steel wire cutters, bronze grips"),
    "multimeter": ("brass", "meter", "Brass multimeter with dial"),
    "survey_lens": ("vantite", "lens", "Brass monocle with teal lens"),
    "planning_tablet": ("bronze", "tablet", "Bronze tablet with glowing screen"),
    "field_manual": ("wood", "book", "Leather field manual with bronze cog emblem"),
    "core_seed": ("core_root", "seed", "Bronze seed pod with amber light"),
    "core_upgrade_t2": ("brass", "upgrade", "Brass upgrade module"),
    "core_upgrade_t3": ("steel", "upgrade", "Steel upgrade module, cyan light"),
    "core_upgrade_t4": ("vantite_steel", "upgrade", "Vantite upgrade module, green light"),
    "core_upgrade_t5": ("lattice", "upgrade", "Lattice upgrade module, magenta light"),
    "core_upgrade_t6": ("aegis", "upgrade", "Aegis upgrade module, cyan light"),
    "pattern_card": ("brass", "card", "Brass punch card (blank)"),
    "pattern_card_recorded": ("brass", "card_recorded", "Punch card with holes and blue stripe"),
    "outpost_seed": ("vantite_steel", "seed_outpost", "Steel capsule with green light and sapling"),
    "worker_capsule": ("bronze", "capsule", "Glass capsule with bronze caps (empty)"),
    "worker_capsule_filled": ("bronze", "capsule_filled", "Capsule with a tiny robot inside"),
    "autarky_seed": ("aegis", "seed_autarky", "Black-gold seed with cyan veins"),
    "walker_frame": ("bronze", "frame_walker", "Bronze humanoid frame"),
    "crab_frame": ("steel", "frame_crab", "Steel six-leg frame"),
    "drone_frame": ("steel", "frame_drone", "Steel quad-rotor frame"),
    "worker_upgrade_speed": ("electric_accent", "upgrade_chip", "Chip with arrow"),
    "worker_upgrade_cargo": ("brass", "upgrade_chip", "Chip with box"),
    "worker_upgrade_armor": ("steel", "upgrade_chip", "Chip with shield"),
    "worker_upgrade_range": ("verdigris", "upgrade_chip", "Chip with antenna"),
})

EQUIP_SPRITES = {}
for mat, pal in [("bronze", "bronze"), ("steel", "steel")]:
    for tool in ["pickaxe", "axe", "shovel", "hoe", "sword"]:
        if mat == "steel" and tool == "hoe":
            continue
        EQUIP_SPRITES[f"{mat}_{tool}"] = (pal, tool, f"{mat} {tool}, wooden handle")
    for piece in ["helmet", "chest", "legs", "boots"]:
        EQUIP_SPRITES[f"{mat}_{piece}"] = (pal, f"armor_{piece}", f"{mat} {piece}")
EQUIP_SPRITES.update({
    "bronze_sling": ("bronze", "sling", "Wooden Y-sling with leather pouch"),
    "steel_sledgehammer": ("steel", "sledgehammer", "Heavy steel sledgehammer"),
    "shock_baton": ("electric", "baton", "Steel baton with blue glowing tip"),
    "vantite_blade": ("vantite", "sword_long", "Long teal blade"),
    "vantite_excavator": ("vantite", "excavator", "Vantite pick-shovel: pick arc, spade tip, teal core"),
    "lattice_blade": ("lattice", "sword_long_glow", "Violet crystal blade"),
    "aegis_blade": ("aegis", "sword_long_glow", "Black-gold blade, cyan edge"),
})
for set_id, pal in [("exo_rig", "electric"), ("frontier_suit", "vantite"), ("lattice_carapace", "lattice"), ("aegis", "aegis")]:
    for piece in ["helmet", "chest", "legs", "boots"]:
        EQUIP_SPRITES[f"{set_id}_{piece}"] = (pal, f"armor_{piece}_tech", f"{set_id} {piece}")
for gid, spec in GEO_ITEMS.items():
    EQUIP_SPRITES[gid] = (spec["palette"], f"icon_{gid}", f"Inventory icon of {gid}")
ITEM_SPRITES.update(EQUIP_SPRITES)

# ---------------------------------------------------------------------------------------------
# Other textures.
# ---------------------------------------------------------------------------------------------
OTHER_TEXTURES = {
    "entity/cable/rope.png": ("wood", "cable_rope", "16x16 twisted hemp rope, tileable along U"),
    "entity/cable/copper.png": ("copper", "cable_metal", "16x16 bare copper wire strands"),
    "entity/cable/insulated.png": ("rubber", "cable_insulated", "16x16 black rubber with faint stripe"),
    "entity/cable/hv.png": ("rubber", "cable_hv", "16x16 thick black with red bands"),
    "entity/cable/superconductor.png": ("lattice", "cable_super", "16x16 violet core with frost sheath"),
    "entity/cable/phase.png": ("aegis_glow", "beam", "16x16 cyan energy beam, bright centre"),
    "entity/cable/gondola_cable.png": ("steel", "cable_metal", "16x16 steel wire rope"),
    "entity/grapple_hook.png": ("vantite", "hook", "32x32 hook entity texture"),
    "entity/beam/disassembler.png": ("lattice", "beam", "beam texture"),
    "entity/beam/lance.png": ("aegis_glow", "beam", "beam texture"),
    "misc/ghost_white.png": ("glass", "white", "plain white 16x16 for tinted ghosts"),
}

# Mod logo for the mods list: 256x256 at src/main/resources/autarkia_logo.png
LOGO = ("bronze", "logo", "Colony core flower with a gear and the word AUTARKIA")

"""Cable terminals of grid nodes: one table drives the model fittings (GeckoLib and block JSON) and the Java cable ends.

Points are model pixels in the node frame: origin at the bottom centre of the node (centre) block, x east, y up, z south,
block facing north; Java turns them with the horizontal facing. Each fitting is (x, y, z, style) or (x, y, z, style, side):
the base point sits on the model surface and the style stacks its parts from there, upward or out along side (+x, -x,
+z, -z). Style "end" adds nothing: the point is the cable end on an existing model part (pole insulators).
tools/geo.py adds the parts to block models, tools/datagen.py to block JSON models, and tools/data.py writes
src/generated/java/dev/autarkia/registry/CableTerminals.java.
"""

from pathlib import Path

from art_spec import GEO_BLOCKS

ROPE = "rope"
BUSHING = "bushing"
END = "end"

STYLES = {
    ROPE: dict(end=3.0, parts=[((2, 2, 2), 0, "dark_wood:timber", "rope_pulley_side"), ((4, 2, 4), 2, "wood:timber", "rope_pulley"),
                               ((2, 1, 2), 4, "brass:plate", "brass_block")]),
    BUSHING: dict(end=4.5, parts=[((3, 1, 3), 0, "steel:rivet", "steel_block"), ((2, 1, 2), 1, "ceramic:plate", "wall_insulator"),
                                  ((3, 1, 3), 2, "ceramic:plate", "wall_insulator"), ((2, 1, 2), 3, "ceramic:plate", "wall_insulator"),
                                  ((2, 1, 2), 4, "copper:plate", "brass_block")]),
    END: dict(end=0.0, parts=[]),
}
SIDES = {"up": (0, 1, 0), "+x": (1, 0, 0), "-x": (-1, 0, 0), "+z": (0, 0, 1), "-z": (0, 0, -1)}

TERMINALS = {
    "wooden_pole": [(6, 44.5, 0, END), (-6, 44.5, 0, END)],
    "steel_pylon": [(6.5, 73.5, 0, END), (-6.5, 73.5, 0, END), (6.5, 59.5, 0, END), (-6.5, 59.5, 0, END)],
    "hv_pylon": [(6.5, 94, 0, END), (-6.5, 94, 0, END), (6.5, 81, 0, END), (-6.5, 81, 0, END)],
    "cryo_node": [(6, 45, 0, BUSHING, "+x"), (-6, 45, 0, BUSHING, "-x"), (0, 45, 6, BUSHING, "+z"), (0, 45, -6, BUSHING, "-z")],
    "transformer": [(4, 16, 0, BUSHING), (-4, 16, 0, BUSHING)],
    "fuse_box": [(4, 16, 3, BUSHING), (-4, 16, 3, BUSHING)],
    "hand_crank": [(0, 16, 2, ROPE)],
    "treadwheel": [(0, 24, 8, ROPE, "+z"), (0, 24, -8, ROPE, "-z")],
    "windmill": [(0, 8, 8, ROPE, "+z"), (5, 8, -8, ROPE, "-z"), (8, 8, 0, ROPE, "+x"), (-8, 8, 0, ROPE, "-x")],
    "stone_flywheel": [(0, 31, 7, ROPE), (0, 31, -7, ROPE)],
    "stamp_mill": [(0, 32, 0, ROPE)],
    "winding_post": [(0, 16, 0, ROPE)],
    "worker_assembler": [(22.5, 32, 22.5, ROPE), (-6.5, 32, -6.5, ROPE), (22.5, 32, -6.5, ROPE), (-6.5, 32, 22.5, ROPE)],
    "industrial_lamp": [(4, 16, 4, BUSHING)],
    "capacitor_bank": [(4, 16, 4, BUSHING)],
    "lightning_mast": [(0, 12, 5, BUSHING, "+z"), (0, 12, -5, BUSHING, "-z")],
    "waterwheel": [(6.5, 27, 0, BUSHING)],
    "steam_engine": [(4, 3, 20, BUSHING), (-5, 3, -5, BUSHING)],
    "blast_smelter": [(8, 26, 15.5, BUSHING), (8, 26, 0.5, BUSHING)],
    "plate_press": [(4, 16, 4, BUSHING)],
    "wire_drawer": [(4, 16, 4, BUSHING)],
    "steam_turbine": [(0, 46, 17, BUSHING)],
    "battery_bank": [(4, 16, 4, BUSHING)],
    "jaw_crusher": [(8, 14, 22, BUSHING, "+z"), (21, 14, 8, BUSHING, "+x")],
    "magnetic_separator": [(4, 16, 4, BUSHING)],
    "electrolyzer": [(4, 16, 4, BUSHING)],
    "fabricator": [(16, 32, 4, BUSHING)],
    "warehouse_controller": [(4, 16, 4, BUSHING)],
    "capsule_station": [(5.5, 13, 5.5, BUSHING)],
    "domain_beacon": [(5, 16, 5, BUSHING)],
    "recharge_dock": [(4, 16, 4, BUSHING)],
    "outpost_core": [(22.5, 3, 8, BUSHING), (-6.5, 3, 8, BUSHING), (8, 3, 22.5, BUSHING), (8, 3, -6.5, BUSHING)],
    "cable_car_station": [(21.5, 41, 21.5, BUSHING), (-5.5, 41, 21.5, BUSHING), (21.5, 41, -5.5, BUSHING), (-5.5, 41, -5.5, BUSHING)],
    "crate_launcher": [(7, 25, -6, BUSHING), (-7, 25, -6, BUSHING)],
    "froth_flotation_plant": [(16, 43, 0, BUSHING), (-16, 43, 0, BUSHING)],
    "hydro_turbine": [(21, 4, 21, BUSHING), (-21, 4, 21, BUSHING), (21, 4, -21, BUSHING), (-21, 4, -21, BUSHING)],
    "wind_tower": [(6, 4, 6, BUSHING), (-6, 4, 6, BUSHING), (6, 4, -6, BUSHING), (-6, 4, -6, BUSHING)],
    "magma_tap": [(5, 26, 5, BUSHING)],
    "replicator_foundry": [(20, 47, 20, BUSHING), (-20, 47, 20, BUSHING), (20, 47, -20, BUSHING), (-20, 47, -20, BUSHING)],
    "plasma_separator": [(12, 28, 18, BUSHING)],
    "superconducting_coil": [(8, 32, 8, BUSHING)],
    "plasma_generator": [(21.5, 48, 21.5, BUSHING), (-21.5, 48, 21.5, BUSHING), (21.5, 48, -21.5, BUSHING), (-21.5, 48, -21.5, BUSHING)],
    "mag_rail_station": [(5, 4, 5, BUSHING)],
    "nanite_hive": [(23, 10, -5.5, BUSHING, "+x"), (-7, 10, 21.5, BUSHING, "-x"), (21.5, 10, 23, BUSHING, "+z"),
                    (-5.5, 10, -7, BUSHING, "-z")],
    "restoration_projector": [(5, 6, 5, BUSHING), (-5, 6, 5, BUSHING), (5, 6, -5, BUSHING), (-5, 6, -5, BUSHING)],
    "forge_of_ascension": [(20, 64, 20, BUSHING), (-20, 64, 20, BUSHING), (20, 64, -20, BUSHING), (-20, 64, -20, BUSHING)],
    "seed_matrix": [(12, 4, 21, BUSHING), (-12, 4, -21, BUSHING), (21, 4, -12, BUSHING), (-21, 4, 12, BUSHING)],
    "matter_compiler": [(16, 32, 16, BUSHING)],
    "matter_conduit": [(5, 16, 5, BUSHING)],
    "domain_archive": [(4, 16, 4, BUSHING)],
    "singularity_core": [(12, 8, 30, BUSHING), (-12, 8, -30, BUSHING), (30, 8, -12, BUSHING), (-30, 8, 12, BUSHING)],
    "gyro_heart": [(12, 4, 36, BUSHING), (-12, 4, -36, BUSHING), (36, 4, -12, BUSHING), (-36, 4, 12, BUSHING)],
}
PLAIN_MODELS = {"capacitor_bank": [f"capacitor_bank_c{k}" for k in range(5)], "industrial_lamp": ["industrial_lamp", "industrial_lamp_on"],
                "matter_conduit": ["matter_conduit_terminal"]}
FULL_CUBE_FACES = {
    "minecraft:block/cube_all": {f: "#all" for f in ("north", "south", "east", "west", "up", "down")},
    "minecraft:block/cube_column": {"up": "#end", "down": "#end", "north": "#side", "south": "#side", "east": "#side", "west": "#side"},
    "minecraft:block/orientable_with_bottom": {"up": "#top", "down": "#bottom", "north": "#front", "south": "#side", "east": "#side",
                                               "west": "#side"},
}


def _fittings(ident: str):
    """Yield (base, style, side vector) of every fitting of a block."""
    for f in TERMINALS.get(ident, []):
        yield (f[0], f[1], f[2]), f[3], SIDES[f[4] if len(f) > 4 else "up"]


def ends(ident: str) -> list[tuple[float, float, float]]:
    """Return the cable end points of a block in node-frame pixels."""
    out = []
    for base, style, d in _fittings(ident):
        e = STYLES[style]["end"]
        out.append(tuple(b + v * e for b, v in zip(base, d)))
    return out


def _parts(ident: str):
    """Yield (min corner, max corner, geo material, plain texture) of every terminal part in node-frame pixels."""
    for base, style, d in _fittings(ident):
        ax = next(i for i, v in enumerate(d) if v != 0)
        for (w, h, dep), y0, mat, tex in STYLES[style]["parts"]:
            size = {0: (h, dep, w), 1: (w, h, dep), 2: (w, dep, h)}[ax]
            lo, hi = [], []
            for i in range(3):
                if i == ax:
                    a, b = base[i] + d[i] * y0, base[i] + d[i] * (y0 + size[i])
                    lo.append(min(a, b))
                    hi.append(max(a, b))
                else:
                    lo.append(base[i] - size[i] / 2.0)
                    hi.append(base[i] + size[i] / 2.0)
            yield lo, hi, mat, tex


def add_geo(model, footprint) -> None:
    """Add the terminal parts of a block to a GeckoLib model under a bone parented to its root bone.

    Args:
        footprint: ((x0, y0, z0), (x1, y1, z1)) in blocks; the model is centred on its horizontal centre.
    """
    if not any(True for _ in _parts(model.ident)):
        return
    (x0, _, z0), (x1, _, z1) = footprint
    cx, cz = (x0 + x1) / 2.0 * 16, (z0 + z1) / 2.0 * 16
    model.bone("terminals", model.bones[0]["name"])
    for lo, hi, mat, _ in _parts(model.ident):
        model.box("terminals", (-(hi[0] - cx), lo[1], lo[2] - cz), (hi[0] - lo[0], hi[1] - lo[1], hi[2] - lo[2]), mat)


def owner(model_name: str) -> str | None:
    """Return the block whose terminals belong in a block JSON model, or None (GeckoLib blocks get them in geo.json)."""
    for ident, names in PLAIN_MODELS.items():
        if model_name in names:
            return ident
    plain = model_name in TERMINALS and model_name not in PLAIN_MODELS and model_name not in GEO_BLOCKS
    return model_name if plain else None


def with_plain(model_name: str, obj: dict) -> dict:
    """Return a block JSON model with the terminal parts of its block appended (parent cubes become explicit elements)."""
    ident = owner(model_name)
    parts = list(_parts(ident)) if ident else []
    if not parts:
        return obj
    obj = dict(obj)
    textures = dict(obj.get("textures", {}))
    elements = list(obj.get("elements", []))
    parent = obj.get("parent")
    if not elements and parent in FULL_CUBE_FACES:
        faces = FULL_CUBE_FACES[parent]
        elements = [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {f: {"texture": t, "cullface": f} for f, t in faces.items()}}]
        textures.setdefault("particle", faces["north"])
        obj["parent"] = "minecraft:block/block"
    for lo, hi, _, tex in parts:
        key = f"terminal_{tex}"
        textures[key] = f"autarkia:block/{tex}"
        elements.append({"from": [lo[0] + 8, lo[1], lo[2] + 8], "to": [hi[0] + 8, hi[1], hi[2] + 8],
                         "faces": {f: {"texture": f"#{key}"} for f in ("north", "south", "east", "west", "up", "down")}})
    obj["textures"] = textures
    obj["elements"] = elements
    return obj


def write_java(path: Path) -> None:
    """Write CableTerminals.java with the cable end points of every block that has terminals."""
    rows = []
    for ident in TERMINALS:
        pts = ", ".join("{" + ", ".join(f"{v:g}" for v in p) + "}" for p in ends(ident))
        rows.append(f'        Map.entry("{ident}", new double[][] {{{pts}}})')
    lines = ["package dev.autarkia.registry;", "", "import java.util.Map;", "",
             "/** Generated by tools/data.py from tools/terminals.py: cable end points of grid nodes in pixels, node frame facing north. */",
             "public final class CableTerminals {", "    private static final Map<String, double[][]> POINTS = Map.ofEntries(",
             ",\n".join(rows) + ");", "", "    private CableTerminals() {", "    }", "",
             "    /** Return the cable end points of a block id path, or null when the block has none. */",
             "    public static double[][] of(String id) {", "        return POINTS.get(id);", "    }", "}", ""]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines), encoding="utf-8", newline="\n")

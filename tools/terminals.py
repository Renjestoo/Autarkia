"""Cable terminals of grid nodes: one table drives the model fittings (GeckoLib and block JSON) and the Java cable ends.

Points are model pixels in the node frame: origin at the bottom centre of the node (centre) block, x east, y up, z south,
block facing north; Java turns them with the horizontal facing. Each fitting is (x, y, z, style) or (x, y, z, style, side).
For "up" fittings the base point sits on a top surface; for a side (+x, -x, +z, -z) it sits on a wall and the fitting
stands out along that side. Styles:
    bushing: ceramic bushing with a copper cap; the cable ends inside the cap.
    post: a bushing on a 6 px steel post.
    rope: a horizontal grooved sheave on a short shaft; a rope ends on the rim, 3 px from the axle toward the other end,
        at groove height. On a side the sheave sits on a 4 px bracket arm out from the wall.
    rope_post: the same sheave on a 9 px gearbox post.
    end: no parts; the point is the cable end on an existing model part (pole insulators).
Each terminal has an outward horizontal direction from the footprint centre to the terminal when the block has several
terminals of its kind, else none. Java picks, among the terminals of the span's kind (sheaves for rope, the others for
metal cable), the one whose outward direction best faces the other end (ties: the nearer one). tools/geo.py adds the parts to GeckoLib block models, tools/datagen.py to block JSON models,
and tools/data.py writes src/generated/java/dev/autarkia/registry/CableTerminals.java.
"""

import math
from pathlib import Path

from art_spec import GEO_BLOCKS

ROPE = "rope"
ROPE_POST = "rope_post"
BUSHING = "bushing"
POST = "post"
END = "end"
RIM = 3.0
ARM = 4.0
GROOVE = 4.5

BUSHING_PARTS = [((3, 1, 3), 0, "steel:rivet", "steel_block"), ((2, 1, 2), 1, "ceramic:plate", "wall_insulator"),
                 ((3, 1, 3), 2, "ceramic:plate", "wall_insulator"), ((2, 1, 2), 3, "ceramic:plate", "wall_insulator"),
                 ((2, 1, 2), 4, "copper:plate", "brass_block")]
SHEAVE_PARTS = [((2, 3, 2), 0, "dark_wood:timber", "wooden_pole"),
                ((6, 1, 4), 3, "wood:timber", "rope_pulley"), ((4, 1, 6), 3, "wood:timber", "rope_pulley"),
                ((5, 1, 3), 4, "dark_wood:timber", "wooden_pole"), ((3, 1, 5), 4, "dark_wood:timber", "wooden_pole"),
                ((6, 1, 4), 5, "wood:timber", "rope_pulley"), ((4, 1, 6), 5, "wood:timber", "rope_pulley"),
                ((2, 1, 2), 6, "brass:plate", "brass_block")]
STYLES = {
    BUSHING: dict(end=4.5, parts=BUSHING_PARTS),
    POST: dict(end=10.5, parts=[((2, 6, 2), 0, "steel:rivet", "steel_block")] + [(s, y + 6, m, t) for s, y, m, t in BUSHING_PARTS]),
    ROPE: dict(end=GROOVE, parts=SHEAVE_PARTS),
    ROPE_POST: dict(end=GROOVE + 9, parts=[((2, 9, 2), 0, "dark_wood:timber", "wooden_pole")] + [(s, y + 9, m, t) for s, y, m, t in SHEAVE_PARTS]),
    END: dict(end=0.0, parts=[]),
}
RIM_STYLES = (ROPE, ROPE_POST)
SIDES = {"up": (0, 1, 0), "+x": (1, 0, 0), "-x": (-1, 0, 0), "+z": (0, 0, 1), "-z": (0, 0, -1)}
UV_ORIGIN = {"wall_insulator": (5, 4), "rope_pulley": (5, 5)}

TERMINALS = {
    "wooden_pole": [(6, 44.5, 0, END), (-6, 44.5, 0, END)],
    "steel_pylon": [(6.5, 73.5, 0, END), (-6.5, 73.5, 0, END), (6.5, 59.5, 0, END), (-6.5, 59.5, 0, END)],
    "hv_pylon": [(6.5, 94, 0, END), (-6.5, 94, 0, END), (6.5, 81, 0, END), (-6.5, 81, 0, END)],
    "cryo_node": [(6, 45, 0, BUSHING, "+x"), (-6, 45, 0, BUSHING, "-x"), (0, 45, 6, BUSHING, "+z"), (0, 45, -6, BUSHING, "-z")],
    "transformer": [(4, 16, 0, BUSHING), (-4, 16, 0, BUSHING)],
    "fuse_box": [(4, 16, 3, BUSHING), (-4, 16, 3, BUSHING)],
    "hand_crank": [(0, 16, 2, ROPE), (6.5, 10, 6.5, BUSHING)],
    "treadwheel": [(0, 22, 8, ROPE, "+z"), (0, 22, -8, ROPE, "-z"), (21, 2, 7.5, BUSHING), (-21, 2, -7.5, BUSHING)],
    "windmill": [(0, 6, 8, ROPE, "+z"), (5, 6, -8, ROPE, "-z"), (8, 6, 0, ROPE, "+x"), (-8, 6, 0, ROPE, "-x"),
                 (5, 4, 8, BUSHING, "+z"), (-5, 4, -8, BUSHING, "-z")],
    "stone_flywheel": [(0, 29, 8, ROPE, "+z"), (0, 29, -8, ROPE, "-z"), (23, 3, 7, BUSHING), (-23, 3, -7, BUSHING)],
    "stamp_mill": [(0, 32, 0, ROPE), (6.5, 3, 6.5, BUSHING), (-6.5, 3, -6.5, BUSHING)],
    "winding_post": [(0, 16, 0, ROPE), (3, 10, 0, BUSHING, "+x"), (-3, 10, 0, BUSHING, "-x")],
    "worker_assembler": [(22.5, 32, 22.5, ROPE), (-6.5, 32, -6.5, ROPE), (22.5, 32, -6.5, ROPE), (-6.5, 32, 22.5, ROPE),
                         (24, 21, 22.5, BUSHING, "+x"), (-8, 21, -6.5, BUSHING, "-x")],
    "industrial_lamp": [(4, 16, 4, BUSHING)],
    "capacitor_bank": [(4, 16, 4, BUSHING)],
    "lightning_mast": [(0, 12, 5, BUSHING, "+z"), (0, 12, -5, BUSHING, "-z")],
    "waterwheel": [(8, 23, 0, BUSHING, "+x"), (-8, 23, 0, BUSHING, "-x"), (8, 25, 0, ROPE, "+x"), (-8, 25, 0, ROPE, "-x")],
    "steam_engine": [(6.5, 3, 21, BUSHING), (-5, 3, -5, BUSHING), (6.5, 3, 8, BUSHING), (-8, 1.5, 18, BUSHING, "-x"), (6.5, 3, 15, ROPE_POST),
                     (-8, 8, 15, ROPE, "-x")],
    "blast_smelter": [(8, 26, 15.5, BUSHING), (8, 26, 0.5, BUSHING)],
    "plate_press": [(4, 16, 4, BUSHING)],
    "wire_drawer": [(4, 16, 4, BUSHING)],
    "steam_turbine": [(23, 22, 9, BUSHING, "+x"), (-23, 22, 9, BUSHING, "-x")],
    "battery_bank": [(4, 16, 4, BUSHING)],
    "jaw_crusher": [(8, 14, 22, BUSHING, "+z"), (21, 14, 8, BUSHING, "+x")],
    "magnetic_separator": [(4, 16, 4, BUSHING)],
    "electrolyzer": [(4, 16, 4, BUSHING)],
    "fabricator": [(16, 32, 4, BUSHING)],
    "warehouse_controller": [(4, 16, 4, BUSHING)],
    "capsule_station": [(5.5, 13, 5.5, BUSHING), (-5.5, 13, 5.5, BUSHING)],
    "domain_beacon": [(8, 12, 0, BUSHING, "+x"), (-8, 12, 0, BUSHING, "-x")],
    "recharge_dock": [(8, 12, 4, BUSHING, "+x"), (-8, 12, 4, BUSHING, "-x")],
    "outpost_core": [(22.5, 3, 8, BUSHING), (-6.5, 3, 8, BUSHING), (8, 3, 22.5, BUSHING), (8, 3, -6.5, BUSHING)],
    "cable_car_station": [(24, 26, 22.5, BUSHING, "+x"), (24, 26, -6.5, BUSHING, "+x"), (-8, 26, 22.5, BUSHING, "-x"),
                          (-8, 26, -6.5, BUSHING, "-x")],
    "crate_launcher": [(7, 25, -6, BUSHING), (-7, 25, -6, BUSHING)],
    "froth_flotation_plant": [(14.85, 40, 14.85, BUSHING), (-14.85, 40, 14.85, BUSHING), (14.85, 40, -14.85, BUSHING),
                              (-14.85, 40, -14.85, BUSHING)],
    "hydro_turbine": [(21, 4, 21, POST), (-21, 4, 21, POST), (21, 4, -21, POST), (-21, 4, -21, POST)],
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
    "gyro_heart": [(-34, 4, -24, BUSHING), (24, 4, -34, BUSHING), (34, 4, 24, BUSHING), (-24, 4, 34, BUSHING)],
}
PLAIN_MODELS = {"capacitor_bank": [f"capacitor_bank_c{k}" for k in range(5)], "industrial_lamp": ["industrial_lamp", "industrial_lamp_on"],
                "matter_conduit": ["matter_conduit_terminal"]}
FULL_CUBE_FACES = {
    "minecraft:block/cube_all": {f: "#all" for f in ("north", "south", "east", "west", "up", "down")},
    "minecraft:block/cube_column": {"up": "#end", "down": "#end", "north": "#side", "south": "#side", "east": "#side", "west": "#side"},
    "minecraft:block/orientable_with_bottom": {"up": "#top", "down": "#bottom", "north": "#front", "south": "#side", "east": "#side",
                                               "west": "#side"},
}


def _centre(ident: str) -> tuple[float, float]:
    """Return the horizontal footprint centre of a block in node-frame pixels."""
    if ident not in GEO_BLOCKS:
        return 0.0, 0.0
    (x0, _, z0), (x1, _, z1) = GEO_BLOCKS[ident]["footprint"]
    return (x0 + x1) / 2.0 * 16, (z0 + z1) / 2.0 * 16


def _fittings(ident: str):
    """Yield (base, style, side vector, sheave or bushing centre) of every fitting of a block."""
    for f in TERMINALS.get(ident, []):
        side = SIDES[f[4] if len(f) > 4 else "up"]
        base = (f[0], f[1], f[2])
        centre = base
        if f[3] in RIM_STYLES and side[1] == 0:
            centre = (base[0] + side[0] * ARM, base[1], base[2] + side[2] * ARM)
        yield base, f[3], side, centre


def terminals(ident: str) -> list[tuple[str, float, float, float, float, float, float]]:
    """Return (kind, x, y, z, outward x, outward z, rim radius) per terminal; kind is RIM for rope sheaves, else POINT.

    The outward direction points horizontally from the footprint centre to the terminal when the block has several
    terminals of that kind, else it is zero.
    """
    cx, cz = _centre(ident)
    raw = []
    for base, style, side, centre in _fittings(ident):
        e = STYLES[style]["end"]
        if style in RIM_STYLES:
            raw.append(("RIM", centre[0], centre[1] + e, centre[2], RIM))
        else:
            raw.append(("POINT", base[0] + side[0] * e, base[1] + side[1] * e, base[2] + side[2] * e, 0.0))
    out = []
    for kind, x, y, z, rim in raw:
        dx, dz = x - cx, z - cz
        n = math.hypot(dx, dz)
        several = sum(1 for r in raw if r[0] == kind) > 1
        ox, oz = (dx / n, dz / n) if several and n > 1e-6 else (0.0, 0.0)
        out.append((kind, x, y, z, ox, oz, rim))
    return out


def _parts(ident: str):
    """Yield (min corner, max corner, geo material, plain texture) of every terminal part in node-frame pixels."""
    for base, style, side, centre in _fittings(ident):
        if style == ROPE:
            if side[1] == 0:
                ax = 0 if side[0] else 2
                a, b = base[ax], base[ax] + side[ax] * ARM
                lo = [base[0] - 1, base[1], base[2] - 1]
                hi = [base[0] + 1, base[1] + 2, base[2] + 1]
                lo[ax], hi[ax] = min(a, b), max(a, b)
                yield lo, hi, "dark_wood:timber", "wooden_pole"
            for (w, h, dep), y0, mat, tex in STYLES[ROPE]["parts"]:
                yield ([centre[0] - w / 2.0, centre[1] + y0, centre[2] - dep / 2.0],
                       [centre[0] + w / 2.0, centre[1] + y0 + h, centre[2] + dep / 2.0], mat, tex)
            continue
        ax = next(i for i, v in enumerate(side) if v != 0)
        for (w, h, dep), y0, mat, tex in STYLES[style]["parts"]:
            size = {0: (h, dep, w), 1: (w, h, dep), 2: (w, dep, h)}[ax]
            lo, hi = [], []
            for i in range(3):
                if i == ax:
                    a, b = base[i] + side[i] * y0, base[i] + side[i] * (y0 + size[i])
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


def element(lo, hi, texture_key: str, tex: str) -> dict:
    """Return a block JSON element with an explicit uv on every face inside an opaque area of its texture."""
    u0, v0 = UV_ORIGIN.get(tex, (4, 4))
    size = [hi[i] - lo[i] for i in range(3)]
    dims = {"north": (size[0], size[1]), "south": (size[0], size[1]), "east": (size[2], size[1]), "west": (size[2], size[1]),
            "up": (size[0], size[2]), "down": (size[0], size[2])}
    return {"from": [lo[0] + 8, lo[1], lo[2] + 8], "to": [hi[0] + 8, hi[1], hi[2] + 8],
            "faces": {f: {"texture": f"#{texture_key}", "uv": [u0, v0, u0 + w, v0 + h]} for f, (w, h) in dims.items()}}


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
        elements.append(element(lo, hi, key, tex))
    obj["textures"] = textures
    obj["elements"] = elements
    return obj


def write_java(path: Path) -> None:
    """Write CableTerminals.java with the terminals of every block that has them."""
    rows = []
    for ident in TERMINALS:
        ts = ", ".join(f"new Terminal(Kind.{k}, {x:g}, {y:g}, {z:g}, {ox:.4f}, {oz:.4f}, {r:g})" for k, x, y, z, ox, oz, r in terminals(ident))
        rows.append(f'        Map.entry("{ident}", new Terminal[] {{{ts}}})')
    lines = ["package dev.autarkia.registry;", "", "import java.util.Map;", "",
             "/** Generated by tools/data.py from tools/terminals.py: cable terminals of grid nodes in pixels, node frame facing north. */",
             "public final class CableTerminals {",
             "    private static final Map<String, Terminal[]> TERMINALS = Map.ofEntries(",
             ",\n".join(rows) + ");", "",
             "    /** How a cable meets a terminal: at a fixed point, or on a sheave rim toward the other end. */",
             "    public enum Kind {", "        POINT, RIM", "    }", "",
             "    /**",
             "     * One terminal: the cable point (for RIM the sheave axle at groove height), the outward horizontal direction (zero",
             "     * when the terminal faces every side) and the rim radius in pixels (zero for POINT).",
             "     */",
             "    public record Terminal(Kind kind, double x, double y, double z, double outX, double outZ, double rim) {",
             "    }", "",
             "    private CableTerminals() {", "    }", "",
             "    /** Return the terminals of a block id path, or null when the block has none. */",
             "    public static Terminal[] of(String id) {", "        return TERMINALS.get(id);", "    }", "",
             "    /** Return true when the block has a rope sheave, so rope spans may end on it. */",
             "    public static boolean takesRope(String id) {", "        return has(id, Kind.RIM);", "    }", "",
             "    /** Return true when the block has a ceramic bushing, so metal cable spans may end on it. */",
             "    public static boolean takesMetal(String id) {", "        return has(id, Kind.POINT);", "    }", "",
             "    private static boolean has(String id, Kind kind) {", "        Terminal[] ts = TERMINALS.get(id);",
             "        if (ts == null) {", "            return false;", "        }", "        for (Terminal t : ts) {",
             "            if (t.kind() == kind) {", "                return true;", "            }", "        }",
             "        return false;", "    }", "}", ""]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines), encoding="utf-8", newline="\n")

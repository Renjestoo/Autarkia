"""Render one 3D showcase scene per tier for the CurseForge gallery (scene_N_<name>.png), with no game run.

Each scene places the tier's machines and workers from the shipped GeckoLib and block models on a grass
platform, strings sagging cables between connectors, and draws everything with one shared depth buffer
(geo_render.rasterize) at twice the output size before the downscale in curseforge_art.write.
Positions are in blocks: x to the right of the front view, z toward the back, y up; models stand on y 0.
"""

import math

import numpy as np
from PIL import Image, ImageDraw

from art_spec import RIGS, TIER_LAMP, WORKER_SHOWN, WORKER_TYPES
from curseforge_art import CUBE_MACHINES, LABEL_FONT, TEXTURES, TIER_NAMES, _hex, _rgba, pixel_text, write
from geo_core import Model, geo_paths, load_model
from geo_render import face_quads, rasterize

W, H = 1680, 960
TOP = 170
SKY_TOP, SKY_LOW = (126, 178, 240), (214, 232, 250)
CABLES = {"rope": ((121, 85, 51, 255), 5), "copper": ((214, 118, 58, 255), 4), "insulated": ((34, 34, 38, 255), 5),
          "hv": ((60, 28, 30, 255), 7), "super": ((168, 120, 255, 255), 6), "phase": ((127, 255, 240, 255), 5)}
SCENES = [
    (1, "muscle_and_timber", "Windmills, treadwheels and rope drives power the first Colony Core.", (18, 12), [
        ("geo", "colony_core", (0, 0)), ("geo", "windmill", (-6, -3)), ("geo", "treadwheel", (-6, 3)), ("geo", "stamp_mill", (-2, -4)),
        ("geo", "quern", (-2, 4)), ("geo", "hand_crank", (-3, 1)), ("geo", "stone_flywheel", (4, -4)), ("geo", "worker_assembler", (4, 3)),
        ("cube", "crucible", (7, -1)), ("cube", "crate", (7, 1)), ("cube", "crate", (7, 2)), ("cube", "tally_board", (7, 3)),
        ("worker", "porter", (2, -1)), ("worker", "porter", (-3, -1)), ("worker", "porter", (1, 5))],
     [("rope", (-6, 3.4, -3), (-2, 1.6, -4)), ("rope", (-6, 1.6, 3), (-2, 0.8, 4))]),
    (2, "steam_and_copper", "Boilers and steam engines drive a copper grid; storms charge the capacitors.", (18, 12), [
        ("geo", "boiler", (-6, -3)), ("geo", "boiler", (-6, 1)), ("geo", "steam_engine", (-3, -3)), ("geo", "steam_engine", (-3, 1)),
        ("geo", "lightning_mast", (6, -4)), ("geo", "wooden_pole", (0, -1)), ("geo", "wooden_pole", (5, 1)), ("geo", "waterwheel", (7, 4)),
        ("geo", "blast_smelter", (2, 3)), ("geo", "sluice", (-1, 4)), ("cube", "capacitor_bank", (7, -1)), ("cube", "capacitor_bank", (7, 0)),
        ("worker", "hauler_crab", (2, -3)), ("worker", "hauler_crab", (-4, 5))],
     [("copper", (-3, 2.0, -3), (0, 2.8, -1)), ("copper", (0, 2.8, -1), (5, 2.8, 1)), ("copper", (5, 2.8, 1), (7, 1.0, 0))]),
    (3, "electric_works", "Turbines, steel pylons and a warehouse; drones repair and extend the factory.", (18, 12), [
        ("geo", "steam_turbine", (-5, -2)), ("geo", "steel_pylon", (-1, -4)), ("geo", "steel_pylon", (5, -4)), ("geo", "jaw_crusher", (1, 1)),
        ("geo", "fabricator", (4, 1)), ("cube", "battery_bank", (-7, 3)), ("cube", "battery_bank", (-6, 3)), ("cube", "transformer", (7, -2)),
        ("cube", "warehouse_controller", (6, 4)), ("cube", "ledger_terminal", (7, 4)), ("cube", "electrolyzer", (-2, 4)),
        ("cube", "magnetic_separator", (-1, 4)), ("worker", "builder_drone", (-2, 0, 3.5)), ("worker", "builder_drone", (3, -2, 4.5)),
        ("worker", "loader", (3, 5)), ("worker", "lineman", (-3, 1))],
     [("insulated", (-5, 2.4, -2), (-1, 4.6, -4)), ("insulated", (-1, 4.6, -4), (5, 4.6, -4)), ("insulated", (5, 4.6, -4), (7, 1.0, -2))]),
    (4, "frontier", "Outposts grow at far deposits; HV lines, wind towers and cable cars bring the ore home.", (20, 13), [
        ("geo", "outpost_core", (-1, 0)), ("geo", "hv_pylon", (-7, -4)), ("geo", "hv_pylon", (3, -5)), ("geo", "wind_tower", (7, -4)),
        ("geo", "hydro_turbine", (-6, 3)), ("geo", "cable_car_station", (6, 2)), ("geo", "froth_flotation_plant", (2, 4)),
        ("geo", "crate_launcher", (-2, 5)), ("geo", "magma_tap", (-4, -1)), ("cube", "depot", (4, 0)),
        ("worker", "surveyor", (-3, -3, 4.0)), ("worker", "sentinel", (3, -1, 5.0)), ("worker", "cable_layer_drone", (0, -4, 6.0))],
     [("hv", (-7, 6.6, -4), (3, 6.6, -5)), ("hv", (3, 6.6, -5), (7, 7.5, -4))]),
    (5, "matter", "Plasma, superconductors and nanites: the colony takes matter apart and prints new walkers.", (20, 13), [
        ("geo", "plasma_generator", (-6, -2)), ("geo", "cryo_node", (-2, -4)), ("geo", "cryo_node", (4, -4)), ("geo", "superconducting_coil", (7, -3)),
        ("geo", "plasma_separator", (-6, 3)), ("geo", "matter_buffer", (-2, 2)), ("geo", "nanite_hive", (2, 2)), ("geo", "replicator_foundry", (6, 3)),
        ("cube", "mag_rail_station", (-8, 5)), ("cube", "mag_rail_station", (8, 5)), ("worker", "replicator_walker", (3, -1)),
        ("worker", "replicator_walker", (0, 5))],
     [("super", (-6, 2.6, -2), (-2, 2.7, -4)), ("super", (-2, 2.7, -4), (4, 2.7, -4)), ("super", (4, 2.7, -4), (7, 1.6, -3))]),
    (6, "ascendant", "The Singularity Core drinks explosions; the Forge of Ascension fits the Aegis armour.", (22, 14), [
        ("geo", "singularity_core", (-6, -2)), ("geo", "gyro_heart", (2, -3)), ("geo", "matter_compiler", (7, -4)), ("geo", "seed_matrix", (-6, 4)),
        ("geo", "restoration_projector", (-1, 3)), ("geo", "forge_of_ascension", (3, 4)), ("cube", "domain_archive", (8, 2)),
        ("cube", "matter_conduit", (8, 4)), ("cube", "phase_node", (-1, -6)), ("cube", "phase_node", (9, -1))],
     [("phase", (-1, 0.5, -6), (9, 0.5, -1))]),
]


def _rot_y(quarter: int) -> np.ndarray:
    a = math.radians(90 * quarter)
    return np.array([[math.cos(a), 0, math.sin(a)], [0, 1, 0], [-math.sin(a), 0, math.cos(a)]])


def _place(quads, atlas: np.ndarray, at, quarter: int = 0) -> list:
    """Return entries of the quads rotated about Y by quarter turns and moved to a block position (x, z[, y])."""
    x, z, y = (at[0], at[1], at[2] if len(at) > 2 else 0.0)
    r = _rot_y(quarter)
    off = np.array([x * 16.0, y * 16.0, z * 16.0])
    return [(pts @ r.T + off, uv, r @ n, atlas) for pts, uv, n in quads]


def _geo(ident: str):
    p = geo_paths("block", ident, f"block/{ident}_geo.png")
    model, uvs = load_model(p["geo"], p["anim"], ident)
    atlas = _rgba(p["tex"])
    return face_quads(model, atlas.shape, uvs), atlas


def _worker(wid: str):
    rig = WORKER_TYPES[wid][0]
    p = geo_paths("entity", rig, f"entity/{wid}.png")
    model, uvs = load_model(p["geo"], p["anim"], rig)
    spec = RIGS[rig]
    hidden = frozenset(b for b in spec["optional"] + spec["upgrades"] if b not in WORKER_SHOWN[wid])
    atlas = _rgba(p["tex"])
    return face_quads(model, atlas.shape, uvs, hidden=hidden), atlas


def _box(atlas: np.ndarray, front, side, top, bottom):
    """Return the quads of one 16-pixel box; each argument is a UV rectangle (u, v, w, h); front is the north face."""
    m = Model("box")
    m.bone("b")
    m.box("b", (-8, 0, -8), (16, 16, 16), "?:?")
    c = m.index["b"]["cubes"][-1]
    uvs = {(id(c), "north"): front, (id(c), "south"): side, (id(c), "east"): side, (id(c), "west"): side,
           (id(c), "up"): top, (id(c), "down"): bottom}
    return face_quads(m, atlas.shape, uvs)


def _cube(ident: str):
    texs = [_rgba(TEXTURES / "block" / f"{f}.png") for f in CUBE_MACHINES[ident]]
    s = texs[0].shape[0]
    atlas = np.concatenate([t[:s, :s] for t in texs], axis=1)
    return _box(atlas, (0, 0, s, s), (s, 0, s, s), (2 * s, 0, s, s), (2 * s, s, s, -s)), atlas


def _ground_atlas(seed: int) -> np.ndarray:
    """Return a 16x48 atlas of procedural grass top, grass side and dirt, own palette."""
    rng = np.random.default_rng(seed)
    grass = np.array([[86, 150, 54], [96, 162, 60], [108, 172, 66], [78, 138, 48]])
    dirt = np.array([[121, 85, 58], [134, 96, 66], [108, 76, 52], [146, 106, 72]])
    top = grass[rng.integers(0, 4, (16, 16))]
    side = dirt[rng.integers(0, 4, (16, 16))]
    side[:3] = grass[rng.integers(0, 4, (3, 16))]
    side[3, rng.random(16) < 0.5] = grass[1]
    bottom = dirt[rng.integers(0, 4, (16, 16))]
    rgb = np.concatenate([top, side, bottom], axis=1).astype(np.uint8)
    return np.dstack([rgb, np.full((16, 48), 255, np.uint8)])


def _ground(size) -> list:
    """Return entries of a one-block-thick grass platform of size (x, z) centred on the origin, top at y 0."""
    sx, sz = size
    atlases = [_ground_atlas(s) for s in range(4)]
    out = []
    for i in range(sx):
        for k in range(sz):
            atlas = atlases[(i * 7 + k * 3) % 4]
            quads = _box(atlas, (16, 0, 16, 16), (16, 0, 16, 16), (0, 0, 16, 16), (32, 0, 16, 16))
            out += _place(quads, atlas, (i - sx / 2 + 0.5, k - sz / 2 + 0.5, -1))
    return out


def _sky(seed: int) -> Image.Image:
    """Return the sky gradient with two layers of distant hill silhouettes."""
    t = np.linspace(0.0, 1.0, H)[:, None]
    rows = np.array(SKY_TOP)[None, :] * (1 - t) + np.array(SKY_LOW)[None, :] * t
    img = Image.fromarray(np.repeat(rows[:, None, :], W, axis=1).astype(np.uint8)).convert("RGBA")
    d = ImageDraw.Draw(img)
    rng = np.random.default_rng(seed)
    for base, color, amp in ((H * 0.45, (150, 182, 168, 255), 60), (H * 0.55, (112, 156, 120, 255), 45)):
        ph = rng.random(3) * 6.28
        xs = np.arange(0, W + 8, 8)
        ys = base - amp * (0.5 * np.sin(xs / 210 + ph[0]) + 0.3 * np.sin(xs / 97 + ph[1]) + 0.2 * np.sin(xs / 41 + ph[2]))
        ys = (ys // 8) * 8
        d.polygon([(0, H)] + [(int(x), int(y)) for x, y in zip(xs, ys)] + [(W, H)], fill=color)
    return img


def _cable(d: ImageDraw.ImageDraw, project, kind: str, a, b) -> None:
    """Draw a sagging cable from block point a to block point b (x, y, z) in screen space."""
    color, width = CABLES[kind]
    pa, pb = np.array([a[0], a[1], a[2]], float) * 16, np.array([b[0], b[1], b[2]], float) * 16
    span = np.linalg.norm(pb - pa)
    sag = 0.0 if kind == "phase" else span * 0.06
    pts = [pa + (pb - pa) * s - np.array([0, sag * 4 * s * (1 - s), 0]) for s in np.linspace(0, 1, 32)]
    scr = [tuple(p) for p in project(np.array(pts))]
    if kind == "phase":
        d.line(scr, fill=(255, 255, 255, 255), width=width // 2)
        d.line(scr, fill=color, width=width + 4)
        d.line(scr, fill=(255, 255, 255, 255), width=2)
    else:
        d.line(scr, fill=color, width=width)


def render_scene(tier: int, name: str, caption: str, size, items, cables) -> Image.Image:
    """Return the full-size scene image for one tier."""
    print(f"[scene] tier {tier}: {len(items)} models")
    cache: dict = {}
    entries = _ground(size)
    for kind, ident, at in items:
        if (kind, ident) not in cache:
            cache[(kind, ident)] = {"geo": _geo, "worker": _worker, "cube": _cube}[kind](ident)
        quads, atlas = cache[(kind, ident)]
        entries += _place(quads, atlas, at)
    fit = np.concatenate([e[0] for e in entries])
    art, project = rasterize(entries, (W, H - TOP), 135, 30, fit, margin=36)
    img = _sky(tier)
    img.alpha_composite(Image.fromarray(art), (0, TOP))
    d = ImageDraw.Draw(img)

    def shifted(points):
        p = project(points)
        p[:, 1] += TOP
        return p

    for kind, a, b in cables:
        _cable(d, shifted, kind, a, b)
    accent = _hex(TIER_LAMP[tier])
    title = pixel_text(f"TIER {tier}: {TIER_NAMES[tier]}", 3, accent)
    band = Image.new("RGBA", (max(title.width, int(d.textlength(caption, font=LABEL_FONT) * 1.6)) + 48, title.height + 64), (16, 17, 22, 200))
    band.alpha_composite(title, (24, 14))
    bd = ImageDraw.Draw(band)
    bd.text((26, 22 + title.height), caption, font=LABEL_FONT.font_variant(size=24), fill=(236, 236, 240, 255))
    bd.rectangle((0, 0, 5, band.height), fill=accent)
    img.alpha_composite(band, (30, 24))
    return img


def make_scenes() -> list[str]:
    """Render and write every tier scene; return the file names."""
    names = []
    for tier, name, caption, size, items, cables in SCENES:
        fname = f"scene_{tier}_{name}.png"
        write(render_scene(tier, name, caption, size, items, cables), fname)
        names.append(fname)
    return names


if __name__ == "__main__":
    print("[scene] rendering tier scenes")
    make_scenes()
    print("[scene] done")

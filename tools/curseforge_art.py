"""Render the CurseForge project art into curseforge/images/ from the shipped assets (no game run).

Usage: F:/envs/5070_11/python.exe tools/curseforge_art.py
Writes: logo, banner, one machine panel per tier (tier_N.png), an all-machines collage, workers, item panels,
armour sets, one rendered 3D scene per tier, and curseforge/description.html (plus a text-only copy) and the
repository README.md from curseforge/description.md.
Every image is at most MAX_WIDTH pixels wide: CurseForge refuses images 850 pixels wide or wider.
"""

import json
import re
import sys
from pathlib import Path

import markdown
import numpy as np
from PIL import Image, ImageDraw, ImageFont

from art_spec import ARMOR_ASSETS, EQUIP_SPRITES, GEO_BLOCKS, ITEM_SPRITES, RIGS, TIER_LAMP, WORKER_SHOWN, WORKER_TYPES
from contact_sheet import mannequin
from geo_core import Model, geo_paths, load_model
from geo_render import face_quads, render
from paint import ASSETS, ROOT, TEXTURES, upscale

OUT = ROOT / "curseforge" / "images"
MAX_WIDTH = 840
FONTS = Path("C:/Windows/Fonts")
TITLE_FONT = ImageFont.truetype(str(FONTS / "impact.ttf"), 18)
LABEL_FONT = ImageFont.truetype(str(FONTS / "bahnschrift.ttf"), 15)
SMALL_FONT = ImageFont.truetype(str(FONTS / "bahnschrift.ttf"), 13)
BG_TOP, BG_BOTTOM = (18, 19, 25), (30, 32, 43)
PANEL = (35, 37, 48, 255)
SLOT = (24, 25, 33, 255)
TEXT = (232, 232, 236, 255)
TIER_NAMES = {1: "MUSCLE AND TIMBER", 2: "STEAM AND COPPER", 3: "ELECTRIC WORKS", 4: "FRONTIER", 5: "MATTER", 6: "ASCENDANT"}
TIER_VERBS = {1: "Hit", 2: "Cook", 3: "Separate", 4: "Carry", 5: "Take apart", 6: "Write"}
SET_TIER = {"bronze": 1, "steel_rig": 2, "exo_rig": 3, "frontier_suit": 4, "lattice_carapace": 5, "aegis": 6}
SET_ITEM_PREFIX = {"steel_rig": "steel"}
PALETTE_TIER = {"bronze": 1, "wood": 1, "stone": 1, "brass": 2, "copper": 2, "brick": 2, "steel": 3, "electric": 3,
                "verdigris": 2, "rubber": 3, "electric_accent": 3, "vantite": 4, "vantite_steel": 4, "lattice": 5, "frost": 5,
                "nanite": 5, "aegis": 6}
CUBE_MACHINES = {
    "crate": ("crate_side", "crate_side", "crate_top"), "tally_board": ("tally_board_front", "tally_board_side", "tally_board_side"),
    "project_board": ("project_board_front", "project_board_side", "project_board_side"),
    "crucible": ("crucible_side", "crucible_side", "crucible_top"), "capacitor_bank": ("capacitor_bank_front", "capacitor_bank_side", "capacitor_bank_side"),
    "transformer": ("transformer_side", "transformer_side", "transformer_top"), "fuse_box": ("fuse_box_front", "fuse_box_side", "fuse_box_side"),
    "warehouse_controller": ("warehouse_controller_front", "warehouse_controller_side", "warehouse_controller_side"),
    "ledger_terminal": ("ledger_terminal_front", "ledger_terminal_side", "ledger_terminal_side"),
    "depot": ("depot_side", "depot_side", "depot_top"),
    "matter_conduit": ("matter_conduit", "matter_conduit", "matter_conduit"), "phase_node": ("phase_node", "phase_node", "phase_node"),
}
CORE_PARTS = ["tier_2", "tier_3", "tier_3_ring", "tier_4", "tier_5", "tier_6"]
CORE_ORBS = ["tier_3_orb", "tier_5_orb", "tier_6_orb"]
MATERIAL_RE = re.compile(r"^(raw_|crushed_|washed_|pure_)|(_ingot|_nugget|_concentrate)$|^(lattice_shard|lattice_crystal|aegis_plate|slag)$")
COLONY_ITEMS = ["wrench", "wire_cutter", "multimeter", "survey_lens", "planning_tablet", "field_manual", "core_seed", "core_upgrade_t2",
                "core_upgrade_t3", "core_upgrade_t4", "core_upgrade_t5", "core_upgrade_t6", "pattern_card", "pattern_card_recorded",
                "outpost_seed", "worker_capsule", "worker_capsule_filled", "autarky_seed"]
ARMOR_PIECES = ("helmet", "chest", "legs", "boots")


def _rgba(path: Path) -> np.ndarray:
    with Image.open(path) as im:
        return np.array(im.convert("RGBA"))


def _img(a: np.ndarray) -> Image.Image:
    return Image.fromarray(np.ascontiguousarray(a))


def _names() -> dict[str, str]:
    lang = json.loads((ASSETS / "lang" / "en_us.json").read_text(encoding="utf-8"))
    out = {}
    for key, value in lang.items():
        parts = key.split(".")
        if len(parts) == 3 and parts[1] == "autarkia" and parts[0] in ("block", "item", "entity"):
            out.setdefault(parts[2], value)
    return out


def _tiers() -> dict[str, int]:
    src = (ROOT / "src" / "main" / "java" / "dev" / "autarkia" / "registry" / "AutarkiaItems.java").read_text(encoding="utf-8")
    out = {m.group(2): int(m.group(1)) for m in re.finditer(r'(?:item|simple)\((\d),\s*"(\w+)"', src)}
    out.update({m.group(2).lower(): int(m.group(1)) for m in re.finditer(r"block\((\d),\s*AutarkiaBlocks\.(\w+)\)", src)})
    return out


def _tier_of(ident: str, tiers: dict[str, int]) -> int:
    if ident in tiers:
        return tiers[ident]
    for prefix, t in SET_TIER.items():
        if ident.startswith(prefix + "_"):
            return t
    return PALETTE_TIER.get(ITEM_SPRITES.get(ident, ("bronze",))[0], 1)


def _hex(c: str) -> tuple[int, int, int, int]:
    return int(c[1:3], 16), int(c[3:5], 16), int(c[5:7], 16), 255


def _shade(c, k: float) -> tuple[int, int, int, int]:
    return int(c[0] * k), int(c[1] * k), int(c[2] * k), 255


def pixel_text(text: str, scale: int, color, shadow=(0, 0, 0, 255)) -> Image.Image:
    """Return text drawn without anti-aliasing and enlarged by scale, with a one-pixel drop shadow."""
    probe = ImageDraw.Draw(Image.new("L", (1, 1)))
    x0, y0, x1, y1 = probe.textbbox((0, 0), text, font=TITLE_FONT)
    mask = Image.new("L", (x1 - x0 + 2, y1 - y0 + 2), 0)
    d = ImageDraw.Draw(mask)
    d.fontmode = "1"
    d.text((-x0, -y0), text, fill=255, font=TITLE_FONT)
    m = np.array(mask) > 0
    out = np.zeros((m.shape[0] + 1, m.shape[1] + 1, 4), dtype=np.uint8)
    out[1:, 1:][m] = shadow
    out[:-1, :-1][m] = color
    return _img(upscale(out, scale))


def _background(w: int, h: int) -> Image.Image:
    t = np.linspace(0.0, 1.0, h)[:, None]
    rows = np.array(BG_TOP)[None, :] * (1 - t) + np.array(BG_BOTTOM)[None, :] * t
    rgb = np.repeat(rows[:, None, :], w, axis=1).astype(np.uint8)
    return _img(np.dstack([rgb, np.full((h, w), 255, np.uint8)]))


def _label(img: Image.Image, text: str, box_w: int, y: int, font=LABEL_FONT, fill=TEXT) -> None:
    d = ImageDraw.Draw(img)
    words, lines, cur = text.split(), [], ""
    for w in words:
        trial = (cur + " " + w).strip()
        if d.textlength(trial, font=font) > box_w - 8 and cur:
            lines.append(cur)
            cur = w
        else:
            cur = trial
    lines.append(cur)
    for i, ln in enumerate(lines[:2]):
        d.text(((box_w - d.textlength(ln, font=font)) / 2, y + i * (font.size + 3)), ln, font=font, fill=fill)


def tile(art: np.ndarray, caption: str, w: int, h: int, accent) -> Image.Image:
    """Return a slot-style tile with the art centred and the caption underneath."""
    t = Image.new("RGBA", (w, h), SLOT)
    d = ImageDraw.Draw(t)
    d.rectangle((0, 0, w - 1, h - 1), outline=_shade(accent, 0.55), width=2)
    a = _img(art)
    room = h - (40 if caption else 8)
    if a.width > w - 12 or a.height > room:
        k = min((w - 12) / a.width, room / a.height)
        a = a.resize((max(1, int(a.width * k)), max(1, int(a.height * k))), Image.Resampling.NEAREST)
    t.alpha_composite(a, ((w - a.width) // 2, 4 + (room - a.height) // 2))
    if caption:
        _label(t, caption, w, h - 38)
    return t


def panel(title: str, subtitle: str, accent, tiles: list[Image.Image], cols: int, width: int) -> Image.Image:
    """Return a framed panel: a header band with the title and a grid of tiles in the tier colour."""
    gap = 10
    tw = tiles[0].width if tiles else 0
    rows = (len(tiles) + cols - 1) // cols
    head = 92 if subtitle else 70
    h = head + rows * (tiles[0].height + gap) + gap if tiles else head
    p = Image.new("RGBA", (width, h), PANEL)
    d = ImageDraw.Draw(p)
    d.rectangle((0, 0, width - 1, head - 1), fill=_shade(accent, 0.30))
    d.rectangle((0, 0, width - 1, h - 1), outline=accent, width=4)
    d.line((4, head - 1, width - 5, head - 1), fill=accent, width=2)
    t = pixel_text(title, 3, accent)
    p.alpha_composite(t, ((width - t.width) // 2, 12))
    if subtitle:
        d.text(((width - d.textlength(subtitle, font=LABEL_FONT)) / 2, 12 + t.height + 6), subtitle, font=LABEL_FONT, fill=TEXT)
    x0 = (width - (cols * tw + (cols - 1) * gap)) // 2
    for i, tl in enumerate(tiles):
        r, c = divmod(i, cols)
        p.alpha_composite(tl, (x0 + c * (tw + gap), head + gap + r * (tl.height + gap)))
    return p


def stack(parts: list[Image.Image], pad: int = 24) -> Image.Image:
    """Return the parts stacked vertically on the page background."""
    w = max(pt.width for pt in parts) + 2 * pad
    h = sum(pt.height for pt in parts) + pad * (len(parts) + 1)
    page = _background(w, h)
    y = pad
    for pt in parts:
        page.alpha_composite(pt, ((w - pt.width) // 2, y))
        y += pt.height + pad
    return page


def tier_parts(model) -> set[str]:
    """Return the tier_* bones of a model; hiding them shows its tier-1 look (the Colony Core)."""
    return {b["name"] for b in model.bones if b["name"].startswith("tier_")}


def render_geo_block(ident: str, size: int) -> np.ndarray:
    """Return an isometric render of a GeckoLib machine model at its tier-1 look."""
    p = geo_paths("block", ident, f"block/{ident}_geo.png")
    model, uvs = load_model(p["geo"], p["anim"], ident)
    return render(model, _rgba(p["tex"]), uvs, size, 135, 30, hidden=tier_parts(model))


def render_core_tier(tier: int, size: int) -> np.ndarray:
    """Return the Colony Core as ColonyCoreRenderer shows it at a colony tier (active pose, petals open)."""
    p = geo_paths("block", "colony_core", "block/colony_core_geo.png")
    model, uvs = load_model(p["geo"], p["anim"], "colony_core")
    orb = 6 if tier >= 6 else 5 if tier == 5 else 3 if tier >= 3 else 1
    hidden = {b for b in CORE_PARTS if int(b[5]) > tier} | {b for b in CORE_ORBS if int(b[5]) != orb}
    if orb > 1:
        model.index["core_orb"]["cubes"] = []
    return render(model, _rgba(p["tex"]), uvs, size, 150, 25, pose=model.anims["active"].sample(0.0), hidden=hidden)


def _variant_model(ident: str) -> str | None:
    """Return the model of the north-facing (else first) blockstate variant, or None for a multipart block."""
    bs = json.loads((ASSETS / "blockstates" / f"{ident}.json").read_text(encoding="utf-8"))
    variants = bs.get("variants")
    if not variants:
        return None
    v = variants[next((k for k in variants if "facing=north" in k), next(iter(variants)))]
    return (v[0] if isinstance(v, list) else v)["model"]


def block_model(ident: str):
    """Return (model, uvs, atlas) built from a block's JSON element model, or None when its model chain has no elements.

    Raises:
        ValueError: an element is rotated, a face uv is rotated, or a face uses a texture outside Autarkia; these are
            not drawn by this renderer.
    """
    name, textures, elements = _variant_model(ident), {}, None
    while name and name.startswith("autarkia:"):
        obj = json.loads((ASSETS / "models" / (name.split(":", 1)[1] + ".json")).read_text(encoding="utf-8"))
        textures = {**obj.get("textures", {}), **textures}
        if elements is None and "elements" in obj:
            elements = obj["elements"]
        name = obj.get("parent", "")
    if not elements:
        return None

    def resolve(ref: str) -> str:
        for _ in range(8):
            if not ref.startswith("#"):
                break
            ref = textures[ref[1:]]
        ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
        if ns != "autarkia":
            raise ValueError(f"{ident}: face texture {ref} is not an Autarkia texture")
        return path

    paths = sorted({resolve(f["texture"]) for e in elements for f in e["faces"].values()})
    texs = [_rgba(TEXTURES / f"{p}.png") for p in paths]
    s = max(t.shape[1] for t in texs)
    tiles = [np.array(_img(t[:t.shape[1], :t.shape[1]]).resize((s, s), Image.Resampling.NEAREST)) for t in texs]
    atlas = np.concatenate([*tiles, np.zeros((s, s, 4), dtype=np.uint8)], axis=1)
    k, blank = s / 16.0, (len(paths) * s, 0, s, s)
    m = Model(ident)
    m.bone("b")
    uvs = {}
    for e in elements:
        if "rotation" in e:
            raise ValueError(f"{ident}: rotated elements are not drawn")
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        m.box("b", (8 - x1, y0, z0 - 8), (x1 - x0, y1 - y0, z1 - z0), "?:?")
        c = m.index["b"]["cubes"][-1]
        default = {"north": (16 - x1, 16 - y1, 16 - x0, 16 - y0), "south": (x0, 16 - y1, x1, 16 - y0),
                   "east": (16 - z1, 16 - y1, 16 - z0, 16 - y0), "west": (z0, 16 - y1, z1, 16 - y0),
                   "up": (x0, z0, x1, z1), "down": (x0, 16 - z1, x1, 16 - z0)}
        for face in default:
            f = e["faces"].get(face)
            if f is None:
                uvs[(id(c), face)] = blank
                continue
            if f.get("rotation"):
                raise ValueError(f"{ident}: rotated face uvs are not drawn")
            u0, v0, u1, v1 = f.get("uv", default[face])
            off = paths.index(resolve(f["texture"])) * s
            if face in ("up", "down"):
                uvs[(id(c), face)] = (off + u1 * k, v1 * k, (u0 - u1) * k, (v0 - v1) * k)
            else:
                uvs[(id(c), face)] = (off + u0 * k, v0 * k, (u1 - u0) * k, (v1 - v0) * k)
    return m, uvs, atlas


def render_cube(faces: tuple[str, str, str], size: int) -> np.ndarray:
    """Return an isometric render of a full block whose front, side and top textures are given."""
    texs = [_rgba(TEXTURES / "block" / f"{f}.png") for f in faces]
    s = texs[0].shape[1]
    atlas = np.concatenate([t[:s, :s] for t in texs], axis=1)
    m = Model("cube")
    m.bone("c")
    m.box("c", (-8, 0, -8), (16, 16, 16), "?:?")
    c = m.index["c"]["cubes"][-1]
    uvs = {(id(c), "north"): (0, 0, s, s), (id(c), "south"): (s, 0, s, s), (id(c), "east"): (s, 0, s, s),
           (id(c), "west"): (s, 0, s, s), (id(c), "up"): (2 * s, 0, s, s), (id(c), "down"): (2 * s, s, s, -s)}
    return render(m, atlas, uvs, size, 135, 30)


def render_worker(wid: str, size: int) -> np.ndarray:
    """Return a render of a worker on its rig with only its own optional bones shown."""
    rig = WORKER_TYPES[wid][0]
    p = geo_paths("entity", rig, f"entity/{wid}.png")
    model, uvs = load_model(p["geo"], p["anim"], rig)
    spec = RIGS[rig]
    hidden = frozenset(b for b in spec["optional"] + spec["upgrades"] if b not in WORKER_SHOWN[wid])
    return render(model, _rgba(p["tex"]), uvs, size, 150, 20, hidden=hidden)


def render_armor_set(asset: str, w: int, h: int) -> np.ndarray:
    """Return the flat armour layers of a set on an invisible mannequin, front three-quarter view."""
    eq = TEXTURES / "entity" / "equipment"
    body, legs = _rgba(eq / "humanoid" / f"{asset}.png"), _rgba(eq / "humanoid_leggings" / f"{asset}.png")
    mb, ub = mannequin(1.0)
    ml, ul = mannequin(0.5)
    fit = face_quads(mb, body.shape, ub)
    yaw, pitch = 155, 10
    layers = [render(ml, legs, ul, (w, h), yaw, pitch, hidden={"head", "right_arm", "left_arm"}, fit_quads=fit),
              render(mb, body, ub, (w, h), yaw, pitch, hidden={"head", "body", "right_arm", "left_arm"}, fit_quads=fit),
              render(mb, body, ub, (w, h), yaw, pitch, hidden={"right_leg", "left_leg"}, fit_quads=fit)]
    out = np.zeros((h, w, 4), dtype=np.uint8)
    for layer in layers:
        m = layer[..., 3] > 0
        out[m] = layer[m]
    return out


def item_icon(ident: str, scale: int) -> np.ndarray | None:
    path = TEXTURES / "item" / f"{ident}.png"
    if not path.exists():
        print(f"[art] no item texture for {ident}")
        return None
    return upscale(_rgba(path), scale)


def machines(names: dict[str, str], tiers: dict[str, int]) -> dict[int, list[tuple[str, str]]]:
    """Return per tier the machines as (id, kind) with kind 'geo' or 'cube'."""
    out: dict[int, list[tuple[str, str]]] = {t: [] for t in range(1, 7)}
    for ident, spec in GEO_BLOCKS.items():
        out[spec["tier"]].append((ident, "geo"))
    for ident in CUBE_MACHINES:
        if ident not in names:
            print(f"[art] cube machine {ident} has no language name; skipped")
            continue
        out[_tier_of(ident, tiers)].append((ident, "cube"))
    return out


def machine_art(ident: str, kind: str, size: int) -> np.ndarray:
    if kind == "geo":
        return render_geo_block(ident, size)
    built = block_model(ident)
    if built is None:
        return render_cube(CUBE_MACHINES[ident], size)
    model, uvs, atlas = built
    return render(model, atlas, uvs, size, 135, 30)


def write(img: Image.Image, name: str) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    img = img.convert("RGB")
    if img.width > MAX_WIDTH:
        img = img.resize((MAX_WIDTH, round(img.height * MAX_WIDTH / img.width)), Image.Resampling.LANCZOS)
    img.save(OUT / name, optimize=True)
    print(f"[art] wrote {name} ({img.width}x{img.height})")


def make_logo() -> None:
    logo = _rgba(ROOT / "src" / "main" / "resources" / "autarkia_logo.png")
    k = max(1, 512 // logo.shape[0])
    write(_img(upscale(logo, k)), "logo.png")


def make_tier_panels(per_tier, names) -> dict[int, Image.Image]:
    """Write tier_N.png (machines of each tier) and return the panels."""
    panels = {}
    for t, items in per_tier.items():
        accent = _hex(TIER_LAMP[t])
        print(f"[art] tier {t}: {len(items)} machines")
        cols = 6 if len(items) <= 12 else 7
        tw = 180 if cols == 6 else 156
        tiles = [tile(machine_art(i, k, tw - 12), names.get(i, i), tw, 214, accent) for i, k in items]
        p = panel(f"TIER {t}: {TIER_NAMES[t]}", f"Matter verb: {TIER_VERBS[t]}. {len(items)} machines.", accent, tiles, cols, 1180)
        panels[t] = p
        write(stack([p]), f"tier_{t}.png")
    return panels


def make_all_machines(per_tier, names) -> None:
    """Write machines_all.png: every machine of every tier in one collage."""
    parts = []
    for t, items in per_tier.items():
        accent = _hex(TIER_LAMP[t])
        tiles = [tile(machine_art(i, k, 112), names.get(i, i), 124, 160, accent) for i, k in items]
        parts.append(panel(f"TIER {t}: {TIER_NAMES[t]}", "", accent, tiles, 10, 1340))
    write(stack(parts), "machines_all.png")


def make_core_tiers() -> None:
    """Write core_tiers.png: the Colony Core at each of the six colony tiers."""
    accent = _hex("#FFB347")
    tiles = [tile(render_core_tier(t, 340), f"Tier {t}", 370, 400, _hex(TIER_LAMP[t])) for t in range(1, 7)]
    write(stack([panel("THE CORE GROWS WITH YOUR COLONY", "Every tier adds parts to the Colony Core, and its orb changes colour.",
                       accent, tiles, 3, 1180)]), "core_tiers.png")


def make_workers(names) -> None:
    accent = _hex("#FFB347")
    tiles = [tile(render_worker(w, 210), names.get(w, w), 230, 270, _hex(WORKER_TYPES[w][3])) for w in WORKER_TYPES]
    write(stack([panel("THE WORKERS", "Walkers, crabs and drones that carry real items and build the colony.", accent, tiles, 5, 1240)]),
          "workers.png")


def make_items(tiers) -> None:
    """Write items_materials.png, items_components.png and items_equipment.png."""
    def ordered(ids):
        return sorted(ids, key=lambda i: (_tier_of(i, tiers), i))

    materials = ordered(i for i in ITEM_SPRITES if MATERIAL_RE.search(i))
    equipment = ordered(i for i in EQUIP_SPRITES if not i.endswith(tuple("_" + p for p in ARMOR_PIECES)))
    components = ordered(i for i in ITEM_SPRITES if i not in materials and i not in EQUIP_SPRITES and i not in COLONY_ITEMS)
    groups = [("items_materials.png", "MATERIALS", "Ores, ingots, processing stages and endgame matter.", materials, "#C9A227", 5, 14),
              ("items_components.png", "COMPONENTS AND COLONY ITEMS", "Parts, cables, worker frames, seeds, upgrades and tools.",
               components + ordered(COLONY_ITEMS), "#4FE3FF", 5, 12),
              ("items_equipment.png", "TOOLS AND WEAPONS", "Six tiers of tools and weapons, from bronze to Aegis.", equipment, "#FF5CF0", 6, 10)]
    for name, title, sub, ids, color, scale, cols in groups:
        accent = _hex(color)
        tiles = []
        for i in ids:
            icon = item_icon(i, scale)
            if icon is not None:
                tiles.append(tile(icon, "", 16 * scale + 16, 16 * scale + 16, _hex(TIER_LAMP[_tier_of(i, tiers)])))
        write(stack([panel(title, sub, accent, tiles, cols, cols * (16 * scale + 26) + 40)]), name)


def make_armor(names) -> None:
    tiles = []
    for asset in ARMOR_ASSETS:
        t = SET_TIER[asset]
        accent = _hex(TIER_LAMP[t])
        body = render_armor_set(asset, 200, 300)
        prefix = SET_ITEM_PREFIX.get(asset, asset)
        icons = [item_icon(f"{prefix}_{p}", 3) for p in ARMOR_PIECES]
        strip = np.zeros((48, 4 * 52, 4), dtype=np.uint8)
        for k, ic in enumerate(icons):
            if ic is not None:
                strip[:48, k * 52:k * 52 + 48] = ic[:48, :48]
        art = np.zeros((300 + 56, 210, 4), dtype=np.uint8)
        art[:300, 5:205] = body
        art[308:356, 1:209] = strip[:, :208]
        label = names.get(f"{prefix}_chest", asset).replace(" Chestplate", "").replace(" Chest", "")
        tiles.append(tile(art, f"T{t}: {label}", 224, 410, accent))
    write(stack([panel("ARMOUR SETS", "Every set keeps the functions of the sets before it.", _hex("#7FFFF0"), tiles, 6, 1460)]), "armor.png")


def make_banner(names) -> None:
    """Write banner.png: title, tagline, the logo and one showpiece machine per tier."""
    w, h = 1600, 520
    page = _background(w, h)
    logo = _img(upscale(_rgba(ROOT / "src" / "main" / "resources" / "autarkia_logo.png"), 1)).resize((220, 220), Image.Resampling.NEAREST)
    page.alpha_composite(logo, (60, 40))
    title = pixel_text("AUTARKIA", 8, _hex("#F5D76E"))
    page.alpha_composite(title, (310, 52))
    d = ImageDraw.Draw(page)
    big = ImageFont.truetype(str(FONTS / "bahnschrift.ttf"), 34)
    d.text((316, 60 + title.height + 10), "A factory that builds itself.", font=big, fill=TEXT)
    d.text((316, 60 + title.height + 56), "Six physical tech tiers. A Colony Core that plans, builds, repairs and expands.", font=LABEL_FONT,
           fill=(190, 190, 200, 255))
    show = [("windmill", 1), ("boiler", 2), ("steam_turbine", 3), ("cable_car_station", 4), ("replicator_foundry", 5), ("singularity_core", 6)]
    x = 60
    for ident, t in show:
        art = _img(render_geo_block(ident, 200))
        page.alpha_composite(art, (x, h - 215))
        d.line((x + 20, h - 10, x + 180, h - 10), fill=_hex(TIER_LAMP[t]), width=4)
        x += 250
    write(page, "banner.png")


README_TAIL = """
---

## Building

Requires JDK 25. Run `gradlew build`; the jar is written to `build/libs/`.

## License

[MIT](LICENSE). Add-ons, bridges and compatibility mods are welcome.
"""


def make_descriptions() -> None:
    """Write description.html, description_text_only.md/.html (no image lines) and the repository README.md."""
    folder = ROOT / "curseforge"
    md = (folder / "description.md").read_text(encoding="utf-8")
    (folder / "description.html").write_text(markdown.markdown(md, extensions=["extra"]) + "\n", encoding="utf-8")
    text = re.sub(r"\n{3,}", "\n\n", "\n".join(ln for ln in md.splitlines() if not ln.startswith("![")).strip()) + "\n"
    (folder / "description_text_only.md").write_text(text, encoding="utf-8")
    (folder / "description_text_only.html").write_text(markdown.markdown(text, extensions=["extra"]) + "\n", encoding="utf-8")
    readme = re.sub(r"^(!\[[^\]]*\]\()", r"\1curseforge/images/", md, flags=re.M).rstrip() + "\n" + README_TAIL
    (ROOT / "README.md").write_text(readme, encoding="utf-8")
    print("[art] wrote description.html, the text-only description and README.md")


def main() -> int:
    """Render every CurseForge image and the HTML description."""
    print("[art] phase 1: names and tiers")
    names, tiers = _names(), _tiers()
    per_tier = machines(names, tiers)
    print("[art] phase 2: logo and banner")
    make_logo()
    make_banner(names)
    print("[art] phase 3: machine panels")
    make_tier_panels(per_tier, names)
    make_all_machines(per_tier, names)
    print("[art] phase 4: Colony Core tiers, workers, items, armour")
    make_core_tiers()
    make_workers(names)
    make_items(tiers)
    make_armor(names)
    print("[art] phase 5: tier scenes and description")
    from curseforge_scenes import make_scenes

    make_scenes()
    make_descriptions()
    print("[art] done")
    return 0


if __name__ == "__main__":
    sys.exit(main())

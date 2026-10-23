"""Generate every texture listed in tools/art_spec.py, then validate sizes and coverage.

Usage: F:/envs/5070_11/python.exe tools/textures.py [section ...] [name-filter ...]
Sections: blocks, particles, items, other, armor, logo, rigs, icons (default: all). Any other argument
is a substring filter on asset names (rigs match rig or worker id); without filters everything is
painted. Exits 1 on any missing or malformed output. Output is byte-identical between runs.
"""

import importlib
import json
import re
import sys
from collections import Counter
from pathlib import Path

import numpy as np
from PIL import Image
from tqdm import tqdm

from art_spec import (ARMOR_ASSETS, BLOCK_TEXTURES, GEO_BLOCKS, ITEM_SPRITES, JSON_BIG, LOGO, OTHER_TEXTURES,
                      PARTICLE_TEXTURES, RIGS, WORKER_TYPES)
from geo_core import build_atlas, builder, collect_faces, footprint_px, mat_parts
from geo_faces import paint_face
from geo_render import render
from paint import ROOT, TEXTURES, rng_for, save_png

LOGO_PATH = ROOT / "src" / "main" / "resources" / "autarkia_logo.png"
ITEM_MODULES = ["paint_items", "paint_tools", "paint_gadgets", "paint_armor", "paint_icons", "paint_tools_t4"]
NON_BODY = {"glass", "glow", "water", "energy", "grate", "lamp", "screen", "crystal", "roundel", "spoked", "cog", "pane",
            "glint"}
EQUIPMENT = TEXTURES / "entity" / "equipment"

Written = list[tuple[Path, tuple[int, int] | None]]


def _registry(modules: list[str], attr: str) -> dict:
    """Merge the painter dicts `attr` of several modules, failing on duplicate keys."""
    out = {}
    for name in modules:
        part = getattr(importlib.import_module(name), attr)
        clash = set(out) & set(part)
        if clash:
            raise ValueError(f"{name}.{attr} redefines {sorted(clash)}")
        out.update(part)
    return out


def _paint(registry: dict, style: str, name: str, pal: str, kind: str) -> np.ndarray:
    if style not in registry:
        raise KeyError(f"{kind} '{name}': no painter for style '{style}'")
    img = registry[style](name, pal, rng_for(kind, name))
    if img.dtype != np.uint8 or img.ndim != 3 or img.shape[2] != 4:
        raise ValueError(f"{kind} '{name}': painter '{style}' returned {img.dtype} {img.shape}, expected RGBA uint8")
    return img


def main_material(model) -> str:
    """Return the body material with the largest rendered face area of a model."""
    area = Counter()
    for _b, _c, _f, mat, w, h in collect_faces(model):
        if mat_parts(mat)[1] not in NON_BODY:
            area[mat] += w * h
    if not area:
        raise ValueError(f"{model.ident}: no opaque body material to derive a particle texture from")
    return max(sorted(area), key=lambda m: area[m])


def section_blocks(names: list[str]) -> Written:
    """Paint BLOCK_TEXTURES into textures/block; animated styles (paint_blocks.FRAMES) become a vertical frame strip plus .mcmeta."""
    styles = _registry(["paint_blocks"], "STYLES")
    frames = importlib.import_module("paint_blocks").FRAMES
    out = []
    for name in tqdm(names, desc="blocks", unit="tex"):
        pal, style, _desc = BLOCK_TEXTURES[name]
        path = TEXTURES / "block" / f"{name}.png"
        meta = path.with_name(path.name + ".mcmeta")
        if style not in frames:
            meta.unlink(missing_ok=True)
            out.append((save_png(_paint(styles, style, name, pal, "block"), path), (16, 16)))
            continue
        count, ticks = frames[style]
        strip = np.concatenate([styles[style](name, pal, rng_for("block", name), phase=i / count) for i in range(count)], axis=0)
        meta.write_text(json.dumps({"animation": {"frametime": ticks, "interpolate": True}}, indent=2) + "\n", encoding="utf-8")
        out.append((save_png(strip, path), (16, 16 * count)))
    return out


def section_particles(names: list[str]) -> Written:
    """Paint each geo block's particle texture from the model's main body material (JSON_BIG blocks: their net)."""
    out = []
    for name in tqdm(names, desc="particles", unit="tex"):
        if name in JSON_BIG:
            mat = f"{JSON_BIG[name]['palette']}:grate"
        else:
            mat = main_material(builder("block", name)())
        img = paint_face(mat, "side", 16, 16, rng_for("particle", name))
        out.append((save_png(img, TEXTURES / "block" / f"{name}.png"), (16, 16)))
    return out


def section_items(names: list[str]) -> Written:
    """Paint ITEM_SPRITES into textures/item."""
    shapes = _registry(ITEM_MODULES, "SHAPES")
    out = []
    for name in tqdm(names, desc="items", unit="sprite"):
        pal, shape, _desc = ITEM_SPRITES[name]
        out.append((save_png(_paint(shapes, shape, name, pal, "item"), TEXTURES / "item" / f"{name}.png"), (16, 16)))
    return out


def section_other(names: list[str]) -> Written:
    """Paint OTHER_TEXTURES into textures/<key>; a 'WxH' in the description fixes the size (default 16x16)."""
    styles = _registry(["paint_misc", "paint_power_t5"], "OTHER")
    out = []
    for key in tqdm(names, desc="other", unit="tex"):
        pal, style, desc = OTHER_TEXTURES[key]
        m = re.search(r"(\d+)x(\d+)", desc)
        size = (int(m.group(1)), int(m.group(2))) if m else (16, 16)
        out.append((save_png(_paint(styles, style, key, pal, "other"), TEXTURES / key), size))
    return out


def section_armor(names: list[str]) -> Written:
    """Paint the flat humanoid and humanoid_leggings layers of every armour asset."""
    armor = importlib.import_module("paint_armor")
    out = []
    for asset in tqdm(names, desc="armor", unit="set"):
        body, legs = armor.armor_layers(asset, ARMOR_ASSETS[asset])
        out.append((save_png(body, EQUIPMENT / "humanoid" / f"{asset}.png"), (64, 32)))
        out.append((save_png(legs, EQUIPMENT / "humanoid_leggings" / f"{asset}.png"), (64, 32)))
    return out


def section_logo(names: list[str]) -> Written:
    """Paint the 256x256 mod logo."""
    misc = importlib.import_module("paint_misc")
    print(f"[textures] logo ({LOGO[0]}, {LOGO[1]})")
    return [(save_png(misc.logo(), LOGO_PATH), (256, 256))] if names else []


def section_rigs(names: list[str]) -> Written:
    """Write rig templates, masks and per-worker recolours with glowmasks."""
    rigs = importlib.import_module("geo_rigs")
    out = []
    for rig in tqdm(names, desc="rigs", unit="rig"):
        atlas, mask, _uvs = rigs.rig_atlas(builder("rig", rig)())
        out += [(p, (atlas.shape[1], atlas.shape[0])) for p in rigs.write_rig_textures(rig, atlas, mask)]
    return out


def icon_fit(footprint) -> list | None:
    """Return a fit region for icons of tall blocks (at least 4 blocks high and 4x taller than wide).

    The icon then frames the top 3 blocks of the footprint column, where the identifying head detail
    (rotor, arms, insulators, spike) sits; everything else is cropped by the canvas. None keeps the full fit.
    """
    w, h, d = footprint_px(footprint)
    if h < 64 or h < 4 * max(w, d):
        return None
    xs, ys, zs = (-w / 2.0, w / 2.0), (h - 48.0, h), (-d / 2.0, d / 2.0)
    corners = np.array([(x, y, z) for x in xs for y in ys for z in zs])
    return [(corners, None, None)]


def section_icons(names: list[str]) -> Written:
    """Render a 32x32 isometric inventory icon for every geo block from its model and atlas."""
    out = []
    for name in tqdm(names, desc="icons", unit="icon"):
        model = builder("block", name)()
        atlas, uvs = build_atlas(model, paint_face)
        img = render(model, atlas, uvs, 32, fit_quads=icon_fit(GEO_BLOCKS[name]["footprint"]))
        out.append((save_png(img, TEXTURES / "item" / f"{name}.png"), (32, 32)))
    return out


SECTIONS = {"blocks": (section_blocks, list(BLOCK_TEXTURES)), "particles": (section_particles, PARTICLE_TEXTURES),
            "items": (section_items, list(ITEM_SPRITES)), "other": (section_other, list(OTHER_TEXTURES)),
            "armor": (section_armor, list(ARMOR_ASSETS)), "logo": (section_logo, ["logo"]),
            "rigs": (section_rigs, list(RIGS)), "icons": (section_icons, list(GEO_BLOCKS))}


def select(section: str, filters: list[str]) -> list[str]:
    """Return the section's names matching any filter (all names when there are no filters)."""
    names = SECTIONS[section][1]
    if not filters:
        return list(names)
    if section == "rigs":
        return [r for r in names if any(f in r or any(f in w and WORKER_TYPES[w][0] == r for w in WORKER_TYPES) for f in filters)]
    return [n for n in names if any(f in n for f in filters)]


def expected(section: str, names: list[str]) -> list[Path]:
    """Return every path the spec requires for the given names of a section."""
    t = TEXTURES
    if section == "blocks" or section == "particles":
        return [t / "block" / f"{n}.png" for n in names]
    if section == "items":
        return [t / "item" / f"{n}.png" for n in names]
    if section == "other":
        return [t / k for k in names]
    if section == "armor":
        return [EQUIPMENT / d / f"{a}.png" for a in names for d in ("humanoid", "humanoid_leggings")]
    if section == "logo":
        return [LOGO_PATH] if names else []
    if section == "rigs":
        return ([t / "entity" / "template" / f"{r}{s}.png" for r in names for s in ("", "_mask")]
                + [t / "entity" / f"{w}{s}.png" for w, spec in WORKER_TYPES.items() if spec[0] in names for s in ("", "_glowmask")])
    return [t / "item" / f"{n}.png" for n in names]


def validate(written: Written, required: list[Path]) -> list[str]:
    """Check every written file's mode and size and that every required file was written."""
    errs = []
    for path, size in written:
        with Image.open(path) as im:
            if im.mode != "RGBA":
                errs.append(f"{path}: mode {im.mode}, expected RGBA")
            if size is not None and im.size != size:
                errs.append(f"{path}: size {im.size}, expected {size}")
            if im.getextrema()[3][1] == 0:
                errs.append(f"{path}: fully transparent")
    done = {p for p, _ in written}
    errs += [f"{p}: required by art_spec but not generated" for p in required if p not in done]
    return errs


def main(argv: list[str]) -> int:
    """Run the selected (default: all) sections, optionally filtered by name, and validate the results."""
    sections = [a for a in argv if a in SECTIONS] or list(SECTIONS)
    filters = [a for a in argv if a not in SECTIONS]
    written, required = [], []
    for s in sections:
        names = select(s, filters)
        print(f"[textures] section {s}: {len(names)} assets")
        part = SECTIONS[s][0](names)
        written += part
        required += expected(s, names)
        print(f"[textures] section {s}: {len(part)} files written")
    errs = validate(written, required)
    if errs:
        print(f"[textures] FAILED with {len(errs)} violation(s):")
        for e in errs:
            print("  -", e)
        return 1
    print(f"[textures] ok: {len(written)} files written and validated")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

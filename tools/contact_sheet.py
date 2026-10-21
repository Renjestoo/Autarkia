"""Render review sheets of the generated art into screenshots/art/ (reads the shipped files from disk).

Usage: F:/envs/5070_11/python.exe tools/contact_sheet.py <section> [filter ...]
Sections: blocks, items, icons, geo, rigs, armor, misc, all. Filters keep ids containing any given
substring and add a suffix to the sheet name, so several reviewers can render subsets in parallel.
"""

import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

from art_spec import (ARMOR_ASSETS, ARMOR_BONES, BLOCK_TEXTURES, GEO_ARMOR, GEO_BLOCKS, GEO_ENTITIES, GEO_ITEMS,
                      ITEM_SPRITES, OTHER_TEXTURES, PARTICLE_TEXTURES, RIGS, WORKER_SHOWN, WORKER_TYPES)
from geo_core import Model, geo_paths, load_model
from geo_render import face_quads, render
from paint import ROOT, TEXTURES, upscale

OUT = ROOT / "screenshots" / "art"
BG = (46, 48, 58, 255)
SLOT = (139, 139, 139, 255)
FONT = ImageFont.load_default(size=13)
MAX_W, MAX_H = 1800, 1500


def _rgba(path: Path) -> np.ndarray:
    with Image.open(path) as im:
        return np.array(im.convert("RGBA"))


def _tile(img: np.ndarray, label: str, bg=BG) -> Image.Image:
    """Return img on a background with a caption underneath."""
    h, w = img.shape[:2]
    tw = max(w, int(FONT.getlength(label)) + 4)
    t = Image.new("RGBA", (tw + 8, h + 26), bg)
    t.alpha_composite(Image.fromarray(np.ascontiguousarray(img)), (4 + (tw - w) // 2, 4))
    ImageDraw.Draw(t).text((4, h + 8), label, fill=(230, 230, 230, 255), font=FONT)
    return t


def _row(tiles: list[Image.Image], label: str = "") -> Image.Image:
    """Lay tiles out left to right, wrapping at MAX_W, with an optional row title."""
    lines, cur, x = [], [], 0
    for t in tiles:
        if cur and x + t.width > MAX_W:
            lines.append(cur)
            cur, x = [], 0
        cur.append(t)
        x += t.width
    if cur:
        lines.append(cur)
    head = 20 if label else 0
    h = head + sum(max(t.height for t in ln) for ln in lines)
    w = max(sum(t.width for t in ln) for ln in lines)
    img = Image.new("RGBA", (w, h), BG)
    if label:
        ImageDraw.Draw(img).text((4, 3), label, fill=(255, 220, 120, 255), font=FONT)
    y = head
    for ln in lines:
        x = 0
        for t in ln:
            img.alpha_composite(t, (x, y))
            x += t.width
        y += max(t.height for t in ln)
    return img


def _pages(rows: list[Image.Image], name: str) -> list[Path]:
    """Stack rows into pages no taller than MAX_H and save them; return the paths."""
    OUT.mkdir(parents=True, exist_ok=True)
    pages, cur, h = [], [], 0
    for r in rows:
        if cur and h + r.height > MAX_H:
            pages.append(cur)
            cur, h = [], 0
        cur.append(r)
        h += r.height
    if cur:
        pages.append(cur)
    paths = []
    for i, pg in enumerate(pages):
        img = Image.new("RGBA", (max(r.width for r in pg), sum(r.height for r in pg)), BG)
        y = 0
        for r in pg:
            img.alpha_composite(r, (0, y))
            y += r.height
        p = OUT / (f"{name}.png" if len(pages) == 1 else f"{name}_{i + 1}.png")
        img.save(p)
        paths.append(p)
    return paths


def _keep(ident: str, filters: list[str]) -> bool:
    return not filters or any(f in ident for f in filters)


def _flat_grid(names: list[str], folder: str, scale: int, tiled: bool, slot: bool) -> list[Image.Image]:
    tiles = []
    for n in names:
        img = _rgba(TEXTURES / folder / f"{n}.png")
        big = upscale(img, scale)
        if slot:
            pad = np.zeros((big.shape[0] + 8, big.shape[1] + 8, 4), dtype=np.uint8)
            pad[:] = SLOT
            m = big[..., 3] > 0
            pad[4:-4, 4:-4][m] = big[m]
            big = pad
        if tiled:
            t3 = upscale(np.tile(img, (3, 3, 1)), 2)
            canvas = np.zeros((max(big.shape[0], t3.shape[0]), big.shape[1] + 6 + t3.shape[1], 4), dtype=np.uint8)
            canvas[:big.shape[0], :big.shape[1]] = big
            canvas[:t3.shape[0], big.shape[1] + 6:] = t3
            big = canvas
        tiles.append(_tile(big, n))
    return tiles


def sheet_blocks(filters: list[str]) -> list[Path]:
    """Block textures at 6x with a 3x3 tiling preview, then geo particle textures."""
    names = [n for n in BLOCK_TEXTURES if _keep(n, filters)]
    parts = [n for n in PARTICLE_TEXTURES if _keep(n, filters)]
    rows = [_row(_flat_grid(names, "block", 6, True, False), "block textures")] if names else []
    if parts:
        rows.append(_row(_flat_grid(parts, "block", 6, True, False), "geo particle textures"))
    return _pages(rows, "blocks" + _suffix(filters))


def sheet_items(filters: list[str]) -> list[Path]:
    """Item sprites at 6x on an inventory-slot background, plus a 1x strip."""
    names = [n for n in ITEM_SPRITES if _keep(n, filters)]
    rows = [_row(_flat_grid(names, "item", 6, False, True), "item sprites (6x)")]
    strip = [_tile(upscale(_rgba(TEXTURES / "item" / f"{n}.png"), 2), "") for n in names]
    rows.append(_row(strip, "2x strip"))
    return _pages(rows, "items" + _suffix(filters))


def sheet_icons(filters: list[str]) -> list[Path]:
    """Geo block inventory icons (32x32) at 4x on slot backgrounds and at 1x."""
    names = [n for n in GEO_BLOCKS if _keep(n, filters)]
    rows = [_row(_flat_grid(names, "item", 4, False, True), "geo block icons (4x)")]
    rows.append(_row([_tile(_rgba(TEXTURES / "item" / f"{n}.png"), "") for n in names], "1x"))
    return _pages(rows, "icons" + _suffix(filters))


def _model_row(model: Model, uvs: dict, atlas: np.ndarray, label: str, hidden=frozenset(), size: int = 220) -> Image.Image:
    """Return a review row: front/back/side/top views, animation frames and the atlas."""
    fit = face_quads(model, atlas.shape, uvs, None, hidden)
    tiles = [_tile(render(model, atlas, uvs, size, yaw, pitch, hidden=hidden, fit_quads=fit), cap)
             for yaw, pitch, cap in ((135, 30, "front iso"), (315, 30, "back iso"), (180, 0, "front"), (90, 0, "side"))]
    for name, a in model.anims.items():
        for k in range(4):
            t = a.length * k / 4.0
            pose = a.sample(t)
            tiles.append(_tile(render(model, atlas, uvs, size // 2, 135, 30, pose, hidden, fit_quads=fit), f"{name} {t:.2f}s"))
    sc = max(1, min(4, 220 // max(atlas.shape[:2])))
    tiles.append(_tile(upscale(atlas, sc), f"atlas {atlas.shape[1]}x{atlas.shape[0]}"))
    return _row(tiles, label)


def sheet_geo(filters: list[str]) -> list[Path]:
    """One review row per geo block, entity, item and armor model."""
    rows = []
    groups = [("block", GEO_BLOCKS, "block/{}_geo.png"), ("entity", GEO_ENTITIES, "entity/{}.png"),
              ("item", GEO_ITEMS, "item/{}_geo.png"), ("armor", GEO_ARMOR, "armor/{}.png")]
    for group, spec, tex in groups:
        for ident in spec:
            if not _keep(ident, filters):
                continue
            print(f"[sheet] geo {group}/{ident}")
            p = geo_paths(group, ident, tex.format(ident))
            model, uvs = load_model(p["geo"], p["anim"], ident)
            rows.append(_model_row(model, uvs, _rgba(p["tex"]), f"{group}/{ident}"))
    return _pages(rows, "geo" + _suffix(filters))


def sheet_rigs(filters: list[str]) -> list[Path]:
    """Each worker type on its rig with only its optional bones shown, plus the rig templates."""
    rows = []
    for wid, (rig, *_c) in WORKER_TYPES.items():
        if not (_keep(wid, filters) or _keep(rig, filters)):
            continue
        print(f"[sheet] rig {wid} ({rig})")
        p = geo_paths("entity", rig, f"entity/{wid}.png")
        model, uvs = load_model(p["geo"], p["anim"], rig)
        spec = RIGS[rig]
        hidden = frozenset(b for b in spec["optional"] + spec["upgrades"] if b not in WORKER_SHOWN[wid])
        rows.append(_model_row(model, uvs, _rgba(p["tex"]), f"{wid} ({rig})", hidden))
    for rig, spec in RIGS.items():
        if not _keep(rig, filters):
            continue
        p = geo_paths("entity", rig, f"entity/template/{rig}.png")
        model, uvs = load_model(p["geo"], p["anim"], rig)
        tiles = [_tile(render(model, _rgba(p["tex"]), uvs, 220, y, 30), f"all bones {y}") for y in (135, 315)]
        for suffix in ("", "_mask"):
            img = _rgba(TEXTURES / "entity" / "template" / f"{rig}{suffix}.png")
            tiles.append(_tile(upscale(img, max(1, 256 // img.shape[1])), f"template{suffix}"))
        rows.append(_row(tiles, f"rig {rig} (template, every bone visible)"))
    return _pages(rows, "rigs" + _suffix(filters))


def mannequin(inflate: float) -> tuple[Model, dict]:
    """Return a vanilla humanoid box-UV model (64x32 layout) and its face UV map."""
    m = Model("mannequin")
    boxes = [("head", (-4, 24, -4), (8, 8, 8), (0, 0)), ("body", (-4, 12, -2), (8, 12, 4), (16, 16)),
             ("right_arm", (-8, 12, -2), (4, 12, 4), (40, 16)), ("left_arm", (4, 12, -2), (4, 12, 4), (40, 16)),
             ("right_leg", (-4, 0, -2), (4, 12, 4), (0, 16)), ("left_leg", (0, 0, -2), (4, 12, 4), (0, 16))]
    uvs = {}
    for name, o, s, (u, v) in boxes:
        m.bone(name)
        m.box(name, o, s, "?:?", inflate=inflate)
        c = m.index[name]["cubes"][-1]
        x, y, z = s
        box = {"west": (u + z + x, v + z, z, y), "east": (u, v + z, z, y), "north": (u + z, v + z, x, y),
               "south": (u + 2 * z + x, v + z, x, y), "up": (u + z, v, x, z), "down": (u + z + x, v + z, x, -z)}
        for f, uv in box.items():
            uvs[(id(c), f)] = uv
    return m, uvs


def sheet_armor(filters: list[str]) -> list[Path]:
    """Flat armour layers at 6x and on a box-UV mannequin, front and back."""
    rows = []
    for asset in ARMOR_ASSETS:
        if not _keep(asset, filters):
            continue
        print(f"[sheet] armour {asset}")
        eq = TEXTURES / "entity" / "equipment"
        body, legs = _rgba(eq / "humanoid" / f"{asset}.png"), _rgba(eq / "humanoid_leggings" / f"{asset}.png")
        tiles = [_tile(upscale(body, 5), "humanoid"), _tile(upscale(legs, 5), "humanoid_leggings")]
        mb, ub = mannequin(1.0)
        ml, ul = mannequin(0.5)
        for yaw in (135, 315, 180):
            a = render(mb, body, ub, 200, yaw, 15, hidden={"right_leg", "left_leg"})
            b = render(ml, legs, ul, 200, yaw, 15, hidden={"head", "right_arm", "left_arm"})
            boots = render(mb, body, ub, 200, yaw, 15, hidden={"head", "body", "right_arm", "left_arm"})
            tiles += [_tile(a, f"body {yaw}"), _tile(b, f"legs {yaw}"), _tile(boots, f"boots {yaw}")]
        rows.append(_row(tiles, f"flat armour {asset}"))
    for ident in GEO_ARMOR:
        if _keep(ident, filters):
            p = geo_paths("armor", ident, f"armor/{ident}.png")
            model, uvs = load_model(p["geo"], p["anim"], ident)
            rows.append(_model_row(model, uvs, _rgba(p["tex"]), f"geo armour {ident} ({', '.join(ARMOR_BONES)})"))
    return _pages(rows, "armor" + _suffix(filters))


def sheet_misc(filters: list[str]) -> list[Path]:
    """OTHER_TEXTURES at 6x with tiling preview, and the logo."""
    tiles = []
    for key in OTHER_TEXTURES:
        if _keep(key, filters):
            img = _rgba(TEXTURES / key)
            tiles.append(_tile(upscale(img, 6), key))
            tiles.append(_tile(upscale(np.tile(img, (2, 3, 1)), 3), "tiled"))
    rows = [_row(tiles, "other textures")]
    logo = ROOT / "src" / "main" / "resources" / "autarkia_logo.png"
    if logo.exists():
        rows.append(_row([_tile(_rgba(logo), "logo 1x"), _tile(upscale(_rgba(logo), 2)[:400, :400], "logo 2x crop")], "logo"))
    return _pages(rows, "misc" + _suffix(filters))


def _suffix(filters: list[str]) -> str:
    return ("_" + "_".join(filters)) if filters else ""


SHEETS = {"blocks": sheet_blocks, "items": sheet_items, "icons": sheet_icons, "geo": sheet_geo, "rigs": sheet_rigs,
          "armor": sheet_armor, "misc": sheet_misc}


def main(argv: list[str]) -> int:
    """Render the requested sheet section(s) and print the written paths."""
    if not argv or (argv[0] not in SHEETS and argv[0] != "all"):
        print(f"usage: contact_sheet.py <{'|'.join(SHEETS)}|all> [filter ...]")
        return 1
    names = list(SHEETS) if argv[0] == "all" else [argv[0]]
    for n in names:
        print(f"[sheet] {n}: rendering")
        for p in SHEETS[n](argv[1:]):
            print(f"[sheet] wrote {p}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

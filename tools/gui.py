"""Generate menu background textures, the GUI widget atlas and GuiLayouts.java from one layout table.

Every menu is a 176-wide panel with the player inventory at the bottom. The panel wears its machine's material
(THEMES: the face finish its block model uses) as a frame and, lightened, as its surface; slots are vanilla inset
wells, the energy bar, progress arrow and flame are drawn empty here and filled by the screen from
textures/gui/widgets.png. The code-drawn screens with light text (colony, multimeter, sites) get dark nine-slice
panel sprites in textures/gui/sprites (DARK_PANELS).

Usage: gui.py
"""

import json
from pathlib import Path

import numpy as np
from PIL import Image

import art_spec
from geo_faces import paint_face
from paint import rng_for

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src" / "generated" / "resources" / "assets" / "autarkia" / "textures" / "gui"
JAVA = ROOT / "src" / "generated" / "java" / "dev" / "autarkia" / "registry" / "GuiLayouts.java"

WOOD, BRICK, BRASS, STEEL, VANTITE, LATTICE, AEGIS = ("wood:timber", "canvas"), ("brick:brick", "ceramic"), ("brass:panel", "canvas"), \
    ("steel:rivet", "ceramic"), ("vantite_steel:panel", "ceramic"), ("lattice:plate", "ceramic"), ("aegis_body:seam", "ceramic")
THEMES = {
    "stamp_mill": WOOD, "crucible": BRICK, "crate": ("wood:wood", "canvas"), "worker_assembler": WOOD, "boiler": BRICK,
    "energy_storage": BRASS, "sluice": ("wood:wood", "canvas"), "blast_smelter": BRICK, "plate_press": BRASS, "wire_drawer": BRASS,
    "belt_splitter": ("rubber:rubber", "ceramic"), "sorting_chute": ("wood:wood", "canvas"), "rack_2": WOOD, "pattern_loom": WOOD,
    "fuse_box": STEEL, "steam_turbine": STEEL, "jaw_crusher": STEEL, "magnetic_separator": STEEL, "electrolyzer": STEEL,
    "fabricator": STEEL, "rack_3": STEEL, "warehouse_controller": STEEL, "outpost_core": VANTITE, "capsule_station": BRASS,
    "ledger_terminal": STEEL, "cable_car_station": VANTITE, "crate_launcher": VANTITE, "depot": VANTITE, "replicator_foundry": LATTICE,
    "froth_flotation_plant": VANTITE, "plasma_separator": LATTICE, "cryo_node": LATTICE, "superconducting_coil": LATTICE,
    "plasma_generator": LATTICE, "rack_4": LATTICE, "matter_buffer": LATTICE, "mag_rail_station": LATTICE, "nanite_hive": LATTICE,
    "singularity_core": AEGIS, "matter_compiler": AEGIS, "matter_conduit": AEGIS, "domain_archive": AEGIS,
    "forge_of_ascension": AEGIS, "seed_matrix": AEGIS, "restoration_projector": AEGIS,
}
"""Per layout: (frame material as its block model wears it, inner fill palette)."""

SPRITE_W, SPRITE_H, SPRITE_BORDER = 336, 400, 6
DARK_PANELS = {"colony_panel": ("wood:timber", (36, 28, 20)), "multimeter_panel": ("brass:panel", (22, 25, 28)),
               "sites_panel": ("vantite_steel:panel", (18, 26, 30))}
"""Nine-slice GUI sprites for the code-drawn screens with light text: (frame material, dark inner RGB tone)."""

LAYOUTS = {
    "stamp_mill": dict(tier=1, height=166, inputs=[(56, 35)], outputs=[(116, 35), (134, 35)], energy=(8, 18), arrow=(79, 35), data=5),
    "crucible": dict(tier=1, height=166, inputs=[(38, 17), (38, 35)], outputs=[(116, 35)], arrow=(79, 35), flame=(40, 55), data=6),
    "crate": dict(tier=1, height=166, grid=(8, 18, 9, 3), data=0),
    "worker_assembler": dict(tier=1, height=166, inputs=[(26, 35), (56, 26), (74, 26), (92, 26), (56, 44), (74, 44), (92, 44)], outputs=[],
                             energy=(8, 18), arrow=(120, 35), figure=(146, 29), data=5),
    "boiler": dict(tier=2, height=166, inputs=[(80, 54), (8, 18)], outputs=[(8, 54)], flame=(62, 56), bars=[(28, 18), (46, 18)], data=7),
    "energy_storage": dict(tier=2, height=166, inputs=[(152, 35)], outputs=[], energy=(8, 18), data=6),
    "sluice": dict(tier=2, height=166, inputs=[(56, 35)], outputs=[(116, 35), (134, 35)], arrow=(79, 35), data=6),
    "blast_smelter": dict(tier=2, height=166, inputs=[(38, 17), (38, 35), (38, 53)], outputs=[(116, 35), (134, 35)], energy=(8, 18),
                          arrow=(79, 35), heat=(24, 18), data=6),
    "plate_press": dict(tier=2, height=166, inputs=[(56, 35)], outputs=[(116, 35)], energy=(8, 18), arrow=(79, 35), data=5),
    "wire_drawer": dict(tier=2, height=166, inputs=[(38, 35), (56, 35)], outputs=[(116, 35)], energy=(8, 18), arrow=(79, 35), data=5),
    "belt_splitter": dict(tier=2, height=166, grid=(98, 17, 3, 3), data=5),
    "sorting_chute": dict(tier=2, height=166, grid=(98, 17, 3, 3), data=5),
    "rack_2": dict(tier=2, height=216, inputs=[(8, 18), (48, 18)], grid=(8, 60, 9, 3), data=6),
    "pattern_loom": dict(tier=2, height=184, inputs=[(8, 57)], data=9),
    "fuse_box": dict(tier=3, height=166, data=4),
    "steam_turbine": dict(tier=3, height=166, energy=(8, 18), data=7),
    "jaw_crusher": dict(tier=3, height=166, inputs=[(56, 35)], outputs=[(116, 35), (134, 35)], energy=(8, 18), arrow=(79, 35), data=5),
    "magnetic_separator": dict(tier=3, height=166, inputs=[(56, 35)], outputs=[(116, 35), (134, 35), (152, 35)], energy=(8, 18), arrow=(79, 35),
                               data=5),
    "electrolyzer": dict(tier=3, height=166, inputs=[(38, 35), (56, 35)], outputs=[(116, 35), (134, 35)], energy=(8, 18), arrow=(79, 35), data=5),
    "fabricator": dict(tier=3, height=166, inputs=[(30 + 18 * c, 17 + 18 * r) for r in range(3) for c in range(3)], outputs=[(124, 55)],
                       energy=(8, 18), arrow=(94, 35), data=6),
    "rack_3": dict(tier=3, height=216, inputs=[(8, 18), (48, 18), (88, 18)], grid=(8, 60, 9, 3), data=6),
    "warehouse_controller": dict(tier=3, height=166, data=6),
    "outpost_core": dict(tier=4, height=176, data=7),
    "capsule_station": dict(tier=3, height=230, inputs=[(7 + 18 * c, 44 + 18 * r) for r in range(3) for c in range(3)],
                            outputs=[(63 + 18 * c, 44 + 18 * r) for r in range(3) for c in range(3)], grid=(119, 44, 3, 3), data=8),
    "ledger_terminal": dict(tier=3, width=264, height=240, inputs=[(8 + 18 * c, 124) for c in range(9)] + [(178, 124)],
                            outputs=[(8 + 18 * c, 88 + 18 * r) for r in range(2) for c in range(9)], wells=[(7, 29, 164, 38), (175, 15, 82, 105)],
                            data=0),
    "cable_car_station": dict(tier=4, height=232, inputs=[(8 + 18 * c, 31 + 18 * r) for r in range(3) for c in range(9)] + [(8, 94), (26, 94)],
                              outputs=[(8 + 18 * c, 31 + 18 * r) for r in range(3) for c in range(9)], wells=[(110, 92, 60, 44)], data=9),
    "crate_launcher": dict(tier=4, height=232, inputs=[(8 + 18 * c, 18 + 18 * r) for r in range(3) for c in range(9)] + [(8, 78)], data=9),
    "depot": dict(tier=4, height=236, grid=(8, 32, 9, 6), data=0),
    "replicator_foundry": dict(tier=5, height=166, inputs=[(26, 35), (56, 26), (74, 26), (92, 26), (56, 44), (74, 44), (92, 44)], outputs=[],
                              energy=(8, 18), arrow=(120, 35), figure=(146, 29), data=7),
    "froth_flotation_plant": dict(tier=4, height=166, inputs=[(56, 35), (26, 53)], outputs=[(98, 35), (116, 35), (134, 35)], energy=(8, 18),
                                  arrow=(73, 35), wells=[(151, 17, 18, 62)], data=6),
    "plasma_separator": dict(tier=5, height=166, inputs=[(38, 26), (38, 44)], outputs=[(116, 26), (134, 26), (116, 44)], energy=(8, 18),
                             arrow=(79, 35), data=5),
    "cryo_node": dict(tier=5, height=166, inputs=[(8, 18), (26, 18), (44, 18), (62, 18)], bars=[(152, 18)], data=8),
    "superconducting_coil": dict(tier=5, height=166, inputs=[(8, 18), (26, 18), (44, 18), (62, 18)], energy=(152, 18), bars=[(134, 18)],
                                 data=9),
    "plasma_generator": dict(tier=5, height=166, inputs=[(30, 18)], energy=(8, 18), wells=[(29, 61, 140, 8)], data=8),
    "rack_4": dict(tier=5, height=216, inputs=[(8, 18), (48, 18), (88, 18), (128, 18)], grid=(8, 60, 9, 3), data=6),
    "matter_buffer": dict(tier=5, width=220, height=222, wells=[(7, 37, 202, 82)], data=0),
    "mag_rail_station": dict(tier=5, width=236, height=234, inputs=[(8 + 18 * c, 34 + 18 * r) for r in range(3) for c in range(9)] + [(8, 90)],
                             outputs=[(8 + 18 * c, 34 + 18 * r) for r in range(3) for c in range(9)], grid=(176, 34, 3, 3),
                             wells=[(27, 93, 66, 7)], data=8),
    "nanite_hive": dict(tier=5, width=272, height=240, inputs=[(246, 76)], wells=[(7, 17, 234, 82)], energy=(248, 18), data=68),
    "singularity_core": dict(tier=6, height=180, energy=(8, 18), wells=[(25, 79, 144, 8)], data=11),
    "matter_compiler": dict(tier=6, height=166, inputs=[(38, 35)], outputs=[(116, 26), (134, 26), (116, 44), (134, 44)], energy=(8, 18),
                            arrow=(79, 35), heat=(24, 18), wells=[(78, 16, 18, 18)], data=7),
    "matter_conduit": dict(tier=6, height=166, grid=(26, 17, 3, 3), energy=(8, 18), data=10),
    "domain_archive": dict(tier=6, width=264, height=240, grid=(8, 88, 9, 3), wells=[(7, 29, 128, 38), (139, 15, 119, 71)], energy=(244, 88),
                           data=8),
    "forge_of_ascension": dict(tier=6, height=220, inputs=[(8 + 18 * c, 18 + 18 * r) for r in range(3) for c in range(9)], wells=[(7, 76, 162, 48)],
                               data=6),
    "seed_matrix": dict(tier=6, width=304, height=220, inputs=[(8 + 146 * (i // 4), 26 + 18 * (i % 4)) for i in range(8)], outputs=[(282, 100)],
                        wells=[(7, 120, 262, 5)],
                        data=36),
    "restoration_projector": dict(tier=6, height=186, energy=(156, 18), data=13),
}


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


def rect(img, x, y, w, h, c):
    for i in range(x, x + w):
        for j in range(y, y + h):
            img.putpixel((i, j), c)


def _scale(px: np.ndarray, f: float) -> np.ndarray:
    out = px.astype(np.float64)
    out[..., :3] = np.clip(out[..., :3] * f, 0, 255)
    return out.astype(np.uint8)


def panel(name, w, h):
    """Return the menu panel of a layout: the machine's own material (as its block model wears it) lightened towards
    the theme's fill colour, so the dark vanilla label text stays readable (see framed)."""
    frame_mat, fill = THEMES[name]
    return framed(name, frame_mat, hexc(art_spec.PALETTES[fill][3])[:3], w, h)


def framed(key, frame_mat, tone, w, h):
    """Return a panel: a 5px frame in a material finish with an outline, a lit top-left and shaded bottom-right bevel
    and corner rivets, around the same material pulled towards the RGB tone at low contrast."""
    frame = paint_face(frame_mat, "side", w, h, rng_for("gui", key))
    inner = paint_face(frame_mat.replace(":seam", ":plate"), "side", w, h, rng_for("gui inner", key)).astype(np.float64)
    light = np.array(tone, dtype=np.float64)
    luma = inner[..., :3] @ np.array([0.299, 0.587, 0.114])
    detail = (luma - luma.mean())[..., None] * (0.3 if light.mean() > 128 else 0.14)
    img = np.zeros((h, w, 4), dtype=np.uint8)
    img[..., :3] = np.clip(light * 0.8 + inner[..., :3] * 0.2 + detail, 0, 255)
    img[..., 3] = 255
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.minimum(np.minimum(xx, yy), np.minimum(w - 1 - xx, h - 1 - yy))
    lit = (xx + yy) < (w + h) / 2
    band = d <= 4
    img[band] = frame[band]
    img[d == 0] = _scale(frame[d == 0], 0.42)
    for k, up, down in ((1, 1.25, 0.72), (4, 0.8, 1.1)):
        m = d == k
        img[m & lit] = _scale(img[m & lit], up)
        img[m & ~lit] = _scale(img[m & ~lit], down)
    m = d == 5
    img[m & lit] = _scale(img[m & lit], 0.8)
    img[m & ~lit] = _scale(img[m & ~lit], 1.06)
    for cx, cy in ((2, 2), (w - 4, 2), (2, h - 4), (w - 4, h - 4)):
        img[cy, cx] = _scale(frame[cy, cx], 1.45)
        img[cy, cx + 1] = img[cy + 1, cx] = _scale(frame[cy, cx], 1.1)
        img[cy + 1, cx + 1] = _scale(frame[cy, cx], 0.55)
    for x, y in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        img[y, x] = (0, 0, 0, 0)
    return Image.fromarray(img)


def well(img, x, y, w, h):
    dark = (55, 55, 55, 255)
    light = (255, 255, 255, 255)
    mid = (139, 139, 139, 255)
    rect(img, x, y, w, h, mid)
    rect(img, x, y, w - 1, 1, dark)
    rect(img, x, y, 1, h - 1, dark)
    rect(img, x + 1, y + h - 1, w - 1, 1, light)
    rect(img, x + w - 1, y + 1, 1, h - 1, light)


def slot(img, x, y):
    well(img, x - 1, y - 1, 18, 18)


def arrow(img, x, y, color):
    for i in range(24):
        for j in range(17):
            head = i >= 14 and abs(j - 8) <= 8 - (i - 14) * 0.8
            shaft = i < 15 and 5 <= j <= 11
            if head or shaft:
                img.putpixel((x + i, y + j), color)


def flame(img, x, y, colors, lit):
    shape = ["......#.......", ".....##.......", ".....###......", "....####.#....", "....#######...", "...########...",
             "..##########..", "..##########..", ".############.", ".############.", ".############.", "..##########..",
             "...########...", "....######...."]
    for j, row in enumerate(shape):
        for i, ch in enumerate(row):
            if ch == "#":
                if lit:
                    c = colors[0] if j < 5 else colors[1] if j < 9 else colors[2]
                else:
                    c = (90, 90, 90, 255) if (i + j) % 3 else (110, 110, 110, 255)
                img.putpixel((x + i, y + j), c)


def inv_x(spec):
    """Return the x of the first player inventory column: 8 on a 176-wide panel, centered on a wider one."""
    return (spec.get("width", 176) - 176) // 2 + 8


def background(name, spec):
    h = spec["height"]
    img = panel(name, spec.get("width", 176), h)
    inv_y = h - 82
    ix = inv_x(spec)
    for r in range(3):
        for c in range(9):
            slot(img, ix + c * 18, inv_y + r * 18)
    for c in range(9):
        slot(img, ix + c * 18, inv_y + 58)
    for (x, y) in spec.get("inputs", []) + spec.get("outputs", []):
        slot(img, x, y)
    for (x, y, w, h) in spec.get("wells", []):
        well(img, x, y, w, h)
    if "grid" in spec:
        gx, gy, cols, rows = spec["grid"]
        for r in range(rows):
            for c in range(cols):
                slot(img, gx + c * 18, gy + r * 18)
    if "energy" in spec:
        ex, ey = spec["energy"]
        well(img, ex - 1, ey - 1, 14, 54)
        rect(img, ex, ey, 12, 52, (40, 22, 18, 255))
    for bx, by in spec.get("bars", []):
        well(img, bx - 1, by - 1, 14, 54)
        rect(img, bx, by, 12, 52, (24, 28, 36, 255))
    if "heat" in spec:
        hx, hy = spec["heat"]
        well(img, hx - 1, hy - 1, 8, 54)
        rect(img, hx, hy, 6, 52, (36, 20, 16, 255))
    if "arrow" in spec:
        ax, ay = spec["arrow"]
        arrow(img, ax, ay, (120, 112, 98, 255))
    if "flame" in spec:
        fx, fy = spec["flame"]
        flame(img, fx, fy, None, False)
    if "figure" in spec:
        figure(img, *spec["figure"])
    return img


def figure(img, x, y):
    """Draw a 24x28 inset bay with the embossed silhouette of a worker robot: where the assembled worker appears."""
    well(img, x, y, 24, 28)
    shape = ["....######....", "...########...", "...#..##..#...", "...########...", ".....####.....", ".############.",
             "##.########.##", "##.########.##", "##.########.##", "...########...", "....##..##....", "....##..##....",
             "....##..##....", "...###..###..."]
    for j, row in enumerate(shape):
        for i, ch in enumerate(row):
            if ch == "#":
                img.putpixel((x + 5 + i, y + 7 + j), (112, 112, 112, 255))
                if j == 0 or shape[j - 1][i] != "#":
                    img.putpixel((x + 5 + i, y + 6 + j), (160, 160, 160, 255))


def widgets():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    bronze = [hexc(c) for c in art_spec.PALETTES["bronze"]]
    for i in range(24):
        col = shade(bronze[3], 0.8 + 0.4 * i / 23)
        for j in range(17):
            head = i >= 14 and abs(j - 8) <= 8 - (i - 14) * 0.8
            shaft = i < 15 and 5 <= j <= 11
            if head or shaft:
                edge = j in (5, 11) and i < 15
                img.putpixel((i, j), shade(col, 0.75) if edge else col)
    for j in range(52):
        t = 1 - j / 51
        r = int(200 + 55 * t)
        g = int(40 + 180 * t)
        b = int(20 + 40 * t)
        for i in range(12):
            stripe = 0.85 if j % 4 == 0 else 1.0
            edge = 0.8 if i in (0, 11) else 1.0
            img.putpixel((24 + i, j), (int(r * stripe * edge), int(g * stripe * edge), int(b * stripe * edge), 255))
    flame(img, 48, 0, [(255, 240, 150, 255), (255, 170, 40, 255), (220, 70, 20, 255)], True)
    return img


def java(layouts):
    lines = ["package dev.autarkia.registry;", "", "/** Menu layouts generated by tools/gui.py; do not edit by hand. */",
             "public final class GuiLayouts {"]
    for name, s in layouts.items():
        slots = list(s.get("inputs", [])) + list(s.get("outputs", []))
        if "grid" in s:
            gx, gy, cols, rows = s["grid"]
            slots += [(gx + c * 18, gy + r * 18) for r in range(rows) for c in range(cols)]
        arr = ", ".join(f"{{{x}, {y}}}" for x, y in slots)
        ex, ey = s.get("energy", (-1, -1))
        ax, ay = s.get("arrow", (-1, -1))
        fx, fy = s.get("flame", (-1, -1))
        lines.append(f"    public static final GuiLayout {name.upper()} = new GuiLayout(\"{name}\", {s.get('width', 176)}, {s['height']}, "
                     f"new int[][]{{{arr}}}, {len(s.get('inputs', []))}, {s['data']}, {ex}, {ey}, {ax}, {ay}, {fx}, {fy}, {s['height'] - 82}, "
                     f"{inv_x(s)});")
    lines += ["", "    private GuiLayouts() {", "    }", "}", ""]
    return "\n".join(lines)


def main():
    TEX.mkdir(parents=True, exist_ok=True)
    print(f"phase 1: {len(LAYOUTS)} backgrounds")
    for name, spec in LAYOUTS.items():
        background(name, spec).save(TEX / f"{name}.png")
        print(f"  wrote textures/gui/{name}.png")
    print(f"phase 2: widgets and {len(DARK_PANELS)} nine-slice panel sprites")
    widgets().save(TEX / "widgets.png")
    sprites = TEX / "sprites"
    sprites.mkdir(exist_ok=True)
    meta = json.dumps({"gui": {"scaling": {"type": "nine_slice", "width": SPRITE_W, "height": SPRITE_H, "border": SPRITE_BORDER}}}, indent=4)
    for key, (mat, tone) in DARK_PANELS.items():
        framed(key, mat, tone, SPRITE_W, SPRITE_H).save(sprites / f"{key}.png")
        (sprites / f"{key}.png.mcmeta").write_text(meta + "\n", encoding="utf-8")
        print(f"  wrote textures/gui/sprites/{key}.png")
    print("phase 3: GuiLayouts.java")
    JAVA.parent.mkdir(parents=True, exist_ok=True)
    JAVA.write_text(java(LAYOUTS), encoding="utf-8")
    print(f"done: {len(LAYOUTS)} layouts")


if __name__ == "__main__":
    main()

"""Generate menu background textures, the GUI widget atlas and GuiLayouts.java from one layout table.

Every menu is a 176-wide panel with the player inventory at the bottom. The panel frame takes the tier palette
(T1 bronze, T2 brass, T3 steel, T4 vantite steel, T5 lattice, T6 aegis); slots are inset wells, the energy bar,
progress arrow and flame are drawn empty here and filled by the screen from textures/gui/widgets.png.

Usage: gui.py
"""

import random
from pathlib import Path

from PIL import Image

import art_spec

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src" / "generated" / "resources" / "assets" / "autarkia" / "textures" / "gui"
JAVA = ROOT / "src" / "generated" / "java" / "dev" / "autarkia" / "registry" / "GuiLayouts.java"

TIER_FRAME = {1: "bronze", 2: "brass", 3: "steel", 4: "vantite_steel", 5: "lattice", 6: "aegis_gold"}
TIER_FILL = {1: "canvas", 2: "canvas", 3: "ceramic", 4: "ceramic", 5: "nanite", 6: "nanite"}

LAYOUTS = {
    "stamp_mill": dict(tier=1, height=166, inputs=[(56, 35)], outputs=[(116, 35), (134, 35)], energy=(8, 18), arrow=(79, 35), data=5),
    "crucible": dict(tier=1, height=166, inputs=[(38, 26), (38, 44)], outputs=[(116, 35)], arrow=(79, 35), flame=(40, 62), data=6),
    "crate": dict(tier=1, height=166, grid=(8, 18, 9, 3), data=0),
    "worker_assembler": dict(tier=1, height=166, inputs=[(26, 35), (56, 26), (74, 26), (92, 26), (56, 44), (74, 44), (92, 44)], outputs=[],
                             energy=(8, 18), arrow=(120, 35), data=5),
    "boiler": dict(tier=2, height=166, inputs=[(80, 54), (8, 18)], outputs=[(8, 54)], flame=(62, 56), bars=[(28, 18), (46, 18)], data=7),
    "energy_storage": dict(tier=2, height=166, inputs=[(152, 35)], outputs=[], energy=(8, 18), data=6),
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


def panel(w, h, tier, rnd):
    frame = [hexc(c) for c in art_spec.PALETTES[TIER_FRAME[tier]]]
    fill = [hexc(c) for c in art_spec.PALETTES[TIER_FILL[tier]]]
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for x in range(w):
        for y in range(h):
            n = rnd.random()
            base = fill[3] if n > 0.15 else fill[2]
            img.putpixel((x, y), shade(base, 0.97 + 0.06 * rnd.random()))
    for x in range(w):
        for y in range(h):
            d = min(x, y, w - 1 - x, h - 1 - y)
            if d == 0:
                img.putpixel((x, y), frame[0])
            elif d == 1:
                img.putpixel((x, y), frame[4] if (x < w / 2 and y < h / 2) or x < 2 or y < 2 else frame[2])
            elif d == 2:
                img.putpixel((x, y), frame[3] if x <= 2 or y <= 2 else frame[1])
            elif d == 3:
                img.putpixel((x, y), shade(fill[1], 1.0))
    for (rx, ry) in [(5, 5), (w - 6, 5), (5, h - 6), (w - 6, h - 6)]:
        rect(img, rx - 1, ry - 1, 3, 3, frame[1])
        img.putpixel((rx - 1, ry - 1), frame[4])
        img.putpixel((rx, ry), frame[3])
    for x in (0, 1, w - 1, w - 2):
        for y in (0, 1, h - 1, h - 2):
            if (x in (0, w - 1)) and (y in (0, h - 1)):
                img.putpixel((x, y), (0, 0, 0, 0))
    return img


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


def background(name, spec, rnd):
    h = spec["height"]
    img = panel(176, h, spec["tier"], rnd)
    inv_y = h - 82
    for r in range(3):
        for c in range(9):
            slot(img, 8 + c * 18, inv_y + r * 18)
    for c in range(9):
        slot(img, 8 + c * 18, inv_y + 58)
    for (x, y) in spec.get("inputs", []) + spec.get("outputs", []):
        slot(img, x, y)
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
    if "arrow" in spec:
        ax, ay = spec["arrow"]
        arrow(img, ax, ay, (120, 112, 98, 255))
    if "flame" in spec:
        fx, fy = spec["flame"]
        flame(img, fx, fy, None, False)
    return img


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
        lines.append(f"    public static final GuiLayout {name.upper()} = new GuiLayout(\"{name}\", 176, {s['height']}, new int[][]{{{arr}}}, "
                     f"{len(s.get('inputs', []))}, {s['data']}, {ex}, {ey}, {ax}, {ay}, {fx}, {fy}, {s['height'] - 82});")
    lines += ["", "    private GuiLayouts() {", "    }", "}", ""]
    return "\n".join(lines)


def main():
    TEX.mkdir(parents=True, exist_ok=True)
    print(f"phase 1: {len(LAYOUTS)} backgrounds")
    for name, spec in LAYOUTS.items():
        rnd = random.Random(f"gui:{name}")
        background(name, spec, rnd).save(TEX / f"{name}.png")
        print(f"  wrote textures/gui/{name}.png")
    print("phase 2: widgets")
    widgets().save(TEX / "widgets.png")
    print("phase 3: GuiLayouts.java")
    JAVA.parent.mkdir(parents=True, exist_ok=True)
    JAVA.write_text(java(LAYOUTS), encoding="utf-8")
    print(f"done: {len(LAYOUTS)} layouts")


if __name__ == "__main__":
    main()

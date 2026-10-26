"""Render every Autarkia GUI offline and check its layout, without running Minecraft.

Each screen is drawn at GUI scale 1 from its generated background (textures/gui), the vanilla font and button sprites
(read from the 26.3 client jar, for this review only) and a widget spec in gui_screens.py that mirrors the screen's Java
code. Text is measured with the vanilla glyph advances. Checks per screen: text wider than its free space, elements that
overlap, and a panel larger than 320 x 240 GUI pixels (the space every GUI scale guarantees). Sheets go to
screenshots/gui/ at 3x; the report lists every finding and the exit code is 1 when there is any.

Usage: gui_render.py [screen filter ...]
"""

import io
import json
import re
import sys
import zipfile
from dataclasses import dataclass, field
from pathlib import Path

import numpy as np
from PIL import Image

import gui
from gui_screens import SCREENS

ROOT = Path(__file__).resolve().parent.parent
JAR = Path(r"C:\Users\renat\curseforge\minecraft\Install\versions\26.3\26.3.jar")
LANG = ROOT / "src" / "generated" / "resources" / "assets" / "autarkia" / "lang" / "en_us.json"
OUT = ROOT / "screenshots" / "gui"
MAX_W, MAX_H = 320, 240
VANILLA = {"container.inventory": "Inventory"}
SCALE = 3


def _jar_image(path: str) -> Image.Image:
    with zipfile.ZipFile(JAR) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


class Font:
    """The vanilla ascii bitmap font: 8 px cells, advance = glyph width + 1, space 4, shadow offset (1, 1)."""

    def __init__(self):
        sheet = np.array(_jar_image("font/ascii"))
        self.glyphs: dict[str, np.ndarray] = {}
        self.adv: dict[str, int] = {" ": 4}
        rows = json.loads(_jar_text("font/include/default.json"))["providers"]
        chars = next(p for p in rows if p.get("file") == "minecraft:font/ascii.png")["chars"]
        for r, line in enumerate(chars):
            for c, ch in enumerate(line):
                if ch == "\x00" or ch == " ":
                    continue
                cell = sheet[r * 8:(r + 1) * 8, c * 8:(c + 1) * 8, 3] > 0
                cols = np.flatnonzero(cell.any(axis=0))
                self.glyphs[ch] = cell
                self.adv[ch] = (cols[-1] + 2) if cols.size else 1

    def width(self, text: str) -> int:
        return sum(self.adv.get(ch, 6) for ch in text)

    def draw(self, img: np.ndarray, text: str, x: int, y: int, rgb: int, shadow: bool) -> None:
        col = np.array([(rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255], dtype=np.uint8)
        dark = (col // 4).astype(np.uint8)
        for dx, c in (((1, dark),) if shadow else ()) + ((0, col),):
            cx = x + dx
            for ch in text:
                g = self.glyphs.get(ch)
                if g is not None:
                    _blit_mask(img, g, cx, y + dx, c)
                cx += self.adv.get(ch, 6)


def _jar_text(path: str) -> str:
    with zipfile.ZipFile(JAR) as z:
        return z.read(f"assets/minecraft/{path}").decode("utf-8")


def _blit_mask(img: np.ndarray, mask: np.ndarray, x: int, y: int, rgb) -> None:
    h, w = mask.shape
    H, W = img.shape[:2]
    x0, y0, x1, y1 = max(0, x), max(0, y), min(W, x + w), min(H, y + h)
    if x0 >= x1 or y0 >= y1:
        return
    m = mask[y0 - y:y1 - y, x0 - x:x1 - x]
    img[y0:y1, x0:x1, :3][m] = rgb
    img[y0:y1, x0:x1, 3][m] = 255


def _nine_slice(sprite: Image.Image, w: int, h: int, border: int) -> np.ndarray:
    s = np.array(sprite)
    sh, sw = s.shape[:2]
    out = np.zeros((h, w, 4), dtype=np.uint8)
    b = min(border, w // 2, h // 2)
    xs = [(0, b, 0, b), (b, w - b, b, sw - b), (w - b, w, sw - b, sw)]
    ys = [(0, b, 0, b), (b, h - b, b, sh - b), (h - b, h, sh - b, sh)]
    for dy0, dy1, sy0, sy1 in ys:
        for dx0, dx1, sx0, sx1 in xs:
            if dx1 <= dx0 or dy1 <= dy0:
                continue
            part = Image.fromarray(s[sy0:sy1, sx0:sx1]).resize((dx1 - dx0, dy1 - dy0), Image.NEAREST)
            out[dy0:dy1, dx0:dx1] = np.array(part)
    return out


def _over(dst: np.ndarray, src: np.ndarray, x: int, y: int) -> None:
    h, w = src.shape[:2]
    H, W = dst.shape[:2]
    x0, y0, x1, y1 = max(0, x), max(0, y), min(W, x + w), min(H, y + h)
    if x0 >= x1 or y0 >= y1:
        return
    s = src[y0 - y:y1 - y, x0 - x:x1 - x]
    m = s[..., 3] > 0
    dst[y0:y1, x0:x1][m] = s[m]


@dataclass
class Box:
    """One drawn element with its bounds, for the overlap check; kind is text, button, edit, slot or bar."""

    kind: str
    label: str
    x: int
    y: int
    w: int
    h: int

    def overlaps(self, o: "Box") -> bool:
        return self.x < o.x + o.w and o.x < self.x + self.w and self.y < o.y + o.h and o.y < self.y + self.h


@dataclass
class Result:
    """A rendered screen: the 1x image, its element boxes and the findings."""

    name: str
    image: np.ndarray
    boxes: list[Box] = field(default_factory=list)
    findings: list[str] = field(default_factory=list)


class Renderer:
    """Draw specs from gui_screens.SCREENS and record findings."""

    def __init__(self):
        self.font = Font()
        self.lang = json.loads(LANG.read_text(encoding="utf-8"))
        self.button = _jar_image("gui/sprites/widget/button")
        self.button_off = _jar_image("gui/sprites/widget/button_disabled")

    def text_of(self, key: str, args=(), res: "Result | None" = None) -> str:
        """Return the English text of a lang key (or the literal when it is no key), with %s filled from args.

        A key-shaped string (autarkia namespace, no spaces) missing from the lang is recorded as a finding on res.
        """
        if key in VANILLA:
            return VANILLA[key]
        if res is not None and key not in self.lang and " " not in key and ".autarkia." in key:
            res.findings.append(f"lang key '{key}' is missing: the screen shows the raw key")
        raw = self.lang.get(key, key).replace("%%", "\x01")
        seq = iter(range(len(raw)))

        def arg(m: re.Match) -> str:
            n = int(m.group(1)) - 1 if m.group(1) else next(seq)
            return str(args[n]) if n < len(args) else "0"

        return re.sub(r"%(?:(\d+)\$)?s", arg, raw).replace("\x01", "%")

    def render(self, name: str, spec: dict) -> Result:
        """Draw one screen and check it."""
        w, h = spec["size"]
        img = np.zeros((h, w, 4), dtype=np.uint8)
        res = Result(name, img)
        if "background" in spec:
            bg = np.array(Image.open(gui.TEX / f"{spec['background']}.png").convert("RGBA"))
            _over(img, bg, 0, 0)
        if "panel" in spec:
            _over(img, _nine_slice(Image.open(gui.TEX / "sprites" / f"{spec['panel']}.png").convert("RGBA"), w, h, gui.SPRITE_BORDER), 0, 0)
        for x, y in spec.get("slots", []):
            res.boxes.append(Box("slot", f"slot {x},{y}", x - 1, y - 1, 18, 18))
        for el in spec.get("elements", []):
            self.element(res, el)
        if w > MAX_W or h > MAX_H:
            res.findings.append(f"panel {w} x {h} is larger than {MAX_W} x {MAX_H}: cut off at the largest GUI scale of a small window")
        self.check(res, spec)
        return res

    def element(self, res: Result, el: dict) -> None:
        kind = el["kind"]
        if kind == "button":
            sprite = self.button if el.get("active", True) else self.button_off
            _over(res.image, _nine_slice(sprite, el["w"], el["h"], 3), el["x"], el["y"])
            label = self.text_of(el["text"], el.get("args", ()), res)
            inner = el["w"] - 4
            for alt in [label] + [self.text_of(k, (), res) for k in el.get("variants", ())]:
                if self.font.width(alt) > inner:
                    res.findings.append(f"button '{alt}' needs {self.font.width(alt)} px, has {inner}: the label scrolls or is clipped")
            tw = self.font.width(label)
            self.font.draw(res.image, label, el["x"] + (el["w"] - min(tw, inner)) // 2, el["y"] + (el["h"] - 8) // 2, 0xE0E0E0, True)
            res.boxes.append(Box("button", label, el["x"], el["y"], el["w"], el["h"]))
        elif kind == "edit":
            x, y, w, h = el["x"], el["y"], el["w"], el["h"]
            res.image[y:y + h, x:x + w] = (160, 160, 160, 255)
            res.image[y + 1:y + h - 1, x + 1:x + w - 1] = (0, 0, 0, 255)
            self.font.draw(res.image, self.text_of(el.get("hint", ""))[:40], x + 4, y + (h - 8) // 2, 0x707070, True)
            res.boxes.append(Box("edit", el.get("hint", "edit"), x, y, w, h))
        elif kind == "bar":
            x, y, w, h = el["x"], el["y"], el["w"], el["h"]
            c = el.get("color", 0x2A2A2A)
            res.image[y:y + h, x:x + w] = ((c >> 16) & 255, (c >> 8) & 255, c & 255, 255)
            res.boxes.append(Box("bar", el.get("label", "bar"), x, y, w, h))
        elif kind == "text":
            self.text(res, el)

    def split(self, text: str, width: int) -> list[str]:
        """Return text word-wrapped to width pixels the way the vanilla splitter breaks at spaces."""
        lines, cur = [], ""
        for word in text.split(" "):
            cand = word if not cur else cur + " " + word
            if self.font.width(cand) <= width or not cur:
                cur = cand
            else:
                lines.append(cur)
                cur = word
        return lines + [cur]

    def text(self, res: Result, el: dict) -> None:
        variants = el.get("variants") or [(el["text"], el.get("args", ()))]
        texts = [self.text_of(k, a, res) for k, a in variants]
        if el.get("lines"):
            room, n = el["room"], el["lines"]
            longest = max(texts, key=lambda t: len(self.split(t, room)))
            lines = self.split(longest, room)
            if len(lines) > n:
                res.findings.append(f"text '{longest}' needs {len(lines)} lines of {room} px, has {n}")
            for i, line in enumerate(lines[:n]):
                y = el["y"] + i * el.get("line_h", 9)
                self.font.draw(res.image, line, el["x"], y, el.get("color", 0x404040), el.get("shadow", False))
                res.boxes.append(Box("text", line, el["x"], y, self.font.width(line), 8))
            return
        widest = max(texts, key=self.font.width)
        tw = self.font.width(widest)
        room = el.get("room")
        x = el["x"] - tw if el.get("right") else el["x"]
        if room is None:
            room = (res.image.shape[1] - 5) - (x if not el.get("right") else 0) if not el.get("right") else el["x"] - 5
        if tw > room:
            res.findings.append(f"text '{widest}' needs {tw} px, has {room}" + (" (the screen cuts it with ...)" if el.get("cut") else ""))
        shown = widest
        if el.get("cut") and tw > room:
            while shown and self.font.width(shown + "...") > room:
                shown = shown[:-1]
            shown += "..."
            tw = self.font.width(shown)
            x = el["x"] - tw if el.get("right") else el["x"]
        self.font.draw(res.image, shown, x, el["y"], el.get("color", 0x404040), el.get("shadow", False))
        res.boxes.append(Box("text", shown, x, el["y"], tw, 8))

    def check(self, res: Result, spec: dict) -> None:
        allowed = {frozenset(p) for p in spec.get("overlap_ok", [])}
        b = res.boxes
        for i in range(len(b)):
            for j in range(i + 1, len(b)):
                if b[i].kind == "slot" and b[j].kind == "slot":
                    continue
                if b[i].overlaps(b[j]) and frozenset((b[i].label, b[j].label)) not in allowed:
                    res.findings.append(f"{b[i].kind} '{b[i].label}' overlaps {b[j].kind} '{b[j].label}'")
        h, w = res.image.shape[:2]
        for box in b:
            if box.x < 4 or box.y < 3 or box.x + box.w > w - 4 or box.y + box.h > h - 3:
                res.findings.append(f"{box.kind} '{box.label}' reaches into the panel frame ({box.x},{box.y} {box.w}x{box.h})")


def sheet(results: list[Result], name: str) -> Path:
    """Lay the results out at SCALE in a grid on a grey world-like backdrop and save it."""
    from PIL import ImageDraw
    tiles = []
    for r in results:
        big = Image.fromarray(r.image).resize((r.image.shape[1] * SCALE, r.image.shape[0] * SCALE), Image.NEAREST)
        t = Image.new("RGBA", (big.width + 16, big.height + 34), (90, 96, 104, 255))
        t.alpha_composite(big, (8, 26))
        ImageDraw.Draw(t).text((8, 6), f"{r.name}  ({len(r.findings)} findings)", fill=(255, 230, 140, 255))
        tiles.append(t)
    per_row = 2
    rows = [tiles[i:i + per_row] for i in range(0, len(tiles), per_row)]
    W = max(sum(t.width for t in row) for row in rows)
    H = sum(max(t.height for t in row) for row in rows)
    img = Image.new("RGBA", (W, H), (60, 64, 70, 255))
    y = 0
    for row in rows:
        x = 0
        for t in row:
            img.alpha_composite(t, (x, y))
            x += t.width
        y += max(t.height for t in row)
    OUT.mkdir(parents=True, exist_ok=True)
    p = OUT / f"{name}.png"
    img.convert("RGB").save(p)
    return p


def main(argv: list[str]) -> int:
    """Render the selected screens (default all), write sheets of 4 screens each and print every finding."""
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    r = Renderer()
    names = [n for n in SCREENS if not argv or any(f in n for f in argv)]
    print(f"[gui] rendering {len(names)} screens")
    results = []
    for n in names:
        res = r.render(n, SCREENS[n](r))
        results.append(res)
        print(f"[gui] {n}: {len(res.findings)} findings")
        for f in res.findings:
            print(f"      - {f}")
    for i in range(0, len(results), 4):
        print(f"[gui] wrote {sheet(results[i:i + 4], f'gui_{i // 4 + 1:02d}')}")
    total = sum(len(x.findings) for x in results)
    print(f"[gui] {total} findings in {len(results)} screens")
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

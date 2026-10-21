"""Component and colony item sprite painters: SHAPES[shape](name, palette, rng) -> 16x16 RGBA.

Sprites follow vanilla item conventions: a readable silhouette lit from the top-left, highlight
pixels, and a 1px outline darkened from the neighbouring material.
"""

import numpy as np

from art_spec import TIER_LAMP
from paint import (BLACK, WHITE, col, disc_mask, ellipse_mask, line, line_pts, mix, new, over, poly_mask, put,
                   ramp, rect, sprite)


def box(x0: int, y0: int, x1: int, y1: int) -> np.ndarray:
    """Return a 16x16 mask of the inclusive rectangle."""
    m = np.zeros((16, 16), dtype=bool)
    m[y0:y1 + 1, x0:x1 + 1] = True
    return m


def stroke(pts, width: int = 2) -> np.ndarray:
    """Return a 16x16 mask of a polyline; width 2 adds the right neighbour, width 3 also the one below."""
    offs = [(0, 0), (1, 0), (0, 1)][:max(1, min(3, width))]
    m = np.zeros((16, 16), dtype=bool)
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        for x, y in line_pts(x0, y0, x1, y1):
            for dx, dy in offs:
                if 0 <= x + dx < 16 and 0 <= y + dy < 16:
                    m[y + dy, x + dx] = True
    return m


def body(img: np.ndarray, mask: np.ndarray, r: np.ndarray, base: float = 2.0, k: float = 0.6, cap: int = 2,
         gloss: int = 1, rng=None, noise: float = 0.0) -> None:
    """Shade a masked region with top-left relief and composite it onto img (no outline)."""
    over(img, sprite(mask, r, rng, base, noise, k, cap, outline=False, gloss=gloss))


def outline(img: np.ndarray, t: float = 0.55) -> None:
    """Add a 1px outline whose colour darkens the darkest touching material pixel toward black."""
    a = img[..., 3] > 0
    h, w = a.shape
    src = img.copy()
    lum = src[..., :3].astype(int).sum(axis=2)
    for y in range(h):
        for x in range(w):
            if a[y, x]:
                continue
            best = None
            for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and a[ny, nx] and (best is None or lum[ny, nx] < lum[best[1], best[0]]):
                    best = (nx, ny)
            if best is not None:
                img[y, x] = mix(tuple(int(v) for v in src[best[1], best[0]]), BLACK, t)


def ramps(pal: str) -> tuple[np.ndarray, np.ndarray]:
    """Return (body, trim) ramps; the two-tone aegis palette splits into black body and gold trim."""
    if pal == "aegis":
        return ramp("aegis_body"), ramp("aegis_gold")
    return ramp(pal), ramp(pal)


def circuit(name, pal, rng):
    """Green board with copper traces, a black chip with tin legs and solder pads."""
    r, cu, chip, tin = ramp(pal), ramp("copper"), ramp("rubber"), ramp("tin")
    img = new(16, 16)
    board = box(1, 3, 14, 12)
    board[3, 1] = board[3, 14] = board[12, 1] = board[12, 14] = False
    body(img, board, r, base=1.7, k=0.7, gloss=0)
    for pts in (((2, 5), (4, 5), (5, 6)), ((2, 10), (4, 10), (5, 9)), ((10, 6), (11, 5), (13, 5)),
                ((10, 9), (11, 10), (13, 10)), ((7, 4), (7, 5)), ((8, 10), (8, 11), (12, 11)), ((3, 7), (3, 8))):
        for p, q in zip(pts, pts[1:]):
            line(img, *p, *q, col(cu, 3))
    for x, y in ((2, 5), (2, 10), (13, 5), (13, 10), (12, 11), (7, 4), (3, 7)):
        put(img, x, y, col(tin, 4))
    rect(img, 6, 6, 9, 9, col(chip, 1))
    rect(img, 6, 6, 9, 6, col(chip, 3))
    rect(img, 6, 6, 6, 9, col(chip, 2))
    put(img, 7, 7, col(chip, 4))
    for x in (6, 8):
        put(img, x + 1, 5, col(tin, 3))
    for y in (7, 8):
        put(img, 5, y, col(tin, 3))
        put(img, 10, y, col(tin, 2))
    put(img, 12, 7, col(ramp("red_paint"), 3))
    put(img, 13, 4, col(r, 4))
    put(img, 3, 11, col(r, 0))
    outline(img)
    return img


def circuit_adv(name, pal, rng):
    """Dark teal board with clipped corners, gold traces and a large chip with a glowing core."""
    r, gold, steel = ramp(pal), ramp("gold"), ramp("steel")
    img = new(16, 16)
    board = poly_mask(16, 16, [(3, 1), (12, 1), (14, 3), (14, 12), (12, 14), (3, 14), (1, 12), (1, 3)])
    body(img, board, r, base=1.4, k=0.7, gloss=0)
    for pts in (((7, 2), (7, 4)), ((9, 2), (9, 4)), ((2, 7), (4, 7)), ((2, 9), (4, 9)), ((11, 7), (13, 7)),
                ((11, 9), (13, 9)), ((7, 11), (7, 13)), ((9, 11), (9, 13)), ((3, 4), (4, 5)), ((12, 12), (11, 11))):
        line(img, *pts[0], *pts[1], col(gold, 3))
    for x, y in ((7, 2), (9, 2), (2, 7), (2, 9), (13, 7), (13, 9), (7, 13), (9, 13)):
        put(img, x, y, col(gold, 4))
    body(img, box(5, 5, 10, 10), steel, base=1.0, k=0.8, gloss=0)
    rect(img, 7, 7, 8, 8, col(r, 4))
    put(img, 7, 7, WHITE)
    put(img, 12, 3, col(r, 4))
    put(img, 3, 12, col(r, 4))
    outline(img)
    return img


def servo(name, pal, rng):
    """Side view of a hobby servo: steel box with flange ears, brass gear boss, white horn, three-wire lead."""
    r, brass, horn = ramp(pal), ramp("brass"), ramp("ceramic")
    img = new(16, 16)
    body(img, box(3, 6, 12, 13), r, base=2.0, k=0.7)
    body(img, box(1, 7, 14, 8), r, base=1.7, k=0.6, gloss=0)
    put(img, 2, 7, col(r, 0))
    put(img, 13, 7, col(r, 0))
    rect(img, 5, 10, 10, 11, col(brass, 2))
    rect(img, 5, 10, 10, 10, col(brass, 3))
    body(img, box(4, 4, 7, 5), brass, base=2.2, k=0.7, gloss=1)
    body(img, box(2, 2, 9, 3), horn, base=2.8, k=0.6, gloss=1)
    put(img, 5, 2, col(horn, 1))
    put(img, 3, 3, col(horn, 1))
    put(img, 8, 3, col(horn, 1))
    for i, c in enumerate((ramp("leather"), ramp("red_paint"), ramp("#E08A2E"))):
        rect(img, 13, 10 + i, 15, 10 + i, col(c, 3))
    outline(img)
    return img


def ai_core(name, pal, rng):
    """Isometric violet processor cube with glowing circuit lines and a bright core."""
    r, glow = ramp(pal), ramp("#FF5CF0")
    img = new(16, 16)
    top = poly_mask(16, 16, [(8, 1), (14, 4), (8, 7), (2, 4)])
    left = poly_mask(16, 16, [(2, 4), (8, 7), (8, 14), (2, 11)]) & ~top
    right = poly_mask(16, 16, [(8, 7), (14, 4), (14, 11), (8, 14)]) & ~top & ~left
    img[left] = col(r, 2)
    img[right] = col(r, 1)
    img[top] = col(r, 3)
    trace = [(3, 7), (4, 7), (4, 8), (4, 9), (5, 10), (6, 10), (6, 11), (3, 10)]
    for x, y in trace:
        put(img, x, y, col(glow, 3))
        put(img, 16 - x, y, col(glow, 2))
    line(img, 8, 7, 8, 13, col(r, 4))
    for x, y in ((7, 3), (8, 3), (9, 3), (6, 4), (10, 4), (7, 5), (8, 5), (9, 5)):
        put(img, x, y, col(glow, 3))
    put(img, 8, 4, WHITE)
    put(img, 8, 1, col(r, 4))
    outline(img, 0.6)
    return img


def _cell(img, cap: np.ndarray, liquid: np.ndarray, glass: np.ndarray) -> None:
    body(img, box(7, 0, 8, 1), cap, base=2.5, k=0.6, gloss=0)
    body(img, box(5, 1, 10, 3), cap, base=2.0, k=0.7)
    body(img, box(5, 12, 10, 14), cap, base=1.6, k=0.7, gloss=0)
    rect(img, 5, 4, 10, 11, col(glass, 0))
    rect(img, 6, 5, 9, 11, col(liquid, 2))
    rect(img, 6, 5, 9, 5, col(liquid, 4))
    rect(img, 9, 6, 9, 11, col(liquid, 1))
    rect(img, 6, 6, 6, 10, col(liquid, 3))
    put(img, 6, 6, WHITE)
    put(img, 8, 8, col(liquid, 4))
    put(img, 7, 10, col(liquid, 4))


def cell_fuel(name, pal, rng):
    """Upright fuel cell: lattice caps, glass window of magenta plasma."""
    img = new(16, 16)
    _cell(img, ramp(pal), ramp("#FF5CF0"), ramp(pal))
    outline(img)
    return img


def cell_coolant(name, pal, rng):
    """Upright coolant cell: steel caps rimed with frost, icy blue coolant."""
    img = new(16, 16)
    _cell(img, ramp("steel"), ramp("#6CC8FF"), ramp("steel"))
    for x, y in ((5, 2), (9, 1), (10, 13), (6, 14), (5, 12), (10, 3)):
        put(img, x, y, col(ramp(pal), 4))
    outline(img)
    return img


def wrench(name, pal, rng):
    """Diagonal open-end wrench with a ring end at the handle."""
    r = ramp(pal)
    img = new(16, 16)
    shaft = stroke([(3, 12), (10, 5)])
    yy, xx = np.mgrid[0:16, 0:16]
    head = disc_mask(16, 16, 11.5, 4.5, 3.3)
    u = ((xx - 11.5) - (yy - 4.5)) / np.sqrt(2)
    v = ((xx - 11.5) + (yy - 4.5)) / np.sqrt(2)
    head &= ~((u > -0.3) & (np.abs(v) < 1.2))
    ring = disc_mask(16, 16, 3.0, 12.5, 2.4) & ~disc_mask(16, 16, 3.0, 12.5, 0.9)
    body(img, shaft | head | ring, r, base=2.2, k=0.7, gloss=2)
    for p in line_pts(5, 9, 9, 5):
        put(img, *p, col(r, 3))
    outline(img)
    return img


def cutter(name, pal, rng):
    """Diagonal wire cutters: steel jaws at the top right, bronze grips splayed at the bottom left."""
    r, grip = ramp(pal), ramp("bronze")
    img = new(16, 16)
    body(img, poly_mask(16, 16, [(7, 7), (1, 8), (1, 10), (7, 9)]), grip, base=2.1, k=0.6, gloss=1)
    body(img, poly_mask(16, 16, [(8, 9), (6, 15), (8, 15), (10, 9)]), grip, base=1.8, k=0.6, gloss=0)
    jaw = poly_mask(16, 16, [(8, 6), (12, 2), (14, 1), (13, 3), (10, 8), (8, 9)])
    body(img, jaw, r, base=2.3, k=0.6, gloss=1)
    for p in line_pts(10, 6, 13, 2):
        put(img, *p, col(r, 0))
    body(img, disc_mask(16, 16, 8.5, 7.5, 1.8), r, base=2.0, k=0.8, gloss=0)
    put(img, 8, 7, col(r, 4))
    put(img, 9, 8, col(r, 0))
    for x, y in ((1, 9), (1, 10), (6, 15), (7, 15)):
        put(img, x, y, col(grip, 0))
    outline(img)
    return img


def meter(name, pal, rng):
    """Brass multimeter: ceramic dial with a red needle, selector knob and probe sockets."""
    r, face, red, blk = ramp(pal), ramp("ceramic"), ramp("red_paint"), ramp("rubber")
    img = new(16, 16)
    case = box(3, 1, 12, 14)
    case[1, 3] = case[1, 12] = case[14, 3] = case[14, 12] = False
    body(img, case, r, base=2.0, k=0.7)
    rect(img, 4, 2, 11, 6, col(face, 3))
    rect(img, 4, 2, 11, 2, col(face, 4))
    rect(img, 4, 6, 11, 6, col(face, 1))
    for x, y in ((5, 5), (6, 4), (7, 3), (8, 3), (9, 4), (10, 5)):
        put(img, x, y, col(face, 1))
    line(img, 7, 6, 9, 3, col(red, 2))
    body(img, disc_mask(16, 16, 7.5, 9.5, 1.9), blk, base=1.8, k=0.9, gloss=1)
    put(img, 7, 8, col(r, 4))
    put(img, 5, 13, col(red, 3))
    put(img, 10, 13, col(blk, 3))
    put(img, 5, 12, col(r, 0))
    put(img, 10, 12, col(r, 0))
    outline(img)
    return img


def lens(name, pal, rng):
    """Brass monocle ring with a teal lens and a short diagonal handle."""
    r, brass = ramp(pal), ramp("brass")
    img = new(16, 16)
    ring = disc_mask(16, 16, 9.5, 6.0, 5.2)
    handle = stroke([(2, 13), (5, 10)])
    body(img, ring | handle, brass, base=2.0, k=0.7, gloss=1)
    glass = disc_mask(16, 16, 9.5, 6.0, 3.6)
    yy, xx = np.mgrid[0:16, 0:16]
    shade = np.clip(np.rint(2.6 + ((xx - 9.5) + (yy - 6.0)) * -0.25), 1, 4).astype(int)
    img[glass, :3] = r[shade][glass]
    for x, y in ((8, 3), (7, 4), (8, 4), (7, 5)):
        put(img, x, y, WHITE if (x, y) == (8, 3) else col(r, 4))
    put(img, 3, 12, col(brass, 4))
    outline(img)
    return img


def tablet(name, pal, rng):
    """Bronze-framed tablet with a glowing amber blueprint screen and a button."""
    r, lit, dark = ramp(pal), ramp(TIER_LAMP[1]), ramp("rubber")
    img = new(16, 16)
    frame = box(2, 1, 13, 14)
    frame[1, 2] = frame[1, 13] = frame[14, 2] = frame[14, 13] = False
    body(img, frame, r, base=2.0, k=0.7)
    rect(img, 4, 3, 11, 10, col(dark, 1))
    for x in range(4, 12, 2):
        for y in range(3, 11):
            put(img, x, y, col(dark, 2))
    rect(img, 5, 5, 9, 5, col(lit, 3))
    rect(img, 5, 5, 5, 9, col(lit, 3))
    rect(img, 5, 9, 10, 9, col(lit, 2))
    rect(img, 9, 5, 9, 7, col(lit, 2))
    put(img, 7, 7, col(lit, 4))
    put(img, 10, 4, WHITE)
    body(img, box(7, 12, 8, 12), dark, base=2.6, k=0.5, gloss=0)
    outline(img)
    return img


def book(name, pal, rng):
    """Leather field manual: dark spine, cream page edge, bronze cog emblem on the cover."""
    lea, pages, cog = ramp("leather"), ramp("canvas"), ramp("bronze")
    img = new(16, 16)
    body(img, box(3, 2, 12, 13), lea, base=2.0, k=0.6, gloss=0)
    rect(img, 13, 3, 13, 14, col(pages, 3))
    rect(img, 4, 14, 13, 14, col(pages, 2))
    put(img, 13, 3, col(pages, 4))
    rect(img, 3, 2, 4, 13, col(lea, 1))
    rect(img, 3, 3, 3, 12, col(lea, 0))
    rect(img, 4, 4, 4, 4, col(cog, 3))
    rect(img, 4, 11, 4, 11, col(cog, 3))
    gear = disc_mask(16, 16, 8.5, 8.0, 2.4)
    for k in range(8):
        a = k * np.pi / 4
        gear[int(round(8.0 + 3.3 * np.sin(a))), int(round(8.5 + 3.3 * np.cos(a)))] = True
    gear &= ~disc_mask(16, 16, 8.5, 8.0, 0.8)
    img[gear] = col(cog, 3)
    p = np.pad(gear, 1)
    img[gear & ~p[2:, 2:]] = col(cog, 1)
    img[gear & ~p[:-2, :-2]] = col(cog, 4)
    put(img, 8, 8, col(lea, 0))
    outline(img)
    return img


def seed(name, pal, rng):
    """Bronze seed pod of overlapping petals around an amber glow, with roots below."""
    r, petal, amber = ramp(pal), ramp("bronze"), ramp(TIER_LAMP[1])
    img = new(16, 16)
    pod = ellipse_mask(16, 16, 3, 1, 12, 12)
    body(img, pod, petal, base=2.0, k=0.7, gloss=1)
    for p in line_pts(5, 3, 7, 11):
        put(img, *p, col(petal, 1))
    for p in line_pts(10, 3, 8, 11):
        put(img, *p, col(petal, 1))
    for y in range(4, 11):
        put(img, 7 + (y > 6), y, col(amber, 3 if 5 < y < 9 else 2))
        put(img, 8 - (y > 8), y, col(amber, 2))
    put(img, 7, 7, col(amber, 4))
    put(img, 8, 7, WHITE)
    for pts in (((7, 12), (5, 14)), ((8, 12), (8, 15)), ((9, 12), (11, 14)), ((5, 14), (4, 14))):
        line(img, *pts[0], *pts[1], col(r, 1))
    put(img, 6, 13, col(r, 3))
    put(img, 10, 13, col(r, 3))
    outline(img)
    return img


def upgrade(name, pal, rng):
    """Upgrade cartridge in the tier casing: lamp window in the tier colour, tier pips, gold pins."""
    tier = int(name[-1])
    r, trim = ramps(pal)
    lamp, gold = ramp(TIER_LAMP[tier]), ramp("gold")
    img = new(16, 16)
    casing = box(2, 2, 13, 12)
    casing[2, 2] = casing[2, 13] = False
    body(img, casing, r, base=2.0, k=0.7)
    if pal == "aegis":
        rect(img, 2, 12, 13, 12, col(trim, 2))
        rect(img, 3, 3, 12, 3, col(trim, 3))
    rect(img, 3, 13, 12, 13, col(r, 0))
    for x in range(4, 12, 2):
        put(img, x, 14, col(gold, 3))
        put(img, x, 13, col(gold, 2))
    win = disc_mask(16, 16, 7.5, 7.5, 3.1)
    img[win] = col(r, 0)
    core = disc_mask(16, 16, 7.5, 7.5, 2.2)
    img[core] = col(lamp, 2)
    img[disc_mask(16, 16, 7.0, 7.0, 1.2)] = col(lamp, 3)
    put(img, 6, 6, WHITE)
    put(img, 9, 9, col(lamp, 1))
    for i in range(tier):
        put(img, 3 + i * 2 if tier < 6 else 2 + i * 2, 4 if pal != "aegis" else 5, col(lamp, 3))
    outline(img)
    return img


def card(name, pal, rng):
    """Brass punch card with a clipped corner and faint unpunched hole guides."""
    r = ramp(pal)
    img = new(16, 16)
    sheet = box(1, 3, 14, 12)
    for i in range(3):
        sheet[3 + i, 1:4 - i] = False
    body(img, sheet, r, base=2.6, k=0.5, gloss=1)
    for y in (6, 8, 10):
        for x in range(4, 14, 2):
            put(img, x, y, col(r, 2))
    rect(img, 4, 4, 12, 4, col(r, 3))
    outline(img)
    return img


def card_recorded(name, pal, rng):
    """Punch card with a blue data stripe and a deterministic pattern of punched holes."""
    r, blue = ramp(pal), ramp("electric_accent")
    img = card(name, pal, rng)
    rect(img, 4, 4, 13, 4, col(blue, 3))
    rect(img, 4, 5, 13, 5, col(blue, 1))
    put(img, 4, 4, col(blue, 4))
    for y in (7, 9, 11):
        for x in range(3, 14, 2):
            if rng.random() < 0.55:
                put(img, x, y, col(r, 0))
    return img


def seed_outpost(name, pal, rng):
    """Steel capsule with a green lamp band and a glass dome holding a sapling."""
    r, lamp, glass = ramp(pal), ramp(TIER_LAMP[4]), ramp("glass")
    img = new(16, 16)
    dome = ellipse_mask(16, 16, 4, 1, 11, 8) & box(0, 0, 15, 6)
    img[dome] = col(glass, 2)
    rect(img, 7, 4, 7, 6, col(ramp("wood"), 2))
    for x, y, s in ((6, 2, 2), (7, 2, 3), (8, 2, 2), (5, 3, 1), (6, 3, 2), (7, 3, 2), (8, 3, 1), (9, 3, 1),
                    (8, 4, 1), (6, 4, 0)):
        put(img, x, y, col(lamp, s))
    put(img, 7, 1, col(lamp, 3))
    put(img, 5, 3, col(glass, 4))
    put(img, 5, 4, WHITE)
    body(img, box(4, 7, 11, 14), r, base=2.0, k=0.7)
    rect(img, 4, 10, 11, 10, col(lamp, 3))
    rect(img, 4, 11, 11, 11, col(lamp, 1))
    put(img, 5, 10, WHITE)
    rect(img, 6, 15, 9, 15, col(r, 1))
    outline(img)
    return img


def _capsule(img, r, glass) -> None:
    body(img, box(5, 1, 10, 3), r, base=2.2, k=0.7)
    body(img, box(5, 12, 10, 14), r, base=1.8, k=0.7, gloss=0)
    rect(img, 5, 4, 10, 11, col(glass, 1))
    rect(img, 6, 4, 6, 11, col(glass, 3))
    put(img, 6, 5, WHITE)
    rect(img, 10, 4, 10, 11, col(glass, 0))
    rect(img, 7, 0, 8, 0, col(r, 3))


def capsule(name, pal, rng):
    """Empty glass worker capsule with bronze end caps."""
    img = new(16, 16)
    _capsule(img, ramp(pal), ramp("glass"))
    put(img, 8, 8, col(ramp("glass"), 2))
    outline(img)
    return img


def capsule_filled(name, pal, rng):
    """Worker capsule holding a tiny folded walker with an amber eye."""
    r, bot, eye = ramp(pal), ramp("bronze"), ramp(TIER_LAMP[1])
    img = new(16, 16)
    _capsule(img, r, ramp("glass"))
    rect(img, 6, 5, 9, 6, col(bot, 3))
    put(img, 6, 5, col(bot, 4))
    rect(img, 7, 6, 8, 6, col(eye, 4))
    rect(img, 6, 7, 9, 9, col(bot, 2))
    put(img, 6, 7, col(bot, 4))
    rect(img, 9, 8, 9, 9, col(bot, 1))
    put(img, 5, 8, col(bot, 1))
    put(img, 10, 8, col(bot, 1))
    rect(img, 6, 10, 6, 11, col(bot, 1))
    rect(img, 9, 10, 9, 11, col(bot, 1))
    outline(img)
    return img


def seed_autarky(name, pal, rng):
    """Faceted black seed with a gold crown and glowing cyan veins."""
    blk, gold, glow = ramp("aegis_body"), ramp("aegis_gold"), ramp("aegis_glow")
    img = new(16, 16)
    pod = poly_mask(16, 16, [(8, 1), (12, 5), (12, 10), (8, 15), (3, 10), (3, 5)])
    body(img, pod, blk, base=2.0, k=0.8, gloss=0)
    for p in line_pts(8, 3, 8, 13):
        put(img, *p, col(glow, 2))
    for pts in (((8, 6), (5, 9)), ((8, 6), (11, 9)), ((8, 10), (5, 11)), ((8, 10), (10, 12))):
        for p in line_pts(*pts[0], *pts[1]):
            put(img, *p, col(glow, 3))
    put(img, 8, 8, WHITE)
    rect(img, 6, 3, 10, 3, col(gold, 3))
    rect(img, 7, 2, 9, 2, col(gold, 2))
    put(img, 8, 1, col(gold, 4))
    put(img, 4, 5, col(gold, 3))
    put(img, 11, 5, col(gold, 2))
    outline(img, 0.4)
    return img


def frame_walker(name, pal, rng):
    """Bronze humanoid skeleton frame: ring head, spine, shoulder and hip bars, limbs, rivet joints."""
    r = ramp(pal)
    m = np.zeros((16, 16), dtype=bool)
    m |= box(6, 0, 9, 3) & ~box(7, 1, 8, 2)
    for x0, y0, x1, y1 in ((7, 4, 8, 10), (3, 5, 12, 6), (5, 10, 10, 11), (3, 5, 3, 11), (12, 5, 12, 11),
                           (5, 11, 6, 15), (9, 11, 10, 15), (4, 15, 6, 15), (9, 15, 11, 15)):
        m |= box(x0, y0, x1, y1)
    img = new(16, 16)
    body(img, m, r, base=2.0, k=0.5, gloss=1)
    for x, y in ((3, 5), (12, 5), (7, 10), (5, 11), (10, 11), (3, 11), (12, 11)):
        put(img, x, y, col(r, 4))
    put(img, 7, 1, col(ramp(TIER_LAMP[1]), 3))
    outline(img)
    return img


def frame_crab(name, pal, rng):
    """Top view of a steel six-leg frame: oval carapace ring with a cross brace and jointed legs."""
    r = ramp(pal)
    m = ellipse_mask(16, 16, 4, 4, 11, 11) & ~ellipse_mask(16, 16, 6, 6, 9, 9)
    m |= box(7, 5, 8, 10) | box(5, 7, 10, 8)
    for pts in (((4, 6), (2, 4), (1, 5)), ((4, 8), (1, 8), (0, 10)), ((5, 10), (3, 12), (2, 14)),
                ((11, 6), (13, 4), (14, 5)), ((11, 8), (14, 8), (15, 10)), ((10, 10), (12, 12), (13, 14))):
        m |= stroke(list(pts), 1)
    img = new(16, 16)
    body(img, m, r, base=2.1, k=0.6, gloss=1)
    for x, y in ((2, 4), (1, 8), (3, 12), (13, 4), (14, 8), (12, 12)):
        put(img, x, y, col(r, 4))
    put(img, 7, 7, col(r, 4))
    put(img, 8, 8, col(r, 0))
    outline(img)
    return img


def frame_drone(name, pal, rng):
    """Top view of a steel quad-rotor X frame with a hub and four rotor rings."""
    r = ramp(pal)
    m = stroke([(4, 4), (11, 11)]) | stroke([(11, 3), (4, 10)]) | box(6, 6, 9, 9)
    for cx, cy in ((3.0, 3.0), (12.0, 3.0), (3.0, 12.0), (12.0, 12.0)):
        m |= disc_mask(16, 16, cx, cy, 2.9) & ~disc_mask(16, 16, cx, cy, 1.6)
    img = new(16, 16)
    body(img, m, r, base=2.1, k=0.6, gloss=1)
    for cx, cy in ((3, 3), (12, 3), (3, 12), (12, 12)):
        put(img, cx, cy, col(r, 3))
    rect(img, 7, 7, 8, 8, col(ramp(TIER_LAMP[3]), 3))
    put(img, 7, 7, WHITE)
    outline(img)
    return img


def _symbol(kind: str) -> list[tuple[int, int]]:
    """Return the pixels of an upgrade symbol inside the chip window (5..10, 5..10)."""
    if kind == "speed":
        return [(5, 5), (6, 6), (7, 7), (6, 8), (5, 9), (8, 5), (9, 6), (10, 7), (9, 8), (8, 9)]
    if kind == "cargo":
        return [(x, y) for x in range(5, 11) for y in range(6, 11) if x in (5, 10) or y in (6, 10)] + [(7, 6), (8, 6), (7, 7), (8, 7)]
    if kind == "armor":
        rows = {4: (5, 10), 5: (5, 10), 6: (5, 10), 7: (5, 10), 8: (6, 9), 9: (6, 9), 10: (7, 8)}
        return [(x, y) for y, (a, b) in rows.items() for x in range(a, b + 1)]
    pts = []
    for y in range(4, 12):
        for x in range(4, 12):
            d = np.hypot(x - 4.5, y - 10.5)
            if d < 1.6 or 3.0 <= d < 4.2 or 5.6 <= d < 6.8:
                pts.append((x + 1, y))
    return pts


def upgrade_chip(name, pal, rng):
    """Worker upgrade microchip with gold legs and an embossed arrow, box, shield or antenna."""
    kind = name.rsplit("_", 1)[1]
    r, gold, dark = ramp(pal), ramp("gold"), ramp("rubber")
    img = new(16, 16)
    for y in (4, 6, 9, 11):
        rect(img, 1, y, 2, y, col(gold, 3))
        rect(img, 13, y, 14, y, col(gold, 2))
    for x in (4, 6, 9, 11):
        put(img, x, 1, col(gold, 3))
        put(img, x, 14, col(gold, 2))
    chip = box(3, 2, 12, 13)
    chip[2, 3] = True
    body(img, chip, r, base=1.8, k=0.7, gloss=0)
    rect(img, 4, 4, 11, 11, col(dark, 1) if pal != "electric_accent" else col(r, 0))
    for x, y in _symbol(kind):
        if 4 <= x <= 11 and 4 <= y <= 11:
            put(img, x, y, col(r, 4) if kind != "armor" or x < 8 else col(r, 3))
    put(img, 4, 3, WHITE)
    outline(img)
    return img


SHAPES = {
    "circuit": circuit, "servo": servo, "circuit_adv": circuit_adv, "ai_core": ai_core, "cell_fuel": cell_fuel,
    "cell_coolant": cell_coolant, "wrench": wrench, "cutter": cutter, "meter": meter, "lens": lens, "tablet": tablet,
    "book": book, "seed": seed, "upgrade": upgrade, "card": card, "card_recorded": card_recorded,
    "seed_outpost": seed_outpost, "capsule": capsule, "capsule_filled": capsule_filled, "seed_autarky": seed_autarky,
    "frame_walker": frame_walker, "frame_crab": frame_crab, "frame_drone": frame_drone, "upgrade_chip": upgrade_chip,
}

"""Machine and electrical block textures: fronts, lamps, belts, insulators, tubes, docks and beacons.

Every painter has the signature (name, palette, rng) -> 16x16 RGBA array.
"""

import numpy as np

from paint import (add_outline, bevel, col, dither, disc_mask, line, new, noisy_fill, plate, put, ramp, rect, rivet,
                   shade_img, value_noise)
from paint_blocks_metal import body, inset

WHITE = (255, 255, 255, 255)


def casing(pal: str, rng: np.random.Generator, screws: bool = True) -> np.ndarray:
    """Return a beveled machine casing plate with optional corner screws."""
    r = body(pal)
    img = plate(16, 16, r, rng, spread=0.22)
    bevel(img, 0, 0, 15, 15, col(r, 3), col(r, 0))
    if screws:
        for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
            rivet(img, x, y, r)
    return img


def _lamp(pal: str, rng: np.random.Generator, on: bool) -> np.ndarray:
    r = ramp(pal)
    lit = ramp("#FFD27F")
    img = casing(pal, rng, screws=False)
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5)
    glass = (xx >= 2) & (xx <= 13) & (yy >= 2) & (yy <= 13)
    if on:
        vals = 4.3 - d * 0.35
        img[glass] = shade_img(lit, dither(vals - 0.5, 0.5).clip(2, 4))[glass]
        img[glass & (d < 2.5)] = WHITE
    else:
        g = ramp("glass")
        vals = 1.4 - d * 0.1 + (value_noise(16, 16, rng, 4) - 0.5) * 0.6
        img[glass] = shade_img(g, dither(vals - 0.5, 0.4).clip(0, 2))[glass]
        put(img, 4, 4, col(g, 3))
        put(img, 5, 4, col(g, 2))
        put(img, 4, 5, col(g, 2))
    for k in (5, 10):
        rect(img, k, 2, k, 13, col(r, 2))
        rect(img, 2, k, 13, k, col(r, 2))
        if not on:
            rect(img, k + 1, 2, k + 1, 13, col(r, 0))
    for k in (5, 10):
        for j in (5, 10):
            put(img, k, j, col(r, 4))
    inset(img, 1, 1, 14, 14, r)
    return img


def lamp_off(name, pal, rng):
    """Caged industrial lamp with dark glass."""
    return _lamp(pal, rng, False)


def lamp_on(name, pal, rng):
    """Caged industrial lamp glowing warm white."""
    return _lamp(pal, rng, True)


def marker(name, pal, rng):
    """Small violet crystal on a steel stake (transparent ground)."""
    v, s = ramp(pal), ramp("steel")
    img = new(16, 16)
    rect(img, 7, 7, 8, 15, col(s, 2))
    rect(img, 7, 7, 7, 15, col(s, 3))
    rect(img, 6, 13, 9, 13, col(s, 1))
    pts = [(7, 1), (8, 1), (6, 2), (7, 2), (8, 2), (9, 2), (5, 3), (6, 3), (7, 3), (8, 3), (9, 3), (10, 3),
           (5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4), (6, 5), (7, 5), (8, 5), (9, 5), (7, 6), (8, 6)]
    for x, y in pts:
        put(img, x, y, col(v, 4 if x < 7 and y < 4 else 3 if x < 8 else 2 if y < 5 else 1))
    put(img, 7, 2, WHITE)
    add_outline(img, col(v, 0))
    return img


def valve(name, pal, rng):
    """Brass pipe junction with a red spoked handwheel."""
    r, red = ramp(pal), ramp("red_paint")
    img = casing(pal, rng, screws=False)
    rect(img, 6, 0, 9, 15, col(r, 2))
    rect(img, 6, 0, 6, 15, col(r, 4))
    rect(img, 9, 0, 9, 15, col(r, 0))
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5)
    rim = (d >= 4.6) & (d < 6.4)
    img[rim] = col(red, 2)
    img[rim & (xx + yy < 13)] = col(red, 3)
    img[rim & (xx + yy > 18)] = col(red, 1)
    for x0, y0, x1, y1 in ((7, 3, 7, 12), (8, 3, 8, 12), (3, 7, 12, 7), (3, 8, 12, 8)):
        line(img, x0, y0, x1, y1, col(red, 2))
    img[d < 1.8] = col(r, 3)
    put(img, 7, 7, col(r, 4))
    put(img, 4, 4, col(red, 4))
    return img


def arrester(name, pal, rng):
    """Copper spike standing on stacked ceramic discs (transparent ground)."""
    c, cer = ramp(pal), ramp("ceramic")
    img = new(16, 16)
    for y0, w in ((8, 5), (10, 6), (12, 5), (14, 6)):
        rect(img, 8 - w, y0, 7 + w, y0 + 1, col(cer, 2))
        rect(img, 8 - w, y0, 7 + w, y0, col(cer, 4))
        put(img, 8 - w, y0 + 1, col(cer, 1))
        put(img, 7 + w, y0 + 1, col(cer, 1))
    rect(img, 7, 1, 8, 7, col(c, 2))
    rect(img, 7, 1, 7, 7, col(c, 4))
    put(img, 7, 0, col(c, 3))
    rect(img, 6, 6, 9, 7, col(c, 1))
    rect(img, 6, 6, 9, 6, col(c, 3))
    add_outline(img, col(cer, 0))
    return img


STRIPES = (2, 5, 9, 12)


def capacitor_front(name, pal, rng):
    """Brass capacitor casing with four dark charge-stripe windows (lit by the stripe overlay)."""
    r = body(pal)
    img = casing(pal, rng)
    for x in STRIPES:
        rect(img, x - 1, 2, x + 2, 13, col(r, 3))
        rect(img, x, 3, x + 1, 12, col(ramp("rubber"), 1))
        rect(img, x, 3, x + 1, 3, col(ramp("rubber"), 0))
        put(img, x + 2, 13, col(r, 1))
    return img


def charge_stripe(name, pal, rng):
    """Glowing overlay for the four capacitor stripe windows (transparent elsewhere)."""
    r = ramp(pal)
    img = new(16, 16)
    for x in STRIPES:
        for y in range(3, 13):
            v = 4 if (y + x) % 4 == 0 else 3
            put(img, x, y, col(r, v))
            put(img, x + 1, y, col(r, v - 1))
        put(img, x, 3, WHITE)
    return img


def press_front(name, pal, rng):
    """Brass press frame with a steel ram above a die plate."""
    r, s = body(pal), ramp("steel")
    img = casing(pal, rng, screws=False)
    inset(img, 2, 2, 13, 13, r)
    rect(img, 3, 3, 12, 12, col(r, 0))
    rect(img, 7, 3, 8, 6, col(s, 3))
    rect(img, 4, 6, 11, 8, col(s, 2))
    bevel(img, 4, 6, 11, 8, col(s, 4), col(s, 0))
    rect(img, 4, 11, 11, 12, col(s, 1))
    rect(img, 4, 11, 11, 11, col(s, 3))
    rect(img, 6, 10, 9, 10, col(ramp("copper"), 3))
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(img, x, y, r)
    return img


def drawer_front(name, pal, rng):
    """Brass wire drawer: steel die on the left feeding copper wire onto a spool."""
    s, cu = ramp("steel"), ramp("copper")
    img = casing(pal, rng)
    yy, xx = np.mgrid[0:16, 0:16]
    die = np.hypot(xx - 4.5, yy - 7.5)
    img[die < 2.8] = col(s, 2)
    img[(die < 2.8) & (xx + yy < 11)] = col(s, 3)
    img[die < 1.0] = col(s, 0)
    sp = np.hypot(xx - 10.5, yy - 7.5)
    img[sp < 3.8] = col(cu, 1)
    ring = (sp < 3.3)
    img[ring] = col(cu, 2)
    for y in range(5, 11, 2):
        rect(img, 8, y, 13, y, col(cu, 3))
    img[sp < 1.2] = col(s, 2)
    rect(img, 5, 7, 9, 7, col(cu, 4))
    return img


def belt(name, pal, rng):
    """Rubber belt with raised ribs every 4px (tiles vertically) and worn edges."""
    r = ramp(pal)
    img = noisy_fill(16, 16, r, rng, 1.0, 2.0, cells=(4, 2))
    for y in range(0, 16, 4):
        rect(img, 1, y, 14, y, col(r, 4))
        rect(img, 1, y + 1, 14, y + 1, col(r, 3))
        rect(img, 1, y + 2, 14, y + 2, col(r, 0))
    rect(img, 0, 0, 0, 15, col(r, 0))
    rect(img, 15, 0, 15, 15, col(r, 0))
    return img


def belt_side(name, pal, rng):
    """Steel side frame: top and bottom rails with three rollers between."""
    r = ramp(pal)
    img = noisy_fill(16, 16, r, rng, 0.6, 1.4)
    for y0 in (0, 12):
        rect(img, 0, y0, 15, y0 + 3, col(r, 2))
        rect(img, 0, y0, 15, y0, col(r, 4))
        rect(img, 0, y0 + 3, 15, y0 + 3, col(r, 0))
        for x in (2, 8, 14):
            put(img, x, y0 + 1, col(r, 3))
    for cx in (2.5, 7.5, 12.5):
        img[disc_mask(16, 16, cx, 7.5, 2.4)] = col(r, 2)
        img[disc_mask(16, 16, cx - 0.6, 6.9, 1.2)] = col(r, 4)
        put(img, int(cx), 7, col(r, 0))
    return img


def splitter_top(name, pal, rng):
    """Belt with a yellow diverter arrow pointing to both sides."""
    img = belt(name, pal, rng)
    h = ramp("hazard")
    pts = [(7, 3), (8, 3), (6, 4), (9, 4), (5, 5), (10, 5), (4, 6), (11, 6), (7, 4), (8, 4), (7, 5), (8, 5),
           (7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9), (7, 10), (8, 10), (7, 11), (8, 11)]
    for x, y in pts:
        put(img, x, y, col(h, 3))
    for x, y in ((6, 9), (5, 10), (9, 9), (10, 10), (4, 11), (11, 11)):
        put(img, x, y, col(h, 3))
    for x, y in pts[:4]:
        put(img, x, y - 1, col(h, 4))
    return img


def loom_front(name, pal, rng):
    """Brass loom with a punch-card reader slot, a card sticking out and a status lamp."""
    r, paper = body(pal), ramp("canvas")
    img = casing(pal, rng)
    inset(img, 2, 3, 13, 6, r)
    rect(img, 3, 4, 12, 5, col(r, 0))
    rect(img, 4, 1, 11, 4, col(paper, 4))
    for x in (5, 7, 8, 10):
        put(img, x, 2, col(paper, 1))
    put(img, 6, 3, col(paper, 1))
    rect(img, 4, 4, 11, 4, col(paper, 2))
    for k, x in enumerate(range(3, 13, 3)):
        rect(img, x, 9, x + 1, 12, col(r, 1))
        rect(img, x, 9, x + 1, 9, col(r, 4))
        put(img, x, 10 + k % 3, col(r, 4))
    put(img, 12, 7, col(ramp("#FFD27F"), 3))
    return img


def loom_top(name, pal, rng):
    """Loom top: card slot plus a glass inspection window."""
    r, g = body(pal), ramp("glass")
    img = casing(pal, rng)
    rect(img, 3, 3, 12, 4, col(r, 0))
    rect(img, 3, 5, 12, 5, col(r, 3))
    rect(img, 3, 7, 12, 12, col(g, 2))
    inset(img, 2, 6, 13, 13, r)
    for k in range(3):
        put(img, 4 + k, 10 - k, col(g, 4))
    return img


def insulator(name, pal, rng):
    """Stacked ceramic insulator on a steel wall bracket (transparent ground)."""
    s, cer = ramp(pal), ramp("ceramic")
    img = new(16, 16)
    rect(img, 2, 12, 13, 15, col(s, 2))
    bevel(img, 2, 12, 13, 15, col(s, 3), col(s, 0))
    rect(img, 7, 9, 8, 12, col(s, 1))
    for y0, w in ((2, 3), (4, 5), (6, 4), (8, 5)):
        rect(img, 8 - w, y0, 7 + w, y0 + 1, col(cer, 3))
        rect(img, 8 - w, y0, 7 + w, y0, col(cer, 4))
        put(img, 7 + w, y0 + 1, col(cer, 1))
        put(img, 8 - w, y0, col(cer, 4))
    put(img, 6, 4, WHITE)
    rect(img, 7, 0, 8, 1, col(ramp("copper"), 3))
    add_outline(img, col(s, 0))
    return img


def transformer_side(name, pal, rng):
    """Grey cooling fins: vertical ribs with lit and shaded faces between rails."""
    r = ramp(pal)
    img = new(16, 16)
    for x in range(16):
        rect(img, x, 0, x, 15, col(r, (3, 2, 1, 0)[x % 4]))
    for y0 in (0, 14):
        rect(img, 0, y0, 15, y0 + 1, col(r, 2))
        rect(img, 0, y0, 15, y0, col(r, 4))
    n = value_noise(16, 16, rng, 4)
    img[(n > 0.85) & (np.arange(16)[:, None] > 1) & (np.arange(16)[:, None] < 14)] = col(r, 1)
    return img


def transformer_top(name, pal, rng):
    """Top plate with three ceramic bushings capped by copper terminals."""
    r, cer, cu = ramp(pal), ramp("ceramic"), ramp("copper")
    img = casing(pal, rng)
    for cx in (3.5, 7.5, 11.5):
        img[disc_mask(16, 16, cx, 7.5, 2.4)] = col(cer, 1)
        img[disc_mask(16, 16, cx - 0.4, 7.1, 1.9)] = col(cer, 3)
        img[disc_mask(16, 16, cx, 7.5, 0.9)] = col(cu, 3)
        put(img, int(cx - 1), 6, WHITE)
    rect(img, 2, 12, 13, 12, col(r, 1))
    return img


def fuse_front(name, pal, rng):
    """Grey fuse box: glass fuse window on the left, big red-handled lever on the right."""
    r, g, red = ramp(pal), ramp("glass"), ramp("red_paint")
    img = casing(pal, rng)
    inset(img, 2, 3, 7, 12, r)
    rect(img, 3, 4, 6, 11, col(g, 1))
    rect(img, 4, 5, 5, 10, col(ramp("ceramic"), 3))
    rect(img, 4, 7, 5, 8, col(ramp("copper"), 3))
    put(img, 3, 4, col(g, 3))
    rect(img, 10, 3, 12, 12, col(r, 0))
    rect(img, 11, 4, 11, 9, col(r, 3))
    rect(img, 9, 3, 13, 5, col(red, 2))
    rect(img, 9, 3, 13, 3, col(red, 4))
    rect(img, 9, 5, 13, 5, col(red, 1))
    rect(img, 10, 11, 12, 11, col(r, 3))
    return img


def battery_front(name, pal, rng):
    """Steel cabinet with a round gauge above a row of green charge cells."""
    r, cer, ok = ramp(pal), ramp("ceramic"), ramp("#7CFF6B")
    img = casing(pal, rng)
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 5.0)
    img[d < 3.6] = col(r, 0)
    img[d < 2.9] = col(cer, 3)
    line(img, 7, 5, 9, 3, col(ramp("red_paint"), 2))
    put(img, 7, 5, col(r, 0))
    for k, x in enumerate((2, 5, 8, 11)):
        rect(img, x, 10, x + 2, 13, col(r, 0))
        rect(img, x, 11 - (k == 3), x + 1, 12, col(ok, 2 if k < 3 else 1))
        put(img, x, 11, col(ok, 4) if k < 3 else col(ok, 1))
    return img


def separator_front(name, pal, rng):
    """Magnetic separator drum wound with copper coils inside a steel frame."""
    r, cu = ramp(pal), ramp("copper")
    img = casing(pal, rng, screws=False)
    rect(img, 2, 3, 13, 12, col(r, 1))
    for y in range(3, 13):
        rect(img, 2, y, 13, y, col(r, 3 if y < 7 else 2 if y < 10 else 1))
    for x0 in (3, 7, 11):
        for y in range(3, 13):
            put(img, x0, y, col(cu, 3 if y % 2 else 2))
            put(img, x0 + 1, y, col(cu, 2 if y % 2 else 1))
    rect(img, 2, 3, 13, 3, col(r, 4))
    rect(img, 2, 12, 13, 12, col(r, 0))
    rect(img, 0, 6, 1, 9, col(r, 3))
    rect(img, 14, 6, 15, 9, col(r, 1))
    return img


def electrolyzer_front(name, pal, rng):
    """Glass tank of blue electrolyte with two dark electrodes and rising bubbles."""
    r, w = ramp(pal), ramp("water")
    img = casing(pal, rng)
    rect(img, 2, 2, 13, 13, col(r, 0))
    rect(img, 3, 5, 12, 12, col(w, 2))
    rect(img, 3, 3, 12, 4, col(ramp("glass"), 1))
    rect(img, 3, 5, 12, 5, col(w, 3))
    for x0 in (5, 10):
        rect(img, x0, 2, x0, 11, col(r, 1))
        put(img, x0, 2, col(r, 3))
    for x, y in ((6, 10), (6, 7), (7, 9), (11, 8), (9, 11), (11, 6)):
        put(img, x, y, col(w, 4))
    put(img, 6, 8, WHITE)
    put(img, 3, 6, col(w, 4))
    return img


def controller_front(name, pal, rng):
    """Warehouse controller: dark screen with a cyan fill bar and a row of status lamps."""
    r, c = ramp(pal), ramp("#4FE3FF")
    img = casing(pal, rng)
    inset(img, 2, 2, 13, 9, r)
    rect(img, 3, 3, 12, 8, col(ramp("rubber"), 1))
    rect(img, 4, 6, 11, 7, col(c, 0))
    rect(img, 4, 6, 9, 7, col(c, 2))
    rect(img, 4, 6, 9, 6, col(c, 3))
    for x in (4, 6, 8):
        put(img, x, 4, col(c, 3))
    for x, lamp in zip((3, 6, 9, 12), ("#7CFF6B", "#7CFF6B", "#FFD27F", "#FF3B3B"), strict=True):
        put(img, x, 11, col(ramp(lamp), 3))
        put(img, x, 12, col(ramp(lamp), 1))
    return img


def terminal_front(name, pal, rng):
    """Ledger terminal: green text screen above a small keyboard."""
    r, gr = ramp(pal), ramp("#7CFF6B")
    img = casing(pal, rng, screws=False)
    inset(img, 2, 1, 13, 8, r)
    rect(img, 3, 2, 12, 7, col(ramp("rubber"), 0))
    for y, ln in ((3, 7), (4, 5), (5, 8), (6, 3)):
        rect(img, 4, y, 3 + ln, y, col(gr, 2))
    put(img, 4 + 3, 6, col(gr, 4))
    rect(img, 2, 10, 13, 14, col(r, 1))
    for y in (11, 13):
        for x in range(3, 13, 2):
            put(img, x, y, col(r, 4))
            put(img, x + 1, y, col(r, 2))
    rect(img, 5, 13, 10, 13, col(r, 3))
    return img


def tube_glass(name, pal, rng):
    """Glass tube section: translucent body with glints between brass end rings."""
    r, g = ramp(pal), ramp("glass")
    img = new(16, 16, col(g, 3, 120))
    for y in range(16):
        put(img, 3, y, col(g, 4, 200))
        put(img, 4, y, col(g, 4, 160))
        put(img, 12, y, col(g, 1, 160))
    for y0 in (0, 13):
        rect(img, 0, y0, 15, y0 + 2, col(r, 2))
        rect(img, 0, y0, 15, y0, col(r, 4))
        rect(img, 0, y0 + 2, 15, y0 + 2, col(r, 0))
        for x in (2, 7, 12):
            put(img, x, y0 + 1, col(r, 3))
    return img


def tube_frame(name, pal, rng):
    """Brass frame ring: beveled square ring with bolts and an open centre."""
    r = ramp(pal)
    img = plate(16, 16, r, rng)
    bevel(img, 0, 0, 15, 15, col(r, 4), col(r, 0))
    img[3:13, 3:13] = 0
    bevel(img, 2, 2, 13, 13, col(r, 1), col(r, 3))
    img[3:13, 3:13] = 0
    for x, y in ((0, 0), (14, 0), (0, 14), (14, 14)):
        put(img, x + (1 if x == 0 else 0), y + (1 if y == 0 else 0), col(r, 4))
    for x, y in ((7, 0), (0, 7), (14, 7), (7, 14)):
        put(img, x, y + (1 if y == 0 else 0), col(r, 4))
    return img


def dock_top(name, pal, rng):
    """Charging pad: hazard-striped edge around a pad with a cyan lightning glyph."""
    r, h, c = ramp(pal), ramp("hazard"), ramp("#4FE3FF")
    img = new(16, 16)
    for y in range(16):
        for x in range(16):
            img[y, x] = col(h, 3) if ((x + y) // 2) % 2 == 0 else col(h, 0)
    pad = noisy_fill(12, 12, r, rng, 1.2, 2.2)
    img[2:14, 2:14] = pad
    inset(img, 2, 2, 13, 13, r)
    bolt = [(9, 3), (8, 4), (8, 5), (7, 6), (7, 7), (6, 7), (8, 7), (9, 7), (8, 8), (7, 9), (7, 10), (6, 11), (6, 12)]
    for x, y in bolt:
        put(img, x, y, col(c, 3))
    put(img, 9, 3, WHITE)
    return img


def dock_side(name, pal, rng):
    """Steel dock side with a socket strip of three cable ports and a charge-level bar."""
    r, c = ramp(pal), ramp("#4FE3FF")
    img = casing(pal, rng)
    inset(img, 2, 3, 13, 6, r)
    rect(img, 3, 4, 12, 5, col(r, 0))
    rect(img, 3, 4, 9, 5, col(c, 2))
    rect(img, 3, 4, 9, 4, col(c, 3))
    rect(img, 2, 9, 13, 12, col(r, 1))
    bevel(img, 2, 9, 13, 12, col(r, 0), col(r, 3))
    for x in (4, 7, 10):
        rect(img, x, 10, x + 1, 11, col(r, 0))
        put(img, x, 10, col(ramp("copper"), 3))
    return img


def beacon_side(name, pal, rng):
    """Steel frame around a glass pane showing a violet amethyst core."""
    r, g, v = ramp(pal), ramp("glass"), ramp("lattice")
    img = casing(pal, rng, screws=False)
    rect(img, 2, 2, 13, 13, col(g, 2, 255))
    m = [(7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (5, 7),
         (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (6, 8), (7, 8), (8, 8), (9, 8), (6, 9), (7, 9), (8, 9), (9, 9),
         (7, 10), (8, 10), (7, 11), (8, 11)]
    for x, y in m:
        put(img, x, y, col(v, 4 if x <= 7 and y <= 6 else 3 if x <= 7 else 2))
    put(img, 7, 5, WHITE)
    for k in range(3):
        put(img, 3 + k, 12 - k, col(g, 4))
    inset(img, 1, 1, 14, 14, r)
    for x in (1, 14):
        for y in (1, 14):
            put(img, x, y, col(r, 4))
    return img


def beacon_top(name, pal, rng):
    """Emitter lens: concentric glass rings with a violet glowing centre."""
    r, g, v = ramp(pal), ramp("glass"), ramp("lattice")
    img = casing(pal, rng)
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5)
    img[d < 6.0] = col(r, 0)
    img[d < 5.2] = col(g, 1)
    img[d < 4.0] = col(g, 2)
    img[d < 2.8] = col(v, 3)
    img[d < 1.4] = col(v, 4)
    put(img, 5, 5, WHITE)
    put(img, 6, 4, col(g, 4))
    return img


STYLES = {
    "lamp_off": lamp_off, "lamp_on": lamp_on, "marker": marker, "valve": valve, "arrester": arrester,
    "capacitor_front": capacitor_front, "charge_stripe": charge_stripe, "press_front": press_front,
    "drawer_front": drawer_front, "belt": belt, "belt_side": belt_side, "splitter_top": splitter_top,
    "loom_front": loom_front, "loom_top": loom_top, "insulator": insulator, "transformer_side": transformer_side,
    "transformer_top": transformer_top, "fuse_front": fuse_front, "battery_front": battery_front,
    "separator_front": separator_front, "electrolyzer_front": electrolyzer_front,
    "controller_front": controller_front, "terminal_front": terminal_front, "tube_glass": tube_glass,
    "tube_frame": tube_frame, "dock_top": dock_top, "dock_side": dock_side, "beacon_side": beacon_side,
    "beacon_top": beacon_top,
}

"""Face finishes for GeckoLib atlases: each paints one (h, w, 4) patch from a palette ramp.

A material string is 'palette:finish[:arg]'; palette is a PALETTES name or '#RRGGBB'. Every finish
copes with any size from 1x1 upwards and bevels only where the patch is at least 3x3.
"""

import numpy as np

from geo_core import mat_parts
from paint import (CLEAR, bevel, col, dither, disc_mask, line, mix, new, noisy_fill, plate, put, ramp, rect,
                   rivet, shade_img, value_noise)


def _plate(r, w, h, rng, arg):
    return plate(w, h, r, rng, rivets=(arg == "rivet"))


def _rivet(r, w, h, rng, arg):
    img = plate(w, h, r, rng)
    if w >= 5 and h >= 5:
        step = 4
        for x in range(1, w - 2, step):
            rivet(img, x, 1, r)
            rivet(img, x, h - 3, r)
        for y in range(1 + step, h - 3, step):
            rivet(img, 1, y, r)
            rivet(img, w - 3, y, r)
    return img


def _panel(r, w, h, rng, arg):
    img = plate(w, h, r, rng, spread=0.4)
    step = int(arg) if arg.isdigit() else 8
    for x in range(step, w - 1, step):
        rect(img, x, 1, x, h - 2, col(r, 1))
        rect(img, x + 1, 1, x + 1, h - 2, col(r, 3))
    for y in range(step, h - 1, step):
        rect(img, 1, y, w - 2, y, col(r, 1))
        rect(img, 1, y + 1, w - 2, y + 1, col(r, 3))
    if w >= 6 and h >= 6:
        for x in list(range(0, w - 3, step)):
            for y in list(range(0, h - 3, step)):
                rivet(img, x + 2, y + 2, r)
    return img


def _flat(r, w, h, rng, arg):
    base = float(arg) if arg else 2.0
    return noisy_fill(w, h, r, rng, base - 0.55, base + 0.55, cells=(6, 3), dith=0.35)


def _dark(r, w, h, rng, arg):
    return noisy_fill(w, h, r, rng, -0.2, 1.2, cells=(3, 2), dith=0.8)


def _wood(r, w, h, rng, arg):
    vertical = h > w if arg != "h" else False
    if arg == "v":
        vertical = True
    W, H = (h, w) if vertical else (w, h)
    n = value_noise(W, H, rng, 6)
    streak = value_noise(W, H, rng, 2)
    vals = 1.6 + 0.9 * n
    yy = np.arange(H)[:, None]
    plank = 4
    vals = vals + 0.5 * (np.sin(yy * 2.1 + streak * 2.0) > 0.6)
    img = shade_img(r, dither(vals - 0.5, 0.6))
    for y in range(plank - 1, H, plank):
        rect(img, 0, y, W - 1, y, col(r, 0))
        offs = int(rng.integers(0, max(1, W)))
        for x in range(offs % 9, W, 9 + int(rng.integers(0, 4))):
            put(img, x, y - 1, col(r, 1))
    for _ in range(max(1, W * H // 90)):
        kx, ky = int(rng.integers(0, W)), int(rng.integers(0, H))
        put(img, kx, ky, col(r, 0))
        put(img, kx + 1, ky, col(r, 1))
    if vertical:
        img = np.ascontiguousarray(np.transpose(img, (1, 0, 2)))
    return img


def _timber(r, w, h, rng, arg):
    vertical = h > w
    W, H = (h, w) if vertical else (w, h)
    n = value_noise(W, H, rng, 5)
    grain = np.sin(np.arange(H)[:, None] * 2.6 + value_noise(W, H, rng, 3) * 3.0)
    vals = 2.0 + 0.6 * (n - 0.5) + 0.45 * (grain > 0.55) - 0.45 * (grain < -0.75)
    img = shade_img(r, dither(vals - 0.5, 0.5))
    if W >= 3 and H >= 3:
        rect(img, 0, 0, W - 1, 0, col(r, 3))
        rect(img, 0, H - 1, W - 1, H - 1, col(r, 0))
    if vertical:
        img = np.ascontiguousarray(np.transpose(img, (1, 0, 2)))
    return img


def _log(r, w, h, rng, arg):
    img = noisy_fill(w, h, r, rng, 0.6, 2.0, cells=(3, 1), dith=0.6)
    vertical = h >= w
    if vertical:
        for x in range(w):
            if rng.random() < 0.35:
                rect(img, x, 0, x, h - 1, col(r, 0))
            elif rng.random() < 0.2:
                rect(img, x, 0, x, h - 1, col(r, 2))
    else:
        for y in range(h):
            if rng.random() < 0.35:
                rect(img, 0, y, w - 1, y, col(r, 0))
    return img


def _rings(r, w, h, rng, arg):
    img = new(w, h, col(r, 3))
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.sqrt((xx - (w - 1) / 2.0) ** 2 + (yy - (h - 1) / 2.0) ** 2)
    ring = (np.floor(d * 1.2 + value_noise(w, h, rng, 3) * 0.8) % 2) == 1
    img[ring] = col(r, 2)
    edge = d >= min(w, h) / 2.0 - 1
    img[edge] = col(r, 1)
    put(img, w // 2, h // 2, col(r, 1))
    return img


def _stone(r, w, h, rng, arg):
    img = noisy_fill(w, h, r, rng, 1.4, 2.9, cells=(4, 2), dith=0.5)
    if w >= 3 and h >= 3:
        bevel(img, 0, 0, w - 1, h - 1, col(r, 3), col(r, 1))
    return img


def _cobble(r, w, h, rng, arg):
    n = max(2, (w * h) // 14)
    pts = np.stack([rng.random(n) * w, rng.random(n) * h], axis=1)
    yy, xx = np.mgrid[0:h, 0:w].astype(float) + 0.5
    d = np.stack([np.hypot(xx - px, yy - py) for px, py in pts])
    srt = np.sort(d, axis=0)
    owner = np.argmin(d, axis=0)
    shade = rng.random(n)
    vals = 1.6 + shade[owner] * 1.2 - 0.05 * srt[0]
    img = shade_img(r, dither(vals - 0.5, 0.6))
    border = (srt[1] - srt[0]) < 0.9
    img[border] = col(r, 0)
    hi = np.zeros_like(border)
    hi[1:, 1:] = border[:-1, :-1] & ~border[1:, 1:]
    img[hi & ~border] = col(r, 3)
    return img


def _brick(r, w, h, rng, arg):
    mortar = ramp("stone")
    img = new(w, h, col(mortar, 2))
    for row, y in enumerate(range(0, h, 4)):
        off = 0 if row % 2 == 0 else 4
        for x in range(-off, w, 8):
            s = int(rng.integers(1, 4))
            rect(img, x, y, x + 6, y + 2, col(r, s))
            rect(img, x, y, x + 6, y, col(r, min(4, s + 1)))
            put(img, x + int(rng.integers(1, 6)), y + 1, col(r, max(0, s - 1)))
    return img


def _glass(r, w, h, rng, arg):
    img = new(w, h, col(r, 2, 150))
    for x in range(w):
        for y in range(h):
            if (x + y) % 7 == 3 or (x + y) % 7 == 4 and (x + 2 * y) % 5 == 0:
                img[y, x] = col(r, 4, 200)
    if w >= 3 and h >= 3:
        bevel(img, 0, 0, w - 1, h - 1, col(r, 3, 255), col(r, 1, 255))
    return img


def _glow(r, w, h, rng, arg):
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.hypot((xx - (w - 1) / 2.0) / max(1, w / 2.0), (yy - (h - 1) / 2.0) / max(1, h / 2.0))
    vals = 4.2 - 1.6 * d + (rng.random((h, w)) - 0.5) * 0.5
    img = shade_img(r, dither(vals - 0.5, 0.5).clip(2, 4))
    core = d < 0.35
    img[core] = mix(col(r, 4), (255, 255, 255, 255), 0.5)
    return img


def _coil(r, w, h, rng, arg):
    vertical = arg == "v" or (arg == "" and h < w)
    W, H = (h, w) if vertical else (w, h)
    img = new(W, H)
    for y in range(H):
        k = y % 3
        rect(img, 0, y, W - 1, y, col(r, (3, 2, 0)[k]))
        if k == 0 and W > 2:
            for x in range(int(rng.integers(0, 3)), W, 5):
                put(img, x, y, col(r, 4))
    if vertical:
        img = np.ascontiguousarray(np.transpose(img, (1, 0, 2)))
    return img


def _grate(r, w, h, rng, arg):
    img = new(w, h)
    for y in range(h):
        for x in range(w):
            if x % 3 == 0 or y % 3 == 0 or x == w - 1 or y == h - 1:
                img[y, x] = col(r, 3 if (x % 3 == 0 and y % 3 == 0) else (2 if x % 3 == 0 else 1))
    return img


def _canvas(r, w, h, rng, arg):
    vals = 2.6 + 0.6 * (value_noise(w, h, rng, 6) - 0.5)
    yy, xx = np.mgrid[0:h, 0:w]
    vals = vals + 0.35 * (((xx + yy) % 2) - 0.5)
    img = shade_img(r, dither(vals - 0.5, 0.4))
    if w >= 3 and h >= 3:
        rect(img, 0, 0, w - 1, 0, col(r, 1))
        rect(img, 0, h - 1, w - 1, h - 1, col(r, 1))
    return img


def _rope(r, w, h, rng, arg):
    img = new(w, h)
    for y in range(h):
        for x in range(w):
            img[y, x] = col(r, (3, 2, 1, 2)[(x + y) % 4])
    return img


def _hazard(r, w, h, rng, arg):
    hz = ramp("hazard")
    img = new(w, h)
    for y in range(h):
        for x in range(w):
            on = ((x + y) // 3) % 2 == 0
            img[y, x] = col(hz, 3 if on else 1)
    img[(rng.random((h, w)) < 0.08)] = col(hz, 2)
    return img


def _rubber(r, w, h, rng, arg):
    img = noisy_fill(w, h, r, rng, 0.6, 2.2, cells=(2, 1), dith=1.0)
    return img


def _gauge(r, w, h, rng, arg):
    img = plate(w, h, r, rng)
    face = ramp("ceramic")
    rad = min(w, h) / 2.0 - 1.0
    if rad < 1.5:
        return img
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    ring = disc_mask(w, h, cx, cy, rad + 0.6)
    img[ring] = col(ramp("brass"), 3)
    img[disc_mask(w, h, cx, cy, rad - 0.4)] = col(face, 3)
    line(img, int(round(cx)), int(round(cy)), int(round(cx + rad * 0.6)), int(round(cy - rad * 0.6)), col(ramp("red_paint"), 2))
    put(img, int(round(cx)), int(round(cy)), col(face, 0))
    return img


def _screen(r, w, h, rng, arg):
    lit = ramp(arg or "#4FE3FF")
    img = new(w, h, col(ramp("rubber"), 1))
    for y in range(1, h - 1, 2):
        n = int(rng.integers(1, max(2, w - 1)))
        rect(img, 1, y, min(w - 2, n), y, col(lit, 3))
    if w >= 3 and h >= 3:
        bevel(img, 0, 0, w - 1, h - 1, col(r, 1), col(r, 3))
    return img


def _vent(r, w, h, rng, arg):
    img = plate(w, h, r, rng)
    for y in range(2, h - 2, 2):
        rect(img, 2, y, w - 3, y, col(r, 0))
    return img


def _crystal(r, w, h, rng, arg):
    yy, xx = np.mgrid[0:h, 0:w]
    facet = ((xx * 2 + yy * 3 + rng.integers(0, 5)) // 4) % 3
    vals = 2.2 + facet * 0.7 + (rng.random((h, w)) - 0.5) * 0.3
    img = shade_img(r, dither(vals - 0.5, 0.4))
    for _ in range(max(1, w * h // 20)):
        put(img, int(rng.integers(0, w)), int(rng.integers(0, h)), (255, 255, 255, 255))
    return img


def _water(r, w, h, rng, arg):
    n = value_noise(w, h, rng, 3)
    yy = np.arange(h)[:, None]
    vals = 2.0 + 0.8 * n + 0.6 * (np.sin(yy * 1.6 + n * 4.0) > 0.7)
    img = shade_img(r, dither(vals - 0.5, 0.6), alpha=190)
    return img


def _lens(r, w, h, rng, arg):
    img = new(w, h, col(r, 1))
    yy, xx = np.mgrid[0:h, 0:w]
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    d = np.hypot(xx - cx, yy - cy)
    rad = min(w, h) / 2.0
    for k, s in ((1.0, 1), (0.8, 2), (0.55, 3), (0.3, 4)):
        img[d <= rad * k] = col(r, s)
    put(img, int(cx - rad * 0.35), int(cy - rad * 0.35), (255, 255, 255, 255))
    return img


def _grille(r, w, h, rng, arg):
    img = new(w, h, col(r, 0))
    for y in range(0, h, 2):
        rect(img, 0, y, w - 1, y, col(r, 2))
        if w > 2:
            put(img, 0, y, col(r, 3))
    return img


def _hatch(r, w, h, rng, arg):
    img = plate(w, h, r, rng)
    if w >= 6 and h >= 6:
        bevel(img, 2, 2, w - 3, h - 3, col(r, 1), col(r, 3))
        rect(img, w - 5, h // 2 - 1, w - 4, h // 2, col(r, 4))
        put(img, w - 4, h // 2 + 1, col(r, 0))
    return img


def _energy(r, w, h, rng, arg):
    n = value_noise(w, h, rng, 3)
    yy, xx = np.mgrid[0:h, 0:w]
    swirl = np.sin((xx + yy) * 0.9 + n * 6.0)
    vals = 2.6 + 1.2 * swirl + (rng.random((h, w)) - 0.5) * 0.4
    img = shade_img(r, dither(vals - 0.5, 0.6).clip(1, 4))
    img[(swirl > 0.92)] = (255, 255, 255, 255)
    return img


def _seam(r, w, h, rng, arg):
    img = plate(w, h, r, rng, base=1.0, spread=0.4)
    g = ramp(arg or "#7FFFF0")
    if w >= 4:
        rect(img, w // 2, 1, w // 2, h - 2, col(g, 3))
    if h >= 6:
        rect(img, 1, h // 2, w - 2, h // 2, col(g, 3))
    if w >= 4 and h >= 4:
        rect(img, 0, 0, w - 1, 0, col(r, 3))
    return img


def _bands(r, w, h, rng, arg):
    other = ramp(arg or "ceramic")
    img = new(w, h)
    for y in range(h):
        c = col(r, 2) if (y // 3) % 2 == 0 else col(other, 3)
        rect(img, 0, y, w - 1, y, c)
    if w >= 3:
        rect(img, 0, 0, 0, h - 1, mix(img[0, 1], (255, 255, 255, 255), 0.25))
    return img


def _frost(r, w, h, rng, arg):
    img = noisy_fill(w, h, r, rng, 2.0, 4.0, cells=(3, 2), dith=1.0)
    for _ in range(max(1, w * h // 12)):
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
        put(img, x, y, (255, 255, 255, 255))
    return img


def _hex(r, w, h, rng, arg):
    img = new(w, h, col(r, 1))
    yy, xx = np.mgrid[0:h, 0:w]
    q = (xx + (yy // 3) % 2 * 2) % 4
    edge = (q == 0) | (yy % 3 == 0)
    img[~edge] = col(r, 3)
    img[(~edge) & (q == 1) & (yy % 3 == 1)] = col(r, 4)
    img[edge] = col(r, 0)
    return img


def _leather(r, w, h, rng, arg):
    img = noisy_fill(w, h, r, rng, 1.2, 2.6, cells=(2, 1), dith=1.0)
    if w >= 4 and h >= 4:
        for x in range(1, w - 1, 2):
            put(img, x, 1, col(r, 3))
            put(img, x, h - 2, col(r, 3))
    return img


def _lamp(r, w, h, rng, arg):
    img = _glow(r, w, h, rng, arg)
    if w >= 3 and h >= 3:
        cage = ramp(arg or "steel")
        bevel(img, 0, 0, w - 1, h - 1, col(cage, 2), col(cage, 1))
    return img


FINISHES = {
    "plate": _plate, "rivet": _rivet, "panel": _panel, "flat": _flat, "dark": _dark, "wood": _wood,
    "timber": _timber, "log": _log, "rings": _rings, "stone": _stone, "cobble": _cobble, "brick": _brick,
    "glass": _glass, "glow": _glow, "coil": _coil, "grate": _grate, "canvas": _canvas, "rope": _rope,
    "hazard": _hazard, "rubber": _rubber, "gauge": _gauge, "screen": _screen, "vent": _vent,
    "crystal": _crystal, "water": _water, "lens": _lens, "grille": _grille, "hatch": _hatch,
    "energy": _energy, "seam": _seam, "bands": _bands, "frost": _frost, "hex": _hex, "leather": _leather,
    "lamp": _lamp,
}


def _pane(r, w, h, rng, arg):
    img = new(w, h)
    if w >= 3 and h >= 3:
        off = int(rng.integers(0, max(1, w)))
        for x in range(w):
            for y in range(h):
                d = (x + y + off) % (w + h)
                if d == 2:
                    img[y, x] = (255, 255, 255, 255)
                elif d in (1, 3):
                    img[y, x] = col(r, 4)
    if arg == "frame" and w >= 3 and h >= 3:
        bevel(img, 0, 0, w - 1, h - 1, col(r, 3), col(r, 1))
    return img


FINISHES["pane"] = _pane


def _chain(r, w, h, rng, arg):
    vertical = h > w
    W, H = (h, w) if vertical else (w, h)
    img = new(W, H, col(r, 1))
    for x in range(W):
        link = x % 2 == 0
        rect(img, x, 0, x, H - 1, col(r, 3 if link else 1))
        if link and H >= 3:
            put(img, x, 0, col(r, 4))
            put(img, x, H - 1, col(r, 0))
    if vertical:
        img = np.ascontiguousarray(np.transpose(img, (1, 0, 2)))
    return img


FINISHES["chain"] = _chain


def _polar(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(float)
    dx, dy = xx + 0.5 - w / 2.0, yy + 0.5 - h / 2.0
    return np.hypot(dx, dy), min(w, h) / 2.0, dx, dy


def _rim(img, r, d, rad, dx, dy, width):
    band = (d <= rad) & (d > rad - width)
    lit = (dx + dy) < 0
    img[band & lit] = col(r, 3)
    img[band & ~lit] = col(r, 1)
    edge = (d <= rad) & (d > rad - 0.75)
    img[edge & ((dx + dy) < -rad * 0.9)] = col(r, 4)


def _roundel(r, w, h, rng, arg):
    d, rad, dx, dy = _polar(w, h)
    img = noisy_fill(w, h, r, rng, 1.5, 2.6, cells=(4, 2), dith=0.4)
    groove = np.abs(d - rad * 0.62) < 0.5
    img[groove] = col(r, 1)
    img[np.abs(d - rad * 0.62 - 0.9) < 0.45] = col(r, 3)
    _rim(img, r, d, rad, dx, dy, max(1.0, rad / 9.0))
    img[d < max(1.0, rad * 0.16)] = col(r, 3)
    img[d > rad] = CLEAR
    return img


def _spoked(r, w, h, rng, arg):
    n = int(arg) if arg.isdigit() else 6
    d, rad, dx, dy = _polar(w, h)
    img = new(w, h)
    sw = max(0.75, rad / 10.0)
    for k in range(n):
        a = 2 * np.pi * k / n + np.pi / 2
        along = dx * np.cos(a) + dy * np.sin(a)
        off = np.abs(-dx * np.sin(a) + dy * np.cos(a))
        sp = (off < sw) & (along > 0) & (d <= rad)
        img[sp] = col(r, 2)
        img[sp & (off < sw * 0.4) & ((dx + dy) < 0)] = col(r, 3)
    _rim(img, r, d, rad, dx, dy, max(1.0, rad / 6.0))
    hub = d < max(1.5, rad * 0.22)
    img[hub] = col(r, 2)
    img[hub & (d < max(0.8, rad * 0.1))] = col(r, 4)
    img[hub & (d > max(1.5, rad * 0.22) - 0.8) & ((dx + dy) > 0)] = col(r, 1)
    return img


def _cog(r, w, h, rng, arg):
    d, rad, dx, dy = _polar(w, h)
    img = noisy_fill(w, h, r, rng, 1.6, 2.6, cells=(3, 2), dith=0.4)
    body = rad * 0.74
    teeth = max(6, int(round(2 * np.pi * rad / 3.2)))
    ang = (np.arctan2(dy, dx) / (2 * np.pi) * teeth) % 1.0
    keep = (d <= body) | ((d <= rad) & (ang < 0.5))
    lit = (dx + dy) < 0
    img[(d > body - 0.9) & keep & lit] = col(r, 3)
    img[(d > body - 0.9) & keep & ~lit] = col(r, 1)
    img[np.abs(d - rad * 0.45) < 0.5] = col(r, 1)
    img[d < max(1.0, rad * 0.18)] = col(r, 4)
    img[~keep] = CLEAR
    return img


FINISHES["roundel"] = _roundel
FINISHES["spoked"] = _spoked
FINISHES["cog"] = _cog


def _foam(r, w, h, rng, arg):
    img = noisy_fill(w, h, r, rng, 2.4, 3.6, cells=(3, 2), dith=0.6)
    fleck = ramp(arg) if arg else r
    for _ in range(max(1, w * h // 8)):
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
        put(img, x, y, col(r, 4))
        put(img, x + 1, y + 1, col(r, 1))
    for _ in range(max(1, w * h // 24)):
        put(img, int(rng.integers(0, w)), int(rng.integers(0, h)), col(fleck, 2))
    return img


FINISHES["foam"] = _foam


def _glint(r, w, h, rng, arg):
    img = new(w, h)
    if w >= 3 and h >= 3:
        rect(img, 1, 0, w - 2, 0, col(r, 3))
        put(img, 1, 1, col(r, 4))
        put(img, 2, 1, col(r, 3))
        put(img, 1, 2, col(r, 3))
        put(img, w - 2, h - 2, col(r, 2))
    return img


FINISHES["glint"] = _glint


def _shell(r, w, h, rng, arg):
    base = float(arg) if arg else 2.4
    img = plate(w, h, r, rng, base=base, spread=0.2)
    if w >= 3 and h >= 3:
        rect(img, 1, 0, w - 2, 0, col(r, min(4, int(base) + 2)))
        if w >= 8 and h >= 6:
            for x, y in ((1, 2), (w - 3, 2), (1, h - 3), (w - 3, h - 3)):
                rivet(img, x, y, r)
    return img


FINISHES["shell"] = _shell


def paint_face(mat: str, group: str, w: int, h: int, rng: np.random.Generator, r: np.ndarray | None = None) -> np.ndarray:
    """Paint one atlas patch for material 'palette:finish[:arg]'.

    Args:
        group: 'side' or 'top'; 'log' faces become end-grain rings on 'top'.
        r: ramp override (rig templates paint grey ramps whatever the palette name).
    """
    pal, finish, arg = mat_parts(mat)
    if pal == "aegis" or (pal == "hazard" and finish != "hazard"):
        raise ValueError(f"material '{mat}': '{pal}' is a two-tone palette; use aegis_body, aegis_gold or the hazard finish")
    if finish not in FINISHES:
        raise KeyError(f"unknown finish '{finish}' in material '{mat}'; known: {sorted(FINISHES)}")
    rr = ramp(pal) if r is None else r
    if finish == "log" and group == "top":
        finish = "rings"
    img = FINISHES[finish](rr, w, h, rng, arg)
    if img.shape != (h, w, 4):
        img = np.ascontiguousarray(img[:h, :w])
    out = new(w, h, CLEAR)
    out[:img.shape[0], :img.shape[1]] = img
    return out

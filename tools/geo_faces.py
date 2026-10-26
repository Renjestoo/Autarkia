"""Face finishes for GeckoLib atlases: each paints one (h, w, 4) patch from a palette ramp.

A material string is 'palette:finish[:arg]'; palette is a PALETTES name or '#RRGGBB'. Finishes paint tone
maps onto hue-shifted n-shade ramps (geo_faces_metal) in the vanilla manner: soft 2-3px clusters, no ordered
dithering, structure shaded in the material's own hue. Every finish copes with any size from 1x1 upwards.
This module holds the wood, mineral, glass, light and cloth finishes; geo_faces_metal the machined metals.
"""

import numpy as np

from geo_core import mat_parts
from geo_faces_metal import LUMA, METAL, hex_ramp, mottle, paint, rim, streaks, tcol, tones
from paint import CLEAR, new, ramp


def _turn(img: np.ndarray, vertical: bool) -> np.ndarray:
    return np.ascontiguousarray(np.transpose(img, (1, 0, 2))) if vertical else img


def _wood(r, w, h, rng, arg):
    vertical = arg == "v" or (arg != "h" and h > w)
    W, H = (h, w) if vertical else (w, h)
    vals = 0.52 + (streaks(W, H, rng, 9.0, 1.0) - 0.5) * 0.22 + mottle(W, H, rng, 0.03)
    for y0 in range(0, H, 4):
        vals[y0:y0 + 4] += rng.uniform(-0.06, 0.06)
        vals[y0] += 0.05
        if y0 + 3 < H:
            vals[y0 + 3] = 0.3 + (vals[y0 + 3] - 0.5) * 0.3
        if W >= 6:
            j = int(rng.integers(1, W - 1))
            vals[y0:min(H, y0 + 3), j] = 0.34
            vals[y0:min(H, y0 + 3), min(W - 1, j + 1)] += 0.06
    return _turn(paint(tones(r), vals), vertical)


def _timber(r, w, h, rng, arg):
    vertical = h > w
    W, H = (h, w) if vertical else (w, h)
    vals = 0.5 + (streaks(W, H, rng, 10.0, 1.0) - 0.5) * 0.3 + mottle(W, H, rng, 0.04)
    if H >= 3:
        vals[0] += 0.12
        vals[-1] -= 0.15
    return _turn(paint(tones(r), vals), vertical)


def _log(r, w, h, rng, arg):
    vertical = h >= w
    W, H = (h, w) if not vertical else (w, h)
    bark = streaks(W, H, rng, 1.6, 7.0)
    vals = np.where(bark < 0.38, 0.14 + bark * 0.3, 0.36 + (bark - 0.38) * 0.45) + mottle(W, H, rng, 0.04)
    ridge = (bark >= 0.38) & (np.roll(bark, 1, axis=1) < 0.38)
    vals[ridge] += 0.12
    img = paint(tones(r), vals)
    return img if vertical else np.ascontiguousarray(np.transpose(img, (1, 0, 2)))


def _rings(r, w, h, rng, arg):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    ax, ay = np.abs(xx - (w - 1) / 2.0), np.abs(yy - (h - 1) / 2.0)
    edge = np.minimum((w - 1) / 2.0 - ax, (h - 1) / 2.0 - ay)
    depth = min(w, h) / 2.0 - edge + 0.25 * np.minimum(ax, ay) / max(1.0, min(w, h) / 2.0)
    wob = mottle(w, h, rng, 0.5, (3, 2), 0.0)
    vals = 0.62 + 0.08 * np.cos(depth * 1.9 + wob * 1.5) + mottle(w, h, rng, 0.03)
    vals[edge < 1.5] -= 0.1
    bark = edge < 0.5
    vals[bark] = 0.26 + mottle(w, h, rng, 0.06)[bark]
    vals[np.hypot(ax, ay) < 0.8] = 0.48
    return paint(tones(r), vals)


def _stone(r, w, h, rng, arg):
    vals = 0.5 + mottle(w, h, rng, 0.14) + (streaks(w, h, rng, 4.0, 2.0) - 0.5) * 0.12
    rim(vals, 0.06, 0.1)
    return paint(tones(r), vals)


def _cobble(r, w, h, rng, arg):
    n = max(2, (w * h) // 22)
    pts = np.stack([rng.random(n) * w, rng.random(n) * h], axis=1)
    yy, xx = np.mgrid[0:h, 0:w].astype(float) + 0.5
    d = np.stack([np.hypot(xx - px, yy - py) for px, py in pts])
    srt, owner = np.sort(d, axis=0), np.argmin(d, axis=0)
    ox, oy = xx - pts[owner, 0], yy - pts[owner, 1]
    vals = 0.42 + rng.uniform(0.0, 0.22, n)[owner] - 0.05 * (ox + oy) / 2.0 + mottle(w, h, rng, 0.04)
    border = (srt[1] - srt[0]) < 0.9
    hi = np.zeros_like(border)
    hi[1:, 1:] = border[:-1, :-1] & ~border[1:, 1:]
    vals[hi] += 0.1
    vals[border] = 0.16 + (rng.random(int(border.sum())) - 0.5) * 0.06
    return paint(tones(r), vals)


def _brick(r, w, h, rng, arg):
    vals = np.zeros((h, w))
    mortar = np.zeros((h, w), dtype=bool)
    for row, y in enumerate(range(0, h, 4)):
        mortar[min(h - 1, y + 3)] = True
        for x in range(-(4 if row % 2 else 0), w, 8):
            xa, xb = max(0, x), min(w, x + 7)
            if xa < xb:
                vals[y:y + 3, xa:xb] = rng.uniform(0.38, 0.6)
                vals[y, xa:xb] += 0.1
                vals[min(h - 1, y + 2), xa:xb] -= 0.08
            if 0 <= x + 7 < w:
                mortar[y:y + 3, x + 7] = True
    img = paint(tones(r), vals + mottle(w, h, rng, 0.06))
    joint = paint(tones(ramp("stone")), 0.66 + mottle(w, h, rng, 0.05))
    img[mortar] = joint[mortar]
    return img


def _glass(r, w, h, rng, arg):
    R = tones(r, mute=0.05)
    yy, xx = np.mgrid[0:h, 0:w]
    vals = 0.55 + mottle(w, h, rng, 0.04)
    img = paint(R, vals, alpha=150)
    period = max(12, (w + h) // 2)
    k = (xx + yy + int(rng.integers(0, period))) % period
    glint = (k < 2) | (k == 4)
    img[glint] = paint(R, vals + 0.32, alpha=205)[glint]
    if w >= 3 and h >= 3:
        f = vals.copy()
        f[0, :], f[:, 0] = 0.78, 0.72
        f[-1, :], f[:, -1] = 0.36, 0.4
        frame = np.zeros((h, w), dtype=bool)
        frame[0, :] = frame[-1, :] = frame[:, 0] = frame[:, -1] = True
        img[frame] = paint(R, f)[frame]
    return img


def _pane(r, w, h, rng, arg):
    R = tones(r, mute=0.05)
    img = new(w, h)
    if w >= 3 and h >= 3:
        off = int(rng.integers(0, max(1, w)))
        for x in range(w):
            for y in range(h):
                k = (x + y + off) % (w + h)
                if k in (2, 3):
                    img[y, x] = tcol(R, 1.0 if k == 2 else 0.86)
                elif k == 6:
                    img[y, x] = tcol(R, 0.8)
    if arg == "frame" and w >= 3 and h >= 3:
        for x in range(w):
            img[0, x], img[h - 1, x] = tcol(R, 0.74), tcol(R, 0.36)
        for y in range(1, h - 1):
            img[y, 0], img[y, w - 1] = tcol(R, 0.68), tcol(R, 0.4)
    return img


def _hot(r: np.ndarray) -> np.ndarray:
    """Return the emissive ramp of r: a muted, darker outer range like a sea lantern rim, the full colour in the
    middle, and three shades fading towards white on top."""
    full = tones(r, shift=0.12, mute=0.12).astype(np.float64)
    soft = tones(r, shift=0.3, mute=0.6).astype(np.float64) * 0.82
    t = np.clip((np.linspace(0.0, 1.0, len(full)) - 0.3) / 0.5, 0.0, 1.0)[:, None]
    R = soft * (1 - t) + full * t
    top = R[-1]
    return np.rint(np.vstack([R, [top + (255 - top) * k for k in (0.18, 0.34, 0.5)]])).astype(np.int64)


def _glow(r, w, h, rng, arg):
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.hypot((xx - (w - 1) / 2.0) / max(1, w / 2.0), (yy - (h - 1) / 2.0) / max(1, h / 2.0))
    vals = 0.97 - 0.68 * d ** 1.3 + mottle(w, h, rng, 0.06, (3, 2), 0.01)
    return paint(_hot(r), np.clip(vals, 0.24, 1.0))


def _lamp(r, w, h, rng, arg):
    img = _glow(r, w, h, rng, arg)
    if w >= 3 and h >= 3:
        cage = tones(ramp(arg or "steel"))
        for x in range(w):
            img[0, x], img[h - 1, x] = tcol(cage, 0.62), tcol(cage, 0.3)
        for y in range(1, h - 1):
            img[y, 0], img[y, w - 1] = tcol(cage, 0.55), tcol(cage, 0.34)
    return img


def _energy(r, w, h, rng, arg):
    yy, xx = np.mgrid[0:h, 0:w]
    swirl = np.sin((xx + yy) * 0.55 + mottle(w, h, rng, 0.5, (4, 2), 0.0) * 5.0)
    vals = 0.62 + 0.26 * swirl + mottle(w, h, rng, 0.03)
    return paint(_hot(r), np.clip(vals, 0.42, 1.0))


def _water(r, w, h, rng, arg):
    n = streaks(w, h, rng, 5.0, 2.0)
    vals = 0.42 + 0.22 * n + mottle(w, h, rng, 0.04)
    yy = np.arange(h)[:, None]
    vals = vals + 0.2 * (np.sin(yy * 1.4 + n * 4.0) > 0.82)
    return paint(tones(r, mute=0.1), vals, alpha=190)


def _crystal(r, w, h, rng, arg):
    R = tones(r, mute=0.05)
    n = max(2, (w * h) // 18)
    pts = np.stack([rng.random(n) * w, rng.random(n) * h], axis=1)
    yy, xx = np.mgrid[0:h, 0:w].astype(float) + 0.5
    d = np.stack([np.hypot(xx - px, yy - py) for px, py in pts])
    srt, owner = np.sort(d, axis=0), np.argmin(d, axis=0)
    vals = 0.52 + rng.uniform(-0.12, 0.16, n)[owner] - 0.05 * ((xx - pts[owner, 0]) + (yy - pts[owner, 1]))
    ridge = (srt[1] - srt[0]) < 0.8
    vals[ridge] += 0.12
    vals = vals + mottle(w, h, rng, 0.03)
    for _ in range(max(1, w * h // 45)):
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
        vals[y, x] = 1.0
    return paint(R, vals)


def _lens(r, w, h, rng, arg):
    R = tones(r, mute=0.05)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    rad = max(1.0, min(w, h) / 2.0)
    d = np.hypot(xx - cx, yy - cy)
    vals = 0.6 - 0.22 * ((xx - cx) + (yy - cy)) / rad - 0.1 * d / rad
    vals[d >= rad - 0.6] = 0.2
    img = paint(R, vals)
    hx, hy = round(cx - rad * 0.35), round(cy - rad * 0.35)
    if 0 <= hx < w and 0 <= hy < h:
        img[hy, hx] = tcol(R, 1.0)
    return img


def _canvas(r, w, h, rng, arg):
    vals = 0.62 + mottle(w, h, rng, 0.06) + (streaks(w, h, rng, 5.0, 1.0) - 0.5) * 0.08
    vals = vals + (streaks(w, h, rng, 1.0, 5.0) - 0.5) * 0.06
    if w >= 3 and h >= 3:
        vals[0] -= 0.12
        vals[1] += 0.04
        vals[-1] -= 0.15
    return paint(tones(r), vals)


def _rope(r, w, h, rng, arg):
    yy, xx = np.mgrid[0:h, 0:w]
    vals = 0.55 + 0.16 * np.cos(2 * np.pi * (xx - 2 * yy) / 6.0) + mottle(w, h, rng, 0.04)
    if h >= 3:
        vals[0] -= 0.08
        vals[-1] -= 0.14
    return paint(tones(r), vals)


def _hazard(r, w, h, rng, arg):
    hz = ramp("hazard")
    yellow, black = tones(hex_ramp(hz[2])), tones(hex_ramp(hz[1]))
    yy, xx = np.mgrid[0:h, 0:w]
    on = ((xx + yy) // 4) % 2 == 0
    vals = mottle(w, h, rng, 0.06)
    if h > 1:
        vals = vals + np.linspace(0.05, -0.05, h)[:, None]
    rim(vals, 0.06, 0.1)
    wear = mottle(w, h, rng, 0.5, (3, 2), 0.0) > 0.36
    img = paint(black, 0.42 + vals)
    stripe = on & ~wear
    img[stripe] = paint(yellow, 0.56 + vals)[stripe]
    scuff = on & wear
    img[scuff] = paint(yellow, 0.3 + vals)[scuff]
    return img


def _rubber(r, w, h, rng, arg):
    vals = 0.45 + mottle(w, h, rng, 0.11)
    rim(vals, 0.06, 0.1)
    return paint(tones(r), vals)


def _bands(r, w, h, rng, arg):
    other = ramp(arg or "ceramic")
    yy = np.mgrid[0:h, 0:w][0]
    vals = mottle(w, h, rng, 0.03) + np.linspace(0.1, -0.1, w)[None, :] - 0.1 * ((yy % 3 == 0) & (yy > 0))
    first = (yy // 3) % 2 == 0
    img = paint(tones(other), 0.7 + vals)
    img[first] = paint(tones(r), 0.5 + vals)[first]
    return img


def _frost(r, w, h, rng, arg):
    vals = 0.68 + mottle(w, h, rng, 0.12) + (streaks(w, h, rng, 3.0, 3.0) - 0.5) * 0.08
    for _ in range(max(1, w * h // 30)):
        vals[int(rng.integers(0, h)), int(rng.integers(0, w))] = 1.0
    return paint(tones(r, mute=0.05), vals)


def _leather(r, w, h, rng, arg):
    vals = 0.5 + mottle(w, h, rng, 0.09, (3, 2), 0.04)
    if w >= 4 and h >= 4:
        vals[0] -= 0.1
        vals[-1] -= 0.12
        for x in range(1, w - 1):
            if x % 3 != 0:
                vals[1, x] = vals[h - 2, x] = 0.78
    return paint(tones(r), vals)


def _foam(r, w, h, rng, arg):
    vals = 0.68 + mottle(w, h, rng, 0.06)
    for _ in range(max(1, w * h // 12)):
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
        for dx, dy, t in ((0, 0, 0.92), (1, 0, 0.8), (0, 1, 0.8), (1, 1, 0.56), (2, 2, 0.5)):
            if x + dx < w and y + dy < h:
                vals[y + dy, x + dx] = t
    img = paint(tones(r), vals)
    fleck = tones(ramp(arg)) if arg else tones(r)
    for _ in range(max(1, w * h // 36)):
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
        img[y, x] = tcol(fleck, 0.55)
        if x + 1 < w:
            img[y, x + 1] = tcol(fleck, 0.35)
    return img


def _glint(r, w, h, rng, arg):
    R = tones(r, mute=0.05)
    img = new(w, h)
    if w >= 3 and h >= 3:
        for x in range(1, w - 1):
            img[0, x] = tcol(R, 0.78)
        img[1, 1], img[1, 2], img[2, 1] = tcol(R, 1.0), tcol(R, 0.84), tcol(R, 0.8)
        img[h - 2, w - 2] = tcol(R, 0.6)
    return img


FINISHES = dict(METAL)
FINISHES.update({
    "wood": _wood, "timber": _timber, "log": _log, "rings": _rings, "stone": _stone, "cobble": _cobble,
    "brick": _brick, "glass": _glass, "pane": _pane, "glow": _glow, "lamp": _lamp, "energy": _energy,
    "water": _water, "crystal": _crystal, "lens": _lens, "canvas": _canvas, "rope": _rope, "hazard": _hazard,
    "rubber": _rubber, "bands": _bands, "frost": _frost, "leather": _leather, "foam": _foam, "glint": _glint,
})


BODY_MUTE = {"brass": 0.16, "aegis_gold": 0.2, "lattice": 0.22}
"""Extra pull towards grey for saturated body palettes, applied to their 5 keys before a non-emissive finish."""
EMISSIVE = {"glow", "lamp", "energy", "crystal", "lens", "pane", "glint"}


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
    if pal in BODY_MUTE and finish not in EMISSIVE:
        rr = rr + ((rr.astype(np.float64) @ LUMA)[:, None] - rr) * BODY_MUTE[pal]
    if finish == "log" and group == "top":
        finish = "rings"
    img = FINISHES[finish](rr, w, h, rng, arg)
    if img.shape != (h, w, 4):
        img = np.ascontiguousarray(img[:h, :w])
    out = new(w, h, CLEAR)
    out[:img.shape[0], :img.shape[1]] = img
    return out

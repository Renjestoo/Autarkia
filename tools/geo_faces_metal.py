"""Tone-map primitives and the machined-metal face finishes for GeckoLib atlases.

A finish builds a float tone map (0 darkest, 1 lightest) and maps it onto a hue-shifted, muted n-shade
ramp, so shading spreads over every shade without ordered dithering. Structure (rims, seams, studs,
slots) is drawn as tone offsets in the material's own hue, lit from the top left.
"""

import numpy as np

from paint import COOL_SHADOW, WARM_LIGHT, fbm, line_pts, ramp, shade_n

SHADES = 16
LUMA = np.array([0.299, 0.587, 0.114])


def tones(r: np.ndarray, n: int = SHADES, shift: float = 0.3, mute: float = 0.15) -> np.ndarray:
    """Return an (n, 3) ramp resampled from 5-shade ramp r: cool shadows, warm lights, muted towards grey.

    A ramp whose four lower keys are neutral and whose top key is saturated (aegis_body) keeps that top key
    as an accent on its two brightest shades only; the other shades span a netherite-dark grey range built
    from its fourth key.
    """
    keys = r.astype(np.float64)
    sat = keys.max(axis=1) - keys.min(axis=1)
    accent = keys[4].copy() if sat[:4].max() < 12 and sat[4] > 60 else None
    if accent is not None:
        dark = hex_ramp(keys[3]).astype(np.float64)
        pos = np.linspace(0.1, 0.78, 5)
        keys = np.stack([np.interp(pos, np.linspace(0.0, 1.0, 5), dark[:, c]) for c in range(3)], axis=1)
    t = np.linspace(0.0, 1.0, n)
    rgb = np.stack([np.interp(t, np.linspace(0.0, 1.0, 5), keys[:, c]) for c in range(3)], axis=1)
    rgb = rgb + ((rgb @ LUMA)[:, None] - rgb) * mute
    k_dark = np.clip(0.5 - t, 0, None)[:, None] * shift
    k_light = np.clip(t - 0.5, 0, None)[:, None] * shift * 0.8
    rgb = rgb * (1 - k_dark) + COOL_SHADOW * k_dark
    rgb = rgb * (1 - k_light) + WARM_LIGHT * k_light
    if accent is not None:
        rgb[-2], rgb[-1] = accent, accent + (255 - accent) * 0.3
    return np.clip(np.rint(rgb), 0, 255).astype(np.int64)


def hex_ramp(c) -> np.ndarray:
    """Return the 5-shade ramp that paint.ramp expands from one RGB colour."""
    return ramp("#%02X%02X%02X" % tuple(int(v) for v in c[:3]))


def tcol(R: np.ndarray, t: float, a: int = 255) -> tuple[int, int, int, int]:
    """Return the RGBA colour of tone t (0..1) on ramp R."""
    c = R[int(np.clip(round(t * (len(R) - 1)), 0, len(R) - 1))]
    return int(c[0]), int(c[1]), int(c[2]), a


def paint(R: np.ndarray, vals: np.ndarray, mask: np.ndarray | None = None, alpha: int = 255) -> np.ndarray:
    """Map a tone map onto ramp R; pixels outside mask stay clear, the rest get the given alpha."""
    img = shade_n(np.clip(vals, 0.0, 1.0), R, mask)
    if alpha != 255:
        img[..., 3] = np.where(img[..., 3] > 0, alpha, 0)
    return img


def mottle(w: int, h: int, rng: np.random.Generator, amp: float, cells=(3, 2), jitter: float = 0.02) -> np.ndarray:
    """Return zero-centred soft cluster noise of amplitude amp plus a faint per-pixel jitter."""
    n = fbm(w, h, rng, cells, (0.6, 0.4, 0.2)[:len(cells)])
    return (n - 0.5) * 2 * amp + (rng.random((h, w)) - 0.5) * 2 * jitter


def streaks(w: int, h: int, rng: np.random.Generator, cx: float, cy: float) -> np.ndarray:
    """Return smooth noise in [0, 1] with features about cx by cy pixels (grain, brushing, bark)."""
    gw, gh = int(np.ceil(w / cx)) + 2, int(np.ceil(h / cy)) + 2
    grid = rng.random((gh, gw))
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    fx, fy = xx / cx, yy / cy
    ix, iy = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - ix, fy - iy
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = grid[iy, ix] * (1 - tx) + grid[iy, ix + 1] * tx
    b = grid[iy + 1, ix] * (1 - tx) + grid[iy + 1, ix + 1] * tx
    return a * (1 - ty) + b * ty


def rim(vals: np.ndarray, light: float = 0.1, dark: float = 0.13) -> None:
    """Lighten the top and left rows and darken the bottom and right rows in place (3x3 patches and up)."""
    h, w = vals.shape
    if w >= 3 and h >= 3:
        vals[0, :] += light
        vals[1:, 0] += light * 0.6
        vals[-1, :] -= dark
        vals[:-1, -1] -= dark * 0.75


def sheen(w: int, h: int, rng: np.random.Generator, cell: float = 3.2) -> np.ndarray:
    """Return smooth noise in [0, 1] that only varies across the '/' diagonal: soft polished-metal streaks."""
    u = (np.arange(w)[None, :] + np.arange(h)[:, None]) / cell
    grid = rng.random(int(np.ceil((w + h) / cell)) + 2)
    i = np.floor(u).astype(int)
    t = u - i
    t = t * t * (3 - 2 * t)
    return grid[i] * (1 - t) + grid[i + 1] * t


def metal(w: int, h: int, rng: np.random.Generator, base: float = 0.5, spread: float = 0.1) -> np.ndarray:
    """Return a polished-metal tone map: diagonal sheen streaks, broad soft blotches, faint horizontal brushing,
    small clusters and a top-left-lit gradient, so large faces keep tonal variation."""
    u = (np.arange(w)[None, :] + np.arange(h)[:, None]) / max(1, w + h - 2)
    broad = fbm(w, h, rng, (8, 4), (0.6, 0.4))
    return (base + (sheen(w, h, rng) - 0.5) * 2 * spread + (broad - 0.5) * 1.6 * spread
            + (streaks(w, h, rng, 7.0, 1.0) - 0.5) * 0.8 * spread + mottle(w, h, rng, spread * 0.45, (3, 2), 0.012)
            + (0.5 - u) * 0.14)


def stud(vals: np.ndarray, x: int, y: int, base: float) -> None:
    """Stamp a 2x2 rivet head at (x, y), lit from the top left, with a soft drop shadow."""
    h, w = vals.shape
    for dx, dy, t in ((0, 0, 0.4), (1, 0, 0.16), (0, 1, 0.12), (1, 1, -0.04), (2, 1, -0.13), (1, 2, -0.13),
                      (2, 2, -0.15)):
        if 0 <= x + dx < w and 0 <= y + dy < h:
            vals[y + dy, x + dx] = base + t


def groove(vals: np.ndarray, x0: int, y0: int, x1: int, y1: int, depth: float = 0.2) -> None:
    """Cut a 1px horizontal or vertical channel from (x0, y0) to (x1, y1) with a lit lip below or right of it."""
    h, w = vals.shape
    if y0 == y1 and 0 <= y0 < h:
        vals[y0, max(0, x0):x1 + 1] -= depth
        if y0 + 1 < h:
            vals[y0 + 1, max(0, x0):x1 + 1] += depth * 0.45
    elif 0 <= x0 < w:
        vals[max(0, y0):y1 + 1, x0] -= depth
        if x0 + 1 < w:
            vals[max(0, y0):y1 + 1, x0 + 1] += depth * 0.4


def plate_finish(r, w, h, rng, arg):
    """Brushed plate with an in-hue rim; arg 'rivet' adds corner rivets."""
    vals = metal(w, h, rng)
    rim(vals)
    if arg == "rivet" and w >= 6 and h >= 6:
        for x, y in ((1, 1), (w - 3, 1), (1, h - 3), (w - 3, h - 3)):
            stud(vals, x, y, 0.5)
    return paint(tones(r), vals)


def rivet_finish(r, w, h, rng, arg):
    """Plate with rivet rows along every edge, spaced 6px apart (4px on small faces)."""
    vals = metal(w, h, rng)
    rim(vals)
    if w >= 5 and h >= 5:
        step = 6 if min(w, h) >= 10 else 4
        xs = range(1, w - 2, step)
        for x in xs:
            stud(vals, x, 1, 0.5)
            stud(vals, x, h - 3, 0.5)
        for y in range(1 + step, h - 4, step):
            stud(vals, 1, y, 0.5)
            stud(vals, w - 3, y, 0.5)
    return paint(tones(r), vals)


def panel_finish(r, w, h, rng, arg):
    """Plate split into sub-panels every arg px (default 8) by in-hue channels; panels 10px and up get corner rivets."""
    step = int(arg) if arg.isdigit() else 8
    vals = metal(w, h, rng, spread=0.08)
    xs, ys = list(range(step, w - 1, step)), list(range(step, h - 1, step))
    bx, by = [-1, *xs, w], [-1, *ys, h]
    bevel = 0.08 if step >= 5 else 0.0
    for i in range(len(bx) - 1):
        for j in range(len(by) - 1):
            x0, x1, y0, y1 = bx[i] + 1, bx[i + 1] - 1, by[j] + 1, by[j + 1] - 1
            if x0 > x1 or y0 > y1:
                continue
            vals[y0:y1 + 1, x0:x1 + 1] += rng.uniform(-0.035, 0.035)
            if bevel and x1 - x0 >= 2 and y1 - y0 >= 2:
                vals[y0, x0:x1 + 1] += bevel
                vals[y0 + 1:y1 + 1, x0] += bevel * 0.6
                vals[y1, x0:x1 + 1] -= bevel
                vals[y0:y1, x1] -= bevel * 0.75
            if step >= 10 and x1 - x0 >= 9 and y1 - y0 >= 9:
                for x, y in ((x0 + 1, y0 + 1), (x1 - 2, y0 + 1), (x0 + 1, y1 - 2), (x1 - 2, y1 - 2)):
                    stud(vals, x, y, 0.5)
    cut = np.zeros((h, w), dtype=bool)
    cut[:, xs] = True
    cut[ys, :] = True
    vals[cut] -= 0.22 if step >= 5 else 0.13
    rim(vals, 0.06, 0.08)
    return paint(tones(r), vals)


def flat_finish(r, w, h, rng, arg):
    """Calm matte surface at shade arg (0..4 on the palette, default 2) with soft clusters."""
    base = min(0.84, max(0.16, (float(arg) if arg else 2.0) / 4.0))
    return paint(tones(r, MATTE_SHADES), base + matte(w, h, rng, 0.15))


MATTE_SHADES = 24


def matte(w: int, h: int, rng: np.random.Generator, amp: float) -> np.ndarray:
    """Return zero-centred matte surface noise like smooth stone or polished blackstone: short horizontal clusters,
    broad soft blotches and fine 2px grain."""
    return ((streaks(w, h, rng, 3.0, 1.6) - 0.5) * 2 * amp + (fbm(w, h, rng, (6, 3), (0.6, 0.4)) - 0.5) * amp
            + mottle(w, h, rng, amp * 0.6, (2,), 0.015))


def dark_finish(r, w, h, rng, arg):
    """Shadowed recess or interior in the palette's darkest shades."""
    vals = 0.24 + matte(w, h, rng, 0.15)
    if h > 2:
        vals = vals + np.linspace(-0.04, 0.03, h)[:, None]
    return paint(tones(r, MATTE_SHADES), vals)


def shell_finish(r, w, h, rng, arg):
    """Smooth armour shell at shade arg (default 2.4) with a lit top lip and corner rivets on 8x6 faces and up."""
    base = (float(arg) if arg else 2.4) / 4.0
    vals = metal(w, h, rng, base, 0.08)
    rim(vals, 0.06, 0.12)
    if w >= 3 and h >= 3:
        vals[0, 1:-1] = base + 0.2
    if w >= 8 and h >= 6:
        for x, y in ((1, 2), (w - 3, 2), (1, h - 3), (w - 3, h - 3)):
            stud(vals, x, y, base)
    return paint(tones(r), vals)


def seam_lines(w: int, h: int) -> tuple[np.ndarray, list[tuple[int, int]]]:
    """Return the 1px seam mask of a patch and its glowing core points, laid out by patch size.

    Faces 10x10 and up get staggered panel joints (one full horizontal joint, an upper and a lower vertical
    joint offset like brickwork) glowing at the two junctions; faces 6px long and 3px wide get one line along
    their long axis glowing at its middle; smaller faces get only a single core pixel when at least 2x2.
    """
    seam = np.zeros((h, w), dtype=bool)
    if w >= 10 and h >= 10:
        y, xa, xb = h // 2, w // 3, (2 * w) // 3
        seam[y, 1:w - 1] = True
        seam[1:y, xa] = True
        seam[y + 1:h - 1, xb] = True
        return seam, [(xa, y), (xb, y)]
    if max(w, h) >= 6 and min(w, h) >= 3:
        if w >= h:
            seam[h // 2, 1:w - 1] = True
        else:
            seam[1:h - 1, w // 2] = True
        return seam, [(w // 2, h // 2)]
    if w >= 2 and h >= 2:
        seam[h // 2, w // 2] = True
        return seam, [(w // 2, h // 2)]
    return seam, []


def seam_finish(r, w, h, rng, arg):
    """Dark plate with thin seams in ramp(arg) colour that are bright only at their cores and fade to dim along the joint.

    Only core pixels take exactly ramp(arg or '#7FFFF0') shade 3: geo.glow_face finds the glowmask by that colour,
    so only the cores emit light.
    """
    g = ramp(arg or "#7FFFF0")
    vals = metal(w, h, rng, 0.32, 0.1)
    rim(vals, 0.1, 0.1)
    seam, cores = seam_lines(w, h)
    p = np.pad(seam, 1)
    near = (p[:-2, 1:-1] | p[2:, 1:-1] | p[1:-1, :-2] | p[1:-1, 2:]) & ~seam
    vals[near] -= 0.06
    img = paint(tones(r), vals)
    if not cores:
        return img
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.min([np.hypot(xx - cx, yy - cy) for cx, cy in cores], axis=0)
    reach = max(2.5, min(w, h) * 0.4)
    k = np.clip(1.0 - d / reach, 0.2, 0.85)[..., None]
    dim = paint(tones(g, mute=0.35), np.clip(0.9 - d / (reach * 2.0), 0.35, 0.85)).astype(np.float64)
    mixed = np.rint(img[..., :3] * (1 - k) + dim[..., :3] * k).astype(np.uint8)
    img[seam, :3] = mixed[seam]
    halo = near & (d < 2.0)
    img[halo, :3] = np.rint(img[halo, :3] * 0.75 + dim[halo, :3] * 0.25).astype(np.uint8)
    core = seam & (d < 1.5)
    img[core] = (int(g[3][0]), int(g[3][1]), int(g[3][2]), 255)
    return img


def vent_finish(r, w, h, rng, arg):
    """Plate with louvred slots: dark slot rows with a lit lip beneath, inset 2px from the sides."""
    vals = metal(w, h, rng)
    rim(vals)
    step = 3 if h >= 8 else 2
    for y in range(2, h - 2, step) if w >= 5 else ():
        vals[y, 2:w - 2] = 0.1 + (vals[y, 2:w - 2] - 0.5) * 0.4
        if step == 3:
            vals[y + 1, 2:w - 2] += 0.12
        vals[y, 2] += 0.06
    return paint(tones(r), vals)


def hatch_finish(r, w, h, rng, arg):
    """Plate with an inset hatch door (dark gap, raised rim) and a small handle on faces 4x4 and up."""
    vals = metal(w, h, rng)
    rim(vals)
    if w >= 4 and h >= 4:
        m = 2 if min(w, h) >= 8 else 1
        x0, y0, x1, y1 = m, m, w - 1 - m, h - 1 - m
        vals[y0, x0:x1 + 1] -= 0.2
        vals[y0:y1 + 1, x0] -= 0.2
        vals[y1, x0:x1 + 1] -= 0.12
        vals[y0:y1 + 1, x1] -= 0.12
        if x1 - x0 >= 3 and y1 - y0 >= 3:
            vals[y0 + 1, x0 + 1:x1] += 0.1
            vals[y1 - 1, x0 + 1:x1] -= 0.06
            hy = (y0 + y1) // 2
            vals[hy, x1 - 2:x1] = 0.85
            vals[hy + 1, x1 - 2:x1] = 0.3
    return paint(tones(r), vals)


def grate_finish(r, w, h, rng, arg):
    """See-through grate: 1px bars on a 3px pitch around 2x2 holes, crossings lit, bottom and right bars shaded."""
    yy, xx = np.mgrid[0:h, 0:w]
    hb, vb = (yy % 3 == 0) | (yy == h - 1), (xx % 3 == 0) | (xx == w - 1)
    bars = hb | vb
    vals = 0.5 + mottle(w, h, rng, 0.07) + np.where(hb & vb, 0.14, np.where(vb, -0.07, 0.0))
    if h > 1:
        vals = vals + np.linspace(0.06, -0.08, h)[:, None]
    if w >= 3 and h >= 3:
        vals[-1, :] -= 0.1
        vals[:, -1] -= 0.08
    return paint(tones(r), vals, bars)


def grille_finish(r, w, h, rng, arg):
    """Horizontal bars every 2px over a dark gap, each bar shading from a lit left end."""
    vals = 0.12 + mottle(w, h, rng, 0.04)
    bar = 0.58 + mottle(w, h, rng, 0.07) + np.linspace(0.08, -0.1, w)[None, :]
    rows = (np.arange(h) % 2 == 0)
    vals[rows] = bar[rows]
    if w > 2:
        vals[rows, 0] = 0.75
    return paint(tones(r), vals)


def gauge_finish(r, w, h, rng, arg):
    """Plate with a round dial: brass bezel lit top left, ceramic face, tick marks, red needle."""
    vals = metal(w, h, rng)
    rim(vals)
    img = paint(tones(r), vals)
    rad = min(w, h) / 2.0 - 1.0
    if rad < 1.5:
        return img
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    d, lit = np.hypot(xx - cx, yy - cy), (xx - cx) + (yy - cy)
    ring, face = d <= rad + 0.6, d <= rad - 0.4
    brass, cer = tones(ramp("brass")), tones(ramp("ceramic"))
    img[ring] = paint(brass, np.clip(0.55 - 0.14 * lit / rad, 0.3, 0.85) + mottle(w, h, rng, 0.03))[ring]
    shade = 0.68 - 0.08 * lit / rad - 0.12 * (d > rad - 1.4) * (lit < 0) + mottle(w, h, rng, 0.04)
    img[face] = paint(cer, shade)[face]
    if rad >= 2.5:
        for px, py in ((cx, cy - rad + 1), (cx + rad - 1, cy), (cx - rad + 1, cy), (cx, cy + rad - 1)):
            img[round(py), round(px)] = tcol(cer, 0.3)
    red = tones(ramp("red_paint"))
    c0 = (round(cx), round(cy))
    for x, y in line_pts(*c0, round(cx + rad * 0.7), round(cy - rad * 0.7)):
        img[y, x] = tcol(red, 0.62)
    img[c0[1], c0[0]] = tcol(cer, 0.2)
    return img


def screen_finish(r, w, h, rng, arg):
    """Recessed screen: shadowed bezel, dark tinted glass, rows of lit text segments in colour arg."""
    lit = tones(ramp(arg or "#4FE3FF"), mute=0.0)
    img = paint(tones(ramp("rubber")), 0.45 + mottle(w, h, rng, 0.06) + np.linspace(0.12, -0.04, h)[:, None])
    img[..., :3] = np.rint(img[..., :3] * 0.78 + lit[3] * 0.22).astype(np.uint8)
    for y in range(1, h - 1, 2):
        x = 1
        while x < w - 1:
            n = int(rng.integers(1, max(2, min(5, w - 1 - x) + 1)))
            t = float(rng.uniform(0.62, 0.86))
            for k in range(x, min(w - 1, x + n)):
                img[y, k] = tcol(lit, t)
            x += n + 1
    if w >= 3 and h >= 3:
        bez = tones(r)
        for x in range(w):
            img[0, x], img[h - 1, x] = tcol(bez, 0.22), tcol(bez, 0.58)
        for y in range(h):
            img[y, 0], img[y, w - 1] = tcol(bez, 0.25), tcol(bez, 0.55)
    return img


def coil_finish(r, w, h, rng, arg):
    """Wire windings on a 3px pitch, each lit on top; horizontal turns ('v' or a short face: vertical turns)."""
    vertical = arg == "v" or (arg == "" and h < w)
    W, H = (h, w) if vertical else (w, h)
    vals = mottle(W, H, rng, 0.04)
    for y in range(H):
        vals[y] += (0.76, 0.56, 0.32)[y % 3] + (rng.uniform(-0.04, 0.04) if y % 3 == 0 else 0.0)
    for y in range(0, H, 3):
        for x in range(int(rng.integers(0, 9)), W, 11):
            vals[y, x] = 0.9
    img = paint(tones(r), vals)
    return np.ascontiguousarray(np.transpose(img, (1, 0, 2))) if vertical else img


def chain_finish(r, w, h, rng, arg):
    """Chain links along the long axis: lit links with dark joints."""
    vertical = h > w
    W, H = (h, w) if vertical else (w, h)
    vals = np.zeros((H, W))
    for x in range(W):
        vals[:, x] = (0.72, 0.5, 0.22)[x % 3]
    if H >= 3:
        vals[0] += 0.12
        vals[-1] -= 0.14
    img = paint(tones(r), vals)
    return np.ascontiguousarray(np.transpose(img, (1, 0, 2))) if vertical else img


def _polar(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(float)
    dx, dy = xx + 0.5 - w / 2.0, yy + 0.5 - h / 2.0
    return np.hypot(dx, dy), min(w, h) / 2.0, dx, dy


def _wheel_rim(vals, d, rad, dx, dy, width):
    band = (d <= rad) & (d > rad - width)
    lit = -(dx + dy) / max(rad, 1.0)
    vals[band] = 0.5 + 0.22 * lit[band]
    vals[(d <= rad) & (d > rad - 0.75) & (lit > 0.9)] = 0.92


def roundel_finish(r, w, h, rng, arg):
    """Round plate (flywheel, millstone face): soft surface, a grooved ring, lit rim, raised hub; corners clear."""
    d, rad, dx, dy = _polar(w, h)
    vals = 0.5 + mottle(w, h, rng, 0.08) - 0.06 * (dx + dy) / max(rad, 1.0)
    ring = rad * 0.62
    vals[np.abs(d - ring) < 0.5] -= 0.18
    vals[np.abs(d - ring - 0.9) < 0.45] += 0.08
    _wheel_rim(vals, d, rad, dx, dy, max(1.0, rad / 9.0))
    hub = d < max(1.0, rad * 0.16)
    vals[hub] = 0.62 - 0.1 * (dx + dy)[hub] / max(1.0, rad * 0.16)
    return paint(tones(r), vals, d <= rad)


def spoked_finish(r, w, h, rng, arg):
    """Spoked wheel (arg spokes, default 6): lit rim, spokes, hub; gaps clear."""
    n = int(arg) if arg.isdigit() else 6
    d, rad, dx, dy = _polar(w, h)
    vals = np.full((h, w), 0.5)
    mask = np.zeros((h, w), dtype=bool)
    sw = max(0.75, rad / 10.0)
    for k in range(n):
        a = 2 * np.pi * k / n + np.pi / 2
        along = dx * np.cos(a) + dy * np.sin(a)
        off = np.abs(-dx * np.sin(a) + dy * np.cos(a))
        sp = (off < sw) & (along > 0) & (d <= rad)
        mask |= sp
        vals[sp & (off < sw * 0.4) & ((dx + dy) < 0)] = 0.66
    band = (d <= rad) & (d > rad - max(1.0, rad / 6.0))
    mask |= band
    _wheel_rim(vals, d, rad, dx, dy, max(1.0, rad / 6.0))
    hub = d < max(1.5, rad * 0.22)
    mask |= hub
    vals[hub] = 0.55 - 0.15 * (dx + dy)[hub] / max(1.5, rad * 0.22)
    vals[hub & (d < max(0.8, rad * 0.1))] = 0.85
    return paint(tones(r), vals, mask)


def cog_finish(r, w, h, rng, arg):
    """Gear: body with a lit rim, teeth on a 3px pitch, hub ring and bright axle; gaps clear."""
    d, rad, dx, dy = _polar(w, h)
    vals = 0.5 + mottle(w, h, rng, 0.06)
    body = rad * 0.74
    teeth = max(6, round(2 * np.pi * rad / 3.2))
    ang = (np.arctan2(dy, dx) / (2 * np.pi) * teeth) % 1.0
    keep = (d <= body) | ((d <= rad) & (ang < 0.5))
    lit = -(dx + dy) / max(rad, 1.0)
    edge = (d > body - 0.9) & keep
    vals[edge] = 0.5 + 0.22 * lit[edge]
    vals[np.abs(d - rad * 0.45) < 0.5] -= 0.15
    vals[d < max(1.0, rad * 0.18)] = 0.85
    return paint(tones(r), vals, keep)


def hex_finish(r, w, h, rng, arg):
    """Hex lattice: offset rows of raised cells (lit top, shaded bottom) with corner-cut borders in dark channels."""
    yy, xx = np.mgrid[0:h, 0:w]
    row = yy // 4
    sx = (xx + (row % 2) * 3) % 6
    sy = yy % 4
    border = (sx == 0) | (sy == 0) | (((sx == 1) | (sx == 5)) & ((sy == 1) | (sy == 3)))
    cell = row * 1000 + (xx + (row % 2) * 3) // 6
    jitter = rng.uniform(-0.06, 0.06, int(cell.max()) + 1)
    vals = 0.55 + jitter[cell] + mottle(w, h, rng, 0.04) + np.where(sy == 1, 0.1, np.where(sy == 3, -0.08, 0.0))
    vals[border] = 0.18 + (rng.random(int(border.sum())) - 0.5) * 0.04
    return paint(tones(r), vals)


METAL = {
    "plate": plate_finish, "rivet": rivet_finish, "panel": panel_finish, "flat": flat_finish,
    "dark": dark_finish, "shell": shell_finish, "seam": seam_finish, "vent": vent_finish, "hatch": hatch_finish,
    "grate": grate_finish, "grille": grille_finish, "gauge": gauge_finish, "screen": screen_finish,
    "coil": coil_finish, "chain": chain_finish, "roundel": roundel_finish, "spoked": spoked_finish,
    "cog": cog_finish, "hex": hex_finish,
}

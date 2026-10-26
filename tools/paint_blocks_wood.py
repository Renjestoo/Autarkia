"""Timber, stone-craft and clay block textures: planks, crates, boards, posts, pulleys, crucibles and querns.

Every painter has the signature (name, palette, rng) -> 16x16 RGBA array. Painters build continuous tone fields in
[0, 1] and map them onto 6 to 15 muted, hue-shifted shades with shade_n, as vanilla wood, brick and stone do: no
ordered dithering, no hard bevel frames.
"""

import numpy as np
from paint import fbm, line_pts, ramp, ramp_n, relief, shade_n
from paint_blocks_metal import brushed, metal, rim, soft, stud

TAU = 2 * np.pi
YY, XX = np.mgrid[0:16, 0:16]


def tones(pal: str, n: int, lo: float, hi: float, sat: float = 1.0, shift: float = 0.2) -> np.ndarray:
    """Return an (n, 3) ramp of palette pal sampled from position lo to hi (0..4, dark to light), saturation scaled by sat."""
    fine = ramp_n(pal, 65, shift).astype(np.float64)
    rgb = fine[np.rint(np.linspace(lo, hi, n) * 16).astype(int)]
    grey = (rgb @ np.array([0.299, 0.587, 0.114]))[:, None]
    return np.clip(np.rint(grey + (rgb - grey) * sat), 0, 255).astype(np.int64)


def key_ramp(keys, n: int) -> np.ndarray:
    """Return an (n, 3) ramp interpolated evenly through RGB key colours, dark to light."""
    k = np.array([c[:3] for c in keys], dtype=np.float64)
    t = np.linspace(0, len(k) - 1, n)
    rgb = np.stack([np.interp(t, np.arange(len(k)), k[:, c]) for c in range(3)], axis=1)
    return np.clip(np.rint(rgb), 0, 255).astype(np.int64)


def layer(img: np.ndarray, vals: np.ndarray, r: np.ndarray, mask: np.ndarray) -> None:
    """Paint tone values (0..1) through ramp r into img wherever mask is set."""
    img[mask] = shade_n(np.clip(vals, 0, 1), r)[mask]


def streaks(w: int, h: int, rng: np.random.Generator, length: float) -> np.ndarray:
    """Return grain noise in [0, 1], smooth along x with period w (tileable) and independent per row."""
    k = max(1, round(w / length))
    grid = rng.random((h, k))
    x = np.arange(w) * k / w
    i = np.floor(x).astype(int)
    t = x - i
    t = t * t * (3 - 2 * t)
    return grid[:, i % k] * (1 - t) + grid[:, (i + 1) % k] * t


def grain(rng: np.random.Generator, w: int = 16, h: int = 16, vertical: bool = False) -> np.ndarray:
    """Return two-octave wood grain in [0, 1] running along x, or along y when vertical."""
    if vertical:
        return grain(rng, h, w).T
    return 0.55 * streaks(w, h, rng, 6) + 0.33 * streaks(w, h, rng, 2.7) + 0.12 * rng.random((h, w))


def plank_tones(rng: np.random.Generator, w: int = 16, h: int = 16, height: int = 4, base: float = 0.66,
                amp: float = 0.56, vertical: bool = False) -> np.ndarray:
    """Return tones of planks `height` px wide: per-plank tone, top-lit grain, a dark in-hue seam and one butt joint each.

    The planks run along x (along y when vertical); the field tiles when height divides the size across the planks.
    """
    if vertical:
        return plank_tones(rng, h, w, height, base, amp).T
    g = grain(rng, w, h)
    t = np.zeros((h, w))
    j0 = int(rng.integers(0, w))
    for p in range(h // height):
        y0 = p * height
        tone = base + (rng.random() - 0.5) * 0.14
        for r in range(height - 1):
            t[y0 + r] = tone + 0.12 * (1 - r / max(1, height - 2)) - 0.06 + (g[y0 + r] - 0.5) * amp
        t[y0 + height - 1] = 0.04 + g[y0 + height - 1] * 0.16
        jx = (j0 + p * (w // 2) + int(rng.integers(-2, 3))) % w
        t[y0:y0 + height - 1, jx] -= 0.22
    return t


def wood_ramp(pal: str, n: int = 7) -> np.ndarray:
    """Return the muted plank ramp of a wood palette, contrast close to vanilla planks."""
    return tones(pal, n, 0.85, 3.7, 0.85, 0.15)


def bronze_ramp(n: int = 6) -> np.ndarray:
    """Return the high-contrast bronze fitting ramp used for brackets, nails, straps and hubs."""
    return tones("bronze", n, 0.0, 3.9, 1.0)


def frame_tones(rng: np.random.Generator, w: int, base: float = 0.68) -> tuple[np.ndarray, np.ndarray]:
    """Return (tones, mask) of a top-lit board frame w px wide: grained stiles at the sides, rails between, dark end seams."""
    hg, vg = grain(rng), grain(rng, vertical=True)
    stile = (XX < w) | (XX >= 16 - w)
    rail = ~stile & ((YY < w) | (YY >= 16 - w))
    t = np.where(stile, base + (vg - 0.5) * 0.52, base + 0.03 + (hg - 0.5) * 0.52)
    t[0, :] += 0.07
    t[:, 0] += 0.07
    t[:, 16 - w] += 0.1
    t[:, w - 1] -= 0.07
    t[:, 15] -= 0.14
    t[16 - w, w:16 - w] += 0.1
    t[w - 1, w:16 - w] -= 0.07
    t[15, w:16 - w] -= 0.14
    for x in (w, 15 - w):
        seam = rail & (XX == x)
        t[seam] = (0.08 + hg * 0.12)[seam]
    return t, stile | rail


def nail(img: np.ndarray, r: np.ndarray, x: int, y: int, mask: np.ndarray) -> None:
    """Paint a nail head lit from the top left at (x, y), its shadow below it (or right of it) inside mask."""
    img[y, x, :3] = r[-1]
    for sx, sy in ((x, y + 1), (x + 1, y)):
        if 0 <= sx < 16 and 0 <= sy < 16 and mask[sy, sx]:
            img[sy, sx, :3] = r[0]
            return


def brackets(img: np.ndarray, rng: np.random.Generator, arm: int = 2, length: int = 5) -> np.ndarray:
    """Paint four bronze L brackets with nails over the corners; return their mask."""
    r = bronze_ramp()
    base = np.zeros((16, 16), dtype=bool)
    base[:arm, :length] = True
    base[:length, :arm] = True
    m = base | base[:, ::-1] | base[::-1] | base[::-1, ::-1]
    layer(img, relief(m, 0.5, 0.2, 2) + (fbm(16, 16, rng, (4, 2)) - 0.5) * 0.2, r, m)
    for x, y in ((length - 2, 0), (0, length - 2)):
        for fx, fy in ((x, y), (15 - x, y), (x, 15 - y), (15 - x, 15 - y)):
            nail(img, r, fx, fy, m)
    return m


def plank(name, pal, rng):
    """Plain tileable planks in the vanilla layout: four 4 px planks, dark seams, staggered butt joints."""
    return shade_n(plank_tones(rng), wood_ramp(pal))


def beam(name, pal, rng):
    """Rough vertical timber with checks and two riveted iron bands that shade the wood below them."""
    r, iron = tones(pal, 7, 0.7, 3.6, 0.85, 0.15), tones("iron", 6, 0.0, 2.8, 1.0, 0.3)
    g = grain(rng, vertical=True)
    t = 0.56 + (g - 0.5) * 0.5 + (fbm(16, 16, rng, (8, 4)) - 0.5) * 0.2
    for _ in range(3):
        x, y, ln = int(rng.integers(1, 15)), int(rng.integers(0, 16)), int(rng.integers(3, 7))
        rows = (np.arange(ln) + y) % 16
        t[rows, x] = 0.1 + rng.random(ln) * 0.1
        t[rows, x - 1] += 0.1
    bands = [(2, 5), (11, 14)]
    for y0, y1 in bands:
        t[y1] -= 0.25
    img = shade_n(np.clip(t, 0, 1), r)
    n = fbm(16, 16, rng, (4, 2))
    for y0, y1 in bands:
        m = (YY >= y0) & (YY < y1)
        vals = 0.7 - (YY - y0) * 0.26 + (n - 0.5) * 0.3
        layer(img, vals, iron, m)
        for x in (2, 8, 13):
            img[y0 + 1, x, :3] = iron[-1]
            img[y0 + 2, x, :3] = iron[0]
    return img


def crate_side(name, pal, rng):
    """Crate side: grained frame boards around two recessed horizontal planks, bronze L brackets with nails."""
    r = wood_ramp(pal)
    ft, fm = frame_tones(rng, 3)
    t = np.zeros((16, 16))
    t[3:13] = plank_tones(rng, 16, 10, 5, base=0.56)
    t[3, :] -= 0.16
    t[:, 3] -= 0.16
    t[fm] = ft[fm]
    img = shade_n(np.clip(t, 0, 1), r)
    brackets(img, rng)
    return img


def crate_top(name, pal, rng):
    """Crate lid: the side's frame and brackets around lid planks crossed by two nailed bronze straps."""
    r, b = wood_ramp(pal), bronze_ramp()
    ft, fm = frame_tones(rng, 3, 0.7)
    t = np.zeros((16, 16))
    t[3:13] = plank_tones(rng, 16, 10, 5, base=0.6)
    t[3, :] -= 0.16
    t[:, 3] -= 0.16
    for x0 in (5, 10):
        t[3:13, x0 + 2] -= 0.18
    t[fm] = ft[fm]
    img = shade_n(np.clip(t, 0, 1), r)
    n = fbm(16, 16, rng, (4, 2))
    for x0 in (5, 10):
        m = (XX >= x0) & (XX <= x0 + 1) & (YY >= 3) & (YY <= 12)
        layer(img, 0.62 - (XX - x0) * 0.2 + (n - 0.5) * 0.25, b, m)
        for y in (4, 11):
            nail(img, b, x0, y, m)
    brackets(img, rng)
    return img


def board_front(name, pal, rng):
    """Slate chalk board in a grained wooden frame: chalk dust, two tally groups, a chalk stick on the ledge."""
    r = wood_ramp(pal)
    slate, chalk = tones("deepslate", 7, 0.4, 4.0, 0.9, 0.25), tones("canvas", 5, 2.0, 3.7, 0.25, 0.1)
    ft, fm = frame_tones(rng, 2)
    dust = fbm(16, 16, rng, (8, 4), (0.6, 0.4))
    t = 0.42 + (fbm(16, 16, rng, (4, 2)) - 0.5) * 0.3 + (rng.random((16, 16)) - 0.5) * 0.2 + np.clip(dust - 0.55, 0, 1) * 0.6
    t[2, :] -= 0.14
    t[:, 2] -= 0.14
    img = shade_n(np.clip(t, 0, 1), slate)
    layer(img, ft, r, fm)
    marks = [((x, 3), (x, 7)) for x in (3, 5, 7, 9)] + [((2, 7), (10, 3))] + [((x, 9), (x, 12)) for x in (3, 5, 7)]
    for (x0, y0), (x1, y1) in marks:
        for x, y in line_pts(x0, y0, x1, y1):
            img[y, x, :3] = chalk[int(rng.integers(1, 5))]
    for x, y in ((10, 10), (11, 10), (12, 11), (10, 12), (11, 12)):
        img[y, x, :3] = slate[min(6, round(t[y, x] * 6) + 2)]
    for x, k in ((4, 4), (5, 3), (6, 2)):
        img[14, x, :3] = chalk[k]
    img[14, 7, :3] = r[1]
    return img


def chute_tones(rng: np.random.Generator) -> tuple[np.ndarray, np.ndarray]:
    """Return (tones, rail mask) of a polished slide: vertical boards with a soft long sheen between dark side rails."""
    t = plank_tones(rng, height=4, base=0.66, amp=0.4, vertical=True)
    t += 0.12 * np.exp(-((XX - 6.5) / 2.4) ** 2)
    vg = grain(rng, vertical=True)
    rail = (XX < 2) | (XX > 13)
    t = np.where(rail, 0.36 + (vg - 0.5) * 0.3, t)
    t[:, 0] += 0.06
    t[:, 1] -= 0.04
    t[:, 14] += 0.14
    t[:, 15] -= 0.1
    t[:, 2] -= 0.14
    return t, rail


def chute_ramp(pal: str) -> np.ndarray:
    """Return the slightly lighter, polished wood ramp of slides."""
    return tones(pal, 7, 0.7, 3.9, 0.85, 0.15)


def chute(name, pal, rng):
    """Polished slide of vertical boards between dark side rails, a soft long sheen and bronze rail nails."""
    t, rail = chute_tones(rng)
    img, b = shade_n(np.clip(t, 0, 1), chute_ramp(pal)), bronze_ramp()
    for x in (1, 14):
        for y in (3, 11):
            nail(img, b, x, y, rail)
    return img


def cork_board(name, pal, rng):
    """Granular cork in a grained wooden frame with three pinned paper cards casting soft shadows."""
    r, cork = wood_ramp(pal), tones(pal, 7, 1.9, 4.0, 0.75, 0.15)
    paper = tones("canvas", 6, 0.6, 3.6, 0.5, 0.15)
    ft, fm = frame_tones(rng, 2)
    t = 0.58 + (fbm(16, 16, rng, (4, 2)) - 0.5) * 0.25 + (rng.random((16, 16)) - 0.5) * 0.45
    t[rng.random((16, 16)) < 0.12] -= 0.3
    t[2, :] -= 0.18
    t[:, 2] -= 0.18
    cards = ((3, 3, 5, 5, "red_paint"), (9, 4, 4, 5, "electric_accent"), (4, 9, 7, 4, "verdigris"))
    for x0, y0, w, h, _pin in cards:
        t[y0 + 1:y0 + h + 1, x0 + 1:x0 + w + 1] -= 0.3
    img = shade_n(np.clip(t, 0, 1), cork)
    layer(img, ft, r, fm)
    for x0, y0, w, h, pin in cards:
        m = (XX >= x0) & (XX < x0 + w) & (YY >= y0) & (YY < y0 + h)
        vals = 0.86 - (YY - y0) * 0.03 + (rng.random((16, 16)) - 0.5) * 0.1
        vals[y0 + h - 1] -= 0.18
        vals[:, x0 + w - 1] -= 0.1
        for ly in range(y0 + 2, y0 + h - 1, 2):
            ln = int(rng.integers(max(1, w - 4), w - 1))
            vals[ly, x0 + 1:x0 + 1 + ln] = 0.38
        layer(img, vals, paper, m)
        pr = tones(pin, 4, 1.0, 4.0, 0.75)
        px = x0 + w // 2
        img[y0, px, :3] = pr[3]
        img[y0 + 1, px, :3] = pr[1]
    return img


def winding_post(name, pal, rng):
    """Particle texture of the GeckoLib winding post: bark log side (vertical furrows, like oak_log) crossed by the bronze
    ratchet wheel seen edge-on (teeth on top, period 4 so it tiles) with its pawl resting in a notch.
    """
    r, b = tones(pal, 7, 0.35, 3.1, 0.8, 0.2), metal("bronze", 10)
    t = 0.56 + (grain(rng, vertical=True) - 0.5) * 0.45
    furrow = streaks(16, 16, rng, 4).T < 0.27
    t[furrow] -= 0.32
    t[np.roll(furrow, -1, 1) & ~furrow] += 0.08
    u = XX % 4
    teeth = ((YY == 5) & (u != 3)) | ((YY == 4) & (u == 0))
    band = (YY >= 6) & (YY <= 8)
    pawl = np.zeros((16, 16), dtype=bool)
    for x, y in ((13, 1), (13, 2), (14, 2), (14, 3), (15, 3), (15, 4)):
        pawl[y, x] = True
    t[5][u[5] == 3] -= 0.22
    t[4][u[4] == 1] -= 0.18
    t[9] -= 0.28
    t[10] -= 0.1
    t[np.roll(np.roll(pawl, 1, 0), 1, 1) & ~pawl & ~teeth & ~band] -= 0.24
    img = shade_n(np.clip(t, 0, 1), r)
    bt = 0.42 + brushed(rng, 0.05) + soft(rng, 0.04, (4, 2), (0.6, 0.4))
    bt[4] = 0.78
    bt[5] = np.select([u[5] == 0, u[5] == 1], [0.6, 0.46], 0.3)
    bt[6] += 0.3
    bt[8] -= 0.24
    bt[pawl] = np.where(np.roll(pawl, 1, 1) & pawl, 0.28, 0.66)[pawl]
    layer(img, bt, b, teeth | band | pawl)
    return img


def pulley_wheel(name, pal, rng):
    """Wooden sheave seen face-on (round, clear corners): end-grain rings, two checks, a rope groove and a bronze hub."""
    r, b = tones(pal, 7, 0.6, 3.9, 0.85, 0.15), bronze_ramp(7)
    d = np.hypot(XX - 7.5, YY - 7.5)
    light = (XX - 7.5) + (YY - 7.5)
    n = fbm(16, 16, rng, (4, 2))
    t = 0.56 + 0.1 * np.sin(d * 2.4 + n * 3) - 0.022 * light + (rng.random((16, 16)) - 0.5) * 0.04
    for _ in range(2):
        th = rng.random() * TAU
        for k in np.arange(3.0, 5.6, 0.5):
            t[round(7.5 + np.sin(th) * k), round(7.5 + np.cos(th) * k)] -= 0.24
    groove = (d >= 6.1) & (d < 7.0)
    t[groove] = 0.2 + 0.018 * light[groove]
    edge = d >= 7.0
    t[edge] = 0.56 - 0.03 * light[edge] - 0.16 * (d[edge] >= 7.5)
    disc = d < 7.9
    img = shade_n(np.clip(t, 0, 1), r, disc)
    hub = d < 2.6
    vals = relief(hub, 0.55, 0.14, 2) - 0.03 * light
    vals[d < 1.0] = 0.06
    layer(img, vals, b, hub)
    img[6, 6, :3] = b[-1]
    return img


def pulley_side(name, pal, rng):
    """Bronze pulley housing plate in the bronze_block style: cast plate with soft polish bands, in-hue edge, four domed
    rivets, and a raised bearing ring round a dark bore in the centre (pixels 5-10, the up faces of the model's base plates).
    """
    r = metal(pal)
    s = XX + YY
    d = np.hypot(XX - 7.5, YY - 7.5)
    t = 0.5 + soft(rng, 0.07, (4, 2), (0.6, 0.4)) + brushed(rng, 0.04) + 0.12 * np.cos(TAU * (s - 4) / 26)
    t += rim(0.1, -0.08, -0.12, -0.3)
    ring = (d >= 2.2) & (d < 3.7)
    t[ring] += 0.08 + 0.022 * (15 - s[ring])
    t[(d >= 3.7) & (d < 4.5) & (s > 15)] -= 0.1
    bore = d < 2.2
    t[bore] = 0.2 - 0.02 * (15 - s[bore])
    t[d < 1.1] = 0.04
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        stud(t, x, y, 0.32)
    return shade_n(np.clip(t, 0, 1), r)


def crucible_side(name, pal, rng):
    """Clay brick pot wall laid out for the model: a light rim lip on row 4, its shadow, offset brick courses, sooty base."""
    r, mortar = tones(pal, 7, 0.8, 3.8, 0.85, 0.2), tones(pal, 4, 1.8, 3.6, 0.35, 0.1)
    n, soot = fbm(16, 16, rng, (4, 2), (0.6, 0.4)), fbm(16, 16, rng, (8, 4))
    course = ((YY - 2) % 16) // 4
    rr = (YY - 2) % 4
    off = (course % 2) * 4
    bid = course * 2 + ((XX + off) % 16) // 8
    base = 0.45 + rng.random(8) * 0.25
    t = base[bid] + 0.14 * (rr == 0) - 0.12 * (rr == 2) + (n - 0.5) * 0.3 + (rng.random((16, 16)) - 0.5) * 0.08
    joints = (rr == 3) | ((XX + off) % 8 == 7)
    t[4] = 0.86 + (n[4] - 0.5) * 0.12
    t[5] = 0.12 + n[5] * 0.1
    fade = np.clip((YY - 10) / 5.0, 0, 1) * (0.12 + 0.2 * soot)
    t -= fade
    img = shade_n(np.clip(t, 0, 1), r)
    joints[4:6] = False
    layer(img, 0.6 + (n - 0.5) * 0.5 - fade * 1.5, mortar, joints)
    return img


def crucible_top(name, pal, rng):
    """Pot from above: fired clay rim ring (pixels 1-3 and 12-14, the model's rims) with brick joints around the sooty cavity floor (4-11)."""
    r, mortar = tones(pal, 7, 0.8, 3.8, 0.85, 0.2), tones(pal, 4, 1.8, 3.6, 0.35, 0.1)
    floor = tones("slag", 6, 0.0, 2.8, 0.8, 0.25)
    n = fbm(16, 16, rng, (4, 2), (0.6, 0.4))
    inner = (XX >= 4) & (XX <= 11) & (YY >= 4) & (YY <= 11)
    t = 0.64 + (n - 0.5) * 0.3 + (rng.random((16, 16)) - 0.5) * 0.16
    t[1, 1:15] += 0.1
    t[1:15, 1] += 0.1
    t[14, 1:15] -= 0.1
    t[1:15, 14] -= 0.1
    t[0, :] -= 0.12
    t[15, :] -= 0.12
    t[:, 0] -= 0.12
    t[:, 15] -= 0.12
    img = shade_n(np.clip(t, 0, 1), r)
    joints = np.zeros((16, 16), dtype=bool)
    joints[1:4, 5] = joints[1:4, 10] = joints[12:15, 7] = True
    joints[8, 1:4] = joints[6, 12:15] = joints[11, 12:15] = True
    layer(img, 0.35 + (n - 0.5) * 0.3, mortar, joints)
    f = 0.42 + (fbm(16, 16, rng, (4, 2)) - 0.5) * 0.4 + (rng.random((16, 16)) - 0.5) * 0.16
    f[4] -= 0.26
    f[5] -= 0.12
    f[:, 4] -= 0.22
    f[:, 5] -= 0.1
    layer(img, f, floor, inner)
    return img


def wave_field(x: np.ndarray, y: np.ndarray, rng: np.random.Generator, phase: float, count: int, kmax: int,
               sway: float) -> np.ndarray:
    """Return a sum of plane waves over unit-periodic coordinates, tileable and looping over phase (0..1), roughly in [-1, 1].

    Each wave sways back and forth by up to sway radians per loop instead of travelling, which keeps the motion slow.
    """
    acc = np.zeros(np.shape(x))
    for _ in range(count):
        kx, ky = 0, 0
        while kx == 0 and ky == 0:
            kx, ky = (int(v) for v in rng.integers(-kmax, kmax + 1, 2))
        p0, p1, a = rng.random() * TAU, rng.random() * TAU, sway * (0.6 + rng.random() * 0.8)
        acc += np.sin(TAU * (kx * x + ky * y) + p0 + a * np.sin(TAU * phase + p1))
    return acc / np.sqrt(count)


def molten(name, pal, rng, phase=0.0):
    """Molten bronze like vanilla lava: smoothly churning glow with bright cores and drifting dark cooling crust; phase (0..1) loops."""
    crust_r, hot = ramp(pal), ramp("magma")
    glow = key_ramp([crust_r[0] * 0.7, crust_r[0], crust_r[1] * 0.9 + hot[1] * 0.1, hot[2], (222, 90, 16), hot[3],
                     (252, 148, 40), (255, 179, 71), (255, 214, 120)], 15)
    x, y = XX / 16.0, YY / 16.0
    wx = wave_field(x, y, rng, phase, 3, 1, 0.9)
    wy = wave_field(x, y, rng, phase, 3, 1, 0.9)
    u, v = x + 0.09 * wx, y + 0.09 * wy
    flow = 0.65 * wave_field(u, v, rng, phase, 6, 2, 0.9) + 0.45 * wave_field(u, v, rng, phase, 6, 4, 0.7)
    crust = wave_field(u, v, rng, phase, 5, 2, 0.5)
    skin = fbm(16, 16, rng, (4, 2))
    t = 0.62 + 0.27 * flow
    edge = np.clip((crust - 0.85) / 0.4, 0, 1)
    edge = edge * edge * (3 - 2 * edge)
    t = t * (1 - edge) + (0.04 + 0.2 * skin) * edge
    return shade_n(np.clip(t, 0, 1), glow)


def stone_ramp(pal: str) -> np.ndarray:
    """Return the muted 7-shade stone ramp, close to vanilla stone and smooth stone."""
    return tones(pal, 7, 0.5, 3.9, 1.0, 0.12)


def quern_side(name, pal, rng):
    """Two stacked millstones with chamfered lit tops, shaded undersides, vertical dressing marks and a dark grind gap."""
    r = stone_ramp(pal)
    t = 0.52 + (fbm(16, 16, rng, (4, 2), (0.6, 0.4)) - 0.5) * 0.4 + (rng.random((16, 16)) - 0.5) * 0.16
    for y0, y1 in ((0, 6), (8, 15)):
        t[y0] += 0.3
        t[y0 + 1] += 0.1
        t[y1] -= 0.26
        t[y1 - 1] -= 0.1
        for x in range(1, 16, 4):
            xx, yy = (x + int(rng.integers(0, 2))) % 16, y0 + 2 + int(rng.integers(0, y1 - y0 - 4))
            t[yy:yy + 2, xx] -= 0.3
            t[yy:yy + 2, (xx - 1) % 16] += 0.14
    t[7] = 0.03 + rng.random(16) * 0.1
    return shade_n(np.clip(t, 0, 1), r)


def quern_top(name, pal, rng):
    """Millstone top: round stone on its darker bed, eight radial furrows, a deep central eye and an off-centre handle socket."""
    r = stone_ramp(pal)
    d = np.hypot(XX - 7.5, YY - 7.5)
    t = 0.62 + (fbm(16, 16, rng, (4, 2), (0.6, 0.4)) - 0.5) * 0.2 + (rng.random((16, 16)) - 0.5) * 0.1
    furrow = np.zeros((16, 16), dtype=bool)
    for a in range(8):
        th = a * TAU / 8 + 0.2
        pts = line_pts(round(7.5 + np.cos(th) * 2.8), round(7.5 + np.sin(th) * 2.8),
                       round(7.5 + np.cos(th) * 6.8), round(7.5 + np.sin(th) * 6.8))
        for x, y in pts:
            furrow[y, x] = True
    lit = ~furrow & np.roll(furrow, 1, 1) & (d < 7.0)
    t[furrow] -= 0.34
    t[lit] += 0.14
    t[(d >= 7.0) & (d < 7.8)] -= 0.1 + 0.02 * ((XX + YY)[(d >= 7.0) & (d < 7.8)] - 15)
    t[d >= 7.8] -= 0.24
    eye = d < 2.3
    t[eye] = 0.12 + 0.05 * ((XX + YY)[eye] - 15)
    t[d < 1.2] = 0.0
    for (x, y), v in (((11, 11), 0.08), ((12, 11), 0.1), ((11, 12), 0.12), ((12, 12), 0.22), ((13, 12), 0.7), ((12, 13), 0.7)):
        t[y, x] = v
    return shade_n(np.clip(t, 0, 1), r)


def sorting_top(name, pal, rng):
    """Chute bed from above (the chute's boards and rails) crossed by a bronze gate bar on a pivot boss, casting a shadow."""
    t, _rail = chute_tones(rng)
    b = bronze_ramp(7)
    bar = (YY >= 7) & (YY <= 8) & (XX >= 3) & (XX <= 13)
    pivot = (YY >= 6) & (YY <= 9) & (XX >= 2) & (XX <= 3)
    shadow = np.roll(np.roll(bar | pivot, 1, 0), 1, 1) & ~(bar | pivot)
    t[shadow] -= 0.24
    img = shade_n(np.clip(t, 0, 1), chute_ramp(pal))
    n = fbm(16, 16, rng, (4, 2))
    layer(img, 0.7 - (YY - 7) * 0.28 + (n - 0.5) * 0.2, b, bar)
    layer(img, relief(pivot, 0.6, 0.15, 2), b, pivot)
    img[7, 2, :3] = b[-1]
    img[8, 13, :3] = b[1]
    return img


STYLES = {
    "plank": plank, "beam": beam, "crate_side": crate_side, "crate_top": crate_top, "board_front": board_front,
    "chute": chute, "cork_board": cork_board, "winding_post": winding_post, "pulley_wheel": pulley_wheel,
    "pulley_side": pulley_side, "crucible_side": crucible_side, "crucible_top": crucible_top, "molten": molten,
    "quern_side": quern_side, "quern_top": quern_top, "sorting_top": sorting_top,
}

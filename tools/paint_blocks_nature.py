"""Natural block textures: ores, raw blocks, outcrops, geodes, crystal clusters and magma vents.

Every painter has the signature (name, palette, rng) -> 16x16 RGBA array; full-block textures tile.
"""

import numpy as np

from paint import fbm, mix, new, ramp_n, rng_for, shade_n, value_noise


def wput(img: np.ndarray, x: int, y: int, c) -> None:
    """Set a pixel with wrap-around so painted features tile seamlessly."""
    img[y % img.shape[0], x % img.shape[1]] = c


ORE_TINT = {"zinc": (0.9, 1.0, 1.06)}
"""Per-channel RGB factors that push an ore palette towards its in-world look (zinc: blue-grey-green, apart from tin)."""


def _rgba8(r: np.ndarray, i: int) -> tuple[int, int, int, int]:
    """Return shade i of an n-shade ramp as an opaque RGBA tuple."""
    c = r[max(0, min(len(r) - 1, i))]
    return int(c[0]), int(c[1]), int(c[2]), 255


def _shift(mask: np.ndarray, dx: int, dy: int) -> np.ndarray:
    """Return the mask moved by (dx, dy), without wrap-around."""
    h, w = mask.shape
    out = np.zeros_like(mask)
    out[max(0, dy):h + min(0, dy), max(0, dx):w + min(0, dx)] = mask[max(0, -dy):h - max(0, dy), max(0, -dx):w - max(0, dx)]
    return out


def _trim(*layers: np.ndarray) -> list[np.ndarray]:
    """Crop every layer to the bounding box of the first (a bool mask)."""
    ys, xs = np.nonzero(layers[0])
    return [a[ys.min():ys.max() + 1, xs.min():xs.max() + 1] for a in layers]


def _lump(rng: np.random.Generator, w: int, h: int) -> np.ndarray:
    """Return a rounded w x h lump mask with slightly jittered corners; lumps of 6+ pixels never keep all four corners."""
    yy, xx = np.mgrid[0:h, 0:w]
    d = ((xx - (w - 1) / 2) / (w / 2)) ** 2 + ((yy - (h - 1) / 2) / (h / 2)) ** 2
    m = d <= 0.85 + rng.random((h, w)) * 0.3
    if w * h >= 6 and m[0, 0] and m[0, -1] and m[-1, 0] and m[-1, -1]:
        m[-1, -1] = False
        if rng.random() < 0.5:
            m[[(0, 0), (0, -1), (-1, 0)][int(rng.integers(0, 3))]] = False
    return m


def _accrete(rng: np.random.Generator, size: int, box: int = 5) -> np.ndarray:
    """Return an irregular blob of `size` pixels (at most box x box) grown from one seed by adding random edge pixels.

    Edge pixels with more filled neighbours are likelier, so blobs stay compact but keep notches and one-pixel spurs.
    """
    n = 2 * box + 1
    m = np.zeros((n, n), dtype=bool)
    m[box, box] = True
    while m.sum() < size:
        ring = (_shift(m, 0, 1) | _shift(m, 0, -1) | _shift(m, 1, 0) | _shift(m, -1, 0)) & ~m
        ys, xs = np.nonzero(m)
        ok = []
        for y, x in zip(*np.nonzero(ring)):
            if max(ys.max(), y) - min(ys.min(), y) < box and max(xs.max(), x) - min(xs.min(), x) < box:
                nb = int(m[y - 1, x]) + int(m[y + 1, x]) + int(m[y, x - 1]) + int(m[y, x + 1])
                ok.append((y, x, 0.4 + nb ** 3))
        w = np.array([o[2] for o in ok])
        y, x, _ = ok[int(rng.choice(len(ok), p=w / w.sum()))]
        m[y, x] = True
    return _trim(m)[0]


def _mottle(mask: np.ndarray, rng: np.random.Generator, lo: int, hi: int) -> np.ndarray:
    """Return mottled shade indices (-1 outside): lit upper-left edges, shaded lower-right edges, noisy mixed tones inside,
    and one hi + 1 highlight near the top-left (a second one on blobs of 8+ pixels)."""
    h, w = mask.shape
    yy, xx = np.mgrid[0:h, 0:w]
    up, down = _shift(mask, 0, 1), _shift(mask, 0, -1)
    left, right = _shift(mask, 1, 0), _shift(mask, -1, 0)
    v = (lo + hi + 1) / 2 + 0.8 * ~up + 0.6 * ~left - 1.0 * ~down - 0.8 * ~right + rng.normal(0.0, 0.8, (h, w))
    idx = np.clip(np.rint(v), lo, hi).astype(int)
    score = np.where(mask, v - 0.6 * (xx + yy) + rng.random((h, w)), -99.0)
    for o in np.argsort(score, axis=None)[::-1][:1 + (mask.sum() >= 8)]:
        idx[o // w, o % w] = hi + 1
    return np.where(mask, idx, -1)


def _prism(m: np.ndarray, idx: np.ndarray, x: int, length: int, width: int, lean: int, tone: int) -> None:
    """Paint an upright crystal prism into a 5x5 box with its foot on the bottom row.

    Facets left to right: lit, mid (3-wide only), dark; a single bright tip pixel, a darker foot. A lean of -1 or 1 moves
    the upper half one pixel sideways; tone (-1..1) shifts the whole crystal so neighbours differ.
    """
    facets = {1: (4,), 2: (5, 2), 3: (6, 4, 2)}[width]
    for t in range(length):
        top = t == length - 1
        cols = [width // 2 if width == 3 else 0] if top and width > 1 else range(width)
        for c in cols:
            xx = x + c + lean * (t >= (length + 1) // 2)
            if not 0 <= xx <= 4:
                continue
            s = 7 if top else facets[c] + (t == length - 2 and c == 0) - (t == 0) + tone
            m[4 - t, xx] = True
            idx[4 - t, xx] = int(np.clip(s, 1, 7))


def _spike(m: np.ndarray, idx: np.ndarray, x: int, rise: int, length: int, step: int, steep: bool, tone: int) -> None:
    """Paint a one-pixel side crystal growing up and sideways (step -1 or 1) from (x, 4 - rise); pixels outside the box are dropped."""
    for t in range(length):
        xx, y = x + step * (t // 2 if steep else (t + 1) // 2), 4 - rise - t
        if 0 <= xx <= 4 and y >= 0:
            m[y, xx] = True
            idx[y, xx] = int(np.clip(7 if t == length - 1 else 3 + (t > 0) + tone, 1, 7))


def _shards(rng: np.random.Generator, kind: str) -> tuple[np.ndarray, np.ndarray]:
    """Return (mask, shade indices) of a zinc crystal feature in a 5x5 box.

    kind "fan": a main prism (2-3 px wide, 3-5 tall, maybe leaning) with one or two shorter side spikes leaning outwards;
    "stub": a short 2 px wide prism; "speck": a lone 1-2 px crystal. Every crystal gets its own tone.
    """
    m = np.zeros((5, 5), dtype=bool)
    idx = np.full((5, 5), -1)

    def tone() -> int:
        return int(rng.integers(-1, 2))

    if kind == "speck":
        _prism(m, idx, int(rng.integers(0, 5)), int(rng.integers(1, 3)), 1, 0, tone())
    elif kind == "stub":
        _prism(m, idx, int(rng.integers(0, 4)), int(rng.integers(2, 4)), 2, 0, tone())
    else:
        w = 3 if rng.random() < 0.35 else 2
        x = int(rng.integers(1, 5 - w))
        sides = [s for s in (-1, 1) if rng.random() < 0.7] or [(-1, 1)[int(rng.integers(0, 2))]]
        for s in sides:
            _spike(m, idx, x - 1 if s < 0 else x + w, int(rng.integers(0, 2)), int(rng.integers(2, 4)), s, rng.random() < 0.5, tone())
        _prism(m, idx, x, int(rng.integers(3, 6)), w, int(rng.integers(-1, 2)) * (rng.random() < 0.5), tone())
    return tuple(_trim(m, idx))


def _shade(mask: np.ndarray, rng: np.random.Generator, lo: int, hi: int) -> np.ndarray:
    """Return shade indices (-1 outside) lighting a lump from the top-left; the brightest top-left pixel gets hi + 1."""
    h, w = mask.shape
    yy, xx = np.mgrid[0:h, 0:w]
    ys, xs = np.nonzero(mask)
    grad = ((xs.mean() - xx) + (ys.mean() - yy)) / max(1.5, (w + h) / 2.5)
    up, down = _shift(mask, 0, 1), _shift(mask, 0, -1)
    left, right = _shift(mask, 1, 0), _shift(mask, -1, 0)
    v = (lo + hi) / 2 + 0.9 * ~up + 0.7 * ~left - 1.0 * ~down - 0.9 * ~right + grad + (rng.random((h, w)) - 0.5) * 0.7
    idx = np.clip(np.rint(v), lo, hi).astype(int)
    score = np.where(mask, v - 0.05 * (xx + yy), -99.0)
    y, x = np.unravel_index(int(np.argmax(score)), score.shape)
    idx[y, x] = hi + 1
    return np.where(mask, idx, -1)


def _vein(rng: np.random.Generator) -> tuple[np.ndarray, np.ndarray]:
    """Return (mask, shade indices) of a short glowing crack with one bright rounded node on it."""
    n = int(rng.integers(5, 9))
    m = np.zeros((8, 9), dtype=bool)
    x, y, dy = 0, int(rng.integers(2, 6)), 1 if rng.random() < 0.5 else -1
    path = []
    for _ in range(n):
        path.append((x, y))
        m[y, x] = True
        x += 1
        if rng.random() < 0.55:
            y = min(7, max(0, y + dy))
            if rng.random() < 0.5:
                m[y, x - 1] = True
        if rng.random() < 0.2:
            dy = -dy
    idx = np.where(m, 3 + (rng.random(m.shape) < 0.4), -1)
    nx, ny = path[int(rng.integers(1, n - 1))]
    node = np.zeros_like(m)
    w, h = int(rng.integers(2, 4)), 2 + (rng.random() < 0.4)
    ox, oy = max(0, nx - w // 2), max(0, min(8 - h, ny - h // 2))
    node[oy:oy + h, ox:ox + w] = _lump(rng, w, h)[:, :min(w, 9 - ox)]
    nidx = _shade(node, rng, 5, 7)
    idx = np.where(node, nidx, idx)
    return tuple(_trim(m | node, idx))


def _rims(mask: np.ndarray) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Return (padded mask, lower-right rim, upper-left rim) of a feature padded by one pixel on every side."""
    pm = np.pad(mask, 1)
    lower = (_shift(pm, 0, 1) | _shift(pm, 1, 0) | _shift(pm, 1, 1)) & ~pm
    upper = (_shift(pm, 0, -1) | _shift(pm, -1, 0)) & ~pm & ~lower
    return pm, lower, upper


def _stamp(img: np.ndarray, occ: np.ndarray, rng: np.random.Generator, mask: np.ndarray, idx: np.ndarray, metal: np.ndarray,
           rims: tuple, plain: bool = False) -> bool:
    """Paint one feature with its contact rims at a random free spot at least 1px inside the tile; False when none is free.

    Features never cross the tile edge and their rims never overlap another feature's rims, so a 3x3 tiling shows no seams
    and no merged clusters. plain (metal ores) keeps only part of the lower-right rim, a broken contact shadow instead of an
    outline, and picks the best of 5 random spots by wrapped distance to earlier features, so the spread has no clumps or rows.
    """
    h, w = mask.shape
    pm, lower, upper = _rims(mask)
    foot = pm | lower | upper
    spots = []
    for y0 in range(1, 16 - h):
        for x0 in range(1, 16 - w):
            t = np.zeros((16, 16), dtype=bool)
            t[y0 - 1:y0 + h + 1, x0 - 1:x0 + w + 1] = foot
            if not (t & occ).any():
                spots.append((x0, y0, t))
    if not spots:
        return False
    if plain and occ.any():
        oy, ox = np.nonzero(occ)

        def gap(s) -> float:
            cx, cy = s[0] + w / 2, s[1] + h / 2
            dx, dy = np.abs(ox - cx), np.abs(oy - cy)
            return float(np.min(np.hypot(np.minimum(dx, 16 - dx), np.minimum(dy, 16 - dy))))

        x0, y0, t = max((spots[int(rng.integers(0, len(spots)))] for _ in range(5)), key=gap)
    else:
        x0, y0, t = spots[int(rng.integers(0, len(spots)))]
    occ |= t
    view = img[y0 - 1:y0 + h + 1, x0 - 1:x0 + w + 1]
    view[lower & (rng.random(pm.shape) < 0.65) if plain else lower] = rims[0]
    view[upper & (rng.random(pm.shape) < 0.55)] = rims[1]
    pidx = np.pad(idx, 1, constant_values=-1)
    for y, x in zip(*np.nonzero(pm)):
        view[y, x] = _rgba8(metal, int(pidx[y, x]))
    return True


def _overlay(pal: str, ground: str, rng: np.random.Generator, features, need: int, glow: bool = False) -> np.ndarray:
    """Return a transparent 16x16 ore overlay of the given features with contact rims tuned to the ground (stone or deepslate).

    Feature shade indices address the 8-shade ramp_n of the palette (ORE_TINT applied). A plain ore gets a broken dark
    contact rim below and right and a light rock lip above and left; a glow ore adds shade 8 (a near-white core), a full
    dark lower rim and a dim metal halo above and left.
    """
    metal, g = ramp_n(pal, 8, 0.3), ramp_n(ground, 8, 0.3)
    if pal in ORE_TINT:
        metal = np.clip(np.rint(metal * np.array(ORE_TINT[pal])), 0, 255).astype(np.int64)
    if glow:
        metal = np.vstack([metal, np.rint((metal[-1] + 255) / 2).astype(np.int64)])
    if glow:
        rims = (mix(metal[0], g[0], 0.5), mix(metal[3], g[3], 0.5))
    else:
        rims = (mix(metal[1], g[2], 0.55), mix(metal[4], g[5], 0.6))
    img, occ = new(16, 16), np.zeros((16, 16), dtype=bool)
    placed = sum(_stamp(img, occ, rng, m, i, metal, rims, plain=not glow) for m, i in features)
    if placed < need:
        raise ValueError(f"ore overlay '{pal}' on {ground}: placed {placed} features, need {need}; adjust the feature sizes")
    return img


def _ore_features(pal: str, rng: np.random.Generator) -> list:
    """Return the shaded features of a metal ore: crystal fans, stubs and specks for zinc, mottled irregular blobs of mixed
    sizes (large clusters down to 1-2 px specks) for every other metal."""
    if pal == "zinc":
        return [_shards(rng, k) for k in ("fan", "fan", "fan", "fan", "stub", "speck", "speck", "speck")]
    out = []
    for lo, hi in ((12, 16), (10, 14), (7, 10), (5, 7), (3, 5), (2, 3), (1, 2), (1, 2)):
        m = _accrete(rng, int(rng.integers(lo, hi + 1)))
        out.append((m, _mottle(m, rng, 1, 6)))
    return out


def ore_stone(name, pal, rng):
    """Transparent ore overlay for vanilla stone: shaded metal clusters with dark contact rims."""
    return _overlay(pal, "stone", rng, _ore_features(pal, rng), 5)


def ore_deepslate(name, pal, rng):
    """Transparent ore overlay for vanilla deepslate: shaded metal clusters with dark contact rims."""
    return _overlay(pal, "deepslate", rng, _ore_features(pal, rng), 5)


def ore_deepslate_glow(name, pal, rng):
    """Transparent ore overlay for vanilla deepslate: glowing cracks of the metal, each with a bright node, plus small sparks."""
    feats = [_vein(rng) for _ in range(3)]
    for _ in range(3):
        m = _lump(rng, 2, 2 if rng.random() < 0.5 else 1)
        feats.append((m, _shade(m, rng, 4, 5)))
    return _overlay(pal, "deepslate", rng, feats, 5, glow=True)


LIGHT = np.array((-0.5, -0.6, 0.62)) / np.linalg.norm((-0.5, -0.6, 0.62))
"""Unit light vector (x right, y down, z out of the face): vanilla lights its textures from the upper left."""


def _metal(pal: str, n: int = 8, glow: bool = False) -> np.ndarray:
    """Return the n-shade ramp_n of a palette with ORE_TINT applied; glow appends a near-white core shade."""
    r = ramp_n(pal, n, 0.3)
    if pal in ORE_TINT:
        r = np.clip(np.rint(r * np.array(ORE_TINT[pal])), 0, 255).astype(np.int64)
    if glow:
        r = np.vstack([r, np.rint((r[-1] + 255) / 2).astype(np.int64)])
    return r


def _paint(r: np.ndarray, idx: np.ndarray, mask: np.ndarray | None = None) -> np.ndarray:
    """Return the RGBA image of integer shade indices into ramp r (clipped to it); pixels outside mask stay clear."""
    return shade_n(np.clip(idx, 0, len(r) - 1) / (len(r) - 1), r, mask)


def _sites(rng: np.random.Generator, n: int) -> tuple[np.ndarray, np.ndarray]:
    """Return wrapped offsets (dx, dy), each (n, 16, 16), from n sites to every pixel centre.

    Each site is the best of 10 random candidates by wrapped distance to the earlier sites (best-candidate sampling), so the
    sites neither clump nor line up, and every field built from the offsets tiles seamlessly.
    """
    pts = [rng.random(2) * 16]
    for _ in range(n - 1):
        cand = rng.random((10, 2)) * 16
        dd = np.abs(cand[:, None] - np.array(pts)[None]) % 16
        dd = np.minimum(dd, 16 - dd)
        pts.append(cand[int(np.argmax(np.hypot(dd[..., 0], dd[..., 1]).min(axis=1)))])
    pts = np.array(pts)
    yy, xx = np.mgrid[0:16, 0:16].astype(np.float64) + 0.5
    return (xx[None] - pts[:, 0, None, None] + 8) % 16 - 8, (yy[None] - pts[:, 1, None, None] + 8) % 16 - 8


def _nearest(d: np.ndarray, dx: np.ndarray, dy: np.ndarray):
    """Return (owner, second, d1, d2, odx, ody): the nearest and second-nearest site per pixel by the (n, 16, 16) field d,
    their field values, and the offset of the pixel from its owner site."""
    order = np.argsort(d, axis=0)

    def take(a: np.ndarray, k: int) -> np.ndarray:
        return np.take_along_axis(a, order[k:k + 1], 0)[0]

    return order[0], order[1], take(d, 0), take(d, 1), take(dx, 0), take(dy, 0)


def _raw(rng: np.random.Generator, faceted: bool, hi: int) -> np.ndarray:
    """Return 16x16 shade indices (0..hi) of fused raw-ore nuggets in a darker matrix, like vanilla raw iron; tileable.

    Nuggets are lumpy discs lit from the upper left, shaded from 2 on their lower-right flank up to 6 with one brightest pixel
    at hi, so they blend softly into the matrix; faceted nuggets are cut into five flat faces around a flat table, like crystal
    chunks. Fused nuggets get a soft crease on their lower-right side; the matrix uses shades 0..2 with a contact shadow below
    and right of each nugget.
    """
    n = 8
    dx, dy = _sites(rng, n)
    rad = 2.3 + 1.1 * rng.random(n)
    lobes = 1 + 0.22 * np.sin(rng.integers(2, 5, n)[:, None, None] * np.arctan2(dy, dx) + rng.random(n)[:, None, None] * 2 * np.pi)
    q = np.hypot(dx, dy) / (rad[:, None, None] * lobes) + (value_noise(16, 16, rng, 4)[None] - 0.5) * 0.45
    own, _second, q1, q2, ox, oy = _nearest(q, dx, dy)
    ux, uy = ox / rad[own], oy / rad[own]
    inside = q1 < 1.0
    if faceted:
        step = 2 * np.pi / 5
        rot = (rng.random(n) * step)[own]
        a = (np.floor((np.arctan2(uy, ux) + rot) / step) + 0.5) * step - rot
        table = q1 < 0.45
        nx, ny = np.where(table, 0.0, 0.75 * np.cos(a)), np.where(table, 0.0, 0.75 * np.sin(a))
    else:
        nx, ny = 0.85 * ux, 0.85 * uy
    nz = np.sqrt(np.clip(1 - nx ** 2 - ny ** 2, 0.04, None))
    lam = (nx * LIGHT[0] + ny * LIGHT[1] + nz * LIGHT[2]) / np.sqrt(nx ** 2 + ny ** 2 + nz ** 2)
    v = 1.8 + 4.6 * np.clip(lam, 0, 1) ** 1.2 + rng.uniform(-0.4, 0.4, n)[own] + rng.normal(0, 0.25, (16, 16))
    crease = inside & (q2 < 1.05) & (q2 - q1 < 0.22) & (ux + uy > 0)
    nug = np.clip(np.rint(v - 1.2 * crease), 2, 6)
    shadow = (np.roll(inside, 1, 0) | np.roll(inside, 1, 1) | np.roll(inside, (1, 1), (0, 1))) & ~inside
    tone = 0.5 + 1.6 * fbm(16, 16, rng, (4, 2), (0.6, 0.4)) + rng.normal(0, 0.25, (16, 16)) - 0.9 * shadow
    idx = np.where(inside, nug, np.clip(np.rint(tone), 0, 2)).astype(int)
    for j in range(n):
        m = inside & (own == j)
        if m.sum() >= 6:
            s = np.where(m, lam, -9.0)
            idx[np.unravel_index(int(np.argmax(s)), s.shape)] = hi
    return idx


def raw_block(name, pal, rng):
    """Raw ore storage block like vanilla raw iron: fused, softly lit metal nuggets in a darker matrix of the same ramp; tileable."""
    return _paint(_metal(pal), _raw(rng, False, 7))


def raw_block_crystal(name, pal, rng):
    """Raw ore storage block of faceted crystal chunks in a darker matrix (zinc, matching its crystal ore); tileable."""
    return _paint(_metal(pal), _raw(rng, True, 7))


def raw_block_glow(name, pal, rng):
    """Raw ore storage block of softly lit nuggets whose brightest pixel glows near-white (vantite, matching its ore); tileable."""
    return _paint(_metal(pal, glow=True), _raw(rng, False, 8))


def _gap(spot: tuple[int, int], occ: np.ndarray, shape: tuple[int, int]) -> float:
    """Return the wrapped distance from the centre of a feature box of shape (h, w) at spot (x0, y0) to the nearest occupied
    pixel (0 when nothing is occupied yet)."""
    oy, ox = np.nonzero(occ)
    if not len(ox):
        return 0.0
    ddx, ddy = np.abs(ox - spot[0] - shape[1] / 2) % 16, np.abs(oy - spot[1] - shape[0] / 2) % 16
    return float(np.min(np.hypot(np.minimum(ddx, 16 - ddx), np.minimum(ddy, 16 - ddy))))


def _scatter(rng: np.random.Generator, feats: list, ground: str, need: int) -> np.ndarray:
    """Return a transparent 16x16 overlay of (mask, shade indices, ramp) features with contact rims tuned to the ground.

    Unlike _stamp, features wrap around the tile edge (tiling shows no seams) and reach the border rows, which the outcrop
    faces sample. Each spot is the best of 6 random free ones by wrapped distance to earlier features, and rims never overlap.
    Every feature gets the plain-ore rims of _overlay (a broken dark rim below and right, a light rock lip above and left),
    also glowing ones, whose full dark rims would read as holes on light stone. Raises ValueError when fewer than need fit.
    """
    g = ramp_n(ground, 8, 0.3)
    img, occ = new(16, 16), np.zeros((16, 16), dtype=bool)
    placed = 0
    for mask, idx, metal in feats:
        rims = (mix(metal[1], g[2], 0.55), mix(metal[4], g[5], 0.6))
        pm, lower, upper = _rims(mask)
        fy, fx = np.nonzero(pm | lower | upper)
        free = [(x0, y0) for y0 in range(16) for x0 in range(16) if not occ[(fy + y0) % 16, (fx + x0) % 16].any()]
        if not free:
            continue
        cands = [free[int(rng.integers(0, len(free)))] for _ in range(6)]
        x0, y0 = cands[int(np.argmax([_gap(s, occ, pm.shape) for s in cands]))]
        occ[(fy + y0) % 16, (fx + x0) % 16] = True
        for layer, c in ((lower & (rng.random(pm.shape) < 0.65), rims[0]), (upper & (rng.random(pm.shape) < 0.55), rims[1])):
            ys, xs = np.nonzero(layer)
            img[(ys + y0) % 16, (xs + x0) % 16] = c
        pidx = np.pad(idx, 1, constant_values=-1)
        for y, x in zip(*np.nonzero(pm)):
            img[(y + y0) % 16, (x + x0) % 16] = _rgba8(metal, int(pidx[y, x]))
        placed += 1
    if placed < need:
        raise ValueError(f"outcrop overlay on {ground}: placed {placed} features, need {need}; make the features smaller")
    return img


def _outcrop_features(pal: str, rng: np.random.Generator) -> list:
    """Return (mask, shade indices, ramp) outcrop specks, smaller than ore features since the rock faces are 2 to 8 px: zinc
    crystal stubs and specks, glowing vantite nodes, and mottled blobs for every other metal (copper with two verdigris ones)."""
    if pal == "zinc":
        metal = _metal(pal)
        return [(*_shards(rng, k), metal) for k in ("fan", "stub", "stub", "stub", "speck", "speck", "speck", "speck", "speck")]
    if pal == "vantite":
        metal, out = _metal(pal, glow=True), []
        for w, h in ((3, 2), (2, 3), (2, 2), (2, 2), (2, 1), (1, 2), (1, 1), (1, 1), (1, 1)):
            m = _lump(rng, w, h)
            out.append((m, _shade(m, rng, 5, 7) if w * h >= 4 else _shade(m, rng, 4, 5), metal))
        return out
    metal, green, out = _metal(pal), _metal("verdigris") if pal == "copper" else None, []
    for k, (lo, hi) in enumerate(((8, 10), (6, 8), (4, 6), (4, 5), (3, 4), (3, 4), (2, 3), (2, 3), (2, 2))):
        m = _accrete(rng, int(rng.integers(lo, hi + 1)))
        out.append((m, _mottle(m, rng, 1, 6), green if green is not None and k in (2, 6) else metal))
    return out


def outcrop_specks(name, pal, rng):
    """Transparent ore overlay for the outcrop rock pile, drawn over vanilla stone (each pile face samples a part of it):
    small ore specks with contact rims spread over the whole tile, wrapping."""
    return _scatter(rng, _outcrop_features(pal, rng), "stone", 8)


def _crystal(idx: np.ndarray, base: tuple[float, float], tip: tuple[float, float], half: float, tone: int) -> None:
    """Paint one crystal from base to tip (pixel coordinates, y down) into the shade indices idx (-1 = clear), in place.

    Across its axis: a lit left facet, a mid ridge and a dark right facet with a darker outer edge; along it the crystal
    brightens towards the tip, tapers over its last third and ends in one pale tip pixel (shade 8); the foot is a shade darker.
    Pixels of earlier crystals just right of and below this one turn a shade darker, so overlapping crystals stay apart.
    """
    (bx, by), (tx, ty) = base, tip
    length = float(np.hypot(tx - bx, ty - by))
    ux, uy = (tx - bx) / length, (ty - by) / length
    yy, xx = np.mgrid[0:16, 0:16].astype(np.float64) + 0.5
    px, py = xx - bx, yy - by
    t = (px * ux + py * uy) / length
    s = -px * uy + py * ux
    w = half * np.clip((1.0 - t) / 0.35, 0.18, 1.0)
    m = (t >= 0) & (t <= 1) & (np.abs(s) <= w)
    f = s / np.maximum(w, 0.5)
    v = np.where(f < -0.34, 5, np.where(f > 0.34, 3, 4)) + np.rint(2.0 * t) - (t < 0.15) + ((f < -0.34) & (t > 0.7)) + tone
    v = v - (m & ~_shift(m, -1, 0) & (f > 0))
    ring = (_shift(m, 1, 0) | _shift(m, 0, 1) | _shift(m, 1, 1)) & ~m & (idx >= 0)
    idx[ring] = np.maximum(idx[ring] - 1, 2)
    idx[m] = np.clip(v[m], 2, 7)
    tips = np.where(m, t, -1.0)
    idx[np.unravel_index(int(np.argmax(tips)), tips.shape)] = 8


def crystal_cross(name, pal, rng):
    """Cross-model sprite like vanilla amethyst clusters: a tall central crystal, two crystals leaning out behind it and two short
    ones in front, faceted and lit from the upper left on a transparent ground (9-shade ramp, no outline)."""
    idx = np.full((16, 16), -1)

    def j() -> float:
        return float(rng.uniform(-0.6, 0.6))

    for base, tip, half, tone in (((5.5, 16.0), (1.2 + j(), 8.0 + j()), 1.6, 0), ((10.5, 16.0), (14.8 + j(), 8.8 + j()), 1.6, 0),
                                  ((8.0, 16.0), (8.0 + j(), 1.2), 2.7, 0), ((4.0, 16.0), (3.0 + j(), 12.2 + j()), 1.1, -1),
                                  ((12.0, 16.0), (12.8 + j(), 12.6 + j()), 1.1, -1)):
        _crystal(idx, base, tip, half, tone)
    return _paint(_metal(pal, 9), idx, idx >= 0)


def _shell(rng: np.random.Generator) -> tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    """Return (shade indices 1..7, owner, second, gap) of a tileable faceted crystal shell like a dark amethyst block; gap is
    each pixel's distance margin to the nearest facet boundary (d2 - d1).

    Each Voronoi facet is a flat tilted plane with its own tone and slope; a boundary pixel is lit on its facet's upper-left side
    and creased dark on the lower-right side, and five pale glints (one bright pixel, one softer beside it) sit on lit boundaries.
    """
    n = 12
    dx, dy = _sites(rng, n)
    own, sec, d1, d2, ox, oy = _nearest(np.hypot(dx, dy), dx, dy)
    th = rng.random(n) * 2 * np.pi
    v = rng.uniform(2.0, 3.6, n)[own] + 0.35 * (np.cos(th)[own] * ox + np.sin(th)[own] * oy) + (fbm(16, 16, rng, (4, 2), (0.6, 0.4)) - 0.5) * 1.2
    edge = (d2 - d1) < 0.9
    lower = edge & (ox + oy > 0)
    idx = np.clip(np.rint(v - 1.1 * lower + 0.8 * (edge & ~lower)), 1, 5).astype(int)
    lit = np.argwhere(edge & ~lower)
    for k in rng.permutation(len(lit))[:5]:
        y, x = lit[k]
        idx[y, x] = 7
        idx[(y + 1) % 16, (x - 1) % 16] = max(idx[(y + 1) % 16, (x - 1) % 16], 6)
    return idx, own, sec, d2 - d1


def geode_shell(name, pal, rng):
    """Lattice geode inner layer: dark faceted violet crystal mosaic with pale glints, like a darker amethyst block; tileable."""
    return _paint(_metal(pal), _shell(rng)[0])


def geode_shell_charged(name, pal, rng):
    """Charged geode shell: the plain shell's exact facets (painted from the plain block's seed, so charging only adds light) with
    thin glowing seams along about 40 % of the facet boundaries, a faint halo beside them and near-white sparks where glowing
    seams meet; tileable."""
    idx, own, sec, gap = _shell(rng_for("block", name.removesuffix("_charged")))
    keys = np.minimum(own, sec) * 64 + np.maximum(own, sec)
    pairs = np.unique(keys[gap < 0.9])
    vein = (gap < 0.55) & np.isin(keys, rng.choice(pairs, size=max(1, int(len(pairs) * 0.4)), replace=False))
    halo = (np.roll(vein, 1, 0) | np.roll(vein, -1, 0) | np.roll(vein, 1, 1) | np.roll(vein, -1, 1)) & ~vein
    idx = np.where(halo, np.minimum(idx + 1, 5), idx)
    idx = np.where(vein, 6 + (value_noise(16, 16, rng, 2) > 0.6), idx)
    near = np.stack([np.roll(own, (a, b), (0, 1)) for a in (-1, 0, 1) for b in (-1, 0, 1)])
    srt = np.sort(near, axis=0)
    junction = vein & ((srt[1:] != srt[:-1]).sum(axis=0) >= 2)
    idx[junction] = 8
    spots = np.argwhere(vein & ~junction)
    for k in rng.permutation(len(spots))[:3]:
        idx[tuple(spots[k])] = 8
    return _paint(_metal(pal, glow=True), idx)


def magma_vent(name, pal, rng, phase=0.0):
    """Cave-floor magma vent like a cracked vanilla magma block: dark basalt plates, raised and lit from the upper left, split by
    a network of thin glowing cracks that widen and brighten towards a small molten opening in the centre; plate edges along
    the cracks are heated dark red. A heat wave runs outwards through the cracks and the opening flickers; phase (0..1) is the
    animation frame (the loop is seamless and the geometry is the same in every frame). Tileable."""
    rock, hot = _metal("slag", 6), _metal(pal, 8, glow=True)
    n = 11
    dx, dy = _sites(rng, n)
    own, _sec, d1, d2, ox, oy = _nearest(np.hypot(dx, dy), dx, dy)
    yy, xx = np.mgrid[0:16, 0:16].astype(np.float64) + 0.5
    rc = np.hypot(xx - 8, yy - 8)
    near = np.clip(1 - rc / 7, 0, 1)
    gap = d2 - d1
    w = 0.6 + 0.9 * near ** 2
    crack = gap < w
    core = rc + (value_noise(16, 16, rng, 4) - 0.5) * 1.4 < 1.7
    flicker = value_noise(16, 16, rng, 2)
    plate = rng.uniform(1.2, 2.2, n)[own] - 0.3 * (ox + oy) + (fbm(16, 16, rng, (4, 2), (0.6, 0.4)) - 0.5) * 0.8
    rim = ~crack & ~core & (gap < w + 0.8)
    wave = np.cos(2 * np.pi * (phase - rc / 10))
    img = _paint(rock, np.clip(np.rint(plate - 0.6 * rim), 0, 3).astype(int))
    heat = 2.6 + 1.6 * (1 - gap / w) + 3.0 * near + 0.8 * wave
    heat = np.where(core, 7.2 + 0.8 * (1 - rc / 1.7) + 0.5 * np.cos(2 * np.pi * (phase + 2 * flicker)), heat)
    heat = np.where(rim, 1.0 + 0.6 * near + 0.6 * (wave > 0.4), heat)
    glow = crack | core | (rim & ((near > 0.15) | (wave > 0.4)))
    out = _paint(hot, np.clip(np.rint(heat), 1, 8).astype(int))
    img[glow] = out[glow]
    return img


STYLES = {
    "ore_stone": ore_stone, "ore_deepslate": ore_deepslate, "ore_deepslate_glow": ore_deepslate_glow,
    "raw_block": raw_block, "raw_block_crystal": raw_block_crystal, "raw_block_glow": raw_block_glow,
    "outcrop_specks": outcrop_specks, "geode_shell": geode_shell, "geode_shell_charged": geode_shell_charged,
    "crystal_cross": crystal_cross, "magma_vent": magma_vent,
}

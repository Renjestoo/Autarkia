"""Natural block textures: ores, raw blocks, outcrops, geodes, crystal clusters and magma vents.

Every painter has the signature (name, palette, rng) -> 16x16 RGBA array; full-block textures tile.
"""

import numpy as np

from paint import add_outline, col, dither, fbm, mix, new, poly_mask, put, ramp, shade_img, value_noise

FLECKS = [
    [(1, 0), (2, 0), (0, 1), (1, 1), (2, 1), (3, 1), (1, 2), (2, 2)],
    [(0, 0), (1, 0), (0, 1), (1, 1), (2, 1), (1, 2), (2, 2), (3, 2)],
    [(1, 0), (0, 1), (1, 1), (2, 1), (0, 2), (1, 2)],
    [(0, 0), (1, 0), (2, 0), (1, 1), (2, 1), (2, 2)],
    [(1, 0), (2, 0), (0, 1), (1, 1), (2, 1), (0, 2), (1, 2)],
]
SHARDS = [
    [(1, 0), (0, 1), (1, 1), (2, 1), (0, 2), (1, 2), (2, 2), (1, 3)],
    [(2, 0), (1, 1), (2, 1), (0, 2), (1, 2), (0, 3)],
    [(1, 0), (1, 1), (0, 2), (1, 2), (2, 2), (1, 3)],
    [(0, 0), (1, 1), (2, 1), (1, 2), (2, 2), (3, 3)],
]


def wput(img: np.ndarray, x: int, y: int, c) -> None:
    """Set a pixel with wrap-around so painted features tile seamlessly."""
    img[y % img.shape[0], x % img.shape[1]] = c


def stone_base(rng: np.random.Generator, r: np.ndarray | None = None) -> np.ndarray:
    """Return tileable stone: soft blotches of shades 1-3 with sparse lit pits."""
    r = ramp("stone") if r is None else r
    n = fbm(16, 16, rng, (8, 4, 2), (0.45, 0.35, 0.2))
    img = shade_img(r, dither(1.15 + n * 2.0 - 0.5, 0.5).clip(0, 4))
    for _ in range(5):
        x, y = int(rng.integers(0, 16)), int(rng.integers(0, 16))
        wput(img, x, y, col(r, 0))
        wput(img, x - 1, y - 1, col(r, 3))
    return img


def deepslate_base(rng: np.random.Generator) -> np.ndarray:
    """Return tileable deepslate: dark layered rock with horizontal streaks."""
    r = ramp("deepslate")
    n = value_noise(16, 16, rng, 8) * 0.6 + value_noise(16, 16, rng, 4) * 0.4
    rows = rng.random(16)
    rows = (rows + np.roll(rows, 1) + np.roll(rows, -1)) / 3.0
    img = shade_img(r, dither(1.0 + n * 1.6 + (rows[:, None] - 0.5) * 1.2 - 0.5, 0.4).clip(0, 4))
    for _ in range(6):
        y, x, ln = int(rng.integers(0, 16)), int(rng.integers(0, 16)), int(rng.integers(3, 7))
        for k in range(ln):
            wput(img, x + k, y, col(r, 0))
            if rng.random() < 0.5:
                wput(img, x + k, y - 1, col(r, 3))
    return img


def _cluster_sites(rng: np.random.Generator, count: int) -> list[tuple[int, int]]:
    """Return jittered, well separated fleck positions on a 3x2 grid (wrapping)."""
    cells = [(cx, cy) for cy in range(2) for cx in range(3)]
    order = rng.permutation(len(cells))[:count]
    return [(int(cells[i][0] * 5.33 + rng.integers(0, 3)), int(cells[i][1] * 8 + rng.integers(0, 5))) for i in order]


def paint_fleck(img: np.ndarray, x0: int, y0: int, pattern, metal: np.ndarray, shadow) -> None:
    """Paint one lit ore fleck: highlight top-left, shaded bottom-right, drop shadow outside."""
    pts = set(pattern)
    tl = min(pts, key=lambda p: (p[0] + p[1], p[1]))
    for x, y in pts:
        if (x, y) == tl:
            c = col(metal, 4)
        elif (x + 1, y + 1) not in pts and ((x + 1, y) not in pts or (x, y + 1) not in pts):
            c = col(metal, 1)
        elif (x - 1, y) not in pts or (x, y - 1) not in pts:
            c = col(metal, 3)
        else:
            c = col(metal, 2)
        wput(img, x0 + x, y0 + y, c)
    if shadow is not None:
        for x, y in pts:
            for dx, dy in ((1, 1), (1, 0), (0, 1)):
                if (x + dx, y + dy) not in pts and rng_free(img, x0 + x + dx, y0 + y + dy, metal):
                    wput(img, x0 + x + dx, y0 + y + dy, shadow)


def rng_free(img: np.ndarray, x: int, y: int, metal: np.ndarray) -> bool:
    """Return True when the wrapped pixel is not already a metal fleck pixel."""
    px = img[y % img.shape[0], x % img.shape[1], :3]
    return not any(np.array_equal(px, m) for m in metal)


def _ore(img: np.ndarray, pal: str, rng: np.random.Generator, shadow, count: int = 6) -> np.ndarray:
    metal = ramp(pal)
    pats = SHARDS if pal in ("zinc", "vantite") else FLECKS
    for x, y in _cluster_sites(rng, count):
        paint_fleck(img, x, y, pats[int(rng.integers(0, len(pats)))], metal, shadow)
    return img


def ore_stone(name, pal, rng):
    """Stone with metal flecks."""
    return _ore(stone_base(rng), pal, rng, col(ramp("stone"), 0))


def ore_deepslate(name, pal, rng):
    """Deepslate with metal flecks."""
    return _ore(deepslate_base(rng), pal, rng, col(ramp("deepslate"), 0))


def ore_deepslate_glow(name, pal, rng):
    """Deepslate crossed by glowing veins of the metal with bright crystal nodes."""
    img = deepslate_base(rng)
    m = ramp(pal)
    for _ in range(3):
        x, y = int(rng.integers(0, 16)), int(rng.integers(0, 16))
        dx = 1 if rng.random() < 0.5 else -1
        for k in range(10):
            wput(img, x, y + 1, col(m, 1))
            wput(img, x, y, col(m, 3 if k % 3 else 4))
            x += dx if rng.random() < 0.7 else 0
            y += 1 if rng.random() < 0.5 else 0
            if rng.random() < 0.15:
                dx = -dx
    for x, y in _cluster_sites(rng, 3):
        paint_fleck(img, x, y, SHARDS[1], m, col(m, 0))
        wput(img, x + 1, y + 1, (255, 255, 255, 255))
    return img


def voronoi(rng: np.random.Generator, n: int, size: int = 16):
    """Return (owner, d1, d2, sdx, sdy) of a tileable Voronoi diagram with n random sites."""
    pts = rng.random((n, 2)) * size
    yy, xx = np.mgrid[0:size, 0:size].astype(float) + 0.5
    dx = (xx[None] - pts[:, 0, None, None] + size / 2) % size - size / 2
    dy = (yy[None] - pts[:, 1, None, None] + size / 2) % size - size / 2
    d = np.hypot(dx, dy)
    order = np.argsort(d, axis=0)
    owner = order[0]
    srt = np.take_along_axis(d, order, axis=0)
    sdx = np.take_along_axis(dx, owner[None], axis=0)[0]
    sdy = np.take_along_axis(dy, owner[None], axis=0)[0]
    return owner, srt[0], srt[1], sdx, sdy


def lumps(r: np.ndarray, rng: np.random.Generator, n: int, base: float = 2.1, light: float = 0.22) -> np.ndarray:
    """Return tileable rounded lumps lit from the top-left with dark crevices between them."""
    owner, d1, d2, sdx, sdy = voronoi(rng, n)
    tone = rng.random(n) * 0.6 - 0.3
    vals = base + tone[owner] - light * (sdx + sdy) - 0.06 * d1
    img = shade_img(r, dither(vals - 0.5, 0.4).clip(0, 4))
    crev = (d2 - d1) < 1.0
    img[crev] = col(r, 0)
    p = np.roll(np.roll(crev, -1, 0), -1, 1)
    img[p & ~crev] = col(r, 1)
    q = np.roll(np.roll(crev, 1, 0), 1, 1)
    img[q & ~crev & ~p] = col(r, 3)
    return img


def raw_block(name, pal, rng):
    """Chunky raw metal lumps with glints."""
    r = ramp(pal)
    img = lumps(r, rng, 7)
    for _ in range(4):
        x, y = int(rng.integers(0, 16)), int(rng.integers(0, 16))
        if img[y, x, :3].tolist() != list(r[0]):
            wput(img, x, y, col(r, 4))
    return img


def outcrop_base(name, pal, rng):
    """Small rounded rocks with dark gaps (stone)."""
    return lumps(ramp(pal), rng, 9, base=2.0, light=0.2)


def outcrop_specks(name, pal, rng):
    """Transparent overlay with only lit metal specks."""
    img = new(16, 16)
    m = ramp(pal)
    pats = SHARDS if pal in ("zinc", "vantite") else FLECKS
    for x, y in _cluster_sites(rng, 6):
        paint_fleck(img, x, y, pats[int(rng.integers(0, len(pats)))], m, mix(col(m, 0), (0, 0, 0, 255), 0.4))
    return img


def geode_shell(name, pal, rng):
    """Dark violet shell with small crystal speckles."""
    r = ramp(pal)
    n = fbm(16, 16, rng, (8, 4, 2))
    img = shade_img(r, dither(n * 1.6 - 0.3, 0.5).clip(0, 2))
    for _ in range(7):
        x, y = int(rng.integers(0, 16)), int(rng.integers(0, 16))
        wput(img, x, y, col(r, 3))
        wput(img, x + 1, y, col(r, 2))
        wput(img, x, y - 1, col(r, 4))
    return img


def geode_shell_charged(name, pal, rng):
    """Geode shell with bright glowing veins and white sparks."""
    img = geode_shell(name, pal, rng)
    r = ramp(pal)
    for _ in range(2):
        x, y = int(rng.integers(0, 16)), int(rng.integers(0, 16))
        for k in range(14):
            wput(img, x, y + 1, col(r, 2))
            wput(img, x, y, col(r, 4) if k % 4 else (255, 255, 255, 255))
            x += 1
            y += int(rng.integers(-1, 2))
    return img


def crystal_cross(name, pal, rng):
    """Cluster of faceted crystal spikes on transparent ground (cross model)."""
    r = ramp(pal)
    img = new(16, 16)
    spikes = [(4, 8, 3, -2), (12, 10, 3, 2), (8, 14, 4, 0), (5, 5, 2, -1), (11, 6, 2, 1)]
    for bx, hgt, half, lean in sorted(spikes, key=lambda s: s[1]):
        tip = (bx + lean, 15 - hgt)
        poly = [(bx - half, 15), (bx + half, 15), (bx + half, 15 - hgt * 0.45), tip, (bx - half, 15 - hgt * 0.45)]
        m = poly_mask(16, 16, poly)
        xs = np.arange(16)[None, :]
        ridge = bx + lean * (1 - (np.arange(16)[:, None] - tip[1]) / max(1, hgt))
        idx = np.where(xs < ridge - 0.5, 3, np.where(xs > ridge + 0.5, 1, 4))
        idx = idx + (np.arange(16)[:, None] > 13) * -1
        layer = shade_img(r, np.clip(idx, 0, 4), m)
        img[m] = layer[m]
        put(img, tip[0], tip[1], col(r, 4))
    add_outline(img, col(r, 0))
    return img


def magma_vent(name, pal, rng, phase=0.0):
    """Cracked dark basalt around a glowing magma core; phase (0..1) is the frame of the slow glow pulse."""
    rock = ramp("slag")
    mg = ramp(pal)
    pulse = np.sin(2 * np.pi * phase)
    n = fbm(16, 16, rng, (8, 4, 2))
    img = shade_img(rock, dither(0.5 + n * 1.8 - 0.5, 0.5).clip(0, 3))
    for ang in rng.random(6) * 2 * np.pi:
        x, y = 7.5, 7.5
        for k in range(9):
            x += np.cos(ang) + (rng.random() - 0.5) * 0.8
            y += np.sin(ang) + (rng.random() - 0.5) * 0.8
            hot = k + pulse * 1.5
            c = col(mg, 3) if hot < 3 else col(mg, 2) if hot < 6 else col(mg, 1)
            wput(img, int(round(x)), int(round(y)), c)
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5) + (value_noise(16, 16, rng, 4) - 0.5) * 1.5 - pulse * 0.5
    img[d < 3.6] = col(mg, 2)
    img[d < 2.6] = col(mg, 3)
    img[d < 1.6] = col(mg, 4)
    img[d < 0.8] = mix(col(mg, 4), (255, 255, 255, 255), 0.6)
    ring = (d >= 3.6) & (d < 4.4)
    img[ring] = col(rock, 0)
    return img


STYLES = {
    "ore_stone": ore_stone, "ore_deepslate": ore_deepslate, "ore_deepslate_glow": ore_deepslate_glow,
    "raw_block": raw_block, "outcrop_base": outcrop_base, "outcrop_specks": outcrop_specks,
    "geode_shell": geode_shell, "geode_shell_charged": geode_shell_charged, "crystal_cross": crystal_cross,
    "magma_vent": magma_vent,
}

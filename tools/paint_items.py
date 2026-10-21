"""Material and component item sprite painters: SHAPES[shape](name, palette, rng) -> 16x16 RGBA.

Sprites are built from layered masks. Each layer is shaded from its own ramp with top-left lighting,
later layers sit on top and can cast a dark crease onto the layers below, and a 1px outline in the
darkest tone of the touching part surrounds the silhouette, like vanilla items.
"""

import math

import numpy as np

from paint import disc_mask, ellipse_mask, line_pts, outline_col, poly_mask, ramp, relief

S = 16
WHITE = (255, 255, 255, 255)


class Sprite:
    """A 16x16 sprite assembled from shaded layers; tracks the ramp and shade of every pixel."""

    def __init__(self):
        self.ramps: list[np.ndarray] = []
        self.part = np.full((S, S), -1, dtype=int)
        self.idx = np.zeros((S, S), dtype=int)
        self.white = np.zeros((S, S), dtype=bool)

    def _rid(self, r: np.ndarray) -> int:
        for i, q in enumerate(self.ramps):
            if q is r or np.array_equal(q, r):
                return i
        self.ramps.append(r)
        return len(self.ramps) - 1

    def layer(self, mask: np.ndarray, r: np.ndarray, base: float = 2.0, k: float = 0.7, noise: float = 0.0,
              rng: np.random.Generator | None = None, crease: bool = False, cap: int = 2, lo: int = 0, hi: int = 4) -> np.ndarray:
        """Shade mask with ramp r (relief lighting) and paint it on top; return the int shade indices used.

        Args:
            crease: darken underlying pixels that touch this layer from outside (contact shadow).
            lo, hi: clamp range of shade indices.
        """
        vals = relief(mask, base, k, cap)
        if noise and rng is not None:
            vals = vals + (rng.random((S, S)) - 0.5) * 2 * noise
        idx = np.clip(np.rint(vals).astype(int), lo, hi)
        if crease:
            p = np.pad(mask, 1)
            touch = (p[:-2, 1:-1] | p[1:-1, :-2] | p[2:, 1:-1] | p[1:-1, 2:]) & ~mask & (self.part >= 0)
            self.idx[touch] = np.maximum(self.idx[touch] - 2, 0)
        self.paint(mask, r, idx)
        return idx

    def paint(self, mask: np.ndarray, r: np.ndarray, idx) -> None:
        """Paint explicit shade indices (scalar or array) of ramp r into mask."""
        rid = self._rid(r)
        self.part[mask] = rid
        self.idx[mask] = (np.broadcast_to(idx, (S, S)))[mask] if np.ndim(idx) else idx
        self.white[mask] = False

    def px(self, pts, r: np.ndarray, i: int) -> None:
        """Paint single pixels (x, y) with shade i of ramp r."""
        m = np.zeros((S, S), dtype=bool)
        for x, y in pts:
            if 0 <= x < S and 0 <= y < S:
                m[y, x] = True
        self.paint(m, r, i)

    def shift(self, mask: np.ndarray, d: int) -> None:
        """Add d to the shade index of opaque pixels inside mask (clamped 0..4)."""
        m = mask & (self.part >= 0)
        self.idx[m] = np.clip(self.idx[m] + d, 0, 4)

    def glint(self, pts) -> None:
        """Mark opaque pixels as pure white highlights."""
        for x, y in pts:
            if 0 <= x < S and 0 <= y < S and self.part[y, x] >= 0:
                self.white[y, x] = True

    def opaque(self) -> np.ndarray:
        return self.part >= 0

    def render(self, outline: bool = True) -> np.ndarray:
        """Return the RGBA image with an optional outline in the darkest tone of the adjacent part."""
        img = np.zeros((S, S, 4), dtype=np.uint8)
        for rid, r in enumerate(self.ramps):
            m = self.part == rid
            img[m, :3] = r[np.clip(self.idx[m], 0, 4)]
            img[m, 3] = 255
        img[self.white] = WHITE
        if outline:
            a = self.part >= 0
            p = np.pad(self.part, 1, constant_values=-1)
            for dy, dx in ((-1, 0), (0, -1), (1, 0), (0, 1)):
                nb = p[1 + dy:1 + dy + S, 1 + dx:1 + dx + S]
                hit = (~a) & (nb >= 0) & (img[..., 3] == 0)
                for rid, r in enumerate(self.ramps):
                    m = hit & (nb == rid)
                    img[m] = outline_col(r)
        return img


def grid() -> tuple[np.ndarray, np.ndarray]:
    """Return pixel-centre (x, y) coordinate arrays."""
    yy, xx = np.mgrid[0:S, 0:S].astype(float)
    return xx, yy


def diag() -> tuple[np.ndarray, np.ndarray]:
    """Return (u, v) per pixel: u runs bottom-left to top-right, v across it (0 on the anti-diagonal)."""
    xx, yy = np.mgrid[0:S, 0:S][::-1].astype(float)
    return xx - yy, xx + yy - 15.0


def mask_rows(rows: list[str], ch: str = "#") -> np.ndarray:
    """Return a mask from ASCII rows (missing rows and columns are empty)."""
    m = np.zeros((S, S), dtype=bool)
    for y, row in enumerate(rows[:S]):
        for x, c in enumerate(row[:S]):
            m[y, x] = c in ch
    return m


def lumpy(rng: np.random.Generator, cx: float, cy: float, rx: float, ry: float, wobble: float = 0.18) -> np.ndarray:
    """Return an irregular blob mask around (cx, cy)."""
    xx, yy = grid()
    ang = np.arctan2(yy - cy, xx - cx)
    ph = rng.random(3) * 6.28
    rad = 1.0 + wobble * (np.sin(3 * ang + ph[0]) * 0.6 + np.sin(5 * ang + ph[1]) * 0.4)
    return ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2 <= rad ** 2


def raw_chunk(name, pal, rng):
    """Bumpy raw ore lump made of two fused nodules with a crease and speckled surface."""
    r = ramp(pal)
    s = Sprite()
    s.layer(lumpy(rng, 10.0, 5.6, 3.8, 3.3), r, 1.9, 0.9, 0.4, rng)
    s.layer(lumpy(rng, 6.2, 9.4, 5.0, 4.2), r, 2.1, 0.9, 0.4, rng, crease=True)
    s.layer(lumpy(rng, 11.4, 10.8, 2.8, 2.4), r, 1.9, 0.9, 0.3, rng, crease=True)
    pits = (rng.random((S, S)) < 0.1) & s.opaque()
    s.shift(pits, -1)
    s.px([(3, 8), (4, 7), (9, 3), (11, 9)], r, 4)
    s.glint([(4, 7)])
    return s.render()


def ingot(name, pal, rng):
    """Trapezoid bar seen from the front-left: lit top face, bright leading edge, shaded front and end."""
    r = ramp(pal)
    s = Sprite()
    top = poly_mask(S, S, [(4, 4), (13, 4), (11, 6), (2, 6)])
    front = poly_mask(S, S, [(1, 7), (11, 7), (11, 11), (1, 11)])
    end = poly_mask(S, S, [(12, 6), (14, 4), (14, 8), (12, 11)])
    s.paint(end, r, 1)
    s.paint(top, r, 3)
    s.paint(front, r, 2)
    s.px([(x, 7) for x in range(1, 12)], r, 4)
    s.px([(x, 11) for x in range(1, 12)], r, 1)
    s.px([(x, 4) for x in range(5, 9)] + [(3, 5), (4, 5)], r, 4)
    s.px([(2, 8), (3, 8), (2, 9)], r, 3)
    s.px([(12, 7), (13, 5)], r, 2)
    s.px([(12, 10), (13, 8), (14, 7)], r, 0)
    s.glint([(2, 7), (5, 4)])
    return s.render()


def nugget(name, pal, rng):
    """Small cluster of three rounded nuggets."""
    r = ramp(pal)
    s = Sprite()
    s.layer(ellipse_mask(S, S, 8, 5, 12, 9), r, 2.0, 0.8)
    s.layer(ellipse_mask(S, S, 3, 7, 9, 12), r, 2.2, 0.8, crease=True)
    s.layer(ellipse_mask(S, S, 8, 9, 11, 12), r, 2.0, 0.8, crease=True)
    s.px([(4, 8), (5, 8), (9, 6), (9, 10)], r, 4)
    s.glint([(4, 8)])
    return s.render()


def crushed(name, pal, rng):
    """Mound of angular gravel: metal-bearing stones mixed with grey gangue, dark gaps between stones."""
    r, st = ramp(pal), ramp("stone")
    s = Sprite()
    xx, yy = grid()
    mound = (((xx - 7.5) / 6.6) ** 2 + ((yy - 13.2) / 8.4) ** 2 <= 1.0) & (yy <= 13.6) & (yy >= 4)
    n = 16
    pts = np.stack([2 + rng.random(n) * 12, 5 + rng.random(n) * 8.5], axis=1)
    d = np.stack([np.abs(xx - px) * 1.0 + np.abs(yy - py) * 1.15 for px, py in pts])
    srt = np.sort(d, axis=0)
    owner = np.argmin(d, axis=0)
    metal = rng.random(n) < 0.55
    base = 1.6 + rng.random(n) * 0.9
    for i in range(n):
        m = mound & (owner == i)
        if not m.any():
            continue
        rr = r if metal[i] else st
        s.layer(m, rr, base[i], 0.9, cap=1)
    gap = mound & ((srt[1] - srt[0]) < 0.55)
    s.shift(gap, -2)
    s.glint([(5, 6)])
    return s.render()


def washed(name, pal, rng):
    """Smooth, wet, glossy rounded pile with white specular streaks and water droplets."""
    r, wa = ramp(pal), ramp("water")
    s = Sprite()
    xx, yy = grid()
    big = (((xx - 8.5) / 5.8) ** 2 + ((yy - 12.0) / 7.0) ** 2 <= 1.0) & (yy <= 12.6)
    small = (((xx - 4.0) / 3.4) ** 2 + ((yy - 12.0) / 4.0) ** 2 <= 1.0) & (yy <= 12.6)
    s.layer(small, r, 1.8, 0.9)
    s.layer(big, r, 2.3, 0.9, crease=True)
    rim = big & (yy >= 11.5)
    s.shift(rim, -1)
    s.px([(6, 7), (7, 6), (8, 6), (5, 8), (2, 10)], r, 4)
    s.glint([(7, 6), (6, 7), (2, 10), (10, 8)])
    s.px([(11, 9), (12, 10)], wa, 3)
    s.px([(4, 12), (5, 12)], wa, 3)
    puddle = np.zeros((S, S), dtype=bool)
    puddle[13, 2:14] = True
    s.paint(puddle, wa, 2)
    s.px([(3, 13), (4, 13)], wa, 4)
    return s.render()


def pure(name, pal, rng):
    """Three clean faceted crystal shards standing on a small base, lit facet left, shaded facet right."""
    r = ramp(pal)
    s = Sprite()
    shards = [((2, 13), (3, 6), (5, 4), (7, 13), 4.5),
              ((9, 13), (11, 5), (13, 4), (14, 13), 11.7),
              ((5, 13), (7, 3), (8, 1), (10, 13), 7.8)]
    xx, _ = grid()
    for a, b, c, d, axis in shards:
        m = poly_mask(S, S, [a, b, c, d])
        idx = np.where(xx < axis - 0.3, 3, np.where(xx > axis + 0.3, 1, 2))
        s.paint(m, r, idx)
        s.layer(m & ~np.pad(m, 1)[2:, 1:-1], r, 1.0, 0.0)
    s.px([(5, 4), (8, 1), (13, 4)], r, 4)
    s.glint([(8, 1), (5, 4), (13, 4)])
    s.px([(7, 4), (7, 5), (6, 7), (3, 8), (11, 7)], r, 4)
    return s.render()


def concentrate(name, pal, rng):
    """Stack of six round pellets, each with its own shading and a bright highlight."""
    r = ramp(pal)
    s = Sprite()
    spots = [(4.5, 11.4), (8.0, 11.6), (11.5, 11.4), (6.2, 8.2), (9.8, 8.2), (8.0, 5.0)]
    order = [3, 4, 5, 0, 1, 2]
    for i in order:
        cx, cy = spots[i]
        m = disc_mask(S, S, cx, cy, 1.95)
        s.layer(m, r, 2.1, 0.8, crease=True)
        s.px([(int(cx - 1), int(cy - 1))], r, 4)
    s.glint([(7, 4), (3, 10)])
    return s.render()


def shard(name, pal, rng):
    """Long faceted crystal shard pointing top-right, bright ridge and inner glow."""
    r = ramp(pal)
    s = Sprite()
    u, v = diag()
    w = 4.4 * np.clip(1.0 - np.abs(u - 2.0) / 12.5, 0, 1)
    m = (np.abs(v - 0.5) <= w) & (u > -10.5) & (u < 13.5)
    idx = np.where(v < 0, 3, np.where(v > 1, 1, 4))
    s.paint(m, r, idx)
    s.shift(m & (v > 1.6), -1)
    s.px([(6, 9), (8, 7)], r, 4)
    s.glint([(12, 3), (7, 8)])
    return s.render()


def crystal(name, pal, rng):
    """Cut gem with crown facets, a table and a glowing core."""
    r = ramp(pal)
    s = Sprite()
    oct_pts = [(5, 2), (10, 2), (13, 5), (13, 10), (10, 13), (5, 13), (2, 10), (2, 5)]
    body = poly_mask(S, S, oct_pts)
    xx, yy = grid()
    idx = np.where(yy + xx < 13, 3, np.where(yy + xx > 17, 1, 2))
    s.paint(body, r, idx)
    table = poly_mask(S, S, [(6, 5), (9, 5), (10, 6), (10, 9), (9, 10), (6, 10), (5, 9), (5, 6)])
    s.paint(table, r, 2)
    facet = np.zeros((S, S), bool)
    for a, b in (((5, 2), (6, 5)), ((10, 2), (9, 5)), ((13, 5), (10, 6)), ((13, 10), (10, 9)),
                 ((10, 13), (9, 10)), ((5, 13), (6, 10)), ((2, 10), (5, 9)), ((2, 5), (5, 6))):
        for x, y in line_pts(*a, *b):
            facet[y, x] = True
    s.shift(facet & body, -1)
    rim = table & ~np.pad(table, 1)[2:, 2:]
    s.shift(rim, -1)
    core = disc_mask(S, S, 7.5, 7.5, 1.6)
    s.paint(core, r, 4)
    s.glint([(7, 7), (8, 7), (7, 8), (11, 3)])
    s.px([(6, 3), (7, 3), (3, 6), (4, 4)], r, 4)
    s.px([(12, 10), (11, 11), (10, 12)], r, 0)
    return s.render()


def plate_glow(name, pal, rng):
    """Curved black armour plate with a gold rim, gold rivets and a glowing cyan seam (two-tone aegis)."""
    body, gold, glow = ramp("aegis_body"), ramp("aegis_gold"), ramp("aegis_glow")
    s = Sprite()
    shape = poly_mask(S, S, [(2, 3), (13, 3), (13, 9), (8, 14), (7, 14), (2, 9)])
    inner = poly_mask(S, S, [(3, 4), (12, 4), (12, 9), (8, 13), (7, 13), (3, 9)])
    s.paint(shape, gold, 2)
    s.layer(inner, body, 2.0, 0.6, lo=0, hi=3)
    s.px([(x, 3) for x in range(2, 14)] + [(2, y) for y in range(4, 9)], gold, 3)
    s.px([(3, 3), (4, 3)], gold, 4)
    seam = np.zeros((S, S), dtype=bool)
    seam[7, 4:12] = True
    seam[5:12, 7] = True
    s.paint(seam, glow, 3)
    s.px([(7, 7)], glow, 4)
    s.glint([(7, 7)])
    s.px([(4, 5), (11, 5), (4, 9), (11, 9)], gold, 3)
    return s.render()


def slag(name, pal, rng):
    """Porous dark slag lump with rimmed pores."""
    r = ramp(pal)
    s = Sprite()
    m = lumpy(rng, 7.8, 8.6, 5.8, 4.8, 0.22)
    s.layer(m, r, 2.0, 0.8, 0.3, rng)
    pores = [(5, 7), (9, 6), (7, 10), (11, 9), (4, 10), (10, 11), (8, 8)]
    for x, y in pores:
        if m[y, x]:
            s.px([(x, y)], r, 0)
            if m[y + 1, x + 1]:
                s.px([(x + 1, y + 1)], r, 3)
    s.px([(4, 5), (5, 5)], r, 4)
    return s.render()


def gear(name, pal, rng):
    """Eight-tooth gear with a raised hub and centre hole."""
    r = ramp(pal)
    s = Sprite()
    xx, yy = grid()
    dx, dy = xx - 7.5, yy - 7.5
    d = np.hypot(dx, dy)
    ang = np.arctan2(dy, dx)
    tooth = np.cos(8 * (ang + 0.2)) > 0.15
    m = (d <= np.where(tooth, 7.3, 5.4)) & (d >= 1.5)
    s.layer(m, r, 2.0, 0.6)
    hub = (d <= 3.2) & (d >= 1.5)
    s.layer(hub, r, 2.6, 0.8, crease=True)
    s.px([(5, 4), (4, 5)], r, 4)
    s.glint([(5, 5)])
    return s.render()


def spring(name, pal, rng):
    """Flat spiral mainspring: steel band wound in a clock-spring spiral with an outer hooked tail."""
    r = ramp(pal)
    s = Sprite()
    band = np.zeros((S, S), bool)
    shade = np.zeros((S, S), int)
    cx, cy = 7.5, 7.5
    for t in np.linspace(0.0, 1.0, 1400):
        ang = t * 2 * math.pi * 2.4
        rad = 0.9 + 5.7 * t
        x, y = int(round(cx + rad * math.cos(ang))), int(round(cy + rad * math.sin(ang)))
        if 0 <= x < S and 0 <= y < S:
            band[y, x] = True
            shade[y, x] = 3 if math.sin(ang + 0.8) < 0 else 2
    s.paint(band, r, shade)
    tail = np.zeros((S, S), bool)
    for x, y in [(13, 9), (14, 10), (14, 11), (13, 12)]:
        tail[y, x] = True
    s.paint(tail & ~band, r, 2)
    s.px([(7, 7), (8, 7)], r, 1)
    s.glint([(5, 4), (3, 7)])
    return s.render()


def coil(name, pal, rng):
    """Copper wire winding on an iron core with iron end cheeks."""
    r, fe = ramp(pal), ramp("iron")
    s = Sprite()
    core = np.zeros((S, S), bool)
    core[7:9, 1:15] = True
    s.layer(core, fe, 2.0, 0.6)
    wind = np.zeros((S, S), bool)
    wind[4:12, 4:12] = True
    xx, yy = grid()
    idx = np.clip(np.rint(3.2 - (yy - 4) * 0.33).astype(int) - (xx.astype(int) % 2), 0, 4)
    s.paint(wind, r, idx)
    s.px([(x, 4) for x in range(4, 12, 2)], r, 4)
    for x0 in (3, 12):
        cheek = np.zeros((S, S), bool)
        cheek[3:13, x0] = True
        s.layer(cheek, fe, 2.0, 0.6)
    s.px([(3, 3), (12, 3)], fe, 4)
    s.glint([(5, 4)])
    return s.render()


def plate(name, pal, rng):
    """Square metal plate in isometric view: bevelled top, visible thickness, rivet holes."""
    r = ramp(pal)
    s = Sprite()
    top = poly_mask(S, S, [(1, 8), (8, 4), (14, 7), (7, 11)])
    side_l = poly_mask(S, S, [(1, 8), (7, 11), (7, 13), (1, 10)])
    side_r = poly_mask(S, S, [(7, 11), (14, 7), (14, 9), (7, 13)])
    s.paint(side_l, r, 1)
    s.paint(side_r, r, 0)
    s.layer(top, r, 2.6, 0.5, lo=2, hi=4)
    s.px([(2, 8), (3, 7), (4, 7), (5, 6), (6, 5), (7, 5)], r, 4)
    s.px([(4, 8), (8, 6), (11, 7), (7, 9)], r, 1)
    s.glint([(5, 6)])
    return s.render()


def paste(name, pal, rng):
    """Squeeze tube of shimmering paste with crimped end, cap and a dab of paste at the nozzle."""
    r, cap = ramp(pal), ramp("rubber")
    s = Sprite()
    u, v = diag()
    tube = (u >= -8) & (u <= 5) & (np.abs(v) <= 2.6 - np.clip((-u - 4) * 0.2, 0, 1))
    crimp = (u >= -11) & (u < -8) & (np.abs(v) <= 3.4)
    nozzle = (u > 5) & (u <= 8) & (np.abs(v) <= 1.3)
    s.layer(crimp, r, 1.6, 0.6)
    s.layer(tube, r, 2.4, 0.8, crease=True)
    s.layer(nozzle, cap, 2.2, 0.6, crease=True)
    blob = disc_mask(S, S, 12.6, 2.6, 1.5)
    s.layer(blob, r, 3.2, 0.6)
    s.shift(crimp & ((u.astype(int) % 2) == 0), -1)
    band = tube & (u >= -3) & (u <= -1)
    s.shift(band, -1)
    s.glint([(6, 7), (12, 2), (4, 9)])
    return s.render()


def rope(name, pal, rng):
    """Coil of twisted hemp rope with a loose tail."""
    r = ramp(pal)
    s = Sprite()
    xx, yy = grid()
    dx, dy = (xx - 7.5) / 1.0, (yy - 8.0) / 0.78
    d = np.hypot(dx, dy)
    ang = np.arctan2(dy, dx)
    ring = (d >= 2.6) & (d <= 6.6)
    tw = (np.floor(ang / (2 * math.pi) * 18 + d * 0.9) % 2).astype(int)
    idx = 2 + tw + np.where(dy < 0, 0, -1)
    s.paint(ring, r, np.clip(idx, 1, 4))
    groove = ring & (np.abs(d - 4.6) < 0.45)
    s.shift(groove, -1)
    tail = np.zeros((S, S), bool)
    for x, y in [(12, 11), (13, 12), (13, 13), (14, 14)]:
        tail[y, x] = True
    s.layer(tail, r, 2.6, 0.4)
    s.px([(5, 3), (6, 3)], r, 4)
    return s.render()


def _spool(s: Sprite, flange: np.ndarray, wire_rows, frost: bool = False) -> None:
    """Draw a spool: back flange, wound barrel (wire_rows(mask) paints it), front flange with axle hole."""
    back = ellipse_mask(S, S, 2, 1, 5, 14)
    s.layer(back, flange, 1.6, 0.6)
    barrel = np.zeros((S, S), bool)
    barrel[3:13, 4:12] = True
    wire_rows(barrel)
    front = ellipse_mask(S, S, 10, 1, 13, 14)
    s.layer(front, flange, 2.3, 0.8, crease=True)
    s.px([(11, 7), (12, 7), (11, 8)], flange, 0)
    s.px([(11, 2), (10, 4)], flange, 4)


def spool(name, pal, rng):
    """Wooden spool wound with wire in the item palette (copper or black insulated cable)."""
    w, r = ramp("wood"), ramp(pal)
    s = Sprite()
    xx, yy = grid()

    def wire(m):
        idx = np.where(yy.astype(int) % 2 == 0, 3, 2) - np.where(yy > 9, 1, 0)
        s.paint(m, r, np.clip(idx, 0, 4))
        s.px([(x, 3) for x in range(5, 11, 2)], r, 4)

    _spool(s, w, wire)
    s.glint([(6, 3)])
    return s.render()


def spool_hv(name, pal, rng):
    """Steel spool with thick black HV cable banded in red."""
    f, rub, red = ramp(pal), ramp("rubber"), ramp("red_paint")
    s = Sprite()
    xx, yy = grid()

    def wire(m):
        idx = np.where(yy.astype(int) % 3 == 0, 1, 3) - np.where(yy > 9, 1, 0)
        s.paint(m, rub, np.clip(idx, 0, 4))
        band = m & ((xx.astype(int) == 6) | (xx.astype(int) == 9))
        s.paint(band, red, np.clip(idx + 0, 1, 4))

    _spool(s, f, wire)
    s.px([(5, 4), (7, 4)], rub, 4)
    return s.render()


def spool_frost(name, pal, rng):
    """Frost-covered spool wound with violet superconductor cable, ice crystals on top."""
    f, cab = ramp(pal), ramp("lattice")
    s = Sprite()
    xx, yy = grid()

    def wire(m):
        idx = np.where(yy.astype(int) % 2 == 0, 3, 2) - np.where(yy > 9, 1, 0)
        s.paint(m, cab, np.clip(idx, 0, 4))

    _spool(s, f, wire)
    s.glint([(5, 3), (8, 4), (11, 3), (3, 2), (7, 11), (12, 12)])
    s.px([(6, 3), (9, 3)], f, 4)
    return s.render()


SHAPES = {
    "raw_chunk": raw_chunk, "ingot": ingot, "nugget": nugget, "crushed": crushed, "washed": washed, "pure": pure,
    "concentrate": concentrate, "shard": shard, "crystal": crystal, "plate_glow": plate_glow, "slag": slag,
    "gear": gear, "spring": spring, "coil": coil, "plate": plate, "paste": paste, "rope": rope, "spool": spool,
    "spool_hv": spool_hv, "spool_frost": spool_frost,
}

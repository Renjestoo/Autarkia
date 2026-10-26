"""Tone-canvas toolkit for the machine block painters (paint_blocks_machine).

A Face holds, per pixel of a 16x16 texture, a ramp key, a continuous tone (0 darkest, 1 lightest) and an alpha; shapes
are lit from the top left by raising or sinking masks, and render() maps the tones onto every shade of muted,
hue-shifted ramps without ordered dithering.
"""

import numpy as np
from paint import fbm, hex_rgb, new, ramp_n, shade_n

YY, XX = np.mgrid[0:16, 0:16]
FULL = np.ones((16, 16), dtype=bool)
SAT = {"brass": 0.9, "copper": 0.8, "steel": 0.9}


def box(x0: int, y0: int, x1: int, y1: int) -> np.ndarray:
    """Return the mask of the inclusive pixel rectangle (x0, y0)-(x1, y1)."""
    return (XX >= x0) & (XX <= x1) & (YY >= y0) & (YY <= y1)


def dist(cx: float, cy: float) -> np.ndarray:
    """Return every pixel's distance from (cx, cy)."""
    return np.hypot(XX - cx, YY - cy)


def toplight(cx: float, cy: float) -> np.ndarray:
    """Return -1..1 per pixel: how much the direction from (cx, cy) faces the top-left light."""
    return np.cos(np.arctan2(YY - cy, XX - cx) + 0.75 * np.pi)


def shifted(mask: np.ndarray, dx: int, dy: int) -> np.ndarray:
    """Return mask moved by (dx, dy) without wrap-around."""
    out = np.zeros_like(mask)
    out[max(dy, 0):16 + min(dy, 0), max(dx, 0):16 + min(dx, 0)] = mask[max(-dy, 0):16 + min(-dy, 0),
                                                                        max(-dx, 0):16 + min(-dx, 0)]
    return out


def sides(mask: np.ndarray) -> tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    """Return the top, left, bottom and right edge pixels of mask; outside the canvas counts as outside."""
    return (mask & ~shifted(mask, 0, 1), mask & ~shifted(mask, 1, 0), mask & ~shifted(mask, 0, -1),
            mask & ~shifted(mask, -1, 0))


def muted(name: str, n: int = 10, shift: float = 0.3, sat: float = 0.8) -> np.ndarray:
    """Return ramp_n(name, n, shift) with each shade's saturation scaled by sat (1 keeps it)."""
    r = ramp_n(name, n, shift).astype(np.float64)
    grey = (r @ np.array([0.299, 0.587, 0.114]))[:, None]
    return np.clip(np.rint(grey + (r - grey) * sat), 0, 255).astype(np.int64)


def metal(pal: str, n: int = 12) -> np.ndarray:
    """Return the muted n-shade ramp of a metal palette."""
    return muted(pal, n, 0.3, SAT.get(pal, 0.85))


def stops(n: int, *codes: str) -> np.ndarray:
    """Return an n-shade ramp interpolated through '#RRGGBB' stops given dark to light."""
    c = np.array([hex_rgb(h) for h in codes], dtype=np.float64)
    t, k = np.linspace(0, 1, n), np.linspace(0, 1, len(codes))
    return np.clip(np.rint(np.stack([np.interp(t, k, c[:, i]) for i in range(3)], axis=1)), 0, 255).astype(np.int64)


def blend(a: np.ndarray, b: np.ndarray, t: float) -> np.ndarray:
    """Return ramp a moved towards ramp b (same length) by t."""
    return np.clip(np.rint(a * (1 - t) + b * t), 0, 255).astype(np.int64)


def grain(rng: np.random.Generator, amp: float = 0.07, fine: float = 0.03, cells=(4, 2)) -> np.ndarray:
    """Return a tileable tone offset: soft fbm blotches within +-amp plus per-pixel jitter within +-fine."""
    return (fbm(16, 16, rng, cells, (0.6, 0.4)) - 0.5) * 2 * amp + (rng.random((16, 16)) - 0.5) * 2 * fine


def brushed(rng: np.random.Generator, amp: float = 0.06, streak: float = 0.1, fine: float = 0.015) -> np.ndarray:
    """Return a tileable tone offset like rolled metal: broad blotches, short horizontal streaks and faint jitter."""
    knots = rng.random((16, 5)) - 0.5
    knots[:, -1] = knots[:, 0]
    rows = np.stack([np.interp(np.arange(16), np.linspace(0, 16, 5), k) for k in knots]) * 2 * streak
    return grain(rng, amp, fine, (8, 4)) + rows


class Face:
    """A 16x16 tone canvas: each pixel holds a ramp key, a tone (0 darkest, 1 lightest) and an alpha."""

    def __init__(self, rng: np.random.Generator, ramps: dict[str, np.ndarray], base: str, clear: bool = False):
        self.rng, self.ramps = rng, ramps
        self.key = np.full((16, 16), base, dtype=object)
        self.tone = np.full((16, 16), 0.5)
        self.alpha = np.full((16, 16), 0 if clear else 255)

    def fill(self, mask: np.ndarray, key: str, tone, alpha=255) -> None:
        """Give the masked pixels ramp key, tone and alpha (numbers or 16x16 fields)."""
        self.key[mask] = key
        self.tone[mask] = (np.zeros((16, 16)) + tone)[mask]
        self.alpha[mask] = (np.zeros((16, 16), dtype=int) + alpha)[mask]

    def add(self, mask: np.ndarray, delta) -> None:
        """Add delta (a number or a 16x16 field) to the masked tones."""
        self.tone[mask] += (np.zeros((16, 16)) + delta)[mask]

    def raised(self, mask: np.ndarray, light: float = 0.14, dark: float = 0.18) -> None:
        """Light the top and left edges of mask and shade its bottom and right edges, like a raised part."""
        t, l, b, r = sides(mask)
        lit = t | (l & ~b)
        self.add(lit, light)
        self.add((b | r) & ~lit, -dark)

    def sunk(self, mask: np.ndarray, dark: float = 0.2, light: float = 0.08) -> None:
        """Shade the top and left edges of mask (deeper under the top edge) and light its bottom and right edges."""
        t, l, b, r = sides(mask)
        shade = t | (l & ~b)
        self.add(shade, -dark)
        self.add(shifted(t, 0, 1) & mask & ~shade, -dark * 0.45)
        self.add((b | r) & ~shade, light)

    def rim(self, hole: np.ndarray, light: float = 0.1, dark: float = 0.06) -> None:
        """Bevel the surface around a recess: the lip below and right of hole lightens, the edge above and left darkens."""
        out = ~hole
        below, right = shifted(hole, 0, 1) & out, shifted(hole, 1, 0) & out
        self.add(below, light)
        self.add(right & ~below, light * 0.6)
        above, left = shifted(hole, 0, -1) & out, shifted(hole, -1, 0) & out
        self.add(above & ~below, -dark)
        self.add(left & ~above & ~below & ~right, -dark * 0.6)

    def recess(self, hole: np.ndarray, key: str, tone: float, dark: float = 0.14) -> None:
        """Cut a recess: fill hole with a dim key tone brightening towards its floor, shade its walls, bevel its surround."""
        rows = np.flatnonzero(hole.any(axis=1))
        depth = (YY - rows[0]) / max(1, rows[-1] - rows[0])
        self.fill(hole, key, tone + depth * 0.08 + grain(self.rng, 0.04, 0.02))
        self.sunk(hole, dark, dark * 0.5)
        self.rim(hole)

    def shadow(self, mask: np.ndarray, onto: np.ndarray, delta: float = 0.16) -> None:
        """Darken the pixels of onto just below and right of mask, the shadow mask casts."""
        s = (shifted(mask, 1, 1) | shifted(mask, 0, 1) | shifted(mask, 1, 0)) & ~mask & onto
        self.add(s, -delta)

    def bolt(self, x: int, y: int) -> None:
        """Raise a 2x2 bolt head at (x, y) with a shadow below and right."""
        self.shadow(box(x, y, x + 1, y + 1), FULL, 0.1)
        for dx, dy, d in ((0, 0, 0.3), (1, 0, 0.12), (0, 1, 0.06), (1, 1, -0.1)):
            self.add(box(x + dx, y + dy, x + dx, y + dy), d)

    def render(self) -> np.ndarray:
        """Return the RGBA image: each ramp key's tones mapped onto all of its shades."""
        out = new(16, 16)
        t = np.clip(self.tone, 0, 1)
        for k, r in self.ramps.items():
            m = self.key == k
            if m.any():
                out[m] = shade_n(t, r)[m]
        out[..., 3] = self.alpha
        out[self.alpha == 0] = 0
        return out


def casing(f: Face, key: str, bolts=((1, 1), (13, 1), (1, 13), (13, 13))) -> None:
    """Paint a whole-face casing plate: soft grain, a top-lit gradient, an in-hue rim and bolt heads."""
    f.fill(FULL, key, 0.52 + brushed(f.rng) + np.linspace(0.05, -0.05, 16)[:, None])
    f.raised(FULL, 0.07, 0.11)
    for x, y in bolts:
        f.bolt(x, y)


def cylinder(f: Face, mask: np.ndarray, key: str, x0: int, x1: int, mid: float = 0.55, k: float = 0.32) -> None:
    """Fill mask with a vertical cylinder lit from the left: tone falls from mid + k/2 at x0 to mid - k/2 at x1."""
    t = mid + k * (0.5 - (XX - x0) / max(1, x1 - x0))
    f.fill(mask, key, t + grain(f.rng, 0.03, 0.02))

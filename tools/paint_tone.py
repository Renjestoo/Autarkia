"""Tone-map shading helpers for the metal block painters (paint_blocks_metal, paint_logistics_t5).

A painter builds a float tone map (0 darkest, 1 lightest) from soft noise, gradients and in-hue edges, then maps it
onto a muted hue-shifted ramp with shade_n, so surfaces read as vanilla-style metal: soft clusters, no ordered dithering.
"""

import numpy as np

from paint import fbm, ramp_n, shade_n, value_noise

LUMA = np.array([0.299, 0.587, 0.114])
YY, XX = np.mgrid[0:16, 0:16].astype(np.float64)


def mramp(pal: str, n: int = 10, lo: float = 0.0, hi: float = 1.0, shift: float = 0.3, mute: float = 0.0) -> np.ndarray:
    """Return an n-shade hue-shifted ramp spanning tones lo..hi (0..1) of a palette or '#RRGGBB', pulled towards grey by mute."""
    fine = ramp_n(pal, 65, shift).astype(np.float64)
    pos = np.linspace(lo, hi, n) * 64
    rgb = np.stack([np.interp(pos, np.arange(65), fine[:, c]) for c in range(3)], axis=1)
    grey = (rgb @ LUMA)[:, None]
    return np.clip(np.rint(rgb + (grey - rgb) * mute), 0, 255).astype(np.int64)


METAL = {
    "tin": ("tin", 0.15, 1.0, 0.25, 0.0),
    "bronze": ("bronze", 0.04, 0.86, 0.3, 0.22),
    "zinc": ("zinc", 0.02, 0.86, 0.45, 0.15),
    "brass": ("brass", 0.06, 0.94, 0.3, 0.14),
    "steel": ("steel", 0.08, 0.95, 0.25, 0.0),
    "vantite": ("vantite", 0.04, 0.8, 0.3, 0.18),
    "vantite_steel": ("vantite_steel", 0.04, 0.92, 0.3, 0.12),
    "lattice": ("lattice", 0.04, 0.9, 0.3, 0.18),
    "foundation": ("foundation", 0.1, 0.9, 0.2, 0.0),
    "wood": ("wood", 0.0, 1.0, 0.25, 0.1),
    "aegis": ("#3C3C3C", 0.0, 0.62, 0.35, 0.0),
}
"""Body ramp per palette: (palette or hex, lo, hi, hue shift, mute) for mramp."""


def metal(pal: str, n: int = 14) -> np.ndarray:
    """Return the muted n-shade body ramp of a material palette."""
    src, lo, hi, shift, mute = METAL[pal]
    return mramp(src, n, lo, hi, shift, mute)


def gold() -> np.ndarray:
    """Return the muted aegis gold trim ramp."""
    return mramp("aegis_gold", 10, 0.04, 0.82, 0.3, 0.3)


def cyan(n: int = 6) -> np.ndarray:
    """Return the aegis cyan glow ramp, short of white."""
    return mramp("aegis_glow", n, 0.1, 0.86, 0.1, 0.3)


def soft(rng: np.random.Generator, amp: float, cells=(8, 4, 2), weights=(0.5, 0.3, 0.2)) -> np.ndarray:
    """Return tileable soft cluster noise in [-amp, amp]."""
    return (fbm(16, 16, rng, cells, weights) - 0.5) * 2 * amp


def grain(rng: np.random.Generator, amp: float) -> np.ndarray:
    """Return per-pixel jitter in [-amp, amp] that breaks up flat runs without forming a pattern."""
    return (rng.random((16, 16)) - 0.5) * 2 * amp


def aniso(rng: np.random.Generator, cx: int, cy: int) -> np.ndarray:
    """Return tileable value noise in [0, 1] with feature size cx across and cy down (the larger a multiple of the smaller)."""
    if cx >= cy:
        k = cx // cy
        return value_noise(16, 16 * k, rng, cx)[::k]
    k = cy // cx
    return value_noise(16 * k, 16, rng, cy)[:, ::k]


def brushed(rng: np.random.Generator, amp: float, vertical: bool = False) -> np.ndarray:
    """Return brushed-metal streaks in [-amp, amp]: long along the grain, a pixel or two across it."""
    lines = rng.random(16)
    if vertical:
        n = 0.5 * aniso(rng, 1, 8) + 0.2 * aniso(rng, 2, 16) + 0.3 * lines[None, :]
    else:
        n = 0.5 * aniso(rng, 8, 1) + 0.2 * aniso(rng, 16, 2) + 0.3 * lines[:, None]
    return (n - 0.5) * 2 * amp


def alloy(rng: np.random.Generator, base: float = 0.5, vertical: bool = False) -> np.ndarray:
    """Return a lattice-alloy plate tone map: brushed grain, soft clusters, per-pixel grain and a diagonal sheen band."""
    s = XX + YY
    return (base + brushed(rng, 0.1, vertical) + soft(rng, 0.06, (4, 2), (0.6, 0.4)) + grain(rng, 0.035)
            + 0.08 * np.cos(2 * np.pi * (s - 6) / 20) - (s - 15) * 0.008)


def plating(rng: np.random.Generator, base: float = 0.7) -> np.ndarray:
    """Return a netherite-like dark plating tone map: soft mottled clusters darkening towards the bottom right."""
    return base + soft(rng, 0.14, (4, 2), (0.6, 0.4)) + grain(rng, 0.04) - (XX + YY - 15) * 0.008


def rim(lit: float = 0.08, shade: float = -0.06, top: float = -0.14, bottom: float = -0.28, x0: int = 0, y0: int = 0,
        x1: int = 15, y1: int = 15) -> np.ndarray:
    """Return a tone offset for a soft in-hue block edge: darker outer ring (bottom and right most), lit inner top-left row."""
    o = np.zeros((16, 16))
    o[y0 + 1, x0 + 1:x1] = lit
    o[y0 + 1:y1, x0 + 1] = lit
    o[y1 - 1, x0 + 1:x1] = shade
    o[y0 + 1:y1, x1 - 1] = shade
    o[y0, x0:x1 + 1] = top
    o[y0:y1 + 1, x0] = top
    o[y1, x0:x1 + 1] = bottom
    o[y0:y1 + 1, x1] = bottom
    return o


def recess(t: np.ndarray, x0: int, y0: int, x1: int, y1: int, depth: float = 0.1, wall: float = 0.12) -> None:
    """Sink a rectangle into a tone map: shadowed top and left inner wall, lit bottom and right lip."""
    t[y0:y1 + 1, x0:x1 + 1] -= depth
    t[y0, x0:x1 + 1] -= wall
    t[y0:y1 + 1, x0] -= wall
    t[y1, x0 + 1:x1 + 1] += wall * 0.7
    t[y0 + 1:y1 + 1, x1] += wall * 0.7


def stud(t: np.ndarray, x: int, y: int, h: float = 0.3) -> None:
    """Raise a 2x2 domed rivet head with its top-left pixel at (x, y), lit top-left with a soft shadow below-right."""
    t[y, x] += h
    t[y, x + 1] += h * 0.35
    t[y + 1, x] += h * 0.2
    t[y + 1, x + 1] -= h * 0.4
    if y + 2 < 16:
        t[y + 2, x + 1] -= h * 0.2
    if x + 2 < 16:
        t[y + 1, x + 2] -= h * 0.2


def groove_h(t: np.ndarray, y: int, x0: int, x1: int, depth: float = 0.28) -> None:
    """Cut a horizontal engraved line: dark groove with a lit lip below."""
    t[y, x0:x1 + 1] -= depth
    if y + 1 < 16:
        t[y + 1, x0:x1 + 1] += depth * 0.4


def groove_v(t: np.ndarray, x: int, y0: int, y1: int, depth: float = 0.28) -> None:
    """Cut a vertical engraved line: dark groove with a lit lip to the right."""
    t[y0:y1 + 1, x] -= depth
    if x + 1 < 16:
        t[y0:y1 + 1, x + 1] += depth * 0.4


def layer(img: np.ndarray, t: np.ndarray, r: np.ndarray, mask: np.ndarray) -> None:
    """Paint tone map t through ramp r into img wherever mask is set."""
    img[mask] = shade_n(np.clip(t, 0, 1), r)[mask]


def tone(t: np.ndarray, r: np.ndarray) -> np.ndarray:
    """Return a tone map shaded through ramp r as an opaque image."""
    return shade_n(np.clip(t, 0, 1), r)

"""Tier 5 power painters for OTHER_TEXTURES: the warm superconductor tube and the frost shell drawn over a cold one.

Same conventions as paint_misc: textures run along U and tile along it; rows are the cross-section, lit from the top.
"""

import numpy as np

from paint import WHITE, col, dither, new, ramp, shade_img, value_noise
from paint_misc import _cyl


def cable_super_warm(name, pal, rng):
    """Warm superconductor (T 0.3 or more): the violet core in a bare silver sheath, wet streaks, no ice."""
    core, sheath = ramp(pal), ramp("nanite")
    img = shade_img(sheath, dither(_cyl(rng, 0.4, 0.85) - 0.5, 0.5))
    xx = np.arange(16)
    pulse = 0.5 + 0.5 * np.cos(xx * np.pi / 4)
    for y, base in ((6, 2.2), (7, 2.9), (8, 2.9), (9, 2.2)):
        for x, p in enumerate(pulse):
            img[y, x] = col(core, int(np.clip(round(base + p * 0.5 - 0.4), 0, 4)))
    rows = np.arange(16)[:, None]
    wet = (rng.random((16, 16)) < 0.10) & (rows >= 11) & (rows <= 13)
    img[wet] = col(sheath, 4)
    img[[0, 15], :] = col(sheath, 0)
    return img


def frost_shell(name, pal, rng):
    """Translucent rime over a cold superconductor: soft white frost (alpha 70 to 200) with bright ice needles."""
    frost = ramp(pal)
    n = value_noise(16, 16, rng, 4)
    img = new(16, 16)
    for y in range(16):
        for x in range(16):
            v = float(n[y, x])
            c = col(frost, int(np.clip(round(3.0 + v * 1.4), 0, 4)), int(70 + 130 * v))
            img[y, x] = c
    needles = rng.random((16, 16)) < 0.10
    img[needles] = (*WHITE[:3], 230)
    return img


OTHER = {"cable_super_warm": cable_super_warm, "frost_shell": frost_shell}

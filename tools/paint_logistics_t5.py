"""Tier-5 logistics block textures that the shared painters do not cover: the curved mag rail.

Every painter has the signature (name, palette, rng) -> 16x16 RGBA array.
"""

import numpy as np

from paint import new
from paint_tone import XX, YY, alloy, layer, metal, mramp, soft

CORNER = (16.0, 16.0)
RAILS = (14.5, 2.5)
TRACK = 8.5
SLEEPERS = (15.0, 45.0, 75.0)


def mag_rail_corner(name, pal, rng):
    """Curved rail (cutout) from the south edge to the east edge: two parallel alloy rails on quarter circles over three
    radial 2px alloy sleepers (spaced like the straight rail's ties), each with a magnet core glowing at the track centre.

    The rails meet the edges at 1 to 2 and 13 to 14 px like the straight mag_rail texture, so curve and straight pieces join;
    as there, each rail's side away from the track centre is lit, and each sleeper's side towards the north-east is lit.
    """
    r, glow = metal(pal), mramp("lattice", 6, 0.5, 0.95, 0.15, 0.1)
    img = new(16, 16)
    dx, dy = CORNER[0] - (XX + 0.5), CORNER[1] - (YY + 0.5)
    d = np.hypot(dx, dy)
    ang = np.degrees(np.arctan2(dy, dx))
    st = alloy(rng, 0.24)
    for a0 in SLEEPERS:
        da = ang - a0
        side = d * np.sin(np.radians(da))
        tie = (d >= 2.0) & (d <= 15.0) & (np.abs(side) < 1.0) & (np.abs(da) < 45.0)
        lit = side > 0
        layer(img, st + np.where(lit, 0.1, -0.1), r, tie)
        layer(img, 0.9 - np.abs(d - TRACK) * 0.18 - (~lit) * 0.18, glow, tie & (np.abs(d - TRACK) <= 2.0))
    rt = alloy(rng, 0.66) + soft(rng, 0.04, (4, 2), (0.6, 0.4))
    for rail in RAILS:
        off = d - rail
        lit = off * np.sign(rail - TRACK) > 0
        layer(img, rt + np.where(lit, 0.18, -0.12), r, np.abs(off) < 1.0)
    return np.ascontiguousarray(img)


STYLES = {"mag_rail_corner": mag_rail_corner}

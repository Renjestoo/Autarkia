"""Tier-5 logistics block textures that the shared painters do not cover: the curved mag rail.

Every painter has the signature (name, palette, rng) -> 16x16 RGBA array.
"""

import math

import numpy as np

from paint import col, new, put, ramp

WHITE = (255, 255, 255, 255)
CORNER = (16.0, 16.0)
RAILS = (14.5, 2.5)
PAD_ANGLES = (22.5, 67.5)


def mag_rail_corner(name, pal, rng):
    """Curved rail (cutout) from the south edge to the east edge: two alloy rails on quarter circles, glowing magnet pads between.

    The rails meet the edges at 1 to 2 and 13 to 14 px like the straight mag_rail texture, so curve and straight pieces join.
    """
    r = ramp(pal)
    img = new(16, 16)
    for y in range(16):
        for x in range(16):
            dx, dy = CORNER[0] - (x + 0.5), CORNER[1] - (y + 0.5)
            d = math.hypot(dx, dy)
            ang = math.degrees(math.atan2(dy, dx))
            for rail in RAILS:
                off = d - rail
                if abs(off) < 1.0:
                    put(img, x, y, col(r, 4 if off < -0.3 else 1 if off > 0.3 else 2))
            for pad in PAD_ANGLES:
                da = abs(ang - pad)
                if 4.0 <= d <= 12.5 and da <= 12.0:
                    edge = d < 4.8 or d > 11.7 or da > 9.5
                    core = abs(d - 8.25) < 1.6 and da <= 4.0
                    put(img, x, y, col(r, 0) if edge else col(r, 4) if core else col(r, 2))
    for pad in PAD_ANGLES:
        a = math.radians(pad)
        gx = int(CORNER[0] - 7.0 * math.cos(a))
        gy = int(CORNER[1] - 7.0 * math.sin(a))
        put(img, gx, gy, WHITE)
    return np.ascontiguousarray(img)


STYLES = {"mag_rail_corner": mag_rail_corner}

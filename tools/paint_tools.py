"""Tool and weapon sprite painters: SHAPES[shape](name, palette, rng) -> 16x16 RGBA.

All tools lie on the anti-diagonal like vanilla: handle at the bottom-left, working end at the top-right.
Handles and blades use diagonal coordinates (u along the handle, v across it, see paint_items.diag);
tool heads are drawn as pixel masks and shaded with top-left lighting.
"""

import numpy as np

from paint import disc_mask, line_pts, ramp
from paint_items import S, Sprite, diag, mask_rows

AEGIS = "aegis"


def _antisym(m: np.ndarray) -> np.ndarray:
    """Return the union of a mask and its mirror across the anti-diagonal (x, y) -> (15 - y, 15 - x)."""
    return m | m[::-1, ::-1].T


def _handle(s: Sprite, u: np.ndarray, v: np.ndarray, u0: float, u1: float, r: np.ndarray | None = None) -> None:
    """Paint a two-pixel diagonal handle from u0 to u1: lit upper-left pixels, shaded lower-right pixels."""
    w = ramp("wood") if r is None else r
    m = (u >= u0) & (u <= u1)
    s.paint(m & (v == 0), w, 2)
    s.paint(m & (v == 1), w, 1)
    s.paint(m & (v == 0) & ((u.astype(int) + 13) % 6 == 2), w, 3)


def _ramps(pal: str) -> tuple[np.ndarray, np.ndarray]:
    """Return (body, trim) ramps; the two-tone aegis palette splits into black body and gold trim."""
    if pal == AEGIS:
        return ramp("aegis_body"), ramp("aegis_gold")
    r = ramp(pal)
    return r, r


PICK = [
    "................",
    "....######......",
    "..##########....",
    ".####....####...",
    ".##.......###...",
    ".#.........#....",
]

AXE = [
    "................",
    "......##........",
    ".....####.......",
    "....#######.....",
    "....########....",
    "....#########...",
    ".....#######....",
    "......###.......",
]

HOE = [
    "................",
    "................",
    "......########..",
    ".....#########..",
    ".....##.........",
    ".....##.........",
    "......#.........",
]

def pickaxe(name, pal, rng):
    """Pickaxe: curved two-pointed head arching over the top-right corner."""
    r, _ = _ramps(pal)
    s = Sprite()
    u, v = diag()
    _handle(s, u, v, -13, 8)
    head = _antisym(mask_rows(PICK))
    s.layer(head, r, 2.3, 0.8, crease=True)
    s.px([(4, 1), (5, 1), (6, 1)], r, 4)
    s.glint([(4, 1)])
    return s.render()


def axe(name, pal, rng):
    """Axe: broad blade on the upper-left of the handle top with a bright honed edge."""
    r, _ = _ramps(pal)
    s = Sprite()
    u, v = diag()
    _handle(s, u, v, -13, 9)
    head = mask_rows(AXE)
    s.layer(head, r, 2.2, 0.8, crease=True)
    s.px([(6, 1), (5, 2), (4, 3), (4, 4), (4, 5), (5, 6)], r, 4)
    s.px([(9, 6), (8, 7), (12, 5)], r, 1)
    s.glint([(6, 1)])
    return s.render()


def shovel(name, pal, rng):
    """Shovel: rounded spade blade at the top-right with a metal collar on the handle."""
    r, _ = _ramps(pal)
    s = Sprite()
    u, v = diag()
    _handle(s, u, v, -13, 3)
    collar = (u >= 1) & (u <= 3.5) & (np.abs(v - 0.5) <= 1.6)
    s.layer(collar, r, 1.6, 0.6, crease=True)
    blade = (((u - 8.0) / 5.6) ** 2 + ((v - 0.5) / 3.8) ** 2 <= 1.0) & (u >= 3.5)
    s.layer(blade, r, 2.3, 0.8, crease=True)
    s.shift(blade & (v < -1.5), 1)
    s.glint([(11, 2)])
    return s.render()


def hoe(name, pal, rng):
    """Hoe: head bar from the handle top to the left, blade turned down at its far end."""
    r, _ = _ramps(pal)
    s = Sprite()
    u, v = diag()
    _handle(s, u, v, -13, 9)
    head = mask_rows(HOE)
    s.layer(head, r, 2.2, 0.8, crease=True)
    s.px([(x, 2) for x in range(6, 13)], r, 4)
    s.glint([(6, 2)])
    return s.render()


def _sword(s: Sprite, u, v, blade_r, guard_r, grip_r, pommel_r, u_tip: float, u_guard: float, guard_w: float,
           ridge: int = 4) -> np.ndarray:
    """Paint a diagonal sword (four-pixel blade, crossguard, grip, pommel); return the blade mask."""
    grip = (u >= u_guard - 6.0) & (u < u_guard - 2.0)
    s.paint(grip & (v == 0), grip_r, 2)
    s.paint(grip & (v == 1), grip_r, 1)
    s.paint(grip & (v == 0) & ((u.astype(int) % 4) == 1), grip_r, 3)
    pommel = (u >= u_guard - 8.6) & (u < u_guard - 5.9) & (np.abs(v - 0.5) <= 1.6)
    s.layer(pommel, pommel_r, 2.4, 0.7, crease=True)
    blade = (u >= u_guard - 0.5) & (u <= u_tip) & (v >= -1) & (v <= 2)
    blade &= ~((u > u_tip - 3.0) & ((v < 0) | (v > 1)))
    idx = np.select([v == -1, v == 0, v == 1], [3, ridge, 2], 1)
    s.paint(blade, blade_r, idx)
    guard = (u >= u_guard - 2.6) & (u <= u_guard - 0.4) & (np.abs(v - 0.5) <= guard_w)
    s.layer(guard, guard_r, 2.2, 0.8, crease=True)
    return blade


def sword(name, pal, rng):
    """Short sword: lit edge and ridge, crossguard, wooden grip, pommel."""
    r, t = _ramps(pal)
    s = Sprite()
    u, v = diag()
    _sword(s, u, v, r, t, ramp("wood"), t, 12.5, -3.0, 4.6)
    s.glint([(12, 2)])
    return s.render()


def sword_long(name, pal, rng):
    """Long blade reaching the top-right corner, slim guard, leather grip."""
    r, t = _ramps(pal)
    s = Sprite()
    u, v = diag()
    _sword(s, u, v, r, t, ramp("leather"), t, 14.5, -4.5, 5.4)
    s.glint([(13, 1), (9, 5)])
    return s.render()


def sword_long_glow(name, pal, rng):
    """Long energy blade: glowing violet crystal (lattice) or black blade with a cyan edge (aegis)."""
    s = Sprite()
    u, v = diag()
    if pal == AEGIS:
        body, gold, glow = ramp("aegis_body"), ramp("aegis_gold"), ramp("aegis_glow")
        blade = _sword(s, u, v, body, gold, ramp("rubber"), gold, 14.5, -4.5, 5.4, ridge=3)
        s.paint(blade & (v == -1), glow, 3)
        s.paint(blade & (u > 11.5), glow, 4)
        s.glint([(13, 1), (8, 6)])
    else:
        r = ramp(pal)
        _sword(s, u, v, r, r, ramp("rubber"), r, 14.5, -4.5, 5.4)
        s.glint([(13, 1), (11, 3), (8, 6), (5, 9)])
    return s.render()


def sledgehammer(name, pal, rng):
    """Heavy block head across the top-right with darker striking faces and a bright band."""
    r, _ = _ramps(pal)
    s = Sprite()
    u, v = diag()
    _handle(s, u, v, -13, 6)
    head = (u >= 5.0) & (u <= 12.0) & (np.abs(v - 0.5) <= 6.5)
    s.layer(head, r, 2.1, 0.8, crease=True)
    faces = head & (np.abs(v - 0.5) >= 5.5)
    s.shift(faces, -1)
    band = head & (np.abs(v - 0.5) <= 1.0)
    s.shift(band, 1)
    s.glint([(6, 2)])
    return s.render()


def baton(name, pal, rng):
    """Shock baton: rubber grip, steel rod, glowing blue tip with sparks."""
    r, acc, rub = ramp(pal), ramp("electric_accent"), ramp("rubber")
    s = Sprite()
    u, v = diag()
    grip = (u >= -13) & (u <= -4)
    s.paint(grip & (v == 0), rub, 3)
    s.paint(grip & (v == 1), rub, 1)
    s.paint(grip & (v == 0) & (u.astype(int) % 4 == 1), rub, 4)
    ring = (u >= -4.5) & (u <= -2.5) & (np.abs(v - 0.5) <= 1.6)
    s.layer(ring, r, 2.4, 0.7, crease=True)
    rod = (u > -2.5) & (u <= 7) & ((v == 0) | (v == 1))
    s.paint(rod & (v == 0), r, 3)
    s.paint(rod & (v == 1), r, 1)
    tip = (u > 7) & (u <= 12.5) & (np.abs(v - 0.5) <= 1.6)
    s.layer(tip, acc, 3.0, 0.7, crease=True, lo=2)
    s.px([(14, 2), (12, 0), (15, 4), (10, 1)], acc, 4)
    s.glint([(12, 3), (13, 2)])
    return s.render()


def sling(name, pal, rng):
    """Wooden Y-sling: forked frame, leather bands and a pouch at the top-right."""
    w, lea = ramp("wood"), ramp("leather")
    s = Sprite()
    u, v = diag()
    _handle(s, u, v, -13, -1)
    prong = np.zeros((S, S), bool)
    for x, y in line_pts(7, 8, 7, 2):
        prong[y, x] = True
        prong[y, x + 1] = True
    prong = _antisym(prong)
    s.layer(prong, w, 2.6, 0.7)
    band = np.zeros((S, S), bool)
    for x, y in line_pts(8, 2, 13, 7):
        band[y, x] = True
    s.paint(band & ~prong, lea, 1)
    pouch = disc_mask(S, S, 11.0, 4.0, 1.5)
    s.layer(pouch, lea, 2.8, 0.7, crease=True)
    s.px([(7, 2), (13, 7)], w, 4)
    s.glint([(10, 3)])
    return s.render()


SHAPES = {
    "pickaxe": pickaxe, "axe": axe, "shovel": shovel, "hoe": hoe, "sword": sword, "sling": sling,
    "sledgehammer": sledgehammer, "baton": baton, "sword_long": sword_long, "sword_long_glow": sword_long_glow,
}

"""Inventory sprite painters for GeckoLib items: SHAPES['icon_<id>'](name, palette, rng) -> 16x16 RGBA.

Every icon lies on the vanilla diagonal (stock or handle bottom-left, muzzle or head top-right). Parts are
laid out in diagonal coordinates a = x - y (along the item, growing toward the top-right) and
b = x + y (across it, growing toward the grip side); b = 15 is the main diagonal.
"""

import numpy as np

from paint import add_outline, col, disc_mask, new, outline_col, over, put, ramp, sprite

YY, XX = np.mgrid[0:16, 0:16]
A, B = XX - YY, XX + YY


def dm(a0: int, a1: int, b0: int, b1: int) -> np.ndarray:
    """Return the mask of pixels with a0 <= x-y <= a1 and b0 <= x+y <= b1."""
    return (A >= a0) & (A <= a1) & (B >= b0) & (B <= b1)


def layer(img: np.ndarray, mask: np.ndarray, pal: str, base: float = 2.0, gloss: int = 1, k: float = 0.6) -> None:
    """Shade a part with top-left relief from palette pal and draw it over img."""
    over(img, sprite(mask & ((XX >= 0) & (XX < 16)), ramp(pal), base=base, k=k, outline=False, gloss=gloss))


def finish(img: np.ndarray, pal: str) -> np.ndarray:
    """Add the 1px dark outline of palette pal around the whole silhouette."""
    add_outline(img, outline_col(ramp(pal)))
    return img


def dot(img: np.ndarray, a: int, b: int, c) -> None:
    """Set the pixel at diagonal coordinates (a, b) when it exists (a + b must be even)."""
    if (a + b) % 2 == 0:
        put(img, (a + b) // 2, (b - a) // 2, c)


def icon_steam_repeater(name, pal, rng):
    """Steel repeater: wooden stock and grip, brass drum and tank, gauge on top."""
    img = new(16, 16)
    layer(img, dm(-12, -7, 14, 17), "wood")
    layer(img, dm(-5, -3, 17, 21), "wood")
    layer(img, dm(3, 8, 17, 18), "brass")
    layer(img, dm(4, 12, 14, 15), pal)
    layer(img, dm(11, 12, 13, 16), pal, base=1.6)
    layer(img, dm(-7, 0, 13, 17), pal, base=2.2)
    layer(img, dm(0, 4, 12, 18), "brass", base=2.2, gloss=2)
    for b in (13, 15, 17):
        dot(img, 2, b + 1, col(ramp("brass"), 0))
    layer(img, dm(-4, -2, 10, 12), "brass")
    dot(img, -3, 11, col(ramp("ceramic"), 4))
    return finish(img, pal)


def icon_electric_drill(name, pal, rng):
    """Pistol drill: steel housing with cyan band, rubber grip, battery pack, fluted bit."""
    img = new(16, 16)
    layer(img, dm(-5, -3, 17, 20), "rubber", base=2.4)
    layer(img, dm(-8, -1, 20, 22), pal, base=2.2)
    layer(img, dm(-6, -3, 21, 21), "electric_accent", base=3.2, gloss=0)
    layer(img, dm(-8, 2, 12, 17), pal, base=2.2)
    layer(img, dm(-1, 0, 12, 17), "electric_accent", base=3.0)
    layer(img, dm(2, 4, 13, 16), pal, base=1.6)
    layer(img, dm(5, 12, 14, 15), "tin", base=2.6)
    for a in range(5, 12, 2):
        dot(img, a, 15 if a % 4 == 1 else 14, col(ramp("tin"), 1))
    return finish(img, pal)


def icon_electric_chainsaw(name, pal, rng):
    """Chainsaw: steel housing, top handle, hazard guard, guide bar with a running chain edge."""
    img = new(16, 16)
    layer(img, dm(-6, -2, 8, 9), pal, base=1.6)
    layer(img, dm(-6, -5, 8, 12), pal, base=1.6)
    layer(img, dm(-3, -2, 8, 12), pal, base=1.6)
    layer(img, dm(-9, -2, 11, 19), pal, base=2.2)
    layer(img, dm(-6, -5, 11, 19), "electric_accent", base=3.0, gloss=0)
    layer(img, dm(-2, -1, 10, 20), "hazard", base=3.0, gloss=0)
    layer(img, dm(0, 11, 14, 16), "steel", base=2.6)
    chain = ramp("steel")
    for a in range(0, 13):
        for b in (13, 17):
            dot(img, a, b, col(chain, 4 if (a // 2) % 2 else 0))
    for b in range(13, 18):
        dot(img, 12, b, col(chain, 4 if b % 4 < 2 else 0))
    return finish(img, pal)


def icon_arc_rifle(name, pal, rng):
    """Arc rifle: steel receiver and stock, cyan capacitor, emitter barrel ringed by copper coils."""
    img = new(16, 16)
    layer(img, dm(-13, -7, 14, 17), pal, base=1.8)
    layer(img, dm(-5, -3, 17, 21), "rubber", base=2.4)
    layer(img, dm(-7, -1, 13, 17), pal, base=2.3)
    layer(img, dm(-5, -2, 11, 12), "electric_accent", base=3.2)
    layer(img, dm(-1, 11, 14, 15), pal, base=2.0)
    for a0 in (1, 4, 7):
        layer(img, dm(a0, a0 + 1, 12, 17), "copper", base=2.4, gloss=0)
        dot(img, a0, 13 if a0 % 2 else 12, col(ramp("electric_accent"), 4))
    layer(img, dm(10, 12, 12, 12), "copper", gloss=0)
    layer(img, dm(10, 12, 17, 17), "copper", gloss=0)
    return finish(img, pal)


def icon_grapple_launcher(name, pal, rng):
    """Grapple launcher: teal tube, brass side reel, leather grip and a pronged steel hook."""
    img = new(16, 16)
    layer(img, dm(-12, -8, 14, 16), "vantite_steel")
    layer(img, dm(-5, -3, 17, 21), "leather")
    layer(img, dm(-9, 5, 13, 17), pal, base=2.2)
    layer(img, dm(-6, 2, 12, 12), pal, base=3.4, gloss=0)
    layer(img, disc_mask(16, 16, 6.5, 10.0, 2.3), "brass", base=2.4, gloss=1)
    put(img, 6, 10, col(ramp("steel"), 1))
    layer(img, dm(5, 7, 12, 18), "vantite_steel", base=2.0)
    layer(img, dm(8, 12, 15, 15), "tin", base=2.8)
    layer(img, dm(8, 10, 12, 12), "tin", base=2.6, gloss=0)
    layer(img, dm(8, 10, 18, 18), "tin", base=2.6, gloss=0)
    return finish(img, pal)


def icon_rail_rifle(name, pal, rng):
    """Rail rifle: vantite-steel receiver and twin rails around a glowing teal core, top scope."""
    img = new(16, 16)
    layer(img, dm(-13, -8, 14, 17), "leather")
    layer(img, dm(-6, -4, 18, 22), "leather")
    layer(img, dm(-8, 0, 13, 18), "vantite_steel", base=2.4)
    layer(img, dm(0, 12, 12, 13), "vantite_steel", base=2.6, gloss=2)
    layer(img, dm(0, 12, 17, 18), "vantite_steel", base=2.2, gloss=0)
    layer(img, dm(0, 11, 15, 15), pal, base=3.6, gloss=3)
    layer(img, dm(-7, -1, 10, 11), "vantite_steel", base=1.8)
    dot(img, -1, 11, col(ramp(pal), 4))
    dot(img, -7, 11, col(ramp(pal), 3))
    return finish(img, "vantite_steel")


def icon_nanite_multitool(name, pal, rng):
    """Lattice multitool: rubber grip, violet shaft with glowing band, crystal-tipped pick head."""
    img = new(16, 16)
    layer(img, dm(-13, -5, 15, 16), "rubber", base=2.4)
    layer(img, dm(-5, 6, 15, 16), pal, base=2.2)
    layer(img, dm(-4, -2, 14, 17), pal, base=2.6)
    layer(img, dm(0, 1, 14, 17), pal, base=3.4, gloss=1)
    layer(img, dm(5, 8, 10, 21), pal, base=2.2)
    layer(img, dm(5, 7, 7, 9), pal, base=3.4, gloss=2)
    layer(img, dm(5, 7, 22, 24), pal, base=3.0, gloss=1)
    return finish(img, pal)


def icon_disassembler_beam(name, pal, rng):
    """Disassembler: violet receiver, magenta cell, four fins around a crystal emitter."""
    img = new(16, 16)
    layer(img, dm(-6, -4, 18, 22), "rubber", base=2.4)
    layer(img, dm(-11, 0, 13, 18), pal, base=2.0)
    layer(img, dm(-8, -4, 11, 12), "#FF5CF0", base=2.8, gloss=1)
    layer(img, dm(1, 6, 12, 12), pal, base=3.0, gloss=0)
    layer(img, dm(1, 6, 19, 19), pal, base=2.4, gloss=0)
    layer(img, dm(2, 3, 9, 22), pal, base=3.0, gloss=1)
    layer(img, disc_mask(16, 16, 11.4, 3.6, 2.4), pal, base=3.4, gloss=2)
    put(img, 11, 4, col(ramp("#FF5CF0"), 3))
    put(img, 12, 3, col(ramp("#FF5CF0"), 4))
    return finish(img, pal)


def icon_aegis_multitool(name, pal, rng):
    """Aegis multitool: black handle with gold bands and cyan seam, gold-pronged head inside an orbit ring."""
    img = new(16, 16)
    gold = "aegis_gold"
    ring = disc_mask(16, 16, 10.5, 5.0, 4.6) & ~disc_mask(16, 16, 10.5, 5.0, 3.4)
    layer(img, ring, gold, base=2.4, gloss=2)
    layer(img, dm(-13, 4, 15, 16), "aegis_body", base=2.0)
    layer(img, dm(-11, -10, 14, 17), gold, gloss=0)
    layer(img, dm(-3, -2, 14, 17), gold, gloss=0)
    layer(img, dm(-9, -4, 14, 14), "aegis_glow", base=3.2, gloss=0)
    layer(img, dm(4, 7, 11, 19), "aegis_body", base=2.4)
    layer(img, dm(5, 6, 8, 10), gold, base=2.6)
    layer(img, dm(5, 6, 20, 22), gold, base=2.2)
    layer(img, dm(8, 9, 14, 16), "aegis_glow", base=3.4, gloss=1)
    return finish(img, "aegis_body")


def icon_singularity_lance(name, pal, rng):
    """Singularity lance: long black-gold shaft, three charged gold rings, black tip with cyan rim."""
    img = new(16, 16)
    gold = "aegis_gold"
    layer(img, dm(-15, 4, 15, 16), "aegis_body", base=2.2)
    layer(img, dm(-15, -13, 14, 17), gold)
    layer(img, dm(-9, -6, 15, 16), "leather")
    for a0, half in ((-4, 3), (-1, 3), (2, 2)):
        layer(img, dm(a0, a0 + 1, 15 - half, 16 + half), gold, base=2.6, gloss=1)
        dot(img, a0, 15 - half + ((a0 + 15 - half) % 2), col(ramp("aegis_glow"), 4))
    layer(img, disc_mask(16, 16, 11.3, 3.7, 3.0), "aegis_glow", base=2.6, gloss=0)
    layer(img, disc_mask(16, 16, 11.3, 3.7, 2.0), "aegis_body", base=1.4, gloss=1)
    return finish(img, "aegis_body")


SHAPES = {f.__name__: f for f in (icon_steam_repeater, icon_electric_drill, icon_electric_chainsaw, icon_arc_rifle,
                                  icon_grapple_launcher, icon_rail_rifle, icon_nanite_multitool, icon_disassembler_beam,
                                  icon_aegis_multitool, icon_singularity_lance)}

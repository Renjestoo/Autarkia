"""Machine and electrical block textures: fronts, lamps, belts, insulators, tubes, docks and beacons.

Every painter has the signature (name, palette, rng) -> 16x16 RGBA array. Painters build a paint_face.Face: a tone canvas
lit from the top left whose continuous tones map onto muted, hue-shifted ramps without ordered dithering, so fronts shade
like vanilla machine faces (in-hue rims, recessed openings with depth, raised parts, soft grain). lightning_arrester,
wall_insulator, capsule_tube, capsule_tube_frame, belt_side and capacitor_bank_front are UV atlases of element models;
their painters name the regions the models sample.
"""

import numpy as np
from paint import fbm, poly_mask, ramp_n
from paint_face import (
    FULL,
    XX,
    YY,
    Face,
    blend,
    box,
    brushed,
    casing,
    cylinder,
    dist,
    grain,
    metal,
    muted,
    shifted,
    sides,
    stops,
    toplight,
)

STRIPES = (2, 5, 9, 12)
GLOW = ("#6B3410", "#A4561C", "#D2832E", "#EDAA45", "#F8CC6E", "#FFE6A6", "#FFF7DE")
SMOKE = ("#1F1A17", "#2C241F", "#3C3129", "#4F4135", "#665443", "#7F6A55")
DARK_GLASS = ("#12161C", "#1A212A", "#242E3A", "#33404F", "#485868", "#687A8C")
RED = ("#3A1411", "#561D17", "#74281E", "#8F3626", "#A84832", "#BE6145", "#D2805F")
OCHRE = ("#3A2E14", "#5A4719", "#7F6421", "#9E7C2C", "#B8933C", "#CCAA55")


def _lamp(pal: str, rng: np.random.Generator, on: bool) -> np.ndarray:
    st = metal(pal)
    glow = stops(len(st), *GLOW)
    f = Face(rng, {"st": st, "lit": blend(st, glow, 0.5), "glass": glow if on else stops(8, *SMOKE)}, "st")
    casing(f, "st", bolts=())
    f.add(FULL, -0.1)
    cells = np.zeros((16, 16), dtype=bool)
    for x, y in ((2, 2), (9, 2), (2, 9), (9, 9)):
        cell = box(x, y, x + 4, y + 4)
        cells |= cell
        d = dist(x + 2, y + 2)
        if on:
            f.fill(cell, "glass", 1.06 - d * 0.2 + grain(rng, 0.04, 0.02))
            t, l, b, r = sides(cell)
            f.add(t | l | b | r, -0.1)
        else:
            f.fill(cell, "glass", 0.36 + np.where(d < 1.6, 0.14, 0.0) + grain(rng, 0.06, 0.03))
            f.sunk(cell, 0.22, 0.06)
            f.add(box(x + 1, y + 1, x + 1, y + 1), 0.34)
            f.add(box(x + 2, y + 1, x + 2, y + 1) | box(x + 1, y + 2, x + 1, y + 2), 0.14)
    cage = ~cells
    f.raised(cage, 0.14, 0.14)
    f.bolt(7, 7)
    if on:
        spill = cage & (shifted(cells, 0, 1) | shifted(cells, 0, -1) | shifted(cells, 1, 0) | shifted(cells, -1, 0))
        f.key[spill] = "lit"
        f.add(spill, 0.08)
    return f.render()


def lamp_off(name, pal, rng):
    """Caged industrial lamp, unlit: four smoky glass cells in a steel cage."""
    return _lamp(pal, rng, False)


def lamp_on(name, pal, rng):
    """Caged industrial lamp, lit: four glowing cells, warm white at their centres, light spilling on the cage."""
    return _lamp(pal, rng, True)


def marker(name, pal, rng):
    """Claim marker sprite for a cross model: three violet crystal shards on a steel stake (transparent ground)."""
    f = Face(rng, {"st": metal("steel"), "cr": muted(pal, 10, 0.25, 1.0)}, "st", clear=True)
    stake = box(7, 9, 8, 15)
    cylinder(f, stake, "st", 7, 8, 0.5, 0.3)
    collar = box(6, 9, 9, 10)
    cylinder(f, collar, "st", 6, 9, 0.56, 0.4)
    f.add(box(6, 10, 9, 10), -0.12)
    foot = box(6, 15, 9, 15)
    cylinder(f, foot, "st", 6, 9, 0.4, 0.3)
    left, right = np.zeros((16, 16), dtype=bool), np.zeros((16, 16), dtype=bool)
    for y in range(3, 9):
        left[y, 3 + (y - 3) // 2:5 + (y - 3) // 2] = True
    for y in range(4, 9):
        right[y, 11 - (y - 4) // 2:13 - (y - 4) // 2] = True
    main = box(6, 2, 9, 8) | box(7, 0, 8, 1)
    for m, mid in ((left, None), (right, None), (main, 7)):
        m = m & ~collar
        lit = sides(m)[1] if mid is None else XX <= mid
        f.fill(m, "cr", np.where(lit, 0.72, 0.5) + (8 - YY) * 0.03 + grain(rng, 0.03, 0.02))
        f.raised(m, 0.1, 0.18)
        f.add(sides(m)[0], 0.16)
    f.add(box(7, 0, 7, 0) | box(6, 3, 6, 4), 0.2)
    f.add(box(8, 5, 8, 7), -0.06)
    return f.render()


def valve(name, pal, rng):
    """Pressure valve particle texture: a muted red spoked handwheel on a flanged brass pipe junction."""
    f = Face(rng, {"br": metal(pal), "red": stops(9, *RED)}, "br")
    casing(f, "br", bolts=())
    pipe = box(5, 0, 10, 15)
    cylinder(f, pipe, "br", 5, 10, 0.55, 0.42)
    for y0 in (0, 13):
        fl = box(4, y0, 11, y0 + 2)
        cylinder(f, fl, "br", 4, 11, 0.6, 0.42)
        f.raised(fl, 0.1, 0.14)
    d, lt = dist(7.5, 7.5), toplight(7.5, 7.5)
    wheel = (d >= 4.3) & (d < 6.3)
    u, v = XX - 7.5, YY - 7.5
    spokes = ((np.abs(u - v) < 0.6) | (np.abs(u + v) < 0.6)) & (d < 4.5) & (d > 1.4)
    hub = d < 2.0
    f.shadow(wheel | spokes | hub, ~(wheel | spokes | hub), 0.22)
    torus = np.where(d >= 5.3, 0.24 * lt, -0.16 * lt)
    f.fill(wheel, "red", 0.5 + torus + grain(rng, 0.03, 0.03))
    f.fill(spokes, "red", 0.5 - 0.08 * (u + v) / 4 + grain(rng, 0.03, 0.02))
    f.fill(hub, "br", 0.6 + 0.2 * lt + grain(rng, 0.03, 0.02))
    f.add(box(7, 7, 7, 7), 0.2)
    return f.render()


def arrester(name, pal, rng):
    """Lightning arrester UV atlas (opaque): copper rod and grounding strap in x 7-8 over a glazed ceramic base.

    The model samples the rod sides from x 7-8, y 2-12 and its tip from y 0-1, the base top from x 5-10, y 5-10 and the
    base sides from x 5-10, y 13-15, so the rod column reads as a strap where it crosses the base faces.
    """
    f = Face(rng, {"cu": metal(pal), "cer": muted("ceramic", 9, 0.3, 0.9)}, "cer")
    f.fill(FULL, "cer", 0.55 + grain(rng, 0.05, 0.03))
    for y in range(0, 13, 3):
        f.add(box(0, y, 15, y), 0.1)
        f.add(box(0, y + 2, 15, y + 2), -0.12)
    top = box(5, 5, 10, 10)
    f.fill(top, "cer", 0.66 + grain(rng, 0.04, 0.02) - (XX + YY - 15) * 0.012)
    f.raised(top, 0.1, 0.14)
    base = box(0, 13, 15, 15)
    f.fill(base, "cer", np.array([0.0] * 13 + [0.72, 0.56, 0.4])[YY] + grain(rng, 0.03, 0.02))
    rod = box(7, 0, 8, 15)
    cylinder(f, rod, "cu", 7, 8, 0.52, 0.26)
    f.add(box(7, 0, 8, 1), 0.2)
    f.add(box(7, 0, 7, 0), 0.12)
    f.add(box(7, 12, 8, 12), -0.12)
    f.add(box(7, 13, 8, 13), 0.12)
    f.add(box(7, 15, 8, 15), -0.12)
    f.shadow(rod & ~base, top, 0.12)
    return f.render()


def capacitor_front(name, pal, rng):
    """Capacitor bank front: brass casing with four smoky glass cell windows at x 2-3, 5-6, 9-10, 12-13, y 3-12.

    The windows sit exactly under the charge stripe overlay elements of the capacitor_bank_c* models.
    """
    f = Face(rng, {"br": metal(pal), "cell": stops(8, *DARK_GLASS)}, "br")
    casing(f, "br", bolts=())
    panel = box(1, 2, 14, 13)
    f.add(panel, -0.12)
    f.sunk(panel, 0.12, 0.1)
    f.rim(panel)
    for x in STRIPES:
        win = box(x, 3, x + 1, 12)
        f.fill(win, "cell", np.where(XX == x, 0.44, 0.28) + (YY - 3) * 0.022 + grain(rng, 0.05, 0.03))
        f.sunk(win, 0.22, 0.06)
        f.add(box(x, 5, x, 7), 0.16)
        f.add(box(x, 5, x, 5), 0.14)
        f.add(box(x, 13, x + 1, 13), 0.1)
        f.add(box(x, 1, x, 1), 0.18)
        f.add(box(x + 1, 1, x + 1, 1), -0.08)
    return f.render()


def charge_stripe(name, pal, rng):
    """Emissive overlay of the four capacitor windows: blue charge glow, brightest down each window's left column."""
    f = Face(rng, {"g": muted(pal, 12, 0.15, 0.9)}, "g", clear=True)
    wave = fbm(16, 16, rng, (8, 4), (0.6, 0.4)) - 0.5
    for x in STRIPES:
        win = box(x, 3, x + 1, 12)
        f.fill(win, "g", np.where(XX == x, 0.84, 0.56) + wave * 0.4 + (rng.random((16, 16)) - 0.5) * 0.1)
        f.add(box(x, 3, x + 1, 3), 0.14)
        f.add(box(x, 11, x + 1, 12), -0.12)
        spark = 4 + int(rng.integers(0, 6))
        f.add(box(x, spark, x, spark), 0.2)
    return f.render()


def press_front(name, pal, rng):
    """Plate press front: brass casing around a deep opening with a steel ram above a die holding a copper sheet."""
    f = Face(rng, {"br": metal(pal), "st": metal("steel"), "cu": metal("copper")}, "br")
    casing(f, "br")
    hole = box(3, 3, 12, 12)
    f.recess(hole, "br", 0.12)
    rod = box(7, 3, 8, 4)
    cylinder(f, rod, "st", 7, 8, 0.5, 0.3)
    ram = box(4, 5, 11, 7)
    f.fill(ram, "st", 0.56 + grain(rng, 0.04, 0.02))
    f.raised(ram, 0.14, 0.18)
    f.shadow(ram, hole & ~ram, 0.06)
    sheet = box(5, 9, 10, 9)
    f.fill(sheet, "cu", 0.64 + grain(rng, 0.04, 0.03))
    f.add(box(5, 9, 5, 9), 0.1)
    die = box(4, 10, 11, 12)
    f.fill(die, "st", 0.44 + grain(rng, 0.04, 0.02))
    f.raised(die, 0.16, 0.12)
    f.add(box(4, 12, 11, 12), -0.06)
    return f.render()


def drawer_front(name, pal, rng):
    """Wire drawer front: a steel drawing die feeding copper wire onto a wound spool, inside a recess of the casing."""
    f = Face(rng, {"br": metal(pal), "st": metal("steel"), "cu": metal("copper")}, "br")
    casing(f, "br")
    hole = box(2, 3, 13, 12)
    f.recess(hole, "br", 0.12)
    die = box(2, 4, 5, 11)
    f.fill(die, "st", 0.52 + grain(rng, 0.04, 0.02))
    f.raised(die, 0.14, 0.18)
    cone = box(3, 6, 5, 9)
    f.add(cone, -0.12)
    f.add(box(4, 7, 5, 8), -0.18)
    f.add(box(3, 9, 5, 9), 0.06)
    wire = box(6, 7, 8, 7)
    f.fill(wire, "cu", 0.72 + grain(rng, 0.03, 0.02))
    f.shadow(wire, hole & ~wire, 0.05)
    for y in (4, 11):
        fl = box(8, y, 13, y)
        f.fill(fl, "st", 0.6 - (XX - 8) * 0.05 + grain(rng, 0.03, 0.02))
    f.add(box(8, 11, 13, 11), -0.08)
    coil = box(9, 5, 12, 10)
    cylinder(f, coil, "cu", 9, 12, 0.54, 0.4)
    f.add(coil & (YY % 2 == 0), -0.05)
    f.add(box(9, 5, 12, 5), -0.08)
    return f.render()


RUBBER = ("#0F0F11", "#18181B", "#222226", "#2D2D32", "#38383E", "#45454B", "#55555B", "#6A6A70")


def _belt_surface(f: Face, rng: np.random.Generator) -> None:
    f.fill(FULL, "rub", 0.38 + grain(rng, 0.07, 0.04))
    for y in (1, 5, 9, 13):
        off = (rng.random() - 0.5) * 0.08 + (rng.random((16, 16)) - 0.5) * 0.08
        f.add(box(1, y, 14, y), 0.36 + off)
        f.add(box(1, y + 1, 14, y + 1), 0.18 + off)
        f.add(box(1, y + 2, 14, y + 2), -0.2)
    f.add(box(0, 0, 0, 15), -0.12)
    f.add(box(15, 0, 15, 15), -0.18)


def belt(name, pal, rng):
    """Rubber belt top: transverse cleats every 4px with worn soft grain (tiles along the belt) and darker edges."""
    f = Face(rng, {"rub": stops(10, *RUBBER)}, "rub")
    _belt_surface(f, rng)
    wear = box(1, 0, 14, 15) & (rng.random((16, 16)) < 0.1)
    f.add(wear, 0.1)
    return f.render()


def belt_side(name, pal, rng):
    """Belt frame side (tiles along the belt): steel channels with axle bolts and the dark rubber belt edge.

    The flat belt shows y 11-15 (rubber edge, channel), the sloped belts y 0-2, the splitter guide rails y 6-10.
    """
    f = Face(rng, {"st": metal(pal), "rub": stops(9, *RUBBER)}, "st")
    profile = np.array([0.0, 0.7, 0.46, 0.4, 0.52, 0.5, 0.5, 0.5, 0.5, 0.48, 0.34, 0.0, 0.7, 0.5, 0.46, 0.3])[YY]
    f.fill(FULL, "st", profile + brushed(rng))
    for y in (0, 11):
        f.fill(box(0, y, 15, y), "rub", 0.46 + grain(rng, 0.08, 0.05))
    for x in (3, 11):
        f.bolt(x, 6)
        f.bolt(x, 13)
        f.add(box(x, 1, x, 1), 0.16)
        f.add(box(x + 1, 2, x + 1, 2), -0.12)
    return f.render()


def splitter_top(name, pal, rng):
    """Belt splitter top: rubber belt under a raised steel wedge deflector whose ridge faces the incoming items."""
    f = Face(rng, {"rub": stops(9, *RUBBER), "st": metal("steel")}, "rub")
    _belt_surface(f, rng)
    wedge = poly_mask(16, 16, [(7.5, 2), (12.5, 13), (2.5, 13)])
    f.shadow(wedge, ~wedge, 0.24)
    f.fill(wedge, "st", np.where(XX <= 7, 0.52, 0.32) + (YY - 2) * 0.01 + brushed(rng, 0.04, 0.04))
    f.add(wedge & (XX == 7), 0.1)
    f.raised(wedge, 0.12, 0.18)
    f.bolt(7, 9)
    return f.render()


def loom_front(name, pal, rng):
    """Pattern loom front: a punch card in a reader slot, warp threads behind a brass heddle beam and a dim amber lamp."""
    f = Face(rng, {"br": metal(pal), "pa": muted("canvas", 9, 0.2, 0.9), "amb": stops(9, *GLOW)}, "br")
    casing(f, "br", bolts=((1, 13), (13, 13)))
    slot = box(2, 4, 13, 5)
    f.recess(slot, "br", 0.1)
    card = box(4, 1, 11, 4)
    f.fill(card, "pa", 0.7 + grain(rng, 0.04, 0.03) - (YY - 1) * 0.05)
    f.raised(card, 0.08, 0.12)
    for x, y in ((5, 2), (6, 2), (9, 2), (5, 3), (9, 3), (10, 3)):
        f.add(box(x, y, x, y), -0.34)
    f.shadow(card, slot & ~card, 0.06)
    panel = box(2, 7, 13, 13)
    f.recess(panel, "br", 0.14)
    warp = panel & (XX % 2 == 1) & (XX < 13) & (YY > 7)
    f.fill(warp, "pa", 0.5 + (YY - 8) * 0.03 + grain(rng, 0.04, 0.03))
    beam = box(2, 9, 13, 10)
    f.shadow(beam, panel & ~beam, 0.1)
    f.fill(beam, "br", np.where(YY == 9, 0.68, 0.48) + brushed(rng, 0.03, 0.03))
    for x in (4, 8, 11):
        f.add(box(x, 9, x, 9), 0.14)
        f.add(box(x, 10, x, 10), -0.1)
    f.fill(box(13, 1, 13, 1), "amb", 0.55)
    f.fill(box(12, 1, 12, 1), "amb", 0.4)
    f.add(box(12, 2, 13, 2), -0.12)
    return f.render()


def loom_top(name, pal, rng):
    """Pattern loom top: a card slot and an inspection window over the warp threads and the woven cloth."""
    f = Face(rng, {"br": metal(pal), "pa": muted("canvas", 9, 0.2, 0.9)}, "br")
    casing(f, "br")
    slot = box(3, 3, 12, 4)
    f.recess(slot, "br", 0.1)
    f.fill(box(5, 4, 10, 4), "pa", 0.5 + grain(rng, 0.03, 0.02))
    frame = box(2, 6, 13, 13)
    f.raised(frame, 0.12, 0.16)
    win = box(3, 7, 12, 12)
    f.recess(win, "br", 0.2)
    threads = win & (XX % 2 == 0) & (YY < 10)
    f.fill(threads, "pa", 0.4 + (YY - 7) * 0.05 + grain(rng, 0.04, 0.03))
    cloth = box(3, 10, 12, 12)
    f.fill(cloth, "pa", 0.48 + (YY % 2) * 0.06 + grain(rng, 0.05, 0.03))
    f.add(box(3, 10, 12, 10), -0.1)
    f.add(box(3, 10, 3, 12), -0.08)
    f.add(box(4, 7, 4, 8) | box(5, 7, 5, 7), 0.16)
    return f.render()


def insulator(name, pal, rng):
    """Wall insulator UV atlas (opaque): ceramic body and shed, copper terminal cap, steel base plate.

    The model samples the cap sides at x 7-8, y 9 and its top at x 7-8, y 7-8; the body sides at x 6-9, y 10-13; the shed
    sides at y 12 and the shed and base tops over x 5-10, y 5-10; the base sides at x 5-10, y 14-15.
    """
    cer = muted("ceramic", 9, 0.3, 0.9)
    f = Face(rng, {"st": metal(pal), "cer": cer, "cu": metal("copper")}, "st")
    casing(f, "st", bolts=((1, 1), (13, 1), (1, 13), (13, 13)))
    shed = box(5, 5, 10, 10)
    f.fill(shed, "cer", 0.6 + grain(rng, 0.04, 0.02))
    f.raised(shed, 0.12, 0.14)
    body = box(6, 6, 9, 9)
    f.fill(body, "cer", 0.7 + grain(rng, 0.03, 0.02))
    f.raised(body, 0.1, 0.12)
    f.shadow(body, shed & ~body, 0.08)
    cap = box(7, 7, 8, 8)
    f.fill(cap, "cu", 0.58)
    f.add(box(7, 7, 7, 7), 0.18)
    f.add(box(8, 8, 8, 8), -0.12)
    cylinder(f, box(7, 9, 8, 9), "cu", 7, 8, 0.55, 0.26)
    sides_ = box(6, 10, 9, 13)
    cylinder(f, sides_, "cer", 6, 9, 0.6, 0.34)
    f.add(box(6, 10, 9, 10), 0.06)
    f.add(box(6, 13, 9, 13), -0.12)
    cylinder(f, box(5, 12, 10, 12), "cer", 5, 10, 0.72, 0.34)
    base = box(5, 14, 10, 15)
    f.fill(base, "st", np.where(YY == 14, 0.64, 0.42) + grain(rng, 0.03, 0.02))
    f.add(box(5, 14, 5, 15), 0.06)
    return f.render()


def transformer_side(name, pal, rng):
    """Transformer side (column texture): vertical cooling fins between bolted top and bottom bands."""
    f = Face(rng, {"st": metal(pal)}, "st")
    fin = np.array([0.18, 0.7, 0.58, 0.42])[XX % 4]
    f.fill(FULL, "st", fin + grain(rng, 0.05, 0.025, (8, 4)))
    f.add(box(0, 2, 15, 2), -0.12)
    f.add(box(0, 3, 15, 3), -0.05)
    f.add(box(0, 13, 15, 13), -0.06)
    for y0 in (0, 14):
        band = box(0, y0, 15, y0 + 1)
        f.fill(band, "st", 0.5 + grain(rng, 0.04, 0.03))
        f.add(box(0, y0, 15, y0), 0.14)
        f.add(box(0, y0 + 1, 15, y0 + 1), -0.08)
        for x in (2, 10):
            f.add(box(x, y0, x, y0), 0.2)
            f.add(box(x + 1, y0 + 1, x + 1, y0 + 1), -0.14)
    return f.render()


def transformer_top(name, pal, rng):
    """Transformer top plate with three ceramic bushings capped by copper terminals."""
    f = Face(rng, {"st": metal(pal), "cer": muted("ceramic", 9, 0.3, 0.9), "cu": metal("copper")}, "st")
    casing(f, "st", bolts=((1, 13), (13, 13)))
    for cx, cy in ((3.5, 3.5), (11.5, 3.5), (7.5, 11.5)):
        d, lt = dist(cx, cy), toplight(cx, cy)
        bush = d < 3.0
        f.shadow(bush, ~bush, 0.22)
        f.fill(bush, "cer", 0.34 + 0.18 * lt + grain(rng, 0.03, 0.02))
        f.add((d >= 2.4) & (lt < 0), -0.08)
        f.add((d >= 1.6) & (d < 2.2), -0.14)
        f.fill(d < 1.6, "cer", 0.5 + 0.14 * lt + grain(rng, 0.02, 0.02))
        term = d < 0.8
        f.fill(term, "cu", 0.56 + 0.22 * lt)
    return f.render()


def fuse_front(name, pal, rng):
    """Fuse box front: a glass window over a ceramic fuse cartridge and a slotted lever with a muted red grip."""
    f = Face(rng, {"st": metal(pal), "gl": stops(8, *DARK_GLASS), "cer": muted("ceramic", 9, 0.3, 0.9),
                   "cu": metal("copper"), "red": stops(9, *RED)}, "st")
    casing(f, "st")
    win = box(3, 3, 6, 12)
    f.recess(win, "gl", 0.3)
    fuse = box(4, 5, 5, 10)
    cylinder(f, fuse, "cer", 4, 5, 0.5, 0.26)
    f.add(box(4, 7, 5, 8), -0.06)
    for y in (4, 11):
        cylinder(f, box(4, y, 5, y), "cu", 4, 5, 0.58, 0.24)
    f.add(box(3, 4, 3, 5), 0.24)
    slot = box(10, 4, 11, 12)
    f.recess(slot, "st", 0.1)
    arm = box(10, 6, 11, 10)
    cylinder(f, arm, "st", 10, 11, 0.56, 0.26)
    f.shadow(arm, slot & ~arm, 0.06)
    pivot = box(9, 10, 12, 11)
    f.fill(pivot, "st", 0.56 + grain(rng, 0.03, 0.02))
    f.raised(pivot, 0.14, 0.16)
    grip = box(9, 3, 12, 5)
    f.shadow(grip, ~grip, 0.18)
    f.fill(grip, "red", np.array([0.0, 0.0, 0.0, 0.72, 0.54, 0.36] + [0.0] * 10)[YY] + grain(rng, 0.03, 0.02))
    f.add(box(9, 3, 9, 5), 0.06)
    return f.render()


def battery_front(name, pal, rng):
    """Battery bank front: a round ceramic gauge in a steel bezel above four zinc cells with copper caps and lamps."""
    f = Face(rng, {"st": metal(pal), "cer": muted("ceramic", 9, 0.3, 0.9), "zn": metal("zinc"), "cu": metal("copper"),
                   "red": stops(9, *RED), "ok": stops(7, "#1E2E14", "#2F4A1C", "#456B26", "#5E8C33", "#7DAA48", "#A7C877")},
             "st")
    casing(f, "st")
    d, lt = dist(7.5, 5.0), toplight(7.5, 5.0)
    bezel, dial = d < 3.8, d < 2.9
    f.shadow(bezel, ~bezel, 0.16)
    f.fill(bezel, "st", 0.55 - 0.2 * lt + grain(rng, 0.03, 0.02))
    f.fill(dial, "cer", 0.42 + 0.1 * lt + grain(rng, 0.03, 0.02))
    f.add(dial & (d > 2.0) & (YY < 5), -0.14)
    for x, y in ((7, 3), (8, 4)):
        f.fill(box(x, y, x, y), "red", 0.4)
    f.fill(box(7, 5, 7, 5), "st", 0.2)
    bay = box(2, 9, 13, 13)
    f.recess(bay, "st", 0.12)
    for k, x in enumerate((3, 6, 9, 12)):
        can = box(x - 1, 10, x, 13) & bay
        cylinder(f, can, "zn", x - 1, x, 0.28, 0.24)
        f.fill(box(x - 1, 10, x, 10), "cu", np.where(XX == x - 1, 0.66, 0.46))
        f.fill(box(x - 1, 8, x - 1, 8), "ok" if k < 3 else "st", 0.78 if k < 3 else 0.28)
        f.add(box(x, 8, x, 8), -0.1)
    return f.render()


def separator_front(name, pal, rng):
    """Magnetic separator front: a horizontal steel drum wound with three copper coils, on bearing blocks."""
    f = Face(rng, {"st": metal(pal), "cu": metal("copper")}, "st")
    casing(f, "st", bolts=())
    hole = box(2, 2, 13, 13)
    f.recess(hole, "st", 0.1)
    drum = box(2, 4, 13, 11)
    shade = np.array([0.0] * 4 + [0.74, 0.68, 0.6, 0.54, 0.47, 0.4, 0.32, 0.24] + [0.0] * 4)[YY]
    f.fill(drum, "st", shade + grain(rng, 0.03, 0.02))
    f.shadow(drum, hole & ~drum, 0.06)
    for x0 in (3, 7, 11):
        coil = box(x0, 4, x0 + 1, 11)
        f.fill(coil, "cu", shade + 0.04 + (YY % 2) * 0.05 + grain(rng, 0.02, 0.02))
        f.add(box(x0 + 1, 4, x0 + 1, 11), -0.05)
    for x0 in (0, 14):
        brg = box(x0, 6, x0 + 1, 9)
        f.fill(brg, "st", 0.6 + grain(rng, 0.03, 0.02))
        f.raised(brg, 0.14, 0.18)
    return f.render()


def electrolyzer_front(name, pal, rng):
    """Electrolyzer front: a glass tank of muted blue electrolyte with two dark electrodes and rising bubbles."""
    f = Face(rng, {"st": metal(pal), "gl": stops(8, *DARK_GLASS), "cu": metal("copper"),
                   "aq": stops(9, "#14283A", "#1B3850", "#244A66", "#2E5D7C", "#3B7290", "#4F88A3", "#6FA4B8", "#9CC4D0")},
             "st")
    casing(f, "st")
    frame = box(2, 2, 13, 13)
    f.raised(frame, 0.12, 0.16)
    tank = box(3, 3, 12, 12)
    f.recess(tank, "gl", 0.28)
    liquid = box(3, 6, 12, 12)
    f.fill(liquid, "aq", 0.62 - (YY - 6) * 0.05 + grain(rng, 0.05, 0.03))
    f.add(box(3, 6, 12, 6), 0.16)
    f.add(box(3, 6, 3, 12), -0.08)
    for x in (5, 10):
        rod = box(x, 3, x, 11)
        f.fill(rod, "st", np.where(YY >= 6, 0.22, 0.34) + grain(rng, 0.02, 0.02))
        f.fill(box(x, 2, x, 2), "cu", 0.66)
    for x, y in ((6, 10), (6, 8), (4, 9), (11, 9), (9, 7), (11, 11), (6, 6), (9, 10)):
        f.add(box(x, y, x, y), 0.3)
    f.add(box(4, 4, 4, 4) | box(5, 3, 5, 3), 0.3)
    return f.render()


def _lens(f: Face, x: int, y: int, key: str) -> None:
    f.shadow(box(x, y, x + 1, y + 1), FULL, 0.14)
    for dx, dy, t in ((0, 0, 0.92), (1, 0, 0.64), (0, 1, 0.56), (1, 1, 0.36)):
        f.fill(box(x + dx, y + dy, x + dx, y + dy), key, t)


def controller_front(name, pal, rng):
    """Warehouse controller front: a dark screen with rack readouts and a fill bar above four domed status lamps."""
    crt = stops(9, "#0D1A1C", "#12262A", "#173438", "#1E4449", "#285A5E", "#3A7A7A", "#5AA39C", "#8CCBC0")
    lamps = {"g": ("#1E3214", "#3D6424", "#6A9A3C", "#A7CB78"), "a": ("#3E2A0E", "#7A5418", "#B98632", "#E3BE78"),
             "r": ("#3A1210", "#70251B", "#A84832", "#D98A70")}
    f = Face(rng, {"st": metal(pal), "crt": crt, **{k: stops(6, *v) for k, v in lamps.items()}}, "st")
    casing(f, "st")
    frame = box(2, 2, 13, 9)
    f.raised(frame, 0.12, 0.16)
    scr = box(3, 3, 12, 8)
    f.recess(scr, "crt", 0.16, 0.08)
    for x0, x1, h in ((4, 5, 0.5), (7, 8, 0.42), (10, 11, 0.56)):
        f.add(box(x0, 4, x1, 4), h)
    f.add(box(4, 6, 11, 7), 0.12)
    f.add(box(4, 6, 8, 7), 0.44)
    f.add(box(4, 6, 8, 6), 0.12)
    f.add(box(3, 3, 3, 3), 0.1)
    strip = box(2, 10, 13, 13)
    f.recess(strip, "st", 0.26)
    for x, k in zip((3, 6, 9, 12), "ggar", strict=True):
        _lens(f, x, 11, k)
    return f.render()


def terminal_front(name, pal, rng):
    """Ledger terminal front: a green phosphor text screen above a recessed keyboard of raised keycaps."""
    ph = stops(9, "#0B140C", "#102012", "#162C18", "#1E3B20", "#2A5128", "#3B6E34", "#5A9448", "#8CBF6E")
    f = Face(rng, {"st": metal(pal), "ph": ph}, "st")
    casing(f, "st", bolts=())
    frame = box(2, 1, 13, 8)
    f.raised(frame, 0.12, 0.16)
    scr = box(3, 2, 12, 7)
    f.recess(scr, "ph", 0.14, 0.06)
    for y, text in ((3, "##.###.##."), (5, "###.#.##..")):
        for i, ch in enumerate(text):
            if ch == "#":
                f.add(box(4 + i, y, 4 + i, y), 0.5 + (rng.random() - 0.5) * 0.1)
    f.add(box(11, 5, 11, 5), 0.74)
    deck = box(2, 10, 13, 14)
    f.recess(deck, "st", 0.22)
    for y, keys in ((11, ((3, 4), (6, 7), (9, 10), (12, 12))), (13, ((3, 3), (5, 10), (12, 12)))):
        for x0, x1 in keys:
            f.fill(box(x0, y, x1, y), "st", 0.68 + grain(rng, 0.03, 0.02))
            f.add(box(x0, y, x0, y), 0.08)
            f.fill(box(x0, y + 1, x1, y + 1), "st", 0.36)
    return f.render()


def tube_glass(name, pal, rng):
    """Capsule tube glass (translucent): a pale tube with edge glints, alpha only where the tube model samples.

    The core samples x 5-10, y 5-10 and the arms extend that cross to the borders, so x 5 and 10 or y 5 and 10 are the
    tube's corner edges and x 6 or y 6 its reflection line; the four corner squares stay clear.
    """
    f = Face(rng, {"gl": muted("glass", 9, 0.2, 0.8)}, "gl", clear=True)
    cross = box(5, 0, 10, 15) | box(0, 5, 15, 10)
    f.fill(cross, "gl", 0.72 + grain(rng, 0.04, 0.02), 64)
    refl = cross & ((XX == 6) | (YY == 6))
    f.fill(refl, "gl", 0.9, 104)
    lit = cross & ((XX == 5) | (YY == 5))
    f.fill(lit, "gl", 0.96, 156)
    dark = cross & ((XX == 10) | (YY == 10)) & ~lit
    f.fill(dark, "gl", 0.55, 140)
    f.fill(box(7, 7, 7, 7) | box(8, 8, 8, 8), "gl", 1.0, 150)
    return f.render()


def tube_frame(name, pal, rng):
    """Capsule tube collar (cutout): a bolted brass ring around a clear centre at x 5-10, y 5-10.

    The arm collar samples its end ring from x 4-11, y 4-11 and its outer faces from the two-pixel border bands.
    """
    f = Face(rng, {"br": metal(pal)}, "br")
    f.fill(FULL, "br", 0.55 + grain(rng, 0.05, 0.03))
    f.raised(FULL, 0.12, 0.16)
    lip = box(2, 2, 13, 13)
    f.add(sides(lip)[0] | sides(lip)[1], -0.1)
    f.add(sides(lip)[2] | sides(lip)[3], 0.08)
    ring = box(4, 4, 11, 11)
    f.raised(ring, 0.14, 0.16)
    for x, y in ((7, 0), (0, 7), (14, 7), (7, 14)):
        f.bolt(x, y)
    f.alpha[box(5, 5, 10, 10)] = 0
    return f.render()


def dock_top(name, pal, rng):
    """Recharge dock top: a pad with two copper contact plates inside a worn ochre and steel diagonal-striped border."""
    f = Face(rng, {"st": metal(pal), "paint": stops(8, *OCHRE), "cu": metal("copper"),
                   "rub": ramp_n("rubber", 9, 0.2)}, "st")
    casing(f, "st", bolts=())
    border = ~box(2, 2, 13, 13)
    paint = border & (((XX + YY) // 3) % 2 == 0) & (rng.random((16, 16)) > 0.04)
    f.key[paint] = "paint"
    f.add(paint, 0.08)
    pad = box(2, 2, 13, 13)
    f.recess(pad, "st", 0.36)
    f.fill(box(7, 4, 8, 11), "rub", 0.4 + grain(rng, 0.04, 0.03))
    for x0 in (4, 9):
        plate = box(x0, 4, x0 + 2, 11)
        f.fill(plate, "cu", 0.56 + grain(rng, 0.04, 0.03))
        f.raised(plate, 0.16, 0.18)
        f.shadow(plate, pad & ~plate, 0.08)
    return f.render()


def dock_side(name, pal, rng):
    """Recharge dock side: a segmented charge gauge window above a socket strip of three copper-pinned cable ports."""
    cy = stops(8, "#0E2A30", "#14404A", "#1C5A66", "#287684", "#3A93A0", "#5AB0B8", "#8ACFCF")
    f = Face(rng, {"st": metal(pal), "gl": stops(8, *DARK_GLASS), "cy": cy, "cu": metal("copper")}, "st")
    casing(f, "st")
    win = box(2, 4, 13, 6)
    f.recess(win, "gl", 0.22)
    for x0 in (3, 6, 9):
        f.fill(box(x0, 5, x0 + 1, 5), "cy", 0.72 + grain(rng, 0.03, 0.03))
        f.add(box(x0, 5, x0, 5), 0.14)
    strip = box(2, 9, 13, 12)
    f.recess(strip, "st", 0.3)
    for x in (4, 7, 10):
        port = box(x - 1, 10, x + 1, 11)
        f.fill(port, "st", 0.5)
        f.raised(port, 0.16, 0.12)
        f.fill(box(x, 10, x, 11), "st", 0.1)
        f.fill(box(x, 11, x, 11), "cu", 0.6)
    return f.render()


def beacon_side(name, pal, rng):
    """Domain beacon side: a bolted steel frame around dark glass with a faceted violet amethyst core."""
    f = Face(rng, {"st": metal(pal), "gl": stops(8, "#14121C", "#1C1A28", "#262338", "#332E4A", "#46405E", "#625A7A"),
                   "cr": muted("lattice", 10, 0.25, 0.85)}, "st")
    casing(f, "st", bolts=((0, 0), (14, 0), (0, 14), (14, 14)))
    win = box(2, 2, 13, 13)
    f.recess(win, "gl", 0.34, 0.18)
    d = dist(7.5, 7.5)
    f.add(win, 0.3 * np.clip(1 - d / 6.5, 0, 1) + grain(rng, 0.07, 0.03))
    f.add(win & (np.abs(XX + YY - 9) < 0.6) & (XX < 6), 0.14)
    gem = (np.abs(XX - 7.5) / 3.0 + np.abs(YY - 7.5) / 5.0) <= 1.0
    f.fill(gem, "cr", np.where(XX <= 7, 0.72, 0.5) + (7.5 - YY) * 0.03 + grain(rng, 0.03, 0.02))
    f.raised(gem, 0.1, 0.16)
    f.add(box(6, 4, 6, 5) | box(7, 3, 7, 3), 0.18)
    f.add(box(4, 3, 4, 3) | box(3, 4, 3, 4), 0.3)
    return f.render()


def beacon_top(name, pal, rng):
    """Domain beacon top: a round emitter lens of violet-tinted glass rings around a glowing core in a steel bezel."""
    f = Face(rng, {"st": metal(pal), "gl": stops(8, "#14121C", "#1C1A28", "#262338", "#332E4A", "#46405E", "#625A7A"),
                   "cr": muted("lattice", 10, 0.2, 0.9)}, "st")
    casing(f, "st")
    d, lt = dist(7.5, 7.5), toplight(7.5, 7.5)
    bezel, glass, core = d < 6.2, d < 4.8, d < 3.2
    f.shadow(bezel, ~bezel, 0.14)
    f.fill(bezel, "st", 0.56 + 0.18 * lt + grain(rng, 0.03, 0.02))
    f.fill(glass, "gl", 0.4 - 0.18 * lt + grain(rng, 0.03, 0.02))
    f.add(glass & (d > 4.0), -0.12)
    f.fill(core, "cr", 1.02 - d * 0.16 + grain(rng, 0.03, 0.02))
    f.add(box(5, 5, 5, 5), 0.2)
    f.key[box(4, 6, 4, 6)] = "gl"
    f.add(box(4, 6, 4, 6), 0.4)
    return f.render()


STYLES = {
    "lamp_off": lamp_off, "lamp_on": lamp_on, "marker": marker, "valve": valve, "arrester": arrester,
    "capacitor_front": capacitor_front, "charge_stripe": charge_stripe, "press_front": press_front,
    "drawer_front": drawer_front, "belt": belt, "belt_side": belt_side, "splitter_top": splitter_top,
    "loom_front": loom_front, "loom_top": loom_top, "insulator": insulator, "transformer_side": transformer_side,
    "transformer_top": transformer_top, "fuse_front": fuse_front, "battery_front": battery_front,
    "separator_front": separator_front, "electrolyzer_front": electrolyzer_front,
    "controller_front": controller_front, "terminal_front": terminal_front, "tube_glass": tube_glass,
    "tube_frame": tube_frame, "dock_top": dock_top, "dock_side": dock_side, "beacon_side": beacon_side,
    "beacon_top": beacon_top,
}

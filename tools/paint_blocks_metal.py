"""Metal and structural block textures: storage blocks, plates, panels, racks, grates and tier-5/6 fittings.

Every painter has the signature (name, palette, rng) -> 16x16 RGBA array.
"""

import numpy as np

from paint import (add_outline, bevel, col, dither, fbm, new, noisy_fill, plate, poly_mask, put, ramp, rect, rivet,
                   shade_img, value_noise)

WHITE = (255, 255, 255, 255)


def body(pal: str) -> np.ndarray:
    """Return the body ramp of a palette (aegis bodies are black with a gold top shade)."""
    return ramp("aegis_body" if pal == "aegis" else pal)


def trim(pal: str) -> np.ndarray:
    """Return the trim ramp of a palette (aegis trim is gold)."""
    return ramp("aegis_gold" if pal == "aegis" else pal)


def glow(pal: str) -> np.ndarray:
    """Return the glow ramp that accents a palette."""
    return ramp({"aegis": "aegis_glow", "vantite": "vantite", "vantite_steel": "vantite"}.get(pal, pal))


def inset(img: np.ndarray, x0: int, y0: int, x1: int, y1: int, r: np.ndarray) -> None:
    """Draw a recessed frame: dark top and left edge, light bottom and right edge."""
    bevel(img, x0, y0, x1, y1, col(r, 1), col(r, 3))


def sheen(img: np.ndarray, r: np.ndarray, rng: np.random.Generator) -> None:
    """Add two short diagonal highlight streaks like polished metal."""
    for x0 in (3 + int(rng.integers(0, 3)), 9 + int(rng.integers(0, 3))):
        for k in range(3):
            put(img, x0 + k, 4 + 3 - k + (x0 > 8) * 5, col(r, 3))


def metal_block(name, pal, rng):
    """Storage block of a metal; layout varies by material (tiles, plates, engraving, seams)."""
    r = body(pal)
    img = plate(16, 16, r, rng, base=1.6 if name.startswith("vantite") else 2.0)
    if name.startswith("tin"):
        for x0, y0 in ((0, 0), (8, 0), (0, 8), (8, 8)):
            bevel(img, x0, y0, x0 + 7, y0 + 7, col(r, 4), col(r, 1))
            put(img, x0 + 1, y0 + 1, WHITE)
    elif name.startswith("bronze"):
        for y0 in (0, 8):
            bevel(img, 0, y0, 15, y0 + 7, col(r, 3), col(r, 0))
            for x in (2, 13):
                rivet(img, x, y0 + 2, r)
                rivet(img, x, y0 + 5, r)
    elif name.startswith("brass"):
        bevel(img, 0, 0, 15, 15, col(r, 4), col(r, 0))
        inset(img, 2, 2, 13, 13, r)
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            put(img, x, y, col(r, 4))
        rect(img, 5, 7, 10, 8, col(r, 3))
        rect(img, 7, 5, 8, 10, col(r, 3))
        put(img, 7, 7, col(r, 4))
    elif name.startswith("vantite"):
        g = ramp("vantite")
        bevel(img, 0, 0, 15, 15, col(r, 3), col(r, 0))
        rect(img, 1, 7, 14, 7, col(g, 4))
        rect(img, 7, 1, 7, 6, col(g, 4))
        rect(img, 10, 8, 10, 14, col(g, 4))
        put(img, 7, 7, WHITE)
    else:
        bevel(img, 0, 0, 15, 15, col(r, 4), col(r, 0))
        for y in (4, 9, 12):
            rect(img, 2, y, 13, y, col(r, 3) if y != 9 else col(r, 1))
    if not name.startswith(("tin", "vantite")):
        sheen(img, r, rng)
    return img


def metal_block_rivet(name, pal, rng):
    """Riveted plate block: two plates joined by a seam, rivet rows along the edges."""
    r = body(pal)
    img = plate(16, 16, r, rng)
    bevel(img, 0, 0, 15, 15, col(r, 3), col(r, 0))
    if pal == "vantite_steel":
        rect(img, 7, 1, 7, 14, col(r, 0))
        rect(img, 8, 1, 8, 14, col(r, 3))
        for y in (2, 6, 10):
            rivet(img, 2, y, r)
            rivet(img, 12, y, r)
            rivet(img, 5, y + 2, r)
            rivet(img, 10, y + 2, r)
    else:
        rect(img, 1, 7, 14, 7, col(r, 0))
        rect(img, 1, 8, 14, 8, col(r, 3))
        for x in (2, 6, 10):
            rivet(img, x, 2, r)
            rivet(img, x + 2, 11, r)
        rivet(img, 13, 4, r)
        rivet(img, 1, 12, r)
    return img


def hex_points(cx: float, cy: float, rad: float) -> list[tuple[float, float]]:
    """Return the six corners of a pointy-top hexagon."""
    return [(cx + rad * np.cos(np.pi / 6 + k * np.pi / 3), cy + rad * np.sin(np.pi / 6 + k * np.pi / 3)) for k in range(6)]


def metal_block_hex(name, pal, rng):
    """Alloy block with a glowing hexagonal lattice inlay."""
    r = body(pal)
    img = plate(16, 16, r, rng, base=1.6)
    bevel(img, 0, 0, 15, 15, col(r, 3), col(r, 0))
    outer = poly_mask(16, 16, hex_points(7.5, 7.5, 6.2))
    inner = poly_mask(16, 16, hex_points(7.5, 7.5, 4.6))
    core = poly_mask(16, 16, hex_points(7.5, 7.5, 2.2))
    img[outer & ~inner] = col(r, 4)
    img[inner] = col(r, 1)
    img[core] = col(r, 3)
    put(img, 7, 7, WHITE)
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(img, x, y, r)
    return img


def lattice_building(name, pal, rng):
    """Hexagonal building tiles of lattice alloy set in dark mortar (tileable)."""
    r = ramp(pal)
    img = noisy_fill(16, 16, r, rng, -0.3, 0.9, cells=(4, 2))
    rows = [(2, 4), (1, 5), (0, 6), (0, 6), (0, 6), (1, 5), (2, 4)]
    noise_v = fbm(16, 16, rng, (4, 2))
    for oy, ox in ((0, 0), (0, 8), (8, 4), (8, 12)):
        cell = np.zeros((16, 16), dtype=bool)
        for y, (a, b) in enumerate(rows):
            for x in range(a, b + 1):
                cell[(oy + y) % 16, (ox + x) % 16] = True
        lit = cell & ~np.roll(cell, 1, 0) | cell & ~np.roll(cell, 1, 1)
        dark = cell & ~np.roll(cell, -1, 0) | cell & ~np.roll(cell, -1, 1)
        vals = 2.4 + (noise_v - 0.5) * 0.7
        img[cell] = shade_img(r, dither(vals - 0.5, 0.4).clip(2, 3))[cell]
        img[dark] = col(r, 1)
        img[lit] = col(r, 4)
    for x, y in ((4, 7), (12, 7), (0, 15), (8, 15)):
        put(img, x, y, col(r, 3))
    return img


def foundation(name, pal, rng):
    """Heavy concrete slab with riveted steel edge strips top and bottom."""
    r = ramp(pal)
    img = noisy_fill(16, 16, r, rng, 1.3, 2.6, cells=(4, 2), dith=0.6)
    for _ in range(6):
        x, y = int(rng.integers(1, 15)), int(rng.integers(4, 12))
        put(img, x, y, col(r, 0))
        put(img, x + 1, y + 1, col(r, 3))
    for y0 in (0, 13):
        rect(img, 0, y0, 15, y0 + 2, col(r, 3))
        rect(img, 0, y0, 15, y0, col(r, 4))
        rect(img, 0, y0 + 2, 15, y0 + 2, col(r, 1))
        for x in (1, 6, 11):
            put(img, x + 1, y0 + 1, WHITE)
            put(img, x + 2, y0 + 1, col(r, 1))
    rect(img, 0, 12, 15, 12, col(r, 0))
    rect(img, 0, 3, 15, 3, col(r, 1))
    return img


def bastion(name, pal, rng):
    """Black armour plates in a gold frame with cyan glow seams and gold rivets."""
    r, g, c = body(pal), trim(pal), ramp("aegis_glow")
    img = plate(16, 16, r, rng, base=1.4, spread=0.3)
    bevel(img, 0, 0, 15, 15, col(g, 3), col(g, 1))
    bevel(img, 1, 1, 14, 14, col(g, 2), col(r, 0))
    rect(img, 7, 2, 8, 13, col(r, 0))
    rect(img, 2, 7, 13, 8, col(r, 0))
    rect(img, 7, 3, 7, 12, col(c, 3))
    rect(img, 3, 7, 12, 7, col(c, 3))
    put(img, 7, 7, col(c, 4))
    for x, y in ((3, 3), (11, 3), (3, 11), (11, 11)):
        rivet(img, x, y, g)
    return img


def bastion_glass(name, pal, rng):
    """Gold-framed glass: thin gold border and corner caps, clear centre with faint glints."""
    g = trim(pal)
    img = new(16, 16)
    bevel(img, 0, 0, 15, 15, col(g, 3), col(g, 1))
    for x, y in ((0, 0), (14, 0), (0, 14), (14, 14)):
        rect(img, x, y, x + 1, y + 1, col(g, 2))
        put(img, x, y, col(g, 4))
    c = ramp("aegis_glow")
    for k in range(4):
        put(img, 3 + k, 6 - k, col(c, 4, 110))
        put(img, 9 + k, 13 - k, col(c, 3, 90))
    return img


def grate(name, pal, rng):
    """Steel catwalk grate: beveled bars around see-through square holes."""
    r = ramp(pal)
    img = new(16, 16)
    for y in range(16):
        for x in range(16):
            hx, hy = x % 4 == 0, y % 4 == 0
            if hx and hy:
                img[y, x] = col(r, 4)
            elif hy:
                img[y, x] = col(r, 3 if x % 4 != 3 else 2)
            elif hx:
                img[y, x] = col(r, 2 if y % 4 != 3 else 1)
    for _ in range(4):
        x, y = int(rng.integers(0, 4)) * 4, int(rng.integers(0, 16))
        if y % 4:
            put(img, x, y, col(r, 1))
    return img


def hazard_stripes(name, pal, rng):
    """Worn diagonal yellow and black stripes (tileable)."""
    r = ramp(pal)
    n = value_noise(16, 16, rng, 4)
    img = new(16, 16)
    for y in range(16):
        for x in range(16):
            band = ((x + y) // 4) % 2 == 0
            if band:
                v = 3 if n[y, x] > 0.25 else 2
                if (x + y) % 4 == 0:
                    v = 4
                img[y, x] = col(r, v)
            else:
                img[y, x] = col(r, 1 if n[y, x] > 0.3 else 0)
    for _ in range(9):
        x, y = int(rng.integers(0, 16)), int(rng.integers(0, 16))
        put(img, x, y, col(r, 1) if ((x + y) // 4) % 2 == 0 else col(r, 2))
    return img


def rack_frame(name, pal, rng):
    """Open rack frame: two posts and two beams around a see-through centre."""
    r = ramp(pal)
    img = new(16, 16)
    wood = pal == "wood"
    post = shade_img(r, np.tile(np.array([[3, 2, 1]]), (16, 1)))
    for x0 in (0, 13):
        img[:, x0:x0 + 3] = post if wood else noisy_fill(3, 16, r, rng, 1.6, 2.8, cells=(2, 1))
        rect(img, x0, 0, x0, 15, col(r, 3))
        rect(img, x0 + 2, 0, x0 + 2, 15, col(r, 0))
    for y0 in (0, 13):
        img[y0:y0 + 3, 3:13] = shade_img(r, np.array([[3], [2], [1]]).repeat(10, axis=1))
        rect(img, 3, y0 + 2, 12, y0 + 2, col(r, 0))
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        put(img, x, y, col(r, 4) if not wood else col(r, 0))
    if wood:
        for y in range(16):
            if rng.random() < 0.3:
                put(img, 1, y, col(r, 1))
                put(img, 14, y, col(r, 1))
    if pal == "steel":
        paint = ramp("copper")
        for _ in range(7):
            x = int(rng.choice([0, 1, 2, 13, 14, 15]))
            y = int(rng.integers(0, 16))
            put(img, x, y, col(paint, 3))
            put(img, x, (y + 1) % 16, col(paint, 2))
    if pal == "lattice":
        for y in (4, 8, 11):
            put(img, 1, y, col(r, 4))
            put(img, 14, y, col(r, 4))
    return img


def red_tag(name, pal, rng):
    """Red warning tag on a string with a white exclamation mark (transparent ground)."""
    r = ramp("red_paint")
    img = new(16, 16)
    m = poly_mask(16, 16, [(5, 5), (8, 2), (11, 5), (11, 14), (5, 14)])
    img[m] = col(r, 2)
    rect(img, 6, 5, 6, 13, col(r, 3))
    rect(img, 10, 6, 10, 13, col(r, 1))
    rect(img, 6, 13, 10, 13, col(r, 1))
    put(img, 8, 4, col(r, 0))
    rect(img, 8, 7, 8, 10, WHITE)
    put(img, 8, 12, WHITE)
    add_outline(img, col(r, 0))
    s = ramp("canvas")
    for k, (x, y) in enumerate(((8, 3), (8, 2), (9, 1), (10, 0))):
        if k > 1:
            put(img, x, y, col(s, 2))
    return img


def panel(name, pal, rng):
    """Machine side panel: beveled casing, recessed inner plate, corner screws (vents for capacitors)."""
    r = body(pal)
    img = plate(16, 16, r, rng, spread=0.22)
    bevel(img, 0, 0, 15, 15, col(trim(pal) if pal == "aegis" else r, 3), col(r, 0))
    inset(img, 2, 2, 13, 13, r)
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(img, x, y, trim(pal))
    if "capacitor" in name or "vent" in name:
        for y in (5, 7, 9, 11):
            rect(img, 4, y, 11, y, col(r, 0))
            rect(img, 4, y + 1, 11, y + 1, col(r, 3))
    if pal == "aegis":
        c = ramp("aegis_glow")
        rect(img, 7, 4, 7, 11, col(c, 3))
        put(img, 7, 4, col(c, 4))
    return img


def panel_vent(name, pal, rng):
    """Panel with stacked louvres."""
    r = body(pal)
    img = plate(16, 16, r, rng, spread=0.22)
    bevel(img, 0, 0, 15, 15, col(r, 3), col(r, 0))
    for y in range(3, 13, 3):
        rect(img, 3, y, 12, y, col(r, 0))
        rect(img, 3, y + 1, 12, y + 1, col(r, 3))
        put(img, 2, y, col(r, 1))
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(img, x, y, r)
    return img


def panel_top(name, pal, rng):
    """Top plate with a hinged hatch, handle and corner bolts."""
    r = body(pal)
    img = plate(16, 16, r, rng, spread=0.22)
    bevel(img, 0, 0, 15, 15, col(r, 3), col(r, 0))
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5)
    img[(d < 5.6) & (d >= 4.6)] = col(r, 0)
    lid = d < 4.6
    img[lid & (xx + yy < 14)] = col(r, 3)
    img[lid & (xx + yy >= 14)] = col(r, 2)
    img[lid & (xx + yy > 18)] = col(r, 1)
    rect(img, 5, 7, 10, 7, col(r, 4))
    rect(img, 5, 8, 10, 8, col(r, 0))
    put(img, 7, 7, WHITE)
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(img, x, y, r)
    return img


def depot_side(name, pal, rng):
    """Corrugated container wall with frame rails and a worn stencil arrow."""
    r = body(pal)
    img = new(16, 16)
    for x in range(16):
        rect(img, x, 0, x, 15, col(r, (3, 2, 2, 1)[x % 4]))
    n = value_noise(16, 16, rng, 4)
    img[n > 0.82] = col(r, 1)
    for y0 in (0, 14):
        rect(img, 0, y0, 15, y0 + 1, col(r, 2))
        rect(img, 0, y0, 15, y0, col(r, 4))
        for x in (1, 7, 13):
            put(img, x, y0 + 1, col(r, 0))
    arrow = [(7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6),
             (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9), (7, 10), (8, 10), (7, 11), (8, 11)]
    for x, y in arrow:
        if rng.random() > 0.15:
            put(img, x, y, col(r, 4))
    return img


def conduit(name, pal, rng):
    """Gold pipe banded at both ends with a window onto a glowing violet core."""
    g, v = trim(pal), ramp("lattice")
    img = new(16, 16)
    for x in range(16):
        rect(img, x, 0, x, 15, col(g, (1, 2, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 2, 1)[x]))
    core = shade_img(v, dither(2.6 + (fbm(8, 10, rng, (4, 2)) - 0.5) * 2.2 - 0.5, 0.5).clip(2, 4))
    img[3:13, 4:12] = core
    rect(img, 7, 3, 8, 12, col(v, 4))
    for y in range(4, 12, 3):
        put(img, 7, y, WHITE)
    rect(img, 3, 3, 3, 12, col(g, 0))
    rect(img, 12, 3, 12, 12, col(g, 4))
    for y0 in (0, 13):
        rect(img, 0, y0, 15, y0 + 2, col(g, 2))
        rect(img, 0, y0, 15, y0, col(g, 4))
        rect(img, 0, y0 + 2, 15, y0 + 2, col(g, 0))
        for x in (2, 7, 12):
            put(img, x, y0 + 1, col(g, 4))
    return img


def mag_rail(name, pal, rng):
    """Rail (cutout): two alloy rails over glowing magnet pads."""
    r = ramp(pal)
    img = new(16, 16)
    for y0 in (1, 9):
        rect(img, 3, y0, 12, y0 + 5, col(r, 0))
        rect(img, 4, y0 + 1, 11, y0 + 4, col(r, 2))
        rect(img, 5, y0 + 2, 10, y0 + 3, col(r, 4))
        put(img, 6, y0 + 2, WHITE)
        rect(img, 4, y0 + 1, 11, y0 + 1, col(r, 3))
    for x0 in (1, 13):
        rect(img, x0, 0, x0 + 1, 15, col(r, 2))
        rect(img, x0, 0, x0, 15, col(r, 4))
        rect(img, x0 + 1, 0, x0 + 1, 15, col(r, 1))
    return img


def station_side(name, pal, rng):
    """Lattice station panel with a glowing status strip and screws."""
    r = ramp(pal)
    img = plate(16, 16, r, rng)
    bevel(img, 0, 0, 15, 15, col(r, 3), col(r, 0))
    inset(img, 2, 2, 13, 9, r)
    rect(img, 3, 3, 12, 8, col(r, 1))
    for x in range(4, 12, 2):
        put(img, x, 5, col(r, 4))
        put(img, x + 1, 6, col(r, 3))
    rect(img, 2, 11, 13, 12, col(r, 0))
    rect(img, 3, 11, 9, 11, col(r, 4))
    rect(img, 3, 12, 9, 12, col(r, 3))
    for x, y in ((1, 13), (13, 13)):
        rivet(img, x, y, r)
    return img


def station_top(name, pal, rng):
    """Docking top: two clamp jaws either side of a glowing guide line."""
    r = ramp(pal)
    img = plate(16, 16, r, rng)
    bevel(img, 0, 0, 15, 15, col(r, 3), col(r, 0))
    for x0 in (2, 10):
        rect(img, x0, 3, x0 + 3, 12, col(r, 1))
        bevel(img, x0, 3, x0 + 3, 12, col(r, 3), col(r, 0))
        rect(img, x0 + 1, 7, x0 + 2, 8, col(r, 0))
    rect(img, 7, 1, 8, 14, col(r, 0))
    rect(img, 7, 2, 7, 13, col(r, 4))
    put(img, 7, 7, WHITE)
    return img


def phase_node(name, pal, rng, phase=0.0):
    """Violet crystal sphere inside a black-gold aegis cage; phase (0..1) turns the light swirl around the sphere."""
    b, g, v = ramp("aegis_body"), ramp("aegis_gold"), ramp(pal)
    img = noisy_fill(16, 16, b, rng, 0.4, 1.6)
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5)
    sph = d < 5.6
    a = 2 * np.pi * phase + np.pi * 1.25
    lit = 3.3 + 0.35 * (np.cos(a) * (xx - 7.5) + np.sin(a) * (yy - 7.5)) / 2.0 - 0.2 * d
    img[sph] = shade_img(v, np.clip(np.rint(lit), 1, 4).astype(int))[sph]
    hx, hy = int(round(7.5 + 2.5 * np.cos(a))), int(round(7.5 + 2.5 * np.sin(a)))
    put(img, hx, hy, WHITE)
    put(img, hx + 1, hy, col(v, 4))
    for k in (3, 12):
        rect(img, k, 0, k, 15, col(g, 2))
        rect(img, 0, k, 15, k, col(g, 2))
    for k in (3, 12):
        for j in (3, 12):
            put(img, k, j, col(g, 4))
    bevel(img, 0, 0, 15, 15, col(g, 3), col(g, 1))
    return img


def archive_front(name, pal, rng):
    """Black terminal in a gold frame showing a cyan holographic grid with data blocks."""
    b, g, c = body(pal), trim(pal), ramp("aegis_glow")
    img = plate(16, 16, b, rng, base=1.2, spread=0.3)
    bevel(img, 0, 0, 15, 15, col(g, 3), col(g, 1))
    rect(img, 2, 2, 13, 11, col(c, 0))
    for k in range(2, 14, 3):
        rect(img, k, 2, k, 11, col(c, 1))
    for k in range(2, 12, 3):
        rect(img, 2, k, 13, k, col(c, 1))
    for x, y in ((5, 5), (8, 8), (11, 5), (5, 8)):
        put(img, x, y, col(c, 4))
    rect(img, 9, 3, 10, 4, col(c, 3))
    rect(img, 3, 9, 4, 10, col(c, 2))
    inset(img, 1, 1, 14, 12, g)
    for x in range(3, 13, 2):
        put(img, x, 13, col(g, 2))
        put(img, x, 14, col(b, 0))
    return img


def noise(name, pal, rng):
    """Soft steam-cloud puffs with partial transparency."""
    r = ramp(pal)
    n = fbm(16, 16, rng, (8, 4, 2))
    img = shade_img(r, dither(2.2 + n * 2.4 - 0.5, 0.5).clip(2, 4))
    img[..., 3] = np.clip(70 + n * 170, 0, 255).astype(np.uint8)
    return img


STYLES = {
    "metal_block": metal_block, "metal_block_rivet": metal_block_rivet, "metal_block_hex": metal_block_hex,
    "lattice_building": lattice_building, "foundation": foundation, "bastion": bastion, "bastion_glass": bastion_glass,
    "grate": grate, "hazard_stripes": hazard_stripes, "rack_frame": rack_frame, "red_tag": red_tag, "panel": panel,
    "panel_vent": panel_vent, "panel_top": panel_top, "depot_side": depot_side, "conduit": conduit,
    "mag_rail": mag_rail, "station_side": station_side, "station_top": station_top, "phase_node": phase_node,
    "archive_front": archive_front, "noise": noise,
}

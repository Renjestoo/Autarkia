"""Metal and structural block textures: storage blocks, plates, panels, racks, grates and tier-5/6 fittings.

Every painter has the signature (name, palette, rng) -> 16x16 RGBA array and shades a tone map with the
paint_tone helpers, so surfaces read as vanilla-style metal: 6-12 shades, soft clusters, no ordered dithering.
"""

import numpy as np

from paint import bevel, col, fbm, new, poly_mask, ramp
from paint_tone import (XX, YY, alloy, brushed, gold, grain, groove_h, groove_v, layer, metal, mramp, plating, recess,
                        rim, soft, stud, tone)


def body(pal: str) -> np.ndarray:
    """Return the body ramp of a palette (aegis bodies are black with a gold top shade)."""
    return ramp("aegis_body" if pal == "aegis" else pal)


def inset(img: np.ndarray, x0: int, y0: int, x1: int, y1: int, r: np.ndarray) -> None:
    """Draw a recessed frame: dark top and left edge, light bottom and right edge."""
    bevel(img, x0, y0, x1, y1, col(r, 1), col(r, 3))


def tin_tiles(r, rng):
    """Polished tin: four 8x8 tiles, each lit from its top-left with a soft sheen band, in-hue seams between them."""
    u, v = XX % 8, YY % 8
    tile = rng.random((2, 2))[(YY // 8).astype(int), (XX // 8).astype(int)]
    t = 0.6 + soft(rng, 0.08, (4, 2), (0.6, 0.4)) + (7 - u - v) * 0.022 + (tile - 0.5) * 0.08
    t += 0.14 * np.exp(-((u + v - 5.0) ** 2) / 2.5) - 0.07 * np.exp(-((u + v - 10.5) ** 2) / 2.0)
    t[(u == 0) | (v == 0)] += 0.1
    t[(u == 7) | (v == 7)] -= 0.26
    t[(u == 7) & (v == 7)] -= 0.06
    return tone(t, r)


def bronze_plate(r, rng):
    """Cast bronze plate: diagonal polish bands like copper, a raised rim and four corner rivets."""
    s = XX + YY
    t = 0.5 + soft(rng, 0.08, (4, 2), (0.6, 0.4)) + brushed(rng, 0.04) + 0.14 * np.cos(2 * np.pi * (s - 4) / 26)
    t += 0.08 * np.exp(-((s - 9.0) ** 2) / 1.5)
    t -= 0.06 * np.exp(-((s - 20.0) ** 2) / 4.0)
    t += rim(0.1, -0.08, -0.12, -0.3)
    recess(t, 2, 2, 13, 13, 0.0, 0.08)
    for x, y in ((3, 3), (11, 3), (3, 11), (11, 11)):
        stud(t, x, y, 0.32)
    return tone(t, r)


def zinc_spangle(r, rng):
    """Galvanised zinc: three rolled plate bands (lit top row, shaded bottom row) over soft crystalline spangle facets."""
    k = 9
    pts = rng.random((k, 2)) * 16
    dx = (XX[..., None] + 0.5) - pts[:, 0]
    dy = (YY[..., None] + 0.5) - pts[:, 1]
    dx, dy = (dx + 8) % 16 - 8, (dy + 8) % 16 - 8
    d = np.hypot(dx, dy)
    order = np.argsort(d, axis=2)
    cell = order[..., 0]
    d1 = np.take_along_axis(d, order[..., :1], 2)[..., 0]
    d2 = np.take_along_axis(d, order[..., 1:2], 2)[..., 0]
    face = 0.42 + rng.random(k) * 0.2
    ang = rng.random(k) * 2 * np.pi
    gx = np.take_along_axis(dx, cell[..., None], 2)[..., 0]
    gy = np.take_along_axis(dy, cell[..., None], 2)[..., 0]
    t = face[cell] + 0.025 * (gx * np.cos(ang[cell]) + gy * np.sin(ang[cell])) + soft(rng, 0.03, (2,), (1.0,))
    t -= 0.07 * (d2 - d1 < 0.9)
    band = (YY - 1) % 5
    t += np.select([band == 0, band == 4], [0.12, -0.12], 0.0) + (7.5 - XX) * 0.008
    t += rim(0.08, -0.06, -0.14, -0.28)
    return tone(t, r)


def brass_engraved(r, rng):
    """Polished brass: gold-like soft blotches, a soft edge, an engraved inner border and a small engraved diamond."""
    t = 0.58 + soft(rng, 0.15, (8, 4), (0.7, 0.3)) + soft(rng, 0.05, (2,), (1.0,)) + (15 - XX - YY) * 0.01
    t += 0.1 * np.exp(-((XX + YY - 7.0) ** 2) / 3.0)
    t += rim(0.12, -0.08, -0.16, -0.3)
    groove_h(t, 3, 3, 12)
    groove_h(t, 12, 3, 12)
    groove_v(t, 3, 3, 12)
    groove_v(t, 12, 3, 12)
    m = np.abs(XX - 7.5) + np.abs(YY - 7.5)
    t[(m > 2.2) & (m < 3.2)] -= 0.24
    t[(m > 1.2) & (m <= 2.2) & (XX + YY > 15)] += 0.1
    return tone(t, r)


def vantite_glow(r, rng):
    """Dark teal vantite plate, brushed, with faint pale-teal glow seams and a soft halo around them."""
    g = mramp("vantite", 5, 0.66, 0.96, 0.1, 0.15)
    t = 0.5 + brushed(rng, 0.1) + soft(rng, 0.07, (4, 2), (0.6, 0.4)) + grain(rng, 0.03) + (15 - XX - YY) * 0.01
    t += rim(0.1, -0.06, -0.16, -0.3)
    seam = np.zeros((16, 16), dtype=bool)
    seam[7, 1:15] = True
    seam[1:7, 7] = True
    seam[8:15, 10] = True
    p = np.pad(seam, 1)
    halo = (p[:-2, 1:-1] | p[2:, 1:-1] | p[1:-1, :-2] | p[1:-1, 2:]) & ~seam
    t[halo] += 0.12
    img = tone(t, r)
    gt = 0.45 + soft(rng, 0.35, (4, 2), (0.5, 0.5))
    gt[7, 7] = gt[7, 10] = 1.0
    layer(img, gt, g, seam)
    return img


METAL_BLOCKS = {"tin": tin_tiles, "bronze": bronze_plate, "zinc": zinc_spangle, "brass": brass_engraved, "vantite": vantite_glow}


def metal_block(name, pal, rng):
    """Storage block of a metal; layout by material: tin tiles, bronze cast plate, zinc spangle, brass engraving, vantite seams."""
    return METAL_BLOCKS[pal](metal(pal), rng)


def metal_block_rivet(name, pal, rng):
    """Riveted plate block: two brushed plates joined by a seam (vertical for vantite steel), rivets along the plate ends."""
    r = metal(pal)
    vert = pal == "vantite_steel"
    t = 0.56 + brushed(rng, 0.12, vertical=vert) + soft(rng, 0.05, (8, 4)) + grain(rng, 0.03)
    a, b = (XX, YY) if vert else (YY, XX)
    t += np.where(a < 8, 7 - a, 15 - a) * 0.022 - 0.077
    t += (15 - b) * 0.006
    t += rim(0.1, -0.06, -0.14, -0.3)
    if vert:
        groove_v(t, 7, 1, 14, 0.3)
        for y in (2, 7, 12):
            stud(t, 3, y, 0.3)
            stud(t, 11, y, 0.3)
    else:
        groove_h(t, 7, 1, 14, 0.3)
        for y in (3, 11):
            stud(t, 2, y, 0.3)
            stud(t, 12, y, 0.3)
    return tone(t, r)


def hex_points(cx: float, cy: float, rad: float) -> list[tuple[float, float]]:
    """Return the six corners of a pointy-top hexagon."""
    return [(cx + rad * np.cos(np.pi / 6 + k * np.pi / 3), cy + rad * np.sin(np.pi / 6 + k * np.pi / 3)) for k in range(6)]


def metal_block_hex(name, pal, rng):
    """Violet alloy block: brushed plate with a raised hexagonal inlay ring around a glowing crystal core."""
    r = metal(pal)
    core_r = mramp("lattice", 5, 0.55, 0.95, 0.2, 0.12)
    t = 0.44 + brushed(rng, 0.05) + soft(rng, 0.05, (8, 4)) + (15 - XX - YY) * 0.007
    t += rim(0.1, -0.06, -0.14, -0.3)
    outer = poly_mask(16, 16, hex_points(7.5, 7.5, 6.4))
    inner = poly_mask(16, 16, hex_points(7.5, 7.5, 4.6))
    core = poly_mask(16, 16, hex_points(7.5, 7.5, 2.4))
    ring = outer & ~inner
    t[ring] += 0.24 + (7.5 - YY[ring]) * 0.025
    t[inner] -= 0.1
    t[inner & (YY < 5)] -= 0.08
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        stud(t, x, y, 0.3)
    img = tone(t, r)
    ct = 0.75 - np.hypot(XX - 6.8, YY - 6.8) * 0.22 + soft(rng, 0.06, (4, 2), (0.6, 0.4))
    layer(img, ct, core_r, core)
    return img


def lattice_building(name, pal, rng):
    """Hexagonal violet alloy building tiles, each softly top-lit, set in dark mortar (tileable)."""
    r = metal(pal)
    pts = np.array([(4.0, 4.0), (12.0, 4.0), (0.0, 12.0), (8.0, 12.0)])
    dx = (XX[..., None] + 0.5 - pts[:, 0] + 8) % 16 - 8
    dy = ((YY[..., None] + 0.5 - pts[:, 1] + 8) % 16 - 8) * 0.866
    d = np.sort(np.hypot(dx, dy), axis=2)
    cell = np.argmin(np.hypot(dx, dy), axis=2)
    mortar = d[..., 1] - d[..., 0] < 1.0
    face = ~mortar
    top = face & (np.roll(mortar, 1, 0) | np.roll(mortar, 1, 1))
    low = face & (np.roll(mortar, -1, 0) | np.roll(mortar, -1, 1)) & ~top
    t = 0.18 + soft(rng, 0.08, (4, 2), (0.5, 0.5))
    shade = 0.56 + (rng.random(4) - 0.5) * 0.1
    t[face] = (shade[cell] + soft(rng, 0.1, (4, 2), (0.6, 0.4)) + grain(rng, 0.07) - d[..., 0] * 0.012)[face]
    t[top] += 0.16
    t[low] -= 0.16
    return tone(t, r)


def foundation(name, pal, rng):
    """Heavy concrete slab with small pits, riveted steel edge strips top and bottom."""
    c, s = metal(pal), metal("steel", 10)
    t = 0.5 + soft(rng, 0.09, (8, 4), (0.6, 0.4)) + soft(rng, 0.06, (2,), (1.0,)) + grain(rng, 0.06)
    for _ in range(7):
        x, y = int(rng.integers(1, 14)), int(rng.integers(4, 11))
        t[y, x] -= 0.3
        t[y + 1, x + 1] += 0.12
    t[3, :] -= 0.18
    t[12, :] -= 0.1
    img = tone(t, c)
    st = 0.55 + brushed(rng, 0.09) + soft(rng, 0.04, (2,), (1.0,))
    for y0 in (0, 13):
        st[y0] += 0.2
        st[y0 + 2] -= 0.22
        for x in (2, 7, 12):
            st[y0 + 1, x] += 0.32
            st[y0 + 1, x + 1] -= 0.2
    band = (YY < 3) | (YY > 12)
    layer(img, st, s, band)
    return img


def seam_glow(img: np.ndarray, line: np.ndarray, reach: float, cx: float = 7.5, cy: float = 7.5) -> None:
    """Light a 1px seam with dim cyan that glows only near its core at (cx, cy) and fades into the dark seam by reach px."""
    d = np.hypot(XX - cx, YY - cy)
    layer(img, 0.95 - d / reach, mramp("aegis_glow", 7, 0.0, 0.78, 0.1, 0.35), line & (d < reach))


def bastion(name, pal, rng):
    """Netherite-like armour: four mottled dark plates lit on their top-left edges, a gold frame with corner brackets,
    a dark seam cross glowing dim cyan only at its core, gold rivets."""
    b, g = metal("aegis", 12), gold()
    t = plating(rng, 0.74)
    for x0, x1 in ((1, 6), (9, 14)):
        for y0, y1 in ((1, 6), (9, 14)):
            t += rim(0.0, 0.0, 0.2, -0.16, x0, y0, x1, y1)
    t[1:15, 7] = t[7, 1:15] = 0.06
    t[1:15, 8] = t[8, 1:15] = 0.16
    img = tone(t, b)
    frame = (XX == 0) | (XX == 15) | (YY == 0) | (YY == 15)
    brackets = frame | (((XX <= 3) | (XX >= 12)) & ((YY == 1) | (YY == 14))) | (((YY <= 3) | (YY >= 12)) & ((XX == 1) | (XX == 14)))
    gt = 0.5 + soft(rng, 0.08, (4, 2), (0.6, 0.4)) - (XX + YY) * 0.008
    gt[0, :] += 0.16
    gt[:, 0] += 0.1
    gt[15, :] -= 0.22
    gt[:, 15] -= 0.16
    gt[brackets & ~frame] -= 0.06
    layer(img, gt, g, brackets)
    seam_glow(img, (XX == 7) | (YY == 7), 3.6)
    for x, y in ((3, 3), (11, 3), (3, 11), (11, 11)):
        rt = np.full((16, 16), 0.45)
        rt[y, x] = 0.85
        rt[y + 1, x + 1] = 0.2
        layer(img, rt, g, (XX >= x) & (XX <= x + 1) & (YY >= y) & (YY <= y + 1))
    return img


def bastion_glass(name, pal, rng):
    """Gold-framed clear glass: shaded gold border with corner caps, clear centre with vanilla-style glints."""
    g = gold()
    img = new(16, 16)
    frame = (XX == 0) | (XX == 15) | (YY == 0) | (YY == 15)
    caps = ((XX < 2) | (XX > 13)) & ((YY < 2) | (YY > 13))
    t = 0.6 + soft(rng, 0.08, (4, 2), (0.6, 0.4)) - (XX + YY) * 0.012
    t[0, :] += 0.12
    t[:, 0] += 0.06
    t[15, :] -= 0.18
    t[:, 15] -= 0.14
    t[caps & ~frame] += 0.18
    layer(img, t, g, frame | caps)
    glint = mramp("aegis_glow", 3, 0.7, 0.95, 0.05, 0.4)
    gl = np.zeros((16, 16), dtype=bool)
    for x, y in ((4, 3), (3, 4), (2, 5), (12, 11), (11, 12)):
        gl[y, x] = True
    layer(img, 0.4 + (XX < 8) * 0.6 - YY * 0.01, glint, gl)
    return img


def grate(name, pal, rng):
    """Steel mesh grate (cutout, tileable): 2px raised bars lit on their top-left, square see-through holes between."""
    r = metal(pal)
    img = new(16, 16)
    u, v = XX % 4, YY % 4
    gap_x, gap_y = (u == 1) | (u == 2), (v == 1) | (v == 2)
    t = 0.52 + soft(rng, 0.1, (8, 4), (0.6, 0.4)) + soft(rng, 0.07, (2,), (1.0,))
    hb, vb, knot = ~gap_y & gap_x, ~gap_x & gap_y, ~gap_x & ~gap_y
    t[hb & (v == 3)] += 0.12
    t[hb & (v == 0)] -= 0.1
    t[vb & (u == 3)] += 0.08
    t[vb & (u == 0)] -= 0.1
    t[knot & (u == 3) & (v == 3)] += 0.2
    t[knot & (u == 0) & (v == 0)] -= 0.08
    t[knot & (u != v)] += 0.05
    layer(img, t, r, ~(gap_x & gap_y))
    return img


def hazard_stripes(name, pal, rng):
    """Worn diagonal yellow and black paint stripes on steel (tileable); chipped edges show bare metal."""
    y, k, s = mramp("hazard", 8, 0.42, 0.95, 0.3, 0.22), metal("steel", 8), mramp("steel", 6, 0.0, 0.3, 0.25, 0.0)
    band = ((XX + YY) // 4) % 2 == 0
    yt = 0.55 + soft(rng, 0.16, (8, 4, 2)) + np.where((XX + YY) % 4 == 0, 0.1, 0.0) - np.where((XX + YY) % 4 == 3, 0.1, 0.0)
    kt = 0.5 + soft(rng, 0.3, (8, 4, 2))
    img = tone(kt, s)
    layer(img, yt, y, band)
    wear = fbm(16, 16, rng, (8, 2), (0.5, 0.5)) + rng.random((16, 16)) * 0.25
    edge = ((XX + YY) % 4 == 0) | ((XX + YY) % 4 == 3)
    bare = (wear > 0.95) | (edge & (wear > 0.85))
    layer(img, 0.6 + soft(rng, 0.2, (4, 2), (0.6, 0.4)), k, bare)
    return img


def rack_frame(name, pal, rng):
    """Open rack frame: two posts and two beams around a see-through centre; posts in columns 0-1, beam tops in rows 0-2.

    The rack models sample columns 0-1 for the posts and rows 0-2 of columns 3-12 for the shelves, so those regions
    carry the material read: wood grain, brushed steel with orange primer chips, or violet alloy with glow studs.
    """
    r = metal(pal)
    img = new(16, 16)
    wood = pal == "wood"
    post = (XX < 3) | (XX > 12)
    beam = ((YY < 3) | (YY > 12)) & ~post
    streak = 0.2 if wood else 0.1
    pt = 0.52 + brushed(rng, streak, vertical=True) + soft(rng, 0.05, (4, 2), (0.6, 0.4))
    pt += np.select([XX % 13 == 0, XX % 13 == 1, XX % 13 == 2], [0.16, 0.02, -0.2], 0.0)
    bt = 0.52 + brushed(rng, streak) + soft(rng, 0.05, (4, 2), (0.6, 0.4))
    bt += np.select([YY % 13 == 0, YY % 13 == 1, YY % 13 == 2], [0.16, 0.02, -0.22], 0.0)
    bt[beam & ((XX == 3) | (XX == 12))] -= 0.1
    layer(img, pt, r, post)
    layer(img, bt, r, beam)
    if wood:
        for x, y in ((1, 4), (14, 10), (6, 1), (10, 14)):
            layer(img, np.full((16, 16), 0.22), r, (XX == x) & (YY == y))
    if pal == "steel":
        paint = mramp("copper", 4, 0.35, 0.8, 0.25, 0.3)
        chips = (rng.random((16, 16)) < 0.07) & (post | beam)
        layer(img, 0.5 + soft(rng, 0.4, (2,), (1.0,)), paint, chips)
    if pal == "lattice":
        sheen = grain(rng, 0.045) + 0.09 * np.cos(2 * np.pi * (XX + YY - 4) / 14)
        layer(img, pt + sheen - 0.04 + (7.5 - YY) * 0.012, r, post)
        layer(img, bt + sheen - 0.04 + (7.5 - XX) * 0.012, r, beam)
        lt = mramp("lattice", 3, 0.7, 0.95, 0.1, 0.1)
        for y in (4, 8, 11):
            layer(img, np.full((16, 16), 0.5 + (y == 8) * 0.5), lt, (YY == y) & ((XX == 1) | (XX == 14)))
    return img


def red_tag(name, pal, rng):
    """Red warning tag on a string, softly shaded, with a pale painted exclamation mark (transparent ground)."""
    r, paper, s = mramp("red_paint", 8, 0.0, 0.9, 0.25, 0.1), mramp("canvas", 4, 0.5, 1.0, 0.1, 0.0), mramp("canvas", 3, 0.2, 0.6)
    img = new(16, 16)
    m = poly_mask(16, 16, [(5, 5), (8, 2), (11, 5), (11, 14), (5, 14)])
    t = 0.6 + soft(rng, 0.08, (4, 2), (0.6, 0.4)) + (8 - XX) * 0.04 - (YY - 8) * 0.012
    edge = m & ~(np.roll(m, 1, 0) & np.roll(m, -1, 0) & np.roll(m, 1, 1) & np.roll(m, -1, 1))
    t[edge] -= 0.35
    t[edge & (XX < 8) & (YY < 13)] += 0.15
    t[4, 8] = 0.0
    layer(img, t, r, m)
    mark = ((XX == 8) & (YY >= 7) & (YY <= 10)) | ((XX == 8) & (YY == 12))
    layer(img, 0.9 - (YY - 7) * 0.12, paper, mark)
    string = np.zeros((16, 16), dtype=bool)
    for x, y in ((8, 1), (9, 0)):
        string[y, x] = True
    layer(img, 0.3 + YY * 0.3, s, string)
    return img


def casing(rng, base: float = 0.6) -> np.ndarray:
    """Return the shared machine-casing tone map: brushed plate, soft edge, 2px frame band and a recessed centre plate."""
    s = XX + YY
    t = base + brushed(rng, 0.13) + soft(rng, 0.04, (2,), (1.0,)) + grain(rng, 0.025) + 0.1 * np.cos(2 * np.pi * (s - 5) / 18)
    t += 0.07 * np.exp(-((s - 8.0) ** 2) / 1.2) - (s - 15) * 0.006
    t += rim(0.1, -0.06, -0.14, -0.3)
    recess(t, 3, 3, 12, 12, 0.1, 0.14)
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        t[y, x] += 0.3
        t[y + 1, x + 1] -= 0.22
    return t


def panel(name, pal, rng):
    """Machine side panel: brushed casing, frame band with corner screws, recessed centre plate (vents on capacitors).

    Aegis panels gild the frame band and fill the recess with two netherite-like dark plates split by a seam that
    glows dim cyan only at its core.
    """
    r = metal(pal)
    t = casing(rng, 0.6)
    if "capacitor" in name:
        for y in (5, 7, 9):
            t[y, 5:11] -= 0.3
            t[y + 1, 5:11] += 0.12
    img = tone(t, r)
    if pal == "aegis":
        layer(img, t - 0.05, gold(), (XX < 3) | (XX > 12) | (YY < 3) | (YY > 12))
        pt = plating(rng, 0.76)
        recess(pt, 3, 3, 12, 12, 0.0, 0.2)
        for x0, x1 in ((4, 6), (9, 11)):
            pt += rim(0.0, 0.0, 0.16, -0.12, x0, 4, x1, 11)
        pt[4:12, 7] = 0.05
        pt[4:12, 8] = 0.15
        layer(img, pt, metal("aegis", 12), (XX >= 3) & (XX <= 12) & (YY >= 3) & (YY <= 12))
        seam_glow(img, (XX == 7) & (YY >= 4) & (YY <= 11), 2.1, 7.5, 7.5)
    return img


def panel_vent(name, pal, rng):
    """Machine side panel with three louvres across the recessed centre plate."""
    r = metal(pal)
    t = casing(rng)
    for y in (4, 7, 10):
        t[y, 4:12] -= 0.32
        t[y + 1, 4:12] += 0.16
        t[y + 2, 4:12] -= 0.04
        t[y, 4] -= 0.06
    return tone(t, r)


def panel_top(name, pal, rng):
    """Top plate: brushed casing with a round hinged hatch, its handle and corner bolts."""
    r = metal(pal)
    t = 0.58 + brushed(rng, 0.06) + soft(rng, 0.05, (8, 4)) + (15 - XX - YY) * 0.006 + rim(0.1, -0.06, -0.14, -0.3)
    d = np.hypot(XX - 7.5, YY - 7.5)
    t[(d >= 4.6) & (d < 5.6)] -= 0.28
    t[(d >= 4.6) & (d < 5.6) & (XX + YY > 16)] += 0.12
    lid = d < 4.6
    t[lid] += 0.08 + ((7.5 - XX) + (7.5 - YY))[lid] * 0.025
    t[7, 5:11] += 0.22
    t[8, 5:11] -= 0.22
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        stud(t, x, y, 0.3)
    return tone(t, r)


def depot_side(name, pal, rng):
    """Corrugated container wall: shaded vertical ribs, riveted frame rails, worn pale stencilled 'this way up' chevrons."""
    r = metal(pal)
    paint = mramp("canvas", 4, 0.45, 0.9, 0.1, 0.2)
    t = 0.55 + brushed(rng, 0.05, vertical=True) + soft(rng, 0.05, (8, 4))
    t += np.select([XX % 4 == 0, XX % 4 == 1, XX % 4 == 2], [0.14, 0.06, -0.04], -0.16)
    for y0 in (0, 14):
        t[y0:y0 + 2] = 0.55 + brushed(rng, 0.05)[y0:y0 + 2]
        t[y0] += 0.16
        t[y0 + 1] -= 0.12
        for x in (2, 8, 13):
            t[y0, x] += 0.25
            t[y0 + 1, x] -= 0.15
    t[2] -= 0.12
    t[13] -= 0.08
    img = tone(t, r)
    lift = YY - (np.abs(XX - 7.5) - 0.5)
    chevrons = (np.abs(XX - 7.5) <= 4.5) & (((lift >= 4) & (lift < 6)) | ((lift >= 8) & (lift < 10)))
    wear = fbm(16, 16, rng, (4, 2), (0.6, 0.4)) + rng.random((16, 16)) * 0.15
    layer(img, t * 0.7 + 0.2, paint, chevrons & (wear < 0.85))
    return img


def conduit(name, pal, rng):
    """Gold pipe, round-shaded, banded at both ends, with a window onto a softly glowing violet core."""
    g, v = gold(), mramp("lattice", 7, 0.3, 0.95, 0.2, 0.1)
    prof = np.array([0.3, 0.5, 0.66, 0.74, 0.72, 0.66, 0.6, 0.56, 0.52, 0.48, 0.44, 0.4, 0.36, 0.3, 0.24, 0.16])
    t = prof[XX.astype(int)] + brushed(rng, 0.04, vertical=True) + soft(rng, 0.04, (8, 4))
    for y0 in (0, 13):
        t[y0:y0 + 3] += 0.06
        t[y0] += 0.12
        t[y0 + 2] -= 0.2
    recess(t, 3, 3, 12, 12, 0.0, 0.16)
    img = tone(t, g)
    win = (XX >= 4) & (XX <= 11) & (YY >= 4) & (YY <= 11)
    ct = 0.45 + soft(rng, 0.22, (4, 2), (0.6, 0.4)) - np.abs(XX - 7.5) * 0.06 + 0.25 * np.exp(-((XX - 7.5) ** 2) / 1.2)
    layer(img, ct, v, win)
    return img


def mag_rail(name, pal, rng):
    """Rail (cutout): two alloy rails at columns 1-2 and 13-14 (outer side lit, like vanilla rails) on four 2px brushed
    alloy sleepers at vanilla's tie rows, each with a magnet core glowing at the track centre."""
    r, glow = metal(pal), mramp("lattice", 6, 0.5, 0.95, 0.15, 0.1)
    img = new(16, 16)
    st = alloy(rng, 0.24)
    for y0 in (1, 5, 9, 13):
        tie = (XX >= 1) & (XX <= 14) & (YY >= y0) & (YY <= y0 + 1)
        layer(img, st + np.where(YY == y0, 0.1, -0.1), r, tie)
        core = tie & (XX >= 6) & (XX <= 9)
        layer(img, 0.9 - np.abs(XX - 7.5) * 0.18 - (YY - y0) * 0.18, glow, core)
    rt = alloy(rng, 0.66, vertical=True)
    rt[:, 1] += 0.18
    rt[:, 14] += 0.18
    rt[:, 2] -= 0.12
    rt[:, 13] -= 0.12
    layer(img, rt, r, (XX == 1) | (XX == 2) | (XX == 13) | (XX == 14))
    return img


def station_side(name, pal, rng):
    """Lattice station panel: casing with a recessed dark display of glowing indicator bars and a lit status strip."""
    r, glow = metal(pal), mramp("lattice", 5, 0.55, 0.95, 0.15, 0.1)
    t = alloy(rng, 0.58) + rim(0.1, -0.06, -0.14, -0.3)
    recess(t, 2, 2, 13, 9, 0.3, 0.1)
    recess(t, 2, 11, 13, 12, 0.25, 0.08)
    for x, y in ((1, 13), (13, 13)):
        stud(t, x, y, 0.3)
    img = tone(t, r)
    bars = np.zeros((16, 16), dtype=bool)
    for k, x in enumerate(range(4, 12, 2)):
        bars[8 - (2 + k % 3):8, x] = True
    layer(img, 0.4 + (8 - YY) * 0.15, glow, bars)
    strip = (YY == 11) & (XX >= 3) & (XX <= 9)
    layer(img, 0.9 - (XX - 3) * 0.06, glow, strip)
    return img


def station_top(name, pal, rng):
    """Docking top: brushed casing, two raised clamp jaws either side of a recessed glowing guide line."""
    r, glow = metal(pal), mramp("lattice", 5, 0.55, 0.95, 0.15, 0.1)
    t = alloy(rng, 0.5, vertical=True) + rim(0.1, -0.06, -0.14, -0.3)
    for x0 in (2, 10):
        jaw = (XX >= x0) & (XX <= x0 + 3) & (YY >= 3) & (YY <= 12)
        t[jaw] += 0.16
        t[3, x0:x0 + 4] += 0.12
        t[3:13, x0] += 0.08
        t[12, x0:x0 + 4] -= 0.2
        t[3:13, x0 + 3] -= 0.16
        t[7:9, x0 + 1:x0 + 3] -= 0.3
    recess(t, 6, 1, 9, 14, 0.25, 0.1)
    img = tone(t, r)
    line = (XX == 7) & (YY >= 2) & (YY <= 13)
    layer(img, 0.6 + 0.3 * np.exp(-((YY - 7.5) ** 2) / 8.0), glow, line)
    return img


def phase_node(name, pal, rng, phase=0.0):
    """Violet crystal sphere inside a black-gold aegis cage; phase (0..1) turns the light swirl around the sphere."""
    b, g, v = metal("aegis", 9), gold(), mramp(pal, 9, 0.15, 0.95, 0.25, 0.1)
    img = tone(0.5 + soft(rng, 0.1, (8, 4), (0.6, 0.4)), b)
    d = np.hypot(XX - 7.5, YY - 7.5)
    sph = d < 5.6
    a = 2 * np.pi * phase + np.pi * 1.25
    lit = 0.62 + 0.09 * (np.cos(a) * (XX - 7.5) + np.sin(a) * (YY - 7.5)) - 0.05 * d - 0.03 * (XX + YY - 15)
    lit += 0.18 * np.exp(-((XX - 7.5 - 2.4 * np.cos(a)) ** 2 + (YY - 7.5 - 2.4 * np.sin(a)) ** 2) / 1.6)
    layer(img, lit, v, sph)
    bars = ((XX == 3) | (XX == 12) | (YY == 3) | (YY == 12))
    frame = (XX == 0) | (XX == 15) | (YY == 0) | (YY == 15)
    gt = 0.5 + soft(rng, 0.06, (4, 2), (0.6, 0.4)) - (XX + YY) * 0.01
    gt[((XX == 3) | (XX == 12)) & ((YY == 3) | (YY == 12))] += 0.3
    gt[0, :] += 0.15
    gt[:, 0] += 0.1
    gt[15, :] -= 0.2
    gt[:, 15] -= 0.15
    layer(img, gt, g, bars | frame)
    return img


def archive_front(name, pal, rng):
    """Black terminal in a gold frame: a recessed dark bezel round deep cyan glass with soft scanlines, a glow falloff
    from the centre, glowing data rows with faint halos and a corner glare; netherite-like plating and vents below."""
    b, g, c = metal("aegis", 12), gold(), mramp("aegis_glow", 9, 0.0, 0.8, 0.12, 0.35)
    t = plating(rng, 0.62)
    t[13, 3:13:2] += 0.3
    t[14, 3:13:2] -= 0.3
    img = tone(t, b)
    ft = 0.55 + soft(rng, 0.06, (4, 2), (0.6, 0.4)) - (XX + YY) * 0.008
    ft[0, :] += 0.16
    ft[:, 0] += 0.1
    ft[15, :] -= 0.22
    ft[:, 15] -= 0.16
    layer(img, ft, g, (XX == 0) | (XX == 15) | (YY == 0) | (YY == 15))
    bt = plating(rng, 0.45)
    bt[1, :] -= 0.2
    bt[:, 1] -= 0.15
    bt[12, :] += 0.25
    bt[:, 14] += 0.2
    layer(img, bt, b, (XX >= 1) & (XX <= 14) & (YY >= 1) & (YY <= 12))
    d = np.hypot((XX - 7.5) / 6.5, (YY - 6.5) / 5.5)
    st = 0.08 + 0.3 * np.clip(1 - d, 0, 1) ** 1.5 + soft(rng, 0.03, (4, 2), (0.6, 0.4)) - (YY % 2) * 0.035
    st[2, :] -= 0.07
    st[:, 2] -= 0.05
    for y in (4, 6, 8, 10):
        x = 3 + int(rng.integers(0, 2))
        while x < 12:
            n = int(rng.integers(1, 4))
            st[y, x:min(x + n, 12)] = 0.62 + rng.random() * 0.2 - (y - 4) * 0.02
            st[y - 1, x:min(x + n, 12)] += 0.07
            x += n + 1 + int(rng.random() < 0.3)
    st[2, 12] += 0.3
    st[3, 13] += 0.3
    st[2, 13] += 0.2
    layer(img, st, c, (XX >= 2) & (XX <= 13) & (YY >= 2) & (YY <= 11))
    return img


def noise(name, pal, rng):
    """Soft steam-cloud puffs, smoothly shaded and partly transparent."""
    r = mramp(pal, 8, 0.45, 1.0, 0.2, 0.0)
    n = fbm(16, 16, rng, (8, 4, 2))
    img = tone(0.15 + n * 0.85, r)
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

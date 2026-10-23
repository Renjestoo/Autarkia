"""Timber, stone-craft and clay block textures: planks, crates, boards, posts, pulleys, crucibles and querns.

Every painter has the signature (name, palette, rng) -> 16x16 RGBA array.
"""

import numpy as np

from paint import (add_outline, bevel, col, dither, disc_mask, fbm, mix, new, noisy_fill, plate, put, ramp, rect, rivet,
                   shade_img, tile_shift, value_noise)
from paint_blocks_nature import wput

WHITE = (255, 255, 255, 255)


def planks(r: np.ndarray, rng: np.random.Generator, height: int = 4, base: float = 2.0) -> np.ndarray:
    """Return tileable horizontal planks with grain streaks, seams and butt joints."""
    img = new(16, 16)
    for p in range(16 // height):
        y0 = p * height
        tone = base + (rng.random() - 0.5) * 0.7
        n = value_noise(16, height, rng, 4)
        vals = tone + (n - 0.5) * 0.8
        img[y0:y0 + height] = shade_img(r, dither(vals - 0.5, 0.3).clip(1, 3))
        for _ in range(2):
            gy, gx, ln = y0 + int(rng.integers(0, height - 1)), int(rng.integers(0, 16)), int(rng.integers(4, 9))
            for k in range(ln):
                wput(img, gx + k, gy, col(r, 1))
        rect(img, 0, y0 + height - 1, 15, y0 + height - 1, col(r, 0))
        if rng.random() < 0.5:
            rect(img, 0, y0, 15, y0, col(r, 3))
        jx = int(rng.integers(0, 16))
        for y in range(y0, y0 + height - 1):
            wput(img, jx, y, col(r, 0))
            wput(img, jx + 1, y, col(r, 3))
    return img


def plank(name, pal, rng):
    """Plain tileable planks."""
    return planks(ramp(pal), rng)


def beam(name, pal, rng):
    """Rough vertical timber with two riveted iron bands."""
    r, iron = ramp(pal), ramp("iron")
    n = value_noise(16, 16, rng, 4)
    xx = np.arange(16)[None, :]
    grain = np.sin(xx * 2.2 + value_noise(16, 16, rng, 8) * 4.0)
    vals = 1.9 + (n - 0.5) * 0.6 + 0.6 * (grain > 0.7) - 0.7 * (grain < -0.8)
    img = shade_img(r, dither(vals - 0.5, 0.3).clip(0, 3))
    for _ in range(2):
        x, y = int(rng.integers(2, 14)), int(rng.integers(0, 16))
        put(img, x, y, col(r, 0))
        put(img, x, y + 1, col(r, 0))
        put(img, x - 1, y, col(r, 1))
    for y0 in (2, 11):
        rect(img, 0, y0, 15, y0 + 2, col(iron, 2))
        rect(img, 0, y0, 15, y0, col(iron, 3))
        rect(img, 0, y0 + 2, 15, y0 + 2, col(iron, 1))
        rect(img, 0, y0 + 3, 15, y0 + 3, col(r, 0))
        for x in (3, 12):
            put(img, x, y0 + 1, col(iron, 4))
    return img


def crate_side(name, pal, rng):
    """Crate side: frame boards around horizontal planks, bronze corner brackets with nails."""
    r, b = ramp(pal), ramp("bronze")
    img = planks(r, rng, base=2.2)
    for x0 in (0, 13):
        rect(img, x0, 0, x0 + 2, 15, col(r, 2))
        rect(img, x0, 0, x0, 15, col(r, 3))
        rect(img, x0 + 2, 0, x0 + 2, 15, col(r, 0))
    for y0 in (0, 13):
        rect(img, 0, y0, 15, y0 + 2, col(r, 2))
        rect(img, 0, y0, 15, y0, col(r, 3))
        rect(img, 0, y0 + 2, 15, y0 + 2, col(r, 0))
    for x0, y0 in ((0, 0), (12, 0), (0, 12), (12, 12)):
        rect(img, x0, y0, x0 + 3, y0 + 3, col(b, 2))
        bevel(img, x0, y0, x0 + 3, y0 + 3, col(b, 4), col(b, 0))
        put(img, x0 + 1, y0 + 1, col(b, 4))
        put(img, x0 + 2, y0 + 2, col(b, 0))
    return img


def crate_top(name, pal, rng):
    """Crate lid: planks crossed by two bronze straps with nail heads."""
    r, b = ramp(pal), ramp("bronze")
    img = planks(r, rng, base=2.3)
    bevel(img, 0, 0, 15, 15, col(r, 3), col(r, 0))
    for x0 in (3, 11):
        rect(img, x0, 0, x0 + 1, 15, col(b, 2))
        rect(img, x0, 0, x0, 15, col(b, 3))
        rect(img, x0 + 2, 0, x0 + 2, 15, col(r, 0))
        for y in (2, 7, 12):
            put(img, x0, y, col(b, 4))
            put(img, x0 + 1, y, col(b, 1))
    return img


def board_front(name, pal, rng):
    """Slate chalk board in a wooden frame with chalk tally marks."""
    r, s = ramp(pal), ramp("deepslate")
    img = noisy_fill(16, 16, s, rng, -0.2, 1.0, cells=(4, 2))
    chalk = mix(WHITE, col(s, 4), 0.25)
    smudge = mix(col(s, 1), col(s, 3), 0.5)
    for k in range(4):
        rect(img, 3 + k * 2, 3, 3 + k * 2, 7, chalk)
    for k in range(9):
        put(img, 2 + k, 7 - k // 2, mix(chalk, col(s, 3), 0.25))
    for k in range(3):
        rect(img, 3 + k * 2, 9, 3 + k * 2, 12, chalk)
    rect(img, 10, 10, 12, 10, smudge)
    rect(img, 9, 12, 12, 12, smudge)
    for x0, y0, x1, y1 in ((0, 0, 15, 1), (0, 14, 15, 15), (0, 0, 1, 15), (14, 0, 15, 15)):
        rect(img, x0, y0, x1, y1, col(r, 2))
    bevel(img, 0, 0, 15, 15, col(r, 3), col(r, 0))
    bevel(img, 1, 1, 14, 14, col(r, 1), col(r, 3))
    rect(img, 4, 13, 7, 13, WHITE)
    return img


def chute(name, pal, rng):
    """Polished wooden slide with darker side rails and a long sheen."""
    r = ramp(pal)
    n = value_noise(16, 16, rng, 8)
    yy = np.arange(16)[:, None]
    vals = 2.5 + (n - 0.5) * 0.5 + 0.4 * (np.sin(yy * 0.9 + n * 5) > 0.8)
    img = shade_img(r, dither(vals - 0.5, 0.3).clip(1, 4))
    for x in range(5, 8):
        rect(img, x, 0, x, 15, col(r, 4) if x == 6 else col(r, 3))
    for x0 in (0, 14):
        rect(img, x0, 0, x0 + 1, 15, col(r, 1))
        rect(img, x0 + (1 if x0 == 0 else 0), 0, x0 + (1 if x0 == 0 else 0), 15, col(r, 0))
    for y in (3, 11):
        put(img, 0, y, col(ramp("bronze"), 4))
        put(img, 15, y, col(ramp("bronze"), 4))
    return img


def cork_board(name, pal, rng):
    """Cork board in a wood frame with three pinned paper cards."""
    r, paper, pin = ramp(pal), ramp("canvas"), ramp("red_paint")
    img = noisy_fill(16, 16, r, rng, 2.4, 3.8, cells=(2, 1), dith=1.0)
    for x0, y0, w, h, pc in ((2, 2, 5, 5, pin), (8, 3, 6, 4, ramp("electric_accent")), (4, 8, 7, 6, ramp("verdigris"))):
        rect(img, x0 + 1, y0 + 1, x0 + w, y0 + h, col(r, 0))
        rect(img, x0, y0, x0 + w - 1, y0 + h - 1, col(paper, 4))
        rect(img, x0, y0 + h - 1, x0 + w - 1, y0 + h - 1, col(paper, 2))
        for ly in range(y0 + 2, y0 + h - 1, 2):
            rect(img, x0 + 1, ly, x0 + w - 2 - (ly % 3 == 0), ly, col(paper, 1))
        put(img, x0 + w // 2, y0, col(pc, 3))
        put(img, x0 + w // 2, y0 + 1, col(pc, 1))
    bevel(img, 0, 0, 15, 15, col(r, 2), col(r, 0))
    rect(img, 1, 1, 14, 1, col(r, 1))
    return img


def winding_post(name, pal, rng):
    """Vertical wooden post with a bronze ratchet wheel and pawl."""
    r, b = ramp(pal), ramp("bronze")
    img = new(16, 16)
    for x in range(16):
        rect(img, x, 0, x, 15, col(r, (1, 2, 2, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 0)[x]))
    n = value_noise(16, 16, rng, 4)
    img[(n > 0.8)] = col(r, 1)
    yy, xx = np.mgrid[0:16, 0:16]
    ang = np.arctan2(yy - 7.5, xx - 7.5)
    d = np.hypot(xx - 7.5, yy - 7.5)
    teeth = d < 5.6 + 0.9 * ((np.floor((ang + np.pi) / (2 * np.pi) * 10) % 2) == 0)
    img[teeth] = col(b, 1)
    img[d < 5.0] = col(b, 2)
    img[(d < 5.0) & (xx + yy < 13)] = col(b, 3)
    img[d < 2.2] = col(b, 0)
    img[d < 1.2] = col(b, 4)
    rect(img, 12, 1, 13, 4, col(b, 2))
    put(img, 12, 1, col(b, 4))
    return img


def pulley_wheel(name, pal, rng):
    """Wooden sheave (round, transparent corners) with a groove ring and bronze hub."""
    r, b = ramp(pal), ramp("bronze")
    img = new(16, 16)
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5)
    disc = d < 7.9
    vals = 2.2 - 0.12 * ((xx - 7.5) + (yy - 7.5)) + (value_noise(16, 16, rng, 4) - 0.5) * 0.6
    img[disc] = shade_img(r, dither(vals - 0.5, 0.3).clip(1, 4))[disc]
    img[(d >= 6.3) & (d < 7.0)] = col(r, 0)
    for a in range(4):
        th = a * np.pi / 2 + np.pi / 4
        for k in np.arange(2.5, 6.0, 0.5):
            put(img, int(round(7.5 + np.cos(th) * k)), int(round(7.5 + np.sin(th) * k)), col(r, 1))
    img[d < 2.6] = col(b, 2)
    img[(d < 2.6) & (xx + yy < 14)] = col(b, 3)
    img[d < 1.0] = col(b, 0)
    put(img, 6, 6, col(b, 4))
    add_outline(img, col(r, 0))
    return img


def pulley_side(name, pal, rng):
    """Bronze bracket plate with bolt holes and a central bearing."""
    r = ramp(pal)
    img = plate(16, 16, r, rng)
    bevel(img, 0, 0, 15, 15, col(r, 4), col(r, 0))
    m = disc_mask(16, 16, 7.5, 7.5, 3.6)
    img[m] = col(r, 1)
    img[disc_mask(16, 16, 7.5, 7.5, 2.6)] = col(r, 3)
    img[disc_mask(16, 16, 7.5, 7.5, 1.2)] = col(r, 0)
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        put(img, x, y, col(r, 0))
        put(img, x + 1, y, col(r, 1))
        put(img, x, y - 1, col(r, 4))
    return img


def crucible_side(name, pal, rng):
    """Clay brick pot wall: courses of bricks under a smooth rim band."""
    r = ramp(pal)
    img = new(16, 16, col(r, 0))
    for row, y in enumerate(range(3, 16, 4)):
        off = (row % 2) * 4
        for x in range(-off, 16, 8):
            tone = int(rng.integers(1, 4))
            rect(img, x, y, x + 6, y + 2, col(r, tone))
            rect(img, x, y, x + 6, y, col(r, min(4, tone + 1)))
            put(img, x + int(rng.integers(1, 6)), y + 2, col(r, max(0, tone - 1)))
    rect(img, 0, 0, 15, 2, col(r, 3))
    rect(img, 0, 0, 15, 0, col(r, 4))
    rect(img, 0, 2, 15, 2, col(r, 1))
    soot = value_noise(16, 16, rng, 4) > 0.75
    img[soot & (np.arange(16)[:, None] > 10)] = col(r, 0)
    return img


def crucible_top(name, pal, rng):
    """Pot seen from above: thick clay rim around a dark sooty interior."""
    r = ramp(pal)
    yy, xx = np.mgrid[0:16, 0:16]
    img = noisy_fill(16, 16, r, rng, 2.0, 3.2)
    inner = (xx >= 3) & (xx <= 12) & (yy >= 3) & (yy <= 12)
    img[inner] = shade_img(ramp("slag"), dither(value_noise(16, 16, rng, 4) * 1.3 - 0.3, 0.4).clip(0, 1))[inner]
    bevel(img, 0, 0, 15, 15, col(r, 4), col(r, 1))
    bevel(img, 2, 2, 13, 13, col(r, 1), col(r, 4))
    return img


def molten(name, pal, rng, phase=0.0):
    """Glowing molten metal: bright swirling flow with darker cooling crust; phase (0..1) moves the swirl and drifts the crust."""
    r, hot = ramp(pal), ramp("#FFB347")
    n = fbm(16, 16, rng, (8, 4, 2))
    sw = np.sin((np.arange(16)[None, :] + np.arange(16)[:, None] * 0.5) * 0.8 + n * 6 + 2 * np.pi * phase)
    vals = 2.8 + 1.0 * sw + (n - 0.5)
    img = shade_img(hot, dither(vals - 0.5, 0.5).clip(1, 4))
    img[sw > 0.85] = mix(col(hot, 4), WHITE, 0.6)
    drift = int(round(phase * 16))
    crust = tile_shift(value_noise(16, 16, rng, 4), drift, drift)
    img[crust > 0.74] = col(r, 2)
    img[crust > 0.84] = col(r, 1)
    return img


def quern_side(name, pal, rng):
    """Two stacked millstones separated by a dark grind line, with chisel marks."""
    r = ramp(pal)
    img = noisy_fill(16, 16, r, rng, 1.5, 2.8, cells=(4, 2), dith=0.5)
    for y0, y1 in ((0, 6), (8, 15)):
        rect(img, 0, y0, 15, y0, col(r, 4))
        rect(img, 0, y1, 15, y1, col(r, 0))
        for x in range(1, 16, 3):
            put(img, x + (y0 // 8), y0 + 2 + int(rng.integers(0, 3)), col(r, 1))
    rect(img, 0, 7, 15, 7, col(r, 0))
    rect(img, 0, 6, 15, 6, col(r, 1))
    return img


def quern_top(name, pal, rng):
    """Millstone top: radial grooves around a central eye and an off-centre handle hole."""
    r = ramp(pal)
    img = noisy_fill(16, 16, r, rng, 1.8, 3.0, cells=(4, 2), dith=0.5)
    for a in range(8):
        th = a * np.pi / 4 + 0.2
        for k in np.arange(3.0, 7.5, 0.7):
            put(img, int(round(7.5 + np.cos(th) * k)), int(round(7.5 + np.sin(th) * k)), col(r, 1))
    img[disc_mask(16, 16, 7.5, 7.5, 2.2)] = col(r, 0)
    img[disc_mask(16, 16, 7.5, 7.5, 1.2)] = col(ramp("slag"), 0)
    put(img, 12, 11, col(r, 0))
    put(img, 12, 12, col(r, 0))
    put(img, 11, 11, col(r, 4))
    bevel(img, 0, 0, 15, 15, col(r, 3), col(r, 1))
    return img


def sorting_top(name, pal, rng):
    """Wooden chute seen from above with side rails and a bronze gate bar across it."""
    r, b = ramp(pal), ramp("bronze")
    img = planks(r, rng, base=2.4)
    img = np.ascontiguousarray(np.transpose(img, (1, 0, 2)))
    for x0 in (0, 13):
        rect(img, x0, 0, x0 + 2, 15, col(r, 1))
        rect(img, x0 + (2 if x0 == 0 else 0), 0, x0 + (2 if x0 == 0 else 0), 15, col(r, 0))
        rect(img, x0 + (0 if x0 == 0 else 2), 0, x0 + (0 if x0 == 0 else 2), 15, col(r, 3))
    rect(img, 3, 7, 12, 8, col(b, 2))
    rect(img, 3, 7, 12, 7, col(b, 4))
    rect(img, 3, 9, 12, 9, col(r, 0))
    put(img, 3, 7, col(b, 0))
    rivet(img, 1, 7, b)
    return img


STYLES = {
    "plank": plank, "beam": beam, "crate_side": crate_side, "crate_top": crate_top, "board_front": board_front,
    "chute": chute, "cork_board": cork_board, "winding_post": winding_post, "pulley_wheel": pulley_wheel,
    "pulley_side": pulley_side, "crucible_side": crucible_side, "crucible_top": crucible_top, "molten": molten,
    "quern_side": quern_side, "quern_top": quern_top, "sorting_top": sorting_top,
}

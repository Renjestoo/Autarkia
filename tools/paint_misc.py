"""Painters for OTHER_TEXTURES (cables, beams, hook, ghost) and the mod logo.

Cable and beam textures run along U (left to right) and tile seamlessly in that direction; rows
are the cross-section, lit from the top.
"""

import numpy as np
from PIL import Image, ImageDraw

from paint import (BLACK, WHITE, add_outline, col, dither, disc_mask, mix, new, ramp, relief, shade_img, sprite,
                   upscale, value_noise)

CROSS = np.array([0.6, 1.4, 2.2, 2.8, 3.2, 3.3, 3.2, 3.0, 2.8, 2.6, 2.3, 2.0, 1.7, 1.3, 0.9, 0.4])


def _cyl(rng, lo: float = 0.0, amp: float = 1.0) -> np.ndarray:
    """Return 16x16 float shade indices: cylinder cross-section shading plus U-tileable noise."""
    n = value_noise(16, 16, rng, 4)
    return lo + CROSS[:, None] * amp + (n - 0.5) * 0.5


def cable_rope(name, pal, rng):
    """Twisted three-strand hemp rope."""
    r = ramp(pal)
    yy, xx = np.mgrid[0:16, 0:16]
    phase = (xx + yy) % 5
    vals = _cyl(rng, 0.9, 0.85) + np.where(phase == 0, -1.1, 0.0) + np.where(phase == 1, 0.5, 0.0)
    img = shade_img(r, dither(vals - 0.5, 0.4))
    fibre = (rng.random((16, 16)) < 0.06) & (phase == 2) & (yy > 2) & (yy < 13)
    img[fibre] = col(r, 4)
    return img


def cable_metal(name, pal, rng):
    """Bare stranded wire rope: slanted strands with bright crowns and dark lays."""
    r = ramp(pal)
    yy, xx = np.mgrid[0:16, 0:16]
    phase = (xx + 2 * yy) % 4
    vals = _cyl(rng, 0.6, 0.85) + np.select([phase == 0, phase == 1, phase == 3], [-1.2, 0.7, -0.3], 0.0)
    img = shade_img(r, np.clip(np.rint(vals), 0, 4).astype(int))
    spark = (phase == 1) & (yy >= 3) & (yy <= 6)
    img[spark & (rng.random((16, 16)) < 0.3)] = col(r, 4)
    img[(phase == 1) & (yy == 4) & (xx % 8 == 1)] = WHITE
    return img


def cable_insulated(name, pal, rng):
    """Black rubber sheath with a soft highlight and a faint moulded stripe."""
    r = ramp(pal)
    vals = _cyl(rng, 0.2, 0.7)
    img = shade_img(r, dither(vals - 0.5, 0.5))
    img[4, :] = col(r, 4)
    img[4, ::4] = col(r, 3)
    img[10, :] = mix(col(r, 3), col(ramp("steel"), 1), 0.4)
    img[[0, 15], :] = col(r, 0)
    return img


def cable_hv(name, pal, rng):
    """Thick black HV cable with red marker bands."""
    r, red = ramp(pal), ramp("red_paint")
    vals = _cyl(rng, 0.2, 0.75)
    img = shade_img(r, dither(vals - 0.5, 0.5))
    band = shade_img(red, np.clip(np.rint(vals[:, :3] * 0.9 + 0.3), 0, 4).astype(int))
    img[:, 6:9] = band
    img[:, 5] = col(r, 0)
    img[:, 9] = col(r, 0)
    img[3, :] = col(r, 4)
    img[3, 6:9] = col(red, 4)
    img[[0, 15], :] = col(r, 0)
    return img


def cable_super(name, pal, rng):
    """Glowing violet superconductor core inside a frosted sheath with ice crystals."""
    r, frost = ramp(pal), ramp("frost")
    img = shade_img(frost, dither(_cyl(rng, 0.5, 0.9) - 0.5, 0.5))
    xx = np.arange(16)
    pulse = 0.5 + 0.5 * np.cos(xx * np.pi / 4)
    for y, base in ((5, 2.0), (6, 3.0), (7, 3.6), (8, 3.6), (9, 3.0), (10, 2.0)):
        for x, p in enumerate(pulse):
            img[y, x] = col(r, int(np.clip(round(base + p * 0.6 - 0.3), 0, 4)))
    img[7, ::8] = WHITE
    img[8, 4::8] = WHITE
    ice = (rng.random((16, 16)) < 0.12) & ((np.arange(16)[:, None] < 4) | (np.arange(16)[:, None] > 11))
    img[ice] = WHITE
    img[[0, 15], :] = col(frost, 0)
    return img


def beam(name, pal, rng):
    """Energy beam: white-hot centre fading to transparent edges, with a U-periodic shimmer."""
    r = ramp(pal)
    img = new(16, 16)
    xx = np.arange(16)
    shimmer = 0.5 + 0.5 * np.sin(xx * np.pi / 4)
    alpha = [40, 70, 110, 150, 190, 225, 250, 255, 255, 250, 225, 190, 150, 110, 70, 40]
    for y in range(16):
        d = abs(y - 7.5)
        for x in range(16):
            v = 4.3 - d * 0.55 + (shimmer[x] - 0.5) * 0.6
            if d < 1:
                c = WHITE
            else:
                c = col(r, int(np.clip(round(v), 1, 4)))
            img[y, x] = (c[0], c[1], c[2], int(np.clip(alpha[y] + (shimmer[x] - 0.5) * 50, 20, 255)))
    for x in range(0, 16, 4):
        y = 4 + int(rng.integers(0, 2)) * 7
        img[y, (x + int(rng.integers(0, 3))) % 16] = (*col(r, 4)[:3], 230)
    return img


def _draw32(fn) -> np.ndarray:
    im = Image.new("L", (32, 32), 0)
    fn(ImageDraw.Draw(im))
    return np.array(im) > 0


def hook(name, pal, rng):
    """32x32 grapple hook: eye ring, shaft, three barbed prongs with glowing teal tips."""
    r = ramp(pal)

    def shape(d):
        d.ellipse((11, 1, 20, 10), fill=255)
        d.rectangle((14, 9, 17, 23), fill=255)
        d.line([(15, 22), (9, 26), (6, 22), (5, 16)], fill=255, width=3)
        d.line([(16, 22), (22, 26), (25, 22), (26, 16)], fill=255, width=3)
        d.line([(15, 22), (15, 30)], fill=255, width=3)
        d.polygon([(4, 14), (8, 17), (4, 19)], fill=255)
        d.polygon([(27, 14), (23, 17), (27, 19)], fill=255)

    m = _draw32(shape)
    m &= ~disc_mask(32, 32, 15.5, 5.5, 2.2)
    img = sprite(m, r, base=2.1, k=0.55, cap=3, outline=True, gloss=3)
    glow = ramp("#A8F0DC")
    for x, y in ((4, 14), (5, 15), (27, 14), (26, 15), (15, 30), (16, 30)):
        img[y, x] = col(glow, 3)
    img[14, 4] = WHITE
    img[14, 27] = WHITE
    for y in range(11, 22, 3):
        img[y, 14] = col(r, 4)
        img[y + 1, 17] = col(r, 0)
    return img


def white(name, pal, rng):
    """Plain opaque white for tinted ghost previews (tinting needs an exact white base)."""
    return new(16, 16, WHITE)


OTHER = {"cable_rope": cable_rope, "cable_metal": cable_metal, "cable_insulated": cable_insulated, "cable_hv": cable_hv,
         "cable_super": cable_super, "beam": beam, "hook": hook, "white": white}

GLYPHS = {
    "A": [".XXX.", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X"],
    "U": ["X...X", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX."],
    "T": ["XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "..X.."],
    "R": ["XXXX.", "X...X", "X...X", "XXXX.", "X.X..", "X..X.", "X...X"],
    "K": ["X...X", "X..X.", "X.X..", "XX...", "X.X..", "X..X.", "X...X"],
    "I": ["XXX", ".X.", ".X.", ".X.", ".X.", ".X.", "XXX"],
}


def _vesica(cx: float, cy: float, angle: float, inner: float, outer: float, half: float) -> np.ndarray:
    """Return a 128x128 mask of a pointed leaf running radially from `inner` to `outer`."""
    length = outer - inner
    rad = (length * length / 4 + half * half) / (2 * half)
    mid = (inner + outer) / 2
    ux, uy = np.cos(angle), np.sin(angle)
    px, py = -uy, ux
    mx, my = cx + ux * mid, cy + uy * mid
    off = rad - half
    yy, xx = np.mgrid[0:128, 0:128] + 0.5
    a = (xx - (mx + px * off)) ** 2 + (yy - (my + py * off)) ** 2 <= rad * rad
    b = (xx - (mx - px * off)) ** 2 + (yy - (my - py * off)) ** 2 <= rad * rad
    return a & b


def logo() -> np.ndarray:
    """Return the 256x256 logo: bronze six-petal colony core with an amber orb over a brass gear, AUTARKIA below."""
    bronze, brass, amber = ramp("bronze"), ramp("brass"), ramp("#FFB347")
    cx, cy = 64.0, 52.0
    img = new(128, 128)
    yy, xx = np.mgrid[0:128, 0:128] + 0.5
    rr = np.hypot(xx - cx, yy - cy)
    th = np.arctan2(yy - cy, xx - cx)
    teeth = ((th / (2 * np.pi) * 12 + 0.25) % 1.0) < 0.5
    gear = ((rr <= 39) | ((rr <= 46) & teeth)) & (rr >= 27)
    for k in range(6):
        a = k * np.pi / 3
        spoke = (np.abs(-(xx - cx) * np.sin(a) + (yy - cy) * np.cos(a)) < 2.5) & (rr < 30)
        gear |= spoke
    gv = relief(gear, base=1.6, k=0.45, cap=3)
    gimg = shade_img(brass, dither(gv - 0.5, 0.15), gear)
    holes = np.zeros_like(gear)
    for k in range(12):
        a = (k + 0.5) * np.pi / 6
        holes |= disc_mask(128, 128, cx + np.cos(a) * 33 - 0.5, cy + np.sin(a) * 33 - 0.5, 1.6)
    gimg[holes & gear] = col(brass, 0)
    img[gear] = gimg[gear]
    for k in range(6):
        a = -np.pi / 2 + k * np.pi / 3
        pm = _vesica(cx, cy, a, 5.0, 36.0, 9.0)
        tone = 0.45 * (-np.cos(a + np.pi / 4))
        pv = relief(pm, base=2.4 + tone, k=0.5, cap=3)
        ux, uy = np.cos(a), np.sin(a)
        along = (xx - cx) * ux + (yy - cy) * uy
        across = -(xx - cx) * uy + (yy - cy) * ux
        pv = pv - 0.9 * ((np.abs(across) < 0.8) & (along > 12) & (along < 31))
        pv = pv + 0.6 * ((across < -2.5) & (across > -4.5) & (along > 14) & (along < 28))
        pimg = shade_img(bronze, dither(pv - 0.5, 0.35), pm)
        edge = pm & ~np.pad(pm, 1)[2:, 1:-1]
        pimg[edge] = col(bronze, 0)
        img[pm] = pimg[pm]
    halo = (rr < 15) & (rr >= 11.5) & (((xx.astype(int) + yy.astype(int)) % 2) == 0)
    img[halo] = col(amber, 3)
    orb = rr < 11.5
    ov = 3.6 - rr / 11.5 * 2.4 - ((xx - cx) + (yy - cy)) * 0.05
    oimg = shade_img(amber, dither(ov - 0.5, 0.5), orb)
    img[orb] = oimg[orb]
    img[disc_mask(128, 128, cx - 4, cy - 4, 2.6)] = mix(col(amber, 4), WHITE, 0.6)
    img[disc_mask(128, 128, cx - 4, cy - 4, 1.2)] = WHITE
    add_outline(img, mix(col(bronze, 0), BLACK, 0.6))
    word = "AUTARKIA"
    widths = [len(GLYPHS[c][0]) for c in word]
    total = sum(w * 2 + 2 for w in widths) - 2
    x0, y0 = (128 - total) // 2, 104
    text = np.zeros((128, 128), dtype=bool)
    x = x0
    for c, w in zip(word, widths):
        for gy, row in enumerate(GLYPHS[c]):
            for gx, ch in enumerate(row):
                if ch == "X":
                    text[y0 + gy * 2:y0 + gy * 2 + 2, x + gx * 2:x + gx * 2 + 2] = True
        x += w * 2 + 2
    shadow = np.zeros_like(text)
    shadow[1:, 1:] = text[:-1, :-1]
    img[shadow & ~text] = mix(col(bronze, 0), BLACK, 0.5)
    tv = 4.2 - (yy - y0) / 14.0 * 2.6
    timg = shade_img(bronze, dither(tv - 0.5, 0.3), text)
    img[text] = timg[text]
    add_outline(img, mix(col(bronze, 0), BLACK, 0.7))
    return upscale(img, 2)

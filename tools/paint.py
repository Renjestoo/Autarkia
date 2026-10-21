"""Palette-driven pixel-art primitives shared by every Autarkia texture painter.

Images are numpy arrays of shape (h, w, 4), dtype uint8, RGBA. Every random choice comes from
`rng_for(...)`, which seeds a numpy Generator from a SHA-256 of the given keys, so all output is
byte-identical between runs.
"""

import hashlib
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

from art_spec import PALETTES

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "generated" / "resources" / "assets" / "autarkia"
TEXTURES = ASSETS / "textures"

BLACK = (0, 0, 0, 255)
WHITE = (255, 255, 255, 255)
CLEAR = (0, 0, 0, 0)

BAYER4 = (np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]], dtype=np.float64) + 0.5) / 16.0 - 0.5


def hex_rgb(code: str) -> tuple[int, int, int]:
    """Parse '#RRGGBB'."""
    code = code.lstrip("#")
    return int(code[0:2], 16), int(code[2:4], 16), int(code[4:6], 16)


def ramp(name: str) -> np.ndarray:
    """Return a (5, 3) int array, dark to light, for a PALETTES name or a '#RRGGBB' colour.

    A hex colour expands to a ramp: 45 %, 70 %, 100 % of the colour, then 35 % and 70 % towards white.
    """
    if name.startswith("#"):
        c = np.array(hex_rgb(name), dtype=np.float64)
        rows = [c * 0.45, c * 0.7, c, c + (255 - c) * 0.35, c + (255 - c) * 0.7]
        return np.clip(np.rint(rows), 0, 255).astype(np.int64)
    if name not in PALETTES:
        raise KeyError(f"unknown palette '{name}'; add it to tools/art_spec.py PALETTES")
    return np.array([hex_rgb(h) for h in PALETTES[name]], dtype=np.int64)


def col(r: np.ndarray, i: int, a: int = 255) -> tuple[int, int, int, int]:
    """Return shade i (0..4) of ramp r as an RGBA tuple."""
    c = r[max(0, min(4, i))]
    return int(c[0]), int(c[1]), int(c[2]), a


def mix(c1, c2, t: float) -> tuple[int, int, int, int]:
    """Blend two RGB(A) colours; t=0 gives c1."""
    a = list(c1) + [255] * (4 - len(c1))
    b = list(c2) + [255] * (4 - len(c2))
    return tuple(int(round(a[k] + (b[k] - a[k]) * t)) for k in range(4))


def outline_col(r: np.ndarray) -> tuple[int, int, int, int]:
    """Return the outline colour of a ramp: its darkest shade pushed halfway to black."""
    return mix(col(r, 0), BLACK, 0.45)


def rng_for(*keys) -> np.random.Generator:
    """Return a Generator seeded from the SHA-256 of the joined keys."""
    digest = hashlib.sha256("|".join(str(k) for k in keys).encode("utf-8")).digest()
    return np.random.default_rng(int.from_bytes(digest[:8], "little"))


def new(w: int, h: int, fill=CLEAR) -> np.ndarray:
    """Return a new RGBA canvas."""
    img = np.zeros((h, w, 4), dtype=np.uint8)
    img[:, :] = fill
    return img


def put(img: np.ndarray, x: int, y: int, c) -> None:
    """Set one pixel if it lies inside the canvas."""
    if 0 <= y < img.shape[0] and 0 <= x < img.shape[1]:
        img[y, x] = c


def rect(img: np.ndarray, x0: int, y0: int, x1: int, y1: int, c) -> None:
    """Fill the inclusive rectangle (x0, y0)-(x1, y1), clipped to the canvas."""
    h, w = img.shape[:2]
    xa, xb = max(0, min(x0, x1)), min(w - 1, max(x0, x1))
    ya, yb = max(0, min(y0, y1)), min(h - 1, max(y0, y1))
    if xa <= xb and ya <= yb:
        img[ya:yb + 1, xa:xb + 1] = c


def line(img: np.ndarray, x0: int, y0: int, x1: int, y1: int, c) -> None:
    """Draw a 1px Bresenham line."""
    for x, y in line_pts(x0, y0, x1, y1):
        put(img, x, y, c)


def line_pts(x0: int, y0: int, x1: int, y1: int) -> list[tuple[int, int]]:
    """Return the Bresenham pixels from (x0, y0) to (x1, y1)."""
    pts = []
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        pts.append((x0, y0))
        if x0 == x1 and y0 == y1:
            return pts
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def poly_mask(w: int, h: int, pts) -> np.ndarray:
    """Return a bool mask of a filled, non-antialiased polygon."""
    im = Image.new("L", (w, h), 0)
    ImageDraw.Draw(im).polygon([tuple(p) for p in pts], fill=255, outline=255)
    return np.array(im) > 0


def ellipse_mask(w: int, h: int, x0: float, y0: float, x1: float, y1: float) -> np.ndarray:
    """Return a bool mask of the ellipse inscribed in the inclusive pixel box (x0, y0)-(x1, y1)."""
    yy, xx = np.mgrid[0:h, 0:w]
    cx, cy = (x0 + x1) / 2.0, (y0 + y1) / 2.0
    rx, ry = (x1 - x0 + 1) / 2.0, (y1 - y0 + 1) / 2.0
    return ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2 <= 1.0


def disc_mask(w: int, h: int, cx: float, cy: float, r: float) -> np.ndarray:
    """Return a bool mask of pixels whose centres lie within r of (cx, cy)."""
    yy, xx = np.mgrid[0:h, 0:w]
    return (xx - cx) ** 2 + (yy - cy) ** 2 <= r * r


def fill(img: np.ndarray, mask: np.ndarray, c) -> None:
    """Paint colour c into every masked pixel."""
    img[mask] = c


def value_noise(w: int, h: int, rng: np.random.Generator, cell: int, tile: bool = True) -> np.ndarray:
    """Return smooth value noise in [0, 1] with lattice spacing `cell` pixels; tileable when tile=True."""
    gw, gh = max(1, -(-w // cell)), max(1, -(-h // cell))
    grid = rng.random((gh + 1, gw + 1))
    if tile:
        grid[-1, :] = grid[0, :]
        grid[:, -1] = grid[:, 0]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    fx, fy = xx / cell, yy / cell
    ix, iy = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - ix, fy - iy
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    ix1, iy1 = np.minimum(ix + 1, gw), np.minimum(iy + 1, gh)
    a = grid[iy, ix] * (1 - tx) + grid[iy, ix1] * tx
    b = grid[iy1, ix] * (1 - tx) + grid[iy1, ix1] * tx
    return a * (1 - ty) + b * ty


def fbm(w: int, h: int, rng: np.random.Generator, cells=(8, 4, 2), weights=(0.5, 0.3, 0.2)) -> np.ndarray:
    """Return normalised multi-octave value noise in [0, 1]."""
    acc = np.zeros((h, w))
    for c, wt in zip(cells, weights):
        acc += value_noise(w, h, rng, c) * wt
    lo, hi = acc.min(), acc.max()
    return (acc - lo) / (hi - lo) if hi > lo else acc * 0 + 0.5


def dither(values: np.ndarray, strength: float = 1.0) -> np.ndarray:
    """Round float shade indices to ints with 4x4 ordered dithering."""
    h, w = values.shape
    tiles = np.tile(BAYER4, (-(-h // 4), -(-w // 4)))[:h, :w]
    return np.floor(values + 0.5 + tiles * strength).astype(int)


def shade_img(r: np.ndarray, idx: np.ndarray, mask: np.ndarray | None = None, alpha: int = 255) -> np.ndarray:
    """Map an int shade-index array (clipped to 0..4) through ramp r into a new RGBA image."""
    h, w = idx.shape
    out = new(w, h)
    rgb = r[np.clip(idx, 0, 4)]
    out[..., :3] = rgb
    out[..., 3] = alpha
    if mask is not None:
        out[~mask] = CLEAR
    return out


def paint_idx(img: np.ndarray, r: np.ndarray, idx: np.ndarray, mask: np.ndarray) -> None:
    """Paint shade indices from ramp r into img wherever mask is set."""
    rgb = r[np.clip(idx, 0, 4)]
    img[mask, :3] = rgb[mask]
    img[mask, 3] = 255


def depth_map(mask: np.ndarray, cap: int = 3) -> np.ndarray:
    """Return per-pixel chebyshev distance to the mask edge (1 at the edge), capped."""
    d = np.zeros(mask.shape, dtype=np.int64)
    cur = mask.copy()
    for k in range(1, cap + 1):
        d[cur] = k
        p = np.pad(cur, 1, constant_values=False)
        cur = cur & p[:-2, 1:-1] & p[2:, 1:-1] & p[1:-1, :-2] & p[1:-1, 2:] & p[:-2, :-2] & p[2:, 2:] & p[:-2, 2:] & p[2:, :-2]
        if not cur.any():
            break
    return d


def relief(mask: np.ndarray, base: float = 2.0, k: float = 0.6, cap: int = 2) -> np.ndarray:
    """Return float shade indices lighting the mask from the top-left like a raised, beveled shape."""
    d = depth_map(mask, cap).astype(np.float64)
    p = np.pad(d, 1)
    tl, br = p[:-2, :-2], p[2:, 2:]
    return base + k * (br - tl)


def sprite(mask: np.ndarray, r: np.ndarray, rng: np.random.Generator | None = None, base: float = 2.0,
           noise: float = 0.0, k: float = 0.6, cap: int = 2, outline: bool = True, gloss: int = 1) -> np.ndarray:
    """Shade a sprite silhouette with top-left lighting, optional speckle and a 1px dark outline.

    Args:
        gloss: number of brightest top-left pixels promoted to shade 4.
    """
    h, w = mask.shape
    vals = relief(mask, base, k, cap)
    if rng is not None and noise > 0:
        vals = vals + (rng.random((h, w)) - 0.5) * 2 * noise
    idx = np.clip(np.rint(vals).astype(int), 0, 3 if gloss else 4)
    if gloss:
        score = np.where(mask, vals - (np.arange(w)[None, :] + np.arange(h)[:, None]) * 0.02, -99)
        order = np.argsort(score, axis=None)[::-1][:gloss]
        for o in order:
            idx[o // w, o % w] = 4
    out = shade_img(r, idx, mask)
    if outline:
        add_outline(out, outline_col(r))
    return out


def add_outline(img: np.ndarray, c, diagonal: bool = False) -> None:
    """Paint c into transparent pixels that touch an opaque pixel (4-neighbourhood, or 8 when diagonal)."""
    a = img[..., 3] > 0
    p = np.pad(a, 1)
    n = p[:-2, 1:-1] | p[2:, 1:-1] | p[1:-1, :-2] | p[1:-1, 2:]
    if diagonal:
        n |= p[:-2, :-2] | p[2:, 2:] | p[:-2, 2:] | p[2:, :-2]
    img[n & ~a] = c


def edge_mask(mask: np.ndarray) -> np.ndarray:
    """Return the mask pixels that touch a non-mask pixel or the border (4-neighbourhood)."""
    p = np.pad(mask, 1, constant_values=False)
    inner = p[:-2, 1:-1] & p[2:, 1:-1] & p[1:-1, :-2] & p[1:-1, 2:]
    return mask & ~inner


def bevel(img: np.ndarray, x0: int, y0: int, x1: int, y1: int, light, dark, corner=None) -> None:
    """Draw a 1px bevel frame: light top and left edges, dark bottom and right edges."""
    rect(img, x0, y0, x1, y0, light)
    rect(img, x0, y0, x0, y1, light)
    rect(img, x0, y1, x1, y1, dark)
    rect(img, x1, y0, x1, y1, dark)
    if corner is not None:
        put(img, x1, y0, corner)
        put(img, x0, y1, corner)


def rivet(img: np.ndarray, x: int, y: int, r: np.ndarray, big: bool = False) -> None:
    """Draw a rivet head at (x, y): bright top-left pixel with a dark drop shadow; 2x2 when big."""
    if big:
        put(img, x, y, col(r, 4))
        put(img, x + 1, y, col(r, 3))
        put(img, x, y + 1, col(r, 3))
        put(img, x + 1, y + 1, col(r, 1))
        put(img, x + 2, y + 1, col(r, 0))
        put(img, x + 1, y + 2, col(r, 0))
        put(img, x + 2, y + 2, col(r, 0))
    else:
        put(img, x, y, col(r, 4))
        put(img, x + 1, y + 1, col(r, 0))


def speckle(img: np.ndarray, rng: np.random.Generator, mask: np.ndarray, c, density: float) -> None:
    """Scatter colour c onto a random fraction of the masked pixels."""
    hit = mask & (rng.random(mask.shape) < density)
    img[hit] = c


def noisy_fill(w: int, h: int, r: np.ndarray, rng: np.random.Generator, lo: float = 1.4, hi: float = 2.6,
               cells=(4, 2), weights=(0.6, 0.4), dith: float = 0.45) -> np.ndarray:
    """Return a w x h image of dithered value noise mapped onto ramp indices lo..hi."""
    n = fbm(w, h, rng, cells, weights)
    return shade_img(r, dither(lo + n * (hi - lo) - 0.5, dith))


def plate(w: int, h: int, r: np.ndarray, rng: np.random.Generator, base: float = 2.0, rivets: bool = False,
          spread: float = 0.35) -> np.ndarray:
    """Return a beveled metal plate: calm base shade, soft top-lit gradient, sparse scratches, optional rivets."""
    n = fbm(w, h, rng, (6, 3), (0.7, 0.3))
    grad = np.linspace(0.25, -0.25, h)[:, None] if h > 1 else np.zeros((1, 1))
    vals = base + (n - 0.5) * 2 * spread + grad
    idx = dither(vals - 0.5, 0.35)
    spots = rng.random((h, w))
    idx = idx + (spots < 0.035) - ((spots > 0.97) & (idx > 0))
    img = shade_img(r, idx)
    if w >= 3 and h >= 3:
        bevel(img, 0, 0, w - 1, h - 1, col(r, int(base) + 1), col(r, int(base) - 1))
    if rivets and w >= 6 and h >= 6:
        for x, y in ((1, 1), (w - 3, 1), (1, h - 3), (w - 3, h - 3)):
            rivet(img, x, y, r)
    return img


def tile_shift(img: np.ndarray, dx: int, dy: int) -> np.ndarray:
    """Return img rolled by (dx, dy) with wrap-around."""
    return np.roll(np.roll(img, dy, axis=0), dx, axis=1)


def over(dst: np.ndarray, src: np.ndarray, x: int = 0, y: int = 0) -> None:
    """Composite src onto dst at (x, y) using src alpha as a hard cutout (alpha > 0 wins)."""
    h, w = src.shape[:2]
    H, W = dst.shape[:2]
    xa, ya, xb, yb = max(0, x), max(0, y), min(W, x + w), min(H, y + h)
    if xa >= xb or ya >= yb:
        return
    s = src[ya - y:yb - y, xa - x:xb - x]
    m = s[..., 3] > 0
    dst[ya:yb, xa:xb][m] = s[m]


def to_image(img: np.ndarray) -> Image.Image:
    """Wrap an RGBA array as a PIL image."""
    if img.dtype != np.uint8 or img.ndim != 3 or img.shape[2] != 4:
        raise ValueError(f"expected an RGBA uint8 array, got {img.dtype} {img.shape}")
    return Image.fromarray(np.ascontiguousarray(img))


def save_png(img, path: Path) -> Path:
    """Write an RGBA array or PIL image as PNG (deterministic bytes) and return the path."""
    path.parent.mkdir(parents=True, exist_ok=True)
    im = img if isinstance(img, Image.Image) else to_image(img)
    im.save(path, format="PNG", optimize=False)
    return path


def upscale(img: np.ndarray, k: int) -> np.ndarray:
    """Return a nearest-neighbour enlargement by integer factor k."""
    return np.repeat(np.repeat(img, k, axis=0), k, axis=1)

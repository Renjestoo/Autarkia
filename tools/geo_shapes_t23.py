"""Shape helpers for the tier 2 and tier 3 block models: pixel-mask extrusion, rings, radial parts, lattices."""

import math

import numpy as np

from geo_core import Model


def circle_mask(d: int, inner: float = 0.0) -> np.ndarray:
    """Return a d x d bool mask of a pixel disc, with a hole of diameter `inner` when given."""
    c = (d - 1) / 2.0
    yy, xx = np.mgrid[0:d, 0:d]
    r2 = (xx - c) ** 2 + (yy - c) ** 2
    m = r2 <= (d / 2.0) ** 2 - 0.25 * d / 8.0
    if inner > 0:
        m &= r2 > (inner / 2.0) ** 2
    return m


def rects(mask: np.ndarray) -> list[tuple[int, int, int, int]]:
    """Decompose a bool mask into non-overlapping rectangles (r0, r1, c0, c1), end-exclusive."""
    out, active = [], {}
    h = mask.shape[0]
    for r in range(h + 1):
        runs = set()
        if r < h:
            row = mask[r]
            c = 0
            while c < row.size:
                if row[c]:
                    s = c
                    while c < row.size and row[c]:
                        c += 1
                    runs.add((s, c))
                else:
                    c += 1
        for key in list(active):
            if key not in runs:
                out.append((active.pop(key), r, key[0], key[1]))
        for key in runs:
            active.setdefault(key, r)
    return sorted(out)


def extrude(m: Model, bone: str, mask: np.ndarray, axis: str, center, depth: int, mat: str, faces=None) -> None:
    """Extrude a pixel mask into boxes; the mask is centred on `center` and `depth` thick along `axis`.

    Args:
        mask: rows run top to bottom (y down) for axes x and z; for axis y rows run along +z.
        axis: 'z' maps columns to x, 'x' maps columns to z, 'y' maps columns to x and rows to z.
    """
    h, w = mask.shape
    cx, cy, cz = center
    for r0, r1, c0, c1 in rects(mask):
        if axis == "z":
            o = (cx - w / 2.0 + c0, cy + h / 2.0 - r1, cz - depth / 2.0)
            s = (c1 - c0, r1 - r0, depth)
        elif axis == "x":
            o = (cx - depth / 2.0, cy + h / 2.0 - r1, cz - w / 2.0 + c0)
            s = (depth, r1 - r0, c1 - c0)
        else:
            o = (cx - w / 2.0 + c0, cy - depth / 2.0, cz - h / 2.0 + r0)
            s = (c1 - c0, depth, r1 - r0)
        m.box(bone, o, s, mat, faces=faces)


def radial(m: Model, bone: str, center, axis: str, n: int, size, r_mid: float, mat: str, phase: float = 0.0,
           faces=None) -> None:
    """Place n copies of a box around `axis` through `center`, each rotated by 360/n degrees.

    Args:
        size: world-axis size of the copy at angle 0, which sits at +Y (axes x, z) or +Z (axis y) by r_mid.
    """
    cx, cy, cz = center
    sx, sy, sz = size
    for k in range(n):
        a = phase + 360.0 * k / n
        if axis == "y":
            o = (cx - sx / 2.0, cy - sy / 2.0, cz + r_mid - sz / 2.0)
            rot = (0, a, 0)
        else:
            o = (cx - sx / 2.0, cy + r_mid - sy / 2.0, cz - sz / 2.0)
            rot = (a, 0, 0) if axis == "x" else (0, 0, a)
        m.box(bone, o, size, mat, faces=faces, rotation=rot, pivot=center)


def ring_box(m: Model, bone: str, center, outer, inner, height: int, mat: str) -> None:
    """Add a square frame (four walls) lying flat, outer/inner are (x, z) sizes, bottom at center y."""
    cx, y, cz = center
    ox, oz = outer
    ix, iz = inner
    t = (ox - ix) / 2.0
    m.box(bone, (cx - ox / 2.0, y, cz - oz / 2.0), (ox, height, (oz - iz) / 2.0), mat)
    m.box(bone, (cx - ox / 2.0, y, cz + iz / 2.0), (ox, height, (oz - iz) / 2.0), mat)
    m.box(bone, (cx - ox / 2.0, y, cz - iz / 2.0), (t, height, iz), mat)
    m.box(bone, (cx + ix / 2.0, y, cz - iz / 2.0), (t, height, iz), mat)


def strut(m: Model, bone: str, p0, p1, thick: int, mat: str) -> None:
    """Add a thin square beam between two points lying in an axis plane (x, y or z constant)."""
    p0, p1 = np.array(p0, float), np.array(p1, float)
    d = p1 - p0
    length = int(round(float(np.linalg.norm(d))))
    mid = (p0 + p1) / 2.0
    if abs(d[2]) < 1e-6:
        ang = math.degrees(math.atan2(d[1], -d[0]))
        o = (mid[0] - length / 2.0, mid[1] - thick / 2.0, mid[2] - thick / 2.0)
        m.box(bone, o, (length, thick, thick), mat, rotation=(0, 0, ang), pivot=tuple(mid))
    elif abs(d[0]) < 1e-6:
        ang = math.degrees(math.atan2(d[1], d[2]))
        o = (mid[0] - thick / 2.0, mid[1] - thick / 2.0, mid[2] - length / 2.0)
        m.box(bone, o, (thick, thick, length), mat, rotation=(ang, 0, 0), pivot=tuple(mid))
    else:
        raise ValueError(f"strut {tuple(p0)}->{tuple(p1)} must lie in an x or z plane")


def lattice_mast(m: Model, bone: str, y0: float, y1: float, half: float, panel: float, mat: str, brace: str,
                 post: int = 2) -> None:
    """Add a square lattice mast: corner posts, horizontal rings and zig-zag face braces."""
    h = y1 - y0
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.box(bone, (sx * half - post / 2.0, y0, sz * half - post / 2.0), (post, int(round(h)), post), mat)
    n = max(1, int(round(h / panel)))
    step = h / n
    for k in range(n + 1):
        y = y0 + k * step
        if k in (0, n):
            continue
        for sz in (-1, 1):
            m.box(bone, (-half + post / 2.0, y - 0.5, sz * half - 0.5), (int(round(2 * half - post)), 1, 1), brace)
        for sx in (-1, 1):
            m.box(bone, (sx * half - 0.5, y - 0.5, -half + post / 2.0), (1, 1, int(round(2 * half - post))), brace)
    for k in range(n):
        ya, yb = y0 + k * step, y0 + (k + 1) * step
        flip = 1 if k % 2 == 0 else -1
        for sz in (-1, 1):
            strut(m, bone, (-half * flip, ya, sz * half), (half * flip, yb, sz * half), 1, brace)
        for sx in (-1, 1):
            strut(m, bone, (sx * half, ya, -half * flip), (sx * half, yb, half * flip), 1, brace)

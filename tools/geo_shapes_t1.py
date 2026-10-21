"""Composite shape helpers for block models: rings, cut-out disc faces, radial boxes, struts and balls.

All positions are geo file coordinates. Rotation signs already account for GeckoLib's X mirror, so a
helper placed at angle theta (counter-clockwise from +X in the file frame) lands there in game too.
"""

import math

from geo_core import Model


def ring(m: Model, bone: str, c, r: float, axis: str, depth: int, radial: int, n: int, mat: str,
         start: float = 0.0, seg_len: int | None = None) -> None:
    """Add n tangential segments forming a ring of outer radius r around axis 'x', 'y' or 'z' through c.

    Args:
        depth: thickness along the axis; radial: thickness across the rim.
        seg_len: segment length; default closes the ring at the outer radius.
    """
    length = seg_len or int(math.floor(2 * r * math.tan(math.pi / n) + 0.75))
    rho = r - radial / 2.0
    for k in range(n):
        th = start + 360.0 * k / n
        t = math.radians(th)
        if axis == "z":
            ctr = (c[0] + rho * math.cos(t), c[1] + rho * math.sin(t), c[2])
            size, rot = (length, radial, depth), (0, 0, 90 - th)
        elif axis == "x":
            ctr = (c[0], c[1] + rho * math.sin(t), c[2] + rho * math.cos(t))
            size, rot = (depth, radial, length), (th + 90, 0, 0)
        else:
            ctr = (c[0] + rho * math.cos(t), c[1], c[2] + rho * math.sin(t))
            size, rot = (length, depth, radial), (0, 90 - th, 0)
        origin = tuple(ctr[i] - size[i] / 2.0 for i in range(3))
        m.box(bone, origin, size, mat, rotation=rot, pivot=ctr)


def plane(m: Model, bone: str, c, w: int, h: int, axis: str, mat: str) -> None:
    """Add a zero-thickness w x h plane centred on c, perpendicular to axis (x: w along z; y: w along x, h along z)."""
    if axis == "z":
        m.box(bone, (c[0] - w / 2.0, c[1] - h / 2.0, c[2]), (w, h, 0), mat)
    elif axis == "x":
        m.box(bone, (c[0], c[1] - h / 2.0, c[2] - w / 2.0), (0, h, w), mat)
    else:
        m.box(bone, (c[0] - w / 2.0, c[1], c[2] - h / 2.0), (w, 0, h), mat)


def disc_faces(m: Model, bone: str, c, d: int, axis: str, depth: float, mat: str, gap: float = 0.1) -> None:
    """Add two d x d cut-out face planes capping a cylinder of the given depth along axis through c."""
    i = "xyz".index(axis)
    for s in (-1, 1):
        p = list(c)
        p[i] += s * (depth / 2.0 + gap)
        plane(m, bone, p, d, d, axis, mat)


def radial(m: Model, bone: str, c, axis: str, theta: float, dist: float, size, mat: str, **kw) -> None:
    """Add a box whose first long dimension points radially at angle theta, centred dist from c.

    Args:
        size: for axis 'y' and 'z' the length runs along X of `size`; for axis 'x' along Z.
    """
    t = math.radians(theta)
    if axis == "y":
        ctr, rot = (c[0] + dist * math.cos(t), c[1] + size[1] / 2.0, c[2] + dist * math.sin(t)), (0, -theta, 0)
    elif axis == "z":
        ctr, rot = (c[0] + dist * math.cos(t), c[1] + dist * math.sin(t), c[2]), (0, 0, -theta)
    else:
        ctr, rot = (c[0], c[1] + dist * math.sin(t), c[2] + dist * math.cos(t)), (theta, 0, 0)
    origin = tuple(ctr[k] - size[k] / 2.0 for k in range(3))
    m.box(bone, origin, size, mat, rotation=rot, pivot=ctr, **kw)


def strut(m: Model, bone: str, p0, p1, axis: str, at: float, width: int, depth: int, mat: str, **kw) -> None:
    """Add a beam between two points of the plane perpendicular to axis ('z': points are (x, y); 'x': (z, y)).

    Args:
        at: the plane coordinate along axis where the beam is centred.
    """
    du, dv = p1[0] - p0[0], p1[1] - p0[1]
    length = int(round(math.hypot(du, dv)))
    mu, mv = (p0[0] + p1[0]) / 2.0, (p0[1] + p1[1]) / 2.0
    theta = math.degrees(math.atan2(dv, du))
    if axis == "z":
        radial(m, bone, (mu, mv, at), "z", theta, 0, (length, width, depth), mat, **kw)
    else:
        radial(m, bone, (at, mv, mu), "x", theta, 0, (depth, width, length), mat, **kw)


def ball(m: Model, bone: str, c, d: int, mat: str) -> None:
    """Add a stepped sphere of diameter d centred on c from four overlapping boxes."""
    b = d - 2 * max(1, round(d / 6))
    for size in ((d, b, b), (b, d, b), (b, b, d), (d - 2, d - 2, d - 2)):
        m.box(bone, tuple(c[i] - size[i] / 2.0 for i in range(3)), size, mat)

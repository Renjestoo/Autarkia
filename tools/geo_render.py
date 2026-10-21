"""Software rasterizer for GeckoLib models built with geo_core: orthographic views, per-face affine sampling.

Geometry follows GeckoLib 5.5.7 exactly: render x = -file x, bone and cube rotations negate X and Y,
rotation order Z * Y * X about the pivot, animation rotations add to the base rotation, animation
X translation is negated, animation scale applies about the pivot after rotation. Faces are drawn
back to front (painter's algorithm) with a depth test so interpenetrating cubes resolve correctly.
"""

import math

import numpy as np

from geo_core import collect_faces


def _rot(rx: float, ry: float, rz: float) -> np.ndarray:
    """Return Rz @ Ry @ Rx for angles in radians (render-space conventions)."""
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    mx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    my = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    mz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return mz @ my @ mx


def _affine(m3: np.ndarray, t: np.ndarray) -> np.ndarray:
    """Return a 4x4 matrix from a 3x3 linear part and a translation."""
    m = np.eye(4)
    m[:3, :3] = m3
    m[:3, 3] = t
    return m


def _trans(t) -> np.ndarray:
    m = np.eye(4)
    m[:3, 3] = t
    return m


def bone_matrices(model, pose: dict | None = None) -> dict[str, np.ndarray]:
    """Return render-space 4x4 world matrices per bone for an optional pose {bone: {channel: xyz}}."""
    pose = pose or {}
    mats = {}
    for b in model.bones:
        p = pose.get(b["name"], {})
        base = b["rotation"] or (0.0, 0.0, 0.0)
        ar = p.get("rotation", (0.0, 0.0, 0.0))
        ap = p.get("position", (0.0, 0.0, 0.0))
        sc = p.get("scale", (1.0, 1.0, 1.0))
        rx, ry, rz = (math.radians(-(base[0] + ar[0])), math.radians(-(base[1] + ar[1])), math.radians(base[2] + ar[2]))
        piv = np.array([-b["pivot"][0], b["pivot"][1], b["pivot"][2]])
        local = (_trans((-ap[0], ap[1], ap[2])) @ _trans(piv) @ _affine(_rot(rx, ry, rz), np.zeros(3))
                 @ _affine(np.diag(sc), np.zeros(3)) @ _trans(-piv))
        parent = mats[b["parent"]] if b["parent"] else np.eye(4)
        mats[b["name"]] = parent @ local
    return mats


def _hidden_bones(model, hidden: set[str]) -> set[str]:
    """Expand a set of hidden bones to include all their descendants."""
    out = set(hidden)
    for b in model.bones:
        if b["parent"] in out:
            out.add(b["name"])
    return out


def face_quads(model, atlas_shape, uvs: dict, pose: dict | None = None, hidden: set[str] = frozenset()):
    """Return a list of (corners 4x3 render space, uv 4x2 texel coords, normal) for all visible faces."""
    mats = bone_matrices(model, pose)
    skip = _hidden_bones(model, set(hidden))
    quads = []
    for b, c, f, _mat, _w, _h in collect_faces(model):
        if b["name"] in skip:
            continue
        o, s = np.array(c["origin"]), np.array(c["size"], dtype=float)
        inf = c["inflate"] or 0.0
        x0, x1 = -(o[0] + s[0]) - inf, -o[0] + inf
        y0, y1 = o[1] - inf, o[1] + s[1] + inf
        z0, z1 = o[2] - inf, o[2] + s[2] + inf
        corners = {
            "west": [(x0, y1, z1), (x0, y1, z0), (x0, y0, z0), (x0, y0, z1)],
            "east": [(x1, y1, z0), (x1, y1, z1), (x1, y0, z1), (x1, y0, z0)],
            "north": [(x0, y1, z0), (x1, y1, z0), (x1, y0, z0), (x0, y0, z0)],
            "south": [(x1, y1, z1), (x0, y1, z1), (x0, y0, z1), (x1, y0, z1)],
            "up": [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)],
            "down": [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
        }[f]
        pts = np.array(corners, dtype=float)
        if c["rotation"]:
            r = c["rotation"]
            cp = c["pivot"] if c["pivot"] else tuple(np.array(c["origin"]) + s / 2.0)
            piv = np.array([-cp[0], cp[1], cp[2]])
            m = _rot(math.radians(-r[0]), math.radians(-r[1]), math.radians(r[2]))
            pts = (pts - piv) @ m.T + piv
        wm = mats[b["name"]]
        pts = pts @ wm[:3, :3].T + wm[:3, 3]
        u, v, w, h = uvs[(id(c), f)]
        uv = np.array([(u + w, v), (u, v), (u, v + h), (u + w, v + h)], dtype=float)
        n = np.cross(pts[1] - pts[0], pts[3] - pts[0])
        ln = np.linalg.norm(n)
        if ln < 1e-9:
            continue
        quads.append((pts, uv, n / ln))
    return quads


def view_matrix(yaw: float, pitch: float) -> np.ndarray:
    """Return a 3x3 rotation taking render space to view space (camera looks along -Z, Y up).

    yaw rotates the model about Y (degrees), then pitch tilts it toward the camera about X.
    """
    a, p = math.radians(yaw), math.radians(pitch)
    ry = np.array([[math.cos(a), 0, math.sin(a)], [0, 1, 0], [-math.sin(a), 0, math.cos(a)]])
    rx = np.array([[1, 0, 0], [0, math.cos(p), -math.sin(p)], [0, math.sin(p), math.cos(p)]])
    return rx @ ry


def _light(n: np.ndarray) -> float:
    """Minecraft-like face shading from a render-space normal: up 1.0, N/S 0.8, E/W 0.6, down 0.5."""
    nx, ny, nz = n
    return nx * nx * 0.6 + nz * nz * 0.8 + ny * ny * (1.0 if ny > 0 else 0.5)


def render(model, atlas: np.ndarray, uvs: dict, size: int | tuple[int, int] = 32, yaw: float = 135.0,
           pitch: float = 30.0, pose: dict | None = None, hidden: set[str] = frozenset(), margin: int = 1,
           fit_quads=None, shade: bool = True) -> np.ndarray:
    """Rasterize a model into an RGBA array, scaled to fit the canvas with a margin.

    Args:
        yaw: model rotation about Y in degrees; 135 shows the north (front) face on the left, 315 the back.
        fit_quads: optional quads used only to compute the fit (keeps scale stable across animation frames).
    """
    W, H = (size, size) if isinstance(size, int) else size
    quads = face_quads(model, atlas.shape, uvs, pose, hidden)
    vm = view_matrix(yaw, pitch)
    fit = fit_quads if fit_quads is not None else quads
    out = np.zeros((H, W, 4), dtype=np.uint8)
    if not quads or not fit:
        return out
    allp = np.concatenate([q[0] for q in fit]) @ vm.T
    lo, hi = allp.min(axis=0), allp.max(axis=0)
    span = max((hi[0] - lo[0]) / (W - 2 * margin), (hi[1] - lo[1]) / (H - 2 * margin), 1e-6)
    k = 1.0 / span
    cx, cy = (lo[0] + hi[0]) / 2.0, (lo[1] + hi[1]) / 2.0
    zbuf = np.full((H, W), -np.inf)
    ah, aw = atlas.shape[:2]
    items = []
    for pts, uv, n in quads:
        vp = pts @ vm.T
        vn = vm @ n
        if vn[2] <= 1e-6:
            continue
        sp = np.stack([(vp[:, 0] - cx) * k + W / 2.0, (cy - vp[:, 1]) * k + H / 2.0, vp[:, 2]], axis=1)
        items.append((sp[:, 2].mean(), sp, uv, _light(n) if shade else 1.0))
    items.sort(key=lambda it: it[0])
    for _, sp, uv, lit in items:
        p0, e1, e3 = sp[0, :2], sp[1, :2] - sp[0, :2], sp[3, :2] - sp[0, :2]
        det = e1[0] * e3[1] - e1[1] * e3[0]
        if abs(det) < 1e-9:
            continue
        xa, xb = int(max(0, math.floor(sp[:, 0].min()))), int(min(W - 1, math.ceil(sp[:, 0].max())))
        ya, yb = int(max(0, math.floor(sp[:, 1].min()))), int(min(H - 1, math.ceil(sp[:, 1].max())))
        if xa > xb or ya > yb:
            continue
        yy, xx = np.mgrid[ya:yb + 1, xa:xb + 1].astype(float)
        dx, dy = xx + 0.5 - p0[0], yy + 0.5 - p0[1]
        a = (dx * e3[1] - dy * e3[0]) / det
        b = (e1[0] * dy - e1[1] * dx) / det
        inside = (a >= 0) & (a < 1) & (b >= 0) & (b < 1)
        if not inside.any():
            continue
        z = sp[0, 2] + a * (sp[1, 2] - sp[0, 2]) + b * (sp[3, 2] - sp[0, 2])
        u = uv[0, 0] + a * (uv[1, 0] - uv[0, 0]) + b * (uv[3, 0] - uv[0, 0])
        v = uv[0, 1] + a * (uv[1, 1] - uv[0, 1]) + b * (uv[3, 1] - uv[0, 1])
        ui = np.clip(np.floor(u).astype(int), int(uv[:, 0].min()), int(uv[:, 0].max()) - 1).clip(0, aw - 1)
        vi = np.clip(np.floor(v).astype(int), int(uv[:, 1].min()), int(uv[:, 1].max()) - 1).clip(0, ah - 1)
        tex = atlas[vi, ui]
        zb = zbuf[ya:yb + 1, xa:xb + 1]
        ok = inside & (tex[..., 3] >= 128) & (z >= zb - 1e-4)
        if not ok.any():
            continue
        rgb = np.clip(tex[..., :3].astype(float) * lit, 0, 255).astype(np.uint8)
        region = out[ya:yb + 1, xa:xb + 1]
        region[ok, :3] = rgb[ok]
        region[ok, 3] = 255
        zb[ok] = z[ok]
    return out

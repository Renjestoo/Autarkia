"""Armour sprites and flat 64x32 humanoid armour layers (vanilla box-UV layout).

The flat layers follow the vanilla humanoid layout: head box at (0, 0) 8x8x8, body at (16, 16)
8x12x4, arm at (40, 16) 4x12x4 and leg at (0, 16) 4x12x4. The side faces of each box form one
continuous band (right, front, left, back), so horizontal trims wrap around the part.
"""

import numpy as np

from paint import (add_outline, bevel, col, dither, fbm, new, outline_col, poly_mask, put, ramp, rect, relief, rivet,
                   rng_for, shade_img)

STYLE = {
    "bronze": dict(body="bronze", trim="bronze", rivet="bronze", glow=None, under="dark_wood", kind="classic"),
    "steel_rig": dict(body="steel", trim="steel", rivet="bronze", glow=None, under="leather", kind="rig"),
    "exo_rig": dict(body="electric", trim="steel", rivet="steel", glow="electric_accent", under="rubber", kind="exo"),
    "frontier_suit": dict(body="vantite", trim="vantite_steel", rivet="vantite_steel", glow="#A8F0DC", under="vantite_steel", kind="frontier"),
    "lattice_carapace": dict(body="lattice", trim="lattice", rivet="lattice", glow="#B39DFF", under="lattice", kind="lattice"),
    "aegis": dict(body="aegis_body", trim="aegis_gold", rivet="aegis_gold", glow="aegis_glow", under="aegis_body", kind="aegis"),
}
TECH_GLOW = {"electric": "electric_accent", "vantite": "#A8F0DC", "lattice": "#B39DFF", "aegis": "aegis_glow"}
TECH_TRIM = {"electric": "hazard", "vantite": "vantite_steel", "lattice": "lattice", "aegis": "aegis_gold"}


def _faces(u: int, v: int, x: int, y: int, z: int) -> dict[str, tuple[int, int, int, int]]:
    """Return the box-UV rectangles (x, y, w, h) of a vanilla model box."""
    return {"top": (u + z, v, x, z), "bottom": (u + z + x, v, x, z), "right": (u, v + z, z, y),
            "front": (u + z, v + z, x, y), "left": (u + z + x, v + z, z, y), "back": (u + 2 * z + x, v + z, x, y)}


HEAD = _faces(0, 0, 8, 8, 8)
BODY = _faces(16, 16, 8, 12, 4)
ARM = _faces(40, 16, 4, 12, 4)
LEG = _faces(0, 16, 4, 12, 4)
SIDES = ("right", "front", "left", "back")


class Kit:
    """Ramps and painting helpers for one armour asset."""

    def __init__(self, asset: str, pal: str):
        s = STYLE[asset]
        self.asset, self.kind = asset, s["kind"]
        self.r = ramp(s["body"] if pal != "aegis" else "aegis_body")
        self.t = ramp(s["trim"])
        self.k = ramp(s["rivet"])
        self.g = ramp(s["glow"]) if s["glow"] else None
        self.u = ramp(s["under"])
        self.rng = rng_for("armor_layer", asset)
        self.base = {"aegis": 1.2, "lattice": 1.0}.get(self.kind, 2.0)

    def plate(self, img: np.ndarray, x: int, y: int, w: int, h: int, r=None, base: float | None = None) -> None:
        """Fill a rectangle with a calm, top-lit plate and a light/dark bevel."""
        if w <= 0 or h <= 0:
            return
        r = self.r if r is None else r
        b = self.base if base is None else base
        n = fbm(w, h, self.rng, (4, 2), (0.7, 0.3))
        vals = b + (n - 0.5) * 0.7 + np.linspace(0.3, -0.3, h)[:, None]
        idx = dither(vals - 0.5, 0.35).clip(0, 3)
        img[y:y + h, x:x + w] = shade_img(r, idx)
        if w >= 2 and h >= 2:
            hi, lo = col(r, int(b) + 1), col(r, max(0, int(b) - 1))
            bevel(img, x, y, x + w - 1, y + h - 1, hi, lo)
            if self.kind == "aegis" and r is self.r:
                rect(img, x, y, x + w - 1, y, col(self.t, 2))

    def band(self, img: np.ndarray, faces: dict, y0: int, y1: int, which=SIDES, r=None, base=None) -> None:
        """Plate rows y0..y1 (relative to the side-face top) on the given side faces."""
        for f in which:
            fx, fy, fw, fh = faces[f]
            self.plate(img, fx, fy + y0, fw, min(y1, fh - 1) - y0 + 1, r, base)

    def trim_row(self, img: np.ndarray, faces: dict, row: int, shade: int = 3, which=SIDES, r=None) -> None:
        """Paint one bright trim row across the side faces."""
        r = self.t if r is None else r
        for f in which:
            fx, fy, fw, _ = faces[f]
            rect(img, fx, fy + row, fx + fw - 1, fy + row, col(r, shade))

    def glow_px(self, img: np.ndarray, x: int, y: int, shade: int = 3) -> None:
        """Put one glow pixel (falls back to a bright trim pixel without a glow ramp)."""
        put(img, x, y, col(self.g if self.g is not None else self.t, shade))


def _helmet(img: np.ndarray, k: Kit) -> None:
    tx, ty, _, _ = HEAD["top"]
    k.plate(img, tx, ty, 8, 8)
    if k.kind in ("classic", "rig"):
        rect(img, tx + 3, ty, tx + 4, ty + 7, col(k.t, 3))
        rect(img, tx + 4, ty, tx + 4, ty + 7, col(k.t, 2))
    elif k.kind == "lattice":
        for x, y in ((tx + 2, ty + 2), (tx + 5, ty + 3), (tx + 3, ty + 5)):
            put(img, x, y, col(k.r, 4))
            put(img, x + 1, y + 1, col(k.r, 3))
    else:
        rect(img, tx + 3, ty + 1, tx + 4, ty + 6, col(k.t, 3 if k.kind != "aegis" else 2))
    for f in ("right", "left"):
        fx, fy, _, _ = HEAD[f]
        k.plate(img, fx, fy, 8, 6)
        rect(img, fx, fy + 6, fx + 4, fy + 6, col(k.r, int(k.base) - 1 if k.base >= 1 else 0))
    bx, by, _, _ = HEAD["back"]
    k.plate(img, bx, by, 8, 7)
    fx, fy, _, _ = HEAD["front"]
    k.plate(img, fx, fy, 8, 2)
    k.plate(img, fx, fy + 2, 1, 5)
    k.plate(img, fx + 7, fy + 2, 1, 5)
    if k.kind == "classic":
        rect(img, fx + 1, fy + 2, fx + 6, fy + 2, col(k.r, 0))
        k.plate(img, fx + 3, fy + 2, 2, 4)
        k.plate(img, fx + 1, fy + 4, 2, 2)
        k.plate(img, fx + 5, fy + 4, 2, 2)
    elif k.kind == "rig":
        k.plate(img, fx + 1, fy + 2, 6, 2, ramp("rubber"), 1.0)
        rect(img, fx + 2, fy + 3, fx + 5, fy + 3, col(ramp("glass"), 3))
        put(img, fx + 2, fy + 3, col(ramp("glass"), 4))
    else:
        rows = 3 if k.kind in ("frontier", "aegis") else 2
        for yy in range(rows):
            for xx in range(1, 7):
                put(img, fx + xx, fy + 2 + yy, col(k.g, 4 if (xx + yy) % 5 == 0 else (3 if yy == 0 else 2)))
        if k.kind == "aegis":
            rect(img, fx + 3, fy + 2 + rows, fx + 4, fy + 6, col(k.t, 2))
        if k.kind == "exo":
            k.plate(img, fx + 1, fy + 5, 6, 2, ramp("rubber"), 1.2)
    k.trim_row(img, HEAD, 0, 3)
    if k.kind == "aegis":
        for f in ("right", "left", "back"):
            hx, hy, _, _ = HEAD[f]
            rect(img, hx + 1, hy + 3, hx + 6, hy + 3, col(k.g, 3))
    for f in ("right", "left"):
        hx, hy, _, _ = HEAD[f]
        rivet(img, hx + 1, hy + 2, k.k)
        rivet(img, hx + 5, hy + 2, k.k)
        if k.kind == "exo":
            k.plate(img, hx + 2, hy + 2, 4, 3, k.r, 2.5)
            k.glow_px(img, hx + 3, hy + 3, 4)
        if k.kind == "frontier":
            k.plate(img, hx + 2, hy + 2, 3, 3, k.t, 2.0)
            k.glow_px(img, hx + 3, hy + 3, 3)
        if k.kind == "lattice":
            _facet(img, hx + 1, hy + 1, 3, 4, k.r)
            _facet(img, hx + 4, hy + 2, 3, 3, k.r)
    if k.kind == "lattice":
        _facet(img, HEAD["back"][0] + 1, HEAD["back"][1] + 1, 6, 4, k.r)
        _facet(img, HEAD["top"][0] + 2, HEAD["top"][1] + 1, 4, 6, k.r)


def _chest(img: np.ndarray, k: Kit) -> None:
    k.band(img, BODY, 0, 9)
    fx, fy, _, _ = BODY["front"]
    bx, by, _, _ = BODY["back"]
    k.trim_row(img, BODY, 0, 3)
    if k.kind == "classic":
        rect(img, fx + 3, fy + 1, fx + 3, fy + 7, col(k.r, 3))
        rect(img, fx + 4, fy + 1, fx + 4, fy + 7, col(k.r, 1))
        rect(img, fx + 1, fy + 5, fx + 6, fy + 5, col(k.r, 1))
        rect(img, fx + 1, fy + 7, fx + 6, fy + 7, col(k.r, 1))
        for x in (fx + 1, fx + 5):
            rivet(img, x, fy + 2, k.k)
    elif k.kind == "rig":
        lr = ramp("leather")
        for x in (fx + 1, fx + 6, bx + 1, bx + 6):
            rect(img, x, fy, x, fy + 9, col(lr, 2))
            put(img, x, fy + 4, col(ramp("bronze"), 3))
        rect(img, fx + 1, fy + 7, fx + 6, fy + 7, col(lr, 1))
        for x, y in ((fx + 3, fy + 2), (fx + 3, fy + 5), (bx + 3, by + 3)):
            rivet(img, x, y, k.k)
        k.plate(img, bx + 2, by + 2, 4, 6, ramp("brass"), 2.0)
    elif k.kind == "exo":
        k.plate(img, fx + 2, fy + 1, 4, 4, k.r, 2.6)
        rect(img, fx + 3, fy + 2, fx + 4, fy + 3, col(k.g, 3))
        put(img, fx + 3, fy + 2, col(k.g, 4))
        hz = ramp("hazard")
        for xx in range(8):
            put(img, fx + xx, fy + 8, col(hz, 3 if xx % 4 < 2 else 0))
            put(img, bx + xx, by + 9, col(hz, 3 if xx % 4 < 2 else 0))
        k.plate(img, bx + 1, by + 1, 6, 7, ramp("steel"), 1.5)
        for yy in (2, 4, 6):
            rect(img, bx + 2, by + yy, bx + 5, by + yy, col(k.g, 2 if yy != 4 else 3))
        rect(img, fx, fy + 6, fx + 7, fy + 6, col(k.u, 2))
    elif k.kind == "frontier":
        k.plate(img, fx + 1, fy + 2, 6, 3, k.t, 2.0)
        rect(img, fx + 2, fy + 3, fx + 5, fy + 3, col(k.g, 3))
        put(img, fx + 2, fy + 3, col(k.g, 4))
        k.plate(img, bx + 1, by + 1, 2, 8, k.t, 2.3)
        k.plate(img, bx + 5, by + 1, 2, 8, k.t, 2.3)
        rect(img, fx + 3, fy + 6, fx + 4, fy + 9, col(k.r, 1))
    elif k.kind == "lattice":
        dark = ramp("lattice")
        for f in SIDES:
            x0, y0, w, _ = BODY[f]
            k.plate(img, x0, y0, w, 10, dark, 0.8)
        for (x, y, w, h) in ((fx + 1, fy + 1, 3, 4), (fx + 4, fy + 1, 3, 4), (fx + 2, fy + 6, 4, 3),
                             (bx + 1, by + 1, 6, 3), (bx + 2, by + 5, 4, 3)):
            _facet(img, x, y, w, h, k.r)
        put(img, fx + 4, fy + 5, col(k.g, 4))
        put(img, fx + 3, fy + 5, col(k.g, 3))
    else:
        rect(img, fx + 3, fy + 1, fx + 4, fy + 9, col(k.t, 2))
        rect(img, fx + 1, fy + 1, fx + 2, fy + 1, col(k.t, 3))
        rect(img, fx + 5, fy + 1, fx + 6, fy + 1, col(k.t, 3))
        for xx in range(1, 7):
            if xx not in (3, 4):
                put(img, fx + xx, fy + 5, col(k.g, 3))
        rect(img, fx + 3, fy + 4, fx + 4, fy + 5, col(k.g, 4))
        rect(img, bx + 3, by, bx + 4, by + 9, col(k.t, 2))
        rect(img, bx + 1, by + 6, bx + 6, by + 6, col(k.g, 3))
        for f in ("right", "left"):
            x0, y0, _, _ = BODY[f]
            rect(img, x0 + 1, y0 + 2, x0 + 1, y0 + 8, col(k.g, 3))


def _facet(img: np.ndarray, x: int, y: int, w: int, h: int, r: np.ndarray) -> None:
    """Paint a faceted crystal plate: bright top-left facet, mid body, dark lower-right edge."""
    for yy in range(h):
        for xx in range(w):
            s = 3 if xx + yy < (w + h) // 2 - 1 else 2
            if xx == w - 1 or yy == h - 1:
                s = 1
            put(img, x + xx, y + yy, col(r, s))
    put(img, x, y, col(r, 4))
    if w > 2:
        put(img, x + 1, y, col(r, 4))


def _arms(img: np.ndarray, k: Kit) -> None:
    tx, ty, _, _ = ARM["top"]
    k.plate(img, tx, ty, 4, 4)
    k.band(img, ARM, 0, 4)
    k.trim_row(img, ARM, 4, 2)
    for f in ("right", "front", "left", "back"):
        x0, y0, _, _ = ARM[f]
        rivet(img, x0 + 1, y0 + 1, k.k)
    if k.kind in ("exo", "frontier", "aegis", "lattice"):
        k.band(img, ARM, 8, 10, r=k.t if k.kind != "aegis" else k.r, base=2.0 if k.kind != "aegis" else 1.2)
        for f in ("right", "left"):
            x0, y0, _, _ = ARM[f]
            rect(img, x0 + 1, y0 + 9, x0 + 2, y0 + 9, col(k.g, 3))
    if k.kind == "lattice":
        for f in ("right", "left", "front", "back"):
            x0, y0, _, _ = ARM[f]
            _facet(img, x0, y0, 4, 4, k.r)
    if k.kind == "aegis":
        rect(img, tx, ty, tx + 3, ty, col(k.t, 3))
        for f in ("right", "left"):
            x0, y0, _, _ = ARM[f]
            rect(img, x0, y0 + 2, x0 + 3, y0 + 2, col(k.g, 3))


def _boots(img: np.ndarray, k: Kit) -> None:
    bx, by, _, _ = LEG["bottom"]
    k.plate(img, bx, by, 4, 4, ramp("rubber") if k.kind != "aegis" else k.r, 1.2)
    k.band(img, LEG, 6, 11)
    k.trim_row(img, LEG, 6, 3)
    fx, fy, _, _ = LEG["front"]
    k.plate(img, fx, fy + 9, 4, 3, k.t if k.kind != "lattice" else k.r, 2.6 if k.kind != "aegis" else 2.2)
    rect(img, LEG["right"][0], LEG["right"][1] + 11, LEG["back"][0] + 3, LEG["back"][1] + 11,
          col(ramp("rubber"), 1) if k.kind != "aegis" else col(k.t, 1))
    if k.g is not None:
        for f in ("right", "left"):
            x0, y0, _, _ = LEG[f]
            rect(img, x0 + 1, y0 + 8, x0 + 2, y0 + 8, col(k.g, 3))
    if k.kind == "frontier":
        x0, y0, _, _ = LEG["back"]
        k.plate(img, x0, y0 + 8, 4, 3, ramp("vantite_steel"), 1.2)
        rect(img, x0 + 1, y0 + 10, x0 + 2, y0 + 10, col(k.g, 4))
    if k.kind in ("classic", "rig"):
        for f in ("right", "left"):
            x0, y0, _, _ = LEG[f]
            rivet(img, x0 + 1, y0 + 7, k.k)


def _leggings(img: np.ndarray, k: Kit) -> None:
    tx, ty, _, _ = LEG["top"]
    k.plate(img, tx, ty, 4, 4, k.u if k.kind not in ("classic",) else k.r, 1.6)
    k.band(img, LEG, 0, 8)
    if k.kind == "lattice":
        k.band(img, LEG, 0, 8, r=ramp("lattice"), base=0.8)
        _facet(img, LEG["front"][0], LEG["front"][1] + 1, 4, 3, k.r)
        _facet(img, LEG["right"][0], LEG["right"][1] + 1, 4, 3, k.r)
        _facet(img, LEG["left"][0], LEG["left"][1] + 1, 4, 3, k.r)
    fx, fy, _, _ = LEG["front"]
    k.plate(img, fx, fy + 5, 4, 3, k.t if k.kind != "aegis" else ramp("aegis_gold"), 2.4)
    put(img, fx + 1, fy + 6, col(k.t if k.kind != "aegis" else ramp("aegis_gold"), 4))
    for f in ("right", "left"):
        x0, y0, _, _ = LEG[f]
        if k.g is not None:
            rect(img, x0 + 1, y0 + 1, x0 + 1, y0 + 4, col(k.g, 3))
        elif k.kind == "classic":
            rivet(img, x0 + 1, y0 + 1, k.k)
        else:
            rect(img, x0 + 1, y0 + 1, x0 + 2, y0 + 3, col(ramp("leather"), 2))
    k.band(img, BODY, 7, 11, r=k.u if k.kind != "classic" else ramp("leather"), base=1.6)
    k.trim_row(img, BODY, 7, 3)
    bfx, bfy, _, _ = BODY["front"]
    buckle = ramp("bronze") if k.kind in ("classic", "rig") else (k.g if k.g is not None else k.t)
    rect(img, bfx + 3, bfy + 8, bfx + 4, bfy + 10, col(buckle, 3))
    put(img, bfx + 3, bfy + 8, col(buckle, 4))
    if k.kind == "exo":
        hz = ramp("hazard")
        for f in ("right", "left"):
            x0, y0, _, _ = LEG[f]
            for yy in range(0, 9, 2):
                put(img, x0 + 3, y0 + yy, col(hz, 3))


def armor_layers(asset: str, pal: str) -> tuple[np.ndarray, np.ndarray]:
    """Return the (humanoid, humanoid_leggings) 64x32 RGBA layers of one armour asset."""
    if asset not in STYLE:
        raise KeyError(f"armour asset '{asset}' has no style; known: {sorted(STYLE)}")
    k = Kit(asset, pal)
    body = new(64, 32)
    _helmet(body, k)
    _chest(body, k)
    _arms(body, k)
    _boots(body, k)
    legs = new(64, 32)
    _leggings(legs, k)
    return body, legs


def _ramps(pal: str, tech: bool) -> tuple[np.ndarray, np.ndarray, np.ndarray | None]:
    """Return (body, trim, glow) ramps of an armour sprite."""
    if not tech:
        return ramp(pal), ramp("bronze" if pal == "steel" else pal), None
    body = ramp("aegis_body" if pal == "aegis" else pal)
    return body, ramp(TECH_TRIM[pal]), ramp(TECH_GLOW[pal])


def _base(pal: str) -> float:
    """Return the sprite base shade: darker for the aegis and lattice families so glow accents read."""
    return {"aegis": 1.4, "lattice": 1.6}.get(pal, 2.0)


def _finish(mask: np.ndarray, r: np.ndarray, rng, base: float = 2.0) -> np.ndarray:
    """Shade a silhouette with top-left relief and a dark outline."""
    vals = relief(mask, base, 0.7, 2) + (rng.random(mask.shape) - 0.5) * 0.3
    img = shade_img(r, np.clip(np.rint(vals).astype(int), 0, 4), mask)
    add_outline(img, outline_col(r))
    return img


def _helmet_mask(tech: bool) -> np.ndarray:
    m = np.zeros((16, 16), bool)
    m |= poly_mask(16, 16, [(4, 3), (11, 3), (13, 5), (13, 11), (2, 11), (2, 5)])
    m[11, 2:14] = False
    if tech:
        m[11, 4:12] = True
    else:
        m[9:12, 5:11] = False
    return m


def _sprite_helmet(name: str, pal: str, rng, tech: bool) -> np.ndarray:
    r, t, g = _ramps(pal, tech)
    m = _helmet_mask(tech)
    img = _finish(m, r, rng, _base(pal))
    if tech:
        rect(img, 2, 6, 13, 6, col(r, 0))
        rect(img, 2, 9, 13, 9, col(r, 0))
        rect(img, 3, 7, 12, 8, col(g, 3))
        rect(img, 4, 7, 6, 7, col(g, 4))
        rect(img, 9, 8, 11, 8, col(g, 2))
        rect(img, 7, 3, 8, 6, col(t, 3))
        put(img, 7, 3, col(t, 4))
    else:
        rect(img, 7, 3, 8, 8, col(t, 3))
        put(img, 7, 4, col(t, 4))
        rect(img, 5, 8, 10, 8, col(r, 0))
        rect(img, 7, 9, 8, 10, col(r, 2))
        rivet(img, 3, 6, t)
        rivet(img, 11, 6, t)
    return img


def _sprite_chest(name: str, pal: str, rng, tech: bool) -> np.ndarray:
    r, t, g = _ramps(pal, tech)
    m = poly_mask(16, 16, [(1, 3), (5, 2), (10, 2), (14, 3), (14, 7), (12, 7), (12, 13), (3, 13), (3, 7), (1, 7)])
    m[2:4, 6:10] = False
    m[2, 5] = m[2, 10] = False
    img = _finish(m, r, rng, _base(pal))
    if tech:
        rect(img, 7, 6, 8, 7, col(g, 3))
        put(img, 7, 6, col(g, 4))
        rect(img, 4, 10, 11, 10, col(t, 3))
        rect(img, 1, 4, 3, 4, col(t, 3))
        rect(img, 12, 4, 14, 4, col(t, 3))
        put(img, 5, 5, col(g, 2))
        put(img, 10, 5, col(g, 2))
    else:
        rect(img, 7, 4, 7, 12, col(r, 3))
        rect(img, 8, 4, 8, 12, col(r, 1))
        rect(img, 4, 9, 11, 9, col(r, 1))
        rivet(img, 4, 5, t)
        rivet(img, 10, 5, t)
        rect(img, 1, 4, 3, 4, col(t, 3))
        rect(img, 12, 4, 14, 4, col(t, 3))
    return img


def _sprite_legs(name: str, pal: str, rng, tech: bool) -> np.ndarray:
    r, t, g = _ramps(pal, tech)
    m = np.zeros((16, 16), bool)
    m[2:6, 3:13] = True
    m[6:14, 3:7] = True
    m[6:14, 9:13] = True
    img = _finish(m, r, rng, _base(pal))
    rect(img, 3, 3, 12, 3, col(t, 3))
    rect(img, 7, 3, 8, 4, col(t if not tech else g, 4))
    if tech:
        rect(img, 4, 8, 5, 9, col(g, 3))
        rect(img, 10, 8, 11, 9, col(g, 3))
        put(img, 4, 8, col(g, 4))
        put(img, 10, 8, col(g, 4))
    else:
        rect(img, 4, 9, 6, 9, col(r, 3))
        rect(img, 9, 9, 11, 9, col(r, 3))
        rivet(img, 4, 6, t)
        rivet(img, 10, 6, t)
    return img


def _sprite_boots(name: str, pal: str, rng, tech: bool) -> np.ndarray:
    r, t, g = _ramps(pal, tech)
    m = np.zeros((16, 16), bool)
    m[6:13, 3:7] = True
    m[10:13, 1:7] = True
    m[6:13, 9:13] = True
    m[10:13, 9:15] = True
    img = _finish(m, r, rng, _base(pal))
    rect(img, 3, 6, 6, 6, col(t, 3))
    rect(img, 9, 6, 12, 6, col(t, 3))
    sole = col(ramp("rubber"), 1) if pal != "aegis" else col(t, 1)
    rect(img, 1, 12, 6, 12, sole)
    rect(img, 9, 12, 14, 12, sole)
    if tech:
        rect(img, 4, 9, 5, 9, col(g, 3))
        rect(img, 10, 9, 11, 9, col(g, 3))
        put(img, 6, 11, col(g, 4))
        put(img, 14, 11, col(g, 4))
    else:
        put(img, 2, 10, col(r, 4))
        put(img, 10, 10, col(r, 4))
    return img


def _shape(fn, tech: bool):
    return lambda name, pal, rng: fn(name, pal, rng, tech)


SHAPES = {
    "armor_helmet": _shape(_sprite_helmet, False), "armor_chest": _shape(_sprite_chest, False),
    "armor_legs": _shape(_sprite_legs, False), "armor_boots": _shape(_sprite_boots, False),
    "armor_helmet_tech": _shape(_sprite_helmet, True), "armor_chest_tech": _shape(_sprite_chest, True),
    "armor_legs_tech": _shape(_sprite_legs, True), "armor_boots_tech": _shape(_sprite_boots, True),
}

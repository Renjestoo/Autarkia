"""GeckoLib block models of tier 4 (frontier): vantite steel, teal vantite and green #7CFF6B lamps."""

import math

from geo_core import Model

VS = "vantite_steel"
LAMP = "#7CFF6B"


def strut(m: Model, bone: str, p0, p1, t: int, mat: str, **kw) -> None:
    """Add a square beam of thickness t from p0 to p1 (file coordinates), rotated about p0."""
    dx, dy, dz = (p1[i] - p0[i] for i in range(3))
    length = math.sqrt(dx * dx + dy * dy + dz * dz)
    phi = math.asin(max(-1.0, min(1.0, dz / length)))
    gamma = math.atan2(dx, dy) if math.cos(phi) > 1e-9 else 0.0
    rot = (-math.degrees(phi), 0.0, math.degrees(gamma))
    m.box(bone, (p0[0] - t / 2.0, p0[1], p0[2] - t / 2.0), (t, max(1, round(length)), t), mat,
          rotation=rot if any(abs(a) > 1e-6 for a in rot) else None, pivot=tuple(p0), **kw)


def rnd(m: Model, bone: str, c, d: int, h: int, mat: str, axis: str = "y", n: int = 4, **kw) -> None:
    """Add a regular 2n-gon prism of flat-to-flat width d and length h as n crossed bars.

    For axis y, c is the bottom centre; for axis z, c is the centre of the north end; for axis x,
    c is the centre of the file -X end. Bars are staggered by 0.05 px so end faces never z-fight.
    """
    w = max(1, round(d * math.tan(math.radians(90.0 / n))))
    for j in range(n):
        a = 180.0 * j / n
        off = 0.05 * j
        if axis == "y":
            origin, size, rot = (c[0] - d / 2.0, c[1] + off, c[2] - w / 2.0), (d, h, w), (0, a, 0)
            piv = (c[0], c[1], c[2])
        elif axis == "z":
            origin, size, rot = (c[0] - d / 2.0, c[1] - w / 2.0, c[2] + off), (d, w, h), (0, 0, a)
            piv = (c[0], c[1], c[2])
        else:
            origin, size, rot = (c[0] + off, c[1] - d / 2.0, c[2] - w / 2.0), (h, d, w), (a, 0, 0)
            piv = (c[0], c[1], c[2])
        m.box(bone, origin, size, mat, rotation=rot if a else None, pivot=piv if a else None, **kw)


def ring(m: Model, bone: str, c, radius: float, n: int, size, mat: str, axis: str = "y", skip=(), **kw) -> None:
    """Add n tangent segments of size (width, height, thickness) on a circle around c (axis y or z)."""
    sw, sh, st = size
    for i in range(n):
        if i in skip:
            continue
        th = 360.0 * i / n
        s, co = math.sin(math.radians(th)), math.cos(math.radians(th))
        if axis == "y":
            p = (c[0] + radius * s, c[1], c[2] - radius * co)
            m.box(bone, (p[0] - sw / 2.0, p[1], p[2] - st / 2.0), (sw, sh, st), mat,
                  rotation=(0, -th, 0) if th else None, pivot=p if th else None, **kw)
        else:
            p = (c[0] + radius * s, c[1] + radius * co, c[2])
            m.box(bone, (p[0] - sw / 2.0, p[1] - sh / 2.0, p[2]), (sw, sh, st), mat,
                  rotation=(0, 0, th) if th else None, pivot=p if th else None, **kw)


def radial(m: Model, bone: str, c, radius: float, n: int, size, mat: str, phase: float = 0.0, pitch: float = 0.0) -> None:
    """Add n plates of size (length, height, thickness) pointing radially out from a vertical axis."""
    ln, h, t = size
    for i in range(n):
        th = phase + 360.0 * i / n
        s, co = math.sin(math.radians(th)), math.cos(math.radians(th))
        p = (c[0] + radius * s, c[1], c[2] - radius * co)
        m.box(bone, (p[0] - ln / 2.0, p[1], p[2] - t / 2.0), (ln, h, t), mat,
              rotation=(pitch, 90.0 - th, 0), pivot=(p[0], p[1] + h / 2.0, p[2]))


def hv_pylon() -> Model:
    """Seven-block lattice transmission tower with banded peak, crossarms and insulator strings."""
    m = Model("hv_pylon")
    m.bone("mast")

    def hw(y):
        return 6.0 - 3.5 * (y - 3.0) / 85.0

    for sx in (-1, 1):
        for sz in (-1, 1):
            m.cbox("mast", (sx * 6, 0, sz * 6), (4, 3, 4), "foundation:stone")
            m.cbox("mast", (sx * 6, 3, sz * 6), (2, 1, 2), f"{VS}:plate")
            strut(m, "mast", (sx * 6, 3, sz * 6), (sx * 2.5, 88, sz * 2.5), 2, f"{VS}:rivet")
    levels = [3, 16, 30, 44, 58, 72, 86]
    for a, b in zip(levels, levels[1:]):
        ha, hb = hw(a), hw(b)
        for side in (-1, 1):
            strut(m, "mast", (-hb, b, side * hb), (hb, b, side * hb), 1, f"{VS}:plate")
            strut(m, "mast", (side * hb, b, -hb), (side * hb, b, hb), 1, f"{VS}:plate")
            strut(m, "mast", (-ha, a, side * ha), (hb, b, side * hb), 1, f"{VS}:plate")
            strut(m, "mast", (ha, a, side * ha), (-hb, b, side * hb), 1, f"{VS}:plate")
            strut(m, "mast", (side * ha, a, -ha), (side * hb, b, hb), 1, f"{VS}:plate")
            strut(m, "mast", (side * ha, a, ha), (side * hb, b, -hb), 1, f"{VS}:plate")
    m.cbox("mast", (0, 86, 0), (6, 2, 6), f"{VS}:rivet")
    m.cbox("mast", (0, 88, 0), (5, 18, 5), "red_paint:bands:ceramic")
    m.cbox("mast", (0, 106, 0), (6, 2, 6), f"{VS}:rivet")
    m.cbox("mast", (0, 108, 0), (2, 2, 2), f"{VS}:plate")
    m.cbox("mast", (0, 110, 0), (2, 2, 2), "#FF3B3B:glow")
    m.bone("arms", "mast", pivot=(0, 100, 0))
    for y in (90, 103):
        m.cbox("arms", (0, y, 0), (16, 2, 3), f"{VS}:rivet")
        m.cbox("arms", (0, y + 2, 0), (8, 1, 2), f"{VS}:plate")
        for sx in (-1, 1):
            strut(m, "arms", (sx * 2.5, y + 5, 0), (sx * 7.5, y + 2, 0), 1, f"{VS}:plate")
            m.cbox("arms", (sx * 7, y - 1, 0), (2, 1, 3), f"{VS}:plate")
    m.bone("insulators", "arms", pivot=(0, 102, 0))
    for y in (89, 102):
        for sx in (-1, 1):
            x = sx * 6.5
            m.cbox("insulators", (x, y - 1, 0), (1, 1, 1), "steel:plate")
            for k in range(3):
                m.cbox("insulators", (x, y - 3 - 2 * k, 0), (3, 1, 3), "ceramic:plate")
                m.cbox("insulators", (x, y - 2 - 2 * k, 0), (1, 1, 1), "steel:plate")
            m.cbox("insulators", (x, y - 9, 0), (2, 2, 2), "steel:rivet")
    idle = m.anim("idle", "loop", 1.0)
    idle.osc("insulators", "rotation", (2.0, 0, 0.6))
    return m


def hydro_turbine() -> Model:
    """3x3x3 spiral-case turbine: open volute with a runner port, top intake funnel, front outlet."""
    m = Model("hydro_turbine")
    m.bone("casing")
    m.cbox("casing", (0, 0, 0), (48, 3, 48), "foundation:panel:12")
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.cbox("casing", (sx * 21, 3, sz * 21), (3, 1, 3), f"{VS}:rivet")
    rnd(m, "casing", (0, 3, 0), 32, 1, f"{VS}:dark")
    ring(m, "casing", (0, 3, 0), 17, 8, (14, 16, 3), f"{VS}:panel:8", skip=(0,))
    for i, h in enumerate((6, 7, 8, 9, 10, 11, 12)):
        ring(m, "casing", (0, 3, 0), 20, 8, (16, h, 3), f"{VS}:rivet", skip=tuple(k for k in range(8) if k != i + 1))
    for sx in (-1, 1):
        m.cbox("casing", (sx * 6, 3, -17), (3, 16, 3), f"{VS}:rivet")
    m.cbox("casing", (0, 16, -17), (9, 3, 3), f"{VS}:plate")
    m.cbox("casing", (0, 3, -17), (9, 5, 3), f"{VS}:plate")
    rnd(m, "casing", (0, 19, 0), 38, 2, f"{VS}:panel:8")
    ring(m, "casing", (0, 21, 0), 16, 8, (2, 1, 2), "vantite:plate")
    m.bone("runner", "casing", pivot=(0, 12, 0))
    rnd(m, "runner", (0, 5, 0), 6, 12, "vantite:rivet")
    radial(m, "runner", (0, 6, 0), 7, 6, (8, 10, 1), "vantite:plate", pitch=20)
    rnd(m, "runner", (0, 4, 0), 10, 1, f"{VS}:plate")
    m.bone("intake", "casing", pivot=(0, 21, 0))
    rnd(m, "intake", (0, 21, 0), 10, 6, f"{VS}:rivet")
    rnd(m, "intake", (0, 26, 0), 12, 1, "vantite:plate")
    for d, y0 in ((14, 27), (19, 31), (24, 35), (29, 39)):
        rnd(m, "intake", (0, y0, 0), d, 4, f"{VS}:panel:6")
    rnd(m, "intake", (0, 43, 0), 30, 2, "water:water")
    ring(m, "intake", (0, 43, 0), 15.5, 8, (13, 4, 2), f"{VS}:rivet")
    ring(m, "intake", (0, 46, 0), 15.5, 8, (13, 1, 3), "vantite:plate")
    for sx in (-1, 1):
        m.cbox("intake", (sx * 9, 46, 0), (1, 1, 14), f"{VS}:plate")
    for k in range(4):
        a = math.radians(45 + 90 * k)
        strut(m, "intake", (16 * math.sin(a), 21, -16 * math.cos(a)), (13 * math.sin(a), 43, -13 * math.cos(a)), 2, "vantite:plate")
    m.bone("outlet", "casing", pivot=(0, 3, -21))
    m.cbox("outlet", (0, 3, -21), (16, 6, 6), f"{VS}:rivet", faces={"north": f"{VS}:dark"})
    m.cbox("outlet", (0, 9, -21), (18, 1, 6), "vantite:plate")
    m.cbox("outlet", (0, 4, -23.5), (12, 1, 1), "water:water")
    spin = m.anim("spin", "loop", 1.0)
    spin.spin("runner", "y", 1)
    idle = m.anim("idle", "loop", 1.0)
    idle.spin("runner", "y", 1.0 / 6.0, steps=4)
    return m


def wind_tower() -> Model:
    """Eight-block white-teal wind turbine with nacelle and three 64px blades on the front rotor."""
    m = Model("wind_tower")
    m.bone("tower")
    m.cbox("tower", (0, 0, 0), (16, 3, 16), "foundation:panel:8")
    m.cbox("tower", (0, 3, 0), (14, 1, 14), f"{VS}:rivet")
    rnd(m, "tower", (0, 4, 0), 13, 2, f"{VS}:rivet")
    y = 6
    for d, h in ((12, 22), (11, 22), (10, 22), (9, 22), (8, 16)):
        rnd(m, "tower", (0, y, 0), d, h, "ceramic:plate")
        y += h
        if y < 110:
            rnd(m, "tower", (0, y - 3, 0), d + 1, 2, "vantite:plate")
    m.cbox("tower", (0, 6, -6.2), (4, 8, 1), f"{VS}:hatch")
    m.cbox("tower", (0, 14, -6.2), (5, 1, 2), f"{VS}:plate")
    m.bone("nacelle", "tower", pivot=(0, 112, 0))
    rnd(m, "nacelle", (0, 110, 0), 9, 3, f"{VS}:rivet")
    m.box("nacelle", (-5, 113, -5), (10, 9, 13), "ceramic:panel:6", faces={"south": f"{VS}:grille"})
    m.box("nacelle", (-4, 122, -4), (8, 2, 11), "ceramic:plate")
    m.box("nacelle", (-5, 116, -5), (10, 2, 12), "vantite:plate", inflate=0.1)
    m.box("nacelle", (2, 124, 5), (1, 3, 1), f"{VS}:plate")
    m.box("nacelle", (0, 126, 5), (5, 1, 1), f"{VS}:plate")
    m.box("nacelle", (-3, 124, 5), (2, 1, 2), "#FF3B3B:glow")
    m.bone("rotor", "nacelle", pivot=(0, 118, -6))
    rnd(m, "rotor", (0, 118, -7), 7, 3, "ceramic:plate", axis="z")
    rnd(m, "rotor", (0, 118, -8), 4, 1, "vantite:plate", axis="z")
    for k in range(3):
        b = m.bone(f"blade_{k + 1}", "rotor", pivot=(0, 118, -6), rotation=(0, 0, 120 * k))
        m.box(b, (-1.5, 121, -6.5), (3, 6, 2), "ceramic:plate")
        m.box(b, (-2, 127, -6), (4, 24, 1), "ceramic:plate")
        m.box(b, (-1.5, 151, -6), (3, 20, 1), "ceramic:plate")
        m.box(b, (-1, 171, -6), (2, 10, 1), "ceramic:plate")
        m.box(b, (-1, 181, -6), (2, 4, 1), "red_paint:plate")
    spin = m.anim("spin", "loop", 2.0)
    spin.spin("rotor", "z", -1)
    for a in (2, 4, 6):
        w = m.anim(f"wobble{a}", "loop", 3.0)
        w.osc("nacelle", "rotation", (0, a, 0))
    idle = m.anim("idle", "loop", 1.0)
    idle.keys("rotor", "rotation", [(0, (0, 0, 0)), (0.5, (0, 0, -4)), (1.0, (0, 0, 0))])
    return m


def magma_tap() -> Model:
    """Heavy pump head on a heat-shielded pipe with a glowing magma intake at its foot."""
    m = Model("magma_tap")
    m.bone("base")
    m.cbox("base", (0, 0, 0), (16, 2, 16), f"{VS}:rivet")
    m.cbox("base", (0, 2, 0), (12, 7, 12), f"{VS}:panel:6")
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.cbox("base", (sx * 6.5, 2, sz * 6.5), (3, 8, 3), f"{VS}:rivet")
    rnd(m, "base", (0, 9, 0), 6, 9, f"{VS}:plate")
    for y in (10, 13, 16):
        rnd(m, "base", (0, y, 0), 9, 1, "vantite:plate")
    m.cbox("base", (0, 18, 0), (14, 8, 14), f"{VS}:rivet", faces={"north": f"{VS}:gauge"})
    m.cbox("base", (0, 21, 0), (15, 2, 15), "vantite:plate")
    rnd(m, "base", (0, 26, 0), 8, 2, f"{VS}:panel")
    m.cbox("base", (6, 26, 4), (2, 3, 2), f"{VS}:plate")
    m.cbox("base", (6, 29, 4), (2, 1, 2), f"{LAMP}:glow")
    m.bone("glow", "base", pivot=(0, 3, 0))
    m.cbox("glow", (0, 3, 0), (13, 2, 13), "magma:glow")
    m.cbox("glow", (0, 6, -6.2), (6, 2, 1), "magma:glow")
    m.bone("pump", "base", pivot=(0, 28, 0))
    m.cbox("pump", (0, 28, 0), (2, 2, 2), "steel:plate")
    m.cbox("pump", (0, 30, 0), (7, 2, 7), f"{VS}:rivet")
    pump = m.anim("pump", "loop", 1.0)
    pump.keys("pump", "position", [(0, (0, 0, 0)), (0.4, (0, -2, 0)), (0.5, (0, -2, 0)), (1.0, (0, 0, 0))])
    pump.keys("glow", "scale", [(0, (1, 1, 1)), (0.4, (1.04, 1.4, 1.04)), (1.0, (1, 1, 1))])
    idle = m.anim("idle", "loop", 1.0)
    idle.osc("glow", "scale", (0.02, 0.2, 0.02), cycles=2)
    idle.osc("pump", "position", (0, 0.3, 0))
    return m


def froth_flotation_plant() -> Model:
    """3x3x3 round flotation tank with a glass window ring, froth top and a motor-driven agitator."""
    m = Model("froth_flotation_plant")
    m.bone("tank")
    rnd(m, "tank", (0, 0, 0), 46, 2, f"{VS}:rivet")
    rnd(m, "tank", (0, 2, 0), 44, 16, f"{VS}:panel:8")
    rnd(m, "tank", (0, 26, 0), 44, 11, f"{VS}:panel:8")
    ring(m, "tank", (0, 37, 0), 21, 8, (18, 3, 2), f"{VS}:rivet")
    ring(m, "tank", (0, 17, 0), 22.6, 8, (2, 10, 2), "vantite:plate", skip=())
    for sx in (-1, 1):
        m.cbox("tank", (sx * 21.5, 37, 0), (3, 4, 4), f"{VS}:rivet")
    m.cbox("tank", (0, 40, 0), (46, 3, 5), f"{VS}:rivet", faces={"north": "hazard:hazard"})
    rnd(m, "tank", (0, 43, 0), 10, 4, f"{VS}:panel")
    m.cbox("tank", (0, 47, 0), (6, 1, 6), "vantite:plate")
    m.cbox("tank", (3, 44, -5.3), (2, 2, 1), f"{LAMP}:glow")
    m.cbox("tank", (-22.5, 2, 0), (2, 10, 4), "copper:plate")
    m.cbox("tank", (-22.5, 12, 0), (3, 2, 5), "copper:rivet")
    m.bone("window", "tank", pivot=(0, 18, 0))
    rnd(m, "window", (0, 18, 0), 43, 8, "glass:glass")
    m.bone("froth", "tank", pivot=(0, 36, 0))
    rnd(m, "froth", (0, 36, 0), 41, 2, "ceramic:foam:copper")
    m.bone("agitator", "tank", pivot=(0, 36, 0))
    m.cbox("agitator", (0, 33, 0), (2, 8, 2), "steel:plate")
    rnd(m, "agitator", (0, 39, 0), 4, 1, "steel:rivet")
    for k in range(4):
        m.cbox("agitator", (0, 37, 0), (12, 2, 1), "vantite:plate", rotation=(0, 45 * k, 0), pivot=(0, 38, 0))
    agitate = m.anim("agitate", "loop", 1.0)
    agitate.spin("agitator", "y", 1)
    agitate.spin("froth", "y", 0.125, steps=2)
    agitate.osc("froth", "position", (0, 0.4, 0), cycles=2)
    idle = m.anim("idle", "loop", 1.0)
    idle.spin("agitator", "y", 0.25, steps=2)
    idle.osc("froth", "position", (0, 0.2, 0))
    return m


def cable_car_station() -> Model:
    """2x3x2 station tower: braced frame, control cabin, railed loading platform, bull wheel on top."""
    m = Model("cable_car_station")
    m.bone("frame")
    m.cbox("frame", (0, 0, 0), (32, 2, 32), "foundation:panel:16")
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.cbox("frame", (sx * 14.5, 2, sz * 14.5), (3, 36, 3), f"{VS}:rivet")
        strut(m, "frame", (sx * 14.5, 4, -13), (sx * 14.5, 36, 13), 1, f"{VS}:plate")
        strut(m, "frame", (sx * 14.5, 4, 13), (sx * 14.5, 36, -13), 1, f"{VS}:plate")
    strut(m, "frame", (-13, 18, 14.5), (13, 36, 14.5), 1, f"{VS}:plate")
    strut(m, "frame", (13, 18, 14.5), (-13, 36, 14.5), 1, f"{VS}:plate")
    m.cbox("frame", (0, 38, 0), (32, 3, 32), f"{VS}:panel:8", faces={"north": "hazard:hazard"})
    rnd(m, "frame", (0, 41, 0), 8, 3, f"{VS}:rivet")
    m.cbox("frame", (0, 2, 8), (26, 13, 12), f"{VS}:panel:8")
    m.cbox("frame", (0, 15, 8), (28, 1, 14), "vantite:plate")
    m.cbox("frame", (-3, 8, 1.5), (12, 5, 1), "glass:glass")
    m.cbox("frame", (-3, 6, 1.5), (12, 2, 1), f"{VS}:screen:{LAMP}")
    m.cbox("frame", (9, 2, 1.5), (5, 9, 1), f"{VS}:hatch")
    m.cbox("frame", (9, 12, 1.5), (3, 1, 1), f"{LAMP}:glow")
    m.bone("platform", "frame", pivot=(0, 14, -8))
    m.cbox("platform", (0, 14, -8), (29, 2, 15), f"{VS}:grate")
    m.cbox("platform", (0, 13, -15), (29, 1, 1), f"{VS}:plate")
    for x in (-10, 0, 10):
        m.cbox("platform", (x, 2, -14), (2, 12, 2), f"{VS}:plate")
    for x in (-13.5, -7, 0, 7, 13.5):
        m.cbox("platform", (x, 16, -15), (1, 6, 1), f"{VS}:plate")
    m.cbox("platform", (0, 21, -15), (28, 1, 1), "hazard:hazard")
    m.cbox("platform", (0, 18, -15), (28, 1, 1), f"{VS}:plate")
    for sx in (-1, 1):
        for z in (-12, -7, -2):
            m.cbox("platform", (sx * 13.5, 16, z), (1, 6, 1), f"{VS}:plate")
        m.cbox("platform", (sx * 13.5, 21, -8), (1, 1, 13), "hazard:hazard")
    m.bone("bull_wheel", "frame", pivot=(0, 45, 0))
    rnd(m, "bull_wheel", (0, 43.8, 0), 6, 4, "vantite:rivet")
    ring(m, "bull_wheel", (0, 43.5, 0), 13.5, 12, (8, 3, 2), f"{VS}:rivet")
    ring(m, "bull_wheel", (0, 44.5, 0), 15, 12, (8, 1, 1), "rubber:rubber")
    for k in range(6):
        a = math.radians(60 * k)
        strut(m, "bull_wheel", (2 * math.sin(a), 45, -2 * math.cos(a)), (13 * math.sin(a), 45, -13 * math.cos(a)), 2, f"{VS}:plate")
    spin = m.anim("wheel_spin", "loop", 2.0)
    spin.spin("bull_wheel", "y", 1)
    idle = m.anim("idle", "loop", 1.0)
    idle.osc("bull_wheel", "rotation", (0, 1.5, 0))
    return m


def crate_launcher() -> Model:
    """Catapult: hazard-edged base, side cheeks with a stop bar, spring drum, long arm with a crate cup."""
    m = Model("crate_launcher")
    m.bone("base")
    m.cbox("base", (0, 0, 0), (16, 3, 16), f"{VS}:rivet")
    m.cbox("base", (0, 3, -6.5), (16, 1, 3), "hazard:hazard")
    for sx in (-1, 1):
        m.cbox("base", (sx * 7, 3, -6), (2, 22, 2), f"{VS}:rivet")
        m.cbox("base", (sx * 7, 10, -3), (2, 8, 4), f"{VS}:panel:6")
        strut(m, "base", (sx * 7, 3, 6), (sx * 7, 23, -5), 2, f"{VS}:plate")
        rnd(m, "base", (sx * 7 - 1, 14, -3), 5, 2, "vantite:rivet", axis="x")
    m.cbox("base", (0, 23, -6.5), (12, 2, 2), f"{VS}:plate", faces={"south": "rubber:rubber"})
    rnd(m, "base", (-6, 14, -3), 7, 12, f"{VS}:plate", axis="x")
    for x in (-5, 3):
        rnd(m, "base", (x, 14, -3), 8, 2, "steel:coil", axis="x")
    m.cbox("base", (0, 3, 6.5), (6, 9, 2), f"{VS}:plate")
    m.cbox("base", (0, 12, 6.5), (6, 1, 2), "rubber:rubber")
    m.cbox("base", (5, 3, 2), (3, 2, 3), f"{LAMP}:lamp")
    m.bone("arm", "base", pivot=(0, 14, -3))
    m.box("arm", (-1.5, 13, -5), (3, 2, 13), f"{VS}:rivet")
    m.box("arm", (-1, 12, -1), (2, 1, 6), "vantite:plate")
    m.bone("cup", "arm", pivot=(0, 15, 5))
    m.box("cup", (-3, 15, 2), (6, 1, 6), f"{VS}:plate")
    m.box("cup", (-3, 16, 2), (6, 3, 1), f"{VS}:grate")
    m.box("cup", (-3, 16, 7), (6, 3, 1), f"{VS}:grate")
    m.box("cup", (-3, 16, 3), (1, 3, 4), f"{VS}:grate")
    m.box("cup", (2, 16, 3), (1, 3, 4), f"{VS}:grate")
    throw = m.anim("throw", "once", 1.0)
    throw.keys("arm", "rotation", [(0, (0, 0, 0)), (0.05, (-6, 0, 0)), (0.18, (110, 0, 0)), (0.3, (104, 0, 0)),
                                   (0.4, (110, 0, 0)), (0.85, (8, 0, 0)), (1.0, (0, 0, 0))])
    throw.keys("cup", "rotation", [(0, (0, 0, 0)), (0.18, (25, 0, 0)), (0.3, (0, 0, 0)), (1.0, (0, 0, 0))])
    idle = m.anim("idle", "loop", 1.0)
    idle.osc("arm", "rotation", (1.0, 0, 0))
    return m


def outpost_core() -> Model:
    """2x2x2 compact colony core: vantite plinth, ribbed spire, four glow fins and a green orb."""
    m = Model("outpost_core")
    m.bone("base")
    rnd(m, "base", (0, 0, 0), 30, 3, f"{VS}:panel:8")
    rnd(m, "base", (0, 3, 0), 26, 1, f"{LAMP}:glow")
    rnd(m, "base", (0, 3, 0), 24, 3, f"{VS}:rivet")
    for k in range(4):
        a = math.radians(45 + 90 * k)
        strut(m, "base", (12 * math.sin(a), 1.5, -12 * math.cos(a)), (7 * math.sin(a), 7, -7 * math.cos(a)), 3, "vantite:plate")
    m.bone("spire", "base", pivot=(0, 6, 0))
    rnd(m, "spire", (0, 6, 0), 8, 12, f"{VS}:rivet")
    for y in (9, 13):
        rnd(m, "spire", (0, y, 0), 9, 1, "vantite:plate")
    rnd(m, "spire", (0, 18, 0), 6, 4, "vantite:plate")
    for k in range(4):
        a = math.radians(90 * k)
        strut(m, "spire", (2.5 * math.sin(a), 20, -2.5 * math.cos(a)), (5 * math.sin(a), 28, -5 * math.cos(a)), 1, f"{VS}:plate")
    m.bone("fins", "spire", pivot=(0, 6, 0))
    radial(m, "fins", (0, 6, 0), 9, 4, (7, 10, 1), f"{VS}:seam:{LAMP}", phase=45)
    radial(m, "fins", (0, 16, 0), 8, 4, (5, 6, 1), "vantite:plate", phase=45)
    radial(m, "fins", (0, 22, 0), 7, 4, (3, 3, 1), "vantite:plate", phase=45)
    m.bone("orb", "spire", pivot=(0, 26, 0))
    rnd(m, "orb", (0, 23, 0), 8, 6, f"{LAMP}:glow")
    m.cbox("orb", (0, 22, 0), (6, 8, 6), f"{LAMP}:glow")
    grow = m.anim("grow", "hold", 10.0)
    grow.keys("base", "scale", [(0, (0, 0, 0)), (0.5, (0.2, 0.05, 0.2)), (3.0, (1, 1, 1)), (10.0, (1, 1, 1))])
    grow.keys("spire", "position", [(0, (0, -24, 0)), (3.0, (0, -24, 0)), (6.0, (0, 1, 0)), (6.5, (0, 0, 0)), (10.0, (0, 0, 0))])
    grow.keys("fins", "scale", [(0, (1, 0, 1)), (5.5, (1, 0, 1)), (8.0, (1, 1, 1)), (10.0, (1, 1, 1))])
    grow.keys("fins", "rotation", [(0, (0, -90, 0)), (5.5, (0, -90, 0)), (8.0, (0, 0, 0)), (10.0, (0, 0, 0))])
    grow.keys("orb", "scale", [(0, (0, 0, 0)), (7.0, (0, 0, 0)), (9.0, (1.25, 1.25, 1.25)), (10.0, (1, 1, 1))])
    active = m.anim("active", "loop", 3.0)
    active.osc("orb", "scale", (0.12, 0.12, 0.12), base=(1, 1, 1), cycles=2)
    active.spin("orb", "y", 1)
    active.spin("fins", "y", 0.25, steps=4)
    return m


MODELS = {
    "hv_pylon": hv_pylon,
    "hydro_turbine": hydro_turbine,
    "wind_tower": wind_tower,
    "magma_tap": magma_tap,
    "froth_flotation_plant": froth_flotation_plant,
    "cable_car_station": cable_car_station,
    "crate_launcher": crate_launcher,
    "outpost_core": outpost_core,
}

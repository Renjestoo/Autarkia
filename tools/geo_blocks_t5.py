"""GeckoLib block models of tier 5 (matter): lattice violet frames, magenta glow, frost and nanite silver."""

import math

from geo_core import Model

GLOW = "#FF5CF0"
VIOLET = "#B39DFF"
EPS = 0.02


def prism(m: Model, bone: str, base, radius: float, height: int, mat: str, sides: int = 6, axis: str = "y",
          faces: dict | None = None) -> None:
    """Add a (2 * sides)-gon prism of rotated boxes; base is the centre of its start face on the axis.

    Boxes are offset by EPS along the axis so coplanar end faces never z-fight.
    """
    cx, cy, cz = base
    w = max(1, round(2 * radius * math.tan(math.pi / (2 * sides))))
    d = max(1, round(2 * radius))
    for k in range(sides):
        a, e = 180.0 * k / sides, k * EPS
        if axis == "y":
            o, s, rot = (cx - w / 2, cy + e, cz - d / 2), (w, height, d), (0, a, 0)
        elif axis == "z":
            o, s, rot = (cx - w / 2, cy - d / 2, cz + e), (w, d, height), (0, 0, a)
        else:
            o, s, rot = (cx + e, cy - d / 2, cz - w / 2), (height, d, w), (a, 0, 0)
        m.box(bone, o, s, mat, faces=faces, rotation=rot if a else None, pivot=base if a else None)


def ring(m: Model, bone: str, center, radius: float, segs: int, tan: int, rad: int, ax: int, mat: str,
         axis: str = "y", phase: float = 0.0) -> None:
    """Add a ring of segs boxes (tangential x radial x axial size) around an axis through center."""
    cx, cy, cz = center
    for k in range(segs):
        a = phase + 360.0 * k / segs
        if axis == "y":
            o, s, rot = (cx + radius - rad / 2, cy - ax / 2, cz - tan / 2), (rad, ax, tan), (0, a, 0)
        elif axis == "z":
            o, s, rot = (cx + radius - rad / 2, cy - tan / 2, cz - ax / 2), (rad, tan, ax), (0, 0, a)
        else:
            o, s, rot = (cx - ax / 2, cy + radius - rad / 2, cz - tan / 2), (ax, rad, tan), (a, 0, 0)
        turned = abs(a % 360) > 1e-9
        m.box(bone, o, s, mat, rotation=rot if turned else None, pivot=center if turned else None)


def plasma_generator() -> Model:
    """Tokamak: violet coil frames around a magenta plasma torus inside a lattice frame."""
    m = Model("plasma_generator")
    m.bone("casing")
    m.cbox("casing", (0, 0, 0), (48, 4, 48), "lattice:panel")
    m.cbox("casing", (0, 4, 0), (40, 2, 40), "lattice:rivet", faces={"up": "lattice:hex"})
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.cbox("casing", (sx * 21.5, 4, sz * 21.5), (5, 40, 5), "lattice:rivet")
            m.cbox("casing", (sx * 21.5, 20, sz * 21.5), (6, 2, 6), f"{GLOW}:glow")
    for sz in (-1, 1):
        m.cbox("casing", (0, 44, sz * 21.5), (48, 4, 5), "lattice:panel")
    for sx in (-1, 1):
        m.cbox("casing", (sx * 21.5, 44, 0), (5, 4, 38), "lattice:panel")
    m.cbox("casing", (0, 42, 0), (38, 2, 4), "nanite:plate")
    m.cbox("casing", (0, 42, 0), (4, 2, 38), "nanite:plate")
    m.cbox("casing", (0, 6, -19), (12, 7, 4), "lattice:panel", faces={"north": f"lattice:screen:{GLOW}"})
    for k in range(4):
        a = 45 + 90 * k
        m.box("casing", (12.5, 6, -1.5), (3, 13, 3), "nanite:plate", rotation=(0, a, 0), pivot=(0, 6, 0))
    m.bone("torus", "casing", pivot=(0, 24, 0))
    ring(m, "torus", (0, 24, 0), 14, 12, 8, 10, 10, "lattice:pane")
    ring(m, "torus", (0, 24, 0), 14, 12, 1, 12, 12, "nanite:rivet", phase=15)
    m.bone("plasma", "casing", pivot=(0, 24, 0))
    ring(m, "plasma", (0, 24, 0), 14, 16, 7, 6, 6, f"{GLOW}:energy")
    m.bone("coils", "casing", pivot=(0, 24, 0))
    prism(m, "coils", (0, 6, 0), 3.5, 36, "lattice:coil", sides=4)
    for k in range(8):
        a = 22.5 + 45 * k
        for o, s in (((7, 17, -1), (14, 2, 2)), ((7, 29, -1), (14, 2, 2)), ((7, 19, -1), (2, 10, 2)), ((19, 19, -1), (2, 10, 2))):
            m.box("coils", o, s, "lattice:coil", rotation=(0, a, 0), pivot=(0, 24, 0))
        m.box("coils", (21, 23, -0.5), (1, 2, 1), f"{GLOW}:glow", rotation=(0, a, 0), pivot=(0, 24, 0))
    run = m.anim("run", "loop", 2.0)
    run.spin("plasma", "y", 1.0, steps=12)
    run.osc("plasma", "scale", (0.04, 0.22, 0.04), cycles=4, steps=16)
    run.osc("coils", "scale", (0.01, 0.0, 0.01), cycles=4, steps=16)
    idle = m.anim("idle", "loop", 1.0)
    idle.spin("plasma", "y", 1.0 / 16, steps=4)
    idle.osc("plasma", "scale", (0.02, 0.08, 0.02), base=(0.94, 0.7, 0.94), steps=8)
    return m


def cryo_node() -> Model:
    """Pole with white coolant pipes carrying a frosted cryo head; frost pulses."""
    m = Model("cryo_node")
    m.bone("pole")
    m.cbox("pole", (0, 0, 0), (14, 3, 14), "lattice:rivet")
    m.cbox("pole", (0, 3, 0), (10, 2, 10), "lattice:panel")
    m.cbox("pole", (0, 5, 0), (4, 29, 4), "lattice:plate")
    for sx in (-1, 1):
        m.cbox("pole", (sx * 4, 5, -1), (2, 28, 2), "frost:plate")
        m.cbox("pole", (sx * 4, 4, -1), (3, 2, 3), "nanite:plate")
    for y in (12, 22):
        m.cbox("pole", (0, y, -0.5), (10, 2, 3), "nanite:rivet")
    m.cbox("pole", (0, 30, 0), (6, 2, 6), "nanite:plate")
    m.bone("head", "pole", pivot=(0, 39, 0))
    m.cbox("head", (0, 32, 0), (12, 3, 12), "lattice:rivet")
    m.cbox("head", (0, 35, 0), (10, 9, 10), "nanite:panel:5")
    m.cbox("head", (0, 38, 0), (11, 2, 11), "frost:glow")
    for sx in (-1, 1):
        m.cbox("head", (sx * 5.5, 36, 0), (1, 6, 4), "nanite:vent")
        m.cbox("head", (0, 36, sx * 5.5), (4, 6, 1), "nanite:vent")
    m.cbox("head", (0, 44, 0), (12, 2, 12), "lattice:rivet")
    m.cbox("head", (0, 46, 0), (4, 2, 4), "frost:crystal")
    m.bone("frost", "head", pivot=(0, 39, 0))
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.cbox("frost", (sx * 5.5, 35, sz * 5.5), (2, 7, 2), "frost:frost")
            m.cbox("frost", (sx * 5, 30, sz * 5), (1, 2, 1), "frost:crystal")
            m.cbox("frost", (sx * 3, 29, sz * 5), (1, 3, 1), "frost:crystal")
        m.cbox("frost", (sx * 4, 30, -1), (3, 3, 3), "frost:frost")
    m.cbox("frost", (0, 46, 0), (10, 1, 10), "frost:frost", faces={"up": "frost:crystal"})
    idle = m.anim("idle", "loop", 2.0)
    idle.osc("frost", "scale", (0.06, 0.04, 0.06), steps=8)
    warm = m.anim("warm", "loop", 2.0)
    warm.keys("frost", "scale", [(0, (0.02, 0.02, 0.02)), (2.0, (0.02, 0.02, 0.02))])
    return m


def superconducting_coil() -> Model:
    """Cryostat drum with exposed copper-violet winding and frost crust."""
    m = Model("superconducting_coil")
    m.bone("casing")
    m.cbox("casing", (0, 0, 0), (32, 3, 32), "lattice:panel")
    prism(m, "casing", (0, 3, 0), 13, 7, "lattice:rivet")
    prism(m, "casing", (0, 21, 0), 13, 7, "lattice:rivet")
    prism(m, "casing", (0, 28, 0), 11, 2, "nanite:plate")
    for k in range(4):
        m.box("casing", (12, 10, -1), (2, 11, 2), "nanite:plate", rotation=(0, 45 + 90 * k, 0), pivot=(0, 10, 0))
    m.cbox("casing", (0, 30, 0), (4, 2, 4), "nanite:rivet")
    for sx in (-1, 1):
        m.cbox("casing", (sx * 6, 30, 0), (2, 1, 2), "frost:plate")
    m.box("casing", (-2.5, 23, -14), (5, 5, 1), "nanite:gauge")
    m.box("casing", (-2, 5, -14), (4, 2, 1), f"{GLOW}:glow")
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.cbox("casing", (sx * 14, 3, sz * 14), (4, 2, 4), "nanite:plate")
    m.bone("coil", "casing", pivot=(0, 15.5, 0))
    prism(m, "coil", (0, 10, 0), 12, 11, "copper:coil")
    for y in (12, 18):
        prism(m, "coil", (0, y, 0), 12.5, 1, "lattice:plate")
    prism(m, "coil", (0, 15, 0), 12.5, 1, f"{GLOW}:glow")
    m.bone("frost", "casing", pivot=(0, 16, 0))
    prism(m, "frost", (0, 9, 0), 13.5, 1, "frost:frost")
    prism(m, "frost", (0, 21, 0), 13.5, 1, "frost:frost")
    prism(m, "frost", (0, 30, 0), 9, 1, "frost:frost", sides=4)
    for k in range(8):
        m.box("frost", (12.5, 19, -0.5), (1, 2, 1), "frost:crystal", rotation=(0, 22.5 + 45 * k, 0), pivot=(0, 19, 0))
    idle = m.anim("idle", "loop", 2.0)
    idle.osc("frost", "scale", (0.02, 0.03, 0.02), steps=8)
    idle.osc("coil", "scale", (0.01, 0.0, 0.01), cycles=1, steps=8)
    charge = m.anim("charge", "loop", 2.0)
    charge.osc("coil", "scale", (0.05, 0.0, 0.05), cycles=4, steps=16)
    charge.osc("frost", "scale", (0.05, 0.1, 0.05), base=(1.03, 1.05, 1.03), cycles=2, steps=16)
    warm = m.anim("warm", "loop", 2.0)
    warm.keys("frost", "scale", [(0, (0.02, 0.02, 0.02)), (2.0, (0.02, 0.02, 0.02))])
    warm.osc("coil", "scale", (0.01, 0.0, 0.01), cycles=1, steps=8)
    return m


def plasma_separator() -> Model:
    """Glass arc chamber whose nozzles pour coloured streams into three receivers."""
    m = Model("plasma_separator")
    m.bone("body")
    m.cbox("body", (0, 0, 0), (32, 3, 32), "lattice:panel")
    m.cbox("body", (0, 3, 10), (28, 22, 12), "lattice:rivet", faces={"north": "lattice:panel:7"})
    m.cbox("body", (0, 25, 10), (20, 3, 8), "nanite:plate")
    m.box("body", (-10, 16, 3), (6, 6, 1), f"lattice:screen:{GLOW}")
    m.box("body", (5, 16, 3), (5, 5, 1), "nanite:gauge")
    prism(m, "body", (0, 3, -3), 7, 5, "lattice:rivet")
    streams = {-10: "#FF9A3C", 0: "#FCD34B", 10: "#7FC9AF"}
    for x, c in streams.items():
        m.cbox("body", (x, 3, -12.5), (6, 3, 6), "nanite:rivet")
        m.cbox("body", (x, 4, -12.5), (4, 2, 4), f"{c}:glow")
    m.cbox("body", (0, 9, -10.5), (2, 2, 4), "nanite:plate")
    m.cbox("body", (0, 9, -12.5), (2, 1, 2), "lattice:plate")
    for sx in (-1, 1):
        m.cbox("body", (sx * 7.5, 9, -3), (3, 2, 2), "nanite:plate")
        m.cbox("body", (sx * 10, 9, -7.5), (2, 2, 11), "nanite:plate")
        m.cbox("body", (sx * 10, 8, -12.5), (2, 1, 2), "lattice:plate")
    m.bone("chamber", "body", pivot=(0, 8, -3))
    prism(m, "chamber", (0, 8, -3), 6, 14, "lattice:pane")
    for k in range(6):
        m.box("chamber", (5.5, 9, -4), (1, 13, 2), "nanite:plate", rotation=(0, 30 + 60 * k, 0), pivot=(0, 9, -3))
    prism(m, "chamber", (0, 8, -3), 6.5, 1, "nanite:plate")
    prism(m, "chamber", (0, 22, -3), 7, 3, "lattice:rivet")
    m.cbox("chamber", (0, 25, -3), (4, 2, 4), "nanite:rivet")
    m.cbox("chamber", (0, 18, -3), (2, 4, 2), "nanite:plate")
    m.cbox("chamber", (0, 9, -3), (2, 4, 2), "nanite:plate")
    m.cbox("chamber", (0, 17, -3), (1, 1, 1), "lattice:crystal")
    m.cbox("chamber", (0, 13, -3), (1, 1, 1), "lattice:crystal")
    m.bone("arc", "chamber", pivot=(0, 15.5, -3))
    m.cbox("arc", (0, 13, -3), (1, 5, 1), f"{GLOW}:glow")
    m.cbox("arc", (0, 14, -3), (3, 3, 3), f"{GLOW}:energy")
    m.box("arc", (-0.5, 13.5, -3.5), (1, 4, 1), f"{VIOLET}:glow", rotation=(0, 0, 35), pivot=(0, 15.5, -3))
    m.box("arc", (-0.5, 13.5, -3.5), (1, 4, 1), f"{VIOLET}:glow", rotation=(35, 0, 0), pivot=(0, 15.5, -3))
    m.bone("nozzles", "body", pivot=(0, 8, -12))
    for x, c in streams.items():
        m.cbox("nozzles", (x, 5, -12.5), (1, 3, 1), f"{c}:glow")
    work = m.anim("work", "loop", 1.0)
    flick = [(1, 1, 1), (1.6, 0.9, 1.6), (0.6, 1.1, 0.6), (1.3, 1, 1.3), (0.8, 0.9, 0.8), (1.5, 1.1, 1.5),
             (0.7, 1, 0.7), (1.2, 0.95, 1.2), (1, 1, 1)]
    work.keys("arc", "scale", [(k / 8, v) for k, v in enumerate(flick)])
    work.spin("arc", "y", 1.0, steps=8)
    work.keys("nozzles", "scale", [(k / 8, (1, 1.0 if k % 2 == 0 else 0.85, 1)) for k in range(9)])
    idle = m.anim("idle", "loop", 1.0)
    idle.osc("arc", "scale", (0.08, 0.05, 0.08), base=(0.45, 0.6, 0.45), steps=8)
    idle.keys("nozzles", "scale", [(0, (1, 0.05, 1)), (1.0, (1, 0.05, 1))])
    return m


def nanite_hive() -> Model:
    """Silver hexagonal hive cells on a lattice base; the top vent lid swings open on release."""
    m = Model("nanite_hive")
    m.bone("shell")
    m.cbox("shell", (0, 0, 0), (32, 4, 32), "lattice:panel")
    m.cbox("shell", (0, 4, 0), (28, 2, 28), "lattice:rivet", faces={"up": "lattice:hex"})
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.cbox("shell", (sx * 13.5, 6, sz * 13.5), (3, 8, 3), "lattice:rivet")
            m.cbox("shell", (sx * 13.5, 14, sz * 13.5), (3, 1, 3), f"{GLOW}:glow")
    m.box("shell", (-4, 1, -16.5), (8, 2, 1), f"{GLOW}:glow")
    m.bone("cells", "shell", pivot=(0, 6, 0))
    heights = [14, 11, 16, 12, 15, 10]
    centers = [(0.0, 0.0, 19)] + [(9 * math.cos(math.radians(30 + 60 * k)), 9 * math.sin(math.radians(30 + 60 * k)), heights[k])
                                  for k in range(6)]
    for x, z, h in centers:
        prism(m, "cells", (x, 6, z), 4, h, "nanite:flat:3", sides=3, faces={"up": "nanite:hex"})
        prism(m, "cells", (x, 6 + h - 3, z), 4.4, 1, "lattice:plate", sides=3)
        prism(m, "cells", (x, 6, z), 4.4, 1, "lattice:plate", sides=3)
        if h < 19:
            m.cbox("cells", (x, 6 + h, z), (2, 1, 2), f"{GLOW}:glow")
    m.cbox("cells", (0, 25, 0), (4, 1, 4), f"{GLOW}:energy")
    m.bone("vent", "cells", pivot=(0, 26, 4.5))
    prism(m, "vent", (0, 26, 0), 4.5, 2, "nanite:rivet", sides=3, faces={"up": "lattice:hex"})
    m.cbox("vent", (0, 28, -2), (2, 1, 2), "lattice:plate")
    m.cbox("vent", (0, 26, 4), (6, 1, 1), "lattice:plate")
    release = m.anim("release", "loop", 2.0)
    release.keys("vent", "rotation", [(0, (0, 0, 0)), (0.35, (-75, 0, 0)), (1.6, (-75, 0, 0)), (2.0, (0, 0, 0))])
    release.keys("cells", "scale", [(0, (1, 1, 1)), (0.35, (1.02, 1.05, 1.02)), (1.0, (1, 1, 1)), (1.6, (1.02, 1.05, 1.02)),
                                    (2.0, (1, 1, 1))])
    idle = m.anim("idle", "loop", 2.0)
    idle.osc("cells", "scale", (0.01, 0.02, 0.01), steps=8)
    idle.keys("vent", "rotation", [(0, (0, 0, 0)), (0.9, (0, 0, 0)), (1.0, (-4, 0, 0)), (1.1, (0, 0, 0)), (2.0, (0, 0, 0))])
    return m


def matter_buffer() -> Model:
    """Lattice frame around a caged glass sphere with a swirling violet matter vortex."""
    m = Model("matter_buffer")
    m.bone("frame")
    m.cbox("frame", (0, 0, 0), (32, 4, 32), "lattice:panel")
    m.cbox("frame", (0, 28, 0), (32, 4, 32), "lattice:panel")
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.cbox("frame", (sx * 13.5, 4, sz * 13.5), (5, 24, 5), "lattice:rivet")
            m.cbox("frame", (sx * 13.5, 15, sz * 13.5), (6, 2, 6), f"{GLOW}:glow")
    prism(m, "frame", (0, 4, 0), 7, 2, "nanite:plate")
    prism(m, "frame", (0, 26, 0), 7, 2, "nanite:plate")
    for r, y0, h in ((7, 6, 3), (10, 9, 14), (7, 23, 3)):
        prism(m, "frame", (0, y0, 0), r, h, "lattice:glint")
    ring(m, "frame", (0, 16, 0), 10.5, 16, 5, 1, 1, "nanite:plate", axis="y")
    ring(m, "frame", (0, 16, 0), 10.5, 16, 5, 1, 1, "nanite:plate", axis="x")
    ring(m, "frame", (0, 16, 0), 10.5, 16, 5, 1, 1, "nanite:plate", axis="z")
    m.bone("swirl", "frame", pivot=(0, 16, 0))
    for strand in (0, 120, 240):
        for k in range(12):
            t = k / 11
            r, y, a = 8 - 4 * t, 7 + 17 * t, strand + 40 * k
            m.box("swirl", (r - 1, y, -1.5), (2, 1, 3), f"{VIOLET}:glow" if k % 3 else f"{GLOW}:glow",
                  rotation=(0, a, 0), pivot=(0, y, 0))
    m.bone("core", "frame", pivot=(0, 16, 0))
    m.cbox("core", (0, 13, 0), (6, 6, 6), f"{GLOW}:energy")
    m.box("core", (-3, 13, -3), (6, 6, 6), "lattice:energy", rotation=(45, 45, 0), pivot=(0, 16, 0))
    swirl = m.anim("swirl", "loop", 3.0)
    swirl.spin("swirl", "y", 2.0, steps=16)
    swirl.spin("core", "y", -1.0, steps=12)
    swirl.osc("core", "scale", (0.12, 0.12, 0.12), cycles=3, steps=12)
    return m


def replicator_foundry() -> Model:
    """Large lattice print bay: glazed front doors, gantry printer head over a moving bed."""
    m = Model("replicator_foundry")
    m.bone("frame")
    m.cbox("frame", (0, 0, 0), (48, 4, 48), "lattice:panel")
    m.cbox("frame", (0, 4, 21), (48, 40, 6), "lattice:rivet", faces={"north": "lattice:panel"})
    for sx in (-1, 1):
        m.cbox("frame", (sx * 21, 4, -21), (6, 40, 6), "lattice:rivet", faces={"north": f"lattice:seam:{GLOW}"})
        m.cbox("frame", (sx * 21, 4, 15), (6, 40, 6), "lattice:rivet")
        m.cbox("frame", (sx * 21, 4, -3), (6, 8, 30), "lattice:panel")
        m.cbox("frame", (sx * 21, 38, -3), (6, 6, 30), "lattice:panel")
        m.cbox("frame", (sx * 23.5, 12, -3), (1, 26, 30), "lattice:pane:frame")
        m.cbox("frame", (sx * 20.5, 12, -3), (1, 26, 2), "nanite:plate")
        m.cbox("frame", (sx * 16, 40, 0), (2, 2, 36), "nanite:plate")
    m.cbox("frame", (0, 44, 0), (48, 3, 48), "lattice:panel")
    for sx in (-1, 1):
        m.cbox("frame", (sx * 10, 47, 4), (12, 1, 12), "nanite:grate")
    m.cbox("frame", (0, 47, -14), (30, 1, 2), f"{GLOW}:glow")
    m.cbox("frame", (0, 36, -20.5), (36, 8, 5), "lattice:rivet")
    m.box("frame", (-10, 39, -24), (20, 2, 1), f"{GLOW}:glow")
    m.cbox("frame", (0, 38, 2), (32, 2, 4), "nanite:plate")
    m.bone("bed", "frame", pivot=(0, 4, 1))
    m.cbox("bed", (0, 4, 1), (24, 6, 24), "lattice:rivet")
    m.cbox("bed", (0, 10, 1), (22, 1, 22), "nanite:panel:4")
    m.cbox("bed", (0, 11, 1), (8, 6, 8), "lattice:panel")
    m.cbox("bed", (0, 17, 1), (8, 1, 8), f"{GLOW}:glow")
    m.cbox("bed", (5.5, 11, 6), (3, 3, 3), "nanite:plate")
    m.bone("printer_head", "frame", pivot=(0, 38, 2))
    m.cbox("printer_head", (0, 34, 2), (8, 4, 6), "lattice:rivet")
    m.cbox("printer_head", (0, 24, 2), (4, 10, 4), "nanite:plate")
    m.cbox("printer_head", (0, 21, 2), (2, 3, 2), f"{GLOW}:glow")
    for sx in (-1, 1):
        m.cbox("printer_head", (sx * 3, 28, 2), (2, 4, 2), "nanite:vent")
    m.bone("doors", "frame", pivot=(0, 4, -22.5))
    for sx in (-1, 1):
        cx = sx * 9
        m.cbox("doors", (cx + sx * 7.5, 4, -22.5), (3, 32, 2), "lattice:rivet")
        m.cbox("doors", (cx - sx * 7.5, 4, -22.5), (3, 32, 2), "lattice:rivet")
        m.cbox("doors", (cx, 33, -22.5), (12, 3, 2), "lattice:rivet")
        m.cbox("doors", (cx, 4, -22.5), (12, 6, 2), "lattice:panel")
        m.cbox("doors", (cx, 10, -22.5), (12, 23, 1), "lattice:pane")
        m.cbox("doors", (sx * 2.5, 17, -23.5), (1, 6, 1), "nanite:plate")
    pr = m.anim("print", "loop", 3.0)
    pr.keys("printer_head", "position", [(k * 0.375, ((-7 if k % 2 == 0 else 7), 0, 0)) for k in range(9)])
    pr.osc("bed", "position", (0, 0, 4), steps=12)
    idle = m.anim("idle", "loop", 2.0)
    idle.osc("printer_head", "position", (0, 0.6, 0), steps=8)
    return m


MODELS = {"plasma_generator": plasma_generator, "cryo_node": cryo_node, "superconducting_coil": superconducting_coil,
          "plasma_separator": plasma_separator, "nanite_hive": nanite_hive, "matter_buffer": matter_buffer,
          "replicator_foundry": replicator_foundry}

"""GeckoLib entity models: the cable-car gondola and the rolling stone flywheel.

Origins sit between the feet (bottom centre); the gondola hangs from a cable at y = +40 px.
"""

import math

from geo_core import Model


def gondola() -> Model:
    """Open-top riveted ore gondola, 2 blocks wide, with bottom-dump doors and a hanger yoke to the cable."""
    m = Model("gondola")
    m.bone("root")
    m.bone("hanger", "root", pivot=(0, 40, 0))
    for x in (-14, 14):
        m.cbox("hanger", (x, 14, 0), (2, 8, 2), "vantite_steel:rivet")
    m.cbox("hanger", (0, 22, 0), (30, 2, 2), "vantite_steel:rivet")
    m.cbox("hanger", (0, 24, 0), (2, 12, 2), "vantite_steel:plate")
    m.cbox("hanger", (0, 36, 0), (4, 3, 10), "vantite_steel:rivet", faces={"east": "hazard:hazard", "west": "hazard:hazard"})
    m.cbox("hanger", (0, 39, 0), (4, 1, 10), "vantite_steel:plate")
    for z in (-3, 3):
        m.box("hanger", (-1.5, 40, z - 1.5), (3, 3, 3), "steel:plate", faces={"east": "steel:rings", "west": "steel:rings"})
    m.bone("cabin", "hanger", pivot=(0, 0, 0))
    wall = "vantite_steel:plate"
    m.box("cabin", (-16, 0, -10), (32, 14, 1), wall)
    m.box("cabin", (-16, 0, 9), (32, 14, 1), wall)
    m.box("cabin", (15, 0, -9), (1, 14, 18), wall)
    m.box("cabin", (-16, 0, -9), (1, 14, 18), wall)
    m.box("cabin", (-17, 13, -11), (34, 1, 2), "vantite_steel:plate", faces={"north": "hazard:hazard"})
    m.box("cabin", (-17, 13, 9), (34, 1, 2), "vantite_steel:plate", faces={"south": "hazard:hazard"})
    m.box("cabin", (-17, 13, -9), (2, 1, 18), "vantite_steel:plate")
    m.box("cabin", (15, 13, -9), (2, 1, 18), "vantite_steel:plate")
    for x in (-12, -4, 4, 12):
        m.cbox("cabin", (x, 0, -10.5), (2, 13, 1), "vantite_steel:rivet")
        m.cbox("cabin", (x, 0, 10.5), (2, 13, 1), "vantite_steel:rivet")
    for z in (-5, 5):
        m.cbox("cabin", (-16.5, 0, z), (1, 13, 2), "vantite_steel:rivet")
        m.cbox("cabin", (16.5, 0, z), (1, 13, 2), "vantite_steel:rivet")
    m.box("cabin", (-1, -1, -10), (2, 1, 20), "vantite_steel:rivet")
    m.box("cabin", (-16, -1, -10), (2, 1, 20), "vantite_steel:rivet")
    m.box("cabin", (14, -1, -10), (2, 1, 20), "vantite_steel:rivet")
    m.bone("door_l", "cabin", pivot=(15, 0.5, 0))
    m.box("door_l", (0, 0, -9), (15, 1, 18), "vantite_steel:rivet")
    m.bone("door_r", "cabin", pivot=(-15, 0.5, 0))
    m.box("door_r", (-15, 0, -9), (15, 1, 18), "vantite_steel:rivet")
    m.bone("cargo", "cabin", pivot=(0, 1, 0))
    for (sx, sy, sz), y in (((28, 5, 16), 1), ((22, 3, 12), 6), ((14, 2, 8), 9), ((6, 1, 4), 11)):
        m.cbox("cargo", (0, y, 0), (sx, sy, sz), "stone:cobble")
    for x, y, z, mat in ((-9, 6, -3, "copper:crystal"), (6, 6, 4, "iron:crystal"), (2, 9, -2, "gold:crystal"),
                         (-4, 9, 2, "tin:crystal"), (10, 6, -5, "copper:crystal")):
        m.cbox("cargo", (x, y, z), (2, 1, 2), mat)
    opening = m.anim("doors_open", "once", 3.0)
    opening.keys("door_l", "rotation", [(0, (0, 0, 0)), (0.6, (0, 0, -80)), (0.75, (0, 0, -74)), (0.9, (0, 0, -80)),
                                       (2.2, (0, 0, -80)), (3.0, (0, 0, 0))])
    opening.keys("door_r", "rotation", [(0, (0, 0, 0)), (0.6, (0, 0, 80)), (0.75, (0, 0, 74)), (0.9, (0, 0, 80)),
                                       (2.2, (0, 0, 80)), (3.0, (0, 0, 0))])
    opening.keys("cargo", "position", [(0, (0, 0, 0)), (0.4, (0, 0, 0)), (1.3, (0, -14, 0)), (3.0, (0, -14, 0))])
    opening.keys("cargo", "scale", [(0, (1, 1, 1)), (0.4, (1, 1, 1)), (1.3, (0.6, 0.2, 0.6)), (1.31, (0, 0, 0)),
                                    (3.0, (0, 0, 0))])
    opening.keys("hanger", "rotation", [(0, (0, 0, 0)), (0.7, (0, 0, 2)), (1.4, (0, 0, -1.5)), (2.1, (0, 0, 0.8)),
                                        (3.0, (0, 0, 0))])
    idle = m.anim("idle", "loop", 1.0)
    idle.osc("hanger", "rotation", (0.8, 0, 0.5))
    return m


def flywheel_disc(m: Model, bone: str, cy: float) -> None:
    """Add the 44px stone flywheel disc (axis along X, centre at y=cy) with bronze rim, spokes and hub."""
    piv = (0, cy, 0)
    for k in range(8):
        m.box(bone, (-5, cy - 22, -4.5), (10, 44, 9), "stone:flat", rotation=(22.5 * k, 0, 0) if k else None,
              pivot=piv if k else None, inflate=0.02 * k or None)
    for k in range(16):
        m.box(bone, (-6, cy + 20, -4.5), (12, 2, 9), "bronze:plate", rotation=(22.5 * k + 11.25, 0, 0), pivot=piv,
              inflate=0.01 * (k % 2) or None)
    for side in (5, -6):
        for k in range(4):
            m.box(bone, (side, cy - 20, -1), (1, 40, 2), "bronze:plate", rotation=(45 * k, 0, 0) if k else None,
                  pivot=piv if k else None, inflate=0.01 * k or None)
    m.box(bone, (-7, cy - 4, -2.5), (14, 8, 5), "bronze:rivet", faces={"east": "bronze:rings", "west": "bronze:rings"})
    m.box(bone, (-7, cy - 2.5, -4), (14, 5, 8), "bronze:rivet", faces={"east": "bronze:rings", "west": "bronze:rings"},
          inflate=0.02)
    m.box(bone, (-8, cy - 1, -1), (16, 2, 2), "steel:plate")


def rolling_flywheel() -> Model:
    """The stone flywheel disc on its own; roll spins about local X, wobble settles it flat like a coin."""
    m = Model("rolling_flywheel")
    m.bone("root")
    m.bone("disc", "root", pivot=(0, 22, 0))
    flywheel_disc(m, "disc", 22)
    roll = m.anim("roll", "loop", 1.0)
    roll.spin("disc", "x", 1, steps=16)
    wobble = m.anim("wobble", "once", 2.0)
    frames = [(0.0, 0), (0.3, 18), (0.6, 36), (0.9, 54), (1.2, 70), (1.45, 80), (1.65, 86), (1.85, 89), (2.0, 90)]
    for t, tilt in frames:
        a = math.radians(tilt)
        drop = 22 * math.cos(a) + 5 * math.sin(a) - 22
        wobble.key("disc", "rotation", t, (0, 0, tilt))
        wobble.key("disc", "position", t, (0, drop, 0))
        wobble.key("root", "rotation", t, (0, 540 * (1 - (1 - t / 2.0) ** 2), 0))
    return m


MODELS = {"gondola": gondola, "rolling_flywheel": rolling_flywheel}

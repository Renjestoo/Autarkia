"""GeckoLib block models of tier 3 (electric works): steel bodies with cyan indicator accents."""

import numpy as np

from geo_core import Model
from geo_shapes_t23 import circle_mask, extrude, lattice_mast, radial, ring_box, strut

CYAN = "#4FE3FF:glow"


def steel_pylon() -> Model:
    """Steel lattice mast on a brick foot with a braced crossarm, ceramic insulators and a cyan beacon."""
    m = Model("steel_pylon")
    m.bone("mast")
    m.cbox("mast", (0, 0, 0), (12, 5, 12), "brick:brick")
    m.cbox("mast", (0, 5, 0), (14, 1, 14), "foundation:stone")
    m.cbox("mast", (0, 6, 0), (11, 1, 11), "steel:rivet")
    lattice_mast(m, "mast", 7, 72, 4, 8, "steel:plate", "steel:flat")
    m.cbox("mast", (0, 72, 0), (10, 2, 10), "steel:rivet")
    m.bone("beacon", "mast", pivot=(0, 75, 0))
    m.cbox("beacon", (0, 74, 0), (2, 2, 2), CYAN)
    m.bone("arm", "mast", pivot=(0, 66, 0))
    for y in (66, 52):
        m.box("arm", (-8, y, -1.5), (16, 3, 3), "steel:rivet")
        for s in (-1, 1):
            strut(m, "arm", (4 * s, y - 7, 0), (7.5 * s, y, 0), 1, "steel:flat")
        m.box("arm", (-8, y + 1, -2), (2, 1, 1), "hazard:hazard")
        m.box("arm", (6, y + 1, -2), (2, 1, 1), "hazard:hazard")
    m.bone("insulators", "arm", pivot=(0, 69, 0))
    for y0 in (69, 55):
        for x in (-6.5, 6.5):
            for w, y in ((3, y0), (2, y0 + 1), (3, y0 + 2), (2, y0 + 3), (3, y0 + 4)):
                m.cbox("insulators", (x, y, 0), (w, 1, w), "ceramic:plate")
            m.cbox("insulators", (x, y0 + 5, 0), (1, 1, 1), "iron:plate")
    m.anim("idle", "loop", 1.0).keys("beacon", "scale", [(0, (1, 1, 1)), (0.45, (1, 1, 1)), (0.5, (0.3, 0.3, 0.3)),
                                                          (0.7, (0.3, 0.3, 0.3)), (0.75, (1, 1, 1)), (1.0, (1, 1, 1))])
    return m


def _turbine_mask(r: float, cut: float = 0.0) -> np.ndarray:
    """Return the 46x43 casing silhouette (half-cylinder over a rectangle), optionally with a round port."""
    yy, xx = np.mgrid[0:43, 0:46]
    xc = xx - 22.5
    yc = 45.5 - yy
    inside = (yc < 24) | (xc ** 2 + (yc - 24) ** 2 < r * r)
    if cut:
        inside &= xc ** 2 + (yc - 24) ** 2 >= cut * cut
    return inside


def steam_turbine() -> Model:
    """Half-cylinder steel casing with a bladed rotor behind a front grille and intake pipes at the back."""
    m = Model("steam_turbine")
    m.bone("casing")
    m.cbox("casing", (0, 0, 0), (48, 3, 30), "steel:rivet")
    extrude(m, "casing", _turbine_mask(23), "z", (0, 24.5, 1), 22, "steel:panel", faces={"north": "steel:dark"})
    extrude(m, "casing", _turbine_mask(23, 17), "z", (0, 24.5, -11.5), 3, "steel:rivet")
    extrude(m, "casing", circle_mask(38, 34), "z", (0, 24, -13.5), 1, "brass:plate")
    for y in range(12, 37, 4):
        w = 2 * int(np.sqrt(max(0.0, 17 ** 2 - (y + 0.5 - 24) ** 2)))
        if w > 0:
            m.box("casing", (-w / 2.0, y, -14), (w, 1, 1), "steel:flat")
    m.box("casing", (-0.5, 7, -14), (1, 34, 1), "steel:flat")
    for x in (18, -20):
        m.box("casing", (x, 8, -14), (2, 2, 1), CYAN)
    m.box("casing", (-6, 46, -4), (12, 1, 8), "brass:rivet")
    m.box("casing", (-2, 47, -2), (4, 1, 4), "red_paint:plate")
    m.bone("rotor", "casing", pivot=(0, 24, -11.5))
    extrude(m, "rotor", circle_mask(8), "z", (0, 24, -11.5), 3, "brass:rivet")
    radial(m, "rotor", (0, 24, -11.5), "z", 8, (3, 12, 1), 10, "steel:plate")
    radial(m, "rotor", (0, 24, -10.5), "z", 8, (2, 12, 1), 10, "steel:flat:1.4", phase=9)
    m.box("rotor", (-1, 23, -13.5), (2, 2, 1), "brass:plate")
    m.bone("intake", "casing")
    for x in (-14, 14):
        extrude(m, "intake", circle_mask(6), "y", (x, 22, 13), 38, "copper:plate")
        for y in (6, 20, 34):
            extrude(m, "intake", circle_mask(8), "y", (x, y, 13), 1, "brass:plate")
        m.box("intake", (x - 2, 41, 10), (4, 2, 6), "copper:plate")
    m.box("intake", (16.5, 27, 12), (2, 2, 2), "iron:plate")
    extrude(m, "intake", circle_mask(6, 2), "x", (18.5, 28, 13), 1, "red_paint:plate")
    m.bone("gauge", "casing", pivot=(-18, 29, -14))
    m.box("gauge", (-21, 26, -14), (6, 6, 1), "steel:gauge")
    sp = m.anim("spin", "loop", 1.0)
    sp.spin("rotor", "z", 2, steps=16)
    sp.osc("gauge", "rotation", (0, 0, 1.5), cycles=4)
    m.anim("idle", "loop", 1.0).spin("rotor", "z", 1 / 8, steps=4)
    return m


def jaw_crusher() -> Model:
    """Riveted steel crusher: stepped hopper on top, swinging jaw plate in front, flywheel on one side."""
    m = Model("jaw_crusher")
    m.bone("body")
    m.cbox("body", (0, 0, 0), (32, 3, 32), "steel:rivet")
    for x in (-13, 10):
        m.box("body", (x, 3, -14), (3, 23, 28), "steel:rivet")
        m.box("body", (x - 0.5, 21, -9), (4, 4, 5), "brass:rivet")
    m.box("body", (-10, 3, 10), (20, 23, 4), "steel:panel")
    m.box("body", (-10, 6, 8), (20, 18, 2), "iron:grille")
    m.box("body", (-10, 3, -14), (20, 2, 6), "iron:plate")
    m.box("body", (-13, 23, -7), (26, 2, 2), "iron:plate")
    m.box("body", (10.5, 22, -15), (2, 2, 1), CYAN)
    m.box("body", (-12.5, 22, -15), (2, 2, 1), "#7CFF6B:glow")
    m.bone("jaw", "body", pivot=(0, 24, -6))
    m.box("jaw", (-9, 5, -9), (18, 18, 3), "steel:rivet")
    for y in range(6, 22, 3):
        m.box("jaw", (-9, y, -6), (18, 1, 1), "iron:plate")
    m.box("jaw", (-3, 22, -9), (6, 4, 6), "brass:rivet")
    m.box("jaw", (-6, 9, -10), (12, 1, 1), "hazard:hazard")
    m.bone("flywheel", "body", pivot=(14.5, 15, 2))
    extrude(m, "flywheel", circle_mask(24, 19), "x", (14.5, 15, 2), 3, "rubber:flat:2")
    radial(m, "flywheel", (14.5, 15, 2), "x", 6, (2, 7, 2), 6, "iron:plate")
    extrude(m, "flywheel", circle_mask(6), "x", (14.5, 15, 2), 3, "brass:rivet")
    m.bone("hopper", "body", pivot=(0, 26, 0))
    for o, y in ((20, 26), (24, 28), (28, 30)):
        ring_box(m, "hopper", (0, y, 0), (o, o), (o - 2, o - 2), 2, "steel:plate")
    m.cbox("hopper", (0, 30, 0), (26, 1, 26), "slag:cobble")
    cr = m.anim("crush", "loop", 1.0)
    cr.keys("jaw", "rotation", [(0, (0, 0, 0)), (0.5, (10, 0, 0)), (1.0, (0, 0, 0))])
    cr.spin("flywheel", "x", 1, steps=12)
    cr.osc("hopper", "position", (0, 0.2, 0), cycles=2)
    idle = m.anim("idle", "loop", 1.0)
    idle.spin("flywheel", "x", 1 / 6, steps=4)
    idle.keys("jaw", "rotation", [(0, (0, 0, 0)), (0.5, (1, 0, 0)), (1.0, (0, 0, 0))])
    return m


def fabricator() -> Model:
    """Glass-fronted steel cabinet with an XY gantry, a robotic head and a print bed; cyan lights."""
    m = Model("fabricator")
    m.bone("body")
    m.cbox("body", (0, 0, 0), (32, 6, 16), "steel:panel", faces={"north": "steel:vent"})
    m.box("body", (-16, 28, -7), (32, 4, 15), "steel:rivet")
    for x in (-16, 13):
        m.box("body", (x, 6, -8), (3, 22, 16), "steel:plate")
    m.box("body", (-13, 6, 6), (26, 22, 2), "steel:flat:1.3")
    m.box("body", (-13, 6, -7), (26, 22, 0), "glass:pane:frame")
    m.box("body", (-11, 6, -5), (22, 2, 10), "steel:rivet")
    m.box("body", (-3, 8, -2), (5, 3, 4), "brass:plate")
    m.box("body", (-3, 11, -2), (5, 1, 4), CYAN)
    for z in (-4, 3):
        m.box("body", (-13, 25, z), (26, 1, 1), "steel:plate")
    for i, x in enumerate((-12, -9, -6)):
        m.box("body", (x, 29, -8), (2, 2, 1), "#7CFF6B:glow" if i == 2 else CYAN)
    m.box("body", (4, 29, -8), (8, 2, 1), "steel:screen:#4FE3FF")
    m.bone("gantry", "body", pivot=(0, 24, 0))
    m.box("gantry", (-1.5, 23, -5), (3, 2, 10), "steel:rivet")
    for z in (-5, 3):
        m.box("gantry", (-2, 24, z), (4, 1, 2), "brass:plate")
    m.bone("head", "gantry", pivot=(0, 21, 0))
    m.box("head", (-2, 20, -2), (4, 3, 4), "steel:plate", faces={"north": "#4FE3FF:lamp"})
    m.bone("arm", "head", pivot=(0, 20, 0))
    m.box("arm", (-1, 13, -1), (2, 7, 2), "iron:plate")
    m.box("arm", (-0.5, 11, -0.5), (1, 2, 1), "brass:plate")
    m.box("arm", (-0.5, 10, -0.5), (1, 1, 1), CYAN)
    w = m.anim("work", "loop", 2.0)
    w.keys("gantry", "position", [(0, (0, 0, 0)), (0.5, (-8, 0, 0)), (1.0, (6, 0, 0)), (1.5, (-4, 0, 0)), (2.0, (0, 0, 0))])
    w.keys("head", "position", [(0, (0, 0, 0)), (0.25, (0, 0, 2.5)), (0.75, (0, 0, -2.5)), (1.25, (0, 0, 2)),
                                (1.75, (0, 0, -2)), (2.0, (0, 0, 0))])
    w.keys("arm", "position", [(0, (0, 0, 0)), (0.2, (0, -1.5, 0)), (0.4, (0, 0, 0)), (0.7, (0, -1.5, 0)), (0.9, (0, 0, 0)),
                               (1.2, (0, -1.5, 0)), (1.4, (0, 0, 0)), (1.7, (0, -1.5, 0)), (2.0, (0, 0, 0))])
    m.anim("idle", "loop", 1.0).osc("arm", "position", (0, 0.5, 0))
    return m


def capsule_station() -> Model:
    """Brass cabinet with a glass window and a round tube mouth on top closed by a valve flap."""
    m = Model("capsule_station")
    m.bone("body")
    m.cbox("body", (0, 0, 0), (16, 2, 16), "steel:rivet")
    m.cbox("body", (0, 2, 0), (14, 10, 14), "brass:panel:7")
    m.cbox("body", (0, 12, 0), (15, 1, 15), "steel:plate")
    m.box("body", (-4, 10, -8), (8, 1, 1), "brass:plate")
    m.box("body", (-4, 4, -8), (8, 1, 1), "brass:plate")
    m.box("body", (-4, 5, -8), (1, 5, 1), "brass:plate")
    m.box("body", (3, 5, -8), (1, 5, 1), "brass:plate")
    m.box("body", (-3, 5, -7.5), (6, 5, 1), "glass:glass")
    m.box("body", (-1, 6, -7.3), (2, 3, 1), "bronze:plate")
    m.box("body", (5, 9, -8), (2, 1, 1), CYAN)
    m.box("body", (6, 2, 4), (2, 10, 2), "copper:plate")
    m.bone("mouth", "body", pivot=(0, 13, 0))
    extrude(m, "mouth", circle_mask(10, 6), "y", (0, 14, 0), 2, "brass:plate")
    extrude(m, "mouth", circle_mask(6), "y", (0, 13.5, 0), 1, "rubber:dark")
    m.bone("valve", "mouth", pivot=(0, 15, 4))
    extrude(m, "valve", circle_mask(8), "y", (0, 15.5, 0), 1, "copper:plate")
    m.box("valve", (-1, 15, -5), (2, 1, 1), "copper:plate")
    puff = m.anim("puff", "once", 0.5)
    puff.keys("valve", "rotation", [(0, (0, 0, 0)), (0.06, (-55, 0, 0)), (0.18, (-35, 0, 0)), (0.3, (-12, 0, 0)),
                                    (0.4, (-4, 0, 0)), (0.5, (0, 0, 0))])
    m.anim("idle", "loop", 1.0).keys("valve", "rotation", [(0, (0, 0, 0)), (0.5, (-2, 0, 0)), (0.6, (0, 0, 0)),
                                                           (1.0, (0, 0, 0))])
    return m


MODELS = {"steel_pylon": steel_pylon, "steam_turbine": steam_turbine, "jaw_crusher": jaw_crusher,
          "fabricator": fabricator, "capsule_station": capsule_station}

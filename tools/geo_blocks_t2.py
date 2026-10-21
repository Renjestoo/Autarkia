"""GeckoLib block models of tier 2 (steam and copper)."""

import math

from geo_core import Model
from geo_shapes_t23 import circle_mask, extrude, lattice_mast, radial, strut

GLOW_FIRE = "#FF8A2A:glow"


def waterwheel() -> Model:
    """Timber A-frames carrying a 46px paddle wheel that turns about X."""
    m = Model("waterwheel")
    m.bone("frame")
    for s in (-1, 1):
        x = 7 * s
        m.cbox("frame", (x, 0, 0), (2, 2, 44), "dark_wood:timber")
        for a in (22, -22):
            m.box("frame", (x - 1, 0, -1), (2, 25, 2), "wood:timber", rotation=(a, 0, 0), pivot=(x, 24, 0))
        m.cbox("frame", (x, 9, 0), (2, 2, 12), "dark_wood:timber")
        m.cbox("frame", (6.5 * s, 21, 0), (3, 6, 6), "brass:rivet")
    m.bone("wheel", "frame", pivot=(0, 24, 0))
    rim = circle_mask(42, 37)
    for x in (-4.5, 4.5):
        extrude(m, "wheel", rim, "x", (x, 24, 0), 1, "wood:timber")
        for a in (0, 60, 120):
            m.box("wheel", (x - 0.5, 23, -18), (1, 2, 36), "wood:timber", rotation=(a, 0, 0), pivot=(x, 24, 0))
        extrude(m, "wheel", circle_mask(26, 22), "x", (x, 24, 0), 1, "dark_wood:timber")
    radial(m, "wheel", (0, 24, 0), "x", 12, (10, 7, 2), 19.5, "wood:wood")
    radial(m, "wheel", (0, 24, 0), "x", 12, (10, 1, 2), 15.5, "brass:plate", phase=15)
    extrude(m, "wheel", circle_mask(8), "x", (0, 24, 0), 10, "steel:rivet")
    m.box("wheel", (-8, 23, -1), (16, 2, 2), "iron:plate")
    m.anim("spin", "loop", 3.0).spin("wheel", "x", 1, steps=12)
    m.anim("idle", "loop", 1.0).osc("wheel", "rotation", (2, 0, 0))
    return m


def boiler() -> Model:
    """Brick firebox with a glowing door window under a banded, riveted steel drum and a pressure gauge."""
    m = Model("boiler")
    m.bone("body")
    m.cbox("body", (0, 0, 0), (32, 2, 32), "steel:rivet")
    m.bone("firebox", "body")
    m.cbox("firebox", (0, 2, 0), (30, 12, 30), "brick:brick")
    m.cbox("firebox", (0, 13, 0), (31, 1, 31), "steel:plate")
    for x in (-11, 11):
        m.box("firebox", (x - 1.5, 2, -15.5), (3, 2, 1), "iron:plate")
    extrude(m, "body", circle_mask(28), "y", (0, 28, 0), 28, "steel:flat:2.2")
    for y in (17, 27, 37):
        extrude(m, "body", circle_mask(30, 27), "y", (0, y, 0), 2, "brass:rivet")
    extrude(m, "body", circle_mask(22), "y", (0, 43, 0), 2, "steel:rivet")
    extrude(m, "body", circle_mask(14), "y", (0, 45, 0), 2, "steel:plate")
    m.cbox("body", (0, 46, 0), (4, 2, 4), "brass:plate")
    m.cbox("body", (6, 44, 5), (3, 4, 3), "copper:plate")
    m.cbox("body", (6, 47, 5), (5, 1, 5), "brass:plate")
    for s in (-1, 1):
        m.box("body", (13 * s - 1, 4, -1), (2, 30, 2), "copper:plate")
        m.box("body", (12.5 * s - 1.5, 32, -1.5), (3, 2, 3), "brass:plate")
        m.box("body", (13 * s - 1.5, 15, -1.5), (3, 1, 3), "brass:plate")
    m.box("body", (-15, 20, -6), (1, 10, 2), "glass:glass")
    m.bone("door", "firebox", pivot=(-6, 7.5, -15.5))
    m.box("door", (-6, 10, -16), (12, 2, 1), "steel:plate")
    m.box("door", (-6, 3, -16), (12, 4, 1), "steel:plate", faces={"north": "steel:vent"})
    m.box("door", (-6, 7, -16), (3, 3, 1), "steel:plate")
    m.box("door", (3, 7, -16), (3, 3, 1), "steel:plate")
    m.box("door", (4, 4, -17), (1, 2, 1), "brass:plate")
    m.bone("glow", "firebox", pivot=(0, 8.5, -15))
    m.box("glow", (-3, 7, -15.4), (6, 3, 1), GLOW_FIRE)
    m.bone("gauge", "body", pivot=(0, 31, -14.5))
    m.box("gauge", (-4, 27, -15), (8, 8, 1), "brass:plate")
    m.box("gauge", (-3, 28, -15.5), (6, 6, 1), "ceramic:flat:3")
    m.bone("gauge_needle", "gauge", pivot=(0, 31, -15.6))
    m.box("gauge_needle", (-0.5, 31, -15.6), (1, 3, 0), "red_paint:flat")
    m.anim("idle", "loop", 1.0).osc("glow", "scale", (0.06, 0.06, 0.06), base=(0.94, 0.94, 0.94))
    burn = m.anim("burning", "loop", 1.0)
    burn.keys("glow", "scale", [(0, (1, 1, 1)), (0.2, (1.25, 1.2, 1.1)), (0.45, (0.95, 0.95, 1)),
                                (0.7, (1.2, 1.15, 1.1)), (1.0, (1, 1, 1))])
    burn.keys("door", "rotation", [(0, (0, 0, 0)), (0.15, (0, 1.5, 0)), (0.3, (0, -0.5, 0)), (0.55, (0, 1, 0)),
                                   (0.75, (0, 0, 0)), (1.0, (0, 0, 0))])
    return m


def _engine_frames(steps: int = 16):
    """Return (t, flywheel_deg, crosshead_dz, rod_deg) for one revolution of the steam engine crank."""
    out = []
    for k in range(steps + 1):
        rho = 360.0 * k / steps
        a = math.radians(-rho)
        py, pz = 12 - 3 * math.cos(a), 7 - 3 * math.sin(a)
        dy = py - 9
        zc = pz - math.sqrt(49 - dy * dy)
        out.append((k / steps, rho, zc, math.degrees(math.asin(dy / 7.0))))
    return out


def steam_engine() -> Model:
    """Horizontal steel cylinder on a brass bed; crosshead and connecting rod drive a red-spoked flywheel."""
    m = Model("steam_engine")
    m.bone("frame")
    m.cbox("frame", (0, 0, 0), (16, 3, 32), "brass:rivet")
    for x in (-5, -1):
        m.box("frame", (x, 3, -6), (1, 9, 1), "steel:plate")
        m.box("frame", (x, 3, 5), (1, 3, 1), "steel:plate")
    m.box("frame", (-5, 6, -6), (4, 1, 12), "steel:plate")
    m.box("frame", (-5, 11, -6), (4, 1, 8), "steel:plate")
    m.box("frame", (6, 3, 5), (2, 9, 4), "brass:plate")
    m.box("frame", (-1, 3, 5), (3, 9, 4), "brass:plate")
    m.bone("cylinder", "frame")
    extrude(m, "cylinder", circle_mask(10), "z", (-3, 9, -11), 10, "steel:panel")
    for z in (-15.5, -6.5):
        extrude(m, "cylinder", circle_mask(12), "z", (-3, 9, z), 1, "iron:plate")
    m.box("cylinder", (-6, 14, -14), (6, 3, 6), "brass:rivet")
    m.box("cylinder", (-4, 17, -12), (2, 8, 2), "copper:plate")
    m.box("cylinder", (-4, 23, -12), (2, 2, 10), "copper:plate")
    m.box("cylinder", (-5.5, 19, -13.5), (5, 1, 5), "red_paint:plate")
    for x in (-7, 0):
        m.box("cylinder", (x, 3, -15), (1, 2, 8), "steel:plate")
    m.bone("piston", "frame", pivot=(-3, 9, 0))
    m.box("piston", (-4, 8, -7), (2, 2, 6), "iron:plate")
    m.box("piston", (-5, 7, -1.5), (4, 4, 3), "steel:rivet")
    m.bone("rod", "piston", pivot=(-3, 9, 0))
    m.box("rod", (-3.5, 8.5, 0), (1, 1, 7), "iron:plate")
    m.box("rod", (-4, 8, 6), (2, 2, 2), "brass:plate")
    m.bone("flywheel", "frame", pivot=(0, 12, 7))
    extrude(m, "flywheel", circle_mask(18, 13), "x", (4.5, 12, 7), 3, "iron:plate")
    radial(m, "flywheel", (4.5, 12, 7), "x", 6, (2, 6, 2), 3.5, "red_paint:plate")
    extrude(m, "flywheel", circle_mask(5), "x", (4.5, 12, 7), 4, "steel:rivet")
    m.box("flywheel", (-5, 11, 6), (13, 2, 2), "iron:plate")
    m.box("flywheel", (-4, 9, 6), (2, 3, 2), "steel:plate")
    m.box("flywheel", (-4.5, 8.5, 6.5), (3, 1, 1), "brass:plate")
    run = m.anim("run", "loop", 1.0)
    for t, rho, zc, rod in _engine_frames():
        run.key("flywheel", "rotation", t, (rho, 0, 0))
        run.key("piston", "position", t, (0, 0, zc))
        run.key("rod", "rotation", t, (rod, 0, 0))
    m.anim("idle", "loop", 1.0).osc("flywheel", "rotation", (3, 0, 0))
    return m


def lightning_mast() -> Model:
    """Copper lattice mast on a brass coil base with a spike rod; the glow at the tip flashes on charge."""
    m = Model("lightning_mast")
    m.bone("mast")
    m.cbox("mast", (0, 0, 0), (14, 2, 14), "steel:rivet")
    m.cbox("mast", (0, 2, 0), (12, 4, 12), "brass:rivet")
    lattice_mast(m, "mast", 6, 54, 3, 8, "copper:plate", "copper:flat")
    m.cbox("mast", (0, 54, 0), (10, 2, 10), "brass:rivet")
    m.bone("coil", "mast", pivot=(0, 12, 0))
    for y in (8, 12, 16):
        extrude(m, "coil", circle_mask(14, 11), "y", (0, y + 1, 0), 2, "copper:coil")
    for sx, sz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        m.box("coil", (sx * 4.5 - 0.5, 6, sz * 4.5 - 0.5), (1, 14, 1), "ceramic:plate")
    m.bone("rod", "mast", pivot=(0, 56, 0))
    extrude(m, "rod", circle_mask(6), "y", (0, 56.5, 0), 1, "ceramic:plate")
    extrude(m, "rod", circle_mask(4), "y", (0, 57.5, 0), 1, "ceramic:plate")
    extrude(m, "rod", circle_mask(6), "y", (0, 58.5, 0), 1, "ceramic:plate")
    m.box("rod", (-1, 59, -1), (2, 4, 2), "copper:plate")
    m.box("rod", (-0.5, 63, -0.5), (1, 1, 1), "copper:plate")
    m.bone("glow", "rod", pivot=(0, 61, 0))
    m.cbox("glow", (0, 59, 0), (4, 4, 4), "#9BE7FF:glow")
    hide = (0, 0, 0)
    m.anim("idle", "loop", 1.0).keys("glow", "scale", [(0, hide), (0.45, hide), (0.5, (0.45, 0.6, 0.45)),
                                                        (0.55, hide), (1.0, hide)])
    ch = m.anim("charge", "once", 1.0)
    ch.keys("glow", "scale", [(0, hide), (0.08, (1.7, 1.9, 1.7)), (0.2, (0.9, 1.1, 0.9)), (0.35, (1.5, 1.6, 1.5)),
                              (0.6, (0.6, 0.6, 0.6)), (1.0, hide)])
    ch.keys("coil", "scale", [(0, (1, 1, 1)), (0.1, (1.08, 1.02, 1.08)), (0.5, (1.03, 1, 1.03)), (1.0, (1, 1, 1))])
    return m


def wooden_pole() -> Model:
    """Round telegraph pole with a braced crossarm and two brown glass insulators."""
    m = Model("wooden_pole")
    m.bone("pole")
    extrude(m, "pole", circle_mask(6), "y", (0, 23, 0), 46, "wood:log")
    extrude(m, "pole", circle_mask(8), "y", (0, 1.5, 0), 3, "dark_wood:timber")
    for i, y in enumerate((14, 20, 26, 32)):
        s = 1 if i % 2 == 0 else -1
        m.box("pole", (3 if s > 0 else -5, y, -0.5), (2, 1, 1), "iron:plate")
    m.box("pole", (-0.5, 30, -3.5), (1, 6, 1), "iron:plate")
    m.bone("crossarm", "pole", pivot=(0, 39, 0))
    m.box("crossarm", (-8, 38, -1.5), (16, 2, 3), "wood:timber")
    for s in (-1, 1):
        strut(m, "crossarm", (3 * s, 32, 0), (7 * s, 38, 0), 1, "dark_wood:timber")
    m.box("crossarm", (-1, 38.5, -2), (2, 1, 1), "iron:plate")
    m.bone("insulators", "crossarm", pivot=(0, 40, 0))
    for x in (-6, 6):
        m.box("insulators", (x - 0.5, 40, -0.5), (1, 1, 1), "iron:plate")
        m.box("insulators", (x - 1.5, 41, -1.5), (3, 3, 3), "#9A5B2A:glass")
        m.box("insulators", (x - 1, 44, -1), (2, 1, 2), "#9A5B2A:glass")
        m.box("insulators", (x - 0.5, 45, -0.5), (1, 1, 1), "#9A5B2A:glass")
    m.anim("idle", "loop", 1.0).osc("pole", "rotation", (0, 0, 0.4))
    return m


def sluice() -> Model:
    """Inclined brass-lined wooden trough with riffles and a rippling water sheet; the head box is at +Z."""
    m = Model("sluice")
    tilt = dict(rotation=(10, 0, 0), pivot=(0, 7, 0))
    m.bone("trough")
    m.box("trough", (-6, 5, -23), (12, 1, 46), "wood:timber", faces={"up": "brass:plate"}, **tilt)
    for x in (-7, 6):
        m.box("trough", (x, 5, -23), (1, 4, 46), "wood:timber", **tilt)
        m.box("trough", (x, 9, -23), (1, 1, 46), "brass:plate", **tilt)
    for z, h in ((-18, 2), (0, 5), (16, 7)):
        for x in (-6, 6):
            m.cbox("trough", (x, 0, z), (2, h, 2), "dark_wood:timber")
        m.cbox("trough", (0, max(0, h - 2), z), (10, 1, 1), "dark_wood:timber")
    m.bone("riffles", "trough")
    for z in range(-20, 17, 6):
        m.box("riffles", (-6, 6, z), (12, 1, 1), "dark_wood:timber", **tilt)
    m.bone("head", "trough", pivot=(0, 11, 21))
    m.box("head", (-7, 11, 18), (14, 5, 6), "wood:wood", faces={"up": "dark_wood:dark"})
    m.box("head", (-7, 11, 18), (1, 5, 1), "brass:plate")
    m.box("head", (6, 11, 18), (1, 5, 1), "brass:plate")
    m.box("head", (-3, 11, 16), (6, 1, 2), "brass:plate")
    m.box("head", (-7, 15, 23), (14, 1, 1), "brass:plate")
    m.bone("water", "trough", pivot=(0, 7, 0))
    m.box("water", (-6, 7, -22), (12, 0, 44), "water:water", **tilt)
    m.box("water", (-3, 10.5, 16.5), (6, 0, 2), "water:water")
    m.anim("idle", "loop", 1.0).osc("water", "position", (0, 0.15, 0))
    flow = m.anim("flow", "loop", 1.0)
    flow.osc("water", "position", (0, 0.3, 0.6), cycles=2)
    flow.osc("water", "rotation", (0.6, 0, 0))
    return m


def blast_smelter() -> Model:
    """Stepped brick furnace tower with brass bands, copper pipes, a chimney and a glowing door window."""
    m = Model("blast_smelter")
    m.bone("body")
    m.cbox("body", (0, 0, 0), (32, 2, 32), "steel:rivet")
    m.cbox("body", (0, 2, 0), (30, 7, 30), "brick:brick")
    m.cbox("body", (0, 8, 0), (31, 2, 31), "brass:rivet", faces={"up": "steel:plate"})
    m.cbox("body", (0, 10, 0), (24, 13, 24), "brick:brick")
    m.cbox("body", (0, 15, 0), (25, 1, 25), "brass:plate")
    m.cbox("body", (0, 21, 0), (25, 2, 25), "brass:rivet", faces={"up": "steel:plate"})
    m.cbox("body", (0, 23, 0), (18, 3, 18), "brick:brick")
    for x in (-12, 12):
        m.box("body", (x - 1, 2, -15.5), (2, 6, 1), "iron:plate")
    m.box("body", (-7, 17, -12.5), (14, 3, 1), "steel:plate", faces={"north": "steel:vent"})
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.box("body", (sx * 11.5 - 1, 10, sz * 11.5 - 1), (2, 13, 2), "steel:plate")
    m.bone("chimney", "body", pivot=(0, 26, 0))
    m.cbox("chimney", (0, 26, 0), (8, 5, 8), "brick:brick")
    m.cbox("chimney", (0, 31, 0), (10, 1, 10), "steel:plate", faces={"up": "rubber:dark"})
    m.cbox("chimney", (0, 28, 0), (9, 1, 9), "brass:plate")
    m.bone("pipes", "body")
    for s in (-1, 1):
        m.box("pipes", (13.5 * s - 1, 10, -1), (2, 15, 2), "copper:plate")
        m.box("pipes", (8.5 if s > 0 else -14.5, 24, -1), (6, 2, 2), "copper:plate")
        m.box("pipes", (13.5 * s - 1.5, 16, -1.5), (3, 1, 3), "brass:plate")
        m.box("pipes", (13.5 * s - 1.5, 10, -1.5), (3, 1, 3), "brass:plate")
    m.bone("door", "body", pivot=(-5, 5.5, -15.5))
    m.box("door", (-5, 2, -16), (10, 3, 1), "steel:plate")
    m.box("door", (-5, 7, -16), (10, 2, 1), "steel:plate")
    m.box("door", (-5, 5, -16), (3, 2, 1), "steel:plate")
    m.box("door", (2, 5, -16), (3, 2, 1), "steel:plate")
    m.box("door", (3, 3, -17), (1, 2, 1), "brass:plate")
    m.bone("glow", "body", pivot=(0, 6, -15))
    m.box("glow", (-2, 5, -15.4), (4, 2, 1), GLOW_FIRE)
    m.box("glow", (-7, 11, -12.4), (14, 2, 1), GLOW_FIRE)
    m.anim("idle", "loop", 1.0).osc("glow", "scale", (0.06, 0.06, 0), base=(0.94, 0.94, 1))
    work = m.anim("working", "loop", 1.0)
    work.keys("glow", "scale", [(0, (1, 1, 1)), (0.25, (1.3, 1.25, 1)), (0.5, (1, 1, 1)), (0.75, (1.25, 1.2, 1)),
                                (1.0, (1, 1, 1))])
    work.keys("door", "rotation", [(0, (0, 0, 0)), (0.1, (0, 1.5, 0)), (0.2, (0, -1, 0)), (0.35, (0, 0.8, 0)),
                                   (0.5, (0, 0, 0)), (0.6, (0, 1.2, 0)), (0.75, (0, -0.6, 0)), (1.0, (0, 0, 0))])
    return m


MODELS = {"waterwheel": waterwheel, "boiler": boiler, "steam_engine": steam_engine, "lightning_mast": lightning_mast,
          "wooden_pole": wooden_pole, "sluice": sluice, "blast_smelter": blast_smelter}

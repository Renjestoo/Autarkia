"""GeckoLib block models of tier 1, part two: the colony core flower and the worker assembler cage."""

from geo_core import Model
from geo_shapes_t1 import ball, plane, radial, ring, strut

AMBER = "#FFB347"
SPARK = "#FFD27F"


def _petal(m: Model, k: int) -> None:
    """Add petal k (1..6): a mount bone turning it to its sector and a hinged blade facing outward."""
    mount = m.bone(f"petal_mount_{k}", "frame", pivot=(0, 17, 0), rotation=(0, 60 * (k - 1) + 30, 0))
    p = m.bone(f"petal_{k}", mount, pivot=(0, 17, -10), rotation=(-12, 0, 0))
    inner = {"south": "bronze:flat:1.4"}
    m.box(p, (-5, 17, -10.5), (10, 6, 1), "bronze:plate", faces=inner)
    m.box(p, (-4, 23, -10.5), (8, 5, 1), "bronze:plate", faces=inner)
    m.box(p, (-3, 28, -10.5), (6, 4, 1), "bronze:plate", faces=inner)
    m.box(p, (-2, 32, -10.5), (4, 2, 1), "bronze:plate", faces=inner)
    m.box(p, (-1, 34, -10.5), (2, 1, 1), "bronze:plate", faces=inner)
    m.box(p, (-0.5, 18, -11.5), (1, 15, 1), "copper:plate")
    m.box(p, (-5.5, 17, -11), (11, 2, 1), "bronze:rivet")
    plane(m, p, (0, 25.5, -9.4), 2, 13, "z", f"{AMBER}:glow")


def colony_core() -> Model:
    """Organic-mechanical brain: bronze roots, ribbed frame, six petals around an amber orb, a ring above."""
    m = Model("colony_core")
    m.bone("roots", pivot=(0, 0, 0))
    m.cbox("roots", (0, 0, 0), (14, 4, 14), "core_root:timber")
    m.box("roots", (-8, 0, -8), (16, 3, 16), "bronze:rivet", rotation=(0, 45, 0), pivot=(0, 0, 0))
    for k in range(6):
        a = 60 * k
        radial(m, "roots", (0, 0, 0), "y", a, 10, (11, 4, 6), "bronze:rivet")
        radial(m, "roots", (0, 0, 0), "y", a + 5, 16, (8, 3, 5), "core_root:timber")
        radial(m, "roots", (0, 0, 0), "y", a + 11, 21, (6, 2, 3), "core_root:timber")
        radial(m, "roots", (0, 0, 0), "y", a - 17, 17, (7, 2, 3), "core_root:timber", inflate=-0.1)
        radial(m, "roots", (0, 0, 0), "y", a + 30, 12, (7, 2, 3), "core_root:timber", inflate=-0.1)
        radial(m, "roots", (0, 3, 0), "y", a + 5, 15.5, (4, 1, 1), f"{AMBER}:glow")
        radial(m, "roots", (0, 0, 0), "y", a, 13.5, (1, 5, 7), "bronze:plate", inflate=-0.2)
    m.bone("frame", pivot=(0, 0, 0))
    m.cbox("frame", (0, 3, 0), (16, 5, 16), "bronze:rivet", faces={"up": "bronze:panel:8"})
    m.box("frame", (-7, 3, -7), (14, 4, 14), "bronze:rivet", rotation=(0, 45, 0), pivot=(0, 3, 0))
    m.cbox("frame", (0, 8, 0), (8, 10, 8), "bronze:panel:5")
    m.cbox("frame", (0, 18, 0), (10, 2, 10), "bronze:rivet")
    ring(m, "frame", (0, 17, 0), 10.5, "y", 2, 2, 12, "bronze:plate")
    for k in range(6):
        radial(m, "frame", (0, 8, 0), "y", 60 * k, 9, (2, 8, 2), "bronze:timber")
        radial(m, "frame", (0, 8, 0), "y", 60 * k, 6, (6, 1, 1), "copper:plate")
    m.cbox("frame", (0, 32, 0), (2, 7, 2), "bronze:plate")
    for k in range(1, 7):
        _petal(m, k)
    m.bone("core_orb", "frame", pivot=(0, 26, 0))
    ball(m, "core_orb", (0, 26, 0), 12, f"{AMBER}:glow")
    m.bone("ring", "frame", pivot=(0, 40, 0))
    ring(m, "ring", (0, 40.5, 0), 15, "y", 2, 2, 16, "bronze:rivet")
    m.cbox("ring", (0, 39, 0), (4, 3, 4), "bronze:rivet")
    for a in (0, 120, 240):
        radial(m, "ring", (0, 40, 0), "y", a, 7.5, (11, 1, 1), "bronze:plate")
        radial(m, "ring", (0, 39.5, 0), "y", a + 60, 14, (3, 3, 3), f"{AMBER}:glow")
    m.cbox("ring", (0, 42, 0), (2, 4, 2), "bronze:plate")
    m.cbox("ring", (0, 46, 0), (2, 2, 2), f"{AMBER}:glow")
    petals = [f"petal_{k}" for k in range(1, 7)]

    a = m.anim("roots", "hold", 3.0)
    a.keys("roots", "scale", [(0, (0, 0.2, 0)), (2.0, (0.85, 1, 0.85)), (3.0, (1, 1, 1))])
    a.keys("frame", "scale", [(0, (0, 0, 0)), (3.0, (0, 0, 0))])
    a.keys("frame", "position", [(0, (0, -40, 0)), (3.0, (0, -40, 0))])

    a = m.anim("frame", "hold", 4.0)
    a.keys("frame", "position", [(0, (0, -40, 0)), (3.2, (0, 2, 0)), (4.0, (0, 0, 0))])
    a.keys("core_orb", "scale", [(0, (0.4, 0.4, 0.4)), (4.0, (0.5, 0.5, 0.5))])
    a.keys("ring", "scale", [(0, (0.2, 1, 0.2)), (3.2, (0.6, 1, 0.6)), (4.0, (1, 1, 1))])

    a = m.anim("bloom", "hold", 3.0)
    for i, p in enumerate(petals):
        lag = 0.08 * i
        a.keys(p, "rotation", [(0, (0, 0, 0)), (0.3 + lag, (0, 0, 0)), (2.4 + lag, (76, 0, 0)), (3.0, (70, 0, 0))])
    a.keys("core_orb", "scale", [(0, (0.5, 0.5, 0.5)), (2.2, (1.15, 1.15, 1.15)), (3.0, (1, 1, 1))])

    a = m.anim("active", "loop", 4.0)
    for i, p in enumerate(petals):
        a.osc(p, "rotation", (4, 0, 0), base=(70, 0, 0), cycles=1, phase=i / 6.0)
    a.osc("core_orb", "scale", (0.1, 0.1, 0.1), cycles=2)
    a.spin("ring", "y", 1)

    a = m.anim("upgrade", "once", 5.0)
    for i, p in enumerate(petals):
        a.keys(p, "rotation", [(0, (70, 0, 0)), (1.0 + 0.05 * i, (45, 0, 0)), (2.2, (85, 0, 0)), (3.4, (50, 0, 0)), (5.0, (70, 0, 0))])
    a.spin("ring", "y", 5, steps=20)
    a.keys("core_orb", "scale", [(0, (1, 1, 1)), (1.2, (1.3, 1.3, 1.3)), (2.4, (0.9, 0.9, 0.9)), (3.6, (1.35, 1.35, 1.35)), (5.0, (1, 1, 1))])
    a.keys("ring", "position", [(0, (0, 0, 0)), (2.5, (0, 2, 0)), (5.0, (0, 0, 0))])

    a = m.anim("dormant", "loop", 2.0)
    for p in petals:
        a.osc(p, "rotation", (2, 0, 0), base=(-4, 0, 0), cycles=1)
    a.osc("core_orb", "scale", (0.03, 0.03, 0.03), base=(0.72, 0.72, 0.72), cycles=1)
    return m


def _arm(m: Model, side: str, x: float, lean: float) -> None:
    """Add a jointed welding arm hanging from the gantry carriage at file x."""
    s = 1 if x > 0 else -1
    arm = m.bone(f"arm_{side}", "gantry", pivot=(x, 24, 0), rotation=(0, 0, lean))
    m.box(arm, (x - 1.5, 22.5, -1.5), (3, 3, 3), "iron:rivet")
    m.box(arm, (x - 1, 18, -1), (2, 5, 2), "bronze:plate")
    m.box(arm, (x - 0.5 - s * 1.0, 19, -0.5), (1, 3, 1), "copper:plate")
    fore = m.bone(f"forearm_{side}", arm, pivot=(x, 18, 0), rotation=(0, 0, lean * 1.6))
    m.box(fore, (x - 1.5, 16.5, -1.5), (3, 3, 3), "iron:rivet")
    m.box(fore, (x - 1, 13, -1), (2, 4, 2), "bronze:plate")
    tip = m.bone(f"weld_tip_{side}", fore, pivot=(x, 13, 0))
    m.box(tip, (x - 0.5, 11, -0.5), (1, 2, 1), "copper:plate")
    m.box(tip, (x - 0.5, 10, -0.5), (1, 1, 1), f"{SPARK}:glow")


def _chassis(m: Model) -> None:
    """Add the half-built walker chassis standing on the centre pad (grows from the floor)."""
    m.bone("chassis", pivot=(0, 3, 0))
    for x in (-2.5, 2.5):
        m.cbox("chassis", (x, 3, -0.5), (3, 1, 5), "iron:plate")
        m.cbox("chassis", (x, 4, 0), (2, 5, 2), "iron:plate")
    m.cbox("chassis", (0, 9, 0), (8, 2, 4), "iron:rivet")
    for x in (-3.5, 3.5):
        for z in (-2.5, 2.5):
            m.cbox("chassis", (x, 11, z), (1, 7, 1), "iron:plate")
    m.cbox("chassis", (0, 11, 0), (2, 7, 2), "copper:coil")
    m.cbox("chassis", (0, 14, -2.5), (6, 3, 0), "bronze:plate")
    m.cbox("chassis", (0, 18, 0), (8, 1, 6), "iron:plate")
    for x in (-5, 5):
        m.cbox("chassis", (x, 13, 0), (2, 5, 2), "iron:plate")
    m.cbox("chassis", (0, 19, 0), (5, 4, 5), "bronze:rivet")
    plane(m, "chassis", (0, 21, -2.6), 2, 2, "z", f"{AMBER}:glow")


def worker_assembler() -> Model:
    """2x2x2 bronze workshop cage with a gantry, two welding arms, a growing chassis and a front grate hatch."""
    m = Model("worker_assembler")
    m.bone("base")
    m.cbox("base", (0, 0, 0), (32, 3, 32), "bronze:rivet", faces={"up": "dark_wood:wood"})
    plane(m, "base", (0, 3.1, 0), 16, 16, "y", "bronze:roundel")
    for x in (-14.5, 14.5):
        for z in (-14.5, 14.5):
            m.cbox("base", (x, 3, z), (3, 26, 3), "bronze:rivet")
    for z in (-14.5, 14.5):
        m.cbox("base", (0, 29, z), (32, 3, 3), "bronze:rivet", faces={"north": "bronze:panel:8"})
    for x in (-14.5, 14.5):
        m.cbox("base", (x, 29, 0), (3, 3, 26), "bronze:rivet")
    strut(m, "base", (-13, 3), (13, 29), "z", 15.0, 2, 1, "bronze:plate")
    strut(m, "base", (13, 3), (-13, 29), "z", 14.6, 2, 1, "bronze:plate")
    for x in (-14.5, 14.5):
        m.cbox("base", (x, 15, 0), (2, 2, 26), "bronze:rivet")
        m.cbox("base", (x, 3, 0), (2, 2, 26), "iron:plate")
    for x in (-8, 8):
        plane(m, "base", (x, 30.5, -16.1), 2, 1, "z", f"{AMBER}:glow")
    m.bone("gantry", "base", pivot=(0, 26, 0))
    m.cbox("gantry", (0, 26, 0), (26, 3, 4), "bronze:rivet")
    for x in (-7, 7):
        m.cbox("gantry", (x, 24, 0), (4, 2, 5), "iron:plate")
    _arm(m, "l", 7, 22)
    _arm(m, "r", -7, -22)
    _chassis(m)
    m.bone("hatch", "base", pivot=(0, 3, -15.5))
    for x in (-11.5, 11.5):
        m.cbox("hatch", (x, 3, -15.5), (2, 12, 1), "bronze:rivet")
    m.cbox("hatch", (0, 3, -15.5), (21, 2, 1), "bronze:rivet")
    m.cbox("hatch", (0, 13, -15.5), (21, 2, 1), "bronze:rivet")
    plane(m, "hatch", (0, 9, -15.5), 21, 8, "z", "bronze:grate")
    m.cbox("hatch", (0, 10, -16.5), (4, 1, 1), "iron:plate")
    plane(m, "hatch", (0, 14, -16.1), 6, 1, "z", f"{AMBER}:glow")

    a = m.anim("idle", "loop", 2.0)
    a.osc("arm_l", "rotation", (0, 0, 3), cycles=1)
    a.osc("arm_r", "rotation", (0, 0, 3), cycles=1, phase=0.5)
    a.osc("forearm_l", "rotation", (4, 0, 0), cycles=1, phase=0.25)
    a.osc("forearm_r", "rotation", (4, 0, 0), cycles=1, phase=0.75)

    a = m.anim("assemble", "loop", 2.0)
    a.osc("arm_l", "rotation", (14, 0, 6), cycles=2)
    a.osc("arm_r", "rotation", (14, 0, 6), cycles=2, phase=0.5)
    a.osc("forearm_l", "rotation", (12, 0, 8), cycles=2, phase=0.25)
    a.osc("forearm_r", "rotation", (12, 0, 8), cycles=2, phase=0.75)
    for tip, ph in (("weld_tip_l", 0.0), ("weld_tip_r", 0.37)):
        a.keys(tip, "scale", [(0, (1, 1, 1)), (0.2 + ph * 0.1, (1.8, 1.4, 1.8)), (0.45, (0.7, 1, 0.7)), (0.8, (1.6, 1.3, 1.6)),
                              (1.1, (0.8, 1, 0.8)), (1.5, (1.9, 1.5, 1.9)), (2.0, (1, 1, 1))])
    a.osc("chassis", "position", (0, 0.3, 0), cycles=4)

    a = m.anim("finish", "once", 2.0)
    a.keys("hatch", "rotation", [(0, (0, 0, 0)), (0.2, (-4, 0, 0)), (1.1, (92, 0, 0)), (1.3, (84, 0, 0)), (2.0, (88, 0, 0))])
    for arm, fore, s in (("arm_l", "forearm_l", -1), ("arm_r", "forearm_r", 1)):
        a.keys(arm, "rotation", [(0, (0, 0, 0)), (0.8, (0, 0, 30 * s)), (2.0, (0, 0, 30 * s))])
        a.keys(fore, "rotation", [(0, (0, 0, 0)), (0.8, (0, 0, 40 * s)), (2.0, (0, 0, 40 * s))])
    a.keys("chassis", "position", [(0, (0, 0, 0)), (1.2, (0, 0, 0)), (1.5, (0, 1, 0)), (1.8, (0, 0, 0))])
    return m

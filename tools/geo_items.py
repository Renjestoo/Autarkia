"""GeckoLib equipment item models (07 section 4.2).

Every model sits in the 16px item box around its origin: grip near (0, 4, 3), long axis along Z with the
muzzle, bit or tip toward -Z and the shooter's side toward +Z.
"""

from geo_core import Model


def rod_z(m: Model, bone: str, cx: float, cy: float, z0: float, z1: float, d: int, mat: str,
          faces: dict | None = None) -> None:
    """Add a rounded rod of diameter d along Z from z0 to z1 (two crossed boxes when d > 2)."""
    length = z1 - z0
    if d <= 2:
        m.box(bone, (cx - d / 2, cy - d / 2, z0), (d, d, length), mat, faces)
        return
    c = 1 if d < 7 else 2
    m.box(bone, (cx - d / 2, cy - (d - 2 * c) / 2, z0), (d, d - 2 * c, length), mat, faces)
    m.box(bone, (cx - (d - 2 * c) / 2, cy - d / 2, z0), (d - 2 * c, d, length), mat, faces, inflate=0.02)


def rod_x(m: Model, bone: str, cy: float, cz: float, x0: float, x1: float, d: int, mat: str,
          faces: dict | None = None) -> None:
    """Add a rounded rod of diameter d along X from x0 to x1."""
    length = x1 - x0
    if d <= 2:
        m.box(bone, (x0, cy - d / 2, cz - d / 2), (length, d, d), mat, faces)
        return
    c = 1 if d < 7 else 2
    m.box(bone, (x0, cy - (d - 2 * c) / 2, cz - d / 2), (length, d - 2 * c, d), mat, faces)
    m.box(bone, (x0, cy - d / 2, cz - (d - 2 * c) / 2), (length, d, d - 2 * c), mat, faces, inflate=0.02)


def ring_z(m: Model, bone: str, cx: float, cy: float, z0: float, d: int, mat: str, faces: dict | None = None,
           w: int = 1, t: int = 1, rotation=None) -> None:
    """Add a square ring frame of outer size d in the XY plane, t thick along Z starting at z0.

    Args:
        rotation: optional cube rotation applied to every bar about the ring centre.
    """
    h = d / 2
    kw = {"rotation": rotation, "pivot": (cx, cy, z0 + t / 2)} if rotation else {}
    m.box(bone, (cx - h, cy + h - w, z0), (d, w, t), mat, faces, **kw)
    m.box(bone, (cx - h, cy - h, z0), (d, w, t), mat, faces, **kw)
    m.box(bone, (cx - h, cy - h + w, z0), (w, d - 2 * w, t), mat, faces, **kw)
    m.box(bone, (cx + h - w, cy - h + w, z0), (w, d - 2 * w, t), mat, faces, **kw)


def grip(m: Model, bone: str, mat: str, z: float = 3.0, top: float = 7.0, length: int = 5) -> None:
    """Add a pistol grip hanging below y=top, raked back by 15 degrees."""
    m.box(bone, (-1.5, top - length, z - 1.5), (3, length, 3), mat, rotation=(15, 0, 0), pivot=(0, top, z))


def steam_repeater() -> Model:
    """Steel repeater with a revolving brass drum, pressure tank and gauge."""
    m = Model("steam_repeater")
    m.bone("body", pivot=(0, 7, 3))
    m.box("body", (-2, 7, -1), (4, 4, 6), "steel:rivet")
    m.cbox("body", (0, 11, 2), (2, 1, 5), "brass:plate")
    m.box("body", (-1.5, 7, 5), (3, 4, 3), "wood:timber", faces={"south": "brass:plate"})
    m.cbox("body", (0, 5.5, 7), (3, 2, 2), "wood:timber")
    grip(m, "body", "wood:timber", z=3.0)
    m.cbox("body", (0, 5, 1.5), (1, 2, 1), "brass:plate")
    rod_z(m, "body", 0, 6, -6, -1, 3, "brass:plate", faces={"north": "brass:rings", "south": "brass:rings"})
    m.bone("drum", "body", pivot=(0, 9, -2.5))
    rod_z(m, "drum", 0, 9, -4, -1, 5, "brass:rivet", faces={"north": "brass:rings", "south": "brass:rings"})
    m.bone("barrel", "body", pivot=(0, 9, -4))
    m.box("barrel", (-1, 8, -7.5), (2, 2, 4), "steel:plate")
    m.cbox("barrel", (0, 7.5, -7.5), (3, 3, 1), "steel:plate", faces={"north": "steel:dark"})
    m.cbox("barrel", (0, 10, -6), (1, 1, 1), "brass:plate")
    m.bone("gauge", "body", pivot=(0, 11.5, 1))
    m.cbox("gauge", (0, 11, 1), (1, 1, 1), "brass:plate")
    m.cbox("gauge", (0, 12, 1), (3, 3, 2), "brass:plate", faces={"south": "brass:gauge", "north": "brass:gauge"})
    idle = m.anim("idle", "loop", 1.0)
    idle.keys("gauge", "rotation", [(0, (0, 0, 0)), (0.5, (0, 0, 4)), (1.0, (0, 0, 0))])
    fire = m.anim("fire", "once", 0.25)
    fire.keys("barrel", "position", [(0, (0, 0, 0)), (0.04, (0, 0, 1.5)), (0.25, (0, 0, 0))])
    fire.keys("body", "rotation", [(0, (0, 0, 0)), (0.04, (-6, 0, 0)), (0.25, (0, 0, 0))])
    pump = m.anim("pump", "once", 0.5)
    pump.keys("drum", "rotation", [(0, (0, 0, 0)), (0.3, (0, 0, 60)), (0.5, (0, 0, 60))])
    pump.keys("gauge", "rotation", [(0, (0, 0, 0)), (0.15, (0, 0, -10)), (0.3, (0, 0, 6)), (0.5, (0, 0, 0))])
    return m


def electric_drill() -> Model:
    """Pistol drill: steel motor housing with a cyan charge band, rubber grip, battery pack, fluted bit."""
    m = Model("electric_drill")
    m.bone("body", pivot=(0, 7, 3))
    rod_z(m, "body", 0, 10.5, -3, 5, 5, "electric:rivet", faces={"south": "electric:vent", "north": "electric:plate"})
    rod_z(m, "body", 0, 10.5, 0, 1, 6, "electric_accent:glow")
    m.cbox("body", (0, 13, 2.5), (1, 1, 3), "electric_accent:glow")
    grip(m, "body", "rubber:rubber", z=3.0, top=8.5, length=6)
    m.cbox("body", (0, 6.5, 1.5), (1, 2, 1), "#F2C230:plate")
    rod_z(m, "body", 0, 10.5, -5, -3, 3, "steel:plate")
    m.bone("battery", "body", pivot=(0, 1.5, 3.5))
    m.box("battery", (-2.5, 0, 1), (5, 3, 6), "electric:rivet")
    m.box("battery", (-2.5, 1, 1.5), (5, 1, 5), "electric_accent:glow", inflate=0.1)
    m.bone("bit", "body", pivot=(0, 10.5, -5))
    m.box("bit", (-0.5, 10, -8), (1, 1, 3), "tin:plate")
    for i, z in enumerate((-6, -7, -8)):
        m.box("bit", (-1, 10, z), (2, 1, 1), "tin:plate", rotation=(0, 0, 60 * i), pivot=(0, 10.5, z + 0.5))
    idle = m.anim("idle", "loop", 1.0)
    idle.osc("body", "position", (0, 0.15, 0))
    spin = m.anim("spin", "loop", 0.25)
    spin.spin("bit", "z", -1)
    spin.keys("body", "position", [(0, (0, 0, 0)), (0.0625, (0.1, 0.05, 0)), (0.125, (0, 0, 0)),
                                   (0.1875, (-0.1, 0.05, 0)), (0.25, (0, 0, 0))])
    return m


def electric_chainsaw() -> Model:
    """Chainsaw: steel motor housing with hazard guard, top handle, guide bar and chain."""
    m = Model("electric_chainsaw")
    m.bone("body", pivot=(0, 8, 4))
    m.box("body", (-2.5, 6, -1), (5, 6, 8), "electric:rivet", faces={"east": "electric:vent", "west": "electric:vent"})
    m.box("body", (-2.6, 6, 1), (5, 6, 1), "electric_accent:glow", inflate=0.1)
    m.cbox("body", (0, 10, -1.5), (5, 3, 1), "hazard:hazard")
    m.cbox("body", (0, 12, 0.5), (1, 2, 1), "electric:plate")
    m.cbox("body", (0, 12, 5.5), (1, 2, 1), "electric:plate")
    m.cbox("body", (0, 14, 3), (1, 1, 7), "rubber:rubber")
    m.cbox("body", (0, 6, 7.5), (2, 5, 1), "rubber:rubber")
    m.bone("bar", "body", pivot=(0, 8.5, -1))
    m.box("bar", (-0.5, 6.5, -7), (1, 4, 6), "steel:plate")
    m.box("bar", (-0.6, 8, -6), (1, 1, 1), "steel:dark")
    m.bone("chain", "bar", pivot=(0, 8.5, -1))
    m.box("chain", (-1, 10.5, -7), (2, 1, 7), "steel:chain")
    m.box("chain", (-1, 5.5, -7), (2, 1, 7), "steel:chain")
    m.box("chain", (-1, 5.5, -8), (2, 6, 1), "steel:chain")
    idle = m.anim("idle", "loop", 1.0)
    idle.osc("body", "position", (0, 0.1, 0), cycles=2)
    run = m.anim("run", "loop", 0.25)
    run.keys("chain", "position", [(0, (0, 0, 0)), (0.0625, (0, 0, -0.5)), (0.125, (0, 0, 0)),
                                   (0.1875, (0, 0, -0.5)), (0.25, (0, 0, 0))])
    run.keys("body", "position", [(0, (0, 0, 0)), (0.0625, (0, 0.15, 0)), (0.125, (0, 0, 0)),
                                  (0.1875, (0, 0.15, 0)), (0.25, (0, 0, 0))])
    return m


def arc_rifle() -> Model:
    """Arc rifle: steel receiver and stock, copper coil rings glowing cyan around an emitter barrel."""
    m = Model("arc_rifle")
    m.bone("body", pivot=(0, 7, 3))
    m.box("body", (-2, 7, -1), (4, 4, 7), "electric:rivet")
    m.box("body", (-1.5, 6.5, 6), (3, 4, 2), "electric:plate", faces={"south": "rubber:rubber"})
    grip(m, "body", "rubber:rubber", z=3.5)
    m.cbox("body", (0, 11, 2), (2, 2, 4), "electric_accent:glow")
    m.cbox("body", (0, 5.5, 1.5), (1, 2, 1), "electric:plate")
    m.bone("barrel", "body", pivot=(0, 9, -1))
    m.box("barrel", (-1, 8, -8), (2, 2, 7), "electric:plate")
    m.cbox("barrel", (0, 10.5, -7.5), (1, 1, 2), "copper:plate")
    m.cbox("barrel", (0, 6.5, -7.5), (1, 1, 2), "copper:plate")
    m.bone("coils", "body", pivot=(0, 9, -4.5))
    for z in (-7, -5, -3):
        ring_z(m, "coils", 0, 9, z, 4, "copper:coil", faces={"north": "electric_accent:glow", "south": "electric_accent:glow"})
    idle = m.anim("idle", "loop", 1.0)
    idle.osc("coils", "scale", (0.06, 0.06, 0), base=(1, 1, 1))
    fire = m.anim("fire", "once", 0.5)
    fire.keys("coils", "scale", [(0, (1, 1, 1)), (0.05, (1.3, 1.3, 1.1)), (0.4, (1, 1, 1))])
    fire.keys("barrel", "position", [(0, (0, 0, 0)), (0.05, (0, 0, 1)), (0.3, (0, 0, 0))])
    fire.keys("body", "rotation", [(0, (0, 0, 0)), (0.05, (-4, 0, 0)), (0.35, (0, 0, 0))])
    return m


def grapple_launcher() -> Model:
    """Grapple launcher: vantite tube with a side cable reel and a three-pronged hook in the muzzle."""
    m = Model("grapple_launcher")
    m.bone("body", pivot=(0, 7, 3))
    rod_z(m, "body", 0, 9, -5, 5, 4, "vantite:rivet", faces={"north": "vantite_steel:dark"})
    rod_z(m, "body", 0, 9, -5, -4, 5, "vantite_steel:plate", faces={"north": "vantite_steel:dark"})
    m.cbox("body", (0, 11, 0), (1, 1, 6), "vantite:glow")
    grip(m, "body", "leather:leather", z=3.5)
    m.cbox("body", (0, 7.5, 6.5), (2, 3, 3), "vantite_steel:plate")
    m.box("body", (2, 7, 1), (1, 2, 2), "vantite_steel:plate")
    m.bone("reel", "body", pivot=(3.5, 9, 1.5))
    rod_x(m, "reel", 9, 1.5, 2, 3, 4, "brass:plate")
    rod_x(m, "reel", 9, 1.5, 3, 4, 2, "steel:coil")
    rod_x(m, "reel", 9, 1.5, 4, 5, 4, "brass:plate")
    m.bone("hook", "body", pivot=(0, 9, -6))
    m.cbox("hook", (0, 7.5, -5.5), (3, 3, 1), "steel:plate")
    m.box("hook", (-0.5, 8.5, -8), (1, 1, 3), "vantite:crystal")
    for ry in (35, -35):
        m.box("hook", (-0.5, 8.5, -8), (1, 1, 3), "tin:plate", rotation=(0, ry, 0), pivot=(0, 9, -5.5))
    for rx in (35, -35):
        m.box("hook", (-0.5, 8.5, -8), (1, 1, 3), "tin:plate", rotation=(rx, 0, 0), pivot=(0, 9, -5.5))
    idle = m.anim("idle", "loop", 1.0)
    idle.keys("reel", "rotation", [(0, (0, 0, 0)), (0.5, (8, 0, 0)), (1.0, (0, 0, 0))])
    fire = m.anim("fire", "once", 0.5)
    fire.keys("hook", "position", [(0, (0, 0, 0)), (0.15, (0, 0, -7)), (0.2, (0, 0, -9)), (0.45, (0, 0, -9)),
                                   (0.46, (0, 0, 0)), (0.5, (0, 0, 0))])
    fire.keys("hook", "scale", [(0, (1, 1, 1)), (0.15, (1, 1, 1)), (0.2, (0, 0, 0)), (0.46, (0, 0, 0)), (0.5, (1, 1, 1))])
    fire.spin("reel", "x", -2)
    fire.keys("body", "rotation", [(0, (0, 0, 0)), (0.05, (-5, 0, 0)), (0.3, (0, 0, 0))])
    reel = m.anim("reel", "loop", 0.5)
    reel.spin("reel", "x", 1)
    reel.keys("body", "position", [(0, (0, 0, 0)), (0.125, (0, 0.1, 0)), (0.25, (0, 0, 0)), (0.375, (0, 0.1, 0)),
                                   (0.5, (0, 0, 0))])
    return m


def rail_rifle() -> Model:
    """Rail rifle: vantite receiver, two parallel rails around a glowing core, top scope."""
    m = Model("rail_rifle")
    m.bone("body", pivot=(0, 7, 3))
    m.box("body", (-2, 6, 0), (4, 4, 7), "vantite_steel:rivet")
    m.cbox("body", (0, 6, 7.5), (3, 3, 1), "leather:leather")
    grip(m, "body", "leather:leather", z=3.5, top=6)
    m.cbox("body", (0, 7.5, -4), (1, 1, 8), "vantite:glow")
    m.cbox("body", (0, 7, 0.5), (3, 3, 1), "vantite_steel:plate")
    m.bone("rail_top", "body", pivot=(0, 9.5, 0))
    m.box("rail_top", (-1.5, 9, -8), (3, 1, 8), "vantite_steel:plate", faces={"down": "vantite:glow"})
    for z in (-7, -4.5, -2):
        m.cbox("rail_top", (0, 10, z), (1, 1, 1), "vantite:plate")
    m.bone("rail_bottom", "body", pivot=(0, 6.5, 0))
    m.box("rail_bottom", (-1.5, 6, -8), (3, 1, 8), "vantite_steel:plate", faces={"up": "vantite:glow"})
    for z in (-7, -4.5, -2):
        m.cbox("rail_bottom", (0, 5, z), (1, 1, 1), "vantite:plate")
    m.bone("scope", "body", pivot=(0, 11, 3.5))
    m.cbox("scope", (0, 10, 3.5), (1, 1, 1), "vantite_steel:plate")
    m.box("scope", (-1, 11, 1), (2, 2, 5), "vantite_steel:plate", faces={"north": "vantite:lens", "south": "vantite:lens"})
    idle = m.anim("idle", "loop", 1.0)
    idle.osc("rail_top", "position", (0, 0.2, 0))
    idle.osc("rail_bottom", "position", (0, -0.2, 0))
    charge = m.anim("charge", "hold", 1.5)
    charge.keys("rail_top", "position", [(0, (0, 0, 0)), (0.6, (0, 1.5, 0)), (1.2, (0, 1.5, 0)), (1.35, (0, 1.7, 0)),
                                         (1.5, (0, 1.5, 0))])
    charge.keys("rail_bottom", "position", [(0, (0, 0, 0)), (0.6, (0, -1.5, 0)), (1.2, (0, -1.5, 0)),
                                            (1.35, (0, -1.7, 0)), (1.5, (0, -1.5, 0))])
    fire = m.anim("fire", "once", 0.5)
    fire.keys("rail_top", "position", [(0, (0, 1.5, 0)), (0.08, (0, 0, 0)), (0.5, (0, 0, 0))])
    fire.keys("rail_bottom", "position", [(0, (0, -1.5, 0)), (0.08, (0, 0, 0)), (0.5, (0, 0, 0))])
    fire.keys("body", "rotation", [(0, (0, 0, 0)), (0.05, (-8, 0, 0)), (0.35, (0, 0, 0))])
    fire.keys("scope", "position", [(0, (0, 0, 0)), (0.05, (0, 0, 0.5)), (0.3, (0, 0, 0))])
    return m


def nanite_multitool() -> Model:
    """Lattice multitool: handle with a glowing band, the pick, axe and shovel heads (1x1 mode) and a wide 3x3 head.

    The renderer shows head_pick, head_axe and head_shovel in 1x1 and only head_area in 3x3.
    """
    m = Model("nanite_multitool")
    m.bone("body", pivot=(0, 6, 4))
    rod_z(m, "body", 0, 6, 3, 8, 3, "rubber:rubber", faces={"south": "lattice:plate"})
    rod_z(m, "body", 0, 6, -4, 3, 2, "lattice:plate")
    m.cbox("body", (0, 4.5, 2), (3, 3, 1), "lattice:rivet")
    m.cbox("body", (0, 4.5, -4.5), (3, 3, 1), "lattice:rivet")
    m.bone("glow", "body", pivot=(0, 6, -1))
    rod_z(m, "glow", 0, 6, -2, -1, 3, "lattice:glow")
    m.cbox("glow", (0, 7, 5.5), (1, 1, 2), "lattice:glow")
    m.bone("head_pick", "body", pivot=(0, 6, -5.5))
    m.cbox("head_pick", (0, 2, -6), (2, 8, 2), "lattice:rivet")
    m.box("head_pick", (-0.5, 10, -7), (1, 2, 1), "lattice:crystal", rotation=(-20, 0, 0), pivot=(0, 10, -6))
    m.box("head_pick", (-0.5, 0, -7), (1, 2, 1), "lattice:crystal", rotation=(20, 0, 0), pivot=(0, 2, -6))
    m.bone("head_axe", "body", pivot=(0, 6, -5.5))
    m.cbox("head_axe", (0, 5, -6), (2, 3, 2), "lattice:rivet")
    m.box("head_axe", (-0.5, 6, -8), (1, 6, 3), "lattice:crystal", faces={"north": "lattice:glow"})
    m.box("head_axe", (-0.5, 3, -6.5), (1, 2, 1), "lattice:plate")
    m.bone("head_shovel", "body", pivot=(0, 6, -5.5))
    m.cbox("head_shovel", (0, 5, -5.5), (2, 2, 1), "lattice:rivet")
    m.box("head_shovel", (-2.5, 5.5, -8), (5, 1, 3), "lattice:plate", faces={"north": "lattice:crystal", "up": "lattice:crystal"})
    m.bone("head_area", "body", pivot=(0, 6, -5.5))
    m.cbox("head_area", (0, 4.5, -5.5), (2, 3, 1), "lattice:rivet")
    m.box("head_area", (-3, 3, -8), (6, 6, 2), "lattice:plate", faces={"north": "lattice:glow"})
    for sx in (-2, 0, 2):
        for sy in (3.5, 5.5, 7.5):
            m.box("head_area", (sx - 0.5, sy, -8.5), (1, 1, 1), "lattice:crystal")
    idle = m.anim("idle", "loop", 2.0)
    idle.osc("glow", "scale", (0.15, 0.15, 0.3), base=(1, 1, 1))
    idle.osc("head_area", "scale", (0.04, 0.04, 0.0), base=(1, 1, 1))
    use = m.anim("use", "loop", 0.5)
    use.keys("body", "rotation", [(0, (0, 0, 0)), (0.2, (25, 0, 0)), (0.5, (0, 0, 0))])
    use.keys("glow", "scale", [(0, (1, 1, 1)), (0.2, (1.3, 1.3, 1.5)), (0.5, (1, 1, 1))])
    return m


def disassembler_beam() -> Model:
    """Disassembler: lattice receiver, magenta cell, crystal emitter inside four radiating fins."""
    m = Model("disassembler_beam")
    m.bone("body", pivot=(0, 7, 3))
    rod_z(m, "body", 0, 9, -3, 6, 4, "lattice:rivet", faces={"south": "lattice:vent"})
    grip(m, "body", "rubber:rubber", z=3.5)
    m.cbox("body", (0, 11, 2.5), (2, 2, 3), "#FF5CF0:glow")
    m.cbox("body", (0, 5.5, 1.5), (1, 2, 1), "lattice:plate")
    m.cbox("body", (0, 7, -4), (4, 4, 1), "lattice:plate", faces={"north": "lattice:lens"})
    m.bone("emitter", "body", pivot=(0, 9, -6))
    m.box("emitter", (-1, 8, -8), (2, 2, 3), "lattice:crystal", rotation=(0, 0, 45), pivot=(0, 9, -6.5))
    m.cbox("emitter", (0, 8.5, -8.5), (1, 1, 1), "#FF5CF0:glow")
    m.bone("fins", "body", pivot=(0, 9, -5))
    for cx, cy, sx, sy in ((0, 11, 1, 2), (0, 5, 1, 2), (2.5, 8.5, 2, 1), (-2.5, 8.5, 2, 1)):
        m.cbox("fins", (cx, cy, -5), (sx, sy, 4), "lattice:plate", faces={"north": "lattice:glow"})
    idle = m.anim("idle", "loop", 2.0)
    idle.spin("fins", "z", 0.25)
    idle.spin("emitter", "z", -0.25)
    fire = m.anim("fire", "loop", 0.5)
    fire.spin("emitter", "z", -1)
    fire.spin("fins", "z", 0.5)
    fire.osc("emitter", "scale", (0.2, 0.2, 0.2), base=(1, 1, 1), cycles=2, steps=8)
    return m


def aegis_multitool() -> Model:
    """Aegis multitool: black-gold handle with a cyan seam, forked head and two orbiting gold rings."""
    m = Model("aegis_multitool")
    m.bone("body", pivot=(0, 6, 4))
    rod_z(m, "body", 0, 6, -3, 8, 2, "aegis_body:rivet")
    rod_z(m, "body", 0, 6, 1, 2, 3, "aegis_gold:plate")
    rod_z(m, "body", 0, 6, 6, 7, 3, "aegis_gold:plate")
    m.box("body", (-0.5, 6.5, 2), (1, 1, 4), "aegis_glow:glow", inflate=0.05)
    m.bone("head", "body", pivot=(0, 6, -4.5))
    m.cbox("head", (0, 4, -5), (3, 4, 3), "aegis_body:plate", faces={"north": "aegis_body:seam:#7FFFF0"})
    m.cbox("head", (0, 3.5, -5), (2, 5, 2), "aegis_gold:plate")
    m.box("head", (-0.5, 8, -6.5), (1, 4, 1), "aegis_gold:plate", rotation=(-30, 0, 0), pivot=(0, 8, -5.5))
    m.box("head", (-0.5, 0, -6.5), (1, 4, 1), "aegis_gold:plate", rotation=(30, 0, 0), pivot=(0, 4, -5.5))
    m.cbox("head", (0, 5, -7.5), (1, 2, 2), "aegis_glow:glow")
    m.bone("rings", "head", pivot=(0, 6, -5))
    ring_z(m, "rings", 0, 6, -5.5, 7, "aegis_gold:plate", faces={"south": "aegis_glow:glow"})
    ring_z(m, "rings", 0, 6, -5.5, 7, "aegis_gold:plate", faces={"east": "aegis_glow:glow"}, rotation=(0, 90, 0))
    idle = m.anim("idle", "loop", 2.0)
    idle.spin("rings", "z", 1)
    use = m.anim("use", "loop", 0.5)
    use.keys("body", "rotation", [(0, (0, 0, 0)), (0.2, (25, 0, 0)), (0.5, (0, 0, 0))])
    use.spin("rings", "z", 2)
    return m


def singularity_lance() -> Model:
    """Singularity lance: black-gold shaft, three gold rings that charge and spin, black tip with cyan rim."""
    m = Model("singularity_lance")
    m.bone("body", pivot=(0, 8, 4))
    rod_z(m, "body", 0, 8, -5, 7, 2, "aegis_body:rivet")
    rod_z(m, "body", 0, 8, 7, 8, 3, "aegis_gold:plate")
    rod_z(m, "body", 0, 8, 3, 6, 3, "leather:leather")
    rod_z(m, "body", 0, 8, 2, 3, 4, "aegis_gold:plate")
    m.box("body", (-0.5, 8.5, -4), (1, 1, 6), "aegis_glow:glow", inflate=0.05)
    m.bone("tip", "body", pivot=(0, 8, -6.5))
    rod_z(m, "tip", 0, 8, -8, -5, 4, "aegis_body:plate", faces={"north": "aegis_body:lens"})
    rod_z(m, "tip", 0, 8, -7, -6, 5, "aegis_glow:glow")
    for name, z, d in (("ring_1", -3.5, 6), ("ring_2", -1.5, 5), ("ring_3", 0.5, 4)):
        m.bone(name, "body", pivot=(0, 8, z + 0.5))
        ring_z(m, name, 0, 8, z, d, "aegis_gold:plate", faces={"south": "aegis_glow:glow"})
    idle = m.anim("idle", "loop", 2.0)
    idle.spin("ring_1", "z", 0.25)
    idle.spin("ring_2", "z", -0.25)
    idle.spin("ring_3", "z", 0.5)
    idle.osc("tip", "scale", (0.05, 0.05, 0.05), base=(1, 1, 1))
    charge = m.anim("charge", "hold", 3.0)
    for k, (name, sign) in enumerate((("ring_1", 1), ("ring_2", -1), ("ring_3", 1))):
        charge.keys(name, "rotation", [(0, (0, 0, 0)), (1.0, (0, 0, 90 * sign)), (2.0, (0, 0, 360 * sign)),
                                       (3.0, (0, 0, 900 * sign))])
        charge.keys(name, "position", [(0, (0, 0, 0)), (3.0, (0, 0, -(k + 1)))])
    charge.keys("tip", "scale", [(0, (1, 1, 1)), (3.0, (1.4, 1.4, 1.4))])
    fire = m.anim("fire", "once", 1.0)
    for k, name in enumerate(("ring_1", "ring_2", "ring_3")):
        fire.keys(name, "position", [(0, (0, 0, -(k + 1))), (0.1, (0, 0, -(k + 3))), (1.0, (0, 0, 0))])
        fire.keys(name, "rotation", [(0, (0, 0, 0)), (1.0, (0, 0, 180 * (1 if k % 2 == 0 else -1)))])
    fire.keys("tip", "scale", [(0, (1.4, 1.4, 1.4)), (0.1, (0.6, 0.6, 0.6)), (1.0, (1, 1, 1))])
    fire.keys("body", "position", [(0, (0, 0, 0)), (0.08, (0, 0, 1.5)), (0.6, (0, 0, 0))])
    return m


MODELS = {"steam_repeater": steam_repeater, "electric_drill": electric_drill, "electric_chainsaw": electric_chainsaw,
          "arc_rifle": arc_rifle, "grapple_launcher": grapple_launcher, "rail_rifle": rail_rifle,
          "nanite_multitool": nanite_multitool, "disassembler_beam": disassembler_beam,
          "aegis_multitool": aegis_multitool, "singularity_lance": singularity_lance}

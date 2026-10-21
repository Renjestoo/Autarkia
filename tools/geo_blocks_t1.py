"""GeckoLib block models of tier 1 (muscle and timber)."""

from geo_blocks_t1b import colony_core, worker_assembler
from geo_core import Model
from geo_shapes_t1 import disc_faces, plane, radial, ring, strut


def hand_crank() -> Model:
    """Cobblestone base, compact bronze gearbox, crank arm with a wooden handle turning about Z on the front."""
    m = Model("hand_crank")
    m.bone("base")
    m.cbox("base", (0, 0, 0), (16, 8, 16), "stone:cobble")
    m.cbox("base", (0, 8, 0), (16, 2, 16), "bronze:rivet", faces={"up": "bronze:panel:8"})
    for x in (-6.5, 6.5):
        for z in (-6.5, 6.5):
            m.cbox("base", (x, 0, z), (3, 8, 3), "bronze:plate", inflate=0.15)
    m.cbox("base", (0, 10, 2), (10, 5, 8), "bronze:rivet", faces={"north": "bronze:plate", "up": "bronze:vent"})
    m.cbox("base", (0, 15, 2), (6, 1, 4), "bronze:plate")
    m.cbox("base", (0, 10, -2.5), (4, 5, 1), "iron:plate")
    for x in (-5.1, 5.1):
        plane(m, "base", (x, 12.5, 2), 6, 6, "x", "bronze:cog")
    m.bone("shaft", "base", pivot=(0, 13, -4))
    m.box("shaft", (-1, 12, -5), (2, 2, 3), "iron:plate")
    m.bone("crank", "base", pivot=(0, 13, -5.5))
    m.box("crank", (-1.5, 11.5, -6), (3, 3, 1), "bronze:plate")
    m.box("crank", (-1, 13, -6), (2, 3, 1), "bronze:plate")
    m.bone("handle", "crank", pivot=(0, 15, -7))
    m.box("handle", (-1, 14, -8), (2, 2, 2), "wood:timber")
    turn = m.anim("turn", "loop", 1.0)
    turn.spin("shaft", "z", -1)
    turn.spin("crank", "z", -1)
    idle = m.anim("idle", "loop", 1.0)
    idle.keys("crank", "rotation", [(0, (0, 0, 0)), (0.5, (0, 0, -6)), (1.0, (0, 0, 0))])
    return m


def quern() -> Model:
    """Two round millstones on a plank tray; the top runner and its stick handle turn about Y."""
    m = Model("quern")
    m.bone("base")
    m.cbox("base", (0, 0, 0), (16, 1, 16), "wood:wood")
    ring(m, "base", (0, 4, 0), 7.5, "y", 6, 2, 16, "stone:stone")
    plane(m, "base", (0, 7.1, 0), 15, 15, "y", "stone:roundel")
    m.cbox("base", (0, 4, -6.5), (3, 2, 3), "stone:stone")
    m.cbox("base", (0, 1, -7), (5, 1, 2), "canvas:canvas")
    m.bone("runner", "base", pivot=(0, 7, 0))
    ring(m, "runner", (0, 9.5, 0), 6.5, "y", 5, 2, 16, "stone:stone")
    ring(m, "runner", (0, 9.5, 0), 7, "y", 1, 1, 16, "bronze:plate")
    plane(m, "runner", (0, 12.1, 0), 13, 13, "y", "stone:roundel")
    plane(m, "runner", (0, 12.2, 0), 3, 3, "y", "stone:dark")
    m.bone("handle", "runner", pivot=(4.5, 12, 0))
    m.box("handle", (3.5, 12, -1), (2, 4, 2), "wood:timber")
    turn = m.anim("turn", "loop", 1.0)
    turn.spin("runner", "y", 1)
    idle = m.anim("idle", "loop", 1.0)
    idle.keys("runner", "rotation", [(0, (0, 0, 0)), (0.5, (0, 5, 0)), (1.0, (0, 0, 0))])
    return m


def treadwheel() -> Model:
    """A 48px wooden walking wheel with slatted drum on twin A-frames; the wheel turns about Z."""
    m = Model("treadwheel")
    m.bone("frame")
    for z in (-7.5, 7.5):
        m.cbox("frame", (0, 0, z), (48, 2, 1), "dark_wood:timber")
        strut(m, "frame", (-18, 1), (0, 24), "z", z, 2, 1, "wood:timber")
        strut(m, "frame", (18, 1), (0, 24), "z", z, 2, 1, "wood:timber")
        m.cbox("frame", (0, 10, z), (22, 2, 1), "dark_wood:timber")
        m.cbox("frame", (0, 21, z), (6, 6, 1), "bronze:rivet")
    m.bone("wheel", "frame", pivot=(0, 24, 0))
    for z in (-6.5, 6.5):
        ring(m, "wheel", (0, 24, z), 24, "z", 1, 2, 20, "wood:timber")
        for a in (0, 60, 120):
            radial(m, "wheel", (0, 24, z), "z", a, 0, (44, 2, 1), "wood:timber")
    ring(m, "wheel", (0, 24, 0), 22, "z", 12, 1, 24, "wood:wood", seg_len=3)
    m.box("wheel", (-3, 21, -6), (6, 6, 12), "bronze:rivet")
    m.box("wheel", (-1, 23, -8), (2, 2, 16), "iron:plate")
    spin = m.anim("spin", "loop", 2.0)
    spin.spin("wheel", "z", -1)
    idle = m.anim("idle", "loop", 1.0)
    idle.keys("wheel", "rotation", [(0, (0, 0, 0)), (0.5, (0, 0, -2)), (1.0, (0, 0, 0))])
    return m


def windmill() -> Model:
    """Tapered timber tower with a gabled cap carrying a four-sail rotor on its front at y=56."""
    m = Model("windmill")
    m.bone("tower")
    m.cbox("tower", (0, 0, 0), (16, 3, 16), "stone:cobble")
    levels = ((3, 16, 16), (17, 32, 14), (33, 46, 12))
    for y0, y1, s in levels:
        m.cbox("tower", (0, y0, 0), (s, y1 - y0, s), "pale_wood:wood:h")
        h = (s - 2) / 2.0
        for x in (-h, h):
            for z in (-h, h):
                m.cbox("tower", (x, y0, z), (2, y1 - y0, 2), "dark_wood:timber", inflate=0.1)
    m.cbox("tower", (0, 16, 0), (16, 1, 16), "dark_wood:timber")
    m.cbox("tower", (0, 32, 0), (14, 1, 14), "dark_wood:timber")
    plane(m, "tower", (0, 8.5, -8.1), 6, 9, "z", "dark_wood:wood:v")
    m.cbox("tower", (0, 12.5, -8.6), (8, 1, 1), "dark_wood:timber")
    m.cbox("tower", (2, 7, -8.6), (1, 1, 1), "bronze:plate")
    plane(m, "tower", (0, 25, -7.1), 4, 4, "z", "glass:pane:frame")
    plane(m, "tower", (0, 25, -7.05), 4, 4, "z", "dark_wood:dark")
    plane(m, "tower", (0, 40, -6.1), 3, 4, "z", "dark_wood:dark")
    m.bone("cap", "tower", pivot=(0, 46, 0))
    m.cbox("cap", (0, 46, 0), (14, 8, 16), "dark_wood:wood")
    for i, (w, y) in enumerate(((14, 54), (10, 56), (6, 58), (2, 60))):
        m.cbox("cap", (0, y, 0), (w + (2 if i == 0 else 0), 2, 16), "wood:timber" if i % 2 == 0 else "dark_wood:timber")
    m.cbox("cap", (0, 62, 5), (1, 2, 1), "bronze:plate")
    m.cbox("cap", (1.5, 62, 5), (3, 2, 0), "bronze:plate")
    m.cbox("cap", (0, 46, -7.5), (14, 2, 1), "bronze:rivet")
    m.bone("rotor", "cap", pivot=(0, 56, -11))
    m.box("rotor", (-1, 55, -10), (2, 2, 3), "iron:plate")
    m.box("rotor", (-2, 54, -12), (4, 4, 2), "bronze:rivet")
    plane(m, "rotor", (0, 56, -12.1), 4, 4, "z", "bronze:cog")
    for k in range(4):
        b = m.bone(f"blade_{k + 1}", "rotor", pivot=(0, 56, -11), rotation=(0, 0, 90 * k))
        m.box(b, (-0.5, 58, -11.5), (1, 38, 1), "dark_wood:timber")
        m.box(b, (0.5, 64, -11), (7, 32, 0), "canvas:canvas")
        m.box(b, (7.5, 64, -11.5), (1, 32, 1), "dark_wood:timber")
        for i in range(5):
            m.box(b, (0.5, 64 + 7.75 * i, -11.5), (7, 1, 1), "wood:timber")
    spin = m.anim("spin", "loop", 2.0)
    spin.spin("rotor", "z", -1)
    for deg in (2, 4, 6):
        w = m.anim(f"wobble{deg}", "loop", 2.0)
        w.keys("cap", "rotation", [(0, (0, 0, 0)), (0.5, (0, deg, 0)), (1.0, (0, 0, 0)), (1.5, (0, -deg, 0)), (2.0, (0, 0, 0))])
    idle = m.anim("idle", "loop", 1.0)
    idle.keys("rotor", "rotation", [(0, (0, 0, 0)), (0.5, (0, 0, -3)), (1.0, (0, 0, 0))])
    return m


def stone_flywheel() -> Model:
    """A 44px smooth-stone disc with bronze rim and spokes on a stone mount; the disc turns about Z at y=24."""
    m = Model("stone_flywheel")
    m.bone("mount")
    for z in (-7, 7):
        m.cbox("mount", (0, 0, z), (48, 3, 2), "stone:stone")
        m.cbox("mount", (0, 3, z), (8, 26, 2), "stone:stone")
        m.cbox("mount", (0, 29, z), (10, 2, 2), "stone:stone")
        strut(m, "mount", (-14, 3), (-4, 18), "z", z, 3, 2, "stone:stone")
        strut(m, "mount", (14, 3), (4, 18), "z", z, 3, 2, "stone:stone")
        m.cbox("mount", (0, 20, z), (6, 8, 2), "bronze:rivet", inflate=0.25)
    for x in (-22, 22):
        m.cbox("mount", (x, 0, 0), (4, 3, 12), "stone:stone")
    m.bone("disc", "mount", pivot=(0, 24, 0))
    ring(m, "disc", (0, 24, 0), 22, "z", 8, 3, 20, "stone:stone")
    disc_faces(m, "disc", (0, 24, 0), 44, "z", 8.0, "stone:roundel")
    for z in (-4.5, 4.5):
        ring(m, "disc", (0, 24, z), 22, "z", 1, 2, 20, "bronze:plate")
        for a in (0, 45, 90, 135):
            radial(m, "disc", (0, 24, z * 1.13), "z", a, 0, (40, 2, 1), "bronze:plate")
    m.box("disc", (-4, 20, -6), (8, 8, 12), "bronze:rivet")
    disc_faces(m, "disc", (0, 24, 0), 8, "z", 12.0, "bronze:cog")
    m.box("disc", (-1, 23, -8), (2, 2, 16), "iron:plate")
    spin = m.anim("spin", "loop", 1.0)
    spin.spin("disc", "z", -1)
    idle = m.anim("idle", "loop", 1.0)
    idle.keys("disc", "rotation", [(0, (0, 0, 0)), (0.5, (0, 0, -2)), (1.0, (0, 0, 0))])
    return m


def stamp_mill() -> Model:
    """Log frame over a cobble mortar; a cam on a bronze shaft lifts an iron-shod hammer that drops hard."""
    m = Model("stamp_mill")
    m.bone("frame")
    m.cbox("frame", (0, 0, 0), (16, 3, 16), "stone:cobble")
    m.cbox("frame", (0, 3, 0), (10, 5, 10), "stone:cobble", faces={"up": "stone:stone"})
    plane(m, "frame", (0, 8.1, 0), 6, 6, "y", "stone:dark")
    for x in (-6.5, 6.5):
        m.cbox("frame", (x, 3, 0), (3, 29, 3), "wood:log")
        m.cbox("frame", (x, 19, -3.5), (3, 4, 4), "dark_wood:timber")
    for z in (-1.5, 1.5):
        m.cbox("frame", (0, 11, z), (10, 2, 1), "dark_wood:timber")
        m.cbox("frame", (0, 27, z), (10, 2, 1), "dark_wood:timber")
    m.cbox("frame", (0, 29, 0), (16, 3, 4), "dark_wood:timber")
    for x in (-6.5, 6.5):
        m.cbox("frame", (x, 28.5, 0), (3, 1, 4), "bronze:plate", inflate=0.2)
    m.bone("cam", "frame", pivot=(0, 22, -5))
    m.box("cam", (-5, 21, -6), (10, 2, 2), "bronze:plate")
    radial(m, "cam", (0, 22, -5), "x", -90, 2.5, (2, 3, 5), "bronze:rivet")
    plane(m, "cam", (2.1, 22, -5), 4, 4, "x", "bronze:cog")
    plane(m, "cam", (-2.1, 22, -5), 4, 4, "x", "bronze:cog")
    m.bone("hammer", "frame", pivot=(0, 8, 0))
    m.box("hammer", (-1, 10, -1), (2, 11, 2), "wood:timber")
    m.box("hammer", (-2, 8, -2), (4, 3, 4), "iron:rivet")
    m.box("hammer", (-1.5, 14, -1.5), (3, 1, 3), "iron:plate")
    m.box("hammer", (-1, 17, -4), (2, 2, 3), "bronze:plate")
    stamp = m.anim("stamp", "loop", 2.0)
    stamp.keys("hammer", "position", [(0, (0, 0, 0)), (0.8, (0, 4, 0)), (1.6, (0, 8, 0)), (1.8, (0, 0, 0)), (1.9, (0, 0.6, 0)), (2.0, (0, 0, 0))])
    stamp.spin("cam", "x", 1)
    idle = m.anim("idle", "loop", 1.0)
    idle.keys("cam", "rotation", [(0, (0, 0, 0)), (0.5, (4, 0, 0)), (1.0, (0, 0, 0))])
    return m


MODELS = {"hand_crank": hand_crank, "quern": quern, "treadwheel": treadwheel, "windmill": windmill,
          "stone_flywheel": stone_flywheel, "stamp_mill": stamp_mill, "colony_core": colony_core,
          "worker_assembler": worker_assembler}

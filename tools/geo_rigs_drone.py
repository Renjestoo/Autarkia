"""Drone rig: quad-rotor worker (Builder, Surveyor, Cable-Layer, Sentinel), 8 px tall, front -Z.

Materials use the rig regions primary, secondary, lamp and neutral. Left parts sit on file +X;
tool bones hang below the hull (the drone flies, so they may reach below y = 0).
"""

import math

from geo_core import Model

ROTORS = (("fl", 1, -1), ("fr", -1, -1), ("bl", 1, 1), ("br", -1, 1))


def _side(o, s, sign: int):
    """Return the origin of a cube given for the left (+X) side, mirrored when sign is -1."""
    return o if sign > 0 else (-(o[0] + s[0]), o[1], o[2])


def _sym(m: Model, bone: str, o, s, mat: str, sign: int, **kw) -> None:
    m.box(bone, _side(o, s, sign), s, mat, **kw)


def _body(m: Model) -> None:
    m.bone("hull", "root", pivot=(0, 4, 0))
    for sign in (1, -1):
        _sym(m, "hull", (2.5, 0, -3), (1, 1, 6), "neutral:plate", sign)
        _sym(m, "hull", (2.5, 1, -2), (1, 1, 1), "neutral:flat:2", sign)
        _sym(m, "hull", (2.5, 1, 1), (1, 1, 1), "neutral:flat:2", sign)
    m.box("hull", (-3, 2, -3.5), (6, 4, 7), "primary:rivet")
    m.box("hull", (-2, 6, -2), (4, 1, 4), "secondary:panel:4")
    m.box("hull", (-2, 2.5, -4.5), (4, 3, 1), "secondary:plate")
    m.bone("eye_lamp", "hull", pivot=(0, 4, -5))
    m.box("eye_lamp", (-1, 3, -5), (2, 2, 1), "lamp:glow")
    m.bone("battery_pack", "hull", pivot=(0, 1.5, 0))
    m.box("battery_pack", (-2, 1, -2.5), (4, 1, 5), "neutral:rivet")
    m.box("battery_pack", (-1, 1, 2.5), (2, 1, 1), "lamp:glow")
    for tag, sx, sz in ROTORS:
        cx, cz = 6.0 * sx, 6.0 * sz
        rotor = m.bone(f"rotor_{tag}", "hull", pivot=(2.5 * sx, 5.5, 3 * sz))
        m.box(rotor, (4.5 * sx - 2, 5, 4.75 * sz - 0.5), (4, 1, 1), "neutral:plate",
              rotation=(0, -40 * sx * sz, 0), pivot=(4.5 * sx, 5.5, 4.75 * sz))
        m.box(rotor, (cx - 1, 4.5, cz - 1), (2, 2, 2), "primary:rivet")
        m.box(rotor, (cx - 0.5, 3.5, cz - 0.5), (1, 1, 1), "neutral:flat:1")
        blade = m.bone(f"blade_{tag}", rotor, pivot=(cx, 7, cz))
        m.box(blade, (cx - 0.5, 6.5, cz - 0.5), (1, 1, 1), "secondary:plate")
        m.box(blade, (cx - 4, 7, cz - 0.5), (8, 0, 1), "neutral:flat:3")
        m.box(blade, (cx - 0.5, 7, cz - 4), (1, 0, 8), "neutral:flat:3")
    m.bone("cargo_slot", "hull", pivot=(0, -1, 0))


def _optional(m: Model) -> None:
    m.bone("cargo_claw", "hull", pivot=(0, 1, 0))
    m.box("cargo_claw", (-0.5, -1, -0.5), (1, 2, 1), "neutral:plate")
    m.box("cargo_claw", (-2, -1.5, -0.5), (4, 1, 1), "secondary:plate")
    for sign in (1, -1):
        _sym(m, "cargo_claw", (1, -3.5, -0.5), (1, 2, 1), "neutral:plate", sign)
        _sym(m, "cargo_claw", (0.5, -3.5, -0.5), (1, 1, 1), "neutral:flat:3", sign)
    m.bone("weld_arm", "hull", pivot=(-2.5, 2, -3))
    m.box("weld_arm", (-3, 0, -3.5), (1, 2, 1), "neutral:plate")
    m.box("weld_arm", (-3, 0, -6.5), (1, 1, 3), "secondary:plate")
    m.bone("weld_tip", "weld_arm", pivot=(-2.5, 0.5, -7))
    m.box("weld_tip", (-3, 0, -7.5), (1, 1, 1), "neutral:flat:3")
    m.box("weld_tip", (-3, 0, -8.5), (1, 1, 1), "lamp:glow")
    m.bone("nozzle", "hull", pivot=(2.5, 1, -4))
    m.box("nozzle", (2, 0, -5), (1, 2, 2), "primary:plate")
    m.box("nozzle", (2, 0.5, -7), (1, 1, 2), "neutral:plate")
    m.bone("scanner_dish", "hull", pivot=(0, 1, 0))
    m.box("scanner_dish", (-0.5, 0, -0.5), (1, 1, 1), "neutral:plate")
    m.box("scanner_dish", (-2.5, -1, -2.5), (5, 1, 5), "secondary:panel:5")
    m.box("scanner_dish", (-0.5, -2, -0.5), (1, 1, 1), "lamp:glow")
    m.bone("scanner_beam", "hull", pivot=(0, -2, 0))
    m.box("scanner_beam", (-1.5, -9, 0), (3, 7, 0), "lamp:glow")
    m.box("scanner_beam", (0, -9, -1.5), (0, 7, 3), "lamp:glow")
    m.bone("spool", "hull", pivot=(0, -0.5, 2.5))
    m.box("spool", (-2.5, -2, 1), (5, 3, 3), "secondary:coil:v")
    for sign in (1, -1):
        _sym(m, "spool", (2.5, -2.5, 0.5), (1, 4, 4), "primary:plate", sign)
    m.box("spool", (-0.5, 1, 2), (1, 1, 1), "neutral:plate")
    m.bone("winch_hook", "hull", pivot=(0, -2, 2.5))
    m.box("winch_hook", (-0.5, -6, 2), (1, 4, 1), "neutral:flat:1")
    m.box("winch_hook", (-1, -8, 2), (2, 2, 1), "neutral:rivet")
    m.box("winch_hook", (0.5, -9, 2), (1, 1, 1), "neutral:plate")
    m.bone("heavy_plates", "hull", pivot=(0, 4, 0))
    for sign in (1, -1):
        _sym(m, "heavy_plates", (3, 2, -3), (1, 4, 6), "secondary:rivet", sign)
    m.box("heavy_plates", (-2.5, 6, -3), (5, 1, 1), "secondary:plate")
    m.bone("arc_gun", "hull", pivot=(0, 1, -3))
    m.box("arc_gun", (-1.5, -0.5, -4.5), (3, 2, 3), "neutral:rivet")
    m.box("arc_gun", (-0.5, 0, -8.5), (1, 1, 4), "primary:plate")
    m.bone("arc_coil", "arc_gun", pivot=(0, 0.5, -6.5))
    m.box("arc_coil", (-1, -0.5, -7.5), (2, 2, 1), "lamp:glow")
    m.box("arc_coil", (-1, -0.5, -6), (2, 2, 1), "lamp:glow")


def _upgrades(m: Model) -> None:
    m.bone("upg_armor", "hull", pivot=(0, 4, -4))
    m.box("upg_armor", (-3, 2, -5), (6, 1, 1), "primary:rivet")
    m.box("upg_armor", (-2.5, 7, -1.5), (5, 1, 3), "neutral:rivet")
    m.bone("upg_cargo", "hull", pivot=(0, 4, 4.5))
    m.box("upg_cargo", (-2, 2.5, 3.5), (4, 3, 2), "secondary:rivet")
    m.bone("upg_range", "hull", pivot=(2, 6, 3))
    m.box("upg_range", (1.5, 6, 2.5), (1, 4, 1), "neutral:plate")
    m.box("upg_range", (1, 10, 2), (2, 1, 2), "lamp:glow")
    m.bone("upg_speed", "hull", pivot=(0, 4, 4.5))
    for sign in (1, -1):
        _sym(m, "upg_speed", (2, 3, 3.5), (1, 2, 2), "secondary:panel:3", sign)
        _sym(m, "upg_speed", (2, 3.5, 5.5), (1, 1, 1), "lamp:glow", sign)


def _anims(m: Model) -> None:
    idle = m.anim("idle", "loop", 2.0)
    idle.osc("hull", "position", (0, 0.6, 0), steps=8)
    idle.osc("hull", "rotation", (2, 0, 2.5), phase=0.25, steps=8)
    idle.keys("eye_lamp", "scale", [(0, (1, 1, 1)), (1, (0.85, 0.85, 1)), (2, (1, 1, 1))])
    idle.osc("winch_hook", "rotation", (6, 0, 4), phase=0.1, steps=8)

    fly = m.anim("fly", "loop", 1.0)
    fly.osc("hull", "position", (0, 0.4, 0), steps=8)
    fly.keys("hull", "rotation", [(t, (12, 0, 2 * math.sin(2 * math.pi * t))) for t in (0, 0.25, 0.5, 0.75, 1.0)])
    fly.keys("winch_hook", "rotation", [(0, (-18, 0, 0)), (0.5, (-14, 0, 0)), (1, (-18, 0, 0))])
    for a in (idle, fly):
        a.keys("scanner_beam", "scale", [(0, (0, 0, 0)), (a.length, (0, 0, 0))])

    carry = m.anim("carry", "loop", 1.0)
    carry.keys("cargo_claw", "position", [(0, (0, -0.5, 0)), (0.5, (0, -0.8, 0)), (1, (0, -0.5, 0))])
    carry.keys("cargo_claw", "scale", [(0, (0.8, 1, 1)), (1, (0.8, 1, 1))])
    carry.keys("hull", "rotation", [(0, (-4, 0, 0)), (0.5, (-3, 0, 0)), (1, (-4, 0, 0))])
    carry.keys("hull", "position", [(0, (0, -0.3, 0)), (0.5, (0, 0.2, 0)), (1, (0, -0.3, 0))])

    work = m.anim("work", "loop", 1.0)
    work.osc("weld_arm", "rotation", (-15, 10, 0), base=(-10, 0, 0), steps=8)
    work.osc("nozzle", "rotation", (0, 20, 0), phase=0.25, steps=8)
    work.spin("cargo_claw", "y", 1)
    work.osc("hull", "position", (0, 0.3, 0), cycles=2, steps=8)
    work.osc("weld_tip", "scale", (0.4, 0.4, 0.4), cycles=4, base=(1, 1, 1), steps=16)

    place = m.anim("place", "once", 0.8)
    place.keys("cargo_claw", "position", [(0, (0, 0, 0)), (0.35, (0, -3, 0)), (0.5, (0, -3, 0)), (0.8, (0, 0, 0))])
    place.keys("cargo_claw", "scale", [(0, (0.8, 1, 1)), (0.4, (1.2, 1, 1)), (0.8, (1, 1, 1))])
    place.keys("hull", "position", [(0, (0, 0, 0)), (0.35, (0, -0.8, 0)), (0.8, (0, 0, 0))])

    weld = m.anim("weld", "once", 1.0)
    weld.keys("weld_arm", "rotation", [(0, (0, 0, 0)), (0.15, (-25, 0, 0)), (0.3, (-22, 6, 0)), (0.45, (-27, -6, 0)),
                                       (0.6, (-23, 4, 0)), (0.85, (-25, 0, 0)), (1, (0, 0, 0))])
    weld.keys("weld_tip", "scale", [(0, (1, 1, 1)), (0.2, (1.6, 1.6, 1.6)), (0.3, (0.8, 0.8, 0.8)), (0.45, (1.5, 1.5, 1.5)),
                                    (0.6, (0.9, 0.9, 0.9)), (0.75, (1.4, 1.4, 1.4)), (1, (1, 1, 1))])
    weld.keys("hull", "rotation", [(0, (0, 0, 0)), (0.15, (8, 0, 0)), (0.85, (8, 0, 0)), (1, (0, 0, 0))])

    scan = m.anim("scan", "once", 2.0)
    scan.spin("scanner_dish", "y", 2, steps=16)
    scan.keys("scanner_beam", "scale", [(0, (0.2, 0.2, 0.2)), (0.3, (1, 1, 1)), (0.6, (1.3, 1, 1.3)), (1.0, (1, 1, 1)),
                                        (1.4, (1.3, 1, 1.3)), (1.8, (1, 1, 1)), (2.0, (0.2, 0.2, 0.2))])
    scan.keys("hull", "rotation", [(0, (0, 0, 0)), (0.3, (-4, 0, 0)), (1.7, (-4, 0, 0)), (2.0, (0, 0, 0))])
    scan.keys("eye_lamp", "scale", [(0, (1, 1, 1)), (0.3, (1.3, 1.3, 1)), (1.7, (1.3, 1.3, 1)), (2.0, (1, 1, 1))])

    string = m.anim("string", "once", 1.0)
    string.spin("spool", "x", 2, steps=16)
    string.keys("winch_hook", "position", [(0, (0, 0, 0)), (0.4, (0, -3, 0)), (0.6, (0, -3, 0)), (1.0, (0, 0, 0))])
    string.keys("hull", "position", [(0, (0, 0, 0)), (0.5, (0, 0.8, 0)), (1.0, (0, 0, 0))])
    string.keys("cargo_claw", "rotation", [(0, (0, 0, 0)), (0.5, (0, 90, 0)), (1.0, (0, 0, 0))])

    shoot = m.anim("shoot", "once", 0.5)
    shoot.keys("arc_gun", "position", [(0, (0, 0, 0)), (0.05, (0, 0, 1.5)), (0.4, (0, 0, 0))])
    shoot.keys("arc_coil", "scale", [(0, (1, 1, 1)), (0.05, (1.6, 1.6, 1.2)), (0.25, (1.1, 1.1, 1)), (0.5, (1, 1, 1))])
    shoot.keys("hull", "rotation", [(0, (0, 0, 0)), (0.05, (-6, 0, 0)), (0.4, (0, 0, 0))])
    shoot.keys("hull", "position", [(0, (0, 0, 0)), (0.05, (0, 0, 0.8)), (0.4, (0, 0, 0))])

    unfold = m.anim("unfold", "once", 1.5)
    for tag, sx, sz in ROTORS:
        folded = (0, 50 * sx * sz, 0)
        unfold.keys(f"rotor_{tag}", "rotation", [(0, folded), (0.4, folded), (1.0, (0, -6 * sx * sz, 0)), (1.2, (0, 0, 0))])
        unfold.keys(f"blade_{tag}", "scale", [(0, (0.4, 1, 0.4)), (0.6, (0.4, 1, 0.4)), (1.1, (1, 1, 1))])
    unfold.keys("hull", "position", [(0, (0, -1, 0)), (0.9, (0, -1, 0)), (1.3, (0, 1, 0)), (1.5, (0, 0, 0))])
    unfold.keys("eye_lamp", "scale", [(0, (0.2, 0.2, 1)), (1.0, (0.2, 0.2, 1)), (1.2, (1.3, 1.3, 1)), (1.5, (1, 1, 1))])

    recharge = m.anim("recharge", "loop", 2.0)
    recharge.keys("hull", "position", [(0, (0, -0.5, 0)), (2, (0, -0.5, 0))])
    recharge.keys("eye_lamp", "scale", [(0, (0.6, 0.6, 1)), (1, (1, 1, 1)), (2, (0.6, 0.6, 1))])
    recharge.keys("battery_pack", "scale", [(0, (1, 1, 1)), (1, (1.05, 1.2, 1.05)), (2, (1, 1, 1))])

    rundown = m.anim("rundown", "hold", 2.0)
    rundown.keys("hull", "position", [(0, (0, 0, 0)), (0.3, (0, 0.6, 0)), (0.9, (0, -0.6, 0)), (1.1, (0, -0.2, 0)),
                                      (1.3, (0, -0.6, 0))])
    rundown.keys("hull", "rotation", [(0, (0, 0, 0)), (1.0, (5, 0, 9)), (1.3, (4, 0, 7))])
    rundown.keys("eye_lamp", "scale", [(0, (1, 1, 1)), (0.6, (0.6, 0.6, 1)), (0.8, (0.9, 0.9, 1)), (2.0, (0.3, 0.3, 1))])
    rundown.keys("winch_hook", "rotation", [(0, (0, 0, 0)), (1.3, (0, 0, 60))])
    rundown.keys("scanner_beam", "scale", [(0, (0, 0, 0)), (2.0, (0, 0, 0))])

    death = m.anim("death", "hold", 1.5)
    death.keys("hull", "position", [(0, (0, 0, 0)), (0.3, (0, 1, 0)), (0.8, (0, -2, 0)), (1.0, (0, -1.5, 0)), (1.2, (0, -2, 0))])
    death.keys("hull", "rotation", [(0, (0, 0, 0)), (0.3, (-10, 30, 15)), (0.8, (15, 60, 35)), (1.2, (12, 60, 30))])
    death.keys("rotor_fl", "rotation", [(0, (0, 0, 0)), (0.8, (0, 0, 30))])
    death.keys("rotor_br", "rotation", [(0, (0, 0, 0)), (0.8, (20, 0, 0))])
    death.keys("eye_lamp", "scale", [(0, (1, 1, 1)), (0.6, (1, 1, 1)), (0.8, (0, 0, 1))])
    death.keys("scanner_beam", "scale", [(0, (0, 0, 0)), (1.5, (0, 0, 0))])

    spin = m.anim("rotor_spin", "loop", 0.5)
    for tag, sx, sz in ROTORS:
        spin.spin(f"blade_{tag}", "y", sx * sz)


def drone() -> Model:
    """Build the drone rig with every base, optional and upgrade bone and all drone animations."""
    m = Model("drone")
    m.bone("root")
    _body(m)
    _optional(m)
    _upgrades(m)
    _anims(m)
    return m

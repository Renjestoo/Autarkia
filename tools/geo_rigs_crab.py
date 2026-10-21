"""Crab rig: six-legged flat worker (Hauler Crab, Loader), 11 px tall, front -Z.

Materials use the rig regions primary, secondary, lamp and neutral. Left parts sit on file +X.
"""

import math

from geo_core import Model

LEGS = ((1, -3.0, 25.0), (2, 0.0, 0.0), (3, 3.0, -25.0))
GROUP_A = {"leg_l1", "leg_r2", "leg_l3"}


def _side(o, s, sign: int):
    """Return the origin of a cube given for the left (+X) side, mirrored when sign is -1."""
    return o if sign > 0 else (-(o[0] + s[0]), o[1], o[2])


def _sym(m: Model, bone: str, o, s, mat: str, sign: int, **kw) -> None:
    m.box(bone, _side(o, s, sign), s, mat, **kw)


def _body(m: Model) -> None:
    m.bone("carapace", "root", pivot=(0, 5, 0))
    m.box("carapace", (-5, 3, -4), (10, 1, 8), "neutral:plate")
    m.box("carapace", (-6, 4, -5), (12, 4, 10), "primary:rivet")
    m.box("carapace", (-5, 8, -4), (10, 1, 8), "primary:plate")
    m.box("carapace", (-5, 4, -6), (10, 2, 1), "secondary:grille")
    m.box("carapace", (-5, 4.5, 5), (10, 3, 1), "neutral:vent")
    m.bone("eye_lamp", "carapace", pivot=(0, 6.5, -6))
    m.box("eye_lamp", (-3, 6, -6.5), (6, 1, 1), "lamp:glow")
    for side, sign in (("l", 1), ("r", -1)):
        stalk = m.bone(f"eye_stalk_{side}", "carapace", pivot=(2 * sign, 9, -3.5))
        _sym(m, stalk, (1.5, 9, -4), (1, 1, 1), "neutral:plate", sign)
        _sym(m, stalk, (1, 10, -4.5), (2, 1, 2), "secondary:plate", sign, faces={"north": "lamp:glow"})
        for i, z, fan in LEGS:
            leg = m.bone(f"leg_{side}{i}", "carapace", pivot=(6 * sign, 5.5, z), rotation=(0, fan * sign, 0))
            _sym(m, leg, (6, 5, z - 0.5), (4, 1, 1), "primary:plate", sign)
            _sym(m, leg, (9, 4.5, z - 1), (2, 2, 2), "neutral:rivet", sign)
            _sym(m, leg, (9.5, 0, z - 0.5), (1, 5, 1), "neutral:plate", sign)
    m.bone("cargo_slot", "carapace", pivot=(0, 10, 2))


def _optional(m: Model) -> None:
    for side, sign in (("l", 1), ("r", -1)):
        claw = m.bone(f"claw_{side}", "carapace", pivot=(4 * sign, 5, -5))
        _sym(m, claw, (3, 4, -8), (2, 2, 3), "primary:plate", sign)
        _sym(m, claw, (2.5, 3.5, -10), (3, 3, 2), "secondary:rivet", sign)
        _sym(m, claw, (2.5, 5.5, -12), (3, 1, 2), "neutral:plate", sign)
        _sym(m, claw, (2.5, 3.5, -12), (3, 1, 2), "neutral:plate", sign)
    m.bone("cargo_bed", "carapace", pivot=(0, 9, 2))
    m.box("cargo_bed", (-5, 9, -1), (10, 1, 6), "secondary:panel:5")
    for sign in (1, -1):
        _sym(m, "cargo_bed", (4, 10, -1), (1, 1, 6), "secondary:plate", sign)
    m.box("cargo_bed", (-4, 10, 4), (8, 1, 1), "secondary:plate")
    m.box("cargo_bed", (-4, 10, -1), (8, 1, 1), "secondary:plate")
    m.bone("debris_scoop", "carapace", pivot=(0, 3.5, -5))
    for sign in (1, -1):
        _sym(m, "debris_scoop", (1.5, 1.5, -6), (1, 2, 1), "neutral:plate", sign)
        _sym(m, "debris_scoop", (2, 1.5, -9.5), (1, 1, 3), "secondary:plate", sign)
    m.box("debris_scoop", (-3, 0.5, -9.5), (6, 1, 4), "secondary:rivet")
    m.bone("spring_key", "carapace", pivot=(0, 6, 6.5))
    m.box("spring_key", (-0.5, 5.5, 5), (1, 1, 2), "neutral:plate")
    m.box("spring_key", (-0.5, 5, 7), (1, 2, 1), "primary:plate")
    for sign in (1, -1):
        _sym(m, "spring_key", (0.5, 4.5, 7), (2, 3, 1), "primary:rivet", sign)
    m.bone("battery_pack", "carapace", pivot=(0, 6, 6.5))
    m.box("battery_pack", (-3, 4, 5.5), (6, 4, 2), "neutral:rivet")
    m.box("battery_pack", (-2, 6, 7.5), (4, 1, 1), "lamp:glow")
    m.bone("fork_mast", "carapace", pivot=(0, 1, -7.5))
    for sign in (1, -1):
        _sym(m, "fork_mast", (3, 0, -8), (1, 14, 1), "primary:plate", sign)
        _sym(m, "fork_mast", (3, 3, -7), (1, 1, 1), "neutral:plate", sign)
        _sym(m, "fork_mast", (3, 9, -7), (1, 1, 2), "neutral:plate", sign)
    m.box("fork_mast", (-4, 13, -8), (8, 1, 1), "primary:rivet")
    m.box("fork_mast", (-2, 14, -8), (4, 1, 1), "lamp:glow")
    m.bone("fork_carriage", "fork_mast", pivot=(0, 1, -8.5))
    m.box("fork_carriage", (-4, 1, -9), (8, 5, 1), "secondary:grille")
    m.box("fork_carriage", (-4, 6, -9), (8, 1, 1), "neutral:plate")
    m.bone("fork_tines", "fork_carriage", pivot=(0, 1, -12))
    for sign in (1, -1):
        _sym(m, "fork_tines", (1.5, 0.5, -15), (2, 1, 6), "neutral:plate", sign)
        _sym(m, "fork_tines", (1.5, 1, -10), (2, 3, 1), "neutral:plate", sign)
    m.bone("pallet_slot", "fork_carriage", pivot=(0, 2, -12))


def _upgrades(m: Model) -> None:
    m.bone("upg_armor", "carapace", pivot=(0, 5, 0))
    for sign in (1, -1):
        _sym(m, "upg_armor", (6, 3, -4), (1, 4, 8), "primary:rivet", sign)
    m.bone("upg_cargo", "carapace", pivot=(0, 2, 1.5))
    m.box("upg_cargo", (-4, 1, -1), (8, 2, 5), "secondary:rivet")
    m.bone("upg_range", "carapace", pivot=(5.5, 9, 4))
    m.box("upg_range", (5, 9, 3.5), (1, 6, 1), "neutral:plate")
    m.box("upg_range", (4.5, 15, 3), (2, 1, 2), "lamp:glow")
    m.bone("upg_speed", "carapace", pivot=(0, 5, 7))
    for sign in (1, -1):
        _sym(m, "upg_speed", (4, 4, 6), (1, 3, 2), "secondary:panel:3", sign)
        _sym(m, "upg_speed", (4, 4.5, 8), (1, 1, 1), "lamp:glow", sign)


def _leg_names():
    """Yield (bone name, side sign) for all six legs."""
    for side, sign in (("l", 1), ("r", -1)):
        for i, _z, _fan in LEGS:
            yield f"leg_{side}{i}", sign


def _leg_keys(a, name: str, sign: int, frames) -> None:
    """Key one leg from (t, lift, forward swing) degrees; signs mirror per side."""
    a.keys(name, "rotation", [(t, (0, sign * swing, -sign * lift)) for t, lift, swing in frames])


def _all_legs(a, frames) -> None:
    """Key every leg with the same (t, lift) frames and no swing."""
    for name, sign in _leg_names():
        _leg_keys(a, name, sign, [(t, lift, 0) for t, lift in frames])


def _gait(name: str, t: float) -> tuple[float, float]:
    """Return (lift, swing) of a tripod gait over 0.8 s; GROUP_A legs swing while the others stand."""
    u = (t / 0.8 + (0.0 if name in GROUP_A else 0.5)) % 1.0
    if u < 0.5:
        return 22 * math.sin(math.pi * u / 0.5), -14 + 28 * (u / 0.5)
    return 0.0, 14 - 28 * ((u - 0.5) / 0.5)


def _anims(m: Model) -> None:
    idle = m.anim("idle", "loop", 2.0)
    idle.keys("carapace", "position", [(0, (0, 0, 0)), (1, (0, -0.3, 0)), (2, (0, 0, 0))])
    idle.keys("eye_stalk_l", "rotation", [(0, (0, 0, 0)), (0.7, (0, 0, -10)), (1.4, (8, 0, 4)), (2, (0, 0, 0))])
    idle.keys("eye_stalk_r", "rotation", [(0, (0, 0, 0)), (0.5, (6, 0, 8)), (1.3, (0, 0, -4)), (2, (0, 0, 0))])
    for side, sign in (("l", 1), ("r", -1)):
        idle.keys(f"claw_{side}", "rotation", [(0, (0, 0, 0)), (1, (-6, 4 * sign, 0)), (2, (0, 0, 0))])
    idle.keys("eye_lamp", "scale", [(0, (1, 1, 1)), (1, (0.85, 1, 1)), (2, (1, 1, 1))])

    walk = m.anim("walk", "loop", 0.8)
    frames = [0.8 * k / 8 for k in range(9)]
    for name, sign in _leg_names():
        _leg_keys(walk, name, sign, [(t, *_gait(name, t)) for t in frames])
    walk.keys("carapace", "position", [(t, (0, 0.3 * abs(math.sin(2 * math.pi * t / 0.8)), 0)) for t in frames])
    walk.keys("carapace", "rotation", [(t, (0, 0, 2 * math.sin(2 * math.pi * t / 0.8))) for t in frames])

    carry = m.anim("carry", "loop", 1.0)
    for side, sign in (("l", 1), ("r", -1)):
        hold = (-28, -10 * sign, 0)
        carry.keys(f"claw_{side}", "rotation", [(0, hold), (0.5, (-24, -10 * sign, 0)), (1, hold)])
    carry.keys("fork_carriage", "position", [(0, (0, 5, 0)), (1, (0, 5, 0))])
    carry.keys("fork_mast", "rotation", [(0, (-5, 0, 0)), (1, (-5, 0, 0))])
    carry.keys("carapace", "position", [(0, (0, -0.4, 0)), (0.5, (0, -0.2, 0)), (1, (0, -0.4, 0))])

    work = m.anim("work", "loop", 1.0)
    work.keys("claw_l", "rotation", [(0, (0, 0, 0)), (0.25, (18, 12, 0)), (0.5, (0, 0, 0)), (1, (0, 0, 0))])
    work.keys("claw_r", "rotation", [(0, (0, 0, 0)), (0.5, (0, 0, 0)), (0.75, (18, -12, 0)), (1, (0, 0, 0))])
    work.keys("carapace", "rotation", [(0, (3, 0, 0)), (0.25, (5, 0, 2)), (0.75, (5, 0, -2)), (1, (3, 0, 0))])
    work.keys("debris_scoop", "rotation", [(0, (0, 0, 0)), (0.5, (12, 0, 0)), (1, (0, 0, 0))])
    work.keys("fork_carriage", "position", [(0, (0, 0, 0)), (0.5, (0, 2, 0)), (1, (0, 0, 0))])

    place = m.anim("place", "once", 0.8)
    for side in ("l", "r"):
        place.keys(f"claw_{side}", "rotation", [(0, (0, 0, 0)), (0.35, (28, 0, 0)), (0.5, (24, 0, 0)), (0.8, (0, 0, 0))])
    place.keys("carapace", "rotation", [(0, (0, 0, 0)), (0.35, (7, 0, 0)), (0.8, (0, 0, 0))])
    place.keys("fork_carriage", "position", [(0, (0, 3, 0)), (0.4, (0, 0, 0)), (0.8, (0, 0, 0))])

    lift = m.anim("lift", "once", 1.0)
    lift.keys("fork_carriage", "position", [(0, (0, 0, 0)), (0.8, (0, 6, 0)), (1.0, (0, 5.5, 0))])
    lift.keys("fork_mast", "rotation", [(0, (0, 0, 0)), (0.8, (-6, 0, 0)), (1.0, (-5, 0, 0))])
    lift.keys("carapace", "position", [(0, (0, 0, 0)), (0.4, (0, -0.6, 0)), (1.0, (0, 0, 0))])

    lower = m.anim("lower", "once", 1.0)
    lower.keys("fork_carriage", "position", [(0, (0, 5.5, 0)), (0.8, (0, 0, 0)), (1.0, (0, 0, 0))])
    lower.keys("fork_mast", "rotation", [(0, (-5, 0, 0)), (0.8, (2, 0, 0)), (1.0, (0, 0, 0))])
    lower.keys("carapace", "rotation", [(0, (0, 0, 0)), (0.7, (4, 0, 0)), (1.0, (0, 0, 0))])

    scoop = m.anim("scoop", "once", 1.0)
    scoop.keys("debris_scoop", "rotation", [(0, (0, 0, 0)), (0.3, (35, 0, 0)), (0.65, (-30, 0, 0)), (1.0, (0, 0, 0))])
    scoop.keys("carapace", "rotation", [(0, (0, 0, 0)), (0.3, (9, 0, 0)), (0.65, (-4, 0, 0)), (1.0, (0, 0, 0))])
    for side, sign in (("l", 1), ("r", -1)):
        scoop.keys(f"claw_{side}", "rotation", [(0, (0, 0, 0)), (0.3, (20, 15 * sign, 0)), (0.65, (-15, 0, 0)), (1.0, (0, 0, 0))])

    unfold = m.anim("unfold", "once", 1.5)
    unfold.keys("carapace", "position", [(0, (0, -3, 0)), (0.6, (0, -3, 0)), (1.2, (0, 0.6, 0)), (1.4, (0, 0, 0))])
    _all_legs(unfold, [(0, 70), (0.3, 70), (1.1, 0)])
    for side in ("l", "r"):
        unfold.keys(f"eye_stalk_{side}", "scale", [(0, (1, 0.1, 1)), (1.1, (1, 0.1, 1)), (1.3, (1, 1.3, 1)), (1.5, (1, 1, 1))])
        unfold.keys(f"claw_{side}", "rotation", [(0, (-70, 0, 0)), (0.6, (-70, 0, 0)), (1.2, (0, 0, 0))])
    unfold.keys("eye_lamp", "scale", [(0, (0.2, 1, 1)), (1.2, (0.2, 1, 1)), (1.4, (1.3, 1, 1)), (1.5, (1, 1, 1))])
    unfold.keys("fork_mast", "rotation", [(0, (60, 0, 0)), (0.6, (60, 0, 0)), (1.3, (0, 0, 0))])

    recharge = m.anim("recharge", "loop", 2.0)
    recharge.keys("carapace", "position", [(0, (0, -1.5, 0)), (2, (0, -1.5, 0))])
    _all_legs(recharge, [(0, 18), (2.0, 18)])
    for side in ("l", "r"):
        recharge.keys(f"eye_stalk_{side}", "rotation", [(0, (30, 0, 0)), (2, (30, 0, 0))])
    recharge.keys("eye_lamp", "scale", [(0, (0.6, 1, 1)), (1, (1, 1, 1)), (2, (0.6, 1, 1))])
    recharge.spin("spring_key", "z", 3)

    rundown = m.anim("rundown", "hold", 2.0)
    rundown.keys("carapace", "position", [(0, (0, 0, 0)), (1.3, (0, -2.8, 0))])
    rundown.keys("carapace", "rotation", [(0, (0, 0, 0)), (1.3, (4, 0, 3))])
    _all_legs(rundown, [(0, 0), (1.3, 32)])
    for side, sign in (("l", 1), ("r", -1)):
        rundown.keys(f"eye_stalk_{side}", "rotation", [(0, (0, 0, 0)), (1.5, (55, 0, -15 * sign))])
        rundown.keys(f"claw_{side}", "rotation", [(0, (0, 0, 0)), (1.3, (25, 0, 0))])
    rundown.keys("eye_lamp", "scale", [(0, (1, 1, 1)), (0.8, (0.6, 1, 1)), (1.0, (0.9, 1, 1)), (2.0, (0.3, 1, 1))])
    rundown.keys("spring_key", "rotation", [(0, (0, 0, 0)), (2, (0, 0, 40))])

    death = m.anim("death", "hold", 1.5)
    death.keys("carapace", "position", [(0, (0, 0, 0)), (0.5, (0, 2, 0)), (1.0, (0, -3, 0))])
    death.keys("carapace", "rotation", [(0, (0, 0, 0)), (0.5, (0, 0, 40)), (1.0, (0, 0, 18)), (1.2, (0, 0, 22))])
    _all_legs(death, [(0, 0), (0.8, 75), (1.1, 70)])
    for side in ("l", "r"):
        death.keys(f"eye_stalk_{side}", "rotation", [(0, (0, 0, 0)), (1.0, (70, 0, 0))])
    death.keys("eye_lamp", "scale", [(0, (1, 1, 1)), (0.8, (1, 1, 1)), (1.0, (0, 0, 1))])


def crab() -> Model:
    """Build the crab rig with every base, optional and upgrade bone and all crab animations."""
    m = Model("crab")
    m.bone("root")
    _body(m)
    _optional(m)
    _upgrades(m)
    _anims(m)
    return m

"""Walker rig: squat bipedal worker (Porter, Lineman, Replicator Walker), 21 px tall, front -Z.

Materials use the rig regions primary, secondary, lamp and neutral; geo_rigs turns them into a grey
template plus an R/G/B region mask. Left parts sit on file +X, right parts on file -X (GeckoLib mirror).
"""

import math

from geo_core import Model


def _side(o, s, sign: int):
    """Return the origin of a cube given for the left (+X) side, mirrored when sign is -1."""
    return o if sign > 0 else (-(o[0] + s[0]), o[1], o[2])


def _sym(m: Model, bone: str, o, s, mat: str, sign: int, **kw) -> None:
    m.box(bone, _side(o, s, sign), s, mat, **kw)


def _legs(m: Model) -> None:
    for side, sign in (("l", 1), ("r", -1)):
        leg = m.bone(f"leg_{side}", "root", pivot=(2.5 * sign, 8, 0))
        _sym(m, leg, (1, 5, -1.5), (3, 3, 3), "primary:rivet", sign)
        _sym(m, leg, (1.5, 2, -1), (2, 3, 2), "neutral:plate", sign)
        _sym(m, leg, (1, 4, -2), (3, 1, 1), "neutral:flat:3", sign)
        foot = m.bone(f"foot_{side}", leg, pivot=(2.5 * sign, 2, 0))
        _sym(m, foot, (0.5, 0, -3.5), (4, 2, 5), "secondary:rivet", sign)
        _sym(m, foot, (0.5, 0, -4), (4, 1, 1), "neutral:plate", sign)


def _torso(m: Model) -> None:
    m.bone("body", "root", pivot=(0, 8, 0))
    m.box("body", (-3.5, 7, -2), (7, 1, 4), "neutral:plate")
    m.box("body", (-4.5, 8, -3), (9, 8, 6), "primary:rivet", faces={"north": "primary:panel:4"})
    m.box("body", (-3, 10, -3.5), (6, 3, 1), "secondary:vent")
    m.box("body", (-3.5, 15.5, -2.5), (7, 1, 5), "secondary:plate")
    m.bone("cargo_slot", "body", pivot=(0, 10, -6))
    m.bone("head", "body", pivot=(0, 16, 0))
    m.box("head", (-3, 16, -2.5), (6, 5, 5), "primary:plate", faces={"up": "secondary:panel:3"})
    for sign in (1, -1):
        _sym(m, "head", (3, 17.5, -1), (1, 2, 2), "neutral:plate", sign)
    m.bone("eye_lamp", "head", pivot=(0, 18.5, -3.25))
    m.box("eye_lamp", (-3, 16, -3), (6, 5, 1), "neutral:flat:1")
    m.box("eye_lamp", (-2, 16.5, -3.5), (4, 4, 1), "lamp:lens")
    for side, sign in (("l", 1), ("r", -1)):
        arm = m.bone(f"arm_{side}", "body", pivot=(5 * sign, 15, 0))
        _sym(m, arm, (4, 13, -2), (3, 3, 4), "primary:rivet", sign)
        _sym(m, arm, (4.5, 9, -1), (2, 4, 2), "neutral:plate", sign)
        hand = m.bone(f"hand_{side}", arm, pivot=(5.5 * sign, 9, 0))
        _sym(m, hand, (4, 8, -1.5), (3, 1, 3), "secondary:plate", sign)
        _sym(m, hand, (4, 6, -1.5), (3, 2, 1), "neutral:plate", sign)
        _sym(m, hand, (4, 6, 0.5), (3, 2, 1), "neutral:plate", sign)


def _optional(m: Model) -> None:
    m.bone("carry_rack", "body", pivot=(0, 8, 3))
    for sign in (1, -1):
        _sym(m, "carry_rack", (3, 8, 3), (1, 11, 1), "secondary:timber", sign)
    m.box("carry_rack", (-4, 18, 3), (8, 1, 1), "secondary:timber")
    m.box("carry_rack", (-4, 8, 3), (8, 1, 4), "secondary:wood")
    m.box("carry_rack", (-4, 9, 6), (8, 1, 1), "secondary:timber")
    m.bone("spring_key", "body", pivot=(0, 13.5, 4.5))
    m.box("spring_key", (-0.5, 13, 3), (1, 1, 3), "neutral:plate")
    m.box("spring_key", (-0.5, 12.5, 6), (1, 2, 1), "primary:flat:3.4")
    for sign in (1, -1):
        _sym(m, "spring_key", (0.5, 11.5, 6), (3, 4, 1), "primary:flat:3.4", sign)
    m.bone("battery_pack", "body", pivot=(0, 9, 4))
    m.box("battery_pack", (-3, 9, 3), (6, 5, 2), "neutral:rivet")
    m.box("battery_pack", (-2, 12, 5), (4, 1, 1), "lamp:glow")
    for sign in (1, -1):
        _sym(m, "battery_pack", (1, 14, 3.5), (1, 1, 1), "secondary:plate", sign)
    m.bone("tool_belt", "body", pivot=(0, 8, 0))
    m.box("tool_belt", (-5, 8, -3.5), (10, 1, 7), "secondary:leather")
    m.box("tool_belt", (-1, 7.5, -4), (2, 2, 1), "neutral:flat:3")
    for sign in (1, -1):
        _sym(m, "tool_belt", (4.5, 6, -2), (2, 3, 3), "primary:plate", sign)
    m.bone("climb_spikes", "body", pivot=(0, 12, -3.5))
    for sign in (1, -1):
        _sym(m, "climb_spikes", (2, 9, -4), (1, 6, 1), "neutral:plate", sign)
        for y in (9.5, 12, 14):
            _sym(m, "climb_spikes", (2, y, -5), (1, 1, 1), "neutral:flat:4", sign)
    m.bone("cable_reel", "body", pivot=(0, 16, 5))
    m.box("cable_reel", (-2.5, 14, 3.5), (5, 4, 3), "secondary:coil:v")
    m.box("cable_reel", (-4, 15.5, 4.5), (8, 1, 1), "neutral:plate")
    for sign in (1, -1):
        _sym(m, "cable_reel", (2.5, 13.5, 3), (1, 5, 4), "primary:plate", sign)
    for side, sign in (("l", 1), ("r", -1)):
        aux = m.bone(f"leg_aux_{side}", "root", pivot=(5 * sign, 9.5, 1.5), rotation=(0, 0, -20 * sign))
        _sym(m, aux, (4.5, 0.5, 1), (1, 9, 1), "neutral:plate", sign)
        _sym(m, aux, (4, 4, 0.5), (2, 4, 2), "primary:rivet", sign)
        _sym(m, aux, (3.5, 0, -0.5), (3, 1, 4), "secondary:plate", sign)
    m.bone("drill", "hand_r", pivot=(-5.5, 6, 0))
    m.box("drill", (-6.5, 5, -1), (2, 1, 2), "neutral:plate")
    m.box("drill", (-6, 2, -0.5), (1, 3, 1), "neutral:coil")
    m.bone("pod_shell_l", "body", pivot=(4, 12, 8))
    m.box("pod_shell_l", (0, 12, 5), (4, 7, 6), "primary:rivet")
    m.bone("pod_shell_r", "body", pivot=(-4, 12, 8))
    m.box("pod_shell_r", (-4, 12, 5), (4, 7, 6), "primary:rivet")
    m.bone("pod_shell_top", "body", pivot=(0, 19, 8))
    m.box("pod_shell_top", (-4.5, 19, 4.5), (9, 2, 7), "secondary:panel:4")
    m.box("pod_shell_top", (-2, 13, 6), (4, 6, 4), "lamp:glow")
    m.bone("lamp_mast", "body", pivot=(-3.5, 16, 1.5))
    m.box("lamp_mast", (-4, 16, 1), (1, 7, 1), "neutral:plate")
    m.box("lamp_mast", (-4.5, 23, 0.5), (2, 2, 2), "lamp:glow")
    m.box("lamp_mast", (-5, 25, 0), (3, 1, 3), "secondary:plate")


def _upgrades(m: Model) -> None:
    m.bone("upg_armor", "body", pivot=(0, 14, -4))
    m.box("upg_armor", (-4, 14, -4.5), (8, 2, 1), "primary:rivet")
    for sign in (1, -1):
        _sym(m, "upg_armor", (4, 16, -2), (3, 1, 4), "neutral:rivet", sign)
    m.bone("upg_cargo", "body", pivot=(5.5, 12, 0))
    m.box("upg_cargo", (4.5, 10, -2), (2, 4, 4), "secondary:rivet")
    m.bone("upg_range", "head", pivot=(1.5, 21, 1.5))
    m.box("upg_range", (1.5, 21, 1), (1, 4, 1), "neutral:plate")
    m.box("upg_range", (1, 25, 0.5), (2, 1, 2), "lamp:glow")
    m.bone("upg_speed", "body", pivot=(0, 6, 2.5))
    for sign in (1, -1):
        _sym(m, "upg_speed", (4.5, 5, 1), (1, 3, 3), "secondary:panel:3", sign)
        _sym(m, "upg_speed", (4.5, 5, 4), (1, 1, 1), "lamp:glow", sign)


def _steps(length: float, n: int = 8) -> list[float]:
    return [length * k / n for k in range(n + 1)]


def _anims(m: Model) -> None:
    idle = m.anim("idle", "loop", 2.0)
    idle.keys("body", "position", [(0, (0, 0, 0)), (1, (0, -0.3, 0)), (2, (0, 0, 0))])
    idle.keys("head", "rotation", [(0, (0, 0, 0)), (0.6, (0, 8, 0)), (1.4, (0, -8, 0)), (2, (0, 0, 0))])
    idle.keys("eye_lamp", "scale", [(0, (1, 1, 1)), (1, (0.85, 0.85, 1)), (2, (1, 1, 1))])
    for side, sign in (("l", 1), ("r", -1)):
        idle.keys(f"arm_{side}", "rotation", [(0, (0, 0, 0)), (1, (0, 0, -3 * sign)), (2, (0, 0, 0))])
    idle.spin("spring_key", "z", 1)

    walk = m.anim("walk", "loop", 1.0)
    for side, ph in (("r", 0.0), ("l", 0.5)):
        frames, feet, lift, arms = [], [], [], []
        for t in _steps(1.0):
            s = math.sin(2 * math.pi * (t + ph))
            c = math.cos(2 * math.pi * (t + ph))
            frames.append((t, (-26 * s, 0, 0)))
            feet.append((t, (16 * s, 0, 0)))
            lift.append((t, (0, max(0.0, -c) * 1.2, 0)))
            arms.append((t, (22 * s, 0, 0)))
        walk.keys(f"leg_{side}", "rotation", frames).keys(f"foot_{side}", "rotation", feet)
        walk.keys(f"leg_{side}", "position", lift).keys(f"arm_{side}", "rotation", arms)
    walk.keys("body", "position", [(t, (0, -0.5 * abs(math.sin(2 * math.pi * t)), 0)) for t in _steps(1.0)])
    walk.keys("body", "rotation", [(t, (4, 0, 3 * math.sin(2 * math.pi * t))) for t in _steps(1.0)])

    carry = m.anim("carry", "loop", 1.0)
    for side, sign in (("l", 1), ("r", -1)):
        carry.keys(f"arm_{side}", "rotation", [(0, (-72, 0, 8 * sign)), (0.5, (-68, 0, 8 * sign)), (1, (-72, 0, 8 * sign))])
        carry.keys(f"hand_{side}", "rotation", [(0, (30, 0, 0)), (1, (30, 0, 0))])
    carry.keys("body", "rotation", [(0, (-4, 0, 0)), (1, (-4, 0, 0))])

    work = m.anim("work", "loop", 1.0)
    work.keys("arm_r", "rotation", [(0, (-30, 0, 0)), (0.25, (-95, 0, 0)), (0.4, (-20, 0, 0)), (0.75, (-95, 0, 0)),
                                    (0.9, (-20, 0, 0)), (1, (-30, 0, 0))])
    work.keys("arm_l", "rotation", [(0, (-50, 0, 0)), (0.5, (-40, 0, 0)), (1, (-50, 0, 0))])
    work.keys("body", "rotation", [(0, (6, 0, 0)), (0.4, (9, 0, 0)), (0.9, (9, 0, 0)), (1, (6, 0, 0))])
    work.keys("head", "rotation", [(0, (10, 0, 0)), (1, (10, 0, 0))])

    crank = m.anim("crank", "loop", 1.0)
    for side in ("l", "r"):
        rot, pos = [], []
        for t in _steps(1.0):
            a = 2 * math.pi * t
            rot.append((t, (-62 + 30 * math.sin(a), 0, 0)))
            pos.append((t, (0, 1.2 * math.cos(a), -1.2 * math.sin(a))))
        crank.keys(f"arm_{side}", "rotation", rot).keys(f"arm_{side}", "position", pos)
        crank.keys(f"hand_{side}", "rotation", [(0, (20, 0, 0)), (1, (20, 0, 0))])
    crank.keys("body", "rotation", [(t, (8 + 3 * math.sin(2 * math.pi * t), 0, 0)) for t in _steps(1.0)])
    crank.keys("leg_l", "rotation", [(0, (-15, 0, 0)), (1, (-15, 0, 0))])
    crank.keys("leg_r", "rotation", [(0, (12, 0, 0)), (1, (12, 0, 0))])

    climb = m.anim("climb", "loop", 1.0)
    for side, ph in (("r", 0.0), ("l", 0.5)):
        climb.keys(f"arm_{side}", "rotation", [(t, (-135 - 30 * math.sin(2 * math.pi * (t + ph)), 0, 0)) for t in _steps(1.0)])
        climb.keys(f"leg_{side}", "rotation", [(t, (-25 + 22 * math.sin(2 * math.pi * (t + ph)), 0, 0)) for t in _steps(1.0)])
        climb.keys(f"foot_{side}", "rotation", [(0, (20, 0, 0)), (1, (20, 0, 0))])
    climb.keys("body", "rotation", [(0, (10, 0, 0)), (1, (10, 0, 0))])
    climb.keys("head", "rotation", [(0, (-15, 0, 0)), (1, (-15, 0, 0))])

    place = m.anim("place", "once", 0.75)
    place.keys("arm_r", "rotation", [(0, (0, 0, 0)), (0.35, (-88, 0, 6)), (0.5, (-80, 0, 6)), (0.75, (0, 0, 0))])
    place.keys("hand_r", "rotation", [(0, (0, 0, 0)), (0.35, (25, 0, 0)), (0.75, (0, 0, 0))])
    place.keys("arm_l", "rotation", [(0, (0, 0, 0)), (0.35, (-25, 0, 0)), (0.75, (0, 0, 0))])
    place.keys("body", "rotation", [(0, (0, 0, 0)), (0.35, (12, 0, 0)), (0.75, (0, 0, 0))])
    place.keys("head", "rotation", [(0, (0, 0, 0)), (0.35, (15, 0, 0)), (0.75, (0, 0, 0))])

    weld = m.anim("weld", "once", 1.0)
    weld.keys("arm_r", "rotation", [(0, (0, 0, 0)), (0.15, (-80, 0, 0)), (0.3, (-77, 3, 0)), (0.45, (-82, -3, 0)),
                                    (0.6, (-78, 2, 0)), (0.85, (-80, 0, 0)), (1, (0, 0, 0))])
    weld.keys("head", "rotation", [(0, (0, 0, 0)), (0.15, (14, 0, 0)), (0.85, (14, 0, 0)), (1, (0, 0, 0))])
    weld.keys("body", "rotation", [(0, (0, 0, 0)), (0.15, (6, 0, 0)), (0.85, (6, 0, 0)), (1, (0, 0, 0))])
    weld.keys("eye_lamp", "scale", [(0, (1, 1, 1)), (0.2, (1.25, 1.25, 1)), (0.35, (0.9, 0.9, 1)), (0.5, (1.25, 1.25, 1)),
                                    (0.65, (0.9, 0.9, 1)), (0.8, (1.2, 1.2, 1)), (1, (1, 1, 1))])

    string = m.anim("string", "once", 1.0)
    string.keys("arm_r", "rotation", [(0, (0, 0, 0)), (0.3, (-165, 0, 10)), (0.5, (-120, 0, 5)), (1, (0, 0, 0))])
    string.keys("arm_l", "rotation", [(0, (0, 0, 0)), (0.3, (-40, 0, 0)), (1, (0, 0, 0))])
    string.keys("body", "rotation", [(0, (0, 0, 0)), (0.3, (-6, 0, -5)), (1, (0, 0, 0))])
    string.spin("cable_reel", "x", -2)

    unfold = m.anim("unfold", "once", 1.5)
    unfold.keys("body", "position", [(0, (0, -4, 0)), (0.3, (0, -4, 0)), (1.0, (0, 0.5, 0)), (1.15, (0, 0, 0))])
    unfold.keys("head", "position", [(0, (0, -2.5, 0)), (0.5, (0, -2.5, 0)), (1.1, (0, 0, 0))])
    for side, sign in (("l", 1), ("r", -1)):
        unfold.keys(f"arm_{side}", "rotation", [(0, (-40, 0, 40 * sign)), (0.4, (-40, 0, 40 * sign)), (1.1, (0, 0, 0))])
        unfold.keys(f"leg_{side}", "rotation", [(0, (-70, 0, 0)), (0.3, (-70, 0, 0)), (1.0, (0, 0, 0))])
        unfold.keys(f"foot_{side}", "rotation", [(0, (70, 0, 0)), (0.3, (70, 0, 0)), (1.0, (0, 0, 0))])
    unfold.keys("eye_lamp", "scale", [(0, (0.2, 0.2, 1)), (1.0, (0.2, 0.2, 1)), (1.2, (1.3, 1.3, 1)), (1.5, (1, 1, 1))])

    deploy = m.anim("deploy", "hold", 5.0)
    for side, sign in (("l", 1), ("r", -1)):
        deploy.keys(f"leg_aux_{side}", "rotation", [(0, (0, 0, 0)), (1.5, (0, 0, -30 * sign))])
    deploy.keys("body", "position", [(0, (0, 0, 0)), (1.5, (0, -2, 0))])
    deploy.keys("pod_shell_l", "rotation", [(0, (0, 0, 0)), (1.5, (0, 0, 0)), (3.0, (0, 0, 110))])
    deploy.keys("pod_shell_r", "rotation", [(0, (0, 0, 0)), (1.5, (0, 0, 0)), (3.0, (0, 0, -110))])
    deploy.keys("pod_shell_top", "position", [(0, (0, 0, 0)), (2.5, (0, 0, 0)), (4.0, (0, 8, 0))])
    deploy.keys("pod_shell_top", "rotation", [(0, (0, 0, 0)), (2.5, (0, 0, 0)), (5.0, (0, 180, 0))])
    deploy.keys("lamp_mast", "position", [(0, (0, 0, 0)), (1.0, (0, 3, 0))])
    deploy.keys("arm_r", "rotation", [(0, (0, 0, 0)), (1.5, (-25, 0, 0))])
    deploy.keys("drill", "position", [(0, (0, 0, 0)), (1.5, (0, 0, 0)), (2.5, (0, -1.5, 0))])
    deploy.spin("drill", "y", 6, t0=2.5, t1=5.0, steps=24)
    deploy.keys("eye_lamp", "scale", [(0, (1, 1, 1)), (4.5, (1, 1, 1)), (5.0, (1.2, 1.2, 1))])

    recharge = m.anim("recharge", "loop", 2.0)
    recharge.keys("body", "position", [(0, (0, -1, 0)), (2, (0, -1, 0))])
    recharge.keys("head", "rotation", [(0, (15, 0, 0)), (2, (15, 0, 0))])
    for side, sign in (("l", 1), ("r", -1)):
        recharge.keys(f"arm_{side}", "rotation", [(0, (5, 0, -6 * sign)), (2, (5, 0, -6 * sign))])
    recharge.keys("eye_lamp", "scale", [(0, (0.6, 0.6, 1)), (1, (1, 1, 1)), (2, (0.6, 0.6, 1))])
    recharge.spin("spring_key", "z", 3)

    rundown = m.anim("rundown", "hold", 2.0)
    rundown.keys("body", "rotation", [(0, (0, 0, 0)), (1.2, (22, 0, 4)), (1.4, (19, 0, 3)), (1.6, (22, 0, 4))])
    rundown.keys("body", "position", [(0, (0, 0, 0)), (1.2, (0, -1.5, 0))])
    rundown.keys("head", "rotation", [(0, (0, 0, 0)), (1.4, (28, 0, -6))])
    for side, sign in (("l", 1), ("r", -1)):
        rundown.keys(f"arm_{side}", "rotation", [(0, (0, 0, 0)), (1.2, (18, 0, -12 * sign))])
        rundown.keys(f"leg_{side}", "rotation", [(0, (0, 0, 0)), (1.2, (-14, 0, 0))])
        rundown.keys(f"foot_{side}", "rotation", [(0, (0, 0, 0)), (1.2, (14, 0, 0))])
    rundown.keys("eye_lamp", "scale", [(0, (1, 1, 1)), (0.6, (0.6, 0.6, 1)), (0.8, (0.95, 0.95, 1)),
                                       (1.4, (0.5, 0.5, 1)), (2, (0.3, 0.3, 1))])
    rundown.keys("spring_key", "rotation", [(0, (0, 0, 0)), (2, (0, 0, 40))])

    death = m.anim("death", "hold", 1.5)
    death.keys("root", "rotation", [(0, (0, 0, 0)), (0.4, (22, 0, 4)), (0.8, (90, 0, 6)), (0.95, (82, 0, 6)), (1.1, (90, 0, 6))])
    death.keys("root", "position", [(0, (0, 0, 0)), (0.4, (0, 0.5, 0)), (0.8, (0, 3.5, 0))])
    death.keys("head", "rotation", [(0, (0, 0, 0)), (0.9, (-25, 15, 0))])
    for side, sign in (("l", 1), ("r", -1)):
        death.keys(f"arm_{side}", "rotation", [(0, (0, 0, 0)), (0.8, (-150, 0, -25 * sign))])
    death.keys("eye_lamp", "scale", [(0, (1, 1, 1)), (0.7, (1, 1, 1)), (0.9, (0, 0, 1))])


def walker() -> Model:
    """Build the walker rig with every base, optional and upgrade bone and all walker animations."""
    m = Model("walker")
    m.bone("root")
    _legs(m)
    _torso(m)
    _optional(m)
    _upgrades(m)
    _anims(m)
    return m

"""GeckoLib block models of tier 6 (ascendant): aegis black body, gold trim and cyan glow."""

import math

from geo_core import Model
from geo_shapes_t23 import circle_mask, extrude

BODY = "aegis_body:panel"
RIVET = "aegis_body:rivet"
PLATE = "aegis_body:plate"
SEAM = "aegis_body:seam"
DARK = "aegis_body:dark"
GOLD = "aegis_gold:plate"
GOLD_R = "aegis_gold:rivet"
GLOW = "aegis_glow:glow"
ENERGY = "aegis_glow:energy"
LENS = "aegis_glow:lens"
CRYSTAL = "aegis_glow:crystal"


def ring(m: Model, bone: str, c, radius: float, axis: str, n: int, thick: int, depth: int, mat: str,
         inner: str | None = None) -> None:
    """Add a ring of n tangent segments around `axis` ('x', 'y' or 'z') centred on c.

    Args:
        thick: radial thickness in pixels; depth: extent along the ring axis.
        inner: material of the faces pointing at the ring centre.
    """
    cx, cy, cz = c
    seg = int(math.ceil(2 * radius * math.tan(math.pi / n))) + 1
    for k in range(n):
        a = 360.0 * k / n
        if axis == "y":
            m.box(bone, (cx - seg / 2, cy - depth / 2, cz - radius - thick / 2), (seg, depth, thick), mat,
                  faces={"south": inner} if inner else None, rotation=(0, a, 0), pivot=c)
        elif axis == "z":
            m.box(bone, (cx - seg / 2, cy + radius - thick / 2, cz - depth / 2), (seg, thick, depth), mat,
                  faces={"down": inner} if inner else None, rotation=(0, 0, a), pivot=c)
        else:
            m.box(bone, (cx - depth / 2, cy + radius - thick / 2, cz - seg / 2), (depth, thick, seg), mat,
                  faces={"down": inner} if inner else None, rotation=(a, 0, 0), pivot=c)


def orb(m: Model, bone: str, c, s: int, mat: str, rot=None) -> None:
    """Add a rounded orb of diameter s centred on c from three crossed boxes of distinct thicknesses."""
    cx, cy, cz = c
    a, b = max(1, round(s * 0.66)), max(1, round(s * 0.8))
    if a == b:
        a = max(1, b - 1)
    for sx, sy, sz in ((s, a, b), (b, s, a), (a, b, s)):
        m.box(bone, (cx - sx / 2, cy - sy / 2, cz - sz / 2), (sx, sy, sz), mat, rotation=rot, pivot=c if rot else None)


def aim_yaw(px: float, pz: float) -> float:
    """Return the file Y rotation that makes a negative file X tilt swing a hanging bone toward the Y axis."""
    rx, rz = -px, pz
    ln = math.hypot(rx, rz)
    cx, cz = -rx / ln, -rz / ln
    return -math.degrees(math.atan2(-cx, -cz))


def two_link(reach: float, drop: float, l1: float, l2: float) -> tuple[float, float]:
    """Return (upper, elbow) tilt angles in degrees that put a two-link hanging arm tip at reach/drop."""
    d = min(math.hypot(reach, drop), l1 + l2 - 0.01)
    cb = max(-1.0, min(1.0, (d * d - l1 * l1 - l2 * l2) / (2 * l1 * l2)))
    beta = math.acos(cb)
    alpha = math.atan2(reach, drop) - math.atan2(l2 * math.sin(beta), l1 + l2 * math.cos(beta))
    return math.degrees(alpha), math.degrees(beta)


def singularity_core() -> Model:
    """Black-gold pedestal with three nested gyro rings around a black core with cyan rim glow."""
    m = Model("singularity_core")
    m.bone("base")
    m.cbox("base", (0, 0, 0), (80, 4, 80), RIVET)
    m.cbox("base", (0, 4, 0), (72, 4, 72), SEAM)
    for z in (-35, 35):
        m.cbox("base", (0, 8, z), (72, 1, 2), GOLD)
    for x in (-35, 35):
        m.cbox("base", (x, 8, 0), (2, 1, 68), GOLD)
    m.cbox("base", (0, 8, 0), (40, 4, 40), BODY)
    m.cbox("base", (0, 12, 0), (14, 10, 14), GOLD_R)
    m.cbox("base", (0, 22, 0), (18, 3, 18), PLATE, faces={"up": SEAM})
    for k in range(4):
        m.box("base", (-1, 22, -10), (2, 3, 2), GOLD, faces={"up": GLOW}, rotation=(0, 45 + 90 * k, 0), pivot=(0, 22, 0))
    for x in (-34, 34):
        for z in (-34, 34):
            m.cbox("base", (x, 8, z), (8, 28, 8), SEAM)
            m.cbox("base", (x, 12, z), (9, 2, 9), GOLD)
            m.cbox("base", (x, 36, z), (10, 3, 10), GOLD_R)
            m.cbox("base", (x, 39, z), (4, 7, 4), CRYSTAL)
            m.cbox("base", (x * 0.62, 8, z * 0.62), (6, 3, 6), GOLD)
    c = (0, 46, 0)
    m.bone("core", "base", pivot=c)
    orb(m, "core", c, 22, PLATE)
    ring(m, "core", c, 12, "y", 12, 2, 2, GLOW)
    ring(m, "core", c, 12, "z", 12, 1, 1, GLOW)
    m.bone("ring_outer", "base", pivot=c)
    ring(m, "ring_outer", c, 33, "z", 18, 3, 4, GOLD_R, GLOW)
    m.bone("ring_mid", "base", pivot=c, rotation=(0, 45, 0))
    ring(m, "ring_mid", c, 26, "x", 16, 3, 3, GOLD, GLOW)
    m.bone("ring_inner", "base", pivot=c, rotation=(0, 0, 25))
    ring(m, "ring_inner", c, 19, "y", 14, 2, 3, GOLD, GLOW)

    run = m.anim("run", "loop", 6.0)
    run.spin("ring_outer", "y", 3, steps=24)
    run.spin("ring_mid", "z", -4, steps=32)
    run.spin("ring_inner", "x", 5, steps=40)
    run.osc("core", "scale", (0.06, 0.06, 0.06), cycles=6, steps=24)
    idle = m.anim("idle", "loop", 4.0)
    idle.spin("ring_outer", "y", 1, steps=16)
    idle.spin("ring_mid", "z", -1, steps=16)
    idle.spin("ring_inner", "x", 1, steps=16)
    idle.osc("core", "scale", (0.03, 0.03, 0.03), cycles=2, steps=16)
    for a, cycles in ((run, 1.5), (idle, 1.0)):
        for k, bone in enumerate(("ring_outer", "ring_mid", "ring_inner")):
            a.osc(bone, "position", (0, 2, 0), cycles=cycles, phase=k / 3, steps=24)
    drink = m.anim("drink", "once", 1.0)
    drink.keys("core", "scale", [(0, (1, 1, 1)), (0.3, (1.35, 1.35, 1.35)), (0.6, (0.88, 0.88, 0.88)), (1.0, (1, 1, 1))])
    drink.keys("ring_inner", "scale", [(0, (1, 1, 1)), (0.35, (0.85, 0.85, 0.85)), (1.0, (1, 1, 1))])
    drink.keys("ring_mid", "scale", [(0, (1, 1, 1)), (0.45, (0.9, 0.9, 0.9)), (1.0, (1, 1, 1))])
    return m


def gyro_heart() -> Model:
    """Huge horizontal gyroscope disc inside a black-gold housing with cyan glow seams."""
    m = Model("gyro_heart")
    m.bone("base")
    m.cbox("base", (0, 0, 0), (80, 4, 80), RIVET)
    ring(m, "base", (0, 12, 0), 34, "y", 16, 6, 16, SEAM, GOLD)
    ring(m, "base", (0, 21, 0), 34, "y", 16, 8, 2, GOLD_R)
    m.cbox("base", (0, 4, 0), (56, 2, 56), DARK)
    for x in (-34, 34):
        for z in (-34, 34):
            m.cbox("base", (x, 4, z), (8, 40, 8), SEAM)
            m.cbox("base", (x, 22, z), (10, 3, 10), GOLD)
            m.cbox("base", (x, 44, z), (10, 4, 10), GOLD_R)
    m.box("base", (-48, 40, -3), (96, 4, 6), RIVET, rotation=(0, 45, 0), pivot=(0, 42, 0))
    m.box("base", (-48, 39, -3), (96, 4, 6), RIVET, rotation=(0, -45, 0), pivot=(0, 41, 0))
    m.cbox("base", (0, 36, 0), (14, 10, 14), GOLD_R)
    m.cbox("base", (0, 33, 0), (8, 3, 8), PLATE, faces={"down": GLOW})
    c = (0, 25, 0)
    m.bone("rotor", "base", pivot=c)
    ring(m, "rotor", c, 26, "y", 16, 4, 6, GOLD_R)
    m.cbox("rotor", (0, 23, 0), (44, 4, 18), SEAM)
    m.box("rotor", (-9, 22.5, -22), (18, 5, 44), SEAM)
    m.box("rotor", (-25, 27, -2), (50, 2, 4), GOLD, rotation=(0, 45, 0), pivot=c)
    m.box("rotor", (-25, 27.5, -2), (50, 2, 4), GOLD, rotation=(0, -45, 0), pivot=c)
    orb(m, "rotor", (0, 27, 0), 10, ENERGY)
    for k in range(4):
        m.box("rotor", (-2, 21, -27), (4, 8, 3), PLATE, faces={"north": GLOW}, rotation=(0, 45 + 90 * k, 0), pivot=c)
    m.bone("rim", "base", pivot=(0, 22, 0))
    ring(m, "rim", (0, 22, 0), 30.5, "y", 20, 1, 2, GLOW)

    spin = m.anim("spin", "loop", 1.0)
    spin.spin("rotor", "y", 1, steps=8)
    spin.osc("rim", "scale", (0.0, 0.5, 0.0), cycles=2, steps=8)
    m.anim("idle", "loop", 1.0).osc("rim", "scale", (0.0, 0.25, 0.0), steps=8)
    return m


def matter_compiler() -> Model:
    """Black-gold frame with an inner cage chamber and three emitter arms aimed at a glowing centre."""
    m = Model("matter_compiler")
    m.bone("body")
    m.cbox("body", (0, 0, 0), (32, 5, 32), SEAM, faces={"up": RIVET, "down": RIVET})
    m.cbox("body", (0, 28, 0), (32, 4, 32), RIVET, faces={"up": SEAM})
    m.cbox("body", (0, 5, 0), (22, 1, 22), GOLD, faces={"up": "aegis_gold:panel:5"})
    m.cbox("body", (0, 26, 0), (22, 2, 22), GOLD_R)
    for x in (-14, 14):
        for z in (-14, 14):
            m.cbox("body", (x, 5, z), (4, 23, 4), SEAM)
            m.cbox("body", (x, 14, z), (5, 3, 5), GOLD)
    c = (0, 16, 0)
    m.bone("chamber", "body", pivot=c)
    ring(m, "chamber", (0, 8, 0), 8, "y", 8, 1, 1, GOLD, GLOW)
    ring(m, "chamber", (0, 24, 0), 8, "y", 8, 1, 1, GOLD, GLOW)
    for k in range(4):
        m.box("chamber", (-0.5, 8, -8.5), (1, 16, 1), GOLD, rotation=(0, 45 + 90 * k, 0), pivot=c)
    m.bone("glow", "body", pivot=c)
    orb(m, "glow", c, 13, GLOW, rot=(0, 45, 0))
    m.bone("emitters", "body", pivot=c)
    for i, (px, pz) in enumerate(((-10, -10), (10, -10), (0, 12))):
        name = f"emitter_{i + 1}"
        alpha = math.degrees(math.atan2(math.hypot(px, pz), 10.0))
        m.bone(name, "emitters", pivot=(px, 26, pz), rotation=(-alpha, aim_yaw(px, pz), 0))
        m.cbox(name, (px, 22, pz), (4, 4, 4), PLATE, faces={"up": GOLD})
        m.cbox(name, (px, 16, pz), (2, 6, 2), GOLD)
        m.cbox(name, (px, 15, pz), (3, 1, 3), PLATE)
        m.cbox(name, (px, 13, pz), (1, 2, 1), GLOW)

    comp = m.anim("compile", "loop", 2.0)
    comp.spin("glow", "y", 2, base=(0, 0, 0), steps=16)
    comp.osc("glow", "scale", (0.18, 0.18, 0.18), cycles=4, steps=16)
    comp.spin("chamber", "y", -0.25 * 4, steps=16)
    for i in range(3):
        comp.osc(f"emitter_{i + 1}", "position", (0, -0.8, 0), cycles=4, phase=i / 3.0, steps=16)
    idle = m.anim("idle", "loop", 1.0)
    idle.osc("glow", "scale", (0.06, 0.06, 0.06), steps=8)
    idle.osc("glow", "position", (0, 0.5, 0), steps=8)
    return m


def seed_matrix() -> Model:
    """Four pillars around a floating black-gold seed pod; rings of light pack it."""
    m = Model("seed_matrix")
    m.bone("base")
    m.cbox("base", (0, 0, 0), (48, 4, 48), RIVET)
    m.cbox("base", (0, 4, 0), (36, 3, 36), SEAM)
    m.cbox("base", (0, 7, 0), (20, 2, 20), GOLD_R)
    m.cbox("base", (0, 9, 0), (14, 1, 14), GLOW)
    m.bone("pillars", "base")
    for x, z in ((0, -20), (0, 20), (-20, 0), (20, 0)):
        m.cbox("pillars", (x, 4, z), (5, 40, 5), SEAM)
        for y in (8, 24, 38):
            m.cbox("pillars", (x, y, z), (6, 2, 6), GOLD)
        m.cbox("pillars", (x, 44, z), (7, 4, 7), GOLD_R)
        m.cbox("pillars", (x * 0.78, 28, z * 0.78), (2, 6, 2), CRYSTAL)
    for x in (-21, 21):
        for z in (-21, 21):
            m.cbox("pillars", (x, 4, z), (6, 6, 6), GOLD_R, faces={"up": GLOW})
    c = (0, 27, 0)
    m.bone("seed", "base", pivot=c)
    orb(m, "seed", c, 15, SEAM)
    orb(m, "seed", (0, 34, 0), 9, GOLD_R)
    orb(m, "seed", (0, 20, 0), 8, GOLD)
    m.cbox("seed", (0, 38, 0), (3, 3, 3), GOLD)
    m.cbox("seed", (0, 41, 0), (1, 2, 1), GLOW)
    m.cbox("seed", (0, 14, 0), (2, 3, 2), GOLD)
    for x, z in ((-7.5, 0), (7.5, 0), (0, -7.5), (0, 7.5)):
        sx, sz = (1, 2) if x else (2, 1)
        m.cbox("seed", (x, 21, z), (sx, 12, sz), GLOW)
    m.bone("rings", "base", pivot=c)
    for name, y, r in (("ring_low", 20, 14), ("ring_mid", 27, 15), ("ring_high", 34, 14)):
        m.bone(name, "rings", pivot=(0, y, 0))
        ring(m, name, (0, y, 0), r, "y", 12, 1, 1, GLOW)

    pack = m.anim("pack", "loop", 3.0)
    pack.spin("rings", "y", 1, steps=12)
    pack.osc("ring_low", "position", (0, 4, 0), cycles=1, steps=12)
    pack.osc("ring_high", "position", (0, -4, 0), cycles=1, steps=12)
    pack.osc("ring_mid", "scale", (-0.18, 0, -0.18), base=(0.9, 1, 0.9), cycles=2, steps=12)
    pack.osc("ring_low", "scale", (-0.15, 0, -0.15), base=(0.9, 1, 0.9), cycles=1, steps=12)
    pack.osc("ring_high", "scale", (-0.15, 0, -0.15), base=(0.9, 1, 0.9), cycles=1, steps=12)
    pack.osc("seed", "scale", (-0.06, -0.06, -0.06), cycles=2, steps=12)
    pack.spin("seed", "y", -1, steps=12)
    idle = m.anim("idle", "loop", 2.0)
    idle.osc("seed", "position", (0, 1.0, 0), steps=8)
    idle.spin("rings", "y", 1 / 12, steps=4)
    idle.osc("ring_mid", "position", (0, -0.5, 0), steps=8)
    return m


def restoration_projector() -> Model:
    """Pedestal with a big upward projector lens and a rotating pronged ring."""
    m = Model("restoration_projector")
    m.bone("base")
    m.cbox("base", (0, 0, 0), (16, 3, 16), RIVET)
    m.cbox("base", (0, 3, 0), (12, 3, 12), GOLD_R)
    m.cbox("base", (0, 6, 0), (8, 14, 6), SEAM)
    m.cbox("base", (0, 6, 0), (6, 13, 8), BODY)
    m.cbox("base", (0, 11, 0), (9, 2, 9), GOLD)
    m.cbox("base", (0, 20, 0), (14, 6, 14), SEAM, faces={"up": PLATE})
    m.cbox("base", (0, 24, 0), (15, 2, 15), GOLD)
    m.bone("lens", "base", pivot=(0, 27, 0))
    m.cbox("lens", (0, 26, 0), (12, 2, 12), PLATE, faces={"up": LENS})
    m.cbox("lens", (0, 28, 0), (6, 2, 6), GLOW)
    m.bone("ring", "base", pivot=(0, 28.5, 0))
    ring(m, "ring", (0, 28.5, 0), 7, "y", 10, 1, 1, GOLD, GLOW)
    for k in range(3):
        m.box("ring", (-0.5, 29, -7.5), (1, 3, 1), GOLD, faces={"up": GLOW}, rotation=(0, 120 * k, 0), pivot=(0, 28.5, 0))

    proj = m.anim("project", "loop", 2.0)
    proj.spin("ring", "y", 2, steps=16)
    proj.osc("ring", "position", (0, 0.8, 0), cycles=2, steps=16)
    proj.osc("lens", "scale", (0.08, 0.15, 0.08), cycles=4, steps=16)
    idle = m.anim("idle", "loop", 2.0)
    idle.spin("ring", "y", 1 / 3, steps=6)
    idle.osc("lens", "scale", (0.03, 0.05, 0.03), steps=6)
    return m


ARM_SHOULDER_Y, ARM_ELBOW_Y, ARM_L1, ARM_L2 = 47.0, 35.0, 12.0, 12.0
ARM_SPOTS = ((-11, -11), (11, -11), (11, 11), (-11, 11))
FIT_TARGETS = {"boots": (18.0, 4.0), "legs": (25.0, 5.0), "chest": (34.0, 6.0), "helmet": (43.0, 5.5)}


def arm_pose(target: str) -> tuple[tuple, tuple]:
    """Return (upper, fore) file rotations reaching the named fitting height from any shoulder."""
    h, r = FIT_TARGETS[target]
    reach = math.hypot(11, 11) - r
    a, b = two_link(reach, ARM_SHOULDER_Y - h, ARM_L1, ARM_L2)
    return (-a, 0, 0), (-b, 0, 0)


def forge_of_ascension() -> Model:
    """Open 3x3 chamber: glowing pad, corner pillars, roof with four jointed arms and a sweeping scan ring."""
    m = Model("forge_of_ascension")
    m.bone("base")
    m.cbox("base", (0, 0, 0), (48, 4, 48), RIVET)
    m.cbox("base", (0, 4, 0), (46, 8, 46), SEAM)
    m.cbox("base", (0, 12, 0), (48, 2, 48), GOLD)
    for z in (-16, 16):
        m.cbox("base", (0, 14, z), (48, 2, 16), SEAM)
    for x in (-16, 16):
        m.cbox("base", (x, 14, 0), (16, 2, 16), SEAM)
    for z in (-7, 7):
        m.cbox("base", (0, 14, z), (16, 2, 2), GOLD)
    for x in (-7, 7):
        m.cbox("base", (x, 14, 0), (2, 2, 12), GOLD)
    m.cbox("base", (0, 5, -23.5), (12, 6, 1), GOLD, faces={"north": "aegis_gold:panel:4"})
    m.bone("pad", "base", pivot=(0, 15, 0))
    m.cbox("pad", (0, 14, 0), (12, 2, 12), GLOW)
    m.bone("pillars", "base")
    for x in (-19, 19):
        for z in (-19, 19):
            m.cbox("pillars", (x, 16, z), (10, 32, 10), SEAM)
            for y in (16, 30, 45):
                m.cbox("pillars", (x, y, z), (11, 2, 11), GOLD)
            m.cbox("pillars", (x * 0.74, 20, z * 0.74), (1, 22, 1), GLOW)
    m.bone("roof", "base")
    m.cbox("roof", (0, 54, 0), (46, 10, 46), RIVET, faces={"up": SEAM})
    m.cbox("roof", (0, 58, 0), (48, 2, 48), GOLD)
    for z in (-21, 21):
        m.cbox("roof", (0, 48, z), (48, 6, 6), BODY)
    for x in (-21, 21):
        m.cbox("roof", (x, 48, 0), (6, 6, 36), BODY)
    m.cbox("roof", (0, 48, 0), (14, 6, 14), GOLD_R, faces={"down": LENS})
    m.cbox("roof", (0, 63, 0), (20, 1, 20), GOLD)
    for x, z in ARM_SPOTS:
        m.cbox("roof", (x, 48, z), (5, 6, 5), GOLD_R)
    for i, (x, z) in enumerate(ARM_SPOTS):
        arm, fore = f"arm_{i + 1}", f"arm_{i + 1}_fore"
        m.bone(arm, "roof", pivot=(x, ARM_SHOULDER_Y, z), rotation=(0, aim_yaw(x, z), 0))
        m.cbox(arm, (x, 44, z), (4, 4, 4), PLATE)
        m.cbox(arm, (x, ARM_ELBOW_Y, z), (3, 10, 3), GOLD_R)
        m.bone(fore, arm, pivot=(x, ARM_ELBOW_Y, z))
        m.cbox(fore, (x, 33, z), (4, 4, 4), PLATE)
        m.cbox(fore, (x, 26, z), (2, 7, 2), GOLD)
        m.cbox(fore, (x, 25, z), (3, 2, 3), PLATE)
        m.cbox(fore, (x, 23, z), (2, 2, 2), GLOW)
    m.bone("scan_ring", "base", pivot=(0, 47, 0))
    ring(m, "scan_ring", (0, 47, 0), 13, "y", 16, 2, 2, GOLD, GLOW)
    _forge_anims(m)
    return m


def _arm_keys(a, frames) -> None:
    """Key all four arms through frames [(t, pose_name | None)] (None = rest)."""
    for i in range(4):
        arm, fore = f"arm_{i + 1}", f"arm_{i + 1}_fore"
        for t, pose in frames:
            up, lo = arm_pose(pose) if pose else ((0, 0, 0), (0, 0, 0))
            a.key(arm, "rotation", t, up)
            a.key(fore, "rotation", t, lo)


def _fit(m: Model, name: str, length: float, target: str) -> None:
    a = m.anim(name, "once", length)
    work = [(0.0, None), (0.8, target)]
    t = 1.2
    while t < length - 1.0:
        work.append((t, target))
        t += 0.6
    work += [(length - 0.8, target), (length, None)]
    _arm_keys(a, work)
    for i in range(4):
        taps = [(t, (0, 0.8 if (k + i) % 2 and 1 < k < len(work) - 2 else 0, 0)) for k, (t, _p) in enumerate(work)]
        a.keys(f"arm_{i + 1}_fore", "position", taps)
    drop = FIT_TARGETS[target][0] - 47.0
    a.keys("scan_ring", "position", [(0, (0, 0, 0)), (0.8, (0, drop, 0)), (length - 0.8, (0, drop, 0)), (length, (0, 0, 0))])
    a.spin("scan_ring", "y", length / 2.0, steps=int(length * 4))
    a.osc("pad", "scale", (0.08, 0.5, 0.08), cycles=length, steps=int(length * 4))


def _forge_anims(m: Model) -> None:
    idle = m.anim("idle", "loop", 2.0)
    idle.spin("scan_ring", "y", 1 / 16, steps=4)
    idle.osc("pad", "scale", (0.0, 0.25, 0.0), steps=8)
    for i in range(4):
        idle.osc(f"arm_{i + 1}", "rotation", (-2.0, 0, 0), phase=i / 4.0, steps=8)
    wake = m.anim("wake", "once", 1.0)
    wake.keys("pad", "scale", [(0, (0.6, 0.2, 0.6)), (0.5, (1.1, 1.5, 1.1)), (1.0, (1, 1, 1))])
    wake.keys("scan_ring", "position", [(0, (0, 0, 0)), (0.5, (0, -4, 0)), (1.0, (0, 0, 0))])
    wake.keys("scan_ring", "scale", [(0, (0.7, 1, 0.7)), (0.5, (1.05, 1, 1.05)), (1.0, (1, 1, 1))])
    for i in range(4):
        wake.keys(f"arm_{i + 1}", "rotation", [(0, (0, 0, 0)), (0.4, (-18, 0, 0)), (1.0, (0, 0, 0))])
        wake.keys(f"arm_{i + 1}_fore", "rotation", [(0, (0, 0, 0)), (0.5, (-25, 0, 0)), (1.0, (0, 0, 0))])
    scan = m.anim("scan", "once", 2.0)
    scan.keys("scan_ring", "position", [(0, (0, 0, 0)), (1.0, (0, -31, 0)), (2.0, (0, 0, 0))])
    scan.spin("scan_ring", "y", 1, steps=8)
    scan.keys("pad", "scale", [(0, (1, 1, 1)), (1.0, (1.05, 1.6, 1.05)), (2.0, (1, 1, 1))])
    for name, length, target in (("fit_boots", 5.0, "boots"), ("fit_legs", 6.0, "legs"),
                                 ("fit_chest", 7.0, "chest"), ("fit_helmet", 5.0, "helmet")):
        _fit(m, name, length, target)
    seal = m.anim("seal", "once", 4.0)
    _arm_keys(seal, [(0, None), (0.8, "chest"), (2.0, "helmet"), (3.2, "chest"), (4.0, None)])
    seal.keys("scan_ring", "position", [(0, (0, 0, 0)), (1.0, (0, -31, 0)), (2.0, (0, 0, 0)), (3.0, (0, -31, 0)), (4.0, (0, 0, 0))])
    seal.spin("scan_ring", "y", 2, steps=16)
    seal.osc("pad", "scale", (0.1, 0.8, 0.1), cycles=4, steps=16)
    retract = m.anim("retract", "once", 2.0)
    _arm_keys(retract, [(0, "chest"), (1.4, None), (2.0, None)])
    retract.keys("scan_ring", "position", [(0, (0, -14, 0)), (1.5, (0, 0, 0)), (2.0, (0, 0, 0))])
    retract.keys("pad", "scale", [(0, (1, 1, 1)), (2.0, (0.9, 0.6, 0.9))])


def domain_archive() -> Model:
    """Black-gold cabinet with three gold data drums behind a front grate and a cyan status lamp on top."""
    m = Model("domain_archive")
    m.bone("cabinet")
    m.cbox("cabinet", (0, 0, 0), (16, 2, 16), RIVET)
    m.cbox("cabinet", (0, 14, 0), (16, 2, 16), RIVET, faces={"up": PLATE})
    for x in (-8, 7):
        m.box("cabinet", (x, 2, -8), (1, 12, 16), BODY)
    m.box("cabinet", (-7, 2, 7), (14, 12, 1), BODY)
    m.box("cabinet", (-7, 2, -2), (14, 12, 1), DARK)
    m.box("cabinet", (-8, 13, -8.5), (16, 1, 1), GOLD)
    m.box("cabinet", (-8, 2, -8.5), (16, 1, 1), GOLD)
    m.box("cabinet", (-7, 3, -7.5), (14, 10, 0), "aegis_body:grate")
    for k, (x, y) in enumerate(((-3.5, 10.5), (3.5, 10.5), (0, 4.5)), 1):
        b = m.bone(f"drum_{k}", "cabinet", pivot=(x, y, -4))
        extrude(m, b, circle_mask(5), "z", (x, y, -4), 2, GOLD)
        m.box(b, (x - 2.5, y - 2.5, -5.05), (5, 5, 0), "aegis_gold:spoked")
        m.cbox(b, (x, y - 0.5, -5.5), (1, 1, 1), DARK)
    m.bone("lamp", "cabinet", pivot=(5.5, 16, -6))
    m.box("lamp", (4.5, 16, -7), (2, 1, 2), GLOW)
    run = m.anim("run", "loop", 4.0)
    for k, turns in ((1, 1), (2, -1), (3, 2)):
        run.spin(f"drum_{k}", "z", turns, steps=16)
    run.osc("lamp", "scale", (0.15, 0.15, 0.15), cycles=4, steps=16)
    m.anim("idle", "loop", 1.0).keys("lamp", "scale", [(0, (1, 1, 1)), (1.0, (1, 1, 1))])
    return m


MODELS = {
    "domain_archive": domain_archive,
    "singularity_core": singularity_core,
    "gyro_heart": gyro_heart,
    "matter_compiler": matter_compiler,
    "seed_matrix": seed_matrix,
    "restoration_projector": restoration_projector,
    "forge_of_ascension": forge_of_ascension,
}
